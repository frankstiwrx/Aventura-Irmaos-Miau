package com.irmaosmiau.combate;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Coordenador das regras compartilhadas entre dois lutadores.
 *
 * Nesta etapa o motor ja possui a primeira interacao persistente:
 * PEGADA_1. O sistema continua separado de teclado, renderer e classes
 * concretas de personagem.
 */
public final class SistemaCombate {

    private final IrmaoMiau lutadorA;
    private final IrmaoMiau lutadorB;
    private final RegistroAcoes registroAcoes;

    private ContextoCombate contextoAParaBAtual;
    private ContextoCombate contextoBParaAAtual;

    // =====================================================
    // INTERACAO COMPARTILHADA
    // =====================================================
    private EstadoInteracaoCombate estadoInteracao
            = EstadoInteracaoCombate.LIVRE;

    private IrmaoMiau aplicadorInteracao;
    private IrmaoMiau receptorInteracao;
    private String idAcaoInteracao;

    // =====================================================
    // ANIMACAO CONJUNTA ATIVA
    // =====================================================
    private AnimacaoCombateAtiva animacaoCombateAtiva;
    // Entrega 5J: variante (molde + papel) da animacao conjunta em uso.
    private SelecaoAnimacaoConjunta selecaoAnimacao;
    private final RegistroAnimacoesCombate registroAnimacoes;

    // Ultima tentativa: util para debug e, depois, feedback visual/sonoro.
    // =====================================================
    // POLITICA DE INTERACAO (Entrega 3A)
    // =====================================================
    private final PoliticaInteracao politicaInteracao;

    /* Retratos tirados quando a interacao foi ESTABELECIDA (fim da entrada). */
    private EstadoCorporalObservado referenciaAplicador;
    private EstadoCorporalObservado referenciaReceptor;

    private MotivoEncerramentoInteracao ultimoMotivoEncerramento;

    // =====================================================
    // ACOES TECNICAS DURANTE A PEGADA (Entrega 5A)
    // =====================================================
    /** Folego do aplicador para tentar o desequilibrio. */
    public static final double CUSTO_DESEQUILIBRIO = 4.0;
    /** Folego do receptor para tentar o rompimento tecnico (pago com ou sem sucesso). */
    public static final double CUSTO_ROMPIMENTO_TECNICO = 3.0;
    /*
     * Entrega 5B: resistir PASSIVAMENTE nao custa nada (antes: 1 de folego).
     * Evita drenar o adversario so repetindo tentativas contra quem nao agiu.
     */
    /** Recarga entre acoes tecnicas do mesmo lutador (~0,75 s). */
    public static final int RECARGA_ACAO_TECNICA_TICKS = 45;
    /** Duracao do DESEQUILIBRIO no receptor (~2 s). */
    public static final int DURACAO_DESEQUILIBRIO_TICKS = 120;

    // =====================================================
    // FIRMEZA DA PEGADA (Entrega 5B)
    // =====================================================
    /** Firmeza de uma pegada recem-estabelecida (0..100). */
    public static final double FIRMEZA_INICIAL = 100.0;
    /** Desgaste por tick depois da entrada: 100 -> 0 em 1000 ticks (~16,7 s a 60 tps). */
    public static final double DESGASTE_FIRMEZA_POR_TICK = 0.10;
    /** Desgaste extra nos ticks com deslocamento conjunto (total 0,25/tick: ~6,7 s). */
    public static final double DESGASTE_EXTRA_DESLOCAMENTO_POR_TICK = 0.15;
    /** Firmeza recuperada por um reforco (limitada a 100). */
    public static final double REFORCO_FIRMEZA = 25.0;
    /** Folego do aplicador por reforco. */
    public static final double CUSTO_REFORCO = 3.0;
    /** Recarga entre reforcos (~1 s), separada da recarga das acoes de disputa. */
    public static final int RECARGA_REFORCO_TICKS = 60;
    /** Abaixo disso o HUD avisa "controle enfraquecendo". */
    public static final double LIMIAR_CONTROLE_FRACO = 30.0;
    /** Deslocamento conjunto: 1 px por tick (caminhada normal: 3,2 a 4,8 px). */
    public static final int VELOCIDADE_DESLOCAMENTO_CONJUNTO = 1;

    /* Firmeza da interacao ATUAL (0 quando nao ha interacao). */
    private double firmeza;
    /* Houve deslocamento conjunto desde o ultimo desgaste? (1 cobranca por tick) */
    private boolean deslocouNesteTick;
    private final java.util.Map<IrmaoMiau, Long> recargaReforcoAte = new java.util.IdentityHashMap<>();

    /* Relogio do sistema (1 por atualizar()), usado nas recargas. */
    private long tick;
    private final java.util.Map<IrmaoMiau, Long> recargaAte = new java.util.IdentityHashMap<>();
    private ResultadoDisputa ultimaDisputa;

    private ResultadoAcao ultimoResultado;
    private String ultimaAcaoId;
    private IrmaoMiau ultimoAtacante;

    public SistemaCombate(
            IrmaoMiau lutadorA,
            IrmaoMiau lutadorB
    ) {
        this(
                lutadorA,
                lutadorB,
                new RegistroAcoes()
        );
    }

    public SistemaCombate(
            IrmaoMiau lutadorA,
            IrmaoMiau lutadorB,
            RegistroAcoes registroAcoes
    ) {
        this(
                lutadorA,
                lutadorB,
                registroAcoes,
                new RegistroAnimacoesCombate()
        );
    }

    /**
     * Entrega 2: registros injetados (por exemplo, carregados de JSON).
     *
     * Toda acao JOGAVEL com animacao conjunta precisa encontrar essa
     * animacao no registro; caso contrario o sistema nem e criado, em vez
     * de falhar silenciosamente no meio da luta.
     */
    public SistemaCombate(
            IrmaoMiau lutadorA,
            IrmaoMiau lutadorB,
            RegistroAcoes registroAcoes,
            RegistroAnimacoesCombate registroAnimacoes
    ) {
        this(
                lutadorA,
                lutadorB,
                registroAcoes,
                registroAnimacoes,
                new PoliticaInteracaoPadrao()
        );
    }

    /**
     * Entrega 3A: politica de interacao injetavel.
     */
    public SistemaCombate(
            IrmaoMiau lutadorA,
            IrmaoMiau lutadorB,
            RegistroAcoes registroAcoes,
            RegistroAnimacoesCombate registroAnimacoes,
            PoliticaInteracao politicaInteracao
    ) {

        if (politicaInteracao == null) {
            throw new IllegalArgumentException(
                    "A politica de interacao nao pode ser nula."
            );
        }

        this.politicaInteracao = politicaInteracao;

        if (registroAnimacoes == null) {
            throw new IllegalArgumentException(
                    "O registro de animacoes nao pode ser nulo."
            );
        }

        if (registroAcoes != null) {
            for (AcaoCombate acao : registroAcoes.listar()) {
                if (acao.isJogavel()
                        && acao.possuiAnimacaoConjunta()
                        && !registroAnimacoes.contem(acao.getIdAnimacaoConjunta())) {
                    throw new IllegalArgumentException(
                            "A acao " + acao.getId()
                            + " usa a animacao inexistente: "
                            + acao.getIdAnimacaoConjunta()
                    );
                }
            }
        }

        this.registroAnimacoes = registroAnimacoes;

        if (lutadorA == null || lutadorB == null) {
            throw new IllegalArgumentException(
                    "O SistemaCombate precisa de dois lutadores."
            );
        }

        if (lutadorA == lutadorB) {
            throw new IllegalArgumentException(
                    "Os dois lados do combate precisam ser personagens diferentes."
            );
        }

        if (registroAcoes == null) {
            throw new IllegalArgumentException(
                    "O registro de acoes nao pode ser nulo."
            );
        }

        this.lutadorA = lutadorA;
        this.lutadorB = lutadorB;
        this.registroAcoes = registroAcoes;

        atualizar();
    }

    public IrmaoMiau getLutadorA() {
        return lutadorA;
    }

    public IrmaoMiau getLutadorB() {
        return lutadorB;
    }

    public RegistroAcoes getRegistroAcoes() {
        return registroAcoes;
    }

    public RegistroAnimacoesCombate getRegistroAnimacoes() {
        return registroAnimacoes;
    }

    /**
     * Atualiza a fotografia compartilhada da luta e verifica se uma
     * interacao persistente ainda pode continuar espacialmente.
     */
    public void atualizar() {

        tick++;

        contextoAParaBAtual = new ContextoCombate(
                lutadorA,
                lutadorB
        );

        contextoBParaAAtual = new ContextoCombate(
                lutadorB,
                lutadorA
        );

        atualizarManutencaoDaInteracao();
        atualizarFirmeza();
        atualizarAnimacaoCombate();
    }

    /**
     * Entrega 5B: desgaste da firmeza. So corre com a interacao ja
     * estabelecida (nunca nos 30 quadros de entrada). Firmeza zero encerra
     * a pegada pelo mecanismo normal (motivo SEM_CONTROLE). Se a politica ja
     * encerrou a interacao neste tick, nada acontece (sem encerramento duplo).
     */
    private void atualizarFirmeza() {
        if (!isInteracaoEstabelecida()) {
            deslocouNesteTick = false;
            return;
        }
        double desgaste = DESGASTE_FIRMEZA_POR_TICK
                + (deslocouNesteTick ? DESGASTE_EXTRA_DESLOCAMENTO_POR_TICK : 0.0);
        deslocouNesteTick = false;
        firmeza = Math.max(0.0, firmeza - desgaste);
        if (firmeza <= 1e-9) {
            firmeza = 0.0;
            encerrarInteracao(MotivoEncerramentoInteracao.SEM_CONTROLE);
        }
    }

    /** Firmeza da pegada atual (0..100); 0 sem interacao. */
    public double getFirmeza() {
        return estaEmInteracao() ? firmeza : 0.0;
    }

    /**
     * Avanca um unico relogio compartilhado por aplicador e receptor.
     * Ao terminar, libera o controle dos dois sem encerrar a interacao
     * persistente conquistada pela acao.
     */
    private void atualizarAnimacaoCombate() {

        if (animacaoCombateAtiva == null) {
            return;
        }

        animacaoCombateAtiva.atualizar();
        aplicarQuadroDaAnimacao();

        if (animacaoCombateAtiva.isFinalizada()) {
            finalizarAnimacaoCombateAtiva();
        }
    }

    /** Envia o MESMO quadro aos dois lutadores, sem alterar X fisico. */
    private void aplicarQuadroDaAnimacao() {
        if (animacaoCombateAtiva == null) {
            return;
        }
        // 5J: a variante escolhida pela dupla (molde + papel) ao iniciar a tecnica.
        AnimacaoConjuntaQuadros sequencia = selecaoAnimacao != null
                ? selecaoAnimacao.quadros()
                : registroAnimacoes.buscar(animacaoCombateAtiva.getIdAnimacao());
        if (sequencia == null) {
            return;
        }
        int indice = sequencia.indiceParaProgresso(
                animacaoCombateAtiva.getProgresso());
        aplicarPoseNaPostura(animacaoCombateAtiva.getAplicador(),
                sequencia.copiarQuadroAplicador(indice),
                sequencia.deslocamentoAplicadorX(indice),
                sequencia.getOffsetVisualY());
        aplicarPoseNaPostura(animacaoCombateAtiva.getReceptor(),
                sequencia.copiarQuadroReceptor(indice),
                sequencia.deslocamentoReceptorX(indice),
                sequencia.getOffsetVisualY());
    }

    /**
     * Entrega 5J: quem esta de joelhos recebe a mesma pose de tronco, cabeca
     * e bracos, com as pernas de joelhos e o corpo abaixado (fallback da
     * PEGADA_AJOELHADA: nada de "levantar" no meio da pegada). Em pe: igual a 5I.
     */
    private static void aplicarPoseNaPostura(IrmaoMiau lutador, int[] pose, double dx, double dy) {
        if (lutador.isAjoelhado()) {
            lutador.aplicarPoseCombateExterna(
                    com.irmaosmiau.entities.PosturaAjoelhada.adaptar(lutador, pose),
                    dx, dy + com.irmaosmiau.entities.PosturaAjoelhada.quedaPx(lutador));
            return;
        }
        lutador.aplicarPoseCombateExterna(pose, dx, dy);
    }

    private void atualizarManutencaoDaInteracao() {

        if (!estaEmInteracao()) {
            return;
        }

        AcaoCombate acao = registroAcoes.buscar(
                idAcaoInteracao
        );

        if (acao == null
                || !acao.isJogavel()
                || aplicadorInteracao == null
                || receptorInteracao == null) {

            encerrarInteracao(MotivoEncerramentoInteracao.ACAO_INDISPONIVEL);
            return;
        }

        ContextoCombate contexto = criarContexto(
                aplicadorInteracao,
                receptorInteracao
        );

        /*
         * Depois que a pegada foi conquistada, o custo de folego nao e
         * reavaliado a cada frame. Quem decide se a interacao continua e a
         * PoliticaInteracao: limites espaciais sempre; mudancas corporais
         * somente depois de estabelecida (fim da animacao de entrada).
         */
        boolean entradaEmAndamento = !isInteracaoEstabelecida();

        SituacaoInteracao situacao = new SituacaoInteracao(
                acao,
                contexto,
                entradaEmAndamento,
                referenciaAplicador,
                referenciaReceptor,
                EstadoCorporalObservado.de(aplicadorInteracao),
                EstadoCorporalObservado.de(receptorInteracao)
        );

        politicaInteracao.avaliar(situacao)
                .ifPresent(this::encerrarInteracao);
    }

    /**
     * true quando a interacao existe e a animacao de entrada ja terminou
     * (ou a acao nao tinha animacao).
     */
    public boolean isInteracaoEstabelecida() {
        return estaEmInteracao() && referenciaAplicador != null;
    }

    /** Motivo do ultimo encerramento de interacao (null se nunca houve). */
    public MotivoEncerramentoInteracao getUltimoMotivoEncerramento() {
        return ultimoMotivoEncerramento;
    }

    public PoliticaInteracao getPoliticaInteracao() {
        return politicaInteracao;
    }

    /** Fixa os retratos corporais usados pela politica a partir de agora. */
    private void estabelecerInteracao() {
        referenciaAplicador = EstadoCorporalObservado.de(aplicadorInteracao);
        referenciaReceptor = EstadoCorporalObservado.de(receptorInteracao);
    }

    public ContextoCombate getContextoAParaBAtual() {
        return contextoAParaBAtual;
    }

    public ContextoCombate getContextoBParaAAtual() {
        return contextoBParaAAtual;
    }

    public boolean possuiAcao(String idAcao) {
        return registroAcoes.contem(idAcao);
    }

    public AcaoCombate getAcao(String idAcao) {
        return registroAcoes.buscar(idAcao);
    }

    public EstadoInteracaoCombate getEstadoInteracao() {
        return estadoInteracao;
    }

    public boolean estaEmInteracao() {
        return estadoInteracao != EstadoInteracaoCombate.LIVRE;
    }

    public boolean estaEmPegada1() {
        return estadoInteracao == EstadoInteracaoCombate.PEGADA_1;
    }

    /**
     * 5J: variante da animacao conjunta da tecnica atual (entrada ou pose
     * mantida); null sem interacao com animacao.
     */
    public SelecaoAnimacaoConjunta getSelecaoAnimacao() {
        return estaEmInteracao() || animacaoCombateAtiva != null ? selecaoAnimacao : null;
    }

    public IrmaoMiau getAplicadorInteracao() {
        return aplicadorInteracao;
    }

    public IrmaoMiau getReceptorInteracao() {
        return receptorInteracao;
    }

    public String getIdAcaoInteracao() {
        return idAcaoInteracao;
    }

    public boolean isAnimacaoCombateAtiva() {
        return animacaoCombateAtiva != null;
    }

    public AnimacaoCombateAtiva getAnimacaoCombateAtiva() {
        return animacaoCombateAtiva;
    }

    public double getProgressoAnimacaoCombate() {
        return animacaoCombateAtiva == null
                ? 0.0
                : animacaoCombateAtiva.getProgresso();
    }

    public ResultadoAcao getUltimoResultado() {
        return ultimoResultado;
    }

    public String getUltimaAcaoId() {
        return ultimaAcaoId;
    }

    public IrmaoMiau getUltimoAtacante() {
        return ultimoAtacante;
    }

    /** Encerramento solicitado de fora (motivo ENCERRAMENTO_MANUAL). */
    public void encerrarInteracao() {
        encerrarInteracao(MotivoEncerramentoInteracao.ENCERRAMENTO_MANUAL);
    }

    private void encerrarInteracao(MotivoEncerramentoInteracao motivo) {

        if (estaEmInteracao()) {
            ultimoMotivoEncerramento = motivo;
        }

        cancelarAnimacaoCombateAtiva();

        referenciaAplicador = null;
        referenciaReceptor = null;
        firmeza = 0.0;
        deslocouNesteTick = false;
        // Tambem remove a pose FINAL persistente depois que a timeline terminou.
        if (aplicadorInteracao != null) {
            aplicadorInteracao.limparEstadoAnimacaoCombate();
        }
        if (receptorInteracao != null) {
            receptorInteracao.limparEstadoAnimacaoCombate();
        }

        estadoInteracao = EstadoInteracaoCombate.LIVRE;
        aplicadorInteracao = null;
        receptorInteracao = null;
        idAcaoInteracao = null;
    }

    /**
     * Cria uma fotografia nova do instante atual.
     */
    // =====================================================
    // ACOES TECNICAS (Entrega 5A)
    // =====================================================
    /**
     * O APLICADOR de uma pegada ja estabelecida tenta desequilibrar o
     * receptor. Nao reinicia a animacao de entrada. Sucesso aplica
     * DESEQUILIBRIO temporario no receptor; a pegada continua.
     */
    public ResultadoAcaoTecnica tentarDesequilibrio(IrmaoMiau atacante) {
        validarDoCombate(atacante);
        if (!isInteracaoEstabelecida() || atacante != aplicadorInteracao) {
            return ResultadoAcaoTecnica.SEM_PEGADA;
        }
        IrmaoMiau defensor = receptorInteracao;
        ResultadoAcaoTecnica pre = prepararAcao(atacante, CUSTO_DESEQUILIBRIO);
        if (pre != null) {
            return pre;
        }
        ResultadoDisputa disputa = disputar(TipoDisputa.DESEQUILIBRIO, atacante, defensor, CUSTO_DESEQUILIBRIO);
        if (disputa.isSucessoDoAtaque()) {
            defensor.getEfeitos().aplicar(
                    com.irmaosmiau.entities.EfeitosTemporarios.Tipo.DESEQUILIBRIO,
                    DURACAO_DESEQUILIBRIO_TICKS);
            return ResultadoAcaoTecnica.SUCESSO;
        }
        return ResultadoAcaoTecnica.RESISTIDO;   // resistencia passiva: sem custo (5B)
    }

    /**
     * O RECEPTOR de uma pegada ja estabelecida tenta solta-la sem se
     * deslocar. Paga o folego com ou sem sucesso. Sucesso encerra a
     * interacao pelo mecanismo normal (motivo ROMPIMENTO_TECNICO).
     */
    public ResultadoAcaoTecnica tentarRompimentoTecnico(IrmaoMiau receptor) {
        validarDoCombate(receptor);
        if (!isInteracaoEstabelecida() || receptor != receptorInteracao) {
            return ResultadoAcaoTecnica.SEM_PEGADA;
        }
        IrmaoMiau aplicador = aplicadorInteracao;
        ResultadoAcaoTecnica pre = prepararAcao(receptor, CUSTO_ROMPIMENTO_TECNICO);
        if (pre != null) {
            return pre;
        }
        ResultadoDisputa disputa = disputar(TipoDisputa.ROMPIMENTO_TECNICO, receptor, aplicador,
                CUSTO_ROMPIMENTO_TECNICO);
        if (disputa.isSucessoDoAtaque()) {
            encerrarInteracao(MotivoEncerramentoInteracao.ROMPIMENTO_TECNICO);
            return ResultadoAcaoTecnica.SUCESSO;
        }
        return ResultadoAcaoTecnica.RESISTIDO;   // resistencia passiva: sem custo (5B)
    }

    /**
     * Entrega 5B: o APLICADOR reforca a pegada estabelecida: +25 de firmeza
     * (maximo 100) por 3 de folego, recarga propria de 60 ticks. Nao mexe na
     * animacao nem nas posicoes.
     */
    public ResultadoAcaoTecnica tentarReforcarPegada(IrmaoMiau aplicador) {
        validarDoCombate(aplicador);
        if (!isInteracaoEstabelecida() || aplicador != aplicadorInteracao) {
            return ResultadoAcaoTecnica.SEM_PEGADA;
        }
        if (getRecargaReforcoRestante(aplicador) > 0) {
            return ResultadoAcaoTecnica.EM_RECARGA;
        }
        if (firmeza >= FIRMEZA_INICIAL - 1e-9) {
            return ResultadoAcaoTecnica.SEM_EFEITO;
        }
        if (aplicador.getFolegoAtual() < CUSTO_REFORCO) {
            return ResultadoAcaoTecnica.SEM_FOLEGO;
        }
        aplicador.consumirFolegoCombate(CUSTO_REFORCO);
        firmeza = Math.min(FIRMEZA_INICIAL, firmeza + REFORCO_FIRMEZA);
        recargaReforcoAte.put(aplicador, tick + RECARGA_REFORCO_TICKS);
        return ResultadoAcaoTecnica.SUCESSO;
    }

    /**
     * Entrega 5B: o APLICADOR desloca a dupla 1 px na direcao pedida (-1/+1),
     * mantendo a distancia relativa, as linhas e a orientacao. Os dois sao
     * movidos pelo MESMO numero inteiro de pixels, e o desenho de cada um e
     * relativo ao proprio x: o encaixe da pose conjunta nao muda.
     *
     * @param xMinimo menor x permitido para qualquer um dos dois
     * @param xMaximo maior x permitido para qualquer um dos dois
     */
    public ResultadoAcaoTecnica deslocarPegadaControlada(IrmaoMiau aplicador, int direcao,
            int xMinimo, int xMaximo) {
        validarDoCombate(aplicador);
        if (!isInteracaoEstabelecida() || aplicador != aplicadorInteracao || direcao == 0) {
            return ResultadoAcaoTecnica.SEM_PEGADA;
        }
        int dx = Integer.signum(direcao) * VELOCIDADE_DESLOCAMENTO_CONJUNTO;
        IrmaoMiau receptor = receptorInteracao;
        int menor = Math.min(aplicador.getX(), receptor.getX()) + dx;
        int maior = Math.max(aplicador.getX(), receptor.getX()) + dx;
        if (menor < xMinimo || maior > xMaximo) {
            return ResultadoAcaoTecnica.LIMITE_ARENA;
        }
        aplicador.deslocarFisicamente(dx);
        receptor.deslocarFisicamente(dx);
        // Entrega 5K: so o aplicador paga o esforco, pelo deslocamento real da dupla.
        aplicador.gastarFolegoEsforco(
                com.irmaosmiau.entities.CustosFolego.ARRASTO_POR_PIXEL * Math.abs(dx), "arrasto");
        // A politica compara com estes retratos: deslocar a referencia junto
        // evita romper por MOVIMENTO, sem esconder outras mudancas do corpo.
        referenciaAplicador = referenciaAplicador.transladado(dx);
        referenciaReceptor = referenciaReceptor.transladado(dx);
        deslocouNesteTick = true;
        return ResultadoAcaoTecnica.SUCESSO;
    }

    public int getRecargaReforcoRestante(IrmaoMiau lutador) {
        Long ate = recargaReforcoAte.get(lutador);
        return ate == null ? 0 : (int) Math.max(0, ate - tick);
    }

    /** Recarga e folego; null = pode agir. */
    private ResultadoAcaoTecnica prepararAcao(IrmaoMiau lutador, double custo) {
        if (getRecargaRestante(lutador) > 0) {
            return ResultadoAcaoTecnica.EM_RECARGA;
        }
        if (lutador.getFolegoAtual() < custo) {
            return ResultadoAcaoTecnica.SEM_FOLEGO;
        }
        return null;
    }

    /**
     * Disputa com o folego do momento da tentativa; depois cobra o custo de
     * quem agiu e inicia a recarga.
     */
    private ResultadoDisputa disputar(TipoDisputa tipo, IrmaoMiau atacante, IrmaoMiau defensor, double custo) {
        ultimaDisputa = ResolvedorDisputa.resolver(tipo, atacante, defensor, firmeza);
        atacante.consumirFolegoCombate(custo);
        recargaAte.put(atacante, tick + RECARGA_ACAO_TECNICA_TICKS);
        return ultimaDisputa;
    }

    /** Ticks que faltam para o lutador poder fazer outra acao tecnica (0 = livre). */
    public int getRecargaRestante(IrmaoMiau lutador) {
        Long ate = recargaAte.get(lutador);
        return ate == null ? 0 : (int) Math.max(0, ate - tick);
    }

    /** Ultima disputa resolvida (null se nenhuma). Para HUD de debug e testes. */
    public ResultadoDisputa getUltimaDisputa() {
        return ultimaDisputa;
    }

    private void validarDoCombate(IrmaoMiau lutador) {
        if (lutador != lutadorA && lutador != lutadorB) {
            throw new IllegalArgumentException("O lutador precisa pertencer a este SistemaCombate.");
        }
    }

    public ContextoCombate criarContexto(
            IrmaoMiau atacante,
            IrmaoMiau defensor
    ) {

        validarParticipantes(atacante, defensor);

        return new ContextoCombate(
                atacante,
                defensor
        );
    }

    public boolean estaNoAlcanceHorizontal(
            AcaoCombate acao,
            ContextoCombate contexto
    ) {

        validarAcaoEContexto(acao, contexto);

        return contexto.getDistanciaHorizontal()
                <= acao.getAlcanceHorizontal();
    }

    public boolean estaDentroDaDiferencaDeLinhas(
            AcaoCombate acao,
            ContextoCombate contexto
    ) {

        validarAcaoEContexto(acao, contexto);

        return contexto.getDiferencaLinhas()
                <= acao.getDiferencaMaximaLinhas();
    }

    public boolean atendeOrientacaoDaAcao(
            AcaoCombate acao,
            ContextoCombate contexto
    ) {

        validarAcaoEContexto(acao, contexto);

        if (!acao.isExigeFrenteParaOponente()) {
            return true;
        }

        return contexto.isAtacanteDeFrenteParaDefensor();
    }

    public boolean temFolegoSuficiente(
            AcaoCombate acao,
            ContextoCombate contexto
    ) {

        validarAcaoEContexto(acao, contexto);

        return contexto.getFolegoAtacante()
                >= acao.getCustoFolego();
    }

    /**
     * Evita iniciar exatamente a mesma interacao outra vez enquanto ela
     * ja esta ativa. Isso impede repetir a tecla e pagar o custo de novo.
     */
    private boolean acaoJaRepresentaInteracaoAtual(
            AcaoCombate acao,
            IrmaoMiau atacante,
            IrmaoMiau defensor
    ) {

        EstadoInteracaoCombate estadoAoSucesso
                = acao.getEstadoInteracaoAoSucesso();

        return estadoAoSucesso != null
                && estadoInteracao == estadoAoSucesso
                && aplicadorInteracao == atacante
                && receptorInteracao == defensor;
    }

    /**
     * Requisitos objetivos que existem nesta fase.
     *
     * Estados corporais especificos por tecnica, disputa, defesa e efeitos
     * mais ricos entram nas proximas fases.
     */
    public boolean podeTentarAcao(
            IrmaoMiau atacante,
            IrmaoMiau defensor,
            String idAcao
    ) {

        AcaoCombate acao = registroAcoes.buscar(idAcao);

        if (acao == null) {
            return false;
        }

        validarParticipantes(atacante, defensor);

        // Tecnicas PENDENTES existem no catalogo, mas nunca executam.
        if (!acao.isJogavel()) {
            return false;
        }

        if (isAnimacaoCombateAtiva()) {
            return false;
        }

        if (acaoJaRepresentaInteracaoAtual(
                acao,
                atacante,
                defensor
        )) {
            return false;
        }

        ContextoCombate contexto = new ContextoCombate(
                atacante,
                defensor
        );

        return estaNoAlcanceHorizontal(acao, contexto)
                && estaDentroDaDiferencaDeLinhas(acao, contexto)
                && atendeOrientacaoDaAcao(acao, contexto)
                && temFolegoSuficiente(acao, contexto);
    }

    /**
     * Primeira resolucao funcional de uma acao de combate.
     *
     * Nesta versao:
     * - requisitos invalidos -> INDISPONIVEL;
     * - requisitos validos   -> SUCESSO deterministico;
     * - o custo de folego e pago uma unica vez;
     * - se a acao define um estado compartilhado, ele e iniciado.
     *
     * Atributos, defesa e aleatoriedade entram depois que esta estrada
     * completa estiver validada no jogo.
     */
    public ResultadoAcao tentarAcao(
            IrmaoMiau atacante,
            IrmaoMiau defensor,
            String idAcao
    ) {

        validarParticipantes(atacante, defensor);

        ultimaAcaoId = idAcao;
        ultimoAtacante = atacante;

        AcaoCombate acao = registroAcoes.buscar(idAcao);

        if (acao == null
                || !podeTentarAcao(
                        atacante,
                        defensor,
                        idAcao
                )) {

            ultimoResultado = ResultadoAcao.INDISPONIVEL;
            return ultimoResultado;
        }

        if (!atacante.consumirFolegoCombate(
                acao.getCustoFolego()
        )) {

            ultimoResultado = ResultadoAcao.INDISPONIVEL;
            return ultimoResultado;
        }

        EstadoInteracaoCombate estadoAoSucesso
                = acao.getEstadoInteracaoAoSucesso();

        if (estadoAoSucesso != null) {
            iniciarInteracao(
                    estadoAoSucesso,
                    atacante,
                    defensor,
                    acao.getId()
            );
        }

        if (acao.possuiAnimacaoConjunta()) {
            iniciarAnimacaoCombate(
                    acao,
                    atacante,
                    defensor
            );
        } else if (estadoAoSucesso != null) {
            // Sem animacao de entrada: a interacao ja nasce estabelecida.
            estabelecerInteracao();
        }

        ultimoResultado = ResultadoAcao.SUCESSO;
        return ultimoResultado;
    }


    private void iniciarAnimacaoCombate(
            AcaoCombate acao,
            IrmaoMiau aplicador,
            IrmaoMiau receptor
    ) {

        cancelarAnimacaoCombateAtiva();

        // 5J: escolhe a variante uma vez, no inicio (nada muda no meio da tecnica).
        selecaoAnimacao = registroAnimacoes.contem(acao.getIdAnimacaoConjunta())
                ? registroAnimacoes.resolver(acao.getIdAnimacaoConjunta(), aplicador, receptor)
                : null;

        animacaoCombateAtiva = new AnimacaoCombateAtiva(
                acao.getIdAnimacaoConjunta(),
                aplicador,
                receptor,
                acao.getDuracaoAnimacaoFrames()
        );

        aplicador.setBloqueadoPorAnimacaoCombate(true);
        receptor.setBloqueadoPorAnimacaoCombate(true);
        aplicarQuadroDaAnimacao(); // Quadro 1 ja no frame inicial.
    }

    private void finalizarAnimacaoCombateAtiva() {

        if (animacaoCombateAtiva == null) {
            return;
        }

        IrmaoMiau aplicador = animacaoCombateAtiva.getAplicador();
        IrmaoMiau receptor = animacaoCombateAtiva.getReceptor();

        // Mantem o ultimo quadro enquanto a pegada estiver conquistada.
        // Bloqueio de movimento termina normalmente apos os 30 frames.
        boolean manterPose = estaEmInteracao()
                && aplicadorInteracao == aplicador
                && receptorInteracao == receptor;
        if (manterPose) {
            aplicador.setBloqueadoPorAnimacaoCombate(false);
            receptor.setBloqueadoPorAnimacaoCombate(false);
            // Entrada concluida: a partir daqui a PoliticaInteracao observa
            // as mudancas corporais em relacao a este instante.
            estabelecerInteracao();
        } else {
            aplicador.limparEstadoAnimacaoCombate();
            receptor.limparEstadoAnimacaoCombate();
        }

        animacaoCombateAtiva = null;
    }

    private void cancelarAnimacaoCombateAtiva() {

        if (animacaoCombateAtiva == null) {
            return;
        }

        animacaoCombateAtiva.finalizar();
        finalizarAnimacaoCombateAtiva();
    }

    private void iniciarInteracao(
            EstadoInteracaoCombate novoEstado,
            IrmaoMiau aplicador,
            IrmaoMiau receptor,
            String idAcao
    ) {

        if (novoEstado == null
                || novoEstado == EstadoInteracaoCombate.LIVRE) {

            throw new IllegalArgumentException(
                    "Uma interacao ativa precisa de um estado diferente de LIVRE."
            );
        }

        estadoInteracao = novoEstado;
        aplicadorInteracao = aplicador;
        receptorInteracao = receptor;
        idAcaoInteracao = idAcao;

        // Ainda nao estabelecida: depende do fim da animacao de entrada.
        referenciaAplicador = null;
        referenciaReceptor = null;
        firmeza = FIRMEZA_INICIAL;
        deslocouNesteTick = false;
    }

    private void validarParticipantes(
            IrmaoMiau atacante,
            IrmaoMiau defensor
    ) {

        if (atacante == null || defensor == null) {
            throw new IllegalArgumentException(
                    "Atacante e defensor nao podem ser nulos."
            );
        }

        if (atacante == defensor) {
            throw new IllegalArgumentException(
                    "Atacante e defensor precisam ser personagens diferentes."
            );
        }

        boolean atacantePertenceAoCombate
                = atacante == lutadorA || atacante == lutadorB;

        boolean defensorPertenceAoCombate
                = defensor == lutadorA || defensor == lutadorB;

        if (!atacantePertenceAoCombate
                || !defensorPertenceAoCombate) {

            throw new IllegalArgumentException(
                    "Os personagens precisam pertencer a este SistemaCombate."
            );
        }
    }

    private static void validarAcaoEContexto(
            AcaoCombate acao,
            ContextoCombate contexto
    ) {

        if (acao == null) {
            throw new IllegalArgumentException(
                    "A acao nao pode ser nula."
            );
        }

        if (contexto == null) {
            throw new IllegalArgumentException(
                    "O contexto nao pode ser nulo."
            );
        }
    }
}
