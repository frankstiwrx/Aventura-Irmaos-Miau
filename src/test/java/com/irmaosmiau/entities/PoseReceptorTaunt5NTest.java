package com.irmaosmiau.entities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5N (testes 7 e 8): com a pose de controle (pose de combate
 * externa) ativa, nem o idle de combate nem o taunt mudam o desenho do
 * receptor; e segurar/ser segurado conta como atividade (sem taunt logo
 * depois da pegada).
 */
class PoseReceptorTaunt5NTest {

    private static int[] imagem(IrmaoMiau l) {
        BufferedImage img = new BufferedImage(260, 260, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        l.desenhar(g);
        g.dispose();
        return img.getRGB(0, 0, 260, 260, null, 0, 260);
    }

    @Test
    void idleETauntNaoSubstituemAPoseDeControle() {
        for (IrmaoMiau l : new IrmaoMiau[] {new IrmaoMiauBranco(40, 20, 0.7), new IrmaoMiauPreto(40, 20, 0.7)}) {
            int[] pose = new int[30];
            pose[2] = 7;
            pose[3] = 1;
            pose[9] = 9;                       // tronco puxado (pose de controle)
            pose[21] = 12;
            l.aplicarPoseCombateExterna(pose, 0, 0);
            l.definirPiscandoParaTestes(false);
            int[] antes = imagem(l);
            l.envelhecerAtividadeParaTestes(10_000);
            l.atualizar(false, false);
            assertTrue(l.estaEmTaunt(), "o relogio do taunt correu");
            l.definirPiscandoParaTestes(false);
            assertArrayEquals(antes, imagem(l), "o taunt nao substitui a pose do receptor");
        }
    }

    @Test
    void pegadaContaComoAtividade() {
        IrmaoMiau l = new IrmaoMiauPreto(40, 20, 0.7);
        l.envelhecerAtividadeParaTestes(10_000);
        l.registrarAtividadeDeCombate();
        l.atualizar(false, false);
        assertFalse(l.estaEmTaunt(), "logo depois da pegada nao ha taunt");
    }

    @Test
    void profundidadeConjuntaSoDentroDasLinhas() {
        IrmaoMiau l = new IrmaoMiauBranco(40, 150, 0.7);
        assertFalse(l.podeDeslocarProfundidade(-1), "ja na linha 1");
        assertTrue(l.podeDeslocarProfundidade(80));
        assertFalse(l.podeDeslocarProfundidade(81), "alem da linha 5");
        l.deslocarProfundidadeFisicamente(41);
        assertTrue(l.getLinhaAtual() == 2 && l.getY() == 191);
    }
}
