package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.CamadaComando;
import com.irmaosmiau.controles.PosicaoComando;
import com.irmaosmiau.controles.ResolvedorComandosContextuais;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.input.TravaComandos;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Acoes logicas de UM slot vindas de um gamepad (Entrega 5D).
 *
 * O gamepad e lido por estado (a cada tick), nao por eventos; as bordas
 * (apertou/soltou) sao calculadas aqui e alimentam a mesma TravaComandos do
 * teclado. Assim valem as mesmas regras: acao de toque uma vez por
 * pressionamento (no GamePanel), bloqueio da entrada da pegada (3A) e
 * liberacao na perda de foco ou desconexao.
 *
 * Seguranca:
 * - sem leitura (desconectado/sem foco) = nenhuma acao ativa;
 * - na primeira leitura depois de reconectar ou recuperar o foco, o que ja
 *   estiver segurado fica TRAVADO ate ser solto (nada "antigo" dispara).
 *
 * Entrega 5F: gatilhos com HISTERESE (apertado a partir de 0,5; solto so
 * abaixo de 0,35) e, no esquema CONTEXTUAL, o resolvedor das 3 camadas do
 * D-pad (um comando por toque), limpo na desconexao, no foco e na pausa.
 */
public final class FonteGamepad implements FonteAcoes {

    private final SlotJogador slot;
    private final MapaGamepad mapa;
    private final TravaComandos trava = new TravaComandos();

    private Set<AcaoJogador> brutas = EnumSet.noneOf(AcaoJogador.class);
    private Set<AcaoJogador> analogico = EnumSet.noneOf(AcaoJogador.class);
    private boolean pausaSegurada;
    private boolean pausaTocada;
    private boolean travarNaProximaLeitura;
    private final ResolvedorComandosContextuais contextual = new ResolvedorComandosContextuais();
    private final boolean[] gatilhoApertado = new boolean[2];

    /** Gatilho conta como apertado ate cair abaixo deste valor (histerese). */
    public static final float LIMIAR_SOLTAR_GATILHO = 0.35f;

    public FonteGamepad(SlotJogador slot, MapaGamepad mapa) {
        this.slot = slot;
        this.mapa = mapa;
    }

    /**
     * Uma leitura por tick.
     *
     * @param estado leitura atual; null = sem leitura (desconectado ou janela sem foco)
     */
    public void atualizar(EstadoGamepad estadoCru) {
        EstadoGamepad estado = estadoCru == null ? null : comHistereseNosGatilhos(estadoCru);
        if (estado == null) {
            gatilhoApertado[0] = false;
            gatilhoApertado[1] = false;
            contextual.limpar();
        }
        Set<AcaoJogador> novas;
        boolean pausa;
        if (estado == null) {
            novas = EnumSet.noneOf(AcaoJogador.class);
            analogico = EnumSet.noneOf(AcaoJogador.class);
            pausa = false;
        } else {
            Set<AcaoJogador> analogicoAtual = EnumSet.noneOf(AcaoJogador.class);
            novas = mapa.acoesAtivas(estado, slot, analogico, analogicoAtual);
            analogico = analogicoAtual;
            pausa = estado.pressionado(mapa.getBotaoPausa());
        }

        for (AcaoJogador a : AcaoJogador.values()) {
            boolean antes = brutas.contains(a);
            boolean agora = novas.contains(a);
            if (agora && !antes) {
                trava.pressionar(a.name());
            } else if (!agora && antes) {
                trava.soltar(a.name());
            }
        }
        brutas = novas;

        if (estado != null && travarNaProximaLeitura) {
            trava.travarPressionados("");
            contextual.limpar();
            pausaSegurada = pausa;            // START segurado ao voltar nao pausa
            travarNaProximaLeitura = false;
        }
        if (pausa && !pausaSegurada) {
            pausaTocada = true;
        }
        pausaSegurada = pausa;
        // Sem leitura nao alimenta o resolvedor: a limpeza continua exigindo uma soltura REAL do D-pad.
        if (estado != null) {
            contextual.atualizar(mapa.entradaContextual(estado, slot));
        }
    }

    /** Gatilho vira 1 (apertado) ou 0 (solto) com histerese; o resto fica igual. */
    private EstadoGamepad comHistereseNosGatilhos(EstadoGamepad e) {
        EstadoGamepad r = e;
        EixoGamepad[] eixos = {EixoGamepad.GATILHO_ESQ, EixoGamepad.GATILHO_DIR};
        for (int i = 0; i < 2; i++) {
            float v = e.eixo(eixos[i]);
            gatilhoApertado[i] = gatilhoApertado[i] ? v >= LIMIAR_SOLTAR_GATILHO : v >= EstadoGamepad.LIMIAR_GATILHO;
            r = r.comEixo(eixos[i], gatilhoApertado[i] ? 1f : 0f);
        }
        return r;
    }

    /** Comando contextual deste toque (esquema CONTEXTUAL), uma unica vez. */
    public Optional<PosicaoComando> consumirComandoContextual() {
        return contextual.consumir();
    }

    /** Camada segurada agora (HUD). */
    public CamadaComando camadaAtiva() {
        return contextual.getCamadaAtiva();
    }

    /** Pausa/menu: esquece comandos pendentes; nada dispara ate soltar o D-pad. */
    public void limparContextual() {
        contextual.limpar();
    }

    public MapaGamepad getMapa() {
        return mapa;
    }

    /** A proxima leitura trava o que estiver segurado (reconexao / foco de volta). */
    public void travarNaProximaLeitura() {
        travarNaProximaLeitura = true;
    }

    /** true uma unica vez por toque no botao de pausa. */
    public boolean consumirPausa() {
        boolean p = pausaTocada;
        pausaTocada = false;
        return p;
    }

    @Override
    public boolean isAtiva(AcaoJogador acao) {
        return brutas.contains(acao) && trava.isLivre(acao.name());
    }

    @Override
    public void travarPressionadas() {
        trava.travarPressionados("");
    }

    @Override
    public void liberarTodas() {
        trava.travarPressionados("");
        contextual.limpar();
        pausaTocada = false;
        travarNaProximaLeitura = true;
    }

    public SlotJogador getSlot() {
        return slot;
    }

    /** Alguma acao ou o botao de pausa segurado (para testes e diagnostico). */
    public boolean temAlgoSegurado() {
        return !brutas.isEmpty() || pausaSegurada;
    }
}
