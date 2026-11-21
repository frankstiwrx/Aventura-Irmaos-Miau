package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.IntegridadeMembros;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.8: quem arrasta pelas pernas ANDA de verdade (a pose muda com os passos, em pe e de
 * joelhos: nada de pose parada deslizando), sem mudar o tamanho das pernas; quem e arrastado mostra
 * os bracos ficando para tras so enquanto e puxado.
 */
class Arrasto5Q8GamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void preparar(boolean deJoelhos) {
        novo(InicioLuta.EM_PE);
        j1Pega(1);
        tecla("brancoControle", true);
        tecla("brancoEsquerda", true);
        ticks(6);
        tecla("brancoEsquerda", false);
        tecla("brancoControle", false);
        m().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));
        ticks(80);
        String ate = j2().getX() > j1().getX() ? "brancoDireita" : "brancoEsquerda";
        for (int i = 0; i < 200 && Math.abs(j1().getX() - j2().getX()) > 90; i++) {
            tecla(ate, true);
            ticks(1);
        }
        tecla(ate, false);
        ticks(2);
        if (deJoelhos) {
            j1().ajoelhar();
            ticks(30);
        }
        MotorSolo.Resposta r = m().comando(Lado.A, new ComandoSolo(Direcao.DIREITA, Familia.RS));
        assertEquals(CatalogoTecnicasSolo.SEGURAR_PERNAS, r.tecnica().id(), r.motivo());
        ticks(25);
    }

    private void quemArrastaAnda(boolean deJoelhos) {
        preparar(deJoelhos);
        int[] parado = j1().copiarPoseCombateExterna();
        int[] bracoParado = j2().copiarPoseCombateExterna();
        String longe = j2().getX() > j1().getX() ? "brancoEsquerda" : "brancoDireita";
        tecla(longe, true);
        int maiorMudanca = 0;
        int bracosParaTras = 0;
        double[] pernasRef = IntegridadeMembros.comprimentosPernas(parado, j1().getPernasEmRepouso());
        for (int i = 0; i < 40; i++) {
            ticks(1);
            int[] p = j1().copiarPoseCombateExterna();
            for (int k : new int[] {22, 23, 24, 25, 26, 27, 28, 29}) {
                maiorMudanca = Math.max(maiorMudanca, Math.abs(p[k] - parado[k]));
            }
            double[] c = IntegridadeMembros.comprimentosPernas(p, j1().getPernasEmRepouso());
            for (int k = 0; k < 4; k++) {
                assertTrue(Math.abs(c[k] - pernasRef[k]) <= 2.5, "passo sem esticar a perna " + k + " tick " + i);
            }
            int[] d = j2().copiarPoseCombateExterna();
            bracosParaTras = Math.max(bracosParaTras, bracoParado[17] - d[17]);
        }
        tecla(longe, false);
        assertTrue(maiorMudanca >= 3, (deJoelhos ? "de joelhos" : "em pe") + ": os passos aparecem (" + maiorMudanca + ")");
        assertTrue(bracosParaTras >= 3, "o arrastado deixa os bracos para tras enquanto e puxado (" + bracosParaTras + ")");
        ticks(40);
        int[] depois = j2().copiarPoseCombateExterna();
        assertTrue(Math.abs(depois[17] - bracoParado[17]) <= 1, "parou de puxar: os bracos voltam");
    }

    @Test
    void emPeQuemArrastaDaPassosEQuemEArrastadoReage() throws Exception {
        naEdt(() -> quemArrastaAnda(false));
    }

    @Test
    void deJoelhosQuemArrastaAndaDeJoelhosEQuemEArrastadoReage() throws Exception {
        naEdt(() -> quemArrastaAnda(true));
    }
}
