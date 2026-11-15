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

    public static final List<String> OPCOES = List.of("JOGAR", "TREINO", "PERSONAGENS", "CONFIGURAÇÕES", "SAIR");

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
            b.setBounds(80, 130 + i * 54, 280, 42);
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
        g.setFont(ArteMenus.PEQUENO);
        g.setColor(new Color(10, 30, 80, 170));
        g.fillRoundRect(80, 418, 640, 22, 10, 10);
        g.setColor(ArteMenus.BRANCO);
        ArteMenus.centralizado(g, "↑↓ escolher · ENTER / A confirmar · JOGAR = luta entre 2 jogadores", 400, 433);
        g.dispose();
    }

    IrmaoMiau[] lutadores() {
        return new IrmaoMiau[] {branco, preto};
    }

    JButton botao(String texto) {
        return botoes[OPCOES.indexOf(texto)];
    }
}
