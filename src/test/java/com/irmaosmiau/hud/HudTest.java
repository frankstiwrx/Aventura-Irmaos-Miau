package com.irmaosmiau.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

class HudTest {

    // =====================================================
    // VALORES DOS ATRIBUTOS
    // =====================================================
    @Test
    void dadosDoHudVemDosAtributosReaisSemRecalcular() {
        IrmaoMiau branco = new IrmaoMiauBranco(0, 150, 0.7);
        IrmaoMiau preto = new IrmaoMiauPreto(0, 150, 0.7);

        DadosLutadorHud b = DadosLutadorHud.de(branco, "J1");
        assertEquals("Irmão Miau Branco", b.getNome());
        assertEquals("J1", b.getRotulo());
        assertEquals(25.0, b.getFolegoMaximo());          // AtributosLutador do Branco
        assertEquals(35.0, b.getCondicionamentoMaximo());
        assertEquals(branco.getFolegoAtual(), b.getFolegoAtual());
        assertEquals(1.0, b.getPercentualFolego());
        assertEquals("25/25", b.textoFolego());
        assertEquals("35/35", b.textoCondicionamento());

        DadosLutadorHud p = DadosLutadorHud.de(preto, null);
        assertEquals(40.0, p.getFolegoMaximo());
        assertEquals(25.0, p.getCondicionamentoMaximo());

        // Depois de gastar folego, o HUD acompanha o valor real.
        branco.consumirFolegoCombate(3.0);
        DadosLutadorHud depois = DadosLutadorHud.de(branco, "J1");
        assertEquals(22.0, depois.getFolegoAtual(), 1e-9);
        assertEquals(branco.getPercentualFolego(), depois.getPercentualFolego(), 1e-9);
        assertEquals("22/25", depois.textoFolego());
    }

    @Test
    void percentualELimitadoEntreZeroEUm() {
        assertEquals(0.0, new DadosLutadorHud(null, "x", -5, 10, 5, 10).getPercentualFolego());
        assertEquals(1.0, new DadosLutadorHud(null, "x", 50, 10, 5, 10).getPercentualFolego());
        assertEquals(0.0, new DadosLutadorHud(null, "x", 5, 0, 5, 10).getPercentualFolego());
    }

    // =====================================================
    // FEEDBACK DA TECNICA E DA INTERACAO
    // =====================================================
    private static IrmaoMiau branco(int x) {
        IrmaoMiau b = new IrmaoMiauBranco(x, 150, 0.7);
        b.setViradoParaDireita(true);
        return b;
    }

    private static IrmaoMiau preto() {
        IrmaoMiau p = new IrmaoMiauPreto(500, 150, 0.7);
        p.setViradoParaDireita(false);
        return p;
    }

    @Test
    void feedbackExplicaSucessoEIndisponibilidade() {
        IrmaoMiau b = branco(220);
        IrmaoMiau p = preto();
        SistemaCombate s = CarregadorConteudoCombate.carregarParaJogo().criarSistema(b, p);
        FeedbackCombate f = new FeedbackCombate();

        f.registrarTentativa(s, b, p, "pegada_1", s.tentarAcao(b, p, "pegada_1"), 10);
        assertEquals("Pegada 1: fora de alcance", f.getMensagem(10));
        assertEquals(FeedbackCombate.Tipo.AVISO, f.getTipo());

        b.setViradoParaDireita(false);
        IrmaoMiau perto = branco(440);
        perto.setViradoParaDireita(false);
        SistemaCombate s2 = CarregadorConteudoCombate.carregarParaJogo().criarSistema(perto, p);
        f.registrarTentativa(s2, perto, p, "pegada_1", s2.tentarAcao(perto, p, "pegada_1"), 20);
        assertEquals("Pegada 1: de costas", f.getMensagem(20));

        perto.setViradoParaDireita(true);
        f.registrarTentativa(s2, perto, p, "pegada_1", s2.tentarAcao(perto, p, "pegada_1"), 30);
        assertEquals("Pegada 1!", f.getMensagem(30));
        assertEquals(FeedbackCombate.Tipo.SUCESSO, f.getTipo());

        f.registrarTentativa(s2, perto, p, "pegada_1", s2.tentarAcao(perto, p, "pegada_1"), 31);
        assertEquals("Pegada 1: aguarde a animação", f.getMensagem(31));

        f.registrarTentativa(s2, perto, p, "pegada_2", s2.tentarAcao(perto, p, "pegada_2"), 32);
        assertEquals("Pegada 2: ainda não disponível", f.getMensagem(32));

        // Mensagem some depois de ~1,6 s
        assertNull(f.getMensagem(32 + FeedbackCombate.DURACAO_MENSAGEM_TICKS));
    }

    @Test
    void feedbackAvisaRompimentoEEstadoDaInteracao() {
        IrmaoMiau b = branco(440);
        IrmaoMiau p = preto();
        SistemaCombate s = CarregadorConteudoCombate.carregarParaJogo().criarSistema(b, p);
        FeedbackCombate f = new FeedbackCombate();

        assertNull(FeedbackCombate.textoEstadoInteracao(s));
        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(b, p, "pegada_1"));
        assertEquals("PEGADA 1 · ENTRANDO", FeedbackCombate.textoEstadoInteracao(s));
        for (int t = 0; t < 30; t++) {
            s.atualizar();
            assertTrue(!f.observar(s, t));
        }
        // Entrega 5B: estabelecida mostra a firmeza.
        assertEquals("PEGADA 1 · FIRMEZA 100%", FeedbackCombate.textoEstadoInteracao(s));

        b.moverX(-5);
        s.atualizar();
        assertTrue(f.observar(s, 40));
        assertEquals("Pegada desfeita: movimento", f.getMensagem(40));
        assertNull(FeedbackCombate.textoEstadoInteracao(s));
    }

    // =====================================================
    // DESENHO
    // =====================================================
    @Test
    void hudDesenhaSemErroENaoTocaAFaixaDosPersonagens() {
        IrmaoMiau b = new IrmaoMiauBranco(0, 150, 0.7);
        IrmaoMiau p = new IrmaoMiauPreto(0, 150, 0.7);
        HudCombate.Estado estado = new HudCombate.Estado(
                List.of(DadosLutadorHud.de(b, "JOGADOR"), DadosLutadorHud.de(p, "PARCEIRO")),
                "PEGADA 1 · ESTABELECIDA", "Pegada 1!", FeedbackCombate.Tipo.SUCESSO,
                true, "F5 reiniciar · ESC pausa");

        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        new HudCombate().desenhar(g, estado, 800, 450);
        g.dispose();

        // Algo foi desenhado no topo e na faixa de dicas...
        assertTrue((img.getRGB(20, 20) >>> 24) > 0, "painel do jogador");
        assertTrue((img.getRGB(780, 20) >>> 24) > 0, "painel do parceiro");
        assertTrue((img.getRGB(400, 445) >>> 24) > 0, "dica de controles");
        // ...e nada entre y = 70 e y = 430 (cabecas, corpos e pes).
        for (int y = 70; y < 430; y += 3) {
            for (int x = 0; x < 800; x += 5) {
                assertEquals(0, img.getRGB(x, y) >>> 24, "HUD cobrindo x=" + x + " y=" + y);
            }
        }
    }

    @Test
    void estadoDoHudExigeUmOuDoisLutadores() {
        IrmaoMiau b = new IrmaoMiauBranco(0, 150, 0.7);
        DadosLutadorHud d = DadosLutadorHud.de(b, null);
        assertThrows(IllegalArgumentException.class,
                () -> new HudCombate.Estado(List.of(), null, null, null, false, null));
        assertThrows(IllegalArgumentException.class,
                () -> new HudCombate.Estado(List.of(d, d, d), null, null, null, false, null));
        assertNotNull(new HudCombate.Estado(List.of(d), null, null, null, false, null));
    }
}
