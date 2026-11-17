package com.irmaosmiau.treino;

import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.Posicao;

/**
 * Entrega 5Q.2: situacoes iniciais do LABORATORIO (so posicoes reais do
 * MotorSolo; nenhuma duplicada). O lado "ativo" e quem CONTROLA a posicao:
 * - posicoes dominantes (100 kg, joelho, montada, costas): o ativo esta por cima;
 * - guardas e controle de perna: o ativo esta por baixo e controla;
 * - solo livre: o ativo esta em pe (ninguem controla);
 * - em pe com pegada: o ativo aplica a pegada.
 */
public enum SituacaoTreino {
    EM_PE("Em pé, sem pegada", Posicao.EM_PE_SEPARADOS, Papel.NINGUEM, false),
    EM_PE_PEGADA("Em pé, com pegada", Posicao.EM_PE_PEGADA, Papel.PEGADA, false),
    SOLO_LIVRE("Solo livre (um em pé)", Posicao.EM_PE_CONTRA_SOLO, Papel.EM_PE, false),
    GUARDA_FECHADA("Guarda fechada", Posicao.GUARDA, Papel.BAIXO, false),
    GUARDA_ABERTA("Guarda aberta", Posicao.GUARDA_ABERTA, Papel.BAIXO, false),
    MEIA_GUARDA("Meia-guarda", Posicao.MEIA_GUARDA, Papel.BAIXO, false),
    BORBOLETA("Guarda borboleta", Posicao.GUARDA_BORBOLETA, Papel.BAIXO, false),
    DE_LA_RIVA("Guarda De La Riva", Posicao.DE_LA_RIVA, Papel.BAIXO, false),
    LACO("Guarda laço", Posicao.GUARDA_LACO, Papel.BAIXO, true),
    ARANHA("Guarda aranha", Posicao.GUARDA_ARANHA, Papel.BAIXO, true),
    SINGLE_LEG_X("Single-leg X", Posicao.SINGLE_LEG_X, Papel.BAIXO, false),
    CEM_QUILOS("100 kg (controle lateral)", Posicao.CONTROLE_LATERAL, Papel.CIMA, false),
    JOELHO_BARRIGA("Joelho na barriga", Posicao.JOELHO_BARRIGA, Papel.CIMA, false),
    MONTADA("Montada", Posicao.MONTADA, Papel.CIMA, false),
    COSTAS("Controle das costas", Posicao.COSTAS, Papel.CIMA, false),
    ASHI_GARAMI("Ashi garami", Posicao.ASHI_GARAMI, Papel.BAIXO, false);

    /** Onde o lado ativo fica. */
    public enum Papel {
        NINGUEM, PEGADA, EM_PE, BAIXO, CIMA
    }

    private final String nome;
    private final Posicao posicao;
    private final Papel papel;
    private final boolean exigeManga;

    SituacaoTreino(String nome, Posicao posicao, Papel papel, boolean exigeManga) {
        this.nome = nome;
        this.posicao = posicao;
        this.papel = papel;
        this.exigeManga = exigeManga;
    }

    public String getNome() {
        return nome;
    }

    public Posicao getPosicao() {
        return posicao;
    }

    public Papel getPapel() {
        return papel;
    }

    /** Laco e aranha so existem com a manga presa (dada a quem controla). */
    public boolean exigeManga() {
        return exigeManga;
    }

    /** Quem fica "por cima" no motor para este ativo (null = ninguem). */
    public Lado porCima(Lado ativo) {
        return switch (papel) {
            case CIMA, EM_PE -> ativo;
            case BAIXO -> ativo.outro();
            default -> null;
        };
    }

    /** Quem controla no motor (null = ninguem). */
    public Lado controlador(Lado ativo) {
        return switch (papel) {
            case CIMA, BAIXO, PEGADA -> ativo;
            default -> null;
        };
    }
}
