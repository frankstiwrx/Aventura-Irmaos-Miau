package com.irmaosmiau.animacao;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Liga o ControladorAnimacoes ao runtime da partida (Entrega 5G).
 *
 * Uma chamada por tick LOGICO, depois do movimento e do combate:
 * 1. estado visual do lutador (ControladorAnimacoes, somente leitura);
 * 2. animacao do perfil do personagem (PerfisAnimacoes);
 * 3. se for JSON: quadro pelo tempo da animacao, contado em ticks desde que
 *    o estado comecou (comeca sempre pelo 1o quadro da sequencia), e a pose
 *    vai para o renderizador do proprio personagem (ReprodutorPoseJson, o
 *    mesmo canal de pose da PEGADA_1);
 * 4. se for legada (ou o JSON nao existir): libera e o codigo antigo desenha.
 *
 * Nunca move ninguem (x, y e linha ficam com as regras do jogo) e nunca
 * passa por cima da pose do combate.
 */
public final class AnimadorPartida {

    /** De onde vem o desenho do lutador agora (inspecao para testes/diagnostico). */
    public record OrigemVisual(FonteAnimacao fonte, String idAnimacao, int quadro, EstadoVisual estado) {

        public boolean isJson() {
            return fonte == FonteAnimacao.JSON_INDIVIDUAL;
        }
    }

    private static final class Reproducao {
        final DefinicaoAnimacao definicao;
        final long tickInicio;
        final ReprodutorPoseJson reprodutor;
        int quadro;

        Reproducao(DefinicaoAnimacao definicao, long tickInicio) {
            this.definicao = definicao;
            this.tickInicio = tickInicio;
            this.reprodutor = new ReprodutorPoseJson(definicao.getDados().orElseThrow());
        }
    }

    private final ControladorAnimacoes controlador;
    private final double msPorTick;
    private final Map<IrmaoMiau, Reproducao> ativas = new IdentityHashMap<>();
    private final Map<IrmaoMiau, OrigemVisual> origens = new IdentityHashMap<>();
    // Entrega 5J: passos do arrasto e reacao ao desequilibrio sobre a pose da pegada.
    private final CamadaPoseCombate camada = new CamadaPoseCombate();
    // Entrega 5K: transicoes de postura (procedurais); deitado = quadro 1 do JSON do chao.
    private final TransicoesPostura transicoes = new TransicoesPostura(this::poseDeitado);
    // Entrega 5L: reacoes curtas a eventos defensivos (fora da pegada).
    // 5N: a liberacao do receptor comeca da ultima reacao dele (camada).
    private final ReacoesDefesa reacoes = new ReacoesDefesa(transicoes, camada::snapshotReceptor);
    // Entrega 5L: fuga de quadril: quadro pela distancia andada e sentido (frente/tras).
    private final Map<IrmaoMiau, double[]> trilhaChao = new IdentityHashMap<>();
    /** Pixels andados por quadro da fuga de quadril. */
    public static final double PX_POR_QUADRO_FUGA = 3.0;

    /** @param msPorTick duracao nominal de um tick do loop logico (GamePanel: 16 ms) */
    public AnimadorPartida(ControladorAnimacoes controlador, double msPorTick) {
        this.controlador = controlador;
        this.msPorTick = msPorTick;
    }

    /**
     * @param lutadores          os lutadores em campo AGORA (os que sairam sao liberados)
     * @param sistema            pode ser null (modo individual)
     * @param deslocandoConjunto arrasto 5B neste tick
     * @param tick               relogio logico do jogo (para durante a pausa)
     */
    public void atualizar(Collection<IrmaoMiau> lutadores, SistemaCombate sistema, boolean deslocandoConjunto,
            long tick) {
        for (IrmaoMiau l : new ArrayList<>(ativas.keySet())) {
            if (!contem(lutadores, l)) {
                liberar(l);
            }
        }
        origens.keySet().removeIf(l -> !contem(lutadores, l));
        reacoes.observar(sistema, tick);
        for (IrmaoMiau l : lutadores) {
            atualizar(l, sistema, deslocandoConjunto, tick);
            camada.atualizar(l, sistema, deslocandoConjunto, tick);
        }
    }

    private static boolean contem(Collection<IrmaoMiau> c, IrmaoMiau l) {
        for (IrmaoMiau x : c) {
            if (x == l) {
                return true;
            }
        }
        return false;
    }

    private void atualizar(IrmaoMiau l, SistemaCombate sistema, boolean deslocandoConjunto, long tick) {
        EstadoVisual estado = ControladorAnimacoes.estadoVisual(l, sistema, deslocandoConjunto);
        if (estado == EstadoVisual.TRANSICAO_POSTURA) {
            liberar(l);                                   // solta o JSON da postura anterior
            boolean desenhou = transicoes.aplicar(l);
            origens.put(l, new OrigemVisual(desenhou ? FonteAnimacao.PROCEDURAL : FonteAnimacao.LEGADA_CODIGO,
                    l.getTransicaoAtiva().getContrato(), (int) Math.round(l.getProgressoTransicao() * 100), estado));
            return;
        }
        transicoes.liberar(l);
        // Entrega 5L: reacao defensiva (fora da pegada; o combate tem prioridade).
        boolean naPegada = sistema != null && sistema.estaEmInteracao()
                && (sistema.getAplicadorInteracao() == l || sistema.getReceptorInteracao() == l);
        if (!naPegada && reacoes.ativa(l, tick) != null) {
            liberar(l);                                   // solta o JSON so enquanto a reacao desenha
            ReacoesDefesa.Tipo r0 = reacoes.aplicar(l, tick);
            if (r0 != null) {
                origens.put(l, new OrigemVisual(FonteAnimacao.PROCEDURAL, r0.name(), -1, EstadoVisual.REACAO_DEFESA));
                return;
            }
        } else {
            reacoes.liberar(l);
        }
        Optional<AnimacaoResolvida> r = controlador.resolver(l, sistema, deslocandoConjunto);
        if (r.isEmpty() || r.get().usaCodigoLegado()) {
            liberar(l);
            origens.put(l, new OrigemVisual(r.map(x -> x.definicao().fonte()).orElse(FonteAnimacao.LEGADA_CODIGO),
                    r.map(x -> x.definicao().id()).orElse(null), -1, estado));
            return;
        }
        DefinicaoAnimacao d = r.get().definicao();
        // Entrega 5L: no modo PROPOSTA (F6), o movimento no chao usa a fuga de quadril.
        boolean fuga = false;
        if (d.estado() == EstadoVisual.MOVIMENTO_CHAO
                && com.irmaosmiau.combate.ModoAnimacoesPegada.atual() == com.irmaosmiau.combate.ModoAnimacoesPegada.EXPERIMENTAL) {
            Optional<DefinicaoAnimacao> f = controlador.getPerfis().getCatalogo()
                    .buscar(d.id().substring(0, d.id().indexOf('.')) + ".fuga_quadril.json");
            if (f.isPresent()) {
                d = f.get();
                fuga = true;
            }
        }
        Reproducao rep = ativas.get(l);
        if (rep == null || !rep.definicao.id().equals(d.id())) {
            liberar(l);                                   // troca de estado: comeca do 1o quadro
            rep = new Reproducao(d, tick);
            ativas.put(l, rep);
        }
        AnimacaoIndividual a = d.getDados().orElseThrow();
        int quadro = fuga ? quadroDaFuga(l, a, rep.tickInicio == tick) : a.indiceQuadroNoTick(tick - rep.tickInicio, msPorTick);
        if (!rep.reprodutor.aplicarQuadro(l, quadro)) {
            // Outro dono da pose (combate): nao interferir.
            ativas.remove(l);
            origens.put(l, new OrigemVisual(FonteAnimacao.LEGADA_CODIGO, null, -1, estado));
            return;
        }
        rep.quadro = quadro;
        origens.put(l, new OrigemVisual(FonteAnimacao.JSON_INDIVIDUAL, d.id(), quadro, estado));
    }

    private void liberar(IrmaoMiau l) {
        Reproducao rep = ativas.remove(l);
        if (rep != null) {
            rep.reprodutor.liberar();
        }
    }

    /** Solta todos (fim da partida). */
    public void liberarTodos() {
        for (IrmaoMiau l : new ArrayList<>(ativas.keySet())) {
            liberar(l);
        }
        origens.clear();
    }

    /** De onde vem o desenho do lutador no ultimo tick (vazio = ainda nao atualizado). */
    public Optional<OrigemVisual> origem(IrmaoMiau lutador) {
        return Optional.ofNullable(origens.get(lutador));
    }

    /**
     * Entrega 5L: quadro da fuga de quadril pela DISTANCIA realmente andada
     * (parou = quadro parado; voltou a andar = continua de onde estava). Indo
     * para a frente (para onde olha) a sequencia roda ao contrario.
     */
    private int quadroDaFuga(IrmaoMiau l, AnimacaoIndividual a, boolean comecou) {
        double[] t = trilhaChao.computeIfAbsent(l, k -> new double[] {k.getX(), k.getY(), 0, -1});
        if (comecou) {
            t[0] = l.getX();
            t[1] = l.getY();
        }
        double dx = l.getX() - t[0];
        double dy = l.getY() - t[1];
        t[2] += Math.abs(dx) + Math.abs(dy);
        if (dx != 0) {
            boolean paraFrente = (dx > 0) == l.isViradoParaDireita();
            t[3] = paraFrente ? 1 : -1;
        }
        t[0] = l.getX();
        t[1] = l.getY();
        int[] seq = a.getSequencia();
        int passo = (int) (t[2] / PX_POR_QUADRO_FUGA) % seq.length;
        int i = t[3] > 0 ? (seq.length - passo) % seq.length : passo;
        return seq[i];
    }

    /** Fase da fuga de quadril (distancia acumulada em px; 0 = sem dados). */
    public double distanciaFuga(IrmaoMiau l) {
        double[] t = trilhaChao.get(l);
        return t == null ? 0 : t[2];
    }

    public ReacoesDefesa getReacoes() {
        return reacoes;
    }

    /** Pose deitado pelo JSON do movimento no chao do perfil (quadro 1), se houver. */
    private TransicoesPostura.Pose poseDeitado(IrmaoMiau l) {
        return controlador.getPerfis().doLutador(l)
                .flatMap(p -> p.resolver(EstadoVisual.MOVIMENTO_CHAO, controlador.getPerfis().getCatalogo()))
                .flatMap(r -> r.definicao().getDados())
                .map(a -> {
                    int[] q = a.quadro(0);
                    q[3] = 1;
                    return new TransicoesPostura.Pose(q, a.getDeslocamentoVisualY());
                })
                .orElse(null);
    }

    public TransicoesPostura getTransicoes() {
        return transicoes;
    }

    /** 5J: camada de arrasto/desequilibrio (HUD tecnico e testes). */
    public CamadaPoseCombate getCamada() {
        return camada;
    }

    /** Lutadores com animacao JSON ativa agora. */
    public List<IrmaoMiau> comJsonAtivo() {
        return new ArrayList<>(ativas.keySet());
    }
}
