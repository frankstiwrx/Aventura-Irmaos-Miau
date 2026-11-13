package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * REGRESSAO "GOLDEN" DA PEGADA_1.
 *
 * Copia congelada dos arrays aprovados pelo criador (MiauStudio JSON v2,
 * aplicador Branco, receptor Preto, centro de autoria X=360 linha=2,
 * offsets visuais 0/0) e dos parametros da acao "pegada_1".
 *
 * Se este teste falhar, alguem alterou a animacao ou a regra da PEGADA_1.
 * NAO "conserte" o teste copiando os novos valores sem autorizacao
 * explicita do criador e sem novo teste jogavel no NetBeans.
 */
class Pegada1RegressaoArraysTest {

    private static final int[][] APLICADOR_APROVADO = {
        {56, 0, 7, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {56, 0, 7, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 38, 0, 60, 3, -10, 12, -14, 26, 0, 0, 0, 0, 0, 0, 0, 0},
        {56, 0, 7, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 13, 119, -25, -10, 12, -14, 26, 7, -2, 14, 3, 11, -6, 0, 0}
    };

    private static final int[][] RECEPTOR_APROVADO = {
        {140, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {140, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -12, -12, -15, -9, 0, 0, 0, 0, 0, 0, 0, 0},
        {125, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, -6, 0, 0, 21, -11, 8, 10, -23, -18, -28, 0, 14, 0, 27, 0, 15, -1, 0, 0}
    };

    @Test
    void arraysDaPegada1ContinuamIdenticosAosAprovados() {
        AnimacaoConjuntaQuadros anim =
                new RegistroAnimacoesCombate().buscar("pegada_1_entrada");

        assertNotNull(anim, "animacao pegada_1_entrada sumiu do registro");
        assertEquals(3, anim.quantidadeQuadros());

        for (int i = 0; i < 3; i++) {
            assertArrayEquals(APLICADOR_APROVADO[i], anim.copiarQuadroAplicador(i),
                    "APLICADOR (Branco) quadro " + (i + 1) + " foi alterado");
            assertArrayEquals(RECEPTOR_APROVADO[i], anim.copiarQuadroReceptor(i),
                    "RECEPTOR (Preto) quadro " + (i + 1) + " foi alterado");
        }
    }

    @Test
    void offsetsDerivadosContinuamIguais() {
        AnimacaoConjuntaQuadros anim =
                new RegistroAnimacoesCombate().buscar("pegada_1_entrada");

        assertEquals(0.0, anim.getOffsetVisualY());
        assertEquals(0.0, anim.deslocamentoAplicadorX(0));
        assertEquals(0.0, anim.deslocamentoAplicadorX(1));
        assertEquals(0.0, anim.deslocamentoAplicadorX(2));
        assertEquals(0.0, anim.deslocamentoReceptorX(0));
        assertEquals(0.0, anim.deslocamentoReceptorX(1));
        assertEquals(-15.0, anim.deslocamentoReceptorX(2)); // 140 -> 125
    }

    @Test
    void parametrosDaAcaoPegada1ContinuamIguais() {
        AcaoCombate acao = new RegistroAcoes().buscar("pegada_1");

        assertNotNull(acao);
        assertEquals("pegada_1", acao.getId());
        assertEquals("Pegada 1", acao.getNome());
        assertSame(CategoriaAcao.PEGADA, acao.getCategoria());
        assertSame(EstadoInteracaoCombate.PEGADA_1, acao.getEstadoInteracaoAoSucesso());
        assertTrue(acao.possuiAnimacaoConjunta());
        assertEquals("pegada_1_entrada", acao.getIdAnimacaoConjunta());
        assertEquals(30, acao.getDuracaoAnimacaoFrames());
        assertEquals(125.0, acao.getAlcanceHorizontal());
        assertEquals(1, acao.getDiferencaMaximaLinhas());
        assertTrue(acao.isExigeFrenteParaOponente());
        assertEquals(3.0, acao.getCustoFolego());
        assertEquals(20.0, acao.getDificuldade());
        assertEquals(0.50, acao.getPesoTecnica());
        assertEquals(0.30, acao.getPesoForca());
        assertEquals(0.20, acao.getPesoAgilidade());
        assertEquals(0.00, acao.getPesoPesoCorporal());
    }
}
