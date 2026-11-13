package com.irmaosmiau.controles;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Borda de subida das acoes de TOQUE (Entrega 5C): "uma acao por
 * pressionamento", sem repetir enquanto a tecla continua segurada.
 * Substitui os campos xxxAnterior das acoes de combate do GamePanel.
 */
public final class DetectorToques {

    private final Map<SlotJogador, Set<AcaoJogador>> anteriores = new EnumMap<>(SlotJogador.class);

    public DetectorToques() {
        for (SlotJogador s : SlotJogador.values()) {
            anteriores.put(s, EnumSet.noneOf(AcaoJogador.class));
        }
    }

    /** true somente no tick em que a acao passou de solta para ativa. */
    public boolean tocou(SlotJogador slot, AcaoJogador acao, boolean ativaAgora) {
        boolean antes = anteriores.get(slot).contains(acao);
        sincronizar(slot, acao, ativaAgora);
        return ativaAgora && !antes;
    }

    /** Registra o estado sem disparar (ex.: o que ja estava segurado ao reiniciar). */
    public void sincronizar(SlotJogador slot, AcaoJogador acao, boolean ativaAgora) {
        if (ativaAgora) {
            anteriores.get(slot).add(acao);
        } else {
            anteriores.get(slot).remove(acao);
        }
    }
}
