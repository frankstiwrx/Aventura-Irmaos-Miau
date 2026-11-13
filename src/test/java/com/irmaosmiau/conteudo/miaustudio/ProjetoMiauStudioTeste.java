package com.irmaosmiau.conteudo.miaustudio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Monta textos de projeto MiauStudio v2 para testes de validacao.
 * Cada pose tem todos os campos; campos podem ser removidos ou trocados.
 */
final class ProjetoMiauStudioTeste {

    static final String[] CAMPOS = {
        "escalaX10", "viradoDireita",
        "offCinturaX", "offCinturaY", "rotCintura",
        "offPeitoX", "offPeitoY", "rotPeito",
        "rotGlobal", "rotCabeca", "offCabX", "offCabY",
        "offCotEsqX", "offCotEsqY", "offMaoEsqX", "offMaoEsqY",
        "offCotDirX", "offCotDirY", "offMaoDirX", "offMaoDirY",
        "offJoelEsqX", "offJoelEsqY", "offPeEsqX", "offPeEsqY",
        "offJoelDirX", "offJoelDirY", "offPeDirX", "offPeDirY",
        "offsetCentroX", "offsetLinha"
    };

    private ProjetoMiauStudioTeste() {
    }

    /** Pose valida: escala 7, virado conforme parametro, articulacoes = base + i. */
    static Map<String, String> pose(int base, boolean virado) {
        Map<String, String> p = new LinkedHashMap<>();
        for (int i = 0; i < CAMPOS.length; i++) {
            p.put(CAMPOS[i], String.valueOf(base + i));
        }
        p.put("escalaX10", "7");
        p.put("viradoDireita", String.valueOf(virado));
        p.put("offsetLinha", "0");
        return p;
    }

    static String json(Map<String, String> pose) {
        StringBuilder sb = new StringBuilder("{");
        boolean primeiro = true;
        for (Map.Entry<String, String> e : pose.entrySet()) {
            if (!primeiro) {
                sb.append(", ");
            }
            sb.append('"').append(e.getKey()).append("\": ").append(e.getValue());
            primeiro = false;
        }
        return sb.append('}').toString();
    }

    static String quadro(String poseBranco, String posePreto) {
        StringBuilder sb = new StringBuilder("{");
        if (poseBranco != null) {
            sb.append("\"branco\": ").append(poseBranco);
        }
        if (posePreto != null) {
            if (poseBranco != null) {
                sb.append(", ");
            }
            sb.append("\"preto\": ").append(posePreto);
        }
        return sb.append('}').toString();
    }

    static String quadroValido(int base) {
        return quadro(json(pose(base, true)), json(pose(-base, false)));
    }

    static String projeto(String cabecalhoExtra, String... quadros) {
        return "{\n"
                + "\"tipo\": \"MIAUSTUDIO_PROJECT\",\n"
                + "\"versao\": 2,\n"
                + "\"nomeAnimacao\": \"TESTE\",\n"
                + "\"modoAnimacao\": \"CONJUNTA\",\n"
                + "\"aplicadorConjunto\": \"Branco\",\n"
                + "\"centroInteracaoX\": 360,\n"
                + "\"linhaInteracao\": 2,\n"
                + "\"offsetVisualAnimacaoX\": 0,\n"
                + "\"offsetVisualAnimacaoY\": 0,\n"
                + cabecalhoExtra
                + "\"timelineConjunta\": [" + String.join(", ", quadros) + "]\n"
                + "}\n";
    }

    static String projetoValido(int quantidadeQuadros) {
        String[] q = new String[quantidadeQuadros];
        for (int i = 0; i < q.length; i++) {
            q[i] = quadroValido(i + 1);
        }
        return projeto("", q);
    }

    static byte[] lerRecurso(String caminho) {
        try (InputStream in = ProjetoMiauStudioTeste.class.getResourceAsStream(caminho)) {
            if (in == null) {
                throw new IllegalStateException("recurso ausente: " + caminho);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static String lerTexto(String caminho) {
        return new String(lerRecurso(caminho), StandardCharsets.UTF_8);
    }
}
