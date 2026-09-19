package com.irmaosmiau.main;

import com.irmaosmiau.game.GamePanel;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.ui.MenuPanel;
import com.irmaosmiau.ui.SelecaoPersonagemPanel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame janela = new JFrame(
                    "Aventura dos Irmãos Miau"
            );

            janela.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE
            );

            janela.setResizable(false);

            mostrarMenu(janela);

            janela.setVisible(true);
        });
    }

    private static void mostrarMenu(JFrame janela) {

        MenuPanel menu = new MenuPanel(
                () -> mostrarSelecao(janela)
        );

        trocarTela(janela, menu);
    }

    private static void mostrarSelecao(JFrame janela) {

        SelecaoPersonagemPanel selecao
                = new SelecaoPersonagemPanel(
                        tipo -> iniciarJogo(
                                janela,
                                tipo
                        )
                );

        trocarTela(janela, selecao);
    }

    private static void iniciarJogo(
            JFrame janela,
            TipoPersonagem tipo
    ) {

        GamePanel jogo = new GamePanel(tipo);

        trocarTela(janela, jogo);

        jogo.requestFocusInWindow();
    }

    private static void trocarTela(
            JFrame janela,
            JPanel painel
    ) {

        janela.setContentPane(painel);

        janela.pack();

        janela.setLocationRelativeTo(null);

        janela.revalidate();
        janela.repaint();
    }
}