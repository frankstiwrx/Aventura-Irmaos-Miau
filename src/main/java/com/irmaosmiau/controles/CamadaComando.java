package com.irmaosmiau.controles;

/**
 * Camadas dos comandos contextuais (Entrega 5F). Prioridade na resolucao:
 * TRES (mod. 2 + mod. 3 + direcao) > DOIS (mod. 2 + direcao) > UM (direcao).
 * No gamepad, esquema CONTEXTUAL: mod. 2 = LT, mod. 3 = LB.
 */
public enum CamadaComando {
    UM(1),
    DOIS(2),
    TRES(3);

    private final int numero;

    CamadaComando(int numero) {
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }

    /** Camada pelos modificadores segurados. O mod. 3 sozinho nao muda de camada. */
    public static CamadaComando pelos(boolean modificador2, boolean modificador3) {
        if (modificador2 && modificador3) {
            return TRES;
        }
        return modificador2 ? DOIS : UM;
    }
}
