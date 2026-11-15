package com.irmaosmiau.entities;

/**
 * Entrega 5O: estado MECANICO das guardas sustentadas de um lutador (nao e so
 * uma pose). Os golpes de impacto ainda nao existem; quando existirem, eles
 * perguntam {@link #protege(Nivel)} para decidir bloqueio, dano e
 * desequilibrio. Hoje nenhuma reducao de dano e simulada.
 *
 * Ciclo: NENHUMA -> ENTRANDO ({@link #TICKS_ENTRADA}) -> ATIVA (enquanto o
 * comando for mantido) -> SAINDO ({@link #TICKS_SAIDA}) -> NENHUMA. Trocar de
 * guarda no meio passa de uma pose para a outra (ENTRANDO de novo, a partir da
 * anterior). Interromper (pegada, queda, rolamento...) zera na hora.
 */
public final class DefesaCorporal {

    /** Regiao protegida pela guarda (consultada pelos futuros golpes). */
    public enum Nivel { ALTO, MEDIO, BAIXO }

    public enum Tipo {
        GUARDA_ALTA(Nivel.ALTO, "GUARDA ALTA"),
        GUARDA_MEDIA(Nivel.MEDIO, "GUARDA MÉDIA"),
        GUARDA_BAIXA(Nivel.BAIXO, "GUARDA BAIXA");

        private final Nivel nivel;
        private final String rotulo;

        Tipo(Nivel nivel, String rotulo) {
            this.nivel = nivel;
            this.rotulo = rotulo;
        }

        public Nivel getNivel() {
            return nivel;
        }

        public String getRotulo() {
            return rotulo;
        }
    }

    public enum Fase { NENHUMA, ENTRANDO, ATIVA, SAINDO }

    /** Tempo de entrada na guarda (ticks de 16 ms, ~0,1 s). */
    /**
     * 5O.1: velocidade de caminhada enquanto uma guarda (alta, media ou baixa)
     * esta segurada, como fracao da velocidade normal da postura. Sem corrida.
     * Ponto unico de ajuste.
     */
    public static final double MULTIPLICADOR_VELOCIDADE = 0.5;

    public static final int TICKS_ENTRADA = 6;
    /** Tempo de saida para a postura normal (~0,13 s). */
    public static final int TICKS_SAIDA = 8;

    private Tipo tipo;
    private Tipo anterior;
    private Fase fase = Fase.NENHUMA;
    private int ticksNaFase;
    private long ticksAtiva;

    /**
     * Mantem (ou comeca) a guarda pedida neste tick.
     *
     * @return true se a guarda COMECOU agora (para cobrar a entrada uma vez)
     */
    public boolean manter(Tipo pedido) {
        if (pedido == null) {
            soltar();
            return false;
        }
        if (fase == Fase.NENHUMA || fase == Fase.SAINDO) {
            anterior = fase == Fase.SAINDO ? tipo : null;
            tipo = pedido;
            fase = Fase.ENTRANDO;
            ticksNaFase = 0;
            return true;
        }
        if (pedido != tipo) {
            anterior = tipo;                       // troca de guarda: passa de uma para a outra
            tipo = pedido;
            fase = Fase.ENTRANDO;
            ticksNaFase = 0;
            return true;
        }
        return false;
    }

    /** Larga a guarda (saida suave). */
    public void soltar() {
        if (fase == Fase.ENTRANDO || fase == Fase.ATIVA) {
            fase = Fase.SAINDO;
            ticksNaFase = 0;
            anterior = null;
        }
    }

    /** Fim imediato (agarrado, caiu, rolou...): sem pose presa. */
    public void interromper() {
        fase = Fase.NENHUMA;
        tipo = null;
        anterior = null;
        ticksNaFase = 0;
        ticksAtiva = 0;
    }

    /** Um tick logico. */
    public void atualizar() {
        switch (fase) {
            case ENTRANDO -> {
                if (++ticksNaFase >= TICKS_ENTRADA) {
                    fase = Fase.ATIVA;
                    ticksNaFase = 0;
                    anterior = null;
                }
            }
            case ATIVA -> {
                ticksNaFase++;
                ticksAtiva++;
            }
            case SAINDO -> {
                if (++ticksNaFase >= TICKS_SAIDA) {
                    interromper();
                }
            }
            default -> {
            }
        }
    }

    /** A guarda protege este nivel AGORA (so na fase ATIVA; entrando ainda nao fecha). */
    public boolean protege(Nivel nivel) {
        return fase == Fase.ATIVA && tipo != null && tipo.getNivel() == nivel;
    }

    public boolean isAtiva() {
        return fase != Fase.NENHUMA;
    }

    /** Comando valendo (entrando ou ativa), para travar a locomocao. */
    public boolean isSegurada() {
        return fase == Fase.ENTRANDO || fase == Fase.ATIVA;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** Guarda de onde a entrada parte (troca), ou null (parte da postura normal). */
    public Tipo getAnterior() {
        return anterior;
    }

    public Fase getFase() {
        return fase;
    }

    /** 0..1 dentro da fase de entrada ou saida. */
    public double getProgressoFase() {
        return switch (fase) {
            case ENTRANDO -> Math.min(1.0, ticksNaFase / (double) TICKS_ENTRADA);
            case SAINDO -> Math.min(1.0, ticksNaFase / (double) TICKS_SAIDA);
            case ATIVA -> 1.0;
            default -> 0.0;
        };
    }

    /** Ticks na guarda ativa (relogio da respiracao; nao recomeca a cada tick). */
    public long getTicksAtiva() {
        return ticksAtiva;
    }
}
