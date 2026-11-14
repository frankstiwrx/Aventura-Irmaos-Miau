package com.irmaosmiau.posicional;

import java.util.Objects;
import java.util.Optional;

/**
 * Estado posicional da dupla (Entrega 5E). Imutavel.
 *
 * - posicao: onde a dupla esta (em pe, guarda, cem quilos...);
 * - porCima: no chao, quem esta por cima (o outro esta por baixo);
 * - controlador: quem tem o controle posicional (pode ser o de baixo na guarda);
 * - transicao: tentativa em andamento (origem/destino ficam na definicao).
 *
 * Responsabilidades: o SistemaCombate continua dono do corpo, da interacao
 * (PEGADA_1, firmeza, timeline de 30 quadros) e das disputas 5A. Este modelo
 * guarda SO o que o motor atual nao tem: posicao da dupla, cima/baixo,
 * controle posicional e a transicao entre posicoes. Em pe, ele e LIDO do
 * SistemaCombate (LeituraPosicionalEmPe), sem estado duplicado.
 */
public final class EstadoPosicional {

    private final Posicao posicao;
    private final Lado porCima;
    private final Lado controlador;
    private final TransicaoEmCurso transicao;

    private EstadoPosicional(Posicao posicao, Lado porCima, Lado controlador, TransicaoEmCurso transicao) {
        if (posicao == null) {
            throw new IllegalArgumentException("A posicao e obrigatoria.");
        }
        if (posicao.isNoChao() && porCima == null) {
            throw new IllegalArgumentException("No chao alguem precisa estar por cima: " + posicao);
        }
        if (!posicao.isNoChao() && porCima != null) {
            throw new IllegalArgumentException("Em pe ninguem esta por cima: " + posicao);
        }
        if (posicao == Posicao.EM_PE_PEGADA && controlador == null) {
            throw new IllegalArgumentException("Na disputa de pegada alguem controla.");
        }
        if (posicao == Posicao.EM_PE_SEPARADOS && controlador != null) {
            throw new IllegalArgumentException("Separados, ninguem controla.");
        }
        this.posicao = posicao;
        this.porCima = porCima;
        this.controlador = controlador;
        this.transicao = transicao;
    }

    public static EstadoPosicional separados() {
        return new EstadoPosicional(Posicao.EM_PE_SEPARADOS, null, null, null);
    }

    public static EstadoPosicional pegada(Lado controlador) {
        return new EstadoPosicional(Posicao.EM_PE_PEGADA, null, controlador, null);
    }

    public static EstadoPosicional noChao(Posicao posicao, Lado porCima, Lado controlador) {
        return new EstadoPosicional(posicao, porCima, controlador, null);
    }

    EstadoPosicional comTransicao(TransicaoEmCurso t) {
        return new EstadoPosicional(posicao, porCima, controlador, t);
    }

    public Posicao getPosicao() {
        return posicao;
    }

    public Optional<Lado> getPorCima() {
        return Optional.ofNullable(porCima);
    }

    public Optional<Lado> getPorBaixo() {
        return porCima == null ? Optional.empty() : Optional.of(porCima.outro());
    }

    public Optional<Lado> getControlador() {
        return Optional.ofNullable(controlador);
    }

    public Optional<TransicaoEmCurso> getTransicao() {
        return Optional.ofNullable(transicao);
    }

    /** O lado cumpre o papel pedido na posicao atual? */
    public boolean temPapel(Lado lado, PapelPosicional papel) {
        return switch (papel) {
            case QUALQUER -> true;
            case CONTROLADOR -> lado == controlador;
            case CONTROLADO -> controlador != null && lado != controlador;
            case POR_CIMA -> lado == porCima;
            case POR_BAIXO -> porCima != null && lado != porCima;
        };
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EstadoPosicional e && posicao == e.posicao && porCima == e.porCima
                && controlador == e.controlador && Objects.equals(transicao, e.transicao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(posicao, porCima, controlador, transicao);
    }

    @Override
    public String toString() {
        return posicao + (porCima == null ? "" : " cima=" + porCima)
                + (controlador == null ? "" : " controle=" + controlador)
                + (transicao == null ? "" : " [" + transicao + "]");
    }
}
