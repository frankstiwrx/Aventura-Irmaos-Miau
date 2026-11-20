package com.irmaosmiau.cenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.5: nuvens lentas (Academia externa, Campo, Grande Torneio), predio e torcida. */
class Cenarios5Q5Test {

    private static int[] fundo(Cenario c, long tick) {
        c.atualizarAmbiente(tick);
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        c.desenharFundo(g, 800, 450);
        g.dispose();
        return img.getRGB(0, 0, 800, 300, null, 0, 800);
    }

    @Test
    void nuvensAndamDevagarEVoltamPeloOutroLado() {
        NuvensAnimadas.Nuvem n = new NuvensAnimadas.Nuvem(100, 40, 1.0, 0.05);
        assertEquals(100, NuvensAnimadas.x(n, 0, 800), 1e-9);
        assertEquals(103, NuvensAnimadas.x(n, 60, 800), 1e-9, "~3 px por segundo: lento");
        double volta = NuvensAnimadas.x(n, (long) ((800 + 2 * NuvensAnimadas.MARGEM) / 0.05), 800);
        assertEquals(100, volta, 1e-6, "da a volta e reaparece");
    }

    @Test
    void cenariosExternosTemNuvensEmMovimentoEODeterminismoContinua() {
        for (Cenario c : new Cenario[] {new CenarioFachadaAcademia(), new CenarioCampoRemaster(),
            new CenarioArenaTorneio()}) {
            int[] t0 = fundo(c, 0);
            int[] t0b = fundo(c, 0);
            int[] t600 = fundo(c, 600);
            assertTrue(Arrays.equals(t0, t0b), c.getId() + ": o mesmo tick desenha a mesma imagem");
            assertTrue(!Arrays.equals(t0, t600), c.getId() + ": as nuvens andaram");
        }
    }

    @Test
    void predioDaAcademiaChegaAoChao() {
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        CenarioFachadaAcademia.predioAoLonge(g, 800);
        g.dispose();
        int x = 800 - 100 + 30;
        assertTrue((img.getRGB(x, CenarioFachadaAcademia.Y_CHAO - 2) >>> 24) > 0, "sem vao de ceu embaixo do predio");
        assertTrue((img.getRGB(x, 180) >>> 24) > 0);
    }

    @Test
    void campeonatoTemMaisTorcidaViva() {
        CenarioCampeonato c = new CenarioCampeonato();
        int[] a = fundo(c, 10);
        int[] b = fundo(c, 40);
        int diferentes = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                diferentes++;
            }
        }
        assertTrue(diferentes > 200, "torcida e bandeiras se mexendo: " + diferentes);
    }
}
