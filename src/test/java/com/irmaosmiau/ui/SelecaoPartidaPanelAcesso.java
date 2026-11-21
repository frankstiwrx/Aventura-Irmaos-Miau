package com.irmaosmiau.ui;

/** Acesso de testes de outros pacotes aos tempos da selecao de partida. */
public final class SelecaoPartidaPanelAcesso {

    private SelecaoPartidaPanelAcesso() {
    }

    public static int[] tempos() {
        return SelecaoPartidaPanel.TEMPOS.clone();
    }

    public static String texto(int segundos) {
        return SelecaoPartidaPanel.textoTempo(segundos);
    }
}
