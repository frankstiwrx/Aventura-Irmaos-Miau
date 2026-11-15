package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5J (teste 20): escolha "De joelhos / Em pe" na selecao, com teclado, mouse e gamepad. */
class InicioLuta5JTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    @AfterEach
    void restaurar() {
        SelecaoLutadoresPanel.reiniciarInicioDaSessao();
    }

    private SelecaoLutadoresPanel abrir(boolean treino, AtomicInteger comecou) {
        ServicoPerfis servico = new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)),
                CatalogoPersonagens.padrao());
        return new SelecaoLutadoresPanel(new OpcoesDeLutadores(CatalogoPersonagens.padrao(), servico), treino,
                (t, b, p) -> comecou.incrementAndGet(), () -> { });
    }

    @Test
    void padraoDeJoelhosEGamepadTrocaParaEmPe() throws Exception {
        AtomicInteger comecou = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            SelecaoLutadoresPanel tela = abrir(false, comecou);
            assertEquals(InicioLuta.DE_JOELHOS, tela.getInicio(), "padrao das lutas de jiu-jitsu");
            assertTrue(NavegadorMenus.navegaveis(tela, false).contains(tela.inicio), "campo navegavel pelo D-pad");
            NavegadorMenus.executar(tela, tela.inicio, Comando.DIREITA);     // gamepad/teclado: -> muda o valor
            assertEquals(InicioLuta.EM_PE, tela.getInicio());
            assertEquals(InicioLuta.EM_PE, SelecaoLutadoresPanel.inicioDaSessao());
            NavegadorMenus.executar(tela, tela.botaoAmbos(), Comando.CONFIRMAR);
        });
        assertEquals(1, comecou.get(), "A confirma e comeca a luta");
    }

    @Test
    void escolhaLembradaNaSessaoETambemNoTreino() throws Exception {
        AtomicInteger comecou = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            SelecaoLutadoresPanel tela = abrir(false, comecou);
            tela.inicio.setSelectedItem(InicioLuta.EM_PE);                     // mouse
            SelecaoLutadoresPanel treino = abrir(true, comecou);
            assertEquals(InicioLuta.EM_PE, treino.getInicio(), "a proxima tela abre com a ultima escolha");
            assertTrue(NavegadorMenus.navegaveis(treino, false).contains(treino.inicio));
            NavegadorMenus.executar(treino, treino.inicio, Comando.ESQUERDA);
            assertEquals(InicioLuta.DE_JOELHOS, SelecaoLutadoresPanel.inicioDaSessao());
        });
    }
}
