package com.irmaosmiau.dispositivos;

import java.util.ArrayList;
import java.util.List;

/**
 * Texto do diagnostico de gamepad (Entrega 5E). Funcao pura: recebe o
 * provedor (fisico ou simulado) e a situacao dos mapeamentos, devolve as
 * linhas. Distingue claramente:
 *
 * 1. biblioteca GLFW inicializada ou nao;
 * 2. joysticks FISICAMENTE detectados (glfwJoystickPresent);
 * 3. gamepads com mapeamento (glfwJoystickIsGamepad) = utilizaveis no jogo;
 * 4. nome, GUID, eixos, botoes e direcionais de cada um;
 * 5. o motivo de um aparelho nao estar disponivel.
 */
public final class RelatorioDiagnostico {

    private RelatorioDiagnostico() {
    }

    public static List<String> linhas(ProvedorGamepads p, ArquivoMapeamentos.Status mapeamentos) {
        List<String> l = new ArrayList<>();
        l.add("Diagnóstico de gamepad — Aventura dos Irmãos Miau");
        l.add("1. Biblioteca: " + (p.isDisponivel() ? "OK - " + p.descricao() + " inicializada"
                : "INDISPONIVEL - " + p.descricao()));
        // Linha mantida para scripts/testes da 5D.
        l.add("Leitura de gamepad: " + (p.isDisponivel() ? "OK" : "INDISPONIVEL") + " - " + p.descricao());

        List<DispositivoDetectado> detectados = p.detectados();
        l.add("2. Joysticks detectados pelo sistema: " + detectados.size());
        for (DispositivoDetectado d : detectados) {
            l.add("   [porta " + d.porta() + "] " + texto(d.nome()) + " | GUID " + texto(d.guid()) + " | "
                    + contagem(d.eixos(), "eixo(s)") + ", " + contagem(d.botoes(), "botão(ões)") + ", "
                    + contagem(d.direcionais(), "direcional(is)")
                    + " | gamepad: " + (d.gamepad() ? "SIM (" + texto(d.nomeGamepad()) + ")" : "NÃO"));
            d.motivoIndisponivel().ifPresent(m -> l.add("      -> " + m));
        }

        List<InfoGamepad> utilizaveis = p.conectados();
        l.add("3. Gamepads utilizáveis no jogo: " + utilizaveis.size());
        // Linha mantida da 5D.
        l.add("Gamepads conectados: " + utilizaveis.size());
        for (InfoGamepad g : utilizaveis) {
            l.add("   " + g.chave() + " | " + g.nome() + " | " + g.identidade());
        }

        l.add("4. Mapeamentos extras: " + mapeamentos.descricao());
        for (String problema : mapeamentos.problemas()) {
            l.add("      -> " + problema);
        }

        l.add("Conclusão: " + conclusao(p, detectados, utilizaveis));
        return l;
    }

    static String conclusao(ProvedorGamepads p, List<DispositivoDetectado> detectados, List<InfoGamepad> utilizaveis) {
        if (!p.isDisponivel()) {
            return "a leitura de gamepad não está disponível neste computador; o jogo funciona só com o teclado.";
        }
        if (detectados.isEmpty()) {
            return "nenhum joystick chegou ao GLFW. O problema está antes do jogo: confira cabo/pareamento "
                    + "Bluetooth, se o Windows mostra o controle em \"Configurar controladores de jogo USB\" "
                    + "(joy.cpl), se outro programa (Steam Input, DS4Windows) não o mantém exclusivo e o modo "
                    + "do controle (XInput/DirectInput).";
        }
        long semMapa = detectados.stream().filter(d -> !d.gamepad()).count();
        if (utilizaveis.isEmpty()) {
            return semMapa + " joystick(s) detectado(s), mas SEM mapeamento de gamepad: forneça um mapeamento "
                    + "SDL com o GUID mostrado acima (docs/CONTROLES.md, \"Joystick sem mapeamento\").";
        }
        if (semMapa > 0) {
            return utilizaveis.size() + " gamepad(s) prontos; " + semMapa + " joystick(s) sem mapeamento ficam de fora.";
        }
        return "pronto para jogar: escolha Gamepad em CONFIGURAÇÕES → CONTROLES.";
    }

    private static String contagem(int n, String unidade) {
        return (n < 0 ? "?" : String.valueOf(n)) + " " + unidade;
    }

    private static String texto(String s) {
        return s == null || s.isBlank() ? "(sem nome)" : s;
    }
}
