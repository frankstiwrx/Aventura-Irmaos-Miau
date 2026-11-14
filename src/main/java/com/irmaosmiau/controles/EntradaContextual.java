package com.irmaosmiau.controles;

import java.util.EnumSet;
import java.util.Set;

/**
 * O que o jogador segura AGORA para os comandos contextuais (Entrega 5F),
 * independente do aparelho: direcoes de tecnica e os dois modificadores.
 * O gamepad (esquema CONTEXTUAL) produz isto; o teclado podera produzir
 * no futuro com 4 teclas de direcao + 2 modificadores (nunca 12 teclas).
 */
public record EntradaContextual(Set<DirecaoTecnica> direcoes, boolean modificador2, boolean modificador3) {

    public static final EntradaContextual NADA = new EntradaContextual(Set.of(), false, false);

    public EntradaContextual {
        direcoes = direcoes == null || direcoes.isEmpty()
                ? Set.of() : java.util.Collections.unmodifiableSet(EnumSet.copyOf(direcoes));
    }

    public static EntradaContextual de(boolean mod2, boolean mod3, DirecaoTecnica... dirs) {
        Set<DirecaoTecnica> s = EnumSet.noneOf(DirecaoTecnica.class);
        java.util.Collections.addAll(s, dirs);
        return new EntradaContextual(s, mod2, mod3);
    }

    public CamadaComando camada() {
        return CamadaComando.pelos(modificador2, modificador3);
    }
}
