package com.irmaosmiau.cenario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Entrega 5P.3: Academia de jiu-jitsu dos Irmaos Miau (Java2D, sem imagens).
 *
 * Interpretacao da arte de referencia: sala interna com luminarias tubulares
 * no teto, parede clara com emblemas, barra fixa a esquerda, protecao de EVA
 * preta encaixada (quebra-cabeca) na base da parede e tatame cinza-azulado em
 * placas. Os emblemas sao AUTORAIS (nenhuma marca real): o selo central
 * "JIU-JITSU * IRMAOS MIAU" com o monograma "IM" e uma faixa de graduacao, a
 * placa "ACADEMIA MIAU" e o selo amarelo "OSS!".
 *
 * Escala: a protecao de EVA (64 px) bate abaixo da cintura dos lutadores
 * (190 px em pe); o selo central (138 px) fica do tamanho do tronco; as paredes
 * laterais em perspectiva dao profundidade a sala. O tatame tem uma fileira de
 * placas por linha de movimentacao (pes em y = 326 + 20 x linha) e comeca no
 * encontro com a parede (y = 296), como os outros mapas.
 */
public final class CenarioAcademia implements Cenario {

    public static final String ID = "academia";

    public static final int Y_CHAO = CenarioDojo.Y_BASE_PAREDE;        // 296
    /** Topo da protecao de EVA da parede. */
    public static final int Y_EVA = 232;
    /** Encontro da parede com o teto. */
    public static final int Y_TETO = 46;
    /** Recuo das paredes laterais (perspectiva da sala). */
    private static final int LATERAL = 34;

    private static final Color TETO = new Color(214, 204, 206);
    private static final Color TETO_SOMBRA = new Color(190, 180, 184);
    private static final Color PAREDE_TOPO = new Color(244, 234, 230);
    private static final Color PAREDE_BASE = new Color(232, 220, 216);
    private static final Color LATERAL_COR = new Color(222, 210, 206);
    private static final Color EVA = new Color(30, 30, 34);
    private static final Color EVA_LATERAL = new Color(22, 22, 26);
    private static final Color EVA_JUNTA = new Color(56, 56, 62);
    private static final Color TATAME_A = new Color(98, 102, 120);
    private static final Color TATAME_B = new Color(88, 92, 110);
    private static final Color TATAME_FUNDO = new Color(78, 82, 100);
    private static final Color TATAME_JUNTA = new Color(56, 58, 74);
    private static final Color TATAME_BRILHO = new Color(255, 255, 255, 26);
    private static final Color PRETO = new Color(26, 26, 28);
    private static final Color BRANCO = new Color(250, 248, 244);
    private static final Color VERMELHO = new Color(214, 44, 44);
    private static final Color LARANJA = new Color(240, 120, 30);
    private static final Color AMARELO = new Color(246, 196, 30);
    private static final Color METAL = new Color(40, 42, 46);
    private static final Color LUZ = new Color(236, 246, 255);
    private static final Color LUZ_HALO = new Color(170, 210, 255, 70);

    private BufferedImage fundoCache;
    private BufferedImage chaoCache;
    private BufferedImage frenteCache;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getNome() {
        return "Academia de jiu-jitsu";
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
        // Teto (faixa em perspectiva)
        g.setPaint(new GradientPaint(0, 0, TETO_SOMBRA, 0, Y_TETO, TETO));
        g.fillRect(0, 0, w, Y_TETO);
        // Parede do fundo
        g.setPaint(new GradientPaint(0, Y_TETO, PAREDE_TOPO, 0, Y_EVA, PAREDE_BASE));
        g.fillRect(0, Y_TETO, w, Y_CHAO - Y_TETO);
        g.setColor(new Color(200, 188, 186));
        g.fillRect(0, Y_TETO, w, 3);                          // rodateto

        // Paredes laterais (trapezios: profundidade da sala)
        lateral(g, 0, LATERAL, false, w);
        lateral(g, w, w - LATERAL, true, w);

        // Eletroduto e quadro de luz na direita
        g.setColor(new Color(176, 176, 180));
        g.setStroke(new BasicStroke(3f));
        g.drawLine(w - LATERAL - 18, Y_TETO + 4, w - LATERAL - 18, 196);
        g.drawLine(w - LATERAL - 18, 196, w - LATERAL - 40, 196);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(214, 214, 218));
        g.fillRoundRect(w - LATERAL - 62, 188, 26, 18, 3, 3);
        g.setColor(new Color(150, 150, 156));
        g.drawRoundRect(w - LATERAL - 62, 188, 26, 18, 3, 3);
        g.setColor(new Color(240, 160, 40));
        g.fillRect(w - LATERAL - 58, 194, 3, 5);
        g.fillRect(w - LATERAL - 53, 194, 3, 5);

        // Luminarias tubulares em "V" (dois pares)
        luminaria(g, w / 2 - 120, 8);                         // longe dos paineis de folego (x 10-220 / 580-790)
        luminaria(g, w / 2 + 120, 8);

        // Barra fixa (esquerda), selo central, placa e selo amarelo
        barraFixa(g, 74, 112, 150);
        seloCentral(g, w / 2 - 20, 138, 70);
        placa(g, w / 2 + 80, 120, 132, 82);
        seloOss(g, w - LATERAL - 92, 150, 42);

        // Protecao de EVA preta (quebra-cabeca)
        eva(g, w);

        // Sombra no encontro parede x tatame
        g.setPaint(new GradientPaint(0, Y_CHAO - 10, new Color(0, 0, 0, 0), 0, Y_CHAO, new Color(0, 0, 0, 70)));
        g.fillRect(0, Y_CHAO - 10, w, 10);
    }

    private static void lateral(Graphics2D g, int xBorda, int xFundo, boolean direita, int w) {
        Polygon p = new Polygon(new int[] {xBorda, xFundo, xFundo, xBorda},
                new int[] {0, Y_TETO, Y_CHAO, Y_CHAO + 30}, 4);
        g.setColor(LATERAL_COR);
        g.fillPolygon(p);
        Polygon eva = new Polygon(new int[] {xBorda, xFundo, xFundo, xBorda},
                new int[] {Y_EVA - 18, Y_EVA, Y_CHAO, Y_CHAO + 30}, 4);
        g.setColor(EVA_LATERAL);
        g.fillPolygon(eva);
        g.setColor(new Color(0, 0, 0, 40));
        g.drawLine(xFundo, Y_TETO, xFundo, Y_CHAO);
    }

    private static void luminaria(Graphics2D g, int cx, int y) {
        for (int s : new int[] {-1, 1}) {
            int x0 = cx + s * 6;
            int x1 = cx + s * 92;
            int y1 = y + 28;
            g.setColor(LUZ_HALO);
            g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x0, y, x1, y1);
            g.setColor(LUZ);
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x0, y, x1, y1);
            g.setColor(METAL);
            g.fill(new Ellipse2D.Double(x1 - 5, y1 - 5, 10, 10));
        }
        g.setStroke(new BasicStroke(1f));
        g.setColor(METAL);
        g.fill(new Ellipse2D.Double(cx - 8, y - 6, 16, 12));
    }

    private static void barraFixa(Graphics2D g, int x, int y, int comp) {
        g.setColor(new Color(0, 0, 0, 40));                   // sombra na parede
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x + 6, y + 30, x + comp + 6, y + 30);
        g.setColor(METAL);
        g.drawLine(x, y + 24, x + comp, y + 24);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x + 14, y + 24, x + 28, y);
        g.drawLine(x + comp - 14, y + 24, x + comp - 28, y);
        g.setStroke(new BasicStroke(1f));
        g.fillRect(x + 22, y - 6, 12, 10);
        g.fillRect(x + comp - 34, y - 6, 12, 10);
        g.fillRect(x + 8, y + 26, 8, 8);
        g.fillRect(x + comp - 16, y + 26, 8, 8);
    }

    /** Selo autoral: anel com "JIU-JITSU * IRMAOS MIAU", monograma IM e faixa vermelha. */
    private static void seloCentral(Graphics2D g, int cx, int cy, int r) {
        g.setColor(new Color(0, 0, 0, 36));
        g.fill(new Ellipse2D.Double(cx - r + 4, cy - r + 5, r * 2, r * 2));
        g.setColor(PRETO);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(BRANCO);
        g.fill(new Ellipse2D.Double(cx - r + 4, cy - r + 4, (r - 4) * 2, (r - 4) * 2));
        g.setColor(PRETO);
        g.fill(new Ellipse2D.Double(cx - r + 7, cy - r + 7, (r - 7) * 2, (r - 7) * 2));
        int ri = r - 22;
        g.setColor(BRANCO);
        g.fill(new Ellipse2D.Double(cx - ri, cy - ri, ri * 2, ri * 2));
        g.setColor(PRETO);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(cx - ri + 4, cy - ri + 4, (ri - 4) * 2, (ri - 4) * 2));
        g.setStroke(new BasicStroke(1f));
        textoEmArco(g, "JIU-JITSU ★ IRMÃOS MIAU", cx, cy, r - 14, new Font(Font.SANS_SERIF, Font.BOLD, 11), BRANCO);
        // Monograma
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(PRETO);
        g.drawString("IM", cx - fm.stringWidth("IM") / 2, cy + 8);
        // faixa de graduacao (preta com ponta vermelha)
        g.setColor(PRETO);
        g.fillRect(cx - 30, cy + 16, 60, 9);
        g.setColor(VERMELHO);
        g.fillRect(cx + 10, cy + 16, 14, 9);
        g.setColor(BRANCO);
        g.fillRect(cx + 13, cy + 16, 2, 9);
        g.fillRect(cx + 18, cy + 16, 2, 9);
        // pata pequena no alto
        pata(g, cx, cy - 26, 7, VERMELHO);
    }

    /** Placa preta autoral "ACADEMIA MIAU" com um icone de pata. */
    private static void placa(Graphics2D g, int x, int y, int lw, int lh) {
        g.setColor(new Color(0, 0, 0, 40));
        g.fillRect(x + 4, y + 5, lw, lh);
        g.setColor(PRETO);
        g.fillRect(x, y, lw, lh);
        g.setColor(new Color(70, 70, 76));
        g.drawRect(x, y, lw - 1, lh - 1);
        pata(g, x + 26, y + lh / 2 + 2, 11, LARANJA);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(BRANCO);
        g.drawString("ACADEMIA", x + 46, y + lh / 2 - 2);
        g.setColor(LARANJA);
        g.drawString("MIAU", x + 46, y + lh / 2 + 16);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
        g.setColor(new Color(200, 200, 204));
        g.drawString("DESDE O PRIMEIRO TATAME", x + 10, y + lh - 8);
    }

    /** Selo amarelo autoral "OSS!". */
    private static void seloOss(Graphics2D g, int cx, int cy, int r) {
        g.setColor(new Color(0, 0, 0, 40));
        g.fill(new Ellipse2D.Double(cx - r + 3, cy - r + 4, r * 2, r * 2));
        g.setColor(AMARELO);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(PRETO);
        g.fill(new Ellipse2D.Double(cx - r + 6, cy - r + 6, (r - 6) * 2, (r - 6) * 2));
        g.setColor(AMARELO);
        g.setStroke(new BasicStroke(2f));
        g.draw(new java.awt.geom.Arc2D.Double(cx - r + 12, cy - r + 12, (r - 12) * 2, (r - 12) * 2, 110, 260,
                java.awt.geom.Arc2D.OPEN));
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("OSS!", cx - fm.stringWidth("OSS!") / 2, cy + 4);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 7));
        fm = g.getFontMetrics();
        g.drawString("JIU-JITSU", cx - fm.stringWidth("JIU-JITSU") / 2, cy + 15);
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

    private static void textoEmArco(Graphics2D g, String t, int cx, int cy, int r, Font f, Color cor) {
        g.setFont(f);
        g.setColor(cor);
        FontMetrics fm = g.getFontMetrics();
        double total = 0;
        for (char ch : t.toCharArray()) {
            total += fm.charWidth(ch) + 0.5;
        }
        double ang = -Math.PI / 2 - total / r / 2;              // centrado no alto do anel
        AffineTransform antes = g.getTransform();
        for (char ch : t.toCharArray()) {
            double cw = fm.charWidth(ch) + 0.5;
            double a = ang + cw / r / 2;
            g.setTransform(antes);
            g.translate(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
            g.rotate(a + Math.PI / 2);
            g.drawString(String.valueOf(ch), (float) (-fm.charWidth(ch) / 2.0), (float) (fm.getAscent() / 2.5));
            ang += cw / r;
        }
        g.setTransform(antes);
    }

    private static void eva(Graphics2D g, int w) {
        int x0 = LATERAL;
        int x1 = w - LATERAL;
        g.setColor(EVA);
        g.fillRect(x0, Y_EVA, x1 - x0, Y_CHAO - Y_EVA);
        // borda superior em dentes de quebra-cabeca
        g.setColor(EVA);
        for (int x = x0; x < x1; x += 16) {
            g.fillRect(x + 4, Y_EVA - 4, 8, 5);
        }
        g.setColor(EVA_JUNTA);
        int placa = 104;
        for (int x = x0 + placa; x < x1; x += placa) {
            // junta vertical com encaixes
            Path2D j = new Path2D.Double();
            j.moveTo(x, Y_EVA);
            for (int y = Y_EVA; y < Y_CHAO; y += 16) {
                j.lineTo(x, y + 5);
                j.lineTo(x + 4, y + 5);
                j.lineTo(x + 4, y + 11);
                j.lineTo(x, y + 11);
                j.lineTo(x, y + 16);
            }
            g.draw(j);
        }
        g.setColor(new Color(255, 255, 255, 14));
        g.fill(new RoundRectangle2D.Double(x0, Y_EVA + 2, x1 - x0, 10, 4, 4));
    }

    // =====================================================
    // CHAO (tatame em placas)
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
        g.setColor(TATAME_FUNDO);
        g.fillRect(0, Y_CHAO, w, yPrimeira - Y_CHAO);
        for (int linha = 0; linha < CenarioDojo.TOTAL_LINHAS; linha++) {
            g.setColor(linha % 2 == 0 ? TATAME_A : TATAME_B);
            g.fillRect(0, CenarioDojo.yCentroLinha(linha) - metade, w, CenarioDojo.ESPACO_LINHAS);
        }
        int yUltima = CenarioDojo.yCentroLinha(CenarioDojo.TOTAL_LINHAS - 1) + metade;
        g.setColor(TATAME_A);
        g.fillRect(0, yUltima, w, h - yUltima);
        // Juntas das placas (2 fileiras de linhas por placa, como placas de 1 m) em perspectiva
        double fugaX = w / 2.0;
        double fugaY = -420;
        g.setColor(TATAME_JUNTA);
        g.setStroke(new BasicStroke(1.4f));
        int[] horizontais = {Y_CHAO, yPrimeira, yPrimeira + 2 * CenarioDojo.ESPACO_LINHAS,
            yPrimeira + 4 * CenarioDojo.ESPACO_LINHAS, yUltima + 12};
        for (int y : horizontais) {
            g.drawLine(0, y, w, y);
        }
        for (int xBase = -900; xBase <= w + 900; xBase += 150) {
            g.drawLine(projetar(xBase, Y_CHAO, fugaX, fugaY, h), Y_CHAO, projetar(xBase, h, fugaX, fugaY, h), h);
        }
        g.setStroke(new BasicStroke(1f));
        // Brilho sutil das luminarias no vinil do tatame
        for (int cx : new int[] {w / 2 - 120, w / 2 + 120}) {
            g.setPaint(new RadialGradientPaint(cx, Y_CHAO + 40, 160, new float[] {0f, 1f},
                    new Color[] {TATAME_BRILHO, new Color(255, 255, 255, 0)}));
            g.fillRect(cx - 160, Y_CHAO, 320, 120);
        }
        // Paredes laterais encontram o tatame
        g.setColor(new Color(0, 0, 0, 50));
        g.fillPolygon(new int[] {0, LATERAL, 0}, new int[] {Y_CHAO, Y_CHAO, Y_CHAO + 30}, 3);
        g.fillPolygon(new int[] {w, w - LATERAL, w}, new int[] {Y_CHAO, Y_CHAO, Y_CHAO + 30}, 3);
    }

    private static int projetar(int xBase, int y, double fugaX, double fugaY, int h) {
        double t = (y - fugaY) / (h - fugaY);
        return (int) Math.round(fugaX + (xBase - fugaX) * t);
    }

    // =====================================================
    // PRIMEIRO PLANO (so a borda de baixo, abaixo dos pes)
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
            g.setPaint(new GradientPaint(0, y0, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 50)));
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
