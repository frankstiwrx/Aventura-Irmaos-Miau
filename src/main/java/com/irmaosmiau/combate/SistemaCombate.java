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

    // Ultima tentativa: util para debug e, depois, feedback visual/sonoro.
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

    /**
     * Atualiza a fotografia compartilhada da luta e verifica se uma
     * interacao persistente ainda pode continuar espacialmente.
     */
    public void atualizar() {

        contextoAParaBAtual = new ContextoCombate(
                lutadorA,
                lutadorB
        );

        contextoBParaAAtual = new ContextoCombate(
                lutadorB,
                lutadorA
        );

        atualizarManutencaoDaInteracao();
        atualizarAnimacaoCombate();
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

        if (animacaoCombateAtiva.isFinalizada()) {
            finalizarAnimacaoCombateAtiva();
        }
    }

    private void atualizarManutencaoDaInteracao() {

        if (!estaEmInteracao()) {
            return;
        }

        AcaoCombate acao = registroAcoes.buscar(
                idAcaoInteracao
        );

        if (acao == null
                || aplicadorInteracao == null
                || receptorInteracao == null) {

            encerrarInteracao();
            return;
        }

        ContextoCombate contexto = criarContexto(
                aplicadorInteracao,
                receptorInteracao
        );

        /*
         * Depois que a pegada foi conquistada, o custo de folego nao e
         * reavaliado a cada frame. Para MANTER esta primeira interacao
         * verificamos somente os requisitos espaciais/orientacao.
         *
         * Quando o sistema de disputa entrar, romper uma pegada deixara de
         * depender apenas destes limites simples.
         */
        boolean aindaValida
                = estaNoAlcanceHorizontal(acao, contexto)
                && estaDentroDaDiferencaDeLinhas(acao, contexto)
                && atendeOrientacaoDaAcao(acao, contexto);

        if (!aindaValida) {
            encerrarInteracao();
        }
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

    public void encerrarInteracao() {

        cancelarAnimacaoCombateAtiva();

        estadoInteracao = EstadoInteracaoCombate.LIVRE;
        aplicadorInteracao = null;
        receptorInteracao = null;
        idAcaoInteracao = null;
    }

    /**
     * Cria uma fotografia nova do instante atual.
     */
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

        animacaoCombateAtiva = new AnimacaoCombateAtiva(
                acao.getIdAnimacaoConjunta(),
                aplicador,
                receptor,
                acao.getDuracaoAnimacaoFrames()
        );

        aplicador.setBloqueadoPorAnimacaoCombate(true);
        receptor.setBloqueadoPorAnimacaoCombate(true);
    }

    private void finalizarAnimacaoCombateAtiva() {

        if (animacaoCombateAtiva == null) {
            return;
        }

        IrmaoMiau aplicador = animacaoCombateAtiva.getAplicador();
        IrmaoMiau receptor = animacaoCombateAtiva.getReceptor();

        aplicador.limparEstadoAnimacaoCombate();
        receptor.limparEstadoAnimacaoCombate();

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
