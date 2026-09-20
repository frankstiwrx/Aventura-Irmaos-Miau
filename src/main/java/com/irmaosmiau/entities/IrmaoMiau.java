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

    private boolean respirando = false;
    
    // =============================
// ROLAMENTO
// =============================

private boolean rolando = false;

private Direcao direcaoRolamento = null;

private int framesRolamento = 0;

private static final int DURACAO_ROLAMENTO = 22;

private static final double CUSTO_ROLAMENTO = 6.0;

private static final double VELOCIDADE_ROLAMENTO = 7.0;

    private double faseRespiracao = 0.0;

    private static final double CUSTO_PULO_ALTO = 8.0;
    private static final double CUSTO_PULO_CURTO = 3.5;

    private static final double VELOCIDADE_CANSADO = 0.50;

    private static final double CUSTO_MOVIMENTO_CANSADO = 0.025;

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
// ESTADO CORPORAL
// =============================

public enum EstadoCorporal {
    EM_PE,
    AGACHADO,
    DEITADO
}

private EstadoCorporal estadoCorporal =
        EstadoCorporal.EM_PE;
    
    // =============================
    // MOVIMENTO
    // =============================
    private boolean pulando = false;
    private boolean agachado = false;
    private boolean andando = false;

    private double alturaPulo = 0;
    private double velocidadePulo = 0;

    private double faseAnimacao = 0;

    /*
 * Guarda a parte decimal do movimento.
 *
 * O personagem continua sendo desenhado
 * em coordenadas inteiras, mas não perdemos
 * frações de velocidade entre os frames.
     */
    private double restoMovimentoX = 0.0;
    private double restoMovimentoY = 0.0;

    // =============================
    // SISTEMA DE ROLAMENTO
    // =============================
    
    public void iniciarRolamento(
        Direcao direcao
) {

    /*
     * Não existe rolamento sem direção.
     */
    if (direcao == null) {
        return;
    }

    /*
     * Não pode iniciar outro enquanto
     * já está rolando.
     */
    if (rolando) {
        return;
    }

    /*
     * Não rola no ar.
     */
    if (pulando) {
        return;
    }
    
    if (isDeitado()) {
    return;
}

    /*
     * Precisa possuir fôlego suficiente.
     */
    if (folegoAtual < CUSTO_ROLAMENTO) {
        return;
    }

    /*
     * O rolamento interrompe outras ações.
     */
    correndo = false;
    respirando = false;
    agachado = false;

    ultimaAtividade =
        System.currentTimeMillis();

faseRespiracao = 0;
    
    gastarEnergia(
            CUSTO_ROLAMENTO
    );

    direcaoRolamento = direcao;

    framesRolamento = 0;

    rolando = true;
}
    
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
        if (isSemFolego()
                || andando
                || correndo
                || pulando) {

            return;
        }

        double taxa
                = 0.002
                + atributos.getRecuperacao()
                * 0.000025;

        condicionamentoAtual += taxa;

        if (condicionamentoAtual
                > atributos.getCondicionamento()) {

            condicionamentoAtual
                    = atributos.getCondicionamento();
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

    protected int getOscilacaoRespiracao() {

    if (!respirando) {

        return 0;
    }

    return (int) Math.round(
            Math.sin(
                    faseRespiracao
            ) * 2
    );
}
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

        agachado =
        querAgachar
        && !pulando
        && !isDeitado();
        
        if (!isDeitado()) {

    if (agachado) {

        estadoCorporal =
                EstadoCorporal.AGACHADO;

    } else {

        estadoCorporal =
                EstadoCorporal.EM_PE;
    }
}
        
        if (rolando) {

    andando = false;
    agachado = false;
    correndo = false;
    respirando = false;

    atualizarRolamento();

    /*
     * O rolamento também pode alterar
     * a posição vertical.
     *
     * Precisamos sincronizar a posição
     * visual antes de sair do atualizar().
     */
    y = yBase;

    atualizarPiscar();

    return;
}
        
        if (
        agachado
        || pulando
        || respirando
        || isSemFolego()
) {

            if (respirando) {

    faseRespiracao += 0.10;

} else {

    faseRespiracao = 0;
}
            
    correndo = false;
}

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
        respirando
        && !pulando
) {

    recuperarFolegoRespirando();

    recuperarCondicionamentoRespirando();

} else if (
        correndo
        && andando
        && !isSemFolego()
) {

    gastarEnergia(
            0.12
    );

} else if (
        andando
        && isSemFolego()
) {

    gastarCondicionamento(
            0.025
    );

} else {

    recuperarFolego();

    recuperarCondicionamentoLeve();
}

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
            double quantidade
    ) {

        int yMinimo
                = yLinha1;

        int yMaximo
                = yLinha1
                + (TOTAL_LINHAS - 1)
                * ESPACO_ENTRE_LINHAS;

        // Guarda a parte decimal
        restoMovimentoY += quantidade;

        int deslocamentoInteiro
                = (int) restoMovimentoY;

        yBase += deslocamentoInteiro;

        restoMovimentoY
                -= deslocamentoInteiro;

        // Não deixa passar da Linha 1
        if (yBase < yMinimo) {

            yBase = yMinimo;

            /*
         * Evita guardar movimento
         * contra a parede invisível.
             */
            restoMovimentoY = 0;
        }

        // Não deixa passar da Linha 5
        if (yBase > yMaximo) {

            yBase = yMaximo;

            restoMovimentoY = 0;
        }

        // Descobre qual das 5 linhas
        // está mais próxima.
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
            double quantidade
    ) {

        restoMovimentoX += quantidade;

        int deslocamentoInteiro
                = (int) restoMovimentoX;

        x += deslocamentoInteiro;

        restoMovimentoX
                -= deslocamentoInteiro;
    }

    // =============================
    // ROLAMENTO
    // =============================
    
    private void atualizarRolamento() {

    if (!rolando) {
        return;
    }

    double velocidade =
            VELOCIDADE_ROLAMENTO;

    /*
     * Na diagonal dividimos a velocidade
     * para não fazer a diagonal percorrer
     * uma distância maior.
     *
     * 1 / sqrt(2) ≈ 0.707
     */
    double diagonal =
            velocidade * 0.7071;

    switch (direcaoRolamento) {

        case CIMA:
            moverVertical(
                    -velocidade
            );
            break;

        case BAIXO:
            moverVertical(
                    velocidade
            );
            break;

        case ESQUERDA:
            moverX(
                    -velocidade
            );
            break;

        case DIREITA:
            moverX(
                    velocidade
            );
            break;

        case CIMA_ESQUERDA:
            moverX(
                    -diagonal
            );

            moverVertical(
                    -diagonal
            );
            break;

        case CIMA_DIREITA:
            moverX(
                    diagonal
            );

            moverVertical(
                    -diagonal
            );
            break;

        case BAIXO_ESQUERDA:
            moverX(
                    -diagonal
            );

            moverVertical(
                    diagonal
            );
            break;

        case BAIXO_DIREITA:
            moverX(
                    diagonal
            );

            moverVertical(
                    diagonal
            );
            break;
    }

    framesRolamento++;

    if (
            framesRolamento
            >= DURACAO_ROLAMENTO
    ) {

        finalizarRolamento();
    }
    
    
    
}
 // Finalizar Rolamento   
    private void finalizarRolamento() {

    rolando = false;

    direcaoRolamento = null;

    framesRolamento = 0;
}
    
    // =============================
// DEITAR / LEVANTAR
// =============================

public void alternarDeitado() {

    /*
     * Não pode mudar de estado corporal
     * durante ações temporárias.
     */
    if (pulando || rolando) {
        return;
    }

    /*
     * Se já está deitado, levanta.
     */
    if (estadoCorporal
            == EstadoCorporal.DEITADO) {

        estadoCorporal =
                EstadoCorporal.EM_PE;

        ultimaAtividade =
                System.currentTimeMillis();

        return;
    }

    /*
     * Entra no estado deitado.
     */
    estadoCorporal =
            EstadoCorporal.DEITADO;

    correndo = false;
    respirando = false;
    agachado = false;

    faseRespiracao = 0;

    ultimaAtividade =
            System.currentTimeMillis();
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

        if (pulando
        || agachado
        || respirando
        || rolando
                || isDeitado()
        || condicionamentoAtual <= 0) {

    return;
}

        /*
     * Sem fôlego suficiente para
     * o pulo alto, ele tenta apenas
     * o pulo curto.
         */
        if (folegoAtual
                < CUSTO_PULO_ALTO) {

            pularCurto();

            return;
        }

        ultimaAtividade
                = System.currentTimeMillis();

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
        && !respirando
        && !rolando
                && !isDeitado()
        && !isSemFolego()
        && condicionamentoAtual > 0;
    }

    private double calcularVelocidadeNormal() {

    return 3.2
            + (
                    atributos.getVelocidade()
                    / 100.0
            ) * 1.6;
}
    public boolean isCorrendo() {

        return correndo;
    }

    // =============================
    // PULO CURTO
    // =============================
    public void pularCurto() {

        if (pulando
                || agachado
                || respirando
                || rolando
                || isDeitado()
                || condicionamentoAtual <= 0) {

            return;
        }

        ultimaAtividade
                = System.currentTimeMillis();

        pulando = true;

        alturaPulo = 1;

        if (folegoAtual
                >= CUSTO_PULO_CURTO) {

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

    double velocidade =
            calcularVelocidadeNormal();

    /*
     * Sem fôlego:
     * metade da velocidade.
     */
    if (isSemFolego()) {

        velocidade *= 0.50;
    }

    return velocidade;
}
    
    public double getVelocidadeRespirando() {

    return calcularVelocidadeNormal()
            * 0.15;
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

    public double getVelocidadeRastejando() {

    /*
     * 7% da velocidade normal.
     *
     * O movimento decimal permite
     * velocidades tão pequenas sem
     * travar o personagem.
     */
    return calcularVelocidadeNormal()
            * 0.07;
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
                + deslocamentoIdle
         + getOscilacaoRespiracao();
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
    
    public boolean isDeitado() {

    return estadoCorporal
            == EstadoCorporal.DEITADO;
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
    
    protected double getProgressoRolamento() {

    if (!rolando) {
        return 0.0;
    }

    return Math.min(
            1.0,
            (double) framesRolamento
            / DURACAO_ROLAMENTO
    );
}
    
    protected double getAnguloRolamento() {

    if (!rolando) {
        return 0.0;
    }

    return getProgressoRolamento()
            * Math.PI
            * 2.0;
}
    
    public boolean isRolando() {

    return rolando;
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

    if (
            correndo
            || pulando
            || respirando
    ) {

        return;
    }

    folegoAtual +=
            calcularTaxaRecuperacaoFolego();

    if (
            folegoAtual
            > atributos.getFolego()
    ) {

        folegoAtual =
                atributos.getFolego();
    }
}

private void recuperarFolegoRespirando() {

    if (pulando) {

        return;
    }

    /*
     * Respirar recupera cerca de
     * quatro vezes mais rápido que
     * simplesmente ficar parado.
     */
    double taxa =
            calcularTaxaRecuperacaoFolego()
            * 15.0;

    folegoAtual += taxa;

    if (
            folegoAtual
            > atributos.getFolego()
    ) {

        folegoAtual =
                atributos.getFolego();
    }
}

private void recuperarCondicionamentoRespirando() {

    /*
     * Precisa possuir pelo menos
     * 25% do fôlego máximo.
     *
     * Primeiro recuperamos o ar,
     * depois começamos a recuperar
     * o condicionamento.
     */
    double minimoFolego =
            atributos.getFolego()
            * 0.25;

    if (
            folegoAtual
            < minimoFolego
    ) {

        return;
    }

    double taxaBase =
            0.002
            + atributos.getRecuperacao()
            * 0.000025;

    /*
     * Respirar também melhora um pouco
     * a recuperação física.
     */
    condicionamentoAtual +=
            taxaBase * 2.5;

    if (
            condicionamentoAtual
            > atributos.getCondicionamento()
    ) {

        condicionamentoAtual =
                atributos.getCondicionamento();
    }
}
    
    private double calcularTaxaRecuperacaoFolego() {

    return 0.008
            + atributos.getRecuperacao()
            * 0.00020;
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

    public void setRespirando(
            boolean respirando
    ) {

        this.respirando =
        respirando
        && !pulando
        && !rolando
                && !isDeitado()
        && condicionamentoAtual > 0;

        /*
     * Respirando não pode correr.
         */
        if (this.respirando) {

            correndo = false;
        }
    }

    public boolean isRespirando() {

        return respirando;
    }

}
