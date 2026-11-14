package com.irmaosmiau.dispositivos;

/** Esquema de botoes de um slot no gamepad (Entrega 5F). */
public enum EsquemaGamepad {
    /** O da 5D: D-pad move, cada tecnica num botao. Padrao; nada e migrado sozinho. */
    CLASSICO("Clássico (5D)"),
    /** Experimental: D-pad = tecnicas por contexto, em 3 camadas (D-pad, LT+D-pad, LT+LB+D-pad). */
    CONTEXTUAL("Contextual (experimental)");

    private final String rotulo;

    EsquemaGamepad(String rotulo) {
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
