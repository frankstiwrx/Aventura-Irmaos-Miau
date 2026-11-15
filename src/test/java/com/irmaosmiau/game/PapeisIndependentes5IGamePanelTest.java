package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.combate.TipoDisputa;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Entrega 5I no GamePanel real: os papeis de aplicador e receptor vem da luta,
 * nao do slot. J1 = jogadorBranco do painel (getLutador1), J2 = getLutador2,
 * qualquer que seja o irmao escolhido.
 */
class PapeisIndependentes5IGamePanelTest {

    // 5O.2 (pedido): o padrao virou o Contextual; este teste e do Classico (MapaGamepad.classico() = o padrao antigo).

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;

    private GamePanel painel;
    private ProvedorGamepadSimulado sim;

    @AfterEach
    void restaurar() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
    }

    // =====================================================
    // AJUDANTES
    // =====================================================
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

    private IrmaoMiau l1() {
        return painel.getLutador1();
    }

    private IrmaoMiau l2() {
        return painel.getLutador2();
    }

    private GamePanel dois(EscolhaLutador j1, EscolhaLutador j2) {
        GamePanel g = new GamePanel(TipoPersonagem.AMBOS, j1, j2, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    private GamePanel treino(EscolhaLutador jogador, EscolhaLutador parceiro) {
        GamePanel g = new GamePanel(ConfiguracaoTreino.padrao().comJogador(jogador).comParceiro(parceiro),
                () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    private void comGamepads(MapaGamepad mapa, SlotJogador... noGamepad) {
        sim = new ProvedorGamepadSimulado();
        AssociacaoDispositivos a = AssociacaoDispositivos.padrao();
        for (int i = 0; i < noGamepad.length; i++) {
            sim.conectar("g" + (i + 1), "ID" + i, "Xbox " + (i + 1));
            a = a.com(noGamepad[i], TipoDispositivo.GAMEPAD);
        }
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(a));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(mapa));
    }

    /** Anda com a tecla ate o alcance da PEGADA_1 de quem anda sobre o outro. */
    private int aproximar(String teclaAndar, IrmaoMiau quem, IrmaoMiau alvo) {
        tecla(teclaAndar, true);
        int n = 0;
        for (; n < 300 && !s().podeTentarAcao(quem, alvo, "pegada_1"); n++) {
            ticks(1);
        }
        tecla(teclaAndar, false);
        assertTrue(s().podeTentarAcao(quem, alvo, "pegada_1"), "chegou ao alcance");
        return n;
    }

    /** Toca a pegada e conta os ticks ate a pegada ficar estabelecida. */
    private int pegarEContar(String teclaPegada) {
        tecla(teclaPegada, true);
        ticks(1);
        tecla(teclaPegada, false);
        assertTrue(s().estaEmInteracao(), "a pegada comecou");
        int n = 1;
        for (; n < 200 && !s().isInteracaoEstabelecida(); n++) {
            ticks(1);
        }
        assertTrue(s().isInteracaoEstabelecida());
        return n;
    }

    /** J2 (setas) pega o J1 vindo pela direita. */
    private int j2Pega() {
        aproximar("pretoEsquerda", l2(), l1());
        return pegarEContar("pretoPegada1");
    }

    private int j1Pega() {
        aproximar("brancoDireita", l1(), l2());
        return pegarEContar("brancoPegada1");
    }

    // =====================================================
    // 2 e 3. TREINO CONTROLANDO QUALQUER IRMAO
    // =====================================================
    @Test
    void treinoControlandoOPretoFazTudoEOBrancoFicaPassivo() throws Exception {
        naEdt(() -> {
            painel = treino(oficial(CatalogoPersonagens.ID_PRETO), oficial(CatalogoPersonagens.ID_BRANCO));
            assertTrue(l1() instanceof IrmaoMiauPreto, "o jogador controla o Preto");
            assertTrue(l2() instanceof IrmaoMiauBranco, "o Branco e o parceiro");
            int xParceiro = l2().getX();
            j1Pega();                                       // WASD + H movem e pegam com o Preto
            assertSame(l1(), s().getAplicadorInteracao());
            assertSame(l2(), s().getReceptorInteracao());
            assertEquals(xParceiro, l2().getX(), "parceiro passivo nao anda");

            // Teclas do segundo jogador seguem ignoradas no treino.
            toque("pretoRompimento");
            assertTrue(s().isInteracaoEstabelecida(), "o parceiro nao rompe sozinho");

            // Reforco, desequilibrio e arrasto (SHIFT + D) do Preto aplicador.
            toque("brancoReforco");
            assertTrue(s().getRecargaReforcoRestante(l1()) > 0, "reforco aplicado");
            int xb = l1().getX();
            tecla("brancoControle", true);
            tecla("brancoDireita", true);
            ticks(10);
            tecla("brancoDireita", false);
            tecla("brancoControle", false);
            ticks(1);
            assertTrue(l1().getX() > xb, "arrasta o parceiro");
            assertTrue(s().isInteracaoEstabelecida());
            toque("brancoDesequilibrio");
            assertEquals(TipoDisputa.DESEQUILIBRIO, s().getUltimaDisputa().getTipo());
        });
    }

    @Test
    void treinoControlandoOBrancoContinuaComoAntes() throws Exception {
        naEdt(() -> {
            painel = treino(oficial(CatalogoPersonagens.ID_BRANCO), oficial(CatalogoPersonagens.ID_PRETO));
            assertTrue(l1() instanceof IrmaoMiauBranco);
            assertTrue(l2() instanceof IrmaoMiauPreto);
            j1Pega();
            assertSame(l1(), s().getAplicadorInteracao());
        });
    }

    // =====================================================
    // 4, 5 e 6. PEGADA PELO J1 OU PELO J2 (PAPEIS INVERTIDOS)
    // =====================================================
    @Test
    void j1IniciaAPegadaComoAntes() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            j1Pega();
            assertSame(l1(), s().getAplicadorInteracao());
            assertSame(l2(), s().getReceptorInteracao());
        });
    }

    @Test
    void j2IniciaAPegadaEOsPapeisSeInvertem() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            j2Pega();
            assertSame(l2(), s().getAplicadorInteracao(), "J2 (Preto) aplica");
            assertSame(l1(), s().getReceptorInteracao(), "J1 (Branco) recebe");
            assertTrue(s().estaEmPegada1());
        });
    }

    @Test
    void j2ComOBrancoTambemAplica() throws Exception {
        naEdt(() -> {
            painel = dois(oficial(CatalogoPersonagens.ID_PRETO), oficial(CatalogoPersonagens.ID_BRANCO));
            assertTrue(l1() instanceof IrmaoMiauPreto);
            assertTrue(l2() instanceof IrmaoMiauBranco);
            j2Pega();
            assertSame(l2(), s().getAplicadorInteracao());
        });
    }

    // =====================================================
    // 7. POSES COERENTES COM OS PAPEIS INVERTIDOS
    // =====================================================
    /** Mascara alfa dos dois lutadores desenhados como no GamePanel (espelho incluso). */
    private static boolean[][] mascara(IrmaoMiau... lutadores) {
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        for (IrmaoMiau l : lutadores) {
            Graphics2D g = img.createGraphics();
            if (!l.isViradoParaDireita()) {
                double eixo = l.getX() + 35 * 0.7;
                g.translate(eixo, 0);
                g.scale(-1, 1);
                g.translate(-eixo, 0);
            }
            l.desenhar(g);
            g.dispose();
        }
        boolean[][] m = new boolean[450][800];
        for (int y = 0; y < 450; y++) {
            for (int x = 0; x < 800; x++) {
                m[y][x] = (img.getRGB(x, y) >>> 24) > 128;
            }
        }
        return m;
    }

    private static int esquerda(boolean[][] m) {
        for (int x = 0; x < 800; x++) {
            for (boolean[] linha : m) {
                if (linha[x]) {
                    return x;
                }
            }
        }
        return -1;
    }

    /** Fracao de pixels em comum (IoU) entre a e o espelho horizontal de b, alinhados pela esquerda. */
    private static double semelhancaEspelhada(boolean[][] a, boolean[][] b) {
        boolean[][] e = new boolean[450][800];
        for (int y = 0; y < 450; y++) {
            for (int x = 0; x < 800; x++) {
                e[y][799 - x] = b[y][x];
            }
        }
        // O eixo do espelho do GamePanel cai em meio pixel: aceita o melhor alinhamento a +-2 px.
        double melhor = 0;
        for (int ajuste = -2; ajuste <= 2; ajuste++) {
            int dx = esquerda(a) - esquerda(e) + ajuste;
            int inter = 0;
            int uniao = 0;
            for (int y = 0; y < 450; y++) {
                for (int x = 0; x < 800; x++) {
                    int xe = x - dx;
                    boolean pa = a[y][x];
                    boolean pe = xe >= 0 && xe < 800 && e[y][xe];
                    inter += pa && pe ? 1 : 0;
                    uniao += pa || pe ? 1 : 0;
                }
            }
            melhor = Math.max(melhor, uniao == 0 ? 0 : (double) inter / uniao);
        }
        return melhor;
    }

    @Test
    void posesDaPegadaComPapeisInvertidosSaoOEspelhoDasOriginais() throws Exception {
        // A: J1 = Branco (esquerda) aplica no Preto. B: J1 = Preto (esquerda), J2 = Branco aplica
        // vindo da direita. Mesmos irmaos nos mesmos papeis, lados trocados: o desenho de B deve
        // ser o espelho do de A (aplicador e receptor recebem os seus proprios quadros).
        int[] marcos = {1, 8, 16, 24};
        boolean[][][] a = new boolean[marcos.length + 1][][];
        boolean[][][] b = new boolean[marcos.length + 1][][];
        int[] distA = new int[marcos.length + 1];
        int[] distB = new int[marcos.length + 1];
        naEdt(() -> {
            painel = dois(null, null);
            aproximar("brancoDireita", l1(), l2());
            tecla("brancoPegada1", true);
            for (int i = 0, t = 0; i < marcos.length; i++) {
                for (; t < marcos[i]; t++) {
                    ticks(1);
                    tecla("brancoPegada1", false);
                }
                a[i] = mascara(l1(), l2());
                distA[i] = l2().getX() - l1().getX();
            }
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            a[marcos.length] = mascara(l1(), l2());
            distA[marcos.length] = l2().getX() - l1().getX();
            assertTrue(l1().isViradoParaDireita() && !l2().isViradoParaDireita(), "frente a frente");
        });
        naEdt(() -> {
            painel = dois(oficial(CatalogoPersonagens.ID_PRETO), oficial(CatalogoPersonagens.ID_BRANCO));
            aproximar("pretoEsquerda", l2(), l1());
            tecla("pretoPegada1", true);
            for (int i = 0, t = 0; i < marcos.length; i++) {
                for (; t < marcos[i]; t++) {
                    ticks(1);
                    tecla("pretoPegada1", false);
                }
                b[i] = mascara(l1(), l2());
                distB[i] = l2().getX() - l1().getX();
            }
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            b[marcos.length] = mascara(l1(), l2());
            distB[marcos.length] = l2().getX() - l1().getX();
            assertSame(l2(), s().getAplicadorInteracao());
            assertTrue(l1().isViradoParaDireita() && !l2().isViradoParaDireita(), "frente a frente");
        });
        for (int i = 0; i <= marcos.length; i++) {
            assertEquals(distA[i], distB[i], "mesma distancia entre a dupla no quadro " + i);
            double iou = semelhancaEspelhada(a[i], b[i]);
            assertTrue(iou > 0.95, "quadro " + i + ": silhuetas espelhadas (IoU " + iou + ")");
            // Controle: a pose nao e simetrica, entao o teste distingue troca indevida de quadros.
            assertTrue(semelhancaEspelhada(a[i], a[i]) < 0.85, "a dupla nao e simetrica no quadro " + i);
        }
    }

    // =====================================================
    // 8 e 9. ACOES DO APLICADOR J2 E DO RECEPTOR J1
    // =====================================================
    @Test
    void aplicadorJ2ReforcaEDesequilibra() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            j2Pega();
            toque("pretoReforco");
            assertTrue(s().getRecargaReforcoRestante(l2()) > 0, "PAGE_UP reforca");
            assertEquals(0, s().getRecargaReforcoRestante(l1()));
            toque("pretoDesequilibrio");
            assertNotNull(s().getUltimaDisputa());
            assertEquals(TipoDisputa.DESEQUILIBRIO, s().getUltimaDisputa().getTipo());
        });
    }

    @Test
    void receptorJ1RompeEJ1NaoDesequilibraComoReceptor() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            j2Pega();
            toque("brancoDesequilibrio");                   // nao e o aplicador
            assertNull(s().getUltimaDisputa(), "desequilibrio do receptor nao vale");
            toque("brancoReforco");
            assertEquals(0, s().getRecargaReforcoRestante(l1()), "reforco do receptor nao vale");
            toque("brancoRompimento");                      // tecla 5
            assertNotNull(s().getUltimaDisputa());
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, s().getUltimaDisputa().getTipo());
        });
    }

    // =====================================================
    // 10, 11 e 12. ARRASTO
    // =====================================================
    @Test
    void aplicadorJ2ArrastaComDeleteESetas() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            j2Pega();
            int x1 = l1().getX();
            int x2 = l2().getX();
            tecla("pretoControle", true);
            tecla("pretoEsquerda", true);
            ticks(10);
            tecla("pretoEsquerda", false);
            tecla("pretoControle", false);
            ticks(1);
            assertTrue(l2().getX() < x2, "o aplicador anda");
            assertEquals(x1 - x2, l1().getX() - l2().getX(), "a dupla anda junta");
            assertTrue(s().isInteracaoEstabelecida(), "arrastar nao rompe");
        });
    }

    @Test
    void receptorNaoArrastaOAplicador() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            j2Pega();
            int x2 = l2().getX();
            tecla("brancoControle", true);                  // receptor J1 tentando o "modo controle"
            tecla("brancoEsquerda", true);
            ticks(40);
            tecla("brancoEsquerda", false);
            tecla("brancoControle", false);
            ticks(1);
            assertEquals(x2, l2().getX(), "o aplicador nao foi levado");
            // 5L (pedido): antes "andar como receptor rompe" (fuga gratuita reprovada no Xbox).
            assertTrue(s().isInteracaoEstabelecida(), "andar como receptor so resiste: a pegada continua");
        });
    }

    @Test
    void arrastoAutomaticoContextualDoAplicadorJ1EJ2() throws Exception {
        MapaGamepad contextual = MapaGamepad.classico()
                .comEsquema(J1, EsquemaGamepad.CONTEXTUAL).comEsquema(J2, EsquemaGamepad.CONTEXTUAL);
        // J2 aplicador no Xbox (Contextual): RS cima pega, analogico esquerdo arrasta sem LB.
        comGamepads(contextual, J1, J2);
        naEdt(() -> {
            painel = dois(null, null);
            ticks(1);
            sim.definir("g2", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f));
            for (int i = 0; i < 300 && !s().podeTentarAcao(l2(), l1(), "pegada_1"); i++) {
                ticks(1);
            }
            sim.definir("g2", EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_Y, -1f));
            ticks(1);
            sim.definir("g2", EstadoGamepad.repouso());
            ticks(40);
            assertSame(l2(), s().getAplicadorInteracao(), "J2 pegou pelo RS");
            assertTrue(s().isInteracaoEstabelecida());
            int x1 = l1().getX();
            int x2 = l2().getX();
            sim.definir("g2", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f));
            ticks(10);
            sim.definir("g2", EstadoGamepad.repouso());
            ticks(1);
            assertTrue(l2().getX() < x2, "arrasto automatico do J2");
            assertEquals(x1 - x2, l1().getX() - l2().getX());
            assertTrue(s().isInteracaoEstabelecida());

            // O receptor J1 andando no analogico nao arrasta. 5L (pedido): e tambem nao rompe mais.
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f));
            int x2b = l2().getX();
            ticks(40);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertEquals(x2b, l2().getX(), "receptor nao leva o aplicador");
            assertTrue(s().isInteracaoEstabelecida(), "5L (pedido): antes assertFalse (fuga gratuita)");
        });
    }

    @Test
    void arrastoAutomaticoContextualDoAplicadorJ1() throws Exception {
        comGamepads(MapaGamepad.classico().comEsquema(J1, EsquemaGamepad.CONTEXTUAL), J1);
        naEdt(() -> {
            painel = dois(null, null);
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, 1f));
            for (int i = 0; i < 300 && !s().podeTentarAcao(l1(), l2(), "pegada_1"); i++) {
                ticks(1);
            }
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_Y, -1f));
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(40);
            assertSame(l1(), s().getAplicadorInteracao());
            int x1 = l1().getX();
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, 1f));
            ticks(10);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertTrue(l1().getX() > x1, "arrasto automatico do J1");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // =====================================================
    // 13. ENTRADAS SIMULTANEAS
    // =====================================================
    @Test
    void pegadasSimultaneasResolvemPeloJ1() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            aproximar("brancoDireita", l1(), l2());
            assertTrue(s().podeTentarAcao(l2(), l1(), "pegada_1"), "os dois no alcance");
            tecla("brancoPegada1", true);
            tecla("pretoPegada1", true);
            ticks(1);
            tecla("brancoPegada1", false);
            tecla("pretoPegada1", false);
            assertSame(l1(), s().getAplicadorInteracao(), "deterministico: o J1 vence o empate");
            assertSame(l2(), s().getReceptorInteracao());
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            assertSame(l1(), s().getAplicadorInteracao(), "nenhuma segunda pegada");
        });
    }

    @Test
    void desequilibrarERomperNoMesmoToqueValeOPapelReal() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            j2Pega();
            // J1 (receptor) aperta os dois juntos: vale o rompimento.
            tecla("brancoDesequilibrio", true);
            tecla("brancoRompimento", true);
            ticks(1);
            tecla("brancoDesequilibrio", false);
            tecla("brancoRompimento", false);
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, s().getUltimaDisputa().getTipo());
        });
        naEdt(() -> {
            painel = dois(null, null);
            j2Pega();
            // J2 (aplicador) aperta os dois juntos: vale o desequilibrio.
            tecla("pretoDesequilibrio", true);
            tecla("pretoRompimento", true);
            ticks(1);
            tecla("pretoDesequilibrio", false);
            tecla("pretoRompimento", false);
            assertEquals(TipoDisputa.DESEQUILIBRIO, s().getUltimaDisputa().getTipo());
            String msg = painel.mensagemFeedbackParaTestes();
            assertFalse(msg != null && msg.contains("ROMPIMENTO"),
                    "o rompimento do mesmo toque foi descartado (nada de aviso indevido): " + msg);
        });
    }

    // =====================================================
    // 14. ALCANCE, DURACAO E RESISTENCIA PRESERVADOS
    // =====================================================
    @Test
    void alcanceDuracaoEFirmezaIguaisParaOsDoisSlots() throws Exception {
        int[] duracao = new int[2];
        double[] firmeza = new double[2];
        naEdt(() -> {
            painel = dois(null, null);
            toque("pretoPegada1");                          // longe: fora do alcance
            assertFalse(s().estaEmInteracao(), "J2 longe nao pega");
            assertEquals(ResultadoAcao.INDISPONIVEL, s().getUltimoResultado());
            duracao[0] = j1Pega();
            firmeza[0] = s().getFirmeza();
        });
        naEdt(() -> {
            painel = dois(null, null);
            toque("brancoPegada1");
            assertFalse(s().estaEmInteracao(), "J1 longe nao pega");
            duracao[1] = j2Pega();
            firmeza[1] = s().getFirmeza();
        });
        assertEquals(duracao[0], duracao[1], "mesma duracao da entrada da PEGADA_1");
        assertEquals(firmeza[0], firmeza[1], 1e-9, "mesma firmeza inicial");
    }

    // =====================================================
    // 15 e 16. XBOX CLASSICO DO J2 (LB ARRASTA, OESTE PEGA)
    // =====================================================
    @Test
    void j2NoXboxClassicoPegaReforcaEArrastaComLb() throws Exception {
        comGamepads(MapaGamepad.classico(), J2);
        naEdt(() -> {
            painel = dois(null, null);
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.DPAD_ESQUERDA));
            for (int i = 0; i < 300 && !s().podeTentarAcao(l2(), l1(), "pegada_1"); i++) {
                ticks(1);
            }
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.OESTE));
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(40);
            assertSame(l2(), s().getAplicadorInteracao(), "X/Quadrado pega no Classico");
            assertTrue(s().isInteracaoEstabelecida());

            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.NORTE));
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertTrue(s().getRecargaReforcoRestante(l2()) > 0, "Y reforca");

            int x2 = l2().getX();
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.LB)
                    .com(BotaoGamepad.DPAD_ESQUERDA));
            ticks(10);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertTrue(l2().getX() < x2, "LB + direcao arrasta (Classico)");
            assertTrue(s().isInteracaoEstabelecida());

            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.LESTE));
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertEquals(TipoDisputa.DESEQUILIBRIO, s().getUltimaDisputa().getTipo(),
                    "B do aplicador desequilibra (nao rompe)");
        });
    }

    @Test
    void receptorJ1NoXboxClassicoRompeComB() throws Exception {
        comGamepads(MapaGamepad.classico(), J1);
        naEdt(() -> {
            painel = dois(null, null);
            ticks(1);
            j2Pega();                                       // J2 no teclado
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.LESTE));
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, s().getUltimaDisputa().getTipo(),
                    "B do receptor rompe");
        });
    }

    @Test
    void classicoSemLbContinuaRompendoAoAndar() throws Exception {
        comGamepads(MapaGamepad.classico(), J2);
        naEdt(() -> {
            painel = dois(null, null);
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.DPAD_ESQUERDA));
            for (int i = 0; i < 300 && !s().podeTentarAcao(l2(), l1(), "pegada_1"); i++) {
                ticks(1);
            }
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.OESTE));
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.DPAD_ESQUERDA));
            ticks(60);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertFalse(s().isInteracaoEstabelecida(), "no Classico, andar sem LB rompe (5H preservado)");
        });
    }

    // =====================================================
    // 17. REINICIO MANTEM O IRMAO ESCOLHIDO
    // =====================================================
    @Test
    void reinicioDoTreinoMantemOPretoNoControle() throws Exception {
        naEdt(() -> {
            painel = treino(oficial(CatalogoPersonagens.ID_PRETO), oficial(CatalogoPersonagens.ID_BRANCO));
            j1Pega();
            painel.reiniciarTreino();
            assertTrue(l1() instanceof IrmaoMiauPreto, "continua o Preto");
            assertTrue(l2() instanceof IrmaoMiauBranco);
            assertFalse(s().estaEmInteracao());
            assertEquals(ConfiguracaoTreino.X_JOGADOR_PADRAO, l1().getX());
            j1Pega();
            assertSame(l1(), s().getAplicadorInteracao());
        });
    }
}
