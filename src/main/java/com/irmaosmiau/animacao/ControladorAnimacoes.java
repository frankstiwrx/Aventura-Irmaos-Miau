package com.irmaosmiau.animacao;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;

import java.util.Optional;

/**
 * Ponto CENTRAL que decide qual animacao um lutador usa agora (Entrega 5E).
 *
 * Separacao:
 * - identidade: PerfisAnimacoes (por id de personagem);
 * - movimento: IrmaoMiau/GamePanel/SistemaCombate (nao sao tocados aqui);
 * - estado de combate: SistemaCombate (somente leitura);
 * - animacao: EstadoVisual -> PerfilAnimacoes -> DefinicaoAnimacao;
 * - dados visuais: AnimacaoIndividual (JSON) ou o codigo legado.
 *
 * Somente LE o lutador e o combate: nunca altera posicao, estado ou pose.
 */
public final class ControladorAnimacoes {

    private final PerfisAnimacoes perfis;

    public ControladorAnimacoes(PerfisAnimacoes perfis) {
        this.perfis = perfis;
    }

    public PerfisAnimacoes getPerfis() {
        return perfis;
    }

    /**
     * Estado visual pela prioridade do renderizador atual: combate
     * (pose externa) > rolamento > sprawl > deitado > pulo > agachado >
     * corrida/caminhada > taunt > parado.
     *
     * @param sistema            pode ser null (modo individual)
     * @param deslocandoConjunto o GamePanel esta arrastando a dupla neste tick (5B)
     */
    public static EstadoVisual estadoVisual(IrmaoMiau lutador, SistemaCombate sistema, boolean deslocandoConjunto) {
        if (sistema != null && sistema.estaEmInteracao()
                && (sistema.getAplicadorInteracao() == lutador || sistema.getReceptorInteracao() == lutador)) {
            if (!sistema.isInteracaoEstabelecida()) {
                return EstadoVisual.PEGADA_ENTRADA;
            }
            return deslocandoConjunto ? EstadoVisual.ARRASTO_CONJUNTO : EstadoVisual.PEGADA_ESTABELECIDA;
        }
        if (lutador.getTransicaoAtiva() != null) {
            return EstadoVisual.TRANSICAO_POSTURA;
        }
        if (lutador.isRolando()) {
            return EstadoVisual.ROLAMENTO;
        }
        if (lutador.isFazendoSprawl()) {
            return EstadoVisual.SPRAWL;
        }
        if (lutador.isDeitado()) {
            return lutador.estaAndando() ? EstadoVisual.MOVIMENTO_CHAO : EstadoVisual.DEITADO;
        }
        if (lutador.isAjoelhado()) {
            return lutador.estaAndando() ? EstadoVisual.AJOELHADO_MOVIMENTO : EstadoVisual.AJOELHADO;
        }
        if (lutador.isPulando()) {
            return EstadoVisual.PULO;
        }
        if (lutador.isAgachado()) {
            return EstadoVisual.AGACHADO;
        }
        if (lutador.estaAndando()) {
            return lutador.isCorrendo() ? EstadoVisual.CORRIDA : EstadoVisual.CAMINHADA;
        }
        return lutador.estaEmTaunt() ? EstadoVisual.TAUNT : EstadoVisual.PARADO;
    }

    /** Animacao escolhida para o lutador agora (vazio = sem perfil: legado puro). */
    public Optional<AnimacaoResolvida> resolver(IrmaoMiau lutador, SistemaCombate sistema, boolean deslocandoConjunto) {
        EstadoVisual estado = estadoVisual(lutador, sistema, deslocandoConjunto);
        return perfis.doLutador(lutador).flatMap(p -> p.resolver(estado, perfis.getCatalogo()));
    }

    /** Animacao de uma ACAO/estado para um personagem pelo id (telas, ferramentas, testes). */
    public Optional<AnimacaoResolvida> resolver(String idPersonagem, EstadoVisual estado) {
        return perfis.buscar(idPersonagem).flatMap(p -> p.resolver(estado, perfis.getCatalogo()));
    }
}
