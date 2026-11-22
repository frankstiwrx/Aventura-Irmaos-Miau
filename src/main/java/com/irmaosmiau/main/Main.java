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

            // Entrega 5M: o jogo abre na tela de titulo.
            mostrarAbertura(janela);

            janela.setVisible(true);

            /*
             * Entrega 5D: leitura de gamepad (GLFW, thread propria) so depois
             * que a janela Swing existe. Qualquer falha = jogo so com teclado.
             * Ao fechar a janela (EXIT_ON_CLOSE) a leitura e encerrada.
             */
            ServicoGamepads.iniciarFisico();
            Runtime.getRuntime().addShutdownHook(new Thread(ServicoGamepads::encerrar, "encerrar-gamepad"));

            // Entrega 5L: o jogo comeca com as animacoes de pegada propostas (F6 volta as autorais).
            com.irmaosmiau.combate.ModoAnimacoesPegada.definir(
                    com.irmaosmiau.combate.ModoAnimacoesPegada.padraoDoJogo());
            // Entrega 5O: rolamento e sprawl na versao nova (F7 volta ao original para comparar).
            com.irmaosmiau.animacao.VersaoMovimentos.definir(
                    com.irmaosmiau.animacao.VersaoMovimentos.padraoDoJogo());

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

    /** Entrega 5M: tela de titulo (ENTER / A / START vai para o menu). */
    private static void mostrarAbertura(JFrame janela) {
        trocarTela(janela, new com.irmaosmiau.ui.AberturaPanel(() -> mostrarMenu(janela), ServicoGamepads::global));
        AudioJogo.trilhas().tocar(com.irmaosmiau.audio.ContextoMusical.MENU_INICIAL);       // 5Q.6
    }

    /** Entrega 5M: SAIR pergunta e fecha a janela pelo caminho normal (EXIT_ON_CLOSE + gancho do gamepad). */
    private static void sair(JFrame janela) {
        int r = JOptionPane.showConfirmDialog(janela, "Sair do jogo?", "Sair", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            janela.dispatchEvent(new java.awt.event.WindowEvent(janela, java.awt.event.WindowEvent.WINDOW_CLOSING));
        }
    }

    private static void mostrarMenu(JFrame janela) {

        // Entrega 5M: menu reorganizado (JOGAR, TREINO, PERSONAGENS, CONFIGURACOES, SAIR).
        JPanel menu = new com.irmaosmiau.ui.MenuPrincipalPanel(
                () -> mostrarSelecao(janela),
                () -> abrirTreino(janela),
                () -> mostrarPerfis(janela),
                () -> mostrarConfiguracoes(janela),
                () -> sair(janela)
        );

        trocarTela(janela, menu);

        // Musica do menu (arquivo ausente = silencio, sem erro). 5Q.6: mapeamento proprio por tela
        AudioJogo.trilhas().tocar(com.irmaosmiau.audio.ContextoMusical.MENU_PRINCIPAL);
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

    /**
     * O TREINO usa a mesma selecao do JOGAR (modo TREINO): um dispositivo escolhe
     * o lutador e o parceiro; a segunda tela tem a arena e o inicio (lembrado na
     * sessao, como na selecao antiga).
     */
    private static void mostrarSelecaoTreino(JFrame janela, OpcoesDeLutadores opcoes) {
        AudioJogo.trilhas().tocar(com.irmaosmiau.audio.ContextoMusical.MENU_TREINAMENTO);   // 5Q.6

        trocarTela(janela, com.irmaosmiau.ui.SelecaoPartidaPanel.treino(
                CatalogoPersonagens.padrao(),
                opcoes,
                RegistroCenarios.padrao(),
                PreferenciasJogo.global().cenarioValido(RegistroCenarios.padrao()),
                SelecaoLutadoresPanel.inicioDaSessao(),
                (jogador, parceiro, cenario, inicio) -> {
                    SelecaoLutadoresPanel.lembrarInicioDaSessao(inicio);
                    iniciarTreino(janela, jogador, parceiro, inicio, cenario);
                },
                () -> mostrarMenu(janela),
                ServicoGamepads::global,
                () -> PreferenciasDispositivos.global().getAtual()
        ));
    }

    /**
     * @param jogador  escolha do slot Branco (null = Branco oficial)
     * @param parceiro escolha do slot Preto (null = Preto oficial)
     */
    private static void iniciarTreino(JFrame janela, EscolhaLutador jogador, EscolhaLutador parceiro,
            com.irmaosmiau.entities.InicioLuta inicio, String cenarioEscolhido) {

        // 5M.3: o mapa escolhido na selecao; se sumir, o cenario preferido.
        String cenario = cenarioEscolhido != null && RegistroCenarios.padrao().contem(cenarioEscolhido)
                ? cenarioEscolhido
                : PreferenciasJogo.global().cenarioValido(RegistroCenarios.padrao());

        ConfiguracaoTreino configuracao = ConfiguracaoTreino.padrao().comCenario(cenario);
        if (jogador != null) {
            configuracao = configuracao.comJogador(jogador);
        }
        if (parceiro != null) {
            configuracao = configuracao.comParceiro(parceiro);
        }
        // Entrega 5J: de joelhos ou em pe (F5 e TENTAR NOVAMENTE mantem).
        configuracao = configuracao.comInicio(inicio);

        GamePanel treino = new GamePanel(
                configuracao,
                () -> mostrarMenu(janela),
                () -> mostrarSelecaoTreino(janela, OpcoesDeLutadores.padrao()),
                () -> iniciarTreino(janela, jogador, parceiro, inicio, cenario)
        );

        trocarTela(janela, treino);

        treino.requestFocusInWindow();
    }

    /** Entrega 4C: tela de configuracoes (audio e cenario). */
    private static void mostrarConfiguracoes(JFrame janela) {
        AudioJogo.trilhas().tocar(com.irmaosmiau.audio.ContextoMusical.CONFIGURACOES);   // 5Q.6

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
        AudioJogo.trilhas().tocar(com.irmaosmiau.audio.ContextoMusical.CONFIGURACOES);   // 5Q.6

        trocarTela(janela, new ControlesPanel(
                PreferenciasControles.global(),
                Dialogos.SWING,
                () -> mostrarConfiguracoes(janela)
        ));
    }

    /** Entrega 4C: criar/editar perfis personalizados (ainda nao usados nas lutas). */
    private static void mostrarPerfis(JFrame janela) {
        AudioJogo.trilhas().tocar(com.irmaosmiau.audio.ContextoMusical.CRIACAO_PERSONAGENS);   // 5Q.6

        trocarTela(janela, new PerfisPanel(
                ServicoPerfis.padrao(),
                Dialogos.SWING,
                () -> mostrarMenu(janela)
        ));
    }

    /**
     * Entrega 5M: selecao em grade de JOGAR (2 jogadores, cursores
     * independentes, perfis personalizados, MAPA e TEMPO). A selecao antiga
     * (SelecaoLutadoresPanel) continua no TREINO.
     */
    private static void mostrarSelecao(JFrame janela) {
        AudioJogo.trilhas().tocar(com.irmaosmiau.audio.ContextoMusical.SELECAO_PERSONAGENS);   // 5Q.6

        trocarTela(janela, new com.irmaosmiau.ui.SelecaoPartidaPanel(
                CatalogoPersonagens.padrao(),
                OpcoesDeLutadores.padrao(),
                RegistroCenarios.padrao(),
                PreferenciasJogo.global().cenarioValido(RegistroCenarios.padrao()),
                new com.irmaosmiau.ui.SelecaoPartidaPanel.AoIniciar() {
                    @Override
                    public void iniciar(EscolhaLutador j1, EscolhaLutador j2, String cenario, int segundos) {
                        iniciarPartida(janela, j1, j2, cenario, segundos, com.irmaosmiau.regras.ModoLuta.PADRAO,
                                null);
                    }

                    @Override
                    public void iniciar(EscolhaLutador j1, EscolhaLutador j2, String cenario, int segundos,
                            com.irmaosmiau.regras.ModoLuta modo) {
                        iniciarPartida(janela, j1, j2, cenario, segundos, modo, null);   // o modo das configuracoes
                    }
                },
                () -> mostrarMenu(janela),
                ServicoGamepads::global,
                () -> PreferenciasDispositivos.global().getAtual()
        ));
    }

    /**
     * 5Q.6: a partida normal; o Modo Desenvolvedor liga/desliga dentro dela (F8). Se ele estiver
     * ligado no F5, a luta recomeca com a mesma sessao (e no cenario do F4); o jogo sempre comeca
     * no modo normal.
     */
    private static void iniciarPartida(JFrame janela, EscolhaLutador j1, EscolhaLutador j2, String cenario,
            int segundos, com.irmaosmiau.regras.ModoLuta modo, com.irmaosmiau.game.ModoDesenvolvedor dev) {
        GamePanel[] atual = new GamePanel[1];
        GamePanel jogo = new GamePanel(
                TipoPersonagem.AMBOS,
                j1,
                j2,
                com.irmaosmiau.entities.InicioLuta.EM_PE,
                () -> mostrarMenu(janela),
                () -> mostrarSelecao(janela),
                () -> iniciarPartida(janela, j1, j2, cenario, segundos, modo, atual[0].getModoDesenvolvedor())
        );
        atual[0] = jogo;
        jogo.configurarPartida(dev != null ? dev.cenarioOu(cenario) : cenario, segundos);
        jogo.configurarModoLuta(modo);                         // regras escolhidas nas configuracoes
        jogo.configurarModoDesenvolvedor(dev);
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