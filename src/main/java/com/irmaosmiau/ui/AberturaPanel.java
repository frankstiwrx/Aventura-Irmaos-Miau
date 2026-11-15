package com.irmaosmiau.ui;

import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.InfoGamepad;
import com.irmaosmiau.dispositivos.LeitorNavegacao;
import com.irmaosmiau.dispositivos.ProvedorGamepads;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Entrega 5M: tela de abertura. Patio do Dojo Miau (original), o letreiro
 * e os dois irmaos oficiais frente a frente, respirando (idle do proprio
 * renderizador). ENTER / ESPACO / A / START continuam para o menu, uma vez so:
 * um botao que ja estava apertado quando a tela abriu precisa ser solto antes.
 */
public final class AberturaPanel extends JPanel implements TelaNavegavel {

    private static final long serialVersionUID = 1L;
    static final int MS_POR_TICK = 16;
    /** Duracao da entrada do letreiro (ms). */
    static final int ENTRADA_MS = 500;

    private final transient Runnable aoContinuar;
    private final transient Supplier<ProvedorGamepads> provedor;
    private final transient IrmaoMiau branco = new IrmaoMiauBranco(240, 166, 1.0);
    private final transient IrmaoMiau preto = new IrmaoMiauPreto(470, 166, 1.0);
    private final transient Map<String, LeitorNavegacao> leitores = new HashMap<>();
    private final transient Map<String, Boolean> startAnterior = new HashMap<>();
    private final Timer timer = new Timer(MS_POR_TICK, e -> tick(MS_POR_TICK));
    private long tempoMs;
    private boolean concluida;

    public AberturaPanel(Runnable aoContinuar, Supplier<ProvedorGamepads> provedor) {
        this.aoContinuar = aoContinuar;
        this.provedor = provedor;
        setPreferredSize(new Dimension(800, 450));
        setFocusable(true);
        branco.setViradoParaDireita(true);
        preto.setViradoParaDireita(false);
        ArteMenus.apresentacao(branco);
        ArteMenus.apresentacao(preto);
        for (String k : new String[] {"ENTER", "SPACE"}) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + k), "continuar");
        }
        getActionMap().put("continuar", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                continuar();
            }
        });
    }

    /** O NavegadorMenus nao age aqui (a abertura le ENTER/A/START sozinha). */
    @Override
    public boolean aceitaNavegacao() {
        return false;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
        requestFocusInWindow();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    /** Um passo de tempo (timer da tela; tambem usado pelos testes). */
    void tick(long ms) {
        tempoMs += ms;
        branco.atualizarApresentacao();                  // 5M.3: pose fixa, so a piscada
        preto.atualizarApresentacao();
        lerGamepads();
        repaint();
    }

    private void lerGamepads() {
        ProvedorGamepads p = provedor == null ? null : provedor.get();
        if (p == null) {
            return;
        }
        for (InfoGamepad g : p.conectados()) {
            EstadoGamepad e = p.estado(g.chave()).orElse(null);
            LeitorNavegacao l = leitores.computeIfAbsent(g.chave(), k -> new LeitorNavegacao());
            boolean a = l.ler(e, tempoMs).contains(LeitorNavegacao.Comando.CONFIRMAR);
            boolean start = e != null && e.pressionado(BotaoGamepad.INICIAR);
            // START: so a borda, e quem ja estava segurando quando a tela abriu precisa soltar.
            boolean startAntes = startAnterior.getOrDefault(g.chave(), true);
            startAnterior.put(g.chave(), start);
            if (a || (start && !startAntes)) {
                continuar();
            }
        }
    }

    /** Vai para o menu (uma vez so). */
    void continuar() {
        if (concluida) {
            return;
        }
        concluida = true;
        timer.stop();
        if (aoContinuar != null) {
            aoContinuar.run();
        }
    }

    boolean isConcluida() {
        return concluida;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = ArteMenus.suave(g0);
        ArteMenus.desenharPatioDojo(g, getWidth(), getHeight(), tempoMs / 1000.0);
        ArteMenus.sombra(g, branco, 420);
        ArteMenus.sombra(g, preto, 420);
        ArteMenus.desenharLutador(g, branco);
        ArteMenus.desenharLutador(g, preto);

        // Letreiro: desce no inicio e oscila de leve.
        double t = Math.min(1.0, tempoMs / (double) ENTRADA_MS);
        int y = (int) (-30 + (88 + 30) * (1 - Math.pow(1 - t, 3)) + Math.sin(tempoMs / 600.0) * 2);
        g.setColor(new Color(10, 30, 80, 140));
        g.fillRoundRect(110, y - 46, 580, 62, 18, 18);
        ArteMenus.letreiro(g, "AVENTURA DOS IRMÃOS MIAU", 400, y, ArteMenus.LETREIRO, ArteMenus.AMARELO);

        if ((tempoMs / 500) % 2 == 0 && tempoMs > ENTRADA_MS) {
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.setColor(new Color(10, 30, 80, 170));
            g.fillRoundRect(250, 416, 300, 26, 12, 12);
            g.setColor(ArteMenus.BRANCO);
            ArteMenus.centralizado(g, "PRESSIONE ENTER / A / START", 400, 435);
        }
        g.dispose();
    }

    long getTempoMs() {
        return tempoMs;
    }

    IrmaoMiau getBranco() {
        return branco;
    }

    IrmaoMiau getPreto() {
        return preto;
    }

    /** Para testes: aciona a tecla ligada a "continuar". */
    void pressionarEnterParaTestes() {
        getActionMap().get(getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke("pressed ENTER"))).actionPerformed(null);
    }
}
