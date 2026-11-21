package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoAcrobacia;
import com.irmaosmiau.regras.Graduacao;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.7 corretiva: estrela e mortais mais ageis. Execucao-base mais curta e encurtada pela
 * agilidade/velocidade (minimo legivel), recarga-base menor e o R3 apertado um pouco antes da
 * acrobacia anterior acabar fica guardado (antes era descartado). Rolamento de joelhos e sprawl iguais.
 */
class AcrobaciasCorretiva5Q7GamePanelTest extends Base5OGamePanel {

    @Test
    void execucaoMaisCurtaPelaAgilidadeComDuracaoMinima() {
        assertEquals(26, TipoAcrobacia.ESTRELA_ESQUERDA.getDuracaoTicks(), "estrela 30 -> 26");
        assertEquals(28, TipoAcrobacia.MORTAL_FRENTE.getDuracaoTicks(), "mortal para a frente 32 -> 28");
        assertEquals(30, TipoAcrobacia.MORTAL_TRAS.getDuracaoTicks(), "mortal para tras 34 -> 30");
        IrmaoMiau lento = new IrmaoMiauBranco(100, 300, 0.7);
        IrmaoMiau agil = new IrmaoMiauPreto(300, 300, 0.7);
        agil.setGraduacao(Graduacao.PRETA);
        for (TipoAcrobacia t : TipoAcrobacia.values()) {
            int dl = lento.duracaoAcrobacia(t);
            int da = agil.duracaoAcrobacia(t);
            assertTrue(da < dl, t + ": o agil executa mais rapido " + da + " x " + dl);
            assertTrue(da >= TipoAcrobacia.DURACAO_MINIMA_TICKS, t + ": duracao minima legivel");
            assertTrue(dl <= t.getDuracaoTicks(), t + ": nunca mais lenta que a base");
        }
        assertTrue(agil.recargaAcrobaciaDinamica() <= 6, "faixa preta agil quase sem espera: " + agil.recargaAcrobaciaDinamica());
        assertTrue(lento.recargaAcrobaciaDinamica() <= 14, "branca descansada: " + lento.recargaAcrobaciaDinamica());
    }

    @Test
    void aMesmaDistanciaEmMenosTempo() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j2().setGraduacao(Graduacao.PRETA);
            int x0 = j2().getX();
            assertTrue(j2().iniciarAcrobacia(TipoAcrobacia.MORTAL_FRENTE));
            int d = j2().getDuracaoAcrobaciaAtual();
            ticks(d + 1);
            assertFalse(j2().isEmAcrobacia());
            assertEquals(60, Math.abs(j2().getX() - x0), 3, "mortal para a frente continua 60 px");
        });
    }

    @Test
    void r3ApertadoNoFimDaAcrobaciaFicaGuardadoEEncadeia() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            painel.configurarPartida("dojo", 0);
            j2().setRecursosInfinitos(true, true);
            tecla("pretoCima", true);                           // estrela (profundidade)
            toque("pretoRolar");
            assertTrue(j2().isEmAcrobacia(), "primeira estrela");
            int d = j2().getDuracaoAcrobaciaAtual();
            ticks(d - 4);
            toque("pretoRolar");                                // cedo demais: antes era descartado
            assertTrue(painel.temAcrobaciaPendente(SlotJogador.JOGADOR_2), "guardado");
            boolean segunda = false;
            for (int i = 0; i < 4 + j2().recargaAcrobaciaDinamica() + 3 && !segunda; i++) {
                ticks(1);
                segunda = j2().isEmAcrobacia() && j2().getRecargaAcrobacia() == 0 && i > 3;
            }
            tecla("pretoCima", false);
            assertTrue(segunda, "a segunda sai sozinha assim que pode, sem apertar de novo");
        });
    }

    @Test
    void r3GuardadoExpiraSeNaoDerParaFazer() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("pretoCima", true);
            toque("pretoRolar");
            toque("pretoRolar");
            tecla("pretoCima", false);
            j2().alternarDeitado();                             // deixou de poder (deitou)
            ticks(GamePanel.JANELA_ACROBACIA_PENDENTE_TICKS + 40);
            assertFalse(painel.temAcrobaciaPendente(SlotJogador.JOGADOR_2), "o comando guardado nao fica pendurado");
        });
    }

    @Test
    void rolamentoDeJoelhosESprawlNaoMudaram() {
        // 5Q.8 (pedido): o rolamento de joelhos ficou mais 40% mais lento (46 -> 76)
        assertEquals(76, IrmaoMiau.duracaoRolamento(EstadoCorporal.AJOELHADO), "rolamento de joelhos da 5Q.8");
        assertEquals(37, IrmaoMiau.duracaoRolamento(EstadoCorporal.DEITADO));
        assertEquals(22, IrmaoMiau.duracaoRolamento(EstadoCorporal.EM_PE));
        assertEquals(32, IrmaoMiau.duracaoSprawl());
    }
}
