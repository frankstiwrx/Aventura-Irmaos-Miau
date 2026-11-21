package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.regras.ModoLuta;
import com.irmaosmiau.regras.ModoRegras;

import org.junit.jupiter.api.Test;

/** O modo de luta das configuracoes chega ao motor; um modo EM BREVE nunca entra na partida. */
class ModoLutaGamePanelTest extends Base5OGamePanel {

    @Test
    void modoDaConfiguracaoChegaAoMotor() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            assertEquals(ModoLuta.FINALIZACAO, painel.getModoLuta(), "padrao");
            painel.configurarModoLuta(ModoLuta.FINALIZACAO);
            assertEquals(ModoRegras.LIVRE, painel.getMotorSolo().getModoRegras());
            // sistema de regras (pedido): PONTOS + FINALIZACAO ficou disponivel; o ARCADE continua EM BREVE
            painel.configurarModoLuta(ModoLuta.PONTOS_E_FINALIZACAO);
            assertEquals(ModoLuta.PONTOS_E_FINALIZACAO, painel.getModoLuta());
            assertEquals(ModoRegras.LIVRE, painel.getMotorSolo().getModoRegras());
            painel.configurarModoLuta(ModoLuta.ARCADE);
            assertEquals(ModoLuta.FINALIZACAO, painel.getModoLuta(), "EM BREVE nao entra");
            painel.configurarModoLuta(null);
            assertEquals(ModoLuta.FINALIZACAO, painel.getModoLuta());
        });
    }
}
