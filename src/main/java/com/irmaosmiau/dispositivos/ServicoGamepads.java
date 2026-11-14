package com.irmaosmiau.dispositivos;

import java.util.Locale;

/**
 * Provedor de gamepads usado pelo jogo (Entrega 5D).
 *
 * - Padrao: AUSENTE (nada e lido). Testes e MainPrototipo ficam assim, ou
 *   instalam um ProvedorGamepadSimulado.
 * - {@link #iniciarFisico()} (chamado pelo Main depois que a janela existe)
 *   tenta o GLFW por REFLEXAO: sem a biblioteca, sem as DLLs, num sistema nao
 *   suportado ou com {@code -Dirmaosmiau.gamepad=desligado}, o jogo segue so
 *   com o teclado e o motivo aparece em CONFIGURACOES -> CONTROLES.
 */
public final class ServicoGamepads {

    public static final String PROPRIEDADE = "irmaosmiau.gamepad";
    static final String CLASSE_GLFW = "com.irmaosmiau.dispositivos.glfw.ProvedorGamepadGlfw";

    private static ProvedorGamepads global;

    private ServicoGamepads() {
    }

    public static synchronized ProvedorGamepads global() {
        if (global == null) {
            global = new ProvedorGamepadsAusente("leitura de gamepad não iniciada");
        }
        return global;
    }

    /** Troca o provedor (encerra o anterior). null = ausente. */
    public static synchronized void instalarGlobal(ProvedorGamepads provedor) {
        if (global != null && global != provedor) {
            global.encerrar();
        }
        global = provedor;
    }

    /** Encerra a leitura (fim do programa). */
    public static synchronized void encerrar() {
        if (global != null) {
            global.encerrar();
        }
        global = null;
    }

    /** Tenta a leitura fisica; nunca lanca excecao. */
    public static synchronized ProvedorGamepads iniciarFisico() {
        ProvedorGamepads p = criarFisico();
        instalarGlobal(p);
        if (!p.isDisponivel()) {
            System.err.println("[gamepad] " + p.descricao() + " (o jogo segue com o teclado)");
        }
        return p;
    }

    static ProvedorGamepads criarFisico() {
        String config = System.getProperty(PROPRIEDADE, "");
        if (config.equalsIgnoreCase("desligado")) {
            return new ProvedorGamepadsAusente("gamepad desligado (-D" + PROPRIEDADE + "=desligado)");
        }
        String so = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (so.contains("mac")) {
            // GLFW no macOS exige a thread principal do processo (a do Swing nao serve).
            return new ProvedorGamepadsAusente("gamepad ainda não suportado no macOS");
        }
        prepararLwjgl();
        try {
            Class<?> c = Class.forName(CLASSE_GLFW);
            return (ProvedorGamepads) c.getMethod("iniciar").invoke(null);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            return new ProvedorGamepadsAusente("gamepad indisponível: " + descrever(ex.getCause()));
        } catch (Throwable t) {
            return new ProvedorGamepadsAusente("gamepad indisponível: " + descrever(t));
        }
    }

    /**
     * No JDK 25+ usa o backend de memoria FFM do LWJGL (sem os avisos de
     * sun.misc.Unsafe). So define se ninguem definiu antes.
     */
    private static void prepararLwjgl() {
        if (Runtime.version().feature() >= 25 && System.getProperty("org.lwjgl.system.memoryBackend") == null) {
            System.setProperty("org.lwjgl.system.memoryBackend", "org.lwjgl.system.MemoryBackendFFM");
        }
    }

    private static String descrever(Throwable t) {
        if (t == null) {
            return "erro desconhecido";
        }
        if (t instanceof NoClassDefFoundError || t instanceof ClassNotFoundException) {
            return "biblioteca LWJGL não encontrada";
        }
        if (t instanceof UnsatisfiedLinkError) {
            return "biblioteca nativa do GLFW não carregou";
        }
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }
}
