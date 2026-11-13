package com.irmaosmiau.combate;

import java.util.Optional;

/**
 * Decide, a cada tick, se uma interacao persistente continua.
 *
 * O SistemaCombate chama a politica; quando ela devolve um motivo, o
 * sistema encerra a interacao e limpa poses e bloqueios dos dois lados.
 * Trocar a politica (ex.: futura movimentacao conjunta agarrada) nao
 * exige mexer no GamePanel nem nos personagens.
 */
@FunctionalInterface
public interface PoliticaInteracao {

    /**
     * @return vazio para manter a interacao; um motivo para encerra-la
     */
    Optional<MotivoEncerramentoInteracao> avaliar(SituacaoInteracao situacao);
}
