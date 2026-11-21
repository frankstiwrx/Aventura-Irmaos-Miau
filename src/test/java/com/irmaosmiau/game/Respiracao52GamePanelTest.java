package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.CustosFolego;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5O.2: respirar em pe, de joelhos e deitado, com recuperacao e movimento por postura.
 * Complemento 5Q.8 (pedido): o botao de respirar saiu do RT sozinho e foi para o Y/Triangulo (J1 nas teclas:
 * a tecla de respirar; J2 no teclado-controle: I = Y). A mecanica e os numeros nao mudaram.
 */
class Respiracao52GamePanelTest extends Base5OGamePanel {

    private static void deitar(IrmaoMiau l) {
        if (l.isAjoelhado()) {
            l.alternarDeitado();
        }
        l.alternarDeitado();
        assertTrue(l.isDeitado());
    }

    /** Folego recuperado pelo J1 segurando o respirar por n ticks, partindo de 10 abaixo do maximo. */
    private double recuperouRespirando(int n) {
        j1().gastarFolegoEsforco(10.0, "teste");
        ticks(1);
        double f = j1().getFolegoAtual();
        tecla("brancoRespirar", true);
        ticks(n);
        tecla("brancoRespirar", false);
        return j1().getFolegoAtual() - f;
    }

    @Test
    void rtRespiraNasTresPosturasSemMudarAPostura() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("brancoRespirar", true);
            ticks(3);
            assertTrue(j1().isRespirando(), "em pe");
            tecla("brancoRespirar", false);

            novo(InicioLuta.DE_JOELHOS);
            tecla("brancoRespirar", true);
            ticks(60);
            assertTrue(j1().isRespirando(), "de joelhos");
            assertTrue(j1().isAjoelhado(), "continua de joelhos");
            tecla("brancoRespirar", false);

            novo(InicioLuta.EM_PE);
            deitar(j1());
            ticks(40);
            tecla("brancoRespirar", true);
            ticks(60);
            assertTrue(j1().isRespirando(), "deitado");
            assertTrue(j1().isDeitado(), "continua deitado, nao levanta");
            tecla("brancoRespirar", false);
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
                tecla("brancoRespirar", true);
                ticks(2);
                x = j1().getX();
                ticks(60);
                int respirando = x - j1().getX();
                tecla("brancoRespirar", false);
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
            tecla("brancoRespirar", true);
            ticks(600);
            assertTrue(j1().isRespirando());
            assertFalse(j2().isRespirando(), "o J2 nao respira pelo botao do J1");
            assertTrue(j1().getFolegoAtual() <= j1().getAtributos().getFolego() + 1e-9);
            tecla("pretoControleI", true);
            ticks(2);
            assertTrue(j2().isRespirando());
            tecla("brancoRespirar", false);
            ticks(2);
            assertFalse(j1().isRespirando());
            assertTrue(j2().isRespirando(), "cada um no seu botao");
            tecla("pretoControleI", false);
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
            // complemento 5Q.8 (pedido): o RT sozinho nao respira mais; o botao de respirar sim
            assertFalse(j1().isRespirando(), "RT sozinho nao respira");
            tecla("brancoModDefesa", false);
            tecla("brancoRespirar", true);
            ticks(2);
            assertTrue(j1().isRespirando(), "o botao de respirar respira");
            double antes = j1().getFolegoAtual();
            ticks(30);
            double respirando = j1().getFolegoAtual() - antes;
            assertTrue(semRespirar < respirando / 3, "RT + direcao nao da a recuperacao da respiracao: "
                    + semRespirar + " vs " + respirando);
            assertTrue(j1().isDeitado());
            tecla("brancoRespirar", false);
        });
    }

    @Test
    void respirarAgarradoNaoQuebraAPegada() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            tecla("pretoControleI", true);                       // o receptor (J2) segura o Y (I)
            ticks(30);
            assertTrue(s().isInteracaoEstabelecida(), "a pegada continua");
            tecla("pretoControleI", false);
        });
    }
}
