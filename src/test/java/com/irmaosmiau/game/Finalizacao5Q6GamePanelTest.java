package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.6: o fluxo COMPLETO das finalizacoes pelo GamePanel real com os dois
 * jogadores no gamepad (simulado): BATER pelo item da roda (RS esquerda), pelo RB + RS
 * baixo e com o gatilho ainda segurado, em qualquer pressao, com J1 ou J2, terminando
 * a luta; defender com modificador e escapar pela janela; soltar com o LT segurado.
 */
class Finalizacao5Q6GamePanelTest {

    private GamePanel painel;
    private ProvedorGamepadSimulado sim;

    @AfterEach
    void restaurar() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
    }

    private void criar() {
        sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID0", "Xbox 1");
        sim.conectar("g2", "ID1", "Xbox 2");
        AssociacaoDispositivos a = AssociacaoDispositivos.padrao()
                .com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD)
                .com(SlotJogador.JOGADOR_2, TipoDispositivo.GAMEPAD);
        MapaGamepad m = MapaGamepad.classico().comEsquema(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL)
                .comEsquema(SlotJogador.JOGADOR_2, EsquemaGamepad.CONTEXTUAL);
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(a));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(m));
        painel = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        painel.getLutador1().setGraduacao(com.irmaosmiau.regras.Graduacao.PRETA);
        painel.getLutador2().setGraduacao(com.irmaosmiau.regras.Graduacao.PRETA);
        ticks(1);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private static String g(Lado l) {
        return l == Lado.A ? "g1" : "g2";
    }

    private static EstadoGamepad r() {
        return EstadoGamepad.repouso();
    }

    /** Coloca {@code atk} na posicao e comeca a primeira finalizacao disponivel. */
    private void finalizacaoDe(Lado atk, Posicao p, Lado porCima, boolean ateDisputa) {
        m().forcarPosicao(p, porCima, atk);
        ticks(40);
        m().definirEstabilidade(90);
        for (Familia f : new Familia[] {Familia.L2, Familia.L1, Familia.L1_L2}) {
            for (Direcao d : Direcao.values()) {
                if (m().getAtaque().isEmpty()) {
                    m().comando(atk, new ComandoSolo(d, f));
                }
            }
        }
        assertTrue(m().getAtaque().isPresent(), "uma finalizacao comecou em " + p);
        for (int i = 0; ateDisputa && i < 300 && m().getAtaque().orElseThrow().fase()
                != MotorSolo.FaseFinalizacao.DISPUTA; i++) {
            ticks(1);
        }
    }

    private void apertar(Lado l, EstadoGamepad e) {
        sim.definir(g(l), e);
        ticks(2);
        sim.definir(g(l), r());
        ticks(1);
    }

    @Test
    void baterPeloItemDaRodaTerminaALutaNaHoraComJ1EJ2() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (Lado atk : Lado.values()) {
                criar();
                finalizacaoDe(atk, Posicao.COSTAS, atk, false);     // ainda na ENTRADA (pressao 0)
                apertar(atk.outro(), r().comEixo(EixoGamepad.DIR_X, -1f));    // RS esquerda: BATER
                assertTrue(painel.isFimDeJogo(), "bateu na entrada: fim de luta (" + atk + ")");
                assertTrue(painel.getMensagemFim().contains("BATEU"), painel.getMensagemFim());
                assertTrue(painel.getMensagemFim().startsWith(atk == Lado.A ? "J1" : "J2"));
            }
        });
    }

    @Test
    void baterComRbERsBaixoEComOGatilhoSeguradoEmOutrasFinalizacoes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            criar();
            finalizacaoDe(Lado.A, Posicao.MONTADA, Lado.A, true);
            assertTrue(m().getAtaque().orElseThrow().pressao() < 100, "longe dos 100%");
            apertar(Lado.B, r().com(BotaoGamepad.RB).comEixo(EixoGamepad.DIR_Y, 1f));   // RB + RS baixo
            assertTrue(painel.isFimDeJogo(), "RB + RS baixo bate");

            criar();
            finalizacaoDe(Lado.B, Posicao.COSTAS, Lado.B, true);
            apertar(Lado.A, r().com(BotaoGamepad.GATILHO_ESQ).comEixo(EixoGamepad.DIR_X, -1f));   // LT segurado
            assertTrue(painel.isFimDeJogo(), "LT + RS esquerda tambem bate");
            assertTrue(painel.getMensagemFim().startsWith("J2"));
        });
    }

    @Test
    void defenderComModificadorEDepoisEscaparPelaJanela() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            criar();
            finalizacaoDe(Lado.A, Posicao.COSTAS, Lado.A, true);
            apertar(Lado.B, r().com(BotaoGamepad.LB).comEixo(EixoGamepad.DIR_Y, 1f));   // LB + RS baixo: defende
            MotorSolo.Ataque a = m().getAtaque().orElseThrow();
            assertTrue(a.janelaDeFuga(), "a defesa abriu a janela de fuga");
            assertEquals(65, a.limiarEscape(), 1e-9);
            assertFalse(painel.isFimDeJogo());
            ticks(MotorSolo.RECARGA_ACAO_FINALIZACAO + 1);
            apertar(Lado.B, r().comEixo(EixoGamepad.DIR_Y, -1f));                    // RS cima: escapa
            assertTrue(m().getAtaque().isEmpty(), "escapou pela janela");
            assertFalse(painel.isFimDeJogo());
        });
    }

    @Test
    void soltarComOLtSeguradoNaoTerminaALuta() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            criar();
            finalizacaoDe(Lado.A, Posicao.COSTAS, Lado.A, true);
            apertar(Lado.A, r().com(BotaoGamepad.GATILHO_ESQ).comEixo(EixoGamepad.DIR_Y, 1f));   // LT + RS baixo
            assertTrue(m().getAtaque().isEmpty(), "soltou com o gatilho segurado");
            assertFalse(painel.isFimDeJogo());
        });
    }
}
