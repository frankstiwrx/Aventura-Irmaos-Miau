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
    private final Runnable aoTentarNovamente;

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

    private void verificarFimDeJogo() {

        if (modoDoisMiaus) {

            boolean brancoFim
                    = jogadorBranco.isSemCondicionamento();

            boolean pretoFim
                    = jogadorPreto.isSemCondicionamento();

            if (brancoFim && pretoFim) {

                fimDeJogo = true;
                mensagemFim
                        = "OS DOIS MIAUS NÃO CONSEGUEM MAIS LUTAR";

            } else if (brancoFim) {

                fimDeJogo = true;
                mensagemFim
                        = "MIAU BRANCO NÃO CONSEGUE MAIS LUTAR";

            } else if (pretoFim) {

                fimDeJogo = true;
                mensagemFim
                        = "MIAU PRETO NÃO CONSEGUE MAIS LUTAR";
            }

        } else if (jogadorPrincipal
                .isSemCondicionamento()) {

            fimDeJogo = true;

            mensagemFim
                    = jogadorPrincipal.getNome()
                    + " NÃO CONSEGUE MAIS LUTAR";
        }

        if (fimDeJogo) {

    atualizarMenuFim();

    menuFim.setVisible(true);

    setComponentZOrder(
            menuFim,
            0
    );

    repaint();
}
    }
    
    // Atualizaer menu fim
    
    private void atualizarMenuFim() {

    for (
            java.awt.Component componente
            : menuFim.getComponents()
    ) {

        if (
                componente
                instanceof javax.swing.JLabel
        ) {

            javax.swing.JLabel label =
                    (javax.swing.JLabel) componente;

            if (
                    "motivoFim".equals(
                            label.getName()
                    )
            ) {

                label.setText(
                        mensagemFim
                );

                break;
            }
        }
    }
}
    
    //Menu de fim
    
    private void criarMenuFim() {

    menuFim = new JPanel(
            new GridBagLayout()
    );

    menuFim.setBackground(
            new Color(
                    230,
                    230,
                    230
            )
    );

    menuFim.setBounds(
            220,
            90,
            360,
            285
    );

    GridBagConstraints gbc =
            new GridBagConstraints();

    gbc.gridx = 0;
    gbc.fill =
            GridBagConstraints.HORIZONTAL;

    gbc.insets =
            new Insets(
                    8,
                    25,
                    8,
                    25
            );

    // =========================
    // TÍTULO
    // =========================

    javax.swing.JLabel titulo =
            new javax.swing.JLabel(
                    "FIM",
                    javax.swing.SwingConstants.CENTER
            );

    titulo.setFont(
            new Font(
                    "Arial",
                    Font.BOLD,
                    32
            )
    );

    gbc.gridy = 0;

    menuFim.add(
            titulo,
            gbc
    );

    // =========================
    // MOTIVO
    // =========================

    javax.swing.JLabel motivo =
            new javax.swing.JLabel(
                    "",
                    javax.swing.SwingConstants.CENTER
            );

    motivo.setFont(
            new Font(
                    "Arial",
                    Font.BOLD,
                    12
            )
    );

    /*
     * Guardamos a referência dentro
     * do próprio painel para atualizar
     * depois quando alguém perder.
     */
    motivo.setName("motivoFim");

    gbc.gridy = 1;

    menuFim.add(
            motivo,
            gbc
    );

    // =========================
    // BOTÕES
    // =========================

    JButton tentarNovamente =
            new JButton(
                    "TENTAR NOVAMENTE"
            );

    JButton selecionarPersonagens =
            new JButton(
                    "SELECIONAR PERSONAGENS"
            );

    JButton menuPrincipal =
            new JButton(
                    "MENU PRINCIPAL"
            );

    Font fonteBotao =
            new Font(
                    "Arial",
                    Font.BOLD,
                    15
            );

    tentarNovamente.setFont(
            fonteBotao
    );

    selecionarPersonagens.setFont(
            fonteBotao
    );

    menuPrincipal.setFont(
            fonteBotao
    );

    tentarNovamente.addActionListener(
            e -> {

                gameLoop.stop();

                aoTentarNovamente.run();
            }
    );

    selecionarPersonagens.addActionListener(
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

    gbc.gridy = 2;

    menuFim.add(
            tentarNovamente,
            gbc
    );

    gbc.gridy = 3;

    menuFim.add(
            selecionarPersonagens,
            gbc
    );

    gbc.gridy = 4;

    menuFim.add(
            menuPrincipal,
            gbc
    );

    menuFim.setVisible(false);

    add(menuFim);
}
    

    // =============================
    // PULOS
    // =============================
    private boolean puloAnterior = false;

    private boolean puloBrancoAnterior = false;
    private boolean puloPretoAnterior = false;

    private boolean puloCurtoAnterior = false;

    private boolean puloCurtoBrancoAnterior = false;
    private boolean puloCurtoPretoAnterior = false;

    private boolean fimDeJogo = false;

    private String mensagemFim = "";

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
    private JPanel menuFim;

    public GamePanel(
            TipoPersonagem tipo,
            Runnable aoVoltarMenu,
            Runnable aoTrocarPersonagem,
            Runnable aoTentarNovamente
    ) {

        this.aoVoltarMenu = aoVoltarMenu;
        this.aoTrocarPersonagem = aoTrocarPersonagem;
        this.aoTentarNovamente = aoTentarNovamente;

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
        modoDoisMiaus
                = tipo == TipoPersonagem.AMBOS;

        if (modoDoisMiaus) {

            jogadorPrincipal = null;

            jogadorBranco
                    = new IrmaoMiauBranco(
                            220,
                            150,
                            ESCALA_PERSONAGEM
                    );

            jogadorPreto
                    = new IrmaoMiauPreto(
                            500,
                            150,
                            ESCALA_PERSONAGEM
                    );

        } else {

            jogadorBranco = null;
            jogadorPreto = null;

            if (tipo == TipoPersonagem.BRANCO) {

                jogadorPrincipal
                        = new IrmaoMiauBranco(
                                350,
                                150,
                                ESCALA_PERSONAGEM
                        );

            } else {

                jogadorPrincipal
                        = new IrmaoMiauPreto(
                                350,
                                150,
                                ESCALA_PERSONAGEM
                        );
            }
        }

        // =========================
        // INPUT
        // =========================
        input
                = new InputHandler(this);

        // =========================
        // PAUSA
        // =========================
        criarMenuPausa();
        criarMenuFim();
        configurarPausa();

        // =========================
        // GAME LOOP
        // =========================
        gameLoop
                = new Timer(
                        16,
                        e -> atualizar()
                );

        gameLoop.start();
    }

    // =====================================================
    // ATUALIZAÇÃO
    // =====================================================
    private void atualizar() {

        if (fimDeJogo) {
            return;
        }

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

        verificarFimDeJogo();
        repaint();
    }

    // =====================================================
    // JOGADOR INDIVIDUAL
    // =====================================================
    private void atualizarJogadorPrincipal() {

        boolean agachando
                = input.isAgachar();

        boolean correndo
                = input.isCorrer()
                && !agachando;

        jogadorPrincipal.setCorrendo(
                correndo
        );

        moverPersonagem(
                jogadorPrincipal,
                input.isEsquerda(),
                input.isDireita(),
                input.isCima(),
                input.isBaixo(),
                agachando,
                correndo
        );

        // =========================
        // PULO ALTO
        // =========================
        if (input.isPular()
                && !puloAnterior) {

            jogadorPrincipal.pular();
        }

        // =========================
        // PULO CURTO
        // =========================
        if (input.isPularCurto()
                && !puloCurtoAnterior) {

            jogadorPrincipal.pularCurto();
        }

        boolean andando
                = input.isEsquerda()
                || input.isDireita()
                || input.isCima()
                || input.isBaixo();

        jogadorPrincipal.atualizar(
                andando,
                agachando
        );

        puloAnterior
                = input.isPular();

        puloCurtoAnterior
                = input.isPularCurto();
    }

    // =====================================================
    // MIAU BRANCO
    // =====================================================
    private void atualizarMiauBranco() {

        boolean agachando
                = input.isAgachar();

        boolean correndo
                = input.isCorrer()
                && !agachando;

        jogadorBranco.setCorrendo(
                correndo
        );

        moverPersonagem(
                jogadorBranco,
                input.isEsquerda(),
                input.isDireita(),
                input.isCima(),
                input.isBaixo(),
                agachando,
                correndo
        );

        // Pulo alto
        if (input.isPular()
                && !puloBrancoAnterior) {

            jogadorBranco.pular();
        }

        // Pulo curto
        if (input.isPularCurto()
                && !puloCurtoBrancoAnterior) {

            jogadorBranco.pularCurto();
        }

        boolean andando
                = input.isEsquerda()
                || input.isDireita()
                || input.isCima()
                || input.isBaixo();

        jogadorBranco.atualizar(
                andando,
                agachando
        );

        puloBrancoAnterior
                = input.isPular();

        puloCurtoBrancoAnterior
                = input.isPularCurto();
    }

    // =====================================================
    // MIAU PRETO
    // =====================================================
    private void atualizarMiauPreto() {

        boolean agachando
                = input.isPretoAgachar();

        boolean correndo
                = input.isPretoCorrer()
                && !agachando;

        jogadorPreto.setCorrendo(
                correndo
        );

        moverPersonagem(
                jogadorPreto,
                input.isPretoEsquerda(),
                input.isPretoDireita(),
                input.isPretoCima(),
                input.isPretoBaixo(),
                agachando,
                correndo
        );

        // Pulo alto
        if (input.isPretoPular()
                && !puloPretoAnterior) {

            jogadorPreto.pular();
        }

        // Pulo curto
        if (input.isPretoPularCurto()
                && !puloCurtoPretoAnterior) {

            jogadorPreto.pularCurto();
        }

        boolean andando
                = input.isPretoEsquerda()
                || input.isPretoDireita()
                || input.isPretoCima()
                || input.isPretoBaixo();

        jogadorPreto.atualizar(
                andando,
                agachando
        );

        puloPretoAnterior
                = input.isPretoPular();

        puloCurtoPretoAnterior
                = input.isPretoPularCurto();
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
            boolean agachando,
            boolean correndo
    ) {

        double velocidadeAtual;

        // =========================
        // DEFINE A VELOCIDADE
        // =========================
        if (agachando) {

            velocidadeAtual
                    = personagem
                            .getVelocidadeAgachado();

        } else if (correndo) {

            velocidadeAtual
                    = personagem
                            .getVelocidadeCorrida();

        } else {

            velocidadeAtual
                    = personagem
                            .getVelocidadeBase();
        }

        /*
     * Por enquanto nosso movimento ainda usa int.
     *
     * Quando fizermos o estado DEITADO com 7%,
     * transformaremos isso em movimento decimal.
         */
        int deslocamento
                = Math.max(
                        1,
                        (int) Math.round(
                                velocidadeAtual
                        )
                );

        // =========================
        // MOVIMENTO
        // =========================
        if (direita) {

            personagem.moverX(
                    deslocamento
            );
        }

        if (esquerda) {

            personagem.moverX(
                    -deslocamento
            );
        }

        if (cima) {

            personagem.moverVertical(
                    -deslocamento
            );
        }

        if (baixo) {

            personagem.moverVertical(
                    deslocamento
            );
        }
    }

    // =====================================================
    // OS DOIS SEMPRE OLHAM UM PARA O OUTRO
    // =====================================================
    private void atualizarOrientacaoDosMiaus() {

        if (jogadorBranco.getX()
                < jogadorPreto.getX()) {

            brancoViradoDireita = true;
            pretoViradoDireita = false;

        } else if (jogadorBranco.getX()
                > jogadorPreto.getX()) {

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

        menuPausa
                = new JPanel(
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

        GridBagConstraints gbc
                = new GridBagConstraints();

        gbc.gridx = 0;

        gbc.fill
                = GridBagConstraints.HORIZONTAL;

        gbc.insets
                = new Insets(
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
                = new JButton(
                        "CONTINUAR"
                );

        JButton trocarMiau
                = new JButton(
                        "TROCAR DE MIAU"
                );

        JButton menuPrincipal
                = new JButton(
                        "MENU PRINCIPAL"
                );

        Font fonteBotao
                = new Font(
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

        pausado
                = !pausado;

        menuPausa.setVisible(
                pausado
        );

        repaint();
    }

    private void desenharFimDeJogo(
        Graphics2D g2
) {

    g2.setColor(
            new Color(
                    0,
                    0,
                    0,
                    120
            )
    );

    g2.fillRect(
            0,
            0,
            LARGURA,
            ALTURA
    );
}
    /*
    private void desenharFimDeJogo(
            Graphics2D g2
    ) {

        // Fundo escurecido
        g2.setColor(
                new Color(
                        0,
                        0,
                        0,
                        150
                )
        );

        g2.fillRect(
                0,
                0,
                LARGURA,
                ALTURA
        );

        // FIM
        g2.setColor(Color.WHITE);

        g2.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        52
                )
        );

        String fim = "FIM";

        FontMetrics fm
                = g2.getFontMetrics();

        int fimX
                = (LARGURA
                - fm.stringWidth(fim)) / 2;

        g2.drawString(
                fim,
                fimX,
                190
        );

        // Motivo
        g2.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        16
                )
        );

        fm = g2.getFontMetrics();

        int mensagemX
                = (LARGURA
                - fm.stringWidth(mensagemFim)) / 2;

        g2.drawString(
                mensagemFim,
                mensagemX,
                225
        );
    }
*/
    // =====================================================
    // DESENHO
    // =====================================================
    @Override
    protected void paintComponent(
            Graphics g
    ) {

        super.paintComponent(g);

        Graphics2D g2
                = (Graphics2D) g;

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

        if (fimDeJogo) {

            desenharFimDeJogo(g2);
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

        Graphics2D copia
                = (Graphics2D) g2.create();

        if (!viradoParaDireita) {

            /*
             * A cabeça possui 70 unidades
             * de largura.
             *
             * Portanto, 35 representa
             * aproximadamente o centro do
             * personagem.
             */
            double eixoCentral
                    = personagem.getX()
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
                + (jogadorPrincipal
                        .getLinhaAtual()
                + 1)
                + "/5",
                20,
                70
        );

        g2.drawString(
                "WASD mover | Z correr | SPACE pulo | V pulo curto | C agachar | ESC pausa",
                20,
                95
        );

        g2.drawString(
                "CONDICIONAMENTO",
                20,
                120
        );

        desenharBarra(
                g2,
                20,
                125,
                180,
                12,
                jogadorPrincipal.getPercentualCondicionamento(),
                new Color(60, 180, 80)
        );

        g2.drawString(
                "FÔLEGO",
                20,
                153
        );

        desenharBarra(
                g2,
                20,
                158,
                180,
                12,
                jogadorPrincipal.getPercentualFolego(),
                new Color(60, 140, 220)
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
                + (jogadorBranco
                        .getLinhaAtual()
                + 1)
                + "/5",
                20,
                96
        );

        g2.drawString(
                "CONDICIONAMENTO",
                20,
                120
        );

        desenharBarra(
                g2,
                20,
                125,
                160,
                12,
                jogadorBranco.getPercentualCondicionamento(),
                new Color(60, 180, 80)
        );

        g2.drawString(
                "FÔLEGO",
                20,
                153
        );

        desenharBarra(
                g2,
                20,
                158,
                160,
                12,
                jogadorBranco.getPercentualFolego(),
                new Color(60, 140, 220)
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
                + (jogadorPreto
                        .getLinhaAtual()
                + 1)
                + "/5",
                96
        );

        desenharTextoDireita(
                g2,
                "CONDICIONAMENTO",
                120
        );

        desenharBarra(
                g2,
                620,
                125,
                160,
                12,
                jogadorPreto.getPercentualCondicionamento(),
                new Color(60, 180, 80)
        );

        desenharTextoDireita(
                g2,
                "FÔLEGO",
                153
        );

        desenharBarra(
                g2,
                620,
                158,
                160,
                12,
                jogadorPreto.getPercentualFolego(),
                new Color(60, 140, 220)
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

        String pausa
                = "ESC = pausar";

        FontMetrics fm
                = g2.getFontMetrics();

        int largura
                = fm.stringWidth(
                        pausa
                );

        g2.drawString(
                pausa,
                (LARGURA
                - largura) / 2,
                25
        );
    }

    private void desenharTextoDireita(
            Graphics2D g2,
            String texto,
            int y
    ) {

        FontMetrics fm
                = g2.getFontMetrics();

        int larguraTexto
                = fm.stringWidth(
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

    private void desenharBarra(
            Graphics2D g2,
            int x,
            int y,
            int largura,
            int altura,
            double percentual,
            Color cor
    ) {

        percentual
                = Math.max(
                        0,
                        Math.min(
                                1,
                                percentual
                        )
                );

        // Fundo da barra
        g2.setColor(
                new Color(
                        45,
                        45,
                        45
                )
        );

        g2.fillRect(
                x,
                y,
                largura,
                altura
        );

        // Conteúdo
        g2.setColor(cor);

        g2.fillRect(
                x + 2,
                y + 2,
                (int) ((largura - 4)
                * percentual),
                altura - 4
        );
    }
}
