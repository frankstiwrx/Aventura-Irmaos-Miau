package com.irmaosmiau.combate;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Entrega 5L: algo defensivo que o MOTOR confirmou (as animacoes so reagem a
 * isto, nunca ao botao apertado).
 *
 * @param tipo       o que aconteceu
 * @param defensor   quem defendeu (ou tentou)
 * @param oponente   quem atacava / segurava
 * @param tick       tick do SistemaCombate
 * @param sequencia  cresce a cada evento (para detectar um novo)
 * @param detalhe    ex.: "AGACHAMENTO", "ROLAMENTO", "PULO"
 */
public record EventoDefesa(Tipo tipo, IrmaoMiau defensor, IrmaoMiau oponente, long tick, long sequencia,
        String detalhe) {

    public enum Tipo {
        /** A pegada foi evitada antes do contato (agachamento rapido, rolamento ou pulo). */
        PEGADA_EVITADA,
        /** ROMPER funcionou: a pegada acabou. */
        ROMPIMENTO_SUCESSO,
        /** ROMPER foi resistido: a pegada continua (defesa fracassada). */
        ROMPIMENTO_FALHOU
    }
}
