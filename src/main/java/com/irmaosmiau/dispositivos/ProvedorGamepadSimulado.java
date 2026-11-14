package com.irmaosmiau.dispositivos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gamepads "de mentira" (Entrega 5D): testes e desenvolvimento sem hardware.
 * Conectar, desconectar e mudar o estado acontecem na hora.
 */
public final class ProvedorGamepadSimulado implements ProvedorGamepads {

    private final Map<String, InfoGamepad> conectados = new LinkedHashMap<>();
    private final Map<String, EstadoGamepad> estados = new LinkedHashMap<>();
    private final List<DispositivoDetectado> semMapeamento = new ArrayList<>();

    /** Conecta um gamepad simulado em repouso e devolve sua chave. */
    public synchronized InfoGamepad conectar(String chave, String identidade, String nome) {
        InfoGamepad info = new InfoGamepad(chave, identidade, nome);
        conectados.put(chave, info);
        estados.put(chave, EstadoGamepad.repouso());
        return info;
    }

    /**
     * Entrega 5E: joystick que o sistema mostra, mas sem mapeamento de
     * gamepad (o jogo nao o usa; so o diagnostico o lista).
     */
    public synchronized void conectarSemMapeamento(int porta, String nome, String guid, int eixos, int botoes,
            int direcionais) {
        semMapeamento.add(new DispositivoDetectado(porta, nome, guid, false, null, eixos, botoes, direcionais));
    }

    @Override
    public synchronized List<DispositivoDetectado> detectados() {
        List<DispositivoDetectado> l = new ArrayList<>(ProvedorGamepads.super.detectados());
        l.addAll(semMapeamento);
        return List.copyOf(l);
    }

    public synchronized void desconectar(String chave) {
        conectados.remove(chave);
        estados.remove(chave);
    }

    public synchronized void definir(String chave, EstadoGamepad estado) {
        if (!conectados.containsKey(chave)) {
            throw new IllegalStateException("Gamepad simulado nao conectado: " + chave);
        }
        estados.put(chave, estado);
    }

    @Override
    public synchronized List<InfoGamepad> conectados() {
        return List.copyOf(new ArrayList<>(conectados.values()));
    }

    @Override
    public synchronized Optional<EstadoGamepad> estado(String chave) {
        return Optional.ofNullable(estados.get(chave));
    }

    @Override
    public boolean isDisponivel() {
        return true;
    }

    @Override
    public String descricao() {
        return "gamepad simulado";
    }

    @Override
    public synchronized void encerrar() {
        conectados.clear();
        estados.clear();
        semMapeamento.clear();
    }
}
