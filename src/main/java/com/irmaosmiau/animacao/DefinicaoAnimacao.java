package com.irmaosmiau.animacao;

import java.util.Optional;

/**
 * Identifica uma animacao no CatalogoAnimacoes (Entrega 5E).
 *
 * @param id         unico (ex.: "branco.caminhada.legada", "branco.movimento_chao.json")
 * @param estado     para qual estado visual ela serve
 * @param rig        esqueleto compativel ("miau_v1" = pose de 30 valores do MiauStudio)
 * @param fonte      codigo legado, JSON individual ou conjunta do MiauStudio
 * @param referencia onde esta o comportamento (classe/metodo/array legado, recurso JSON
 *                   ou id do RegistroAnimacoesCombate)
 * @param dados      poses carregadas (so JSON_INDIVIDUAL)
 */
public record DefinicaoAnimacao(String id, EstadoVisual estado, String rig, FonteAnimacao fonte,
        String referencia, AnimacaoIndividual dados) {

    public DefinicaoAnimacao {
        if (id == null || id.isBlank() || estado == null || rig == null || fonte == null) {
            throw new IllegalArgumentException("Definicao de animacao incompleta: " + id);
        }
        if ((fonte == FonteAnimacao.JSON_INDIVIDUAL) != (dados != null)) {
            throw new IllegalArgumentException(id + ": so animacoes JSON carregam poses.");
        }
        if (dados != null && (dados.getEstado() != estado || !dados.getRig().equals(rig))) {
            throw new IllegalArgumentException(id + ": estado/rig do arquivo nao conferem.");
        }
    }

    public static DefinicaoAnimacao legada(String id, EstadoVisual estado, String referencia) {
        return new DefinicaoAnimacao(id, estado, "miau_v1",
                estado.isConjunto() ? FonteAnimacao.MIAUSTUDIO_CONJUNTA : FonteAnimacao.LEGADA_CODIGO,
                referencia, null);
    }

    /** Entrega 5K: animacao gerada em codigo a partir do rig (fallback substituivel). */
    public static DefinicaoAnimacao procedural(String id, EstadoVisual estado, String referencia) {
        return new DefinicaoAnimacao(id, estado, "miau_v1", FonteAnimacao.PROCEDURAL, referencia, null);
    }

    public static DefinicaoAnimacao json(String id, String recurso, AnimacaoIndividual dados) {
        return new DefinicaoAnimacao(id, dados.getEstado(), dados.getRig(), FonteAnimacao.JSON_INDIVIDUAL,
                recurso, dados);
    }

    public Optional<AnimacaoIndividual> getDados() {
        return Optional.ofNullable(dados);
    }
}
