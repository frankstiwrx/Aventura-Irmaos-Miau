package com.irmaosmiau.entities;

/**
 * Entrega 5K: custos de folego do CORPO num lugar so (facil de ajustar).
 *
 * Os custos de combate ja existentes (pegada 3, reforco 3, desequilibrio e
 * rompimento) continuam onde estavam (tecnicas.json e SistemaCombate); aqui
 * entram apenas os NOVOS: caminhada, transicoes de postura e o esforco do
 * arrasto. Escala: a do folego atual (Branco e Preto tem maximos diferentes,
 * definidos pelos atributos).
 *
 * Referencia de balanceamento: caminhar em pe por 10 s logicos, na
 * velocidade normal do proprio lutador, gasta ~1 ponto.
 */
public final class CustosFolego {

    /** Ticks logicos por segundo (GamePanel: 16 ms por tick). */
    public static final double TICKS_POR_SEGUNDO = 1000.0 / 16.0;

    /** Caminhar em pe na velocidade normal: pontos por segundo. */
    public static final double CAMINHADA_POR_SEGUNDO = 0.10;

    /*
     * A caminhada e cobrada pela DISTANCIA (velocidade normal = referencia).
     * De joelhos anda a 18% da velocidade; o fator por distancia faz o custo
     * POR TEMPO ficar em ~45% do de caminhar em pe (esforco real, sem punir
     * a postura mais usada do jogo). Deitado (7% da velocidade): ~35%.
     */
    public static final double FATOR_DISTANCIA_EM_PE = 1.0;
    public static final double FATOR_DISTANCIA_AJOELHADO = 2.5;
    public static final double FATOR_DISTANCIA_DEITADO = 5.0;

    /**
     * Arrasto (so o APLICADOR paga): por pixel que a dupla realmente andou.
     * Inclui o "andar" do aplicador (ele nao usa a caminhada normal) e o
     * esforco extra de levar o outro: a 1 px/tick ~0,16 por segundo, ~1,6x
     * caminhar em pe.
     */
    public static final double ARRASTO_POR_PIXEL = 0.0025;

    // ---------------- Entrega 5L: defesa
    /** Receptor preso que forca para sair (por tick): ~0,25/s. */
    public static final double RESISTENCIA_POR_TICK = 0.004;
    /** Abaixar (agachar / de joelhos) a tempo e evitar a pegada: custo unico. */
    public static final double EVASAO_ABAIXAR = 1.5;
    /** Quem tentou a pegada evitada paga esta fracao do custo da tecnica (gastou a tentativa). */
    public static final double FRACAO_CUSTO_PEGADA_EVITADA = 0.5;

    // ---------------- Transicoes de postura (custo unico por transicao)
    public static final double AJOELHAR = 0.5;            // em pe -> ajoelhado
    public static final double DEITAR = 0.5;              // ajoelhado -> deitado
    public static final double ERGUER_JOELHOS = 0.8;      // deitado -> ajoelhado
    public static final double LEVANTAR = 1.2;            // ajoelhado -> em pe
    /** Rapidas (duplo clique): sempre acima da soma das etapas lentas. */
    public static final double RAPIDA_DEITAR = 2.5;       // em pe -> deitado      (lento: 0,5 + 0,5)
    public static final double RAPIDA_LEVANTAR_DO_CHAO = 4.0; // deitado -> em pe  (lento: 0,8 + 1,2)
    public static final double RAPIDA_LEVANTAR = 2.0;     // ajoelhado -> em pe    (lento: 1,2)

    private CustosFolego() {
    }

    /** Custo de caminhada por distancia percorrida (px), pela postura. */
    public static double caminhada(double distanciaPx, double velocidadeNormalPxPorTick,
            IrmaoMiau.EstadoCorporal postura) {
        if (distanciaPx <= 0 || velocidadeNormalPxPorTick <= 0) {
            return 0.0;
        }
        double fator = switch (postura) {
            case AJOELHADO -> FATOR_DISTANCIA_AJOELHADO;
            case DEITADO -> FATOR_DISTANCIA_DEITADO;
            default -> FATOR_DISTANCIA_EM_PE;
        };
        double porTickNaVelocidadeNormal = CAMINHADA_POR_SEGUNDO / TICKS_POR_SEGUNDO;
        return porTickNaVelocidadeNormal * (distanciaPx / velocidadeNormalPxPorTick) * fator;
    }
}
