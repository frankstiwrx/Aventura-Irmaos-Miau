package com.irmaosmiau.hud;

import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.regras.Placar;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Placar do modo PONTOS + FINALIZACAO no topo da tela: J1 (pontos, vantagens, punicoes) | cronometro | J2.
 * Ocupa a faixa do relogio de antes (o aviso de combate continua logo abaixo); some com o HUD no F2.
 */
public final class PlacarLuta {

    public static final int LARGURA = 312;
    public static final int Y = 3;
    public static final int ALTURA = 29;

    private static final Color FUNDO = new Color(10, 30, 80, 200);
    private static final Color J1 = new Color(110, 170, 255);
    private static final Color J2 = new Color(255, 110, 110);
    private static final Color AMARELO = new Color(255, 206, 46);
    private static final Font PONTOS = new Font("Arial", Font.BOLD, 20);
    private static final Font PEQUENA = new Font("Arial", Font.BOLD, 10);
    private static final Font RELOGIO = new Font("Arial", Font.BOLD, 17);

    private PlacarLuta() {
    }

    /** "1:30" (segundos restantes; negativo = sem limite: "LIVRE"). */
    public static String relogio(int segundosRestantes) {
        if (segundosRestantes < 0) {
            return "LIVRE";
        }
        return segundosRestantes / 60 + ":" + String.format("%02d", segundosRestantes % 60);
    }

    public static void desenhar(Graphics2D g0, Placar p, int segundosRestantes, int larguraTela) {
        Graphics2D g = (Graphics2D) g0.create();
        int x = larguraTela / 2 - LARGURA / 2;
        g.setColor(FUNDO);
        g.fillRoundRect(x, Y, LARGURA, ALTURA, 10, 10);
        g.setColor(new Color(255, 255, 255, 60));
        g.setStroke(new BasicStroke(1));
        g.drawRoundRect(x, Y, LARGURA, ALTURA, 10, 10);
        lado(g, p, Lado.A, x + 8, true);
        lado(g, p, Lado.B, x + LARGURA - 8, false);
        g.setFont(RELOGIO);
        g.setColor(segundosRestantes >= 0 && segundosRestantes <= 10 ? new Color(255, 120, 90) : AMARELO);
        String t = relogio(segundosRestantes);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, larguraTela / 2 - fm.stringWidth(t) / 2, Y + 21);
        g.dispose();
    }

    private static void lado(Graphics2D g, Placar p, Lado l, int borda, boolean esquerda) {
        g.setFont(PEQUENA);
        String rot = l == Lado.A ? "J1" : "J2";
        String extra = "V" + p.vantagens(l) + " P" + p.punicoes(l);
        FontMetrics fp = g.getFontMetrics();
        g.setFont(PONTOS);
        String pts = String.valueOf(p.pontos(l));
        FontMetrics fg = g.getFontMetrics();
        int larguraBloco = fp.stringWidth(rot) + 6 + fg.stringWidth(pts) + 8 + fp.stringWidth(extra);
        int x = esquerda ? borda : borda - larguraBloco;
        g.setFont(PEQUENA);
        g.setColor(l == Lado.A ? J1 : J2);
        g.drawString(rot, x, Y + 19);
        x += fp.stringWidth(rot) + 6;
        g.setFont(PONTOS);
        g.setColor(Color.WHITE);
        g.drawString(pts, x, Y + 22);
        x += fg.stringWidth(pts) + 8;
        g.setFont(PEQUENA);
        g.setColor(new Color(200, 215, 240));
        g.drawString(extra, x, Y + 19);
    }
}
