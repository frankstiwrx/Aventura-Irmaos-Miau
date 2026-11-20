package com.irmaosmiau.cenario;

import java.awt.Graphics2D;

/**
 * Entrega 5Q.5: nuvens que andam devagar (camada dinamica dos cenarios externos).
 *
 * Cada nuvem anda alguns pixels por segundo e volta pelo outro lado da tela. A posicao
 * depende so do tick do jogo: o mesmo tick desenha sempre a mesma imagem (sem timer
 * proprio, sem aleatoriedade). O ceu fica num cache, as nuvens por cima dele e o resto do
 * fundo (predios, montanhas, muros) num cache transparente por cima das nuvens.
 */
final class NuvensAnimadas {

    /** Desenho de uma nuvem do cenario (o formato de cada um). */
    @FunctionalInterface
    interface Pintor {
        void nuvem(Graphics2D g, int cx, int cy, double escala);
    }

    /** Nuvem: x inicial, altura, escala e velocidade (px por tick; ~0,04 = 2,5 px/s). */
    record Nuvem(double x, int y, double escala, double velocidade) {
    }

    /** Margem para a nuvem sair inteira antes de reaparecer do outro lado. */
    static final int MARGEM = 90;

    private final Nuvem[] nuvens;
    private final Pintor pintor;

    NuvensAnimadas(Pintor pintor, Nuvem... nuvens) {
        this.pintor = pintor;
        this.nuvens = nuvens.clone();
    }

    /** x da nuvem no tick (volta pelo outro lado). */
    static double x(Nuvem n, long tick, int largura) {
        double faixa = largura + 2.0 * MARGEM;
        double v = (n.x() + MARGEM + n.velocidade() * tick) % faixa;
        if (v < 0) {
            v += faixa;
        }
        return v - MARGEM;
    }

    void desenhar(Graphics2D g, long tick, int largura) {
        for (Nuvem n : nuvens) {
            pintor.nuvem(g, (int) Math.round(x(n, tick, largura)), n.y(), n.escala());
        }
    }
}
