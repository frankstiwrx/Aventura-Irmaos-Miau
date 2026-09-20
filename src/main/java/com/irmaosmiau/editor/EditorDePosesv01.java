package com.irmaosmiau.editor;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class EditorDePosesv01 extends JFrame {

    private JComboBox<String> comboPersonagem;
    private JTextArea txtCodigoGerado;
    private CanvasPanel painelDesenho;

    // --- VARIÁVEIS DO RIGGING ---
    private double customEscala = 1.5;
    private int rotGlobal = 0;
    
    // Cabeça
    private int offCabecaX = 0, offCabecaY = 0, rotCabeca = 0;
    // Braços
    private int offCotEsqX = 0, offCotEsqY = 0, offMaoEsqX = 0, offMaoEsqY = 0;
    private int offCotDirX = 0, offCotDirY = 0, offMaoDirX = 0, offMaoDirY = 0;
    // Pernas
    private int offJoelEsqX = 0, offJoelEsqY = 0, offPeEsqX = 0, offPeEsqY = 0;
    private int offJoelDirX = 0, offJoelDirY = 0, offPeDirX = 0, offPeDirY = 0;

    private String personagemAtual = "Branco";

    public EditorDePosesv01() {
        setTitle("MiauStudio - Mapeamento de Juntas & Rigging Completo");
        setSize(1300, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Painel de Desenho à Esquerda
        painelDesenho = new CanvasPanel();
        painelDesenho.setBackground(new Color(135, 206, 235));
        add(painelDesenho, BorderLayout.CENTER);

        // Painel Direito (Controles + Código)
        JPanel painelDireito = new JPanel(new BorderLayout());
        painelDireito.setPreferredSize(new Dimension(450, 850));

        JPanel painelControles = new JPanel(new GridLayout(0, 1, 0, 0));
        painelControles.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Seletor
        comboPersonagem = new JComboBox<>(new String[]{"Irmão Miau Branco", "Irmão Miau Preto"});
        comboPersonagem.addActionListener(e -> {
            personagemAtual = comboPersonagem.getSelectedIndex() == 0 ? "Branco" : "Preto";
            painelDesenho.repaint();
        });
        painelControles.add(comboPersonagem);

        // Sliders Globais e Cabeça
        addSlider(painelControles, "Escala (div 10): ", 5, 30, 15, v -> customEscala = v / 10.0);
        addSlider(painelControles, "ROTAÇÃO CONJUNTO COMPLETO: ", -180, 180, 0, v -> rotGlobal = v);
        
        painelControles.add(new JLabel("--- CABEÇA E ROSTO ---"));
        addSlider(painelControles, "Cabeça Offset X: ", -100, 100, 0, v -> offCabecaX = v);
        addSlider(painelControles, "Cabeça Offset Y: ", -100, 100, 0, v -> offCabecaY = v);
        addSlider(painelControles, "Rotação da Cabeça: ", -180, 180, 0, v -> rotCabeca = v);

        // Sliders Braço Esquerdo
        painelControles.add(new JLabel("--- BRAÇO ESQUERDO ---"));
        addSlider(painelControles, "Cotovelo Esq X: ", -100, 100, 0, v -> offCotEsqX = v);
        addSlider(painelControles, "Cotovelo Esq Y: ", -100, 100, 0, v -> offCotEsqY = v);
        addSlider(painelControles, "Mão Esq X: ", -100, 100, 0, v -> offMaoEsqX = v);
        addSlider(painelControles, "Mão Esq Y: ", -100, 100, 0, v -> offMaoEsqY = v);

        // Sliders Braço Direito
        painelControles.add(new JLabel("--- BRAÇO DIREITO ---"));
        addSlider(painelControles, "Cotovelo Dir X: ", -100, 100, 0, v -> offCotDirX = v);
        addSlider(painelControles, "Cotovelo Dir Y: ", -100, 100, 0, v -> offCotDirY = v);
        addSlider(painelControles, "Mão Dir X: ", -100, 100, 0, v -> offMaoDirX = v);
        addSlider(painelControles, "Mão Dir Y: ", -100, 100, 0, v -> offMaoDirY = v);

        // Sliders Perna Esquerda
        painelControles.add(new JLabel("--- PERNA ESQUERDA ---"));
        addSlider(painelControles, "Joelho Esq X: ", -100, 100, 0, v -> offJoelEsqX = v);
        addSlider(painelControles, "Joelho Esq Y: ", -100, 100, 0, v -> offJoelEsqY = v);
        addSlider(painelControles, "Pé Esq X: ", -100, 100, 0, v -> offPeEsqX = v);
        addSlider(painelControles, "Pé Esq Y: ", -100, 100, 0, v -> offPeEsqY = v);

        // Sliders Perna Direita
        painelControles.add(new JLabel("--- PERNA DIREITA ---"));
        addSlider(painelControles, "Joelho Dir X: ", -100, 100, 0, v -> offJoelDirX = v);
        addSlider(painelControles, "Joelho Dir Y: ", -100, 100, 0, v -> offJoelDirY = v);
        addSlider(painelControles, "Pé Dir X: ", -100, 100, 0, v -> offPeDirX = v);
        addSlider(painelControles, "Pé Dir Y: ", -100, 100, 0, v -> offPeDirY = v);

        JScrollPane scrollControles = new JScrollPane(painelControles);
        scrollControles.getVerticalScrollBar().setUnitIncrement(16);
        painelDireito.add(scrollControles, BorderLayout.CENTER);

        // Código Gerado
        txtCodigoGerado = new JTextArea(10, 20);
        txtCodigoGerado.setEditable(false);
        txtCodigoGerado.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JPanel painelCodigo = new JPanel(new BorderLayout());
        painelCodigo.setBorder(BorderFactory.createTitledBorder("Código da Pose (Copie e Cole na Classe)"));
        painelCodigo.add(new JScrollPane(txtCodigoGerado), BorderLayout.CENTER);
        
        painelDireito.add(painelCodigo, BorderLayout.SOUTH);
        add(painelDireito, BorderLayout.EAST);

        atualizarCodigoGerado();
    }

    private void addSlider(JPanel p, String rotulo, int min, int max, int val, Consumer<Integer> acao) {
        JPanel linha = new JPanel(new BorderLayout());
        JLabel l = new JLabel(rotulo + val);
        l.setPreferredSize(new Dimension(160, 20));
        JSlider s = new JSlider(min, max, val);
        s.addChangeListener(e -> {
            l.setText(rotulo + s.getValue());
            acao.accept(s.getValue());
            atualizarCodigoGerado();
            painelDesenho.repaint();
        });
        linha.add(l, BorderLayout.WEST);
        linha.add(s, BorderLayout.CENTER);
        p.add(linha);
    }

    private void atualizarCodigoGerado() {
        String codigo = String.format(
            "// --- ROTAÇÃO DO CONJUNTO COMPLETO ---\n" +
            "Graphics2D copia = (Graphics2D) g2.create();\n" +
            "copia.rotate(Math.toRadians(%d), x + 35 * escala, y + 120 * escala);\n\n" +
            "// --- OFFSETS DAS JUNTAS (Adicionar nas variáveis locais) ---\n" +
            "int offCabecaX = (int)(%d * escala);\n" +
            "int offCabecaY = (int)(%d * escala);\n" +
            "// Rotação Cabeça: %d graus\n\n" +
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
            rotGlobal, 
            offCabecaX, offCabecaY, rotCabeca,
            offCotEsqX, offCotEsqY, offMaoEsqX, offMaoEsqY,
            offCotDirX, offCotDirY, offMaoDirX, offMaoDirY,
            offJoelEsqX, offJoelEsqY, offPeEsqX, offPeEsqY,
            offJoelDirX, offJoelDirY, offPeDirX, offPeDirY
        );
        txtCodigoGerado.setText(codigo);
        txtCodigoGerado.setCaretPosition(0);
    }

    // Renderizador customizado que aplica a física exata das classes originais
    private class CanvasPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(new Color(50, 160, 60));
            g2d.fillRect(0, getHeight() - 100, getWidth(), 100);

            double s = customEscala;
            int x = getWidth() / 2 - 40;
            int y = getHeight() - 320;

            boolean isBranco = personagemAtual.equals("Branco");
            Color corKimono = isBranco ? Color.WHITE : Color.BLACK;
            Color corPelo = isBranco ? Color.BLACK : Color.WHITE;

            // ROTAÇÃO DO CONJUNTO COMPLETO
            double pivotX = x + 35 * s;
            double pivotY = y + 120 * s;
            g2d.translate(pivotX, pivotY);
            g2d.rotate(Math.toRadians(rotGlobal));
            g2d.translate(-pivotX, -pivotY);

            // --- DESENHO DO ROSTO E CABEÇA ---
            Graphics2D gCabeca = (Graphics2D) g2d.create();
            int cabX = x + (int)(6 * s) + (int)(offCabecaX * s);
            int cabY = y + (int)(24 * s) + (int)(offCabecaY * s);
            
            gCabeca.translate(cabX + 29*s, cabY + 29*s); // Pivot da cabeça
            gCabeca.rotate(Math.toRadians(rotCabeca));
            gCabeca.translate(-(cabX + 29*s), -(cabY + 29*s));
            
            gCabeca.setColor(corPelo);
            gCabeca.fillOval(cabX, cabY, (int)(58 * s), (int)(58 * s)); // Cabeça
            
            // Orelhas
            gCabeca.fillPolygon(new int[]{cabX+(int)(5*s), cabX+(int)(15*s), cabX+(int)(25*s)}, 
                                new int[]{cabY+(int)(15*s), cabY-(int)(5*s), cabY+(int)(5*s)}, 3);
            gCabeca.fillPolygon(new int[]{cabX+(int)(33*s), cabX+(int)(43*s), cabX+(int)(53*s)}, 
                                new int[]{cabY+(int)(5*s), cabY-(int)(5*s), cabY+(int)(15*s)}, 3);
            
            // Olhos
            gCabeca.setColor(corKimono);
            gCabeca.fillOval(cabX+(int)(16*s), cabY+(int)(22*s), (int)(7*s), (int)(11*s));
            gCabeca.fillOval(cabX+(int)(35*s), cabY+(int)(22*s), (int)(7*s), (int)(11*s));
            gCabeca.dispose();

            // --- DESENHO DO CORPO ---
            int corpoY = y + (int) (70 * s);
            Polygon corpo = new Polygon();
            if (isBranco) {
                corpo.addPoint(x - (int)(10*s), corpoY);
                corpo.addPoint(x + (int)(82*s), corpoY);
                corpo.addPoint(x + (int)(64*s), corpoY + (int)(105*s));
                corpo.addPoint(x + (int)(8*s), corpoY + (int)(105*s));
            } else {
                corpo.addPoint(x - (int)(4*s), corpoY);
                corpo.addPoint(x + (int)(78*s), corpoY);
                corpo.addPoint(x + (int)(61*s), corpoY + (int)(100*s));
                corpo.addPoint(x + (int)(10*s), corpoY + (int)(100*s));
            }
            g2d.setColor(corKimono);
            g2d.fillPolygon(corpo);

            // Faixa
            g2d.setColor(new Color(55, 55, 220));
            if (isBranco) g2d.fillRect(x - (int)(8*s), corpoY + (int)(82*s), (int)(96*s), (int)(12*s));
            else g2d.fillRect(x - (int)(6*s), corpoY + (int)(80*s), (int)(90*s), (int)(12*s));

            // --- BASE DE COORDENADAS DOS MEMBROS ---
            int ombroEsqX, ombroEsqY, cotEsqX, cotEsqY, maoEsqX, maoEsqY;
            int ombroDirX, ombroDirY, cotDirX, cotDirY, maoDirX, maoDirY;
            int quadEsqX, quadEsqY, joelEsqX, joelEsqY, peEsqX, peEsqY;
            int quadDirX, quadDirY, joelDirX, joelDirY, peDirX, peDirY;

            if (isBranco) {
                ombroEsqX = x + (int)(3*s); ombroEsqY = corpoY + (int)(22*s);
                cotEsqX = x - (int)(14*s); cotEsqY = corpoY + (int)(42*s);
                maoEsqX = x + (int)(3*s); maoEsqY = corpoY + (int)(58*s);
                
                ombroDirX = x + (int)(73*s); ombroDirY = corpoY + (int)(20*s);
                cotDirX = x + (int)(95*s); cotDirY = corpoY + (int)(36*s);
                maoDirX = x + (int)(111*s); maoDirY = corpoY + (int)(29*s);

                quadEsqX = x + (int)(22*s); quadEsqY = corpoY + (int)(100*s);
                joelEsqX = x + (int)(10*s); joelEsqY = corpoY + (int)(136*s);
                peEsqX = x; peEsqY = corpoY + (int)(174*s);

                quadDirX = x + (int)(52*s); quadDirY = corpoY + (int)(100*s);
                joelDirX = x + (int)(66*s); joelDirY = corpoY + (int)(137*s);
                peDirX = x + (int)(82*s); peDirY = corpoY + (int)(171*s);
            } else {
                ombroEsqX = x + (int)(5*s); ombroEsqY = corpoY + (int)(22*s);
                cotEsqX = x - (int)(8*s); cotEsqY = corpoY + (int)(52*s);
                maoEsqX = x + (int)(35*s); maoEsqY = corpoY + (int)(57*s);
                
                ombroDirX = x + (int)(70*s); ombroDirY = corpoY + (int)(23*s);
                cotDirX = x + (int)(88*s); cotDirY = corpoY + (int)(48*s);
                maoDirX = x + (int)(108*s); maoDirY = corpoY + (int)(62*s);

                quadEsqX = x + (int)(24*s); quadEsqY = corpoY + (int)(97*s);
                joelEsqX = x + (int)(15*s); joelEsqY = corpoY + (int)(132*s);
                peEsqX = x + (int)(4*s); peEsqY = corpoY + (int)(171*s);

                quadDirX = x + (int)(50*s); quadDirY = corpoY + (int)(97*s);
                joelDirX = x + (int)(59*s); joelDirY = corpoY + (int)(132*s);
                peDirX = x + (int)(72*s); peDirY = corpoY + (int)(171*s);
            }

            // Aplicando OFFSETS INTERATIVOS das juntas
            cotEsqX += offCotEsqX * s; cotEsqY += offCotEsqY * s;
            maoEsqX += offMaoEsqX * s; maoEsqY += offMaoEsqY * s;
            
            cotDirX += offCotDirX * s; cotDirY += offCotDirY * s;
            maoDirX += offMaoDirX * s; maoDirY += offMaoDirY * s;

            joelEsqX += offJoelEsqX * s; joelEsqY += offJoelEsqY * s;
            peEsqX += offPeEsqX * s; peEsqY += offPeEsqY * s;

            joelDirX += offJoelDirX * s; joelDirY += offJoelDirY * s;
            peDirX += offPeDirX * s; peDirY += offPeDirY * s;

            // --- RENDERIZANDO MEMBROS ---
            g2d.setStroke(new BasicStroke((float) (18 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(corKimono);

            // Braços
            g2d.drawLine(ombroEsqX, ombroEsqY, cotEsqX, cotEsqY);
            g2d.drawLine(cotEsqX, cotEsqY, maoEsqX, maoEsqY);
            g2d.drawLine(ombroDirX, ombroDirY, cotDirX, cotDirY);
            g2d.drawLine(cotDirX, cotDirY, maoDirX, maoDirY);

            // Pernas
            g2d.drawLine(quadEsqX, quadEsqY, joelEsqX, joelEsqY);
            g2d.drawLine(joelEsqX, joelEsqY, peEsqX, peEsqY);
            g2d.drawLine(quadDirX, quadDirY, joelDirX, joelDirY);
            g2d.drawLine(joelDirX, joelDirY, peDirX, peDirY);

            // Patas/Mãos
            g2d.setColor(corPelo);
            int largMao = (int)(20*s), altMao = (int)(18*s);
            int largPe = (int)(25*s), altPe = (int)(17*s);
            
            g2d.fillOval(maoEsqX - largMao/2, maoEsqY - altMao/2, largMao, altMao);
            g2d.fillOval(maoDirX - largMao/2, maoDirY - altMao/2, largMao, altMao);
            g2d.fillOval(peEsqX - largPe/2, peEsqY - altPe/2, largPe, altPe);
            g2d.fillOval(peDirX - largPe/2, peDirY - altPe/2, largPe, altPe);

            g2d.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EditorDePosesv01().setVisible(true));
    }
}