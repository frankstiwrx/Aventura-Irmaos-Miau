package com.irmaosmiau.ui;

import com.irmaosmiau.entities.IrmaoMiau;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.Shape;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * Entrega 5M: arte das telas de apresentacao, desenhada em Java2D (sem
 * imagens externas): paleta azul-claro / azul-escuro / branco / amarelo,
 * o cenario original "Patio do Dojo Miau", o letreiro com contorno, os
 * botoes de menu e os irmaos pelo renderizador oficial.
 *
 * Nada aqui toca a luta: os lutadores desenhados sao instancias proprias da
 * tela, criadas so para a apresentacao.
 */
public final class ArteMenus {

    public static final Color AZUL_CLARO = new Color(120, 190, 240);
    public static final Color AZUL_CEU = new Color(160, 214, 250);
    public static final Color AZUL_ESCURO = new Color(18, 40, 92);
    public static final Color AZUL_PAINEL = new Color(22, 52, 118, 215);
    public static final Color AMARELO = new Color(255, 206, 46);
    public static final Color BRANCO = new Color(248, 248, 244);
    public static final Color J1 = new Color(60, 150, 255);
    public static final Color J2 = new Color(235, 64, 64);

    public static final Font LETREIRO = new Font("Arial", Font.BOLD, 40);
    public static final Font MENU = new Font("Arial", Font.BOLD, 20);
    public static final Font TEXTO = new Font("Arial", Font.BOLD, 13);
    public static final Font PEQUENO = new Font("Arial", Font.PLAIN, 11);

    private ArteMenus() {
    }

    public static Graphics2D suave(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    // =====================================================
    // CENARIO ORIGINAL: PATIO DO DOJO MIAU
    // =====================================================
    /**
     * Ceu, colinas, o dojo (telhado em duas aguas com patas de gato no
     * frontao, varanda, lanternas), o patio de terra batida e o tatame azul.
     *
     * @param tempo segundos (nuvens e bandeirolas se mexem devagar)
     */
    public static void desenharPatioDojo(Graphics2D g, int w, int h, double tempo) {
        g.setPaint(new GradientPaint(0, 0, AZUL_CLARO, 0, h * 0.55f, AZUL_CEU));
        g.fillRect(0, 0, w, h);

        // nuvens simples
        g.setColor(new Color(255, 255, 255, 200));
        for (int i = 0; i < 4; i++) {
            int x = (int) ((i * 230 + tempo * 6) % (w + 160)) - 80;
            int y = 30 + (i % 2) * 28;
            g.fillOval(x, y, 90, 26);
            g.fillOval(x + 30, y - 12, 60, 30);
        }
        // colinas
        g.setColor(new Color(90, 170, 110));
        g.fillOval(-120, h / 2 - 40, 460, 160);
        g.fillOval(420, h / 2 - 30, 520, 150);

        // dojo
        int dx = w / 2 - 170;
        int base = (int) (h * 0.62);
        g.setColor(new Color(244, 232, 205));
        g.fillRect(dx, base - 120, 340, 120);                       // paredes
        g.setColor(new Color(150, 92, 52));
        for (int i = 0; i < 5; i++) {
            g.fillRect(dx + 10 + i * 80, base - 120, 10, 120);      // pilares
        }
        g.setColor(AZUL_ESCURO);
        Polygon telhado = new Polygon();
        telhado.addPoint(dx - 30, base - 116);
        telhado.addPoint(dx + 170, base - 190);
        telhado.addPoint(dx + 370, base - 116);
        g.fillPolygon(telhado);
        g.setColor(new Color(40, 78, 150));
        g.fillRect(dx - 30, base - 122, 400, 10);                   // beiral
        // emblema original no frontao: uma pata de gato amarela
        g.setColor(AMARELO);
        int px = dx + 170;
        int py = base - 150;
        g.fillOval(px - 13, py - 6, 26, 20);
        g.fillOval(px - 20, py - 18, 10, 11);
        g.fillOval(px - 7, py - 24, 10, 11);
        g.fillOval(px + 6, py - 24, 10, 11);
        g.fillOval(px + 15, py - 16, 10, 11);
        // porta e varanda
        g.setColor(new Color(110, 68, 40));
        g.fillRect(dx + 140, base - 80, 60, 80);
        g.setColor(new Color(170, 120, 74));
        g.fillRect(dx - 20, base - 8, 380, 10);
        // lanternas e bandeirolas
        for (int i = 0; i < 2; i++) {
            int lx = dx - 50 + i * 420;
            g.setColor(new Color(110, 68, 40));
            g.fillRect(lx + 7, base - 70, 4, 70);
            g.setColor(new Color(255, 120, 60));
            g.fillRoundRect(lx, base - 92, 18, 24, 8, 8);
        }
        double onda = Math.sin(tempo * 2) * 2;
        Color[] cores = {AMARELO, BRANCO, J1, AMARELO, BRANCO, J1, AMARELO};
        for (int i = 0; i < cores.length; i++) {
            int bx = dx - 10 + i * 52;
            g.setColor(cores[i]);
            Polygon b = new Polygon();
            b.addPoint(bx, base - 112);
            b.addPoint(bx + 18, base - 112);
            b.addPoint(bx + 9, (int) (base - 96 + onda));
            g.fillPolygon(b);
        }

        // patio e tatame
        g.setColor(new Color(214, 186, 130));
        g.fillRect(0, base, w, h - base);
        g.setColor(new Color(60, 120, 200));
        g.fillRect(40, base + 22, w - 80, h - base - 22);
        g.setColor(new Color(90, 150, 225));
        g.setStroke(new BasicStroke(3));
        g.drawRect(60, base + 34, w - 120, h - base - 40);
    }

    // =====================================================
    // LETREIRO
    // =====================================================
    /** Texto centralizado com contorno azul-escuro, sombra e preenchimento amarelo. */
    public static void letreiro(Graphics2D g, String texto, int cx, int y, Font fonte, Color preenchimento) {
        GlyphVector gv = fonte.createGlyphVector(g.getFontRenderContext(), texto);
        int largura = (int) gv.getLogicalBounds().getWidth();
        Shape forma = gv.getOutline(cx - largura / 2f, y);
        Graphics2D s = (Graphics2D) g.create();
        s.translate(3, 4);
        s.setColor(new Color(0, 0, 0, 110));
        s.fill(forma);
        s.dispose();
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(AZUL_ESCURO);
        g.draw(forma);
        g.setColor(preenchimento);
        g.fill(forma);
    }

    /** Texto simples centralizado. */
    public static void centralizado(Graphics2D g, String texto, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, cx - fm.stringWidth(texto) / 2, y);
    }

    // =====================================================
    // LUTADORES (renderizador oficial)
    // =====================================================
    /**
     * Desenha o lutador como no GamePanel (quem olha para a esquerda e
     * espelhado no proprio eixo). A instancia e da tela, nao de uma luta.
     */
    public static void desenharLutador(Graphics2D g0, IrmaoMiau l) {
        Graphics2D g = (Graphics2D) g0.create();
        // 5M.1: no desenho "em pe" (idle dos menus) a boca usa o traco herdado de quem chama.
        // Sem isto, o contorno do letreiro (6 px) ou dos cursores engrossava o bigodinho.
        g.setStroke(tracoDoRosto(l.getEscala()));
        if (!l.isViradoParaDireita()) {
            double eixo = l.getX() + 35 * l.getEscala();
            g.translate(eixo, 0);
            g.scale(-1, 1);
            g.translate(-eixo, 0);
        }
        l.desenhar(g);
        g.dispose();
    }

    /**
     * Mesmo traco que o rig oficial usa na cabeca (IrmaoMiauBranco/Preto e
     * MiauStudio): max(1, 1.5 * escala), pontas arredondadas.
     */
    static BasicStroke tracoDoRosto(double escala) {
        return IrmaoMiau.tracoDaBoca(escala);                  // 5M.2: uma definicao so
    }

    /** Sombra eliptica no chao embaixo do lutador. */
    public static void sombra(Graphics2D g, IrmaoMiau l, int yChao) {
        g.setColor(new Color(0, 0, 0, 60));
        int larg = (int) (100 * l.getEscala());
        g.fillOval((int) (l.getX() + 35 * l.getEscala() - larg / 2.0), yChao - 6, larg, 12);
    }

    // =====================================================
    // BOTOES
    // =====================================================
    /** Botao de menu: caixa azul-escura, texto branco; em foco/mouse, amarelo. */
    public static JButton botaoMenu(String texto) {
        JButton b = new JButton(texto) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = suave(g0);
                boolean ativo = isFocusOwner() || getModel().isRollover();
                g.setColor(ativo ? new Color(30, 70, 150, 235) : AZUL_PAINEL);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g.setColor(ativo ? AMARELO : new Color(120, 170, 235));
                g.setStroke(new BasicStroke(ativo ? 3 : 1.5f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
                g.setFont(getFont());
                g.setColor(!isEnabled() ? new Color(150, 160, 180) : ativo ? AMARELO : BRANCO);
                FontMetrics fm = g.getFontMetrics();
                String t = (ativo ? "▶ " : "") + getText();
                g.drawString(t, (getWidth() - fm.stringWidth(t)) / 2, (getHeight() + fm.getAscent()) / 2 - 3);
                g.dispose();
            }
        };
        b.setFont(MENU);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setBorder(BorderFactory.createEmptyBorder());
        return b;
    }
}
