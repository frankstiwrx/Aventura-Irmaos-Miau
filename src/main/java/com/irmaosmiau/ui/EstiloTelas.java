package com.irmaosmiau.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Visual comum das telas de configuracoes e perfis (800 x 450): fundo
 * escuro em degrade, cartao central translucido, fontes e botoes padrao.
 */
final class EstiloTelas {

    static final int LARGURA = 800;
    static final int ALTURA = 450;

    static final Color TEXTO = new Color(240, 238, 230);
    static final Color TEXTO_SUAVE = new Color(190, 190, 185);
    static final Color ERRO = new Color(255, 140, 120);
    static final Color OK = new Color(140, 220, 140);
    static final Color DESTAQUE = new Color(176, 44, 40);

    static final Font TITULO = new Font("Arial", Font.BOLD, 22);
    static final Font ROTULO = new Font("Arial", Font.BOLD, 13);
    static final Font NORMAL = new Font("Arial", Font.PLAIN, 13);
    static final Font PEQUENA = new Font("Arial", Font.PLAIN, 11);

    private EstiloTelas() {
    }

    static void pintarFundo(Graphics g, int largura, int altura) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, new Color(40, 52, 64), 0, altura, new Color(22, 28, 36)));
        g2.fillRect(0, 0, largura, altura);
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillRoundRect(16, 12, largura - 32, altura - 24, 18, 18);
        g2.setColor(DESTAQUE);
        g2.fillRect(16, 12, 6, altura - 24);
        g2.dispose();
    }

    static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(TITULO);
        l.setForeground(TEXTO);
        return l;
    }

    static JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(ROTULO);
        l.setForeground(TEXTO);
        return l;
    }

    static JLabel texto(String texto, Font fonte, Color cor) {
        JLabel l = new JLabel(texto);
        l.setFont(fonte);
        l.setForeground(cor);
        return l;
    }

    static JButton botao(String texto) {
        JButton b = new JButton(texto);
        b.setFont(ROTULO);
        b.setFocusPainted(false);
        return b;
    }

    static void transparente(JComponent c) {
        c.setOpaque(false);
        c.setBorder(BorderFactory.createEmptyBorder());
    }
}
