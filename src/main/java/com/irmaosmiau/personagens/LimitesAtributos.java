package com.irmaosmiau.personagens;

import com.irmaosmiau.entities.AtributosLutador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Limites validos para cada atributo de {@link AtributosLutador}.
 *
 * AtributosLutador nao define limites; estes valores sao a regra PROVISORIA
 * da Entrega 4B, coerente com o uso atual:
 * - as formulas do jogo tratam os atributos numa escala 0..100
 *   (ex.: velocidade / 100.0 em IrmaoMiau);
 * - condicionamento e folego sao maximos de energia e precisam ser &gt; 0
 *   (o HUD e o jogo dividem por eles);
 * - peso esta em kg (Branco 72, Preto 68).
 * Oficiais: Branco 35/25/42/22/25/25/22/72, Preto 25/40/18/45/45/28/38/68,
 * todos dentro dos limites. Ajustar aqui quando o criador definir a regra final.
 */
public final class LimitesAtributos {

    /** Um atributo: nome (igual ao getter de AtributosLutador), minimo e maximo. */
    public static final class Limite {
        private final String nome;
        private final double minimo;
        private final double maximo;
        private final ToDoubleFunction<AtributosLutador> leitor;

        Limite(String nome, double minimo, double maximo, ToDoubleFunction<AtributosLutador> leitor) {
            this.nome = nome;
            this.minimo = minimo;
            this.maximo = maximo;
            this.leitor = leitor;
        }

        public String getNome() {
            return nome;
        }

        public double getMinimo() {
            return minimo;
        }

        public double getMaximo() {
            return maximo;
        }

        public double ler(AtributosLutador a) {
            return leitor.applyAsDouble(a);
        }
    }

    private static final Map<String, Limite> LIMITES = new LinkedHashMap<>();

    static {
        add("condicionamento", 1, 100, AtributosLutador::getCondicionamento);
        add("folego", 1, 100, AtributosLutador::getFolego);
        add("forca", 1, 100, AtributosLutador::getForca);
        add("velocidade", 1, 100, AtributosLutador::getVelocidade);
        add("agilidade", 1, 100, AtributosLutador::getAgilidade);
        add("tecnica", 1, 100, AtributosLutador::getTecnica);
        add("recuperacao", 1, 100, AtributosLutador::getRecuperacao);
        add("peso", 40, 120, AtributosLutador::getPeso);
    }

    private static void add(String nome, double min, double max, ToDoubleFunction<AtributosLutador> leitor) {
        LIMITES.put(nome, new Limite(nome, min, max, leitor));
    }

    private LimitesAtributos() {
    }

    /** Os 8 atributos, na ordem do construtor de AtributosLutador. */
    public static List<Limite> todos() {
        return Collections.unmodifiableList(new ArrayList<>(LIMITES.values()));
    }

    public static Limite de(String nome) {
        Limite l = LIMITES.get(nome);
        if (l == null) {
            throw new IllegalArgumentException("Atributo desconhecido: " + nome + " (validos: " + LIMITES.keySet() + ")");
        }
        return l;
    }

    /** Lista de problemas (vazia = atributos validos). */
    public static List<String> problemas(AtributosLutador a) {
        List<String> erros = new ArrayList<>();
        if (a == null) {
            erros.add("atributos ausentes");
            return erros;
        }
        for (Limite l : LIMITES.values()) {
            double v = l.ler(a);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                erros.add(l.nome + " invalido (" + v + ")");
            } else if (v < l.minimo || v > l.maximo) {
                erros.add(l.nome + " = " + formatar(v) + " fora do intervalo ["
                        + formatar(l.minimo) + ", " + formatar(l.maximo) + "]");
            }
        }
        return erros;
    }

    /** @throws IllegalArgumentException com todos os problemas encontrados */
    public static void validar(AtributosLutador a) {
        List<String> erros = problemas(a);
        if (!erros.isEmpty()) {
            throw new IllegalArgumentException("Atributos invalidos: " + String.join("; ", erros));
        }
    }

    private static String formatar(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
