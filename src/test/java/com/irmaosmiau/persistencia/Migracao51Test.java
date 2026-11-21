package com.irmaosmiau.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5O.1: arquivos salvos na 5O continuam abrindo (padroes atualizados, acrobacia obsoleta ignorada). */
class Migracao51Test {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private static String controlesJson(MapaControles m, boolean comAcrobacia) {
        StringBuilder sb = new StringBuilder("{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{");
        boolean primeiro = true;
        for (SlotJogador s : SlotJogador.values()) {
            sb.append(primeiro ? "" : ",").append('"').append(s.getIdArquivo()).append("\":{");
            primeiro = false;
            boolean p2 = true;
            for (Map.Entry<AcaoJogador, TeclaFisica> e : m.doSlot(s).entrySet()) {
                sb.append(p2 ? "" : ",").append('"').append(e.getKey().name()).append("\":\"")
                        .append(e.getValue().getNome()).append('"');
                p2 = false;
            }
            if (comAcrobacia) {
                sb.append(",\"ACROBACIA\":\"").append(s == SlotJogador.JOGADOR_1 ? "9" : "NUMPAD5").append('"');
            }
            sb.append('}');
        }
        return sb.append("}}").toString();
    }

    @Test
    void controlesPadraoDa5OViramOsPadroesNovos() throws Exception {
        Files.writeString(tmp.resolve(RepositorioControles.NOME_ARQUIVO), controlesJson(MapaControles.padrao5O(), true),
                StandardCharsets.UTF_8);
        RepositorioControles.Leitura l = new RepositorioControles(new DiretorioDados(tmp)).carregar();
        assertEquals(MapaControles.padrao(), l.mapa());
        assertTrue(l.avisos().stream().anyMatch(a -> a.startsWith("J1: teclas padrão atualizadas")), l.avisos().toString());
    }

    @Test
    void j1PersonalizadoPerdeSoAsTeclasDoTecladoControle() throws Exception {
        MapaControles personalizado = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.PULAR, TeclaFisica.porNome("BACK_SPACE").orElseThrow());
        String json = controlesJson(personalizado, false).replace("\"INICIAR_PEGADA\":\"2\"", "\"INICIAR_PEGADA\":\"W\"");
        Files.writeString(tmp.resolve(RepositorioControles.NOME_ARQUIVO), json, StandardCharsets.UTF_8);
        RepositorioControles.Leitura l = new RepositorioControles(new DiretorioDados(tmp)).carregar();
        assertEquals("BACKSPACE", l.mapa().rotulo(SlotJogador.JOGADOR_1, AcaoJogador.PULAR), "personalizacao mantida");
        assertEquals("—", l.mapa().rotulo(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA),
                "W e do teclado-controle do J2: a pegada fica sem tecla ate o jogador escolher");
        assertTrue(l.avisos().stream().anyMatch(a -> a.contains("teclado-controle do J2")), l.avisos().toString());
    }

    @Test
    void gamepadPadraoDa5OComAcrobaciaViraOPadraoNovo() throws Exception {
        StringBuilder botoes = new StringBuilder();
        MapaGamepad.ConfigSlot c = MapaGamepad.padrao5O(SlotJogador.JOGADOR_1, EsquemaGamepad.CLASSICO);
        c.acoes().forEach((a, b) -> botoes.append(",\"").append(a.name()).append("\":\"").append(b.name()).append('"'));
        c.comandos().forEach((k, b) -> botoes.append(",\"").append(k.name()).append("\":\"").append(b.name()).append('"'));
        botoes.append(",\"ACROBACIA\":\"NORTE\"");
        String json = "{\"tipo\":\"" + RepositorioGamepad.TIPO + "\",\"versaoEsquema\":" + RepositorioGamepad.VERSAO_ESQUEMA
                + ",\"jogadores\":{\"jogador1\":{\"esquema\":\"CLASSICO\",\"botoes\":{" + botoes.substring(1) + "}}}}";
        Files.writeString(tmp.resolve(RepositorioGamepad.NOME_ARQUIVO), json, StandardCharsets.UTF_8);
        MapaGamepad m = new RepositorioGamepad(new DiretorioDados(tmp)).carregar();
        // 5O.2 (pedido): Classico padrao nunca escolhido vira o novo padrao (Contextual).
        assertEquals(MapaGamepad.padrao(), m);
        // complemento 5Q.8 (pedido): respirar saiu do RT sozinho e foi para o Y (o Select continua livre)
        assertTrue(m.quemUsa(SlotJogador.JOGADOR_1, BotaoGamepad.VOLTAR).isEmpty(), "Select livre");
        assertEquals(BotaoGamepad.NORTE, m.botao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.RESPIRAR)).orElseThrow());
        assertTrue(m.selectValeComoRb(SlotJogador.JOGADOR_1));
        assertEquals(BotaoGamepad.NORTE, MapaGamepad.classico().botao(SlotJogador.JOGADOR_1,
                ItemGamepad.de(AcaoJogador.REFORCAR_PEGADA)).orElseThrow(), "o reforco no Y (Classico) continua");
    }
}
