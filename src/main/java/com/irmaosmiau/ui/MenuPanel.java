package com.irmaosmiau.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class MenuPanel extends JPanel {

    public MenuPanel(Runnable aoComecar) {

        setPreferredSize(new Dimension(800, 450));
        setBackground(new Color(135, 206, 235));
        setLayout(new BorderLayout());

        JLabel titulo = new JLabel(
                "AVENTURA DOS IRMÃOS MIAU",
                SwingConstants.CENTER
        );

        titulo.setFont(
                new Font("Arial", Font.BOLD, 32)
        );

        JButton botaoComecar = new JButton("COMEÇAR");

        botaoComecar.setFont(
                new Font("Arial", Font.BOLD, 20)
        );

        botaoComecar.addActionListener(e -> aoComecar.run());

        JPanel centro = new JPanel();

        centro.setOpaque(false);
        centro.add(botaoComecar);

        add(titulo, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
    }
}