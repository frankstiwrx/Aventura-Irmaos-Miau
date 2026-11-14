package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;
import com.irmaosmiau.ui.NavegadorMenusAcesso;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5I (complemento): pausa navegavel; o botao que fecha o menu nao vira acao. */
class PausaNavegacao5IGamePanelTest {

    private GamePanel painel;

    @AfterEach
    void restaurar() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
    }

    private static void naEdt(Runnable r) throws Exception {
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

    private void pausar() {
        painel.getActionMap().get("alternarPausa").actionPerformed(null);
    }

    @Test
    void navegadorSoAgeComOJogoPausadoEBContinua() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            assertFalse(painel.aceitaNavegacao(), "em combate o gamepad e do jogo");
            assertFalse(painel.escVolta(), "ESC continua sendo a pausa");
            pausar();
            assertTrue(painel.isPausado());
            assertTrue(painel.aceitaNavegacao());
            assertSame(painel.getBotaoContinuar(), NavegadorMenusAcesso.botaoVoltar(painel), "B = continuar");
            NavegadorMenusAcesso.executar(painel, null, Comando.VOLTAR);
            assertFalse(painel.isPausado(), "B fecha a pausa sem reiniciar nada");
        });
    }

    @Test
    void teclaSeguradaAoSairDaPausaNaoDisparaPuloCurto() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(2);
            pausar();
            tecla("brancoPularCurto", true);              // segurado durante a pausa
            NavegadorMenusAcesso.executar(painel, painel.getBotaoContinuar(), Comando.CONFIRMAR);
            assertFalse(painel.isPausado());
            ticks(3);
            assertFalse(painel.getLutador1().isPulando(), "o toque que fechou o menu nao pula");
            tecla("brancoPularCurto", false);
            ticks(1);
            tecla("brancoPularCurto", true);
            ticks(1);
            tecla("brancoPularCurto", false);
            assertTrue(painel.getLutador1().isPulando(), "um toque novo funciona");
        });
    }

    @Test
    void aDoGamepadQueFechaAPausaNaoPula() throws Exception {
        ProvedorGamepadSimulado sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID0", "Xbox 1");
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD)));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(MapaGamepad.padrao()));
        naEdt(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(2);
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.INICIAR));
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertTrue(painel.isPausado(), "Start pausa");
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL));   // A no CONTINUAR
            ticks(1);
            NavegadorMenusAcesso.executar(painel, painel.getBotaoContinuar(), Comando.CONFIRMAR);
            assertFalse(painel.isPausado());
            ticks(5);
            assertFalse(painel.getLutador1().isPulando(), "o A segurado nao vira pulo curto");
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL));
            ticks(1);
            assertTrue(painel.getLutador1().isPulando(), "soltou e apertou de novo: pula");
        });
    }
}
