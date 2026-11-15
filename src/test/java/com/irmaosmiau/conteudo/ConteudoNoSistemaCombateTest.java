package com.irmaosmiau.conteudo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.AnimacaoConjuntaQuadros;
import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import org.junit.jupiter.api.Test;

/**
 * Conteudo carregado de JSON rodando no SistemaCombate real.
 */
class ConteudoNoSistemaCombateTest {

    private static IrmaoMiau branco(int x) {
        IrmaoMiau b = new IrmaoMiauBranco(x, 150, 0.7);
        b.setViradoParaDireita(true);
        return b;
    }

    private static IrmaoMiau preto(int x) {
        IrmaoMiau p = new IrmaoMiauPreto(x, 150, 0.7);
        p.setViradoParaDireita(false);
        return p;
    }

    /** Estado observavel de um tick, para comparar legado x JSON. */
    private static String fotografia(SistemaCombate s, IrmaoMiau b, IrmaoMiau p) {
        return s.getEstadoInteracao()
                + "|anim=" + s.isAnimacaoCombateAtiva()
                + "|bloq=" + b.isBloqueadoPorAnimacaoCombate() + "," + p.isBloqueadoPorAnimacaoCombate()
                + "|poseB=" + java.util.Arrays.toString(b.copiarPoseCombateExterna())
                + "|poseP=" + java.util.Arrays.toString(p.copiarPoseCombateExterna())
                + "|dx=" + b.getDeslocamentoVisualCombateX() + "," + p.getDeslocamentoVisualCombateX()
                + "|dy=" + b.getDeslocamentoVisualCombateY() + "," + p.getDeslocamentoVisualCombateY()
                + "|folego=" + b.getFolegoAtual() + "," + p.getFolegoAtual();
    }

    @Test
    void pegada1ViaJsonTemExatamenteOMesmoComportamentoTickATick() {
        IrmaoMiau b1 = branco(413);
        IrmaoMiau p1 = preto(500);
        SistemaCombate legado = new SistemaCombate(b1, p1);

        IrmaoMiau b2 = branco(413);
        IrmaoMiau p2 = preto(500);
        SistemaCombate json = CarregadorConteudoCombate.carregarPadrao().criarSistema(b2, p2);

        assertEquals(legado.tentarAcao(b1, p1, "pegada_1"), json.tentarAcao(b2, p2, "pegada_1"));
        for (int tick = 0; tick <= 60; tick++) {
            assertEquals(fotografia(legado, b1, p1), fotografia(json, b2, p2), "tick " + tick);
            if (tick == 45) {
                // rompe por distancia nos dois
                b1.moverX(-1);
                b2.moverX(-1);
            }
            legado.atualizar();
            json.atualizar();
        }
        assertFalse(json.estaEmInteracao());
    }

    @Test
    void pegadasPendentesDoCatalogoNaoExecutam() {
        IrmaoMiau b = branco(413);
        IrmaoMiau p = preto(500);
        SistemaCombate s = CarregadorConteudoCombate.carregarPadrao().criarSistema(b, p);
        double folego = b.getFolegoAtual();

        // 5L (pedido): as pegadas 2-4 sao jogaveis; a 87 px (124 u) so a PEGADA_3 (alcance 135)
        // alcanca; a 2 (115) e a 4 (110) precisam chegar mais perto.
        for (String id : new String[]{"pegada_2", "pegada_4"}) {
            assertTrue(s.possuiAcao(id));
            assertFalse(s.podeTentarAcao(b, p, id), id);
            assertEquals(ResultadoAcao.INDISPONIVEL, s.tentarAcao(b, p, id), id);
        }
        assertTrue(s.podeTentarAcao(b, p, "pegada_3"));
        assertEquals(folego, b.getFolegoAtual());
        assertFalse(s.estaEmInteracao());
        assertFalse(b.isPoseCombateExternaAtiva());
    }

    @Test
    void animacaoFicticiaDeCincoQuadrosTocaNosRenderersExistentes() {
        ConteudoCombate c = CarregadorConteudoCombate.carregar(FonteConteudo.classpath(),
                "/conteudo-teste/animacoes.json", "/conteudo-teste/tecnicas.json");
        AnimacaoConjuntaQuadros anim = c.getRegistroAnimacoes().exigir("teste_5_quadros");

        // Na autoria o aplicador e o Preto.
        IrmaoMiau b = branco(220);
        IrmaoMiau p = preto(500);
        SistemaCombate s = c.criarSistema(b, p);

        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(p, b, "tecnica_teste"));

        // duracoesQuadros [4, 4, 8, 8, 16] em 40 frames
        int[] esperado = new int[40];
        int[] limites = {4, 8, 16, 24, 40};
        for (int f = 0, q = 0; f < 40; f++) {
            while (f >= limites[q]) {
                q++;
            }
            esperado[f] = q;
        }

        for (int f = 0; f < 40; f++) {
            int q = esperado[f];
            assertArrayEquals(anim.copiarQuadroAplicador(q), p.copiarPoseCombateExterna(), "frame " + f);
            assertArrayEquals(anim.copiarQuadroReceptor(q), b.copiarPoseCombateExterna(), "frame " + f);
            assertEquals(anim.deslocamentoAplicadorX(q), p.getDeslocamentoVisualCombateX());
            assertEquals(-4.0, b.getDeslocamentoVisualCombateY());
            assertTrue(p.isBloqueadoPorAnimacaoCombate());
            s.atualizar();
        }

        // Sem estado de interacao: ao terminar, as poses saem e o controle volta.
        assertFalse(s.isAnimacaoCombateAtiva());
        assertFalse(s.estaEmInteracao());
        assertFalse(p.isPoseCombateExternaAtiva());
        assertFalse(b.isPoseCombateExternaAtiva());
        assertFalse(p.isBloqueadoPorAnimacaoCombate());
    }
}
