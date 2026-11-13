package com.irmaosmiau.controles;

/**
 * Conflito ou falta num mapa de controles (Entrega 5C).
 *
 * outroSlot/outraAcao so existem em TECLA_REPETIDA.
 */
public record ProblemaControle(Tipo tipo, SlotJogador slot, AcaoJogador acao,
        TeclaFisica tecla, SlotJogador outroSlot, AcaoJogador outraAcao) {

    public enum Tipo {
        /** A mesma tecla em duas acoes (no mesmo jogador ou entre os dois jogadores). */
        TECLA_REPETIDA,
        /** ESC ou F1..F12. */
        TECLA_RESERVADA,
        /** Acao sem tecla (ex.: acao nova cuja tecla padrao ja estava em uso). */
        SEM_TECLA
    }

    public String descricao() {
        String quem = slot.getSigla() + " · " + acao.getRotulo();
        return switch (tipo) {
            case TECLA_REPETIDA -> "Tecla " + tecla.getRotulo() + " usada em " + quem + " e em "
                    + outroSlot.getSigla() + " · " + outraAcao.getRotulo();
            case TECLA_RESERVADA -> "Tecla " + tecla.getRotulo() + " é reservada ao sistema (" + quem + ")";
            case SEM_TECLA -> quem + " está sem tecla";
        };
    }
}
