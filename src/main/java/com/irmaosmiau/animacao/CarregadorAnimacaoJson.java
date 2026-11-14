package com.irmaosmiau.animacao;

import com.irmaosmiau.conteudo.ConteudoInvalidoException;
import com.irmaosmiau.conteudo.json.ObjetoJson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Le arquivos de animacao individual (Entrega 5E). Formato NOVO e proprio;
 * nao altera o PEGADA_1.json nem o projeto do MiauStudio.
 *
 * <pre>
 * {
 *   "tipo": "ANIMACAO_INDIVIDUAL_IRMAOS_MIAU",
 *   "versaoEsquema": 1,
 *   "id": "branco.movimento_chao",
 *   "estado": "MOVIMENTO_CHAO",
 *   "rig": "miau_v1",
 *   "uso": "INDIVIDUAL",
 *   "loop": true,
 *   "duracaoQuadroMs": 150,
 *   "sequencia": [0, 1, 2, 3, 4, 3, 2, 1],
 *   "deslocamentoVisual": { "x": 0, "y": 70 },
 *   "espelhamento": "LADO_DO_LUTADOR",
 *   "origem": "IrmaoMiauBranco.MOVIMENTO_CHAO_MIAUSTUDIO",
 *   "quadros": [ { "pose": [30 inteiros] }, ... ]
 * }
 * </pre>
 *
 * Eventos e pontos de referencia ficam para quando houver uso real.
 */
public final class CarregadorAnimacaoJson {

    public static final String TIPO = "ANIMACAO_INDIVIDUAL_IRMAOS_MIAU";
    public static final int VERSAO_ESQUEMA = 1;
    /** Rigs conhecidos: hoje so o esqueleto de 30 valores do MiauStudio. */
    public static final Set<String> RIGS = Set.of("miau_v1");

    private static final Set<String> CAMPOS = Set.of("tipo", "versaoEsquema", "id", "estado", "rig", "uso",
            "loop", "duracaoQuadroMs", "sequencia", "deslocamentoVisual", "espelhamento", "origem", "quadros");

    private CarregadorAnimacaoJson() {
    }

    /** Carrega do classpath (NetBeans e JAR). */
    public static AnimacaoIndividual doRecurso(String caminho) {
        try (InputStream in = CarregadorAnimacaoJson.class.getResourceAsStream(caminho)) {
            if (in == null) {
                throw new ConteudoInvalidoException("Animacao nao encontrada: " + caminho);
            }
            return ler(new String(in.readAllBytes(), StandardCharsets.UTF_8), caminho);
        } catch (IOException ex) {
            throw new ConteudoInvalidoException("Falha ao ler " + caminho + ": " + ex.getMessage(), ex);
        }
    }

    /** @throws ConteudoInvalidoException com o caminho do problema */
    public static AnimacaoIndividual ler(String json, String origemArquivo) {
        ObjetoJson doc = ObjetoJson.ler(json, origemArquivo);
        for (String chave : doc.chaves()) {
            if (!CAMPOS.contains(chave)) {
                throw doc.erro(chave, "campo desconhecido");
            }
        }
        if (!TIPO.equals(doc.textoOpcional("tipo", null))) {
            throw doc.erro("tipo", "esperado \"" + TIPO + "\"");
        }
        int versao = doc.inteiro("versaoEsquema");
        if (versao < 1 || versao > VERSAO_ESQUEMA) {
            throw doc.erro("versaoEsquema", "versao " + versao + " nao suportada (ate " + VERSAO_ESQUEMA + ")");
        }
        String uso = doc.textoOpcional("uso", "INDIVIDUAL");
        if (!uso.equals("INDIVIDUAL")) {
            throw doc.erro("uso", "este formato e so para animacoes individuais (conjuntas: MiauStudio)");
        }
        String rig = doc.texto("rig");
        if (!RIGS.contains(rig)) {
            throw doc.erro("rig", "rig desconhecido \"" + rig + "\"");
        }
        EstadoVisual estado;
        try {
            estado = EstadoVisual.valueOf(doc.texto("estado"));
        } catch (IllegalArgumentException ex) {
            throw doc.erro("estado", "estado visual desconhecido");
        }
        if (estado.isConjunto()) {
            throw doc.erro("estado", "estado conjunto em animacao individual");
        }
        List<int[]> quadros = new ArrayList<>();
        List<ObjetoJson> lista = doc.listaDeObjetos("quadros");
        for (ObjetoJson q : lista) {
            int[] pose = q.inteirosOpcional("pose");
            if (pose == null) {
                throw q.erro("pose", "quadro sem pose");
            }
            quadros.add(pose);
        }
        int[] sequencia = doc.inteirosOpcional("sequencia");
        if (sequencia == null) {
            sequencia = new int[quadros.size()];
            for (int i = 0; i < sequencia.length; i++) {
                sequencia[i] = i;
            }
        }
        double dx = 0;
        double dy = 0;
        if (doc.contem("deslocamentoVisual")) {
            ObjetoJson d = doc.objeto("deslocamentoVisual");
            dx = d.decimalOpcional("x", 0);
            dy = d.decimalOpcional("y", 0);
        }
        AnimacaoIndividual.Espelhamento esp;
        try {
            esp = AnimacaoIndividual.Espelhamento.valueOf(doc.textoOpcional("espelhamento", "LADO_DO_LUTADOR"));
        } catch (IllegalArgumentException ex) {
            throw doc.erro("espelhamento", "valor desconhecido");
        }
        try {
            return new AnimacaoIndividual(doc.texto("id"), estado, rig, quadros, sequencia,
                    doc.inteiro("duracaoQuadroMs"), doc.booleanoOpcional("loop", true), dx, dy, esp,
                    doc.textoOpcional("origem", origemArquivo));
        } catch (IllegalArgumentException ex) {
            if (ex instanceof ConteudoInvalidoException c) {
                throw c;
            }
            throw new ConteudoInvalidoException("[" + origemArquivo + "] " + ex.getMessage(), ex);
        }
    }
}
