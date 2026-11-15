package com.irmaosmiau.animacao;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.PosturaAjoelhada;
import com.irmaosmiau.entities.TransicaoPostura;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Entrega 5K: animacoes PROCEDURAIS das transicoes de postura (contratos
 * TRANSICAO_*), feitas com o proprio rig de 30 valores de cada irmao.
 *
 * Interpola a pose da postura de origem ate a de destino (pernas, tronco,
 * rotacao global e altura do corpo), com curvas diferentes:
 * - lentas: suave no inicio e no fim (smoothstep);
 * - rapidas: explosivas. Levantar rapido passa um pouco do ponto (o corpo
 *   "sobe" alem de em pe e assenta) com os bracos impulsionando; deitar
 *   rapido cai acelerando, bracos a frente para amortecer.
 *
 * So desenho: a postura logica ja mudou no IrmaoMiau; a posicao, a linha e a
 * colisao nunca sao alteradas. Usa o canal de pose externa sem disputar com o
 * combate (se o motor colocar uma pose, a transicao para de desenhar).
 */
public final class TransicoesPostura {

    /* Pose deitado de reserva (quadro 1 do MOVIMENTO_CHAO autoral de cada irmao). */
    private static final int[] DEITADO_BRANCO = {261, 4, 7, 1, 0, 0, 0, 0, 0, 18, -88, 34, 0, 0, 6, -11, -31, -16,
        -18, 24, -36, 58, 6, 8, 15, 15, -19, 5, -33, 15};
    private static final int[] DEITADO_PRETO = {507, 4, 7, 0, 0, 0, 0, 0, 0, 15, -89, 23, 0, 0, -2, -3, -44, 22,
        -4, 3, -47, 15, 0, 0, -3, -7, -11, 9, -43, 5};
    private static final double DEITADO_DY = 70;

    /* Indices do rig. */
    private static final int COT_ESQ_X = 14;
    private static final int MAO_ESQ_X = 16;
    private static final int MAO_ESQ_Y = 17;
    private static final int MAO_DIR_X = 20;
    private static final int MAO_DIR_Y = 21;

    /** Pose de uma postura + altura em que ela e desenhada. */
    public record Pose(int[] valores, double dy) {
    }

    private final Function<IrmaoMiau, Pose> deitado;
    private final Map<IrmaoMiau, int[]> aplicadas = new IdentityHashMap<>();

    /**
     * @param deitado pose deitado do lutador (o animador usa o JSON do
     *                movimento no chao); null = a reserva autoral embutida
     */
    public TransicoesPostura(Function<IrmaoMiau, Pose> deitado) {
        this.deitado = deitado;
    }

    /** Pose estatica de uma postura no rig do lutador. */
    public Pose pose(IrmaoMiau l, EstadoCorporal postura) {
        switch (postura) {
            case AJOELHADO:
                return new Pose(PosturaAjoelhada.poseParado(l), PosturaAjoelhada.quedaPx(l));
            case DEITADO:
                Pose p = deitado == null ? null : deitado.apply(l);
                if (p != null) {
                    return p;
                }
                int[] base = (l instanceof IrmaoMiauPreto ? DEITADO_PRETO : DEITADO_BRANCO).clone();
                base[3] = 1;
                return new Pose(base, DEITADO_DY);
            default:
                int[] emPe = new int[IrmaoMiau.TAMANHO_POSE_MIAUSTUDIO];
                emPe[2] = (int) Math.round(l.getEscala() * 10);
                emPe[3] = 1;
                return new Pose(emPe, 0);
        }
    }

    /** Curva de progresso (0..1, rapidas podem passar de 1 no meio). */
    static double curva(TransicaoPostura t, double x) {
        double u = Math.max(0, Math.min(1, x));
        if (!t.isRapida()) {
            return u * u * (3 - 2 * u);                     // smoothstep
        }
        if (t == TransicaoPostura.RAPIDA_DEITAR) {
            return u * u;                                   // cai acelerando
        }
        double c = 1.70158;                                 // ease-out-back: passa do ponto e assenta
        double v = u - 1;
        return 1 + (c + 1) * v * v * v + c * v * v;
    }

    /** Pose da transicao no progresso x (0..1). */
    public Pose quadro(IrmaoMiau l, TransicaoPostura t, double x) {
        Pose de = pose(l, t.getOrigem());
        Pose para = pose(l, t.getDestino());
        double k = curva(t, x);
        int[] p = Arrays.copyOf(para.valores(), para.valores().length);
        for (int i = 4; i < p.length; i++) {
            p[i] = (int) Math.round(de.valores()[i] + (para.valores()[i] - de.valores()[i]) * k);
        }
        double dy = de.dy() + (para.dy() - de.dy()) * k;
        if (t.isRapida()) {
            double impulso = Math.sin(Math.PI * Math.max(0, Math.min(1, x)));   // 0 nas pontas
            if (t == TransicaoPostura.RAPIDA_DEITAR) {
                p[MAO_DIR_X] += (int) Math.round(22 * impulso);              // bracos a frente
                p[MAO_ESQ_X] += (int) Math.round(18 * impulso);
                p[COT_ESQ_X] += (int) Math.round(8 * impulso);
            } else {
                p[MAO_DIR_Y] -= (int) Math.round(24 * impulso);              // bracos impulsionam
                p[MAO_ESQ_Y] -= (int) Math.round(18 * impulso);
            }
        }
        return new Pose(p, dy);
    }

    /**
     * Desenha o quadro atual da transicao do lutador (se a pose externa
     * estiver livre ou for nossa).
     *
     * @return false se outro sistema (combate) e dono da pose
     */
    public boolean aplicar(IrmaoMiau l) {
        TransicaoPostura t = l.getTransicaoAtiva();
        if (t == null) {
            liberar(l);
            return false;
        }
        int[] nossa = aplicadas.get(l);
        if (l.isPoseCombateExternaAtiva() && (nossa == null || !Arrays.equals(nossa, l.copiarPoseCombateExterna()))) {
            return false;
        }
        Pose q = quadro(l, t, l.getProgressoTransicao());
        l.aplicarPoseCombateExterna(q.valores(), 0.0, q.dy());
        aplicadas.put(l, q.valores());
        return true;
    }

    /** Devolve o desenho normal (so limpa a pose se ainda for a nossa). */
    public void liberar(IrmaoMiau l) {
        int[] nossa = aplicadas.remove(l);
        if (nossa != null && l.isPoseCombateExternaAtiva() && Arrays.equals(nossa, l.copiarPoseCombateExterna())) {
            l.limparPoseCombateExterna();
        }
    }
}
