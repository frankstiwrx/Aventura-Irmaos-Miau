package com.irmaosmiau.posicional;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Le a posicao EM PE a partir do SistemaCombate (Entrega 5E), sem duplicar
 * estado: o motor atual ja sabe se ha pegada, quem a aplicou e a firmeza.
 *
 * - sem interacao: EM_PE_SEPARADOS;
 * - PEGADA_1 nos 30 quadros de entrada: EM_PE_SEPARADOS com a transicao
 *   "estabelecer_pegada" em andamento (ticks = quadros da timeline);
 * - pegada estabelecida: EM_PE_PEGADA, controle = quem aplicou AGORA (o
 *   aplicador da pegada nao e um "atacante permanente": se a pegada cai e o
 *   outro pega, o controle passa para ele).
 */
public final class LeituraPosicionalEmPe {

    private LeituraPosicionalEmPe() {
    }

    public static Lado lado(SistemaCombate sistema, IrmaoMiau lutador) {
        if (lutador == sistema.getLutadorA()) {
            return Lado.A;
        }
        if (lutador == sistema.getLutadorB()) {
            return Lado.B;
        }
        throw new IllegalArgumentException("Lutador fora deste combate.");
    }

    public static EstadoPosicional ler(SistemaCombate sistema, CatalogoTransicoes catalogo) {
        if (!sistema.estaEmInteracao()) {
            return EstadoPosicional.separados();
        }
        Lado aplicador = lado(sistema, sistema.getAplicadorInteracao());
        if (sistema.isInteracaoEstabelecida()) {
            return EstadoPosicional.pegada(aplicador);
        }
        DefinicaoTransicao pegada = catalogo.buscar(CatalogoTransicoes.ESTABELECER_PEGADA).orElseThrow();
        int ticks = (int) Math.round(sistema.getProgressoAnimacaoCombate() * pegada.getDuracaoTicks());
        return EstadoPosicional.separados()
                .comTransicao(new TransicaoEmCurso(pegada, aplicador, ticks, false));
    }

    public static FatosCombate fatos(SistemaCombate sistema) {
        return new FatosCombate(sistema.isInteracaoEstabelecida(), sistema.getFirmeza());
    }
}
