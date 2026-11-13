package com.irmaosmiau.treino;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Como o parceiro de treino age a cada tick.
 *
 * Primeira versao: {@link #PASSIVO}. Futuras versoes (defesa simples,
 * IA de treino) entram como novas implementacoes, sem mexer no GamePanel.
 */
@FunctionalInterface
public interface ComportamentoParceiro {

    /** Chamado uma vez por tick, no lugar do input do segundo jogador. */
    void atualizar(IrmaoMiau parceiro);

    /**
     * Parceiro parado: nao anda, nao ataca, nao muda de estado corporal.
     * Apenas segue vivo (respiracao/idle/recuperacao de folego) e reage
     * ao motor de combate (poses da PEGADA_1).
     */
    ComportamentoParceiro PASSIVO = parceiro -> {
        parceiro.setCorrendo(false);
        parceiro.setRespirando(false);
        parceiro.atualizar(false, false);
    };
}
