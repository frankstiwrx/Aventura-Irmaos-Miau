package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.IntegridadeMembros;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.8: sequencias ALEATORIAS (semente fixa) na pegada em pe — rasteira, forcar queda,
 * empurrao, RT em instantes variados, arrastar a dupla, desequilibrar — com J1 e J2 atacando. Em
 * cada quadro o DEFENSOR fica integro (segmentos, tronco no quadril, corpo inclinando inteiro: a
 * cintura acompanha o tronco) e, depois de tudo, a pegada volta exatamente a pose limpa (sem drift).
 */
class Integridade5Q8GamePanelTest extends Base5OGamePanel {

    private void sequencias(long semente, int partidas) {
        Random r = new Random(semente);
        for (int partida = 0; partida < partidas; partida++) {
            novo(InicioLuta.EM_PE);
            Lado ini = r.nextBoolean() ? Lado.A : Lado.B;
            IrmaoMiau a = ini == Lado.A ? j1() : j2();
            IrmaoMiau b = ini == Lado.A ? j2() : j1();
            String pa = ini == Lado.A ? "branco" : "preto";
            String pb = ini == Lado.A ? "preto" : "branco";
            String ir = ini == Lado.A ? "brancoDireita" : "pretoEsquerda";
            tecla(ir, true);
            for (int i = 0; i < 400 && !s().podeTentarAcao(a, b, "pegada_1"); i++) {
                ticks(1);
            }
            tecla(ir, false);
            ticks(2);
            toque(pa + "Pegada1");
            ticks(80);
            if (painel.getMotorSolo().getPosicao() != Posicao.EM_PE_PEGADA) {
                continue;
            }
            int[] ref = b.copiarPoseCombateExterna();
            String hist = "semente " + semente + " partida " + partida + " " + ini + ":";
            for (int passo = 0; passo < 10 && painel.getMotorSolo().getPosicao() == Posicao.EM_PE_PEGADA; passo++) {
                int acao = r.nextInt(6);
                switch (acao) {
                    case 0 -> painel.getMotorSolo().comando(ini, new ComandoSolo(Direcao.CIMA, Familia.L2));
                    case 1 -> painel.getMotorSolo().comando(ini, new ComandoSolo(Direcao.BAIXO, Familia.L2));
                    case 2 -> painel.getMotorSolo().comando(ini, new ComandoSolo(Direcao.CIMA, Familia.R1));
                    case 3 -> {
                        tecla(pa + "Controle", true);
                        tecla(r.nextBoolean() ? pa + "Esquerda" : pa + "Direita", true);
                    }
                    case 4 -> {
                        painel.getMotorSolo().comando(ini, new ComandoSolo(Direcao.CIMA, Familia.L2));
                        ticks(1 + r.nextInt(6));
                        tecla(pb + "ModDefesa", true);
                    }
                    default -> {
                        painel.getMotorSolo().comando(ini, new ComandoSolo(Direcao.BAIXO, Familia.L2));
                        ticks(1 + r.nextInt(6));
                        tecla(pb + "ModDefesa", true);
                    }
                }
                hist += " " + acao;
                int dur = 6 + r.nextInt(50);
                for (int i = 0; i < dur && painel.getMotorSolo().getPosicao() == Posicao.EM_PE_PEGADA; i++) {
                    ticks(1);
                    int[] q = b.copiarPoseCombateExterna();
                    if (q == null || b.getTransicaoAtiva() != null) {
                        continue;                             // caindo de verdade: a transicao de queda (aprovada)
                    }
                    double[] d = IntegridadeMembros.desvios(ref, q, b);
                    String o = hist + " t" + i + ": ";
                    // ate 4 px: o passo APROVADO de arrastar a dupla (camada da pegada) somado ao passo da reacao
                    // de desequilibrio; as tecnicas ficam em ~2 px
                    assertTrue(d[0] <= 4.0, o + "segmento " + d[0]);
                    assertTrue(d[1] <= 0.5, o + "tronco fora do quadril " + d[1]);
                    assertTrue(d[3] <= IntegridadeMembros.TORCAO_MAXIMA_NA_PEGADA + 4, o + "cintura quebrando " + d[3]);
                }
                for (String k : new String[] {"Controle", "Esquerda", "Direita", "ModDefesa"}) {
                    tecla(pa + k, false);
                    tecla(pb + k, false);
                }
            }
            ticks(300);
            if (painel.getMotorSolo().getPosicao() == Posicao.EM_PE_PEGADA) {
                assertArrayEquals(ref, b.copiarPoseCombateExterna(), hist + ": sem drift na pegada");
            }
        }
    }

    @Test
    void sequenciasAleatoriasNaoDeformamNemDeixamDrift() throws Exception {
        naEdt(() -> {
            sequencias(1, 8);
            sequencias(4, 8);
        });
    }
}
