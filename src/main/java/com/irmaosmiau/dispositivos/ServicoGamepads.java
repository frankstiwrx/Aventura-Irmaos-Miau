package com.irmaosmiau.dispositivos;

import com.irmaosmiau.persistencia.DiretorioDados;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 *
 * Entrega 5E: mapeamentos extras (formato SDL) sao lidos, SOMENTE LEITURA, de
 * {@code -Dirmaosmiau.gamepad.mapeamentos=<arquivo>} ou, se existir,
 * {@code <pasta de dados>/gamecontrollerdb.txt}. O jogo nunca cria esse arquivo.
 */
public final class ServicoGamepads {

    public static final String PROPRIEDADE = "irmaosmiau.gamepad";
    static final String CLASSE_GLFW = "com.irmaosmiau.dispositivos.glfw.ProvedorGamepadGlfw";
    public static final String PROPRIEDADE_MAPEAMENTOS = "irmaosmiau.gamepad.mapeamentos";
    public static final String ARQUIVO_MAPEAMENTOS = "gamecontrollerdb.txt";

    private static ArquivoMapeamentos.Status statusMapeamentos = ArquivoMapeamentos.Status.NENHUM;

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
        Path arquivo = localizarMapeamentos();
        ArquivoMapeamentos.Validacao validacao = null;
        String problemaLeitura = null;
        if (arquivo != null) {
            try {
                validacao = ArquivoMapeamentos.lerArquivo(arquivo);
            } catch (java.io.IOException | RuntimeException ex) {
                problemaLeitura = "não foi possível ler: " + ex.getMessage();
            }
        }
        String texto = validacao != null && validacao.temValidas() ? validacao.textoParaGlfw() : null;
        try {
            Class<?> c = Class.forName(CLASSE_GLFW);
            ProvedorGamepads p = (ProvedorGamepads) c.getMethod("iniciar", String.class).invoke(null, texto);
            registrarStatus(arquivo, validacao, problemaLeitura, p.mapeamentosAceitos().orElse(null));
            return p;
        } catch (java.lang.reflect.InvocationTargetException ex) {
            registrarStatus(arquivo, validacao, problemaLeitura, null);
            return new ProvedorGamepadsAusente("gamepad indisponível: " + descrever(ex.getCause()));
        } catch (Throwable t) {
            registrarStatus(arquivo, validacao, problemaLeitura, null);
            return new ProvedorGamepadsAusente("gamepad indisponível: " + descrever(t));
        }
    }

    /** Arquivo de mapeamentos extras, se houver (somente leitura). */
    static Path localizarMapeamentos() {
        String configurado = System.getProperty(PROPRIEDADE_MAPEAMENTOS);
        if (configurado != null && !configurado.isBlank()) {
            return Paths.get(configurado.trim());
        }
        try {
            Path padrao = DiretorioDados.doUsuario().resolver(ARQUIVO_MAPEAMENTOS);
            return Files.isRegularFile(padrao) ? padrao : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static synchronized void registrarStatus(Path arquivo, ArquivoMapeamentos.Validacao v,
            String problemaLeitura, Boolean aceito) {
        if (arquivo == null) {
            statusMapeamentos = ArquivoMapeamentos.Status.NENHUM;
            return;
        }
        java.util.List<String> problemas = new java.util.ArrayList<>();
        if (problemaLeitura != null) {
            problemas.add(problemaLeitura);
        }
        if (v != null) {
            problemas.addAll(v.problemas());
            if (!v.temValidas() && problemaLeitura == null) {
                problemas.add("nenhuma linha válida: o arquivo foi ignorado");
            }
        }
        statusMapeamentos = new ArquivoMapeamentos.Status(arquivo.toString(), v == null ? 0 : v.validas().size(),
                java.util.List.copyOf(problemas), aceito);
    }

    /** Situacao do ultimo arquivo de mapeamentos (para o diagnostico). */
    public static synchronized ArquivoMapeamentos.Status statusMapeamentos() {
        return statusMapeamentos;
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
