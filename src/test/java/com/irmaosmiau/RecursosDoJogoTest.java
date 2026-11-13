package com.irmaosmiau;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Recursos lidos pelo classpath com caminho absoluto: os mesmos caminhos
 * valem no NetBeans (target/classes) e dentro do JAR. Se um arquivo for
 * renomeado ou mudar de pasta, este teste acusa antes do jogo.
 */
class RecursosDoJogoTest {

    private static final String[] OBRIGATORIOS = {
        "/assets/backgrounds/menuprincipal.png",
        "/assets/sprites/miau-branco.png",
        "/assets/sprites/miau-preto.png",
        "/conteudo/tecnicas/tecnicas.json",
        "/conteudo/animacoes/animacoes.json",
        "/conteudo/miaustudio/PEGADA_1.json",
        "/audio/LEIAME.md",
        "/audio/musica/menu.wav",
        "/audio/musica/treino.wav",
        "/audio/efeitos/pegada.wav",
        "/audio/efeitos/rompimento.wav"
    };

    @Test
    void todosOsRecursosDoJogoEstaoNoClasspath() {
        for (String caminho : OBRIGATORIOS) {
            assertNotNull(RecursosDoJogoTest.class.getResource(caminho), "faltando: " + caminho);
        }
    }

    @Test
    void musicaDeCombateAindaNaoExisteDeProposito() {
        // Documentado em audio/LEIAME.md: o jogo segue em silencio.
        assertNull(RecursosDoJogoTest.class.getResource("/audio/musica/combate.wav"));
    }
}
