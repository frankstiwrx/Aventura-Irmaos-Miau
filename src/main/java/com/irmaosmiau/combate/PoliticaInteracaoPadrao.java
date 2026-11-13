package com.irmaosmiau.combate;

import java.util.Optional;

/**
 * Politica provisoria aprovada para a PEGADA_1 (Entrega 3A), valida para
 * qualquer interacao enquanto nao existir movimentacao conjunta.
 *
 * Sempre (inclusive durante a entrada):
 *  - fora do alcance da acao        -> ALCANCE
 *  - mais linhas que o permitido    -> LINHAS
 *  - aplicador de costas (se exigido) -> ORIENTACAO
 *
 * Depois de estabelecida (entrada terminou), comparando o corpo de CADA
 * lutador com o retrato tirado no estabelecimento:
 *  - comecou a rolar                -> ROLAMENTO
 *  - comecou um sprawl              -> SPRAWL
 *  - comecou a pular                -> PULO
 *  - deitou ou levantou             -> DEITAR_OU_LEVANTAR
 *  - agachou ou deixou de agachar   -> AGACHAMENTO
 *  - mudou de X ou de Y (andou, correu, trocou de profundidade) -> MOVIMENTO
 *
 * Ficar parado (inclusive respirando) mantem a interacao e a pose final.
 * A regra observa o que o corpo FEZ; uma acao que nao pode acontecer
 * (ex.: pulo sem folego) nao rompe nada.
 */
public final class PoliticaInteracaoPadrao implements PoliticaInteracao {

    @Override
    public Optional<MotivoEncerramentoInteracao> avaliar(SituacaoInteracao s) {

        AcaoCombate acao = s.getAcao();
        ContextoCombate c = s.getContexto();

        if (c.getDistanciaHorizontal() > acao.getAlcanceHorizontal()) {
            return Optional.of(MotivoEncerramentoInteracao.ALCANCE);
        }
        if (c.getDiferencaLinhas() > acao.getDiferencaMaximaLinhas()) {
            return Optional.of(MotivoEncerramentoInteracao.LINHAS);
        }
        if (acao.isExigeFrenteParaOponente() && !c.isAtacanteDeFrenteParaDefensor()) {
            return Optional.of(MotivoEncerramentoInteracao.ORIENTACAO);
        }

        if (s.isEntradaEmAndamento()) {
            // Os dois estao bloqueados pela animacao: nada corporal rompe.
            return Optional.empty();
        }

        Optional<MotivoEncerramentoInteracao> aplicador =
                mudancaCorporal(s.getReferenciaAplicador(), s.getAtualAplicador());
        if (aplicador.isPresent()) {
            return aplicador;
        }
        return mudancaCorporal(s.getReferenciaReceptor(), s.getAtualReceptor());
    }

    /** Primeira mudanca corporal relevante entre dois retratos, se houver. */
    static Optional<MotivoEncerramentoInteracao> mudancaCorporal(
            EstadoCorporalObservado antes,
            EstadoCorporalObservado agora
    ) {
        if (agora.isRolando() && !antes.isRolando()) {
            return Optional.of(MotivoEncerramentoInteracao.ROLAMENTO);
        }
        if (agora.isFazendoSprawl() && !antes.isFazendoSprawl()) {
            return Optional.of(MotivoEncerramentoInteracao.SPRAWL);
        }
        if (agora.isPulando() && !antes.isPulando()) {
            return Optional.of(MotivoEncerramentoInteracao.PULO);
        }
        if (agora.isDeitado() != antes.isDeitado()) {
            return Optional.of(MotivoEncerramentoInteracao.DEITAR_OU_LEVANTAR);
        }
        if (agora.isAgachado() != antes.isAgachado()) {
            return Optional.of(MotivoEncerramentoInteracao.AGACHAMENTO);
        }
        if (agora.getX() != antes.getX()
                || agora.getY() != antes.getY()
                || agora.getLinha() != antes.getLinha()) {
            return Optional.of(MotivoEncerramentoInteracao.MOVIMENTO);
        }
        return Optional.empty();
    }
}
