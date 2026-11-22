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
 * SUPERENTREGA 6.3 - Laboratorio de combate do Rig 2.0 (experimental).
 *
 * Evolucao do laboratorio, NAO substituicao: o LaboratorioRig2 (6.1/6.2) continua abrindo como antes, e a copia
 * homologada da 6.2 esta em historico/. Esta janela usa o mesmo modelo (ConceitoRig2) e a mesma biblioteca
 * (AnimacoesRig2, MovimentosRig2, DuelosRig2, BibliotecaRig2): lista tudo o que esta registrado, inclusive os
 * movimentos do jogo ainda pendentes (que aparecem como pendentes, sem execucao).
 *
 * Abrir (a partir da raiz do repositorio):  java docs/rig2/LaboratorioCombateRig2.java
 * Capturas e teste dos controles:          java docs/rig2/LaboratorioCombateRig2.java --capturas-6.3 docs/rig2/laboratorio-6.3
 */
public class LaboratorioCombateRig2 {

    final JFrame janela = new JFrame("Rig 2.0 · Laboratório de combate (6.3) · experimental");
    final List<BibliotecaRig2.Metadados> todas = BibliotecaRig2.todas();
    final DefaultListModel<BibliotecaRig2.Metadados> modelo = new DefaultListModel<>();
    final JList<BibliotecaRig2.Metadados> lista = new JList<>(modelo);
    final JComboBox<String> cCategoria = new JComboBox<>();
    final JCheckBox chPendentes = new JCheckBox("Mostrar pendentes", true);
    final JTextArea info = new JTextArea(14, 30);
    final JButton bTocar = new JButton("▶ Reproduzir"), bPausar = new JButton("❚❚ Pausar"), bParar = new JButton("■ Parar"),
            bReiniciar = new JButton("↺ Reiniciar");
    final JComboBox<String> cVelocidade = new JComboBox<>(new String[] {"0,25×", "0,5×", "1,0×", "1,5×", "2,0×"});
    final JSlider sTempo = new JSlider(0, 1000, 0), sVista = new JSlider(0, 180, 40), sDistancia = new JSlider(-20, 30, 0),
            sGiroA = new JSlider(-60, 60, 0), sGiroB = new JSlider(-60, 60, 0), sLateral = new JSlider(-40, 40, 0);
    final JCheckBox chEsqueleto = new JCheckBox("Esqueleto"), chContatos = new JCheckBox("Pontos de contato", true),
            chSegundo = new JCheckBox("Segundo lutador", true), chTrocar = new JCheckBox("Trocar Branco ↔ Preto");
    final JLabel lTempo = new JLabel(" "), lVista = new JLabel(" ");
    final Visualizador vista = new Visualizador();
    final javax.swing.Timer relogio = new javax.swing.Timer(15, e -> passo());
    long ultimoNano;
    boolean sincronizando;

    // reprodutores (um lutador ou dois); so um fica ativo por vez
    final AnimacoesRig2.Reprodutor solo = new AnimacoesRig2.Reprodutor();
    final DuelosRig2.ReprodutorDuplo dupla = new DuelosRig2.ReprodutorDuplo();
    BibliotecaRig2.Metadados selecionada;

    LaboratorioCombateRig2() {
        // ---- lista
        Set<String> cats = new TreeSet<>();
        for (BibliotecaRig2.Metadados m : todas) cats.add(m.categoria());
        cCategoria.addItem("Todas as categorias");
        for (String c : cats) cCategoria.addItem(c);
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foco) {
                BibliotecaRig2.Metadados m = (BibliotecaRig2.Metadados) v;
                super.getListCellRendererComponent(l, (m.executavel() ? "▶ " : "○ ") + m.nome(), i, sel, foco);
                if (!m.executavel() && !sel) setForeground(Color.GRAY);
                setToolTipText(m.estado());
                return this;
            }
        });
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) selecionar(lista.getSelectedValue()); });
        cCategoria.addActionListener(e -> filtrar());
        chPendentes.addActionListener(e -> filtrar());
        JPanel esquerda = new JPanel(new BorderLayout(4, 4));
        JPanel topo = new JPanel(new GridLayout(2, 1));
        topo.add(cCategoria);
        topo.add(chPendentes);
        esquerda.add(topo, BorderLayout.NORTH);
        esquerda.add(new JScrollPane(lista), BorderLayout.CENTER);
        esquerda.setPreferredSize(new Dimension(300, 800));
        esquerda.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 0));

        // ---- controles
        JPanel direita = new JPanel();
        direita.setLayout(new BoxLayout(direita, BoxLayout.Y_AXIS));
        direita.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 6));
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        JScrollPane pInfo = new JScrollPane(info);
        pInfo.setBorder(new TitledBorder("Animação selecionada"));
        direita.add(pInfo);
        JPanel transp = new JPanel(new GridLayout(2, 2, 4, 4));
        for (JButton b : new JButton[] {bTocar, bPausar, bParar, bReiniciar}) transp.add(b);
        transp.setBorder(new TitledBorder("Reprodução"));
        direita.add(transp);
        JPanel linha = new JPanel(new GridLayout(0, 1));
        linha.setBorder(new TitledBorder("Linha do tempo e vista"));
        cVelocidade.setSelectedIndex(2);
        linha.add(cVelocidade);
        linha.add(lTempo);
        linha.add(sTempo);
        linha.add(lVista);
        linha.add(sVista);
        direita.add(linha);
        JPanel cena = new JPanel(new GridLayout(0, 1));
        cena.setBorder(new TitledBorder("Cena"));
        for (JCheckBox c : new JCheckBox[] {chEsqueleto, chContatos, chSegundo, chTrocar}) cena.add(c);
        cena.add(new JLabel("Distância extra entre os lutadores"));
        cena.add(sDistancia);
        cena.add(new JLabel("Orientação de A (giro extra)"));
        cena.add(sGiroA);
        cena.add(new JLabel("Orientação de B (giro extra)"));
        cena.add(sGiroB);
        cena.add(new JLabel("Posição lateral de B"));
        cena.add(sLateral);
        direita.add(cena);
        direita.setPreferredSize(new Dimension(320, 800));

        janela.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        janela.add(esquerda, BorderLayout.WEST);
        janela.add(vista, BorderLayout.CENTER);
        janela.add(new JScrollPane(direita, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
                BorderLayout.EAST);
        vista.setPreferredSize(new Dimension(760, 800));

        bTocar.addActionListener(e -> tocar());
        bPausar.addActionListener(e -> { solo.pausar(); dupla.pausar(); atualizar(); });
        bParar.addActionListener(e -> { solo.parar(); dupla.parar(); relogio.stop(); atualizar(); });
        bReiniciar.addActionListener(e -> { solo.reiniciar(); dupla.reiniciar(); iniciarRelogio(); atualizar(); });
        cVelocidade.addActionListener(e -> {
            double v = new double[] {.25, .5, 1, 1.5, 2}[cVelocidade.getSelectedIndex()];
            solo.velocidade = v;
            dupla.velocidade = v;
        });
        sTempo.addChangeListener(e -> { if (!sincronizando) arrastarTempo(sTempo.getValue() / 1000.0); });
        for (JSlider s : new JSlider[] {sVista, sDistancia, sGiroA, sGiroB, sLateral}) s.addChangeListener(e -> atualizar());
        for (JCheckBox c : new JCheckBox[] {chEsqueleto, chContatos, chSegundo, chTrocar}) c.addActionListener(e -> atualizar());
        filtrar();
        for (int i = 0; i < modelo.size(); i++) if (modelo.get(i).id().equals("derrubada_forcada")) lista.setSelectedIndex(i);
        janela.pack();
        janela.setLocationRelativeTo(null);
    }

    void filtrar() {
        String cat = (String) cCategoria.getSelectedItem();
        modelo.clear();
        for (BibliotecaRig2.Metadados m : todas) {
            if (!chPendentes.isSelected() && !m.executavel()) continue;
            if (cat != null && !cat.startsWith("Todas") && !m.categoria().equals(cat)) continue;
            modelo.addElement(m);
        }
    }

    void selecionar(BibliotecaRig2.Metadados m) {
        if (m == null) return;
        selecionada = m;
        solo.parar();
        dupla.parar();
        relogio.stop();
        StringBuilder b = new StringBuilder();
        b.append(m.nome()).append("\n\n");
        b.append("id: ").append(m.id()).append("\nid no jogo: ").append(m.idJogo()).append("\ncategoria: ").append(m.categoria());
        b.append("\nestado: ").append(m.estado());
        if (!m.executavel()) {
            b.append("\n\nPENDENTE no Rig 2.0: ainda não existe execução desta animação.\n").append(m.articulacoes());
            b.append("\nposição inicial: ").append(m.poseInicial()).append("\nposição final: ").append(m.poseFinal());
        } else {
            b.append(String.format(Locale.ROOT, "\nlutadores: %d\nduração: %.2f s%s", m.participantes(), m.duracao(), m.loop() ? " (loop)" : ""));
            b.append("\npose inicial: ").append(m.poseInicial()).append("\npose final: ").append(m.poseFinal());
            b.append("\narticulações: ").append(m.articulacoes()).append("\nperspectivas: ").append(m.perspectivas());
            if (!m.contatos().isEmpty()) b.append("\ncontatos: ").append(String.join("; ", m.contatos()));
            if (!m.eventos().isEmpty()) {
                b.append("\neventos:");
                for (BibliotecaRig2.Evento e : m.eventos()) b.append(String.format(Locale.ROOT, "\n  %.3f s  %s", e.tempo(), e.nome()));
            }
        }
        info.setText(b.toString());
        info.setCaretPosition(0);
        bTocar.setEnabled(m.executavel());
        bReiniciar.setEnabled(m.executavel());
        sTempo.setEnabled(m.executavel());
        if (m.executavel()) {
            if (m.participantes() == 2) { dupla.tocar(DuelosRig2.criar(m.id())); dupla.pausar(); }
            else { solo.tocar(AnimacoesRig2.criar(m.id(), null)); solo.pausar(); }
        }
        atualizar();
    }

    void tocar() {
        if (selecionada == null || !selecionada.executavel()) return;
        if (selecionada.participantes() == 2) {
            if (dupla.estado == AnimacoesRig2.Reprodutor.Estado.PAUSADO && dupla.atual != null) dupla.continuar();
            else dupla.tocar(DuelosRig2.criar(selecionada.id()));
        } else {
            if (solo.estado == AnimacoesRig2.Reprodutor.Estado.PAUSADO && solo.atual != null) solo.continuar();
            else solo.tocar(AnimacoesRig2.criar(selecionada.id(), null));
        }
        iniciarRelogio();
        atualizar();
    }

    void iniciarRelogio() {
        ultimoNano = System.nanoTime();
        if (!relogio.isRunning()) relogio.start();
    }

    void passo() {
        long agora = System.nanoTime();
        avancar((agora - ultimoNano) / 1e9);
        ultimoNano = agora;
    }

    void avancar(double dt) {
        solo.avancar(dt);
        dupla.avancar(dt);
        if (!solo.ativo() && !dupla.ativo()) relogio.stop();
        atualizar();
    }

    /** Linha do tempo: arrastar pausa a reproducao e posiciona o tempo (0..1 da duracao atual). */
    void arrastarTempo(double x) {
        if (dupla.atual != null) { dupla.pausar(); dupla.tempo = x * dupla.atual.duracao(); }
        else if (solo.atual != null) { solo.pausar(); solo.tempo = x * solo.atual.duracao(); }
        atualizar();
    }

    double duracaoAtual() { return dupla.atual != null ? dupla.atual.duracao() : solo.atual != null ? solo.atual.duracao() : 0; }

    double tempoAtual() { return dupla.atual != null ? dupla.tempoNoCiclo() : solo.tempoNoCiclo(); }

    void atualizar() {
        sincronizando = true;
        double d = duracaoAtual();
        sTempo.setValue(d > 0 ? (int) Math.round(1000 * tempoAtual() / d) : 0);
        sincronizando = false;
        String nome = dupla.atual != null ? dupla.atual.nome() : solo.atual != null ? solo.atual.nome() : "—";
        lTempo.setText(String.format(Locale.ROOT, "<html><small>%s<br>%.2f s / %.2f s</small></html>", nome, tempoAtual(), d));
        lVista.setText("Perspectiva: " + sVista.getValue() + "°");
        vista.repaint();
    }

    /** O quadro da cena com os ajustes do usuario (distancia, orientacoes, posicao lateral). */
    DuelosRig2.QuadroDuplo quadroAtual() {
        DuelosRig2.QuadroDuplo q;
        if (dupla.atual != null) q = dupla.quadro();
        else {
            q = DuelosRig2.base(tempoAtual(), DuelosRig2.DISTANCIA_PEGADA + 20);
            if (solo.atual != null) q.pose[0] = solo.atual.amostrar(solo.tempoNoCiclo());
            else q.pose[0] = ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.COMBATE);
        }
        q.z[0] -= sDistancia.getValue() / 2.0;
        q.z[1] += sDistancia.getValue() / 2.0;
        q.guinada[0] += sGiroA.getValue();
        q.guinada[1] += sGiroB.getValue();
        q.x[1] += sLateral.getValue();
        return q;
    }

    final class Visualizador extends JPanel {
        Visualizador() { setBackground(ConceitoRig2.FUNDO); }

        @Override protected void paintComponent(Graphics gr) {
            super.paintComponent(gr);
            Graphics2D g = (Graphics2D) gr.create();
            desenhar(g, getWidth(), getHeight());
            g.dispose();
        }
    }

    void desenhar(Graphics2D g, int w, int h) {
        ConceitoRig2.Pintor p = new ConceitoRig2.Pintor(g, w, h);
        ConceitoRig2.fundo(p);
        ConceitoRig2.cartao(p, 12, 12, w - 24, h - 24);
        double esc = Math.max(.3, Math.min((h - 150) / 330.0, (w - 80) / 380.0)), chao = h - 90;
        p.linha(new Color(166, 176, 190, 60), 1, 30, chao + 1, w - 30, chao + 1);
        DuelosRig2.QuadroDuplo q = quadroAtual();
        boolean doisVisiveis = chSegundo.isSelected() || dupla.atual != null;
        ConceitoRig2.Aparencia[] ap = chTrocar.isSelected()
                ? new ConceitoRig2.Aparencia[] {ConceitoRig2.Aparencia.PRETO, ConceitoRig2.Aparencia.BRANCO} : DuelosRig2.APARENCIAS;
        if (solo.atual != null && solo.deslocamento() != 0)
            LaboratorioRig2.marcasDoChao(p, sVista.getValue() + q.guinada[0], solo.deslocamento(), w / 2.0, chao, esc);
        DuelosRig2.desenhar(p, q, DuelosRig2.resolver(q), sVista.getValue(), w / 2.0, chao, esc, chEsqueleto.isSelected(),
                chContatos.isSelected(), ap, new boolean[] {true, doisVisiveis});
        // textos
        String titulo = selecionada == null ? "—" : selecionada.nome();
        p.texto(titulo.length() > 70 ? titulo.substring(0, 70) + "…" : titulo, 32, 40, 15, ConceitoRig2.TEXTO, true, -1);
        String est = selecionada == null ? "" : selecionada.estado();
        p.texto(String.format(Locale.ROOT, "perspectiva %d°  ·  A = %s  ·  B = %s  ·  %s", sVista.getValue(), ap[0].nome(), ap[1].nome(), est),
                32, 60, 11, ConceitoRig2.SUAVE, false, -1);
        if (dupla.atual != null) p.texto("fase: " + q.fase, 32, 78, 11, ConceitoRig2.JUNTA, false, -1);
        if (selecionada != null && !selecionada.executavel())
            p.texto("PENDENTE: sem execução no Rig 2.0 (mostrando a guarda de combate)", 32, 96, 12, new Color(240, 170, 90), true, -1);
        // linha do tempo com eventos
        double d = duracaoAtual();
        double x0 = 40, x1 = w - 40, y = h - 48;
        p.linha(ConceitoRig2.SUAVE, 2, x0, y, x1, y);
        if (d > 0) {
            if (selecionada != null) for (BibliotecaRig2.Evento e : selecionada.eventos()) {
                double ex = x0 + (x1 - x0) * Math.min(1, e.tempo() / d);
                p.linha(ConceitoRig2.RAIZ, 2, ex, y - 7, ex, y + 7);
            }
            double cx = x0 + (x1 - x0) * tempoAtual() / d;
            p.forma(new Ellipse2D.Double(cx - 6, y - 6, 12, 12), ConceitoRig2.JUNTA, new Color(125, 85, 27), 1);
            p.texto(String.format(Locale.ROOT, "%.2f s / %.2f s   (azul: eventos)", tempoAtual(), d), x0, y + 22, 10.5, ConceitoRig2.SUAVE, false, -1);
        }
        p.texto("Rig 2.0 · laboratório de combate 6.3 (experimental)", w - 32, h - 20, 10, ConceitoRig2.SUAVE, false, 1);
    }

    // ===================================================================== capturas e teste dos controles
    BufferedImage capturarJanela() {
        janela.getRootPane().validate();
        Dimension dm = janela.getContentPane().getSize();
        BufferedImage img = new BufferedImage(dm.width, dm.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        janela.getContentPane().paint(g);
        g.dispose();
        return img;
    }

    static BufferedImage folhaDupla(String id, double vista, int n, boolean esqueleto, String titulo) {
        return folhaDupla(id, vista, n, esqueleto, titulo, 0, 0);
    }

    /** giroA/giroB: orientacao extra de cada lutador (vista 90 com -50/+50 = "duelo 2,5D", os dois em 3/4 para a camera). */
    static BufferedImage folhaDupla(String id, double vista, int n, boolean esqueleto, String titulo, double giroA, double giroB) {
        DuelosRig2.AnimacaoDupla a = DuelosRig2.criar(id);
        List<BufferedImage> q = new ArrayList<>();
        List<String> l = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            double t = a.emLoop() ? a.duracao() * k / n : a.duracao() * k / (n - 1);
            DuelosRig2.QuadroDuplo qd = a.amostrar(t);
            qd.guinada[0] += giroA;
            qd.guinada[1] += giroB;
            q.add(DuelosRig2.quadro(qd, vista, 230, 250, .55, esqueleto, true));
            l.add(String.format(Locale.ROOT, "%.2f s · %s", t, a.amostrar(t).fase));
        }
        return LaboratorioRig2.folhaQuadros(q, l, Math.min(n, 6), titulo,
                String.format(Locale.ROOT, "perspectiva %d°  ·  A = Miau Branco (aplicador)  ·  B = Miau Preto  ·  verde: contato estabelecido", (int) vista));
    }

    static void salvar(BufferedImage img, Path arq) throws Exception {
        ImageIO.write(img, "png", arq.toFile());
        System.out.println("gerado: " + arq);
    }

    static void capturas(Path dir) throws Exception {
        Path q = dir.resolve("quadros");
        Files.createDirectories(q);
        // movimentos basicos
        salvar(LaboratorioRig2.folhaAnimacao(MovimentosRig2.CORRIDA, 40, 12, false, 12, "Corrida (no lugar)"), q.resolve("corrida-040.png"));
        salvar(LaboratorioRig2.folhaAnimacao(MovimentosRig2.CORRIDA, 90, 12, false, 12, "Corrida (no lugar)"), q.resolve("corrida-090.png"));
        salvar(LaboratorioRig2.folhaAnimacao(MovimentosRig2.PULO, 40, 12, false, 12, "Pulo"), q.resolve("pulo-040.png"));
        salvar(LaboratorioRig2.folhaAnimacao(MovimentosRig2.PULO, 90, 12, false, 12, "Pulo"), q.resolve("pulo-090.png"));
        salvar(LaboratorioRig2.folhaAnimacao(MovimentosRig2.AGACHAR, 40, 6, false, 6, "Agachar (descida)"), q.resolve("agachar-040.png"));
        salvar(LaboratorioRig2.folhaAnimacao(MovimentosRig2.AGACHADO, 90, 6, false, 6, "Agachado (sustentado)"), q.resolve("agachado-090.png"));
        salvar(LaboratorioRig2.folhaAnimacao(MovimentosRig2.LEVANTAR_AGACHAMENTO, 40, 6, false, 6, "Levantar do agachamento"), q.resolve("levantar-agachamento-040.png"));
        // pegadas e tecnica, em tres perspectivas
        for (String id : new String[] {"pegada_1", "pegada_2", "pegada_3"})
            for (int v : new int[] {40, 90, 140})
                salvar(folhaDupla(id, v, 6, false, DuelosRig2.criar(id).nome()), q.resolve(String.format("%s-%03d.png", id, v)));
        for (int v : new int[] {40, 90, 140})
            salvar(folhaDupla("derrubada_forcada", v, 12, false, "Derrubada forçada (desfecho defendido), a partir da pegada 1"),
                    q.resolve(String.format("derrubada-forcada-%03d.png", v)));
        for (String id : new String[] {"pegada_1", "pegada_2", "pegada_3", "derrubada_forcada"})
            salvar(folhaDupla(id, 90, id.startsWith("derrubada") ? 12 : 6, false, DuelosRig2.criar(id).nome()
                    + " · duelo 2,5D (vista 90°, cada lutador girado 50° para a câmera)", -50, 50), q.resolve(id.replace('_', '-') + "-duelo-2-5d.png"));
        salvar(folhaDupla("derrubada_forcada", 40, 6, true, "Derrubada forçada (defendida) com esqueleto e contatos"), q.resolve("derrubada-forcada-esqueleto-040.png"));
        // dois personagens lado a lado, parados, em varias vistas
        List<BufferedImage> vs = new ArrayList<>();
        List<String> ls = new ArrayList<>();
        for (int v : new int[] {0, 40, 90, 140, 180}) {
            vs.add(DuelosRig2.quadro(DuelosRig2.criar("pegada_1-mantida").amostrar(0), v, 230, 250, .55, false, false));
            ls.add(v + "°");
        }
        salvar(LaboratorioRig2.folhaQuadros(vs, ls, 5, "Miau Branco e Miau Preto na mesma cena (pegada 1 mantida)", "a mesma cena em cinco perspectivas"),
                q.resolve("dois-lutadores-perspectivas.png"));

        // janela e controles
        List<Throwable> erros = Collections.synchronizedList(new ArrayList<>());
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> erros.add(e));
        LaboratorioCombateRig2[] ref = new LaboratorioCombateRig2[1];
        SwingUtilities.invokeAndWait(() -> { ref[0] = new LaboratorioCombateRig2(); ref[0].janela.setSize(1420, 880); ref[0].janela.validate(); });
        LaboratorioCombateRig2 lab = ref[0];
        BufferedImage[] img = new BufferedImage[1];
        Object[][] janelas = {{"derrubada_forcada", .5, 40, false, "janela-derrubada-forcada-040.png"},
            {"pegada_2-mantida", .3, 90, true, "janela-pegada-2-esqueleto-090.png"}, {"pulo", .69, 40, false, "janela-pulo-040.png"},
            {"jogo/queda_pegada", 0., 40, false, "janela-pendente-queda-pegada.png"}};
        for (Object[] j : janelas) {
            SwingUtilities.invokeAndWait(() -> {
                lab.chPendentes.setSelected(true);
                lab.cCategoria.setSelectedIndex(0);
                lab.filtrar();
                for (int i = 0; i < lab.modelo.size(); i++) if (lab.modelo.get(i).id().equals(j[0])) lab.lista.setSelectedIndex(i);
                lab.sVista.setValue((Integer) j[2]);
                lab.chEsqueleto.setSelected((Boolean) j[3]);
                if (lab.selecionada.executavel()) lab.arrastarTempo((Double) j[1] / lab.duracaoAtual());
                lab.lista.ensureIndexIsVisible(lab.lista.getSelectedIndex());
                img[0] = lab.capturarJanela();
            });
            salvar(img[0], dir.resolve((String) j[4]));
        }
        SwingUtilities.invokeAndWait(() -> {
            for (int c = 0; c < lab.cCategoria.getItemCount(); c++) { lab.cCategoria.setSelectedIndex(c); lab.capturarJanela(); }
            lab.cCategoria.setSelectedIndex(0);
            for (BibliotecaRig2.Metadados m : lab.todas) {
                if (!m.executavel()) continue;
                for (int i = 0; i < lab.modelo.size(); i++) if (lab.modelo.get(i).id().equals(m.id())) lab.lista.setSelectedIndex(i);
                lab.bTocar.doClick();
                lab.relogio.stop();
                for (int k = 0; k < 20; k++) lab.avancar(1 / 30.0);
                lab.bPausar.doClick();
                double t = lab.tempoAtual();
                lab.avancar(.2);
                if (lab.tempoAtual() != t) throw new AssertionError("pausado, o tempo andou: " + m.id());
                for (int v = 0; v < 5; v++) { lab.cVelocidade.setSelectedIndex(v); lab.bTocar.doClick(); lab.relogio.stop(); lab.avancar(1 / 30.0); }
                lab.cVelocidade.setSelectedIndex(2);
                lab.sTempo.setValue(500);
                lab.bReiniciar.doClick();
                lab.relogio.stop();
                for (int v = 0; v <= 180; v += 45) { lab.sVista.setValue(v); lab.avancar(1 / 30.0); }
                for (JCheckBox ch : new JCheckBox[] {lab.chEsqueleto, lab.chContatos, lab.chSegundo, lab.chTrocar}) { ch.doClick(); lab.capturarJanela(); ch.doClick(); }
                for (JSlider s : new JSlider[] {lab.sDistancia, lab.sGiroA, lab.sGiroB, lab.sLateral}) {
                    s.setValue(s.getMinimum()); lab.capturarJanela(); s.setValue(s.getMaximum()); lab.capturarJanela(); s.setValue(0);
                }
                lab.bParar.doClick();
            }
            // um item pendente: sem execucao
            for (int i = 0; i < lab.modelo.size(); i++) if (!lab.modelo.get(i).executavel()) { lab.lista.setSelectedIndex(i); break; }
            if (lab.bTocar.isEnabled()) throw new AssertionError("item pendente com reproducao habilitada");
            lab.bTocar.doClick();
            if (lab.solo.ativo() || lab.dupla.ativo()) throw new AssertionError("item pendente tocou");
        });
        SwingUtilities.invokeAndWait(lab.janela::dispose);
        if (!erros.isEmpty()) throw new AssertionError("excecoes: " + erros);
        System.out.println("OK: laboratorio de combate - todas as animacoes executaveis reproduzidas, pausadas, aceleradas, arrastadas na linha"
                + " do tempo, reiniciadas e paradas; vista, esqueleto, contatos, segundo lutador, troca de aparencia, distancia, orientacoes e"
                + " posicao lateral exercitados; item pendente sem execucao; todos os filtros de categoria; sem excecoes.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 1 && args[0].equals("--capturas-6.3")) {
            capturas(Paths.get(args[1]));
            System.exit(0);
        }
        SwingUtilities.invokeLater(() -> new LaboratorioCombateRig2().janela.setVisible(true));
    }
}
