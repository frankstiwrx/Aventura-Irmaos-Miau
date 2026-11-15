package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.EstadoVisual;
import com.irmaosmiau.combate.EventoDefesa;
import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.CustosFolego;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/** Entrega 5L-A (testes 1-10 e 24) no GamePanel real: nada de fuga gratuita; evasao antes do contato. */
class DefesaPegada5LGamePanelTest {

    private GamePanel painel;

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void tecla(String nome, boolean pressionada) {
        painel.getActionMap().get(nome + (pressionada ? "Pressionada" : "Solta")).actionPerformed(null);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private void toque(String nome) {
        tecla(nome, true);
        ticks(1);
        tecla(nome, false);
        ticks(1);
    }

    private SistemaCombate s() {
        return painel.getSistemaCombate();
    }

    private IrmaoMiau j1() {
        return painel.getLutador1();
    }

    private IrmaoMiau j2() {
        return painel.getLutador2();
    }

    private GamePanel dois(InicioLuta inicio) {
        GamePanel g = new GamePanel(TipoPersonagem.AMBOS, null, null, inicio, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    /** J1 pega o J2 (de joelhos ja estao no alcance; em pe anda ate ele). */
    private void j1Pega(InicioLuta inicio) {
        painel = dois(inicio);
        ticks(1);
        if (inicio == InicioLuta.EM_PE) {
            tecla("brancoDireita", true);
            for (int i = 0; i < 300 && !s().podeTentarAcao(j1(), j2(), "pegada_1"); i++) {
                ticks(1);
            }
            tecla("brancoDireita", false);
        }
        toque("brancoPegada1");
        ticks(40);
        assertTrue(s().isInteracaoEstabelecida());
        assertSame(j2(), s().getReceptorInteracao());
    }

    // ---------------- 1 e 2: andar e mudar de linha nao soltam o receptor
    @Test
    void receptorNaoEscapaAndandoNemMudandoDeLinha() throws Exception {
        for (InicioLuta inicio : InicioLuta.values()) {
            for (String dir : new String[] {"pretoDireita", "pretoEsquerda", "pretoCima", "pretoBaixo"}) {
                naEdt(() -> {
                    j1Pega(inicio);
                    int x = j2().getX();
                    int linha = j2().getLinhaAtual();
                    tecla(dir, true);
                    ticks(120);
                    tecla(dir, false);
                    ticks(1);
                    String caso = inicio + " " + dir;
                    assertTrue(s().isInteracaoEstabelecida(), caso + ": a pegada continua");
                    assertEquals(x, j2().getX(), caso + ": o receptor nao se afasta");
                    assertEquals(linha, j2().getLinhaAtual(), caso + ": nem muda de linha");
                });
            }
        }
    }

    @Test
    void forcarDesgastaAFirmezaECustaFolegoAoReceptor() throws Exception {
        double[] semForca = new double[1];
        naEdt(() -> {
            j1Pega(InicioLuta.DE_JOELHOS);
            double f = s().getFirmeza();
            ticks(100);
            semForca[0] = f - s().getFirmeza();
        });
        naEdt(() -> {
            j1Pega(InicioLuta.DE_JOELHOS);
            double f = s().getFirmeza();
            double folego = j2().getFolegoAtual();
            tecla("pretoEsquerda", true);
            ticks(100);
            tecla("pretoEsquerda", false);
            double perda = f - s().getFirmeza();
            assertEquals(semForca[0] + 100 * SistemaCombate.DESGASTE_RESISTENCIA_POR_TICK, perda, 1e-6,
                    "forcar gasta a firmeza mais rapido");
            assertTrue(j2().getFolegoAtual() < folego, "e custa folego ao receptor");
            assertTrue(painel.linhasTecnicas().get(8).contains("PEGADA: Gola e manga"), painel.linhasTecnicas().get(8));
        });
    }

    @Test
    void forcarSemFolegoNaoTemEfeitoENaoFicaNegativo() throws Exception {
        naEdt(() -> {
            j1Pega(InicioLuta.DE_JOELHOS);
            j2().gastarFolegoEsforco(1000, "teste");
            double f = s().getFirmeza();
            tecla("pretoEsquerda", true);
            ticks(1);
            assertEquals(0.0, j2().getFolegoAtual(), "nunca negativo");
            ticks(9);
            tecla("pretoEsquerda", false);
            assertEquals(f - 10 * SistemaCombate.DESGASTE_FIRMEZA_POR_TICK, s().getFirmeza(), 1e-6,
                    "sem folego a forca nao conta");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // ---------------- 3: o aplicador continua arrastando
    @Test
    void aplicadorContinuaArrastando() throws Exception {
        naEdt(() -> {
            j1Pega(InicioLuta.DE_JOELHOS);
            int x = j1().getX();
            tecla("brancoControle", true);
            tecla("brancoEsquerda", true);
            ticks(10);
            tecla("brancoEsquerda", false);
            tecla("brancoControle", false);
            ticks(1);
            assertTrue(j1().getX() < x);
            assertEquals(j2().getX() - j1().getX(), InicioLuta.DISTANCIA_DE_JOELHOS, "a dupla anda junta");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // ---------------- 4, 7, 9: comandos de fuga sao ignorados na pegada
    @Test
    void puloRolamentoSprawlAgacharEPosturaNaoSoltam() throws Exception {
        for (String cmd : new String[] {"pretoPular", "pretoPularCurto", "pretoRolar", "pretoSprawl", "pretoAgachar",
            "pretoDeitar"}) {
            naEdt(() -> {
                j1Pega(InicioLuta.EM_PE);
                tecla("pretoEsquerda", true);                    // rolar precisa de direcao
                tecla(cmd, true);
                ticks(30);
                tecla(cmd, false);
                tecla("pretoEsquerda", false);
                ticks(30);
                assertTrue(s().isInteracaoEstabelecida(), cmd + ": a pegada continua");
                assertFalse(j2().isPulando() || j2().isRolando() || j2().isFazendoSprawl() || j2().isAgachado(), cmd);
            });
        }
    }

    // ---------------- 5: ROMPER continua sendo a saida
    @Test
    void romperContinuaFuncionandoPelaRegra() throws Exception {
        naEdt(() -> {
            j1Pega(InicioLuta.EM_PE);
            toque("pretoRompimento");
            EventoDefesa e = s().getUltimoEventoDefesa();
            if (s().isInteracaoEstabelecida()) {
                assertEquals(EventoDefesa.Tipo.ROMPIMENTO_FALHOU, e.tipo(), "resistido: a pegada fica");
                assertTrue(painel.getAnimador().getCamada().intensidadeForca(j2()) > 0, "puxao visual so com o evento");
                j1().consumirFolegoCombate(j1().getFolegoAtual());   // aplicador exausto
                ticks(SistemaCombate.RECARGA_ACAO_TECNICA_TICKS);
                toque("pretoRompimento");
            }
            assertFalse(s().estaEmInteracao());
            assertEquals(MotivoEncerramentoInteracao.ROMPIMENTO_TECNICO, s().getUltimoMotivoEncerramento());
            assertEquals(EventoDefesa.Tipo.ROMPIMENTO_SUCESSO, s().getUltimoEventoDefesa().tipo());
            assertEquals(EstadoVisual.REACAO_DEFESA, painel.getAnimador().origem(j2()).orElseThrow().estado());
            assertTrue(painel.linhasTecnicas().get(8).contains("ROMPIMENTO_TECNICO"), painel.linhasTecnicas().get(8));
        });
    }

    // ---------------- 6 e 8: evasao antes do contato e a janela do agachamento
    @Test
    void agacharATempoEvitaAPegadaComCustos() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.DE_JOELHOS);         // de joelhos: o "abaixar" tambem conta
            ticks(1);
            double fa = j1().getFolegoAtual();
            double fd = j2().getFolegoAtual();
            tecla("pretoAgachar", true);
            ticks(3);
            toque("brancoPegada1");
            tecla("pretoAgachar", false);
            assertFalse(s().estaEmInteracao(), "pegada evitada");
            assertEquals(ResultadoAcao.DEFENDIDO, s().getUltimoResultado());
            assertEquals(EventoDefesa.Tipo.PEGADA_EVITADA, s().getUltimoEventoDefesa().tipo());
            assertEquals("AGACHAMENTO", s().getUltimoEventoDefesa().detalhe());
            assertTrue(j2().getFolegoAtual() < fd - CustosFolego.EVASAO_ABAIXAR + 0.05, "quem evita paga");
            assertTrue(j1().getFolegoAtual() < fa - 0.8, "a tentativa custa metade ao atacante");   // 5P.1 (pedido): 1,8/2
            assertTrue(painel.mensagemFeedbackParaTestes().contains("EVITADA"), painel.mensagemFeedbackParaTestes());
            assertEquals(EstadoVisual.REACAO_DEFESA, painel.getAnimador().origem(j2()).orElseThrow().estado());
            assertEquals("DEFESA_ALCANCE_FALHO", painel.getAnimador().origem(j1()).orElseThrow().idAnimacao());
        });
    }

    @Test
    void agacharCedoDemaisNaoProtege() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.DE_JOELHOS);
            ticks(1);
            tecla("pretoAgachar", true);
            ticks(IrmaoMiau.JANELA_EVASAO_ABAIXAR_TICKS + 5);   // segurou demais: nao e "rapido"
            toque("brancoPegada1");
            tecla("pretoAgachar", false);
            assertTrue(s().estaEmInteracao(), "fora da janela a pegada entra");
        });
    }

    @Test
    void rolandoEvitaMasNaoFicaInvulneravel() throws Exception {
        naEdt(() -> {
            j1PegaPerto();
            // 5O.1 (pedido): em pe o R3 faz acrobacia; o rolamento tecnico parte de joelhos.
            j2().ajoelhar();
            tecla("pretoDireita", true);
            tecla("pretoRolar", true);
            ticks(1);
            tecla("pretoRolar", false);
            tecla("pretoDireita", false);
            assertTrue(j2().isRolando());
            tecla("brancoPegada1", true);
            ticks(1);
            tecla("brancoPegada1", false);
            assertFalse(s().estaEmInteracao(), "rolando: evitou");
            for (int i = 0; i < 200 && j2().isRolando(); i++) {
                ticks(1);
            }
            ticks(70);                                     // recarga/folego: tenta de novo parado
            tecla("brancoDireita", true);
            for (int i = 0; i < 300 && !s().podeTentarAcao(j1(), j2(), "pegada_1"); i++) {
                ticks(1);
            }
            tecla("brancoDireita", false);
            toque("brancoPegada1");
            assertTrue(s().estaEmInteracao(), "parado, a pegada entra");
        });
    }

    private void j1PegaPerto() {
        painel = dois(InicioLuta.EM_PE);
        ticks(1);
        tecla("brancoDireita", true);
        for (int i = 0; i < 300 && !s().podeTentarAcao(j1(), j2(), "pegada_1"); i++) {
            ticks(1);
        }
        tecla("brancoDireita", false);
    }

    // ---------------- 24: pausa e perda de foco durante a forca
    @Test
    void pausaEFocoNaoDesgastamNemSoltam() throws Exception {
        naEdt(() -> {
            j1Pega(InicioLuta.DE_JOELHOS);
            tecla("pretoEsquerda", true);
            ticks(5);
            double f = s().getFirmeza();
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            ticks(100);
            assertEquals(f, s().getFirmeza(), 1e-9, "pausado: nada muda");
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            painel.simularPerdaDeFocoParaTestes();
            ticks(10);
            assertEquals(0, s().getTicksResistindo(), "sem teclas, sem forca");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }
}
