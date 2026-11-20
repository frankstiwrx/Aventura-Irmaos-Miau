package com.irmaosmiau.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.Arrays;

public abstract class IrmaoMiau {

    // =============================================================
    // MAPA RÁPIDO DA CLASSE
    // =============================================================
    /*
     * Esta é a classe-base dos Irmãos Miau. Para localizar rápido:
     *
     * RIG / PIVÔS            -> cintura e peito
     * ESTADOS                -> movimento e estado corporal
     * ROLAMENTO              -> início, atualização e direção
     * SPRAWL                 -> início e atualização
     * GAME LOOP              -> atualizar(...)
     * MOVIMENTO              -> moverX / moverVertical / velocidades
     * CHÃO                   -> deitar / levantar
     * PULO                   -> pulo alto e curto
     * ENERGIA                -> fôlego e condicionamento
     * IDLE / PISCAR          -> comportamento visual parado
     * DESENHO BASE           -> cabeça, patas e preparação gráfica
     * GETTERS / ESTADOS      -> informações usadas por outras classes
     *
     * Regra de manutenção: mecânica fica aqui; desenho específico e
     * dados das animações ficam em IrmaoMiauBranco/IrmaoMiauPreto.
     */

    protected int x;
    protected int y;

    protected double escala;

    // =============================
    // EIXO CENTRAL DA CINTURA
    // =============================
    /*
     * A cintura é o novo "root" do rig.
     *
     * As animações antigas continuam usando o sistema atual.
     * As próximas podem usar este ponto como referência
     * para translação e rotação do corpo inteiro.
     *
     * Estes valores são locais (antes da escala).
     * Um futuro personagem com proporções diferentes pode
     * sobrescrever estes métodos.
     */
    public double getEixoCinturaLocalX() {

        return 36.0;
    }

    public double getEixoCinturaLocalY() {

        return 157.0;
    }

    public double getEixoCinturaX() {

        return x
                + getEixoCinturaLocalX()
                * escala;
    }

    public double getEixoCinturaY() {

        return getYVisual()
                + getEixoCinturaLocalY()
                * escala;
    }

    // =============================
    // EIXO DO PEITO / TRONCO SUPERIOR
    // =============================
    /*
     * Este é o segundo pivô do rig.
     *
     * A cintura continua sendo o root geral.
     * O peito permite dobrar somente o tronco superior,
     * levando junto cabeça e braços.
     *
     * As pernas continuam ligadas à parte inferior.
     */
    public double getEixoPeitoLocalX() {

        return 36.0;
    }

    public double getEixoPeitoLocalY() {

        return 150.0;
    }

    public double getEixoPeitoX() {

        return x
                + getEixoPeitoLocalX()
                * escala;
    }

    public double getEixoPeitoY() {

        return getYVisual()
                + getEixoPeitoLocalY()
                * escala;
    }

    protected Color corPelo;
    protected Color corKimono;
    protected Color corRosto;

    protected String nome;
    protected String estilo;

    protected AtributosLutador atributos;

    private double condicionamentoAtual;
    private double folegoAtual;

    // =============================
    // POSE EXTERNA DE COMBATE
    // =============================
    /*
     * Ponte entre o motor de combate e os renderers dos personagens.
     *
     * Nesta etapa apenas ARMAZENAMOS a pose externa. Branco/Preto ainda
     * nao a desenham; essa prioridade visual sera ligada na etapa 8D.
     *
     * O formato continua sendo o padrao de 30 valores do MiauStudio.
     */
    public static final int TAMANHO_POSE_MIAUSTUDIO = 30;

    private int[] poseCombateExterna;
    private double deslocamentoVisualCombateX;
    private double deslocamentoVisualCombateY;

    /*
     * Estado separado da pose visual. Assim o motor podera bloquear o
     * controle durante uma animacao sem transformar o renderer em regra.
     */
    private boolean bloqueadoPorAnimacaoCombate = false;

    /*
     * Entrega 5A: efeitos temporarios desta instancia (ex.: DESEQUILIBRIO).
     * Sem efeito ativo o multiplicador e exatamente 1.0 (formulas inalteradas).
     */
    private final EfeitosTemporarios efeitos = new EfeitosTemporarios();

    private boolean correndo = false;

    private boolean respirando = false;
    
    // =============================
// ROLAMENTO
// =============================

private boolean rolando = false;

private Direcao direcaoRolamento = null;

private boolean viradoParaDireita = true;

private boolean viradoParaDireitaNoInicioRolamento = true;

private int framesRolamento = 0;

private static final int DURACAO_ROLAMENTO = 22;

/* 5O.1: o rolamento tecnico custa menos que a estrela (5,0) e o mortal (7,0). Antes: 6,0. */
private static final double CUSTO_ROLAMENTO = 4.0;

private static final double VELOCIDADE_ROLAMENTO = 7.0;

/**
 * 5O.1: multiplicador CENTRAL da velocidade do rolamento a partir de deitado
 * (rolar do chao e mais dificil): a mesma distancia, em mais tempo. De joelhos
 * e em pe = 1,0 (a velocidade de sempre).
 */
public static final double MULTIPLICADOR_ROLAMENTO_DEITADO = 0.6;

/* Duracao e velocidade do rolamento EM ANDAMENTO (decididas no inicio). */
private int duracaoRolamentoAtual = DURACAO_ROLAMENTO;
private double velocidadeRolamentoAtual = VELOCIDADE_ROLAMENTO;
private EstadoCorporal posturaDoRolamento = EstadoCorporal.EM_PE;

    private double faseRespiracao = 0.0;

    private static final double CUSTO_PULO_ALTO = 8.0;
    private static final double CUSTO_PULO_CURTO = 3.5;
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
// SPRAWL
// =============================

private boolean fazendoSprawl = false;

private int framesSprawl = 0;

/*
 * O jogo roda aproximadamente a 60 FPS.
 * 26 frames ≈ 0,42 segundo.
 */
private static final int DURACAO_SPRAWL = 26;

private static final double CUSTO_SPRAWL = 4.0;
    
    // =============================
// ESTADO CORPORAL
// =============================

public enum EstadoCorporal {
    EM_PE,
    AGACHADO,
    DEITADO,
    /** Entrega 5J: de joelhos (postura de inicio do jiu-jitsu). */
    AJOELHADO
}

private EstadoCorporal estadoCorporal =
        EstadoCorporal.EM_PE;

    // =============================
    // ENTREGA 5K: CICLO DE POSTURAS E ESFORCO
    // =============================
    /* Ajoelhado: true = veio de deitado (o proximo clique simples levanta). */
    private boolean cicloSubindo;
    /* Transicao em andamento (so visual: a postura logica ja mudou). */
    private TransicaoPostura transicaoAtiva;
    private int ticksTransicao;
    /* Distancia que o PROPRIO lutador andou neste tick (moverX/moverVertical). */
    private double deslocamentoProprioTick;
    /* Houve esforco neste tick (arrasto, transicao): sem recuperacao passiva. */
    private boolean esforcoNesteTick;
    private double ultimoCustoFolego;
    /* Entrega 5L: ticks seguidos com o comando de agachar/abaixar apertado (0 = solto). */
    private int ticksAbaixando;
    private String ultimoCustoMotivo = "";

    
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
    if (rolando || fazendoSprawl) {
        return;
    }

    /*
     * Não rola no ar, mudando de postura, em acrobacia nem preso numa pegada
     * (o GamePanel ja barra o receptor preso).
     * 5O.1: de joelhos e deitado AGORA rolam (R3 contextual); a postura fica.
     */
    if (pulando || transicaoAtiva != null || acrobacia != null || bloqueadoPorAnimacaoCombate) {
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

/*
 * Guardamos para que lado o Miau estava olhando
 * NO MOMENTO em que iniciou o rolamento.
 *
 * Assim, mesmo se os dois cruzarem de posição
 * durante a animação, ela não muda no meio.
 */
viradoParaDireitaNoInicioRolamento =
        viradoParaDireita;

direcaoRolamento = direcao;

framesRolamento = 0;

posturaDoRolamento = estadoCorporal == EstadoCorporal.AGACHADO ? EstadoCorporal.EM_PE : estadoCorporal;
double mult = posturaDoRolamento == EstadoCorporal.DEITADO ? MULTIPLICADOR_ROLAMENTO_DEITADO : 1.0;
duracaoRolamentoAtual = (int) Math.round(DURACAO_ROLAMENTO / mult);
// mesma distancia de sempre (22 x 7 px), so mais devagar deitado
velocidadeRolamentoAtual = VELOCIDADE_ROLAMENTO * DURACAO_ROLAMENTO / (double) duracaoRolamentoAtual;
defesa.interromper();

rolando = true;
}

/** 5O.1: custo do rolamento tecnico (o mesmo em qualquer postura). */
public static double custoRolamento() {
    return CUSTO_ROLAMENTO;
}

/** 5O.1: duracao do rolamento em andamento (ticks): maior a partir de deitado. */
public int getDuracaoRolamentoAtual() {
    return duracaoRolamentoAtual;
}

/** 5O.1: duracao do rolamento a partir de uma postura (ticks). */
public static int duracaoRolamento(EstadoCorporal postura) {
    return postura == EstadoCorporal.DEITADO
            ? (int) Math.round(DURACAO_ROLAMENTO / MULTIPLICADOR_ROLAMENTO_DEITADO) : DURACAO_ROLAMENTO;
}

/** 5O.1: postura em que o rolamento em andamento comecou (e termina). */
public EstadoCorporal getPosturaDoRolamento() {
    return posturaDoRolamento;
}
    
    // =============================
// SISTEMA DE SPRAWL
// =============================

public void iniciarSprawl() {

    /*
     * Não inicia outro sprawl enquanto
     * já estiver executando um.
     */
    if (fazendoSprawl) {
        return;
    }

    /*
     * Não pode fazer sprawl durante
     * outras ações incompatíveis.
     */
    if (pulando
            || rolando
            || acrobacia != null
            || isDeitado()
            || isAjoelhado()
            || transicaoAtiva != null
            || recargaSprawl > 0) {     // 5O: sem sprawl em sequencia

        return;
    }

    /*
     * Precisa ter fôlego suficiente.
     */
    if (folegoAtual < CUSTO_SPRAWL) {
        return;
    }

    /*
     * Sprawl interrompe outras ações.
     */
    correndo = false;
    respirando = false;
    agachado = false;

    faseRespiracao = 0;

    ultimaAtividade =
            System.currentTimeMillis();

    gastarEnergia(
            CUSTO_SPRAWL
    );

    framesSprawl = 0;

    fazendoSprawl = true;
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

    // =============================
    // ENERGIA - RECUPERAÇÃO LEVE
    // =============================
    private void recuperarCondicionamentoLeve() {

        if (vinculadoEmPegada || vinculadoNoSolo) {
            return;                                     // 5O.3
        }

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
        this.atributosBase = atributos;                // 5Q.1: a faixa branca (referencia imutavel)
        this.corKimonoPadrao = corKimono;

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

        // Entrega 5A: duracao dos efeitos avanca no loop logico.
        efeitos.atualizar();
        manterRecursosInfinitos();                       // 5Q.4: Modo Desenvolvedor
        // 5O: recargas do sprawl e das acrobacias; guarda (entrada/saida).
        if (recargaSprawl > 0) {
            recargaSprawl--;
        }
        if (recargaAcrobacia > 0) {
            recargaAcrobacia--;
        }
        defesa.atualizar();

        // Entrega 5K: distancia andada e esforco deste tick (zerados para o proximo).
        double deslocouNesteTick = deslocamentoProprioTick;
        deslocamentoProprioTick = 0;
        boolean esforcou = esforcoNesteTick;
        esforcoNesteTick = false;
        if (transicaoAtiva != null && ++ticksTransicao >= transicaoAtiva.getDuracaoTicks()) {
            transicaoAtiva = null;
        }

        // =========================
        // ESTADO ATUAL
        // =========================
        andando = estaAndando;

        // Entrega 5L: o comando de abaixar conta desde o primeiro tick (de pe ou de joelhos).
        ticksAbaixando = querAgachar && !pulando && !isDeitado() ? ticksAbaixando + 1 : 0;

        agachado =
        querAgachar
        && !pulando
        && !isDeitado()
        && !isAjoelhado();
        
        if (!isDeitado() && !isAjoelhado()) {

    if (agachado) {

        estadoCorporal =
                EstadoCorporal.AGACHADO;

    } else {

        estadoCorporal =
                EstadoCorporal.EM_PE;
    }
}
        
        // 5O: acrobacia em andamento (como o rolamento: o corpo segue a tecnica).
        if (acrobacia != null) {
            andando = false;
            agachado = false;
            correndo = false;
            respirando = false;
            atualizarAcrobacia();
            deslocamentoProprioTick = 0;      // custo proprio (nao e caminhada)
            y = yBase;
            atualizarPiscar();
            return;
        }

        if (rolando) {

    andando = false;
    agachado = false;
    correndo = false;
    respirando = false;

    atualizarRolamento();
    deslocamentoProprioTick = 0;   // 5K: o rolamento tem custo proprio (nao e caminhada)

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
        if (fazendoSprawl) {

    andando = false;
    agachado = false;
    correndo = false;
    respirando = false;

    atualizarSprawl();

    /*
     * Sprawl não muda de linha nem
     * desloca o personagem sozinho.
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

} else if (deslocouNesteTick > 0) {

    // Entrega 5K: andar (em pe, de joelhos ou deitado) gasta pouco e nao recupera.
    gastarEnergia(CustosFolego.caminhada(deslocouNesteTick, calcularVelocidadeSemEfeitos(), estadoCorporal)
            * (defesa.isAtiva() ? CustosFolego.FATOR_DISTANCIA_GUARDA : 1.0));   // 5O.1: guarda andando

} else if (esforcou) {

    // Entrega 5K: arrasto/transicao neste tick: sem recuperacao passiva.

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

        double antesY = yBase + restoMovimentoY;       // 5K: distancia efetiva
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

        deslocamentoProprioTick += Math.abs(yBase + restoMovimentoY - antesY);

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
        // 5K: a caminhada cobra pelo que o lutador realmente andou.
        deslocamentoProprioTick += Math.abs(quantidade);
    }

    // =============================
    // ROLAMENTO
    // =============================
    
    private void atualizarRolamento() {

    if (!rolando) {
        return;
    }

    double velocidade =
            velocidadeRolamentoAtual;

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
            >= duracaoRolamentoAtual
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
    
    private void atualizarSprawl() {

    if (!fazendoSprawl) {
        return;
    }

    framesSprawl++;

    if (framesSprawl >= DURACAO_SPRAWL) {

        finalizarSprawl();
    }
}

private void finalizarSprawl() {

    fazendoSprawl = false;

    framesSprawl = 0;

    recargaSprawl = RECARGA_SPRAWL_TICKS;   // 5O
}
    
    // =============================
// DEITAR / LEVANTAR
// =============================

public void alternarDeitado() {

    /*
     * Não pode mudar de estado corporal
     * durante ações temporárias.
     */
    if (pulando || rolando || fazendoSprawl) {
        return;
    }

    /*
     * Se já está deitado (ou de joelhos, 5J), levanta.
     */
    if (estadoCorporal
            == EstadoCorporal.DEITADO
            || estadoCorporal == EstadoCorporal.AJOELHADO) {

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
                || fazendoSprawl
                || isDeitado()
                || isAjoelhado()
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
                && !isAjoelhado()
        && !isSemFolego()
        && condicionamentoAtual > 0;
    }

    private double calcularVelocidadeNormal() {

    // Formula original intacta; o fator de efeitos e 1.0 sem efeitos ativos.
    return (3.2
            + (
                    atributos.getVelocidade()
                    / 100.0
            ) * 1.6)
            * efeitos.getFatorVelocidade();
}

    /**
     * Entrega 5B: deslocamento horizontal inteiro feito pelo motor de combate
     * (deslocamento conjunto da pegada). Nao usa nem altera o resto decimal
     * do movimento normal; o desenho continua relativo a x.
     */
    public void deslocarFisicamente(int dx) {
        x += dx;
    }

    /** Efeitos temporarios desta instancia (Entrega 5A). */
    public EfeitosTemporarios getEfeitos() {
        return efeitos;
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
                || fazendoSprawl
                || isDeitado()
                || isAjoelhado()
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

    // =============================
    // VELOCIDADES DE MOVIMENTO
    // =============================
    public double getVelocidadeBase() {

    double velocidade =
            calcularVelocidadeNormal();

    /*
     * Sem fôlego:
     * metade da velocidade.
     */
    if (isSemFolego()) {

        velocidade *= RegrasVelocidade.FATOR_SEM_FOLEGO;
    }

    return velocidade;
}
    
    public double getVelocidadeRespirando() {

    // 5O.2: depende da postura (em pe 0,15 como sempre; de joelhos e deitado mais devagar).
    return calcularVelocidadeNormal()
            * CustosFolego.velocidadeRespirando(posturaBase());
}

    /** 5O.2: postura para as regras (agachado conta como em pe). */
    private EstadoCorporal posturaBase() {
        return estadoCorporal == EstadoCorporal.AGACHADO ? EstadoCorporal.EM_PE : estadoCorporal;
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
                * RegrasVelocidade.FATOR_CORRIDA;
    }

    public double getVelocidadeAgachado() {

        return getVelocidadeBase()
                * RegrasVelocidade.FATOR_AGACHADO;
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
            * RegrasVelocidade.FATOR_DEITADO;
}
    
    // =============================
    // INFORMAÇÕES DO IDLE
    // =============================
    protected int getPoseIdle() {

        return poseIdle;
    }

    // =============================
    // 5O: DEFESAS, SPRAWL E ACROBACIAS
    // =============================
    /** Recarga depois de um sprawl (sem repeticao infinita): ~0,5 s. */
    public static final int RECARGA_SPRAWL_TICKS = 30;
    /** Recarga depois de uma acrobacia: ~0,4 s. */
    public static final int RECARGA_ACROBACIA_TICKS = 24;

    private final DefesaCorporal defesa = new DefesaCorporal();
    private int recargaSprawl;
    private int recargaAcrobacia;
    private TipoAcrobacia acrobacia;
    private int framesAcrobacia;
    private double vxAcrobacia;
    private double vyAcrobacia;
    private boolean viradoNoInicioAcrobacia;

    /** Estado mecanico das guardas (consultado pelos futuros golpes). */
    public DefesaCorporal getDefesa() {
        return defesa;
    }

    public int getRecargaSprawl() {
        return recargaSprawl;
    }

    /** Fase do sprawl pelo progresso: ENTRADA, PROTECAO, RECUPERACAO (null fora dele). */
    public String getFaseSprawl() {
        if (!fazendoSprawl) {
            return null;
        }
        double p = getProgressoSprawl();
        return p < 0.25 ? "ENTRADA" : p < 0.62 ? "PROTECAO" : "RECUPERACAO";
    }

    /** Por que o sprawl nao pode comecar agora (null = pode). */
    public String motivoSemSprawl() {
        if (isDeitado() || isAjoelhado()) {
            return "SÓ EM PÉ";
        }
        if (transicaoAtiva != null) {
            return "MUDANDO DE POSTURA";
        }
        if (fazendoSprawl || rolando || pulando || acrobacia != null) {
            return "EM MOVIMENTO";
        }
        if (recargaSprawl > 0) {
            return "EM RECUPERACAO";
        }
        if (folegoAtual < CUSTO_SPRAWL) {
            return "SEM FOLEGO";
        }
        return null;
    }

    /** Custo do sprawl (para o HUD e os testes). */
    public static double custoSprawl() {
        return CUSTO_SPRAWL;
    }

    /** 0..1 do sprawl (publico para a animacao experimental). */
    public double progressoSprawl() {
        return getProgressoSprawl();
    }

    /** 0..1 do rolamento (publico para a animacao experimental; a regra nao muda). */
    public double progressoRolamento() {
        return getProgressoRolamento();
    }

    public Direcao getDirecaoRolamento() {
        return rolando ? direcaoRolamento : null;
    }

    /** Lado para onde olhava quando o rolamento comecou (a animacao nao vira no meio). */
    public boolean isViradoParaDireitaNoInicioRolamento() {
        return viradoParaDireitaNoInicioRolamento;
    }

    // ---------------- acrobacias
    /**
     * Inicia a acrobacia (Y + direcao). So em pe, livre, sem outra acao do
     * corpo em andamento, fora da recarga e com folego. O deslocamento e
     * decidido aqui (pela orientacao no inicio) e anda tick a tick com
     * moverX / moverVertical (as cinco linhas limitam a profundidade); a borda
     * da arena e o adversario sao respeitados pelo GamePanel.
     *
     * @return true se comecou
     */
    public boolean iniciarAcrobacia(TipoAcrobacia tipo) {
        if (tipo == null || motivoSemAcrobacia(tipo) != null) {
            return false;
        }
        defesa.interromper();
        correndo = false;
        respirando = false;
        agachado = false;
        gastarFolegoEsforco(tipo.getCusto(), "acrobacia");
        acrobacia = tipo;
        framesAcrobacia = 0;
        viradoNoInicioAcrobacia = viradoParaDireita;
        double frente = viradoParaDireita ? 1 : -1;
        vxAcrobacia = tipo.getDeslocamentoFrentePx() * frente / tipo.getDuracaoTicks();
        vyAcrobacia = tipo.getDeslocamentoProfundidadePx(viradoParaDireita) / (double) tipo.getDuracaoTicks();
        ultimaAtividade = System.currentTimeMillis();
        return true;
    }

    /** Por que a acrobacia nao pode comecar agora (null = pode). */
    public String motivoSemAcrobacia(TipoAcrobacia tipo) {
        if (isDeitado() || isAjoelhado()) {
            return "SÓ EM PÉ";
        }
        if (transicaoAtiva != null) {
            return "MUDANDO DE POSTURA";
        }
        if (acrobacia != null || rolando || fazendoSprawl || pulando) {
            return "EM MOVIMENTO";
        }
        if (recargaAcrobacia > 0) {
            return "EM RECUPERACAO";
        }
        if (folegoAtual < tipo.getCusto()) {
            return "SEM FOLEGO";
        }
        return null;
    }

    private void atualizarAcrobacia() {
        moverX(vxAcrobacia);
        if (vyAcrobacia != 0) {
            moverVertical(vyAcrobacia);
        }
        if (++framesAcrobacia >= acrobacia.getDuracaoTicks()) {
            acrobacia = null;
            framesAcrobacia = 0;
            recargaAcrobacia = RECARGA_ACROBACIA_TICKS;
        }
    }

    public TipoAcrobacia getAcrobacia() {
        return acrobacia;
    }

    public boolean isEmAcrobacia() {
        return acrobacia != null;
    }

    public double progressoAcrobacia() {
        return acrobacia == null ? 0.0 : Math.min(1.0, framesAcrobacia / (double) acrobacia.getDuracaoTicks());
    }

    public boolean isViradoParaDireitaNoInicioAcrobacia() {
        return viradoNoInicioAcrobacia;
    }

    public int getRecargaAcrobacia() {
        return recargaAcrobacia;
    }

    /** O GamePanel parou o avanco contra o adversario: a acrobacia termina no lugar. */
    public void pararAvancoAcrobacia() {
        vxAcrobacia = 0;
    }

    // =============================
    // 5N: RIG SEMANTICO, PROFUNDIDADE CONJUNTA E ATIVIDADE NA PEGADA
    // =============================
    /** Rig semantico do lutador (as reacoes compartilhadas do receptor usam o mesmo). */
    public static final String RIG_PADRAO = "miau_v1";

    /**
     * 5N: o esqueleto de 30 valores do MiauStudio. Um personagem futuro com
     * outro esqueleto devolve outro nome e as reacoes compartilhadas caem no
     * fallback (a pose do motor fica como esta).
     */
    public String getRigAnimacao() {
        return RIG_PADRAO;
    }

    /** 5N: cabe na faixa das cinco linhas se a profundidade mudar dy px? */
    public boolean podeDeslocarProfundidade(int dy) {
        int novo = yBase + dy;
        return novo >= yLinha1 && novo <= yLinha1 + (TOTAL_LINHAS - 1) * ESPACO_ENTRE_LINHAS;
    }

    /**
     * 5N: o arrasto da dupla muda a profundidade (como moverVertical, mas
     * inteiro e sem custo proprio: quem paga e o aplicador, no motor).
     * Quem chama confere {@link #podeDeslocarProfundidade(int)} antes.
     */
    public void deslocarProfundidadeFisicamente(int dy) {
        yBase += dy;
        y += dy;
        restoMovimentoY = 0;
        linhaAtual = (int) Math.round((double) (yBase - yLinha1) / ESPACO_ENTRE_LINHAS);
    }

    /** 5O: devolve o lutador para dentro da arena (sem guardar resto de movimento contra a parede). */
    public void encostarNaBorda(int dx) {
        x += dx;
        restoMovimentoX = 0;
    }

    /** 5N: segurar ou ser segurado conta como atividade (o taunt nao aparece logo depois). */
    public void registrarAtividadeDeCombate() {
        ultimaAtividade = System.currentTimeMillis();
    }

    // =============================
    // 5M.3: POSE DE APRESENTACAO (MENUS)
    // =============================
    private boolean poseDeApresentacao;

    /**
     * 5M.3: deixa o lutador na postura neutra fixa das telas de menu: sem
     * idle de combate e sem taunt. Quem usa nao chama atualizar(...) (que
     * avanca o relogio de idle/taunt); chama atualizarApresentacao(), que so
     * cuida da piscada. A luta nunca chama isto.
     */
    public void entrarEmPoseDeApresentacao() {
        poseDeApresentacao = true;
        estadoIdle = EstadoIdle.NENHUM;
        poseIdle = 0;
    }

    public boolean isPoseDeApresentacao() {
        return poseDeApresentacao;
    }

    /** 5M.3 (testes): finge que o lutador esta parado ha mais tempo (para chegar ao taunt). */
    void envelhecerAtividadeParaTestes(long ms) {
        ultimaAtividade -= ms;
    }

    /** 5M.3: idle de combate em pe ativo (somente leitura, para testes e diagnostico). */
    public boolean estaEmIdleDeCombate() {
        return estadoIdle == EstadoIdle.EM_PE || estadoIdle == EstadoIdle.AGACHADO;
    }

    /** 5M.3: um passo de tempo na apresentacao: so a piscada (o corpo nao muda). */
    public void atualizarApresentacao() {
        atualizarPiscar();
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

    /** 5O.1: publico (passos da guarda andando). */
    public boolean isAndando() {

        return andando;
    }

    /**
     * Entrega 5E: leitura publica (somente leitura) para o ControladorAnimacoes
     * escolher a animacao; nao muda nenhuma regra de movimento ou desenho.
     */
    public boolean estaAndando() {
        return isAndando();
    }

    /** Entrega 5E: idem, para o taunt do idle. */
    public boolean estaEmTaunt() {
        return isTaunt();
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
    
    // =============================
    // ENTREGA 5K: TRANSICOES DE POSTURA
    // =============================
    /** Transicao do proximo clique SIMPLES (ciclo em pe -> joelhos -> deitado -> joelhos -> em pe). */
    public TransicaoPostura proximaTransicaoSimples() {
        return switch (estadoCorporal) {
            case DEITADO -> TransicaoPostura.ERGUER_JOELHOS;
            case AJOELHADO -> cicloSubindo ? TransicaoPostura.LEVANTAR : TransicaoPostura.DEITAR;
            default -> TransicaoPostura.AJOELHAR;
        };
    }

    /** Transicao do DUPLO clique: em pe deita direto; de joelhos ou deitado, levanta direto. */
    public TransicaoPostura proximaTransicaoRapida() {
        return switch (estadoCorporal) {
            case DEITADO -> TransicaoPostura.RAPIDA_LEVANTAR_DO_CHAO;
            case AJOELHADO -> TransicaoPostura.RAPIDA_LEVANTAR;
            default -> TransicaoPostura.RAPIDA_DEITAR;
        };
    }

    /**
     * Executa uma transicao inteira ou nada: confere bloqueios e folego,
     * cobra UM custo e muda a postura logica (a animacao acompanha).
     */
    public TransicaoPostura.Resultado executarTransicao(TransicaoPostura t) {
        if (t == null || pulando || rolando || fazendoSprawl || bloqueadoPorAnimacaoCombate) {
            return TransicaoPostura.Resultado.BLOQUEADA;
        }
        EstadoCorporal atual = estadoCorporal == EstadoCorporal.AGACHADO ? EstadoCorporal.EM_PE : estadoCorporal;
        if (atual != t.getOrigem()) {
            return TransicaoPostura.Resultado.BLOQUEADA;
        }
        if (folegoAtual < t.getCusto()) {
            return TransicaoPostura.Resultado.SEM_FOLEGO;
        }
        gastarEnergia(t.getCusto());
        registrarCusto(t.getCusto(), t.getContrato());
        esforcoNesteTick = true;
        correndo = false;
        respirando = false;
        agachado = false;
        faseRespiracao = 0;
        estadoCorporal = t.getDestino();
        // Direcao do ciclo: de joelhos vindo de deitado sobe; vindo de em pe desce.
        cicloSubindo = t == TransicaoPostura.ERGUER_JOELHOS;
        transicaoAtiva = t;
        ticksTransicao = 0;
        ultimaAtividade = System.currentTimeMillis();
        return TransicaoPostura.Resultado.FEITA;
    }

    /** Animacao de transicao em andamento (null = nenhuma). */
    public TransicaoPostura getTransicaoAtiva() {
        return transicaoAtiva;
    }

    /** 0..1 da animacao de transicao atual. */
    public double getProgressoTransicao() {
        return transicaoAtiva == null ? 1.0
                : Math.min(1.0, (ticksTransicao + 1) / (double) transicaoAtiva.getDuracaoTicks());
    }

    /** De joelhos: o proximo clique simples levanta (true) ou deita (false). */
    public boolean isCicloSubindo() {
        return cicloSubindo;
    }

    /**
     * Esforco do motor de combate (ex.: arrastar): gasta o que der, sem
     * deixar negativo, e suspende a recuperacao passiva neste tick.
     */
    // ---------------- 5Q.4: MODO DESENVOLVEDOR (politica de recursos desta instancia; padrao: desligada)
    private boolean folegoInfinito;
    private boolean condicionamentoInfinito;

    /**
     * Recursos "infinitos" do Modo Desenvolvedor: nada e gasto e o valor fica no MAXIMO NORMAL do
     * lutador (atributos e faixa reais; nenhum numero gigante). So o Modo Desenvolvedor liga.
     */
    public void setRecursosInfinitos(boolean folego, boolean condicionamento) {
        folegoInfinito = folego;
        condicionamentoInfinito = condicionamento;
        manterRecursosInfinitos();
    }

    public boolean isFolegoInfinito() {
        return folegoInfinito;
    }

    public boolean isCondicionamentoInfinito() {
        return condicionamentoInfinito;
    }

    /** Folego e condicionamento de volta ao maximo normal (restaurar). */
    public void restaurarRecursos() {
        folegoAtual = atributos.getFolego();
        condicionamentoAtual = atributos.getCondicionamento();
    }

    private void manterRecursosInfinitos() {
        if (folegoInfinito) {
            folegoAtual = atributos.getFolego();
        }
        if (condicionamentoInfinito) {
            condicionamentoAtual = atributos.getCondicionamento();
        }
    }

    // ---------------- 5Q: graduacao logica e tecnicas aprendidas (independentes da faixa desenhada)
    // 5Q.1: a graduacao e a FONTE UNICA de atributos, cor da faixa e elegibilidade das tecnicas.
    // Padrao: faixa branca = os atributos originais (na 5Q o padrao era preta; mudou a pedido).
    private com.irmaosmiau.regras.Graduacao graduacao = com.irmaosmiau.regras.Graduacao.BRANCA;
    /** Atributos da faixa branca (os do personagem/perfil); nunca alterados. */
    private AtributosLutador atributosBase;
    private Color corKimonoPadrao;

    public AtributosLutador getAtributosBase() {
        return atributosBase;
    }
    private final java.util.Set<String> naoAprendidas = new java.util.HashSet<>();

    /** Graduacao LOGICA (libera tecnicas); a cor da faixa desenhada e so cosmetica. */
    public com.irmaosmiau.regras.Graduacao getGraduacao() {
        return graduacao;
    }

    /**
     * 5Q.1: troca a graduacao e RECALCULA os atributos a partir da base (nunca do valor
     * atual: repetir ou reiniciar nao acumula bonus; voltar a branca restaura o original).
     * Folego e condicionamento voltam ao novo maximo (escolha feita antes da luta).
     */
    public void setGraduacao(com.irmaosmiau.regras.Graduacao g) {
        graduacao = g == null ? com.irmaosmiau.regras.Graduacao.BRANCA : g;
        atributos = com.irmaosmiau.regras.EvolucaoGraduacao.aplicar(atributosBase, graduacao);
        folegoAtual = atributos.getFolego();
        condicionamentoAtual = atributos.getCondicionamento();
    }

    /** 5Q.1: cor do kimono escolhida (casaco e calca). null volta ao padrao do personagem. */
    public void setCorKimono(CorKimono k) {
        corKimono = k == null ? corKimonoPadrao : k.getCor();
    }

    public CorKimono getCorKimono() {
        return CorKimono.de(corKimono);
    }

    public CorKimono getCorKimonoPadrao() {
        return CorKimono.de(corKimonoPadrao);
    }

    /** 5Q.1: cor da faixa pela graduacao (fonte unica). */
    public Color getCorFaixa() {
        return switch (graduacao) {
            case BRANCA -> new Color(244, 244, 238);
            case AZUL -> new Color(55, 55, 220);
            case ROXA -> new Color(122, 46, 168);
            case MARROM -> new Color(116, 72, 36);
            case PRETA -> new Color(24, 24, 26);
        };
    }

    /**
     * 5Q.1: a faixa no retangulo dado (no mesmo Graphics/rotacao do corpo, entao
     * acompanha a cintura): cor da graduacao, contorno cinza (faixa branca em kimono
     * branco / preta em kimono preto continuam visiveis) e a tarja na ponta
     * (vermelha na preta, preta nas outras).
     */
    protected void desenharFaixa(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(getCorFaixa());
        g.fillRect(x, y, w, h);
        g.setColor(graduacao == com.irmaosmiau.regras.Graduacao.PRETA ? new Color(200, 30, 30) : new Color(22, 22, 22));
        g.fillRect(x + (int) Math.round(w * 0.70), y, Math.max(2, (int) Math.round(w * 0.12)), h);
        g.setColor(new Color(128, 128, 128));               // contorno POR DENTRO: a silhueta nao muda
        g.fillRect(x, y, w, 1);
        g.fillRect(x, y + h - 1, w, 1);
    }

    /** A tecnica (id base, ex.: "armlock") foi aprendida? Padrao: todas. */
    public boolean aprendeu(String tecnicaBase) {
        return !naoAprendidas.contains(tecnicaBase);
    }

    public void esquecerTecnica(String tecnicaBase) {
        naoAprendidas.add(tecnicaBase);
    }

    public void aprenderTecnica(String tecnicaBase) {
        naoAprendidas.remove(tecnicaBase);
    }

    /** 5P.2: desgaste de CONDICIONAMENTO de uma acao/impacto (nunca negativo). */
    public void gastarCondicionamentoEsforco(double quantidade, String motivo) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("O custo de condicionamento nao pode ser negativo.");
        }
        gastarCondicionamento(quantidade);
    }

    /** 5P.2: 0 (descansado) a 1 (condicionamento zerado); efeitos graduais. */
    public double fatorExaustao() {
        return CustosFolego.fatorExaustao(condicionamentoAtual, atributos.getCondicionamento());
    }

    /** 5P.2: exaustao critica (identificada; a luta NAO termina por isso nesta entrega). */
    public boolean isExaustaoCritica() {
        return condicionamentoAtual <= atributos.getCondicionamento() * CustosFolego.LIMIAR_EXAUSTAO_CRITICA;
    }

    public void gastarFolegoEsforco(double quantidade, String motivo) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("O custo de folego nao pode ser negativo.");
        }
        gastarEnergia(quantidade);
        registrarCusto(quantidade, motivo);
        esforcoNesteTick = true;
    }

    private void registrarCusto(double valor, String motivo) {
        ultimoCustoFolego = valor;
        ultimoCustoMotivo = motivo == null ? "" : motivo;
    }

    /** Ultimo custo pontual de folego (HUD tecnico). */
    public double getUltimoCustoFolego() {
        return ultimoCustoFolego;
    }

    public String getUltimoCustoMotivo() {
        return ultimoCustoMotivo;
    }

    /** Velocidade normal sem efeitos temporarios (referencia do custo da caminhada). */
    private double calcularVelocidadeSemEfeitos() {
        return 3.2 + (atributos.getVelocidade() / 100.0) * 1.6;
    }

    // =============================
    // ENTREGA 5L: EVASAO ANTES DO CONTATO
    // =============================
    /** Janela do "abaixar" que ainda evita uma pegada (~190 ms); segurar depois disso nao protege. */
    public static final int JANELA_EVASAO_ABAIXAR_TICKS = 12;

    /**
     * Evasao possivel AGORA contra uma pegada que esta chegando, ou null:
     * "ROLAMENTO", "PULO" ou "AGACHAMENTO" (o abaixar recente, de pe ou de joelhos).
     */
    public String getEvasaoAtiva() {
        if (rolando) {
            return "ROLAMENTO";
        }
        if (acrobacia != null && acrobacia.noAr(progressoAcrobacia())) {
            return "ACROBACIA";                    // 5O: como o pulo (so pegadas; nada de invulnerabilidade geral)
        }
        if (pulando) {
            return "PULO";
        }
        if (ticksAbaixando > 0 && ticksAbaixando <= JANELA_EVASAO_ABAIXAR_TICKS) {
            return "AGACHAMENTO";
        }
        return null;
    }

    public int getTicksAbaixando() {
        return ticksAbaixando;
    }

    /** Entrega 5J: de joelhos. */
    public boolean isAjoelhado() {
        return estadoCorporal == EstadoCorporal.AJOELHADO;
    }

    /**
     * Entrega 5J: fica de joelhos (inicio da luta). Nao vale no meio de
     * pulo, rolamento ou sprawl.
     */
    public void ajoelhar() {
        if (pulando || rolando || fazendoSprawl) {
            return;
        }
        estadoCorporal = EstadoCorporal.AJOELHADO;
        correndo = false;
        agachado = false;
        andando = false;
        cicloSubindo = false;       // 5K: como quem acabou de ajoelhar (proximo clique deita)
        transicaoAtiva = null;
    }

    /** Postura corporal atual (5J: para o HUD tecnico e as animacoes). */
    public EstadoCorporal getEstadoCorporal() {
        return estadoCorporal;
    }

    /** Entrega 5J: de joelhos anda bem mais devagar que em pe. */
    public double getVelocidadeAjoelhado() {
        return calcularVelocidadeNormal() * RegrasVelocidade.FATOR_AJOELHADO;
    }

    // =============================
    // 5Q.5: DESLOCAMENTO ACELERADO NO CHAO (X de joelhos / deitado)
    // =============================
    private boolean deslocamentoAcelerado;

    /**
     * O GamePanel informa a cada tick se o X esta segurado enquanto ele anda de
     * joelhos ou deitado. Sem folego (ou parado, ou em pe) nao acelera.
     */
    public void setDeslocamentoAcelerado(boolean x) {
        deslocamentoAcelerado = x && RegrasVelocidade.aceleraNoChao(estadoCorporal) && !isSemFolego()
                && !respirando && !rolando && transicaoAtiva == null;
    }

    /** O X foi aceito neste tick (antes de saber se ele anda). */
    public boolean isDeslocamentoAceleradoPedido() {
        return deslocamentoAcelerado;
    }

    /** Andando acelerado agora (desenho: joelhos rapidos / impulsos de quadril). */
    public boolean isDeslocamentoAcelerado() {
        return deslocamentoAcelerado && andando;
    }

    /** Velocidade individual no chao com o X (de joelhos ou deitado). */
    public double getVelocidadeChaoAcelerada() {
        return calcularVelocidadeNormal() * RegrasVelocidade.fatorPostura(estadoCorporal)
                * RegrasVelocidade.fatorAcelerado(estadoCorporal);
    }

    /** Folego do tick acelerando (respeita os recursos infinitos do Modo Desenvolvedor). */
    public void cobrarDeslocamentoAcelerado() {
        gastarEnergia(RegrasVelocidade.custoAcelerado(estadoCorporal));
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

        // 5M.2: o rosto nao deixa o traco dos olhos fechados vazar para quem chamou.
        java.awt.Stroke tracoAnterior = g2.getStroke();

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

        /*
         * 5M.2: traco proprio da boca, igual ao do rig oficial na cabeca.
         * Antes a boca herdava o traco atual: com os olhos fechados
         * (2 x escala, pontas quadradas) ela engrossava a cada piscada;
         * no desenho "em pe" herdava o traco de quem chamou.
         */
        g2.setStroke(
                tracoDaBoca(escala)
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

        g2.setStroke(
                tracoAnterior
        );
    }

    /** 5M.2: traco da boca: max(1, 1.5 x escala), pontas redondas (o mesmo do rig e do MiauStudio). */
    public static BasicStroke tracoDaBoca(double escala) {
        return new BasicStroke(
                (float) Math.max(1.0, 1.5 * escala),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        );
    }

    /** 5M.2: forca os olhos abertos/fechados (testes do rosto). */
    void definirPiscandoParaTestes(boolean fechado) {
        piscando = fechado;
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
        contornarForma(g2, new java.awt.geom.RoundRectangle2D.Double(pataX, pataY, largura, altura, 5, 5), corPelo);
    }

    // =============================
    // 5Q.4: CONTORNOS DISCRETOS
    // =============================
    /*
     * Branco de kimono branco (ou preto de kimono preto) perdia a separacao entre mao,
     * manga, tronco e perna. Cada membro ganha uma borda fina num tom proximo da propria
     * cor (mais escuro nas cores claras, mais claro nas escuras), desenhada logo antes
     * do membro: o membro da frente separa-se do de tras e do tronco. A faixa nao muda.
     */
    /** Largura extra (px, os dois lados juntos) do contorno de um membro. */
    protected double larguraContorno() {
        return 2.6 * escala;
    }

    /** Tom do contorno: perto da cor, sem virar linha preta de desenho. */
    public static Color corContorno(Color c) {
        double lum = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
        if (lum > 0.45) {
            return new Color((int) (c.getRed() * 0.66), (int) (c.getGreen() * 0.66), (int) (c.getBlue() * 0.66));
        }
        return new Color(Math.min(255, c.getRed() + 62), Math.min(255, c.getGreen() + 62),
                Math.min(255, c.getBlue() + 62));
    }

    /**
     * Membro em dois segmentos (ombro-cotovelo-mao, quadril-joelho-pe) com a cor e a
     * largura ATUAIS do Graphics: primeiro a borda, depois o membro. Cor e traco
     * ficam como estavam.
     */
    protected void membro(Graphics2D g, int ax, int ay, int bx, int by, int cx, int cy) {
        java.awt.Stroke traco = g.getStroke();
        Color cor = g.getColor();
        float w = traco instanceof BasicStroke b ? b.getLineWidth() : (float) (18 * escala);
        int[] xs = {ax, bx, cx};
        int[] ys = {ay, by, cy};
        g.setColor(corContorno(cor));
        g.setStroke(new BasicStroke((float) (w + larguraContorno()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(xs, ys, 3);
        g.setColor(cor);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(xs, ys, 3);
        g.setStroke(traco);
    }

    /** Borda fina de uma forma ja pintada (tronco, pelve, patas). */
    protected void contornarForma(Graphics2D g, java.awt.Shape forma, Color cor) {
        java.awt.Stroke traco = g.getStroke();
        Color atual = g.getColor();
        g.setColor(corContorno(cor));
        g.setStroke(new BasicStroke((float) (1.3 * escala), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(forma);
        g.setStroke(traco);
        g.setColor(atual);
    }

    // =============================
    // POSE EXTERNA DE COMBATE
    // =============================
    /**
     * Recebe uma pose temporaria controlada pelo motor de combate.
     *
     * Fazemos copia defensiva para que o array original da animacao nao
     * possa ser alterado acidentalmente pelo personagem.
     */
    public void aplicarPoseCombateExterna(
            int[] pose,
            double deslocamentoVisualX,
            double deslocamentoVisualY
    ) {

        if (pose == null) {
            throw new IllegalArgumentException(
                    "A pose externa de combate nao pode ser nula."
            );
        }

        if (pose.length != TAMANHO_POSE_MIAUSTUDIO) {
            throw new IllegalArgumentException(
                    "A pose externa precisa possuir exatamente "
                    + TAMANHO_POSE_MIAUSTUDIO
                    + " valores."
            );
        }

        poseCombateExterna = Arrays.copyOf(
                pose,
                pose.length
        );

        deslocamentoVisualCombateX = deslocamentoVisualX;
        deslocamentoVisualCombateY = deslocamentoVisualY;
    }

    public void limparPoseCombateExterna() {
        poseCombateExterna = null;
        deslocamentoVisualCombateX = 0.0;
        deslocamentoVisualCombateY = 0.0;
    }

    public boolean isPoseCombateExternaAtiva() {
        return poseCombateExterna != null;
    }

    /**
     * Copia publica para debug/testes sem expor o array interno mutavel.
     */
    public int[] copiarPoseCombateExterna() {

        if (poseCombateExterna == null) {
            return null;
        }

        return Arrays.copyOf(
                poseCombateExterna,
                poseCombateExterna.length
        );
    }

    /**
     * Acesso protegido que os renderers Branco/Preto usarao na etapa 8D.
     */
    protected int[] getPoseCombateExternaParaDesenho() {
        int[] p = copiarPoseCombateExterna();
        // 5O.2: respirando de joelhos ou deitado = o peito e a cabeca sobem e descem 1-2 px (so desenho).
        if (p != null && respirando && (isAjoelhado() || isDeitado())) {
            int r = (int) Math.round(Math.sin(faseRespiracao) * (isDeitado() ? 1.0 : 1.5));
            p[8] -= r;
            p[13] -= r;
        }
        return p;
    }

    /**
     * 5O.3: o lutador esta numa pegada (aplicando OU sofrendo), do inicio da
     * entrada ate o fim do vinculo. Marcado pelo SistemaCombate; enquanto true,
     * TODA recuperacao (folego e condicionamento, passiva e respirando) para.
     * O consumo normal continua.
     */
    private boolean vinculadoEmPegada;

    public void setVinculadoEmPegada(boolean vinculado) {
        this.vinculadoEmPegada = vinculado;
    }

    public boolean isVinculadoEmPegada() {
        return vinculadoEmPegada;
    }

    /**
     * 5P: ligado ao outro numa posicao de solo (guarda, cem quilos, montada,
     * costas...). Marcado pelo MotorSolo; como na pegada (5O.3), nenhuma
     * recuperacao de folego ou condicionamento enquanto durar.
     */
    private boolean vinculadoNoSolo;

    public void setVinculadoNoSolo(boolean vinculado) {
        this.vinculadoNoSolo = vinculado;
    }

    public boolean isVinculadoNoSolo() {
        return vinculadoNoSolo;
    }

    /** 5P.1: fator de respiracao no solo (o MotorSolo calcula pela posicao e pelo papel). */
    private double fatorRespiracaoSolo = 1.0;

    public void setFatorRespiracaoNoSolo(double fator) {
        this.fatorRespiracaoSolo = Math.max(0.0, Math.min(1.0, fator));
    }

    /**
     * 5P.1: quanto a respiracao voluntaria rende AGORA (0..1) pelo contexto:
     * livre 1; na pegada RESPIRACAO_NA_PEGADA; no solo o fator do motor (0 no meio
     * da propria tecnica); andando, metade. A recuperacao PASSIVA continua so para
     * quem esta livre.
     */
    public double fatorRespiracaoContexto() {
        double f = 1.0;
        if (vinculadoEmPegada) {
            f = Math.min(f, CustosFolego.RESPIRACAO_NA_PEGADA);
        }
        if (vinculadoNoSolo) {
            f = Math.min(f, fatorRespiracaoSolo);
        }
        if (andando) {
            f *= CustosFolego.RESPIRACAO_ANDANDO;
        }
        return f;
    }

    /**
     * 5P: o MotorSolo muda a postura (cair, acompanhar, raspar, levantar) sem
     * cobrar o custo de uma transicao comum; a animacao de transicao de postura
     * mais proxima e usada para nao "teletransportar" o corpo. Interrompe pulo,
     * rolamento, sprawl, acrobacia e guarda.
     */
    /**
     * 5Q.5: troca a transicao que ACABOU de comecar por outra equivalente (mesma origem e
     * destino), ex.: a levantada tecnica do solo livre no lugar da levantada rapida.
     */
    public void preferirTransicao(TransicaoPostura t) {
        if (t != null && transicaoAtiva != null && ticksTransicao == 0 && transicaoAtiva != t
                && transicaoAtiva.getOrigem() == t.getOrigem() && transicaoAtiva.getDestino() == t.getDestino()) {
            transicaoAtiva = t;
        }
    }

    public void aplicarPosturaDoSolo(EstadoCorporal destino) {
        EstadoCorporal atual = estadoCorporal == EstadoCorporal.AGACHADO ? EstadoCorporal.EM_PE : estadoCorporal;
        rolando = false;
        fazendoSprawl = false;
        acrobacia = null;
        pulando = false;
        alturaPulo = 0;
        velocidadePulo = 0;
        correndo = false;
        respirando = false;
        agachado = false;
        defesa.interromper();
        if (atual == destino) {
            return;
        }
        TransicaoPostura escolhida = null;
        for (TransicaoPostura t : TransicaoPostura.values()) {
            if (t.getOrigem() == atual && t.getDestino() == destino && (escolhida == null || t.isRapida())) {
                escolhida = t;
            }
        }
        estadoCorporal = destino;
        cicloSubindo = destino == EstadoCorporal.AJOELHADO && atual == EstadoCorporal.DEITADO;
        transicaoAtiva = escolhida;
        ticksTransicao = 0;
        ultimaAtividade = System.currentTimeMillis();
    }

    /** 5O.2: fase da respiracao (desenho e testes). */
    public double getFaseRespiracao() {
        return faseRespiracao;
    }

    public double getDeslocamentoVisualCombateX() {
        return deslocamentoVisualCombateX;
    }

    public double getDeslocamentoVisualCombateY() {
        return deslocamentoVisualCombateY;
    }

    public boolean isBloqueadoPorAnimacaoCombate() {
        return bloqueadoPorAnimacaoCombate;
    }

    public void setBloqueadoPorAnimacaoCombate(
            boolean bloqueado
    ) {
        bloqueadoPorAnimacaoCombate = bloqueado;
    }

    /**
     * Limpa de uma vez o estado temporario deixado por uma animacao.
     */
    public void limparEstadoAnimacaoCombate() {
        limparPoseCombateExterna();
        bloqueadoPorAnimacaoCombate = false;
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

    /**
     * Escala visual/fisica usada pelo rig deste personagem.
     *
     * O motor espacial usa este valor para converter distancias da tela
     * para unidades locais do atacante. Assim o alcance de uma tecnica
     * nao fica preso a uma quantidade fixa de pixels.
     */
    public double getEscala() {

        return escala;
    }

    /**
     * Informa para qual lado o personagem esta olhando no mundo do jogo.
     */
    public boolean isViradoParaDireita() {

        return viradoParaDireita;
    }
    
    protected double getProgressoRolamento() {

    if (!rolando) {
        return 0.0;
    }

    return Math.min(
            1.0,
            (double) framesRolamento
            / duracaoRolamentoAtual
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
    public boolean isFazendoSprawl() {

    return fazendoSprawl;
}

protected double getProgressoSprawl() {

    if (!fazendoSprawl) {
        return 0.0;
    }

    return Math.min(
            1.0,
            (double) framesSprawl
            / DURACAO_SPRAWL
    );
}
    
    
    public void setViradoParaDireita(
        boolean viradoParaDireita
) {

    this.viradoParaDireita =
            viradoParaDireita;
}

/*
 * Retorna true quando o rolamento está indo
 * para a frente do personagem.
 *
 * CIMA e BAIXO sozinhos não contam como
 * frente nem trás.
 */
protected boolean isRolamentoParaFrente() {

    if (direcaoRolamento == null) {
        return false;
    }

    boolean indoParaDireita =
            direcaoRolamento == Direcao.DIREITA
            || direcaoRolamento == Direcao.CIMA_DIREITA
            || direcaoRolamento == Direcao.BAIXO_DIREITA;

    boolean indoParaEsquerda =
            direcaoRolamento == Direcao.ESQUERDA
            || direcaoRolamento == Direcao.CIMA_ESQUERDA
            || direcaoRolamento == Direcao.BAIXO_ESQUERDA;

    if (viradoParaDireitaNoInicioRolamento) {

        return indoParaDireita;

    } else {

        return indoParaEsquerda;
    }
}

/*
 * Mesma lógica, mas para trás.
 */
protected boolean isRolamentoParaTras() {

    if (direcaoRolamento == null) {
        return false;
    }

    boolean possuiMovimentoHorizontal =
            direcaoRolamento == Direcao.DIREITA
            || direcaoRolamento == Direcao.ESQUERDA
            || direcaoRolamento == Direcao.CIMA_DIREITA
            || direcaoRolamento == Direcao.CIMA_ESQUERDA
            || direcaoRolamento == Direcao.BAIXO_DIREITA
            || direcaoRolamento == Direcao.BAIXO_ESQUERDA;

    if (!possuiMovimentoHorizontal) {
        return false;
    }

    return !isRolamentoParaFrente();
}

    // =============================
    // ENERGIA - CONSULTAS
    // =============================
    public double getPercentualFolego() {

        return folegoAtual
                / atributos.getFolego();
    }

    public double getPercentualCondicionamento() {

        return condicionamentoAtual
                / atributos.getCondicionamento();
    }

// =============================
    // ENERGIA - CONSUMO E RECUPERAÇÃO
    // =============================
    /**
     * Interface publica e controlada para custos do motor de combate.
     *
     * Retorna false sem alterar o personagem quando nao ha folego
     * suficiente. O motor nao precisa acessar campos internos nem duplicar
     * a regra de limite minimo da energia.
     */
    public boolean consumirFolegoCombate(
            double quantidade
    ) {

        if (quantidade < 0) {
            throw new IllegalArgumentException(
                    "O custo de folego nao pode ser negativo."
            );
        }

        if (folegoInfinito) {
            return true;                                 // 5Q.4: Modo Desenvolvedor (nada e gasto)
        }
        if (folegoAtual < quantidade) {
            return false;
        }

                gastarEnergia(quantidade);
        registrarCusto(quantidade, "combate");
        ultimaAtividade = System.currentTimeMillis();
        return true;
    }

    private void gastarEnergia(
            double quantidade
    ) {

        if (quantidade <= 0) {
            return;
        }

        if (folegoInfinito) {
            return;                                      // 5Q.4: Modo Desenvolvedor
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

        if (condicionamentoInfinito) {
            return;                                      // 5Q.4: Modo Desenvolvedor
        }

        condicionamentoAtual -= quantidade;

        if (condicionamentoAtual < 0) {

            condicionamentoAtual = 0;
        }
    }

private void recuperarFolego() {

    if (vinculadoEmPegada || vinculadoNoSolo) {
        return;                                         // 5O.3: aplicando ou sofrendo pegada
    }

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

    double contexto = fatorRespiracaoContexto();          // 5P.1: substitui o bloqueio absoluto da 5O.3
    if (contexto <= 0) {
        return;
    }

    if (pulando) {

        return;
    }

    /*
     * Respirar recupera cerca de
     * quinze vezes mais rápido que
     * simplesmente ficar parado.
     */
    double taxa =
            calcularTaxaRecuperacaoFolego()
            * 15.0
            * CustosFolego.recuperacaoRespirando(posturaBase())   // 5O.2: por postura
            * contexto
            * (1.0 - CustosFolego.EXAUSTAO_RESPIRACAO * fatorExaustao());   // 5P.2: exausto respira pior

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

    double contexto = fatorRespiracaoContexto();          // 5P.1
    if (contexto <= 0) {
        return;
    }

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
            taxaBase * 2.5 * CustosFolego.recuperacaoRespirando(posturaBase()) * contexto;   // 5O.2 / 5P.1

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

        // 5O.2: respira em pe, de joelhos E deitado (sem mudar a postura); nunca no ar,
        // rolando, no sprawl, numa acrobacia ou no meio de uma transicao de postura.
        this.respirando =
        respirando
        && !pulando
        && !rolando
        && !fazendoSprawl
        && acrobacia == null
        && transicaoAtiva == null
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
