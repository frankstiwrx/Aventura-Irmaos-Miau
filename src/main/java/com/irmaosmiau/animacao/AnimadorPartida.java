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
        Optional<AnimacaoResolvida> r = controlador.resolver(l, sistema, deslocandoConjunto);
        if (r.isEmpty() || r.get().usaCodigoLegado()) {
            liberar(l);
            origens.put(l, new OrigemVisual(r.map(x -> x.definicao().fonte()).orElse(FonteAnimacao.LEGADA_CODIGO),
                    r.map(x -> x.definicao().id()).orElse(null), -1, estado));
            return;
        }
        DefinicaoAnimacao d = r.get().definicao();
        Reproducao rep = ativas.get(l);
        if (rep == null || !rep.definicao.id().equals(d.id())) {
            liberar(l);                                   // troca de estado: comeca do 1o quadro
            rep = new Reproducao(d, tick);
            ativas.put(l, rep);
        }
        AnimacaoIndividual a = d.getDados().orElseThrow();
        int quadro = a.indiceQuadroNoTick(tick - rep.tickInicio, msPorTick);
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

    /** 5J: camada de arrasto/desequilibrio (HUD tecnico e testes). */
    public CamadaPoseCombate getCamada() {
        return camada;
    }

    /** Lutadores com animacao JSON ativa agora. */
    public List<IrmaoMiau> comJsonAtivo() {
        return new ArrayList<>(ativas.keySet());
    }
}
