package com.irmaosmiau.conteudo.miaustudio;

import static com.irmaosmiau.conteudo.miaustudio.ProjetoMiauStudioTeste.json;
import static com.irmaosmiau.conteudo.miaustudio.ProjetoMiauStudioTeste.pose;
import static com.irmaosmiau.conteudo.miaustudio.ProjetoMiauStudioTeste.projeto;
import static com.irmaosmiau.conteudo.miaustudio.ProjetoMiauStudioTeste.projetoValido;
import static com.irmaosmiau.conteudo.miaustudio.ProjetoMiauStudioTeste.quadro;
import static com.irmaosmiau.conteudo.miaustudio.ProjetoMiauStudioTeste.quadroValido;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.AnimacaoConjuntaQuadros;
import com.irmaosmiau.combate.Pegada1RegressaoArraysTest;
import com.irmaosmiau.combate.RegistroAnimacoesCombate;
import com.irmaosmiau.conteudo.ConteudoInvalidoException;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ConversorMiauStudioTest {

    private static final String PEGADA_1 = "/conteudo/miaustudio/PEGADA_1.json";

    /** SHA-256 do PEGADA_1.json original entregue pelo criador. */
    private static final String SHA256_PEGADA_1_ORIGINAL =
            "a5d37b2ad6a877bda9821f7bb7f065b25018f358172dacf6a94099a1a7edab72";

    private static AnimacaoConjuntaMiauStudio importarPegada1() {
        return ConversorMiauStudio.importarConjunta(
                ProjetoMiauStudioTeste.lerTexto(PEGADA_1), PEGADA_1);
    }

    private static String mensagemDeErro(String json) {
        return assertThrows(ConteudoInvalidoException.class,
                () -> ConversorMiauStudio.importarConjunta(json, "teste.json")).getMessage();
    }

    // =====================================================
    // PARIDADE COM A PEGADA_1 (teste obrigatorio)
    // =====================================================
    @Test
    void pegada1ImportadaEIdenticaAosSeisArraysJavaAprovados() {
        AnimacaoConjuntaMiauStudio a = importarPegada1();

        assertEquals(3, a.quantidadeQuadros());
        int[][] aplicador = a.copiarQuadrosAplicador();
        int[][] receptor = a.copiarQuadrosReceptor();
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[i], aplicador[i],
                    "aplicador quadro " + (i + 1));
            assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[i], receptor[i],
                    "receptor quadro " + (i + 1));
        }
    }

    @Test
    void pegada1ImportadaEquivaleAoRegistroLegadoEmTudo() {
        AnimacaoConjuntaQuadros json = importarPegada1().paraQuadros();
        AnimacaoConjuntaQuadros legado =
                new RegistroAnimacoesCombate().buscar("pegada_1_entrada");

        assertEquals(legado.quantidadeQuadros(), json.quantidadeQuadros());
        assertEquals(legado.getOffsetVisualX(), json.getOffsetVisualX());
        assertEquals(legado.getOffsetVisualY(), json.getOffsetVisualY());
        for (int i = 0; i < legado.quantidadeQuadros(); i++) {
            assertArrayEquals(legado.copiarQuadroAplicador(i), json.copiarQuadroAplicador(i));
            assertArrayEquals(legado.copiarQuadroReceptor(i), json.copiarQuadroReceptor(i));
            assertEquals(legado.deslocamentoAplicadorX(i), json.deslocamentoAplicadorX(i));
            assertEquals(legado.deslocamentoReceptorX(i), json.deslocamentoReceptorX(i));
        }
        for (int f = 0; f <= 30; f++) {
            assertEquals(legado.indiceParaProgresso(f / 30.0), json.indiceParaProgresso(f / 30.0),
                    "frame " + f);
        }
    }

    @Test
    void metadadosDeAutoriaDaPegada1() {
        AnimacaoConjuntaMiauStudio a = importarPegada1();

        assertEquals("PEGADA_1", a.getNomeAnimacao());
        assertEquals("Branco", a.getNomeAplicador());
        assertEquals("Preto", a.getNomeReceptor());
        assertEquals(360, a.getCentroAutoriaX());
        assertEquals(2, a.getLinhaAutoria());
        assertEquals(0, a.getOffsetVisualX());
        assertEquals(0, a.getOffsetVisualY());
        // O centro de autoria NAO entra nos quadros: cenaX continua relativo.
        assertEquals(56, a.copiarQuadrosAplicador()[0][0]);
        assertEquals(140, a.copiarQuadrosReceptor()[0][0]);
    }

    @Test
    void projetoOriginalNaoEAlteradoPelaImportacao() throws Exception {
        importarPegada1();
        importarPegada1();

        byte[] bytes = ProjetoMiauStudioTeste.lerRecurso(PEGADA_1);
        String hash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        assertEquals(SHA256_PEGADA_1_ORIGINAL, hash);
    }

    // =====================================================
    // QUANTIDADE VARIAVEL, APLICADOR PRETO, OFFSETS
    // =====================================================
    @Test
    void importaAnimacaoFicticiaDeCincoQuadrosComAplicadorPreto() {
        String caminho = "/conteudo-teste/miaustudio/TESTE_5_QUADROS.json";
        AnimacaoConjuntaMiauStudio a = ConversorMiauStudio.importarConjunta(
                ProjetoMiauStudioTeste.lerTexto(caminho), caminho);

        assertEquals(5, a.quantidadeQuadros());
        assertEquals("Preto", a.getNomeAplicador());
        assertEquals("Branco", a.getNomeReceptor());
        assertEquals(400, a.getCentroAutoriaX());
        assertEquals(1, a.getLinhaAutoria());
        assertEquals(6, a.getOffsetVisualX());
        assertEquals(-4, a.getOffsetVisualY());

        int[][] aplicador = a.copiarQuadrosAplicador();   // = "preto" no JSON
        int[][] receptor = a.copiarQuadrosReceptor();     // = "branco" no JSON
        for (int q = 0; q < 5; q++) {
            int basePreto = -(10 * q + 1);
            assertEquals(-60 + q * 4, aplicador[q][0], "cenaX aplicador q" + q);
            assertEquals(0, aplicador[q][1]);
            assertEquals(7, aplicador[q][2]);
            assertEquals(1, aplicador[q][3], "Preto virado para a direita");
            for (int i = 0; i < 26; i++) {
                assertEquals(basePreto + i, aplicador[q][4 + i], "articulacao " + i + " q" + q);
            }

            int baseBranco = q < 3 ? 100 * q + 1 : 300 + q;
            assertEquals(100 + q * 2, receptor[q][0]);
            assertEquals(q < 3 ? 0 : 1, receptor[q][1], "offsetLinha q" + q);
            assertEquals(0, receptor[q][3], "Branco virado para a esquerda");
            for (int i = 0; i < 26; i++) {
                assertEquals(baseBranco + i, receptor[q][4 + i]);
            }
        }

        AnimacaoConjuntaQuadros quadros = a.paraQuadros();
        assertEquals(6.0, quadros.deslocamentoAplicadorX(0));
        assertEquals(6.0 + 16, quadros.deslocamentoAplicadorX(4));
        assertEquals(-4.0, quadros.getOffsetVisualY());
    }

    @Test
    void aceitaDeUmAteSessentaQuadros() {
        assertEquals(1, ConversorMiauStudio.importarConjunta(projetoValido(1), "x").quantidadeQuadros());
        assertEquals(7, ConversorMiauStudio.importarConjunta(projetoValido(7), "x").quantidadeQuadros());
        assertEquals(60, ConversorMiauStudio.importarConjunta(projetoValido(60), "x").quantidadeQuadros());

        String msg = mensagemDeErro(projetoValido(61));
        assertTrue(msg.contains("61 quadros"), msg);
    }

    @Test
    void mapeiaCadaCampoNoIndiceExportadoPeloMiauStudio() {
        // Mesma ordem de PoseSalva.paraLinhaJava() no editor.
        String[] ordem = {
            "offsetCentroX", "offsetLinha", "escalaX10", "viradoDireita",
            "offCinturaX", "offCinturaY", "rotCintura", "offPeitoX", "offPeitoY", "rotPeito",
            "rotGlobal", "rotCabeca", "offCabX", "offCabY",
            "offCotEsqX", "offCotEsqY", "offMaoEsqX", "offMaoEsqY",
            "offCotDirX", "offCotDirY", "offMaoDirX", "offMaoDirY",
            "offJoelEsqX", "offJoelEsqY", "offPeEsqX", "offPeEsqY",
            "offJoelDirX", "offJoelDirY", "offPeDirX", "offPeDirY"
        };
        for (int indice = 4; indice < 30; indice++) {
            Map<String, String> p = pose(0, true);
            for (String c : ordem) {
                if (!c.equals("escalaX10") && !c.equals("viradoDireita") && !c.equals("offsetLinha")) {
                    p.put(c, "0");
                }
            }
            p.put(ordem[indice], "77");
            String texto = projeto("", quadro(json(p), json(pose(0, false))));
            int[] resultado = ConversorMiauStudio.importarConjunta(texto, "x")
                    .copiarQuadrosAplicador()[0];
            for (int i = 4; i < 30; i++) {
                assertEquals(i == indice ? 77 : 0, resultado[i],
                        ordem[indice] + " deveria estar so no indice " + indice);
            }
        }
    }

    // =====================================================
    // ERROS COMPREENSIVEIS
    // =====================================================
    @Test
    void rejeitaTipoEVersaoErrados() {
        String valido = projetoValido(2);

        String msg = mensagemDeErro(valido.replace("\"MIAUSTUDIO_PROJECT\"", "\"OUTRO\""));
        assertTrue(msg.contains("[teste.json] tipo"), msg);

        msg = mensagemDeErro(valido.replace("\"versao\": 2", "\"versao\": 1"));
        assertTrue(msg.contains("versao 1 nao suportada"), msg);
    }

    @Test
    void rejeitaAplicadorDesconhecido() {
        String msg = mensagemDeErro(projetoValido(1)
                .replace("\"aplicadorConjunto\": \"Branco\"", "\"aplicadorConjunto\": \"Verde\""));
        assertTrue(msg.contains("aplicadorConjunto"), msg);
        assertTrue(msg.contains("Verde"), msg);
    }

    @Test
    void rejeitaTimelineAusenteOuVazia() {
        String semTimeline = projetoValido(1).replaceAll("\"timelineConjunta\": \\[.*\\]", "\"outro\": 1");
        assertTrue(mensagemDeErro(semTimeline).contains("nao possui animacao conjunta"));

        String vazia = projeto("");
        assertTrue(mensagemDeErro(vazia).contains("nao possui quadros"));
    }

    @Test
    void rejeitaQuadroNuloOuIncompleto() {
        String msg = mensagemDeErro(projeto("", quadroValido(1), "null"));
        assertTrue(msg.contains("timelineConjunta[1]"), msg);
        assertTrue(msg.contains("item 2 esta vazio"), msg);

        msg = mensagemDeErro(projeto("", quadroValido(1), quadro(json(pose(1, true)), null)));
        assertTrue(msg.contains("timelineConjunta[1].preto"), msg);
        assertTrue(msg.contains("quadro 2 nao possui a pose do preto"), msg);
    }

    @Test
    void rejeitaCampoDeArticulacaoAusenteComCaminhoExato() {
        Map<String, String> p = pose(1, false);
        p.remove("offMaoEsqY");
        String msg = mensagemDeErro(projeto("", quadroValido(1), quadroValido(2),
                quadro(json(pose(3, true)), json(p))));

        assertTrue(msg.contains("timelineConjunta[2].preto.offMaoEsqY"), msg);
        assertTrue(msg.contains("ausente"), msg);
    }

    @Test
    void rejeitaTiposEValoresInvalidos() {
        Map<String, String> p = pose(1, true);
        p.put("rotCabeca", "\"dez\"");
        assertTrue(mensagemDeErro(projeto("", quadro(json(p), json(pose(1, false)))))
                .contains("branco.rotCabeca: era esperado numero inteiro"));

        p = pose(1, true);
        p.put("offCabX", "1.5");
        assertTrue(mensagemDeErro(projeto("", quadro(json(p), json(pose(1, false)))))
                .contains("branco.offCabX"));

        p = pose(1, true);
        p.put("escalaX10", "99");
        assertTrue(mensagemDeErro(projeto("", quadro(json(p), json(pose(1, false)))))
                .contains("branco.escalaX10: valor 99 fora do intervalo"));

        p = pose(1, true);
        p.put("viradoDireita", "1");
        assertTrue(mensagemDeErro(projeto("", quadro(json(p), json(pose(1, false)))))
                .contains("branco.viradoDireita: era esperado true/false"));
    }

    @Test
    void erroDeSintaxeInformaLinhaEColuna() {
        String quebrado = projetoValido(1).replace("\"versao\": 2,", "\"versao\": 2");
        String msg = mensagemDeErro(quebrado);
        assertTrue(msg.startsWith("[teste.json] JSON invalido na linha 4"), msg);
    }

    @Test
    void numerosInteirosEscritosComPontoZeroSaoAceitos() {
        String texto = projetoValido(1).replace("\"centroInteracaoX\": 360", "\"centroInteracaoX\": 360.0");
        assertEquals(360, ConversorMiauStudio.importarConjunta(texto, "x").getCentroAutoriaX());
    }
}
