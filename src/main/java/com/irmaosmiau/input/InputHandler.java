package com.irmaosmiau.input;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Centraliza todo o mapeamento de teclado dos dois personagens.
 *
 * MAPA RÁPIDO DO ARQUIVO 1. Teclas do Miau Branco 2. Teclas do Miau Preto 3.
 * Estado atual dos controles 4. Configuração dos key bindings 5. Método
 * genérico de mapeamento 6. Getters do Miau Branco 7. Getters do Miau Preto
 */
public class InputHandler {

    // =====================================================
    // 1. TECLAS - MIAU BRANCO
    // =====================================================
    private static final String TECLA_BRANCO_ESQUERDA = "A";
    private static final String TECLA_BRANCO_DIREITA = "D";
    private static final String TECLA_BRANCO_CIMA = "W";
    private static final String TECLA_BRANCO_BAIXO = "S";

    private static final String TECLA_BRANCO_PULAR = "SPACE";
    private static final String TECLA_BRANCO_AGACHAR = "C";
    private static final String TECLA_BRANCO_CORRER = "Z";
    private static final String TECLA_BRANCO_PULAR_CURTO = "V";
    private static final String TECLA_BRANCO_RESPIRAR = "X";
    private static final String TECLA_BRANCO_ROLAR = "R";
    private static final String TECLA_BRANCO_DEITAR = "F";
    private static final String TECLA_BRANCO_SPRAWL = "G";

    /* Primeira entrada do motor de combate. */
    private static final String TECLA_BRANCO_PEGADA_1 = "H";

    // =====================================================
    // 2. TECLAS - MIAU PRETO
    // =====================================================
    private static final String TECLA_PRETO_ESQUERDA = "LEFT";
    private static final String TECLA_PRETO_DIREITA = "RIGHT";
    private static final String TECLA_PRETO_CIMA = "UP";
    private static final String TECLA_PRETO_BAIXO = "DOWN";

    private static final String TECLA_PRETO_PULAR = "N";
    private static final String TECLA_PRETO_AGACHAR = "M";
    private static final String TECLA_PRETO_CORRER = "K";
    private static final String TECLA_PRETO_PULAR_CURTO = "B";
    private static final String TECLA_PRETO_RESPIRAR = "L";
    private static final String TECLA_PRETO_ROLAR = "O";
    private static final String TECLA_PRETO_DEITAR = "J";
    private static final String TECLA_PRETO_SPRAWL = "I";

    // =====================================================
    // 3. ESTADO ATUAL DOS CONTROLES - MIAU BRANCO
    // =====================================================
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
    private boolean sprawl;
    private boolean pegada1;

    // =====================================================
    // 3. ESTADO ATUAL DOS CONTROLES - MIAU PRETO
    // =====================================================
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
    private boolean pretoSprawl;

    /*
     * Entrega 3A: comandos segurados durante um bloqueio de animacao de
     * combate ficam travados ate a tecla ser solta (nada retroativo).
     */
    private static final String PREFIXO_BRANCO = "branco";
    private static final String PREFIXO_PRETO = "preto";

    private final TravaComandos trava = new TravaComandos();

    // =====================================================
    // 4. CONFIGURAÇÃO DOS KEY BINDINGS
    // =====================================================
    public InputHandler(JComponent componente) {

        InputMap inputMap = componente.getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        ActionMap actionMap = componente.getActionMap();

        configurarControlesBranco(inputMap, actionMap);
        configurarControlesPreto(inputMap, actionMap);
    }

    private void configurarControlesBranco(
            InputMap inputMap,
            ActionMap actionMap
    ) {

        // Movimento
        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_ESQUERDA, "brancoEsquerda",
                () -> esquerda = true,
                () -> esquerda = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_DIREITA, "brancoDireita",
                () -> direita = true,
                () -> direita = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_CIMA, "brancoCima",
                () -> cima = true,
                () -> cima = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_BAIXO, "brancoBaixo",
                () -> baixo = true,
                () -> baixo = false);

        // Ações
        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_PULAR, "brancoPular",
                () -> pular = true,
                () -> pular = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_AGACHAR, "brancoAgachar",
                () -> agachar = true,
                () -> agachar = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_CORRER, "brancoCorrer",
                () -> correr = true,
                () -> correr = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_PULAR_CURTO, "brancoPularCurto",
                () -> pularCurto = true,
                () -> pularCurto = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_RESPIRAR, "brancoRespirar",
                () -> respirar = true,
                () -> respirar = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_ROLAR, "brancoRolar",
                () -> rolar = true,
                () -> rolar = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_DEITAR, "brancoDeitar",
                () -> deitar = true,
                () -> deitar = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_SPRAWL, "brancoSprawl",
                () -> sprawl = true,
                () -> sprawl = false);

        mapearTecla(inputMap, actionMap,
                TECLA_BRANCO_PEGADA_1, "brancoPegada1",
                () -> pegada1 = true,
                () -> pegada1 = false);
    }

    private void configurarControlesPreto(
            InputMap inputMap,
            ActionMap actionMap
    ) {

        // Movimento
        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_ESQUERDA, "pretoEsquerda",
                () -> pretoEsquerda = true,
                () -> pretoEsquerda = false);

        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_DIREITA, "pretoDireita",
                () -> pretoDireita = true,
                () -> pretoDireita = false);

        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_CIMA, "pretoCima",
                () -> pretoCima = true,
                () -> pretoCima = false);

        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_BAIXO, "pretoBaixo",
                () -> pretoBaixo = true,
                () -> pretoBaixo = false);

        // Ações
        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_PULAR, "pretoPular",
                () -> pretoPular = true,
                () -> pretoPular = false);

        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_AGACHAR, "pretoAgachar",
                () -> pretoAgachar = true,
                () -> pretoAgachar = false);

        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_CORRER, "pretoCorrer",
                () -> pretoCorrer = true,
                () -> pretoCorrer = false);

        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_PULAR_CURTO, "pretoPularCurto",
                () -> pretoPularCurto = true,
                () -> pretoPularCurto = false);

        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_RESPIRAR, "pretoRespirar",
                () -> pretoRespirar = true,
                () -> pretoRespirar = false);

        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_ROLAR, "pretoRolar",
                () -> pretoRolar = true,
                () -> pretoRolar = false);

        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_DEITAR, "pretoDeitar",
                () -> pretoDeitar = true,
                () -> pretoDeitar = false);

        mapearTecla(inputMap, actionMap,
                TECLA_PRETO_SPRAWL, "pretoSprawl",
                () -> pretoSprawl = true,
                () -> pretoSprawl = false);
    }

    // =====================================================
    // 5. MAPEAMENTO GENÉRICO
    // =====================================================
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
                trava.pressionar(nome);
                aoPressionar.run();
            }
        }
        );

        actionMap.put(
                solta,
                new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                trava.soltar(nome);
                aoSoltar.run();
            }
        }
        );
    }

    // =====================================================
    // 5.1 TRAVA DE COMANDOS DURANTE BLOQUEIO (Entrega 3A)
    // =====================================================
    /**
     * Chamado a cada frame em que o Branco esta bloqueado por uma animacao
     * de combate: toda tecla dele que estiver segurada passa a ser lida como
     * solta ate ser liberada e pressionada de novo.
     */
    public void travarComandosPressionadosBranco() {
        trava.travarPressionados(PREFIXO_BRANCO);
    }

    /** Igual a {@link #travarComandosPressionadosBranco()}, para o Preto. */
    public void travarComandosPressionadosPreto() {
        trava.travarPressionados(PREFIXO_PRETO);
    }

    // =====================================================
    // 6. GETTERS - MIAU BRANCO
    // =====================================================
    public boolean isEsquerda() {
        return esquerda && trava.isLivre("brancoEsquerda");
    }

    public boolean isDireita() {
        return direita && trava.isLivre("brancoDireita");
    }

    public boolean isCima() {
        return cima && trava.isLivre("brancoCima");
    }

    public boolean isBaixo() {
        return baixo && trava.isLivre("brancoBaixo");
    }

    public boolean isPular() {
        return pular && trava.isLivre("brancoPular");
    }

    public boolean isAgachar() {
        return agachar && trava.isLivre("brancoAgachar");
    }

    public boolean isCorrer() {
        return correr && trava.isLivre("brancoCorrer");
    }

    public boolean isPularCurto() {
        return pularCurto && trava.isLivre("brancoPularCurto");
    }

    public boolean isRespirar() {
        return respirar && trava.isLivre("brancoRespirar");
    }

    public boolean isRolar() {
        return rolar && trava.isLivre("brancoRolar");
    }

    public boolean isDeitar() {
        return deitar && trava.isLivre("brancoDeitar");
    }

    public boolean isSprawl() {
        return sprawl && trava.isLivre("brancoSprawl");
    }

    public boolean isPegada1() {
        return pegada1 && trava.isLivre("brancoPegada1");
    }

    // =====================================================
    // 7. GETTERS - MIAU PRETO
    // =====================================================
    public boolean isPretoEsquerda() {
        return pretoEsquerda && trava.isLivre("pretoEsquerda");
    }

    public boolean isPretoDireita() {
        return pretoDireita && trava.isLivre("pretoDireita");
    }

    public boolean isPretoCima() {
        return pretoCima && trava.isLivre("pretoCima");
    }

    public boolean isPretoBaixo() {
        return pretoBaixo && trava.isLivre("pretoBaixo");
    }

    public boolean isPretoPular() {
        return pretoPular && trava.isLivre("pretoPular");
    }

    public boolean isPretoAgachar() {
        return pretoAgachar && trava.isLivre("pretoAgachar");
    }

    public boolean isPretoCorrer() {
        return pretoCorrer && trava.isLivre("pretoCorrer");
    }

    public boolean isPretoPularCurto() {
        return pretoPularCurto && trava.isLivre("pretoPularCurto");
    }

    public boolean isPretoRespirar() {
        return pretoRespirar && trava.isLivre("pretoRespirar");
    }

    public boolean isPretoRolar() {
        return pretoRolar && trava.isLivre("pretoRolar");
    }

    public boolean isPretoDeitar() {
        return pretoDeitar && trava.isLivre("pretoDeitar");
    }

    public boolean isPretoSprawl() {
        return pretoSprawl && trava.isLivre("pretoSprawl");
    }
}
