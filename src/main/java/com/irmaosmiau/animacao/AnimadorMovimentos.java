package com.irmaosmiau.animacao;

import com.irmaosmiau.entities.DefesaCorporal;
import com.irmaosmiau.entities.Direcao;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoAcrobacia;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Entrega 5O: desenha os movimentos compartilhados no lutador, pelo canal de
 * pose externa (o mesmo das pegadas), so quando ele esta livre ou e nosso.
 *
 * - Guardas (sempre): delta sobre a postura atual; entrada, troca e saida
 *   suaves; respiracao pelo tempo na guarda (nao recomeca a cada tick).
 * - Acrobacias (sempre; nao existe versao anterior).
 * - Sprawl e rolamento: so na versao EXPERIMENTAL_5O (F7); na ORIGINAL nada
 *   e aplicado e o desenho Java de sempre continua.
 *
 * Rig incompativel (personagem futuro com outro esqueleto) = fallback: nada
 * e aplicado. Nunca move ninguem e nunca muda regra, tempo ou custo.
 */
public final class AnimadorMovimentos {

    /** Periodo da respiracao das guardas (ms). */
    public static final int PERIODO_RESPIRACAO_MS = 2400;

    private final BibliotecaMovimentos biblioteca;
    private final TransicoesPostura posturas;
    private final Map<IrmaoMiau, int[]> aplicadas = new IdentityHashMap<>();
    private final Map<IrmaoMiau, String> descricao = new IdentityHashMap<>();

    public AnimadorMovimentos(BibliotecaMovimentos biblioteca, TransicoesPostura posturas) {
        this.biblioteca = biblioteca;
        this.posturas = posturas;
    }

    /** O lutador esta num movimento que esta camada desenha agora? */
    public boolean quer(IrmaoMiau l) {
        if (l.getDefesa().isAtiva() || l.isEmAcrobacia()) {
            return true;
        }
        return VersaoMovimentos.atual() == VersaoMovimentos.EXPERIMENTAL_5O && (l.isFazendoSprawl() || l.isRolando());
    }

    /**
     * Desenha o movimento atual.
     *
     * @return descricao do que foi desenhado (HUD/testes), ou null (nada: legado desenha)
     */
    public String aplicar(IrmaoMiau l) {
        int[] pose = new int[30];
        double dy;
        String id;
        if (l.getDefesa().isAtiva()) {
            id = "guarda:" + l.getDefesa().getTipo().name();
            Double d = poseGuarda(l, pose);
            if (d == null) {
                return falhou(l);
            }
            dy = d;
        } else {
            BibliotecaMovimentos.Movimento principal;
            BibliotecaMovimentos.Movimento lateral = null;
            double p;
            boolean inverterGiro = false;
            if (l.isEmAcrobacia()) {
                TipoAcrobacia t = l.getAcrobacia();
                principal = buscar(t.isEstrela() ? "estrela_5o"
                        : t == TipoAcrobacia.MORTAL_FRENTE ? "mortal_frente_5o" : "mortal_tras_5o");
                inverterGiro = t == TipoAcrobacia.ESTRELA_DIREITA;
                p = l.progressoAcrobacia();
                id = "acrobacia:" + t.name();
            } else if (l.isFazendoSprawl()) {
                principal = buscar("sprawl_5o");
                p = l.progressoSprawl();
                id = "sprawl_5o:" + l.getFaseSprawl();
            } else {
                Direcao d = l.getDirecaoRolamento();
                int dx = d == null ? 0 : d.name().contains("DIREITA") ? 1 : d.name().contains("ESQUERDA") ? -1 : 0;
                int dz = d == null ? 0 : d.name().startsWith("CIMA") || d.name().startsWith("BAIXO") ? 1 : 0;
                if (dx == 0) {
                    principal = buscar("rolamento_lateral_5o");
                    id = "rolamento_5o:LATERAL";
                } else {
                    // Pelo lado para onde ele olha AGORA (o desenho e espelhado por ele): rolar para onde olha = frente.
                    boolean frente = (dx > 0) == l.isViradoParaDireita();
                    principal = buscar(frente ? "rolamento_frente_5o" : "rolamento_tras_5o");
                    lateral = dz != 0 ? buscar("rolamento_lateral_5o") : null;
                    id = "rolamento_5o:" + (frente ? "FRENTE" : "TRAS") + (dz != 0 ? "_DIAGONAL" : "");
                }
                p = l.progressoRolamento();
            }
            if (principal == null || !principal.getRig().equals(l.getRigAnimacao())) {
                return falhou(l);
            }
            dy = principal.noProgresso(p, pose)[0];
            if (lateral != null) {
                // Diagonal: o giro e o da frente/tras; o corpo pega 30% do rolamento lateral (ombro e quadril).
                int[] lat = new int[30];
                double dyLat = lateral.noProgresso(p, lat)[0];
                for (int k = 4; k < 30; k++) {
                    if (k != 10) {
                        pose[k] = (int) Math.round(pose[k] * 0.7 + lat[k] * 0.3);
                    }
                }
                dy = dy * 0.7 + dyLat * 0.3;
            }
            if (inverterGiro) {
                pose[10] = -pose[10];
            }
            pose[2] = (int) Math.round(l.getEscala() * 10);
            pose[3] = 1;
        }
        int[] nossa = aplicadas.get(l);
        if (l.isPoseCombateExternaAtiva() && (nossa == null || !Arrays.equals(nossa, l.copiarPoseCombateExterna()))) {
            return null;                                   // combate ou outra camada: nao interferir
        }
        l.aplicarPoseCombateExterna(pose, 0.0, dy);
        aplicadas.put(l, pose);
        descricao.put(l, id);
        return id;
    }

    private String falhou(IrmaoMiau l) {
        liberar(l);
        return null;
    }

    private BibliotecaMovimentos.Movimento buscar(String id) {
        return biblioteca.buscar(id).orElse(null);
    }

    /** Pose da guarda sobre a postura; devolve o dy (null = rig incompativel ou sem arquivo). */
    private Double poseGuarda(IrmaoMiau l, int[] destino) {
        DefesaCorporal d = l.getDefesa();
        int[] alvo = deltaGuarda(l, d.getTipo(), d.getTicksAtiva());
        if (alvo == null) {
            return null;
        }
        int[] delta;
        double w = ReacaoReceptor.suave(d.getProgressoFase());
        switch (d.getFase()) {
            case ENTRANDO -> {
                int[] de = d.getAnterior() == null ? new int[30] : deltaGuarda(l, d.getAnterior(), 0);
                delta = ReacaoReceptor.mistura(de == null ? new int[30] : de, alvo, w);
            }
            case SAINDO -> delta = ReacaoReceptor.mistura(alvo, new int[30], w);
            default -> delta = alvo;
        }
        TransicoesPostura.Pose base = posturas.pose(l, l.getEstadoCorporal());
        int[] b = base.valores();
        System.arraycopy(b, 0, destino, 0, 30);
        boolean ajoelhado = l.isAjoelhado();
        for (int k = 4; k < 30; k++) {
            int v = delta[k];
            if (ajoelhado && k >= 22) {
                v = 0;                                     // de joelhos: as pernas sao as da postura
            } else if (ajoelhado && k <= 6) {
                v = v / 2;
            }
            destino[k] = b[k] + v;
        }
        return base.dy();
    }

    private int[] deltaGuarda(IrmaoMiau l, DefesaCorporal.Tipo tipo, long ticksAtiva) {
        if (tipo == null) {
            return null;
        }
        BibliotecaMovimentos.Movimento m = buscar(tipo.name().toLowerCase(java.util.Locale.ROOT));
        if (m == null || !m.getRig().equals(l.getRigAnimacao()) || m.quantidade() < 2) {
            return null;
        }
        double fase = (ticksAtiva * CamadaPoseCombate.MS_POR_TICK % PERIODO_RESPIRACAO_MS) / PERIODO_RESPIRACAO_MS;
        double t = 0.5 - 0.5 * Math.cos(2 * Math.PI * fase);
        return ReacaoReceptor.mistura(m.quadro(0), m.quadro(1), t);
    }

    /** Devolve o lutador ao desenho normal (so se a pose ainda for a nossa). */
    public void liberar(IrmaoMiau l) {
        int[] nossa = aplicadas.remove(l);
        descricao.remove(l);
        if (nossa != null && l.isPoseCombateExternaAtiva() && Arrays.equals(nossa, l.copiarPoseCombateExterna())) {
            l.limparPoseCombateExterna();
        }
    }

    /** O que esta camada desenhou por ultimo no lutador (null = nada). */
    public String descricao(IrmaoMiau l) {
        return descricao.get(l);
    }

    public BibliotecaMovimentos getBiblioteca() {
        return biblioteca;
    }
}
