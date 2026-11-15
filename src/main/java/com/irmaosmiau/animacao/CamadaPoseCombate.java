package com.irmaosmiau.animacao;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.entities.IrmaoMiau;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Entrega 5J: camada ADITIVA e provisoria sobre a pose mantida da PEGADA_1
 * (PEGADA_ARRASTO e PEGADA_DESEQUILIBRIO), ate existirem animacoes do
 * MiauStudio para essas fases.
 *
 * - Arrasto: passos curtos e alternados nas pernas dos DOIS lutadores, so
 *   enquanto a dupla esta sendo arrastada; a fase avanca pela distancia
 *   REAL percorrida (um passo a cada {@link #PIXELS_POR_FASE} px), entao para
 *   quando a dupla para e continua de onde estava quando ela volta a andar.
 * - Desequilibrio: quando o motor aplica o efeito DESEQUILIBRIO no receptor
 *   (so acontece com sucesso), o tronco inclina para a frente e um joelho
 *   ajusta o apoio por {@link #DURACAO_REACAO_TICKS} ticks, e volta.
 *
 * Nunca move ninguem, nunca muda a regra, a velocidade ou a firmeza: so soma
 * poucos graus/unidades a uma COPIA da pose que o SistemaCombate deixou, e
 * devolve a pose original quando nada esta acontecendo (sem acumulo). Se o
 * motor trocar a pose (nova tecnica, fim da pegada), a camada recomeca dela.
 */
public final class CamadaPoseCombate {

    /** Um passo a cada 4 px de deslocamento real da dupla. */
    public static final int PIXELS_POR_FASE = 4;
    /** Joelho/pe que avanca no passo, em unidades do rig (~2 px na escala 0,7). */
    public static final int PASSO = 3;
    /** Quanto o pe levanta no passo. */
    public static final int ELEVACAO_PE = 2;

    public static final int DURACAO_REACAO_TICKS = 24;
    /** Inclinacao maxima do peito na reacao (graus) e da cintura. */
    public static final int INCLINACAO_PEITO = 8;
    public static final int INCLINACAO_CINTURA = 4;

    // Indices do quadro de 30 valores (ConversorMiauStudio).
    private static final int ROT_CINTURA = 6;
    private static final int ROT_PEITO = 9;
    private static final int ROT_CABECA = 11;
    private static final int JOEL_ESQ_X = 22;
    private static final int PE_ESQ_X = 24;
    private static final int PE_ESQ_Y = 25;
    private static final int JOEL_DIR_X = 26;
    private static final int PE_DIR_X = 28;
    private static final int PE_DIR_Y = 29;

    private static final class Estado {
        int[] base;
        int[] ultimaEscrita;
        int ultimoX;
        int distancia;
        int restanteAnterior;
        long inicioReacao = Long.MIN_VALUE;
        int fase = -1;
        double intensidadeReacao;
    }

    private final Map<IrmaoMiau, Estado> estados = new IdentityHashMap<>();

    /**
     * Um tick logico (depois do movimento e do combate).
     *
     * @param arrastando o GamePanel arrastou a dupla neste tick
     */
    public void atualizar(IrmaoMiau l, SistemaCombate sistema, boolean arrastando, long tick) {
        boolean naPegada = sistema != null && sistema.isInteracaoEstabelecida()
                && !sistema.isAnimacaoCombateAtiva()
                && (sistema.getAplicadorInteracao() == l || sistema.getReceptorInteracao() == l);
        int[] atual = l.copiarPoseCombateExterna();
        if (!naPegada || atual == null) {
            estados.remove(l);
            return;
        }
        Estado e = estados.get(l);
        if (e == null) {
            e = new Estado();
            e.ultimoX = l.getX();
            e.restanteAnterior = l.getEfeitos().getTicksRestantes(EfeitosTemporarios.Tipo.DESEQUILIBRIO);
            estados.put(l, e);
        }
        if (e.ultimaEscrita == null || !Arrays.equals(atual, e.ultimaEscrita)) {
            e.base = atual;                              // pose nova do motor: recomeca dela
        }

        // Arrasto: fase pela distancia realmente percorrida.
        if (arrastando) {
            e.distancia += Math.abs(l.getX() - e.ultimoX);
            e.fase = Math.floorMod(e.distancia / PIXELS_POR_FASE, 4);
        } else {
            e.fase = -1;
        }
        e.ultimoX = l.getX();

        // Desequilibrio: o efeito do motor comecou (ou recomecou) neste tick.
        int restante = l.getEfeitos().getTicksRestantes(EfeitosTemporarios.Tipo.DESEQUILIBRIO);
        if (restante > e.restanteAnterior && sistema.getReceptorInteracao() == l) {
            e.inicioReacao = tick;
        }
        e.restanteAnterior = restante;
        e.intensidadeReacao = envelope(e.inicioReacao == Long.MIN_VALUE ? -1 : tick - e.inicioReacao);

        int[] pose = compor(e.base, e.fase, e.intensidadeReacao);
        if (!Arrays.equals(pose, atual)) {
            l.aplicarPoseCombateExterna(pose, l.getDeslocamentoVisualCombateX(), l.getDeslocamentoVisualCombateY());
        }
        e.ultimaEscrita = pose;
    }

    /** 0..1: sobe em 6 ticks, segura ate 12, volta a 0 em 24. Fora disso, 0. */
    static double envelope(long t) {
        if (t < 0 || t >= DURACAO_REACAO_TICKS) {
            return 0.0;
        }
        if (t < 6) {
            return (t + 1) / 6.0;
        }
        if (t < 12) {
            return 1.0;
        }
        return (DURACAO_REACAO_TICKS - t) / (double) (DURACAO_REACAO_TICKS - 12);
    }

    /** Base + passo do arrasto + reacao do desequilibrio (copia nova). */
    static int[] compor(int[] base, int fase, double reacao) {
        int[] p = Arrays.copyOf(base, base.length);
        if (fase == 1) {
            p[JOEL_ESQ_X] += PASSO;
            p[PE_ESQ_X] += PASSO;
            p[PE_ESQ_Y] -= ELEVACAO_PE;
        } else if (fase == 3) {
            p[JOEL_DIR_X] += PASSO;
            p[PE_DIR_X] += PASSO;
            p[PE_DIR_Y] -= ELEVACAO_PE;
        }
        if (reacao > 0) {
            p[ROT_PEITO] += (int) Math.round(INCLINACAO_PEITO * reacao);
            p[ROT_CINTURA] += (int) Math.round(INCLINACAO_CINTURA * reacao);
            p[ROT_CABECA] -= (int) Math.round(INCLINACAO_CINTURA * reacao);
            p[JOEL_DIR_X] += (int) Math.round(PASSO * reacao);
        }
        return p;
    }

    /** Fase do passo do arrasto (-1 = sem passo agora). */
    public int faseArrasto(IrmaoMiau l) {
        Estado e = estados.get(l);
        return e == null ? -1 : e.fase;
    }

    /** Reacao ao desequilibrio acontecendo agora (0 = nenhuma). */
    public double intensidadeReacao(IrmaoMiau l) {
        Estado e = estados.get(l);
        return e == null ? 0.0 : e.intensidadeReacao;
    }

    /** Pose que o motor deixou (sem a camada), para testes. */
    public int[] poseBase(IrmaoMiau l) {
        Estado e = estados.get(l);
        return e == null || e.base == null ? null : e.base.clone();
    }

    public void esquecer(IrmaoMiau l) {
        estados.remove(l);
    }
}
