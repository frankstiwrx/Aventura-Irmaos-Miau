package com.irmaosmiau.animacao;

/**
 * Entrega 5Q.7: integridade dos bracos quando uma animacao de tecnica soma deslocamentos sobre
 * uma pose que NAO e a de repouso (a pose da pegada em pe).
 *
 * As animacoes proprias (AnimacoesTecnicas) somam deslocamentos de cotovelo e mao pensados para
 * o braco em repouso. Na pegada o braco ja esta estendido ate o kimono do outro; o mesmo
 * deslocamento estica o antebraco (ate ~1,6x) ou encolhe o outro (~0,3x) no meio da reacao da
 * rasteira e da derrubada forcada que falham. Aqui o braco GIRA em torno do ombro e dobra/estende
 * o cotovelo (cinematica de dois segmentos) com os comprimentos que ele tinha ANTES da animacao:
 * a mao vai na direcao pedida, mas nenhum segmento muda de tamanho e o giro e limitado.
 *
 * So desenho: nada aqui muda regras, posicoes ou o motor.
 */
public final class IntegridadeMembros {

    /** Giro maximo do braco em torno do ombro (graus) numa reacao. */
    public static final double GIRO_MAXIMO_GRAUS = 30.0;
    /** Quanto a mao pode se aproximar do ombro (fracao do alcance de antes): dobrar o cotovelo. */
    public static final double ALCANCE_MINIMO = 0.55;

    /** Indices do rig (cotovelo x, mao x) do braco de tras e do braco da frente. */
    private static final int[][] INDICES = {{14, 16}, {18, 20}};

    private IntegridadeMembros() {
    }

    /**
     * Corrige os bracos de {@code pose} (in-place) para terem os comprimentos de {@code antes}.
     *
     * @param repouso {ombro x, y, cotovelo x, y, mao x, y} de cada braco (IrmaoMiau.getBracosEmRepouso)
     */
    public static void preservarBracos(int[] antes, int[] pose, int[][] repouso) {
        if (antes == null || pose == null || repouso == null || antes.length < 22 || pose.length < 22) {
            return;
        }
        for (int b = 0; b < 2 && b < repouso.length; b++) {
            preservar(antes, pose, repouso[b], INDICES[b][0], INDICES[b][1]);
        }
    }

    private static void preservar(int[] antes, int[] pose, int[] r, int ic, int im) {
        double sx = r[0];
        double sy = r[1];
        double e0x = r[2] + antes[ic];
        double e0y = r[3] + antes[ic + 1];
        double h0x = r[4] + antes[im];
        double h0y = r[5] + antes[im + 1];
        double htx = r[4] + pose[im];
        double hty = r[5] + pose[im + 1];
        double l1 = Math.hypot(e0x - sx, e0y - sy);
        double l2 = Math.hypot(h0x - e0x, h0y - e0y);
        double d0 = Math.hypot(h0x - sx, h0y - sy);
        if (l1 < 1 || l2 < 1 || d0 < 1) {
            return;
        }
        // direcao: a mao vai para onde a animacao pede, com o giro limitado
        double a0 = Math.atan2(h0y - sy, h0x - sx);
        double at = Math.hypot(htx - sx, hty - sy) < 1 ? a0 : Math.atan2(hty - sy, htx - sx);
        double giro = normalizar(at - a0);
        double lim = Math.toRadians(GIRO_MAXIMO_GRAUS);
        giro = Math.max(-lim, Math.min(lim, giro));
        double a = a0 + giro;
        // alcance: dobrar o cotovelo pode; esticar alem do que o braco mede, nao
        double dt = Math.hypot(htx - sx, hty - sy);
        double max = Math.min(l1 + l2 - 0.5, Math.max(d0, dt));
        double min = Math.max(Math.abs(l1 - l2) + 0.5, d0 * ALCANCE_MINIMO);
        double d = Math.max(Math.min(min, max), Math.min(max, dt));
        double hx = sx + d * Math.cos(a);
        double hy = sy + d * Math.sin(a);
        // cotovelo: dois segmentos com os comprimentos de antes, dobrando para o mesmo lado de antes
        double cruz = (h0x - sx) * (e0y - sy) - (h0y - sy) * (e0x - sx);
        double lado = cruz >= 0 ? 1 : -1;
        double cos = (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d);
        double ang = Math.acos(Math.max(-1, Math.min(1, cos)));
        double ea = a + lado * ang;
        double ex = sx + l1 * Math.cos(ea);
        double ey = sy + l1 * Math.sin(ea);
        pose[ic] = (int) Math.round(ex - r[2]);
        pose[ic + 1] = (int) Math.round(ey - r[3]);
        pose[im] = (int) Math.round(hx - r[4]);
        pose[im + 1] = (int) Math.round(hy - r[5]);
    }

    private static double normalizar(double a) {
        while (a > Math.PI) {
            a -= 2 * Math.PI;
        }
        while (a < -Math.PI) {
            a += 2 * Math.PI;
        }
        return a;
    }

    /**
     * Comprimento (px locais) do braco e do antebraco {tras braco, tras antebraco, frente braco,
     * frente antebraco} de uma pose (testes e diagnostico).
     */
    public static double[] comprimentos(int[] pose, int[][] repouso) {
        double[] c = new double[4];
        for (int b = 0; b < 2; b++) {
            int[] r = repouso[b];
            double ex = r[2] + pose[INDICES[b][0]];
            double ey = r[3] + pose[INDICES[b][0] + 1];
            double hx = r[4] + pose[INDICES[b][1]];
            double hy = r[5] + pose[INDICES[b][1] + 1];
            c[2 * b] = Math.hypot(ex - r[0], ey - r[1]);
            c[2 * b + 1] = Math.hypot(hx - ex, hy - ey);
        }
        return c;
    }
}
