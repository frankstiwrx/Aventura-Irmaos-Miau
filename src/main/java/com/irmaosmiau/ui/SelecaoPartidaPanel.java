package com.irmaosmiau.ui;

import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.InfoGamepad;
import com.irmaosmiau.dispositivos.LeitorNavegacao;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.dispositivos.ProvedorGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.DefinicaoPersonagem;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.personagens.LimitesAtributos;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Entrega 5M: selecao de personagens em grade para JOGAR (2 jogadores).
 *
 * Esquerda J1, centro o elenco (5 x 3), direita J2, embaixo MAPA / TEMPO /
 * DIFICULDADE e LUTAR. Cada jogador tem o SEU cursor e confirma sozinho
 * (personagens iguais permitidos); nada de foco compartilhado:
 *
 * - teclado J1: W A S D mover · ESPACO (ou H) confirmar · Q cancelar;
 * - teclado J2: setas mover · ENTER (ou END) confirmar · BACKSPACE cancelar;
 * - gamepad: o slot associado a GAMEPAD em CONFIGURACOES recebe os gamepads
 *   conectados, na ordem (J1 primeiro): D-pad/analogico, A confirma, B cancela;
 * - ESC volta ao menu principal.
 *
 * Fases de cada jogador: ESCOLHENDO -> FAIXA -> KIMONO -> PRONTO (personagem original, confirmar) ou
 * ESCOLHENDO -> PERFIL -> PRONTO (X / Quadrado, ou E / J no teclado: os perfis personalizados daquele
 * personagem-base). O perfil usa a aparencia e os atributos salvos no editor: nao passa por faixa nem
 * kimono (a faixa e a branca, sem bonus de graduacao). Cancelar volta uma fase; cancelar escolhendo volta
 * ao menu.
 *
 * Reorganizacao dos menus (JOGAR): a tela tem duas ETAPAS. Na primeira (lutadores) cada jogador, do
 * mesmo jeito, confirma personagem -> (perfil) -> FAIXA -> KIMONO e so entao fica PRONTO, com as escolhas
 * travadas (cancelar volta ao kimono). Com os dois prontos, AVANCAR (qualquer um confirma) abre a segunda
 * etapa, CONFIGURACOES DA LUTA ({@link ConfiguracaoLuta}: arena, tempo, dificuldade, regras e INICIAR
 * LUTA). Voltar dela devolve os lutadores intactos. As entradas sao as mesmas nas duas etapas (o aperto
 * que avanca nunca vale na etapa seguinte).
 *
 * TREINO: o mesmo layout e o mesmo fluxo, mas UM dispositivo escolhe os dois: primeiro o LUTADOR, depois o
 * PARCEIRO (cancelar no parceiro volta ao lutador). A segunda etapa mostra so a ARENA (e o INICIO) e
 * INICIAR TREINO ({@link ConfiguracaoLuta#treino}).
 *
 * Os espacos bloqueados sao so visuais: nao ha personagens cadastrados para
 * eles, nao revelam nada e nao podem ser confirmados.
 */
public final class SelecaoPartidaPanel extends JPanel implements TelaNavegavel {

    private static final long serialVersionUID = 1L;

    /** O que fazer quando os dois estao prontos e o J1 confirma LUTAR. */
    @FunctionalInterface
    public interface AoIniciar {
        void iniciar(EscolhaLutador j1, EscolhaLutador j2, String idCenario, int segundosLimite);

        /** Com o modo de luta escolhido nas configuracoes (o padrao ignora o modo). */
        default void iniciar(EscolhaLutador j1, EscolhaLutador j2, String idCenario, int segundosLimite,
                com.irmaosmiau.regras.ModoLuta modo) {
            iniciar(j1, j2, idCenario, segundosLimite);
        }
    }

    /** 5M.3: o que fazer quando o J1 confirma TREINAR (parceiro passivo, mapa e inicio). */
    @FunctionalInterface
    public interface AoIniciarTreino {
        void iniciar(EscolhaLutador jogador, EscolhaLutador parceiro, String idCenario, InicioLuta inicio);
    }

    /** 5M.3: a mesma tela serve a luta (2 jogadores) e o treino (so o J1 escolhe). */
    public enum Modo { PARTIDA, TREINO }

    public static final int COLUNAS = 5;
    public static final int LINHAS = 3;
    static final int CELULA = 56;
    static final int ESPACO = 8;
    static final int GRADE_X = 244;
    static final int GRADE_Y = 64;

    /**
     * Tempos de luta oferecidos (segundos; 0 = sem limite), livres para qualquer faixa e modo: a faixa nunca
     * escolhe o tempo de uma luta casual (os tempos oficiais por faixa ficam para um futuro modo campeonato).
     */
    public static final int[] TEMPOS = {60, 90, 120, 180, 240, 300, 360, 420, 480, 540, 600, 0};

    /** Tempo com que a tela abre: sem limite (como antes). */
    public static final int TEMPO_PADRAO = 0;

    /** Fase de um jogador. */
    public enum Fase { ESCOLHENDO,
        /** Escolhendo um perfil personalizado do personagem-base (X / Quadrado). */
        PERFIL,
        /** JOGAR: escolhendo a faixa (lados mudam, confirmar segue para o kimono). */
        FAIXA,
        /** JOGAR: escolhendo o kimono (confirmar deixa PRONTO). */
        KIMONO,
        PRONTO }

    private static final class Jogador {
        int cursor;
        Fase fase = Fase.ESCOLHENDO;
        /** Perfis personalizados da base (sem o oficial), abertos pelo X / Quadrado. */
        List<EscolhaLutador> perfis = List.of();
        int perfil;
        /** Um perfil foi confirmado (PRONTO com o perfil: sem faixa nem kimono). */
        boolean personalizado;
        long avisoBloqueadoAte = -1;
        long avisoSemPerfisAte = -1;
        IrmaoMiau previa;
        String idPrevia;
        /** 5Q.1: faixa (fonte dos atributos) e kimono (null = padrao do personagem) deste slot. */
        com.irmaosmiau.regras.Graduacao faixa = com.irmaosmiau.regras.Graduacao.BRANCA;
        com.irmaosmiau.entities.CorKimono kimono;
    }

    private final transient List<DefinicaoPersonagem> elenco = new ArrayList<>();
    private final transient OpcoesDeLutadores opcoes;
    private final transient AoIniciar aoIniciar;
    private final transient AoIniciarTreino aoIniciarTreino;
    private final Modo modo;
    private final transient Runnable aoVoltar;
    private final transient Supplier<ProvedorGamepads> provedor;
    private final transient Supplier<AssociacaoDispositivos> associacao;
    private final transient Map<SlotJogador, Jogador> jogadores = new EnumMap<>(SlotJogador.class);
    private final transient Map<String, LeitorNavegacao> leitores = new HashMap<>();
    private final transient Set<String> segurando = new HashSet<>();
    private long tempoMs;
    private long avisoAguardandoAte = -1;
    private boolean iniciada;
    /** A segunda etapa (JOGAR: configuracoes da luta; TREINO: arena) e se ela esta aberta. */
    private final transient ConfiguracaoLuta config;
    private boolean naConfiguracao;
    private final Timer timer = new Timer(16, e -> tick(16));

    public SelecaoPartidaPanel(CatalogoPersonagens catalogo, OpcoesDeLutadores opcoes, RegistroCenarios cenarios,
            String idCenarioInicial, AoIniciar aoIniciar, Runnable aoVoltar,
            Supplier<ProvedorGamepads> provedor, Supplier<AssociacaoDispositivos> associacao) {
        this(Modo.PARTIDA, catalogo, opcoes, cenarios, idCenarioInicial, aoIniciar, null, null, aoVoltar, provedor,
                associacao);
    }

    /**
     * A selecao do TREINO: o mesmo layout do JOGAR, com um dispositivo so escolhendo o lutador e depois o
     * parceiro (qualquer teclado ou gamepad move o lutador da vez). A segunda etapa tem a arena e o inicio.
     */
    public static SelecaoPartidaPanel treino(CatalogoPersonagens catalogo, OpcoesDeLutadores opcoes,
            RegistroCenarios cenarios, String idCenarioInicial, InicioLuta inicioInicial, AoIniciarTreino aoIniciar,
            Runnable aoVoltar, Supplier<ProvedorGamepads> provedor, Supplier<AssociacaoDispositivos> associacao) {
        if (aoIniciar == null) {
            throw new IllegalArgumentException("Acao de inicio do treino e obrigatoria.");
        }
        return new SelecaoPartidaPanel(Modo.TREINO, catalogo, opcoes, cenarios, idCenarioInicial, null, aoIniciar,
                inicioInicial, aoVoltar, provedor, associacao);
    }

    private SelecaoPartidaPanel(Modo modo, CatalogoPersonagens catalogo, OpcoesDeLutadores opcoes,
            RegistroCenarios cenarios, String idCenarioInicial, AoIniciar aoIniciar, AoIniciarTreino aoIniciarTreino,
            InicioLuta inicioInicial, Runnable aoVoltar, Supplier<ProvedorGamepads> provedor,
            Supplier<AssociacaoDispositivos> associacao) {
        if (catalogo == null || opcoes == null || cenarios == null || (aoIniciar == null && aoIniciarTreino == null)) {
            throw new IllegalArgumentException("Catalogo, opcoes, cenarios e acao de inicio sao obrigatorios.");
        }
        this.modo = modo;
        this.opcoes = opcoes;
        this.aoIniciar = aoIniciar;
        this.aoIniciarTreino = aoIniciarTreino;
        this.aoVoltar = aoVoltar;
        this.provedor = provedor;
        this.associacao = associacao;
        elenco.addAll(catalogo.listar());
        config = modo == Modo.PARTIDA ? new ConfiguracaoLuta(cenarios, idCenarioInicial)
                : ConfiguracaoLuta.treino(cenarios, idCenarioInicial, inicioInicial);
        for (SlotJogador s : SlotJogador.values()) {
            Jogador j = new Jogador();
            j.cursor = Math.min(s.ordinal(), elenco.size() - 1);    // J1 no 1o, J2 no 2o
            jogadores.put(s, j);
        }
        setPreferredSize(new Dimension(800, 450));
        setFocusable(true);
        instalarMouse();                                         // 5Q.4
        instalarTeclas();
    }

    // =====================================================
    // ENTRADAS
    // =====================================================
    private void instalarTeclas() {
        teclas(SlotJogador.JOGADOR_1, Comando.ANTERIOR, "W");
        teclas(SlotJogador.JOGADOR_1, Comando.PROXIMO, "S");
        teclas(SlotJogador.JOGADOR_1, Comando.ESQUERDA, "A");
        teclas(SlotJogador.JOGADOR_1, Comando.DIREITA, "D");
        teclas(SlotJogador.JOGADOR_1, Comando.CONFIRMAR, "SPACE", "H");
        teclas(SlotJogador.JOGADOR_1, Comando.VOLTAR, "Q");
        teclas(SlotJogador.JOGADOR_1, Comando.PERFIS, "E");          // = X / Quadrado
        teclas(SlotJogador.JOGADOR_2, Comando.PERFIS, "J");          // = X do teclado-controle do J2
        teclas(SlotJogador.JOGADOR_2, Comando.ANTERIOR, "UP");
        teclas(SlotJogador.JOGADOR_2, Comando.PROXIMO, "DOWN");
        teclas(SlotJogador.JOGADOR_2, Comando.ESQUERDA, "LEFT");
        teclas(SlotJogador.JOGADOR_2, Comando.DIREITA, "RIGHT");
        teclas(SlotJogador.JOGADOR_2, Comando.CONFIRMAR, "ENTER", "END");
        teclas(SlotJogador.JOGADOR_2, Comando.VOLTAR, "BACK_SPACE");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed ESCAPE"), "voltarMenu");
        getActionMap().put("voltarMenu", acao(() -> {
            if (naConfiguracao) {
                voltarAosLutadores();                        // ESC na configuracao: volta uma tela
                repaint();
            } else {
                voltarAoMenu();
            }
        }));
    }

    /** Cada tecla age uma vez por aperto (segurar nao repete; o auto-repeat do sistema e ignorado). */
    private void teclas(SlotJogador slot, Comando c, String... nomes) {
        for (String n : nomes) {
            String id = slot.name() + "." + n;
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + n), id + ".p");
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released " + n), id + ".s");
            getActionMap().put(id + ".p", acao(() -> {
                if (segurando.add(id)) {
                    comandar(slot, c);
                }
            }));
            getActionMap().put(id + ".s", acao(() -> segurando.remove(id)));
        }
    }

    private static AbstractAction acao(Runnable r) {
        return new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        };
    }

    private void lerGamepads() {
        ProvedorGamepads p = provedor == null ? null : provedor.get();
        AssociacaoDispositivos a = associacao == null ? null : associacao.get();
        if (p == null || a == null) {
            return;
        }
        List<SlotJogador> comGamepad = new ArrayList<>();
        for (SlotJogador s : SlotJogador.values()) {
            if (a.tipo(s) == TipoDispositivo.GAMEPAD) {
                comGamepad.add(s);
            }
        }
        List<InfoGamepad> conectados = p.conectados();
        if (modo == Modo.TREINO) {
            comGamepad.clear();                                 // treino: todo gamepad move o lutador da vez
            for (int i = 0; i < conectados.size(); i++) {
                comGamepad.add(SlotJogador.JOGADOR_1);
            }
        }
        for (int i = 0; i < conectados.size() && i < comGamepad.size(); i++) {
            InfoGamepad g = conectados.get(i);
            EstadoGamepad e = p.estado(g.chave()).orElse(null);
            LeitorNavegacao l = leitores.computeIfAbsent(g.chave(), k -> new LeitorNavegacao(true));
            for (Comando c : l.ler(e, tempoMs)) {
                comandar(comGamepad.get(i), c);
            }
        }
    }

    // =====================================================
    // REGRAS DA TELA
    // =====================================================
    /** Um comando de um jogador (teclado, gamepad ou testes). */
    void comandar(SlotJogador slot, Comando c) {
        if (iniciada) {
            return;
        }
        if (naConfiguracao) {
            // segunda etapa: um foco so, que os dois jogadores podem mover
            switch (config.comandar(c, tempoMs)) {
                case INICIAR -> iniciar();
                case VOLTAR -> voltarAosLutadores();
                default -> {
                }
            }
            repaint();
            return;
        }
        if (modo == Modo.TREINO) {
            slot = slotDaVez();                                 // treino: um dispositivo escolhe os dois
            if (slot == SlotJogador.JOGADOR_2 && c == Comando.VOLTAR
                    && jogadores.get(slot).fase == Fase.ESCOLHENDO) {
                voltarUmaFase(jogadores.get(SlotJogador.JOGADOR_1));   // cancelar no parceiro: volta ao lutador
                repaint();
                return;
            }
        }
        Jogador j = jogadores.get(slot);
        Fase antes = j.fase;
        switch (j.fase) {
            case ESCOLHENDO -> escolhendo(j, c);
            case PERFIL -> perfil(j, c);
            case FAIXA, KIMONO -> personalizando(j, c);
            default -> pronto(j, c);
        }
        if (modo == Modo.TREINO && slot == SlotJogador.JOGADOR_1 && antes != Fase.PRONTO && j.fase == Fase.PRONTO) {
            parceiroPadrao();
        }
        repaint();
    }

    /** TREINO: quem recebe os comandos (o lutador ate ficar pronto, depois o parceiro). */
    SlotJogador slotDaVez() {
        return jogadores.get(SlotJogador.JOGADOR_1).fase == Fase.PRONTO ? SlotJogador.JOGADOR_2
                : SlotJogador.JOGADOR_1;
    }

    private void escolhendo(Jogador j, Comando c) {
        int total = COLUNAS * LINHAS;
        switch (c) {
            case ANTERIOR -> j.cursor = j.cursor - COLUNAS >= 0 ? j.cursor - COLUNAS : j.cursor;
            case PROXIMO -> j.cursor = j.cursor + COLUNAS < total ? j.cursor + COLUNAS : j.cursor;
            case ESQUERDA -> j.cursor = j.cursor % COLUNAS == 0 ? j.cursor + COLUNAS - 1 : j.cursor - 1;
            case DIREITA -> j.cursor = j.cursor % COLUNAS == COLUNAS - 1 ? j.cursor - COLUNAS + 1 : j.cursor + 1;
            case CONFIRMAR -> {
                if (personagemEm(j.cursor) == null) {
                    j.avisoBloqueadoAte = tempoMs + 700;          // bloqueado: so um aviso
                    return;
                }
                j.perfis = List.of();                             // confirmar = o personagem original
                j.personalizado = false;
                j.kimono = null;                                  // 5Q.1: kimono padrao do novo personagem
                j.fase = Fase.FAIXA;
            }
            case PERFIS -> abrirPerfis(j);
            case VOLTAR -> voltarAoMenu();
            default -> {
            }
        }
    }

    /** X / Quadrado: os perfis personalizados do personagem-base em destaque (se houver). */
    private void abrirPerfis(Jogador j) {
        DefinicaoPersonagem d = personagemEm(j.cursor);
        if (d == null) {
            j.avisoBloqueadoAte = tempoMs + 700;
            return;
        }
        List<EscolhaLutador> desta = new ArrayList<>();
        for (EscolhaLutador e : opcoes.paraBase(d.getId()).getValor()) {
            if (!e.isOficial()) {
                desta.add(e);
            }
        }
        if (desta.isEmpty()) {
            j.avisoSemPerfisAte = tempoMs + 1200;                 // nenhum perfil deste personagem
            return;
        }
        j.perfis = List.copyOf(desta);
        j.perfil = 0;
        j.personalizado = false;
        j.fase = Fase.PERFIL;
    }

    private void perfil(Jogador j, Comando c) {
        switch (c) {
            case ESQUERDA, ANTERIOR -> j.perfil = Math.floorMod(j.perfil - 1, j.perfis.size());
            case DIREITA, PROXIMO -> j.perfil = Math.floorMod(j.perfil + 1, j.perfis.size());
            case CONFIRMAR -> {
                j.personalizado = true;                           // perfil: PRONTO sem faixa nem kimono
                j.fase = Fase.PRONTO;
            }
            case VOLTAR -> {
                j.perfis = List.of();
                j.fase = Fase.ESCOLHENDO;
            }
            default -> {
            }
        }
    }

    /** FAIXA e KIMONO do personagem original (o mesmo fluxo para J1 e J2). */
    private void personalizando(Jogador j, Comando c) {
        boolean faixa = j.fase == Fase.FAIXA;
        switch (c) {
            case ESQUERDA -> mudarVisual(j, faixa, -1);
            case DIREITA -> mudarVisual(j, faixa, 1);
            case CONFIRMAR -> j.fase = faixa ? Fase.KIMONO : Fase.PRONTO;     // o kimono confirmado = PRONTO
            case VOLTAR -> j.fase = faixa ? Fase.ESCOLHENDO : Fase.FAIXA;
            default -> {
            }
        }
    }

    private void pronto(Jogador j, Comando c) {
        // PRONTO: as escolhas ficam travadas; so cancelar (volta uma fase) ou AVANCAR
        if (c == Comando.VOLTAR) {
            voltarUmaFase(j);
        } else if (c == Comando.CONFIRMAR) {
            avancar();
        }
    }

    /** Cancelar no PRONTO: o perfil volta a lista de perfis; o original volta ao kimono. */
    private void voltarUmaFase(Jogador j) {
        if (j.personalizado) {
            j.personalizado = false;
            j.fase = Fase.PERFIL;
        } else {
            j.fase = Fase.KIMONO;
        }
    }

    /** Os dois prontos (JOGAR)? So entao o AVANCAR abre as configuracoes. */
    boolean ambosProntos() {
        return jogadores.get(SlotJogador.JOGADOR_1).fase == Fase.PRONTO
                && jogadores.get(SlotJogador.JOGADOR_2).fase == Fase.PRONTO;
    }

    /** AVANCAR: abre as configuracoes da luta com os dois prontos; senao avisa que falta o outro. */
    private void avancar() {
        if (!ambosProntos()) {
            avisoAguardandoAte = tempoMs + 900;
            return;
        }
        naConfiguracao = true;
        config.abrir();
        leitores.values().forEach(LeitorNavegacao::desarmar);   // o botao que avancou nao vale na outra tela
    }

    /** Volta das configuracoes para os lutadores: as escolhas e o PRONTO dos dois ficam. */
    private void voltarAosLutadores() {
        naConfiguracao = false;
        leitores.values().forEach(LeitorNavegacao::desarmar);
    }

    // =====================================================
    // 5Q.4: MOUSE (o mouse age pelo J1, e pelo J2 so na propria faixa/kimono)
    // =====================================================
    private void instalarMouse() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent ev) {
                clicar(ev.getX(), ev.getY());
            }
        });
    }

    /** Um clique (testavel sem janela): personagem, faixa/kimono, AVANCAR ou a segunda etapa. */
    void clicar(int x, int y) {
        if (iniciada) {
            return;
        }
        if (naConfiguracao) {
            switch (config.clicar(x, y, tempoMs)) {
                case INICIAR -> iniciar();
                case VOLTAR -> voltarAosLutadores();
                default -> {
                }
            }
            repaint();
            return;
        }
        SlotJogador quem = modo == Modo.TREINO ? slotDaVez() : SlotJogador.JOGADOR_1;
        clicarLutadores(x, y, quem);
        repaint();
    }

    /** Primeira etapa: o mouse age pelo J1 (no treino, pelo lutador da vez), nas faixas/kimonos e no AVANCAR. */
    private void clicarLutadores(int x, int y, SlotJogador quem) {
        Jogador alvo = jogadores.get(quem);
        for (int i = 0; i < COLUNAS * LINHAS; i++) {
            if (celula(i).contains(x, y)) {
                if (alvo.fase == Fase.ESCOLHENDO) {
                    alvo.cursor = i;
                    comandar(quem, Comando.CONFIRMAR);
                }
                return;
            }
        }
        Jogador j1 = jogadores.get(SlotJogador.JOGADOR_1);
        Jogador j2 = jogadores.get(SlotJogador.JOGADOR_2);
        if (y >= 84 && y < 112 && x >= 10 && x < 230) {
            clicarVisual(j1, y < 98, (x % 230) < 120 ? -1 : 1);
        } else if (y >= 84 && y < 112 && x >= 570 && x < 790) {
            clicarVisual(j2, y < 98, x < 680 ? -1 : 1);
        } else if (BOTAO_AVANCAR.contains(x, y)) {
            avancar();
        }
    }

    private void clicarVisual(Jogador j, boolean faixa, int passo) {
        if ((j.fase == Fase.FAIXA || j.fase == Fase.KIMONO) && faixa == (j.fase == Fase.FAIXA)) {
            mudarVisual(j, faixa, passo);
        }
    }

    static final Rectangle BOTAO_AVANCAR = new Rectangle(320, 364, 160, 34);

    /** 5Q.1: proxima faixa ou proximo kimono do slot (circular). */
    private void mudarVisual(Jogador j, boolean faixa, int passo) {
        if (faixa) {
            j.faixa = com.irmaosmiau.regras.Graduacao.values()[Math.floorMod(j.faixa.ordinal() + passo, com.irmaosmiau.regras.Graduacao.values().length)];
            return;
        }
        com.irmaosmiau.entities.CorKimono atual = kimonoEfetivo(j);
        int i = atual == null ? 0 : atual.ordinal();
        j.kimono = com.irmaosmiau.entities.CorKimono.values()[Math.floorMod(i + passo, com.irmaosmiau.entities.CorKimono.values().length)];
    }

    /** Kimono que o slot vai usar (o escolhido ou o padrao do personagem). */
    com.irmaosmiau.entities.CorKimono kimonoEfetivo(SlotJogador slot) {
        return kimonoEfetivo(jogadores.get(slot));
    }

    private com.irmaosmiau.entities.CorKimono kimonoEfetivo(Jogador j) {
        if (j.kimono != null) {
            return j.kimono;
        }
        EscolhaLutador e = escolhaBase(j);
        return e == null ? null : padraoKimono(e);
    }

    private final transient Map<String, com.irmaosmiau.entities.CorKimono> kimonosPadrao = new HashMap<>();

    private com.irmaosmiau.entities.CorKimono padraoKimono(EscolhaLutador e) {
        return kimonosPadrao.computeIfAbsent(e.getIdBase(), k -> e.getBase().criarLutador(0, 0, 0.5).getCorKimonoPadrao());
    }

    /** TREINO: com o lutador pronto, o cursor do parceiro comeca no outro irmao (como no treino antigo). */
    private void parceiroPadrao() {
        Jogador p = jogadores.get(SlotJogador.JOGADOR_2);
        if (p.fase != Fase.ESCOLHENDO) {
            return;
        }
        EscolhaLutador j = escolha(SlotJogador.JOGADOR_1);
        String outro = j != null && CatalogoPersonagens.ID_PRETO.equals(j.getIdBase())
                ? CatalogoPersonagens.ID_BRANCO : CatalogoPersonagens.ID_PRETO;
        for (int i = 0; i < elenco.size(); i++) {
            if (elenco.get(i).getId().equals(outro)) {
                p.cursor = i;
            }
        }
    }

    private void iniciar() {
        if (iniciada) {
            return;
        }
        iniciada = true;
        timer.stop();
        if (modo == Modo.TREINO) {
            aoIniciarTreino.iniciar(escolha(SlotJogador.JOGADOR_1), escolha(SlotJogador.JOGADOR_2), config.idArena(),
                    config.inicio());
            return;
        }
        aoIniciar.iniciar(escolha(SlotJogador.JOGADOR_1), escolha(SlotJogador.JOGADOR_2), config.idArena(),
                config.segundos(), config.modoLuta());
    }

    private void voltarAoMenu() {
        if (iniciada) {
            return;
        }
        iniciada = true;
        timer.stop();
        if (aoVoltar != null) {
            aoVoltar.run();
        }
    }

    /**
     * Escolha confirmada (ou em escolha) do jogador; null num espaco bloqueado. O personagem original leva
     * a faixa e o kimono do slot; o PERFIL vai como foi salvo (aparencia e atributos do editor, faixa
     * branca sem bonus): nenhuma escolha do menu muda o perfil.
     */
    EscolhaLutador escolha(SlotJogador slot) {
        Jogador j = jogadores.get(slot);
        EscolhaLutador e = escolhaBase(j);
        if (e == null) {
            return null;
        }
        return e.isOficial() ? e.com(j.faixa, j.kimono) : e;
    }

    /** A escolha sem faixa/kimono: o perfil em destaque/confirmado ou o personagem do cursor. */
    private EscolhaLutador escolhaBase(Jogador j) {
        if ((j.fase == Fase.PERFIL || j.personalizado) && !j.perfis.isEmpty()) {
            return j.perfis.get(j.perfil);
        }
        DefinicaoPersonagem d = personagemEm(j.cursor);
        return d == null ? null : EscolhaLutador.oficial(d);
    }

    /** O jogador esta com um perfil (escolhendo ou confirmado)? */
    boolean comPerfil(SlotJogador s) {
        EscolhaLutador e = escolha(s);
        return e != null && !e.isOficial();
    }

    private DefinicaoPersonagem personagemEm(int i) {
        return i >= 0 && i < elenco.size() ? elenco.get(i) : null;
    }

    // =====================================================
    // TEMPO / CICLO DE VIDA
    // =====================================================
    @Override
    public boolean aceitaNavegacao() {
        return false;                                           // a tela le os dois jogadores sozinha
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
        requestFocusInWindow();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    void tick(long ms) {
        tempoMs += ms;
        lerGamepads();
        for (SlotJogador s : SlotJogador.values()) {
            atualizarPrevia(s);
        }
        repaint();
    }

    /** Previa grande: o lutador oficial (ou o perfil) em idle, recriado so quando muda. */
    private void atualizarPrevia(SlotJogador s) {
        Jogador j = jogadores.get(s);
        EscolhaLutador e = escolha(s);
        String id = e == null ? null : e.getId() + "|" + e.getGraduacao() + "|" + e.getKimono();
        if (id == null) {
            j.previa = null;
            j.idPrevia = null;
            return;
        }
        if (!id.equals(j.idPrevia)) {
            boolean esq = s == SlotJogador.JOGADOR_1;
            j.previa = e.criarLutador(esq ? 88 : 648, 108, 0.7);
            j.previa.setViradoParaDireita(esq);
            ArteMenus.apresentacao(j.previa);
            j.idPrevia = id;
        }
        j.previa.atualizarApresentacao();                // 5M.3: pose fixa, so a piscada
    }

    // =====================================================
    // DESENHO
    // =====================================================
    static Rectangle celula(int i) {
        int c = i % COLUNAS;
        int l = i / COLUNAS;
        return new Rectangle(GRADE_X + c * (CELULA + ESPACO), GRADE_Y + l * (CELULA + ESPACO), CELULA, CELULA);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = ArteMenus.suave(g0);
        g.setPaint(new GradientPaint(0, 0, new Color(40, 110, 200), 0, 450, ArteMenus.AZUL_ESCURO));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(255, 255, 255, 18));
        for (int x = -450; x < 800; x += 36) {
            g.fillPolygon(new int[] {x, x + 18, x + 468, x + 450}, new int[] {0, 0, 450, 450}, 4);
        }
        if (naConfiguracao) {
            config.desenhar(g, tempoMs, resumo(SlotJogador.JOGADOR_1), resumo(SlotJogador.JOGADOR_2));
            g.dispose();
            return;
        }
        ArteMenus.letreiro(g, modo == Modo.TREINO ? "TREINO: ESCOLHA OS LUTADORES" : "ESCOLHA SEU LUTADOR", 400, 44,
                new Font("Arial", Font.BOLD, 26), ArteMenus.AMARELO);

        painelJogador(g, SlotJogador.JOGADOR_1, 10);
        painelJogador(g, SlotJogador.JOGADOR_2, 570);
        grade(g);
        rodapePartida(g);
        g.dispose();
    }

    private void grade(Graphics2D g) {
        for (int i = 0; i < COLUNAS * LINHAS; i++) {
            Rectangle r = celula(i);
            DefinicaoPersonagem d = personagemEm(i);
            g.setColor(d == null ? new Color(14, 26, 60) : new Color(70, 140, 225));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            if (d == null) {
                silhueta(g, r);
                cadeado(g, r.x + r.width - 18, r.y + r.height - 20);
            } else {
                retrato(g, d, r);
            }
            g.setColor(new Color(160, 200, 255, 120));
            g.setStroke(new BasicStroke(1));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        }
        // cursores: J1 por fora (azul), J2 por dentro (vermelho); juntos continuam distintos
        Rectangle r1 = celula(jogadores.get(SlotJogador.JOGADOR_1).cursor);
        Rectangle r2 = celula(jogadores.get(SlotJogador.JOGADOR_2).cursor);
        cursor(g, r1, -4, ArteMenus.J1, "1", true, jogadores.get(SlotJogador.JOGADOR_1).fase);
        if (modo == Modo.PARTIDA || slotDaVez() == SlotJogador.JOGADOR_2) {   // treino: o parceiro quando e a vez
            cursor(g, r2, 2, ArteMenus.J2, "2", false, jogadores.get(SlotJogador.JOGADOR_2).fase);
        }
    }

    private void cursor(Graphics2D g, Rectangle r, int d, Color cor, String rotulo, boolean esquerda, Fase fase) {
        g.setColor(cor);
        g.setStroke(new BasicStroke(fase == Fase.ESCOLHENDO ? 3 : 4));
        g.drawRoundRect(r.x + d, r.y + d, r.width - 2 * d, r.height - 2 * d, 8, 8);
        int tx = esquerda ? r.x - 6 : r.x + r.width - 10;
        g.fillOval(tx, r.y - 8, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.drawString(rotulo, tx + 5, r.y + 4);
    }

    private static void silhueta(Graphics2D g, Rectangle r) {
        g.setColor(new Color(40, 54, 90));
        int cx = r.x + r.width / 2;
        g.fillOval(cx - 12, r.y + 10, 24, 22);
        g.fillPolygon(new int[] {cx - 12, cx - 6, cx - 11}, new int[] {r.y + 16, r.y + 12, r.y + 4}, 3);
        g.fillPolygon(new int[] {cx + 12, cx + 6, cx + 11}, new int[] {r.y + 16, r.y + 12, r.y + 4}, 3);
        g.fillRoundRect(cx - 16, r.y + 32, 32, 24, 8, 8);
        g.setColor(new Color(110, 130, 170));
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("?", cx - 4, r.y + 27);
    }

    private static void cadeado(Graphics2D g, int x, int y) {
        g.setColor(new Color(220, 190, 80));
        g.setStroke(new BasicStroke(2.2f));
        g.drawArc(x + 2, y - 1, 10, 12, 0, 180);
        g.fillRoundRect(x, y + 5, 14, 11, 3, 3);
        g.setColor(new Color(90, 70, 20));
        g.fillOval(x + 5, y + 8, 4, 4);
    }

    /** Retrato: cabeca e tronco do proprio renderizador, recortados na celula. */
    private final transient Map<String, IrmaoMiau> retratos = new HashMap<>();

    private void retrato(Graphics2D g0, DefinicaoPersonagem d, Rectangle r) {
        IrmaoMiau l = retratos.computeIfAbsent(d.getId() + "@" + r.x + "," + r.y,
                k -> EscolhaLutador.oficial(d).criarLutador(r.x + r.width / 2 - 25, r.y + 2, 0.62));
        l.setViradoParaDireita(true);
        ArteMenus.apresentacao(l);
        Graphics2D g = (Graphics2D) g0.create();
        g.clip(new java.awt.geom.RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 8, 8));
        ArteMenus.desenharLutador(g, l);
        g.dispose();
    }

    private void painelJogador(Graphics2D g, SlotJogador s, int x) {
        Jogador j = jogadores.get(s);
        Color cor = s == SlotJogador.JOGADOR_1 ? ArteMenus.J1 : ArteMenus.J2;
        g.setColor(ArteMenus.AZUL_PAINEL);
        g.fillRoundRect(x, 56, 220, 386, 14, 14);
        g.setColor(cor);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(x, 56, 220, 386, 14, 14);
        boolean treino = modo == Modo.TREINO;
        g.setFont(new Font("Arial", Font.BOLD, treino ? 15 : 20));
        g.drawString(treino ? (s == SlotJogador.JOGADOR_1 ? "LUTADOR" : "PARCEIRO")
                : s == SlotJogador.JOGADOR_1 ? "J1" : "J2", x + 10, 80);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        boolean aguardaVez = treino && s == SlotJogador.JOGADOR_2 && slotDaVez() == SlotJogador.JOGADOR_1;
        g.setColor(j.fase == Fase.PRONTO ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        String fase = aguardaVez ? "A SEGUIR"
                : switch (j.fase) {
                    case PRONTO -> "PRONTO!";
                    case PERFIL -> "ESCOLHA O PERFIL";
                    case FAIXA -> "ESCOLHA A FAIXA";
                    case KIMONO -> "ESCOLHA O KIMONO";
                    default -> "ESCOLHENDO";
                };
        g.drawString(fase, x + 210 - g.getFontMetrics().stringWidth(fase), 78);

        visual(g, s, j, x);

        // previa
        g.setColor(new Color(255, 255, 255, 30));
        g.fillOval(x + 40, 268, 140, 18);
        EscolhaLutador e = escolha(s);
        DefinicaoPersonagem d = e == null ? null : e.getBase();
        if (j.previa != null) {
            ArteMenus.desenharLutador(g, j.previa);
        } else {
            Rectangle grande = new Rectangle(x + 60, 112, 100, 100);
            g.setColor(new Color(10, 22, 52));
            g.fillRoundRect(grande.x, grande.y, grande.width, grande.height + 60, 16, 16);
            g.setColor(new Color(40, 54, 90));
            g.fillOval(x + 85, 128, 50, 46);
            g.fillRoundRect(x + 78, 176, 64, 70, 14, 14);
            cadeado(g, x + 102, 246);
        }
        if (j.avisoBloqueadoAte > tempoMs || j.avisoSemPerfisAte > tempoMs) {
            g.setColor(new Color(200, 40, 40, 220));
            g.fillRoundRect(x + 20, 190, 180, 24, 10, 10);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            ArteMenus.centralizado(g, j.avisoBloqueadoAte > tempoMs ? "BLOQUEADO" : "SEM PERFIS DESTE MIAU",
                    x + 110, 207);
        }

        // nome, estilo, perfil e atributos (dados reais do catalogo/perfil)
        boolean perfil = e != null && !e.isOficial();
        boolean escolhendoPerfil = perfil && j.fase == Fase.PERFIL;
        g.setColor(escolhendoPerfil ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        String nome = d == null ? "???" : perfil ? e.getNome() : nomeExibido(d);
        if (escolhendoPerfil) {
            nome = "< " + nome + " >";
        }
        if (g.getFontMetrics().stringWidth(nome) > 206) {
            g.setFont(new Font("Arial", Font.BOLD, 12));
        }
        ArteMenus.centralizado(g, nome, x + 110, 304);
        g.setColor(ArteMenus.BRANCO);
        g.setFont(ArteMenus.PEQUENO);
        ArteMenus.centralizado(g, d == null ? "Em breve" : perfil ? "Base: " + nomeBase(d) : "Estilo: " + d.getEstilo(),
                x + 110, 319);
        if (perfil) {
            g.setColor(new Color(190, 205, 235));
            ArteMenus.centralizado(g, escolhendoPerfil ? "Perfil " + (j.perfil + 1) + " / " + j.perfis.size()
                    + " · confirme" : "Perfil personalizado", x + 110, 333);
        }
        if (e != null) {
            atributos(g, e.getAtributos(), x + 12, 344);
        }
    }

    /**
     * 5Q.1: "FAIXA < AZUL >" e "KIMONO < PRETO >" do slot, com a linha em foco destacada. Com um perfil as
     * duas linhas ficam indisponiveis: valem a aparencia e a faixa visual salvas no editor.
     */
    private void visual(Graphics2D g, SlotJogador s, Jogador j, int x) {
        if (comPerfil(s)) {
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.setColor(new Color(150, 165, 200));
            g.drawString("FAIXA", x + 12, 94);
            g.drawString("KIMONO", x + 12, 108);
            String t = "DO PERFIL";
            g.drawString(t, x + 208 - g.getFontMetrics().stringWidth(t), 94);
            g.drawString(t, x + 208 - g.getFontMetrics().stringWidth(t), 108);
            return;
        }
        com.irmaosmiau.entities.CorKimono k = kimonoEfetivo(j);
        linhaVisual(g, "FAIXA", j.faixa.name(), x, 94, j.fase == Fase.FAIXA);
        linhaVisual(g, "KIMONO", k == null ? "-" : k.getNome().toUpperCase(Locale.ROOT), x, 108, j.fase == Fase.KIMONO);
    }

    private static void linhaVisual(Graphics2D g, String rotulo, String valor, int x, int y, boolean foco) {
        if (foco) {
            g.setColor(new Color(255, 206, 46, 60));
            g.fillRoundRect(x + 6, y - 10, 208, 13, 6, 6);
        }
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.setColor(foco ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        g.drawString(rotulo, x + 12, y);
        String v = (foco ? "< " : "") + valor + (foco ? " >" : "");
        g.drawString(v, x + 208 - g.getFontMetrics().stringWidth(v), y);
    }

    // ---- 5Q.1: acesso para testes
    com.irmaosmiau.regras.Graduacao faixa(SlotJogador s) {
        return jogadores.get(s).faixa;
    }

    /** "Irmão Miau Branco" -> "Miau Branco" (a base de um perfil). */
    private static String nomeBase(DefinicaoPersonagem d) {
        return d.getNome().replaceFirst("^Irmão ", "");
    }

    /** "Irmão Miau Branco" -> "MIAU BRANCO". */
    static String nomeExibido(DefinicaoPersonagem d) {
        return d.getNome().replaceFirst("^Irmão ", "").toUpperCase(Locale.ROOT);
    }

    private static void atributos(Graphics2D g, AtributosLutador a, int x, int y) {
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        int i = 0;
        for (LimitesAtributos.Limite l : LimitesAtributos.todos()) {
            double v = l.ler(a);
            int yy = y + i * 12;
            g.setColor(ArteMenus.BRANCO);
            g.drawString(PerfisPanel.nomeVisivel(l.getNome()), x, yy + 9);
            g.setColor(new Color(10, 22, 52));
            g.fillRect(x + 92, yy + 2, 80, 7);
            double frac = (v - l.getMinimo()) / Math.max(1e-9, l.getMaximo() - l.getMinimo());
            g.setColor(ArteMenus.AMARELO);
            g.fillRect(x + 92, yy + 2, (int) Math.round(80 * Math.max(0, Math.min(1, frac))), 7);
            g.setColor(ArteMenus.BRANCO);
            g.drawString(v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v), x + 178, yy + 9);
            i++;
        }
    }

    /** Primeira etapa: quem esta pronto, o AVANCAR (habilitado so com os dois), o atalho dos perfis e as dicas. */
    private void rodapePartida(Graphics2D g) {
        g.setColor(ArteMenus.AZUL_PAINEL);
        g.fillRoundRect(240, 258, 320, 184, 14, 14);
        int y = 290;
        for (SlotJogador s : SlotJogador.values()) {
            Fase f = jogadores.get(s).fase;
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.setColor(s == SlotJogador.JOGADOR_1 ? ArteMenus.J1 : ArteMenus.J2);
            g.drawString(modo == Modo.TREINO ? (s == SlotJogador.JOGADOR_1 ? "LUTADOR" : "PARCEIRO")
                    : s == SlotJogador.JOGADOR_1 ? "J1" : "J2", 262, y);
            g.setColor(f == Fase.PRONTO ? ArteMenus.AMARELO : ArteMenus.BRANCO);
            String t = f == Fase.PRONTO ? (comPerfil(s) ? "PRONTO · PERFIL" : "PRONTO")
                    : modo == Modo.TREINO ? "AGUARDANDO" : "AGUARDANDO CONFIRMAÇÃO";
            g.drawString(t, 538 - g.getFontMetrics().stringWidth(t), y);
            y += 30;
        }
        boolean prontos = ambosProntos();
        dicaPerfil(g, 400, 352);
        g.setColor(prontos ? new Color(255, 206, 46, 235) : new Color(30, 70, 150));
        g.fillRoundRect(BOTAO_AVANCAR.x, BOTAO_AVANCAR.y, BOTAO_AVANCAR.width, BOTAO_AVANCAR.height, 12, 12);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(prontos ? ArteMenus.AZUL_ESCURO : new Color(120, 135, 175));
        ArteMenus.centralizado(g, "AVANÇAR", 400, 388);
        g.setFont(ArteMenus.PEQUENO);
        String dica;
        if (avisoAguardandoAte > tempoMs) {
            g.setColor(ArteMenus.AMARELO);
            dica = "AGUARDANDO O OUTRO JOGADOR FICAR PRONTO";
        } else {
            g.setColor(ArteMenus.BRANCO);
            boolean algumPronto = jogadores.values().stream().anyMatch(x -> x.fase == Fase.PRONTO);
            if (modo == Modo.TREINO) {
                dica = prontos ? "Confirme AVANÇAR" : slotDaVez() == SlotJogador.JOGADOR_2
                        ? "Agora escolha o parceiro · cancelar volta ao lutador"
                        : "Um controle escolhe os dois: primeiro o lutador";
            } else {
                dica = prontos ? "Confirme AVANÇAR (J1 ou J2)" : algumPronto ? "Aguardando o outro jogador"
                        : "Personagem · faixa · kimono: confirme cada um";
            }
        }
        ArteMenus.centralizado(g, dica, 400, 416);
        g.setColor(new Color(190, 205, 235));
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        ArteMenus.centralizado(g, modo == Modo.TREINO ? "WASD/setas · ESPAÇO/ENTER confirma · Q volta · E/J perfil · ESC menu"
                : "J1 WASD·ESPAÇO·Q·E   J2 setas·ENTER·BACKSPACE·J   ESC menu", 400, 433);
    }

    /** Dica discreta "X / [quadrado] — ESCOLHER PERFIL" (o quadrado desenhado: o caractere sai pequeno demais). */
    private static void dicaPerfil(Graphics2D g, int cx, int y) {
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(new Color(255, 220, 120));
        java.awt.FontMetrics fm = g.getFontMetrics();
        String a = "X / ";
        String b = "  —  ESCOLHER PERFIL";
        int lado = 9;
        int x = cx - (fm.stringWidth(a) + lado + fm.stringWidth(b)) / 2;
        g.drawString(a, x, y);
        x += fm.stringWidth(a);
        g.setStroke(new BasicStroke(1.6f));
        g.drawRect(x, y - lado, lado, lado);
        g.drawString(b, x + lado, y);
    }

    /** "MIAU BRANCO · AZUL · KIMONO PRETO" (resumo na segunda etapa). */
    private String resumo(SlotJogador s) {
        EscolhaLutador e = escolha(s);
        if (e == null) {
            return "-";
        }
        String nome = e.isOficial() ? nomeExibido(e.getBase()) : e.getNome().toUpperCase(Locale.ROOT);
        if (!e.isOficial()) {
            return nome + " · PERFIL";                        // aparencia e faixa visual do perfil
        }
        com.irmaosmiau.entities.CorKimono k = kimonoEfetivo(s);
        return nome + " · " + jogadores.get(s).faixa.name()
                + (k == null ? "" : " · KIMONO " + k.getNome().toUpperCase(Locale.ROOT));
    }

    /** "SEM LIMITE", "1 MIN", "1 MIN 30 S", "10 MIN". */
    static String textoTempo(int s) {
        if (s <= 0) {
            return "SEM LIMITE";
        }
        return s / 60 + " MIN" + (s % 60 == 0 ? "" : " " + s % 60 + " S");
    }

    // =====================================================
    // ACESSO PARA TESTES
    // =====================================================
    Fase fase(SlotJogador s) {
        return jogadores.get(s).fase;
    }

    int cursor(SlotJogador s) {
        return jogadores.get(s).cursor;
    }

    String mapaAtual() {
        return config.idArena();
    }

    int tempoAtual() {
        return modo == Modo.TREINO ? 0 : config.segundos();          // treino: sem limite
    }

    boolean naConfiguracao() {
        return naConfiguracao;
    }

    ConfiguracaoLuta configuracao() {
        return config;
    }

    Modo modo() {
        return modo;
    }

    InicioLuta inicioAtual() {
        return config.inicio();
    }

    boolean mapaTemOpcoes() {
        return config.temVariasArenas();
    }

    /** Quantos perfis a lista aberta tem (0 = fechada) e o nome do perfil em destaque. */
    List<String> perfisAbertos(SlotJogador s) {
        List<String> l = new ArrayList<>();
        for (EscolhaLutador e : jogadores.get(s).perfis) {
            l.add(e.getNome());
        }
        return l;
    }

    boolean isAvisoSemPerfis(SlotJogador s) {
        return jogadores.get(s).avisoSemPerfisAte > tempoMs;
    }

    boolean isAvisoBloqueado(SlotJogador s) {
        return jogadores.get(s).avisoBloqueadoAte > tempoMs;
    }

    IrmaoMiau previa(SlotJogador s) {
        return jogadores.get(s).previa;
    }

    /** Aciona uma tecla de verdade (pressed/released) pelos mapas de entrada. */
    void tecla(String nome, boolean pressionada) {
        Object id = getInputMap(WHEN_IN_FOCUSED_WINDOW).get(
                KeyStroke.getKeyStroke((pressionada ? "pressed " : "released ") + nome));
        getActionMap().get(id).actionPerformed(null);
    }
}
