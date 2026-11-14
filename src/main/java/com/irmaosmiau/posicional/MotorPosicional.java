package com.irmaosmiau.posicional;

import java.util.Optional;

/**
 * Executa transicoes entre posicoes em tempo real (Entrega 5E).
 *
 * Ciclo de uma tentativa (sem turnos, sem pausa):
 * 1. tentar(...): valida origem, papel, condicoes e se esta liberada no modo;
 * 2. atualizar(): um tick do loop logico por chamada (preparacao/janela);
 *    durante esse tempo o outro lado pode sinalizarDefesa(...);
 * 3. resolver(...) quando pronta: aplica o desfecho do resultado (sucesso,
 *    defendida, falha ou inversao) e os papeis mudam conforme o desfecho.
 *
 * O RESULTADO vem de fora (ResolvedorTransicao): no jogo, sera a disputa 5A
 * (ResolvedorDisputa) com atributos, folego e firmeza; nos testes, um valor
 * fixo. Nenhuma formula nova aqui.
 *
 * Modo JOGO recusa transicoes que possam terminar numa posicao ainda nao
 * jogavel (sem logica/visual prontos). Modo SIMULACAO libera tudo.
 */
public final class MotorPosicional {

    public enum Modo {
        JOGO,
        SIMULACAO
    }

    public enum ResultadoTentativa {
        INICIADA,
        TRANSICAO_EM_ANDAMENTO,
        DESCONHECIDA,
        POSICAO_INCOMPATIVEL,
        PAPEL_INCOMPATIVEL,
        CONDICAO_NAO_ATENDIDA,
        NAO_LIBERADA_NO_JOGO
    }

    /** Decide o resultado de uma tentativa pronta (no futuro: disputa 5A). */
    @FunctionalInterface
    public interface ResolvedorTransicao {
        ResultadoTransicao resolver(TransicaoEmCurso transicao, FatosCombate fatos);
    }

    private final Modo modo;
    private final CatalogoTransicoes catalogo;
    private EstadoPosicional estado;

    public MotorPosicional(Modo modo, CatalogoTransicoes catalogo, EstadoPosicional inicial) {
        this.modo = modo;
        this.catalogo = catalogo;
        this.estado = inicial;
    }

    public EstadoPosicional getEstado() {
        return estado;
    }

    public ResultadoTentativa tentar(Lado iniciador, String idTransicao, FatosCombate fatos) {
        if (estado.getTransicao().isPresent()) {
            return ResultadoTentativa.TRANSICAO_EM_ANDAMENTO;
        }
        Optional<DefinicaoTransicao> d = catalogo.buscar(idTransicao);
        if (d.isEmpty()) {
            return ResultadoTentativa.DESCONHECIDA;
        }
        DefinicaoTransicao def = d.get();
        if (def.getOrigem() != estado.getPosicao()) {
            return ResultadoTentativa.POSICAO_INCOMPATIVEL;
        }
        if (!estado.temPapel(iniciador, def.getPapelIniciador())) {
            return ResultadoTentativa.PAPEL_INCOMPATIVEL;
        }
        for (CondicaoTransicao c : def.getCondicoes()) {
            if (!c.atende(estado, iniciador, fatos)) {
                return ResultadoTentativa.CONDICAO_NAO_ATENDIDA;
            }
        }
        if (modo == Modo.JOGO && !liberadaNoJogo(def)) {
            return ResultadoTentativa.NAO_LIBERADA_NO_JOGO;
        }
        estado = estado.comTransicao(new TransicaoEmCurso(def, iniciador, 0, false));
        return ResultadoTentativa.INICIADA;
    }

    /** Todas as posicoes onde ela pode terminar sao jogaveis? */
    public static boolean liberadaNoJogo(DefinicaoTransicao def) {
        return def.destinosPossiveis().stream().allMatch(Posicao::isJogavelNoJogo);
    }

    /** Um tick do loop logico. */
    public void atualizar() {
        estado.getTransicao().ifPresent(t -> estado = estado.comTransicao(t.avancar()));
    }

    /** O OUTRO lado reage dentro da janela de preparacao. */
    public boolean sinalizarDefesa(Lado defensor) {
        Optional<TransicaoEmCurso> t = estado.getTransicao();
        if (t.isEmpty() || t.get().iniciador() == defensor || t.get().isPronta()) {
            return false;
        }
        estado = estado.comTransicao(t.get().comDefesa());
        return true;
    }

    /**
     * Aplica o resultado de uma tentativa pronta.
     *
     * @throws IllegalStateException sem tentativa pronta, ou resultado impossivel para ela
     */
    public EstadoPosicional resolver(ResultadoTransicao resultado) {
        TransicaoEmCurso t = estado.getTransicao()
                .orElseThrow(() -> new IllegalStateException("Nenhuma transicao em andamento."));
        if (!t.isPronta()) {
            throw new IllegalStateException("Transicao ainda em preparacao: " + t);
        }
        Desfecho d = t.definicao().desfecho(resultado).orElseThrow(() -> new IllegalStateException(
                resultado + " nao e possivel em " + t.definicao().getId()));
        estado = d.aplicar(estado, t.iniciador());
        return estado;
    }

    public EstadoPosicional resolverCom(ResolvedorTransicao resolvedor, FatosCombate fatos) {
        TransicaoEmCurso t = estado.getTransicao()
                .orElseThrow(() -> new IllegalStateException("Nenhuma transicao em andamento."));
        return resolver(resolvedor.resolver(t, fatos));
    }

    /** Interrompe a tentativa (ex.: a pegada foi rompida); a posicao fica como estava. */
    public void cancelar() {
        estado.getTransicao().ifPresent(t -> estado = estado.comTransicao(null));
    }
}
