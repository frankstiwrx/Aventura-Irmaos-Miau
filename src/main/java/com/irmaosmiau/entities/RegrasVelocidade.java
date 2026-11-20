package com.irmaosmiau.entities;

import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;

/**
 * Entrega 5Q.5: regras de VELOCIDADE num lugar so (balanceamento).
 *
 * A velocidade de cada lutador continua saindo dos atributos dele (a diferenca entre
 * os irmaos e preservada: {@code IrmaoMiau.calcularVelocidadeNormal}); aqui ficam os
 * FATORES por postura, por acionamento (X = acelerar), por folego e por deslocamento
 * individual x controlado (arrasto com a pegada).
 *
 * Referencia (px/tick, irmao medio ~3,9 px/tick andando):
 * em pe andando 1,0 | correndo 1,7 | agachado 0,25 | de joelhos 0,18 (acelerado 0,36) |
 * deitado 0,07 (acelerado 0,18) | arrasto em pe 1 px/tick fixo | arrasto de joelhos
 * ~0,35 px/tick (mais lento que o arrasto em pe e que andar de joelhos sozinho).
 */
public final class RegrasVelocidade {

    public static final double FATOR_CORRIDA = 1.70;
    public static final double FATOR_AGACHADO = 0.25;
    public static final double FATOR_AJOELHADO = 0.18;
    public static final double FATOR_DEITADO = 0.07;
    /** Sem folego: metade (andar) e sem aceleracao. */
    public static final double FATOR_SEM_FOLEGO = 0.50;

    /** 5Q.5: X segurado de joelhos (sem levantar: nao e corrida em pe). */
    public static final double FATOR_ACELERADO_JOELHOS = 2.0;
    /** 5Q.5: X segurado deitado (impulsos de quadril). */
    public static final double FATOR_ACELERADO_DEITADO = 2.5;
    /** 5Q.5: folego por tick acelerando (a corrida em pe gasta 0,12). */
    public static final double CUSTO_ACELERADO_JOELHOS = 0.10;
    public static final double CUSTO_ACELERADO_DEITADO = 0.10;

    /** Arrasto com a pegada em pe (px/tick, inalterado desde a 5N). */
    public static final double CONJUNTO_EM_PE = 1.0;
    /**
     * 5Q.5: arrasto com alguem de joelhos/deitado = esta fracao do deslocamento
     * INDIVIDUAL de joelhos de quem conduz (antes usava a velocidade do arrasto em pe).
     */
    public static final double FATOR_CONJUNTO_NO_CHAO = 0.55;

    private RegrasVelocidade() {
    }

    /** Fator da postura para o deslocamento individual (sem acelerar). */
    public static double fatorPostura(EstadoCorporal p) {
        return switch (p) {
            case AJOELHADO -> FATOR_AJOELHADO;
            case DEITADO -> FATOR_DEITADO;
            case AGACHADO -> FATOR_AGACHADO;
            default -> 1.0;
        };
    }

    /** Fator do X segurado nesta postura (1 = sem efeito). */
    public static double fatorAcelerado(EstadoCorporal p) {
        return switch (p) {
            case AJOELHADO -> FATOR_ACELERADO_JOELHOS;
            case DEITADO -> FATOR_ACELERADO_DEITADO;
            default -> 1.0;
        };
    }

    public static double custoAcelerado(EstadoCorporal p) {
        return p == EstadoCorporal.DEITADO ? CUSTO_ACELERADO_DEITADO : CUSTO_ACELERADO_JOELHOS;
    }

    /** O X acelera nesta postura? (em pe o X ja e a corrida.) */
    public static boolean aceleraNoChao(EstadoCorporal p) {
        return p == EstadoCorporal.AJOELHADO || p == EstadoCorporal.DEITADO;
    }

    /** Arrasto controlado (px/tick) de quem conduz, pela postura dos dois. */
    public static double conjunto(IrmaoMiau aplicador, IrmaoMiau receptor) {
        boolean noChao = aplicador.isAjoelhado() || aplicador.isDeitado() || receptor.isAjoelhado()
                || receptor.isDeitado();
        if (!noChao) {
            return CONJUNTO_EM_PE;
        }
        return Math.min(CONJUNTO_EM_PE, aplicador.getVelocidadeAjoelhado() * FATOR_CONJUNTO_NO_CHAO);
    }
}
