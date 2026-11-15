package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.combate.TipoDisputa;
import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Entrega 5D no GamePanel real com um gamepad SIMULADO: as acoes do gamepad
 * passam pelas mesmas regras do teclado (PEGADA_1, T/U/Y, arrasto, trava,
 * treino, pausa, foco, desconexao e reconexao).
 */
class GamepadGamePanelTest {

    // 5O.1 (pedido): o J2 no teclado virou TECLADO-CONTROLE (layout fixo do Xbox). Estes testes
    // cobrem as TECLAS LIVRES configuraveis, que continuam existindo (-Dirmaosmiau.teclado.j2=livre);
    // o teclado-controle tem testes proprios (TecladoControle51Test).
    @org.junit.jupiter.api.BeforeEach
    void tecladoLivre51() {
        com.irmaosmiau.controles.LayoutTecladoControle.definirParaTestes(java.util.Set.of());
    }

    @org.junit.jupiter.api.AfterEach
    void tecladoPadrao51() {
        com.irmaosmiau.controles.LayoutTecladoControle.definirParaTestes(null);
    }

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;

    private GamePanel painel;
    private ProvedorGamepadSimulado sim;

    @AfterEach
    void restaurarGlobais() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasControles.instalarGlobal(null);
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    /** Gamepads simulados conectados ANTES da partida (chaves g1, g2...). */
    private void criar(AssociacaoDispositivos assoc, MapaControles teclas, boolean treino, int gamepads) {
        sim = new ProvedorGamepadSimulado();
        for (int i = 1; i <= gamepads; i++) {
            sim.conectar("g" + i, "ID-" + i, "Controle " + i);
        }
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(assoc));
        PreferenciasControles.instalarGlobal(PreferenciasControles.emMemoria(teclas));
        painel = treino
                ? new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { })
                : new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
    }

    private static AssociacaoDispositivos gamepadNo(SlotJogador... slots) {
        AssociacaoDispositivos a = AssociacaoDispositivos.padrao();
        for (SlotJogador s : slots) {
            a = a.com(s, TipoDispositivo.GAMEPAD);
        }
        return a;
    }

    private void gp(String chave, EstadoGamepad e) {
        sim.definir(chave, e);
    }

    private static EstadoGamepad repouso() {
        return EstadoGamepad.repouso();
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private void toqueGp(String chave, BotaoGamepad b) {
        gp(chave, repouso().com(b));
        ticks(1);
        gp(chave, repouso());
        ticks(1);
    }

    private void tecla(String keystroke) {
        Object nome = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(keystroke));
        assertNotNull(nome, keystroke);
        painel.getActionMap().get(nome).actionPerformed(null);
    }

    private SistemaCombate sistema() {
        return painel.getSistemaCombate();
    }

    /** J1 no gamepad g1: anda pelo analogico ate o alcance e aperta OESTE (pegada). */
    private void estabelecerPegadaPeloGamepad() {
        gp("g1", repouso().comEixo(EixoGamepad.ESQ_X, 1f));
        for (int i = 0; i < 200 && !sistema().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
            ticks(1);
        }
        gp("g1", repouso());
        ticks(1);
        toqueGp("g1", BotaoGamepad.OESTE);
        ticks(31);
        assertTrue(sistema().isInteracaoEstabelecida(), "pegada pelo gamepad");
    }

    // =====================================================
    // TECLADO + GAMEPAD
    // =====================================================
    @Test
    void j1NoTecladoEJ2NoGamepadNaMesmaPartida() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J2), null, false, 1);
            int xb = painel.getLutador1().getX();
            int xp = painel.getLutador2().getX();
            tecla("pressed NUMPAD6");   // 5O.1 (pedido): D -> NUM 6
            gp("g1", repouso().comEixo(EixoGamepad.ESQ_X, -1f));
            ticks(10);
            tecla("released NUMPAD6");
            gp("g1", repouso());
            assertTrue(painel.getLutador1().getX() > xb, "J1 pelo teclado");
            assertTrue(painel.getLutador2().getX() < xp, "J2 pelo gamepad");

            int xp2 = painel.getLutador2().getX();
            tecla("pressed LEFT");                     // teclado do J2 desligado enquanto o gamepad manda
            ticks(10);
            tecla("released LEFT");
            assertEquals(xp2, painel.getLutador2().getX(), "nenhum comando duplicado pelo teclado");
            assertTrue(painel.montarEstadoHud().getMensagem() == null
                    || !painel.montarEstadoHud().getMensagem().contains("DESCONECTADO"));
        });
    }

    @Test
    void j1NoGamepadEJ2NoTeclado() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J1), null, false, 1);
            int xb = painel.getLutador1().getX();
            int xp = painel.getLutador2().getX();
            gp("g1", repouso().com(BotaoGamepad.DPAD_DIREITA));
            tecla("pressed LEFT");
            ticks(10);
            assertTrue(painel.getLutador1().getX() > xb);
            assertTrue(painel.getLutador2().getX() < xp);
            tecla("released LEFT");
        });
    }

    @Test
    void analogicoMoveNaHorizontalEEntreAsLinhas() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J1), null, false, 1);
            IrmaoMiau b = painel.getLutador1();
            int linha = b.getLinhaAtual();
            int y = b.getY();
            gp("g1", repouso().comEixo(EixoGamepad.ESQ_Y, 1f));         // baixo
            ticks(60);
            gp("g1", repouso());
            ticks(1);
            assertTrue(b.getY() > y, "desceu");
            assertTrue(b.getLinhaAtual() > linha, "mudou de linha");
            int linhaDepois = b.getLinhaAtual();
            gp("g1", repouso().comEixo(EixoGamepad.ESQ_Y, 0.2f));       // ruido na zona morta
            ticks(60);
            assertEquals(linhaDepois, b.getLinhaAtual(), "ruido nao move");
            assertTrue(b.getLinhaAtual() <= 4, "continua dentro das cinco linhas");
        });
    }

    // =====================================================
    // TECNICAS PELA NOVA ORIGEM
    // =====================================================
    @Test
    void pegada1PeloGamepadComOMesmoCusto() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J1), null, false, 1);
            double folego = painel.getLutador1().getFolegoAtual();
            estabelecerPegadaPeloGamepad();
            assertEquals("pegada_1", sistema().getIdAcaoInteracao());
            assertEquals(folego - 3, painel.getLutador1().getFolegoAtual(), 1.0);
            assertTrue(painel.montarEstadoHud().getEstadoInteracao().startsWith("PEGADA 1 · FIRMEZA"));
        });
    }

    @Test
    void toqueUmaVezEContinuoEnquantoSegura() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J1), null, false, 1);
            estabelecerPegadaPeloGamepad();
            ticks(300);
            double f = sistema().getFirmeza();
            gp("g1", repouso().com(BotaoGamepad.NORTE));               // reforco segurado
            ticks(100);
            gp("g1", repouso());
            ticks(1);
            assertEquals(Math.min(100, f + 25) - 101 * SistemaCombate.DESGASTE_FIRMEZA_POR_TICK,
                    sistema().getFirmeza(), 0.2, "um unico reforco por toque");

            gp("g1", repouso().com(BotaoGamepad.RB));                  // correr: continuo
            ticks(5);
            assertTrue(painel.getLutador1().isCorrendo());
            gp("g1", repouso());
            ticks(2);
            assertFalse(painel.getLutador1().isCorrendo());
        });
    }

    @Test
    void desequilibrioReforcoERompimentoComAsMesmasRegras() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J1, J2), null, false, 2);
            estabelecerPegadaPeloGamepad();
            toqueGp("g1", BotaoGamepad.LESTE);                         // J1: desequilibrar
            assertEquals(TipoDisputa.DESEQUILIBRIO, sistema().getUltimaDisputa().getTipo());
            assertTrue(painel.getLutador2().getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
            ticks(200);
            toqueGp("g1", BotaoGamepad.NORTE);
            assertEquals("PEGADA REFORÇADA", painel.montarEstadoHud().getMensagem());
            toqueGp("g2", BotaoGamepad.LESTE);                         // J2: romper (mesmo botao, outro papel)
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, sistema().getUltimaDisputa().getTipo());
        });
    }

    @Test
    void arrastoConjuntoComModificadorLogicoDoGamepad() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J1), null, false, 1);
            estabelecerPegadaPeloGamepad();
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            int xb = b.getX();
            int distancia = p.getX() - b.getX();
            gp("g1", repouso().com(BotaoGamepad.LB).comEixo(EixoGamepad.ESQ_X, 1f));
            ticks(20);
            assertEquals("DESLOCAMENTO CONTROLADO", painel.montarEstadoHud().getMensagem());
            gp("g1", repouso());
            ticks(1);
            assertEquals(xb + 20, b.getX());
            assertEquals(distancia, p.getX() - b.getX());
            assertTrue(sistema().isInteracaoEstabelecida());
            gp("g1", repouso().comEixo(EixoGamepad.ESQ_X, 1f));        // sem o modificador: rompe
            ticks(1);
            assertEquals(MotivoEncerramentoInteracao.MOVIMENTO, sistema().getUltimoMotivoEncerramento());
        });
    }

    @Test
    void travaDaEntradaValeParaOGamepad() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J1), null, false, 1);
            gp("g1", repouso().comEixo(EixoGamepad.ESQ_X, 1f));
            for (int i = 0; i < 200 && !sistema().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
                ticks(1);
            }
            gp("g1", repouso().comEixo(EixoGamepad.ESQ_X, 1f).com(BotaoGamepad.OESTE));
            ticks(1);
            gp("g1", repouso().comEixo(EixoGamepad.ESQ_X, 1f));       // continua segurando a direita
            ticks(40);
            assertTrue(sistema().isInteracaoEstabelecida(), "ninguem sai andando sozinho");
        });
    }

    // =====================================================
    // CICLO DE VIDA
    // =====================================================
    @Test
    void startPausaEPerdaDeFoco() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J1), null, false, 1);
            toqueGp("g1", BotaoGamepad.INICIAR);
            assertTrue(painel.isPausado());
            int x = painel.getLutador1().getX();
            gp("g1", repouso().com(BotaoGamepad.DPAD_DIREITA));
            ticks(10);
            assertEquals(x, painel.getLutador1().getX(), "pausado: nada se move");
            gp("g1", repouso().com(BotaoGamepad.DPAD_DIREITA).com(BotaoGamepad.INICIAR));
            ticks(1);
            assertFalse(painel.isPausado(), "Start despausa");
            ticks(5);
            assertTrue(painel.getLutador1().getX() > x);

            painel.simularPerdaDeFocoParaTestes();
            int x2 = painel.getLutador1().getX();
            ticks(10);
            assertEquals(x2, painel.getLutador1().getX(), "sem foco: gamepad ignorado");
            painel.simularFocoDeVoltaParaTestes();
            ticks(10);
            assertEquals(x2, painel.getLutador1().getX(), "segurado na volta: travado");
            gp("g1", repouso());
            ticks(1);
            gp("g1", repouso().com(BotaoGamepad.DPAD_DIREITA));
            ticks(5);
            assertTrue(painel.getLutador1().getX() > x2, "soltar e apertar volta a funcionar");
        });
    }

    @Test
    void desconexaoDuranteAcaoContinuaEReconexao() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J1), null, false, 1);
            gp("g1", repouso().com(BotaoGamepad.DPAD_DIREITA));
            ticks(5);
            sim.desconectar("g1");
            ticks(1);
            assertEquals("J1: GAMEPAD DESCONECTADO · teclado ativo", painel.montarEstadoHud().getMensagem());
            int x = painel.getLutador1().getX();
            ticks(20);
            assertEquals(x, painel.getLutador1().getX(), "nada continua andando depois de desconectar");
            tecla("pressed NUMPAD6");   // 5O.1 (pedido): D -> NUM 6
            ticks(3);
            tecla("released NUMPAD6");
            assertTrue(painel.getLutador1().getX() > x, "teclado assume");

            sim.conectar("g1", "ID-1", "Controle 1");
            gp("g1", repouso().com(BotaoGamepad.DPAD_ESQUERDA));       // ja segurando ao reconectar
            ticks(1);
            assertEquals("J1: GAMEPAD CONECTADO · Controle 1", painel.montarEstadoHud().getMensagem());
            int x2 = painel.getLutador1().getX();
            ticks(10);
            assertEquals(x2, painel.getLutador1().getX(), "botao antigo nao age");
            gp("g1", repouso());
            ticks(1);
            gp("g1", repouso().com(BotaoGamepad.DPAD_ESQUERDA));
            ticks(5);
            assertTrue(painel.getLutador1().getX() < x2);
        });
    }

    @Test
    void semGamepadAPartidaSegueNoTecladoComAviso() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J2), null, false, 0);
            ticks(1);
            assertEquals("J2: nenhum gamepad livre · usando o teclado", painel.montarEstadoHud().getMensagem());
            int xp = painel.getLutador2().getX();
            tecla("pressed LEFT");
            ticks(5);
            tecla("released LEFT");
            assertTrue(painel.getLutador2().getX() < xp);
        });
    }

    // =====================================================
    // TREINO, 5C E AJUDA
    // =====================================================
    @Test
    void treinoComGamepadEParceiroPassivo() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J1, J2), null, true, 2);
            assertFalse(painel.getSessaoDispositivos().usaGamepadAgora(J2), "no treino o J2 nao existe");
            estabelecerPegadaPeloGamepad();
            gp("g2", repouso().com(BotaoGamepad.LESTE));
            ticks(50);
            assertNull(sistema().getUltimaDisputa(), "parceiro nao defende sozinho nem pelo gamepad");
            painel.getActionMap().get("reiniciarTreino").actionPerformed(null);
            assertFalse(sistema().estaEmInteracao());
            assertTrue(painel.montarEstadoHud().getDicaControles().contains("F5 reiniciar"));
        });
    }

    @Test
    void tecladoRemapeadoDa5CConviveComOGamepad() throws Exception {
        naEdt(() -> {
            MapaControles remap = MapaControles.padrao()
                    .comTecla(J1, AcaoJogador.INICIAR_PEGADA, TeclaFisica.porNome("P").orElseThrow())
                    .comTecla(J1, AcaoJogador.MOVER_DIREITA, TeclaFisica.porNome("NUMPAD6").orElseThrow());
            criar(gamepadNo(J2), remap, false, 1);
            tecla("pressed NUMPAD6");
            for (int i = 0; i < 200 && !sistema().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
                ticks(1);
            }
            tecla("released NUMPAD6");
            tecla("pressed P");
            ticks(1);
            tecla("released P");
            ticks(31);
            assertTrue(sistema().isInteracaoEstabelecida(), "J1 pelo teclado remapeado");
            toqueGp("g1", BotaoGamepad.LESTE);
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, sistema().getUltimaDisputa().getTipo(), "J2 pelo gamepad");
        });
    }

    @Test
    void ajudaEFaixaMostramOsBotoesDoGamepad() throws Exception {
        naEdt(() -> {
            criar(gamepadNo(J2), null, false, 1);
            estabelecerPegadaComTeclado();
            String dica = painel.montarEstadoHud().getDicaControles();
            assertTrue(dica.contains("J1: NUM 8456") &&   // 5O.1 (pedido): WASD -> NUM 8456
                     dica.contains("J2: analógico · B/Bola romper"), dica);
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            painel.mostrarAjudaControles(true);
            PainelAjudaControles.Conteudo c = painel.montarAjudaControles();
            assertTrue(c.colunas().get(1).titulo().contains("GAMEPAD"), c.colunas().get(1).titulo());
            String j2 = c.colunas().get(1).linhas().stream()
                    .map(l -> l.texto() + "=" + l.tecla()).collect(Collectors.joining("|"));
            assertTrue(j2.contains("Romper a pegada (rompimento técnico)=B/Bola"), j2);
            assertTrue(j2.contains("Pausar / continuar=Start/Options"), j2);
            assertTrue(c.agora().contains("J2: B/Bola romper"), c.agora());
        });
    }

    private void estabelecerPegadaComTeclado() {
        tecla("pressed NUMPAD6");   // 5O.1 (pedido): D -> NUM 6
        for (int i = 0; i < 200 && !sistema().podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
            ticks(1);
        }
        tecla("released NUMPAD6");
        tecla("pressed 2");         // 5O.1 (pedido): H -> 2
        ticks(1);
        tecla("released 2");
        ticks(31);
        assertTrue(sistema().isInteracaoEstabelecida());
    }
}
