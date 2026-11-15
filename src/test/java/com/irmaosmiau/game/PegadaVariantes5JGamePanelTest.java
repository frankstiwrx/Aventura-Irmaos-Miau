package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.CamadaPoseCombate;
import com.irmaosmiau.combate.SelecaoAnimacaoConjunta;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5J (testes 4, 7, 8, 11 e 21) no GamePanel real. */
class PegadaVariantes5JGamePanelTest {

    private static final String PRETO = "pegada_1_entrada_preto";

    private GamePanel painel;
    private ProvedorGamepadSimulado sim;

    @AfterEach
    void restaurar() {
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
    }

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

    private SistemaCombate s() {
        return painel.getSistemaCombate();
    }

    private GamePanel dois(EscolhaLutador j1, EscolhaLutador j2) {
        GamePanel g = new GamePanel(TipoPersonagem.AMBOS, j1, j2, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    private void aproximar(String andar, IrmaoMiau quem, IrmaoMiau alvo) {
        tecla(andar, true);
        for (int i = 0; i < 300 && !s().podeTentarAcao(quem, alvo, "pegada_1"); i++) {
            ticks(1);
        }
        tecla(andar, false);
        assertTrue(s().podeTentarAcao(quem, alvo, "pegada_1"));
    }

    private static boolean[][] mascara(IrmaoMiau... ls) {
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        for (IrmaoMiau l : ls) {
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
            for (boolean[] l : m) {
                if (l[x]) {
                    return x;
                }
            }
        }
        return -1;
    }

    private static double espelhada(boolean[][] a, boolean[][] b) {
        boolean[][] e = new boolean[450][800];
        for (int y = 0; y < 450; y++) {
            for (int x = 0; x < 800; x++) {
                e[y][799 - x] = b[y][x];
            }
        }
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
            melhor = Math.max(melhor, (double) inter / uniao);
        }
        return melhor;
    }

    // ---------------- 4 e 7. espelho independente dos papeis; geometria de contato
    @Test
    void pretoAplicandoDaEsquerdaEOEspelhoDoPretoAplicandoDaDireita() throws Exception {
        int[] marcos = {1, 12, 24, 40};
        boolean[][][] dir = new boolean[marcos.length][][];
        boolean[][][] esq = new boolean[marcos.length][][];
        int[] dist = new int[2];
        naEdt(() -> {
            // Preto (J2) aplica vindo da DIREITA, olhando para a esquerda.
            painel = dois(null, null);
            aproximar("pretoEsquerda", painel.getLutador2(), painel.getLutador1());
            int antes = painel.getLutador2().getX() - painel.getLutador1().getX();
            tecla("pretoPegada1", true);
            for (int i = 0, t = 0; i < marcos.length; i++) {
                for (; t < marcos[i]; t++) {
                    ticks(1);
                    tecla("pretoPegada1", false);
                }
                dir[i] = mascara(painel.getLutador1(), painel.getLutador2());
            }
            assertTrue(s().isInteracaoEstabelecida());
            assertEquals(PRETO, s().getSelecaoAnimacao().idTrilhaAplicador());
            assertEquals(antes, painel.getLutador2().getX() - painel.getLutador1().getX(),
                    "contato: a distancia fisica nao muda (sem teletransporte)");
            dist[0] = antes;
        });
        naEdt(() -> {
            // Preto (J1) aplica vindo da ESQUERDA, olhando para a direita, no Branco (J2).
            painel = dois(oficial(CatalogoPersonagens.ID_PRETO), oficial(CatalogoPersonagens.ID_BRANCO));
            aproximar("brancoDireita", painel.getLutador1(), painel.getLutador2());
            dist[1] = painel.getLutador2().getX() - painel.getLutador1().getX();
            tecla("brancoPegada1", true);
            for (int i = 0, t = 0; i < marcos.length; i++) {
                for (; t < marcos[i]; t++) {
                    ticks(1);
                    tecla("brancoPegada1", false);
                }
                esq[i] = mascara(painel.getLutador1(), painel.getLutador2());
            }
            assertTrue(painel.getLutador1() instanceof IrmaoMiauPreto, "nenhuma troca de identidade");
            assertTrue(painel.getLutador2() instanceof IrmaoMiauBranco);
            assertSame(painel.getLutador1(), s().getAplicadorInteracao());
            assertEquals(SelecaoAnimacaoConjunta.Tipo.AUTORAL, s().getSelecaoAnimacao().tipo());
            assertEquals(PRETO, s().getSelecaoAnimacao().idTrilhaAplicador(), "mesma variante dos dois lados");
        });
        assertEquals(dist[0], dist[1], "mesma distancia de contato");
        for (int i = 0; i < marcos.length; i++) {
            double iou = espelhada(dir[i], esq[i]);
            assertTrue(iou > 0.95, "quadro " + i + ": espelho correto (IoU " + iou + ")");
            assertTrue(espelhada(dir[i], dir[i]) < 0.85, "a cena nao e simetrica: o teste distingue papeis trocados");
        }
    }

    // ---------------- 8. entrada uma vez, pose sustentada
    @Test
    void entradaUmaVezEPoseSustentada() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            aproximar("pretoEsquerda", painel.getLutador2(), painel.getLutador1());
            tecla("pretoPegada1", true);
            ticks(1);
            tecla("pretoPegada1", false);
            assertTrue(s().isAnimacaoCombateAtiva(), "entrada");
            ticks(40);
            assertFalse(s().isAnimacaoCombateAtiva());
            int[] pose = painel.getLutador2().copiarPoseCombateExterna();
            for (int i = 0; i < 120; i++) {
                ticks(1);
                assertFalse(s().isAnimacaoCombateAtiva(), "a entrada nao reexecuta (tick " + i + ")");
            }
            assertTrue(s().isInteracaoEstabelecida());
            assertArrayEquals(pose, painel.getLutador2().copiarPoseCombateExterna(), "pose de contato mantida");
        });
    }

    // ---------------- 11. arrasto nos dois sentidos, pelos dois slots
    @Test
    void passosNoArrastoParaOsDoisLadosPelosDoisSlots() throws Exception {
        for (String[] caso : new String[][] {
            {"brancoPegada1", "brancoControle", "brancoDireita", "1"},
            {"brancoPegada1", "brancoControle", "brancoEsquerda", "1"},
            {"pretoPegada1", "pretoControle", "pretoEsquerda", "2"},
            {"pretoPegada1", "pretoControle", "pretoDireita", "2"}}) {
            naEdt(() -> {
                painel = dois(null, null);
                if (caso[3].equals("1")) {
                    aproximar("brancoDireita", painel.getLutador1(), painel.getLutador2());
                } else {
                    aproximar("pretoEsquerda", painel.getLutador2(), painel.getLutador1());
                }
                tecla(caso[0], true);
                ticks(1);
                tecla(caso[0], false);
                ticks(40);
                IrmaoMiau apl = s().getAplicadorInteracao();
                IrmaoMiau rec = s().getReceptorInteracao();
                CamadaPoseCombate c = painel.getAnimador().getCamada();
                int[] base = apl.copiarPoseCombateExterna();
                int x = apl.getX();
                tecla(caso[1], true);
                tecla(caso[2], true);
                boolean viuPasso = false;
                for (int i = 0; i < 12; i++) {
                    ticks(1);
                    viuPasso |= c.faseArrasto(apl) == 1 || c.faseArrasto(apl) == 3;
                    assertEquals(c.faseArrasto(apl), c.faseArrasto(rec), "os dois andam no mesmo passo");
                }
                tecla(caso[2], false);
                tecla(caso[1], false);
                ticks(1);
                String nome = String.join("+", caso);
                assertTrue(viuPasso, nome);
                assertTrue(apl.getX() != x, nome + ": a dupla andou");
                assertEquals(-1, c.faseArrasto(apl), nome + ": parou");
                assertArrayEquals(base, apl.copiarPoseCombateExterna(), nome + ": sem deslizamento permanente da pose");
                assertTrue(s().isInteracaoEstabelecida(), nome);
            });
        }
    }

    @Test
    void passosNoArrastoAutomaticoDoContextual() throws Exception {
        sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID0", "Xbox 1");
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_2, TipoDispositivo.GAMEPAD)));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(
                MapaGamepad.padrao().comEsquema(SlotJogador.JOGADOR_2, EsquemaGamepad.CONTEXTUAL)));
        naEdt(() -> {
            painel = dois(null, null);
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f));
            for (int i = 0; i < 300 && !s().podeTentarAcao(painel.getLutador2(), painel.getLutador1(), "pegada_1"); i++) {
                ticks(1);
            }
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_Y, -1f));
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f));
            ticks(6);
            assertTrue(painel.getAnimador().getCamada().faseArrasto(painel.getLutador2()) >= 0, "passos no Contextual");
            assertTrue(painel.linhasTecnicas().get(3).contains("passos"), painel.linhasTecnicas().toString());
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertEquals(-1, painel.getAnimador().getCamada().faseArrasto(painel.getLutador2()));
        });
    }

    // ---------------- 21. pausa e perda de foco durante as animacoes transitorias
    @Test
    void pausaCongelaEPerdaDeFocoEncerraOPassoSemSobra() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            aproximar("brancoDireita", painel.getLutador1(), painel.getLutador2());
            tecla("brancoPegada1", true);
            ticks(1);
            tecla("brancoPegada1", false);
            ticks(40);
            IrmaoMiau apl = painel.getLutador1();
            int[] base = apl.copiarPoseCombateExterna();
            tecla("brancoControle", true);
            tecla("brancoDireita", true);
            ticks(5);
            int[] noPasso = apl.copiarPoseCombateExterna();
            int x = apl.getX();
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            ticks(30);
            assertArrayEquals(noPasso, apl.copiarPoseCombateExterna(), "pausado: nada anda");
            assertEquals(x, apl.getX());
            painel.getActionMap().get("alternarPausa").actionPerformed(null);
            painel.simularPerdaDeFocoParaTestes();          // solta as teclas
            ticks(2);
            assertArrayEquals(base, apl.copiarPoseCombateExterna(), "sem teclas: pose da pegada exata");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    @Test
    void desequilibrioMostraReacaoDoReceptorNoHud() throws Exception {
        naEdt(() -> {
            painel = dois(null, null);
            aproximar("brancoDireita", painel.getLutador1(), painel.getLutador2());
            tecla("brancoPegada1", true);
            ticks(1);
            tecla("brancoPegada1", false);
            ticks(40);
            tecla("brancoDesequilibrio", true);
            ticks(1);
            tecla("brancoDesequilibrio", false);
            ticks(3);
            assertTrue(s().getUltimaDisputa().isSucessoDoAtaque(), "Branco oficial desequilibra o Preto (5B)");
            assertTrue(painel.getAnimador().getCamada().intensidadeReacao(painel.getLutador2()) > 0);
            assertEquals(0.0, painel.getAnimador().getCamada().intensidadeReacao(painel.getLutador1()));
            assertTrue(painel.linhasTecnicas().get(3).contains("J2 Miau Preto reagindo")
                    || painel.linhasTecnicas().get(3).contains("reagindo"), painel.linhasTecnicas().toString());
            assertTrue(painel.linhasTecnicas().get(0).startsWith("POSTURA J1: EM_PE"));
        });
    }
}
