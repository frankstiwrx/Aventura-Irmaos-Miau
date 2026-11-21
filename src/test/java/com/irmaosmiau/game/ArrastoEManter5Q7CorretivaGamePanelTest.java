package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.IntegridadeMembros;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.7 corretiva no GamePanel real: quem e arrastado continua deitado RETO (tronco e
 * cintura alinhados, pernas e bracos do tamanho de repouso) em pe, de joelhos, para tras e para a
 * frente; quem arrasta mostra o esforco sem deformar; e o MANTER A GUARDA tem microanimacao propria.
 */
class ArrastoEManter5Q7CorretivaGamePanelTest extends Base5OGamePanel {

    private static final double FOLGA = 2.5;

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void derrubarJ2() {
        j1Pega(1);
        tecla("brancoControle", true);
        tecla("brancoEsquerda", true);
        ticks(6);
        tecla("brancoEsquerda", false);
        tecla("brancoControle", false);
        m().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));
        ticks(80);
        assertTrue(j2().isDeitado(), "caiu");
    }

    private void segurar(boolean deJoelhos) {
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
        assertEquals(Lado.A, m().getSeguradorPernas().orElse(null));
    }

    /** Segmentos de bracos e pernas do tamanho de repouso (o rig do lutador). */
    private void segmentosDeRepouso(IrmaoMiau l, int[] p, String oque) {
        double[] rb = IntegridadeMembros.comprimentos(new int[30], l.getBracosEmRepouso());
        double[] rp = IntegridadeMembros.comprimentosPernas(new int[30], l.getPernasEmRepouso());
        double[] cb = IntegridadeMembros.comprimentos(p, l.getBracosEmRepouso());
        double[] cp = IntegridadeMembros.comprimentosPernas(p, l.getPernasEmRepouso());
        for (int k = 0; k < 4; k++) {
            assertTrue(Math.abs(rb[k] - cb[k]) <= FOLGA, oque + ": braco " + k + " " + rb[k] + " -> " + cb[k]);
            assertTrue(Math.abs(rp[k] - cp[k]) <= FOLGA, oque + ": perna " + k + " " + rp[k] + " -> " + cp[k]);
        }
    }

    private void arrastaConferindo(String tecla, int n, String oque) {
        tecla(tecla, true);
        for (int i = 0; i < n; i++) {
            ticks(1);
            int[] d = j2().copiarPoseCombateExterna();
            assertNotNull(d, oque + ": o arrastado e desenhado pela pose (deitado reto)");
            assertEquals(0, d[7], oque + ": tronco no quadril");
            assertEquals(0, d[8], oque);
            assertTrue(Math.abs(d[9] - d[6]) <= 3, oque + " tick " + i + ": cintura alinhada (" + d[9] + "/" + d[6] + ")");
            assertTrue(Math.abs(d[9]) <= 3, oque + " tick " + i + ": tronco reto " + d[9]);
            segmentosDeRepouso(j2(), d, oque + " arrastado tick " + i);
            int[] s = j1().copiarPoseCombateExterna();
            if (s != null) {
                double[] dv = IntegridadeMembros.desvios(new int[30], s, j1());
                assertTrue(dv[1] <= 0.5, oque + ": tronco de quem arrasta no quadril");
            }
        }
        tecla(tecla, false);
    }

    @Test
    void arrastarEmPeParaTrasEParaFrenteSemDobrarACintura() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            derrubarJ2();
            segurar(false);
            String longe = j2().getX() > j1().getX() ? "brancoEsquerda" : "brancoDireita";
            String perto = longe.equals("brancoEsquerda") ? "brancoDireita" : "brancoEsquerda";
            int x = j2().getX();
            arrastaConferindo(longe, 40, "para tras");
            assertTrue(Math.abs(j2().getX() - x) > 5, "arrastou");
            arrastaConferindo(perto, 30, "para a frente");
            assertTrue(m().getSeguradorPernas().isPresent(), "ainda segura");
        });
    }

    @Test
    void arrastarDeJoelhosComResistenciaESoltar() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            derrubarJ2();
            segurar(true);
            assertTrue(j1().isAjoelhado(), "segura de joelhos");
            String longe = j2().getX() > j1().getX() ? "brancoEsquerda" : "brancoDireita";
            int x = j2().getX();
            arrastaConferindo(longe, 30, "de joelhos");
            assertTrue(Math.abs(j2().getX() - x) >= 2, "de joelhos tambem arrasta (devagar)");
            m().comando(Lado.A, new ComandoSolo(Direcao.DIREITA, Familia.RS));      // solta
            for (int i = 0; i < 14; i++) {
                ticks(1);
                int[] d = j2().copiarPoseCombateExterna();
                if (d != null) {
                    assertTrue(Math.abs(d[9] - d[6]) <= 3, "soltando: cintura alinhada");
                }
            }
            assertTrue(m().getSeguradorPernas().isEmpty());
        });
    }
}
