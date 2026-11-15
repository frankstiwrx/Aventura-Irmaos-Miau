package com.irmaosmiau.controles;

/**
 * Entrega 5K: clique simples x duplo clique de UM botao, pelo relogio logico
 * do jogo (ticks), sem depender do relogio do sistema (testavel).
 *
 * - Recebe so BORDAS de subida (segurar o botao nao gera cliques).
 * - O 1o clique fica pendente; um 2o clique dentro da janela vira DUPLO
 *   (uma acao so) e o simples NAO e executado.
 * - Se a janela passar sem 2o clique, sai o SIMPLES pendente.
 * - Depois de um DUPLO o estado zera: um 3o clique comeca uma nova contagem.
 *
 * Custo de ergonomia: o clique simples so e resolvido quando a janela acaba
 * (~250 ms), porque ate la ele ainda pode virar duplo.
 */
public final class ReconhecedorCliqueDuplo {

    /** O que resolveu neste tick. */
    public enum Evento { NADA, SIMPLES, DUPLO }

    /** Janela padrao do duplo clique. */
    public static final int JANELA_MS_PADRAO = 250;

    private final int janelaTicks;
    private long pendenteDesde = -1;

    /** @param janelaTicks maior intervalo (em ticks) entre os dois cliques */
    public ReconhecedorCliqueDuplo(int janelaTicks) {
        if (janelaTicks < 1) {
            throw new IllegalArgumentException("A janela precisa de pelo menos 1 tick.");
        }
        this.janelaTicks = janelaTicks;
    }

    /** Janela em ticks para um intervalo em ms (arredonda para cima). */
    public static int ticksDaJanela(int janelaMs, int msPorTick) {
        return Math.max(1, (int) Math.ceil(janelaMs / (double) msPorTick));
    }

    public int getJanelaTicks() {
        return janelaTicks;
    }

    /**
     * Um tick.
     *
     * @param borda o botao acabou de ser apertado neste tick
     * @param tick  relogio logico (cresce 1 por tick; parado na pausa)
     */
    public Evento atualizar(boolean borda, long tick) {
        if (borda) {
            if (pendenteDesde >= 0 && tick - pendenteDesde <= janelaTicks) {
                pendenteDesde = -1;
                return Evento.DUPLO;
            }
            boolean haviaVencido = pendenteDesde >= 0;
            pendenteDesde = tick;
            return haviaVencido ? Evento.SIMPLES : Evento.NADA;
        }
        if (pendenteDesde >= 0 && tick - pendenteDesde > janelaTicks) {
            pendenteDesde = -1;
            return Evento.SIMPLES;
        }
        return Evento.NADA;
    }

    /** Ha um clique esperando a janela fechar. */
    public boolean isPendente() {
        return pendenteDesde >= 0;
    }

    /** Descarta o clique pendente (pausa, foco, F5, troca de tela). */
    public void limpar() {
        pendenteDesde = -1;
    }
}
