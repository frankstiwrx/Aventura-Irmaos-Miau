package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.6: rasteira no GamePanel real. Defendida, o receptor faz so um pequeno
 * desequilibrio e VOLTA a pose da pegada (antes a pose sumia/ficava a de queda e a
 * tentativa seguinte partia dela, acumulando); puxar o outro na pegada abre a rasteira.
 */
class Rasteira5Q6GamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void rasteira() {
        MotorSolo.Resposta r = m().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));
        assertEquals("rasteira", m().getTentativa().map(t -> t.tecnica().id()).orElse(r.toString()));
        for (int i = 0; i < 200 && m().getTentativa().isPresent(); i++) {
            ticks(1);
        }
    }

    private static int diferenca(int[] a, int[] b) {
        int d = 0;
        for (int i = 4; i < 30; i++) {
            d = Math.max(d, Math.abs(a[i] - b[i]));
        }
        return d;
    }

    @Test
    void defendidaVoltaAPoseDaPegadaSemAcumularNasTentativasSeguidas() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(80);                                           // parado: base cheia
            int[] pegada = j2().copiarPoseCombateExterna();
            assertNotNull(pegada, "pose da pegada");
            int[] pegadaJ1 = j1().copiarPoseCombateExterna();
            for (int k = 0; k < 4; k++) {
                rasteira();
                assertEquals("DEFENDIDA", m().getUltimoEvento().tipo(), "parado com base: segura");
                assertEquals("rasteira_defendida", m().efeitoVisual(Lado.B).orElseThrow().tecnica(),
                        "reacao curta de quem segurou");
                int pico = 0;
                for (int i = 0; i < MotorSolo.TICKS_REACAO_DEFENDIDA; i++) {
                    ticks(1);
                    int[] q = j2().copiarPoseCombateExterna();
                    assertNotNull(q, "nunca fica sem pose no meio da pegada");
                    pico = Math.max(pico, diferenca(q, pegada));
                }
                ticks(40);
                assertTrue(s().isInteracaoEstabelecida(), "a pegada continua");
                int[] depois = j2().copiarPoseCombateExterna();
                assertNotNull(depois, "a pose da pegada voltou (antes: sumia)");
                assertTrue(diferenca(depois, pegada) <= 3, "voltou a postura da pegada na tentativa " + k
                        + " (dif " + diferenca(depois, pegada) + ")");
                assertTrue(diferenca(j1().copiarPoseCombateExterna(), pegadaJ1) <= 3, "quem tentou tambem volta");
                assertTrue(pico <= 32, "reacao pequena, sem esticar: " + pico);
            }
        });
    }

    @Test
    void puxarOOutroNaPegadaAbreARasteira() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(80);
            assertTrue(m().pontuacaoRasteira(Lado.B) < MotorSolo.RASTEIRA_LIMIAR, "parado com base: nao entra");
            tecla("brancoControle", true);                       // arrasto controlado da pegada
            tecla("brancoEsquerda", true);                       // J1 puxa o J2
            ticks(6);
            tecla("brancoEsquerda", false);
            tecla("brancoControle", false);
            ticks(2);
            assertTrue(s().isInteracaoEstabelecida(), "pegada continua: " + m().getPosicao() + " " + s().getUltimoMotivoEncerramento());
            assertTrue(m().pontuacaoRasteira(Lado.B) >= MotorSolo.RASTEIRA_LIMIAR, "puxado: o pe saiu do lugar");
            rasteira();
            ticks(2);
            assertTrue(j2().isDeitado() || m().isAterrissando() || m().getPosicao().isNoChao(), "derrubou");
        });
    }
}
