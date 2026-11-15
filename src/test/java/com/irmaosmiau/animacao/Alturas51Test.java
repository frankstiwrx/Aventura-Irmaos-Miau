package com.irmaosmiau.animacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.DefesaCorporal;
import com.irmaosmiau.entities.Direcao;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Entrega 5O.1: altura REAL do desenho (pixels nao transparentes) do rolamento
 * e do sprawl experimentais, medida como no laboratorio de poses: o rolamento
 * passa perto do tatame (no maximo a altura de quem esta de joelhos), o sprawl
 * protege perto da altura de quem esta deitado, e nada some abaixo do piso.
 */
class Alturas51Test {

    private final BibliotecaMovimentos bib = BibliotecaMovimentos.padrao();
    private final TransicoesPostura posturas = new TransicoesPostura(null);

    @AfterEach
    void limpar() {
        VersaoMovimentos.restaurarPadrao();
    }

    /** {topo, base} em px acima de uma linha fixa (a mesma para todas as poses). */
    private static int[] medir(IrmaoMiau l, int[] pose, double dy) {
        BufferedImage img = new BufferedImage(220, 260, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        l.aplicarPoseCombateExterna(pose, 0, dy);
        l.desenhar(g);
        g.dispose();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 40) {
                    min = Math.min(min, y);
                    max = Math.max(max, y);
                }
            }
        }
        return new int[] {200 - min, 200 - max};
    }

    private static IrmaoMiau novo(boolean branco) {
        return branco ? new IrmaoMiauBranco(75, 40, 0.7) : new IrmaoMiauPreto(75, 40, 0.7);
    }

    private int[] postura(boolean branco, IrmaoMiau.EstadoCorporal e) {
        IrmaoMiau l = novo(branco);
        TransicoesPostura.Pose p = posturas.pose(l, e);
        return medir(l, p.valores(), p.dy());
    }

    private final AssentamentoMovimentos assentamento = new AssentamentoMovimentos();

    /** Como o AnimadorMovimentos desenha: quadros do arquivo + assentamento do proprio personagem. */
    private int[] noProgresso(boolean branco, String id, double p) {
        int[] pose = new int[30];
        BibliotecaMovimentos.Movimento m = bib.buscar(id).orElseThrow();
        m.noProgresso(p, pose);
        double dy = assentamento.dy(novo(branco), m, p);
        pose[2] = 7;
        pose[3] = 1;
        return medir(novo(branco), pose, dy);
    }

    @Test
    void rolamentoPertoDoTatameSemSumirNoPiso() {
        for (boolean branco : new boolean[] {true, false}) {
            int[] pe = postura(branco, IrmaoMiau.EstadoCorporal.EM_PE);
            int chao = pe[1];
            int alturaEmPe = pe[0] - chao;
            int alturaJoelhos = postura(branco, IrmaoMiau.EstadoCorporal.AJOELHADO)[0] - chao;
            for (String id : new String[] {"rolamento_frente_5o", "rolamento_tras_5o", "rolamento_lateral_5o"}) {
                double soma = 0;
                int n = 0;
                for (int i = 0; i <= 100; i += 5) {
                    int[] m = noProgresso(branco, id, i / 100.0);
                    assertTrue(Math.abs(m[1] - chao) <= 8, id + " " + i + "%: base " + (m[1] - chao) + " px do tatame");
                    if (i >= 15 && i <= 75) {                      // durante o giro (antes de levantar)
                        int h = m[0] - chao;
                        assertTrue(h <= alturaJoelhos, id + " " + i + "%: " + h + " > joelhos " + alturaJoelhos);
                        soma += h;
                        n++;
                    }
                }
                assertTrue(soma / n <= 0.7 * alturaEmPe, id + ": media " + soma / n + " de " + alturaEmPe);
            }
        }
    }

    @Test
    void sprawlProtegeNaAlturaDeQuemEstaDeitado() {
        for (boolean branco : new boolean[] {true, false}) {
            int[] pe = postura(branco, IrmaoMiau.EstadoCorporal.EM_PE);
            int chao = pe[1];
            int[] deitado = postura(branco, IrmaoMiau.EstadoCorporal.DEITADO);
            int alturaDeitado = deitado[0] - deitado[1];
            for (int i = 0; i <= 100; i += 5) {
                int[] m = noProgresso(branco, "sprawl_5o", i / 100.0);
                assertTrue(Math.abs(m[1] - chao) <= 8, "sprawl " + i + "%: sem flutuar nem afundar (" + (m[1] - chao) + ")");
                if (i >= 45 && i <= 55) {                    // fase de protecao (a mais baixa)
                    int h = m[0] - chao;
                    assertTrue(h <= alturaDeitado * 1.2, "protecao " + i + "%: " + h + " vs deitado " + alturaDeitado);
                    assertTrue(h <= 0.7 * (pe[0] - chao), "protecao bem abaixo de quem esta em pe: " + h + " de " + (pe[0] - chao));
                }
            }
        }
    }

    @Test
    void rolamentoDeJoelhosComecaETerminaNaPostura() {
        VersaoMovimentos.definir(VersaoMovimentos.EXPERIMENTAL_5O);
        AnimadorMovimentos a = new AnimadorMovimentos(bib, posturas);
        for (boolean branco : new boolean[] {true, false}) {
            IrmaoMiau l = novo(branco);
            l.ajoelhar();
            l.iniciarRolamento(Direcao.ESQUERDA);
            assertTrue(l.isRolando());
            assertTrue(a.aplicar(l).endsWith(":AJOELHADO"));
            int[] base = posturas.pose(l, IrmaoMiau.EstadoCorporal.AJOELHADO).valores();
            int[] inicio = l.copiarPoseCombateExterna();
            for (int k = 4; k < 30; k++) {
                assertEquals(base[k], inicio[k], 1, "inicio = postura de joelhos (indice " + k + ")");
            }
            int[] antes = inicio;
            while (l.isRolando()) {
                l.atualizar(false, false);
                if (!l.isRolando()) {
                    break;
                }
                a.aplicar(l);
                int[] p = l.copiarPoseCombateExterna();
                for (int k = 4; k < 30; k++) {
                    if (k != 10) {
                        assertTrue(Math.abs(p[k] - antes[k]) <= 30, "sem salto no indice " + k);
                    }
                }
                antes = p;
            }
            int[] fim = antes;
            for (int k = 4; k < 30; k++) {
                if (k != 10) {
                    assertEquals(base[k], fim[k], 12, "termina na postura de joelhos (indice " + k + ")");
                }
            }
            assertEquals(0, Math.IEEEremainder(fim[10] - base[10], 360), 12, "giro fechado (360 = 0)");
            assertTrue(l.isAjoelhado());
        }
    }

    @Test
    void guardaAndandoMexeSoAsPernas() {
        AnimadorMovimentos a = new AnimadorMovimentos(bib, posturas);
        IrmaoMiau l = novo(true);
        l.getDefesa().manter(DefesaCorporal.Tipo.GUARDA_ALTA);
        for (int i = 0; i < DefesaCorporal.TICKS_ENTRADA + 2; i++) {
            l.getDefesa().manter(DefesaCorporal.Tipo.GUARDA_ALTA);
            l.atualizar(false, false);
        }
        a.aplicar(l);
        int[] parado = l.copiarPoseCombateExterna();
        assertEquals(0.0, a.amplitudePassosGuarda(l), 1e-9);
        boolean pernasMexeram = false;
        for (int i = 0; i < 20; i++) {
            l.getDefesa().manter(DefesaCorporal.Tipo.GUARDA_ALTA);
            l.atualizar(true, false);                            // andando
            a.aplicar(l);
            int[] p = l.copiarPoseCombateExterna();
            for (int k = 14; k <= 21; k++) {
                assertEquals(parado[k], p[k], 2, "bracos continuam protegendo (indice " + k + ")");
            }
            pernasMexeram |= p[24] != parado[24] || p[28] != parado[28];
        }
        assertTrue(pernasMexeram, "pes alternam");
        assertEquals(1.0, a.amplitudePassosGuarda(l), 1e-9);
        l.atualizar(false, false);
        a.aplicar(l);
        assertTrue(a.amplitudePassosGuarda(l) < 1.0 && a.amplitudePassosGuarda(l) > 0, "parar sem tranco (rampa)");
        assertTrue(l.getDefesa().isAtiva(), "a guarda continua");
    }

    @Test
    void originalContinuaComODesenhoDeSempre() {
        VersaoMovimentos.definir(VersaoMovimentos.ORIGINAL);
        AnimadorMovimentos a = new AnimadorMovimentos(bib, posturas);
        IrmaoMiau l = novo(true);
        l.iniciarRolamento(Direcao.DIREITA);
        assertFalse(a.quer(l), "F7 ORIGINAL: rolamento Java de sempre");
        IrmaoMiau s = novo(false);
        s.iniciarSprawl();
        assertFalse(a.quer(s), "F7 ORIGINAL: sprawl Java de sempre");
    }
}
