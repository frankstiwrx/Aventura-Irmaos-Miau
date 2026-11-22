package com.irmaosmiau.ui;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Entrega 5M: menu principal reorganizado (substitui o MenuPanel no fluxo;
 * o MenuPanel continua no projeto, como a SelecaoPersonagemPanel da 4D).
 *
 * JOGAR (luta entre 2 jogadores) · TREINO · PERSONAGENS · CONFIGURACOES · SAIR.
 * Navegacao pelo NavegadorMenus (setas/D-pad/analogico, ENTER/A, B/ESC).
 * Nao existe opcao "1 JOGADOR" enquanto nao houver IA.
 */
public final class MenuPrincipalPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    // 5Q.6: o MODO DESENVOLVEDOR saiu do menu: liga/desliga com F8 dentro de uma partida normal
    public static final List<String> OPCOES = List.of("JOGAR", "TREINO", "PERSONAGENS", "CONFIGURAÇÕES", "SAIR");

    /** Descricao curta de cada opcao (mesma ordem), na barra de baixo, para a opcao em foco. */
    public static final List<String> DESCRICOES = List.of(
            "Entre na arena e dispute uma luta entre dois jogadores.",
            "Pratique suas habilidades e aperfeiçoe suas técnicas.",
            "Conheça e personalize seus personagens.",
            "Ajuste o som do jogo e os seus controles.",
            "Encerrar o jogo.");

    private final JButton[] botoes = new JButton[OPCOES.size()];
    private final transient IrmaoMiau branco = new IrmaoMiauBranco(470, 196, 0.85);
    private final transient IrmaoMiau preto = new IrmaoMiauPreto(650, 196, 0.85);
    private final Timer timer = new Timer(16, e -> tick());
    private long tempoMs;

    /**
     * @param aoJogar         abre a selecao de personagens (2 jogadores)
     * @param aoTreino        fluxo de treino existente
     * @param aoPersonagens   personagens personalizados (tela existente)
     * @param aoConfiguracoes configuracoes existentes
     * @param aoSair          encerra o jogo (o Main confirma e fecha a janela)
     */
    public MenuPrincipalPanel(Runnable aoJogar, Runnable aoTreino, Runnable aoPersonagens,
            Runnable aoConfiguracoes, Runnable aoSair) {
        setPreferredSize(new Dimension(800, 450));
        setLayout(null);
        Runnable[] acoes = {aoJogar, aoTreino, aoPersonagens, aoConfiguracoes, aoSair};
        for (int i = 0; i < botoes.length; i++) {
            JButton b = ArteMenus.botaoMenu(OPCOES.get(i));
            b.setBounds(80, 118 + i * 56, 280, 44);
            Runnable acao = acoes[i];
            b.addActionListener(e -> {
                if (acao != null) {
                    acao.run();
                }
            });
            add(b);
            botoes[i] = b;
        }
        branco.setViradoParaDireita(true);
        preto.setViradoParaDireita(false);
        ArteMenus.apresentacao(branco);
        ArteMenus.apresentacao(preto);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
        SwingUtilities.invokeLater(() -> botoes[0].requestFocusInWindow());
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    void tick() {
        tempoMs += 16;
        branco.atualizarApresentacao();                  // 5M.3: pose fixa, so a piscada
        preto.atualizarApresentacao();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = ArteMenus.suave(g0);
        ArteMenus.desenharPatioDojo(g, getWidth(), getHeight(), tempoMs / 1000.0);
        g.setColor(new Color(10, 30, 80, 120));
        g.fillRoundRect(60, 112, 320, 290, 18, 18);
        ArteMenus.letreiro(g, "AVENTURA DOS IRMÃOS MIAU", 400, 70, new Font("Arial", Font.BOLD, 32), ArteMenus.AMARELO);
        ArteMenus.sombra(g, branco, 410);
        ArteMenus.sombra(g, preto, 410);
        ArteMenus.desenharLutador(g, branco);
        ArteMenus.desenharLutador(g, preto);
        // barra de baixo: a descricao da opcao em foco (teclado, controle ou mouse) e os comandos
        g.setColor(new Color(10, 30, 80, 175));
        g.fillRoundRect(80, 410, 640, 34, 10, 10);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(ArteMenus.AMARELO);
        ArteMenus.centralizado(g, descricaoEmFoco(), 400, 425);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.setColor(ArteMenus.BRANCO);
        ArteMenus.centralizado(g, "↑↓ escolher · ENTER / A confirmar · ESC / B voltar", 400, 439);
        creditosEVersao(g);
        g.dispose();
    }

    /**
     * Creditos (canto inferior esquerdo) e versao BETA (canto inferior direito), pequenos, nos cantos livres ao
     * lado da barra de descricao: nada cobre as opcoes nem a descricao. So no menu principal.
     */
    private static void creditosEVersao(Graphics2D g) {
        Font f = new Font("Arial", Font.PLAIN, 10);
        // "by frankstiwrx · 2026" em duas linhas: a linha inteira nao cabe ao lado da barra
        texto(g, "by " + VersaoJogo.AUTOR, f, 6, 425, false);
        texto(g, "· " + VersaoJogo.ANO, f, 6, 439, false);
        texto(g, VersaoJogo.VERSAO, new Font("Arial", Font.BOLD, 11), 794, 425, true);
        Font fb = new Font("Arial", Font.BOLD, 9);
        g.setFont(fb);
        int w = g.getFontMetrics().stringWidth(VersaoJogo.ETAPA) + 8;
        g.setColor(new Color(255, 206, 46, 230));
        g.fillRoundRect(794 - w, 430, w, 12, 6, 6);
        g.setColor(ArteMenus.AZUL_ESCURO);
        g.drawString(VersaoJogo.ETAPA, 794 - w + 4, 439);
    }

    /** Texto pequeno com sombra (legivel sobre o patio). */
    private static void texto(Graphics2D g, String t, Font f, int x, int y, boolean alinharDireita) {
        g.setFont(f);
        int xx = alinharDireita ? x - g.getFontMetrics().stringWidth(t) : x;
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(t, xx + 1, y + 1);
        g.setColor(new Color(235, 240, 250));
        g.drawString(t, xx, y);
    }

    /** A descricao da opcao com o foco (a primeira, se nenhuma). */
    String descricaoEmFoco() {
        for (int i = 0; i < botoes.length; i++) {
            if (botoes[i].isFocusOwner()) {
                return DESCRICOES.get(i);
            }
        }
        return DESCRICOES.get(0);
    }

    IrmaoMiau[] lutadores() {
        return new IrmaoMiau[] {branco, preto};
    }

    JButton botao(String texto) {
        return botoes[OPCOES.indexOf(texto)];
    }
}
