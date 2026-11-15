package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.LayoutTecladoControle;
import com.irmaosmiau.controles.SlotJogador;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Entrega 5O.1: estado de um "controle" feito de teclas (o J2 no teclado).
 *
 * Guarda as teclas do layout seguradas (LayoutTecladoControle) e as traduz
 * para um EstadoGamepad: botoes digitais, gatilhos (1,0 apertado) e os dois
 * analogicos (-1/0/1 por eixo; a diagonal tem modulo raiz de 2 e o LeitorAnalogico
 * normaliza). A FonteGamepad do slot le este estado como leria o Xbox, entao
 * zona morta, histerese, bordas, travas e familias sao exatamente as mesmas.
 */
public final class TecladoControle {

    private final SlotJogador slot;
    private final Map<String, String> layout;
    private final Set<String> seguradas = new LinkedHashSet<>();

    public TecladoControle(SlotJogador slot) {
        this.slot = slot;
        this.layout = LayoutTecladoControle.layout(slot);
    }

    public SlotJogador getSlot() {
        return slot;
    }

    /** Teclas do layout (nome do KeyStroke). */
    public Set<String> teclas() {
        return layout.keySet();
    }

    public void pressionar(String tecla) {
        if (layout.containsKey(tecla)) {
            seguradas.add(tecla);
        }
    }

    public void soltar(String tecla) {
        seguradas.remove(tecla);
    }

    /** Janela perdeu o foco: nada fica preso. */
    public void liberar() {
        seguradas.clear();
    }

    public boolean segurada(String tecla) {
        return seguradas.contains(tecla);
    }

    private boolean alvo(String a) {
        for (String t : seguradas) {
            if (a.equals(layout.get(t))) {
                return true;
            }
        }
        return false;
    }

    private float eixo(String menos, String mais) {
        return (alvo(mais) ? 1f : 0f) - (alvo(menos) ? 1f : 0f);
    }

    /** Leitura atual como se fosse um gamepad. */
    public EstadoGamepad estado() {
        boolean[] b = new boolean[BotaoGamepad.FISICOS];
        for (BotaoGamepad botao : BotaoGamepad.values()) {
            if (!botao.isVirtual()) {
                b[botao.ordinal()] = alvo(botao.name());
            }
        }
        float[] e = new float[EixoGamepad.values().length];
        e[EixoGamepad.ESQ_X.ordinal()] = eixo("LS_ESQUERDA", "LS_DIREITA");
        e[EixoGamepad.ESQ_Y.ordinal()] = eixo("LS_CIMA", "LS_BAIXO");
        e[EixoGamepad.DIR_X.ordinal()] = eixo("RS_ESQUERDA", "RS_DIREITA");
        e[EixoGamepad.DIR_Y.ordinal()] = eixo("RS_CIMA", "RS_BAIXO");
        e[EixoGamepad.GATILHO_ESQ.ordinal()] = alvo(BotaoGamepad.GATILHO_ESQ.name()) ? 1f : 0f;
        e[EixoGamepad.GATILHO_DIR.ordinal()] = alvo(BotaoGamepad.GATILHO_DIR.name()) ? 1f : 0f;
        return EstadoGamepad.de(b, e);
    }
}
