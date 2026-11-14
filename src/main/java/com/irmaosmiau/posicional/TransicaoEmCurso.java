package com.irmaosmiau.posicional;

/**
 * Tentativa em andamento (Entrega 5E). Imutavel; o MotorPosicional cria uma
 * nova a cada tick. Os ticks sao do loop logico (nunca quadros desenhados).
 *
 * @param defesaSinalizada o outro lutador reagiu dentro da janela (defesa
 *                         simultanea); o resolvedor futuro usa isso na disputa
 */
public record TransicaoEmCurso(DefinicaoTransicao definicao, Lado iniciador, int ticksDecorridos,
        boolean defesaSinalizada) {

    public boolean isPronta() {
        return ticksDecorridos >= definicao.getDuracaoTicks();
    }

    /** 0..1 da preparacao (para HUD/animacao no futuro). */
    public double progresso() {
        return Math.min(1.0, ticksDecorridos / (double) definicao.getDuracaoTicks());
    }

    TransicaoEmCurso avancar() {
        return new TransicaoEmCurso(definicao, iniciador, ticksDecorridos + 1, defesaSinalizada);
    }

    TransicaoEmCurso comDefesa() {
        return new TransicaoEmCurso(definicao, iniciador, ticksDecorridos, true);
    }

    @Override
    public String toString() {
        return definicao.getId() + " por " + iniciador + " " + ticksDecorridos + "/" + definicao.getDuracaoTicks()
                + (defesaSinalizada ? " (defesa)" : "");
    }
}
