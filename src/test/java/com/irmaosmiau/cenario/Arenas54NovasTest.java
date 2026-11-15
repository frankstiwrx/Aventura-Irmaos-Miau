package com.irmaosmiau.cenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/** Entrega 5P.4: Academia dos Irmaos Miau (fachada) e Campeonato Miau (Java2D, sem imagens). */
class Arenas54NovasTest {

    private static final int W = 800;
    private static final int H = 450;

    private static Cenario[] novos() {
        return new Cenario[] {new CenarioFachadaAcademia(), new CenarioCampeonato()};
    }

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
    void oitoMapasComOsNovosNoFim() {
        RegistroCenarios r = RegistroCenarios.padrao();
        assertEquals(8, r.quantidade());
        assertEquals("Academia dos Irmãos Miau", r.exigir("academia_miau").getNome());
        assertEquals("Campeonato Miau", r.exigir("campeonato").getNome());
        assertEquals(6, r.ids().indexOf("academia_miau"));
        assertEquals(7, r.ids().indexOf("campeonato"));
        assertEquals("dojo", RegistroCenarios.ID_PADRAO);
    }

    @Test
    void fundoCobreATelaAteOChaoEOChaoCobreAsCincoLinhas() {
        for (Cenario c : novos()) {
            BufferedImage fundo = camada(c, 0);
            BufferedImage chao = camada(c, 1);
            for (int y = 0; y < CenarioDojo.Y_BASE_PAREDE; y += 9) {
                for (int x = 0; x < W; x += 13) {
                    assertEquals(255, fundo.getRGB(x, y) >>> 24, c.getId() + ": fundo opaco em (" + x + "," + y + ")");
                }
            }
            for (int linha = 0; linha < CenarioDojo.TOTAL_LINHAS; linha++) {
                int y = CenarioDojo.yCentroLinha(linha);
                for (int x : new int[] {5, 200, 400, 600, 795}) {
                    assertEquals(255, chao.getRGB(x, y) >>> 24, c.getId() + ": chao nos pes da linha " + (linha + 1));
                }
            }
            assertEquals(0, chao.getRGB(400, CenarioDojo.Y_BASE_PAREDE - 10) >>> 24,
                    c.getId() + ": o chao comeca no horizonte (296), como nos outros mapas");
            Color l1 = new Color(chao.getRGB(400, CenarioDojo.yCentroLinha(0) + 3));
            Color l2 = new Color(chao.getRGB(400, CenarioDojo.yCentroLinha(1) + 3));
            assertNotEquals(l1, l2, c.getId() + ": uma faixa por linha de movimentacao");
        }
    }

    @Test
    void primeiroPlanoNaoCobreOsLutadores() {
        int y0 = CenarioDojo.yCentroLinha(CenarioDojo.TOTAL_LINHAS - 1) + CenarioDojo.ESPACO_LINHAS;   // 426
        for (Cenario c : novos()) {
            BufferedImage frente = camada(c, 2);
            for (int y = 0; y < y0 - 14; y += 2) {
                for (int x = 0; x < W; x += 3) {
                    assertEquals(0, frente.getRGB(x, y) >>> 24, c.getId() + ": primeiro plano em (" + x + "," + y + ")");
                }
            }
        }
    }

    @Test
    void cenarioAtrasMaisAltoQueOsLutadoresEBarreiraAbaixoDaCintura() {
        int lutador = 190;
        assertTrue(CenarioDojo.Y_BASE_PAREDE - CenarioFachadaAcademia.Y_BEIRAL < lutador + 10,
                "fachada proporcional aos lutadores");
        int barreira = CenarioDojo.Y_BASE_PAREDE - CenarioCampeonato.Y_BARREIRA;
        assertTrue(barreira < lutador / 2, "placas abaixo da cintura: " + barreira);
    }

    @Test
    void desenhoEmCacheEIdenticoACadaQuadro() {
        for (Cenario c : novos()) {
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
