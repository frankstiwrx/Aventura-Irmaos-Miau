package com.irmaosmiau.cenario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Entrega 5P.4: Academia dos Irmaos Miau (fachada), desenhada com Java2D.
 *
 * A luta acontece na area externa, num tatame azul com borda vermelha em frente
 * a academia: telhado azul-marinho, letreiro "ACADEMIA DOS IRMAOS MIAU" com a
 * pata amarela, bandeiras com os rostos do Miau Branco e do Miau Preto, porta
 * de vidro mostrando o tatame amarelo e vermelho la dentro, mural com dois
 * lutadores, modalidades (JIU-JITSU, JUDO, WRESTLING, SAMBO), banco, vaso,
 * cabideiro de faixas, alambrado, arvore e predio ao longe.
 *
 * Escala: a fachada (y 112 a 296) e mais alta que os lutadores (190 px); a
 * porta (~120 px) fica do tamanho de um lutador recuado. Chao a partir de
 * y = 296, uma faixa por linha (pes em y = 326 + 20 x linha).
 */
public final class CenarioFachadaAcademia implements Cenario {

    public static final String ID = "academia_miau";

    public static final int Y_CHAO = CenarioDojo.Y_BASE_PAREDE;        // 296
    /** Beiral do telhado principal. */
    public static final int Y_BEIRAL = 112;

    private static final Color CEU_TOPO = new Color(40, 140, 236);
    private static final Color CEU_BASE = new Color(150, 210, 250);
    private static final Color NUVEM = new Color(250, 252, 255);
    private static final Color MARINHO = new Color(30, 50, 110);
    private static final Color MARINHO_CLARO = new Color(52, 78, 150);
    private static final Color FACHADA = new Color(238, 232, 226);
    private static final Color FACHADA_SOMBRA = new Color(214, 206, 200);
    private static final Color VERMELHO = new Color(204, 52, 50);
    private static final Color AMARELO = new Color(250, 200, 30);
    private static final Color MADEIRA = new Color(150, 96, 52);
    private static final Color VIDRO = new Color(170, 210, 236, 120);
    private static final Color INTERIOR = new Color(226, 186, 130);
    private static final Color FOLHA = new Color(90, 170, 40);
    private static final Color FOLHA_ESCURA = new Color(60, 130, 30);
    private static final Color TATAME_A = new Color(46, 112, 214);
    private static final Color TATAME_B = new Color(38, 100, 200);
    private static final Color TATAME_JUNTA = new Color(26, 74, 160);
    private static final Color BORDA = new Color(226, 70, 70);

    private BufferedImage fundoCache;
    private BufferedImage chaoCache;
    private BufferedImage frenteCache;
    private long tickAmbiente;

    @Override
    public void atualizarAmbiente(long tick) {
        tickAmbiente = tick;
    }

    /** Camada dinamica: a copa da arvore balanca de leve (o tronco fica no cache). */
    private void folhagem(Graphics2D g) {
        int x = 34;
        double b = 3 * Math.sin(tickAmbiente / 45.0);
        g.setColor(FOLHA_ESCURA);
        g.fill(new Ellipse2D.Double(x - 50 + b, -30, 150, 110));
        g.setColor(FOLHA);
        g.fill(new Ellipse2D.Double(x - 30 + b * 1.4, -20, 100, 80));
        g.fill(new Ellipse2D.Double(x + 40 + b * 0.8, 10, 90, 60));
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getNome() {
        return "Academia dos Irmãos Miau";
    }

    @Override
    public void desenharFundo(Graphics2D g2, int largura, int altura) {
        if (fundoCache == null || fundoCache.getWidth() != largura || fundoCache.getHeight() != altura) {
            fundoCache = renderizar(largura, altura, this::pintarFundo);
        }
        g2.drawImage(fundoCache, 0, 0, null);
        folhagem(g2);                                          // 5Q.1: camada dinamica
    }

    private void pintarFundo(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, CEU_TOPO, 0, Y_CHAO, CEU_BASE));
        g.fillRect(0, 0, w, Y_CHAO);
        nuvem(g, 330, 40, 0.8);
        nuvem(g, 640, 28, 1.0);
        nuvem(g, 760, 70, 0.6);
        // predio ao longe (direita)
        g.setColor(new Color(244, 190, 160));
        g.fillRect(w - 110, 60, 80, 110);
        g.setColor(new Color(230, 140, 110));
        g.fillRect(w - 116, 54, 92, 8);
        g.setColor(new Color(110, 170, 220));
        for (int y = 76; y < 160; y += 26) {
            g.fillRect(w - 100, y, 18, 14);
            g.fillRect(w - 66, y, 18, 14);
        }
        // alambrado nas laterais
        alambrado(g, 0, 120, 190);
        alambrado(g, w - 120, w, 190);
        // arvore (esquerda) e arbustos
        arvore(g, 34, Y_CHAO);
        arbusto(g, 20, Y_CHAO - 4, 34);
        arbusto(g, w - 24, Y_CHAO - 4, 34);

        int x0 = 70;
        int x1 = w - 70;
        int cx = w / 2;
        // corpo da fachada
        g.setColor(FACHADA);
        g.fillRect(x0, 150, x1 - x0, Y_CHAO - 150);
        g.setColor(MARINHO);
        g.fillRect(x0, Y_CHAO - 22, x1 - x0, 22);              // rodape azul
        g.setColor(VERMELHO);
        g.fillRect(x0, 150, 10, Y_CHAO - 150);
        g.fillRect(x1 - 10, 150, 10, Y_CHAO - 150);
        // telhados: abas laterais e frontao central
        g.setColor(MARINHO);
        g.fillRect(x0 - 16, 138, x1 - x0 + 32, 14);
        Polygon frontao = new Polygon(new int[] {cx - 200, cx, cx + 200}, new int[] {Y_BEIRAL + 8, 40, Y_BEIRAL + 8}, 3);
        g.fillPolygon(frontao);
        g.setColor(new Color(214, 206, 214));
        g.fillPolygon(new int[] {cx - 176, cx, cx + 176}, new int[] {Y_BEIRAL + 8, 56, Y_BEIRAL + 8}, 3);
        g.setColor(MADEIRA);
        g.fillRect(cx - 4, 60, 8, Y_BEIRAL - 52);
        g.setColor(MARINHO);
        g.fillRect(cx - 190, Y_BEIRAL + 4, 380, 10);
        // letreiro
        g.setColor(MARINHO);
        g.fillRect(cx - 150, 66, 300, 54);
        g.setColor(MARINHO_CLARO);
        g.drawRect(cx - 150, 66, 299, 53);
        pata(g, cx - 120, 98, 18, AMARELO);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g.setColor(Color.WHITE);
        g.drawString("ACADEMIA DOS", cx - 82, 86);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        g.setColor(AMARELO);
        g.drawString("IRMÃOS MIAU", cx - 92, 112);
        // luminarias do beiral
        g.setColor(new Color(255, 236, 170));
        for (int x : new int[] {cx - 140, cx - 40, cx + 40, cx + 140, x0 + 30, x1 - 50}) {
            g.fillRoundRect(x - 12, x == x0 + 30 || x == x1 - 50 ? 152 : Y_BEIRAL + 14, 24, 4, 3, 3);
        }
        // entrada de vidro com o interior
        int px = cx - 96;
        int py = 158;
        int pw = 192;
        int ph = Y_CHAO - 22 - py;
        g.setColor(INTERIOR);
        g.fillRect(px, py, pw, ph);
        g.setColor(new Color(110, 70, 40));
        g.fillRect(px, py, pw, 10);                            // teto interno
        g.setColor(new Color(255, 240, 200));
        for (int x = px + 24; x < px + pw; x += 52) {
            g.fillRect(x, py + 3, 22, 3);
        }
        g.setColor(VERMELHO);
        g.fillRect(px, py + ph - 26, pw, 26);                  // tatame interno
        g.setColor(AMARELO);
        g.fillRect(px + 14, py + ph - 22, pw - 28, 18);
        g.setColor(VERMELHO);
        g.fill(new Ellipse2D.Double(cx - 18, py + 24, 36, 36));
        pata(g, cx, py + 44, 11, Color.WHITE);
        prateleira(g, px + 14, py + 30, true);
        prateleira(g, px + pw - 54, py + 30, false);
        g.setColor(VIDRO);
        g.fillRect(px, py, 44, ph);
        g.fillRect(px + pw - 44, py, 44, ph);
        g.setColor(new Color(255, 255, 255, 90));
        g.setStroke(new BasicStroke(3f));
        g.drawLine(px + 8, py + 70, px + 30, py + 30);
        g.drawLine(px + pw - 36, py + 70, px + pw - 14, py + 30);
        g.setStroke(new BasicStroke(4f));
        g.setColor(MARINHO);
        g.drawRect(px, py, pw, ph);
        g.drawLine(px + 44, py, px + 44, py + ph);
        g.drawLine(px + pw - 44, py, px + pw - 44, py + ph);
        g.setStroke(new BasicStroke(1f));
        // bandeiras com os rostos dos Miaus
        bandeira(g, cx - 140, 120, false);
        bandeira(g, cx + 140, 120, true);
        // mural (esquerda): dois lutadores e riscos vermelhos
        mural(g, x0 + 16, 160);
        // modalidades (direita)
        g.setColor(MARINHO);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        int my = 180;
        for (String m : new String[] {"JIU-JITSU", "JUDÔ", "WRESTLING", "SAMBO"}) {
            g.drawString(m, x1 - 116, my);
            my += 17;
        }
        g.setColor(VERMELHO);
        g.fillPolygon(new int[] {x1 - 116, x1 - 30, x1 - 116}, new int[] {my - 6, my - 10, my - 3}, 3);
        // banco com mochila, vaso, cabideiro de faixas
        banco(g, x0 + 22, Y_CHAO - 22);
        vaso(g, px - 26, Y_CHAO - 22);
        cabideiro(g, x1 - 112, Y_CHAO - 22);
        // calcada
        g.setColor(new Color(214, 210, 206));
        g.fillRect(0, Y_CHAO - 6, w, 6);
    }

    private static void nuvem(Graphics2D g, int cx, int cy, double e) {
        g.setColor(NUVEM);
        int[][] b = {{-40, 4, 18}, {-16, -6, 24}, {12, -8, 22}, {36, 2, 16}};
        for (int[] o : b) {
            g.fill(new Ellipse2D.Double(cx + (o[0] - o[2]) * e, cy + (o[1] - o[2]) * e, o[2] * 2 * e, o[2] * 1.6 * e));
        }
    }

    private static void alambrado(Graphics2D g, int x0, int x1, int topo) {
        g.setColor(new Color(60, 140, 70));
        g.fillRect(x0, topo + 20, x1 - x0, Y_CHAO - topo - 20);
        g.setColor(new Color(40, 60, 70, 170));
        for (int x = x0 - 120; x < x1; x += 10) {
            g.drawLine(x, Y_CHAO, x + (Y_CHAO - topo), topo);
            g.drawLine(x + (Y_CHAO - topo), Y_CHAO, x, topo);
        }
        g.setColor(new Color(50, 60, 70));
        g.fillRect(x0, topo, x1 - x0, 3);
    }

    private static void arvore(Graphics2D g, int x, int base) {
        g.setColor(MADEIRA);
        g.fillPolygon(new int[] {x - 10, x + 10, x + 30, x + 18}, new int[] {base, base, 40, 40}, 4);
        // 5Q.1: a copa e desenhada na camada dinamica (balancando)
    }

    private static void arbusto(Graphics2D g, int cx, int base, int r) {
        g.setColor(FOLHA_ESCURA);
        g.fill(new Ellipse2D.Double(cx - r, base - r * 1.4, r * 2, r * 1.6));
        g.setColor(FOLHA);
        g.fill(new Ellipse2D.Double(cx - r * 0.6, base - r * 1.3, r, r * 0.8));
    }

    private static void prateleira(Graphics2D g, int x, int y, boolean trofeus) {
        g.setColor(new Color(110, 70, 40));
        g.fillRect(x, y, 40, 44);
        g.setColor(new Color(150, 100, 60));
        g.fillRect(x + 3, y + 20, 34, 3);
        for (int i = 0; i < 3; i++) {
            g.setColor(trofeus ? AMARELO : Color.WHITE);
            g.fillRect(x + 5 + i * 11, y + (trofeus ? 6 : 26), 8, 12);
        }
    }

    /** Bandeira branca com o rosto do Miau (true = Preto). */
    private static void bandeira(Graphics2D g, int cx, int y, boolean preto) {
        g.setColor(MADEIRA);
        g.fillRect(cx - 32, y - 4, 64, 4);
        g.setColor(new Color(248, 248, 248));
        g.fillRect(cx - 28, y, 56, 82);
        Color rosto = preto ? new Color(28, 28, 40) : Color.WHITE;
        g.setColor(rosto);
        g.fillPolygon(new int[] {cx - 18, cx - 14, cx - 6}, new int[] {y + 30, y + 14, y + 24}, 3);
        g.fillPolygon(new int[] {cx + 18, cx + 14, cx + 6}, new int[] {y + 30, y + 14, y + 24}, 3);
        g.fill(new Ellipse2D.Double(cx - 18, y + 22, 36, 30));
        g.setColor(MARINHO);
        g.setStroke(new BasicStroke(2f));
        if (!preto) {
            g.draw(new Ellipse2D.Double(cx - 18, y + 22, 36, 30));
            g.drawLine(cx - 18, y + 30, cx - 14, y + 14);
            g.drawLine(cx + 18, y + 30, cx + 14, y + 14);
        }
        g.setColor(preto ? Color.WHITE : MARINHO);
        g.drawLine(cx - 10, y + 33, cx - 4, y + 36);           // olhos bravos
        g.drawLine(cx + 10, y + 33, cx + 4, y + 36);
        g.fillRect(cx - 1, y + 42, 3, 2);
        g.setStroke(new BasicStroke(1f));
    }

    private static void mural(Graphics2D g, int x, int y) {
        g.setColor(VERMELHO);
        for (int i = 0; i < 4; i++) {
            g.fillPolygon(new int[] {x + 4 + i * 30, x + 30 + i * 30, x + 10 + i * 30},
                    new int[] {y + 40 + i * 8, y + 12 + i * 8, y + 44 + i * 8}, 3);
        }
        g.setColor(MARINHO_CLARO);
        // dois lutadores agarrados (silhuetas)
        g.fill(new Ellipse2D.Double(x + 30, y + 30, 18, 18));
        g.fill(new Ellipse2D.Double(x + 74, y + 30, 18, 18));
        g.fillPolygon(new int[] {x + 22, x + 56, x + 62, x + 50, x + 30}, new int[] {y + 96, y + 52, y + 64, y + 96, y + 96}, 5);
        g.fillPolygon(new int[] {x + 100, x + 66, x + 60, x + 72, x + 92}, new int[] {y + 96, y + 52, y + 64, y + 96, y + 96}, 5);
        g.fillRect(x + 50, y + 54, 22, 8);
    }

    private static void banco(Graphics2D g, int x, int base) {
        g.setColor(MADEIRA);
        g.fillRect(x, base - 26, 96, 7);
        g.fillRect(x + 6, base - 20, 6, 20);
        g.fillRect(x + 84, base - 20, 6, 20);
        g.setColor(VERMELHO);
        g.fillRoundRect(x + 14, base - 46, 40, 20, 10, 10);
        g.setColor(new Color(30, 30, 34));
        g.fillRect(x + 64, base - 46, 9, 20);
        g.setColor(Color.WHITE);
        g.fillRect(x + 54, base - 22, 18, 10);
    }

    private static void vaso(Graphics2D g, int cx, int base) {
        g.setColor(new Color(244, 200, 200));
        g.fillPolygon(new int[] {cx - 12, cx + 12, cx + 9, cx - 9}, new int[] {base - 24, base - 24, base, base}, 4);
        g.setColor(FOLHA);
        for (int a = -60; a <= 60; a += 30) {
            double r = Math.toRadians(a - 90);
            g.fillPolygon(new int[] {cx - 3, cx + 3, (int) (cx + Math.cos(r) * 40)},
                    new int[] {base - 24, base - 24, (int) (base - 24 + Math.sin(r) * 40)}, 3);
        }
    }

    private static void cabideiro(Graphics2D g, int x, int base) {
        g.setColor(MADEIRA);
        g.fillRect(x, base - 56, 4, 56);
        g.fillRect(x + 56, base - 56, 4, 56);
        g.fillRect(x, base - 56, 60, 4);
        Color[] faixas = {new Color(40, 80, 200), new Color(120, 60, 170), new Color(30, 30, 34)};
        for (int i = 0; i < 3; i++) {
            g.setColor(faixas[i]);
            g.fillRect(x + 10 + i * 16, base - 52, 9, 34);
        }
    }

    private static void pata(Graphics2D g, int cx, int cy, int r, Color cor) {
        g.setColor(cor);
        double s = r / 10.0;
        g.fill(new Ellipse2D.Double(cx - 6 * s, cy - 1 * s, 12 * s, 10 * s));
        g.fill(new Ellipse2D.Double(cx - 10 * s, cy - 7 * s, 5 * s, 6 * s));
        g.fill(new Ellipse2D.Double(cx - 5 * s, cy - 11 * s, 4.5 * s, 6 * s));
        g.fill(new Ellipse2D.Double(cx + 0.5 * s, cy - 11 * s, 4.5 * s, 6 * s));
        g.fill(new Ellipse2D.Double(cx + 5 * s, cy - 7 * s, 5 * s, 6 * s));
    }

    // ---------------- chao: tatame azul com borda vermelha
    @Override
    public void desenharChao(Graphics2D g2, int largura, int altura) {
        if (chaoCache == null || chaoCache.getWidth() != largura || chaoCache.getHeight() != altura) {
            chaoCache = renderizar(largura, altura, this::pintarChao);
        }
        g2.drawImage(chaoCache, 0, 0, null);
    }

    private void pintarChao(Graphics2D g, int w, int h) {
        tatame(g, w, h, BORDA, TATAME_A, TATAME_B, TATAME_JUNTA);
        // sombras de folhas da arvore no tatame
        g.setColor(new Color(10, 40, 110, 70));
        for (int[] s : new int[][] {{20, 330, 70, 14}, {60, 360, 50, 10}, {w - 110, 380, 90, 14}, {w - 70, 410, 60, 10}}) {
            g.fill(new Ellipse2D.Double(s[0], s[1], s[2], s[3]));
        }
    }

    /** Tatame comum aos dois mapas desta entrega: faixa da borda no fundo e uma faixa por linha. */
    static void tatame(Graphics2D g, int w, int h, Color borda, Color a, Color b, Color junta) {
        int metade = CenarioDojo.ESPACO_LINHAS / 2;
        int yPrimeira = CenarioDojo.yCentroLinha(0) - metade;
        int yUltima = CenarioDojo.yCentroLinha(CenarioDojo.TOTAL_LINHAS - 1) + metade;
        g.setColor(borda);
        g.fillRect(0, Y_CHAO, w, yPrimeira - Y_CHAO);
        for (int linha = 0; linha < CenarioDojo.TOTAL_LINHAS; linha++) {
            g.setColor(linha % 2 == 0 ? a : b);
            g.fillRect(0, CenarioDojo.yCentroLinha(linha) - metade, w, CenarioDojo.ESPACO_LINHAS);
        }
        g.setColor(borda);
        g.fillRect(0, yUltima, w, h - yUltima);
        g.setColor(junta);
        g.setStroke(new BasicStroke(1.2f));
        for (int y = yPrimeira; y <= yUltima; y += 2 * CenarioDojo.ESPACO_LINHAS) {
            g.drawLine(0, y, w, y);
        }
        double fugaX = w / 2.0;
        double fugaY = -500;
        for (int xb = -900; xb <= w + 900; xb += 110) {
            int xa = (int) Math.round(fugaX + (xb - fugaX) * (yPrimeira - fugaY) / (h - fugaY));
            int xz = (int) Math.round(fugaX + (xb - fugaX) * (yUltima - fugaY) / (h - fugaY));
            g.drawLine(xa, yPrimeira, xz, yUltima);
        }
        g.setStroke(new BasicStroke(1f));
        g.setPaint(new GradientPaint(0, Y_CHAO, new Color(0, 0, 0, 60), 0, Y_CHAO + 12, new Color(0, 0, 0, 0)));
        g.fillRect(0, Y_CHAO, w, 12);
    }

    // ---------------- primeiro plano: so a sombra abaixo da linha 5
    @Override
    public void desenharPrimeiroPlano(Graphics2D g2, int largura, int altura) {
        if (frenteCache == null || frenteCache.getWidth() != largura || frenteCache.getHeight() != altura) {
            frenteCache = renderizar(largura, altura, CenarioFachadaAcademia::sombraFrente);
        }
        g2.drawImage(frenteCache, 0, 0, null);
    }

    static void sombraFrente(Graphics2D g, int w, int h) {
        int y0 = CenarioDojo.yCentroLinha(CenarioDojo.TOTAL_LINHAS - 1) + CenarioDojo.ESPACO_LINHAS;
        if (y0 < h) {
            g.setPaint(new GradientPaint(0, y0, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 50)));
            g.fillRect(0, y0, w, h - y0);
        }
    }

    @FunctionalInterface
    interface Pintor {
        void pintar(Graphics2D g, int largura, int altura);
    }

    static BufferedImage renderizar(int largura, int altura, Pintor pintor) {
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

    static FontMetrics metricas(Graphics2D g) {
        return g.getFontMetrics();
    }
}
