package com.irmaosmiau.cenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catalogo de cenarios por id.
 *
 * Para um cenario novo: implementar {@link Cenario} e registrar aqui
 * (ou num registro proprio). O GamePanel so conhece o contrato.
 */
public final class RegistroCenarios {

    /** Cenario usado quando ninguem escolhe outro. */
    public static final String ID_PADRAO = CenarioDojo.ID;

    private final Map<String, Cenario> cenarios = new LinkedHashMap<>();

    /** Registro vazio. */
    public RegistroCenarios() {
    }

    /** Dojo (padrao), campo original, campo remasterizado, arena do torneio (5O.4), academia e fortaleza nevada (5P.3). */
    public static RegistroCenarios padrao() {
        RegistroCenarios r = new RegistroCenarios();
        r.registrar(new CenarioDojo());
        r.registrar(new CenarioOriginal());
        // 5O.4: cenarios novos (os dois antigos continuam iguais e na mesma ordem).
        r.registrar(new CenarioCampoRemaster());
        r.registrar(new CenarioArenaTorneio());
        // 5P.3: academia de jiu-jitsu e fortaleza nevada ao luar (depois dos quatro anteriores).
        r.registrar(new CenarioAcademia());
        r.registrar(new CenarioFortalezaNevada());
        return r;
    }

    public void registrar(Cenario cenario) {
        if (cenario == null || cenario.getId() == null || cenario.getId().isBlank()) {
            throw new IllegalArgumentException("O cenario precisa de um id.");
        }
        String id = normalizar(cenario.getId());
        if (cenarios.containsKey(id)) {
            throw new IllegalArgumentException("Ja existe um cenario com o id: " + id);
        }
        cenarios.put(id, cenario);
    }

    public Cenario buscar(String id) {
        return id == null ? null : cenarios.get(normalizar(id));
    }

    public Cenario exigir(String id) {
        Cenario c = buscar(id);
        if (c == null) {
            throw new IllegalArgumentException("Cenario nao registrado: " + id
                    + " (registrados: " + cenarios.keySet() + ")");
        }
        return c;
    }

    public boolean contem(String id) {
        return buscar(id) != null;
    }

    public int quantidade() {
        return cenarios.size();
    }

    public List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<>(cenarios.keySet()));
    }

    /** Proximo cenario na ordem de cadastro (circular). */
    public Cenario proximo(Cenario atual) {
        List<Cenario> lista = new ArrayList<>(cenarios.values());
        if (lista.isEmpty()) {
            throw new IllegalStateException("Nenhum cenario registrado.");
        }
        int i = atual == null ? -1 : lista.indexOf(atual);
        return lista.get((i + 1) % lista.size());
    }

    private static String normalizar(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
