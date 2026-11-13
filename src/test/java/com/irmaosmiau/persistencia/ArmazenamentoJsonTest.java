package com.irmaosmiau.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.conteudo.json.EscritorJson;
import com.irmaosmiau.conteudo.json.LeitorJson;
import com.irmaosmiau.conteudo.json.ObjetoJson;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class ArmazenamentoJsonTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private static Map<String, Object> dados(Object valor) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valor", valor);
        return m;
    }

    private static List<String> arquivosNa(Path pasta) throws IOException {
        try (Stream<Path> s = Files.list(pasta)) {
            return s.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    // =====================================================
    // ESCRITOR JSON
    // =====================================================
    @Test
    void escritorGeraJsonQueOLeitorDoProjetoLe() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("texto", "ção \"aspas\" \\ \n fim");
        m.put("inteiro", 42);
        m.put("decimal", 0.6);
        m.put("bool", true);
        m.put("nulo", null);
        m.put("lista", List.of(1, "a", List.of()));
        m.put("objeto", Map.of());

        Object lido = LeitorJson.ler(EscritorJson.escrever(m));

        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) lido;
        assertEquals("ção \"aspas\" \\ \n fim", r.get("texto"));
        assertEquals(42L, r.get("inteiro"));
        assertEquals(0.6, r.get("decimal"));
        assertEquals(true, r.get("bool"));
        assertTrue(r.containsKey("nulo"));
        assertEquals(3, ((List<?>) r.get("lista")).size());
    }

    @Test
    void escritorRecusaObjetosNaoSimples() {
        assertThrows(IllegalArgumentException.class, () -> EscritorJson.escrever(dados(Color.RED)));
        assertThrows(IllegalArgumentException.class, () -> EscritorJson.escrever(dados(new javax.swing.JPanel())));
        assertThrows(IllegalArgumentException.class, () -> EscritorJson.escrever(dados(Double.NaN)));
    }

    // =====================================================
    // GRAVAR / LER
    // =====================================================
    @Test
    void gravaCriandoPastasELeDeVolta() {
        ArmazenamentoJson a = new ArmazenamentoJson();
        Path arquivo = tmp.resolve("a/b/c/doc.json");

        a.gravar(arquivo, "TESTE", 1, dados("ola"));

        assertTrue(Files.exists(arquivo));
        ObjetoJson doc = a.ler(arquivo, "TESTE", 1).orElseThrow();
        assertEquals("ola", doc.texto("valor"));
        assertEquals(1, doc.inteiro("versaoEsquema"));
    }

    @Test
    void arquivoInexistenteDevolveVazioSemCriarNada() {
        ArmazenamentoJson a = new ArmazenamentoJson();
        assertTrue(a.ler(tmp.resolve("nao/existe.json"), "TESTE", 1).isEmpty());
        assertFalse(Files.exists(tmp.resolve("nao")));
    }

    @Test
    void jsonCorrompidoEDetectadoEOArquivoNaoEAlterado() throws IOException {
        Path arquivo = tmp.resolve("doc.json");
        Files.writeString(arquivo, "{ \"tipo\": \"TESTE\", ", StandardCharsets.UTF_8);

        PersistenciaException ex = assertThrows(PersistenciaException.class,
                () -> new ArmazenamentoJson().ler(arquivo, "TESTE", 1));

        assertEquals(PersistenciaException.Motivo.ARQUIVO_CORROMPIDO, ex.getMotivo());
        assertTrue(ex.getMessage().contains("doc.json"), ex.getMessage());
        assertEquals("{ \"tipo\": \"TESTE\", ", Files.readString(arquivo));
    }

    @Test
    void tipoOuVersaoIncompativeis() throws IOException {
        ArmazenamentoJson a = new ArmazenamentoJson();
        Path arquivo = tmp.resolve("doc.json");

        Files.writeString(arquivo, "{\"tipo\": \"OUTRO\", \"versaoEsquema\": 1}");
        assertEquals(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL,
                assertThrows(PersistenciaException.class, () -> a.ler(arquivo, "TESTE", 1)).getMotivo());

        Files.writeString(arquivo, "{\"tipo\": \"TESTE\"}");
        assertTrue(assertThrows(PersistenciaException.class, () -> a.ler(arquivo, "TESTE", 1))
                .getMessage().contains("versaoEsquema"));

        Files.writeString(arquivo, "{\"tipo\": \"TESTE\", \"versaoEsquema\": 7}");
        PersistenciaException futura = assertThrows(PersistenciaException.class, () -> a.ler(arquivo, "TESTE", 1));
        assertEquals(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL, futura.getMotivo());
        assertTrue(futura.getMessage().contains("versao mais nova do jogo"), futura.getMessage());

        Files.writeString(arquivo, "{\"tipo\": \"TESTE\", \"versaoEsquema\": 0}");
        assertEquals(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL,
                assertThrows(PersistenciaException.class, () -> a.ler(arquivo, "TESTE", 1)).getMotivo());

        Files.writeString(arquivo, "{\"tipo\": 5, \"versaoEsquema\": 1}");
        assertEquals(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL,
                assertThrows(PersistenciaException.class, () -> a.ler(arquivo, "TESTE", 1)).getMotivo());
    }

    // =====================================================
    // GRAVACAO SEGURA
    // =====================================================
    @Test
    void quedaAntesDaSubstituicaoPreservaOArquivoAntigoENaoDeixaTemporario() throws IOException {
        ArmazenamentoJson a = new ArmazenamentoJson();
        Path arquivo = tmp.resolve("doc.json");
        a.gravar(arquivo, "TESTE", 1, dados("versao boa"));
        String antes = Files.readString(arquivo);

        a.setAntesDeSubstituirParaTestes(() -> {
            throw new IllegalStateException("simulando queda de energia");
        });
        PersistenciaException ex = assertThrows(PersistenciaException.class,
                () -> a.gravar(arquivo, "TESTE", 1, dados("versao nova")));

        assertEquals(PersistenciaException.Motivo.ERRO_DE_ARQUIVO, ex.getMotivo());
        assertTrue(ex.getMessage().contains("preservado"), ex.getMessage());
        assertEquals(antes, Files.readString(arquivo));
        assertEquals(List.of("doc.json"), arquivosNa(tmp), "nenhum .tmp sobrando");
    }

    @Test
    void dadoNaoSerializavelNaoTocaNoDisco() throws IOException {
        ArmazenamentoJson a = new ArmazenamentoJson();
        Path arquivo = tmp.resolve("doc.json");
        a.gravar(arquivo, "TESTE", 1, dados("ok"));
        String antes = Files.readString(arquivo);

        assertThrows(IllegalArgumentException.class, () -> a.gravar(arquivo, "TESTE", 1, dados(Double.NaN)));

        assertEquals(antes, Files.readString(arquivo));
        assertEquals(List.of("doc.json"), arquivosNa(tmp));
    }

    @Test
    void regravarSubstituiOConteudoInteiro() throws IOException {
        ArmazenamentoJson a = new ArmazenamentoJson();
        Path arquivo = tmp.resolve("doc.json");
        a.gravar(arquivo, "TESTE", 1, dados("um texto bem comprido para o primeiro arquivo"));
        a.gravar(arquivo, "TESTE", 1, dados("curto"));

        assertEquals("curto", a.ler(arquivo, "TESTE", 1).orElseThrow().texto("valor"));
        assertEquals(List.of("doc.json"), arquivosNa(tmp));
    }

    @Test
    void camposReservadosNaoPodemSerSobrescritos() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("versaoEsquema", 99);
        assertThrows(IllegalArgumentException.class,
                () -> new ArmazenamentoJson().gravar(tmp.resolve("x.json"), "TESTE", 1, m));
    }

    @Test
    void arquivoProblematicoEGuardadoEmVezDeApagado() throws IOException {
        Path arquivo = tmp.resolve("configuracoes.json");
        Files.writeString(arquivo, "lixo");

        Path copia = new ArmazenamentoJson().guardarComoCorrompido(arquivo);

        assertFalse(Files.exists(arquivo));
        assertTrue(copia.getFileName().toString().startsWith("configuracoes.json.corrompido-"));
        assertEquals("lixo", Files.readString(copia));
    }

    // =====================================================
    // DIRETORIO DE DADOS
    // =====================================================
    @Test
    void diretorioPadraoEPersonalizado() {
        String antes = System.getProperty(DiretorioDados.PROPRIEDADE_PASTA);
        try {
            System.clearProperty(DiretorioDados.PROPRIEDADE_PASTA);
            Path padrao = DiretorioDados.doUsuario().getRaiz();
            assertTrue(padrao.endsWith(".aventura-irmaos-miau"), padrao.toString());

            System.setProperty(DiretorioDados.PROPRIEDADE_PASTA, tmp.toString());
            assertEquals(tmp.toAbsolutePath().normalize(), DiretorioDados.doUsuario().getRaiz());
        } finally {
            if (antes == null) {
                System.clearProperty(DiretorioDados.PROPRIEDADE_PASTA);
            } else {
                System.setProperty(DiretorioDados.PROPRIEDADE_PASTA, antes);
            }
        }

        DiretorioDados d = new DiretorioDados(tmp);
        assertEquals(tmp.resolve("perfis").toAbsolutePath().normalize(), d.resolver("perfis"));
        assertThrows(IllegalArgumentException.class, () -> d.resolver("../fora"));
    }
}
