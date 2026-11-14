package com.irmaosmiau.animacao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.Pegada1RegressaoArraysTest;
import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.conteudo.ConteudoInvalidoException;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.personagens.CatalogoPersonagens;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

/** Entrega 5E: catalogo, perfis, controlador, migracao JSON com paridade e root motion. */
class AnimacoesTest {

    private static final PerfisAnimacoes PERFIS = PerfisAnimacoes.padrao();
    private static final CatalogoAnimacoes CAT = PERFIS.getCatalogo();
    private static final ControladorAnimacoes CONTROLADOR = new ControladorAnimacoes(PERFIS);

    private static Object campoLegado(Class<?> c, String nome) throws Exception {
        Field f = c.getDeclaredField(nome);
        f.setAccessible(true);
        return f.get(null);
    }

    private static AnimacaoIndividual chaoJson(Class<?> classe) {
        String id = (classe == IrmaoMiauBranco.class ? "branco" : "preto") + ".movimento_chao.json";
        return CAT.exigir(id).getDados().orElseThrow();
    }

    // =====================================================
    // MIGRACAO JSON: FIDELIDADE E COMPARACAO VISUAL
    // =====================================================
    @Test
    void jsonDoChaoTemExatamenteOsDadosDoArrayLegado() throws Exception {
        for (Class<?> c : new Class<?>[]{IrmaoMiauBranco.class, IrmaoMiauPreto.class}) {
            AnimacaoIndividual a = chaoJson(c);
            assertTrue(a.mesmosQuadros((int[][]) campoLegado(c, "MOVIMENTO_CHAO_MIAUSTUDIO")), c.getSimpleName());
            assertArrayEquals((int[]) campoLegado(c, "SEQUENCIA_MOVIMENTO_CHAO_MIAUSTUDIO"), a.getSequencia());
            assertEquals((long) campoLegado(c, "TEMPO_QUADRO_MOVIMENTO_CHAO_MS"), a.getDuracaoQuadroMs());
            assertEquals(70.0, a.getDeslocamentoVisualY());
            assertTrue(a.isLoop());
        }
    }

    @Test
    void quadroNoTempoIgualAContaDoLegado() throws Exception {
        for (Class<?> c : new Class<?>[]{IrmaoMiauBranco.class, IrmaoMiauPreto.class}) {
            AnimacaoIndividual a = chaoJson(c);
            int[] seq = (int[]) campoLegado(c, "SEQUENCIA_MOVIMENTO_CHAO_MIAUSTUDIO");
            long ms = (long) campoLegado(c, "TEMPO_QUADRO_MOVIMENTO_CHAO_MS");
            for (long t = 0; t < 5000; t += 7) {
                assertEquals(seq[(int) (t / ms) % seq.length], a.indiceQuadroNoTempo(t), "t=" + t);
            }
            assertEquals(a.indiceQuadroNoTempo(1000), a.indiceQuadroNoTick(60, 1000.0 / 60), "60 ticks = 1 s");
        }
    }

    private static BufferedImage render(IrmaoMiau l, Consumer<Graphics2D> desenho) {
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        if (!l.isViradoParaDireita()) {                  // mesmo espelhamento do GamePanel
            double eixo = l.getX() + 35 * 0.7;
            g.translate(eixo, 0);
            g.scale(-1, 1);
            g.translate(-eixo, 0);
        }
        desenho.accept(g);
        g.dispose();
        return img;
    }

    private static int comparar(BufferedImage a, BufferedImage b) {
        int pintados = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                assertEquals(a.getRGB(x, y), b.getRGB(x, y), "pixel (" + x + "," + y + ")");
                if ((a.getRGB(x, y) >>> 24) != 0) {
                    pintados++;
                }
            }
        }
        return pintados;
    }

    @Test
    void chaoPeloJsonEPixelAPixelIgualAoLegadoNosDoisLadosEDoisPersonagens() throws Exception {
        for (IrmaoMiau l : new IrmaoMiau[]{new IrmaoMiauBranco(300, 150, 0.7), new IrmaoMiauPreto(420, 150, 0.7)}) {
            l.alternarDeitado();
            l.atualizar(false, false);
            Method legado = l.getClass().getDeclaredMethod("desenharPoseChaoMiauStudio", Graphics2D.class, int.class);
            legado.setAccessible(true);
            ReprodutorPoseJson json = new ReprodutorPoseJson(chaoJson(l.getClass()));
            for (boolean direita : new boolean[]{true, false}) {
                l.setViradoParaDireita(direita);
                for (int q = 0; q < 5; q++) {
                    int quadro = q;
                    BufferedImage antigo = render(l, g -> {
                        try {
                            legado.invoke(l, g, quadro);
                        } catch (ReflectiveOperationException ex) {
                            throw new AssertionError(ex);
                        }
                    });
                    assertTrue(json.aplicarQuadro(l, quadro));
                    BufferedImage novo = render(l, l::desenhar);
                    json.liberar();
                    assertFalse(l.isPoseCombateExternaAtiva(), "liberado");
                    assertTrue(comparar(antigo, novo) > 1500, "havia desenho");
                }
            }
        }
    }

    // =====================================================
    // CATALOGO, PERFIS E FALLBACK
    // =====================================================
    @Test
    void perfisPadraoUsamOLegadoEmTudoECaminhadaCorridaArrastoTemEntrada() {
        for (String id : new String[]{CatalogoPersonagens.ID_BRANCO, CatalogoPersonagens.ID_PRETO}) {
            for (EstadoVisual e : EstadoVisual.values()) {
                AnimacaoResolvida r = CONTROLADOR.resolver(id, e).orElseThrow(() -> new AssertionError(id + e));
                assertEquals(e, r.definicao().estado());
                assertTrue(r.usaCodigoLegado(), "o jogo continua desenhando pelo codigo atual: " + e);
            }
        }
        assertTrue(CAT.exigir("branco.caminhada.legada").referencia().contains("getOscilacaoPasso"));
        assertTrue(CAT.exigir("preto.corrida.legada").referencia().contains("amplitude 15"));
        assertEquals(FonteAnimacao.LEGADA_CODIGO, CAT.exigir("branco.caminhada.legada").fonte());
        assertEquals(FonteAnimacao.MIAUSTUDIO_CONJUNTA, CAT.exigir("arrasto_conjunto.legado").fonte());
        assertEquals(FonteAnimacao.JSON_INDIVIDUAL, CAT.exigir("preto.movimento_chao.json").fonte());
    }

    @Test
    void trocarUmaAnimacaoNoPerfilEmUmaLinha() {
        PerfilAnimacoes branco = PERFIS.buscar(CatalogoPersonagens.ID_BRANCO).orElseThrow();
        PerfilAnimacoes comJson = branco.comAnimacao(EstadoVisual.MOVIMENTO_CHAO, "branco.movimento_chao.json", CAT);
        assertEquals(FonteAnimacao.JSON_INDIVIDUAL,
                comJson.resolver(EstadoVisual.MOVIMENTO_CHAO, CAT).orElseThrow().definicao().fonte());
        assertEquals("branco.caminhada.legada",
                comJson.resolver(EstadoVisual.CAMINHADA, CAT).orElseThrow().definicao().id(), "o resto nao muda");
        assertThrows(IllegalArgumentException.class,
                () -> branco.comAnimacao(EstadoVisual.CAMINHADA, "branco.movimento_chao.json", CAT),
                "animacao de outro estado");
        assertThrows(IllegalArgumentException.class,
                () -> branco.comAnimacao(EstadoVisual.CAMINHADA, "nao.existe", CAT));
    }

    @Test
    void personagemHipoteticoCompartilhaESubstituiSemVirarLutador() {
        CatalogoAnimacoes c = CatalogoAnimacoes.padrao();
        AnimacaoIndividual caminhada = CarregadorAnimacaoJson.doRecurso("/animacoes/teste_caminhada_boxeador.json");
        c.registrar(DefinicaoAnimacao.json("teste.boxeador.caminhada", "teste", caminhada));
        PerfisAnimacoes perfis = PerfisAnimacoes.padrao();
        PerfilAnimacoes base = perfis.buscar(CatalogoPersonagens.ID_BRANCO).orElseThrow();

        PerfilAnimacoes boxeador = PerfilAnimacoes.baseadoEm("teste_gato_boxeador", base);
        AnimacaoResolvida corridaInicial = boxeador.resolver(EstadoVisual.CORRIDA, c).orElseThrow();
        assertTrue(corridaInicial.herdada());
        assertEquals(CatalogoPersonagens.ID_BRANCO, corridaInicial.perfilDeOrigem(), "compartilhada do base");

        boxeador = boxeador.comAnimacao(EstadoVisual.CAMINHADA, "teste.boxeador.caminhada", c);
        AnimacaoResolvida andar = boxeador.resolver(EstadoVisual.CAMINHADA, c).orElseThrow();
        assertFalse(andar.herdada());
        assertEquals("teste.boxeador.caminhada", andar.definicao().id(), "so a caminhada foi trocada");
        assertEquals("branco.corrida.legada", boxeador.resolver(EstadoVisual.CORRIDA, c).orElseThrow().definicao().id());

        assertFalse(CatalogoPersonagens.padrao().contem("teste_gato_boxeador"), "nao e um lutador jogavel");
        assertFalse(perfis.buscar("teste_gato_boxeador").isPresent(), "nem um perfil registrado no jogo");
        PerfilAnimacoes outroRig = PerfilAnimacoes.novo("teste_humano", "humano_v0");
        assertThrows(IllegalArgumentException.class,
                () -> outroRig.comAnimacao(EstadoVisual.CAMINHADA, "teste.boxeador.caminhada", c), "rig incompativel");
        assertTrue(outroRig.resolver(EstadoVisual.CAMINHADA, c).isEmpty(), "sem animacao: fallback legado");
    }

    @Test
    void perfilPersonalizadoUsaAsAnimacoesDoPersonagemBase() {
        IrmaoMiau perfil = new IrmaoMiauBranco(100, 150, 0.7, new AtributosLutador(70, 70, 70, 60, 80, 60, 60, 60),
                "Branco Forte");
        assertEquals(CatalogoPersonagens.ID_BRANCO, PERFIS.doLutador(perfil).orElseThrow().getIdPersonagem());
        assertEquals("branco.parado.legada", CONTROLADOR.resolver(perfil, null, false).orElseThrow().definicao().id());
    }

    // =====================================================
    // CONTROLADOR: ESTADO -> ANIMACAO, SEM MEXER NO MOVIMENTO
    // =====================================================
    @Test
    void estadoVisualSegueOCorpoEOCombate() {
        IrmaoMiau b = new IrmaoMiauBranco(440, 150, 0.7);
        IrmaoMiau p = new IrmaoMiauPreto(500, 150, 0.7);
        b.setViradoParaDireita(true);
        p.setViradoParaDireita(false);
        assertEquals(EstadoVisual.PARADO, ControladorAnimacoes.estadoVisual(b, null, false));
        b.setCorrendo(false);
        b.atualizar(true, false);
        assertEquals(EstadoVisual.CAMINHADA, ControladorAnimacoes.estadoVisual(b, null, false));
        b.setCorrendo(true);
        b.atualizar(true, false);
        assertEquals(EstadoVisual.CORRIDA, ControladorAnimacoes.estadoVisual(b, null, false));
        b.setCorrendo(false);
        b.atualizar(false, false);

        SistemaCombate s = CarregadorConteudoCombate.carregarParaJogo().criarSistema(b, p);
        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(b, p, "pegada_1"));
        tick(b, p, s, 5);
        assertEquals(EstadoVisual.PEGADA_ENTRADA, ControladorAnimacoes.estadoVisual(b, s, false));
        assertEquals(EstadoVisual.PEGADA_ENTRADA, ControladorAnimacoes.estadoVisual(p, s, false));
        tick(b, p, s, 30);
        assertEquals(EstadoVisual.PEGADA_ESTABELECIDA, ControladorAnimacoes.estadoVisual(b, s, false));
        assertEquals("pegada_1.estabelecida", CONTROLADOR.resolver(p, s, false).orElseThrow().definicao().id());

        // Arrasto 5B: fallback legado; a pose final nao muda e a dupla anda junta.
        int xb = b.getX();
        s.deslocarPegadaControlada(b, 1, 15, 715);
        assertEquals(EstadoVisual.ARRASTO_CONJUNTO, ControladorAnimacoes.estadoVisual(b, s, true));
        assertEquals("arrasto_conjunto.legado", CONTROLADOR.resolver(b, s, true).orElseThrow().definicao().id());
        assertEquals(xb + 1, b.getX());
        assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[2], b.copiarPoseCombateExterna());
        assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[2], p.copiarPoseCombateExterna());
    }

    private static void tick(IrmaoMiau b, IrmaoMiau p, SistemaCombate s, int n) {
        for (int i = 0; i < n; i++) {
            b.atualizar(false, false);
            p.atualizar(false, false);
            s.atualizar();
        }
    }

    @Test
    void animacaoNaoMoveOLutadorENemPassaPorCimaDoCombate() {
        IrmaoMiau b = new IrmaoMiauBranco(440, 150, 0.7);
        IrmaoMiau p = new IrmaoMiauPreto(500, 150, 0.7);
        b.setViradoParaDireita(true);
        p.setViradoParaDireita(false);
        ReprodutorPoseJson json = new ReprodutorPoseJson(chaoJson(IrmaoMiauBranco.class));
        int x = b.getX();
        int y = b.getY();
        for (int t = 0; t < 3000; t += 50) {
            json.aplicarNoTempo(b, t);
            CONTROLADOR.resolver(b, null, false);
        }
        json.liberar();
        assertEquals(x, b.getX(), "animacao nao desloca");
        assertEquals(y, b.getY());

        SistemaCombate s = CarregadorConteudoCombate.carregarParaJogo().criarSistema(b, p);
        s.tentarAcao(b, p, "pegada_1");
        tick(b, p, s, 35);
        int[] poseCombate = b.copiarPoseCombateExterna();
        assertFalse(json.aplicarQuadro(b, 2), "pose do combate tem prioridade");
        json.liberar();
        assertArrayEquals(poseCombate, b.copiarPoseCombateExterna(), "PEGADA_1 intacta");
    }

    @Test
    void rootMotionPorDistanciaNaoDeslizaParado() {
        SincronizadorPassos passos = new SincronizadorPassos(40, 4);
        assertEquals(0, passos.quadroPara(0));
        assertEquals(0, passos.quadroPara(9.9));
        assertEquals(1, passos.quadroPara(10));
        assertEquals(3, passos.quadroPara(39));
        assertEquals(0, passos.quadroPara(40), "ciclo fecha");
        assertEquals(passos.quadroPara(25), passos.quadroPara(-25), "para tras tambem anda");
        // Arrasto 5B a 1 px/tick: 10 ticks = 1 quadro; parado o quadro nao muda com o tempo.
        double distancia = 0;
        int q = passos.quadroPara(distancia);
        for (int tick = 0; tick < 100; tick++) {
            assertEquals(q, passos.quadroPara(distancia), "parado: pes nao andam");
        }
        for (int tick = 0; tick < 10; tick++) {
            distancia += SistemaCombate.VELOCIDADE_DESLOCAMENTO_CONJUNTO;
        }
        assertEquals(1, passos.quadroPara(distancia));
        assertThrows(IllegalArgumentException.class, () -> new SincronizadorPassos(0, 4));
    }

    // =====================================================
    // VALIDACAO DO FORMATO JSON
    // =====================================================
    private static final String BASE = "{\"tipo\":\"ANIMACAO_INDIVIDUAL_IRMAOS_MIAU\",\"versaoEsquema\":1,"
            + "\"id\":\"x\",\"estado\":\"CAMINHADA\",\"rig\":\"miau_v1\",\"duracaoQuadroMs\":100,";
    private static final String POSE = "[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]";

    @Test
    void arquivosInvalidosSaoRecusadosComMotivo() {
        String ok = BASE + "\"quadros\":[{\"pose\":" + POSE + "}]}";
        assertEquals(1, CarregadorAnimacaoJson.ler(ok, "ok").getQuantidadeQuadros());
        assertArrayEquals(new int[]{0}, CarregadorAnimacaoJson.ler(ok, "ok").getSequencia(), "sequencia padrao");
        String[] ruins = {
            BASE + "\"quadros\":[{\"pose\":[1,2,3]}]}",                                   // pose curta
            BASE + "\"sequencia\":[0,5],\"quadros\":[{\"pose\":" + POSE + "}]}",           // indice inexistente
            BASE.replace("miau_v1", "humano_v0") + "\"quadros\":[{\"pose\":" + POSE + "}]}",
            BASE.replace("ANIMACAO_INDIVIDUAL", "OUTRO") + "\"quadros\":[{\"pose\":" + POSE + "}]}",
            BASE.replace("CAMINHADA", "PEGADA_ENTRADA") + "\"quadros\":[{\"pose\":" + POSE + "}]}",
            BASE.replace("\"versaoEsquema\":1", "\"versaoEsquema\":2") + "\"quadros\":[{\"pose\":" + POSE + "}]}",
            BASE + "\"extra\":1,\"quadros\":[{\"pose\":" + POSE + "}]}",
            BASE + "\"quadros\":[]}",
            "{ nao e json",
        };
        for (String r : ruins) {
            assertThrows(ConteudoInvalidoException.class, () -> CarregadorAnimacaoJson.ler(r, "teste"), r);
        }
        assertThrows(ConteudoInvalidoException.class,
                () -> CarregadorAnimacaoJson.doRecurso("/conteudo/animacoes/individuais/nao_existe.json"));
    }

    @Test
    void naoLoopParaNoUltimoQuadro() {
        AnimacaoIndividual a = CarregadorAnimacaoJson.ler(BASE + "\"loop\":false,\"sequencia\":[0,0],"
                + "\"quadros\":[{\"pose\":" + POSE + "}]}", "t");
        assertEquals(0, a.indiceQuadroNoTempo(99999));
    }
}
