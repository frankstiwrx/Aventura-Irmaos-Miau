package com.irmaosmiau.posicional;

/** Como uma tentativa de transicao termina (Entrega 5E). */
public enum ResultadoTransicao {
    /** Quem tentou conseguiu. */
    SUCESSO,
    /** O outro defendeu: a posicao de origem se mantem. */
    DEFENDIDA,
    /** Nao funcionou por conta propria (sem merito do outro). */
    FALHA,
    /** O outro aproveitou e virou a situacao (papeis invertidos). */
    INVERSAO
}
