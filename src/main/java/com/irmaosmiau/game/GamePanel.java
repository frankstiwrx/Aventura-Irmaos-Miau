package com.irmaosmiau.game;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.input.InputHandler;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.Timer;

public class GamePanel extends JPanel {

    public static final int LARGURA = 800;
    public static final int ALTURA = 450;
    private static final int TOPO_GRAMA = 300;

    private final IrmaoMiau jogador;

    private final InputHandler input;

    private final Timer gameLoop;

    private final int velocidade = 4;

    // Serve para detectar um único toque
    // em W, S e ESPAÇO.
    //private boolean cimaAnterior = false;
    //private boolean baixoAnterior = false;
    private boolean puloAnterior = false;

    public GamePanel(TipoPersonagem tipo) {

        setPreferredSize(
                new Dimension(LARGURA, ALTURA)
        );

        setBackground(
                new Color(135, 206, 235)
        );

        setFocusable(true);

        // =========================
        // PERSONAGEM
        // =========================
        if (tipo == TipoPersonagem.BRANCO) {

            jogador = new IrmaoMiauBranco(
                    350,
                    150,
                    0.7
            );

        } else {

            jogador = new IrmaoMiauPreto(
                    350,
                    150,
                    0.7
            );
        }

        // =========================
        // INPUT
        // =========================
        input = new InputHandler(this);

        // =========================
        // GAME LOOP
        // =========================
        gameLoop = new Timer(
                16,
                e -> atualizar()
        );

        gameLoop.start();
    }

    private void atualizar() {

        boolean agachando
                = input.isAgachar();

        // =========================
        // ESQUERDA / DIREITA
        // =========================
        if (!agachando) {

            if (input.isDireita()) {

                jogador.moverX(
                        velocidade
                );
            }

            if (input.isEsquerda()) {

                jogador.moverX(
                        -velocidade
                );
            }
        }

        // =========================
        // TROCA DE LINHA
        // =========================
        // =========================
// MOVIMENTO VERTICAL
// =========================
        if (!agachando) {

            if (input.isCima()) {

                jogador.moverVertical(
                        -velocidade
                );
            }

            if (input.isBaixo()) {

                jogador.moverVertical(
                        velocidade
                );
            }
        }

        // =========================
        // PULO
        // =========================
        if (input.isPular()
                && !puloAnterior) {

            jogador.pular();
        }

        // =========================
        // ANIMAÇÃO / FÍSICA
        // =========================
        boolean andando
                = !agachando
                && (input.isEsquerda()
                || input.isDireita()
                || input.isCima()
                || input.isBaixo());

        jogador.atualizar(
                andando,
                agachando
        );

        // Guardamos o estado anterior
        
    //    cimaAnterior
    //            = input.isCima();

    //    baixoAnterior
    //            = input.isBaixo();

    //    puloAnterior
    //            = input.isPular();

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2
                = (Graphics2D) g;

        // =========================
        // CHÃO
        // =========================
        g2.setColor(
                new Color(80, 180, 80)
        );

        g2.fillRect(
                0,
                TOPO_GRAMA,
                LARGURA,
                ALTURA - TOPO_GRAMA
        );

        // =========================
        // HUD DE TESTE
        // =========================
        g2.setColor(Color.BLACK);

        g2.drawString(
                jogador.getNome(),
                20,
                30
        );

        g2.drawString(
                "Estilo: "
                + jogador.getEstilo(),
                20,
                50
        );

        g2.drawString(
                "Linha: "
                + (jogador.getLinhaAtual() + 1)
                + "/5",
                20,
                70
        );

        g2.drawString(
                "W/S = linhas | A/D = andar | ESPAÇO = pular | C = agachar",
                20,
                95
        );

        jogador.desenhar(g2);
    }
}
