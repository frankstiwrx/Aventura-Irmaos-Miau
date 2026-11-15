package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5L.1: HUD de comandos do J2 (teclado) igual ao do J1 (Xbox Contextual). */
class HudJ25L1GamePanelTest {

    private GamePanel painel;

    @AfterEach
    void restaurar() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private void toque(String nome) {
        painel.getActionMap().get(nome + "Pressionada").actionPerformed(null);
        ticks(1);
        painel.getActionMap().get(nome + "Solta").actionPerformed(null);
        ticks(1);
    }

    /** Configuracao de testes do usuario: J1 no Xbox (Contextual), J2 no teclado. */
    private void criar() {
        ProvedorGamepadSimulado sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID0", "Xbox 1");
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD)));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(
                MapaGamepad.padrao().comEsquema(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL)));
        painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.DE_JOELHOS, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        ticks(1);
    }

    private static String[] textos(PainelDpadContextual.Quadro q) {
        return new String[] {PainelDpadContextual.texto(q, DirecaoTecnica.CIMA),
            PainelDpadContextual.texto(q, DirecaoTecnica.DIREITA), PainelDpadContextual.texto(q, DirecaoTecnica.BAIXO),
            PainelDpadContextual.texto(q, DirecaoTecnica.ESQUERDA)};
    }

    @Test
    void j2NoTecladoVeAMesmaCruzDoJ1() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            criar();
            PainelDpadContextual.Quadro q1 = painel.montarQuadroDpad(SlotJogador.JOGADOR_1);
            PainelDpadContextual.Quadro q2 = painel.montarQuadroDpad(SlotJogador.JOGADOR_2);
            assertNotNull(q1, "J1 no Xbox: como antes");
            assertNotNull(q2, "J2 no teclado: agora tambem");
            assertEquals("J2", q2.sigla());
            assertEquals("RS", q2.nomeCamada(), "mesmo simbolo do RS, nunca teclas");
            assertEquals(q1.nomeCamada(), q2.nomeCamada());
            String[] livre = {"PEGAR", "CRUZADA", "MANGAS", "NUCA"};
            for (int i = 0; i < 4; i++) {
                assertEquals(livre[i], textos(q1)[i], "J1 sem regressao");
                assertEquals(livre[i], textos(q2)[i], "J2 com as mesmas quatro pegadas");
            }
        });
    }

    @Test
    void cruzDoJ2MudaComOPapelNaPegada() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            criar();
            toque("pretoPegada1");                          // J2 (teclado) aplica
            ticks(40);
            assertTrue(painel.getSistemaCombate().isInteracaoEstabelecida());
            PainelDpadContextual.Quadro q2 = painel.montarQuadroDpad(SlotJogador.JOGADOR_2);
            assertEquals("REFORÇO", PainelDpadContextual.texto(q2, DirecaoTecnica.CIMA));
            assertEquals("DESEQ.", PainelDpadContextual.texto(q2, DirecaoTecnica.DIREITA));
            assertEquals("ROMPER", PainelDpadContextual.texto(painel.montarQuadroDpad(SlotJogador.JOGADOR_1),
                    DirecaoTecnica.BAIXO), "J1 receptor");
        });
    }

    @Test
    void j1NoTecladoETreinoContinuamComoAntes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.DE_JOELHOS, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(1);
            assertNull(painel.montarQuadroDpad(SlotJogador.JOGADOR_1), "J1 no teclado: sem cruz (como antes)");
            assertNotNull(painel.montarQuadroDpad(SlotJogador.JOGADOR_2));
            painel = new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(1);
            assertNull(painel.montarQuadroDpad(SlotJogador.JOGADOR_2), "no treino o J2 nao joga");
        });
    }
}
