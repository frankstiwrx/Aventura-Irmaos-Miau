package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fluxo completo da PEGADA_1 no SistemaCombate, com os personagens reais
 * e sem janela (headless). Reproduz o teste manual "tecla H" do
 * MainPrototipo: um tick do teste = um tick de SistemaCombate.atualizar().
 *
 * Posicoes: escala 0.7, Branco em x=413 e Preto em x=500 -> 87 px entre
 * as cinturas = 124.3 unidades (alcance da PEGADA_1 = 125 u).
 */
class Pegada1SistemaCombateTest {

    private static final String PEGADA_1 = "pegada_1";
    private static final double ESCALA = 0.7;
    private static final int Y = 150;

    private IrmaoMiau branco;
    private IrmaoMiau preto;
    private SistemaCombate sistema;
    private AnimacaoConjuntaQuadros quadros;

    @BeforeEach
    void prepararLutadoresNoAlcance() {
        branco = new IrmaoMiauBranco(413, Y, ESCALA);
        preto = new IrmaoMiauPreto(500, Y, ESCALA);
        // Mesma orientacao que o GamePanel aplica: frente a frente.
        branco.setViradoParaDireita(true);
        preto.setViradoParaDireita(false);

        sistema = new SistemaCombate(branco, preto);
        quadros = new RegistroAnimacoesCombate().buscar("pegada_1_entrada");
        assertNotNull(quadros);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            sistema.atualizar();
        }
    }

    private void assertQuadroSincronizado(int indice) {
        assertArrayEquals(quadros.copiarQuadroAplicador(indice),
                branco.copiarPoseCombateExterna(),
                "Branco fora do quadro " + (indice + 1));
        assertArrayEquals(quadros.copiarQuadroReceptor(indice),
                preto.copiarPoseCombateExterna(),
                "Preto fora do quadro " + (indice + 1));
        assertEquals(quadros.deslocamentoAplicadorX(indice),
                branco.getDeslocamentoVisualCombateX());
        assertEquals(quadros.deslocamentoReceptorX(indice),
                preto.getDeslocamentoVisualCombateX());
    }

    // =====================================================
    // REQUISITOS
    // =====================================================
    @Test
    void foraDoAlcanceEhIndisponivelENaoCobraFolego() {
        IrmaoMiau b = new IrmaoMiauBranco(220, Y, ESCALA);
        IrmaoMiau p = new IrmaoMiauPreto(500, Y, ESCALA);
        SistemaCombate s = new SistemaCombate(b, p);
        double folego = b.getFolegoAtual();

        assertFalse(s.podeTentarAcao(b, p, PEGADA_1));
        assertEquals(ResultadoAcao.INDISPONIVEL, s.tentarAcao(b, p, PEGADA_1));
        assertEquals(folego, b.getFolegoAtual());
        assertEquals(EstadoInteracaoCombate.LIVRE, s.getEstadoInteracao());
        assertFalse(b.isPoseCombateExternaAtiva());
    }

    @Test
    void limiteDeAlcanceEhDe125Unidades() {
        assertTrue(sistema.podeTentarAcao(branco, preto, PEGADA_1));

        // 1 px a mais (88 px = 125.7 u) ja fica fora.
        branco.moverX(-1);
        sistema.atualizar();
        assertFalse(sistema.podeTentarAcao(branco, preto, PEGADA_1));
    }

    @Test
    void exigeEstarDeFrenteParaOPreto() {
        branco.setViradoParaDireita(false);
        sistema.atualizar();

        assertFalse(sistema.podeTentarAcao(branco, preto, PEGADA_1));
    }

    @Test
    void aceitaUmaLinhaDeDiferencaMasNaoDuas() {
        preto.descerLinha();
        assertTrue(sistema.podeTentarAcao(branco, preto, PEGADA_1));

        preto.descerLinha();
        assertFalse(sistema.podeTentarAcao(branco, preto, PEGADA_1));
    }

    @Test
    void semFolegoSuficienteEhIndisponivel() {
        // 5P.1 (pedido): custo da pegada = 3.0 x 0,6 = 1,8; deixa o Branco com 1,5.
        assertTrue(branco.consumirFolegoCombate(branco.getFolegoAtual() - 1.5));

        assertEquals(ResultadoAcao.INDISPONIVEL,
                sistema.tentarAcao(branco, preto, PEGADA_1));
        assertEquals(1.5, branco.getFolegoAtual(), 1e-9);
        assertFalse(sistema.estaEmInteracao());
    }

    // =====================================================
    // SUCESSO, CUSTO E ESTADO COMPARTILHADO
    // =====================================================
    @Test
    void sucessoCobraFolegoUmaUnicaVezEIniciaInteracao() {
        double folegoBranco = branco.getFolegoAtual();
        double folegoPreto = preto.getFolegoAtual();

        assertEquals(ResultadoAcao.SUCESSO,
                sistema.tentarAcao(branco, preto, PEGADA_1));

        assertEquals(folegoBranco - (3.0 * com.irmaosmiau.entities.CustosFolego.FATOR_CUSTO_PEGADAS), branco.getFolegoAtual(), 1e-9);   // 5P.1 (pedido)
        assertEquals(folegoPreto, preto.getFolegoAtual(), 1e-9);
        assertEquals(EstadoInteracaoCombate.PEGADA_1, sistema.getEstadoInteracao());
        assertTrue(sistema.estaEmPegada1());
        assertSame(branco, sistema.getAplicadorInteracao());
        assertSame(preto, sistema.getReceptorInteracao());
        assertEquals(PEGADA_1, sistema.getIdAcaoInteracao());

        // Apertar H de novo (durante e depois da animacao) nao paga outra vez.
        assertEquals(ResultadoAcao.INDISPONIVEL,
                sistema.tentarAcao(branco, preto, PEGADA_1));
        ticks(40);
        assertEquals(ResultadoAcao.INDISPONIVEL,
                sistema.tentarAcao(branco, preto, PEGADA_1));
        assertEquals(folegoBranco - (3.0 * com.irmaosmiau.entities.CustosFolego.FATOR_CUSTO_PEGADAS), branco.getFolegoAtual(), 1e-9);   // 5P.1 (pedido)
    }

    // =====================================================
    // TIMELINE: 3 QUADROS SINCRONIZADOS EM 30 TICKS
    // =====================================================
    @Test
    void tresQuadrosAparecemSincronizadosNosDoisLutadores() {
        sistema.tentarAcao(branco, preto, PEGADA_1);

        // Quadro 1 ja no tick do H.
        assertTrue(sistema.isAnimacaoCombateAtiva());
        assertQuadroSincronizado(0);

        ticks(9);   // frame 9
        assertQuadroSincronizado(0);

        ticks(1);   // frame 10
        assertQuadroSincronizado(1);

        ticks(9);   // frame 19
        assertQuadroSincronizado(1);

        ticks(1);   // frame 20
        assertQuadroSincronizado(2);

        ticks(9);   // frame 29
        assertQuadroSincronizado(2);
        assertTrue(sistema.isAnimacaoCombateAtiva());
    }

    @Test
    void ambosFicamBloqueadosDuranteAEntrada() {
        sistema.tentarAcao(branco, preto, PEGADA_1);

        for (int f = 0; f < 30; f++) {
            assertTrue(branco.isBloqueadoPorAnimacaoCombate(), "Branco frame " + f);
            assertTrue(preto.isBloqueadoPorAnimacaoCombate(), "Preto frame " + f);
            sistema.atualizar();
        }
    }

    @Test
    void poseFinalPersisteEControleVoltaAposATimeline() {
        sistema.tentarAcao(branco, preto, PEGADA_1);
        ticks(30);

        assertFalse(sistema.isAnimacaoCombateAtiva());
        assertNull(sistema.getAnimacaoCombateAtiva());
        assertFalse(branco.isBloqueadoPorAnimacaoCombate());
        assertFalse(preto.isBloqueadoPorAnimacaoCombate());

        assertEquals(EstadoInteracaoCombate.PEGADA_1, sistema.getEstadoInteracao());
        assertQuadroSincronizado(2);

        // A terceira pose continua enquanto nada romper a pegada.
        ticks(120);
        assertEquals(EstadoInteracaoCombate.PEGADA_1, sistema.getEstadoInteracao());
        assertQuadroSincronizado(2);
    }

    @Test
    void posicaoFisicaNaoMudaDuranteAAnimacao() {
        int xBranco = branco.getX();
        int xPreto = preto.getX();

        sistema.tentarAcao(branco, preto, PEGADA_1);
        ticks(30);

        assertEquals(xBranco, branco.getX());
        assertEquals(xPreto, preto.getX());
    }

    // =====================================================
    // ROMPIMENTO
    // =====================================================
    @Test
    void afastarAlemDoAlcanceRompeELimpaAsPoses() {
        sistema.tentarAcao(branco, preto, PEGADA_1);
        ticks(30);

        branco.moverX(-1);   // 88 px = 125.7 u > 125 u
        sistema.atualizar();

        assertEquals(EstadoInteracaoCombate.LIVRE, sistema.getEstadoInteracao());
        assertNull(sistema.getAplicadorInteracao());
        assertNull(sistema.getReceptorInteracao());
        assertFalse(branco.isPoseCombateExternaAtiva());
        assertFalse(preto.isPoseCombateExternaAtiva());
        assertEquals(0.0, branco.getDeslocamentoVisualCombateX());
        assertEquals(0.0, preto.getDeslocamentoVisualCombateX());

        // Desenho normal volta e a pegada pode ser tentada de novo.
        branco.moverX(1);
        sistema.atualizar();
        assertTrue(sistema.podeTentarAcao(branco, preto, PEGADA_1));
    }

    @Test
    void romperDuranteAEntradaCancelaAAnimacaoEDesbloqueia() {
        sistema.tentarAcao(branco, preto, PEGADA_1);
        ticks(5);

        preto.moverX(10);
        sistema.atualizar();

        assertFalse(sistema.isAnimacaoCombateAtiva());
        assertFalse(sistema.estaEmInteracao());
        assertFalse(branco.isBloqueadoPorAnimacaoCombate());
        assertFalse(preto.isBloqueadoPorAnimacaoCombate());
        assertFalse(branco.isPoseCombateExternaAtiva());
        assertFalse(preto.isPoseCombateExternaAtiva());
    }

    @Test
    void mudarDuasLinhasOuVirarDeCostasRompe() {
        sistema.tentarAcao(branco, preto, PEGADA_1);
        ticks(30);
        preto.descerLinha();
        preto.descerLinha();
        sistema.atualizar();
        assertFalse(sistema.estaEmInteracao());

        preto.subirLinha();
        preto.subirLinha();
        sistema.atualizar();
        sistema.tentarAcao(branco, preto, PEGADA_1);
        ticks(30);
        branco.setViradoParaDireita(false);
        sistema.atualizar();
        assertFalse(sistema.estaEmInteracao());
        assertFalse(branco.isPoseCombateExternaAtiva());
    }

    @Test
    void encerrarInteracaoManualLimpaTudo() {
        sistema.tentarAcao(branco, preto, PEGADA_1);
        ticks(30);

        sistema.encerrarInteracao();

        assertEquals(EstadoInteracaoCombate.LIVRE, sistema.getEstadoInteracao());
        assertFalse(branco.isPoseCombateExternaAtiva());
        assertFalse(preto.isPoseCombateExternaAtiva());
        assertFalse(branco.isBloqueadoPorAnimacaoCombate());
        assertFalse(preto.isBloqueadoPorAnimacaoCombate());
    }

    // =====================================================
    // VALIDACOES DO SISTEMA
    // =====================================================
    @Test
    void personagensDeForaDoCombateSaoRejeitados() {
        IrmaoMiau intruso = new IrmaoMiauPreto(450, Y, ESCALA);

        assertThrows(IllegalArgumentException.class,
                () -> sistema.tentarAcao(branco, intruso, PEGADA_1));
        assertThrows(IllegalArgumentException.class,
                () -> sistema.tentarAcao(branco, branco, PEGADA_1));
        assertThrows(IllegalArgumentException.class,
                () -> new SistemaCombate(branco, branco));
        assertThrows(IllegalArgumentException.class,
                () -> new SistemaCombate(branco, null));
    }

    @Test
    void acaoDesconhecidaEhIndisponivel() {
        assertEquals(ResultadoAcao.INDISPONIVEL,
                sistema.tentarAcao(branco, preto, "pegada_9"));
        assertFalse(sistema.estaEmInteracao());
    }
}
