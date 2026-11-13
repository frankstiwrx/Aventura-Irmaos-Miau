package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Entrega 2: registro de animacoes, Builder de AcaoCombate, distribuicao
 * temporal, injecao de registros e tecnicas pendentes.
 */
class RegistrosExpansiveisTest {

    private static int[] pose(int marcador) {
        int[] p = new int[30];
        p[29] = marcador;
        return p;
    }

    private static AnimacaoConjuntaQuadros animacao(int quadros) {
        int[][] a = new int[quadros][];
        int[][] r = new int[quadros][];
        for (int i = 0; i < quadros; i++) {
            a[i] = pose(i);
            r[i] = pose(100 + i);
        }
        return new AnimacaoConjuntaQuadros(a, r, 0, 0);
    }

    // =====================================================
    // RegistroAnimacoesCombate
    // =====================================================
    @Test
    void registroVazioNaoTemNada() {
        RegistroAnimacoesCombate r = RegistroAnimacoesCombate.vazio();
        assertEquals(0, r.quantidade());
        assertFalse(r.contem("pegada_1_entrada"));
    }

    @Test
    void registroLegadoContinuaComAPegada1() {
        RegistroAnimacoesCombate r = new RegistroAnimacoesCombate();
        assertEquals(Set.of("pegada_1_entrada"), r.ids());
        assertTrue(r.contem("PEGADA_1_ENTRADA"));
    }

    @Test
    void animacaoDuplicadaERejeitada() {
        RegistroAnimacoesCombate r = new RegistroAnimacoesCombate();
        String msg = assertThrows(IllegalArgumentException.class,
                () -> r.registrar(" Pegada_1_Entrada ", animacao(2))).getMessage();
        assertTrue(msg.contains("pegada_1_entrada"), msg);
        // A original permanece.
        assertEquals(3, r.buscar("pegada_1_entrada").quantidadeQuadros());
    }

    @Test
    void animacaoInexistenteDaErroClaro() {
        RegistroAnimacoesCombate r = new RegistroAnimacoesCombate();
        assertNull(r.buscar("nao_existe"));
        String msg = assertThrows(IllegalArgumentException.class,
                () -> r.exigir("nao_existe")).getMessage();
        assertTrue(msg.contains("nao_existe"), msg);
        assertTrue(msg.contains("pegada_1_entrada"), msg);
    }

    @Test
    void animacaoFicticiaPodeSerRegistradaSemTocarNosRenderers() {
        RegistroAnimacoesCombate r = new RegistroAnimacoesCombate();
        r.registrar("ficticia_7", animacao(7));
        assertEquals(2, r.quantidade());
        assertEquals(7, r.exigir("ficticia_7").quantidadeQuadros());
    }

    // =====================================================
    // Distribuicao temporal
    // =====================================================
    @Test
    void distribuicaoIgualAosTercosReproduzOComportamentoOriginal() {
        AnimacaoConjuntaQuadros original = new RegistroAnimacoesCombate().buscar("pegada_1_entrada");
        int[][] a = new int[3][];
        int[][] r = new int[3][];
        for (int i = 0; i < 3; i++) {
            a[i] = original.copiarQuadroAplicador(i);
            r[i] = original.copiarQuadroReceptor(i);
        }
        AnimacaoConjuntaQuadros distribuida =
                new AnimacaoConjuntaQuadros(a, r, 0, 0, 30, new int[]{10, 10, 10});

        for (int f = 0; f <= 30; f++) {
            assertEquals(original.indiceParaProgresso(f / 30.0),
                    distribuida.indiceParaProgresso(f / 30.0), "frame " + f);
        }
    }

    @Test
    void distribuicaoDesigualTrocaOsQuadrosNosLimitesCertos() {
        AnimacaoConjuntaQuadros a = new AnimacaoConjuntaQuadros(
                new int[][]{pose(0), pose(1), pose(2)}, new int[][]{pose(0), pose(1), pose(2)},
                0, 0, 0, new int[]{2, 6, 12});

        assertEquals(20, a.getDuracaoTotalFrames());
        assertTrue(a.possuiDistribuicaoTemporal());
        assertArrayEquals(new int[]{2, 6, 12}, a.copiarDuracoesQuadros());
        int[] esperado = {0, 0, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2};
        for (int f = 0; f <= 20; f++) {
            assertEquals(esperado[f], a.indiceParaProgresso(f / 20.0), "frame " + f);
        }
    }

    @Test
    void distribuicaoTemporalEValidada() {
        int[][] q = {pose(0), pose(1)};
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoConjuntaQuadros(q, q, 0, 0, 0, new int[]{5}));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoConjuntaQuadros(q, q, 0, 0, 0, new int[]{5, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoConjuntaQuadros(q, q, 0, 0, 30, new int[]{5, 5}));
        assertThrows(IllegalArgumentException.class,
                () -> new AnimacaoConjuntaQuadros(q, q, 0, 0, -1, null));

        AnimacaoConjuntaQuadros semDistribuicao = new AnimacaoConjuntaQuadros(q, q, 0, 0);
        assertFalse(semDistribuicao.possuiDistribuicaoTemporal());
        assertNull(semDistribuicao.copiarDuracoesQuadros());
        assertEquals(0, semDistribuicao.getDuracaoTotalFrames());
    }

    // =====================================================
    // Builder de AcaoCombate
    // =====================================================
    @Test
    void builderReproduzExatamenteAPegada1Legada() {
        AcaoCombate legado = new RegistroAcoes().buscar("pegada_1");
        AcaoCombate b = AcaoCombate.builder("pegada_1", "Pegada 1", CategoriaAcao.PEGADA)
                .estadoInteracaoAoSucesso(EstadoInteracaoCombate.PEGADA_1)
                .animacao("pegada_1_entrada", 30)
                .alcanceHorizontal(125.0)
                .diferencaMaximaLinhas(1)
                .exigeFrenteParaOponente(true)
                .custoFolego(3.0)
                .dificuldade(20.0)
                .pesos(0.50, 0.30, 0.20, 0.00)
                .build();

        AssercoesAcao.assertMesmasRegras(legado, b);
    }

    @Test
    void builderTemPadroesSegurosEValida() {
        AcaoCombate a = AcaoCombate.builder("x", "X", CategoriaAcao.ESCAPE).build();
        assertTrue(a.isJogavel());
        assertFalse(a.possuiAnimacaoConjunta());
        assertNull(a.getEstadoInteracaoAoSucesso());
        assertNull(a.getDescricao());
        assertEquals(0.0, a.getAlcanceHorizontal());

        assertThrows(IllegalArgumentException.class,
                () -> AcaoCombate.builder("x", "X", CategoriaAcao.ESCAPE).animacao("a", 0).build());
        assertThrows(IllegalArgumentException.class,
                () -> AcaoCombate.builder("x", "X", CategoriaAcao.ESCAPE).dificuldade(101).build());
        assertThrows(IllegalArgumentException.class,
                () -> AcaoCombate.builder("x", "X", CategoriaAcao.ESCAPE).disponibilidade(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> AcaoCombate.builder(null, "X", CategoriaAcao.ESCAPE).build());
    }

    @Test
    void construtorOriginalCriaAcaoJogavel() {
        AcaoCombate legado = new RegistroAcoes().buscar("pegada_1");
        assertSame(DisponibilidadeAcao.JOGAVEL, legado.getDisponibilidade());
        assertTrue(legado.isJogavel());
        assertNull(legado.getDescricao());
    }

    // =====================================================
    // Injecao no SistemaCombate e tecnicas pendentes
    // =====================================================
    private static IrmaoMiau brancoNoAlcance() {
        IrmaoMiau b = new IrmaoMiauBranco(413, 150, 0.7);
        b.setViradoParaDireita(true);
        return b;
    }

    private static IrmaoMiau pretoNoAlcance() {
        IrmaoMiau p = new IrmaoMiauPreto(500, 150, 0.7);
        p.setViradoParaDireita(false);
        return p;
    }

    @Test
    void sistemaUsaORegistroDeAnimacoesInjetado() {
        RegistroAnimacoesCombate animacoes = RegistroAnimacoesCombate.vazio();
        animacoes.registrar("pegada_1_entrada", animacao(3));
        IrmaoMiau b = brancoNoAlcance();
        IrmaoMiau p = pretoNoAlcance();
        SistemaCombate s = new SistemaCombate(b, p, new RegistroAcoes(), animacoes);

        assertSame(animacoes, s.getRegistroAnimacoes());
        s.tentarAcao(b, p, "pegada_1");
        assertEquals(0, b.copiarPoseCombateExterna()[29]);   // quadro do registro injetado
        assertEquals(100, p.copiarPoseCombateExterna()[29]);
    }

    @Test
    void construtoresAntigosContinuamUsandoORegistroLegado() {
        SistemaCombate s1 = new SistemaCombate(brancoNoAlcance(), pretoNoAlcance());
        SistemaCombate s2 = new SistemaCombate(brancoNoAlcance(), pretoNoAlcance(), new RegistroAcoes());
        assertEquals(Set.of("pegada_1_entrada"), s1.getRegistroAnimacoes().ids());
        assertEquals(Set.of("pegada_1_entrada"), s2.getRegistroAnimacoes().ids());
    }

    @Test
    void sistemaRecusaAcaoJogavelComAnimacaoInexistente() {
        String msg = assertThrows(IllegalArgumentException.class,
                () -> new SistemaCombate(brancoNoAlcance(), pretoNoAlcance(),
                        new RegistroAcoes(), RegistroAnimacoesCombate.vazio())).getMessage();
        assertTrue(msg.contains("pegada_1_entrada"), msg);

        assertThrows(IllegalArgumentException.class,
                () -> new SistemaCombate(brancoNoAlcance(), pretoNoAlcance(),
                        new RegistroAcoes(), null));
    }

    @Test
    void tecnicaPendenteNuncaExecuta() {
        RegistroAcoes acoes = new RegistroAcoes();
        acoes.registrar(AcaoCombate.builder("pegada_2", "Pegada 2", CategoriaAcao.PEGADA)
                .disponibilidade(DisponibilidadeAcao.PENDENTE)
                // Mesmo com requisitos "faceis" e uma animacao que nao existe:
                .animacao("animacao_que_ainda_nao_existe", 30)
                .alcanceHorizontal(10_000)
                .diferencaMaximaLinhas(4)
                .build());

        IrmaoMiau b = brancoNoAlcance();
        IrmaoMiau p = pretoNoAlcance();
        // Pendente com animacao ausente nao impede criar o sistema.
        SistemaCombate s = new SistemaCombate(b, p, acoes, new RegistroAnimacoesCombate());
        double folego = b.getFolegoAtual();

        assertFalse(s.podeTentarAcao(b, p, "pegada_2"));
        assertEquals(ResultadoAcao.INDISPONIVEL, s.tentarAcao(b, p, "pegada_2"));
        assertEquals(folego, b.getFolegoAtual());
        assertFalse(s.estaEmInteracao());
        assertFalse(s.isAnimacaoCombateAtiva());
        assertFalse(b.isPoseCombateExternaAtiva());

        // A pegada_1 continua funcionando no mesmo sistema.
        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(b, p, "pegada_1"));
    }
}
