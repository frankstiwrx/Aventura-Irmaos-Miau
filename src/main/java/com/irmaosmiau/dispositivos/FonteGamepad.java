package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.input.TravaComandos;

import java.util.EnumSet;
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

    public FonteGamepad(SlotJogador slot, MapaGamepad mapa) {
        this.slot = slot;
        this.mapa = mapa;
    }

    /**
     * Uma leitura por tick.
     *
     * @param estado leitura atual; null = sem leitura (desconectado ou janela sem foco)
     */
    public void atualizar(EstadoGamepad estado) {
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
            pausaSegurada = pausa;            // START segurado ao voltar nao pausa
            travarNaProximaLeitura = false;
        }
        if (pausa && !pausaSegurada) {
            pausaTocada = true;
        }
        pausaSegurada = pausa;
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
