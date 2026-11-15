package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.audio.SaidaAudio;
import com.irmaosmiau.audio.ServicoAudio;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioConfiguracoes;
import com.irmaosmiau.persistencia.RepositorioControles;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasJogo;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5C: tela CONFIGURACOES -> CONTROLES (headless, dialogos falsos, pasta de teste). */
class ControlesPanelTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private DialogosFalsos dialogos;
    private PreferenciasControles prefs;
    private ControlesPanel tela;
    private final AtomicInteger voltou = new AtomicInteger();

    private RepositorioControles repo() {
        return new RepositorioControles(new DiretorioDados(tmp));
    }

    @BeforeEach
    void preparar() {
        dialogos = new DialogosFalsos();
    }

    private void abrir() throws Exception {
        prefs = PreferenciasControles.carregar(repo());
        SwingUtilities.invokeAndWait(() -> tela = new ControlesPanel(prefs, dialogos, voltou::incrementAndGet));
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private String tecla(SlotJogador s, AcaoJogador a) {
        return tela.mapaEmEdicao().rotulo(s, a);
    }

    @Test
    void mostraTodasAsAcoesDeCadaJogadorComAsTeclasAtuais() throws Exception {
        abrir();
        naEdt(() -> {
            assertEquals(SlotJogador.JOGADOR_1, tela.slotAtual());
            for (AcaoJogador a : SlotJogador.JOGADOR_1.acoes()) {
                assertNotNull(tela.botaoTecla(a), a.name());
                assertEquals(MapaControles.padrao().rotulo(SlotJogador.JOGADOR_1, a), tela.botaoTecla(a).getText());
            }
            assertEquals("2", tela.botaoTecla(AcaoJogador.INICIAR_PEGADA).getText());   // 5O.1 (pedido): H -> 2
            tela.selecionarSlot(SlotJogador.JOGADOR_2);
            assertEquals("U", tela.botaoTecla(AcaoJogador.ROMPIMENTO_TECNICO).getText());
            assertEquals("←", tela.botaoTecla(AcaoJogador.MOVER_ESQUERDA).getText());
            // 5I (pedido): antes null ("J2 nao tem pegada").
            assertEquals("END", tela.botaoTecla(AcaoJogador.INICIAR_PEGADA).getText());
        });
    }

    @Test
    void clicarEApertarRemapeiaEsoGravaAoSalvar() throws Exception {
        abrir();
        naEdt(() -> {
            tela.botaoTecla(AcaoJogador.INICIAR_PEGADA).doClick();
            assertTrue(tela.isCapturando());
            assertEquals("pressione uma tecla…", tela.botaoTecla(AcaoJogador.INICIAR_PEGADA).getText());
            // 5O.1 (pedido): P e do teclado-controle do J2 (recusada); a tecla livre usada e a VIRGULA.
            tela.receberTecla(KeyEvent.VK_COMMA);
            assertFalse(tela.isCapturando());
            assertEquals(",", tecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA));
            assertEquals("2", prefs.getAtual().rotulo(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA),
                    "nada vale antes de SALVAR");
            assertFalse(Files.exists(repo().getArquivo()));
            assertTrue(tela.salvar());
            assertEquals(",", prefs.getAtual().rotulo(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA));
        });
        assertEquals(",", PreferenciasControles.carregar(repo()).getAtual()
                .rotulo(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA), "persistido");
    }

    @Test
    void remapearOPretoNaoMexeNoBranco() throws Exception {
        abrir();
        naEdt(() -> {
            tela.selecionarSlot(SlotJogador.JOGADOR_2);
            tela.iniciarCaptura(AcaoJogador.ROMPIMENTO_TECNICO);
            tela.receberTecla(KeyEvent.VK_NUMPAD3);                   // 5O.1 (pedido): NUM 0 = correr do J1
            assertEquals("NUM 3", tecla(SlotJogador.JOGADOR_2, AcaoJogador.ROMPIMENTO_TECNICO));
            assertEquals(MapaControles.padrao().doSlot(SlotJogador.JOGADOR_1),
                    tela.mapaEmEdicao().doSlot(SlotJogador.JOGADOR_1));
        });
    }

    // 5O.1: as teclas do teclado-controle do J2 sao recusadas para o J1 (nunca a mesma tecla nos dois).
    @Test
    void teclaDoTecladoControleDoJ2RecusadaNoJ1() throws Exception {
        abrir();
        naEdt(() -> {
            for (int k : new int[] {KeyEvent.VK_W, KeyEvent.VK_P, KeyEvent.VK_SHIFT, KeyEvent.VK_UP, KeyEvent.VK_ENTER}) {
                tela.iniciarCaptura(AcaoJogador.PULAR);
                tela.receberTecla(k);
                assertTrue(tela.isCapturando(), "recusada: " + k);
                assertTrue(tela.textoStatus().contains("teclado-controle do J2"), tela.textoStatus());
                tela.receberTecla(KeyEvent.VK_ESCAPE);
            }
            assertEquals(MapaControles.padrao(), tela.mapaEmEdicao());
        });
    }

    @Test
    void escCancelaEReservadasOuNaoSuportadasSaoRecusadas() throws Exception {
        abrir();
        naEdt(() -> {
            tela.iniciarCaptura(AcaoJogador.PULAR);
            tela.receberTecla(KeyEvent.VK_F5);
            assertTrue(tela.isCapturando(), "F5 recusada, continua esperando");
            assertTrue(tela.textoStatus().contains("reservada"), tela.textoStatus());
            tela.receberTecla(KeyEvent.VK_TAB);
            assertTrue(tela.isCapturando());
            assertTrue(tela.textoStatus().contains("não é suportada"), tela.textoStatus());
            tela.receberTecla(KeyEvent.VK_ESCAPE);
            assertFalse(tela.isCapturando());
            assertEquals(MapaControles.padrao(), tela.mapaEmEdicao());
        });
    }

    @Test
    void conflitoPerguntaETrocaSoSeConfirmar() throws Exception {
        abrir();
        naEdt(() -> {
            // J1 pegada <- NUM 6 (ja e "direita" do J1): recusar a troca nao muda nada. 5O.1 (pedido): D -> NUM 6.
            dialogos.confirmacoes.add(false);
            tela.iniciarCaptura(AcaoJogador.INICIAR_PEGADA);
            tela.receberTecla(KeyEvent.VK_NUMPAD6);
            assertEquals(MapaControles.padrao(), tela.mapaEmEdicao());
            assertTrue(dialogos.perguntas.get(0).contains("J1 · Mover para a direita"), dialogos.perguntas.get(0));

            dialogos.confirmacoes.add(true);
            tela.iniciarCaptura(AcaoJogador.INICIAR_PEGADA);
            tela.receberTecla(KeyEvent.VK_NUMPAD6);
            assertEquals("NUM 6", tecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA));
            assertEquals("2", tecla(SlotJogador.JOGADOR_1, AcaoJogador.MOVER_DIREITA), "trocadas");
            assertTrue(tela.mapaEmEdicao().isValido());

            // Entre os jogadores (mesmo teclado): J2 pular <- NUM 4 (esquerda do J1).
            tela.selecionarSlot(SlotJogador.JOGADOR_2);
            dialogos.confirmacoes.add(false);
            tela.iniciarCaptura(AcaoJogador.PULAR);
            tela.receberTecla(KeyEvent.VK_NUMPAD4);
            assertTrue(dialogos.perguntas.get(2).contains("J1 · Mover para a esquerda"), dialogos.perguntas.get(2));
            assertEquals("N", tecla(SlotJogador.JOGADOR_2, AcaoJogador.PULAR));
        });
    }

    @Test
    void capturaConsomeAsTeclasDaJanela() throws Exception {
        abrir();
        naEdt(() -> {
            JButton salvar = new JButton();
            KeyEvent espaco = new KeyEvent(salvar, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_SPACE, ' ');
            KeyEvent solta = new KeyEvent(salvar, KeyEvent.KEY_RELEASED, 0L, 0, KeyEvent.VK_SPACE, ' ');
            assertFalse(tela.capturarEventoParaTestes(espaco), "sem captura, nada e consumido");
            tela.iniciarCaptura(AcaoJogador.REFORCAR_PEGADA);
            KeyEvent espaco2 = new KeyEvent(salvar, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_SPACE, ' ');
            // ESPACO ja e "pular" do J1: recusar a troca.
            dialogos.confirmacoes.add(false);
            assertTrue(tela.capturarEventoParaTestes(espaco2));
            assertTrue(espaco2.isConsumed(), "o botao em foco nao recebe o ESPACO");
            assertFalse(tela.capturarEventoParaTestes(solta), "fim da captura: teclas voltam ao normal");
        });
    }

    @Test
    void restaurarPadroesEDescartarAoVoltar() throws Exception {
        abrir();
        naEdt(() -> {
            tela.iniciarCaptura(AcaoJogador.PULAR);
            tela.receberTecla(KeyEvent.VK_COMMA);                       // 5O.1 (pedido): P -> ,
            tela.restaurarPadroes();
            assertEquals(MapaControles.padrao(), tela.mapaEmEdicao());

            tela.iniciarCaptura(AcaoJogador.PULAR);
            tela.receberTecla(KeyEvent.VK_COMMA);
            dialogos.respostas.add(Dialogos.Resposta.CANCELAR);
            tela.voltar();
            assertEquals(0, voltou.get(), "cancelar fica na tela");
            dialogos.respostas.add(Dialogos.Resposta.NAO);
            tela.voltar();
            assertEquals(1, voltou.get());
            assertEquals(MapaControles.padrao(), prefs.getAtual(), "descartado");
            assertFalse(Files.exists(repo().getArquivo()));
        });
    }

    @Test
    void voltarComSimSalva() throws Exception {
        abrir();
        naEdt(() -> {
            tela.iniciarCaptura(AcaoJogador.CORRER);
            tela.receberTecla(KeyEvent.VK_PERIOD);                      // 5O.1 (pedido): Q e do J2 (LT)
            dialogos.respostas.add(Dialogos.Resposta.SIM);
            tela.voltar();
            assertEquals(1, voltou.get());
            assertEquals(".", prefs.getAtual().rotulo(SlotJogador.JOGADOR_1, AcaoJogador.CORRER));
        });
        assertTrue(Files.exists(repo().getArquivo()));
    }

    @Test
    void cabeEm800x450ComRolagem() throws Exception {
        abrir();
        naEdt(() -> {
            tela.setSize(EstiloTelas.LARGURA, EstiloTelas.ALTURA);
            layoutRecursivo(tela);
            Rectangle tela800 = new Rectangle(0, 0, 800, 450);
            for (Component c : tela.getComponents()) {
                assertTrue(tela800.contains(c.getBounds()), c + " fora da tela: " + c.getBounds());
            }
            JScrollPane rolagem = tela.rolagem();
            assertTrue(rolagem.getHeight() >= 200, "lista com altura legivel: " + rolagem.getHeight());
            assertTrue(rolagem.getViewport().getView().getPreferredSize().height > rolagem.getViewport().getHeight(),
                    "todas as acoes do J1 nao cabem sem rolagem: a barra existe");
        });
    }

    private static void layoutRecursivo(Container c) {
        c.doLayout();
        for (Component filho : c.getComponents()) {
            if (filho instanceof Container cc) {
                layoutRecursivo(cc);
            }
        }
    }

    @Test
    void configuracoesTemOBotaoControles() throws Exception {
        ServicoAudio audio = new ServicoAudio(SaidaAudio.SILENCIOSA, c -> Optional.empty());
        PreferenciasJogo pj = PreferenciasJogo.carregar(new RepositorioConfiguracoes(new DiretorioDados(tmp)), audio);
        AtomicInteger abriu = new AtomicInteger();
        naEdt(() -> {
            ConfiguracoesPanel c = new ConfiguracoesPanel(pj, RegistroCenarios.padrao(), dialogos,
                    () -> { }, abriu::incrementAndGet);
            c.botaoControles().doClick();
            assertEquals(1, abriu.get());
            ConfiguracoesPanel sem = new ConfiguracoesPanel(pj, RegistroCenarios.padrao(), dialogos, () -> { });
            assertEquals(null, sem.botaoControles().getParent(), "sem callback, sem botao");
        });
    }
}
