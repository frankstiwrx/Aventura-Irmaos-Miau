package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.SimuladorLuta.Comando;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import org.junit.jupiter.api.Test;

/** Entrega 5B: firmeza, desgaste, reforco, deslocamento conjunto e custos revisados. */
class FirmezaPegadaTest {

    private static final double D = SistemaCombate.DESGASTE_FIRMEZA_POR_TICK;

    private static void assertPoseFinal(SimuladorLuta s) {
        assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[2], s.branco.copiarPoseCombateExterna());
        assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[2], s.preto.copiarPoseCombateExterna());
    }

    // =====================================================
    // CUSTOS REVISADOS
    // =====================================================
    @Test
    void resistenciaPassivaNaoCustaNadaETeURespeitamSeusCustos() {
        AtributosLutador muralha = new AtributosLutador(25, 40, 60, 45, 95, 95, 38, 110);
        SimuladorLuta s = new SimuladorLuta(new IrmaoMiauBranco(SimuladorLuta.X_BRANCO, 150, 0.7),
                new IrmaoMiauPreto(SimuladorLuta.X_PRETO, 150, 0.7, muralha, "Muralha"));
        s.estabelecerPegada1();
        double fb = s.branco.getFolegoAtual();
        double fp = s.preto.getFolegoAtual();

        assertEquals(ResultadoAcaoTecnica.RESISTIDO, s.sistema.tentarDesequilibrio(s.branco));
        assertEquals(fb - 4.0, s.branco.getFolegoAtual(), 1e-9, "T custa 4");
        assertEquals(fp, s.preto.getFolegoAtual(), 1e-9, "resistir passivamente nao custa");

        // U com os oficiais: o Branco (firmeza 100) resiste sem pagar nada.
        SimuladorLuta o = new SimuladorLuta();
        o.estabelecerPegada1();
        double ob = o.branco.getFolegoAtual();
        double op = o.preto.getFolegoAtual();
        assertEquals(ResultadoAcaoTecnica.RESISTIDO, o.sistema.tentarRompimentoTecnico(o.preto));
        assertEquals(op - 3.0, o.preto.getFolegoAtual(), 1e-9, "U custa 3");
        assertEquals(ob, o.branco.getFolegoAtual(), 1e-9, "aplicador resiste sem custo");
    }

    // =====================================================
    // FIRMEZA: INICIO, ENTRADA, DESGASTE, LIMITES
    // =====================================================
    @Test
    void firmezaComecaEm100ENaoCaiDuranteAEntrada() {
        SimuladorLuta s = new SimuladorLuta();
        assertEquals(0.0, s.sistema.getFirmeza(), "sem interacao");

        s.sistema.tentarAcao(s.branco, s.preto, "pegada_1");
        for (int f = 0; f < 30; f++) {
            assertEquals(SistemaCombate.FIRMEZA_INICIAL, s.sistema.getFirmeza(), "frame " + f);
            s.tick(Comando.PARADO, Comando.PARADO);
        }
        assertTrue(s.sistema.isInteracaoEstabelecida());
        assertEquals(100.0, s.sistema.getFirmeza(), "estabeleceu sem desgaste");
        assertPoseFinal(s);
    }

    @Test
    void desgasteGradualEPrevisivelPorTick() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();

        s.ticks(1);
        assertEquals(100.0 - D, s.sistema.getFirmeza(), 1e-9);
        s.ticks(99);
        assertEquals(100.0 - 100 * D, s.sistema.getFirmeza(), 1e-6, "linear: 0,10 por tick");
        assertTrue(s.sistema.isInteracaoEstabelecida());
    }

    @Test
    void firmezaZeroEncerraComMotivoProprioELimpaTudo() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        int ticks = 0;
        while (s.sistema.estaEmInteracao() && ticks < 2000) {
            assertTrue(s.sistema.getFirmeza() >= 0 && s.sistema.getFirmeza() <= 100);
            s.ticks(1);
            ticks++;
        }
        assertEquals(1000, ticks, 1, "~1000 ticks (~16,7 s) sem reforco");
        assertEquals(MotivoEncerramentoInteracao.SEM_CONTROLE, s.sistema.getUltimoMotivoEncerramento());
        assertEquals(0.0, s.sistema.getFirmeza());
        assertFalse(s.branco.isPoseCombateExternaAtiva());
        assertFalse(s.preto.isPoseCombateExternaAtiva());
        assertNull(s.sistema.getAplicadorInteracao());
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.deslocarPegadaControlada(s.branco, 1, 0, 800));
    }

    @Test
    void encerrarNaoApagaEfeitosIndependentes() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.sistema.tentarDesequilibrio(s.branco));
        s.sistema.encerrarInteracao();
        assertTrue(s.preto.getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
        assertEquals(0.0, s.sistema.getFirmeza());
    }

    // =====================================================
    // FIRMEZA NAS DISPUTAS
    // =====================================================
    @Test
    void firmeza100ReproduzA5AEFirmezaBaixaFavoreceORompimento() {
        AtributosLutador b = IrmaoMiauBranco.atributosOficiais();
        AtributosLutador p = IrmaoMiauPreto.atributosOficiais();

        ResultadoDisputa a5 = ResolvedorDisputa.resolver(TipoDisputa.ROMPIMENTO_TECNICO, p, 40, b, 22);
        ResultadoDisputa f100 = ResolvedorDisputa.resolver(TipoDisputa.ROMPIMENTO_TECNICO, p, 40, b, 22, 100);
        assertEquals(a5.getPontuacaoDefesa(), f100.getPontuacaoDefesa(), 1e-12);
        assertEquals(a5.getPontuacaoAtaque(), f100.getPontuacaoAtaque(), 1e-12);
        assertEquals(0.283, f100.getPontuacaoAtaque(), 0.001);
        assertEquals(0.331, f100.getPontuacaoDefesa(), 0.001);
        assertFalse(f100.isSucessoDoAtaque());

        ResultadoDisputa f40 = ResolvedorDisputa.resolver(TipoDisputa.ROMPIMENTO_TECNICO, p, 40, b, 22, 40);
        assertEquals(f100.getPontuacaoDefesa() * (0.7 + 0.3 * 0.4), f40.getPontuacaoDefesa(), 1e-12);
        assertTrue(f40.isSucessoDoAtaque(), "com firmeza 40 o Preto rompe: " + f40);

        // Monotonia: mais firmeza nunca piora a defesa do aplicador.
        double anterior = -1;
        for (int f = 0; f <= 100; f += 5) {
            double d = ResolvedorDisputa.resolver(TipoDisputa.ROMPIMENTO_TECNICO, p, 40, b, 22, f).getPontuacaoDefesa();
            assertTrue(d >= anterior);
            anterior = d;
        }
        // Moderada: firmeza 0 tira no maximo 30% da defesa.
        assertEquals(0.7, ResolvedorDisputa.resolver(TipoDisputa.ROMPIMENTO_TECNICO, p, 40, b, 22, 0)
                .getPontuacaoDefesa() / f100.getPontuacaoDefesa(), 1e-12);
    }

    @Test
    void desequilibrioPerdeAVantagemDeControleComFirmezaBaixa() {
        AtributosLutador b = IrmaoMiauBranco.atributosOficiais();
        AtributosLutador p = IrmaoMiauPreto.atributosOficiais();
        ResultadoDisputa f100 = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, b, 22, p, 40, 100);
        ResultadoDisputa a5 = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, b, 22, p, 40);
        assertEquals(a5.getPontuacaoAtaque(), f100.getPontuacaoAtaque(), 1e-12, "igual a 5A com firmeza 100");
        assertTrue(f100.isSucessoDoAtaque());

        ResultadoDisputa f0 = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, b, 22, p, 40, 0);
        assertEquals(f100.getPontuacaoAtaque() / 1.10, f0.getPontuacaoAtaque(), 1e-12);
        assertFalse(f0.isSucessoDoAtaque(), "sem controle o Branco nao desequilibra o Preto: " + f0);

        // Deterministico e empate continua favorecendo o defensor.
        AtributosLutador zero = new AtributosLutador(50, 50, 0, 50, 0, 0, 50, 40);
        assertFalse(ResolvedorDisputa.resolver(TipoDisputa.ROMPIMENTO_TECNICO, zero, 50, zero, 50, 37).isSucessoDoAtaque());
    }

    @Test
    void naPartidaORompimentoFicaPossivelQuandoAFirmezaCai() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        assertEquals(ResultadoAcaoTecnica.RESISTIDO, s.sistema.tentarRompimentoTecnico(s.preto));

        // Parado, o Branco tambem recupera o folego (25/25): precisa de firmeza ~30.
        s.ticks(700);
        assertTrue(s.sistema.getFirmeza() < 35);
        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.sistema.tentarRompimentoTecnico(s.preto),
                String.valueOf(s.sistema.getUltimaDisputa()));
        assertEquals(MotivoEncerramentoInteracao.ROMPIMENTO_TECNICO, s.sistema.getUltimoMotivoEncerramento());
    }

    // =====================================================
    // REFORCO
    // =====================================================
    @Test
    void reforcoRecuperaFirmezaCobraFolegoETemRecarga() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        assertEquals(ResultadoAcaoTecnica.SEM_EFEITO, s.sistema.tentarReforcarPegada(s.branco),
                "com 100% nao cobra nada");

        s.ticks(400);                     // firmeza 60
        double f = s.sistema.getFirmeza();
        double folego = s.branco.getFolegoAtual();

        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.sistema.tentarReforcarPegada(s.branco));
        assertEquals(f + SistemaCombate.REFORCO_FIRMEZA, s.sistema.getFirmeza(), 1e-9);
        assertEquals(folego - SistemaCombate.CUSTO_REFORCO, s.branco.getFolegoAtual(), 1e-9);
        assertFalse(s.sistema.isAnimacaoCombateAtiva(), "nao reinicia a entrada");
        assertPoseFinal(s);

        assertEquals(ResultadoAcaoTecnica.EM_RECARGA, s.sistema.tentarReforcarPegada(s.branco));
        s.ticks(SistemaCombate.RECARGA_REFORCO_TICKS);
        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.sistema.tentarReforcarPegada(s.branco));
        assertTrue(s.sistema.getFirmeza() <= 100.0, "nunca passa de 100");
        assertEquals(100.0, s.sistema.getFirmeza(), 1e-9);
    }

    @Test
    void reforcoSoParaOAplicadorComPegadaEFolego() {
        SimuladorLuta s = new SimuladorLuta();
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.tentarReforcarPegada(s.branco));
        s.sistema.tentarAcao(s.branco, s.preto, "pegada_1");
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.tentarReforcarPegada(s.branco), "durante a entrada");
        s.ticks(31);
        s.ticks(100);
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.tentarReforcarPegada(s.preto), "receptor nao reforca");
        s.branco.consumirFolegoCombate(s.branco.getFolegoAtual() - 1.0);
        double f = s.sistema.getFirmeza();
        assertEquals(ResultadoAcaoTecnica.SEM_FOLEGO, s.sistema.tentarReforcarPegada(s.branco));
        assertEquals(f, s.sistema.getFirmeza(), 1e-9);
        assertEquals(1.0, s.branco.getFolegoAtual(), 1e-9);
    }

    // =====================================================
    // DESLOCAMENTO CONJUNTO
    // =====================================================
    @Test
    void deslocamentoConjuntoMantemDistanciaLinhasEPegada() {
        SimuladorLuta s = new SimuladorLuta();
        s.preto.descerLinha();               // linhas diferentes (1 de diferenca)
        s.estabelecerPegada1();
        int distancia = s.preto.getX() - s.branco.getX();
        int linhaB = s.branco.getLinhaAtual();
        int linhaP = s.preto.getLinhaAtual();
        int xb = s.branco.getX();

        for (int i = 0; i < 50; i++) {
            assertEquals(ResultadoAcaoTecnica.SUCESSO, s.sistema.deslocarPegadaControlada(s.branco, 1, 0, 800));
            s.tick(Comando.PARADO, Comando.PARADO);
            assertEquals(distancia, s.preto.getX() - s.branco.getX(), "tick " + i);
        }
        assertEquals(xb + 50, s.branco.getX(), "1 px por tick (mais lento que caminhar)");
        assertEquals(linhaB, s.branco.getLinhaAtual());
        assertEquals(linhaP, s.preto.getLinhaAtual());
        assertTrue(s.sistema.isInteracaoEstabelecida(), "nao rompe por MOVIMENTO");
        assertTrue(s.branco.isViradoParaDireita() && !s.preto.isViradoParaDireita(), "orientacao preservada");
        assertPoseFinal(s);
        // Desgaste maior enquanto desloca: 50 ticks x (0,10 + 0,15)
        assertEquals(100.0 - 50 * (D + SistemaCombate.DESGASTE_EXTRA_DESLOCAMENTO_POR_TICK),
                s.sistema.getFirmeza(), 1e-6);
    }

    @Test
    void deslocamentoRespeitaOsLimitesDaArena() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        int limite = s.preto.getX() + 3;
        for (int i = 0; i < 3; i++) {
            assertEquals(ResultadoAcaoTecnica.SUCESSO, s.sistema.deslocarPegadaControlada(s.branco, 1, 0, limite));
        }
        int xb = s.branco.getX();
        assertEquals(ResultadoAcaoTecnica.LIMITE_ARENA, s.sistema.deslocarPegadaControlada(s.branco, 1, 0, limite));
        assertEquals(xb, s.branco.getX(), "nao atravessou");
        assertEquals(limite, s.preto.getX());
    }

    @Test
    void deslocamentoSoPeloAplicadorEstabelecido() {
        SimuladorLuta s = new SimuladorLuta();
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.deslocarPegadaControlada(s.branco, 1, 0, 800));
        s.sistema.tentarAcao(s.branco, s.preto, "pegada_1");
        int xb = s.branco.getX();
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.deslocarPegadaControlada(s.branco, 1, 0, 800),
                "nada durante os 30 quadros");
        assertEquals(xb, s.branco.getX());
        s.ticks(30);
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.deslocarPegadaControlada(s.preto, -1, 0, 800));
    }

    @Test
    void movimentacaoNormalContinuaRompendo() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        s.sistema.deslocarPegadaControlada(s.branco, 1, 0, 800);
        s.tick(Comando.andarDireita(), Comando.PARADO);      // caminhada normal
        assertEquals(MotivoEncerramentoInteracao.MOVIMENTO, s.sistema.getUltimoMotivoEncerramento());

        // Deslocar junto e, no mesmo tick, o receptor pular: a mudanca do corpo ainda rompe.
        SimuladorLuta s2 = new SimuladorLuta();
        s2.estabelecerPegada1();
        s2.sistema.deslocarPegadaControlada(s2.branco, 1, 0, 800);
        s2.tick(Comando.PARADO, Comando.pular());
        assertEquals(MotivoEncerramentoInteracao.PULO, s2.sistema.getUltimoMotivoEncerramento());
    }

    @Test
    void perfisPersonalizadosUsamAFirmezaNasDisputas() {
        AtributosLutador fortao = new AtributosLutador(35, 60, 90, 22, 25, 90, 22, 90);
        IrmaoMiau b = new IrmaoMiauBranco(SimuladorLuta.X_BRANCO, 150, 0.7, fortao, "Fortao");
        SimuladorLuta s = new SimuladorLuta(b, new IrmaoMiauPreto(SimuladorLuta.X_PRETO, 150, 0.7));
        s.estabelecerPegada1();
        s.ticks(600);
        assertEquals(ResultadoAcaoTecnica.RESISTIDO, s.sistema.tentarRompimentoTecnico(s.preto),
                "perfil forte/tecnico segura mesmo com firmeza ~40: " + s.sistema.getUltimaDisputa());
    }
}
