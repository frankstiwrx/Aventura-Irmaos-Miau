package com.irmaosmiau.posicional;

/** Condicao para INICIAR uma transicao (Entrega 5E). */
public interface CondicaoTransicao {

    String descricao();

    boolean atende(EstadoPosicional estado, Lado iniciador, FatosCombate fatos);

    /** A PEGADA_1 precisa estar estabelecida (fim dos 30 quadros). */
    CondicaoTransicao PEGADA_ESTABELECIDA = new CondicaoTransicao() {
        @Override
        public String descricao() {
            return "pegada estabelecida";
        }

        @Override
        public boolean atende(EstadoPosicional estado, Lado iniciador, FatosCombate fatos) {
            return fatos.pegadaEstabelecida();
        }
    };
}
