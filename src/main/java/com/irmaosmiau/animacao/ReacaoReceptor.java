package com.irmaosmiau.animacao;

import java.util.Arrays;

/**
 * Entrega 5N: a reacao COMPARTILHADA de quem recebe uma tecnica de pegada.
 *
 * Pertence a TECNICA (ex.: "pegada_1") e ao papel RECEPTOR, nunca a um
 * personagem: o mesmo arquivo serve ao Branco, ao Preto e a qualquer
 * personagem futuro de rig compativel ({@link AdaptadorRigReacao}).
 *
 * Contrato dos quadros (rig semantico "miau_v1", 30 valores; os 4 primeiros
 * - centro, linha, escala, lado - sao ignorados). Os valores sao DELTAS
 * somados a pose que o motor deixou no receptor (a da animacao conjunta, se
 * o autor desenhou uma), em espaco LOCAL do lutador (+X = para onde ele olha,
 * -Y = para cima), por isso o espelhamento e automatico:
 *
 * <ul>
 *   <li>quadro 0 = CONTATO: o instante em que a mao do aplicador chega;</li>
 *   <li>quadros 1 e 2 = SUSTENTADA: a pose de controle, alternando devagar
 *       entre os dois (respiracao / tensao), enquanto a pegada existir;</li>
 *   <li>quadro 3 = LIBERACAO: o pequeno recuo quando a pegada acaba.</li>
 * </ul>
 */
public final class ReacaoReceptor {

    public static final int CONTATO = 0;
    public static final int SUSTENTADA_A = 1;
    public static final int SUSTENTADA_B = 2;
    public static final int LIBERACAO = 3;
    public static final int QUADROS = 4;

    /** Fracao da entrada do aplicador em que o contato comeca e em que a pose ja esta sustentada. */
    public static final double INICIO_CONTATO = 0.40;
    public static final double PICO_CONTATO = 0.70;

    private final String idTecnica;
    private final String rig;
    private final int[][] quadros;
    private final int periodoMs;
    private final String origem;

    public ReacaoReceptor(String idTecnica, String rig, int[][] quadros, int periodoMs, String origem) {
        if (idTecnica == null || idTecnica.isBlank() || rig == null || rig.isBlank()) {
            throw new IllegalArgumentException("Reacao do receptor sem tecnica ou rig.");
        }
        if (quadros == null || quadros.length != QUADROS) {
            throw new IllegalArgumentException(idTecnica + ": a reacao do receptor precisa de " + QUADROS
                    + " quadros (CONTATO, SUSTENTADA A e B, LIBERACAO).");
        }
        this.quadros = new int[QUADROS][];
        for (int i = 0; i < QUADROS; i++) {
            if (quadros[i] == null || quadros[i].length != AnimacaoIndividual.TAMANHO_POSE) {
                throw new IllegalArgumentException(idTecnica + ": quadro " + i + " precisa de "
                        + AnimacaoIndividual.TAMANHO_POSE + " valores.");
            }
            int[] q = quadros[i].clone();
            Arrays.fill(q, 0, 4, 0);                       // cabecalho nao e delta
            this.quadros[i] = q;
        }
        this.idTecnica = idTecnica;
        this.rig = rig;
        this.periodoMs = Math.max(200, periodoMs);
        this.origem = origem;
    }

    /** Monta a partir do arquivo (formato individual, "uso": "RECEPTOR"). */
    public static ReacaoReceptor de(String idTecnica, AnimacaoIndividual a) {
        if (a.getQuantidadeQuadros() != QUADROS) {
            throw new IllegalArgumentException(a.getId() + ": a reacao do receptor precisa de " + QUADROS
                    + " quadros (tem " + a.getQuantidadeQuadros() + ").");
        }
        int[][] q = new int[QUADROS][];
        for (int i = 0; i < QUADROS; i++) {
            q[i] = a.quadro(i);
        }
        return new ReacaoReceptor(idTecnica, a.getRig(), q, a.getDuracaoQuadroMs() * 2, a.getOrigem());
    }

    public String getIdTecnica() {
        return idTecnica;
    }

    public String getRig() {
        return rig;
    }

    public String getOrigem() {
        return origem;
    }

    public int[] quadro(int i) {
        return quadros[i].clone();
    }

    /** Peso do contato (0..1) pela fracao da entrada do aplicador (suave, sem salto). */
    public static double pesoContato(double progressoEntrada) {
        double t = (progressoEntrada - INICIO_CONTATO) / (PICO_CONTATO - INICIO_CONTATO);
        return suave(t);
    }

    /**
     * Durante a ENTRADA: nada antes do contato, sobe ate o quadro CONTATO e
     * depois se acomoda na SUSTENTADA_A no fim da entrada (sem saltos).
     */
    public int[] deltaEntrada(double progresso) {
        if (progresso <= PICO_CONTATO) {
            return mistura(new int[AnimacaoIndividual.TAMANHO_POSE], quadros[CONTATO], pesoContato(progresso));
        }
        double t = (progresso - PICO_CONTATO) / (1.0 - PICO_CONTATO);
        return mistura(quadros[CONTATO], quadros[SUSTENTADA_A], suave(t));
    }

    /**
     * Pose SUSTENTADA: alterna devagar entre A e B (respiracao), pelo tempo
     * desde que a pegada foi estabelecida (nunca recomeca a cada tick).
     */
    public int[] deltaSustentada(long msDesdeEstabelecida) {
        double fase = (Math.max(0, msDesdeEstabelecida) % periodoMs) / (double) periodoMs;
        double t = 0.5 - 0.5 * Math.cos(2 * Math.PI * fase);
        return mistura(quadros[SUSTENTADA_A], quadros[SUSTENTADA_B], t);
    }

    public int[] deltaLiberacao() {
        return quadros[LIBERACAO].clone();
    }

    static double suave(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3 - 2 * c);
    }

    static int[] mistura(int[] a, int[] b, double t) {
        int[] r = new int[a.length];
        for (int i = 4; i < a.length; i++) {
            r[i] = (int) Math.round(a[i] + (b[i] - a[i]) * t);
        }
        return r;
    }
}
