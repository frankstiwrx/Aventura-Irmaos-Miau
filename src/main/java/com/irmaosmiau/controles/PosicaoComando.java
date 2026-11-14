package com.irmaosmiau.controles;

/** Uma das 12 posicoes de comando: camada + direcao (Entrega 5F). */
public record PosicaoComando(CamadaComando camada, DirecaoTecnica direcao) {

    @Override
    public String toString() {
        return "C" + camada.getNumero() + direcao.getSeta();
    }
}
