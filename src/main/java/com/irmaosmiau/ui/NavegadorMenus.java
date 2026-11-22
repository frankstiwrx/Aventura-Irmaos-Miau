package com.irmaosmiau.ui;

import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.InfoGamepad;
import com.irmaosmiau.dispositivos.LeitorNavegacao;
import com.irmaosmiau.dispositivos.ProvedorGamepads;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;

/**
 * Entrega 5I (complemento): navegar em TODOS os menus sem mouse.
 *
 * Gamepad (qualquer um conectado): D-pad ou analogico esquerdo movem o foco,
 * A confirma, B volta (ver {@link LeitorNavegacao}: zona morta, repeticao
 * controlada e nada dispara com um botao que ainda estava segurado da tela
 * anterior). Teclado: setas movem, ENTER confirma, ESC volta. O mouse
 * continua funcionando como antes.
 *
 * - Ordem do foco: de cima para baixo, da esquerda para a direita.
 * - Esquerda/direita mudam o valor de combos e sliders; em botoes, movem.
 * - "Voltar" = o botao visivel marcado com {@link #marcarVoltar}; em dialogos,
 *   fecha o dialogo (o mesmo que o X: cancelar).
 * - O foco atual ganha uma borda de destaque.
 * - Telas {@link TelaNavegavel} decidem quando o navegador age (no combate,
 *   so com o jogo pausado; nos controles, nunca durante a captura).
 */
public final class NavegadorMenus {

    /** Client property: o botao e o "voltar" da tela. */
    public static final String PROPRIEDADE_VOLTAR = "irmaosmiau.navegacao.voltar";
    private static final String BORDA_ORIGINAL = "irmaosmiau.navegacao.bordaOriginal";
    static final Color COR_FOCO = new Color(255, 196, 40);
    static final int INTERVALO_MS = 16;

    private final JFrame janela;
    private final Supplier<ProvedorGamepads> provedor;
    private final LongSupplier relogio;
    private final Map<String, LeitorNavegacao> leitores = new HashMap<>();
    private final Timer timer;
    private Object telaAnterior;
    private boolean aceitavaAntes;

    private final transient KeyEventDispatcher teclado = this::tecla;
    // 5Q.4: o mouse tambem move o FOCO (um destaque so, venha do mouse, do teclado ou do controle)
    private final transient java.awt.event.AWTEventListener mouse = this::mouseEntrou;

    private void mouseEntrou(java.awt.AWTEvent e) {
        if (e.getID() == java.awt.event.MouseEvent.MOUSE_ENTERED && e.getSource() instanceof Component c
                && deveSeguirMouse(c, janela)) {
            c.requestFocusInWindow();
        }
    }
    private final transient PropertyChangeListener destaque = e -> trocarDestaque(
            (Component) e.getOldValue(), (Component) e.getNewValue());

    public NavegadorMenus(JFrame janela, Supplier<ProvedorGamepads> provedor, LongSupplier relogio) {
        this.janela = janela;
        this.provedor = provedor;
        this.relogio = relogio;
        this.timer = new Timer(INTERVALO_MS, e -> lerGamepads());
    }

    /** Marca o botao "voltar" da tela (B no gamepad, ESC no teclado). */
    public static <T extends AbstractButton> T marcarVoltar(T botao) {
        botao.putClientProperty(PROPRIEDADE_VOLTAR, Boolean.TRUE);
        return botao;
    }

    public void instalar() {
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher(teclado);
        kfm.addPropertyChangeListener("permanentFocusOwner", destaque);
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(mouse, java.awt.AWTEvent.MOUSE_EVENT_MASK);
        timer.start();
    }

    public void desinstalar() {
        timer.stop();
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.removeKeyEventDispatcher(teclado);
        kfm.removePropertyChangeListener("permanentFocusOwner", destaque);
        java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(mouse);
    }

    /**
     * 5Q.4: o mouse sobre um botao/combo/slider navegavel desta janela (ou de um dialogo dela)
     * leva o foco para ele: o destaque do mouse e o do teclado/controle sao o mesmo.
     */
    static boolean deveSeguirMouse(Component c, Window janela) {
        if (!(c instanceof AbstractButton || c instanceof JComboBox || c instanceof JSlider)
                || !c.isEnabled() || !c.isFocusable() || !c.isShowing() || c.isFocusOwner()) {
            return false;
        }
        Window w = SwingUtilities.getWindowAncestor(c);
        return w != null && (w == janela || w.getOwner() == janela);
    }

    // =====================================================
    // GAMEPAD
    // =====================================================
    /** Um passo do timer (tambem usado pelos testes). */
    void lerGamepads() {
        Container alvo = alvo();
        boolean aceita = alvo != null;
        Object tela = alvo == null ? null : alvo;
        if (tela != telaAnterior || aceita != aceitavaAntes) {
            leitores.values().forEach(LeitorNavegacao::desarmar);   // nada passa de uma tela para outra
            telaAnterior = tela;
            aceitavaAntes = aceita;
        }
        ProvedorGamepads p = provedor.get();
        if (p == null) {
            return;
        }
        Set<String> vistos = new HashSet<>();
        long agora = relogio.getAsLong();
        for (InfoGamepad g : p.conectados()) {
            vistos.add(g.chave());
            LeitorNavegacao leitor = leitores.computeIfAbsent(g.chave(), k -> new LeitorNavegacao());
            EstadoGamepad estado = p.estado(g.chave()).orElse(null);
            if (!aceita) {
                leitor.ler(estado, agora);   // mantem as bordas atualizadas
                leitor.desarmar();
                continue;
            }
            for (LeitorNavegacao.Comando c : leitor.ler(estado, agora)) {
                aplicar(c);
                Container depois = alvo();
                if (depois != alvo) {
                    break;                        // a tela mudou: o resto do toque nao vale
                }
            }
        }
        leitores.keySet().retainAll(vistos);
    }

    /** Onde os comandos agem agora (null = em lugar nenhum). */
    Container alvo() {
        Window ativa = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (ativa instanceof JDialog d && d.getOwner() == janela) {
            return d.getContentPane();
        }
        if (ativa != janela && ativa != null) {
            return null;
        }
        Container c = janela.getContentPane();
        if (c instanceof TelaNavegavel t && !t.aceitaNavegacao()) {
            return null;
        }
        return c;
    }

    void aplicar(LeitorNavegacao.Comando c) {
        Container alvo = alvo();
        if (alvo == null) {
            return;
        }
        Component foco = focoDentro(alvo);
        Window w = SwingUtilities.getWindowAncestor(alvo);
        if (c == LeitorNavegacao.Comando.VOLTAR && w instanceof JDialog d) {
            d.dispatchEvent(new WindowEvent(d, WindowEvent.WINDOW_CLOSING));   // = cancelar
            return;
        }
        Component destino = executar(alvo, foco, c);
        if (destino != null) {
            destino.requestFocusInWindow();
        }
    }

    /**
     * Aplica o comando a tela (cliques e valores) e devolve o componente que
     * deve receber o foco, ou null. Sem depender de janela ativa (testavel).
     */
    static Component executar(Container alvo, Component foco, LeitorNavegacao.Comando c) {
        switch (c) {
            case ANTERIOR:
                return vizinho(alvo, foco, -1);
            case PROXIMO:
                return vizinho(alvo, foco, 1);
            case ESQUERDA:
                return ajustar(foco, -1) ? null : vizinho(alvo, foco, -1);
            case DIREITA:
                return ajustar(foco, 1) ? null : vizinho(alvo, foco, 1);
            case CONFIRMAR:
                if (foco instanceof AbstractButton b) {
                    b.doClick(0);
                    return null;
                }
                return foco == null ? vizinho(alvo, null, 1) : null;
            case PERFIS:
                return null;                                   // so a selecao de lutadores usa
            case VOLTAR:
            default:
                AbstractButton v = botaoVoltar(alvo);
                if (v != null) {
                    v.doClick(0);
                }
                return null;
        }
    }

    /** O botao "voltar" visivel da tela, ou null. */
    static AbstractButton botaoVoltar(Container alvo) {
        for (Component c : navegaveis(alvo, true)) {
            if (c instanceof AbstractButton b && Boolean.TRUE.equals(b.getClientProperty(PROPRIEDADE_VOLTAR))) {
                return b;
            }
        }
        return null;
    }

    // =====================================================
    // TECLADO
    // =====================================================
    private boolean tecla(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED || e.getModifiersEx() != 0) {
            return false;
        }
        Container alvo = alvo();
        if (alvo == null) {
            return false;
        }
        Component foco = focoDentro(alvo);
        if (foco instanceof JTextComponent || (foco instanceof JComboBox<?> cb && cb.isPopupVisible())) {
            return false;                          // texto e lista aberta usam as setas/ENTER deles
        }
        LeitorNavegacao.Comando c = switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> LeitorNavegacao.Comando.ANTERIOR;
            case KeyEvent.VK_DOWN -> LeitorNavegacao.Comando.PROXIMO;
            case KeyEvent.VK_LEFT -> LeitorNavegacao.Comando.ESQUERDA;
            case KeyEvent.VK_RIGHT -> LeitorNavegacao.Comando.DIREITA;
            case KeyEvent.VK_ENTER -> LeitorNavegacao.Comando.CONFIRMAR;
            case KeyEvent.VK_ESCAPE -> escVolta(alvo) ? LeitorNavegacao.Comando.VOLTAR : null;
            default -> null;
        };
        if (c == null) {
            return false;
        }
        aplicar(c);
        e.consume();
        return true;
    }

    private boolean escVolta(Container alvo) {
        if (SwingUtilities.getWindowAncestor(alvo) instanceof JDialog) {
            return false;                          // o proprio dialogo trata o ESC
        }
        return !(alvo instanceof TelaNavegavel t) || t.escVolta();
    }

    // =====================================================
    // FOCO
    // =====================================================
    private static Component focoDentro(Container alvo) {
        Component f = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return f != null && SwingUtilities.isDescendingFrom(f, alvo) && f != alvo ? f : null;
    }

    static Component vizinho(Container alvo, Component foco, int passo) {
        List<Component> lista = navegaveis(alvo, false);
        if (lista.isEmpty()) {
            return null;
        }
        int i = foco == null ? -1 : lista.indexOf(foco);
        int proximo = i < 0 ? (passo > 0 ? 0 : lista.size() - 1) : Math.floorMod(i + passo, lista.size());
        return lista.get(proximo);
    }

    /** Combos e sliders: esquerda/direita mudam o valor. */
    private static boolean ajustar(Component foco, int passo) {
        if (foco instanceof JComboBox<?> cb && cb.getItemCount() > 0) {
            cb.setSelectedIndex(Math.floorMod(cb.getSelectedIndex() + passo, cb.getItemCount()));
            return true;
        }
        if (foco instanceof JSlider s) {
            int delta = Math.max(1, (s.getMaximum() - s.getMinimum()) / 20);
            s.setValue(s.getValue() + passo * delta);
            return true;
        }
        return false;
    }

    /** Componentes que recebem foco, na ordem de leitura (linhas de cima para baixo). */
    static List<Component> navegaveis(Container alvo, boolean incluirSemFoco) {
        List<Component> l = new ArrayList<>();
        coletar(alvo, l, incluirSemFoco);
        Map<Component, Point> pos = new HashMap<>();
        for (Component c : l) {
            pos.put(c, SwingUtilities.convertPoint(c.getParent(), c.getLocation(), alvo));
        }
        l.sort(Comparator.<Component>comparingInt(c -> pos.get(c).y / 12).thenComparingInt(c -> pos.get(c).x));
        return l;
    }

    private static void coletar(Container c, List<Component> l, boolean incluirSemFoco) {
        for (Component f : c.getComponents()) {
            if (!f.isVisible()) {
                continue;
            }
            boolean util = f instanceof AbstractButton || f instanceof JComboBox || f instanceof JSlider
                    || f instanceof JTextComponent tc && tc.isEditable();
            if (util && f.isEnabled() && (incluirSemFoco || f.isFocusable())) {
                l.add(f);
            }
            if (f instanceof Container sub && !(f instanceof JComboBox) && !(f instanceof AbstractButton)) {
                coletar(sub, l, incluirSemFoco);
            }
        }
    }

    // =====================================================
    // DESTAQUE DO FOCO
    // =====================================================
    private void trocarDestaque(Component antes, Component depois) {
        // 5Q.4: quem estava aceso so pelo mouse apaga quando o foco vai para outro (repinta a tela)
        Window w = depois == null ? null : SwingUtilities.getWindowAncestor(depois);
        if (w != null) {
            w.repaint();
        }
        if (antes instanceof JComponent a && a.getClientProperty(BORDA_ORIGINAL) != null) {
            Object original = a.getClientProperty(BORDA_ORIGINAL);
            a.setBorder(original instanceof Border b ? b : null);
            a.putClientProperty(BORDA_ORIGINAL, null);
        }
        if (depois instanceof JComponent d && (d instanceof AbstractButton || d instanceof JComboBox
                || d instanceof JSlider) && SwingUtilities.getWindowAncestor(d) != null
                && (SwingUtilities.getWindowAncestor(d) == janela
                        || SwingUtilities.getWindowAncestor(d).getOwner() == janela)) {
            Border original = d.getBorder();
            d.putClientProperty(BORDA_ORIGINAL, original == null ? Boolean.FALSE : original);
            Border foco = BorderFactory.createLineBorder(COR_FOCO, 3);
            d.setBorder(original == null ? foco : BorderFactory.createCompoundBorder(foco, original));
        }
    }

    /** Para testes: o foco atual tem o destaque. */
    static boolean temDestaque(Component c) {
        return c instanceof JComponent j && j.getClientProperty(BORDA_ORIGINAL) != null;
    }
}
