package com.irmaosmiau.controles;

import java.util.Optional;

/**
 * Transforma o que esta segurado em NO MAXIMO UM comando por toque
 * (Entrega 5F). Deterministico, sem relogio: so a sequencia de entradas.
 *
 * Regras:
 * 1. Dispara so na BORDA: a direcao acabou de ser apertada (nao estava no
 *    tick anterior) e e a UNICA direcao segurada. Duas direcoes juntas
 *    (diagonal do D-pad, dedo escorregando) nao disparam nada.
 * 2. A camada e a dos modificadores NESSE instante: LT+LB+dir > LT+dir > dir.
 * 3. Segurar nao repete. Trocar modificadores com a direcao segurada nao
 *    dispara outra coisa: e preciso soltar a direcao e apertar de novo.
 * 4. limpar() (perda de foco, desconexao, pausa): esquece o comando
 *    pendente e nada dispara ate todas as direcoes serem soltas.
 */
public final class ResolvedorComandosContextuais {

    private EntradaContextual anterior = EntradaContextual.NADA;
    private boolean esperandoSoltar;
    private PosicaoComando pendente;
    private CamadaComando camadaAtiva = CamadaComando.UM;

    /** Uma leitura por tick. */
    public void atualizar(EntradaContextual entrada) {
        EntradaContextual e = entrada == null ? EntradaContextual.NADA : entrada;
        camadaAtiva = e.camada();
        if (esperandoSoltar) {
            if (e.direcoes().isEmpty()) {
                esperandoSoltar = false;
            }
            anterior = e;
            return;
        }
        if (e.direcoes().size() == 1) {
            DirecaoTecnica d = e.direcoes().iterator().next();
            if (!anterior.direcoes().contains(d) && anterior.direcoes().isEmpty()) {
                pendente = new PosicaoComando(e.camada(), d);
            }
        }
        anterior = e;
    }

    /** O comando deste toque, uma unica vez. */
    public Optional<PosicaoComando> consumir() {
        PosicaoComando p = pendente;
        pendente = null;
        return Optional.ofNullable(p);
    }

    /** Camada que o jogador esta segurando agora (para o HUD). */
    public CamadaComando getCamadaAtiva() {
        return camadaAtiva;
    }

    public void limpar() {
        pendente = null;
        esperandoSoltar = true;
        camadaAtiva = CamadaComando.UM;
    }
}
