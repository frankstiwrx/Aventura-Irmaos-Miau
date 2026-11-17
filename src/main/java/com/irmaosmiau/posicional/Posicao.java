package com.irmaosmiau.posicional;

/**
 * Posicao da DUPLA (Entrega 5E). Nao e o estado corporal de um lutador
 * (andando, agachado, deitado...), que continua no IrmaoMiau.
 *
 * jogavelNoJogo: so posicoes com logica E representacao visual prontas.
 * As de chao estao modeladas e testaveis (simulador), mas o MotorPosicional
 * em modo JOGO recusa qualquer transicao que possa terminar nelas.
 */
public enum Posicao {

    EM_PE_SEPARADOS("Em pé, separados", false, true),
    EM_PE_PEGADA("Em pé, disputa de pegada", false, true),
    GUARDA("Guarda", true, false),
    MEIA_GUARDA("Meia-guarda", true, false),
    CONTROLE_LATERAL("Controle lateral (cem quilos)", true, false),
    MONTADA("Montada", true, false),
    COSTAS("Controle das costas", true, false),
    // ---- Entrega 5P (MotorSolo): posicoes novas do combate de solo jogavel ----
    /** Guarda aberta (quem esta por baixo controla a distancia). */
    GUARDA_ABERTA("Guarda aberta", true, false),
    /** Joelho na barriga. */
    JOELHO_BARRIGA("Joelho na barriga", true, false),
    /** Um em pe (por cima) e o outro no chao, sem vinculo: o de pe pode entrar, o de baixo pode levantar. */
    EM_PE_CONTRA_SOLO("Um em pé, outro no chão", true, false),
    /** Os dois no chao sem controle (queda sem controle consolidado, viradas, escapes para o neutro). */
    SOLO_DISPUTADO("Solo disputado", true, false),
    // ---- Entrega 5P.2: variacoes de guarda (quem esta por baixo controla) ----
    /** Borboleta: sentado, ganchos por dentro das coxas; quem esta por cima de joelhos. */
    GUARDA_BORBOLETA("Guarda borboleta", true, false),
    /** De La Riva: gancho por fora de uma perna de quem esta EM PE. */
    DE_LA_RIVA("Guarda De La Riva", true, false),
    /** Laco: a perna enrolada na MANGA do outro (exige a conexao de manga); quem esta por cima em pe. */
    GUARDA_LACO("Guarda laço", true, false),
    // ---- Entrega 5Q ----
    /** Aranha: pes nos biceps e as duas mangas presas (exige a manga). */
    GUARDA_ARANHA("Guarda aranha", true, false),
    /** Single-leg X: controle de uma perna de quem esta EM PE, por baixo. */
    SINGLE_LEG_X("Single-leg X", true, false),
    /**
     * Ashi garami: controle de perna. "Por cima" = o dono da perna (de joelhos),
     * controlador = quem segura a perna (sentado/deitado). Base das chaves de perna.
     */
    ASHI_GARAMI("Ashi garami (controle de perna)", true, false);

    private final String nome;
    private final boolean noChao;
    private final boolean jogavelNoJogo;

    Posicao(String nome, boolean noChao, boolean jogavelNoJogo) {
        this.nome = nome;
        this.noChao = noChao;
        this.jogavelNoJogo = jogavelNoJogo;
    }

    public String getNome() {
        return nome;
    }

    /** No chao existe "por cima" e "por baixo". */
    public boolean isNoChao() {
        return noChao;
    }

    /**
     * 5P: no solo disputado ninguem esta por cima; em todas as outras posicoes
     * de chao alguem esta.
     */
    public boolean exigeAlguemPorCima() {
        return noChao && this != SOLO_DISPUTADO;
    }

    /** 5P: os dois corpos estao LIGADOS (nenhum se afasta livremente; o folego nao recupera). */
    public boolean isVinculoDeSolo() {
        return noChao && this != EM_PE_CONTRA_SOLO;
    }

    /** 5P.2: alguma guarda (quem esta por baixo controla a posicao). */
    public boolean isGuarda() {
        return this == GUARDA || this == GUARDA_ABERTA || this == MEIA_GUARDA || this == GUARDA_BORBOLETA
                || this == DE_LA_RIVA || this == GUARDA_LACO || this == GUARDA_ARANHA || this == SINGLE_LEG_X;
    }

    /** 5P.2: guardas em que quem esta por cima fica EM PE (o gancho/laco prende uma perna de pe). */
    public boolean isGuardaComOutroEmPe() {
        return this == DE_LA_RIVA || this == GUARDA_LACO || this == SINGLE_LEG_X;
    }

    public boolean isJogavelNoJogo() {
        return jogavelNoJogo;
    }
}
