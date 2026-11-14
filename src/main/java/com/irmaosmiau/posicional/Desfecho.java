package com.irmaosmiau.posicional;

/**
 * Para onde a dupla vai num resultado de transicao, e quem fica por cima e
 * com o controle (Entrega 5E). As regras sao relativas a quem INICIOU a
 * transicao, entao a mesma definicao serve para os dois lados.
 */
public record Desfecho(Posicao destino, RegraLado porCima, RegraLado controlador) {

    /** Quem fica com um papel depois da transicao. */
    public enum RegraLado {
        /** Quem iniciou a transicao. */
        INICIADOR,
        /** O outro lutador. */
        OUTRO,
        /** Quem ja tinha o papel antes. */
        MANTEM,
        /** Ninguem (ex.: separados em pe). */
        NENHUM
    }

    /** Volta/permanece na origem sem mudar papeis. */
    public static Desfecho permanece(Posicao origem) {
        return new Desfecho(origem, origem.isNoChao() ? RegraLado.MANTEM : RegraLado.NENHUM,
                origem == Posicao.EM_PE_SEPARADOS ? RegraLado.NENHUM : RegraLado.MANTEM);
    }

    EstadoPosicional aplicar(EstadoPosicional antes, Lado iniciador) {
        Lado cima = resolver(porCima, iniciador, antes.getPorCima().orElse(null));
        Lado controle = resolver(controlador, iniciador, antes.getControlador().orElse(null));
        if (destino == Posicao.EM_PE_SEPARADOS) {
            return EstadoPosicional.separados();
        }
        if (destino == Posicao.EM_PE_PEGADA) {
            return EstadoPosicional.pegada(controle);
        }
        return EstadoPosicional.noChao(destino, cima, controle);
    }

    private static Lado resolver(RegraLado regra, Lado iniciador, Lado anterior) {
        return switch (regra) {
            case INICIADOR -> iniciador;
            case OUTRO -> iniciador.outro();
            case MANTEM -> anterior;
            case NENHUM -> null;
        };
    }
}
