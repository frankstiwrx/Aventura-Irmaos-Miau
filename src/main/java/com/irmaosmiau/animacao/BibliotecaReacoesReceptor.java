package com.irmaosmiau.animacao;

import com.irmaosmiau.conteudo.ConteudoInvalidoException;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Entrega 5N: biblioteca das reacoes do receptor, por TECNICA.
 *
 * Convencao: a reacao da tecnica "pegada_N" fica em
 * {@code /conteudo/animacoes/receptor/receptor_pegada_N.json}. Uma tecnica
 * nova so precisa de um arquivo novo; um personagem novo nao precisa de nada
 * (usa as reacoes que ja existem, adaptadas ao seu rig).
 *
 * Tecnica sem arquivo = sem reacao (o receptor fica com a pose do motor, como
 * antes da 5N).
 */
public final class BibliotecaReacoesReceptor {

    public static final String PASTA = "/conteudo/animacoes/receptor/";
    /** As tecnicas de pegada que ja tem reacao no jogo. */
    public static final List<String> TECNICAS_DO_JOGO = List.of("pegada_1", "pegada_2", "pegada_3", "pegada_4");

    private final Map<String, ReacaoReceptor> reacoes = new LinkedHashMap<>();

    public static String caminho(String idTecnica) {
        return PASTA + "receptor_" + idTecnica + ".json";
    }

    /** As reacoes do jogo (classpath: NetBeans e JAR). */
    public static BibliotecaReacoesReceptor padrao() {
        BibliotecaReacoesReceptor b = new BibliotecaReacoesReceptor();
        for (String id : TECNICAS_DO_JOGO) {
            b.registrar(ReacaoReceptor.de(id, CarregadorAnimacaoJson.reacaoReceptorDoRecurso(caminho(id))));
        }
        return b;
    }

    /** Tenta carregar a reacao de uma tecnica pela convencao (vazio se o arquivo nao existe). */
    public Optional<ReacaoReceptor> carregarSeExistir(String idTecnica) {
        String c = caminho(idTecnica);
        try (InputStream in = getClass().getResourceAsStream(c)) {
            if (in == null) {
                return Optional.empty();
            }
        } catch (java.io.IOException ex) {
            throw new ConteudoInvalidoException("Falha ao ler " + c, ex);
        }
        ReacaoReceptor r = ReacaoReceptor.de(idTecnica, CarregadorAnimacaoJson.reacaoReceptorDoRecurso(c));
        registrar(r);
        return Optional.of(r);
    }

    public BibliotecaReacoesReceptor registrar(ReacaoReceptor r) {
        reacoes.put(r.getIdTecnica(), r);
        return this;
    }

    public Optional<ReacaoReceptor> buscar(String idTecnica) {
        return Optional.ofNullable(idTecnica == null ? null : reacoes.get(idTecnica));
    }

    public Set<String> tecnicas() {
        return java.util.Collections.unmodifiableSet(reacoes.keySet());
    }
}
