package com.irmaosmiau.combate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dados visuais separados das regras e dos atributos do combate.
 *
 * Duas formas de uso:
 * - {@code new RegistroAnimacoesCombate()}: registro LEGADO com a PEGADA_1
 *   em arrays Java (comportamento original, protegido pelo teste golden);
 * - {@link #vazio()} + {@link #registrar}: registro preenchido por conteudo,
 *   por exemplo pelo CarregadorConteudoCombate a partir de JSON do MiauStudio.
 */
public final class RegistroAnimacoesCombate {

    /** Origem registrada quando nenhuma e informada (arrays em Java). */
    public static final String ORIGEM_CODIGO_JAVA = "codigo Java (RegistroAnimacoesCombate)";

    private final Map<String, AnimacaoConjuntaQuadros> animacoes = new LinkedHashMap<>();
    private final Map<String, String> origens = new LinkedHashMap<>();

    public RegistroAnimacoesCombate() {
        // Dados gerados a partir de PEGADA_1.json, MiauStudio projeto v2.
        // Aplicador: Branco; receptor: Preto; centro de autoria (360, linha 2).
        registrar("pegada_1_entrada", new AnimacaoConjuntaQuadros(
            new int[][] {
                {56, 0, 7, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {56, 0, 7, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 38, 0, 60, 3, -10, 12, -14, 26, 0, 0, 0, 0, 0, 0, 0, 0},
                {56, 0, 7, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 13, 119, -25, -10, 12, -14, 26, 7, -2, 14, 3, 11, -6, 0, 0}
            },
            new int[][] {
                {140, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {140, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -12, -12, -15, -9, 0, 0, 0, 0, 0, 0, 0, 0},
                {125, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, -6, 0, 0, 21, -11, 8, 10, -23, -18, -28, 0, 14, 0, 27, 0, 15, -1, 0, 0}
            },
            0.0, 0.0
        ));
    }

    private RegistroAnimacoesCombate(boolean semPadrao) {
        // Registro vazio, para conteudo carregado de arquivos.
    }

    /** Registro sem nenhuma animacao pre-cadastrada. */
    public static RegistroAnimacoesCombate vazio() {
        return new RegistroAnimacoesCombate(true);
    }

    /**
     * Cadastra uma animacao.
     *
     * @throws IllegalArgumentException se o id for vazio, a sequencia nula
     *         ou ja existir uma animacao com o mesmo id
     */
    public void registrar(String id, AnimacaoConjuntaQuadros sequencia) {
        registrar(id, sequencia, ORIGEM_CODIGO_JAVA);
    }

    /**
     * Cadastra uma animacao informando de onde ela veio (ex.: caminho do
     * projeto MiauStudio). A origem serve para diagnostico e testes.
     */
    public void registrar(String id, AnimacaoConjuntaQuadros sequencia, String origem) {
        if (origem == null || origem.isBlank()) {
            throw new IllegalArgumentException("A origem da animacao e obrigatoria.");
        }
        if (id == null || id.isBlank() || sequencia == null) {
            throw new IllegalArgumentException("ID e sequencia sao obrigatorios.");
        }
        String normalizado = normalizar(id);
        if (animacoes.containsKey(normalizado)) {
            throw new IllegalArgumentException(
                    "Ja existe uma animacao de combate registrada com o id: " + normalizado);
        }
        animacoes.put(normalizado, sequencia);
        origens.put(normalizado, origem.trim());
    }

    /** De onde a animacao veio, ou null se o id nao existe. */
    public String getOrigem(String id) {
        return id == null ? null : origens.get(normalizar(id));
    }

    public AnimacaoConjuntaQuadros buscar(String id) {
        return id == null ? null : animacoes.get(normalizar(id));
    }

    /** Como {@link #buscar}, mas com erro claro quando o id nao existe. */
    public AnimacaoConjuntaQuadros exigir(String id) {
        AnimacaoConjuntaQuadros a = buscar(id);
        if (a == null) {
            throw new IllegalArgumentException(
                    "Animacao de combate nao registrada: " + id
                    + " (registradas: " + animacoes.keySet() + ")");
        }
        return a;
    }

    public boolean contem(String id) {
        return buscar(id) != null;
    }

    public int quantidade() {
        return animacoes.size();
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(animacoes.keySet());
    }

    private static String normalizar(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
