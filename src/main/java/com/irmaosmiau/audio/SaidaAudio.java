package com.irmaosmiau.audio;

/**
 * Onde o som efetivamente toca. Separado do ServicoAudio para que a logica
 * (musica, efeitos, mudo, volume) seja testavel sem placa de som.
 */
public interface SaidaAudio {

    /**
     * Inicia uma reproducao. Nunca lanca excecao: se nao for possivel
     * tocar, devolve {@link Reproducao#NENHUMA}.
     *
     * @param volume 0.0 (mudo) a 1.0 (volume original)
     */
    Reproducao tocar(SomCarregado som, boolean emLoop, double volume);

    /** true quando esta saida nunca produz som (ex.: ambiente headless). */
    boolean isSilenciosa();

    /** Uma reproducao em andamento. */
    interface Reproducao {

        void parar();

        void setVolume(double volume);

        boolean isAtiva();

        /** Reproducao vazia, usada quando nada pode tocar. */
        Reproducao NENHUMA = new Reproducao() {
            @Override
            public void parar() {
            }

            @Override
            public void setVolume(double volume) {
            }

            @Override
            public boolean isAtiva() {
                return false;
            }
        };
    }

    /** Saida que nao toca nada. */
    SaidaAudio SILENCIOSA = new SaidaAudio() {
        @Override
        public Reproducao tocar(SomCarregado som, boolean emLoop, double volume) {
            return Reproducao.NENHUMA;
        }

        @Override
        public boolean isSilenciosa() {
            return true;
        }
    };
}
