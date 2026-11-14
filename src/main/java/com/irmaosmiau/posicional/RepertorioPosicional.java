package com.irmaosmiau.posicional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PROPOSTA (Entrega 5E): ate quatro acoes principais por posicao/papel, com
 * variacao pelo modificador (ex.: L1/LB ou SHIFT). Nao cria teclas nem
 * botoes: e a tabela que uma futura acao generica (EXECUTAR_TECNICA_1..4)
 * consultara para saber qual transicao iniciar.
 *
 * Posicao + papel do lutador -> slot (1..4, com/sem modificador) -> transicao
 * -> (resultado) -> animacao da transicao (CatalogoAnimacoes, futuro).
 */
public final class RepertorioPosicional {

    public enum SlotContextual {
        ACAO_1,
        ACAO_2,
        ACAO_3,
        ACAO_4
    }

    /** Uma opcao exibivel: slot, se precisa do modificador, e a transicao. */
    public record Opcao(SlotContextual slot, boolean comModificador, DefinicaoTransicao transicao) {
    }

    private record Chave(Posicao posicao, PapelPosicional papel) {
    }

    private record Entrada(SlotContextual slot, boolean modificador, String idTransicao) {
    }

    private final CatalogoTransicoes catalogo;
    private final Map<Chave, List<Entrada>> tabela = new java.util.HashMap<>();

    private RepertorioPosicional(CatalogoTransicoes catalogo) {
        this.catalogo = catalogo;
    }

    private RepertorioPosicional com(Posicao p, PapelPosicional papel, SlotContextual slot, boolean mod, String id) {
        catalogo.buscar(id).orElseThrow(() -> new IllegalArgumentException("Transicao inexistente: " + id));
        List<Entrada> l = tabela.computeIfAbsent(new Chave(p, papel), k -> new ArrayList<>());
        for (Entrada e : l) {
            if (e.slot() == slot && e.modificador() == mod) {
                throw new IllegalArgumentException("Slot repetido em " + p + "/" + papel + ": " + slot);
            }
        }
        l.add(new Entrada(slot, mod, id));
        return this;
    }

    /** Esboco provisorio sobre o grafo de referencia. */
    public static RepertorioPosicional referencia(CatalogoTransicoes c) {
        return new RepertorioPosicional(c)
                .com(Posicao.EM_PE_SEPARADOS, PapelPosicional.QUALQUER, SlotContextual.ACAO_1, false,
                        CatalogoTransicoes.ESTABELECER_PEGADA)
                .com(Posicao.EM_PE_PEGADA, PapelPosicional.CONTROLADOR, SlotContextual.ACAO_1, false,
                        CatalogoTransicoes.QUEDA)
                .com(Posicao.EM_PE_PEGADA, PapelPosicional.CONTROLADOR, SlotContextual.ACAO_1, true,
                        CatalogoTransicoes.PUXAR_GUARDA)
                .com(Posicao.EM_PE_PEGADA, PapelPosicional.CONTROLADO, SlotContextual.ACAO_1, true,
                        CatalogoTransicoes.PUXAR_GUARDA)
                .com(Posicao.GUARDA, PapelPosicional.POR_CIMA, SlotContextual.ACAO_1, false,
                        CatalogoTransicoes.PASSAGEM_GUARDA)
                .com(Posicao.GUARDA, PapelPosicional.POR_BAIXO, SlotContextual.ACAO_1, false,
                        CatalogoTransicoes.RASPAGEM)
                .com(Posicao.CONTROLE_LATERAL, PapelPosicional.POR_BAIXO, SlotContextual.ACAO_1, false,
                        CatalogoTransicoes.REPOSICAO_GUARDA);
    }

    /** Opcoes do lado no estado atual (respeitando o papel exigido de cada transicao). */
    public List<Opcao> opcoes(EstadoPosicional estado, Lado lado) {
        List<Opcao> r = new ArrayList<>();
        for (Map.Entry<Chave, List<Entrada>> e : tabela.entrySet()) {
            if (e.getKey().posicao() != estado.getPosicao() || !estado.temPapel(lado, e.getKey().papel())) {
                continue;
            }
            for (Entrada en : e.getValue()) {
                DefinicaoTransicao d = catalogo.buscar(en.idTransicao()).orElseThrow();
                if (estado.temPapel(lado, d.getPapelIniciador())) {
                    r.add(new Opcao(en.slot(), en.modificador(), d));
                }
            }
        }
        r.sort(java.util.Comparator.comparing((Opcao o) -> o.slot()).thenComparing(Opcao::comModificador));
        return r;
    }

    /** O que o slot faria agora (vazio = nada). */
    public Optional<DefinicaoTransicao> transicaoDo(EstadoPosicional estado, Lado lado, SlotContextual slot,
            boolean comModificador) {
        return opcoes(estado, lado).stream()
                .filter(o -> o.slot() == slot && o.comModificador() == comModificador)
                .map(Opcao::transicao).findFirst();
    }

    /** Quantos slots distintos cada posicao/papel usa (limite de 4 por papel). */
    Map<PapelPosicional, Integer> slotsUsados(Posicao p) {
        Map<PapelPosicional, Integer> m = new EnumMap<>(PapelPosicional.class);
        tabela.forEach((k, v) -> {
            if (k.posicao() == p) {
                m.put(k.papel(), (int) v.stream().map(Entrada::slot).distinct().count());
            }
        });
        return m;
    }
}
