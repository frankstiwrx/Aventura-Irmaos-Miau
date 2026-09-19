package com.irmaosmiau.game;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.input.InputHandler;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

public class GamePanel extends JPanel {

    public static final int LARGURA = 800;
    public static final int ALTURA = 450;

    private static final int TOPO_GRAMA = 300;

    private final IrmaoMiau jogador;
    private final InputHandler input;
    private final Timer gameLoop;

    private final Runnable aoVoltarMenu;
    private final Runnable aoTrocarPersonagem;

    private final int velocidade = 4;

    private boolean puloAnterior = false;
    private boolean pausado = false;

    private JPanel menuPausa;

    public GamePanel(
            TipoPersonagem tipo,
            Runnable aoVoltarMenu,
            Runnable aoTrocarPersonagem
    ) {

        this.aoVoltarMenu = aoVoltarMenu;
        this.aoTrocarPersonagem = aoTrocarPersonagem;

        setPreferredSize(
                new Dimension(LARGURA, ALTURA)
        );

        setBackground(
                new Color(135, 206, 235)
        );

        setFocusable(true);

        // Usaremos posicionamento manual apenas
        // para colocar o menu de pausa sobre o jogo.
        setLayout(null);

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
        // MENU DE PAUSA
        // =========================
        criarMenuPausa();

        // =========================
        // TECLA ESC
        // =========================
        configurarPausa();

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

        // Se estiver pausado, nada do jogo
        // será atualizado.
        if (pausado) {
            repaint();
            return;
        }

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

        // Guarda o estado da tecla ESPAÇO
        // para detectar somente um toque.
        puloAnterior = input.isPular();

        repaint();
    }

    private void criarMenuPausa() {

        menuPausa = new JPanel(
                new GridBagLayout()
        );

        menuPausa.setBackground(
                new Color(230, 230, 230)
        );

        menuPausa.setBounds(
                250,
                90,
                300,
                270
        );

        GridBagConstraints gbc
                = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(
                8,
                20,
                8,
                20
        );

        javax.swing.JLabel titulo
                = new javax.swing.JLabel(
                        "PAUSADO",
                        javax.swing.SwingConstants.CENTER
                );

        titulo.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        28
                )
        );

        gbc.gridy = 0;

        menuPausa.add(
                titulo,
                gbc
        );

        JButton continuar
                = new JButton("CONTINUAR");

        JButton trocarMiau
                = new JButton("TROCAR DE MIAU");

        JButton menuPrincipal
                = new JButton("MENU PRINCIPAL");

        Font fonteBotao = new Font(
                "Arial",
                Font.BOLD,
                16
        );

        continuar.setFont(fonteBotao);
        trocarMiau.setFont(fonteBotao);
        menuPrincipal.setFont(fonteBotao);

        continuar.addActionListener(
                e -> alternarPausa()
        );

        trocarMiau.addActionListener(
                e -> {
                    gameLoop.stop();
                    aoTrocarPersonagem.run();
                }
        );

        menuPrincipal.addActionListener(
                e -> {
                    gameLoop.stop();
                    aoVoltarMenu.run();
                }
        );

        gbc.gridy = 1;
        menuPausa.add(
                continuar,
                gbc
        );

        gbc.gridy = 2;
        menuPausa.add(
                trocarMiau,
                gbc
        );

        gbc.gridy = 3;
        menuPausa.add(
                menuPrincipal,
                gbc
        );

        menuPausa.setVisible(false);

        add(menuPausa);
    }

    private void configurarPausa() {

        getInputMap(
                WHEN_IN_FOCUSED_WINDOW
        ).put(
                javax.swing.KeyStroke.getKeyStroke(
                        "pressed ESCAPE"
                ),
                "alternarPausa"
        );

        getActionMap().put(
                "alternarPausa",
                new javax.swing.AbstractAction() {

                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent e
                    ) {

                        alternarPausa();
                    }
                }
        );
    }

    private void alternarPausa() {

        pausado = !pausado;

        menuPausa.setVisible(
                pausado
        );

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
                "W/S = mover | A/D = andar | ESPAÇO = pular | C = agachar | ESC = pausar",
                20,
                95
        );

        jogador.desenhar(g2);

        // Escurece levemente o jogo quando
        // o menu de pausa estiver aberto.
        if (pausado) {

            g2.setColor(
                    new Color(
                            0,
                            0,
                            0,
                            90
                    )
            );

            g2.fillRect(
                    0,
                    0,
                    LARGURA,
                    ALTURA
            );
        }
    }
}