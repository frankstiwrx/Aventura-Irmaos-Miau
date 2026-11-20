package com.irmaosmiau.animacao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.7: os bracos giram e dobram sem mudar de tamanho (os dois personagens). */
class IntegridadeMembros5Q7Test {

    @Test
    void qualquerDeslocamentoPreservaOsSegmentosEmBrancoEPreto() {
        Random r = new Random(57);
        for (IrmaoMiau l : new IrmaoMiau[] {new IrmaoMiauBranco(100, 300, 0.7), new IrmaoMiauPreto(300, 300, 0.7)}) {
            int[][] rep = l.getBracosEmRepouso();
            for (int n = 0; n < 300; n++) {
                int[] antes = new int[30];
                for (int i : new int[] {14, 15, 16, 17, 18, 19, 20, 21}) {
                    antes[i] = r.nextInt(41) - 20;                 // poses de partida variadas (pegadas)
                }
                int[] pose = antes.clone();
                for (int i : new int[] {14, 15, 16, 17, 18, 19, 20, 21}) {
                    pose[i] += r.nextInt(61) - 30;                 // deslocamentos de reacao
                }
                double[] a = IntegridadeMembros.comprimentos(antes, rep);
                IntegridadeMembros.preservarBracos(antes, pose, rep);
                double[] d = IntegridadeMembros.comprimentos(pose, rep);
                for (int k = 0; k < 4; k++) {
                    if (a[k] >= 2) {
                        assertTrue(Math.abs(a[k] - d[k]) <= 2.0, l.getClass().getSimpleName() + " segmento " + k
                                + ": " + a[k] + " -> " + d[k]);
                    }
                }
            }
        }
    }

    @Test
    void semDeslocamentoNadaMuda() {
        IrmaoMiau l = new IrmaoMiauPreto(300, 300, 0.7);
        int[] antes = new int[30];
        antes[16] = 12;
        antes[17] = -8;
        antes[20] = 30;
        antes[21] = -20;
        antes[18] = 10;
        antes[19] = -6;
        int[] pose = antes.clone();
        IntegridadeMembros.preservarBracos(antes, pose, l.getBracosEmRepouso());
        for (int i = 14; i < 22; i++) {
            assertTrue(Math.abs(pose[i] - antes[i]) <= 1, "indice " + i);
        }
    }
}
