package com.irmaosmiau.combate;

/**
 * Estado compartilhado entre dois lutadores.
 *
 * Diferente do EstadoCorporal de cada personagem, este estado pertence
 * a RELACAO entre os dois lutadores. Assim uma pegada nao fica "guardada"
 * dentro do Branco ou do Preto individualmente.
 */
public enum EstadoInteracaoCombate {

    LIVRE,
    PEGADA_1,
    PEGADA_2,
    PEGADA_3,
    PEGADA_4
}
