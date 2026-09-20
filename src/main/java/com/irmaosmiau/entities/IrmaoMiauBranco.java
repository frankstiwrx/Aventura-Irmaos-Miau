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

    @Override
    public void desenhar(Graphics2D g2) {

        prepararDesenho(g2);
        desenharCabeca(g2);

        /*
         * O Branco possui uma passada mais firme/pesada.
         */
        int passo
                = (int) (getOscilacaoPasso() * 0.75);

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

        if (isIdleEmPe()) {

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

        if (isIdleAgachado()) {

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

        if (isTaunt()) {

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
    protected double getProfundidadeAgachamento() {

        return 24;
    }

    @Override
    protected double getFlexaoPernasAgachado() {

        return 34;
    }
}
