package com.irmaosmiau.posicional;

/**
 * Papel DINAMICO de um lutador na posicao atual (Entrega 5E). Muda a cada
 * transicao: quem estava por baixo pode passar a controlar depois de uma
 * raspagem. Nao e "atacante" nem "defensor" permanente.
 */
public enum PapelPosicional {
    /** Tem o controle posicional (em pe: quem estabeleceu a pegada). */
    CONTROLADOR,
    /** E controlado. */
    CONTROLADO,
    /** No chao, por cima. */
    POR_CIMA,
    /** No chao, por baixo. */
    POR_BAIXO,
    /** Qualquer um dos dois (sem requisito de papel). */
    QUALQUER
}
