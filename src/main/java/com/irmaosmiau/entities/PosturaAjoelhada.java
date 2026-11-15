package com.irmaosmiau.entities;

import java.util.Arrays;

/**
 * Entrega 5J: a postura AJOELHADO no rig de 30 valores do MiauStudio.
 *
 * FALLBACK PROCEDURAL e provisorio: as pernas sao calculadas a partir dos
 * pivos de cada renderizador (o Branco e o Preto tem quadril, joelho e pe em
 * lugares diferentes), para que os joelhos toquem o tatame e as canelas
 * fiquem deitadas para tras. Tronco, cabeca e bracos nao mudam (ficam livres
 * para as pegadas). O corpo inteiro desce "queda" unidades do rig.
 *
 * Substituivel: o jogo usa primeiro as animacoes JSON
 * (individuais/branco_ajoelhado.json etc.); este codigo e o que o
 * renderizador desenha se nenhuma pose externa estiver ativa, e o adaptador
 * que mantem a dupla de joelhos durante a PEGADA_1.
 */
public final class PosturaAjoelhada {

    /** Indices das pernas no quadro de 30 valores (ver ConversorMiauStudio). */
    public static final int JOEL_ESQ_X = 22;
    public static final int PE_DIR_Y = 29;

    /*
     * Pivos do rig (unidades antes da escala), copiados dos renderizadores:
     * quadril, joelho e pe de cada perna, e a linha do chao (pe).
     *   Branco: joelho esq (10,136) pe esq (0,174) joelho dir (66,137) pe dir (82,171)
     *   Preto:  joelho esq (15,132) pe esq (4,171)  joelho dir (59,132) pe dir (72,171)
     * Ajoelhado: joelhos logo abaixo/a frente dos quadris, no chao depois da
     * queda; pes 34 unidades atras dos joelhos, na mesma altura (canela deitada).
     */
    private static final int[] PERNAS_BRANCO = {16, 0, -8, -38, -6, 0, -56, -35};
    private static final int QUEDA_BRANCO = 38;
    private static final int[] PERNAS_PRETO = {13, 0, -10, -39, -3, 0, -50, -39};
    private static final int QUEDA_PRETO = 39;

    /** Passo do movimento ajoelhado: o joelho que avanca anda poucas unidades. */
    public static final int PASSO_JOELHO = 4;

    private PosturaAjoelhada() {
    }

    private static boolean preto(IrmaoMiau l) {
        return l instanceof IrmaoMiauPreto;
    }

    /** Pernas ajoelhadas do rig (8 valores: joelho esq x/y, pe esq x/y, joelho dir x/y, pe dir x/y). */
    public static int[] pernas(IrmaoMiau l) {
        return (preto(l) ? PERNAS_PRETO : PERNAS_BRANCO).clone();
    }

    /** Quanto o corpo desce, em unidades do rig. */
    public static int queda(IrmaoMiau l) {
        return preto(l) ? QUEDA_PRETO : QUEDA_BRANCO;
    }

    /** Quanto o corpo desce, em pixels (escala do lutador). */
    public static double quedaPx(IrmaoMiau l) {
        return Math.round(queda(l) * l.getEscala());
    }

    /** Pose parada de joelhos (bracos e tronco neutros). */
    public static int[] poseParado(IrmaoMiau l) {
        int[] p = new int[IrmaoMiau.TAMANHO_POSE_MIAUSTUDIO];
        p[2] = (int) Math.round(l.getEscala() * 10);
        p[3] = 1;
        System.arraycopy(pernas(l), 0, p, JOEL_ESQ_X, 8);
        return p;
    }

    /**
     * Mesma pose de cima (tronco, cabeca, bracos: ex. a pegada) com as pernas
     * de joelhos. Nao altera o array recebido.
     */
    public static int[] adaptar(IrmaoMiau l, int[] poseEmPe) {
        int[] p = Arrays.copyOf(poseEmPe, poseEmPe.length);
        System.arraycopy(pernas(l), 0, p, JOEL_ESQ_X, 8);
        return p;
    }

    /**
     * Quadro do deslocamento de joelhos: fase 0/2 = neutro, 1 = joelho
     * esquerdo a frente, 3 = joelho direito a frente.
     */
    public static int[] poseMovimento(IrmaoMiau l, int fase) {
        int[] p = poseParado(l);
        int f = Math.floorMod(fase, 4);
        if (f == 1) {
            p[JOEL_ESQ_X] += PASSO_JOELHO;
            p[JOEL_ESQ_X + 2] += PASSO_JOELHO;
            p[JOEL_ESQ_X + 4] -= PASSO_JOELHO / 2;
        } else if (f == 3) {
            p[JOEL_ESQ_X + 4] += PASSO_JOELHO;
            p[JOEL_ESQ_X + 6] += PASSO_JOELHO;
            p[JOEL_ESQ_X] -= PASSO_JOELHO / 2;
        }
        return p;
    }
}
