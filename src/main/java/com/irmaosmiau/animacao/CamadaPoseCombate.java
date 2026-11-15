package com.irmaosmiau.animacao;

import com.irmaosmiau.combate.ModoAnimacoesPegada;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.entities.IrmaoMiau;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Entrega 5J: camada ADITIVA sobre a pose que o motor deixou na pegada
 * (PEGADA_ARRASTO e PEGADA_DESEQUILIBRIO).
 *
 * - Arrasto: passos curtos e alternados nas pernas dos DOIS lutadores, so
 *   enquanto a dupla esta sendo arrastada; a fase avanca pela distancia
 *   REAL percorrida (um passo a cada {@link #PIXELS_POR_FASE} px; 5N: em X e
 *   em profundidade), entao para quando a dupla para e continua de onde
 *   estava quando ela volta a andar.
 * - Desequilibrio: quando o motor aplica o efeito DESEQUILIBRIO no receptor
 *   (so acontece com sucesso), o tronco inclina para a frente e um joelho
 *   ajusta o apoio por {@link #DURACAO_REACAO_TICKS} ticks, e volta.
 *
 * Entrega 5N (modo EXPERIMENTAL, o padrao do jogo; o F6 volta ao autoral e
 * desliga o que e da 5N para comparar):
 * - Reacao COMPARTILHADA do receptor ({@link BibliotecaReacoesReceptor}):
 *   durante a entrada do aplicador sobe ate o CONTATO no instante certo e se
 *   acomoda; com a pegada estabelecida fica na pose SUSTENTADA (respirando
 *   devagar) enquanto o motor disser que a pegada existe.
 * - Direcao do puxao: arrastado, o receptor inclina para a frente (puxado) ou
 *   para tras (empurrado) e vira a cabeca quando a dupla muda de linha.
 * - Reforco: quando a firmeza sobe (reforco confirmado), o aplicador ajusta o
 *   tronco e o receptor acusa a pressao por um instante.
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
    /** 5N: inclinacao do receptor pela direcao do arrasto (graus). */
    public static final int INCLINACAO_PUXAO = 3;
    /** 5N: subida da firmeza que conta como reforco. */
    public static final double SUBIDA_REFORCO = 5.0;
    /** Duracao nominal de um tick logico (GamePanel). */
    public static final double MS_POR_TICK = 16.0;

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

    /** 5N: o que a reacao do receptor esta mostrando agora (HUD tecnico e testes). */
    public enum FaseReceptor { NENHUMA, CONTATO, SUSTENTADA, SEM_REACAO }

    /** 5N: ultimo delta do receptor (para a liberacao continuar dele, sem salto). */
    public record SnapshotReceptor(String tecnica, int[] delta, int[] liberacao) {
    }

    private static final class Estado {
        int[] base;
        int[] ultimaEscrita;
        int ultimoX;
        int ultimoY;
        int distancia;
        int restanteAnterior;
        long inicioReacao = Long.MIN_VALUE;
        int fase = -1;
        double intensidadeReacao;
        // Entrega 5L: receptor forcando / ROMPER resistido (a pegada continua).
        long ultimaSequenciaDefesa;
        long inicioDefesaFalhou = Long.MIN_VALUE;
        double intensidadeForca;
        // Entrega 5N
        long tickEstabelecida = Long.MIN_VALUE;
        String tecnica;
        int[] deltaReceptor;
        int[] liberacao;
        FaseReceptor faseReceptor = FaseReceptor.NENHUMA;
        double firmezaAnterior = -1;
        long inicioReforco = Long.MIN_VALUE;
        int puxaoX;
        int puxaoY;
        double intensidadeReforco;
    }

    private final Map<IrmaoMiau, Estado> estados = new IdentityHashMap<>();
    private final BibliotecaReacoesReceptor biblioteca;

    public CamadaPoseCombate() {
        this(BibliotecaReacoesReceptor.padrao());
    }

    /** 5N: com uma biblioteca de reacoes do receptor propria (testes, personagens futuros). */
    public CamadaPoseCombate(BibliotecaReacoesReceptor biblioteca) {
        this.biblioteca = biblioteca == null ? new BibliotecaReacoesReceptor() : biblioteca;
    }

    public BibliotecaReacoesReceptor getBiblioteca() {
        return biblioteca;
    }

    /**
     * Um tick logico (depois do movimento e do combate).
     *
     * @param arrastando o GamePanel arrastou a dupla neste tick
     */
    public void atualizar(IrmaoMiau l, SistemaCombate sistema, boolean arrastando, long tick) {
        boolean participa = sistema != null && sistema.estaEmInteracao()
                && (sistema.getAplicadorInteracao() == l || sistema.getReceptorInteracao() == l);
        boolean receptor = participa && sistema.getReceptorInteracao() == l;
        boolean estabelecida = participa && sistema.isInteracaoEstabelecida() && !sistema.isAnimacaoCombateAtiva();
        boolean experimental = ModoAnimacoesPegada.atual() == ModoAnimacoesPegada.EXPERIMENTAL;
        // 5N: na ENTRADA so o receptor reage (o aplicador e a animacao autoral/experimental dele).
        boolean entrada = experimental && receptor && sistema.isAnimacaoCombateAtiva()
                && !sistema.isInteracaoEstabelecida();
        int[] atual = l.copiarPoseCombateExterna();
        if (!(estabelecida || entrada) || atual == null) {
            estados.remove(l);
            return;
        }
        Estado e = estados.get(l);
        if (e == null) {
            e = new Estado();
            e.ultimoX = l.getX();
            e.ultimoY = l.getY();
            e.restanteAnterior = l.getEfeitos().getTicksRestantes(EfeitosTemporarios.Tipo.DESEQUILIBRIO);
            estados.put(l, e);
        }
        if (e.ultimaEscrita == null || !Arrays.equals(atual, e.ultimaEscrita)) {
            e.base = atual;                              // pose nova do motor: recomeca dela
        }
        if (estabelecida && e.tickEstabelecida == Long.MIN_VALUE) {
            e.tickEstabelecida = tick;                   // relogio da pose sustentada (nao recomeca)
        }
        l.registrarAtividadeDeCombate();                 // 5N: o taunt nao aparece logo depois

        // Arrasto: fase pela distancia realmente percorrida (5N: X e profundidade).
        int dx = l.getX() - e.ultimoX;
        int dy = l.getY() - e.ultimoY;
        if (arrastando && estabelecida) {
            e.distancia += Math.abs(dx) + Math.abs(dy);
            e.fase = Math.floorMod(e.distancia / PIXELS_POR_FASE, 4);
        } else {
            e.fase = -1;
        }
        e.puxaoX = arrastando && dx != 0 ? Integer.signum(dx) : 0;
        e.puxaoY = arrastando && dy != 0 ? Integer.signum(dy) : 0;
        e.ultimoX = l.getX();
        e.ultimoY = l.getY();

        // Desequilibrio: o efeito do motor comecou (ou recomecou) neste tick.
        int restante = l.getEfeitos().getTicksRestantes(EfeitosTemporarios.Tipo.DESEQUILIBRIO);
        if (restante > e.restanteAnterior && receptor) {
            e.inicioReacao = tick;
        }
        e.restanteAnterior = restante;
        e.intensidadeReacao = envelope(e.inicioReacao == Long.MIN_VALUE ? -1 : tick - e.inicioReacao);

        // Entrega 5L: o receptor que forca puxa o tronco para tras (sem soltar); ROMPER resistido
        // da um puxao mais forte e curto. So o que o motor confirmou.
        com.irmaosmiau.combate.EventoDefesa ev = sistema.getUltimoEventoDefesa();
        if (ev != null && ev.sequencia() > e.ultimaSequenciaDefesa) {
            e.ultimaSequenciaDefesa = ev.sequencia();
            if (ev.tipo() == com.irmaosmiau.combate.EventoDefesa.Tipo.ROMPIMENTO_FALHOU && ev.defensor() == l) {
                e.inicioDefesaFalhou = tick;
            }
        }
        boolean forcando = receptor && sistema.getTicksResistindo() > 0;
        double falhou = e.inicioDefesaFalhou == Long.MIN_VALUE ? 0 : envelope(tick - e.inicioDefesaFalhou);
        e.intensidadeForca = estabelecida ? Math.max(forcando ? 0.5 : 0.0, falhou) : 0.0;

        // 5N: reforco confirmado (a firmeza subiu de uma vez).
        double firmeza = sistema.getFirmeza();
        if (estabelecida && e.firmezaAnterior >= 0 && firmeza - e.firmezaAnterior >= SUBIDA_REFORCO) {
            e.inicioReforco = tick;
        }
        e.firmezaAnterior = estabelecida ? firmeza : -1;      // a entrada nao conta como reforco

        int[] pose = compor(e.base, e.fase, e.intensidadeReacao);
        if (e.intensidadeForca > 0) {
            pose[ROT_PEITO] -= (int) Math.round(INCLINACAO_PEITO * e.intensidadeForca);
            pose[ROT_CINTURA] -= (int) Math.round(INCLINACAO_CINTURA * e.intensidadeForca);
            pose[ROT_CABECA] -= (int) Math.round(3 * e.intensidadeForca);
        }
        if (experimental) {
            aplicarReacoes5N(l, sistema, e, pose, receptor, entrada, tick);
        }
        if (!Arrays.equals(pose, atual)) {
            l.aplicarPoseCombateExterna(pose, l.getDeslocamentoVisualCombateX(), l.getDeslocamentoVisualCombateY());
        }
        e.ultimaEscrita = pose;
    }

    /** 5N: reacao do receptor (contato/sustentada), direcao do puxao e reforco, somados a pose. */
    private void aplicarReacoes5N(IrmaoMiau l, SistemaCombate sistema, Estado e, int[] pose, boolean receptor,
            boolean entrada, long tick) {
        double reforco = envelope(e.inicioReforco == Long.MIN_VALUE ? -1 : tick - e.inicioReforco);
        e.intensidadeReforco = reforco;
        if (!receptor) {
            e.faseReceptor = FaseReceptor.NENHUMA;
            if (reforco > 0) {
                pose[ROT_PEITO] -= (int) Math.round(3 * reforco);     // aplicador ajusta a firmeza
                pose[ROT_CINTURA] -= (int) Math.round(2 * reforco);
            }
            return;
        }
        String tecnica = sistema.getIdAcaoInteracao();
        ReacaoReceptor r = biblioteca.buscar(tecnica).orElse(null);
        int[] delta = null;
        if (r != null) {
            int[] bruto = entrada
                    ? r.deltaEntrada(sistema.getProgressoAnimacaoCombate())
                    : r.deltaSustentada((long) ((tick - e.tickEstabelecida) * MS_POR_TICK));
            delta = AdaptadorRigReacao.adaptar(bruto, l, r);
        }
        if (delta == null) {
            e.faseReceptor = FaseReceptor.SEM_REACAO;            // fallback: a pose do motor
            e.deltaReceptor = null;
            e.liberacao = null;
            e.tecnica = tecnica;
            return;
        }
        e.faseReceptor = entrada ? FaseReceptor.CONTATO : FaseReceptor.SUSTENTADA;
        e.tecnica = tecnica;
        e.deltaReceptor = delta.clone();
        e.liberacao = AdaptadorRigReacao.adaptar(r.deltaLiberacao(), l, r);
        for (int i = 4; i < pose.length; i++) {
            pose[i] += delta[i];
        }
        if (e.puxaoX != 0) {
            boolean puxado = (e.puxaoX > 0) == l.isViradoParaDireita();   // a dupla vai para onde ele olha
            pose[ROT_PEITO] += puxado ? INCLINACAO_PUXAO : -INCLINACAO_PUXAO;
        }
        if (e.puxaoY != 0) {
            pose[ROT_CABECA] += 2 * e.puxaoY;
        }
        if (reforco > 0) {
            pose[ROT_PEITO] += (int) Math.round(4 * reforco);         // receptor acusa a pressao
            pose[ROT_CABECA] += (int) Math.round(2 * reforco);
        }
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

    /** Entrega 5L: forca do receptor preso (0 = nenhuma; 0,5 forcando; ate 1 no ROMPER resistido). */
    public double intensidadeForca(IrmaoMiau l) {
        Estado e = estados.get(l);
        return e == null ? 0.0 : e.intensidadeForca;
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

    /** 5N: reacao ao reforco confirmado acontecendo agora (0 = nenhuma). */
    public double intensidadeReforco(IrmaoMiau l) {
        Estado e = estados.get(l);
        return e == null ? 0.0 : e.intensidadeReforco;
    }

    /** 5N: o que a reacao do receptor mostra agora. */
    public FaseReceptor faseReceptor(IrmaoMiau l) {
        Estado e = estados.get(l);
        return e == null ? FaseReceptor.NENHUMA : e.faseReceptor;
    }

    /** 5N: delta da reacao do receptor aplicado neste tick (null = nenhum). */
    public int[] deltaReceptor(IrmaoMiau l) {
        Estado e = estados.get(l);
        return e == null || e.deltaReceptor == null ? null : e.deltaReceptor.clone();
    }

    /** 5N: ticks desde que a pegada foi estabelecida (para este lutador); -1 se nao. */
    public long ticksSustentada(IrmaoMiau l, long tick) {
        Estado e = estados.get(l);
        return e == null || e.tickEstabelecida == Long.MIN_VALUE ? -1 : tick - e.tickEstabelecida;
    }

    /** 5N: a ultima reacao do receptor (a liberacao continua dela). */
    public SnapshotReceptor snapshotReceptor(IrmaoMiau l) {
        Estado e = estados.get(l);
        if (e == null || e.deltaReceptor == null) {
            return null;
        }
        return new SnapshotReceptor(e.tecnica, e.deltaReceptor.clone(),
                e.liberacao == null ? new int[e.deltaReceptor.length] : e.liberacao.clone());
    }

    /** 5N: a dupla esta sendo levada (direcao em X e em profundidade, -1/0/+1). */
    public int[] direcaoPuxao(IrmaoMiau l) {
        Estado e = estados.get(l);
        return e == null ? new int[] {0, 0} : new int[] {e.puxaoX, e.puxaoY};
    }

    public void esquecer(IrmaoMiau l) {
        estados.remove(l);
    }
}
