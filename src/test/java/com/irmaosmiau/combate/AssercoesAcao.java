package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Comparacao campo a campo de duas acoes (regras do motor). */
public final class AssercoesAcao {

    private AssercoesAcao() {
    }

    public static void assertMesmasRegras(AcaoCombate esperada, AcaoCombate atual) {
        String id = esperada.getId();
        assertEquals(esperada.getId(), atual.getId(), "id");
        assertEquals(esperada.getNome(), atual.getNome(), id + " nome");
        assertEquals(esperada.getCategoria(), atual.getCategoria(), id + " categoria");
        assertEquals(esperada.getEstadoInteracaoAoSucesso(), atual.getEstadoInteracaoAoSucesso(), id + " estado");
        assertEquals(esperada.getIdAnimacaoConjunta(), atual.getIdAnimacaoConjunta(), id + " animacao");
        assertEquals(esperada.getDuracaoAnimacaoFrames(), atual.getDuracaoAnimacaoFrames(), id + " duracao");
        assertEquals(esperada.getAlcanceHorizontal(), atual.getAlcanceHorizontal(), id + " alcance");
        assertEquals(esperada.getDiferencaMaximaLinhas(), atual.getDiferencaMaximaLinhas(), id + " linhas");
        assertEquals(esperada.isExigeFrenteParaOponente(), atual.isExigeFrenteParaOponente(), id + " frente");
        assertEquals(esperada.getCustoFolego(), atual.getCustoFolego(), id + " custo");
        assertEquals(esperada.getDificuldade(), atual.getDificuldade(), id + " dificuldade");
        assertEquals(esperada.getPesoTecnica(), atual.getPesoTecnica(), id + " peso tecnica");
        assertEquals(esperada.getPesoForca(), atual.getPesoForca(), id + " peso forca");
        assertEquals(esperada.getPesoAgilidade(), atual.getPesoAgilidade(), id + " peso agilidade");
        assertEquals(esperada.getPesoPesoCorporal(), atual.getPesoPesoCorporal(), id + " peso corporal");
        assertEquals(esperada.getDisponibilidade(), atual.getDisponibilidade(), id + " disponibilidade");
    }
}
