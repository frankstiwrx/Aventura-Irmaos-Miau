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
    COSTAS("Controle das costas", true, false);

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

    public boolean isJogavelNoJogo() {
        return jogavelNoJogo;
    }
}
