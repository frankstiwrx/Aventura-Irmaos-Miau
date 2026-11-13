package com.irmaosmiau.hud;

import com.irmaosmiau.combate.AcaoCombate;
import com.irmaosmiau.combate.ContextoCombate;
import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Mensagens curtas para o jogador sobre o combate ("Pegada 1!",
 * "fora de alcance", "pegada desfeita: movimento").
 *
 * So OBSERVA o SistemaCombate: nao muda nenhuma regra. O relogio e em
 * ticks do jogo (60 por segundo), para ser deterministico nos testes.
 */
public final class FeedbackCombate {

    /** Quanto tempo uma mensagem fica na tela (~1,6 s). */
    public static final int DURACAO_MENSAGEM_TICKS = 96;

    /** Tipo visual da mensagem. */
    public enum Tipo {
        SUCESSO,
        AVISO,
        INFO
    }

    private String mensagem;
    private Tipo tipo;
    private long expiraNoTick = -1;

    private boolean estavaEmInteracao;

    // =====================================================
    // EVENTOS
    // =====================================================
    /**
     * Registra uma tentativa de tecnica feita pelo jogador.
     * Em caso de INDISPONIVEL, explica o primeiro requisito que falhou.
     */
    public void registrarTentativa(SistemaCombate sistema, IrmaoMiau atacante, IrmaoMiau defensor,
            String idAcao, ResultadoAcao resultado, long tick) {

        AcaoCombate acao = sistema.getAcao(idAcao);
        String nome = acao == null ? idAcao : acao.getNome();

        if (resultado == ResultadoAcao.SUCESSO) {
            mostrar(nome + "!", Tipo.SUCESSO, tick);
            return;
        }
        mostrar(nome + ": " + motivoIndisponivel(sistema, atacante, defensor, acao), Tipo.AVISO, tick);
    }

    /**
     * Deve ser chamado uma vez por tick depois de sistema.atualizar().
     *
     * @return true se uma interacao terminou neste tick
     */
    public boolean observar(SistemaCombate sistema, long tick) {
        boolean emInteracao = sistema.estaEmInteracao();
        boolean terminou = estavaEmInteracao && !emInteracao;
        estavaEmInteracao = emInteracao;

        if (terminou) {
            mostrar("Pegada desfeita: " + texto(sistema.getUltimoMotivoEncerramento()), Tipo.INFO, tick);
        }
        return terminou;
    }

    /** Mensagem informativa avulsa (ex.: "Posições reiniciadas"). */
    public void mostrarInfo(String texto, long tick) {
        mostrar(texto, Tipo.INFO, tick);
    }

    /** Esquece o estado anterior (usado quando os lutadores sao recriados). */
    public void reiniciarObservacao() {
        estavaEmInteracao = false;
    }

    private void mostrar(String texto, Tipo t, long tick) {
        mensagem = texto;
        tipo = t;
        expiraNoTick = tick + DURACAO_MENSAGEM_TICKS;
    }

    // =====================================================
    // CONSULTAS
    // =====================================================
    /** Mensagem visivel neste tick, ou null. */
    public String getMensagem(long tick) {
        return tick < expiraNoTick ? mensagem : null;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** Linha de estado da interacao atual, ou null quando livre. */
    public static String textoEstadoInteracao(SistemaCombate sistema) {
        if (sistema == null || !sistema.estaEmInteracao()) {
            return null;
        }
        AcaoCombate acao = sistema.getAcao(sistema.getIdAcaoInteracao());
        String nome = acao == null ? String.valueOf(sistema.getEstadoInteracao()) : acao.getNome();
        return nome.toUpperCase() + (sistema.isInteracaoEstabelecida() ? " · ESTABELECIDA" : " · ENTRANDO");
    }

    /** Primeiro requisito que impede a tecnica, em linguagem de jogador. */
    static String motivoIndisponivel(SistemaCombate sistema, IrmaoMiau atacante, IrmaoMiau defensor,
            AcaoCombate acao) {
        if (acao == null) {
            return "técnica desconhecida";
        }
        if (!acao.isJogavel()) {
            return "ainda não disponível";
        }
        if (sistema.isAnimacaoCombateAtiva()) {
            return "aguarde a animação";
        }
        if (sistema.estaEmInteracao()
                && sistema.getAplicadorInteracao() == atacante
                && acao.getId().equals(sistema.getIdAcaoInteracao())) {
            return "já está aplicada";
        }
        ContextoCombate c = sistema.criarContexto(atacante, defensor);
        if (!sistema.estaNoAlcanceHorizontal(acao, c)) {
            return "fora de alcance";
        }
        if (!sistema.estaDentroDaDiferencaDeLinhas(acao, c)) {
            return "linha diferente";
        }
        if (!sistema.atendeOrientacaoDaAcao(acao, c)) {
            return "de costas";
        }
        if (!sistema.temFolegoSuficiente(acao, c)) {
            return "sem fôlego";
        }
        return "indisponível";
    }

    static String texto(MotivoEncerramentoInteracao motivo) {
        if (motivo == null) {
            return "-";
        }
        switch (motivo) {
            case ALCANCE: return "distância";
            case LINHAS: return "linhas";
            case ORIENTACAO: return "de costas";
            case MOVIMENTO: return "movimento";
            case PULO: return "pulo";
            case AGACHAMENTO: return "agachou";
            case DEITAR_OU_LEVANTAR: return "deitou/levantou";
            case ROLAMENTO: return "rolamento";
            case SPRAWL: return "sprawl";
            case ACAO_INDISPONIVEL: return "técnica indisponível";
            case ENCERRAMENTO_MANUAL: return "reinício";
            default: return motivo.name().toLowerCase();
        }
    }
}
