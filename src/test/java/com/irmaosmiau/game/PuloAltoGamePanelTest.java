package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5G no GamePanel real: A = pulo normal, RB + A = pulo alto, R3 rola, L3 deita. */
class PuloAltoGamePanelTest {

    private GamePanel painel;
    private ProvedorGamepadSimulado sim;

    @AfterEach
    void restaurar() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
    }

    private void criar(EsquemaGamepad esquema) {
        sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "X", "Xbox");
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD)));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(
                MapaGamepad.padrao().comEsquema(SlotJogador.JOGADOR_1, esquema)));
        painel = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        painel.atualizarUmTickParaTestes();
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    /** Segura o estado ate pousar; devolve ticks no ar e o folego gasto no salto. */
    private int[] saltar(EstadoGamepad segurado) {
        IrmaoMiau b = painel.getLutador1();
        double antes = b.getFolegoAtual();
        sim.definir("g1", segurado);
        ticks(1);
        double gasto = antes - b.getFolegoAtual();
        int noAr = 1;
        for (int i = 0; i < 120 && b.isPulando(); i++) {
            ticks(1);
            noAr++;
        }
        ticks(30);                                           // segurando depois de pousar: sem novo salto
        assertFalse(b.isPulando(), "segurar nao repete o pulo");
        sim.definir("g1", EstadoGamepad.repouso());
        ticks(1);
        return new int[]{noAr, (int) Math.round(gasto * 10)};
    }

    @Test
    void aPulaNormalERbMaisAPulaAltoNosDoisEsquemas() throws Exception {
        for (EsquemaGamepad e : EsquemaGamepad.values()) {
            SwingUtilities.invokeAndWait(() -> {
                criar(e);
                int[] normal = saltar(EstadoGamepad.repouso().com(BotaoGamepad.SUL));
                sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.RB));
                ticks(1);
                int[] alto = saltar(EstadoGamepad.repouso().com(BotaoGamepad.RB).com(BotaoGamepad.SUL));
                assertEquals(35, normal[1], e + ": pulo normal = pularCurto (custo 3,5)");
                assertEquals(80, alto[1], e + ": pulo alto = pular (custo 8), e so ele");
                assertTrue(alto[0] > normal[0] + 5, e + ": fica mais tempo no ar (" + alto[0] + " x " + normal[0] + ")");
            });
            restaurar();
        }
    }

    @Test
    void r3RolaEL3Deita() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            criar(EsquemaGamepad.CLASSICO);
            IrmaoMiau b = painel.getLutador1();
            // Rolamento exige direcao (regra existente): analogico inclinado + R3, o caso do pedido.
            sim.definir("g1", EstadoGamepad.repouso().comEixo(com.irmaosmiau.dispositivos.EixoGamepad.ESQ_X, 1f));
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso().comEixo(com.irmaosmiau.dispositivos.EixoGamepad.ESQ_X, 1f)
                    .com(BotaoGamepad.R3));
            ticks(1);
            assertTrue(b.isRolando(), "R3 = rolamento");
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(80);
            // 5K (pedido): L3 e o comando de postura; L3 + L3 rapido = deitar direto, de novo = levantar.
            for (int vez = 0; vez < 2; vez++) {
                sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.L3));
                ticks(1);
                sim.definir("g1", EstadoGamepad.repouso());
                ticks(1);
            }
            assertTrue(b.isDeitado(), "L3 duplo = deitar");
            ticks(20);
            for (int vez = 0; vez < 2; vez++) {
                sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.L3));
                ticks(1);
                sim.definir("g1", EstadoGamepad.repouso());
                ticks(1);
            }
            assertFalse(b.isDeitado(), "L3 duplo de novo = levantar");
        });
    }
}
