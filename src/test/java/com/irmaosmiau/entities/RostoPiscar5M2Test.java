package com.irmaosmiau.entities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Correcao 5M.2: piscar so muda os olhos. A boca (e o resto do rosto) fica
 * identica de olhos abertos e fechados, com qualquer traco herdado, para o
 * Branco e o Preto, em escalas pequenas e grandes. As comparacoes sao entre
 * imagens feitas na mesma maquina (nao dependem do antialiasing do sistema).
 */
class RostoPiscar5M2Test {

    private static final double[] ESCALAS = {0.62, 0.7, 0.85, 1.0, 1.6};
    private static final int TAM = 260;

    private static final Stroke[] HERDADOS = {
        new BasicStroke(1),
        new BasicStroke(2.5f),
        new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
        new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    };

    private static int[] cabeca(IrmaoMiau l, boolean fechado, Stroke herdado) {
        BufferedImage img = new BufferedImage(TAM, TAM, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(herdado);
        l.definirPiscandoParaTestes(fechado);
        l.desenharCabeca(g);
        g.dispose();
        return img.getRGB(0, 0, TAM, TAM, null, 0, TAM);
    }

    /** Faixa vertical dos olhos (com folga para o antialiasing). */
    private static boolean naFaixaDosOlhos(IrmaoMiau l, int y) {
        double e = l.getEscala();
        int topo = l.getYVisual() + (int) (30 * e) - 3;
        int base = l.getYVisual() + (int) (36 * e) + 3;
        return y >= topo && y <= base;
    }

    private static void piscarSoMudaOsOlhos(Function<Double, IrmaoMiau> fabrica) {
        for (double e : ESCALAS) {
            for (Stroke herdado : HERDADOS) {
                IrmaoMiau l = fabrica.apply(e);
                int[] aberto = cabeca(l, false, herdado);
                int[] fechado = cabeca(l, true, herdado);
                int diferentes = 0;
                for (int i = 0; i < aberto.length; i++) {
                    if (aberto[i] != fechado[i]) {
                        diferentes++;
                        int y = i / TAM;
                        assertTrue(naFaixaDosOlhos(l, y),
                                "escala " + e + ": pixel fora dos olhos mudou ao piscar (y=" + y + ")");
                    }
                }
                assertTrue(diferentes > 0, "os olhos precisam mudar ao piscar");
            }
        }
    }

    private static void tracoHerdadoNaoMudaORosto(Function<Double, IrmaoMiau> fabrica) {
        for (double e : ESCALAS) {
            for (boolean fechado : new boolean[] {false, true}) {
                int[] base = cabeca(fabrica.apply(e), fechado, HERDADOS[0]);
                for (Stroke herdado : HERDADOS) {
                    assertArrayEquals(base, cabeca(fabrica.apply(e), fechado, herdado),
                            "escala " + e + (fechado ? " fechado" : " aberto") + ": rosto depende do traco herdado");
                }
            }
        }
    }

    private static IrmaoMiau branco(double e) {
        return new IrmaoMiauBranco(40, 20, e);
    }

    private static IrmaoMiau preto(double e) {
        return new IrmaoMiauPreto(40, 20, e);
    }

    @Test
    void brancoBocaEstavelAoPiscar() {
        piscarSoMudaOsOlhos(RostoPiscar5M2Test::branco);
    }

    @Test
    void pretoBocaEstavelAoPiscar() {
        piscarSoMudaOsOlhos(RostoPiscar5M2Test::preto);
    }

    @Test
    void brancoRostoNaoDependeDoTracoHerdado() {
        tracoHerdadoNaoMudaORosto(RostoPiscar5M2Test::branco);
    }

    @Test
    void pretoRostoNaoDependeDoTracoHerdado() {
        tracoHerdadoNaoMudaORosto(RostoPiscar5M2Test::preto);
    }

    @Test
    void cabecaDevolveOTracoDeQuemChamou() {
        Stroke herdado = new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        for (boolean fechado : new boolean[] {false, true}) {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setStroke(herdado);
            IrmaoMiau l = branco(1.0);
            l.definirPiscandoParaTestes(fechado);
            l.desenharCabeca(g);
            assertEquals(herdado, g.getStroke(), "o traco dos olhos fechados nao vaza");
            g.dispose();
        }
    }

    @Test
    void tracoDaBocaIgualAoDoRig() {
        assertEquals(1.5f, IrmaoMiau.tracoDaBoca(1.0).getLineWidth(), 1e-6);
        assertEquals(1.0f, IrmaoMiau.tracoDaBoca(0.5).getLineWidth(), 1e-6);
        assertEquals(BasicStroke.CAP_ROUND, IrmaoMiau.tracoDaBoca(0.7).getEndCap());
    }
}
