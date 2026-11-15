package com.irmaosmiau.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Entrega 5K (testes 1-8, 12, 17-21, 26-28): ciclo de posturas, custos e folego no proprio lutador. */
class PosturasEFolego5KTest {

    private static IrmaoMiau branco() {
        return new IrmaoMiauBranco(300, 150, 0.7);
    }

    private static IrmaoMiau preto() {
        return new IrmaoMiauPreto(300, 150, 0.7);
    }

    private static void parado(IrmaoMiau l, int ticks) {
        for (int i = 0; i < ticks; i++) {
            l.atualizar(false, false);
        }
    }

    // ---------------- 1 e 2. ciclo simples e direcao
    @Test
    void cicloSimplesCompletoComDirecaoExplicita() {
        IrmaoMiau l = branco();
        List<EstadoCorporal> visto = new ArrayList<>();
        List<TransicaoPostura> feitas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            TransicaoPostura t = l.proximaTransicaoSimples();
            assertEquals(TransicaoPostura.Resultado.FEITA, l.executarTransicao(t));
            feitas.add(t);
            visto.add(l.getEstadoCorporal());
            parado(l, 20);
        }
        assertEquals(List.of(EstadoCorporal.AJOELHADO, EstadoCorporal.DEITADO, EstadoCorporal.AJOELHADO,
                EstadoCorporal.EM_PE), visto);
        assertEquals(List.of(TransicaoPostura.AJOELHAR, TransicaoPostura.DEITAR, TransicaoPostura.ERGUER_JOELHOS,
                TransicaoPostura.LEVANTAR), feitas, "de joelhos: desce vindo de em pe, sobe vindo de deitado");
    }

    @Test
    void inicioDeJoelhosDesceNoProximoClique() {
        IrmaoMiau l = branco();
        l.ajoelhar();
        assertFalse(l.isCicloSubindo());
        assertEquals(TransicaoPostura.DEITAR, l.proximaTransicaoSimples());
    }

    // ---------------- 3, 4, 5, 6 e 7. duplo clique
    @Test
    void duploCliqueTransicoesDiretasComUmCustoSo() {
        IrmaoMiau l = branco();
        double f = l.getFolegoAtual();
        assertEquals(TransicaoPostura.RAPIDA_DEITAR, l.proximaTransicaoRapida());
        assertEquals(TransicaoPostura.Resultado.FEITA, l.executarTransicao(l.proximaTransicaoRapida()));
        assertTrue(l.isDeitado(), "em pe -> deitado direto, sem passar por joelhos");
        assertEquals(f - CustosFolego.RAPIDA_DEITAR, l.getFolegoAtual(), 1e-9, "um custo unico");
        parado(l, 20);

        f = l.getFolegoAtual();
        assertEquals(TransicaoPostura.RAPIDA_LEVANTAR_DO_CHAO, l.proximaTransicaoRapida());
        l.executarTransicao(l.proximaTransicaoRapida());
        assertEquals(EstadoCorporal.EM_PE, l.getEstadoCorporal(), "deitado -> em pe direto");
        assertEquals(f - CustosFolego.RAPIDA_LEVANTAR_DO_CHAO, l.getFolegoAtual(), 1e-9);

        l.ajoelhar();
        f = l.getFolegoAtual();
        assertEquals(TransicaoPostura.RAPIDA_LEVANTAR, l.proximaTransicaoRapida());
        l.executarTransicao(l.proximaTransicaoRapida());
        assertEquals(EstadoCorporal.EM_PE, l.getEstadoCorporal());
        assertEquals(f - CustosFolego.RAPIDA_LEVANTAR, l.getFolegoAtual(), 1e-9);

        // De joelhos vindo de deitado (subindo) o duplo tambem levanta.
        IrmaoMiau m = branco();
        m.executarTransicao(TransicaoPostura.RAPIDA_DEITAR);
        m.executarTransicao(m.proximaTransicaoSimples());
        assertTrue(m.isAjoelhado() && m.isCicloSubindo());
        assertEquals(TransicaoPostura.RAPIDA_LEVANTAR, m.proximaTransicaoRapida());
    }

    @Test
    void rapidaSempreMaisCaraQueOCaminhoLento() {
        assertTrue(CustosFolego.RAPIDA_DEITAR > CustosFolego.AJOELHAR + CustosFolego.DEITAR);
        assertTrue(CustosFolego.RAPIDA_LEVANTAR_DO_CHAO > CustosFolego.ERGUER_JOELHOS + CustosFolego.LEVANTAR);
        assertTrue(CustosFolego.RAPIDA_LEVANTAR > CustosFolego.LEVANTAR);
        for (TransicaoPostura t : TransicaoPostura.values()) {
            assertTrue(t.getCusto() > 0, t.name());
            assertTrue(t.getContrato().startsWith("TRANSICAO_"), t.name());
        }
    }

    // ---------------- 12. sem folego: nada muda
    @Test
    void semFolegoATransicaoFalhaInteira() {
        IrmaoMiau l = branco();
        while (l.getFolegoAtual() >= 1.0) {
            l.consumirFolegoCombate(1.0);
        }
        double f = l.getFolegoAtual();
        assertEquals(TransicaoPostura.Resultado.SEM_FOLEGO, l.executarTransicao(l.proximaTransicaoRapida()));
        assertEquals(EstadoCorporal.EM_PE, l.getEstadoCorporal(), "nem parte da transicao");
        assertEquals(f, l.getFolegoAtual(), 1e-12, "nada cobrado");
        assertEquals(null, l.getTransicaoAtiva());
        parado(l, 400);                                        // parado recupera: nunca fica preso
        assertEquals(TransicaoPostura.Resultado.FEITA, l.executarTransicao(l.proximaTransicaoRapida()));
    }

    @Test
    void transicaoBloqueadaNoArERolando() {
        IrmaoMiau l = branco();
        l.pular();
        assertEquals(TransicaoPostura.Resultado.BLOQUEADA, l.executarTransicao(l.proximaTransicaoSimples()));
        IrmaoMiau m = branco();
        m.iniciarRolamento(Direcao.DIREITA);
        assertEquals(TransicaoPostura.Resultado.BLOQUEADA, m.executarTransicao(m.proximaTransicaoSimples()));
        IrmaoMiau n = branco();
        n.setBloqueadoPorAnimacaoCombate(true);
        assertEquals(TransicaoPostura.Resultado.BLOQUEADA, n.executarTransicao(n.proximaTransicaoSimples()));
        assertEquals(TransicaoPostura.Resultado.BLOQUEADA, branco().executarTransicao(TransicaoPostura.LEVANTAR),
                "transicao que nao parte da postura atual");
    }

    @Test
    void animacaoDaTransicaoTemDuracaoEFimLimpo() {
        IrmaoMiau l = branco();
        l.executarTransicao(TransicaoPostura.AJOELHAR);
        assertEquals(TransicaoPostura.AJOELHAR, l.getTransicaoAtiva());
        assertTrue(l.isAjoelhado(), "a postura logica muda na hora");
        parado(l, TransicaoPostura.AJOELHAR.getDuracaoTicks());
        assertEquals(null, l.getTransicaoAtiva());
        assertEquals(1.0, l.getProgressoTransicao());
    }

    // ---------------- 17. caminhada: 10 s ~ 1 ponto
    private static double caminhar(IrmaoMiau l, int ticks, double fatorVelocidade) {
        double antes = l.getFolegoAtual();
        for (int i = 0; i < ticks; i++) {
            l.moverX(l.getVelocidadeBase() * fatorVelocidade);
            l.atualizar(true, false);
        }
        return antes - l.getFolegoAtual();
    }

    @Test
    void caminharDezSegundosGastaUmPonto() {
        int dezSegundos = (int) Math.round(10 * CustosFolego.TICKS_POR_SEGUNDO);
        assertEquals(625, dezSegundos);
        assertEquals(1.0, caminhar(branco(), dezSegundos, 1.0), 0.02, "Branco");
        assertEquals(1.0, caminhar(preto(), dezSegundos, 1.0), 0.02, "Preto (velocidade propria)");
        assertEquals(0.5, caminhar(branco(), dezSegundos, 0.5), 0.02, "metade da distancia, metade do custo");
    }

    // ---------------- 18. sem movimento real, sem custo
    @Test
    void parouOuBloqueadoNaoCobraERecupera() {
        IrmaoMiau l = branco();
        l.consumirFolegoCombate(5);
        double f = l.getFolegoAtual();
        for (int i = 0; i < 60; i++) {
            l.moverVertical(-50);                         // ja na linha de cima: nada anda
            l.atualizar(true, false);                     // "andando" apertado, sem deslocamento
        }
        assertTrue(l.getFolegoAtual() > f, "sem deslocamento real: recupera como parado");
    }

    // ---------------- 19. de joelhos e deitado
    @Test
    void caminhadaDeJoelhosGastaPoucoPorTempo() {
        IrmaoMiau l = branco();
        l.ajoelhar();
        double porTempo = 0;
        double antes = l.getFolegoAtual();
        for (int i = 0; i < 625; i++) {
            l.moverX(l.getVelocidadeAjoelhado());
            l.atualizar(true, false);
        }
        porTempo = antes - l.getFolegoAtual();
        assertEquals(1.0 * 0.18 * CustosFolego.FATOR_DISTANCIA_AJOELHADO, porTempo, 0.02,
                "10 s de joelhos: ~0,45 (menos que em pe, mais caro por distancia)");
        assertTrue(l.isAjoelhado());
    }

    // ---------------- 20 e 21. tabela de custos e alternancia sem lucro
    @Test
    void alternarPosturaRapidoNaoRecuperaFolego() {
        IrmaoMiau l = branco();
        double f = l.getFolegoAtual();
        for (int i = 0; i < 8; i++) {
            l.executarTransicao(l.proximaTransicaoSimples());
            l.atualizar(false, false);
        }
        assertTrue(l.getFolegoAtual() < f, "ciclar posturas so gasta");
        assertEquals("TRANSICAO_LEVANTAR", l.getUltimoCustoMotivo());
    }

    // ---------------- 26, 27 e 28
    @Test
    void folegoNuncaNegativoERecuperacaoLimitadaAoMaximoProprio() {
        IrmaoMiau b = branco();
        IrmaoMiau p = preto();
        assertNotEquals(b.getAtributos().getFolego(), p.getAtributos().getFolego(), "maximos diferentes por irmao");
        b.gastarFolegoEsforco(1000, "teste");
        assertEquals(0.0, b.getFolegoAtual(), "nunca negativo");
        double r1 = b.getFolegoAtual();
        parado(b, 60);
        double emUmSegundo = b.getFolegoAtual() - r1;
        assertTrue(emUmSegundo > 0.3 && emUmSegundo < 3.0, "recuperacao gradual: " + emUmSegundo + "/s");
        parado(b, 20_000);
        assertEquals(b.getAtributos().getFolego(), b.getFolegoAtual(), 1e-9, "para no maximo do Branco");
        parado(p, 20_000);
        assertEquals(p.getAtributos().getFolego(), p.getFolegoAtual(), 1e-9, "e no do Preto");
    }

    @Test
    void esforcoDoArrastoSuspendeARecuperacaoNoTick() {
        IrmaoMiau l = branco();
        l.consumirFolegoCombate(5);
        double f = l.getFolegoAtual();
        l.gastarFolegoEsforco(0.0025, "arrasto");
        l.atualizar(false, false);
        assertEquals(f - 0.0025, l.getFolegoAtual(), 1e-12, "sem recuperar no tick do esforco");
        l.atualizar(false, false);
        assertTrue(l.getFolegoAtual() > f - 0.0025, "no tick seguinte, parado, volta a recuperar");
    }
}
