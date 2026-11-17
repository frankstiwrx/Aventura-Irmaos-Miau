package com.irmaosmiau.regras;

import com.irmaosmiau.entities.AtributosLutador;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Entrega 5Q.1: atributos por graduacao, em EVOLUCAO COMPOSTA a partir da faixa
 * branca (= os atributos originais do personagem, sem mudanca).
 *
 * Cada promocao multiplica o valor da faixa ANTERIOR e arredonda para baixo
 * NAQUELA promocao: novo = floor(anterior x multiplicador). Ex. (40): 40 -> 48 ->
 * 56 -> 66 -> 75. O PESO nunca muda. A conta e exata (BigDecimal), sem o erro de
 * ponto flutuante que poderia jogar um 60 para 59.
 *
 * Os atributos-base nunca sao alterados: o resultado e sempre um objeto novo,
 * entao voltar para a branca restaura exatamente o original e reaplicar a mesma
 * faixa nao soma bonus.
 */
public final class EvolucaoGraduacao {

    /** Multiplicador da promocao PARA esta faixa (vindo da anterior). */
    public static final Map<Graduacao, BigDecimal> MULTIPLICADORES;

    static {
        Map<Graduacao, BigDecimal> m = new EnumMap<>(Graduacao.class);
        m.put(Graduacao.AZUL, new BigDecimal("1.20"));
        m.put(Graduacao.ROXA, new BigDecimal("1.17"));
        m.put(Graduacao.MARROM, new BigDecimal("1.18"));
        m.put(Graduacao.PRETA, new BigDecimal("1.15"));
        MULTIPLICADORES = Collections.unmodifiableMap(m);
    }

    private EvolucaoGraduacao() {
    }

    /** Um valor promovido ate a faixa g (arredondando para baixo a cada promocao). */
    public static double evoluir(double valorBranca, Graduacao g) {
        BigDecimal v = BigDecimal.valueOf(valorBranca);
        for (Graduacao x : Graduacao.values()) {
            if (x == Graduacao.BRANCA) {
                continue;
            }
            if (x.ordinal() > g.ordinal()) {
                break;
            }
            v = v.multiply(MULTIPLICADORES.get(x)).setScale(0, RoundingMode.FLOOR);
        }
        return v.doubleValue();
    }

    /** Atributos da faixa g a partir dos atributos da branca (objeto novo; o peso nao muda). */
    public static AtributosLutador aplicar(AtributosLutador branca, Graduacao g) {
        if (branca == null) {
            throw new IllegalArgumentException("Os atributos-base sao obrigatorios.");
        }
        if (g == null || g == Graduacao.BRANCA) {
            return branca.copiar();
        }
        return new AtributosLutador(
                evoluir(branca.getCondicionamento(), g),
                evoluir(branca.getFolego(), g),
                evoluir(branca.getForca(), g),
                evoluir(branca.getVelocidade(), g),
                evoluir(branca.getAgilidade(), g),
                evoluir(branca.getTecnica(), g),
                evoluir(branca.getRecuperacao(), g),
                branca.getPeso());
    }
}
