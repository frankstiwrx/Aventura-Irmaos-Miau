package com.irmaosmiau.animacao;

/**
 * Resultado de uma resolucao (Entrega 5E).
 *
 * @param definicao      animacao escolhida
 * @param perfilDeOrigem perfil que a forneceu (o proprio ou um base)
 * @param herdada        true se veio do perfil base (compartilhada)
 */
public record AnimacaoResolvida(DefinicaoAnimacao definicao, String perfilDeOrigem, boolean herdada) {

    /** O renderizador atual (codigo legado) e quem desenha? */
    public boolean usaCodigoLegado() {
        return definicao.fonte() != FonteAnimacao.JSON_INDIVIDUAL;
    }
}
