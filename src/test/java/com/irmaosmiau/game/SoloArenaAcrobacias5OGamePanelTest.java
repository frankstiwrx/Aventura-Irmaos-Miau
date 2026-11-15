package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoAcrobacia;

import org.junit.jupiter.api.Test;

/** Entrega 5O (testes 37-61 e acrobacias): pegadas no solo, limites da arena e Y + direcao. */
class SoloArenaAcrobacias5OGamePanelTest extends Base5OGamePanel {

    private static final int MIN = GamePanel.LIMITE_ESQUERDO_ARENA;
    private static final int MAX = GamePanel.LIMITE_DIREITO_ARENA;

    private static void deitar(IrmaoMiau l) {
        if (l.isAjoelhado()) {
            l.alternarDeitado();                    // de joelhos -> em pe
        }
        l.alternarDeitado();                        // em pe -> deitado
        assertTrue(l.isDeitado());
    }

    private static void emPe(IrmaoMiau l) {
        if (l.isAjoelhado() || l.isDeitado()) {
            l.alternarDeitado();
        }
    }

    // =====================================================
    // SOLO
    // =====================================================
    // 37-43: a regra e do motor (motivo explicito); em pe e de joelhos preservadas
    @Test
    void pegadasPorPostura() throws Exception {
        naEdt(() -> {
            String[] posturas = {"EM_PE", "AJOELHADO", "DEITADO"};
            for (String a : posturas) {
                for (String b : posturas) {
                    novo(InicioLuta.DE_JOELHOS);           // perto: o alcance nao atrapalha a comparacao
                    for (int k = 0; k < 2; k++) {
                        IrmaoMiau l = k == 0 ? j1() : j2();
                        String p = k == 0 ? a : b;
                        if (p.equals("EM_PE")) {
                            emPe(l);
                        } else if (p.equals("DEITADO")) {
                            deitar(l);
                        }
                    }
                    boolean solo = a.equals("DEITADO") || b.equals("DEITADO");
                    for (int n = 1; n <= 4; n++) {
                        String m = s().motivoIndisponivel(j1(), j2(), "pegada_" + n);
                        String caso = a + " x " + b + " pegada_" + n;
                        if (solo) {
                            assertEquals("INDISPONIVEL NO SOLO", m, caso);
                            assertFalse(s().podeTentarAcao(j1(), j2(), "pegada_" + n), caso);
                        } else {
                            assertNotEquals("INDISPONIVEL NO SOLO", m, caso);
                        }
                    }
                }
            }
        });
    }

    // 44-46: pelo comando (motor, nao HUD): nada de pegada, nada de folego, nada de estado
    @Test
    void comandoInvalidoNaoCustaNemCriaPegada() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.DE_JOELHOS);
            deitar(j2());
            double f = j1().getFolegoAtual();
            assertEquals(ResultadoAcao.INDISPONIVEL, s().tentarAcao(j1(), j2(), "pegada_1"));
            toque("brancoPegada1");
            assertFalse(s().estaEmInteracao());
            assertEquals(f, j1().getFolegoAtual(), 0.05);
            // HUD: nada de pegadas; 5P.1 (pedido): o "EM BREVE" sai e entram os comandos do solo livre
            String t = painel.montarQuadroDpad(SlotJogador.JOGADOR_2).nomeCamada();
            assertTrue(t.startsWith("SOLO LIVRE") && t.contains("NO CHÃO"), t);
            assertFalse(t.contains("EM BREVE"), t);
        });
    }

    // transicao de postura em andamento tambem bloqueia
    @Test
    void transicaoBloqueia() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.DE_JOELHOS);
            j2().executarTransicao(j2().proximaTransicaoSimples());
            assertTrue(j2().getTransicaoAtiva() != null);
            assertEquals("MUDANDO DE POSTURA", s().motivoIndisponivel(j1(), j2(), "pegada_1"));
        });
    }

    // 47-50: com a pegada valida, mudar de postura nao vale (nada preso, nada de fuga); ROMPER continua
    @Test
    void posturaNaPegadaERompimento() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            assertTrue(s().isInteracaoEstabelecida());
            int x = j2().getX();
            for (String k : new String[] {"pretoDeitar", "pretoDireita", "pretoCima"}) {
                tecla(k, true);
                ticks(40);
                tecla(k, false);
            }
            ticks(20);
            assertTrue(s().isInteracaoEstabelecida(), "nem deitar, nem andar, nem trocar de linha soltam");
            assertFalse(j2().isDeitado());
            assertEquals(x, j2().getX());
            j1().consumirFolegoCombate(j1().getFolegoAtual());
            toque("pretoRompimento");
            assertFalse(s().estaEmInteracao(), "ROMPER continua sendo a saida");
            ticks(30);
            assertFalse(j2().isPoseCombateExternaAtiva() && j2().getDefesa().isAtiva(), "nada preso");
        });
    }

    // =====================================================
    // ARENA
    // =====================================================
    private void segurar(String[] teclas, int n) {
        for (String k : teclas) {
            tecla(k, true);
        }
        for (int i = 0; i < n; i++) {
            ticks(1);
            for (IrmaoMiau l : new IrmaoMiau[] {j1(), j2()}) {
                assertTrue(l.getX() >= MIN && l.getX() <= MAX, "fora da arena: " + l.getX());
                assertTrue(l.getLinhaAtual() >= 0 && l.getLinhaAtual() <= 4);
            }
        }
        for (String k : teclas) {
            tecla(k, false);
        }
    }

    // 51-55: andar, correr e pular nao passam das bordas; linhas 1 e 5
    @Test
    void movimentoLivreRespeitaAArena() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            segurar(new String[] {"brancoEsquerda"}, 400);
            assertEquals(MIN, j1().getX());
            segurar(new String[] {"pretoDireita", "pretoCorrer"}, 400);
            assertEquals(MAX, j2().getX());
            segurar(new String[] {"brancoEsquerda", "brancoPular"}, 120);
            assertEquals(MIN, j1().getX());
            segurar(new String[] {"brancoCima"}, 200);
            assertEquals(0, j1().getLinhaAtual());
            segurar(new String[] {"brancoBaixo"}, 300);
            assertEquals(4, j1().getLinhaAtual());
        });
    }

    // 56-58: rolamento, sprawl e acrobacias na borda
    @Test
    void movimentosEspeciaisNaBorda() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            segurar(new String[] {"brancoEsquerda"}, 400);
            for (int i = 0; i < 3; i++) {
                tecla("brancoEsquerda", true);
                toque("brancoRolar");
                segurar(new String[] {"brancoEsquerda"}, 40);
            }
            tecla("brancoModDefesa", true);
            toque("brancoPegada3");
            segurar(new String[] {}, 40);
            tecla("brancoModDefesa", false);
            // 5O.1 (pedido): acrobacia = R3 (rolar) + direcao em pe (antes Y/9 + direcao)
            tecla("brancoEsquerda", true);
            toque("brancoRolar");
            segurar(new String[] {"brancoEsquerda"}, 60);                      // mortal para tras contra a parede
            assertEquals(MIN, j1().getX());
        });
    }

    // 59-61: a pegada acaba na borda com o jogador segurando a direcao: continua dentro
    @Test
    void fimDaPegadaNaBorda() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            tecla("brancoControle", true);
            segurar(new String[] {"brancoEsquerda"}, 120);
            int d = j2().getX() - j1().getX();
            segurar(new String[] {"brancoEsquerda"}, 200);
            assertEquals(d, j2().getX() - j1().getX(), "a dupla nao se separa");
            s().encerrarInteracao();
            tecla("brancoControle", false);
            segurar(new String[] {"brancoEsquerda", "pretoEsquerda"}, 300);
            assertEquals(MIN, Math.min(j1().getX(), j2().getX()));
        });
    }

    // =====================================================
    // ACROBACIAS
    // =====================================================
    // 5O.1 (pedido): R3 (R / rolar) + direcao relativa ao corpo, em pe (antes Y/9); custo; recarga;
    // segurado nao repete
    @Test
    void acrobaciasPelaDirecaoDoCorpo() throws Exception {
        String[][] casos = {{"brancoDireita", "MORTAL_FRENTE"}, {"brancoEsquerda", "MORTAL_TRAS"},
            {"brancoCima", "ESTRELA_ESQUERDA"}, {"brancoBaixo", "ESTRELA_DIREITA"}};
        naEdt(() -> {
            for (String[] c : casos) {
                novo(InicioLuta.EM_PE);
                segurar(new String[] {"brancoEsquerda"}, 30);           // espaco para o mortal para a frente
                double f = j1().getFolegoAtual();
                tecla(c[0], true);
                tecla("brancoRolar", true);
                ticks(2);
                TipoAcrobacia t = TipoAcrobacia.valueOf(c[1]);
                assertEquals(t, j1().getAcrobacia(), c[0]);
                assertEquals(f - t.getCusto(), j1().getFolegoAtual(), 0.05);
                assertTrue(painel.getAnimador().getMovimentos().descricao(j1()).startsWith("acrobacia:" + t.name()));
                ticks(t.getDuracaoTicks() + 2);
                assertFalse(j1().isEmAcrobacia());
                ticks(40);
                assertFalse(j1().isEmAcrobacia(), "segurado nao repete");
                tecla(c[0], false);
                tecla("brancoRolar", false);
            }
            // J2 olhando para a esquerda: para a esquerda = para a frente
            novo(InicioLuta.EM_PE);
            tecla("pretoEsquerda", true);
            tecla("pretoRolar", true);
            ticks(2);
            assertEquals(TipoAcrobacia.MORTAL_FRENTE, j2().getAcrobacia());
        });
    }

    // Recarga e agarrado: nao sai; R3 sozinho nao faz nada.
    // 5O.1 (pedido): de joelhos, R3 + direcao agora ROLA (antes: recusada "SO EM PE").
    @Test
    void acrobaciaRecusadaQuandoNaoCabe() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("brancoRolar", true);
            ticks(5);
            assertFalse(j1().isEmAcrobacia(), "R3 sozinho");
            tecla("brancoRolar", false);
            ticks(1);
            tecla("brancoCima", true);
            toque("brancoRolar");
            assertTrue(j1().isEmAcrobacia());
            tecla("brancoCima", false);
            ticks(TipoAcrobacia.ESTRELA_ESQUERDA.getDuracaoTicks() + 1);
            tecla("brancoBaixo", true);
            toque("brancoRolar");
            assertFalse(j1().isEmAcrobacia(), "na recarga");
            assertEquals("EM RECUPERACAO", painel.ultimoMotivoFamilia(SlotJogador.JOGADOR_1));
            tecla("brancoBaixo", false);

            novo(InicioLuta.DE_JOELHOS);
            tecla("brancoCima", true);
            toque("brancoRolar");
            assertNull(j1().getAcrobacia());
            assertTrue(j1().isRolando(), "de joelhos: rolamento tecnico");
            assertTrue(j1().isAjoelhado(), "continua de joelhos");
            tecla("brancoCima", false);

            novo(InicioLuta.EM_PE);
            j1Pega(1);
            tecla("pretoCima", true);
            toque("pretoRolar");
            assertFalse(j2().isEmAcrobacia() || j2().isRolando(), "sem fuga gratuita da pegada");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // Nao atravessa o adversario; no ar evita pegadas (como o pulo), nada alem disso
    @Test
    void acrobaciaNaoAtravessaOAdversario() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.DE_JOELHOS);
            emPe(j1());
            emPe(j2());
            ticks(2);
            int lado = Integer.signum(j1().getX() - j2().getX());
            tecla("brancoDireita", true);
            tecla("brancoRolar", true);                                // 5O.1 (pedido): R3 em vez de Y
            boolean viuNoAr = false;
            for (int i = 0; i < 40; i++) {
                ticks(1);
                viuNoAr |= "ACROBACIA".equals(j1().getEvasaoAtiva());
                assertEquals(lado, Integer.signum(j1().getX() - j2().getX()), "nao passou para o outro lado");
                assertTrue(Math.abs(j1().getX() - j2().getX()) >= GamePanel.DISTANCIA_MINIMA_ACROBACIA - 1
                        || !j1().isEmAcrobacia());
            }
            assertTrue(viuNoAr, "no ar evita pegadas, como o pulo");
        });
    }
}
