package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5I (complemento): o que o navegador faz em cada tela (sem janela ativa). */
class NavegadorMenusTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private static JButton botao(JPanel p, String t, int x, int y) {
        JButton b = new JButton(t);
        b.setBounds(x, y, 100, 30);
        p.add(b);
        return b;
    }

    @Test
    void ordemDeLeituraMovimentoConfirmarEVoltar() throws Exception {
        naEdt(() -> {
            JPanel p = new JPanel(null);
            AtomicInteger cliques = new AtomicInteger();
            JButton b2 = botao(p, "B", 200, 50);
            JButton a = botao(p, "A", 10, 50);
            JButton c = botao(p, "C", 10, 120);
            JButton escondido = botao(p, "X", 10, 200);
            escondido.setVisible(false);
            JButton desligado = botao(p, "D", 10, 250);
            desligado.setEnabled(false);
            JButton voltar = NavegadorMenus.marcarVoltar(botao(p, "VOLTAR", 10, 300));
            voltar.addActionListener(e -> cliques.incrementAndGet());
            a.addActionListener(e -> cliques.addAndGet(10));

            assertEquals(List.of(a, b2, c, voltar), NavegadorMenus.navegaveis(p, false),
                    "linha por linha, esquerda para direita; sem escondidos nem desligados");
            assertSame(a, NavegadorMenus.executar(p, null, Comando.PROXIMO), "sem foco: o primeiro");
            assertSame(voltar, NavegadorMenus.executar(p, null, Comando.ANTERIOR));
            assertSame(b2, NavegadorMenus.executar(p, a, Comando.PROXIMO));
            assertSame(a, NavegadorMenus.executar(p, voltar, Comando.PROXIMO), "da a volta");
            assertSame(b2, NavegadorMenus.executar(p, a, Comando.DIREITA), "em botoes, direita move");

            assertNull(NavegadorMenus.executar(p, a, Comando.CONFIRMAR));
            assertEquals(10, cliques.get(), "A/ENTER clica o botao em foco");
            assertNull(NavegadorMenus.executar(p, c, Comando.VOLTAR));
            assertEquals(11, cliques.get(), "B/ESC clica o VOLTAR da tela");
            voltar.setVisible(false);
            NavegadorMenus.executar(p, c, Comando.VOLTAR);
            assertEquals(11, cliques.get(), "voltar escondido nao e acionado");
        });
    }

    @Test
    void esquerdaEDireitaMudamOValorDoCombo() throws Exception {
        naEdt(() -> {
            JPanel p = new JPanel(null);
            JComboBox<String> cb = new JComboBox<>(new String[] {"um", "dois", "tres"});
            cb.setBounds(10, 10, 100, 24);
            p.add(cb);
            assertNull(NavegadorMenus.executar(p, cb, Comando.DIREITA));
            assertEquals(1, cb.getSelectedIndex());
            NavegadorMenus.executar(p, cb, Comando.ESQUERDA);
            NavegadorMenus.executar(p, cb, Comando.ESQUERDA);
            assertEquals(2, cb.getSelectedIndex(), "da a volta");
        });
    }

    @Test
    void telaDeSelecaoTemVoltarETodosOsControles() throws Exception {
        AtomicInteger voltou = new AtomicInteger();
        naEdt(() -> {
            ServicoPerfis servico = new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)),
                    CatalogoPersonagens.padrao());
            SelecaoLutadoresPanel tela = new SelecaoLutadoresPanel(
                    new OpcoesDeLutadores(CatalogoPersonagens.padrao(), servico), false,
                    (t, b, p) -> { }, voltou::incrementAndGet);
            List<java.awt.Component> lista = NavegadorMenus.navegaveis(tela, false);
            assertTrue(lista.contains(tela.slotBranco().irmao), "seletor de irmao navegavel");
            assertTrue(lista.contains(tela.botaoAmbos()));
            // Trocar o irmao pelo gamepad: direita no seletor.
            NavegadorMenus.executar(tela, tela.slotBranco().irmao, Comando.DIREITA);
            assertEquals(CatalogoPersonagens.ID_PRETO, tela.slotBranco().idBase());
            NavegadorMenus.executar(tela, null, Comando.VOLTAR);
        });
        assertEquals(1, voltou.get(), "B volta ao menu");
    }

    @Test
    void controlesNaoNavegamDuranteACaptura() throws Exception {
        naEdt(() -> {
            ControlesPanel tela = new ControlesPanel(
                    com.irmaosmiau.preferencias.PreferenciasControles.emMemoria(
                            com.irmaosmiau.controles.MapaControles.padrao()),
                    com.irmaosmiau.preferencias.PreferenciasDispositivos.emMemoria(
                            com.irmaosmiau.dispositivos.AssociacaoDispositivos.padrao()),
                    new com.irmaosmiau.dispositivos.ProvedorGamepadSimulado(),
                    Dialogos.SWING, () -> { });
            assertTrue(tela.aceitaNavegacao());
            tela.iniciarCaptura(com.irmaosmiau.controles.AcaoJogador.PULAR);
            assertFalse(tela.aceitaNavegacao(), "A/B/setas vao para a captura, nao para o menu");
            tela.cancelarCapturaParaTestes();
            assertTrue(tela.aceitaNavegacao());
        });
    }
}
