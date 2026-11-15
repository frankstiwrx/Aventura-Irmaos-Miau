package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.EstadoVisual;
import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.combate.TipoDisputa;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.PosturaAjoelhada;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/** Entrega 5J (testes 14 a 19): postura e inicio de joelhos no GamePanel real. */
class Ajoelhado5JGamePanelTest {

    private GamePanel painel;

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private static EscolhaLutador oficial(String id) {
        return EscolhaLutador.oficial(CatalogoPersonagens.padrao().exigir(id));
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

    private GamePanel dois(EscolhaLutador j1, EscolhaLutador j2, InicioLuta inicio) {
        GamePanel g = new GamePanel(TipoPersonagem.AMBOS, j1, j2, inicio, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    /** Topo e base (linhas com pixel) do desenho do lutador. */
    private static int[] topoEBase(IrmaoMiau l) {
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        l.desenhar(g);
        g.dispose();
        int topo = -1;
        int base = -1;
        for (int y = 0; y < 450; y++) {
            for (int x = 0; x < 800; x++) {
                if ((img.getRGB(x, y) >>> 24) > 128) {
                    if (topo < 0) {
                        topo = y;
                    }
                    base = y;
                    break;
                }
            }
        }
        return new int[] {topo, base};
    }

    // ---------------- 14. postura ajoelhada dos dois personagens
    @Test
    void osDoisIrmaosAjoelhamComJoelhosNoTatame() {
        for (IrmaoMiau[] par : new IrmaoMiau[][] {
            {new IrmaoMiauBranco(300, 150, 0.7), new IrmaoMiauBranco(300, 150, 0.7)},
            {new IrmaoMiauPreto(300, 150, 0.7), new IrmaoMiauPreto(300, 150, 0.7)}}) {
            IrmaoMiau emPe = par[0];
            IrmaoMiau joelhos = par[1];
            joelhos.ajoelhar();
            assertTrue(joelhos.isAjoelhado());
            assertEquals(IrmaoMiau.EstadoCorporal.AJOELHADO, joelhos.getEstadoCorporal());
            joelhos.atualizar(false, true);               // agachar nao tira de joelhos
            assertTrue(joelhos.isAjoelhado());
            int[] a = topoEBase(emPe);
            int[] b = topoEBase(joelhos);
            double queda = PosturaAjoelhada.quedaPx(joelhos);
            assertEquals(a[0] + queda, b[0], 2, joelhos.getNome() + ": a cabeca desce a altura das coxas");
            assertEquals(a[1], b[1], 4, joelhos.getNome() + ": joelhos e pes no mesmo chao de quem esta em pe");
        }
    }

    @Test
    void deJoelhosNaoPulaNemRolaNemFazSprawlEFLevanta() {
        IrmaoMiau l = new IrmaoMiauBranco(300, 150, 0.7);
        l.ajoelhar();
        l.pular();
        l.pularCurto();
        l.iniciarRolamento(com.irmaosmiau.entities.Direcao.DIREITA);
        l.iniciarSprawl();
        l.setCorrendo(true);
        assertFalse(l.isPulando() || l.isRolando() || l.isFazendoSprawl() || l.isCorrendo());
        assertTrue(l.isAjoelhado(), "comandos incompativeis sao ignorados (comportamento seguro)");
        l.alternarDeitado();
        assertFalse(l.isAjoelhado(), "F (deitar/levantar) levanta");
        assertFalse(l.isDeitado());
    }

    // ---------------- 15. inicio proximo, mesma linha, sem sobreposicao
    @Test
    void inicioDeJoelhosEmTodasAsCombinacoes() throws Exception {
        String b = CatalogoPersonagens.ID_BRANCO;
        String p = CatalogoPersonagens.ID_PRETO;
        for (String[] c : new String[][] {{b, p}, {p, b}, {b, b}, {p, p}}) {
            naEdt(() -> {
                painel = dois(oficial(c[0]), oficial(c[1]), InicioLuta.DE_JOELHOS);
                ticks(1);
                IrmaoMiau j1 = painel.getLutador1();
                IrmaoMiau j2 = painel.getLutador2();
                String caso = c[0] + "/" + c[1];
                assertTrue(j1.isAjoelhado() && j2.isAjoelhado(), caso);
                assertEquals(j1.getLinhaAtual(), j2.getLinhaAtual(), caso + ": mesma linha");
                assertEquals(InicioLuta.DISTANCIA_DE_JOELHOS, j2.getX() - j1.getX(), caso);
                assertEquals(400 * 2, (j1.getX() + 24.5) + (j2.getX() + 24.5), 1.0, caso + ": simetricos no centro");
                assertTrue(j1.isViradoParaDireita() && !j2.isViradoParaDireita(), caso + ": frente a frente");
                assertTrue(s().podeTentarAcao(j1, j2, "pegada_1") && s().podeTentarAcao(j2, j1, "pegada_1"),
                        caso + ": os dois ja podem disputar a pegada");
                assertFalse(s().estaEmInteracao(), caso + ": nenhuma pegada automatica");
                assertEquals(0, s().getRecargaRestante(j1) + s().getRecargaRestante(j2), caso + ": ninguem comeca em recarga");
                assertTrue(j2.getX() - j1.getX() > 82 * 0.7, caso + ": sem sobreposicao dos corpos");
                assertEquals(EstadoVisual.AJOELHADO, painel.getAnimador().origem(j1).orElseThrow().estado());
                assertTrue(painel.getAnimador().origem(j1).orElseThrow().isJson(), "fallback em JSON substituivel");
            });
        }
    }

    // ---------------- 16. deslocamento lento
    @Test
    void ajoelhadoAndaDevagarSemLevantar() throws Exception {
        int[] dx = new int[2];
        naEdt(() -> {
            painel = dois(null, null, InicioLuta.EM_PE);
            int x = painel.getLutador1().getX();
            tecla("brancoEsquerda", true);
            ticks(30);
            tecla("brancoEsquerda", false);
            dx[0] = x - painel.getLutador1().getX();
            painel = dois(null, null, InicioLuta.DE_JOELHOS);
            x = painel.getLutador1().getX();
            tecla("brancoEsquerda", true);
            ticks(30);
            assertEquals(EstadoVisual.AJOELHADO_MOVIMENTO,
                    painel.getAnimador().origem(painel.getLutador1()).orElseThrow().estado());
            tecla("brancoEsquerda", false);
            ticks(1);
            dx[1] = x - painel.getLutador1().getX();
            assertTrue(painel.getLutador1().isAjoelhado(), "andar nao levanta");
            assertTrue(painel.getLutador1().isViradoParaDireita(), "orientacao continua frente a frente");
        });
        assertTrue(dx[1] > 0, "anda");
        assertTrue(dx[1] * 4 < dx[0], "bem mais devagar que em pe (" + dx[1] + " x " + dx[0] + ")");
    }

    // ---------------- 17. pegada de joelhos
    @Test
    void pegadaDeJoelhosMantemOsDoisDeJoelhosEAsAcoes() throws Exception {
        naEdt(() -> {
            painel = dois(null, null, InicioLuta.DE_JOELHOS);
            ticks(1);
            IrmaoMiau j1 = painel.getLutador1();
            IrmaoMiau j2 = painel.getLutador2();
            toque("pretoPegada1");                          // J2 aplica de joelhos
            for (int i = 0; i < 40; i++) {
                ticks(1);
                int[] pose = j2.copiarPoseCombateExterna();
                assertArrayEquals(PosturaAjoelhada.pernas(j2), Arrays.copyOfRange(pose, 22, 30),
                        "tick " + i + ": pernas de joelhos, sem salto para a postura em pe");
                assertEquals(PosturaAjoelhada.quedaPx(j2), j2.getDeslocamentoVisualCombateY(), 1e-9);
                assertArrayEquals(PosturaAjoelhada.pernas(j1), Arrays.copyOfRange(j1.copiarPoseCombateExterna(), 22, 30));
            }
            assertTrue(s().isInteracaoEstabelecida());
            assertSame(j2, s().getAplicadorInteracao());
            assertTrue(painel.linhasTecnicas().get(2).contains("pegada_1_entrada_preto"), painel.linhasTecnicas().toString());
            toque("pretoReforco");
            toque("pretoDesequilibrio");
            assertEquals(TipoDisputa.DESEQUILIBRIO, s().getUltimaDisputa().getTipo());
            int x2 = j2.getX();
            tecla("pretoControle", true);
            tecla("pretoEsquerda", true);
            ticks(8);
            tecla("pretoEsquerda", false);
            tecla("pretoControle", false);
            ticks(1);
            assertTrue(j2.getX() < x2, "arrasta de joelhos");
            assertTrue(s().isInteracaoEstabelecida());
            assertTrue(j1.isAjoelhado() && j2.isAjoelhado());
        });
    }

    @Test
    void levantarNoMeioDaPegadaRompeComoDeitar() throws Exception {
        naEdt(() -> {
            painel = dois(null, null, InicioLuta.DE_JOELHOS);
            ticks(1);
            toque("brancoPegada1");
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            toque("pretoDeitar");                            // F/L3 do receptor: levanta
            ticks(1);
            assertFalse(s().estaEmInteracao());
            assertEquals(MotivoEncerramentoInteracao.DEITAR_OU_LEVANTAR, s().getUltimoMotivoEncerramento());
        });
    }

    // ---------------- 18. combate em pe preservado
    @Test
    void inicioEmPeEIgualAoDeAntes() throws Exception {
        naEdt(() -> {
            GamePanel antigo = new GamePanel(TipoPersonagem.AMBOS, null, null, () -> { }, () -> { }, () -> { });
            antigo.pararLoopParaTestes();
            painel = dois(null, null, InicioLuta.EM_PE);
            assertEquals(antigo.getLutador1().getX(), painel.getLutador1().getX());
            assertEquals(antigo.getLutador2().getX(), painel.getLutador2().getX());
            assertFalse(painel.getLutador1().isAjoelhado());
            assertEquals(InicioLuta.EM_PE, antigo.getInicioLuta(), "sem escolha, o codigo continua em pe");
        });
    }

    // ---------------- 19. F5 no treino
    @Test
    void treinoDeJoelhosSobreviveAoF5() throws Exception {
        naEdt(() -> {
            ConfiguracaoTreino c = ConfiguracaoTreino.padrao()
                    .comJogador(oficial(CatalogoPersonagens.ID_PRETO))
                    .comParceiro(oficial(CatalogoPersonagens.ID_BRANCO))
                    .comInicio(InicioLuta.DE_JOELHOS);
            painel = new GamePanel(c, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(1);
            assertTrue(painel.getLutador1().isAjoelhado() && painel.getLutador2().isAjoelhado());
            int xp = painel.getLutador2().getX();
            toque("brancoPegada1");
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida(), "pega de joelhos no treino");
            assertEquals(xp, painel.getLutador2().getX(), "parceiro passivo");
            painel.reiniciarTreino();
            ticks(1);
            assertTrue(painel.getLutador1().isAjoelhado() && painel.getLutador2().isAjoelhado(), "F5 mantem de joelhos");
            assertEquals(InicioLuta.xEsquerdaDeJoelhos(0.7), painel.getLutador1().getX());
            assertEquals(InicioLuta.xDireitaDeJoelhos(0.7), painel.getLutador2().getX());
            assertTrue(painel.getLutador1() instanceof IrmaoMiauPreto, "e com o mesmo irmao");
            assertFalse(s().estaEmInteracao());
        });
    }

    @Test
    void treinoEmPeVoltaAsPosicoesPadrao() {
        ConfiguracaoTreino c = ConfiguracaoTreino.padrao().comInicio(InicioLuta.DE_JOELHOS).comInicio(InicioLuta.EM_PE);
        assertEquals(ConfiguracaoTreino.X_JOGADOR_PADRAO, c.getXJogador());
        assertEquals(ConfiguracaoTreino.X_PARCEIRO_PADRAO, c.getXParceiro());
        assertEquals(InicioLuta.EM_PE, ConfiguracaoTreino.padrao().getInicio());
    }
}
