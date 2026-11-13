package com.irmaosmiau.ui;

import com.irmaosmiau.personagens.PerfilLutador;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Gera o id (nome do arquivo) de um perfil a partir do nome digitado:
 * "Branco Forte!" -> "branco_forte". Se ja existir, acrescenta _2, _3...
 *
 * O id resultante sempre passa por PerfilLutador.normalizarId (mesma regra
 * da 4B); nada de validacao duplicada.
 */
final class GeradorIdPerfil {

    private static final int TAMANHO_BASE = 34;

    private GeradorIdPerfil() {
    }

    static String gerar(String nome, Predicate<String> disponivel) {
        String base = slug(nome == null ? "" : nome);
        if (base.length() < 3) {
            base = ("perfil_" + base).replaceAll("_+$", "");
        }
        if (base.length() > TAMANHO_BASE) {
            base = base.substring(0, TAMANHO_BASE).replaceAll("_+$", "");
        }
        String candidato = PerfilLutador.normalizarId(base);
        int n = 2;
        while (!disponivel.test(candidato)) {
            candidato = PerfilLutador.normalizarId(base + "_" + n++);
        }
        return candidato;
    }

    static String slug(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
