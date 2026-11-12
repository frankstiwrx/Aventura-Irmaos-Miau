package com.irmaosmiau.combate;

import java.util.Arrays;

/**
 * Quadros sincronizados de uma tecnica exportada pelo MiauStudio.
 * Ambos os tracks compartilham um unico indice de quadro.
 * Indices 0/1 sao offsets relativos ao centro de autoria; o motor
 * utiliza a VARIACAO em relacao ao primeiro quadro, sem teletransporte.
 */
public final class AnimacaoConjuntaQuadros {

    private static final int TAMANHO_POSE = 30;
    private final int[][] aplicador;
    private final int[][] receptor;
    private final double offsetVisualX;
    private final double offsetVisualY;

    public AnimacaoConjuntaQuadros(int[][] aplicador, int[][] receptor,
            double offsetVisualX, double offsetVisualY) {
        this.aplicador = copiarEValidar(aplicador);
        this.receptor = copiarEValidar(receptor);
        if (this.aplicador.length != this.receptor.length) {
            throw new IllegalArgumentException("Tracks conjuntos devem ter a mesma quantidade de quadros.");
        }
        this.offsetVisualX = offsetVisualX;
        this.offsetVisualY = offsetVisualY;
    }

    private static int[][] copiarEValidar(int[][] quadros) {
        if (quadros == null || quadros.length == 0) {
            throw new IllegalArgumentException("A animacao precisa de quadros.");
        }
        int[][] copia = new int[quadros.length][];
        for (int i = 0; i < quadros.length; i++) {
            if (quadros[i] == null || quadros[i].length != TAMANHO_POSE) {
                throw new IllegalArgumentException("Cada quadro precisa ter 30 valores.");
            }
            copia[i] = Arrays.copyOf(quadros[i], TAMANHO_POSE);
        }
        return copia;
    }

    public int quantidadeQuadros() {
        return aplicador.length;
    }

    /** Um terco da timeline para cada um dos tres quadros da PEGADA_1. */
    public int indiceParaProgresso(double progresso) {
        double valor = Math.max(0.0, Math.min(1.0, progresso));
        return Math.min(quantidadeQuadros() - 1,
                (int) Math.floor(valor * quantidadeQuadros()));
    }

    public int[] copiarQuadroAplicador(int indice) {
        return Arrays.copyOf(aplicador[indice], TAMANHO_POSE);
    }

    public int[] copiarQuadroReceptor(int indice) {
        return Arrays.copyOf(receptor[indice], TAMANHO_POSE);
    }

    /** Deslocamento relativo: quadro 1 = 0 (sem salto na entrada). */
    public double deslocamentoAplicadorX(int indice) {
        return offsetVisualX + aplicador[indice][0] - aplicador[0][0];
    }

    public double deslocamentoReceptorX(int indice) {
        return offsetVisualX + receptor[indice][0] - receptor[0][0];
    }

    public double getOffsetVisualY() {
        return offsetVisualY;
    }
}
