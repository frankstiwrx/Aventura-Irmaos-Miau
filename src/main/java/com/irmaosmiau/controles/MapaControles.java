package com.irmaosmiau.controles;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Qual tecla fisica aciona cada acao logica, nos dois slots (Entrega 5C).
 *
 * Imutavel: cada alteracao devolve um novo mapa (a tela de controles edita
 * uma copia e so troca a oficial ao salvar).
 *
 * Regras (ver {@link #problemas()}):
 * - uma tecla aciona no maximo UMA acao, considerando os dois jogadores
 *   juntos (mesmo teclado; nunca ha ambiguidade sobre quem agiu);
 * - ESC e F1..F12 sao reservadas;
 * - toda acao usada pelo slot deve ter tecla.
 * O modificador (MODO_DESLOCAMENTO_CONTROLADO) e uma acao como as outras:
 * pode ser qualquer tecla livre, e o arrasto continua funcionando com as
 * teclas de esquerda/direita configuradas.
 */
public final class MapaControles {

    /** Quem usa uma tecla. */
    public record Atribuicao(SlotJogador slot, AcaoJogador acao) {
    }

    private final Map<SlotJogador, Map<AcaoJogador, TeclaFisica>> teclas;

    private MapaControles(Map<SlotJogador, Map<AcaoJogador, TeclaFisica>> teclas) {
        this.teclas = teclas;
    }

    // =====================================================
    // CRIACAO
    // =====================================================
    /** Os controles de sempre (identicos aos anteriores a 5C). */
    public static MapaControles padrao() {
        Map<SlotJogador, Map<AcaoJogador, TeclaFisica>> m = vazio();
        for (SlotJogador s : SlotJogador.values()) {
            for (AcaoJogador a : s.acoes()) {
                m.get(s).put(a, teclaPadrao(s, a));
            }
        }
        return new MapaControles(m);
    }

    /** Mapa sem nenhuma tecla (base do carregamento de arquivos). */
    public static MapaControles semTeclas() {
        return new MapaControles(vazio());
    }

    public static TeclaFisica teclaPadrao(SlotJogador slot, AcaoJogador acao) {
        exigirUso(slot, acao);
        return TeclaFisica.exigir(acao.nomeTeclaPadrao(slot));
    }

    private static Map<SlotJogador, Map<AcaoJogador, TeclaFisica>> vazio() {
        Map<SlotJogador, Map<AcaoJogador, TeclaFisica>> m = new EnumMap<>(SlotJogador.class);
        for (SlotJogador s : SlotJogador.values()) {
            m.put(s, new EnumMap<>(AcaoJogador.class));
        }
        return m;
    }

    private Map<SlotJogador, Map<AcaoJogador, TeclaFisica>> copia() {
        Map<SlotJogador, Map<AcaoJogador, TeclaFisica>> m = vazio();
        for (SlotJogador s : SlotJogador.values()) {
            m.get(s).putAll(teclas.get(s));
        }
        return m;
    }

    private static void exigirUso(SlotJogador slot, AcaoJogador acao) {
        if (slot == null || acao == null) {
            throw new IllegalArgumentException("Slot e acao sao obrigatorios.");
        }
        if (!slot.usa(acao)) {
            throw new IllegalArgumentException(slot.getNome() + " nao usa a acao " + acao);
        }
    }

    // =====================================================
    // CONSULTAS
    // =====================================================
    public Optional<TeclaFisica> tecla(SlotJogador slot, AcaoJogador acao) {
        return Optional.ofNullable(teclas.get(slot).get(acao));
    }

    /** Rotulo da tecla ou "—" se a acao estiver sem tecla. */
    public String rotulo(SlotJogador slot, AcaoJogador acao) {
        return tecla(slot, acao).map(TeclaFisica::getRotulo).orElse("—");
    }

    /** Primeira atribuicao que usa a tecla (ordem: slot, acao). */
    public Optional<Atribuicao> quemUsa(TeclaFisica tecla) {
        for (SlotJogador s : SlotJogador.values()) {
            for (Map.Entry<AcaoJogador, TeclaFisica> e : teclas.get(s).entrySet()) {
                if (e.getValue() == tecla) {
                    return Optional.of(new Atribuicao(s, e.getKey()));
                }
            }
        }
        return Optional.empty();
    }

    /** Todas as atribuicoes de um slot, na ordem das acoes. */
    public Map<AcaoJogador, TeclaFisica> doSlot(SlotJogador slot) {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(teclas.get(slot)));
    }

    public boolean ehPadrao() {
        return equals(padrao());
    }

    // =====================================================
    // ALTERACOES (sempre devolvem um novo mapa)
    // =====================================================
    public MapaControles comTecla(SlotJogador slot, AcaoJogador acao, TeclaFisica tecla) {
        exigirUso(slot, acao);
        if (tecla == null) {
            throw new IllegalArgumentException("Tecla obrigatoria (use semTecla para remover).");
        }
        Map<SlotJogador, Map<AcaoJogador, TeclaFisica>> m = copia();
        m.get(slot).put(acao, tecla);
        return new MapaControles(m);
    }

    public MapaControles semTecla(SlotJogador slot, AcaoJogador acao) {
        exigirUso(slot, acao);
        Map<SlotJogador, Map<AcaoJogador, TeclaFisica>> m = copia();
        m.get(slot).remove(acao);
        return new MapaControles(m);
    }

    /**
     * Atribui a tecla; se outra acao ja a usava, essa outra recebe a tecla
     * antiga desta acao (troca). Nunca cria repeticao.
     */
    public MapaControles trocandoSeNecessario(SlotJogador slot, AcaoJogador acao, TeclaFisica tecla) {
        exigirUso(slot, acao);
        Optional<Atribuicao> dono = quemUsa(tecla);
        MapaControles novo = comTecla(slot, acao, tecla);
        if (dono.isPresent() && !dono.get().equals(new Atribuicao(slot, acao))) {
            Optional<TeclaFisica> antiga = tecla(slot, acao);
            novo = antiga.isPresent()
                    ? novo.comTecla(dono.get().slot(), dono.get().acao(), antiga.get())
                    : novo.semTecla(dono.get().slot(), dono.get().acao());
        }
        return novo;
    }

    /** Volta um slot aos padroes, mantendo o outro como esta. */
    public MapaControles comPadroesDo(SlotJogador slot) {
        Map<SlotJogador, Map<AcaoJogador, TeclaFisica>> m = copia();
        m.get(slot).clear();
        for (AcaoJogador a : slot.acoes()) {
            m.get(slot).put(a, teclaPadrao(slot, a));
        }
        return new MapaControles(m);
    }

    // =====================================================
    // VALIDACAO
    // =====================================================
    public List<ProblemaControle> problemas() {
        List<ProblemaControle> lista = new ArrayList<>();
        Map<TeclaFisica, Atribuicao> vistas = new LinkedHashMap<>();
        for (SlotJogador s : SlotJogador.values()) {
            for (AcaoJogador a : s.acoes()) {
                TeclaFisica t = teclas.get(s).get(a);
                if (t == null) {
                    lista.add(new ProblemaControle(ProblemaControle.Tipo.SEM_TECLA, s, a, null, null, null));
                    continue;
                }
                if (t.isReservada()) {
                    lista.add(new ProblemaControle(ProblemaControle.Tipo.TECLA_RESERVADA, s, a, t, null, null));
                }
                Atribuicao anterior = vistas.putIfAbsent(t, new Atribuicao(s, a));
                if (anterior != null) {
                    lista.add(new ProblemaControle(ProblemaControle.Tipo.TECLA_REPETIDA, s, a, t,
                            anterior.slot(), anterior.acao()));
                }
            }
        }
        return lista;
    }

    public boolean isValido() {
        return problemas().isEmpty();
    }

    // =====================================================
    // IGUALDADE
    // =====================================================
    @Override
    public boolean equals(Object o) {
        return o instanceof MapaControles outro && teclas.equals(outro.teclas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teclas);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MapaControles{");
        for (SlotJogador s : SlotJogador.values()) {
            sb.append(s.getSigla()).append('=');
            Map<String, String> nomes = new LinkedHashMap<>();
            teclas.get(s).forEach((a, t) -> nomes.put(a.name(), t.getNome()));
            sb.append(nomes).append(' ');
        }
        return sb.append('}').toString();
    }
}
