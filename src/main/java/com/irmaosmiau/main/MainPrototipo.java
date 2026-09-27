package com.irmaosmiau.main;

import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.game.GamePanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Main local para prototipagem.
 *
 * Abre o jogo diretamente no modo com os dois Irmãos Miau,
 * pulando menu principal e seleção de personagem.
 *
 * Este arquivo deve ficar apenas no ambiente local e ser
 * ignorado pelo Git através de .git/info/exclude.
 */
public class MainPrototipo {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame janela = new JFrame(
                    "Aventura dos Irmãos Miau - PROTÓTIPO"
            );

            janela.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE
            );

            janela.setResizable(false);

            iniciarPrototipo(janela);

            janela.setVisible(true);
        });
    }

    private static void iniciarPrototipo(
            JFrame janela
    ) {

        GamePanel jogo = new GamePanel(
                TipoPersonagem.AMBOS,

                /*
                 * Como esta Main não possui menu nem tela de seleção,
                 * os três callbacks simplesmente reiniciam o protótipo.
                 *
                 * Assim, mesmo que um botão do menu de pausa/fim seja
                 * usado, continuamos dentro do ambiente de testes.
                 */
                () -> iniciarPrototipo(janela),
                () -> iniciarPrototipo(janela),
                () -> iniciarPrototipo(janela)
        );

        janela.setContentPane(jogo);

        janela.pack();

        janela.setLocationRelativeTo(null);

        janela.revalidate();
        janela.repaint();

        jogo.requestFocusInWindow();
    }
}
