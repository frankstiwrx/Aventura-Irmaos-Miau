package com.irmaosmiau.controles;

/** Agrupamento das acoes nas telas de controles e de ajuda (Entrega 5C). */
public enum GrupoAcao {
    MOVIMENTO("MOVIMENTO"),
    COMBATE("COMBATE"),
    DEFESA("DEFESA"),
    SISTEMA("SISTEMA");

    private final String titulo;

    GrupoAcao(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() {
        return titulo;
    }
}
