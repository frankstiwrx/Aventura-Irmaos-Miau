package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/** Entrega 5M (testes 31 a 40): mapa, tempo e inicio em pe da luta de JOGAR. */
class Partida5MGamePanelTest {

    /** Ticks de 16 ms em um segundo de luta (arredondado para cima). */
    private static final int TICKS_POR_SEGUNDO = (int) Math.ceil(1000 / 16.0);

    private GamePanel painel;

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private static EscolhaLutador oficial(String id) {
        return EscolhaLutador.oficial(CatalogoPersonagens.padrao().exigir(id));
    }

    private GamePanel jogar(String cenario, int segundos) {
        GamePanel g = new GamePanel(TipoPersonagem.AMBOS, oficial(CatalogoPersonagens.ID_BRANCO),
                oficial(CatalogoPersonagens.ID_PRETO), InicioLuta.EM_PE, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        g.configurarPartida(cenario, segundos);
        return g;
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    // 31. O mapa escolhido e o da luta
    @Test
    void mapaEscolhidoEUsado() throws Exception {
        naEdt(() -> {
            painel = jogar("original", 0);
            assertEquals("original", painel.getCenario().getId());
            painel = jogar("dojo", 0);
            assertEquals("dojo", painel.getCenario().getId());
        });
    }

    // 32. Mapa desconhecido nao quebra: fica o atual
    @Test
    void mapaDesconhecidoMantemOAtual() throws Exception {
        naEdt(() -> {
            painel = jogar("nao_existe", 0);
            assertTrue(painel.getCenario() != null);
        });
    }

    // 33. JOGAR comeca os dois em pe
    @Test
    void jogarComecaEmPe() throws Exception {
        naEdt(() -> {
            painel = jogar("dojo", 60);
            ticks(1);
            assertEquals(InicioLuta.EM_PE, painel.getInicioLuta());
            assertFalse(painel.getLutador1().isAjoelhado() || painel.getLutador1().isDeitado());
            assertFalse(painel.getLutador2().isAjoelhado() || painel.getLutador2().isDeitado());
        });
    }

    // 34. Sem limite: nao ha relogio e a luta nao acaba por tempo
    @Test
    void semLimiteNaoAcaba() throws Exception {
        naEdt(() -> {
            painel = jogar("dojo", 0);
            assertEquals(-1, painel.getSegundosRestantes());
            ticks(TICKS_POR_SEGUNDO * 3);
            assertFalse(painel.isFimDeJogo());
        });
    }

    // 35. O relogio desce com a luta
    @Test
    void relogioDesce() throws Exception {
        naEdt(() -> {
            painel = jogar("dojo", 60);
            assertEquals(60, painel.getSegundosRestantes());
            ticks(TICKS_POR_SEGUNDO * 2);
            assertEquals(58, painel.getSegundosRestantes());
        });
    }

    // 36. Pausado o relogio para; despausado volta a correr
    @Test
    void pausaParaORelogio() throws Exception {
        naEdt(() -> {
            painel = jogar("dojo", 60);
            ticks(TICKS_POR_SEGUNDO);
            int antes = painel.getSegundosRestantes();
            painel.alternarPausaParaTestes();
            assertTrue(painel.isPausado());
            ticks(TICKS_POR_SEGUNDO * 5);
            assertEquals(antes, painel.getSegundosRestantes(), "pausa nao conta");
            painel.alternarPausaParaTestes();
            ticks(TICKS_POR_SEGUNDO);
            assertEquals(antes - 1, painel.getSegundosRestantes());
        });
    }

    // 37. Tempo esgotado encerra a luta com motivo explicito, e o relogio para
    @Test
    void tempoEsgotadoEncerra() throws Exception {
        naEdt(() -> {
            painel = jogar("dojo", 60);
            ticks(TICKS_POR_SEGUNDO * 60 + 2);
            assertTrue(painel.isFimDeJogo());
            assertEquals(0, painel.getSegundosRestantes());
            assertTrue(painel.getMensagemFim().startsWith("TEMPO ESGOTADO"), painel.getMensagemFim());
            assertTrue(painel.getMensagemFim().contains("EMPATE"), "ninguem se cansou: empate");
            ticks(10);
            assertEquals(0, painel.getSegundosRestantes());
        });
    }

    // 38. Resultado por tempo: mais condicionamento leva a vantagem
    @Test
    void resultadoPorTempo() {
        assertEquals("EMPATE", GamePanel.resultadoPorTempo(0.5, 0.505, "Irmão Miau Branco", "Irmão Miau Preto"));
        assertEquals("VANTAGEM: J1 MIAU BRANCO",
                GamePanel.resultadoPorTempo(0.9, 0.4, "Irmão Miau Branco", "Irmão Miau Preto"));
        assertEquals("VANTAGEM: J2 MIAU PRETO",
                GamePanel.resultadoPorTempo(0.2, 0.4, "Irmão Miau Branco", "Irmão Miau Preto"));
    }

    // 39. No treino o tempo nao vale
    @Test
    void treinoIgnoraTempo() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            painel.configurarPartida(null, 60);
            assertEquals(-1, painel.getSegundosRestantes());
            assertTrue(painel.isModoTreino());
        });
    }

    // 40. O relogio e desenhado sem erro (HUD completo)
    @Test
    void desenhaComRelogio() throws Exception {
        naEdt(() -> {
            painel = jogar("original", 90);
            ticks(3);
            painel.setSize(800, 450);
            BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            painel.paint(g);
            g.dispose();
            int cor = img.getRGB(400, 8);
            assertTrue(((cor >>> 24) & 0xff) > 0, "caixa do relogio no topo");
        });
    }
}
