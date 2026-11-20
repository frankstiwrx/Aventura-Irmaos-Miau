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
 * Fases de cada jogador: ESCOLHENDO -> (PERFIL, se houver perfis do irmao) ->
 * PRONTO. Pronto, o J1 ajusta a partida e confirma LUTAR (o J2 precisa estar
 * pronto). Cancelar volta uma fase; cancelar escolhendo volta ao menu.
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

    /** Tempos de luta oferecidos (0 = sem limite). */
    public static final int[] TEMPOS = {0, 60, 90, 180};

    /** Fase de um jogador. */
    public enum Fase { ESCOLHENDO, PERFIL, PRONTO }

    /** Linhas de configuracao (o J1 navega quando esta pronto). */
    enum Linha { MAPA, TEMPO, DIFICULDADE, PARCEIRO, INICIO, LUTAR,
        /** 5Q.1: faixa e kimono do J1 e (no treino) do parceiro. */
        FAIXA, KIMONO, FAIXA_PARCEIRO, KIMONO_PARCEIRO }

    private static final class Jogador {
        int cursor;
        Fase fase = Fase.ESCOLHENDO;
        List<EscolhaLutador> perfis = List.of();
        int perfil;
        long avisoBloqueadoAte = -1;
        IrmaoMiau previa;
        String idPrevia;
        /** 5Q.1: faixa (fonte dos atributos) e kimono (null = padrao do personagem) deste slot. */
        com.irmaosmiau.regras.Graduacao faixa = com.irmaosmiau.regras.Graduacao.BRANCA;
        com.irmaosmiau.entities.CorKimono kimono;
        /** 5Q.1: linha do J2 na partida (0 = faixa, 1 = kimono). */
        int linhaVisual;
    }

    private final transient List<DefinicaoPersonagem> elenco = new ArrayList<>();
    private final transient OpcoesDeLutadores opcoes;
    private final transient AoIniciar aoIniciar;
    private final transient AoIniciarTreino aoIniciarTreino;
    private final Modo modo;
    /* 5M.3 (treino): parceiros possiveis (oficiais + perfis), o escolhido e o inicio. */
    private final transient List<EscolhaLutador> parceiros = new ArrayList<>();
    private int parceiro;
    private int inicio;
    private final transient Runnable aoVoltar;
    private final transient Supplier<ProvedorGamepads> provedor;
    private final transient Supplier<AssociacaoDispositivos> associacao;
    private final transient Map<SlotJogador, Jogador> jogadores = new EnumMap<>(SlotJogador.class);
    private final transient Map<String, LeitorNavegacao> leitores = new HashMap<>();
    private final transient Set<String> segurando = new HashSet<>();
    private final List<String> mapas;
    private final List<String> nomesMapas = new ArrayList<>();
    private int mapa;
    private int tempo;
    private Linha linhaJ1 = Linha.LUTAR;
    private long tempoMs;
    private long avisoAguardandoAte = -1;
    private boolean iniciada;
    private final Timer timer = new Timer(16, e -> tick(16));

    public SelecaoPartidaPanel(CatalogoPersonagens catalogo, OpcoesDeLutadores opcoes, RegistroCenarios cenarios,
            String idCenarioInicial, AoIniciar aoIniciar, Runnable aoVoltar,
            Supplier<ProvedorGamepads> provedor, Supplier<AssociacaoDispositivos> associacao) {
        this(Modo.PARTIDA, catalogo, opcoes, cenarios, idCenarioInicial, aoIniciar, null, null, aoVoltar, provedor,
                associacao);
    }

    /**
     * 5M.3: a selecao do TREINO. So o J1 escolhe (teclado do J1 ou do J2 e
     * qualquer gamepad movem o mesmo cursor); o parceiro passivo, o mapa e o
     * inicio (de joelhos / em pe) sao linhas de configuracao. Sem TEMPO nem
     * DIFICULDADE.
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
        mapas = List.copyOf(cenarios.ids());
        for (String id : mapas) {
            nomesMapas.add(cenarios.exigir(id).getNome().toUpperCase(Locale.ROOT));
        }
        mapa = Math.max(0, mapas.indexOf(idCenarioInicial));
        for (SlotJogador s : SlotJogador.values()) {
            Jogador j = new Jogador();
            j.cursor = Math.min(s.ordinal(), elenco.size() - 1);    // J1 no 1o, J2 no 2o
            jogadores.put(s, j);
        }
        if (modo == Modo.TREINO) {
            for (DefinicaoPersonagem d : elenco) {
                parceiros.addAll(opcoes.paraBase(d.getId()).getValor());
            }
            inicio = Math.max(0, List.of(InicioLuta.values()).indexOf(
                    inicioInicial == null ? InicioLuta.PADRAO_TELAS : inicioInicial));
            parceiroPadrao();
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
        teclas(SlotJogador.JOGADOR_2, Comando.ANTERIOR, "UP");
        teclas(SlotJogador.JOGADOR_2, Comando.PROXIMO, "DOWN");
        teclas(SlotJogador.JOGADOR_2, Comando.ESQUERDA, "LEFT");
        teclas(SlotJogador.JOGADOR_2, Comando.DIREITA, "RIGHT");
        teclas(SlotJogador.JOGADOR_2, Comando.CONFIRMAR, "ENTER", "END");
        teclas(SlotJogador.JOGADOR_2, Comando.VOLTAR, "BACK_SPACE");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed ESCAPE"), "voltarMenu");
        getActionMap().put("voltarMenu", acao(() -> voltarAoMenu()));
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
            comGamepad.clear();                                 // treino: todo gamepad move o J1
            for (int i = 0; i < conectados.size(); i++) {
                comGamepad.add(SlotJogador.JOGADOR_1);
            }
        }
        for (int i = 0; i < conectados.size() && i < comGamepad.size(); i++) {
            InfoGamepad g = conectados.get(i);
            EstadoGamepad e = p.estado(g.chave()).orElse(null);
            LeitorNavegacao l = leitores.computeIfAbsent(g.chave(), k -> new LeitorNavegacao());
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
        if (modo == Modo.TREINO) {
            slot = SlotJogador.JOGADOR_1;                       // treino: um cursor so
        }
        Jogador j = jogadores.get(slot);
        switch (j.fase) {
            case ESCOLHENDO -> escolhendo(slot, j, c);
            case PERFIL -> perfil(j, c);
            default -> pronto(slot, j, c);
        }
        repaint();
    }

    private void escolhendo(SlotJogador slot, Jogador j, Comando c) {
        int total = COLUNAS * LINHAS;
        switch (c) {
            case ANTERIOR -> j.cursor = j.cursor - COLUNAS >= 0 ? j.cursor - COLUNAS : j.cursor;
            case PROXIMO -> j.cursor = j.cursor + COLUNAS < total ? j.cursor + COLUNAS : j.cursor;
            case ESQUERDA -> j.cursor = j.cursor % COLUNAS == 0 ? j.cursor + COLUNAS - 1 : j.cursor - 1;
            case DIREITA -> j.cursor = j.cursor % COLUNAS == COLUNAS - 1 ? j.cursor - COLUNAS + 1 : j.cursor + 1;
            case CONFIRMAR -> {
                DefinicaoPersonagem d = personagemEm(j.cursor);
                if (d == null) {
                    j.avisoBloqueadoAte = tempoMs + 700;          // bloqueado: so um aviso
                    return;
                }
                j.perfis = opcoes.paraBase(d.getId()).getValor();
                j.perfil = 0;
                j.kimono = null;                                  // 5Q.1: kimono padrao do novo personagem
                j.fase = j.perfis.size() > 1 ? Fase.PERFIL : Fase.PRONTO;
                if (j.fase == Fase.PRONTO && slot == SlotJogador.JOGADOR_1) {
                    linhaJ1 = Linha.FAIXA;                        // 5Q.4: personaliza antes de lutar
                    parceiroPadrao();
                }
            }
            case VOLTAR -> voltarAoMenu();
            default -> {
            }
        }
    }

    private void perfil(Jogador j, Comando c) {
        switch (c) {
            case ESQUERDA -> j.perfil = Math.floorMod(j.perfil - 1, j.perfis.size());
            case DIREITA -> j.perfil = Math.floorMod(j.perfil + 1, j.perfis.size());
            case CONFIRMAR -> {
                j.fase = Fase.PRONTO;
                if (j == jogadores.get(SlotJogador.JOGADOR_1)) {
                    linhaJ1 = Linha.FAIXA;                        // 5Q.4
                    parceiroPadrao();
                }
            }
            case VOLTAR -> j.fase = Fase.ESCOLHENDO;
            default -> {
            }
        }
    }

    private void pronto(SlotJogador slot, Jogador j, Comando c) {
        if (c == Comando.VOLTAR) {
            j.fase = Fase.ESCOLHENDO;
            return;
        }
        if (slot != SlotJogador.JOGADOR_1) {
            // 5Q.1: o J2 so escolhe a propria faixa e o proprio kimono (a partida e configurada pelo J1)
            switch (c) {
                case ANTERIOR, PROXIMO -> j.linhaVisual = 1 - j.linhaVisual;
                case ESQUERDA -> mudarVisual(slot, j, j.linhaVisual == 0, -1);
                case DIREITA -> mudarVisual(slot, j, j.linhaVisual == 0, 1);
                default -> {
                }
            }
            return;
        }
        switch (c) {
            case ANTERIOR -> linhaJ1 = linhaVizinha(-1);
            case PROXIMO -> linhaJ1 = linhaVizinha(1);
            case ESQUERDA -> mudarValor(-1);
            case DIREITA -> mudarValor(1);
            case CONFIRMAR -> {
                if (linhaJ1 != Linha.LUTAR) {
                    linhaJ1 = proximaDaPersonalizacao(linhaJ1);   // 5Q.4: FAIXA -> KIMONO -> ... -> LUTAR
                } else if (modo == Modo.PARTIDA && jogadores.get(SlotJogador.JOGADOR_2).fase != Fase.PRONTO) {
                    avisoAguardandoAte = tempoMs + 900;
                } else {
                    iniciar();
                }
            }
            default -> {
            }
        }
    }

    /**
     * 5Q.4: confirmar numa linha de personalizacao segue para a proxima (faixa, kimono e, no
     * treino, faixa e kimono do parceiro); nas outras linhas volta para LUTAR, como antes.
     */
    private Linha proximaDaPersonalizacao(Linha l) {
        return switch (l) {
            case FAIXA -> Linha.KIMONO;
            case KIMONO -> modo == Modo.TREINO ? Linha.FAIXA_PARCEIRO : Linha.LUTAR;
            case FAIXA_PARCEIRO -> Linha.KIMONO_PARCEIRO;
            default -> Linha.LUTAR;
        };
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

    /** Um clique (testavel sem janela): personagem, faixa/kimono, configuracoes ou LUTAR. */
    void clicar(int x, int y) {
        if (iniciada) {
            return;
        }
        SlotJogador j1 = SlotJogador.JOGADOR_1;
        Jogador J1 = jogadores.get(j1);
        for (int i = 0; i < COLUNAS * LINHAS; i++) {
            if (celula(i).contains(x, y)) {
                if (J1.fase == Fase.ESCOLHENDO) {
                    J1.cursor = i;
                    comandar(j1, Comando.CONFIRMAR);
                }
                repaint();
                return;
            }
        }
        int passo = (x % 230) < 120 ? -1 : 1;
        if (y >= 84 && y < 112 && x >= 10 && x < 230 && J1.fase == Fase.PRONTO) {
            linhaJ1 = y < 98 ? Linha.FAIXA : Linha.KIMONO;
            mudarValor(passo);
        } else if (y >= 84 && y < 112 && x >= 570 && x < 790) {
            if (modo == Modo.TREINO && J1.fase == Fase.PRONTO) {
                linhaJ1 = y < 98 ? Linha.FAIXA_PARCEIRO : Linha.KIMONO_PARCEIRO;
                mudarValor(x < 680 ? -1 : 1);
            } else if (modo == Modo.PARTIDA && jogadores.get(SlotJogador.JOGADOR_2).fase == Fase.PRONTO) {
                Jogador j2 = jogadores.get(SlotJogador.JOGADOR_2);
                j2.linhaVisual = y < 98 ? 0 : 1;
                mudarVisual(SlotJogador.JOGADOR_2, j2, j2.linhaVisual == 0, x < 680 ? -1 : 1);
            }
        } else if (J1.fase == Fase.PRONTO && x >= 250 && x < 550 && y >= 266 && y < 356) {
            Linha[] linhas = modo == Modo.TREINO ? new Linha[] {Linha.MAPA, Linha.PARCEIRO, Linha.INICIO}
                    : new Linha[] {Linha.MAPA, Linha.TEMPO, Linha.DIFICULDADE};
            Linha l = linhas[Math.min(2, (y - 266) / 32)];
            if (l != Linha.DIFICULDADE) {
                linhaJ1 = l;
                mudarValor(x < 462 ? -1 : 1);
            }
        } else if (x >= 320 && x < 480 && y >= 364 && y < 398 && J1.fase == Fase.PRONTO) {
            linhaJ1 = Linha.LUTAR;
            comandar(j1, Comando.CONFIRMAR);
        }
        repaint();
    }

    /** DIFICULDADE e pulada: indisponivel enquanto nao houver IA. */
    private Linha linhaVizinha(int passo) {
        // 5Q.1: faixa/kimono ficam ACIMA do mapa (o caminho antigo, de LUTAR para cima, nao muda)
        Linha[] ordem = modo == Modo.TREINO
                ? new Linha[] {Linha.FAIXA, Linha.KIMONO, Linha.FAIXA_PARCEIRO, Linha.KIMONO_PARCEIRO, Linha.MAPA,
                    Linha.PARCEIRO, Linha.INICIO, Linha.LUTAR}
                : new Linha[] {Linha.FAIXA, Linha.KIMONO, Linha.MAPA, Linha.TEMPO, Linha.LUTAR};
        int i = java.util.Arrays.asList(ordem).indexOf(linhaJ1);
        return ordem[Math.max(0, Math.min(ordem.length - 1, i + passo))];
    }

    private void mudarValor(int passo) {
        if (linhaJ1 == Linha.MAPA && mapas.size() > 1) {
            mapa = Math.floorMod(mapa + passo, mapas.size());
        } else if (linhaJ1 == Linha.TEMPO) {
            tempo = Math.floorMod(tempo + passo, TEMPOS.length);
        } else if (linhaJ1 == Linha.PARCEIRO && !parceiros.isEmpty()) {
            parceiro = Math.floorMod(parceiro + passo, parceiros.size());
        } else if (linhaJ1 == Linha.INICIO) {
            inicio = Math.floorMod(inicio + passo, InicioLuta.values().length);
        } else if (linhaJ1 == Linha.FAIXA || linhaJ1 == Linha.KIMONO) {
            mudarVisual(SlotJogador.JOGADOR_1, jogadores.get(SlotJogador.JOGADOR_1), linhaJ1 == Linha.FAIXA, passo);
        } else if (linhaJ1 == Linha.FAIXA_PARCEIRO || linhaJ1 == Linha.KIMONO_PARCEIRO) {
            mudarVisual(SlotJogador.JOGADOR_2, jogadores.get(SlotJogador.JOGADOR_2), linhaJ1 == Linha.FAIXA_PARCEIRO,
                    passo);
        }
    }

    /** 5Q.1: proxima faixa ou proximo kimono do slot (circular). */
    private void mudarVisual(SlotJogador slot, Jogador j, boolean faixa, int passo) {
        if (faixa) {
            j.faixa = com.irmaosmiau.regras.Graduacao.values()[Math.floorMod(j.faixa.ordinal() + passo, com.irmaosmiau.regras.Graduacao.values().length)];
            return;
        }
        com.irmaosmiau.entities.CorKimono atual = kimonoEfetivo(slot);
        int i = atual == null ? 0 : atual.ordinal();
        j.kimono = com.irmaosmiau.entities.CorKimono.values()[Math.floorMod(i + passo, com.irmaosmiau.entities.CorKimono.values().length)];
    }

    /** Kimono que o slot vai usar (o escolhido ou o padrao do personagem). */
    com.irmaosmiau.entities.CorKimono kimonoEfetivo(SlotJogador slot) {
        Jogador j = jogadores.get(modo == Modo.TREINO ? slot : slot);
        if (j.kimono != null) {
            return j.kimono;
        }
        EscolhaLutador e = escolhaBase(slot);
        return e == null ? null : padraoKimono(e);
    }

    private final transient Map<String, com.irmaosmiau.entities.CorKimono> kimonosPadrao = new HashMap<>();

    private com.irmaosmiau.entities.CorKimono padraoKimono(EscolhaLutador e) {
        return kimonosPadrao.computeIfAbsent(e.getIdBase(), k -> e.getBase().criarLutador(0, 0, 0.5).getCorKimonoPadrao());
    }

    /** Como no treino antigo: o parceiro e o outro irmao oficial (pode ser trocado depois). */
    private void parceiroPadrao() {
        if (modo != Modo.TREINO || parceiros.isEmpty()) {
            return;
        }
        EscolhaLutador j = escolha(SlotJogador.JOGADOR_1);
        String outro = j != null && CatalogoPersonagens.ID_PRETO.equals(j.getIdBase())
                ? CatalogoPersonagens.ID_BRANCO : CatalogoPersonagens.ID_PRETO;
        for (int i = 0; i < parceiros.size(); i++) {
            if (parceiros.get(i).isOficial() && outro.equals(parceiros.get(i).getIdBase())) {
                parceiro = i;
                return;
            }
        }
        parceiro = 0;
    }

    private void iniciar() {
        if (iniciada) {
            return;
        }
        iniciada = true;
        timer.stop();
        if (modo == Modo.TREINO) {
            aoIniciarTreino.iniciar(escolha(SlotJogador.JOGADOR_1), escolha(SlotJogador.JOGADOR_2), mapas.get(mapa),
                    InicioLuta.values()[inicio]);
            return;
        }
        aoIniciar.iniciar(escolha(SlotJogador.JOGADOR_1), escolha(SlotJogador.JOGADOR_2), mapas.get(mapa),
                TEMPOS[tempo]);
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

    /** Escolha confirmada (ou em escolha) do jogador; null num espaco bloqueado. */
    EscolhaLutador escolha(SlotJogador slot) {
        EscolhaLutador e = escolhaBase(slot);
        Jogador j = jogadores.get(slot);
        return e == null ? null : e.com(j.faixa, j.kimono);              // 5Q.1: faixa e kimono do slot
    }

    /** A escolha sem faixa/kimono (personagem ou perfil). */
    private EscolhaLutador escolhaBase(SlotJogador slot) {
        if (modo == Modo.TREINO && slot == SlotJogador.JOGADOR_2) {
            return parceiros.isEmpty() ? null : parceiros.get(parceiro);       // o parceiro passivo
        }
        Jogador j = jogadores.get(slot);
        if (j.fase != Fase.ESCOLHENDO && !j.perfis.isEmpty()) {
            return j.perfis.get(j.perfil);
        }
        DefinicaoPersonagem d = personagemEm(j.cursor);
        return d == null ? null : EscolhaLutador.oficial(d);
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
        ArteMenus.letreiro(g, modo == Modo.TREINO ? "TREINO: ESCOLHA SEU LUTADOR" : "ESCOLHA SEU LUTADOR", 400, 44,
                new Font("Arial", Font.BOLD, 26), ArteMenus.AMARELO);

        painelJogador(g, SlotJogador.JOGADOR_1, 10);
        painelJogador(g, SlotJogador.JOGADOR_2, 570);
        grade(g);
        configuracoes(g);
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
        if (modo == Modo.PARTIDA) {                             // treino: so o cursor do J1
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
        boolean parceiroTreino = modo == Modo.TREINO && s == SlotJogador.JOGADOR_2;
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString(parceiroTreino ? "PARCEIRO" : s == SlotJogador.JOGADOR_1 ? "J1" : "J2", x + 10, 80);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(j.fase == Fase.PRONTO || parceiroTreino ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        String fase = parceiroTreino ? "PASSIVO"
                : j.fase == Fase.PRONTO ? "PRONTO!" : j.fase == Fase.PERFIL ? "ESCOLHA O PERFIL" : "ESCOLHENDO";
        g.drawString(fase, x + 210 - g.getFontMetrics().stringWidth(fase), 78);

        visual(g, s, j, x, parceiroTreino);

        // previa
        g.setColor(new Color(255, 255, 255, 30));
        g.fillOval(x + 40, 268, 140, 18);
        EscolhaLutador e = escolha(s);
        DefinicaoPersonagem d = parceiroTreino ? (e == null ? null : e.getBase()) : personagemEm(j.cursor);
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
        if (j.avisoBloqueadoAte > tempoMs) {
            g.setColor(new Color(200, 40, 40, 220));
            g.fillRoundRect(x + 30, 190, 160, 24, 10, 10);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            ArteMenus.centralizado(g, "BLOQUEADO", x + 110, 207);
        }

        // nome, estilo, perfil e atributos (dados reais do catalogo/perfil)
        g.setColor(ArteMenus.BRANCO);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        ArteMenus.centralizado(g, d == null ? "???" : nomeExibido(d), x + 110, 304);
        g.setFont(ArteMenus.PEQUENO);
        ArteMenus.centralizado(g, d == null ? "Em breve" : "Estilo: " + d.getEstilo(), x + 110, 319);
        if (parceiroTreino && e != null && !e.isOficial()) {
            g.setColor(ArteMenus.BRANCO);
            ArteMenus.centralizado(g, "Perfil: " + e.getNome(), x + 110, 333);
        } else if (!parceiroTreino && e != null && j.perfis.size() > 1 && j.fase != Fase.ESCOLHENDO) {
            g.setColor(j.fase == Fase.PERFIL ? ArteMenus.AMARELO : ArteMenus.BRANCO);
            ArteMenus.centralizado(g, (j.fase == Fase.PERFIL ? "< " : "") + "Perfil: " + e.getNome()
                    + (j.fase == Fase.PERFIL ? " >" : ""), x + 110, 333);
        }
        if (e != null) {
            atributos(g, e.getAtributos(), x + 12, 344);
        }
    }

    /** 5Q.1: "FAIXA < AZUL >" e "KIMONO < PRETO >" do slot, com a linha em foco destacada. */
    private void visual(Graphics2D g, SlotJogador s, Jogador j, int x, boolean parceiroTreino) {
        boolean pronto = jogadores.get(SlotJogador.JOGADOR_1).fase == Fase.PRONTO;
        boolean focoFaixa;
        boolean focoKimono;
        if (s == SlotJogador.JOGADOR_1) {
            focoFaixa = pronto && linhaJ1 == Linha.FAIXA;
            focoKimono = pronto && linhaJ1 == Linha.KIMONO;
        } else if (parceiroTreino) {
            focoFaixa = pronto && linhaJ1 == Linha.FAIXA_PARCEIRO;
            focoKimono = pronto && linhaJ1 == Linha.KIMONO_PARCEIRO;
        } else {
            focoFaixa = j.fase == Fase.PRONTO && j.linhaVisual == 0;
            focoKimono = j.fase == Fase.PRONTO && j.linhaVisual == 1;
        }
        com.irmaosmiau.entities.CorKimono k = kimonoEfetivo(s);
        linhaVisual(g, "FAIXA", j.faixa.name(), x, 94, focoFaixa);
        linhaVisual(g, "KIMONO", k == null ? "-" : k.getNome().toUpperCase(Locale.ROOT), x, 108, focoKimono);
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

    private void configuracoes(Graphics2D g) {
        boolean j1Pronto = jogadores.get(SlotJogador.JOGADOR_1).fase == Fase.PRONTO;
        g.setColor(ArteMenus.AZUL_PAINEL);
        g.fillRoundRect(240, 258, 320, 184, 14, 14);
        boolean treino = modo == Modo.TREINO;
        linha(g, Linha.MAPA, "MAPA", nomesMapas.get(mapa), 284, mapas.size() > 1, j1Pronto);
        if (treino) {
            linha(g, Linha.PARCEIRO, "PARCEIRO", textoParceiro(), 316, parceiros.size() > 1, j1Pronto);
            linha(g, Linha.INICIO, "INÍCIO", InicioLuta.values()[inicio].getRotulo().toUpperCase(Locale.ROOT), 348,
                    true, j1Pronto);
        } else {
            linha(g, Linha.TEMPO, "TEMPO", textoTempo(TEMPOS[tempo]), 316, true, j1Pronto);
            linha(g, Linha.DIFICULDADE, "DIFICULDADE", "EM BREVE", 348, false, j1Pronto);
        }

        boolean focoLutar = j1Pronto && linhaJ1 == Linha.LUTAR;
        boolean prontos = j1Pronto && (treino || jogadores.get(SlotJogador.JOGADOR_2).fase == Fase.PRONTO);
        g.setColor(focoLutar ? new Color(255, 206, 46, 235) : new Color(30, 70, 150));
        g.fillRoundRect(320, 364, 160, 34, 12, 12);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(focoLutar ? ArteMenus.AZUL_ESCURO : new Color(170, 190, 230));
        ArteMenus.centralizado(g, treino ? "TREINAR!" : "LUTAR!", 400, 388);

        g.setFont(ArteMenus.PEQUENO);
        g.setColor(ArteMenus.BRANCO);
        String dica;
        if (avisoAguardandoAte > tempoMs) {
            g.setColor(ArteMenus.AMARELO);
            dica = "AGUARDANDO O J2 FICAR PRONTO";
        } else if (treino && j1Pronto) {
            dica = "cima/baixo escolhe · lados mudam · confirme TREINAR!";
        } else if (treino) {
            dica = "Escolha seu lutador · o parceiro fica parado";
        } else if (prontos) {
            dica = "J1: confirme LUTAR! para começar";
        } else if (j1Pronto) {
            dica = "J1 ajusta a partida · aguardando o J2";
        } else {
            dica = "Confirme seu lutador · cancelar volta";
        }
        ArteMenus.centralizado(g, dica, 400, 416);
        g.setColor(new Color(190, 205, 235));
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        ArteMenus.centralizado(g, treino ? "WASD/setas · ESPAÇO/ENTER confirma · Q volta · ESC menu"
                : "J1 WASD·ESPAÇO·Q   J2 setas·ENTER·BACKSPACE   ESC menu", 400, 433);
    }

    /** Triangulo de seta (as fontes padrao nem sempre tem os caracteres de seta). */
    private static void seta(Graphics2D g, int cx, int cy, int lado) {
        g.fillPolygon(new int[] {cx + 5 * lado, cx - 5 * lado, cx - 5 * lado}, new int[] {cy, cy - 6, cy + 6}, 3);
    }

    /** Nome curto do parceiro: "MIAU PRETO" ou o nome do perfil. */
    private String textoParceiro() {
        EscolhaLutador e = parceiros.isEmpty() ? null : parceiros.get(parceiro);
        if (e == null) {
            return "-";
        }
        String t = e.isOficial() ? nomeExibido(e.getBase()) : e.getNome().toUpperCase(Locale.ROOT);
        return t.length() > 16 ? t.substring(0, 15) + "…" : t;
    }

    static String textoTempo(int s) {
        return s == 0 ? "SEM LIMITE" : s + " s";
    }

    private void linha(Graphics2D g, Linha l, String rotulo, String valor, int y, boolean setas, boolean j1Pronto) {
        boolean foco = j1Pronto && linhaJ1 == l;
        if (foco) {
            g.setColor(new Color(255, 206, 46, 60));
            g.fillRoundRect(250, y - 18, 300, 26, 8, 8);
        }
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(foco ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        g.drawString(rotulo, 262, y);
        boolean habilitado = l != Linha.DIFICULDADE;
        g.setColor(!habilitado ? new Color(140, 150, 175) : foco ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        if (g.getFontMetrics().stringWidth(valor) > 110) {
            g.setFont(new Font("Arial", Font.BOLD, 11));         // 5M.3: nomes longos cabem entre as setas
        }
        ArteMenus.centralizado(g, valor, 462, y);
        g.setColor(setas && habilitado ? (foco ? ArteMenus.AMARELO : ArteMenus.BRANCO) : new Color(90, 105, 140));
        seta(g, 392, y - 5, -1);
        seta(g, 532, y - 5, 1);
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
        return mapas.get(mapa);
    }

    int tempoAtual() {
        return TEMPOS[tempo];
    }

    Modo modo() {
        return modo;
    }

    InicioLuta inicioAtual() {
        return InicioLuta.values()[inicio];
    }

    boolean mapaTemOpcoes() {
        return mapas.size() > 1;
    }

    Linha linhaJ1() {
        return linhaJ1;
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
