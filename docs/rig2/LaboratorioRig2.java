import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 * SUPERENTREGA 6.1 - Laboratorio interativo do Rig 2.0 (Swing + Java2D).
 *
 * Experimental e isolado: usa somente o modelo do conceito (ConceitoRig2.java, nesta mesma pasta);
 * nao usa nenhuma classe do jogo, nao altera o Main oficial e nao entra no build do Maven.
 *
 * Abrir (Java 22 ou mais novo, a partir da raiz do repositorio):
 *   java docs/rig2/LaboratorioRig2.java
 * Capturas e teste automatico dos controles (sem mostrar a janela):
 *   java docs/rig2/LaboratorioRig2.java --capturas docs/rig2/laboratorio-6.1
 */
public class LaboratorioRig2 {

    static final double CORPO_PADRAO = 40;

    /** Presets simples de braco (valores absolutos de elevacao, avanco e flexao do cotovelo). */
    enum PresetBraco {
        DA_POSE("Da pose", Double.NaN, Double.NaN, Double.NaN),
        LEVANTADO("Levantado", 160, 10, 15),
        ESTENDIDO("Estendido à frente", 0, 90, 0),
        RECOLHIDO("Recolhido junto ao tronco", 5, 25, 135),
        FLEXIONADO("Cotovelo flexionado", Double.NaN, Double.NaN, 110),
        ABAIXADO("Abaixado e solto", 8, 0, 12);

        final String nome;
        final double elevacao, avanco, flexao;
        PresetBraco(String nome, double e, double a, double f) { this.nome = nome; elevacao = e; avanco = a; flexao = f; }
        @Override public String toString() { return nome; }
    }

    // ===================================================================== estado
    double corpo = CORPO_PADRAO;
    ConceitoRig2.ParametrosPose pose = ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.COMBATE);
    boolean esqueleto = false;

    // ===================================================================== interface
    final JFrame janela = new JFrame("Rig 2.0 · Laboratório interativo (6.2) · experimental");
    // 6.2: animacoes (a logica fica em AnimacoesRig2; a janela so pede e avanca o relogio)
    final AnimacoesRig2.Reprodutor reprodutor = new AnimacoesRig2.Reprodutor();
    final javax.swing.Timer relogio = new javax.swing.Timer(15, e -> passoDoRelogio());
    long ultimoNano;
    final JComboBox<ItemAnimacao> cAnimacao = new JComboBox<>();
    final JComboBox<String> cVelocidade = new JComboBox<>(new String[] {"0,5×", "1,0×", "1,5×", "2,0×"});
    final JButton bTocar = new JButton("▶ Reproduzir"), bPausar = new JButton("❚❚ Pausar"),
            bParar = new JButton("■ Parar"), bReiniciar = new JButton("↺ Reiniciar");
    final JCheckBox chTransicao = new JCheckBox("Transição animada ao trocar a pose", true);
    final JLabel lAnimacao = new JLabel();

    /** Item da lista de animacoes. */
    record ItemAnimacao(String id, String nome) {
        @Override public String toString() { return nome; }
    }

    /** Pose desenhada: a da animacao (com o giro manual da cabeca) ou a manual, quando nada toca. */
    ConceitoRig2.ParametrosPose exibida() { return reprodutor.pose(pose); }
    final Visualizador vista = new Visualizador();
    final JSlider sCorpo = new JSlider(0, 180, (int) CORPO_PADRAO);
    final JSlider sCabeca = new JSlider((int) -ConceitoRig2.GIRO_CABECA_MAX, (int) ConceitoRig2.GIRO_CABECA_MAX, 0);
    final JLabel lCorpo = new JLabel(), lCabeca = new JLabel(), lStatus = new JLabel();
    final JComboBox<ConceitoRig2.PoseBase> cPose = new JComboBox<>(ConceitoRig2.PoseBase.values());
    final JComboBox<ConceitoRig2.PresetPernas> cPernas = new JComboBox<>(ConceitoRig2.PresetPernas.values());
    final JCheckBox chEsqueleto = new JCheckBox("Mostrar esqueleto e diagnóstico");
    final ControlesBraco[] bracos = {new ControlesBraco(0, "Braço direito"), new ControlesBraco(1, "Braço esquerdo")};
    final JButton bRestaurarPose = new JButton("Restaurar pose"), bRestaurarVista = new JButton("Restaurar perspectiva (40°)");
    final List<JButton> atalhosVista = new ArrayList<>();
    boolean sincronizando;

    /** Controles de um braco: preset + elevacao, avanco e flexao do cotovelo. */
    final class ControlesBraco {
        final int i;
        final JComboBox<PresetBraco> preset = new JComboBox<>(PresetBraco.values());
        final JSlider elevacao = new JSlider((int) ConceitoRig2.ELEVACAO_MIN, (int) ConceitoRig2.ELEVACAO_MAX, 0);
        final JSlider avanco = new JSlider((int) ConceitoRig2.AVANCO_MIN, (int) ConceitoRig2.AVANCO_MAX, 0);
        final JSlider flexao = new JSlider((int) ConceitoRig2.FLEXAO_MIN, (int) ConceitoRig2.FLEXAO_MAX, 0);
        final JLabel lElev = new JLabel(), lAv = new JLabel(), lFlex = new JLabel();
        final JPanel painel = new JPanel();

        ControlesBraco(int i, String titulo) {
            this.i = i;
            painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
            painel.setBorder(new TitledBorder(titulo));
            linha(painel, new JLabel("Preset"), preset);
            linha(painel, lElev, elevacao);
            linha(painel, lAv, avanco);
            linha(painel, lFlex, flexao);
            preset.addActionListener(e -> { if (!sincronizando) aplicarPresetBraco(i, (PresetBraco) preset.getSelectedItem()); });
            for (JSlider s : new JSlider[] {elevacao, avanco, flexao}) s.addChangeListener(e -> {
                if (sincronizando) return;
                pose.elevacao[i] = elevacao.getValue();
                pose.avanco[i] = avanco.getValue();
                pose.flexao[i] = flexao.getValue();
                atualizar();
            });
        }

        void sincronizar() {
            ConceitoRig2.ParametrosPose ex = exibida(); // durante a animacao os controles mostram os valores animados
            elevacao.setValue((int) Math.round(ex.elevacao[i]));
            avanco.setValue((int) Math.round(ex.avanco[i]));
            flexao.setValue((int) Math.round(ex.flexao[i]));
            lElev.setText(String.format("Elevação lateral: %+d°", (int) Math.round(ex.elevacao[i])));
            lAv.setText(String.format("Avanço (à frente): %+d°", (int) Math.round(ex.avanco[i])));
            lFlex.setText(String.format("Cotovelo flexionado: %d°", (int) Math.round(ex.flexao[i])));
            boolean livre = !reprodutor.ativo();
            for (JComponent c : new JComponent[] {preset, elevacao, avanco, flexao}) c.setEnabled(livre);
        }
    }

    /** Painel de controles que acompanha a largura da rolagem (nada fica cortado a direita). */
    static final class PainelRolavel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    static void linha(JPanel p, JComponent rotulo, JComponent controle) {
        rotulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        controle.setAlignmentX(Component.LEFT_ALIGNMENT);
        controle.setMaximumSize(new Dimension(Integer.MAX_VALUE, controle.getPreferredSize().height));
        p.add(rotulo);
        p.add(controle);
    }

    LaboratorioRig2() {
        JPanel controles = new PainelRolavel();
        controles.setLayout(new BoxLayout(controles, BoxLayout.Y_AXIS));
        controles.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel pVista = new JPanel();
        pVista.setLayout(new BoxLayout(pVista, BoxLayout.Y_AXIS));
        pVista.setBorder(new TitledBorder("Perspectiva"));
        Hashtable<Integer, JLabel> marcas = new Hashtable<>();
        for (int a : new int[] {0, 40, 90, 140, 180}) marcas.put(a, new JLabel(a + "°"));
        sCorpo.setLabelTable(marcas);
        sCorpo.setPaintLabels(true);
        sCorpo.setMajorTickSpacing(10);
        sCorpo.setPaintTicks(true);
        linha(pVista, lCorpo, sCorpo);
        JPanel atalhos = new JPanel(new GridLayout(1, 5, 4, 0));
        String[] nomes = {"0°", "40°", "90°", "140°", "180°"};
        int[] angulos = {0, 40, 90, 140, 180};
        for (int k = 0; k < 5; k++) {
            int a = angulos[k];
            JButton b = new JButton(nomes[k]);
            b.setMargin(new Insets(2, 2, 2, 2));
            b.setToolTipText(new String[] {"Frente", "3/4 frontal", "Perfil", "3/4 costas", "Costas"}[k]);
            b.addActionListener(e -> sCorpo.setValue(a));
            atalhos.add(b);
            atalhosVista.add(b);
        }
        atalhos.setAlignmentX(Component.LEFT_ALIGNMENT);
        atalhos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        pVista.add(atalhos);
        linha(pVista, lCabeca, sCabeca);
        controles.add(pVista);

        JPanel pAnim = new JPanel();
        pAnim.setLayout(new BoxLayout(pAnim, BoxLayout.Y_AXIS));
        pAnim.setBorder(new TitledBorder("Animações (6.2)"));
        for (String id : AnimacoesRig2.CATALOGO) cAnimacao.addItem(new ItemAnimacao(id, AnimacoesRig2.nome(id)));
        for (String id : MovimentosRig2.IDS) cAnimacao.addItem(new ItemAnimacao(id, AnimacoesRig2.nome(id))); // 6.3: corrida, agachamento e pulo
        linha(pAnim, new JLabel("Animação"), cAnimacao);
        JPanel transporte = new JPanel(new GridLayout(2, 2, 4, 4));
        for (JButton b : new JButton[] {bTocar, bPausar, bParar, bReiniciar}) { b.setMargin(new Insets(2, 2, 2, 2)); transporte.add(b); }
        transporte.setAlignmentX(Component.LEFT_ALIGNMENT);
        transporte.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        pAnim.add(transporte);
        cVelocidade.setSelectedIndex(1);
        linha(pAnim, new JLabel("Velocidade"), cVelocidade);
        chTransicao.setAlignmentX(Component.LEFT_ALIGNMENT);
        pAnim.add(chTransicao);
        lAnimacao.setAlignmentX(Component.LEFT_ALIGNMENT);
        pAnim.add(lAnimacao);
        controles.add(pAnim);

        JPanel pPose = new JPanel();
        pPose.setLayout(new BoxLayout(pPose, BoxLayout.Y_AXIS));
        pPose.setBorder(new TitledBorder("Pose"));
        linha(pPose, new JLabel("Pose-base"), cPose);
        linha(pPose, new JLabel("Pernas (preset experimental)"), cPernas);
        chEsqueleto.setAlignmentX(Component.LEFT_ALIGNMENT);
        pPose.add(chEsqueleto);
        controles.add(pPose);

        for (ControlesBraco cb : bracos) controles.add(cb.painel);

        JPanel botoes = new JPanel(new GridLayout(2, 1, 0, 4));
        botoes.add(bRestaurarPose);
        botoes.add(bRestaurarVista);
        botoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        botoes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        controles.add(Box.createVerticalStrut(6));
        controles.add(botoes);
        controles.add(Box.createVerticalStrut(6));
        lStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        controles.add(lStatus);
        JLabel teclas = new JLabel("<html><small>Teclado: ← → ou A/D giram o corpo 1°<br>(Shift: 10°) · Q/E giram a cabeça<br>R restaura a perspectiva</small></html>");
        teclas.setAlignmentX(Component.LEFT_ALIGNMENT);
        controles.add(teclas);
        for (Component c : controles.getComponents()) if (c instanceof JComponent j) j.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane rolagem = new JScrollPane(controles, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setPreferredSize(new Dimension(330, 760));
        rolagem.getVerticalScrollBar().setUnitIncrement(16);

        janela.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        janela.add(vista, BorderLayout.CENTER);
        janela.add(rolagem, BorderLayout.EAST);
        vista.setPreferredSize(new Dimension(760, 760));

        sCorpo.addChangeListener(e -> { if (!sincronizando) { corpo = sCorpo.getValue(); atualizar(); } });
        sCabeca.addChangeListener(e -> { if (!sincronizando) { pose.giroCabeca = sCabeca.getValue(); atualizar(); } });
        cPose.setSelectedItem(pose.base);
        cPose.addActionListener(e -> { if (!sincronizando) trocarPose((ConceitoRig2.PoseBase) cPose.getSelectedItem()); });
        cPernas.addActionListener(e -> { if (!sincronizando) { pose.pernas = (ConceitoRig2.PresetPernas) cPernas.getSelectedItem(); atualizar(); } });
        chEsqueleto.addActionListener(e -> { esqueleto = chEsqueleto.isSelected(); atualizar(); });
        bRestaurarPose.addActionListener(e -> restaurarPose());
        bRestaurarVista.addActionListener(e -> restaurarPerspectiva());
        bTocar.addActionListener(e -> tocar());
        bPausar.addActionListener(e -> { reprodutor.pausar(); atualizar(); });
        bParar.addActionListener(e -> parar());
        bReiniciar.addActionListener(e -> { reprodutor.reiniciar(); iniciarRelogio(); atualizar(); });
        cVelocidade.addActionListener(e -> reprodutor.velocidade = new double[] {.5, 1, 1.5, 2}[cVelocidade.getSelectedIndex()]);
        teclado();
        atualizar();
        janela.pack();
        janela.setLocationRelativeTo(null);
    }

    void teclado() {
        JRootPane raiz = janela.getRootPane();
        InputMap im = raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = raiz.getActionMap();
        Object[][] mapa = {
            {"LEFT", -1.0, 0.0}, {"RIGHT", 1.0, 0.0}, {"A", -1.0, 0.0}, {"D", 1.0, 0.0},
            {"shift LEFT", -10.0, 0.0}, {"shift RIGHT", 10.0, 0.0}, {"shift A", -10.0, 0.0}, {"shift D", 10.0, 0.0},
            {"Q", 0.0, -1.0}, {"E", 0.0, 1.0}};
        for (Object[] m : mapa) {
            String tecla = (String) m[0];
            double dc = (Double) m[1], dh = (Double) m[2];
            im.put(KeyStroke.getKeyStroke(tecla), tecla);
            am.put(tecla, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    if (dc != 0) sCorpo.setValue((int) ConceitoRig2.limitar(sCorpo.getValue() + dc, 0, 180));
                    if (dh != 0) sCabeca.setValue((int) ConceitoRig2.limitar(sCabeca.getValue() + dh,
                            -ConceitoRig2.GIRO_CABECA_MAX, ConceitoRig2.GIRO_CABECA_MAX));
                }
            });
        }
        im.put(KeyStroke.getKeyStroke("R"), "restaurar");
        am.put("restaurar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { restaurarPerspectiva(); }
        });
    }

    // ===================================================================== acoes
    void trocarPose(ConceitoRig2.PoseBase b) {
        ConceitoRig2.ParametrosPose antes = exibida().copia();
        boolean mudou = b != exibidaBase();
        ConceitoRig2.ParametrosPose nova = ConceitoRig2.ParametrosPose.padrao(b);
        nova.pernas = pose.pernas;
        nova.giroCabeca = pose.giroCabeca;
        pose = nova;           // o angulo do corpo e preservado
        if (chTransicao.isSelected() && mudou) {
            // 6.2: a troca vira uma transicao animada que parte da pose mostrada agora e segue para o idle
            antes.giroCabeca -= pose.giroCabeca; // o giro manual e somado de novo pelo reprodutor
            String id = b == ConceitoRig2.PoseBase.COMBATE ? AnimacoesRig2.NEUTRA_PARA_COMBATE : AnimacoesRig2.COMBATE_PARA_NEUTRA;
            selecionarAnimacao(id);
            reprodutor.tocar(AnimacoesRig2.criar(id, antes));
            iniciarRelogio();
        } else if (reprodutor.ativo()) {
            reprodutor.parar();
            relogio.stop();
        }
        atualizar();
    }

    ConceitoRig2.PoseBase exibidaBase() { return reprodutor.ativo() ? reprodutor.atual.baseFinal() : pose.base; }

    // ---------------------------------------------------------------- reproducao (6.2)
    void tocar() {
        ItemAnimacao item = (ItemAnimacao) cAnimacao.getSelectedItem();
        if (item == null) return;
        if (reprodutor.estado == AnimacoesRig2.Reprodutor.Estado.PAUSADO && reprodutor.atual != null
                && reprodutor.atual.id().equals(item.id())) {
            reprodutor.continuar();
        } else {
            ConceitoRig2.ParametrosPose inicio = exibida().copia();
            inicio.giroCabeca -= pose.giroCabeca;
            reprodutor.tocar(AnimacoesRig2.criar(item.id(), inicio));
        }
        iniciarRelogio();
        atualizar();
    }

    /** Parar: a animacao sai e volta a edicao manual (a pose de antes, ou a nova base apos uma transicao). */
    void parar() {
        reprodutor.parar();
        relogio.stop();
        atualizar();
    }

    void iniciarRelogio() {
        ultimoNano = System.nanoTime();
        if (!relogio.isRunning()) relogio.start();
    }

    /** Chamado pelo Timer: avanca pelo tempo real decorrido (nao pela quantidade de repinturas). */
    void passoDoRelogio() {
        long agora = System.nanoTime();
        double dt = (agora - ultimoNano) / 1e9;
        ultimoNano = agora;
        avancarRelogio(dt);
    }

    void avancarRelogio(double dt) {
        if (!reprodutor.ativo()) { relogio.stop(); return; }
        if (reprodutor.avancar(dt)) {
            // a transicao terminou e o idle assumiu: a edicao manual passa a ser a nova base
            ConceitoRig2.ParametrosPose nova = ConceitoRig2.ParametrosPose.padrao(reprodutor.atual.baseFinal());
            nova.giroCabeca = pose.giroCabeca;
            nova.pernas = pose.pernas;
            pose = nova;
            selecionarAnimacao(reprodutor.atual.id());
        }
        atualizar();
    }

    void selecionarAnimacao(String id) {
        boolean antes = sincronizando;
        sincronizando = true;
        for (int k = 0; k < cAnimacao.getItemCount(); k++) if (cAnimacao.getItemAt(k).id().equals(id)) cAnimacao.setSelectedIndex(k);
        sincronizando = antes;
    }

    String estadoAnimacao() {
        if (!reprodutor.ativo()) return "Parado · edição manual";
        String est = reprodutor.estado == AnimacoesRig2.Reprodutor.Estado.TOCANDO ? "Tocando" : "Pausado";
        String ciclo = reprodutor.atual.emLoop() ? " · ciclo " + reprodutor.ciclo() : "";
        return String.format("%s · %.2f s / %.2f s%s · %s", est, reprodutor.tempoNoCiclo(), reprodutor.atual.duracao(), ciclo,
                cVelocidade.getSelectedItem());
    }

    void aplicarPresetBraco(int i, PresetBraco p) {
        ConceitoRig2.ParametrosPose base = ConceitoRig2.ParametrosPose.padrao(pose.base);
        if (p == PresetBraco.DA_POSE) {
            pose.elevacao[i] = base.elevacao[i];
            pose.avanco[i] = base.avanco[i];
            pose.flexao[i] = base.flexao[i];
        } else {
            if (!Double.isNaN(p.elevacao)) pose.elevacao[i] = p.elevacao;
            if (!Double.isNaN(p.avanco)) pose.avanco[i] = p.avanco;
            pose.flexao[i] = p.flexao;
        }
        atualizar();
    }

    void restaurarPose() {
        if (reprodutor.ativo()) { reprodutor.parar(); relogio.stop(); }
        pose = ConceitoRig2.ParametrosPose.padrao(pose.base);
        for (ControlesBraco cb : bracos) { sincronizando = true; cb.preset.setSelectedItem(PresetBraco.DA_POSE); sincronizando = false; }
        atualizar();
    }

    void restaurarPerspectiva() {
        corpo = CORPO_PADRAO;
        pose.giroCabeca = 0;
        atualizar();
    }

    /** Recalcula, sincroniza os controles e redesenha. */
    void atualizar() {
        pose.limitarTudo();
        sincronizando = true;
        try {
            sCorpo.setValue((int) Math.round(corpo));
            sCabeca.setValue((int) Math.round(pose.giroCabeca));
            cPose.setSelectedItem(exibidaBase());
            cPernas.setSelectedItem(pose.pernas);
            cPernas.setEnabled(!reprodutor.ativo()); // a animacao controla as pernas enquanto toca
            chEsqueleto.setSelected(esqueleto);
            for (ControlesBraco cb : bracos) cb.sincronizar();
        } finally {
            sincronizando = false;
        }
        double rosto = ConceitoRig2.Desenho.guinadaCabeca(corpo + pose.giroCabeca);
        lCorpo.setText(String.format("Corpo: %d°  (%s)", (int) Math.round(corpo), nomeVista(corpo)));
        lCabeca.setText(String.format("Cabeça: %+d° em relação ao tronco (rosto a %.0f°)",
                (int) Math.round(pose.giroCabeca), rosto));
        lStatus.setText("<html>" + estadoOssos() + "</html>");
        lAnimacao.setText("<html><small>" + estadoAnimacao() + "</small></html>");
        vista.repaint();
    }

    static String nomeVista(double g) {
        if (g < 20) return "frente";
        if (g < 65) return "3/4 frontal";
        if (g < 115) return "perfil";
        if (g < 160) return "3/4 costas";
        return "costas";
    }

    /** Confere os comprimentos dos ossos da pose atual (mostrado no painel). */
    String estadoOssos() {
        ConceitoRig2.Corpo c = ConceitoRig2.montarPose(exibida());
        double pior = 0;
        double[] esperado = {ConceitoRig2.BRACO, ConceitoRig2.ANTEBRACO, ConceitoRig2.COXA, ConceitoRig2.CANELA};
        for (int i = 0; i < 2; i++) {
            double[] real = {c.cotovelo[i].menos(c.ombro[i]).tam(), c.mao[i].menos(c.cotovelo[i]).tam(),
                    c.joelho[i].menos(c.quadril[i]).tam(), c.tornozelo[i].menos(c.joelho[i]).tam()};
            for (int k = 0; k < 4; k++) pior = Math.max(pior, Math.abs(real[k] - esperado[k]));
        }
        return pior < 1e-6
                ? "<font color='#2e7d32'>Ossos com comprimento constante ✓</font><br><small>braço 39 · antebraço 30<br>coxa 46,5 · canela 46</small>"
                : "<font color='#c62828'>Comprimento de osso alterado: " + pior + "</font>";
    }

    // ===================================================================== area de visualizacao
    final class Visualizador extends JPanel {
        Visualizador() { setBackground(ConceitoRig2.FUNDO); setFocusable(true); }

        @Override protected void paintComponent(Graphics gr) {
            super.paintComponent(gr);
            Graphics2D g = (Graphics2D) gr.create();
            desenhar(g, getWidth(), getHeight());
            g.dispose();
        }
    }

    /** Desenha a cena completa (tambem usado nas capturas). */
    void desenhar(Graphics2D g, int w, int h) {
        ConceitoRig2.Pintor p = new ConceitoRig2.Pintor(g, w, h);
        ConceitoRig2.fundo(p);
        ConceitoRig2.cartao(p, 12, 12, w - 24, h - 24);
        ConceitoRig2.ParametrosPose ex = exibida();
        ConceitoRig2.Corpo c = ConceitoRig2.montarPose(ex);
        double esc = Math.max(.3, Math.min((h - 120) / 315.0, (w - 80) / 240.0));
        double ox = w / 2.0, chao = h - 58;
        // linha do chao
        p.linha(new Color(166, 176, 190, 60), 1, 30, chao + 1, w - 30, chao + 1);
        marcasDoChao(p, corpo, reprodutor.deslocamento(), ox, chao, esc);
        ConceitoRig2.figura(p, c, corpo, ox, chao, esc, 1, false);
        if (esqueleto) diagnostico(p, c, ox, chao, esc);
        // textos (coordenadas da tela)
        double rosto = ConceitoRig2.Desenho.guinadaCabeca(corpo + ex.giroCabeca);
        p.texto(String.format("CORPO %d°  ·  %s", (int) Math.round(corpo), nomeVista(corpo).toUpperCase(Locale.ROOT)),
                32, 42, 16, ConceitoRig2.TEXTO, true, -1);
        String pernas = reprodutor.ativo() ? "da animação" : pose.pernas.nome.toLowerCase(Locale.ROOT);
        p.texto(String.format("cabeça %+d° em relação ao tronco  ·  rosto a %.0f°  ·  pose %s  ·  pernas: %s",
                (int) Math.round(pose.giroCabeca), rosto, exibidaBase().nome.toLowerCase(Locale.ROOT), pernas),
                32, 62, 11.5, ConceitoRig2.SUAVE, false, -1);
        if (reprodutor.ativo())
            p.texto((reprodutor.estado == AnimacoesRig2.Reprodutor.Estado.TOCANDO ? "▶ " : "❚❚ ") + reprodutor.atual.nome()
                    + String.format(Locale.ROOT, "  ·  %.2f s", reprodutor.tempoNoCiclo()), 32, 82, 11.5, ConceitoRig2.JUNTA, false, -1);
        p.texto("Rig 2.0 · laboratório experimental 6.2", w - 32, h - 26, 10.5, ConceitoRig2.SUAVE, false, 1);
    }

    /**
     * 6.2: marcas no chao que andam para tras na velocidade da caminhada no lugar. O pe de apoio anda
     * junto com elas, entao da para ver que ele nao escorrega em relacao ao chao.
     */
    static void marcasDoChao(ConceitoRig2.Pintor p, double guinada, double deslocamento, double ox, double chao, double esc) {
        if (deslocamento == 0) return;
        double esp = 24;
        p.salvar();
        p.mover(ox, chao);
        p.escalar(esc);
        p.mover(0, -ConceitoRig2.CHAO);
        ConceitoRig2.Camera cam = new ConceitoRig2.Camera(guinada);
        double base = deslocamento - Math.floor(deslocamento / esp) * esp;
        for (double z = -96 - base; z <= 96; z += esp) {
            double alfa = 1 - Math.abs(z) / 100;
            if (alfa <= 0) continue;
            for (int lado = -1; lado <= 1; lado += 2) {
                Point2D q = cam.p(new ConceitoRig2.V(lado * 30, ConceitoRig2.CHAO + 1, z));
                p.preencher(new Ellipse2D.Double(q.getX() - 3, q.getY() - 1, 6, 2), new Color(166, 176, 190, (int) (150 * alfa)));
            }
        }
        p.restaurar();
    }

    /** Esqueleto + diagnostico: eixo do tronco, centro e direcao da cabeca, lados perto/longe. */
    void diagnostico(ConceitoRig2.Pintor p, ConceitoRig2.Corpo c, double ox, double chao, double esc) {
        p.salvar();
        p.mover(ox, chao);
        p.escalar(esc);
        p.mover(0, -ConceitoRig2.CHAO);
        ConceitoRig2.Desenho d = new ConceitoRig2.Desenho(p, c, corpo);
        ConceitoRig2.Camera cam = d.cam;
        double t = 8 / Math.max(esc, .5) * 1.4;
        // eixo do tronco (pivo da cintura -> pescoco), prolongado ate o chao
        Point2D cintura = cam.p(c.transformar(new ConceitoRig2.V(0, ConceitoRig2.PIVO_CINTURA, 0)));
        Point2D pesc = cam.p(c.pescoco);
        p.linha(new Color(100, 215, 240, 140), 1, cintura.getX(), cintura.getY(), pesc.getX(), pesc.getY());
        d.esqueleto(false);
        // direcao do rosto
        Point2D cab = cam.p(c.cabeca);
        double r = Math.toRadians(d.guinadaRosto());
        p.linha(ConceitoRig2.JUNTA, 1.4, cab.getX(), cab.getY(), cab.getX() + Math.sin(r) * 30, cab.getY());
        // rotulo fora da cabeca (texto claro sobre o pelo branco nao seria legivel)
        double ladoRotulo = Math.sin(r) >= 0 ? -1 : 1;
        p.texto("centro da cabeça", cab.getX() + ladoRotulo * 52, cab.getY() - 30, t, ConceitoRig2.TEXTO, false, ladoRotulo < 0 ? 1 : -1);
        p.linha(new Color(200, 205, 214, 150), .6, cab.getX(), cab.getY(), cab.getX() + ladoRotulo * 50, cab.getY() - 28);
        p.texto("pelve (raiz)", cam.p(c.pelve).getX() + 9, cam.p(c.pelve).getY() + 4, t, ConceitoRig2.TEXTO, false, -1);
        int perto = d.ladoPerto();
        for (int i = 0; i < 2; i++) {
            String lado = (i == 0 ? "D" : "E") + (perto < 0 ? "" : i == perto ? " · perto" : " · longe");
            Point2D o = cam.p(c.ombro[i]), q = cam.p(c.quadril[i]);
            double dx = (o.getX() < pesc.getX()) ? -1 : 1;
            p.texto("ombro " + lado, o.getX() + dx * 8, o.getY() - 8, t, ConceitoRig2.TEXTO, false, dx < 0 ? 1 : -1);
            p.texto("quadril " + (i == 0 ? "D" : "E"), q.getX() + dx * 8, q.getY() + 3, t, ConceitoRig2.SUAVE, false, dx < 0 ? 1 : -1);
        }
        p.restaurar();
    }

    // ===================================================================== capturas e teste dos controles
    BufferedImage capturarJanela() {
        janela.getRootPane().validate();
        Dimension d = janela.getContentPane().getSize();
        BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        janela.getContentPane().paint(g);
        g.dispose();
        return img;
    }

    BufferedImage capturarVista(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        desenhar(g, w, h);
        g.dispose();
        return img;
    }

    static void salvar(BufferedImage img, Path arq) throws Exception {
        ImageIO.write(img, "png", arq.toFile());
        System.out.println("gerado: " + arq);
    }

    /** Folha de contato: quadros da rotacao lado a lado, com o angulo embaixo. */
    static BufferedImage folha(ConceitoRig2.Corpo c, int[] angulos, int colunas, String titulo) {
        int qw = 150, qh = 230, topo = 40, rot = 22, linhas = (angulos.length + colunas - 1) / colunas;
        BufferedImage img = new BufferedImage(colunas * qw, topo + linhas * (qh + rot), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(ConceitoRig2.FUNDO);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(ConceitoRig2.TEXTO);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString(titulo, 12, 26);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        for (int k = 0; k < angulos.length; k++) {
            int x = (k % colunas) * qw, y = topo + (k / colunas) * (qh + rot);
            g.drawImage(ConceitoRig2.quadro(c, angulos[k], qw, qh, .7, false), x, y, null);
            g.setColor(ConceitoRig2.SUAVE);
            String s = angulos[k] + "°";
            g.drawString(s, x + qw / 2 - g.getFontMetrics().stringWidth(s) / 2, y + qh + 15);
        }
        g.dispose();
        return img;
    }

    static int[] faixa(int de, int ate, int passo) {
        int[] r = new int[(ate - de) / passo + 1];
        for (int k = 0; k < r.length; k++) r[k] = de + k * passo;
        return r;
    }

    /**
     * Gera as capturas obrigatorias e exercita todos os controles (sliders nos extremos, todos os
     * presets, botoes e teclado), registrando qualquer excecao.
     */
    static void capturas(Path dir) throws Exception {
        Files.createDirectories(dir);
        Path seq = dir.resolve("rotacao");
        Files.createDirectories(seq);
        List<Throwable> erros = Collections.synchronizedList(new ArrayList<>());
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> erros.add(e));
        final LaboratorioRig2[] ref = new LaboratorioRig2[1];
        SwingUtilities.invokeAndWait(() -> {
            ref[0] = new LaboratorioRig2();
            ref[0].janela.setSize(1180, 820);
            ref[0].janela.validate();
        });
        LaboratorioRig2 lab = ref[0];
        SwingUtilities.invokeAndWait(() -> lab.chTransicao.setSelected(false)); // rotina da 6.1: troca instantanea
        // 1) perspectivas (janela inteira, pose de combate)
        int[] angulos = {0, 40, 65, 90, 140, 180};
        for (int a : angulos) {
            SwingUtilities.invokeAndWait(() -> lab.sCorpo.setValue(a));
            BufferedImage[] img = new BufferedImage[1];
            SwingUtilities.invokeAndWait(() -> img[0] = lab.capturarJanela());
            salvar(img[0], dir.resolve(String.format("janela-%03d.png", a)));
        }
        // 2) teste 7.3: 40 -> flexiona o direito, estende o esquerdo -> 90 -> 140 -> 0
        SwingUtilities.invokeAndWait(() -> {
            lab.sCorpo.setValue(40);
            lab.bracos[0].preset.setSelectedItem(PresetBraco.RECOLHIDO);
            lab.bracos[1].preset.setSelectedItem(PresetBraco.ESTENDIDO);
        });
        BufferedImage[] janela73 = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> janela73[0] = lab.capturarJanela());
        salvar(janela73[0], dir.resolve("janela-bracos-flexionado-e-estendido.png"));
        List<ConceitoRig2.V> antes = ConceitoRig2.juntas(ConceitoRig2.montarPose(lab.pose));
        int[] seq73 = {40, 90, 140, 0};
        BufferedImage tira = new BufferedImage(4 * 300, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D gt = tira.createGraphics();
        for (int k = 0; k < 4; k++) {
            int a = seq73[k];
            SwingUtilities.invokeAndWait(() -> lab.sCorpo.setValue(a));
            BufferedImage[] v = new BufferedImage[1];
            SwingUtilities.invokeAndWait(() -> v[0] = lab.capturarVista(300, 400));
            gt.drawImage(v[0], k * 300, 0, null);
        }
        gt.dispose();
        salvar(tira, dir.resolve("teste-7.3-rotacao-com-bracos.png"));
        List<ConceitoRig2.V> depois = ConceitoRig2.juntas(ConceitoRig2.montarPose(lab.pose));
        if (!antes.equals(depois)) throw new AssertionError("a pose mudou durante o teste 7.3");
        System.out.println("OK: teste 7.3 - articulacoes identicas apos 40 -> 90 -> 140 -> 0.");
        // 3) esqueleto em 3/4
        SwingUtilities.invokeAndWait(() -> {
            lab.sCorpo.setValue(40);
            lab.chEsqueleto.doClick();
        });
        BufferedImage[] esq = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> esq[0] = lab.capturarJanela());
        salvar(esq[0], dir.resolve("janela-esqueleto-3-4.png"));
        // 4) pose neutra com cabeca girada e pernas
        SwingUtilities.invokeAndWait(() -> {
            lab.chEsqueleto.doClick();
            lab.bRestaurarPose.doClick();
            lab.cPose.setSelectedItem(ConceitoRig2.PoseBase.NEUTRA);
            lab.sCorpo.setValue(65);
            lab.sCabeca.setValue(25);
            lab.cPernas.setSelectedItem(ConceitoRig2.PresetPernas.ESQUERDA_FRENTE);
        });
        BufferedImage[] neu = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> neu[0] = lab.capturarJanela());
        salvar(neu[0], dir.resolve("janela-neutra-cabeca-e-perna.png"));

        // 5) sequencias de rotacao (folhas de contato)
        ConceitoRig2.Corpo comb = ConceitoRig2.combate(), neutra = ConceitoRig2.neutro();
        ConceitoRig2.Corpo teste = ConceitoRig2.montarPose(ConceitoRig2.poseTesteBracos());
        salvar(folha(comb, faixa(0, 180, 5), 13, "Rotação 0° a 180° (passo 5°) · pose de combate"), seq.resolve("combate-passo-5.png"));
        salvar(folha(neutra, faixa(0, 180, 5), 13, "Rotação 0° a 180° (passo 5°) · pose neutra"), seq.resolve("neutra-passo-5.png"));
        salvar(folha(teste, faixa(0, 180, 10), 10, "Rotação com braço direito flexionado e esquerdo estendido (passo 10°)"),
                seq.resolve("teste-bracos-passo-10.png"));
        int[] criticos = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 38, 39, 40, 41, 42, 88, 89, 90, 91, 92, 93, 94, 95, 96, 138, 139, 140, 141, 142};
        salvar(folha(comb, criticos, 10, "Ângulos críticos (passo 1°) · 0-10, 38-42, 88-96, 138-142 · combate"), seq.resolve("combate-angulos-criticos.png"));

        // 6) exercita todos os controles (extremos, presets, botoes, teclado)
        SwingUtilities.invokeAndWait(() -> {
            for (ConceitoRig2.PoseBase b : ConceitoRig2.PoseBase.values()) {
                lab.cPose.setSelectedItem(b);
                for (ConceitoRig2.PresetPernas pr : ConceitoRig2.PresetPernas.values()) lab.cPernas.setSelectedItem(pr);
                for (ControlesBraco cb : lab.bracos) {
                    for (PresetBraco pb : PresetBraco.values()) cb.preset.setSelectedItem(pb);
                    for (JSlider s : new JSlider[] {cb.elevacao, cb.avanco, cb.flexao}) {
                        s.setValue(s.getMinimum());
                        lab.capturarVista(200, 260);
                        s.setValue(s.getMaximum());
                        lab.capturarVista(200, 260);
                    }
                }
                for (int a = 0; a <= 180; a += 15) { lab.sCorpo.setValue(a); lab.sCabeca.setValue(a % 2 == 0 ? 60 : -60); lab.capturarVista(200, 260); }
                lab.bRestaurarPose.doClick();
                lab.bRestaurarVista.doClick();
                lab.chEsqueleto.doClick();
                lab.capturarVista(200, 260);
                lab.chEsqueleto.doClick();
            }
            for (String acao : new String[] {"RIGHT", "shift RIGHT", "LEFT", "Q", "E", "restaurar"})
                lab.janela.getRootPane().getActionMap().get(acao).actionPerformed(new ActionEvent(lab.janela, 0, acao));
        });
        if (Math.round(lab.corpo) != (long) CORPO_PADRAO) throw new AssertionError("restaurar perspectiva falhou");
        SwingUtilities.invokeAndWait(lab.janela::dispose);
        if (!erros.isEmpty()) throw new AssertionError("excecoes durante o uso dos controles: " + erros);
        System.out.println("OK: todos os controles exercitados (extremos dos sliders, presets, botoes e teclado) sem excecoes.");
    }

    // ===================================================================== capturas da 6.2 (animacoes)
    /** Um quadro de animacao: fundo, marcas do chao, personagem e, opcionalmente, o esqueleto. */
    static BufferedImage quadroAnim(ConceitoRig2.ParametrosPose pose, double guinada, double deslocamento, boolean esqueleto,
                                    int w, int h, double esc) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        ConceitoRig2.Pintor p = new ConceitoRig2.Pintor(g, w, h);
        ConceitoRig2.fundo(p);
        ConceitoRig2.Corpo c = ConceitoRig2.montarPose(pose);
        p.linha(new Color(166, 176, 190, 50), 1, 6, h - 23, w - 6, h - 23);
        marcasDoChao(p, guinada, deslocamento, w / 2.0, h - 24, esc);
        ConceitoRig2.figura(p, c, guinada, w / 2.0, h - 24, esc, 1, false);
        if (esqueleto) {
            p.salvar();
            p.mover(w / 2.0, h - 24);
            p.escalar(esc);
            p.mover(0, -ConceitoRig2.CHAO);
            new ConceitoRig2.Desenho(p, c, guinada).esqueleto(false);
            p.restaurar();
        }
        g.dispose();
        return img;
    }

    /** Folha de contato generica: quadros com uma legenda embaixo de cada um. */
    static BufferedImage folhaQuadros(List<BufferedImage> quadros, List<String> legendas, int colunas, String titulo, String subtitulo) {
        int qw = quadros.get(0).getWidth(), qh = quadros.get(0).getHeight(), topo = 54, rot = 22;
        int linhas = (quadros.size() + colunas - 1) / colunas;
        BufferedImage img = new BufferedImage(colunas * qw, topo + linhas * (qh + rot), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(ConceitoRig2.FUNDO);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(ConceitoRig2.TEXTO);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString(titulo, 12, 24);
        g.setColor(ConceitoRig2.SUAVE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.drawString(subtitulo, 12, 43);
        for (int k = 0; k < quadros.size(); k++) {
            int x = (k % colunas) * qw, y = topo + (k / colunas) * (qh + rot);
            g.drawImage(quadros.get(k), x, y, null);
            String s = legendas.get(k);
            g.drawString(s, x + qw / 2 - g.getFontMetrics().stringWidth(s) / 2, y + qh + 15);
        }
        g.dispose();
        return img;
    }

    /** Folha de uma animacao ao longo do tempo, numa perspectiva. */
    static BufferedImage folhaAnimacao(String id, double guinada, int n, boolean esqueleto, int colunas, String titulo) {
        AnimacoesRig2.Animacao a = AnimacoesRig2.criar(id, null);
        List<BufferedImage> q = new ArrayList<>();
        List<String> l = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            double t = a.emLoop() ? a.duracao() * k / n : a.duracao() * k / (n - 1);
            ConceitoRig2.ParametrosPose p = a.amostrar(t);
            q.add(quadroAnim(p, guinada, a.deslocamento(t), esqueleto, 150, 230, .7));
            l.add(String.format(Locale.ROOT, "%.2f s", t));
        }
        String sub = String.format(Locale.ROOT, "perspectiva %d°  ·  %s %.2f s  ·  %d quadros", (int) guinada,
                a.emLoop() ? "ciclo de" : "duração", a.duracao(), n);
        return folhaQuadros(q, l, colunas, titulo, sub);
    }

    void estadoAnimacao(String id, double t, double guinada, boolean comEsqueleto) {
        reprodutor.velocidade = 1;
        cVelocidade.setSelectedIndex(1);
        selecionarAnimacao(id);
        reprodutor.tocar(AnimacoesRig2.criar(id, null));
        reprodutor.tempo = t;
        corpo = guinada;
        esqueleto = comEsqueleto;
        atualizar();
    }

    /**
     * Capturas da 6.2 e teste dos controles de reproducao (sem mostrar a janela). O relogio e avancado
     * com passos fixos, para o resultado ser reproduzivel.
     */
    static void capturas62(Path dir) throws Exception {
        Files.createDirectories(dir);
        Path seq = dir.resolve("quadros");
        Files.createDirectories(seq);
        List<Throwable> erros = Collections.synchronizedList(new ArrayList<>());
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> erros.add(e));
        final LaboratorioRig2[] ref = new LaboratorioRig2[1];
        SwingUtilities.invokeAndWait(() -> {
            ref[0] = new LaboratorioRig2();
            ref[0].janela.setSize(1180, 900);
            ref[0].janela.validate();
        });
        LaboratorioRig2 lab = ref[0];
        BufferedImage[] img = new BufferedImage[1];

        // 1) janela em diferentes animacoes
        Object[][] janelas = {
            {"janela-caminhada-040.png", AnimacoesRig2.CAMINHADA, .30, 40.0, false},
            {"janela-caminhada-esqueleto-040.png", AnimacoesRig2.CAMINHADA, .30, 40.0, true},
            {"janela-caminhada-090.png", AnimacoesRig2.CAMINHADA, .58, 90.0, false},
            {"janela-idle-combate-040.png", AnimacoesRig2.IDLE_COMBATE, .6, 40.0, false},
            {"janela-idle-neutro-040.png", AnimacoesRig2.IDLE_NEUTRO, 1.8, 40.0, false},
            {"janela-transicao-neutra-combate-040.png", AnimacoesRig2.NEUTRA_PARA_COMBATE, .5, 40.0, false}};
        for (Object[] j : janelas) {
            SwingUtilities.invokeAndWait(() -> {
                lab.estadoAnimacao((String) j[1], (Double) j[2], (Double) j[3], (Boolean) j[4]);
                img[0] = lab.capturarJanela();
            });
            salvar(img[0], dir.resolve((String) j[0]));
        }

        // 2) folhas de contato
        salvar(folhaAnimacao(AnimacoesRig2.IDLE_NEUTRO, 40, 8, false, 8, "Idle neutro (respiração)"), seq.resolve("idle-neutro-040.png"));
        salvar(folhaAnimacao(AnimacoesRig2.IDLE_COMBATE, 40, 8, false, 8, "Idle de combate (guarda)"), seq.resolve("idle-combate-040.png"));
        for (int a : new int[] {40, 90, 140})
            salvar(folhaAnimacao(AnimacoesRig2.CAMINHADA, a, 12, false, 12, "Caminhada (no lugar) · um ciclo, dois passos"),
                    seq.resolve(String.format("caminhada-%03d.png", a)));
        for (int a : new int[] {0, 180})
            salvar(folhaAnimacao(AnimacoesRig2.CAMINHADA, a, 6, false, 6, "Caminhada (no lugar)"), seq.resolve(String.format("caminhada-%03d.png", a)));
        salvar(folhaAnimacao(AnimacoesRig2.CAMINHADA, 40, 8, true, 8, "Caminhada com esqueleto"), seq.resolve("caminhada-esqueleto-040.png"));
        salvar(folhaAnimacao(AnimacoesRig2.NEUTRA_PARA_COMBATE, 40, 11, false, 11, "Transição neutra → combate"), seq.resolve("transicao-neutra-combate-040.png"));
        salvar(folhaAnimacao(AnimacoesRig2.COMBATE_PARA_NEUTRA, 40, 11, false, 11, "Transição combate → neutra"), seq.resolve("transicao-combate-neutra-040.png"));
        salvar(folhaAnimacao(AnimacoesRig2.NEUTRA_PARA_COMBATE, 90, 11, false, 11, "Transição neutra → combate"), seq.resolve("transicao-neutra-combate-090.png"));

        // 3) experimento 6.1: caminhada girando 40 -> 65 -> 90 -> 140 -> 40 sem reiniciar o ciclo
        AnimacoesRig2.Reprodutor r = new AnimacoesRig2.Reprodutor();
        r.tocar(AnimacoesRig2.criar(AnimacoesRig2.CAMINHADA, null));
        double[] marcos = {40, 65, 90, 140, 40};
        int n = 24;
        List<BufferedImage> q = new ArrayList<>();
        List<String> l = new ArrayList<>();
        double tempoAnterior = -1;
        for (int k = 0; k < n; k++) {
            double x = (double) k / (n - 1) * (marcos.length - 1);
            int seg = Math.min((int) x, marcos.length - 2);
            double ang = marcos[seg] + (marcos[seg + 1] - marcos[seg]) * (x - seg);
            if (k > 0) r.avancar(1 / 12.0);
            if (r.tempo <= tempoAnterior) throw new AssertionError("o ciclo reiniciou ao girar");
            tempoAnterior = r.tempo;
            q.add(quadroAnim(r.pose(ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.NEUTRA)), ang, r.deslocamento(), false, 150, 230, .7));
            l.add(String.format(Locale.ROOT, "%.0f° · %.2f s", ang, r.tempo));
        }
        salvar(folhaQuadros(q, l, 12, "Caminhada girando durante a reprodução: 40° → 65° → 90° → 140° → 40°",
                "o relógio segue (1/12 s por quadro); a perspectiva muda sem reiniciar o ciclo de passos"), seq.resolve("caminhada-girando.png"));
        System.out.println("OK: caminhada girando 40 -> 65 -> 90 -> 140 -> 40 sem reiniciar o ciclo.");

        // 4) controles de reproducao pela propria interface
        SwingUtilities.invokeAndWait(() -> {
            lab.esqueleto = false;
            lab.reprodutor.parar();
            lab.relogio.stop();
            lab.pose = ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.NEUTRA);
            lab.corpo = 40;
            lab.atualizar();
            lab.relogio.stop();
            for (String id : AnimacoesRig2.CATALOGO) {
                lab.selecionarAnimacao(id);
                lab.bTocar.doClick();
                lab.relogio.stop();
                for (int k = 0; k < 30; k++) lab.avancarRelogio(1 / 60.0);
                double t = lab.reprodutor.tempo;
                lab.bPausar.doClick();
                lab.avancarRelogio(.2);
                if (lab.reprodutor.tempo != t) throw new AssertionError("pausado, o tempo andou: " + id);
                lab.bTocar.doClick();               // continua de onde parou
                lab.relogio.stop();
                if (lab.reprodutor.tempo != t) throw new AssertionError("continuar reiniciou: " + id);
                for (int v = 0; v < 4; v++) { lab.cVelocidade.setSelectedIndex(v); lab.avancarRelogio(1 / 60.0); }
                lab.cVelocidade.setSelectedIndex(1);
                lab.bReiniciar.doClick();
                lab.relogio.stop();
                if (lab.reprodutor.tempo != 0) throw new AssertionError("reiniciar falhou: " + id);
                for (int a = 0; a <= 180; a += 20) { lab.sCorpo.setValue(a); lab.sCabeca.setValue(a % 40 == 0 ? 30 : -30); lab.avancarRelogio(1 / 30.0); }
                lab.chEsqueleto.doClick(); lab.avancarRelogio(1 / 30.0); lab.capturarVista(220, 300); lab.chEsqueleto.doClick();
                ConceitoRig2.ParametrosPose manual = lab.pose;
                lab.bParar.doClick();
                if (lab.exibida() != manual) throw new AssertionError("parar nao voltou a pose manual: " + id);
            }
            // troca de pose com transicao animada: neutra -> combate e de volta
            lab.sCabeca.setValue(0);
            lab.chTransicao.setSelected(true);
            lab.cPose.setSelectedItem(ConceitoRig2.PoseBase.COMBATE);
            lab.relogio.stop();
            if (!lab.reprodutor.ativo() || !lab.reprodutor.atual.id().equals(AnimacoesRig2.NEUTRA_PARA_COMBATE))
                throw new AssertionError("a troca de pose nao iniciou a transicao");
            for (int k = 0; k < 90; k++) lab.avancarRelogio(1 / 60.0);
            if (!lab.reprodutor.atual.id().equals(AnimacoesRig2.IDLE_COMBATE) || lab.pose.base != ConceitoRig2.PoseBase.COMBATE)
                throw new AssertionError("a transicao nao terminou no idle de combate");
            lab.cPose.setSelectedItem(ConceitoRig2.PoseBase.NEUTRA);
            lab.relogio.stop();
            for (int k = 0; k < 90; k++) lab.avancarRelogio(1 / 60.0);
            if (!lab.reprodutor.atual.id().equals(AnimacoesRig2.IDLE_NEUTRO) || lab.pose.base != ConceitoRig2.PoseBase.NEUTRA)
                throw new AssertionError("a transicao de volta nao terminou no idle neutro");
            // restaurar pose para a animacao e devolve a edicao manual
            lab.bRestaurarPose.doClick();
            if (lab.reprodutor.ativo()) throw new AssertionError("restaurar pose nao parou a animacao");
            // controles dos bracos ficam bloqueados durante a reproducao e voltam ao parar
            lab.selecionarAnimacao(AnimacoesRig2.CAMINHADA);
            lab.bTocar.doClick();
            lab.relogio.stop();
            if (lab.bracos[0].elevacao.isEnabled() || lab.cPernas.isEnabled()) throw new AssertionError("controles manuais livres durante a animacao");
            lab.bParar.doClick();
            if (!lab.bracos[0].elevacao.isEnabled() || !lab.cPernas.isEnabled()) throw new AssertionError("controles manuais nao voltaram");
        });
        SwingUtilities.invokeAndWait(lab.janela::dispose);
        if (!erros.isEmpty()) throw new AssertionError("excecoes durante o uso dos controles: " + erros);
        System.out.println("OK: reproduzir, pausar, continuar, velocidades, reiniciar, parar, troca de pose animada, giro do corpo e da cabeca"
                + " durante a reproducao, esqueleto e bloqueio/liberacao dos controles manuais, sem excecoes.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 1 && args[0].equals("--capturas")) {          // capturas e controles da 6.1
            capturas(Paths.get(args[1]));
            System.exit(0);
        }
        if (args.length > 1 && args[0].equals("--capturas-6.2")) {      // animacoes da 6.2
            capturas62(Paths.get(args[1]));
            System.exit(0);
        }
        SwingUtilities.invokeLater(() -> new LaboratorioRig2().janela.setVisible(true));
    }
}
