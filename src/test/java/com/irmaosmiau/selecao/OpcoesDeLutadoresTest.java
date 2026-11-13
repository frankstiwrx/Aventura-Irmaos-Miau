package com.irmaosmiau.selecao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ResultadoCarga;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class OpcoesDeLutadoresTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private ServicoPerfis servico() {
        return new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao());
    }

    private OpcoesDeLutadores opcoes() {
        return new OpcoesDeLutadores(CatalogoPersonagens.padrao(), servico());
    }

    private static List<String> nomes(ResultadoCarga<List<EscolhaLutador>> r) {
        return r.getValor().stream().map(EscolhaLutador::getNome).toList();
    }

    @Test
    void semPerfisSoOsOficiais() {
        assertEquals(List.of("Irmão Miau Branco"), nomes(opcoes().paraBase("miau_branco")));
        assertEquals(List.of("Irmão Miau Preto"), nomes(opcoes().paraBase("miau_preto")));
        assertTrue(new OpcoesDeLutadores(CatalogoPersonagens.padrao(), null).paraBase("miau_branco").getAvisos().isEmpty());
    }

    @Test
    void perfisAparecemNoSlotDaSuaBaseDepoisDoOficial() {
        ServicoPerfis s = servico();
        s.criar("zeca", "Zeca", "miau_branco", new AtributosLutador(30, 30, 30, 30, 30, 30, 30, 70));
        s.criarAPartirDoPersonagem("ana", "Ana", "miau_branco");
        s.criarAPartirDoPersonagem("pretinho", "Pretinho", "miau_preto");

        ResultadoCarga<List<EscolhaLutador>> branco = opcoes().paraBase("miau_branco");
        assertEquals(List.of("Irmão Miau Branco", "Ana", "Zeca"), nomes(branco));
        assertTrue(branco.getValor().get(0).isOficial());
        assertEquals(30.0, branco.getValor().get(2).getAtributos().getForca());

        assertEquals(List.of("Irmão Miau Preto", "Pretinho"), nomes(opcoes().paraBase("miau_preto")));
    }

    @Test
    void perfilCorrompidoNaoImpedeOsOficiaisEVirAviso() throws Exception {
        servico().criarAPartirDoPersonagem("bom", "Bom", "miau_preto");
        Files.writeString(tmp.resolve("perfis/ruim.json"), "{ estragado");

        ResultadoCarga<List<EscolhaLutador>> r = opcoes().paraBase("miau_preto");
        assertEquals(List.of("Irmão Miau Preto", "Bom"), nomes(r));
        assertTrue(r.getAvisos().get(0).contains("ruim.json"));
        assertEquals("{ estragado", Files.readString(tmp.resolve("perfis/ruim.json")));
    }

    @Test
    void pastaDePerfisIlegivelDeixaSoOsOficiais() throws Exception {
        Files.writeString(tmp.resolve("perfis"), "nao sou uma pasta");
        ResultadoCarga<List<EscolhaLutador>> r = opcoes().paraBase("miau_branco");
        assertEquals(List.of("Irmão Miau Branco"), nomes(r));
    }

    @Test
    void lerOpcoesNaoAlteraNenhumArquivo() throws Exception {
        servico().criarAPartirDoPersonagem("intacto", "Intacto", "miau_branco");
        Path arquivo = tmp.resolve("perfis/intacto.json");
        byte[] antes = Files.readAllBytes(arquivo);
        long data = Files.getLastModifiedTime(arquivo).toMillis();

        for (int i = 0; i < 5; i++) {
            opcoes().paraBase("miau_branco").getValor().get(1).criarLutador(0, 150, 0.7);
        }

        assertTrue(java.util.Arrays.equals(antes, Files.readAllBytes(arquivo)));
        assertEquals(data, Files.getLastModifiedTime(arquivo).toMillis());
        try (java.util.stream.Stream<Path> s = Files.list(tmp.resolve("perfis"))) {
            assertEquals(1, s.count(), "nada duplicado");
        }
    }
}
