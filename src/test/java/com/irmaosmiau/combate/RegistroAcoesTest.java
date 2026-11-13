package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegistroAcoesTest {

    private static AcaoCombate acaoSimples(String id) {
        return new AcaoCombate(
                id, "Acao " + id, CategoriaAcao.IMPACTO, null,
                null, 0, 50.0, 0, false,
                1.0, 10.0, 1.0, 0.0, 0.0, 0.0
        );
    }

    @Test
    void registroPadraoContemSomentePegada1() {
        RegistroAcoes registro = new RegistroAcoes();

        assertEquals(1, registro.quantidade());
        assertTrue(registro.contem("pegada_1"));
        assertNotNull(registro.buscar("pegada_1"));
    }

    @Test
    void buscaIgnoraMaiusculasEEspacos() {
        RegistroAcoes registro = new RegistroAcoes();
        AcaoCombate acao = registro.buscar("pegada_1");

        assertSame(acao, registro.buscar("PEGADA_1"));
        assertSame(acao, registro.buscar("  Pegada_1  "));
        assertTrue(registro.contem(" PEGADA_1 "));
    }

    @Test
    void idsDesconhecidosOuNulosRetornamVazio() {
        RegistroAcoes registro = new RegistroAcoes();

        assertNull(registro.buscar("nao_existe"));
        assertNull(registro.buscar(null));
        assertFalse(registro.contem("nao_existe"));
        assertFalse(registro.contem(null));
    }

    @Test
    void registrarNovaAcaoPreservaOrdemDeCadastro() {
        RegistroAcoes registro = new RegistroAcoes();
        registro.registrar(acaoSimples("extra"));

        assertEquals(2, registro.quantidade());
        assertEquals(
                java.util.List.of("pegada_1", "extra"),
                registro.listar().stream().map(AcaoCombate::getId).toList()
        );
    }

    @Test
    void idDuplicadoENuloSaoRejeitados() {
        RegistroAcoes registro = new RegistroAcoes();

        assertThrows(IllegalArgumentException.class,
                () -> registro.registrar(acaoSimples("PEGADA_1")));
        assertThrows(IllegalArgumentException.class,
                () -> registro.registrar(null));
        assertEquals(1, registro.quantidade());
    }

    @Test
    void listagemNaoPodeSerAlteradaPorFora() {
        RegistroAcoes registro = new RegistroAcoes();

        assertThrows(UnsupportedOperationException.class,
                () -> registro.listar().clear());
    }
}
