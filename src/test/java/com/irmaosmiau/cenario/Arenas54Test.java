package com.irmaosmiau.cenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Entrega 5O.4: Campo Aberto Remasterizado e Arena do Grande Torneio (Java2D, sem imagens). */
class Arenas54Test {

    private static final int W = 800;
    private static final int H = 450;

    private static BufferedImage camada(Cenario c, int qual) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        switch (qual) {
            case 0 -> c.desenharFundo(g, W, H);
            case 1 -> c.desenharChao(g, W, H);
            default -> c.desenharPrimeiroPlano(g, W, H);
        }
        g.dispose();
        return img;
    }

    @Test
    void quatroMapasNaOrdemComOsAntigosIntactos() {
        RegistroCenarios r = RegistroCenarios.padrao();
        assertEquals(List.of("dojo", "original", "campo_remaster", "torneio", "academia", "fortaleza_neve"), r.ids());   // 5P.3 (pedido): + academia e fortaleza nevada
        assertEquals("dojo", RegistroCenarios.ID_PADRAO, "o padrao continua o Dojo");
        assertEquals("Campo original", r.exigir("original").getNome());
        assertEquals("Dojo de jiu-jitsu", r.exigir("dojo").getNome());
        assertEquals("Campo aberto remasterizado", r.exigir("campo_remaster").getNome());
        assertEquals("Arena do Grande Torneio", r.exigir("torneio").getNome());
        // o campo original continua pintando exatamente o de sempre
        BufferedImage chao = camada(r.exigir("original"), 1);
        assertEquals(CenarioOriginal.COR_GRAMA.getRGB(), chao.getRGB(400, 350));
    }

    @Test
    void chaoCobreAsCincoLinhasEFundoFicaAcima() {
        for (Cenario c : new Cenario[] {new CenarioCampoRemaster(), new CenarioArenaTorneio()}) {
            BufferedImage fundo = camada(c, 0);
            BufferedImage chao = camada(c, 1);
            assertTrue((fundo.getRGB(400, 20) >>> 24) == 255, c.getId() + ": ceu pintado");
            for (int linha = 0; linha < CenarioDojo.TOTAL_LINHAS; linha++) {
                int y = CenarioDojo.yCentroLinha(linha);
                for (int x : new int[] {20, 400, 780}) {
                    assertEquals(255, chao.getRGB(x, y) >>> 24, c.getId() + ": chao opaco nos pes da linha " + (linha + 1));
                }
            }
            assertEquals(0, chao.getRGB(400, CenarioDojo.Y_BASE_PAREDE - 10) >>> 24,
                    c.getId() + ": o chao comeca no horizonte (296), como no Dojo");
            // faixas alternadas por linha: linhas vizinhas tem cores diferentes no centro
            Color l1 = new Color(chao.getRGB(400, CenarioDojo.yCentroLinha(0) + 3));
            Color l2 = new Color(chao.getRGB(400, CenarioDojo.yCentroLinha(1) + 3));
            assertNotEquals(l1, l2, c.getId() + ": uma faixa por linha de movimentacao");
        }
    }

    @Test
    void primeiroPlanoNaoCobreOsLutadores() {
        int y0 = CenarioDojo.yCentroLinha(CenarioDojo.TOTAL_LINHAS - 1) + CenarioDojo.ESPACO_LINHAS;   // 426
        for (Cenario c : new Cenario[] {new CenarioCampoRemaster(), new CenarioArenaTorneio()}) {
            BufferedImage frente = camada(c, 2);
            for (int y = 0; y < y0 - 14; y += 3) {
                for (int x = 0; x < W; x += 5) {
                    assertEquals(0, frente.getRGB(x, y) >>> 24, c.getId() + ": primeiro plano em (" + x + "," + y + ")");
                }
            }
        }
    }

    @Test
    void desenhoEmCacheEIdenticoACadaQuadro() {
        for (Cenario c : new Cenario[] {new CenarioCampoRemaster(), new CenarioArenaTorneio()}) {
            BufferedImage a = camada(c, 0);
            BufferedImage b = camada(c, 0);
            for (int y = 0; y < H; y += 7) {
                for (int x = 0; x < W; x += 7) {
                    assertEquals(a.getRGB(x, y), b.getRGB(x, y));
                }
            }
        }
    }
}
