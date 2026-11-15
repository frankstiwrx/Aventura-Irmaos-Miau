package com.irmaosmiau.cenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/** Entrega 5P.3: Academia de jiu-jitsu e Fortaleza nevada ao luar (Java2D, sem imagens). */
class Arenas53Test {

    private static final int W = 800;
    private static final int H = 450;

    private static Cenario[] novos() {
        return new Cenario[] {new CenarioAcademia(), new CenarioFortalezaNevada()};
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
    void registradosDepoisDosAntigosComNomes() {
        RegistroCenarios r = RegistroCenarios.padrao();
        assertEquals("Academia de jiu-jitsu", r.exigir("academia").getNome());
        assertEquals("Fortaleza nevada ao luar", r.exigir("fortaleza_neve").getNome());
        assertEquals(4, r.ids().indexOf("academia"));
        assertEquals(5, r.ids().indexOf("fortaleza_neve"));
        assertEquals("dojo", RegistroCenarios.ID_PADRAO, "o padrao continua o Dojo");
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
    void escalaDoCenarioNaoDeixaOsLutadoresGigantes() {
        // um lutador em pe tem ~190 px: a protecao de EVA fica abaixo da cintura, a muralha abaixo da cabeca
        int lutador = 190;
        int eva = CenarioDojo.Y_BASE_PAREDE - CenarioAcademia.Y_EVA;
        int muro = CenarioDojo.Y_BASE_PAREDE - CenarioFortalezaNevada.Y_MURO;
        int parede = CenarioDojo.Y_BASE_PAREDE - CenarioAcademia.Y_TETO;
        assertTrue(eva > lutador / 4 && eva < lutador / 2, "EVA abaixo da cintura: " + eva);
        assertTrue(muro > lutador / 2 && muro < lutador, "muralha imponente, menor que o lutador: " + muro);
        assertTrue(parede > lutador, "a parede da academia e mais alta que os lutadores: " + parede);
    }

    @Test
    void neveDeVerdadeNoChaoECeuNoturno() {
        BufferedImage chao = camada(new CenarioFortalezaNevada(), 1);
        Color neve = new Color(chao.getRGB(400, CenarioDojo.yCentroLinha(2)));
        assertTrue(neve.getRed() > 190 && neve.getGreen() > 200 && neve.getBlue() > 230, "chao de neve: " + neve);
        BufferedImage fundo = camada(new CenarioFortalezaNevada(), 0);
        Color ceu = new Color(fundo.getRGB(30, 4));
        assertTrue(ceu.getRed() + ceu.getGreen() + ceu.getBlue() < 200, "ceu noturno: " + ceu);
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
