package com.irmaosmiau.hud;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Fotografia, para o HUD, dos recursos de um lutador.
 *
 * So le valores publicos do personagem (nenhuma regra de atributo e
 * recalculada aqui): folego/condicionamento atuais e os maximos que vem
 * dos atributos.
 */
public final class DadosLutadorHud {

    private final String rotulo;
    private final String nome;
    private final double folegoAtual;
    private final double folegoMaximo;
    private final double condicionamentoAtual;
    private final double condicionamentoMaximo;

    public DadosLutadorHud(String rotulo, String nome, double folegoAtual, double folegoMaximo,
            double condicionamentoAtual, double condicionamentoMaximo) {
        this.rotulo = rotulo;
        this.nome = nome;
        this.folegoAtual = folegoAtual;
        this.folegoMaximo = folegoMaximo;
        this.condicionamentoAtual = condicionamentoAtual;
        this.condicionamentoMaximo = condicionamentoMaximo;
    }

    /**
     * @param rotulo texto curto acima do nome ("JOGADOR", "PARCEIRO"...),
     *               ou null
     */
    public static DadosLutadorHud de(IrmaoMiau lutador, String rotulo) {
        return new DadosLutadorHud(
                rotulo,
                lutador.getNome(),
                lutador.getFolegoAtual(),
                lutador.getAtributos().getFolego(),
                lutador.getCondicionamentoAtual(),
                lutador.getAtributos().getCondicionamento()
        );
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getNome() {
        return nome;
    }

    public double getFolegoAtual() {
        return folegoAtual;
    }

    public double getFolegoMaximo() {
        return folegoMaximo;
    }

    public double getCondicionamentoAtual() {
        return condicionamentoAtual;
    }

    public double getCondicionamentoMaximo() {
        return condicionamentoMaximo;
    }

    public double getPercentualFolego() {
        return percentual(folegoAtual, folegoMaximo);
    }

    public double getPercentualCondicionamento() {
        return percentual(condicionamentoAtual, condicionamentoMaximo);
    }

    /** Ex.: "22/25" (arredondado, so para exibicao). */
    public String textoFolego() {
        return Math.round(folegoAtual) + "/" + Math.round(folegoMaximo);
    }

    public String textoCondicionamento() {
        return Math.round(condicionamentoAtual) + "/" + Math.round(condicionamentoMaximo);
    }

    private static double percentual(double atual, double maximo) {
        if (maximo <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, atual / maximo));
    }
}
