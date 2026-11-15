package com.irmaosmiau.game;

import com.irmaosmiau.animacao.VersaoMovimentos;
import com.irmaosmiau.combate.ModoAnimacoesPegada;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;

/** Entrega 5O: utilitarios comuns dos testes no GamePanel. */
abstract class Base5OGamePanel {

    protected GamePanel painel;

    @AfterEach
    void restaurarGlobais() {
        ModoAnimacoesPegada.restaurarPadrao();
        VersaoMovimentos.restaurarPadrao();
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
    }

    protected static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    protected void novo(InicioLuta inicio) {
        painel = new GamePanel(TipoPersonagem.AMBOS, null, null, inicio, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        ticks(2);
    }

    protected void tecla(String nome, boolean p) {
        painel.getActionMap().get(nome + (p ? "Pressionada" : "Solta")).actionPerformed(null);
    }

    protected void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    protected void toque(String nome) {
        tecla(nome, true);
        ticks(1);
        tecla(nome, false);
        ticks(1);
    }

    protected SistemaCombate s() {
        return painel.getSistemaCombate();
    }

    protected IrmaoMiau j1() {
        return painel.getLutador1();
    }

    protected IrmaoMiau j2() {
        return painel.getLutador2();
    }

    /** J1 anda ate poder tentar a pegada n e pega (fica estabelecida). */
    protected void j1Pega(int n) {
        tecla("brancoDireita", true);
        for (int i = 0; i < 400 && !s().podeTentarAcao(j1(), j2(), "pegada_" + n); i++) {
            ticks(1);
        }
        tecla("brancoDireita", false);
        ticks(2);
        toque("brancoPegada" + n);
        ticks(40);
    }
}
