package com.irmaosmiau.cenario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Entrega 5P.4: Campeonato Miau, ginasio coberto desenhado com Java2D.
 *
 * Teto com trelicas e refletores, janelas altas, arquibancadas com torcida de
 * silhuetas de gatos, escadas com corrimao amarelo, estandartes com o rosto do
 * Miau, placar central "CAMPEONATO MIAU" (J1 00 / TEMPO 05:00 / J2 00, so
 * decorativo: nenhuma pontuacao), mesa com trofeu e medalhas, placas de
 * publicidade autorais e o tatame azul com faixa amarela (superficie jogavel).
 *
 * Escala: as placas da barreira (y 236 a 286) ficam abaixo da cintura dos
 * lutadores; o placar e a torcida ficam acima e atras. Chao a partir de
 * y = 296, uma faixa por linha (pes em y = 326 + 20 x linha).
 */
public final class CenarioCampeonato implements Cenario {

    public static final String ID = "campeonato";

    public static final int Y_CHAO = CenarioDojo.Y_BASE_PAREDE;        // 296
    /** Topo da barreira de placas em volta do tatame. */
    public static final int Y_BARREIRA = 236;

    private static final Color NOITE = new Color(26, 26, 64);
    private static final Color AZUL_ESCURO = new Color(36, 40, 96);
    private static final Color ROXO = new Color(96, 70, 190);
    private static final Color AMARELO = new Color(250, 196, 36);
    private static final Color BRANCO = new Color(244, 244, 250);
    private static final Color TRELICA = new Color(70, 76, 130);
    private static final Color VERMELHO = new Color(214, 50, 50);
    private static final Color AZUL = new Color(40, 100, 230);
    private static final Color TATAME_A = new Color(40, 112, 226);
    private static final Color TATAME_B = new Color(34, 100, 212);
    private static final Color JUNTA = new Color(24, 74, 170);

    private BufferedImage fundoCache;
    private BufferedImage chaoCache;
    private BufferedImage frenteCache;
    private long tickAmbiente;

    @Override
    public void atualizarAmbiente(long tick) {
        tickAmbiente = tick;
    }

    /** Camada dinamica: bandeirinhas tremulando e alguns torcedores da frente pulando. */
    private void ambiente(Graphics2D g, int w) {
        double t = tickAmbiente / 12.0;
        for (int i = 0; i < 2; i++) {
            int x = i == 0 ? 120 : w - 120;
            int y = 118;
            int o = (int) Math.round(3 * Math.sin(t + i));
            g.setColor(new Color(36, 36, 90));
            g.fillPolygon(new int[] {x + 3, x + 40, x + 34 + o, x + 40, x + 3},
                    new int[] {y, y + 2 + o, y + 14, y + 26 - o, y + 26}, 5);
            pata(g, x + 18, y + 14 + o / 2, 7, BRANCO);
        }
        int fila = 5;
        int y = 90 + fila * 24 + 14;
        for (int k = 0; k < 10; k++) {
            int x = 24 + k * 80 + (k % 2) * 9;
            if (Math.abs(x - w / 2) < 130) {
                continue;
            }
            int pulo = (int) Math.round(Math.max(0, 4 * Math.sin(t * 1.3 + k * 0.9)));
            torcedor(g, x, y - pulo, new Color(80, 90, 160));
        }
        torcidaExtra(g, w, t);                                 // 5Q.5
    }

    /**
     * 5Q.5: um pouco mais de torcida viva (sem descaracterizar): uma fila do meio balanca,
     * alguns levantam o braco e, de vez em quando, um flash de camera pisca na arquibancada.
     * Tudo pelo tick (deterministico) e atras dos lutadores.
     */
    private void torcidaExtra(Graphics2D g, int w, double t) {
        int cx = w / 2;
        int yMeio = 90 + 3 * 24 + 14;
        for (int k = 0; k < 9; k++) {
            int x = 50 + k * 88 + (k % 2) * 12;
            if (Math.abs(x - cx) < 130) {
                continue;
            }
            int balanco = (int) Math.round(2 * Math.sin(t * 0.9 + k * 1.7));
            torcedor(g, x + balanco, yMeio - Math.max(0, balanco), new Color(70, 80, 150));
        }
        g.setColor(new Color(214, 190, 120));
        for (int k = 0; k < 6; k++) {
            int x = 70 + k * 130;
            if (Math.abs(x - cx) < 130) {
                continue;
            }
            double s = Math.sin(t * 1.1 + k * 2.3);
            if (s > 0.2) {                                     // braco erguido (comemora)
                int y = 90 + (k % 3 + 2) * 24 + 14;
                int a = (int) Math.round(6 * s);
                g.fillRoundRect(x + 5, y - 20 - a, 3, 12 + a, 2, 2);
            }
        }
        long ciclo = tickAmbiente % 150;
        if (ciclo < 4) {                                       // flash de camera (rapido e discreto)
            int k = (int) ((tickAmbiente / 150) % 7);
            int x = 40 + k * 113;
            if (Math.abs(x - cx) > 120) {
                int y = 90 + (k % 4 + 1) * 24 + 6;
                g.setColor(new Color(255, 255, 240, 200 - (int) ciclo * 40));
                g.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6));
            }
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getNome() {
        return "Campeonato Miau";
    }

    @Override
    public void desenharFundo(Graphics2D g2, int largura, int altura) {
        if (fundoCache == null || fundoCache.getWidth() != largura || fundoCache.getHeight() != altura) {
            fundoCache = CenarioFachadaAcademia.renderizar(largura, altura, this::pintarFundo);
        }
        g2.drawImage(fundoCache, 0, 0, null);
        ambiente(g2, largura);                                 // 5Q.1: camada dinamica
    }

    private void pintarFundo(Graphics2D g, int w, int h) {
        int cx = w / 2;
        g.setPaint(new GradientPaint(0, 0, NOITE, 0, Y_CHAO, AZUL_ESCURO));
        g.fillRect(0, 0, w, Y_CHAO);
        // trelicas do teto e refletores
        g.setColor(TRELICA);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(0, 8, w, 8);
        g.drawLine(0, 26, w, 26);
        for (int x = 0; x < w; x += 26) {
            g.drawLine(x, 8, x + 13, 26);
            g.drawLine(x + 26, 8, x + 13, 26);
        }
        g.setStroke(new BasicStroke(1f));
        for (int x : new int[] {60, 150, 240, w - 240, w - 150, w - 60}) {
            g.setPaint(new RadialGradientPaint(x, 34, 60, new float[] {0f, 1f},
                    new Color[] {new Color(255, 236, 190, 120), new Color(255, 236, 190, 0)}));
            g.fill(new Ellipse2D.Double(x - 60, -26, 120, 120));
            g.setColor(new Color(255, 244, 210));
            g.fillRoundRect(x - 16, 28, 32, 10, 6, 6);
        }
        // janelas altas
        g.setColor(new Color(60, 150, 220));
        for (int x = 16; x < w; x += 46) {
            if (Math.abs(x + 18 - cx) > 110) {
                g.fillRect(x, 52, 36, 26);
            }
        }
        // arquibancadas com a torcida (silhuetas de gatos)
        Random r = new Random(504);
        for (int fila = 0; fila < 6; fila++) {
            int y = 90 + fila * 24;
            g.setColor(new Color(44 + fila * 3, 48 + fila * 3, 104));
            g.fillRect(0, y + 12, w, 12);
            for (int x = 6 + (fila % 2) * 9; x < w; x += 18) {
                if (Math.abs(x - cx) < 116 && fila < 5) {
                    continue;                                  // atras do placar
                }
                int tom = 70 + r.nextInt(70);
                Color c = r.nextInt(7) == 0 ? new Color(200, 160, 90) : new Color(tom / 2, tom / 2 + 10, tom + 40);
                torcedor(g, x, y + 14, c);
            }
        }
        // escadas com corrimao amarelo (laterais)
        escada(g, 0, false);
        escada(g, w, true);
        // placar central
        placar(g, cx);
        // estandartes com o rosto do Miau
        estandarte(g, 200, 50);
        estandarte(g, w - 200, 50);
        // bandeirinhas de pata: 5Q.1 desenhadas na camada dinamica (tremulando)
        g.setColor(new Color(200, 200, 210));
        g.fillRect(120, 118, 3, 70);
        g.fillRect(w - 120, 118, 3, 70);
        // mesa com trofeu, medalhas e plantas
        mesa(g, cx);
        // barreira de placas
        barreira(g, w, cx);
    }

    private static void torcedor(Graphics2D g, int x, int y, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(x - 7, y - 12, 14, 13));
        g.fillPolygon(new int[] {x - 7, x - 5, x - 1}, new int[] {y - 8, y - 16, y - 11}, 3);
        g.fillPolygon(new int[] {x + 7, x + 5, x + 1}, new int[] {y - 8, y - 16, y - 11}, 3);
        g.fillRoundRect(x - 8, y - 2, 16, 12, 6, 6);
    }

    private static void escada(Graphics2D g, int borda, boolean direita) {
        int s = direita ? -1 : 1;
        g.setColor(new Color(170, 174, 200));
        for (int i = 0; i < 6; i++) {
            int y = 150 + i * 14;
            int x0 = borda + s * (8 + i * 10);
            g.fillRect(Math.min(x0, x0 + s * 40), y, 40, 6);
        }
        g.setColor(AMARELO);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(borda + s * 6, 146, borda + s * 66, 222);
        g.setStroke(new BasicStroke(1f));
    }

    private static void placar(Graphics2D g, int cx) {
        g.setColor(TRELICA);
        g.fillRect(cx - 120, 34, 6, 200);
        g.fillRect(cx + 114, 34, 6, 200);
        g.setColor(new Color(20, 20, 40));
        g.fillRoundRect(cx - 112, 40, 224, 124, 10, 10);
        g.setColor(AMARELO);
        CenarioCampeonato.pata(g, cx, 60, 11, AMARELO);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Arc2D.Double(cx - 92, 56, 44, 44, 110, 120, Arc2D.OPEN));     // louros
        g.draw(new Arc2D.Double(cx + 48, 56, 44, 44, -50, 120, Arc2D.OPEN));
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        centro(g, "CAMPEONATO", cx, 86, BRANCO);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 22));
        centro(g, "MIAU", cx, 108, AMARELO);
        // J1 / TEMPO / J2 (decorativo)
        caixa(g, cx - 100, 118, 54, VERMELHO, "J1", "00");
        caixa(g, cx - 40, 118, 80, new Color(40, 40, 60), "TEMPO", "05:00");
        caixa(g, cx + 46, 118, 54, AZUL, "J2", "00");
    }

    private static void caixa(Graphics2D g, int x, int y, int lw, Color topo, String rotulo, String valor) {
        g.setColor(new Color(12, 12, 24));
        g.fillRect(x, y, lw, 40);
        g.setColor(topo);
        g.fillRect(x, y, lw, 14);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        centro(g, rotulo, x + lw / 2, y + 11, BRANCO);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        centro(g, valor, x + lw / 2, y + 35, BRANCO);
    }

    private static void centro(Graphics2D g, String t, int cx, int y, Color c) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(c);
        g.drawString(t, cx - fm.stringWidth(t) / 2, y);
    }

    private static void estandarte(Graphics2D g, int cx, int y) {
        int lw = 64;
        int lh = 120;
        int x = cx - lw / 2;
        Polygon p = new Polygon(new int[] {x, x + lw, x + lw, cx, x}, new int[] {y, y, y + lh, y + lh + 22, y + lh}, 5);
        g.setColor(AMARELO);
        g.fillPolygon(p);
        Polygon d = new Polygon(new int[] {x + 4, x + lw - 4, x + lw - 4, cx, x + 4},
                new int[] {y + 4, y + 4, y + lh - 2, y + lh + 16, y + lh - 2}, 5);
        g.setColor(BRANCO);
        g.fillPolygon(d);
        g.setColor(ROXO);
        g.fillPolygon(new int[] {x + 4, cx, x + lw - 4}, new int[] {y + 4, y + 22, y + 4}, 3);
        g.fillPolygon(new int[] {x + 4, cx, x + lw - 4, x + lw - 4, cx, x + 4},
                new int[] {y + lh - 22, y + lh - 6, y + lh - 22, y + lh - 2, y + lh + 16, y + lh - 2}, 6);
        // rosto do Miau (marinho)
        Color m = new Color(36, 36, 90);
        g.setColor(m);
        g.fillPolygon(new int[] {cx - 20, cx - 16, cx - 6}, new int[] {y + 46, y + 28, y + 40}, 3);
        g.fillPolygon(new int[] {cx + 20, cx + 16, cx + 6}, new int[] {y + 46, y + 28, y + 40}, 3);
        g.fill(new Ellipse2D.Double(cx - 20, y + 38, 40, 34));
        g.setColor(BRANCO);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(cx - 11, y + 52, cx - 4, y + 55);
        g.drawLine(cx + 11, y + 52, cx + 4, y + 55);
        g.setStroke(new BasicStroke(1f));
    }


    private static void mesa(Graphics2D g, int cx) {
        g.setColor(VERMELHO);
        g.fillRect(cx - 40, 196, 80, 40);
        g.setColor(new Color(160, 30, 30));
        g.fillRect(cx - 40, 196, 80, 5);
        // trofeu
        g.setColor(AMARELO);
        g.fill(new Arc2D.Double(cx - 18, 160, 36, 36, 180, 180, Arc2D.PIE));
        g.fillRect(cx - 18, 168, 36, 10);
        g.fillRect(cx - 3, 194, 6, 4);
        g.fillRect(cx - 10, 190, 20, 6);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(cx - 26, 168, 12, 12));
        g.draw(new Ellipse2D.Double(cx + 14, 168, 12, 12));
        g.setStroke(new BasicStroke(1f));
        pata(g, cx, 180, 5, new Color(200, 140, 20));
        // medalhas
        g.setColor(new Color(140, 110, 70));
        g.fillRect(cx - 98, 186, 44, 4);
        Color[] fitas = {ROXO, VERMELHO, ROXO};
        for (int i = 0; i < 3; i++) {
            int mx = cx - 92 + i * 14;
            g.setColor(fitas[i]);
            g.fillPolygon(new int[] {mx - 3, mx + 3, mx}, new int[] {190, 190, 208}, 3);
            g.setColor(i == 1 ? new Color(200, 200, 210) : AMARELO);
            g.fill(new Ellipse2D.Double(mx - 5, 206, 10, 10));
        }
        // plantas
        for (int px : new int[] {cx - 140, cx + 140}) {
            g.setColor(new Color(150, 90, 50));
            g.fillRect(px - 10, 214, 20, 22);
            g.setColor(new Color(70, 170, 60));
            for (int a = -60; a <= 60; a += 30) {
                double rr = Math.toRadians(a - 90);
                g.fillPolygon(new int[] {px - 3, px + 3, (int) (px + Math.cos(rr) * 30)},
                        new int[] {214, 214, (int) (214 + Math.sin(rr) * 30)}, 3);
            }
        }
    }

    private static void barreira(Graphics2D g, int w, int cx) {
        g.setColor(new Color(30, 30, 80));
        g.fillRect(0, Y_BARREIRA, w, Y_CHAO - Y_BARREIRA);
        g.setColor(AMARELO);
        g.fillRect(0, Y_BARREIRA - 4, w, 4);
        placa(g, 40, cx - 150, true, "MIAU", "JIU-JITSU");
        placa(g, cx + 150, w - 40, false, "IRMÃOS", "MIAU");
        // centro: pata com louros
        g.setColor(BRANCO);
        g.fillRect(cx - 120, Y_BARREIRA + 4, 240, 50);
        g.setColor(new Color(36, 36, 90));
        g.fillRect(cx - 114, Y_BARREIRA + 9, 228, 40);
        pata(g, cx, Y_BARREIRA + 32, 13, AMARELO);
        g.setColor(AMARELO);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Arc2D.Double(cx - 56, Y_BARREIRA + 12, 30, 30, 110, 120, Arc2D.OPEN));
        g.draw(new Arc2D.Double(cx + 26, Y_BARREIRA + 12, 30, 30, -50, 120, Arc2D.OPEN));
        g.setStroke(new BasicStroke(1f));
    }

    private static void placa(Graphics2D g, int x0, int x1, boolean comPata, String l1, String l2) {
        int y = Y_BARREIRA + 6;
        g.setColor(new Color(36, 36, 90));
        g.fillRect(x0, y, x1 - x0, 46);
        g.setColor(BRANCO);
        g.drawRect(x0, y, x1 - x0 - 1, 45);
        // listras diagonais
        for (int i = 0; i < 3; i++) {
            g.setColor(i == 1 ? BRANCO : (i == 0 ? AMARELO : ROXO));
            int xs = comPata ? x0 + 8 + i * 12 : x1 - 50 + i * 12;
            g.fillPolygon(new int[] {xs, xs + 8, xs + 22, xs + 14}, new int[] {y + 46, y + 46, y, y}, 4);
        }
        int cx = (x0 + x1) / 2 + (comPata ? 20 : -20);
        if (comPata) {
            pata(g, cx - 56, y + 25, 12, AMARELO);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 15));
        centro(g, l1, cx, y + 20, BRANCO);
        centro(g, l2, cx, y + 39, BRANCO);
    }

    static void pata(Graphics2D g, int cx, int cy, int r, Color cor) {
        g.setColor(cor);
        double s = r / 10.0;
        g.fill(new Ellipse2D.Double(cx - 6 * s, cy - 1 * s, 12 * s, 10 * s));
        g.fill(new Ellipse2D.Double(cx - 10 * s, cy - 7 * s, 5 * s, 6 * s));
        g.fill(new Ellipse2D.Double(cx - 5 * s, cy - 11 * s, 4.5 * s, 6 * s));
        g.fill(new Ellipse2D.Double(cx + 0.5 * s, cy - 11 * s, 4.5 * s, 6 * s));
        g.fill(new Ellipse2D.Double(cx + 5 * s, cy - 7 * s, 5 * s, 6 * s));
    }

    // ---------------- chao: tatame azul com faixa amarela
    @Override
    public void desenharChao(Graphics2D g2, int largura, int altura) {
        if (chaoCache == null || chaoCache.getWidth() != largura || chaoCache.getHeight() != altura) {
            chaoCache = CenarioFachadaAcademia.renderizar(largura, altura,
                    (g, w, h) -> CenarioFachadaAcademia.tatame(g, w, h, AMARELO, TATAME_A, TATAME_B, JUNTA));
        }
        g2.drawImage(chaoCache, 0, 0, null);
    }

    @Override
    public void desenharPrimeiroPlano(Graphics2D g2, int largura, int altura) {
        if (frenteCache == null || frenteCache.getWidth() != largura || frenteCache.getHeight() != altura) {
            frenteCache = CenarioFachadaAcademia.renderizar(largura, altura, CenarioFachadaAcademia::sombraFrente);
        }
        g2.drawImage(frenteCache, 0, 0, null);
    }
}
