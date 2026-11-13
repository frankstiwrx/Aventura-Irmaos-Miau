package com.irmaosmiau.entities;

import java.util.EnumMap;
import java.util.Map;

/**
 * Efeitos temporarios de UMA instancia de lutador (Entrega 5A).
 *
 * - Duracao em ticks do loop logico (IrmaoMiau.atualizar chama {@link #atualizar()}).
 * - Reaplicar um efeito ativo SUBSTITUI a duracao (renova), nunca acumula.
 * - Nada aqui toca em AtributosLutador, perfis ou no catalogo: quando o
 *   efeito expira, o multiplicador volta a 1,0 sozinho.
 * - Uma partida nova ou o F5 do treino criam instancias novas = sem efeitos.
 */
public final class EfeitosTemporarios {

    /** Efeitos conhecidos e o que fazem enquanto ativos. */
    public enum Tipo {
        /** Fora da base: anda a 60% da velocidade normal. */
        DESEQUILIBRIO(0.60);

        private final double fatorVelocidade;

        Tipo(double fatorVelocidade) {
            this.fatorVelocidade = fatorVelocidade;
        }

        public double getFatorVelocidade() {
            return fatorVelocidade;
        }
    }

    private final Map<Tipo, Integer> restantes = new EnumMap<>(Tipo.class);

    /**
     * Aplica (ou renova) um efeito por {@code ticks} ticks.
     *
     * @throws IllegalArgumentException se ticks &lt;= 0
     */
    public void aplicar(Tipo tipo, int ticks) {
        if (tipo == null || ticks <= 0) {
            throw new IllegalArgumentException("Tipo e duracao positiva sao obrigatorios.");
        }
        restantes.put(tipo, ticks);
    }

    /** Um tick do loop logico: diminui as duracoes e remove os expirados. */
    public void atualizar() {
        restantes.replaceAll((t, n) -> n - 1);
        restantes.values().removeIf(n -> n <= 0);
    }

    public boolean isAtivo(Tipo tipo) {
        return restantes.containsKey(tipo);
    }

    /** Ticks restantes (0 se inativo). */
    public int getTicksRestantes(Tipo tipo) {
        return restantes.getOrDefault(tipo, 0);
    }

    public boolean temAlgumAtivo() {
        return !restantes.isEmpty();
    }

    /** Produto dos fatores ativos (1,0 sem efeitos). */
    public double getFatorVelocidade() {
        double fator = 1.0;
        for (Tipo t : restantes.keySet()) {
            fator *= t.getFatorVelocidade();
        }
        return fator;
    }

    public void limpar() {
        restantes.clear();
    }
}
