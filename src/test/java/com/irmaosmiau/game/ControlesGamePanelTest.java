package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.combate.TipoDisputa;
import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.hud.AjudaControles;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Entrega 5C no GamePanel real: teclas configuradas -> acoes logicas -> mesmos
 * resultados de antes (PEGADA_1, T, U, Y, arrasto), treino, dois jogadores,
 * perda de foco e ajuda com as teclas reais.
 */
class ControlesGamePanelTest {

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

    @AfterEach
    void restaurarGlobal() {
        PreferenciasControles.instalarGlobal(null);
    }

    private static TeclaFisica t(String nome) {
        return TeclaFisica.porNome(nome).orElseThrow();
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void criar(MapaControles mapa, boolean treino) {
        PreferenciasControles.instalarGlobal(PreferenciasControles.emMemoria(mapa));
        painel = treino
                ? new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { })
                : new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
    }

    /** Aperta/solta uma tecla FISICA pelo InputMap real do painel. */
    private void fisica(String keystroke) {
        Object nome = mapeado(keystroke);
        assertNotNull(nome, "nada mapeado em " + keystroke);
        painel.getActionMap().get(nome).actionPerformed(null);
    }

    private Object mapeado(String keystroke) {
        return painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(keystroke));
    }

    private void toqueFisico(String tecla) {
        fisica("pressed " + tecla);
        ticks(1);
        fisica("released " + tecla);
        ticks(1);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private SistemaCombate sistema() {
        return painel.getSistemaCombate();
    }

    /** Anda com a tecla de direita configurada ate o alcance e aperta a tecla de pegada configurada. */
    private void estabelecerPegada(String direita, String pegada) {
        fisica("pressed " + direita);
        for (int i = 0; i < 200 && !sistema().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
            ticks(1);
        }
        fisica("released " + direita);
        toqueFisico(pegada);
        ticks(31);
        assertTrue(sistema().isInteracaoEstabelecida(), "pegada estabelecida");
    }

    // =====================================================
    // REGRESSAO COM OS PADROES
    // =====================================================
    @Test
    void padroesContinuamIguaisNoJogo() throws Exception {
        naEdt(() -> {
            criar(null, false);
            assertEquals(MapaControles.padrao(), painel.getMapaControles());
            double folego = painel.getLutador1().getFolegoAtual();
            estabelecerPegada("NUMPAD6", "2");   // 5O.1 (pedido): teclas novas do J1
            assertEquals(folego - 3, painel.getLutador1().getFolegoAtual(), 1.0, "custo 3 uma vez");
            toqueFisico("0");                            // 5O.1 (pedido): T -> 0
            assertTrue(painel.getLutador2().getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
            String dica = painel.montarEstadoHud().getDicaControles();
            assertTrue(dica.contains("0 desequilibrar") && dica.contains("Y reforçar")   // 5O.1 (pedido)
                    && dica.contains("CTRL+NUM 4/NUM 6 arrastar") && dica.contains("U romper"), dica);
        });
        naEdt(() -> {
            PreferenciasControles.instalarGlobal(null);
            GamePanel individual = new GamePanel(TipoPersonagem.PRETO, () -> { }, () -> { }, () -> { });
            individual.pararLoopParaTestes();
            assertEquals("NUM 8456 mover · NUM 0 correr · ESPAÇO pular · V pulo curto · C agachar · ESC pausa · F4 cenário",
                    individual.montarEstadoHud().getDicaControles(), "faixa do modo individual inalterada");
        });
    }

    // =====================================================
    // TECLAS PERSONALIZADAS NAS TECNICAS
    // =====================================================
    @Test
    void pegada1ComTeclaPersonalizada() throws Exception {
        naEdt(() -> {
            criar(MapaControles.padrao().comTecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA, t("P")), false);
            assertNull(mapeado("pressed 2"), "2 nao faz mais nada");   // 5O.1 (pedido): H -> 2
            double folego = painel.getLutador1().getFolegoAtual();
            estabelecerPegada("NUMPAD6", "P");   // 5O.1 (pedido)
            assertEquals("pegada_1", sistema().getIdAcaoInteracao());
            assertEquals(folego - 3, painel.getLutador1().getFolegoAtual(), 1.0);
            assertTrue(painel.montarEstadoHud().getEstadoInteracao().startsWith("PEGADA 1 · FIRMEZA"));
        });
    }

    @Test
    void desequilibrioReforcoERompimentoComTeclasPersonalizadas() throws Exception {
        naEdt(() -> {
            criar(MapaControles.padrao()
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.TENTAR_DESEQUILIBRIO, t("Q"))
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.REFORCAR_PEGADA, t("E"))
                    .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.ROMPIMENTO_TECNICO, t("P")), false);
            assertNull(mapeado("pressed 0"));            // 5O.1 (pedido): T -> 0
            assertNull(mapeado("pressed Y"));
            assertNull(mapeado("pressed U"));
            estabelecerPegada("NUMPAD6", "2");   // 5O.1 (pedido): teclas novas do J1

            toqueFisico("Q");
            assertEquals(TipoDisputa.DESEQUILIBRIO, sistema().getUltimaDisputa().getTipo());
            assertTrue(painel.getLutador2().getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));

            ticks(500);                                    // 5L.1: desgaste mais lento (antes 200)
            double antes = sistema().getFirmeza();
            toqueFisico("E");
            assertEquals("PEGADA REFORÇADA", painel.montarEstadoHud().getMensagem());
            assertTrue(sistema().getFirmeza() > antes + 20);

            toqueFisico("P");
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, sistema().getUltimaDisputa().getTipo());
        });
    }

    @Test
    void arrastoComModificadorEMovimentoRemapeados() throws Exception {
        naEdt(() -> {
            criar(MapaControles.padrao()
                    // 5O.1 (pedido): CTRL e NUM 4/6 viraram os padroes do J1; o remapeamento usa SHIFT e , .
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, t("SHIFT"))
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MOVER_ESQUERDA, t("SEMICOLON"))
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MOVER_DIREITA, t("PERIOD")), false);
            estabelecerPegada("PERIOD", "2");
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            int xb = b.getX();
            int distancia = p.getX() - b.getX();

            fisica("shift pressed SHIFT");
            fisica("shift pressed PERIOD");
            ticks(20);
            assertEquals("DESLOCAMENTO CONTROLADO", painel.montarEstadoHud().getMensagem());
            fisica("released SHIFT");
            fisica("released PERIOD");
            assertEquals(xb + 20, b.getX());
            assertEquals(distancia, p.getX() - b.getX());
            assertTrue(sistema().isInteracaoEstabelecida());
            assertNull(mapeado("ctrl pressed CONTROL"), "CTRL nao arrasta mais");

            fisica("pressed PERIOD");                   // sem o modificador: caminhada normal
            ticks(1);
            fisica("released PERIOD");
            assertEquals(MotivoEncerramentoInteracao.MOVIMENTO, sistema().getUltimoMotivoEncerramento());
        });
    }

    // =====================================================
    // TOQUE x CONTINUA, TRAVA E FOCO
    // =====================================================
    @Test
    void toqueDisparaUmaVezEContinuaValeEnquantoSegura() throws Exception {
        naEdt(() -> {
            criar(MapaControles.padrao()
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.REFORCAR_PEGADA, t("E"))
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MOVER_DIREITA, t("NUMPAD6")), false);
            int x = painel.getLutador1().getX();
            fisica("pressed NUMPAD6");
            ticks(10);
            int andou = painel.getLutador1().getX() - x;
            ticks(10);
            assertTrue(painel.getLutador1().getX() - x > andou, "continua andando enquanto segura");
            fisica("released NUMPAD6");

            estabelecerPegada("NUMPAD6", "2");   // 5O.1 (pedido): H -> 2
            ticks(300);
            double f = sistema().getFirmeza();
            fisica("pressed E");
            ticks(100);                                  // segurar alem da recarga (60)
            fisica("released E");
            assertEquals(Math.min(100, f + 25) - 100 * SistemaCombate.DESGASTE_FIRMEZA_POR_TICK,
                    sistema().getFirmeza(), 0.2, "um unico reforco por toque");
        });
    }

    @Test
    void teclaRemapeadaSeguradaNaEntradaNaoAndaSozinha() throws Exception {
        naEdt(() -> {
            criar(MapaControles.padrao().comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MOVER_DIREITA, t("NUMPAD6")),
                    false);
            fisica("pressed NUMPAD6");
            for (int i = 0; i < 200 && !sistema().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
                ticks(1);
            }
            toqueFisico("2");                           // ainda segurando a direita (5O.1: H -> 2)
            ticks(40);
            assertTrue(sistema().isInteracaoEstabelecida(), "TravaComandos: ninguem sai andando sozinho");
            fisica("released NUMPAD6");
            fisica("pressed NUMPAD6");
            ticks(1);
            assertEquals(MotivoEncerramentoInteracao.MOVIMENTO, sistema().getUltimoMotivoEncerramento());
        });
    }

    @Test
    void perdaDeFocoSoltaAsTeclas() throws Exception {
        naEdt(() -> {
            criar(null, false);
            fisica("pressed NUMPAD6");                  // 5O.1 (pedido): D -> NUM 6
            fisica("pressed LEFT");
            ticks(5);
            painel.simularPerdaDeFocoParaTestes();
            int xb = painel.getLutador1().getX();
            int xp = painel.getLutador2().getX();
            ticks(20);
            assertEquals(xb, painel.getLutador1().getX(), "Branco parou");
            assertEquals(xp, painel.getLutador2().getX(), "Preto parou");
            fisica("pressed NUMPAD6");
            ticks(5);
            assertTrue(painel.getLutador1().getX() > xb, "volta a responder");
        });
    }

    // =====================================================
    // TREINO E DOIS JOGADORES
    // =====================================================
    @Test
    void treinoComTeclasPersonalizadasEParceiroPassivo() throws Exception {
        naEdt(() -> {
            criar(MapaControles.padrao()
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA, t("P"))
                    .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.ROMPIMENTO_TECNICO, t("NUMPAD3")), true);   // 5O.1 (pedido): NUM 0 virou correr do J1
            estabelecerPegada("NUMPAD6", "P");   // 5O.1 (pedido)
            toqueFisico("NUMPAD3");                      // tecla do J2: ignorada no treino
            ticks(50);
            assertNull(sistema().getUltimaDisputa(), "parceiro nao defende");
            assertTrue(painel.montarEstadoHud().getDicaControles().contains("F5 reiniciar"));
            painel.getActionMap().get("reiniciarTreino").actionPerformed(null);
            assertFalse(sistema().estaEmInteracao());
            toqueFisico("P");                            // longe: fora de alcance, sem pegada
            assertFalse(sistema().estaEmInteracao());
        });
    }

    @Test
    void doisJogadoresComMovimentoDoJ2Remapeado() throws Exception {
        naEdt(() -> {
            criar(MapaControles.padrao()
                    .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.MOVER_ESQUERDA, t("COMMA")), false);
            int xp = painel.getLutador2().getX();
            int xb = painel.getLutador1().getX();
            fisica("pressed COMMA");
            ticks(10);
            fisica("released COMMA");
            assertTrue(painel.getLutador2().getX() < xp, "Preto andou com a tecla nova");
            assertEquals(xb, painel.getLutador1().getX(), "Branco parado");
            assertNull(mapeado("pressed LEFT"));
            assertTrue(painel.montarEstadoHud().getDicaControles().contains("J2: ↑/,/↓/→"),
                    painel.montarEstadoHud().getDicaControles());
        });
    }

    // =====================================================
    // AJUDA COM AS TECLAS CONFIGURADAS
    // =====================================================
    @Test
    void ajudaNaPausaEFaixaMostramAsTeclasConfiguradas() throws Exception {
        naEdt(() -> {
            criar(MapaControles.padrao()
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA, t("P"))
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.TENTAR_DESEQUILIBRIO, t("Q"))
                    .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, t("SHIFT")), false);   // 5O.1 (pedido): CTRL virou padrao
            String livre = painel.montarEstadoHud().getDicaControles();
            assertTrue(livre.contains("P pegar") && !livre.contains("2 pegar"), livre);

            estabelecerPegada("NUMPAD6", "P");   // 5O.1 (pedido)
            String agora = painel.montarEstadoHud().getDicaControles();
            assertTrue(agora.contains("Q desequilibrar") && agora.contains("SHIFT+NUM 4/NUM 6 arrastar")
                    && agora.contains("U romper"), agora);

            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            painel.mostrarAjudaControles(true);
            assertTrue(painel.isAjudaVisivel());
            PainelAjudaControles.Conteudo c = painel.montarAjudaControles();
            assertEquals(2, c.colunas().size());
            String j1 = c.colunas().get(0).linhas().stream()
                    .map(l -> l.texto() + "=" + l.tecla()).collect(Collectors.joining("|"));
            assertTrue(j1.contains("Iniciar pegada (PEGADA 1)=P"), j1);
            assertTrue(j1.contains("Desequilibrar (na pegada)=Q"), j1);
            assertTrue(j1.contains("=SHIFT (+NUM 4/NUM 6)"), j1);   // 5O.1 (pedido)
            assertTrue(c.agora().contains("J1: Q desequilibrar") && c.agora().contains("J2: U romper"), c.agora());

            // ESC fecha a ajuda e continua pausado; ESC de novo despausa.
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            assertFalse(painel.isAjudaVisivel());
            assertTrue(painel.isPausado());
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            assertFalse(painel.isPausado());
        });
    }

    @Test
    void ajudaRenderizaSemErroNaTelaDoJogo() throws Exception {
        naEdt(() -> {
            criar(null, true);
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            painel.mostrarAjudaControles(true);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                    GamePanel.LARGURA, GamePanel.ALTURA, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = img.createGraphics();
            painel.setSize(GamePanel.LARGURA, GamePanel.ALTURA);
            painel.paint(g);
            g.dispose();
            assertEquals(1, painel.montarAjudaControles().colunas().size(), "treino: so o jogador");
            assertTrue(painel.montarAjudaControles().observacao().contains("passivo"));
            assertTrue(AjudaControles.SISTEMA.contains("ESC pausa"));
        });
    }
}
