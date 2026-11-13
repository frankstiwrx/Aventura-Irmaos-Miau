package com.irmaosmiau.personagens;

import java.awt.Color;

/**
 * Referencias visuais de um personagem para menus, HUD e futuras telas.
 *
 * Sao DADOS descritivos: o desenho em jogo continua sendo feito pelo
 * renderer do proprio personagem. Nada aqui altera a aparencia atual.
 */
public final class ReferenciasVisuais {

    private final Color corPelo;
    private final Color corKimono;
    private final Color corRosto;
    private final String caminhoRetrato;

    /**
     * @param caminhoRetrato recurso de imagem no classpath (pode ser null)
     */
    public ReferenciasVisuais(Color corPelo, Color corKimono, Color corRosto, String caminhoRetrato) {
        if (corPelo == null || corKimono == null || corRosto == null) {
            throw new IllegalArgumentException("As cores de pelo, kimono e rosto sao obrigatorias.");
        }
        this.corPelo = corPelo;
        this.corKimono = corKimono;
        this.corRosto = corRosto;
        this.caminhoRetrato = caminhoRetrato == null || caminhoRetrato.isBlank()
                ? null
                : caminhoRetrato.trim();
    }

    public Color getCorPelo() {
        return corPelo;
    }

    public Color getCorKimono() {
        return corKimono;
    }

    public Color getCorRosto() {
        return corRosto;
    }

    /** Pode ser null quando o personagem ainda nao tem retrato. */
    public String getCaminhoRetrato() {
        return caminhoRetrato;
    }
}
