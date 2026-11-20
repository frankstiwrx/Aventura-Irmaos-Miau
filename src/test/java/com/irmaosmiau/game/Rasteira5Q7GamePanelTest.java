package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.IntegridadeMembros;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.7 no GamePanel real: a rasteira e a derrubada forcada que FALHAM nao deformam os
 * bracos do defensor em nenhum instante (antes o antebraco ia de ~1,1x a ~1,6x no meio da
 * reacao: a pose da pegada ja estende o braco e a tecnica somava deslocamentos de repouso),
 * nem acumulam nas tentativas seguidas. As quedas que entram continuam derrubando.
 */
class Rasteira5Q7GamePanelTest extends Base5OGamePanel {

    /** Folga (px locais) por arredondamento das juntas. */
    private static final double FOLGA = 2.5;

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    /** Durante n ticks, nenhum segmento dos bracos de l sai do tamanho que tinha na pegada. */
    private void bracosSemDeformar(IrmaoMiau l, int[] pegada, int n, String oque) {
        double[] ref = IntegridadeMembros.comprimentos(pegada, l.getBracosEmRepouso());
        for (int i = 0; i < n; i++) {
            ticks(1);
            int[] p = l.copiarPoseCombateExterna();
            if (p == null) {
                continue;
            }
            double[] c = IntegridadeMembros.comprimentos(p, l.getBracosEmRepouso());
            for (int k = 0; k < 4; k++) {
                assertTrue(Math.abs(c[k] - ref[k]) <= FOLGA, oque + " tick " + i + ": segmento " + k + " "
                        + Math.round(ref[k]) + " -> " + Math.round(c[k]));
            }
        }
    }

    @Test
    void rasteiraQueFalhaNaoEsticaOsBracosEmNenhumInstanteNemAcumula() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(80);
            int[] pegada = j2().copiarPoseCombateExterna();
            assertNotNull(pegada);
            for (int tentativa = 0; tentativa < 3; tentativa++) {
                MotorSolo.Resposta r = m().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));
                assertTrue(r.aceito(), r.motivo());
                bracosSemDeformar(j2(), pegada, 50, "rasteira " + tentativa);
                assertEquals("DEFENDIDA", m().getUltimoEvento().tipo(), "parado com base: nao entra");
                ticks(20);
                assertArrayEquals(pegada, j2().copiarPoseCombateExterna(), "volta a pose da pegada sem acumular");
            }
        });
    }

    @Test
    void derrubadaForcadaQueNaoDerrubaUsaAReacaoDoEmpurraoSemDeformar() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(80);
            int[] pegada = j2().copiarPoseCombateExterna();
            MotorSolo.Resposta r = m().comando(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.L2));
            assertTrue(r.aceito(), r.motivo());
            bracosSemDeformar(j2(), pegada, 50, "derrubada forcada");
            assertEquals(Posicao.EM_PE_PEGADA, m().getPosicao(), "nao caiu (so desequilibrou)");
            ticks(40);
            assertArrayEquals(pegada, j2().copiarPoseCombateExterna(), "volta a pose da pegada");
        });
    }

    @Test
    void derrubadaForcadaSeguradaComRtTemAReacaoCurtaPropria() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(80);
            int[] pegada = j2().copiarPoseCombateExterna();
            MotorSolo.Resposta r = m().comando(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.L2));
            assertTrue(r.aceito(), r.motivo());
            ticks(2);
            tecla("pretoModDefesa", true);
            boolean viu = false;
            for (int i = 0; i < 40; i++) {
                ticks(1);
                viu |= m().efeitoVisual(Lado.B).map(e -> "derrubada_forcada_defendida".equals(e.tecnica()))
                        .orElse(false);
            }
            tecla("pretoModDefesa", false);
            assertEquals("DEFENDIDA", m().getUltimoEvento().tipo(), m().getUltimoEvento().toString());
            assertTrue(viu, "o defensor tem uma reacao curta (tronco cede e volta)");
            ticks(40);
            assertArrayEquals(pegada, j2().copiarPoseCombateExterna(), "volta a pose da pegada");
        });
    }

    @Test
    void rasteiraQueEntraContinuaDerrubando() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            tecla("brancoControle", true);
            tecla("brancoEsquerda", true);
            ticks(6);
            tecla("brancoEsquerda", false);
            tecla("brancoControle", false);
            m().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));
            ticks(80);
            assertTrue(j2().isDeitado(), "a rasteira bem-sucedida continua derrubando");
        });
    }
}
