package com.irmaosmiau.conteudo.miaustudio;

import com.irmaosmiau.combate.AnimacaoConjuntaQuadros;

import java.util.Arrays;

/**
 * Resultado da importacao de uma animacao CONJUNTA de um projeto MiauStudio.
 *
 * Os quadros ja estao no formato runtime de 30 inteiros, identico ao gerado
 * pela exportacao Java do editor ("offsetCentroX, offsetLinha, escalaX10,
 * viradoDireita(1/0), cinturaX, ..."). cenaX/linha sao RELATIVOS ao centro
 * de interacao; o centro usado na autoria e guardado so como metadado.
 */
public final class AnimacaoConjuntaMiauStudio {

    private final String nomeAnimacao;
    private final String nomeAplicador;
    private final String nomeReceptor;
    private final int centroAutoriaX;
    private final int linhaAutoria;
    private final int offsetVisualX;
    private final int offsetVisualY;
    private final int[][] aplicador;
    private final int[][] receptor;

    AnimacaoConjuntaMiauStudio(
            String nomeAnimacao,
            String nomeAplicador,
            String nomeReceptor,
            int centroAutoriaX,
            int linhaAutoria,
            int offsetVisualX,
            int offsetVisualY,
            int[][] aplicador,
            int[][] receptor
    ) {
        this.nomeAnimacao = nomeAnimacao;
        this.nomeAplicador = nomeAplicador;
        this.nomeReceptor = nomeReceptor;
        this.centroAutoriaX = centroAutoriaX;
        this.linhaAutoria = linhaAutoria;
        this.offsetVisualX = offsetVisualX;
        this.offsetVisualY = offsetVisualY;
        this.aplicador = copiar(aplicador);
        this.receptor = copiar(receptor);
    }

    private static int[][] copiar(int[][] origem) {
        int[][] c = new int[origem.length][];
        for (int i = 0; i < origem.length; i++) {
            c[i] = Arrays.copyOf(origem[i], origem[i].length);
        }
        return c;
    }

    public String getNomeAnimacao() {
        return nomeAnimacao;
    }

    /** "Branco" ou "Preto": personagem que APLICA a tecnica na autoria. */
    public String getNomeAplicador() {
        return nomeAplicador;
    }

    public String getNomeReceptor() {
        return nomeReceptor;
    }

    /** Centro X usado SOMENTE durante a autoria no MiauStudio. */
    public int getCentroAutoriaX() {
        return centroAutoriaX;
    }

    public int getLinhaAutoria() {
        return linhaAutoria;
    }

    public int getOffsetVisualX() {
        return offsetVisualX;
    }

    public int getOffsetVisualY() {
        return offsetVisualY;
    }

    public int quantidadeQuadros() {
        return aplicador.length;
    }

    public int[][] copiarQuadrosAplicador() {
        return copiar(aplicador);
    }

    public int[][] copiarQuadrosReceptor() {
        return copiar(receptor);
    }

    /** Quadros runtime com tempos iguais (comportamento atual da PEGADA_1). */
    public AnimacaoConjuntaQuadros paraQuadros() {
        return new AnimacaoConjuntaQuadros(aplicador, receptor, offsetVisualX, offsetVisualY);
    }

    /**
     * Quadros runtime com duracao total e/ou distribuicao temporal.
     *
     * @param duracaoTotalFrames  0 = nao definida
     * @param duracoesQuadros     null = quadros com tempos iguais
     */
    public AnimacaoConjuntaQuadros paraQuadros(int duracaoTotalFrames, int[] duracoesQuadros) {
        return new AnimacaoConjuntaQuadros(aplicador, receptor, offsetVisualX, offsetVisualY,
                duracaoTotalFrames, duracoesQuadros);
    }
}
