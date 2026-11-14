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

    private void montar() {
        JPanel cartao = new JPanel(new GridBagLayout());
        cartao.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(6, 8, 6, 8);

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 4;
        cartao.add(EstiloTelas.titulo("CONFIGURAÇÕES"), g);
        g.gridwidth = 1;

        linhaAudio(cartao, g, 1, "Música", musica, volumeMusica, valorMusica);
        linhaAudio(cartao, g, 2, "Efeitos sonoros", efeitos, volumeEfeitos, valorEfeitos);

        g.gridy = 3;
        g.gridx = 0;
        cartao.add(EstiloTelas.rotulo("Cenário preferido"), g);
        g.gridx = 1;
        g.gridwidth = 3;
        cenario.setFont(EstiloTelas.NORMAL);
        cenario.setPreferredSize(new Dimension(260, 28));
        cartao.add(cenario, g);
        g.gridwidth = 1;

        // Entrega 5C: teclas configuraveis dos dois jogadores.
        int linha = 4;
        if (aoAbrirControles != null) {
            g.gridy = linha++;
            g.gridx = 0;
            cartao.add(EstiloTelas.rotulo("Teclado"), g);
            g.gridx = 1;
            g.gridwidth = 3;
            cartao.add(controles, g);
            g.gridwidth = 1;
        }

        g.gridy = linha++;
        g.gridx = 0;
        g.gridwidth = 4;
        cartao.add(EstiloTelas.texto("As mudanças de som valem na hora; clique em SALVAR para mantê-las.",
                EstiloTelas.PEQUENA, EstiloTelas.TEXTO_SUAVE), g);

        g.gridy = linha++;
        status.setPreferredSize(new Dimension(640, 34));
        status.setVerticalAlignment(JLabel.TOP);
        cartao.add(status, g);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        botoes.setOpaque(false);
        botoes.add(restaurar);
        botoes.add(salvar);
        botoes.add(voltar);
        g.gridy = linha;
        g.anchor = GridBagConstraints.CENTER;
        cartao.add(botoes, g);

        add(cartao);
    }

    private static void linhaAudio(JPanel cartao, GridBagConstraints g, int linha, String nome,
            JCheckBox ligado, JSlider volume, JLabel valor) {
        g.gridy = linha;
        g.gridx = 0;
        cartao.add(EstiloTelas.rotulo(nome), g);

        g.gridx = 1;
        ligado.setFont(EstiloTelas.NORMAL);
        ligado.setForeground(EstiloTelas.TEXTO);
        ligado.setOpaque(false);
        cartao.add(ligado, g);

        g.gridx = 2;
        volume.setOpaque(false);
        volume.setPreferredSize(new Dimension(300, 30));
        cartao.add(volume, g);

        g.gridx = 3;
        valor.setPreferredSize(new Dimension(50, 20));
        cartao.add(valor, g);
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
