package com.irmaosmiau.combate;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Catalogo central das acoes conhecidas pelo motor de combate.
 *
 * Nesta fase os dados ainda sao cadastrados em Java. A API foi mantida
 * simples para que, no futuro, o conteudo possa vir de JSON sem obrigar o
 * SistemaCombate a conhecer onde ou como uma tecnica foi carregada.
 */
public final class RegistroAcoes {

    private final Map<String, AcaoCombate> acoes = new LinkedHashMap<>();

    public RegistroAcoes() {
        registrarAcoesPadrao();
    }

    private void registrarAcoesPadrao() {

        registrar(
                new AcaoCombate(
                        "pegada_1",
                        "Pegada 1",
                        CategoriaAcao.PEGADA,
                        EstadoInteracaoCombate.PEGADA_1,
                        /* alcance em unidades locais do atacante */
                        80.0,
                        1,
                        true,
                        3.0,
                        20.0,
                        0.50,
                        0.30,
                        0.20,
                        0.00
                )
        );
    }

    public void registrar(AcaoCombate acao) {

        if (acao == null) {
            throw new IllegalArgumentException("A acao nao pode ser nula.");
        }

        String id = normalizarId(acao.getId());

        if (acoes.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Ja existe uma acao registrada com o id: " + id
            );
        }

        acoes.put(id, acao);
    }

    public boolean contem(String id) {
        return id != null && acoes.containsKey(normalizarId(id));
    }

    public AcaoCombate buscar(String id) {

        if (id == null) {
            return null;
        }

        return acoes.get(normalizarId(id));
    }

    public Collection<AcaoCombate> listar() {
        return Collections.unmodifiableCollection(acoes.values());
    }

    public int quantidade() {
        return acoes.size();
    }

    private static String normalizarId(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
