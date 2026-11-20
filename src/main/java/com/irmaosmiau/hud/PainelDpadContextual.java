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

    static Color cor(Quadro q, DirecaoTecnica d) {
        CelulaContextual c = q.celulas().get(d);
        if (c == null) {
            return VAZIA;
        }
        if (!c.isExecutavel()) {
            return RESERVADA;
        }
        return c.disponivelAgora() ? DISPONIVEL : BLOQUEADA;
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
        g.setColor(cor(q, d));
        g.drawString(t, x, y);
        g.setFont(ROTULO);
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
