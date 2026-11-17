package com.irmaosmiau.entities;

import java.awt.Color;

/**
 * Entrega 5Q.1: cor do kimono (casaco e calca juntos; nao existe calca separada).
 * Branco e preto sao exatamente as cores originais do jogo. A cor do kimono nunca
 * muda pelo, rosto, especie nem atributos.
 */
public enum CorKimono {
    BRANCO("Branco", Color.WHITE),
    AZUL("Azul", new Color(36, 78, 176)),
    PRETO("Preto", Color.BLACK);

    private final String nome;
    private final Color cor;

    CorKimono(String nome, Color cor) {
        this.nome = nome;
        this.cor = cor;
    }

    public String getNome() {
        return nome;
    }

    public Color getCor() {
        return cor;
    }

    /** A cor do enum para uma cor de desenho (null se nao for uma das tres). */
    public static CorKimono de(Color c) {
        for (CorKimono k : values()) {
            if (k.cor.equals(c)) {
                return k;
            }
        }
        return null;
    }
}
