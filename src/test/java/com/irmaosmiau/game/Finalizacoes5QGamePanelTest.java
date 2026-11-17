package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q no GamePanel real: J2 finaliza pelo teclado virtual (E/Q + setas),
 * J1 defende e bate pelo teclado (4 + tecla), HUD da disputa, vitoria por
 * finalizacao na tela de fim, treino sem fim de luta e reiniciar luta.
 */
class Finalizacoes5QGamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void ctl(String tecla, boolean p) {
        tecla("pretoControle" + tecla, p);
    }

    /** J2 segura o modificador (E = LB/L1, Q = LT/L2) e toca a seta. */
    private void j2Familia(String mod, String seta) {
        ctl(mod, true);
        ticks(1);
        ctl(seta, true);
        ticks(1);
        ctl(seta, false);
        ctl(mod, false);
        ticks(1);
    }

    private void ateDisputa() {
        for (int i = 0; i < 60 && m().getAtaque().map(a -> a.fase() == MotorSolo.FaseFinalizacao.ENTRADA).orElse(false); i++) {
            ticks(1);
        }
    }

    @Test
    void j2FinalizaPeloTecladoVirtualELutaTermina() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.MONTADA, Lado.B, Lado.B);
            ticks(40);
            m().definirEstabilidade(80);
            j2Familia("E", "RIGHT");                            // L1 + -> = americana
            assertTrue(m().getAtaque().isPresent(), "americana pelo teclado virtual");
            assertEquals(Lado.B, m().getAtaque().get().atacante());
            ateDisputa();
            PainelDpadContextual.Quadro q1 = painel.montarQuadroDpad(SlotJogador.JOGADOR_1);
            assertTrue(q1.nomeCamada().startsWith("DEFENDA: AMERICANA"), q1.nomeCamada());
            assertEquals("PROTEGER BRAÇO", PainelDpadContextual.texto(q1, DirecaoTecnica.BAIXO));
            assertTrue(painel.montarQuadroDpad(SlotJogador.JOGADOR_2).nomeCamada().startsWith("ATACANDO AMERICANA"));
            for (int i = 0; i < 600 && !painel.isFimDeJogo(); i++) {
                ticks(1);
            }
            assertTrue(painel.isFimDeJogo());
            assertTrue(painel.getMensagemFim().startsWith("J2 VENCEU POR FINALIZAÇÃO · AMERICANA"), painel.getMensagemFim());
        });
    }

    @Test
    void j1DefendeEBatePeloTeclado() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.COSTAS, Lado.B, Lado.B);
            ticks(40);
            m().definirEstabilidade(70);
            j2Familia("Q", "UP");                               // L2 + ^ = mata-leao
            ateDisputa();
            double p = m().getAtaque().orElseThrow().pressao();
            toque("brancoPegada3");                             // J1: v = maos no pescoco
            assertTrue(m().getAtaque().orElseThrow().pressao() < p, "defendeu");
            tecla("brancoModProjecao", true);                   // J1: 4 + v = bater
            toque("brancoPegada3");
            tecla("brancoModProjecao", false);
            ticks(1);
            assertTrue(m().getVitoria().orElseThrow().rendicao());
            assertTrue(painel.isFimDeJogo());
            assertTrue(painel.getMensagemFim().contains("MATA-LEÃO (BATEU)"), painel.getMensagemFim());
        });
    }

    @Test
    void noTreinoAFinalizacaoSoAvisaEALutaSegue() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(2);
            m().forcarPosicao(Posicao.MONTADA, Lado.A, Lado.A);
            ticks(40);
            m().definirEstabilidade(80);
            m().comando(Lado.A, new ComandoSolo(Direcao.DIREITA, Familia.L1));
            for (int i = 0; i < 600 && painel.avisoAtual() == null || i < 600 && !painel.avisoAtual().contains("VENCEU"); i++) {
                ticks(1);
            }
            assertTrue(painel.avisoAtual().contains("J1 VENCEU POR FINALIZAÇÃO"), painel.avisoAtual());
            assertFalse(painel.isFimDeJogo(), "no treino a luta segue");
            assertTrue(m().getVitoria().isEmpty());
        });
    }

    @Test
    void reiniciarDepoisDaFinalizacaoLimpaTudo() throws Exception {
        AtomicInteger reinicios = new AtomicInteger();
        naEdt(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { },
                    reinicios::incrementAndGet);
            painel.pararLoopParaTestes();
            ticks(2);
            m().forcarPosicao(Posicao.COSTAS, Lado.A, Lado.A);
            ticks(40);
            m().definirEstabilidade(80);
            m().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));
            ateDisputa();
            m().comando(Lado.B, new ComandoSolo(Direcao.BAIXO, Familia.R1));   // bateu
            ticks(1);
            assertTrue(painel.isFimDeJogo());
            painel.reiniciarLuta();
            assertEquals(1, reinicios.get());
            assertTrue(m().getVitoria().isEmpty() && m().getAtaque().isEmpty());
            assertEquals(Posicao.EM_PE_SEPARADOS, m().getPosicao());
            assertFalse(j1().isVinculadoNoSolo() || j2().isVinculadoNoSolo(), "nenhum vinculo sobra");
        });
    }

    @Test
    void hudMostraGraduacaoEProibicaoSemPoluir() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.MONTADA, Lado.B, Lado.B);
            ticks(40);
            m().definirEstabilidade(80);
            j2().setGraduacao(com.irmaosmiau.regras.Graduacao.BRANCA);
            ctl("E", true);
            ticks(1);
            PainelDpadContextual.Quadro q = painel.montarQuadroDpad(SlotJogador.JOGADOR_2);
            assertEquals("KIMURA·Az", PainelDpadContextual.texto(q, DirecaoTecnica.BAIXO), "bloqueada pela graduacao");
            assertFalse(q.celulas().get(DirecaoTecnica.BAIXO).disponivelAgora());
            assertTrue(q.celulas().get(DirecaoTecnica.DIREITA).disponivelAgora(), "americana (branca) disponivel");
            ctl("E", false);
        });
    }
}
