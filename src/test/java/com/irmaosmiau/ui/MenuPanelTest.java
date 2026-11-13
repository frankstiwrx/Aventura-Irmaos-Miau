package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void construtorOriginalContinuaSemTreino() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MenuPanel menu = new MenuPanel(() -> { });
            assertNotNull(botao(menu, "COMEÇAR"));
            assertNull(botao(menu, "TREINO"));
        });
    }
}
