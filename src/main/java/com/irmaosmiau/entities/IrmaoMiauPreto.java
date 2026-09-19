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
                "Jiu-Jitsu"
        );
    }

    @Override
    public void desenhar(Graphics2D g2) {

        prepararDesenho(g2);
        desenharCabeca(g2);

        /*
         * O Preto possui uma passada mais leve e móvel.
         */
        int passo =
                (int) (getOscilacaoPasso() * 1.15);

        int encolhimento =
                getEncolhimentoAgachado();

        int aberturaAgachado =
                isAgachado()
                        ? (int) (8 * escala)
                        : 0;

        /*
         * O Preto é mais inquieto que o Branco:
         * as amplitudes de idle são um pouco maiores.
         */
        int idleX = 0;
        int idleY = 0;
        int idleAlternado = 0;

        if (isIdleEmPe()) {

            idleX =
                    getIdleX(
                            Math.max(
                                    1,
                                    (int) Math.round(4 * escala)
                            )
                    );

            idleY =
                    getIdleY(
                            Math.max(
                                    1,
                                    (int) Math.round(3 * escala)
                            )
                    );

            idleAlternado =
                    getIdleAlternado(
                            Math.max(
                                    1,
                                    (int) Math.round(4 * escala)
                            )
                    );
        }

        if (isIdleAgachado()) {

            idleX =
                    getIdleX(
                            Math.max(
                                    1,
                                    (int) Math.round(5 * escala)
                            )
                    );

            idleY =
                    getIdleY(
                            Math.max(
                                    1,
                                    (int) Math.round(2 * escala)
                            )
                    );

            idleAlternado =
                    getIdleAlternado(
                            Math.max(
                                    1,
                                    (int) Math.round(4 * escala)
                            )
                    );
        }

        if (isTaunt()) {

            idleX =
                    getIdleX(
                            Math.max(
                                    1,
                                    (int) Math.round(3 * escala)
                            )
                    );

            idleY =
                    getIdleY(
                            Math.max(
                                    1,
                                    (int) Math.round(2 * escala)
                            )
                    );

            idleAlternado =
                    getIdleAlternado(
                            Math.max(
                                    1,
                                    (int) Math.round(2 * escala)
                            )
                    );
        }

        int corpoY =
                getYVisual()
                + (int) (70 * escala);

        // =========================
        // CORPO
        // =========================

        Polygon corpo =
                new Polygon();

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

        int ombroEsquerdoX =
                x + (int) (5 * escala);

        int ombroEsquerdoY =
                corpoY + (int) (22 * escala);

        int cotoveloEsquerdoX =
                x
                - (int) (8 * escala)
                + passo / 4
                - aberturaAgachado / 2
                + idleX;

        int cotoveloEsquerdoY =
                corpoY
                + (int) (52 * escala)
                + idleY;

        int maoEsquerdaX =
                x
                + (int) (35 * escala)
                + passo / 5
                + idleAlternado;

        int maoEsquerdaY =
                corpoY
                + (int) (57 * escala)
                - idleY;

        // =================================================
        // BRAÇO DIREITO
        // aberto e baixo na lateral
        // =================================================

        int ombroDireitoX =
                x + (int) (70 * escala);

        int ombroDireitoY =
                corpoY + (int) (23 * escala);

        int cotoveloDireitoX =
                x
                + (int) (88 * escala)
                - passo / 4
                + aberturaAgachado / 2
                - idleAlternado;

        int cotoveloDireitoY =
                corpoY
                + (int) (48 * escala)
                - idleY;

        int maoDireitaX =
                x
                + (int) (108 * escala)
                - passo / 5
                + aberturaAgachado
                - idleX;

        int maoDireitaY =
                corpoY
                + (int) (62 * escala)
                + idleY;

        // =================================================
        // PERNA ESQUERDA
        // =================================================

        int quadrilEsquerdoX =
                x + (int) (24 * escala);

        int quadrilEsquerdoY =
                corpoY + (int) (97 * escala);

        int joelhoEsquerdoX =
                x
                + (int) (15 * escala)
                - passo / 2
                - aberturaAgachado
                + idleAlternado;

        int joelhoEsquerdoY =
                corpoY
                + (int) (132 * escala)
                - encolhimento / 2
                + idleY;

        int peEsquerdoX =
                x
                + (int) (4 * escala)
                - passo
                - aberturaAgachado
                + idleX;

        int peEsquerdoY =
                corpoY
                + (int) (171 * escala)
                - encolhimento;

        // =================================================
        // PERNA DIREITA
        // =================================================

        int quadrilDireitoX =
                x + (int) (50 * escala);

        int quadrilDireitoY =
                corpoY + (int) (97 * escala);

        int joelhoDireitoX =
                x
                + (int) (59 * escala)
                + passo / 2
                + aberturaAgachado
                - idleAlternado;

        int joelhoDireitoY =
                corpoY
                + (int) (132 * escala)
                - encolhimento / 2
                - idleY;

        int peDireitoX =
                x
                + (int) (72 * escala)
                + passo
                + aberturaAgachado
                - idleX;

        int peDireitoY =
                corpoY
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

            ombroEsquerdoX =
                    x + (int) (8 * escala);

            ombroEsquerdoY =
                    corpoY + (int) (22 * escala);

            cotoveloEsquerdoX =
                    x
                    + (int) (10 * escala)
                    + idleX / 2;

            cotoveloEsquerdoY =
                    corpoY
                    + (int) (57 * escala)
                    + idleY;

            maoEsquerdaX =
                    x
                    + (int) (12 * escala)
                    + idleAlternado;

            maoEsquerdaY =
                    corpoY
                    + (int) (88 * escala)
                    + idleY;


            ombroDireitoX =
                    x + (int) (67 * escala);

            ombroDireitoY =
                    corpoY + (int) (22 * escala);

            cotoveloDireitoX =
                    x
                    + (int) (65 * escala)
                    - idleX / 2;

            cotoveloDireitoY =
                    corpoY
                    + (int) (57 * escala)
                    - idleY;

            maoDireitaX =
                    x
                    + (int) (63 * escala)
                    - idleAlternado;

            maoDireitaY =
                    corpoY
                    + (int) (88 * escala)
                    + idleY;


            quadrilEsquerdoX =
                    x + (int) (29 * escala);

            joelhoEsquerdoX =
                    x
                    + (int) (30 * escala)
                    + idleX / 2;

            peEsquerdoX =
                    x
                    + (int) (29 * escala)
                    + idleX;


            quadrilDireitoX =
                    x + (int) (45 * escala);

            joelhoDireitoX =
                    x
                    + (int) (45 * escala)
                    - idleX / 2;

            peDireitoX =
                    x
                    + (int) (46 * escala)
                    - idleX;

            joelhoEsquerdoY =
                    corpoY
                    + (int) (133 * escala)
                    + idleY;

            joelhoDireitoY =
                    corpoY
                    + (int) (133 * escala)
                    - idleY;

            peEsquerdoY =
                    corpoY + (int) (171 * escala);

            peDireitoY =
                    corpoY + (int) (171 * escala);
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

        int larguraMao =
                (int) (20 * escala);

        int alturaMao =
                (int) (18 * escala);

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

        int larguraPe =
                (int) (25 * escala);

        int alturaPe =
                (int) (17 * escala);

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
    protected double getProfundidadeAgachamento() {

        return 14;
    }

    @Override
    protected double getFlexaoPernasAgachado() {

        return 25;
    }
}
