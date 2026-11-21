package com.irmaosmiau.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.ComandoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5F: gamepad.json proprio (esquema e botoes por slot), sempre em pasta de teste. */
class GamepadPersistenciaTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private RepositorioGamepad repo() {
        return new RepositorioGamepad(new DiretorioDados(tmp));
    }

    private Path arquivo() {
        return tmp.resolve(RepositorioGamepad.NOME_ARQUIVO);
    }

    private static MapaGamepad personalizado() {
        return MapaGamepad.padrao()
                .comEsquema(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL)
                // 5O.1 (pedido): o Y voltou a ficar livre (acrobacia = R3 + direcao em pe).
                .comBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(ComandoGamepad.TECNICA_CIMA), BotaoGamepad.NORTE)
                .semBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.ROLAR))
                // complemento 5Q.8 (pedido): respirar agora e no Y; aqui o Y e a tecnica, respirar vai no R3
                .comBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.RESPIRAR), BotaoGamepad.R3)
                .semBotao(SlotJogador.JOGADOR_2, ItemGamepad.de(AcaoJogador.CORRER));
    }

    @Test
    void semArquivoClassicoENadaECriado() throws Exception {
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertEquals(MapaGamepad.padrao(), p.getAtual());
        assertTrue(p.getAvisos().isEmpty());
        try (Stream<Path> s = Files.list(tmp)) {
            assertEquals(0, s.count());
        }
    }

    @Test
    void salvarEReiniciar() throws Exception {
        PreferenciasGamepad.carregar(repo()).salvar(personalizado());
        String json = Files.readString(arquivo());
        assertTrue(json.contains("MAPEAMENTO_GAMEPAD_IRMAOS_MIAU") && json.contains("\"CONTEXTUAL\""), json);
        assertFalse(json.contains("gamecontrollerdb"), "nao e o arquivo SDL");
        MapaGamepad lido = PreferenciasGamepad.carregar(repo()).getAtual();
        assertEquals(personalizado(), lido);
        assertEquals(BotaoGamepad.NORTE,
                lido.botao(SlotJogador.JOGADOR_1, ItemGamepad.de(ComandoGamepad.TECNICA_CIMA)).orElseThrow());
        assertTrue(lido.botao(SlotJogador.JOGADOR_2, ItemGamepad.de(AcaoJogador.CORRER)).isEmpty(), "sem botao e valido");
        assertFalse(Files.exists(tmp.resolve(RepositorioControles.NOME_ARQUIVO)), "controles.json intacto");
        assertFalse(Files.exists(tmp.resolve(RepositorioDispositivos.NOME_ARQUIVO)));
    }

    @Test
    void jogadorAusenteFicaNoClassico() throws Exception {
        Files.writeString(arquivo(), "{\"tipo\":\"MAPEAMENTO_GAMEPAD_IRMAOS_MIAU\",\"versaoEsquema\":1,"
                + "\"jogadores\":{\"jogador2\":{\"esquema\":\"CONTEXTUAL\",\"botoes\":{\"TECNICA_BAIXO\":\"DPAD_BAIXO\"}}}}");
        MapaGamepad m = repo().carregar();
        // 5O.2 (pedido): o jogador ausente fica no esquema PADRAO, que agora e o Contextual (antes Classico).
        assertEquals(MapaGamepad.ESQUEMA_PADRAO, m.esquema(SlotJogador.JOGADOR_1));
        assertEquals(EsquemaGamepad.CONTEXTUAL, m.esquema(SlotJogador.JOGADOR_2));
        // complemento 5Q.8 (pedido): arquivo de antes (o RT sozinho respirava): o Y livre passa a respirar
        assertEquals(2, m.itens(SlotJogador.JOGADOR_2).stream()
                .filter(i -> m.botao(SlotJogador.JOGADOR_2, i).isPresent()).count(), "o do arquivo + respirar no Y");
        assertEquals(BotaoGamepad.NORTE, m.botao(SlotJogador.JOGADOR_2, ItemGamepad.de(AcaoJogador.RESPIRAR)).orElseThrow());
    }

    @Test
    void arquivosInvalidosPreservadosAteSalvar() throws Exception {
        String[] ruins = {
            "lixo",
            "{\"tipo\":\"MAPEAMENTO_GAMEPAD_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                    + "{\"esquema\":\"CLASSICO\",\"botoes\":{\"PULAR\":\"SUL\",\"CORRER\":\"SUL\"}}}}",
            "{\"tipo\":\"MAPEAMENTO_GAMEPAD_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                    + "{\"esquema\":\"CLASSICO\",\"botoes\":{\"TECNICA_CIMA\":\"DPAD_CIMA\"}}}}",
            "{\"tipo\":\"MAPEAMENTO_GAMEPAD_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                    + "{\"esquema\":\"CLASSICO\",\"botoes\":{\"PULAR\":\"BOTAO_99\"}}}}",
            "{\"tipo\":\"MAPEAMENTO_GAMEPAD_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                    + "{\"esquema\":\"CLASSICO\",\"botoes\":{\"PULAR\":\"INICIAR\"}}}}",
            // 5I (pedido): "jogador2 INICIAR_PEGADA" saiu desta lista: agora e valido.
            "{\"tipo\":\"MAPEAMENTO_GAMEPAD_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                    + "{\"esquema\":\"MAGICO\"}}}",
        };
        for (String r : ruins) {
            Files.writeString(arquivo(), r);
            assertThrows(PersistenciaException.class, () -> repo().carregar(), r);
            PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
            assertEquals(MapaGamepad.padrao(), p.getAtual(), r);
            assertEquals(1, p.getAvisos().size());
            assertEquals(r, Files.readString(arquivo()), "dados preservados");
        }
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        p.salvar(personalizado());
        try (Stream<Path> s = Files.list(tmp)) {
            assertEquals(1, s.filter(f -> f.getFileName().toString().startsWith("gamepad.json.corrompido-")).count());
        }
        assertThrows(IllegalArgumentException.class, () -> p.salvar(MapaGamepad.padrao()
                .comBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.CORRER), BotaoGamepad.SUL)));
    }
}
