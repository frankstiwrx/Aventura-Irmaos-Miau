package com.irmaosmiau.ui;

import com.irmaosmiau.entities.TipoPersonagem;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Image;
import java.net.URL;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class SelecaoPersonagemPanel extends JPanel {

    public SelecaoPersonagemPanel(
            Consumer<TipoPersonagem> aoSelecionar
    ) {

        setPreferredSize(
                new Dimension(800, 450)
        );

        setBackground(
                new Color(135, 206, 235)
        );

        setLayout(
                new GridBagLayout()
        );

        JPanel painel =
                new JPanel(
                        new GridBagLayout()
                );

        painel.setOpaque(false);

        GridBagConstraints gbc =
                new GridBagConstraints();

        // =========================
        // TÍTULO
        // =========================

        JLabel titulo = new JLabel(
                "ESCOLHA SEU MIAU",
                SwingConstants.CENTER
        );

        titulo.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        28
                )
        );

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        gbc.insets = new Insets(
                0,
                0,
                25,
                0
        );

        painel.add(
                titulo,
                gbc
        );

        // =========================
        // IMAGENS
        // =========================

        JLabel imagemBranco =
                criarImagem(
                        "/assets/sprites/miau-branco.png"
                );

        JLabel imagemPreto =
                criarImagem(
                        "/assets/sprites/miau-preto.png"
                );

        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 1;

        gbc.insets = new Insets(
                0,
                20,
                15,
                20
        );

        painel.add(
                imagemBranco,
                gbc
        );

        gbc.gridx = 1;

        painel.add(
                imagemPreto,
                gbc
        );

        // =========================
        // BOTÕES INDIVIDUAIS
        // =========================

        JButton branco =
                new JButton(
                        "MIAU BRANCO"
                );

        JButton preto =
                new JButton(
                        "MIAU PRETO"
                );

        Font fonteBotao =
                new Font(
                        "Arial",
                        Font.BOLD,
                        16
                );

        branco.setFont(fonteBotao);
        preto.setFont(fonteBotao);

        branco.setPreferredSize(
                new Dimension(
                        180,
                        45
                )
        );

        preto.setPreferredSize(
                new Dimension(
                        180,
                        45
                )
        );

        branco.addActionListener(
                e -> aoSelecionar.accept(
                        TipoPersonagem.BRANCO
                )
        );

        preto.addActionListener(
                e -> aoSelecionar.accept(
                        TipoPersonagem.PRETO
                )
        );

        gbc.gridx = 0;
        gbc.gridy = 2;

        gbc.insets = new Insets(
                0,
                20,
                12,
                20
        );

        painel.add(
                branco,
                gbc
        );

        gbc.gridx = 1;

        painel.add(
                preto,
                gbc
        );

        // =========================
        // OS DOIS MIAUS
        // =========================

        JButton ambos =
                new JButton(
                        "OS DOIS MIAUS"
                );

        ambos.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        16
                )
        );

        ambos.setPreferredSize(
                new Dimension(
                        250,
                        45
                )
        );

        ambos.addActionListener(
                e -> aoSelecionar.accept(
                        TipoPersonagem.AMBOS
                )
        );

        gbc.gridx = 0;
        gbc.gridy = 3;

        gbc.gridwidth = 2;

        gbc.insets = new Insets(
                5,
                20,
                0,
                20
        );

        painel.add(
                ambos,
                gbc
        );

        add(painel);
    }

    private JLabel criarImagem(
            String caminho
    ) {

        URL recurso =
                getClass().getResource(
                        caminho
                );

        if (recurso == null) {

            JLabel placeholder =
                    new JLabel(
                            "IMAGEM",
                            SwingConstants.CENTER
                    );

            placeholder.setPreferredSize(
                    new Dimension(
                            180,
                            180
                    )
            );

            return placeholder;
        }

        ImageIcon original =
                new ImageIcon(
                        recurso
                );

        Image imagemRedimensionada =
                original
                        .getImage()
                        .getScaledInstance(
                                180,
                                180,
                                Image.SCALE_SMOOTH
                        );

        JLabel imagem =
                new JLabel(
                        new ImageIcon(
                                imagemRedimensionada
                        )
                );

        imagem.setPreferredSize(
                new Dimension(
                        180,
                        180
                )
        );

        return imagem;
    }
}