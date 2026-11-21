package com.irmaosmiau.animacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.8: microanimacoes defensivas mais perceptiveis (40% maiores) e o MANTER presente. */
class MicroDefensivas5Q8Test {

    private static int pico(String chave) {
        int m = 0;
        for (double t = 0; t <= 1.0; t += 0.05) {
            double[] d = AnimacoesTecnicas.amostraParaTeste(chave, t);
            for (int i = 4; i < 30; i++) {
                m = Math.max(m, (int) Math.round(Math.abs(d[i])));
            }
        }
        return m;
    }

    @Test
    void defensivasDePorBaixoFicaramMaisVisiveis() {
        assertEquals(1.4, AnimacoesTecnicas.AMPLITUDE_MICRO_DEFENSIVA, 1e-9);
        // picos da 5Q.7: enquadrar 18, recuperar guarda 16, virar 12, escapar da montada 14, girar 14, defender 18
        assertTrue(pico("enquadrar:ATACA") >= 24, "enquadrar " + pico("enquadrar:ATACA"));
        assertTrue(pico("escape_quadril:ATACA") >= 21, "recuperar a guarda");
        assertTrue(pico("virar_joelhos:ATACA") >= 16, "virar");
        assertTrue(pico("escape_montada:ATACA") >= 19, "escapar da montada");
        assertTrue(pico("girar_nas_costas:ATACA") >= 19, "girar");
        assertTrue(pico("defender_maos:ATACA") >= 24, "defender");
        assertEquals(pico("enquadrar:ATACA"), pico("enquadrar_joelho:ATACA"), "o alias acompanha");
        assertTrue(pico("retencao:ATACA") >= 6 && pico("retencao:ATACA") <= 12, "MANTER continua sutil");
    }
}
