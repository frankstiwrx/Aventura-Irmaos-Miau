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
                        25, // fôlego
                        42, // força
                        22, // velocidade
                        25, // agilidade
                        25, // técnica
                        22, // recuperação
                        72 // peso
                )
        );
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

private void desenharDeitado(Graphics2D g2) {

    Graphics2D copia =
            (Graphics2D) g2.create();

    prepararDesenho(copia);

    /*
     * Em vez de redesenhar o personagem,
     * usamos exatamente a pose normal
     * e giramos o conjunto inteiro em 90 graus.
     *
     * O ponto de rotação fica próximo aos pés.
     */
    double pontoX =
            x + 35 * escala;

    double pontoY =
            y + 244 * escala;

    copia.rotate(
            Math.PI / 2.0,
            pontoX,
            pontoY
    );

    /*
     * Parado no chão:
     * mantém a pose neutra.
     *
     * Rastejando:
     * reutiliza exatamente a mesma animação
     * de caminhada, só que com todo o Miau
     * rotacionado em 90 graus.
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
        // TAUNT / POSTURA NATURAL
        // =================================================
        if (isTaunt()) {

            /*
             * Depois de alguns segundos parado,
             * o Branco relaxa a postura:
             *
             * - braços quase retos para baixo;
             * - pernas mais próximas;
             * - pequenas poses continuam acontecendo.
             */
            ombroEsquerdoX
                    = x + (int) (7 * escala);

            ombroEsquerdoY
                    = corpoY + (int) (22 * escala);

            cotoveloEsquerdoX
                    = x
                    + (int) (9 * escala)
                    + idleX / 2;

            cotoveloEsquerdoY
                    = corpoY
                    + (int) (56 * escala)
                    + idleY;

            maoEsquerdaX
                    = x
                    + (int) (11 * escala)
                    + idleAlternado;

            maoEsquerdaY
                    = corpoY
                    + (int) (89 * escala)
                    + idleY;

            ombroDireitoX
                    = x + (int) (69 * escala);

            ombroDireitoY
                    = corpoY + (int) (22 * escala);

            cotoveloDireitoX
                    = x
                    + (int) (68 * escala)
                    - idleX / 2;

            cotoveloDireitoY
                    = corpoY
                    + (int) (56 * escala)
                    - idleY;

            maoDireitaX
                    = x
                    + (int) (67 * escala)
                    - idleAlternado;

            maoDireitaY
                    = corpoY
                    + (int) (89 * escala)
                    + idleY;

            quadrilEsquerdoX
                    = x + (int) (29 * escala);

            joelhoEsquerdoX
                    = x
                    + (int) (29 * escala)
                    + idleX / 2;

            peEsquerdoX
                    = x
                    + (int) (27 * escala)
                    + idleX;

            quadrilDireitoX
                    = x + (int) (47 * escala);

            joelhoDireitoX
                    = x
                    + (int) (47 * escala)
                    - idleX / 2;

            peDireitoX
                    = x
                    + (int) (50 * escala)
                    - idleX;

            joelhoEsquerdoY
                    = corpoY
                    + (int) (137 * escala)
                    + idleY;

            joelhoDireitoY
                    = corpoY
                    + (int) (137 * escala)
                    - idleY;

            peEsquerdoY
                    = corpoY + (int) (174 * escala);

            peDireitoY
                    = corpoY + (int) (174 * escala);
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
