package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.entities.InicioLuta;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/** F2: esconde e devolve o HUD (so visual); o jogo, os comandos e as rodas continuam funcionando. */
class HudOcultoGamePanelTest extends Base5OGamePanel {

    private int[] quadro() {
        BufferedImage img = new BufferedImage(GamePanel.LARGURA, GamePanel.ALTURA, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        painel.setSize(GamePanel.LARGURA, GamePanel.ALTURA);
        painel.paintComponent(g);
        g.dispose();
        return img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
    }

    private static int diferentes(int[] a, int[] b) {
        int n = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                n++;
            }
        }
        return n;
    }

    @Test
    void f2EscondeEDevolveOHudSemPararOJogo() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            assertFalse(painel.isHudOculto(), "a partida comeca com o HUD");
            int[] comHud = quadro();
            alternarHud();
            assertTrue(painel.isHudOculto());
            int[] semHud = quadro();
            assertTrue(diferentes(comHud, semHud) > 2000, "barras, rodas e textos somem");
            alternarHud();
            assertFalse(painel.isHudOculto());
            int[] devolvido = quadro();
            assertTrue(diferentes(devolvido, semHud) > 2000, "o HUD voltou");

            // escondido, o jogo continua: nao pausa, a pegada entra e as rodas continuam calculadas
            alternarHud();
            assertFalse(painel.isPausado());
            j1Pega(1);
            assertTrue(s().isInteracaoEstabelecida(), "comandos funcionam com o HUD escondido");
            assertNotNull(painel.montarQuadroDpad(SlotJogador.JOGADOR_2), "a roda do J2 continua calculada");
            assertTrue(painel.isHudOculto(), "o estado fica ate alternar de novo");

            // uma partida nova comeca com o HUD completo
            novo(InicioLuta.EM_PE);
            assertFalse(painel.isHudOculto());
        });
    }

    private void alternarHud() {
        painel.getActionMap().get("alternarHud").actionPerformed(null);
        ticks(1);
    }
}
