package com.irmaosmiau.combate;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Fotografia do instante em que uma acao de combate e avaliada.
 *
 * O contexto evita espalhar dezenas de parametros pelos calculos do motor.
 * Ele apenas observa os personagens; nao altera estado, posicao ou energia.
 */
public final class ContextoCombate {

    private final IrmaoMiau atacante;
    private final IrmaoMiau defensor;

    private final double centroAtacanteX;
    private final double centroDefensorX;
    private final double distanciaHorizontal;

    private final int linhaAtacante;
    private final int linhaDefensor;
    private final int diferencaLinhas;

    private final boolean atacanteAEsquerda;

    private final boolean atacanteAgachado;
    private final boolean atacantePulando;
    private final boolean atacanteDeitado;
    private final boolean atacanteRolando;
    private final boolean atacanteFazendoSprawl;

    private final boolean defensorAgachado;
    private final boolean defensorPulando;
    private final boolean defensorDeitado;
    private final boolean defensorRolando;
    private final boolean defensorFazendoSprawl;

    private final double folegoAtacante;
    private final double folegoDefensor;
    private final double condicionamentoAtacante;
    private final double condicionamentoDefensor;

    private final AtributosLutador atributosAtacante;
    private final AtributosLutador atributosDefensor;

    public ContextoCombate(
            IrmaoMiau atacante,
            IrmaoMiau defensor
    ) {

        if (atacante == null) {
            throw new IllegalArgumentException("O atacante nao pode ser nulo.");
        }

        if (defensor == null) {
            throw new IllegalArgumentException("O defensor nao pode ser nulo.");
        }

        if (atacante == defensor) {
            throw new IllegalArgumentException("Atacante e defensor precisam ser personagens diferentes.");
        }

        this.atacante = atacante;
        this.defensor = defensor;

        /*
         * A cintura/root e o nosso ponto fisico de referencia.
         * Isso funciona melhor que usar apenas o X bruto do desenho e ja
         * permite personagens futuros com proporcoes diferentes.
         */
        this.centroAtacanteX = atacante.getEixoCinturaX();
        this.centroDefensorX = defensor.getEixoCinturaX();
        this.distanciaHorizontal = Math.abs(
                centroDefensorX - centroAtacanteX
        );

        this.linhaAtacante = atacante.getLinhaAtual();
        this.linhaDefensor = defensor.getLinhaAtual();
        this.diferencaLinhas = Math.abs(
                linhaDefensor - linhaAtacante
        );

        this.atacanteAEsquerda =
                centroAtacanteX < centroDefensorX;

        this.atacanteAgachado = atacante.isAgachado();
        this.atacantePulando = atacante.isPulando();
        this.atacanteDeitado = atacante.isDeitado();
        this.atacanteRolando = atacante.isRolando();
        this.atacanteFazendoSprawl = atacante.isFazendoSprawl();

        this.defensorAgachado = defensor.isAgachado();
        this.defensorPulando = defensor.isPulando();
        this.defensorDeitado = defensor.isDeitado();
        this.defensorRolando = defensor.isRolando();
        this.defensorFazendoSprawl = defensor.isFazendoSprawl();

        this.folegoAtacante = atacante.getFolegoAtual();
        this.folegoDefensor = defensor.getFolegoAtual();
        this.condicionamentoAtacante = atacante.getCondicionamentoAtual();
        this.condicionamentoDefensor = defensor.getCondicionamentoAtual();

        this.atributosAtacante = atacante.getAtributos();
        this.atributosDefensor = defensor.getAtributos();
    }

    public IrmaoMiau getAtacante() {
        return atacante;
    }

    public IrmaoMiau getDefensor() {
        return defensor;
    }

    public double getCentroAtacanteX() {
        return centroAtacanteX;
    }

    public double getCentroDefensorX() {
        return centroDefensorX;
    }

    public double getDistanciaHorizontal() {
        return distanciaHorizontal;
    }

    public int getLinhaAtacante() {
        return linhaAtacante;
    }

    public int getLinhaDefensor() {
        return linhaDefensor;
    }

    public int getDiferencaLinhas() {
        return diferencaLinhas;
    }

    public boolean isAtacanteAEsquerda() {
        return atacanteAEsquerda;
    }

    public boolean isAtacanteADireita() {
        return centroAtacanteX > centroDefensorX;
    }

    public boolean isMesmaLinha() {
        return diferencaLinhas == 0;
    }

    public boolean isAtacanteAgachado() {
        return atacanteAgachado;
    }

    public boolean isAtacantePulando() {
        return atacantePulando;
    }

    public boolean isAtacanteDeitado() {
        return atacanteDeitado;
    }

    public boolean isAtacanteRolando() {
        return atacanteRolando;
    }

    public boolean isAtacanteFazendoSprawl() {
        return atacanteFazendoSprawl;
    }

    public boolean isDefensorAgachado() {
        return defensorAgachado;
    }

    public boolean isDefensorPulando() {
        return defensorPulando;
    }

    public boolean isDefensorDeitado() {
        return defensorDeitado;
    }

    public boolean isDefensorRolando() {
        return defensorRolando;
    }

    public boolean isDefensorFazendoSprawl() {
        return defensorFazendoSprawl;
    }

    public double getFolegoAtacante() {
        return folegoAtacante;
    }

    public double getFolegoDefensor() {
        return folegoDefensor;
    }

    public double getCondicionamentoAtacante() {
        return condicionamentoAtacante;
    }

    public double getCondicionamentoDefensor() {
        return condicionamentoDefensor;
    }

    public AtributosLutador getAtributosAtacante() {
        return atributosAtacante;
    }

    public AtributosLutador getAtributosDefensor() {
        return atributosDefensor;
    }
}
