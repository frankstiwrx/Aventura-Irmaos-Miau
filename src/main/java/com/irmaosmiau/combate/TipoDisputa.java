package com.irmaosmiau.combate;

/**
 * Tipos de disputa tecnica e os PESOS de cada atributo para quem ataca e
 * para quem defende (Entrega 5A).
 *
 * Cada linha de pesos soma 1,0 e todos sao &gt;= 0: aumentar qualquer
 * atributo nunca piora o lado que o usa. O peso corporal fica entre
 * 0,10 e 0,20 (influencia moderada, nunca dominante).
 *
 * fatorPosicaoAtaque: vantagem de quem controla a acao. No desequilibrio o
 * aplicador ja segura a pegada (+10%); no rompimento o receptor nao tem
 * essa vantagem (1,0).
 */
public enum TipoDisputa {

    /**
     * Aplicador da pegada tenta tirar o receptor da base.
     * Ataque: tecnica e forca. Defesa: tecnica, agilidade (base), forca e peso.
     */
    DESEQUILIBRIO(
            new Pesos(0.40, 0.35, 0.10, 0.15),
            new Pesos(0.35, 0.20, 0.25, 0.20),
            1.10),

    /**
     * Receptor tenta soltar a pegada sem se deslocar.
     * Ataque: tecnica, forca e agilidade. Defesa (manter a pegada): forca e tecnica.
     */
    ROMPIMENTO_TECNICO(
            new Pesos(0.45, 0.30, 0.15, 0.10),
            new Pesos(0.40, 0.40, 0.00, 0.20),
            1.00);

    /** Pesos de tecnica, forca, agilidade e peso corporal (soma 1,0). */
    public static final class Pesos {
        final double tecnica;
        final double forca;
        final double agilidade;
        final double pesoCorporal;

        Pesos(double tecnica, double forca, double agilidade, double pesoCorporal) {
            this.tecnica = tecnica;
            this.forca = forca;
            this.agilidade = agilidade;
            this.pesoCorporal = pesoCorporal;
        }

        public double getTecnica() {
            return tecnica;
        }

        public double getForca() {
            return forca;
        }

        public double getAgilidade() {
            return agilidade;
        }

        public double getPesoCorporal() {
            return pesoCorporal;
        }

        public double soma() {
            return tecnica + forca + agilidade + pesoCorporal;
        }
    }

    private final Pesos ataque;
    private final Pesos defesa;
    private final double fatorPosicaoAtaque;

    TipoDisputa(Pesos ataque, Pesos defesa, double fatorPosicaoAtaque) {
        this.ataque = ataque;
        this.defesa = defesa;
        this.fatorPosicaoAtaque = fatorPosicaoAtaque;
    }

    public Pesos getPesosAtaque() {
        return ataque;
    }

    public Pesos getPesosDefesa() {
        return defesa;
    }

    public double getFatorPosicaoAtaque() {
        return fatorPosicaoAtaque;
    }
}
