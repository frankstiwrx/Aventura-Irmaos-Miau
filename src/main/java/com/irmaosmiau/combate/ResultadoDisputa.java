package com.irmaosmiau.combate;

import java.util.Locale;

/** Resultado explicito de uma disputa tecnica (com as pontuacoes, para debug e testes). */
public final class ResultadoDisputa {

    private final TipoDisputa tipo;
    private final double pontuacaoAtaque;
    private final double pontuacaoDefesa;
    private final boolean sucessoDoAtaque;

    ResultadoDisputa(TipoDisputa tipo, double pontuacaoAtaque, double pontuacaoDefesa, boolean sucessoDoAtaque) {
        this.tipo = tipo;
        this.pontuacaoAtaque = pontuacaoAtaque;
        this.pontuacaoDefesa = pontuacaoDefesa;
        this.sucessoDoAtaque = sucessoDoAtaque;
    }

    public TipoDisputa getTipo() {
        return tipo;
    }

    public double getPontuacaoAtaque() {
        return pontuacaoAtaque;
    }

    public double getPontuacaoDefesa() {
        return pontuacaoDefesa;
    }

    /** true = o lado que agiu venceu; false = o outro lado resistiu (inclui empate). */
    public boolean isSucessoDoAtaque() {
        return sucessoDoAtaque;
    }

    public double getMargem() {
        return pontuacaoAtaque - pontuacaoDefesa;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s %.3f x %.3f -> %s", tipo, pontuacaoAtaque, pontuacaoDefesa,
                sucessoDoAtaque ? "SUCESSO" : "RESISTIDO");
    }
}
