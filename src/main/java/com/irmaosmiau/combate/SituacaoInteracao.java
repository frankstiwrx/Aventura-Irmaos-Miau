package com.irmaosmiau.combate;

/**
 * Tudo que uma {@link PoliticaInteracao} precisa para decidir, num tick,
 * se a interacao ativa continua.
 *
 * Durante a ENTRADA (animacao conjunta ainda tocando) os retratos de
 * referencia sao nulos: a interacao ainda nao foi estabelecida.
 */
public final class SituacaoInteracao {

    private final AcaoCombate acao;
    private final ContextoCombate contexto;
    private final boolean entradaEmAndamento;
    private final EstadoCorporalObservado referenciaAplicador;
    private final EstadoCorporalObservado referenciaReceptor;
    private final EstadoCorporalObservado atualAplicador;
    private final EstadoCorporalObservado atualReceptor;

    public SituacaoInteracao(
            AcaoCombate acao,
            ContextoCombate contexto,
            boolean entradaEmAndamento,
            EstadoCorporalObservado referenciaAplicador,
            EstadoCorporalObservado referenciaReceptor,
            EstadoCorporalObservado atualAplicador,
            EstadoCorporalObservado atualReceptor
    ) {
        if (acao == null || contexto == null || atualAplicador == null || atualReceptor == null) {
            throw new IllegalArgumentException("Acao, contexto e estados atuais sao obrigatorios.");
        }
        if (!entradaEmAndamento && (referenciaAplicador == null || referenciaReceptor == null)) {
            throw new IllegalArgumentException(
                    "Uma interacao estabelecida precisa dos retratos de referencia.");
        }
        this.acao = acao;
        this.contexto = contexto;
        this.entradaEmAndamento = entradaEmAndamento;
        this.referenciaAplicador = referenciaAplicador;
        this.referenciaReceptor = referenciaReceptor;
        this.atualAplicador = atualAplicador;
        this.atualReceptor = atualReceptor;
    }

    public AcaoCombate getAcao() {
        return acao;
    }

    /** Fotografia aplicador -&gt; receptor deste tick. */
    public ContextoCombate getContexto() {
        return contexto;
    }

    public boolean isEntradaEmAndamento() {
        return entradaEmAndamento;
    }

    /** Null durante a entrada. */
    public EstadoCorporalObservado getReferenciaAplicador() {
        return referenciaAplicador;
    }

    /** Null durante a entrada. */
    public EstadoCorporalObservado getReferenciaReceptor() {
        return referenciaReceptor;
    }

    public EstadoCorporalObservado getAtualAplicador() {
        return atualAplicador;
    }

    public EstadoCorporalObservado getAtualReceptor() {
        return atualReceptor;
    }
}
