package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.combate.TipoDisputa;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.ComandoGamepad;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5H no GamePanel real: tecnicas pelo analogico direito e arrasto automatico (gamepad simulado). */
class AnalogicoDireitoGamePanelTest {

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;

    private GamePanel painel;
    private ProvedorGamepadSimulado sim;

    @AfterEach
    void restaurar() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void criar(MapaGamepad mapa, SlotJogador... noGamepad) {
        sim = new ProvedorGamepadSimulado();
        AssociacaoDispositivos a = AssociacaoDispositivos.padrao();
        for (int i = 0; i < noGamepad.length; i++) {
            sim.conectar("g" + (i + 1), "ID" + i, "Xbox " + (i + 1));
            a = a.com(noGamepad[i], TipoDispositivo.GAMEPAD);
        }
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(a));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(mapa));
        painel = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        ticks(1);
    }

    private static MapaGamepad contextual(SlotJogador... s) {
        MapaGamepad m = MapaGamepad.padrao();
        for (SlotJogador x : s) {
            m = m.comEsquema(x, EsquemaGamepad.CONTEXTUAL);
        }
        return m;
    }

    private static EstadoGamepad r() {
        return EstadoGamepad.repouso();
    }

    private static EstadoGamepad andar(float x) {
        return r().comEixo(EixoGamepad.ESQ_X, x);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private SistemaCombate s() {
        return painel.getSistemaCombate();
    }

    /** Anda com o analogico ESQUERDO e, no alcance, pega com o DIREITO para cima (sem soltar o esquerdo). */
    private void andarEPegarComOsDoisAnalogicos() {
        sim.definir("g1", andar(1f));
        for (int i = 0; i < 200 && !s().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
            ticks(1);
        }
        sim.definir("g1", andar(1f).comEixo(EixoGamepad.DIR_Y, -1f));    // os dois ao mesmo tempo
        ticks(1);
        sim.definir("g1", r());
        ticks(31);
        assertTrue(s().isInteracaoEstabelecida(), "PEGADA_1 pelo analogico direito");
    }

    @Test
    void tecnicasNoAnalogicoDireitoEnquantoMoveComOEsquerdo() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), J1);
            double folego = painel.getLutador1().getFolegoAtual();
            andarEPegarComOsDoisAnalogicos();
            assertEquals(folego - 3, painel.getLutador1().getFolegoAtual(), 1.5, "custo da PEGADA_1 inalterado");
            sim.definir("g1", r().comEixo(EixoGamepad.DIR_X, 1f));      // RS direita: desequilibrar
            ticks(1);
            assertEquals(TipoDisputa.DESEQUILIBRIO, s().getUltimaDisputa().getTipo());
            sim.definir("g1", r().comEixo(EixoGamepad.DIR_X, 1f));
            ticks(80);                                                  // segurado: sem repeticao
            assertEquals(0, s().getRecargaRestante(painel.getLutador1()), "uma execucao so");
        });
    }

    @Test
    void arrastoAutomaticoSemLbComLimitesELinhas() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), J1);
            andarEPegarComOsDoisAnalogicos();
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            int xb = b.getX();
            int distancia = p.getX() - b.getX();
            int lb = b.getLinhaAtual();
            int lp = p.getLinhaAtual();
            sim.definir("g1", andar(1f));                               // SO o analogico esquerdo
            ticks(30);
            assertEquals(xb + 30, b.getX(), "velocidade reduzida do arrasto 5B (1 px/tick)");
            assertEquals(distancia, p.getX() - b.getX(), "a dupla anda junta");
            assertTrue(s().isInteracaoEstabelecida());
            assertEquals("DESLOCAMENTO CONTROLADO", painel.montarEstadoHud().getMensagem());
            for (int i = 0; i < 400 && p.getX() < GamePanel.LIMITE_DIREITO_ARENA; i++) {
                ticks(1);
            }
            ticks(20);                                                  // empurrando contra a borda
            assertEquals(GamePanel.LIMITE_DIREITO_ARENA, p.getX(), "limite do dojo");
            assertTrue(s().isInteracaoEstabelecida(), "firmeza ainda segura (o arrasto gasta mais, regra 5B)");
            assertEquals(lb, b.getLinhaAtual());
            assertEquals(lp, p.getLinhaAtual(), "linhas de profundidade preservadas");
            sim.definir("g1", r());
            ticks(1);
            // 5N (pedido): antes, mudar de linha rompia (3A: MOVIMENTO). Agora o aplicador conduz a
            // dupla para a outra linha junto, e a pegada continua (arrasto multidirecional).
            sim.definir("g1", r().comEixo(EixoGamepad.ESQ_Y, 1f));
            ticks(25);
            assertTrue(s().isInteracaoEstabelecida(), "5N: mudanca conjunta de linha nao rompe");
            assertEquals(b.getLinhaAtual(), p.getLinhaAtual(), "5N: os dois na mesma linha");
            assertTrue(b.getLinhaAtual() > lb, "5N: a dupla foi para a frente");
        });
    }

    @Test
    void depoisDoRompimentoAndaSozinho() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), J1);
            andarEPegarComOsDoisAnalogicos();
            s().encerrarInteracao();
            IrmaoMiau p = painel.getLutador2();
            int xp = p.getX();
            int xb = painel.getLutador1().getX();
            sim.definir("g1", andar(-1f));
            ticks(10);
            assertTrue(painel.getLutador1().getX() < xb - 10, "caminhada normal");
            assertEquals(xp, p.getX(), "sem pegada nao arrasta ninguem");
        });
    }

    @Test
    void receptorNaoArrastaOAplicadorEJ2Independente() throws Exception {
        naEdt(() -> {
            criar(contextual(J2), J2);                                  // J1 teclado, J2 Xbox contextual
            tecla("pressed NUMPAD6");   // 5O.1 (pedido): D -> NUM 6
            for (int i = 0; i < 200 && !s().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
                ticks(1);
            }
            tecla("released NUMPAD6");
            tecla("pressed 2");         // 5O.1 (pedido): H -> 2
            ticks(1);
            tecla("released 2");
            ticks(31);
            assertTrue(s().isInteracaoEstabelecida());
            int xb = painel.getLutador1().getX();
            sim.definir("g1", andar(1f));                               // receptor anda
            ticks(1);
            // 5L (pedido): antes "o receptor andando rompe" (era a fuga gratuita reprovada no Xbox).
            assertTrue(s().isInteracaoEstabelecida(), "o receptor andando so resiste: a pegada continua");
            assertEquals(xb, painel.getLutador1().getX(), "o aplicador nao foi levado");
        });
    }

    private void tecla(String ks) {
        Object nome = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(ks));
        painel.getActionMap().get(nome).actionPerformed(null);
    }

    @Test
    void doisJogadoresNoContextualComOsAnalogicos() throws Exception {
        naEdt(() -> {
            criar(contextual(J1, J2), J1, J2);
            andarEPegarComOsDoisAnalogicos();
            sim.definir("g2", r().comEixo(EixoGamepad.DIR_Y, 1f));      // J2 RS baixo: romper
            ticks(1);
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, s().getUltimaDisputa().getTipo());
        });
    }

    @Test
    void classicoIntactoESemArrastoAutomatico() throws Exception {
        naEdt(() -> {
            criar(MapaGamepad.padrao(), J1);
            sim.definir("g1", r().com(BotaoGamepad.DPAD_DIREITA));
            for (int i = 0; i < 200 && !s().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
                ticks(1);
            }
            sim.definir("g1", r());
            ticks(1);
            sim.definir("g1", r().com(BotaoGamepad.OESTE));
            ticks(1);
            sim.definir("g1", r().comEixo(EixoGamepad.DIR_Y, -1f));      // RS no Classico: nada
            ticks(31);
            assertTrue(s().isInteracaoEstabelecida());
            int xb = painel.getLutador1().getX();
            sim.definir("g1", r().com(BotaoGamepad.LB).comEixo(EixoGamepad.ESQ_X, 1f));
            ticks(10);
            assertEquals(xb + 10, painel.getLutador1().getX(), "LB + analogico continua arrastando no Classico");
            sim.definir("g1", andar(1f));
            ticks(1);
            assertEquals(MotivoEncerramentoInteracao.MOVIMENTO, s().getUltimoMotivoEncerramento(),
                    "sem LB, andar rompe como na 5B");
        });
    }

    @Test
    void mapeamentoPersonalizadoPreservadoEAnalogicoDireitoTambemVale() throws Exception {
        naEdt(() -> {
            MapaGamepad meu = contextual(J1).comBotao(J1, ItemGamepad.de(ComandoGamepad.TECNICA_CIMA), BotaoGamepad.NORTE);
            criar(meu, J1);
            assertEquals(BotaoGamepad.NORTE, PreferenciasGamepad.global().getAtual()
                    .botao(J1, ItemGamepad.de(ComandoGamepad.TECNICA_CIMA)).orElseThrow());
            sim.definir("g1", andar(1f));
            for (int i = 0; i < 200 && !s().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
                ticks(1);
            }
            sim.definir("g1", r().com(BotaoGamepad.NORTE));            // botao personalizado
            ticks(32);
            assertTrue(s().isInteracaoEstabelecida(), "Y personalizado = tecnica para cima");
        });
    }

    @Test
    void r3RolaPelaDirecaoDoEsquerdoSemTecnica() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), J1);
            sim.definir("g1", andar(-1f).comEixo(EixoGamepad.DIR_Y, -0.7f));
            ticks(1);
            sim.definir("g1", r());
            ticks(2);
            sim.definir("g1", andar(-1f));
            ticks(1);
            sim.definir("g1", andar(-1f).com(BotaoGamepad.R3).comEixo(EixoGamepad.DIR_Y, -0.9f));
            ticks(1);
            // 5O.1 (pedido): em pe, R3 + direcao = acrobacia (de joelhos/deitado = rolamento).
            assertTrue(painel.getLutador1().isEmAcrobacia(), "R3 em pe: acrobacia na direcao do analogico esquerdo");
            assertFalse(painel.montarEstadoHud().getMensagem() != null
                    && painel.montarEstadoHud().getMensagem().startsWith("J1: nada"), "nenhuma tecnica disparada");
        });
    }
}
