package com.irmaosmiau.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EfeitosTemporariosTest {

    @Test
    void efeitoExpiraSozinhoDepoisDaDuracao() {
        EfeitosTemporarios e = new EfeitosTemporarios();
        e.aplicar(EfeitosTemporarios.Tipo.DESEQUILIBRIO, 3);

        assertTrue(e.isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
        assertEquals(0.6, e.getFatorVelocidade(), 1e-12);
        e.atualizar();
        e.atualizar();
        assertEquals(1, e.getTicksRestantes(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
        e.atualizar();
        assertFalse(e.isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
        assertEquals(1.0, e.getFatorVelocidade());
        assertFalse(e.temAlgumAtivo());
    }

    @Test
    void reaplicarRenovaSemAcumular() {
        EfeitosTemporarios e = new EfeitosTemporarios();
        e.aplicar(EfeitosTemporarios.Tipo.DESEQUILIBRIO, 100);
        for (int i = 0; i < 90; i++) {
            e.atualizar();
        }
        e.aplicar(EfeitosTemporarios.Tipo.DESEQUILIBRIO, 100);
        e.aplicar(EfeitosTemporarios.Tipo.DESEQUILIBRIO, 100);
        assertEquals(100, e.getTicksRestantes(EfeitosTemporarios.Tipo.DESEQUILIBRIO), "renova, nao soma");
        assertEquals(0.6, e.getFatorVelocidade(), 1e-12, "fator nao multiplica por reaplicar");

        e.limpar();
        assertEquals(1.0, e.getFatorVelocidade());
        assertThrows(IllegalArgumentException.class, () -> e.aplicar(EfeitosTemporarios.Tipo.DESEQUILIBRIO, 0));
        assertThrows(IllegalArgumentException.class, () -> e.aplicar(null, 5));
    }

    @Test
    void desequilibrioReduzAVelocidadeDaInstanciaERestauraAoExpirar() {
        IrmaoMiau p = new IrmaoMiauPreto(300, 150, 0.7);
        double normal = p.getVelocidadeBase();
        double corridaNormal = p.getVelocidadeCorrida();

        p.getEfeitos().aplicar(EfeitosTemporarios.Tipo.DESEQUILIBRIO, 10);
        assertEquals(normal * 0.6, p.getVelocidadeBase(), 1e-12);
        assertEquals(corridaNormal * 0.6, p.getVelocidadeCorrida(), 1e-12);
        assertEquals(45.0, p.getAtributos().getVelocidade(), "atributo nao e alterado");

        for (int i = 0; i < 10; i++) {
            p.atualizar(false, false);     // loop logico
        }
        assertEquals(normal, p.getVelocidadeBase(), 1e-12, "restaurado automaticamente");

        IrmaoMiau outro = new IrmaoMiauPreto(300, 150, 0.7);
        assertEquals(normal, outro.getVelocidadeBase(), 1e-12, "efeito e da instancia, nao do personagem");
    }
}
