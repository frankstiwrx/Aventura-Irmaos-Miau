package com.irmaosmiau.combate;

/** Resultado de uma acao tecnica durante a pegada (Entrega 5A). */
public enum ResultadoAcaoTecnica {
    /** Disputa vencida por quem agiu. */
    SUCESSO,
    /** Disputa vencida pelo outro lado (inclui empate). */
    RESISTIDO,
    /** Folego insuficiente: nada foi cobrado nem disputado. */
    SEM_FOLEGO,
    /** Sem pegada estabelecida, ainda na entrada, ou lutador no papel errado. */
    SEM_PEGADA,
    /** Acao tecnica recente: aguardar a recarga. */
    EM_RECARGA
}
