package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.CamadaComando;
import com.irmaosmiau.controles.DirecaoTecnica;
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
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Entrega 5P.2 com Xbox SIMULADO (Contextual): LB = L1 (guardas), LT = L2
 * (quedas), LT + LB = L1+L2 (especiais), RS sozinho com a pegada (tiro de
 * guarda), espacos de finalizacao reservados na montada e painel vazio discreto.
 */
class Familias5P2GamepadTest {

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;

    private GamePanel painel;
    private ProvedorGamepadSimulado sim;

    @AfterEach
    void restaurar() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasControles.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void criar() {
        sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID-1", "Xbox 1");
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                AssociacaoDispositivos.padrao().com(J1, TipoDispositivo.GAMEPAD)));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(
                MapaGamepad.classico().comEsquema(J1, EsquemaGamepad.CONTEXTUAL)));
        painel = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
    }

    private static EstadoGamepad r() {
        return EstadoGamepad.repouso();
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private void gp(EstadoGamepad e) {
        sim.definir("g1", e);
    }

    private void toque(EstadoGamepad e) {
        gp(e);
        ticks(1);
        gp(r());
        ticks(1);
    }

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void pegar() {
        gp(r().comEixo(EixoGamepad.ESQ_X, 1f));
        for (int i = 0; i < 200 && !painel.getSistemaCombate().podeTentarAcao(painel.getLutador1(),
                painel.getLutador2(), "pegada_1"); i++) {
            ticks(1);
        }
        gp(r());
        ticks(1);
        toque(r().com(BotaoGamepad.DPAD_CIMA));
        ticks(31);
        assertTrue(painel.getSistemaCombate().isInteracaoEstabelecida());
    }

    @Test
    void rsComAPegadaDaOTiroDeGuardaELbDaAsGuardas() throws Exception {
        naEdt(() -> {
            criar();
            pegar();
            ticks(20);
            toque(r().com(BotaoGamepad.DPAD_ESQUERDA));         // RS <- (Xbox)
            assertEquals(CatalogoTecnicasSolo.TIRO_GUARDA, m().getTentativa().orElseThrow().tecnica().id());
        });
        naEdt(() -> {
            criar();
            pegar();
            ticks(20);
            gp(r().com(BotaoGamepad.LB));
            ticks(1);
            gp(r().com(BotaoGamepad.LB).com(BotaoGamepad.DPAD_BAIXO));   // L1 + v = puxar guarda
            ticks(1);
            gp(r());
            ticks(1);
            assertEquals(CatalogoTecnicasSolo.PUXAR_GUARDA, m().getTentativa().orElseThrow().tecnica().id());
        });
    }

    @Test
    void ltDaAsQuedasComAPegadaCerta() throws Exception {
        naEdt(() -> {
            criar();
            pegar();                                            // PEGADA_1 (nao e cruzada/nuca nem mangas)
            ticks(20);
            gp(r().com(BotaoGamepad.GATILHO_ESQ));
            ticks(1);
            PainelDpadContextual.Quadro q = painel.montarQuadroDpad(J1);
            assertEquals(CamadaComando.DOIS, q.camada());
            assertEquals("QUADRIL", PainelDpadContextual.texto(q, DirecaoTecnica.DIREITA));
            assertFalse(q.celulas().get(DirecaoTecnica.DIREITA).disponivelAgora(), "sem cruzada/nuca: cinza");
            assertFalse(q.celulas().get(DirecaoTecnica.ESQUERDA).disponivelAgora(), "sem as mangas: cinza");
            assertTrue(q.celulas().get(DirecaoTecnica.CIMA).disponivelAgora(), "rasteira: qualquer pegada");
            gp(r().com(BotaoGamepad.GATILHO_ESQ).com(BotaoGamepad.DPAD_DIREITA));
            ticks(1);
            gp(r());
            ticks(1);
            assertTrue(m().getTentativa().isEmpty(), "projecao de quadril recusada sem a pegada certa");
            assertTrue(painel.montarEstadoHud().getMensagem().contains("CRUZADA/NUCA"),
                    painel.montarEstadoHud().getMensagem());
        });
    }

    @Test
    void ltMaisLbEmPeNaoDisparaNadaEOPainelVazioEDiscreto() throws Exception {
        naEdt(() -> {
            criar();
            ticks(2);
            gp(r().com(BotaoGamepad.GATILHO_ESQ).com(BotaoGamepad.LB));
            ticks(1);
            PainelDpadContextual.Quadro q = painel.montarQuadroDpad(J1);
            assertEquals(CamadaComando.QUATRO, q.camada());
            assertTrue(q.celulas().isEmpty(), "livre em pe: L1+L2 sem tecnica");
            gp(r().com(BotaoGamepad.GATILHO_ESQ).com(BotaoGamepad.LB).com(BotaoGamepad.DPAD_BAIXO));
            ticks(1);
            gp(r());
            ticks(1);
            assertTrue(m().getTentativa().isEmpty(), "nem puxar guarda (L1 v) nem derrubada (L2 v)");
            BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            PainelDpadContextual.desenhar(g, 10, q);
            g.dispose();
        });
    }

    @Test
    void finalizacoesReservadasNaMontadaAparecemENaoExecutam() throws Exception {
        naEdt(() -> {
            criar();
            ticks(2);
            m().forcarPosicao(Posicao.MONTADA, Lado.A, Lado.A);
            ticks(40);
            gp(r().com(BotaoGamepad.LB));
            ticks(1);
            PainelDpadContextual.Quadro q = painel.montarQuadroDpad(J1);
            assertTrue(q.nomeCamada().contains("FINALIZ."), q.nomeCamada());
            assertEquals("AMERICANA*", PainelDpadContextual.texto(q, DirecaoTecnica.CIMA), "reservada, com *");
            assertFalse(q.celulas().get(DirecaoTecnica.CIMA).isExecutavel());
            double f = painel.getLutador1().getFolegoAtual();
            gp(r().com(BotaoGamepad.LB).com(BotaoGamepad.DPAD_CIMA));
            ticks(1);
            gp(r());
            ticks(1);
            assertTrue(painel.montarEstadoHud().getMensagem().contains("FINALIZAÇÃO FUTURA"),
                    painel.montarEstadoHud().getMensagem());
            assertEquals(Posicao.MONTADA, m().getPosicao());
            assertTrue(painel.getLutador1().getFolegoAtual() >= f - 1e-9, "nada cobrado");
            gp(r().com(BotaoGamepad.GATILHO_ESQ).com(BotaoGamepad.LB));
            ticks(1);
            assertEquals("TRIÂNGULO*", PainelDpadContextual.texto(painel.montarQuadroDpad(J1), DirecaoTecnica.CIMA));
            gp(r());
            ticks(1);
            // RS sozinho na montada continua com os fundamentos
            assertEquals("ALTA", PainelDpadContextual.texto(painel.montarQuadroDpad(J1), DirecaoTecnica.CIMA));
        });
    }
}
