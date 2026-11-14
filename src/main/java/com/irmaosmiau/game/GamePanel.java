package com.irmaosmiau.game;

import com.irmaosmiau.combate.ContextoCombate;
import com.irmaosmiau.combate.EstadoInteracaoCombate;
import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.audio.AudioJogo;
import com.irmaosmiau.audio.ServicoAudio;
import com.irmaosmiau.cenario.Cenario;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.AcoesContextuais;
import com.irmaosmiau.controles.DetectorToques;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.PapelCombate;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.CamadaComando;
import com.irmaosmiau.controles.CelulaContextual;
import com.irmaosmiau.controles.PosicaoComando;
import com.irmaosmiau.controles.RepertorioContextual;
import com.irmaosmiau.dispositivos.ComandoGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.preferencias.PreferenciasGamepad;
import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.conteudo.ConteudoCombate;
import com.irmaosmiau.conteudo.ConteudoInvalidoException;
import com.irmaosmiau.entities.Direcao;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.hud.AjudaControles;
import com.irmaosmiau.hud.DadosLutadorHud;
import com.irmaosmiau.hud.FeedbackCombate;
import com.irmaosmiau.hud.HudCombate;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.SessaoDispositivos;
import com.irmaosmiau.input.InputHandler;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasJogo;
import com.irmaosmiau.treino.ConfiguracaoTreino;
import com.irmaosmiau.treino.SessaoTreino;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Painel principal da gameplay.
 *
 * Responsabilidades desta classe:
 * - criar os personagens conforme o modo escolhido;
 * - ler o estado do InputHandler;
 * - atualizar movimento e ações dos Miaus;
 * - controlar pausa e fim de jogo;
 * - desenhar cenário, personagens e HUD.
 *
 * MAPA RÁPIDO DO ARQUIVO
 * 1. Configuração geral
 * 2. Personagens e modo de jogo
 * 3. Estado de entrada / ações por clique
 * 4. Pausa, fim de jogo e orientação
 * 5. Construtor
 * 6. Game loop
 * 7. Atualização dos personagens
 * 8. Movimento e direção
 * 9. Pausa e fim de jogo
 * 10. Renderização
 * 11. HUD
 */
public class GamePanel extends JPanel {

    // =====================================================
    // 1. CONFIGURAÇÃO GERAL
    // =====================================================
    public static final int LARGURA = 800;
    public static final int ALTURA = 450;

    private static final int INTERVALO_GAME_LOOP_MS = 16;

    private static final double ESCALA_PERSONAGEM = 0.7;

    private static final int Y_INICIAL_PERSONAGEM = 150;
    private static final int X_JOGADOR_INDIVIDUAL = 350;
    private static final int X_BRANCO_DUO = 220;
    private static final int X_PRETO_DUO = 500;

    /*
     * Entrega 4A: o debug tecnico do motor e uma camada separada do HUD
     * do jogador. Comeca escondido e alterna com F3 (MainPrototipo liga
     * por padrao).
     */
    private boolean debugVisivel = false;

    // =====================================================
    // CENARIO E HUD (Entrega 4A)
    // =====================================================
    private final RegistroCenarios cenarios = RegistroCenarios.padrao();

    /*
     * Entrega 4C: preferencias centralizadas (audio, cenario). F4/F9/F10
     * passam por aqui para que tela, audio e arquivo salvo nao divirjam.
     */
    private final PreferenciasJogo preferencias = PreferenciasJogo.global();

    private Cenario cenario = cenarios.exigir(preferencias.cenarioValido(cenarios));

    private final HudCombate hud = new HudCombate();
    private final FeedbackCombate feedback = new FeedbackCombate();

    /* Relogio do jogo em ticks (60 por segundo), usado pelo feedback. */
    private long tick;

    private final InputHandler input;
    private final Timer gameLoop;

    private final Runnable aoVoltarMenu;
    private final Runnable aoTrocarPersonagem;
    private final Runnable aoTentarNovamente;

    // =====================================================
    // 2. PERSONAGENS E MODO DE JOGO
    // =====================================================
    private final boolean modoDoisMiaus;

    // Usado quando apenas um personagem foi selecionado.
    private final IrmaoMiau jogadorPrincipal;

    // Usados no modo "OS DOIS MIAUS" e no treino (Branco = jogador).
    // Nao sao final: o treino recria os lutadores ao reiniciar (F5).
    private IrmaoMiau jogadorBranco;
    private IrmaoMiau jogadorPreto;

    // Modo treino (Entrega 4A): parceiro controlado por comportamento.
    private final boolean modoTreino;
    private final SessaoTreino sessaoTreino;

    private final ServicoAudio audio = AudioJogo.servico();

    /*
     * O motor existe apenas quando ha dois lutadores na arena.
     * No modo individual ele fica nulo por enquanto.
     */
    private SistemaCombate sistemaCombate;

    // =====================================================
    // 3. ESTADO DE ENTRADA / AÇÕES DE UM ÚNICO DISPARO
    // =====================================================
    /*
     * Pulo, rolamento, sprawl e deitar são ações disparadas
     * apenas na transição solta -> pressionada. Esses campos
     * guardam o estado da tecla no frame anterior.
     */

    // Jogador individual / controles do Branco.
    private boolean puloAnterior;
    private boolean puloCurtoAnterior;
    private boolean rolamentoAnterior;
    private boolean sprawlAnterior;
    private boolean deitarAnterior;

    // Branco no modo de dois jogadores.
    private boolean puloBrancoAnterior;
    private boolean puloCurtoBrancoAnterior;
    private boolean rolamentoBrancoAnterior;
    private boolean sprawlBrancoAnterior;
    private boolean deitarBrancoAnterior;
    /*
     * Entrega 5C: borda de subida das acoes de COMBATE (pegada, T, Y, U)
     * por slot e acao logica, em vez de um campo por tecla.
     */
    private final DetectorToques toques = new DetectorToques();
    // Entrega 5B: deslocamento conjunto em andamento.
    private boolean deslocandoConjunto;

    /*
     * Entrega 5B: limites horizontais da arena para o deslocamento conjunto
     * (x do canto do desenho; o corpo vai de x-15 a x+83 na escala 0,7).
     * A caminhada normal continua sem limite, como antes.
     */
    static final int LIMITE_ESQUERDO_ARENA = 15;
    static final int LIMITE_DIREITO_ARENA = LARGURA - 85;

    // Preto no modo de dois jogadores.
    private boolean puloPretoAnterior;
    private boolean puloCurtoPretoAnterior;
    private boolean rolamentoPretoAnterior;
    private boolean sprawlPretoAnterior;
    private boolean deitarPretoAnterior;

    // =====================================================
    // 4. ORIENTAÇÃO, PAUSA E FIM DE JOGO
    // =====================================================
    private boolean brancoViradoDireita = true;
    private boolean pretoViradoDireita = false;

    private boolean pausado;
    private JPanel menuPausa;
    // Entrega 5C: tela de ajuda de controles (pausa -> CONTROLES).
    private boolean ajudaVisivel;
    private JButton voltarDaAjuda;
    private final transient java.awt.event.WindowFocusListener aoPerderFoco =
            new java.awt.event.WindowAdapter() {
                @Override
                public void windowLostFocus(java.awt.event.WindowEvent e) {
                    liberarTeclas();
                }

                @Override
                public void windowGainedFocus(java.awt.event.WindowEvent e) {
                    if (dispositivos != null) {
                        dispositivos.setFocado(true);
                    }
                }
            };
    // Entrega 5D: gamepads ligados aos slots desta partida.
    private SessaoDispositivos dispositivos;
    // Entrega 5F: esquema e botoes salvos (gamepad.json) ou o Classico da 5D.
    private final MapaGamepad mapaGamepad = PreferenciasGamepad.global().getAtual();

    private boolean fimDeJogo;
    private String mensagemFim = "";
    private JPanel menuFim;



    // =====================================================
    // 5. CONSTRUTOR
    // =====================================================
    public GamePanel(
            TipoPersonagem tipo,
            Runnable aoVoltarMenu,
            Runnable aoTrocarPersonagem,
            Runnable aoTentarNovamente
    ) {
        this(tipo, null, null, null, aoVoltarMenu, aoTrocarPersonagem, aoTentarNovamente);
    }

    /**
     * Entrega 4D: partida com lutadores escolhidos (oficiais ou perfis).
     * slotBranco/slotPreto nulos = personagem oficial daquele slot.
     * O slot Branco aceita apenas Branco/perfis do Branco (controles WASD/H);
     * o slot Preto, apenas Preto/perfis do Preto (setas).
     */
    public GamePanel(
            TipoPersonagem tipo,
            EscolhaLutador slotBranco,
            EscolhaLutador slotPreto,
            Runnable aoVoltarMenu,
            Runnable aoTrocarPersonagem,
            Runnable aoTentarNovamente
    ) {
        this(tipo, null, slotBranco, slotPreto, aoVoltarMenu, aoTrocarPersonagem, aoTentarNovamente);
    }

    /**
     * Entrega 4A: modo treino (jogador nos controles WASD/H, parceiro
     * controlado pelo comportamento da configuracao).
     */
    public GamePanel(
            ConfiguracaoTreino treino,
            Runnable aoVoltarMenu,
            Runnable aoTrocarPersonagem,
            Runnable aoTentarNovamente
    ) {
        this(TipoPersonagem.AMBOS, exigirTreino(treino), null, null,
                aoVoltarMenu, aoTrocarPersonagem, aoTentarNovamente);
    }

    /**
     * Instancia o lutador escolhido para um slot, conferindo que a base
     * combina com os controles daquele slot.
     */
    private static IrmaoMiau criarDoSlot(EscolhaLutador escolha, String idBaseDoSlot, int x) {
        if (!idBaseDoSlot.equals(escolha.getIdBase())) {
            throw new IllegalArgumentException("O slot " + idBaseDoSlot + " so aceita o personagem oficial"
                    + " ou perfis baseados nele; recebeu " + escolha.getDescricao());
        }
        return escolha.criarLutador(x, Y_INICIAL_PERSONAGEM, ESCALA_PERSONAGEM);
    }

    private static ConfiguracaoTreino exigirTreino(ConfiguracaoTreino treino) {
        if (treino == null) {
            throw new IllegalArgumentException("A configuracao de treino e obrigatoria.");
        }
        return treino;
    }

    private GamePanel(
            TipoPersonagem tipo,
            ConfiguracaoTreino treino,
            EscolhaLutador slotBranco,
            EscolhaLutador slotPreto,
            Runnable aoVoltarMenu,
            Runnable aoTrocarPersonagem,
            Runnable aoTentarNovamente
    ) {

        this.aoVoltarMenu = aoVoltarMenu;
        this.aoTrocarPersonagem = aoTrocarPersonagem;
        this.aoTentarNovamente = aoTentarNovamente;

        setPreferredSize(
                new Dimension(
                        LARGURA,
                        ALTURA
                )
        );

        setBackground(
                new Color(
                        135,
                        206,
                        235
                )
        );

        setFocusable(true);

        setLayout(null);

        // =========================
        // PERSONAGENS
        // =========================
        modoTreino = treino != null;

        modoDoisMiaus
                = tipo == TipoPersonagem.AMBOS;

        if (modoTreino) {

            jogadorPrincipal = null;

            sessaoTreino = new SessaoTreino(
                    treino,
                    CatalogoPersonagens.padrao(),
                    carregarConteudoCombate()
            );

            jogadorBranco = sessaoTreino.getJogador();
            jogadorPreto = sessaoTreino.getParceiro();

        } else if (modoDoisMiaus) {

            sessaoTreino = null;

            jogadorPrincipal = null;

            jogadorBranco = slotBranco == null
                    ? new IrmaoMiauBranco(
                            X_BRANCO_DUO,
                            Y_INICIAL_PERSONAGEM,
                            ESCALA_PERSONAGEM
                    )
                    : criarDoSlot(slotBranco, CatalogoPersonagens.ID_BRANCO, X_BRANCO_DUO);

            jogadorPreto = slotPreto == null
                    ? new IrmaoMiauPreto(
                            X_PRETO_DUO,
                            Y_INICIAL_PERSONAGEM,
                            ESCALA_PERSONAGEM
                    )
                    : criarDoSlot(slotPreto, CatalogoPersonagens.ID_PRETO, X_PRETO_DUO);

        } else {

            sessaoTreino = null;

            jogadorBranco = null;
            jogadorPreto = null;

            if (tipo == TipoPersonagem.BRANCO) {

                jogadorPrincipal = slotBranco == null
                        ? new IrmaoMiauBranco(
                                X_JOGADOR_INDIVIDUAL,
                                Y_INICIAL_PERSONAGEM,
                                ESCALA_PERSONAGEM
                        )
                        : criarDoSlot(slotBranco, CatalogoPersonagens.ID_BRANCO, X_JOGADOR_INDIVIDUAL);

            } else {

                jogadorPrincipal = slotPreto == null
                        ? new IrmaoMiauPreto(
                                X_JOGADOR_INDIVIDUAL,
                                Y_INICIAL_PERSONAGEM,
                                ESCALA_PERSONAGEM
                        )
                        : criarDoSlot(slotPreto, CatalogoPersonagens.ID_PRETO, X_JOGADOR_INDIVIDUAL);
            }
        }

        // =========================
        // MOTOR DE COMBATE
        // =========================
        /*
         * Entrega 3A: tecnicas e animacoes vem dos catalogos JSON
         * (conteudo/tecnicas, conteudo/animacoes, conteudo/miaustudio).
         * Se algo obrigatorio faltar, o jogo PARA com erro claro: nao
         * existe troca silenciosa para os arrays legados.
         */
        if (modoTreino) {
            sistemaCombate = sessaoTreino.getSistema();
            cenario = cenarios.exigir(treino.getIdCenario());
        } else {
            sistemaCombate = modoDoisMiaus
                    ? carregarConteudoCombate().criarSistema(
                            jogadorBranco,
                            jogadorPreto
                    )
                    : null;
        }

        // =========================
        // INPUT
        // =========================
        // Entrega 5C: teclas configuradas (controles.json) ou os padroes.
        input
                = new InputHandler(this, PreferenciasControles.global().getAtual());

        // Entrega 5D: teclado e/ou gamepad por slot. So os slots em campo:
        // no treino e no modo individual, apenas o Jogador 1.
        dispositivos = new SessaoDispositivos(
                PreferenciasDispositivos.global().getAtual(),
                ServicoGamepads.global(),
                input,
                modoDoisMiaus && !modoTreino
                        ? List.of(SlotJogador.JOGADOR_1, SlotJogador.JOGADOR_2)
                        : List.of(SlotJogador.JOGADOR_1),
                mapaGamepad);

        // =========================
        // PAUSA
        // =========================
        criarMenuPausa();
        criarMenuFim();
        configurarPausa();
        configurarAtalhos();

        // =========================
        // GAME LOOP
        // =========================
        gameLoop
                = new Timer(
                        INTERVALO_GAME_LOOP_MS,
                        e -> atualizar()
                );

        gameLoop.start();

        // Musica da arena (arquivo ausente = silencio, sem erro).
        audio.tocarMusica(
                modoTreino
                ? AudioJogo.MUSICA_TREINO
                : AudioJogo.MUSICA_COMBATE
        );
    }

    /**
     * Carrega o conteudo de combate do classpath (NetBeans e JAR).
     * Em caso de falha mostra a mensagem completa (console e, se houver
     * tela, numa janela de erro) e propaga a excecao.
     */
    private static ConteudoCombate carregarConteudoCombate() {
        try {
            return CarregadorConteudoCombate.carregarParaJogo();
        } catch (ConteudoInvalidoException ex) {
            System.err.println(ex.getMessage());
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(
                        null,
                        ex.getMessage(),
                        "Aventura dos Irmãos Miau - erro de conteúdo",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            throw ex;
        }
    }

    /** Acesso para testes do pacote (o jogo nao usa). */
    SistemaCombate getSistemaCombate() {
        return sistemaCombate;
    }

    // =====================================================
    // 6. GAME LOOP
    // =====================================================
    private void atualizar() {

        if (fimDeJogo) {
            return;
        }

        // Entrega 5D: gamepads (conexao, desconexao, leitura e Start = pausa).
        dispositivos.atualizar();
        for (String aviso : dispositivos.consumirMensagens()) {
            feedback.mostrarInfo(aviso, tick);
        }
        if (dispositivos.consumirPausa()) {
            alternarPausa();
        }

        if (pausado) {
            // Entrega 5F: nada do D-pad contextual fica pendente durante a pausa.
            dispositivos.limparContextual();

            repaint();
            return;
        }

        if (modoDoisMiaus) {

            atualizarMiauBranco();

            if (modoTreino) {
                // Parceiro de treino: ignora as teclas do segundo jogador.
                sessaoTreino.atualizarParceiro();
            } else {
                atualizarMiauPreto();
            }

            /*
             * A orientacao precisa refletir a posicao DESTE frame antes de
             * o motor espacial fotografar quem esta de frente para quem.
             */
            atualizarOrientacaoDosMiaus();

            sistemaCombate.atualizar();
            if (feedback.observar(sistemaCombate, tick)) {
                audio.tocarEfeito(AudioJogo.EFEITO_ROMPIMENTO);
            }
            atualizarEntradasCombate();

        } else {

            atualizarJogadorPrincipal();
            // Sem motor de combate no modo individual: comandos contextuais nao tem efeito.
            dispositivos.consumirComandoContextual(SlotJogador.JOGADOR_1);
        }

        tick++;
        verificarFimDeJogo();
        repaint();
    }

    // =====================================================
    // 7. ATUALIZAÇÃO - JOGADOR INDIVIDUAL
    // =====================================================
    private void atualizarJogadorPrincipal() {

        boolean agachando
                = input.isAgachar();

        // =========================
// DEITAR / LEVANTAR
// =========================
        if (input.isDeitar()
                && !deitarAnterior) {

            jogadorPrincipal.alternarDeitado();
        }

        // =========================
        // RESPIRAÇÃO
        // =========================
        boolean respirando
                = input.isRespirar();

        jogadorPrincipal.setRespirando(
                respirando
        );

        // =========================
        // CORRIDA
        // =========================
        boolean correndo
                = input.isCorrer()
                && !agachando
                && !respirando;

        jogadorPrincipal.setCorrendo(
                correndo
        );

        // =========================
// ROLAMENTO
// =========================
        if (input.isRolar()
                && !rolamentoAnterior) {

            Direcao direcao
                    = obterDirecao(
                            input.isEsquerda(),
                            input.isDireita(),
                            input.isCima(),
                            input.isBaixo()
                    );

            jogadorPrincipal.iniciarRolamento(
                    direcao
            );
        }

        // =========================
// SPRAWL
// =========================
        if (input.isSprawl()
                && !sprawlAnterior) {

            jogadorPrincipal.iniciarSprawl();
        }

        // =========================
        // MOVIMENTO
        // =========================
        moverPersonagem(
                jogadorPrincipal,
                input.isEsquerda(),
                input.isDireita(),
                input.isCima(),
                input.isBaixo(),
                agachando,
                correndo,
                respirando
        );

        // =========================
        // PULO ALTO
        // =========================
        if (input.isPular()
                && !puloAnterior) {

            jogadorPrincipal.pular();
        }

        // =========================
        // PULO CURTO
        // =========================
        if (input.isPularCurto()
                && !puloCurtoAnterior) {

            jogadorPrincipal.pularCurto();
        }

        boolean andando
                = input.isEsquerda()
                || input.isDireita()
                || input.isCima()
                || input.isBaixo();

        jogadorPrincipal.atualizar(
                andando,
                agachando
        );

        puloAnterior
                = input.isPular();

        puloCurtoAnterior
                = input.isPularCurto();

        rolamentoAnterior
                = input.isRolar();

        deitarAnterior
                = input.isDeitar();

        sprawlAnterior
                = input.isSprawl();

    }

    // =====================================================
    // 7. ATUALIZAÇÃO - MIAU BRANCO
    // =====================================================
    private void atualizarMiauBranco() {

        if (jogadorBranco.isBloqueadoPorAnimacaoCombate()) {
            atualizarMiauBrancoBloqueadoPorCombate();
            return;
        }

        boolean agachando
                = input.isAgachar();

        // =========================
// DEITAR / LEVANTAR
// =========================
        if (input.isDeitar()
                && !deitarBrancoAnterior) {

            jogadorBranco.alternarDeitado();
        }

        // =========================
        // RESPIRAÇÃO
        // =========================
        boolean respirando
                = input.isRespirar();

        jogadorBranco.setRespirando(
                respirando
        );

        // =========================
        // CORRIDA
        // =========================
        boolean correndo
                = input.isCorrer()
                && !agachando
                && !respirando;

        jogadorBranco.setCorrendo(
                correndo
        );

        // =========================
// ROLAMENTO
// =========================
        if (input.isRolar()
                && !rolamentoBrancoAnterior) {

            Direcao direcao
                    = obterDirecao(
                            input.isEsquerda(),
                            input.isDireita(),
                            input.isCima(),
                            input.isBaixo()
                    );

            jogadorBranco.iniciarRolamento(
                    direcao
            );
        }

        // =========================
// SPRAWL
// =========================
        if (input.isSprawl()
                && !sprawlBrancoAnterior) {

            jogadorBranco.iniciarSprawl();
        }

        // =========================
        // MOVIMENTO
        // =========================
        /*
         * Entrega 5B: SHIFT + A/D com a pegada estabelecida, sendo o
         * aplicador = deslocamento conjunto lento (a dupla se move junta).
         * Sem SHIFT, A/D continua sendo caminhada normal (e rompe a pegada).
         */
        boolean horizontal = input.isEsquerda() ^ input.isDireita();
        boolean conjunto = horizontal
                && input.isControle()
                && sistemaCombate != null
                && sistemaCombate.isInteracaoEstabelecida()
                && sistemaCombate.getAplicadorInteracao() == jogadorBranco;

        if (conjunto) {
            com.irmaosmiau.combate.ResultadoAcaoTecnica r = sistemaCombate.deslocarPegadaControlada(
                    jogadorBranco,
                    input.isDireita() ? 1 : -1,
                    LIMITE_ESQUERDO_ARENA,
                    LIMITE_DIREITO_ARENA);
            if (!deslocandoConjunto) {
                feedback.mostrarInfo(r == com.irmaosmiau.combate.ResultadoAcaoTecnica.LIMITE_ARENA
                        ? "LIMITE DA ARENA" : "DESLOCAMENTO CONTROLADO", tick);
            }
        }
        deslocandoConjunto = conjunto;

        moverPersonagem(
                jogadorBranco,
                !conjunto && input.isEsquerda(),
                !conjunto && input.isDireita(),
                input.isCima(),
                input.isBaixo(),
                agachando,
                correndo,
                respirando
        );

        // =========================
        // PULO ALTO
        // =========================
        if (input.isPular()
                && !puloBrancoAnterior) {

            jogadorBranco.pular();
        }

        // =========================
        // PULO CURTO
        // =========================
        if (input.isPularCurto()
                && !puloCurtoBrancoAnterior) {

            jogadorBranco.pularCurto();
        }

        boolean andando
                = (!conjunto && (input.isEsquerda() || input.isDireita()))
                || input.isCima()
                || input.isBaixo();

        jogadorBranco.atualizar(
                andando,
                agachando
        );

        puloBrancoAnterior
                = input.isPular();

        puloCurtoBrancoAnterior
                = input.isPularCurto();
        rolamentoBrancoAnterior
                = input.isRolar();
        deitarBrancoAnterior
                = input.isDeitar();
        sprawlBrancoAnterior
                = input.isSprawl();

    }

    // =====================================================
    // 7. ATUALIZAÇÃO - MIAU PRETO
    // =====================================================
    private void atualizarMiauPreto() {

        if (jogadorPreto.isBloqueadoPorAnimacaoCombate()) {
            atualizarMiauPretoBloqueadoPorCombate();
            return;
        }

        boolean agachando
                = input.isPretoAgachar();

        // =========================
// DEITAR / LEVANTAR
// =========================
        if (input.isPretoDeitar()
                && !deitarPretoAnterior) {

            jogadorPreto.alternarDeitado();
        }
        // =========================
        // RESPIRAÇÃO
        // =========================
        boolean respirando
                = input.isPretoRespirar();

        jogadorPreto.setRespirando(
                respirando
        );

        // =========================
        // CORRIDA
        // =========================
        boolean correndo
                = input.isPretoCorrer()
                && !agachando
                && !respirando;

        jogadorPreto.setCorrendo(
                correndo
        );

        // =========================
// ROLAMENTO
// =========================
        if (input.isPretoRolar()
                && !rolamentoPretoAnterior) {

            Direcao direcao
                    = obterDirecao(
                            input.isPretoEsquerda(),
                            input.isPretoDireita(),
                            input.isPretoCima(),
                            input.isPretoBaixo()
                    );

            jogadorPreto.iniciarRolamento(
                    direcao
            );
        }

        // =========================
// SPRAWL
// =========================
        if (input.isPretoSprawl()
                && !sprawlPretoAnterior) {

            jogadorPreto.iniciarSprawl();
        }

        // =========================
        // MOVIMENTO
        // =========================
        moverPersonagem(
                jogadorPreto,
                input.isPretoEsquerda(),
                input.isPretoDireita(),
                input.isPretoCima(),
                input.isPretoBaixo(),
                agachando,
                correndo,
                respirando
        );

        // =========================
        // PULO ALTO
        // =========================
        if (input.isPretoPular()
                && !puloPretoAnterior) {

            jogadorPreto.pular();
        }

        // =========================
        // PULO CURTO
        // =========================
        if (input.isPretoPularCurto()
                && !puloCurtoPretoAnterior) {

            jogadorPreto.pularCurto();
        }

        boolean andando
                = input.isPretoEsquerda()
                || input.isPretoDireita()
                || input.isPretoCima()
                || input.isPretoBaixo();

        jogadorPreto.atualizar(
                andando,
                agachando
        );

        puloPretoAnterior
                = input.isPretoPular();

        puloCurtoPretoAnterior
                = input.isPretoPularCurto();

        rolamentoPretoAnterior
                = input.isPretoRolar();

        deitarPretoAnterior
                = input.isPretoDeitar();
        sprawlPretoAnterior
                = input.isPretoSprawl();
    }

    // =====================================================
    // 7.4 BLOQUEIO TEMPORARIO POR ANIMACAO DE COMBATE
    // =====================================================
    private void atualizarMiauBrancoBloqueadoPorCombate() {

        jogadorBranco.setCorrendo(false);
        jogadorBranco.setRespirando(false);
        jogadorBranco.atualizar(false, false);

        // Teclas seguradas agora so voltam a valer depois de soltas (3A).
        input.travarComandosPressionadosBranco();

        // Sincroniza as bordas das teclas para nada disparar ao desbloquear.
        puloBrancoAnterior = input.isPular();
        puloCurtoBrancoAnterior = input.isPularCurto();
        rolamentoBrancoAnterior = input.isRolar();
        deitarBrancoAnterior = input.isDeitar();
        sprawlBrancoAnterior = input.isSprawl();
    }

    private void atualizarMiauPretoBloqueadoPorCombate() {

        jogadorPreto.setCorrendo(false);
        jogadorPreto.setRespirando(false);
        jogadorPreto.atualizar(false, false);

        input.travarComandosPressionadosPreto();

        puloPretoAnterior = input.isPretoPular();
        puloCurtoPretoAnterior = input.isPretoPularCurto();
        rolamentoPretoAnterior = input.isPretoRolar();
        deitarPretoAnterior = input.isPretoDeitar();
        sprawlPretoAnterior = input.isPretoSprawl();
    }

    // =====================================================
    // 7.5 ENTRADAS DO MOTOR DE COMBATE
    // =====================================================
    private void atualizarEntradasCombate() {

        /*
         * Entrega 5C: tecla fisica -> acao logica (InputHandler) -> borda de
         * subida (DetectorToques) -> sistema existente. A ordem e os
         * resultados sao os da 5B (pegada, desequilibrio, reforco, rompimento);
         * o SistemaCombate continua decidindo se a acao vale no estado atual.
         */
        for (AcaoJogador acao : AcoesContextuais.ACOES_DE_COMBATE) {
            for (SlotJogador slot : SlotJogador.values()) {
                if (!slot.usa(acao)) {
                    continue;
                }
                // No treino o parceiro e passivo: as teclas do segundo jogador sao ignoradas.
                boolean ativa = !(modoTreino && slot == SlotJogador.JOGADOR_2)
                        && input.isAtiva(slot, acao);
                if (toques.tocou(slot, acao, ativa)) {
                    executarAcaoCombate(slot, acao);
                }
            }
        }

        // Entrega 5F: D-pad contextual (3 camadas), pelo mesmo caminho das teclas.
        for (SlotJogador slot : SlotJogador.values()) {
            dispositivos.consumirComandoContextual(slot).ifPresent(p -> executarComandoContextual(slot, p));
        }
    }

    /** Uma das 12 posicoes: executa a acao existente, ou avisa que esta vazia/reservada. */
    private void executarComandoContextual(SlotJogador slot, PosicaoComando posicao) {
        if (modoTreino && slot == SlotJogador.JOGADOR_2) {
            return;                                     // parceiro passivo
        }
        IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
        IrmaoMiau outro = ator == jogadorBranco ? jogadorPreto : jogadorBranco;
        java.util.Optional<CelulaContextual> c = RepertorioContextual.celula(
                RepertorioContextual.paraCombate(sistemaCombate, slot, ator, outro), posicao);
        if (c.isEmpty()) {
            feedback.mostrarInfo(slot.getSigla() + ": nada em " + nomeCamada(slot, posicao.camada())
                    + " " + posicao.direcao().getSeta(), tick);
        } else if (c.get().isExecutavel()) {
            executarAcaoCombate(slot, c.get().acao());
        } else {
            feedback.mostrarInfo(c.get().rotulo() + ": ainda não disponível no jogo", tick);
        }
    }

    /** "D-pad", "LT + D-pad", "LT + LB + D-pad" com os botoes configurados. */
    String nomeCamada(SlotJogador slot, CamadaComando camada) {
        String m2 = mapaGamepad.botao(slot, ItemGamepad.de(ComandoGamepad.MODIFICADOR_CAMADA_2))
                .map(b -> b.getRotulo().split("/")[0]).orElse("?");
        String m3 = mapaGamepad.botao(slot, ItemGamepad.de(ComandoGamepad.MODIFICADOR_CAMADA_3))
                .map(b -> b.getRotulo().split("/")[0]).orElse("?");
        return switch (camada) {
            case UM -> "D-pad";
            case DOIS -> m2 + " + D-pad";
            case TRES -> m2 + " + " + m3 + " + D-pad";
        };
    }

    /** Entrega 5F: cruz do D-pad do slot (null se o slot nao usa o contextual agora). */
    PainelDpadContextual.Quadro montarQuadroDpad(SlotJogador slot) {
        if (sistemaCombate == null || !dispositivos.usaContextualAgora(slot)
                || (modoTreino && slot == SlotJogador.JOGADOR_2)) {
            return null;
        }
        IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
        IrmaoMiau outro = ator == jogadorBranco ? jogadorPreto : jogadorBranco;
        CamadaComando camada = dispositivos.camadaAtiva(slot);
        return PainelDpadContextual.montar(slot.getSigla(), camada, nomeCamada(slot, camada),
                RepertorioContextual.paraCombate(sistemaCombate, slot, ator, outro));
    }

    /** Uma acao de combate pedida por um slot, encaminhada ao SistemaCombate. */
    private void executarAcaoCombate(SlotJogador slot, AcaoJogador acao) {

        IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
        IrmaoMiau outro = ator == jogadorBranco ? jogadorPreto : jogadorBranco;

        switch (acao) {
            case INICIAR_PEGADA -> {
                ResultadoAcao resultado = sistemaCombate.tentarAcao(ator, outro, "pegada_1");
                feedback.registrarTentativa(sistemaCombate, ator, outro, "pegada_1", resultado, tick);
                if (resultado == ResultadoAcao.SUCESSO) {
                    audio.tocarEfeito(AudioJogo.EFEITO_PEGADA);
                }
            }
            // Desequilibrio: so vale se este lutador for o APLICADOR da pegada.
            case TENTAR_DESEQUILIBRIO -> feedback.registrarAcaoTecnica(
                    com.irmaosmiau.combate.TipoDisputa.DESEQUILIBRIO,
                    sistemaCombate.tentarDesequilibrio(ator),
                    tick);
            // Reforco: so vale se este lutador for o APLICADOR.
            case REFORCAR_PEGADA -> feedback.registrarReforco(
                    sistemaCombate.tentarReforcarPegada(ator), tick);
            // Rompimento tecnico: so vale se este lutador for o RECEPTOR.
            case ROMPIMENTO_TECNICO -> feedback.registrarAcaoTecnica(
                    com.irmaosmiau.combate.TipoDisputa.ROMPIMENTO_TECNICO,
                    sistemaCombate.tentarRompimentoTecnico(ator),
                    tick);
            default -> {
                // Acoes continuas/modificadoras sao lidas no movimento.
            }
        }
    }

    // =====================================================
    // 8. DIREÇÃO DAS AÇÕES
    // =====================================================
    private Direcao obterDirecao(
            boolean esquerda,
            boolean direita,
            boolean cima,
            boolean baixo
    ) {

        if (cima && esquerda) {
            return Direcao.CIMA_ESQUERDA;
        }

        if (cima && direita) {
            return Direcao.CIMA_DIREITA;
        }

        if (baixo && esquerda) {
            return Direcao.BAIXO_ESQUERDA;
        }

        if (baixo && direita) {
            return Direcao.BAIXO_DIREITA;
        }

        if (cima) {
            return Direcao.CIMA;
        }

        if (baixo) {
            return Direcao.BAIXO;
        }

        if (esquerda) {
            return Direcao.ESQUERDA;
        }

        if (direita) {
            return Direcao.DIREITA;
        }

        return null;
    }

    // =====================================================
    // 8. MOVIMENTO GENÉRICO
    // =====================================================
    private void moverPersonagem(
            IrmaoMiau personagem,
            boolean esquerda,
            boolean direita,
            boolean cima,
            boolean baixo,
            boolean agachando,
            boolean correndo,
            boolean respirando
    ) {
        /*
 * Durante o rolamento, o personagem
 * possui movimento próprio.
         */
        if (personagem.isRolando()
                || personagem.isFazendoSprawl()) {
            return;
        }
        double velocidadeAtual;

        // =========================
        // DEFINE A VELOCIDADE
        // =========================
        if (personagem.isDeitado()) {

            velocidadeAtual
                    = personagem.getVelocidadeRastejando();

        } else if (respirando) {

            velocidadeAtual
                    = personagem
                            .getVelocidadeRespirando();

        } else if (agachando) {

            velocidadeAtual
                    = personagem
                            .getVelocidadeAgachado();

        } else if (correndo) {

            velocidadeAtual
                    = personagem
                            .getVelocidadeCorrida();

        } else {

            velocidadeAtual
                    = personagem
                            .getVelocidadeBase();
        }

        /*
     * Agora o deslocamento continua
     * sendo double.
     *
     * IrmaoMiau é quem acumula
     * as frações dos pixels.
         */
        double deslocamento
                = velocidadeAtual;

        // =========================
        // MOVIMENTO HORIZONTAL
        // =========================
        if (direita) {

            personagem.moverX(
                    deslocamento
            );
        }

        if (esquerda) {

            personagem.moverX(
                    -deslocamento
            );
        }

        // =========================
        // MOVIMENTO VERTICAL
        // =========================
        if (cima) {

            personagem.moverVertical(
                    -deslocamento
            );
        }

        if (baixo) {

            personagem.moverVertical(
                    deslocamento
            );
        }
    }

    // =====================================================
    // 8. ORIENTAÇÃO DOS DOIS MIAUS
    // =====================================================
    private void atualizarOrientacaoDosMiaus() {

        if (jogadorBranco.getX()
                < jogadorPreto.getX()) {

            brancoViradoDireita = true;
            pretoViradoDireita = false;

        } else if (jogadorBranco.getX()
                > jogadorPreto.getX()) {

            brancoViradoDireita = false;
            pretoViradoDireita = true;
        }
        jogadorBranco.setViradoParaDireita(
                brancoViradoDireita
        );

        jogadorPreto.setViradoParaDireita(
                pretoViradoDireita
        );
        /*
         * Se os dois estiverem exatamente
         * no mesmo X, mantemos a direção
         * anterior.
         *
         * Isso evita os personagens
         * ficarem "tremendo" ou alternando
         * a direção sem necessidade.
         */
    }

    // =====================================================
    // 9. MENU DE PAUSA
    // =====================================================
    private void criarMenuPausa() {

        menuPausa
                = new JPanel(
                        new GridBagLayout()
                );

        menuPausa.setBackground(
                new Color(
                        230,
                        230,
                        230
                )
        );

        menuPausa.setBounds(
                250,
                65,
                300,
                320
        );

        GridBagConstraints gbc
                = new GridBagConstraints();

        gbc.gridx = 0;

        gbc.fill
                = GridBagConstraints.HORIZONTAL;

        gbc.insets
                = new Insets(
                        8,
                        20,
                        8,
                        20
                );

        javax.swing.JLabel titulo
                = new javax.swing.JLabel(
                        "PAUSADO",
                        javax.swing.SwingConstants.CENTER
                );

        titulo.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        28
                )
        );

        gbc.gridy = 0;

        menuPausa.add(
                titulo,
                gbc
        );

        JButton continuar
                = new JButton(
                        "CONTINUAR"
                );

        JButton trocarMiau
                = new JButton(
                        "TROCAR DE MIAU"
                );

        JButton menuPrincipal
                = new JButton(
                        "MENU PRINCIPAL"
                );

        // Entrega 5C: ajuda com as teclas configuradas.
        JButton controles
                = new JButton(
                        "CONTROLES"
                );

        Font fonteBotao
                = new Font(
                        "Arial",
                        Font.BOLD,
                        16
                );

        continuar.setFont(
                fonteBotao
        );

        trocarMiau.setFont(
                fonteBotao
        );

        menuPrincipal.setFont(
                fonteBotao
        );

        controles.setFont(
                fonteBotao
        );

        controles.addActionListener(
                e -> mostrarAjudaControles(true)
        );

        continuar.addActionListener(
                e -> alternarPausa()
        );

        trocarMiau.addActionListener(
                e -> {

                    gameLoop.stop();

                    aoTrocarPersonagem.run();
                }
        );

        menuPrincipal.addActionListener(
                e -> {

                    gameLoop.stop();

                    aoVoltarMenu.run();
                }
        );

        gbc.gridy = 1;

        menuPausa.add(
                continuar,
                gbc
        );

        gbc.gridy = 2;

        menuPausa.add(
                trocarMiau,
                gbc
        );

        gbc.gridy = 3;

        menuPausa.add(
                controles,
                gbc
        );

        gbc.gridy = 4;

        menuPausa.add(
                menuPrincipal,
                gbc
        );

        menuPausa.setVisible(false);

        add(menuPausa);

        voltarDaAjuda = new JButton("VOLTAR");
        voltarDaAjuda.setFont(fonteBotao);
        voltarDaAjuda.setBounds(630, 400, 120, 28);
        voltarDaAjuda.addActionListener(e -> mostrarAjudaControles(false));
        voltarDaAjuda.setVisible(false);
        add(voltarDaAjuda);
    }

    private void configurarPausa() {

        getInputMap(
                WHEN_IN_FOCUSED_WINDOW
        ).put(
                javax.swing.KeyStroke.getKeyStroke(
                        "pressed ESCAPE"
                ),
                "alternarPausa"
        );

        getInputMap(
                WHEN_IN_FOCUSED_WINDOW
        ).put(
                javax.swing.KeyStroke.getKeyStroke(
                        "shift pressed ESCAPE"
                ),
                "alternarPausa"
        );

        getActionMap().put(
                "alternarPausa",
                new javax.swing.AbstractAction() {

            @Override
            public void actionPerformed(
                    java.awt.event.ActionEvent e
            ) {

                alternarPausa();
            }
        }
        );
    }

    private void alternarPausa() {

        // ESC com a ajuda aberta volta ao menu de pausa.
        if (ajudaVisivel) {
            mostrarAjudaControles(false);
            return;
        }

        pausado
                = !pausado;

        menuPausa.setVisible(
                pausado
        );

        repaint();
    }

    /** Entrega 5C: abre/fecha a ajuda de controles (somente com o jogo pausado). */
    void mostrarAjudaControles(boolean visivel) {
        ajudaVisivel = visivel && pausado;
        menuPausa.setVisible(pausado && !ajudaVisivel);
        voltarDaAjuda.setVisible(ajudaVisivel);
        repaint();
    }

    boolean isAjudaVisivel() {
        return ajudaVisivel;
    }

    /** Conteudo da ajuda: colunas por jogador, acoes uteis agora e atalhos do sistema. */
    PainelAjudaControles.Conteudo montarAjudaControles() {
        AjudaControles.Rotulos mapa = rotulos();
        List<PainelAjudaControles.Coluna> colunas = new java.util.ArrayList<>();
        List<String> agora = new java.util.ArrayList<>();
        if (modoDoisMiaus && !modoTreino) {
            colunas.add(new PainelAjudaControles.Coluna("JOGADOR 1 · " + jogadorBranco.getNome()
                    + dispositivoNoTitulo(SlotJogador.JOGADOR_1),
                    AjudaControles.linhas(mapa, SlotJogador.JOGADOR_1)));
            colunas.add(new PainelAjudaControles.Coluna("JOGADOR 2 · " + jogadorPreto.getNome()
                    + dispositivoNoTitulo(SlotJogador.JOGADOR_2),
                    AjudaControles.linhas(mapa, SlotJogador.JOGADOR_2)));
            adicionarAgora(agora, "J1", mapa, SlotJogador.JOGADOR_1, jogadorBranco);
            adicionarAgora(agora, "J2", mapa, SlotJogador.JOGADOR_2, jogadorPreto);
        } else {
            IrmaoMiau jogador = modoDoisMiaus ? jogadorBranco : jogadorPrincipal;
            colunas.add(new PainelAjudaControles.Coluna("JOGADOR · " + jogador.getNome()
                    + dispositivoNoTitulo(SlotJogador.JOGADOR_1),
                    AjudaControles.linhas(mapa, SlotJogador.JOGADOR_1)));
            if (modoDoisMiaus) {
                adicionarAgora(agora, "J1", mapa, SlotJogador.JOGADOR_1, jogadorBranco);
            }
        }
        return new PainelAjudaControles.Conteudo(colunas, String.join("   ", agora),
                modoTreino ? "Parceiro de treino passivo." : "");
    }

    /** Entrega 5D: teclas ou botoes, conforme o dispositivo que controla cada slot AGORA. */
    AjudaControles.Rotulos rotulos() {
        java.util.Set<SlotJogador> noGamepad = java.util.EnumSet.noneOf(SlotJogador.class);
        for (SlotJogador s : SlotJogador.values()) {
            if (dispositivos.usaGamepadAgora(s)) {
                noGamepad.add(s);
            }
        }
        return new AjudaControles.Rotulos(input.getMapa(), mapaGamepad, noGamepad);
    }

    private String dispositivoNoTitulo(SlotJogador slot) {
        return dispositivos.usaGamepadAgora(slot) ? " · GAMEPAD" : "";
    }

    private void adicionarAgora(List<String> agora, String sigla, AjudaControles.Rotulos mapa,
            SlotJogador slot, IrmaoMiau lutador) {
        String texto = AjudaControles.agora(mapa, slot, PapelCombate.de(sistemaCombate, lutador));
        if (!texto.isEmpty()) {
            agora.add(sigla + ": " + texto);
        }
    }

    /** Entrega 5C: janela sem foco = nenhuma tecla continua segurada. */
    private void liberarTeclas() {
        if (dispositivos != null) {
            dispositivos.setFocado(false);
        }
        input.liberarTodas();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        java.awt.Window janela = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (janela != null) {
            janela.addWindowFocusListener(aoPerderFoco);
        }
    }

    // =====================================================
    // 9. FIM DE JOGO
    // =====================================================
    private void criarMenuFim() {

        menuFim = new JPanel(
                new GridBagLayout()
        );

        menuFim.setBackground(
                new Color(
                        230,
                        230,
                        230
                )
        );

        menuFim.setBounds(
                220,
                90,
                360,
                285
        );

        GridBagConstraints gbc
                = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.fill
                = GridBagConstraints.HORIZONTAL;

        gbc.insets
                = new Insets(
                        8,
                        25,
                        8,
                        25
                );

        // =========================
        // TÍTULO
        // =========================
        javax.swing.JLabel titulo
                = new javax.swing.JLabel(
                        "FIM",
                        javax.swing.SwingConstants.CENTER
                );

        titulo.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        32
                )
        );

        gbc.gridy = 0;

        menuFim.add(
                titulo,
                gbc
        );

        // =========================
        // MOTIVO
        // =========================
        javax.swing.JLabel motivo
                = new javax.swing.JLabel(
                        "",
                        javax.swing.SwingConstants.CENTER
                );

        motivo.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        12
                )
        );

        /*
         * Guardamos a referência dentro
         * do próprio painel para atualizar
         * depois quando alguém perder.
         */
        motivo.setName("motivoFim");

        gbc.gridy = 1;

        menuFim.add(
                motivo,
                gbc
        );

        // =========================
        // BOTÕES
        // =========================
        JButton tentarNovamente
                = new JButton(
                        "TENTAR NOVAMENTE"
                );

        JButton selecionarPersonagens
                = new JButton(
                        "SELECIONAR PERSONAGENS"
                );

        JButton menuPrincipal
                = new JButton(
                        "MENU PRINCIPAL"
                );

        Font fonteBotao
                = new Font(
                        "Arial",
                        Font.BOLD,
                        15
                );

        tentarNovamente.setFont(
                fonteBotao
        );

        selecionarPersonagens.setFont(
                fonteBotao
        );

        menuPrincipal.setFont(
                fonteBotao
        );

        tentarNovamente.addActionListener(
                e -> {

                    gameLoop.stop();

                    aoTentarNovamente.run();
                }
        );

        selecionarPersonagens.addActionListener(
                e -> {

                    gameLoop.stop();

                    aoTrocarPersonagem.run();
                }
        );

        menuPrincipal.addActionListener(
                e -> {

                    gameLoop.stop();

                    aoVoltarMenu.run();
                }
        );

        gbc.gridy = 2;

        menuFim.add(
                tentarNovamente,
                gbc
        );

        gbc.gridy = 3;

        menuFim.add(
                selecionarPersonagens,
                gbc
        );

        gbc.gridy = 4;

        menuFim.add(
                menuPrincipal,
                gbc
        );

        menuFim.setVisible(false);

        add(menuFim);
    }

    private void verificarFimDeJogo() {

        if (modoDoisMiaus) {

            boolean brancoFim
                    = jogadorBranco.isSemCondicionamento();

            boolean pretoFim
                    = jogadorPreto.isSemCondicionamento();

            if (brancoFim && pretoFim) {

                fimDeJogo = true;
                mensagemFim
                        = "OS DOIS MIAUS NÃO CONSEGUEM MAIS LUTAR";

            } else if (brancoFim) {

                fimDeJogo = true;
                mensagemFim
                        = "MIAU BRANCO NÃO CONSEGUE MAIS LUTAR";

            } else if (pretoFim) {

                fimDeJogo = true;
                mensagemFim
                        = "MIAU PRETO NÃO CONSEGUE MAIS LUTAR";
            }

        } else if (jogadorPrincipal
                .isSemCondicionamento()) {

            fimDeJogo = true;

            mensagemFim
                    = jogadorPrincipal.getNome()
                    + " NÃO CONSEGUE MAIS LUTAR";
        }

        if (fimDeJogo) {

            atualizarMenuFim();

            menuFim.setVisible(true);

            setComponentZOrder(
                    menuFim,
                    0
            );

            repaint();
        }
    }

    private void atualizarMenuFim() {

        for (java.awt.Component componente
                : menuFim.getComponents()) {

            if (componente instanceof javax.swing.JLabel) {

                javax.swing.JLabel label
                        = (javax.swing.JLabel) componente;

                if ("motivoFim".equals(
                        label.getName()
                )) {

                    label.setText(
                            mensagemFim
                    );

                    break;
                }
            }
        }
    }

    private void desenharFimDeJogo(
            Graphics2D g2
    ) {

        g2.setColor(
                new Color(
                        0,
                        0,
                        0,
                        120
                )
        );

        g2.fillRect(
                0,
                0,
                LARGURA,
                ALTURA
        );
    }

    // =====================================================
    // 10. RENDERIZAÇÃO PRINCIPAL
    // =====================================================
    protected void paintComponent(
            Graphics g
    ) {

        super.paintComponent(g);

        Graphics2D g2
                = (Graphics2D) g;

        // =========================
        // CENARIO (fundo e chao)
        // =========================
        cenario.desenharFundo(g2, LARGURA, ALTURA);
        cenario.desenharChao(g2, LARGURA, ALTURA);

        // =========================
        // PERSONAGENS (do fundo para a frente)
        // =========================
        if (modoDoisMiaus) {

            /*
             * Quem esta numa linha mais ao fundo e desenhado primeiro.
             * Mesma linha: Branco antes do Preto (ordem anterior, estavel).
             */
            for (IrmaoMiau lutador : ordemDeDesenho()) {
                desenharPersonagemOrientado(
                        g2,
                        lutador,
                        lutador == jogadorBranco
                                ? brancoViradoDireita
                                : pretoViradoDireita
                );
            }

        } else {

            jogadorPrincipal.desenhar(g2);
        }

        cenario.desenharPrimeiroPlano(g2, LARGURA, ALTURA);

        // =========================
        // HUD DO JOGADOR E DEBUG
        // =========================
        hud.desenhar(g2, montarEstadoHud(), LARGURA, ALTURA);

        // Entrega 5F: cruz do D-pad contextual de cada jogador, sob o seu painel.
        PainelDpadContextual.Quadro dpad1 = montarQuadroDpad(SlotJogador.JOGADOR_1);
        if (dpad1 != null) {
            PainelDpadContextual.desenhar(g2, 10, dpad1);
        }
        PainelDpadContextual.Quadro dpad2 = montarQuadroDpad(SlotJogador.JOGADOR_2);
        if (dpad2 != null) {
            PainelDpadContextual.desenhar(g2, LARGURA - 10 - PainelDpadContextual.LARGURA, dpad2);
        }

        if (debugVisivel) {
            desenharCamadaDebug(g2);
        }

        // =========================
        // ESCURECER DURANTE PAUSA
        // =========================
        if (pausado) {

            g2.setColor(
                    new Color(
                            0,
                            0,
                            0,
                            90
                    )
            );

            g2.fillRect(
                    0,
                    0,
                    LARGURA,
                    ALTURA
            );
        }

        if (ajudaVisivel) {

            PainelAjudaControles.desenhar(g2, montarAjudaControles(), LARGURA, ALTURA);
        }

        if (fimDeJogo) {

            desenharFimDeJogo(g2);
        }
    }

    /** Lutadores na ordem em que sao desenhados (fundo -> frente). */
    List<IrmaoMiau> ordemDeDesenho() {
        return OrdemProfundidade.doFundoParaFrente(jogadorBranco, jogadorPreto);
    }

    // =====================================================
    // 10. ESPELHAMENTO / ORIENTAÇÃO VISUAL
    // =====================================================
    private void desenharPersonagemOrientado(
            Graphics2D g2,
            IrmaoMiau personagem,
            boolean viradoParaDireita
    ) {

        Graphics2D copia
                = (Graphics2D) g2.create();

        if (!viradoParaDireita) {

            /*
             * A cabeça possui 70 unidades
             * de largura.
             *
             * Portanto, 35 representa
             * aproximadamente o centro do
             * personagem.
             */
            double eixoCentral
                    = personagem.getX()
                    + 35
                    * ESCALA_PERSONAGEM;

            copia.translate(
                    eixoCentral,
                    0
            );

            copia.scale(
                    -1,
                    1
            );

            copia.translate(
                    -eixoCentral,
                    0
            );
        }

        personagem.desenhar(
                copia
        );

        copia.dispose();
    }

    // =====================================================
    // DEBUG TEMPORARIO DO MOTOR DE COMBATE
    // =====================================================
    private void desenharDebugCombate(
            Graphics2D g2
    ) {

        if (sistemaCombate == null) {

            return;
        }

        ContextoCombate contexto
                = sistemaCombate.getContextoAParaBAtual();

        if (contexto == null) {
            return;
        }

        boolean pegadaAtiva
                = sistemaCombate.getEstadoInteracao()
                == EstadoInteracaoCombate.PEGADA_1;

        boolean pegadaDisponivel
                = !pegadaAtiva
                && sistemaCombate.podeTentarAcao(
                        jogadorBranco,
                        jogadorPreto,
                        "pegada_1"
                );

        String estadoPegada = pegadaAtiva
                ? "ATIVA"
                : (pegadaDisponivel
                        ? "DISPONIVEL"
                        : "INDISPONIVEL");

        g2.setColor(Color.BLACK);
        g2.setFont(
                new Font(
                        "Monospaced",
                        Font.PLAIN,
                        11
                )
        );

        g2.drawString(
                "COMBATE: ATIVO",
                270,
                45
        );

        g2.drawString(
                String.format(
                        "DIST: %.1f px / %.1f u",
                        contexto.getDistanciaHorizontalPixels(),
                        contexto.getDistanciaHorizontalCombate()
                ),
                270,
                62
        );

        g2.drawString(
                "LINHAS: "
                + contexto.getDiferencaLinhas()
                + " | POS: "
                + contexto.getRelacaoHorizontal(),
                270,
                79
        );

        g2.drawString(
                "FRENTE A->B: "
                + (contexto.isAtacanteDeFrenteParaDefensor()
                        ? "SIM"
                        : "NAO")
                + " | B->A: "
                + (contexto.isDefensorDeFrenteParaAtacante()
                        ? "SIM"
                        : "NAO"),
                270,
                96
        );

        g2.drawString(
                "PEGADA_1: " + estadoPegada,
                270,
                113
        );

        g2.drawString(
                "INTERACAO: "
                + sistemaCombate.getEstadoInteracao(),
                270,
                130
        );

        String textoAnimacao = "-";

        if (sistemaCombate.isAnimacaoCombateAtiva()) {

            com.irmaosmiau.combate.AnimacaoCombateAtiva animacao
                    = sistemaCombate.getAnimacaoCombateAtiva();

            int percentualAnimacao
                    = (int) Math.round(
                            animacao.getProgresso() * 100.0
                    );

            textoAnimacao
                    = animacao.getIdAnimacao()
                    + " "
                    + animacao.getFrameAtual()
                    + "/"
                    + animacao.getDuracaoFrames()
                    + " ("
                    + percentualAnimacao
                    + "%)";
        }

        g2.drawString(
                "ANIM: " + textoAnimacao,
                270,
                147
        );

        ResultadoAcao ultimoResultado
                = sistemaCombate.getUltimoResultado();

        String textoUltima = ultimoResultado == null
                ? "-"
                : String.valueOf(
                        sistemaCombate.getUltimaAcaoId()
                ) + " -> " + ultimoResultado;

        g2.drawString(
                "ULTIMA: " + textoUltima,
                270,
                164
        );

        g2.drawString(
                "H = tentar PEGADA_1 (Branco)",
                270,
                181
        );

        // Entrega 3A: de onde veio a animacao e por que a ultima pegada acabou.
        com.irmaosmiau.combate.AcaoCombate pegada1
                = sistemaCombate.getAcao("pegada_1");
        String fonte = pegada1 == null || !pegada1.possuiAnimacaoConjunta()
                ? "-"
                : String.valueOf(sistemaCombate.getRegistroAnimacoes()
                        .getOrigem(pegada1.getIdAnimacaoConjunta()));

        g2.drawString(
                "FONTE ANIM: " + fonte,
                270,
                198
        );

        g2.drawString(
                "ROMPIMENTO: "
                + (sistemaCombate.getUltimoMotivoEncerramento() == null
                        ? "-"
                        : sistemaCombate.getUltimoMotivoEncerramento()),
                270,
                215
        );

        // Entrega 5B: firmeza, desgaste e recargas.
        g2.drawString(
                String.format(java.util.Locale.ROOT, "FIRMEZA: %.1f  (-%.2f/t, +%.2f/t movendo) | REC T:%d Y:%d",
                        sistemaCombate.getFirmeza(),
                        SistemaCombate.DESGASTE_FIRMEZA_POR_TICK,
                        SistemaCombate.DESGASTE_EXTRA_DESLOCAMENTO_POR_TICK,
                        sistemaCombate.getRecargaRestante(jogadorBranco),
                        sistemaCombate.getRecargaReforcoRestante(jogadorBranco)),
                270,
                249
        );

        // Entrega 5A: ultima disputa tecnica com as pontuacoes.
        g2.drawString(
                "DISPUTA: "
                + (sistemaCombate.getUltimaDisputa() == null
                        ? "-"
                        : sistemaCombate.getUltimaDisputa()),
                270,
                232
        );
    }

    // =====================================================
    // 11. HUD DO JOGADOR (Entrega 4A)
    // =====================================================
    /** Monta o que o HUD mostra neste frame (somente leitura do jogo). */
    HudCombate.Estado montarEstadoHud() {

        List<DadosLutadorHud> lutadores;
        String dica;

        if (modoTreino) {
            lutadores = List.of(
                    DadosLutadorHud.de(jogadorBranco, "JOGADOR"),
                    DadosLutadorHud.de(jogadorPreto, "PARCEIRO")
            );
            dica = AjudaControles.dicaHud(AjudaControles.Modo.TREINO, rotulos(),
                    PapelCombate.de(sistemaCombate, jogadorBranco), PapelCombate.de(sistemaCombate, jogadorPreto));
        } else if (modoDoisMiaus) {
            lutadores = List.of(
                    DadosLutadorHud.de(jogadorBranco, "J1"),
                    DadosLutadorHud.de(jogadorPreto, "J2")
            );
            dica = AjudaControles.dicaHud(AjudaControles.Modo.DOIS_JOGADORES, rotulos(),
                    PapelCombate.de(sistemaCombate, jogadorBranco), PapelCombate.de(sistemaCombate, jogadorPreto));
        } else {
            lutadores = List.of(
                    DadosLutadorHud.de(jogadorPrincipal, "JOGADOR")
            );
            dica = AjudaControles.dicaHud(AjudaControles.Modo.INDIVIDUAL, rotulos(),
                    PapelCombate.LIVRE, PapelCombate.LIVRE);
        }

        return new HudCombate.Estado(
                lutadores,
                FeedbackCombate.textoEstado(sistemaCombate,
                        modoDoisMiaus
                                ? List.of(jogadorBranco, jogadorPreto)
                                : List.of(jogadorPrincipal)),
                feedback.getMensagem(tick),
                feedback.getTipo(),
                modoTreino,
                dica
        );
    }

    /** Debug tecnico do motor, numa caixa propria abaixo do HUD. */
    private void desenharCamadaDebug(Graphics2D g2) {

        if (sistemaCombate == null) {
            return;
        }

        Graphics2D d = (Graphics2D) g2.create();
        d.translate(0, 30);
        d.setColor(new Color(255, 255, 255, 170));
        d.fillRoundRect(262, 32, 400, 224, 8, 8);
        desenharDebugCombate(d);
        d.dispose();
    }

    // =====================================================
    // 12. ATALHOS DE APRESENTACAO (Entrega 4A)
    // =====================================================
    /*
     * Teclas livres (nao usadas pelos controles dos Miaus):
     * F3 = debug tecnico | F4 = proximo cenario
     */
    private void configurarAtalhos() {
        atalho("F3", "alternarDebug", () -> setDebugVisivel(!debugVisivel));
        atalho("F4", "proximoCenario", () -> {
            cenario = cenarios.proximo(cenario);
            mostrarPreferencia("Cenário: " + cenario.getNome(),
                    preferencias.definirCenarioPreferido(cenario.getId()));
            repaint();
        });
        atalho("F5", "reiniciarTreino", this::reiniciarTreino);
        atalho("F9", "alternarMusica", () -> {
            java.util.Optional<String> erro = preferencias.alternarMusica();
            mostrarPreferencia("Música: "
                    + (preferencias.getAtual().isMusicaAtiva() ? "ligada" : "desligada"), erro);
        });
        atalho("F10", "alternarEfeitos", () -> {
            java.util.Optional<String> erro = preferencias.alternarEfeitos();
            mostrarPreferencia("Efeitos: "
                    + (preferencias.getAtual().isEfeitosAtivos() ? "ligados" : "desligados"), erro);
        });
    }

    /** Mensagem do atalho; se a gravacao falhou, avisa (a mudanca vale assim mesmo). */
    private void mostrarPreferencia(String texto, java.util.Optional<String> erroAoSalvar) {
        feedback.mostrarInfo(
                erroAoSalvar.map(e -> texto + " (não foi salvo)").orElse(texto),
                tick
        );
        erroAoSalvar.ifPresent(e -> System.err.println("[preferencias] " + e));
    }

    /**
     * Treino: volta jogador e parceiro as posicoes iniciais (novas
     * instancias, folego cheio, sem pegada). Fora do treino nao faz nada.
     */
    void reiniciarTreino() {

        if (!modoTreino || pausado || fimDeJogo) {
            return;
        }

        sessaoTreino.reiniciar();

        jogadorBranco = sessaoTreino.getJogador();
        jogadorPreto = sessaoTreino.getParceiro();
        sistemaCombate = sessaoTreino.getSistema();
        brancoViradoDireita = jogadorBranco.isViradoParaDireita();
        pretoViradoDireita = jogadorPreto.isViradoParaDireita();

        // Nada do que estiver segurado dispara no primeiro tick.
        puloBrancoAnterior = input.isPular();
        puloCurtoBrancoAnterior = input.isPularCurto();
        rolamentoBrancoAnterior = input.isRolar();
        deitarBrancoAnterior = input.isDeitar();
        sprawlBrancoAnterior = input.isSprawl();
        for (AcaoJogador acao : AcoesContextuais.ACOES_DE_COMBATE) {
            if (SlotJogador.JOGADOR_1.usa(acao)) {
                toques.sincronizar(SlotJogador.JOGADOR_1, acao, input.isAtiva(SlotJogador.JOGADOR_1, acao));
            }
        }
        deslocandoConjunto = false;

        feedback.reiniciarObservacao();
        feedback.mostrarInfo("Posições reiniciadas", tick);
        repaint();
    }

    /**
     * Ao sair da tela (menu, selecao, tentar novamente) o loop deste
     * painel para; antes ele continuava rodando em segundo plano.
     */
    @Override
    public void removeNotify() {
        gameLoop.stop();
        java.awt.Window janela = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (janela != null) {
            janela.removeWindowFocusListener(aoPerderFoco);
        }
        super.removeNotify();
    }

    // =====================================================
    // ACESSO PARA TESTES DO PACOTE
    // =====================================================
    void pararLoopParaTestes() {
        gameLoop.stop();
    }

    void atualizarUmTickParaTestes() {
        atualizar();
    }

    boolean isModoTreino() {
        return modoTreino;
    }

    SessaoTreino getSessaoTreino() {
        return sessaoTreino;
    }

    IrmaoMiau getLutador1() {
        return modoDoisMiaus ? jogadorBranco : jogadorPrincipal;
    }

    IrmaoMiau getLutador2() {
        return jogadorPreto;
    }

    boolean isPausado() {
        return pausado;
    }

    /** Entrega 5C: o mesmo que acontece quando a janela perde o foco. */
    void simularPerdaDeFocoParaTestes() {
        liberarTeclas();
    }

    SessaoDispositivos getSessaoDispositivos() {
        return dispositivos;
    }

    void simularFocoDeVoltaParaTestes() {
        dispositivos.setFocado(true);
    }

    MapaControles getMapaControles() {
        return input.getMapa();
    }

    private void atalho(String tecla, String nome, Runnable acao) {

        // Tambem com SHIFT segurado (modo controle padrao) o atalho continua valendo.
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                javax.swing.KeyStroke.getKeyStroke("pressed " + tecla),
                nome
        );
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                javax.swing.KeyStroke.getKeyStroke("shift pressed " + tecla),
                nome
        );

        getActionMap().put(nome, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                acao.run();
            }
        });
    }

    public void setDebugVisivel(boolean visivel) {
        debugVisivel = visivel;
        repaint();
    }

    public boolean isDebugVisivel() {
        return debugVisivel;
    }

    /** Cenario atual (somente leitura). */
    public Cenario getCenario() {
        return cenario;
    }

    /** Troca o cenario por id; erro claro se nao existir. */
    public void usarCenario(String idCenario) {
        cenario = cenarios.exigir(idCenario);
        repaint();
    }
}
