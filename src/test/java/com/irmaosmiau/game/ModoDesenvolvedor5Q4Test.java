package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.4: Modo Desenvolvedor (recursos sem desgaste, isolamento, F5/F4, opcoes da pausa). */
class ModoDesenvolvedor5Q4Test extends Base5OGamePanel {

    private void gastar(IrmaoMiau l) {
        l.gastarFolegoEsforco(30, "teste");
        l.gastarCondicionamentoEsforco(30, "teste");
    }

    private ModoDesenvolvedor dev() {
        ModoDesenvolvedor d = new ModoDesenvolvedor();
        painel.configurarModoDesenvolvedor(d);
        return d;
    }

    @Test
    void recursosInfinitosNosDoisLutadoresNoMaximoNormal() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            double maxF1 = j1().getFolegoAtual();
            double maxC2 = j2().getCondicionamentoAtual();
            dev();
            for (int i = 0; i < 50; i++) {
                gastar(j1());
                gastar(j2());
                ticks(1);
            }
            assertEquals(maxF1, j1().getFolegoAtual(), 1e-9, "folego fica no maximo normal (sem numero gigante)");
            assertEquals(maxC2, j2().getCondicionamentoAtual(), 1e-9);
            assertTrue(j1().isFolegoInfinito() && j2().isCondicionamentoInfinito());
            assertFalse(j1().isSemFolego());
        });
    }

    @Test
    void foraDoModoOConsumoENormal() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            double f = j1().getFolegoAtual();
            double c = j2().getCondicionamentoAtual();
            gastar(j1());
            gastar(j2());
            assertTrue(j1().getFolegoAtual() < f);
            assertTrue(j2().getCondicionamentoAtual() < c);
            assertNull(painel.getModoDesenvolvedor());
            assertFalse(j1().isFolegoInfinito() || j1().isCondicionamentoInfinito());
        });
    }

    @Test
    void treinoIgnoraOModoEPartidaNovaNasceSemEle() throws Exception {
        naEdt(() -> {
            GamePanel treino = new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { });
            treino.pararLoopParaTestes();
            treino.configurarModoDesenvolvedor(new ModoDesenvolvedor());
            assertNull(treino.getModoDesenvolvedor(), "o laboratorio do treino continua separado");
            IrmaoMiau l = treino.getLutador1();
            double f = l.getFolegoAtual();
            gastar(l);
            assertTrue(l.getFolegoAtual() < f);

            novo(InicioLuta.EM_PE);
            dev();
            novo(InicioLuta.EM_PE);                              // outra partida: nada global sobra
            assertNull(painel.getModoDesenvolvedor());
            assertFalse(j1().isFolegoInfinito());
            assertFalse(painel.botoesDesenvolvedor().get(0).isVisible());
        });
    }

    @Test
    void opcoesDaPausaAlternamERestauram() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            ModoDesenvolvedor d = dev();
            List<JButton> b = painel.botoesDesenvolvedor();
            assertEquals(3, b.size());
            assertTrue(b.stream().allMatch(JButton::isVisible));
            assertTrue(b.get(0).getText().endsWith("SIM"));

            b.get(0).doClick();                                  // folego normal
            assertFalse(d.isFolegoInfinito());
            assertTrue(b.get(0).getText().endsWith("NÃO"));
            assertTrue(d.selo().contains("FÔLEGO NORMAL"));
            double f = j1().getFolegoAtual();
            double c = j1().getCondicionamentoAtual();
            gastar(j1());
            ticks(1);
            assertTrue(j1().getFolegoAtual() < f, "folego volta a gastar");
            assertEquals(c, j1().getCondicionamentoAtual(), 1e-9, "condicionamento segue infinito");

            b.get(1).doClick();                                  // condicionamento normal
            gastar(j1());
            assertTrue(j1().getCondicionamentoAtual() < c);

            b.get(2).doClick();                                  // restaurar
            assertEquals(f, j1().getFolegoAtual(), 1e-9);
            assertEquals(c, j1().getCondicionamentoAtual(), 1e-9);
        });
    }

    @Test
    void f5RecomecaPelaSessaoEF4GuardaOCenario() throws Exception {
        AtomicInteger reinicios = new AtomicInteger();
        naEdt(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { },
                    reinicios::incrementAndGet);
            painel.pararLoopParaTestes();
            ModoDesenvolvedor d = dev();
            String antes = painel.idCenarioParaTestes();
            painel.getActionMap().get("proximoCenario").actionPerformed(null);
            assertNotEquals(antes, painel.idCenarioParaTestes());
            assertEquals(painel.idCenarioParaTestes(), d.getCenario(), "o F5 recomeca no cenario do F4");
            assertEquals(d.getCenario(), d.cenarioOu("dojo"));
            painel.getActionMap().get("reiniciarTreino").actionPerformed(null);
            assertEquals(1, reinicios.get(), "F5 recria a luta com a mesma sessao (o Main repassa o objeto)");
            assertSame(d, painel.getModoDesenvolvedor());
        });
    }

    @Test
    void seloPermanenteDesenhado() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            BufferedImage sem = pintar();
            dev();
            BufferedImage com = pintar();
            boolean diferente = false;
            for (int y = 62; y < 78 && !diferente; y++) {
                for (int x = 12; x < 200 && !diferente; x++) {
                    diferente = sem.getRGB(x, y) != com.getRGB(x, y);
                }
            }
            assertTrue(diferente, "o selo vermelho aparece no canto");
        });
    }

    private BufferedImage pintar() {
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        painel.setSize(800, 450);
        painel.paint(g);
        g.dispose();
        return img;
    }
}
