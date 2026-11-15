package com.irmaosmiau.combate;

/**
 * Entrega 5K: comparacao de animacoes de pegada (recurso de desenvolvimento).
 *
 * - AUTORAL (padrao): as animacoes do usuario (PEGADA_1 e PEGADA_1_PRETO),
 *   exatamente como na 5J.
 * - EXPERIMENTAL: usa as variantes marcadas "experimental" no catalogo
 *   quando existe uma compativel (molde, papel e postura); senao, a autoral.
 *
 * Vale para a sessao inteira (sobrevive a F5, TENTAR NOVAMENTE e troca de
 * partida), nunca e gravado em arquivo e comeca AUTORAL a cada execucao,
 * salvo -Dirmaosmiau.animacoes.pegada=experimental. A troca vale a partir da
 * proxima pegada (nada muda no meio de uma tecnica). No jogo: F6.
 */
public enum ModoAnimacoesPegada {

    AUTORAL,
    EXPERIMENTAL;

    public static final String PROPRIEDADE = "irmaosmiau.animacoes.pegada";

    private static volatile ModoAnimacoesPegada atual = inicial();

    private static ModoAnimacoesPegada inicial() {
        return "experimental".equalsIgnoreCase(System.getProperty(PROPRIEDADE, "")) ? EXPERIMENTAL : AUTORAL;
    }

    public static ModoAnimacoesPegada atual() {
        return atual;
    }

    public static void definir(ModoAnimacoesPegada modo) {
        atual = modo == null ? AUTORAL : modo;
    }

    /** Alterna e devolve o novo modo. */
    public static ModoAnimacoesPegada alternar() {
        atual = atual == AUTORAL ? EXPERIMENTAL : AUTORAL;
        return atual;
    }
}
