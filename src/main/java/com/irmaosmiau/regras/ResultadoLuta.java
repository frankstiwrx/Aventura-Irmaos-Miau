package com.irmaosmiau.regras;

import com.irmaosmiau.posicional.Lado;

import java.util.Optional;

/**
 * Resultado de uma luta com pontos quando o tempo acaba (art. 2.5 e 2.6 da IBJJF): mais pontos; empate nos
 * pontos, mais vantagens; empate tambem nas vantagens, menos punicoes. Empatando em tudo, a regra manda o
 * arbitro decidir (2.6): essa decisao nao existe no jogo, entao o resultado e EMPATE, dito assim, sem
 * sortear nem inventar um criterio.
 */
public record ResultadoLuta(Lado vencedor, Motivo motivo, Placar placar) {

    public enum Motivo {
        PONTOS("POR PONTOS"),
        VANTAGENS("POR VANTAGENS"),
        PUNICOES("POR MENOS PUNIÇÕES"),
        DESCLASSIFICACAO("POR DESCLASSIFICAÇÃO"),
        EMPATE("EMPATE");

        private final String rotulo;

        Motivo(String rotulo) {
            this.rotulo = rotulo;
        }

        public String getRotulo() {
            return rotulo;
        }
    }

    public Optional<Lado> getVencedor() {
        return Optional.ofNullable(vencedor);
    }

    /** Pelo placar, nesta ordem: desclassificacao, pontos, vantagens, punicoes; senao empate. */
    public static ResultadoLuta pelo(Placar p) {
        if (p.getDesclassificado().isPresent()) {
            return new ResultadoLuta(p.getDesclassificado().get().outro(), Motivo.DESCLASSIFICACAO, p);
        }
        int d = Integer.compare(p.pontos(Lado.A), p.pontos(Lado.B));
        if (d != 0) {
            return new ResultadoLuta(d > 0 ? Lado.A : Lado.B, Motivo.PONTOS, p);
        }
        d = Integer.compare(p.vantagens(Lado.A), p.vantagens(Lado.B));
        if (d != 0) {
            return new ResultadoLuta(d > 0 ? Lado.A : Lado.B, Motivo.VANTAGENS, p);
        }
        d = Integer.compare(p.punicoes(Lado.B), p.punicoes(Lado.A));
        if (d != 0) {
            return new ResultadoLuta(d > 0 ? Lado.A : Lado.B, Motivo.PUNICOES, p);
        }
        return new ResultadoLuta(null, Motivo.EMPATE, p);
    }

    /** "J1 VENCEU POR PONTOS · 6 x 2 · VANT 1 x 0 · PUN 0 x 0" (linhas separadas por " · " na tela final). */
    public String texto() {
        String placar = "PLACAR " + placar().texto() + " · VANT " + placar().vantagens(Lado.A) + " x "
                + placar().vantagens(Lado.B) + " · PUN " + placar().punicoes(Lado.A) + " x " + placar().punicoes(Lado.B);
        if (vencedor == null) {
            return "EMPATE (decisão do árbitro ainda não existe no jogo) · " + placar;
        }
        return (vencedor == Lado.A ? "J1" : "J2") + " VENCEU " + motivo.getRotulo() + " · " + placar;
    }
}
