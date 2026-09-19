package com.irmaosmiau.main;

import com.irmaosmiau.game.GamePanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame janela = new JFrame("Aventura dos Irmãos Miau");

            GamePanel gamePanel = new GamePanel();

            janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            janela.setResizable(false);

            janela.add(gamePanel);

            janela.pack();
            janela.setLocationRelativeTo(null);
            janela.setVisible(true);
        });
    }
}