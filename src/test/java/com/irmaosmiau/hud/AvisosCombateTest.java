package com.irmaosmiau.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/** Entrega 5P.1: aviso discreto no topo central (um de cada vez, fila curta, expira). */
class AvisosCombateTest {

    private static void ticks(AvisosCombate a, int n) {
        for (int i = 0; i < n; i++) {
            a.atualizar();
        }
    }

    @Test
    void mostraUmDeCadaVezEExpira() {
        AvisosCombate a = new AvisosCombate();
        assertNull(a.getAtual());
        a.adicionar("J1 derrubou J2!");
        assertEquals("J1 derrubou J2!", a.getAtual());
        ticks(a, AvisosCombate.DURACAO_TICKS - 1);
        assertEquals("J1 derrubou J2!", a.getAtual(), "~1,8 s na tela");
        ticks(a, 1);
        assertNull(a.getAtual(), "some sozinho");
        assertTrue(AvisosCombate.DURACAO_TICKS * 16 >= 1500 && AvisosCombate.DURACAO_TICKS * 16 <= 2000);
    }

    @Test
    void filaCurtaSemSobreporENemAtrasar() {
        AvisosCombate a = new AvisosCombate();
        a.adicionar("1");
        for (int i = 2; i <= 6; i++) {
            a.adicionar(String.valueOf(i));
        }
        assertEquals("1", a.getAtual(), "um de cada vez");
        assertEquals(AvisosCombate.FILA_MAXIMA, a.pendentes(), "fila curta: os mais velhos saem");
        ticks(a, AvisosCombate.DURACAO_COM_FILA_TICKS);
        assertEquals("4", a.getAtual(), "2 e 3 foram descartados");
        ticks(a, AvisosCombate.DURACAO_COM_FILA_TICKS * 2);
        assertEquals("6", a.getAtual());
        ticks(a, AvisosCombate.DURACAO_TICKS);
        assertNull(a.getAtual());
    }

    @Test
    void ignoraVazioERepeticaoImediataELimpa() {
        AvisosCombate a = new AvisosCombate();
        a.adicionar(null);
        a.adicionar("  ");
        assertNull(a.getAtual());
        a.adicionar("J2 levantou!");
        a.adicionar("J2 levantou!");
        assertEquals(0, a.pendentes(), "repeticao imediata nao duplica");
        a.limpar();
        assertNull(a.getAtual());
        assertEquals(0, a.pendentes());
    }

    @Test
    void ficaAbaixoDoRelogioEEntreOsPaineis() {
        // relogio: y 4..30; paineis de folego: x 10..220 e 580..790 (tela 800)
        assertTrue(AvisosCombate.Y > 30, "abaixo do relogio");
        int esquerda = 800 / 2 - AvisosCombate.LARGURA_MAXIMA / 2;
        assertTrue(esquerda > 220 && esquerda + AvisosCombate.LARGURA_MAXIMA < 580, "entre os paineis");
        AvisosCombate a = new AvisosCombate();
        a.adicionar("J1 escapou da montada e recuperou a guarda com muita calma!");
        BufferedImage img = new BufferedImage(800, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        a.desenhar(g, 800);
        g.dispose();
        for (int x = 0; x < 800; x++) {
            for (int y = 0; y < 120; y++) {
                if ((img.getRGB(x, y) >>> 24) != 0) {
                    assertTrue(y >= AvisosCombate.Y && y <= AvisosCombate.Y + AvisosCombate.ALTURA, "y " + y);
                    assertTrue(x > 220 && x < 580, "x " + x);
                }
            }
        }
    }
}
