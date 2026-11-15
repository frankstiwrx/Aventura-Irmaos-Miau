package com.irmaosmiau.animacao;

/**
 * Entrega 5O: qual versao do ROLAMENTO TECNICO e do SPRAWL o jogo desenha.
 *
 * - ORIGINAL: o desenho de sempre (arrays ROLAMENTO_MIAUSTUDIO e
 *   SPRAWL_MIAUSTUDIO dentro de IrmaoMiauBranco/IrmaoMiauPreto, intactos).
 * - EXPERIMENTAL_5O: as animacoes novas compartilhadas
 *   (/conteudo/animacoes/movimentos/*_5o.json, rig miau_v1).
 *
 * Independente do F6 (que continua sendo so das pegadas e das reacoes do
 * receptor): F7 alterna. A mecanica (distancia, tempo, custo, colisao) e a
 * mesma nas duas versoes; so o desenho muda. As guardas e as acrobacias sao
 * novas (nao ha versao anterior) e aparecem nas duas.
 */
public enum VersaoMovimentos {
    ORIGINAL("ORIGINAL"),
    EXPERIMENTAL_5O("EXPERIMENTAL 5O");

    public static final String PROPRIEDADE = "irmaosmiau.animacoes.movimentos";

    private static volatile VersaoMovimentos atual = inicial();

    private final String rotulo;

    VersaoMovimentos(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    private static VersaoMovimentos inicial() {
        String p = System.getProperty(PROPRIEDADE);
        if (p != null) {
            for (VersaoMovimentos v : values()) {
                if (v.name().equalsIgnoreCase(p.trim())) {
                    return v;
                }
            }
        }
        return ORIGINAL;                        // motor: o de sempre (o Main aplica o padrao do jogo)
    }

    /** O jogo abre na versao nova, para comparar com F7. */
    public static VersaoMovimentos padraoDoJogo() {
        return EXPERIMENTAL_5O;
    }

    public static VersaoMovimentos atual() {
        return atual;
    }

    public static void definir(VersaoMovimentos v) {
        atual = v == null ? ORIGINAL : v;
    }

    public static void restaurarPadrao() {
        atual = inicial();
    }

    public static VersaoMovimentos alternar() {
        atual = atual == ORIGINAL ? EXPERIMENTAL_5O : ORIGINAL;
        return atual;
    }
}
