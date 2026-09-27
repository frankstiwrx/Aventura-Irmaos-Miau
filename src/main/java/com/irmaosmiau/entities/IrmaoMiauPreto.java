package com.irmaosmiau.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;

public class IrmaoMiauPreto extends IrmaoMiau {

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
    // TAUNT - ANIMAÇÃO CRIADA NO MIAUSTUDIO
    // =============================================================
    private static final int[][] TAUNTPRETO_MIAUSTUDIO = {
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
    private static final int[] SEQUENCIA_TAUNT_PRETO_MIAUSTUDIO = {
        0, 1, 2, 3, 4, 3, 2, 1
    };

    private static final long TEMPO_QUADRO_TAUNT_PRETO_MS = 230L;

    private boolean tauntPretoMiauStudioEstavaAtivo = false;
    private long inicioTauntPretoMiauStudio = 0L;

    private int getIndiceQuadroTauntPretoMiauStudio(
            boolean tauntAtivo
    ) {

        long agora = System.currentTimeMillis();

        if (!tauntAtivo) {

            tauntPretoMiauStudioEstavaAtivo = false;
            inicioTauntPretoMiauStudio = 0L;

            return 0;
        }

        if (!tauntPretoMiauStudioEstavaAtivo) {

            tauntPretoMiauStudioEstavaAtivo = true;
            inicioTauntPretoMiauStudio = agora;
        }

        long tempoDecorrido =
                agora - inicioTauntPretoMiauStudio;

        int indiceSequencia =
                (int) (
                        tempoDecorrido
                        / TEMPO_QUADRO_TAUNT_PRETO_MS
                )
                % SEQUENCIA_TAUNT_PRETO_MIAUSTUDIO.length;

        return SEQUENCIA_TAUNT_PRETO_MIAUSTUDIO[
                indiceSequencia
        ];
    }

    // =============================================================
    // ROLAMENTO - ANIMAÇÃO CRIADA NO MIAUSTUDIO
    // =============================================================
    private static final int[][] ROLAMENTOPRETO_MIAUSTUDIO = {
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
        interpolarRolamentoMiauStudio(
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

    private int[] interpolarRolamentoMiauStudio(
            double progresso
    ) {

        double limitado =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                progresso
                        )
                );

        double posicao =
                limitado
                * (ROLAMENTOPRETO_MIAUSTUDIO.length - 1);

        int quadroA =
                (int) Math.floor(posicao);

        int quadroB =
                Math.min(
                        quadroA + 1,
                        ROLAMENTOPRETO_MIAUSTUDIO.length - 1
                );

        double t =
                posicao - quadroA;

        int[] a =
                ROLAMENTOPRETO_MIAUSTUDIO[quadroA];

        int[] b =
                ROLAMENTOPRETO_MIAUSTUDIO[quadroB];

        int[] resultado =
                new int[a.length];

        for (int i = 0; i < resultado.length; i++) {

            /*
             * cenaX, linha, escala e orientação são metadados
             * do editor. Mantemos o valor do primeiro quadro,
             * pois o renderer do jogo não usa esses campos.
             */
            if (i <= 3) {
                resultado[i] = a[i];
                continue;
            }

            /*
             * Rotações usam o menor caminho angular.
             *
             * Exemplo importante do Preto:
             * -180° -> 88° continua o giro por -92°,
             * em vez de voltar 268° no sentido contrário.
             */
            if (
                    i == A_ROT_CINTURA
                    || i == A_ROT_TRONCO
                    || i == A_ROT_GLOBAL
                    || i == A_ROT_CABECA
            ) {

                resultado[i] =
                        (int) Math.round(
                                interpolarAngulo(
                                        a[i],
                                        b[i],
                                        t
                                )
                        );

            } else {

                resultado[i] =
                        (int) Math.round(
                                a[i]
                                + (b[i] - a[i]) * t
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

private void desenharRolamentoAntigo(
        Graphics2D g2
) {

    double progresso =
            getProgressoRolamento();

    if (progresso < 0.12) {

        desenharEntradaRolamento(
                g2,
                progresso / 0.12
        );

    } else if (progresso < 0.28) {

        desenharMergulhoRolamento(
                g2,
                (progresso - 0.12) / 0.16
        );

    } else if (progresso < 0.68) {

        desenharGiroRolamento(
                g2,
                (progresso - 0.28) / 0.40
        );

    } else if (progresso < 0.86) {

        desenharSaidaRolamento(
                g2,
                (progresso - 0.68) / 0.18
        );

    } else {

        desenharLevantandoRolamento(
                g2,
                (progresso - 0.86) / 0.14
        );
    }
}
    private void desenharEntradaRolamento(
        Graphics2D g2,
        double fase
) {

    Graphics2D copia =
            (Graphics2D) g2.create();

    prepararDesenho(copia);

    double centroX =
            x + 37 * escala;

    double centroY =
            y + (82 + 18 * fase) * escala;

    /*
     * O Preto já inclina bastante
     * durante a preparação.
     */
    double inclinacao =
            fase * 0.42;

    double aumento =
            1.0 + fase * 0.08;

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
    // CORPO
    // =========================

    copia.setColor(
            corKimono
    );

    int largura =
            (int) Math.round(
                    (65 + fase * 10)
                    * escala
            );

    int altura =
            (int) Math.round(
                    (100 - fase * 38)
                    * escala
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

    int cabeca =
            (int) Math.round(
                    55 * escala
            );

    /*
     * A cabeça começa a avançar,
     * preparando o mergulho.
     */
    int cabecaX =
            (int) Math.round(
                    centroX
                    - cabeca / 2.0
                    - fase * 14 * escala
            );

    int cabecaY =
            (int) Math.round(
                    centroY
                    - 73 * escala
                    + fase * 34 * escala
            );

    copia.fillOval(
            cabecaX,
            cabecaY,
            cabeca,
            cabeca
    );

    // =========================
    // BRAÇO DE APOIO
    // =========================

    copia.setColor(
            corKimono
    );

    copia.setStroke(
            new BasicStroke(
                    (float) Math.max(
                            2,
                            12 * escala
                    ),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            )
    );

    copia.drawLine(
            (int) (centroX - 18 * escala),
            (int) (centroY - 5 * escala),
            (int) (
                    centroX
                    - (28 + 18 * fase) * escala
            ),
            (int) (
                    centroY
                    + (22 + 18 * fase) * escala
            )
    );

    /*
     * O outro braço recolhe.
     */
    copia.drawLine(
            (int) (centroX + 17 * escala),
            (int) (centroY),
            (int) (
                    centroX
                    + (25 - 14 * fase) * escala
            ),
            (int) (
                    centroY
                    + 28 * escala
            )
    );

    // =========================
    // PERNAS
    // =========================

    copia.setStroke(
            new BasicStroke(
                    (float) Math.max(
                            2,
                            14 * escala
                    ),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            )
    );

    copia.drawLine(
            (int) (centroX - 15 * escala),
            (int) (centroY + 35 * escala),
            (int) (
                    centroX
                    - (31 + fase * 8) * escala
            ),
            (int) (
                    centroY
                    + (63 - fase * 15) * escala
            )
    );

    copia.drawLine(
            (int) (centroX + 15 * escala),
            (int) (centroY + 35 * escala),
            (int) (
                    centroX
                    + (36 + fase * 10) * escala
            ),
            (int) (
                    centroY
                    + (60 - fase * 18) * escala
            )
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
            x + 37 * escala;

    double centroY =
            y + (96 + 5 * fase) * escala;

    /*
     * Rotação forte durante o mergulho.
     */
    double angulo =
            0.42
            + fase * 1.12;

    double aumento =
            1.08
            + fase * 0.12;

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
    // CORPO ALONGADO
    // =========================

    copia.setColor(
            corKimono
    );

    int largura =
            (int) Math.round(
                    (82 - fase * 9)
                    * escala
            );

    int altura =
            (int) Math.round(
                    (58 + fase * 4)
                    * escala
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
            (int) Math.round(28 * escala),
            (int) Math.round(28 * escala)
    );

    /*
     * Aqui a cabeça já desapareceu:
     * ela está recolhida.
     */

    // =========================
    // PATA DE APOIO
    // =========================

    copia.setColor(
            corPelo
    );

    copia.fillOval(
            (int) Math.round(
                    centroX
                    - (43 - fase * 8)
                    * escala
            ),
            (int) Math.round(
                    centroY
                    + 10 * escala
            ),
            (int) Math.round(
                    28 * escala
            ),
            (int) Math.round(
                    18 * escala
            )
    );

    // =========================
    // PERNA TRASEIRA RECOLHENDO
    // =========================

    copia.fillOval(
            (int) Math.round(
                    centroX
                    + (17 - fase * 7)
                    * escala
            ),
            (int) Math.round(
                    centroY
                    + (15 - fase * 5)
                    * escala
            ),
            (int) Math.round(
                    29 * escala
            ),
            (int) Math.round(
                    19 * escala
            )
    );

    copia.dispose();
}
    
    private void desenharGiroRolamento(
        Graphics2D g2,
        double fase
) {

    Graphics2D copia =
            (Graphics2D) g2.create();

    prepararDesenho(copia);

    double centroX =
            x + 37 * escala;

    double centroY =
            y + 195 * escala;

    /*
     * Preto continua grande durante
     * a cambalhota, mas ligeiramente
     * menos redondo que o Branco.
     */
    double aumento =
            1.18
            + Math.sin(
                    fase * Math.PI
            ) * 0.07;

    /*
     * Uma volta completa no miolo.
     */
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

    copia.setColor(
            corKimono
    );

    int largura =
            (int) Math.round(
                    78 * escala
            );

    int altura =
            (int) Math.round(
                    57 * escala
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
                    31 * escala
            ),
            (int) Math.round(
                    31 * escala
            )
    );

    /*
     * Não desenhamos cabeça separada.
     */

    // =========================
    // PATAS
    // =========================

    copia.setColor(
            corPelo
    );

    /*
     * Uma pata mais próxima do centro.
     */
    copia.fillOval(
            (int) Math.round(
                    centroX
                    - 34 * escala
            ),
            (int) Math.round(
                    centroY
                    + 5 * escala
            ),
            (int) Math.round(
                    23 * escala
            ),
            (int) Math.round(
                    17 * escala
            )
    );

    /*
     * Outra mais projetada.
     */
    copia.fillOval(
            (int) Math.round(
                    centroX
                    + 12 * escala
            ),
            (int) Math.round(
                    centroY
                    + 8 * escala
            ),
            (int) Math.round(
                    29 * escala
            ),
            (int) Math.round(
                    18 * escala
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
            x + 37 * escala;

    double centroY =
            y + (100 - 7 * fase) * escala;

    double angulo =
            1.45
            - fase * 1.15;

    double aumento =
            1.20
            - fase * 0.10;

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
                    (78 - fase * 12)
                    * escala
            );

    int altura =
            (int) Math.round(
                    (58 + fase * 35)
                    * escala
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
                    26 * escala
            ),
            (int) Math.round(
                    26 * escala
            )
    );

    // =========================
    // PERNA DE SAÍDA
    // =========================

    copia.setColor(
            corKimono
    );

    copia.setStroke(
            new BasicStroke(
                    (float) Math.max(
                            2,
                            13 * escala
                    ),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            )
    );

    /*
     * Uma perna já procura o chão.
     */
    copia.drawLine(
            (int) (centroX + 10 * escala),
            (int) (centroY + 18 * escala),
            (int) (
                    centroX
                    + (25 + 22 * fase)
                    * escala
            ),
            (int) (
                    centroY
                    + (25 + 35 * fase)
                    * escala
            )
    );

    // PATA
    copia.setColor(
            corPelo
    );

    copia.fillOval(
            (int) (
                    centroX
                    + (32 + 17 * fase)
                    * escala
            ),
            (int) (
                    centroY
                    + (42 + 18 * fase)
                    * escala
            ),
            (int) (28 * escala),
            (int) (18 * escala)
    );

    /*
     * A cabeça só começa a reaparecer
     * na segunda metade.
     */
    if (fase > 0.50) {

        double aparicao =
                (fase - 0.50) / 0.50;

        int cabeca =
                (int) Math.round(
                        55
                        * escala
                        * aparicao
                );

        copia.setColor(
                corPelo
        );

        copia.fillOval(
                (int) Math.round(
                        centroX
                        - cabeca / 2.0
                        - 10 * escala
                ),
                (int) Math.round(
                        centroY
                        - 52
                        * escala
                        * aparicao
                ),
                cabeca,
                cabeca
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
            x + 37 * escala;

    double centroY =
            y + (96 - 16 * fase)
            * escala;

    double aumento =
            1.10
            - fase * 0.10;

    double inclinacao =
            0.30
            * (1.0 - fase);

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
    // CORPO
    // =========================

    copia.setColor(
            corKimono
    );

    int largura =
            (int) Math.round(
                    (65 + fase * 10)
                    * escala
            );

    int altura =
            (int) Math.round(
                    (92 + fase * 8)
                    * escala
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
            (int) Math.round(23 * escala),
            (int) Math.round(23 * escala)
    );

    // =========================
    // CABEÇA
    // =========================

    copia.setColor(
            corPelo
    );

    int cabeca =
            (int) Math.round(
                    55 * escala
            );

    copia.fillOval(
            (int) Math.round(
                    centroX
                    - cabeca / 2.0
            ),
            (int) Math.round(
                    centroY
                    - 72 * escala
                    + 12
                    * escala
                    * (1.0 - fase)
            ),
            cabeca,
            cabeca
    );

    // =========================
    // PERNAS
    // =========================

    copia.setColor(
            corKimono
    );

    copia.setStroke(
            new BasicStroke(
                    (float) Math.max(
                            2,
                            14 * escala
                    ),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            )
    );

    /*
     * Preto termina com uma base
     * um pouco mais aberta.
     */
    double abertura =
            19 + fase * 20;

    copia.drawLine(
            (int) (
                    centroX
                    - 12 * escala
            ),
            (int) (
                    centroY
                    + 35 * escala
            ),
            (int) (
                    centroX
                    - abertura * escala
            ),
            (int) (
                    centroY
                    + 68 * escala
            )
    );

    copia.drawLine(
            (int) (
                    centroX
                    + 12 * escala
            ),
            (int) (
                    centroY
                    + 35 * escala
            ),
            (int) (
                    centroX
                    + abertura * escala
            ),
            (int) (
                    centroY
                    + 68 * escala
            )
    );

    copia.dispose();
}
    
    
    // =============================================================
    // ARRASTAR NO CHÃO - ANIMAÇÃO CRIADA NO MIAUSTUDIO
    // =============================================================
    private static final int[][] ARRASTANDO_MIAUSTUDIO = {
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

        desenharPoseMiauStudio(
                g2,
                ARRASTANDO_MIAUSTUDIO[indiceQuadro],
                40
        );
    }

    /*
     * Renderer comum das poses criadas no MiauStudio.
     *
     * É usado tanto pelo rastejo quanto pelo novo rolamento.
     * O parâmetro deslocamentoVisualY permite que a animação
     * de chão continue 40 px mais baixa sem afetar o rolamento.
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
                getIndiceQuadroTauntPretoMiauStudio(
                        tauntVisualAtivo
                );

        // =========================
        // ROLAMENTO
        // =========================
        if (isRolando()) {

            desenharRolamento(g2);

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
                    TAUNTPRETO_MIAUSTUDIO[
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
