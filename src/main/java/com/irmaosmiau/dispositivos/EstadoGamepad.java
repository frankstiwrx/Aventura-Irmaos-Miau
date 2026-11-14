package com.irmaosmiau.dispositivos;

import java.util.Arrays;

/**
 * Leitura instantanea de um gamepad (Entrega 5D). Imutavel: o provedor
 * fisico publica um novo objeto a cada leitura e o jogo le sem travar.
 */
public final class EstadoGamepad {

    /** Limiar para um gatilho analogico contar como "pressionado". */
    public static final float LIMIAR_GATILHO = 0.5f;

    private static final EstadoGamepad REPOUSO = new EstadoGamepad(
            new boolean[BotaoGamepad.FISICOS], new float[EixoGamepad.values().length]);

    private final boolean[] botoes;
    private final float[] eixos;

    private EstadoGamepad(boolean[] botoes, float[] eixos) {
        this.botoes = botoes;
        this.eixos = eixos;
    }

    /** Nada apertado, analogicos no centro, gatilhos soltos. */
    public static EstadoGamepad repouso() {
        return REPOUSO;
    }

    /**
     * A partir de vetores crus (ordem de {@link BotaoGamepad} e {@link EixoGamepad}).
     * Valores fora de faixa sao limitados; NaN vira 0.
     */
    public static EstadoGamepad de(boolean[] botoes, float[] eixos) {
        boolean[] b = Arrays.copyOf(botoes, BotaoGamepad.FISICOS);
        float[] e = new float[EixoGamepad.values().length];
        for (int i = 0; i < e.length && i < eixos.length; i++) {
            float v = Float.isNaN(eixos[i]) ? 0f : eixos[i];
            boolean gatilho = i >= EixoGamepad.GATILHO_ESQ.ordinal();
            e[i] = Math.max(gatilho ? 0f : -1f, Math.min(1f, v));
        }
        return new EstadoGamepad(b, e);
    }

    /** Copia com o botao apertado (testes e gamepad simulado). */
    public EstadoGamepad com(BotaoGamepad botao) {
        if (botao.isVirtual()) {
            return comEixo(botao == BotaoGamepad.GATILHO_ESQ ? EixoGamepad.GATILHO_ESQ : EixoGamepad.GATILHO_DIR, 1f);
        }
        boolean[] b = botoes.clone();
        b[botao.ordinal()] = true;
        return new EstadoGamepad(b, eixos);
    }

    public EstadoGamepad comEixo(EixoGamepad eixo, float valor) {
        float[] e = eixos.clone();
        e[eixo.ordinal()] = valor;
        return de(botoes, e);
    }

    public boolean pressionado(BotaoGamepad botao) {
        return switch (botao) {
            case GATILHO_ESQ -> eixo(EixoGamepad.GATILHO_ESQ) >= LIMIAR_GATILHO;
            case GATILHO_DIR -> eixo(EixoGamepad.GATILHO_DIR) >= LIMIAR_GATILHO;
            default -> botoes[botao.ordinal()];
        };
    }

    public float eixo(EixoGamepad eixo) {
        return eixos[eixo.ordinal()];
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EstadoGamepad e && Arrays.equals(botoes, e.botoes) && Arrays.equals(eixos, e.eixos);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(botoes) + Arrays.hashCode(eixos);
    }
}
