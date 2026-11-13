package com.irmaosmiau.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;

import java.util.Map;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

/** Entrega 5C: InputHandler lendo o MapaControles (teclas fisicas -> acoes logicas). */
class InputHandlerControlesTest {

    /** Mesma tabela de MapaControlesTest (copiada: pacotes diferentes). */
    private static final Map<String, String> ANTERIORES = Map.ofEntries(
            Map.entry("brancoEsquerda", "A"), Map.entry("brancoDireita", "D"), Map.entry("brancoCima", "W"),
            Map.entry("brancoBaixo", "S"), Map.entry("brancoPular", "SPACE"), Map.entry("brancoAgachar", "C"),
            Map.entry("brancoCorrer", "Z"), Map.entry("brancoPularCurto", "V"), Map.entry("brancoRespirar", "X"),
            Map.entry("brancoRolar", "R"), Map.entry("brancoDeitar", "F"), Map.entry("brancoSprawl", "G"),
            Map.entry("brancoPegada1", "H"), Map.entry("brancoDesequilibrio", "T"),
            Map.entry("brancoReforco", "Y"), Map.entry("brancoControle", "SHIFT"),
            Map.entry("pretoEsquerda", "LEFT"), Map.entry("pretoDireita", "RIGHT"), Map.entry("pretoCima", "UP"),
            Map.entry("pretoBaixo", "DOWN"), Map.entry("pretoPular", "N"), Map.entry("pretoAgachar", "M"),
            Map.entry("pretoCorrer", "K"), Map.entry("pretoPularCurto", "B"), Map.entry("pretoRespirar", "L"),
            Map.entry("pretoRolar", "O"), Map.entry("pretoDeitar", "J"), Map.entry("pretoSprawl", "I"),
            Map.entry("pretoRompimento", "U"));

    private static TeclaFisica t(String nome) {
        return TeclaFisica.porNome(nome).orElseThrow();
    }

    private static void fisica(JPanel painel, String keystroke) {
        Object nome = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(keystroke));
        assertNotNull(nome, "nada mapeado em " + keystroke);
        painel.getActionMap().get(nome).actionPerformed(null);
    }

    @Test
    void mapeamentoPadraoIdenticoAoAnterior() {
        JPanel painel = new JPanel();
        new InputHandler(painel);
        InputMap m = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        for (Map.Entry<String, String> e : ANTERIORES.entrySet()) {
            String tecla = e.getValue();
            for (String mod : new String[]{"", "shift "}) {
                assertEquals(e.getKey() + "Pressionada", m.get(KeyStroke.getKeyStroke(mod + "pressed " + tecla)),
                        mod + tecla);
                assertEquals(e.getKey() + "Solta", m.get(KeyStroke.getKeyStroke(mod + "released " + tecla)),
                        mod + tecla);
            }
            assertNotNull(painel.getActionMap().get(e.getKey() + "Pressionada"));
        }
        assertNull(m.get(KeyStroke.getKeyStroke("pressed P")), "nenhuma tecla nova apareceu");
    }

    @Test
    void remapeamentoTrocaATeclaFisicaENaoOComando() {
        JPanel painel = new JPanel();
        MapaControles mapa = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA, t("P"))
                .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.ROMPIMENTO_TECNICO, t("NUMPAD0"));
        InputHandler input = new InputHandler(painel, mapa);
        InputMap m = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertNull(m.get(KeyStroke.getKeyStroke("pressed H")), "H ficou livre");
        assertNull(m.get(KeyStroke.getKeyStroke("pressed U")), "U ficou livre");
        assertEquals("brancoPegada1Pressionada", m.get(KeyStroke.getKeyStroke("pressed P")));
        assertEquals("pretoRompimentoPressionada", m.get(KeyStroke.getKeyStroke("pressed NUMPAD0")));

        fisica(painel, "pressed P");
        assertTrue(input.isPegada1());
        assertTrue(input.isAtiva(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA));
        assertFalse(input.isPretoRompimento());
        fisica(painel, "pressed NUMPAD0");
        assertTrue(input.isPretoRompimento());
        fisica(painel, "released P");
        assertFalse(input.isPegada1());
        assertTrue(input.isPretoRompimento(), "slots independentes");
    }

    @Test
    void modificadorConfiguradoFuncionaComQualquerCombinacao() {
        JPanel painel = new JPanel();
        MapaControles mapa = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, t("CONTROL"));
        InputHandler input = new InputHandler(painel, mapa);

        // Segurar CTRL: o Swing gera "ctrl pressed CONTROL" e depois "ctrl pressed D".
        fisica(painel, "ctrl pressed CONTROL");
        fisica(painel, "ctrl pressed D");
        assertTrue(input.isControle() && input.isDireita());
        // J2 continua funcionando com o CTRL do J1 segurado.
        fisica(painel, "ctrl pressed LEFT");
        assertTrue(input.isPretoEsquerda());
        // Soltar em qualquer ordem.
        fisica(painel, "released CONTROL");
        assertFalse(input.isControle());
        assertTrue(input.isDireita());
        fisica(painel, "released D");
        fisica(painel, "released LEFT");
        assertFalse(input.isDireita() || input.isPretoEsquerda());
        assertNull(painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke("shift pressed SHIFT")), "SHIFT nao e mais o modificador");
    }

    @Test
    void perdaDeFocoSoltaTudoInclusiveTravas() {
        JPanel painel = new JPanel();
        InputHandler input = new InputHandler(painel);
        fisica(painel, "pressed D");
        fisica(painel, "pressed LEFT");
        fisica(painel, "shift pressed SHIFT");
        input.travarComandosPressionadosPreto();
        assertTrue(input.isDireita() && input.isControle());

        input.liberarTodas();
        assertFalse(input.isDireita() || input.isControle() || input.isPretoEsquerda());
        // Depois da volta do foco, apertar de novo funciona (nenhuma trava sobrou).
        fisica(painel, "pressed LEFT");
        assertTrue(input.isPretoEsquerda());
    }

    @Test
    void travaComandosCompativelComTeclaRemapeada() {
        JPanel painel = new JPanel();
        MapaControles mapa = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MOVER_DIREITA, t("NUMPAD6"));
        InputHandler input = new InputHandler(painel, mapa);
        fisica(painel, "pressed NUMPAD6");
        assertTrue(input.isDireita());
        input.travarComandosPressionadosBranco();
        assertFalse(input.isDireita(), "segurada durante o bloqueio = solta");
        fisica(painel, "released NUMPAD6");
        fisica(painel, "pressed NUMPAD6");
        assertTrue(input.isDireita(), "soltar e apertar de novo volta a valer");
    }

    @Test
    void mapaComConflitoCaiNosPadroes() {
        JPanel painel = new JPanel();
        MapaControles ruim = MapaControles.padrao().comTecla(SlotJogador.JOGADOR_2, AcaoJogador.PULAR, t("A"));
        InputHandler input = new InputHandler(painel, ruim);
        assertEquals(MapaControles.padrao(), input.getMapa());
    }

    @Test
    void acaoSemTeclaNaoQuebraOsOutrosControles() {
        JPanel painel = new JPanel();
        MapaControles sem = MapaControles.padrao().semTecla(SlotJogador.JOGADOR_1, AcaoJogador.REFORCAR_PEGADA);
        InputHandler input = new InputHandler(painel, sem);
        assertNull(painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("pressed Y")));
        assertNotNull(painel.getActionMap().get("brancoReforcoPressionada"), "o comando continua existindo");
        fisica(painel, "pressed A");
        assertTrue(input.isEsquerda());
    }
}
