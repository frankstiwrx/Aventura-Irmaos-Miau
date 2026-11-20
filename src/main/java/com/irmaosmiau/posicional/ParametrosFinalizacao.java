package com.irmaosmiau.posicional;

/**
 * Entrega 5Q.5: TODOS os parametros de velocidade e pressao das finalizacoes num
 * lugar so (balanceamento). O jogo roda a ~62 ticks por segundo (16 ms).
 *
 * Linha do tempo de uma finalizacao sem defesa (faixa branca, controle medio):
 * entrada ~0,5 s (a defesa corta) -> disputa a partir de 30% -> ~6 a 8 s ate 100%
 * so com a pressao passiva (bem menos apertando) -> NA PONTA: ~0,7 s em 100% antes
 * de consolidar, com o bater, a defesa e o soltar ainda valendo.
 */
public final class ParametrosFinalizacao {

    /** Multiplica a janela de entrada do catalogo (5Q.5: antes 1,3; 0,2 s nao dava para reagir). */
    public static final double FATOR_ENTRADA = 3.0;
    /** Janela base da entrada (ticks, antes do fator): ate a azul / roxa em diante. */
    public static final int JANELA_BASE = 10;
    public static final int JANELA_AVANCADA = 14;

    /** Pressao (0-100) no inicio da disputa, depois da entrada. */
    public static final double PRESSAO_INICIAL = 30;
    /** Pressao que sobe sozinha por tick (antes do fator de ritmo e da eficiencia). */
    public static final double GANHO_ESTRANGULAMENTO = 0.40;
    public static final double GANHO_CHAVE = 0.35;
    /** 5Q.5: ritmo da pressao passiva (antes 1,0: chegava a 100% rapido demais). */
    public static final double FATOR_GANHO_PASSIVO = 0.6;

    /** Apertar (quem ataca) e defender (quem e atacado), por acao, com eficiencia 1. */
    public static final double PRESSAO_APERTO = 14;
    public static final double PRESSAO_DEFESA = 22;
    /** Escapar so funciona com a pressao ate aqui. */
    public static final double LIMIAR_ESCAPE = 45;
    public static final double ESCAPE_FALHO_PRESSAO = 8;
    /** 5Q.6: depois de uma defesa tecnica, por estes ticks o escape aceita mais pressao. */
    public static final int TICKS_JANELA_FUGA = 45;
    /** 5Q.6: quanto a janela de fuga soma ao LIMIAR_ESCAPE (45 -> 65). */
    public static final double BONUS_JANELA_FUGA = 20;
    /** Ticks entre duas acoes do mesmo lado na disputa. */
    public static final int RECARGA_ACAO = 12;

    /**
     * 5Q.5: em 100% a finalizacao fica NA PONTA por este tempo antes de consolidar:
     * quem defende ainda bate, defende (a pressao cai e volta a disputa) e quem ataca
     * ainda solta. Antes consolidava no mesmo tick em que chegava a 100%.
     */
    public static final int TICKS_NA_PONTA = 45;

    /** Custos base (passam pelo CustosFolego.custoSolo). */
    public static final double CUSTO_APERTO = 1.0;
    public static final double CUSTO_DEFESA = 1.0;
    public static final double CUSTO_ESCAPE = 2.5;
    public static final double CUSTO_PERNA = 4.0;
    public static final double CUSTO_OUTRAS = 3.0;

    /**
     * 5Q.5: postura quebrada (guarda fechada). Dura mais e, no comeco, quem foi
     * quebrado AINDA NAO consegue refazer a postura: e a janela real da guilhotina,
     * do triangulo e das raspagens que exigem o outro sem postura.
     */
    public static final int POSTURA_QUEBRADA_TICKS = 75;
    public static final int POSTURA_BLOQUEADA_TICKS = 40;

    private ParametrosFinalizacao() {
    }

    /** Janela de entrada do catalogo (antes do FATOR_ENTRADA) para esta graduacao. */
    public static int janelaCatalogo(com.irmaosmiau.regras.Graduacao g) {
        return g.ordinal() >= com.irmaosmiau.regras.Graduacao.ROXA.ordinal() ? JANELA_AVANCADA : JANELA_BASE;
    }

    public static double ganhoCatalogo(CategoriaFinalizacao c) {
        return c == CategoriaFinalizacao.ESTRANGULAMENTO ? GANHO_ESTRANGULAMENTO : GANHO_CHAVE;
    }
}
