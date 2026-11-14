package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.ComandoGamepad;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioControles;
import com.irmaosmiau.persistencia.RepositorioDispositivos;
import com.irmaosmiau.persistencia.RepositorioGamepad;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5F: remapeamento do gamepad em CONFIGURACOES -> CONTROLES (gamepad simulado). */
class ControlesGamepadPanelTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private ControlesPanel tela;
    private PreferenciasGamepad prefs;
    private ProvedorGamepadSimulado sim;
    private final DialogosFalsos dialogos = new DialogosFalsos();
    private final AtomicInteger voltou = new AtomicInteger();

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;

    private void abrir() throws Exception {
        DiretorioDados d = new DiretorioDados(tmp);
        sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "XBOX", "Xbox Wireless Controller");
        prefs = PreferenciasGamepad.carregar(new RepositorioGamepad(d));
        PreferenciasControles c = PreferenciasControles.carregar(new RepositorioControles(d));
        PreferenciasDispositivos disp = PreferenciasDispositivos.carregar(new RepositorioDispositivos(d));
        SwingUtilities.invokeAndWait(() -> {
            tela = new ControlesPanel(c, disp, prefs, sim, dialogos, voltou::incrementAndGet);
            tela.selecionarModoLista(ControlesPanel.ModoLista.GAMEPAD);
        });
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private BotaoGamepad botao(ItemGamepad i) {
        return tela.gamepadEmEdicao().botao(J1, i).orElse(null);
    }

    @Test
    void listaOsBotoesDoClassicoECapturaUmBotaoReal() throws Exception {
        abrir();
        naEdt(() -> {
            ItemGamepad correr = ItemGamepad.de(AcaoJogador.CORRER);
            JButton b = tela.botaoGamepad(correr);
            assertNotNull(b);
            assertEquals("RB/R1", b.getText());
            b.doClick();
            assertTrue(tela.isCapturandoBotao());
            assertEquals("aperte um botão…", b.getText());
            tela.lerBotoesDaCapturaParaTestes();
            assertTrue(tela.isCapturandoBotao(), "nada apertado: continua esperando");
            dialogos.confirmacoes.add(true);                 // 5G: R3 agora e "rolar": trocar
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.R3));
            tela.lerBotoesDaCapturaParaTestes();
            assertFalse(tela.isCapturandoBotao());
            assertEquals(BotaoGamepad.R3, botao(correr));
            // 5G: o RB e dividido com o pulo alto; rolar nao pode dividi-lo e fica sem botao (avisado).
            assertEquals(null, botao(ItemGamepad.de(AcaoJogador.ROLAR)), "trocados sem conflito");
            assertTrue(tela.textoStatus().contains("ficou sem botão"), tela.textoStatus());
            assertTrue(dialogos.perguntas.get(0).contains("Rolar"), dialogos.perguntas.get(0));
            assertEquals("R3", tela.botaoGamepad(correr).getText());
        });
    }

    @Test
    void botaoJaSeguradoNoInicioEIgnoradoEGatilhoUsaLimiar() throws Exception {
        abrir();
        naEdt(() -> {
            ItemGamepad correr = ItemGamepad.de(AcaoJogador.CORRER);
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL));
            tela.iniciarCapturaGamepad(correr);
            tela.lerBotoesDaCapturaParaTestes();
            assertTrue(tela.isCapturandoBotao(), "A ja estava apertado: ignorado");
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL)
                    .comEixo(EixoGamepad.GATILHO_DIR, 0.3f));
            tela.lerBotoesDaCapturaParaTestes();
            assertTrue(tela.isCapturandoBotao(), "gatilho leve (0,3) nao conta");
            dialogos.confirmacoes.add(true);                 // RT era sprawl: trocar
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL)
                    .comEixo(EixoGamepad.GATILHO_DIR, 0.8f));
            tela.lerBotoesDaCapturaParaTestes();
            assertFalse(tela.isCapturandoBotao());
            assertEquals(BotaoGamepad.GATILHO_DIR, botao(correr));
            // 5G: RB dividido com o pulo alto: sprawl fica sem botao em vez de criar conflito.
            assertEquals(null, botao(ItemGamepad.de(AcaoJogador.SPRAWL)), "trocados sem conflito");
            assertTrue(dialogos.perguntas.get(0).contains("LT/L2") || dialogos.perguntas.get(0).contains("RT/R2"));
        });
    }

    @Test
    void conflitoRecusadoReservadoCancelarERemover() throws Exception {
        abrir();
        naEdt(() -> {
            ItemGamepad correr = ItemGamepad.de(AcaoJogador.CORRER);
            dialogos.confirmacoes.add(false);
            tela.iniciarCapturaGamepad(correr);
            tela.receberBotao(BotaoGamepad.SUL);
            assertEquals(BotaoGamepad.RB, botao(correr), "recusou a troca: nada mudou");

            tela.iniciarCapturaGamepad(correr);
            tela.receberBotao(BotaoGamepad.GUIA);
            assertTrue(tela.isCapturandoBotao(), "Guide e reservado: continua esperando");
            tela.receberBotao(BotaoGamepad.INICIAR);
            assertFalse(tela.isCapturandoBotao(), "Start cancela");

            tela.iniciarCapturaGamepad(correr);
            KeyEvent esc = new KeyEvent(tela, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
            assertTrue(tela.capturarEventoParaTestes(esc), "teclado preso na tela durante a captura");
            assertFalse(tela.isCapturandoBotao(), "ESC cancela");

            tela.iniciarCapturaGamepad(correr);
            KeyEvent espaco = new KeyEvent(tela, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_SPACE, ' ');
            assertTrue(tela.capturarEventoParaTestes(espaco));
            assertTrue(tela.isCapturandoBotao(), "ESPACO nao aciona nada nem vira botao");
            KeyEvent back = new KeyEvent(tela, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_BACK_SPACE, '\b');
            tela.capturarEventoParaTestes(back);
            assertEquals(null, botao(correr), "Backspace: sem botao (ex.: RB defeituoso)");
            assertTrue(tela.gamepadEmEdicao().isValido());
        });
    }

    @Test
    void esquemaContextualPorSlotSalvarEReabrir() throws Exception {
        abrir();
        naEdt(() -> {
            tela.seletorEsquema().setSelectedItem(EsquemaGamepad.CONTEXTUAL);
            assertEquals(EsquemaGamepad.CONTEXTUAL, tela.gamepadEmEdicao().esquema(J1));
            assertEquals(EsquemaGamepad.CLASSICO, tela.gamepadEmEdicao().esquema(SlotJogador.JOGADOR_2), "outro slot igual");
            assertNotNull(tela.botaoGamepad(ItemGamepad.de(ComandoGamepad.TECNICA_CIMA)));
            assertEquals("LT/L2", tela.botaoGamepad(ItemGamepad.de(ComandoGamepad.MODIFICADOR_CAMADA_2)).getText());
            tela.iniciarCapturaGamepad(ItemGamepad.de(ComandoGamepad.TECNICA_CIMA));
            dialogos.confirmacoes.add(true);
            tela.receberBotao(BotaoGamepad.NORTE);           // NORTE era rolar: troca
            assertEquals(BotaoGamepad.NORTE, botao(ItemGamepad.de(ComandoGamepad.TECNICA_CIMA)));
            assertTrue(tela.salvar());
        });
        MapaGamepad lido = PreferenciasGamepad.carregar(new RepositorioGamepad(new DiretorioDados(tmp))).getAtual();
        assertEquals(EsquemaGamepad.CONTEXTUAL, lido.esquema(J1));
        assertEquals(BotaoGamepad.NORTE, lido.botao(J1, ItemGamepad.de(ComandoGamepad.TECNICA_CIMA)).orElseThrow());
        assertTrue(Files.exists(tmp.resolve(RepositorioGamepad.NOME_ARQUIVO)));
    }

    @Test
    void restaurarEDescartar() throws Exception {
        abrir();
        naEdt(() -> {
            tela.seletorEsquema().setSelectedItem(EsquemaGamepad.CONTEXTUAL);
            tela.restaurarPadroes();
            assertEquals(MapaGamepad.padrao(), tela.gamepadEmEdicao());
            tela.seletorEsquema().setSelectedItem(EsquemaGamepad.CONTEXTUAL);
            dialogos.respostas.add(Dialogos.Resposta.NAO);
            tela.voltar();
            assertEquals(1, voltou.get());
            assertEquals(MapaGamepad.padrao(), prefs.getAtual(), "descartado");
        });
        assertFalse(Files.exists(tmp.resolve(RepositorioGamepad.NOME_ARQUIVO)));
    }

    @Test
    void cabeEm800x450() throws Exception {
        abrir();
        naEdt(() -> {
            tela.seletorEsquema().setSelectedItem(EsquemaGamepad.CONTEXTUAL);
            tela.setSize(800, 450);
            layout(tela);
            Rectangle r = new Rectangle(0, 0, 800, 450);
            for (Component c : tela.getComponents()) {
                assertTrue(r.contains(c.getBounds()), c.getBounds().toString());
            }
            for (Component c : new Component[]{tela.seletorEsquema(), tela.seletorLista(), tela.seletorDispositivo()}) {
                Rectangle b = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), tela);
                assertTrue(r.contains(b) && b.width >= 90, b.toString());
            }
            assertTrue(tela.rolagem().getHeight() >= 200, "lista legivel: " + tela.rolagem().getHeight());
            assertTrue(tela.textoSituacaoGamepads().contains("Xbox Wireless Controller"));
        });
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component f : c.getComponents()) {
            if (f instanceof Container cc) {
                layout(cc);
            }
        }
    }
}
