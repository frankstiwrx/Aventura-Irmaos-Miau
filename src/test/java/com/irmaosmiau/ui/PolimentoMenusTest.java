package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.audio.SaidaAudio;
import com.irmaosmiau.audio.ServicoAudio;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.PapelCombate;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.hud.AjudaControles;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioConfiguracoes;
import com.irmaosmiau.preferencias.PreferenciasJogo;

import java.awt.Component;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Polimento dos menus: descricoes do menu principal, CONFIGURACOES navegavel linha a linha e a faixa de dicas. */
class PolimentoMenusTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    @Test
    void menuPrincipalTemUmaDescricaoPorOpcaoReal() throws Exception {
        assertEquals(MenuPrincipalPanel.OPCOES.size(), MenuPrincipalPanel.DESCRICOES.size());
        assertTrue(MenuPrincipalPanel.DESCRICOES.stream().allMatch(d -> !d.isBlank() && d.length() < 70));
        naEdt(() -> {
            MenuPrincipalPanel m = new MenuPrincipalPanel(() -> { }, () -> { }, () -> { }, () -> { }, () -> { });
            assertEquals(MenuPrincipalPanel.DESCRICOES.get(0), m.descricaoEmFoco(), "sem foco: a primeira");
        });
    }

    private ConfiguracoesPanel configuracoes(AtomicInteger voltou, AtomicInteger controles) {
        PreferenciasJogo pj = PreferenciasJogo.carregar(new RepositorioConfiguracoes(new DiretorioDados(tmp)),
                new ServicoAudio(SaidaAudio.SILENCIOSA, c -> Optional.empty()));
        ConfiguracoesPanel c = new ConfiguracoesPanel(pj, RegistroCenarios.padrao(), new DialogosFalsos(),
                voltou::incrementAndGet, controles::incrementAndGet);
        c.setSize(800, 450);
        layoutTudo(c);
        return c;
    }

    private static void layoutTudo(java.awt.Container c) {
        c.doLayout();
        for (Component k : c.getComponents()) {
            if (k instanceof java.awt.Container cc) {
                layoutTudo(cc);
            }
        }
    }

    @Test
    void configuracoesUmaOpcaoPorLinhaNaOrdemDoControle() throws Exception {
        AtomicInteger voltou = new AtomicInteger();
        AtomicInteger controles = new AtomicInteger();
        naEdt(() -> {
            ConfiguracoesPanel c = configuracoes(voltou, controles);
            List<Component> ordem = NavegadorMenus.navegaveis(c, false);
            assertEquals(List.of(c.controleMusica(), c.controleVolumeMusica(), c.controleEfeitos(),
                    c.controleVolumeEfeitos(), c.botaoControles()), ordem.subList(0, 5),
                    "cima/baixo anda de linha em linha, sem pular");
            assertTrue(ordem.contains(c.botaoVoltar()));
            // baixo: da musica para o volume da musica; lados no volume ajustam (sem mudar o foco)
            assertEquals(c.controleVolumeMusica(),
                    NavegadorMenus.executar(c, c.controleMusica(), Comando.PROXIMO));
            int v = c.controleVolumeMusica().getValue();
            assertNull(NavegadorMenus.executar(c, c.controleVolumeMusica(), Comando.DIREITA));
            assertEquals(v + 5, c.controleVolumeMusica().getValue());
            // A/ENTER liga e desliga
            boolean ligada = c.controleMusica().isSelected();
            NavegadorMenus.executar(c, c.controleMusica(), Comando.CONFIRMAR);
            assertEquals(!ligada, c.controleMusica().isSelected());
            NavegadorMenus.executar(c, c.controleMusica(), Comando.CONFIRMAR);
            // o botao CONTROLES abre a tela de controles (salvo antes: com mudancas, a tela pergunta, como sempre)
            assertTrue(c.salvar());
            NavegadorMenus.executar(c, c.botaoControles(), Comando.CONFIRMAR);
            assertEquals(1, controles.get());
            // a caixa de ligar mostra a borda de foco do navegador
            assertTrue(c.controleMusica().isBorderPainted());
        });
    }

    @Test
    void configuracoesCabemComFolgaEm800x450() throws Exception {
        naEdt(() -> {
            ConfiguracoesPanel c = configuracoes(new AtomicInteger(), new AtomicInteger());
            for (Component k : NavegadorMenus.navegaveis(c, true)) {
                java.awt.Point p = SwingUtilities.convertPoint(k.getParent(), k.getLocation(), c);
                assertTrue(p.y >= 40 && p.y + k.getHeight() <= 410 && p.x >= 40 && p.x + k.getWidth() <= 760,
                        "dentro do cartao compacto: " + k);
            }
        });
    }

    @Test
    void faixaDeDicasDoTreinoTemOF11ECabe() {
        String treino = AjudaControles.dicaHud(AjudaControles.Modo.TREINO, MapaControles.padrao(),
                PapelCombate.LIVRE, PapelCombate.LIVRE);
        assertTrue(treino.contains("F11 alternar Miau"), treino);
        String jogar = AjudaControles.dicaHud(AjudaControles.Modo.DOIS_JOGADORES, MapaControles.padrao(),
                PapelCombate.LIVRE, PapelCombate.LIVRE);
        assertFalse(jogar.contains("F11"), "o F11 so existe no treino");
        java.awt.FontMetrics fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
                .getFontMetrics(new Font("Arial", Font.PLAIN, 11));
        assertTrue(fm.stringWidth(treino.replace(AjudaControles.GRUPO, "  |  ")) < 784, treino);
    }
}
