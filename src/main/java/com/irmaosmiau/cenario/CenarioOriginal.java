package com.irmaosmiau.cenario;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Cenario original do prototipo: ceu azul e grama verde.
 *
 * Reproduz exatamente o desenho que existia no GamePanel antes da
 * Entrega 4A (fundo 135,206,235 e grama 80,180,80 a partir de y = 300).
 * Mantido como opcao de compatibilidade.
 */
public final class CenarioOriginal implements Cenario {

    public static final String ID = "original";

    /** Mesmos valores do GamePanel original. */
    public static final Color COR_CEU = new Color(135, 206, 235);
    public static final Color COR_GRAMA = new Color(80, 180, 80);
    public static final int TOPO_GRAMA = 300;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getNome() {
        return "Campo original";
    }

    @Override
    public void desenharFundo(Graphics2D g2, int largura, int altura) {
        g2.setColor(COR_CEU);
        g2.fillRect(0, 0, largura, altura);
    }

    @Override
    public void desenharChao(Graphics2D g2, int largura, int altura) {
        g2.setColor(COR_GRAMA);
        g2.fillRect(0, TOPO_GRAMA, largura, altura - TOPO_GRAMA);
    }
}
