package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.conteudo.ConteudoCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.PosturaAjoelhada;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5L-B (testes 11, 13-19, 26): as quatro pegadas no motor e suas animacoes. */
class QuatroPegadas5LTest {

    private static final String[] PEGADAS = {"pegada_1", "pegada_2", "pegada_3", "pegada_4"};

    @AfterEach
    void padrao() {
        ModoAnimacoesPegada.restaurarPadrao();
    }

    private static ConteudoCombate conteudo() {
        return CarregadorConteudoCombate.carregarPadrao();
    }

    private static IrmaoMiau b(int x) {
        return new IrmaoMiauBranco(x, 150, 0.7);
    }

    private static IrmaoMiau p(int x) {
        return new IrmaoMiauPreto(x, 150, 0.7);
    }

    private static SistemaCombate dupla(IrmaoMiau a, IrmaoMiau r) {
        a.setViradoParaDireita(a.getX() < r.getX());
        r.setViradoParaDireita(!a.isViradoParaDireita());
        return conteudo().criarSistema(a, r);
    }

    // ---------------- 11 e 16: disponiveis com alcance e custo proprios
    @Test
    void quatroPegadasJogaveisComAlcanceECustoProprios() {
        RegistroAcoes r = conteudo().getRegistroAcoes();
        double[] alcance = {125, 115, 135, 110};
        double[] custo = {3.0, 3.5, 2.5, 4.0};
        for (int i = 0; i < 4; i++) {
            AcaoCombate a = r.buscar(PEGADAS[i]);
            assertTrue(a.isJogavel(), PEGADAS[i]);
            assertEquals(alcance[i], a.getAlcanceHorizontal(), PEGADAS[i]);
            assertEquals(custo[i], a.getCustoFolego(), PEGADAS[i]);
            assertEquals(30, a.getDuracaoAnimacaoFrames(), "mesma duracao de entrada");
            assertEquals(EstadoInteracaoCombate.valueOf("PEGADA_" + (i + 1)), a.getEstadoInteracaoAoSucesso());
        }
        // A 120 u (84 px, inicio de joelhos): 1 e 3 alcancam; 2 e 4 pedem para chegar mais perto.
        SistemaCombate s = dupla(b(334), p(418));
        assertTrue(s.podeTentarAcao(s.getLutadorA(), s.getLutadorB(), "pegada_1"));
        assertFalse(s.podeTentarAcao(s.getLutadorA(), s.getLutadorB(), "pegada_2"));
        assertTrue(s.podeTentarAcao(s.getLutadorA(), s.getLutadorB(), "pegada_3"));
        assertFalse(s.podeTentarAcao(s.getLutadorA(), s.getLutadorB(), "pegada_4"));
        SistemaCombate perto = dupla(b(340), p(415));      // 75 px = 107 u
        for (String id : PEGADAS) {
            assertTrue(perto.podeTentarAcao(perto.getLutadorA(), perto.getLutadorB(), id), id);
        }
    }

    // ---------------- 15: identidade mecanica
    @Test
    void firmezaReforcoERompimentoDiferentesPorPegadaEPorEspecialidade() {
        IrmaoMiau bra = b(1);
        IrmaoMiau pre = p(1);
        assertEquals(100.0, ParametrosPegadas.firmezaInicial("pegada_1", bra));
        assertEquals(85.0, ParametrosPegadas.firmezaInicial("pegada_3", bra));
        assertEquals(95.0, ParametrosPegadas.firmezaInicial("pegada_4", bra));
        assertEquals(1.0, ParametrosPegadas.fatorReforco("pegada_1", bra), "PEGADA_1 igual a 5K");
        assertEquals(1.0, ParametrosPegadas.fatorRompimento("pegada_1", pre), "PEGADA_1 igual a 5K para os dois");
        assertTrue(ParametrosPegadas.fatorRompimento("pegada_2", bra) < ParametrosPegadas.fatorRompimento("pegada_2", pre),
                "cruzada: especialidade do Branco (mais dificil de romper)");
        assertTrue(ParametrosPegadas.fatorRompimento("pegada_3", pre) < ParametrosPegadas.fatorRompimento("pegada_3", bra),
                "duas mangas: especialidade do Preto");
        assertTrue(ParametrosPegadas.fatorRompimento("pegada_3", bra) > 1.0, "duas mangas: mais facil de romper");
        assertTrue(ParametrosPegadas.fatorReforco("pegada_3", bra) > ParametrosPegadas.fatorReforco("pegada_2", bra));
        assertEquals("GUARDA", ParametrosPegadas.de("pegada_3").preparaPara());
        for (String id : PEGADAS) {
            ParametrosPegadas.Parametros x = ParametrosPegadas.de(id);
            assertTrue(x.firmezaInicial() >= 80 && x.firmezaInicial() <= 100, id + ": sem vantagem exagerada");
            assertTrue(x.fatorRompimento() >= 0.8 && x.fatorRompimento() <= 1.2, id);
        }
    }

    @Test
    void firmezaInicialEReforcoNoMotor() {
        IrmaoMiau a = b(340);
        IrmaoMiau r = p(415);
        SistemaCombate s = dupla(a, r);
        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(a, r, "pegada_3"));
        for (int i = 0; i < 31; i++) {
            s.atualizar();
        }
        assertTrue(s.isInteracaoEstabelecida());
        assertEquals(EstadoInteracaoCombate.PEGADA_3, s.getEstadoInteracao());
        assertTrue(s.getFirmeza() < 85.0 && s.getFirmeza() > 84.0, "comeca em 85: " + s.getFirmeza());
        double antes = s.getFirmeza();
        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.tentarReforcarPegada(a));
        assertEquals(Math.min(100, antes + SistemaCombate.REFORCO_FIRMEZA * 1.2), s.getFirmeza(), 1e-9,
                "reforco x1,2 nas mangas");
    }

    // ---------------- 13, 14, 17: selecao por molde/papel/lado em todas as combinacoes
    @Test
    void animacoesDasQuatroPegadasPorMoldeEPapel() {
        RegistroAnimacoesCombate reg = conteudo().getRegistroAnimacoes();
        for (int t = 2; t <= 4; t++) {
            String base = "pegada_" + t + "_entrada";
            assertEquals(base, reg.resolver(base, b(1), p(2)).idTrilhaAplicador());
            assertEquals(base, reg.resolver(base, b(600), p(516)).idTrilhaAplicador(), "aplicador a direita: igual");
            assertEquals(base + "_preto", reg.resolver(base, p(1), b(2)).idTrilhaAplicador());
            SelecaoAnimacaoConjunta bb = reg.resolver(base, b(1), b(2));
            assertEquals(SelecaoAnimacaoConjunta.Tipo.COMPOSTA_POR_PAPEL, bb.tipo());
            assertEquals(base + "_preto", bb.idTrilhaReceptor(), "Branco receptor desenhado na do Preto");
            assertEquals(base, reg.resolver(base, p(1), p(2)).idTrilhaReceptor());
            for (ModoAnimacoesPegada m : ModoAnimacoesPegada.values()) {
                assertEquals(base + "_preto", reg.resolver(base, p(1), b(2), m).idTrilhaAplicador(),
                        m + ": sem autoral para comparar, a proposta e a unica");
            }
            assertTrue(reg.getOrigem(base).endsWith("_PROPOSTA.json"));
        }
    }

    // ---------------- 20: F6 continua trocando a PEGADA_1
    @Test
    void f6TrocaSoAPegada1EntreAutoralEProposta() {
        RegistroAnimacoesCombate reg = conteudo().getRegistroAnimacoes();
        assertEquals("pegada_1_entrada",
                reg.resolver("pegada_1_entrada", b(1), p(2), ModoAnimacoesPegada.AUTORAL).idTrilhaAplicador());
        assertEquals("pegada_1_entrada_branco_experimental",
                reg.resolver("pegada_1_entrada", b(1), p(2), ModoAnimacoesPegada.EXPERIMENTAL).idTrilhaAplicador());
        assertEquals(ModoAnimacoesPegada.EXPERIMENTAL, ModoAnimacoesPegada.padraoDoJogo(), "o jogo abre na proposta");
        assertEquals("PROPOSTA (Claude)", ModoAnimacoesPegada.EXPERIMENTAL.rotulo());
    }

    // ---------------- 19: de joelhos, as pernas continuam de joelhos em todas as pegadas
    @Test
    void pegadasDeJoelhosMantemOsDoisDeJoelhos() {
        for (String id : PEGADAS) {
            IrmaoMiau a = b(340);
            IrmaoMiau r = p(415);
            a.ajoelhar();
            r.ajoelhar();
            SistemaCombate s = dupla(a, r);
            assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(a, r, id), id);
            for (int i = 0; i < 31; i++) {
                s.atualizar();
                assertArrayEquals(PosturaAjoelhada.pernas(a), Arrays.copyOfRange(a.copiarPoseCombateExterna(), 22, 30), id);
                assertArrayEquals(PosturaAjoelhada.pernas(r), Arrays.copyOfRange(r.copiarPoseCombateExterna(), 22, 30), id);
            }
        }
    }

    // ---------------- geometria: contato sem atravessar, nos dois lados
    private static boolean[][] mascara(IrmaoMiau l) {
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        if (!l.isViradoParaDireita()) {
            double eixo = l.getX() + 35 * 0.7;
            g.translate(eixo, 0);
            g.scale(-1, 1);
            g.translate(-eixo, 0);
        }
        l.desenhar(g);
        g.dispose();
        boolean[][] m = new boolean[450][800];
        for (int y = 0; y < 450; y++) {
            for (int x = 0; x < 800; x++) {
                m[y][x] = (img.getRGB(x, y) >>> 24) > 128;
            }
        }
        return m;
    }

    @Test
    void pegadasPropostasTocamSemAtravessar() {
        RegistroAnimacoesCombate reg = conteudo().getRegistroAnimacoes();
        for (int t = 2; t <= 4; t++) {
            for (String id : new String[] {"pegada_" + t + "_entrada", "pegada_" + t + "_entrada_preto"}) {
                for (boolean esquerda : new boolean[] {true, false}) {
                    AnimacaoConjuntaQuadros q = reg.exigir(id);
                    boolean brancoAplica = reg.getIdentidadeAplicador(id) == IdentidadeRig.BRANCO;
                    int xa = esquerda ? 300 : 384;
                    int xr = esquerda ? 384 : 300;
                    IrmaoMiau apl = brancoAplica ? b(xa) : p(xa);
                    IrmaoMiau rec = brancoAplica ? p(xr) : b(xr);
                    apl.setViradoParaDireita(esquerda);
                    rec.setViradoParaDireita(!esquerda);
                    apl.aplicarPoseCombateExterna(q.copiarQuadroAplicador(2), 0, 0);
                    rec.aplicarPoseCombateExterna(q.copiarQuadroReceptor(2), 0, 0);
                    boolean[][] ma = mascara(apl);
                    boolean[][] mr = mascara(rec);
                    int contato = 0;
                    int minR = 800;
                    int maxR = -1;
                    int minA = 800;
                    int maxA = -1;
                    for (int y = 0; y < 450; y++) {
                        for (int x = 0; x < 800; x++) {
                            contato += ma[y][x] && mr[y][x] ? 1 : 0;
                            if (mr[y][x]) {
                                minR = Math.min(minR, x);
                                maxR = Math.max(maxR, x);
                            }
                            if (ma[y][x]) {
                                minA = Math.min(minA, x);
                                maxA = Math.max(maxA, x);
                            }
                        }
                    }
                    String caso = id + (esquerda ? " (esq)" : " (dir)");
                    assertTrue(contato > 20, caso + ": maos no adversario (" + contato + ")");
                    int largura = maxR - minR;
                    if (esquerda) {
                        assertTrue(maxA < minR + largura * 0.7, caso + ": nao atravessa");
                    } else {
                        assertTrue(minA > maxR - largura * 0.7, caso + ": nao atravessa");
                    }
                }
            }
        }
    }

    @Test
    void cadaPegadaTemMaosEmLugaresDiferentes() {
        RegistroAnimacoesCombate reg = conteudo().getRegistroAnimacoes();
        int[][] maos = new int[4][];
        String[] ids = {"pegada_1_entrada_branco_experimental", "pegada_2_entrada", "pegada_3_entrada", "pegada_4_entrada"};
        for (int i = 0; i < 4; i++) {
            int[] q = reg.exigir(ids[i]).copiarQuadroAplicador(2);
            maos[i] = new int[] {q[16], q[17], q[20], q[21]};
        }
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                int dist = Math.abs(maos[i][0] - maos[j][0]) + Math.abs(maos[i][1] - maos[j][1])
                        + Math.abs(maos[i][2] - maos[j][2]) + Math.abs(maos[i][3] - maos[j][3]);
                assertTrue(dist > 20, ids[i] + " x " + ids[j] + ": movimentos identificaveis (" + dist + ")");
            }
        }
    }

    // ---------------- 18 e 26: autorais preservados; recursos no classpath (JAR)
    private static String sha(String recurso) throws Exception {
        try (InputStream in = QuatroPegadas5LTest.class.getResourceAsStream(recurso)) {
            assertNotNull(in, recurso);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
        }
    }

    @Test
    void autoraisEExperimentaisDa5KIntactosERecursosNovosEmpacotados() throws Exception {
        String m = "/conteudo/miaustudio/";
        assertEquals("a5d37b2ad6a877bda9821f7bb7f065b25018f358172dacf6a94099a1a7edab72", sha(m + "PEGADA_1.json"));
        assertEquals("dc75f6641a48f2f87b486d3e73ffa66b3bca33aff56115228228920f0210fd43", sha(m + "PEGADA_1_PRETO.json"));
        assertEquals("abf3b49f9d4d25884ee960ddabc8621f2ba37c245519ea39ec3fb2dfd97a321c",
                sha(m + "PEGADA_1_BRANCO_AJOELHADO_EXPERIMENTAL.json"));
        assertEquals("beeb3c093addb83a39fa86f8500b454bdbc5f9148d9f5febadafd460d3c54e5e",
                sha(m + "PEGADA_1_BRANCO_EXPERIMENTAL.json"));
        assertEquals("a62582ea3e63bd930ee7a2f01a7a4d1d03aad769a8d5e6419f123c5a30283534",
                sha(m + "PEGADA_1_PRETO_AJOELHADO_EXPERIMENTAL.json"));
        assertEquals("3dc056cb684b3bc49777085bbf245d0efd724a4743fbba1d7e6db11083636ff3",
                sha(m + "PEGADA_1_PRETO_EXPERIMENTAL.json"));
        for (int t = 2; t <= 4; t++) {
            for (String quem : new String[] {"BRANCO", "PRETO"}) {
                assertNotNull(getClass().getResource(m + "PEGADA_" + t + "_" + quem + "_PROPOSTA.json"));
            }
        }
        assertNotNull(getClass().getResource("/conteudo/animacoes/individuais/branco_fuga_quadril.json"));
        assertNotNull(getClass().getResource("/conteudo/animacoes/individuais/preto_fuga_quadril.json"));
    }
}
