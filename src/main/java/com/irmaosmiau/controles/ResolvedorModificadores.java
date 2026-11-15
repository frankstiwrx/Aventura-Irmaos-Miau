package com.irmaosmiau.controles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Entrega 5O.1: combinacao de MODIFICADORES segurados -> familia de tecnicas
 * do RS (ou das setas do J2 no teclado-controle). Um lugar so para todas as
 * combinacoes, presentes e futuras; o GamePanel e a FonteGamepad so perguntam.
 *
 * Regras:
 * - cada combinacao tem uma familia e uma PRIORIDADE (menor = vence);
 * - so combinacoes ATIVAS valem no jogo; as RESERVADAS existem para o HUD e a
 *   documentacao e nunca disparam nada (nenhuma tecnica inventada);
 * - vale a combinacao ativa MAIS ESPECIFICA (mais modificadores) contida no
 *   que esta segurado; empate = prioridade. Assim nunca saem duas familias
 *   juntas, e segurar um modificador reservado a mais (ex.: RT + LB) cai na
 *   familia ativa que sobra (DEFESAS), como antes;
 * - combinacoes de dois botoes sao o padrao; tres botoes ficam reservados.
 *
 * Hoje: nenhum = PEGADAS; RT = DEFESAS; RB (ou Select) = QUEDAS E TRANSICOES;
 * RT + RB = DEFESAS (RT tem prioridade). LT e LB continuam sendo agachar /
 * arrasto e as camadas do Contextual: as familias deles ficam RESERVADAS.
 */
public final class ResolvedorModificadores {

    /** Modificadores logicos (gamepad; o teclado-controle usa as mesmas teclas-botao). */
    public enum Modificador {
        RT, RB, LT, LB
    }

    /** Uma linha da tabela. familia == null = reservada sem familia ainda. */
    public record Combinacao(Set<Modificador> modificadores, FamiliaTecnica familia, String rotulo,
            int prioridade, boolean ativa) {

        public Combinacao {
            modificadores = Collections.unmodifiableSet(modificadores.isEmpty()
                    ? EnumSet.noneOf(Modificador.class) : EnumSet.copyOf(modificadores));
        }

        /** "RT + LB + RS" etc. */
        public String comando() {
            List<String> p = new ArrayList<>();
            modificadores.forEach(m -> p.add(m.name()));
            p.add("RS");
            return String.join(" + ", p);
        }
    }

    private static final List<Combinacao> TABELA = List.of(
            new Combinacao(EnumSet.noneOf(Modificador.class), FamiliaTecnica.PEGADAS, "PEGADAS", 9, true),
            new Combinacao(EnumSet.of(Modificador.RT), FamiliaTecnica.DEFESAS, "DEFESAS", 1, true),
            new Combinacao(EnumSet.of(Modificador.RB), FamiliaTecnica.PROJECOES, "QUEDAS E TRANSIÇÕES", 2, true),
            // ---- reservadas (nada executa; futuras familias) ----
            new Combinacao(EnumSet.of(Modificador.LT), null, "RESERVADA (LT)", 3, false),
            new Combinacao(EnumSet.of(Modificador.LB), null, "RESERVADA (LB)", 4, false),
            new Combinacao(EnumSet.of(Modificador.RT, Modificador.LB), null, "RESERVADA (RT + LB)", 5, false),
            new Combinacao(EnumSet.of(Modificador.RT, Modificador.LT), null, "RESERVADA (RT + LT)", 6, false),
            new Combinacao(EnumSet.of(Modificador.LT, Modificador.LB), null, "RESERVADA (LT + LB)", 7, false),
            new Combinacao(EnumSet.of(Modificador.RT, Modificador.LT, Modificador.LB), null,
                    "RESERVADA ESPECIAL (RT + LT + LB)", 8, false));

    private ResolvedorModificadores() {
    }

    public static List<Combinacao> tabela() {
        return TABELA;
    }

    /** A combinacao ativa que vale agora (sempre existe: no minimo PEGADAS). */
    public static Combinacao resolver(Set<Modificador> segurados) {
        Set<Modificador> s = segurados == null || segurados.isEmpty()
                ? EnumSet.noneOf(Modificador.class) : EnumSet.copyOf(segurados);
        return TABELA.stream()
                .filter(Combinacao::ativa)
                .filter(c -> s.containsAll(c.modificadores()))
                .min(Comparator.comparingInt((Combinacao c) -> -c.modificadores().size())
                        .thenComparingInt(Combinacao::prioridade))
                .orElseThrow();
    }

    public static FamiliaTecnica familia(Set<Modificador> segurados) {
        return resolver(segurados).familia();
    }

    /** A linha EXATA da combinacao segurada (ativa ou reservada), para HUD/documentacao. */
    public static Optional<Combinacao> exata(Set<Modificador> segurados) {
        Set<Modificador> s = segurados == null || segurados.isEmpty()
                ? EnumSet.noneOf(Modificador.class) : EnumSet.copyOf(segurados);
        return TABELA.stream().filter(c -> c.modificadores().equals(s)).findFirst();
    }
}
