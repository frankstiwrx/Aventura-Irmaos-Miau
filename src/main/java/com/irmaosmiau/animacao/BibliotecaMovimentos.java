package com.irmaosmiau.animacao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Entrega 5O: movimentos COMPARTILHADOS por tecnica e rig (nunca por
 * personagem): guardas, sprawl e rolamentos experimentais, acrobacias.
 * Arquivo: {@code /conteudo/animacoes/movimentos/<id>.json} ("uso": "MOVIMENTO").
 *
 * - Guardas: 2 quadros = DELTAS sobre a postura atual (em pe ou de joelhos),
 *   alternando devagar (respiracao).
 * - Sequencias: quadros ABSOLUTOS pelo progresso 0..1 do movimento, com
 *   interpolacao entre quadros vizinhos e deslocamento vertical por quadro.
 */
public final class BibliotecaMovimentos {

    public static final String PASTA = "/conteudo/animacoes/movimentos/";
    public static final List<String> GUARDAS = List.of("guarda_alta", "guarda_media", "guarda_baixa");
    public static final List<String> SEQUENCIAS = List.of("sprawl_5o", "rolamento_frente_5o", "rolamento_tras_5o",
            "rolamento_lateral_5o", "estrela_5o", "mortal_frente_5o", "mortal_tras_5o");

    /** Um movimento carregado. */
    public static final class Movimento {
        private final String id;
        private final String rig;
        private final int[][] quadros;
        private final int[] dys;

        Movimento(String id, CarregadorAnimacaoJson.Movimento m) {
            this.id = id;
            this.rig = m.animacao().getRig();
            this.quadros = new int[m.animacao().getQuantidadeQuadros()][];
            for (int i = 0; i < quadros.length; i++) {
                quadros[i] = m.animacao().quadro(i);
            }
            this.dys = m.deslocamentosY() == null ? new int[quadros.length] : m.deslocamentosY().clone();
        }

        public String getId() {
            return id;
        }

        public String getRig() {
            return rig;
        }

        public int quantidade() {
            return quadros.length;
        }

        public int[] quadro(int i) {
            return quadros[i].clone();
        }

        /** Pose (30 valores) e deslocamento vertical no progresso 0..1, interpolando quadros vizinhos. */
        public double[] noProgresso(double p, int[] destino) {
            return noProgresso(p, destino, dys);
        }

        /** Deslocamentos do arquivo (assentados no Miau Branco). */
        public int[] deslocamentosY() {
            return dys.clone();
        }

        private double[] noProgresso(double p, int[] destino, int[] dys) {
            double t = Math.max(0, Math.min(1, p)) * (quadros.length - 1);
            int i = Math.min(quadros.length - 2, (int) Math.floor(t));
            double f = quadros.length == 1 ? 0 : t - i;
            int[] a = quadros[Math.max(0, i)];
            int[] b = quadros[Math.min(quadros.length - 1, i + 1)];
            for (int k = 0; k < 30; k++) {
                destino[k] = k < 4 ? a[k] : (int) Math.round(a[k] + (b[k] - a[k]) * f);
            }
            double dy = dys[Math.max(0, i)] + (dys[Math.min(quadros.length - 1, i + 1)] - dys[Math.max(0, i)]) * f;
            return new double[] {dy};
        }
    }

    private final Map<String, Movimento> movimentos = new LinkedHashMap<>();

    public static String caminho(String id) {
        return PASTA + id + ".json";
    }

    public static BibliotecaMovimentos padrao() {
        BibliotecaMovimentos b = new BibliotecaMovimentos();
        for (String id : GUARDAS) {
            b.movimentos.put(id, new Movimento(id, CarregadorAnimacaoJson.movimentoDoRecurso(caminho(id))));
        }
        for (String id : SEQUENCIAS) {
            b.movimentos.put(id, new Movimento(id, CarregadorAnimacaoJson.movimentoDoRecurso(caminho(id))));
        }
        return b;
    }

    public Optional<Movimento> buscar(String id) {
        return Optional.ofNullable(movimentos.get(id));
    }

    public java.util.Set<String> ids() {
        return java.util.Collections.unmodifiableSet(movimentos.keySet());
    }
}
