package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.AnimadorJoelhosAcelerado;
import com.irmaosmiau.animacao.FonteAnimacao;
import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.6 no GamePanel real: levantada depois de uma queda sem "teletransporte"
 * (a transicao desenha do comeco ao fim), corrida de joelhos para tras inclinando para
 * tras, guarda aberta x fechada ao sair por cima, afastamento com recuo no chao,
 * rolamento de joelhos e sprawl um pouco mais lentos.
 */
class Movimento5Q6GamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void derrubarJ2() {
        j1Pega(1);
        tecla("brancoControle", true);
        tecla("brancoEsquerda", true);
        ticks(6);
        tecla("brancoEsquerda", false);
        tecla("brancoControle", false);
        m().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));
        ticks(80);
        assertTrue(j2().isDeitado(), "caiu");
    }

    @Test
    void levantadaDepoisDaQuedaDesenhaDoComecoAoFimSemSalto() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            derrubarJ2();
            m().comando(Lado.B, new ComandoSolo(Direcao.CIMA, Familia.RS));   // levantar tecnico
            int[] antes = null;
            double dyAntes = Double.NaN;
            int transicao = 0;
            for (int i = 0; i < 70; i++) {
                ticks(1);
                if (j2().getTransicaoAtiva() == null) {
                    continue;
                }
                transicao++;
                assertEquals(FonteAnimacao.PROCEDURAL, painel.getAnimador().origem(j2()).orElseThrow().fonte(),
                        "a transicao desenha (antes ficava congelada e recusada)");
                int[] p = j2().copiarPoseCombateExterna();
                assertNotNull(p);
                double dy = j2().getDeslocamentoVisualCombateY();
                if (antes != null) {
                    assertTrue(Math.abs(p[10] - antes[10]) <= 8, "rotacao gradual: " + antes[10] + " -> " + p[10]);
                    assertTrue(Math.abs(dy - dyAntes) <= 8, "subida gradual: " + dyAntes + " -> " + dy);
                }
                antes = p;
                dyAntes = dy;
            }
            assertTrue(transicao >= 20, "a levantada leva tempo: " + transicao);
            assertFalse(j2().isDeitado());
        });
    }

    @Test
    void corridaDeJoelhosParaTrasInclinaParaTrasNosDoisLados() throws Exception {
        naEdt(() -> {
            for (String quem : new String[] {"branco", "preto"}) {
                novo(InicioLuta.DE_JOELHOS);
                IrmaoMiau l = quem.equals("branco") ? j1() : j2();
                boolean olhaDireita = l.isViradoParaDireita();
                String tras = olhaDireita ? "Esquerda" : "Direita";
                tecla(quem + "Correr", true);
                tecla(quem + tras, true);
                ticks(12);
                int[] p = l.copiarPoseCombateExterna();
                tecla(quem + tras, false);
                tecla(quem + "Correr", false);
                assertTrue(l.isAjoelhado());
                assertNotNull(p, quem + ": passada acelerada");
                int[] frente = AnimadorJoelhosAcelerado.pose(l, 0, true);
                int[] costas = AnimadorJoelhosAcelerado.pose(l, 0, false);
                assertTrue(costas[9] < frente[9], "para tras o tronco vai para tras");
                assertTrue(p[9] < frente[9], quem + ": andando de costas nao inclina para a frente (" + p[9] + ")");
            }
        });
    }

    @Test
    void guardaFechadaRetemMaisQueAAberta() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.GUARDA, Lado.A, Lado.B);
            ticks(40);
            MotorSolo.Resposta r = m().comando(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.R1));
            assertEquals("FAÇA POSTURA ANTES (RS↓)", r.motivo(), "na fechada precisa trabalhar a postura");
            m().comando(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.RS));       // postura
            ticks(MotorSolo.RECARGA_TICKS);
            m().definirEstabilidade(70);
            assertEquals(MotorSolo.Resultado.INICIADA, m().comando(Lado.A,
                    new ComandoSolo(Direcao.BAIXO, Familia.R1)).resultado());
            int janelaFechada = m().getTentativa().orElseThrow().janelaTotal();
            for (int i = 0; i < 100 && m().getTentativa().isPresent(); i++) {
                ticks(1);
            }
            assertEquals(Posicao.GUARDA, m().getPosicao(), "com controle 70 a fechada segura");

            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
            ticks(40);
            m().definirEstabilidade(70);
            assertEquals(MotorSolo.Resultado.INICIADA, m().comando(Lado.A,
                    new ComandoSolo(Direcao.BAIXO, Familia.R1)).resultado(), "na aberta sai sem postura");
            assertTrue(m().getTentativa().orElseThrow().janelaTotal() < janelaFechada, "e mais rapido");
            for (int i = 0; i < 100 && m().getTentativa().isPresent(); i++) {
                ticks(1);
            }
            assertEquals(Posicao.EM_PE_CONTRA_SOLO, m().getPosicao(), "a aberta sem controle efetivo nao prende");
        });
    }

    @Test
    void afastarComOsPesQuemEstaNoChaoTambemRecua() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.GUARDA, Lado.B, Lado.A);
            ticks(40);
            assertEquals(MotorSolo.Resultado.INICIADA, m().comando(Lado.A,
                    new ComandoSolo(Direcao.BAIXO, Familia.R1)).resultado());
            for (int i = 0; i < 100 && m().getTentativa().isPresent(); i++) {
                ticks(1);
            }
            int x0 = j1().getX();
            ticks(30);
            assertTrue(Math.abs(j1().getX() - x0) >= 8, "quem empurrou com os pes escorregou para tras");
            assertTrue(Math.abs(j1().getX() - j2().getX()) >= MotorSolo.DISTANCIA_APOS_AFASTAR - 1);
        });
    }

    @Test
    void rolamentoDeJoelhosESprawlMenosAcelerados() {
        // 5Q.7 (pedido): o rolamento de joelhos ficou mais 40% mais lento (28 -> 46 ticks)
        // 5Q.8 (pedido): e mais 40% (46 -> 76 ticks)
        // adendo 5Q.8 (pedido): o rolamento de joelhos usa a velocidade do deitado (76 -> 37) e o sprawl
        // ficou 20% mais lento (32 -> 40)
        assertEquals(37, IrmaoMiau.duracaoRolamento(EstadoCorporal.AJOELHADO));
        assertEquals(22, IrmaoMiau.duracaoRolamento(EstadoCorporal.EM_PE), "em pe nao mudou");
        assertEquals(37, IrmaoMiau.duracaoRolamento(EstadoCorporal.DEITADO), "o deitado aprovado nao mudou");
        assertEquals(40, IrmaoMiau.duracaoSprawl());
    }
}
