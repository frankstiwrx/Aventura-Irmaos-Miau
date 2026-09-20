package com.irmaosmiau.entities;

public class AtributosLutador {

    private final double condicionamento;
    private final double folego;

    private final double forca;
    private final double velocidade;
    private final double agilidade;
    private final double tecnica;
    private final double recuperacao;

    private final double peso;

    public AtributosLutador(
            double condicionamento,
            double folego,
            double forca,
            double velocidade,
            double agilidade,
            double tecnica,
            double recuperacao,
            double peso
    ) {

        this.condicionamento = condicionamento;
        this.folego = folego;

        this.forca = forca;
        this.velocidade = velocidade;
        this.agilidade = agilidade;
        this.tecnica = tecnica;
        this.recuperacao = recuperacao;

        this.peso = peso;
    }

    public double getCondicionamento() {
        return condicionamento;
    }

    public double getFolego() {
        return folego;
    }

    public double getForca() {
        return forca;
    }

    public double getVelocidade() {
        return velocidade;
    }

    public double getAgilidade() {
        return agilidade;
    }

    public double getTecnica() {
        return tecnica;
    }

    public double getRecuperacao() {
        return recuperacao;
    }

    public double getPeso() {
        return peso;
    }
}