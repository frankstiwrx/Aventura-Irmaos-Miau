package com.irmaosmiau.ui;

/**
 * Entrega 5I (complemento): tela que controla quando o {@link NavegadorMenus}
 * pode agir. Telas comuns nao precisam implementar (sempre navegaveis).
 */
public interface TelaNavegavel {

    /** false = o navegador nao mexe na tela agora (combate rodando, captura de tecla...). */
    boolean aceitaNavegacao();

    /** false = ESC nao e o "voltar" do navegador nesta tela (a propria tela trata o ESC). */
    default boolean escVolta() {
        return true;
    }
}
