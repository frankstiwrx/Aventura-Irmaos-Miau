package com.irmaosmiau.dispositivos;

import java.util.List;
import java.util.Optional;

/**
 * Origem de leituras de gamepad (Entrega 5D): fisica (GLFW), simulada
 * (testes) ou ausente (sem biblioteca/hardware).
 *
 * Contrato: todos os metodos podem ser chamados de qualquer thread e
 * NUNCA bloqueiam (o jogo le a cada tick na thread do Swing).
 */
public interface ProvedorGamepads {

    /** Gamepads conectados agora, em ordem estavel (porta/ordem de conexao). */
    List<InfoGamepad> conectados();

    /** Ultima leitura do gamepad; vazio se ele nao estiver mais conectado. */
    Optional<EstadoGamepad> estado(String chave);

    /** true se a leitura fisica esta funcionando (mesmo sem nenhum controle conectado). */
    boolean isDisponivel();

    /** Texto para a tela de controles (ex.: "GLFW 3.5" ou o motivo da indisponibilidade). */
    String descricao();

    /** Libera recursos (thread, biblioteca nativa). Chamadas repetidas sao inofensivas. */
    void encerrar();

    /**
     * Entrega 5E (diagnostico): TODOS os joysticks presentes, inclusive os
     * sem mapeamento de gamepad. Padrao: os conectados, sem detalhes crus.
     */
    default List<DispositivoDetectado> detectados() {
        List<DispositivoDetectado> l = new java.util.ArrayList<>();
        int porta = 0;
        for (InfoGamepad g : conectados()) {
            l.add(new DispositivoDetectado(porta++, g.nome(), g.identidade(), true, g.nome(), -1, -1, -1));
        }
        return List.copyOf(l);
    }

    /** Se mapeamentos extras foram enviados, o GLFW os aceitou? (vazio = nenhum enviado) */
    default Optional<Boolean> mapeamentosAceitos() {
        return Optional.empty();
    }
}
