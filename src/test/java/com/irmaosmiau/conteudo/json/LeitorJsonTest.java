package com.irmaosmiau.conteudo.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.conteudo.ConteudoInvalidoException;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LeitorJsonTest {

    @Test
    @SuppressWarnings("unchecked")
    void leTodosOsTiposJson() {
        Object v = LeitorJson.ler("{\"a\": 1, \"b\": -2.5, \"c\": \"ção\\n\\u0041\", "
                + "\"d\": true, \"e\": false, \"f\": null, \"g\": [1, {\"h\": []}], \"i\": {}}");

        Map<String, Object> m = (Map<String, Object>) v;
        assertEquals(1L, m.get("a"));
        assertEquals(-2.5, m.get("b"));
        assertEquals("ção\nA", m.get("c"));
        assertEquals(Boolean.TRUE, m.get("d"));
        assertEquals(Boolean.FALSE, m.get("e"));
        assertTrue(m.containsKey("f"));
        assertNull(m.get("f"));
        assertEquals(2, ((List<Object>) m.get("g")).size());
        assertEquals(List.of("a", "b", "c", "d", "e", "f", "g", "i"), List.copyOf(m.keySet()));
    }

    @Test
    void ignoraBomDoWindows() {
        assertEquals(List.of(), LeitorJson.ler("﻿[]"));
    }

    @Test
    void errosInformamLinhaEColuna() {
        String msg = assertThrows(ConteudoInvalidoException.class,
                () -> LeitorJson.ler("{\n  \"a\": 1\n  \"b\": 2\n}")).getMessage();
        assertTrue(msg.contains("linha 3, coluna 3"), msg);
    }

    @Test
    void rejeitaJsonMalFormado() {
        for (String ruim : new String[]{"", "   ", "{", "[1,]", "{\"a\" 1}", "{\"a\":1}x",
                "tru", "\"sem fim", "01x", "{\"a\":1,\"a\":2}", "[1 2]", "{a:1}"}) {
            assertThrows(ConteudoInvalidoException.class, () -> LeitorJson.ler(ruim),
                    "deveria rejeitar: " + ruim);
        }
        assertThrows(ConteudoInvalidoException.class, () -> LeitorJson.ler(null));
    }

    @Test
    void objetosSaoSomenteLeitura() {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) LeitorJson.ler("{\"a\": 1}");
        assertThrows(UnsupportedOperationException.class, () -> m.put("b", 2));
    }

    @Test
    void objetoJsonValidaCamposComCaminho() {
        ObjetoJson o = ObjetoJson.ler("{\"x\": {\"n\": \"7\"}, \"lista\": [1, null]}", "arq.json");

        String msg = assertThrows(ConteudoInvalidoException.class,
                () -> o.objeto("x").inteiro("n")).getMessage();
        assertEquals("[arq.json] x.n: era esperado numero inteiro, encontrado texto \"7\"", msg);

        msg = assertThrows(ConteudoInvalidoException.class, () -> o.texto("falta")).getMessage();
        assertEquals("[arq.json] falta: campo obrigatorio ausente", msg);

        msg = assertThrows(ConteudoInvalidoException.class, () -> o.inteirosOpcional("lista")).getMessage();
        assertTrue(msg.contains("lista[1]"), msg);

        assertThrows(ConteudoInvalidoException.class, () -> ObjetoJson.ler("[]", "arq.json"));
    }
}
