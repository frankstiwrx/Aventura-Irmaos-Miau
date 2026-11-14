package com.irmaosmiau.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5D: dispositivos.json proprio, sempre em pasta de teste. */
class DispositivosPersistenciaTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private RepositorioDispositivos repo() {
        return new RepositorioDispositivos(new DiretorioDados(tmp));
    }

    private Path arquivo() {
        return tmp.resolve(RepositorioDispositivos.NOME_ARQUIVO);
    }

    @Test
    void semArquivoTodosNoTecladoENadaECriado() throws Exception {
        PreferenciasDispositivos p = PreferenciasDispositivos.carregar(repo());
        assertEquals(AssociacaoDispositivos.padrao(), p.getAtual());
        assertTrue(p.getAvisos().isEmpty());
        try (Stream<Path> s = Files.list(tmp)) {
            assertEquals(0, s.count());
        }
    }

    @Test
    void escolhaSobreviveAReinicializacaoSemTocarNosOutrosArquivos() throws Exception {
        AssociacaoDispositivos j2 = AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_2, TipoDispositivo.GAMEPAD);
        PreferenciasDispositivos.carregar(repo()).salvar(j2);
        String json = Files.readString(arquivo());
        assertTrue(json.contains("DISPOSITIVOS_IRMAOS_MIAU") && json.contains("GAMEPAD"), json);
        assertEquals(j2, PreferenciasDispositivos.carregar(repo()).getAtual());
        assertFalse(Files.exists(tmp.resolve(RepositorioControles.NOME_ARQUIVO)));
        assertFalse(Files.exists(tmp.resolve(RepositorioConfiguracoes.NOME_ARQUIVO)));
    }

    @Test
    void jogadorAusenteNoArquivoFicaNoTeclado() throws Exception {
        Files.writeString(arquivo(), "{\"tipo\":\"DISPOSITIVOS_IRMAOS_MIAU\",\"versaoEsquema\":1,"
                + "\"jogadores\":{\"jogador1\":\"GAMEPAD\"}}");
        AssociacaoDispositivos a = repo().carregar();
        assertEquals(TipoDispositivo.GAMEPAD, a.tipo(SlotJogador.JOGADOR_1));
        assertEquals(TipoDispositivo.TECLADO, a.tipo(SlotJogador.JOGADOR_2));
    }

    @Test
    void arquivoInvalidoUsaTecladoEPreservaOArquivoAteSalvar() throws Exception {
        String[] ruins = {
            "nao e json",
            "{\"tipo\":\"DISPOSITIVOS_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":\"JOYSTICK\"}}",
            "{\"tipo\":\"DISPOSITIVOS_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador9\":\"TECLADO\"}}",
            "{\"tipo\":\"DISPOSITIVOS_IRMAOS_MIAU\",\"versaoEsquema\":7}",
            "{\"tipo\":\"DISPOSITIVOS_IRMAOS_MIAU\",\"versaoEsquema\":1,\"extra\":1}",
        };
        for (String ruim : ruins) {
            Files.writeString(arquivo(), ruim);
            PreferenciasDispositivos p = PreferenciasDispositivos.carregar(repo());
            assertEquals(AssociacaoDispositivos.padrao(), p.getAtual(), ruim);
            assertEquals(1, p.getAvisos().size(), ruim);
            assertEquals(ruim, Files.readString(arquivo()), "intacto");
        }
        PreferenciasDispositivos p = PreferenciasDispositivos.carregar(repo());
        p.salvar(AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD));
        try (Stream<Path> s = Files.list(tmp)) {
            assertEquals(1, s.filter(f -> f.getFileName().toString().startsWith("dispositivos.json.corrompido-")).count());
        }
        assertEquals(TipoDispositivo.GAMEPAD, repo().carregar().tipo(SlotJogador.JOGADOR_1));
    }
}
