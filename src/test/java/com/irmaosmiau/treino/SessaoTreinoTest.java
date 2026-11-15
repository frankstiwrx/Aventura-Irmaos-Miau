package com.irmaosmiau.treino;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.EstadoCorporalObservado;
import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.Pegada1RegressaoArraysTest;
import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.personagens.CatalogoPersonagens;

import org.junit.jupiter.api.Test;

class SessaoTreinoTest {

    private static SessaoTreino nova() {
        return new SessaoTreino(ConfiguracaoTreino.padrao(), CatalogoPersonagens.padrao(),
                CarregadorConteudoCombate.carregarParaJogo());
    }

    /** Um tick como no GamePanel: jogador anda (ou nao), parceiro passivo, motor. */
    private static void tick(SessaoTreino s, double deslocamentoJogador) {
        IrmaoMiau j = s.getJogador();
        if (!j.isBloqueadoPorAnimacaoCombate() && deslocamentoJogador != 0) {
            j.moverX(deslocamentoJogador);
            j.atualizar(true, false);
        } else {
            j.atualizar(false, false);
        }
        s.atualizarParceiro();
        s.getSistema().atualizar();
    }

    private static void aproximar(SessaoTreino s) {
        int guarda = 0;
        while (!s.getSistema().podeTentarAcao(s.getJogador(), s.getParceiro(), "pegada_1")) {
            tick(s, s.getJogador().getVelocidadeBase());
            assertTrue(++guarda < 200, "nunca entrou no alcance");
        }
    }

    // =====================================================
    // INICIALIZACAO
    // =====================================================
    @Test
    void treinoPadraoBrancoJogadorPretoParceiroNoDojo() {
        SessaoTreino s = nova();
        ConfiguracaoTreino c = s.getConfiguracao();

        assertEquals("dojo", c.getIdCenario());
        assertInstanceOf(IrmaoMiauBranco.class, s.getJogador());
        assertInstanceOf(IrmaoMiauPreto.class, s.getParceiro());
        assertEquals(ConfiguracaoTreino.X_JOGADOR_PADRAO, s.getJogador().getX());
        assertEquals(ConfiguracaoTreino.X_PARCEIRO_PADRAO, s.getParceiro().getX());
        assertTrue(s.getJogador().isViradoParaDireita());
        assertFalse(s.getParceiro().isViradoParaDireita());
        assertFalse(s.getSistema().podeTentarAcao(s.getJogador(), s.getParceiro(), "pegada_1"),
                "comeca fora de alcance: o jogador precisa se aproximar");
        assertEquals("/conteudo/miaustudio/PEGADA_1.json",
                s.getSistema().getRegistroAnimacoes().getOrigem("pegada_1_entrada"));
    }

    @Test
    void idsInexistentesFalhamComMensagemClara() {
        ConfiguracaoTreino ruim = new ConfiguracaoTreino("gato_fantasma", CatalogoPersonagens.ID_PRETO,
                "dojo", 100, 200, ComportamentoParceiro.PASSIVO);
        String msg = assertThrows(IllegalArgumentException.class,
                () -> new SessaoTreino(ruim, CatalogoPersonagens.padrao(),
                        CarregadorConteudoCombate.carregarParaJogo())).getMessage();
        assertTrue(msg.contains("gato_fantasma"), msg);

        assertThrows(IllegalArgumentException.class, () -> new ConfiguracaoTreino(
                "a", "b", "dojo", 100, 100, ComportamentoParceiro.PASSIVO));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracaoTreino(
                "a", "b", "dojo", 100, 200, null));
    }

    // =====================================================
    // PARCEIRO PASSIVO
    // =====================================================
    @Test
    void parceiroPassivoNaoSeMoveNemMudaDeEstado() {
        SessaoTreino s = nova();
        EstadoCorporalObservado antes = EstadoCorporalObservado.de(s.getParceiro());
        double folego = s.getParceiro().getFolegoAtual();

        for (int i = 0; i < 300; i++) {
            tick(s, i % 50 < 25 ? 4 : -4);   // jogador anda para frente e para tras
        }

        EstadoCorporalObservado depois = EstadoCorporalObservado.de(s.getParceiro());
        assertEquals(antes.toString(), depois.toString());
        assertTrue(s.getParceiro().getFolegoAtual() >= folego, "parceiro nao gasta folego");
        assertNull(s.getSistema().getUltimoResultado(), "parceiro nunca tenta tecnicas");
    }

    // =====================================================
    // PEGADA_1 NO TREINO
    // =====================================================
    @Test
    void pegada1FuncionaNoTreinoComRegrasEPoliticaDoMotor() {
        SessaoTreino s = nova();
        SistemaCombate sis = s.getSistema();
        aproximar(s);

        double folego = s.getJogador().getFolegoAtual();
        assertEquals(ResultadoAcao.SUCESSO, sis.tentarAcao(s.getJogador(), s.getParceiro(), "pegada_1"));
        assertEquals(folego - (3.0 * com.irmaosmiau.entities.CustosFolego.FATOR_CUSTO_PEGADAS), s.getJogador().getFolegoAtual(), 1e-9);   // 5P.1 (pedido)

        for (int f = 0; f < 30; f++) {
            assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[f / 10],
                    s.getJogador().copiarPoseCombateExterna(), "frame " + f);
            assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[f / 10],
                    s.getParceiro().copiarPoseCombateExterna(), "frame " + f);
            tick(s, 4);   // tentar andar durante a entrada: ignorado (bloqueado)
        }

        // Permanencia: parceiro passivo e jogador parado mantem a pegada.
        for (int i = 0; i < 200; i++) {
            tick(s, 0);
        }
        assertTrue(sis.isInteracaoEstabelecida());
        assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[2], s.getParceiro().copiarPoseCombateExterna());

        // Rompimento: o jogador anda (para frente, para continuar no alcance
        // e o motivo ser o movimento, nao a distancia).
        tick(s, 4);
        assertFalse(sis.estaEmInteracao());
        assertEquals(MotivoEncerramentoInteracao.MOVIMENTO, sis.getUltimoMotivoEncerramento());
        assertFalse(s.getParceiro().isPoseCombateExternaAtiva());
        assertFalse(s.getJogador().isPoseCombateExternaAtiva());
    }

    // =====================================================
    // REINICIO
    // =====================================================
    @Test
    void reiniciarVoltaAsPosicoesIniciaisSemPegadaEComFolegoCheio() {
        SessaoTreino s = nova();
        aproximar(s);
        s.getSistema().tentarAcao(s.getJogador(), s.getParceiro(), "pegada_1");
        for (int i = 0; i < 10; i++) {
            tick(s, 0);
        }
        IrmaoMiau jogadorAntigo = s.getJogador();
        SistemaCombate sistemaAntigo = s.getSistema();

        s.reiniciar();

        assertEquals(1, s.getReinicios());
        assertNotSame(jogadorAntigo, s.getJogador());
        assertNotSame(sistemaAntigo, s.getSistema());
        assertEquals(ConfiguracaoTreino.X_JOGADOR_PADRAO, s.getJogador().getX());
        assertEquals(ConfiguracaoTreino.X_PARCEIRO_PADRAO, s.getParceiro().getX());
        assertEquals(s.getJogador().getAtributos().getFolego(), s.getJogador().getFolegoAtual());
        assertFalse(s.getSistema().estaEmInteracao());
        assertFalse(s.getJogador().isPoseCombateExternaAtiva());
        assertFalse(s.getParceiro().isBloqueadoPorAnimacaoCombate());
        // A sessao antiga foi encerrada (nada fica "agarrado" em memoria).
        assertFalse(sistemaAntigo.estaEmInteracao());
        assertFalse(jogadorAntigo.isPoseCombateExternaAtiva());

        // E da para treinar de novo.
        aproximar(s);
        assertEquals(ResultadoAcao.SUCESSO,
                s.getSistema().tentarAcao(s.getJogador(), s.getParceiro(), "pegada_1"));
    }
}
