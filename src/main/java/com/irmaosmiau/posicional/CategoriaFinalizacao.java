package com.irmaosmiau.posicional;

/** Entrega 5Q: tipo de finalizacao (muda a defesa, o rotulo e a pose provisoria). */
public enum CategoriaFinalizacao {
    /** Chaves de membros superiores (L1). Defesa: proteger / puxar o braco. */
    BRACO("PROTEGER BRAÇO"),
    /** Estrangulamentos (L2). Defesa: maos no pescoco / queixo baixo. */
    ESTRANGULAMENTO("MÃOS NO PESCOÇO"),
    /** Ataques as pernas (L1+L2, so a partir do controle de perna). Defesa: girar / esconder o pe. */
    PERNA("GIRAR O PÉ");

    private final String defesa;

    CategoriaFinalizacao(String defesa) {
        this.defesa = defesa;
    }

    /** Rotulo da defesa no HUD de quem esta sendo atacado. */
    public String getDefesa() {
        return defesa;
    }
}
