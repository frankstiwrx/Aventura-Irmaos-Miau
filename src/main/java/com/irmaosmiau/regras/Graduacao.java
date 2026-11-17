package com.irmaosmiau.regras;

/**
 * Entrega 5Q: graduacao LOGICA do lutador (progressao propria dos Irmaos Miau).
 *
 * Independente da cor COSMETICA da faixa desenhada no personagem: trocar a
 * faixa visual nunca muda o que o lutador pode tentar. A graduacao so libera o
 * acesso a uma tecnica; nao garante a execucao (controle, posicao e reacao do
 * outro decidem).
 */
public enum Graduacao {
    BRANCA("Br"),
    AZUL("Az"),
    ROXA("Rx"),
    MARROM("Mr"),
    PRETA("Pt");

    private final String sigla;

    Graduacao(String sigla) {
        this.sigla = sigla;
    }

    public String getSigla() {
        return sigla;
    }

    /** Esta graduacao alcanca a exigida? */
    public boolean alcanca(Graduacao exigida) {
        return exigida == null || ordinal() >= exigida.ordinal();
    }
}
