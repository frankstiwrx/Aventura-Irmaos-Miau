package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/**
 * Correcao: lutador numa linha mais ao fundo deve ser desenhado atras.
 */
class OrdemProfundidadeTest {

    private static IrmaoMiau branco(int linha) {
        IrmaoMiau b = new IrmaoMiauBranco(300, 150, 0.7);
        for (int i = 0; i < linha; i++) {
            b.descerLinha();
        }
        return b;
    }

    private static IrmaoMiau preto(int linha) {
        IrmaoMiau p = new IrmaoMiauPreto(300, 150, 0.7);
        for (int i = 0; i < linha; i++) {
            p.descerLinha();
        }
        return p;
    }

    // =====================================================
    // REGRA (personagens reais, sem tela)
    // =====================================================
    @Test
    void brancoAtrasDoPreto() {
        IrmaoMiau b = branco(0);
        IrmaoMiau p = preto(3);
        assertEquals(List.of(b, p), OrdemProfundidade.doFundoParaFrente(b, p));
    }

    @Test
    void pretoAtrasDoBranco() {
        IrmaoMiau b = branco(4);
        IrmaoMiau p = preto(1);
        assertEquals(List.of(p, b), OrdemProfundidade.doFundoParaFrente(b, p));
    }

    @Test
    void mesmaLinhaMantemOrdemEstavel() {
        IrmaoMiau b = branco(2);
        IrmaoMiau p = preto(2);
        for (int i = 0; i < 50; i++) {
            assertEquals(List.of(b, p), OrdemProfundidade.doFundoParaFrente(b, p));
        }
        assertEquals(List.of(p, b), OrdemProfundidade.doFundoParaFrente(p, b), "empate respeita a ordem dada");
    }

    @Test
    void puloEAgachamentoNaoTrocamAOrdem() {
        IrmaoMiau b = branco(1);   // atras
        IrmaoMiau p = preto(2);    // na frente

        b.pular();
        for (int i = 0; i < 8; i++) {
            b.atualizar(false, false);
            p.atualizar(false, true);   // Preto agachado
        }
        assertTrue(b.isPulando(), "Branco no ar");
        assertTrue(p.isAgachado(), "Preto agachado");
        assertTrue(b.getY() < p.getY() - 20, "o Y da tela mudou com o pulo...");
        assertEquals(List.of(b, p), OrdemProfundidade.doFundoParaFrente(b, p), "...mas a ordem nao");

        // Preto pulando na frente continua na frente.
        IrmaoMiau b2 = branco(1);
        IrmaoMiau p2 = preto(2);
        p2.pular();
        for (int i = 0; i < 10; i++) {
            p2.atualizar(false, false);
        }
        assertTrue(p2.getY() < b2.getY(), "Preto no ar fica mais alto na tela que o Branco");
        assertEquals(List.of(b2, p2), OrdemProfundidade.doFundoParaFrente(b2, p2));
    }

    // =====================================================
    // GAMEPANEL REAL: quem aparece por cima na imagem
    // =====================================================
    private GamePanel painel;

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    /** Desenha um lutador sozinho com o mesmo espelhamento do GamePanel. */
    private static BufferedImage sozinho(IrmaoMiau l) {
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
        return img;
    }

    /**
     * Onde os dois lutadores (desenhados sozinhos) sao totalmente opacos,
     * a tela do jogo deve mostrar o pixel de quem esta na frente.
     *
     * @return quantos pixels sobrepostos foram conferidos
     */
    private static int conferirSobreposicao(GamePanel p, IrmaoMiau frente, IrmaoMiau atras) {
        BufferedImage tela = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        p.setSize(800, 450);
        p.paint(tela.createGraphics());
        BufferedImage f = sozinho(frente);
        BufferedImage a = sozinho(atras);

        int conferidos = 0;
        for (int y = 64; y < 432; y++) {          // fora das faixas do HUD
            for (int x = 0; x < 800; x++) {
                int pf = f.getRGB(x, y);
                int pa = a.getRGB(x, y);
                if ((pf >>> 24) == 255 && (pa >>> 24) == 255 && pf != pa) {
                    assertEquals(pf, tela.getRGB(x, y), "pixel (" + x + "," + y + ") deveria ser de "
                            + frente.getNome());
                    conferidos++;
                }
            }
        }
        return conferidos;
    }

    private GamePanel doisMiaus() {
        GamePanel p = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        p.pararLoopParaTestes();
        return p;
    }

    @Test
    void gamePanelBrancoNaFrenteDoPretoQuandoMaisPerto() throws Exception {
        naEdt(() -> {
            painel = doisMiaus();
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            b.moverX(220);                 // 440 x 500: corpos se sobrepoem
            b.descerLinha();
            b.descerLinha();               // Branco linha 3, Preto linha 1
            painel.atualizarUmTickParaTestes();

            assertEquals(List.of(p, b), painel.ordemDeDesenho());
            assertTrue(conferirSobreposicao(painel, b, p) > 200, "precisa haver sobreposicao real");
        });
    }

    @Test
    void gamePanelPretoNaFrenteDoBrancoQuandoMaisPerto() throws Exception {
        naEdt(() -> {
            painel = doisMiaus();
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            b.moverX(220);
            p.descerLinha();
            p.descerLinha();               // Preto linha 3, Branco linha 1
            painel.atualizarUmTickParaTestes();

            assertEquals(List.of(b, p), painel.ordemDeDesenho());
            assertTrue(conferirSobreposicao(painel, p, b) > 200);
        });
    }

    @Test
    void gamePanelMesmaLinhaMantemBrancoAntesDoPreto() throws Exception {
        naEdt(() -> {
            painel = doisMiaus();
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            b.moverX(220);
            for (int i = 0; i < 30; i++) {
                painel.atualizarUmTickParaTestes();
                assertEquals(List.of(b, p), painel.ordemDeDesenho(), "tick " + i);
            }
            assertTrue(conferirSobreposicao(painel, p, b) > 200, "comportamento anterior preservado");
        });
    }

    @Test
    void durantePegada1ComLinhasDiferentesQuemEstaMaisPertoFicaNaFrente() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            SistemaCombate s = painel.getSistemaCombate();

            b.descerLinha();                    // Branco linha 1, Preto linha 0 (1 linha: pegada permitida)
            while (!s.podeTentarAcao(b, p, "pegada_1")) {
                b.moverX(2);
                painel.atualizarUmTickParaTestes();
            }
            painel.getActionMap().get("brancoPegada1Pressionada").actionPerformed(null);
            painel.atualizarUmTickParaTestes();
            painel.getActionMap().get("brancoPegada1Solta").actionPerformed(null);
            assertTrue(s.isAnimacaoCombateAtiva());

            for (int f = 0; f < 40; f++) {      // entrada (30) + pose final
                assertEquals(List.of(p, b), painel.ordemDeDesenho(), "frame " + f);
                painel.atualizarUmTickParaTestes();
            }
            assertTrue(s.isInteracaoEstabelecida());
            assertTrue(b.isPoseCombateExternaAtiva() && p.isPoseCombateExternaAtiva());
            int n = conferirSobreposicao(painel, b, p);
            assertTrue(n > 0, "pose do MiauStudio sobreposta corretamente (pixels: " + n + ")");
            System.out.println("[OrdemProfundidadeTest] pixels sobrepostos na PEGADA_1: " + n);
        });
    }
}
