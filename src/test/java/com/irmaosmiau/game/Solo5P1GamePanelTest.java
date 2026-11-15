package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.hud.AvisosCombate;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5P.1 no GamePanel real: solo livre na HUD (sem "EM BREVE"), titulos
 * curtos, cruz de aterrissagem, RT (borda = base; segurado = respirar),
 * avisos no topo so com resultados e REINICIAR LUTA na pausa.
 */
class Solo5P1GamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void j1Rb(String direcao) {
        tecla("brancoModProjecao", true);
        toque(direcao);
        tecla("brancoModProjecao", false);
    }

    private String titulo(SlotJogador s) {
        return painel.montarQuadroDpad(s).nomeCamada();
    }

    // ---------------- 1. solo livre: J1 ou J2 deita e o HUD mostra acoes
    @Test
    void j1DeitaEOsDoisVeemComandosDoSoloLivre() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1().alternarDeitado();
            ticks(30);
            assertEquals(Posicao.EM_PE_CONTRA_SOLO, m().getPosicao());
            assertTrue(titulo(SlotJogador.JOGADOR_1).startsWith("SOLO LIVRE · NO CHÃO"), titulo(SlotJogador.JOGADOR_1));
            assertTrue(titulo(SlotJogador.JOGADOR_2).startsWith("SOLO LIVRE · EM PÉ"), titulo(SlotJogador.JOGADOR_2));
            for (SlotJogador s : SlotJogador.values()) {
                assertFalse(titulo(s).contains("EM BREVE"), titulo(s));
                assertFalse(painel.montarQuadroDpad(s).celulas().isEmpty(), s + " tem comandos");
            }
            assertFalse(m().conectado(j1()), "sem vinculo a distancia");
            assertEquals("J1 foi ao chão.", painel.avisoAtual());
        });
    }

    @Test
    void j2DeitaPeloTecladoControleEJ1VeOSoloLivre() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j2().alternarDeitado();
            ticks(30);
            assertEquals(Lado.A, m().getEstado().getPorCima().orElseThrow());
            assertTrue(titulo(SlotJogador.JOGADOR_1).startsWith("SOLO LIVRE · EM PÉ"));
            assertTrue(titulo(SlotJogador.JOGADOR_2).contains("RT AR"), "dica de respirar no solo");
        });
    }

    @Test
    void titulosCurtosCabemNoPainel() {
        for (Posicao p : Posicao.values()) {
            String n = GamePanel.nomeCurto(p);
            assertTrue(n.length() <= 10, p + " -> " + n);
        }
        assertEquals("100 KG", GamePanel.nomeCurto(Posicao.CONTROLE_LATERAL));
        assertEquals("G. ABERTA", GamePanel.nomeCurto(Posicao.GUARDA_ABERTA));
    }

    // ---------------- 6. aterrissagem clara: quem cai ve como receber
    @Test
    void quemCaiVeACruzDeAterrissagemEFechaAGuarda() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            j1Rb("brancoPegada2");                              // queda com a pegada
            tecla("brancoDireita", true);                       // acompanhar
            for (int i = 0; i < 60 && !m().isAterrissando(); i++) {
                ticks(1);
            }
            tecla("brancoDireita", false);
            assertTrue(m().isAterrissando());
            assertEquals("CAINDO: COMO RECEBER?", titulo(SlotJogador.JOGADOR_2));
            tecla("pretoControleUP", true);                     // J2: cima = fechar a guarda
            ticks(1);
            tecla("pretoControleUP", false);
            ticks(MotorSolo.ATERRISSAGEM_TICKS + 40);
            assertEquals(Posicao.GUARDA, m().getPosicao());
        });
    }

    // ---------------- 3. RT: segurado e respiracao; na entrada do outro e base
    @Test
    void rtSeguradoRespiraESofrendoEntradaViraBase() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("pretoControleP", true);                      // RT do J2 segurado: respirar
            ticks(5);
            assertTrue(painel.respirandoPeloModificador(SlotJogador.JOGADOR_2, j2()), "segurado e livre: respira");
            tecla("pretoControleP", false);
            ticks(1);
            // J1 entra nas pernas; durante a janela o RT do J2 e base, nao respiracao
            tecla("brancoDireita", true);
            for (int i = 0; i < 200 && Math.abs(j1().getX() - j2().getX()) > 100; i++) {
                ticks(1);
            }
            tecla("brancoDireita", false);
            j1Rb("brancoPegada3");                              // RB + baixo = entrada nas pernas
            assertTrue(m().getTentativa().isPresent(), "entrada iniciada");
            assertTrue(m().sofrendoEntrada(Lado.B));
            tecla("pretoControleP", true);
            ticks(1);
            assertFalse(painel.respirandoPeloModificador(SlotJogador.JOGADOR_2, j2()), "na entrada o RT e base");
            tecla("pretoControleP", false);
            ticks(80);
            assertEquals(Posicao.SOLO_DISPUTADO, m().getPosicao(), "o RT apertado na janela fez base");
        });
    }

    // ---------------- 7. aviso no topo: so com resultado confirmado
    @Test
    void avisoNoTopoSoComResultadoEExpira() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            assertNull(painel.avisoAtual(), "pegada em pe nao e resultado do solo");
            j1Rb("brancoPegada2");
            assertNull(painel.avisoAtual(), "a tentativa nao vira aviso");
            ticks(30);
            assertNotNull(painel.avisoAtual());
            assertTrue(painel.avisoAtual().startsWith("J1"), painel.avisoAtual());
            ticks(AvisosCombate.DURACAO_TICKS * 3);
            assertNull(painel.avisoAtual(), "expirou");
        });
    }

    // ---------------- 9. REINICIAR LUTA
    @Test
    void reiniciarLutaNaPausaRecomecaAMesmaLutaRepetidamente() throws Exception {
        AtomicInteger reinicios = new AtomicInteger();
        naEdt(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { },
                    reinicios::incrementAndGet);
            painel.pararLoopParaTestes();
            ticks(2);
            assertNotNull(painel.botaoReiniciarLuta());
            assertEquals("REINICIAR LUTA", painel.botaoReiniciarLuta().getText());
            assertTrue(painel.botaoReiniciarLuta().isVisible());
            j1().alternarDeitado();
            ticks(30);
            assertNotNull(painel.avisoAtual());
            painel.alternarPausaParaTestes();
            painel.botaoReiniciarLuta().doClick();
            assertEquals(1, reinicios.get(), "o mesmo caminho do 'tentar novamente' (mesmos Miaus, arena e modo)");
            assertNull(painel.avisoAtual(), "avisos limpos");
            assertEquals(Posicao.EM_PE_SEPARADOS, m().getPosicao(), "solo limpo");
            painel.reiniciarLuta();
            assertEquals(2, reinicios.get(), "pode repetir");
        });
    }
}
