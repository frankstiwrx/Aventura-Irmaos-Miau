package com.irmaosmiau.entities;

/**
 * Entrega 5O: acrobacias (Y + direcao do movimento). Direcoes relativas ao
 * CORPO: frente/tras pela orientacao do lutador; esquerda/direita do corpo
 * sao o eixo da profundidade (a estrela atravessa linhas do tatame).
 *
 * Custos comparados com os existentes (CustosFolego / IrmaoMiau): rolamento
 * 6,0, sprawl 4,0, pegada 3,0, levantar 1,2. Estrela (lateral, mais curta)
 * fica abaixo do rolamento; o mortal (o maior deslocamento e no ar) acima.
 */
public enum TipoAcrobacia {
    ESTRELA_ESQUERDA("ESTRELA (ESQUERDA)", 30, 0, -40, 5.0, true),
    ESTRELA_DIREITA("ESTRELA (DIREITA)", 30, 0, 40, 5.0, true),
    MORTAL_FRENTE("MORTAL PARA A FRENTE", 32, 60, 0, 7.0, false),
    MORTAL_TRAS("MORTAL PARA TRÁS", 34, -55, 0, 7.0, false);

    private final String rotulo;
    private final int duracaoTicks;
    private final int frentePx;
    private final int ladoPx;
    private final double custo;
    private final boolean estrela;

    TipoAcrobacia(String rotulo, int duracaoTicks, int frentePx, int ladoPx, double custo, boolean estrela) {
        this.rotulo = rotulo;
        this.duracaoTicks = duracaoTicks;
        this.frentePx = frentePx;
        this.ladoPx = ladoPx;
        this.custo = custo;
        this.estrela = estrela;
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getDuracaoTicks() {
        return duracaoTicks;
    }

    /** Deslocamento total para a frente do corpo (negativo = para tras). */
    public int getDeslocamentoFrentePx() {
        return frentePx;
    }

    /**
     * Deslocamento total em profundidade. A esquerda do corpo de quem olha
     * para a direita fica no fundo (linha menor); olhando para a esquerda,
     * na frente da tela.
     */
    public int getDeslocamentoProfundidadePx(boolean viradoParaDireita) {
        return viradoParaDireita ? ladoPx : -ladoPx;
    }

    public double getCusto() {
        return custo;
    }

    public boolean isEstrela() {
        return estrela;
    }

    /** No ar (as maos/pes ainda nao voltaram): a parte do meio do movimento. */
    public boolean noAr(double progresso) {
        return progresso >= 0.25 && progresso <= 0.75;
    }

    /**
     * Acrobacia pela direcao do movimento segurada, relativa ao corpo.
     *
     * @param dx -1/0/+1 na tela (esquerda/direita)
     * @param dy -1/0/+1 na profundidade (fundo/frente da tela)
     */
    public static TipoAcrobacia pelaDirecao(int dx, int dy, boolean viradoParaDireita) {
        if (dx != 0) {
            boolean paraFrente = (dx > 0) == viradoParaDireita;
            return paraFrente ? MORTAL_FRENTE : MORTAL_TRAS;
        }
        if (dy != 0) {
            // esquerda do corpo: fundo (dy < 0) para quem olha para a direita
            boolean esquerda = (dy < 0) == viradoParaDireita;
            return esquerda ? ESTRELA_ESQUERDA : ESTRELA_DIREITA;
        }
        return null;
    }
}
