package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.combate.TipoDisputa;
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
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5F no GamePanel real: D-pad contextual em 3 camadas com gamepad SIMULADO. */
class ContextualGamePanelTest {

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;

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

    /** gN = gamepad do slot N (g1 do J1, g2 do J2 quando os dois usam gamepad). */
    private void criar(MapaGamepad mapa, boolean treino, SlotJogador... noGamepad) {
        sim = new ProvedorGamepadSimulado();
        AssociacaoDispositivos a = AssociacaoDispositivos.padrao();
        for (int i = 0; i < noGamepad.length; i++) {
            sim.conectar("g" + (i + 1), "ID-" + (i + 1), "Xbox " + (i + 1));
            a = a.com(noGamepad[i], TipoDispositivo.GAMEPAD);
        }
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(a));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(mapa));
        painel = treino
                ? new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { })
                : new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
    }

    private static MapaGamepad contextual(SlotJogador... slots) {
        MapaGamepad m = MapaGamepad.padrao();
        for (SlotJogador s : slots) {
            m = m.comEsquema(s, EsquemaGamepad.CONTEXTUAL);
        }
        return m;
    }

    private static EstadoGamepad r() {
        return EstadoGamepad.repouso();
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private void gp(String chave, EstadoGamepad e) {
        sim.definir(chave, e);
    }

    /** Toque: aperta neste tick, solta no proximo. */
    private void toque(String chave, EstadoGamepad e) {
        gp(chave, e);
        ticks(1);
        gp(chave, r());
        ticks(1);
    }

    private SistemaCombate s() {
        return painel.getSistemaCombate();
    }

    private void tecla(String ks) {
        Object nome = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(ks));
        painel.getActionMap().get(nome).actionPerformed(null);
    }

    /** J1 no gamepad contextual: anda pelo analogico ate o alcance e pega com D-pad cima. */
    private void pegarPeloDpad() {
        gp("g1", r().comEixo(EixoGamepad.ESQ_X, 1f));
        for (int i = 0; i < 200 && !s().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
            ticks(1);
        }
        gp("g1", r());
        ticks(1);
        toque("g1", r().com(BotaoGamepad.DPAD_CIMA));
        ticks(31);
        assertTrue(s().isInteracaoEstabelecida(), "PEGADA_1 pelo D-pad");
    }

    @Test
    void pegada1PeloDpadPreservadaECustoIgual() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), false, J1);
            double folego = painel.getLutador1().getFolegoAtual();
            int x = painel.getLutador1().getX();
            gp("g1", r().com(BotaoGamepad.DPAD_DIREITA));
            ticks(10);
            assertEquals(x, painel.getLutador1().getX(), "no contextual o D-pad nao anda");
            gp("g1", r());
            ticks(1);
            pegarPeloDpad();
            assertEquals(folego - 3, painel.getLutador1().getFolegoAtual(), 1.0);
            assertEquals("pegada_1", s().getIdAcaoInteracao());
        });
    }

    @Test
    void aplicadorDesequilibraEReforcaPeloDpad() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), false, J1);
            pegarPeloDpad();
            toque("g1", r().com(BotaoGamepad.DPAD_DIREITA));
            assertEquals(TipoDisputa.DESEQUILIBRIO, s().getUltimaDisputa().getTipo());
            assertTrue(painel.getLutador2().getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
            ticks(200);
            double f = s().getFirmeza();
            gp("g1", r().com(BotaoGamepad.DPAD_CIMA));
            ticks(100);                                      // segurar nao repete
            gp("g1", r());
            ticks(1);
            assertEquals(Math.min(100, f + 25) - 101 * SistemaCombate.DESGASTE_FIRMEZA_POR_TICK, s().getFirmeza(), 0.2);
        });
    }

    @Test
    void reservadaNaoExecutaENadaMuda() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), false, J1);
            pegarPeloDpad();
            double folego = painel.getLutador1().getFolegoAtual();
            gp("g1", r().com(BotaoGamepad.GATILHO_ESQ));
            ticks(1);
            gp("g1", r().com(BotaoGamepad.GATILHO_ESQ).com(BotaoGamepad.DPAD_CIMA));   // LT + cima = QUEDA*
            ticks(1);
            assertEquals("QUEDA: ainda não disponível no jogo", painel.montarEstadoHud().getMensagem());
            assertTrue(s().isInteracaoEstabelecida(), "nenhuma posicao de chao foi ativada");
            assertEquals(folego, painel.getLutador1().getFolegoAtual(), 0.5, "nada cobrado");
            gp("g1", r());
            ticks(1);
            gp("g1", r().com(BotaoGamepad.DPAD_ESQUERDA));                            // vazio
            ticks(1);
            assertTrue(painel.montarEstadoHud().getMensagem().startsWith("J1: nada na camada 1 ←"),  // 5H
                    painel.montarEstadoHud().getMensagem());
            assertNull(s().getUltimaDisputa());
        });
    }

    @Test
    void trocarModificadorDuranteOToqueNaoDisparaOutraTecnica() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), false, J1);
            pegarPeloDpad();
            gp("g1", r().com(BotaoGamepad.DPAD_DIREITA));                               // desequilibrio
            ticks(1);
            assertEquals(TipoDisputa.DESEQUILIBRIO, s().getUltimaDisputa().getTipo());
            ticks(60);                                                                   // fim da recarga
            gp("g1", r().com(BotaoGamepad.DPAD_DIREITA).com(BotaoGamepad.GATILHO_ESQ));
            ticks(1);
            gp("g1", r().com(BotaoGamepad.DPAD_DIREITA).com(BotaoGamepad.GATILHO_ESQ).com(BotaoGamepad.LB));
            ticks(5);
            gp("g1", r().com(BotaoGamepad.DPAD_DIREITA));
            ticks(5);
            assertEquals(0, s().getRecargaRestante(painel.getLutador1()), "nenhum golpe novo sem novo toque");
        });
    }

    @Test
    void receptorRompePeloDpadEDoisJogadoresIndependentes() throws Exception {
        naEdt(() -> {
            criar(contextual(J2), false, J2);              // J1 no teclado, J2 no Xbox contextual
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
            PainelDpadContextual.Quadro q2 = painel.montarQuadroDpad(J2);
            assertNotNull(q2);
            assertNull(painel.montarQuadroDpad(J1), "J1 no teclado: sem cruz");
            assertEquals("ROMPER", PainelDpadContextual.texto(q2, DirecaoTecnica.BAIXO));
            toque("g1", r().com(BotaoGamepad.DPAD_BAIXO));
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, s().getUltimaDisputa().getTipo());
        });
    }

    @Test
    void arrastoComLbEAnalogicoESuspensoComLt() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), false, J1);
            pegarPeloDpad();
            IrmaoMiau b = painel.getLutador1();
            int xb = b.getX();
            gp("g1", r().com(BotaoGamepad.LB).comEixo(EixoGamepad.ESQ_X, 1f));
            ticks(20);
            gp("g1", r());
            ticks(1);
            assertEquals(xb + 20, b.getX(), "arrasto continuo com LB + analogico");
            assertTrue(s().isInteracaoEstabelecida());
            // 5H (pedido): arrasto automatico no Contextual; LT + LB nao o suspendem mais.
            gp("g1", r().com(BotaoGamepad.LB).com(BotaoGamepad.GATILHO_ESQ).comEixo(EixoGamepad.ESQ_X, 1f));
            ticks(5);
            assertTrue(s().isInteracaoEstabelecida(), "continua arrastando");
            assertEquals(xb + 25, b.getX());
        });
    }

    @Test
    void hudMostraCamadaAtivaEAtualizaComOEstado() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), false, J1);
            ticks(1);
            PainelDpadContextual.Quadro q = painel.montarQuadroDpad(J1);
            assertEquals(CamadaComando.UM, q.camada());
            assertEquals("PEGAR", PainelDpadContextual.texto(q, DirecaoTecnica.CIMA));
            // 5L (pedido): livre, RS baixo = PEGADA_3 (antes vazio).
            assertEquals("MANGAS", PainelDpadContextual.texto(q, DirecaoTecnica.BAIXO));
            pegarPeloDpad();
            assertEquals("REFORÇO", PainelDpadContextual.texto(painel.montarQuadroDpad(J1), DirecaoTecnica.CIMA));
            gp("g1", r().com(BotaoGamepad.GATILHO_ESQ));
            ticks(1);
            q = painel.montarQuadroDpad(J1);
            assertEquals(CamadaComando.DOIS, q.camada());
            assertEquals("LT + RS", q.nomeCamada());                     // 5H: analogico direito
            assertEquals("QUEDA*", PainelDpadContextual.texto(q, DirecaoTecnica.CIMA));
            // 5H (pedido): LB sozinho = camada 3; LT + LB = avancada (vazia).
            gp("g1", r().com(BotaoGamepad.LB));
            ticks(1);
            q = painel.montarQuadroDpad(J1);
            assertEquals("LB + RS", q.nomeCamada());
            assertEquals("GUARDA*", PainelDpadContextual.texto(q, DirecaoTecnica.CIMA));
            gp("g1", r().com(BotaoGamepad.GATILHO_ESQ).com(BotaoGamepad.LB));
            ticks(1);
            q = painel.montarQuadroDpad(J1);
            assertEquals("LT + LB + RS", q.nomeCamada());
            assertEquals("·", PainelDpadContextual.texto(q, DirecaoTecnica.CIMA), "avancada: vazia");
            BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            painel.setSize(800, 450);
            painel.paint(g);
            g.dispose();
            assertTrue((img.getRGB(10 + PainelDpadContextual.LARGURA / 2, PainelDpadContextual.Y + 38) >>> 24) > 0);
            assertTrue(PainelDpadContextual.Y + PainelDpadContextual.ALTURA <= 133, "acima das cabecas");
        });
    }

    @Test
    void pausaFocoEDesconexaoLimpamOsComandos() throws Exception {
        naEdt(() -> {
            criar(contextual(J1), false, J1);
            // No alcance da pegada: um "pegar" vazado da pausa teria efeito visivel.
            gp("g1", r().comEixo(EixoGamepad.ESQ_X, 1f));
            for (int i = 0; i < 200 && !s().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
                ticks(1);
            }
            gp("g1", r());
            ticks(1);
            assertTrue(s().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"));
            gp("g1", r().com(BotaoGamepad.INICIAR));
            ticks(1);
            assertTrue(painel.isPausado());
            gp("g1", r().com(BotaoGamepad.DPAD_CIMA));
            ticks(3);                                        // D-pad apertado durante a pausa
            gp("g1", r().com(BotaoGamepad.DPAD_CIMA).com(BotaoGamepad.INICIAR));
            ticks(1);
            assertFalse(painel.isPausado());
            gp("g1", r().com(BotaoGamepad.DPAD_CIMA));
            ticks(5);
            assertFalse(s().estaEmInteracao(), "nada da pausa dispara depois");

            gp("g1", r());
            ticks(1);
            painel.simularPerdaDeFocoParaTestes();
            gp("g1", r().com(BotaoGamepad.DPAD_CIMA));
            ticks(3);
            painel.simularFocoDeVoltaParaTestes();
            ticks(3);
            assertFalse(s().estaEmInteracao(), "sem foco e na volta segurando: nada");

            gp("g1", r().com(BotaoGamepad.DPAD_CIMA));
            sim.desconectar("g1");
            ticks(2);
            sim.conectar("g1", "ID-1", "Xbox 1");
            gp("g1", r().com(BotaoGamepad.DPAD_CIMA));
            ticks(3);
            assertFalse(s().estaEmInteracao(), "reconectou segurando: nada");
        });
    }

    @Test
    void treinoParceiroPassivoEClassicoInalterado() throws Exception {
        naEdt(() -> {
            criar(contextual(J1, J2), true, J1, J2);
            assertNull(painel.montarQuadroDpad(J2), "no treino o J2 nao existe");
            pegarPeloDpad();
            toque("g2", r().com(BotaoGamepad.DPAD_BAIXO));
            ticks(30);
            assertNull(s().getUltimaDisputa(), "parceiro nao age sozinho nem pelo gamepad");
        });
        restaurar();
        naEdt(() -> {
            criar(MapaGamepad.padrao(), false, J1);           // classico 5D
            int x = painel.getLutador1().getX();
            gp("g1", r().com(BotaoGamepad.DPAD_DIREITA));
            ticks(5);
            assertTrue(painel.getLutador1().getX() > x, "no classico o D-pad continua andando");
            assertNull(painel.montarQuadroDpad(J1), "sem cruz no classico");
        });
    }
}
