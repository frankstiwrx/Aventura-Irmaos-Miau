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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Complemento da 5Q.8: o respirar saiu do RT sozinho e foi para o Y. O gamepad.json padrao de antes vira o
 * padrao novo; um personalizado de antes (sem botao de respirar) ganha o Y se ele estiver livre; um arquivo
 * salvo depois desta versao fica exatamente como o jogador escolheu.
 */
class RespirarNoY5Q8PersistenciaTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private RepositorioGamepad repo() {
        return new RepositorioGamepad(new DiretorioDados(tmp));
    }

    private void gravarAntigo(MapaGamepad.ConfigSlot c, boolean escolhido) throws Exception {
        StringBuilder b = new StringBuilder();
        c.acoes().forEach((a, x) -> b.append(",\"").append(a.name()).append("\":\"").append(x.name()).append('"'));
        c.comandos().forEach((k, x) -> b.append(",\"").append(k.name()).append("\":\"").append(x.name()).append('"'));
        String json = "{\"tipo\":\"" + RepositorioGamepad.TIPO + "\",\"versaoEsquema\":" + RepositorioGamepad.VERSAO_ESQUEMA
                + ",\"jogadores\":{\"jogador1\":{\"esquema\":\"" + c.esquema().name() + "\""
                + (escolhido ? ",\"esquemaEscolhido\":true" : "") + ",\"botoes\":{" + b.substring(1) + "}}}}";
        Files.writeString(tmp.resolve(RepositorioGamepad.NOME_ARQUIVO), json, StandardCharsets.UTF_8);
    }

    @Test
    void padraoDeAntesViraOPadraoNovoComAviso() throws Exception {
        gravarAntigo(MapaGamepad.padrao51(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL), true);
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertEquals(MapaGamepad.padrao().config(SlotJogador.JOGADOR_1), p.getAtual().config(SlotJogador.JOGADOR_1));
        assertTrue(p.getAvisos().stream().anyMatch(a -> a.contains("Y/Triângulo")), p.getAvisos().toString());
    }

    @Test
    void personalizadoDeAntesGanhaOYSeEstiverLivre() throws Exception {
        MapaGamepad.ConfigSlot meu = MapaGamepad.padrao().semBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.RESPIRAR))
                .comBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.CORRER), BotaoGamepad.VOLTAR)
                .config(SlotJogador.JOGADOR_1);
        gravarAntigo(meu, true);
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertEquals(BotaoGamepad.NORTE, p.getAtual().botao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.RESPIRAR))
                .orElseThrow(), "o Y passa a respirar");
        assertEquals(BotaoGamepad.VOLTAR, p.getAtual().botao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.CORRER))
                .orElseThrow(), "o resto do personalizado fica");
        assertTrue(p.getAvisos().stream().anyMatch(a -> a.contains("respirar agora é no Y")), p.getAvisos().toString());
    }

    @Test
    void personalizadoDeAntesComOYOcupadoAvisaSemTrocarNada() throws Exception {
        MapaGamepad.ConfigSlot meu = MapaGamepad.padrao().semBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.RESPIRAR))
                .comBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(ComandoGamepad.TECNICA_CIMA), BotaoGamepad.NORTE)
                .config(SlotJogador.JOGADOR_1);
        gravarAntigo(meu, true);
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertEquals(meu, p.getAtual().config(SlotJogador.JOGADOR_1), "nada sobrescrito");
        assertTrue(p.getAvisos().stream().anyMatch(a -> a.contains("atribua RESPIRAR")), p.getAvisos().toString());
    }

    @Test
    void salvoDepoisFicaComoOJogadorEscolheu() throws Exception {
        MapaGamepad sem = MapaGamepad.padrao().semBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.RESPIRAR));
        repo().salvar(sem);
        assertTrue(Files.readString(tmp.resolve(RepositorioGamepad.NOME_ARQUIVO)).contains(RepositorioGamepad.CAMPO_RESPIRAR_Y));
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertEquals(sem, p.getAtual(), "sem migracao depois de salvo");
        assertTrue(p.getAvisos().isEmpty(), p.getAvisos().toString());
    }
}
