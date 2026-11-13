package com.irmaosmiau.controles;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Papel de um lutador no estado ATUAL do combate (Entrega 5C).
 * Somente leitura do SistemaCombate; nao guarda estado proprio.
 */
public enum PapelCombate {
    /** Sem interacao (ou sem motor, no modo individual). */
    LIVRE,
    /** Pegada nos 30 quadros de entrada: ninguem age. */
    ENTRANDO,
    /** Quem aplicou a pegada estabelecida. */
    APLICADOR,
    /** Quem recebeu a pegada estabelecida. */
    RECEPTOR;

    public static PapelCombate de(SistemaCombate sistema, IrmaoMiau lutador) {
        if (sistema == null || lutador == null || !sistema.estaEmInteracao()) {
            return LIVRE;
        }
        if (!sistema.isInteracaoEstabelecida()) {
            return ENTRANDO;
        }
        if (sistema.getAplicadorInteracao() == lutador) {
            return APLICADOR;
        }
        return sistema.getReceptorInteracao() == lutador ? RECEPTOR : LIVRE;
    }
}
