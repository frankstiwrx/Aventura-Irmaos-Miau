package com.irmaosmiau.dispositivos;

import java.util.List;
import java.util.Optional;

/**
 * Sem leitura de gamepad (Entrega 5D): biblioteca ausente/desligada, sistema
 * nao suportado ou falha ao iniciar. O jogo segue so com o teclado.
 */
public final class ProvedorGamepadsAusente implements ProvedorGamepads {

    private final String motivo;

    public ProvedorGamepadsAusente(String motivo) {
        this.motivo = motivo == null ? "suporte a gamepad indisponível" : motivo;
    }

    @Override
    public List<InfoGamepad> conectados() {
        return List.of();
    }

    @Override
    public Optional<EstadoGamepad> estado(String chave) {
        return Optional.empty();
    }

    @Override
    public boolean isDisponivel() {
        return false;
    }

    @Override
    public String descricao() {
        return motivo;
    }

    @Override
    public void encerrar() {
        // nada a liberar
    }
}
