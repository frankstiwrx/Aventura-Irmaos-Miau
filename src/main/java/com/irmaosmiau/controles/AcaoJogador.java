package com.irmaosmiau.controles;

/**
 * Acoes LOGICAS do jogador (Entrega 5C): o jogo e o motor reagem a estas
 * acoes, nunca a uma tecla fisica especifica.
 *
 * Tecla fisica -> AcaoJogador -> interpretacao do estado (GamePanel /
 * AcoesContextuais) -> sistema existente (IrmaoMiau, SistemaCombate).
 *
 * Cada constante registra, num lugar so:
 * - sufixoComando: nome do comando no ActionMap/TravaComandos ("branco" + sufixo),
 *   o mesmo usado desde a Entrega 1 (compatibilidade com testes e com a trava);
 * - rotulo e verbo: textos da tela de controles e da ajuda;
 * - grupo e tipo de acionamento;
 * - tecla padrao de cada jogador (nome do KeyStroke do Swing). null = o
 *   slot nao usa esta acao (ex.: o Jogador 2 ainda nao aplica pegada).
 *
 * Para adicionar uma acao: nova constante aqui + leitura no GamePanel.
 * Arquivos controles.json antigos recebem a tecla padrao automaticamente
 * (ver RepositorioControles).
 */
public enum AcaoJogador {

    // ---------------- MOVIMENTO ----------------
    MOVER_ESQUERDA("Esquerda", "Mover para a esquerda", "esquerda",
            GrupoAcao.MOVIMENTO, TipoAcionamento.CONTINUA, "A", "LEFT"),
    MOVER_DIREITA("Direita", "Mover para a direita", "direita",
            GrupoAcao.MOVIMENTO, TipoAcionamento.CONTINUA, "D", "RIGHT"),
    MOVER_CIMA("Cima", "Mover para cima (linha de trás)", "cima",
            GrupoAcao.MOVIMENTO, TipoAcionamento.CONTINUA, "W", "UP"),
    MOVER_BAIXO("Baixo", "Mover para baixo (linha da frente)", "baixo",
            GrupoAcao.MOVIMENTO, TipoAcionamento.CONTINUA, "S", "DOWN"),
    PULAR("Pular", "Pular", "pular",
            GrupoAcao.MOVIMENTO, TipoAcionamento.TOQUE, "SPACE", "N"),
    PULO_CURTO("PularCurto", "Pulo curto", "pulo curto",
            GrupoAcao.MOVIMENTO, TipoAcionamento.TOQUE, "V", "B"),
    AGACHAR("Agachar", "Agachar", "agachar",
            GrupoAcao.MOVIMENTO, TipoAcionamento.CONTINUA, "C", "M"),
    CORRER("Correr", "Correr", "correr",
            GrupoAcao.MOVIMENTO, TipoAcionamento.CONTINUA, "Z", "K"),
    ROLAR("Rolar", "Rolar", "rolar",
            GrupoAcao.MOVIMENTO, TipoAcionamento.TOQUE, "R", "O"),
    DEITAR_LEVANTAR("Deitar", "Deitar / levantar", "deitar",
            GrupoAcao.MOVIMENTO, TipoAcionamento.TOQUE, "F", "J"),
    RESPIRAR("Respirar", "Respirar (recuperar fôlego)", "respirar",
            GrupoAcao.MOVIMENTO, TipoAcionamento.CONTINUA, "X", "L"),

    // ---------------- COMBATE ----------------
    INICIAR_PEGADA("Pegada1", "Iniciar pegada (PEGADA 1)", "pegar",
            GrupoAcao.COMBATE, TipoAcionamento.TOQUE, "H", null),
    TENTAR_DESEQUILIBRIO("Desequilibrio", "Desequilibrar (na pegada)", "desequilibrar",
            GrupoAcao.COMBATE, TipoAcionamento.TOQUE, "T", null),
    REFORCAR_PEGADA("Reforco", "Reforçar a pegada", "reforçar",
            GrupoAcao.COMBATE, TipoAcionamento.TOQUE, "Y", null),
    MODO_DESLOCAMENTO_CONTROLADO("Controle", "Modo controle (+ esquerda/direita = arrastar)", "arrastar",
            GrupoAcao.COMBATE, TipoAcionamento.MODIFICADOR, "SHIFT", null),

    // ---------------- DEFESA ----------------
    SPRAWL("Sprawl", "Sprawl", "sprawl",
            GrupoAcao.DEFESA, TipoAcionamento.TOQUE, "G", "I"),
    ROMPIMENTO_TECNICO("Rompimento", "Romper a pegada (rompimento técnico)", "romper",
            GrupoAcao.DEFESA, TipoAcionamento.TOQUE, null, "U");

    private final String sufixoComando;
    private final String rotulo;
    private final String verbo;
    private final GrupoAcao grupo;
    private final TipoAcionamento acionamento;
    private final String padraoJogador1;
    private final String padraoJogador2;

    AcaoJogador(String sufixoComando, String rotulo, String verbo, GrupoAcao grupo,
            TipoAcionamento acionamento, String padraoJogador1, String padraoJogador2) {
        this.sufixoComando = sufixoComando;
        this.rotulo = rotulo;
        this.verbo = verbo;
        this.grupo = grupo;
        this.acionamento = acionamento;
        this.padraoJogador1 = padraoJogador1;
        this.padraoJogador2 = padraoJogador2;
    }

    public String getSufixoComando() {
        return sufixoComando;
    }

    /** Texto completo (tela de controles). */
    public String getRotulo() {
        return rotulo;
    }

    /** Verbo curto (faixa de dicas e ajuda contextual). */
    public String getVerbo() {
        return verbo;
    }

    public GrupoAcao getGrupo() {
        return grupo;
    }

    public TipoAcionamento getAcionamento() {
        return acionamento;
    }

    /** Nome da tecla padrao no slot, ou null se o slot nao usa a acao. */
    String nomeTeclaPadrao(SlotJogador slot) {
        return slot == SlotJogador.JOGADOR_1 ? padraoJogador1 : padraoJogador2;
    }
}
