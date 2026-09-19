package com.irmaosmiau.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class MenuPanel extends JPanel {

    public MenuPanel(Runnable aoComecar) {

        setPreferredSize(new Dimension(800, 450));
        setBackground(new Color(135, 206, 235));
        setLayout(new GridBagLayout());

        JPanel centro = new JPanel(
                new GridBagLayout()
        );

        centro.setOpaque(false);

        GridBagConstraints gbc
                = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titulo = new JLabel(
                "AVENTURA DOS IRMÃOS MIAU",
                SwingConstants.CENTER
        );

        titulo.setFont(
                new Font("Arial", Font.BOLD, 32)
        );

        gbc.gridy = 0;
        gbc.insets = new Insets(
                0,
                0,
                30,
                0
        );

        centro.add(titulo, gbc);

        JButton botaoComecar
                = new JButton("COMEÇAR");

        botaoComecar.setFont(
                new Font("Arial", Font.BOLD, 20)
        );

        botaoComecar.setPreferredSize(
                new Dimension(180, 50)
        );

        botaoComecar.addActionListener(
                e -> aoComecar.run()
        );

        gbc.gridy = 1;
        gbc.insets = new Insets(
                0,
                70,
                0,
                70
        );

        centro.add(botaoComecar, gbc);

        add(centro);
    }
}