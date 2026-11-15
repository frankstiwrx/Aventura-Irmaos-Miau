package com.irmaosmiau.hud;

import com.irmaosmiau.combate.AcaoCombate;
import com.irmaosmiau.combate.ContextoCombate;
import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.ResultadoAcaoTecnica;
import com.irmaosmiau.combate.TipoDisputa;
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;

import java.util.ArrayList;
import java.util.List;

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
    private double firmezaAnterior = Double.NaN;

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
        if (resultado == ResultadoAcao.DEFENDIDO) {
            // Entrega 5L: o defensor evitou a pegada antes do contato.
            com.irmaosmiau.combate.EventoDefesa e = sistema.getUltimoEventoDefesa();
            mostrar(nome + ": EVITADA" + (e == null ? "" : " (" + e.detalhe().toLowerCase() + ")"), Tipo.AVISO, tick);
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

        // Entrega 5B: aviso unico ao cruzar o limiar de controle fraco.
        double firmeza = sistema.isInteracaoEstabelecida() ? sistema.getFirmeza() : Double.NaN;
        if (!Double.isNaN(firmeza) && !Double.isNaN(firmezaAnterior)
                && firmezaAnterior >= SistemaCombate.LIMIAR_CONTROLE_FRACO
                && firmeza < SistemaCombate.LIMIAR_CONTROLE_FRACO) {
            mostrar("CONTROLE ENFRAQUECENDO", Tipo.AVISO, tick);
        }
        firmezaAnterior = firmeza;

        if (terminou && sistema.getUltimoMotivoEncerramento() == MotivoEncerramentoInteracao.SEM_CONTROLE) {
            mostrar("PEGADA DESFEITA: SEM CONTROLE", Tipo.INFO, tick);
            return true;
        }

        // O rompimento tecnico ja mostrou "PEGADA ROMPIDA" no proprio tick.
        if (terminou && sistema.getUltimoMotivoEncerramento() != MotivoEncerramentoInteracao.ROMPIMENTO_TECNICO) {
            mostrar("Pegada desfeita: " + texto(sistema.getUltimoMotivoEncerramento()), Tipo.INFO, tick);
        }
        return terminou;
    }

    /**
     * Entrega 5A: resultado de uma acao tecnica (desequilibrio ou rompimento).
     */
    public void registrarAcaoTecnica(TipoDisputa tipo, ResultadoAcaoTecnica resultado, long tick) {
        boolean desequilibrio = tipo == TipoDisputa.DESEQUILIBRIO;
        String acao = desequilibrio ? "DESEQUILÍBRIO" : "ROMPIMENTO TÉCNICO";
        switch (resultado) {
            case SUCESSO:
                mostrar(desequilibrio ? "DESEQUILIBROU!" : "PEGADA ROMPIDA!", Tipo.SUCESSO, tick);
                break;
            case RESISTIDO:
                mostrar(desequilibrio ? "TENTATIVA DE DESEQUILÍBRIO: RESISTIU" : "DEFESA FALHOU", Tipo.AVISO, tick);
                break;
            case SEM_FOLEGO:
                mostrar(acao + ": SEM FÔLEGO", Tipo.AVISO, tick);
                break;
            case EM_RECARGA:
                mostrar(acao + ": aguarde", Tipo.INFO, tick);
                break;
            case SEM_PEGADA:
            default:
                mostrar(acao + ": só com a pegada estabelecida", Tipo.INFO, tick);
                break;
        }
    }

    /** Entrega 5B: resultado do reforco (Y). */
    public void registrarReforco(ResultadoAcaoTecnica resultado, long tick) {
        switch (resultado) {
            case SUCESSO:
                mostrar("PEGADA REFORÇADA", Tipo.SUCESSO, tick);
                break;
            case SEM_FOLEGO:
                mostrar("REFORÇO: SEM FÔLEGO", Tipo.AVISO, tick);
                break;
            case EM_RECARGA:
                mostrar("REFORÇO: AGUARDE", Tipo.INFO, tick);
                break;
            case SEM_EFEITO:
                mostrar("PEGADA JÁ ESTÁ FIRME", Tipo.INFO, tick);
                break;
            default:
                mostrar("REFORÇO: só com a pegada estabelecida", Tipo.INFO, tick);
                break;
        }
    }

    /**
     * Linha de estado: interacao (se houver) + efeitos ativos, por exemplo
     * "PEGADA 1 · ESTABELECIDA · Irmão Miau Preto DESEQUILIBRADO".
     */
    public static String textoEstado(SistemaCombate sistema, List<IrmaoMiau> lutadores) {
        List<String> efeitos = new ArrayList<>();
        for (IrmaoMiau l : lutadores) {
            if (l != null && l.getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO)) {
                efeitos.add(l.getNome() + " DESEQUILIBRADO");
            }
        }
        String interacao = textoEstadoInteracao(sistema);
        if (efeitos.isEmpty()) {
            return interacao;
        }
        // Com efeito ativo, a interacao aparece abreviada para caber no centro.
        List<String> partes = new ArrayList<>();
        if (interacao != null) {
            partes.add(interacao.split(" · ")[0]);
        }
        partes.addAll(efeitos);
        return String.join(" · ", partes);
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
        // Entrega 5B: estabelecida mostra a firmeza ("PEGADA 1 · FIRMEZA 85%").
        return nome.toUpperCase() + (sistema.isInteracaoEstabelecida()
                ? " · FIRMEZA " + Math.round(sistema.getFirmeza()) + "%"
                : " · ENTRANDO");
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
            case ROMPIMENTO_TECNICO: return "rompimento técnico";
            case SEM_CONTROLE: return "sem controle";
            case ENCERRAMENTO_MANUAL: return "reinício";
            default: return motivo.name().toLowerCase();
        }
    }
}
