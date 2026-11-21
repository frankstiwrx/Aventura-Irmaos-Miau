package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;

import org.junit.jupiter.api.Test;

/**
 * 5P.1 (pedido): a recuperacao passiva continua bloqueada na pegada, mas
 * RESPIRAR de proposito (RT) recupera um pouco (fator
 * CustosFolego.RESPIRACAO_NA_PEGADA), nos dois papeis.
 *
 * Entrega 5O.3: com uma pegada ativa (aplicando ou sofrendo) ninguem recupera
 * folego nem condicionamento, nem respirando (RT) nem parado; o consumo normal
 * continua; ao acabar a pegada a recuperacao volta.
 *
 * Complemento 5Q.8 (pedido): respirar de proposito agora e o botao de respirar (J1: a tecla de respirar;
 * J2 no teclado-controle: I = Y), nao mais o RT sozinho. Os numeros nao mudaram.
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

    /** 5P.1 (pedido): n ticks de RT na pegada: os dois ganham um pouco de folego. */
    private void recuperaPoucoComRt(int n, String caso) {
        double f1 = j1().getFolegoAtual();
        double f2 = j2().getFolegoAtual();
        ticks(n);
        assertTrue(j1().getFolegoAtual() > f1, caso + ": J1 respirando recupera um pouco");
        assertTrue(j2().getFolegoAtual() > f2, caso + ": J2 respirando recupera um pouco");
    }

    @Test
    void j1AplicandoNinguemRecuperaParadoNemComRt() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            assertTrue(j1().isVinculadoEmPegada() && j2().isVinculadoEmPegada());
            cansar(j1(), j2());
            semRecuperar(60, "parados (recuperacao passiva)");
            tecla("brancoRespirar", true);                       // respirar do aplicador
            ctl("I", true);                                      // respirar (Y) do receptor
            recuperaPoucoComRt(60, "RT dos dois");               // 5P.1 (pedido): antes nao recuperava
            tecla("brancoRespirar", false);
            ctl("I", false);
        });
    }

    @Test
    void j2AplicandoNinguemRecupera() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j2Pega();
            cansar(j1(), j2());
            semRecuperar(60, "J2 aplicando, parados");
            ctl("I", true);
            tecla("brancoRespirar", true);
            recuperaPoucoComRt(60, "J2 aplicando, RT dos dois");  // 5P.1 (pedido)
            ctl("I", false);
            tecla("brancoRespirar", false);
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
            ctl("I", true);
            double com = j2().getFolegoAtual();
            ticks(30);
            double gastoComRt = com - j2().getFolegoAtual();
            ctl("I", false);
            // 5P.1 (pedido): o RT agarrado recupera, mas bem menos que respirando livre
            double ganhoNaPegada = gastoSemRt - gastoComRt;
            assertTrue(ganhoNaPegada > 0, "segurar o RT agarrado recupera um pouco");
            assertTrue(s().isInteracaoEstabelecida(), "o RT nao quebra a pegada");
            s().encerrarInteracao();
            ticks(5);
            double livre = j2().getFolegoAtual();
            ctl("I", true);
            ticks(30);
            ctl("I", false);
            double ganhoLivre = j2().getFolegoAtual() - livre;
            assertTrue(ganhoNaPegada < ganhoLivre * 0.4, "na pegada " + ganhoNaPegada + " x livre " + ganhoLivre);
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
            tecla("brancoRespirar", true);
            double r = j1().getFolegoAtual();
            ticks(20);
            assertTrue(j1().getFolegoAtual() > r, "e respirando com o RT");
            tecla("brancoRespirar", false);
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
                    tecla("brancoRespirar", true);
                    ticks(20);
                    tecla("brancoRespirar", false);
                    assertTrue(j1().getFolegoAtual() > f, inicio + (deitar ? " deitado" : "") + ": respira livre");
                }
            }
        });
    }
}
