package com.irmaosmiau.combate;

import com.irmaosmiau.entities.IrmaoMiau;

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
    // Entrega 5J: molde de quem aplica / recebe em cada animacao, e variantes de uma base.
    private final Map<String, IdentidadeRig[]> identidades = new LinkedHashMap<>();
    private final Map<String, java.util.List<String>> variantes = new LinkedHashMap<>();
    private final Map<String, AnimacaoConjuntaQuadros> composicoes = new java.util.HashMap<>();
    private final java.util.Set<String> experimentais = new java.util.HashSet<>();
    private final Map<String, String> posturas = new LinkedHashMap<>();

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
        registrarIdentidades("pegada_1_entrada", IdentidadeRig.BRANCO, IdentidadeRig.PRETO);
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

    // =====================================================
    // ENTREGA 5J: VARIANTES POR IDENTIDADE E PAPEL
    // =====================================================
    /** Para quais moldes a animacao foi desenhada (aplicador, receptor). */
    public void registrarIdentidades(String id, IdentidadeRig aplicador, IdentidadeRig receptor) {
        if (!contem(id) || aplicador == null || receptor == null) {
            throw new IllegalArgumentException("Identidades exigem uma animacao registrada e os dois moldes: " + id);
        }
        identidades.put(normalizar(id), new IdentidadeRig[] {aplicador, receptor});
    }

    /** Molde do aplicador desenhado na animacao (null = nao informado). */
    public IdentidadeRig getIdentidadeAplicador(String id) {
        IdentidadeRig[] i = id == null ? null : identidades.get(normalizar(id));
        return i == null ? null : i[0];
    }

    /** Molde do receptor desenhado na animacao (null = nao informado). */
    public IdentidadeRig getIdentidadeReceptor(String id) {
        IdentidadeRig[] i = id == null ? null : identidades.get(normalizar(id));
        return i == null ? null : i[1];
    }

    /**
     * Declara idVariante como outra versao autoral de idBase (outra dupla de
     * moldes). Exige o MESMO numero de quadros, a mesma duracao, a mesma
     * distribuicao temporal e o mesmo offset visual: assim trocar de variante
     * nunca muda o tempo da tecnica nem cria salto.
     */
    public void registrarVariante(String idBase, String idVariante) {
        AnimacaoConjuntaQuadros base = exigir(idBase);
        AnimacaoConjuntaQuadros v = exigir(idVariante);
        String b = normalizar(idBase);
        String nv = normalizar(idVariante);
        if (b.equals(nv) || variantes.containsKey(nv)) {
            throw new IllegalArgumentException("Variante invalida: " + idVariante + " de " + idBase);
        }
        // 5K: uma EXPERIMENTAL pode ter outros quadros (mesma duracao total); as autorais, nao.
        boolean mesmosQuadros = experimentais.contains(nv)
                || (base.quantidadeQuadros() == v.quantidadeQuadros()
                        && java.util.Arrays.equals(base.copiarDuracoesQuadros(), v.copiarDuracoesQuadros()));
        if (!mesmosQuadros
                || base.getDuracaoTotalFrames() != v.getDuracaoTotalFrames()
                || base.getOffsetVisualX() != v.getOffsetVisualX()
                || base.getOffsetVisualY() != v.getOffsetVisualY()) {
            throw new IllegalArgumentException("A variante " + idVariante + " precisa ter os mesmos quadros,"
                    + " duracao e offset visual de " + idBase + " (" + base.quantidadeQuadros() + " quadros, "
                    + base.getDuracaoTotalFrames() + " frames).");
        }
        if (getIdentidadeAplicador(nv) == null) {
            throw new IllegalArgumentException("A variante " + idVariante + " precisa declarar aplicador e receptor.");
        }
        variantes.computeIfAbsent(b, k -> new java.util.ArrayList<>()).add(nv);
    }

    /** Variantes declaradas para a base (sem a propria base). */
    public java.util.List<String> getVariantes(String idBase) {
        return java.util.List.copyOf(variantes.getOrDefault(normalizar(idBase), java.util.List.of()));
    }

    /**
     * Escolhe os quadros para esta dupla, pelo MOLDE de cada lutador e pelo
     * PAPEL (nunca pelo slot nem pelo lado da tela; o espelhamento horizontal
     * e feito pela orientacao de cada lutador ao desenhar).
     *
     * 1. Uma animacao desenhada para (molde do aplicador, molde do receptor): AUTORAL.
     * 2. Senao, cada trilha vem de uma animacao em que aquele molde tem aquele
     *    papel: COMPOSTA_POR_PAPEL (ex.: Branco x Branco).
     * 3. Senao: a base, como ate a 5I.
     */
    public SelecaoAnimacaoConjunta resolver(String idBase, IrmaoMiau aplicador, IrmaoMiau receptor) {
        return resolver(idBase, aplicador, receptor, ModoAnimacoesPegada.atual());
    }

    /**
     * Entrega 5K: com o modo de comparacao. No EXPERIMENTAL, primeiro as
     * variantes experimentais compativeis (molde, papel e postura do
     * aplicador); sem nenhuma, cai no caminho autoral de sempre. No AUTORAL,
     * as experimentais nunca sao escolhidas.
     */
    public SelecaoAnimacaoConjunta resolver(String idBase, IrmaoMiau aplicador, IrmaoMiau receptor,
            ModoAnimacoesPegada modo) {
        AnimacaoConjuntaQuadros base = exigir(idBase);
        String b = normalizar(idBase);
        IdentidadeRig ia = IdentidadeRig.de(aplicador);
        IdentidadeRig ir = IdentidadeRig.de(receptor);
        if (ia == null || ir == null) {
            return new SelecaoAnimacaoConjunta(b, b, b, base, SelecaoAnimacaoConjunta.Tipo.BASE, false);
        }
        java.util.List<String> todas = new java.util.ArrayList<>();
        todas.add(b);
        todas.addAll(variantes.getOrDefault(b, java.util.List.of()));
        String posturaAplicador = aplicador.isAjoelhado() ? "AJOELHADO" : "EM_PE";
        if (modo == ModoAnimacoesPegada.EXPERIMENTAL) {
            java.util.List<String> exp = new java.util.ArrayList<>();
            for (String c : todas) {
                if (experimentais.contains(c) && posturaCompativel(c, posturaAplicador)) {
                    exp.add(c);
                }
            }
            java.util.Optional<SelecaoAnimacaoConjunta> s = escolher(b, exp, ia, ir, true);
            if (s.isPresent()) {
                return s.get();
            }
        }
        java.util.List<String> autorais = new java.util.ArrayList<>();
        for (String c : todas) {
            if (!experimentais.contains(c) && posturaCompativel(c, posturaAplicador)) {
                autorais.add(c);
            }
        }
        return escolher(b, autorais, ia, ir, false)
                .orElse(new SelecaoAnimacaoConjunta(b, b, b, base, SelecaoAnimacaoConjunta.Tipo.BASE, false));
    }

    private boolean posturaCompativel(String id, String posturaAplicador) {
        String p = posturas.get(id);
        return p == null || p.equals(posturaAplicador);
    }

    /** Dupla exata (AUTORAL) ou composicao por papel entre animacoes compativeis. */
    private java.util.Optional<SelecaoAnimacaoConjunta> escolher(String b, java.util.List<String> candidatas,
            IdentidadeRig ia, IdentidadeRig ir, boolean experimental) {
        for (String c : candidatas) {
            if (getIdentidadeAplicador(c) == ia && getIdentidadeReceptor(c) == ir) {
                return java.util.Optional.of(new SelecaoAnimacaoConjunta(b, c, c, animacoes.get(c),
                        SelecaoAnimacaoConjunta.Tipo.AUTORAL, experimental));
            }
        }
        for (String ca : candidatas) {
            if (getIdentidadeAplicador(ca) != ia) {
                continue;
            }
            for (String cr : candidatas) {
                if (getIdentidadeReceptor(cr) != ir) {
                    continue;
                }
                AnimacaoConjuntaQuadros a = animacoes.get(ca);
                AnimacaoConjuntaQuadros r = animacoes.get(cr);
                // So compoe trilhas com o mesmo tempo (mesmos quadros e distribuicao).
                if (a.quantidadeQuadros() != r.quantidadeQuadros()
                        || !java.util.Arrays.equals(a.copiarDuracoesQuadros(), r.copiarDuracoesQuadros())) {
                    continue;
                }
                AnimacaoConjuntaQuadros composta = composicoes.computeIfAbsent(ca + "|" + cr, k -> compor(a, r));
                return java.util.Optional.of(new SelecaoAnimacaoConjunta(b, ca, cr, composta,
                        SelecaoAnimacaoConjunta.Tipo.COMPOSTA_POR_PAPEL, experimental));
            }
        }
        return java.util.Optional.empty();
    }

    // ---------------- Entrega 5K: metadados de experimentais e postura
    /** Marca a animacao como EXPERIMENTAL (so usada no modo de comparacao). */
    public void registrarExperimental(String id) {
        exigir(id);
        experimentais.add(normalizar(id));
    }

    public boolean isExperimental(String id) {
        return id != null && experimentais.contains(normalizar(id));
    }

    /** Postura do aplicador para a qual a animacao foi feita ("EM_PE" ou "AJOELHADO"). */
    public void registrarPostura(String id, String postura) {
        exigir(id);
        if (!"EM_PE".equals(postura) && !"AJOELHADO".equals(postura)) {
            throw new IllegalArgumentException("Postura invalida: " + postura + " (use EM_PE ou AJOELHADO)");
        }
        posturas.put(normalizar(id), postura);
    }

    public String getPostura(String id) {
        return id == null ? null : posturas.get(normalizar(id));
    }

    /** Trilha do aplicador de a + trilha do receptor de r (compatibilidade garantida no registro). */
    private static AnimacaoConjuntaQuadros compor(AnimacaoConjuntaQuadros a, AnimacaoConjuntaQuadros r) {
        int n = a.quantidadeQuadros();
        int[][] ta = new int[n][];
        int[][] tr = new int[n][];
        for (int i = 0; i < n; i++) {
            ta[i] = a.copiarQuadroAplicador(i);
            tr[i] = r.copiarQuadroReceptor(i);
        }
        return new AnimacaoConjuntaQuadros(ta, tr, a.getOffsetVisualX(), a.getOffsetVisualY(),
                a.getDuracaoTotalFrames(), a.copiarDuracoesQuadros());
    }

    private static String normalizar(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
