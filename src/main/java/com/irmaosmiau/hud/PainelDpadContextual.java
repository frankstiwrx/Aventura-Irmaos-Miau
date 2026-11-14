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
        g.drawString(q.sigla() + " · " + q.nomeCamada(), x + 6, Y + 12);

        int cx = x + LARGURA / 2;
        int cy = Y + 38;
        g.setColor(new Color(255, 255, 255, 70));
        g.fillRect(cx - 3, cy - 9, 6, 18);
        g.fillRect(cx - 9, cy - 3, 18, 6);

        g.setFont(ROTULO);
        FontMetrics fm = g.getFontMetrics();
        escrever(g, fm, q, DirecaoTecnica.CIMA, cx, cy - 13, 0);
        escrever(g, fm, q, DirecaoTecnica.BAIXO, cx, cy + 21, 0);
        escrever(g, fm, q, DirecaoTecnica.ESQUERDA, cx - 13, cy + 4, -1);
        escrever(g, fm, q, DirecaoTecnica.DIREITA, cx + 13, cy + 4, 1);
        g.dispose();
    }

    /** alinhamento: -1 = termina em ax, 0 = centrado, 1 = comeca em ax. */
    private static void escrever(Graphics2D g, FontMetrics fm, Quadro q, DirecaoTecnica d, int ax, int y,
            int alinhamento) {
        String t = texto(q, d);
        int w = fm.stringWidth(t);
        int x = alinhamento < 0 ? ax - w : alinhamento == 0 ? ax - w / 2 : ax;
        g.setColor(cor(q, d));
        g.drawString(t, x, y);
    }

    /** Para testes: a posicao que a celula ocupa. */
    static PosicaoComando posicao(Quadro q, DirecaoTecnica d) {
        return new PosicaoComando(q.camada(), d);
    }
}
