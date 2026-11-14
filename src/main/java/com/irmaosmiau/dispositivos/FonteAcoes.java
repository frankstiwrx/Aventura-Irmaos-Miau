package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.AcaoJogador;

/**
 * Uma origem de acoes logicas para UM slot (Entrega 5D), alem do teclado.
 * O InputHandler combina o teclado do slot com a fonte ligada a ele.
 */
public interface FonteAcoes {

    /** Acao segurada agora e nao travada. */
    boolean isAtiva(AcaoJogador acao);

    /** Bloqueio de combate (3A): o que esta segurado so volta a valer depois de solto. */
    void travarPressionadas();

    /** Perda de foco: nada continua valendo ate ser solto e apertado de novo. */
    void liberarTodas();
}
