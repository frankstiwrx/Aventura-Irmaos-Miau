package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.SimuladorLuta.Comando;
import com.irmaosmiau.entities.IrmaoMiau;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Entrega 3A: politica de interacao da PEGADA_1 no fluxo real
 * (conteudo JSON + ordem de tick do GamePanel).
 */
class PoliticaInteracaoTest {

    private static final int[] QUADRO_3_APLICADOR = Pegada1RegressaoArraysTest.APLICADOR_APROVADO[2];
    private static final int[] QUADRO_3_RECEPTOR = Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[2];

    private static void assertPoseFinal(SimuladorLuta s) {
        assertArrayEquals(QUADRO_3_APLICADOR, s.branco.copiarPoseCombateExterna());
        assertArrayEquals(QUADRO_3_RECEPTOR, s.preto.copiarPoseCombateExterna());
        assertEquals(-15.0, s.preto.getDeslocamentoVisualCombateX());
    }

    /** Depois do rompimento nao pode sobrar nada da pegada nos dois. */
    private static void assertTudoLimpo(SimuladorLuta s) {
        assertEquals(EstadoInteracaoCombate.LIVRE, s.sistema.getEstadoInteracao());
        assertFalse(s.sistema.isInteracaoEstabelecida());
        assertFalse(s.sistema.isAnimacaoCombateAtiva());
        for (IrmaoMiau l : new IrmaoMiau[]{s.branco, s.preto}) {
            assertFalse(l.isPoseCombateExternaAtiva(), l.getNome() + " ficou com pose congelada");
            assertFalse(l.isBloqueadoPorAnimacaoCombate(), l.getNome() + " ficou bloqueado");
            assertEquals(0.0, l.getDeslocamentoVisualCombateX());
            assertEquals(0.0, l.getDeslocamentoVisualCombateY());
        }
    }

    // =====================================================
    // PARADO: MANTEM
    // =====================================================
    @Test
    void ficarParadoMantemAPegadaEAPoseFinalIndefinidamente() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();

        for (int i = 0; i < 600; i++) {   // ~10 segundos
            s.tick(Comando.PARADO, Comando.PARADO);
            assertTrue(s.sistema.estaEmPegada1(), "tick " + i);
            assertPoseFinal(s);
        }
        assertNull(s.sistema.getUltimoMotivoEncerramento());
    }

    @Test
    void respirarParadoNaoRompe() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();

        for (int i = 0; i < 120; i++) {
            s.tick(Comando.respirar(), Comando.respirar());
        }
        assertTrue(s.sistema.estaEmPegada1());
        assertPoseFinal(s);
    }

    // =====================================================
    // ENTRADA: 30 FRAMES BLOQUEADOS, COMANDOS IGNORADOS
    // =====================================================
    @Test
    void comandosDuranteAEntradaSaoIgnoradosENaoInterrompem() {
        SimuladorLuta s = new SimuladorLuta();
        SimuladorLuta controle = new SimuladorLuta();   // mesma pegada, sem comandos
        int xBranco = s.branco.getX();
        int xPreto = s.preto.getX();

        controle.sistema.tentarAcao(controle.branco, controle.preto, "pegada_1");
        controle.ticks(30);
        s.sistema.tentarAcao(s.branco, s.preto, "pegada_1");
        Comando[] tentativas = {Comando.andarEsquerda(), Comando.pular(), Comando.deitar(),
            Comando.rolarCima(), Comando.sprawl(), Comando.agachar(), Comando.andarParaBaixo()};
        for (int f = 0; f < 30; f++) {
            assertTrue(s.sistema.isAnimacaoCombateAtiva(), "frame " + f);
            Comando c = tentativas[f % tentativas.length];
            s.tick(c, c);
        }

        assertTrue(s.sistema.isInteracaoEstabelecida());
        assertEquals(xBranco, s.branco.getX());
        assertEquals(xPreto, s.preto.getX());
        assertFalse(s.branco.isPulando() || s.preto.isPulando());
        assertFalse(s.branco.isDeitado() || s.preto.isDeitado());
        assertFalse(s.branco.isRolando() || s.preto.isRolando());
        assertFalse(s.branco.isFazendoSprawl() || s.preto.isFazendoSprawl());
        // Nenhuma acao ignorada gastou folego: igual ao controle parado.
        assertEquals(controle.branco.getFolegoAtual(), s.branco.getFolegoAtual(), 1e-9);
        assertEquals(controle.preto.getFolegoAtual(), s.preto.getFolegoAtual(), 1e-9);
        assertPoseFinal(s);
    }

    @Test
    void entradaContinuaComOsTresQuadrosDoJson() {
        SimuladorLuta s = new SimuladorLuta();
        s.sistema.tentarAcao(s.branco, s.preto, "pegada_1");
        for (int f = 0; f < 30; f++) {
            int q = f / 10;
            assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[q],
                    s.branco.copiarPoseCombateExterna(), "frame " + f);
            assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[q],
                    s.preto.copiarPoseCombateExterna(), "frame " + f);
            s.tick(Comando.andarDireita(), Comando.andarDireita());
        }
        assertPoseFinal(s);
    }

    // =====================================================
    // DEPOIS DA PEGADA: CADA ACAO, PARA CADA LUTADOR
    // =====================================================
    static Stream<Arguments> acoesQueRompem() {
        return Stream.of(
                Arguments.of("andar para frente", (Function<Boolean, Comando>) b -> b ? Comando.andarDireita() : Comando.andarEsquerda(), MotivoEncerramentoInteracao.MOVIMENTO),
                Arguments.of("andar para tras", (Function<Boolean, Comando>) b -> b ? Comando.andarEsquerda() : Comando.andarDireita(), MotivoEncerramentoInteracao.MOVIMENTO),
                Arguments.of("andar em profundidade", (Function<Boolean, Comando>) b -> Comando.andarParaBaixo(), MotivoEncerramentoInteracao.MOVIMENTO),
                Arguments.of("correr", (Function<Boolean, Comando>) b -> Comando.correrDireita(), MotivoEncerramentoInteracao.MOVIMENTO),
                Arguments.of("pular", (Function<Boolean, Comando>) b -> Comando.pular(), MotivoEncerramentoInteracao.PULO),
                Arguments.of("pulo curto", (Function<Boolean, Comando>) b -> Comando.pularCurto(), MotivoEncerramentoInteracao.PULO),
                Arguments.of("agachar", (Function<Boolean, Comando>) b -> Comando.agachar(), MotivoEncerramentoInteracao.AGACHAMENTO),
                Arguments.of("deitar", (Function<Boolean, Comando>) b -> Comando.deitar(), MotivoEncerramentoInteracao.DEITAR_OU_LEVANTAR),
                Arguments.of("rolar", (Function<Boolean, Comando>) b -> Comando.rolarCima(), MotivoEncerramentoInteracao.ROLAMENTO),
                Arguments.of("sprawl", (Function<Boolean, Comando>) b -> Comando.sprawl(), MotivoEncerramentoInteracao.SPRAWL)
        );
    }

    @ParameterizedTest(name = "Branco: {0}")
    @MethodSource("acoesQueRompem")
    void acaoDoBrancoRompeNoMesmoTick(String nome, Function<Boolean, Comando> comando,
            MotivoEncerramentoInteracao motivo) {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();

        s.tick(comando.apply(true), Comando.PARADO);

        assertEquals(motivo, s.sistema.getUltimoMotivoEncerramento());
        assertTudoLimpo(s);
    }

    @ParameterizedTest(name = "Preto: {0}")
    @MethodSource("acoesQueRompem")
    void acaoDoPretoRompeNoMesmoTick(String nome, Function<Boolean, Comando> comando,
            MotivoEncerramentoInteracao motivo) {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();

        s.tick(Comando.PARADO, comando.apply(false));

        assertEquals(motivo, s.sistema.getUltimoMotivoEncerramento());
        assertTudoLimpo(s);
    }

    // =====================================================
    // O MOVIMENTO PEDIDO ACONTECE NORMALMENTE DEPOIS
    // =====================================================
    @Test
    void depoisDeRomperOPuloContinuaNormalmente() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        int yChao = s.preto.getY();

        s.tick(Comando.PARADO, Comando.pular());
        assertTrue(s.preto.isPulando());
        s.ticks(5);

        assertTrue(s.preto.isPulando());
        assertTrue(s.preto.getY() < yChao, "Preto subiu no pulo");
        assertFalse(s.preto.isPoseCombateExternaAtiva());
    }

    @Test
    void depoisDeRomperOAndarContinuaNormalmente() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        int x0 = s.branco.getX();

        for (int i = 0; i < 20; i++) {
            s.tick(Comando.andarEsquerda(), Comando.PARADO);
        }

        assertTrue(s.branco.getX() < x0 - 20, "Branco andou 20 ticks");
        assertTudoLimpo(s);
    }

    @Test
    void depoisDeRomperDeitarOPretoFicaDeitadoSemPoseDeCombate() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();

        s.tick(Comando.PARADO, Comando.deitar());

        assertTrue(s.preto.isDeitado());
        assertTudoLimpo(s);
        s.ticks(30);
        assertTrue(s.preto.isDeitado());
        assertFalse(s.preto.isPoseCombateExternaAtiva());
    }

    // =====================================================
    // ACAO QUE NAO PODE ACONTECER NAO ROMPE
    // =====================================================
    @Test
    void puloImpossivelNaoRompe() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();

        // pular() nao faz nada enquanto respira
        Comando respirandoEPulando = new Comando(l -> {
            l.setRespirando(true);
            l.pular();
        }, false, false);
        s.tick(Comando.PARADO, respirandoEPulando);

        assertFalse(s.preto.isPulando());
        assertTrue(s.sistema.estaEmPegada1());
        assertPoseFinal(s);
    }

    // =====================================================
    // REGRAS ESPACIAIS CONTINUAM
    // =====================================================
    @Test
    void regrasEspaciaisAntigasTemSeusProprosMotivos() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        s.branco.setViradoParaDireita(false);
        s.sistema.atualizar();
        assertEquals(MotivoEncerramentoInteracao.ORIENTACAO, s.sistema.getUltimoMotivoEncerramento());

        SimuladorLuta s2 = new SimuladorLuta();
        s2.sistema.tentarAcao(s2.branco, s2.preto, "pegada_1");
        s2.preto.moverX(40); // durante a entrada: 100 px = 142,9 u
        s2.sistema.atualizar();
        assertEquals(MotivoEncerramentoInteracao.ALCANCE, s2.sistema.getUltimoMotivoEncerramento());
        assertTudoLimpo(s2);

        SimuladorLuta s3 = new SimuladorLuta();
        s3.sistema.tentarAcao(s3.branco, s3.preto, "pegada_1");
        s3.preto.descerLinha();
        s3.preto.descerLinha();
        s3.sistema.atualizar();
        assertEquals(MotivoEncerramentoInteracao.LINHAS, s3.sistema.getUltimoMotivoEncerramento());

        SimuladorLuta s4 = new SimuladorLuta();
        s4.estabelecerPegada1();
        s4.sistema.encerrarInteracao();
        assertEquals(MotivoEncerramentoInteracao.ENCERRAMENTO_MANUAL, s4.sistema.getUltimoMotivoEncerramento());
        assertTudoLimpo(s4);
    }

    // =====================================================
    // REPETICAO APOS O ROMPIMENTO
    // =====================================================
    @Test
    void pegada1PodeSerRepetidaDepoisDoRompimento() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        double folegoDepoisDaPrimeira = s.branco.getFolegoAtual();

        // Preto deita e levanta: rompe e volta ao mesmo lugar.
        s.tick(Comando.PARADO, Comando.deitar());
        assertTudoLimpo(s);
        s.tick(Comando.PARADO, Comando.deitar());
        assertFalse(s.preto.isDeitado());

        double antes = s.branco.getFolegoAtual();
        assertEquals(ResultadoAcao.SUCESSO, s.sistema.tentarAcao(s.branco, s.preto, "pegada_1"));
        assertEquals(antes - 3.0, s.branco.getFolegoAtual(), 1e-9);
        assertTrue(antes >= folegoDepoisDaPrimeira, "folego so recupera, nunca e cobrado de novo sem H");

        for (int f = 0; f < 30; f++) {
            int q = f / 10;
            assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[q],
                    s.branco.copiarPoseCombateExterna(), "segunda pegada frame " + f);
            s.tick(Comando.PARADO, Comando.PARADO);
        }
        assertTrue(s.sistema.isInteracaoEstabelecida());
        assertPoseFinal(s);
    }

    // =====================================================
    // POLITICA INJETAVEL / UNIDADE
    // =====================================================
    @Test
    void politicaPodeSerSubstituidaSemMexerNoResto() {
        PoliticaInteracao nuncaRompe = situacao -> Optional.empty();
        SimuladorLuta s = new SimuladorLuta(nuncaRompe);
        s.estabelecerPegada1();

        for (int i = 0; i < 30; i++) {
            s.tick(Comando.andarEsquerda(), Comando.PARADO);
        }
        assertTrue(s.sistema.estaEmPegada1());
        assertNotEquals(SimuladorLuta.X_BRANCO, s.branco.getX());
        assertThrows(IllegalArgumentException.class, () -> new SistemaCombate(s.branco, s.preto,
                new RegistroAcoes(), new RegistroAnimacoesCombate(), null));
    }

    @Test
    void mudancaCorporalDetectaCadaCampo() {
        EstadoCorporalObservado base = new EstadoCorporalObservado(10, 20, 1, false, false, false, false, false);

        assertEquals(Optional.empty(), PoliticaInteracaoPadrao.mudancaCorporal(base, base));
        assertEquals(Optional.of(MotivoEncerramentoInteracao.MOVIMENTO), PoliticaInteracaoPadrao.mudancaCorporal(base,
                new EstadoCorporalObservado(11, 20, 1, false, false, false, false, false)));
        assertEquals(Optional.of(MotivoEncerramentoInteracao.MOVIMENTO), PoliticaInteracaoPadrao.mudancaCorporal(base,
                new EstadoCorporalObservado(10, 21, 1, false, false, false, false, false)));
        assertEquals(Optional.of(MotivoEncerramentoInteracao.MOVIMENTO), PoliticaInteracaoPadrao.mudancaCorporal(base,
                new EstadoCorporalObservado(10, 20, 2, false, false, false, false, false)));
        assertEquals(Optional.of(MotivoEncerramentoInteracao.PULO), PoliticaInteracaoPadrao.mudancaCorporal(base,
                new EstadoCorporalObservado(10, 5, 1, true, false, false, false, false)));
        assertEquals(Optional.of(MotivoEncerramentoInteracao.AGACHAMENTO), PoliticaInteracaoPadrao.mudancaCorporal(base,
                new EstadoCorporalObservado(10, 20, 1, false, true, false, false, false)));
        assertEquals(Optional.of(MotivoEncerramentoInteracao.DEITAR_OU_LEVANTAR), PoliticaInteracaoPadrao.mudancaCorporal(base,
                new EstadoCorporalObservado(10, 20, 1, false, false, true, false, false)));
        assertEquals(Optional.of(MotivoEncerramentoInteracao.ROLAMENTO), PoliticaInteracaoPadrao.mudancaCorporal(base,
                new EstadoCorporalObservado(30, 20, 1, false, false, false, true, false)));
        assertEquals(Optional.of(MotivoEncerramentoInteracao.SPRAWL), PoliticaInteracaoPadrao.mudancaCorporal(base,
                new EstadoCorporalObservado(10, 20, 1, false, false, false, false, true)));

        // Ja deitado no estabelecimento e continua deitado: nao rompe.
        EstadoCorporalObservado deitado = new EstadoCorporalObservado(10, 20, 1, false, false, true, false, false);
        assertEquals(Optional.empty(), PoliticaInteracaoPadrao.mudancaCorporal(deitado, deitado));
    }

    @Test
    void situacaoEstabelecidaExigeRetratos() {
        SimuladorLuta s = new SimuladorLuta();
        AcaoCombate acao = s.sistema.getAcao("pegada_1");
        ContextoCombate c = s.sistema.getContextoAParaBAtual();
        EstadoCorporalObservado e = EstadoCorporalObservado.de(s.branco);

        assertThrows(IllegalArgumentException.class,
                () -> new SituacaoInteracao(acao, c, false, null, null, e, e));
        new SituacaoInteracao(acao, c, true, null, null, e, e); // entrada: ok
    }
}
