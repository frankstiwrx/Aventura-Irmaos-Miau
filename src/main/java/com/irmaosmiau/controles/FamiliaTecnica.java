package com.irmaosmiau.controles;

/**
 * Entrega 5O: a FAMILIA de tecnicas que a direcao da tecnica (RS no gamepad;
 * H/6/7/8 ou END/NUM 7-9 no teclado) escolhe agora, pelo modificador segurado.
 *
 * Prioridade deterministica quando os dois modificadores estao segurados:
 * DEFESAS (RT) vence QUEDAS (RB). Defesa e reacao e nunca inicia contato;
 * assim apertar os dois por engano nunca dispara uma queda nem uma pegada.
 */
public enum FamiliaTecnica {
    /** Sem modificador: as quatro pegadas (como ate a 5N). */
    PEGADAS("PEGADAS"),
    /** RT + direcao: guarda alta / media / baixa e sprawl. */
    DEFESAS("DEFESAS"),
    /** RB + direcao: quedas e transicoes (reservado; nada executa ainda). */
    PROJECOES("QUEDAS E TRANSIÇÕES"),
    /** Y + direcao do movimento: acrobacias (le o analogico ESQUERDO, nao o RS). */
    ACROBACIAS("ACROBACIAS");

    private final String rotulo;

    FamiliaTecnica(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Familia da direcao da tecnica pelos modificadores segurados (RT antes de RB). */
    public static FamiliaTecnica dosModificadores(boolean defesa, boolean projecao) {
        if (defesa) {
            return DEFESAS;
        }
        return projecao ? PROJECOES : PEGADAS;
    }

    /** Uma escolha: familia + direcao (uma por inclinacao/toque). */
    public record Escolha(FamiliaTecnica familia, DirecaoTecnica direcao) {
    }
}
