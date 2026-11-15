package com.irmaosmiau.posicional;

import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Entrega 5P.2: os ESPACOS de finalizacao (12 por posicao: L1, L2 e L1+L2,
 * cada um com as 4 direcoes do RS) nas posicoes de controle: guarda fechada
 * (quem esta por baixo), cem quilos, montada e costas (quem controla).
 *
 * NADA aqui executa: nenhum espaco tem tecnica real nesta entrega. O MotorSolo
 * recusa o comando ("FINALIZAÇÃO FUTURA") sem custo; o HUD mostra o espaco como
 * reservado (com o nome planejado quando existe). Quando uma finalizacao for
 * implementada, ela entra no catalogo como uma TecnicaSolo e o espaco passa a
 * apontar para ela ({@link #comTecnica}); a tecnica do catalogo sempre vence o
 * espaco reservado (uma acao por comando, no maximo).
 *
 * Identificador estavel: "fin.<posicao>.<familia>.<direcao>" em minusculas,
 * ex.: "fin.montada.l2.cima".
 */
public final class RegistroFinalizacoes {

    /** Familias das finalizacoes (as tres com L). */
    public static final Set<Familia> FAMILIAS = Collections.unmodifiableSet(
            EnumSet.of(Familia.L1, Familia.L2, Familia.L1_L2));

    /** Posicoes com espacos (e o papel de quem finaliza nelas). */
    public static final Map<Posicao, PapelPosicional> POSICOES;

    static {
        Map<Posicao, PapelPosicional> m = new LinkedHashMap<>();
        m.put(Posicao.GUARDA, PapelPosicional.CONTROLADOR);            // por baixo, com a guarda fechada
        m.put(Posicao.CONTROLE_LATERAL, PapelPosicional.CONTROLADOR);
        m.put(Posicao.MONTADA, PapelPosicional.CONTROLADOR);
        m.put(Posicao.COSTAS, PapelPosicional.CONTROLADOR);
        POSICOES = Collections.unmodifiableMap(m);
    }

    /**
     * Um espaco.
     *
     * @param planejada nome da finalizacao planejada (null = espaco livre, sem plano coerente ainda)
     * @param idTecnica tecnica real do catalogo (null = nao implementada, nao executa)
     */
    public record Espaco(String id, Posicao posicao, PapelPosicional papel, Familia familia, Direcao direcao,
            String planejada, String rotuloCurto, String idTecnica) {

        public boolean implementada() {
            return idTecnica != null;
        }

        /** Identificador futuro da animacao (contrato). */
        public String animacaoFutura() {
            return "finalizacao/" + id;
        }
    }

    private final Map<String, Espaco> porId;

    private RegistroFinalizacoes(Map<String, Espaco> porId) {
        this.porId = Collections.unmodifiableMap(porId);
    }

    public static String id(Posicao p, Familia f, Direcao d) {
        return ("fin." + p.name() + "." + f.name() + "." + d.name()).toLowerCase(java.util.Locale.ROOT)
                .replace("controle_lateral", "cem_quilos").replace("l1_l2", "l1l2");
    }

    public List<Espaco> todos() {
        return new ArrayList<>(porId.values());
    }

    public Optional<Espaco> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }

    public List<Espaco> daPosicao(Posicao p) {
        return porId.values().stream().filter(e -> e.posicao() == p).toList();
    }

    /** O espaco que este comando ocupa para este lado agora (vazio fora das posicoes/papel/familias). */
    public Optional<Espaco> para(EstadoPosicional estado, Lado lado, ComandoSolo c) {
        if (c == null || !FAMILIAS.contains(c.familia())) {
            return Optional.empty();
        }
        PapelPosicional papel = POSICOES.get(estado.getPosicao());
        if (papel == null || !estado.temPapel(lado, papel)) {
            return Optional.empty();
        }
        return buscar(id(estado.getPosicao(), c.familia(), c.direcao()));
    }

    /** Copia com uma tecnica real ligada a um espaco (entregas futuras). */
    public RegistroFinalizacoes comTecnica(String idEspaco, String idTecnica) {
        Espaco e = buscar(idEspaco).orElseThrow(() -> new IllegalArgumentException("Espaço inexistente: " + idEspaco));
        Map<String, Espaco> m = new LinkedHashMap<>(porId);
        m.put(idEspaco, new Espaco(e.id(), e.posicao(), e.papel(), e.familia(), e.direcao(), e.planejada(),
                e.rotuloCurto(), idTecnica));
        return new RegistroFinalizacoes(m);
    }

    // =====================================================
    // REFERENCIA: planos coerentes por posicao (nem todo espaco tem plano)
    // L1 = chaves de articulacao; L2 = estrangulamentos; L1+L2 = triangulos e especiais.
    // =====================================================
    private static final Object[][] PLANOS = {
        // guarda fechada (por baixo)
        {Posicao.GUARDA, Familia.L1, Direcao.CIMA, "Chave de braço", "ARMLOCK"},
        {Posicao.GUARDA, Familia.L1, Direcao.DIREITA, "Kimura", "KIMURA"},
        {Posicao.GUARDA, Familia.L1, Direcao.BAIXO, "Omoplata", "OMOPLATA"},
        {Posicao.GUARDA, Familia.L2, Direcao.CIMA, "Estrangulamento cruzado (gola)", "CRUZADO"},
        {Posicao.GUARDA, Familia.L2, Direcao.BAIXO, "Guilhotina", "GUILHOT."},
        {Posicao.GUARDA, Familia.L1_L2, Direcao.CIMA, "Triângulo", "TRIÂNGULO"},
        // cem quilos (quem controla)
        {Posicao.CONTROLE_LATERAL, Familia.L1, Direcao.CIMA, "Americana", "AMERICANA"},
        {Posicao.CONTROLE_LATERAL, Familia.L1, Direcao.DIREITA, "Kimura", "KIMURA"},
        {Posicao.CONTROLE_LATERAL, Familia.L1, Direcao.BAIXO, "Chave de braço (girando)", "ARMLOCK"},
        {Posicao.CONTROLE_LATERAL, Familia.L2, Direcao.CIMA, "Estrangulamento pela gola", "GOLA"},
        {Posicao.CONTROLE_LATERAL, Familia.L2, Direcao.BAIXO, "Kata-gatame (braço e pescoço)", "KATA-GAT."},
        {Posicao.CONTROLE_LATERAL, Familia.L1_L2, Direcao.CIMA, "Estrangulamento norte-sul", "NORTE-SUL"},
        // montada (quem controla)
        {Posicao.MONTADA, Familia.L1, Direcao.CIMA, "Americana", "AMERICANA"},
        {Posicao.MONTADA, Familia.L1, Direcao.DIREITA, "Chave de braço", "ARMLOCK"},
        {Posicao.MONTADA, Familia.L2, Direcao.CIMA, "Estrangulamento cruzado (gola)", "CRUZADO"},
        {Posicao.MONTADA, Familia.L2, Direcao.DIREITA, "Ezequiel", "EZEQUIEL"},
        {Posicao.MONTADA, Familia.L2, Direcao.BAIXO, "Kata-gatame (braço e pescoço)", "KATA-GAT."},
        {Posicao.MONTADA, Familia.L1_L2, Direcao.CIMA, "Triângulo da montada", "TRIÂNGULO"},
        // costas (quem controla)
        {Posicao.COSTAS, Familia.L1, Direcao.CIMA, "Chave de braço (das costas)", "ARMLOCK"},
        {Posicao.COSTAS, Familia.L2, Direcao.CIMA, "Mata-leão", "MATA-LEÃO"},
        {Posicao.COSTAS, Familia.L2, Direcao.DIREITA, "Arco e flecha (gola)", "ARCO"},
        {Posicao.COSTAS, Familia.L2, Direcao.BAIXO, "Estrangulamento pela gola", "GOLA"},
        {Posicao.COSTAS, Familia.L1_L2, Direcao.CIMA, "Triângulo de costas", "TRIÂNGULO"},
    };

    public static RegistroFinalizacoes referencia() {
        Map<String, Espaco> m = new LinkedHashMap<>();
        for (Map.Entry<Posicao, PapelPosicional> pp : POSICOES.entrySet()) {
            for (Familia f : FAMILIAS) {
                for (Direcao d : Direcao.values()) {
                    String id = id(pp.getKey(), f, d);
                    String nome = null;
                    String curto = "LIVRE";
                    for (Object[] pl : PLANOS) {
                        if (pl[0] == pp.getKey() && pl[1] == f && pl[2] == d) {
                            nome = (String) pl[3];
                            curto = (String) pl[4];
                        }
                    }
                    m.put(id, new Espaco(id, pp.getKey(), pp.getValue(), f, d, nome, curto, null));
                }
            }
        }
        return new RegistroFinalizacoes(m);
    }
}
