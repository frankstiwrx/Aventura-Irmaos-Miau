package com.irmaosmiau.animacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.posicional.CategoriaFinalizacao;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.MotorSolo.TipoEfeitoVisual;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.4: poses provisorias sem membro esticado; pernas proprias por guarda; contorno. */
class Proporcoes5Q4Test {

    @Test
    void posesProvisoriasNaoEsticamOsBracos() {
        for (TipoEfeitoVisual t : TipoEfeitoVisual.values()) {
            for (CategoriaFinalizacao c : CategoriaFinalizacao.values()) {
                for (double p = 0; p <= 1.0001; p += 0.1) {
                    int[] pose = new int[30];
                    AnimadorSolo.aplicarEfeito(pose, new MotorSolo.EfeitoVisual(t, p, c));
                    for (int[] mc : new int[][] {{20, 18}, {16, 14}}) {
                        double mao = Math.hypot(pose[mc[0]], pose[mc[0] + 1]);
                        assertTrue(mao <= AnimadorSolo.LIMITE_MAO + 1, t + " " + c + " mao " + mao);
                        // antebraco: a mao se afasta do cotovelo no maximo metade do alcance
                        double ante = Math.hypot(pose[mc[0]] - pose[mc[1]], pose[mc[0] + 1] - pose[mc[1] + 1]);
                        assertTrue(ante <= AnimadorSolo.LIMITE_MAO / 2.0 + 1.5, t + " " + c + " antebraco " + ante);
                    }
                }
            }
        }
    }

    @Test
    void estrangulamentoAindaMexeOsBracos() {
        int[] pose = new int[30];
        AnimadorSolo.aplicarEfeito(pose, new MotorSolo.EfeitoVisual(TipoEfeitoVisual.FINALIZANDO, 1.0,
                CategoriaFinalizacao.ESTRANGULAMENTO));
        assertTrue(pose[20] > 0 && pose[21] > 0, "a leitura do golpe continua (so sem esticar)");
        assertNotEquals(0, pose[18]);
    }

    @Test
    void pernasDasGuardasMantemOComprimentoEDiferem() {
        Set<String> desenhos = new HashSet<>();
        for (Posicao p : PernasGuarda.GUARDAS.keySet()) {
            for (boolean preto : new boolean[] {false, true}) {
                int[] pose = new int[30];
                IrmaoMiau l = preto ? new com.irmaosmiau.entities.IrmaoMiauPreto(0, 0, 0.7)
                        : new com.irmaosmiau.entities.IrmaoMiauBranco(0, 0, 0.7);
                PernasGuarda.aplicar(pose, l, p);
                for (boolean frente : new boolean[] {false, true}) {
                    double[] ref = PernasGuarda.comprimentosReferencia(preto, frente);
                    double[] cm = PernasGuarda.comprimentos(pose, preto, frente);
                    assertEquals(ref[0], cm[0], 1.5, p + " coxa");
                    assertEquals(ref[1], cm[1], 1.5, p + " canela");
                }
                if (!preto) {
                    desenhos.add(Arrays.toString(Arrays.copyOfRange(pose, 22, 30)));
                }
            }
        }
        assertEquals(PernasGuarda.GUARDAS.size(), desenhos.size(), "cada guarda tem pernas proprias");
        assertEquals(9, PernasGuarda.GUARDAS.size(), "fechada, aberta, meia, borboleta, DLR, laco, aranha, SLX, ashi");
    }

    @Test
    void contornoPertoDaCorEDiferenteDela() {
        for (Color c : new Color[] {Color.WHITE, Color.BLACK, new Color(40, 80, 190)}) {
            Color k = IrmaoMiau.corContorno(c);
            assertNotEquals(c, k);
            int dist = Math.abs(k.getRed() - c.getRed()) + Math.abs(k.getGreen() - c.getGreen())
                    + Math.abs(k.getBlue() - c.getBlue());
            assertTrue(dist <= 3 * 90, "discreto: " + c + " -> " + k);
        }
    }
}
