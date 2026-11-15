package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.AnimacaoIndividual;
import com.irmaosmiau.animacao.AnimadorPartida;
import com.irmaosmiau.animacao.AnimadorPartida.OrigemVisual;
import com.irmaosmiau.animacao.CatalogoAnimacoes;
import com.irmaosmiau.animacao.EstadoVisual;
import com.irmaosmiau.animacao.FonteAnimacao;
import com.irmaosmiau.animacao.PerfisAnimacoes;
import com.irmaosmiau.combate.Pegada1RegressaoArraysTest;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Entrega 5G: a partida REAL desenha o movimento no chao a partir do JSON.
 * Se a ligacao runtime -> JSON for removida (GamePanel sem o animador, perfil
 * sem a JSON...), estes testes falham.
 */
class AnimacoesRuntimeGamePanelTest {

    private GamePanel painel;

    @AfterEach
    void restaurar() {
        PerfisAnimacoes.instalarGlobal(null);
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void criar(boolean treino) {
        painel = treino
                ? new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { })
                : new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
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

    private OrigemVisual origem(IrmaoMiau l) {
        return painel.getAnimador().origem(l).orElseThrow();
    }

    private static int[][] chaoLegado(Class<?> c) throws Exception {
        Field f = c.getDeclaredField("MOVIMENTO_CHAO_MIAUSTUDIO");
        f.setAccessible(true);
        return (int[][]) f.get(null);
    }

    private static AnimacaoIndividual json(String id) {
        return PerfisAnimacoes.global().getCatalogo().exigir(id).getDados().orElseThrow();
    }

    // =====================================================
    // CONEXAO REAL RUNTIME -> JSON
    // =====================================================
    @Test
    void rastejarNaPartidaUsaOJsonDoBrancoEDoPreto() throws Exception {
        naEdt(() -> {
            criar(false);
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            toque("brancoDeitar");
            toque("pretoDeitar");
            assertEquals(EstadoVisual.DEITADO, origem(b).estado());
            assertFalse(origem(b).isJson(), "deitado parado: desenho legado (quadro 1 igual ao JSON)");
            tecla("brancoEsquerda", true);
            tecla("pretoDireita", true);
            ticks(1);
            OrigemVisual ob = origem(b);
            OrigemVisual op = origem(p);
            assertTrue(ob.isJson() && op.isJson(), ob + " / " + op);
            assertEquals("branco.movimento_chao.json", ob.idAnimacao());
            assertEquals("preto.movimento_chao.json", op.idAnimacao());
            assertEquals(0, ob.quadro(), "comeca pelo 1o quadro: sem salto ao sair do repouso");
            try {
                assertArrayEquals(chaoLegado(IrmaoMiauBranco.class)[0], b.copiarPoseCombateExterna(),
                        "a pose desenhada vem do JSON (= quadro 1 do array legado)");
                for (int t = 0; t < 40; t++) {
                    ticks(1);
                    assertArrayEquals(json("branco.movimento_chao.json").quadro(origem(b).quadro()),
                            b.copiarPoseCombateExterna());
                    assertArrayEquals(chaoLegado(IrmaoMiauPreto.class)[origem(p).quadro()], p.copiarPoseCombateExterna());
                }
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
            tecla("brancoEsquerda", false);
            tecla("pretoDireita", false);
            ticks(1);
            assertFalse(origem(b).isJson(), "parou: volta ao legado");
            assertFalse(b.isPoseCombateExternaAtiva(), "nenhuma pose JSON sobra");
        });
    }

    @Test
    void quadrosAvancamPeloTempoDaAnimacaoComPingPong() throws Exception {
        naEdt(() -> {
            criar(false);
            IrmaoMiau b = painel.getLutador1();
            toque("brancoDeitar");
            tecla("brancoDireita", true);
            List<Integer> vistos = new ArrayList<>();
            for (int t = 0; t < 76; t++) {                   // 75 ticks x 16 ms = 1200 ms = 8 quadros
                ticks(1);
                if (vistos.isEmpty() || vistos.get(vistos.size() - 1) != origem(b).quadro()) {
                    vistos.add(origem(b).quadro());
                }
            }
            tecla("brancoDireita", false);
            assertEquals(List.of(0, 1, 2, 3, 4, 3, 2, 1, 0), vistos, "150 ms por quadro, ping-pong do legado");
        });
    }

    @Test
    void desenhoDaPartidaPixelAPixelIgualAoLegado() throws Exception {
        naEdt(() -> {
            criar(false);
            IrmaoMiau b = painel.getLutador1();
            toque("brancoDeitar");
            tecla("brancoDireita", true);
            try {
                Method legado = IrmaoMiauBranco.class.getDeclaredMethod("desenharPoseChaoMiauStudio",
                        Graphics2D.class, int.class);
                legado.setAccessible(true);
                for (int t = 0; t < 30; t += 3) {
                    ticks(3);
                    int quadro = origem(b).quadro();
                    BufferedImage novo = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g1 = novo.createGraphics();
                    b.desenhar(g1);                          // o que a partida desenha agora
                    g1.dispose();
                    BufferedImage antigo = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = antigo.createGraphics();
                    legado.invoke(b, g2, quadro);            // o desenho do codigo antigo
                    g2.dispose();
                    for (int y = 0; y < 450; y += 2) {
                        for (int x = 0; x < 800; x += 2) {
                            assertEquals(antigo.getRGB(x, y), novo.getRGB(x, y), "q" + quadro + " (" + x + "," + y + ")");
                        }
                    }
                }
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex);
            }
            tecla("brancoDireita", false);
        });
    }

    // =====================================================
    // MOVIMENTO FISICO INTACTO
    // =====================================================
    private List<int[]> trajetoRastejando(PerfisAnimacoes perfis) {
        PerfisAnimacoes.instalarGlobal(perfis);
        criar(false);
        IrmaoMiau b = painel.getLutador1();
        toque("brancoDeitar");
        tecla("brancoDireita", true);
        tecla("brancoBaixo", true);
        List<int[]> trajeto = new ArrayList<>();
        for (int t = 0; t < 60; t++) {
            ticks(1);
            trajeto.add(new int[]{b.getX(), b.getY(), b.getLinhaAtual()});
        }
        tecla("brancoDireita", false);
        tecla("brancoBaixo", false);
        return trajeto;
    }

    @Test
    void semDeslocamentoDuploNemMudancaDeProfundidade() throws Exception {
        naEdt(() -> {
            List<int[]> comJson = trajetoRastejando(PerfisAnimacoes.paraJogo(CatalogoAnimacoes::lerRecurso, m -> { }));
            assertTrue(painel.getAnimador().origem(painel.getLutador1()).orElseThrow().isJson());
            List<int[]> soLegado = trajetoRastejando(PerfisAnimacoes.padrao());
            assertFalse(painel.getAnimador().origem(painel.getLutador1()).orElseThrow().isJson());
            for (int t = 0; t < comJson.size(); t++) {
                assertArrayEquals(soLegado.get(t), comJson.get(t), "tick " + t + ": x, y e linha iguais");
            }
            assertTrue(comJson.get(59)[0] > comJson.get(0)[0], "andou");
        });
    }

    // =====================================================
    // TRANSICOES
    // =====================================================
    @Test
    void pausaDirecaoParadaLevantarETrocaDeEstado() throws Exception {
        naEdt(() -> {
            criar(false);
            IrmaoMiau b = painel.getLutador1();
            toque("brancoDeitar");
            tecla("brancoDireita", true);
            ticks(20);
            int quadro = origem(b).quadro();
            int[] pose = b.copiarPoseCombateExterna();
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            ticks(100);
            assertEquals(quadro, origem(b).quadro(), "pausado: o quadro nao anda");
            assertArrayEquals(pose, b.copiarPoseCombateExterna());
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            ticks(1);
            assertTrue(origem(b).isJson(), "retoma de onde parou");

            tecla("brancoDireita", false);
            tecla("brancoEsquerda", true);                   // mudanca de direcao: mesma animacao
            ticks(1);
            assertTrue(origem(b).isJson());
            assertEquals("branco.movimento_chao.json", origem(b).idAnimacao());

            tecla("brancoEsquerda", false);
            ticks(1);
            assertEquals(EstadoVisual.DEITADO, origem(b).estado());
            assertFalse(b.isPoseCombateExternaAtiva());
            toque("brancoDeitar");                           // levantar
            assertEquals(EstadoVisual.PARADO, origem(b).estado());
            assertFalse(b.isPoseCombateExternaAtiva(), "em pe: nada do JSON interfere");

            toque("brancoDeitar");
            tecla("brancoDireita", true);
            ticks(1);
            assertEquals(0, origem(b).quadro(), "novo movimento recomeca do 1o quadro");
            tecla("brancoDireita", false);
            painel.simularPerdaDeFocoParaTestes();
            ticks(1);
            assertFalse(origem(b).isJson(), "perda de foco solta as teclas: legado");
        });
    }

    @Test
    void pegada1EArrastoContinuamComAsPosesDoCombate() throws Exception {
        naEdt(() -> {
            criar(false);
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            tecla("brancoDireita", true);
            for (int i = 0; i < 200 && !painel.getSistemaCombate().podeTentarAcao(b, p, "pegada_1"); i++) {
                ticks(1);
            }
            tecla("brancoDireita", false);
            toque("brancoPegada1");
            assertEquals(FonteAnimacao.MIAUSTUDIO_CONJUNTA, origem(b).fonte());
            ticks(31);
            assertEquals(EstadoVisual.PEGADA_ESTABELECIDA, origem(b).estado());
            assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[2], b.copiarPoseCombateExterna());
            assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[2], p.copiarPoseCombateExterna());
            tecla("brancoControle", true);
            tecla("brancoDireita", true);
            ticks(10);
            assertEquals(EstadoVisual.ARRASTO_CONJUNTO, origem(b).estado());
            assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[2], b.copiarPoseCombateExterna());
            tecla("brancoDireita", false);
            tecla("brancoControle", false);
            assertTrue(painel.getAnimador().comJsonAtivo().isEmpty());
        });
    }

    @Test
    void reiniciarTreinoContinuaUsandoOJson() throws Exception {
        naEdt(() -> {
            criar(true);
            IrmaoMiau antigo = painel.getLutador1();
            toque("brancoDeitar");
            tecla("brancoDireita", true);
            ticks(5);
            assertTrue(origem(antigo).isJson());
            tecla("brancoDireita", false);
            painel.getActionMap().get("reiniciarTreino").actionPerformed(null);
            ticks(1);
            assertTrue(painel.getAnimador().origem(antigo).isEmpty(), "lutador antigo esquecido");
            IrmaoMiau novo = painel.getLutador1();
            toque("brancoDeitar");
            tecla("brancoDireita", true);
            ticks(3);
            assertTrue(origem(novo).isJson());
            tecla("brancoDireita", false);
        });
    }

    // =====================================================
    // FALLBACK
    // =====================================================
    @Test
    void jsonAusenteOuInvalidoVoltaAoLegadoComUmAvisoSo() throws Exception {
        AtomicInteger avisos = new AtomicInteger();
        List<String> textos = new ArrayList<>();
        PerfisAnimacoes semBranco = PerfisAnimacoes.paraJogo(r -> r.contains("branco") ? null : CatalogoAnimacoes.lerRecurso(r),
                m -> {
                    avisos.incrementAndGet();
                    textos.add(m);
                });
        // 5J (pedido): o Branco tem 3 JSON (chao + ajoelhado + ajoelhado em movimento); antes 1 aviso.
        assertEquals(3, avisos.get());
        assertTrue(textos.stream().anyMatch(t -> t.contains("branco_movimento_chao.json ausente")), textos.toString());
        PerfisAnimacoes invalido = PerfisAnimacoes.paraJogo(r -> r.contains("preto") ? "{ lixo" : CatalogoAnimacoes.lerRecurso(r),
                m -> {
                    avisos.incrementAndGet();
                    textos.add(m);
                });
        // 5J (pedido): + 3 avisos do Preto invalido (antes 2 no total).
        assertEquals(6, avisos.get());
        assertTrue(textos.stream().anyMatch(t -> t.contains("preto_movimento_chao.json invalido")), textos.toString());

        naEdt(() -> {
            PerfisAnimacoes.instalarGlobal(semBranco);
            criar(false);
            toque("brancoDeitar");
            toque("pretoDeitar");
            tecla("brancoDireita", true);
            tecla("pretoEsquerda", true);
            ticks(30);
            assertFalse(origem(painel.getLutador1()).isJson(), "Branco sem JSON: legado");
            assertFalse(painel.getLutador1().isPoseCombateExternaAtiva());
            assertTrue(origem(painel.getLutador2()).isJson(), "Preto continua no JSON");
            criar(false);                                    // outra partida: nada novo no log
            ticks(100);
            PerfisAnimacoes.instalarGlobal(invalido);
            criar(false);
            toque("pretoDeitar");
            tecla("pretoEsquerda", true);
            ticks(30);
            assertFalse(origem(painel.getLutador2()).isJson(), "Preto com JSON invalido: legado, sem quebrar");
        });
        assertEquals(6, avisos.get(), "nenhum aviso por quadro nem por partida"); // 5J (pedido): antes 2
    }

    // =====================================================
    // PERSONAGEM FUTURO (RIG COMPATIVEL), SEM SER LUTADOR DO JOGO
    // =====================================================
    /** Somente teste: outro "personagem" com o mesmo rig. */
    static final class GatoFuturoDeTeste extends IrmaoMiauBranco {
        GatoFuturoDeTeste(int x, int y, double escala) {
            super(x, y, escala);
        }
    }

    @Test
    void rigFuturoUsaAPropriaAnimacaoSemMudarORuntime() {
        PerfisAnimacoes perfis = PerfisAnimacoes.paraJogo(CatalogoAnimacoes::lerRecurso, m -> { });
        AnimacaoIndividual propria = com.irmaosmiau.animacao.CarregadorAnimacaoJson
                .doRecurso("/animacoes/teste_rig_futuro_chao.json");
        perfis.getCatalogo().registrar(com.irmaosmiau.animacao.DefinicaoAnimacao.json(
                "teste.gato_futuro.movimento_chao", "teste", propria));
        perfis.registrar(com.irmaosmiau.animacao.PerfilAnimacoes
                .baseadoEm("teste_gato_futuro", perfis.buscar("miau_branco").orElseThrow())
                .comAnimacao(EstadoVisual.MOVIMENTO_CHAO, "teste.gato_futuro.movimento_chao", perfis.getCatalogo()));
        perfis.registrarClasse(GatoFuturoDeTeste.class, "teste_gato_futuro");

        AnimadorPartida animador = new AnimadorPartida(
                new com.irmaosmiau.animacao.ControladorAnimacoes(perfis), 16);
        IrmaoMiau gato = new GatoFuturoDeTeste(300, 150, 0.7);
        IrmaoMiau branco = new IrmaoMiauBranco(500, 150, 0.7);
        gato.alternarDeitado();
        branco.alternarDeitado();
        gato.atualizar(true, false);
        branco.atualizar(true, false);
        animador.atualizar(List.of(gato, branco), null, false, 0);
        assertEquals("teste.gato_futuro.movimento_chao", animador.origem(gato).orElseThrow().idAnimacao());
        assertEquals("branco.movimento_chao.json", animador.origem(branco).orElseThrow().idAnimacao());
        animador.atualizar(List.of(gato, branco), null, false, 7);   // 7 x 16 ms = 112 ms -> quadro 2
        assertArrayEquals(propria.quadro(1), gato.copiarPoseCombateExterna());
        assertNotEquals(propria.quadro(1)[11], branco.copiarPoseCombateExterna()[11]);
        assertEquals("miau_branco", perfis.buscar("teste_gato_futuro").orElseThrow().getBase().orElseThrow()
                .getIdPersonagem(), "o resto vem do perfil base");
    }
}
