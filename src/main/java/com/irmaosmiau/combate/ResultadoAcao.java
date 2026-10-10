package com.irmaosmiau.combate;

/**
 * Resultado logico de uma tentativa de acao de combate.
 *
 * Nesta primeira fundacao o enum apenas define o vocabulario do motor.
 * A resolucao real entra quando o SistemaCombate for conectado.
 */
public enum ResultadoAcao {
    INDISPONIVEL,
    FALHA,
    DEFENDIDO,
    SUCESSO_PARCIAL,
    SUCESSO
}
