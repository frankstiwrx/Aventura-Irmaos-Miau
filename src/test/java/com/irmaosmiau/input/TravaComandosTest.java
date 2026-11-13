package com.irmaosmiau.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.ActionMap;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

/**
 * Entrega 3A: teclas seguradas durante o bloqueio da animacao nao podem
 * agir quando o bloqueio termina.
 */
class TravaComandosTest {

    // =====================================================
    // LOGICA PURA
    // =====================================================
    @Test
    void comandoTravadoSoVoltaDepoisDeSolto() {
        TravaComandos t = new TravaComandos();
        t.pressionar("brancoDireita");
        t.travarPressionados("branco");

        assertTrue(t.isTravado("brancoDireita"));
        t.pressionar("brancoDireita");          // auto-repeat do teclado
        assertTrue(t.isTravado("brancoDireita"));

        t.soltar("brancoDireita");
        assertTrue(t.isLivre("brancoDireita"));
        t.pressionar("brancoDireita");
        assertTrue(t.isLivre("brancoDireita"));
    }

    @Test
    void travaSoAfetaOPrefixoEAsTeclasSeguradas() {
        TravaComandos t = new TravaComandos();
        t.pressionar("brancoPular");
        t.pressionar("pretoPular");
        t.travarPressionados("branco");

        assertTrue(t.isTravado("brancoPular"));
        assertTrue(t.isLivre("pretoPular"));
        assertTrue(t.isLivre("brancoDireita"), "tecla nao segurada nao trava");
    }

    // =====================================================
    // INPUTHANDLER REAL (acoes de teclado acionadas pelo ActionMap)
    // =====================================================
    private static void tecla(ActionMap mapa, String nome, boolean pressionada) {
        mapa.get(nome + (pressionada ? "Pressionada" : "Solta")).actionPerformed(null);
    }

    @Test
    void inputHandlerIgnoraTeclaSeguradaDuranteOBloqueio() {
        JPanel painel = new JPanel();
        InputHandler input = new InputHandler(painel);
        ActionMap mapa = painel.getActionMap();

        // Branco chega andando para a direita e aperta H.
        tecla(mapa, "brancoDireita", true);
        assertTrue(input.isDireita());

        // 30 frames de bloqueio: o GamePanel chama a trava a cada frame.
        for (int f = 0; f < 30; f++) {
            input.travarComandosPressionadosBranco();
            if (f == 12) {
                tecla(mapa, "brancoPular", true);   // apertado durante o bloqueio
            }
        }

        // Fim do bloqueio: nada segurado vale.
        assertFalse(input.isDireita(), "direita segurada nao pode andar sozinha");
        assertFalse(input.isPular(), "pulo apertado no bloqueio nao pode disparar depois");

        // Soltar e apertar de novo volta a funcionar normalmente.
        tecla(mapa, "brancoDireita", false);
        assertFalse(input.isDireita());
        tecla(mapa, "brancoDireita", true);
        assertTrue(input.isDireita());

        tecla(mapa, "brancoPular", false);
        tecla(mapa, "brancoPular", true);
        assertTrue(input.isPular());
    }

    @Test
    void travaDoBrancoNaoAfetaOPretoEViceVersa() {
        JPanel painel = new JPanel();
        InputHandler input = new InputHandler(painel);
        ActionMap mapa = painel.getActionMap();

        tecla(mapa, "brancoEsquerda", true);
        tecla(mapa, "pretoEsquerda", true);

        input.travarComandosPressionadosBranco();
        assertFalse(input.isEsquerda());
        assertTrue(input.isPretoEsquerda());

        input.travarComandosPressionadosPreto();
        assertFalse(input.isPretoEsquerda());

        tecla(mapa, "pretoEsquerda", false);
        tecla(mapa, "pretoEsquerda", true);
        assertTrue(input.isPretoEsquerda());
        assertFalse(input.isEsquerda(), "Branco continua travado ate soltar");
    }

    @Test
    void semBloqueioOControleFuncionaComoAntes() {
        JPanel painel = new JPanel();
        InputHandler input = new InputHandler(painel);
        ActionMap mapa = painel.getActionMap();

        String[] branco = {"brancoEsquerda", "brancoDireita", "brancoCima", "brancoBaixo",
            "brancoPular", "brancoAgachar", "brancoCorrer", "brancoPularCurto",
            "brancoRespirar", "brancoRolar", "brancoDeitar", "brancoSprawl", "brancoPegada1"};
        for (String nome : branco) {
            tecla(mapa, nome, true);
        }
        assertTrue(input.isEsquerda() && input.isDireita() && input.isCima() && input.isBaixo()
                && input.isPular() && input.isAgachar() && input.isCorrer() && input.isPularCurto()
                && input.isRespirar() && input.isRolar() && input.isDeitar() && input.isSprawl()
                && input.isPegada1());
        for (String nome : branco) {
            tecla(mapa, nome, false);
        }
        assertFalse(input.isEsquerda() || input.isPular() || input.isPegada1());
    }
}
