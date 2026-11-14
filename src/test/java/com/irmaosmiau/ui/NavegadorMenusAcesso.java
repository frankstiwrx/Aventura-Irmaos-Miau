package com.irmaosmiau.ui;

import com.irmaosmiau.dispositivos.LeitorNavegacao;

import java.awt.Component;
import java.awt.Container;

import javax.swing.AbstractButton;

/** Acesso de teste ao NavegadorMenus a partir de outros pacotes. */
public final class NavegadorMenusAcesso {

    private NavegadorMenusAcesso() {
    }

    public static Component executar(Container alvo, Component foco, LeitorNavegacao.Comando c) {
        return NavegadorMenus.executar(alvo, foco, c);
    }

    public static AbstractButton botaoVoltar(Container alvo) {
        return NavegadorMenus.botaoVoltar(alvo);
    }
}
