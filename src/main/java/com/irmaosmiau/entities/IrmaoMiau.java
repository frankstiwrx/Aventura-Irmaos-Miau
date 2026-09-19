package com.irmaosmiau.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

public abstract class IrmaoMiau {

    protected int x;
    protected int y;

    protected double escala;

    protected Color corPelo;
    protected Color corKimono;
    protected Color corRosto;

    protected String nome;
    protected String estilo;

    // =============================
    // LINHAS DO CENÁRIO
    // =============================

    private static final int TOTAL_LINHAS = 5;
    private static final int ESPACO_ENTRE_LINHAS = 20;

    // 0 internamente significa Linha 1
    private int linhaAtual = 0;

    // Posição vertical da primeira linha
    private final int yLinha1;

    private int yBase;

    // =============================
    // MOVIMENTO
    // =============================

    private boolean pulando = false;
    private boolean agachado = false;
    private boolean andando = false;

    private double alturaPulo = 0;
    private double velocidadePulo = 0;

    private double faseAnimacao = 0;

    // =============================
    // PISCAR
    // =============================

    private boolean piscando = false;

    private long proximoPiscar =
            System.currentTimeMillis() + 2000;

    private long fimDoPiscar = 0;

    public IrmaoMiau(
            int x,
            int y,
            double escala,
            Color corPelo,
            Color corKimono,
            Color corRosto,
            String nome,
            String estilo
    ) {

        this.x = x;
        this.y = y;

        this.yLinha1 = y;
        this.yBase = y;

        this.escala = escala;

        this.corPelo = corPelo;
        this.corKimono = corKimono;
        this.corRosto = corRosto;

        this.nome = nome;
        this.estilo = estilo;
    }

    public abstract void desenhar(Graphics2D g2);

    // =============================
    // ATUALIZAÇÃO
    // =============================

    public void atualizar(
            boolean estaAndando,
            boolean querAgachar
    ) {

        andando = estaAndando && !querAgachar;

        agachado = querAgachar && !pulando;

        // =========================
        // ANIMAÇÃO DE CAMINHADA
        // =========================

        if (andando) {

            faseAnimacao += 0.25;

        } else {

            faseAnimacao = 0;
        }

        // =========================
        // PULO
        // =========================

        if (pulando) {

            alturaPulo += velocidadePulo;

            velocidadePulo -= 0.65;

            if (
                    alturaPulo <= 0
                    && velocidadePulo < 0
            ) {

                alturaPulo = 0;
                velocidadePulo = 0;

                pulando = false;
            }
        }

        // Linha onde ele está
        // menos a altura do pulo
        y = yBase - (int) Math.round(alturaPulo);

        atualizarPiscar();
    }

    // =============================
    // PISCAR
    // =============================

    private void atualizarPiscar() {

        long agora =
                System.currentTimeMillis();

        if (
                !piscando
                && agora >= proximoPiscar
        ) {

            piscando = true;

            // Olho fechado por 120 ms
            fimDoPiscar = agora + 120;
        }

        if (
                piscando
                && agora >= fimDoPiscar
        ) {

            piscando = false;

            proximoPiscar = agora + 2000;
        }
    }

    // =============================
    // MOVIMENTO VERTICAL
    // =============================
    public void moverVertical(int quantidade) {

    int yMinimo = yLinha1;

    int yMaximo =
            yLinha1
            + (TOTAL_LINHAS - 1)
            * ESPACO_ENTRE_LINHAS;

    yBase += quantidade;

    // Não deixa passar da Linha 1
    if (yBase < yMinimo) {
        yBase = yMinimo;
    }

    // Não deixa passar da Linha 5
    if (yBase > yMaximo) {
        yBase = yMaximo;
    }

    // Descobre qual das 5 linhas
    // está mais próxima da posição atual
    linhaAtual = (int) Math.round(
            (double) (yBase - yLinha1)
            / ESPACO_ENTRE_LINHAS
    );
}
    // =============================
    // MOVIMENTO HORIZONTAL
    // =============================

    public void moverX(int quantidade) {

        x += quantidade;
    }

    // =============================
    // LINHAS
    // =============================

    public void subirLinha() {

        if (linhaAtual > 0) {

            linhaAtual--;

            atualizarPosicaoLinha();
        }
    }

    public void descerLinha() {

        if (linhaAtual < TOTAL_LINHAS - 1) {

            linhaAtual++;

            atualizarPosicaoLinha();
        }
    }

    private void atualizarPosicaoLinha() {

        yBase =
                yLinha1
                + linhaAtual
                * ESPACO_ENTRE_LINHAS;
    }

    // =============================
    // PULO
    // =============================

    public void pular() {

        if (!pulando && !agachado) {

            pulando = true;

            alturaPulo = 1;

            velocidadePulo = 10;
        }
    }

    // =============================
    // ANIMAÇÃO
    // =============================

    protected int getOscilacaoPasso() {

        if (!andando) {
            return 0;
        }

        return (int) (
                Math.sin(faseAnimacao)
                * 10
                * escala
        );
    }

    protected int getYVisual() {

        int deslocamentoAgachado = 0;

        if (agachado) {

            deslocamentoAgachado =
                    (int) (18 * escala);
        }

        return y + deslocamentoAgachado;
    }

    protected int getEncolhimentoAgachado() {

        if (agachado) {

            return (int) (30 * escala);
        }

        return 0;
    }

    public boolean isAgachado() {
        return agachado;
    }

    public boolean isPulando() {
        return pulando;
    }

    public int getLinhaAtual() {
        return linhaAtual;
    }

    // =============================
    // DESENHO BASE
    // =============================

    protected void prepararDesenho(Graphics2D g2) {

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
    }

    protected void desenharCabeca(Graphics2D g2) {

        int yCabeca = getYVisual();

        int cabeca =
                (int) (70 * escala);

        // Cabeça
        g2.setColor(corPelo);

        g2.fillOval(
                x,
                yCabeca,
                cabeca,
                cabeca
        );

        // =========================
        // ORELHAS
        // =========================

        Polygon esquerda =
                new Polygon();

        esquerda.addPoint(
                x + (int) (8 * escala),
                yCabeca + (int) (8 * escala)
        );

        esquerda.addPoint(
                x + (int) (16 * escala),
                yCabeca - (int) (25 * escala)
        );

        esquerda.addPoint(
                x + (int) (28 * escala),
                yCabeca + (int) (8 * escala)
        );

        Polygon direita =
                new Polygon();

        direita.addPoint(
                x + (int) (43 * escala),
                yCabeca + (int) (8 * escala)
        );

        direita.addPoint(
                x + (int) (55 * escala),
                yCabeca - (int) (25 * escala)
        );

        direita.addPoint(
                x + (int) (63 * escala),
                yCabeca + (int) (8 * escala)
        );

        g2.fillPolygon(esquerda);
        g2.fillPolygon(direita);

        // =========================
        // OLHOS
        // =========================

        g2.setColor(corRosto);

        if (!piscando) {

            int olho =
                    (int) (6 * escala);

            g2.fillOval(
                    x + (int) (22 * escala),
                    yCabeca + (int) (30 * escala),
                    olho,
                    olho
            );

            g2.fillOval(
                    x + (int) (45 * escala),
                    yCabeca + (int) (30 * escala),
                    olho,
                    olho
            );

        } else {

            // Quando pisca, os olhos viram
            // dois pequenos risquinhos

            g2.setStroke(
                    new BasicStroke(
                            (float) (2 * escala)
                    )
            );

            g2.drawLine(
                    x + (int) (21 * escala),
                    yCabeca + (int) (33 * escala),
                    x + (int) (29 * escala),
                    yCabeca + (int) (33 * escala)
            );

            g2.drawLine(
                    x + (int) (44 * escala),
                    yCabeca + (int) (33 * escala),
                    x + (int) (52 * escala),
                    yCabeca + (int) (33 * escala)
            );
        }

        // =========================
        // NARIZ
        // =========================

        g2.setColor(
                new Color(220, 120, 140)
        );

        Polygon nariz =
                new Polygon();

        nariz.addPoint(
                x + (int) (33 * escala),
                yCabeca + (int) (43 * escala)
        );

        nariz.addPoint(
                x + (int) (40 * escala),
                yCabeca + (int) (43 * escala)
        );

        nariz.addPoint(
                x + (int) (36 * escala),
                yCabeca + (int) (48 * escala)
        );

        g2.fillPolygon(nariz);

        // =========================
        // BOCA
        // =========================

        g2.setColor(corRosto);

        int bocaX =
                x + (int) (36 * escala);

        int bocaY =
                yCabeca + (int) (48 * escala);

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
    }

    protected void desenharPata(
            Graphics2D g2,
            int pataX,
            int pataY,
            int largura,
            int altura
    ) {

        g2.setColor(corPelo);

        g2.fillRoundRect(
                pataX,
                pataY,
                largura,
                altura,
                5,
                5
        );
    }

    // =============================
    // GETTERS
    // =============================

    public String getNome() {
        return nome;
    }

    public String getEstilo() {
        return estilo;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}