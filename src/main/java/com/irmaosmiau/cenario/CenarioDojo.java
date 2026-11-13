package com.irmaosmiau.cenario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Dojo de jiu-jitsu desenhado com Java2D, sem imagens externas.
 *
 * - Parede clara com viga, janelas de papel (estilo shoji), lambri de
 *   madeira, emblema original de pata de gato e um suporte de faixas.
 * - Tatame em perspectiva: cada uma das 5 linhas de movimentacao e uma
 *   faixa de tatame centrada exatamente onde ficam os pes dos lutadores
 *   (y = 326 + 20 * linha), com borda vermelha no fundo e na frente.
 * - Primeiro plano: apenas uma sombra suave na borda inferior, abaixo
 *   da linha 5 (os pes vao ate y ~ 406), sem cobrir os personagens.
 *
 * Fundo e chao sao renderizados uma vez em imagens e reaproveitados a
 * cada frame (custo por frame = duas copias de imagem).
 */
public final class CenarioDojo implements Cenario {

    public static final String ID = "dojo";

    /** Y dos pes na linha 1 (medido nos renderers atuais) e espaco entre linhas. */
    public static final int Y_PES_LINHA_1 = 326;
    public static final int ESPACO_LINHAS = 20;
    public static final int TOTAL_LINHAS = 5;

    /** Inicio do tatame (encontro com a parede). */
    public static final int Y_BASE_PAREDE = 296;

    private static final Color PAREDE_TOPO = new Color(238, 229, 210);
    private static final Color PAREDE_BASE = new Color(222, 207, 182);
    private static final Color MADEIRA_ESCURA = new Color(96, 62, 38);
    private static final Color MADEIRA = new Color(140, 96, 60);
    private static final Color MADEIRA_CLARA = new Color(166, 120, 78);
    private static final Color PAPEL = new Color(250, 246, 232);
    private static final Color TATAME_A = new Color(52, 118, 128);
    private static final Color TATAME_B = new Color(46, 108, 118);
    private static final Color BORDA_TATAME = new Color(170, 48, 44);

    private BufferedImage fundoCache;
    private BufferedImage chaoCache;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getNome() {
        return "Dojo de jiu-jitsu";
    }

    /** Centro vertical da faixa de tatame de uma linha (0..4). */
    public static int yCentroLinha(int linha) {
        return Y_PES_LINHA_1 + linha * ESPACO_LINHAS;
    }

    // =====================================================
    // FUNDO
    // =====================================================
    @Override
    public void desenharFundo(Graphics2D g2, int largura, int altura) {
        if (fundoCache == null || fundoCache.getWidth() != largura || fundoCache.getHeight() != altura) {
            fundoCache = renderizar(largura, altura, this::pintarFundo);
        }
        g2.drawImage(fundoCache, 0, 0, null);
    }

    private void pintarFundo(Graphics2D g, int largura, int altura) {
        // Parede
        g.setPaint(new GradientPaint(0, 0, PAREDE_TOPO, 0, Y_BASE_PAREDE, PAREDE_BASE));
        g.fillRect(0, 0, largura, Y_BASE_PAREDE);

        // Viga superior
        g.setColor(MADEIRA_ESCURA);
        g.fillRect(0, 34, largura, 12);
        g.setColor(MADEIRA);
        g.fillRect(0, 46, largura, 3);

        // Colunas de madeira
        for (int x : new int[]{30, largura / 2 - 6, largura - 42}) {
            g.setColor(MADEIRA);
            g.fillRect(x, 46, 12, Y_BASE_PAREDE - 46);
            g.setColor(MADEIRA_ESCURA);
            g.fillRect(x + 10, 46, 2, Y_BASE_PAREDE - 46);
        }

        // Janelas de papel com grade de madeira
        janela(g, 75, 70, 190, 130);
        janela(g, largura - 265, 70, 190, 130);

        // Emblema original: pata de gato num circulo
        emblema(g, largura / 2, 128);

        // Suporte de faixas (branca, azul, roxa, marrom, preta)
        suporteDeFaixas(g, largura - 255, 214);

        // Relogio simples
        relogio(g, 170, 230);

        // Lambri de madeira
        g.setColor(MADEIRA_CLARA);
        g.fillRect(0, 246, largura, Y_BASE_PAREDE - 246);
        g.setColor(MADEIRA);
        g.fillRect(0, 244, largura, 3);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(120, 82, 50, 140));
        for (int x = 0; x < largura; x += 40) {
            g.drawLine(x, 247, x, Y_BASE_PAREDE);
        }

        // Rodape
        g.setColor(MADEIRA_ESCURA);
        g.fillRect(0, Y_BASE_PAREDE - 6, largura, 6);
    }

    private static void janela(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(MADEIRA_ESCURA);
        g.fillRect(x - 6, y - 6, w + 12, h + 12);
        g.setColor(PAPEL);
        g.fillRect(x, y, w, h);
        // luz suave
        g.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, 120), x, y + h, new Color(255, 240, 200, 0)));
        g.fillRect(x, y, w, h);
        g.setColor(MADEIRA);
        g.setStroke(new BasicStroke(3f));
        for (int i = 1; i < 4; i++) {
            int gx = x + i * w / 4;
            g.drawLine(gx, y, gx, y + h);
        }
        for (int i = 1; i < 3; i++) {
            int gy = y + i * h / 3;
            g.drawLine(x, gy, x + w, gy);
        }
        g.setStroke(new BasicStroke(1f));
    }

    private static void emblema(Graphics2D g, int cx, int cy) {
        int r = 46;
        g.setColor(MADEIRA_ESCURA);
        g.fill(new Ellipse2D.Double(cx - r - 5, cy - r - 5, (r + 5) * 2, (r + 5) * 2));
        g.setColor(new Color(176, 44, 40));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(new Color(250, 244, 230));
        // almofada principal
        g.fill(new Ellipse2D.Double(cx - 19, cy - 2, 38, 30));
        // dedos
        g.fill(new Ellipse2D.Double(cx - 30, cy - 20, 15, 19));
        g.fill(new Ellipse2D.Double(cx - 14, cy - 33, 13, 18));
        g.fill(new Ellipse2D.Double(cx + 1, cy - 33, 13, 18));
        g.fill(new Ellipse2D.Double(cx + 15, cy - 20, 15, 19));
    }

    private static void suporteDeFaixas(Graphics2D g, int x, int y) {
        Color[] faixas = {
            new Color(245, 245, 240), new Color(40, 80, 170), new Color(110, 50, 140),
            new Color(110, 70, 40), new Color(25, 25, 25)
        };
        g.setColor(MADEIRA_ESCURA);
        g.fillRect(x, y, 190, 6);
        for (int i = 0; i < faixas.length; i++) {
            int fx = x + 14 + i * 36;
            g.setColor(faixas[i]);
            g.fillRect(fx, y + 6, 12, 20);
            g.setColor(new Color(0, 0, 0, 60));
            g.drawRect(fx, y + 6, 11, 19);
            // ponteira preta (ou vermelha na faixa preta)
            g.setColor(i == faixas.length - 1 ? new Color(170, 40, 40) : new Color(20, 20, 20));
            g.fillRect(fx, y + 20, 12, 4);
        }
    }

    private static void relogio(Graphics2D g, int cx, int cy) {
        g.setColor(MADEIRA_ESCURA);
        g.fill(new Ellipse2D.Double(cx - 14, cy - 14, 28, 28));
        g.setColor(PAPEL);
        g.fill(new Ellipse2D.Double(cx - 11, cy - 11, 22, 22));
        g.setColor(MADEIRA_ESCURA);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(cx, cy, cx, cy - 8);
        g.drawLine(cx, cy, cx + 6, cy + 2);
        g.setStroke(new BasicStroke(1f));
    }

    // =====================================================
    // CHAO (TATAME)
    // =====================================================
    @Override
    public void desenharChao(Graphics2D g2, int largura, int altura) {
        if (chaoCache == null || chaoCache.getWidth() != largura || chaoCache.getHeight() != altura) {
            chaoCache = renderizar(largura, altura, this::pintarChao);
        }
        g2.drawImage(chaoCache, 0, 0, null);
    }

    private void pintarChao(Graphics2D g, int largura, int altura) {
        int metade = ESPACO_LINHAS / 2;
        int yPrimeira = yCentroLinha(0) - metade;                       // 316
        int yUltima = yCentroLinha(TOTAL_LINHAS - 1) + metade;          // 416

        // Borda vermelha do fundo (encontro com a parede)
        g.setColor(BORDA_TATAME);
        g.fillRect(0, Y_BASE_PAREDE, largura, yPrimeira - Y_BASE_PAREDE);

        // Uma faixa de tatame por linha de movimentacao
        for (int linha = 0; linha < TOTAL_LINHAS; linha++) {
            int y0 = yCentroLinha(linha) - metade;
            g.setColor(linha % 2 == 0 ? TATAME_A : TATAME_B);
            g.fillRect(0, y0, largura, ESPACO_LINHAS);
        }

        // Borda vermelha da frente
        g.setColor(BORDA_TATAME);
        g.fillRect(0, yUltima, largura, altura - yUltima);

        // Emendas em perspectiva (ponto de fuga acima do centro)
        double fugaX = largura / 2.0;
        double fugaY = -700;
        g.setColor(new Color(0, 0, 0, 40));
        for (int xBase = -400; xBase <= largura + 400; xBase += 100) {
            double t0 = (Y_BASE_PAREDE - fugaY) / (altura - fugaY);
            int x0 = (int) Math.round(fugaX + (xBase - fugaX) * t0);
            g.drawLine(x0, Y_BASE_PAREDE, xBase, altura);
        }

        // Emendas entre faixas (linhas de profundidade)
        g.setColor(new Color(255, 255, 255, 35));
        for (int linha = 0; linha <= TOTAL_LINHAS; linha++) {
            int y = yCentroLinha(0) - metade + linha * ESPACO_LINHAS;
            g.drawLine(0, y, largura, y);
        }

        // Sombra da parede sobre o tatame
        g.setPaint(new GradientPaint(0, Y_BASE_PAREDE, new Color(0, 0, 0, 70),
                0, Y_BASE_PAREDE + 14, new Color(0, 0, 0, 0)));
        g.fillRect(0, Y_BASE_PAREDE, largura, 14);
    }

    // =====================================================
    // PRIMEIRO PLANO
    // =====================================================
    @Override
    public void desenharPrimeiroPlano(Graphics2D g2, int largura, int altura) {
        int y0 = yCentroLinha(TOTAL_LINHAS - 1) + ESPACO_LINHAS;   // abaixo dos pes da linha 5
        if (y0 >= altura) {
            return;
        }
        g2.setPaint(new GradientPaint(0, y0, new Color(0, 0, 0, 0), 0, altura, new Color(0, 0, 0, 80)));
        g2.fillRect(0, y0, largura, altura - y0);
    }

    // =====================================================
    // AUXILIAR
    // =====================================================
    @FunctionalInterface
    private interface Pintor {
        void pintar(Graphics2D g, int largura, int altura);
    }

    private static BufferedImage renderizar(int largura, int altura, Pintor pintor) {
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pintor.pintar(g, largura, altura);
        } finally {
            g.dispose();
        }
        return img;
    }
}
