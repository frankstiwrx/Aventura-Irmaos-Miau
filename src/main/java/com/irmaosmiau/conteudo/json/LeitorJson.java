package com.irmaosmiau.conteudo.json;

import com.irmaosmiau.conteudo.ConteudoInvalidoException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor JSON pequeno e sem dependencias, usado pelo conteudo do jogo.
 *
 * Tipos produzidos:
 *  objeto  -> Map&lt;String, Object&gt; (ordem preservada, somente leitura)
 *  lista   -> List&lt;Object&gt; (somente leitura)
 *  texto   -> String
 *  numero  -> Long (inteiro) ou Double (com ponto/expoente)
 *  boolean -> Boolean
 *  null    -> null
 *
 * Erros de sintaxe informam linha e coluna. O MiauStudio continua com o
 * seu proprio parser interno; este leitor existe para o runtime do jogo
 * nao depender do editor.
 */
public final class LeitorJson {

    private final String texto;
    private int pos;

    private LeitorJson(String texto) {
        this.texto = texto;
        this.pos = 0;
    }

    /** Le um documento JSON completo. */
    public static Object ler(String texto) {
        if (texto == null) {
            throw new ConteudoInvalidoException("JSON vazio (texto nulo).");
        }
        LeitorJson leitor = new LeitorJson(texto);
        // Ignora BOM UTF-8, comum em arquivos salvos no Windows.
        if (leitor.pos < texto.length() && texto.charAt(leitor.pos) == '﻿') {
            leitor.pos++;
        }
        leitor.pularEspacos();
        if (leitor.fim()) {
            throw leitor.erro("documento vazio");
        }
        Object valor = leitor.lerValor();
        leitor.pularEspacos();
        if (!leitor.fim()) {
            throw leitor.erro("conteudo extra depois do fim do JSON");
        }
        return valor;
    }

    private boolean fim() {
        return pos >= texto.length();
    }

    private char atual() {
        return texto.charAt(pos);
    }

    private void pularEspacos() {
        while (!fim()) {
            char c = atual();
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private Object lerValor() {
        pularEspacos();
        if (fim()) {
            throw erro("fim inesperado do JSON");
        }
        char c = atual();
        switch (c) {
            case '{':
                return lerObjeto();
            case '[':
                return lerLista();
            case '"':
                return lerTexto();
            case 't':
                lerLiteral("true");
                return Boolean.TRUE;
            case 'f':
                lerLiteral("false");
                return Boolean.FALSE;
            case 'n':
                lerLiteral("null");
                return null;
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return lerNumero();
                }
                throw erro("valor inesperado '" + c + "'");
        }
    }

    private Map<String, Object> lerObjeto() {
        pos++; // {
        Map<String, Object> mapa = new LinkedHashMap<>();
        pularEspacos();
        if (!fim() && atual() == '}') {
            pos++;
            return Collections.unmodifiableMap(mapa);
        }
        while (true) {
            pularEspacos();
            if (fim() || atual() != '"') {
                throw erro("era esperada uma chave entre aspas");
            }
            String chave = lerTexto();
            if (mapa.containsKey(chave)) {
                throw erro("chave duplicada \"" + chave + "\"");
            }
            pularEspacos();
            if (fim() || atual() != ':') {
                throw erro("era esperado ':' depois de \"" + chave + "\"");
            }
            pos++;
            mapa.put(chave, lerValor());
            pularEspacos();
            if (fim()) {
                throw erro("objeto nao fechado (falta '}')");
            }
            char c = atual();
            pos++;
            if (c == '}') {
                return Collections.unmodifiableMap(mapa);
            }
            if (c != ',') {
                pos--;
                throw erro("era esperado ',' ou '}'");
            }
        }
    }

    private List<Object> lerLista() {
        pos++; // [
        List<Object> lista = new ArrayList<>();
        pularEspacos();
        if (!fim() && atual() == ']') {
            pos++;
            return Collections.unmodifiableList(lista);
        }
        while (true) {
            lista.add(lerValor());
            pularEspacos();
            if (fim()) {
                throw erro("lista nao fechada (falta ']')");
            }
            char c = atual();
            pos++;
            if (c == ']') {
                return Collections.unmodifiableList(lista);
            }
            if (c != ',') {
                pos--;
                throw erro("era esperado ',' ou ']'");
            }
        }
    }

    private String lerTexto() {
        pos++; // "
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (fim()) {
                throw erro("texto nao terminado (falta '\"')");
            }
            char c = atual();
            pos++;
            if (c == '"') {
                return sb.toString();
            }
            if (c < 0x20) {
                pos--;
                throw erro("caractere de controle dentro de texto");
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (fim()) {
                throw erro("escape incompleto");
            }
            char e = atual();
            pos++;
            switch (e) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if (pos + 4 > texto.length()) {
                        throw erro("escape \\u incompleto");
                    }
                    try {
                        sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException ex) {
                        throw erro("escape \\u invalido");
                    }
                    pos += 4;
                    break;
                default:
                    pos--;
                    throw erro("escape invalido \\" + e);
            }
        }
    }

    private Object lerNumero() {
        int inicio = pos;
        boolean decimal = false;
        if (atual() == '-') {
            pos++;
        }
        if (fim() || !Character.isDigit(atual())) {
            throw erro("numero invalido");
        }
        while (!fim()) {
            char c = atual();
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                decimal = true;
                pos++;
            } else {
                break;
            }
        }
        String numero = texto.substring(inicio, pos);
        try {
            if (decimal) {
                double d = Double.parseDouble(numero);
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    throw new NumberFormatException();
                }
                return d;
            }
            return Long.parseLong(numero);
        } catch (NumberFormatException ex) {
            pos = inicio;
            throw erro("numero invalido: " + numero);
        }
    }

    private void lerLiteral(String literal) {
        if (!texto.startsWith(literal, pos)) {
            throw erro("literal invalido (esperado " + literal + ")");
        }
        pos += literal.length();
    }

    private ConteudoInvalidoException erro(String detalhe) {
        int linha = 1;
        int coluna = 1;
        int limite = Math.min(pos, texto.length());
        for (int i = 0; i < limite; i++) {
            if (texto.charAt(i) == '\n') {
                linha++;
                coluna = 1;
            } else {
                coluna++;
            }
        }
        return new ConteudoInvalidoException(
                "JSON invalido na linha " + linha + ", coluna " + coluna + ": " + detalhe);
    }
}
