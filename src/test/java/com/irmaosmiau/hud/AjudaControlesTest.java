package com.irmaosmiau.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.PapelCombate;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Entrega 5C: textos de ajuda gerados das teclas configuradas. */
class AjudaControlesTest {

    private static TeclaFisica t(String nome) {
        return TeclaFisica.porNome(nome).orElseThrow();
    }

    @Test
    void padroesGeramAsMesmasDicasDeAntes() {
        MapaControles p = MapaControles.padrao();
        assertEquals("WASD mover · Z correr · ESPAÇO pular · V pulo curto · C agachar · ESC pausa · F4 cenário",
                AjudaControles.dicaHud(AjudaControles.Modo.INDIVIDUAL, p, PapelCombate.LIVRE, PapelCombate.LIVRE));
        assertEquals("WASD", AjudaControles.movimento(p, SlotJogador.JOGADOR_1));
        assertEquals("setas", AjudaControles.movimento(p, SlotJogador.JOGADOR_2));
        assertEquals("SHIFT+A/D", AjudaControles.arrasto(p, SlotJogador.JOGADOR_1));
    }

    @Test
    void faixaContextualMudaComOPapel() {
        MapaControles p = MapaControles.padrao();
        String livre = AjudaControles.dicaHud(AjudaControles.Modo.DOIS_JOGADORES, p,
                PapelCombate.LIVRE, PapelCombate.LIVRE);
        // 5I (pedido): antes "J2: setas" (o J2 nao aplicava pegada).
        assertEquals("J1: WASD · H pegar   J2: setas · END pegar   ESC pausa/controles · F3 · F4", livre);
        String pegada = AjudaControles.dicaHud(AjudaControles.Modo.DOIS_JOGADORES, p,
                PapelCombate.APLICADOR, PapelCombate.RECEPTOR);
        assertEquals("J1: WASD · T desequilibrar · Y reforçar · SHIFT+A/D arrastar   J2: setas · U romper"
                + "   ESC pausa/controles · F3 · F4", pegada);
        String entrando = AjudaControles.dicaHud(AjudaControles.Modo.TREINO, p,
                PapelCombate.ENTRANDO, PapelCombate.ENTRANDO);
        assertEquals("WASD · ESPAÇO pular · F5 reiniciar · ESC pausa/controles", entrando);
    }

    @Test
    void textosRefletemAsTeclasRemapeadas() {
        MapaControles m = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.INICIAR_PEGADA, t("P"))
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MOVER_CIMA, t("I"))
                .comTecla(SlotJogador.JOGADOR_2, AcaoJogador.SPRAWL, t("NUMPAD1"))
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO, t("Q"));
        assertEquals("I/A/S/D", AjudaControles.movimento(m, SlotJogador.JOGADOR_1));
        assertEquals("P pegar", AjudaControles.agora(m, SlotJogador.JOGADOR_1, PapelCombate.LIVRE));
        assertEquals("T desequilibrar · Y reforçar · Q+A/D arrastar",
                AjudaControles.agora(m, SlotJogador.JOGADOR_1, PapelCombate.APLICADOR));

        List<AjudaControles.Linha> linhas = AjudaControles.linhas(m, SlotJogador.JOGADOR_1);
        assertTrue(linhas.contains(new AjudaControles.Linha("Iniciar pegada (PEGADA 1)", "P")), linhas.toString());
        assertTrue(linhas.contains(new AjudaControles.Linha("COMBATE", null)));
        assertEquals("MOVIMENTO", linhas.get(0).texto());
        assertTrue(linhas.get(0).isTitulo());
        assertFalse(linhas.stream().anyMatch(l -> "H".equals(l.tecla())), "H nao aparece mais");
        // 5I (pedido): antes 16 + 3; o J1 ganhou o rompimento. 5L (pedido): + 3 pegadas.
        assertEquals(20 + 3, linhas.size(), "20 acoes + MOVIMENTO, COMBATE, DEFESA");
        assertTrue(AjudaControles.linhas(m, SlotJogador.JOGADOR_2)
                .contains(new AjudaControles.Linha("Sprawl", "NUM 1")));
    }

    @Test
    void faixasCabemNaLarguraDaTela() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        java.awt.FontMetrics fm = img.createGraphics().getFontMetrics(new Font("Arial", Font.PLAIN, 11));
        MapaControles p = MapaControles.padrao();
        for (AjudaControles.Modo modo : AjudaControles.Modo.values()) {
            for (PapelCombate a : PapelCombate.values()) {
                String dica = AjudaControles.dicaHud(modo, p, a, PapelCombate.RECEPTOR);
                assertTrue(fm.stringWidth(dica) < 790, modo + "/" + a + ": " + dica);
            }
        }
    }
}
