package com.irmaosmiau.posicional;

import com.irmaosmiau.posicional.Desfecho.RegraLado;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transicoes conhecidas (Entrega 5E).
 *
 * Referencia: EM PE -> PEGADA -> QUEDA -> GUARDA -> PASSAGEM -> CONTROLE
 * LATERAL, com caminhos de defesa e inversao (queda defendida/revertida,
 * passagem defendida, raspagem, reposicao de guarda, puxada para a guarda).
 *
 * So "estabelecer_pegada" existe no jogo (e a PEGADA_1, 30 ticks, ja feita
 * pelo SistemaCombate). As demais duracoes sao PROVISORIAS e servem apenas
 * ao simulador e aos testes: nenhuma formula de balanceamento nova.
 */
public final class CatalogoTransicoes {

    public static final String ESTABELECER_PEGADA = "estabelecer_pegada";
    public static final String QUEDA = "queda";
    public static final String PUXAR_GUARDA = "puxar_guarda";
    public static final String PASSAGEM_GUARDA = "passagem_guarda";
    public static final String RASPAGEM = "raspagem";
    public static final String REPOSICAO_GUARDA = "reposicao_guarda";

    private final Map<String, DefinicaoTransicao> porId = new LinkedHashMap<>();

    public CatalogoTransicoes(Collection<DefinicaoTransicao> definicoes) {
        for (DefinicaoTransicao d : definicoes) {
            if (porId.putIfAbsent(d.getId(), d) != null) {
                throw new IllegalArgumentException("Transicao repetida: " + d.getId());
            }
        }
    }

    public Optional<DefinicaoTransicao> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }

    public Collection<DefinicaoTransicao> todas() {
        return porId.values();
    }

    /** Transicoes que comecam na posicao. */
    public List<DefinicaoTransicao> aPartirDe(Posicao origem) {
        return porId.values().stream().filter(d -> d.getOrigem() == origem).toList();
    }

    private static Map<ResultadoTransicao, Desfecho> desfechos(Object... pares) {
        Map<ResultadoTransicao, Desfecho> m = new EnumMap<>(ResultadoTransicao.class);
        for (int i = 0; i < pares.length; i += 2) {
            m.put((ResultadoTransicao) pares[i], (Desfecho) pares[i + 1]);
        }
        return m;
    }

    private static Desfecho para(Posicao destino, RegraLado cima, RegraLado controle) {
        return new Desfecho(destino, cima, controle);
    }

    /** O grafo de referencia da 5E. */
    public static CatalogoTransicoes referencia() {
        RegraLado ini = RegraLado.INICIADOR;
        RegraLado out = RegraLado.OUTRO;
        RegraLado nada = RegraLado.NENHUM;
        return new CatalogoTransicoes(List.of(
                // Em pe: a PEGADA_1 atual (30 quadros de entrada; o SistemaCombate executa).
                new DefinicaoTransicao(ESTABELECER_PEGADA, "Estabelecer pegada", Posicao.EM_PE_SEPARADOS,
                        PapelPosicional.QUALQUER, Cadencia.PREPARADA, 30, List.of(),
                        desfechos(ResultadoTransicao.SUCESSO, para(Posicao.EM_PE_PEGADA, nada, ini),
                                ResultadoTransicao.FALHA, Desfecho.permanece(Posicao.EM_PE_SEPARADOS)),
                        "pegada_1"),
                // Queda: quem controla a pegada derruba; o outro pode defender ou reverter (sprawl).
                new DefinicaoTransicao(QUEDA, "Queda", Posicao.EM_PE_PEGADA,
                        PapelPosicional.CONTROLADOR, Cadencia.PREPARADA, 45,
                        List.of(CondicaoTransicao.PEGADA_ESTABELECIDA),
                        desfechos(ResultadoTransicao.SUCESSO, para(Posicao.GUARDA, ini, ini),
                                ResultadoTransicao.DEFENDIDA, Desfecho.permanece(Posicao.EM_PE_PEGADA),
                                ResultadoTransicao.FALHA, Desfecho.permanece(Posicao.EM_PE_SEPARADOS),
                                ResultadoTransicao.INVERSAO, para(Posicao.GUARDA, out, out)),
                        null),
                // Puxar para a guarda: quem esta na pegada senta e fica por baixo, com a guarda.
                new DefinicaoTransicao(PUXAR_GUARDA, "Puxar para a guarda", Posicao.EM_PE_PEGADA,
                        PapelPosicional.QUALQUER, Cadencia.RAPIDA, 20,
                        List.of(CondicaoTransicao.PEGADA_ESTABELECIDA),
                        desfechos(ResultadoTransicao.SUCESSO, para(Posicao.GUARDA, out, ini),
                                ResultadoTransicao.FALHA, Desfecho.permanece(Posicao.EM_PE_PEGADA)),
                        null),
                // Passagem: quem esta por cima passa; defendida = continua na guarda.
                new DefinicaoTransicao(PASSAGEM_GUARDA, "Passagem de guarda", Posicao.GUARDA,
                        PapelPosicional.POR_CIMA, Cadencia.DISPUTA, 60, List.of(),
                        desfechos(ResultadoTransicao.SUCESSO, para(Posicao.CONTROLE_LATERAL, ini, ini),
                                ResultadoTransicao.DEFENDIDA, para(Posicao.GUARDA, RegraLado.MANTEM, out),
                                ResultadoTransicao.FALHA, Desfecho.permanece(Posicao.GUARDA),
                                ResultadoTransicao.INVERSAO, para(Posicao.MONTADA, out, out)),
                        null),
                // Raspagem: quem esta por baixo inverte e termina por cima.
                new DefinicaoTransicao(RASPAGEM, "Raspagem", Posicao.GUARDA,
                        PapelPosicional.POR_BAIXO, Cadencia.PREPARADA, 45, List.of(),
                        desfechos(ResultadoTransicao.SUCESSO, para(Posicao.MONTADA, ini, ini),
                                ResultadoTransicao.DEFENDIDA, Desfecho.permanece(Posicao.GUARDA),
                                ResultadoTransicao.FALHA, Desfecho.permanece(Posicao.GUARDA)),
                        null),
                // Reposicao: quem esta por baixo no cem quilos recupera a guarda.
                new DefinicaoTransicao(REPOSICAO_GUARDA, "Reposição de guarda", Posicao.CONTROLE_LATERAL,
                        PapelPosicional.POR_BAIXO, Cadencia.DISPUTA, 40, List.of(),
                        desfechos(ResultadoTransicao.SUCESSO, para(Posicao.GUARDA, out, ini),
                                ResultadoTransicao.DEFENDIDA, Desfecho.permanece(Posicao.CONTROLE_LATERAL),
                                ResultadoTransicao.FALHA, Desfecho.permanece(Posicao.CONTROLE_LATERAL)),
                        null)));
    }
}
