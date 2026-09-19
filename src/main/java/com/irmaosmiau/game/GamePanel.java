package com.irmaosmiau.game;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.input.InputHandler;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
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

    private static final double ESCALA_PERSONAGEM = 0.7;

    private final InputHandler input;
    private final Timer gameLoop;

    private final Runnable aoVoltarMenu;
    private final Runnable aoTrocarPersonagem;

    private final int velocidade = 4;

    // =============================
    // MODO DE JOGO
    // =============================

    private final boolean modoDoisMiaus;

    // Usado quando apenas um personagem
    // foi selecionado.
    private final IrmaoMiau jogadorPrincipal;

    // Usados no modo "OS DOIS MIAUS".
    private final IrmaoMiau jogadorBranco;
    private final IrmaoMiau jogadorPreto;

    // =============================
    // PULO
    // =============================

    private boolean puloAnterior = false;

    private boolean puloBrancoAnterior = false;
    private boolean puloPretoAnterior = false;

    // =============================
    // ORIENTAÇÃO
    // =============================

    private boolean brancoViradoDireita = true;
    private boolean pretoViradoDireita = false;

    // =============================
    // PAUSA
    // =============================

    private boolean pausado = false;

    private JPanel menuPausa;

    public GamePanel(
            TipoPersonagem tipo,
            Runnable aoVoltarMenu,
            Runnable aoTrocarPersonagem
    ) {

        this.aoVoltarMenu =
                aoVoltarMenu;

        this.aoTrocarPersonagem =
                aoTrocarPersonagem;

        setPreferredSize(
                new Dimension(
                        LARGURA,
                        ALTURA
                )
        );

        setBackground(
                new Color(
                        135,
                        206,
                        235
                )
        );

        setFocusable(true);

        setLayout(null);

        // =========================
        // PERSONAGENS
        // =========================

        modoDoisMiaus =
                tipo == TipoPersonagem.AMBOS;

        if (modoDoisMiaus) {

            jogadorPrincipal = null;

            jogadorBranco =
                    new IrmaoMiauBranco(
                            220,
                            150,
                            ESCALA_PERSONAGEM
                    );

            jogadorPreto =
                    new IrmaoMiauPreto(
                            500,
                            150,
                            ESCALA_PERSONAGEM
                    );

        } else {

            jogadorBranco = null;
            jogadorPreto = null;

            if (tipo == TipoPersonagem.BRANCO) {

                jogadorPrincipal =
                        new IrmaoMiauBranco(
                                350,
                                150,
                                ESCALA_PERSONAGEM
                        );

            } else {

                jogadorPrincipal =
                        new IrmaoMiauPreto(
                                350,
                                150,
                                ESCALA_PERSONAGEM
                        );
            }
        }

        // =========================
        // INPUT
        // =========================

        input =
                new InputHandler(this);

        // =========================
        // PAUSA
        // =========================

        criarMenuPausa();

        configurarPausa();

        // =========================
        // GAME LOOP
        // =========================

        gameLoop =
                new Timer(
                        16,
                        e -> atualizar()
                );

        gameLoop.start();
    }

    // =====================================================
    // ATUALIZAÇÃO
    // =====================================================

    private void atualizar() {

        if (pausado) {

            repaint();
            return;
        }

        if (modoDoisMiaus) {

            atualizarMiauBranco();
            atualizarMiauPreto();

            atualizarOrientacaoDosMiaus();

        } else {

            atualizarJogadorPrincipal();
        }

        repaint();
    }

    // =====================================================
    // JOGADOR INDIVIDUAL
    // =====================================================

    private void atualizarJogadorPrincipal() {

        boolean agachando =
                input.isAgachar();

        moverPersonagem(
                jogadorPrincipal,
                input.isEsquerda(),
                input.isDireita(),
                input.isCima(),
                input.isBaixo(),
                agachando
        );

        if (
                input.isPular()
                && !puloAnterior
        ) {

            jogadorPrincipal.pular();
        }

        //modifiquei aqui:
        /* boolean andando =
                !agachando
                && (
                        input.isEsquerda()
                        || input.isDireita()
                        || input.isCima()
                        || input.isBaixo()
                );*/
        boolean andando =
        input.isEsquerda()
        || input.isDireita()
        || input.isCima()
        || input.isBaixo();

        jogadorPrincipal.atualizar(
                andando,
                agachando
        );

        puloAnterior =
                input.isPular();
    }

    // =====================================================
    // MIAU BRANCO
    // =====================================================

    private void atualizarMiauBranco() {

        boolean agachando =
                input.isAgachar();

        moverPersonagem(
                jogadorBranco,
                input.isEsquerda(),
                input.isDireita(),
                input.isCima(),
                input.isBaixo(),
                agachando
        );

        if (
                input.isPular()
                && !puloBrancoAnterior
        ) {

            jogadorBranco.pular();
        }

        boolean andando =
        input.isEsquerda()
        || input.isDireita()
        || input.isCima()
        || input.isBaixo();

        jogadorBranco.atualizar(
                andando,
                agachando
        );

        puloBrancoAnterior =
                input.isPular();
    }

    // =====================================================
    // MIAU PRETO
    // =====================================================

    private void atualizarMiauPreto() {

        boolean agachando =
                input.isPretoAgachar();

        moverPersonagem(
                jogadorPreto,
                input.isPretoEsquerda(),
                input.isPretoDireita(),
                input.isPretoCima(),
                input.isPretoBaixo(),
                agachando
        );

        if (
                input.isPretoPular()
                && !puloPretoAnterior
        ) {

            jogadorPreto.pular();
        }

        boolean andando =
        input.isPretoEsquerda()
        || input.isPretoDireita()
        || input.isPretoCima()
        || input.isPretoBaixo();

        jogadorPreto.atualizar(
                andando,
                agachando
        );

        puloPretoAnterior =
                input.isPretoPular();
    }

    // =====================================================
    // MOVIMENTO GENÉRICO
    // =====================================================

private void moverPersonagem(
        IrmaoMiau personagem,
        boolean esquerda,
        boolean direita,
        boolean cima,
        boolean baixo,
        boolean agachando
) {

    int velocidadeAtual = velocidade;

    // Agachado anda com 25% da velocidade normal.
    if (agachando) {
        velocidadeAtual =
                Math.max(
                        1,
                        (int) Math.round(
                                velocidade * 0.25
                        )
                );
    }

    if (direita) {
        personagem.moverX(
                velocidadeAtual
        );
    }

    if (esquerda) {
        personagem.moverX(
                -velocidadeAtual
        );
    }

    if (cima) {
        personagem.moverVertical(
                -velocidadeAtual
        );
    }

    if (baixo) {
        personagem.moverVertical(
                velocidadeAtual
        );
    }
}

    // =====================================================
    // OS DOIS SEMPRE OLHAM UM PARA O OUTRO
    // =====================================================

    private void atualizarOrientacaoDosMiaus() {

        if (
                jogadorBranco.getX()
                < jogadorPreto.getX()
        ) {

            brancoViradoDireita = true;
            pretoViradoDireita = false;

        } else if (
                jogadorBranco.getX()
                > jogadorPreto.getX()
        ) {

            brancoViradoDireita = false;
            pretoViradoDireita = true;
        }

        /*
         * Se os dois estiverem exatamente
         * no mesmo X, mantemos a direção
         * anterior.
         *
         * Isso evita os personagens
         * ficarem "tremendo" ou alternando
         * a direção sem necessidade.
         */
    }

    // =====================================================
    // MENU DE PAUSA
    // =====================================================

    private void criarMenuPausa() {

        menuPausa =
                new JPanel(
                        new GridBagLayout()
                );

        menuPausa.setBackground(
                new Color(
                        230,
                        230,
                        230
                )
        );

        menuPausa.setBounds(
                250,
                90,
                300,
                270
        );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.gridx = 0;

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.insets =
                new Insets(
                        8,
                        20,
                        8,
                        20
                );

        javax.swing.JLabel titulo =
                new javax.swing.JLabel(
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

        JButton continuar =
                new JButton(
                        "CONTINUAR"
                );

        JButton trocarMiau =
                new JButton(
                        "TROCAR DE MIAU"
                );

        JButton menuPrincipal =
                new JButton(
                        "MENU PRINCIPAL"
                );

        Font fonteBotao =
                new Font(
                        "Arial",
                        Font.BOLD,
                        16
                );

        continuar.setFont(
                fonteBotao
        );

        trocarMiau.setFont(
                fonteBotao
        );

        menuPrincipal.setFont(
                fonteBotao
        );

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

        pausado =
                !pausado;

        menuPausa.setVisible(
                pausado
        );

        repaint();
    }

    // =====================================================
    // DESENHO
    // =====================================================

    @Override
    protected void paintComponent(
            Graphics g
    ) {

        super.paintComponent(g);

        Graphics2D g2 =
                (Graphics2D) g;

        // =========================
        // CHÃO
        // =========================

        g2.setColor(
                new Color(
                        80,
                        180,
                        80
                )
        );

        g2.fillRect(
                0,
                TOPO_GRAMA,
                LARGURA,
                ALTURA - TOPO_GRAMA
        );

        // =========================
        // MODO DOIS MIAUS
        // =========================

        if (modoDoisMiaus) {

            desenharHudDoisMiaus(g2);

            desenharPersonagemOrientado(
                    g2,
                    jogadorBranco,
                    brancoViradoDireita
            );

            desenharPersonagemOrientado(
                    g2,
                    jogadorPreto,
                    pretoViradoDireita
            );

        } else {

            desenharHudIndividual(g2);

            jogadorPrincipal.desenhar(g2);
        }

        // =========================
        // ESCURECER DURANTE PAUSA
        // =========================

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

    // =====================================================
    // ESPELHAMENTO DO PERSONAGEM
    // =====================================================

    private void desenharPersonagemOrientado(
            Graphics2D g2,
            IrmaoMiau personagem,
            boolean viradoParaDireita
    ) {

        Graphics2D copia =
                (Graphics2D) g2.create();

        if (!viradoParaDireita) {

            /*
             * A cabeça possui 70 unidades
             * de largura.
             *
             * Portanto, 35 representa
             * aproximadamente o centro do
             * personagem.
             */
            double eixoCentral =
                    personagem.getX()
                    + 35
                    * ESCALA_PERSONAGEM;

            copia.translate(
                    eixoCentral,
                    0
            );

            copia.scale(
                    -1,
                    1
            );

            copia.translate(
                    -eixoCentral,
                    0
            );
        }

        personagem.desenhar(
                copia
        );

        copia.dispose();
    }

    // =====================================================
    // HUD INDIVIDUAL
    // =====================================================

    private void desenharHudIndividual(
            Graphics2D g2
    ) {

        g2.setColor(
                Color.BLACK
        );

        g2.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        g2.drawString(
                jogadorPrincipal.getNome(),
                20,
                30
        );

        g2.drawString(
                "Estilo: "
                + jogadorPrincipal.getEstilo(),
                20,
                50
        );

        g2.drawString(
                "Linha: "
                + (
                        jogadorPrincipal
                                .getLinhaAtual()
                        + 1
                )
                + "/5",
                20,
                70
        );

        g2.drawString(
                "W/S = mover | A/D = andar | ESPAÇO = pular | C = agachar | ESC = pausar",
                20,
                95
        );
    }

    // =====================================================
    // HUD - DOIS MIAUS
    // =====================================================

    private void desenharHudDoisMiaus(
            Graphics2D g2
    ) {

        g2.setColor(
                Color.BLACK
        );

        // =========================
        // BRANCO - ESQUERDA
        // =========================

        g2.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );

        g2.drawString(
                "MIAU BRANCO",
                20,
                25
        );

        g2.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        g2.drawString(
                "WASD = mover",
                20,
                45
        );

        g2.drawString(
                "ESPAÇO = pular",
                20,
                62
        );

        g2.drawString(
                "C = agachar",
                20,
                79
        );

        g2.drawString(
                "Linha: "
                + (
                        jogadorBranco
                                .getLinhaAtual()
                        + 1
                )
                + "/5",
                20,
                96
        );

        // =========================
        // PRETO - DIREITA
        // =========================

        g2.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );

        desenharTextoDireita(
                g2,
                "MIAU PRETO",
                25
        );

        g2.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        12
                )
        );

        desenharTextoDireita(
                g2,
                "SETAS = mover",
                45
        );

        desenharTextoDireita(
                g2,
                "N = pular",
                62
        );

        desenharTextoDireita(
                g2,
                "M = agachar",
                79
        );

        desenharTextoDireita(
                g2,
                "Linha: "
                + (
                        jogadorPreto
                                .getLinhaAtual()
                        + 1
                )
                + "/5",
                96
        );

        // =========================
        // PAUSA
        // =========================

        g2.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        11
                )
        );

        String pausa =
                "ESC = pausar";

        FontMetrics fm =
                g2.getFontMetrics();

        int largura =
                fm.stringWidth(
                        pausa
                );

        g2.drawString(
                pausa,
                (
                        LARGURA
                        - largura
                ) / 2,
                25
        );
    }

    private void desenharTextoDireita(
            Graphics2D g2,
            String texto,
            int y
    ) {

        FontMetrics fm =
                g2.getFontMetrics();

        int larguraTexto =
                fm.stringWidth(
                        texto
                );

        g2.drawString(
                texto,
                LARGURA
                - larguraTexto
                - 20,
                y
        );
    }
}