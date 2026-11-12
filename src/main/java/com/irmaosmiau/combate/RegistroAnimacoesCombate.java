package com.irmaosmiau.combate;

import java.util.HashMap;
import java.util.Map;

/** Dados visuais separados das regras e dos atributos do combate. */
public final class RegistroAnimacoesCombate {

    private final Map<String, AnimacaoConjuntaQuadros> animacoes = new HashMap<>();

    public RegistroAnimacoesCombate() {
        // Dados gerados a partir de PEGADA_1.json, MiauStudio projeto v2.
        // Aplicador: Branco; receptor: Preto; centro de autoria (360, linha 2).
        registrar("pegada_1_entrada", new AnimacaoConjuntaQuadros(
            new int[][] {
                {56, 0, 7, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {56, 0, 7, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 38, 0, 60, 3, -10, 12, -14, 26, 0, 0, 0, 0, 0, 0, 0, 0},
                {56, 0, 7, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 13, 119, -25, -10, 12, -14, 26, 7, -2, 14, 3, 11, -6, 0, 0}
            },
            new int[][] {
                {140, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {140, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -12, -12, -15, -9, 0, 0, 0, 0, 0, 0, 0, 0},
                {125, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, -6, 0, 0, 21, -11, 8, 10, -23, -18, -28, 0, 14, 0, 27, 0, 15, -1, 0, 0}
            },
            0.0, 0.0
        ));
    }

    public void registrar(String id, AnimacaoConjuntaQuadros sequencia) {
        if (id == null || id.isBlank() || sequencia == null) {
            throw new IllegalArgumentException("ID e sequencia sao obrigatorios.");
        }
        animacoes.put(id.trim().toLowerCase(), sequencia);
    }

    public AnimacaoConjuntaQuadros buscar(String id) {
        return id == null ? null : animacoes.get(id.trim().toLowerCase());
    }
}
