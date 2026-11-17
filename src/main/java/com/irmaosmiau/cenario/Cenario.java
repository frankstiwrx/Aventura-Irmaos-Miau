package com.irmaosmiau.cenario;

import java.awt.Graphics2D;

/**
 * Contrato de um cenario de arena.
 *
 * Ordem de desenho usada pelo GamePanel em cada frame:
 * <pre>
 * 1. desenharFundo        (ceu, paredes, decoracao distante)
 * 2. desenharChao         (grama, tatame, linhas de profundidade)
 * 3. personagens          (ordem e profundidade inalteradas)
 * 4. desenharPrimeiroPlano (elementos na frente, sem cobrir os lutadores)
 * 5. HUD
 * </pre>
 *
 * Um cenario so DESENHA: nao altera posicoes, linhas, colisoes nem regras.
 * Coordenadas em pixels da tela do jogo (800 x 450).
 */
public interface Cenario {

    /** Identificador unico (ex.: "dojo"). */
    String getId();

    /** Nome legivel (ex.: "Dojo de jiu-jitsu"). */
    String getNome();

    void desenharFundo(Graphics2D g2, int largura, int altura);

    void desenharChao(Graphics2D g2, int largura, int altura);

    /**
     * 5Q.1 (arenas vivas): avanca o ambiente para o tick do jogo (neve, torcida,
     * folhas). Chamado uma vez por tick pelo loop do jogo; nada de timer proprio.
     * Os efeitos sao desenhados em desenharFundo, por cima do cache estatico e
     * ATRAS dos lutadores; o mesmo tick desenha sempre a mesma imagem.
     */
    default void atualizarAmbiente(long tick) {
    }

    /** Opcional: por padrao nada e desenhado na frente dos personagens. */
    default void desenharPrimeiroPlano(Graphics2D g2, int largura, int altura) {
    }
}
