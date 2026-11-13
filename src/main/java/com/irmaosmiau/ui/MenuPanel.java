package com.irmaosmiau.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class MenuPanel extends JPanel {

    private Image imagemFundo;

    public MenuPanel(
            Runnable aoComecar
    ) {
        this(aoComecar, null);
    }

    /**
     * Entrega 4A: menu com a opcao TREINO (abaixo de COMECAR).
     * Com aoTreinar nulo o menu fica igual ao original.
     */
    public MenuPanel(
            Runnable aoComecar,
            Runnable aoTreinar
    ) {
        this(aoComecar, aoTreinar, null, null);
    }

    /**
     * Entrega 4C: menu completo com CONFIGURACOES e PERSONAGENS
     * PERSONALIZADOS. Com quatro opcoes os botoes ficam mais largos e um
     * pouco mais baixos para caber em 800 x 450.
     */
    public MenuPanel(
            Runnable aoComecar,
            Runnable aoTreinar,
            Runnable aoConfiguracoes,
            Runnable aoPerfis
    ) {

        setPreferredSize(
                new Dimension(
                        800,
                        450
                )
        );

        setLayout(
                new GridBagLayout()
        );

        // =========================
        // IMAGEM DE FUNDO
        // =========================

        URL recurso =
                getClass().getResource(
                        "/assets/backgrounds/menuprincipal.png"
                );

        if (recurso != null) {

            imagemFundo =
                    new ImageIcon(
                            recurso
                    ).getImage();
        }

        // =========================
        // PAINEL CENTRAL
        // =========================

        JPanel centro =
                new JPanel(
                        new GridBagLayout()
                );

        /*
         * Muito importante:
         * deixa a imagem aparecer por trás.
         */
        centro.setOpaque(false);

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.gridx = 0;
        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.anchor =
                GridBagConstraints.CENTER;

        // =========================
        // TÍTULO
        // =========================

        JLabel titulo =
                new JLabel(
                        "AVENTURA DOS IRMÃOS MIAU",
                        SwingConstants.CENTER
                );

        titulo.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        32
                )
        );

        titulo.setForeground(
                new Color(
                        35,
                        35,
                        35
                )
        );

        gbc.gridy = 0;

        gbc.insets =
                new Insets(
                        0,
                        0,
                        25,
                        0
                );

        centro.add(
                titulo,
                gbc
        );

        // =========================
        // BOTÃO COMEÇAR
        // =========================

        JButton botaoComecar =
                new JButton(
                        "COMEÇAR"
                );

        botaoComecar.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        20
                )
        );

        botaoComecar.setPreferredSize(
                new Dimension(
                        180,
                        50
                )
        );

        botaoComecar.addActionListener(
                e -> aoComecar.run()
        );

        gbc.gridy = 1;

        gbc.insets =
                new Insets(
                        0,
                        70,
                        0,
                        70
                );

        boolean menuCompleto = aoConfiguracoes != null || aoPerfis != null;

        if (menuCompleto) {
            botaoComecar.setFont(new Font("Arial", Font.BOLD, 17));
            botaoComecar.setPreferredSize(new Dimension(340, 42));
            gbc.insets = new Insets(0, 40, 0, 40);
        }

        centro.add(
                botaoComecar,
                gbc
        );

        // =========================
        // BOTÃO TREINO (Entrega 4A)
        // =========================
        if (aoTreinar != null) {

            JButton botaoTreino =
                    new JButton(
                            "TREINO"
                    );

            botaoTreino.setFont(
                    botaoComecar.getFont()
            );

            botaoTreino.setPreferredSize(
                    botaoComecar.getPreferredSize()
            );

            botaoTreino.addActionListener(
                    e -> aoTreinar.run()
            );

            gbc.gridy = 2;

            gbc.insets =
                    new Insets(
                            menuCompleto ? 10 : 12,
                            menuCompleto ? 40 : 70,
                            0,
                            menuCompleto ? 40 : 70
                    );

            centro.add(
                    botaoTreino,
                    gbc
            );
        }

        // =========================
        // CONFIGURAÇÕES E PERFIS (Entrega 4C)
        // =========================
        adicionarOpcao(centro, gbc, 3, "CONFIGURAÇÕES", aoConfiguracoes, botaoComecar);
        adicionarOpcao(centro, gbc, 4, "PERSONAGENS PERSONALIZADOS", aoPerfis, botaoComecar);

        add(
                centro
        );
    }

    private static void adicionarOpcao(
            JPanel centro,
            GridBagConstraints gbc,
            int linha,
            String texto,
            Runnable acao,
            JButton modelo
    ) {

        if (acao == null) {
            return;
        }

        JButton botao = new JButton(texto);
        botao.setFont(modelo.getFont());
        botao.setPreferredSize(modelo.getPreferredSize());
        botao.addActionListener(e -> acao.run());

        gbc.gridy = linha;
        gbc.insets = new Insets(10, 40, 0, 40);
        centro.add(botao, gbc);
    }

    // =============================
    // DESENHA O FUNDO
    // =============================

    @Override
    protected void paintComponent(
            Graphics g
    ) {

        super.paintComponent(g);

        if (imagemFundo != null) {

            g.drawImage(
                    imagemFundo,
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    this
            );

        } else {

            /*
             * Caso a imagem não seja encontrada,
             * mantém nosso azul antigo.
             */
            g.setColor(
                    new Color(
                            135,
                            206,
                            235
                    )
            );

            g.fillRect(
                    0,
                    0,
                    getWidth(),
                    getHeight()
            );
        }
    }
}