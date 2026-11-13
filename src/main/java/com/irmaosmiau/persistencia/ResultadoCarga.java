package com.irmaosmiau.persistencia;

import java.util.List;

/**
 * Valor carregado + avisos para mostrar ao jogador.
 *
 * Usado quando o jogo prefere seguir com valores padrao em vez de parar:
 * nesse caso o problema nunca e silencioso, ele vem nos avisos.
 */
public final class ResultadoCarga<T> {

    private final T valor;
    private final List<String> avisos;

    public ResultadoCarga(T valor, List<String> avisos) {
        this.valor = valor;
        this.avisos = avisos == null ? List.of() : List.copyOf(avisos);
    }

    public T getValor() {
        return valor;
    }

    public List<String> getAvisos() {
        return avisos;
    }

    public boolean temAvisos() {
        return !avisos.isEmpty();
    }
}
