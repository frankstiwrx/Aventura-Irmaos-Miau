package com.irmaosmiau.conteudo.json;

import com.irmaosmiau.conteudo.ConteudoInvalidoException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Acesso tipado e validado a um objeto JSON lido pelo {@link LeitorJson}.
 *
 * Cada objeto conhece a ORIGEM (nome do arquivo) e o CAMINHO dentro do
 * documento ("timelineConjunta[2].preto"), para que qualquer erro diga
 * exatamente qual campo de qual arquivo precisa ser corrigido.
 */
public final class ObjetoJson {

    private final Map<String, Object> dados;
    private final String origem;
    private final String caminho;

    private ObjetoJson(Map<String, Object> dados, String origem, String caminho) {
        this.dados = dados;
        this.origem = origem;
        this.caminho = caminho;
    }

    /** Le o texto JSON e exige que a raiz seja um objeto. */
    public static ObjetoJson ler(String textoJson, String origem) {
        Object raiz;
        try {
            raiz = LeitorJson.ler(textoJson);
        } catch (ConteudoInvalidoException ex) {
            throw ConteudoInvalidoException.em(origem, null, ex.getMessage());
        }
        return deValor(raiz, origem, "(raiz)");
    }

    @SuppressWarnings("unchecked")
    private static ObjetoJson deValor(Object valor, String origem, String caminho) {
        if (!(valor instanceof Map)) {
            throw ConteudoInvalidoException.em(origem, caminho,
                    "era esperado um objeto JSON { ... }, encontrado " + descrever(valor));
        }
        return new ObjetoJson((Map<String, Object>) valor, origem, caminho);
    }

    public String getOrigem() {
        return origem;
    }

    public String getCaminho() {
        return caminho;
    }

    public Set<String> chaves() {
        return Collections.unmodifiableSet(dados.keySet());
    }

    /** true quando a chave existe e nao e null. */
    public boolean contem(String chave) {
        return dados.get(chave) != null;
    }

    public ConteudoInvalidoException erro(String chave, String detalhe) {
        return ConteudoInvalidoException.em(origem, caminhoDe(chave), detalhe);
    }

    private String caminhoDe(String chave) {
        if (chave == null) {
            return caminho;
        }
        return "(raiz)".equals(caminho) ? chave : caminho + "." + chave;
    }

    private Object exigir(String chave) {
        if (!dados.containsKey(chave)) {
            throw erro(chave, "campo obrigatorio ausente");
        }
        Object v = dados.get(chave);
        if (v == null) {
            throw erro(chave, "campo obrigatorio esta null");
        }
        return v;
    }

    // =====================================================
    // TEXTO
    // =====================================================
    public String texto(String chave) {
        Object v = exigir(chave);
        if (!(v instanceof String)) {
            throw erro(chave, "era esperado texto, encontrado " + descrever(v));
        }
        String s = ((String) v).trim();
        if (s.isEmpty()) {
            throw erro(chave, "texto vazio");
        }
        return s;
    }

    public String textoOpcional(String chave, String padrao) {
        return contem(chave) ? texto(chave) : padrao;
    }

    // =====================================================
    // NUMEROS
    // =====================================================
    public int inteiro(String chave) {
        return paraInteiro(exigir(chave), caminhoDe(chave));
    }

    public int inteiroOpcional(String chave, int padrao) {
        return contem(chave) ? inteiro(chave) : padrao;
    }

    public int inteiroEntre(String chave, int minimo, int maximo) {
        int v = inteiro(chave);
        if (v < minimo || v > maximo) {
            throw erro(chave, "valor " + v + " fora do intervalo [" + minimo + ", " + maximo + "]");
        }
        return v;
    }

    public double decimal(String chave) {
        Object v = exigir(chave);
        if (!(v instanceof Number)) {
            throw erro(chave, "era esperado numero, encontrado " + descrever(v));
        }
        return ((Number) v).doubleValue();
    }

    public double decimalOpcional(String chave, double padrao) {
        return contem(chave) ? decimal(chave) : padrao;
    }

    private int paraInteiro(Object v, String caminhoCampo) {
        if (v instanceof Long) {
            long l = (Long) v;
            if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
                throw ConteudoInvalidoException.em(origem, caminhoCampo,
                        "inteiro fora do limite: " + l);
            }
            return (int) l;
        }
        if (v instanceof Double) {
            double d = (Double) v;
            if (d == Math.rint(d) && Math.abs(d) <= Integer.MAX_VALUE) {
                return (int) d;
            }
            throw ConteudoInvalidoException.em(origem, caminhoCampo,
                    "era esperado numero inteiro, encontrado " + d);
        }
        throw ConteudoInvalidoException.em(origem, caminhoCampo,
                "era esperado numero inteiro, encontrado " + descrever(v));
    }

    // =====================================================
    // BOOLEAN
    // =====================================================
    public boolean booleano(String chave) {
        Object v = exigir(chave);
        if (!(v instanceof Boolean)) {
            throw erro(chave, "era esperado true/false, encontrado " + descrever(v));
        }
        return (Boolean) v;
    }

    public boolean booleanoOpcional(String chave, boolean padrao) {
        return contem(chave) ? booleano(chave) : padrao;
    }

    // =====================================================
    // OBJETOS E LISTAS
    // =====================================================
    public ObjetoJson objeto(String chave) {
        return deValor(exigir(chave), origem, caminhoDe(chave));
    }

    private List<?> listaBruta(String chave) {
        Object v = exigir(chave);
        if (!(v instanceof List)) {
            throw erro(chave, "era esperada uma lista [ ... ], encontrado " + descrever(v));
        }
        return (List<?>) v;
    }

    public int tamanhoLista(String chave) {
        return listaBruta(chave).size();
    }

    /** Objetos de uma lista; elementos null ou de outro tipo geram erro com indice. */
    public List<ObjetoJson> listaDeObjetos(String chave) {
        List<?> bruta = listaBruta(chave);
        List<ObjetoJson> resultado = new ArrayList<>(bruta.size());
        for (int i = 0; i < bruta.size(); i++) {
            String caminhoItem = caminhoDe(chave) + "[" + i + "]";
            Object item = bruta.get(i);
            if (item == null) {
                throw ConteudoInvalidoException.em(origem, caminhoItem,
                        "item " + (i + 1) + " esta vazio (null)");
            }
            resultado.add(deValor(item, origem, caminhoItem));
        }
        return Collections.unmodifiableList(resultado);
    }

    /** Lista de inteiros, ou null quando o campo nao existe/esta null. */
    public int[] inteirosOpcional(String chave) {
        if (!contem(chave)) {
            return null;
        }
        List<?> bruta = listaBruta(chave);
        int[] resultado = new int[bruta.size()];
        for (int i = 0; i < bruta.size(); i++) {
            Object item = bruta.get(i);
            String caminhoItem = caminhoDe(chave) + "[" + i + "]";
            if (item == null) {
                throw ConteudoInvalidoException.em(origem, caminhoItem, "valor null");
            }
            resultado[i] = paraInteiro(item, caminhoItem);
        }
        return resultado;
    }

    private static String descrever(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Map) {
            return "objeto";
        }
        if (v instanceof List) {
            return "lista";
        }
        if (v instanceof String) {
            return "texto \"" + v + "\"";
        }
        if (v instanceof Boolean) {
            return "boolean " + v;
        }
        return "numero " + v;
    }
}
