package com.irmaosmiau.regras;

/**
 * Pontuacoes do modo PONTOS + FINALIZACAO, pela tabela do Livro de Regras da IBJJF (jun/2024, art. 2.5.2):
 * 4 pontos montada e controle das costas; 3 passagem de guarda; 2 queda, raspagem e joelho na barriga.
 * Os criterios de cada uma (art. 4) ficam no {@link ArbitroJiuJitsu}.
 */
public enum Pontuacao {
    QUEDA(2, "QUEDA"),
    RASPAGEM(2, "RASPAGEM"),
    JOELHO_BARRIGA(2, "JOELHO NA BARRIGA"),
    PASSAGEM(3, "PASSAGEM"),
    MONTADA(4, "MONTADA"),
    COSTAS(4, "COSTAS");

    private final int pontos;
    private final String rotulo;

    Pontuacao(int pontos, String rotulo) {
        this.pontos = pontos;
        this.rotulo = rotulo;
    }

    public int getPontos() {
        return pontos;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** "MONTADA +4" (notificacao do placar). */
    public String aviso() {
        return rotulo + " +" + pontos;
    }

    /** Posicao que se pontua pelo dominio (pode voltar a valer depois que o outro escapar). */
    public boolean isPosicional() {
        return this == JOELHO_BARRIGA || this == PASSAGEM || this == MONTADA || this == COSTAS;
    }
}
