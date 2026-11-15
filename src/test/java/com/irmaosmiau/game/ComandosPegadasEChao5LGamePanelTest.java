package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.AnimadorPartida;
import com.irmaosmiau.animacao.EstadoVisual;
import com.irmaosmiau.combate.EstadoInteracaoCombate;
import com.irmaosmiau.combate.ModoAnimacoesPegada;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5L-B/C (testes 11-13, 20-25) no GamePanel real: comandos das pegadas e a fuga de quadril. */
class ComandosPegadasEChao5LGamePanelTest {

    private GamePanel painel;
    private ProvedorGamepadSimulado sim;

    @AfterEach
    void restaurar() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
        ModoAnimacoesPegada.restaurarPadrao();
    }

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void tecla(String nome, boolean pressionada) {
        painel.getActionMap().get(nome + (pressionada ? "Pressionada" : "Solta")).actionPerformed(null);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private void toque(String nome) {
        tecla(nome, true);
        ticks(1);
        tecla(nome, false);
        ticks(1);
    }

    private SistemaCombate s() {
        return painel.getSistemaCombate();
    }

    private static EscolhaLutador oficial(String id) {
        return EscolhaLutador.oficial(CatalogoPersonagens.padrao().exigir(id));
    }

    private GamePanel dois(EscolhaLutador a, EscolhaLutador b) {
        GamePanel g = new GamePanel(TipoPersonagem.AMBOS, a, b, InicioLuta.DE_JOELHOS, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    /** Aproxima o J1 ate todas as pegadas alcancarem. */
    private void encostar() {
        tecla("brancoDireita", true);
        for (int i = 0; i < 300 && !s().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_4"); i++) {
            ticks(1);
        }
        tecla("brancoDireita", false);
        ticks(1);
    }

    private void gamepad(SlotJogador slot, EsquemaGamepad esquema) {
        sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID0", "Xbox 1");
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                AssociacaoDispositivos.padrao().com(slot, TipoDispositivo.GAMEPAD)));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(MapaGamepad.padrao().comEsquema(slot, esquema)));
    }

    private void rs(float x, float y) {
        sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_X, x).comEixo(EixoGamepad.DIR_Y, y));
        ticks(1);
        sim.definir("g1", EstadoGamepad.repouso());
        ticks(1);
    }

    // ---------------- 11 e 25: RS livre = 4 pegadas, Contextual e Classico, J1 e J2
    @Test
    void rsEscolheAsQuatroPegadasNosDoisEsquemas() throws Exception {
        float[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (EsquemaGamepad esquema : EsquemaGamepad.values()) {
            for (int i = 0; i < 4; i++) {
                gamepad(SlotJogador.JOGADOR_2, esquema);
                int k = i;
                naEdt(() -> {
                    painel = dois(null, null);
                    ticks(1);
                    encostar();
                    rs(dirs[k][0], dirs[k][1]);
                    String caso = esquema + " RS " + k;
                    assertTrue(s().estaEmInteracao(), caso);
                    assertSame(painel.getLutador2(), s().getAplicadorInteracao(), caso + ": J2 pelo gamepad");
                    assertEquals(EstadoInteracaoCombate.valueOf("PEGADA_" + (k + 1)), s().getEstadoInteracao(), caso);
                });
            }
        }
    }

    // ---------------- 12: com a pegada, o RS volta as acoes de pegada (Contextual) / nada (Classico)
    @Test
    void comPegadaORsVoltaAReforcarDesequilibrarERomper() throws Exception {
        gamepad(SlotJogador.JOGADOR_2, EsquemaGamepad.CONTEXTUAL);
        naEdt(() -> {
            painel = dois(null, null);
            ticks(1);
            toque("brancoPegada1");                         // J1 aplica; J2 (Xbox) e o receptor
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            rs(0, -1);                                      // receptor: cima nao e pegada nenhuma agora
            assertEquals(EstadoInteracaoCombate.PEGADA_1, s().getEstadoInteracao());
            assertSame(painel.getLutador1(), s().getAplicadorInteracao());
            rs(0, 1);                                       // receptor: baixo = ROMPER
            assertEquals(com.irmaosmiau.combate.TipoDisputa.ROMPIMENTO_TECNICO, s().getUltimaDisputa().getTipo());
        });
        gamepad(SlotJogador.JOGADOR_2, EsquemaGamepad.CLASSICO);
        naEdt(() -> {
            painel = dois(null, null);
            ticks(1);
            toque("brancoPegada1");
            ticks(40);
            rs(0, 1);
            assertTrue(s().isInteracaoEstabelecida(), "Classico: com pegada o RS nao faz nada");
            assertSame(painel.getLutador1(), s().getAplicadorInteracao());
        });
    }

    // ---------------- 11 e 14 (teclado): 6/7/8 e NUM 7/8/9
    @Test
    void tecladoTemAsQuatroPegadasNosDoisSlots() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            var mapa = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            assertEquals("brancoPegada2Pressionada", mapa.get(KeyStroke.getKeyStroke("pressed 6")));
            assertEquals("brancoPegada4Pressionada", mapa.get(KeyStroke.getKeyStroke("pressed 8")));
            assertEquals("pretoPegada3Pressionada", mapa.get(KeyStroke.getKeyStroke("pressed NUMPAD8")));
            ticks(1);
            encostar();
            toque("brancoPegada4");
            assertEquals(EstadoInteracaoCombate.PEGADA_4, s().getEstadoInteracao());
            ticks(40);
            assertTrue(painel.linhasTecnicas().get(8).contains("Gola e nuca"), painel.linhasTecnicas().get(8));
        });
    }

    // ---------------- 13: combinacoes de irmaos com as novas pegadas
    @Test
    void combinacoesDeIrmaosComAsNovasPegadas() throws Exception {
        String b = CatalogoPersonagens.ID_BRANCO;
        String p = CatalogoPersonagens.ID_PRETO;
        for (String[] c : new String[][] {{b, p}, {p, b}, {b, b}, {p, p}}) {
            naEdt(() -> {
                painel = dois(oficial(c[0]), oficial(c[1]));
                ticks(1);
                encostar();
                toque("brancoPegada2");
                ticks(40);
                String caso = c[0] + "/" + c[1];
                assertTrue(s().isInteracaoEstabelecida(), caso);
                assertEquals(EstadoInteracaoCombate.PEGADA_2, s().getEstadoInteracao(), caso);
                assertTrue(painel.getLutador1().isAjoelhado() && painel.getLutador2().isAjoelhado(), caso);
            });
        }
    }

    // ---------------- 21, 22, 23: fuga de quadril
    private List<Integer> rastejar(String tecla, int n) {
        List<Integer> quadros = new ArrayList<>();
        tecla(tecla, true);
        for (int i = 0; i < n; i++) {
            ticks(1);
            quadros.add(painel.getAnimador().origem(painel.getLutador1()).orElseThrow().quadro());
        }
        tecla(tecla, false);
        return quadros;
    }

    private void deitarJ1() {
        toque("brancoDeitar");
        toque("brancoDeitar");                              // de joelhos: duplo levanta; entao:
        ticks(20);
        toque("brancoDeitar");
        toque("brancoDeitar");                              // em pe: duplo deita direto
        ticks(20);
        assertTrue(painel.getLutador1().isDeitado());
    }

    @Test
    void fugaDeQuadrilAvancaPelaDistanciaParaERetoma() throws Exception {
        ModoAnimacoesPegada.definir(ModoAnimacoesPegada.EXPERIMENTAL);
        naEdt(() -> {
            painel = dois(null, null);
            ticks(1);
            deitarJ1();
            IrmaoMiau l = painel.getLutador1();
            int x = l.getX();
            List<Integer> q = rastejar("brancoEsquerda", 90);
            AnimadorPartida.OrigemVisual o = painel.getAnimador().origem(l).orElseThrow();
            assertEquals("branco.fuga_quadril.json", o.idAnimacao(), "modo proposta: fuga de quadril");
            int andou = x - l.getX();
            assertTrue(andou > 3, "a fisica continua a mesma (rastejando)");
            assertTrue(q.stream().distinct().count() >= 3, "o corpo se mexe: " + q);
            double dist = painel.getAnimador().distanciaFuga(l);
            assertEquals(andou, dist, 1.0, "quadros sincronizados com a distancia real");
            ticks(5);
            assertEquals(EstadoVisual.DEITADO, painel.getAnimador().origem(l).orElseThrow().estado(), "parou: deitado");
            rastejar("brancoEsquerda", 10);
            assertTrue(painel.getAnimador().distanciaFuga(l) >= dist, "retoma de onde parou");
        });
    }

    @Test
    void sentidoDoDeslocamentoInverteASequencia() throws Exception {
        ModoAnimacoesPegada.definir(ModoAnimacoesPegada.EXPERIMENTAL);
        List<List<Integer>> seqs = new ArrayList<>();
        for (String dir : new String[] {"brancoEsquerda", "brancoDireita"}) {
            naEdt(() -> {
                painel = dois(null, null);
                ticks(1);
                deitarJ1();
                List<Integer> q = rastejar(dir, 120);
                List<Integer> mudancas = new ArrayList<>();
                for (int v : q) {
                    if (mudancas.isEmpty() || mudancas.get(mudancas.size() - 1) != v) {
                        mudancas.add(v);
                    }
                }
                seqs.add(mudancas.subList(0, Math.min(4, mudancas.size())));
            });
        }
        assertEquals(List.of(0, 1, 2, 3), seqs.get(0), "para tras (longe do oponente): sequencia normal");
        assertEquals(List.of(0, 3, 2, 1), seqs.get(1), "para a frente: ao contrario");
    }

    @Test
    void modoAutoralUsaOMovimentoNoChaoAnteriorComMesmaFisicaEFolego() throws Exception {
        int[] andou = new int[2];
        double[] gasto = new double[2];
        ModoAnimacoesPegada[] modos = {ModoAnimacoesPegada.AUTORAL, ModoAnimacoesPegada.EXPERIMENTAL};
        for (int m = 0; m < 2; m++) {
            ModoAnimacoesPegada.definir(modos[m]);
            int k = m;
            naEdt(() -> {
                painel = dois(null, null);
                ticks(1);
                deitarJ1();
                ticks(200);
                IrmaoMiau l = painel.getLutador1();
                int x = l.getX();
                double f = l.getFolegoAtual();
                rastejar("brancoEsquerda", 60);
                andou[k] = x - l.getX();
                gasto[k] = f - l.getFolegoAtual();
                String id = painel.getAnimador().origem(l).orElseThrow().idAnimacao();
                assertEquals(k == 0 ? "branco.movimento_chao.json" : "branco.fuga_quadril.json", id);
            });
        }
        assertEquals(andou[0], andou[1], "a animacao nao muda a velocidade");
        assertEquals(gasto[0], gasto[1], 1e-9, "nem o folego");
    }

    @Test
    void deitadoNaPegadaNaoEscapaRastejando() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(1);
            tecla("brancoDireita", true);
            for (int i = 0; i < 300 && !s().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_3"); i++) {
                ticks(1);
            }
            tecla("brancoDireita", false);
            toque("brancoPegada3");
            ticks(40);
            int x = painel.getLutador2().getX();
            tecla("pretoDireita", true);
            ticks(60);
            tecla("pretoDireita", false);
            assertTrue(s().isInteracaoEstabelecida());
            assertEquals(x, painel.getLutador2().getX());
            assertFalse(painel.getLutador2().isDeitado());
        });
    }
}
