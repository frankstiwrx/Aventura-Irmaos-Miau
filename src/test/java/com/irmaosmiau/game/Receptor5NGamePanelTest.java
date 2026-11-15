package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.CamadaPoseCombate;
import com.irmaosmiau.animacao.CamadaPoseCombate.FaseReceptor;
import com.irmaosmiau.animacao.EstadoVisual;
import com.irmaosmiau.animacao.ReacoesDefesa;
import com.irmaosmiau.combate.EventoDefesa;
import com.irmaosmiau.combate.ModoAnimacoesPegada;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;

import java.util.Arrays;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Entrega 5N (testes 1-23 e 51-58 da integracao): o receptor reage a cada
 * tecnica com a reacao compartilhada, mantem a pose sustentada enquanto a
 * pegada existe e sai dela sem salto, no GamePanel real.
 */
class Receptor5NGamePanelTest {

    private GamePanel painel;
    private IrmaoMiau apl;
    private IrmaoMiau rec;

    @BeforeEach
    void modoDoJogo() {
        ModoAnimacoesPegada.definir(ModoAnimacoesPegada.EXPERIMENTAL);
    }

    @AfterEach
    void restaurar() {
        ModoAnimacoesPegada.restaurarPadrao();
    }

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void tecla(String nome, boolean p) {
        painel.getActionMap().get(nome + (p ? "Pressionada" : "Solta")).actionPerformed(null);
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

    private CamadaPoseCombate camada() {
        return painel.getAnimador().getCamada();
    }

    private static EscolhaLutador oficial(String id) {
        return id == null ? null : EscolhaLutador.oficial(CatalogoPersonagens.padrao().exigir(id));
    }

    /** Monta a luta, aproxima e estabelece a pegada n (j1 = o J1 aplica). */
    private void pegada(String idJ1, String idJ2, int n, boolean j1, InicioLuta inicio) {
        painel = new GamePanel(TipoPersonagem.AMBOS, oficial(idJ1), oficial(idJ2), inicio, () -> { }, () -> { },
                () -> { });
        painel.pararLoopParaTestes();
        ticks(1);
        apl = j1 ? painel.getLutador1() : painel.getLutador2();
        rec = j1 ? painel.getLutador2() : painel.getLutador1();
        String andar = j1 ? "brancoDireita" : "pretoEsquerda";
        tecla(andar, true);
        for (int i = 0; i < 400 && !s().podeTentarAcao(apl, rec, "pegada_" + n); i++) {
            ticks(1);
        }
        tecla(andar, false);
        ticks(2);
        toque((j1 ? "branco" : "preto") + "Pegada" + n);
        ticks(40);
        assertTrue(s().isInteracaoEstabelecida(), "pegada_" + n + " estabelecida");
        assertSame(rec, s().getReceptorInteracao());
        assertEquals("pegada_" + n, s().getIdAcaoInteracao());
    }

    private void pegada(int n, boolean j1, InicioLuta inicio) {
        pegada(null, null, n, j1, inicio);
    }

    /** O delta do receptor esta mesmo na pose que vai para a tela. */
    private void reacaoNaTela(String caso) {
        assertEquals(FaseReceptor.SUSTENTADA, camada().faseReceptor(rec), caso);
        int[] delta = camada().deltaReceptor(rec);
        assertNotNull(delta, caso);
        int[] base = camada().poseBase(rec);
        int[] tela = rec.copiarPoseCombateExterna();
        int iguais = 0;
        for (int i = 4; i < 30; i++) {
            if (tela[i] == base[i]) {
                iguais++;
            }
        }
        assertTrue(iguais < 26, caso + ": a pose desenhada nao e a guarda do motor");
        assertTrue(rec.isPoseCombateExternaAtiva(), caso + ": o desenho usa a pose de combate");
    }

    // 1-4. Cada pegada produz reacao no receptor (em pe, Branco aplicando no Preto)
    @Test
    void quatroPegadasProduzemReacao() throws Exception {
        naEdt(() -> {
            for (int n = 1; n <= 4; n++) {
                pegada(n, true, InicioLuta.EM_PE);
                reacaoNaTela("P" + n);
            }
        });
    }

    // 5. As reacoes sao diferentes conforme o controle
    @Test
    void reacoesDiferentesNoJogo() throws Exception {
        int[][] deltas = new int[5][];
        naEdt(() -> {
            for (int n = 1; n <= 4; n++) {
                pegada(n, true, InicioLuta.EM_PE);
                deltas[n] = camada().deltaReceptor(rec);
            }
        });
        for (int a = 1; a <= 4; a++) {
            for (int b = a + 1; b <= 4; b++) {
                assertFalse(Arrays.equals(deltas[a], deltas[b]), "P" + a + " x P" + b);
            }
        }
    }

    // 6, 7, 8, 57. A pose sustentada fica enquanto a pegada existe; nada de idle/taunt/JSON por cima;
    // o relogio da sustentada nao recomeca a cada tick
    @Test
    void poseSustentadaPermanece() throws Exception {
        naEdt(() -> {
            pegada(1, true, InicioLuta.EM_PE);
            long inicio = camada().ticksSustentada(rec, s().getTick());
            for (int i = 0; i < 300; i++) {
                ticks(1);
                assertEquals(FaseReceptor.SUSTENTADA, camada().faseReceptor(rec));
                assertTrue(rec.isPoseCombateExternaAtiva(), "a pose de controle continua sendo a desenhada");
                assertFalse(painel.getAnimador().origem(rec).orElseThrow().isJson(), "nada de idle JSON por cima");
            }
            assertFalse(rec.estaEmTaunt() && !rec.isPoseCombateExternaAtiva());
            assertTrue(s().isInteracaoEstabelecida());
            assertTrue(camada().ticksSustentada(rec, s().getTick()) >= inicio + 300, "relogio continuo");
        });
    }

    // 9. Fim da pegada: a reacao sai (liberacao curta) e o receptor volta ao normal
    @Test
    void fimDaPegadaLibera() throws Exception {
        naEdt(() -> {
            pegada(4, true, InicioLuta.EM_PE);
            s().encerrarInteracao();
            ticks(1);
            assertEquals(FaseReceptor.NENHUMA, camada().faseReceptor(rec));
            ReacoesDefesa r = painel.getAnimador().getReacoes();
            assertEquals(ReacoesDefesa.Tipo.RECEPTOR_LIBERADO, r.ativa(rec, painel.tickParaTestes()));
            assertEquals(EstadoVisual.REACAO_DEFESA, painel.getAnimador().origem(rec).orElseThrow().estado());
            ticks(ReacoesDefesa.DURACAO_TICKS + 2);
            assertNull(r.ativa(rec, painel.tickParaTestes()), "volta a postura normal");
            assertNotNull(r.getUltimoFim());
        });
    }

    // 56. Sem teletransporte: a liberacao comeca da pose sustentada (nenhuma articulacao salta)
    @Test
    void liberacaoSemSalto() throws Exception {
        naEdt(() -> {
            pegada(1, true, InicioLuta.EM_PE);
            int[] antes = rec.copiarPoseCombateExterna();
            s().encerrarInteracao();
            ticks(1);
            int[] depois = rec.copiarPoseCombateExterna();
            assertNotNull(depois);
            for (int i = 4; i < 30; i++) {
                assertTrue(Math.abs(depois[i] - antes[i]) <= 8, "salto na articulacao " + i);
            }
        });
    }

    // 10. Rompimento com sucesso libera a pose
    @Test
    void rompimentoLibera() throws Exception {
        naEdt(() -> {
            pegada(1, true, InicioLuta.EM_PE);
            apl.consumirFolegoCombate(apl.getFolegoAtual());   // aplicador exausto: o ROMPER entra
            toque("pretoRompimento");
            assertFalse(s().estaEmInteracao());
            assertEquals(EventoDefesa.Tipo.ROMPIMENTO_SUCESSO, s().getUltimoEventoDefesa().tipo());
            assertEquals(FaseReceptor.NENHUMA, camada().faseReceptor(rec));
            assertEquals(ReacoesDefesa.Tipo.DEFESA_ROMPIMENTO_SUCESSO,
                    painel.getAnimador().getReacoes().ativa(rec, painel.tickParaTestes()));
        });
    }

    // 11 e 14. ROMPER resistido e forcar mantem a pose de controle (com esforco visivel)
    @Test
    void resistenciaMantemControle() throws Exception {
        naEdt(() -> {
            pegada(2, true, InicioLuta.EM_PE);
            tecla("pretoDireita", true);
            ticks(10);
            assertTrue(camada().intensidadeForca(rec) > 0, "forcando");
            assertEquals(FaseReceptor.SUSTENTADA, camada().faseReceptor(rec));
            tecla("pretoDireita", false);
            ticks(1);
            toque("pretoRompimento");
            if (s().isInteracaoEstabelecida()) {
                assertEquals(EventoDefesa.Tipo.ROMPIMENTO_FALHOU, s().getUltimoEventoDefesa().tipo());
                assertEquals(FaseReceptor.SUSTENTADA, camada().faseReceptor(rec), "ROMPER resistido: pose fica");
                assertTrue(camada().intensidadeForca(rec) > 0);
            }
        });
    }

    // 12. Desequilibrio confirmado: o receptor reage e continua na pose de controle
    @Test
    void desequilibrioNaPose() throws Exception {
        naEdt(() -> {
            pegada(3, true, InicioLuta.EM_PE);
            rec.consumirFolegoCombate(rec.getFolegoAtual());
            for (int i = 0; i < 8 && camada().intensidadeReacao(rec) == 0; i++) {
                toque("brancoDesequilibrio");
                ticks(SistemaCombate.RECARGA_ACAO_TECNICA_TICKS);
                ticks(1);
                if (camada().intensidadeReacao(rec) == 0) {
                    continue;
                }
            }
            for (int i = 0; i < 4 && camada().intensidadeReacao(rec) == 0; i++) {
                toque("brancoDesequilibrio");
                ticks(2);
            }
            assertTrue(camada().intensidadeReacao(rec) > 0 || rec.getEfeitos()
                    .getTicksRestantes(com.irmaosmiau.entities.EfeitosTemporarios.Tipo.DESEQUILIBRIO) > 0);
            assertEquals(FaseReceptor.SUSTENTADA, camada().faseReceptor(rec));
        });
    }

    // 13. Reforco confirmado: o aplicador ajusta e o receptor acusa a pressao
    @Test
    void reforcoVisivel() throws Exception {
        naEdt(() -> {
            pegada(1, true, InicioLuta.EM_PE);
            ticks(400);                                        // a firmeza cai um pouco
            double f = s().getFirmeza();
            toque("brancoReforco");
            assertTrue(s().getFirmeza() > f + 5, "o motor reforcou");
            assertTrue(camada().intensidadeReforco(rec) > 0, "receptor reage");
            assertTrue(camada().intensidadeReforco(apl) > 0, "aplicador ajusta");
            assertEquals(FaseReceptor.SUSTENTADA, camada().faseReceptor(rec));
        });
    }

    // 15-19 e 58. Branco x Preto, Preto x Branco, Branco x Branco, Preto x Preto, aplicador nos dois slots
    @Test
    void combinacoesDePersonagens() throws Exception {
        String b = CatalogoPersonagens.ID_BRANCO;
        String p = CatalogoPersonagens.ID_PRETO;
        String[][] duplas = {{b, p}, {p, b}, {b, b}, {p, p}};
        naEdt(() -> {
            for (String[] d : duplas) {
                for (boolean j1 : new boolean[] {true, false}) {
                    for (int n = 1; n <= 4; n++) {
                        pegada(d[0], d[1], n, j1, InicioLuta.EM_PE);
                        String caso = d[0] + " x " + d[1] + " P" + n + (j1 ? " J1 aplica" : " J2 aplica");
                        reacaoNaTela(caso);
                        assertTrue(rec instanceof IrmaoMiauBranco || rec instanceof IrmaoMiauPreto);
                    }
                }
            }
        });
    }

    // 20. Orientacao: o mesmo delta para quem olha para a esquerda ou para a direita
    @Test
    void mesmaReacaoDosDoisLados() throws Exception {
        int[][] d = new int[2][];
        naEdt(() -> {
            pegada(2, true, InicioLuta.EM_PE);           // receptor (J2) olha para a esquerda
            assertFalse(rec.isViradoParaDireita());
            d[0] = camada().deltaReceptor(rec);
            pegada(2, false, InicioLuta.EM_PE);          // receptor (J1) olha para a direita
            assertTrue(rec.isViradoParaDireita());
            d[1] = camada().deltaReceptor(rec);
        });
        assertArrayEquals(d[0], d[1], "delta local; o espelhamento e do GamePanel");
    }

    // 21-22. Em pe os pes ficam apoiados; de joelhos as pernas sao as do motor (nao levanta)
    @Test
    void emPeEDeJoelhos() throws Exception {
        naEdt(() -> {
            for (int n = 1; n <= 4; n++) {
                pegada(n, true, InicioLuta.EM_PE);
                int[] d = camada().deltaReceptor(rec);
                assertEquals(0, d[24] | d[25] | d[28] | d[29], "pes apoiados");
                pegada(n, true, InicioLuta.DE_JOELHOS);
                assertTrue(rec.isAjoelhado(), "continua de joelhos");
                d = camada().deltaReceptor(rec);
                for (int i = 22; i < 30; i++) {
                    assertEquals(0, d[i], "P" + n + " de joelhos: pernas do motor");
                }
                reacaoNaTela("P" + n + " de joelhos");
            }
        });
    }

    // 23. F6 (autoral) desliga a reacao: a pose do receptor e a do motor, como antes da 5N
    @Test
    void f6VoltaAoAutoral() throws Exception {
        naEdt(() -> {
            ModoAnimacoesPegada.definir(ModoAnimacoesPegada.AUTORAL);
            pegada(1, true, InicioLuta.EM_PE);
            assertEquals(FaseReceptor.NENHUMA, camada().faseReceptor(rec));
            assertNull(camada().deltaReceptor(rec));
            assertArrayEquals(camada().poseBase(rec), rec.copiarPoseCombateExterna());
            s().encerrarInteracao();
            ticks(1);
            assertNull(painel.getAnimador().getReacoes().ativa(rec, painel.tickParaTestes()),
                    "sem reacao 5N no autoral");
        });
    }

    // 51-52. Contato no momento certo da entrada do aplicador
    @Test
    void contatoNoMeioDaEntrada() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(1);
            apl = painel.getLutador1();
            rec = painel.getLutador2();
            tecla("brancoDireita", true);
            for (int i = 0; i < 400 && !s().podeTentarAcao(apl, rec, "pegada_1"); i++) {
                ticks(1);
            }
            tecla("brancoDireita", false);
            ticks(2);
            tecla("brancoPegada1", true);
            ticks(1);
            tecla("brancoPegada1", false);
            assertTrue(s().isAnimacaoCombateAtiva());
            boolean viuSemContato = false;
            boolean viuContato = false;
            for (int i = 0; i < 40 && s().isAnimacaoCombateAtiva(); i++) {
                ticks(1);
                int[] d = camada().deltaReceptor(rec);
                boolean algum = d != null && Arrays.stream(d).anyMatch(v -> v != 0);
                if (s().getProgressoAnimacaoCombate() < 0.35) {
                    viuSemContato |= !algum;
                }
                viuContato |= algum && camada().faseReceptor(rec) == FaseReceptor.CONTATO;
            }
            assertTrue(viuSemContato, "antes da mao chegar, nada");
            assertTrue(viuContato, "na chegada, CONTATO");
            ticks(5);
            assertEquals(FaseReceptor.SUSTENTADA, camada().faseReceptor(rec));
        });
    }
}
