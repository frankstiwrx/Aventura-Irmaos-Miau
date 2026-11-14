package com.irmaosmiau.posicional;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Uma transicao entre posicoes (Entrega 5E): origem, papel exigido de quem
 * inicia, condicoes, duracao em TICKS do loop logico e o desfecho de cada
 * resultado possivel. Os destinos estao nos desfechos.
 *
 * idTecnica liga a transicao ao catalogo de tecnicas (conteudo/tecnicas)
 * quando ela ja existe no jogo (ex.: "pegada_1"); vazio = so modelada.
 */
public final class DefinicaoTransicao {

    private final String id;
    private final String nome;
    private final Posicao origem;
    private final PapelPosicional papelIniciador;
    private final Cadencia cadencia;
    private final int duracaoTicks;
    private final List<CondicaoTransicao> condicoes;
    private final Map<ResultadoTransicao, Desfecho> desfechos;
    private final String idTecnica;

    public DefinicaoTransicao(String id, String nome, Posicao origem, PapelPosicional papelIniciador,
            Cadencia cadencia, int duracaoTicks, List<CondicaoTransicao> condicoes,
            Map<ResultadoTransicao, Desfecho> desfechos, String idTecnica) {
        if (id == null || id.isBlank() || origem == null || papelIniciador == null || cadencia == null) {
            throw new IllegalArgumentException("Transicao incompleta: " + id);
        }
        if (duracaoTicks < 1) {
            throw new IllegalArgumentException("Duracao em ticks precisa ser >= 1: " + id);
        }
        if (desfechos == null || !desfechos.containsKey(ResultadoTransicao.SUCESSO)) {
            throw new IllegalArgumentException("Toda transicao precisa de um desfecho de SUCESSO: " + id);
        }
        this.id = id;
        this.nome = nome;
        this.origem = origem;
        this.papelIniciador = papelIniciador;
        this.cadencia = cadencia;
        this.duracaoTicks = duracaoTicks;
        this.condicoes = List.copyOf(condicoes == null ? List.of() : condicoes);
        this.desfechos = Collections.unmodifiableMap(new EnumMap<>(desfechos));
        this.idTecnica = idTecnica;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Posicao getOrigem() {
        return origem;
    }

    public PapelPosicional getPapelIniciador() {
        return papelIniciador;
    }

    public Cadencia getCadencia() {
        return cadencia;
    }

    public int getDuracaoTicks() {
        return duracaoTicks;
    }

    public List<CondicaoTransicao> getCondicoes() {
        return condicoes;
    }

    public Set<ResultadoTransicao> resultadosPossiveis() {
        return desfechos.keySet();
    }

    public Optional<Desfecho> desfecho(ResultadoTransicao r) {
        return Optional.ofNullable(desfechos.get(r));
    }

    /** Posicoes em que a dupla pode terminar (para saber se a transicao e jogavel). */
    public Set<Posicao> destinosPossiveis() {
        Set<Posicao> s = java.util.EnumSet.noneOf(Posicao.class);
        desfechos.values().forEach(d -> s.add(d.destino()));
        return s;
    }

    public Optional<String> getIdTecnica() {
        return Optional.ofNullable(idTecnica);
    }

    @Override
    public String toString() {
        return id;
    }
}
