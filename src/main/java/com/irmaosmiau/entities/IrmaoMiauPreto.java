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

    int passo =
            getOscilacaoPasso();

    int encolhimento =
            getEncolhimentoAgachado();

    int aberturaAgachado =
            isAgachado()
            ? (int) (8 * escala)
            : 0;

    int corpoY =
            getYVisual()
            + (int) (70 * escala);

    // =========================
    // CORPO
    // =========================

    Polygon corpo =
            new Polygon();

    corpo.addPoint(
            x - (int) (10 * escala),
            corpoY
    );

    corpo.addPoint(
            x + (int) (80 * escala),
            corpoY
    );

    corpo.addPoint(
            x + (int) (68 * escala),
            corpoY + (int) (100 * escala)
    );

    corpo.addPoint(
            x + (int) (2 * escala),
            corpoY + (int) (100 * escala)
    );

    g2.setColor(corKimono);
    g2.fillPolygon(corpo);

    g2.setColor(Color.BLACK);

    g2.setStroke(
            new BasicStroke(
                    (float) (4 * escala)
            )
    );

    //g2.drawPolygon(corpo);

    // =========================
    // BRAÇOS
    // =========================

    g2.setColor(corKimono);

    g2.setStroke(
            new BasicStroke(
                    (float) (18 * escala),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            )
    );

    int maoEsquerdaX =
            x
            - (int) (20 * escala)
            + passo
            - aberturaAgachado;

    int maoEsquerdaY =
            corpoY
            + (int) (55 * escala);

    int maoDireitaX =
            x
            + (int) (90 * escala)
            - passo
            + aberturaAgachado;

    int maoDireitaY =
            corpoY
            + (int) (55 * escala);

    g2.drawLine(
            x,
            corpoY + (int) (20 * escala),
            maoEsquerdaX,
            maoEsquerdaY
    );

    g2.drawLine(
            x + (int) (70 * escala),
            corpoY + (int) (20 * escala),
            maoDireitaX,
            maoDireitaY
    );

    // =========================
    // PERNAS
    // =========================

    g2.setColor(corKimono);

    int peEsquerdoX =
            x
            - (int) (10 * escala)
            - passo
            - aberturaAgachado;

    int peEsquerdoY =
            corpoY
            + (int) (145 * escala)
            - encolhimento;

    int peDireitoX =
            x
            + (int) (85 * escala)
            + passo
            + aberturaAgachado;

    int peDireitoY =
            corpoY
            + (int) (145 * escala)
            - encolhimento;

    g2.drawLine(
            x + (int) (20 * escala),
            corpoY + (int) (95 * escala),
            peEsquerdoX,
            peEsquerdoY
    );

    g2.drawLine(
            x + (int) (55 * escala),
            corpoY + (int) (95 * escala),
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
    // FAIXA
    // =========================

    g2.setColor(
            new Color(55, 55, 220)
    );

    g2.fillRect(
            x - (int) (10 * escala),
            corpoY + (int) (80 * escala),
            (int) (100 * escala),
            (int) (12 * escala)
    );
}
}
