package com.irmaosmiau.hud;

import com.irmaosmiau.controles.CamadaComando;
import com.irmaosmiau.controles.CelulaContextual;
import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.PosicaoComando;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Cruz compacta do D-pad contextual (Entrega 5F), logo abaixo do painel do
 * jogador (faixa y 66..128, 138 px de largura): camada segurada e o que cada
 * direcao faz agora.
 *
 * Cores: branco = disponivel; cinza = existe mas nao da agora (recarga,
 * alcance...); escuro + "*" = reservada (transicao 5E ainda nao jogavel);
 * ponto = vazio.
 */
public final class PainelDpadContextual {

    public static final int LARGURA = 138;
    public static final int ALTURA = 62;
    public static final int Y = 66;
    /** 5P.2: o que aparece quando o contexto nao tem nenhuma acao. */
    public static final String TEXTO_VAZIO = "sem técnica neste contexto";

    /** O que desenhar (montado pelo GamePanel, testavel sem tela). */
    public record Quadro(String sigla, CamadaComando camada, String nomeCamada,
            Map<DirecaoTecnica, CelulaContextual> celulas) {
    }

    private static final Font TITULO = new Font("Arial", Font.BOLD, 10);
    private static final Font ROTULO = new Font("Arial", Font.BOLD, 10);
    private static final Color FUNDO = new Color(0, 0, 0, 165);
    private static final Color DISPONIVEL = new Color(255, 240, 200);
    private static final Color BLOQUEADA = new Color(165, 165, 165);
    private static final Color RESERVADA = new Color(175, 170, 205);
    private static final Color VAZIA = new Color(105, 105, 105);
    private static final Color CAMADA = new Color(255, 214, 120);

    private PainelDpadContextual() {
    }

    /** So as celulas da camada pedida, por direcao. */
    public static Quadro montar(String sigla, CamadaComando camada, String nomeCamada, List<CelulaContextual> todas) {
        Map<DirecaoTecnica, CelulaContextual> m = new EnumMap<>(DirecaoTecnica.class);
        for (CelulaContextual c : todas) {
            if (c.posicao().camada() == camada) {
                m.put(c.posicao().direcao(), c);
            }
        }
        return new Quadro(sigla, camada, nomeCamada, m);
    }

    /**
     * 5Q.5: roda completamente vazia no contexto atual nao aparece (a camada continua
     * existindo internamente, para funcionalidades futuras). Antes desenhava um painel
     * com "sem tecnica neste contexto".
     */
    public static boolean deveMostrar(Quadro q) {
        return q != null && !q.celulas().isEmpty();
    }

    /** Texto e cor de uma direcao. */
    public static String texto(Quadro q, DirecaoTecnica d) {
        CelulaContextual c = q.celulas().get(d);
        if (c == null) {
            return "·";
        }
        return c.isExecutavel() ? c.rotulo() : c.rotulo() + "*";
    }

    // 5Q.6: cores semanticas (tons equilibrados, legiveis no fundo escuro)
    public static final Color DEFESA = new Color(135, 190, 255);
    public static final Color TRANSICAO = new Color(135, 220, 150);
    public static final Color FINALIZACAO = new Color(255, 140, 128);
    public static final Color QUEDA = new Color(255, 200, 110);
    public static final Color DESISTENCIA = new Color(255, 255, 255);
    public static final Color MOLDURA_DESISTENCIA = new Color(255, 214, 90);

    /** 5Q.6: natureza da celula (informada; o romper da pegada e defesa; o resto, controle). */
    public static com.irmaosmiau.controles.NaturezaAcao natureza(CelulaContextual c) {
        if (c == null) {
            return null;
        }
        if (c.natureza() != null) {
            return c.natureza();
        }
        if (c.acao() == com.irmaosmiau.controles.AcaoJogador.ROMPIMENTO_TECNICO) {
            return com.irmaosmiau.controles.NaturezaAcao.DEFESA;
        }
        return com.irmaosmiau.controles.NaturezaAcao.CONTROLE;
    }

    /** 5Q.6: cor da natureza (CONTROLE = o creme de antes). */
    public static Color corDaNatureza(com.irmaosmiau.controles.NaturezaAcao n) {
        if (n == null) {
            return DISPONIVEL;
        }
        return switch (n) {
            case DEFESA -> DEFESA;
            case TRANSICAO -> TRANSICAO;
            case FINALIZACAO -> FINALIZACAO;
            case QUEDA -> QUEDA;
            case DESISTENCIA -> DESISTENCIA;
            case CONTROLE -> DISPONIVEL;
        };
    }

    static Color cor(Quadro q, DirecaoTecnica d) {
        CelulaContextual c = q.celulas().get(d);
        if (c == null) {
            return VAZIA;
        }
        if (!c.isExecutavel()) {
            return RESERVADA;
        }
        Color n = corDaNatureza(natureza(c));
        if (c.disponivelAgora()) {
            return n;
        }
        // bloqueada (recarga, requisito, graduacao): a cor da natureza apagada para o cinza (o rotulo
        // continua dizendo a faixa exigida, "·Az", ou "·X" no modo)
        return new Color((n.getRed() + 2 * BLOQUEADA.getRed()) / 3, (n.getGreen() + 2 * BLOQUEADA.getGreen()) / 3,
                (n.getBlue() + 2 * BLOQUEADA.getBlue()) / 3);
    }

    public static void desenhar(Graphics2D g0, int x, Quadro q) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(FUNDO);
        g.fillRoundRect(x, Y, LARGURA, ALTURA, 8, 8);

        g.setFont(TITULO);
        g.setColor(CAMADA);
        caber(g, q.sigla() + " · " + q.nomeCamada(), LARGURA - 10, TITULO);   // 5P.1: nunca vaza do painel
        g.drawString(ultimoTexto, x + 6, Y + 12);

        int cx = x + LARGURA / 2;
        int cy = Y + 38;
        g.setColor(new Color(255, 255, 255, 70));
        g.fillRect(cx - 3, cy - 9, 6, 18);
        g.fillRect(cx - 9, cy - 3, 18, 6);

        g.setFont(ROTULO);
        FontMetrics fm = g.getFontMetrics();
        if (q.celulas().isEmpty()) {
            // 5P.2: sem nada neste contexto: aviso discreto em vez de um painel "quebrado"
            g.setFont(PEQUENA);
            g.setColor(VAZIA);
            String t = TEXTO_VAZIO;
            g.drawString(t, cx - g.getFontMetrics().stringWidth(t) / 2, cy + 21);
            g.dispose();
            return;
        }
        escrever(g, fm, q, DirecaoTecnica.CIMA, cx, cy - 13, 0);
        escrever(g, fm, q, DirecaoTecnica.BAIXO, cx, cy + 21, 0);
        escrever(g, fm, q, DirecaoTecnica.ESQUERDA, cx - 13, cy + 4, -1);
        escrever(g, fm, q, DirecaoTecnica.DIREITA, cx + 13, cy + 4, 1);
        g.dispose();
    }

    /** alinhamento: -1 = termina em ax, 0 = centrado, 1 = comeca em ax. */
    private static void escrever(Graphics2D g, FontMetrics fm0, Quadro q, DirecaoTecnica d, int ax, int y,
            int alinhamento) {
        // 5P.1: os lados tem ~52 px, cima/baixo ~120 px; texto maior encolhe e, se preciso, corta.
        caber(g, texto(q, d), alinhamento == 0 ? LARGURA - 16 : LARGURA / 2 - 17, ROTULO);
        String t = ultimoTexto;
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(t);
        int x = alinhamento < 0 ? ax - w : alinhamento == 0 ? ax - w / 2 : ax;
        CelulaContextual c = q.celulas().get(d);
        com.irmaosmiau.controles.NaturezaAcao n = c != null && c.isExecutavel() ? natureza(c) : null;
        if (n == com.irmaosmiau.controles.NaturezaAcao.DESISTENCIA) {
            g.setColor(MOLDURA_DESISTENCIA);                   // 5Q.6: bater e uma acao especial
            g.drawRoundRect(x - 3, y - fm.getAscent(), w + 5, fm.getAscent() + 3, 4, 4);
        }
        g.setColor(cor(q, d));
        if (n != null && n != com.irmaosmiau.controles.NaturezaAcao.CONTROLE
                && n != com.irmaosmiau.controles.NaturezaAcao.DESISTENCIA) {
            forma(g, n, alinhamento < 0 ? x + w + 2 : x - 6, y - 4);   // 5Q.6: nao depende so da cor
        }
        g.drawString(t, x, y);
        g.setFont(ROTULO);
    }

    /** 5Q.6: forma de 4 px da natureza: quadrado (defesa), triangulo (transicao), circulo (finalizacao), losango (queda). */
    static void forma(Graphics2D g, com.irmaosmiau.controles.NaturezaAcao n, int x, int y) {
        switch (n) {
            case DEFESA -> g.fillRect(x, y - 2, 4, 4);
            case TRANSICAO -> g.fillPolygon(new int[] {x, x + 4, x}, new int[] {y - 2, y, y + 2}, 3);
            case FINALIZACAO -> g.fillOval(x, y - 2, 4, 4);
            case QUEDA -> g.fillPolygon(new int[] {x + 2, x + 4, x + 2, x}, new int[] {y - 3, y, y + 3, y}, 4);
            default -> {
            }
        }
    }

    private static final Font PEQUENA = new Font("Arial", Font.BOLD, 8);
    private static String ultimoTexto = "";

    /** Escolhe a fonte (normal ou pequena) e corta com reticencias para caber em max px. */
    static void caber(Graphics2D g, String texto, int max, Font normal) {
        g.setFont(normal);
        if (g.getFontMetrics().stringWidth(texto) <= max) {
            ultimoTexto = texto;
            return;
        }
        g.setFont(PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        String t = texto;
        while (t.length() > 1 && fm.stringWidth(t) > max) {
            t = t.substring(0, t.length() - 2) + "…";
            if (t.endsWith("……")) {
                t = t.substring(0, t.length() - 1);
            }
        }
        ultimoTexto = t;
    }

    /** Para testes: a posicao que a celula ocupa. */
    static PosicaoComando posicao(Quadro q, DirecaoTecnica d) {
        return new PosicaoComando(q.camada(), d);
    }
}
