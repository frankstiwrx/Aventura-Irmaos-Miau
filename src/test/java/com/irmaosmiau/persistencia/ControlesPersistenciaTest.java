package com.irmaosmiau.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.ProblemaControle;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;
import com.irmaosmiau.preferencias.PreferenciasControles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5C: controles.json (tipo e versao proprios), sempre em pasta de teste. */
class ControlesPersistenciaTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private RepositorioControles repo() {
        return new RepositorioControles(new DiretorioDados(tmp));
    }

    private Path arquivo() {
        return tmp.resolve(RepositorioControles.NOME_ARQUIVO);
    }

    private void escrever(String json) throws Exception {
        Files.writeString(arquivo(), json, StandardCharsets.UTF_8);
    }

    private static TeclaFisica t(String nome) {
        return TeclaFisica.porNome(nome).orElseThrow();
    }

    private static MapaControles remapeado() {
        return MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA, t("P"))
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, t("CONTROL"))
                .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.ROMPIMENTO_TECNICO, t("NUMPAD0"));
    }

    private long copiasCorrompidas() throws Exception {
        try (Stream<Path> s = Files.list(tmp)) {
            return s.filter(p -> p.getFileName().toString().startsWith("controles.json.corrompido-")).count();
        }
    }

    @Test
    void carregarSemArquivoUsaPadroesENaoCriaNada() throws Exception {
        PreferenciasControles p = PreferenciasControles.carregar(repo());
        assertEquals(MapaControles.padrao(), p.getAtual());
        assertTrue(p.getAvisos().isEmpty());
        assertFalse(Files.exists(arquivo()));
        try (Stream<Path> s = Files.list(tmp)) {
            assertEquals(0, s.count(), "nada criado so por abrir");
        }
    }

    @Test
    void persistenciaDepoisDeReiniciar() throws Exception {
        PreferenciasControles p = PreferenciasControles.carregar(repo());
        p.salvar(remapeado());
        assertEquals(remapeado(), p.getAtual());

        String json = Files.readString(arquivo());
        assertTrue(json.contains("\"CONTROLES_IRMAOS_MIAU\""), json);
        assertTrue(json.contains("\"versaoEsquema\""), json);
        assertTrue(json.contains("\"INICIAR_PEGADA\": \"P\"") || json.contains("\"INICIAR_PEGADA\":\"P\""), json);
        assertFalse(Files.exists(tmp.resolve(RepositorioConfiguracoes.NOME_ARQUIVO)),
                "configuracoes.json nao e tocado");

        PreferenciasControles reiniciado = PreferenciasControles.carregar(repo());
        assertEquals(remapeado(), reiniciado.getAtual());
        assertTrue(reiniciado.getAvisos().isEmpty());
    }

    @Test
    void restaurarPadroesESalvar() throws Exception {
        PreferenciasControles p = PreferenciasControles.carregar(repo());
        p.salvar(remapeado());
        p.salvar(MapaControles.padrao());
        assertEquals(MapaControles.padrao(), PreferenciasControles.carregar(repo()).getAtual());
    }

    @Test
    void arquivoCorrompidoPreservadoAteSalvar() throws Exception {
        escrever("{ isto nao e json");
        PreferenciasControles p = PreferenciasControles.carregar(repo());
        assertEquals(MapaControles.padrao(), p.getAtual());
        assertEquals(1, p.getAvisos().size());
        assertTrue(p.getAvisos().get(0).contains("controles padrão"), p.getAvisos().get(0));
        assertEquals("{ isto nao e json", Files.readString(arquivo()), "arquivo intacto");

        p.salvar(remapeado());
        assertEquals(1, copiasCorrompidas(), "o arquivo ruim vira copia");
        assertEquals(remapeado(), PreferenciasControles.carregar(repo()).getAtual());
    }

    @Test
    void arquivosInvalidosNaoSaoAceitos() throws Exception {
        String[] invalidos = {
            // conflito: mesma tecla em duas acoes (no mesmo jogador e entre os dois)
            "{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                    + "{\"PULAR\":\"A\",\"MOVER_ESQUERDA\":\"A\"}}}",
            "{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                    + "{\"PULAR\":\"P\"},\"jogador2\":{\"PULAR\":\"P\"}}}",
            // tecla reservada
            "{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                    + "{\"PULAR\":\"ESCAPE\"}}}",
            // tecla desconhecida
            "{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                    + "{\"PULAR\":\"TECLA_MAGICA\"}}}",
            // acao desconhecida / acao que o slot nao usa
            "{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                    + "{\"VOAR\":\"P\"}}}",
            // 5I (pedido): "jogador2 INICIAR_PEGADA" saiu desta lista: agora e valido.
            // jogador desconhecido
            "{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador3\":{}}}",
            // versao futura e tipo errado
            "{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":99,\"jogadores\":{}}",
            "{\"tipo\":\"CONFIGURACOES_IRMAOS_MIAU\",\"versaoEsquema\":1}",
        };
        for (String json : invalidos) {
            escrever(json);
            assertThrows(PersistenciaException.class, () -> repo().carregar(), json);
            PreferenciasControles p = PreferenciasControles.carregar(repo());
            assertEquals(MapaControles.padrao(), p.getAtual(), json);
            assertEquals(1, p.getAvisos().size(), json);
            assertEquals(json, Files.readString(arquivo()), "dados do usuario preservados");
        }
    }

    @Test
    void arquivoAntigoSemUmaAcaoRecebeOPadraoQuandoLivre() throws Exception {
        // Arquivo "de antes" do reforco: sem REFORCAR_PEGADA no jogador1.
        escrever("{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                + "{\"INICIAR_PEGADA\":\"P\"}}}");
        RepositorioControles.Leitura l = repo().carregar();
        assertEquals("P", l.mapa().rotulo(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA));
        assertEquals("Y", l.mapa().rotulo(SlotJogador.JOGADOR_1, AcaoJogador.REFORCAR_PEGADA));
        assertTrue(l.mapa().isValido());
        assertTrue(l.avisos().isEmpty());
    }

    @Test
    void acaoNovaComTeclaPadraoOcupadaFicaSemTeclaEAvisa() throws Exception {
        escrever("{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{\"jogador1\":"
                + "{\"PULAR\":\"Y\"}}}");
        PreferenciasControles p = PreferenciasControles.carregar(repo());
        MapaControles m = p.getAtual();
        assertEquals("Y", m.rotulo(SlotJogador.JOGADOR_1, AcaoJogador.PULAR));
        assertEquals("—", m.rotulo(SlotJogador.JOGADOR_1, AcaoJogador.REFORCAR_PEGADA));
        assertEquals(List.of(ProblemaControle.Tipo.SEM_TECLA), m.problemas().stream()
                .map(ProblemaControle::tipo).toList());
        assertEquals(1, p.getAvisos().size());
        assertTrue(p.getAvisos().get(0).contains("sem tecla"), p.getAvisos().get(0));
        assertThrows(IllegalArgumentException.class, () -> p.salvar(m), "nao salva com acao sem tecla");
    }

    @Test
    void mapaComConflitoNuncaEGravado() throws Exception {
        MapaControles ruim = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.PULAR, t("A"));
        assertThrows(IllegalArgumentException.class, () -> repo().salvar(ruim));
        PreferenciasControles p = PreferenciasControles.carregar(repo());
        assertThrows(IllegalArgumentException.class, () -> p.salvar(ruim));
        assertEquals(MapaControles.padrao(), p.getAtual());
        assertFalse(Files.exists(arquivo()));
    }

    @Test
    void emMemoriaNuncaTocaNoDisco() throws Exception {
        PreferenciasControles p = PreferenciasControles.emMemoria(null);
        p.salvar(remapeado());
        assertEquals(remapeado(), p.getAtual());
        assertFalse(p.isPersistente());
        try (Stream<Path> s = Files.list(tmp)) {
            assertEquals(0, s.count());
        }
    }
}
