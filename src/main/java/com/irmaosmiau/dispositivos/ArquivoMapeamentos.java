package com.irmaosmiau.dispositivos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mapeamentos extras de gamepad no formato SDL_GameControllerDB, que o
 * GLFW aceita em glfwUpdateGamepadMappings (Entrega 5E).
 *
 * Uma linha por controle:
 * <pre>
 * GUID(32 hex),Nome,a:b0,b:b1,...,leftx:a0,lefty:a1,platform:Windows,
 * </pre>
 * Linhas vazias e comentarios (#) sao ignorados. Linhas com formato errado
 * NAO sao enviadas ao GLFW e aparecem no diagnostico com o motivo. O jogo
 * so LE o arquivo; nunca o cria nem altera.
 */
public final class ArquivoMapeamentos {

    /** Resultado da validacao. */
    public record Validacao(List<String> validas, List<String> problemas) {

        public boolean temValidas() {
            return !validas.isEmpty();
        }

        /** Texto pronto para o GLFW (so linhas validas). */
        public String textoParaGlfw() {
            return String.join("\n", validas) + "\n";
        }
    }

    /** Situacao dos mapeamentos extras para o diagnostico. */
    public record Status(String arquivo, int validas, List<String> problemas, Boolean aceitoPeloGlfw) {

        public static final Status NENHUM = new Status(null, 0, List.of(), null);

        public String descricao() {
            if (arquivo == null) {
                return "nenhum arquivo de mapeamento extra (usando os mapeamentos embutidos no GLFW)";
            }
            String r = arquivo + ": " + validas + " linha(s) válida(s)";
            if (!problemas.isEmpty()) {
                r += ", " + problemas.size() + " ignorada(s)";
            }
            if (aceitoPeloGlfw != null) {
                r += aceitoPeloGlfw ? " · aceito pelo GLFW" : " · RECUSADO pelo GLFW";
            }
            return r;
        }
    }

    private ArquivoMapeamentos() {
    }

    public static Validacao validar(String texto) {
        List<String> validas = new ArrayList<>();
        List<String> problemas = new ArrayList<>();
        String[] linhas = texto == null ? new String[0] : texto.split("\\R");
        for (int i = 0; i < linhas.length; i++) {
            String l = linhas[i].strip();
            if (l.isEmpty() || l.startsWith("#")) {
                continue;
            }
            String motivo = problema(l);
            if (motivo == null) {
                validas.add(l);
            } else {
                problemas.add("linha " + (i + 1) + ": " + motivo);
            }
        }
        return new Validacao(List.copyOf(validas), List.copyOf(problemas));
    }

    private static String problema(String linha) {
        String[] partes = linha.split(",");
        if (partes.length < 3) {
            return "esperado GUID,Nome,mapeamentos...";
        }
        if (!partes[0].toLowerCase(Locale.ROOT).matches("[0-9a-f]{32}")) {
            return "GUID precisa ter 32 caracteres hexadecimais";
        }
        if (partes[1].isBlank()) {
            return "nome vazio";
        }
        int mapeados = 0;
        for (int i = 2; i < partes.length; i++) {
            String p = partes[i].strip();
            if (p.isEmpty()) {
                continue;
            }
            int doisPontos = p.indexOf(':');
            if (doisPontos <= 0 || doisPontos == p.length() - 1) {
                return "entrada sem \"chave:valor\": " + p;
            }
            if (!p.startsWith("platform:")) {
                mapeados++;
            }
        }
        return mapeados == 0 ? "nenhum botao ou eixo mapeado" : null;
    }

    /** Le e valida um arquivo (somente leitura). */
    public static Validacao lerArquivo(Path arquivo) throws IOException {
        return validar(Files.readString(arquivo, StandardCharsets.UTF_8));
    }
}
