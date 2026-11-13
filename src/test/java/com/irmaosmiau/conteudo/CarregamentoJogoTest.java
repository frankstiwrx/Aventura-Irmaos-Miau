package com.irmaosmiau.conteudo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.RegistroAnimacoesCombate;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Entrega 3A: carregamento do conteudo no fluxo do jogo
 * (CarregadorConteudoCombate.carregarParaJogo) e falhas diagnosticaveis.
 */
class CarregamentoJogoTest {

    private static final String PREFIXO = "Falha ao carregar o conteudo de combate";

    /** Classpath real, com alguns arquivos substituidos ou removidos. */
    private static FonteConteudo classpathCom(Map<String, String> trocas) {
        FonteConteudo classpath = FonteConteudo.classpath();
        return caminho -> {
            if (trocas.containsKey(caminho)) {
                String v = trocas.get(caminho);
                if (v == null) {
                    throw new ConteudoInvalidoException("Recurso nao encontrado no classpath: " + caminho);
                }
                return v;
            }
            return classpath.ler(caminho);
        };
    }

    private static String falha(Map<String, String> trocas) {
        String msg = assertThrows(ConteudoInvalidoException.class,
                () -> CarregadorConteudoCombate.carregarParaJogo(classpathCom(trocas))).getMessage();
        assertTrue(msg.startsWith(PREFIXO), msg);
        return msg;
    }

    @Test
    void classpathRealCarregaTudoComOrigemNoJson() {
        ConteudoCombate c = CarregadorConteudoCombate.carregarParaJogo();

        assertEquals("/conteudo/miaustudio/PEGADA_1.json",
                c.getRegistroAnimacoes().getOrigem("pegada_1_entrada"));
        assertEquals(4, c.getRegistroAcoes().quantidade());
    }

    @Test
    void projetoMiauStudioAusenteFalhaSemTrocarPorArraysLegados() {
        Map<String, String> trocas = new HashMap<>();
        trocas.put("/conteudo/miaustudio/PEGADA_1.json", null);

        String msg = falha(trocas);
        assertTrue(msg.contains("/conteudo/miaustudio/PEGADA_1.json"), msg);
    }

    @Test
    void projetoMiauStudioCorrompidoInformaArquivoELinha() {
        Map<String, String> trocas = new HashMap<>();
        trocas.put("/conteudo/miaustudio/PEGADA_1.json", "{\n \"tipo\": \"MIAUSTUDIO_PROJECT\",\n \"versao\": 2,,\n}");

        String msg = falha(trocas);
        assertTrue(msg.contains("[/conteudo/miaustudio/PEGADA_1.json]"), msg);
        assertTrue(msg.contains("linha 3"), msg);
    }

    @Test
    void catalogosAusentesFalham() {
        Map<String, String> semTecnicas = new HashMap<>();
        semTecnicas.put(CarregadorConteudoCombate.CATALOGO_TECNICAS_PADRAO, null);
        assertTrue(falha(semTecnicas).contains("tecnicas.json"));

        Map<String, String> semAnimacoes = new HashMap<>();
        semAnimacoes.put(CarregadorConteudoCombate.CATALOGO_ANIMACOES_PADRAO, null);
        assertTrue(falha(semAnimacoes).contains("animacoes.json"));
    }

    @Test
    void tecnicaApontandoParaAnimacaoInexistenteFalha() {
        Map<String, String> trocas = new HashMap<>();
        trocas.put(CarregadorConteudoCombate.CATALOGO_ANIMACOES_PADRAO,
                "{\"tipo\": \"CATALOGO_ANIMACOES_COMBATE\", \"versao\": 1, \"animacoes\": []}");

        String msg = falha(trocas);
        assertTrue(msg.contains("animacao inexistente: pegada_1_entrada"), msg);
    }

    @Test
    void registroLegadoContinuaDisponivelComOrigemJava() {
        assertEquals(RegistroAnimacoesCombate.ORIGEM_CODIGO_JAVA,
                new RegistroAnimacoesCombate().getOrigem("pegada_1_entrada"));
    }
}
