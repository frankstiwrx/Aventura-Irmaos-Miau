package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.hud.PlacarLuta;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.regras.ArbitroJiuJitsu;
import com.irmaosmiau.regras.Graduacao;
import com.irmaosmiau.regras.ModoLuta;
import com.irmaosmiau.ui.SelecaoPartidaPanelAcesso;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** Modo PONTOS + FINALIZACAO no GamePanel: tempos livres, pausa, placar, fim por tempo e por finalizacao. */
class PontosEFinalizacaoGamePanelTest extends Base5OGamePanel {

    private static final int MS = 16;

    private void partida(int segundos, ModoLuta modo) {
        novo(InicioLuta.EM_PE);
        painel.configurarPartida(RegistroCenarios.ID_PADRAO, segundos);
        painel.configurarModoLuta(modo);
        ticks(2);
    }

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    /** Uma tecnica real do motor, ate se resolver. */
    private void executar(Lado l, String id, double estabilidade) {
        m().definirEstabilidade(estabilidade);
        m().comando(l, CatalogoTecnicasSolo.referencia().exigir(id).comandos().get(0));
        for (int i = 0; i < 200 && m().getTentativa().isPresent(); i++) {
            ticks(1);
        }
        ticks(1);
    }

    // 14-17 + 2: a lista exata, com o tempo mostrado em minutos e segundos (90 s = 1:30, sem arredondar)
    @Test
    void temposOferecidos() {
        assertEquals(Arrays.toString(new int[] {60, 90, 120, 180, 240, 300, 360, 420, 480, 540, 600, 0}),
                Arrays.toString(SelecaoPartidaPanelAcesso.tempos()));
        assertEquals("1 MIN", SelecaoPartidaPanelAcesso.texto(60));
        assertEquals("1 MIN 30 S", SelecaoPartidaPanelAcesso.texto(90));
        assertEquals("10 MIN", SelecaoPartidaPanelAcesso.texto(600));
        assertEquals("SEM LIMITE", SelecaoPartidaPanelAcesso.texto(0));
        assertEquals("1:30", PlacarLuta.relogio(90));
        assertEquals("LIVRE", PlacarLuta.relogio(-1));
    }

    // 14-17: cada tempo acaba exatamente na hora (ticks de 16 ms), uma vez so, sem contagem negativa
    @Test
    void cadaTempoAcabaNaHoraCerta() throws Exception {
        naEdt(() -> {
            for (int t : SelecaoPartidaPanelAcesso.tempos()) {
                if (t == 0) {
                    continue;
                }
                partida(t, ModoLuta.FINALIZACAO);
                assertEquals(t, painel.getSegundosRestantes(), t + " s no comeco");
                long total = (long) t * 1000 / MS;               // 90 s = 5625 ticks
                painel.adiantarTempoDeLutaParaTestes(total - 2 - 2);     // 2 ticks da partida(); fica em total - 2
                ticks(1);
                assertFalse(painel.isFimDeJogo(), t + " s: um tick antes ainda nao acabou");
                assertEquals(1, painel.getSegundosRestantes(), "o ultimo segundo aparece como 0:01");
                ticks(1);
                assertTrue(painel.isFimDeJogo(), t + " s: acabou");
                assertEquals(0, painel.getSegundosRestantes(), "nunca negativo");
                assertTrue(painel.getMensagemFim().startsWith("TEMPO ESGOTADO"), painel.getMensagemFim());
                ticks(10);
                assertEquals(0, painel.getSegundosRestantes());
            }
        });
    }

    // 13: sem limite nunca acaba pelo relogio
    @Test
    void semLimiteNaoAcabaPeloRelogio() throws Exception {
        naEdt(() -> {
            partida(0, ModoLuta.PONTOS_E_FINALIZACAO);
            assertEquals(-1, painel.getSegundosRestantes());
            painel.adiantarTempoDeLutaParaTestes(1_000_000);
            ticks(5);
            assertFalse(painel.isFimDeJogo());
            assertTrue(painel.getArbitro().isPresent(), "o placar existe mesmo sem limite");
        });
    }

    // 20: a pausa para o relogio e a arbitragem
    @Test
    void pausaParaORelogio() throws Exception {
        naEdt(() -> {
            partida(60, ModoLuta.PONTOS_E_FINALIZACAO);
            ticks(63);                                        // ~1 s
            int antes = painel.getSegundosRestantes();
            painel.alternarPausaParaTestes();
            assertTrue(painel.isPausado());
            ticks(500);
            assertEquals(antes, painel.getSegundosRestantes(), "pausado o relogio nao anda");
            painel.alternarPausaParaTestes();
            ticks(200);
            assertTrue(painel.getSegundosRestantes() < antes, "retomou");
        });
    }

    // 18 + 19: o tempo nao depende da faixa
    @Test
    void tempoNaoDependeDaFaixa() throws Exception {
        naEdt(() -> {
            for (Graduacao g : new Graduacao[] {Graduacao.BRANCA, Graduacao.PRETA}) {
                for (int t : new int[] {60, 600}) {
                    novo(InicioLuta.EM_PE);
                    j1().setGraduacao(g);
                    j2().setGraduacao(g);
                    painel.configurarPartida(RegistroCenarios.ID_PADRAO, t);
                    painel.configurarModoLuta(ModoLuta.PONTOS_E_FINALIZACAO);
                    assertEquals(t, painel.getSegundosRestantes(), g + " com " + t + " s");
                    j1().setGraduacao(Graduacao.AZUL);
                    assertEquals(t, painel.getSegundosRestantes(), "mudar a faixa nao muda o tempo");
                }
            }
        });
    }

    // 11: FINALIZACAO continua sem pontos tecnicos
    @Test
    void finalizacaoSemPontos() throws Exception {
        naEdt(() -> {
            partida(0, ModoLuta.FINALIZACAO);
            assertTrue(painel.getArbitro().isEmpty());
            m().forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
            ticks(40);
            executar(Lado.A, CatalogoTecnicasSolo.MONTAR, 95);
            ticks(400);
            assertTrue(painel.getArbitro().isEmpty(), "nenhum placar");
        });
    }

    // 5 + 12 + 21: montada estabilizada vale 4 e o fim do tempo da a vitoria por pontos
    @Test
    void pontosDecidemNoFimDoTempo() throws Exception {
        naEdt(() -> {
            partida(60, ModoLuta.PONTOS_E_FINALIZACAO);
            m().forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
            ticks(40);
            executar(Lado.A, CatalogoTecnicasSolo.MONTAR, 95);
            ArbitroJiuJitsu arb = painel.getArbitro().orElseThrow();
            ticks(arb.getTicksEstabilizacao() + 5);
            assertEquals(4, arb.getPlacar().pontos(Lado.A));
            assertTrue(painel.avisoAtual() != null && painel.avisoAtual().contains("MONTADA +4"), painel.avisoAtual());
            BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            painel.setSize(800, 450);
            painel.paintComponent(g);                         // o placar desenha
            g.dispose();
            painel.adiantarTempoDeLutaParaTestes(60_000 / MS);
            ticks(1);
            assertTrue(painel.isFimDeJogo());
            assertTrue(painel.getMensagemFim().contains("J1 VENCEU POR PONTOS"), painel.getMensagemFim());
            assertTrue(painel.getMensagemFim().contains("PLACAR 4 x 0"), painel.getMensagemFim());
        });
    }

    // 10: finalizacao encerra na hora, com o placar na mensagem
    @Test
    void finalizacaoEncerraNaHora() throws Exception {
        naEdt(() -> {
            partida(0, ModoLuta.PONTOS_E_FINALIZACAO);
            m().forcarPosicao(Posicao.GUARDA, Lado.A, Lado.B);
            ticks(40);
            m().definirEstabilidade(95);
            j2().setGraduacao(Graduacao.PRETA);
            m().comando(Lado.B, new com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo(
                    com.irmaosmiau.posicional.TecnicaSolo.Direcao.CIMA, com.irmaosmiau.posicional.TecnicaSolo.Familia.L2));
            for (int i = 0; i < 40 && m().getAtaque().isPresent()
                    && m().getAtaque().get().fase() == MotorSolo.FaseFinalizacao.ENTRADA; i++) {
                ticks(1);
            }
            m().comando(Lado.A, new com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo(
                    com.irmaosmiau.posicional.TecnicaSolo.Direcao.ESQUERDA,
                    com.irmaosmiau.posicional.TecnicaSolo.Familia.RS));   // J1 bate
            ticks(2);
            assertTrue(painel.isFimDeJogo());
            assertTrue(painel.getMensagemFim().contains("J2 VENCEU POR FINALIZAÇÃO"), painel.getMensagemFim());
            assertTrue(painel.getMensagemFim().contains("PLACAR 0 x 0"), painel.getMensagemFim());
        });
    }

    // puxar a guarda (deitar) nao e queda de quem fica em pe (4.1.11)
    @Test
    void puxarAGuardaNaoEQueda() throws Exception {
        naEdt(() -> {
            partida(0, ModoLuta.PONTOS_E_FINALIZACAO);
            j2().alternarDeitado();
            ticks(40);
            assertEquals(Lado.A, m().getEstado().getPorCima().orElseThrow(), "o J1 ficou em pe por cima");
            ticks(400);
            assertEquals(0, painel.getArbitro().orElseThrow().getPlacar().pontos(Lado.A));
            assertEquals(0, painel.getArbitro().orElseThrow().getPlacar().vantagens(Lado.A));
        });
    }
}
