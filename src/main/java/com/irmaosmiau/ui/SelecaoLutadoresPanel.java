package com.irmaosmiau.ui;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.persistencia.ResultadoCarga;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.personagens.LimitesAtributos;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

/**
 * Selecao de lutadores com personagens oficiais E perfis personalizados
 * (Entrega 4D). Substitui no fluxo do Main a SelecaoPersonagemPanel, que
 * continua no projeto sem alteracoes.
 *
 * Entrega 5I: cada coluna e um SLOT de controles (Jogador 1 = WASD/H,
 * Jogador 2 = setas/END) e escolhe QUALQUER irmao: o seletor "IRMAO" troca
 * a base da coluna (Branco ou Preto) e o combo lista o oficial e os perfis
 * dessa base. Padrao: Jogador 1 = Branco, Jogador 2 = Preto (como antes).
 * Os nomes internos slotBranco/slotPreto seguem significando "coluna do
 * Jogador 1" e "coluna do Jogador 2".
 *
 * Duas variantes: partida (botao de cada coluna = jogar sozinho com ela /
 * OS DOIS MIAUS) e treino (jogador = coluna 1, parceiro passivo = coluna 2;
 * trocar o irmao do jogador troca o parceiro para o outro irmao).
 */
public final class SelecaoLutadoresPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    static final String NOTA_ATRIBUTOS =
            "Hoje o jogo usa condicionamento, fôlego, velocidade e recuperação; os demais ficam salvos para próximas versões.";

    /** O que fazer com a escolha. */
    @FunctionalInterface
    public interface AoEscolher {
        void escolher(TipoPersonagem tipo, EscolhaLutador slotBranco, EscolhaLutador slotPreto);
    }

    private final boolean modoTreino;
    private final transient AoEscolher aoEscolher;

    private final Slot slotBranco;
    private final Slot slotPreto;
    private final List<String> avisos = new ArrayList<>();

    private final JButton botaoBranco = botao("MIAU BRANCO");
    private final JButton botaoPreto = botao("MIAU PRETO");
    private final JButton botaoAmbos = botao("OS DOIS MIAUS");
    private final JButton botaoTreino = botao("COMEÇAR TREINO");
    private final JButton botaoVoltar = NavegadorMenus.marcarVoltar(botao("VOLTAR"));
    private final JLabel rotuloAvisos = new JLabel(" ");

    /*
     * Entrega 5J: de joelhos ou em pe. Lembrado durante a sessao (a proxima
     * selecao abre com a ultima escolha); padrao: de joelhos.
     */
    private static com.irmaosmiau.entities.InicioLuta inicioDaSessao =
            com.irmaosmiau.entities.InicioLuta.PADRAO_TELAS;
    final JComboBox<com.irmaosmiau.entities.InicioLuta> inicio =
            new JComboBox<>(com.irmaosmiau.entities.InicioLuta.values());

    /** Os dois irmaos oficiais (bases possiveis de cada coluna). */
    static final String[] IRMAOS = {CatalogoPersonagens.ID_BRANCO, CatalogoPersonagens.ID_PRETO};

    /** Uma coluna da tela (um slot de controles). */
    final class Slot {
        final JComboBox<String> irmao = new JComboBox<>(new String[] {"MIAU BRANCO", "MIAU PRETO"});
        final JComboBox<EscolhaLutador> combo = new JComboBox<>();
        final JLabel tipo = new JLabel(" ");
        final JLabel base = new JLabel(" ");
        final JLabel imagem = new JLabel(" ", SwingConstants.CENTER);
        final JPanel atributos = new JPanel(null);
        private final transient OpcoesDeLutadores opcoes;
        private String idBase;
        private Runnable aoTrocarIrmao = () -> { };

        Slot(String idBase, OpcoesDeLutadores opcoes) {
            this.opcoes = opcoes;
            combo.setFont(new Font("Arial", Font.PLAIN, 12));
            combo.addActionListener(ev -> atualizar());
            irmao.setFont(new Font("Arial", Font.BOLD, 12));
            irmao.setSelectedIndex(CatalogoPersonagens.ID_PRETO.equals(idBase) ? 1 : 0);
            irmao.addActionListener(ev -> {
                String novo = IRMAOS[Math.max(0, irmao.getSelectedIndex())];
                if (!novo.equals(this.idBase)) {
                    carregar(novo);
                    aoTrocarIrmao.run();
                }
            });
            tipo.setFont(new Font("Arial", Font.BOLD, 12));
            base.setFont(new Font("Arial", Font.PLAIN, 11));
            atributos.setOpaque(false);
            carregar(idBase);
        }

        /** Repovoa o combo com o oficial e os perfis da base escolhida. */
        private void carregar(String novaBase) {
            idBase = novaBase;
            ResultadoCarga<List<EscolhaLutador>> r = opcoes.paraBase(novaBase);
            for (String aviso : r.getAvisos()) {
                if (!avisos.contains(aviso)) {
                    avisos.add(aviso);
                }
            }
            combo.removeAllItems();
            for (EscolhaLutador e : r.getValor()) {
                combo.addItem(e);
            }
            imagem.setIcon(icone(CatalogoPersonagens.ID_PRETO.equals(novaBase)
                    ? "/assets/sprites/miau-preto.png" : "/assets/sprites/miau-branco.png"));
            atualizar();
        }

        /** Base (irmao) atual da coluna. */
        String idBase() {
            return idBase;
        }

        /** Troca o irmao da coluna (mesmo efeito do seletor). */
        void escolherIrmao(String id) {
            irmao.setSelectedIndex(CatalogoPersonagens.ID_PRETO.equals(id) ? 1 : 0);
        }

        EscolhaLutador escolha() {
            return (EscolhaLutador) combo.getSelectedItem();
        }

        void atualizar() {
            EscolhaLutador e = escolha();
            if (e == null) {
                return;
            }
            tipo.setText(e.isOficial() ? "OFICIAL" : "PERSONALIZADO");
            tipo.setForeground(e.isOficial() ? new Color(30, 80, 30) : new Color(150, 40, 30));
            base.setText("Base: " + e.getBase().getNome());
            preencherAtributos(atributos, e.getAtributos());
            atualizarBotoes();
        }
    }

    public SelecaoLutadoresPanel(OpcoesDeLutadores opcoes, boolean modoTreino,
            AoEscolher aoEscolher, Runnable aoVoltar) {
        if (opcoes == null || aoEscolher == null) {
            throw new IllegalArgumentException("Opcoes e acao de escolha sao obrigatorias.");
        }
        this.modoTreino = modoTreino;
        this.aoEscolher = aoEscolher;

        setPreferredSize(new Dimension(800, 450));
        setLayout(null);

        slotBranco = new Slot(CatalogoPersonagens.ID_BRANCO, opcoes);
        slotPreto = new Slot(CatalogoPersonagens.ID_PRETO, opcoes);

        JLabel titulo = new JLabel(modoTreino ? "TREINO: ESCOLHA OS LUTADORES" : "ESCOLHA SEU MIAU",
                SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 24));
        colocar(titulo, 0, 8, 800, 32);

        montarColuna(slotBranco, 30, modoTreino ? "JOGADOR  (WASD · H pegada)" : "JOGADOR 1  (WASD · H)");
        montarColuna(slotPreto, 410, modoTreino ? "PARCEIRO  (passivo)" : "JOGADOR 2  (setas · END)");
        if (modoTreino) {
            // Treinar com o Preto deixa o Branco de parceiro (e vice-versa); o parceiro pode ser trocado depois.
            slotBranco.aoTrocarIrmao = () -> slotPreto.escolherIrmao(
                    CatalogoPersonagens.ID_PRETO.equals(slotBranco.idBase())
                            ? CatalogoPersonagens.ID_BRANCO : CatalogoPersonagens.ID_PRETO);
        }
        atualizarBotoes();

        inicio.setSelectedItem(inicioDaSessao);
        inicio.setFont(new Font("Arial", Font.BOLD, 13));
        inicio.setToolTipText("Início da luta: de joelhos (jiu-jitsu) ou em pé");
        inicio.addActionListener(e -> inicioDaSessao = getInicio());
        JLabel rotuloInicio = new JLabel("INÍCIO:", SwingConstants.RIGHT);
        rotuloInicio.setFont(new Font("Arial", Font.BOLD, 12));

        if (modoTreino) {
            colocar(rotuloInicio, 30, 338, 80, 28);
            colocar(inicio, 115, 338, 160, 28);
            colocar(botaoTreino, 300, 334, 200, 36);
            botaoTreino.addActionListener(e -> escolher(TipoPersonagem.AMBOS));
        } else {
            colocar(botaoBranco, 115, 316, 190, 32);
            colocar(botaoPreto, 495, 316, 190, 32);
            colocar(botaoAmbos, 300, 354, 200, 32);
            colocar(rotuloInicio, 300, 318, 70, 28);
            colocar(inicio, 375, 318, 110, 28);
            botaoBranco.addActionListener(e -> escolher(TipoPersonagem.BRANCO));
            botaoPreto.addActionListener(e -> escolher(TipoPersonagem.PRETO));
            botaoAmbos.addActionListener(e -> escolher(TipoPersonagem.AMBOS));
        }

        JLabel nota = new JLabel(NOTA_ATRIBUTOS, SwingConstants.CENTER);
        nota.setFont(new Font("Arial", Font.PLAIN, 10));
        nota.setForeground(new Color(40, 50, 60));
        colocar(nota, 150, 392, 640, 14);

        rotuloAvisos.setFont(new Font("Arial", Font.PLAIN, 10));
        rotuloAvisos.setForeground(new Color(150, 30, 20));
        rotuloAvisos.setVerticalAlignment(SwingConstants.TOP);
        colocar(rotuloAvisos, 150, 408, 640, 34);
        if (!avisos.isEmpty()) {
            rotuloAvisos.setText("<html>" + avisos.size() + " aviso(s) de perfis: "
                    + ConfiguracoesPanel.escapar(avisos.get(0)) + "</html>");
        }

        if (aoVoltar != null) {
            colocar(botaoVoltar, 24, 404, 110, 30);
            botaoVoltar.addActionListener(e -> aoVoltar.run());
        }
    }

    private void montarColuna(Slot slot, int x, String cabecalho) {
        JLabel cab = new JLabel(cabecalho);
        cab.setFont(new Font("Arial", Font.BOLD, 13));
        colocar(cab, x, 46, 360, 18);

        colocar(slot.imagem, x, 68, 90, 90);
        colocar(slot.irmao, x + 100, 66, 260, 24);
        colocar(slot.combo, x + 100, 94, 260, 24);
        colocar(slot.tipo, x + 100, 122, 130, 18);
        colocar(slot.base, x + 230, 122, 130, 18);
        colocar(slot.atributos, x, 164, 360, 8 * 17);
    }

    /** 5I: o botao de jogar sozinho mostra o irmao escolhido na coluna. */
    private void atualizarBotoes() {
        if (slotBranco == null || slotPreto == null) {
            return;                                     // ainda montando as colunas
        }
        botaoBranco.setText(rotuloIrmao(slotBranco.idBase()));
        botaoPreto.setText(rotuloIrmao(slotPreto.idBase()));
    }

    private static String rotuloIrmao(String idBase) {
        return CatalogoPersonagens.ID_PRETO.equals(idBase) ? "MIAU PRETO" : "MIAU BRANCO";
    }

    private void escolher(TipoPersonagem tipo) {
        aoEscolher.escolher(tipo, slotBranco.escolha(), slotPreto.escolha());
    }

    private void colocar(java.awt.Component c, int x, int y, int l, int a) {
        c.setBounds(x, y, l, a);
        add(c);
    }

    private static JButton botao(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font("Arial", Font.BOLD, 14));
        return b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(135, 206, 235));            // mesmo fundo da selecao original
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(255, 255, 255, 90));
        g2.fillRoundRect(20, 42, 370, 266, 14, 14);
        g2.fillRoundRect(400, 42, 370, 266, 14, 14);
        g2.dispose();
    }

    // =====================================================
    // ATRIBUTOS
    // =====================================================
    private static void preencherAtributos(JPanel painel, AtributosLutador a) {
        painel.removeAll();
        int y = 0;
        for (LimitesAtributos.Limite l : LimitesAtributos.todos()) {
            double v = l.ler(a);
            JLabel nome = new JLabel(PerfisPanel.nomeVisivel(l.getNome()));
            nome.setFont(new Font("Arial", Font.BOLD, 11));
            nome.setBounds(10, y, 120, 15);
            painel.add(nome);

            JProgressBar barra = new JProgressBar((int) l.getMinimo(), (int) l.getMaximo());
            barra.setValue((int) Math.round(v));
            barra.setBorderPainted(false);
            barra.setBounds(132, y + 3, 170, 9);
            painel.add(barra);

            JLabel valor = new JLabel(v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v));
            valor.setFont(new Font("Arial", Font.PLAIN, 11));
            valor.setBounds(308, y, 50, 15);
            painel.add(valor);
            y += 17;
        }
        painel.revalidate();
        painel.repaint();
    }

    private static ImageIcon icone(String caminho) {
        URL recurso = SelecaoLutadoresPanel.class.getResource(caminho);
        if (recurso == null) {
            return null;
        }
        Image img = new ImageIcon(recurso).getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    /** Entrega 5J: inicio escolhido nesta tela. */
    public com.irmaosmiau.entities.InicioLuta getInicio() {
        Object i = inicio.getSelectedItem();
        return i instanceof com.irmaosmiau.entities.InicioLuta il ? il : com.irmaosmiau.entities.InicioLuta.PADRAO_TELAS;
    }

    /** Ultimo inicio escolhido nesta sessao (o Main usa ao comecar a luta). */
    public static synchronized com.irmaosmiau.entities.InicioLuta inicioDaSessao() {
        return inicioDaSessao;
    }

    /** Testes: volta ao padrao. */
    static synchronized void reiniciarInicioDaSessao() {
        inicioDaSessao = com.irmaosmiau.entities.InicioLuta.PADRAO_TELAS;
    }

    // =====================================================
    // ACESSO PARA TESTES
    // =====================================================
    JComboBox<EscolhaLutador> comboBranco() {
        return slotBranco.combo;
    }

    JComboBox<EscolhaLutador> comboPreto() {
        return slotPreto.combo;
    }

    Slot slotBranco() {
        return slotBranco;
    }

    Slot slotPreto() {
        return slotPreto;
    }

    JButton botaoBranco() {
        return botaoBranco;
    }

    JButton botaoPreto() {
        return botaoPreto;
    }

    JButton botaoAmbos() {
        return botaoAmbos;
    }

    JButton botaoTreino() {
        return botaoTreino;
    }

    String textoAvisos() {
        return rotuloAvisos.getText();
    }

    boolean isModoTreino() {
        return modoTreino;
    }
}
