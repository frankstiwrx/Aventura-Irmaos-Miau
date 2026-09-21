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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditorDePoses extends JFrame {

    // --- ESTRUTURA DE DADOS POR PERSONAGEM ---
    private class DadosPersonagem {
        String nome;
        IrmaoMiau instancia;
        double escala;
        boolean viradoDireita;
        
        // --- NOVO: Variáveis de Posição no Cenário ---
        int cenaX;
        int linhaAtual; // 0 a 4 (Define a profundidade/Y)
        
        Color corKimono = Color.WHITE;
        Color corPelo = Color.BLACK;

        int rotGlobal = 0, rotCabeca = 0;
        int offCabX = 0, offCabY = 0;
        int offCotEsqX = 0, offCotEsqY = 0, offMaoEsqX = 0, offMaoEsqY = 0;
        int offCotDirX = 0, offCotDirY = 0, offMaoDirX = 0, offMaoDirY = 0;
        int offJoelEsqX = 0, offJoelEsqY = 0, offPeEsqX = 0, offPeEsqY = 0;
        int offJoelDirX = 0, offJoelDirY = 0, offPeDirX = 0, offPeDirY = 0;

        // Cada personagem mantém seus próprios 5 quadros.
        PoseSalva[] quadrosAnimacao = new PoseSalva[5];

        public DadosPersonagem(String nome, IrmaoMiau instancia, int cenaX, int linhaAtual, double escala, boolean viradoDireita) {
            this.nome = nome;
            this.instancia = instancia;
            this.cenaX = cenaX;
            this.linhaAtual = linhaAtual;
            this.escala = escala;
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

    // --- CONTROLES INTELIGENTES COM DIGITAÇÃO MANUAL ---
    private class ControleNumerico {
        JSlider slider;
        JSpinner spinner;

        public ControleNumerico(JPanel p, String rotulo, int min, int max, int valInit, Consumer<Integer> acao) {
            JPanel linha = new JPanel(new BorderLayout());
            
            JLabel l = new JLabel(rotulo);
            l.setPreferredSize(new Dimension(85, 20));

            spinner = new JSpinner(new SpinnerNumberModel(valInit, min, max, 1));
            spinner.setPreferredSize(new Dimension(50, 20));

            slider = new JSlider(min, max, valInit);

            // Sincronização Slider -> Spinner -> Ação
            slider.addChangeListener(e -> {
                if (!atualizandoUI) {
                    spinner.setValue(slider.getValue());
                    acao.accept(slider.getValue());
                    atualizarCodigoGerado();
                    painelDesenho.repaint();
                }
            });

            // Sincronização Spinner -> Slider -> Ação
            spinner.addChangeListener(e -> {
                if (!atualizandoUI) {
                    int val = (int) spinner.getValue();
                    slider.setValue(val);
                    acao.accept(val);
                    atualizarCodigoGerado();
                    painelDesenho.repaint();
                }
            });

            JPanel pEsq = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            pEsq.add(l);
            pEsq.add(spinner);

            linha.add(pEsq, BorderLayout.WEST);
            linha.add(slider, BorderLayout.CENTER);
            p.add(linha);
        }

        public void setValue(int v) {
            slider.setValue(v);
            spinner.setValue(v);
        }
    }


    // --- UM QUADRO COMPLETO DE ANIMAÇÃO ---
    private class PoseSalva {
        int cenaX;
        int linhaAtual;
        int escalaX10;
        boolean viradoDireita;

        int rotGlobal;
        int rotCabeca;
        int offCabX;
        int offCabY;

        int offCotEsqX;
        int offCotEsqY;
        int offMaoEsqX;
        int offMaoEsqY;

        int offCotDirX;
        int offCotDirY;
        int offMaoDirX;
        int offMaoDirY;

        int offJoelEsqX;
        int offJoelEsqY;
        int offPeEsqX;
        int offPeEsqY;

        int offJoelDirX;
        int offJoelDirY;
        int offPeDirX;
        int offPeDirY;

        PoseSalva(DadosPersonagem pd) {
            cenaX = pd.cenaX;
            linhaAtual = pd.linhaAtual;
            escalaX10 = (int) Math.round(pd.escala * 10.0);
            viradoDireita = pd.viradoDireita;

            rotGlobal = pd.rotGlobal;
            rotCabeca = pd.rotCabeca;
            offCabX = pd.offCabX;
            offCabY = pd.offCabY;

            offCotEsqX = pd.offCotEsqX;
            offCotEsqY = pd.offCotEsqY;
            offMaoEsqX = pd.offMaoEsqX;
            offMaoEsqY = pd.offMaoEsqY;

            offCotDirX = pd.offCotDirX;
            offCotDirY = pd.offCotDirY;
            offMaoDirX = pd.offMaoDirX;
            offMaoDirY = pd.offMaoDirY;

            offJoelEsqX = pd.offJoelEsqX;
            offJoelEsqY = pd.offJoelEsqY;
            offPeEsqX = pd.offPeEsqX;
            offPeEsqY = pd.offPeEsqY;

            offJoelDirX = pd.offJoelDirX;
            offJoelDirY = pd.offJoelDirY;
            offPeDirX = pd.offPeDirX;
            offPeDirY = pd.offPeDirY;
        }

        void aplicarEm(DadosPersonagem pd) {
            pd.cenaX = cenaX;
            pd.linhaAtual = linhaAtual;
            pd.escala = escalaX10 / 10.0;
            pd.viradoDireita = viradoDireita;

            pd.rotGlobal = rotGlobal;
            pd.rotCabeca = rotCabeca;
            pd.offCabX = offCabX;
            pd.offCabY = offCabY;

            pd.offCotEsqX = offCotEsqX;
            pd.offCotEsqY = offCotEsqY;
            pd.offMaoEsqX = offMaoEsqX;
            pd.offMaoEsqY = offMaoEsqY;

            pd.offCotDirX = offCotDirX;
            pd.offCotDirY = offCotDirY;
            pd.offMaoDirX = offMaoDirX;
            pd.offMaoDirY = offMaoDirY;

            pd.offJoelEsqX = offJoelEsqX;
            pd.offJoelEsqY = offJoelEsqY;
            pd.offPeEsqX = offPeEsqX;
            pd.offPeEsqY = offPeEsqY;

            pd.offJoelDirX = offJoelDirX;
            pd.offJoelDirY = offJoelDirY;
            pd.offPeDirX = offPeDirX;
            pd.offPeDirY = offPeDirY;
        }

        String paraLinhaJava() {
            return String.format(
                "{%d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d}",
                cenaX,
                linhaAtual,
                escalaX10,
                viradoDireita ? 1 : 0,
                rotGlobal,
                rotCabeca,
                offCabX,
                offCabY,
                offCotEsqX,
                offCotEsqY,
                offMaoEsqX,
                offMaoEsqY,
                offCotDirX,
                offCotDirY,
                offMaoDirX,
                offMaoDirY,
                offJoelEsqX,
                offJoelEsqY,
                offPeEsqX,
                offPeEsqY,
                offJoelDirX,
                offJoelDirY,
                offPeDirX,
                offPeDirY
            );
        }
    }

    // --- VARIÁVEIS SEGURAS DE INTERFACE ---
    private List<DadosPersonagem> cena = new ArrayList<>();
    private DadosPersonagem alvoAtual;
    private boolean atualizandoUI = true;

    private JComboBox<String> comboAlvo = new JComboBox<>();
    private JTextArea txtCodigoGerado = new JTextArea(10, 20);
    private JTextArea txtAnimacaoGerada = new JTextArea(12, 20);

    private JTextField txtNomeAnimacao = new JTextField("nova_animacao", 14);
    private JComboBox<String> comboQuadroAnimacao = new JComboBox<>(
            new String[]{"Quadro 1", "Quadro 2", "Quadro 3", "Quadro 4", "Quadro 5"}
    );
    private JLabel lblStatusQuadros = new JLabel("Quadros: ○ ○ ○ ○ ○");

    private CanvasPanel painelDesenho = new CanvasPanel();
    
    private JSlider sZoomCena;
    private double zoomGlobalCena = 1.0;
    
    // Nossos novos controles unificados
    private ControleNumerico cCenaX, cLinha, cEscala, cRotGlobal;
    private ControleNumerico cRotCabeca, cCabX, cCabY;
    private ControleNumerico cCotEsqX, cCotEsqY, cMaoEsqX, cMaoEsqY;
    private ControleNumerico cCotDirX, cCotDirY, cMaoDirX, cMaoDirY;
    private ControleNumerico cJoelEsqX, cJoelEsqY, cPeEsqX, cPeEsqY;
    private ControleNumerico cJoelDirX, cJoelDirY, cPeDirX, cPeDirY;
    private JCheckBox chkViradoDireita;

    public EditorDePoses() {
        setTitle("MiauStudio Ultimate - Manual Input, Câmera & Reverse Engineering");
        setSize(1450, 950);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. INICIALIZAR CENA (Posições X reais e Linha baseadas no GamePanel)
        cena.add(new DadosPersonagem("Branco", new IrmaoMiauBranco(0, 0, 0.7), 220, 2, 0.7, true));
        cena.add(new DadosPersonagem("Preto", new IrmaoMiauPreto(0, 0, 0.7), 500, 2, 0.7, false));
        alvoAtual = cena.get(0);

        // 2. PAINEL DE CÂMERA E IMPORTAÇÃO
        JPanel painelTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        painelTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        
        painelTop.add(new JLabel("🔎 Zoom Câmera:"));
        sZoomCena = new JSlider(5, 30, 10);
        sZoomCena.addChangeListener(e -> { zoomGlobalCena = sZoomCena.getValue() / 10.0; painelDesenho.repaint(); });
        painelTop.add(sZoomCena);

        painelTop.add(new JSeparator(SwingConstants.VERTICAL));
        painelTop.add(new JLabel("Novo Lutador (Classe):"));
        JTextField txtClasseImport = new JTextField("com.irmaosmiau.entities.NovoMiau", 20);
        JButton btnImportar = new JButton("Adicionar");
        btnImportar.addActionListener(e -> importarPersonagem(txtClasseImport.getText()));
        painelTop.add(txtClasseImport);
        painelTop.add(btnImportar);
        
        add(painelTop, BorderLayout.NORTH);

        // 3. PAINEL DE PINTURA
        painelDesenho.setBackground(new Color(135, 206, 235));
        add(painelDesenho, BorderLayout.CENTER);

        // 4. PAINEL DE CONTROLES (RIGGING E CENÁRIO)
        JPanel painelDireito = new JPanel(new BorderLayout());
        painelDireito.setPreferredSize(new Dimension(500, 850));

        JPanel pControles = new JPanel(new GridLayout(0, 1, 0, 0));
        pControles.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Bloco de Configuração de Alvo
        pControles.add(new JLabel("🛠 ALVO DE EDIÇÃO:"));
        atualizarComboAlvos();
        comboAlvo.addActionListener(e -> selecionarAlvo(comboAlvo.getSelectedIndex()));
        pControles.add(comboAlvo);

        chkViradoDireita = new JCheckBox("Virado para a Direita");
        chkViradoDireita.addActionListener(e -> {
            if (!atualizandoUI) { alvoAtual.viradoDireita = chkViradoDireita.isSelected(); painelDesenho.repaint(); }
        });
        pControles.add(chkViradoDireita);

        // --- CONTROLES DE POSIÇÃO NO CENÁRIO ---
        pControles.add(new JLabel("--- POSIÇÃO NO CENÁRIO ---"));
        cCenaX = new ControleNumerico(pControles, "Cena X: ", -500, 1500, 220, v -> alvoAtual.cenaX = v);
        cLinha = new ControleNumerico(pControles, "Linha (0-4): ", 0, 4, 2, v -> alvoAtual.linhaAtual = v);
        
        cEscala = new ControleNumerico(pControles, "Escala (x10): ", 2, 30, 7, v -> alvoAtual.escala = v / 10.0);
        cRotGlobal = new ControleNumerico(pControles, "ROT. GERAL: ", -180, 180, 0, v -> alvoAtual.rotGlobal = v);
        
        pControles.add(new JLabel("--- CABEÇA ---"));
        cCabX = new ControleNumerico(pControles, "Off X: ", -100, 100, 0, v -> alvoAtual.offCabX = v);
        cCabY = new ControleNumerico(pControles, "Off Y: ", -100, 100, 0, v -> alvoAtual.offCabY = v);
        cRotCabeca = new ControleNumerico(pControles, "Rotação: ", -180, 180, 0, v -> alvoAtual.rotCabeca = v);

        pControles.add(new JLabel("--- BRAÇO ESQUERDO ---"));
        cCotEsqX = new ControleNumerico(pControles, "Cot X: ", -150, 150, 0, v -> alvoAtual.offCotEsqX = v);
        cCotEsqY = new ControleNumerico(pControles, "Cot Y: ", -150, 150, 0, v -> alvoAtual.offCotEsqY = v);
        cMaoEsqX = new ControleNumerico(pControles, "Mão X: ", -150, 150, 0, v -> alvoAtual.offMaoEsqX = v);
        cMaoEsqY = new ControleNumerico(pControles, "Mão Y: ", -150, 150, 0, v -> alvoAtual.offMaoEsqY = v);

        pControles.add(new JLabel("--- BRAÇO DIREITO ---"));
        cCotDirX = new ControleNumerico(pControles, "Cot X: ", -150, 150, 0, v -> alvoAtual.offCotDirX = v);
        cCotDirY = new ControleNumerico(pControles, "Cot Y: ", -150, 150, 0, v -> alvoAtual.offCotDirY = v);
        cMaoDirX = new ControleNumerico(pControles, "Mão X: ", -150, 150, 0, v -> alvoAtual.offMaoDirX = v);
        cMaoDirY = new ControleNumerico(pControles, "Mão Y: ", -150, 150, 0, v -> alvoAtual.offMaoDirY = v);

        pControles.add(new JLabel("--- PERNA ESQUERDA ---"));
        cJoelEsqX = new ControleNumerico(pControles, "Joel X: ", -150, 150, 0, v -> alvoAtual.offJoelEsqX = v);
        cJoelEsqY = new ControleNumerico(pControles, "Joel Y: ", -150, 150, 0, v -> alvoAtual.offJoelEsqY = v);
        cPeEsqX = new ControleNumerico(pControles, "Pé X: ", -150, 150, 0, v -> alvoAtual.offPeEsqX = v);
        cPeEsqY = new ControleNumerico(pControles, "Pé Y: ", -150, 150, 0, v -> alvoAtual.offPeEsqY = v);

        pControles.add(new JLabel("--- PERNA DIREITA ---"));
        cJoelDirX = new ControleNumerico(pControles, "Joel X: ", -150, 150, 0, v -> alvoAtual.offJoelDirX = v);
        cJoelDirY = new ControleNumerico(pControles, "Joel Y: ", -150, 150, 0, v -> alvoAtual.offJoelDirY = v);
        cPeDirX = new ControleNumerico(pControles, "Pé X: ", -150, 150, 0, v -> alvoAtual.offPeDirX = v);
        cPeDirY = new ControleNumerico(pControles, "Pé Y: ", -150, 150, 0, v -> alvoAtual.offPeDirY = v);

        JScrollPane scrollControles = new JScrollPane(pControles);
        scrollControles.getVerticalScrollBar().setUnitIncrement(16);
        painelDireito.add(scrollControles, BorderLayout.CENTER);

        // Bloco de Código, Engenharia Reversa e Animação
        txtCodigoGerado.setEditable(true);
        txtCodigoGerado.setFont(new Font("Monospaced", Font.PLAIN, 12));

        txtAnimacaoGerada.setEditable(false);
        txtAnimacaoGerada.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JTabbedPane abasCodigo = new JTabbedPane();
        abasCodigo.addTab("Pose atual", new JScrollPane(txtCodigoGerado));
        abasCodigo.addTab("Animação - 5 quadros", new JScrollPane(txtAnimacaoGerada));

        JPanel pCodigo = new JPanel(new BorderLayout());
        pCodigo.setBorder(BorderFactory.createTitledBorder("Pose / Animação"));
        pCodigo.add(abasCodigo, BorderLayout.CENTER);

        JPanel pFerramentas = new JPanel(new GridLayout(0, 1, 4, 4));

        JPanel linhaNome = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        linhaNome.add(new JLabel("Nome:"));
        linhaNome.add(txtNomeAnimacao);
        linhaNome.add(comboQuadroAnimacao);
        linhaNome.add(lblStatusQuadros);
        pFerramentas.add(linhaNome);

        JPanel linhaAnimacao = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

        JButton btnSalvarQuadro = new JButton("Salvar quadro");
        btnSalvarQuadro.addActionListener(e -> salvarQuadroAtual());

        JButton btnCarregarQuadro = new JButton("Carregar quadro");
        btnCarregarQuadro.addActionListener(e -> carregarQuadroSelecionado());

        JButton btnGerarAnimacao = new JButton("Gerar código dos 5 quadros");
        btnGerarAnimacao.addActionListener(e -> gerarCodigoAnimacao());

        linhaAnimacao.add(btnSalvarQuadro);
        linhaAnimacao.add(btnCarregarQuadro);
        linhaAnimacao.add(btnGerarAnimacao);
        pFerramentas.add(linhaAnimacao);

        JButton btnAplicarCodigo = new JButton("Ler Código Colado ⭯");
        btnAplicarCodigo.setBackground(new Color(60, 180, 80));
        btnAplicarCodigo.setForeground(Color.WHITE);
        btnAplicarCodigo.addActionListener(e -> aplicarCodigoAoVisual());
        pFerramentas.add(btnAplicarCodigo);

        pCodigo.add(pFerramentas, BorderLayout.SOUTH);

        painelDireito.add(pCodigo, BorderLayout.SOUTH);
        add(painelDireito, BorderLayout.EAST);

        atualizandoUI = false;
        carregarUIComDadosDoAlvo();
    }

    private void importarPersonagem(String caminhoClasse) {
        try {
            Class<?> clazz = Class.forName(caminhoClasse);
            Constructor<?> construtor = clazz.getConstructor(int.class, int.class, double.class);
            IrmaoMiau novoMiau = (IrmaoMiau) construtor.newInstance(0, 0, 0.7);
            
            cena.add(new DadosPersonagem(clazz.getSimpleName(), novoMiau, 350, 2, 0.7, true));
            atualizarComboAlvos();
            selecionarAlvo(cena.size() - 1);
            JOptionPane.showMessageDialog(this, "Personagem carregado com sucesso!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao importar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
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
        
        cCenaX.setValue(alvoAtual.cenaX);
        cLinha.setValue(alvoAtual.linhaAtual);
        cEscala.setValue((int)(alvoAtual.escala * 10));
        cRotGlobal.setValue(alvoAtual.rotGlobal);
        
        cCabX.setValue(alvoAtual.offCabX); cCabY.setValue(alvoAtual.offCabY); cRotCabeca.setValue(alvoAtual.rotCabeca);
        cCotEsqX.setValue(alvoAtual.offCotEsqX); cCotEsqY.setValue(alvoAtual.offCotEsqY);
        cMaoEsqX.setValue(alvoAtual.offMaoEsqX); cMaoEsqY.setValue(alvoAtual.offMaoEsqY);
        cCotDirX.setValue(alvoAtual.offCotDirX); cCotDirY.setValue(alvoAtual.offCotDirY);
        cMaoDirX.setValue(alvoAtual.offMaoDirX); cMaoDirY.setValue(alvoAtual.offMaoDirY);
        cJoelEsqX.setValue(alvoAtual.offJoelEsqX); cJoelEsqY.setValue(alvoAtual.offJoelEsqY);
        cPeEsqX.setValue(alvoAtual.offPeEsqX); cPeEsqY.setValue(alvoAtual.offPeEsqY);
        cJoelDirX.setValue(alvoAtual.offJoelDirX); cJoelDirY.setValue(alvoAtual.offJoelDirY);
        cPeDirX.setValue(alvoAtual.offPeDirX); cPeDirY.setValue(alvoAtual.offPeDirY);
        
        atualizandoUI = false;
        atualizarCodigoGerado();
        atualizarStatusQuadros();
        painelDesenho.repaint();
    }

    private void atualizarCodigoGerado() {
        if (atualizandoUI) return;
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


    // --- SISTEMA DE ANIMAÇÃO COM 5 QUADROS ---

    private void salvarQuadroAtual() {
        int indice = comboQuadroAnimacao.getSelectedIndex();

        if (indice < 0 || indice >= 5) {
            return;
        }

        alvoAtual.quadrosAnimacao[indice] = new PoseSalva(alvoAtual);
        atualizarStatusQuadros();
        gerarCodigoAnimacaoParcial();

        // Depois de salvar, já avança para o próximo quadro.
        if (indice < 4) {
            comboQuadroAnimacao.setSelectedIndex(indice + 1);
        }
    }

    private void carregarQuadroSelecionado() {
        int indice = comboQuadroAnimacao.getSelectedIndex();

        if (indice < 0 || indice >= 5) {
            return;
        }

        PoseSalva pose = alvoAtual.quadrosAnimacao[indice];

        if (pose == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Esse quadro ainda não foi salvo.",
                    "MiauStudio",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        pose.aplicarEm(alvoAtual);
        carregarUIComDadosDoAlvo();
    }

    private void atualizarStatusQuadros() {
        if (alvoAtual == null) {
            return;
        }

        StringBuilder status = new StringBuilder("Quadros: ");

        for (int i = 0; i < 5; i++) {
            status.append(alvoAtual.quadrosAnimacao[i] != null ? "●" : "○");

            if (i < 4) {
                status.append(" ");
            }
        }

        lblStatusQuadros.setText(status.toString());
    }

    private void gerarCodigoAnimacao() {
        for (int i = 0; i < 5; i++) {
            if (alvoAtual.quadrosAnimacao[i] == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Falta salvar o Quadro " + (i + 1) + ".",
                        "Animação incompleta",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        txtAnimacaoGerada.setText(montarCodigoAnimacao(false));
        txtAnimacaoGerada.setCaretPosition(0);
    }

    private void gerarCodigoAnimacaoParcial() {
        txtAnimacaoGerada.setText(montarCodigoAnimacao(true));
        txtAnimacaoGerada.setCaretPosition(0);
    }

    private String montarCodigoAnimacao(boolean permitirIncompleta) {
        String nome = normalizarNomeAnimacao(txtNomeAnimacao.getText());

        StringBuilder codigo = new StringBuilder();

        codigo.append("/*\n");
        codigo.append(" * MIAUSTUDIO - ANIMAÇÃO DE 5 QUADROS\n");
        codigo.append(" * Nome: ").append(nome).append("\n");
        codigo.append(" * Personagem: ").append(alvoAtual.nome).append("\n");
        codigo.append(" *\n");
        codigo.append(" * ORDEM DOS VALORES:\n");
        codigo.append(" * cenaX, linha, escalaX10, viradoDireita(1/0),\n");
        codigo.append(" * rotGlobal, rotCabeca, cabX, cabY,\n");
        codigo.append(" * cotEsqX, cotEsqY, maoEsqX, maoEsqY,\n");
        codigo.append(" * cotDirX, cotDirY, maoDirX, maoDirY,\n");
        codigo.append(" * joelEsqX, joelEsqY, peEsqX, peEsqY,\n");
        codigo.append(" * joelDirX, joelDirY, peDirX, peDirY\n");
        codigo.append(" */\n");

        codigo.append("private static final int[][] ")
              .append(nome)
              .append(" = {\n");

        for (int i = 0; i < 5; i++) {
            PoseSalva pose = alvoAtual.quadrosAnimacao[i];

            codigo.append("    ");

            if (pose == null) {
                codigo.append(permitirIncompleta ? "null" : "{}");
            } else {
                codigo.append(pose.paraLinhaJava());
            }

            if (i < 4) {
                codigo.append(",");
            }

            codigo.append(" // Quadro ")
                  .append(i + 1)
                  .append("\n");
        }

        codigo.append("};\n");

        return codigo.toString();
    }

    private String normalizarNomeAnimacao(String nomeOriginal) {
        String nome = nomeOriginal == null
                ? ""
                : nomeOriginal.trim().toUpperCase();

        nome = nome.replaceAll("[^A-Z0-9_]", "_");
        nome = nome.replaceAll("_+", "_");

        if (nome.isBlank()) {
            nome = "NOVA_ANIMACAO";
        }

        if (Character.isDigit(nome.charAt(0))) {
            nome = "ANIM_" + nome;
        }

        return nome;
    }

    // --- O MOTOR DE ENGENHARIA REVERSA ---
    private void aplicarCodigoAoVisual() {
        String code = txtCodigoGerado.getText();
        try {
            // Lendo o comentário de cabeçalho
            alvoAtual.rotGlobal = extrairValorInteiro(code, "ROTAÇÃO:\\s*(-?\\d+)");
            alvoAtual.rotCabeca = extrairValorInteiro(code, "R\\. CABEÇA:\\s*(-?\\d+)");
            alvoAtual.offCabX = extrairValorInteiro(code, "OFFSET CAB\\. X:\\s*(-?\\d+)");
            alvoAtual.offCabY = extrairValorInteiro(code, "Y:\\s*(-?\\d+)");

            // Lendo os membros usando Regex
            alvoAtual.offCotEsqX = extrairMembro(code, "cotoveloEsquerdoX");
            alvoAtual.offCotEsqY = extrairMembro(code, "cotoveloEsquerdoY");
            alvoAtual.offMaoEsqX = extrairMembro(code, "maoEsquerdaX");
            alvoAtual.offMaoEsqY = extrairMembro(code, "maoEsquerdaY");

            alvoAtual.offCotDirX = extrairMembro(code, "cotoveloDireitoX");
            alvoAtual.offCotDirY = extrairMembro(code, "cotoveloDireitoY");
            alvoAtual.offMaoDirX = extrairMembro(code, "maoDireitaX");
            alvoAtual.offMaoDirY = extrairMembro(code, "maoDireitaY");

            alvoAtual.offJoelEsqX = extrairMembro(code, "joelhoEsquerdoX");
            alvoAtual.offJoelEsqY = extrairMembro(code, "joelhoEsquerdoY");
            alvoAtual.offPeEsqX = extrairMembro(code, "peEsquerdoX");
            alvoAtual.offPeEsqY = extrairMembro(code, "peEsquerdoY");

            alvoAtual.offJoelDirX = extrairMembro(code, "joelhoDireitoX");
            alvoAtual.offJoelDirY = extrairMembro(code, "joelhoDireitoY");
            alvoAtual.offPeDirX = extrairMembro(code, "peDireitoX");
            alvoAtual.offPeDirY = extrairMembro(code, "peDireitoY");

            carregarUIComDadosDoAlvo();
            JOptionPane.showMessageDialog(this, "Pose aplicada com sucesso!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao interpretar código. O formato está incorreto.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int extrairValorInteiro(String texto, String regex) {
        Matcher m = Pattern.compile(regex).matcher(texto);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    private int extrairMembro(String texto, String nomeVariavel) {
        String regex = nomeVariavel + "\\s*\\+=\\s*\\(int\\)\\s*\\(\\s*(-?\\d+)\\s*\\*\\s*escala\\s*\\);";
        return extrairValorInteiro(texto, regex);
    }

    // --- RENDERIZAÇÃO E PROFUNDIDADE ---
    private class CanvasPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D gBase = (Graphics2D) g;
            gBase.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Graphics2D gCamera = (Graphics2D) gBase.create();
            double centroViewX = getWidth() / 2.0;
            double centroViewY = getHeight() / 2.0;
            gCamera.translate(centroViewX, centroViewY);
            gCamera.scale(zoomGlobalCena, zoomGlobalCena);
            gCamera.translate(-centroViewX, -centroViewY);

            // Chão (O Topo da Grama no GamePanel é Y=300)
            gCamera.setColor(new Color(80, 180, 80));
            gCamera.fillRect(-2000, 300, 5000, 2000); 

            // Para profundidade visual, desenhamos uma CÓPIA ordenada.
            // Assim a ordem do combo de personagens não muda silenciosamente.
            List<DadosPersonagem> ordemRender = new ArrayList<>(cena);
            ordemRender.sort((p1, p2) -> Integer.compare(p1.linhaAtual, p2.linhaAtual));

            for (DadosPersonagem pd : ordemRender) {
                desenharPersonagemEditor(gCamera, pd);
            }
            gCamera.dispose();
        }

        private void desenharPersonagemEditor(Graphics2D gCamera, DadosPersonagem pd) {
            Graphics2D g2d = (Graphics2D) gCamera.create();
            double s = pd.escala;
            
            // X amarrado ao controle horizontal
            double posX = pd.cenaX;
            // Y amarrado à matemática da linha (Grama começa em 300, base do Y fica em torno de 150 a 250)
            double espacamentoLinha = 25.0; // Distância visual entre cada linha
            double posY = 150 + (pd.linhaAtual * espacamentoLinha);

            // Espelhamento
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

// Cabeça (Reflection)
            Graphics2D gCabeca = (Graphics2D) g2d.create();
            
            // 1. CORREÇÃO: Movemos a origem da cabeça para a posição real do boneco na cena
            gCabeca.translate(posX, posY);
            
            // 2. Aplicamos os offsets dos sliders manuais
            gCabeca.translate(pd.offCabX * s, pd.offCabY * s);
            
            // 3. CORREÇÃO: Como a origem já andou, o pivot de rotação passa a ser relativo (removemos o posX e posY da conta)
            double pivotCabX = 35 * s;
            double pivotCabY = 53 * s;
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

 // Descobre se é o Miau Preto para aplicar a geometria correta
            boolean isPreto = pd.nome.contains("Preto");

            // CORPO E FAIXA
            int corpoY = (int)(posY + 70 * s);
            Polygon corpo = new Polygon();
            
            if (isPreto) {
                corpo.addPoint((int)(posX - 4*s), corpoY);
                corpo.addPoint((int)(posX + 78*s), corpoY);
                corpo.addPoint((int)(posX + 61*s), (int)(corpoY + 100*s));
                corpo.addPoint((int)(posX + 10*s), (int)(corpoY + 100*s));
                
                g2d.setColor(pd.corKimono);
                g2d.fillPolygon(corpo);
                g2d.setColor(new Color(55, 55, 220));
                g2d.fillRect((int)(posX - 6*s), (int)(corpoY + 80*s), (int)(90*s), (int)(12*s));
            } else {
                corpo.addPoint((int)(posX - 10*s), corpoY);
                corpo.addPoint((int)(posX + 82*s), corpoY);
                corpo.addPoint((int)(posX + 64*s), (int)(corpoY + 105*s));
                corpo.addPoint((int)(posX + 8*s), (int)(corpoY + 105*s));
                
                g2d.setColor(pd.corKimono);
                g2d.fillPolygon(corpo);
                g2d.setColor(new Color(55, 55, 220));
                g2d.fillRect((int)(posX - 8*s), (int)(corpoY + 82*s), (int)(96*s), (int)(12*s));
            }

            // BASES DE JUNTAS ESPECÍFICAS POR PERSONAGEM
            int ombroEsqX, ombroEsqY, baseCotEsqX, baseCotEsqY, baseMaoEsqX, baseMaoEsqY;
            int ombroDirX, ombroDirY, baseCotDirX, baseCotDirY, baseMaoDirX, baseMaoDirY;
            int quadEsqX, quadEsqY, baseJoelEsqX, baseJoelEsqY, basePeEsqX, basePeEsqY;
            int quadDirX, quadDirY, baseJoelDirX, baseJoelDirY, basePeDirX, basePeDirY;

            if (isPreto) {
                ombroEsqX = (int)(posX + 5*s); ombroEsqY = (int)(corpoY + 22*s);
                baseCotEsqX = (int)(posX - 8*s); baseCotEsqY = (int)(corpoY + 52*s);
                baseMaoEsqX = (int)(posX + 35*s); baseMaoEsqY = (int)(corpoY + 57*s);

                ombroDirX = (int)(posX + 70*s); ombroDirY = (int)(corpoY + 23*s);
                baseCotDirX = (int)(posX + 88*s); baseCotDirY = (int)(corpoY + 48*s);
                baseMaoDirX = (int)(posX + 108*s); baseMaoDirY = (int)(corpoY + 62*s);

                quadEsqX = (int)(posX + 24*s); quadEsqY = (int)(corpoY + 97*s);
                baseJoelEsqX = (int)(posX + 15*s); baseJoelEsqY = (int)(corpoY + 132*s);
                basePeEsqX = (int)(posX + 4*s); basePeEsqY = (int)(corpoY + 171*s);

                quadDirX = (int)(posX + 50*s); quadDirY = (int)(corpoY + 97*s);
                baseJoelDirX = (int)(posX + 59*s); baseJoelDirY = (int)(corpoY + 132*s);
                basePeDirX = (int)(posX + 72*s); basePeDirY = (int)(corpoY + 171*s);
            } else {
                ombroEsqX = (int)(posX + 3*s); ombroEsqY = (int)(corpoY + 22*s);
                baseCotEsqX = (int)(posX - 14*s); baseCotEsqY = (int)(corpoY + 42*s);
                baseMaoEsqX = (int)(posX + 3*s); baseMaoEsqY = (int)(corpoY + 58*s);

                ombroDirX = (int)(posX + 73*s); ombroDirY = (int)(corpoY + 20*s);
                baseCotDirX = (int)(posX + 95*s); baseCotDirY = (int)(corpoY + 36*s);
                baseMaoDirX = (int)(posX + 111*s); baseMaoDirY = (int)(corpoY + 29*s);

                quadEsqX = (int)(posX + 22*s); quadEsqY = (int)(corpoY + 100*s);
                baseJoelEsqX = (int)(posX + 10*s); baseJoelEsqY = (int)(corpoY + 136*s);
                basePeEsqX = (int)(posX + 0*s); basePeEsqY = (int)(corpoY + 174*s);

                quadDirX = (int)(posX + 52*s); quadDirY = (int)(corpoY + 100*s);
                baseJoelDirX = (int)(posX + 66*s); baseJoelDirY = (int)(corpoY + 137*s);
                basePeDirX = (int)(posX + 82*s); basePeDirY = (int)(corpoY + 171*s);
            }

            // JUNTAS REAIS = BASE ORIGINAL + OFFSETS DOS SLIDERS
            int cotEsqX = (int)(baseCotEsqX + pd.offCotEsqX*s);
            int cotEsqY = (int)(baseCotEsqY + pd.offCotEsqY*s);
            int maoEsqX = (int)(baseMaoEsqX + pd.offMaoEsqX*s);
            int maoEsqY = (int)(baseMaoEsqY + pd.offMaoEsqY*s);

            int cotDirX = (int)(baseCotDirX + pd.offCotDirX*s);
            int cotDirY = (int)(baseCotDirY + pd.offCotDirY*s);
            int maoDirX = (int)(baseMaoDirX + pd.offMaoDirX*s);
            int maoDirY = (int)(baseMaoDirY + pd.offMaoDirY*s);

            int joelEsqX = (int)(baseJoelEsqX + pd.offJoelEsqX*s);
            int joelEsqY = (int)(baseJoelEsqY + pd.offJoelEsqY*s);
            int peEsqX = (int)(basePeEsqX + pd.offPeEsqX*s);
            int peEsqY = (int)(basePeEsqY + pd.offPeEsqY*s);

            int joelDirX = (int)(baseJoelDirX + pd.offJoelDirX*s);
            int joelDirY = (int)(baseJoelDirY + pd.offJoelDirY*s);
            int peDirX = (int)(basePeDirX + pd.offPeDirX*s);
            int peDirY = (int)(basePeDirY + pd.offPeDirY*s);

            // DESENHANDO AS LINHAS
            g2d.setStroke(new BasicStroke((float) (18 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(pd.corKimono);
            g2d.drawLine(ombroEsqX, ombroEsqY, cotEsqX, cotEsqY); g2d.drawLine(cotEsqX, cotEsqY, maoEsqX, maoEsqY);
            g2d.drawLine(ombroDirX, ombroDirY, cotDirX, cotDirY); g2d.drawLine(cotDirX, cotDirY, maoDirX, maoDirY);
            g2d.drawLine(quadEsqX, quadEsqY, joelEsqX, joelEsqY); g2d.drawLine(joelEsqX, joelEsqY, peEsqX, peEsqY);
            g2d.drawLine(quadDirX, quadDirY, joelDirX, joelDirY); g2d.drawLine(joelDirX, joelDirY, peDirX, peDirY);

            // DESENHANDO AS PATAS
            g2d.setColor(pd.corPelo);
            int lMao = (int)(20*s), aMao = (int)(18*s), lPe = (int)(25*s), aPe = (int)(17*s);
            g2d.fillOval(maoEsqX - lMao/2, maoEsqY - aMao/2, lMao, aMao);
            g2d.fillOval(maoDirX - lMao/2, maoDirY - aMao/2, lMao, aMao);
            g2d.fillOval(peEsqX - lPe/2, peEsqY - aPe/2, lPe, aPe);
            g2d.fillOval(peDirX - lPe/2, peDirY - aPe/2, lPe, aPe);

            // Setinha vermelha indicando o alvo atual
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