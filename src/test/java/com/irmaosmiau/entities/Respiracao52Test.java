package com.irmaosmiau.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Entrega 5O.2: parametros da respiracao por postura e a respiracao discreta no desenho do chao. */
class Respiracao52Test {

    @Test
    void parametrosCentralizados() {
        assertEquals(1.0, CustosFolego.recuperacaoRespirando(IrmaoMiau.EstadoCorporal.EM_PE), 1e-9);
        assertEquals(0.7, CustosFolego.recuperacaoRespirando(IrmaoMiau.EstadoCorporal.AJOELHADO), 1e-9);
        assertEquals(0.4, CustosFolego.recuperacaoRespirando(IrmaoMiau.EstadoCorporal.DEITADO), 1e-9);
        assertEquals(1.0, CustosFolego.recuperacaoRespirando(IrmaoMiau.EstadoCorporal.AGACHADO), 1e-9);
        IrmaoMiau l = new IrmaoMiauBranco(300, 150, 0.7);
        double emPe = l.getVelocidadeRespirando();
        l.ajoelhar();
        double joelhos = l.getVelocidadeRespirando();
        assertTrue(joelhos < l.getVelocidadeAjoelhado(), "uma penalidade so, abaixo do andar de joelhos");
        l.alternarDeitado();
        l.alternarDeitado();
        assertTrue(l.isDeitado());
        double deitado = l.getVelocidadeRespirando();
        assertTrue(deitado < l.getVelocidadeRastejando());
        assertTrue(deitado < joelhos && joelhos < emPe);
    }

    @Test
    void respiraDeitadoComOPeitoSubindoEDescendoSemLevantar() {
        IrmaoMiau l = new IrmaoMiauPreto(300, 150, 0.7);
        l.alternarDeitado();
        assertTrue(l.isAjoelhado() || l.isDeitado());
        if (l.isAjoelhado()) {
            l.alternarDeitado();
            l.alternarDeitado();
        }
        int[] base = new int[30];
        base[2] = 7;
        base[3] = 1;
        l.aplicarPoseCombateExterna(base, 0, 0);
        Set<Integer> peito = new HashSet<>();
        for (int i = 0; i < 80; i++) {
            l.setRespirando(true);
            l.atualizar(false, false);
            assertTrue(l.isRespirando());
            peito.add(l.getPoseCombateExternaParaDesenho()[8]);
        }
        assertTrue(peito.size() >= 2, "o peito se move: " + peito);
        assertTrue(peito.stream().allMatch(v -> Math.abs(v) <= 1), "discreto: " + peito);
        assertTrue(l.isDeitado(), "nao levanta");
        assertEquals(0, l.copiarPoseCombateExterna()[8], "a pose guardada nao muda (so o desenho)");
        l.setRespirando(false);
        l.atualizar(false, false);
        assertFalse(l.isRespirando());
    }
}
