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

    private static final double ESCALA_MINIMA_SEGURA = 0.0001;

    private final IrmaoMiau atacante;
    private final IrmaoMiau defensor;

    private final double centroAtacanteX;
    private final double centroDefensorX;

    /* Distancia real na tela, util para debug/render. */
    private final double distanciaHorizontalPixels;

    /*
     * Distancia normalizada pela escala do atacante.
     * Esta e a medida que as acoes usam para alcance.
     */
    private final double distanciaHorizontalCombate;

    private final double escalaAtacante;
    private final double escalaDefensor;

    private final int linhaAtacante;
    private final int linhaDefensor;
    private final int diferencaLinhas;

    private final RelacaoHorizontal relacaoHorizontal;

    private final boolean atacanteViradoParaDireita;
    private final boolean defensorViradoParaDireita;
    private final boolean atacanteDeFrenteParaDefensor;
    private final boolean defensorDeFrenteParaAtacante;

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
         * A cintura/root e o ponto fisico de referencia dos personagens.
         */
        this.centroAtacanteX = atacante.getEixoCinturaX();
        this.centroDefensorX = defensor.getEixoCinturaX();

        this.escalaAtacante = atacante.getEscala();
        this.escalaDefensor = defensor.getEscala();

        this.distanciaHorizontalPixels = Math.abs(
                centroDefensorX - centroAtacanteX
        );

        double escalaSeguraAtacante = Math.max(
                ESCALA_MINIMA_SEGURA,
                Math.abs(escalaAtacante)
        );

        this.distanciaHorizontalCombate =
                distanciaHorizontalPixels / escalaSeguraAtacante;

        this.linhaAtacante = atacante.getLinhaAtual();
        this.linhaDefensor = defensor.getLinhaAtual();
        this.diferencaLinhas = Math.abs(
                linhaDefensor - linhaAtacante
        );

        this.relacaoHorizontal = calcularRelacaoHorizontal(
                centroAtacanteX,
                centroDefensorX
        );

        this.atacanteViradoParaDireita = atacante.isViradoParaDireita();
        this.defensorViradoParaDireita = defensor.isViradoParaDireita();

        this.atacanteDeFrenteParaDefensor = calcularDeFrente(
                centroAtacanteX,
                centroDefensorX,
                atacanteViradoParaDireita
        );

        this.defensorDeFrenteParaAtacante = calcularDeFrente(
                centroDefensorX,
                centroAtacanteX,
                defensorViradoParaDireita
        );

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

    private static RelacaoHorizontal calcularRelacaoHorizontal(
            double centroAtacanteX,
            double centroDefensorX
    ) {

        int comparacao = Double.compare(
                centroAtacanteX,
                centroDefensorX
        );

        if (comparacao < 0) {
            return RelacaoHorizontal.ATACANTE_A_ESQUERDA;
        }

        if (comparacao > 0) {
            return RelacaoHorizontal.ATACANTE_A_DIREITA;
        }

        return RelacaoHorizontal.MESMO_EIXO;
    }

    private static boolean calcularDeFrente(
            double centroPersonagemX,
            double centroOponenteX,
            boolean viradoParaDireita
    ) {

        if (Double.compare(
                centroPersonagemX,
                centroOponenteX
        ) == 0) {

            /*
             * No mesmo eixo horizontal nao existe um "lado errado".
             * Isso evita uma tecnica ficar indisponivel exatamente no
             * instante em que os centros se cruzam.
             */
            return true;
        }

        if (centroOponenteX > centroPersonagemX) {
            return viradoParaDireita;
        }

        return !viradoParaDireita;
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

    /**
     * Distancia usada pelas regras de alcance da luta.
     *
     * Mantemos este nome curto como alias da distancia normalizada.
     */
    public double getDistanciaHorizontal() {
        return distanciaHorizontalCombate;
    }

    public double getDistanciaHorizontalPixels() {
        return distanciaHorizontalPixels;
    }

    public double getDistanciaHorizontalCombate() {
        return distanciaHorizontalCombate;
    }

    public double getEscalaAtacante() {
        return escalaAtacante;
    }

    public double getEscalaDefensor() {
        return escalaDefensor;
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

    public RelacaoHorizontal getRelacaoHorizontal() {
        return relacaoHorizontal;
    }

    public boolean isAtacanteAEsquerda() {
        return relacaoHorizontal == RelacaoHorizontal.ATACANTE_A_ESQUERDA;
    }

    public boolean isAtacanteADireita() {
        return relacaoHorizontal == RelacaoHorizontal.ATACANTE_A_DIREITA;
    }

    public boolean isMesmoEixoHorizontal() {
        return relacaoHorizontal == RelacaoHorizontal.MESMO_EIXO;
    }

    public boolean isMesmaLinha() {
        return diferencaLinhas == 0;
    }

    public boolean isAtacanteViradoParaDireita() {
        return atacanteViradoParaDireita;
    }

    public boolean isDefensorViradoParaDireita() {
        return defensorViradoParaDireita;
    }

    public boolean isAtacanteDeFrenteParaDefensor() {
        return atacanteDeFrenteParaDefensor;
    }

    public boolean isDefensorDeFrenteParaAtacante() {
        return defensorDeFrenteParaAtacante;
    }

    public boolean isFrenteAFrente() {
        return atacanteDeFrenteParaDefensor
                && defensorDeFrenteParaAtacante;
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
