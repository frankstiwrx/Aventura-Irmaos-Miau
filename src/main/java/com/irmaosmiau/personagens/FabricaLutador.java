package com.irmaosmiau.personagens;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Cria a instancia jogavel de um personagem.
 *
 * E uma interface funcional: um personagem novo pode ser cadastrado com um
 * lambda que reaproveita qualquer renderer existente (ou futuro), sem que
 * o catalogo precise conhecer a classe concreta.
 */
@FunctionalInterface
public interface FabricaLutador {

    IrmaoMiau criar(int x, int y, double escala);
}
