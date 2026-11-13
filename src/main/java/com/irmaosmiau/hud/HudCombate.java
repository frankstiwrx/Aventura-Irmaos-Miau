package com.irmaosmiau.hud;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/**
 * HUD do jogador (o debug tecnico e outra camada, separada).
 *
 * Layout para 800 x 450, sem cobrir os personagens:
 * <pre>
 *  [painel lutador 1]   [MODO TREINO]        [painel lutador 2]   y   8..58
 *                       [estado interacao]
 *                       [mensagem curta]                          y  ..64
 *  ...........................................................
 *  [dica de controles, faixa fina]                                y 432..450
 * </pre>
 * As cabecas ficam a partir de y ~ 133 (pulo alto ~ 56); os pes vao ate
 * y ~ 406. O HUD fica fora dessas faixas.
 */
public final class HudCombate {

    public static final int ALTURA_FAIXA_SUPERIOR = 64;
    public static final int ALTURA_FAIXA_INFERIOR = 18;

    static final int LARGURA_PAINEL = 210;
    static final int LARGURA_BARRA = 110;

    private static final Color FUNDO_PAINEL = new Color(20, 22, 28, 170);
    private static final Color TEXTO = new Color(245, 245, 240);
    private static final Color TEXTO_SUAVE = new Color(200, 200, 195);
    private static final Color COR_FOLEGO = new Color(70, 150, 230);
    private static final Color COR_CONDICIONAMENTO = new Color(80, 190, 95);
    private static final Color COR_SUCESSO = new Color(120, 220, 120);
    private static final Color COR_AVISO = new Color(250, 200, 90);
    private static final Color COR_INFO = new Color(220, 220, 220);
    private static final Color COR_TREINO = new Color(176, 44, 40);

    private final Font fonteNome = new Font("Arial", Font.BOLD, 13);
    private final Font fonteRotulo = new Font("Arial", Font.BOLD, 9);
    private final Font fonteValor = new Font("Arial", Font.PLAIN, 10);
    private final Font fonteEstado = new Font("Arial", Font.BOLD, 12);
    private final Font fonteMensagem = new Font("Arial", Font.BOLD, 13);
    private final Font fonteDica = new Font("Arial", Font.PLAIN, 11);

    /** Tudo que o HUD mostra num frame. */
    public static final class Estado {
        final List<DadosLutadorHud> lutadores;
        final String estadoInteracao;
        final String mensagem;
        final FeedbackCombate.Tipo tipoMensagem;
        final boolean modoTreino;
        final String dicaControles;

        /**
         * @param lutadores 1 (esquerda) ou 2 (esquerda e direita)
         */
        public Estado(List<DadosLutadorHud> lutadores, String estadoInteracao, String mensagem,
                FeedbackCombate.Tipo tipoMensagem, boolean modoTreino, String dicaControles) {
            if (lutadores == null || lutadores.isEmpty() || lutadores.size() > 2) {
                throw new IllegalArgumentException("O HUD mostra 1 ou 2 lutadores.");
            }
            this.lutadores = List.copyOf(lutadores);
            this.estadoInteracao = estadoInteracao;
            this.mensagem = mensagem;
            this.tipoMensagem = tipoMensagem;
            this.modoTreino = modoTreino;
            this.dicaControles = dicaControles;
        }

        public List<DadosLutadorHud> getLutadores() {
            return lutadores;
        }

        public String getEstadoInteracao() {
            return estadoInteracao;
        }

        public String getMensagem() {
            return mensagem;
        }

        public boolean isModoTreino() {
            return modoTreino;
        }

        public String getDicaControles() {
            return dicaControles;
        }
    }

    public void desenhar(Graphics2D g2, Estado e, int largura, int altura) {
        Graphics2D g = (Graphics2D) g2.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            painel(g, e.lutadores.get(0), 10, 8, false);
            if (e.lutadores.size() == 2) {
                painel(g, e.lutadores.get(1), largura - 10 - LARGURA_PAINEL, 8, true);
            }

            int centro = largura / 2;
            int y = 18;
            if (e.modoTreino) {
                etiqueta(g, "MODO TREINO", centro, 8);
                y = 36;
            }
            if (e.estadoInteracao != null) {
                g.setFont(fonteEstado);
                textoComSombra(g, e.estadoInteracao, centro, y, COR_AVISO);
                y += 16;
            }
            if (e.mensagem != null) {
                g.setFont(fonteMensagem);
                textoComSombra(g, e.mensagem, centro, Math.min(y + 2, ALTURA_FAIXA_SUPERIOR - 4),
                        corDe(e.tipoMensagem));
            }

            if (e.dicaControles != null && !e.dicaControles.isBlank()) {
                int y0 = altura - ALTURA_FAIXA_INFERIOR;
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRect(0, y0, largura, ALTURA_FAIXA_INFERIOR);
                g.setFont(fonteDica);
                g.setColor(TEXTO_SUAVE);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(e.dicaControles, (largura - fm.stringWidth(e.dicaControles)) / 2, altura - 5);
            }
        } finally {
            g.dispose();
        }
    }

    private void painel(Graphics2D g, DadosLutadorHud d, int x, int y, boolean direita) {
        int altura = 50;
        g.setColor(FUNDO_PAINEL);
        g.fillRoundRect(x, y, LARGURA_PAINEL, altura, 10, 10);

        int tx = x + 10;
        int ty = y + 16;

        g.setFont(fonteNome);
        g.setColor(TEXTO);
        String nome = d.getNome();
        if (direita) {
            int w = g.getFontMetrics().stringWidth(nome);
            g.drawString(nome, x + LARGURA_PAINEL - 10 - w, ty);
        } else {
            g.drawString(nome, tx, ty);
        }
        if (d.getRotulo() != null) {
            g.setFont(fonteRotulo);
            g.setColor(TEXTO_SUAVE);
            int w = g.getFontMetrics().stringWidth(d.getRotulo());
            g.drawString(d.getRotulo(), direita ? tx : x + LARGURA_PAINEL - 10 - w, ty);
        }

        barra(g, "FÔLEGO", d.textoFolego(), d.getPercentualFolego(), COR_FOLEGO, x + 10, y + 24, direita);
        barra(g, "CONDIC.", d.textoCondicionamento(), d.getPercentualCondicionamento(),
                COR_CONDICIONAMENTO, x + 10, y + 37, direita);
    }

    private void barra(Graphics2D g, String rotulo, String valor, double percentual, Color cor,
            int x, int y, boolean direita) {
        int larguraRotulo = 48;
        int bx = x + larguraRotulo;
        int h = 8;

        g.setFont(fonteRotulo);
        g.setColor(TEXTO_SUAVE);
        g.drawString(rotulo, x, y + 8);

        g.setColor(new Color(60, 60, 60, 220));
        g.fillRoundRect(bx, y, LARGURA_BARRA, h, 4, 4);
        int preenchido = (int) Math.round((LARGURA_BARRA - 2) * Math.max(0, Math.min(1, percentual)));
        g.setColor(cor);
        if (direita) {
            g.fillRoundRect(bx + 1 + (LARGURA_BARRA - 2 - preenchido), y + 1, preenchido, h - 2, 3, 3);
        } else {
            g.fillRoundRect(bx + 1, y + 1, preenchido, h - 2, 3, 3);
        }

        g.setFont(fonteValor);
        g.setColor(TEXTO);
        g.drawString(valor, bx + LARGURA_BARRA + 4, y + 8);
    }

    private void etiqueta(Graphics2D g, String texto, int centro, int y) {
        g.setFont(fonteRotulo);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(texto) + 16;
        g.setColor(COR_TREINO);
        g.fillRoundRect(centro - w / 2, y, w, 16, 8, 8);
        g.setColor(Color.WHITE);
        g.drawString(texto, centro - fm.stringWidth(texto) / 2, y + 12);
    }

    /** Texto centralizado sobre uma caixa escura (legivel em qualquer cenario). */
    private static void textoComSombra(Graphics2D g, String texto, int centro, int y, Color cor) {
        FontMetrics fm = g.getFontMetrics();
        int largura = fm.stringWidth(texto);
        int x = centro - largura / 2;
        g.setColor(FUNDO_PAINEL);
        g.fillRoundRect(x - 8, y - fm.getAscent() - 1, largura + 16, fm.getAscent() + 5, 8, 8);
        g.setColor(new Color(0, 0, 0, 170));
        g.drawString(texto, x + 1, y + 1);
        g.setColor(cor);
        g.drawString(texto, x, y);
    }

    private static Color corDe(FeedbackCombate.Tipo tipo) {
        if (tipo == FeedbackCombate.Tipo.SUCESSO) {
            return COR_SUCESSO;
        }
        if (tipo == FeedbackCombate.Tipo.AVISO) {
            return COR_AVISO;
        }
        return COR_INFO;
    }
}
