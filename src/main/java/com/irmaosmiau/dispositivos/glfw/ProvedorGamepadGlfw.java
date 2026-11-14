package com.irmaosmiau.dispositivos.glfw;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_1;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_HAT_BUTTONS;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetGamepadName;
import static org.lwjgl.glfw.GLFW.glfwGetGamepadState;
import static org.lwjgl.glfw.GLFW.glfwGetJoystickGUID;
import static org.lwjgl.glfw.GLFW.glfwGetJoystickName;
import static org.lwjgl.glfw.GLFW.glfwGetVersionString;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwInitHint;
import static org.lwjgl.glfw.GLFW.glfwJoystickIsGamepad;
import static org.lwjgl.glfw.GLFW.glfwJoystickPresent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.InfoGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepads;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWGamepadState;

/**
 * Gamepads fisicos via GLFW (LWJGL 3), Entrega 5D.
 *
 * UNICA classe do jogo que referencia LWJGL; e carregada por reflexao em
 * ServicoGamepads, entao a falta da biblioteca ou das DLLs nunca impede o
 * jogo de abrir.
 *
 * Toda chamada GLFW acontece numa thread propria ("irmaosmiau-gamepad"):
 * glfwInit, glfwPollEvents (conexoes), leitura com glfwGetGamepadState
 * (layout padrao com os mapeamentos SDL embutidos no GLFW) e glfwTerminate.
 * A thread publica um mapa IMUTAVEL a cada leitura (~120 Hz); o jogo so le
 * esse mapa (campo volatile), sem bloquear a thread do Swing.
 */
public final class ProvedorGamepadGlfw implements ProvedorGamepads, Runnable {

    private static final long INTERVALO_MS = 8;
    private static final long ESPERA_INICIO_S = 5;

    private record Leitura(InfoGamepad info, EstadoGamepad estado) {
    }

    private volatile Map<String, Leitura> leituras = Map.of();
    private volatile boolean ativo = true;
    private volatile String erroInicio;
    private volatile String versao = "?";
    private final CountDownLatch iniciou = new CountDownLatch(1);
    private final Thread thread;

    private ProvedorGamepadGlfw() {
        thread = new Thread(this, "irmaosmiau-gamepad");
        thread.setDaemon(true);
    }

    /**
     * Inicia a thread e espera o glfwInit.
     *
     * @throws IllegalStateException se o GLFW nao iniciar (o chamador usa o provedor ausente)
     */
    public static ProvedorGamepadGlfw iniciar() throws InterruptedException {
        ProvedorGamepadGlfw p = new ProvedorGamepadGlfw();
        p.thread.start();
        if (!p.iniciou.await(ESPERA_INICIO_S, TimeUnit.SECONDS)) {
            p.encerrar();
            throw new IllegalStateException("GLFW nao respondeu ao iniciar");
        }
        if (p.erroInicio != null) {
            throw new IllegalStateException(p.erroInicio);
        }
        return p;
    }

    @Override
    public void run() {
        GLFWErrorCallback callback = null;
        boolean iniciado = false;
        try {
            callback = GLFWErrorCallback.create((codigo, descricao) ->
                    System.err.println("[gamepad] GLFW erro " + codigo + ": " + GLFWErrorCallback.getDescription(descricao)));
            glfwSetErrorCallback(callback);
            // D-pad como botoes do layout padrao, sem "hats" duplicados nos botoes crus.
            glfwInitHint(GLFW_JOYSTICK_HAT_BUTTONS, GLFW_FALSE);
            iniciado = glfwInit();
            if (!iniciado) {
                erroInicio = "glfwInit falhou";
                return;
            }
            versao = glfwGetVersionString();
            try (GLFWGamepadState estado = GLFWGamepadState.calloc()) {
                // Primeira leitura antes de liberar quem esta esperando o inicio.
                glfwPollEvents();
                leituras = ler(estado);
                iniciou.countDown();
                while (ativo) {
                    glfwPollEvents();
                    leituras = ler(estado);
                    Thread.sleep(INTERVALO_MS);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            if (erroInicio == null && iniciou.getCount() > 0) {
                erroInicio = t.getClass().getSimpleName() + ": " + t.getMessage();
            } else {
                System.err.println("[gamepad] leitura interrompida: " + t);
            }
        } finally {
            leituras = Map.of();               // nada fica "pressionado" depois do fim
            if (iniciado) {
                glfwTerminate();
            }
            glfwSetErrorCallbackSeguro();
            if (callback != null) {
                callback.free();
            }
            iniciou.countDown();
        }
    }

    private static void glfwSetErrorCallbackSeguro() {
        try {
            glfwSetErrorCallback(null);
        } catch (Throwable ignorado) {
            // biblioteca nao carregou: nada a desfazer
        }
    }

    private static Map<String, Leitura> ler(GLFWGamepadState estado) {
        Map<String, Leitura> m = new LinkedHashMap<>();
        boolean[] botoes = new boolean[BotaoGamepad.FISICOS];
        float[] eixos = new float[EixoGamepad.values().length];
        for (int jid = GLFW_JOYSTICK_1; jid <= GLFW_JOYSTICK_LAST; jid++) {
            if (!glfwJoystickPresent(jid) || !glfwJoystickIsGamepad(jid)) {
                continue;
            }
            if (!glfwGetGamepadState(jid, estado)) {
                continue;
            }
            for (int b = 0; b < botoes.length; b++) {
                botoes[b] = estado.buttons(b) == GLFW_PRESS;
            }
            for (int e = 0; e < eixos.length; e++) {
                float v = estado.axes(e);
                // Gatilhos no GLFW vao de -1 (solto) a 1; no jogo, de 0 a 1.
                eixos[e] = e >= EixoGamepad.GATILHO_ESQ.ordinal() ? (v + 1f) / 2f : v;
            }
            String nome = glfwGetGamepadName(jid);
            if (nome == null) {
                nome = glfwGetJoystickName(jid);
            }
            String guid = glfwGetJoystickGUID(jid);
            String chave = "glfw-" + jid;
            m.put(chave, new Leitura(
                    new InfoGamepad(chave, guid == null ? chave : guid, nome == null ? "Gamepad " + (jid + 1) : nome),
                    EstadoGamepad.de(botoes, eixos)));
        }
        return Map.copyOf(m);
    }

    @Override
    public List<InfoGamepad> conectados() {
        List<InfoGamepad> lista = new ArrayList<>();
        Map<String, Leitura> atual = leituras;
        for (int jid = GLFW_JOYSTICK_1; jid <= GLFW_JOYSTICK_LAST; jid++) {
            Leitura l = atual.get("glfw-" + jid);
            if (l != null) {
                lista.add(l.info());
            }
        }
        return List.copyOf(lista);
    }

    @Override
    public Optional<EstadoGamepad> estado(String chave) {
        Leitura l = leituras.get(chave);
        return l == null ? Optional.empty() : Optional.of(l.estado());
    }

    @Override
    public boolean isDisponivel() {
        return ativo && thread.isAlive();
    }

    @Override
    public String descricao() {
        return "GLFW " + versao.split(" ")[0];
    }

    @Override
    public void encerrar() {
        ativo = false;
        leituras = Map.of();
        if (Thread.currentThread() != thread) {
            try {
                thread.join(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
