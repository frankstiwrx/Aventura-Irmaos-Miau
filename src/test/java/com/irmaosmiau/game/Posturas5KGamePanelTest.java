package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.AnimadorPartida.OrigemVisual;
import com.irmaosmiau.animacao.EstadoVisual;
import com.irmaosmiau.animacao.FonteAnimacao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.CustosFolego;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.entities.TransicaoPostura;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;
import com.irmaosmiau.treino.ConfiguracaoTreino;
import com.irmaosmiau.ui.NavegadorMenusAcesso;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5K (testes 1-16, 17, 23, 24, 35) no GamePanel real. */
class Posturas5KGamePanelTest {

    private static final int JANELA = GamePanel.JANELA_DUPLO_CLIQUE_TICKS;

    private GamePanel painel;
    private ProvedorGamepadSimulado sim;

    @AfterEach
    void restaurar() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
        PreferenciasControles.instalarGlobal(null);
    }

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

    private void duplo(String nome) {
        toque(nome);
        toque(nome);
    }

    private GamePanel dois(InicioLuta inicio) {
        GamePanel g = new GamePanel(TipoPersonagem.AMBOS, null, null, inicio, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    private IrmaoMiau j1() {
        return painel.getLutador1();
    }

    private IrmaoMiau j2() {
        return painel.getLutador2();
    }

    private SistemaCombate s() {
        return painel.getSistemaCombate();
    }

    // ---------------- 1, 2, 9: ciclo simples pelo teclado, so depois da janela
    @Test
    void cliqueSimplesEsperaAJanelaESegueOCiclo() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.EM_PE);
            toque("brancoDeitar");
            assertEquals(EstadoCorporal.EM_PE, j1().getEstadoCorporal(), "nada antes da janela fechar");
            assertTrue(painel.isCliquePosturaPendente(SlotJogador.JOGADOR_1));
            assertTrue(painel.linhasTecnicas().get(0).contains("clique..."), painel.linhasTecnicas().get(0));
            ticks(JANELA);
            assertEquals(EstadoCorporal.AJOELHADO, j1().getEstadoCorporal());
            EstadoCorporal[] esperado = {EstadoCorporal.DEITADO, EstadoCorporal.AJOELHADO, EstadoCorporal.EM_PE};
            for (EstadoCorporal e : esperado) {
                ticks(20);
                toque("brancoDeitar");
                ticks(JANELA);
                assertEquals(e, j1().getEstadoCorporal());
            }
        });
    }

    // ---------------- 3, 4, 6, 13: duplo clique, sem intermediarias, J1 e J2 independentes
    @Test
    void duploCliqueDeitaELevantaDiretoEJ2NaoSeMexe() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.EM_PE);
            double f = j1().getFolegoAtual();
            tecla("brancoDeitar", true);
            ticks(1);
            tecla("brancoDeitar", false);
            ticks(1);
            tecla("brancoDeitar", true);
            ticks(1);                                     // aqui o duplo e reconhecido
            assertTrue(j1().isDeitado(), "em pe -> deitado direto");
            assertEquals(f - CustosFolego.RAPIDA_DEITAR, j1().getFolegoAtual(), 0.05, "um custo so");
            tecla("brancoDeitar", false);
            ticks(JANELA + 5);
            assertTrue(j1().isDeitado(), "nenhum simples atrasado depois do duplo");
            assertEquals(EstadoCorporal.EM_PE, j2().getEstadoCorporal(), "J2 independente");

            duplo("pretoDeitar");
            assertTrue(j2().isDeitado());
            ticks(20);
            duplo("brancoDeitar");
            assertEquals(EstadoCorporal.EM_PE, j1().getEstadoCorporal(), "deitado -> em pe direto");
            assertTrue(j2().isDeitado());
        });
    }

    // ---------------- 5: de joelhos o duplo levanta
    @Test
    void duploCliqueDeJoelhosLevanta() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.DE_JOELHOS);
            ticks(1);
            duplo("brancoDeitar");
            assertEquals(EstadoCorporal.EM_PE, j1().getEstadoCorporal());
            assertEquals(TransicaoPostura.RAPIDA_LEVANTAR, j1().getTransicaoAtiva());
        });
    }

    // ---------------- 10: segurar nao repete
    @Test
    void segurarOBotaoNaoRepete() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.EM_PE);
            tecla("brancoDeitar", true);
            ticks(200);
            tecla("brancoDeitar", false);
            ticks(5);
            assertEquals(EstadoCorporal.AJOELHADO, j1().getEstadoCorporal(), "um clique so");
        });
    }

    // ---------------- 15: pausa, foco e F5 limpam o pendente; menu nao vira postura
    @Test
    void pausaFocoEF5LimpamOCliquePendente() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.EM_PE);
            toque("brancoDeitar");
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            ticks(JANELA + 5);
            assertEquals(EstadoCorporal.EM_PE, j1().getEstadoCorporal(), "pausa descarta o clique");

            toque("brancoDeitar");
            painel.simularPerdaDeFocoParaTestes();
            ticks(JANELA + 5);
            assertEquals(EstadoCorporal.EM_PE, j1().getEstadoCorporal(), "perda de foco descarta");

            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            tecla("brancoDeitar", true);                  // segurado no menu
            NavegadorMenusAcesso.executar(painel, painel.getBotaoContinuar(), Comando.CONFIRMAR);
            ticks(JANELA + 5);
            tecla("brancoDeitar", false);
            ticks(2);
            assertEquals(EstadoCorporal.EM_PE, j1().getEstadoCorporal(), "o botao do menu nao vira postura");
        });
        naEdt(() -> {
            painel = new GamePanel(ConfiguracaoTreino.padrao().comInicio(InicioLuta.DE_JOELHOS), () -> { }, () -> { },
                    () -> { });
            painel.pararLoopParaTestes();
            ticks(1);
            toque("brancoDeitar");
            painel.reiniciarTreino();
            ticks(JANELA + 5);
            assertTrue(j1().isAjoelhado(), "F5 descarta o clique e volta de joelhos");
        });
    }

    // ---------------- 16: nada de fuga pela postura na pegada
    @Test
    void posturaBloqueadaParaAplicadorEReceptorNaPegada() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.DE_JOELHOS);
            ticks(1);
            toque("brancoPegada1");
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            double f = j1().getFolegoAtual();
            duplo("brancoDeitar");
            toque("pretoDeitar");
            ticks(JANELA + 5);
            assertTrue(j1().isAjoelhado() && j2().isAjoelhado(), "aplicador e receptor seguem de joelhos");
            assertTrue(s().isInteracaoEstabelecida());
            assertTrue(j1().getFolegoAtual() >= f - 1e-9, "nenhum custo de transicao cobrado");
        });
    }

    // ---------------- 12: sem folego, feedback e nada muda
    @Test
    void semFolegoParaORapidoNadaAcontece() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.EM_PE);
            while (j1().getFolegoAtual() >= 1.0) {
                j1().consumirFolegoCombate(1.0);
            }
            duplo("brancoDeitar");
            assertEquals(EstadoCorporal.EM_PE, j1().getEstadoCorporal());
            assertTrue(painel.mensagemFeedbackParaTestes().contains("SEM FÔLEGO"), painel.mensagemFeedbackParaTestes());
        });
    }

    // ---------------- 14: Xbox (Classico e Contextual) e remapeamento
    @Test
    void l3DoXboxNosDoisEsquemas() throws Exception {
        for (EsquemaGamepad esquema : EsquemaGamepad.values()) {
            sim = new ProvedorGamepadSimulado();
            sim.conectar("g1", "ID0", "Xbox 1");
            ServicoGamepads.instalarGlobal(sim);
            PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                    AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_2, TipoDispositivo.GAMEPAD)));
            PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(
                    MapaGamepad.padrao().comEsquema(SlotJogador.JOGADOR_2, esquema)));
            naEdt(() -> {
                painel = dois(InicioLuta.DE_JOELHOS);
                ticks(1);
                sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.L3));
                ticks(1);
                sim.definir("g1", EstadoGamepad.repouso());
                ticks(JANELA + 2);
                assertTrue(j2().isDeitado(), esquema + ": L3 simples de joelhos (vindo de em pe) deita");
                for (int i = 0; i < 2; i++) {
                    sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.L3));
                    ticks(1);
                    sim.definir("g1", EstadoGamepad.repouso());
                    ticks(1);
                }
                assertEquals(EstadoCorporal.EM_PE, j2().getEstadoCorporal(), esquema + ": L3 + L3 levanta direto");
                assertTrue(j1().isAjoelhado(), "J1 no teclado nao foi afetado");
            });
        }
    }

    @Test
    void comandoDePosturaRemapeado() throws Exception {
        PreferenciasControles.instalarGlobal(PreferenciasControles.emMemoria(MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.DEITAR_LEVANTAR, TeclaFisica.porNome("Q").orElseThrow())));
        naEdt(() -> {
            painel = dois(InicioLuta.EM_PE);
            Object comando = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("pressed Q"));
            assertEquals("brancoDeitarPressionada", comando, "Q agora e o comando de postura do J1");
            duplo("brancoDeitar");
            assertTrue(j1().isDeitado());
        });
    }

    // ---------------- 35: estado logico e animacao sincronizados
    @Test
    void animacaoDaTransicaoAcompanhaOEstado() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.EM_PE);
            duplo("brancoDeitar");
            OrigemVisual o = painel.getAnimador().origem(j1()).orElseThrow();
            assertEquals(EstadoVisual.TRANSICAO_POSTURA, o.estado());
            assertEquals(FonteAnimacao.PROCEDURAL, o.fonte());
            assertEquals("TRANSICAO_RAPIDA_DEITAR", o.idAnimacao());
            assertTrue(j1().isPoseCombateExternaAtiva(), "desenhando a transicao");
            assertTrue(painel.linhasTecnicas().get(7).contains("TRANSICAO_RAPIDA_DEITAR"), painel.linhasTecnicas().toString());
            ticks(TransicaoPostura.RAPIDA_DEITAR.getDuracaoTicks() + 1);
            assertEquals(EstadoVisual.DEITADO, painel.getAnimador().origem(j1()).orElseThrow().estado());
            assertFalse(j1().isPoseCombateExternaAtiva(), "fim da transicao: desenho normal do deitado");
        });
    }

    // ---------------- 17, 23, 24: folego no jogo
    @Test
    void caminharNoJogoGastaEArrastarCobraSoDoAplicador() throws Exception {
        naEdt(() -> {
            painel = dois(InicioLuta.EM_PE);
            double f = j1().getFolegoAtual();
            tecla("brancoEsquerda", true);
            ticks(625);
            tecla("brancoEsquerda", false);
            assertEquals(1.0, f - j1().getFolegoAtual(), 0.05, "10 s caminhando ~ 1 ponto");
            assertTrue(painel.linhasTecnicas().get(5).startsWith("FOLEGO J1: "), painel.linhasTecnicas().toString());
        });
        naEdt(() -> {
            painel = dois(InicioLuta.DE_JOELHOS);
            ticks(1);
            toque("pretoPegada1");
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            ticks(600);                                    // recupera ao maximo parado na pegada
            double fa = j2().getFolegoAtual();
            double fr = j1().getFolegoAtual();
            tecla("pretoControle", true);
            tecla("pretoEsquerda", true);
            ticks(60);
            tecla("pretoEsquerda", false);
            tecla("pretoControle", false);
            assertTrue(j2().getFolegoAtual() < fa, "o aplicador J2 gasta arrastando");
            assertTrue(j1().getFolegoAtual() >= fr - 1e-9, "o receptor nao paga");
            double parado = j2().getFolegoAtual();
            ticks(30);
            assertTrue(j2().getFolegoAtual() >= parado, "dupla parada: sem custo de arrasto (recupera)");
        });
    }
}
