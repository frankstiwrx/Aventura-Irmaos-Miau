package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.SimuladorLuta.Comando;
import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5A: desequilibrio e rompimento tecnico durante a PEGADA_1 real
 * (conteudo JSON, PoliticaInteracao, ordem de tick do GamePanel).
 */
class AcoesTecnicasTest {

    private static final EfeitosTemporarios.Tipo DESEQ = EfeitosTemporarios.Tipo.DESEQUILIBRIO;

    private static void assertPoseFinal(SimuladorLuta s) {
        assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[2], s.branco.copiarPoseCombateExterna());
        assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[2], s.preto.copiarPoseCombateExterna());
    }

    /** Simulador com lutadores escolhidos (ex.: perfis) nas posicoes do SimuladorLuta. */
    private static SimuladorLuta com(AtributosLutador branco, AtributosLutador preto) {
        SimuladorLuta s = new SimuladorLuta();
        IrmaoMiau b = new IrmaoMiauBranco(SimuladorLuta.X_BRANCO, 150, 0.7, branco, "B");
        IrmaoMiau p = new IrmaoMiauPreto(SimuladorLuta.X_PRETO, 150, 0.7, preto, "P");
        return new SimuladorLuta(b, p);
    }

    // =====================================================
    // QUANDO PODE AGIR
    // =====================================================
    @Test
    void semPegadaOuDuranteAEntradaNadaAcontece() {
        SimuladorLuta s = new SimuladorLuta();
        double fb = s.branco.getFolegoAtual();
        double fp = s.preto.getFolegoAtual();

        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.tentarDesequilibrio(s.branco));
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.tentarRompimentoTecnico(s.preto));

        s.sistema.tentarAcao(s.branco, s.preto, "pegada_1");
        fb = s.branco.getFolegoAtual();
        for (int f = 0; f < 29; f++) {
            assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.tentarDesequilibrio(s.branco), "frame " + f);
            assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.tentarRompimentoTecnico(s.preto), "frame " + f);
            s.tick(Comando.PARADO, Comando.PARADO);
        }
        assertFalse(s.preto.getEfeitos().temAlgumAtivo());
        assertTrue(s.branco.getFolegoAtual() >= fb, "nada cobrado durante a entrada");
        assertTrue(s.preto.getFolegoAtual() >= fp);
        assertNull(s.sistema.getUltimaDisputa());
    }

    @Test
    void papelErradoNaoAge() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();

        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.tentarDesequilibrio(s.preto), "receptor nao desequilibra");
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.sistema.tentarRompimentoTecnico(s.branco), "aplicador nao rompe");
        IrmaoMiau intruso = new IrmaoMiauPreto(0, 150, 0.7);
        assertThrows(IllegalArgumentException.class, () -> s.sistema.tentarDesequilibrio(intruso));
    }

    // =====================================================
    // DESEQUILIBRIO
    // =====================================================
    @Test
    void desequilibrioComSucessoAplicaEfeitoCobraFolegoEMantemAPegada() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        double fb = s.branco.getFolegoAtual();
        double fp = s.preto.getFolegoAtual();
        double velocidadePreto = s.preto.getVelocidadeBase();

        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.sistema.tentarDesequilibrio(s.branco),
                String.valueOf(s.sistema.getUltimaDisputa()));

        assertEquals(fb - SistemaCombate.CUSTO_DESEQUILIBRIO, s.branco.getFolegoAtual(), 1e-9);
        assertEquals(fp, s.preto.getFolegoAtual(), 1e-9, "quem perde a disputa nao paga resistencia");
        assertEquals(SistemaCombate.DURACAO_DESEQUILIBRIO_TICKS, s.preto.getEfeitos().getTicksRestantes(DESEQ));
        assertEquals(velocidadePreto * 0.6, s.preto.getVelocidadeBase(), 1e-12);
        assertTrue(s.sistema.isInteracaoEstabelecida(), "pegada continua");
        assertFalse(s.sistema.isAnimacaoCombateAtiva(), "a entrada nao reinicia");
        assertPoseFinal(s);
    }

    @Test
    void efeitoContinuaDepoisDoRompimentoEExpira() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        s.sistema.tentarDesequilibrio(s.branco);

        // Preto anda: a regra antiga rompe a pegada por MOVIMENTO (preservada).
        int x0 = s.preto.getX();
        s.tick(Comando.PARADO, Comando.andarDireita());
        assertEquals(MotivoEncerramentoInteracao.MOVIMENTO, s.sistema.getUltimoMotivoEncerramento());
        assertTrue(s.preto.getEfeitos().isAtivo(DESEQ), "o desequilibrio continua apos o rompimento");

        // Anda mais devagar enquanto desequilibrado...
        for (int i = 0; i < 10; i++) {
            s.tick(Comando.PARADO, Comando.andarDireita());
        }
        double lento = (s.preto.getX() - x0) / 11.0;
        assertEquals(new IrmaoMiauPreto(0, 150, 0.7).getVelocidadeBase() * 0.6, lento, 0.2);

        // ...e volta ao normal ao expirar.
        s.ticks(SistemaCombate.DURACAO_DESEQUILIBRIO_TICKS);
        assertFalse(s.preto.getEfeitos().isAtivo(DESEQ));
        assertEquals(new IrmaoMiauPreto(0, 150, 0.7).getVelocidadeBase(), s.preto.getVelocidadeBase(), 1e-12);
    }

    @Test
    void defesaAltaResisteSemCustoParaQuemResiste() {
        AtributosLutador muralha = new AtributosLutador(25, 40, 60, 45, 95, 95, 38, 110);
        SimuladorLuta s = com(IrmaoMiauBranco.atributosOficiais(), muralha);
        s.estabelecerPegada1();
        double fb = s.branco.getFolegoAtual();
        double fp = s.preto.getFolegoAtual();

        assertEquals(ResultadoAcaoTecnica.RESISTIDO, s.sistema.tentarDesequilibrio(s.branco));

        assertEquals(fb - SistemaCombate.CUSTO_DESEQUILIBRIO, s.branco.getFolegoAtual(), 1e-9);
        // 5B: resistencia passiva nao custa folego.
        assertEquals(fp, s.preto.getFolegoAtual(), 1e-9);
        assertFalse(s.preto.getEfeitos().isAtivo(DESEQ));
        assertTrue(s.sistema.isInteracaoEstabelecida());
        assertPoseFinal(s);
    }

    @Test
    void atributosDiferentesMudamOResultado() {
        AtributosLutador fraco = new AtributosLutador(35, 25, 5, 22, 25, 5, 22, 45);
        AtributosLutador tecnico = new AtributosLutador(35, 25, 60, 22, 25, 95, 22, 72);

        SimuladorLuta s1 = com(fraco, IrmaoMiauPreto.atributosOficiais());
        s1.estabelecerPegada1();
        assertEquals(ResultadoAcaoTecnica.RESISTIDO, s1.sistema.tentarDesequilibrio(s1.branco));

        SimuladorLuta s2 = com(tecnico, IrmaoMiauPreto.atributosOficiais());
        s2.estabelecerPegada1();
        assertEquals(ResultadoAcaoTecnica.SUCESSO, s2.sistema.tentarDesequilibrio(s2.branco));
    }

    // =====================================================
    // FOLEGO E RECARGA
    // =====================================================
    @Test
    void semFolegoNaoCobraNemDisputa() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        s.branco.consumirFolegoCombate(s.branco.getFolegoAtual() - 3.0);

        assertEquals(ResultadoAcaoTecnica.SEM_FOLEGO, s.sistema.tentarDesequilibrio(s.branco));
        assertEquals(3.0, s.branco.getFolegoAtual(), 1e-9);
        assertNull(s.sistema.getUltimaDisputa());
        assertEquals(0, s.sistema.getRecargaRestante(s.branco), "sem fôlego nao inicia recarga");
    }

    @Test
    void recargaImpedeSpam() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();

        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.sistema.tentarDesequilibrio(s.branco));
        double depois = s.branco.getFolegoAtual();
        assertEquals(ResultadoAcaoTecnica.EM_RECARGA, s.sistema.tentarDesequilibrio(s.branco));
        assertEquals(depois, s.branco.getFolegoAtual(), 1e-9, "tentativa em recarga nao cobra");

        s.ticks(SistemaCombate.RECARGA_ACAO_TECNICA_TICKS - 1);
        assertEquals(ResultadoAcaoTecnica.EM_RECARGA, s.sistema.tentarDesequilibrio(s.branco));
        s.ticks(1);
        assertTrue(s.sistema.tentarDesequilibrio(s.branco) != ResultadoAcaoTecnica.EM_RECARGA);
    }

    // =====================================================
    // ROMPIMENTO TECNICO
    // =====================================================
    @Test
    void rompimentoTecnicoFalhaEPegadaContinua() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        double fb = s.branco.getFolegoAtual();
        double fp = s.preto.getFolegoAtual();
        int xp = s.preto.getX();

        assertEquals(ResultadoAcaoTecnica.RESISTIDO, s.sistema.tentarRompimentoTecnico(s.preto));

        assertEquals(fp - SistemaCombate.CUSTO_ROMPIMENTO_TECNICO, s.preto.getFolegoAtual(), 1e-9);
        assertEquals(fb, s.branco.getFolegoAtual(), 1e-9, "5B: aplicador resiste sem custo");
        assertTrue(s.sistema.isInteracaoEstabelecida());
        assertEquals(xp, s.preto.getX(), "sem deslocamento");
        assertPoseFinal(s);
        s.ticks(30);
        assertTrue(s.sistema.isInteracaoEstabelecida(), "a pegada segue normal depois");
    }

    @Test
    void rompimentoTecnicoComSucessoDesfazAPegadaPeloMecanismoExistente() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        s.branco.consumirFolegoCombate(s.branco.getFolegoAtual());   // Branco exausto
        double fp = s.preto.getFolegoAtual();

        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.sistema.tentarRompimentoTecnico(s.preto),
                String.valueOf(s.sistema.getUltimaDisputa()));

        assertEquals(fp - SistemaCombate.CUSTO_ROMPIMENTO_TECNICO, s.preto.getFolegoAtual(), 1e-9);
        assertEquals(EstadoInteracaoCombate.LIVRE, s.sistema.getEstadoInteracao());
        assertEquals(MotivoEncerramentoInteracao.ROMPIMENTO_TECNICO, s.sistema.getUltimoMotivoEncerramento());
        assertFalse(s.branco.isPoseCombateExternaAtiva());
        assertFalse(s.preto.isPoseCombateExternaAtiva());
        assertFalse(s.preto.isBloqueadoPorAnimacaoCombate());
        assertNull(s.sistema.getAplicadorInteracao());
        assertNull(s.sistema.getReceptorInteracao());

        // Depois do rompimento, a PEGADA_1 pode ser feita de novo quando o
        // Branco recuperar o folego (recuperacao 22 e lenta).
        int espera = 0;
        while (s.branco.getFolegoAtual() < 3.0 && espera++ < 5000) {
            s.ticks(1);
        }
        assertEquals(ResultadoAcao.SUCESSO, s.sistema.tentarAcao(s.branco, s.preto, "pegada_1"));
    }

    @Test
    void resistirNuncaDeixaOFolegoNegativo() {
        SimuladorLuta s = new SimuladorLuta();
        s.estabelecerPegada1();
        s.branco.consumirFolegoCombate(s.branco.getFolegoAtual() - 0.4);
        AtributosLutador antes = s.branco.getAtributos();

        // Com 0,4 de folego o Branco (fator 0,6) ainda pode resistir ou nao; em todo caso, >= 0.
        s.sistema.tentarRompimentoTecnico(s.preto);
        assertTrue(s.branco.getFolegoAtual() >= 0.0);
        assertTrue(s.preto.getFolegoAtual() >= 0.0);
        assertEquals(antes.getFolego(), s.branco.getAtributos().getFolego(), "maximo intacto");
    }

    // =====================================================
    // PAPEIS, NAO NOMES
    // =====================================================
    @Test
    void funcionaPeloPapelMesmoComOPretoComoAplicador() {
        IrmaoMiau b = new IrmaoMiauBranco(500, 150, 0.7);
        IrmaoMiau p = new IrmaoMiauPreto(440, 150, 0.7);
        b.setViradoParaDireita(false);
        p.setViradoParaDireita(true);
        SistemaCombate s = CarregadorConteudoCombate.carregarParaJogo().criarSistema(b, p);

        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(p, b, "pegada_1"));
        for (int i = 0; i < 30; i++) {
            s.atualizar();
        }
        assertTrue(s.isInteracaoEstabelecida());
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.tentarDesequilibrio(b), "Branco aqui e receptor");
        assertTrue(s.tentarDesequilibrio(p) != ResultadoAcaoTecnica.SEM_PEGADA, "Preto aplicador pode tentar");
        s.atualizar();
        assertTrue(s.tentarRompimentoTecnico(b) != ResultadoAcaoTecnica.SEM_PEGADA, "Branco receptor pode romper");
    }
}
