package com.irmaosmiau.main;

import com.irmaosmiau.audio.AudioJogo;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.RepositorioConfiguracoes;
import com.irmaosmiau.persistencia.RepositorioControles;
import com.irmaosmiau.persistencia.RepositorioDispositivos;
import com.irmaosmiau.persistencia.RepositorioGamepad;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;
import com.irmaosmiau.preferencias.PreferenciasJogo;
import com.irmaosmiau.selecao.OpcoesDeLutadores;
import com.irmaosmiau.ui.ConfiguracoesPanel;
import com.irmaosmiau.ui.ControlesPanel;
import com.irmaosmiau.ui.Dialogos;
import com.irmaosmiau.ui.PerfisPanel;
import com.irmaosmiau.ui.SelecaoLutadoresPanel;
import com.irmaosmiau.game.GamePanel;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.treino.ConfiguracaoTreino;
import com.irmaosmiau.ui.MenuPanel;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        /*
         * Entrega 5D: "java -jar <jogo>.jar --diagnostico-gamepad [segundos]"
         * testa a leitura de gamepad pelo terminal, sem abrir o jogo. Pelo
         * -jar o manifesto libera o acesso nativo (sem avisos do JDK).
         */
        if (args.length > 0 && args[0].equals("--diagnostico-gamepad")) {
            com.irmaosmiau.dispositivos.DiagnosticoGamepad.main(
                    java.util.Arrays.copyOfRange(args, 1, args.length));
            return;
        }

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
            // Entrega 5C: controles salvos (controles.json) antes da primeira partida.
            PreferenciasControles controles = prepararControles();
            // Entrega 5D: teclado ou gamepad em cada slot (dispositivos.json).
            PreferenciasDispositivos dispositivos = prepararDispositivos();
            // Entrega 5F: esquema e botoes do gamepad (gamepad.json).
            PreferenciasGamepad gamepad = prepararGamepad();

            mostrarMenu(janela);

            janela.setVisible(true);

            /*
             * Entrega 5D: leitura de gamepad (GLFW, thread propria) so depois
             * que a janela Swing existe. Qualquer falha = jogo so com teclado.
             * Ao fechar a janela (EXIT_ON_CLOSE) a leitura e encerrada.
             */
            ServicoGamepads.iniciarFisico();
            Runtime.getRuntime().addShutdownHook(new Thread(ServicoGamepads::encerrar, "encerrar-gamepad"));

            // Entrega 5I: menus navegaveis por gamepad e teclado (sem mouse).
            new com.irmaosmiau.ui.NavegadorMenus(janela, ServicoGamepads::global, System::currentTimeMillis)
                    .instalar();

            java.util.List<String> avisos = new java.util.ArrayList<>(preferencias.getAvisos());
            avisos.addAll(controles.getAvisos());
            avisos.addAll(dispositivos.getAvisos());
            avisos.addAll(gamepad.getAvisos());
            if (!avisos.isEmpty()) {
                JOptionPane.showMessageDialog(
                        janela,
                        String.join(System.lineSeparator() + System.lineSeparator(), avisos),
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

    /** Carrega controles.json (sem criar nada) e instala como controles do jogo. */
    static PreferenciasControles prepararControles() {
        PreferenciasControles controles = PreferenciasControles.carregar(
                new RepositorioControles(DiretorioDados.doUsuario()));
        PreferenciasControles.instalarGlobal(controles);
        return controles;
    }

    /** Carrega dispositivos.json (sem criar nada) e instala como escolha do jogo. */
    static PreferenciasDispositivos prepararDispositivos() {
        PreferenciasDispositivos dispositivos = PreferenciasDispositivos.carregar(
                new RepositorioDispositivos(DiretorioDados.doUsuario()));
        PreferenciasDispositivos.instalarGlobal(dispositivos);
        return dispositivos;
    }

    /** Carrega gamepad.json (sem criar nada) e instala como mapeamento do jogo. */
    static PreferenciasGamepad prepararGamepad() {
        PreferenciasGamepad gamepad = PreferenciasGamepad.carregar(
                new RepositorioGamepad(DiretorioDados.doUsuario()));
        PreferenciasGamepad.instalarGlobal(gamepad);
        return gamepad;
    }

    private static void mostrarMenu(JFrame janela) {

        MenuPanel menu = new MenuPanel(
                () -> mostrarSelecao(janela),
                () -> abrirTreino(janela),
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
    /**
     * Entrega 5I: o TREINO sempre mostra a escolha (qual irmao controlar e
     * quem e o parceiro passivo). Antes (4D) abria direto com o Branco quando
     * nao havia perfis salvos.
     */
    private static void abrirTreino(JFrame janela) {

        mostrarSelecaoTreino(janela, OpcoesDeLutadores.padrao());
    }

    private static void mostrarSelecaoTreino(JFrame janela, OpcoesDeLutadores opcoes) {

        trocarTela(janela, new SelecaoLutadoresPanel(
                opcoes,
                true,
                (tipo, jogador, parceiro) -> iniciarTreino(janela, jogador, parceiro),
                () -> mostrarMenu(janela)
        ));
    }

    /**
     * @param jogador  escolha do slot Branco (null = Branco oficial)
     * @param parceiro escolha do slot Preto (null = Preto oficial)
     */
    private static void iniciarTreino(JFrame janela, EscolhaLutador jogador, EscolhaLutador parceiro) {

        // Treino no cenario preferido (se ainda existir).
        String cenario = PreferenciasJogo.global()
                .cenarioValido(RegistroCenarios.padrao());

        ConfiguracaoTreino configuracao = ConfiguracaoTreino.padrao().comCenario(cenario);
        if (jogador != null) {
            configuracao = configuracao.comJogador(jogador);
        }
        if (parceiro != null) {
            configuracao = configuracao.comParceiro(parceiro);
        }

        GamePanel treino = new GamePanel(
                configuracao,
                () -> mostrarMenu(janela),
                () -> mostrarSelecaoTreino(janela, OpcoesDeLutadores.padrao()),
                () -> iniciarTreino(janela, jogador, parceiro)
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
                () -> mostrarMenu(janela),
                () -> mostrarControles(janela)
        ));
    }

    /** Entrega 5C: remapeamento das teclas dos dois jogadores. */
    private static void mostrarControles(JFrame janela) {

        trocarTela(janela, new ControlesPanel(
                PreferenciasControles.global(),
                Dialogos.SWING,
                () -> mostrarConfiguracoes(janela)
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

        // Entrega 4D: oficiais + perfis personalizados (lidos a cada abertura).
        SelecaoLutadoresPanel selecao
                = new SelecaoLutadoresPanel(
                        OpcoesDeLutadores.padrao(),
                        false,
                        (tipo, branco, preto) -> iniciarJogo(janela, tipo, branco, preto),
                        () -> mostrarMenu(janela)
                );

        trocarTela(janela, selecao);
    }

private static void iniciarJogo(
        JFrame janela,
        TipoPersonagem tipo,
        EscolhaLutador slotBranco,
        EscolhaLutador slotPreto
) {

    GamePanel jogo = new GamePanel(
            tipo,
            slotBranco,
            slotPreto,

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
                    tipo,
                    slotBranco,
                    slotPreto
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