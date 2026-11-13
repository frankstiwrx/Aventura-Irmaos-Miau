package com.irmaosmiau.personagens;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Cria a instancia jogavel de um personagem com atributos e nome de
 * exibicao proprios (perfil personalizado), reaproveitando o mesmo
 * desenho e as mesmas animacoes do personagem-base.
 */
@FunctionalInterface
public interface FabricaLutadorPersonalizado {

    IrmaoMiau criar(int x, int y, double escala, AtributosLutador atributos, String nomeExibicao);
}
