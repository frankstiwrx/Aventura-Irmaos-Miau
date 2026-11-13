package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.cenario.CenarioDojo;
import com.irmaosmiau.combate.EstadoCorporalObservado;
import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.hud.HudCombate;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/**
 * Modo treino com o GamePanel REAL: teclas acionadas pelo ActionMap e
 * ticks executados na thread do Swing (o Timer e parado no inicio).
 */
class ModoTreinoGamePanelTest {

    private final AtomicInteger voltouAoMenu = new AtomicInteger();
    private final AtomicInteger recomecou = new AtomicInteger();

    private GamePanel painel;

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void criarTreino() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(ConfiguracaoTreino.padrao(),
                    voltouAoMenu::incrementAndGet, () -> { }, recomecou::incrementAndGet);
            painel.pararLoopParaTestes();
        });
    }

    private void tecla(String nome, boolean pressionada) throws Exception {
        naEdt(() -> {
            ActionMap mapa = painel.getActionMap();
            mapa.get(nome + (pressionada ? "Pressionada" : "Solta")).actionPerformed(null);
        });
    }

    private void atalho(String nome) throws Exception {
        naEdt(() -> painel.getActionMap().get(nome).actionPerformed(null));
    }

    private void ticks(int n) throws Exception {
        naEdt(() -> {
            for (int i = 0; i < n; i++) {
                painel.atualizarUmTickParaTestes();
            }
        });
    }

    /** Segura D ate a PEGADA_1 ficar disponivel; solta D. */
    private void aproximar() throws Exception {
        tecla("brancoDireita", true);
        for (int i = 0; i < 200; i++) {
            SistemaCombate s = painel.getSistemaCombate();
            if (s.podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1")) {
                break;
            }
            ticks(1);
        }
        tecla("brancoDireita", false);
        ticks(1);
        assertTrue(painel.getSistemaCombate().podeTentarAcao(
                painel.getLutador1(), painel.getLutador2(), "pegada_1"));
    }

    private static JButton botao(Container raiz, String texto, boolean somenteVisivel) {
        for (Component c : raiz.getComponents()) {
            if (c instanceof JButton && texto.equals(((JButton) c).getText())
                    && (!somenteVisivel || c.getParent().isVisible())) {
                return (JButton) c;
            }
            if (c instanceof Container) {
                JButton b = botao((Container) c, texto, somenteVisivel);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    // =====================================================
    // INICIALIZACAO
    // =====================================================
    @Test
    void treinoAbreNoDojoComBrancoJogadorEPretoParceiro() throws Exception {
        criarTreino();

        assertTrue(painel.isModoTreino());
        assertEquals(CenarioDojo.ID, painel.getCenario().getId());
        assertEquals("Irmão Miau Branco", painel.getLutador1().getNome());
        assertEquals("Irmão Miau Preto", painel.getLutador2().getNome());
        assertEquals("/conteudo/miaustudio/PEGADA_1.json",
                painel.getSistemaCombate().getRegistroAnimacoes().getOrigem("pegada_1_entrada"));
        assertFalse(painel.isDebugVisivel(), "debug comeca escondido no jogo");
    }

    @Test
    void modosAntigosContinuamFuncionandoNoDojo() throws Exception {
        naEdt(() -> {
            GamePanel dois = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
            dois.pararLoopParaTestes();
            assertFalse(dois.isModoTreino());
            assertNull(dois.getSessaoTreino());
            assertEquals(CenarioDojo.ID, dois.getCenario().getId());
            GamePanel sozinho = new GamePanel(TipoPersonagem.PRETO, () -> { }, () -> { }, () -> { });
            sozinho.pararLoopParaTestes();
            assertNull(sozinho.getSistemaCombate());
            for (int i = 0; i < 5; i++) {
                dois.atualizarUmTickParaTestes();
                sozinho.atualizarUmTickParaTestes();
            }
            // Os dois desenham sem erro (cenario + personagens + HUD).
            BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
            dois.paint(img.createGraphics());
            sozinho.paint(img.createGraphics());
        });
    }

    // =====================================================
    // CONTROLE DO JOGADOR E PARCEIRO PASSIVO
    // =====================================================
    @Test
    void jogadorAndaComWasdEOParceiroIgnoraAsSetas() throws Exception {
        criarTreino();
        IrmaoMiau jogador = painel.getLutador1();
        IrmaoMiau parceiro = painel.getLutador2();
        int xJogador = jogador.getX();
        String parceiroAntes = EstadoCorporalObservado.de(parceiro).toString();

        // Segundo jogador tenta mover, pular, deitar e rolar o Preto.
        tecla("pretoEsquerda", true);
        tecla("pretoPular", true);
        tecla("pretoDeitar", true);
        tecla("pretoRolar", true);
        // Jogador anda para a direita.
        tecla("brancoDireita", true);
        ticks(10);
        tecla("brancoDireita", false);
        tecla("pretoEsquerda", false);

        assertTrue(jogador.getX() > xJogador + 20, "Branco andou");
        assertEquals(parceiroAntes, EstadoCorporalObservado.de(parceiro).toString(), "Preto parado");
    }

    // =====================================================
    // PEGADA_1, PERMANENCIA E ROMPIMENTO NO TREINO
    // =====================================================
    @Test
    void pegada1ComTeclaHPermaneceERompeAoAndar() throws Exception {
        criarTreino();
        aproximar();

        tecla("brancoPegada1", true);
        ticks(1);
        tecla("brancoPegada1", false);
        SistemaCombate s = painel.getSistemaCombate();
        assertTrue(s.estaEmPegada1());
        HudCombate.Estado hud = painel.montarEstadoHud();
        assertNotNull(hud);

        ticks(30 + 120);
        assertTrue(s.isInteracaoEstabelecida(), "parado: pegada continua");
        assertTrue(painel.getLutador2().isPoseCombateExternaAtiva());

        // Andar para frente (continua no alcance: o motivo e o movimento).
        tecla("brancoDireita", true);
        ticks(1);
        tecla("brancoDireita", false);

        assertFalse(s.estaEmInteracao());
        assertEquals(MotivoEncerramentoInteracao.MOVIMENTO, s.getUltimoMotivoEncerramento());
        assertFalse(painel.getLutador2().isPoseCombateExternaAtiva());
    }

    // =====================================================
    // REINICIO (F5) E VOLTA AO MENU
    // =====================================================
    @Test
    void f5ReiniciaAsPosicoes() throws Exception {
        criarTreino();
        aproximar();
        tecla("brancoPegada1", true);
        ticks(5);
        tecla("brancoPegada1", false);
        IrmaoMiau antigo = painel.getLutador1();

        atalho("reiniciarTreino");

        assertEquals(1, painel.getSessaoTreino().getReinicios());
        assertEquals(ConfiguracaoTreino.X_JOGADOR_PADRAO, painel.getLutador1().getX());
        assertEquals(ConfiguracaoTreino.X_PARCEIRO_PADRAO, painel.getLutador2().getX());
        assertFalse(painel.getSistemaCombate().estaEmInteracao());
        assertFalse(antigo == painel.getLutador1());
        ticks(5);
        assertEquals(ConfiguracaoTreino.X_PARCEIRO_PADRAO, painel.getLutador2().getX());
    }

    @Test
    void pausaEMenuPrincipalVoltamAoMenu() throws Exception {
        criarTreino();

        atalho("alternarPausa");
        assertTrue(painel.isPausado());
        atalho("reiniciarTreino");          // F5 nao age com o jogo pausado
        assertEquals(0, painel.getSessaoTreino().getReinicios());

        naEdt(() -> botao(painel, "MENU PRINCIPAL", true).doClick(0));
        assertEquals(1, voltouAoMenu.get());
    }

    @Test
    void atalhosDeApresentacao() throws Exception {
        criarTreino();

        atalho("alternarDebug");
        assertTrue(painel.isDebugVisivel());
        atalho("proximoCenario");
        assertEquals("original", painel.getCenario().getId());
        atalho("proximoCenario");
        assertEquals("dojo", painel.getCenario().getId());
        atalho("alternarMusica");
        atalho("alternarMusica");
        atalho("alternarEfeitos");
        atalho("alternarEfeitos");
    }

    @Test
    void reiniciarForaDoTreinoNaoFazNada() throws Exception {
        naEdt(() -> {
            GamePanel dois = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
            dois.pararLoopParaTestes();
            int x = dois.getLutador1().getX();
            dois.getActionMap().get("reiniciarTreino").actionPerformed(null);
            assertEquals(x, dois.getLutador1().getX());
        });
    }

    @Test
    void hudDoTreinoMostraModoTreinoValoresEFeedback() throws Exception {
        criarTreino();
        HudCombate.Estado e = painel.montarEstadoHud();

        assertTrue(e.isModoTreino());
        assertEquals("JOGADOR", e.getLutadores().get(0).getRotulo());
        assertEquals("PARCEIRO", e.getLutadores().get(1).getRotulo());
        assertEquals(painel.getLutador1().getFolegoAtual(), e.getLutadores().get(0).getFolegoAtual());
        assertEquals(painel.getLutador2().getAtributos().getCondicionamento(),
                e.getLutadores().get(1).getCondicionamentoMaximo());
        assertTrue(e.getDicaControles().contains("F5 reiniciar"));
        assertNull(e.getEstadoInteracao());

        // H longe do parceiro: feedback explica o motivo.
        tecla("brancoPegada1", true);
        ticks(1);
        tecla("brancoPegada1", false);
        assertEquals("Pegada 1: fora de alcance", painel.montarEstadoHud().getMensagem());

        aproximar();
        tecla("brancoPegada1", true);
        ticks(1);
        tecla("brancoPegada1", false);
        assertEquals("Pegada 1!", painel.montarEstadoHud().getMensagem());
        assertEquals("PEGADA 1 · ENTRANDO", painel.montarEstadoHud().getEstadoInteracao());
        assertEquals(0, recomecou.get());
    }
}
