package com.irmaosmiau.animacao;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.PosturaAjoelhada;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Entrega 5Q.5: deslocamento ACELERADO de joelhos (X segurado). Passada propria,
 * continua de joelhos (nao e a corrida em pe): os joelhos alternam com passo
 * maior, o tronco inclina para a frente e os bracos acompanham. Procedural, sobre a
 * pose ajoelhada parada de cada irmao (mesmas proporcoes do rig).
 */
public final class AnimadorJoelhosAcelerado {

    /** Ciclo de uma passada (ticks): dois joelhos. */
    static final int CICLO_TICKS = 18;
    /** Passo do joelho (unidades do rig): o deslocamento normal usa 4. */
    static final int PASSO = 12;

    private final Map<IrmaoMiau, int[]> aplicadas = new IdentityHashMap<>();

    public boolean quer(IrmaoMiau l) {
        return l.isAjoelhado() && l.isDeslocamentoAcelerado() && l.getTransicaoAtiva() == null;
    }

    /** Pose da passada na fase f (0-1). */
    public static int[] pose(IrmaoMiau l, double f) {
        int[] p = PosturaAjoelhada.poseParado(l);
        double s = Math.sin(2 * Math.PI * f);                    // + joelho de tras a frente, - o da frente
        int j = PosturaAjoelhada.JOEL_ESQ_X;
        // joelho de tras (22-25) e da frente (26-29): um avanca enquanto o outro recua, o pe acompanha
        p[j] += (int) Math.round(PASSO * s);
        p[j + 2] += (int) Math.round(PASSO * s * 0.8);
        p[j + 1] -= (int) Math.round(3 * Math.max(0, s));        // o que avanca sobe um pouco
        p[j + 4] -= (int) Math.round(PASSO * s);
        p[j + 6] -= (int) Math.round(PASSO * s * 0.8);
        p[j + 5] -= (int) Math.round(3 * Math.max(0, -s));
        p[9] += 12;                                              // tronco inclinado para a frente
        p[11] -= 4;
        // bracos alternados (mao de tras 16/17, mao da frente 20/21) com o cotovelo junto
        p[16] += (int) Math.round(-8 * s);
        p[14] += (int) Math.round(-4 * s);
        p[20] += (int) Math.round(8 * s);
        p[18] += (int) Math.round(4 * s);
        return p;
    }

    public String aplicar(IrmaoMiau l, long tick) {
        int[] nossa = aplicadas.get(l);
        if (l.isPoseCombateExternaAtiva() && (nossa == null || !Arrays.equals(nossa, l.copiarPoseCombateExterna()))) {
            return null;                                         // outro dono da pose
        }
        double f = (tick % CICLO_TICKS) / (double) CICLO_TICKS;
        int[] p = pose(l, f);
        double quique = -1.5 * Math.abs(Math.sin(2 * Math.PI * f)) * l.getEscala();
        l.aplicarPoseCombateExterna(p, 0.0, PosturaAjoelhada.quedaPx(l) + quique);
        aplicadas.put(l, p);
        return "joelhos_acelerado";
    }

    public void liberar(IrmaoMiau l) {
        int[] nossa = aplicadas.remove(l);
        if (nossa != null && l.isPoseCombateExternaAtiva() && Arrays.equals(nossa, l.copiarPoseCombateExterna())) {
            l.limparPoseCombateExterna();
        }
    }
}
