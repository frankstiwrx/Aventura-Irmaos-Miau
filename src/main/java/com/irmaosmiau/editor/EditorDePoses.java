package com.irmaosmiau.editor;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EditorDePoses extends JFrame {

    // --- ESTRUTURA DE DADOS POR PERSONAGEM ---
    private class DadosPersonagem {
        String nome;
        IrmaoMiau instancia;
        double x, y, escala;
        boolean viradoDireita;
        
        Color corKimono = Color.WHITE;
        Color corPelo = Color.BLACK;

        int rotGlobal = 0, rotCabeca = 0;
        int offCabX = 0, offCabY = 0;
        int offCotEsqX = 0, offCotEsqY = 0, offMaoEsqX = 0, offMaoEsqY = 0;
        int offCotDirX = 0, offCotDirY = 0, offMaoDirX = 0, offMaoDirY = 0;
        int offJoelEsqX = 0, offJoelEsqY = 0, offPeEsqX = 0, offPeEsqY = 0;
        int offJoelDirX = 0, offJoelDirY = 0, offPeDirX = 0, offPeDirY = 0;

        public DadosPersonagem(String nome, IrmaoMiau instancia, double x, double y, double escala, boolean viradoDireita) {
            this.nome = nome;
            this.instancia = instancia;
            this.x = x; this.y = y; this.escala = escala;
            this.viradoDireita = viradoDireita;
            extrairCores();
        }

        private void extrairCores() {
            try {
                Class<?> clazz = instancia.getClass();
                while (clazz != null) {
                    try {
                        Field fKimono = clazz.getDeclaredField("corKimono");
                        fKimono.setAccessible(true);
                        corKimono = (Color) fKimono.get(instancia);

                        Field fPelo = clazz.getDeclaredField("corPelo");
                        fPelo.setAccessible(true);
                        corPelo = (Color) fPelo.get(instancia);
                        break;
                    } catch (NoSuchFieldException e) {
                        clazz = clazz.getSuperclass();
                    }
                }
            } catch (Exception e) {}
        }
    }

    // --- VARIÁVEIS SEGURAS DE INTERFACE ---
    // Instanciadas no topo para evitar qualquer NullPointerException silencioso
    private List<DadosPersonagem> cena = new ArrayList<>();
    private DadosPersonagem alvoAtual;
    private boolean atualizandoUI = true; // Começa bloqueando eventos até a tela estar pronta!

    private JComboBox<String> comboAlvo = new JComboBox<>();
    private JTextArea txtCodigoGerado = new JTextArea(10, 20);
    private CanvasPanel painelDesenho = new CanvasPanel();
    
    private JSlider sZoomCena;
    private double zoomGlobalCena = 1.0;
    
    private JSlider sEscala, sRotGlobal, sRotCabeca, sCabX, sCabY;
    private JSlider sCotEsqX, sCotEsqY, sMaoEsqX, sMaoEsqY;
    private JSlider sCotDirX, sCotDirY, sMaoDirX, sMaoDirY;
    private JSlider sJoelEsqX, sJoelEsqY, sPeEsqX, sPeEsqY;
    private JSlider sJoelDirX, sJoelDirY, sPeDirX, sPeDirY;
    private JCheckBox chkViradoDireita;

    public EditorDePoses() {
        setTitle("MiauStudio Pro - Rigging, Câmera & Importação");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. INICIALIZAR CENA (Modo 2 Jogadores)
        cena.add(new DadosPersonagem("Branco", new IrmaoMiauBranco(220, 150, 0.7), 220, 150, 0.7, true));
        cena.add(new DadosPersonagem("Preto", new IrmaoMiauPreto(500, 150, 0.7), 500, 150, 0.7, false));
        alvoAtual = cena.get(0);

        // 2. PAINEL DE CÂMERA E IMPORTAÇÃO
        JPanel painelTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        painelTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        
        painelTop.add(new JLabel("🔎 Zoom da Câmera:"));
        sZoomCena = new JSlider(5, 30, 10);
        sZoomCena.addChangeListener(e -> { zoomGlobalCena = sZoomCena.getValue() / 10.0; painelDesenho.repaint(); });
        painelTop.add(sZoomCena);

        painelTop.add(new JSeparator(SwingConstants.VERTICAL));
        painelTop.add(new JLabel("Importar Novo (Pacote.Classe):"));
        JTextField txtClasseImport = new JTextField("com.irmaosmiau.entities.NovoMiau", 20);
        JButton btnImportar = new JButton("Adicionar na Cena");
        
        btnImportar.addActionListener(e -> importarPersonagem(txtClasseImport.getText()));
        
        painelTop.add(txtClasseImport);
        painelTop.add(btnImportar);
        add(painelTop, BorderLayout.NORTH);

        // 3. PAINEL CENTRAL DE PINTURA
        painelDesenho.setBackground(new Color(135, 206, 235));
        add(painelDesenho, BorderLayout.CENTER);

        // 4. PAINEL DE CONTROLES (RIGGING)
        JPanel painelDireito = new JPanel(new BorderLayout());
        painelDireito.setPreferredSize(new Dimension(450, 850));

        JPanel pControles = new JPanel(new GridLayout(0, 1, 0, 0));
        pControles.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        pControles.add(new JLabel("🛠 ALVO DE EDIÇÃO:"));
        atualizarComboAlvos();
        comboAlvo.addActionListener(e -> selecionarAlvo(comboAlvo.getSelectedIndex()));
        pControles.add(comboAlvo);

        chkViradoDireita = new JCheckBox("Virado para a Direita");
        chkViradoDireita.addActionListener(e -> {
            if (!atualizandoUI) { alvoAtual.viradoDireita = chkViradoDireita.isSelected(); painelDesenho.repaint(); }
        });
        pControles.add(chkViradoDireita);

        // Construção segura dos sliders passando valor inicial exato
        sEscala = criarSlider(pControles, "Escala (div 10): ", 2, 30, 7, v -> alvoAtual.escala = v / 10.0);
        sRotGlobal = criarSlider(pControles, "ROT. GERAL: ", -180, 180, 0, v -> alvoAtual.rotGlobal = v);
        
        pControles.add(new JLabel("--- CABEÇA ---"));
        sCabX = criarSlider(pControles, "Offset X: ", -100, 100, 0, v -> alvoAtual.offCabX = v);
        sCabY = criarSlider(pControles, "Offset Y: ", -100, 100, 0, v -> alvoAtual.offCabY = v);
        sRotCabeca = criarSlider(pControles, "Rotação: ", -180, 180, 0, v -> alvoAtual.rotCabeca = v);

        pControles.add(new JLabel("--- BRAÇO ESQUERDO ---"));
        sCotEsqX = criarSlider(pControles, "Cotovelo X: ", -100, 100, 0, v -> alvoAtual.offCotEsqX = v);
        sCotEsqY = criarSlider(pControles, "Cotovelo Y: ", -100, 100, 0, v -> alvoAtual.offCotEsqY = v);
        sMaoEsqX = criarSlider(pControles, "Mão X: ", -100, 100, 0, v -> alvoAtual.offMaoEsqX = v);
        sMaoEsqY = criarSlider(pControles, "Mão Y: ", -100, 100, 0, v -> alvoAtual.offMaoEsqY = v);

        pControles.add(new JLabel("--- BRAÇO DIREITO ---"));
        sCotDirX = criarSlider(pControles, "Cotovelo X: ", -100, 100, 0, v -> alvoAtual.offCotDirX = v);
        sCotDirY = criarSlider(pControles, "Cotovelo Y: ", -100, 100, 0, v -> alvoAtual.offCotDirY = v);
        sMaoDirX = criarSlider(pControles, "Mão X: ", -100, 100, 0, v -> alvoAtual.offMaoDirX = v);
        sMaoDirY = criarSlider(pControles, "Mão Y: ", -100, 100, 0, v -> alvoAtual.offMaoDirY = v);

        pControles.add(new JLabel("--- PERNA ESQUERDA ---"));
        sJoelEsqX = criarSlider(pControles, "Joelho X: ", -100, 100, 0, v -> alvoAtual.offJoelEsqX = v);
        sJoelEsqY = criarSlider(pControles, "Joelho Y: ", -100, 100, 0, v -> alvoAtual.offJoelEsqY = v);
        sPeEsqX = criarSlider(pControles, "Pé X: ", -100, 100, 0, v -> alvoAtual.offPeEsqX = v);
        sPeEsqY = criarSlider(pControles, "Pé Y: ", -100, 100, 0, v -> alvoAtual.offPeEsqY = v);

        pControles.add(new JLabel("--- PERNA DIREITA ---"));
        sJoelDirX = criarSlider(pControles, "Joelho X: ", -100, 100, 0, v -> alvoAtual.offJoelDirX = v);
        sJoelDirY = criarSlider(pControles, "Joelho Y: ", -100, 100, 0, v -> alvoAtual.offJoelDirY = v);
        sPeDirX = criarSlider(pControles, "Pé X: ", -100, 100, 0, v -> alvoAtual.offPeDirX = v);
        sPeDirY = criarSlider(pControles, "Pé Y: ", -100, 100, 0, v -> alvoAtual.offPeDirY = v);

        JScrollPane scrollControles = new JScrollPane(pControles);
        scrollControles.getVerticalScrollBar().setUnitIncrement(16);
        painelDireito.add(scrollControles, BorderLayout.CENTER);

        // Painel do Código
        txtCodigoGerado.setEditable(false);
        txtCodigoGerado.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JPanel pCodigo = new JPanel(new BorderLayout());
        pCodigo.setBorder(BorderFactory.createTitledBorder("Código da Pose do Alvo Atual"));
        pCodigo.add(new JScrollPane(txtCodigoGerado), BorderLayout.CENTER);
        
        painelDireito.add(pCodigo, BorderLayout.SOUTH);
        add(painelDireito, BorderLayout.EAST);

        // Libera os eventos da UI somente agora que tudo existe!
        atualizandoUI = false;
        carregarUIComDadosDoAlvo();
    }

    // Criação dos Sliders Blindados
    private JSlider criarSlider(JPanel p, String rotulo, int min, int max, int valInit, Consumer<Integer> acao) {
        JPanel linha = new JPanel(new BorderLayout());
        JLabel l = new JLabel(rotulo + valInit);
        l.setPreferredSize(new Dimension(135, 20));
        JSlider s = new JSlider(min, max, valInit);
        s.addChangeListener(e -> {
            l.setText(rotulo + s.getValue());
            if (!atualizandoUI) {
                acao.accept(s.getValue());
                atualizarCodigoGerado();
                painelDesenho.repaint();
            }
        });
        linha.add(l, BorderLayout.WEST);
        linha.add(s, BorderLayout.CENTER);
        p.add(linha);
        return s;
    }

    private void importarPersonagem(String caminhoClasse) {
        try {
            Class<?> clazz = Class.forName(caminhoClasse);
            Constructor<?> construtor = clazz.getConstructor(int.class, int.class, double.class);
            IrmaoMiau novoMiau = (IrmaoMiau) construtor.newInstance(350, 150, 0.7);
            
            String nomeSimples = clazz.getSimpleName();
            cena.add(new DadosPersonagem(nomeSimples, novoMiau, 350, 150, 0.7, true));
            atualizarComboAlvos();
            selecionarAlvo(cena.size() - 1);
            JOptionPane.showMessageDialog(this, "Personagem carregado com sucesso!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao importar classe:\n" + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarComboAlvos() {
        boolean estadoAnterior = atualizandoUI;
        atualizandoUI = true;
        comboAlvo.removeAllItems();
        for (int i = 0; i < cena.size(); i++) {
            comboAlvo.addItem("ID " + i + " - " + cena.get(i).nome);
        }
        atualizandoUI = estadoAnterior;
    }

    private void selecionarAlvo(int index) {
        if (index >= 0 && index < cena.size()) {
            alvoAtual = cena.get(index);
            carregarUIComDadosDoAlvo();
        }
    }

    private void carregarUIComDadosDoAlvo() {
        atualizandoUI = true;
        chkViradoDireita.setSelected(alvoAtual.viradoDireita);
        sEscala.setValue((int)(alvoAtual.escala * 10));
        sRotGlobal.setValue(alvoAtual.rotGlobal);
        sCabX.setValue(alvoAtual.offCabX); sCabY.setValue(alvoAtual.offCabY); sRotCabeca.setValue(alvoAtual.rotCabeca);
        sCotEsqX.setValue(alvoAtual.offCotEsqX); sCotEsqY.setValue(alvoAtual.offCotEsqY);
        sMaoEsqX.setValue(alvoAtual.offMaoEsqX); sMaoEsqY.setValue(alvoAtual.offMaoEsqY);
        sCotDirX.setValue(alvoAtual.offCotDirX); sCotDirY.setValue(alvoAtual.offCotDirY);
        sMaoDirX.setValue(alvoAtual.offMaoDirX); sMaoDirY.setValue(alvoAtual.offMaoDirY);
        sJoelEsqX.setValue(alvoAtual.offJoelEsqX); sJoelEsqY.setValue(alvoAtual.offJoelEsqY);
        sPeEsqX.setValue(alvoAtual.offPeEsqX); sPeEsqY.setValue(alvoAtual.offPeEsqY);
        sJoelDirX.setValue(alvoAtual.offJoelDirX); sJoelDirY.setValue(alvoAtual.offJoelDirY);
        sPeDirX.setValue(alvoAtual.offPeDirX); sPeDirY.setValue(alvoAtual.offPeDirY);
        atualizandoUI = false;
        
        atualizarCodigoGerado();
        painelDesenho.repaint();
    }

    private void atualizarCodigoGerado() {
        String codigo = String.format(
            "// ROTAÇÃO: %d | R. CABEÇA: %d | OFFSET CAB. X: %d | Y: %d\n\n" +
            "cotoveloEsquerdoX += (int)(%d * escala);\n" +
            "cotoveloEsquerdoY += (int)(%d * escala);\n" +
            "maoEsquerdaX += (int)(%d * escala);\n" +
            "maoEsquerdaY += (int)(%d * escala);\n\n" +
            "cotoveloDireitoX += (int)(%d * escala);\n" +
            "cotoveloDireitoY += (int)(%d * escala);\n" +
            "maoDireitaX += (int)(%d * escala);\n" +
            "maoDireitaY += (int)(%d * escala);\n\n" +
            "joelhoEsquerdoX += (int)(%d * escala);\n" +
            "joelhoEsquerdoY += (int)(%d * escala);\n" +
            "peEsquerdoX += (int)(%d * escala);\n" +
            "peEsquerdoY += (int)(%d * escala);\n\n" +
            "joelhoDireitoX += (int)(%d * escala);\n" +
            "joelhoDireitoY += (int)(%d * escala);\n" +
            "peDireitoX += (int)(%d * escala);\n" +
            "peDireitoY += (int)(%d * escala);\n",
            alvoAtual.rotGlobal, alvoAtual.rotCabeca, alvoAtual.offCabX, alvoAtual.offCabY,
            alvoAtual.offCotEsqX, alvoAtual.offCotEsqY, alvoAtual.offMaoEsqX, alvoAtual.offMaoEsqY,
            alvoAtual.offCotDirX, alvoAtual.offCotDirY, alvoAtual.offMaoDirX, alvoAtual.offMaoDirY,
            alvoAtual.offJoelEsqX, alvoAtual.offJoelEsqY, alvoAtual.offPeEsqX, alvoAtual.offPeEsqY,
            alvoAtual.offJoelDirX, alvoAtual.offJoelDirY, alvoAtual.offPeDirX, alvoAtual.offPeDirY
        );
        txtCodigoGerado.setText(codigo);
        txtCodigoGerado.setCaretPosition(0);
    }

    private class CanvasPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D gBase = (Graphics2D) g;
            gBase.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Câmera Zoom Global
            Graphics2D gCamera = (Graphics2D) gBase.create();
            double centroViewX = getWidth() / 2.0;
            double centroViewY = getHeight() / 2.0;
            gCamera.translate(centroViewX, centroViewY);
            gCamera.scale(zoomGlobalCena, zoomGlobalCena);
            gCamera.translate(-centroViewX, -centroViewY);

            // Chão Fiel ao Jogo
            gCamera.setColor(new Color(80, 180, 80));
            gCamera.fillRect(-2000, 300, 5000, 2000); 

            // Renderiza todos os bonecos na tela
            for (DadosPersonagem pd : cena) {
                desenharPersonagemEditor(gCamera, pd);
            }
            gCamera.dispose();
        }

        private void desenharPersonagemEditor(Graphics2D gCamera, DadosPersonagem pd) {
            Graphics2D g2d = (Graphics2D) gCamera.create();
            double s = pd.escala;
            double posX = pd.x;
            double posY = pd.y;

            if (!pd.viradoDireita) {
                double eixoCentral = posX + 35 * s;
                g2d.translate(eixoCentral, 0);
                g2d.scale(-1, 1);
                g2d.translate(-eixoCentral, 0);
            }

            double pivotX = posX + 35 * s;
            double pivotY = posY + 120 * s;
            g2d.translate(pivotX, pivotY);
            g2d.rotate(Math.toRadians(pd.rotGlobal));
            g2d.translate(-pivotX, -pivotY);

            // CABEÇA ORIGINAL IMPORTADA
            Graphics2D gCabeca = (Graphics2D) g2d.create();
            gCabeca.translate(pd.offCabX * s, pd.offCabY * s);
            
            double pivotCabX = posX + 35 * s;
            double pivotCabY = posY + 53 * s;
            gCabeca.rotate(Math.toRadians(pd.rotCabeca), pivotCabX, pivotCabY);
            
            try {
                Method metodoCabeca = null;
                Class<?> clazz = pd.instancia.getClass();
                while (clazz != null && metodoCabeca == null) {
                    try { metodoCabeca = clazz.getDeclaredMethod("desenharCabeca", Graphics2D.class); } 
                    catch (Exception ex) { clazz = clazz.getSuperclass(); }
                }
                if (metodoCabeca != null) {
                    metodoCabeca.setAccessible(true);
                    metodoCabeca.invoke(pd.instancia, gCabeca); 
                }
            } catch (Exception ex) {}
            gCabeca.dispose();

            // CORPO
            int corpoY = (int)(posY + 70 * s);
            Polygon corpo = new Polygon();
            corpo.addPoint((int)(posX - 10*s), corpoY);
            corpo.addPoint((int)(posX + 82*s), corpoY);
            corpo.addPoint((int)(posX + 64*s), (int)(corpoY + 105*s));
            corpo.addPoint((int)(posX + 8*s), (int)(corpoY + 105*s));
            
            g2d.setColor(pd.corKimono);
            g2d.fillPolygon(corpo);
            g2d.setColor(new Color(55, 55, 220));
            g2d.fillRect((int)(posX - 8*s), (int)(corpoY + 82*s), (int)(96*s), (int)(12*s));

            // JUNTAS E ARTICULAÇÕES COM BASE NO ALVO
            int ombroEsqX = (int)(posX + 3*s), ombroEsqY = (int)(corpoY + 22*s);
            int cotEsqX = (int)(posX - 14*s + pd.offCotEsqX*s), cotEsqY = (int)(corpoY + 42*s + pd.offCotEsqY*s);
            int maoEsqX = (int)(posX + 3*s + pd.offMaoEsqX*s), maoEsqY = (int)(corpoY + 58*s + pd.offMaoEsqY*s);

            int ombroDirX = (int)(posX + 73*s), ombroDirY = (int)(corpoY + 20*s);
            int cotDirX = (int)(posX + 95*s + pd.offCotDirX*s), cotDirY = (int)(corpoY + 36*s + pd.offCotDirY*s);
            int maoDirX = (int)(posX + 111*s + pd.offMaoDirX*s), maoDirY = (int)(corpoY + 29*s + pd.offMaoDirY*s);

            int quadEsqX = (int)(posX + 22*s), quadEsqY = (int)(corpoY + 100*s);
            int joelEsqX = (int)(posX + 10*s + pd.offJoelEsqX*s), joelEsqY = (int)(corpoY + 136*s + pd.offJoelEsqY*s);
            int peEsqX = (int)(posX + pd.offPeEsqX*s), peEsqY = (int)(corpoY + 174*s + pd.offPeEsqY*s);

            int quadDirX = (int)(posX + 52*s), quadDirY = (int)(corpoY + 100*s);
            int joelDirX = (int)(posX + 66*s + pd.offJoelDirX*s), joelDirY = (int)(corpoY + 137*s + pd.offJoelDirY*s);
            int peDirX = (int)(posX + 82*s + pd.offPeDirX*s), peDirY = (int)(corpoY + 171*s + pd.offPeDirY*s);

            g2d.setStroke(new BasicStroke((float) (18 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(pd.corKimono);
            g2d.drawLine(ombroEsqX, ombroEsqY, cotEsqX, cotEsqY); g2d.drawLine(cotEsqX, cotEsqY, maoEsqX, maoEsqY);
            g2d.drawLine(ombroDirX, ombroDirY, cotDirX, cotDirY); g2d.drawLine(cotDirX, cotDirY, maoDirX, maoDirY);
            g2d.drawLine(quadEsqX, quadEsqY, joelEsqX, joelEsqY); g2d.drawLine(joelEsqX, joelEsqY, peEsqX, peEsqY);
            g2d.drawLine(quadDirX, quadDirY, joelDirX, joelDirY); g2d.drawLine(joelDirX, joelDirY, peDirX, peDirY);

            g2d.setColor(pd.corPelo);
            int lMao = (int)(20*s), aMao = (int)(18*s), lPe = (int)(25*s), aPe = (int)(17*s);
            g2d.fillOval(maoEsqX - lMao/2, maoEsqY - aMao/2, lMao, aMao);
            g2d.fillOval(maoDirX - lMao/2, maoDirY - aMao/2, lMao, aMao);
            g2d.fillOval(peEsqX - lPe/2, peEsqY - aPe/2, lPe, aPe);
            g2d.fillOval(peDirX - lPe/2, peDirY - aPe/2, lPe, aPe);

            if (pd == alvoAtual) {
                g2d.setColor(Color.RED);
                g2d.fillPolygon(new int[]{(int)(posX+25*s), (int)(posX+45*s), (int)(posX+35*s)},
                                new int[]{(int)(posY-10*s), (int)(posY-10*s), (int)(posY+5*s)}, 3);
            }
            g2d.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
            catch (Exception ignored) {}
            new EditorDePoses().setVisible(true);
        });
    }
}