package com.irmaosmiau.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

public abstract class IrmaoMiau {

    protected int x;
    protected int y;

    protected double escala;

    protected Color corPelo;
    protected Color corKimono;
    protected Color corRosto;

    protected String nome;
    protected String estilo;

    protected AtributosLutador atributos;

    private double condicionamentoAtual;
    private double folegoAtual;

    private boolean correndo = false;

    private static final double CUSTO_PULO_ALTO = 7.0;
    private static final double CUSTO_PULO_CURTO = 3.0;

    private static final double VELOCIDADE_CANSADO = 0.50;

    private static final double CUSTO_MOVIMENTO_CANSADO = 0.010;

    private static final double LIMITE_FOLEGO_ZERO = 0.05;

    // =============================
    // LINHAS DO CENÁRIO
    // =============================
    private static final int TOTAL_LINHAS = 5;
    private static final int ESPACO_ENTRE_LINHAS = 20;

    // 0 internamente significa Linha 1
    private int linhaAtual = 0;

    // Posição vertical da primeira linha
    private final int yLinha1;

    private int yBase;

    // =============================
    // MOVIMENTO
    // =============================
    private boolean pulando = false;
    private boolean agachado = false;
    private boolean andando = false;

    private double alturaPulo = 0;
    private double velocidadePulo = 0;

    private double faseAnimacao = 0;

    // =============================
    // SISTEMA DE IDLE
    // =============================
    private enum EstadoIdle {

        NENHUM,
        EM_PE,
        AGACHADO,
        TAUNT
    }

    private EstadoIdle estadoIdle
            = EstadoIdle.EM_PE;

    /*
     * 0 = postura base
     * 1 a 5 = pequenas variações
     */
    private int poseIdle = 0;

    private int indiceSequenciaIdle = 0;

    private long proximaTrocaIdle
            = System.currentTimeMillis() + 500;

    private long ultimaAtividade
            = System.currentTimeMillis();

    private static final long TEMPO_POSE_IDLE = 450;

    private static final long TEMPO_PARA_TAUNT = 7000;

    // =============================
    // SEQUÊNCIA IDLE EM PÉ
    // =============================
    private static final int[] SEQUENCIA_IDLE_EM_PE = {
        0,
        1,
        2,
        3,
        1,
        4,
        2,
        5,
        0,
        3,
        2,
        4,
        1,
        5,
        2,
        0
    };

    // =============================
    // SEQUÊNCIA IDLE AGACHADO
    // =============================
    private static final int[] SEQUENCIA_IDLE_AGACHADO = {
        0,
        1,
        3,
        2,
        4,
        2,
        5,
        1,
        3,
        0,
        4,
        2,
        5,
        3,
        1,
        0
    };

    // =============================
    // SEQUÊNCIA DO TAUNT
    // =============================
    private static final int[] SEQUENCIA_IDLE_TAUNT = {
        0,
        1,
        2,
        1,
        3,
        4,
        2,
        5,
        3,
        1,
        4,
        0,
        2,
        5,
        1,
        3
    };

    // =============================
    // PISCAR
    // =============================
    private boolean piscando = false;

    private long proximoPiscar
            = System.currentTimeMillis() + 2000;

    private long fimDoPiscar = 0;
    
    // recuperar condicionamento
    
    private void recuperarCondicionamentoLeve() {

    /*
     * Sem fôlego o corpo não consegue
     * recuperar condicionamento.
     */
    if (
            isSemFolego()
            || andando
            || correndo
            || pulando
    ) {

        return;
    }

    double taxa =
            0.002
            + atributos.getRecuperacao()
            * 0.000025;

    condicionamentoAtual += taxa;

    if (
            condicionamentoAtual
            > atributos.getCondicionamento()
    ) {

        condicionamentoAtual =
                atributos.getCondicionamento();
    }
}

    // =============================
    // CONSTRUTOR
    // =============================
    public IrmaoMiau(
            int x,
            int y,
            double escala,
            Color corPelo,
            Color corKimono,
            Color corRosto,
            String nome,
            String estilo,
            AtributosLutador atributos
    ) {

        this.x = x;
        this.y = y;

        this.yLinha1 = y;
        this.yBase = y;

        this.escala = escala;

        this.corPelo = corPelo;
        this.corKimono = corKimono;
        this.corRosto = corRosto;

        this.nome = nome;
        this.estilo = estilo;

        this.atributos = atributos;

        this.condicionamentoAtual
                = atributos.getCondicionamento();

        this.folegoAtual
                = atributos.getFolego();
    }

    public abstract void desenhar(Graphics2D g2);

    // =============================
    // ATUALIZAÇÃO
    // =============================
    public void atualizar(
            boolean estaAndando,
            boolean querAgachar
    ) {

        // =========================
        // ESTADO ATUAL
        // =========================
        andando = estaAndando;

        agachado
                = querAgachar
                && !pulando;

        /*
     * Se ficou agachado ou pulou,
     * não pode continuar correndo.
         */
        if (agachado
                || pulando) {

            correndo = false;
        }

        // =========================
// FÔLEGO / CONDICIONAMENTO
// =========================

if (
        correndo
        && andando
        && !isSemFolego()
) {

    // Corrida consome fôlego rapidamente.
    gastarEnergia(
            0.075
    );

} else if (
        andando
        && isSemFolego()
) {

    /*
     * O personagem ainda consegue caminhar,
     * mas está se arrastando fisicamente.
     */
    gastarCondicionamento(
            CUSTO_MOVIMENTO_CANSADO
    );

} else {

    recuperarFolego();
}

recuperarCondicionamentoLeve();

        // =========================
        // ANIMAÇÃO DE CAMINHADA
        // =========================
        if (andando) {

            if (correndo) {

                faseAnimacao += 0.42;

            } else if (agachado) {

                faseAnimacao += 0.14;

            } else {

                faseAnimacao += 0.25;
            }

        } else {

            faseAnimacao = 0;
        }

        // =========================
        // PULO
        // =========================
        if (pulando) {

            alturaPulo += velocidadePulo;

            velocidadePulo -= 0.65;

            if (alturaPulo <= 0
                    && velocidadePulo < 0) {

                alturaPulo = 0;
                velocidadePulo = 0;

                pulando = false;
            }
        }

        // Linha onde ele está
        // menos a altura do pulo
        y
                = yBase
                - (int) Math.round(
                        alturaPulo
                );

        atualizarIdle(
                estaAndando,
                querAgachar
        );

        atualizarPiscar();
    }

    // =============================
    // SISTEMA DE IDLE
    // =============================
    private void atualizarIdle(
            boolean estaAndando,
            boolean querAgachar
    ) {

        long agora
                = System.currentTimeMillis();

        /*
         * Qualquer atividade reinicia
         * o relógio do TAUNT.
         */
        if (estaAndando
                || querAgachar
                || pulando) {

            ultimaAtividade = agora;
        }

        // =========================
        // MOVIMENTO / PULO
        // =========================
        if (pulando
                || estaAndando) {

            mudarEstadoIdle(
                    EstadoIdle.NENHUM,
                    agora
            );

            return;
        }

        // =========================
        // AGACHADO PARADO
        // =========================
        if (agachado) {

            mudarEstadoIdle(
                    EstadoIdle.AGACHADO,
                    agora
            );

            atualizarPoseIdle(
                    agora
            );

            return;
        }

        // =========================
        // TAUNT
        // =========================
        if (agora - ultimaAtividade
                >= TEMPO_PARA_TAUNT) {

            mudarEstadoIdle(
                    EstadoIdle.TAUNT,
                    agora
            );

            atualizarPoseIdle(
                    agora
            );

            return;
        }

        // =========================
        // IDLE NORMAL EM PÉ
        // =========================
        mudarEstadoIdle(
                EstadoIdle.EM_PE,
                agora
        );

        atualizarPoseIdle(
                agora
        );
    }

    private void mudarEstadoIdle(
            EstadoIdle novoEstado,
            long agora
    ) {

        if (estadoIdle
                == novoEstado) {

            return;
        }

        estadoIdle = novoEstado;

        poseIdle = 0;

        indiceSequenciaIdle = 0;

        proximaTrocaIdle
                = agora + TEMPO_POSE_IDLE;
    }

    private void atualizarPoseIdle(
            long agora
    ) {

        if (estadoIdle
                == EstadoIdle.NENHUM) {

            poseIdle = 0;

            return;
        }

        if (agora
                < proximaTrocaIdle) {

            return;
        }

        int[] sequencia
                = getSequenciaIdleAtual();

        indiceSequenciaIdle++;

        if (indiceSequenciaIdle
                >= sequencia.length) {

            indiceSequenciaIdle = 0;
        }

        poseIdle
                = sequencia[indiceSequenciaIdle];

        proximaTrocaIdle
                = agora + TEMPO_POSE_IDLE;
    }

    private int[] getSequenciaIdleAtual() {

        if (estadoIdle
                == EstadoIdle.AGACHADO) {

            return SEQUENCIA_IDLE_AGACHADO;
        }

        if (estadoIdle
                == EstadoIdle.TAUNT) {

            return SEQUENCIA_IDLE_TAUNT;
        }

        return SEQUENCIA_IDLE_EM_PE;
    }

    // =============================
    // PISCAR
    // =============================
    private void atualizarPiscar() {

        long agora
                = System.currentTimeMillis();

        if (!piscando
                && agora >= proximoPiscar) {

            piscando = true;

            // Olho fechado por 120 ms
            fimDoPiscar
                    = agora + 120;
        }

        if (piscando
                && agora >= fimDoPiscar) {

            piscando = false;

            proximoPiscar
                    = agora + 2000;
        }
    }

    // =============================
    // MOVIMENTO VERTICAL
    // =============================
    public void moverVertical(
            int quantidade
    ) {

        int yMinimo
                = yLinha1;

        int yMaximo
                = yLinha1
                + (TOTAL_LINHAS - 1)
                * ESPACO_ENTRE_LINHAS;

        yBase += quantidade;

        // Não deixa passar da Linha 1
        if (yBase
                < yMinimo) {

            yBase = yMinimo;
        }

        // Não deixa passar da Linha 5
        if (yBase
                > yMaximo) {

            yBase = yMaximo;
        }

        // Descobre qual das 5 linhas
        // está mais próxima da posição atual
        linhaAtual
                = (int) Math.round(
                        (double) (yBase
                        - yLinha1)
                        / ESPACO_ENTRE_LINHAS
                );
    }

    // =============================
    // MOVIMENTO HORIZONTAL
    // =============================
    public void moverX(
            int quantidade
    ) {

        x += quantidade;
    }

    // =============================
    // LINHAS
    // =============================
    public void subirLinha() {

        if (linhaAtual > 0) {

            linhaAtual--;

            atualizarPosicaoLinha();
        }
    }

    public void descerLinha() {

        if (linhaAtual
                < TOTAL_LINHAS - 1) {

            linhaAtual++;

            atualizarPosicaoLinha();
        }
    }

    private void atualizarPosicaoLinha() {

        yBase
                = yLinha1
                + linhaAtual
                * ESPACO_ENTRE_LINHAS;
    }

    // =============================
    // PULO
    // =============================
    public void pular() {

    if (
            pulando
            || agachado
            || condicionamentoAtual <= 0
    ) {

        return;
    }

    /*
     * Sem fôlego suficiente para
     * o pulo alto, ele tenta apenas
     * o pulo curto.
     */
    if (
            folegoAtual
            < CUSTO_PULO_ALTO
    ) {

        pularCurto();

        return;
    }

    ultimaAtividade =
            System.currentTimeMillis();

    gastarEnergia(
            CUSTO_PULO_ALTO
    );

    pulando = true;

    alturaPulo = 1;

    velocidadePulo = 10;
}

    // =============================
    // CORRER
    // =============================
    public void setCorrendo(
        boolean correndo
) {

    this.correndo =
            correndo
            && !pulando
            && !agachado
            && !isSemFolego()
            && condicionamentoAtual > 0;
}

    public boolean isCorrendo() {

        return correndo;
    }

    // =============================
    // PULO CURTO
    // =============================
    public void pularCurto() {

    if (
            pulando
            || agachado
            || condicionamentoAtual <= 0
    ) {

        return;
    }

    ultimaAtividade =
            System.currentTimeMillis();

    pulando = true;

    alturaPulo = 1;

    if (
            folegoAtual
            >= CUSTO_PULO_CURTO
    ) {

        gastarEnergia(
                CUSTO_PULO_CURTO
        );

        velocidadePulo = 6.5;

    } else {

        /*
         * Pulinho cansado.
         *
         * Ele ainda consegue sair um pouco
         * do chão, mas não possui explosão.
         */
        velocidadePulo = 4.5;

        gastarCondicionamento(
                0.25
        );
    }
}

    // =============================
    // ANIMAÇÃO DE CAMINHADA
    // =============================
    protected int getOscilacaoPasso() {

        if (!andando) {
            return 0;
        }

        double amplitude
                = correndo
                        ? 15
                        : 10;

        return (int) (Math.sin(
                faseAnimacao
        )
                * amplitude
                * escala);
    }

    //Velocidade
    public double getVelocidadeBase() {

    double velocidadeNormal =
            3.2
            + (
                    atributos.getVelocidade()
                    / 100.0
            ) * 1.6;

    if (isSemFolego()) {

        return velocidadeNormal
                * VELOCIDADE_CANSADO;
    }

    return velocidadeNormal;
}
    public double getVelocidadeCorrida() {

    if (isSemFolego()) {

        /*
         * Sem fôlego não existe corrida.
         *
         * Retornamos apenas a velocidade
         * já reduzida do personagem.
         */
        return getVelocidadeBase();
    }

    return getVelocidadeBase()
            * 1.70;
}

    public double getVelocidadeAgachado() {

        return getVelocidadeBase()
                * 0.25;
    }

    // =============================
    // INFORMAÇÕES DO IDLE
    // =============================
    protected int getPoseIdle() {

        return poseIdle;
    }

    protected boolean isIdleEmPe() {

        return estadoIdle
                == EstadoIdle.EM_PE;
    }

    protected boolean isIdleAgachado() {

        return estadoIdle
                == EstadoIdle.AGACHADO;
    }

    protected boolean isTaunt() {

        return estadoIdle
                == EstadoIdle.TAUNT;
    }

    protected boolean isAndando() {

        return andando;
    }

    // =============================
    // OSCILAÇÃO IDLE ANTIGA
    // =============================
    /*
     * Mantivemos esse método porque
     * IrmaoMiauBranco e IrmaoMiauPreto
     * podem já estar usando ele.
     *
     * Agora ele suporta cinco poses.
     */
    protected int getOscilacaoIdle() {

        switch (poseIdle) {

            case 1:
                return (int) Math.round(
                        -3 * escala
                );

            case 2:
                return (int) Math.round(
                        2 * escala
                );

            case 3:
                return (int) Math.round(
                        -1 * escala
                );

            case 4:
                return (int) Math.round(
                        3 * escala
                );

            case 5:
                return (int) Math.round(
                        1 * escala
                );

            default:
                return 0;
        }
    }

    // =============================
    // DESLOCAMENTOS PARA AS POSES
    // =============================
    protected int getIdleX(
            int amplitude
    ) {

        switch (poseIdle) {

            case 1:
                return -amplitude;

            case 2:
                return amplitude;

            case 3:
                return 0;

            case 4:
                return amplitude / 2;

            case 5:
                return -amplitude / 2;

            default:
                return 0;
        }
    }

    protected int getIdleY(
            int amplitude
    ) {

        switch (poseIdle) {

            case 1:
                return 0;

            case 2:
                return amplitude;

            case 3:
                return -amplitude;

            case 4:
                return amplitude / 2;

            case 5:
                return -amplitude / 2;

            default:
                return 0;
        }
    }

    protected int getIdleAlternado(
            int amplitude
    ) {

        switch (poseIdle) {

            case 1:
                return amplitude;

            case 2:
                return -amplitude;

            case 3:
                return amplitude / 2;

            case 4:
                return -amplitude / 2;

            case 5:
                return amplitude;

            default:
                return 0;
        }
    }

    // =============================
    // POSIÇÃO VISUAL
    // =============================
    protected int getYVisual() {

        int deslocamentoAgachado = 0;

        if (agachado) {

            /*
             * Antes estava 18 fixo.
             *
             * Agora cada Miau pode sobrescrever
             * getProfundidadeAgachamento().
             */
            deslocamentoAgachado
                    = (int) Math.round(
                            getProfundidadeAgachamento()
                            * escala
                    );
        }

        int deslocamentoIdle
                = getDeslocamentoVerticalIdle();

        return y
                + deslocamentoAgachado
                + deslocamentoIdle;
    }

    private int getDeslocamentoVerticalIdle() {

        // =========================
        // IDLE EM PÉ
        // =========================
        if (estadoIdle
                == EstadoIdle.EM_PE) {

            switch (poseIdle) {

                case 2:
                    return (int) Math.round(
                            2 * escala
                    );

                case 3:
                    return -(int) Math.round(
                            1 * escala
                    );

                case 4:
                    return (int) Math.round(
                            1 * escala
                    );

                case 5:
                    return -(int) Math.round(
                            1 * escala
                    );

                default:
                    return 0;
            }
        }

        // =========================
        // IDLE AGACHADO
        // =========================
        if (estadoIdle
                == EstadoIdle.AGACHADO) {

            switch (poseIdle) {

                case 1:
                    return (int) Math.round(
                            1 * escala
                    );

                case 3:
                    return (int) Math.round(
                            1 * escala
                    );

                case 4:
                    return -(int) Math.round(
                            1 * escala
                    );

                default:
                    return 0;
            }
        }

        // =========================
        // TAUNT
        // =========================
        if (estadoIdle
                == EstadoIdle.TAUNT) {

            switch (poseIdle) {

                case 2:
                    return (int) Math.round(
                            1 * escala
                    );

                case 4:
                    return -(int) Math.round(
                            1 * escala
                    );

                default:
                    return 0;
            }
        }

        return 0;
    }

    // =============================
    // AGACHAMENTO
    // =============================
    protected double getProfundidadeAgachamento() {

        return 18;
    }

    protected int getEncolhimentoAgachado() {

        if (agachado) {

            return (int) (getFlexaoPernasAgachado()
                    * escala);
        }

        return 0;
    }

    protected double getFlexaoPernasAgachado() {

        return 30;
    }

    // =============================
    // ESTADOS
    // =============================
    public boolean isAgachado() {

        return agachado;
    }

    public boolean isPulando() {

        return pulando;
    }

    public int getLinhaAtual() {

        return linhaAtual;
    }

    // =============================
    // DESENHO BASE
    // =============================
    protected void prepararDesenho(
            Graphics2D g2
    ) {

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
    }

    protected void desenharCabeca(
            Graphics2D g2
    ) {

        int yCabeca
                = getYVisual();

        int cabeca
                = (int) (70
                * escala);

        // =========================
        // CABEÇA
        // =========================
        g2.setColor(
                corPelo
        );

        g2.fillOval(
                x,
                yCabeca,
                cabeca,
                cabeca
        );

        // =========================
        // ORELHAS
        // =========================
        Polygon esquerda
                = new Polygon();

        esquerda.addPoint(
                x + (int) (8 * escala),
                yCabeca + (int) (8 * escala)
        );

        esquerda.addPoint(
                x + (int) (16 * escala),
                yCabeca - (int) (25 * escala)
        );

        esquerda.addPoint(
                x + (int) (28 * escala),
                yCabeca + (int) (8 * escala)
        );

        Polygon direita
                = new Polygon();

        direita.addPoint(
                x + (int) (43 * escala),
                yCabeca + (int) (8 * escala)
        );

        direita.addPoint(
                x + (int) (55 * escala),
                yCabeca - (int) (25 * escala)
        );

        direita.addPoint(
                x + (int) (63 * escala),
                yCabeca + (int) (8 * escala)
        );

        g2.fillPolygon(
                esquerda
        );

        g2.fillPolygon(
                direita
        );

        // =========================
        // OLHOS
        // =========================
        g2.setColor(
                corRosto
        );

        if (!piscando) {

            int olho
                    = (int) (6
                    * escala);

            g2.fillOval(
                    x + (int) (22 * escala),
                    yCabeca + (int) (30 * escala),
                    olho,
                    olho
            );

            g2.fillOval(
                    x + (int) (45 * escala),
                    yCabeca + (int) (30 * escala),
                    olho,
                    olho
            );

        } else {

            g2.setStroke(
                    new BasicStroke(
                            (float) (2
                            * escala)
                    )
            );

            g2.drawLine(
                    x + (int) (21 * escala),
                    yCabeca + (int) (33 * escala),
                    x + (int) (29 * escala),
                    yCabeca + (int) (33 * escala)
            );

            g2.drawLine(
                    x + (int) (44 * escala),
                    yCabeca + (int) (33 * escala),
                    x + (int) (52 * escala),
                    yCabeca + (int) (33 * escala)
            );
        }

        // =========================
        // NARIZ
        // =========================
        g2.setColor(
                new Color(
                        220,
                        120,
                        140
                )
        );

        Polygon nariz
                = new Polygon();

        nariz.addPoint(
                x + (int) (33 * escala),
                yCabeca + (int) (43 * escala)
        );

        nariz.addPoint(
                x + (int) (40 * escala),
                yCabeca + (int) (43 * escala)
        );

        nariz.addPoint(
                x + (int) (36 * escala),
                yCabeca + (int) (48 * escala)
        );

        g2.fillPolygon(
                nariz
        );

        // =========================
        // BOCA
        // =========================
        g2.setColor(
                corRosto
        );

        int bocaX
                = x
                + (int) (36
                * escala);

        int bocaY
                = yCabeca
                + (int) (48
                * escala);

        g2.drawLine(
                bocaX,
                bocaY,
                bocaX,
                bocaY + (int) (5 * escala)
        );

        g2.drawLine(
                bocaX,
                bocaY + (int) (5 * escala),
                bocaX - (int) (5 * escala),
                bocaY + (int) (9 * escala)
        );

        g2.drawLine(
                bocaX,
                bocaY + (int) (5 * escala),
                bocaX + (int) (5 * escala),
                bocaY + (int) (9 * escala)
        );
    }

    protected void desenharPata(
            Graphics2D g2,
            int pataX,
            int pataY,
            int largura,
            int altura
    ) {

        g2.setColor(
                corPelo
        );

        g2.fillRoundRect(
                pataX,
                pataY,
                largura,
                altura,
                5,
                5
        );
    }

    // =============================
    // GETTERS
    // =============================
    public String getNome() {

        return nome;
    }

    public String getEstilo() {

        return estilo;
    }

    public int getX() {

        return x;
    }

    public int getY() {

        return y;
    }

    //fólego
    public double getPercentualFolego() {

        return folegoAtual
                / atributos.getFolego();
    }

    public double getPercentualCondicionamento() {

        return condicionamentoAtual
                / atributos.getCondicionamento();
    }

//consumo
    private void gastarEnergia(
        double quantidade
) {

    if (quantidade <= 0) {
        return;
    }

    folegoAtual -= quantidade;

    if (folegoAtual < 0) {

        folegoAtual = 0;
    }
}
    
    private void gastarCondicionamento(
        double quantidade
) {

    if (quantidade <= 0) {
        return;
    }

    condicionamentoAtual -= quantidade;

    if (condicionamentoAtual < 0) {

        condicionamentoAtual = 0;
    }
}

    private void recuperarFolego() {

        if (correndo || pulando) {
            return;
        }

        double taxa
                = 0.015
                + atributos.getRecuperacao()
                * 0.00035;

        folegoAtual += taxa;

        if (folegoAtual
                > atributos.getFolego()) {

            folegoAtual
                    = atributos.getFolego();
        }
    }

    public boolean isSemCondicionamento() {

        return condicionamentoAtual <= 0;
    }

    public AtributosLutador getAtributos() {

        return atributos;
    }

    public double getFolegoAtual() {

        return folegoAtual;
    }

    public double getCondicionamentoAtual() {

        return condicionamentoAtual;
    }
public boolean isSemFolego() {

    return folegoAtual
            <= LIMITE_FOLEGO_ZERO;
}
}
