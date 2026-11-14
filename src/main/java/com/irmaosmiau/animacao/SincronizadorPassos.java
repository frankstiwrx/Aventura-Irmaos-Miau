package com.irmaosmiau.animacao;

/**
 * Contrato de ROOT MOTION (Entrega 5E): liga uma animacao de passos ao
 * deslocamento FISICO que ja existe, para os pes nao "deslizarem".
 *
 * - Movimento fisico: quanto o lutador/dupla andou de verdade no cenario
 *   (ex.: arrasto 5B, SistemaCombate.VELOCIDADE_DESLOCAMENTO_CONJUNTO = 1 px/tick).
 * - Movimento visual: so a pose (articulacoes) da animacao.
 *
 * O quadro e escolhido pela DISTANCIA percorrida, nao pelo tempo: parado,
 * a pose nao avanca; mais rapido, os passos aceleram junto. Nada aqui move
 * ninguem. Sem uma animacao de arrasto cadastrada, o fallback continua
 * sendo a pose final da PEGADA_1 transladada (comportamento da 5B).
 *
 * @param pixelsPorCiclo distancia fisica de um ciclo completo de passos
 * @param quadros        quantos quadros tem o ciclo
 */
public record SincronizadorPassos(double pixelsPorCiclo, int quadros) {

    public SincronizadorPassos {
        if (pixelsPorCiclo <= 0 || quadros < 1) {
            throw new IllegalArgumentException("Ciclo de passos invalido.");
        }
    }

    /** Quadro do ciclo para a distancia fisica acumulada (sinal ignorado: andar para tras tambem anima). */
    public int quadroPara(double distanciaAcumuladaPx) {
        double d = Math.abs(distanciaAcumuladaPx) % pixelsPorCiclo;
        int q = (int) Math.floor(d / pixelsPorCiclo * quadros);
        return Math.min(quadros - 1, q);
    }
}
