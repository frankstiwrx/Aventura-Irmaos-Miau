package com.irmaosmiau.input;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class InputHandler {

    private boolean esquerda;
    private boolean direita;
    private boolean cima;
    private boolean baixo;

    private boolean pular;
    private boolean agachar;

    public InputHandler(JComponent componente) {

        InputMap inputMap = componente.getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        ActionMap actionMap = componente.getActionMap();

        mapearTecla(
                inputMap,
                actionMap,
                "A",
                "esquerda",
                () -> esquerda = true,
                () -> esquerda = false
        );

        mapearTecla(
                inputMap,
                actionMap,
                "D",
                "direita",
                () -> direita = true,
                () -> direita = false
        );

        mapearTecla(
                inputMap,
                actionMap,
                "W",
                "cima",
                () -> cima = true,
                () -> cima = false
        );

        mapearTecla(
                inputMap,
                actionMap,
                "S",
                "baixo",
                () -> baixo = true,
                () -> baixo = false
        );

        mapearTecla(
                inputMap,
                actionMap,
                "SPACE",
                "pular",
                () -> pular = true,
                () -> pular = false
        );

        mapearTecla(
                inputMap,
                actionMap,
                "C",
                "agachar",
                () -> agachar = true,
                () -> agachar = false
        );
    }

    private void mapearTecla(
            InputMap inputMap,
            ActionMap actionMap,
            String tecla,
            String nome,
            Runnable aoPressionar,
            Runnable aoSoltar
    ) {

        String pressionada = nome + "Pressionada";
        String solta = nome + "Solta";

        inputMap.put(
                KeyStroke.getKeyStroke("pressed " + tecla),
                pressionada
        );

        inputMap.put(
                KeyStroke.getKeyStroke("released " + tecla),
                solta
        );

        actionMap.put(
                pressionada,
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        aoPressionar.run();
                    }
                }
        );

        actionMap.put(
                solta,
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        aoSoltar.run();
                    }
                }
        );
    }

    public boolean isEsquerda() {
        return esquerda;
    }

    public boolean isDireita() {
        return direita;
    }

    public boolean isCima() {
        return cima;
    }

    public boolean isBaixo() {
        return baixo;
    }

    public boolean isPular() {
        return pular;
    }

    public boolean isAgachar() {
        return agachar;
    }
}