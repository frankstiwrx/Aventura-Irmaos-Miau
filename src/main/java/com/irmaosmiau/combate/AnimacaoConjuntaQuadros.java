package com.irmaosmiau.combate;

import java.util.Arrays;

/**
 * Quadros sincronizados de uma tecnica exportada pelo MiauStudio.
 * Ambos os tracks compartilham um unico indice de quadro.
 * Indices 0/1 sao offsets relativos ao centro de autoria; o motor
 * utiliza a VARIACAO em relacao ao primeiro quadro, sem teletransporte.
 *
 * Tempo (Entrega 2):
 * - sem distribuicao temporal: cada quadro ocupa a mesma fracao da
 *   timeline (comportamento original da PEGADA_1: 10/10/10 em 30 frames);
 * - com distribuicao ("duracoesQuadros"): cada quadro ocupa a sua fracao
 *   proporcional. Nao existe interpolacao: o quadro e trocado inteiro.
 * - duracaoTotalFrames e opcional (0 = definida pela tecnica).
 */
public final class AnimacaoConjuntaQuadros {

    private static final int TAMANHO_POSE = 30;
    private final int[][] aplicador;
    private final int[][] receptor;
    private final double offsetVisualX;
    private final double offsetVisualY;

    /* 0 = nao definida pela animacao (a tecnica define). */
    private final int duracaoTotalFrames;

    /* null = quadros com tempos iguais. */
    private final int[] duracoesQuadros;
    private final int somaDuracoes;

    public AnimacaoConjuntaQuadros(int[][] aplicador, int[][] receptor,
            double offsetVisualX, double offsetVisualY) {
        this(aplicador, receptor, offsetVisualX, offsetVisualY, 0, null);
    }

    /**
     * @param duracaoTotalFrames 0 = nao definida; caso contrario &gt; 0
     * @param duracoesQuadros    null = tempos iguais; caso contrario um
     *                           valor &gt; 0 por quadro. Se a duracao total
     *                           tambem for informada, a soma deve ser igual.
     */
    public AnimacaoConjuntaQuadros(int[][] aplicador, int[][] receptor,
            double offsetVisualX, double offsetVisualY,
            int duracaoTotalFrames, int[] duracoesQuadros) {
        this.aplicador = copiarEValidar(aplicador);
        this.receptor = copiarEValidar(receptor);
        if (this.aplicador.length != this.receptor.length) {
            throw new IllegalArgumentException("Tracks conjuntos devem ter a mesma quantidade de quadros.");
        }
        this.offsetVisualX = offsetVisualX;
        this.offsetVisualY = offsetVisualY;

        if (duracaoTotalFrames < 0) {
            throw new IllegalArgumentException("A duracao total nao pode ser negativa.");
        }

        if (duracoesQuadros != null) {
            if (duracoesQuadros.length != this.aplicador.length) {
                throw new IllegalArgumentException(
                        "A distribuicao temporal precisa de " + this.aplicador.length
                        + " valores (um por quadro), recebeu " + duracoesQuadros.length + ".");
            }
            int soma = 0;
            for (int i = 0; i < duracoesQuadros.length; i++) {
                if (duracoesQuadros[i] <= 0) {
                    throw new IllegalArgumentException(
                            "A duracao do quadro " + (i + 1) + " precisa ser maior que zero.");
                }
                soma += duracoesQuadros[i];
            }
            if (duracaoTotalFrames > 0 && soma != duracaoTotalFrames) {
                throw new IllegalArgumentException(
                        "A soma das duracoes dos quadros (" + soma
                        + ") difere da duracao total (" + duracaoTotalFrames + ").");
            }
            this.duracoesQuadros = Arrays.copyOf(duracoesQuadros, duracoesQuadros.length);
            this.somaDuracoes = soma;
            this.duracaoTotalFrames = duracaoTotalFrames > 0 ? duracaoTotalFrames : soma;
        } else {
            this.duracoesQuadros = null;
            this.somaDuracoes = 0;
            this.duracaoTotalFrames = duracaoTotalFrames;
        }
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

        if (duracoesQuadros == null) {
            // Caminho original, inalterado.
            return Math.min(quantidadeQuadros() - 1,
                    (int) Math.floor(valor * quantidadeQuadros()));
        }

        // Pequena tolerancia para que 10/30 * 30 caia exatamente no frame 10.
        double posicao = valor * somaDuracoes + 1e-9;
        int acumulado = 0;
        for (int i = 0; i < duracoesQuadros.length; i++) {
            acumulado += duracoesQuadros[i];
            if (posicao < acumulado) {
                return i;
            }
        }
        return quantidadeQuadros() - 1;
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

    public double getOffsetVisualX() {
        return offsetVisualX;
    }

    public double getOffsetVisualY() {
        return offsetVisualY;
    }

    /** 0 quando a animacao nao define a propria duracao. */
    public int getDuracaoTotalFrames() {
        return duracaoTotalFrames;
    }

    public boolean possuiDistribuicaoTemporal() {
        return duracoesQuadros != null;
    }

    /** Copia da distribuicao temporal, ou null quando os tempos sao iguais. */
    public int[] copiarDuracoesQuadros() {
        return duracoesQuadros == null ? null : Arrays.copyOf(duracoesQuadros, duracoesQuadros.length);
    }
}
