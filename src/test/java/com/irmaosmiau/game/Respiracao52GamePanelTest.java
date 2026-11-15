package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.CustosFolego;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;

import org.junit.jupiter.api.Test;

/** Entrega 5O.2: RT respira em pe, de joelhos e deitado, com recuperacao e movimento por postura. */
class Respiracao52GamePanelTest extends Base5OGamePanel {

    private static void deitar(IrmaoMiau l) {
        if (l.isAjoelhado()) {
            l.alternarDeitado();
        }
        l.alternarDeitado();
        assertTrue(l.isDeitado());
    }

    /** Folego recuperado pelo J1 segurando o RT (3) por n ticks, partindo de 10 abaixo do maximo. */
    private double recuperouRespirando(int n) {
        j1().gastarFolegoEsforco(10.0, "teste");
        ticks(1);
        double f = j1().getFolegoAtual();
        tecla("brancoModDefesa", true);
        ticks(n);
        tecla("brancoModDefesa", false);
        return j1().getFolegoAtual() - f;
    }

    @Test
    void rtRespiraNasTresPosturasSemMudarAPostura() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("brancoModDefesa", true);
            ticks(3);
            assertTrue(j1().isRespirando(), "em pe");
            tecla("brancoModDefesa", false);

            novo(InicioLuta.DE_JOELHOS);
            tecla("brancoModDefesa", true);
            ticks(60);
            assertTrue(j1().isRespirando(), "de joelhos");
            assertTrue(j1().isAjoelhado(), "continua de joelhos");
            tecla("brancoModDefesa", false);

            novo(InicioLuta.EM_PE);
            deitar(j1());
            ticks(40);
            tecla("brancoModDefesa", true);
            ticks(60);
            assertTrue(j1().isRespirando(), "deitado");
            assertTrue(j1().isDeitado(), "continua deitado, nao levanta");
            tecla("brancoModDefesa", false);
        });
    }

    @Test
    void recuperacaoMenorDeJoelhosEMenorAindaDeitado() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            double emPe = recuperouRespirando(30);
            novo(InicioLuta.DE_JOELHOS);
            double joelhos = recuperouRespirando(30);
            novo(InicioLuta.EM_PE);
            deitar(j1());
            ticks(40);
            double deitado = recuperouRespirando(30);
            assertTrue(emPe > 0 && joelhos > 0 && deitado > 0, emPe + " " + joelhos + " " + deitado);
            assertEquals(CustosFolego.RECUPERACAO_RESPIRANDO_AJOELHADO, joelhos / emPe, 0.05, "de joelhos ~70%");
            assertEquals(CustosFolego.RECUPERACAO_RESPIRANDO_DEITADO, deitado / emPe, 0.05, "deitado ~40%");
            assertTrue(deitado < joelhos && joelhos < emPe);
        });
    }

    @Test
    void andarRespirandoEMaisLentoEmCadaPostura() throws Exception {
        naEdt(() -> {
            for (boolean deitado : new boolean[] {false, true}) {
                novo(InicioLuta.DE_JOELHOS);
                if (deitado) {
                    j1().alternarDeitado();
                    deitar(j1());
                    ticks(40);
                }
                tecla("brancoEsquerda", true);
                int x = j1().getX();
                ticks(60);
                int normal = x - j1().getX();
                tecla("brancoModDefesa", true);
                ticks(2);
                x = j1().getX();
                ticks(60);
                int respirando = x - j1().getX();
                tecla("brancoModDefesa", false);
                tecla("brancoEsquerda", false);
                String p = deitado ? "deitado" : "de joelhos";
                assertTrue(normal > 0, p + " anda");
                assertTrue(respirando < normal, p + ": respirando " + respirando + " < " + normal);
                assertTrue(deitado ? j1().isDeitado() : j1().isAjoelhado(), p + ": a postura nao muda");
            }
        });
    }

    @Test
    void folegoNaoPassaDoMaximoEJ1EJ2RespiramIndependentes() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1().gastarFolegoEsforco(5.0, "teste");
            tecla("brancoModDefesa", true);
            ticks(600);
            assertTrue(j1().isRespirando());
            assertFalse(j2().isRespirando(), "o J2 nao respira pelo RT do J1");
            assertTrue(j1().getFolegoAtual() <= j1().getAtributos().getFolego() + 1e-9);
            tecla("pretoControleP", true);
            ticks(2);
            assertTrue(j2().isRespirando());
            tecla("brancoModDefesa", false);
            ticks(2);
            assertFalse(j1().isRespirando());
            assertTrue(j2().isRespirando(), "cada um no seu RT");
            tecla("pretoControleP", false);
        });
    }

    @Test
    void deitadoRtMaisDirecaoNaoViraGuardaNemRespira() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            deitar(j1());
            ticks(40);
            j1().gastarFolegoEsforco(5.0, "teste");
            tecla("brancoModDefesa", true);
            tecla("brancoPegada1", true);                        // RT + cima deitado
            ticks(5);
            double f = j1().getFolegoAtual();
            ticks(30);
            assertFalse(j1().getDefesa().isAtiva(), "sem guarda no solo");
            assertFalse(j1().isRespirando(), "com a direcao, a defesa vence (e nao respira)");
            double semRespirar = j1().getFolegoAtual() - f;      // so a recuperacao parada de sempre
            tecla("brancoPegada1", false);
            ticks(2);
            assertTrue(j1().isRespirando(), "solta a direcao: respira");
            double antes = j1().getFolegoAtual();
            ticks(30);
            double respirando = j1().getFolegoAtual() - antes;
            assertTrue(semRespirar < respirando / 3, "RT + direcao nao da a recuperacao da respiracao: "
                    + semRespirar + " vs " + respirando);
            assertTrue(j1().isDeitado());
        });
    }

    @Test
    void respirarAgarradoNaoQuebraAPegada() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            tecla("pretoControleP", true);                       // o receptor (J2) segura o RT
            ticks(30);
            assertTrue(s().isInteracaoEstabelecida(), "a pegada continua");
            tecla("pretoControleP", false);
        });
    }
}
