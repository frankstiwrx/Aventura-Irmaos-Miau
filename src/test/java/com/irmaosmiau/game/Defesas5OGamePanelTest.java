package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.VersaoMovimentos;
import com.irmaosmiau.combate.ModoAnimacoesPegada;
import com.irmaosmiau.controles.FamiliaTecnica;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.CustosFolego;
import com.irmaosmiau.entities.DefesaCorporal;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import org.junit.jupiter.api.Test;

/** Entrega 5O (testes 2-6, 11, 14-16, 20-36 e as versoes ORIGINAL x EXPERIMENTAL do complemento). */
class Defesas5OGamePanelTest extends Base5OGamePanel {

    private static final String[][] GUARDAS = {
        {"Pegada1", "GUARDA_ALTA"}, {"Pegada4", "GUARDA_MEDIA"}, {"Pegada2", "GUARDA_BAIXA"}};

    private void guarda(String pre, String tecla) {
        tecla(pre + "ModDefesa", true);
        tecla(pre + tecla, true);
    }

    private void soltarGuarda(String pre, String tecla) {
        tecla(pre + tecla, false);
        tecla(pre + "ModDefesa", false);
    }

    private String desenho(IrmaoMiau l) {
        return painel.getAnimador().getMovimentos().descricao(l);
    }

    // 3-5, 20-22, 31-32, 11: as tres guardas sustentadas; protege o nivel; nao recomeca; nada de idle/taunt
    @Test
    void tresGuardasSustentadas() throws Exception {
        naEdt(() -> {
            for (String[] g : GUARDAS) {
                novo(InicioLuta.EM_PE);
                guarda("branco", g[0]);
                ticks(DefesaCorporal.TICKS_ENTRADA + 2);
                DefesaCorporal d = j1().getDefesa();
                assertEquals(DefesaCorporal.Tipo.valueOf(g[1]), d.getTipo());
                assertEquals(DefesaCorporal.Fase.ATIVA, d.getFase());
                assertTrue(d.protege(d.getTipo().getNivel()));
                assertEquals("guarda:" + g[1], desenho(j1()), "animacao propria");
                long antes = d.getTicksAtiva();
                for (int i = 0; i < 200; i++) {
                    ticks(1);
                    assertEquals(DefesaCorporal.Fase.ATIVA, d.getFase());
                    assertTrue(j1().isPoseCombateExternaAtiva(), "a pose da guarda e a desenhada (nada de idle)");
                }
                assertTrue(d.getTicksAtiva() >= antes + 200, "nao recomeca a cada tick");
                assertFalse(j1().estaEmTaunt(), "taunt nao substitui a guarda");
                assertFalse(s().estaEmInteracao(), "RT + direcao nunca pega");
                soltarGuarda("branco", g[0]);
            }
        });
    }

    // 10, complemento 7-10: as tres poses sao diferentes entre si
    @Test
    void guardasVisualmenteDistintas() throws Exception {
        int[][] poses = new int[3][];
        naEdt(() -> {
            for (int i = 0; i < 3; i++) {
                novo(InicioLuta.EM_PE);
                guarda("branco", GUARDAS[i][0]);
                ticks(DefesaCorporal.TICKS_ENTRADA + 2);
                poses[i] = j1().copiarPoseCombateExterna();
            }
        });
        for (int a = 0; a < 3; a++) {
            for (int b = a + 1; b < 3; b++) {
                int dif = 0;
                for (int k = 4; k < 30; k++) {
                    dif += Math.abs(poses[a][k] - poses[b][k]);
                }
                assertTrue(dif > 60, GUARDAS[a][1] + " x " + GUARDAS[b][1] + ": " + dif);
            }
        }
    }

    // 23-24: troca entre guardas e saida suaves (sem teleporte de articulacao)
    @Test
    void trocaESaidaSuaves() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            guarda("branco", "Pegada1");
            ticks(12);
            int[] antes = j1().copiarPoseCombateExterna();
            tecla("brancoPegada1", false);
            tecla("brancoPegada4", true);
            for (int i = 0; i < DefesaCorporal.TICKS_ENTRADA + 2; i++) {
                ticks(1);
                int[] p = j1().copiarPoseCombateExterna();
                for (int k = 4; k < 30; k++) {
                    assertTrue(Math.abs(p[k] - antes[k]) <= 30, "salto na articulacao " + k);
                }
                antes = p;
            }
            assertEquals(DefesaCorporal.Tipo.GUARDA_MEDIA, j1().getDefesa().getTipo());
            soltarGuarda("branco", "Pegada4");
            ticks(1);
            assertEquals(DefesaCorporal.Fase.SAINDO, j1().getDefesa().getFase());
            ticks(DefesaCorporal.TICKS_SAIDA + 2);
            assertFalse(j1().getDefesa().isAtiva());
            assertNull(desenho(j1()), "volta a postura normal");
        });
    }

    // 25-26: folego: entrada cobra uma vez; ativa nao recupera; sem folego nao entra; nunca negativo
    @Test
    void folegoDasGuardas() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1().consumirFolegoCombate(5.0);
            ticks(1);
            double f = j1().getFolegoAtual();
            guarda("branco", "Pegada1");
            ticks(1);
            // a recuperacao natural do tick acontece antes (no movimento); a entrada cobra 0,3 uma vez
            assertEquals(f - CustosFolego.GUARDA_ENTRADA, j1().getFolegoAtual(), 0.02);
            double f2 = j1().getFolegoAtual();
            ticks(200);
            assertTrue(j1().getFolegoAtual() <= f2 + 1e-9, "guardando nao recupera");
            soltarGuarda("branco", "Pegada1");
            ticks(20);
            j1().consumirFolegoCombate(j1().getFolegoAtual());
            guarda("branco", "Pegada4");
            ticks(3);
            assertFalse(j1().getDefesa().isAtiva(), "sem folego nao entra");
            assertEquals("SEM FOLEGO", painel.ultimoMotivoFamilia(SlotJogador.JOGADOR_1));
            assertTrue(j1().getFolegoAtual() >= 0);
        });
    }

    // 6, 27-28: sprawl (RT + baixo) com entrada, protecao e recuperacao; nao repete; recarga
    @Test
    void sprawlFasesERecarga() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            double f = j1().getFolegoAtual();
            guarda("branco", "Pegada3");
            ticks(1);
            assertTrue(j1().isFazendoSprawl());
            assertEquals(f - IrmaoMiau.custoSprawl(), j1().getFolegoAtual(), 0.05);
            java.util.Set<String> fases = new java.util.LinkedHashSet<>();
            int sprawls = 0;
            boolean antes = true;
            for (int i = 0; i < 60; i++) {
                if (j1().isFazendoSprawl()) {
                    fases.add(j1().getFaseSprawl());
                } else if (antes) {
                    sprawls++;
                }
                antes = j1().isFazendoSprawl();
                ticks(1);
            }
            assertEquals(java.util.List.of("ENTRADA", "PROTECAO", "RECUPERACAO"), java.util.List.copyOf(fases));
            assertEquals(1, sprawls, "segurado nao repete");
            soltarGuarda("branco", "Pegada3");
            ticks(1);
            toque("brancoModDefesa");
            assertTrue(j1().getRecargaSprawl() >= 0);
        });
    }

    @Test
    void sprawlEmSequenciaRespeitaARecarga() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("brancoModDefesa", true);
            toque("brancoPegada3");
            ticks(30);
            assertFalse(j1().isFazendoSprawl());
            assertTrue(j1().getRecargaSprawl() > 0);
            toque("brancoPegada3");
            assertFalse(j1().isFazendoSprawl(), "na recarga nao sai outro");
            assertEquals("EM RECUPERACAO", painel.ultimoMotivoFamilia(SlotJogador.JOGADOR_1));
            ticks(IrmaoMiau.RECARGA_SPRAWL_TICKS);
            toque("brancoPegada3");
            assertTrue(j1().isFazendoSprawl());
        });
    }

    // 29-30: sprawl bloqueado de joelhos, deitado e agarrado (sem fuga gratuita)
    @Test
    void sprawlBloqueadoNosEstadosIncompativeis() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.DE_JOELHOS);
            tecla("pretoModDefesa", true);
            toque("pretoPegada3");
            assertFalse(j2().isFazendoSprawl());
            assertEquals("SÓ EM PÉ", painel.ultimoMotivoFamilia(SlotJogador.JOGADOR_2));
            tecla("pretoModDefesa", false);

            novo(InicioLuta.EM_PE);
            j1Pega(1);
            assertTrue(s().isInteracaoEstabelecida());
            tecla("pretoModDefesa", true);
            toque("pretoPegada3");
            toque("pretoPegada1");
            assertFalse(j2().isFazendoSprawl());
            assertFalse(j2().getDefesa().isAtiva());
            assertEquals("PERSONAGEM AGARRADO", painel.ultimoMotivoFamilia(SlotJogador.JOGADOR_2));
            assertTrue(s().isInteracaoEstabelecida(), "a pegada continua");
        });
    }

    // 14, 16, 34-35: J2 no teclado, Branco e Preto, os dois lados do tatame
    @Test
    void j2NoTecladoEOsDoisLados() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            guarda("preto", "Pegada1");
            guarda("branco", "Pegada4");
            ticks(10);
            assertEquals(DefesaCorporal.Tipo.GUARDA_ALTA, j2().getDefesa().getTipo());
            assertEquals(DefesaCorporal.Tipo.GUARDA_MEDIA, j1().getDefesa().getTipo(), "independentes");
            assertFalse(j2().isViradoParaDireita());
            assertTrue(j1().isViradoParaDireita());
            assertEquals("guarda:GUARDA_ALTA", desenho(j2()));
            assertEquals(1, j2().copiarPoseCombateExterna()[3], "pose local; o GamePanel espelha quem olha p/ esquerda");
        });
    }

    // 13, 15: J1 no Xbox (simulado, Contextual) com RT + RS; J2 no teclado
    @Test
    void xboxSimuladoRtMaisRs() throws Exception {
        ProvedorGamepadSimulado sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID0", "Xbox 1");
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD)));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(
                MapaGamepad.padrao().comEsquema(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL)));
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            EstadoGamepad rt = EstadoGamepad.repouso().comEixo(EixoGamepad.GATILHO_DIR, 1f);
            sim.definir("g1", rt);
            ticks(2);
            assertEquals(FamiliaTecnica.DEFESAS, painel.familiaDo(SlotJogador.JOGADOR_1));
            assertTrue(painel.montarQuadroDpad(SlotJogador.JOGADOR_1).nomeCamada().startsWith("DEFESAS (RT"));
            sim.definir("g1", rt.comEixo(EixoGamepad.DIR_Y, -1f));
            ticks(10);
            assertEquals(DefesaCorporal.Tipo.GUARDA_ALTA, j1().getDefesa().getTipo());
            guarda("preto", "Pegada2");
            ticks(10);
            assertEquals(DefesaCorporal.Tipo.GUARDA_BAIXA, j2().getDefesa().getTipo());
            sim.definir("g1", rt);
            ticks(2);
            sim.definir("g1", rt.comEixo(EixoGamepad.DIR_Y, 1f));
            ticks(2);
            assertTrue(j1().isFazendoSprawl(), "RT + RS para baixo = sprawl");
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(2);
            assertEquals(FamiliaTecnica.PEGADAS, painel.familiaDo(SlotJogador.JOGADOR_1));
        });
    }

    // RB (tecla 4) + direcao: reservado, nada executa, motivo explicito; HUD "QUEDAS"
    @Test
    void quedasReservadas() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.DE_JOELHOS);
            tecla("brancoModProjecao", true);
            ticks(1);
            assertTrue(painel.montarQuadroDpad(SlotJogador.JOGADOR_1).nomeCamada().startsWith("QUEDAS"));
            toque("brancoPegada1");
            assertFalse(s().estaEmInteracao(), "nao pega nem derruba");
            assertEquals("EM PREPARAÇÃO", painel.ultimoMotivoFamilia(SlotJogador.JOGADOR_1));
            tecla("brancoModProjecao", false);
            tecla("brancoModDefesa", true);
            tecla("brancoModProjecao", true);
            ticks(1);
            assertEquals(FamiliaTecnica.DEFESAS, painel.familiaDo(SlotJogador.JOGADOR_1), "RT vence RB");
        });
    }

    // 36, complemento 20-22: F6 nao mexe nas guardas; F7 alterna sprawl/rolamento ORIGINAL x EXPERIMENTAL
    @Test
    void f6EF7() throws Exception {
        naEdt(() -> {
            ModoAnimacoesPegada.definir(ModoAnimacoesPegada.AUTORAL);
            novo(InicioLuta.EM_PE);
            guarda("branco", "Pegada1");
            ticks(8);
            assertEquals("guarda:GUARDA_ALTA", desenho(j1()), "guardas nos dois modos do F6");
            soltarGuarda("branco", "Pegada1");
            ticks(12);

            VersaoMovimentos.definir(VersaoMovimentos.ORIGINAL);
            tecla("brancoModDefesa", true);
            toque("brancoPegada3");
            ticks(3);
            assertTrue(j1().isFazendoSprawl());
            assertNull(desenho(j1()), "ORIGINAL: o desenho Java de sempre");
            ticks(30 + IrmaoMiau.RECARGA_SPRAWL_TICKS);
            painel.getActionMap().get("alternarMovimentos").actionPerformed(null);
            assertEquals(VersaoMovimentos.EXPERIMENTAL_5O, VersaoMovimentos.atual());
            toque("brancoPegada3");
            ticks(3);
            assertNotNull(desenho(j1()));
            assertTrue(desenho(j1()).startsWith("sprawl_5o"));
            assertTrue(String.join(" / ", painel.linhasTecnicas()).contains("MOV (F7): EXPERIMENTAL 5O"),
                    "F3 mostra a versao");
        });
    }

    // complemento 1-4, 15-16: rolamento R3/R + direcao: as 8 direcoes no experimental; mecanica igual nas duas versoes
    @Test
    void rolamentoNasOitoDirecoes() throws Exception {
        // 5O.1 (pedido): o R3 + direcao EM PE virou acrobacia; o rolamento tecnico sai de joelhos
        // (e deitado, mais lento). O teste das 8 direcoes passa a partir de joelhos.
        String[][] dirs = {{"brancoDireita"}, {"brancoEsquerda"}, {"brancoCima"}, {"brancoBaixo"},
            {"brancoCima", "brancoDireita"}, {"brancoBaixo", "brancoDireita"}, {"brancoCima", "brancoEsquerda"},
            {"brancoBaixo", "brancoEsquerda"}};
        String[] esperado = {"FRENTE", "TRAS", "LATERAL", "LATERAL", "FRENTE_DIAGONAL", "FRENTE_DIAGONAL",
            "TRAS_DIAGONAL", "TRAS_DIAGONAL"};
        naEdt(() -> {
            for (int i = 0; i < dirs.length; i++) {
                double[][] fim = new double[2][];
                for (VersaoMovimentos v : VersaoMovimentos.values()) {
                    VersaoMovimentos.definir(v);
                    novo(InicioLuta.DE_JOELHOS);
                    for (String k : dirs[i]) {
                        tecla(k, true);
                    }
                    toque("brancoRolar");
                    for (String k : dirs[i]) {
                        tecla(k, false);
                    }
                    ticks(5);
                    assertTrue(j1().isRolando());
                    if (v == VersaoMovimentos.EXPERIMENTAL_5O) {
                        assertEquals("rolamento_5o:" + esperado[i] + ":AJOELHADO", desenho(j1()),
                                String.join("+", dirs[i]));
                    } else {
                        assertNull(desenho(j1()), "original preservado");
                    }
                    ticks(40);
                    assertFalse(j1().isRolando());
                    fim[v.ordinal()] = new double[] {j1().getX(), j1().getY(), j1().getFolegoAtual()};
                }
                assertEquals(fim[0][0], fim[1][0], "mesma distancia");
                assertEquals(fim[0][1], fim[1][1]);
                assertEquals(fim[0][2], fim[1][2], 1e-9, "mesmo folego");
            }
        });
    }

    // 33: as guardas do Preto tambem (rig compartilhado)
    @Test
    void pretoExecutaAsGuardas() throws Exception {
        naEdt(() -> {
            for (String[] g : GUARDAS) {
                novo(InicioLuta.EM_PE);
                guarda("preto", g[0]);
                ticks(10);
                assertEquals("guarda:" + g[1], desenho(j2()));
                assertNotEquals(0, j2().copiarPoseCombateExterna()[16] | j2().copiarPoseCombateExterna()[20]);
            }
        });
    }
}
