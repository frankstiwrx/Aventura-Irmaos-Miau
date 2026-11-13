package com.irmaosmiau.combate;

/**
 * Estado de producao de uma tecnica cadastrada.
 *
 * JOGAVEL  -> pode ser tentada no jogo.
 * PENDENTE -> existe no catalogo (nome, ideia), mas ainda nao tem
 *             animacao/regras aprovadas; o SistemaCombate sempre a
 *             trata como INDISPONIVEL.
 */
public enum DisponibilidadeAcao {
    JOGAVEL,
    PENDENTE
}
