package com.irmaosmiau.controles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.ReconhecedorCliqueDuplo.Evento;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Entrega 5K (testes 6, 9, 10, 11 e 15): clique simples x duplo pelo relogio logico. */
class ReconhecedorCliqueDuploTest {

    private static final int J = 16;

    /** Roda ticks 0..fim; bordas nos ticks dados; devolve "tick:EVENTO" dos eventos. */
    private static List<String> rodar(ReconhecedorCliqueDuplo r, int fim, int... bordas) {
        List<String> saida = new ArrayList<>();
        for (int t = 0; t <= fim; t++) {
            boolean borda = false;
            for (int b : bordas) {
                borda |= b == t;
            }
            Evento e = r.atualizar(borda, t);
            if (e != Evento.NADA) {
                saida.add(t + ":" + e);
            }
        }
        return saida;
    }

    @Test
    void janelaDe250msSao16Ticks() {
        assertEquals(16, ReconhecedorCliqueDuplo.ticksDaJanela(ReconhecedorCliqueDuplo.JANELA_MS_PADRAO, 16));
        assertThrows(IllegalArgumentException.class, () -> new ReconhecedorCliqueDuplo(0));
    }

    @Test
    void simplesSoDepoisQueAJanelaFecha() {
        ReconhecedorCliqueDuplo r = new ReconhecedorCliqueDuplo(J);
        assertEquals(List.of("18:SIMPLES"), rodar(r, 60, 1), "nada antes da janela fechar (o 2o clique ainda pode vir)");
    }

    @Test
    void doisCliquesDentroDaJanelaSaoUmDuploSemSimples() {
        ReconhecedorCliqueDuplo r = new ReconhecedorCliqueDuplo(J);
        assertEquals(List.of("5:DUPLO"), rodar(r, 60, 1, 5));
        r = new ReconhecedorCliqueDuplo(J);
        assertEquals(List.of("17:DUPLO"), rodar(r, 60, 1, 17), "no limite exato da janela ainda e duplo");
    }

    @Test
    void doisCliquesForaDaJanelaSaoDoisSimples() {
        ReconhecedorCliqueDuplo r = new ReconhecedorCliqueDuplo(J);
        assertEquals(List.of("18:SIMPLES", "40:SIMPLES"), rodar(r, 60, 1, 23));
    }

    @Test
    void terceiroCliqueComecaNovaContagem() {
        ReconhecedorCliqueDuplo r = new ReconhecedorCliqueDuplo(J);
        assertEquals(List.of("3:DUPLO", "22:SIMPLES"), rodar(r, 60, 1, 3, 5),
                "o 3o clique nao e absorvido pelo duplo anterior");
        r = new ReconhecedorCliqueDuplo(J);
        assertEquals(List.of("3:DUPLO", "7:DUPLO"), rodar(r, 60, 1, 3, 5, 7), "quatro cliques = dois duplos");
    }

    @Test
    void botaoSeguradoNaoGeraCliques() {
        // O GamePanel so manda BORDAS: segurar = uma borda so.
        ReconhecedorCliqueDuplo r = new ReconhecedorCliqueDuplo(J);
        assertEquals(List.of("18:SIMPLES"), rodar(r, 200, 1));
    }

    @Test
    void limparDescartaOPendente() {
        ReconhecedorCliqueDuplo r = new ReconhecedorCliqueDuplo(J);
        r.atualizar(true, 1);
        assertTrue(r.isPendente());
        r.limpar();
        assertFalse(r.isPendente());
        for (int t = 2; t < 60; t++) {
            assertEquals(Evento.NADA, r.atualizar(false, t), "pausa/foco/F5: o clique pendente nao sai depois");
        }
        assertEquals(Evento.NADA, r.atualizar(true, 61), "e o proximo clique comeca do zero");
    }
}
