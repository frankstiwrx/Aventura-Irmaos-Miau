package com.irmaosmiau.combate;

/**
 * Entrega 5J: qual animacao conjunta foi escolhida para uma dupla.
 *
 * @param idBase             animacao pedida pela tecnica (ex.: pegada_1_entrada)
 * @param idTrilhaAplicador  animacao de onde vem a trilha do aplicador
 * @param idTrilhaReceptor   animacao de onde vem a trilha do receptor
 * @param quadros            os quadros efetivamente reproduzidos
 * @param tipo               como a escolha foi feita
 */
public record SelecaoAnimacaoConjunta(
        String idBase,
        String idTrilhaAplicador,
        String idTrilhaReceptor,
        AnimacaoConjuntaQuadros quadros,
        Tipo tipo,
        boolean experimental) {

    /** Como na 5J (nao experimental). */
    public SelecaoAnimacaoConjunta(String idBase, String idTrilhaAplicador, String idTrilhaReceptor,
            AnimacaoConjuntaQuadros quadros, Tipo tipo) {
        this(idBase, idTrilhaAplicador, idTrilhaReceptor, quadros, tipo, false);
    }

    /** Como a variante foi escolhida. */
    public enum Tipo {
        /** Uma animacao autoral desenhada exatamente para esta dupla e estes papeis. */
        AUTORAL,
        /**
         * Cada trilha veio de uma animacao autoral desenhada para o MESMO molde
         * no MESMO papel (ex.: Branco x Branco). Nada e copiado entre moldes.
         */
        COMPOSTA_POR_PAPEL,
        /** Sem variante compativel: a animacao base (comportamento ate a 5I). */
        BASE
    }

    /** Texto curto para o HUD tecnico. */
    public String descricao() {
        if (experimental) {
            return tipo == Tipo.COMPOSTA_POR_PAPEL
                    ? "aplicador " + idTrilhaAplicador + " + receptor " + idTrilhaReceptor + " (EXPERIMENTAL, composta)"
                    : idTrilhaAplicador + " (EXPERIMENTAL)";
        }
        return switch (tipo) {
            case AUTORAL -> idTrilhaAplicador + " (autoral)";
            case COMPOSTA_POR_PAPEL -> "aplicador " + idTrilhaAplicador + " + receptor " + idTrilhaReceptor
                    + " (composta por papel)";
            case BASE -> idBase + " (base)";
        };
    }
}
