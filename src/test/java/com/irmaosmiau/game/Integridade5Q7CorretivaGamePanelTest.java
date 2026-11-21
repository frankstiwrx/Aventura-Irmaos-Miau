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
 * Entrega 5Q.7 corretiva no GamePanel real: rasteira e FORCAR QUEDA que falham (sem defesa e com
 * RT), com J1 e com J2 atacando, repetidas e alternadas com o empurrao. Em TODOS os quadros, o
 * corpo inteiro do defensor (e do atacante) fica integro: bracos e pernas do mesmo tamanho, tronco
 * apoiado no quadril, cintura e torcao dentro dos limites; no fim volta a pegada limpa, sem acumular.
 */
class Integridade5Q7CorretivaGamePanelTest extends Base5OGamePanel {

    private static final double FOLGA_SEGMENTO = 2.5;

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void pega(Lado quem) {
        String pre = quem == Lado.A ? "branco" : "preto";
        String ir = quem == Lado.A ? "brancoDireita" : "pretoEsquerda";
        IrmaoMiau a = quem == Lado.A ? j1() : j2();
        IrmaoMiau b = quem == Lado.A ? j2() : j1();
        tecla(ir, true);
        for (int i = 0; i < 400 && !s().podeTentarAcao(a, b, "pegada_1"); i++) {
            ticks(1);
        }
        tecla(ir, false);
        ticks(2);
        toque(pre + "Pegada1");
        ticks(80);
        assertEquals(Posicao.EM_PE_PEGADA, m().getPosicao(), "pegada de " + quem);
    }

    /** Roda n ticks conferindo cada quadro dos dois lutadores contra a pose de referencia. */
    private void integro(IrmaoMiau l, int[] ref, int n, String oque) {
        for (int i = 0; i < n; i++) {
            ticks(1);
            int[] p = l.copiarPoseCombateExterna();
            if (p == null) {
                continue;
            }
            double[] d = IntegridadeMembros.desvios(ref, p, l);
            String q = oque + " tick " + i + ": ";
            assertTrue(d[0] <= FOLGA_SEGMENTO, q + "segmento mudou " + d[0]);
            assertTrue(d[1] <= 0.5, q + "tronco saiu do quadril " + d[1]);
            assertTrue(d[2] <= IntegridadeMembros.DESLOCAMENTO_MAXIMO_CINTURA + 1, q + "cintura " + d[2]);
            assertTrue(d[3] <= IntegridadeMembros.TORCAO_MAXIMA_TRONCO_CINTURA + 1, q + "torcao " + d[3]);
            assertTrue(d[4] <= IntegridadeMembros.INCLINACAO_MAXIMA_TRONCO + 9, q + "inclinacao " + d[4]);
        }
    }

    private void tentativa(Lado ini, Direcao d, Familia f, boolean rt, int[] refDef, String oque) {
        IrmaoMiau def = ini == Lado.A ? j2() : j1();
        MotorSolo.Resposta r = m().comando(ini, new ComandoSolo(d, f));
        assertTrue(r.aceito(), oque + ": " + r.motivo());
        if (rt) {
            ticks(2);
            tecla(ini == Lado.A ? "pretoModDefesa" : "brancoModDefesa", true);
        }
        integro(def, refDef, 60, oque);
        if (rt) {
            tecla(ini == Lado.A ? "pretoModDefesa" : "brancoModDefesa", false);
        }
        assertEquals(Posicao.EM_PE_PEGADA, m().getPosicao(), oque + ": nao derrubou");
        ticks(40);
    }

    private void sequencia(Lado ini) {
        novo(InicioLuta.EM_PE);
        pega(ini);
        IrmaoMiau def = ini == Lado.A ? j2() : j1();
        IrmaoMiau atk = ini == Lado.A ? j1() : j2();
        int[] refDef = def.copiarPoseCombateExterna();
        int[] refAtk = atk.copiarPoseCombateExterna();
        assertNotNull(refDef);
        for (int k = 0; k < 3; k++) {
            tentativa(ini, Direcao.CIMA, Familia.L2, false, refDef, ini + " rasteira " + k);
            assertArrayEquals(refDef, def.copiarPoseCombateExterna(), ini + ": rasteira " + k + " sem acumular");
        }
        tentativa(ini, Direcao.CIMA, Familia.L2, true, refDef, ini + " rasteira com RT");
        tentativa(ini, Direcao.BAIXO, Familia.L2, true, refDef, ini + " forcar com RT");
        assertArrayEquals(refDef, def.copiarPoseCombateExterna(), ini + ": forcar com RT sem acumular");
        assertArrayEquals(refAtk, atk.copiarPoseCombateExterna(), ini + ": atacante volta a pegada");
    }

    @Test
    void j1AtacandoRasteirasEForcarQuedaQueFalhamNaoDeformamNenhumQuadro() throws Exception {
        naEdt(() -> sequencia(Lado.A));
    }

    @Test
    void j2AtacandoRasteirasEForcarQuedaQueFalhamNaoDeformamNenhumQuadro() throws Exception {
        naEdt(() -> sequencia(Lado.B));
    }

    @Test
    void forcarQuedaQueSoDesequilibraEAlternanciaComEmpurraoMantemOCorpoIntegro() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            pega(Lado.A);
            int[] ref = j2().copiarPoseCombateExterna();
            MotorSolo.Resposta r = m().comando(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.L2));
            assertTrue(r.aceito(), r.motivo());
            integro(j2(), ref, 60, "forcar (desequilibra)");
            integro(j1(), j1().copiarPoseCombateExterna(), 1, "atacante");
            ticks(120);                                       // o desequilibrio da pegada termina
            assertArrayEquals(ref, j2().copiarPoseCombateExterna(), "volta a pegada limpa");
            m().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));     // rasteira
            integro(j2(), ref, 60, "rasteira depois do forcar");
            ticks(40);
            assertArrayEquals(ref, j2().copiarPoseCombateExterna(), "sem acumular");
        });
    }

    @Test
    void tentativaLogoDepoisDoDesequilibrioNaoGuardaAReacaoPassageiraNaPegada() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            pega(Lado.A);
            int[] ref = j2().copiarPoseCombateExterna();
            for (int k = 0; k < 3; k++) {
                m().comando(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.L2));     // desequilibra
                ticks(40);                                    // a reacao de desequilibrio da pegada ainda corre
                m().comando(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.L2));     // de novo, segurada com RT
                ticks(2);
                tecla("pretoModDefesa", true);
                integro(j2(), ref, 40, "forcar segurado durante o desequilibrio " + k);
                tecla("pretoModDefesa", false);
                assertEquals(Posicao.EM_PE_PEGADA, m().getPosicao(), "segurou " + k);
                ticks(300);                                   // a base volta (regra): a proxima rodada parte igual
            }
            ticks(240);
            assertArrayEquals(ref, j2().copiarPoseCombateExterna(),
                    "a pegada volta limpa (a reacao passageira nao vira parte dela)");
        });
    }

    @Test
    void quedaQueEntraContinuaDerrubando() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            pega(Lado.A);
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
