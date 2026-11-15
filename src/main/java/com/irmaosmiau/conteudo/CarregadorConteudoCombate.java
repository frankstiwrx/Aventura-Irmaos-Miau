package com.irmaosmiau.conteudo;

import com.irmaosmiau.combate.AcaoCombate;
import com.irmaosmiau.combate.AnimacaoConjuntaQuadros;
import com.irmaosmiau.combate.CategoriaAcao;
import com.irmaosmiau.combate.DisponibilidadeAcao;
import com.irmaosmiau.combate.EstadoInteracaoCombate;
import com.irmaosmiau.combate.RegistroAcoes;
import com.irmaosmiau.combate.RegistroAnimacoesCombate;
import com.irmaosmiau.conteudo.json.ObjetoJson;
import com.irmaosmiau.conteudo.miaustudio.AnimacaoConjuntaMiauStudio;
import com.irmaosmiau.conteudo.miaustudio.ConversorMiauStudio;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Carrega catalogos de animacoes e tecnicas a partir de arquivos JSON.
 *
 * Arquivos padrao (src/main/resources):
 * <pre>
 * /conteudo/animacoes/animacoes.json   catalogo de animacoes conjuntas
 * /conteudo/tecnicas/tecnicas.json     catalogo de tecnicas
 * /conteudo/miaustudio/*.json          projetos originais do MiauStudio
 * </pre>
 *
 * Os projetos do MiauStudio sao somente lidos. Qualquer erro gera
 * {@link ConteudoInvalidoException} com arquivo e campo.
 */
public final class CarregadorConteudoCombate {

    public static final String CATALOGO_ANIMACOES_PADRAO = "/conteudo/animacoes/animacoes.json";
    public static final String CATALOGO_TECNICAS_PADRAO = "/conteudo/tecnicas/tecnicas.json";

    public static final String TIPO_CATALOGO_ANIMACOES = "CATALOGO_ANIMACOES_COMBATE";
    public static final String TIPO_CATALOGO_TECNICAS = "CATALOGO_TECNICAS_COMBATE";
    public static final int VERSAO_CATALOGO = 1;

    /** Unico formato de arquivo de animacao suportado nesta entrega. */
    public static final String FORMATO_MIAUSTUDIO_V2 = "MIAUSTUDIO_PROJECT_V2";

    private static final Set<String> CAMPOS_ANIMACAO = Set.of(
            "id", "formato", "arquivo", "duracaoFrames", "duracoesQuadros", "descricao", "varianteDe");

    private static final Set<String> CAMPOS_TECNICA = Set.of(
            "id", "nome", "categoria", "disponibilidade", "descricao",
            "estadoInteracaoAoSucesso", "animacao", "duracaoAnimacaoFrames",
            "alcanceHorizontal", "diferencaMaximaLinhas", "exigeFrenteParaOponente",
            "custoFolego", "dificuldade", "pesos");

    private static final Set<String> CAMPOS_PESOS = Set.of(
            "tecnica", "forca", "agilidade", "pesoCorporal");

    private CarregadorConteudoCombate() {
    }

    /** Catalogos padrao do classpath. */
    public static ConteudoCombate carregarPadrao() {
        return carregarPadrao(FonteConteudo.classpath());
    }

    /** Catalogos padrao lidos de outra fonte (usado nos testes de falha). */
    public static ConteudoCombate carregarPadrao(FonteConteudo fonte) {
        return carregar(fonte, CATALOGO_ANIMACOES_PADRAO, CATALOGO_TECNICAS_PADRAO);
    }

    /**
     * Ponto de entrada do JOGO: carrega o conteudo padrao e, se algo
     * obrigatorio estiver ausente ou invalido, falha com uma mensagem
     * unica e diagnosticavel. Nunca troca o conteudo por outra fonte.
     *
     * @throws ConteudoInvalidoException com o prefixo
     *         "Falha ao carregar o conteudo de combate"
     */
    public static ConteudoCombate carregarParaJogo() {
        return carregarParaJogo(FonteConteudo.classpath());
    }

    /** @see #carregarParaJogo() */
    public static ConteudoCombate carregarParaJogo(FonteConteudo fonte) {
        try {
            return carregarPadrao(fonte);
        } catch (IllegalArgumentException ex) {
            throw new ConteudoInvalidoException(
                    "Falha ao carregar o conteudo de combate (catalogos "
                    + CATALOGO_TECNICAS_PADRAO + " e " + CATALOGO_ANIMACOES_PADRAO
                    + "): " + ex.getMessage(), ex);
        }
    }

    public static ConteudoCombate carregar(
            FonteConteudo fonte,
            String catalogoAnimacoes,
            String catalogoTecnicas
    ) {
        RegistroAnimacoesCombate animacoes = carregarAnimacoes(fonte, catalogoAnimacoes);
        RegistroAcoes tecnicas = carregarTecnicas(fonte, catalogoTecnicas, animacoes);
        return new ConteudoCombate(tecnicas, animacoes);
    }

    // =====================================================
    // ANIMACOES
    // =====================================================
    public static RegistroAnimacoesCombate carregarAnimacoes(FonteConteudo fonte, String caminhoCatalogo) {

        ObjetoJson catalogo = lerCatalogo(fonte, caminhoCatalogo, TIPO_CATALOGO_ANIMACOES);
        RegistroAnimacoesCombate registro = RegistroAnimacoesCombate.vazio();

        for (ObjetoJson item : catalogo.listaDeObjetos("animacoes")) {
            rejeitarCamposDesconhecidos(item, CAMPOS_ANIMACAO);

            String id = item.texto("id");
            if (registro.contem(id)) {
                throw item.erro("id", "animacao duplicada: " + id);
            }

            String formato = item.texto("formato");
            if (!FORMATO_MIAUSTUDIO_V2.equals(formato)) {
                throw item.erro("formato", "formato \"" + formato + "\" nao suportado; use \""
                        + FORMATO_MIAUSTUDIO_V2 + "\"");
            }

            String arquivo = resolver(caminhoCatalogo, item.texto("arquivo"));
            int duracaoTotal = item.inteiroOpcional("duracaoFrames", 0);
            if (item.contem("duracaoFrames") && duracaoTotal <= 0) {
                throw item.erro("duracaoFrames", "precisa ser maior que zero");
            }
            int[] duracoesQuadros = item.inteirosOpcional("duracoesQuadros");

            AnimacaoConjuntaMiauStudio importada =
                    ConversorMiauStudio.importarConjunta(fonte.ler(arquivo), arquivo);

            AnimacaoConjuntaQuadros quadros;
            try {
                quadros = importada.paraQuadros(duracaoTotal, duracoesQuadros);
            } catch (IllegalArgumentException ex) {
                throw item.erro(null, "animacao " + id + ": " + ex.getMessage());
            }

            registro.registrar(id, quadros, arquivo);
            // 5J: molde de quem aplica e de quem recebe vem do proprio projeto (aplicadorConjunto).
            registro.registrarIdentidades(id,
                    com.irmaosmiau.combate.IdentidadeRig.doNome(importada.getNomeAplicador()),
                    com.irmaosmiau.combate.IdentidadeRig.doNome(importada.getNomeReceptor()));
            if (item.contem("varianteDe")) {
                String base = item.texto("varianteDe");
                if (!registro.contem(base)) {
                    throw item.erro("varianteDe", "animacao base inexistente (declare-a antes): " + base);
                }
                try {
                    registro.registrarVariante(base, id);
                } catch (IllegalArgumentException ex) {
                    throw item.erro("varianteDe", ex.getMessage());
                }
            }
        }
        return registro;
    }

    // =====================================================
    // TECNICAS
    // =====================================================
    public static RegistroAcoes carregarTecnicas(
            FonteConteudo fonte,
            String caminhoCatalogo,
            RegistroAnimacoesCombate animacoes
    ) {
        if (animacoes == null) {
            throw new IllegalArgumentException("O registro de animacoes e obrigatorio.");
        }

        ObjetoJson catalogo = lerCatalogo(fonte, caminhoCatalogo, TIPO_CATALOGO_TECNICAS);
        RegistroAcoes registro = RegistroAcoes.vazio();

        for (ObjetoJson item : catalogo.listaDeObjetos("tecnicas")) {
            AcaoCombate acao = lerTecnica(item, animacoes);
            if (registro.contem(acao.getId())) {
                throw item.erro("id", "tecnica duplicada: " + acao.getId());
            }
            registro.registrar(acao);
        }
        return registro;
    }

    private static AcaoCombate lerTecnica(ObjetoJson item, RegistroAnimacoesCombate animacoes) {

        rejeitarCamposDesconhecidos(item, CAMPOS_TECNICA);

        String id = item.texto("id");
        DisponibilidadeAcao disponibilidade =
                lerEnum(item, "disponibilidade", DisponibilidadeAcao.class);
        boolean jogavel = disponibilidade == DisponibilidadeAcao.JOGAVEL;

        AcaoCombate.Builder b = AcaoCombate.builder(
                id, item.texto("nome"), lerEnum(item, "categoria", CategoriaAcao.class));
        b.disponibilidade(disponibilidade);
        b.descricao(item.textoOpcional("descricao", null));

        if (item.contem("estadoInteracaoAoSucesso")) {
            b.estadoInteracaoAoSucesso(
                    lerEnum(item, "estadoInteracaoAoSucesso", EstadoInteracaoCombate.class));
        }

        // Tecnica JOGAVEL: nenhuma regra numerica pode vir de um padrao silencioso.
        if (jogavel) {
            b.alcanceHorizontal(item.decimal("alcanceHorizontal"));
            b.diferencaMaximaLinhas(item.inteiro("diferencaMaximaLinhas"));
            b.exigeFrenteParaOponente(item.booleano("exigeFrenteParaOponente"));
            b.custoFolego(item.decimal("custoFolego"));
            b.dificuldade(item.decimal("dificuldade"));
        } else {
            b.alcanceHorizontal(item.decimalOpcional("alcanceHorizontal", 0.0));
            b.diferencaMaximaLinhas(item.inteiroOpcional("diferencaMaximaLinhas", 0));
            b.exigeFrenteParaOponente(item.booleanoOpcional("exigeFrenteParaOponente", false));
            b.custoFolego(item.decimalOpcional("custoFolego", 0.0));
            b.dificuldade(item.decimalOpcional("dificuldade", 0.0));
        }

        if (jogavel || item.contem("pesos")) {
            ObjetoJson pesos = item.objeto("pesos");
            rejeitarCamposDesconhecidos(pesos, CAMPOS_PESOS);
            b.pesos(pesos.decimal("tecnica"), pesos.decimal("forca"),
                    pesos.decimal("agilidade"), pesos.decimal("pesoCorporal"));
        }

        if (item.contem("animacao")) {
            String idAnimacao = item.texto("animacao");
            AnimacaoConjuntaQuadros anim = animacoes.buscar(idAnimacao);
            if (anim == null) {
                throw item.erro("animacao", "animacao inexistente: " + idAnimacao
                        + " (registradas: " + animacoes.ids() + ")");
            }
            int duracaoAnimacao = anim.getDuracaoTotalFrames();
            int duracaoTecnica = item.inteiroOpcional("duracaoAnimacaoFrames", 0);
            int duracao;
            if (duracaoTecnica > 0 && duracaoAnimacao > 0 && duracaoTecnica != duracaoAnimacao) {
                throw item.erro("duracaoAnimacaoFrames", duracaoTecnica
                        + " frames, mas a animacao " + idAnimacao + " define "
                        + duracaoAnimacao + " frames");
            } else if (duracaoTecnica > 0) {
                duracao = duracaoTecnica;
            } else if (duracaoAnimacao > 0) {
                duracao = duracaoAnimacao;
            } else {
                throw item.erro("duracaoAnimacaoFrames",
                        "obrigatorio porque a animacao " + idAnimacao + " nao define duracao");
            }
            b.animacao(idAnimacao, duracao);
        } else if (item.contem("duracaoAnimacaoFrames")) {
            throw item.erro("duracaoAnimacaoFrames", "informado sem \"animacao\"");
        }

        try {
            return b.build();
        } catch (IllegalArgumentException ex) {
            throw item.erro(null, "tecnica " + id + ": " + ex.getMessage());
        }
    }

    // =====================================================
    // AUXILIARES
    // =====================================================
    private static ObjetoJson lerCatalogo(FonteConteudo fonte, String caminho, String tipoEsperado) {
        if (fonte == null) {
            throw new IllegalArgumentException("A fonte de conteudo e obrigatoria.");
        }
        ObjetoJson catalogo = ObjetoJson.ler(fonte.ler(caminho), caminho);
        String tipo = catalogo.texto("tipo");
        if (!tipoEsperado.equals(tipo)) {
            throw catalogo.erro("tipo", "esperado \"" + tipoEsperado + "\", encontrado \"" + tipo + "\"");
        }
        int versao = catalogo.inteiro("versao");
        if (versao != VERSAO_CATALOGO) {
            throw catalogo.erro("versao", "versao " + versao + " nao suportada (esperada "
                    + VERSAO_CATALOGO + ")");
        }
        return catalogo;
    }

    /** Caminho relativo ao diretorio do catalogo; "/..." e absoluto no classpath. */
    static String resolver(String caminhoCatalogo, String arquivo) {
        if (arquivo.startsWith("/")) {
            return arquivo;
        }
        int barra = caminhoCatalogo.lastIndexOf('/');
        return barra < 0 ? arquivo : caminhoCatalogo.substring(0, barra + 1) + arquivo;
    }

    private static void rejeitarCamposDesconhecidos(ObjetoJson objeto, Set<String> permitidos) {
        Set<String> desconhecidos = new LinkedHashSet<>(objeto.chaves());
        desconhecidos.removeAll(permitidos);
        if (!desconhecidos.isEmpty()) {
            throw objeto.erro(null, "campo(s) desconhecido(s) " + desconhecidos
                    + "; permitidos: " + permitidos);
        }
    }

    private static <E extends Enum<E>> E lerEnum(ObjetoJson item, String chave, Class<E> tipo) {
        String valor = item.texto(chave);
        try {
            return Enum.valueOf(tipo, valor.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw item.erro(chave, "valor \"" + valor + "\" invalido; use um de "
                    + Arrays.toString(tipo.getEnumConstants()));
        }
    }
}
