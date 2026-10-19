package com.irmaosmiau.combate;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Relogio unico de uma animacao conjunta de combate.
 *
 * Esta classe NAO desenha personagens e NAO conhece o formato das poses.
 * Ela apenas garante que aplicador e receptor avancem pela mesma timeline.
 * Os dados visuais entram nas proximas etapas do motor.
 */
public final class AnimacaoCombateAtiva {

    private final String idAnimacao;
    private final IrmaoMiau aplicador;
    private final IrmaoMiau receptor;
    private final int duracaoFrames;

    private int frameAtual;
    private boolean finalizada;

    public AnimacaoCombateAtiva(
            String idAnimacao,
            IrmaoMiau aplicador,
            IrmaoMiau receptor,
            int duracaoFrames
    ) {

        if (idAnimacao == null || idAnimacao.isBlank()) {
            throw new IllegalArgumentException(
                    "A animacao de combate precisa de um id."
            );
        }

        if (aplicador == null || receptor == null) {
            throw new IllegalArgumentException(
                    "Aplicador e receptor nao podem ser nulos."
            );
        }

        if (aplicador == receptor) {
            throw new IllegalArgumentException(
                    "Aplicador e receptor precisam ser personagens diferentes."
            );
        }

        if (duracaoFrames <= 0) {
            throw new IllegalArgumentException(
                    "A duracao da animacao precisa ser maior que zero."
            );
        }

        this.idAnimacao = idAnimacao.trim().toLowerCase();
        this.aplicador = aplicador;
        this.receptor = receptor;
        this.duracaoFrames = duracaoFrames;
        this.frameAtual = 0;
        this.finalizada = false;
    }

    /**
     * Avanca exatamente um tick da timeline compartilhada.
     */
    public void atualizar() {

        if (finalizada) {
            return;
        }

        frameAtual++;

        if (frameAtual >= duracaoFrames) {
            frameAtual = duracaoFrames;
            finalizada = true;
        }
    }

    public void finalizar() {
        frameAtual = duracaoFrames;
        finalizada = true;
    }

    public String getIdAnimacao() {
        return idAnimacao;
    }

    public IrmaoMiau getAplicador() {
        return aplicador;
    }

    public IrmaoMiau getReceptor() {
        return receptor;
    }

    public int getDuracaoFrames() {
        return duracaoFrames;
    }

    public int getFrameAtual() {
        return frameAtual;
    }

    public boolean isFinalizada() {
        return finalizada;
    }

    /**
     * Progresso normalizado entre 0.0 e 1.0.
     *
     * Este e o MESMO valor que sera usado pelos dois tracks visuais.
     */
    public double getProgresso() {

        if (finalizada) {
            return 1.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        (double) frameAtual / duracaoFrames
                )
        );
    }

    public boolean contemPersonagem(IrmaoMiau personagem) {
        return personagem == aplicador || personagem == receptor;
    }

    public IrmaoMiau getOutroPersonagem(IrmaoMiau personagem) {

        if (personagem == aplicador) {
            return receptor;
        }

        if (personagem == receptor) {
            return aplicador;
        }

        return null;
    }
}
