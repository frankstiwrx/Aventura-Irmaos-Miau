package com.irmaosmiau.posicional;

/**
 * Ritmo de uma transicao (Entrega 5E). Tudo em tempo real: nada pausa a
 * luta; o que muda e quanto tempo (em ticks do loop logico, 60/s) a tentativa
 * leva e quando o outro pode reagir.
 */
public enum Cadencia {
    /** Acontece em poucos ticks (ex.: reforcar a pegada). */
    RAPIDA,
    /** Tem preparacao visivel: janela para o outro defender. */
    PREPARADA,
    /** Disputa que dura: os dois agem ao mesmo tempo ate resolver. */
    DISPUTA
}
