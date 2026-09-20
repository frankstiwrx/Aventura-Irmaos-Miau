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

private void desenharRolamento(
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
    
    private void desenharDeitado(Graphics2D g2) {

    Graphics2D copia =
            (Graphics2D) g2.create();

    prepararDesenho(copia);

    /*
     * O Miau Preto deitado reutiliza exatamente
     * o próprio desenho normal, apenas girado
     * em 90 graus.
     *
     * O ponto de rotação fica próximo aos pés,
     * preservando as dimensões do personagem.
     */
    double pontoX =
            x + 37 * escala;

    double pontoY =
            y + 241 * escala;

    copia.rotate(
            Math.PI / 2.0,
            pontoX,
            pontoY
    );

    /*
     * Parado no chão:
     * usa a pose neutra.
     *
     * Rastejando:
     * reutiliza a animação normal de caminhada,
     * também rotacionada em 90 graus.
     */
    boolean animarRastejo =
            isAndando();

    desenharEmPe(
            copia,
            !animarRastejo
    );

    copia.dispose();
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

        return 14;
    }

    @Override
    protected double getFlexaoPernasAgachado() {

        return 25;
    }
}
