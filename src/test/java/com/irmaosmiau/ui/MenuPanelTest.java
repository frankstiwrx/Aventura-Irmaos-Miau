package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class MenuPanelTest {

    private static JButton botao(Container raiz, String texto) {
        for (Component c : raiz.getComponents()) {
            if (c instanceof JButton && texto.equals(((JButton) c).getText())) {
                return (JButton) c;
            }
            if (c instanceof Container) {
                JButton b = botao((Container) c, texto);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    @Test
    void menuTemComecarETreinoComSeusDestinos() throws Exception {
        AtomicInteger comecar = new AtomicInteger();
        AtomicInteger treino = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            MenuPanel menu = new MenuPanel(comecar::incrementAndGet, treino::incrementAndGet);
            JButton bComecar = botao(menu, "COMEÇAR");
            JButton bTreino = botao(menu, "TREINO");
            assertNotNull(bComecar);
            assertNotNull(bTreino);

            bTreino.doClick(0);
            bComecar.doClick(0);
        });

        assertEquals(1, treino.get());
        assertEquals(1, comecar.get());
    }

    @Test
    void menuCompletoTemConfiguracoesEPerfisECabeNaTela() throws Exception {
        AtomicInteger[] cliques = {new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger()};

        SwingUtilities.invokeAndWait(() -> {
            MenuPanel menu = new MenuPanel(cliques[0]::incrementAndGet, cliques[1]::incrementAndGet,
                    cliques[2]::incrementAndGet, cliques[3]::incrementAndGet);
            menu.setSize(800, 450);
            layoutRecursivo(menu);
            String[] textos = {"COMEÇAR", "TREINO", "CONFIGURAÇÕES", "PERSONAGENS PERSONALIZADOS"};
            for (int i = 0; i < textos.length; i++) {
                JButton b = botao(menu, textos[i]);
                assertNotNull(b, textos[i]);
                b.doClick(0);
                java.awt.Point p = SwingUtilities.convertPoint(b.getParent(), b.getLocation(), menu);
                assertTrue(p.y + b.getHeight() <= 450 && p.y >= 0, textos[i] + " fora da tela: " + p);
                int largura = b.getFontMetrics(b.getFont()).stringWidth(textos[i]);
                assertTrue(largura + 20 <= b.getWidth(), textos[i] + " cortado");
            }
        });

        for (AtomicInteger c : cliques) {
            assertEquals(1, c.get());
        }
    }

    private static void layoutRecursivo(Container c) {
        c.doLayout();
        for (Component filho : c.getComponents()) {
            if (filho instanceof Container) {
                layoutRecursivo((Container) filho);
            }
        }
    }

    @Test
    void construtorOriginalContinuaSemTreino() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MenuPanel menu = new MenuPanel(() -> { });
            assertNotNull(botao(menu, "COMEÇAR"));
            assertNull(botao(menu, "TREINO"));
        });
    }
}
