package com.irmaosmiau.combate;

/**
 * Por que uma interacao persistente (ex.: PEGADA_1) terminou.
 *
 * Usado pelo SistemaCombate, pela PoliticaInteracao, pelos testes e pelo
 * debug na tela. Nao influencia nenhuma regra por si so.
 */
public enum MotivoEncerramentoInteracao {

    /* Espaciais (ja existiam antes da Entrega 3A). */
    ALCANCE,
    LINHAS,
    ORIENTACAO,

    /* Mudancas corporais depois da pegada estabelecida (Entrega 3A). */
    MOVIMENTO,
    PULO,
    AGACHAMENTO,
    DEITAR_OU_LEVANTAR,
    ROLAMENTO,
    SPRAWL,

    /* Acoes tecnicas (Entrega 5A): receptor soltou a pegada sem se deslocar. */
    ROMPIMENTO_TECNICO,

    /* Entrega 5B: firmeza da pegada chegou a zero. */
    SEM_CONTROLE,

    /* Administrativos. */
    ACAO_INDISPONIVEL,
    ENCERRAMENTO_MANUAL
}
