package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.SlotJogador;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Qual dispositivo cada slot usa (Entrega 5D). Imutavel.
 *
 * Os dois slots podem escolher TECLADO (cada um com suas teclas, como sempre)
 * ou GAMEPAD. Dois slots em GAMEPAD recebem gamepads DIFERENTES na partida
 * (SessaoDispositivos): um gamepad nunca controla os dois jogadores.
 */
public final class AssociacaoDispositivos {

    private final Map<SlotJogador, TipoDispositivo> tipos;

    private AssociacaoDispositivos(Map<SlotJogador, TipoDispositivo> tipos) {
        this.tipos = tipos;
    }

    /** Os dois no teclado: exatamente o comportamento anterior a 5D. */
    public static AssociacaoDispositivos padrao() {
        Map<SlotJogador, TipoDispositivo> m = new EnumMap<>(SlotJogador.class);
        for (SlotJogador s : SlotJogador.values()) {
            m.put(s, TipoDispositivo.TECLADO);
        }
        return new AssociacaoDispositivos(m);
    }

    public TipoDispositivo tipo(SlotJogador slot) {
        return tipos.get(slot);
    }

    public AssociacaoDispositivos com(SlotJogador slot, TipoDispositivo tipo) {
        if (slot == null || tipo == null) {
            throw new IllegalArgumentException("Slot e dispositivo sao obrigatorios.");
        }
        Map<SlotJogador, TipoDispositivo> m = new EnumMap<>(tipos);
        m.put(slot, tipo);
        return new AssociacaoDispositivos(m);
    }

    public boolean usaGamepad() {
        return tipos.containsValue(TipoDispositivo.GAMEPAD);
    }

    public boolean ehPadrao() {
        return equals(padrao());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AssociacaoDispositivos a && tipos.equals(a.tipos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipos);
    }

    @Override
    public String toString() {
        return "AssociacaoDispositivos" + tipos;
    }
}
