package com.irmaosmiau.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.1: evolucao composta dos atributos por graduacao (floor a cada promocao; peso fixo). */
class EvolucaoGraduacaoTest {

    @Test
    void exemploDoPedido40() {
        assertEquals(40, EvolucaoGraduacao.evoluir(40, Graduacao.BRANCA));
        assertEquals(48, EvolucaoGraduacao.evoluir(40, Graduacao.AZUL));
        assertEquals(56, EvolucaoGraduacao.evoluir(40, Graduacao.ROXA));
        assertEquals(66, EvolucaoGraduacao.evoluir(40, Graduacao.MARROM));
        assertEquals(75, EvolucaoGraduacao.evoluir(40, Graduacao.PRETA));
    }

    @Test
    void arredondaEmCadaPromocaoENaoSoNoFim() {
        // 22: 26.4->26, 30.42->30, 35.4->35, 40.25->40 (a conta direta daria floor(41.9) = 41)
        assertEquals(26, EvolucaoGraduacao.evoluir(22, Graduacao.AZUL));
        assertEquals(30, EvolucaoGraduacao.evoluir(22, Graduacao.ROXA));
        assertEquals(35, EvolucaoGraduacao.evoluir(22, Graduacao.MARROM));
        assertEquals(40, EvolucaoGraduacao.evoluir(22, Graduacao.PRETA));
        assertEquals(41, Math.floor(22 * 1.20 * 1.17 * 1.18 * 1.15), "a conta acumulada sem floor seria diferente");
    }

    @Test
    void valoresDoBrancoEDoPretoEmCadaFaixa() {
        AtributosLutador b = IrmaoMiauBranco.atributosOficiais();
        AtributosLutador p = IrmaoMiauPreto.atributosOficiais();
        // Branco: cond 35, folego 25, forca 42, vel 22, agil 25, tec 25, rec 22, peso 72
        double[][] branco = {{35, 25, 42, 22, 25, 25, 22}, {42, 30, 50, 26, 30, 30, 26}, {49, 35, 58, 30, 35, 35, 30},
            {57, 41, 68, 35, 41, 41, 35}, {65, 47, 78, 40, 47, 47, 40}};
        for (Graduacao g : Graduacao.values()) {
            AtributosLutador a = EvolucaoGraduacao.aplicar(b, g);
            double[] e = branco[g.ordinal()];
            double[] v = {a.getCondicionamento(), a.getFolego(), a.getForca(), a.getVelocidade(), a.getAgilidade(),
                a.getTecnica(), a.getRecuperacao()};
            for (int i = 0; i < 7; i++) {
                assertEquals(e[i], v[i], 1e-9, g + " atributo " + i);
            }
            assertEquals(72, a.getPeso(), 1e-9, "peso do Branco nao muda");
            AtributosLutador q = EvolucaoGraduacao.aplicar(p, g);
            assertEquals(68, q.getPeso(), 1e-9, "peso do Preto nao muda");
            assertTrue(q.getVelocidade() > a.getVelocidade() && a.getForca() > q.getForca(),
                    g + ": as diferencas entre os dois continuam");
        }
        assertEquals(85, EvolucaoGraduacao.aplicar(p, Graduacao.PRETA).getVelocidade(), 1e-9, "45 -> 54 -> 63 -> 74 -> 85");
    }

    @Test
    void brancaEhOOriginalEAplicarDeNovoNaoAcumula() {
        AtributosLutador b = IrmaoMiauBranco.atributosOficiais();
        AtributosLutador branca = EvolucaoGraduacao.aplicar(b, Graduacao.BRANCA);
        assertNotSame(b, branca, "nunca devolve a base (imutavel)");
        assertEquals(b.getForca(), branca.getForca(), 1e-9);
        AtributosLutador preta1 = EvolucaoGraduacao.aplicar(b, Graduacao.PRETA);
        AtributosLutador preta2 = EvolucaoGraduacao.aplicar(b, Graduacao.PRETA);
        assertEquals(preta1.getForca(), preta2.getForca(), 1e-9, "repetir a escolha nao soma bonus");
        assertEquals(42, b.getForca(), 1e-9, "a base nao foi tocada");
    }
}
