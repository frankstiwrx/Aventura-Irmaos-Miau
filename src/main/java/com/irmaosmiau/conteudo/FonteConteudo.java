package com.irmaosmiau.conteudo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * De onde os arquivos de conteudo sao lidos.
 *
 * O jogo usa o classpath (src/main/resources, dentro do JAR). Testes podem
 * usar o mesmo classpath (src/test/resources) ou uma fonte em memoria.
 * Os arquivos sao apenas LIDOS; nada aqui escreve no disco.
 */
@FunctionalInterface
public interface FonteConteudo {

    /**
     * Retorna o texto UTF-8 do arquivo.
     *
     * @throws ConteudoInvalidoException se o arquivo nao existir
     */
    String ler(String caminho);

    /** Recursos do classpath. Caminhos podem comecar ou nao com "/". */
    static FonteConteudo classpath() {
        return caminho -> {
            if (caminho == null || caminho.isBlank()) {
                throw new ConteudoInvalidoException("Caminho de recurso vazio.");
            }
            String normalizado = caminho.startsWith("/") ? caminho : "/" + caminho;
            try (InputStream in = FonteConteudo.class.getResourceAsStream(normalizado)) {
                if (in == null) {
                    throw new ConteudoInvalidoException(
                            "Recurso nao encontrado no classpath: " + normalizado);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new ConteudoInvalidoException(
                        "Falha ao ler o recurso " + normalizado + ": " + ex.getMessage(), ex);
            }
        };
    }
}
