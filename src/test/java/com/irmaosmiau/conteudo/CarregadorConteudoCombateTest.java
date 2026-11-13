package com.irmaosmiau.conteudo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.AcaoCombate;
import com.irmaosmiau.combate.AnimacaoConjuntaQuadros;
import com.irmaosmiau.combate.AssercoesAcao;
import com.irmaosmiau.combate.CategoriaAcao;
import com.irmaosmiau.combate.DisponibilidadeAcao;
import com.irmaosmiau.combate.RegistroAcoes;
import com.irmaosmiau.combate.RegistroAnimacoesCombate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CarregadorConteudoCombateTest {

    private static final String CAT_ANIM_TESTE = "/conteudo-teste/animacoes.json";
    private static final String CAT_TEC_TESTE = "/conteudo-teste/tecnicas.json";

    // =====================================================
    // CONTEUDO PADRAO DO JOGO
    // =====================================================
    @Test
    void conteudoPadraoCarregaAPegada1IdenticaAoLegado() {
        ConteudoCombate c = CarregadorConteudoCombate.carregarPadrao();

        AnimacaoConjuntaQuadros json = c.getRegistroAnimacoes().exigir("pegada_1_entrada");
        AnimacaoConjuntaQuadros legado = new RegistroAnimacoesCombate().buscar("pegada_1_entrada");
        assertEquals(1, c.getRegistroAnimacoes().quantidade());
        assertEquals(30, json.getDuracaoTotalFrames());
        assertFalse(json.possuiDistribuicaoTemporal());
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(legado.copiarQuadroAplicador(i), json.copiarQuadroAplicador(i));
            assertArrayEquals(legado.copiarQuadroReceptor(i), json.copiarQuadroReceptor(i));
        }

        AssercoesAcao.assertMesmasRegras(
                new RegistroAcoes().buscar("pegada_1"),
                c.getRegistroAcoes().buscar("pegada_1"));
    }

    @Test
    void conteudoPadraoTemQuatroPegadasESoAPrimeiraEJogavel() {
        RegistroAcoes tecnicas = CarregadorConteudoCombate.carregarPadrao().getRegistroAcoes();

        assertEquals(List.of("pegada_1", "pegada_2", "pegada_3", "pegada_4"),
                tecnicas.listar().stream().map(AcaoCombate::getId).toList());
        assertTrue(tecnicas.buscar("pegada_1").isJogavel());
        for (String id : new String[]{"pegada_2", "pegada_3", "pegada_4"}) {
            AcaoCombate a = tecnicas.buscar(id);
            assertEquals(DisponibilidadeAcao.PENDENTE, a.getDisponibilidade(), id);
            assertFalse(a.isJogavel(), id);
            assertFalse(a.possuiAnimacaoConjunta(), id + " nao pode ter animacao inventada");
            assertNull(a.getEstadoInteracaoAoSucesso(), id + " nao pode ter regra inventada");
            assertEquals(CategoriaAcao.PEGADA, a.getCategoria());
            assertNotNull(a.getDescricao());
        }
    }

    // =====================================================
    // CONTEUDO DE TESTE (animacao ficticia)
    // =====================================================
    @Test
    void catalogoDeTesteRegistraAnimacaoFicticiaComDistribuicao() {
        ConteudoCombate c = CarregadorConteudoCombate.carregar(
                FonteConteudo.classpath(), CAT_ANIM_TESTE, CAT_TEC_TESTE);

        AnimacaoConjuntaQuadros f = c.getRegistroAnimacoes().exigir("teste_5_quadros");
        assertEquals(5, f.quantidadeQuadros());
        assertArrayEquals(new int[]{4, 4, 8, 8, 16}, f.copiarDuracoesQuadros());
        assertEquals(40, f.getDuracaoTotalFrames());

        AcaoCombate t = c.getRegistroAcoes().buscar("tecnica_teste");
        assertNotNull(t);
        assertEquals("teste_5_quadros", t.getIdAnimacaoConjunta());
        assertEquals(40, t.getDuracaoAnimacaoFrames(), "duracao herdada da animacao");
        assertEquals(30, c.getRegistroAcoes().buscar("pegada_1").getDuracaoAnimacaoFrames(),
                "pegada_1 herdou 30 frames do catalogo de animacoes");
    }

    // =====================================================
    // ERROS DE CATALOGO
    // =====================================================
    private static FonteConteudo memoria(Map<String, String> arquivos) {
        FonteConteudo classpath = FonteConteudo.classpath();
        return caminho -> arquivos.containsKey(caminho) ? arquivos.get(caminho) : classpath.ler(caminho);
    }

    private static String catalogoAnimacoes(String itens) {
        return "{\"tipo\": \"CATALOGO_ANIMACOES_COMBATE\", \"versao\": 1, \"animacoes\": [" + itens + "]}";
    }

    private static final String ANIM_PEGADA_1 =
            "{\"id\": \"pegada_1_entrada\", \"formato\": \"MIAUSTUDIO_PROJECT_V2\","
            + " \"arquivo\": \"/conteudo/miaustudio/PEGADA_1.json\", \"duracaoFrames\": 30}";

    private static String catalogoTecnicas(String itens) {
        return "{\"tipo\": \"CATALOGO_TECNICAS_COMBATE\", \"versao\": 1, \"tecnicas\": [" + itens + "]}";
    }

    private static String tecnicaJogavel(String id, String extra) {
        return "{\"id\": \"" + id + "\", \"nome\": \"T\", \"categoria\": \"PEGADA\","
                + " \"disponibilidade\": \"JOGAVEL\", \"alcanceHorizontal\": 10,"
                + " \"diferencaMaximaLinhas\": 0, \"exigeFrenteParaOponente\": true,"
                + " \"custoFolego\": 1, \"dificuldade\": 5,"
                + " \"pesos\": {\"tecnica\": 1, \"forca\": 0, \"agilidade\": 0, \"pesoCorporal\": 0}"
                + extra + "}";
    }

    private static String erroAnimacoes(String catalogo) {
        Map<String, String> m = new HashMap<>();
        m.put("/a.json", catalogo);
        return assertThrows(ConteudoInvalidoException.class,
                () -> CarregadorConteudoCombate.carregarAnimacoes(memoria(m), "/a.json")).getMessage();
    }

    private static String erroTecnicas(String catalogo) {
        Map<String, String> m = new HashMap<>();
        m.put("/a.json", catalogoAnimacoes(ANIM_PEGADA_1));
        m.put("/t.json", catalogo);
        return assertThrows(ConteudoInvalidoException.class,
                () -> CarregadorConteudoCombate.carregar(memoria(m), "/a.json", "/t.json")).getMessage();
    }

    @Test
    void animacaoDuplicadaNoCatalogoERejeitada() {
        String msg = erroAnimacoes(catalogoAnimacoes(ANIM_PEGADA_1 + ", " + ANIM_PEGADA_1));
        assertTrue(msg.contains("animacao duplicada: pegada_1_entrada"), msg);
    }

    @Test
    void errosDoCatalogoDeAnimacoes() {
        assertTrue(erroAnimacoes(catalogoAnimacoes(ANIM_PEGADA_1.replace("MIAUSTUDIO_PROJECT_V2", "GIF")))
                .contains("formato \"GIF\" nao suportado"));
        assertTrue(erroAnimacoes(catalogoAnimacoes(ANIM_PEGADA_1.replace("PEGADA_1.json", "NAO_EXISTE.json")))
                .contains("Recurso nao encontrado no classpath: /conteudo/miaustudio/NAO_EXISTE.json"));
        assertTrue(erroAnimacoes(catalogoAnimacoes(ANIM_PEGADA_1.replace("\"duracaoFrames\": 30", "\"duracaoFrame\": 30")))
                .contains("campo(s) desconhecido(s) [duracaoFrame]"));
        assertTrue(erroAnimacoes(catalogoAnimacoes(ANIM_PEGADA_1.replace("}", ", \"duracoesQuadros\": [10, 10]}")))
                .contains("3 valores"));
        assertTrue(erroAnimacoes(catalogoAnimacoes(ANIM_PEGADA_1.replace("}", ", \"duracoesQuadros\": [10, 10, 5]}")))
                .contains("difere da duracao total"));
        assertTrue(erroAnimacoes("{\"tipo\": \"CATALOGO_TECNICAS_COMBATE\", \"versao\": 1, \"animacoes\": []}")
                .contains("tipo"));
        assertTrue(erroAnimacoes(catalogoAnimacoes("").replace("\"versao\": 1", "\"versao\": 9"))
                .contains("versao 9 nao suportada"));
    }

    @Test
    void tecnicaDuplicadaERejeitada() {
        String msg = erroTecnicas(catalogoTecnicas(tecnicaJogavel("x", "") + ", " + tecnicaJogavel("X", "")));
        assertTrue(msg.contains("tecnica duplicada: x"), msg);
    }

    @Test
    void tecnicaComAnimacaoInexistenteERejeitada() {
        String msg = erroTecnicas(catalogoTecnicas(
                tecnicaJogavel("x", ", \"animacao\": \"fantasma\", \"duracaoAnimacaoFrames\": 10")));
        assertTrue(msg.contains("animacao inexistente: fantasma"), msg);
        assertTrue(msg.contains("tecnicas[0].animacao"), msg);
    }

    @Test
    void errosDoCatalogoDeTecnicas() {
        // duracao diferente da definida pela animacao
        assertTrue(erroTecnicas(catalogoTecnicas(tecnicaJogavel("x",
                ", \"animacao\": \"pegada_1_entrada\", \"duracaoAnimacaoFrames\": 20")))
                .contains("a animacao pegada_1_entrada define 30 frames"));
        // duracao sem animacao
        assertTrue(erroTecnicas(catalogoTecnicas(tecnicaJogavel("x", ", \"duracaoAnimacaoFrames\": 20")))
                .contains("informado sem \"animacao\""));
        // JOGAVEL sem regra obrigatoria
        assertTrue(erroTecnicas(catalogoTecnicas(tecnicaJogavel("x", "").replace("\"custoFolego\": 1,", "")))
                .contains("tecnicas[0].custoFolego: campo obrigatorio ausente"));
        // enum invalido
        assertTrue(erroTecnicas(catalogoTecnicas(tecnicaJogavel("x", "").replace("\"PEGADA\"", "\"CHUTE\"")))
                .contains("categoria: valor \"CHUTE\" invalido"));
        assertTrue(erroTecnicas(catalogoTecnicas(tecnicaJogavel("x", "").replace("JOGAVEL", "QUASE")))
                .contains("disponibilidade"));
        // campo com erro de digitacao
        assertTrue(erroTecnicas(catalogoTecnicas(tecnicaJogavel("x", ", \"alcence\": 3")))
                .contains("[alcence]"));
        // regra invalida pela validacao do proprio AcaoCombate
        assertTrue(erroTecnicas(catalogoTecnicas(tecnicaJogavel("x", "").replace("\"dificuldade\": 5", "\"dificuldade\": 500")))
                .contains("dificuldade deve ficar entre 0 e 100"));
    }

    @Test
    void tecnicaPendentePodeSerApenasUmNome() {
        Map<String, String> m = new HashMap<>();
        m.put("/a.json", catalogoAnimacoes(ANIM_PEGADA_1));
        m.put("/t.json", catalogoTecnicas("{\"id\": \"p\", \"nome\": \"P\", \"categoria\": \"QUEDA\","
                + " \"disponibilidade\": \"PENDENTE\"}"));
        AcaoCombate p = CarregadorConteudoCombate.carregar(memoria(m), "/a.json", "/t.json")
                .getRegistroAcoes().buscar("p");
        assertFalse(p.isJogavel());
        assertEquals(0.0, p.getSomaPesosAtributos());
    }

    @Test
    void caminhoRelativoEResolvidoPeloDiretorioDoCatalogo() {
        assertEquals("/conteudo/animacoes/x.json",
                CarregadorConteudoCombate.resolver("/conteudo/animacoes/animacoes.json", "x.json"));
        assertEquals("/abs/x.json",
                CarregadorConteudoCombate.resolver("/conteudo/animacoes/animacoes.json", "/abs/x.json"));
    }
}
