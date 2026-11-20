package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.CamadaComando;
import com.irmaosmiau.controles.CelulaContextual;
import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.NaturezaAcao;
import com.irmaosmiau.controles.PosicaoComando;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.ClassificacaoAcoes;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.ui.MenuPrincipalPanel;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.6: Modo Desenvolvedor dentro da partida normal (F8 liga e desliga, sem
 * opcao no menu) e cores semanticas nas rodas de comando.
 */
class Dev5Q6GamePanelTest extends Base5OGamePanel {

    private void f8(boolean pressionada) {
        painel.getActionMap().get(pressionada ? "alternarCatalogoLab" : "soltouF8").actionPerformed(null);
    }

    @Test
    void f8LigaEDesligaNaPartidaNormalERestauraAsRegras() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            painel.configurarPartida("dojo", 90);
            assertNull(painel.getModoDesenvolvedor(), "o jogo comeca no modo normal");
            ticks(130);
            int restantes = painel.getSegundosRestantes();
            assertTrue(restantes < 90 && restantes > 0, "o relogio corre: " + restantes);
            f8(true);
            f8(true);                                            // segurar (auto-repeat) nao alterna de novo
            f8(true);
            assertNotNull(painel.getModoDesenvolvedor(), "F8 ligou");
            assertEquals(-1, painel.getSegundosRestantes(), "sem limite de tempo enquanto ligado");
            double f = j1().getFolegoAtual();
            j1().gastarFolegoEsforco(5, "teste");
            assertEquals(f, j1().getFolegoAtual(), 1e-9, "folego infinito");
            ticks(200);
            f8(false);
            f8(true);
            assertNull(painel.getModoDesenvolvedor(), "F8 de novo desligou");
            assertEquals(restantes, painel.getSegundosRestantes(), "o relogio continua de onde parou");
            double g = j1().getFolegoAtual();
            j1().gastarFolegoEsforco(5, "teste");
            assertTrue(j1().getFolegoAtual() < g, "regras normais: gasta folego");
            f8(false);
        });
    }

    @Test
    void rodasDoAshiTemONomeDaArticulacao() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            painel.getMotorSolo().forcarPosicao(Posicao.ASHI_GARAMI, Lado.B, Lado.A);
            ticks(40);
            assertTrue(painel.quadroSolo(com.irmaosmiau.controles.SlotJogador.JOGADOR_1,
                    com.irmaosmiau.posicional.TecnicaSolo.Familia.L2).nomeCamada().contains("PÉ/TORNOZELO"));
            assertTrue(painel.quadroSolo(com.irmaosmiau.controles.SlotJogador.JOGADOR_1,
                    com.irmaosmiau.posicional.TecnicaSolo.Familia.L1).nomeCamada().contains("JOELHO/QUADRIL"));
        });
    }

    @Test
    void menuPrincipalSemAOpcaoExclusiva() {
        assertFalse(MenuPrincipalPanel.OPCOES.stream().anyMatch(o -> o.contains("DESENVOLVEDOR")));
        assertEquals("F8", GamePanel.TECLA_MODO_DESENVOLVEDOR);
    }

    @Test
    void coresPelaNaturezaDaAcao() {
        CatalogoTecnicasSolo c = CatalogoTecnicasSolo.referencia();
        assertEquals(NaturezaAcao.DEFESA, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.ENQUADRAR)));
        assertEquals(NaturezaAcao.DEFESA, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.ESCAPE_QUADRIL)));
        assertEquals(NaturezaAcao.TRANSICAO, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.MONTAR)));
        assertEquals(NaturezaAcao.TRANSICAO, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.BERIMBOLO)));
        assertEquals(NaturezaAcao.TRANSICAO, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.PASSAR_GUARDA)));
        assertEquals(NaturezaAcao.QUEDA, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.SINGLE_LEG)));
        assertEquals(NaturezaAcao.QUEDA, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.RASTEIRA)));
        assertEquals(NaturezaAcao.CONTROLE, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.CONSOLIDAR)));
        for (com.irmaosmiau.posicional.TecnicaSolo t : c.todas()) {
            assertNotNull(ClassificacaoAcoes.de(t), "toda tecnica nova recebe uma natureza: " + t.id());
        }
        assertEquals(PainelDpadContextual.FINALIZACAO, PainelDpadContextual.corDaNatureza(NaturezaAcao.FINALIZACAO));
        assertEquals(PainelDpadContextual.DEFESA, PainelDpadContextual.corDaNatureza(NaturezaAcao.DEFESA));
        assertEquals(PainelDpadContextual.TRANSICAO, PainelDpadContextual.corDaNatureza(NaturezaAcao.TRANSICAO));
    }

    @Test
    void rodasDoJogoUsamAsCoresEBloqueadaFicaApagada() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            painel.getMotorSolo().forcarPosicao(Posicao.COSTAS, Lado.A, Lado.A);
            ticks(40);
            painel.getMotorSolo().definirEstabilidade(80);
            painel.getMotorSolo().comando(Lado.A, new com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo(
                    com.irmaosmiau.posicional.TecnicaSolo.Direcao.CIMA,
                    com.irmaosmiau.posicional.TecnicaSolo.Familia.L2));
            PainelDpadContextual.Quadro q = painel.montarQuadroDpad(com.irmaosmiau.controles.SlotJogador.JOGADOR_2);
            assertEquals(NaturezaAcao.DESISTENCIA, q.celulas().get(DirecaoTecnica.ESQUERDA).natureza(), "bater");
            assertEquals(NaturezaAcao.DEFESA, q.celulas().get(DirecaoTecnica.BAIXO).natureza());
        });
        CelulaContextual livre = new CelulaContextual(new PosicaoComando(CamadaComando.UM, DirecaoTecnica.CIMA),
                CelulaContextual.Tipo.EXECUTAVEL, "X", null, null, true, NaturezaAcao.FINALIZACAO);
        CelulaContextual presa = new CelulaContextual(new PosicaoComando(CamadaComando.UM, DirecaoTecnica.CIMA),
                CelulaContextual.Tipo.EXECUTAVEL, "X·Mr", null, null, false, NaturezaAcao.FINALIZACAO);
        PainelDpadContextual.Quadro a = new PainelDpadContextual.Quadro("J1", CamadaComando.UM, "t",
                Map.of(DirecaoTecnica.CIMA, livre));
        PainelDpadContextual.Quadro b = new PainelDpadContextual.Quadro("J1", CamadaComando.UM, "t",
                Map.of(DirecaoTecnica.CIMA, presa));
        assertTrue(PainelDpadContextual.texto(b, DirecaoTecnica.CIMA).contains("·Mr"), "a faixa exigida continua");
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(200, 140,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        PainelDpadContextual.desenhar(g, 0, a);
        PainelDpadContextual.desenhar(g, 0, b);
        g.dispose();
    }
}
