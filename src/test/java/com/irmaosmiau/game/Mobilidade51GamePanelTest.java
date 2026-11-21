package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.VersaoMovimentos;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoAcrobacia;

import org.junit.jupiter.api.Test;

/** Entrega 5O.1: R3 contextual pela postura (em pe acrobacia; joelhos rolamento; deitado rolamento lento). */
class Mobilidade51GamePanelTest extends Base5OGamePanel {

    private static final int MIN = GamePanel.LIMITE_ESQUERDO_ARENA;

    private static void deitar(IrmaoMiau l) {
        if (l.isAjoelhado()) {
            l.alternarDeitado();
        }
        l.alternarDeitado();
        assertTrue(l.isDeitado());
    }

    private String desenho(IrmaoMiau l) {
        return painel.getAnimador().getMovimentos().descricao(l);
    }

    /** R3 (rolar) + direcao do J1 numa tecla; devolve o x antes. */
    private int r3J1(String direcao) {
        int x = j1().getX();
        tecla(direcao, true);
        toque("brancoRolar");
        tecla(direcao, false);
        return x;
    }

    @Test
    void emPeR3FazAcrobaciaPelaDirecaoDoCorpo() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            r3J1("brancoCima");
            assertEquals(TipoAcrobacia.ESTRELA_ESQUERDA, j1().getAcrobacia());
            assertFalse(j1().isRolando());
            novo(InicioLuta.EM_PE);
            r3J1("brancoEsquerda");
            assertEquals(TipoAcrobacia.MORTAL_TRAS, j1().getAcrobacia());
            // J2 pelo teclado-controle: M (R3) + A (para onde olha) = mortal para a frente
            tecla("pretoControleA", true);
            tecla("pretoControleM", true);
            ticks(2);
            assertEquals(TipoAcrobacia.MORTAL_FRENTE, j2().getAcrobacia());
            tecla("pretoControleA", false);
            tecla("pretoControleM", false);
        });
    }

    @Test
    void deJoelhosR3RolaNaVelocidadeDeSempreEContinuaDeJoelhos() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.DE_JOELHOS);
            double f = j1().getFolegoAtual();
            int x = r3J1("brancoEsquerda");
            assertTrue(j1().isRolando());
            assertNull(j1().getAcrobacia());
            // 5Q.6 (pedido): de joelhos o rolamento ficou um pouco menos acelerado (22 -> 28, mesma distancia)
            // 5Q.7 (pedido): mais 40% mais lento (28 -> 46 ticks, mesma distancia)
            // 5Q.8 (pedido): mais 40% mais lento de novo (46 -> 76 ticks, mesma distancia)
            // adendo 5Q.8 (pedido): a velocidade do rolamento deitado (76 -> 37 ticks, mesma distancia)
            assertEquals(37, j1().getDuracaoRolamentoAtual(), "rolamento de joelhos (37 ticks)");
            assertEquals(f - IrmaoMiau.custoRolamento(), j1().getFolegoAtual(), 0.1);
            ticks(80);                                           // 5Q.8 (pedido): o rolamento dura 76 ticks (era 46)
            assertFalse(j1().isRolando());
            assertTrue(j1().isAjoelhado(), "a postura nao muda");
            assertEquals(154, x - j1().getX(), 3, "mesma distancia do rolamento de sempre (22 x 7)");
        });
    }

    @Test
    void deitadoR3RolaMaisDevagarAMesmaDistanciaEContinuaDeitado() throws Exception {
        naEdt(() -> {
            VersaoMovimentos.definir(VersaoMovimentos.EXPERIMENTAL_5O);
            novo(InicioLuta.EM_PE);
            deitar(j1());
            ticks(40);
            int x = r3J1("brancoEsquerda");
            assertTrue(j1().isRolando());
            int dur = j1().getDuracaoRolamentoAtual();
            assertEquals(IrmaoMiau.duracaoRolamento(IrmaoMiau.EstadoCorporal.DEITADO), dur);
            assertTrue(dur > 22, "deitado e mais lento: " + dur);
            assertEquals(0.6, IrmaoMiau.MULTIPLICADOR_ROLAMENTO_DEITADO, 1e-9, "multiplicador central");
            ticks(5);
            assertTrue(desenho(j1()).endsWith(":DEITADO"), desenho(j1()));
            ticks(22 - 5);
            assertTrue(j1().isRolando(), "no tempo do rolamento de joelhos ainda rola");
            ticks(dur);
            assertFalse(j1().isRolando());
            assertTrue(j1().isDeitado(), "nao levanta sozinho");
            assertEquals(154, x - j1().getX(), 3, "mesma distancia, so mais devagar");
        });
    }

    @Test
    void custosRolamentoMaisBaratoQueMortalEEstrelaIntermediaria() {
        assertTrue(IrmaoMiau.custoRolamento() < TipoAcrobacia.ESTRELA_ESQUERDA.getCusto());
        assertTrue(TipoAcrobacia.ESTRELA_ESQUERDA.getCusto() < TipoAcrobacia.MORTAL_FRENTE.getCusto());
        assertTrue(IrmaoMiau.custoRolamento() < TipoAcrobacia.MORTAL_TRAS.getCusto());
        assertEquals(4.0, IrmaoMiau.custoRolamento(), 1e-9);
    }

    @Test
    void custoDoRolamentoNaoSobeDeitado() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            deitar(j1());
            ticks(40);
            double f = j1().getFolegoAtual();
            r3J1("brancoEsquerda");
            assertEquals(f - IrmaoMiau.custoRolamento(), j1().getFolegoAtual(), 0.1);
        });
    }

    @Test
    void oitoDirecoesDeJoelhosNasDuasVersoes() throws Exception {
        String[][] dirs = {{"brancoDireita"}, {"brancoEsquerda"}, {"brancoCima"}, {"brancoBaixo"},
            {"brancoCima", "brancoDireita"}, {"brancoBaixo", "brancoDireita"}, {"brancoCima", "brancoEsquerda"},
            {"brancoBaixo", "brancoEsquerda"}};
        naEdt(() -> {
            for (VersaoMovimentos v : VersaoMovimentos.values()) {
                VersaoMovimentos.definir(v);
                for (String[] d : dirs) {
                    novo(InicioLuta.DE_JOELHOS);
                    for (String k : d) {
                        tecla(k, true);
                    }
                    toque("brancoRolar");
                    for (String k : d) {
                        tecla(k, false);
                    }
                    assertTrue(j1().isRolando(), v + " " + String.join("+", d));
                    ticks(3);
                    if (v == VersaoMovimentos.ORIGINAL) {
                        assertNull(desenho(j1()), "F7 ORIGINAL: desenho de sempre");
                    } else {
                        assertTrue(desenho(j1()).startsWith("rolamento_5o:"), desenho(j1()));
                    }
                    ticks(30);
                    assertTrue(j1().isAjoelhado());
                }
            }
        });
    }

    @Test
    void yLivreNaoFazAcrobacia() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("pretoControleI", true);                       // Y do J2
            tecla("pretoControleA", true);
            ticks(5);
            assertFalse(j2().isEmAcrobacia(), "Y + direcao nao faz mais acrobacia");
            tecla("pretoControleI", false);
            tecla("pretoControleA", false);
            tecla("brancoReforco", true);                        // Y do J1 (reforco no Classico)
            tecla("brancoCima", true);
            ticks(5);
            assertFalse(j1().isEmAcrobacia());
        });
    }

    @Test
    void rolamentoDeJoelhosNaBordaESemFugaDaPegada() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("brancoEsquerda", true);
            ticks(400);
            tecla("brancoEsquerda", false);
            j1().ajoelhar();
            ticks(30);
            for (int i = 0; i < 3; i++) {
                r3J1("brancoEsquerda");
                for (int t = 0; t < 30; t++) {
                    ticks(1);
                    assertTrue(j1().getX() >= MIN, "dentro da arena");
                }
            }
            assertEquals(MIN, j1().getX());

            novo(InicioLuta.DE_JOELHOS);
            j1().alternarDeitado();                              // J1 levanta; o J2 fica de joelhos
            ticks(30);
            j1Pega(1);
            assertTrue(s().isInteracaoEstabelecida());
            assertTrue(j2().isAjoelhado());
            tecla("pretoControleD", true);
            tecla("pretoControleM", true);
            ticks(3);
            assertFalse(j2().isRolando() || j2().isEmAcrobacia(), "receptor preso nao rola para fugir");
            assertTrue(s().estaEmInteracao());
        });
    }
}
