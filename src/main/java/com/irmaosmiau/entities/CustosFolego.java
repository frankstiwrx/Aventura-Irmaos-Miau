package com.irmaosmiau.entities;

/**
 * Entrega 5K: custos de folego do CORPO num lugar so (facil de ajustar).
 *
 * Os custos de combate ja existentes (pegada 3, reforco 3, desequilibrio e
 * rompimento) continuam onde estavam (tecnicas.json e SistemaCombate); aqui
 * entram apenas os NOVOS: caminhada, transicoes de postura e o esforco do
 * arrasto. Escala: a do folego atual (Branco e Preto tem maximos diferentes,
 * definidos pelos atributos).
 *
 * Referencia de balanceamento: caminhar em pe por 10 s logicos, na
 * velocidade normal do proprio lutador, gasta ~1 ponto.
 */
public final class CustosFolego {

    /** Ticks logicos por segundo (GamePanel: 16 ms por tick). */
    public static final double TICKS_POR_SEGUNDO = 1000.0 / 16.0;

    /** Caminhar em pe na velocidade normal: pontos por segundo. */
    public static final double CAMINHADA_POR_SEGUNDO = 0.10;

    /*
     * A caminhada e cobrada pela DISTANCIA (velocidade normal = referencia).
     * De joelhos anda a 18% da velocidade; o fator por distancia faz o custo
     * POR TEMPO ficar em ~45% do de caminhar em pe (esforco real, sem punir
     * a postura mais usada do jogo). Deitado (7% da velocidade): ~35%.
     */
    public static final double FATOR_DISTANCIA_EM_PE = 1.0;
    // 5P.1: andar de joelhos / rastejar mais barato (antes 2,5 e 5,0)
    public static final double FATOR_DISTANCIA_AJOELHADO = 2.0;
    public static final double FATOR_DISTANCIA_DEITADO = 3.5;

    /**
     * Arrasto (so o APLICADOR paga): por pixel que a dupla realmente andou.
     * Inclui o "andar" do aplicador (ele nao usa a caminhada normal) e o
     * esforco extra de levar o outro: a 1 px/tick ~0,16 por segundo, ~1,6x
     * caminhar em pe.
     */
    public static final double ARRASTO_POR_PIXEL = 0.0015;            // 5P.1: antes 0,0025

    // ---------------- Entrega 5L: defesa
    /** Receptor preso que forca para sair (por tick): ~0,25/s. */
    public static final double RESISTENCIA_POR_TICK = 0.0025;         // 5P.1: antes 0,004
    /** Abaixar (agachar / de joelhos) a tempo e evitar a pegada: custo unico. */
    public static final double EVASAO_ABAIXAR = 1.5;
    /** Quem tentou a pegada evitada paga esta fracao do custo da tecnica (gastou a tentativa). */
    public static final double FRACAO_CUSTO_PEGADA_EVITADA = 0.5;

    // ---------------- Entrega 5O: defesas (os custos explosivos ficam no IrmaoMiau/TipoAcrobacia)
    /**
     * Entrar numa guarda (RT + direcao): custo unico pequeno, abaixo de ajoelhar
     * (0,5). Trocar de guarda custa a metade. Mantida, a guarda nao custa por
     * tick, mas segurar a defesa e esforco: o folego NAO recupera enquanto ela
     * estiver ativa (politica sustentavel, sem valor negativo).
     */
    public static final double GUARDA_ENTRADA = 0.3;
    public static final double GUARDA_TROCA = 0.15;
    /**
     * 5O.1: andar com a guarda cobra a caminhada normal proporcional a distancia
     * vezes este fator (1,0 = o mesmo custo por pixel de andar; como a guarda anda
     * a metade, custa a metade por segundo). Nunca recupera folego enquanto anda.
     */
    public static final double FATOR_DISTANCIA_GUARDA = 1.0;

    // ---------------- Entrega 5O.2: respiracao por postura (RT sozinho ou a tecla de respirar)
    /*
     * Recuperacao ao respirar, como fracao da recuperacao em pe (a referencia:
     * 15x a recuperacao parada). Vale para folego e condicionamento.
     */
    public static final double RECUPERACAO_RESPIRANDO_EM_PE = 1.0;
    public static final double RECUPERACAO_RESPIRANDO_AJOELHADO = 0.7;
    public static final double RECUPERACAO_RESPIRANDO_DEITADO = 0.4;
    /*
     * Velocidade andando enquanto respira, como fracao da velocidade NORMAL (uma
     * penalidade so, nao multiplicada pela da postura): em pe 0,15 (a de sempre);
     * de joelhos 0,08 (andar de joelhos e 0,18); deitado 0,03 (rastejar e 0,07).
     */
    public static final double VELOCIDADE_RESPIRANDO_EM_PE = 0.15;
    public static final double VELOCIDADE_RESPIRANDO_AJOELHADO = 0.08;
    public static final double VELOCIDADE_RESPIRANDO_DEITADO = 0.03;

    /** Fator de recuperacao ao respirar na postura. */
    public static double recuperacaoRespirando(IrmaoMiau.EstadoCorporal postura) {
        return switch (postura) {
            case AJOELHADO -> RECUPERACAO_RESPIRANDO_AJOELHADO;
            case DEITADO -> RECUPERACAO_RESPIRANDO_DEITADO;
            default -> RECUPERACAO_RESPIRANDO_EM_PE;
        };
    }

    /** Velocidade ao respirar na postura (fracao da velocidade normal). */
    public static double velocidadeRespirando(IrmaoMiau.EstadoCorporal postura) {
        return switch (postura) {
            case AJOELHADO -> VELOCIDADE_RESPIRANDO_AJOELHADO;
            case DEITADO -> VELOCIDADE_RESPIRANDO_DEITADO;
            default -> VELOCIDADE_RESPIRANDO_EM_PE;
        };
    }

    // ---------------- Entrega 5P.1: balanceamento do combate (um lugar so)
    /** Pegadas (custos do tecnicas.json): x 0,6 (pegada 1: 3,0 -> 1,8). */
    public static final double FATOR_CUSTO_PEGADAS = 0.6;
    /** Tecnicas de queda e de solo (CatalogoTecnicasSolo): x 0,5. */
    public static final double FATOR_CUSTO_SOLO = 0.5;
    /**
     * Folego de referencia (media entre Branco 25 e Preto 40): o custo de solo e
     * escalado pela RAIZ de (folego maximo / referencia), para a mesma sequencia
     * nao pesar desproporcionalmente em quem tem menos folego, mantendo a diferenca.
     */
    public static final double FOLEGO_REFERENCIA = 32.5;

    /** Custo de uma tecnica de solo para um lutador com este folego maximo. */
    public static double custoSolo(double custoBase, double folegoMaximo) {
        return custoBase * FATOR_CUSTO_SOLO * Math.sqrt(Math.max(1.0, folegoMaximo) / FOLEGO_REFERENCIA);
    }

    // ---------------- Entrega 5P.1: recuperacao por contexto (evolui o bloqueio absoluto da 5O.3)
    /*
     * Parado e livre: recuperacao passiva normal. Respirando: 15x a passiva x postura
     * (5O.2) x o fator do contexto abaixo. Ligado (pegada ou solo) NAO ha recuperacao
     * passiva: so respirando de proposito, e bem menos.
     */
    public static final double RESPIRACAO_NA_PEGADA = 0.25;
    public static final double RESPIRACAO_SOLO_POR_CIMA = 0.35;
    /** Por baixo: de 0,35 (controle fraco) ate 0,15 (controle total do outro). */
    public static final double RESPIRACAO_SOLO_POR_BAIXO_MAX = 0.35;
    public static final double RESPIRACAO_SOLO_POR_BAIXO_MIN = 0.15;
    /** Respirando enquanto anda: metade. */
    public static final double RESPIRACAO_ANDANDO = 0.5;
    /** Respirando no meio da propria tecnica (janela): nada. */
    public static final double RESPIRACAO_EM_ACAO = 0.0;

    // ---------------- Transicoes de postura (custo unico por transicao)
    public static final double AJOELHAR = 0.5;            // em pe -> ajoelhado
    public static final double DEITAR = 0.5;              // ajoelhado -> deitado
    public static final double ERGUER_JOELHOS = 0.8;      // deitado -> ajoelhado
    public static final double LEVANTAR = 1.2;            // ajoelhado -> em pe
    /** Rapidas (duplo clique): sempre acima da soma das etapas lentas. */
    public static final double RAPIDA_DEITAR = 2.5;       // em pe -> deitado      (lento: 0,5 + 0,5)
    public static final double RAPIDA_LEVANTAR_DO_CHAO = 4.0; // deitado -> em pe  (lento: 0,8 + 1,2)
    public static final double RAPIDA_LEVANTAR = 2.0;     // ajoelhado -> em pe    (lento: 1,2)

    private CustosFolego() {
    }

    /** Custo de caminhada por distancia percorrida (px), pela postura. */
    public static double caminhada(double distanciaPx, double velocidadeNormalPxPorTick,
            IrmaoMiau.EstadoCorporal postura) {
        if (distanciaPx <= 0 || velocidadeNormalPxPorTick <= 0) {
            return 0.0;
        }
        double fator = switch (postura) {
            case AJOELHADO -> FATOR_DISTANCIA_AJOELHADO;
            case DEITADO -> FATOR_DISTANCIA_DEITADO;
            default -> FATOR_DISTANCIA_EM_PE;
        };
        double porTickNaVelocidadeNormal = CAMINHADA_POR_SEGUNDO / TICKS_POR_SEGUNDO;
        return porTickNaVelocidadeNormal * (distanciaPx / velocidadeNormalPxPorTick) * fator;
    }

    // ---------------- Entrega 5P.2: CONDICIONAMENTO (desgaste fisico, mais lento que o folego)
    /*
     * Folego = energia de curto prazo (respira e volta). Condicionamento = resistencia
     * da luta inteira: cai devagar e volta devagar (a recuperacao passiva livre, que ja
     * existia). Tudo proporcional ao condicionamento maximo de cada Miau (referencia 30),
     * cobrado num lugar so por acao (sem cobranca dupla).
     */
    public static final double CONDICIONAMENTO_REFERENCIA = 30.0;
    /** Fracao do custo de folego que a tecnica tambem tira do condicionamento. */
    public static final double FATOR_COND_TECNICA = 0.10;
    /** Tecnicas explosivas (R1, quedas, tiro de guarda) desgastam mais. */
    public static final double FATOR_COND_EXPLOSIVA = 0.30;
    /** Ser derrubado (impacto), uma vez por queda. */
    public static final double COND_IMPACTO_QUEDA = 0.8;
    /** Por tick, preso sob controle dominante (x estabilidade/100). */
    public static final double COND_DRENO_DOMINADO = 0.003;
    /** Por tick, mantendo a pressao de uma posicao dominante. */
    public static final double COND_DRENO_PRESSAO = 0.0012;
    /** Abaixo desta fracao do condicionamento os efeitos da exaustao comecam (graduais). */
    public static final double LIMIAR_EXAUSTAO = 0.35;
    /** Exaustao critica (preparada para regras futuras; nao encerra a luta). */
    public static final double LIMIAR_EXAUSTAO_CRITICA = 0.05;
    /** Na exaustao total: janelas +40%, ganho de controle -50%, respiracao -40%, explosivas +30% de custo. */
    public static final double EXAUSTAO_JANELA = 0.4;
    public static final double EXAUSTAO_GANHO_CONTROLE = 0.5;
    public static final double EXAUSTAO_RESPIRACAO = 0.4;
    public static final double EXAUSTAO_CUSTO_EXPLOSIVO = 0.3;

    private static double escalaCond(double condicionamentoMaximo) {
        return Math.max(1.0, condicionamentoMaximo) / CONDICIONAMENTO_REFERENCIA;
    }

    /** Condicionamento gasto por uma tecnica de solo (custo ja e o de folego deste lutador). */
    public static double condicionamentoDaTecnica(double custoFolego, boolean explosiva) {
        return custoFolego * (explosiva ? FATOR_COND_EXPLOSIVA : FATOR_COND_TECNICA);
    }

    public static double impactoQueda(double condicionamentoMaximo) {
        return COND_IMPACTO_QUEDA * escalaCond(condicionamentoMaximo);
    }

    public static double drenoDominado(double estabilidade, double condicionamentoMaximo) {
        return COND_DRENO_DOMINADO * Math.max(0, Math.min(100, estabilidade)) / 100.0 * escalaCond(condicionamentoMaximo);
    }

    public static double drenoPressao(double condicionamentoMaximo) {
        return COND_DRENO_PRESSAO * escalaCond(condicionamentoMaximo);
    }

    /** 0 = descansado (acima de 35%); 1 = condicionamento zerado. Gradual. */
    public static double fatorExaustao(double condicionamentoAtual, double condicionamentoMaximo) {
        double f = condicionamentoAtual / Math.max(1.0, condicionamentoMaximo);
        return Math.max(0, Math.min(1, (LIMIAR_EXAUSTAO - f) / LIMIAR_EXAUSTAO));
    }
}
