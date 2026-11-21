package com.irmaosmiau.ui;

import com.irmaosmiau.cenario.Cenario;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.persistencia.ConfiguracoesJogo;
import com.irmaosmiau.persistencia.PersistenciaException;
import com.irmaosmiau.preferencias.PreferenciasJogo;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Tela CONFIGURACOES (Entrega 4C).
 *
 * - Mudancas de audio valem NA HORA (previa) via PreferenciasJogo.aplicar.
 * - So sao gravadas ao clicar em SALVAR.
 * - VOLTAR com alteracoes pendentes pergunta: salvar / descartar / cancelar.
 * - Entrega 5C: botao CONTROLES abre a tela de teclas (mesma pergunta antes de sair).
 */
public final class ConfiguracoesPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient PreferenciasJogo preferencias;
    private final transient RegistroCenarios cenarios;
    private final transient Dialogos dialogos;
    private final transient Runnable aoVoltar;
    private final transient Runnable aoAbrirControles;   // null = sem botao

    private final JCheckBox musica = new JCheckBox("Ligada");
    private final JSlider volumeMusica = new JSlider(0, 100);
    private final JLabel valorMusica = EstiloTelas.texto("", EstiloTelas.NORMAL, EstiloTelas.TEXTO);
    private final JCheckBox efeitos = new JCheckBox("Ligados");
    private final JSlider volumeEfeitos = new JSlider(0, 100);
    private final JLabel valorEfeitos = EstiloTelas.texto("", EstiloTelas.NORMAL, EstiloTelas.TEXTO);
    private final JComboBox<ItemCenario> cenario = new JComboBox<>();
    private final JLabel status = EstiloTelas.texto(" ", EstiloTelas.NORMAL, EstiloTelas.TEXTO_SUAVE);

    private final JButton restaurar = EstiloTelas.botao("RESTAURAR PADRÕES");
    private final JButton salvar = EstiloTelas.botao("SALVAR");
    private final JButton voltar = NavegadorMenus.marcarVoltar(EstiloTelas.botao("VOLTAR"));
    private final JButton controles = EstiloTelas.botao("CONTROLES");

    private boolean atualizandoControles;

    /** Item do combo: mostra o nome, guarda o id. */
    static final class ItemCenario {
        final String id;
        final String nome;

        ItemCenario(String id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    public ConfiguracoesPanel(PreferenciasJogo preferencias, RegistroCenarios cenarios,
            Dialogos dialogos, Runnable aoVoltar) {
        this(preferencias, cenarios, dialogos, aoVoltar, null);
    }

    /** Entrega 5C: com o botao CONTROLES (aoAbrirControles nulo = sem botao). */
    public ConfiguracoesPanel(PreferenciasJogo preferencias, RegistroCenarios cenarios,
            Dialogos dialogos, Runnable aoVoltar, Runnable aoAbrirControles) {
        this.aoAbrirControles = aoAbrirControles;
        this.preferencias = preferencias;
        this.cenarios = cenarios;
        this.dialogos = dialogos;
        this.aoVoltar = aoVoltar;

        setPreferredSize(new Dimension(EstiloTelas.LARGURA, EstiloTelas.ALTURA));
        setLayout(new GridBagLayout());
        montar();

        for (String id : cenarios.ids()) {
            Cenario c = cenarios.exigir(id);
            cenario.addItem(new ItemCenario(c.getId(), c.getNome()));
        }

        mostrar(preferencias.getAtual());
        String idValido = preferencias.cenarioValido(cenarios);
        selecionarCenario(idValido);
        if (!idValido.equals(preferencias.getAtual().getCenarioPreferido())) {
            // Cenario salvo sumiu: a tela ja mostra o padrao; so grava se o jogador salvar.
            preferencias.aplicar(lerControles());
        }
        if (!preferencias.getAvisos().isEmpty()) {
            mostrarStatus(preferencias.getAvisos().get(preferencias.getAvisos().size() - 1), true);
        }

        musica.addActionListener(e -> aoMudar());
        efeitos.addActionListener(e -> aoMudar());
        volumeMusica.addChangeListener(e -> aoMudar());
        volumeEfeitos.addChangeListener(e -> aoMudar());
        cenario.addActionListener(e -> aoMudar());
        restaurar.addActionListener(e -> restaurarPadroes());
        salvar.addActionListener(e -> salvar());
        voltar.addActionListener(e -> voltar());
        controles.addActionListener(e -> abrirControles());
    }

    /**
     * Polimento dos menus: um cartao compacto (sobre o mesmo fundo escuro com a faixa vermelha), agrupado em
     * AUDIO e CONTROLES, com UMA opcao por linha: no controle, cima/baixo anda de linha em linha (a ordem do
     * NavegadorMenus) e os lados ajustam o volume; A/ENTER liga/desliga e confirma; B/ESC volta.
     */
    private void montar() {
        JPanel cartao = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g0) {
                java.awt.Graphics2D g = ArteMenus.suave(g0);
                g.setColor(new java.awt.Color(10, 16, 24, 175));
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.setColor(new java.awt.Color(255, 255, 255, 40));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.dispose();
            }
        };
        cartao.setOpaque(false);
        cartao.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 22, 10, 22));
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(3, 6, 3, 6);

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 3;
        cartao.add(EstiloTelas.titulo("CONFIGURAÇÕES"), g);

        int linha = 1;
        linha = secao(cartao, g, linha, "ÁUDIO");
        linha = linhaLigado(cartao, g, linha, "Música", musica);
        linha = linhaVolume(cartao, g, linha, "Volume da música", volumeMusica, valorMusica);
        linha = linhaLigado(cartao, g, linha, "Efeitos sonoros", efeitos);
        linha = linhaVolume(cartao, g, linha, "Volume dos efeitos", volumeEfeitos, valorEfeitos);

        // 5Q.4: "Cenario preferido" saiu da tela (o mapa e escolhido na selecao de cada luta). O
        // combo continua so na memoria: o valor ja salvo e lido e regravado como esta (arquivos
        // antigos continuam validos e a selecao segue abrindo nesse mapa).

        // Entrega 5C: teclas configuraveis dos dois jogadores (e o gamepad, na mesma tela).
        if (aoAbrirControles != null) {
            linha = secao(cartao, g, linha, "CONTROLES");
            g.gridy = linha++;
            g.gridx = 0;
            g.gridwidth = 1;
            cartao.add(EstiloTelas.rotulo("Teclado e gamepad"), g);
            g.gridx = 1;
            g.gridwidth = 2;
            cartao.add(controles, g);
        }

        g.gridy = linha++;
        g.gridx = 0;
        g.gridwidth = 3;
        g.insets = new Insets(6, 6, 0, 6);
        status.setPreferredSize(new Dimension(420, 18));
        status.setVerticalAlignment(JLabel.TOP);
        cartao.add(status, g);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        botoes.setOpaque(false);
        botoes.add(restaurar);
        botoes.add(salvar);
        botoes.add(voltar);
        g.gridy = linha++;
        g.anchor = GridBagConstraints.CENTER;
        g.insets = new Insets(4, 6, 4, 6);
        cartao.add(botoes, g);

        g.gridy = linha;
        cartao.add(EstiloTelas.texto("↑↓ escolher · ←→ ajustar · ENTER / A confirmar · ESC / B voltar"
                + " · o som muda na hora; SALVAR mantém", EstiloTelas.PEQUENA, EstiloTelas.TEXTO_SUAVE), g);

        add(cartao);
    }

    /** Cabecalho de grupo (amarelo, como nos controles). */
    private static int secao(JPanel cartao, GridBagConstraints g, int linha, String nome) {
        g.gridy = linha;
        g.gridx = 0;
        g.gridwidth = 3;
        g.insets = new Insets(8, 6, 1, 6);
        cartao.add(EstiloTelas.texto(nome, EstiloTelas.ROTULO, EstiloTelas.SUBTITULO), g);
        g.insets = new Insets(3, 6, 3, 6);
        g.gridwidth = 1;
        return linha + 1;
    }

    /** "Musica  [x] Ligada": a caixa mostra a borda de foco do navegador. */
    private static int linhaLigado(JPanel cartao, GridBagConstraints g, int linha, String nome, JCheckBox ligado) {
        g.gridy = linha;
        g.gridx = 0;
        cartao.add(EstiloTelas.rotulo(nome), g);
        g.gridx = 1;
        g.gridwidth = 2;
        ligado.setFont(EstiloTelas.NORMAL);
        ligado.setForeground(EstiloTelas.TEXTO);
        ligado.setOpaque(false);
        ligado.setBorderPainted(true);                 // o destaque do foco aparece tambem na caixa
        ligado.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 8));
        cartao.add(ligado, g);
        g.gridwidth = 1;
        return linha + 1;
    }

    /** "Volume da musica  [-----o----]  60%". */
    private static int linhaVolume(JPanel cartao, GridBagConstraints g, int linha, String nome, JSlider volume,
            JLabel valor) {
        g.gridy = linha;
        g.gridx = 0;
        cartao.add(EstiloTelas.rotulo(nome), g);
        g.gridx = 1;
        volume.setOpaque(false);
        volume.setPreferredSize(new Dimension(230, 26));
        cartao.add(volume, g);
        g.gridx = 2;
        valor.setPreferredSize(new Dimension(44, 20));
        cartao.add(valor, g);
        return linha + 1;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        javax.swing.SwingUtilities.invokeLater(musica::requestFocusInWindow);   // foco inicial: a primeira opcao
    }

    @Override
    protected void paintComponent(Graphics g) {
        EstiloTelas.pintarFundo(g, getWidth(), getHeight());
    }

    // =====================================================
    // CONTROLES <-> CONFIGURACOES
    // =====================================================
    private void mostrar(ConfiguracoesJogo c) {
        atualizandoControles = true;
        try {
            musica.setSelected(c.isMusicaAtiva());
            efeitos.setSelected(c.isEfeitosAtivos());
            volumeMusica.setValue((int) Math.round(c.getVolumeMusica() * 100));
            volumeEfeitos.setValue((int) Math.round(c.getVolumeEfeitos() * 100));
            selecionarCenario(c.getCenarioPreferido());
            atualizarRotulos();
        } finally {
            atualizandoControles = false;
        }
    }

    private void selecionarCenario(String id) {
        for (int i = 0; i < cenario.getItemCount(); i++) {
            if (cenario.getItemAt(i).id.equals(id)) {
                cenario.setSelectedIndex(i);
                return;
            }
        }
    }

    ConfiguracoesJogo lerControles() {
        ItemCenario item = (ItemCenario) cenario.getSelectedItem();
        return new ConfiguracoesJogo(
                musica.isSelected(),
                efeitos.isSelected(),
                volumeMusica.getValue() / 100.0,
                volumeEfeitos.getValue() / 100.0,
                item == null ? RegistroCenarios.ID_PADRAO : item.id);
    }

    private void atualizarRotulos() {
        valorMusica.setText(volumeMusica.getValue() + "%");
        valorEfeitos.setText(volumeEfeitos.getValue() + "%");
        volumeMusica.setEnabled(musica.isSelected());
        volumeEfeitos.setEnabled(efeitos.isSelected());
    }

    private void aoMudar() {
        if (atualizandoControles) {
            return;
        }
        atualizarRotulos();
        preferencias.aplicar(lerControles());      // previa imediata no audio
        mostrarStatus(preferencias.temAlteracoesNaoSalvas() ? "Alterações não salvas." : " ", false);
    }

    // =====================================================
    // ACOES
    // =====================================================
    void restaurarPadroes() {
        mostrar(ConfiguracoesJogo.padrao());
        preferencias.aplicar(lerControles());
        mostrarStatus("Padrões restaurados. Clique em SALVAR para mantê-los.", false);
    }

    /** @return true se gravou */
    boolean salvar() {
        try {
            preferencias.salvar(lerControles());
            mostrarStatus("Configurações salvas.", false);
            status.setForeground(EstiloTelas.OK);
            return true;
        } catch (PersistenciaException ex) {
            mostrarStatus("Não foi possível salvar: " + ex.getMessage(), true);
            dialogos.avisar(this, "Configurações", "Não foi possível salvar as configurações.\n\n"
                    + ex.getMessage() + "\n\nAs mudanças continuam valendo até fechar o jogo.");
            return false;
        }
    }

    void voltar() {
        sairPara(aoVoltar);
    }

    /** Entrega 5C: CONTROLES, com a mesma protecao de alteracoes nao salvas. */
    void abrirControles() {
        if (aoAbrirControles != null) {
            sairPara(aoAbrirControles);
        }
    }

    private void sairPara(Runnable destino) {
        if (preferencias.temAlteracoesNaoSalvas()) {
            Dialogos.Resposta r = dialogos.perguntar(this, "Configurações",
                    "Há alterações não salvas. Deseja salvá-las antes de voltar?");
            if (r == Dialogos.Resposta.CANCELAR) {
                return;
            }
            if (r == Dialogos.Resposta.SIM && !salvar()) {
                return;
            }
            if (r == Dialogos.Resposta.NAO) {
                preferencias.descartarAlteracoes();
            }
        }
        destino.run();
    }

    private void mostrarStatus(String texto, boolean erro) {
        status.setText("<html>" + escapar(texto) + "</html>");
        status.setForeground(erro ? EstiloTelas.ERRO : EstiloTelas.TEXTO_SUAVE);
    }

    static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // =====================================================
    // ACESSO PARA TESTES
    // =====================================================
    JCheckBox controleMusica() {
        return musica;
    }

    JCheckBox controleEfeitos() {
        return efeitos;
    }

    JSlider controleVolumeMusica() {
        return volumeMusica;
    }

    JSlider controleVolumeEfeitos() {
        return volumeEfeitos;
    }

    JComboBox<ItemCenario> controleCenario() {
        return cenario;
    }

    String textoStatus() {
        return status.getText();
    }

    JButton botaoVoltar() {
        return voltar;
    }

    JButton botaoControles() {
        return controles;
    }
}
