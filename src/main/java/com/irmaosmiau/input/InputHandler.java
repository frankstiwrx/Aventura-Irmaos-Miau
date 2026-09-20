package com.irmaosmiau.input;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class InputHandler {

    // =============================
    // MIAU BRANCO
    // =============================
    private boolean esquerda;
    private boolean direita;
    private boolean cima;
    private boolean baixo;

    private boolean pular;
    private boolean agachar;

    private boolean correr;
    private boolean pularCurto;
    private boolean respirar;
    private boolean rolar;

    private boolean deitar;

    // =============================
    // MIAU PRETO
    // =============================
    private boolean pretoEsquerda;
    private boolean pretoDireita;
    private boolean pretoCima;
    private boolean pretoBaixo;

    private boolean pretoPular;
    private boolean pretoAgachar;

    private boolean pretoCorrer;
    private boolean pretoPularCurto;
    private boolean pretoRespirar;
    private boolean pretoRolar;
    
    private boolean pretoDeitar;

    public InputHandler(JComponent componente) {

        InputMap inputMap = componente.getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        ActionMap actionMap = componente.getActionMap();

        // =========================
        // CONTROLES DO MIAU BRANCO
        // =========================
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
        "R",
        "rolar",
        () -> rolar = true,
        () -> rolar = false
);
        
        mapearTecla(
        inputMap,
        actionMap,
        "F",
        "deitar",
        () -> deitar = true,
        () -> deitar = false
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

        mapearTecla(
        inputMap,
        actionMap,
        "Z",
        "correr",
        () -> correr = true,
        () -> correr = false
);
        
        mapearTecla(
        inputMap,
        actionMap,
        "X",
        "respirar",
        () -> respirar = true,
        () -> respirar = false
);

mapearTecla(
        inputMap,
        actionMap,
        "V",
        "pularCurto",
        () -> pularCurto = true,
        () -> pularCurto = false
);
        
        // =========================
        // CONTROLES DO MIAU PRETO
        // =========================
        mapearTecla(
                inputMap,
                actionMap,
                "LEFT",
                "pretoEsquerda",
                () -> pretoEsquerda = true,
                () -> pretoEsquerda = false
        );
        
        mapearTecla(
        inputMap,
        actionMap,
        "O",
        "pretoRolar",
        () -> pretoRolar = true,
        () -> pretoRolar = false
);
mapearTecla(
        inputMap,
        actionMap,
        "J",
        "pretoDeitar",
        () -> pretoDeitar = true,
        () -> pretoDeitar = false
);
        mapearTecla(
                inputMap,
                actionMap,
                "RIGHT",
                "pretoDireita",
                () -> pretoDireita = true,
                () -> pretoDireita = false
        );

        mapearTecla(
                inputMap,
                actionMap,
                "UP",
                "pretoCima",
                () -> pretoCima = true,
                () -> pretoCima = false
        );

        mapearTecla(
                inputMap,
                actionMap,
                "DOWN",
                "pretoBaixo",
                () -> pretoBaixo = true,
                () -> pretoBaixo = false
        );

        mapearTecla(
                inputMap,
                actionMap,
                "N",
                "pretoPular",
                () -> pretoPular = true,
                () -> pretoPular = false
        );
        
        mapearTecla(
        inputMap,
        actionMap,
        "L",
        "pretoRespirar",
        () -> pretoRespirar = true,
        () -> pretoRespirar = false
);

        mapearTecla(
                inputMap,
                actionMap,
                "M",
                "pretoAgachar",
                () -> pretoAgachar = true,
                () -> pretoAgachar = false
        );
        
        mapearTecla(
        inputMap,
        actionMap,
        "K",
        "pretoCorrer",
        () -> pretoCorrer = true,
        () -> pretoCorrer = false
);

mapearTecla(
        inputMap,
        actionMap,
        "B",
        "pretoPularCurto",
        () -> pretoPularCurto = true,
        () -> pretoPularCurto = false
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
                KeyStroke.getKeyStroke(
                        "pressed " + tecla
                ),
                pressionada
        );

        inputMap.put(
                KeyStroke.getKeyStroke(
                        "released " + tecla
                ),
                solta
        );

        actionMap.put(
                pressionada,
                new AbstractAction() {

            @Override
            public void actionPerformed(
                    ActionEvent e
            ) {

                aoPressionar.run();
            }
        }
        );

        actionMap.put(
                solta,
                new AbstractAction() {

            @Override
            public void actionPerformed(
                    ActionEvent e
            ) {

                aoSoltar.run();
            }
        }
        );
    }

    // =============================
    // GETTERS - MIAU BRANCO
    // =============================
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
    
    public boolean isCorrer() {
    return correr;
}

public boolean isPularCurto() {
    return pularCurto;
}

public boolean isRespirar() {

    return respirar;
}

public boolean isRolar() {

    return rolar;
}
public boolean isDeitar() {

    return deitar;
}

    // =============================
    // GETTERS - MIAU PRETO
    // =============================
    public boolean isPretoEsquerda() {
        return pretoEsquerda;
    }

    public boolean isPretoDireita() {
        return pretoDireita;
    }

    public boolean isPretoCima() {
        return pretoCima;
    }

    public boolean isPretoBaixo() {
        return pretoBaixo;
    }

    public boolean isPretoPular() {
        return pretoPular;
    }

    public boolean isPretoAgachar() {
        return pretoAgachar;
    }
    public boolean isPretoCorrer() {
    return pretoCorrer;
}

public boolean isPretoPularCurto() {
    return pretoPularCurto;
}

public boolean isPretoRespirar() {

    return pretoRespirar;
    
}
public boolean isPretoRolar() {

    return pretoRolar;
}

public boolean isPretoDeitar() {

    return pretoDeitar;
}

}
