package com.irmaosmiau.treino;

import com.irmaosmiau.cenario.CenarioDojo;
import com.irmaosmiau.personagens.CatalogoPersonagens;

/**
 * Parametros de uma sessao de treino.
 *
 * O jogador usa os controles do "lado 1" (WASD, H = PEGADA_1). O parceiro
 * e controlado por um {@link ComportamentoParceiro}. Personagens e cenario
 * sao referenciados por id dos catalogos, para aceitar outros no futuro.
 */
public final class ConfiguracaoTreino {

    public static final int X_JOGADOR_PADRAO = 260;
    public static final int X_PARCEIRO_PADRAO = 470;
    public static final int Y_INICIAL = 150;
    public static final double ESCALA = 0.7;

    private final String idJogador;
    private final String idParceiro;
    private final String idCenario;
    private final int xJogador;
    private final int xParceiro;
    private final ComportamentoParceiro comportamentoParceiro;

    public ConfiguracaoTreino(String idJogador, String idParceiro, String idCenario,
            int xJogador, int xParceiro, ComportamentoParceiro comportamentoParceiro) {
        if (idJogador == null || idParceiro == null || idCenario == null || comportamentoParceiro == null) {
            throw new IllegalArgumentException("Jogador, parceiro, cenario e comportamento sao obrigatorios.");
        }
        if (xJogador == xParceiro) {
            throw new IllegalArgumentException("Jogador e parceiro nao podem comecar no mesmo X.");
        }
        this.idJogador = idJogador;
        this.idParceiro = idParceiro;
        this.idCenario = idCenario;
        this.xJogador = xJogador;
        this.xParceiro = xParceiro;
        this.comportamentoParceiro = comportamentoParceiro;
    }

    /** Branco (jogador) x Preto (parceiro passivo) no dojo. */
    public static ConfiguracaoTreino padrao() {
        return new ConfiguracaoTreino(
                CatalogoPersonagens.ID_BRANCO,
                CatalogoPersonagens.ID_PRETO,
                CenarioDojo.ID,
                X_JOGADOR_PADRAO,
                X_PARCEIRO_PADRAO,
                ComportamentoParceiro.PASSIVO);
    }

    public String getIdJogador() {
        return idJogador;
    }

    public String getIdParceiro() {
        return idParceiro;
    }

    public String getIdCenario() {
        return idCenario;
    }

    public int getXJogador() {
        return xJogador;
    }

    public int getXParceiro() {
        return xParceiro;
    }

    public ComportamentoParceiro getComportamentoParceiro() {
        return comportamentoParceiro;
    }
}
