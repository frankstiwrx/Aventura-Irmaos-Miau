package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.TransicoesPostura;
import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.CustosFolego;
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.PosturaAjoelhada;
import com.irmaosmiau.entities.TransicaoPostura;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5K (testes 22-25 e 29-37): esforco no combate, variantes experimentais e transicoes. */
class AnimacoesEEsforco5KTest {

    private static final String BASE = "pegada_1_entrada";
    private static final String B_EXP = "pegada_1_entrada_branco_experimental";
    private static final String P_EXP = "pegada_1_entrada_preto_experimental";
    private static final String B_JOE = "pegada_1_entrada_branco_ajoelhado_experimental";
    private static final String P_JOE = "pegada_1_entrada_preto_ajoelhado_experimental";

    @AfterEach
    void padrao() {
        ModoAnimacoesPegada.definir(ModoAnimacoesPegada.AUTORAL);
    }

    private static RegistroAnimacoesCombate reg() {
        return CarregadorConteudoCombate.carregarPadrao().getRegistroAnimacoes();
    }

    private static IrmaoMiau b(int x) {
        return new IrmaoMiauBranco(x, 150, 0.7);
    }

    private static IrmaoMiau p(int x) {
        return new IrmaoMiauPreto(x, 150, 0.7);
    }

    private static IrmaoMiau joelhos(IrmaoMiau l) {
        l.ajoelhar();
        return l;
    }

    // ---------------- 29. JSONs autorais intactos
    private static String sha(String recurso) throws Exception {
        try (InputStream in = AnimacoesEEsforco5KTest.class.getResourceAsStream(recurso)) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
        }
    }

    @Test
    void jsonsAutoraisContinuamByteAByte() throws Exception {
        assertEquals("a5d37b2ad6a877bda9821f7bb7f065b25018f358172dacf6a94099a1a7edab72",
                sha("/conteudo/miaustudio/PEGADA_1.json"));
        assertEquals("dc75f6641a48f2f87b486d3e73ffa66b3bca33aff56115228228920f0210fd43",
                sha("/conteudo/miaustudio/PEGADA_1_PRETO.json"));
    }

    // ---------------- 30 e 37. leitura das experimentais (classpath = o que vai no JAR)
    @Test
    void experimentaisCarregadasEMarcadas() {
        RegistroAnimacoesCombate r = reg();
        for (String[] e : new String[][] {{B_EXP, "EM_PE", "BRANCO"}, {P_EXP, "EM_PE", "PRETO"},
            {B_JOE, "AJOELHADO", "BRANCO"}, {P_JOE, "AJOELHADO", "PRETO"}}) {
            AnimacaoConjuntaQuadros q = r.exigir(e[0]);
            assertTrue(r.isExperimental(e[0]), e[0]);
            assertEquals(e[1], r.getPostura(e[0]));
            assertEquals(IdentidadeRig.valueOf(e[2]), r.getIdentidadeAplicador(e[0]));
            assertEquals(4, q.quantidadeQuadros(), "4 quadros: preparo, alcance, contato, pegada");
            assertEquals(30, q.getDuracaoTotalFrames(), "mesma duracao da tecnica");
            for (int i = 0; i < 4; i++) {
                assertEquals(0.0, q.deslocamentoAplicadorX(i), "sem teletransporte");
                assertEquals(0.0, q.deslocamentoReceptorX(i));
            }
            String arquivo = r.getOrigem(e[0]);
            assertTrue(arquivo.endsWith("_EXPERIMENTAL.json"), arquivo);
            assertNotNull(getClass().getResource(arquivo), "empacotado no classpath");
        }
        assertFalse(r.isExperimental(BASE));
        assertFalse(r.isExperimental("pegada_1_entrada_preto"));
    }

    // ---------------- 36. padrao continua autoral
    @Test
    void modoAutoralNuncaEscolheExperimental() {
        RegistroAnimacoesCombate r = reg();
        assertEquals(ModoAnimacoesPegada.AUTORAL, ModoAnimacoesPegada.atual());
        IrmaoMiau[][] duplas = {{b(1), p(2)}, {p(1), b(2)}, {b(1), b(2)}, {p(1), p(2)},
            {joelhos(b(1)), joelhos(p(2))}, {joelhos(p(1)), joelhos(b(2))}};
        for (IrmaoMiau[] d : duplas) {
            SelecaoAnimacaoConjunta s = r.resolver(BASE, d[0], d[1]);
            assertFalse(s.experimental(), s.descricao());
            assertFalse(r.isExperimental(s.idTrilhaAplicador()) || r.isExperimental(s.idTrilhaReceptor()));
        }
        assertEquals(BASE, r.resolver(BASE, b(1), p(2)).idTrilhaAplicador());
        assertEquals("pegada_1_entrada_preto", r.resolver(BASE, p(1), b(2)).idTrilhaAplicador());
    }

    @Test
    void experimentalSoComModoLigadoMesmoQuandoEAUnicaQueCombina() {
        // Registro pequeno: a unica animacao com o Preto aplicador e EXPERIMENTAL.
        RegistroAnimacoesCombate r = RegistroAnimacoesCombate.vazio();
        AnimacaoConjuntaQuadros base = new RegistroAnimacoesCombate().buscar(BASE);
        r.registrar("base", base);
        r.registrarIdentidades("base", IdentidadeRig.BRANCO, IdentidadeRig.PRETO);
        r.registrar("exp", base);
        r.registrarIdentidades("exp", IdentidadeRig.PRETO, IdentidadeRig.BRANCO);
        r.registrarExperimental("exp");
        r.registrarVariante("base", "exp");
        assertEquals(SelecaoAnimacaoConjunta.Tipo.BASE, r.resolver("base", p(1), b(2), ModoAnimacoesPegada.AUTORAL).tipo(),
                "modo autoral: a experimental nunca entra");
        assertEquals("exp", r.resolver("base", p(1), b(2), ModoAnimacoesPegada.EXPERIMENTAL).idTrilhaAplicador());
        assertEquals("base", r.resolver("base", b(1), p(2), ModoAnimacoesPegada.EXPERIMENTAL).idTrilhaAplicador(),
                "sem experimental compativel: volta a autoral");
    }

    // ---------------- 31, 32, 33. selecao experimental por molde, papel, postura e lado
    @Test
    void modoExperimentalPorMoldePapelEPostura() {
        RegistroAnimacoesCombate r = reg();
        ModoAnimacoesPegada m = ModoAnimacoesPegada.EXPERIMENTAL;
        assertEquals(B_EXP, r.resolver(BASE, b(100), p(184), m).idTrilhaAplicador());
        assertEquals(B_EXP, r.resolver(BASE, b(500), p(416), m).idTrilhaAplicador(), "aplicador a direita: igual");
        assertEquals(P_EXP, r.resolver(BASE, p(100), b(184), m).idTrilhaAplicador());
        assertEquals(P_EXP, r.resolver(BASE, p(500), b(416), m).idTrilhaAplicador());
        assertEquals(B_JOE, r.resolver(BASE, joelhos(b(1)), joelhos(p(2)), m).idTrilhaAplicador());
        assertEquals(P_JOE, r.resolver(BASE, joelhos(p(1)), joelhos(b(2)), m).idTrilhaAplicador());

        SelecaoAnimacaoConjunta bb = r.resolver(BASE, b(1), b(2), m);
        assertEquals(SelecaoAnimacaoConjunta.Tipo.COMPOSTA_POR_PAPEL, bb.tipo());
        assertEquals(B_EXP, bb.idTrilhaAplicador());
        assertEquals(P_EXP, bb.idTrilhaReceptor(), "Branco receptor desenhado na experimental do Preto");
        SelecaoAnimacaoConjunta pp = r.resolver(BASE, joelhos(p(1)), joelhos(p(2)), m);
        assertEquals(P_JOE, pp.idTrilhaAplicador());
        assertEquals(B_JOE, pp.idTrilhaReceptor());
        assertTrue(pp.experimental() && pp.descricao().contains("EXPERIMENTAL"));
    }

    @Test
    void trocarOModoNoMeioDaTecnicaNaoMudaAPegadaEmCurso() {
        IrmaoMiau x = b(440);
        IrmaoMiau y = p(500);
        x.setViradoParaDireita(true);
        y.setViradoParaDireita(false);
        SistemaCombate s = CarregadorConteudoCombate.carregarPadrao().criarSistema(x, y);
        ModoAnimacoesPegada.definir(ModoAnimacoesPegada.EXPERIMENTAL);
        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(x, y, "pegada_1"));
        ModoAnimacoesPegada.alternar();
        for (int i = 0; i < 31; i++) {
            s.atualizar();
        }
        assertEquals(B_EXP, s.getSelecaoAnimacao().idTrilhaAplicador(), "escolhida no inicio");
        assertArrayEquals(s.getSelecaoAnimacao().quadros().copiarQuadroAplicador(3), x.copiarPoseCombateExterna());
    }

    // ---------------- geometria de contato das experimentais
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
        for (int yy = 0; yy < 450; yy++) {
            for (int xx = 0; xx < 800; xx++) {
                m[yy][xx] = (img.getRGB(xx, yy) >>> 24) > 128;
            }
        }
        return m;
    }

    @Test
    void experimentaisTocamOAdversarioSemAtravessar() {
        RegistroAnimacoesCombate r = reg();
        for (String id : new String[] {B_EXP, P_EXP, B_JOE, P_JOE}) {
            for (boolean aplicadorAEsquerda : new boolean[] {true, false}) {
                AnimacaoConjuntaQuadros q = r.exigir(id);
                boolean brancoAplica = r.getIdentidadeAplicador(id) == IdentidadeRig.BRANCO;
                int xa = aplicadorAEsquerda ? 300 : 384;
                int xr = aplicadorAEsquerda ? 384 : 300;
                IrmaoMiau apl = brancoAplica ? b(xa) : p(xa);
                IrmaoMiau rec = brancoAplica ? p(xr) : b(xr);
                apl.setViradoParaDireita(aplicadorAEsquerda);
                rec.setViradoParaDireita(!aplicadorAEsquerda);
                boolean joe = "AJOELHADO".equals(r.getPostura(id));
                int[] pa = q.copiarQuadroAplicador(2);
                int[] pr = q.copiarQuadroReceptor(2);
                if (joe) {
                    apl.ajoelhar();
                    rec.ajoelhar();
                    pa = PosturaAjoelhada.adaptar(apl, pa);
                    pr = PosturaAjoelhada.adaptar(rec, pr);
                }
                apl.aplicarPoseCombateExterna(pa, 0, joe ? PosturaAjoelhada.quedaPx(apl) : 0);
                rec.aplicarPoseCombateExterna(pr, 0, joe ? PosturaAjoelhada.quedaPx(rec) : 0);
                boolean[][] ma = mascara(apl);
                boolean[][] mr = mascara(rec);
                int contato = 0;
                int minR = 800;
                int maxR = -1;
                int minA = 800;
                int maxA = -1;
                for (int yy = 0; yy < 450; yy++) {
                    for (int xx = 0; xx < 800; xx++) {
                        contato += ma[yy][xx] && mr[yy][xx] ? 1 : 0;
                        if (mr[yy][xx]) {
                            minR = Math.min(minR, xx);
                            maxR = Math.max(maxR, xx);
                        }
                        if (ma[yy][xx]) {
                            minA = Math.min(minA, xx);
                            maxA = Math.max(maxA, xx);
                        }
                    }
                }
                String caso = id + (aplicadorAEsquerda ? " (esq)" : " (dir)");
                assertTrue(contato > 20, caso + ": maos chegam ao adversario (" + contato + " px)");
                int largura = maxR - minR;
                if (aplicadorAEsquerda) {
                    assertTrue(maxA < minR + largura * 0.6, caso + ": nao atravessa o adversario");
                } else {
                    assertTrue(minA > maxR - largura * 0.6, caso + ": nao atravessa o adversario");
                }
            }
        }
    }

    // ---------------- 34 e 35. transicoes sem saltos
    @Test
    void transicoesComecamETerminamNasPosturasSemSaltos() {
        TransicoesPostura tr = new TransicoesPostura(null);
        for (IrmaoMiau l : new IrmaoMiau[] {b(300), p(300)}) {
            for (TransicaoPostura t : TransicaoPostura.values()) {
                TransicoesPostura.Pose ini = tr.quadro(l, t, 0.0);
                TransicoesPostura.Pose fim = tr.quadro(l, t, 1.0);
                TransicoesPostura.Pose de = tr.pose(l, t.getOrigem());
                TransicoesPostura.Pose para = tr.pose(l, t.getDestino());
                assertArrayEquals(java.util.Arrays.copyOfRange(de.valores(), 4, 30),
                        java.util.Arrays.copyOfRange(ini.valores(), 4, 30), t + ": comeca na postura de origem");
                assertArrayEquals(java.util.Arrays.copyOfRange(para.valores(), 4, 30),
                        java.util.Arrays.copyOfRange(fim.valores(), 4, 30), t + ": termina na de destino");
                assertEquals(de.dy(), ini.dy(), 1e-9);
                assertEquals(para.dy(), fim.dy(), 1e-9);
                int passos = t.getDuracaoTicks();
                for (int i = 1; i <= passos; i++) {
                    double a = tr.quadro(l, t, (i - 1) / (double) passos).dy();
                    double c = tr.quadro(l, t, i / (double) passos).dy();
                    assertTrue(Math.abs(c - a) <= 22, t + ": sem salto vertical entre ticks (" + (c - a) + ")");
                }
            }
        }
    }

    @Test
    void rapidasSaoExplosivas() {
        TransicoesPostura tr = new TransicoesPostura(null);
        IrmaoMiau l = b(300);
        double meioLento = tr.quadro(l, TransicaoPostura.LEVANTAR, 0.5).dy();
        double meioRapido = tr.quadro(l, TransicaoPostura.RAPIDA_LEVANTAR, 0.5).dy();
        assertTrue(meioRapido < meioLento, "na metade, a rapida ja subiu mais");
        double minimo = 0;
        for (int i = 0; i <= 20; i++) {
            minimo = Math.min(minimo, tr.quadro(l, TransicaoPostura.RAPIDA_LEVANTAR_DO_CHAO, i / 20.0).dy());
        }
        assertTrue(minimo < 0, "levantar rapido passa um pouco do ponto (impulso) e assenta");
        int[] rapida = tr.quadro(l, TransicaoPostura.RAPIDA_DEITAR, 0.5).valores();
        int[] lenta = tr.quadro(l, TransicaoPostura.DEITAR, 0.5).valores();
        assertTrue(rapida[20] > lenta[20] + 10, "deitar rapido: bracos a frente para amortecer");
    }

    // ---------------- 22-25. esforco no combate
    private SistemaCombate pegada(IrmaoMiau apl, IrmaoMiau rec) {
        apl.setViradoParaDireita(apl.getX() < rec.getX());
        rec.setViradoParaDireita(!apl.isViradoParaDireita());
        SistemaCombate s = CarregadorConteudoCombate.carregarPadrao().criarSistema(apl, rec);
        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(apl, rec, "pegada_1"));
        for (int i = 0; i < 31; i++) {
            s.atualizar();
        }
        assertTrue(s.isInteracaoEstabelecida());
        return s;
    }

    @Test
    void arrastoCobraSoDoAplicadorPeloDeslocamentoReal() {
        IrmaoMiau apl = b(440);
        IrmaoMiau rec = p(500);
        SistemaCombate s = pegada(apl, rec);
        double fa = apl.getFolegoAtual();
        double fr = rec.getFolegoAtual();
        for (int i = 0; i < 40; i++) {
            assertEquals(ResultadoAcaoTecnica.SUCESSO, s.deslocarPegadaControlada(apl, 1, 15, 715));
        }
        assertEquals(fa - 40 * CustosFolego.ARRASTO_POR_PIXEL, apl.getFolegoAtual(), 1e-9);
        assertEquals(fr, rec.getFolegoAtual(), 1e-12, "o receptor nao paga por ser levado");
        assertEquals("arrasto", apl.getUltimoCustoMotivo());

        double antes = apl.getFolegoAtual();
        assertEquals(ResultadoAcaoTecnica.LIMITE_ARENA, s.deslocarPegadaControlada(apl, 1, 15, apl.getX()));
        assertEquals(antes, apl.getFolegoAtual(), 1e-12, "parado no limite: sem custo de arrasto");
    }

    @Test
    void arrastoDoPretoAplicadorTambemCobraDele() {
        IrmaoMiau apl = p(500);
        IrmaoMiau rec = b(440);
        SistemaCombate s = pegada(apl, rec);
        double fa = apl.getFolegoAtual();
        double fr = rec.getFolegoAtual();
        for (int i = 0; i < 10; i++) {
            s.deslocarPegadaControlada(apl, -1, 15, 715);
        }
        assertEquals(fa - 10 * CustosFolego.ARRASTO_POR_PIXEL, apl.getFolegoAtual(), 1e-9);
        assertEquals(fr, rec.getFolegoAtual(), 1e-12);
    }

    @Test
    void custosDeCombateExistentesPreservados() {
        // 5L.1 (pedido): balanceamento 3/4/3 -> 2/3/2,5.
        assertEquals(2.0, SistemaCombate.CUSTO_REFORCO);
        assertEquals(3.0, SistemaCombate.CUSTO_DESEQUILIBRIO);
        assertEquals(2.5, SistemaCombate.CUSTO_ROMPIMENTO_TECNICO);
        IrmaoMiau apl = b(440);
        IrmaoMiau rec = p(500);
        SistemaCombate s = pegada(apl, rec);
        double fa = apl.getFolegoAtual();
        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.tentarDesequilibrio(apl));
        assertEquals(fa - SistemaCombate.CUSTO_DESEQUILIBRIO, apl.getFolegoAtual(), 1e-9, "um custo, sem duplicar");
        assertTrue(rec.getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO), "consequencia real no receptor");
        assertEquals(SistemaCombate.DURACAO_DESEQUILIBRIO_TICKS,
                rec.getEfeitos().getTicksRestantes(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
    }

    @Test
    void desequilibrioResistidoNaoTemConsequencia() {
        IrmaoMiau fraco = new IrmaoMiauBranco(440, 150, 0.7, new AtributosLutador(35, 25, 5, 22, 25, 5, 22, 45));
        IrmaoMiau rec = p(500);
        SistemaCombate s = pegada(fraco, rec);
        double fr = rec.getFolegoAtual();
        assertEquals(ResultadoAcaoTecnica.RESISTIDO, s.tentarDesequilibrio(fraco));
        assertFalse(rec.getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
        assertEquals(fr, rec.getFolegoAtual(), 1e-12);
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.tentarDesequilibrio(rec), "o receptor nao desequilibra");
        assertFalse(fraco.getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
    }
}
