package com.irmaosmiau.regras;

import com.irmaosmiau.posicional.Lado;

import java.util.EnumMap;
import java.util.Map;

/**
 * Placar de uma luta: pontos, vantagens e punicoes de cada lado (A = J1, B = J2).
 *
 * As punicoes seguem a escala do art. 7.2.1 do Livro de Regras da IBJJF: 1a punicao so marcada; 2a da
 * uma vantagem ao adversario; 3a da 2 pontos ao adversario; 4a desclassifica. Hoje o jogo ainda nao
 * reconhece nenhuma infracao com seguranca (ver docs), entao {@link #punir} so e chamado por testes.
 */
public final class Placar {

    private final Map<Lado, Integer> pontos = new EnumMap<>(Lado.class);
    private final Map<Lado, Integer> vantagens = new EnumMap<>(Lado.class);
    private final Map<Lado, Integer> punicoes = new EnumMap<>(Lado.class);
    private Lado desclassificado;

    public Placar() {
        for (Lado l : Lado.values()) {
            pontos.put(l, 0);
            vantagens.put(l, 0);
            punicoes.put(l, 0);
        }
    }

    public int pontos(Lado l) {
        return pontos.get(l);
    }

    public int vantagens(Lado l) {
        return vantagens.get(l);
    }

    public int punicoes(Lado l) {
        return punicoes.get(l);
    }

    void somarPontos(Lado l, int n) {
        pontos.merge(l, n, Integer::sum);
    }

    void somarVantagem(Lado l) {
        vantagens.merge(l, 1, Integer::sum);
    }

    /** Uma punicao (art. 7.2.1). Devolve o efeito, para o aviso. */
    public String punir(Lado l) {
        int n = punicoes.merge(l, 1, Integer::sum);
        return switch (n) {
            case 1 -> "PUNIÇÃO";
            case 2 -> {
                somarVantagem(l.outro());
                yield "PUNIÇÃO · VANTAGEM AO ADVERSÁRIO";
            }
            case 3 -> {
                somarPontos(l.outro(), 2);
                yield "PUNIÇÃO · +2 AO ADVERSÁRIO";
            }
            default -> {
                desclassificado = l;
                yield "DESCLASSIFICAÇÃO";
            }
        };
    }

    public java.util.Optional<Lado> getDesclassificado() {
        return java.util.Optional.ofNullable(desclassificado);
    }

    /** "6 x 2" (pontos do J1 x pontos do J2). */
    public String texto() {
        return pontos(Lado.A) + " x " + pontos(Lado.B);
    }
}
