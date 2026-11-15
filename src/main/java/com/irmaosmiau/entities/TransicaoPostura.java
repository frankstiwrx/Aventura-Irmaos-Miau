package com.irmaosmiau.entities;

import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;

/**
 * Entrega 5K: transicoes de postura pelo comando F/L3 (DEITAR_LEVANTAR).
 *
 * Ciclo de cliques simples: EM PE -> AJOELHADO -> DEITADO -> AJOELHADO -> EM PE.
 * Duplo clique: transicoes rapidas (mais caras que o caminho lento).
 *
 * O "contrato" e o identificador estavel da animacao (hoje procedural;
 * substituivel por JSON do MiauStudio sem mexer na regra).
 */
public enum TransicaoPostura {

    AJOELHAR(EstadoCorporal.EM_PE, EstadoCorporal.AJOELHADO, false, CustosFolego.AJOELHAR, 12,
            "TRANSICAO_AJOELHAR"),
    DEITAR(EstadoCorporal.AJOELHADO, EstadoCorporal.DEITADO, false, CustosFolego.DEITAR, 14,
            "TRANSICAO_DEITAR"),
    ERGUER_JOELHOS(EstadoCorporal.DEITADO, EstadoCorporal.AJOELHADO, false, CustosFolego.ERGUER_JOELHOS, 14,
            "TRANSICAO_ERGUER_JOELHOS"),
    LEVANTAR(EstadoCorporal.AJOELHADO, EstadoCorporal.EM_PE, false, CustosFolego.LEVANTAR, 12,
            "TRANSICAO_LEVANTAR"),
    RAPIDA_DEITAR(EstadoCorporal.EM_PE, EstadoCorporal.DEITADO, true, CustosFolego.RAPIDA_DEITAR, 10,
            "TRANSICAO_RAPIDA_DEITAR"),
    RAPIDA_LEVANTAR_DO_CHAO(EstadoCorporal.DEITADO, EstadoCorporal.EM_PE, true,
            CustosFolego.RAPIDA_LEVANTAR_DO_CHAO, 16, "TRANSICAO_RAPIDA_LEVANTAR_DO_CHAO"),
    RAPIDA_LEVANTAR(EstadoCorporal.AJOELHADO, EstadoCorporal.EM_PE, true, CustosFolego.RAPIDA_LEVANTAR, 8,
            "TRANSICAO_RAPIDA_LEVANTAR");

    private final EstadoCorporal origem;
    private final EstadoCorporal destino;
    private final boolean rapida;
    private final double custo;
    private final int duracaoTicks;
    private final String contrato;

    TransicaoPostura(EstadoCorporal origem, EstadoCorporal destino, boolean rapida, double custo,
            int duracaoTicks, String contrato) {
        this.origem = origem;
        this.destino = destino;
        this.rapida = rapida;
        this.custo = custo;
        this.duracaoTicks = duracaoTicks;
        this.contrato = contrato;
    }

    public EstadoCorporal getOrigem() {
        return origem;
    }

    public EstadoCorporal getDestino() {
        return destino;
    }

    public boolean isRapida() {
        return rapida;
    }

    public double getCusto() {
        return custo;
    }

    /** Duracao da animacao (ticks logicos). A postura logica muda no inicio. */
    public int getDuracaoTicks() {
        return duracaoTicks;
    }

    public String getContrato() {
        return contrato;
    }

    /** Resultado de pedir uma transicao. */
    public enum Resultado {
        FEITA,
        /** Sem folego: nada muda (nem parte da transicao). */
        SEM_FOLEGO,
        /** Pulando, rolando, sprawl ou em animacao de combate. */
        BLOQUEADA
    }
}
