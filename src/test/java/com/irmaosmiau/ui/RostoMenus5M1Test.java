package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * Correcao 5M.1: o rosto dos irmaos nos menus nao pode depender do traco que
 * a tela deixou no Graphics (letreiro 6 px, cursores, paineis). Compara o
 * mesmo desenho feito com tracos herdados diferentes: tem que ser identico
 * (mesma maquina, mesma imagem: nao depende do antialiasing do sistema).
 */
class RostoMenus5M1Test {

    private static int[] desenhar(IrmaoMiau l, BasicStroke herdado) {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setStroke(herdado);
        ArteMenus.desenharLutador(g, l);
        g.dispose();
        return img.getRGB(0, 0, 200, 200, null, 0, 200);
    }

    private static void semInfluenciaDoTracoHerdado(IrmaoMiau l) {
        int[] fino = desenhar(l, new BasicStroke(1));
        int[] letreiro = desenhar(l, new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[] painel = desenhar(l, new BasicStroke(2.5f));
        assertArrayEquals(fino, letreiro, "o contorno do letreiro nao pode engrossar o bigodinho");
        assertArrayEquals(fino, painel, "o traco dos paineis/cursores nao pode engrossar o bigodinho");
    }

    @Test
    void brancoIgualEmQualquerTela() {
        for (double escala : new double[] {0.62, 0.7, 0.85, 1.0}) {
            IrmaoMiau b = new IrmaoMiauBranco(40, 10, escala);
            b.setViradoParaDireita(true);
            semInfluenciaDoTracoHerdado(b);
        }
    }

    @Test
    void pretoIgualEmQualquerTela() {
        for (double escala : new double[] {0.62, 0.7, 0.85, 1.0}) {
            IrmaoMiau p = new IrmaoMiauPreto(40, 10, escala);
            p.setViradoParaDireita(false);
            semInfluenciaDoTracoHerdado(p);
        }
    }

    @Test
    void tracoDoRostoIgualAoDoRigOficial() {
        assertEquals(1.5f, ArteMenus.tracoDoRosto(1.0).getLineWidth(), 1e-6);
        assertEquals(1.0f, ArteMenus.tracoDoRosto(0.5).getLineWidth(), 1e-6, "nunca abaixo de 1 px");
        assertEquals(BasicStroke.CAP_ROUND, ArteMenus.tracoDoRosto(0.85).getEndCap());
    }
}
