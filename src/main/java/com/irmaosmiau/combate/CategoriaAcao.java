package com.irmaosmiau.combate;

/**
 * Categorias amplas entendidas pelo motor de combate.
 *
 * Golpes concretos ("double_leg", "pegada_1", etc.) nao viram enum:
 * eles serao conteudo cadastrado pelo RegistroAcoes.
 */
public enum CategoriaAcao {
    PEGADA,
    IMPACTO,
    QUEDA,
    CONTROLE,
    PASSAGEM,
    RASPAGEM,
    SUBMISSAO,
    ESCAPE
}
