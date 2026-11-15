package com.irmaosmiau.dispositivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.input.InputHandler;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

/** Entrega 5D: associacao de dispositivos aos slots e ciclo de vida (gamepad simulado). */
class SessaoDispositivosTest {

    // 5O.1 (pedido): o J2 no teclado virou TECLADO-CONTROLE (layout fixo do Xbox). Estes testes
    // cobrem as TECLAS LIVRES configuraveis, que continuam existindo (-Dirmaosmiau.teclado.j2=livre);
    // o teclado-controle tem testes proprios (TecladoControle51Test).
    {
        // o InputHandler deste teste e criado num campo (antes do @BeforeEach)
        com.irmaosmiau.controles.LayoutTecladoControle.definirParaTestes(java.util.Set.of());
    }

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
    private static final List<SlotJogador> OS_DOIS = List.of(J1, J2);

    private final JPanel painel = new JPanel();
    private final InputHandler input = new InputHandler(painel);
    private final ProvedorGamepadSimulado sim = new ProvedorGamepadSimulado();

    private void tecla(String keystroke) {
        Object nome = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(keystroke));
        painel.getActionMap().get(nome).actionPerformed(null);
    }

    private SessaoDispositivos sessao(AssociacaoDispositivos a, List<SlotJogador> emJogo) {
        return new SessaoDispositivos(a, sim, input, emJogo, MapaGamepad.experimental());
    }

    private static AssociacaoDispositivos com(SlotJogador s, TipoDispositivo t) {
        return AssociacaoDispositivos.padrao().com(s, t);
    }

    @Test
    void padraoSoTecladoComoAntes() {
        sim.conectar("g1", "ID-A", "Controle A");
        SessaoDispositivos s = sessao(AssociacaoDispositivos.padrao(), OS_DOIS);
        assertFalse(s.usaGamepadAgora(J1) || s.usaGamepadAgora(J2), "ninguem pega gamepad sem escolher");
        tecla("pressed LEFT");
        assertTrue(input.isPretoEsquerda());
        assertTrue(s.consumirMensagens().isEmpty());
    }

    @Test
    void j1NoTecladoEJ2NoGamepad() {
        sim.conectar("g1", "ID-A", "Controle A");
        SessaoDispositivos s = sessao(com(J2, TipoDispositivo.GAMEPAD), OS_DOIS);
        assertEquals("g1", s.gamepadDo(J2).orElseThrow().chave());
        assertEquals(List.of("J2: GAMEPAD · Controle A"), s.consumirMensagens());

        tecla("pressed NUMPAD6");                            // 5O.1 (pedido): D -> NUM 6
        tecla("pressed LEFT");                               // seta do J2: teclado do J2 desligado
        sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f));
        s.atualizar();
        assertTrue(input.isDireita(), "J1 no teclado");
        assertTrue(input.isPretoEsquerda(), "J2 pelo analogico");
        sim.definir("g1", EstadoGamepad.repouso());
        s.atualizar();
        assertFalse(input.isPretoEsquerda(), "a seta segurada nao age: um dispositivo por slot");
        assertFalse(input.isTecladoAtivo(J2));
        assertTrue(input.isTecladoAtivo(J1));
    }

    @Test
    void umGamepadNuncaControlaOsDoisJogadores() {
        sim.conectar("g1", "ID-A", "Controle A");
        AssociacaoDispositivos ambos = com(J1, TipoDispositivo.GAMEPAD).com(J2, TipoDispositivo.GAMEPAD);
        SessaoDispositivos s = sessao(ambos, OS_DOIS);
        assertEquals("g1", s.gamepadDo(J1).orElseThrow().chave());
        assertFalse(s.usaGamepadAgora(J2));
        assertTrue(s.consumirMensagens().contains("J2: nenhum gamepad livre · usando o teclado"));
        sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL));
        s.atualizar();
        assertTrue(input.isPular());
        assertFalse(input.isPretoPular(), "o mesmo gamepad nao age no J2");

        // Um segundo gamepad aparece: vai para o J2, nunca o mesmo do J1.
        sim.conectar("g2", "ID-B", "Controle B");
        s.atualizar();
        assertEquals("g2", s.gamepadDo(J2).orElseThrow().chave());
        assertNotEquals(s.gamepadDo(J1).orElseThrow().chave(), s.gamepadDo(J2).orElseThrow().chave());
    }

    @Test
    void doisGamepadsEmOrdemPrevisivel() {
        sim.conectar("g1", "ID-A", "A");
        sim.conectar("g2", "ID-B", "B");
        SessaoDispositivos s = sessao(com(J1, TipoDispositivo.GAMEPAD).com(J2, TipoDispositivo.GAMEPAD), OS_DOIS);
        assertEquals("g1", s.gamepadDo(J1).orElseThrow().chave());
        assertEquals("g2", s.gamepadDo(J2).orElseThrow().chave());
        sim.definir("g2", EstadoGamepad.repouso().com(BotaoGamepad.LESTE));
        s.atualizar();
        assertTrue(input.isPretoRompimento());
        assertFalse(input.isDesequilibrio(), "o gamepad do J2 nao aciona o J1");
    }

    @Test
    void semGamepadOSlotSegueNoTeclado() {
        SessaoDispositivos s = sessao(com(J2, TipoDispositivo.GAMEPAD), OS_DOIS);
        assertFalse(s.usaGamepadAgora(J2));
        assertEquals(List.of("J2: nenhum gamepad livre · usando o teclado"), s.consumirMensagens());
        tecla("pressed LEFT");
        s.atualizar();
        assertTrue(input.isPretoEsquerda(), "partida so com teclado continua possivel");
        s.atualizar();
        assertTrue(s.consumirMensagens().isEmpty(), "o aviso nao se repete a cada tick");
    }

    @Test
    void desconectarSoltaTudoEDevolveOTeclado() {
        sim.conectar("g1", "ID-A", "A");
        SessaoDispositivos s = sessao(com(J1, TipoDispositivo.GAMEPAD), OS_DOIS);
        s.consumirMensagens();
        sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.DPAD_DIREITA).com(BotaoGamepad.RB));
        s.atualizar();
        assertTrue(input.isDireita() && input.isCorrer());

        sim.desconectar("g1");
        s.atualizar();
        assertFalse(input.isDireita() || input.isCorrer(), "nenhuma acao continua presa");
        assertEquals(List.of("J1: GAMEPAD DESCONECTADO · teclado ativo"), s.consumirMensagens());
        assertTrue(input.isTecladoAtivo(J1));
        tecla("pressed NUMPAD4");                            // 5O.1 (pedido): A -> NUM 4
        assertTrue(input.isEsquerda(), "o jogador continua pelo teclado");
    }

    @Test
    void reconectarVoltaAoMesmoControleSemDispararBotaoSegurado() {
        sim.conectar("g1", "ID-A", "A");
        SessaoDispositivos s = sessao(com(J1, TipoDispositivo.GAMEPAD), OS_DOIS);
        sim.desconectar("g1");
        s.atualizar();
        s.consumirMensagens();

        // Volta em outra porta, com o botao de pegada ja apertado.
        sim.conectar("g7", "ID-A", "A");
        sim.definir("g7", EstadoGamepad.repouso().com(BotaoGamepad.OESTE));
        s.atualizar();
        assertEquals("g7", s.gamepadDo(J1).orElseThrow().chave());
        assertEquals(List.of("J1: GAMEPAD CONECTADO · A"), s.consumirMensagens());
        assertFalse(input.isPegada1(), "botao segurado na reconexao nao dispara");
        assertFalse(input.isTecladoAtivo(J1));
        sim.definir("g7", EstadoGamepad.repouso());
        s.atualizar();
        sim.definir("g7", EstadoGamepad.repouso().com(BotaoGamepad.OESTE));
        s.atualizar();
        assertTrue(input.isPegada1());
    }

    @Test
    void perdaDeFocoIgnoraOGamepadETravaAoVoltar() {
        sim.conectar("g1", "ID-A", "A");
        SessaoDispositivos s = sessao(com(J1, TipoDispositivo.GAMEPAD), OS_DOIS);
        sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.DPAD_DIREITA));
        s.atualizar();
        assertTrue(input.isDireita());
        s.setFocado(false);
        s.atualizar();
        assertFalse(input.isDireita(), "sem foco, gamepad ignorado");
        s.setFocado(true);
        s.atualizar();
        assertFalse(input.isDireita(), "segurado na volta do foco: travado ate soltar");
        sim.definir("g1", EstadoGamepad.repouso());
        s.atualizar();
        sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.DPAD_DIREITA));
        s.atualizar();
        assertTrue(input.isDireita());
    }

    @Test
    void startPausaUmaVezPorToque() {
        sim.conectar("g1", "ID-A", "A");
        SessaoDispositivos s = sessao(com(J1, TipoDispositivo.GAMEPAD), List.of(J1));
        sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.INICIAR));
        s.atualizar();
        assertTrue(s.consumirPausa());
        for (int i = 0; i < 10; i++) {
            s.atualizar();
        }
        assertFalse(s.consumirPausa(), "segurar o Start nao alterna de novo");
    }

    @Test
    void slotForaDeCampoNaoPegaGamepad() {
        sim.conectar("g1", "ID-A", "A");
        SessaoDispositivos s = sessao(com(J2, TipoDispositivo.GAMEPAD), List.of(J1));
        assertFalse(s.usaGamepadAgora(J2), "treino/individual: o J2 nao existe");
        assertTrue(s.consumirMensagens().isEmpty());
    }

    // =====================================================
    // PROVEDOR FISICO E AUSENCIA DE HARDWARE
    // =====================================================
    @Test
    void desligadoPorPropriedadeOJogoSegueSemGamepad() {
        String antes = System.getProperty(ServicoGamepads.PROPRIEDADE);
        System.setProperty(ServicoGamepads.PROPRIEDADE, "desligado");
        try {
            ProvedorGamepads p = ServicoGamepads.criarFisico();
            assertFalse(p.isDisponivel());
            assertTrue(p.conectados().isEmpty());
            assertTrue(p.descricao().contains("desligado"), p.descricao());
            SessaoDispositivos s = new SessaoDispositivos(com(J1, TipoDispositivo.GAMEPAD), p, input,
                    OS_DOIS, MapaGamepad.experimental());
            tecla("pressed NUMPAD6");                            // 5O.1 (pedido): D -> NUM 6
            s.atualizar();
            assertTrue(input.isDireita(), "teclado continua");
        } finally {
            if (antes == null) {
                System.clearProperty(ServicoGamepads.PROPRIEDADE);
            } else {
                System.setProperty(ServicoGamepads.PROPRIEDADE, antes);
            }
        }
    }

    @Test
    void provedorFisicoIniciaEEncerraSemHardwareNemExcecao() {
        // Sem controle conectado (ou com um): nunca lanca excecao; encerra limpo.
        ProvedorGamepads p = ServicoGamepads.criarFisico();
        try {
            List<InfoGamepad> lista = p.conectados();
            for (InfoGamepad g : lista) {
                assertFalse(g.nome().isBlank(), "gamepad real conectado: " + g);
            }
            assertTrue(p.estado("glfw-99").isEmpty());
            if (System.getProperty("os.name", "").toLowerCase().contains("windows")) {
                assertTrue(p.isDisponivel(), "GLFW deveria iniciar no Windows: " + p.descricao());
            }
        } finally {
            p.encerrar();
        }
        assertFalse(p.isDisponivel(), "thread encerrada");
        assertTrue(p.conectados().isEmpty(), "nada fica pressionado depois do fim");
    }

    @Test
    void globalPadraoEAusenteENaoIniciaNada() {
        ServicoGamepads.instalarGlobal(null);
        assertFalse(ServicoGamepads.global().isDisponivel());
        assertTrue(ServicoGamepads.global().conectados().isEmpty());
    }
}
