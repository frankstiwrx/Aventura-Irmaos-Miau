package com.irmaosmiau.game;

import com.irmaosmiau.hud.AjudaControles;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/**
 * Tela de ajuda de controles, aberta pela pausa (Entrega 5C).
 *
 * Desenhada por cima do jogo pausado, com as teclas CONFIGURADAS:
 * uma coluna por jogador em campo, a linha "AGORA" com as acoes de combate
 * uteis no estado atual e os atalhos fixos do sistema. Nao mexe no HUD.
 */
final class PainelAjudaControles {

    record Coluna(String titulo, List<AjudaControles.Linha> linhas) {
    }

    record Conteudo(List<Coluna> colunas, String agora, String observacao) {
    }

    static final int X = 30;
    static final int Y = 14;
    static final int LARGURA_CAIXA = 740;
    static final int ALTURA_CAIXA = 420;
    static final int ALTURA_LINHA = 14;

    private static final Font TITULO = new Font("Arial", Font.BOLD, 20);
    private static final Font GRUPO = new Font("Arial", Font.BOLD, 12);
    private static final Font NORMAL = new Font("Arial", Font.PLAIN, 12);
    private static final Font TECLA = new Font("Arial", Font.BOLD, 12);
    private static final Color FUNDO = new Color(22, 28, 36, 250);
    private static final Color TEXTO = new Color(240, 238, 230);
    private static final Color SUAVE = new Color(190, 190, 185);
    private static final Color DESTAQUE = new Color(255, 214, 120);

    private PainelAjudaControles() {
    }

    static void desenhar(Graphics2D g0, Conteudo c, int largura, int altura) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(FUNDO);
        g.fillRoundRect(X, Y, LARGURA_CAIXA, ALTURA_CAIXA, 14, 14);

        g.setColor(TEXTO);
        g.setFont(TITULO);
        g.drawString("CONTROLES", X + 20, Y + 28);
        g.setFont(NORMAL);
        g.setColor(SUAVE);
        g.drawString("Teclas configuradas em CONFIGURAÇÕES → CONTROLES. ESC ou VOLTAR fecha.", X + 160, Y + 26);

        int y = Y + 48;
        if (c.agora() != null && !c.agora().isBlank()) {
            g.setFont(TECLA);
            g.setColor(DESTAQUE);
            g.drawString("AGORA  " + c.agora(), X + 20, y);
            y += ALTURA_LINHA + 4;
        }

        int larguraColuna = (LARGURA_CAIXA - 40) / Math.max(1, c.colunas().size());
        larguraColuna = Math.min(larguraColuna, 350);
        int topo = y + 4;
        for (int i = 0; i < c.colunas().size(); i++) {
            desenharColuna(g, c.colunas().get(i), X + 20 + i * (larguraColuna + 10), topo, larguraColuna - 10);
        }

        g.setFont(NORMAL);
        g.setColor(SUAVE);
        int base = Y + ALTURA_CAIXA - 40;
        if (c.observacao() != null && !c.observacao().isBlank()) {
            // Uma coluna so (individual/treino): a metade direita esta livre.
            g.drawString(c.observacao(), X + 400, topo + 10);
        }
        g.drawString("SISTEMA (fixas): " + AjudaControles.SISTEMA, X + 20, base);
        g.dispose();
    }

    private static void desenharColuna(Graphics2D g, Coluna coluna, int x, int y, int largura) {
        g.setFont(TITULO.deriveFont(14f));
        g.setColor(TEXTO);
        g.drawString(coluna.titulo(), x, y + 10);
        y += ALTURA_LINHA + 6;
        for (AjudaControles.Linha l : coluna.linhas()) {
            if (l.isTitulo()) {
                g.setFont(GRUPO);
                g.setColor(DESTAQUE);
                g.drawString(l.texto(), x, y + 10);
            } else {
                g.setFont(NORMAL);
                g.setColor(TEXTO);
                g.drawString(l.texto(), x + 8, y + 10);
                g.setFont(TECLA);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(l.tecla(), x + largura - fm.stringWidth(l.tecla()), y + 10);
            }
            y += ALTURA_LINHA;
        }
    }
}
