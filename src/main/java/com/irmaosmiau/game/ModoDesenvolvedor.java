package com.irmaosmiau.game;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Entrega 5Q.4: sessao do MODO DESENVOLVEDOR (separada do treino). Mesmas regras,
 * mesmo motor e mesmos personagens de uma partida; J1 e J2 controlaveis; so a
 * politica de recursos muda: folego e/ou condicionamento ficam no MAXIMO NORMAL de
 * cada lutador, sem desgaste (IrmaoMiau.setRecursosInfinitos).
 *
 * Isolamento: a sessao e um objeto do Main para aquela luta (e para os reinicios
 * dela pelo F5); nenhum estado global. Sair para o menu descarta a sessao, e as
 * partidas e treinos normais nunca a recebem.
 */
public final class ModoDesenvolvedor {

    private boolean folegoInfinito = true;
    private boolean condicionamentoInfinito = true;
    private String cenario;

    /** Cenario escolhido pelo F4 (o F5 recomeca nele); null = o da selecao. */
    public String getCenario() {
        return cenario;
    }

    public void setCenario(String id) {
        cenario = id;
    }

    /** O cenario para recomecar: o do F4, se houve, ou o da selecao. */
    public String cenarioOu(String daSelecao) {
        return cenario != null ? cenario : daSelecao;
    }

    public boolean isFolegoInfinito() {
        return folegoInfinito;
    }

    public boolean isCondicionamentoInfinito() {
        return condicionamentoInfinito;
    }

    public void setFolegoInfinito(boolean v) {
        folegoInfinito = v;
    }

    public void setCondicionamentoInfinito(boolean v) {
        condicionamentoInfinito = v;
    }

    /** Aplica a politica atual aos lutadores da luta. */
    public void aplicar(IrmaoMiau... lutadores) {
        for (IrmaoMiau l : lutadores) {
            if (l != null) {
                l.setRecursosInfinitos(folegoInfinito, condicionamentoInfinito);
            }
        }
    }

    /** Texto do selo permanente. */
    public String selo() {
        return "MODO DESENVOLVEDOR · FÔLEGO " + (folegoInfinito ? "∞" : "NORMAL") + " · COND. "
                + (condicionamentoInfinito ? "∞" : "NORMAL");
    }
}
