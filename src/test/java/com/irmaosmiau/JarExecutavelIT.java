package com.irmaosmiau;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5D: confere o JAR FINAL (roda em "mvn verify", depois do package).
 *
 * - manifesto com a classe principal, Multi-Release e acesso nativo;
 * - LWJGL/GLFW e as DLLs do Windows dentro do JAR (java -jar continua bastando);
 * - "java -jar ... --diagnostico-gamepad" inicia o GLFW pelo JAR e encerra limpo, com ou
 *   sem controle conectado (nao depende de hardware).
 */
class JarExecutavelIT {

    private static Path jar() {
        String caminho = System.getProperty("jar.jogo");
        assertNotNull(caminho, "propriedade jar.jogo (failsafe)");
        return Paths.get(caminho);
    }

    @Test
    void manifestoEBibliotecasDentroDoJar() throws Exception {
        try (JarFile j = new JarFile(jar().toFile())) {
            Attributes a = j.getManifest().getMainAttributes();
            assertEquals("com.irmaosmiau.main.Main", a.getValue("Main-Class"));
            assertEquals("true", a.getValue("Multi-Release"));
            assertEquals("ALL-UNNAMED", a.getValue("Enable-Native-Access"));
            assertNotNull(j.getEntry("com/irmaosmiau/dispositivos/glfw/ProvedorGamepadGlfw.class"));
            assertNotNull(j.getEntry("org/lwjgl/glfw/GLFW.class"));
            assertNotNull(j.getEntry("windows/x64/org/lwjgl/glfw/glfw.dll"));
            assertNotNull(j.getEntry("windows/x64/org/lwjgl/lwjgl.dll"));
            assertNotNull(j.getEntry("conteudo/miaustudio/PEGADA_1.json"), "conteudo do jogo continua no JAR");
            assertEquals(null, j.getEntry("META-INF/INDEX.LIST"), "INDEX.LIST quebraria o carregamento");
        }
    }

    @Test
    void diagnosticoDeGamepadRodaPeloJar() throws Exception {
        String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder pb = new ProcessBuilder(List.of(java,
                "-Dorg.lwjgl.system.SharedLibraryExtractPath=" + System.getProperty("dir.nativos"),
                "-jar", jar().toString(),
                "--diagnostico-gamepad", "0"));
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        Process p = pb.start();
        boolean terminou = p.waitFor(60, TimeUnit.SECONDS);
        String saida = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!terminou) {
            p.destroyForcibly();
        }
        assertTrue(terminou, "diagnostico travou: " + saida);
        assertTrue(saida.contains("Leitura de gamepad:"), saida);
        assertTrue(saida.contains("Gamepads conectados:"), saida);
        assertTrue(!saida.contains("Exception") && !saida.contains("WARNING"), saida);
        if (System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            assertEquals(0, p.exitValue(), "GLFW deveria iniciar no Windows: " + saida);
        }
        assertTrue(new File(System.getProperty("dir.nativos")).isDirectory(), "DLLs extraidas em target/");
    }
}
