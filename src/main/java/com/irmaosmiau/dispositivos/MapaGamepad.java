package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.EntradaContextual;
import com.irmaosmiau.controles.SlotJogador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Botoes do gamepad -> acoes logicas e comandos, POR SLOT (Entregas 5D e 5F).
 *
 * Cada slot tem um esquema (CLASSICO ou CONTEXTUAL) e suas proprias
 * atribuicoes, editaveis em CONFIGURACOES -> CONTROLES e salvas em
 * gamepad.json. Imutavel: cada alteracao devolve um novo mapa.
 *
 * Regras de cada slot:
 * - um botao aciona uma coisa so, com UMA excecao: no CONTEXTUAL o botao do
 *   modo controle (arrasto, segurado com o analogico) pode ser tambem o
 *   modificador da camada 3 (padrao LB): com o modificador da camada 2 (LT)
 *   segurado ele vale como camada 3 e o arrasto fica suspenso;
 * - Start/Options (pausa) e Guide sao reservados;
 * - nenhum botao e obrigatorio (o RB, por exemplo, pode ficar sem uso);
 * - o analogico esquerdo sempre move (zona morta e histerese em LeitorAnalogico).
 */
public final class MapaGamepad {

    public static final BotaoGamepad BOTAO_PAUSA = BotaoGamepad.INICIAR;
    public static final Set<BotaoGamepad> RESERVADOS = Set.of(BotaoGamepad.INICIAR, BotaoGamepad.GUIA);

    /** Configuracao de um slot. */
    public record ConfigSlot(EsquemaGamepad esquema, Map<AcaoJogador, BotaoGamepad> acoes,
            Map<ComandoGamepad, BotaoGamepad> comandos) {

        public ConfigSlot {
            acoes = Collections.unmodifiableMap(acoes.isEmpty()
                    ? new EnumMap<>(AcaoJogador.class) : new EnumMap<>(acoes));
            comandos = Collections.unmodifiableMap(comandos.isEmpty()
                    ? new EnumMap<>(ComandoGamepad.class) : new EnumMap<>(comandos));
        }
    }

    /** Problema de atribuicao. */
    public record Problema(SlotJogador slot, ItemGamepad item, BotaoGamepad botao, ItemGamepad outro) {

        public String descricao() {
            return outro == null
                    ? slot.getSigla() + " · " + item.rotulo() + ": " + botao.getRotulo() + " é reservado"
                    : slot.getSigla() + " · " + botao.getRotulo() + " usado em " + item.rotulo() + " e "
                            + outro.rotulo();
        }
    }

    private final Map<SlotJogador, ConfigSlot> slots;

    private MapaGamepad(Map<SlotJogador, ConfigSlot> slots) {
        this.slots = Collections.unmodifiableMap(new EnumMap<>(slots));
    }

    // =====================================================
    // PADROES
    // =====================================================
    /** O layout HISTORICO da 5D (os dois slots no Classico antigo: A = pulo alto, L3 rola). */
    public static MapaGamepad experimental() {
        Map<SlotJogador, ConfigSlot> m = new EnumMap<>(SlotJogador.class);
        for (SlotJogador s : SlotJogador.values()) {
            m.put(s, padraoAnterior(s, EsquemaGamepad.CLASSICO));
        }
        return new MapaGamepad(m);
    }

    /** Padrao atual (5G): os dois slots no Classico. */
    public static MapaGamepad padrao() {
        Map<SlotJogador, ConfigSlot> m = new EnumMap<>(SlotJogador.class);
        for (SlotJogador s : SlotJogador.values()) {
            m.put(s, padraoDo(s, EsquemaGamepad.CLASSICO));
        }
        return new MapaGamepad(m);
    }

    /**
     * Padroes da 5G: R3 rola e L3 deita/levanta (o polegar esquerdo fica no
     * analogico); A = pulo normal; RB + A = pulo alto (modificador
     * configuravel). No Classico o RB continua correndo quando segurado.
     */
    public static ConfigSlot padraoDo(SlotJogador slot, EsquemaGamepad esquema) {
        // 5I: os dois slots usam todas as acoes de combate (papel vem da luta).
        return transformar51(padrao5O(slot, esquema));
    }

    /** Padroes da 5O (antes da 5O.1), para reconhecer um gamepad.json nunca personalizado. */
    public static ConfigSlot padrao5O(SlotJogador slot, EsquemaGamepad esquema) {
        return transformar5O(transformar5H(transformar5G(layoutBase(slot, esquema, slot::usa), slot::usa)),
                slot::usa);
    }

    /**
     * 5O.1: Select/View deixa de respirar (respirar = RT sozinho) e fica LIVRE:
     * livre, ele vale como RB logico (ver {@link #ALTERNATIVA_RB}). O Y deixa de
     * fazer acrobacia (acrobacia = R3 + direcao em pe); no Classico o Y continua
     * reforcando a pegada.
     */
    private static ConfigSlot transformar51(ConfigSlot c5o) {
        Map<AcaoJogador, BotaoGamepad> a = new EnumMap<>(AcaoJogador.class);
        a.putAll(c5o.acoes());
        if (a.get(AcaoJogador.RESPIRAR) == BotaoGamepad.VOLTAR) {
            a.remove(AcaoJogador.RESPIRAR);
        }
        a.remove(AcaoJogador.ACROBACIA);
        return new ConfigSlot(c5o.esquema(), a, c5o.comandos());
    }

    /**
     * 5O.1: Select/View e uma alternativa ao RB (pulo alto, quedas, corrida no
     * Classico) enquanto estiver livre no slot. O RB continua valendo; Start
     * continua sendo a pausa.
     */
    public static final BotaoGamepad ALTERNATIVA_RB = BotaoGamepad.VOLTAR;

    /** O Select esta livre no slot (entao vale como RB)? */
    public boolean selectValeComoRb(SlotJogador slot) {
        return quemUsa(slot, ALTERNATIVA_RB).isEmpty();
    }

    /** Padroes da 5H a 5N (antes das familias da 5O), para reconhecer um gamepad.json nunca personalizado. */
    public static ConfigSlot padrao5N(SlotJogador slot, EsquemaGamepad esquema) {
        return transformar5H(transformar5G(layoutBase(slot, esquema, slot::usa), slot::usa));
    }

    /**
     * 5O: familias de tecnicas no gamepad, nos dois esquemas.
     * - RT = modificador de DEFESAS (RT + RS: guardas alta/media/baixa e sprawl).
     *   O sprawl, que antes saia no RT sozinho, passa a ser RT + RS para baixo
     *   (no teclado continua com a tecla propria).
     * - RB = modificador de QUEDAS E TRANSICOES (RB + RS; reservado). Continua
     *   sendo o modificador do pulo alto (RB + A) e, no Classico, a corrida.
     * - Y = acrobacia (Y + analogico esquerdo). No Classico o Y tambem e o
     *   reforco: reforcar so vale para quem segura uma pegada e acrobacia so
     *   para quem esta livre, entao os dois nunca valem juntos.
     */
    private static ConfigSlot transformar5O(ConfigSlot c5n, java.util.function.Predicate<AcaoJogador> usa) {
        Map<AcaoJogador, BotaoGamepad> a = new EnumMap<>(AcaoJogador.class);
        a.putAll(c5n.acoes());
        if (a.get(AcaoJogador.SPRAWL) == BotaoGamepad.GATILHO_DIR) {
            a.remove(AcaoJogador.SPRAWL);
        }
        a.put(AcaoJogador.MODIFICADOR_DEFESA, BotaoGamepad.GATILHO_DIR);
        a.put(AcaoJogador.MODIFICADOR_PROJECAO, BotaoGamepad.RB);
        a.put(AcaoJogador.ACROBACIA, BotaoGamepad.NORTE);
        a.keySet().removeIf(acao -> !usa.test(acao));
        return new ConfigSlot(c5n.esquema(), a, c5n.comandos());
    }

    /** Padroes da 5H (uso antigo dos slots), para reconhecer um gamepad.json nunca personalizado. */
    public static ConfigSlot padrao5H(SlotJogador slot, EsquemaGamepad esquema) {
        return transformar5H(padrao5G(slot, esquema));
    }

    /** 5H: no Contextual o arrasto e automatico; o LB fica so como modificador da camada 3. */
    private static ConfigSlot transformar5H(ConfigSlot c5g) {
        if (c5g.esquema() != EsquemaGamepad.CONTEXTUAL) {
            return c5g;
        }
        Map<AcaoJogador, BotaoGamepad> a = new EnumMap<>(AcaoJogador.class);
        a.putAll(c5g.acoes());
        a.remove(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO);
        return new ConfigSlot(c5g.esquema(), a, c5g.comandos());
    }

    /** Padroes da 5G (uso antigo dos slots), para reconhecer um gamepad.json nunca personalizado. */
    public static ConfigSlot padrao5G(SlotJogador slot, EsquemaGamepad esquema) {
        return transformar5G(padraoAnterior(slot, esquema), a -> a.usadaAntesDa5I(slot));
    }

    private static ConfigSlot transformar5G(ConfigSlot antigo, java.util.function.Predicate<AcaoJogador> usa) {
        EsquemaGamepad esquema = antigo.esquema();
        Map<AcaoJogador, BotaoGamepad> a = new EnumMap<>(AcaoJogador.class);
        a.putAll(antigo.acoes());
        Map<ComandoGamepad, BotaoGamepad> c = new EnumMap<>(ComandoGamepad.class);
        c.putAll(antigo.comandos());
        a.remove(AcaoJogador.PULAR);                       // pulo alto = modificador + botao do pulo
        a.put(AcaoJogador.PULO_CURTO, BotaoGamepad.SUL);
        a.put(AcaoJogador.ROLAR, BotaoGamepad.R3);
        a.put(AcaoJogador.DEITAR_LEVANTAR, BotaoGamepad.L3);
        c.put(ComandoGamepad.MODIFICADOR_PULO_ALTO, BotaoGamepad.RB);
        a.keySet().removeIf(acao -> !usa.test(acao));
        return new ConfigSlot(esquema, a, c);
    }

    /** Padroes da 5D/5F com o uso antigo dos slots (reconhecer arquivos e o layout historico). */
    public static ConfigSlot padraoAnterior(SlotJogador slot, EsquemaGamepad esquema) {
        return layoutBase(slot, esquema, a -> a.usadaAntesDa5I(slot));
    }

    private static ConfigSlot layoutBase(SlotJogador slot, EsquemaGamepad esquema,
            java.util.function.Predicate<AcaoJogador> usa) {
        Map<AcaoJogador, BotaoGamepad> a = new EnumMap<>(AcaoJogador.class);
        Map<ComandoGamepad, BotaoGamepad> c = new EnumMap<>(ComandoGamepad.class);
        if (esquema == EsquemaGamepad.CLASSICO) {
            // Esquema provisorio da 5D, sem nenhuma mudanca.
            a.put(AcaoJogador.MOVER_ESQUERDA, BotaoGamepad.DPAD_ESQUERDA);
            a.put(AcaoJogador.MOVER_DIREITA, BotaoGamepad.DPAD_DIREITA);
            a.put(AcaoJogador.MOVER_CIMA, BotaoGamepad.DPAD_CIMA);
            a.put(AcaoJogador.MOVER_BAIXO, BotaoGamepad.DPAD_BAIXO);
            a.put(AcaoJogador.PULAR, BotaoGamepad.SUL);
            a.put(AcaoJogador.AGACHAR, BotaoGamepad.GATILHO_ESQ);
            a.put(AcaoJogador.CORRER, BotaoGamepad.RB);
            a.put(AcaoJogador.ROLAR, BotaoGamepad.L3);
            a.put(AcaoJogador.DEITAR_LEVANTAR, BotaoGamepad.R3);
            a.put(AcaoJogador.RESPIRAR, BotaoGamepad.VOLTAR);
            a.put(AcaoJogador.SPRAWL, BotaoGamepad.GATILHO_DIR);
            a.put(AcaoJogador.INICIAR_PEGADA, BotaoGamepad.OESTE);
            a.put(AcaoJogador.TENTAR_DESEQUILIBRIO, BotaoGamepad.LESTE);
            a.put(AcaoJogador.ROMPIMENTO_TECNICO, BotaoGamepad.LESTE);
            a.put(AcaoJogador.REFORCAR_PEGADA, BotaoGamepad.NORTE);
            a.put(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, BotaoGamepad.LB);
        } else {
            // Contextual: D-pad = tecnicas; LT/LB = camadas; agachar no B; RB livre.
            a.put(AcaoJogador.PULAR, BotaoGamepad.SUL);
            a.put(AcaoJogador.AGACHAR, BotaoGamepad.LESTE);
            a.put(AcaoJogador.CORRER, BotaoGamepad.OESTE);
            a.put(AcaoJogador.ROLAR, BotaoGamepad.NORTE);
            a.put(AcaoJogador.DEITAR_LEVANTAR, BotaoGamepad.L3);
            a.put(AcaoJogador.PULO_CURTO, BotaoGamepad.R3);
            a.put(AcaoJogador.RESPIRAR, BotaoGamepad.VOLTAR);
            a.put(AcaoJogador.SPRAWL, BotaoGamepad.GATILHO_DIR);
            a.put(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, BotaoGamepad.LB);
            c.put(ComandoGamepad.TECNICA_CIMA, BotaoGamepad.DPAD_CIMA);
            c.put(ComandoGamepad.TECNICA_DIREITA, BotaoGamepad.DPAD_DIREITA);
            c.put(ComandoGamepad.TECNICA_BAIXO, BotaoGamepad.DPAD_BAIXO);
            c.put(ComandoGamepad.TECNICA_ESQUERDA, BotaoGamepad.DPAD_ESQUERDA);
            c.put(ComandoGamepad.MODIFICADOR_CAMADA_2, BotaoGamepad.GATILHO_ESQ);
            c.put(ComandoGamepad.MODIFICADOR_CAMADA_3, BotaoGamepad.LB);
        }
        a.keySet().removeIf(acao -> !usa.test(acao));
        return new ConfigSlot(esquema, a, c);
    }

    // =====================================================
    // CONSULTAS
    // =====================================================
    public ConfigSlot config(SlotJogador slot) {
        return slots.get(slot);
    }

    public EsquemaGamepad esquema(SlotJogador slot) {
        return slots.get(slot).esquema();
    }

    public BotaoGamepad getBotaoPausa() {
        return BOTAO_PAUSA;
    }

    public boolean isAnalogicoEsquerdoMove() {
        return true;
    }

    public Optional<BotaoGamepad> botao(SlotJogador slot, ItemGamepad item) {
        ConfigSlot c = slots.get(slot);
        return Optional.ofNullable(item.acao() != null ? c.acoes().get(item.acao()) : c.comandos().get(item.comando()));
    }

    /** Itens editaveis do slot no esquema atual. */
    public List<ItemGamepad> itens(SlotJogador slot) {
        List<ItemGamepad> l = new ArrayList<>();
        for (AcaoJogador a : slot.acoes()) {
            boolean movimentoNoContextual = esquema(slot) == EsquemaGamepad.CONTEXTUAL && isMovimento(a);
            if (!movimentoNoContextual) {
                l.add(ItemGamepad.de(a));
            }
        }
        for (ComandoGamepad c : ComandoGamepad.values()) {
            if (!c.isSoContextual() || esquema(slot) == EsquemaGamepad.CONTEXTUAL) {
                l.add(ItemGamepad.de(c));
            }
        }
        return l;
    }

    private static boolean isMovimento(AcaoJogador a) {
        return a == AcaoJogador.MOVER_ESQUERDA || a == AcaoJogador.MOVER_DIREITA
                || a == AcaoJogador.MOVER_CIMA || a == AcaoJogador.MOVER_BAIXO;
    }

    /** Quem usa o botao no slot (primeiro encontrado). */
    public Optional<ItemGamepad> quemUsa(SlotJogador slot, BotaoGamepad b) {
        for (ItemGamepad i : todosItensComBotao(slot).keySet()) {
            if (todosItensComBotao(slot).get(i) == b) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private Map<ItemGamepad, BotaoGamepad> todosItensComBotao(SlotJogador slot) {
        Map<ItemGamepad, BotaoGamepad> m = new LinkedHashMap<>();
        slots.get(slot).acoes().forEach((a, b) -> m.put(ItemGamepad.de(a), b));
        slots.get(slot).comandos().forEach((c, b) -> m.put(ItemGamepad.de(c), b));
        return m;
    }

    /**
     * Pares que podem dividir um botao: arrasto (com o analogico) e camada 3
     * (com LT); correr (segurado) e modificador do pulo alto (RB + A = pulo
     * alto correndo).
     */
    public static boolean podemDividir(ItemGamepad x, ItemGamepad y) {
        return par(x, y, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, ComandoGamepad.MODIFICADOR_CAMADA_3)
                || par(x, y, AcaoJogador.CORRER, ComandoGamepad.MODIFICADOR_PULO_ALTO)
                || parDeAcoes(x, y, AcaoJogador.TENTAR_DESEQUILIBRIO, AcaoJogador.ROMPIMENTO_TECNICO)
                // 5O: RB = pulo alto (com o A), corrida (Classico) e quedas (com o RS); Y = reforco/acrobacia.
                || par(x, y, AcaoJogador.MODIFICADOR_PROJECAO, ComandoGamepad.MODIFICADOR_PULO_ALTO)
                || parDeAcoes(x, y, AcaoJogador.CORRER, AcaoJogador.MODIFICADOR_PROJECAO)
                || parDeAcoes(x, y, AcaoJogador.REFORCAR_PEGADA, AcaoJogador.ACROBACIA);
    }

    /**
     * 5I: desequilibrar (aplicador) e romper (receptor) nunca valem ao mesmo
     * tempo; o mesmo botao serve aos dois e o GamePanel usa o do papel real.
     */
    private static boolean parDeAcoes(ItemGamepad x, ItemGamepad y, AcaoJogador a, AcaoJogador b) {
        return (x.acao() == a && y.acao() == b) || (x.acao() == b && y.acao() == a);
    }

    private static boolean par(ItemGamepad x, ItemGamepad y, AcaoJogador a, ComandoGamepad c) {
        return (x.acao() == a && y.comando() == c) || (y.acao() == a && x.comando() == c);
    }

    private static boolean parPermitido(ItemGamepad x, ItemGamepad y) {
        return podemDividir(x, y);
    }

    public List<Problema> problemas() {
        List<Problema> l = new ArrayList<>();
        for (SlotJogador s : SlotJogador.values()) {
            Map<ItemGamepad, BotaoGamepad> m = todosItensComBotao(s);
            List<ItemGamepad> vistos = new ArrayList<>();
            for (Map.Entry<ItemGamepad, BotaoGamepad> e : m.entrySet()) {
                if (RESERVADOS.contains(e.getValue())) {
                    l.add(new Problema(s, e.getKey(), e.getValue(), null));
                }
                for (ItemGamepad anterior : vistos) {
                    if (m.get(anterior) == e.getValue() && !parPermitido(anterior, e.getKey())) {
                        l.add(new Problema(s, e.getKey(), e.getValue(), anterior));
                    }
                }
                vistos.add(e.getKey());
            }
        }
        return l;
    }

    public boolean isValido() {
        return problemas().isEmpty();
    }

    // =====================================================
    // EDICAO (sempre um novo mapa)
    // =====================================================
    private MapaGamepad com(SlotJogador slot, ConfigSlot c) {
        Map<SlotJogador, ConfigSlot> m = new EnumMap<>(slots);
        m.put(slot, c);
        return new MapaGamepad(m);
    }

    /** Troca o esquema do slot: o slot volta aos botoes padrao desse esquema. */
    public MapaGamepad comEsquema(SlotJogador slot, EsquemaGamepad esquema) {
        return com(slot, padraoDo(slot, esquema));
    }

    public MapaGamepad comPadroesDo(SlotJogador slot) {
        return comEsquema(slot, esquema(slot));
    }

    public MapaGamepad comConfig(SlotJogador slot, ConfigSlot config) {
        return com(slot, config);
    }

    public MapaGamepad comBotao(SlotJogador slot, ItemGamepad item, BotaoGamepad botao) {
        exigirItem(slot, item);
        ConfigSlot c = slots.get(slot);
        Map<AcaoJogador, BotaoGamepad> a = new EnumMap<>(AcaoJogador.class);
        a.putAll(c.acoes());
        Map<ComandoGamepad, BotaoGamepad> k = new EnumMap<>(ComandoGamepad.class);
        k.putAll(c.comandos());
        if (item.acao() != null) {
            if (botao == null) {
                a.remove(item.acao());
            } else {
                a.put(item.acao(), botao);
            }
        } else if (botao == null) {
            k.remove(item.comando());
        } else {
            k.put(item.comando(), botao);
        }
        return com(slot, new ConfigSlot(c.esquema(), a, k));
    }

    public MapaGamepad semBotao(SlotJogador slot, ItemGamepad item) {
        return comBotao(slot, item, null);
    }

    /** Atribui; quem usava o botao no MESMO slot recebe o botao antigo deste item (troca). */
    public MapaGamepad trocandoSeNecessario(SlotJogador slot, ItemGamepad item, BotaoGamepad botao) {
        Optional<BotaoGamepad> antigo = botao(slot, item);
        MapaGamepad novo = comBotao(slot, item, botao);
        for (ItemGamepad outro : todosItensComBotao(slot).keySet()) {
            if (!outro.equals(item) && botao(slot, outro).orElse(null) == botao && !parPermitido(outro, item)) {
                MapaGamepad trocado = antigo.isPresent() ? novo.comBotao(slot, outro, antigo.get()) : novo.semBotao(slot, outro);
                // O botao antigo pode estar dividido (ex.: RB = correr + pulo alto): se a troca
                // criaria conflito, o outro item fica sem botao (nunca um mapa invalido).
                boolean outroEmConflito = trocado.problemas().stream()
                        .anyMatch(pr -> pr.slot() == slot && (pr.item().equals(outro) || outro.equals(pr.outro())));
                novo = outroEmConflito ? novo.semBotao(slot, outro) : trocado;
            }
        }
        return novo;
    }

    private void exigirItem(SlotJogador slot, ItemGamepad item) {
        if (item.acao() != null && !slot.usa(item.acao())) {
            throw new IllegalArgumentException(slot.getNome() + " nao usa " + item.acao());
        }
        if (item.comando() != null && item.comando().isSoContextual() && esquema(slot) != EsquemaGamepad.CONTEXTUAL) {
            throw new IllegalArgumentException("Comandos de tecnica so existem no esquema contextual.");
        }
    }

    // =====================================================
    // LEITURA (FonteGamepad, uma vez por tick)
    // =====================================================
    /** Acoes seguradas agora pelo slot (botoes + analogico, sem repeticao). */
    public Set<AcaoJogador> acoesAtivas(EstadoGamepad estado, SlotJogador slot, Set<AcaoJogador> analogicoAnterior,
            Set<AcaoJogador> analogicoAtual) {
        ConfigSlot c = slots.get(slot);
        Set<AcaoJogador> ativas = EnumSet.noneOf(AcaoJogador.class);
        for (Map.Entry<AcaoJogador, BotaoGamepad> e : c.acoes().entrySet()) {
            if (slot.usa(e.getKey()) && estado.pressionado(e.getValue())) {
                ativas.add(e.getKey());
            }
        }
        analogicoAtual.clear();
        analogicoAtual.addAll(LeitorAnalogico.direcoes(
                estado.eixo(EixoGamepad.ESQ_X), estado.eixo(EixoGamepad.ESQ_Y), analogicoAnterior));
        ativas.addAll(analogicoAtual);
        if (c.esquema() == EsquemaGamepad.CONTEXTUAL && slot.usa(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO)
                && (analogicoAtual.contains(AcaoJogador.MOVER_ESQUERDA)
                        || analogicoAtual.contains(AcaoJogador.MOVER_DIREITA)
                        || analogicoAtual.contains(AcaoJogador.MOVER_CIMA)
                        || analogicoAtual.contains(AcaoJogador.MOVER_BAIXO))) {
            /*
             * 5H: arrasto AUTOMATICO. Andar com o analogico vale como "modo controle";
             * o GamePanel so arrasta se este lutador for o APLICADOR de uma pegada
             * estabelecida (regra da 5B). Fora disso nada muda. 5N: tambem para
             * frente/fundo e nas diagonais (a dupla muda de linha junta).
             */
            ativas.add(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO);
        }
        return ativas;
    }

    /** Direcoes de tecnica e modificadores segurados (NADA no esquema classico). */
    public EntradaContextual entradaContextual(EstadoGamepad estado, SlotJogador slot) {
        ConfigSlot c = slots.get(slot);
        if (c.esquema() != EsquemaGamepad.CONTEXTUAL) {
            return EntradaContextual.NADA;
        }
        Set<DirecaoTecnica> dirs = EnumSet.noneOf(DirecaoTecnica.class);
        for (ComandoGamepad k : ComandoGamepad.values()) {
            BotaoGamepad b = c.comandos().get(k);
            if (k.getDirecao() != null && b != null && estado.pressionado(b)) {
                dirs.add(k.getDirecao());
            }
        }
        return new EntradaContextual(dirs, segurado(c, ComandoGamepad.MODIFICADOR_CAMADA_2, estado),
                segurado(c, ComandoGamepad.MODIFICADOR_CAMADA_3, estado));
    }

    private static boolean segurado(ConfigSlot c, ComandoGamepad k, EstadoGamepad e) {
        BotaoGamepad b = c.comandos().get(k);
        return b != null && e.pressionado(b);
    }

    // =====================================================
    // ROTULOS
    // =====================================================
    /** Botoes que acionam a acao neste slot (0 ou 1). */
    public List<BotaoGamepad> botoes(SlotJogador slot, AcaoJogador acao) {
        if (!slot.usa(acao)) {
            return List.of();
        }
        BotaoGamepad b = slots.get(slot).acoes().get(acao);
        return b == null ? List.of() : List.of(b);
    }

    /** Rotulo curto para ajuda e HUD ("B/Bola", "D-pad ← / analógico", "RB/R1 + A/Cruz", "—"). */
    public String rotulo(SlotJogador slot, AcaoJogador acao) {
        if (acao == AcaoJogador.PULAR && botoes(slot, acao).isEmpty()) {
            Optional<BotaoGamepad> mod = botao(slot, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO));
            List<BotaoGamepad> pulo = botoes(slot, AcaoJogador.PULO_CURTO);
            if (mod.isPresent() && !pulo.isEmpty()) {
                return mod.get().getRotulo() + " + " + pulo.get(0).getRotulo();
            }
        }
        List<String> partes = new ArrayList<>();
        for (BotaoGamepad b : botoes(slot, acao)) {
            partes.add(b.getRotulo());
        }
        if (isMovimento(acao) && slot.usa(acao)) {
            partes.add("analógico");
        }
        return partes.isEmpty() ? "—" : String.join(" / ", partes);
    }

    public String rotulo(SlotJogador slot, ItemGamepad item) {
        if (item.acao() != null) {
            return rotulo(slot, item.acao());
        }
        return botao(slot, item).map(BotaoGamepad::getRotulo).orElse("—");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MapaGamepad m && slots.equals(m.slots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slots);
    }

    @Override
    public String toString() {
        return "MapaGamepad" + slots;
    }
}
