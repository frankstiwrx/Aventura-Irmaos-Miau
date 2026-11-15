package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5O.3: com uma pegada ativa (aplicando ou sofrendo) ninguem recupera
 * folego nem condicionamento, nem respirando (RT) nem parado; o consumo normal
 * continua; ao acabar a pegada a recuperacao volta.
 */
class FolegoNaPegada53GamePanelTest extends Base5OGamePanel {

    private void ctl(String tecla, boolean p) {
        tecla("pretoControle" + tecla, p);
    }

    /** J2 (teclado-controle) anda ate o J1 e pega com a seta para cima. */
    private void j2Pega() {
        ctl("A", true);
        for (int i = 0; i < 400 && !s().podeTentarAcao(j2(), j1(), "pegada_1"); i++) {
            ticks(1);
        }
        ctl("A", false);
        ticks(2);
        ctl("UP", true);
        ticks(1);
        ctl("UP", false);
        ticks(40);
        assertTrue(s().isInteracaoEstabelecida());
        assertEquals(j2(), s().getAplicadorInteracao());
    }

    private static void cansar(IrmaoMiau... ls) {
        for (IrmaoMiau l : ls) {
            l.gastarFolegoEsforco(10.0, "teste");
        }
    }

    /** n ticks sem nenhum aumento de folego ou condicionamento nos dois. */
    private void semRecuperar(int n, String caso) {
        for (int i = 0; i < n; i++) {
            double[] antes = {j1().getFolegoAtual(), j2().getFolegoAtual(),
                j1().getCondicionamentoAtual(), j2().getCondicionamentoAtual()};
            ticks(1);
            assertTrue(j1().getFolegoAtual() <= antes[0] + 1e-9, caso + ": J1 recuperou folego");
            assertTrue(j2().getFolegoAtual() <= antes[1] + 1e-9, caso + ": J2 recuperou folego");
            assertTrue(j1().getCondicionamentoAtual() <= antes[2] + 1e-9, caso + ": J1 recuperou condicionamento");
            assertTrue(j2().getCondicionamentoAtual() <= antes[3] + 1e-9, caso + ": J2 recuperou condicionamento");
        }
    }

    @Test
    void j1AplicandoNinguemRecuperaParadoNemComRt() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            assertTrue(j1().isVinculadoEmPegada() && j2().isVinculadoEmPegada());
            cansar(j1(), j2());
            semRecuperar(60, "parados (recuperacao passiva)");
            tecla("brancoModDefesa", true);                      // RT do aplicador
            ctl("P", true);                                      // RT do receptor
            semRecuperar(60, "RT dos dois");
            tecla("brancoModDefesa", false);
            ctl("P", false);
        });
    }

    @Test
    void j2AplicandoNinguemRecupera() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j2Pega();
            cansar(j1(), j2());
            semRecuperar(60, "J2 aplicando, parados");
            ctl("P", true);
            tecla("brancoModDefesa", true);
            semRecuperar(60, "J2 aplicando, RT dos dois");
            ctl("P", false);
            tecla("brancoModDefesa", false);
        });
    }

    @Test
    void rtNaPegadaNaoRecuperaNemCobra() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            cansar(j2());
            double sem = j2().getFolegoAtual();
            ticks(30);
            double gastoSemRt = sem - j2().getFolegoAtual();
            ctl("P", true);
            double com = j2().getFolegoAtual();
            ticks(30);
            double gastoComRt = com - j2().getFolegoAtual();
            ctl("P", false);
            assertEquals(gastoSemRt, gastoComRt, 1e-6, "segurar o RT agarrado nao muda o folego");
            assertTrue(s().isInteracaoEstabelecida(), "o RT nao quebra a pegada");
        });
    }

    @Test
    void arrastoResistenciaEDisputaSemRecuperarComConsumoNormal() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j2Pega();
            cansar(j1(), j2());
            double f2 = j2().getFolegoAtual();
            ctl("D", true);                                      // arrasto do J2 (Contextual: automatico)
            tecla("brancoEsquerda", true);                       // o J1 forca para sair (resistencia)
            semRecuperar(30, "arrasto + resistencia");
            ctl("D", false);
            tecla("brancoEsquerda", false);
            assertTrue(j2().getFolegoAtual() < f2, "o arrasto continua custando");
            toque("brancoRompimento");                           // disputa (rompimento do J1)
            if (s().estaEmInteracao()) {
                semRecuperar(20, "depois da disputa, ainda na pegada");
            }
        });
    }

    @Test
    void aoAcabarAPegadaARecuperacaoVolta() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            cansar(j1(), j2());
            s().encerrarInteracao();
            assertFalse(j1().isVinculadoEmPegada() || j2().isVinculadoEmPegada());
            ticks(5);
            double f1 = j1().getFolegoAtual();
            double f2 = j2().getFolegoAtual();
            ticks(60);
            assertTrue(j1().getFolegoAtual() > f1 && j2().getFolegoAtual() > f2, "recupera parado de novo");
            tecla("brancoModDefesa", true);
            double r = j1().getFolegoAtual();
            ticks(20);
            assertTrue(j1().getFolegoAtual() > r, "e respirando com o RT");
            tecla("brancoModDefesa", false);
        });
    }

    @Test
    void livreRecuperaNasTresPosturas() throws Exception {
        naEdt(() -> {
            for (InicioLuta inicio : new InicioLuta[] {InicioLuta.EM_PE, InicioLuta.DE_JOELHOS}) {
                for (boolean deitar : new boolean[] {false, true}) {
                    if (inicio == InicioLuta.EM_PE && deitar) {
                        continue;
                    }
                    novo(inicio);
                    if (deitar) {
                        j1().alternarDeitado();                  // joelhos -> em pe
                        j1().alternarDeitado();                  // em pe -> deitado
                        ticks(40);
                    }
                    cansar(j1());
                    ticks(2);
                    double f = j1().getFolegoAtual();
                    tecla("brancoModDefesa", true);
                    ticks(20);
                    tecla("brancoModDefesa", false);
                    assertTrue(j1().getFolegoAtual() > f, inicio + (deitar ? " deitado" : "") + ": respira livre");
                }
            }
        });
    }
}
