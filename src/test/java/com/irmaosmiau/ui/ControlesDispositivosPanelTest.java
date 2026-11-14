package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ProvedorGamepads;
import com.irmaosmiau.dispositivos.ProvedorGamepadsAusente;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioControles;
import com.irmaosmiau.persistencia.RepositorioDispositivos;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5D: escolha de dispositivo em CONFIGURACOES -> CONTROLES. */
class ControlesDispositivosPanelTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private ControlesPanel tela;
    private PreferenciasDispositivos prefs;
    private final DialogosFalsos dialogos = new DialogosFalsos();
    private final AtomicInteger voltou = new AtomicInteger();

    private void abrir(ProvedorGamepads gamepads) throws Exception {
        DiretorioDados d = new DiretorioDados(tmp);
        prefs = PreferenciasDispositivos.carregar(new RepositorioDispositivos(d));
        PreferenciasControles controles = PreferenciasControles.carregar(new RepositorioControles(d));
        SwingUtilities.invokeAndWait(() -> tela = new ControlesPanel(controles, prefs, gamepads, dialogos,
                voltou::incrementAndGet));
    }

    @Test
    void escolherGamepadParaOJ2ESalvar() throws Exception {
        ProvedorGamepadSimulado sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID", "Xbox Controller");
        abrir(sim);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(TipoDispositivo.TECLADO, tela.seletorDispositivo().getSelectedItem());
            assertTrue(tela.textoSituacaoGamepads().contains("1 gamepad: Xbox Controller"),
                    tela.textoSituacaoGamepads());
            tela.selecionarSlot(SlotJogador.JOGADOR_2);
            tela.seletorDispositivo().setSelectedItem(TipoDispositivo.GAMEPAD);
            assertEquals(TipoDispositivo.GAMEPAD, tela.dispositivosEmEdicao().tipo(SlotJogador.JOGADOR_2));
            assertEquals(TipoDispositivo.TECLADO, tela.dispositivosEmEdicao().tipo(SlotJogador.JOGADOR_1));
            assertTrue(tela.temAlteracoesNaoSalvas());
            tela.selecionarSlot(SlotJogador.JOGADOR_1);
            assertEquals(TipoDispositivo.TECLADO, tela.seletorDispositivo().getSelectedItem(), "por jogador");
            assertTrue(tela.salvar());
        });
        assertEquals(TipoDispositivo.GAMEPAD, new RepositorioDispositivos(new DiretorioDados(tmp)).carregar()
                .tipo(SlotJogador.JOGADOR_2));
        assertTrue(Files.exists(tmp.resolve(RepositorioControles.NOME_ARQUIVO)), "SALVAR grava as duas escolhas");
    }

    @Test
    void descartarERestaurarVoltamAoTeclado() throws Exception {
        abrir(new ProvedorGamepadSimulado());
        SwingUtilities.invokeAndWait(() -> {
            tela.selecionarDispositivo(TipoDispositivo.GAMEPAD);
            tela.restaurarPadroes();
            assertEquals(AssociacaoDispositivos.padrao(), tela.dispositivosEmEdicao());
            tela.selecionarDispositivo(TipoDispositivo.GAMEPAD);
            dialogos.respostas.add(Dialogos.Resposta.NAO);
            tela.voltar();
            assertEquals(1, voltou.get());
            assertEquals(AssociacaoDispositivos.padrao(), prefs.getAtual());
        });
        assertTrue(!Files.exists(tmp.resolve(RepositorioDispositivos.NOME_ARQUIVO)), "nada gravado");
    }

    @Test
    void semSuporteMostraOMotivoECabeEm800x450() throws Exception {
        abrir(new ProvedorGamepadsAusente("biblioteca LWJGL não encontrada"));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(tela.textoSituacaoGamepads().contains("indisponível"), tela.textoSituacaoGamepads());
            tela.setSize(800, 450);
            layout(tela);
            Rectangle r = new Rectangle(0, 0, 800, 450);
            for (Component c : tela.getComponents()) {
                assertTrue(r.contains(c.getBounds()), c.getBounds().toString());
            }
            assertTrue(tela.rolagem().getHeight() >= 180, "lista legivel: " + tela.rolagem().getHeight());
            Rectangle combo = SwingUtilities.convertRectangle(tela.seletorDispositivo().getParent(),
                    tela.seletorDispositivo().getBounds(), tela);
            assertTrue(r.contains(combo) && combo.width > 80, combo.toString());
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
