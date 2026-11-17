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
 * 5Q: todos os 48 espacos apontam para uma tecnica real (finalizacao do
 * CatalogoFinalizacoes ou, no L1+L2, a entrada no controle de perna). Um espaco
 * sem tecnica continuaria recusado ("FINALIZAÇÃO FUTURA", sem custo); a tecnica
 * do catalogo sempre vence (uma acao por comando, no maximo).
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
    // REFERENCIA (5Q): cada espaco aponta para a tecnica REAL
    // L1 / L2 -> a finalizacao do CatalogoFinalizacoes nesta posicao;
    // L1+L2 -> a ENTRADA no controle de perna (a chave de perna em si so existe no ashi garami).
    // =====================================================
    public static RegistroFinalizacoes referencia() {
        CatalogoFinalizacoes cat = CatalogoFinalizacoes.referencia();
        Map<String, Espaco> m = new LinkedHashMap<>();
        for (Map.Entry<Posicao, PapelPosicional> pp : POSICOES.entrySet()) {
            Posicao p = pp.getKey();
            for (Familia f : FAMILIAS) {
                for (Direcao d : Direcao.values()) {
                    String id = id(p, f, d);
                    String nome;
                    String curto;
                    String tecnica;
                    if (f == Familia.L1_L2) {
                        Finalizacao alvo = cat.naPosicao(Posicao.ASHI_GARAMI, Familia.L1_L2, d).orElseThrow();
                        nome = "Entrada na perna (depois: " + alvo.nome() + ")";
                        curto = "→ PERNA";
                        tecnica = p == Posicao.GUARDA ? CatalogoTecnicasSolo.ENTRADA_PERNA_GUARDA
                                : CatalogoTecnicasSolo.ENTRADA_PERNA_DOMINANTE;
                    } else {
                        Finalizacao fz = cat.naPosicao(p, f, d).orElse(null);
                        nome = fz == null ? null : fz.nome();
                        curto = fz == null ? "LIVRE" : fz.rotulo();
                        tecnica = fz == null ? null : fz.id();
                    }
                    m.put(id, new Espaco(id, p, pp.getValue(), f, d, nome, curto, tecnica));
                }
            }
        }
        return new RegistroFinalizacoes(m);
    }
}
