package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Validacoes de dados de AcaoCombate.
 *
 * O helper "criar" usa valores validos e permite variar um campo por vez,
 * para que cada teste mostre exatamente qual regra esta sendo verificada.
 */
class AcaoCombateTest {

    private static AcaoCombate criar(
            String id,
            String nome,
            CategoriaAcao categoria,
            String idAnimacao,
            int duracao,
            double alcance,
            int linhas,
            double custo,
            double dificuldade,
            double pesoTecnica
    ) {
        return new AcaoCombate(
                id, nome, categoria, EstadoInteracaoCombate.PEGADA_1,
                idAnimacao, duracao, alcance, linhas, true,
                custo, dificuldade, pesoTecnica, 0.3, 0.2, 0.0
        );
    }

    private static AcaoCombate valida() {
        return criar("teste", "Teste", CategoriaAcao.PEGADA,
                "anim_teste", 30, 100.0, 1, 3.0, 20.0, 0.5);
    }

    @Test
    void acaoValidaGuardaOsDados() {
        AcaoCombate acao = valida();

        assertEquals("teste", acao.getId());
        assertEquals("Teste", acao.getNome());
        assertEquals(CategoriaAcao.PEGADA, acao.getCategoria());
        assertEquals(EstadoInteracaoCombate.PEGADA_1, acao.getEstadoInteracaoAoSucesso());
        assertTrue(acao.possuiAnimacaoConjunta());
        assertEquals("anim_teste", acao.getIdAnimacaoConjunta());
        assertEquals(30, acao.getDuracaoAnimacaoFrames());
        assertEquals(100.0, acao.getAlcanceHorizontal());
        assertEquals(1, acao.getDiferencaMaximaLinhas());
        assertTrue(acao.isExigeFrenteParaOponente());
        assertEquals(3.0, acao.getCustoFolego());
        assertEquals(20.0, acao.getDificuldade());
        assertEquals(1.0, acao.getSomaPesosAtributos(), 1e-9);
    }

    @Test
    void idNomeEAnimacaoSaoNormalizados() {
        AcaoCombate acao = criar("  PeGaDa_X  ", "  Pegada X  ", CategoriaAcao.PEGADA,
                "  ANIM_X ", 10, 50.0, 0, 0.0, 0.0, 0.0);

        assertEquals("pegada_x", acao.getId());
        assertEquals("Pegada X", acao.getNome());
        assertEquals("anim_x", acao.getIdAnimacaoConjunta());
    }

    @Test
    void acaoSemAnimacaoPrecisaDeDuracaoZero() {
        AcaoCombate semAnimacao = criar("a", "A", CategoriaAcao.IMPACTO,
                null, 0, 10.0, 0, 0.0, 0.0, 0.0);
        assertFalse(semAnimacao.possuiAnimacaoConjunta());
        assertNull(semAnimacao.getIdAnimacaoConjunta());

        AcaoCombate idEmBranco = criar("b", "B", CategoriaAcao.IMPACTO,
                "   ", 0, 10.0, 0, 0.0, 0.0, 0.0);
        assertFalse(idEmBranco.possuiAnimacaoConjunta());

        assertThrows(IllegalArgumentException.class, () -> criar("c", "C",
                CategoriaAcao.IMPACTO, null, 5, 10.0, 0, 0.0, 0.0, 0.0));
    }

    @Test
    void animacaoCadastradaPrecisaDeDuracaoPositiva() {
        assertThrows(IllegalArgumentException.class, () -> criar("a", "A",
                CategoriaAcao.PEGADA, "anim", 0, 10.0, 0, 0.0, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> criar("a", "A",
                CategoriaAcao.PEGADA, "anim", -1, 10.0, 0, 0.0, 0.0, 0.0));
    }

    @Test
    void camposObrigatoriosSaoValidados() {
        assertThrows(IllegalArgumentException.class, () -> criar(null, "A",
                CategoriaAcao.PEGADA, null, 0, 10.0, 0, 0.0, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> criar(" ", "A",
                CategoriaAcao.PEGADA, null, 0, 10.0, 0, 0.0, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> criar("a", null,
                CategoriaAcao.PEGADA, null, 0, 10.0, 0, 0.0, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> criar("a", "",
                CategoriaAcao.PEGADA, null, 0, 10.0, 0, 0.0, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> criar("a", "A",
                null, null, 0, 10.0, 0, 0.0, 0.0, 0.0));
    }

    @Test
    void limitesNumericosSaoValidados() {
        // alcance negativo
        assertThrows(IllegalArgumentException.class, () -> criar("a", "A",
                CategoriaAcao.PEGADA, null, 0, -0.1, 0, 0.0, 0.0, 0.0));
        // diferenca de linhas negativa
        assertThrows(IllegalArgumentException.class, () -> criar("a", "A",
                CategoriaAcao.PEGADA, null, 0, 10.0, -1, 0.0, 0.0, 0.0));
        // custo de folego negativo
        assertThrows(IllegalArgumentException.class, () -> criar("a", "A",
                CategoriaAcao.PEGADA, null, 0, 10.0, 0, -1.0, 0.0, 0.0));
        // dificuldade fora de 0..100
        assertThrows(IllegalArgumentException.class, () -> criar("a", "A",
                CategoriaAcao.PEGADA, null, 0, 10.0, 0, 0.0, -1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> criar("a", "A",
                CategoriaAcao.PEGADA, null, 0, 10.0, 0, 0.0, 100.1, 0.0));
        // peso negativo
        assertThrows(IllegalArgumentException.class, () -> criar("a", "A",
                CategoriaAcao.PEGADA, null, 0, 10.0, 0, 0.0, 0.0, -0.5));
    }

    @Test
    void limitesInclusivosSaoAceitos() {
        AcaoCombate acao = criar("a", "A", CategoriaAcao.PEGADA,
                null, 0, 0.0, 0, 0.0, 100.0, 0.0);
        assertEquals(100.0, acao.getDificuldade());
        assertEquals(0.0, acao.getAlcanceHorizontal());
    }
}
