package com.irmaosmiau.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

public class IrmaoMiauPreto extends IrmaoMiau {

    // =============================================================
    // MAPA RÁPIDO DO PERSONAGEM
    // =============================================================
    /*
     * Ordem lógica para manutenção:
     *
     * 1. CONFIGURAÇÃO / ATRIBUTOS
     * 2. DADOS DAS ANIMAÇÕES
     * 3. TAUNT
     * 4. ROLAMENTO
     * 5. SPRAWL
     * 6. MOVIMENTO NO CHÃO
     * 7. RENDERER DE POSES
     * 8. DESENHO EM PÉ
     * 9. DESENHO PRINCIPAL
     * 10. AJUSTES ESPECÍFICOS DO PERSONAGEM
     *
     * As animações exportadas usam sempre o mesmo formato de 30 valores.
     */

    public IrmaoMiauPreto(int x, int y, double escala) {

        super(
                x,
                y,
                escala,
                Color.BLACK,
                Color.WHITE,
                Color.WHITE,
                "Irmão Miau Preto",
                "Jiu-Jitsu",
                new AtributosLutador(
                        25, // condicionamento
                        40, // fôlego
                        18, // força
                        45, // velocidade
                        45, // agilidade
                        28, // técnica
                        38, // recuperação
                        68 // peso
                )
        );
    }

    // =============================================================
// ANIMAÇÃO - SPRAWL (MIAUSTUDIO)
// =============================================================
private static final int[][] SPRAWL_MIAUSTUDIO = {
    {500, 2, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 24, -11, 0, 0, 3, -7, -6, 3, 0, -6, 9, -10, 26, 0, 10, -4},
    {500, 2, 7, 0, 0, 0, 0, 0, 0, 0, 21, 0, 0, 0, 61, -1, 54, 14, 3, -7, 9, -7, 21, -4, 24, -11, 17, -9, 10, -11},
    {500, 2, 7, 0, 0, 0, 0, 0, 0, 0, 93, -30, 0, 0, 84, 3, 67, -24, -3, -35, -5, -73, 40, 16, 71, 42, 22, -26, 26, -18},
    {500, 2, 7, 0, 0, 0, 0, 0, -3, -27, 88, 0, 0, 0, 56, 1, 55, -16, -44, -30, -5, -73, 62, 11, 111, -26, 22, -26, 44, -66},
    {500, 2, 7, 0, 0, 0, 0, 0, 0, 0, 49, -34, 0, 0, -8, 15, 6, 4, 0, -15, 11, -24, 32, 17, 67, 12, 18, -7, 41, -24}
};

    // =============================================================
    // ANIMAÇÃO - TAUNT (MIAUSTUDIO)
    // =============================================================
    private static final int[][] TAUNT_MIAUSTUDIO = {
        {500, 2, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -19, 30, -7, -4, -34, 13, 4, -3, 14, 0, -4, -3, -17, 0}, // Quadro 1
        {500, 2, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -34, 33, -1, 1, -37, 27, 4, -3, 14, 0, -4, -3, -17, 0}, // Quadro 2
        {500, 2, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, -14, -10, 14, -17, -13, -53, 6, 4, -3, 14, 0, -4, -3, -17, 0}, // Quadro 3
        {500, 2, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, -14, 10, -1, -17, -13, -59, -6, 4, -3, 14, 0, -4, -3, -17, 0}, // Quadro 4
        {500, 2, 7, 0, 0, 0, 0, 0, 0, 0, 0, -6, 0, 0, 3, -16, -31, 7, -7, -11, -33, 1, 4, -3, 14, 0, -4, -3, -17, 0} // Quadro 5
    };

    /*
     * Ida e volta para evitar um corte seco
     * do Quadro 5 diretamente para o Quadro 1.
     */
    private static final int[] SEQUENCIA_TAUNT_MIAUSTUDIO = {
        0, 1, 2, 3, 4, 3, 2, 1
    };

    private static final long TEMPO_QUADRO_TAUNT_MS = 230L;

    private boolean tauntMiauStudioEstavaAtivo = false;
    private long inicioTauntMiauStudio = 0L;

    private int getIndiceQuadroTauntMiauStudio(
            boolean tauntAtivo
    ) {

        long agora = System.currentTimeMillis();

        if (!tauntAtivo) {

            tauntMiauStudioEstavaAtivo = false;
            inicioTauntMiauStudio = 0L;

            return 0;
        }

        if (!tauntMiauStudioEstavaAtivo) {

            tauntMiauStudioEstavaAtivo = true;
            inicioTauntMiauStudio = agora;
        }

        long tempoDecorrido =
                agora - inicioTauntMiauStudio;

        int indiceSequencia =
                (int) (
                        tempoDecorrido
                        / TEMPO_QUADRO_TAUNT_MS
                )
                % SEQUENCIA_TAUNT_MIAUSTUDIO.length;

        return SEQUENCIA_TAUNT_MIAUSTUDIO[
                indiceSequencia
        ];
    }

    // =============================================================
    // ANIMAÇÃO - ROLAMENTO (MIAUSTUDIO)
    // =============================================================
    private static final int[][] ROLAMENTO_MIAUSTUDIO = {
        {500, 2, 7, 0, 0, 0, 63, 0, 0, 0, 0, -33, 0, 0, 29, -22, 27, -49, 13, -48, 9, -69, 14, -16, 38, -35, 4, -24, 10, -53}, // Quadro 1
        {500, 2, 7, 0, 0, 0, 63, 0, 25, 0, 0, 15, 0, 19, 29, -22, 27, -49, -11, -32, -20, -43, 15, -38, 34, -56, -2, -40, -1, -62}, // Quadro 2
        {500, 2, 7, 0, 0, 0, 0, 0, 25, 0, -180, 15, 0, 19, 29, -22, 27, -49, -11, -32, -20, -43, 15, -38, 34, -56, -2, -40, -1, -62}, // Quadro 3
        {500, 2, 7, 0, 0, 0, 0, 0, 25, 0, 88, -53, 0, 19, 29, -22, 27, -49, -11, -32, -20, -43, 15, -38, 34, -56, -2, -40, -1, -62}, // Quadro 4
        {500, 2, 7, 0, 5, 12, 24, 0, 18, 0, 0, -37, 0, 0, 0, 0, -51, 36, 9, -12, 13, -2, 15, 7, 35, -4, 11, -4, 26, -18} // Quadro 5
    };

    /*
     * O rolamento continua durando exatamente o mesmo tempo
     * definido pela classe IrmaoMiau. Aqui apenas trocamos
     * o desenho antigo pelos cinco quadros do MiauStudio.
     *
     * Entre uma pose-chave e outra fazemos interpolação para
     * aproveitar os 22 frames do rolamento e evitar aparência
     * de slideshow.
     */
    
    private void desenharSprawl(
        Graphics2D g2
) {

    int[] poseInterpolada =
            interpolarAnimacaoMiauStudio(
                    SPRAWL_MIAUSTUDIO,
                    getProgressoSprawl()
            );

    desenharPoseMiauStudio(
            g2,
            poseInterpolada,
            40
    );
}
    
    private void desenharRolamento(
            Graphics2D g2
    ) {

double progressoVisual =
        getProgressoRolamento();

/*
 * O rolamento do Preto está natural indo
 * para frente.
 *
 * Quando ele rola para trás, invertemos
 * a timeline.
 */
if (isRolamentoParaTras()) {

    progressoVisual =
            1.0 - progressoVisual;
}

int[] poseInterpolada =
        interpolarAnimacaoMiauStudio(
                ROLAMENTO_MIAUSTUDIO,
                progressoVisual
        );

        /*
         * O X/Y absoluto exportado pelo editor NÃO é usado.
         * A posição real continua sendo a posição do personagem
         * no jogo. Assim o rolamento não teleporta.
         */
        desenharPoseMiauStudio(
                g2,
                poseInterpolada,
                60
        );
    }
    // =============================================================
    // INTERPOLAÇÃO COMUM DAS ANIMAÇÕES MIAUSTUDIO
    // =============================================================
    /*
     * Recebe qualquer animação de poses no formato de 30 valores e
     * calcula uma pose intermediária entre seus quadros-chave.
     *
     * Metadados (0 a 3) permanecem fixos; rotações usam o menor
     * caminho angular; os demais valores usam interpolação linear.
     */
    private int[] interpolarAnimacaoMiauStudio(
            int[][] animacao,
            double progresso
    ) {

        double limitado = Math.max(
                0.0,
                Math.min(1.0, progresso)
        );

        double posicao =
                limitado * (animacao.length - 1);

        int quadroA =
                (int) Math.floor(posicao);

        int quadroB = Math.min(
                quadroA + 1,
                animacao.length - 1
        );

        double t = posicao - quadroA;

        int[] a = animacao[quadroA];
        int[] b = animacao[quadroB];
        int[] resultado = new int[a.length];

        for (int i = 0; i < resultado.length; i++) {

            // cenaX, linha, escala e orientação são metadados.
            if (i <= 3) {
                resultado[i] = a[i];
                continue;
            }

            if (i == A_ROT_CINTURA
                    || i == A_ROT_TRONCO
                    || i == A_ROT_GLOBAL
                    || i == A_ROT_CABECA) {

                resultado[i] = (int) Math.round(
                        interpolarAngulo(
                                a[i],
                                b[i],
                                t
                        )
                );

            } else {

                resultado[i] = (int) Math.round(
                        a[i] + (b[i] - a[i]) * t
                );
            }
        }

        return resultado;
    }

    private double interpolarAngulo(
            double inicio,
            double fim,
            double t
    ) {

        double diferenca =
                ((fim - inicio + 540.0) % 360.0)
                - 180.0;

        return inicio
                + diferenca * t;
    }

    // =============================================================
    // ANIMAÇÃO - MOVIMENTO NO CHÃO (MIAUSTUDIO)
    // =============================================================
    private static final int[][] MOVIMENTO_CHAO_MIAUSTUDIO = {
        {500, 2, 7, 0, 0, 0, 91, 0, 0, 0, 0, -66, 0, 0, 23, -35, -23, -107, -18, -42, -32, -106, 11, 7, 32, 2, 5, -3, 0, 0},
        {500, 2, 7, 0, 0, 0, 91, 0, 0, 0, 0, -71, 0, 0, 1, -33, -21, -109, -5, -45, -32, -106, 10, 10, 32, 2, 10, -2, 0, 0},
        {500, 2, 7, 0, 0, 0, 91, 0, 0, 0, 0, 0, 0, 0, 16, -32, -21, -109, -18, -42, -32, -106, 1, 3, 32, 2, -6, 0, 0, 0},
        {500, 2, 7, 0, 0, 0, 91, 0, 0, 0, 0, -37, 0, 0, -1, -36, -10, -135, 0, -42, -32, -106, 1, 3, 23, -7, -6, 0, -4, 4},
        {500, 2, 7, 0, 0, 0, 91, 0, 0, 0, 0, -37, 0, 0, 26, -50, -8, -114, -15, -39, -29, -102, 7, 9, 28, 12, -6, 0, -7, -12}
    };

    // Índices do formato exportado pelo MiauStudio.
    private static final int A_CINTURA_X = 4;
    private static final int A_CINTURA_Y = 5;
    private static final int A_ROT_CINTURA = 6;
    private static final int A_TRONCO_X = 7;
    private static final int A_TRONCO_Y = 8;
    private static final int A_ROT_TRONCO = 9;
    private static final int A_ROT_GLOBAL = 10;
    private static final int A_ROT_CABECA = 11;
    private static final int A_CAB_X = 12;
    private static final int A_CAB_Y = 13;
    private static final int A_COT_ESQ_X = 14;
    private static final int A_COT_ESQ_Y = 15;
    private static final int A_MAO_ESQ_X = 16;
    private static final int A_MAO_ESQ_Y = 17;
    private static final int A_COT_DIR_X = 18;
    private static final int A_COT_DIR_Y = 19;
    private static final int A_MAO_DIR_X = 20;
    private static final int A_MAO_DIR_Y = 21;
    private static final int A_JOEL_ESQ_X = 22;
    private static final int A_JOEL_ESQ_Y = 23;
    private static final int A_PE_ESQ_X = 24;
    private static final int A_PE_ESQ_Y = 25;
    private static final int A_JOEL_DIR_X = 26;
    private static final int A_JOEL_DIR_Y = 27;
    private static final int A_PE_DIR_X = 28;
    private static final int A_PE_DIR_Y = 29;

    /*
     * Ping-pong evita um corte seco do quadro 5 para o quadro 1.
     * Como o deslocamento real continua sendo feito pelo GamePanel,
     * aqui cuidamos somente da pose visual.
     */
    private static final int[] SEQUENCIA_MOVIMENTO_CHAO_MIAUSTUDIO = {
        0, 1, 2, 3, 4, 3, 2, 1
    };

    private static final long TEMPO_QUADRO_MOVIMENTO_CHAO_MS = 150L;

    private boolean movimentoChaoMiauStudioEstavaAtivo = false;
    private long inicioMovimentoChaoMiauStudio = 0L;

    private int getIndiceQuadroMovimentoChaoMiauStudio() {

        long agora = System.currentTimeMillis();

        if (!movimentoChaoMiauStudioEstavaAtivo) {
            movimentoChaoMiauStudioEstavaAtivo = true;
            inicioMovimentoChaoMiauStudio = agora;
        }

        long tempoDecorrido =
                agora - inicioMovimentoChaoMiauStudio;

        int indiceSequencia =
                (int) (
                        tempoDecorrido
                        / TEMPO_QUADRO_MOVIMENTO_CHAO_MS
                )
                % SEQUENCIA_MOVIMENTO_CHAO_MIAUSTUDIO.length;

        return SEQUENCIA_MOVIMENTO_CHAO_MIAUSTUDIO[
                indiceSequencia
        ];
    }

    private void resetarAnimacaoMovimentoChaoMiauStudio() {
        movimentoChaoMiauStudioEstavaAtivo = false;
        inicioMovimentoChaoMiauStudio = 0L;
    }


    private void desenharMovimentoChaoMiauStudio(
            Graphics2D g2
    ) {

        desenharPoseChaoMiauStudio(
                g2,
                getIndiceQuadroMovimentoChaoMiauStudio()
        );
    }

    /*
     * Desenha um quadro específico da animação de chão.
     *
     * Isso permite usar exatamente o Quadro 1 quando o Miau
     * está deitado e parado. Ao começar a se arrastar, a
     * animação também começa pelo Quadro 1, evitando qualquer
     * salto de coordenadas entre os dois estados.
     */
    private void desenharPoseChaoMiauStudio(
            Graphics2D g2,
            int indiceQuadro
    ) {

        desenharPoseMiauStudio(
                g2,
                MOVIMENTO_CHAO_MIAUSTUDIO[indiceQuadro],
                40
        );
    }

    /*
     * Renderer comum das poses criadas no MiauStudio.
     *
     * É usado pelas animações de chão, rolamento, sprawl e demais poses.
     * O parâmetro deslocamentoVisualY permite que a animação
     * de chão use seu deslocamento vertical sem alterar a posição real.
     */
    private void desenharPoseMiauStudio(
            Graphics2D g2,
            int[] quadro,
            double deslocamentoVisualY
    ) {

        prepararDesenho(g2);

        Graphics2D raiz =
                (Graphics2D) g2.create();

        raiz.translate(
                0,
                deslocamentoVisualY
        );

        double posX = x;
        double posY = getYVisual();

        // Rotação global do formato do MiauStudio.
        double pivotGlobalX =
                posX + 35 * escala;

        double pivotGlobalY =
                posY + 120 * escala;

        raiz.rotate(
                Math.toRadians(
                        quadro[A_ROT_GLOBAL]
                ),
                pivotGlobalX,
                pivotGlobalY
        );

        // Root: cintura.
        double eixoCinturaX =
                posX
                + getEixoCinturaLocalX()
                * escala;

        double eixoCinturaY =
                posY
                + getEixoCinturaLocalY()
                * escala;

        raiz.translate(
                quadro[A_CINTURA_X] * escala,
                quadro[A_CINTURA_Y] * escala
        );

        raiz.rotate(
                Math.toRadians(
                        quadro[A_ROT_CINTURA]
                ),
                eixoCinturaX,
                eixoCinturaY
        );

        int corpoY =
                (int) (
                        posY
                        + 70 * escala
                );

        int topoEsqX =
                (int) (
                        posX
                        + (-4) * escala
                );

        int topoDirX =
                (int) (
                        posX
                        + 78 * escala
                );

        int fundoDirX =
                (int) (
                        posX
                        + 61 * escala
                );

        int fundoEsqX =
                (int) (
                        posX
                        + 10 * escala
                );

        int corpoFimY =
                corpoY
                + (int) (100 * escala);

        int faixaX =
                (int) (
                        posX
                        + (-6) * escala
                );

        int faixaY =
                corpoY
                + (int) (80 * escala);

        int faixaLargura =
                (int) (90 * escala);

        int faixaAltura =
                (int) (12 * escala);

        int divisaoY =
                faixaY + faixaAltura / 2;

        double tDivisao =
                (double) (divisaoY - corpoY)
                / Math.max(
                        1,
                        corpoFimY - corpoY
                );

        int divisaoEsqX =
                (int) Math.round(
                        topoEsqX
                        + (fundoEsqX - topoEsqX)
                        * tDivisao
                );

        int divisaoDirX =
                (int) Math.round(
                        topoDirX
                        + (fundoDirX - topoDirX)
                        * tDivisao
                );

        double tFaixa =
                (double) (faixaY - corpoY)
                / Math.max(
                        1,
                        corpoFimY - corpoY
                );

        int faixaEsqCorpoX =
                (int) Math.round(
                        topoEsqX
                        + (fundoEsqX - topoEsqX)
                        * tFaixa
                );

        int faixaDirCorpoX =
                (int) Math.round(
                        topoDirX
                        + (fundoDirX - topoDirX)
                        * tFaixa
                );

        // =========================
        // JUNTAS BASE + OFFSETS
        // =========================
        int ombroEsquerdoX =
                (int) (posX + 5 * escala);

        int ombroEsquerdoY =
                corpoY + (int) (22 * escala);

        int cotoveloEsquerdoX =
                (int) (
                        posX
                        + -8 * escala
                        + quadro[A_COT_ESQ_X] * escala
                );

        int cotoveloEsquerdoY =
                (int) (
                        corpoY
                        + 52 * escala
                        + quadro[A_COT_ESQ_Y] * escala
                );

        int maoEsquerdaX =
                (int) (
                        posX
                        + 35 * escala
                        + quadro[A_MAO_ESQ_X] * escala
                );

        int maoEsquerdaY =
                (int) (
                        corpoY
                        + 57 * escala
                        + quadro[A_MAO_ESQ_Y] * escala
                );

        int ombroDireitoX =
                (int) (posX + 70 * escala);

        int ombroDireitoY =
                corpoY + (int) (23 * escala);

        int cotoveloDireitoX =
                (int) (
                        posX
                        + 88 * escala
                        + quadro[A_COT_DIR_X] * escala
                );

        int cotoveloDireitoY =
                (int) (
                        corpoY
                        + 48 * escala
                        + quadro[A_COT_DIR_Y] * escala
                );

        int maoDireitaX =
                (int) (
                        posX
                        + 108 * escala
                        + quadro[A_MAO_DIR_X] * escala
                );

        int maoDireitaY =
                (int) (
                        corpoY
                        + 62 * escala
                        + quadro[A_MAO_DIR_Y] * escala
                );

        int quadrilEsquerdoX =
                (int) (posX + 24 * escala);

        int quadrilEsquerdoY =
                corpoY + (int) (97 * escala);

        int joelhoEsquerdoX =
                (int) (
                        posX
                        + 15 * escala
                        + quadro[A_JOEL_ESQ_X] * escala
                );

        int joelhoEsquerdoY =
                (int) (
                        corpoY
                        + 132 * escala
                        + quadro[A_JOEL_ESQ_Y] * escala
                );

        int peEsquerdoX =
                (int) (
                        posX
                        + 4 * escala
                        + quadro[A_PE_ESQ_X] * escala
                );

        int peEsquerdoY =
                (int) (
                        corpoY
                        + 171 * escala
                        + quadro[A_PE_ESQ_Y] * escala
                );

        int quadrilDireitoX =
                (int) (posX + 50 * escala);

        int quadrilDireitoY =
                corpoY + (int) (97 * escala);

        int joelhoDireitoX =
                (int) (
                        posX
                        + 59 * escala
                        + quadro[A_JOEL_DIR_X] * escala
                );

        int joelhoDireitoY =
                (int) (
                        corpoY
                        + 132 * escala
                        + quadro[A_JOEL_DIR_Y] * escala
                );

        int peDireitoX =
                (int) (
                        posX
                        + 72 * escala
                        + quadro[A_PE_DIR_X] * escala
                );

        int peDireitoY =
                (int) (
                        corpoY
                        + 171 * escala
                        + quadro[A_PE_DIR_Y] * escala
                );

        // =========================
        // PARTE INFERIOR
        // =========================
        Polygon pelve =
                new Polygon();

        pelve.addPoint(
                faixaEsqCorpoX,
                faixaY
        );

        pelve.addPoint(
                faixaDirCorpoX,
                faixaY
        );

        pelve.addPoint(
                fundoDirX,
                corpoFimY
        );

        pelve.addPoint(
                fundoEsqX,
                corpoFimY
        );

        raiz.setColor(corKimono);
        raiz.fillPolygon(pelve);

        raiz.setStroke(
                new BasicStroke(
                        (float) (18 * escala),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        raiz.drawLine(
                quadrilEsquerdoX,
                quadrilEsquerdoY,
                joelhoEsquerdoX,
                joelhoEsquerdoY
        );

        raiz.drawLine(
                joelhoEsquerdoX,
                joelhoEsquerdoY,
                peEsquerdoX,
                peEsquerdoY
        );

        raiz.drawLine(
                quadrilDireitoX,
                quadrilDireitoY,
                joelhoDireitoX,
                joelhoDireitoY
        );

        raiz.drawLine(
                joelhoDireitoX,
                joelhoDireitoY,
                peDireitoX,
                peDireitoY
        );

        raiz.setColor(corPelo);

        int larguraPe =
                (int) (25 * escala);

        int alturaPe =
                (int) (17 * escala);

        raiz.fillRoundRect(
                peEsquerdoX - larguraPe / 2,
                peEsquerdoY - alturaPe / 2,
                larguraPe,
                alturaPe,
                5,
                5
        );

        raiz.fillRoundRect(
                peDireitoX - larguraPe / 2,
                peDireitoY - alturaPe / 2,
                larguraPe,
                alturaPe,
                5,
                5
        );

        // Faixa cobre a emenda entre pelve e tronco.
        raiz.setColor(
                new Color(
                        55,
                        55,
                        220
                )
        );

        raiz.fillRect(
                faixaX,
                faixaY,
                faixaLargura,
                faixaAltura
        );

        // =========================
        // TRONCO SUPERIOR
        // =========================
        Graphics2D tronco =
                (Graphics2D) raiz.create();

        tronco.translate(
                quadro[A_TRONCO_X] * escala,
                quadro[A_TRONCO_Y] * escala
        );

        tronco.rotate(
                Math.toRadians(
                        quadro[A_ROT_TRONCO]
                ),
                eixoCinturaX,
                eixoCinturaY
        );

        Polygon troncoSuperior =
                new Polygon();

        troncoSuperior.addPoint(
                topoEsqX,
                corpoY
        );

        troncoSuperior.addPoint(
                topoDirX,
                corpoY
        );

        troncoSuperior.addPoint(
                divisaoDirX,
                divisaoY
        );

        troncoSuperior.addPoint(
                divisaoEsqX,
                divisaoY
        );

        tronco.setColor(corKimono);
        tronco.fillPolygon(troncoSuperior);

        tronco.setStroke(
                new BasicStroke(
                        (float) (18 * escala),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        tronco.drawLine(
                ombroEsquerdoX,
                ombroEsquerdoY,
                cotoveloEsquerdoX,
                cotoveloEsquerdoY
        );

        tronco.drawLine(
                cotoveloEsquerdoX,
                cotoveloEsquerdoY,
                maoEsquerdaX,
                maoEsquerdaY
        );

        tronco.drawLine(
                ombroDireitoX,
                ombroDireitoY,
                cotoveloDireitoX,
                cotoveloDireitoY
        );

        tronco.drawLine(
                cotoveloDireitoX,
                cotoveloDireitoY,
                maoDireitaX,
                maoDireitaY
        );

        int larguraMao =
                (int) (20 * escala);

        int alturaMao =
                (int) (18 * escala);

        tronco.setColor(corPelo);

        tronco.fillRoundRect(
                maoEsquerdaX - larguraMao / 2,
                maoEsquerdaY - alturaMao / 2,
                larguraMao,
                alturaMao,
                5,
                5
        );

        tronco.fillRoundRect(
                maoDireitaX - larguraMao / 2,
                maoDireitaY - alturaMao / 2,
                larguraMao,
                alturaMao,
                5,
                5
        );

        // Cabeça: acompanha o tronco e depois recebe sua própria rotação.
        Graphics2D cabeca =
                (Graphics2D) tronco.create();

        cabeca.setStroke(
                new BasicStroke(
                        (float) Math.max(
                                1.0,
                                1.5 * escala
                        ),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        cabeca.translate(
                quadro[A_CAB_X] * escala,
                quadro[A_CAB_Y] * escala
        );

        cabeca.rotate(
                Math.toRadians(
                        quadro[A_ROT_CABECA]
                ),
                posX + 35 * escala,
                posY + 53 * escala
        );

        desenharCabeca(cabeca);

        cabeca.dispose();
        tronco.dispose();
        raiz.dispose();
    }
private void desenharDeitado(Graphics2D g2) {

    /*
     * Agora PARADO e MOVENDO NO CHÃO usam o mesmo rig e a mesma
     * origem criada no MiauStudio.
     *
     * Antes:
     * parado      -> personagem normal girado 90°
     * movendo      -> pose nova do MiauStudio
     *
     * Como eram dois sistemas de coordenadas diferentes,
     * o personagem "teleportava" ao começar a se mover.
     *
     * Agora:
     * parado      -> Quadro 1 do MiauStudio
     * movendo      -> Q1, Q2, Q3, Q4, Q5...
     */
    if (isAndando()) {

        desenharMovimentoChaoMiauStudio(g2);

        return;
    }

    /*
     * Enquanto está parado, mantemos o Quadro 1 congelado.
     * Também resetamos o relógio da animação. Assim, quando
     * ele voltar a se mover no chão, a animação começa exatamente
     * desta mesma pose.
     */
    resetarAnimacaoMovimentoChaoMiauStudio();

    desenharPoseChaoMiauStudio(
            g2,
            0
    );
}


private void desenharEmPe(
        Graphics2D g2,
        boolean poseNeutra
) {

    prepararDesenho(g2);

    desenharCabeca(g2);
        /*
         * O Preto possui uma passada mais leve e móvel.
         */
        int passo
                = poseNeutra
                        ? 0
                        : (int) (getOscilacaoPasso() * 1.15);

        int encolhimento
                = getEncolhimentoAgachado();

        int aberturaAgachado
                = isAgachado()
                        ? (int) (8 * escala)
                        : 0;

        /*
         * O Preto é mais inquieto que o Branco:
         * as amplitudes de idle são um pouco maiores.
         */
        int idleX = 0;
        int idleY = 0;
        int idleAlternado = 0;

        if (!poseNeutra && isIdleEmPe()) {

            idleX
                    = getIdleX(
                            Math.max(
                                    1,
                                    (int) Math.round(4 * escala)
                            )
                    );

            idleY
                    = getIdleY(
                            Math.max(
                                    1,
                                    (int) Math.round(3 * escala)
                            )
                    );

            idleAlternado
                    = getIdleAlternado(
                            Math.max(
                                    1,
                                    (int) Math.round(4 * escala)
                            )
                    );
        }

        if (!poseNeutra && isIdleAgachado()) {

            idleX
                    = getIdleX(
                            Math.max(
                                    1,
                                    (int) Math.round(5 * escala)
                            )
                    );

            idleY
                    = getIdleY(
                            Math.max(
                                    1,
                                    (int) Math.round(2 * escala)
                            )
                    );

            idleAlternado
                    = getIdleAlternado(
                            Math.max(
                                    1,
                                    (int) Math.round(4 * escala)
                            )
                    );
        }

        if (!poseNeutra && isTaunt()) {

            idleX
                    = getIdleX(
                            Math.max(
                                    1,
                                    (int) Math.round(3 * escala)
                            )
                    );

            idleY
                    = getIdleY(
                            Math.max(
                                    1,
                                    (int) Math.round(2 * escala)
                            )
                    );

            idleAlternado
                    = getIdleAlternado(
                            Math.max(
                                    1,
                                    (int) Math.round(2 * escala)
                            )
                    );
        }

        int corpoY
                = getYVisual()
                + (int) (70 * escala);

        // =========================
        // CORPO
        // =========================
        Polygon corpo
                = new Polygon();

        corpo.addPoint(
                x - (int) (4 * escala),
                corpoY
        );

        corpo.addPoint(
                x + (int) (78 * escala),
                corpoY
        );

        corpo.addPoint(
                x + (int) (61 * escala),
                corpoY + (int) (100 * escala)
        );

        corpo.addPoint(
                x + (int) (10 * escala),
                corpoY + (int) (100 * escala)
        );

        g2.setColor(corKimono);
        g2.fillPolygon(corpo);

        // =========================
        // MEMBROS
        // =========================
        g2.setColor(corKimono);

        g2.setStroke(
                new BasicStroke(
                        (float) (18 * escala),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        // =================================================
        // BRAÇO ESQUERDO
        // dobrado para dentro
        // =================================================
        int ombroEsquerdoX
                = x + (int) (5 * escala);

        int ombroEsquerdoY
                = corpoY + (int) (22 * escala);

        int cotoveloEsquerdoX
                = x
                - (int) (8 * escala)
                + passo / 4
                - aberturaAgachado / 2
                + idleX;

        int cotoveloEsquerdoY
                = corpoY
                + (int) (52 * escala)
                + idleY;

        int maoEsquerdaX
                = x
                + (int) (35 * escala)
                + passo / 5
                + idleAlternado;

        int maoEsquerdaY
                = corpoY
                + (int) (57 * escala)
                - idleY;

        // =================================================
        // BRAÇO DIREITO
        // aberto e baixo na lateral
        // =================================================
        int ombroDireitoX
                = x + (int) (70 * escala);

        int ombroDireitoY
                = corpoY + (int) (23 * escala);

        int cotoveloDireitoX
                = x
                + (int) (88 * escala)
                - passo / 4
                + aberturaAgachado / 2
                - idleAlternado;

        int cotoveloDireitoY
                = corpoY
                + (int) (48 * escala)
                - idleY;

        int maoDireitaX
                = x
                + (int) (108 * escala)
                - passo / 5
                + aberturaAgachado
                - idleX;

        int maoDireitaY
                = corpoY
                + (int) (62 * escala)
                + idleY;

        // =================================================
        // PERNA ESQUERDA
        // =================================================
        int quadrilEsquerdoX
                = x + (int) (24 * escala);

        int quadrilEsquerdoY
                = corpoY + (int) (97 * escala);

        int joelhoEsquerdoX
                = x
                + (int) (15 * escala)
                - passo / 2
                - aberturaAgachado
                + idleAlternado;

        int joelhoEsquerdoY
                = corpoY
                + (int) (132 * escala)
                - encolhimento / 2
                + idleY;

        int peEsquerdoX
                = x
                + (int) (4 * escala)
                - passo
                - aberturaAgachado
                + idleX;

        int peEsquerdoY
                = corpoY
                + (int) (171 * escala)
                - encolhimento;

        // =================================================
        // PERNA DIREITA
        // =================================================
        int quadrilDireitoX
                = x + (int) (50 * escala);

        int quadrilDireitoY
                = corpoY + (int) (97 * escala);

        int joelhoDireitoX
                = x
                + (int) (59 * escala)
                + passo / 2
                + aberturaAgachado
                - idleAlternado;

        int joelhoDireitoY
                = corpoY
                + (int) (132 * escala)
                - encolhimento / 2
                - idleY;

        int peDireitoX
                = x
                + (int) (72 * escala)
                + passo
                + aberturaAgachado
                - idleX;

        int peDireitoY
                = corpoY
                + (int) (171 * escala)
                - encolhimento;

        // =================================================
        // TAUNT / POSTURA NATURAL
        // =================================================
        if (isTaunt()) {

            /*
             * O Preto relaxa ainda mais que o Branco.
             * Os braços ficam próximos do corpo e
             * as pernas ficam praticamente juntas.
             */
            ombroEsquerdoX
                    = x + (int) (8 * escala);

            ombroEsquerdoY
                    = corpoY + (int) (22 * escala);

            cotoveloEsquerdoX
                    = x
                    + (int) (10 * escala)
                    + idleX / 2;

            cotoveloEsquerdoY
                    = corpoY
                    + (int) (57 * escala)
                    + idleY;

            maoEsquerdaX
                    = x
                    + (int) (12 * escala)
                    + idleAlternado;

            maoEsquerdaY
                    = corpoY
                    + (int) (88 * escala)
                    + idleY;

            ombroDireitoX
                    = x + (int) (67 * escala);

            ombroDireitoY
                    = corpoY + (int) (22 * escala);

            cotoveloDireitoX
                    = x
                    + (int) (65 * escala)
                    - idleX / 2;

            cotoveloDireitoY
                    = corpoY
                    + (int) (57 * escala)
                    - idleY;

            maoDireitaX
                    = x
                    + (int) (63 * escala)
                    - idleAlternado;

            maoDireitaY
                    = corpoY
                    + (int) (88 * escala)
                    + idleY;

            quadrilEsquerdoX
                    = x + (int) (29 * escala);

            joelhoEsquerdoX
                    = x
                    + (int) (30 * escala)
                    + idleX / 2;

            peEsquerdoX
                    = x
                    + (int) (29 * escala)
                    + idleX;

            quadrilDireitoX
                    = x + (int) (45 * escala);

            joelhoDireitoX
                    = x
                    + (int) (45 * escala)
                    - idleX / 2;

            peDireitoX
                    = x
                    + (int) (46 * escala)
                    - idleX;

            joelhoEsquerdoY
                    = corpoY
                    + (int) (133 * escala)
                    + idleY;

            joelhoDireitoY
                    = corpoY
                    + (int) (133 * escala)
                    - idleY;

            peEsquerdoY
                    = corpoY + (int) (171 * escala);

            peDireitoY
                    = corpoY + (int) (171 * escala);
        }

        // =========================
        // DESENHO DOS BRAÇOS
        // =========================
        g2.drawLine(
                ombroEsquerdoX,
                ombroEsquerdoY,
                cotoveloEsquerdoX,
                cotoveloEsquerdoY
        );

        g2.drawLine(
                cotoveloEsquerdoX,
                cotoveloEsquerdoY,
                maoEsquerdaX,
                maoEsquerdaY
        );

        g2.drawLine(
                ombroDireitoX,
                ombroDireitoY,
                cotoveloDireitoX,
                cotoveloDireitoY
        );

        g2.drawLine(
                cotoveloDireitoX,
                cotoveloDireitoY,
                maoDireitaX,
                maoDireitaY
        );

        // =========================
        // DESENHO DAS PERNAS
        // =========================
        g2.drawLine(
                quadrilEsquerdoX,
                quadrilEsquerdoY,
                joelhoEsquerdoX,
                joelhoEsquerdoY
        );

        g2.drawLine(
                joelhoEsquerdoX,
                joelhoEsquerdoY,
                peEsquerdoX,
                peEsquerdoY
        );

        g2.drawLine(
                quadrilDireitoX,
                quadrilDireitoY,
                joelhoDireitoX,
                joelhoDireitoY
        );

        g2.drawLine(
                joelhoDireitoX,
                joelhoDireitoY,
                peDireitoX,
                peDireitoY
        );

        // =========================
        // MÃOS
        // =========================
        int larguraMao
                = (int) (20 * escala);

        int alturaMao
                = (int) (18 * escala);

        desenharPata(
                g2,
                maoEsquerdaX - larguraMao / 2,
                maoEsquerdaY - alturaMao / 2,
                larguraMao,
                alturaMao
        );

        desenharPata(
                g2,
                maoDireitaX - larguraMao / 2,
                maoDireitaY - alturaMao / 2,
                larguraMao,
                alturaMao
        );

        // =========================
        // PÉS
        // =========================
        int larguraPe
                = (int) (25 * escala);

        int alturaPe
                = (int) (17 * escala);

        desenharPata(
                g2,
                peEsquerdoX - larguraPe / 2,
                peEsquerdoY - alturaPe / 2,
                larguraPe,
                alturaPe
        );

        desenharPata(
                g2,
                peDireitoX - larguraPe / 2,
                peDireitoY - alturaPe / 2,
                larguraPe,
                alturaPe
        );

        // =========================
        // FAIXA AZUL
        // =========================
        g2.setColor(
                new Color(55, 55, 220)
        );

        g2.fillRect(
                x - (int) (6 * escala),
                corpoY + (int) (80 * escala),
                (int) (90 * escala),
                (int) (12 * escala)
        );
}


    @Override
    public void desenhar(Graphics2D g2) {

        prepararDesenho(g2);

        /*
         * O novo taunt do MiauStudio só assume o desenho
         * quando o Preto está realmente no estado de taunt
         * e não está executando uma ação prioritária.
         */
        boolean tauntVisualAtivo =
                isTaunt()
                && !isRolando()
                && !isDeitado();

        int indiceQuadroTaunt =
                getIndiceQuadroTauntMiauStudio(
                        tauntVisualAtivo
                );

        // =========================
        // ROLAMENTO
        // =========================
        if (isRolando()) {

            desenharRolamento(g2);

            return;
        }

        if (isFazendoSprawl()) {

    desenharSprawl(g2);

    return;
}
        
        // =========================
        // CHÃO
        // =========================
        if (isDeitado()) {

            desenharDeitado(g2);

            return;
        }

        // =========================
        // TAUNT MIAUSTUDIO
        // =========================
        if (tauntVisualAtivo) {

            desenharPoseMiauStudio(
                    g2,
                    TAUNT_MIAUSTUDIO[
                            indiceQuadroTaunt
                    ],
                    0
            );

            return;
        }

        // =========================
        // PERSONAGEM NORMAL
        // =========================
        desenharEmPe(
                g2,
                false
        );
    }

    @Override
    protected double getProfundidadeAgachamento() {

        return 14;
    }

    @Override
    protected double getFlexaoPernasAgachado() {

        return 25;
    }
}
