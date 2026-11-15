package com.irmaosmiau.animacao;

/**
 * "Qual animacao" um lutador precisa agora (Entrega 5E). E a chave entre o
 * estado do jogo (corpo + combate) e a animacao: a regra de movimento nunca
 * olha para isto, e a animacao nunca move o lutador.
 */
public enum EstadoVisual {
    PARADO,
    TAUNT,
    CAMINHADA,
    CORRIDA,
    AGACHADO,
    PULO,
    ROLAMENTO,
    SPRAWL,
    DEITADO,
    MOVIMENTO_CHAO,
    /** Entrega 5J: de joelhos, parado (POSTURA_AJOELHADO_IDLE). */
    AJOELHADO,
    /** Entrega 5J: de joelhos, deslocando (POSTURA_AJOELHADO_MOVIMENTO). */
    AJOELHADO_MOVIMENTO,
    /** Entrega 5K: mudando de postura (contratos TRANSICAO_*; procedural por enquanto). */
    TRANSICAO_POSTURA,
    /** Entrega 5L: reacao curta a um evento defensivo do motor (fora da pegada). */
    REACAO_DEFESA,
    /** Conjunta: os 30 quadros de entrada da PEGADA_1. */
    PEGADA_ENTRADA,
    /** Conjunta: pose final persistente da PEGADA_1. */
    PEGADA_ESTABELECIDA,
    /** Conjunta: arrasto da 5B (deslocamento fisico da dupla). */
    ARRASTO_CONJUNTO;

    public boolean isConjunto() {
        return this == PEGADA_ENTRADA || this == PEGADA_ESTABELECIDA || this == ARRASTO_CONJUNTO;
    }
}
