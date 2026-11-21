package com.irmaosmiau.controles;

/**
 * Qual dispositivo comanda cada personagem (TREINO).
 *
 * Normal: cada slot le os proprios dispositivos (J1 o do J1, J2 o do J2). Com o principal no J2 (F11 no
 * treino), o J2 le os dispositivos do J1 (o "dispositivo principal": teclado do J1 e o gamepad do J1) e o
 * J1 nao recebe nada; o dispositivo proprio do J2, se houver, fica em espera (nao disputa o personagem).
 *
 * O InputHandler e a SessaoDispositivos perguntam aqui de onde vem a entrada de um slot; o resto do jogo
 * continua lendo pelo slot, sem nenhum segundo sistema de controle. Fora do treino nada alterna.
 */
public final class RotaControles {

    private boolean principalNoJ2;

    /** O dispositivo principal comanda o J2 agora? */
    public boolean isPrincipalNoJ2() {
        return principalNoJ2;
    }

    public void definirPrincipalNoJ2(boolean v) {
        principalNoJ2 = v;
    }

    /** Alterna o personagem do dispositivo principal (J1 <-> J2). */
    public void alternar() {
        principalNoJ2 = !principalNoJ2;
    }

    /** O personagem que o dispositivo principal comanda. */
    public SlotJogador controladoPeloPrincipal() {
        return principalNoJ2 ? SlotJogador.JOGADOR_2 : SlotJogador.JOGADOR_1;
    }

    /**
     * De que slot vem a entrada deste personagem (null = de nenhum: o J1 enquanto o principal comanda o J2).
     */
    public SlotJogador origem(SlotJogador alvo) {
        if (!principalNoJ2) {
            return alvo;
        }
        return alvo == SlotJogador.JOGADOR_2 ? SlotJogador.JOGADOR_1 : null;
    }
}
