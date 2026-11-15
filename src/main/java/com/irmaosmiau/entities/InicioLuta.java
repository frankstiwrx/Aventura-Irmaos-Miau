package com.irmaosmiau.entities;

/**
 * Entrega 5J: como a luta comeca.
 *
 * - DE_JOELHOS: os dois ajoelhados, frente a frente, na mesma linha, a uma
 *   distancia em que qualquer um ja pode disputar a pegada (dentro do alcance
 *   da PEGADA_1), sem sobreposicao, sem pegada estabelecida e simetricos em
 *   relacao ao centro da arena (nenhum comeca mais perto).
 * - EM_PE: o inicio de sempre (posicoes e postura de ate a 5I).
 *
 * Nas telas, o padrao para lutas de jiu-jitsu e DE_JOELHOS; no codigo, quem
 * nao informa nada continua EM_PE (comportamento ate a 5I).
 */
public enum InicioLuta {

    DE_JOELHOS("De joelhos"),
    EM_PE("Em pé");

    /** Padrao das telas de escolha (lutas de jiu-jitsu). */
    public static final InicioLuta PADRAO_TELAS = DE_JOELHOS;

    /**
     * Distancia entre os x dos dois ajoelhados. Na escala 0,7 vale 120 na
     * medida das regras, dentro do alcance de 125 da PEGADA_1; e a mesma
     * distancia do primeiro quadro desenhado no MiauStudio (56 -> 140).
     */
    public static final int DISTANCIA_DE_JOELHOS = 84;

    /** Centro da arena (largura 800) para o meio da dupla. */
    public static final int CENTRO_ARENA = 400;

    private final String rotulo;

    InicioLuta(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }

    /** x de quem comeca a esquerda (o meio do desenho, x + 35 * escala, fica simetrico). */
    public static int xEsquerdaDeJoelhos(double escala) {
        return (int) Math.round(CENTRO_ARENA - 35 * escala - DISTANCIA_DE_JOELHOS / 2.0);
    }

    /** x de quem comeca a direita. */
    public static int xDireitaDeJoelhos(double escala) {
        return xEsquerdaDeJoelhos(escala) + DISTANCIA_DE_JOELHOS;
    }

    /** Coloca a postura inicial nos lutadores (EM_PE nao muda nada). */
    public void aplicarPostura(IrmaoMiau... lutadores) {
        if (this != DE_JOELHOS) {
            return;
        }
        for (IrmaoMiau l : lutadores) {
            l.ajoelhar();
        }
    }
}
