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

    /**
     * 5Q.5: levantada tecnica em estagios: senta e apoia (0-0,4), tira o quadril e passa a
     * perna (0,4-0,75), sobe e assenta (0,75-1). Devolve k (0-1) da interpolacao base.
     */
    static double curvaLevantada(double x) {
        double u = Math.max(0, Math.min(1, x));
        if (u < 0.4) {
            return 0.35 * suave(u / 0.4);
        }
        if (u < 0.75) {
            return 0.35 + 0.35 * suave((u - 0.4) / 0.35);
        }
        return 0.7 + 0.3 * suave((u - 0.75) / 0.25);
    }

    private static double suave(double u) {
        return u * u * (3 - 2 * u);
    }

    /** Envelope 0 -> 1 rapido (1 tick) e de volta a 0 entre a e b. */
    private static double envelope(double x, double a, double b) {
        if (x <= 0 || x >= b) {
            return 0;
        }
        double sobe = Math.min(1, x * 30);
        double desce = x <= a ? 1 : 1 - suave((x - a) / (b - a));
        return sobe * desce;
    }

    /** Pose da transicao no progresso x (0..1). */
    public Pose quadro(IrmaoMiau l, TransicaoPostura t, double x) {
        Pose de = pose(l, t.getOrigem());
        Pose para = pose(l, t.getDestino());
        double k = t == TransicaoPostura.LEVANTADA_TECNICA ? curvaLevantada(x) : curva(t, x);
        int[] p = Arrays.copyOf(para.valores(), para.valores().length);
        for (int i = 4; i < p.length; i++) {
            p[i] = (int) Math.round(de.valores()[i] + (para.valores()[i] - de.valores()[i]) * k);
        }
        double dy = de.dy() + (para.dy() - de.dy()) * k;
        if (t == TransicaoPostura.LEVANTADA_TECNICA) {
            // 5Q.5: mao de tras apoiada e pe da frente plantado (o mesmo apoio do fim da janela)
            double apoio = envelope(x, 0.45, 0.85);
            p[10] += (int) Math.round(16 * apoio);                      // ja sentando
            p[MAO_ESQ_X] += (int) Math.round(-16 * apoio);
            p[MAO_ESQ_Y] += (int) Math.round(-22 * apoio);
            p[COT_ESQ_X] += (int) Math.round(-8 * apoio);
            p[26] += (int) Math.round(18 * apoio);
            p[27] += (int) Math.round(-12 * apoio);
            p[28] += (int) Math.round(12 * apoio);
            // passa a perna de tras para tras com o quadril no alto
            double passa = Math.sin(Math.PI * Math.max(0, Math.min(1, (x - 0.35) / 0.5)));
            if (x > 0.35 && x < 0.85) {
                p[22] += (int) Math.round(-14 * passa);
                p[24] += (int) Math.round(-26 * passa);
                p[25] += (int) Math.round(-10 * passa);
                p[MAO_DIR_X] += (int) Math.round(12 * passa);           // braco da frente protege o rosto
                p[MAO_DIR_Y] += (int) Math.round(-14 * passa);
            }
        }
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
