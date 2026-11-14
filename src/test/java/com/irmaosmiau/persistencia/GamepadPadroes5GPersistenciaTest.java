package com.irmaosmiau.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5G: gamepad.json da 5F -> novos padroes so quando nunca personalizado, sempre com aviso. */
class GamepadPadroes5GPersistenciaTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private RepositorioGamepad repo() {
        return new RepositorioGamepad(new DiretorioDados(tmp));
    }

    /** Grava um arquivo como a 5F gravaria (padroes antigos de cada slot). */
    private void gravarComo5F(MapaGamepad.ConfigSlot j1, MapaGamepad.ConfigSlot j2) throws Exception {
        StringBuilder sb = new StringBuilder("{\"tipo\":\"MAPEAMENTO_GAMEPAD_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{");
        MapaGamepad.ConfigSlot[] cs = {j1, j2};
        for (int i = 0; i < 2; i++) {
            Map<String, String> b = new LinkedHashMap<>();
            cs[i].acoes().forEach((a, bt) -> b.put(a.name(), bt.name()));
            cs[i].comandos().forEach((c, bt) -> b.put(c.name(), bt.name()));
            sb.append(i == 0 ? "" : ",").append("\"jogador").append(i + 1).append("\":{\"esquema\":\"")
                    .append(cs[i].esquema().name()).append("\",\"botoes\":{");
            int k = 0;
            for (Map.Entry<String, String> e : b.entrySet()) {
                sb.append(k++ == 0 ? "" : ",").append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
            }
            sb.append("}}");
        }
        sb.append("}}");
        Files.writeString(tmp.resolve(RepositorioGamepad.NOME_ARQUIVO), sb.toString());
    }

    @Test
    void padraoAntigoNuncaPersonalizadoVaiParaONovoComAviso() throws Exception {
        gravarComo5F(MapaGamepad.padraoAnterior(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL),
                MapaGamepad.padraoAnterior(SlotJogador.JOGADOR_2, EsquemaGamepad.CLASSICO));
        String antes = Files.readString(tmp.resolve(RepositorioGamepad.NOME_ARQUIVO));
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertEquals(MapaGamepad.padraoDo(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL), p.getAtual().config(SlotJogador.JOGADOR_1));
        assertEquals(MapaGamepad.padraoDo(SlotJogador.JOGADOR_2, EsquemaGamepad.CLASSICO), p.getAtual().config(SlotJogador.JOGADOR_2));
        assertEquals(2, p.getAvisos().size(), p.getAvisos().toString());
        assertTrue(p.getAvisos().get(0).contains("atualizados"), p.getAvisos().get(0));
        assertEquals(antes, Files.readString(tmp.resolve(RepositorioGamepad.NOME_ARQUIVO)), "arquivo so muda ao salvar");
    }

    @Test
    void personalizadoEMantidoComAvisoDeNovosPadroes() throws Exception {
        MapaGamepad.ConfigSlot meu = MapaGamepad.experimental()
                .semBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.CORRER)).config(SlotJogador.JOGADOR_1);
        gravarComo5F(meu, MapaGamepad.padraoDo(SlotJogador.JOGADOR_2, EsquemaGamepad.CLASSICO));
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertEquals(meu, p.getAtual().config(SlotJogador.JOGADOR_1), "nada sobrescrito em silencio");
        assertEquals(1, p.getAvisos().size(), p.getAvisos().toString());
        assertTrue(p.getAvisos().get(0).contains("RESTAURAR PADRÕES"), p.getAvisos().get(0));
    }

    @Test
    void modificadorDoPuloAltoSalvaNoClassico() throws Exception {
        MapaGamepad m = MapaGamepad.padrao()
                .comBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO), BotaoGamepad.VOLTAR)
                .semBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.RESPIRAR));
        PreferenciasGamepad.carregar(repo()).salvar(m);
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertEquals(m, p.getAtual());
        assertTrue(p.getAvisos().isEmpty());
        assertTrue(Files.readString(tmp.resolve(RepositorioGamepad.NOME_ARQUIVO)).contains("MODIFICADOR_PULO_ALTO"));
    }
}
