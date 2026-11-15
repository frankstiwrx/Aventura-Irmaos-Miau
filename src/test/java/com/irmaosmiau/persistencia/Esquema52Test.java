package com.irmaosmiau.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5O.2: Contextual como padrao, escolha explicita respeitada, Classico selecionavel. */
class Esquema52Test {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private RepositorioGamepad repo() {
        return new RepositorioGamepad(new DiretorioDados(tmp));
    }

    @Test
    void configuracaoNovaAbreNoContextualNosDoisSlots() {
        MapaGamepad m = PreferenciasGamepad.carregar(repo()).getAtual();
        assertEquals(EsquemaGamepad.CONTEXTUAL, m.esquema(SlotJogador.JOGADOR_1));
        assertEquals(EsquemaGamepad.CONTEXTUAL, m.esquema(SlotJogador.JOGADOR_2));
        assertEquals(MapaGamepad.padrao(), m);
    }

    @Test
    void classicoEscolhidoESalvoContinuaClassico() throws Exception {
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        p.salvar(MapaGamepad.padrao().comEsquema(SlotJogador.JOGADOR_2, EsquemaGamepad.CLASSICO));
        assertTrue(Files.readString(tmp.resolve(RepositorioGamepad.NOME_ARQUIVO)).contains("esquemaEscolhido"));
        PreferenciasGamepad r = PreferenciasGamepad.carregar(repo());
        assertEquals(EsquemaGamepad.CLASSICO, r.getAtual().esquema(SlotJogador.JOGADOR_2), "escolha explicita respeitada");
        assertEquals(EsquemaGamepad.CONTEXTUAL, r.getAtual().esquema(SlotJogador.JOGADOR_1));
        assertTrue(r.getAvisos().isEmpty(), r.getAvisos().toString());
    }

    @Test
    void classicoPersonalizadoAntigoNaoEApagado() throws Exception {
        MapaGamepad meu = MapaGamepad.classico()
                .comBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.CORRER), BotaoGamepad.LB)
                .semBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO));
        repo().salvar(meu);
        // simula um arquivo antigo (sem a marca de escolha)
        Path a = tmp.resolve(RepositorioGamepad.NOME_ARQUIVO);
        Files.writeString(a, Files.readString(a).replace("\"esquemaEscolhido\": true,", "")
                .replace("\"esquemaEscolhido\":true,", ""));
        MapaGamepad lido = repo().carregar();
        assertEquals(EsquemaGamepad.CLASSICO, lido.esquema(SlotJogador.JOGADOR_1), "personalizado fica como esta");
        assertEquals(BotaoGamepad.LB, lido.botao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.CORRER)).orElseThrow());
        assertEquals(EsquemaGamepad.CONTEXTUAL, lido.esquema(SlotJogador.JOGADOR_2),
                "o J2 (Classico padrao nunca escolhido) vai para o novo padrao");
    }
}
