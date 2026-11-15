package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.conteudo.ConteudoCombate;
import com.irmaosmiau.conteudo.json.ObjetoJson;
import com.irmaosmiau.conteudo.miaustudio.AnimacaoConjuntaMiauStudio;
import com.irmaosmiau.conteudo.miaustudio.ConversorMiauStudio;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/** Entrega 5J (testes 1, 2, 3, 5 e 6): variante autoral do Preto e escolha por molde + papel. */
class VariantesPegada5JTest {

    private static final String BASE = "pegada_1_entrada";
    private static final String PRETO = "pegada_1_entrada_preto";
    private static final String RECURSO = "/conteudo/miaustudio/PEGADA_1_PRETO.json";

    private static String ler(String r) throws Exception {
        try (InputStream in = VariantesPegada5JTest.class.getResourceAsStream(r)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static RegistroAnimacoesCombate registro() {
        return CarregadorConteudoCombate.carregarPadrao().getRegistroAnimacoes();
    }

    private static IrmaoMiau b(int x) {
        return new IrmaoMiauBranco(x, 150, 0.7);
    }

    private static IrmaoMiau p(int x) {
        return new IrmaoMiauPreto(x, 150, 0.7);
    }

    // ---------------- 1. leitura e registro
    @Test
    void copiaIdentificadaDoProjetoDoPretoEValidaERegistrada() throws Exception {
        String texto = ler(RECURSO);
        ObjetoJson projeto = ObjetoJson.ler(texto, RECURSO);
        assertEquals("MIAUSTUDIO_PROJECT", projeto.texto("tipo"));
        assertEquals(2, projeto.inteiro("versao"));
        assertEquals("CONJUNTA", projeto.texto("modoAnimacao"));
        assertEquals("Preto", projeto.texto("aplicadorConjunto"));
        assertEquals("PEGADA_1_PRETO", projeto.texto("nomeAnimacao"), "copia identificada (o original dizia nova_animacao)");
        assertEquals(230, projeto.inteiro("intervaloPreviewMs"));

        AnimacaoConjuntaMiauStudio importada = ConversorMiauStudio.importarConjunta(texto, RECURSO);
        assertEquals("Preto", importada.getNomeAplicador());
        assertEquals("Branco", importada.getNomeReceptor());
        assertEquals(3, importada.quantidadeQuadros());

        RegistroAnimacoesCombate r = registro();
        AnimacaoConjuntaQuadros q = r.exigir(PRETO);
        assertEquals(RECURSO, r.getOrigem(PRETO));
        assertEquals(IdentidadeRig.PRETO, r.getIdentidadeAplicador(PRETO));
        assertEquals(IdentidadeRig.BRANCO, r.getIdentidadeReceptor(PRETO));
        assertEquals(30, q.getDuracaoTotalFrames(), "mesma duracao da PEGADA_1");
        // Trilha do APLICADOR = pose "preto" do projeto; do RECEPTOR = pose "branco".
        assertEquals(140, q.copiarQuadroAplicador(0)[0]);
        assertEquals(51, q.copiarQuadroAplicador(1)[14], "offCotEsqX do Preto no quadro 2");
        assertEquals(65, q.copiarQuadroReceptor(0)[0]);
        assertEquals(77, q.copiarQuadroReceptor(2)[0]);
        assertEquals(0.0, q.deslocamentoAplicadorX(2), "o Preto aplicador nao se desloca");
        assertEquals(12.0, q.deslocamentoReceptorX(2), "o Branco receptor avanca 12 (77 - 65), sem teletransporte");
    }

    // ---------------- 2. original preservado
    @Test
    void animacaoOriginalDoBrancoContinuaIgual() {
        RegistroAnimacoesCombate r = registro();
        AnimacaoConjuntaQuadros legado = new RegistroAnimacoesCombate().buscar(BASE);
        AnimacaoConjuntaQuadros base = r.exigir(BASE);
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(legado.copiarQuadroAplicador(i), base.copiarQuadroAplicador(i));
            assertArrayEquals(legado.copiarQuadroReceptor(i), base.copiarQuadroReceptor(i));
        }
        assertEquals(IdentidadeRig.BRANCO, r.getIdentidadeAplicador(BASE));
        SelecaoAnimacaoConjunta s = r.resolver(BASE, b(440), p(500));
        assertEquals(SelecaoAnimacaoConjunta.Tipo.AUTORAL, s.tipo());
        assertSame(base, s.quadros(), "Branco aplicando no Preto: exatamente a base");
    }

    // ---------------- 3 e 5. escolha por molde e papel, em todas as combinacoes
    @Test
    void combinacoesEscolhemPorMoldeEPapel() {
        RegistroAnimacoesCombate r = registro();
        AnimacaoConjuntaQuadros base = r.exigir(BASE);
        AnimacaoConjuntaQuadros preto = r.exigir(PRETO);

        SelecaoAnimacaoConjunta pb = r.resolver(BASE, p(500), b(440));
        assertEquals(SelecaoAnimacaoConjunta.Tipo.AUTORAL, pb.tipo());
        assertEquals(PRETO, pb.idTrilhaAplicador());
        assertSame(preto, pb.quadros(), "Preto aplicando no Branco: a variante autoral");

        SelecaoAnimacaoConjunta bb = r.resolver(BASE, b(440), b(500));
        assertEquals(SelecaoAnimacaoConjunta.Tipo.COMPOSTA_POR_PAPEL, bb.tipo());
        assertEquals(BASE, bb.idTrilhaAplicador(), "Branco aplicador desenhado na base");
        assertEquals(PRETO, bb.idTrilhaReceptor(), "Branco receptor desenhado na variante do Preto");
        SelecaoAnimacaoConjunta pp = r.resolver(BASE, p(440), p(500));
        assertEquals(PRETO, pp.idTrilhaAplicador());
        assertEquals(BASE, pp.idTrilhaReceptor());
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(base.copiarQuadroAplicador(i), bb.quadros().copiarQuadroAplicador(i));
            assertArrayEquals(preto.copiarQuadroReceptor(i), bb.quadros().copiarQuadroReceptor(i));
            assertArrayEquals(preto.copiarQuadroAplicador(i), pp.quadros().copiarQuadroAplicador(i));
            assertArrayEquals(base.copiarQuadroReceptor(i), pp.quadros().copiarQuadroReceptor(i));
        }
        assertEquals(30, bb.quadros().getDuracaoTotalFrames());
        assertEquals(30, pp.quadros().getDuracaoTotalFrames());
        assertSame(bb.quadros(), r.resolver(BASE, b(100), b(300)).quadros(), "composicao reaproveitada");
        assertTrue(pb.descricao().contains("autoral"));
        assertTrue(bb.descricao().contains("composta por papel"));
    }

    @Test
    void ladoDaTelaESlotNaoMudamAEscolha() {
        RegistroAnimacoesCombate r = registro();
        // Preto aplicando a partir da ESQUERDA: mesma variante (o espelho e da orientacao).
        assertEquals(PRETO, r.resolver(BASE, p(100), b(184)).idTrilhaAplicador());
        assertEquals(PRETO, r.resolver(BASE, p(600), b(516)).idTrilhaAplicador());
        assertEquals(BASE, r.resolver(BASE, b(600), p(516)).idTrilhaAplicador());
    }

    @Test
    void semMetadadosOuMoldeDesconhecidoUsaABase() {
        RegistroAnimacoesCombate r = RegistroAnimacoesCombate.vazio();
        r.registrar("x", new RegistroAnimacoesCombate().buscar(BASE));
        assertEquals(SelecaoAnimacaoConjunta.Tipo.BASE, r.resolver("x", p(1), b(2)).tipo());
        assertNull(r.getIdentidadeAplicador("x"));
    }

    @Test
    void varianteIncompativelERecusada() {
        RegistroAnimacoesCombate r = RegistroAnimacoesCombate.vazio();
        AnimacaoConjuntaQuadros base = new RegistroAnimacoesCombate().buscar(BASE);
        r.registrar("a", base);
        r.registrarIdentidades("a", IdentidadeRig.BRANCO, IdentidadeRig.PRETO);
        int[][] doisQuadros = {base.copiarQuadroAplicador(0), base.copiarQuadroAplicador(1)};
        r.registrar("curta", new AnimacaoConjuntaQuadros(doisQuadros, doisQuadros, 0, 0));
        r.registrarIdentidades("curta", IdentidadeRig.PRETO, IdentidadeRig.BRANCO);
        assertThrows(IllegalArgumentException.class, () -> r.registrarVariante("a", "curta"),
                "quadros diferentes: trocar de variante mudaria o tempo da tecnica");
        r.registrar("sem_identidade", base);
        assertThrows(IllegalArgumentException.class, () -> r.registrarVariante("a", "sem_identidade"));
        assertThrows(IllegalArgumentException.class, () -> r.registrarVariante("a", "a"));
    }

    // ---------------- 6. sem troca de identidade no motor
    @Test
    void noMotorCadaLutadorRecebeATrilhaDoProprioMolde() {
        ConteudoCombate c = CarregadorConteudoCombate.carregarPadrao();
        AnimacaoConjuntaQuadros preto = c.getRegistroAnimacoes().exigir(PRETO);
        IrmaoMiau branco = b(440);
        IrmaoMiau pretoL = p(500);
        branco.setViradoParaDireita(true);
        pretoL.setViradoParaDireita(false);
        SistemaCombate s = c.criarSistema(branco, pretoL);
        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(pretoL, branco, "pegada_1"));
        for (int t = 0; t < 30; t++) {
            int i = preto.indiceParaProgresso(t / 30.0);
            assertArrayEquals(preto.copiarQuadroAplicador(i), pretoL.copiarPoseCombateExterna(), "tick " + t);
            assertArrayEquals(preto.copiarQuadroReceptor(i), branco.copiarPoseCombateExterna(), "tick " + t);
            assertEquals(preto.deslocamentoReceptorX(i), branco.getDeslocamentoVisualCombateX());
            s.atualizar();
        }
        assertTrue(s.isInteracaoEstabelecida());
        assertEquals(PRETO, s.getSelecaoAnimacao().idTrilhaAplicador());
        assertArrayEquals(preto.copiarQuadroAplicador(2), pretoL.copiarPoseCombateExterna(), "pose final mantida");
        assertNotEquals(c.getRegistroAnimacoes().exigir(BASE).copiarQuadroAplicador(2)[14],
                pretoL.copiarPoseCombateExterna()[14], "nao e a trilha do Branco");
    }
}
