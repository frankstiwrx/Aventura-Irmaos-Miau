package com.irmaosmiau.dispositivos;

/**
 * Botoes do gamepad no layout padrao (Entrega 5D).
 *
 * Os 15 primeiros seguem a ordem do layout padrao de gamepad (SDL/GLFW:
 * A, B, X, Y, LB, RB, BACK, START, GUIDE, L3, R3, D-pad cima/direita/baixo/esquerda).
 * Os nomes sao NEUTROS de marca: SUL e o botao de baixo (Xbox A, PlayStation Cruz).
 * Os gatilhos analogicos entram como "botoes virtuais" (pressionados acima de um limiar).
 */
public enum BotaoGamepad {
    SUL("A/Cruz"),
    LESTE("B/Bola"),
    OESTE("X/Quadrado"),
    NORTE("Y/Triângulo"),
    LB("LB/L1"),
    RB("RB/R1"),
    VOLTAR("Back/Share"),
    INICIAR("Start/Options"),
    GUIA("Guide/PS"),
    L3("L3"),
    R3("R3"),
    DPAD_CIMA("D-pad ↑"),
    DPAD_DIREITA("D-pad →"),
    DPAD_BAIXO("D-pad ↓"),
    DPAD_ESQUERDA("D-pad ←"),
    /** Virtual: gatilho esquerdo acima do limiar. */
    GATILHO_ESQ("LT/L2"),
    /** Virtual: gatilho direito acima do limiar. */
    GATILHO_DIR("RT/R2");

    /** Quantidade de botoes digitais reais (sem os gatilhos virtuais). */
    public static final int FISICOS = 15;

    private final String rotulo;

    BotaoGamepad(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Texto para o jogador: nome Xbox / PlayStation. */
    public String getRotulo() {
        return rotulo;
    }

    public boolean isVirtual() {
        return ordinal() >= FISICOS;
    }
}
