package com.irmaosmiau.persistencia;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Onde ficam os dados do jogador.
 *
 * Padrao: {@code <pasta pessoal>/.aventura-irmaos-miau/}. Pode ser trocado
 * pela propriedade {@code -Dirmaosmiau.dados=<pasta>} ou, nos testes,
 * passando um diretorio temporario diretamente.
 *
 * Nada e criado aqui: as pastas sao criadas na primeira gravacao.
 */
public final class DiretorioDados {

    public static final String NOME_PASTA_PADRAO = ".aventura-irmaos-miau";
    public static final String PROPRIEDADE_PASTA = "irmaosmiau.dados";

    private final Path raiz;

    public DiretorioDados(Path raiz) {
        if (raiz == null) {
            throw new IllegalArgumentException("O diretorio de dados e obrigatorio.");
        }
        this.raiz = raiz.toAbsolutePath().normalize();
    }

    /** Pasta do usuario (ou a da propriedade irmaosmiau.dados, se definida). */
    public static DiretorioDados doUsuario() {
        String configurada = System.getProperty(PROPRIEDADE_PASTA);
        if (configurada != null && !configurada.isBlank()) {
            return new DiretorioDados(Paths.get(configurada.trim()));
        }
        return new DiretorioDados(Paths.get(System.getProperty("user.home"), NOME_PASTA_PADRAO));
    }

    public Path getRaiz() {
        return raiz;
    }

    /** Caminho dentro da raiz; recusa caminhos que escapem dela. */
    public Path resolver(String relativo) {
        Path p = raiz.resolve(relativo).normalize();
        if (!p.startsWith(raiz)) {
            throw new IllegalArgumentException("Caminho fora da pasta de dados: " + relativo);
        }
        return p;
    }
}
