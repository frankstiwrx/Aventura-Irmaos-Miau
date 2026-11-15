package com.irmaosmiau.cenario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Entrega 5O.4: Arena do Grande Torneio, desenhada com Java2D (sem imagens).
 *
 * Atmosfera de um grande torneio de artes marciais ao ar livre, com desenho
 * PROPRIO dos Irmaos Miau: pavilhao com telhados de palha, torre central com a
 * faixa "GRANDE TORNEIO" e o emblema de pata, muralha de tijolos com portao,
 * palmeiras e arbustos; a plataforma de pedra em perspectiva e a area jogavel
 * (uma fileira de lajes por linha de movimentacao, centrada nos pes: y = 326 +
 * 20 x linha), com a borda da plataforma na frente, abaixo dos pes da linha 5.
 *
 * Escala: a muralha (76 px) fica abaixo da cintura dos lutadores (190 px em pe)
 * e o pavilhao aparece ao longe, atras da plataforma.
 */
public final class CenarioArenaTorneio implements Cenario {

    public static final String ID = "torneio";

    public static final int Y_CHAO = CenarioDojo.Y_BASE_PAREDE;   // 296
    private static final int Y_MURO_TOPO = 214;

    private static final Color CEU_TOPO = new Color(30, 120, 230);
    private static final Color CEU_BASE = new Color(120, 190, 250);
    private static final Color NUVEM = new Color(250, 252, 255);
    private static final Color NUVEM_SOMBRA = new Color(206, 222, 246);
    private static final Color PALHA = new Color(196, 152, 70);
    private static final Color PALHA_CLARA = new Color(222, 184, 100);
    private static final Color PALHA_ESCURA = new Color(150, 108, 44);
    private static final Color CREME = new Color(240, 220, 160);
    private static final Color CREME_ESCURO = new Color(210, 186, 124);
    private static final Color TIJOLO = new Color(214, 86, 40);
    private static final Color TIJOLO_ESCURO = new Color(170, 60, 30);
    private static final Color VERMELHO = new Color(200, 60, 40);
    private static final Color OURO = new Color(236, 192, 64);
    private static final Color OURO_ESCURO = new Color(184, 138, 36);
    private static final Color PAREDE = new Color(236, 234, 228);
    private static final Color CINZA_PAREDE = new Color(196, 196, 200);
    private static final Color PEDRA_A = new Color(176, 176, 196);
    private static final Color PEDRA_B = new Color(164, 164, 186);
    private static final Color PEDRA_FUNDO = new Color(150, 150, 172);
    private static final Color JUNTA = new Color(110, 110, 134);
    private static final Color BORDA_FRENTE = new Color(204, 204, 218);
    private static final Color BORDA_SOMBRA = new Color(128, 128, 150);
    private static final Color FOLHA = new Color(40, 150, 50);
    private static final Color FOLHA_CLARA = new Color(90, 190, 70);
    private static final Color TRONCO = new Color(150, 110, 60);
    private static final Color ARBUSTO = new Color(40, 140, 40);
    private static final Color ARBUSTO_CLARO = new Color(80, 180, 60);

    private BufferedImage fundoCache;
    private BufferedImage chaoCache;
    private BufferedImage frenteCache;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getNome() {
        return "Arena do Grande Torneio";
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
        g.setPaint(new GradientPaint(0, 0, CEU_TOPO, 0, Y_MURO_TOPO, CEU_BASE));
        g.fillRect(0, 0, w, Y_CHAO);
        nuvem(g, 110, 40, 1.0);
        nuvem(g, 690, 30, 1.1);
        nuvem(g, 560, 120, 0.6);
        nuvem(g, 250, 112, 0.55);
        nuvem(g, 760, 150, 0.5);
        nuvem(g, 40, 160, 0.5);

        int cx = w / 2;
        // Telhados laterais longos (palha) atras da muralha
        telhadoPalha(g, 40, 166, cx - 60, 210, false);
        telhadoPalha(g, cx + 60, 166, w - 40, 210, true);
        // Enfeites dourados sobre os telhados laterais
        pinaculo(g, 210, 170);
        pinaculo(g, w - 210, 170);

        // Torre central: corpo, faixa, telhado redondo e remate
        g.setColor(VERMELHO);
        g.fillRect(cx - 60, 92, 120, 70);
        g.setColor(PAREDE);
        g.fillRect(cx - 46, 96, 92, 62);
        emblemaPata(g, cx, 126, 24);
        cupula(g, cx, 98, 110, 60);
        g.setColor(OURO);
        g.fillPolygon(new int[] {cx - 6, cx, cx + 6}, new int[] {40, 18, 40}, 3);
        g.setColor(OURO_ESCURO);
        g.fillRect(cx - 12, 38, 24, 6);

        // Faixa do torneio
        int fx = cx - 150;
        int fy = 158;
        g.setColor(OURO_ESCURO);
        g.fillRect(fx - 6, fy - 6, 312, 46);
        g.setColor(VERMELHO);
        g.fillRect(fx - 2, fy - 2, 304, 38);
        g.setColor(PAREDE);
        g.fillRect(fx + 4, fy + 4, 292, 26);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        String txt = "GRANDE TORNEIO";
        g.setColor(new Color(30, 30, 30));
        g.drawString(txt, cx - fm.stringWidth(txt) / 2, fy + 25);

        // Palmeiras e arbustos
        palmeira(g, 70, Y_CHAO - 6, false);
        palmeira(g, w - 70, Y_CHAO - 6, true);

        // Muralha de tijolos com faixa creme
        muralha(g, w);

        // Portao central com telhadinho
        int gx = cx - 46;
        g.setColor(CREME);
        g.fillRect(gx, Y_MURO_TOPO - 6, 92, Y_CHAO - Y_MURO_TOPO + 6);
        g.setColor(VERMELHO);
        g.fillRect(gx + 8, Y_MURO_TOPO + 6, 12, Y_CHAO - Y_MURO_TOPO - 8);
        g.fillRect(gx + 72, Y_MURO_TOPO + 6, 12, Y_CHAO - Y_MURO_TOPO - 8);
        g.setColor(new Color(40, 30, 30));
        g.fill(new Arc2D.Double(cx - 22, Y_MURO_TOPO + 10, 44, 40, 0, 180, Arc2D.PIE));
        g.fillRect(cx - 22, Y_MURO_TOPO + 30, 44, Y_CHAO - Y_MURO_TOPO - 32);
        g.setColor(PAREDE);
        g.fillRect(cx - 14, Y_CHAO - 34, 28, 22);
        emblemaPata(g, cx, Y_CHAO - 23, 9);
        cupula(g, cx, Y_MURO_TOPO - 2, 78, 30);

        for (int x : new int[] {150, 280, w - 280, w - 150}) {
            arbustoRedondo(g, x, Y_MURO_TOPO + 4, 22);
        }
    }

    private static void nuvem(Graphics2D g, int cx, int cy, double e) {
        int[][] bolas = {{-50, 6, 22}, {-24, -6, 28}, {6, -12, 30}, {36, -2, 24}, {58, 8, 16}};
        g.setColor(NUVEM_SOMBRA);
        g.fill(new Ellipse2D.Double(cx - 70 * e, cy + 2 * e, 140 * e, 20 * e));
        g.setColor(NUVEM);
        for (int[] b : bolas) {
            g.fill(new Ellipse2D.Double(cx + (b[0] - b[2]) * e, cy + (b[1] - b[2]) * e, b[2] * 2 * e, b[2] * 1.6 * e));
        }
    }

    /** Telhado de palha em trapezio, com riscos de palha e beiral. */
    private static void telhadoPalha(Graphics2D g, int x0, int y0, int x1, int y1, boolean espelho) {
        int recuo = 36;
        Polygon p = new Polygon(new int[] {x0, x0 + recuo, x1 - recuo, x1}, new int[] {y1, y0, y0, y1}, 4);
        g.setColor(PALHA);
        g.fillPolygon(p);
        g.setColor(PALHA_ESCURA);
        g.setStroke(new BasicStroke(1f));
        for (int x = x0 + 4; x < x1; x += 5) {
            int inc = (x * 7) % 4;
            g.drawLine(x, y1 - 2, x + (espelho ? -2 : 2) - (x - (x0 + x1) / 2) / 30, y0 + 6 + inc);
        }
        g.setColor(PALHA_CLARA);
        g.fillRect(x0 + recuo, y0, x1 - x0 - 2 * recuo, 4);
        g.setColor(PALHA_ESCURA);
        g.fillRect(x0, y1 - 4, x1 - x0, 4);
    }

    /** Cupula de palha arredondada (topo da torre / portao). */
    private static void cupula(Graphics2D g, int cx, int base, int largura, int alturaCupula) {
        Path2D d = new Path2D.Double();
        d.moveTo(cx - largura / 2.0 - 10, base);
        d.curveTo(cx - largura / 2.0, base - alturaCupula * 0.7, cx - largura / 4.0, base - alturaCupula,
                cx, base - alturaCupula);
        d.curveTo(cx + largura / 4.0, base - alturaCupula, cx + largura / 2.0, base - alturaCupula * 0.7,
                cx + largura / 2.0 + 10, base);
        d.closePath();
        g.setColor(PALHA);
        g.fill(d);
        g.setClip(d);
        g.setColor(PALHA_ESCURA);
        for (int i = -12; i <= 12; i++) {
            g.drawLine(cx + i * largura / 24, base, cx + i * largura / 60, base - alturaCupula);
        }
        g.setClip(null);
        g.setColor(PALHA_ESCURA);
        g.fillRect(cx - largura / 2 - 10, base - 3, largura + 20, 4);
    }

    private static void pinaculo(Graphics2D g, int x, int base) {
        g.setColor(OURO_ESCURO);
        g.fillRect(x - 10, base - 6, 20, 8);
        g.setColor(OURO);
        g.fillPolygon(new int[] {x - 8, x - 2, x + 2, x + 8, x}, new int[] {base - 6, base - 34, base - 34, base - 6, base - 12}, 5);
        g.fillPolygon(new int[] {x + 2, x + 16, x + 6}, new int[] {base - 10, base - 22, base - 4}, 3);
    }

    /** Emblema original dos Irmaos Miau: pata num circulo. */
    private static void emblemaPata(Graphics2D g, int cx, int cy, int r) {
        g.setColor(OURO_ESCURO);
        g.fill(new Ellipse2D.Double(cx - r - 3, cy - r - 3, (r + 3) * 2, (r + 3) * 2));
        g.setColor(VERMELHO);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(CREME);
        double s = r / 46.0;
        g.fill(new Ellipse2D.Double(cx - 19 * s, cy - 2 * s, 38 * s, 30 * s));
        g.fill(new Ellipse2D.Double(cx - 30 * s, cy - 20 * s, 15 * s, 19 * s));
        g.fill(new Ellipse2D.Double(cx - 14 * s, cy - 33 * s, 13 * s, 18 * s));
        g.fill(new Ellipse2D.Double(cx + 1 * s, cy - 33 * s, 13 * s, 18 * s));
        g.fill(new Ellipse2D.Double(cx + 15 * s, cy - 20 * s, 15 * s, 19 * s));
    }

    private static void muralha(Graphics2D g, int w) {
        g.setColor(CINZA_PAREDE);
        g.fillRect(0, Y_MURO_TOPO - 8, w, 10);
        g.setColor(CREME);
        g.fillRect(0, Y_MURO_TOPO, w, Y_CHAO - Y_MURO_TOPO);
        int ty0 = Y_MURO_TOPO + 12;
        int ty1 = Y_CHAO - 14;
        g.setColor(TIJOLO);
        g.fillRect(14, ty0, w - 28, ty1 - ty0);
        g.setColor(TIJOLO_ESCURO);
        for (int y = ty0; y <= ty1; y += 10) {
            g.drawLine(14, y, w - 14, y);
            int desloc = ((y - ty0) / 10) % 2 == 0 ? 0 : 14;
            for (int x = 14 + desloc; x < w - 14; x += 28) {
                g.drawLine(x, y, x, Math.min(y + 10, ty1));
            }
        }
        g.setColor(CREME_ESCURO);
        g.fillRect(0, Y_CHAO - 6, w, 6);
    }

    private static void palmeira(Graphics2D g, int x, int base, boolean espelho) {
        int s = espelho ? -1 : 1;
        Path2D t = new Path2D.Double();
        t.moveTo(x - 7, base);
        t.curveTo(x - 4, base - 80, x + s * 6, base - 150, x + s * 16, base - 210);
        t.lineTo(x + s * 24, base - 208);
        t.curveTo(x + s * 14, base - 150, x + 6, base - 80, x + 7, base);
        t.closePath();
        g.setColor(TRONCO);
        g.fill(t);
        g.setColor(new Color(120, 86, 46));
        for (int y = base - 10; y > base - 200; y -= 12) {
            g.drawLine(x - 6 + s * (base - y) / 20, y, x + 6 + s * (base - y) / 20, y - 3);
        }
        int tx = x + s * 20;
        int ty = base - 210;
        double[] angulos = {-160, -125, -90, -55, -20, 10, 200};
        for (double a : angulos) {
            folha(g, tx, ty, Math.toRadians(a), 70);
        }
    }

    private static void folha(Graphics2D g, int x, int y, double a, int comp) {
        double dx = Math.cos(a);
        double dy = Math.sin(a) + 0.35;                        // folhas caem um pouco
        double nx = -dy;
        double ny = dx;
        Polygon p = new Polygon();
        p.addPoint(x, y);
        p.addPoint((int) (x + dx * comp * 0.5 + nx * 10), (int) (y + dy * comp * 0.5 + ny * 10));
        p.addPoint((int) (x + dx * comp), (int) (y + dy * comp + 8));
        p.addPoint((int) (x + dx * comp * 0.5 - nx * 6), (int) (y + dy * comp * 0.5 - ny * 6));
        g.setColor(FOLHA);
        g.fillPolygon(p);
        g.setColor(FOLHA_CLARA);
        g.drawLine(x, y, (int) (x + dx * comp), (int) (y + dy * comp + 8));
    }

    private static void arbustoRedondo(Graphics2D g, int cx, int base, int r) {
        g.setColor(ARBUSTO);
        g.fill(new Ellipse2D.Double(cx - r, base - r * 1.6, r * 2, r * 1.8));
        g.setColor(ARBUSTO_CLARO);
        g.fill(new Ellipse2D.Double(cx - r * 0.6, base - r * 1.5, r, r * 0.8));
    }

    // =====================================================
    // CHAO (plataforma de pedra)
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

        g.setColor(new Color(60, 150, 60));                  // grama entre a muralha e a plataforma
        g.fillRect(0, Y_CHAO, w, 6);
        g.setColor(PEDRA_FUNDO);
        g.fillRect(0, Y_CHAO + 6, w, yPrimeira - Y_CHAO - 6);
        for (int linha = 0; linha < CenarioDojo.TOTAL_LINHAS; linha++) {
            g.setColor(linha % 2 == 0 ? PEDRA_A : PEDRA_B);
            g.fillRect(0, CenarioDojo.yCentroLinha(linha) - metade, w, CenarioDojo.ESPACO_LINHAS);
        }
        // Juntas das lajes em perspectiva (deslocadas por fileira, como lajes de pedra)
        double fugaX = w / 2.0;
        double fugaY = -600;
        g.setColor(JUNTA);
        g.setStroke(new BasicStroke(1.2f));
        for (int fila = -1; fila < CenarioDojo.TOTAL_LINHAS; fila++) {
            int ya = fila < 0 ? Y_CHAO + 6 : yPrimeira + fila * CenarioDojo.ESPACO_LINHAS;
            int yb = fila < 0 ? yPrimeira : ya + CenarioDojo.ESPACO_LINHAS;
            int desloc = Math.floorMod(fila, 2) == 0 ? 0 : 50;
            for (int xBase = -600 + desloc; xBase <= w + 600; xBase += 100) {
                g.drawLine(projetar(xBase, ya, fugaX, fugaY, h), ya, projetar(xBase, yb, fugaX, fugaY, h), yb);
            }
            g.drawLine(0, yb, w, yb);
        }
        g.setStroke(new BasicStroke(1f));
        // Borda frontal da plataforma (degrau de pedra) abaixo dos pes da linha 5
        g.setColor(BORDA_FRENTE);
        g.fillRect(0, yUltima, w, 8);
        g.setColor(BORDA_SOMBRA);
        g.fillRect(0, yUltima + 8, w, h - yUltima - 8);
        g.setColor(JUNTA);
        for (int x = 0; x < w; x += 60) {
            g.drawLine(x, yUltima + 8, x, h);
        }
        // Sombra da muralha sobre a plataforma
        g.setPaint(new GradientPaint(0, Y_CHAO, new Color(0, 0, 0, 60), 0, Y_CHAO + 16, new Color(0, 0, 0, 0)));
        g.fillRect(0, Y_CHAO, w, 16);
    }

    private static int projetar(int xBase, int y, double fugaX, double fugaY, int h) {
        double t = (y - fugaY) / (h - fugaY);
        return (int) Math.round(fugaX + (xBase - fugaX) * t);
    }

    // =====================================================
    // PRIMEIRO PLANO
    // =====================================================
    @Override
    public void desenharPrimeiroPlano(Graphics2D g2, int largura, int altura) {
        if (frenteCache == null || frenteCache.getWidth() != largura || frenteCache.getHeight() != altura) {
            frenteCache = renderizar(largura, altura, this::pintarFrente);
        }
        g2.drawImage(frenteCache, 0, 0, null);
    }

    private void pintarFrente(Graphics2D g, int w, int h) {
        int y0 = CenarioDojo.yCentroLinha(CenarioDojo.TOTAL_LINHAS - 1) + CenarioDojo.ESPACO_LINHAS;
        if (y0 < h) {
            g.setPaint(new GradientPaint(0, y0, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 60)));
            g.fillRect(0, y0, w, h - y0);
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
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pintor.pintar(g, largura, altura);
        } finally {
            g.dispose();
        }
        return img;
    }
}
