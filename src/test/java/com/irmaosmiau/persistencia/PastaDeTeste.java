package com.irmaosmiau.persistencia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDirFactory;

/**
 * Pastas temporarias dos testes de persistencia em target/dados-testes/.
 *
 * Motivo: no Windows, antivirus/indexador seguram por instantes arquivos
 * recem-gravados e a limpeza automatica do @TempDir falhava de vez em
 * quando ("DirectoryNotEmptyException") mesmo com os testes aprovados.
 * Aqui a pasta nao e apagada pelo JUnit (cleanup = NEVER); "mvn clean"
 * remove tudo. Nada e gravado na pasta pessoal do desenvolvedor.
 */
public final class PastaDeTeste implements TempDirFactory {

    @Override
    public Path createTempDirectory(AnnotatedElementContext elemento, ExtensionContext contexto)
            throws IOException {
        Path base = Paths.get("target", "dados-testes").toAbsolutePath();
        Files.createDirectories(base);
        return Files.createTempDirectory(base, contexto.getRequiredTestClass().getSimpleName() + "-");
    }
}
