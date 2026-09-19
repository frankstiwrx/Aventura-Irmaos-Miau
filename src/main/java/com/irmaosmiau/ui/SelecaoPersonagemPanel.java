package com.irmaosmiau.ui;

import com.irmaosmiau.entities.TipoPersonagem;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SelecaoPersonagemPanel extends JPanel {

    public SelecaoPersonagemPanel(
            Consumer<TipoPersonagem> aoSelecionar
    ) {

        setPreferredSize(new Dimension(800, 450));

        setBackground(
                new Color(135, 206, 235)
        );

        setLayout(new GridBagLayout());

        JPanel painel = new JPanel();

        painel.setOpaque(false);

        JLabel titulo = new JLabel(
                "ESCOLHA SEU IRMÃO MIAU"
        );

        titulo.setFont(
                new Font("Arial", Font.BOLD, 25)
        );

        JButton branco = new JButton(
                "Miau Branco - Passador"
        );

        JButton preto = new JButton(
                "Miau Preto - Guardeiro"
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

        painel.add(titulo);
        painel.add(branco);
        painel.add(preto);

        add(painel);
    }
}