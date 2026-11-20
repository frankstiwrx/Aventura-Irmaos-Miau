package com.irmaosmiau.animacao;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.Posicao;

import java.util.Map;

/**
 * Entrega 5Q.4: pernas de QUEM ESTA POR BAIXO em cada guarda (antes as nove guardas
 * usavam as mesmas pernas da pose deitado). Procedural e provisorio, como o resto
 * do AnimadorSolo: pronto para virar pose do MiauStudio por guarda no futuro.
 *
 * As pernas sao montadas por ANGULO a partir do quadril, com o comprimento real da
 * coxa e da canela de cada irmao (as medidas do desenho em IrmaoMiauBranco/Preto):
 * nenhuma guarda estica ou encolhe a perna. Angulos em graus no quadro local do
 * corpo: 0 = perna esticada no sentido do corpo (para os pes); 90 = para a FRENTE do
 * lutador (deitado, rotacao global -88: para cima). Negativo = para tras.
 */
public final class PernasGuarda {

    /** Angulos (coxa, canela) da perna da frente e da de tras. */
    public record Angulos(double coxaFrente, double canelaFrente, double coxaTras, double canelaTras) {
    }

    // quadril, joelho e pe de referencia (x, y) da perna de tras (ESQ) e da frente (DIR), por irmao
    private static final double[][] BRANCO = {{22, 100}, {10, 136}, {0, 174}, {52, 100}, {66, 137}, {82, 171}};
    private static final double[][] PRETO = {{24, 97}, {15, 132}, {4, 171}, {50, 97}, {59, 132}, {72, 171}};

    static final Map<Posicao, Angulos> GUARDAS = Map.of(
            // fechada: joelhos medios, canelas cruzadas por tras da cintura do outro
            Posicao.GUARDA, new Angulos(55, -35, 48, -25),
            // aberta: pes no quadril do outro (canelas quase horizontais)
            Posicao.GUARDA_ABERTA, new Angulos(78, 18, 64, 8),
            // meia: a de baixo presa (quase esticada), a de tras vira o escudo de joelho
            Posicao.MEIA_GUARDA, new Angulos(18, -8, 85, 25),
            // borboleta: joelhos bem altos e abertos, pes de gancho por baixo das coxas
            Posicao.GUARDA_BORBOLETA, new Angulos(95, -5, 88, -12),
            // De La Riva: a da frente enrosca por fora da perna do outro; a de tras no quadril
            Posicao.DE_LA_RIVA, new Angulos(35, 55, 70, 12),
            // laco: a da frente sobe e enlaca o braco (canela alta); a de tras no quadril
            Posicao.GUARDA_LACO, new Angulos(82, 70, 58, 6),
            // aranha: os dois pes altos nos biceps
            Posicao.GUARDA_ARANHA, new Angulos(80, 50, 72, 42),
            // single-leg X: a da frente baixa entre as pernas do outro; a de tras alta no quadril
            Posicao.SINGLE_LEG_X, new Angulos(10, 4, 80, 32),
            // ashi garami: as duas baixas, enroscadas numa perna so
            Posicao.ASHI_GARAMI, new Angulos(26, -28, 14, 22));

    private PernasGuarda() {
    }

    public static boolean temPernasProprias(Posicao p) {
        return GUARDAS.containsKey(p);
    }

    /** Aplica as pernas da guarda na pose (indices 22-29 do rig). */
    public static void aplicar(int[] pose, IrmaoMiau l, Posicao p) {
        Angulos a = GUARDAS.get(p);
        if (a == null || pose.length < 30) {
            return;
        }
        double[][] r = l instanceof IrmaoMiauPreto ? PRETO : BRANCO;
        perna(pose, r[0], r[1], r[2], 22, a.coxaTras(), a.canelaTras());
        perna(pose, r[3], r[4], r[5], 26, a.coxaFrente(), a.canelaFrente());
    }

    private static void perna(int[] pose, double[] quadril, double[] joelho0, double[] pe0, int idx,
            double coxa, double canela) {
        double lc = Math.hypot(joelho0[0] - quadril[0], joelho0[1] - quadril[1]);
        double ls = Math.hypot(pe0[0] - joelho0[0], pe0[1] - joelho0[1]);
        double jx = quadril[0] + lc * Math.sin(Math.toRadians(coxa));
        double jy = quadril[1] + lc * Math.cos(Math.toRadians(coxa));
        double px = jx + ls * Math.sin(Math.toRadians(canela));
        double py = jy + ls * Math.cos(Math.toRadians(canela));
        pose[idx] = (int) Math.round(jx - joelho0[0]);
        pose[idx + 1] = (int) Math.round(jy - joelho0[1]);
        pose[idx + 2] = (int) Math.round(px - pe0[0]);
        pose[idx + 3] = (int) Math.round(py - pe0[1]);
    }

    /** Testes: comprimento (coxa, canela) desenhado pela pose, no quadro local. */
    static double[] comprimentos(int[] pose, boolean preto, boolean frente) {
        double[][] r = preto ? PRETO : BRANCO;
        int o = frente ? 3 : 0;
        int idx = frente ? 26 : 22;
        double jx = r[o + 1][0] + pose[idx];
        double jy = r[o + 1][1] + pose[idx + 1];
        double px = r[o + 2][0] + pose[idx + 2];
        double py = r[o + 2][1] + pose[idx + 3];
        return new double[] {Math.hypot(jx - r[o][0], jy - r[o][1]), Math.hypot(px - jx, py - jy)};
    }

    static double[] comprimentosReferencia(boolean preto, boolean frente) {
        return comprimentos(new int[30], preto, frente);
    }
}
