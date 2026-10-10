package com.irmaosmiau.combate;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Coordenador das regras compartilhadas entre dois lutadores.
 *
 * Nesta primeira versao o sistema apenas consulta o contexto e valida
 * requisitos basicos da AcaoCombate. Ele ainda nao consome energia, nao
 * aplica efeitos, nao toca animacao e nao usa probabilidade.
 */
public final class SistemaCombate {

    private final IrmaoMiau lutadorA;
    private final IrmaoMiau lutadorB;
    private final RegistroAcoes registroAcoes;

    /*
     * Fotografias do frame mais recente. Elas servem para observacao,
     * debug e futuras regras que precisem consultar o estado compartilhado
     * da luta sem recalcular a mesma fotografia varias vezes no mesmo frame.
     *
     * Os metodos que avaliam uma tentativa de acao continuam livres para
     * criar um ContextoCombate novo no instante exato da tentativa.
     */
    private ContextoCombate contextoAParaBAtual;
    private ContextoCombate contextoBParaAAtual;

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
     * Atualiza a fotografia compartilhada da luta para o frame atual.
     *
     * Nesta etapa o metodo apenas observa os dois personagens. Ele nao
     * movimenta, nao gasta folego, nao aplica efeito e nao toca animacao.
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

    /**
     * Cria uma fotografia nova do instante atual.
     *
     * Mesmo existindo uma fotografia do frame em atualizar(), uma tentativa
     * de acao pode pedir um contexto novo para garantir que a avaliacao use
     * exatamente o estado daquele instante.
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

    public boolean temFolegoSuficiente(
            AcaoCombate acao,
            ContextoCombate contexto
    ) {

        validarAcaoEContexto(acao, contexto);

        return contexto.getFolegoAtacante()
                >= acao.getCustoFolego();
    }

    /**
     * Requisitos objetivos que ja existem nesta etapa do motor.
     *
     * Estados especificos de cada tecnica, posicoes de grappling, efeitos e
     * defesa entram nas proximas fases. Nao bloqueamos "DEITADO" de forma
     * generica aqui porque futuras acoes de chao precisam usar o mesmo motor.
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

        ContextoCombate contexto = criarContexto(
                atacante,
                defensor
        );

        return estaNoAlcanceHorizontal(acao, contexto)
                && estaDentroDaDiferencaDeLinhas(acao, contexto)
                && temFolegoSuficiente(acao, contexto);
    }

    /**
     * Scaffold da futura resolucao de combate.
     *
     * Por enquanto:
     * - requisitos invalidos -> INDISPONIVEL;
     * - requisitos validos   -> SUCESSO.
     *
     * SUCESSO ainda nao provoca efeitos. O objetivo e validar o pipeline
     * antes de introduzir RNG, atributos, defesa e animacoes.
     */
    public ResultadoAcao tentarAcao(
            IrmaoMiau atacante,
            IrmaoMiau defensor,
            String idAcao
    ) {

        if (!podeTentarAcao(
                atacante,
                defensor,
                idAcao
        )) {
            return ResultadoAcao.INDISPONIVEL;
        }

        return ResultadoAcao.SUCESSO;
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

        boolean atacantePertenceAoCombate =
                atacante == lutadorA || atacante == lutadorB;

        boolean defensorPertenceAoCombate =
                defensor == lutadorA || defensor == lutadorB;

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
