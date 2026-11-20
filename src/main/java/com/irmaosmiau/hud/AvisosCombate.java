package com.irmaosmiau.hud;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import com.irmaosmiau.controles.NaturezaAcao;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Entrega 5P.1: aviso discreto no topo central ("J1 derrubou J2!"), so com
 * resultados CONFIRMADOS pelo MotorSolo. Um de cada vez, ~1,8 s cada; fila
 * curta (os mais velhos saem) e, com fila, cada um fica menos tempo para o
 * texto nunca atrasar demais em relacao a luta. Fica abaixo do relogio e entre
 * os paineis de folego dos dois lutadores.
 */
public final class AvisosCombate {

    /** ~1,8 s no loop de 16 ms. */
    public static final int DURACAO_TICKS = 110;
    /** Com outros na fila, o atual fica so isto. */
    public static final int DURACAO_COM_FILA_TICKS = 60;
    public static final int FILA_MAXIMA = 3;
    public static final int Y = 34;
    public static final int ALTURA = 22;
    public static final int LARGURA_MAXIMA = 330;

    private static final Font FONTE = new Font("Arial", Font.BOLD, 13);

    /** 5Q.7: cor das mensagens sem natureza (avisos do sistema: a mesma de antes). */
    public static final Color COR_NEUTRA = new Color(255, 236, 160);

    private record Aviso(String texto, NaturezaAcao natureza) {
    }

    private final Deque<Aviso> fila = new ArrayDeque<>();
    private String atual;
    private NaturezaAcao naturezaAtual;
    private int restantes;

    /** Aviso neutro (sistema: modo desenvolvedor, laboratorio, recursos...). */
    public void adicionar(String mensagem) {
        adicionar(mensagem, null);
    }

    /**
     * 5Q.7: aviso de uma acao com a natureza REAL dela (a mesma das rodas): a cor sai de
     * PainelDpadContextual.corDaNatureza, a unica tabela de cores das naturezas.
     */
    public void adicionar(String mensagem, NaturezaAcao natureza) {
        if (mensagem == null || mensagem.isBlank() || mensagem.equals(atual) && restantes > DURACAO_TICKS / 2) {
            return;
        }
        if (atual == null) {
            atual = mensagem;
            naturezaAtual = natureza;
            restantes = DURACAO_TICKS;
            return;
        }
        fila.addLast(new Aviso(mensagem, natureza));
        while (fila.size() > FILA_MAXIMA) {
            fila.removeFirst();
        }
        restantes = Math.min(restantes, DURACAO_COM_FILA_TICKS);
    }

    /** Um tick (so com a luta rodando; na pausa o aviso fica parado). */
    public void atualizar() {
        if (atual == null) {
            return;
        }
        if (--restantes <= 0) {
            Aviso a = fila.pollFirst();
            atual = a == null ? null : a.texto();
            naturezaAtual = a == null ? null : a.natureza();
            restantes = atual == null ? 0 : fila.isEmpty() ? DURACAO_TICKS : DURACAO_COM_FILA_TICKS;
        }
    }

    public void limpar() {
        fila.clear();
        atual = null;
        naturezaAtual = null;
        restantes = 0;
    }

    /** Aviso na tela agora (null = nenhum). */
    public String getAtual() {
        return atual;
    }

    /** 5Q.7: natureza do aviso na tela (null = neutro). */
    public NaturezaAcao getNaturezaAtual() {
        return atual == null ? null : naturezaAtual;
    }

    /** 5Q.7: cor do texto do aviso na tela (a cor da natureza nas rodas; neutro = a de sempre). */
    public Color corAtual() {
        return corDe(getNaturezaAtual());
    }

    public static Color corDe(NaturezaAcao n) {
        return n == null ? COR_NEUTRA : PainelDpadContextual.corDaNatureza(n);
    }

    public int pendentes() {
        return fila.size();
    }

    public void desenhar(Graphics2D g0, int larguraTela) {
        if (atual == null) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(FONTE);
        FontMetrics fm = g.getFontMetrics();
        String texto = atual;
        while (fm.stringWidth(texto) > LARGURA_MAXIMA - 24 && texto.length() > 1) {
            texto = texto.substring(0, texto.length() - 2) + "…";     // nunca sai da caixa
        }
        int w = Math.min(LARGURA_MAXIMA, fm.stringWidth(texto) + 24);
        int x = larguraTela / 2 - w / 2;
        int alfa = restantes < 20 ? Math.max(40, restantes * 10) : 200;   // some suave no fim
        g.setColor(new Color(20, 20, 30, Math.min(170, alfa)));
        g.fillRoundRect(x, Y, w, ALTURA, 10, 10);
        if (naturezaAtual == NaturezaAcao.DESISTENCIA) {           // 5Q.7: o mesmo destaque do BATER na roda
            Color m = PainelDpadContextual.MOLDURA_DESISTENCIA;
            g.setColor(new Color(m.getRed(), m.getGreen(), m.getBlue(), Math.min(255, alfa + 55)));
            g.drawRoundRect(x, Y, w - 1, ALTURA - 1, 10, 10);
        }
        Color c = corAtual();
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(255, alfa + 55)));
        g.drawString(texto, larguraTela / 2 - fm.stringWidth(texto) / 2, Y + 16);
        g.dispose();
    }
}
