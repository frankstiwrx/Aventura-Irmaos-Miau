package com.irmaosmiau.cenario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Entrega 5O.4: Campo Aberto Remasterizado, desenhado com Java2D (sem imagens).
 *
 * Evolucao do campo original (que continua existindo, intacto): ceu azul em
 * degrade, nuvens poligonais, duas camadas de montanhas, arbustos no horizonte,
 * arvores e pedras poligonais nas laterais, e a grama em perspectiva com uma
 * faixa por linha de movimentacao (centrada onde ficam os pes: y = 326 + 20 x
 * linha), igual a regra do Dojo. Primeiro plano: so tufos e pedras pequenas
 * abaixo dos pes da linha 5, nas bordas, sem cobrir os lutadores.
 */
public final class CenarioCampoRemaster implements Cenario {

    public static final String ID = "campo_remaster";

    /** Horizonte (onde a grama encontra os arbustos), o mesmo inicio de chao do Dojo. */
    public static final int Y_HORIZONTE = CenarioDojo.Y_BASE_PAREDE;

    private static final Color CEU_TOPO = new Color(52, 160, 240);
    private static final Color CEU_BASE = new Color(150, 214, 250);
    private static final Color NUVEM = new Color(250, 252, 255);
    private static final Color NUVEM_SOMBRA = new Color(214, 226, 246);
    private static final Color MONTANHA_LONGE = new Color(62, 160, 150);
    private static final Color MONTANHA_LONGE_SOMBRA = new Color(48, 138, 136);
    private static final Color MONTANHA_PERTO = new Color(44, 140, 104);
    private static final Color MONTANHA_PERTO_SOMBRA = new Color(34, 118, 90);
    private static final Color ARBUSTO = new Color(86, 170, 50);
    private static final Color ARBUSTO_CLARO = new Color(120, 196, 60);
    private static final Color ARBUSTO_ESCURO = new Color(60, 136, 40);
    private static final Color GRAMA_A = new Color(128, 196, 60);
    private static final Color GRAMA_B = new Color(116, 186, 52);
    private static final Color GRAMA_FUNDO = new Color(108, 178, 48);
    private static final Color GRAMA_FRENTE = new Color(84, 158, 44);
    private static final Color TRONCO = new Color(128, 74, 36);
    private static final Color TRONCO_SOMBRA = new Color(96, 54, 28);
    private static final Color COPA = new Color(104, 186, 50);
    private static final Color COPA_CLARA = new Color(150, 214, 70);
    private static final Color COPA_ESCURA = new Color(76, 150, 40);
    private static final Color PEDRA = new Color(150, 146, 168);
    private static final Color PEDRA_CLARA = new Color(178, 174, 196);
    private static final Color PEDRA_ESCURA = new Color(116, 112, 136);

    private BufferedImage fundoCache;
    private BufferedImage chaoCache;
    private BufferedImage frenteCache;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getNome() {
        return "Campo aberto remasterizado";
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

    private void pintarFundo(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, CEU_TOPO, 0, Y_HORIZONTE - 40, CEU_BASE));
        g.fillRect(0, 0, w, Y_HORIZONTE);

        nuvem(g, 150, 46, 1.0);
        nuvem(g, 620, 40, 0.95);
        nuvem(g, 330, 100, 0.55);
        nuvem(g, 470, 78, 0.6);

        // Montanhas (duas camadas, facetadas)
        int base = Y_HORIZONTE - 22;
        montanhas(g, base, new int[] {-40, 90, 230, 360, 470, 600, 720, 860},
                new int[] {58, 92, 70, 96, 64, 98, 72, 60}, MONTANHA_LONGE, MONTANHA_LONGE_SOMBRA);
        montanhas(g, base + 8, new int[] {-60, 60, 170, 300, 420, 540, 660, 780, 900},
                new int[] {40, 54, 36, 58, 42, 56, 38, 52, 40}, MONTANHA_PERTO, MONTANHA_PERTO_SOMBRA);

        // Faixa de grama do fundo ate o horizonte
        g.setColor(GRAMA_FUNDO);
        g.fillRect(0, base + 4, w, Y_HORIZONTE - base - 4);

        // Arbustos no horizonte (deixam um caminho aberto no centro)
        for (int x = 150; x < 370; x += 26) {
            arbusto(g, x, Y_HORIZONTE - 4, 16 + (x * 7) % 9);
        }
        for (int x = 440; x < 660; x += 26) {
            arbusto(g, x, Y_HORIZONTE - 4, 16 + (x * 5) % 9);
        }

        // Laterais: arvore + pedras (atras dos lutadores, nas bordas)
        arvore(g, 52, Y_HORIZONTE + 6, 1.0);
        arvore(g, w - 52, Y_HORIZONTE + 6, 1.0);
        pedra(g, 92, Y_HORIZONTE + 8, 58, 44);
        pedra(g, 20, Y_HORIZONTE + 12, 46, 32);
        pedra(g, w - 96, Y_HORIZONTE + 8, 60, 46);
        pedra(g, w - 24, Y_HORIZONTE + 14, 44, 30);
        arbusto(g, 130, Y_HORIZONTE + 10, 15);
        arbusto(g, w - 136, Y_HORIZONTE + 10, 15);
    }

    private static void nuvem(Graphics2D g, int cx, int cy, double e) {
        int[] xs = {-150, -100, -70, -30, 10, 40, 80, 150};
        int[] ys = {22, 10, -14, -26, -18, -2, 6, 22};
        Polygon p = new Polygon();
        for (int i = 0; i < xs.length; i++) {
            p.addPoint(cx + (int) Math.round(xs[i] * e), cy + (int) Math.round(ys[i] * e));
        }
        g.setColor(NUVEM);
        g.fillPolygon(p);
        Polygon s = new Polygon();
        s.addPoint(cx + (int) Math.round(-150 * e), cy + (int) Math.round(22 * e));
        s.addPoint(cx + (int) Math.round(150 * e), cy + (int) Math.round(22 * e));
        s.addPoint(cx + (int) Math.round(70 * e), cy + (int) Math.round(12 * e));
        s.addPoint(cx + (int) Math.round(-60 * e), cy + (int) Math.round(14 * e));
        g.setColor(NUVEM_SOMBRA);
        g.fillPolygon(s);
    }

    private static void montanhas(Graphics2D g, int base, int[] picosX, int[] alturas, Color cor, Color sombra) {
        for (int i = 0; i < picosX.length; i++) {
            int x = picosX[i];
            int topo = base - alturas[i];
            int meia = 120 + alturas[i];
            g.setColor(cor);
            g.fillPolygon(new int[] {x - meia, x, x + meia}, new int[] {base, topo, base}, 3);
            g.setColor(sombra);                               // face em sombra (low poly)
            g.fillPolygon(new int[] {x, x + meia, x + meia / 3}, new int[] {topo, base, base}, 3);
        }
    }

    private static void arbusto(Graphics2D g, int cx, int base, int r) {
        int[][] bolas = {{-r, 0, r}, {0, -r / 2, r + 3}, {r, 0, r}};
        for (int[] b : bolas) {
            octogono(g, cx + b[0], base + b[1] - b[2] / 2, b[2], ARBUSTO);
        }
        octogono(g, cx - 2, base - r, r / 2 + 2, ARBUSTO_CLARO);
        g.setColor(ARBUSTO_ESCURO);
        g.fillRect(cx - r * 2, base - 3, r * 4, 4);
    }

    private static void octogono(Graphics2D g, int cx, int cy, int r, Color c) {
        Polygon p = new Polygon();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 8 + i * Math.PI / 4;
            p.addPoint(cx + (int) Math.round(Math.cos(a) * r), cy + (int) Math.round(Math.sin(a) * r * 0.85));
        }
        g.setColor(c);
        g.fillPolygon(p);
    }

    private static void arvore(Graphics2D g, int x, int base, double e) {
        // tronco com dois galhos
        g.setColor(TRONCO);
        g.fillPolygon(new int[] {x - 9, x + 9, x + 6, x - 5}, new int[] {base, base, base - 120, base - 120}, 4);
        g.fillPolygon(new int[] {x - 2, x + 4, x + 40, x + 34}, new int[] {base - 80, base - 84, base - 118, base - 122}, 4);
        g.fillPolygon(new int[] {x + 2, x - 4, x - 38, x - 32}, new int[] {base - 90, base - 94, base - 124, base - 128}, 4);
        g.setColor(TRONCO_SOMBRA);
        g.fillPolygon(new int[] {x + 3, x + 9, x + 6, x + 2}, new int[] {base, base, base - 120, base - 120}, 4);
        // copa facetada (varios octogonos sobrepostos)
        int topo = base - 150;
        int[][] f = {{-40, 10, 34}, {38, 8, 34}, {0, -6, 40}, {-22, -30, 28}, {24, -30, 28}};
        for (int[] q : f) {
            octogono(g, x + q[0], topo + q[1], q[2], COPA);
        }
        octogono(g, x - 14, topo - 22, 16, COPA_CLARA);
        octogono(g, x + 30, topo - 4, 14, COPA_CLARA);
        g.setColor(COPA_ESCURA);
        g.fillPolygon(new int[] {x - 70, x + 70, x + 40, x - 40}, new int[] {topo + 30, topo + 30, topo + 44, topo + 44}, 4);
    }

    private static void pedra(Graphics2D g, int cx, int base, int w, int h) {
        int[] xs = {cx - w / 2, cx - w / 3, cx + w / 8, cx + w / 2, cx + w / 2 - 4};
        int[] ys = {base, base - h * 2 / 3, base - h, base - h / 3, base};
        g.setColor(PEDRA);
        g.fillPolygon(xs, ys, 5);
        g.setColor(PEDRA_CLARA);
        g.fillPolygon(new int[] {cx - w / 3, cx + w / 8, cx, cx - w / 4},
                new int[] {base - h * 2 / 3, base - h, base - h / 2, base - h / 3}, 4);
        g.setColor(PEDRA_ESCURA);
        g.fillPolygon(new int[] {cx + w / 8, cx + w / 2, cx + w / 2 - 4, cx + w / 6},
                new int[] {base - h, base - h / 3, base, base}, 4);
    }

    // =====================================================
    // CHAO
    // =====================================================
    @Override
    public void desenharChao(Graphics2D g2, int largura, int altura) {
        if (chaoCache == null || chaoCache.getWidth() != largura || chaoCache.getHeight() != altura) {
            chaoCache = renderizar(largura, altura, this::pintarChao);
        }
        g2.drawImage(chaoCache, 0, 0, null);
    }

    private void pintarChao(Graphics2D g, int w, int h) {
        int metade = CenarioDojo.ESPACO_LINHAS / 2;
        int yPrimeira = CenarioDojo.yCentroLinha(0) - metade;
        int yUltima = CenarioDojo.yCentroLinha(CenarioDojo.TOTAL_LINHAS - 1) + metade;

        g.setColor(GRAMA_FUNDO);
        g.fillRect(0, Y_HORIZONTE, w, yPrimeira - Y_HORIZONTE);
        for (int linha = 0; linha < CenarioDojo.TOTAL_LINHAS; linha++) {
            g.setColor(linha % 2 == 0 ? GRAMA_A : GRAMA_B);
            g.fillRect(0, CenarioDojo.yCentroLinha(linha) - metade, w, CenarioDojo.ESPACO_LINHAS);
        }
        g.setColor(GRAMA_FRENTE);
        g.fillRect(0, yUltima, w, h - yUltima);

        // Placas de grama em perspectiva (ponto de fuga acima do centro)
        double fugaX = w / 2.0;
        double fugaY = -500;
        g.setColor(new Color(70, 140, 40, 90));
        g.setStroke(new BasicStroke(1.2f));
        for (int xBase = -500; xBase <= w + 500; xBase += 110) {
            double t0 = (Y_HORIZONTE - fugaY) / (h - fugaY);
            int x0 = (int) Math.round(fugaX + (xBase - fugaX) * t0);
            g.drawLine(x0, Y_HORIZONTE, xBase, h);
        }
        g.setColor(new Color(70, 140, 40, 110));
        for (int linha = 0; linha <= CenarioDojo.TOTAL_LINHAS; linha++) {
            int y = yPrimeira + linha * CenarioDojo.ESPACO_LINHAS;
            g.drawLine(0, y, w, y);
        }
        g.setStroke(new BasicStroke(1f));
        // Brilho suave no centro do campo
        g.setPaint(new GradientPaint(0, Y_HORIZONTE, new Color(255, 255, 210, 40), 0, yUltima, new Color(255, 255, 210, 0)));
        g.fillRect(0, Y_HORIZONTE, w, yUltima - Y_HORIZONTE);
    }

    // =====================================================
    // PRIMEIRO PLANO (abaixo dos pes da linha 5, so nas bordas)
    // =====================================================
    @Override
    public void desenharPrimeiroPlano(Graphics2D g2, int largura, int altura) {
        if (frenteCache == null || frenteCache.getWidth() != largura || frenteCache.getHeight() != altura) {
            frenteCache = renderizar(largura, altura, this::pintarFrente);
        }
        g2.drawImage(frenteCache, 0, 0, null);
    }

    private void pintarFrente(Graphics2D g, int w, int h) {
        int y0 = CenarioDojo.yCentroLinha(CenarioDojo.TOTAL_LINHAS - 1) + CenarioDojo.ESPACO_LINHAS;   // 426
        if (y0 >= h) {
            return;
        }
        g.setPaint(new GradientPaint(0, y0, new Color(0, 60, 0, 0), 0, h, new Color(0, 60, 0, 70)));
        g.fillRect(0, y0, w, h - y0);
        for (int x : new int[] {60, 150, 250, 360, 470, 560, 650, 740}) {
            tufo(g, x, h);
        }
        pedra(g, 18, h + 4, 50, 26);
        pedra(g, w - 16, h + 4, 50, 28);
    }

    private static void tufo(Graphics2D g, int x, int base) {
        g.setColor(new Color(60, 140, 40));
        for (int i = -2; i <= 2; i++) {
            g.fillPolygon(new int[] {x + i * 4 - 2, x + i * 4 + 2, x + i * 6}, new int[] {base, base, base - 12 + Math.abs(i) * 3}, 3);
        }
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
