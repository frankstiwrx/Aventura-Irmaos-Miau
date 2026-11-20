package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.RegrasVelocidade;
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.5 no GamePanel real: arrasto de joelhos mais lento, X acelerando de
 * joelhos (sem levantar, gastando folego), ordem de desenho pela posicao (nao por
 * J1/J2) e rodas vazias escondidas.
 */
class Movimento5Q5GamePanelTest extends Base5OGamePanel {

    private int arrastar(int n) {
        int x0 = j1().getX();
        for (int i = 0; i < n; i++) {
            s().deslocarPegadaControlada(j1(), -1, 0, 0, 800);
            ticks(1);
        }
        return Math.abs(j1().getX() - x0);
    }

    @Test
    void arrastoDeJoelhosEBemMaisLentoQueEmPeEQueAndarDeJoelhos() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            assertTrue(s().isInteracaoEstabelecida());
            int emPe = arrastar(60);

            novo(InicioLuta.DE_JOELHOS);
            j1Pega(1);
            assertTrue(s().isInteracaoEstabelecida(), "de joelhos a pegada vale (5Q.4)");
            assertTrue(j1().isAjoelhado());
            int joelhos = arrastar(60);
            assertTrue(joelhos > 0, "ainda anda");
            assertTrue(joelhos < emPe * 0.6, "arrasto de joelhos " + joelhos + " x em pe " + emPe);
            assertTrue(joelhos / 60.0 < j1().getVelocidadeAjoelhado(), "mais lento que andar de joelhos sozinho");
        });
    }

    private int andarDeJoelhos(boolean x) {
        novo(InicioLuta.DE_JOELHOS);
        assertTrue(j1().isAjoelhado());
        int x0 = j1().getX();
        if (x) {
            tecla("brancoCorrer", true);
        }
        tecla("brancoEsquerda", true);
        ticks(40);
        assertEquals(x, j1().isDeslocamentoAcelerado(), "acelerado so com o X");
        tecla("brancoEsquerda", false);
        tecla("brancoCorrer", false);
        assertTrue(j1().isAjoelhado(), "continua de joelhos: nao e corrida em pe");
        return Math.abs(j1().getX() - x0);
    }

    @Test
    void xAceleraDeJoelhosSemLevantarEGastaFolego() throws Exception {
        naEdt(() -> {
            int normal = andarDeJoelhos(false);
            double f0 = j1().getFolegoAtual();
            int rapido = andarDeJoelhos(true);
            assertTrue(rapido >= normal * 1.6, "acelerado " + rapido + " x normal " + normal);
            assertTrue(j1().getFolegoAtual() < f0, "acelerar gasta folego");
        });
    }

    @Test
    void deitadoAceleradoUsaImpulsosEVelocidadeDosAtributos() {
        IrmaoMiau b = new com.irmaosmiau.entities.IrmaoMiauBranco(300, 150, 0.7);
        IrmaoMiau p = new com.irmaosmiau.entities.IrmaoMiauPreto(400, 150, 0.7);
        b.alternarDeitado();
        p.alternarDeitado();
        for (int i = 0; i < 30; i++) {
            b.atualizar(false, false);
            p.atualizar(false, false);
        }
        assertTrue(b.isDeitado());
        b.setDeslocamentoAcelerado(true);
        assertTrue(b.isDeslocamentoAceleradoPedido());
        assertEquals(b.getVelocidadeRastejando() * RegrasVelocidade.FATOR_ACELERADO_DEITADO,
                b.getVelocidadeChaoAcelerada(), 1e-9);
        assertTrue(b.getVelocidadeChaoAcelerada() != p.getVelocidadeChaoAcelerada()
                || b.getAtributos().getVelocidade() == p.getAtributos().getVelocidade(),
                "a diferenca entre os irmaos continua");
        double f = b.getFolegoAtual();
        b.cobrarDeslocamentoAcelerado();
        assertTrue(b.getFolegoAtual() < f);
        b.setRecursosInfinitos(true, true);                   // Modo Desenvolvedor
        double f2 = b.getFolegoAtual();
        b.cobrarDeslocamentoAcelerado();
        assertEquals(f2, b.getFolegoAtual(), 1e-9, "respeita o folego infinito");
    }

    private void ate(Posicao p, Lado cima, Lado ctrl) {
        painel.getMotorSolo().forcarPosicao(p, cima, ctrl);
        ticks(60);
    }

    @Test
    void ordemDeDesenhoPelaPosicaoNaoPorJ1OuJ2() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            MotorSolo m = painel.getMotorSolo();
            ate(Posicao.MONTADA, Lado.A, Lado.A);
            assertEquals(j1().getLinhaAtual(), j2().getLinhaAtual());
            assertEquals(j1(), painel.ordemDeDesenho().get(1), "montada: quem esta por cima na frente");
            ate(Posicao.MONTADA, Lado.B, Lado.B);
            assertEquals(j2(), painel.ordemDeDesenho().get(1), "o mesmo com o J2 por cima");
            ate(Posicao.GUARDA, Lado.A, Lado.B);
            assertEquals(j2(), painel.ordemDeDesenho().get(1), "guarda: quem esta por baixo na frente (pernas)");
            ate(Posicao.GUARDA, Lado.B, Lado.A);
            assertEquals(j1(), painel.ordemDeDesenho().get(1));
            assertTrue(m.getPosicao().isGuarda());
        });
    }

    @Test
    void rasteiraPegaQuemAcabouDeAndarMasNaoQuemEstaParadoComBase() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            assertTrue(s().isInteracaoEstabelecida());
            tecla("pretoDireita", true);                          // o J2 da um passo (puxa para tras)
            ticks(4);
            tecla("pretoDireita", false);
            ticks(2);
            painel.getMotorSolo().comando(Lado.A, new com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo(
                    com.irmaosmiau.posicional.TecnicaSolo.Direcao.CIMA, com.irmaosmiau.posicional.TecnicaSolo.Familia.L2));
            assertEquals("rasteira", painel.getMotorSolo().getTentativa().orElseThrow().tecnica().id());
            for (int i = 0; i < 120 && painel.getMotorSolo().getTentativa().isPresent(); i++) {
                ticks(1);
            }
            ticks(2);
            assertTrue(j2().isDeitado() || painel.getMotorSolo().isAterrissando()
                    || painel.getMotorSolo().getPosicao().isNoChao(), "andou ha pouco: a rasteira entra");
        });
    }

    @Test
    void rodaVaziaNaoApareceENaoMostraSemTecnica() {
        PainelDpadContextual.Quadro vazio = new PainelDpadContextual.Quadro("J1",
                com.irmaosmiau.controles.CamadaComando.QUATRO, "L1+L2", java.util.Map.of());
        assertFalse(PainelDpadContextual.deveMostrar(vazio));
        assertFalse(PainelDpadContextual.deveMostrar(null));
    }
}
