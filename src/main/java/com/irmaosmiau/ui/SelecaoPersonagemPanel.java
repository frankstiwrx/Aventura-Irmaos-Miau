package com.irmaosmiau.ui;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
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
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

public class SelecaoPersonagemPanel extends JPanel {

    public SelecaoPersonagemPanel(
            Consumer<TipoPersonagem> aoSelecionar
    ) {

        setPreferredSize(
                new Dimension(
                        800,
                        450
                )
        );

        setBackground(
                new Color(
                        135,
                        206,
                        235
                )
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

        JLabel titulo =
                new JLabel(
                        "ESCOLHA SEU MIAU",
                        SwingConstants.CENTER
                );

        titulo.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        26
                )
        );

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        gbc.insets =
                new Insets(
                        0,
                        0,
                        12,
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

        gbc.insets =
                new Insets(
                        0,
                        30,
                        6,
                        30
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
        // ATRIBUTOS
        // =========================

        IrmaoMiauBranco previewBranco =
                new IrmaoMiauBranco(
                        0,
                        0,
                        1
                );

        IrmaoMiauPreto previewPreto =
                new IrmaoMiauPreto(
                        0,
                        0,
                        1
                );

        JPanel atributosBranco =
                criarPainelAtributos(
                        previewBranco
                                .getAtributos()
                );

        JPanel atributosPreto =
                criarPainelAtributos(
                        previewPreto
                                .getAtributos()
                );

        gbc.gridx = 0;
        gbc.gridy = 2;

        gbc.insets =
                new Insets(
                        0,
                        20,
                        8,
                        20
                );

        painel.add(
                atributosBranco,
                gbc
        );

        gbc.gridx = 1;

        painel.add(
                atributosPreto,
                gbc
        );

        // =========================
        // BOTÕES
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
                        14
                );

        branco.setFont(
                fonteBotao
        );

        preto.setFont(
                fonteBotao
        );

        branco.setPreferredSize(
                new Dimension(
                        170,
                        38
                )
        );

        preto.setPreferredSize(
                new Dimension(
                        170,
                        38
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
        gbc.gridy = 3;

        gbc.insets =
                new Insets(
                        0,
                        20,
                        7,
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
                fonteBotao
        );

        ambos.setPreferredSize(
                new Dimension(
                        240,
                        38
                )
        );

        ambos.addActionListener(
                e -> aoSelecionar.accept(
                        TipoPersonagem.AMBOS
                )
        );

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;

        painel.add(
                ambos,
                gbc
        );

        add(
                painel
        );
    }

    // =============================
    // PAINEL DE ATRIBUTOS
    // =============================

    private JPanel criarPainelAtributos(
            AtributosLutador atributos
    ) {

        JPanel painel =
                new JPanel(
                        new GridBagLayout()
                );

        painel.setOpaque(false);

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(
                        1,
                        3,
                        1,
                        3
                );

        adicionarAtributo(
                painel,
                gbc,
                0,
                "Condicionamento",
                atributos.getCondicionamento()
        );

        adicionarAtributo(
                painel,
                gbc,
                1,
                "Fôlego",
                atributos.getFolego()
        );

        adicionarAtributo(
                painel,
                gbc,
                2,
                "Força",
                atributos.getForca()
        );

        adicionarAtributo(
                painel,
                gbc,
                3,
                "Velocidade",
                atributos.getVelocidade()
        );

        adicionarAtributo(
                painel,
                gbc,
                4,
                "Agilidade",
                atributos.getAgilidade()
        );

        adicionarAtributo(
                painel,
                gbc,
                5,
                "Técnica",
                atributos.getTecnica()
        );

        adicionarAtributo(
                painel,
                gbc,
                6,
                "Recuperação",
                atributos.getRecuperacao()
        );

        adicionarPeso(
        painel,
        gbc,
        7,
        atributos.getPeso()
);

        return painel;
    }

    private void adicionarAtributo(
            JPanel painel,
            GridBagConstraints gbc,
            int linha,
            String nome,
            double valor
    ) {

        JLabel label =
                new JLabel(
                        nome
                );

        label.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        10
                )
        );

        gbc.gridx = 0;
        gbc.gridy = linha;
        gbc.anchor =
                GridBagConstraints.WEST;

        painel.add(
                label,
                gbc
        );

        JProgressBar barra =
                new JProgressBar(
                        0,
                        100
                );

        int valorBarra =
                (int) Math.round(
                        Math.max(
                                0,
                                Math.min(
                                        100,
                                        valor
                                )
                        )
                );

        barra.setValue(
                valorBarra
        );

        barra.setPreferredSize(
                new Dimension(
                        95,
                        8
                )
        );

        barra.setStringPainted(
                false
        );

        barra.setBorderPainted(
                false
        );

        gbc.gridx = 1;

        painel.add(
                barra,
                gbc
        );
    }
    
    private void adicionarPeso(
        JPanel painel,
        GridBagConstraints gbc,
        int linha,
        double peso
) {

    JLabel label =
            new JLabel(
                    "Peso"
            );

    label.setFont(
            new Font(
                    "Arial",
                    Font.BOLD,
                    10
            )
    );

    gbc.gridx = 0;
    gbc.gridy = linha;
    gbc.anchor =
            GridBagConstraints.WEST;

    painel.add(
            label,
            gbc
    );

    JLabel valorPeso =
            new JLabel(
                    String.format(
                            "%.0f kg",
                            peso
                    )
            );

    valorPeso.setFont(
            new Font(
                    "Arial",
                    Font.BOLD,
                    10
            )
    );

    gbc.gridx = 1;
    gbc.anchor =
            GridBagConstraints.WEST;

    painel.add(
            valorPeso,
            gbc
    );
}

    // =============================
    // IMAGEM
    // =============================

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
                            130,
                            130
                    )
            );

            return placeholder;
        }

        ImageIcon original =
                new ImageIcon(
                        recurso
                );

        Image redimensionada =
                original
                        .getImage()
                        .getScaledInstance(
                                130,
                                130,
                                Image.SCALE_SMOOTH
                        );

        JLabel imagem =
                new JLabel(
                        new ImageIcon(
                                redimensionada
                        )
                );

        imagem.setPreferredSize(
                new Dimension(
                        130,
                        130
                )
        );

        return imagem;
    }
}