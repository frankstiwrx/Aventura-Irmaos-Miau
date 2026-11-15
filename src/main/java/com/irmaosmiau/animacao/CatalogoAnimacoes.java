package com.irmaosmiau.animacao;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Todas as animacoes conhecidas (Entrega 5E): legadas (fallback, codigo
 * atual), JSON individuais e a conjunta PEGADA_1. Registrar uma animacao
 * aqui NAO muda o jogo: quem escolhe e o PerfilAnimacoes de cada personagem.
 */
public final class CatalogoAnimacoes {

    public static final String RECURSO_BRANCO_CHAO = "/conteudo/animacoes/individuais/branco_movimento_chao.json";
    public static final String RECURSO_PRETO_CHAO = "/conteudo/animacoes/individuais/preto_movimento_chao.json";
    // Entrega 5J: postura de joelhos (fallback procedural em JSON, substituivel pelo MiauStudio).
    public static final String RECURSO_BRANCO_AJOELHADO = "/conteudo/animacoes/individuais/branco_ajoelhado.json";
    public static final String RECURSO_PRETO_AJOELHADO = "/conteudo/animacoes/individuais/preto_ajoelhado.json";
    public static final String RECURSO_BRANCO_AJOELHADO_MOV =
            "/conteudo/animacoes/individuais/branco_ajoelhado_movimento.json";
    public static final String RECURSO_PRETO_AJOELHADO_MOV =
            "/conteudo/animacoes/individuais/preto_ajoelhado_movimento.json";

    private final Map<String, DefinicaoAnimacao> porId = new LinkedHashMap<>();

    public CatalogoAnimacoes registrar(DefinicaoAnimacao d) {
        if (porId.putIfAbsent(d.id(), d) != null) {
            throw new IllegalArgumentException("Animacao repetida: " + d.id());
        }
        return this;
    }

    public Optional<DefinicaoAnimacao> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }

    public DefinicaoAnimacao exigir(String id) {
        return buscar(id).orElseThrow(() -> new IllegalArgumentException("Animacao nao cadastrada: " + id));
    }

    public Collection<DefinicaoAnimacao> todas() {
        return porId.values();
    }

    /** Legadas do Branco e do Preto + JSON migradas + conjuntas (JSON ausente = erro). */
    public static CatalogoAnimacoes padrao() {
        CatalogoAnimacoes c = new CatalogoAnimacoes();
        registrarLegadas(c);
        c.registrar(DefinicaoAnimacao.json("branco.movimento_chao.json", RECURSO_BRANCO_CHAO,
                CarregadorAnimacaoJson.doRecurso(RECURSO_BRANCO_CHAO)));
        c.registrar(DefinicaoAnimacao.json("preto.movimento_chao.json", RECURSO_PRETO_CHAO,
                CarregadorAnimacaoJson.doRecurso(RECURSO_PRETO_CHAO)));
        for (java.util.Map.Entry<String, String> e : new java.util.TreeMap<>(JSON_AJOELHADO).entrySet()) {
            c.registrar(DefinicaoAnimacao.json(e.getKey(), e.getValue(), CarregadorAnimacaoJson.doRecurso(e.getValue())));
        }
        registrarConjuntas(c);
        return c;
    }

    /** As animacoes JSON migradas: id no catalogo -> recurso (Entrega 5G). */
    public static final java.util.Map<String, String> JSON_MIGRADAS = java.util.Map.of(
            "branco.movimento_chao.json", RECURSO_BRANCO_CHAO,
            "preto.movimento_chao.json", RECURSO_PRETO_CHAO);

    /** Entrega 5J: poses de joelhos (fallback em JSON; o usuario pode substituir pelo MiauStudio). */
    public static final java.util.Map<String, String> JSON_AJOELHADO = java.util.Map.of(
            "branco.ajoelhado.json", RECURSO_BRANCO_AJOELHADO,
            "preto.ajoelhado.json", RECURSO_PRETO_AJOELHADO,
            "branco.ajoelhado_movimento.json", RECURSO_BRANCO_AJOELHADO_MOV,
            "preto.ajoelhado_movimento.json", RECURSO_PRETO_AJOELHADO_MOV);

    /**
     * Entrega 5G: catalogo do JOGO, tolerante. Um JSON ausente ou invalido NAO
     * derruba a partida: ele simplesmente nao entra no catalogo (o perfil
     * continua com a animacao legada) e o motivo vai UMA vez para o aviso.
     *
     * @param leitor recurso -> texto (null = ausente)
     */
    public static CatalogoAnimacoes paraJogo(java.util.function.Function<String, String> leitor,
            java.util.function.Consumer<String> aviso) {
        CatalogoAnimacoes c = new CatalogoAnimacoes();
        registrarLegadas(c);
        java.util.Map<String, String> todasJson = new java.util.TreeMap<>(JSON_MIGRADAS);
        todasJson.putAll(JSON_AJOELHADO);
        for (String id : todasJson.keySet()) {
            String recurso = todasJson.get(id);
            try {
                String texto = leitor.apply(recurso);
                if (texto == null) {
                    aviso.accept("[animacoes] " + recurso + " ausente: " + id + " usa o desenho legado.");
                    continue;
                }
                c.registrar(DefinicaoAnimacao.json(id, recurso, CarregadorAnimacaoJson.ler(texto, recurso)));
            } catch (RuntimeException ex) {
                aviso.accept("[animacoes] " + recurso + " invalido (" + ex.getMessage() + "): " + id
                        + " usa o desenho legado.");
            }
        }
        registrarConjuntas(c);
        return c;
    }

    /** Le um recurso do classpath (null se nao existir). */
    public static String lerRecurso(String caminho) {
        try (java.io.InputStream in = CatalogoAnimacoes.class.getResourceAsStream(caminho)) {
            return in == null ? null : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            return null;
        }
    }

    private static void registrarLegadas(CatalogoAnimacoes c) {
        for (String p : new String[]{"branco", "preto"}) {
            String classe = p.equals("branco") ? "IrmaoMiauBranco" : "IrmaoMiauPreto";
            c.registrar(DefinicaoAnimacao.legada(p + ".parado.legada", EstadoVisual.PARADO,
                    classe + ".desenharEmPe (idle procedural de IrmaoMiau)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".taunt.legada", EstadoVisual.TAUNT,
                    classe + ".TAUNT_MIAUSTUDIO"));
            c.registrar(DefinicaoAnimacao.legada(p + ".caminhada.legada", EstadoVisual.CAMINHADA,
                    classe + ".desenharEmPe + IrmaoMiau.getOscilacaoPasso (senoide, amplitude 10)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".corrida.legada", EstadoVisual.CORRIDA,
                    classe + ".desenharEmPe + IrmaoMiau.getOscilacaoPasso (senoide, amplitude 15)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".agachado.legada", EstadoVisual.AGACHADO,
                    classe + ".desenharEmPe (encolhimento procedural)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".pulo.legada", EstadoVisual.PULO,
                    classe + ".desenharEmPe (deslocamento vertical do pulo)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".rolamento.legada", EstadoVisual.ROLAMENTO,
                    classe + ".ROLAMENTO_MIAUSTUDIO (interpolado por progresso)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".sprawl.legada", EstadoVisual.SPRAWL,
                    classe + ".SPRAWL_MIAUSTUDIO (interpolado por progresso)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".deitado.legada", EstadoVisual.DEITADO,
                    classe + ".MOVIMENTO_CHAO_MIAUSTUDIO[0]"));
            c.registrar(DefinicaoAnimacao.legada(p + ".movimento_chao.legada", EstadoVisual.MOVIMENTO_CHAO,
                    classe + ".MOVIMENTO_CHAO_MIAUSTUDIO"));
            // 5K: transicoes de postura (TRANSICAO_AJOELHAR, _DEITAR, _ERGUER_JOELHOS, _LEVANTAR, _RAPIDA_*).
            c.registrar(DefinicaoAnimacao.procedural(p + ".transicao_postura.legada", EstadoVisual.TRANSICAO_POSTURA,
                    "animacao.TransicoesPostura (procedural 5K, interpola as poses das posturas no rig)"));
            // 5J: fallback em codigo (PosturaAjoelhada) quando o JSON nao carrega.
            c.registrar(DefinicaoAnimacao.legada(p + ".ajoelhado.legada", EstadoVisual.AJOELHADO,
                    classe + ".desenhar + PosturaAjoelhada.poseParado (fallback procedural 5J)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".ajoelhado_movimento.legada", EstadoVisual.AJOELHADO_MOVIMENTO,
                    classe + ".desenhar + PosturaAjoelhada.poseParado (fallback procedural 5J, sem passo)"));
        }
    }

    private static void registrarConjuntas(CatalogoAnimacoes c) {
        // Conjuntas: a PEGADA_1 continua com o MiauStudio + RegistroAnimacoesCombate.
        c.registrar(DefinicaoAnimacao.legada("pegada_1.entrada", EstadoVisual.PEGADA_ENTRADA,
                "RegistroAnimacoesCombate:pegada_1_entrada (/conteudo/miaustudio/PEGADA_1.json, 30 quadros)"));
        c.registrar(DefinicaoAnimacao.legada("pegada_1.estabelecida", EstadoVisual.PEGADA_ESTABELECIDA,
                "RegistroAnimacoesCombate:pegada_1_entrada, ultimo quadro (pose final persistente)"));
        c.registrar(DefinicaoAnimacao.legada("arrasto_conjunto.legado", EstadoVisual.ARRASTO_CONJUNTO,
                "pose final da PEGADA_1 transladada pelo SistemaCombate.deslocarPegadaControlada (5B)"));
    }
}
