package com.irmaosmiau.posicional;

/**
 * Fatos do motor atual que as condicoes de transicao podem consultar
 * (Entrega 5E). Somente leitura: vem do SistemaCombate (LeituraPosicionalEmPe)
 * ou, no simulador/testes, sao informados diretamente.
 *
 * @param pegadaEstabelecida a PEGADA_1 terminou os 30 quadros de entrada
 * @param firmeza            firmeza atual da pegada (0..100, 5B)
 */
public record FatosCombate(boolean pegadaEstabelecida, double firmeza) {

    public static final FatosCombate NENHUM = new FatosCombate(false, 0.0);

    public static FatosCombate pegadaFirme() {
        return new FatosCombate(true, 100.0);
    }
}
