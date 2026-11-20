package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JButton;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.4: um destaque so nos menus (mouse x teclado/controle). */
class Destaque5Q4Test {

    @Test
    void focoMandaEMouseSoAcendeSemOutroBotaoComFoco() {
        JButton outro = new JButton("B");
        assertTrue(ArteMenus.destacado(true, false, null), "com foco: aceso");
        assertTrue(ArteMenus.destacado(false, true, null), "so o mouse, ninguem com foco: aceso");
        assertTrue(ArteMenus.destacado(false, true, new JLabel()), "foco fora de botoes: o mouse ainda acende");
        assertFalse(ArteMenus.destacado(false, true, outro), "o controle levou o foco para outro: apaga");
        assertFalse(ArteMenus.destacado(false, false, null));
    }

    @Test
    void mouseSoSegueBotoesVisiveisEHabilitadosDaJanela() {
        java.awt.Window janela = null;                         // testes headless: sem janela real
        JButton b = new JButton("A");
        assertFalse(NavegadorMenus.deveSeguirMouse(b, janela), "fora da tela: nao");
        assertFalse(NavegadorMenus.deveSeguirMouse(new JLabel("x"), janela), "rotulo nao recebe foco");
    }
}
