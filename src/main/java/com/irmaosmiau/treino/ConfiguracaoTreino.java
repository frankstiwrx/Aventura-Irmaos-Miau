package com.irmaosmiau.treino;

import com.irmaosmiau.cenario.CenarioDojo;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;

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

    /* Entrega 4D: lutadores escolhidos (oficial ou perfil). Null = oficial do id. */
    private final EscolhaLutador escolhaJogador;
    private final EscolhaLutador escolhaParceiro;
    /* Entrega 5J: postura inicial (EM_PE = como ate a 5I). */
    private final com.irmaosmiau.entities.InicioLuta inicio;

    public ConfiguracaoTreino(String idJogador, String idParceiro, String idCenario,
            int xJogador, int xParceiro, ComportamentoParceiro comportamentoParceiro) {
        this(idJogador, idParceiro, idCenario, xJogador, xParceiro, comportamentoParceiro, null, null,
                com.irmaosmiau.entities.InicioLuta.EM_PE);
    }

    private ConfiguracaoTreino(String idJogador, String idParceiro, String idCenario,
            int xJogador, int xParceiro, ComportamentoParceiro comportamentoParceiro,
            EscolhaLutador escolhaJogador, EscolhaLutador escolhaParceiro,
            com.irmaosmiau.entities.InicioLuta inicio) {
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
        this.escolhaJogador = escolhaJogador;
        this.escolhaParceiro = escolhaParceiro;
        this.inicio = inicio == null ? com.irmaosmiau.entities.InicioLuta.EM_PE : inicio;
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

    /** Mesma configuracao em outro cenario (ex.: o preferido do jogador). */
    public ConfiguracaoTreino comCenario(String idCenarioNovo) {
        return new ConfiguracaoTreino(idJogador, idParceiro, idCenarioNovo, xJogador, xParceiro,
                comportamentoParceiro, escolhaJogador, escolhaParceiro, inicio);
    }

    /**
     * Entrega 4D: jogador escolhido (oficial ou perfil). O id do jogador
     * passa a ser o do personagem-base da escolha.
     */
    public ConfiguracaoTreino comJogador(EscolhaLutador escolha) {
        if (escolha == null) {
            throw new IllegalArgumentException("A escolha do jogador e obrigatoria.");
        }
        return new ConfiguracaoTreino(escolha.getIdBase(), idParceiro, idCenario, xJogador, xParceiro,
                comportamentoParceiro, escolha, escolhaParceiro, inicio);
    }

    /** Entrega 4D: parceiro escolhido (oficial ou perfil). */
    public ConfiguracaoTreino comParceiro(EscolhaLutador escolha) {
        if (escolha == null) {
            throw new IllegalArgumentException("A escolha do parceiro e obrigatoria.");
        }
        return new ConfiguracaoTreino(idJogador, escolha.getIdBase(), idCenario, xJogador, xParceiro,
                comportamentoParceiro, escolhaJogador, escolha, inicio);
    }

    /** Null quando o jogador e o oficial do id. */
    public EscolhaLutador getEscolhaJogador() {
        return escolhaJogador;
    }

    /** Null quando o parceiro e o oficial do id. */
    public EscolhaLutador getEscolhaParceiro() {
        return escolhaParceiro;
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

    /**
     * Entrega 5J: mesma configuracao com outro inicio. DE_JOELHOS coloca a
     * dupla no centro, a {@link com.irmaosmiau.entities.InicioLuta#DISTANCIA_DE_JOELHOS}
     * (o jogador continua do lado em que estava); EM_PE volta as posicoes padrao.
     */
    public ConfiguracaoTreino comInicio(com.irmaosmiau.entities.InicioLuta novo) {
        com.irmaosmiau.entities.InicioLuta i = novo == null ? com.irmaosmiau.entities.InicioLuta.EM_PE : novo;
        boolean jogadorAEsquerda = xJogador < xParceiro;
        int xj;
        int xp;
        if (i == com.irmaosmiau.entities.InicioLuta.DE_JOELHOS) {
            int esq = com.irmaosmiau.entities.InicioLuta.xEsquerdaDeJoelhos(ESCALA);
            int dir = com.irmaosmiau.entities.InicioLuta.xDireitaDeJoelhos(ESCALA);
            xj = jogadorAEsquerda ? esq : dir;
            xp = jogadorAEsquerda ? dir : esq;
        } else {
            xj = jogadorAEsquerda ? X_JOGADOR_PADRAO : X_PARCEIRO_PADRAO;
            xp = jogadorAEsquerda ? X_PARCEIRO_PADRAO : X_JOGADOR_PADRAO;
        }
        return new ConfiguracaoTreino(idJogador, idParceiro, idCenario, xj, xp,
                comportamentoParceiro, escolhaJogador, escolhaParceiro, i);
    }

    public com.irmaosmiau.entities.InicioLuta getInicio() {
        return inicio;
    }

    public ComportamentoParceiro getComportamentoParceiro() {
        return comportamentoParceiro;
    }
}
