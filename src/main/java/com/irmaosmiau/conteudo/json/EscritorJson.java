package com.irmaosmiau.conteudo.json;

import java.util.List;
import java.util.Map;

/**
 * Escreve JSON legivel (indentado) a partir de Map, List, String, Number,
 * Boolean e null, o mesmo modelo que o {@link LeitorJson} produz.
 *
 * Qualquer outro tipo (ex.: componentes Swing, cores, objetos do jogo) e
 * recusado com erro: so dados simples sao persistidos.
 */
public final class EscritorJson {

    private EscritorJson() {
    }

    public static String escrever(Object valor) {
        StringBuilder sb = new StringBuilder();
        escreverValor(valor, sb, 0);
        sb.append('\n');
        return sb.toString();
    }

    private static void escreverValor(Object v, StringBuilder sb, int nivel) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            escreverTexto((String) v, sb);
        } else if (v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("Numero invalido para JSON: " + d);
            }
            sb.append(Double.toString(d));
        } else if (v instanceof Long || v instanceof Integer || v instanceof Short || v instanceof Byte) {
            sb.append(((Number) v).longValue());
        } else if (v instanceof Map) {
            escreverObjeto((Map<?, ?>) v, sb, nivel);
        } else if (v instanceof List) {
            escreverLista((List<?>) v, sb, nivel);
        } else {
            throw new IllegalArgumentException(
                    "Tipo nao suportado em JSON: " + v.getClass().getName());
        }
    }

    private static void escreverObjeto(Map<?, ?> mapa, StringBuilder sb, int nivel) {
        if (mapa.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<?, ?> e : mapa.entrySet()) {
            if (!(e.getKey() instanceof String)) {
                throw new IllegalArgumentException("Chave de objeto JSON precisa ser texto: " + e.getKey());
            }
            recuo(sb, nivel + 1);
            escreverTexto((String) e.getKey(), sb);
            sb.append(": ");
            escreverValor(e.getValue(), sb, nivel + 1);
            if (++i < mapa.size()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        recuo(sb, nivel);
        sb.append('}');
    }

    private static void escreverLista(List<?> lista, StringBuilder sb, int nivel) {
        if (lista.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[\n");
        for (int i = 0; i < lista.size(); i++) {
            recuo(sb, nivel + 1);
            escreverValor(lista.get(i), sb, nivel + 1);
            if (i < lista.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        recuo(sb, nivel);
        sb.append(']');
    }

    private static void escreverTexto(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void recuo(StringBuilder sb, int nivel) {
        for (int i = 0; i < nivel; i++) {
            sb.append("  ");
        }
    }
}
