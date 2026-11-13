package com.irmaosmiau.main;

import com.irmaosmiau.audio.AudioJogo;
import com.irmaosmiau.game.GamePanel;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.treino.ConfiguracaoTreino;
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
                () -> mostrarSelecao(janela),
                () -> iniciarTreino(janela)
        );

        trocarTela(janela, menu);

        // Musica do menu (arquivo ausente = silencio, sem erro).
        AudioJogo.servico().tocarMusica(AudioJogo.MUSICA_MENU);
    }

    /**
     * Entrega 4A: modo treino (Branco jogador x Preto parceiro passivo,
     * no dojo). Pausa -> MENU PRINCIPAL volta ao menu; TENTAR NOVAMENTE
     * recomeca o treino.
     */
    private static void iniciarTreino(JFrame janela) {

        GamePanel treino = new GamePanel(
                ConfiguracaoTreino.padrao(),
                () -> mostrarMenu(janela),
                () -> mostrarSelecao(janela),
                () -> iniciarTreino(janela)
        );

        trocarTela(janela, treino);

        treino.requestFocusInWindow();
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

    GamePanel jogo = new GamePanel(
            tipo,

            // MENU PRINCIPAL
            () -> mostrarMenu(
                    janela
            ),

            // SELECIONAR PERSONAGENS
            () -> mostrarSelecao(
                    janela
            ),

            // TENTAR NOVAMENTE
            () -> iniciarJogo(
                    janela,
                    tipo
            )
    );

    trocarTela(
            janela,
            jogo
    );

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