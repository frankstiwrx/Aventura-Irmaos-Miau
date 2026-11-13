package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import org.junit.jupiter.api.Test;

/**
 * Timeline das animacoes conjuntas: relogio unico (AnimacaoCombateAtiva)
 * e conversao progresso -> quadro (AnimacaoConjuntaQuadros).
 */
class TimelineAnimacaoTest {

    private static int[] pose(int cenaX, int marcador) {
        int[] p = new int[30];
        p[0] = cenaX;
        p[29] = marcador;
        return p;
    }

    private static AnimacaoConjuntaQuadros tresQuadros() {
        return new AnimacaoConjuntaQuadros(
                new int[][]{pose(10, 1), pose(12, 2), pose(15, 3)},
                new int[][]{pose(100, 1), pose(100, 2), pose(90, 3)},
                4.0, -2.0
        );
    }

    // =====================================================
    // AnimacaoConjuntaQuadros
    // =====================================================
    @Test
    void progressoEDivididoEmTercosIguais() {
        AnimacaoConjuntaQuadros a = tresQuadros();

        assertEquals(3, a.quantidadeQuadros());
        assertEquals(0, a.indiceParaProgresso(0.0));
        assertEquals(0, a.indiceParaProgresso(0.33));
        assertEquals(1, a.indiceParaProgresso(1.0 / 3.0));
        assertEquals(1, a.indiceParaProgresso(0.66));
        assertEquals(2, a.indiceParaProgresso(2.0 / 3.0));
        assertEquals(2, a.indiceParaProgresso(1.0));
    }

    @Test
    void progressoForaDoIntervaloEhLimitado() {
        AnimacaoConjuntaQuadros a = tresQuadros();

        assertEquals(0, a.indiceParaProgresso(-5.0));
        assertEquals(2, a.indiceParaProgresso(7.0));
    }

    @Test
    void deslocamentoXEhRelativoAoPrimeiroQuadroMaisOffsetVisual() {
        AnimacaoConjuntaQuadros a = tresQuadros();

        // Quadro 1 nunca teletransporta: so o offset visual.
        assertEquals(4.0, a.deslocamentoAplicadorX(0));
        assertEquals(4.0, a.deslocamentoReceptorX(0));

        assertEquals(4.0 + 2, a.deslocamentoAplicadorX(1));
        assertEquals(4.0 + 5, a.deslocamentoAplicadorX(2));
        assertEquals(4.0 - 10, a.deslocamentoReceptorX(2));
        assertEquals(-2.0, a.getOffsetVisualY());
    }

    @Test
    void quadrosSaoCopiasDefensivas() {
        int[] original = pose(10, 1);
        int[][] aplicador = {original};
        AnimacaoConjuntaQuadros a = new AnimacaoConjuntaQuadros(
                aplicador, new int[][]{pose(20, 1)}, 0, 0);

        original[29] = 999;               // alterar a fonte...
        assertEquals(1, a.copiarQuadroAplicador(0)[29]); // ...nao afeta o registro

        int[] copia = a.copiarQuadroAplicador(0);
        copia[29] = 777;                  // alterar a copia...
        assertEquals(1, a.copiarQuadroAplicador(0)[29]); // ...tambem nao
    }

    @Test
    void validaFormatoDosQuadros() {
        int[][] ok = {pose(0, 0)};

        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoConjuntaQuadros(null, ok, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoConjuntaQuadros(new int[0][], ok, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoConjuntaQuadros(new int[][]{new int[29]}, ok, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoConjuntaQuadros(new int[][]{null}, ok, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoConjuntaQuadros(
                        new int[][]{pose(0, 0), pose(0, 0)}, ok, 0, 0));
    }

    // =====================================================
    // RegistroAnimacoesCombate
    // =====================================================
    @Test
    void registroDeAnimacoesBuscaPorIdNormalizado() {
        RegistroAnimacoesCombate registro = new RegistroAnimacoesCombate();

        assertSame(registro.buscar("pegada_1_entrada"),
                registro.buscar("  PEGADA_1_ENTRADA "));
        assertNull(registro.buscar("nao_existe"));
        assertNull(registro.buscar(null));
        assertThrows(IllegalArgumentException.class,
                () -> registro.registrar(" ", tresQuadros()));
        assertThrows(IllegalArgumentException.class,
                () -> registro.registrar("x", null));
    }

    // =====================================================
    // AnimacaoCombateAtiva
    // =====================================================
    @Test
    void relogioUnicoAvancaUmTickPorAtualizacao() {
        IrmaoMiau a = new IrmaoMiauBranco(0, 150, 0.7);
        IrmaoMiau b = new IrmaoMiauPreto(100, 150, 0.7);
        AnimacaoCombateAtiva anim = new AnimacaoCombateAtiva(" ANIM ", a, b, 30);

        assertEquals("anim", anim.getIdAnimacao());
        assertEquals(0, anim.getFrameAtual());
        assertEquals(0.0, anim.getProgresso());

        for (int i = 1; i < 30; i++) {
            anim.atualizar();
            assertEquals(i, anim.getFrameAtual());
            assertFalse(anim.isFinalizada());
            assertEquals(i / 30.0, anim.getProgresso(), 1e-12);
        }

        anim.atualizar();
        assertTrue(anim.isFinalizada());
        assertEquals(30, anim.getFrameAtual());
        assertEquals(1.0, anim.getProgresso());

        anim.atualizar(); // apos o fim nao passa da duracao
        assertEquals(30, anim.getFrameAtual());
    }

    @Test
    void finalizarAntecipadamenteLevaAoFim() {
        IrmaoMiau a = new IrmaoMiauBranco(0, 150, 0.7);
        IrmaoMiau b = new IrmaoMiauPreto(100, 150, 0.7);
        AnimacaoCombateAtiva anim = new AnimacaoCombateAtiva("x", a, b, 30);

        anim.atualizar();
        anim.finalizar();

        assertTrue(anim.isFinalizada());
        assertEquals(1.0, anim.getProgresso());
    }

    @Test
    void participantesSaoIdentificados() {
        IrmaoMiau a = new IrmaoMiauBranco(0, 150, 0.7);
        IrmaoMiau b = new IrmaoMiauPreto(100, 150, 0.7);
        IrmaoMiau c = new IrmaoMiauPreto(200, 150, 0.7);
        AnimacaoCombateAtiva anim = new AnimacaoCombateAtiva("x", a, b, 5);

        assertTrue(anim.contemPersonagem(a));
        assertTrue(anim.contemPersonagem(b));
        assertFalse(anim.contemPersonagem(c));
        assertSame(b, anim.getOutroPersonagem(a));
        assertSame(a, anim.getOutroPersonagem(b));
        assertNull(anim.getOutroPersonagem(c));
    }

    @Test
    void relogioValidaParametros() {
        IrmaoMiau a = new IrmaoMiauBranco(0, 150, 0.7);
        IrmaoMiau b = new IrmaoMiauPreto(100, 150, 0.7);

        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoCombateAtiva(null, a, b, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoCombateAtiva("x", null, b, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoCombateAtiva("x", a, a, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoCombateAtiva("x", a, b, 0));
    }

    @Test
    void quadroDeDoisTracksUsaOMesmoIndice() {
        AnimacaoConjuntaQuadros a = tresQuadros();

        for (int f = 0; f <= 30; f++) {
            int i = a.indiceParaProgresso(f / 30.0);
            assertArrayEquals(
                    new int[]{a.copiarQuadroAplicador(i)[29]},
                    new int[]{a.copiarQuadroReceptor(i)[29]},
                    "tracks dessincronizados no frame " + f
            );
        }
    }
}
