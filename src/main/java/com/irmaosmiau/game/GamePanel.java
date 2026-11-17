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
import com.irmaosmiau.animacao.AnimadorPartida;
import com.irmaosmiau.animacao.ControladorAnimacoes;
import com.irmaosmiau.animacao.PerfisAnimacoes;
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
public class GamePanel extends JPanel implements com.irmaosmiau.ui.TelaNavegavel {

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
    // Entrega 5J: postura inicial da luta (de joelhos ou em pe).
    private final com.irmaosmiau.entities.InicioLuta inicioLuta;
    // 5I: arrasto por slot (o aplicador pode ser o J1 ou o J2); deslocandoConjunto = OU dos dois.
    private boolean deslocandoConjuntoJ1;
    private boolean deslocandoConjuntoJ2;

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
    // 5I: primeiro foco da pausa (A/ENTER = continuar) e o "voltar" do B.
    private JButton botaoContinuar;
    private JButton reiniciarLuta;

    // =====================================================
    // ENTREGA 5Q.2: LABORATORIO DO TREINO
    // =====================================================
    /** So o treino usa (a partida normal nunca liga o laboratorio). */
    private final com.irmaosmiau.treino.LaboratorioTreino lab = new com.irmaosmiau.treino.LaboratorioTreino();
    private boolean catalogoLabVisivel;
    private final java.util.List<java.awt.Component> botoesPausaNormal = new java.util.ArrayList<>();
    private final java.util.List<JButton> botoesLab = new java.util.ArrayList<>();
    private JButton botaoLaboratorio;
    private JButton botaoSituacao;
    private JButton botaoControla;
    private JButton botaoParceiroLab;
    private JButton botaoReiniciarSituacao;

    /** O J2 do treino esta parado (parceiro passivo)? No laboratorio pode ser controlado. */
    boolean parceiroPassivo(SlotJogador slot) {
        return modoTreino && slot == SlotJogador.JOGADOR_2 && !lab.isParceiroControlado();
    }

    com.irmaosmiau.treino.LaboratorioTreino getLaboratorio() {
        return lab;
    }

    boolean isCatalogoLabVisivel() {
        return catalogoLabVisivel;
    }

    /**
     * 5Q.2: inicia (ou reinicia) a situacao do laboratorio: a sessao de treino recria os
     * lutadores pelas escolhas (faixa, kimono, folego cheio) e o motor entra na posicao.
     */
    void iniciarSituacaoLaboratorio() {
        if (!modoTreino || fimDeJogo) {
            return;
        }
        reiniciarSessaoTreino();
        religarDispositivosDoTreino();
        lab.aplicar(solo, sistemaCombate, jogadorBranco, jogadorPreto);
        avisos.limpar();
        avisos.adicionar("LAB: " + lab.getSituacao().getNome().toUpperCase(java.util.Locale.ROOT)
                + " · CONTROLA " + (lab.getAtivo() == com.irmaosmiau.posicional.Lado.A ? "J1" : "J2"));
        atualizarBotoesLab();
        if (pausado) {
            mostrarLaboratorio(false);
            alternarPausa();
        }
    }

    /**
     * 5Q.2: com o J2 controlado no laboratorio, liga os dispositivos do J2 (teclado virtual
     * e/ou gamepad), como numa partida; com o parceiro passivo, o treino volta a ler so o J1.
     */
    private void religarDispositivosDoTreino() {
        java.util.List<SlotJogador> slots = lab.isParceiroControlado()
                ? java.util.List.of(SlotJogador.JOGADOR_1, SlotJogador.JOGADOR_2)
                : java.util.List.of(SlotJogador.JOGADOR_1);
        if (!lab.isParceiroControlado()) {
            input.conectarFonte(SlotJogador.JOGADOR_2, null);
        }
        dispositivos = new SessaoDispositivos(PreferenciasDispositivos.global().getAtual(), ServicoGamepads.global(),
                input, slots, mapaGamepad);
    }

    private void atualizarBotoesLab() {
        if (botaoSituacao == null) {
            return;
        }
        botaoSituacao.setText("SITUAÇÃO: " + lab.getSituacao().getNome().toUpperCase(java.util.Locale.ROOT));
        botaoControla.setText("CONTROLA: " + (lab.getAtivo() == com.irmaosmiau.posicional.Lado.A ? "J1" : "J2"));
        botaoParceiroLab.setText("J2: " + (lab.isParceiroControlado() ? "CONTROLADO" : "PASSIVO"));
        botaoReiniciarSituacao.setVisible(modoTreino && lab.isAtiva() && botaoLaboratorio.isVisible());
    }

    /** Troca o menu de pausa entre a vista normal e a do laboratorio (o navegador segue os botoes visiveis). */
    void mostrarLaboratorio(boolean sim) {
        for (java.awt.Component c : botoesPausaNormal) {
            c.setVisible(!sim && (c != reiniciarLuta || aoTentarNovamente != null));
        }
        botaoLaboratorio.setVisible(!sim && modoTreino);
        for (JButton b : botoesLab) {
            b.setVisible(sim);
        }
        atualizarBotoesLab();
        botaoReiniciarSituacao.setVisible(!sim && modoTreino && lab.isAtiva());
        if (sim) {
            botaoSituacao.requestFocusInWindow();
        } else if (botaoContinuar != null) {
            botaoContinuar.requestFocusInWindow();
        }
        menuPausa.revalidate();
        menuPausa.repaint();
    }

    /** HUD tecnico do laboratorio (centro, abaixo dos avisos) e o catalogo (F8). */
    private void desenharLaboratorio(Graphics2D g0) {
        if (!modoTreino || !lab.isAtiva() || solo == null) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        java.util.List<String> linhas = lab.linhasHud(solo, jogadorBranco, jogadorPreto,
                ultimoMotivoFamilia.get(SlotJogador.JOGADOR_1), ultimoMotivoFamilia.get(SlotJogador.JOGADOR_2));
        int y = 60;
        g.setColor(new Color(10, 16, 40, 150));
        g.fillRoundRect(230, y, 340, 6 + 12 * linhas.size(), 8, 8);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.setColor(new Color(190, 230, 255));
        for (String l : linhas) {
            String t = l;
            while (g.getFontMetrics().stringWidth(t) > 330 && t.length() > 2) {
                t = t.substring(0, t.length() - 2) + "…";
            }
            y += 12;
            g.drawString(t, 236, y);
        }
        if (catalogoLabVisivel) {
            java.util.List<com.irmaosmiau.treino.LaboratorioTreino.Item> cat = lab.catalogo(solo);
            int yc = 140;
            int alt = 22 + 12 * Math.max(1, cat.size());
            g.setColor(new Color(10, 16, 40, 200));
            g.fillRoundRect(200, yc, 400, alt, 8, 8);
            g.setColor(new Color(255, 214, 120));
            g.drawString("CATÁLOGO · " + solo.getPosicao().getNome().toUpperCase(java.util.Locale.ROOT) + "  (F8 fecha)",
                    208, yc + 14);
            int yy = yc + 14;
            if (cat.isEmpty()) {
                g.setColor(new Color(170, 170, 170));
                g.drawString("nenhuma finalização nesta posição", 208, yy + 12);
            }
            for (com.irmaosmiau.treino.LaboratorioTreino.Item i : cat) {
                yy += 12;
                g.setColor(i.status() == com.irmaosmiau.treino.LaboratorioTreino.Status.LIBERADA
                        ? new Color(150, 240, 150) : new Color(200, 180, 180));
                String t = i.texto();
                while (g.getFontMetrics().stringWidth(t) > 384 && t.length() > 2) {
                    t = t.substring(0, t.length() - 2) + "…";
                }
                g.drawString(t, 208, yy);
            }
        }
        g.dispose();
    }

    /**
     * 5P.1: REINICIAR LUTA (pausa): recomeca a MESMA luta pelo mesmo caminho do
     * "tentar novamente" do fim de jogo: luta nova com os mesmos Miaus, arena,
     * modo, inicio e tempo; folego, posicoes, pegadas, solo e avisos zerados.
     */
    void reiniciarLuta() {
        if (aoTentarNovamente == null) {
            return;
        }
        gameLoop.stop();
        avisos.limpar();
        if (solo != null) {
            solo.reiniciar();
        }
        aoTentarNovamente.run();
    }

    /** Testes: o botao da pausa. */
    JButton botaoReiniciarLuta() {
        return reiniciarLuta;
    }
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

    /*
     * Entrega 5G: animacoes modulares no runtime. Escolhe a animacao de cada
     * lutador pelo estado e, quando o perfil tem uma JSON (hoje: movimento no
     * chao), entrega os quadros ao renderizador do proprio personagem; senao
     * o desenho legado continua. Atualizado uma vez por tick logico.
     */
    private final AnimadorPartida animador = new AnimadorPartida(
            new ControladorAnimacoes(PerfisAnimacoes.global()), INTERVALO_GAME_LOOP_MS);

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
        this(tipo, null, null, null, com.irmaosmiau.entities.InicioLuta.EM_PE,
                aoVoltarMenu, aoTrocarPersonagem, aoTentarNovamente);
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
        this(tipo, null, slotBranco, slotPreto, com.irmaosmiau.entities.InicioLuta.EM_PE,
                aoVoltarMenu, aoTrocarPersonagem, aoTentarNovamente);
    }

    /**
     * Entrega 5J: partida com o inicio escolhido (de joelhos ou em pe). Vale
     * para OS DOIS MIAUS; no modo individual nao ha adversario e o inicio e em pe.
     */
    public GamePanel(
            TipoPersonagem tipo,
            EscolhaLutador slotBranco,
            EscolhaLutador slotPreto,
            com.irmaosmiau.entities.InicioLuta inicio,
            Runnable aoVoltarMenu,
            Runnable aoTrocarPersonagem,
            Runnable aoTentarNovamente
    ) {
        this(tipo, null, slotBranco, slotPreto, inicio, aoVoltarMenu, aoTrocarPersonagem, aoTentarNovamente);
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
        this(TipoPersonagem.AMBOS, exigirTreino(treino), null, null, treino.getInicio(),
                aoVoltarMenu, aoTrocarPersonagem, aoTentarNovamente);
    }

    /**
     * Instancia o lutador escolhido para um slot, conferindo que a base
     * combina com os controles daquele slot.
     */
    private static IrmaoMiau criarDoSlot(EscolhaLutador escolha, String idBaseDoSlot, int x) {
        // 5I: qualquer irmao em qualquer slot. idBaseDoSlot e so o padrao quando nada foi escolhido.
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
            com.irmaosmiau.entities.InicioLuta inicio,
            Runnable aoVoltarMenu,
            Runnable aoTrocarPersonagem,
            Runnable aoTentarNovamente
    ) {

        this.aoVoltarMenu = aoVoltarMenu;
        this.inicioLuta = inicio == null ? com.irmaosmiau.entities.InicioLuta.EM_PE : inicio;
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

            // 5J: de joelhos = perto e simetrico no centro; em pe = como antes.
            boolean deJoelhos = inicioLuta == com.irmaosmiau.entities.InicioLuta.DE_JOELHOS;
            int xJ1 = deJoelhos ? com.irmaosmiau.entities.InicioLuta.xEsquerdaDeJoelhos(ESCALA_PERSONAGEM) : X_BRANCO_DUO;
            int xJ2 = deJoelhos ? com.irmaosmiau.entities.InicioLuta.xDireitaDeJoelhos(ESCALA_PERSONAGEM) : X_PRETO_DUO;

            jogadorBranco = slotBranco == null
                    ? new IrmaoMiauBranco(
                            xJ1,
                            Y_INICIAL_PERSONAGEM,
                            ESCALA_PERSONAGEM
                    )
                    : criarDoSlot(slotBranco, CatalogoPersonagens.ID_BRANCO, xJ1);

            jogadorPreto = slotPreto == null
                    ? new IrmaoMiauPreto(
                            xJ2,
                            Y_INICIAL_PERSONAGEM,
                            ESCALA_PERSONAGEM
                    )
                    : criarDoSlot(slotPreto, CatalogoPersonagens.ID_PRETO, xJ2);
            inicioLuta.aplicarPostura(jogadorBranco, jogadorPreto);

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
        criarMotorSolo();                                        // 5P

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
        // 5O.1: ENTER (Start do teclado-controle) so abre a pausa; aberta, o ENTER confirma o menu.
        if (dispositivos.consumirPausaTeclado() && !pausado) {
            alternarPausa();
        }

        if (pausado) {
            // Entrega 5F: nada do D-pad contextual fica pendente durante a pausa.
            dispositivos.limparContextual();

            repaint();
            return;
        }

        if (modoDoisMiaus) {

            xAntesDoTick1 = jogadorBranco.getX();
            xAntesDoTick2 = jogadorPreto.getX();
            atualizarMiauBranco();

            if (modoTreino && !lab.isParceiroControlado()) {
                // Parceiro de treino: ignora as teclas do segundo jogador (5Q.2: salvo no laboratorio, se pedido).
                sessaoTreino.atualizarParceiro();
                deslocandoConjuntoJ2 = false;
            } else {
                atualizarMiauPreto();
            }
            deslocandoConjunto = deslocandoConjuntoJ1 || deslocandoConjuntoJ2;
            // 5O: ninguem sai da arena (andar, correr, pular, rolar, sprawl, acrobacias...).
            manterNaArena(jogadorBranco);
            manterNaArena(jogadorPreto);
            impedirAtravessarNaAcrobacia(jogadorBranco, xAntesDoTick1, jogadorPreto, xAntesDoTick2);
            impedirAtravessarNaAcrobacia(jogadorPreto, xAntesDoTick2, jogadorBranco, xAntesDoTick1);

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
            atualizarSolo();                                     // 5P

        } else {

            atualizarJogadorPrincipal();
            manterNaArena(jogadorPrincipal);                     // 5O
            // Sem motor de combate no modo individual: comandos contextuais nao tem efeito.
            dispositivos.consumirComandoContextual(SlotJogador.JOGADOR_1);
        }

        // Entrega 5G: depois do movimento e do combate deste tick (nao move ninguem).
        animador.atualizar(
                modoDoisMiaus ? List.of(jogadorBranco, jogadorPreto) : List.of(jogadorPrincipal),
                sistemaCombate, deslocandoConjunto, tick);

        avisos.atualizar();                                       // 5P.1
        cenario.atualizarAmbiente(tick);                          // 5Q.1: arenas vivas
        tick++;
        contarTempoDeLuta();
        verificarFimDeJogo();
        repaint();
    }

    /**
     * 5O: limite horizontal da arena para QUALQUER movimento (antes so o
     * arrasto respeitava). A profundidade ja e limitada pelas cinco linhas.
     * Mesmas bordas do arrasto conjunto, em todos os mapas.
     */
    static void manterNaArena(IrmaoMiau l) {
        if (l == null) {
            return;
        }
        if (l.getX() < LIMITE_ESQUERDO_ARENA) {
            l.encostarNaBorda(LIMITE_ESQUERDO_ARENA - l.getX());
        } else if (l.getX() > LIMITE_DIREITO_ARENA) {
            l.encostarNaBorda(LIMITE_DIREITO_ARENA - l.getX());
        }
    }

    // =====================================================
    // ENTREGA 5M: MAPA E TEMPO DA PARTIDA
    // =====================================================
    /* Limite de tempo da luta (0 = sem limite) e ticks de luta ATIVA contados. */
    private int limiteSegundos;
    private long ticksDeLuta;

    /**
     * Entrega 5M: aplica as escolhas da selecao (antes do primeiro tick).
     * Mapa inexistente = o atual. O tempo so vale em JOGAR (dois jogadores,
     * fora do treino); 0 = sem limite.
     */
    public void configurarPartida(String idCenario, int segundosLimite) {
        if (idCenario != null && cenarios.contem(idCenario)) {
            cenario = cenarios.exigir(idCenario);
        }
        limiteSegundos = modoDoisMiaus && !modoTreino ? Math.max(0, segundosLimite) : 0;
        ticksDeLuta = 0;
    }

    /** So corre nos ticks de luta (pausa e fim de jogo nao chegam aqui). */
    private void contarTempoDeLuta() {
        if (limiteSegundos <= 0 || fimDeJogo) {
            return;
        }
        ticksDeLuta++;
        if (getSegundosRestantes() <= 0) {
            fimDeJogo = true;
            mensagemFim = "TEMPO ESGOTADO · " + resultadoPorTempo();
        }
    }

    /** Segundos que faltam (arredondados para cima); -1 sem limite. */
    public int getSegundosRestantes() {
        if (limiteSegundos <= 0) {
            return -1;
        }
        long ms = (long) limiteSegundos * 1000 - ticksDeLuta * INTERVALO_GAME_LOOP_MS;
        return (int) Math.max(0, Math.ceil(ms / 1000.0));
    }

    /** Quem terminou com mais condicionamento (proporcional ao maximo de cada um). */
    private String resultadoPorTempo() {
        return resultadoPorTempo(
                jogadorBranco.getCondicionamentoAtual() / jogadorBranco.getAtributos().getCondicionamento(),
                jogadorPreto.getCondicionamentoAtual() / jogadorPreto.getAtributos().getCondicionamento(),
                jogadorBranco.getNome(), jogadorPreto.getNome());
    }

    /** Fracoes de condicionamento (0..1) do J1 e do J2; diferenca menor que 1% = empate. */
    static String resultadoPorTempo(double j1, double j2, String nomeJ1, String nomeJ2) {
        if (Math.abs(j1 - j2) < 0.01) {
            return "EMPATE";
        }
        boolean j1Melhor = j1 > j2;
        String nome = j1Melhor ? nomeJ1 : nomeJ2;
        return "VANTAGEM: " + (j1Melhor ? "J1 " : "J2 ") + nome.replace("Irmão ", "").toUpperCase();
    }

    /** Relogio no topo da tela (so com limite). */
    private void desenharRelogio(Graphics2D g2) {
        int s = getSegundosRestantes();
        if (s < 0) {
            return;
        }
        String t = (s / 60) + ":" + String.format("%02d", s % 60);
        g2.setColor(new Color(10, 30, 80, 190));
        g2.fillRoundRect(LARGURA / 2 - 34, 4, 68, 26, 10, 10);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.setColor(s <= 10 ? new Color(255, 120, 90) : new Color(255, 206, 46));
        int w = g2.getFontMetrics().stringWidth(t);
        g2.drawString(t, LARGURA / 2 - w / 2, 24);
    }

    String getIdCenario() {
        return cenario.getId();
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
        // Entrega 5K: F/L3 = ciclo de posturas (clique simples) ou transicao rapida (duplo).
        processarPostura(SlotJogador.JOGADOR_1, jogadorPrincipal, input.isDeitar() && !deitarAnterior);

        // =========================
        // RESPIRAÇÃO
        // =========================
        boolean respirando
                = input.isRespirar() || respirandoPeloModificador(SlotJogador.JOGADOR_1, jogadorPrincipal);

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

            // 5O.1: R3 contextual pela postura (em pe = acrobacia; joelhos/deitado = rolamento).
            mobilidadeContextual(SlotJogador.JOGADOR_1, jogadorPrincipal, direcao);
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

        // Entrega 5L: receptor de pegada estabelecida nao anda, nao muda de linha, nao pula,
        // nao rola, nao agacha e nao faz sprawl: forcar so desgasta a firmeza (resistencia).
        boolean preso = receptorPreso(jogadorBranco) || presoNoSolo(jogadorBranco);   // 5P
        boolean agachando = !preso && input.isAgachar();

        // =========================
// DEITAR / LEVANTAR
// =========================
        // Entrega 5K: F/L3 = ciclo de posturas (clique simples) ou transicao rapida (duplo).
        processarPostura(SlotJogador.JOGADOR_1, jogadorBranco, input.isDeitar() && !deitarBrancoAnterior);

        // =========================
        // RESPIRAÇÃO
        // =========================
        boolean respirando
                = input.isRespirar() || respirandoPeloModificador(SlotJogador.JOGADOR_1, jogadorBranco);

        jogadorBranco.setRespirando(
                respirando
        );

        // =========================
        // CORRIDA
        // =========================
        boolean correndo = !preso && input.isCorrer() && !agachando && !respirando
                && !jogadorBranco.getDefesa().isAtiva();   // 5O.1: guarda nao corre

        jogadorBranco.setCorrendo(
                correndo
        );

        // =========================
// ROLAMENTO
// =========================
        if (!preso && input.isRolar() && !rolamentoBrancoAnterior) {

            Direcao direcao
                    = obterDirecao(
                            input.isEsquerda(),
                            input.isDireita(),
                            input.isCima(),
                            input.isBaixo()
                    );

            mobilidadeContextual(SlotJogador.JOGADOR_1, jogadorBranco, direcao);   // 5O.1
        }

        // =========================
// SPRAWL
// =========================
        if (!preso && input.isSprawl() && !sprawlBrancoAnterior) {

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
        // 5N: com o modo controle, cima/baixo (e as diagonais) tambem conduzem a dupla.
        boolean vertical = input.isCima() ^ input.isBaixo();
        boolean conjunto = (horizontal || vertical) && !preso                   // 5P: entrada em curso
                && input.isControle()
                && sistemaCombate != null
                && sistemaCombate.isInteracaoEstabelecida()
                && sistemaCombate.getAplicadorInteracao() == jogadorBranco;

        if (conjunto) {
            com.irmaosmiau.combate.ResultadoAcaoTecnica r = sistemaCombate.deslocarPegadaControlada(
                    jogadorBranco,
                    horizontal ? (input.isDireita() ? 1 : -1) : 0,
                    vertical ? (input.isBaixo() ? 1 : -1) : 0,
                    LIMITE_ESQUERDO_ARENA,
                    LIMITE_DIREITO_ARENA);
            if (!deslocandoConjuntoJ1) {
                feedback.mostrarInfo(r == com.irmaosmiau.combate.ResultadoAcaoTecnica.LIMITE_ARENA
                        ? "LIMITE DA ARENA" : "DESLOCAMENTO CONTROLADO", tick);
            }
        }
        deslocandoConjuntoJ1 = conjunto;

        if (preso && (input.isEsquerda() || input.isDireita() || input.isCima() || input.isBaixo())) {
            resistirNaPegada(SlotJogador.JOGADOR_1, jogadorBranco);
        }

        moverPersonagem(
                jogadorBranco,
                !preso && !conjunto && input.isEsquerda(),
                !preso && !conjunto && input.isDireita(),
                !preso && !conjunto && input.isCima(),
                !preso && !conjunto && input.isBaixo(),
                agachando,
                correndo,
                respirando
        );

        // =========================
        // PULO ALTO
        // =========================
        if (!preso && input.isPular() && !puloBrancoAnterior) {

            jogadorBranco.pular();
        }

        // =========================
        // PULO CURTO
        // =========================
        if (!preso && input.isPularCurto() && !puloCurtoBrancoAnterior) {

            jogadorBranco.pularCurto();
        }

        boolean andando = !preso && ((!conjunto && (input.isEsquerda() || input.isDireita()))
                || input.isCima() || input.isBaixo());

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

        // Entrega 5L: receptor de pegada estabelecida nao anda, nao muda de linha, nao pula,
        // nao rola, nao agacha e nao faz sprawl: forcar so desgasta a firmeza (resistencia).
        boolean preso = receptorPreso(jogadorPreto) || presoNoSolo(jogadorPreto);   // 5P
        boolean agachando = !preso && input.isPretoAgachar();

        // =========================
// DEITAR / LEVANTAR
// =========================
        // Entrega 5K: F/L3 = ciclo de posturas (clique simples) ou transicao rapida (duplo).
        processarPostura(SlotJogador.JOGADOR_2, jogadorPreto, input.isPretoDeitar() && !deitarPretoAnterior);
        // =========================
        // RESPIRAÇÃO
        // =========================
        boolean respirando
                = input.isPretoRespirar() || respirandoPeloModificador(SlotJogador.JOGADOR_2, jogadorPreto);

        jogadorPreto.setRespirando(
                respirando
        );

        // =========================
        // CORRIDA
        // =========================
        boolean correndo = !preso && input.isPretoCorrer() && !agachando && !respirando
                && !jogadorPreto.getDefesa().isAtiva();    // 5O.1: guarda nao corre

        jogadorPreto.setCorrendo(
                correndo
        );

        // =========================
// ROLAMENTO
// =========================
        if (!preso && input.isPretoRolar() && !rolamentoPretoAnterior) {

            Direcao direcao
                    = obterDirecao(
                            input.isPretoEsquerda(),
                            input.isPretoDireita(),
                            input.isPretoCima(),
                            input.isPretoBaixo()
                    );

            mobilidadeContextual(SlotJogador.JOGADOR_2, jogadorPreto, direcao);    // 5O.1
        }

        // =========================
// SPRAWL
// =========================
        if (!preso && input.isPretoSprawl() && !sprawlPretoAnterior) {

            jogadorPreto.iniciarSprawl();
        }

        // =========================
        // MOVIMENTO
        // =========================
        /*
         * Entrega 5I: o J2 tambem pode ser o aplicador. Mesma regra da 5B
         * (modo controle + esquerda/direita, so o aplicador de uma pegada
         * estabelecida); no Contextual o modo vem do analogico (5H).
         */
        boolean horizontal = input.isPretoEsquerda() ^ input.isPretoDireita();
        boolean vertical = input.isPretoCima() ^ input.isPretoBaixo();     // 5N
        boolean conjunto = (horizontal || vertical) && !preso                   // 5P: entrada em curso
                && input.isAtiva(SlotJogador.JOGADOR_2, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO)
                && sistemaCombate != null
                && sistemaCombate.isInteracaoEstabelecida()
                && sistemaCombate.getAplicadorInteracao() == jogadorPreto;

        if (conjunto) {
            com.irmaosmiau.combate.ResultadoAcaoTecnica r = sistemaCombate.deslocarPegadaControlada(
                    jogadorPreto,
                    horizontal ? (input.isPretoDireita() ? 1 : -1) : 0,
                    vertical ? (input.isPretoBaixo() ? 1 : -1) : 0,
                    LIMITE_ESQUERDO_ARENA,
                    LIMITE_DIREITO_ARENA);
            if (!deslocandoConjuntoJ2) {
                feedback.mostrarInfo(r == com.irmaosmiau.combate.ResultadoAcaoTecnica.LIMITE_ARENA
                        ? "LIMITE DA ARENA" : "DESLOCAMENTO CONTROLADO", tick);
            }
        }
        deslocandoConjuntoJ2 = conjunto;

        if (preso && (input.isPretoEsquerda() || input.isPretoDireita() || input.isPretoCima() || input.isPretoBaixo())) {
            resistirNaPegada(SlotJogador.JOGADOR_2, jogadorPreto);
        }

        moverPersonagem(
                jogadorPreto,
                !preso && !conjunto && input.isPretoEsquerda(),
                !preso && !conjunto && input.isPretoDireita(),
                !preso && !conjunto && input.isPretoCima(),
                !preso && !conjunto && input.isPretoBaixo(),
                agachando,
                correndo,
                respirando
        );

        // =========================
        // PULO ALTO
        // =========================
        if (!preso && input.isPretoPular() && !puloPretoAnterior) {

            jogadorPreto.pular();
        }

        // =========================
        // PULO CURTO
        // =========================
        if (!preso && input.isPretoPularCurto() && !puloCurtoPretoAnterior) {

            jogadorPreto.pularCurto();
        }

        boolean andando = !preso && (input.isPretoEsquerda()
                || input.isPretoDireita()
                || input.isPretoCima()
                || input.isPretoBaixo());

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
        /*
         * Entrega 5I: os dois slots usam todas as acoes; o PAPEL (aplicador,
         * receptor, livre) vem do SistemaCombate. Resolucao deterministica:
         * - toques do mesmo tick sao lidos primeiro e despachados na ordem
         *   ACOES_DE_COMBATE x (J1, J2), como desde a 5B;
         * - pegada simultanea: vale a do J1; a do J2 e descartada em silencio
         *   se uma interacao comecou neste tick (nada de pegadas incompativeis);
         * - desequilibrar e romper no mesmo toque (botao compartilhado do
         *   Classico): vale a do papel real (aplicador desequilibra, receptor
         *   rompe, livre fica com o desequilibrio, que so avisa).
         */
        pedidosSolo.clear();
        java.util.Map<SlotJogador, java.util.EnumSet<AcaoJogador>> tocadas = new java.util.EnumMap<>(SlotJogador.class);
        for (SlotJogador slot : SlotJogador.values()) {
            java.util.EnumSet<AcaoJogador> doSlot = java.util.EnumSet.noneOf(AcaoJogador.class);
            for (AcaoJogador acao : AcoesContextuais.ACOES_DE_COMBATE) {
                if (!slot.usa(acao)) {
                    continue;
                }
                // No treino o parceiro e passivo: as teclas do segundo jogador sao ignoradas.
                boolean ativa = !(parceiroPassivo(slot))
                        && input.isAtiva(slot, acao);
                if (toques.tocou(slot, acao, ativa)) {
                    doSlot.add(acao);
                }
            }
            resolverBotaoCompartilhado(slot, doSlot);
            tocadas.put(slot, doSlot);
            // 5P.2: aplicando a pegada, as teclas de pegada <- e v sao tiro de guarda e derrubada forcada
            if (!soloConduz(slot) && aplicaPegadaEstabelecida(slot)
                    && !input.isAtiva(slot, AcaoJogador.MODIFICADOR_PROJECAO)
                    && !input.isAtiva(slot, AcaoJogador.MODIFICADOR_DEFESA)) {
                for (AcaoJogador a : new AcaoJogador[] {AcaoJogador.PEGADA_3, AcaoJogador.PEGADA_4}) {
                    if (doSlot.remove(a)) {
                        pedirSolo(slot, direcaoDaAcao(a), com.irmaosmiau.posicional.TecnicaSolo.Familia.RS);
                    }
                }
            }
            // 5P: no chao (ou um em pe contra o solo) as teclas/direcoes de tecnica vao para o MotorSolo.
            if (soloConduz(slot)) {
                for (AcaoJogador a : ACOES_PEGADA) {
                    if (doSlot.contains(a) && !input.isAtiva(slot, AcaoJogador.MODIFICADOR_DEFESA)) {
                        // 5Q: no teclado, 4 (R1) + tecla no chao tambem chega ao motor (antes a tecla era
                        // consumida aqui e a familia R1 nunca a via)
                        pedirSolo(slot, direcaoDaAcao(a), input.isAtiva(slot, AcaoJogador.MODIFICADOR_PROJECAO));
                    }
                }
                doSlot.clear();
            }
        }
        // Entrega 5O: RT/RB + direcao da tecnica = defesas/quedas (nunca pegada); Y + direcao = acrobacia.
        atualizarFamilias(tocadas);
        boolean interacaoAntes = sistemaCombate.estaEmInteracao();
        for (AcaoJogador acao : AcoesContextuais.ACOES_DE_COMBATE) {
            for (SlotJogador slot : SlotJogador.values()) {
                if (!tocadas.get(slot).contains(acao)) {
                    continue;
                }
                if (AcoesContextuais.idPegada(acao) != null && !interacaoAntes && sistemaCombate.estaEmInteracao()) {
                    continue;   // o outro slot ja comecou a pegada neste tick
                }
                executarAcaoCombate(slot, acao);
            }
        }

        // Entrega 5F: D-pad contextual (3 camadas), pelo mesmo caminho das teclas.
        for (SlotJogador slot : SlotJogador.values()) {
            if (soloConduz(slot)) {
                // 5P: RS (Contextual ou Classico) no chao = tecnica de solo (camada 1).
                // 5P.2: LT/LB/LT+LB = L2/L1/L1+L2 (familias do solo e espacos de finalizacao)
                dispositivos.consumirComandoContextual(slot)
                        .ifPresent(p -> pedirSolo(slot, p.direcao(), familiaDaCamada(p.camada())));
                dispositivos.consumirPegadaClassica(slot).ifPresent(d -> pedirSolo(slot, d, false));
                continue;
            }
            dispositivos.consumirComandoContextual(slot).ifPresent(p -> {
                if (solo != null && p.camada() != CamadaComando.UM && !(parceiroPassivo(slot))) {
                    pedirSolo(slot, p.direcao(), familiaDaCamada(p.camada()));   // 5P.2: L1 guardas / L2 quedas
                } else if (p.camada() == CamadaComando.UM && aplicaPegadaEstabelecida(slot)
                        && direcaoDaPegadaNoMotor(p.direcao())) {
                    pedirSolo(slot, p.direcao(), com.irmaosmiau.posicional.TecnicaSolo.Familia.RS);                 // 5P.2: tiro / derrubada
                } else {
                    executarComandoContextual(slot, p);
                }
            });
            // Entrega 5L: no Classico o RS so escolhe a pegada, e so com o lutador livre.
            dispositivos.consumirPegadaClassica(slot).ifPresent(d -> executarPegadaClassica(slot, d));
        }
        enviarPedidosSolo();                                      // 5P
    }

        // =====================================================
    // ENTREGA 5O: FAMILIAS DE TECNICAS (DEFESAS, QUEDAS, ACROBACIAS)
    // =====================================================
    /** Distancia minima entre os dois quando uma acrobacia avanca contra o adversario (px). */
    static final int DISTANCIA_MINIMA_ACROBACIA = 50;

    private int xAntesDoTick1;
    private int xAntesDoTick2;
    private final java.util.Map<SlotJogador, com.irmaosmiau.controles.FamiliaTecnica> familiaAtual =
            new java.util.EnumMap<>(SlotJogador.class);
    private final java.util.Map<SlotJogador, String> ultimoMotivoFamilia = new java.util.EnumMap<>(SlotJogador.class);

    /** Familia que o slot esta escolhendo agora (HUD, F3 e testes). */
    com.irmaosmiau.controles.FamiliaTecnica familiaDo(SlotJogador slot) {
        return familiaAtual.getOrDefault(slot, com.irmaosmiau.controles.FamiliaTecnica.PEGADAS);
    }

    /** Ultima recusa de uma tecnica de familia (motivo explicito), para o F3. */
    String ultimoMotivoFamilia(SlotJogador slot) {
        return ultimoMotivoFamilia.getOrDefault(slot, "-");
    }

    private static com.irmaosmiau.controles.FamiliaTecnica familiaPelosModificadores(InputHandler in, SlotJogador s) {
        return com.irmaosmiau.controles.FamiliaTecnica.dosModificadores(
                in.isAtiva(s, AcaoJogador.MODIFICADOR_DEFESA), in.isAtiva(s, AcaoJogador.MODIFICADOR_PROJECAO));
    }

    /**
     * 5O.1: RT (modificador de defesas; teclado 3 / P) SEM direcao de tecnica =
     * respirar (a mesma mecanica de sempre, sustentada). Com uma direcao, a defesa
     * tem prioridade: guarda ativa (ou saindo) e sprawl nunca respiram, entao nao
     * ha recuperacao gratuita durante a defesa.
     */
    boolean respirandoPeloModificador(SlotJogador slot, IrmaoMiau l) {
        if (solo != null && solo.sofrendoEntrada(lado(slot))) {
            return false;                                    // 5P.1: na entrada do outro o RT e base, nao respiracao
        }
        if (!input.isAtiva(slot, AcaoJogador.MODIFICADOR_DEFESA) || l.getDefesa().isAtiva()
                || l.isFazendoSprawl() || dispositivos.direcaoFamiliaGamepad(slot) != null) {
            return false;
        }
        for (AcaoJogador a : ACOES_PEGADA) {
            if (input.isAtiva(slot, a)) {
                return false;
            }
        }
        return true;
    }

    // =====================================================
    // ENTREGA 5P: QUEDAS E COMBATE DE SOLO (MotorSolo)
    // =====================================================
    private com.irmaosmiau.posicional.MotorSolo solo;
    private final java.util.Map<SlotJogador, Boolean> rtAnterior = new java.util.EnumMap<>(SlotJogador.class);
    /** 5P.1: aviso discreto no topo (resultados confirmados do MotorSolo). */
    private final com.irmaosmiau.hud.AvisosCombate avisos = new com.irmaosmiau.hud.AvisosCombate();

    /** Aviso no topo agora (testes). */
    String avisoAtual() {
        return avisos.getAtual();
    }
    private final java.util.Map<com.irmaosmiau.posicional.Lado, com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo>
            pedidosSolo = new java.util.EnumMap<>(com.irmaosmiau.posicional.Lado.class);

    /** Motor de solo da partida (dois Miaus). Novo a cada luta e a cada F5 (nenhum vinculo sobra). */
    private void criarMotorSolo() {
        if (solo != null) {
            solo.reiniciar();
        }
        solo = sistemaCombate == null ? null
                : new com.irmaosmiau.posicional.MotorSolo(jogadorBranco, jogadorPreto, sistemaCombate,
                        LIMITE_ESQUERDO_ARENA, LIMITE_DIREITO_ARENA);
        animador.setMotorSolo(solo);
    }

    /** Acesso para testes e HUD. */
    com.irmaosmiau.posicional.MotorSolo getMotorSolo() {
        return solo;
    }

    private static com.irmaosmiau.posicional.Lado lado(SlotJogador s) {
        return s == SlotJogador.JOGADOR_1 ? com.irmaosmiau.posicional.Lado.A : com.irmaosmiau.posicional.Lado.B;
    }

    /** O lutador esta ligado numa posicao de solo (nao anda, nao rola, nao muda de postura sozinho). */
    private boolean presoNoSolo(IrmaoMiau l) {
        return solo != null && (solo.conectado(l) || comprometidoNaEntrada(l));
    }

    /**
     * 5P: quem esta no meio da PROPRIA entrada (queda, pernas, empurrao, puxar
     * guarda) nao anda nem arrasta: o LS vira so a intencao de acompanhar (e
     * andar com a pegada nao a rompe no meio da tecnica).
     */
    private boolean comprometidoNaEntrada(IrmaoMiau l) {
        return solo.getTentativa().map(t -> solo.lutador(t.iniciador()) == l
                && !t.tecnica().origens().stream().anyMatch(com.irmaosmiau.posicional.Posicao::isNoChao)).orElse(false);
    }

    /** O motor de solo recebe os comandos de tecnica deste slot agora. */
    private boolean soloConduz(SlotJogador slot) {
        return solo != null && !(parceiroPassivo(slot))
                && solo.conduzComandos(slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto);
    }

    private void pedirSolo(SlotJogador slot, com.irmaosmiau.controles.DirecaoTecnica d, boolean explosivo) {
        pedirSolo(slot, d, explosivo ? com.irmaosmiau.posicional.TecnicaSolo.Familia.R1 : com.irmaosmiau.posicional.TecnicaSolo.Familia.RS);
    }

    /** 5P.2: comando com a FAMILIA (RS, R1, L1, L2, L1+L2) para o MotorSolo. */
    private void pedirSolo(SlotJogador slot, com.irmaosmiau.controles.DirecaoTecnica d, com.irmaosmiau.posicional.TecnicaSolo.Familia familia) {
        if (solo == null || d == null || (parceiroPassivo(slot))) {
            return;
        }
        pedidosSolo.putIfAbsent(lado(slot), new com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo(
                com.irmaosmiau.posicional.TecnicaSolo.Direcao.valueOf(d.name()), familia));
    }

    /**
     * 5P.2: camada do Contextual -> familia (nomes PlayStation): LT (L2) = DOIS,
     * LB (L1) = TRES, LT + LB (L1 + L2) = QUATRO. Os dois juntos SEMPRE valem a
     * familia combinada (nunca a de um so).
     */
    static com.irmaosmiau.posicional.TecnicaSolo.Familia familiaDaCamada(CamadaComando c) {
        return switch (c) {
            case UM -> com.irmaosmiau.posicional.TecnicaSolo.Familia.RS;
            case DOIS -> com.irmaosmiau.posicional.TecnicaSolo.Familia.L2;
            case TRES -> com.irmaosmiau.posicional.TecnicaSolo.Familia.L1;
            case QUATRO -> com.irmaosmiau.posicional.TecnicaSolo.Familia.L1_L2;
        };
    }

    /** 5P.2: o slot aplica uma pegada ESTABELECIDA (em pe): RS <- e v vao para o motor (tiro / derrubada). */
    private boolean aplicaPegadaEstabelecida(SlotJogador slot) {
        IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
        return solo != null && sistemaCombate != null && sistemaCombate.isInteracaoEstabelecida()
                && sistemaCombate.getAplicadorInteracao() == ator && !solo.getPosicao().isNoChao();
    }

    private static boolean direcaoDaPegadaNoMotor(com.irmaosmiau.controles.DirecaoTecnica d) {
        return d == com.irmaosmiau.controles.DirecaoTecnica.ESQUERDA || d == com.irmaosmiau.controles.DirecaoTecnica.BAIXO;
    }

    /** Os comandos dos dois do MESMO tick vao juntos (o motor decide a prioridade). */
    private void enviarPedidosSolo() {
        if (solo == null || pedidosSolo.isEmpty()) {
            return;
        }
        java.util.Map<com.irmaosmiau.posicional.Lado, com.irmaosmiau.posicional.MotorSolo.Resposta> r =
                solo.comandos(new java.util.EnumMap<>(pedidosSolo));
        pedidosSolo.clear();
        r.forEach((l, resp) -> {
            if (resp.resultado() == com.irmaosmiau.posicional.MotorSolo.Resultado.CONTRA) {
                avisos.adicionar(solo.consumirMensagens().stream().reduce((a, b) -> b).orElse(null));
            }
            String sigla = l == com.irmaosmiau.posicional.Lado.A ? "J1" : "J2";
            String nome = resp.tecnica() == null ? "" : resp.tecnica().nome().toUpperCase(java.util.Locale.ROOT) + " · ";
            String txt = switch (resp.resultado()) {
                case INICIADA -> nome + "ENTRANDO";
                case CONTRA -> nome + "CONTRA!";
                case CONCLUIDA -> nome.isEmpty() ? resp.motivo() : nome + "OK";
                case ABANDONADA -> "ABANDONOU A TÉCNICA";
                case RECUSADA -> nome + resp.motivo();
            };
            ultimoMotivoFamilia.put(l == com.irmaosmiau.posicional.Lado.A ? SlotJogador.JOGADOR_1
                    : SlotJogador.JOGADOR_2, resp.aceito() ? "-" : resp.motivo());
            feedback.mostrarInfo(sigla + ": " + txt, tick);
        });
    }

    private com.irmaosmiau.posicional.MotorSolo.Intencao intencao(SlotJogador s) {
        if (parceiroPassivo(s)) {
            return com.irmaosmiau.posicional.MotorSolo.Intencao.NENHUMA;
        }
        int dx = (input.isAtiva(s, AcaoJogador.MOVER_DIREITA) ? 1 : 0)
                - (input.isAtiva(s, AcaoJogador.MOVER_ESQUERDA) ? 1 : 0);
        int dy = (input.isAtiva(s, AcaoJogador.MOVER_BAIXO) ? 1 : 0)
                - (input.isAtiva(s, AcaoJogador.MOVER_CIMA) ? 1 : 0);
        boolean rt = input.isAtiva(s, AcaoJogador.MODIFICADOR_DEFESA);
        boolean apertou = rt && !Boolean.TRUE.equals(rtAnterior.get(s));
        rtAnterior.put(s, rt);
        // 5P.1: a base e o RT APERTADO (reacao); segurado de antes e so respirar
        return new com.irmaosmiau.posicional.MotorSolo.Intencao(dx, dy, apertou);
    }

    /** Um tick do motor de solo (depois das entradas): janelas, quedas, encaixe e orientacao. */
    private void atualizarSolo() {
        if (solo == null) {
            return;
        }
        com.irmaosmiau.posicional.MotorSolo.Intencao ia = intencao(SlotJogador.JOGADOR_1);
        com.irmaosmiau.posicional.MotorSolo.Intencao ib = intencao(SlotJogador.JOGADOR_2);
        com.irmaosmiau.posicional.MotorSolo.Evento antes = solo.getUltimoEvento();
        solo.informarIntencoes(ia, ib);
        solo.atualizar(ia, ib);
        for (String m : solo.consumirMensagens()) {
            avisos.adicionar(m);                             // 5P.1: so resultados confirmados
        }
        for (IrmaoMiau l : List.of(jogadorBranco, jogadorPreto)) {
            solo.orientacao(l).ifPresent(l::setViradoParaDireita);
            manterNaArena(l);
        }
    }

    /** 5P.2: "LB · GUARDAS", "LT · QUEDAS", "LT+LB · ESPECIAIS", "LB · FINALIZ.*" (botoes do dispositivo). */
    private String nomeFamiliaL(SlotJogador slot, com.irmaosmiau.posicional.TecnicaSolo.Familia fam, com.irmaosmiau.posicional.EstadoPosicional e,
            java.util.Map<com.irmaosmiau.controles.DirecaoTecnica, CelulaContextual> m) {
        String botoes = nomeCamada(slot, fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.L1 ? CamadaComando.TRES
                : fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.L2 ? CamadaComando.DOIS : CamadaComando.QUATRO).replace(" + RS", "");
        boolean soReservadas = !m.isEmpty() && m.values().stream().noneMatch(CelulaContextual::isExecutavel);
        // 5Q: finalizacoes reais (ids "tecnica@posicao") ou entradas na perna
        boolean ataques = m.values().stream().anyMatch(c -> (c.idTransicao() != null && c.idTransicao().contains("@"))
                || "→ PERNA".equals(c.rotulo()));
        String o;
        if (ataques) {
            o = fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.L1 ? "CHAVES"
                    : fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.L2 ? "ESTRANG." : "PERNAS";
        } else if (soReservadas) {
            o = "FINALIZ.";
        } else if (!e.getPosicao().isNoChao()) {
            o = fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.L1 ? "GUARDAS" : fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.L2 ? "QUEDAS" : "ESPECIAIS";
        } else {
            o = fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.L1 ? "GUARDAS" : fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.L2 ? "CONTROLES" : "ESPECIAIS";
        }
        return botoes + " · " + o;
    }

    /** 5P.1: nome curto da posicao para a cruz do HUD (o painel tem 138 px). */
    static String nomeCurto(com.irmaosmiau.posicional.Posicao p) {
        return switch (p) {
            case EM_PE_SEPARADOS -> "EM PÉ";
            case EM_PE_PEGADA -> "PEGADA";
            case EM_PE_CONTRA_SOLO -> "SOLO LIVRE";
            case SOLO_DISPUTADO -> "SOLO";
            case GUARDA -> "G. FECHADA";
            case GUARDA_ABERTA -> "G. ABERTA";
            case MEIA_GUARDA -> "MEIA";
            case CONTROLE_LATERAL -> "100 KG";
            case JOELHO_BARRIGA -> "JOELHO";
            case MONTADA -> "MONTADA";
            case COSTAS -> "COSTAS";
            case GUARDA_BORBOLETA -> "BORBOLETA";
            case DE_LA_RIVA -> "DLR";
            case GUARDA_LACO -> "LAÇO";
            case GUARDA_ARANHA -> "ARANHA";
            case SINGLE_LEG_X -> "SLX";
            case ASHI_GARAMI -> "ASHI";
        };
    }

    private String papelCurto(com.irmaosmiau.posicional.Lado l) {
        com.irmaosmiau.posicional.EstadoPosicional e = solo.getEstado();
        if (!e.getPosicao().isNoChao()) {
            return e.getControlador().map(c -> c == l ? "APLICA" : "AGARRADO").orElse("LIVRE");
        }
        if (e.getPosicao() == com.irmaosmiau.posicional.Posicao.EM_PE_CONTRA_SOLO) {
            return e.getPorCima().map(c -> c == l ? "EM PÉ" : "NO CHÃO").orElse("");
        }
        return e.getPorCima().map(c -> c == l ? "CIMA" : "BAIXO").orElse("");
    }

    /** 5P: cruz de comandos do solo (o que cada direcao faz AGORA, deste lado). */
    PainelDpadContextual.Quadro quadroSolo(SlotJogador slot, boolean explosivo) {
        return quadroSolo(slot, explosivo ? com.irmaosmiau.posicional.TecnicaSolo.Familia.R1 : com.irmaosmiau.posicional.TecnicaSolo.Familia.RS);
    }

    /** 5P.2: cruz da familia no solo (ou em pe com o motor): tecnicas reais e espacos reservados. */
    PainelDpadContextual.Quadro quadroSolo(SlotJogador slot, com.irmaosmiau.posicional.TecnicaSolo.Familia fam) {
        java.util.Optional<com.irmaosmiau.posicional.MotorSolo.Ataque> atk = solo.getAtaque();
        if (atk.isPresent()) {
            return quadroFinalizacao(slot, atk.get());                      // 5Q: disputa tem prioridade no HUD
        }
        boolean explosivo = fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.R1;
        com.irmaosmiau.posicional.Lado l = lado(slot);
        com.irmaosmiau.posicional.EstadoPosicional e = solo.getEstado();
        java.util.Map<com.irmaosmiau.controles.DirecaoTecnica, CelulaContextual> m =
                new java.util.EnumMap<>(com.irmaosmiau.controles.DirecaoTecnica.class);
        for (com.irmaosmiau.controles.DirecaoTecnica d : com.irmaosmiau.controles.DirecaoTecnica.values()) {
            celulaSolo(l, d, fam).ifPresent(c -> m.put(d, c));
        }
        if (solo.ladoQueCai().filter(x -> x == l).isPresent()) {
            // 5P.1: caindo: a escolha de como receber, com tempo (ATERRISSAGEM_TICKS)
            m.clear();
            m.put(com.irmaosmiau.controles.DirecaoTecnica.CIMA, new CelulaContextual(new PosicaoComando(CamadaComando.UM,
                    com.irmaosmiau.controles.DirecaoTecnica.CIMA), CelulaContextual.Tipo.EXECUTAVEL, "FECHAR GUARDA",
                    null, null, true));
            for (com.irmaosmiau.controles.DirecaoTecnica d : new com.irmaosmiau.controles.DirecaoTecnica[] {
                com.irmaosmiau.controles.DirecaoTecnica.ESQUERDA, com.irmaosmiau.controles.DirecaoTecnica.DIREITA}) {
                m.put(d, new CelulaContextual(new PosicaoComando(CamadaComando.UM, d), CelulaContextual.Tipo.EXECUTAVEL,
                        "MEIA", null, null, true));
            }
            return new PainelDpadContextual.Quadro(slot.getSigla(), CamadaComando.UM, "CAINDO: COMO RECEBER?", m);
        }
        String nome;
        if (fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.RS || fam == com.irmaosmiau.posicional.TecnicaSolo.Familia.R1) {
            nome = (explosivo ? (e.getPosicao().isNoChao() ? "RB · " : "QUEDAS (RB) · ") : "")
                    + nomeCurto(e.getPosicao()) + " · " + papelCurto(l)
                    + (e.getPosicao().isNoChao() && !explosivo ? " · RT AR" : "");
        } else {
            nome = nomeFamiliaL(slot, fam, e, m) + " · " + nomeCurto(e.getPosicao());
        }
        CamadaComando camada = switch (fam) {
            case L2 -> CamadaComando.DOIS;
            case L1 -> CamadaComando.TRES;
            case L1_L2 -> CamadaComando.QUATRO;
            default -> CamadaComando.UM;
        };
        return new PainelDpadContextual.Quadro(slot.getSigla(), camada, nome, m);
    }

    /** As quatro teclas/botoes de pegada como direcoes da tecnica (mesma ordem do RS). */
    private static com.irmaosmiau.controles.DirecaoTecnica direcaoDaAcao(AcaoJogador a) {
        return switch (a) {
            case INICIAR_PEGADA -> com.irmaosmiau.controles.DirecaoTecnica.CIMA;
            case PEGADA_2 -> com.irmaosmiau.controles.DirecaoTecnica.DIREITA;
            case PEGADA_3 -> com.irmaosmiau.controles.DirecaoTecnica.BAIXO;
            case PEGADA_4 -> com.irmaosmiau.controles.DirecaoTecnica.ESQUERDA;
            default -> null;
        };
    }

    private static final AcaoJogador[] ACOES_PEGADA = {
        AcaoJogador.INICIAR_PEGADA, AcaoJogador.PEGADA_2, AcaoJogador.PEGADA_3, AcaoJogador.PEGADA_4};

    /**
     * Um tick das familias para os dois slots. Prioridade: RT (defesas) > RB
     * (quedas) > nenhum (pegadas). Com um modificador, as teclas/botoes de
     * pegada viram a direcao da familia e NAO pegam. Guardas: sustentadas
     * enquanto a direcao estiver segurada. Sprawl e quedas: uma vez por
     * inclinacao/toque. Acrobacia: uma vez quando Y + direcao comecam juntos.
     */
    private void atualizarFamilias(java.util.Map<SlotJogador, java.util.EnumSet<AcaoJogador>> tocadas) {
        for (IrmaoMiau l : List.of(jogadorBranco, jogadorPreto)) {
            manterDefesaCoerente(l);
        }
        for (SlotJogador slot : SlotJogador.values()) {
            IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
            IrmaoMiau outro = ator == jogadorBranco ? jogadorPreto : jogadorBranco;
            if (parceiroPassivo(slot)) {
                dispositivos.consumirEscolhaFamilia(slot);
                ator.getDefesa().soltar();
                continue;                                 // parceiro passivo
            }
            com.irmaosmiau.controles.FamiliaTecnica fam = familiaPelosModificadores(input, slot);
            familiaAtual.put(slot, fam);
            com.irmaosmiau.controles.DirecaoTecnica teclaSegurada = null;
            com.irmaosmiau.controles.DirecaoTecnica teclaNova = null;
            for (AcaoJogador a : ACOES_PEGADA) {
                if (input.isAtiva(slot, a) && teclaSegurada == null) {
                    teclaSegurada = direcaoDaAcao(a);
                }
                if (tocadas.get(slot).contains(a) && teclaNova == null) {
                    teclaNova = direcaoDaAcao(a);
                }
            }
            java.util.Optional<com.irmaosmiau.controles.FamiliaTecnica.Escolha> pad =
                    dispositivos.consumirEscolhaFamilia(slot);
            if (fam == com.irmaosmiau.controles.FamiliaTecnica.PEGADAS) {
                ator.getDefesa().soltar();
                continue;
            }
            for (AcaoJogador a : ACOES_PEGADA) {
                tocadas.get(slot).remove(a);              // com RT/RB a tecla de pegada nao pega
            }
            com.irmaosmiau.controles.DirecaoTecnica segurada = dispositivos.direcaoFamiliaGamepad(slot);
            if (segurada == null) {
                segurada = teclaSegurada;
            }
            com.irmaosmiau.controles.DirecaoTecnica nova = pad.filter(e -> e.familia() == fam)
                    .map(com.irmaosmiau.controles.FamiliaTecnica.Escolha::direcao).orElse(teclaNova);
            if (fam == com.irmaosmiau.controles.FamiliaTecnica.DEFESAS) {
                if (presoNoSolo(ator)) {
                    ator.getDefesa().soltar();                     // 5P: ligado no solo nao ha guarda em pe
                } else {
                    atualizarDefesas(slot, ator, outro, segurada, nova);
                }
            } else {
                ator.getDefesa().soltar();
                if (nova != null && solo != null) {
                    pedirSolo(slot, nova, true);                   // 5P: RB + RS = quedas / explosivo no chao
                } else if (nova != null) {
                    String m = com.irmaosmiau.combate.ResolvedorFamilias.resolver(fam, nova, ator, outro,
                            sistemaCombate).motivo();
                    ultimoMotivoFamilia.put(slot, m);
                    feedback.mostrarInfo(slot.getSigla() + ": QUEDAS E TRANSIÇÕES · " + m, tick);
                }
            }
        }
    }

    private void atualizarDefesas(SlotJogador slot, IrmaoMiau ator, IrmaoMiau outro,
            com.irmaosmiau.controles.DirecaoTecnica segurada, com.irmaosmiau.controles.DirecaoTecnica nova) {
        com.irmaosmiau.entities.DefesaCorporal.Tipo g = com.irmaosmiau.combate.ResolvedorFamilias.guardaDa(segurada);
        if (g != null) {
            com.irmaosmiau.combate.ResolvedorFamilias.Resolucao r = com.irmaosmiau.combate.ResolvedorFamilias
                    .resolver(com.irmaosmiau.controles.FamiliaTecnica.DEFESAS, segurada, ator, outro, sistemaCombate);
            if (r.disponivel()) {
                manterGuarda(ator, g);
            } else {
                ator.getDefesa().soltar();
                if (nova != null) {
                    ultimoMotivoFamilia.put(slot, r.motivo());
                    feedback.mostrarInfo(slot.getSigla() + ": " + r.rotulo() + " · " + r.motivo(), tick);
                }
            }
        } else {
            ator.getDefesa().soltar();
        }
        if (nova == com.irmaosmiau.controles.DirecaoTecnica.BAIXO) {
            com.irmaosmiau.combate.ResolvedorFamilias.Resolucao r = com.irmaosmiau.combate.ResolvedorFamilias
                    .resolver(com.irmaosmiau.controles.FamiliaTecnica.DEFESAS, nova, ator, outro, sistemaCombate);
            if (r.disponivel()) {
                ator.getDefesa().interromper();
                ator.iniciarSprawl();
                ultimoMotivoFamilia.put(slot, "-");
            } else {
                ultimoMotivoFamilia.put(slot, r.motivo());
                feedback.mostrarInfo(slot.getSigla() + ": SPRAWL · " + r.motivo(), tick);
            }
        }
    }

    /** Guarda mantida neste tick: cobra a entrada/troca uma vez; ativa, o folego nao recupera. */
    private static void manterGuarda(IrmaoMiau l, com.irmaosmiau.entities.DefesaCorporal.Tipo g) {
        boolean trocando = l.getDefesa().isSegurada() && l.getDefesa().getTipo() != g;
        if (l.getDefesa().manter(g)) {
            l.gastarFolegoEsforco(trocando ? com.irmaosmiau.entities.CustosFolego.GUARDA_TROCA
                    : com.irmaosmiau.entities.CustosFolego.GUARDA_ENTRADA, "guarda");
        } else {
            l.gastarFolegoEsforco(0.0, "guarda");
        }
        l.registrarAtividadeDeCombate();                  // guarda nao vira taunt
    }

    /** A guarda some na hora em estados incompativeis (agarrado, rolando, deitado...): nada fica preso. */
    private void manterDefesaCoerente(IrmaoMiau l) {
        boolean naPegada = sistemaCombate != null && sistemaCombate.estaEmInteracao()
                && (sistemaCombate.getAplicadorInteracao() == l || sistemaCombate.getReceptorInteracao() == l);
        if (l.getDefesa().isAtiva() && (naPegada || l.isRolando() || l.isFazendoSprawl() || l.isPulando()
                || l.isEmAcrobacia() || l.isDeitado() || l.getTransicaoAtiva() != null)) {
            l.getDefesa().interromper();
        }
    }

    /**
     * 5O.1: R3 (acao ROLAR) + direcao = mobilidade CONTEXTUAL pela postura.
     * - em pe: acrobacia pela direcao relativa ao corpo (para onde olha =
     *   mortal para a frente; contraria = mortal para tras; fundo/frente da
     *   tela = estrela). Diagonal: vale o componente horizontal (mortal),
     *   regra deterministica de TipoAcrobacia.pelaDirecao;
     * - de joelhos: rolamento tecnico (velocidade de sempre);
     * - deitado: rolamento tecnico mais lento (MULTIPLICADOR_ROLAMENTO_DEITADO).
     * O rolamento usa a versao do F7 (ORIGINAL ou EXPERIMENTAL 5O).
     */
    void mobilidadeContextual(SlotJogador slot, IrmaoMiau l, Direcao d) {
        if (d == null) {
            return;
        }
        if (l.isAjoelhado() || l.isDeitado()) {
            l.iniciarRolamento(d);
            return;
        }
        int dx = d.name().contains("DIREITA") ? 1 : d.name().contains("ESQUERDA") ? -1 : 0;
        int dy = d.name().startsWith("CIMA") ? -1 : d.name().startsWith("BAIXO") ? 1 : 0;
        com.irmaosmiau.entities.TipoAcrobacia t = com.irmaosmiau.entities.TipoAcrobacia.pelaDirecao(dx, dy,
                l.isViradoParaDireita());
        String motivo = sistemaCombate == null ? null
                : com.irmaosmiau.combate.ResolvedorFamilias.motivoComumPublico(l, sistemaCombate);
        if (motivo == null) {
            motivo = l.motivoSemAcrobacia(t);
        }
        if (motivo == null && l.iniciarAcrobacia(t)) {
            ultimoMotivoFamilia.put(slot, "-");
        } else {
            ultimoMotivoFamilia.put(slot, motivo);
            feedback.mostrarInfo(slot.getSigla() + ": " + t.getRotulo() + " · " + motivo, tick);
        }
    }

    /**
     * 5O: a acrobacia nao passa por dentro do adversario (linhas proximas):
     * para a DISTANCIA_MINIMA_ACROBACIA antes dele e o avanco acaba ali.
     */
    static void impedirAtravessarNaAcrobacia(IrmaoMiau a, int xAntesA, IrmaoMiau b, int xAntesB) {
        if (a == null || b == null || !a.isEmAcrobacia() || Math.abs(a.getLinhaAtual() - b.getLinhaAtual()) > 1) {
            return;
        }
        int lado = Integer.signum(xAntesA - xAntesB);
        if (lado == 0) {
            return;
        }
        int dist = (a.getX() - b.getX()) * lado;              // positivo = do lado de antes
        if (dist < DISTANCIA_MINIMA_ACROBACIA && Math.abs(a.getX() - xAntesA) > 0
                && Integer.signum(a.getX() - xAntesA) == -lado) {
            a.encostarNaBorda(b.getX() + lado * DISTANCIA_MINIMA_ACROBACIA - a.getX());
            a.pararAvancoAcrobacia();
        }
    }


    /** Entrega 5L: o lutador e o receptor de uma pegada ja estabelecida (fica preso). */
    private boolean receptorPreso(IrmaoMiau l) {
        return sistemaCombate != null && sistemaCombate.isInteracaoEstabelecida()
                && sistemaCombate.getReceptorInteracao() == l;
    }

    /** Entrega 5L: forcar a saida vira resistencia (firmeza cai mais rapido), nunca fuga. */
    private void resistirNaPegada(SlotJogador slot, IrmaoMiau receptor) {
        boolean contou = sistemaCombate.resistirPegada(receptor);
        if (contou && sistemaCombate.getTicksResistindo() == 0) {
            feedback.mostrarInfo(slot.getSigla() + ": PRESO NA PEGADA · use ROMPER", tick);
        }
    }

    /** 5I: desequilibrio e rompimento no mesmo toque do slot: fica so a do papel real. */
    private void resolverBotaoCompartilhado(SlotJogador slot, java.util.Set<AcaoJogador> doSlot) {
        if (!doSlot.contains(AcaoJogador.TENTAR_DESEQUILIBRIO) || !doSlot.contains(AcaoJogador.ROMPIMENTO_TECNICO)) {
            return;
        }
        IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
        if (sistemaCombate.getReceptorInteracao() == ator) {
            doSlot.remove(AcaoJogador.TENTAR_DESEQUILIBRIO);
        } else {
            doSlot.remove(AcaoJogador.ROMPIMENTO_TECNICO);
        }
    }

    /** Uma das 12 posicoes: executa a acao existente, ou avisa que esta vazia/reservada. */
    private void executarComandoContextual(SlotJogador slot, PosicaoComando posicao) {
        if (parceiroPassivo(slot)) {
            return;                                     // parceiro passivo
        }
        IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
        IrmaoMiau outro = ator == jogadorBranco ? jogadorPreto : jogadorBranco;
        java.util.Optional<CelulaContextual> c = RepertorioContextual.celula(
                RepertorioContextual.paraCombate(sistemaCombate, slot, ator, outro), posicao);
        if (c.isEmpty()) {
            feedback.mostrarInfo(slot.getSigla() + ": nada na camada " + posicao.camada().getNumero()
                    + (posicao.camada() == CamadaComando.QUATRO ? " (avançada)" : "")
                    + " " + posicao.direcao().getSeta(), tick);
        } else if (c.get().isExecutavel()) {
            executarAcaoCombate(slot, c.get().acao());
        } else {
            feedback.mostrarInfo(c.get().rotulo() + ": ainda não disponível no jogo", tick);
        }
    }

    /** Entrega 5L: RS no Classico = as 4 pegadas (livre); com pegada, nada (os botoes do Classico agem). */
    private void executarPegadaClassica(SlotJogador slot, com.irmaosmiau.controles.DirecaoTecnica d) {
        if (parceiroPassivo(slot)) {
            return;
        }
        IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
        if (com.irmaosmiau.controles.PapelCombate.de(sistemaCombate, ator) != com.irmaosmiau.controles.PapelCombate.LIVRE) {
            return;
        }
        RepertorioContextual.celula(RepertorioContextual.paraCombate(sistemaCombate, slot, ator,
                ator == jogadorBranco ? jogadorPreto : jogadorBranco),
                new PosicaoComando(CamadaComando.UM, d))
                .filter(CelulaContextual::isExecutavel)
                .ifPresent(c -> executarAcaoCombate(slot, c.acao()));
    }

    /** "D-pad", "LT + D-pad", "LT + LB + D-pad" com os botoes configurados. */
    String nomeCamada(SlotJogador slot, CamadaComando camada) {
        String m2 = mapaGamepad.botao(slot, ItemGamepad.de(ComandoGamepad.MODIFICADOR_CAMADA_2))
                .map(b -> b.getRotulo().split("/")[0]).orElse("?");
        String m3 = mapaGamepad.botao(slot, ItemGamepad.de(ComandoGamepad.MODIFICADOR_CAMADA_3))
                .map(b -> b.getRotulo().split("/")[0]).orElse("?");
        // 5H: RS = analogico direito (o D-pad continua valendo como alternativa).
        return switch (camada) {
            case UM -> "RS";
            case DOIS -> m2 + " + RS";
            case TRES -> m3 + " + RS";
            case QUATRO -> m2 + " + " + m3 + " + RS";
        };
    }

    /**
     * Entrega 5F: cruz do D-pad do slot (null se o slot nao usa o contextual agora).
     * Entrega 5L.1: o J2 no TECLADO tambem ve a cruz, com o mesmo desenho, setas e
     * "RS" do J1 (os comandos sao os mesmos por papel). O J1 e o Classico nao mudam.
     */
    PainelDpadContextual.Quadro montarQuadroDpad(SlotJogador slot) {
        // 5O: com RT/RB (ou 3/4, INSERT/HOME) segurado, a cruz mostra a FAMILIA (qualquer esquema).
        if (soloConduz(slot)) {
            return quadroSolo(slot, familiaHud(slot));                             // 5P / 5P.2
        }
        if (solo != null && !(parceiroPassivo(slot))
                && familiaDo(slot) == com.irmaosmiau.controles.FamiliaTecnica.PROJECOES) {
            return quadroSolo(slot, com.irmaosmiau.posicional.TecnicaSolo.Familia.R1);                               // 5P: quedas em pe
        }
        if (solo != null && !(parceiroPassivo(slot))
                && familiaHud(slot) != com.irmaosmiau.posicional.TecnicaSolo.Familia.RS && familiaHud(slot) != com.irmaosmiau.posicional.TecnicaSolo.Familia.R1) {
            return quadroSolo(slot, familiaHud(slot));                             // 5P.2: L1 / L2 / L1+L2 em pe
        }
        if (sistemaCombate != null && !(parceiroPassivo(slot))
                && familiaDo(slot) != com.irmaosmiau.controles.FamiliaTecnica.PEGADAS) {
            return quadroDaFamilia(slot, familiaDo(slot));
        }
        boolean j2NoTeclado = slot == SlotJogador.JOGADOR_2 && !dispositivos.usaGamepadAgora(slot);
        if (sistemaCombate == null || !(dispositivos.usaContextualAgora(slot) || j2NoTeclado)
                || (parceiroPassivo(slot))) {
            return null;
        }
        IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
        IrmaoMiau outro = ator == jogadorBranco ? jogadorPreto : jogadorBranco;
        CamadaComando camada = dispositivos.camadaAtiva(slot);
        // 5O: alguem deitado e o lutador livre: as pegadas em pe nao valem; o solo ainda esta em preparacao.
        if (solo == null && camada == CamadaComando.UM && com.irmaosmiau.controles.PapelCombate.de(sistemaCombate, ator)
                == com.irmaosmiau.controles.PapelCombate.LIVRE && (ator.isDeitado() || outro.isDeitado())) {
            return quadroDoSolo(slot, ator);                 // so sem motor de solo (modo sem combate)
        }
        PainelDpadContextual.Quadro q = PainelDpadContextual.montar(slot.getSigla(), camada, nomeCamada(slot, camada),
                RepertorioContextual.paraCombate(sistemaCombate, slot, ator, outro));
        if (camada == CamadaComando.UM && aplicaPegadaEstabelecida(slot)) {
            // 5P.2: aplicando a pegada: RS <- tiro de guarda, RS v derrubada forcada (do motor)
            java.util.Map<com.irmaosmiau.controles.DirecaoTecnica, CelulaContextual> m = new java.util.EnumMap<>(q.celulas());
            for (com.irmaosmiau.controles.DirecaoTecnica d : new com.irmaosmiau.controles.DirecaoTecnica[] {com.irmaosmiau.controles.DirecaoTecnica.ESQUERDA, com.irmaosmiau.controles.DirecaoTecnica.BAIXO}) {
                celulaSolo(lado(slot), d, com.irmaosmiau.posicional.TecnicaSolo.Familia.RS).ifPresent(c -> m.put(d, c));
            }
            q = new PainelDpadContextual.Quadro(q.sigla(), q.camada(), q.nomeCamada(), m);
        }
        return q;
    }

    /** 5P.2: familia que o HUD mostra (R1 pelo RB; L1/L2/L1+L2 pelas camadas do Contextual). */
    com.irmaosmiau.posicional.TecnicaSolo.Familia familiaHud(SlotJogador slot) {
        if (familiaDo(slot) == com.irmaosmiau.controles.FamiliaTecnica.PROJECOES) {
            return com.irmaosmiau.posicional.TecnicaSolo.Familia.R1;
        }
        return familiaDaCamada(dispositivos.camadaAtiva(slot));
    }

    /** 5P.2: celula de uma direcao: tecnica do motor (executavel) ou espaco de finalizacao (reservado). */
    private java.util.Optional<CelulaContextual> celulaSolo(com.irmaosmiau.posicional.Lado l, com.irmaosmiau.controles.DirecaoTecnica d,
            com.irmaosmiau.posicional.TecnicaSolo.Familia fam) {
        com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo cmd = new com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo(
                com.irmaosmiau.posicional.TecnicaSolo.Direcao.valueOf(d.name()), fam);
        java.util.Optional<com.irmaosmiau.posicional.TecnicaSolo> t = solo.getCatalogo().para(solo.getEstado(), l, cmd);
        if (t.isPresent()) {
            return java.util.Optional.of(new CelulaContextual(new PosicaoComando(CamadaComando.UM, d),
                    CelulaContextual.Tipo.EXECUTAVEL, t.get().rotulo(), null, null, solo.motivoSem(l, t.get()) == null));
        }
        java.util.Optional<com.irmaosmiau.posicional.Finalizacao> fz =
                solo.getCatalogoFinalizacoes().para(solo.getEstado(), l, cmd);
        if (fz.isPresent()) {
            return java.util.Optional.of(celulaFinalizacao(l, d, fz.get()));      // 5Q
        }
        return solo.getFinalizacoes().para(solo.getEstado(), l, cmd)
                .map(e -> new CelulaContextual(new PosicaoComando(CamadaComando.UM, d), CelulaContextual.Tipo.RESERVADA,
                        e.rotuloCurto(), null, e.id(), false));
    }

    /**
     * 5Q: celula de finalizacao. Disponivel = clara; requisito de posicao/controle = cinza;
     * graduacao = cinza com a sigla exigida ("ARMLOCK·Az"); proibida no modo = cinza com "·X".
     */
    private CelulaContextual celulaFinalizacao(com.irmaosmiau.posicional.Lado l, com.irmaosmiau.controles.DirecaoTecnica d,
            com.irmaosmiau.posicional.Finalizacao f) {
        String m = solo.motivoFinalizacao(l, f);
        String rot = f.rotulo();
        if (m != null && m.startsWith("GRADUAÇÃO")) {
            rot += "·" + f.graduacao().getSigla();
        } else if ("PROIBIDA NO MODO".equals(m) || "NÃO APRENDIDA".equals(m)) {
            rot += "·X";
        }
        return new CelulaContextual(new PosicaoComando(CamadaComando.UM, d), CelulaContextual.Tipo.EXECUTAVEL, rot,
                null, f.id(), m == null);
    }

    /** 5Q: cruz da finalizacao em disputa (quem ataca e quem defende veem suas acoes e a pressao). */
    private PainelDpadContextual.Quadro quadroFinalizacao(SlotJogador slot, com.irmaosmiau.posicional.MotorSolo.Ataque a) {
        com.irmaosmiau.posicional.Lado l = lado(slot);
        java.util.Map<com.irmaosmiau.controles.DirecaoTecnica, CelulaContextual> m = new java.util.EnumMap<>(com.irmaosmiau.controles.DirecaoTecnica.class);
        boolean disputa = a.fase() == com.irmaosmiau.posicional.MotorSolo.FaseFinalizacao.DISPUTA;
        java.util.function.BiConsumer<com.irmaosmiau.controles.DirecaoTecnica, String> por = (d, t) -> m.put(d, new CelulaContextual(
                new PosicaoComando(CamadaComando.UM, d), CelulaContextual.Tipo.EXECUTAVEL, t, null, null, true));
        String nome = a.finalizacao().rotulo();
        String titulo;
        if (a.atacante() == l) {
            if (disputa) {
                por.accept(com.irmaosmiau.controles.DirecaoTecnica.CIMA, "APERTAR");
            }
            por.accept(com.irmaosmiau.controles.DirecaoTecnica.BAIXO, "SOLTAR");
            titulo = (disputa ? "ATACANDO " : "ENTRANDO ") + nome;
        } else {
            por.accept(com.irmaosmiau.controles.DirecaoTecnica.BAIXO, a.finalizacao().categoria().getDefesa());
            if (disputa) {
                por.accept(com.irmaosmiau.controles.DirecaoTecnica.CIMA, "RB: ESCAPAR");
            }
            por.accept(com.irmaosmiau.controles.DirecaoTecnica.ESQUERDA, "RB↓ BATER");
            titulo = "DEFENDA: " + nome;
        }
        if (disputa) {
            titulo += " " + Math.round(a.pressao()) + "%";
        }
        return new PainelDpadContextual.Quadro(slot.getSigla(), CamadaComando.UM, titulo, m);
    }

    /** 5O: nome do modificador da familia no dispositivo do slot (RT/RB ou a tecla). */
    private String nomeModificador(SlotJogador slot, AcaoJogador mod) {
        if (dispositivos.usaGamepadAgora(slot)) {
            return mapaGamepad.botao(slot, ItemGamepad.de(mod)).map(b -> b.getRotulo().split("/")[0]).orElse("?");
        }
        if (dispositivos.usaTecladoControleAgora(slot)) {             // 5O.1: P = RT, O/SHIFT = RB
            return mapaGamepad.botao(slot, ItemGamepad.de(mod))
                    .map(b -> com.irmaosmiau.controles.LayoutTecladoControle.teclaDo(slot, b.name())
                            + (b == com.irmaosmiau.dispositivos.BotaoGamepad.RB && mapaGamepad.selectValeComoRb(slot)
                                    ? "/" + com.irmaosmiau.controles.LayoutTecladoControle.teclaDo(slot, "VOLTAR") : ""))
                    .orElse("?");
        }
        return input.getMapa().tecla(slot, mod).map(com.irmaosmiau.controles.TeclaFisica::getRotulo).orElse("?");
    }

    /** 5O: cruz da familia, com o que o motor permite AGORA (o mesmo resolvedor que executa). */
    PainelDpadContextual.Quadro quadroDaFamilia(SlotJogador slot, com.irmaosmiau.controles.FamiliaTecnica fam) {
        IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
        IrmaoMiau outro = ator == jogadorBranco ? jogadorPreto : jogadorBranco;
        java.util.Map<com.irmaosmiau.controles.DirecaoTecnica, CelulaContextual> m =
                new java.util.EnumMap<>(com.irmaosmiau.controles.DirecaoTecnica.class);
        for (com.irmaosmiau.controles.DirecaoTecnica d : com.irmaosmiau.controles.DirecaoTecnica.values()) {
            com.irmaosmiau.combate.ResolvedorFamilias.Resolucao r =
                    com.irmaosmiau.combate.ResolvedorFamilias.resolver(fam, d, ator, outro, sistemaCombate);
            boolean reservada = fam == com.irmaosmiau.controles.FamiliaTecnica.PROJECOES;
            m.put(d, new CelulaContextual(new PosicaoComando(CamadaComando.UM, d),
                    reservada ? CelulaContextual.Tipo.RESERVADA : CelulaContextual.Tipo.EXECUTAVEL,
                    rotuloCurtoFamilia(fam, d), null, null, r.disponivel()));
        }
        String nome = fam == com.irmaosmiau.controles.FamiliaTecnica.DEFESAS
                ? "DEFESAS (" + nomeModificador(slot, AcaoJogador.MODIFICADOR_DEFESA) + ")"
                : "QUEDAS: EM BREVE (" + nomeModificador(slot, AcaoJogador.MODIFICADOR_PROJECAO) + ")";
        return new PainelDpadContextual.Quadro(slot.getSigla(), CamadaComando.UM, nome, m);
    }

    static String rotuloCurtoFamilia(com.irmaosmiau.controles.FamiliaTecnica fam,
            com.irmaosmiau.controles.DirecaoTecnica d) {
        if (fam == com.irmaosmiau.controles.FamiliaTecnica.DEFESAS) {
            return switch (d) {
                case CIMA -> "G. ALTA";
                case ESQUERDA -> "G. MÉDIA";
                case DIREITA -> "G. BAIXA";
                case BAIXO -> "SPRAWL";
            };
        }
        return switch (d) {
            case CIMA, DIREITA -> "QUEDA";
            case BAIXO -> "GUARDA";
            case ESQUERDA -> "TRANSIÇÃO";
        };
    }

    /** 5O: com alguem deitado, as pegadas em pe somem; o que vem para o solo aparece como reservado. */
    PainelDpadContextual.Quadro quadroDoSolo(SlotJogador slot, IrmaoMiau ator) {
        java.util.Map<com.irmaosmiau.controles.DirecaoTecnica, CelulaContextual> m =
                new java.util.EnumMap<>(com.irmaosmiau.controles.DirecaoTecnica.class);
        String[] rot = ator.isDeitado()
                ? new String[] {"GUARDA", "AFASTAR", "LEVANTAR", "PUXAR"}
                : new String[] {"PASSAR", "LATERAL", "JOELHO", "MONTAR"};
        com.irmaosmiau.controles.DirecaoTecnica[] ds = {com.irmaosmiau.controles.DirecaoTecnica.CIMA,
            com.irmaosmiau.controles.DirecaoTecnica.DIREITA, com.irmaosmiau.controles.DirecaoTecnica.BAIXO,
            com.irmaosmiau.controles.DirecaoTecnica.ESQUERDA};
        for (int i = 0; i < 4; i++) {
            m.put(ds[i], new CelulaContextual(new PosicaoComando(CamadaComando.UM, ds[i]),
                    CelulaContextual.Tipo.RESERVADA, rot[i], null, null, false));
        }
        return new PainelDpadContextual.Quadro(slot.getSigla(), CamadaComando.UM, "SOLO: EM BREVE", m);
    }

    /** Uma acao de combate pedida por um slot, encaminhada ao SistemaCombate. */
    private void executarAcaoCombate(SlotJogador slot, AcaoJogador acao) {

        IrmaoMiau ator = slot == SlotJogador.JOGADOR_1 ? jogadorBranco : jogadorPreto;
        IrmaoMiau outro = ator == jogadorBranco ? jogadorPreto : jogadorBranco;

        switch (acao) {
            // Entrega 5L: as quatro pegadas pelo mesmo caminho (o motor decide alcance, custo e evasao).
            case INICIAR_PEGADA, PEGADA_2, PEGADA_3, PEGADA_4 -> {
                String id = AcoesContextuais.idPegada(acao);
                ResultadoAcao resultado = sistemaCombate.tentarAcao(ator, outro, id);
                feedback.registrarTentativa(sistemaCombate, ator, outro, id, resultado, tick);
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
    /** 1/raiz(2): a diagonal da guarda andando percorre a mesma distancia por tick. */
    static final double DIAGONAL = 0.7071067811865476;

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
        // 5O.1: com uma guarda segurada ele anda a MULTIPLICADOR_VELOCIDADE (50%), sem corrida.
        boolean guarda = personagem.getDefesa().isAtiva();
        if (guarda) {
            correndo = false;
            respirando = false;
        }
        double velocidadeAtual;

        // =========================
        // DEFINE A VELOCIDADE
        // =========================
        if (respirando && personagem.isRespirando()) {

            // 5O.2: respirando anda devagar em qualquer postura (velocidade propria da postura).
            velocidadeAtual
                    = personagem.getVelocidadeRespirando();

        } else if (personagem.isDeitado()) {

            velocidadeAtual
                    = personagem.getVelocidadeRastejando();

        } else if (personagem.isAjoelhado()) {

            // 5J: de joelhos anda devagar (andar nao levanta).
            velocidadeAtual
                    = personagem.getVelocidadeAjoelhado();

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
        if (guarda) {
            deslocamento *= com.irmaosmiau.entities.DefesaCorporal.MULTIPLICADOR_VELOCIDADE;
            if ((esquerda ^ direita) && (cima ^ baixo)) {
                deslocamento *= DIAGONAL;                 // diagonal normalizada: sem ganho de velocidade
            }
        }

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
                40,
                300,
                370
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

        // 5P.1: a mesma luta de novo (mesmos Miaus, arena, modo e tempo), sem voltar a selecao.
        reiniciarLuta = new JButton("REINICIAR LUTA");

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
        reiniciarLuta.setFont(fonteBotao);
        reiniciarLuta.addActionListener(e -> reiniciarLuta());
        reiniciarLuta.setVisible(aoTentarNovamente != null);

        controles.addActionListener(
                e -> mostrarAjudaControles(true)
        );

        botaoContinuar = com.irmaosmiau.ui.NavegadorMenus.marcarVoltar(continuar);
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
                reiniciarLuta,
                gbc
        );

        gbc.gridy = 3;

        menuPausa.add(
                trocarMiau,
                gbc
        );

        gbc.gridy = 4;

        menuPausa.add(
                controles,
                gbc
        );

        gbc.gridy = 5;

        menuPausa.add(
                menuPrincipal,
                gbc
        );

        // 5Q.2: LABORATORIO (so no treino): uma "vista" do proprio menu de pausa.
        botoesPausaNormal.addAll(java.util.List.of(continuar, reiniciarLuta, trocarMiau, controles, menuPrincipal));
        if (modoTreino) {
            gbc.insets = new Insets(4, 20, 4, 20);
        }
        botaoReiniciarSituacao = new JButton("REINICIAR SITUAÇÃO");
        botaoReiniciarSituacao.addActionListener(e -> iniciarSituacaoLaboratorio());
        botaoLaboratorio = new JButton("LABORATÓRIO");
        botaoLaboratorio.addActionListener(e -> mostrarLaboratorio(true));
        botaoSituacao = new JButton();
        botaoSituacao.addActionListener(e -> {
            lab.proximaSituacao(1);
            atualizarBotoesLab();
        });
        botaoControla = new JButton();
        botaoControla.addActionListener(e -> {
            lab.inverterPapeis();
            atualizarBotoesLab();
        });
        botaoParceiroLab = new JButton();
        botaoParceiroLab.addActionListener(e -> {
            lab.setParceiroControlado(!lab.isParceiroControlado());
            atualizarBotoesLab();
        });
        JButton iniciarLab = new JButton("INICIAR SITUAÇÃO");
        iniciarLab.addActionListener(e -> iniciarSituacaoLaboratorio());
        JButton desligarLab = new JButton("DESLIGAR LABORATÓRIO");
        desligarLab.addActionListener(e -> {
            lab.desativar();
            catalogoLabVisivel = false;
            mostrarLaboratorio(false);
        });
        JButton voltarLab = com.irmaosmiau.ui.NavegadorMenus.marcarVoltar(new JButton("VOLTAR"));
        voltarLab.addActionListener(e -> mostrarLaboratorio(false));
        botoesLab.addAll(java.util.List.of(botaoSituacao, botaoControla, botaoParceiroLab, iniciarLab, desligarLab,
                voltarLab));
        int linha = 6;
        for (JButton b : new JButton[] {botaoReiniciarSituacao, botaoLaboratorio}) {
            b.setFont(fonteBotao);
            b.setVisible(false);
            gbc.gridy = linha++;
            menuPausa.add(b, gbc);
        }
        botaoLaboratorio.setVisible(modoTreino);
        for (JButton b : botoesLab) {
            b.setFont(fonteBotao.deriveFont(fonteBotao.getSize2D() * 0.85f));
            b.setVisible(false);
            gbc.gridy = linha++;
            menuPausa.add(b, gbc);
        }
        atualizarBotoesLab();

        menuPausa.setVisible(false);

        add(menuPausa);

        voltarDaAjuda = com.irmaosmiau.ui.NavegadorMenus.marcarVoltar(new JButton("VOLTAR"));
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
        limparPosturasPendentes();       // 5K: nada de clique de postura atravessando o menu

        menuPausa.setVisible(
                pausado
        );
        if (pausado) {
            botaoContinuar.requestFocusInWindow();
        } else {
            // 5I: o botao/tecla usado para sair do menu nao vira acao de toque (pulo,
            // rolamento, pegada...): so um toque NOVO vale. Movimento segurado continua.
            sincronizarBordasDeToque();
            requestFocusInWindow();
        }

        repaint();
    }

    /** Entrega 5C: abre/fecha a ajuda de controles (somente com o jogo pausado). */
    void mostrarAjudaControles(boolean visivel) {
        ajudaVisivel = visivel && pausado;
        menuPausa.setVisible(pausado && !ajudaVisivel);
        voltarDaAjuda.setVisible(ajudaVisivel);
        repaint();
    }

    /** 5I: o que estiver segurado agora conta como "ja visto" (nenhuma borda de subida no proximo tick). */
    private void sincronizarBordasDeToque() {
        puloAnterior = input.isPular();
        puloCurtoAnterior = input.isPularCurto();
        rolamentoAnterior = input.isRolar();
        sprawlAnterior = input.isSprawl();
        deitarAnterior = input.isDeitar();
        puloBrancoAnterior = input.isPular();
        puloCurtoBrancoAnterior = input.isPularCurto();
        rolamentoBrancoAnterior = input.isRolar();
        deitarBrancoAnterior = input.isDeitar();
        sprawlBrancoAnterior = input.isSprawl();
        puloPretoAnterior = input.isPretoPular();
        puloCurtoPretoAnterior = input.isPretoPularCurto();
        rolamentoPretoAnterior = input.isPretoRolar();
        deitarPretoAnterior = input.isPretoDeitar();
        sprawlPretoAnterior = input.isPretoSprawl();
        for (SlotJogador slot : SlotJogador.values()) {
            for (AcaoJogador acao : AcoesContextuais.ACOES_DE_COMBATE) {
                if (slot.usa(acao)) {
                    toques.sincronizar(slot, acao, input.isAtiva(slot, acao));
                }
            }
        }
    }

    /** 5I: o navegador de menus so age com o jogo pausado ou no fim de jogo. */
    @Override
    public boolean aceitaNavegacao() {
        return pausado || fimDeJogo;
    }

    /** O ESC continua sendo da pausa (abrir/fechar), nao do navegador. */
    @Override
    public boolean escVolta() {
        return false;
    }

    JButton getBotaoContinuar() {
        return botaoContinuar;
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
            if (dispositivos.usaControleAgora(s)) {          // 5O.1: teclado-controle mostra os botoes
                noGamepad.add(s);
            }
        }
        return new AjudaControles.Rotulos(input.getMapa(), mapaGamepad, noGamepad);
    }

    private String dispositivoNoTitulo(SlotJogador slot) {
        return dispositivos.usaGamepadAgora(slot) ? " · GAMEPAD"
                : dispositivos.usaTecladoControleAgora(slot) ? " · TECLADO-CONTROLE" : "";
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
        limparPosturasPendentes();
    }

    // =====================================================
    // ENTREGA 5K: POSTURAS POR F/L3 (CLIQUE SIMPLES E DUPLO)
    // =====================================================
    static final int JANELA_DUPLO_CLIQUE_TICKS = com.irmaosmiau.controles.ReconhecedorCliqueDuplo.ticksDaJanela(
            com.irmaosmiau.controles.ReconhecedorCliqueDuplo.JANELA_MS_PADRAO, INTERVALO_GAME_LOOP_MS);
    private final java.util.Map<SlotJogador, com.irmaosmiau.controles.ReconhecedorCliqueDuplo> cliquesPostura =
            new java.util.EnumMap<>(SlotJogador.class);
    private final java.util.Map<SlotJogador, String> ultimoCliquePostura = new java.util.EnumMap<>(SlotJogador.class);

    private com.irmaosmiau.controles.ReconhecedorCliqueDuplo cliques(SlotJogador slot) {
        return cliquesPostura.computeIfAbsent(slot,
                s -> new com.irmaosmiau.controles.ReconhecedorCliqueDuplo(JANELA_DUPLO_CLIQUE_TICKS));
    }

    /**
     * Um tick do comando de postura do slot. O clique simples so e resolvido
     * quando a janela do duplo clique fecha; o duplo vira UMA transicao rapida.
     * Na pegada (aplicador ou receptor) a postura nao muda: nada de fuga gratuita.
     */
    private void processarPostura(SlotJogador slot, IrmaoMiau lutador, boolean borda) {
        com.irmaosmiau.controles.ReconhecedorCliqueDuplo.Evento e = cliques(slot).atualizar(borda, tick);
        if (e == com.irmaosmiau.controles.ReconhecedorCliqueDuplo.Evento.NADA) {
            return;
        }
        boolean duplo = e == com.irmaosmiau.controles.ReconhecedorCliqueDuplo.Evento.DUPLO;
        if (presoNoSolo(lutador)) {
            feedback.mostrarInfo(slot.getSigla() + ": POSTURA · LIGADO NO SOLO (use as técnicas)", tick);
            return;                                          // 5P: no solo a postura e do MotorSolo
        }
        ultimoCliquePostura.put(slot, duplo ? "duplo" : "simples");
        if (sistemaCombate != null && sistemaCombate.estaEmInteracao()
                && (sistemaCombate.getAplicadorInteracao() == lutador
                        || sistemaCombate.getReceptorInteracao() == lutador)) {
            feedback.mostrarInfo(slot.getSigla() + ": POSTURA SÓ FORA DA PEGADA", tick);
            return;
        }
        com.irmaosmiau.entities.TransicaoPostura t = duplo
                ? lutador.proximaTransicaoRapida() : lutador.proximaTransicaoSimples();
        com.irmaosmiau.entities.TransicaoPostura.Resultado r = lutador.executarTransicao(t);
        if (r == com.irmaosmiau.entities.TransicaoPostura.Resultado.SEM_FOLEGO) {
            feedback.mostrarInfo(slot.getSigla() + ": SEM FÔLEGO (" + t.getContrato() + ")", tick);
        }
    }

    /** Pausa, perda de foco, F5: nenhum clique pendente sobrevive. */
    private void limparPosturasPendentes() {
        cliquesPostura.values().forEach(com.irmaosmiau.controles.ReconhecedorCliqueDuplo::limpar);
    }

    /** HUD/testes: o slot tem um clique simples esperando a janela. */
    boolean isCliquePosturaPendente(SlotJogador slot) {
        return cliques(slot).isPendente();
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

        // 5Q: vitoria por finalizacao (rendicao ou finalizacao consolidada); no treino so avisa e segue.
        if (solo != null && !fimDeJogo && solo.getVitoria().isPresent()) {
            com.irmaosmiau.posicional.MotorSolo.Vitoria v = solo.getVitoria().get();
            String txt = (v.vencedor() == com.irmaosmiau.posicional.Lado.A ? "J1" : "J2") + " VENCEU POR FINALIZAÇÃO · "
                    + v.finalizacao().nome().toUpperCase(java.util.Locale.ROOT) + (v.rendicao() ? " (BATEU)" : "");
            if (modoTreino) {
                avisos.adicionar(txt);
                solo.limparVitoria();
            } else {
                fimDeJogo = true;
                mensagemFim = txt;
            }
        }

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

                    // 5M: "TEMPO ESGOTADO · resultado" em duas linhas (cabe na caixa).
                    label.setText(
                            mensagemFim.contains(" · ")
                            ? "<html><div style='text-align:center'>"
                            + mensagemFim.replace("&", "&amp;").replace("<", "&lt;").replace(" · ", "<br>")
                            + "</div></html>"
                            : mensagemFim
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
        desenharRelogio(g2);
        avisos.desenhar(g2, LARGURA);                             // 5P.1: aviso discreto no topo
        desenharLaboratorio(g2);                                  // 5Q.2: HUD tecnico (so no laboratorio)

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
        d.fillRoundRect(262, 32, 520, 224 + 74 + 68 + 17, 8, 8);
        desenharDebugCombate(d);
        // Entrega 5J: postura, papeis, variante da animacao e camadas visuais.
        d.setColor(new Color(20, 40, 90));
        int y = 268;
        for (String linha : linhasTecnicas()) {
            d.drawString(linha, 270, y);
            y += 15;                                       // 5L.2: 15 (antes 17) para caber a linha da insistencia
        }
        d.dispose();
    }

    /** Entrega 5J: linhas do HUD tecnico (F3). Nada disso aparece no jogo normal. */
    List<String> linhasTecnicas() {
        List<String> l = new java.util.ArrayList<>();
        if (sistemaCombate == null) {
            return l;
        }
        l.add("POSTURA J1: " + jogadorBranco.getEstadoCorporal() + descricaoCiclo(SlotJogador.JOGADOR_1, jogadorBranco)
                + " | J2: " + jogadorPreto.getEstadoCorporal() + descricaoCiclo(SlotJogador.JOGADOR_2, jogadorPreto));
        IrmaoMiau apl = sistemaCombate.getAplicadorInteracao();
        IrmaoMiau rec = sistemaCombate.getReceptorInteracao();
        if (sistemaCombate.estaEmInteracao() && apl != null && rec != null) {
            l.add("APLICADOR: " + rotuloSlot(apl) + " | RECEPTOR: " + rotuloSlot(rec) + " | "
                    + sistemaCombate.getIdAcaoInteracao()
                    + (sistemaCombate.isInteracaoEstabelecida() ? " ESTABELECIDA" : " ENTRADA")
                    + String.format(" | FIRMEZA %.0f", sistemaCombate.getFirmeza()));
        } else {
            l.add("APLICADOR: - | RECEPTOR: - | sem tecnica ativa");
        }
        com.irmaosmiau.combate.SelecaoAnimacaoConjunta sel = sistemaCombate.getSelecaoAnimacao();
        com.irmaosmiau.animacao.CamadaPoseCombate c = animador.getCamada();
        // 5N: o que o receptor mostra (reacao compartilhada da tecnica) e como a ultima pegada acabou.
        String receptor = rec == null || !sistemaCombate.estaEmInteracao() ? "-"
                : rotuloSlot(rec) + " " + rec.getEstadoCorporal() + " " + c.faseReceptor(rec);
        l.add("ANIMACAO: " + (sel == null ? "-" : sel.descricao())
                + (apl != null && apl.isAjoelhado() ? " + fallback ajoelhado" : "")
                + " | RECEPTOR: " + receptor);
        String passos = "-";
        String deseq = "-";
        for (IrmaoMiau x : List.of(jogadorBranco, jogadorPreto)) {
            if (c.faseArrasto(x) >= 0) {
                passos = "fase " + c.faseArrasto(x);
            }
            if (c.intensidadeReacao(x) > 0) {
                deseq = rotuloSlot(x) + " reagindo";
            }
        }
        l.add("ARRASTO: " + descricaoArrasto() + " | passos " + passos + " | LINHAS J1 " + (jogadorBranco.getLinhaAtual() + 1)
                + " J2 " + (jogadorPreto.getLinhaAtual() + 1) + " | DESEQ: " + deseq);
        // Entrega 5K: modo de comparacao, folego, custo e consequencia do desequilibrio.
        l.add("ANIMACOES PEGADA (F6): " + com.irmaosmiau.combate.ModoAnimacoesPegada.atual().rotulo()
                + " | MOV (F7): " + com.irmaosmiau.animacao.VersaoMovimentos.atual().rotulo()
                // 5O: familia escolhida pelos modificadores (o F3 continua com 10 linhas).
                + " | FAMILIA J1 " + familiaDo(SlotJogador.JOGADOR_1).name()
                + " J2 " + familiaDo(SlotJogador.JOGADOR_2).name());
        l.add("FOLEGO J1: " + folego(jogadorBranco) + " | J2: " + folego(jogadorPreto));
        StringBuilder efeitos = new StringBuilder();
        for (IrmaoMiau x : List.of(jogadorBranco, jogadorPreto)) {
            int t = x.getEfeitos().getTicksRestantes(com.irmaosmiau.entities.EfeitosTemporarios.Tipo.DESEQUILIBRIO);
            if (t > 0) {
                efeitos.append(rotuloSlot(x)).append(" DESEQUILIBRADO: velocidade 60% (").append(t).append(" ticks) ");
            }
        }
        l.add((efeitos.length() == 0 ? "CONSEQUENCIA: -" : "CONSEQUENCIA: " + efeitos.toString().trim())
                + " | " + diagnosticoRb() + " | RECUSA J1 " + ultimoMotivoFamilia(SlotJogador.JOGADOR_1)
                + " J2 " + ultimoMotivoFamilia(SlotJogador.JOGADOR_2));
        l.add((solo == null ? "" : solo.resumo() + " | ")                         // 5P
                + "TRANSICAO J1: " + transicao(jogadorBranco) + " | J2: " + transicao(jogadorPreto)
                + " | DEFESA J1: " + defesaDe(jogadorBranco) + " J2: " + defesaDe(jogadorPreto));
        // Entrega 5L: tipo de pegada, defesa e encerramento.
        String tecnica = sistemaCombate.estaEmInteracao()
                ? com.irmaosmiau.combate.ParametrosPegadas.de(sistemaCombate.getIdAcaoInteracao()).nome() : "-";
        com.irmaosmiau.combate.EventoDefesa ev = sistemaCombate.getUltimoEventoDefesa();
        l.add("PEGADA: " + tecnica
                + (sistemaCombate.getTicksResistindo() > 0 ? " | RECEPTOR FORCANDO (" + sistemaCombate.getTicksResistindo() + ")" : "")
                + " | DEFESA: " + (ev == null ? "-" : ev.tipo() + " " + ev.detalhe() + " (" + rotuloSlot(ev.defensor()) + ")")
                + " | FIM: " + (sistemaCombate.getUltimoMotivoEncerramento() == null ? "-"
                        : sistemaCombate.getUltimoMotivoEncerramento()));
        // Entrega 5L.2: insistencia da 5L.1 (dados do proprio motor; zera no sucesso e no fim da pegada).
        IrmaoMiau insistente = sistemaCombate.getInsistente();
        if (insistente == null) {
            l.add("INSISTENCIA: - (bonus 0%)");
        } else {
            int falhas = sistemaCombate.getInsistencia(insistente);
            String acao = sistemaCombate.getTipoInsistencia() == com.irmaosmiau.combate.TipoDisputa.DESEQUILIBRIO
                    ? "DESEQUILIBRAR" : "ROMPER";
            l.add("INSISTENCIA: " + rotuloSlot(insistente) + " " + acao + " | falhas seguidas " + falhas
                    + String.format(" | bonus +%.0f%%", 100 * com.irmaosmiau.combate.SistemaCombate.BONUS_INSISTENCIA * falhas));
        }
        return l;
    }

    /** 5O: RB no gamepad (botao fisico) x modificador logico das quedas (RB ou a tecla 4/HOME). */
    /** 5O.2: esquema do slot no controle (Xbox ou teclado-controle); "TECLAS" nas teclas livres. */
    private String esquemaDe(SlotJogador s) {
        return dispositivos.usaControleAgora(s) ? mapaGamepad.esquema(s).name() : "TECLAS";
    }

    /** 5O/5O.1: RB fisico, Select fisico e RB LOGICO (RB, Select livre ou a tecla do modificador). */
    private String diagnosticoRb() {
        // 5O.2: esquema de controles ativo de cada jogador (nao confundir com a versao de animacoes do F7)
        StringBuilder sb = new StringBuilder("ESQUEMA J1 " + esquemaDe(SlotJogador.JOGADOR_1) + " J2 "
                + esquemaDe(SlotJogador.JOGADOR_2) + " | RB");
        for (SlotJogador s : SlotJogador.values()) {
            java.util.Optional<com.irmaosmiau.dispositivos.FonteGamepad> f = dispositivos.fonte(s)
                    .filter(x -> dispositivos.usaControleAgora(s));
            String rb = f.map(x -> x.temLeitura() ? (x.botaoFisico(com.irmaosmiau.dispositivos.BotaoGamepad.RB)
                    ? "SIM" : "nao") : "sem leitura").orElse("-");
            String sel = f.map(x -> x.temLeitura() ? (x.botaoFisico(com.irmaosmiau.dispositivos.BotaoGamepad.VOLTAR)
                    ? "SIM" : "nao") : "sem leitura").orElse("-");
            boolean logico = input.isAtiva(s, AcaoJogador.MODIFICADOR_PROJECAO);
            sb.append(' ').append(s.getSigla()).append(" fis:").append(rb).append(" sel:").append(sel)
                    .append(" log:").append(logico ? "SIM" : "nao");
        }
        return sb.toString();
    }


    private static String defesaDe(IrmaoMiau l) {
        if (l.isFazendoSprawl()) {
            return "SPRAWL " + l.getFaseSprawl();
        }
        if (l.isEmAcrobacia()) {
            return l.getAcrobacia().name();
        }
        com.irmaosmiau.entities.DefesaCorporal d = l.getDefesa();
        if (d.isAtiva()) {
            return d.getTipo().name() + " " + d.getFase();
        }
        return l.getRecargaSprawl() > 0 ? "- (sprawl recarga " + l.getRecargaSprawl() + ")" : "-";
    }

    /** 5N: ultimo pedido de arrasto (direcao, andou ou bloqueou) em poucos caracteres. */
    private String descricaoArrasto() {
        com.irmaosmiau.combate.SistemaCombate.UltimoArrasto a = sistemaCombate.getUltimoArrasto();
        if (a == null || sistemaCombate.getTick() - a.tick() > 30) {
            return "-";
        }
        String dir = (a.pedidoX() < 0 ? "ESQ" : a.pedidoX() > 0 ? "DIR" : "")
                + (a.pedidoX() != 0 && a.pedidoY() != 0 ? "+" : "")
                + (a.pedidoY() < 0 ? "FUNDO" : a.pedidoY() > 0 ? "FRENTE" : "");
        String r = a.resultado() == com.irmaosmiau.combate.ResultadoAcaoTecnica.LIMITE_ARENA ? "BLOQUEADO"
                : a.bloqueadoX() ? "OK (BORDA X)" : a.bloqueadoY() ? "OK (BORDA LINHA)" : "OK";
        return dir + " " + r;
    }

    private String descricaoCiclo(SlotJogador slot, IrmaoMiau x) {
        String ciclo = x.isAjoelhado() ? (x.isCicloSubindo() ? " (sobe)" : " (desce)") : "";
        String clique = cliques(slot).isPendente() ? " clique..." : "";
        String ultimo = ultimoCliquePostura.containsKey(slot) ? " [" + ultimoCliquePostura.get(slot) + "]" : "";
        return ciclo + clique + ultimo;
    }

    private static String folego(IrmaoMiau x) {
        String custo = x.getUltimoCustoMotivo().isEmpty() ? ""
                : String.format(" ult %.2f %s", x.getUltimoCustoFolego(), x.getUltimoCustoMotivo());
        return String.format("%.1f/%.0f", x.getFolegoAtual(), x.getAtributos().getFolego()) + custo;
    }

    private static String transicao(IrmaoMiau x) {
        return x.getTransicaoAtiva() == null ? "-"
                : x.getTransicaoAtiva().getContrato() + String.format(" %.0f%%", x.getProgressoTransicao() * 100);
    }

    private String rotuloSlot(IrmaoMiau x) {
        return (x == jogadorBranco ? "J1 " : "J2 ") + x.getNome();
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
        // Entrega 5K: F6 = compara animacoes de pegada (AUTORAL <-> EXPERIMENTAL), vale na proxima pegada.
        atalho("F6", "alternarAnimacoesPegada", () -> {
            com.irmaosmiau.combate.ModoAnimacoesPegada m = com.irmaosmiau.combate.ModoAnimacoesPegada.alternar();
            feedback.mostrarInfo("ANIMAÇÕES DE PEGADA: " + m.rotulo() + " (na próxima pegada)", tick);
            repaint();
        });
        // Entrega 5O: F7 = versao do rolamento e do sprawl (ORIGINAL x EXPERIMENTAL 5O); o F6 nao muda.
        atalho("F7", "alternarMovimentos", () -> {
            com.irmaosmiau.animacao.VersaoMovimentos v = com.irmaosmiau.animacao.VersaoMovimentos.alternar();
            feedback.mostrarInfo("ROLAMENTO E SPRAWL: " + v.rotulo(), tick);
            repaint();
        });
        atalho("F4", "proximoCenario", () -> {
            cenario = cenarios.proximo(cenario);
            mostrarPreferencia("Cenário: " + cenario.getNome(),
                    preferencias.definirCenarioPreferido(cenario.getId()));
            repaint();
        });
        atalho("F5", "reiniciarTreino", this::reiniciarTreino);
        atalho("F8", "alternarCatalogoLab", () -> {                 // 5Q.2: catalogo do laboratorio
            if (modoTreino && lab.isAtiva()) {
                catalogoLabVisivel = !catalogoLabVisivel;
                repaint();
            }
        });
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
        if (lab.isAtiva()) {
            iniciarSituacaoLaboratorio();                    // 5Q.2: F5 repete a mesma situacao
            return;
        }
        reiniciarSessaoTreino();
    }

    /** Recria a sessao de treino (lutadores pelas escolhas, motor e vinculos zerados). */
    private void reiniciarSessaoTreino() {

        sessaoTreino.reiniciar();
        limparPosturasPendentes();

        jogadorBranco = sessaoTreino.getJogador();
        jogadorPreto = sessaoTreino.getParceiro();
        sistemaCombate = sessaoTreino.getSistema();
        criarMotorSolo();                                        // 5P: F5 limpa todos os vinculos
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
        deslocandoConjuntoJ1 = false;
        deslocandoConjuntoJ2 = false;

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

    /** 5N: relogio logico do GamePanel (o mesmo das animacoes). */
    long tickParaTestes() {
        return tick;
    }

    /** 5M: fim de jogo e motivo (para testes). */
    boolean isFimDeJogo() {
        return fimDeJogo;
    }

    String getMensagemFim() {
        return mensagemFim;
    }

    /** 5M: aciona ESC (pausa) pelo mapa de acoes, como o teclado. */
    void alternarPausaParaTestes() {
        getActionMap().get("alternarPausa").actionPerformed(null);
    }

    boolean isModoTreino() {
        return modoTreino;
    }

    com.irmaosmiau.entities.InicioLuta getInicioLuta() {
        return inicioLuta;
    }

    SessaoTreino getSessaoTreino() {
        return sessaoTreino;
    }

    /** 5I: mensagem de combate visivel agora (para testes). */
    String mensagemFeedbackParaTestes() {
        return feedback.getMensagem(tick);
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

    AnimadorPartida getAnimador() {
        return animador;
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
