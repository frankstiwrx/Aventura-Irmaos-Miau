package com.irmaosmiau.editor;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

        // Novo root do rig.
        int offCinturaX = 0, offCinturaY = 0, rotCintura = 0;

        // Tronco superior articulado.
        int offPeitoX = 0, offPeitoY = 0, rotPeito = 0;

        int offCabX = 0, offCabY = 0;
        int offCotEsqX = 0, offCotEsqY = 0, offMaoEsqX = 0, offMaoEsqY = 0;
        int offCotDirX = 0, offCotDirY = 0, offMaoDirX = 0, offMaoDirY = 0;
        int offJoelEsqX = 0, offJoelEsqY = 0, offPeEsqX = 0, offPeEsqY = 0;
        int offJoelDirX = 0, offJoelDirY = 0, offPeDirX = 0, offPeDirY = 0;

        // Cada personagem mantém seus próprios 5 quadros.
        PoseSalva[] quadrosAnimacao = new PoseSalva[5];

        /*
         * Histórico independente de edição.
         * Branco e Preto possuem seus próprios Ctrl+Z / Ctrl+Y.
         */
        Deque<PoseSalva> historicoDesfazer = new ArrayDeque<>();
        Deque<PoseSalva> historicoRefazer = new ArrayDeque<>();

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
                    registrarInicioDeEdicao();
                    spinner.setValue(slider.getValue());
                    acao.accept(slider.getValue());
                    atualizarCodigoGerado();
                    painelDesenho.repaint();
                }
            });

            // Sincronização Spinner -> Slider -> Ação
            spinner.addChangeListener(e -> {
                if (!atualizandoUI) {
                    registrarInicioDeEdicao();
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

        int offCinturaX;
        int offCinturaY;
        int rotCintura;

        int offPeitoX;
        int offPeitoY;
        int rotPeito;

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

            offCinturaX = pd.offCinturaX;
            offCinturaY = pd.offCinturaY;
            rotCintura = pd.rotCintura;

            offPeitoX = pd.offPeitoX;
            offPeitoY = pd.offPeitoY;
            rotPeito = pd.rotPeito;

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

            pd.offCinturaX = offCinturaX;
            pd.offCinturaY = offCinturaY;
            pd.rotCintura = rotCintura;

            pd.offPeitoX = offPeitoX;
            pd.offPeitoY = offPeitoY;
            pd.rotPeito = rotPeito;

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
                "{%d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d}",
                cenaX,
                linhaAtual,
                escalaX10,
                viradoDireita ? 1 : 0,
                offCinturaX,
                offCinturaY,
                rotCintura,
                offPeitoX,
                offPeitoY,
                rotPeito,
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
    private ControleNumerico cCinturaX, cCinturaY, cRotCintura;
    private ControleNumerico cPeitoX, cPeitoY, cRotPeito;
    private ControleNumerico cRotCabeca, cCabX, cCabY;
    private ControleNumerico cCotEsqX, cCotEsqY, cMaoEsqX, cMaoEsqY;
    private ControleNumerico cCotDirX, cCotDirY, cMaoDirX, cMaoDirY;
    private ControleNumerico cJoelEsqX, cJoelEsqY, cPeEsqX, cPeEsqY;
    private ControleNumerico cJoelDirX, cJoelDirY, cPeDirX, cPeDirY;
    private JCheckBox chkViradoDireita;

    // --- GUIAS / ARRASTE DIRETO NO CANVAS ---
    private boolean arrasteRigAtivo = true;
    private JToggleButton btnArrasteRig;

    // --- PRÉ-VISUALIZAÇÃO DA ANIMAÇÃO ---
    private JButton btnPlayAnimacao;
    private JSpinner spinIntervaloAnimacao;
    private boolean previewAnimacaoAtivo = false;
    private PoseSalva poseAntesPreview = null;
    private int indiceQuadroAntesPreview = 0;
    private int indicePreviewAnimacao = -1;

    private final Timer timerPreviewAnimacao =
            new Timer(
                    230,
                    e -> avancarPreviewAnimacao()
            );

    // --- HISTÓRICO DESFAZER / REFAZER ---
    private static final int LIMITE_HISTORICO = 100;
    private JButton btnDesfazer;
    private JButton btnRefazer;

    /*
     * Várias mudanças muito próximas (por exemplo, arrastar um slider)
     * contam como uma única edição no Ctrl+Z.
     */
    private boolean agrupandoHistorico = false;
    private DadosPersonagem personagemHistoricoAtual = null;

    private final Timer timerAgruparHistorico =
            new Timer(
                    400,
                    e -> finalizarGrupoHistorico()
            );

    private JPanel criarCabecalhoComReset(
            String titulo,
            Runnable acaoReset
    ) {

        JPanel linha =
                new JPanel(
                        new BorderLayout(
                                6,
                                0
                        )
                );

        linha.add(
                new JLabel(titulo),
                BorderLayout.CENTER
        );

        JButton btnZerar =
                new JButton("↺ Zerar");

        btnZerar.setMargin(
                new Insets(
                        2,
                        8,
                        2,
                        8
                )
        );

        btnZerar.addActionListener(
                e -> zerarSetor(acaoReset)
        );

        linha.add(
                btnZerar,
                BorderLayout.EAST
        );

        return linha;
    }

    private void zerarSetor(
            Runnable acaoReset
    ) {

        if (alvoAtual == null) {
            return;
        }

        pararPreviewAnimacao(true);

        registrarEdicaoDiscreta();

        acaoReset.run();

        carregarUIComDadosDoAlvo();
    }

    private void zerarPoseInteira() {

        zerarSetor(() -> {

            // Rotação global.
            alvoAtual.rotGlobal = 0;

            // Cintura / root.
            alvoAtual.offCinturaX = 0;
            alvoAtual.offCinturaY = 0;
            alvoAtual.rotCintura = 0;

            // Tronco.
            alvoAtual.offPeitoX = 0;
            alvoAtual.offPeitoY = 0;
            alvoAtual.rotPeito = 0;

            // Cabeça / pescoço.
            alvoAtual.rotCabeca = 0;
            alvoAtual.offCabX = 0;
            alvoAtual.offCabY = 0;

            // Braço esquerdo.
            alvoAtual.offCotEsqX = 0;
            alvoAtual.offCotEsqY = 0;
            alvoAtual.offMaoEsqX = 0;
            alvoAtual.offMaoEsqY = 0;

            // Braço direito.
            alvoAtual.offCotDirX = 0;
            alvoAtual.offCotDirY = 0;
            alvoAtual.offMaoDirX = 0;
            alvoAtual.offMaoDirY = 0;

            // Perna esquerda.
            alvoAtual.offJoelEsqX = 0;
            alvoAtual.offJoelEsqY = 0;
            alvoAtual.offPeEsqX = 0;
            alvoAtual.offPeEsqY = 0;

            // Perna direita.
            alvoAtual.offJoelDirX = 0;
            alvoAtual.offJoelDirY = 0;
            alvoAtual.offPeDirX = 0;
            alvoAtual.offPeDirY = 0;
        });
    }

    public EditorDePoses() {
        setTitle("MiauStudio Ultimate - Rig de Cintura, Tronco & Animação");
        setSize(1450, 950);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        timerAgruparHistorico.setRepeats(false);
        timerPreviewAnimacao.setRepeats(true);

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

        btnDesfazer = new JButton("↶ Desfazer");
        btnDesfazer.addActionListener(e -> desfazer());
        painelTop.add(btnDesfazer);

        btnRefazer = new JButton("↷ Refazer");
        btnRefazer.addActionListener(e -> refazer());
        painelTop.add(btnRefazer);

        painelTop.add(new JSeparator(SwingConstants.VERTICAL));

        btnArrasteRig = new JToggleButton("● Arraste: ON", true);
        btnArrasteRig.setToolTipText(
                "Mostra/oculta os pontos do rig e ativa/desativa o arraste direto."
        );
        btnArrasteRig.addActionListener(e -> {

            arrasteRigAtivo =
                    btnArrasteRig.isSelected();

            btnArrasteRig.setText(
                    arrasteRigAtivo
                            ? "● Arraste: ON"
                            : "○ Arraste: OFF"
            );

            if (!arrasteRigAtivo) {
                painelDesenho.cancelarInteracaoRig();
            }

            painelDesenho.repaint();
        });
        painelTop.add(btnArrasteRig);

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
            if (!atualizandoUI) {
                registrarInicioDeEdicao();
                alvoAtual.viradoDireita = chkViradoDireita.isSelected();
                atualizarCodigoGerado();
                painelDesenho.repaint();
            }
        });
        pControles.add(chkViradoDireita);

        JButton btnZerarPoseInteira =
                new JButton("↺ Zerar pose inteira");

        btnZerarPoseInteira.setToolTipText(
                "Zera rotações e offsets do rig, preservando posição, linha, escala e orientação."
        );

        btnZerarPoseInteira.addActionListener(
                e -> zerarPoseInteira()
        );

        pControles.add(btnZerarPoseInteira);

        // --- CONTROLES DE POSIÇÃO NO CENÁRIO ---
        pControles.add(new JLabel("--- POSIÇÃO NO CENÁRIO ---"));
        cCenaX = new ControleNumerico(pControles, "Cena X: ", -500, 1500, 220, v -> alvoAtual.cenaX = v);
        cLinha = new ControleNumerico(pControles, "Linha (0-4): ", 0, 4, 2, v -> alvoAtual.linhaAtual = v);
        
        cEscala = new ControleNumerico(pControles, "Escala (x10): ", 2, 30, 7, v -> alvoAtual.escala = v / 10.0);
        cRotGlobal = new ControleNumerico(pControles, "ROT. GERAL: ", -180, 180, 0, v -> alvoAtual.rotGlobal = v);

        pControles.add(
                criarCabecalhoComReset(
                        "--- EIXO CENTRAL / CINTURA ---",
                        () -> {
                            alvoAtual.offCinturaX = 0;
                            alvoAtual.offCinturaY = 0;
                            alvoAtual.rotCintura = 0;
                        }
                )
        );
        cCinturaX = new ControleNumerico(pControles, "Raiz X: ", -200, 200, 0, v -> alvoAtual.offCinturaX = v);
        cCinturaY = new ControleNumerico(pControles, "Raiz Y: ", -200, 200, 0, v -> alvoAtual.offCinturaY = v);
        cRotCintura = new ControleNumerico(pControles, "Rot. cintura: ", -180, 180, 0, v -> alvoAtual.rotCintura = v);


        // --- TRONCO SUPERIOR ---
        // O tronco dobra usando a própria cintura como pivô.
        pControles.add(
                criarCabecalhoComReset(
                        "--- TRONCO SUPERIOR ---",
                        () -> {
                            alvoAtual.offPeitoX = 0;
                            alvoAtual.offPeitoY = 0;
                            alvoAtual.rotPeito = 0;
                        }
                )
        );
        cPeitoX = new ControleNumerico(
                pControles,
                "Tronco X: ",
                -150,
                150,
                0,
                v -> alvoAtual.offPeitoX = v
        );
        cPeitoY = new ControleNumerico(
                pControles,
                "Tronco Y: ",
                -150,
                150,
                0,
                v -> alvoAtual.offPeitoY = v
        );
        cRotPeito = new ControleNumerico(
                pControles,
                "Rot. tronco: ",
                -180,
                180,
                0,
                v -> alvoAtual.rotPeito = v
        );
        
        pControles.add(
                criarCabecalhoComReset(
                        "--- CABEÇA / PESCOÇO ---",
                        () -> {
                            alvoAtual.rotCabeca = 0;
                            alvoAtual.offCabX = 0;
                            alvoAtual.offCabY = 0;
                        }
                )
        );
        cCabX = new ControleNumerico(pControles, "Off X: ", -100, 100, 0, v -> alvoAtual.offCabX = v);
        cCabY = new ControleNumerico(pControles, "Off Y: ", -100, 100, 0, v -> alvoAtual.offCabY = v);
        cRotCabeca = new ControleNumerico(pControles, "Rotação: ", -180, 180, 0, v -> alvoAtual.rotCabeca = v);

        pControles.add(
                criarCabecalhoComReset(
                        "--- BRAÇO ESQUERDO ---",
                        () -> {
                            alvoAtual.offCotEsqX = 0;
                            alvoAtual.offCotEsqY = 0;
                            alvoAtual.offMaoEsqX = 0;
                            alvoAtual.offMaoEsqY = 0;
                        }
                )
        );
        cCotEsqX = new ControleNumerico(pControles, "Cot X: ", -150, 150, 0, v -> alvoAtual.offCotEsqX = v);
        cCotEsqY = new ControleNumerico(pControles, "Cot Y: ", -150, 150, 0, v -> alvoAtual.offCotEsqY = v);
        cMaoEsqX = new ControleNumerico(pControles, "Mão X: ", -150, 150, 0, v -> alvoAtual.offMaoEsqX = v);
        cMaoEsqY = new ControleNumerico(pControles, "Mão Y: ", -150, 150, 0, v -> alvoAtual.offMaoEsqY = v);

        pControles.add(
                criarCabecalhoComReset(
                        "--- BRAÇO DIREITO ---",
                        () -> {
                            alvoAtual.offCotDirX = 0;
                            alvoAtual.offCotDirY = 0;
                            alvoAtual.offMaoDirX = 0;
                            alvoAtual.offMaoDirY = 0;
                        }
                )
        );
        cCotDirX = new ControleNumerico(pControles, "Cot X: ", -150, 150, 0, v -> alvoAtual.offCotDirX = v);
        cCotDirY = new ControleNumerico(pControles, "Cot Y: ", -150, 150, 0, v -> alvoAtual.offCotDirY = v);
        cMaoDirX = new ControleNumerico(pControles, "Mão X: ", -150, 150, 0, v -> alvoAtual.offMaoDirX = v);
        cMaoDirY = new ControleNumerico(pControles, "Mão Y: ", -150, 150, 0, v -> alvoAtual.offMaoDirY = v);

        pControles.add(
                criarCabecalhoComReset(
                        "--- PERNA ESQUERDA ---",
                        () -> {
                            alvoAtual.offJoelEsqX = 0;
                            alvoAtual.offJoelEsqY = 0;
                            alvoAtual.offPeEsqX = 0;
                            alvoAtual.offPeEsqY = 0;
                        }
                )
        );
        cJoelEsqX = new ControleNumerico(pControles, "Joel X: ", -150, 150, 0, v -> alvoAtual.offJoelEsqX = v);
        cJoelEsqY = new ControleNumerico(pControles, "Joel Y: ", -150, 150, 0, v -> alvoAtual.offJoelEsqY = v);
        cPeEsqX = new ControleNumerico(pControles, "Pé X: ", -150, 150, 0, v -> alvoAtual.offPeEsqX = v);
        cPeEsqY = new ControleNumerico(pControles, "Pé Y: ", -150, 150, 0, v -> alvoAtual.offPeEsqY = v);

        pControles.add(
                criarCabecalhoComReset(
                        "--- PERNA DIREITA ---",
                        () -> {
                            alvoAtual.offJoelDirX = 0;
                            alvoAtual.offJoelDirY = 0;
                            alvoAtual.offPeDirX = 0;
                            alvoAtual.offPeDirY = 0;
                        }
                )
        );
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

        JPanel linhaPreview =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                4,
                                0
                        )
                );

        btnPlayAnimacao =
                new JButton("▶ Play");

        btnPlayAnimacao.setToolTipText(
                "Pré-visualiza os quadros salvos sem alterar sua pose de trabalho."
        );

        btnPlayAnimacao.addActionListener(
                e -> alternarPreviewAnimacao()
        );

        spinIntervaloAnimacao =
                new JSpinner(
                        new SpinnerNumberModel(
                                230,
                                50,
                                2000,
                                10
                        )
                );

        spinIntervaloAnimacao.setPreferredSize(
                new Dimension(
                        75,
                        24
                )
        );

        spinIntervaloAnimacao.addChangeListener(
                e -> timerPreviewAnimacao.setDelay(
                        (int) spinIntervaloAnimacao.getValue()
                )
        );

        linhaPreview.add(btnPlayAnimacao);
        linhaPreview.add(new JLabel("Intervalo:"));
        linhaPreview.add(spinIntervaloAnimacao);
        linhaPreview.add(new JLabel("ms / quadro"));

        pFerramentas.add(linhaPreview);

        JButton btnAplicarCodigo = new JButton("Ler Código Colado ⭯");
        btnAplicarCodigo.setBackground(new Color(60, 180, 80));
        btnAplicarCodigo.setForeground(Color.WHITE);
        btnAplicarCodigo.addActionListener(e -> aplicarCodigoAoVisual());
        pFerramentas.add(btnAplicarCodigo);

        pCodigo.add(pFerramentas, BorderLayout.SOUTH);

        painelDireito.add(pCodigo, BorderLayout.SOUTH);
        add(painelDireito, BorderLayout.EAST);

        configurarAtalhosHistorico();

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
            pararPreviewAnimacao(true);
            finalizarGrupoHistorico();
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

        cCinturaX.setValue(alvoAtual.offCinturaX);
        cCinturaY.setValue(alvoAtual.offCinturaY);
        cRotCintura.setValue(alvoAtual.rotCintura);

        cPeitoX.setValue(alvoAtual.offPeitoX);
        cPeitoY.setValue(alvoAtual.offPeitoY);
        cRotPeito.setValue(alvoAtual.rotPeito);
        
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
        atualizarBotoesHistorico();
        painelDesenho.repaint();
    }

    // =============================
    // HISTÓRICO: DESFAZER / REFAZER
    // =============================

    private void registrarInicioDeEdicao() {

        if (previewAnimacaoAtivo) {
            pararPreviewAnimacao(true);
        }

        if (atualizandoUI || alvoAtual == null) {
            return;
        }

        /*
         * O primeiro evento de uma edição guarda o estado anterior.
         * Eventos seguintes, enquanto o usuário continua arrastando/
         * mexendo no controle, pertencem ao mesmo passo do histórico.
         */
        if (!agrupandoHistorico
                || personagemHistoricoAtual != alvoAtual) {

            adicionarAoHistorico(
                    alvoAtual.historicoDesfazer,
                    new PoseSalva(alvoAtual)
            );

            alvoAtual.historicoRefazer.clear();

            agrupandoHistorico = true;
            personagemHistoricoAtual = alvoAtual;

            atualizarBotoesHistorico();
        }

        timerAgruparHistorico.restart();
    }

    private void registrarEdicaoDiscreta() {

        finalizarGrupoHistorico();
        registrarInicioDeEdicao();
        finalizarGrupoHistorico();
    }

    private void continuarGrupoHistorico() {

        if (agrupandoHistorico) {
            timerAgruparHistorico.restart();
        }
    }

    private void finalizarGrupoHistorico() {

        timerAgruparHistorico.stop();
        agrupandoHistorico = false;
        personagemHistoricoAtual = null;
    }

    private void adicionarAoHistorico(
            Deque<PoseSalva> historico,
            PoseSalva pose
    ) {

        historico.push(pose);

        while (historico.size() > LIMITE_HISTORICO) {
            historico.removeLast();
        }
    }

    private void desfazer() {

        pararPreviewAnimacao(true);
        finalizarGrupoHistorico();

        if (alvoAtual == null
                || alvoAtual.historicoDesfazer.isEmpty()) {
            return;
        }

        adicionarAoHistorico(
                alvoAtual.historicoRefazer,
                new PoseSalva(alvoAtual)
        );

        PoseSalva estadoAnterior =
                alvoAtual.historicoDesfazer.pop();

        estadoAnterior.aplicarEm(alvoAtual);

        carregarUIComDadosDoAlvo();
    }

    private void refazer() {

        pararPreviewAnimacao(true);
        finalizarGrupoHistorico();

        if (alvoAtual == null
                || alvoAtual.historicoRefazer.isEmpty()) {
            return;
        }

        adicionarAoHistorico(
                alvoAtual.historicoDesfazer,
                new PoseSalva(alvoAtual)
        );

        PoseSalva estadoSeguinte =
                alvoAtual.historicoRefazer.pop();

        estadoSeguinte.aplicarEm(alvoAtual);

        carregarUIComDadosDoAlvo();
    }

    private void atualizarBotoesHistorico() {

        if (btnDesfazer == null || btnRefazer == null) {
            return;
        }

        boolean podeDesfazer =
                alvoAtual != null
                && !alvoAtual.historicoDesfazer.isEmpty();

        boolean podeRefazer =
                alvoAtual != null
                && !alvoAtual.historicoRefazer.isEmpty();

        btnDesfazer.setEnabled(podeDesfazer);
        btnRefazer.setEnabled(podeRefazer);

        String nomeAlvo =
                alvoAtual == null
                ? "personagem"
                : alvoAtual.nome;

        int qtdDesfazer =
                alvoAtual == null
                ? 0
                : alvoAtual.historicoDesfazer.size();

        int qtdRefazer =
                alvoAtual == null
                ? 0
                : alvoAtual.historicoRefazer.size();

        btnDesfazer.setToolTipText(
                "Ctrl+Z — desfazer no "
                + nomeAlvo
                + " (" + qtdDesfazer + ")"
        );

        btnRefazer.setToolTipText(
                "Ctrl+Y / Ctrl+Shift+Z — refazer no "
                + nomeAlvo
                + " (" + qtdRefazer + ")"
        );
    }

    private void configurarAtalhosHistorico() {

        int mascaraAtalho =
                Toolkit.getDefaultToolkit()
                        .getMenuShortcutKeyMaskEx();

        JRootPane raiz =
                getRootPane();

        InputMap inputMap =
                raiz.getInputMap(
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                );

        ActionMap actionMap =
                raiz.getActionMap();

        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_Z,
                        mascaraAtalho
                ),
                "miauDesfazer"
        );

        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_Y,
                        mascaraAtalho
                ),
                "miauRefazer"
        );

        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_Z,
                        mascaraAtalho
                        | InputEvent.SHIFT_DOWN_MASK
                ),
                "miauRefazer"
        );

        actionMap.put(
                "miauDesfazer",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent e
                    ) {
                        desfazer();
                    }
                }
        );

        actionMap.put(
                "miauRefazer",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent e
                    ) {
                        refazer();
                    }
                }
        );
    }

    private void atualizarCodigoGerado() {
        if (atualizandoUI) return;
        String codigo = String.format(
            "// ROTAÇÃO: %d | R. CABEÇA: %d | OFFSET CAB. X: %d | Y: %d\n" +
            "// CINTURA X: %d | CINTURA Y: %d | ROT. CINTURA: %d\n" +
            "// TRONCO X: %d | TRONCO Y: %d | ROT. TRONCO: %d\n\n" +
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
            alvoAtual.offCinturaX, alvoAtual.offCinturaY, alvoAtual.rotCintura,
            alvoAtual.offPeitoX, alvoAtual.offPeitoY, alvoAtual.rotPeito,
            alvoAtual.offCotEsqX, alvoAtual.offCotEsqY, alvoAtual.offMaoEsqX, alvoAtual.offMaoEsqY,
            alvoAtual.offCotDirX, alvoAtual.offCotDirY, alvoAtual.offMaoDirX, alvoAtual.offMaoDirY,
            alvoAtual.offJoelEsqX, alvoAtual.offJoelEsqY, alvoAtual.offPeEsqX, alvoAtual.offPeEsqY,
            alvoAtual.offJoelDirX, alvoAtual.offJoelDirY, alvoAtual.offPeDirX, alvoAtual.offPeDirY
        );
        txtCodigoGerado.setText(codigo);
        txtCodigoGerado.setCaretPosition(0);
    }


    // --- SISTEMA DE ANIMAÇÃO COM 5 QUADROS ---

    private void alternarPreviewAnimacao() {

        if (previewAnimacaoAtivo) {
            pararPreviewAnimacao(true);
        } else {
            iniciarPreviewAnimacao();
        }
    }

    private void iniciarPreviewAnimacao() {

        if (alvoAtual == null) {
            return;
        }

        int quadrosSalvos = 0;

        for (PoseSalva pose : alvoAtual.quadrosAnimacao) {
            if (pose != null) {
                quadrosSalvos++;
            }
        }

        if (quadrosSalvos < 2) {

            JOptionPane.showMessageDialog(
                    this,
                    "Salve pelo menos 2 quadros para pré-visualizar a animação.",
                    "Pré-visualização",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        finalizarGrupoHistorico();

        poseAntesPreview =
                new PoseSalva(alvoAtual);

        indiceQuadroAntesPreview =
                comboQuadroAnimacao.getSelectedIndex();

        indicePreviewAnimacao = -1;
        previewAnimacaoAtivo = true;

        btnPlayAnimacao.setText("■ Parar");

        timerPreviewAnimacao.setDelay(
                (int) spinIntervaloAnimacao.getValue()
        );

        /*
         * Mostra imediatamente o primeiro quadro disponível;
         * não precisamos esperar o primeiro disparo do Timer.
         */
        avancarPreviewAnimacao();

        timerPreviewAnimacao.start();

        painelDesenho.cancelarInteracaoRig();
    }

    private void avancarPreviewAnimacao() {

        if (!previewAnimacaoAtivo
                || alvoAtual == null) {

            return;
        }

        /*
         * Procura o próximo quadro salvo.
         * Assim também conseguimos pré-visualizar animações
         * ainda incompletas durante a criação.
         */
        for (int tentativa = 1;
                tentativa <= 5;
                tentativa++) {

            int proximo =
                    (indicePreviewAnimacao + tentativa)
                    % 5;

            PoseSalva pose =
                    alvoAtual.quadrosAnimacao[
                            proximo
                    ];

            if (pose == null) {
                continue;
            }

            indicePreviewAnimacao =
                    proximo;

            pose.aplicarEm(alvoAtual);

            comboQuadroAnimacao.setSelectedIndex(
                    proximo
            );

            painelDesenho.repaint();

            return;
        }
    }

    private void pararPreviewAnimacao(
            boolean restaurarPose
    ) {

        if (!previewAnimacaoAtivo) {
            return;
        }

        timerPreviewAnimacao.stop();

        previewAnimacaoAtivo = false;

        if (restaurarPose
                && poseAntesPreview != null
                && alvoAtual != null) {

            poseAntesPreview.aplicarEm(
                    alvoAtual
            );
        }

        poseAntesPreview = null;
        indicePreviewAnimacao = -1;

        if (btnPlayAnimacao != null) {
            btnPlayAnimacao.setText("▶ Play");
        }

        if (comboQuadroAnimacao != null
                && indiceQuadroAntesPreview >= 0
                && indiceQuadroAntesPreview < 5) {

            comboQuadroAnimacao.setSelectedIndex(
                    indiceQuadroAntesPreview
            );
        }

        if (alvoAtual != null) {
            carregarUIComDadosDoAlvo();
        } else {
            painelDesenho.repaint();
        }
    }

    private void salvarQuadroAtual() {

        pararPreviewAnimacao(true);

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

        pararPreviewAnimacao(true);

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

        registrarEdicaoDiscreta();

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
        codigo.append(" * cinturaX, cinturaY, rotCintura,\n");
        codigo.append(" * troncoX, troncoY, rotTronco,\n");
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
            registrarEdicaoDiscreta();

            // Lendo o comentário de cabeçalho
            alvoAtual.rotGlobal = extrairValorInteiro(code, "ROTAÇÃO:\\s*(-?\\d+)");
            alvoAtual.rotCabeca = extrairValorInteiro(code, "R\\. CABEÇA:\\s*(-?\\d+)");
            alvoAtual.offCabX = extrairValorInteiro(code, "OFFSET CAB\\. X:\\s*(-?\\d+)");
            alvoAtual.offCabY = extrairValorInteiro(code, "OFFSET CAB\\. X:\\s*-?\\d+\\s*\\|\\s*Y:\\s*(-?\\d+)");

            // Novo root da cintura. Em códigos antigos esses valores simplesmente ficam em 0.
            alvoAtual.offCinturaX = extrairValorInteiro(code, "CINTURA X:\\s*(-?\\d+)");
            alvoAtual.offCinturaY = extrairValorInteiro(code, "CINTURA Y:\\s*(-?\\d+)");
            alvoAtual.rotCintura = extrairValorInteiro(code, "ROT\\. CINTURA:\\s*(-?\\d+)");

            // Tronco superior. Também aceita o formato antigo chamado PEITO.
            alvoAtual.offPeitoX = extrairValorInteiro(code, "(?:TRONCO|PEITO) X:\\s*(-?\\d+)");
            alvoAtual.offPeitoY = extrairValorInteiro(code, "(?:TRONCO|PEITO) Y:\\s*(-?\\d+)");
            alvoAtual.rotPeito = extrairValorInteiro(code, "ROT\\. (?:TRONCO|PEITO):\\s*(-?\\d+)");

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

        /*
         * Primeira versão do rig direto no canvas:
         * mãos, cotovelos, joelhos e pés podem ser arrastados.
         */
        private enum PontoRig {
            COTOVELO_ESQUERDO("Cotovelo E"),
            MAO_ESQUERDA("Mão E"),
            COTOVELO_DIREITO("Cotovelo D"),
            MAO_DIREITA("Mão D"),
            JOELHO_ESQUERDO("Joelho E"),
            PE_ESQUERDO("Pé E"),
            JOELHO_DIREITO("Joelho D"),
            PE_DIREITO("Pé D");

            private final String rotulo;

            PontoRig(String rotulo) {
                this.rotulo = rotulo;
            }

            public String getRotulo() {
                return rotulo;
            }
        }

        private class HandleRig {
            PontoRig tipo;
            Point2D.Double pontoTela;
            Point2D.Double baseLocal;
            AffineTransform localParaTela;

            HandleRig(
                    PontoRig tipo,
                    Point2D.Double pontoTela,
                    Point2D.Double baseLocal,
                    AffineTransform localParaTela
            ) {
                this.tipo = tipo;
                this.pontoTela = pontoTela;
                this.baseLocal = baseLocal;
                this.localParaTela = localParaTela;
            }
        }

        private final Map<PontoRig, HandleRig> handlesRig =
                new EnumMap<>(PontoRig.class);

        private PontoRig pontoArrastado = null;
        private PontoRig pontoSobMouse = null;

        /*
         * Guarda o transform original do Graphics do JPanel.
         * Assim convertemos as transformações do Java2D para
         * coordenadas lógicas do mouse, inclusive com zoom.
         */
        private AffineTransform transformBaseGraphics =
                new AffineTransform();

        public CanvasPanel() {

            setToolTipText(
                    "Arraste mãos, cotovelos, joelhos e pés diretamente no Miau."
            );

            MouseAdapter mouseRig = new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {

                    if (!arrasteRigAtivo
                            || previewAnimacaoAtivo) {

                        return;
                    }

                    PontoRig encontrado =
                            encontrarPontoRig(
                                    e.getPoint(),
                                    14.0
                            );

                    if (encontrado != null) {

                        registrarInicioDeEdicao();

                        pontoArrastado = encontrado;

                        setCursor(
                                Cursor.getPredefinedCursor(
                                        Cursor.MOVE_CURSOR
                                )
                        );
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {

                    if (!arrasteRigAtivo
                            || previewAnimacaoAtivo
                            || pontoArrastado == null) {
                        return;
                    }

                    continuarGrupoHistorico();
                    arrastarPontoRig(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {

                    pontoArrastado = null;
                    finalizarGrupoHistorico();

                    atualizarCursorRig(
                            e.getPoint()
                    );
                }

                @Override
                public void mouseMoved(MouseEvent e) {

                    if (!arrasteRigAtivo
                            || previewAnimacaoAtivo) {

                        pontoSobMouse = null;
                        setCursor(Cursor.getDefaultCursor());
                        repaint();

                        return;
                    }

                    pontoSobMouse =
                            encontrarPontoRig(
                                    e.getPoint(),
                                    14.0
                            );

                    atualizarCursorRig(
                            e.getPoint()
                    );

                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {

                    if (pontoArrastado == null) {
                        pontoSobMouse = null;
                        setCursor(Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            };

            addMouseListener(mouseRig);
            addMouseMotionListener(mouseRig);
        }

        private void cancelarInteracaoRig() {

            pontoArrastado = null;
            pontoSobMouse = null;

            handlesRig.clear();

            finalizarGrupoHistorico();

            setCursor(
                    Cursor.getDefaultCursor()
            );

            repaint();
        }

        private void atualizarCursorRig(Point pontoMouse) {

            if (!arrasteRigAtivo
                    || previewAnimacaoAtivo) {

                setCursor(
                        Cursor.getDefaultCursor()
                );

                return;
            }

            if (pontoArrastado != null) {

                setCursor(
                        Cursor.getPredefinedCursor(
                                Cursor.MOVE_CURSOR
                        )
                );

                return;
            }

            PontoRig encontrado =
                    encontrarPontoRig(
                            pontoMouse,
                            14.0
                    );

            if (encontrado != null) {

                setCursor(
                        Cursor.getPredefinedCursor(
                                Cursor.HAND_CURSOR
                        )
                );

            } else {

                setCursor(
                        Cursor.getDefaultCursor()
                );
            }
        }

        private PontoRig encontrarPontoRig(
                Point2D pontoMouse,
                double raioClique
        ) {

            if (!arrasteRigAtivo
                    || previewAnimacaoAtivo) {

                return null;
            }

            PontoRig melhor = null;
            double menorDistancia = raioClique;

            for (HandleRig handle : handlesRig.values()) {

                double distancia =
                        handle.pontoTela.distance(
                                pontoMouse
                        );

                if (distancia <= menorDistancia) {

                    menorDistancia = distancia;
                    melhor = handle.tipo;
                }
            }

            return melhor;
        }

        private void arrastarPontoRig(MouseEvent e) {

            HandleRig handle =
                    handlesRig.get(
                            pontoArrastado
                    );

            if (handle == null
                    || alvoAtual == null) {

                return;
            }

            try {

                AffineTransform telaParaLocal =
                        handle.localParaTela
                                .createInverse();

                Point2D pontoLocal =
                        telaParaLocal.transform(
                                e.getPoint(),
                                null
                        );

                int novoOffsetX =
                        (int) Math.round(
                                (
                                        pontoLocal.getX()
                                        - handle.baseLocal.x
                                )
                                / alvoAtual.escala
                        );

                int novoOffsetY =
                        (int) Math.round(
                                (
                                        pontoLocal.getY()
                                        - handle.baseLocal.y
                                )
                                / alvoAtual.escala
                        );

                aplicarOffsetDoPonto(
                        pontoArrastado,
                        novoOffsetX,
                        novoOffsetY
                );

            } catch (NoninvertibleTransformException ex) {

                // Se alguma transformação ficar inválida,
                // apenas ignoramos aquele frame do arraste.
            }
        }

        private int limitarOffsetMembro(int valor) {

            return Math.max(
                    -150,
                    Math.min(
                            150,
                            valor
                    )
            );
        }

        private void aplicarOffsetDoPonto(
                PontoRig ponto,
                int offsetX,
                int offsetY
        ) {

            int xLimitado =
                    limitarOffsetMembro(
                            offsetX
                    );

            int yLimitado =
                    limitarOffsetMembro(
                            offsetY
                    );

            ControleNumerico controleX;
            ControleNumerico controleY;

            switch (ponto) {

                case COTOVELO_ESQUERDO:
                    alvoAtual.offCotEsqX = xLimitado;
                    alvoAtual.offCotEsqY = yLimitado;
                    controleX = cCotEsqX;
                    controleY = cCotEsqY;
                    break;

                case MAO_ESQUERDA:
                    alvoAtual.offMaoEsqX = xLimitado;
                    alvoAtual.offMaoEsqY = yLimitado;
                    controleX = cMaoEsqX;
                    controleY = cMaoEsqY;
                    break;

                case COTOVELO_DIREITO:
                    alvoAtual.offCotDirX = xLimitado;
                    alvoAtual.offCotDirY = yLimitado;
                    controleX = cCotDirX;
                    controleY = cCotDirY;
                    break;

                case MAO_DIREITA:
                    alvoAtual.offMaoDirX = xLimitado;
                    alvoAtual.offMaoDirY = yLimitado;
                    controleX = cMaoDirX;
                    controleY = cMaoDirY;
                    break;

                case JOELHO_ESQUERDO:
                    alvoAtual.offJoelEsqX = xLimitado;
                    alvoAtual.offJoelEsqY = yLimitado;
                    controleX = cJoelEsqX;
                    controleY = cJoelEsqY;
                    break;

                case PE_ESQUERDO:
                    alvoAtual.offPeEsqX = xLimitado;
                    alvoAtual.offPeEsqY = yLimitado;
                    controleX = cPeEsqX;
                    controleY = cPeEsqY;
                    break;

                case JOELHO_DIREITO:
                    alvoAtual.offJoelDirX = xLimitado;
                    alvoAtual.offJoelDirY = yLimitado;
                    controleX = cJoelDirX;
                    controleY = cJoelDirY;
                    break;

                case PE_DIREITO:
                    alvoAtual.offPeDirX = xLimitado;
                    alvoAtual.offPeDirY = yLimitado;
                    controleX = cPeDirX;
                    controleY = cPeDirY;
                    break;

                default:
                    return;
            }

            /*
             * O modelo já foi atualizado.
             * Agora sincronizamos sliders/spinners sem
             * disparar novamente os listeners deles.
             */
            boolean estadoAnterior =
                    atualizandoUI;

            atualizandoUI = true;

            controleX.setValue(
                    xLimitado
            );

            controleY.setValue(
                    yLimitado
            );

            atualizandoUI =
                    estadoAnterior;

            atualizarCodigoGerado();
            repaint();
        }

        private AffineTransform obterTransformCanvas(
                Graphics2D contexto
        ) {

            try {

                AffineTransform inversaBase =
                        transformBaseGraphics
                                .createInverse();

                inversaBase.concatenate(
                        contexto.getTransform()
                );

                return inversaBase;

            } catch (NoninvertibleTransformException ex) {

                return new AffineTransform();
            }
        }

        private void registrarHandle(
                PontoRig tipo,
                double atualX,
                double atualY,
                double baseX,
                double baseY,
                Graphics2D contexto
        ) {

            if (!arrasteRigAtivo
                    || previewAnimacaoAtivo) {

                return;
            }

            AffineTransform localParaTela =
                    obterTransformCanvas(
                            contexto
                    );

            Point2D pontoTelaGenerico =
                    localParaTela.transform(
                            new Point2D.Double(
                                    atualX,
                                    atualY
                            ),
                            null
                    );

            Point2D.Double pontoTela =
                    new Point2D.Double(
                            pontoTelaGenerico.getX(),
                            pontoTelaGenerico.getY()
                    );

            handlesRig.put(
                    tipo,
                    new HandleRig(
                            tipo,
                            pontoTela,
                            new Point2D.Double(
                                    baseX,
                                    baseY
                            ),
                            localParaTela
                    )
            );
        }

        private void desenharHandlesRig(
                Graphics2D g2
        ) {

            if (!arrasteRigAtivo
                    || previewAnimacaoAtivo) {

                return;
            }

            int raioNormal = 6;
            int raioAtivo = 8;

            g2.setStroke(
                    new BasicStroke(2.0f)
            );

            for (HandleRig handle : handlesRig.values()) {

                boolean ativo =
                        handle.tipo == pontoArrastado
                        || handle.tipo == pontoSobMouse;

                int raio =
                        ativo
                                ? raioAtivo
                                : raioNormal;

                int px =
                        (int) Math.round(
                                handle.pontoTela.x
                        );

                int py =
                        (int) Math.round(
                                handle.pontoTela.y
                        );

                if (ativo) {

                    g2.setColor(
                            new Color(
                                    255,
                                    120,
                                    30
                            )
                    );

                } else {

                    g2.setColor(
                            new Color(
                                    0,
                                    220,
                                    255
                            )
                    );
                }

                g2.fillOval(
                        px - raio,
                        py - raio,
                        raio * 2,
                        raio * 2
                );

                g2.setColor(Color.BLACK);

                g2.drawOval(
                        px - raio,
                        py - raio,
                        raio * 2,
                        raio * 2
                );

                if (ativo) {

                    g2.drawString(
                            handle.tipo.getRotulo(),
                            px + raio + 5,
                            py - raio - 3
                    );
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D gBase = (Graphics2D) g;
            gBase.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            transformBaseGraphics =
                    new AffineTransform(
                            gBase.getTransform()
                    );

            handlesRig.clear();

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

            /*
             * Os handles são desenhados por cima de toda a cena
             * e mantêm tamanho constante mesmo com zoom.
             */
            desenharHandlesRig(gBase);
        }

        private void desenharPersonagemEditor(Graphics2D gCamera, DadosPersonagem pd) {
            Graphics2D g2d = (Graphics2D) gCamera.create();
            double s = pd.escala;

            double posX = pd.cenaX;
            double espacamentoLinha = 25.0;
            double posY = 150 + (pd.linhaAtual * espacamentoLinha);

            // Espelhamento do personagem inteiro.
            if (!pd.viradoDireita) {
                double eixoCentral = posX + 35 * s;
                g2d.translate(eixoCentral, 0);
                g2d.scale(-1, 1);
                g2d.translate(-eixoCentral, 0);
            }

            // Rotação global antiga.
            double pivotX = posX + 35 * s;
            double pivotY = posY + 120 * s;
            g2d.rotate(
                    Math.toRadians(pd.rotGlobal),
                    pivotX,
                    pivotY
            );

            // =================================================
            // ROOT DO RIG: CINTURA
            // =================================================
            double eixoCinturaBaseX =
                    posX
                    + pd.instancia.getEixoCinturaLocalX()
                    * s;

            double eixoCinturaBaseY =
                    posY
                    + pd.instancia.getEixoCinturaLocalY()
                    * s;

            g2d.translate(
                    pd.offCinturaX * s,
                    pd.offCinturaY * s
            );

            g2d.rotate(
                    Math.toRadians(pd.rotCintura),
                    eixoCinturaBaseX,
                    eixoCinturaBaseY
            );

            boolean isPreto = pd.nome.contains("Preto");

            // =================================================
            // GEOMETRIA BASE
            // =================================================
            int corpoY = (int) (posY + 70 * s);

            int topoEsqX;
            int topoDirX;
            int fundoEsqX;
            int fundoDirX;
            int alturaCorpo;
            int faixaX;
            int faixaY;
            int faixaLargura;
            int faixaAltura;

            if (isPreto) {
                topoEsqX = (int) (posX - 4 * s);
                topoDirX = (int) (posX + 78 * s);
                fundoEsqX = (int) (posX + 10 * s);
                fundoDirX = (int) (posX + 61 * s);
                alturaCorpo = (int) (100 * s);

                faixaX = (int) (posX - 6 * s);
                faixaY = (int) (corpoY + 80 * s);
                faixaLargura = (int) (90 * s);
                faixaAltura = (int) (12 * s);
            } else {
                topoEsqX = (int) (posX - 10 * s);
                topoDirX = (int) (posX + 82 * s);
                fundoEsqX = (int) (posX + 8 * s);
                fundoDirX = (int) (posX + 64 * s);
                alturaCorpo = (int) (105 * s);

                faixaX = (int) (posX - 8 * s);
                faixaY = (int) (corpoY + 82 * s);
                faixaLargura = (int) (96 * s);
                faixaAltura = (int) (12 * s);
            }

            int corpoFimY = corpoY + alturaCorpo;

            /*
             * A faixa funciona como uma pequena zona de sobreposição.
             * Isso esconde a emenda entre tronco superior e pelve
             * quando o peito começa a dobrar.
             */
            int divisaoY = faixaY + faixaAltura / 2;

            double tDivisao =
                    (double) (divisaoY - corpoY)
                    / Math.max(1, alturaCorpo);

            int divisaoEsqX =
                    (int) Math.round(
                            topoEsqX
                            + (fundoEsqX - topoEsqX)
                            * tDivisao
                    );

            int divisaoDirX =
                    (int) Math.round(
                            topoDirX
                            + (fundoDirX - topoDirX)
                            * tDivisao
                    );

            double tFaixa =
                    (double) (faixaY - corpoY)
                    / Math.max(1, alturaCorpo);

            int faixaEsqCorpoX =
                    (int) Math.round(
                            topoEsqX
                            + (fundoEsqX - topoEsqX)
                            * tFaixa
                    );

            int faixaDirCorpoX =
                    (int) Math.round(
                            topoDirX
                            + (fundoDirX - topoDirX)
                            * tFaixa
                    );

            // =================================================
            // BASES DE JUNTAS
            // =================================================
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

            if (pd == alvoAtual) {

                registrarHandle(
                        PontoRig.JOELHO_ESQUERDO,
                        joelEsqX,
                        joelEsqY,
                        baseJoelEsqX,
                        baseJoelEsqY,
                        g2d
                );

                registrarHandle(
                        PontoRig.PE_ESQUERDO,
                        peEsqX,
                        peEsqY,
                        basePeEsqX,
                        basePeEsqY,
                        g2d
                );

                registrarHandle(
                        PontoRig.JOELHO_DIREITO,
                        joelDirX,
                        joelDirY,
                        baseJoelDirX,
                        baseJoelDirY,
                        g2d
                );

                registrarHandle(
                        PontoRig.PE_DIREITO,
                        peDirX,
                        peDirY,
                        basePeDirX,
                        basePeDirY,
                        g2d
                );
            }

            // =================================================
            // PARTE INFERIOR: PELVE + PERNAS
            // =================================================
            Polygon pelve = new Polygon();
            pelve.addPoint(faixaEsqCorpoX, faixaY);
            pelve.addPoint(faixaDirCorpoX, faixaY);
            pelve.addPoint(fundoDirX, corpoFimY);
            pelve.addPoint(fundoEsqX, corpoFimY);

            g2d.setColor(pd.corKimono);
            g2d.fillPolygon(pelve);

            g2d.setStroke(
                    new BasicStroke(
                            (float) (18 * s),
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );

            g2d.drawLine(quadEsqX, quadEsqY, joelEsqX, joelEsqY);
            g2d.drawLine(joelEsqX, joelEsqY, peEsqX, peEsqY);
            g2d.drawLine(quadDirX, quadDirY, joelDirX, joelDirY);
            g2d.drawLine(joelDirX, joelDirY, peDirX, peDirY);

            g2d.setColor(pd.corPelo);
            int lPe = (int)(25*s);
            int aPe = (int)(17*s);

            g2d.fillOval(
                    peEsqX - lPe/2,
                    peEsqY - aPe/2,
                    lPe,
                    aPe
            );

            g2d.fillOval(
                    peDirX - lPe/2,
                    peDirY - aPe/2,
                    lPe,
                    aPe
            );

            // A faixa fica na parte inferior e cobre a emenda.
            g2d.setColor(new Color(55, 55, 220));
            g2d.fillRect(
                    faixaX,
                    faixaY,
                    faixaLargura,
                    faixaAltura
            );

            // =================================================
            // TRONCO SUPERIOR ARTICULADO
            // =================================================
            Graphics2D gPeito =
                    (Graphics2D) g2d.create();

            /*
             * O tronco superior não possui outro pivô separado.
             * Ele dobra em torno da própria cintura.
             */
            double eixoTroncoX =
                    eixoCinturaBaseX;

            double eixoTroncoY =
                    eixoCinturaBaseY;

            gPeito.translate(
                    pd.offPeitoX * s,
                    pd.offPeitoY * s
            );

            gPeito.rotate(
                    Math.toRadians(pd.rotPeito),
                    eixoTroncoX,
                    eixoTroncoY
            );

            if (pd == alvoAtual) {

                registrarHandle(
                        PontoRig.COTOVELO_ESQUERDO,
                        cotEsqX,
                        cotEsqY,
                        baseCotEsqX,
                        baseCotEsqY,
                        gPeito
                );

                registrarHandle(
                        PontoRig.MAO_ESQUERDA,
                        maoEsqX,
                        maoEsqY,
                        baseMaoEsqX,
                        baseMaoEsqY,
                        gPeito
                );

                registrarHandle(
                        PontoRig.COTOVELO_DIREITO,
                        cotDirX,
                        cotDirY,
                        baseCotDirX,
                        baseCotDirY,
                        gPeito
                );

                registrarHandle(
                        PontoRig.MAO_DIREITA,
                        maoDirX,
                        maoDirY,
                        baseMaoDirX,
                        baseMaoDirY,
                        gPeito
                );
            }

            // Tronco superior.
            Polygon troncoSuperior = new Polygon();
            troncoSuperior.addPoint(topoEsqX, corpoY);
            troncoSuperior.addPoint(topoDirX, corpoY);
            troncoSuperior.addPoint(divisaoDirX, divisaoY);
            troncoSuperior.addPoint(divisaoEsqX, divisaoY);

            gPeito.setColor(pd.corKimono);
            gPeito.fillPolygon(troncoSuperior);

            // Braços acompanham o peito.
            gPeito.setStroke(
                    new BasicStroke(
                            (float) (18 * s),
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );

            gPeito.drawLine(ombroEsqX, ombroEsqY, cotEsqX, cotEsqY);
            gPeito.drawLine(cotEsqX, cotEsqY, maoEsqX, maoEsqY);
            gPeito.drawLine(ombroDirX, ombroDirY, cotDirX, cotDirY);
            gPeito.drawLine(cotDirX, cotDirY, maoDirX, maoDirY);

            // Mãos acompanham o peito.
            gPeito.setColor(pd.corPelo);

            int lMao = (int)(20*s);
            int aMao = (int)(18*s);

            gPeito.fillOval(
                    maoEsqX - lMao/2,
                    maoEsqY - aMao/2,
                    lMao,
                    aMao
            );

            gPeito.fillOval(
                    maoDirX - lMao/2,
                    maoDirY - aMao/2,
                    lMao,
                    aMao
            );

            // Cabeça também herda a rotação do peito.
            Graphics2D gCabeca =
                    (Graphics2D) gPeito.create();

            /*
             * A cabeça não pode herdar o stroke grosso dos braços.
             * Sem isso, as linhas da boca ficam enormes.
             */
            gCabeca.setStroke(
                    new BasicStroke(
                            (float) Math.max(1.0, 1.5 * s),
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );

            gCabeca.translate(posX, posY);

            gCabeca.translate(
                    pd.offCabX * s,
                    pd.offCabY * s
            );

            double pivotCabX = 35 * s;
            double pivotCabY = 53 * s;

            gCabeca.rotate(
                    Math.toRadians(pd.rotCabeca),
                    pivotCabX,
                    pivotCabY
            );

            try {
                Method metodoCabeca = null;
                Class<?> clazz = pd.instancia.getClass();

                while (clazz != null
                        && metodoCabeca == null) {

                    try {
                        metodoCabeca =
                                clazz.getDeclaredMethod(
                                        "desenharCabeca",
                                        Graphics2D.class
                                );
                    } catch (Exception ex) {
                        clazz = clazz.getSuperclass();
                    }
                }

                if (metodoCabeca != null) {
                    metodoCabeca.setAccessible(true);
                    metodoCabeca.invoke(
                            pd.instancia,
                            gCabeca
                    );
                }

            } catch (Exception ex) {
                // Editor: falha de reflection não deve derrubar a janela.
            }

            gCabeca.dispose();

            // =================================================
            // MARCADORES DO RIG
            // =================================================
            if (pd == alvoAtual
                    && arrasteRigAtivo
                    && !previewAnimacaoAtivo) {

                int raio =
                        Math.max(
                                5,
                                (int) Math.round(7 * s)
                        );

                // Cintura/root.
                g2d.setColor(
                        new Color(255, 0, 180)
                );

                g2d.setStroke(
                        new BasicStroke(2.0f)
                );

                int cinturaX =
                        (int) Math.round(
                                eixoCinturaBaseX
                        );

                int cinturaY =
                        (int) Math.round(
                                eixoCinturaBaseY
                        );

                g2d.drawOval(
                        cinturaX - raio,
                        cinturaY - raio,
                        raio * 2,
                        raio * 2
                );

                g2d.drawLine(
                        cinturaX - raio * 2,
                        cinturaY,
                        cinturaX + raio * 2,
                        cinturaY
                );

                g2d.drawLine(
                        cinturaX,
                        cinturaY - raio * 2,
                        cinturaX,
                        cinturaY + raio * 2
                );

                g2d.drawString(
                        "CINTURA",
                        cinturaX + raio + 4,
                        cinturaY - raio - 2
                );

                // O tronco usa o mesmo pivô da cintura.
                // Por isso desenhamos somente o marcador da CINTURA.
            }

            gPeito.dispose();

            // Setinha vermelha indicando o alvo atual.
            if (pd == alvoAtual
                    && arrasteRigAtivo
                    && !previewAnimacaoAtivo) {
                g2d.setColor(Color.RED);
                g2d.fillPolygon(
                        new int[]{
                            (int)(posX + 25*s),
                            (int)(posX + 45*s),
                            (int)(posX + 35*s)
                        },
                        new int[]{
                            (int)(posY - 10*s),
                            (int)(posY - 10*s),
                            (int)(posY + 5*s)
                        },
                        3
                );
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