package com.irmaosmiau.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

public class IrmaoMiau {

    private int x;
    private int y;
    private double escala;
    private boolean kimonoPreto;

    public IrmaoMiau(int x, int y, double escala, boolean kimonoPreto) {
        this.x = x;
        this.y = y;
        this.escala = escala;
        this.kimonoPreto = kimonoPreto;
    }

    public void desenhar(Graphics2D g2) {

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        Color corPelo;
        Color corKimono;
        Color corRosto;

        if (kimonoPreto) {

            // Miau branco
            corPelo = Color.WHITE;
            corKimono = Color.BLACK;
            corRosto = Color.BLACK;

        } else {

            // Miau preto
            corPelo = Color.BLACK;
            corKimono = Color.WHITE;
            corRosto = Color.WHITE;
        }

        int cabeca = (int) (70 * escala);

        // ========================
        // CABEÇA
        // ========================
        g2.setColor(corPelo);

        g2.fillOval(x, y, cabeca, cabeca);

        // Contorno da cabeça
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke((float) (5 * escala)));
        g2.drawOval(x, y, cabeca, cabeca);

        // ========================
        // ORELHAS
        // ========================
        Polygon orelhaEsquerda = new Polygon();

        orelhaEsquerda.addPoint(
                x + (int) (8 * escala),
                y + (int) (8 * escala)
        );

        orelhaEsquerda.addPoint(
                x + (int) (16 * escala),
                y - (int) (25 * escala)
        );

        orelhaEsquerda.addPoint(
                x + (int) (28 * escala),
                y + (int) (8 * escala)
        );

        Polygon orelhaDireita = new Polygon();

        orelhaDireita.addPoint(
                x + (int) (43 * escala),
                y + (int) (8 * escala)
        );

        orelhaDireita.addPoint(
                x + (int) (55 * escala),
                y - (int) (25 * escala)
        );

        orelhaDireita.addPoint(
                x + (int) (63 * escala),
                y + (int) (8 * escala)
        );

        g2.setColor(Color.BLACK);

        g2.fillPolygon(orelhaEsquerda);
        g2.fillPolygon(orelhaDireita);

        // ========================
        // OLHOS
        // ========================
        if (kimonoPreto) {
            g2.setColor(Color.BLACK);
        } else {
            g2.setColor(Color.WHITE);
        }

        int olho = (int) (6 * escala);

        g2.fillOval(
                x + (int) (22 * escala),
                y + (int) (30 * escala),
                olho,
                olho
        );

        g2.fillOval(
                x + (int) (45 * escala),
                y + (int) (30 * escala),
                olho,
                olho
        );

        // ========================
// NARIZ
// ========================
        g2.setColor(new Color(220, 120, 140));

        Polygon nariz = new Polygon();

        nariz.addPoint(
                x + (int) (33 * escala),
                y + (int) (43 * escala)
        );

        nariz.addPoint(
                x + (int) (40 * escala),
                y + (int) (43 * escala)
        );

        nariz.addPoint(
                x + (int) (36 * escala),
                y + (int) (48 * escala)
        );

        g2.fillPolygon(nariz);

// ========================
// BOCA
// ========================
        g2.setColor(corRosto);

        g2.setStroke(
                new BasicStroke((float) (2 * escala))
        );

        int bocaX = x + (int) (36 * escala);
        int bocaY = y + (int) (48 * escala);

        g2.drawLine(
                bocaX,
                bocaY,
                bocaX,
                bocaY + (int) (5 * escala)
        );

        g2.drawLine(
                bocaX,
                bocaY + (int) (5 * escala),
                bocaX - (int) (5 * escala),
                bocaY + (int) (9 * escala)
        );

        g2.drawLine(
                bocaX,
                bocaY + (int) (5 * escala),
                bocaX + (int) (5 * escala),
                bocaY + (int) (9 * escala)
        );

        // ========================
        // CORPO / KIMONO
        // ========================
        int corpoY = y + cabeca;

        Polygon corpo = new Polygon();

        corpo.addPoint(
                x - (int) (15 * escala),
                corpoY
        );

        corpo.addPoint(
                x + (int) (85 * escala),
                corpoY
        );

        corpo.addPoint(
                x + (int) (65 * escala),
                corpoY + (int) (110 * escala)
        );

        corpo.addPoint(
                x + (int) (5 * escala),
                corpoY + (int) (110 * escala)
        );

        g2.setColor(corKimono);

        g2.fillPolygon(corpo);

        g2.setColor(Color.BLACK);

        g2.setStroke(
                new BasicStroke((float) (5 * escala))
        );

        g2.drawPolygon(corpo);

        // ========================
        // GOLA DO KIMONO
        // ========================
        g2.drawLine(
                x,
                corpoY,
                x + (int) (38 * escala),
                corpoY + (int) (65 * escala)
        );

        g2.drawLine(
                x + (int) (70 * escala),
                corpoY,
                x + (int) (38 * escala),
                corpoY + (int) (65 * escala)
        );

        // ========================
        // BRAÇOS
        // ========================
        // ========================
// BRAÇOS
// ========================
        g2.setColor(corKimono);

        g2.setStroke(
                new BasicStroke(
                        (float) (18 * escala),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        g2.drawLine(
                x - (int) (10 * escala),
                corpoY + (int) (15 * escala),
                x - (int) (45 * escala),
                corpoY + (int) (75 * escala)
        );

        g2.drawLine(
                x + (int) (80 * escala),
                corpoY + (int) (15 * escala),
                x + (int) (110 * escala),
                corpoY + (int) (75 * escala)
        );

        // ========================
// PERNAS
// ========================
        g2.setColor(corKimono);

        g2.drawLine(
                x + (int) (20 * escala),
                corpoY + (int) (105 * escala),
                x + (int) (20 * escala),
                corpoY + (int) (175 * escala)
        );

        g2.drawLine(
                x + (int) (55 * escala),
                corpoY + (int) (105 * escala),
                x + (int) (55 * escala),
                corpoY + (int) (175 * escala)
        );

        // ========================
// MÃOS / PATAS
// ========================
        g2.setColor(corPelo);

        int tamanhoMao = (int) (18 * escala);

        g2.fillRoundRect(
                x - (int) (54 * escala),
                corpoY + (int) (68 * escala),
                tamanhoMao,
                tamanhoMao,
                5,
                5
        );

        g2.fillRoundRect(
                x + (int) (101 * escala),
                corpoY + (int) (68 * escala),
                tamanhoMao,
                tamanhoMao,
                5,
                5
        );

// ========================
// PÉS
// ========================
        int larguraPe = (int) (22 * escala);
        int alturaPe = (int) (16 * escala);

        g2.fillRoundRect(
                x + (int) (9 * escala),
                corpoY + (int) (168 * escala),
                larguraPe,
                alturaPe,
                5,
                5
        );

        g2.fillRoundRect(
                x + (int) (44 * escala),
                corpoY + (int) (168 * escala),
                larguraPe,
                alturaPe,
                5,
                5
        );

        // ========================
        // FAIXA AZUL
        // ========================
        g2.setColor(new Color(55, 55, 220));

        g2.fillRect(
                x - (int) (10 * escala),
                corpoY + (int) (85 * escala),
                (int) (100 * escala),
                (int) (12 * escala)
        );
    }
}
