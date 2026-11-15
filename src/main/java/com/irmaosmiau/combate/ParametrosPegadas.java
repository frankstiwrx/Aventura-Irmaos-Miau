package com.irmaosmiau.combate;

import com.irmaosmiau.entities.IrmaoMiau;

import java.util.List;
import java.util.Map;

/**
 * Entrega 5L: identidade mecanica das quatro pegadas, num lugar so.
 *
 * Alcance, custo de folego, dificuldade e pesos continuam no tecnicas.json
 * (onde ja estavam). Aqui ficam os parametros NOVOS que o motor le:
 *
 * - firmezaInicial: firmeza quando a pegada fica estabelecida (0..100);
 * - fatorReforco: multiplica os +25 do reforco;
 * - fatorRompimento: &gt; 1 = mais facil de romper (a disputa do ROMPER ve
 *   uma firmeza menor); &lt; 1 = mais dificil;
 * - preparaPara: o que a pegada prepara no futuro (so informacao; nada de
 *   queda ou guarda e executado nesta entrega);
 * - especialidade: o irmao que domina a pegada (identidade do personagem):
 *   para ele o rompimento fica 10% mais dificil e o reforco 10% maior.
 *
 * Os atributos dos personagens nao mudam.
 */
public final class ParametrosPegadas {

    /** Ajuste de especialidade (pequeno de proposito). */
    public static final double BONUS_ESPECIALIDADE = 0.10;

    /** Parametros de uma pegada. */
    public record Parametros(String idAcao, String nome, String pontos, double firmezaInicial, double fatorReforco,
            double fatorRompimento, String preparaPara, IdentidadeRig especialidade) {
    }

    private static final Map<String, Parametros> TABELA = Map.of(
            "pegada_1", new Parametros("pegada_1", "Gola e manga", "mao da frente na gola, mao de tras na manga",
                    100.0, 1.0, 1.0, "QUEDA", null),
            "pegada_2", new Parametros("pegada_2", "Cruzada", "maos invertidas: frente na manga, tras cruza para a gola",
                    100.0, 0.8, 0.85, "QUEDA", IdentidadeRig.BRANCO),
            "pegada_3", new Parametros("pegada_3", "Duas mangas", "as duas maos nas mangas do oponente",
                    85.0, 1.2, 1.15, "GUARDA", IdentidadeRig.PRETO),
            "pegada_4", new Parametros("pegada_4", "Gola e nuca", "mao da frente na nuca, mao de tras na gola",
                    95.0, 1.0, 0.9, "QUEDA", IdentidadeRig.BRANCO));

    /** Ordem dos comandos: RS cima, direita, baixo, esquerda. */
    public static final List<String> ORDEM = List.of("pegada_1", "pegada_2", "pegada_3", "pegada_4");

    private ParametrosPegadas() {
    }

    /** Parametros da pegada, ou os neutros (como a PEGADA_1 da 5K) para outra acao. */
    public static Parametros de(String idAcao) {
        Parametros p = idAcao == null ? null : TABELA.get(idAcao);
        return p != null ? p : new Parametros(idAcao, idAcao, "", 100.0, 1.0, 1.0, null, null);
    }

    public static boolean isPegada(String idAcao) {
        return idAcao != null && TABELA.containsKey(idAcao);
    }

    private static boolean especialista(Parametros p, IrmaoMiau aplicador) {
        return p.especialidade() != null && IdentidadeRig.de(aplicador) == p.especialidade();
    }

    /** Firmeza inicial para este aplicador (limitada a 100). */
    public static double firmezaInicial(String idAcao, IrmaoMiau aplicador) {
        return Math.min(SistemaCombate.FIRMEZA_INICIAL, de(idAcao).firmezaInicial());
    }

    /** Multiplicador do reforco para este aplicador. */
    public static double fatorReforco(String idAcao, IrmaoMiau aplicador) {
        Parametros p = de(idAcao);
        return p.fatorReforco() * (especialista(p, aplicador) ? 1.0 + BONUS_ESPECIALIDADE : 1.0);
    }

    /** Multiplicador de vulnerabilidade ao ROMPER, com a especialidade do aplicador. */
    public static double fatorRompimento(String idAcao, IrmaoMiau aplicador) {
        Parametros p = de(idAcao);
        return p.fatorRompimento() * (especialista(p, aplicador) ? 1.0 - BONUS_ESPECIALIDADE : 1.0);
    }
}
