package com.irmaosmiau.animacao;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Entregas 5Q.7 / 5Q.7 corretiva: integridade do CORPO INTEIRO quando uma animacao de tecnica
 * soma deslocamentos sobre uma pose que nao e a de repouso (a pose da pegada em pe, a pose
 * deitada do solo livre, as posicoes de solo).
 *
 * As animacoes proprias (AnimacoesTecnicas) somam deslocamentos de juntas pensados para o corpo
 * em repouso. Sobre outra pose o mesmo deslocamento estica ou encolhe os segmentos (braco,
 * antebraco, coxa, canela) e, somado a outra camada, tira o tronco de cima do quadril. Aqui:
 * - BRACOS e PERNAS giram na raiz (ombro/quadril) e dobram a junta do meio (cinematica de dois
 *   segmentos) com os comprimentos que tinham ANTES da animacao: a mao/o pe vai na direcao pedida,
 *   nenhum segmento muda de tamanho e o giro e limitado;
 * - TRONCO: a animacao nao desloca o tronco para fora do quadril (indices 7/8 ficam os de antes) e a
 *   torcao entre tronco e cintura e a inclinacao total sao limitadas;
 * - CINTURA: o deslocamento do quadril e limitado (o corpo nao "parte" na faixa).
 *
 * So desenho: nada aqui muda regras, posicoes ou o motor.
 */
public final class IntegridadeMembros {

    /** Giro maximo do braco em torno do ombro (graus) numa reacao. */
    public static final double GIRO_MAXIMO_GRAUS = 30.0;
    /** Quanto a mao pode se aproximar do ombro (fracao do alcance de antes): dobrar o cotovelo. */
    public static final double ALCANCE_MINIMO = 0.55;
    /** 5Q.7 corretiva: giro maximo da perna em torno do quadril e quanto o pe pode recolher. */
    public static final double GIRO_MAXIMO_PERNA_GRAUS = 30.0;
    public static final double ALCANCE_MINIMO_PERNA = 0.6;
    /** 5Q.7 corretiva: limites do tronco e da cintura somados pela animacao (graus / px locais). */
    public static final int INCLINACAO_MAXIMA_TRONCO = 26;
    public static final int GIRO_MAXIMO_CINTURA = 14;
    public static final int TORCAO_MAXIMA_TRONCO_CINTURA = 22;
    public static final int DESLOCAMENTO_MAXIMO_CINTURA = 8;

    /** Indices do rig (cotovelo x, mao x) do braco de tras e do braco da frente. */
    private static final int[][] INDICES = {{14, 16}, {18, 20}};
    /** Indices do rig (joelho x, pe x) da perna de tras e da perna da frente. */
    private static final int[][] INDICES_PERNAS = {{22, 24}, {26, 28}};

    private IntegridadeMembros() {
    }

    /** 5Q.7 corretiva: corpo inteiro (bracos, pernas, tronco e cintura) com as medidas do lutador. */
    public static void preservarCorpo(int[] antes, int[] pose, IrmaoMiau l) {
        if (antes == null || pose == null || l == null || antes.length < 30 || pose.length < 30) {
            return;
        }
        preservarTronco(antes, pose);
        preservarBracos(antes, pose, l.getBracosEmRepouso());
        preservarPernas(antes, pose, l.getPernasEmRepouso());
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
            preservar(antes, pose, repouso[b], INDICES[b][0], INDICES[b][1], GIRO_MAXIMO_GRAUS, ALCANCE_MINIMO);
        }
    }

    /** 5Q.7 corretiva: o mesmo para as pernas (quadril fixo; coxa e canela com o tamanho de antes). */
    public static void preservarPernas(int[] antes, int[] pose, int[][] repouso) {
        if (antes == null || pose == null || repouso == null || antes.length < 30 || pose.length < 30) {
            return;
        }
        for (int b = 0; b < 2 && b < repouso.length; b++) {
            preservar(antes, pose, repouso[b], INDICES_PERNAS[b][0], INDICES_PERNAS[b][1],
                    GIRO_MAXIMO_PERNA_GRAUS, ALCANCE_MINIMO_PERNA);
        }
    }

    /**
     * 5Q.7 corretiva: o tronco continua apoiado no quadril. A animacao nao translada o tronco (7/8
     * voltam aos de antes), a cintura nao desliza mais que DESLOCAMENTO_MAXIMO_CINTURA, e as rotacoes
     * somadas ficam dentro dos limites (tronco, cintura e a diferenca entre os dois).
     */
    public static void preservarTronco(int[] antes, int[] pose) {
        pose[7] = antes[7];
        pose[8] = antes[8];
        pose[4] = antes[4] + limitar(pose[4] - antes[4], DESLOCAMENTO_MAXIMO_CINTURA);
        pose[5] = antes[5] + limitar(pose[5] - antes[5], DESLOCAMENTO_MAXIMO_CINTURA);
        int dCintura = limitar(pose[6] - antes[6], GIRO_MAXIMO_CINTURA);
        int dTronco = limitar(pose[9] - antes[9], INCLINACAO_MAXIMA_TRONCO);
        dTronco = dCintura + limitar(dTronco - dCintura, TORCAO_MAXIMA_TRONCO_CINTURA);
        pose[6] = antes[6] + dCintura;
        pose[9] = antes[9] + dTronco;
    }

    private static int limitar(int v, int max) {
        return Math.max(-max, Math.min(max, v));
    }

    private static void preservar(int[] antes, int[] pose, int[] r, int ic, int im, double giroMax, double alcanceMin) {
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
        // direcao: a extremidade vai para onde a animacao pede, com o giro limitado
        double a0 = Math.atan2(h0y - sy, h0x - sx);
        double at = Math.hypot(htx - sx, hty - sy) < 1 ? a0 : Math.atan2(hty - sy, htx - sx);
        double giro = normalizar(at - a0);
        double lim = Math.toRadians(giroMax);
        giro = Math.max(-lim, Math.min(lim, giro));
        double a = a0 + giro;
        // alcance: dobrar a junta pode; esticar alem do que o membro mede, nao
        double dt = Math.hypot(htx - sx, hty - sy);
        double max = Math.min(l1 + l2 - 0.5, Math.max(d0, dt));
        double min = Math.max(Math.abs(l1 - l2) + 0.5, d0 * alcanceMin);
        double d = Math.max(Math.min(min, max), Math.min(max, dt));
        double hx = sx + d * Math.cos(a);
        double hy = sy + d * Math.sin(a);
        // junta do meio: dois segmentos com os comprimentos de antes, dobrando para o mesmo lado de antes
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
        return segmentos(pose, repouso, INDICES);
    }

    /** 5Q.7 corretiva: {coxa de tras, canela de tras, coxa da frente, canela da frente} (px locais). */
    public static double[] comprimentosPernas(int[] pose, int[][] repouso) {
        return segmentos(pose, repouso, INDICES_PERNAS);
    }

    private static double[] segmentos(int[] pose, int[][] repouso, int[][] idx) {
        double[] c = new double[4];
        for (int b = 0; b < 2; b++) {
            int[] r = repouso[b];
            double ex = r[2] + pose[idx[b][0]];
            double ey = r[3] + pose[idx[b][0] + 1];
            double hx = r[4] + pose[idx[b][1]];
            double hy = r[5] + pose[idx[b][1] + 1];
            c[2 * b] = Math.hypot(ex - r[0], ey - r[1]);
            c[2 * b + 1] = Math.hypot(hx - ex, hy - ey);
        }
        return c;
    }

    /**
     * 5Q.7 corretiva: o quanto {@code pose} se afasta da integridade de {@code base} (diagnostico e
     * testes): maior variacao (px) de qualquer segmento de braco ou perna, deslocamento do tronco
     * para fora do quadril (px), deslocamento da cintura (px), torcao tronco-cintura e inclinacao do
     * tronco (graus), nessa ordem.
     */
    public static double[] desvios(int[] base, int[] pose, IrmaoMiau l) {
        double seg = 0;
        double[] b1 = comprimentos(base, l.getBracosEmRepouso());
        double[] p1 = comprimentos(pose, l.getBracosEmRepouso());
        double[] b2 = comprimentosPernas(base, l.getPernasEmRepouso());
        double[] p2 = comprimentosPernas(pose, l.getPernasEmRepouso());
        for (int k = 0; k < 4; k++) {
            seg = Math.max(seg, Math.max(Math.abs(b1[k] - p1[k]), Math.abs(b2[k] - p2[k])));
        }
        double tronco = Math.hypot(pose[7] - base[7], pose[8] - base[8]);
        double cintura = Math.hypot(pose[4] - base[4], pose[5] - base[5]);
        double torcao = Math.abs((pose[9] - base[9]) - (pose[6] - base[6]));
        double inclinacao = Math.abs(pose[9] - base[9]);
        return new double[] {seg, tronco, cintura, torcao, inclinacao};
    }
}
