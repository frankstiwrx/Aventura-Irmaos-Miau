package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.DirecaoTecnica;

/**
 * Comandos do esquema CONTEXTUAL que nao sao acoes de jogador (Entrega 5F):
 * as quatro direcoes de tecnica e os dois modificadores de camada.
 */
public enum ComandoGamepad {
    TECNICA_CIMA("Técnica ↑", DirecaoTecnica.CIMA),
    TECNICA_DIREITA("Técnica →", DirecaoTecnica.DIREITA),
    TECNICA_BAIXO("Técnica ↓", DirecaoTecnica.BAIXO),
    TECNICA_ESQUERDA("Técnica ←", DirecaoTecnica.ESQUERDA),
    /** Segurado: camada 2 (padrao LT). */
    MODIFICADOR_CAMADA_2("Modificador camada 2", null),
    /** Segurado junto com o da camada 2: camada 3 (padrao LB). */
    MODIFICADOR_CAMADA_3("Modificador camada 3 (com o da 2)", null);

    private final String rotulo;
    private final DirecaoTecnica direcao;

    ComandoGamepad(String rotulo, DirecaoTecnica direcao) {
        this.rotulo = rotulo;
        this.direcao = direcao;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Direcao (so os 4 de tecnica). */
    public DirecaoTecnica getDirecao() {
        return direcao;
    }
}
