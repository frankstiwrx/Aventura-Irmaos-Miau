package com.irmaosmiau.dispositivos;

/**
 * Eixos analogicos do layout padrao (Entrega 5D), na ordem SDL/GLFW.
 *
 * Analogicos: -1..1, com Y NEGATIVO para cima (convencao SDL/GLFW).
 * Gatilhos: 0 (solto) .. 1 (apertado ate o fim), ja convertidos pelo provedor.
 */
public enum EixoGamepad {
    ESQ_X,
    ESQ_Y,
    DIR_X,
    DIR_Y,
    GATILHO_ESQ,
    GATILHO_DIR
}
