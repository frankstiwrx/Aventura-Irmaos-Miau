package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.Pegada1RegressaoArraysTest;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/** Entrega 5B no GamePanel real: SHIFT+A/D, Y, HUD, treino, F5 e encaixe visual. */
class ControlePegadaGamePanelTest {

    // 5O.1 (pedido): o J2 no teclado virou TECLADO-CONTROLE (layout fixo do Xbox). Estes testes
    // cobrem as TECLAS LIVRES configuraveis, que continuam existindo (-Dirmaosmiau.teclado.j2=livre);
    // o teclado-controle tem testes proprios (TecladoControle51Test).
    @org.junit.jupiter.api.BeforeEach
    void tecladoLivre51() {
        com.irmaosmiau.controles.LayoutTecladoControle.definirParaTestes(java.util.Set.of());
    }

    @org.junit.jupiter.api.AfterEach
    void tecladoPadrao51() {
        com.irmaosmiau.controles.LayoutTecladoControle.definirParaTestes(null);
    }

    private GamePanel painel;

    private void naEdt(Runnable r) throws Exception {
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

    private void estabelecerPegada() {
        SistemaCombate s = painel.getSistemaCombate();
        tecla("brancoDireita", true);
        for (int i = 0; i < 200 && !s.podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
            ticks(1);
        }
        tecla("brancoDireita", false);
        toque("brancoPegada1");
        ticks(31);
        assertTrue(s.isInteracaoEstabelecida());
    }

    private static GamePanel novo(boolean treino) {
        GamePanel g = treino
                ? new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { })
                : new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    /** Os dois lutadores desenhados como no GamePanel (espelhamento incluso), sem cenario. */
    private static BufferedImage dupla(IrmaoMiau... lutadores) {
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        for (IrmaoMiau l : lutadores) {
            Graphics2D g = img.createGraphics();
            if (!l.isViradoParaDireita()) {
                double eixo = l.getX() + 35 * 0.7;
                g.translate(eixo, 0);
                g.scale(-1, 1);
                g.translate(-eixo, 0);
            }
            l.desenhar(g);
            g.dispose();
        }
        return img;
    }

    // =====================================================
    // TECLAS E SWING
    // =====================================================
    @Test
    void shiftEYMapeadosSemQuebrarOsOutrosControles() throws Exception {
        naEdt(() -> {
            painel = novo(false);
            InputMap m = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            // 5O.1 (pedido): o modo controle do J1 passou de SHIFT para CTRL (SHIFT e o Select do J2).
            assertNotNull(m.get(KeyStroke.getKeyStroke("ctrl pressed CONTROL")), "CTRL liga o controle");
            assertNotNull(m.get(KeyStroke.getKeyStroke("released CONTROL")), "soltar CTRL desliga");
            assertEquals("brancoReforcoPressionada", m.get(KeyStroke.getKeyStroke("pressed Y")));
            // Com SHIFT segurado, todas as teclas continuam funcionando.
            for (String t : new String[]{"NUMPAD4", "NUMPAD6", "NUMPAD8", "NUMPAD5", "SPACE", "2", "0", "Y", "LEFT",
                "U"}) {
                assertNotNull(m.get(KeyStroke.getKeyStroke("ctrl pressed " + t)), t);
                assertNotNull(m.get(KeyStroke.getKeyStroke("ctrl released " + t)), t);
                assertEquals(m.get(KeyStroke.getKeyStroke("pressed " + t)),
                        m.get(KeyStroke.getKeyStroke("ctrl pressed " + t)), t);
            }
        });
    }

    // =====================================================
    // DESLOCAMENTO CONJUNTO
    // =====================================================
    @Test
    void shiftMaisDArrastaADuplaSemRomperEDSozinhoRompe() throws Exception {
        naEdt(() -> {
            painel = novo(false);
            estabelecerPegada();
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            int xb = b.getX();
            int distancia = p.getX() - b.getX();

            tecla("brancoControle", true);
            tecla("brancoDireita", true);
            ticks(40);
            assertEquals("DESLOCAMENTO CONTROLADO", painel.montarEstadoHud().getMensagem());
            tecla("brancoDireita", false);
            tecla("brancoControle", false);

            assertEquals(xb + 40, b.getX(), "1 px/tick");
            assertEquals(distancia, p.getX() - b.getX());
            assertTrue(painel.getSistemaCombate().isInteracaoEstabelecida());
            assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[2], b.copiarPoseCombateExterna());
            assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[2], p.copiarPoseCombateExterna());
            assertEquals(List.of(b, p), painel.ordemDeDesenho(), "mesma linha: ordem estavel");

            tecla("brancoDireita", true);       // sem SHIFT: caminhada normal
            ticks(1);
            tecla("brancoDireita", false);
            assertEquals(MotivoEncerramentoInteracao.MOVIMENTO,
                    painel.getSistemaCombate().getUltimoMotivoEncerramento());
        });
    }

    @Test
    void encaixeVisualIdenticoDepoisDoDeslocamento() throws Exception {
        naEdt(() -> {
            painel = novo(false);
            estabelecerPegada();
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            BufferedImage antes = dupla(b, p);

            tecla("brancoControle", true);
            tecla("brancoEsquerda", true);
            ticks(25);
            tecla("brancoEsquerda", false);
            tecla("brancoControle", false);
            int dx = -25;
            BufferedImage depois = dupla(b, p);

            int pixelsDosLutadores = 0;
            for (int y = 0; y < 450; y++) {
                for (int x = 40; x < 760; x++) {
                    int a = antes.getRGB(x, y);
                    assertEquals(a, depois.getRGB(x + dx, y), "pixel (" + x + "," + y + ")");
                    if ((a >>> 24) != 0) {
                        pixelsDosLutadores++;
                    }
                }
            }
            assertTrue(pixelsDosLutadores > 5000, "havia lutadores na imagem");
        });
    }

    @Test
    void deslocamentoNaoAtravessaOLimiteDaArena() throws Exception {
        naEdt(() -> {
            painel = novo(false);
            estabelecerPegada();
            tecla("brancoControle", true);
            tecla("brancoDireita", true);
            ticks(800);
            tecla("brancoDireita", false);
            tecla("brancoControle", false);
            assertTrue(painel.getLutador2().getX() <= GamePanel.LIMITE_DIREITO_ARENA);
            assertEquals(GamePanel.LIMITE_DIREITO_ARENA, painel.getLutador2().getX());
        });
    }

    // =====================================================
    // REFORCO, HUD E AVISOS
    // =====================================================
    @Test
    void hudMostraFirmezaEOReforcoComYUmaVezPorToque() throws Exception {
        naEdt(() -> {
            painel = novo(false);
            estabelecerPegada();
            assertTrue(painel.montarEstadoHud().getEstadoInteracao().startsWith("PEGADA 1 · FIRMEZA "),
                    painel.montarEstadoHud().getEstadoInteracao());

            ticks(300);                                    // firmeza ~70
            double f = painel.getSistemaCombate().getFirmeza();
            double folego = painel.getLutador1().getFolegoAtual();
            tecla("brancoReforco", true);
            ticks(100);                                    // segurar Y > recarga
            tecla("brancoReforco", false);

            assertEquals(Math.min(100, f + 25) - 100 * SistemaCombate.DESGASTE_FIRMEZA_POR_TICK,
                    painel.getSistemaCombate().getFirmeza(), 0.2, "um unico reforco");
            assertTrue(painel.getLutador1().getFolegoAtual() < folego + 3, "cobrado uma vez");
            assertEquals("PEGADA 1 · FIRMEZA " + Math.round(painel.getSistemaCombate().getFirmeza()) + "%",
                    painel.montarEstadoHud().getEstadoInteracao());
        });
    }

    @Test
    void avisosDeControleFracoEDePegadaSemControle() throws Exception {
        naEdt(() -> {
            painel = novo(false);
            estabelecerPegada();
            ticks(1170);                                   // cruza 30% (5L.1: desgaste mais lento; antes 705)
            assertEquals("CONTROLE ENFRAQUECENDO", painel.montarEstadoHud().getMensagem());
            for (int i = 0; i < 600 && painel.getSistemaCombate().estaEmInteracao(); i++) {
                ticks(1);                                  // ate a firmeza chegar a 0
            }
            assertFalse(painel.getSistemaCombate().estaEmInteracao());
            assertEquals(MotivoEncerramentoInteracao.SEM_CONTROLE,
                    painel.getSistemaCombate().getUltimoMotivoEncerramento());
            assertEquals("PEGADA DESFEITA: SEM CONTROLE", painel.montarEstadoHud().getMensagem());
        });
    }

    // =====================================================
    // TREINO E F5
    // =====================================================
    @Test
    void treinoComFirmezaReforcoEArrastoParceiroPassivoEF5() throws Exception {
        naEdt(() -> {
            painel = novo(true);
            estabelecerPegada();
            ticks(200);
            toque("brancoReforco");
            assertEquals("PEGADA REFORÇADA", painel.montarEstadoHud().getMensagem());
            tecla("brancoControle", true);
            tecla("brancoEsquerda", true);
            ticks(10);
            tecla("brancoEsquerda", false);
            tecla("brancoControle", false);
            assertTrue(painel.getSistemaCombate().isInteracaoEstabelecida());

            toque("pretoRompimento");                      // ignorado no treino
            ticks(100);
            assertNull(painel.getSistemaCombate().getUltimaDisputa(), "parceiro nao defende");

            SistemaCombate antigo = painel.getSistemaCombate();
            painel.getActionMap().get("reiniciarTreino").actionPerformed(null);
            assertNotSame(antigo, painel.getSistemaCombate());
            assertFalse(painel.getSistemaCombate().estaEmInteracao());
            assertEquals(0.0, painel.getSistemaCombate().getFirmeza());
            assertEquals(0, painel.getSistemaCombate().getRecargaReforcoRestante(painel.getLutador1()));
            assertEquals(ConfiguracaoTreino.X_JOGADOR_PADRAO, painel.getLutador1().getX());
            assertEquals(ConfiguracaoTreino.X_PARCEIRO_PADRAO, painel.getLutador2().getX());
        });
    }
}
