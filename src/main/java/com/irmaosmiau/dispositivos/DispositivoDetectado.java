package com.irmaosmiau.dispositivos;

import java.util.Optional;

/**
 * Um joystick que o sistema mostrou ao GLFW (Entrega 5E), com ou sem
 * mapeamento de gamepad. Serve ao diagnostico: o jogo so USA os que tem
 * mapeamento (InfoGamepad).
 *
 * GLFW distingue: glfwJoystickPresent (o aparelho existe) e
 * glfwJoystickIsGamepad (existe E ha um mapeamento para o layout padrao).
 *
 * @param porta       jid do GLFW (0..15)
 * @param nome        nome do joystick no sistema
 * @param guid        GUID usado pelos mapeamentos SDL/GLFW
 * @param gamepad     ha mapeamento de gamepad
 * @param nomeGamepad nome do mapeamento (so com gamepad=true)
 * @param eixos       quantidade de eixos crus (-1 = desconhecido)
 * @param botoes      quantidade de botoes crus (-1 = desconhecido)
 * @param direcionais quantidade de direcionais/hats crus (-1 = desconhecido)
 */
public record DispositivoDetectado(int porta, String nome, String guid, boolean gamepad, String nomeGamepad,
        int eixos, int botoes, int direcionais) {

    /** Por que o jogo nao usa este aparelho (vazio = pode jogar). */
    public Optional<String> motivoIndisponivel() {
        if (gamepad) {
            return Optional.empty();
        }
        return Optional.of("detectado, mas o GLFW não tem mapeamento de gamepad para o GUID " + guid
                + ". Forneça um mapeamento SDL (gamecontrollerdb.txt) — veja docs/CONTROLES.md.");
    }
}
