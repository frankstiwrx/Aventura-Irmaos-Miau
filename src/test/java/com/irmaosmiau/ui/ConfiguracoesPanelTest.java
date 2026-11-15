package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.audio.SaidaAudio;
import com.irmaosmiau.audio.ServicoAudio;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.persistencia.ConfiguracoesJogo;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioConfiguracoes;
import com.irmaosmiau.preferencias.PreferenciasJogo;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class ConfiguracoesPanelTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private ServicoAudio audio;
    private PreferenciasJogo prefs;
    private DialogosFalsos dialogos;
    private final AtomicInteger voltou = new AtomicInteger();
    private ConfiguracoesPanel tela;

    private RepositorioConfiguracoes repo() {
        return new RepositorioConfiguracoes(new DiretorioDados(tmp));
    }

    @BeforeEach
    void preparar() {
        audio = new ServicoAudio(SaidaAudio.SILENCIOSA, c -> Optional.empty());
        dialogos = new DialogosFalsos();
    }

    private void abrir() throws Exception {
        prefs = PreferenciasJogo.carregar(repo(), audio);
        SwingUtilities.invokeAndWait(() ->
                tela = new ConfiguracoesPanel(prefs, RegistroCenarios.padrao(), dialogos, voltou::incrementAndGet));
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    @Test
    void controlesMostramAsPreferenciasCarregadas() throws Exception {
        repo().salvar(ConfiguracoesJogo.padrao().comMusicaAtiva(false).comVolumeEfeitos(0.35)
                .comCenarioPreferido("original"));
        abrir();
        naEdt(() -> {
            assertFalse(tela.controleMusica().isSelected());
            assertTrue(tela.controleEfeitos().isSelected());
            assertEquals(60, tela.controleVolumeMusica().getValue());
            assertEquals(35, tela.controleVolumeEfeitos().getValue());
            assertEquals("original", ((ConfiguracoesPanel.ItemCenario) tela.controleCenario().getSelectedItem()).id);
            assertEquals(8, tela.controleCenario().getItemCount(), "todos os cenarios cadastrados");   // 5P.4 (pedido): antes 6
        });
    }

    @Test
    void volumeMudaNaHoraESoGravaAoSalvar() throws Exception {
        abrir();
        naEdt(() -> {
            tela.controleVolumeMusica().setValue(25);
            tela.controleEfeitos().doClick(0);
        });
        assertEquals(0.25, audio.getVolumeMusica(), 1e-9, "efeito imediato no audio");
        assertFalse(audio.isEfeitosAtivos());
        assertFalse(Files.exists(repo().getArquivo()), "ainda nao confirmado");

        naEdt(() -> assertTrue(tela.salvar()));
        assertEquals(0.25, repo().carregar().getVolumeMusica(), 1e-9);
        assertFalse(repo().carregar().isEfeitosAtivos());
        assertTrue(tela.textoStatus().contains("salvas"));
    }

    @Test
    void cenarioEscolhidoEPersistido() throws Exception {
        abrir();
        naEdt(() -> {
            tela.controleCenario().setSelectedIndex(1);   // original
            tela.salvar();
        });
        assertEquals("original", repo().carregar().getCenarioPreferido());
    }

    @Test
    void restaurarPadroesVoltaAosValoresPadrao() throws Exception {
        repo().salvar(ConfiguracoesJogo.padrao().comMusicaAtiva(false).comVolumeMusica(0.05));
        abrir();
        naEdt(() -> {
            tela.restaurarPadroes();
            assertTrue(tela.controleMusica().isSelected());
            assertEquals(60, tela.controleVolumeMusica().getValue());
        });
        assertTrue(audio.isMusicaAtiva(), "padroes aplicados na hora");
        assertFalse(repo().carregar().isMusicaAtiva(), "mas so gravados ao salvar");
        naEdt(tela::salvar);
        assertEquals(ConfiguracoesJogo.padrao(), repo().carregar());
    }

    @Test
    void voltarComAlteracoesPerguntaECancelarFicaNaTela() throws Exception {
        abrir();
        naEdt(() -> tela.controleVolumeMusica().setValue(10));

        dialogos.respostas.add(Dialogos.Resposta.CANCELAR);
        naEdt(tela::voltar);
        assertEquals(0, voltou.get());

        dialogos.respostas.add(Dialogos.Resposta.NAO);
        naEdt(tela::voltar);
        assertEquals(1, voltou.get());
        assertEquals(0.6, audio.getVolumeMusica(), 1e-9, "descartar reverte o audio");
        assertFalse(Files.exists(repo().getArquivo()));
    }

    @Test
    void voltarESalvarGrava() throws Exception {
        abrir();
        naEdt(() -> tela.controleVolumeEfeitos().setValue(70));
        dialogos.respostas.add(Dialogos.Resposta.SIM);
        naEdt(tela::voltar);
        assertEquals(1, voltou.get());
        assertEquals(0.7, repo().carregar().getVolumeEfeitos(), 1e-9);
    }

    @Test
    void voltarSemAlteracoesNaoPergunta() throws Exception {
        abrir();
        naEdt(() -> tela.botaoVoltar().doClick(0));
        assertEquals(1, voltou.get());
        assertTrue(dialogos.perguntas.isEmpty());
    }

    @Test
    void cenarioSalvoInexistenteMostraDojoEAviso() throws Exception {
        repo().salvar(ConfiguracoesJogo.padrao().comCenarioPreferido("praia"));
        abrir();
        naEdt(() -> {
            assertEquals("dojo", ((ConfiguracoesPanel.ItemCenario) tela.controleCenario().getSelectedItem()).id);
            assertTrue(tela.textoStatus().contains("praia"), tela.textoStatus());
        });
        assertEquals("praia", repo().carregar().getCenarioPreferido(), "nada apagado sem o jogador salvar");
    }

    @Test
    void arquivoCorrompidoAbreComPadroesEAviso() throws Exception {
        Files.writeString(repo().getArquivo(), "{{{");
        abrir();
        naEdt(() -> assertTrue(tela.textoStatus().contains("valores padrão"), tela.textoStatus()));
        assertEquals("{{{", Files.readString(repo().getArquivo()));
    }

    @Test
    void cabeEm800x450() throws Exception {
        abrir();
        naEdt(() -> {
            tela.setSize(800, 450);
            tela.doLayout();
            validarDentro(tela, tela);
        });
    }

    /** Todo componente visivel fica inteiro dentro da tela. */
    static void validarDentro(Container raiz, Component tela) {
        raiz.validate();
        for (Component c : raiz.getComponents()) {
            if (!c.isVisible()) {
                continue;
            }
            java.awt.Point p = SwingUtilities.convertPoint(c.getParent(), c.getLocation(), tela);
            assertTrue(p.x >= 0 && p.y >= 0 && p.x + c.getWidth() <= 800 && p.y + c.getHeight() <= 450,
                    c.getClass().getSimpleName() + " fora da tela: " + p + " " + c.getSize());
            if (c instanceof Container && !(c instanceof javax.swing.JComboBox)
                    && !(c instanceof javax.swing.JSpinner) && !(c instanceof javax.swing.JScrollPane)) {
                validarDentro((Container) c, tela);
            }
        }
    }
}
