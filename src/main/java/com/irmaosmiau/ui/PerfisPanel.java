package com.irmaosmiau.ui;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.persistencia.PersistenciaException;
import com.irmaosmiau.persistencia.ResultadoCarga;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.DefinicaoPersonagem;
import com.irmaosmiau.personagens.LimitesAtributos;
import com.irmaosmiau.personagens.PerfilLutador;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

/**
 * Tela PERSONAGENS PERSONALIZADOS (Entrega 4C).
 *
 * Lista de perfis a esquerda; editor (nome + 8 atributos) a direita.
 * Toda regra de validacao e persistencia vem da 4B (ServicoPerfis,
 * PerfilLutador, LimitesAtributos). Os personagens oficiais nunca sao
 * editados: um perfil novo apenas COPIA os atributos do personagem-base.
 *
 * Entrega 4D: os perfis sao escolhidos na selecao (COMECAR/TREINO).
 */
public final class PerfisPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    static final String AVISO_COMBATE =
            "Use os perfis em COMEÇAR ou TREINO, no slot do personagem-base. Branco e Preto oficiais nunca mudam.";

    private final transient ServicoPerfis servico;
    private final transient Dialogos dialogos;
    private final transient Runnable aoVoltar;

    // ---- lista
    private final DefaultListModel<ItemPerfil> modeloLista = new DefaultListModel<>();
    private final JList<ItemPerfil> lista = new JList<>(modeloLista);
    private final JComboBox<ItemBase> base = new JComboBox<>();
    private final JButton novo = EstiloTelas.botao("NOVO PERFIL");
    private final JButton excluir = EstiloTelas.botao("EXCLUIR");
    private final JButton voltar = EstiloTelas.botao("VOLTAR");

    // ---- editor
    private final JTextField nome = new JTextField();
    private final JLabel rotuloBase = EstiloTelas.texto(" ", EstiloTelas.ROTULO, EstiloTelas.TEXTO);
    private final JLabel rotuloId = EstiloTelas.texto(" ", EstiloTelas.PEQUENA, EstiloTelas.TEXTO_SUAVE);
    private final Map<String, Linha> linhas = new LinkedHashMap<>();
    private final JButton salvar = EstiloTelas.botao("SALVAR");
    private final JButton padraoBase = EstiloTelas.botao("PADRÃO DA BASE");
    private final JButton descartar = EstiloTelas.botao("DESCARTAR");
    private final JLabel status = EstiloTelas.texto(" ", EstiloTelas.NORMAL, EstiloTelas.TEXTO_SUAVE);

    // ---- estado do editor
    private PerfilLutador editando;          // null = nenhum ou novo
    private boolean novoPerfil;
    private String idBaseEditando;
    private String nomeOriginal = "";
    private AtributosLutador atributosOriginais;
    private boolean mudandoSelecao;
    private boolean sincronizando;
    private boolean preenchendoEditor;   // texto/valores colocados pelo proprio codigo

    /** Uma linha do editor: slider + campo numerico. */
    final class Linha {
        final LimitesAtributos.Limite limite;
        final JLabel rotulo;
        final JSlider slider;
        final JSpinner campo;

        Linha(LimitesAtributos.Limite limite) {
            this.limite = limite;
            this.rotulo = EstiloTelas.texto(nomeVisivel(limite.getNome()), EstiloTelas.ROTULO, EstiloTelas.TEXTO);
            int min = (int) Math.ceil(limite.getMinimo());
            int max = (int) Math.floor(limite.getMaximo());
            this.slider = new JSlider(min, max, min);
            this.slider.setOpaque(false);
            this.campo = new JSpinner(new SpinnerNumberModel(limite.getMinimo(), limite.getMinimo(),
                    limite.getMaximo(), 1.0));
            this.campo.setEditor(new JSpinner.NumberEditor(campo, "0.##"));
            this.campo.setFont(EstiloTelas.NORMAL);

            slider.addChangeListener(e -> {
                if (!sincronizando) {
                    sincronizando = true;
                    campo.setValue((double) slider.getValue());
                    sincronizando = false;
                    aoEditar();
                }
            });
            campo.addChangeListener(e -> {
                if (!sincronizando) {
                    sincronizando = true;
                    slider.setValue((int) Math.round(valor()));
                    sincronizando = false;
                    aoEditar();
                }
            });
        }

        double valor() {
            return ((Number) campo.getValue()).doubleValue();
        }

        void definir(double v) {
            sincronizando = true;
            campo.setValue(v);
            slider.setValue((int) Math.round(v));
            sincronizando = false;
        }

        void marcarErro(boolean erro) {
            rotulo.setForeground(erro ? EstiloTelas.ERRO : EstiloTelas.TEXTO);
        }
    }

    static final class ItemPerfil {
        final PerfilLutador perfil;
        final String nomeBase;

        ItemPerfil(PerfilLutador perfil, String nomeBase) {
            this.perfil = perfil;
            this.nomeBase = nomeBase;
        }

        @Override
        public String toString() {
            return perfil.getNome() + "  (" + nomeBase + ")";
        }
    }

    static final class ItemBase {
        final DefinicaoPersonagem definicao;

        ItemBase(DefinicaoPersonagem definicao) {
            this.definicao = definicao;
        }

        @Override
        public String toString() {
            return definicao.getNome();
        }
    }

    public PerfisPanel(ServicoPerfis servico, Dialogos dialogos, Runnable aoVoltar) {
        this.servico = servico;
        this.dialogos = dialogos;
        this.aoVoltar = aoVoltar;

        setPreferredSize(new Dimension(EstiloTelas.LARGURA, EstiloTelas.ALTURA));
        setLayout(null);

        for (DefinicaoPersonagem d : servico.getCatalogo().listar()) {
            base.addItem(new ItemBase(d));
        }
        for (LimitesAtributos.Limite l : LimitesAtributos.todos()) {
            linhas.put(l.getNome(), new Linha(l));
        }

        montar();

        lista.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !mudandoSelecao) {
                aoSelecionar();
            }
        });
        novo.addActionListener(e -> novoPerfil());
        excluir.addActionListener(e -> excluir());
        voltar.addActionListener(e -> voltar());
        salvar.addActionListener(e -> salvar());
        padraoBase.addActionListener(e -> restaurarPadraoDaBase());
        descartar.addActionListener(e -> descartar());
        nome.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { aoEditar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { aoEditar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { aoEditar(); }
        });

        recarregarLista(null);
        limparEditor();
    }

    // =====================================================
    // LAYOUT (posicoes fixas para 800 x 450)
    // =====================================================
    private void montar() {
        colocar(EstiloTelas.titulo("PERSONAGENS PERSONALIZADOS"), 34, 18, 600, 30);
        colocar(EstiloTelas.texto(AVISO_COMBATE, EstiloTelas.PEQUENA, new Color(250, 200, 90)), 34, 48, 740, 16);

        colocar(EstiloTelas.rotulo("Perfis salvos"), 34, 72, 220, 18);
        lista.setFont(EstiloTelas.NORMAL);
        JScrollPane rolagem = new JScrollPane(lista);
        colocar(rolagem, 34, 92, 222, 226);

        colocar(EstiloTelas.rotulo("Base:"), 34, 330, 44, 22);
        base.setFont(EstiloTelas.NORMAL);
        colocar(base, 80, 328, 176, 26);
        colocar(novo, 34, 360, 222, 30);
        colocar(excluir, 34, 396, 108, 30);
        colocar(voltar, 148, 396, 108, 30);

        colocar(EstiloTelas.rotulo("Nome"), 276, 72, 50, 22);
        nome.setFont(EstiloTelas.NORMAL);
        colocar(nome, 326, 70, 230, 26);
        colocar(rotuloBase, 566, 72, 210, 22);
        colocar(rotuloId, 276, 98, 500, 16);

        int y = 120;
        for (Linha l : linhas.values()) {
            colocar(l.rotulo, 276, y, 120, 24);
            colocar(l.slider, 396, y, 220, 24);
            colocar(l.campo, 620, y, 72, 24);
            colocar(EstiloTelas.texto(formatar(l.limite.getMinimo()) + " – " + formatar(l.limite.getMaximo()),
                    EstiloTelas.PEQUENA, EstiloTelas.TEXTO_SUAVE), 698, y, 76, 24);
            y += 27;
        }

        colocar(salvar, 276, 342, 120, 30);
        colocar(padraoBase, 404, 342, 170, 30);
        colocar(descartar, 582, 342, 120, 30);

        status.setVerticalAlignment(JLabel.TOP);
        colocar(status, 276, 380, 500, 46);
    }

    private void colocar(java.awt.Component c, int x, int y, int l, int a) {
        c.setBounds(x, y, l, a);
        add(c);
    }

    @Override
    protected void paintComponent(Graphics g) {
        EstiloTelas.pintarFundo(g, getWidth(), getHeight());
    }

    // =====================================================
    // LISTA
    // =====================================================
    private void recarregarLista(String selecionarId) {
        ResultadoCarga<List<PerfilLutador>> r;
        try {
            r = servico.listar();
        } catch (PersistenciaException ex) {
            mostrarStatus("Não foi possível ler os perfis: " + ex.getMessage(), true);
            return;
        }
        mudandoSelecao = true;
        try {
            modeloLista.clear();
            for (PerfilLutador p : r.getValor()) {
                modeloLista.addElement(new ItemPerfil(p, nomeDaBase(p.getIdPersonagemBase())));
            }
            lista.clearSelection();
            if (selecionarId != null) {
                for (int i = 0; i < modeloLista.size(); i++) {
                    if (modeloLista.get(i).perfil.getId().equals(selecionarId)) {
                        lista.setSelectedIndex(i);
                    }
                }
            }
        } finally {
            mudandoSelecao = false;
        }
        if (r.temAvisos()) {
            mostrarStatus(r.getAvisos().size() + " arquivo(s) de perfil com problema foram ignorados e preservados: "
                    + r.getAvisos().get(0), true);
        }
    }

    private String nomeDaBase(String idBase) {
        DefinicaoPersonagem d = servico.getCatalogo().buscar(idBase);
        return d == null ? idBase + " (inexistente)" : d.getNome();
    }

    private void aoSelecionar() {
        ItemPerfil item = lista.getSelectedValue();
        if (item == null) {
            return;
        }
        if (editando != null && editando.getId().equals(item.perfil.getId()) && !novoPerfil) {
            return;
        }
        String idEscolhido = item.perfil.getId();
        if (!podeAbandonarEdicao()) {
            // Volta a selecao anterior sem disparar de novo.
            mudandoSelecao = true;
            selecionarNaLista(editando == null ? null : editando.getId());
            mudandoSelecao = false;
            return;
        }
        // Salvar pode ter recarregado a lista: reabre o item escolhido pela versao atual.
        mudandoSelecao = true;
        selecionarNaLista(idEscolhido);
        mudandoSelecao = false;
        ItemPerfil atual = lista.getSelectedValue();
        if (atual != null) {
            abrir(atual.perfil);
        }
    }

    private void selecionarNaLista(String id) {
        lista.clearSelection();
        for (int i = 0; id != null && i < modeloLista.size(); i++) {
            if (modeloLista.get(i).perfil.getId().equals(id)) {
                lista.setSelectedIndex(i);
            }
        }
    }

    // =====================================================
    // EDITOR
    // =====================================================
    private void limparEditor() {
        preenchendoEditor = true;
        try {
            limparEditorSemAvisar();
        } finally {
            preenchendoEditor = false;
        }
    }

    private void limparEditorSemAvisar() {
        editando = null;
        novoPerfil = false;
        idBaseEditando = null;
        nome.setText("");
        rotuloBase.setText(" ");
        rotuloId.setText("Selecione um perfil na lista ou crie um NOVO PERFIL.");
        DefinicaoPersonagem primeira = ((ItemBase) base.getItemAt(0)).definicao;
        mostrarAtributos(primeira.getAtributosPadrao());
        nomeOriginal = "";
        atributosOriginais = lerAtributosSemValidar();
        habilitarEditor(false);
    }

    private void abrir(PerfilLutador p) {
        preenchendoEditor = true;
        try {
            editando = p;
            novoPerfil = false;
            idBaseEditando = p.getIdPersonagemBase();
            nomeOriginal = p.getNome();
            atributosOriginais = p.getAtributos();
            nome.setText(p.getNome());
            rotuloBase.setText("Base: " + nomeDaBase(idBaseEditando));
            rotuloId.setText("ID: " + p.getId());
            mostrarAtributos(p.getAtributos());
        } finally {
            preenchendoEditor = false;
        }
        habilitarEditor(true);
        mostrarStatus(" ", false);
    }

    void novoPerfil() {
        if (!podeAbandonarEdicao()) {
            return;
        }
        DefinicaoPersonagem d = ((ItemBase) base.getSelectedItem()).definicao;
        mudandoSelecao = true;
        lista.clearSelection();
        mudandoSelecao = false;

        preenchendoEditor = true;
        try {
            editando = null;
            novoPerfil = true;
            idBaseEditando = d.getId();
            nomeOriginal = "";
            atributosOriginais = null;      // novo = sempre "nao salvo"
            nome.setText("Meu " + d.getNome().replace("Irmão Miau ", ""));
            rotuloBase.setText("Base: " + d.getNome());
            rotuloId.setText("Novo perfil (o ID é gerado a partir do nome ao salvar)");
            // Copia dos atributos oficiais: o oficial nunca e alterado.
            mostrarAtributos(d.getAtributosPadrao());
        } finally {
            preenchendoEditor = false;
        }
        habilitarEditor(true);
        mostrarStatus("Ajuste o nome e os atributos e clique em SALVAR.", false);
    }

    private void mostrarAtributos(AtributosLutador a) {
        for (Linha l : linhas.values()) {
            l.definir(l.limite.ler(a));
            l.marcarErro(false);
        }
    }

    private void habilitarEditor(boolean ativo) {
        nome.setEnabled(ativo);
        for (Linha l : linhas.values()) {
            l.slider.setEnabled(ativo);
            l.campo.setEnabled(ativo);
        }
        salvar.setEnabled(ativo);
        padraoBase.setEnabled(ativo);
        descartar.setEnabled(ativo);
        excluir.setEnabled(ativo && !novoPerfil);
    }

    private AtributosLutador lerAtributosSemValidar() {
        double[] v = new double[8];
        int i = 0;
        for (Linha l : linhas.values()) {
            v[i++] = l.valor();
        }
        return new AtributosLutador(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]);
    }

    /** Ha algo no editor que ainda nao foi salvo? */
    boolean temAlteracoesNaoSalvas() {
        if (novoPerfil) {
            return true;
        }
        if (editando == null || atributosOriginais == null) {
            return false;
        }
        return !nome.getText().trim().equals(nomeOriginal)
                || !PerfilLutador.mesmosValores(lerAtributosSemValidar(), atributosOriginais);
    }

    private void aoEditar() {
        if (sincronizando || preenchendoEditor || (editando == null && !novoPerfil)) {
            return;
        }
        mostrarStatus(temAlteracoesNaoSalvas() ? "Alterações não salvas." : " ", false);
    }

    // =====================================================
    // ACOES
    // =====================================================
    /** @return true se salvou */
    boolean salvar() {
        if (editando == null && !novoPerfil) {
            return false;
        }

        // 1) Campos digitados: valor fora do limite ou texto invalido.
        List<String> erros = new ArrayList<>();
        for (Linha l : linhas.values()) {
            try {
                l.campo.commitEdit();
                l.marcarErro(false);
            } catch (ParseException ex) {
                l.marcarErro(true);
                erros.add(nomeVisivel(l.limite.getNome()) + ": digite um número entre "
                        + formatar(l.limite.getMinimo()) + " e " + formatar(l.limite.getMaximo()));
            }
        }
        AtributosLutador atributos = lerAtributosSemValidar();

        // 2) Regras oficiais da 4B (sem duplicar): LimitesAtributos.
        for (String problema : LimitesAtributos.problemas(atributos)) {
            erros.add(problema);
            String nomeAtributo = problema.split(" ")[0];
            if (linhas.containsKey(nomeAtributo)) {
                linhas.get(nomeAtributo).marcarErro(true);
            }
        }
        if (!erros.isEmpty()) {
            mostrarStatus("Não foi salvo. " + String.join("; ", erros), true);
            return false;
        }

        String nomeDigitado = nome.getText().trim();
        try {
            PerfilLutador salvo;
            if (novoPerfil) {
                String id = GeradorIdPerfil.gerar(nomeDigitado, servico::idDisponivel);
                salvo = servico.criar(id, nomeDigitado, idBaseEditando, atributos);
            } else {
                salvo = servico.atualizar(editando.getId(), nomeDigitado, atributos);
            }
            recarregarLista(salvo.getId());
            abrir(salvo);
            mostrarStatus("Perfil \"" + salvo.getNome() + "\" salvo.", false);
            status.setForeground(EstiloTelas.OK);
            return true;
        } catch (PersistenciaException ex) {
            mostrarStatus("Não foi possível salvar: " + ex.getMessage(), true);
            dialogos.avisar(this, "Personagens personalizados",
                    "Não foi possível salvar o perfil.\n\n" + ex.getMessage());
            return false;
        } catch (IllegalArgumentException ex) {
            // Nome vazio/longo, base inexistente etc. (regras do PerfilLutador/ServicoPerfis)
            mostrarStatus("Não foi salvo. " + ex.getMessage(), true);
            return false;
        }
    }

    void excluir() {
        if (editando == null || novoPerfil) {
            return;
        }
        boolean ok = dialogos.confirmar(this, "Excluir perfil",
                "Excluir o perfil \"" + editando.getNome() + "\"?\nEsta ação não pode ser desfeita.");
        if (!ok) {
            mostrarStatus("Exclusão cancelada.", false);
            return;
        }
        String nomeExcluido = editando.getNome();
        try {
            servico.excluir(editando.getId());
        } catch (PersistenciaException ex) {
            mostrarStatus("Não foi possível excluir: " + ex.getMessage(), true);
            dialogos.avisar(this, "Personagens personalizados", ex.getMessage());
            return;
        }
        recarregarLista(null);
        limparEditor();
        mostrarStatus("Perfil \"" + nomeExcluido + "\" excluído.", false);
    }

    void restaurarPadraoDaBase() {
        if (idBaseEditando == null) {
            return;
        }
        DefinicaoPersonagem d = servico.getCatalogo().buscar(idBaseEditando);
        if (d == null) {
            mostrarStatus("O personagem-base \"" + idBaseEditando + "\" não existe mais.", true);
            return;
        }
        mostrarAtributos(d.getAtributosPadrao());
        mostrarStatus("Atributos do " + d.getNome() + " oficial copiados. Clique em SALVAR para manter.", false);
    }

    void descartar() {
        if (novoPerfil) {
            limparEditor();
            mostrarStatus("Novo perfil descartado.", false);
        } else if (editando != null) {
            abrir(editando);
            mostrarStatus("Alterações descartadas.", false);
        }
    }

    void voltar() {
        if (podeAbandonarEdicao()) {
            aoVoltar.run();
        }
    }

    /** Pergunta salvar/descartar/cancelar se houver alteracoes. */
    private boolean podeAbandonarEdicao() {
        if (!temAlteracoesNaoSalvas()) {
            return true;
        }
        Dialogos.Resposta r = dialogos.perguntar(this, "Personagens personalizados",
                "Há alterações não salvas neste perfil. Deseja salvá-las?");
        if (r == Dialogos.Resposta.CANCELAR) {
            return false;
        }
        if (r == Dialogos.Resposta.SIM) {
            return salvar();
        }
        descartar();
        return true;
    }

    private void mostrarStatus(String texto, boolean erro) {
        status.setText("<html>" + ConfiguracoesPanel.escapar(texto) + "</html>");
        status.setForeground(erro ? EstiloTelas.ERRO : EstiloTelas.TEXTO_SUAVE);
    }

    // =====================================================
    // TEXTOS
    // =====================================================
    static String nomeVisivel(String atributo) {
        switch (atributo) {
            case "condicionamento": return "Condicionamento";
            case "folego": return "Fôlego";
            case "forca": return "Força";
            case "velocidade": return "Velocidade";
            case "agilidade": return "Agilidade";
            case "tecnica": return "Técnica";
            case "recuperacao": return "Recuperação";
            case "peso": return "Peso (kg)";
            default: return atributo;
        }
    }

    private static String formatar(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    // =====================================================
    // ACESSO PARA TESTES
    // =====================================================
    DefaultListModel<ItemPerfil> modeloLista() {
        return modeloLista;
    }

    JList<ItemPerfil> lista() {
        return lista;
    }

    JComboBox<ItemBase> comboBase() {
        return base;
    }

    JTextField campoNome() {
        return nome;
    }

    Linha linha(String atributo) {
        return linhas.get(atributo);
    }

    String textoStatus() {
        return status.getText();
    }

    PerfilLutador perfilEmEdicao() {
        return editando;
    }

    boolean isNovoPerfil() {
        return novoPerfil;
    }
}
