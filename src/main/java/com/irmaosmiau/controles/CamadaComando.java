package com.irmaosmiau.controles;

/**
 * Camadas dos comandos contextuais.
 *
 * Entrega 5H (gamepad CONTEXTUAL: mod. 2 = LT, mod. 3 = LB):
 * - UM: direcao sozinha (analogico direito ou D-pad);
 * - DOIS: LT + direcao;
 * - TRES: LB + direcao;
 * - QUATRO: LT + LB + direcao (camada avancada, reservada: sem tecnicas ainda).
 * Prioridade: os dois modificadores > um modificador > nenhum.
 * (Na 5F a camada 3 exigia LT + LB; mudou a pedido para LB sozinho.)
 */
public enum CamadaComando {
    UM(1),
    DOIS(2),
    TRES(3),
    QUATRO(4);

    private final int numero;

    CamadaComando(int numero) {
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }

    /** Camada pelos modificadores segurados. */
    public static CamadaComando pelos(boolean modificador2, boolean modificador3) {
        if (modificador2 && modificador3) {
            return QUATRO;
        }
        if (modificador3) {
            return TRES;
        }
        return modificador2 ? DOIS : UM;
    }
}
