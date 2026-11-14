package com.irmaosmiau.dispositivos;

/** Dispositivo escolhido para um slot (Entrega 5D). */
public enum TipoDispositivo {
    /** As teclas configuradas do slot (padrao, como antes da 5D). */
    TECLADO("Teclado"),
    /**
     * Um gamepad. Na partida, o Jogador 1 recebe o primeiro gamepad
     * conectado e o Jogador 2 o proximo livre; sem gamepad livre, o slot
     * usa o teclado ate um ser conectado.
     */
    GAMEPAD("Gamepad");

    private final String rotulo;

    TipoDispositivo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
