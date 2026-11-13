package com.irmaosmiau.main;

import com.irmaosmiau.audio.AudioJogo;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.RepositorioConfiguracoes;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.preferencias.PreferenciasJogo;
import com.irmaosmiau.ui.ConfiguracoesPanel;
import com.irmaosmiau.ui.Dialogos;
import com.irmaosmiau.ui.PerfisPanel;
import com.irmaosmiau.game.GamePanel;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.treino.ConfiguracaoTreino;
import com.irmaosmiau.ui.MenuPanel;
import com.irmaosmiau.ui.SelecaoPersonagemPanel;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
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

            // Entrega 4C: preferencias salvas aplicadas antes da primeira tela.
            PreferenciasJogo preferencias = prepararPreferencias();

            mostrarMenu(janela);

            janela.setVisible(true);

            if (!preferencias.getAvisos().isEmpty()) {
                JOptionPane.showMessageDialog(
                        janela,
                        String.join(System.lineSeparator() + System.lineSeparator(),
                                preferencias.getAvisos()),
                        "Configurações",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });
    }

    /**
     * Carrega configuracoes.json da pasta do jogador (sem criar nada),
     * aplica no audio e instala como preferencias do jogo.
     */
    static PreferenciasJogo prepararPreferencias() {
        PreferenciasJogo preferencias = PreferenciasJogo.carregar(
                new RepositorioConfiguracoes(DiretorioDados.doUsuario()),
                AudioJogo.servico()
        );
        PreferenciasJogo.instalarGlobal(preferencias);
        return preferencias;
    }

    private static void mostrarMenu(JFrame janela) {

        MenuPanel menu = new MenuPanel(
                () -> mostrarSelecao(janela),
                () -> iniciarTreino(janela),
                () -> mostrarConfiguracoes(janela),
                () -> mostrarPerfis(janela)
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

        // Treino no cenario preferido (se ainda existir).
        String cenario = PreferenciasJogo.global()
                .cenarioValido(RegistroCenarios.padrao());

        GamePanel treino = new GamePanel(
                ConfiguracaoTreino.padrao().comCenario(cenario),
                () -> mostrarMenu(janela),
                () -> mostrarSelecao(janela),
                () -> iniciarTreino(janela)
        );

        trocarTela(janela, treino);

        treino.requestFocusInWindow();
    }

    /** Entrega 4C: tela de configuracoes (audio e cenario). */
    private static void mostrarConfiguracoes(JFrame janela) {

        trocarTela(janela, new ConfiguracoesPanel(
                PreferenciasJogo.global(),
                RegistroCenarios.padrao(),
                Dialogos.SWING,
                () -> mostrarMenu(janela)
        ));
    }

    /** Entrega 4C: criar/editar perfis personalizados (ainda nao usados nas lutas). */
    private static void mostrarPerfis(JFrame janela) {

        trocarTela(janela, new PerfisPanel(
                ServicoPerfis.padrao(),
                Dialogos.SWING,
                () -> mostrarMenu(janela)
        ));
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