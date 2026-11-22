package com.irmaosmiau.ui;

/**
 * Versao PUBLICA do jogo e creditos (o unico lugar a mudar a cada lancamento). Aparece so no menu principal,
 * nunca sobre a luta ou o HUD. A versao do Maven (pom.xml) e a do artefato de build, nao a publica.
 */
public final class VersaoJogo {

    /** Versao publica (GitHub Releases). */
    public static final String VERSAO = "v0.1.0";
    /** Etapa do desenvolvimento, mostrada junto da versao. */
    public static final String ETAPA = "BETA";
    public static final String AUTOR = "frankstiwrx";
    public static final int ANO = 2026;

    private VersaoJogo() {
    }

    /** "by frankstiwrx · 2026". */
    public static String creditos() {
        return "by " + AUTOR + " · " + ANO;
    }

    /** "v0.1.0 BETA". */
    public static String versaoCompleta() {
        return VERSAO + " " + ETAPA;
    }
}
