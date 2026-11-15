package com.irmaosmiau.entities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5M.3 (testes 4, 5 e 6): a pose de apresentacao dos menus e fixa
 * (sem idle de combate nem taunt), a luta continua com idle e taunt, e a
 * boca segue estavel ao piscar.
 */
class PoseApresentacao5M3Test {

    private static int[] imagem(IrmaoMiau l) {
        BufferedImage img = new BufferedImage(260, 260, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        l.desenhar(g);
        g.dispose();
        return img.getRGB(0, 0, 260, 260, null, 0, 260);
    }

    private static void poseFixa(Function<Double, IrmaoMiau> fabrica) {
        for (double e : new double[] {0.62, 0.85, 1.0}) {
            IrmaoMiau l = fabrica.apply(e);
            l.entrarEmPoseDeApresentacao();
            l.definirPiscandoParaTestes(false);
            int[] antes = imagem(l);
            l.envelhecerAtividadeParaTestes(60_000);              // parado ha 1 minuto
            for (int i = 0; i < 400; i++) {
                l.atualizarApresentacao();
            }
            l.definirPiscandoParaTestes(false);
            assertFalse(l.estaEmTaunt(), "sem taunt no menu");
            assertFalse(l.estaEmIdleDeCombate(), "sem idle de combate no menu");
            assertFalse(l.estaAndando());
            assertArrayEquals(antes, imagem(l), "o corpo nao muda na apresentacao (escala " + e + ")");
        }
    }

    // 4. Nenhuma mudanca corporal durante a apresentacao
    @Test
    void brancoPoseFixa() {
        poseFixa(e -> new IrmaoMiauBranco(40, 20, e));
    }

    @Test
    void pretoPoseFixa() {
        poseFixa(e -> new IrmaoMiauPreto(40, 20, e));
    }

    // 5. Na luta (sem a pose de apresentacao) idle e taunt continuam
    @Test
    void idleETauntPreservadosNaLuta() {
        for (IrmaoMiau l : new IrmaoMiau[] {new IrmaoMiauBranco(40, 20, 0.7), new IrmaoMiauPreto(40, 20, 0.7)}) {
            assertFalse(l.isPoseDeApresentacao());
            l.atualizar(false, false);
            assertTrue(l.estaEmIdleDeCombate(), "idle de combate em pe");
            l.envelhecerAtividadeParaTestes(8_000);
            l.atualizar(false, false);
            assertTrue(l.estaEmTaunt(), "taunt depois de 7 s parado");
        }
    }

    // 6. Na apresentacao a piscada continua possivel e so muda os olhos (a boca da 5M.2 fica igual)
    @Test
    void piscarNaApresentacaoSoMudaOsOlhos() {
        for (IrmaoMiau l : new IrmaoMiau[] {new IrmaoMiauBranco(40, 20, 1.0), new IrmaoMiauPreto(40, 20, 0.62)}) {
            l.entrarEmPoseDeApresentacao();
            l.definirPiscandoParaTestes(false);
            int[] aberto = imagem(l);
            l.definirPiscandoParaTestes(true);
            int[] fechado = imagem(l);
            double e = l.getEscala();
            int topo = l.getYVisual() + (int) (30 * e) - 3;
            int base = l.getYVisual() + (int) (36 * e) + 3;
            boolean mudou = false;
            for (int i = 0; i < aberto.length; i++) {
                if (aberto[i] != fechado[i]) {
                    mudou = true;
                    int y = i / 260;
                    assertTrue(y >= topo && y <= base, "so a faixa dos olhos muda (y=" + y + ")");
                }
            }
            assertTrue(mudou);
        }
    }
}
