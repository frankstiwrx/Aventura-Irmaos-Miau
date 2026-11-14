package com.irmaosmiau.controles;

/** Uma das quatro direcoes de tecnica (Entrega 5F): no gamepad, o D-pad. */
public enum DirecaoTecnica {
    CIMA("↑"),
    DIREITA("→"),
    BAIXO("↓"),
    ESQUERDA("←");

    private final String seta;

    DirecaoTecnica(String seta) {
        this.seta = seta;
    }

    public String getSeta() {
        return seta;
    }
}
