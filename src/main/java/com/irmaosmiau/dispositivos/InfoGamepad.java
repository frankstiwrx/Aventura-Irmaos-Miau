package com.irmaosmiau.dispositivos;

/**
 * Um gamepad conectado (Entrega 5D).
 *
 * @param chave      unica entre os conectados AGORA (ex.: "glfw-0"); muda se reconectar em outra porta
 * @param identidade identifica o modelo/aparelho entre reconexoes (GUID do GLFW); pode repetir
 *                   para dois controles iguais
 * @param nome       nome legivel ("Xbox Controller")
 */
public record InfoGamepad(String chave, String identidade, String nome) {
}
