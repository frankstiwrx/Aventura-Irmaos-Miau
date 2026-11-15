package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.AnimacaoConjuntaQuadros;
import com.irmaosmiau.combate.Pegada1RegressaoArraysTest;
import com.irmaosmiau.combate.RegistroAnimacoesCombate;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.TipoPersonagem;

import org.junit.jupiter.api.Test;

/**
 * Entrega 3A: o GamePanel REAL (o mesmo que Main e MainPrototipo criam)
 * usa o conteudo dos catalogos JSON, nao os arrays legados.
 */
class GamePanelConteudoTest {

    private static GamePanel criar(TipoPersonagem tipo) {
        Runnable nada = () -> { };
        return new GamePanel(tipo, nada, nada, nada);
    }

    @Test
    void modoDoisMiausUsaAPegada1DoJsonDoMiauStudio() {
        SistemaCombate s = criar(TipoPersonagem.AMBOS).getSistemaCombate();
        assertNotNull(s);

        String origem = s.getRegistroAnimacoes().getOrigem("pegada_1_entrada");
        assertEquals("/conteudo/miaustudio/PEGADA_1.json", origem);
        assertNotEquals(RegistroAnimacoesCombate.ORIGEM_CODIGO_JAVA, origem);

        AnimacaoConjuntaQuadros anim = s.getRegistroAnimacoes().exigir("pegada_1_entrada");
        assertEquals(30, anim.getDuracaoTotalFrames());
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[i], anim.copiarQuadroAplicador(i));
            assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[i], anim.copiarQuadroReceptor(i));
        }
    }

    @Test
    void modoDoisMiausUsaOCatalogoDeTecnicasComPendentes() {
        SistemaCombate s = criar(TipoPersonagem.AMBOS).getSistemaCombate();

        assertEquals(4, s.getRegistroAcoes().quantidade(), "catalogo JSON, nao o registro legado");
        assertTrue(s.getAcao("pegada_1").isJogavel());
        assertEquals(30, s.getAcao("pegada_1").getDuracaoAnimacaoFrames());
        assertEquals(3.0, s.getAcao("pegada_1").getCustoFolego());
        // 5L (pedido): as pegadas 2-4 ficaram jogaveis (antes assertFalse).
        for (String id : new String[]{"pegada_2", "pegada_3", "pegada_4"}) {
            assertTrue(s.getAcao(id).isJogavel(), id);
        }
    }

    @Test
    void modoIndividualNaoCriaCombate() {
        assertNull(criar(TipoPersonagem.BRANCO).getSistemaCombate());
        assertNull(criar(TipoPersonagem.PRETO).getSistemaCombate());
    }
}
