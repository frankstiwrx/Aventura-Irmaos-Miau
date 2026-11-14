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
        String idTransicao, boolean disponivelAgora) {

    public enum Tipo {
        EXECUTAVEL,
        RESERVADA
    }

    public boolean isExecutavel() {
        return tipo == Tipo.EXECUTAVEL;
    }
}
