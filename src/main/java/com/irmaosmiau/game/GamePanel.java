package com.irmaosmiau.game;

import com.irmaosmiau.entities.IrmaoMiau;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

public class GamePanel extends JPanel {

    public static final int LARGURA = 800;
    public static final int ALTURA = 450;

    private final IrmaoMiau irmaoMiau;

    public GamePanel() {

        setPreferredSize(
                new Dimension(LARGURA, ALTURA)
        );

        setBackground(
                new Color(135, 206, 235)
        );

        irmaoMiau = new IrmaoMiau(
                150,
                170,
                0.7,
                true
        );
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // Céu já é o background do painel

        // Chão
        g2.setColor(
                new Color(80, 180, 80)
        );

        g2.fillRect(
                0,
                350,
                LARGURA,
                100
        );

        // Título
        g2.setColor(Color.BLACK);

        g2.drawString(
                "AVENTURA DOS IRMÃOS MIAU",
                20,
                30
        );

        // Desenha o personagem
        irmaoMiau.desenhar(g2);
    }
}