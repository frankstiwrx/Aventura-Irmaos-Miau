package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.AcaoJogador;

import java.util.EnumSet;
import java.util.Set;

/**
 * Analogico -> direcoes digitais (Entrega 5D), sem mudar a velocidade do
 * movimento: o analogico apenas "segura" MOVER_ESQUERDA/DIREITA/CIMA/BAIXO
 * como as teclas, e o resto (velocidade, cinco linhas) continua igual.
 *
 * 1. Zona morta RADIAL: |v| < ZONA_MORTA conta como centro (ruido de repouso).
 * 2. Normalizacao: o que sobra e reescalado para 0..1 (borda da zona morta = 0).
 * 3. Histerese por eixo: liga em LIMIAR_ATIVAR e so desliga abaixo de
 *    LIMIAR_SOLTAR, para o ruido perto do limiar nao ligar/desligar a cada tick.
 */
public final class LeitorAnalogico {

    public static final double ZONA_MORTA = 0.25;
    public static final double LIMIAR_ATIVAR = 0.50;
    public static final double LIMIAR_SOLTAR = 0.35;

    private LeitorAnalogico() {
    }

    /** Vetor depois da zona morta radial, com modulo em 0..1. */
    public static double[] normalizar(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return new double[]{0, 0};
        }
        double modulo = Math.hypot(x, y);
        if (modulo < ZONA_MORTA) {
            return new double[]{0, 0};
        }
        double util = Math.min(1.0, (modulo - ZONA_MORTA) / (1.0 - ZONA_MORTA));
        double escala = util / modulo;
        return new double[]{x * escala, y * escala};
    }

    /**
     * Direcoes seguradas pelo analogico (Y negativo = cima).
     *
     * @param anteriores direcoes ativas no tick anterior (histerese)
     */
    public static Set<AcaoJogador> direcoes(double x, double y, Set<AcaoJogador> anteriores) {
        double[] n = normalizar(x, y);
        Set<AcaoJogador> ativas = EnumSet.noneOf(AcaoJogador.class);
        ligar(ativas, anteriores, AcaoJogador.MOVER_DIREITA, n[0]);
        ligar(ativas, anteriores, AcaoJogador.MOVER_ESQUERDA, -n[0]);
        ligar(ativas, anteriores, AcaoJogador.MOVER_BAIXO, n[1]);
        ligar(ativas, anteriores, AcaoJogador.MOVER_CIMA, -n[1]);
        return ativas;
    }

    private static void ligar(Set<AcaoJogador> ativas, Set<AcaoJogador> anteriores, AcaoJogador acao, double valor) {
        double limiar = anteriores.contains(acao) ? LIMIAR_SOLTAR : LIMIAR_ATIVAR;
        if (valor >= limiar) {
            ativas.add(acao);
        }
    }
}
