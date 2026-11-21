package com.irmaosmiau.regras;

/**
 * Modo de luta escolhido na tela CONFIGURACOES DA LUTA (JOGAR).
 *
 * So o que o motor realmente suporta pode iniciar uma partida ({@link #isDisponivel()}). Os outros ja
 * aparecem na tela como EM BREVE, para a interface ficar pronta para recebe-los aos poucos:
 *
 * - FINALIZACAO: a luta de hoje. Vence quem finaliza; com tempo, o fim do tempo encerra pelo criterio
 *   atual do jogo. Sem pontuacao tecnica. Usa a elegibilidade {@link ModoRegras#LIVRE}.
 * - PONTOS_E_FINALIZACAO: inspirado nas competicoes de jiu-jitsu (quedas, raspagens, passagens, montada,
 *   costas...). Depende de pontuacao e arbitragem, que ainda nao existem. Quando existir, usara uma
 *   elegibilidade de campeonato conferida com um regulamento real.
 * - ARCADE: modo livre, com rounds e condicoes de vitoria proprias (nao e regra oficial).
 *
 * Cada modo diz a {@link ModoRegras} (que finalizacoes valem) e, no futuro, pode dizer tempos proprios.
 */
public enum ModoLuta {
    FINALIZACAO("FINALIZAÇÃO", "Vence quem finalizar. Sem pontos.", true, ModoRegras.LIVRE),
    PONTOS_E_FINALIZACAO("PONTOS + FINALIZAÇÃO", "Pontuação de competição: em breve.", false,
            ModoRegras.CAMPEONATO_PROVISORIO),
    ARCADE("ARCADE", "Rounds e regras próprias: em breve.", false, ModoRegras.LIVRE);

    /** O modo com que a tela abre. */
    public static final ModoLuta PADRAO = FINALIZACAO;

    private final String nome;
    private final String descricao;
    private final boolean disponivel;
    private final ModoRegras regras;

    ModoLuta(String nome, String descricao, boolean disponivel, ModoRegras regras) {
        this.nome = nome;
        this.descricao = descricao;
        this.disponivel = disponivel;
        this.regras = regras;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    /** O motor suporta este modo hoje? Os outros nao podem iniciar uma partida. */
    public boolean isDisponivel() {
        return disponivel;
    }

    /** As finalizacoes que valem neste modo. */
    public ModoRegras getRegras() {
        return regras;
    }
}
