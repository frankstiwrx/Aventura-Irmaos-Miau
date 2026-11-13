package com.irmaosmiau.controles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

/** Entrega 5C: modelo de acoes logicas, padroes, remapeamento e conflitos. */
class MapaControlesTest {

    /** As teclas que existiam no InputHandler antes da 5C (comando -> tecla). */
    static final Map<String, String> TECLAS_ANTERIORES = new LinkedHashMap<>();

    static {
        String[][] tabela = {
            {"brancoEsquerda", "A"}, {"brancoDireita", "D"}, {"brancoCima", "W"}, {"brancoBaixo", "S"},
            {"brancoPular", "SPACE"}, {"brancoAgachar", "C"}, {"brancoCorrer", "Z"},
            {"brancoPularCurto", "V"}, {"brancoRespirar", "X"}, {"brancoRolar", "R"},
            {"brancoDeitar", "F"}, {"brancoSprawl", "G"}, {"brancoPegada1", "H"},
            {"brancoDesequilibrio", "T"}, {"brancoReforco", "Y"}, {"brancoControle", "SHIFT"},
            {"pretoEsquerda", "LEFT"}, {"pretoDireita", "RIGHT"}, {"pretoCima", "UP"}, {"pretoBaixo", "DOWN"},
            {"pretoPular", "N"}, {"pretoAgachar", "M"}, {"pretoCorrer", "K"}, {"pretoPularCurto", "B"},
            {"pretoRespirar", "L"}, {"pretoRolar", "O"}, {"pretoDeitar", "J"}, {"pretoSprawl", "I"},
            {"pretoRompimento", "U"},
        };
        for (String[] l : tabela) {
            TECLAS_ANTERIORES.put(l[0], l[1]);
        }
    }

    private static TeclaFisica t(String nome) {
        return TeclaFisica.porNome(nome).orElseThrow();
    }

    @Test
    void padraoIdenticoAosControlesAnteriores() {
        MapaControles p = MapaControles.padrao();
        Map<String, String> atual = new LinkedHashMap<>();
        for (SlotJogador s : SlotJogador.values()) {
            for (AcaoJogador a : s.acoes()) {
                atual.put(s.comando(a), p.tecla(s, a).orElseThrow().getNome());
            }
        }
        assertEquals(TECLAS_ANTERIORES, atual);
        assertTrue(p.isValido(), p.problemas().toString());
        assertTrue(p.ehPadrao());
        assertEquals(16, SlotJogador.JOGADOR_1.acoes().size());
        assertEquals(13, SlotJogador.JOGADOR_2.acoes().size());
    }

    @Test
    void acionamentoContinuoToqueEModificador() {
        assertEquals(TipoAcionamento.CONTINUA, AcaoJogador.MOVER_DIREITA.getAcionamento());
        assertEquals(TipoAcionamento.CONTINUA, AcaoJogador.CORRER.getAcionamento());
        assertEquals(TipoAcionamento.TOQUE, AcaoJogador.INICIAR_PEGADA.getAcionamento());
        assertEquals(TipoAcionamento.TOQUE, AcaoJogador.TENTAR_DESEQUILIBRIO.getAcionamento());
        assertEquals(TipoAcionamento.TOQUE, AcaoJogador.REFORCAR_PEGADA.getAcionamento());
        assertEquals(TipoAcionamento.TOQUE, AcaoJogador.ROMPIMENTO_TECNICO.getAcionamento());
        assertEquals(TipoAcionamento.MODIFICADOR, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO.getAcionamento());
    }

    @Test
    void remapearOBrancoNaoMexeNoPreto() {
        MapaControles m = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA, t("P"));
        assertEquals("P", m.tecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA).orElseThrow().getNome());
        assertEquals(MapaControles.padrao().doSlot(SlotJogador.JOGADOR_2), m.doSlot(SlotJogador.JOGADOR_2));
        assertTrue(m.isValido());
        assertFalse(m.ehPadrao());
        assertEquals("H", MapaControles.padrao().tecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA)
                .orElseThrow().getNome(), "imutavel");
    }

    @Test
    void remapearOPretoNaoMexeNoBranco() {
        MapaControles m = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.ROMPIMENTO_TECNICO, t("NUMPAD0"))
                .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.MOVER_ESQUERDA, t("COMMA"));
        assertEquals(MapaControles.padrao().doSlot(SlotJogador.JOGADOR_1), m.doSlot(SlotJogador.JOGADOR_1));
        assertEquals("NUM 0", m.rotulo(SlotJogador.JOGADOR_2, AcaoJogador.ROMPIMENTO_TECNICO));
        assertTrue(m.isValido());
    }

    @Test
    void slotsIndependentesEAcoesQueOSlotNaoUsa() {
        assertThrows(IllegalArgumentException.class, () -> MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.INICIAR_PEGADA, t("P")));
        assertThrows(IllegalArgumentException.class, () -> MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.ROMPIMENTO_TECNICO, t("P")));
        MapaControles m = MapaControles.padrao().comPadroesDo(SlotJogador.JOGADOR_2);
        assertEquals(MapaControles.padrao(), m);
    }

    @Test
    void conflitosNoMesmoJogadorEEntreOsDois() {
        MapaControles mesmo = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA, t("D"));
        List<ProblemaControle> p1 = mesmo.problemas();
        assertEquals(1, p1.size());
        assertEquals(ProblemaControle.Tipo.TECLA_REPETIDA, p1.get(0).tipo());
        assertEquals(AcaoJogador.MOVER_DIREITA, p1.get(0).outraAcao());
        assertTrue(p1.get(0).descricao().contains("D"), p1.get(0).descricao());

        MapaControles entre = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.PULAR, t("A"));
        ProblemaControle p2 = entre.problemas().get(0);
        assertEquals(SlotJogador.JOGADOR_1, p2.outroSlot());
        assertEquals(SlotJogador.JOGADOR_2, p2.slot());
        assertFalse(entre.isValido());
    }

    @Test
    void modificadorEMovimentoNaoPodemDividirATecla() {
        MapaControles m = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, t("A"));
        assertEquals(ProblemaControle.Tipo.TECLA_REPETIDA, m.problemas().get(0).tipo());
        // Qualquer tecla livre serve de modificador (nao precisa ser SHIFT).
        assertTrue(MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, t("Q")).isValido());
        assertTrue(MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, t("CONTROL")).isValido());
    }

    @Test
    void trocaAutomaticaNuncaCriaRepeticao() {
        MapaControles m = MapaControles.padrao()
                .trocandoSeNecessario(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA, t("D"));
        assertEquals("D", m.tecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA).orElseThrow().getNome());
        assertEquals("H", m.tecla(SlotJogador.JOGADOR_1, AcaoJogador.MOVER_DIREITA).orElseThrow().getNome());
        assertTrue(m.isValido());

        MapaControles entre = MapaControles.padrao()
                .trocandoSeNecessario(SlotJogador.JOGADOR_2, AcaoJogador.PULAR, t("SPACE"));
        assertEquals("N", entre.tecla(SlotJogador.JOGADOR_1, AcaoJogador.PULAR).orElseThrow().getNome());
        assertTrue(entre.isValido());
    }

    @Test
    void teclasReservadasESemTecla() {
        for (String nome : new String[]{"ESCAPE", "F3", "F4", "F5", "F9", "F10", "F1", "F12"}) {
            assertTrue(t(nome).isReservada(), nome);
            MapaControles m = MapaControles.padrao().comTecla(SlotJogador.JOGADOR_1, AcaoJogador.PULAR, t(nome));
            assertEquals(ProblemaControle.Tipo.TECLA_RESERVADA, m.problemas().get(0).tipo(), nome);
        }
        assertFalse(t("A").isReservada());
        assertTrue(TeclaFisica.porNome("TAB").isEmpty(), "TAB troca o foco no Swing");

        MapaControles sem = MapaControles.padrao().semTecla(SlotJogador.JOGADOR_1, AcaoJogador.REFORCAR_PEGADA);
        assertEquals(ProblemaControle.Tipo.SEM_TECLA, sem.problemas().get(0).tipo());
        assertEquals("—", sem.rotulo(SlotJogador.JOGADOR_1, AcaoJogador.REFORCAR_PEGADA));
    }

    @Test
    void restaurarPadroes() {
        MapaControles mexido = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.PULAR, t("P"))
                .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.PULAR, t("Q"));
        MapaControles so1 = mexido.comPadroesDo(SlotJogador.JOGADOR_1);
        assertEquals("SPACE", so1.tecla(SlotJogador.JOGADOR_1, AcaoJogador.PULAR).orElseThrow().getNome());
        assertEquals("Q", so1.tecla(SlotJogador.JOGADOR_2, AcaoJogador.PULAR).orElseThrow().getNome());
        assertEquals(MapaControles.padrao(), so1.comPadroesDo(SlotJogador.JOGADOR_2));
        assertNotEquals(MapaControles.padrao(), mexido);
    }

    @Test
    void todasAsTeclasSuportadasSaoKeyStrokesValidosEComCodigoUnico() {
        for (TeclaFisica tecla : TeclaFisica.todas()) {
            assertNotNull(KeyStroke.getKeyStroke("pressed " + tecla.getNome()), tecla.getNome());
            assertNotNull(KeyStroke.getKeyStroke("shift ctrl alt released " + tecla.getNome()), tecla.getNome());
            assertEquals(tecla.getCodigo(), KeyStroke.getKeyStroke("pressed " + tecla.getNome()).getKeyCode(),
                    tecla.getNome());
            assertEquals(tecla, TeclaFisica.porCodigo(tecla.getCodigo()).orElseThrow());
        }
    }

    @Test
    void acoesContextuaisPorPapel() {
        assertEquals(List.of(AcaoJogador.INICIAR_PEGADA),
                AcoesContextuais.disponiveis(PapelCombate.LIVRE, SlotJogador.JOGADOR_1));
        assertEquals(List.of(), AcoesContextuais.disponiveis(PapelCombate.LIVRE, SlotJogador.JOGADOR_2),
                "o Jogador 2 ainda nao aplica pegada");
        assertEquals(List.of(), AcoesContextuais.disponiveis(PapelCombate.ENTRANDO, SlotJogador.JOGADOR_1));
        assertEquals(List.of(AcaoJogador.TENTAR_DESEQUILIBRIO, AcaoJogador.REFORCAR_PEGADA,
                AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO),
                AcoesContextuais.disponiveis(PapelCombate.APLICADOR, SlotJogador.JOGADOR_1));
        assertEquals(List.of(AcaoJogador.ROMPIMENTO_TECNICO),
                AcoesContextuais.disponiveis(PapelCombate.RECEPTOR, SlotJogador.JOGADOR_2));
        assertEquals(List.of(AcaoJogador.INICIAR_PEGADA, AcaoJogador.TENTAR_DESEQUILIBRIO,
                AcaoJogador.REFORCAR_PEGADA, AcaoJogador.ROMPIMENTO_TECNICO),
                AcoesContextuais.ACOES_DE_COMBATE, "ordem de despacho da 5B");
    }

    @Test
    void detectorDeToquesDisparaUmaVezPorPressionamento() {
        DetectorToques d = new DetectorToques();
        SlotJogador s = SlotJogador.JOGADOR_1;
        assertTrue(d.tocou(s, AcaoJogador.REFORCAR_PEGADA, true));
        for (int i = 0; i < 50; i++) {
            assertFalse(d.tocou(s, AcaoJogador.REFORCAR_PEGADA, true), "segurar nao repete");
        }
        assertFalse(d.tocou(s, AcaoJogador.REFORCAR_PEGADA, false));
        assertTrue(d.tocou(s, AcaoJogador.REFORCAR_PEGADA, true), "soltar e apertar de novo dispara");
        assertTrue(d.tocou(SlotJogador.JOGADOR_2, AcaoJogador.REFORCAR_PEGADA, true), "slots independentes");
        d.sincronizar(s, AcaoJogador.TENTAR_DESEQUILIBRIO, true);
        assertFalse(d.tocou(s, AcaoJogador.TENTAR_DESEQUILIBRIO, true), "ja segurado ao sincronizar");
    }
}
