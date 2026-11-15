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
    /** 5O.1: rolamentos e sprawl assentados no tatame por personagem. */
    private final AssentamentoMovimentos assentamento = new AssentamentoMovimentos();
    /** 5O.1: passos da guarda andando por lutador: [fase do passo, amplitude 0..1]. */
    private final Map<IrmaoMiau, double[]> passos = new IdentityHashMap<>();

    /** 5O.1: avanco da fase dos passos por tick (passos curtos, metade do ritmo da caminhada). */
    public static final double FASE_PASSO_POR_TICK = 0.16;
    /** 5O.1: quanto a amplitude dos passos sobe/desce por tick (comecar e parar sem tranco). */
    public static final double RAMPA_PASSO_POR_TICK = 0.2;

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
                // 5O.1: rolamento (de joelhos ou deitado pelo R3; em pe so por outras origens)
                double pr = l.progressoRolamento();
                Double d = poseRolamentoNaPostura(l, pr, pose);
                if (d == null) {
                    return falhou(l);
                }
                return aplicarPose(l, pose, d, idRolamento(l));
            }
            if (principal == null || !principal.getRig().equals(l.getRigAnimacao())) {
                return falhou(l);
            }
            dy = principal.noProgresso(p, pose)[0];
            if (!l.isEmAcrobacia()) {
                dy = assentamento.dy(l, principal, p);           // 5O.1: sprawl assentado neste personagem
            }
            if (inverterGiro) {
                pose[10] = -pose[10];
            }
            pose[2] = (int) Math.round(l.getEscala() * 10);
            pose[3] = 1;
        }
        return aplicarPose(l, pose, dy, id);
    }

    private String aplicarPose(IrmaoMiau l, int[] pose, double dy, String id) {
        int[] nossa = aplicadas.get(l);
        if (l.isPoseCombateExternaAtiva() && (nossa == null || !Arrays.equals(nossa, l.copiarPoseCombateExterna()))) {
            return null;                                   // combate ou outra camada: nao interferir
        }
        l.aplicarPoseCombateExterna(pose, 0.0, dy);
        aplicadas.put(l, pose);
        descricao.put(l, id);
        return id;
    }

    /** Fracao do inicio e do fim do rolamento que mistura com a postura de partida (joelhos/deitado). */
    static final double BORDA_POSTURA_ROLAMENTO = 0.2;
    /** Fim do rolamento ja na postura (o ultimo tick desenhado e um pouco antes de p = 1). */
    static final double FOLGA_FIM_ROLAMENTO = 0.06;

    private static int dxRolamento(Direcao d) {
        return d == null ? 0 : d.name().contains("DIREITA") ? 1 : d.name().contains("ESQUERDA") ? -1 : 0;
    }

    private String idRolamento(IrmaoMiau l) {
        Direcao d = l.getDirecaoRolamento();
        int dx = dxRolamento(d);
        boolean diag = d != null && dx != 0 && (d.name().startsWith("CIMA") || d.name().startsWith("BAIXO"));
        String tipo = dx == 0 ? "LATERAL" : ((dx > 0) == l.isViradoParaDireita() ? "FRENTE" : "TRAS") + (diag ? "_DIAGONAL" : "");
        IrmaoMiau.EstadoCorporal post = l.getPosturaDoRolamento();
        return "rolamento_5o:" + tipo + (post == IrmaoMiau.EstadoCorporal.EM_PE ? "" : ":" + post.name());
    }

    /** Pose do rolamento no progresso p (sequencia de pe), com a mistura 70/30 nas diagonais; null = sem arquivo/rig. */
    private Double poseRolamento(IrmaoMiau l, double p, int[] pose) {
        Direcao d = l.getDirecaoRolamento();
        int dx = dxRolamento(d);
        int dz = d == null ? 0 : d.name().startsWith("CIMA") || d.name().startsWith("BAIXO") ? 1 : 0;
        BibliotecaMovimentos.Movimento principal;
        BibliotecaMovimentos.Movimento lateral = null;
        if (dx == 0) {
            principal = buscar("rolamento_lateral_5o");
        } else {
            // Pelo lado para onde ele olha AGORA (o desenho e espelhado por ele): rolar para onde olha = frente.
            boolean frente = (dx > 0) == l.isViradoParaDireita();
            principal = buscar(frente ? "rolamento_frente_5o" : "rolamento_tras_5o");
            lateral = dz != 0 ? buscar("rolamento_lateral_5o") : null;
        }
        if (principal == null || !principal.getRig().equals(l.getRigAnimacao())) {
            return null;
        }
        principal.noProgresso(p, pose);
        double dy = assentamento.dy(l, principal, p);             // 5O.1: assentado neste personagem
        if (lateral != null) {
            // Diagonal: o giro e o da frente/tras; o corpo pega 30% do rolamento lateral (ombro e quadril).
            int[] lat = new int[30];
            lateral.noProgresso(p, lat);
            double dyLat = assentamento.dy(l, lateral, p);
            for (int k = 4; k < 30; k++) {
                if (k != 10) {
                    pose[k] = (int) Math.round(pose[k] * 0.7 + lat[k] * 0.3);
                }
            }
            dy = dy * 0.7 + dyLat * 0.3;
        }
        pose[2] = (int) Math.round(l.getEscala() * 10);
        pose[3] = 1;
        return dy;
    }

    /**
     * 5O.1: de joelhos ou deitado o rolamento SAI da postura e VOLTA a ela (nao
     * levanta no desenho): nas bordas mistura a postura com o grupado; o giro e
     * normalizado (360 = 0) para nao desenrolar ao contrario.
     */
    private Double poseRolamentoNaPostura(IrmaoMiau l, double p, int[] pose) {
        IrmaoMiau.EstadoCorporal post = l.getPosturaDoRolamento();
        if (post == IrmaoMiau.EstadoCorporal.EM_PE || post == IrmaoMiau.EstadoCorporal.AGACHADO) {
            return poseRolamento(l, p, pose);
        }
        if (post == IrmaoMiau.EstadoCorporal.DEITADO) {
            return poseRolamentoDeitado(l, p, pose);
        }
        double b = BORDA_POSTURA_ROLAMENTO;
        if (p >= b && p <= 1 - b) {
            return poseRolamento(l, p, pose);
        }
        double q = p < b ? b : 1 - b;
        int[] grupado = new int[30];
        Double dyG = poseRolamento(l, q, grupado);
        if (dyG == null) {
            return null;
        }
        grupado[10] = (int) Math.round(Math.IEEEremainder(grupado[10], 360));
        TransicoesPostura.Pose base = posturas.pose(l, post);
        // no fim chega a postura um pouco antes do ultimo tick (o ultimo quadro desenhado ja e a postura)
        double t = p < b ? ReacaoReceptor.suave(p / b)
                : ReacaoReceptor.suave((1 - p - FOLGA_FIM_ROLAMENTO) / (b - FOLGA_FIM_ROLAMENTO));
        int[] m = ReacaoReceptor.mistura(base.valores(), grupado, t);
        System.arraycopy(m, 0, pose, 0, 30);
        pose[2] = grupado[2];
        pose[3] = 1;
        return base.dy() + (dyG - base.dy()) * t;
    }

    /**
     * 5O.1: a partir de DEITADO o corpo nao senta: sai da pose deitada, gira 360
     * graus (para a frente ou para tras pelo lado para onde olha; lateral = um
     * balanco) com os membros grupando no meio e volta deitado, sempre no nivel
     * do corpo deitado (assentado por personagem).
     */
    private Double poseRolamentoDeitado(IrmaoMiau l, double p, int[] pose) {
        int[] bola = new int[30];
        if (poseRolamento(l, 0.5, bola) == null) {
            return null;
        }
        TransicoesPostura.Pose base = posturas.pose(l, IrmaoMiau.EstadoCorporal.DEITADO);
        int[] b = base.valores();
        int dx = dxRolamento(l.getDirecaoRolamento());
        int sentido = dx == 0 ? 0 : (dx > 0) == l.isViradoParaDireita() ? 1 : -1;
        double w = ReacaoReceptor.suave(Math.min(1.0, Math.sin(Math.PI * p) * 1.6));
        int[] m = ReacaoReceptor.mistura(b, bola, w);
        m[10] = b[10] + (int) Math.round(sentido != 0 ? sentido * 360 * ReacaoReceptor.suave(p)
                : 25 * Math.sin(2 * Math.PI * p));
        System.arraycopy(m, 0, pose, 0, 30);
        pose[0] = b[0];
        pose[1] = b[1];
        pose[2] = bola[2];
        pose[3] = 1;
        int[] ref = b.clone();
        ref[2] = bola[2];
        Double dy = assentamento.alinharCom(l, pose, ref, base.dy());
        return dy == null ? base.dy() : dy;
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
        aplicarPassos(l, destino, ajoelhado || l.isDeitado());
        return base.dy();
    }

    /**
     * 5O.1: guarda andando = parte de cima da guarda (bracos, tronco, cabeca)
     * + passos curtos so nas pernas, com os pes alternando. A amplitude sobe e
     * desce em rampa, entao parar/comecar a andar e trocar de guarda nao dao
     * tranco nem recomecam a pose. De joelhos as pernas sao as da postura.
     */
    private void aplicarPassos(IrmaoMiau l, int[] p, boolean semPernas) {
        double[] st = passos.computeIfAbsent(l, k -> new double[2]);
        boolean anda = l.isAndando() && !semPernas;
        st[1] = anda ? Math.min(1.0, st[1] + RAMPA_PASSO_POR_TICK) : Math.max(0.0, st[1] - RAMPA_PASSO_POR_TICK);
        if (anda) {
            st[0] += FASE_PASSO_POR_TICK;
        }
        if (st[1] <= 0 || semPernas) {
            return;
        }
        double s = Math.sin(st[0]) * st[1];
        p[22] += (int) Math.round(7 * s);                          // joelho esquerdo
        p[24] += (int) Math.round(11 * s);                         // pe esquerdo
        p[25] -= (int) Math.round(6 * Math.max(0, s));             // levanta o pe que vai a frente
        p[26] -= (int) Math.round(7 * s);
        p[28] -= (int) Math.round(11 * s);
        p[29] -= (int) Math.round(6 * Math.max(0, -s));
        p[13] += (int) Math.round(Math.abs(s));                    // cabeca: 1 px, sem balanco exagerado
    }

    /** 5O.1: amplitude atual dos passos da guarda (0 = parado), para HUD e testes. */
    public double amplitudePassosGuarda(IrmaoMiau l) {
        double[] st = passos.get(l);
        return st == null ? 0.0 : st[1];
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
        passos.remove(l);
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
