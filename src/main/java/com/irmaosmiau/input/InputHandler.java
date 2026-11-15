package com.irmaosmiau.input;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;
import com.irmaosmiau.dispositivos.FonteAcoes;

import java.awt.event.ActionEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Ponto UNICO de leitura do teclado dos dois jogadores.
 *
 * Entrega 5C: as teclas deixaram de ser constantes deste arquivo. O fluxo e
 * tecla fisica (MapaControles) -> acao logica (AcaoJogador) -> estado
 * pressionado/solto por slot. Quem decide o que a acao significa no estado
 * atual e o GamePanel (e, no combate, AcoesContextuais), nunca a tecla.
 *
 * Mantido desde a Entrega 1:
 * - nomes de comando no ActionMap ("brancoPegada1Pressionada" etc.);
 * - TravaComandos (3A): tecla segurada durante bloqueio so volta a valer
 *   depois de solta;
 * - getters especificos (isPegada1, isPretoRompimento...), agora apenas
 *   atalhos para {@link #isAtiva(SlotJogador, AcaoJogador)}.
 *
 * Entrega 5D: cada slot pode ter tambem uma FONTE externa (gamepad). A acao
 * vale se vier do teclado do slot OU da fonte; quando um gamepad assume o
 * slot, o teclado daquele slot e desligado para dois dispositivos nao
 * agirem no mesmo jogador. Teclado e gamepad passam pela mesma trava.
 */
public class InputHandler {

    /*
     * Com SHIFT/CTRL/ALT segurados o Swing gera "shift pressed X" etc. em vez
     * de "pressed X". Registrar todas as combinacoes mantem todos os
     * controles funcionando enquanto um modificador (qualquer que seja a
     * tecla escolhida para o modo de controle) esta segurado, e as teclas
     * sao soltas corretamente em qualquer ordem.
     */
    static final String[] COMBINACOES_MODIFICADORES = {
        "", "shift ", "ctrl ", "alt ", "shift ctrl ", "shift alt ", "ctrl alt ", "shift ctrl alt "
    };

    private static final String PREFIXO_BRANCO = SlotJogador.JOGADOR_1.getPrefixoComando();
    private static final String PREFIXO_PRETO = SlotJogador.JOGADOR_2.getPrefixoComando();

    private final MapaControles mapa;
    private final Map<SlotJogador, Set<AcaoJogador>> pressionadas = new EnumMap<>(SlotJogador.class);
    private final TravaComandos trava = new TravaComandos();
    private final Map<SlotJogador, FonteAcoes> fontes = new EnumMap<>(SlotJogador.class);
    private final Set<SlotJogador> tecladoDesligado = EnumSet.noneOf(SlotJogador.class);
    /** 5O.1: slots cujo teclado e um CONTROLE (layout fixo); as teclas soltas deles nao sao ligadas. */
    private final Map<SlotJogador, com.irmaosmiau.dispositivos.TecladoControle> tecladosControle =
            new EnumMap<>(SlotJogador.class);

    // =====================================================
    // CONFIGURACAO DOS KEY BINDINGS
    // =====================================================
    /** Controles padrao (identicos aos de antes da Entrega 5C). */
    public InputHandler(JComponent componente) {
        this(componente, MapaControles.padrao());
    }

    /**
     * Controles configurados. Um mapa com conflitos nunca deveria chegar
     * aqui (PreferenciasControles valida ao carregar e ao salvar); se chegar,
     * os padroes sao usados para o jogo continuar controlavel.
     */
    public InputHandler(JComponent componente, MapaControles mapaConfigurado) {
        MapaControles m = mapaConfigurado == null ? MapaControles.padrao() : mapaConfigurado;
        if (m.problemas().stream().anyMatch(p -> p.tipo() != com.irmaosmiau.controles.ProblemaControle.Tipo.SEM_TECLA)) {
            System.err.println("[controles] mapa com conflitos " + m.problemas() + "; usando os padroes.");
            m = MapaControles.padrao();
        }
        this.mapa = m;

        InputMap inputMap = componente.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = componente.getActionMap();

        for (SlotJogador slot : SlotJogador.values()) {
            pressionadas.put(slot, EnumSet.noneOf(AcaoJogador.class));
            boolean controle = com.irmaosmiau.controles.LayoutTecladoControle.usa(slot);
            for (AcaoJogador acao : slot.acoes()) {
                // 5O.1: no teclado-controle as teclas soltas do slot nao sao ligadas (o comando continua no ActionMap).
                mapearAcao(inputMap, actionMap, slot, acao, controle ? Optional.empty() : mapa.tecla(slot, acao));
            }
            if (controle) {
                mapearTecladoControle(inputMap, actionMap, slot);
            }
        }
    }

    /**
     * 5O.1: cada tecla do layout vira um botao/analogico do controle virtual do
     * slot (comandos "<prefixo>Controle<TECLA>Pressionada/Solta").
     */
    private void mapearTecladoControle(InputMap inputMap, ActionMap actionMap, SlotJogador slot) {
        com.irmaosmiau.dispositivos.TecladoControle tc = new com.irmaosmiau.dispositivos.TecladoControle(slot);
        tecladosControle.put(slot, tc);
        for (String tecla : tc.teclas()) {
            String nome = comandoTecladoControle(slot, tecla);
            for (String mod : COMBINACOES_MODIFICADORES) {
                inputMap.put(KeyStroke.getKeyStroke(mod + "pressed " + tecla), nome + "Pressionada");
                inputMap.put(KeyStroke.getKeyStroke(mod + "released " + tecla), nome + "Solta");
            }
            actionMap.put(nome + "Pressionada", new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    tc.pressionar(tecla);
                }
            });
            actionMap.put(nome + "Solta", new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    tc.soltar(tecla);
                }
            });
        }
    }

    /** Nome do comando de uma tecla do teclado-controle (ex.: "pretoControleW"). */
    public static String comandoTecladoControle(SlotJogador slot, String tecla) {
        return slot.getPrefixoComando() + "Controle" + tecla;
    }

    /** 5O.1: teclado-controle do slot (vazio = teclas soltas). */
    public Optional<com.irmaosmiau.dispositivos.TecladoControle> tecladoControle(SlotJogador slot) {
        return Optional.ofNullable(tecladosControle.get(slot));
    }

    private void mapearAcao(InputMap inputMap, ActionMap actionMap, SlotJogador slot,
            AcaoJogador acao, Optional<TeclaFisica> tecla) {

        String nome = slot.comando(acao);
        String pressionada = nome + "Pressionada";
        String solta = nome + "Solta";

        // Acao sem tecla continua existindo no ActionMap, so nao tem atalho.
        tecla.ifPresent(t -> {
            for (String mod : COMBINACOES_MODIFICADORES) {
                inputMap.put(KeyStroke.getKeyStroke(mod + "pressed " + t.getNome()), pressionada);
                inputMap.put(KeyStroke.getKeyStroke(mod + "released " + t.getNome()), solta);
            }
        });

        actionMap.put(pressionada, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                trava.pressionar(nome);
                pressionadas.get(slot).add(acao);
            }
        });

        actionMap.put(solta, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                trava.soltar(nome);
                pressionadas.get(slot).remove(acao);
            }
        });
    }

    /** Mapa efetivamente em uso (para a ajuda e a faixa de dicas). */
    public MapaControles getMapa() {
        return mapa;
    }

    // =====================================================
    // LEITURA GENERICA (Entrega 5C)
    // =====================================================
    /** A acao esta segurada agora e nao esta travada por um bloqueio de combate. */
    public boolean isAtiva(SlotJogador slot, AcaoJogador acao) {
        return isAtivaNoTeclado(slot, acao) || isAtivaNaFonte(slot, acao);
    }

    /** Somente o teclado (respeita o teclado desligado do slot). */
    public boolean isAtivaNoTeclado(SlotJogador slot, AcaoJogador acao) {
        return !tecladoDesligado.contains(slot)
                && pressionadas.get(slot).contains(acao) && trava.isLivre(slot.comando(acao));
    }

    private boolean isAtivaNaFonte(SlotJogador slot, AcaoJogador acao) {
        FonteAcoes f = fontes.get(slot);
        return f != null && f.isAtiva(acao);
    }

    // =====================================================
    // FONTES EXTERNAS (Entrega 5D)
    // =====================================================
    /** Liga (ou com null desliga) a fonte externa do slot. */
    public void conectarFonte(SlotJogador slot, FonteAcoes fonte) {
        if (fonte == null) {
            fontes.remove(slot);
        } else {
            fontes.put(slot, fonte);
        }
    }

    /**
     * Liga/desliga as teclas do slot. Desligado, o teclado continua sendo
     * acompanhado (soltar uma tecla e registrado), so nao age; ao religar,
     * o que estiver segurado fica travado ate ser solto.
     */
    public void setTecladoAtivo(SlotJogador slot, boolean ativo) {
        if (ativo && tecladoDesligado.remove(slot)) {
            trava.travarPressionados(slot.getPrefixoComando());
        } else if (!ativo) {
            tecladoDesligado.add(slot);
        }
    }

    public boolean isTecladoAtivo(SlotJogador slot) {
        return !tecladoDesligado.contains(slot);
    }

    /**
     * Solta tudo (janela perdeu o foco): sem o evento "released", uma tecla
     * ficaria presa e o lutador andaria sozinho.
     */
    public void liberarTodas() {
        for (Set<AcaoJogador> s : pressionadas.values()) {
            s.clear();
        }
        trava.soltarTodos();
        tecladosControle.values().forEach(com.irmaosmiau.dispositivos.TecladoControle::liberar);
        for (FonteAcoes f : fontes.values()) {
            f.liberarTodas();
        }
    }

    // =====================================================
    // TRAVA DE COMANDOS DURANTE BLOQUEIO (Entrega 3A)
    // =====================================================
    /**
     * Chamado a cada frame em que o Branco esta bloqueado por uma animacao
     * de combate: toda tecla dele que estiver segurada passa a ser lida como
     * solta ate ser liberada e pressionada de novo.
     */
    public void travarComandosPressionadosBranco() {
        trava.travarPressionados(PREFIXO_BRANCO);
        travarFonte(SlotJogador.JOGADOR_1);
    }

    private void travarFonte(SlotJogador slot) {
        FonteAcoes f = fontes.get(slot);
        if (f != null) {
            f.travarPressionadas();
        }
    }

    /** Igual a {@link #travarComandosPressionadosBranco()}, para o Preto. */
    public void travarComandosPressionadosPreto() {
        trava.travarPressionados(PREFIXO_PRETO);
        travarFonte(SlotJogador.JOGADOR_2);
    }

    // =====================================================
    // GETTERS - JOGADOR 1 (Miau Branco / jogador individual)
    // =====================================================
    private boolean j1(AcaoJogador a) {
        return isAtiva(SlotJogador.JOGADOR_1, a);
    }

    private boolean j2(AcaoJogador a) {
        return isAtiva(SlotJogador.JOGADOR_2, a);
    }

    public boolean isEsquerda() {
        return j1(AcaoJogador.MOVER_ESQUERDA);
    }

    public boolean isDireita() {
        return j1(AcaoJogador.MOVER_DIREITA);
    }

    public boolean isCima() {
        return j1(AcaoJogador.MOVER_CIMA);
    }

    public boolean isBaixo() {
        return j1(AcaoJogador.MOVER_BAIXO);
    }

    public boolean isPular() {
        return j1(AcaoJogador.PULAR);
    }

    public boolean isAgachar() {
        return j1(AcaoJogador.AGACHAR);
    }

    public boolean isCorrer() {
        return j1(AcaoJogador.CORRER);
    }

    public boolean isPularCurto() {
        return j1(AcaoJogador.PULO_CURTO);
    }

    public boolean isRespirar() {
        return j1(AcaoJogador.RESPIRAR);
    }

    public boolean isRolar() {
        return j1(AcaoJogador.ROLAR);
    }

    public boolean isDeitar() {
        return j1(AcaoJogador.DEITAR_LEVANTAR);
    }

    public boolean isSprawl() {
        return j1(AcaoJogador.SPRAWL);
    }

    public boolean isPegada1() {
        return j1(AcaoJogador.INICIAR_PEGADA);
    }

    /** Reforcar a pegada (slot 1, se for o aplicador). */
    public boolean isReforco() {
        return j1(AcaoJogador.REFORCAR_PEGADA);
    }

    /** Modo de controle do slot 1 (com esquerda/direita = deslocamento conjunto). */
    public boolean isControle() {
        return j1(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO);
    }

    /** Acao ofensiva do slot 1 (desequilibrio, se for o aplicador). */
    public boolean isDesequilibrio() {
        return j1(AcaoJogador.TENTAR_DESEQUILIBRIO);
    }

    // =====================================================
    // GETTERS - JOGADOR 2 (Miau Preto)
    // =====================================================
    public boolean isPretoEsquerda() {
        return j2(AcaoJogador.MOVER_ESQUERDA);
    }

    public boolean isPretoDireita() {
        return j2(AcaoJogador.MOVER_DIREITA);
    }

    public boolean isPretoCima() {
        return j2(AcaoJogador.MOVER_CIMA);
    }

    public boolean isPretoBaixo() {
        return j2(AcaoJogador.MOVER_BAIXO);
    }

    public boolean isPretoPular() {
        return j2(AcaoJogador.PULAR);
    }

    public boolean isPretoAgachar() {
        return j2(AcaoJogador.AGACHAR);
    }

    public boolean isPretoCorrer() {
        return j2(AcaoJogador.CORRER);
    }

    public boolean isPretoPularCurto() {
        return j2(AcaoJogador.PULO_CURTO);
    }

    public boolean isPretoRespirar() {
        return j2(AcaoJogador.RESPIRAR);
    }

    public boolean isPretoRolar() {
        return j2(AcaoJogador.ROLAR);
    }

    public boolean isPretoDeitar() {
        return j2(AcaoJogador.DEITAR_LEVANTAR);
    }

    public boolean isPretoSprawl() {
        return j2(AcaoJogador.SPRAWL);
    }

    /** Acao defensiva do slot 2 (rompimento tecnico, se for o receptor). */
    public boolean isPretoRompimento() {
        return j2(AcaoJogador.ROMPIMENTO_TECNICO);
    }
}
