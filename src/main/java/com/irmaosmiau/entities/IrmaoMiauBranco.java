package com.irmaosmiau.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

public class IrmaoMiauBranco extends IrmaoMiau {

    public IrmaoMiauBranco(int x, int y, double escala) {

        super(
                x,
                y,
                escala,
                Color.WHITE,
                Color.BLACK,
                Color.BLACK,
                "Irmão Miau Branco",
                "Jiu-Jitsu",
                new AtributosLutador(
                        35, // condicionamento
                        25, // 25 fôlego 
                        42, // força
                        22, // velocidade
                        25, // agilidade
                        25, // técnica
                        22, // recuperação
                        72 // peso
                )

        );
    }

    // =============================================================
    // TAUNT CRIADO NO MIAUSTUDIO
    // =============================================================
    /*
     * Formato original exportado pelo editor:
     *
     * cenaX, linha, escalaX10, viradoDireita,
     * cinturaX, cinturaY, rotCintura,
     * troncoX, troncoY, rotTronco,
     * rotGlobal, rotCabeca, cabX, cabY,
     * cotEsqX, cotEsqY, maoEsqX, maoEsqY,
     * cotDirX, cotDirY, maoDirX, maoDirY,
     * joelEsqX, joelEsqY, peEsqX, peEsqY,
     * joelDirX, joelDirY, peDirX, peDirY
     */
    private static final int[][] TAUNT_MIAUSTUDIO = {
        {220, 2, 7, 1, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0,   0, 0, -17, 16,   0, 0, -4, 24,   0, 0, 0, 0,   0, 0, 0, 0},
        {220, 2, 7, 1, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0,   0, 0, -17, 16,  24,-17, 33,-19,   0, 0, 0, 0,   0, 0, 0, 0},
        {220, 2, 7, 1, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0,   0, 0, -17, 16,  24,-17, 24,  0,   0, 0, 0, 0,   0, 0, 0, 0},
        {220, 2, 7, 1, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0,   0, 0,  -6, 10,  24,-17, 34,-27,   0, 0, 0, 0,   0, 0, 0, 0},
        {220, 2, 7, 1, 0, 0, 0, 0, 0, 0, 0, -19, 0, 0,   0, 0,  -6, 10,  24,-17, 31, -1,   0, 0, 0, 0,   0, 0, 0, 0}
    };

    // Colunas que realmente usamos neste primeiro TAUNT.
    private static final int T_ROT_CABECA = 11;

    private static final int T_COT_ESQ_X = 14;
    private static final int T_COT_ESQ_Y = 15;
    private static final int T_MAO_ESQ_X = 16;
    private static final int T_MAO_ESQ_Y = 17;

    private static final int T_COT_DIR_X = 18;
    private static final int T_COT_DIR_Y = 19;
    private static final int T_MAO_DIR_X = 20;
    private static final int T_MAO_DIR_Y = 21;

    private static final int T_JOEL_ESQ_X = 22;
    private static final int T_JOEL_ESQ_Y = 23;
    private static final int T_PE_ESQ_X = 24;
    private static final int T_PE_ESQ_Y = 25;

    private static final int T_JOEL_DIR_X = 26;
    private static final int T_JOEL_DIR_Y = 27;
    private static final int T_PE_DIR_X = 28;
    private static final int T_PE_DIR_Y = 29;

    /*
     * Fazemos ida e volta para não existir um salto seco
     * do Quadro 5 diretamente para o Quadro 1.
     */
    private static final int[] SEQUENCIA_TAUNT_MIAUSTUDIO = {
        0, 1, 2, 3, 4, 3, 2, 1
    };

    private static final long TEMPO_QUADRO_TAUNT_MS = 230L;

    private boolean tauntMiauStudioEstavaAtivo = false;
    private long inicioTauntMiauStudio = 0L;

    private int getIndiceQuadroTauntMiauStudio(
            boolean tauntVisualAtivo
    ) {

        long agora = System.currentTimeMillis();

        if (!tauntVisualAtivo) {

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

    private void desenharCabecaTauntMiauStudio(
            Graphics2D g2,
            int rotacaoGraus
    ) {

        Graphics2D copia =
                (Graphics2D) g2.create();

        /*
         * Mesmo pivô utilizado pelo MiauStudio:
         * aproximadamente o centro da cabeça.
         */
        double pivotX =
                x + 35 * escala;

        double pivotY =
                getYVisual()
                + 53 * escala;

        copia.rotate(
                Math.toRadians(rotacaoGraus),
                pivotX,
                pivotY
        );

        desenharCabeca(copia);

        copia.dispose();
    }

private void desenharRolamento(
        Graphics2D g2
) {

    double progresso =
            getProgressoRolamento();

    if (progresso < 0.15) {

        desenharEntradaRolamento(
                g2,
                progresso / 0.15
        );

    } else if (progresso < 0.30) {

        desenharMergulhoRolamento(
                g2,
                (progresso - 0.15) / 0.15
        );

    } else if (progresso < 0.70) {

        desenharGiroRolamento(
                g2,
                (progresso - 0.30) / 0.40
        );

    } else if (progresso < 0.85) {

        desenharSaidaRolamento(
                g2,
                (progresso - 0.70) / 0.15
        );

    } else {

        desenharLevantandoRolamento(
                g2,
                (progresso - 0.85) / 0.15
        );
    }
}

private void desenharGiroRolamento(
        Graphics2D g2,
        double fase
) {

    Graphics2D copia =
            (Graphics2D) g2.create();

    /*
     * Durante o miolo da cambalhota
     * o Branco fica entre 20% e 25%
     * maior.
     */
    double aumento =
            1.20
            + Math.sin(
                    fase * Math.PI
            ) * 0.05;

    double centroX =
            x + 35 * escala;

    double centroY =
            y + 195 * escala;

    double angulo =
            fase
            * Math.PI
            * 2.0;

    copia.translate(
            centroX,
            centroY
    );

    copia.rotate(
            angulo
    );

    copia.scale(
            aumento,
            aumento
    );

    copia.translate(
            -centroX,
            -centroY
    );

    // =========================
    // CORPO ENROLADO
    // =========================

    /*
     * Durante o giro NÃO desenhamos
     * a cabeça separadamente.
     *
     * Ela está recolhida junto
     * ao corpo.
     */
    copia.setColor(
            corKimono
    );

    int largura =
            (int) Math.round(
                    72 * escala
            );

    int altura =
            (int) Math.round(
                    62 * escala
            );

    copia.fillRoundRect(
            (int) Math.round(
                    centroX
                    - largura / 2.0
            ),
            (int) Math.round(
                    centroY
                    - altura / 2.0
            ),
            largura,
            altura,
            (int) Math.round(
                    35 * escala
            ),
            (int) Math.round(
                    35 * escala
            )
    );

    // =========================
    // PATAS RECOLHIDAS
    // =========================

    copia.setColor(
            corPelo
    );

    copia.fillOval(
            (int) Math.round(
                    centroX
                    - 31 * escala
            ),
            (int) Math.round(
                    centroY
                    + 8 * escala
            ),
            (int) Math.round(
                    25 * escala
            ),
            (int) Math.round(
                    19 * escala
            )
    );

    copia.fillOval(
            (int) Math.round(
                    centroX
                    + 6 * escala
            ),
            (int) Math.round(
                    centroY
                    + 8 * escala
            ),
            (int) Math.round(
                    25 * escala
            ),
            (int) Math.round(
                    19 * escala
            )
    );

    copia.dispose();
}
    
private void desenharEntradaRolamento(
        Graphics2D g2,
        double fase
) {

    Graphics2D copia =
            (Graphics2D) g2.create();

    prepararDesenho(copia);

    /*
     * Começa praticamente em pé e
     * vai se abaixando para preparar
     * a cambalhota.
     */
    double centroX =
            x + 35 * escala;

    double centroY =
            y + (85 + 25 * fase) * escala;

    double inclinacao =
            fase * 0.30;

    /*
     * Cresce suavemente.
     */
    double aumento =
            1.0 + fase * 0.10;

    copia.translate(
            centroX,
            centroY
    );

    copia.rotate(
            inclinacao
    );

    copia.scale(
            aumento,
            aumento
    );

    copia.translate(
            -centroX,
            -centroY
    );

    // CORPO
    copia.setColor(
            corKimono
    );

    int largura =
            (int) Math.round(
                    (68 + fase * 5) * escala
            );

    int altura =
            (int) Math.round(
                    (105 - fase * 35) * escala
            );

    copia.fillRoundRect(
            (int) Math.round(
                    centroX - largura / 2.0
            ),
            (int) Math.round(
                    centroY - altura / 2.0
            ),
            largura,
            altura,
            (int) Math.round(25 * escala),
            (int) Math.round(25 * escala)
    );

    /*
     * Aqui a cabeça ainda aparece,
     * mas começa a entrar em direção
     * ao peito.
     */
    copia.setColor(
            corPelo
    );

    int tamanhoCabeca =
            (int) Math.round(
                    58 * escala
            );

    int cabecaX =
            (int) Math.round(
                    centroX
                    - tamanhoCabeca / 2.0
                    + fase * 12 * escala
            );

    int cabecaY =
            (int) Math.round(
                    centroY
                    - 75 * escala
                    + fase * 35 * escala
            );

    copia.fillOval(
            cabecaX,
            cabecaY,
            tamanhoCabeca,
            tamanhoCabeca
    );

    // PERNAS COMEÇANDO A DOBRAR
    copia.setColor(
            corKimono
    );

    copia.setStroke(
            new BasicStroke(
                    (float) Math.max(
                            2,
                            15 * escala
                    ),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            )
    );

    copia.drawLine(
            (int) (centroX - 18 * escala),
            (int) (centroY + 35 * escala),
            (int) (centroX - 30 * escala),
            (int) (centroY + (65 - 20 * fase) * escala)
    );

    copia.drawLine(
            (int) (centroX + 18 * escala),
            (int) (centroY + 35 * escala),
            (int) (centroX + 32 * escala),
            (int) (centroY + (65 - 20 * fase) * escala)
    );

    copia.dispose();
}

private void desenharMergulhoRolamento(
        Graphics2D g2,
        double fase
) {

    Graphics2D copia =
            (Graphics2D) g2.create();

    prepararDesenho(copia);

    double centroX =
            x + 35 * escala;

    double centroY =
            y + (100 + 8 * fase) * escala;

    /*
     * Vai de aproximadamente 20 graus
     * até quase 90 graus.
     */
    double angulo =
            0.35 + fase * 1.05;

    /*
     * Já fica visivelmente maior.
     */
    double aumento =
            1.10 + fase * 0.10;

    copia.translate(
            centroX,
            centroY
    );

    copia.rotate(
            angulo
    );

    copia.scale(
            aumento,
            aumento
    );

    copia.translate(
            -centroX,
            -centroY
    );

    // =========================
    // CORPO SE ENROLANDO
    // =========================

    copia.setColor(
            corKimono
    );

    int largura =
            (int) Math.round(
                    (72 + 5 * fase) * escala
            );

    int altura =
            (int) Math.round(
                    (72 - 8 * fase) * escala
            );

    copia.fillRoundRect(
            (int) Math.round(
                    centroX - largura / 2.0
            ),
            (int) Math.round(
                    centroY - altura / 2.0
            ),
            largura,
            altura,
            (int) Math.round(32 * escala),
            (int) Math.round(32 * escala)
    );

    /*
     * Não existe mais cabeça separada.
     *
     * Ela já está recolhida.
     */

    // BRAÇOS/PATAS ENTRANDO
    copia.setColor(
            corPelo
    );

    copia.fillOval(
            (int) Math.round(
                    centroX - 35 * escala
            ),
            (int) Math.round(
                    centroY - 3 * escala
            ),
            (int) Math.round(
                    25 * escala
            ),
            (int) Math.round(
                    19 * escala
            )
    );

    // JOELHOS/PATAS
    copia.fillOval(
            (int) Math.round(
                    centroX + 10 * escala
            ),
            (int) Math.round(
                    centroY + 12 * escala
            ),
            (int) Math.round(
                    27 * escala
            ),
            (int) Math.round(
                    20 * escala
            )
    );

    copia.dispose();
}

private void desenharSaidaRolamento(
        Graphics2D g2,
        double fase
) {

    Graphics2D copia =
            (Graphics2D) g2.create();

    prepararDesenho(copia);

    double centroX =
            x + 35 * escala;

    double centroY =
            y + (108 - 8 * fase) * escala;

    /*
     * Ainda está girado no começo,
     * mas vai voltando.
     */
    double angulo =
            1.35
            - fase * 1.05;

    double aumento =
            1.20 - fase * 0.10;

    copia.translate(
            centroX,
            centroY
    );

    copia.rotate(
            angulo
    );

    copia.scale(
            aumento,
            aumento
    );

    copia.translate(
            -centroX,
            -centroY
    );

    // =========================
    // CORPO ABRINDO
    // =========================

    copia.setColor(
            corKimono
    );

    int largura =
            (int) Math.round(
                    (77 - 9 * fase) * escala
            );

    int altura =
            (int) Math.round(
                    (64 + 35 * fase) * escala
            );

    copia.fillRoundRect(
            (int) Math.round(
                    centroX - largura / 2.0
            ),
            (int) Math.round(
                    centroY - altura / 2.0
            ),
            largura,
            altura,
            (int) Math.round(28 * escala),
            (int) Math.round(28 * escala)
    );

    // PATA DE APOIO
    copia.setColor(
            corPelo
    );

    copia.fillOval(
            (int) Math.round(
                    centroX - 38 * escala
            ),
            (int) Math.round(
                    centroY + 20 * escala
            ),
            (int) Math.round(
                    30 * escala
            ),
            (int) Math.round(
                    19 * escala
            )
    );

    /*
     * A cabeça começa a reaparecer
     * somente na segunda metade
     * da saída.
     */
    if (fase > 0.50) {

        double aparicao =
                (fase - 0.50) / 0.50;

        int tamanhoCabeca =
                (int) Math.round(
                        58
                        * escala
                        * aparicao
                );

        copia.setColor(
                corPelo
        );

        copia.fillOval(
                (int) Math.round(
                        centroX
                        - tamanhoCabeca / 2.0
                        + 12 * escala
                ),
                (int) Math.round(
                        centroY
                        - 55 * escala
                        * aparicao
                ),
                tamanhoCabeca,
                tamanhoCabeca
        );
    }

    copia.dispose();
}

private void desenharLevantandoRolamento(
        Graphics2D g2,
        double fase
) {

    Graphics2D copia =
            (Graphics2D) g2.create();

    prepararDesenho(copia);

    double centroX =
            x + 35 * escala;

    /*
     * Ele começa baixo e volta
     * para a altura normal.
     */
    double centroY =
            y + (100 - 20 * fase) * escala;

    double aumento =
            1.10 - fase * 0.10;

    double inclinacao =
            0.30 * (1.0 - fase);

    copia.translate(
            centroX,
            centroY
    );

    copia.rotate(
            inclinacao
    );

    copia.scale(
            aumento,
            aumento
    );

    copia.translate(
            -centroX,
            -centroY
    );

    // =========================
    // CORPO VOLTANDO AO NORMAL
    // =========================

    copia.setColor(
            corKimono
    );

    int largura =
            (int) Math.round(
                    (68 + 4 * fase) * escala
            );

    int altura =
            (int) Math.round(
                    (95 + 10 * fase) * escala
            );

    copia.fillRoundRect(
            (int) Math.round(
                    centroX - largura / 2.0
            ),
            (int) Math.round(
                    centroY - altura / 2.0
            ),
            largura,
            altura,
            (int) Math.round(24 * escala),
            (int) Math.round(24 * escala)
    );

    // =========================
    // CABEÇA
    // =========================

    copia.setColor(
            corPelo
    );

    int tamanhoCabeca =
            (int) Math.round(
                    58 * escala
            );

    copia.fillOval(
            (int) Math.round(
                    centroX
                    - tamanhoCabeca / 2.0
            ),
            (int) Math.round(
                    centroY
                    - 76 * escala
                    + 16 * escala * (1.0 - fase)
            ),
            tamanhoCabeca,
            tamanhoCabeca
    );

    // =========================
    // PERNAS SE ABRINDO
    // =========================

    copia.setColor(
            corKimono
    );

    copia.setStroke(
            new BasicStroke(
                    (float) Math.max(
                            2,
                            15 * escala
                    ),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            )
    );

    double abertura =
            15 + fase * 18;

    copia.drawLine(
            (int) (centroX - 14 * escala),
            (int) (centroY + 38 * escala),
            (int) (centroX - abertura * escala),
            (int) (centroY + 70 * escala)
    );

    copia.drawLine(
            (int) (centroX + 14 * escala),
            (int) (centroY + 38 * escala),
            (int) (centroX + abertura * escala),
            (int) (centroY + 70 * escala)
    );

    copia.dispose();
}


    // =============================================================
    // ARRASTAR NO CHÃO - ANIMAÇÃO CRIADA NO MIAUSTUDIO
    // =============================================================
    private static final int[][] ARRASTANDO_MIAUSTUDIO = {
        {216, 2, 7, 1, 0, -1, 90, 0, 0, 0, 0, -58, 8, -3, 21, -21, 11, -93, -17, -43, -31, -66, 20, 9, 34, -1, 0, 0, -9, -1},
        {216, 2, 7, 1, 0, -1, 90, 0, 0, 0, 0, -58, 8, -3, 3, -23, 6, -86, 0, -37, -31, -66, 10, 2, 21, 6, 0, 0, -9, -1},
        {216, 2, 7, 1, 0, -1, 90, 0, 0, 0, 0, -58, 8, -3, 30, -27, 6, -86, -13, -34, -31, -66, 24, 2, 37, 9, -14, 2, -9, -1},
        {216, 2, 7, 1, 0, -1, 90, 0, 0, 0, 0, -58, 8, -3, -1, -31, 9, -83, 6, -26, -26, -53, 14, 6, 31, -10, -3, 2, -17, -4},
        {216, 2, 7, 1, 0, -1, 90, 0, 0, 0, 0, -30, 8, -1, 30, -48, 4, -98, -13, -37, -29, -83, 10, -13, 21, -7, -6, -1, -29, -6}
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
    private static final int[] SEQUENCIA_ARRASTANDO_MIAUSTUDIO = {
        0, 1, 2, 3, 4, 3, 2, 1
    };

    private static final long TEMPO_QUADRO_ARRASTANDO_MS = 150L;

    private boolean arrastandoMiauStudioEstavaAtivo = false;
    private long inicioArrastandoMiauStudio = 0L;

    private int getIndiceQuadroArrastandoMiauStudio() {

        long agora = System.currentTimeMillis();

        if (!arrastandoMiauStudioEstavaAtivo) {
            arrastandoMiauStudioEstavaAtivo = true;
            inicioArrastandoMiauStudio = agora;
        }

        long tempoDecorrido =
                agora - inicioArrastandoMiauStudio;

        int indiceSequencia =
                (int) (
                        tempoDecorrido
                        / TEMPO_QUADRO_ARRASTANDO_MS
                )
                % SEQUENCIA_ARRASTANDO_MIAUSTUDIO.length;

        return SEQUENCIA_ARRASTANDO_MIAUSTUDIO[
                indiceSequencia
        ];
    }

    private void resetarAnimacaoArrastandoMiauStudio() {
        arrastandoMiauStudioEstavaAtivo = false;
        inicioArrastandoMiauStudio = 0L;
    }


    private void desenharArrastandoMiauStudio(
            Graphics2D g2
    ) {

        desenharPoseChaoMiauStudio(
                g2,
                getIndiceQuadroArrastandoMiauStudio()
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

        prepararDesenho(g2);

        int[] quadro =
                ARRASTANDO_MIAUSTUDIO[
                        indiceQuadro
                ];

Graphics2D raiz =
        (Graphics2D) g2.create();

/*
 * Deslocamento visual de todo o personagem
 * quando está no chão.
 */
double deslocamentoChaoY = 40;

raiz.translate(
        0,
        deslocamentoChaoY
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
                        + (-10) * escala
                );

        int topoDirX =
                (int) (
                        posX
                        + 82 * escala
                );

        int fundoDirX =
                (int) (
                        posX
                        + 64 * escala
                );

        int fundoEsqX =
                (int) (
                        posX
                        + 8 * escala
                );

        int corpoFimY =
                corpoY
                + (int) (105 * escala);

        int faixaX =
                (int) (
                        posX
                        + (-8) * escala
                );

        int faixaY =
                corpoY
                + (int) (82 * escala);

        int faixaLargura =
                (int) (96 * escala);

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
                (int) (posX + 3 * escala);

        int ombroEsquerdoY =
                corpoY + (int) (22 * escala);

        int cotoveloEsquerdoX =
                (int) (
                        posX
                        + -14 * escala
                        + quadro[A_COT_ESQ_X] * escala
                );

        int cotoveloEsquerdoY =
                (int) (
                        corpoY
                        + 42 * escala
                        + quadro[A_COT_ESQ_Y] * escala
                );

        int maoEsquerdaX =
                (int) (
                        posX
                        + 3 * escala
                        + quadro[A_MAO_ESQ_X] * escala
                );

        int maoEsquerdaY =
                (int) (
                        corpoY
                        + 58 * escala
                        + quadro[A_MAO_ESQ_Y] * escala
                );

        int ombroDireitoX =
                (int) (posX + 73 * escala);

        int ombroDireitoY =
                corpoY + (int) (20 * escala);

        int cotoveloDireitoX =
                (int) (
                        posX
                        + 95 * escala
                        + quadro[A_COT_DIR_X] * escala
                );

        int cotoveloDireitoY =
                (int) (
                        corpoY
                        + 36 * escala
                        + quadro[A_COT_DIR_Y] * escala
                );

        int maoDireitaX =
                (int) (
                        posX
                        + 111 * escala
                        + quadro[A_MAO_DIR_X] * escala
                );

        int maoDireitaY =
                (int) (
                        corpoY
                        + 29 * escala
                        + quadro[A_MAO_DIR_Y] * escala
                );

        int quadrilEsquerdoX =
                (int) (posX + 22 * escala);

        int quadrilEsquerdoY =
                corpoY + (int) (100 * escala);

        int joelhoEsquerdoX =
                (int) (
                        posX
                        + 10 * escala
                        + quadro[A_JOEL_ESQ_X] * escala
                );

        int joelhoEsquerdoY =
                (int) (
                        corpoY
                        + 136 * escala
                        + quadro[A_JOEL_ESQ_Y] * escala
                );

        int peEsquerdoX =
                (int) (
                        posX
                        + 0 * escala
                        + quadro[A_PE_ESQ_X] * escala
                );

        int peEsquerdoY =
                (int) (
                        corpoY
                        + 174 * escala
                        + quadro[A_PE_ESQ_Y] * escala
                );

        int quadrilDireitoX =
                (int) (posX + 52 * escala);

        int quadrilDireitoY =
                corpoY + (int) (100 * escala);

        int joelhoDireitoX =
                (int) (
                        posX
                        + 66 * escala
                        + quadro[A_JOEL_DIR_X] * escala
                );

        int joelhoDireitoY =
                (int) (
                        corpoY
                        + 137 * escala
                        + quadro[A_JOEL_DIR_Y] * escala
                );

        int peDireitoX =
                (int) (
                        posX
                        + 82 * escala
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
     * Agora PARADO e ARRASTANDO usam o mesmo rig e a mesma
     * origem criada no MiauStudio.
     *
     * Antes:
     * parado      -> personagem normal girado 90°
     * arrastando  -> pose nova do MiauStudio
     *
     * Como eram dois sistemas de coordenadas diferentes,
     * o personagem "teleportava" ao começar a se mover.
     *
     * Agora:
     * parado      -> Quadro 1 do MiauStudio
     * arrastando  -> Q1, Q2, Q3, Q4, Q5...
     */
    if (isAndando()) {

        desenharArrastandoMiauStudio(g2);

        return;
    }

    /*
     * Enquanto está parado, mantemos o Quadro 1 congelado.
     * Também resetamos o relógio da animação. Assim, quando
     * ele voltar a se arrastar, a animação começa exatamente
     * desta mesma pose.
     */
    resetarAnimacaoArrastandoMiauStudio();

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

    /*
     * O TAUNT novo é controlado pelo próprio IrmaoMiauBranco.
     * poseNeutra=true é usado, por exemplo, quando ele está deitado
     * e não deve herdar o taunt.
     */
    boolean tauntVisualAtivo =
            !poseNeutra
            && isTaunt();

    int indiceQuadroTaunt =
            getIndiceQuadroTauntMiauStudio(
                    tauntVisualAtivo
            );

    int[] quadroTaunt =
            TAUNT_MIAUSTUDIO[
                    indiceQuadroTaunt
            ];

    if (tauntVisualAtivo) {

        desenharCabecaTauntMiauStudio(
                g2,
                quadroTaunt[T_ROT_CABECA]
        );

    } else {

        desenharCabeca(g2);
    }

        /*
         * O Branco possui uma passada mais firme/pesada.
         */
        int passo
                = poseNeutra
                        ? 0
                        : (int) (getOscilacaoPasso() * 0.75);

        int encolhimento
                = getEncolhimentoAgachado();

        int aberturaAgachado
                = isAgachado()
                        ? (int) (10 * escala)
                        : 0;

        /*
         * Pequenos deslocamentos usados pelas poses de idle.
         * Cada estado usa amplitudes levemente diferentes.
         */
        int idleX = 0;
        int idleY = 0;
        int idleAlternado = 0;

        if (!poseNeutra && isIdleEmPe()) {

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

        if (!poseNeutra && isIdleAgachado()) {

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
                                    (int) Math.round(2 * escala)
                            )
                    );

            idleAlternado
                    = getIdleAlternado(
                            Math.max(
                                    1,
                                    (int) Math.round(3 * escala)
                            )
                    );
        }

        if (!poseNeutra && isTaunt()) {

            idleX
                    = getIdleX(
                            Math.max(
                                    1,
                                    (int) Math.round(2 * escala)
                            )
                    );

            idleY
                    = getIdleY(
                            Math.max(
                                    1,
                                    (int) Math.round(1 * escala)
                            )
                    );

            idleAlternado
                    = getIdleAlternado(
                            Math.max(
                                    1,
                                    (int) Math.round(1 * escala)
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
                x - (int) (10 * escala),
                corpoY
        );

        corpo.addPoint(
                x + (int) (82 * escala),
                corpoY
        );

        corpo.addPoint(
                x + (int) (64 * escala),
                corpoY + (int) (105 * escala)
        );

        corpo.addPoint(
                x + (int) (8 * escala),
                corpoY + (int) (105 * escala)
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
        // ombro -> cotovelo -> mão
        // =================================================
        int ombroEsquerdoX
                = x + (int) (3 * escala);

        int ombroEsquerdoY
                = corpoY + (int) (22 * escala);

        int cotoveloEsquerdoX
                = x
                - (int) (14 * escala)
                + passo / 3
                - aberturaAgachado / 2
                + idleX;

        int cotoveloEsquerdoY
                = corpoY
                + (int) (42 * escala)
                + idleY;

        int maoEsquerdaX
                = x
                + (int) (3 * escala)
                + passo / 2
                - aberturaAgachado
                + idleAlternado;

        int maoEsquerdaY
                = corpoY
                + (int) (58 * escala)
                - idleY;

        // =================================================
        // BRAÇO DIREITO
        // braço da frente
        // =================================================
        int ombroDireitoX
                = x + (int) (73 * escala);

        int ombroDireitoY
                = corpoY + (int) (20 * escala);

        int cotoveloDireitoX
                = x
                + (int) (95 * escala)
                - passo / 3
                + aberturaAgachado / 2
                - idleAlternado;

        int cotoveloDireitoY
                = corpoY
                + (int) (36 * escala)
                - idleY;

        int maoDireitaX
                = x
                + (int) (111 * escala)
                - passo / 2
                + aberturaAgachado
                - idleX;

        int maoDireitaY
                = corpoY
                + (int) (29 * escala)
                + idleY;

        // =================================================
        // PERNA ESQUERDA
        // quadril -> joelho -> pé
        // =================================================
        int quadrilEsquerdoX
                = x + (int) (22 * escala);

        int quadrilEsquerdoY
                = corpoY + (int) (100 * escala);

        int joelhoEsquerdoX
                = x
                + (int) (10 * escala)
                - passo / 2
                - aberturaAgachado
                + idleAlternado;

        int joelhoEsquerdoY
                = corpoY
                + (int) (136 * escala)
                - encolhimento / 2
                + idleY;

        int peEsquerdoX
                = x
                - passo
                - aberturaAgachado
                + idleX;

        int peEsquerdoY
                = corpoY
                + (int) (174 * escala)
                - encolhimento;

        // =================================================
        // PERNA DIREITA
        // =================================================
        int quadrilDireitoX
                = x + (int) (52 * escala);

        int quadrilDireitoY
                = corpoY + (int) (100 * escala);

        int joelhoDireitoX
                = x
                + (int) (66 * escala)
                + passo / 2
                + aberturaAgachado
                - idleAlternado;

        int joelhoDireitoY
                = corpoY
                + (int) (137 * escala)
                - encolhimento / 2
                - idleY;

        int peDireitoX
                = x
                + (int) (82 * escala)
                + passo
                + aberturaAgachado
                - idleX;

        int peDireitoY
                = corpoY
                + (int) (171 * escala)
                - encolhimento;

        // =================================================
        // TAUNT CRIADO NO MIAUSTUDIO
        // =================================================
        if (tauntVisualAtivo) {

            /*
             * Primeiro voltamos exatamente para a geometria-base
             * usada pelo editor. Assim o TAUNT não herda os pequenos
             * deslocamentos do sistema antigo de idle.
             */

            // Ombros
            ombroEsquerdoX =
                    x + (int) (3 * escala);

            ombroEsquerdoY =
                    corpoY + (int) (22 * escala);

            ombroDireitoX =
                    x + (int) (73 * escala);

            ombroDireitoY =
                    corpoY + (int) (20 * escala);

            // Braço esquerdo
            cotoveloEsquerdoX =
                    x
                    - (int) (14 * escala)
                    + (int) (
                            quadroTaunt[T_COT_ESQ_X]
                            * escala
                    );

            cotoveloEsquerdoY =
                    corpoY
                    + (int) (42 * escala)
                    + (int) (
                            quadroTaunt[T_COT_ESQ_Y]
                            * escala
                    );

            maoEsquerdaX =
                    x
                    + (int) (3 * escala)
                    + (int) (
                            quadroTaunt[T_MAO_ESQ_X]
                            * escala
                    );

            maoEsquerdaY =
                    corpoY
                    + (int) (58 * escala)
                    + (int) (
                            quadroTaunt[T_MAO_ESQ_Y]
                            * escala
                    );

            // Braço direito
            cotoveloDireitoX =
                    x
                    + (int) (95 * escala)
                    + (int) (
                            quadroTaunt[T_COT_DIR_X]
                            * escala
                    );

            cotoveloDireitoY =
                    corpoY
                    + (int) (36 * escala)
                    + (int) (
                            quadroTaunt[T_COT_DIR_Y]
                            * escala
                    );

            maoDireitaX =
                    x
                    + (int) (111 * escala)
                    + (int) (
                            quadroTaunt[T_MAO_DIR_X]
                            * escala
                    );

            maoDireitaY =
                    corpoY
                    + (int) (29 * escala)
                    + (int) (
                            quadroTaunt[T_MAO_DIR_Y]
                            * escala
                    );

            // Quadris
            quadrilEsquerdoX =
                    x + (int) (22 * escala);

            quadrilEsquerdoY =
                    corpoY + (int) (100 * escala);

            quadrilDireitoX =
                    x + (int) (52 * escala);

            quadrilDireitoY =
                    corpoY + (int) (100 * escala);

            // Perna esquerda
            joelhoEsquerdoX =
                    x
                    + (int) (10 * escala)
                    + (int) (
                            quadroTaunt[T_JOEL_ESQ_X]
                            * escala
                    );

            joelhoEsquerdoY =
                    corpoY
                    + (int) (136 * escala)
                    + (int) (
                            quadroTaunt[T_JOEL_ESQ_Y]
                            * escala
                    );

            peEsquerdoX =
                    x
                    + (int) (
                            quadroTaunt[T_PE_ESQ_X]
                            * escala
                    );

            peEsquerdoY =
                    corpoY
                    + (int) (174 * escala)
                    + (int) (
                            quadroTaunt[T_PE_ESQ_Y]
                            * escala
                    );

            // Perna direita
            joelhoDireitoX =
                    x
                    + (int) (66 * escala)
                    + (int) (
                            quadroTaunt[T_JOEL_DIR_X]
                            * escala
                    );

            joelhoDireitoY =
                    corpoY
                    + (int) (137 * escala)
                    + (int) (
                            quadroTaunt[T_JOEL_DIR_Y]
                            * escala
                    );

            peDireitoX =
                    x
                    + (int) (82 * escala)
                    + (int) (
                            quadroTaunt[T_PE_DIR_X]
                            * escala
                    );

            peDireitoY =
                    corpoY
                    + (int) (171 * escala)
                    + (int) (
                            quadroTaunt[T_PE_DIR_Y]
                            * escala
                    );
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
                x - (int) (8 * escala),
                corpoY + (int) (82 * escala),
                (int) (96 * escala),
                (int) (12 * escala)
        );
}


    @Override
public void desenhar(Graphics2D g2) {

    prepararDesenho(g2);

    if (isRolando()) {

        desenharRolamento(g2);

        return;
    }

    if (isDeitado()) {

        desenharDeitado(g2);

        return;
    }

    desenharEmPe(
            g2,
            false
    );
}

    @Override
    protected double getProfundidadeAgachamento() {

        return 24;
    }

    @Override
    protected double getFlexaoPernasAgachado() {

        return 34;
    }
}
