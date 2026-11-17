package com.irmaosmiau.cenario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Entrega 5P.3: Arena da Fortaleza Nevada ao Luar (Java2D, sem imagens).
 *
 * Interpretacao da arte de referencia: noite fria com lua cheia, montanhas
 * nevadas e pinheiros ao fundo, muralha de pedra escura com neve e pingentes de
 * gelo, pilares com acabamento vermelho, torre central com o estandarte da pata,
 * estandartes vermelhos laterais (pata e ondas: nada de escrita copiada),
 * lanternas de pedra acesas e um patio coberto de neve. Desenho proprio dos
 * Irmaos Miau.
 *
 * Escala e profundidade: a muralha (136 px) fica abaixo da altura dos lutadores
 * (190 px), os pilares e a torre sobem acima deles e as montanhas ficam bem
 * longe, mais claras (perspectiva atmosferica). O patio de neve tem uma faixa
 * por linha de movimentacao (pes em y = 326 + 20 x linha), com montes de neve
 * junto a muralha e nos cantos de baixo (abaixo dos pes da linha 5).
 */
public final class CenarioFortalezaNevada implements Cenario {

    public static final String ID = "fortaleza_neve";

    public static final int Y_CHAO = CenarioDojo.Y_BASE_PAREDE;        // 296
    /** Topo da muralha (com neve). */
    public static final int Y_MURO = 160;

    private static final Color CEU_TOPO = new Color(10, 16, 44);
    private static final Color CEU_BASE = new Color(46, 60, 112);
    private static final Color LUA = new Color(246, 248, 255);
    private static final Color LUA_SOMBRA = new Color(214, 222, 240);
    private static final Color MONTANHA_LONGE = new Color(78, 92, 146);
    private static final Color MONTANHA = new Color(56, 68, 118);
    private static final Color NEVE = new Color(236, 242, 255);
    private static final Color NEVE_SOMBRA = new Color(184, 198, 236);
    private static final Color NEVE_AZUL = new Color(150, 168, 222);
    private static final Color PINHEIRO = new Color(28, 52, 64);
    private static final Color PEDRA = new Color(40, 40, 54);
    private static final Color PEDRA_CLARA = new Color(54, 54, 70);
    private static final Color PEDRA_JUNTA = new Color(24, 24, 34);
    private static final Color VERMELHO = new Color(184, 34, 36);
    private static final Color VERMELHO_ESCURO = new Color(130, 20, 24);
    private static final Color PRETO = new Color(20, 20, 26);
    private static final Color PRATA = new Color(214, 220, 236);
    private static final Color LAMPIAO = new Color(255, 200, 90);
    private static final Color LAMPIAO_HALO = new Color(255, 170, 60, 110);
    private static final Color CHAO_A = new Color(222, 230, 250);
    private static final Color CHAO_B = new Color(208, 218, 244);
    private static final Color CHAO_FUNDO = new Color(196, 208, 240);

    private BufferedImage fundoCache;
    private BufferedImage chaoCache;
    private BufferedImage frenteCache;
    private long tickAmbiente;
    private static final int FLOCOS = 70;
    private static final int[][] SEMENTES = new int[FLOCOS][4];     // x0, y0, velocidade (x10), tamanho

    static {
        Random r = new Random(5310);
        for (int[] f : SEMENTES) {
            f[0] = r.nextInt(820) - 10;
            f[1] = r.nextInt(Y_CHAO);
            f[2] = 4 + r.nextInt(7);
            f[3] = 2 + r.nextInt(2);
        }
    }

    @Override
    public void atualizarAmbiente(long tick) {
        tickAmbiente = tick;
    }

    /** Neve caindo devagar, com balanco lateral, so ate o horizonte (atras dos lutadores). */
    private void neveCaindo(Graphics2D g, int w) {
        g.setColor(new Color(255, 255, 255, 200));
        for (int i = 0; i < FLOCOS; i++) {
            int[] f = SEMENTES[i];
            double y = Math.floorMod(f[1] + tickAmbiente * f[2] / 10, Y_CHAO - 8);
            double x = f[0] + 6 * Math.sin((tickAmbiente + i * 13) / 40.0);
            g.fill(new Ellipse2D.Double(Math.floorMod((long) x, w + 20) - 10, y, f[3], f[3]));
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getNome() {
        return "Fortaleza nevada ao luar";
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
        neveCaindo(g2, largura);                               // 5Q.1: camada dinamica
    }

    private void pintarFundo(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, CEU_TOPO, 0, Y_MURO, CEU_BASE));
        g.fillRect(0, 0, w, Y_CHAO);
        Random r = new Random(5303);                           // estrelas fixas (mesma imagem sempre)
        g.setColor(new Color(220, 230, 255, 170));
        for (int i = 0; i < 70; i++) {
            int x = r.nextInt(w);
            int y = r.nextInt(130);
            int s = r.nextInt(3) == 0 ? 2 : 1;
            g.fillRect(x, y, s, s);
        }
        // Lua cheia com halo
        int lx = w - 210;
        int ly = 52;
        g.setPaint(new RadialGradientPaint(lx, ly, 90, new float[] {0f, 0.35f, 1f},
                new Color[] {new Color(200, 214, 255, 120), new Color(150, 170, 240, 50), new Color(80, 100, 180, 0)}));
        g.fill(new Ellipse2D.Double(lx - 90, ly - 90, 180, 180));
        g.setColor(LUA);
        g.fill(new Ellipse2D.Double(lx - 27, ly - 27, 54, 54));
        g.setColor(LUA_SOMBRA);
        g.fill(new Ellipse2D.Double(lx - 12, ly - 14, 11, 9));
        g.fill(new Ellipse2D.Double(lx + 5, ly + 2, 14, 10));
        g.fill(new Ellipse2D.Double(lx - 16, ly + 8, 8, 7));

        // Montanhas: as mais longe mais claras (profundidade)
        montanha(g, 150, 70, 210, MONTANHA_LONGE);
        montanha(g, w - 140, 96, 170, MONTANHA_LONGE);
        montanha(g, 330, 40, 250, MONTANHA);
        montanha(g, w - 300, 74, 200, MONTANHA);

        // Pinheiros nevados atras da muralha
        for (int[] p : new int[][] {{20, 70, 46}, {78, 96, 36}, {w - 20, 66, 48}, {w - 82, 98, 36},
            {226, 128, 26}, {w - 230, 130, 26}}) {
            pinheiro(g, p[0], Y_MURO + 8, p[1], p[2]);
        }

        // Muralha de pedra
        muralha(g, w);
        // Pilares
        for (int x : new int[] {108, w - 108}) {
            pilar(g, x, 126, 46);
        }
        // Torre central com o estandarte da pata
        torre(g, w / 2);
        // Estandartes vermelhos e lanternas acesas
        estandarteVermelho(g, 44, 186);
        estandarteVermelho(g, w - 44, 186);
        lanterna(g, w / 2 - 186);
        lanterna(g, w / 2 + 186);
        // Neve acumulada no pe da muralha
        g.setColor(NEVE_SOMBRA);
        for (int x = -20; x < w + 40; x += 64) {
            g.fill(new Ellipse2D.Double(x, Y_CHAO - 14, 90, 22));
        }
        g.setColor(NEVE);
        for (int x = 10; x < w + 40; x += 64) {
            g.fill(new Ellipse2D.Double(x, Y_CHAO - 12, 70, 16));
        }
        // Flocos de neve caindo (fixos no fundo: nunca cobrem os lutadores)
        g.setColor(new Color(255, 255, 255, 210));
        for (int i = 0; i < 90; i++) {
            int x = r.nextInt(w);
            int y = r.nextInt(Y_CHAO - 20);
            int s = 2 + r.nextInt(2);
            g.fill(new Ellipse2D.Double(x, y, s, s));
        }
    }

    private static void montanha(Graphics2D g, int cx, int pico, int meia, Color cor) {
        int base = Y_MURO + 10;
        Polygon p = new Polygon(new int[] {cx - meia, cx - 14, cx, cx + 18, cx + meia},
                new int[] {base, pico + 12, pico, pico + 10, base}, 5);
        g.setColor(cor);
        g.fillPolygon(p);
        // capa de neve no alto
        int neveY = pico + (base - pico) / 3;
        double frac = (double) (neveY - pico) / (base - pico);
        int esq = (int) (cx - meia * frac);
        int dir = (int) (cx + meia * frac);
        Polygon n = new Polygon(new int[] {esq, cx - 14, cx, cx + 18, dir, cx + meia / 6, cx, cx - meia / 5},
                new int[] {neveY, pico + 12, pico, pico + 10, neveY, neveY - 8, neveY + 4, neveY - 6}, 8);
        g.setColor(NEVE_SOMBRA);
        g.fillPolygon(n);
        g.setColor(new Color(255, 255, 255, 70));
        g.fillPolygon(new int[] {cx, cx + 18, cx + 6}, new int[] {pico, pico + 10, neveY}, 3);
    }

    private static void pinheiro(Graphics2D g, int cx, int base, int topo, int meia) {
        g.setColor(new Color(40, 30, 30));
        g.fillRect(cx - 3, base - 14, 6, 14);
        int camadas = 4;
        for (int i = 0; i < camadas; i++) {
            int yb = base - 10 - i * (base - 10 - topo) / camadas;
            int yt = yb - (base - topo) / camadas - 14;
            int m = meia - i * meia / (camadas + 1);
            g.setColor(PINHEIRO);
            g.fillPolygon(new int[] {cx - m, cx, cx + m}, new int[] {yb, yt, yb}, 3);
            g.setColor(NEVE);
            g.fillPolygon(new int[] {cx - m + 4, cx, cx + m - 4, cx + m / 3, cx - m / 3},
                    new int[] {yb - 3, yt + 2, yb - 3, yb - 8, yb - 6}, 5);
        }
    }

    private static void muralha(Graphics2D g, int w) {
        g.setColor(PEDRA);
        g.fillRect(0, Y_MURO, w, Y_CHAO - Y_MURO);
        // blocos
        g.setColor(PEDRA_JUNTA);
        for (int y = Y_MURO + 14; y < Y_CHAO; y += 18) {
            g.drawLine(0, y, w, y);
            int desloc = ((y - Y_MURO) / 18) % 2 == 0 ? 0 : 26;
            for (int x = desloc; x < w; x += 52) {
                g.drawLine(x, y, x, Math.min(y + 18, Y_CHAO));
            }
        }
        g.setColor(new Color(255, 255, 255, 10));
        for (int y = Y_MURO + 15; y < Y_CHAO; y += 18) {
            g.drawLine(0, y, w, y);
        }
        // coroamento + neve + pingentes de gelo
        g.setColor(PEDRA_CLARA);
        g.fillRect(0, Y_MURO - 2, w, 12);
        neveNoTopo(g, 0, w, Y_MURO - 2, 8);
        pingentes(g, 0, w, Y_MURO + 8, 11);
    }

    private static void neveNoTopo(Graphics2D g, int x0, int x1, int y, int espessura) {
        Path2D n = new Path2D.Double();
        n.moveTo(x0 - 4, y + espessura);
        n.lineTo(x0 - 4, y);
        for (int x = x0; x <= x1 + 10; x += 20) {
            n.quadTo(x + 10, y - espessura, x + 20, y);
        }
        n.lineTo(x1 + 4, y + espessura);
        n.closePath();
        g.setColor(NEVE);
        g.fill(n);
        g.setColor(NEVE_SOMBRA);
        g.fillRect(x0 - 4, y + espessura - 2, x1 - x0 + 8, 2);
    }

    private static void pingentes(Graphics2D g, int x0, int x1, int y, int passo) {
        Random r = new Random(x0 * 31L + x1);
        g.setColor(new Color(200, 216, 255, 220));
        for (int x = x0 + 4; x < x1; x += passo + r.nextInt(passo)) {
            int len = 5 + r.nextInt(14);
            g.fillPolygon(new int[] {x - 2, x + 2, x}, new int[] {y, y, y + len}, 3);
        }
    }

    private static void pilar(Graphics2D g, int cx, int topo, int largura) {
        int x = cx - largura / 2;
        g.setColor(PEDRA_CLARA);
        g.fillRect(x, topo, largura, Y_CHAO - topo);
        g.setColor(PEDRA_JUNTA);
        for (int y = topo + 26; y < Y_CHAO; y += 22) {
            g.drawLine(x, y, x + largura, y);
        }
        g.drawRect(x, topo, largura - 1, Y_CHAO - topo);
        // capitel com acabamento vermelho
        g.setColor(VERMELHO);
        g.fillRect(x - 6, topo, largura + 12, 6);
        g.setColor(PEDRA);
        g.fillRect(x - 8, topo - 12, largura + 16, 12);
        neveNoTopo(g, x - 10, x + largura + 10, topo - 14, 8);
        pingentes(g, x - 8, x + largura + 8, topo - 2, 7);
        // base
        g.setColor(PEDRA);
        g.fillRect(x - 6, Y_CHAO - 26, largura + 12, 26);
    }

    private static void torre(Graphics2D g, int cx) {
        int topo = 96;
        int meia = 74;
        g.setColor(PEDRA_CLARA);
        g.fillRect(cx - meia, topo, meia * 2, Y_CHAO - topo);
        g.setColor(PEDRA_JUNTA);
        for (int y = topo + 22; y < Y_CHAO; y += 20) {
            g.drawLine(cx - meia, y, cx + meia, y);
        }
        // telhado em degraus com neve
        g.setColor(PEDRA);
        g.fillRect(cx - meia - 10, topo - 14, meia * 2 + 20, 16);
        g.fillRect(cx - 30, topo - 34, 60, 22);
        neveNoTopo(g, cx - meia - 12, cx + meia + 12, topo - 16, 9);
        neveNoTopo(g, cx - 32, cx + 32, topo - 36, 8);
        pingentes(g, cx - meia - 10, cx + meia + 10, topo + 2, 8);
        // estandarte preto com borda vermelha e a pata num circulo
        int bx = cx - 44;
        int by = topo + 16;
        int bw = 88;
        int bh = 116;
        g.setColor(new Color(150, 110, 70));
        g.fillRect(bx - 6, by - 4, bw + 12, 6);
        Polygon ban = new Polygon(new int[] {bx, bx + bw, bx + bw, cx, bx}, new int[] {by, by, by + bh, by + bh + 22, by + bh}, 5);
        g.setColor(VERMELHO);
        g.fillPolygon(ban);
        Polygon dentro = new Polygon(new int[] {bx + 5, bx + bw - 5, bx + bw - 5, cx, bx + 5},
                new int[] {by + 2, by + 2, by + bh - 3, by + bh + 15, by + bh - 3}, 5);
        g.setColor(PRETO);
        g.fillPolygon(dentro);
        g.setColor(PRATA);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(cx - 30, by + 26, 60, 60, 120, 290, Arc2D.OPEN));
        g.setStroke(new BasicStroke(1f));
        pata(g, cx, by + 58, 16, PRATA);
        // base da torre (degraus)
        g.setColor(PEDRA);
        g.fillRect(cx - meia - 8, Y_CHAO - 18, meia * 2 + 16, 18);
        neveNoTopo(g, cx - meia - 10, cx + meia + 10, Y_CHAO - 20, 6);
    }

    private static void estandarteVermelho(Graphics2D g, int cx, int y) {
        int lw = 40;
        int lh = 96;
        int x = cx - lw / 2;
        g.setColor(new Color(150, 110, 70));
        g.fillRect(x - 4, y - 4, lw + 8, 5);
        Polygon p = new Polygon(new int[] {x, x + lw, x + lw, cx, x}, new int[] {y, y, y + lh, y + lh + 14, y + lh}, 5);
        g.setColor(VERMELHO);
        g.fillPolygon(p);
        g.setColor(VERMELHO_ESCURO);
        g.drawPolygon(p);
        pata(g, cx, y + 30, 11, new Color(240, 220, 214));
        g.setColor(new Color(240, 200, 196));
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 3; i++) {
            g.draw(new Arc2D.Double(cx - 14 + i * 4, y + 58 + i * 7, 28 - i * 8, 18, 0, 180, Arc2D.OPEN));
        }
        g.setStroke(new BasicStroke(1f));
    }

    private static void lanterna(Graphics2D g, int cx) {
        int base = Y_CHAO - 4;
        g.setPaint(new RadialGradientPaint(cx, base - 76, 70, new float[] {0f, 1f},
                new Color[] {LAMPIAO_HALO, new Color(255, 170, 60, 0)}));
        g.fill(new Ellipse2D.Double(cx - 70, base - 146, 140, 140));
        g.setColor(PEDRA_CLARA);
        g.fillRect(cx - 16, base - 52, 32, 52);               // pedestal
        g.setColor(PEDRA);
        g.fillRect(cx - 22, base - 58, 44, 8);
        pata(g, cx, base - 26, 8, PEDRA_JUNTA);
        g.setColor(PEDRA_CLARA);
        g.fillRect(cx - 18, base - 96, 36, 38);               // caixa da luz
        g.setColor(LAMPIAO);
        g.fillRect(cx - 12, base - 90, 24, 26);
        g.setColor(new Color(150, 90, 30));
        g.drawLine(cx, base - 90, cx, base - 64);
        g.drawLine(cx - 12, base - 77, cx + 12, base - 77);
        g.setColor(PEDRA);
        g.fillPolygon(new int[] {cx - 28, cx + 28, cx + 12, cx - 12}, new int[] {base - 96, base - 96, base - 110, base - 110}, 4);
        g.fillRect(cx - 4, base - 120, 8, 10);
        neveNoTopo(g, cx - 28, cx + 28, base - 112, 6);
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

    // =====================================================
    // CHAO (patio de neve)
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
        g.setColor(CHAO_FUNDO);
        g.fillRect(0, Y_CHAO, w, yPrimeira - Y_CHAO);
        for (int linha = 0; linha < CenarioDojo.TOTAL_LINHAS; linha++) {
            g.setColor(linha % 2 == 0 ? CHAO_A : CHAO_B);
            g.fillRect(0, CenarioDojo.yCentroLinha(linha) - metade, w, CenarioDojo.ESPACO_LINHAS);
        }
        g.setColor(CHAO_A);
        g.fillRect(0, yUltima, w, h - yUltima);
        // ondulacoes e sombras azuis da neve (achatadas: perspectiva), maiores perto da camera
        Random r = new Random(77);
        for (int i = 0; i < 46; i++) {
            int y = Y_CHAO + 6 + r.nextInt(h - Y_CHAO - 6);
            double perto = (y - Y_CHAO) / (double) (h - Y_CHAO);
            int lw = (int) (30 + 70 * perto) + r.nextInt(30);
            int x = r.nextInt(w + 60) - 30;
            g.setColor(new Color(NEVE_AZUL.getRed(), NEVE_AZUL.getGreen(), NEVE_AZUL.getBlue(), 60));
            g.fill(new Ellipse2D.Double(x, y, lw, 3 + 4 * perto));
        }
        // brilho do luar no patio
        g.setPaint(new RadialGradientPaint(w - 210, Y_CHAO + 30, 260, new float[] {0f, 1f},
                new Color[] {new Color(255, 255, 255, 50), new Color(255, 255, 255, 0)}));
        g.fillRect(w - 470, Y_CHAO, 520, 160);
        // luz quente das lanternas no chao
        for (int cx : new int[] {w / 2 - 186, w / 2 + 186}) {
            g.setPaint(new RadialGradientPaint(cx, Y_CHAO + 6, 60, new float[] {0f, 1f},
                    new Color[] {new Color(255, 190, 90, 60), new Color(255, 190, 90, 0)}));
            g.fill(new Ellipse2D.Double(cx - 60, Y_CHAO - 6, 120, 26));
        }
        // sombra da muralha
        g.setPaint(new GradientPaint(0, Y_CHAO, new Color(30, 40, 90, 70), 0, Y_CHAO + 18, new Color(30, 40, 90, 0)));
        g.fillRect(0, Y_CHAO, w, 18);
    }

    // =====================================================
    // PRIMEIRO PLANO: montes de neve so nos cantos de baixo (abaixo da linha 5)
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
        int topo = y0 - 8;                                     // 418: nunca acima dos pes da linha 5
        for (int[] m : new int[][] {{-30, 150}, {w - 120, 150}}) {
            g.setColor(NEVE_SOMBRA);
            g.fill(new Ellipse2D.Double(m[0], topo + 4, m[1], 60));
            g.setColor(NEVE);
            g.fill(new Ellipse2D.Double(m[0] + 10, topo, m[1] - 30, 46));
            g.setColor(new Color(50, 50, 64));
            g.fill(new Ellipse2D.Double(m[0] + m[1] / 2.0, topo + 14, 34, 20));   // pedra meio enterrada
            g.setColor(NEVE);
            g.fill(new Ellipse2D.Double(m[0] + m[1] / 2.0 - 2, topo + 10, 36, 10));
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
