package com.irmaosmiau.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.LimitesAtributos;
import com.irmaosmiau.personagens.PerfilLutador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class PerfisTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private CatalogoPersonagens catalogo;
    private RepositorioPerfis repo;
    private ServicoPerfis servico;

    /** Branco oficial com forca 90 e peso 80. */
    private static final AtributosLutador BRANCO_FORTE = new AtributosLutador(35, 25, 90, 22, 25, 25, 22, 80);

    @BeforeEach
    void preparar() {
        catalogo = CatalogoPersonagens.padrao();
        repo = new RepositorioPerfis(new DiretorioDados(tmp));
        servico = new ServicoPerfis(repo, catalogo);
    }

    // =====================================================
    // LIMITES
    // =====================================================
    @Test
    void limitesCobremOsOitoAtributosReaisEAceitamOsOficiais() {
        assertEquals(List.of("condicionamento", "folego", "forca", "velocidade",
                "agilidade", "tecnica", "recuperacao", "peso"),
                LimitesAtributos.todos().stream().map(LimitesAtributos.Limite::getNome).toList());
        assertTrue(LimitesAtributos.problemas(new IrmaoMiauBranco(0, 0, 0.7).getAtributos()).isEmpty());
        assertTrue(LimitesAtributos.problemas(new IrmaoMiauPreto(0, 0, 0.7).getAtributos()).isEmpty());
    }

    @Test
    void atributosInvalidosListamTodosOsProblemas() {
        AtributosLutador ruim = new AtributosLutador(0, 25, 101, 22, 25, 25, Double.NaN, 10);

        String msg = assertThrows(IllegalArgumentException.class,
                () -> servico.criar("ruim", "Ruim", "miau_branco", ruim)).getMessage();

        assertTrue(msg.contains("condicionamento = 0 fora do intervalo [1, 100]"), msg);
        assertTrue(msg.contains("forca = 101"), msg);
        assertTrue(msg.contains("recuperacao invalido"), msg);
        assertTrue(msg.contains("peso = 10 fora do intervalo [40, 120]"), msg);
        assertFalse(repo.existe("ruim"), "nada foi gravado");
    }

    // =====================================================
    // CRIAR / CARREGAR
    // =====================================================
    @Test
    void criaAPartirDoPersonagemCopiandoOsAtributosOficiais() {
        PerfilLutador p = servico.criarAPartirDoPersonagem("meu_preto", "Meu Preto", "miau_preto");

        assertEquals("miau_preto", p.getIdPersonagemBase());
        assertTrue(PerfilLutador.mesmosValores(new IrmaoMiauPreto(0, 0, 0.7).getAtributos(), p.getAtributos()));
        assertEquals(p, servico.carregar("meu_preto").orElseThrow());
        assertTrue(Files.exists(tmp.resolve("perfis/meu_preto.json")));
    }

    @Test
    void criaComAtributosPersonalizadosESalvaNoFormatoVersionado() throws IOException {
        servico.criar("Branco_Forte", "  Branco Forte  ", "MIAU_BRANCO", BRANCO_FORTE);

        PerfilLutador lido = new RepositorioPerfis(new DiretorioDados(tmp)).carregar("branco_forte").orElseThrow();
        assertEquals("branco_forte", lido.getId());
        assertEquals("Branco Forte", lido.getNome());
        assertEquals("miau_branco", lido.getIdPersonagemBase());
        assertEquals(90.0, lido.getAtributos().getForca());
        assertEquals(80.0, lido.getAtributos().getPeso());

        String json = Files.readString(tmp.resolve("perfis/branco_forte.json"));
        assertTrue(json.contains("\"tipo\": \"PERFIL_LUTADOR_IRMAOS_MIAU\""), json);
        assertTrue(json.contains("\"versaoEsquema\": 1"), json);
        assertTrue(json.contains("\"forca\": 90.0"), json);
    }

    @Test
    void conflitosEIdsInvalidos() {
        servico.criarAPartirDoPersonagem("perfil_a", "A", "miau_branco");

        assertEquals(PersistenciaException.Motivo.CONFLITO, assertThrows(PersistenciaException.class,
                () -> servico.criarAPartirDoPersonagem("PERFIL_A", "Outro", "miau_preto")).getMotivo());
        assertEquals(PersistenciaException.Motivo.CONFLITO, assertThrows(PersistenciaException.class,
                () -> servico.criarAPartirDoPersonagem("miau_branco", "Falso", "miau_branco")).getMotivo());
        assertThrows(IllegalArgumentException.class,
                () -> servico.criarAPartirDoPersonagem("gato_novo", "X", "miau_azul"));
        for (String ruim : new String[]{"../fora", "a", "com espaco", "x/y", "", "_comeca", "acentuação"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> servico.criarAPartirDoPersonagem(ruim, "X", "miau_branco"), ruim);
        }
        assertThrows(IllegalArgumentException.class,
                () -> servico.criarAPartirDoPersonagem("nome_vazio", "  ", "miau_branco"));
        assertThrows(IllegalArgumentException.class,
                () -> servico.criarAPartirDoPersonagem("nome_longo", "x".repeat(41), "miau_branco"));
    }

    @Test
    void perfilInexistenteEArquivoAusente() {
        assertTrue(servico.carregar("nao_existe").isEmpty());
        assertTrue(servico.listar().getValor().isEmpty());
        assertFalse(servico.excluir("nao_existe"));
        assertEquals(PersistenciaException.Motivo.CONFLITO, assertThrows(PersistenciaException.class,
                () -> servico.renomear("nao_existe", "X")).getMotivo());
        assertEquals(PersistenciaException.Motivo.CONFLITO, assertThrows(PersistenciaException.class,
                () -> repo.atualizar(new PerfilLutador("nao_existe", "X", "miau_branco", BRANCO_FORTE))).getMotivo());
    }

    // =====================================================
    // ATUALIZAR / LISTAR / EXCLUIR
    // =====================================================
    @Test
    void atualizaAtributosENomePersistindo() {
        servico.criarAPartirDoPersonagem("treino", "Treino", "miau_branco");

        servico.atualizarAtributos("treino", BRANCO_FORTE);
        servico.renomear("treino", "Treino Pesado");

        PerfilLutador p = servico.carregar("treino").orElseThrow();
        assertEquals("Treino Pesado", p.getNome());
        assertEquals(90.0, p.getAtributos().getForca());

        assertThrows(IllegalArgumentException.class, () -> servico.atualizarAtributos("treino",
                new AtributosLutador(35, 25, 500, 22, 25, 25, 22, 72)));
        assertEquals(90.0, servico.carregar("treino").orElseThrow().getAtributos().getForca(),
                "atualizacao invalida nao alterou o arquivo");
    }

    @Test
    void listaOrdenadaEExclusao() {
        servico.criarAPartirDoPersonagem("zeta", "Z", "miau_preto");
        servico.criarAPartirDoPersonagem("alfa", "A", "miau_branco");
        servico.criarAPartirDoPersonagem("meio", "M", "miau_branco");

        assertEquals(List.of("alfa", "meio", "zeta"),
                servico.listar().getValor().stream().map(PerfilLutador::getId).toList());

        assertTrue(servico.excluir("meio"));
        assertFalse(servico.excluir("meio"));
        assertEquals(List.of("alfa", "zeta"),
                servico.listar().getValor().stream().map(PerfilLutador::getId).toList());
        assertFalse(Files.exists(tmp.resolve("perfis/meio.json")));
    }

    @Test
    void arquivosComProblemaAparecemNosAvisosESaoPreservados() throws IOException {
        servico.criarAPartirDoPersonagem("bom", "Bom", "miau_branco");
        Path pasta = tmp.resolve("perfis");
        Files.writeString(pasta.resolve("corrompido.json"), "{ quebrado");
        Files.writeString(pasta.resolve("futuro.json"),
                "{\"tipo\": \"PERFIL_LUTADOR_IRMAOS_MIAU\", \"versaoEsquema\": 9}");
        Files.writeString(pasta.resolve("trocado.json"),
                Files.readString(pasta.resolve("bom.json")));
        Files.writeString(pasta.resolve("base_sumiu.json"), Files.readString(pasta.resolve("bom.json"))
                .replace("\"bom\"", "\"base_sumiu\"").replace("miau_branco", "miau_cinza"));

        ResultadoCarga<List<PerfilLutador>> r = servico.listar();

        assertEquals(List.of("base_sumiu", "bom"), r.getValor().stream().map(PerfilLutador::getId).toList());
        String avisos = String.join("\n", r.getAvisos());
        assertTrue(avisos.contains("corrompido.json"), avisos);
        assertTrue(avisos.contains("futuro.json"), avisos);
        assertTrue(avisos.contains("trocado.json: id interno \"bom\""), avisos);
        assertTrue(avisos.contains("personagem-base \"miau_cinza\" nao existe mais"), avisos);
        assertEquals("{ quebrado", Files.readString(pasta.resolve("corrompido.json")), "nada foi apagado");

        assertEquals(PersistenciaException.Motivo.ARQUIVO_CORROMPIDO, assertThrows(PersistenciaException.class,
                () -> servico.carregar("corrompido")).getMotivo());
        assertEquals(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL, assertThrows(PersistenciaException.class,
                () -> servico.carregar("futuro")).getMotivo());
    }

    @Test
    void atributoFaltandoOuExtraNoArquivoEDadoInvalido() throws IOException {
        servico.criarAPartirDoPersonagem("teste", "T", "miau_branco");
        Path arquivo = tmp.resolve("perfis/teste.json");
        String original = Files.readString(arquivo);

        Files.writeString(arquivo, original.replaceAll("\"peso\": [0-9.]+", "\"pesoo\": 72.0"));
        PersistenciaException ex = assertThrows(PersistenciaException.class, () -> servico.carregar("teste"));
        assertEquals(PersistenciaException.Motivo.DADOS_INVALIDOS, ex.getMotivo());
        assertTrue(ex.getMessage().contains("pesoo"), ex.getMessage());

        Files.writeString(arquivo, original.replace("\"forca\": 42.0", "\"forca\": 999.0"));
        assertTrue(assertThrows(PersistenciaException.class, () -> servico.carregar("teste"))
                .getMessage().contains("forca = 999"));
    }

    // =====================================================
    // INDEPENDENCIA ENTRE OFICIAIS E PERSONALIZADOS
    // =====================================================
    @Test
    void perfisNuncaAlteramOsPersonagensOficiais() {
        AtributosLutador oficialAntes = catalogo.exigir("miau_branco").getAtributosPadrao();
        double forcaOficial = oficialAntes.getForca();

        PerfilLutador p = servico.criarAPartirDoPersonagem("copia", "Copia", "miau_branco");
        servico.atualizarAtributos("copia", BRANCO_FORTE);

        // O perfil devolve copias: mexer nelas e impossivel (AtributosLutador e imutavel)
        // e cada chamada devolve um objeto novo.
        assertNotSame(p.getAtributos(), p.getAtributos());
        assertNotSame(oficialAntes, servico.carregar("copia").orElseThrow().getAtributos());

        // Catalogo usado pelo servico, catalogo novo e o construtor real: todos iguais ao oficial.
        assertEquals(forcaOficial, catalogo.exigir("miau_branco").getAtributosPadrao().getForca());
        assertEquals(42.0, CatalogoPersonagens.padrao().exigir("miau_branco").getAtributosPadrao().getForca());
        assertEquals(42.0, new IrmaoMiauBranco(0, 0, 0.7).getAtributos().getForca());
        assertEquals(72.0, new IrmaoMiauBranco(0, 0, 0.7).getAtributos().getPeso());
        assertTrue(PerfilLutador.mesmosValores(oficialAntes, servico.atributosOficiais(p)));
        assertEquals(2, catalogo.quantidade(), "perfis nao entram no catalogo oficial");

        // E nenhum arquivo foi criado fora da pasta de perfis.
        assertTrue(Files.exists(tmp.resolve("perfis/copia.json")));
    }
}
