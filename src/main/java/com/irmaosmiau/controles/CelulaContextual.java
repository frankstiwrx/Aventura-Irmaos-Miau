package com.irmaosmiau.controles;

/**
 * O que uma das 12 posicoes faz no contexto atual (Entrega 5F).
 *
 * @param tipo            EXECUTAVEL = aciona uma acao existente pelo SistemaCombate;
 *                        RESERVADA = transicao modelada na 5E, ainda nao jogavel
 * @param acao            acao executada (so EXECUTAVEL)
 * @param idTransicao     transicao da 5E (so RESERVADA)
 * @param disponivelAgora previsao para o HUD (recarga, alcance, firmeza); quem
 *                        decide de verdade continua sendo o SistemaCombate
 */
public record CelulaContextual(PosicaoComando posicao, Tipo tipo, String rotulo, AcaoJogador acao,
        String idTransicao, boolean disponivelAgora, NaturezaAcao natureza) {

    /** Sem natureza informada (o HUD usa a cor de antes / a regra do painel). */
    public CelulaContextual(PosicaoComando posicao, Tipo tipo, String rotulo, AcaoJogador acao, String idTransicao,
            boolean disponivelAgora) {
        this(posicao, tipo, rotulo, acao, idTransicao, disponivelAgora, null);
    }

    /** 5Q.6: a mesma celula com a natureza (cor semantica) da acao. */
    public CelulaContextual comNatureza(NaturezaAcao n) {
        return new CelulaContextual(posicao, tipo, rotulo, acao, idTransicao, disponivelAgora, n);
    }

    public enum Tipo {
        EXECUTAVEL,
        RESERVADA
    }

    public boolean isExecutavel() {
        return tipo == Tipo.EXECUTAVEL;
    }
}
