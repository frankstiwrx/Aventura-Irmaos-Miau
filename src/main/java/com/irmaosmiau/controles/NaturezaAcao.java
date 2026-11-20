package com.irmaosmiau.controles;

/**
 * Entrega 5Q.6: a NATUREZA de uma acao das rodas de comando (a cor e a forma do HUD).
 * A cor depende da natureza da acao, nao do botao. Cada natureza tem tambem uma
 * forma pequena ao lado do nome para nao depender so da cor.
 *
 * - DEFESA (azul, quadrado): escapar, proteger, enquadrar, defender, criar espaco, romper;
 * - TRANSICAO (verde, triangulo): passar, montar, costas, trocar de guarda, raspar, ashi, berimbolo;
 * - FINALIZACAO (vermelho, circulo): chaves e estrangulamentos;
 * - QUEDA (ambar, losango): quedas e projecoes em pe (classificacao complementar);
 * - CONTROLE (creme, sem forma: a cor de antes): ajustes, pegadas, pressao, base;
 * - DESISTENCIA (branco com moldura dourada): bater.
 */
public enum NaturezaAcao {
    DEFESA, TRANSICAO, FINALIZACAO, QUEDA, CONTROLE, DESISTENCIA
}
