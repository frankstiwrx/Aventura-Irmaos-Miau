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

    /** 5Q.6: inclinacao do tronco (graus) para a frente; indo para tras inclina para tras. */
    public static final int INCLINACAO = 12;
    /** 5Q.6: indo para tras o tronco inclina para tras (menos que a frente: o peso fica no quadril). */
    public static final int INCLINACAO_TRAS = -8;

    private final Map<IrmaoMiau, int[]> trilha = new IdentityHashMap<>();

    /** Pose da passada na fase f (0-1), indo para a frente. */
    public static int[] pose(IrmaoMiau l, double f) {
        return pose(l, f, true);
    }

    /**
     * Pose da passada na fase f (0-1). {@code paraFrente}: para onde o lutador OLHA (relativo a
     * orientacao, nao a esquerda/direita da tela). Antes o tronco inclinava para a frente mesmo
     * andando de costas.
     */
    public static int[] pose(IrmaoMiau l, double f, boolean paraFrente) {
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
        p[9] += paraFrente ? INCLINACAO : INCLINACAO_TRAS;      // 5Q.6: frente = para a frente; tras = para tras
        p[11] += paraFrente ? -4 : 3;
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
        int[] t = trilha.computeIfAbsent(l, k -> new int[] {k.getX(), 1});
        int dx = l.getX() - t[0];
        if (dx != 0) {
            t[1] = (dx > 0) == l.isViradoParaDireita() ? 1 : -1;      // sentido relativo a para onde olha
        }
        t[0] = l.getX();
        int[] p = pose(l, f, t[1] > 0);
        double quique = -1.5 * Math.abs(Math.sin(2 * Math.PI * f)) * l.getEscala();
        l.aplicarPoseCombateExterna(p, 0.0, PosturaAjoelhada.quedaPx(l) + quique);
        aplicadas.put(l, p);
        return "joelhos_acelerado";
    }

    /** 5Q.6: o ultimo sentido do deslocamento acelerado (true = para onde olha). */
    public boolean paraFrente(IrmaoMiau l) {
        int[] t = trilha.get(l);
        return t == null || t[1] > 0;
    }

    public void liberar(IrmaoMiau l) {
        trilha.remove(l);
        int[] nossa = aplicadas.remove(l);
        if (nossa != null && l.isPoseCombateExternaAtiva() && Arrays.equals(nossa, l.copiarPoseCombateExterna())) {
            l.limparPoseCombateExterna();
        }
    }
}
