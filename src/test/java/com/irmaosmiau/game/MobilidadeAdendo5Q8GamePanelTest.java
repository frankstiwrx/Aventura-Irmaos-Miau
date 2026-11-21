package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;
import com.irmaosmiau.entities.TipoAcrobacia;

import org.junit.jupiter.api.Test;

/**
 * Adendo da 5Q.8: o rolamento tecnico de joelhos usa a velocidade do rolamento deitado (76 -> 37 ticks,
 * mesma distancia e postura) e o sprawl fica 20% mais lento (32 -> 40 ticks) com a MESMA protecao contra
 * as quedas (os primeiros 32 ticks).
 */
class MobilidadeAdendo5Q8GamePanelTest extends Base5OGamePanel {

    private void ctl(String tecla, boolean p) {
        tecla("pretoControle" + tecla, p);
    }

    @Test
    void parametrosCentralizados() {
        assertEquals(IrmaoMiau.MULTIPLICADOR_ROLAMENTO_DEITADO, IrmaoMiau.MULTIPLICADOR_ROLAMENTO_JOELHOS, 1e-9);
        assertEquals(37, IrmaoMiau.duracaoRolamento(EstadoCorporal.DEITADO), "o deitado nao mudou");
        assertEquals(IrmaoMiau.duracaoRolamento(EstadoCorporal.DEITADO), IrmaoMiau.duracaoRolamento(EstadoCorporal.AJOELHADO));
        assertEquals(22, IrmaoMiau.duracaoRolamento(EstadoCorporal.EM_PE), "em pe nao mudou");
        assertEquals(32, IrmaoMiau.DURACAO_SPRAWL_5Q8);
        assertEquals(0.8, IrmaoMiau.FATOR_VELOCIDADE_SPRAWL, 1e-9);
        assertEquals(40, IrmaoMiau.duracaoSprawl(), "32 / 0,8");
        assertEquals(32, IrmaoMiau.TICKS_PROTECAO_SPRAWL, "a protecao contra as quedas nao mudou");
        assertEquals(TipoAcrobacia.DURACAO_MINIMA_TICKS, 16, "acrobacias nao mudaram");
    }

    @Test
    void rolamentoDeJoelhosNosDoisJogadoresComAMesmaDistancia() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.DE_JOELHOS);
            int x1 = j1().getX();
            tecla("brancoEsquerda", true);
            toque("brancoRolar");
            tecla("brancoEsquerda", false);
            assertTrue(j1().isRolando());
            assertEquals(37, j1().getDuracaoRolamentoAtual(), "J1");
            int x2 = j2().getX();
            ctl("D", true);
            ctl("M", true);                                  // J2: R3 + direcao
            ticks(1);
            ctl("M", false);
            ctl("D", false);
            ticks(1);
            assertTrue(j2().isRolando(), "J2 rola de joelhos");
            assertEquals(37, j2().getDuracaoRolamentoAtual(), "J2");
            ticks(40);
            assertFalse(j1().isRolando() || j2().isRolando(), "37 ticks bastam");
            assertTrue(j1().isAjoelhado() && j2().isAjoelhado(), "a postura de joelhos continua");
            assertEquals(154, x1 - j1().getX(), 3, "J1: a mesma distancia (22 x 7)");
            assertEquals(154, j2().getX() - x2, 3, "J2: a mesma distancia");
        });
    }

    @Test
    void sprawlMaisLentoSemCongelarComAMesmaProtecao() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("brancoModDefesa", true);
            toque("brancoPegada3");                          // RT + baixo = sprawl
            tecla("brancoModDefesa", false);
            assertTrue(j1().isFazendoSprawl());
            ctl("P", true);
            ctl("DOWN", true);                               // J2: RT + RS v
            ticks(1);
            ctl("DOWN", false);
            ctl("P", false);
            assertTrue(j2().isFazendoSprawl(), "J2 tambem");
            double antes = j1().progressoSprawl();
            int protegendo = 0;
            int duracao = 1;
            while (j1().isFazendoSprawl() && duracao < 80) {
                if (j1().isSprawlProtegendo()) {
                    protegendo++;
                }
                ticks(1);
                duracao++;
                if (j1().isFazendoSprawl()) {
                    assertTrue(j1().progressoSprawl() > antes, "a animacao anda todo tick (sem congelar)");
                    antes = j1().progressoSprawl();
                }
            }
            assertEquals(IrmaoMiau.duracaoSprawl(), duracao, 2, "~40 ticks");
            assertEquals(IrmaoMiau.TICKS_PROTECAO_SPRAWL, protegendo, 2, "protege os mesmos ~32 ticks de antes");
            assertFalse(j1().isDeitado() || j1().isAjoelhado(), "termina em pe, como antes");
        });
    }
}
