package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.AparenciaLutador;
import com.irmaosmiau.entities.AparenciaLutador.Parte;
import com.irmaosmiau.entities.CorKimono;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.DefinicaoPersonagem;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.personagens.PerfilLutador;
import com.irmaosmiau.regras.Graduacao;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Cores do personagem personalizado: renderizador real, contraste, faixa so visual, perfil salvo. */
class AparenciaPersonalizadaTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private static final Color VERMELHO = new Color(190, 30, 40);
    private static final Color AZUL = new Color(30, 60, 160);
    private static final Color OURO = new Color(230, 180, 30);
    private static final Color LARANJA = new Color(230, 140, 60);
    private static final AparenciaLutador CORES = AparenciaLutador.ORIGINAL.com(Parte.CASACO, VERMELHO)
            .com(Parte.CALCA, AZUL).com(Parte.FAIXA, OURO).com(Parte.PELO, LARANJA);

    private static DefinicaoPersonagem branco() {
        return CatalogoPersonagens.padrao().exigir(CatalogoPersonagens.ID_BRANCO);
    }

    private ServicoPerfis servico() {
        return new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao());
    }

    @Test
    void cadaParteTemSuaCorNoLutador() {
        IrmaoMiau l = branco().criarLutador(0, 0, 0.7);
        l.aplicarAparencia(CORES);
        assertEquals(VERMELHO, l.getCorCasaco());
        assertEquals(AZUL, l.getCorCalca(), "a calca independente do casaco");
        assertEquals(OURO, l.getCorFaixa());
        assertEquals(LARANJA, l.getCorPelo());
        IrmaoMiau so = branco().criarLutador(0, 0, 0.7);
        so.aplicarAparencia(AparenciaLutador.ORIGINAL.com(Parte.CASACO, VERMELHO));
        assertEquals(VERMELHO, so.getCorCalca(), "sem calca propria, a calca segue o kimono");
        so.setCorKimono(CorKimono.AZUL);
        assertEquals(so.getCorCasaco(), so.getCorCalca(), "o kimono escolhido vale nos dois");
    }

    @Test
    void detalhesDoRostoContrastamComAPelagem() {
        for (Color escura : new Color[] {Color.BLACK, new Color(40, 20, 10), new Color(20, 30, 90)}) {
            IrmaoMiau l = branco().criarLutador(0, 0, 0.7);
            l.aplicarAparencia(AparenciaLutador.ORIGINAL.com(Parte.PELO, escura));
            assertEquals(Color.WHITE, l.getCorRosto(), "pelagem escura: olhos claros");
        }
        for (Color clara : new Color[] {Color.WHITE, new Color(250, 230, 120), LARANJA}) {
            assertEquals(Color.BLACK, AparenciaLutador.contraste(clara), "pelagem clara: olhos escuros");
        }
        IrmaoMiau original = branco().criarLutador(0, 0, 0.7);
        assertEquals(AparenciaLutador.contraste(original.getCorPelo()), original.getCorRosto(),
                "a mesma regra dos originais");
    }

    @Test
    void faixaVisualNaoMudaAGraduacaoNemOsAtributos() {
        IrmaoMiau a = branco().criarLutador(0, 0, 0.7);
        IrmaoMiau b = branco().criarLutador(0, 0, 0.7);
        a.setGraduacao(Graduacao.ROXA);
        b.setGraduacao(Graduacao.ROXA);
        b.aplicarAparencia(AparenciaLutador.ORIGINAL.com(Parte.FAIXA, Color.BLACK));
        assertEquals(Graduacao.ROXA, b.getGraduacao());
        assertEquals(Color.BLACK, b.getCorFaixa(), "o desenho da faixa muda");
        assertNotEquals(a.getCorFaixa(), b.getCorFaixa());
        assertEquals(a.getAtributos().getForca(), b.getAtributos().getForca(), 1e-9, "atributos iguais");
        assertEquals(a.getAtributos().getTecnica(), b.getAtributos().getTecnica(), 1e-9);
    }

    @Test
    void salvaERecuperaEOsOriginaisNaoMudam() throws Exception {
        ServicoPerfis s = servico();
        PerfilLutador p = s.criarAPartirDoPersonagem("laranja", "Laranja", CatalogoPersonagens.ID_BRANCO);
        assertTrue(p.getAparencia().isOriginal());
        s.atualizarAparencia("laranja", CORES);
        PerfilLutador lido = servico().carregar("laranja").orElseThrow();   // como ao abrir o jogo de novo
        assertEquals(CORES, lido.getAparencia());
        String json = Files.readString(new RepositorioPerfis(new DiretorioDados(tmp)).arquivoDe("laranja"));
        assertTrue(json.contains("\"aparencia\"") && json.contains("#BE1E28"), "gravado como #RRGGBB: " + json);
        // o personagem oficial nunca muda
        IrmaoMiau oficial = branco().criarLutador(0, 0, 0.7);
        assertEquals(Color.WHITE, oficial.getCorPelo());
        assertEquals(Color.BLACK, oficial.getCorRosto());
        // na selecao: o perfil sai com as cores; com kimono escolhido, o kimono vale no casaco e na calca
        IrmaoMiau luta = EscolhaLutador.perfil(lido, branco()).criarLutador(0, 0, 0.7);
        assertEquals(VERMELHO, luta.getCorCasaco());
        assertEquals(LARANJA, luta.getCorPelo());
        IrmaoMiau comKimono = EscolhaLutador.perfil(lido, branco()).com(Graduacao.AZUL, CorKimono.AZUL)
                .criarLutador(0, 0, 0.7);
        assertEquals(CorKimono.AZUL.getCor(), comKimono.getCorCasaco());
        assertEquals(CorKimono.AZUL.getCor(), comKimono.getCorCalca());
        assertEquals(LARANJA, comKimono.getCorPelo(), "a pelagem continua a do perfil");
        assertEquals(Graduacao.AZUL, comKimono.getGraduacao(), "a graduacao e a da selecao");
    }

    @Test
    void corInvalidaNoArquivoEDadoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> AparenciaLutador.deHex("vermelho"));
        assertEquals(VERMELHO, AparenciaLutador.deHex(AparenciaLutador.hex(VERMELHO)));
        assertThrows(IllegalArgumentException.class, () -> AparenciaLutador.deHex("#12345"));
    }

    @Test
    void editorMudaAPreviaNaHoraEOkOuCancelar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EditorAparenciaPanel e = new EditorAparenciaPanel(() -> branco().criarLutador(60, 70, 0.95),
                    AparenciaLutador.ORIGINAL);
            e.controleParte().setSelectedItem(Parte.PELO);
            e.controleBrilho().setValue(5);                       // pelagem quase preta
            assertTrue(e.getPrevia().getCorPelo().getRed() < 20, "a previa (renderizador real) mudou na hora");
            assertEquals(Color.WHITE, e.getPrevia().getCorRosto(), "e os olhos acompanharam");
            e.controleParte().setSelectedItem(Parte.CALCA);
            e.controleMatiz().setValue(220);
            e.controleSaturacao().setValue(80);
            e.controleBrilho().setValue(70);
            assertNotEquals(e.getPrevia().getCorCasaco(), e.getPrevia().getCorCalca(), "calca independente");
            e.ok.doClick();
            assertEquals(e.getAparencia(), e.getResultado());
            EditorAparenciaPanel c = new EditorAparenciaPanel(() -> branco().criarLutador(60, 70, 0.95), CORES);
            c.controleBrilho().setValue(10);
            c.cancelar.doClick();
            assertNull(c.getResultado(), "cancelar nao devolve nada");
            c.controleParte().setSelectedItem(Parte.FAIXA);
            c.original.doClick();
            assertTrue(c.getAparencia().cor(Parte.FAIXA).isEmpty(), "COR ORIGINAL volta a parte");
        });
    }

    @Test
    void editorDePersonagensSalvaAAparenciaComOPerfil() throws Exception {
        servico().criarAPartirDoPersonagem("azulao", "Azulao", CatalogoPersonagens.ID_BRANCO);
        SwingUtilities.invokeAndWait(() -> {
            PerfisPanel tela = new PerfisPanel(servico(), new DialogosFalsos(), () -> { });
            tela.lista().setSelectedIndex(0);
            tela.definirAparencia(CORES);
            assertTrue(tela.temAlteracoesNaoSalvas(), "mudar a aparencia e uma alteracao");
            assertTrue(tela.salvar());
        });
        assertEquals(CORES, servico().carregar("azulao").orElseThrow().getAparencia());
    }
}
