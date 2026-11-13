package com.irmaosmiau.conteudo;

/**
 * Erro de conteudo (JSON de animacao, tecnica, catalogo...).
 *
 * A mensagem sempre tenta dizer ONDE esta o problema:
 * "[arquivo] caminho.do.campo: o que esta errado".
 *
 * Estende IllegalArgumentException para manter o mesmo contrato de
 * validacao que as classes do motor ja usam.
 */
public class ConteudoInvalidoException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public ConteudoInvalidoException(String mensagem) {
        super(mensagem);
    }

    public ConteudoInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    /** Monta a mensagem padrao "[origem] caminho: detalhe". */
    public static ConteudoInvalidoException em(
            String origem,
            String caminho,
            String detalhe
    ) {
        StringBuilder sb = new StringBuilder();
        if (origem != null && !origem.isBlank()) {
            sb.append('[').append(origem).append("] ");
        }
        if (caminho != null && !caminho.isBlank()) {
            sb.append(caminho).append(": ");
        }
        sb.append(detalhe);
        return new ConteudoInvalidoException(sb.toString());
    }
}
