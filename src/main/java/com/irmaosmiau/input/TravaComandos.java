package com.irmaosmiau.input;

import java.util.HashSet;
import java.util.Set;

/**
 * Impede que comandos segurados durante um bloqueio (ex.: os 30 frames da
 * entrada da PEGADA_1) sejam executados quando o bloqueio termina.
 *
 * Regra: um comando TRAVADO continua sendo lido como "solto" ate o jogador
 * soltar a tecla de verdade. Ao apertar de novo, volta a funcionar.
 *
 * Logica pura, sem Swing, para poder ser testada diretamente.
 */
public final class TravaComandos {

    private final Set<String> pressionados = new HashSet<>();
    private final Set<String> travados = new HashSet<>();

    public void pressionar(String comando) {
        pressionados.add(comando);
    }

    /** Soltar a tecla sempre destrava o comando. */
    public void soltar(String comando) {
        pressionados.remove(comando);
        travados.remove(comando);
    }

    /** Entrega 5C: janela perdeu o foco; nenhuma tecla continua segurada ou travada. */
    public void soltarTodos() {
        pressionados.clear();
        travados.clear();
    }

    /** Trava todos os comandos atualmente pressionados cujo nome comeca com o prefixo. */
    public void travarPressionados(String prefixo) {
        for (String comando : pressionados) {
            if (comando.startsWith(prefixo)) {
                travados.add(comando);
            }
        }
    }

    public boolean isLivre(String comando) {
        return !travados.contains(comando);
    }

    public boolean isTravado(String comando) {
        return travados.contains(comando);
    }
}
