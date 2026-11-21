package com.irmaosmiau.ui;

import com.irmaosmiau.entities.AparenciaLutador;
import com.irmaosmiau.entities.AparenciaLutador.Parte;
import com.irmaosmiau.entities.IrmaoMiau;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Cores do personagem personalizado (editor de personagens -> APARENCIA): uma parte por vez (kimono, calca,
 * faixa visual, pelagem) com controles HSV compactos (matiz, saturacao, brilho) e a previa NO RENDERIZADOR
 * REAL do lutador, atualizada a cada mudanca. Mouse, teclado e gamepad pelo NavegadorMenus (cima/baixo
 * escolhe, lados mudam a parte e os valores, A confirma, B cancela).
 *
 * A faixa daqui e so visual: a graduacao da luta continua a escolhida na selecao.
 */
final class EditorAparenciaPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Supplier<IrmaoMiau> criarBase;
    private transient AparenciaLutador aparencia;
    private transient IrmaoMiau previa;
    private final JComboBox<Parte> parte = new JComboBox<>(Parte.values());
    private final JSlider matiz = new JSlider(0, 360);
    private final JSlider saturacao = new JSlider(0, 100);
    private final JSlider brilho = new JSlider(0, 100);
    private final JLabel amostra = new JLabel();
    private final JLabel aviso = EstiloTelas.texto(" ", EstiloTelas.PEQUENA, new Color(250, 200, 90));
    final JButton original = EstiloTelas.botao("COR ORIGINAL");
    final JButton ok = EstiloTelas.botao("OK");
    final JButton cancelar = NavegadorMenus.marcarVoltar(EstiloTelas.botao("CANCELAR"));
    private boolean sincronizando;
    private transient AparenciaLutador resultado;

    EditorAparenciaPanel(Supplier<IrmaoMiau> criarBase, AparenciaLutador inicial) {
        this.criarBase = criarBase;
        this.aparencia = inicial == null ? AparenciaLutador.ORIGINAL : inicial;
        setLayout(null);
        setPreferredSize(new Dimension(520, 300));
        colocar(EstiloTelas.titulo("APARÊNCIA"), 18, 10, 300, 30);
        colocar(EstiloTelas.rotulo("Parte"), 220, 56, 80, 24);
        parte.setFont(EstiloTelas.NORMAL);
        colocar(parte, 300, 54, 200, 26);   // o rotulo da parte (Parte.toString)
        linha("Matiz", matiz, 92);
        linha("Saturação", saturacao, 126);
        linha("Brilho", brilho, 160);
        amostra.setOpaque(true);
        colocar(EstiloTelas.rotulo("Cor"), 220, 196, 80, 22);
        colocar(amostra, 300, 194, 60, 24);
        colocar(original, 370, 192, 130, 28);
        colocar(aviso, 220, 226, 290, 16);
        colocar(ok, 270, 256, 90, 30);
        colocar(cancelar, 366, 256, 134, 30);
        parte.addActionListener(e -> mostrarParte());
        matiz.addChangeListener(e -> aoMudar());
        saturacao.addChangeListener(e -> aoMudar());
        brilho.addChangeListener(e -> aoMudar());
        original.addActionListener(e -> {
            aparencia = aparencia.com(parteAtual(), null);
            atualizarPrevia();
            mostrarParte();
        });
        ok.addActionListener(e -> fechar(aparencia));
        cancelar.addActionListener(e -> fechar(null));
        atualizarPrevia();
        mostrarParte();
    }

    private void linha(String nome, JSlider s, int y) {
        colocar(EstiloTelas.rotulo(nome), 220, y + 2, 80, 22);
        s.setOpaque(false);
        colocar(s, 300, y, 200, 26);
    }

    private void colocar(Component c, int x, int y, int w, int h) {
        c.setBounds(x, y, w, h);
        add(c);
    }

    Parte parteAtual() {
        return (Parte) parte.getSelectedItem();
    }

    /** A cor que a parte tem agora no desenho (a personalizada ou a original da base). */
    private Color corNoDesenho(Parte p) {
        return switch (p) {
            case CASACO -> previa.getCorCasaco();
            case CALCA -> previa.getCorCalca();
            case FAIXA -> previa.getCorFaixa();
            case PELO -> previa.getCorPelo();
        };
    }

    private void mostrarParte() {
        sincronizando = true;
        try {
            Color c = corNoDesenho(parteAtual());
            float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            matiz.setValue(Math.round(hsv[0] * 360));
            saturacao.setValue(Math.round(hsv[1] * 100));
            brilho.setValue(Math.round(hsv[2] * 100));
            amostra.setBackground(c);
            aviso.setText(parteAtual() == Parte.FAIXA ? "Só visual: a graduação da luta não muda."
                    : aparencia.cor(parteAtual()).isPresent() ? "Personalizada." : "Cor original do personagem.");
        } finally {
            sincronizando = false;
        }
        repaint();
    }

    private void aoMudar() {
        if (sincronizando) {
            return;
        }
        Color c = Color.getHSBColor(matiz.getValue() / 360f, saturacao.getValue() / 100f, brilho.getValue() / 100f);
        aparencia = aparencia.com(parteAtual(), c);
        amostra.setBackground(c);
        aviso.setText(parteAtual() == Parte.FAIXA ? "Só visual: a graduação da luta não muda." : "Personalizada.");
        atualizarPrevia();
    }

    /** O lutador de verdade (base + aparencia), recriado para a previa. */
    private void atualizarPrevia() {
        previa = criarBase.get();
        previa.aplicarAparencia(aparencia);
        previa.setViradoParaDireita(true);
        ArteMenus.apresentacao(previa);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        EstiloTelas.pintarFundo(g0, getWidth(), getHeight());
        Graphics2D g = ArteMenus.suave(g0);
        g.setColor(new Color(255, 255, 255, 30));
        g.fillOval(46, 262, 120, 16);
        ArteMenus.desenharLutador(g, previa);
        g.dispose();
    }

    private void fechar(AparenciaLutador r) {
        resultado = r;
        java.awt.Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JDialog d) {
            d.dispose();
        }
    }

    // ---- acesso (PerfisPanel e testes)
    AparenciaLutador getAparencia() {
        return aparencia;
    }

    AparenciaLutador getResultado() {
        return resultado;
    }

    IrmaoMiau getPrevia() {
        return previa;
    }

    JComboBox<Parte> controleParte() {
        return parte;
    }

    JSlider controleMatiz() {
        return matiz;
    }

    JSlider controleSaturacao() {
        return saturacao;
    }

    JSlider controleBrilho() {
        return brilho;
    }

    /** Abre o editor num dialogo modal; devolve a aparencia nova, ou null se cancelou. */
    static AparenciaLutador editar(Component pai, Supplier<IrmaoMiau> criarBase, AparenciaLutador inicial) {
        java.awt.Window dono = SwingUtilities.getWindowAncestor(pai);
        JDialog d = new JDialog(dono, "Aparência", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        EditorAparenciaPanel p = new EditorAparenciaPanel(criarBase, inicial);
        d.setContentPane(p);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.pack();
        d.setLocationRelativeTo(pai);
        SwingUtilities.invokeLater(p.parte::requestFocusInWindow);
        d.setVisible(true);
        return p.getResultado();
    }
}
