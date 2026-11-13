package com.irmaosmiau.controles;

/**
 * Como uma acao logica e lida pelo jogo (Entrega 5C).
 *
 * O InputHandler so informa "pressionada/solta"; quem dispara uma unica vez
 * por toque e o GamePanel (borda de subida), exatamente como antes.
 */
public enum TipoAcionamento {
    /** Vale enquanto a tecla estiver segurada (andar, correr, agachar...). */
    CONTINUA,
    /** Dispara uma vez por pressionamento (pular, rolar, pegada, T, U, Y...). */
    TOQUE,
    /** Segurada junto com outra acao muda o significado dela (arrasto conjunto). */
    MODIFICADOR
}
