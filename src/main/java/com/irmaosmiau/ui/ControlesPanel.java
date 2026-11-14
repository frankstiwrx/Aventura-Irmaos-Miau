package com.irmaosmiau.ui;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.GrupoAcao;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.ProblemaControle;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.FonteGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.InfoGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepads;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.persistencia.PersistenciaException;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

/**
 * Tela CONFIGURACOES -> CONTROLES (Entrega 5C), em 800 x 450.
 *
 * - Escolhe o jogador (slot) e lista todas as acoes dele, agrupadas, com rolagem.
 * - Clicar na tecla de uma acao e apertar outra remapeia. ESC cancela a
 *   captura; ESC e F1..F12 nunca sao aceitas (reservadas).
 * - Durante a captura TODAS as teclas sao consumidas aqui: nada mais na
 *   janela reage (nem botoes com ESPACO/ENTER) e nenhum comando do jogo roda.
 * - Tecla ja usada (inclusive pelo outro jogador): pergunta se deve trocar.
 * - As mudancas sao uma copia: so valem nas partidas depois de SALVAR.
 *   VOLTAR com alteracoes pergunta salvar / descartar / cancelar.
 * - Entrega 5D: "Dispositivo" escolhe teclado ou gamepad para o jogador
 *   selecionado e mostra os gamepads conectados (atualizado a cada segundo).
 *   As teclas continuam valendo como reserva quando nao ha gamepad livre.
 * - Entrega 5F: "Editar: Gamepad" lista os botoes do jogador no esquema
 *   escolhido (Classico 5D ou Contextual experimental). Clicar e apertar o
 *   botao real remapeia (gatilhos com limiar e histerese; o que ja estava
 *   segurado e ignorado; ESC ou Start cancelam; Backspace deixa sem botao).
 *   Durante a captura nenhuma tecla ou botao aciona a tela.
 */
public final class ControlesPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient PreferenciasControles preferencias;
    private final transient Dialogos dialogos;
    private final transient Runnable aoVoltar;
    private final transient PreferenciasDispositivos preferenciasDispositivos;
    private final transient ProvedorGamepads gamepads;
    private transient AssociacaoDispositivos edicaoDispositivos;
    private final JComboBox<TipoDispositivo> dispositivo = new JComboBox<>(TipoDispositivo.values());
    private final JLabel situacaoGamepads = EstiloTelas.texto(" ", EstiloTelas.PEQUENA, EstiloTelas.TEXTO_SUAVE);
    private final javax.swing.Timer relogioGamepads = new javax.swing.Timer(1000, e -> mostrarSituacaoGamepads());
    private boolean mostrandoDispositivo;

    // Entrega 5F: botoes do gamepad.
    enum ModoLista {
        TECLAS("Teclas"),
        GAMEPAD("Gamepad");

        private final String rotulo;

        ModoLista(String rotulo) {
            this.rotulo = rotulo;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    private final transient PreferenciasGamepad preferenciasGamepad;
    private transient MapaGamepad edicaoGamepad;
    private ModoLista modoLista = ModoLista.TECLAS;
    private final JComboBox<ModoLista> seletorLista = new JComboBox<>(ModoLista.values());
    private final JComboBox<EsquemaGamepad> seletorEsquema = new JComboBox<>(EsquemaGamepad.values());
    private final Map<ItemGamepad, JButton> botoesGamepad = new java.util.LinkedHashMap<>();
    private transient ItemGamepad capturandoBotao;
    private final java.util.Set<BotaoGamepad> seguradosNaCaptura = java.util.EnumSet.noneOf(BotaoGamepad.class);
    private final boolean[] gatilhoNaCaptura = new boolean[2];
    private final javax.swing.Timer relogioCaptura = new javax.swing.Timer(16, e -> lerBotoesDaCaptura());

    private transient MapaControles edicao;
    private SlotJogador slotAtual = SlotJogador.JOGADOR_1;
    private AcaoJogador capturando;

    private final JToggleButton botaoJogador1 = new JToggleButton("JOGADOR 1 · Miau Branco");
    private final JToggleButton botaoJogador2 = new JToggleButton("JOGADOR 2 · Miau Preto");
    private final JPanel lista = new JPanel(new GridBagLayout());
    private final JScrollPane rolagem = new JScrollPane(lista);
    private final Map<AcaoJogador, JButton> botoesTecla = new EnumMap<>(AcaoJogador.class);
    private final JLabel status = EstiloTelas.texto(" ", EstiloTelas.NORMAL, EstiloTelas.TEXTO_SUAVE);

    private final JButton restaurar = EstiloTelas.botao("RESTAURAR PADRÕES");
    private final JButton salvar = EstiloTelas.botao("SALVAR");
    private final JButton voltar = EstiloTelas.botao("VOLTAR");

    private final transient KeyEventDispatcher captura = this::capturarEvento;

    public ControlesPanel(PreferenciasControles preferencias, Dialogos dialogos, Runnable aoVoltar) {
        this(preferencias, PreferenciasDispositivos.global(), ServicoGamepads.global(), dialogos, aoVoltar);
    }

    /** Entrega 5D: com a escolha de dispositivo de cada jogador. */
    public ControlesPanel(PreferenciasControles preferencias, PreferenciasDispositivos preferenciasDispositivos,
            ProvedorGamepads gamepads, Dialogos dialogos, Runnable aoVoltar) {
        this(preferencias, preferenciasDispositivos, PreferenciasGamepad.global(), gamepads, dialogos, aoVoltar);
    }

    /** Entrega 5F: com os botoes do gamepad de cada jogador. */
    public ControlesPanel(PreferenciasControles preferencias, PreferenciasDispositivos preferenciasDispositivos,
            PreferenciasGamepad preferenciasGamepad, ProvedorGamepads gamepads, Dialogos dialogos,
            Runnable aoVoltar) {
        this.preferenciasGamepad = preferenciasGamepad;
        this.edicaoGamepad = preferenciasGamepad.getAtual();
        this.preferenciasDispositivos = preferenciasDispositivos;
        this.gamepads = gamepads;
        this.edicaoDispositivos = preferenciasDispositivos.getAtual();
        this.preferencias = preferencias;
        this.dialogos = dialogos;
        this.aoVoltar = aoVoltar;
        this.edicao = preferencias.getAtual();

        setPreferredSize(new Dimension(EstiloTelas.LARGURA, EstiloTelas.ALTURA));
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(18, 36, 18, 28));
        montar();
        montarLista();

        List<String> avisos = new java.util.ArrayList<>(preferencias.getAvisos());
        avisos.addAll(preferenciasDispositivos.getAvisos());
        avisos.addAll(preferenciasGamepad.getAvisos());
        if (!avisos.isEmpty()) {
            mostrarStatus(avisos.get(avisos.size() - 1), true);
        } else if (!edicao.isValido()) {
            mostrarProblemas();
        }

        botaoJogador1.addActionListener(e -> selecionarSlot(SlotJogador.JOGADOR_1));
        botaoJogador2.addActionListener(e -> selecionarSlot(SlotJogador.JOGADOR_2));
        restaurar.addActionListener(e -> restaurarPadroes());
        salvar.addActionListener(e -> salvar());
        voltar.addActionListener(e -> voltar());
        dispositivo.addActionListener(e -> aoMudarDispositivo());
        seletorLista.addActionListener(e -> {
            if (!mostrandoDispositivo) {
                selecionarModoLista((ModoLista) seletorLista.getSelectedItem());
            }
        });
        seletorEsquema.addActionListener(e -> {
            if (!mostrandoDispositivo) {
                selecionarEsquema((EsquemaGamepad) seletorEsquema.getSelectedItem());
            }
        });
        mostrarDispositivo();
        mostrarSituacaoGamepads();
    }

    // =====================================================
    // MONTAGEM
    // =====================================================
    private void montar() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(EstiloTelas.titulo("CONTROLES"), BorderLayout.WEST);

        JPanel slots = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        slots.setOpaque(false);
        ButtonGroup grupo = new ButtonGroup();
        for (JToggleButton b : new JToggleButton[]{botaoJogador1, botaoJogador2}) {
            b.setFont(EstiloTelas.ROTULO);
            b.setFocusPainted(false);
            grupo.add(b);
            slots.add(b);
        }
        botaoJogador1.setSelected(true);
        topo.add(slots, BorderLayout.EAST);
        // Entrega 5F: a situacao dos gamepads sobe para a linha do titulo (a lista nao perde altura).
        situacaoGamepads.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
        topo.add(situacaoGamepads, BorderLayout.CENTER);

        // Entrega 5D: dispositivo do jogador selecionado. 5F: lista e esquema do gamepad.
        JPanel linhaDispositivo = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        linhaDispositivo.setOpaque(false);
        linhaDispositivo.add(EstiloTelas.rotulo("Dispositivo:"));
        dispositivo.setFont(EstiloTelas.NORMAL);
        dispositivo.setPreferredSize(new Dimension(105, 24));
        linhaDispositivo.add(dispositivo);
        linhaDispositivo.add(EstiloTelas.rotulo("  Editar:"));
        seletorLista.setFont(EstiloTelas.NORMAL);
        seletorLista.setPreferredSize(new Dimension(100, 24));
        linhaDispositivo.add(seletorLista);
        linhaDispositivo.add(EstiloTelas.rotulo("  Esquema do gamepad:"));
        seletorEsquema.setFont(EstiloTelas.NORMAL);
        seletorEsquema.setPreferredSize(new Dimension(185, 24));
        linhaDispositivo.add(seletorEsquema);

        JPanel norte = new JPanel(new BorderLayout(0, 6));
        norte.setOpaque(false);
        norte.add(topo, BorderLayout.NORTH);
        norte.add(linhaDispositivo, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        lista.setOpaque(false);
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        rolagem.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 40)));
        rolagem.getVerticalScrollBar().setUnitIncrement(16);
        add(rolagem, BorderLayout.CENTER);

        JPanel base = new JPanel(new BorderLayout(0, 2));
        base.setOpaque(false);
        base.add(EstiloTelas.texto("<html>Clique na tecla de uma ação e pressione a nova (ESC cancela). "
                + "ESC e F1–F12 são do sistema.<br>Os dois jogadores dividem o teclado: cada tecla só pode "
                + "ter uma ação. Os controles salvos valem na próxima partida.</html>",
                EstiloTelas.PEQUENA, EstiloTelas.TEXTO_SUAVE), BorderLayout.NORTH);
        status.setPreferredSize(new Dimension(700, 32));
        status.setVerticalAlignment(JLabel.TOP);
        base.add(status, BorderLayout.CENTER);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        botoes.setOpaque(false);
        botoes.add(restaurar);
        botoes.add(salvar);
        botoes.add(voltar);
        base.add(botoes, BorderLayout.SOUTH);
        add(base, BorderLayout.SOUTH);
    }

    /** Linhas do slot atual: titulo de grupo, depois "acao ........ [tecla]". */
    private void montarLista() {
        if (modoLista == ModoLista.GAMEPAD) {
            montarListaGamepad();
            return;
        }
        lista.removeAll();
        botoesTecla.clear();
        botoesGamepad.clear();
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.insets = new Insets(2, 10, 2, 10);
        for (GrupoAcao grupo : GrupoAcao.values()) {
            List<AcaoJogador> doGrupo = slotAtual.acoes().stream()
                    .filter(a -> a.getGrupo() == grupo).collect(Collectors.toList());
            if (doGrupo.isEmpty()) {
                continue;
            }
            g.gridx = 0;
            g.gridwidth = 2;
            g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(8, 6, 2, 10);
            JLabel titulo = EstiloTelas.texto(grupo.getTitulo(), EstiloTelas.ROTULO, new Color(255, 214, 120));
            lista.add(titulo, g);
            g.gridy++;
            g.gridwidth = 1;
            g.insets = new Insets(2, 16, 2, 10);
            for (AcaoJogador a : doGrupo) {
                g.gridx = 0;
                g.weightx = 1;
                g.fill = GridBagConstraints.HORIZONTAL;
                lista.add(EstiloTelas.texto(a.getRotulo(), EstiloTelas.NORMAL, EstiloTelas.TEXTO), g);
                g.gridx = 1;
                g.weightx = 0;
                g.fill = GridBagConstraints.NONE;
                g.anchor = GridBagConstraints.EAST;
                JButton tecla = EstiloTelas.botao("");
                tecla.setPreferredSize(new Dimension(170, 24));
                tecla.addActionListener(e -> iniciarCaptura(a));
                botoesTecla.put(a, tecla);
                lista.add(tecla, g);
                g.anchor = GridBagConstraints.WEST;
                g.gridy++;
            }
        }
        // Empurra as linhas para cima.
        g.gridx = 0;
        g.weighty = 1;
        lista.add(new JLabel(), g);
        atualizarTeclas();
        lista.revalidate();
        lista.repaint();
    }

    // =====================================================
    // LISTA DE BOTOES DO GAMEPAD (Entrega 5F)
    // =====================================================
    private void montarListaGamepad() {
        lista.removeAll();
        botoesTecla.clear();
        botoesGamepad.clear();
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        List<ItemGamepad> itens = edicaoGamepad.itens(slotAtual);
        for (GrupoAcao grupo : GrupoAcao.values()) {
            List<ItemGamepad> doGrupo = itens.stream()
                    .filter(i -> i.acao() != null && i.acao().getGrupo() == grupo).collect(Collectors.toList());
            g.gridy = linhasGamepad(g, grupo.getTitulo(), doGrupo);
        }
        List<ItemGamepad> comandos = itens.stream().filter(i -> i.comando() != null).collect(Collectors.toList());
        g.gridy = linhasGamepad(g, "TÉCNICAS PELO D-PAD (camadas: D-pad · LT+D-pad · LT+LB+D-pad)", comandos);
        g.gridx = 0;
        g.weighty = 1;
        lista.add(new JLabel(), g);
        atualizarTeclas();
        lista.revalidate();
        lista.repaint();
    }

    private int linhasGamepad(GridBagConstraints g, String titulo, List<ItemGamepad> itens) {
        if (itens.isEmpty()) {
            return g.gridy;
        }
        g.gridx = 0;
        g.gridwidth = 2;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(8, 6, 2, 10);
        lista.add(EstiloTelas.texto(titulo, EstiloTelas.ROTULO, new Color(255, 214, 120)), g);
        g.gridy++;
        g.gridwidth = 1;
        g.insets = new Insets(2, 16, 2, 10);
        for (ItemGamepad item : itens) {
            g.gridx = 0;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            lista.add(EstiloTelas.texto(item.rotulo(), EstiloTelas.NORMAL, EstiloTelas.TEXTO), g);
            g.gridx = 1;
            g.weightx = 0;
            g.fill = GridBagConstraints.NONE;
            g.anchor = GridBagConstraints.EAST;
            JButton b = EstiloTelas.botao("");
            b.setPreferredSize(new Dimension(170, 24));
            b.addActionListener(e -> iniciarCapturaGamepad(item));
            botoesGamepad.put(item, b);
            lista.add(b, g);
            g.anchor = GridBagConstraints.WEST;
            g.gridy++;
        }
        return g.gridy;
    }

    private void atualizarBotoesGamepad() {
        List<MapaGamepad.Problema> problemas = edicaoGamepad.problemas();
        for (Map.Entry<ItemGamepad, JButton> e : botoesGamepad.entrySet()) {
            ItemGamepad item = e.getKey();
            JButton b = e.getValue();
            if (item.equals(capturandoBotao)) {
                b.setText("aperte um botão…");
                b.setForeground(new Color(150, 60, 0));
                continue;
            }
            b.setText(edicaoGamepad.botao(slotAtual, item).map(BotaoGamepad::getRotulo).orElse("—"));
            boolean comProblema = problemas.stream().anyMatch(p -> p.slot() == slotAtual
                    && (p.item().equals(item) || item.equals(p.outro())));
            b.setForeground(comProblema ? new Color(180, 30, 20) : Color.BLACK);
        }
    }

    void selecionarModoLista(ModoLista modo) {
        cancelarCaptura();
        modoLista = modo == null ? ModoLista.TECLAS : modo;
        mostrarDispositivo();
        montarLista();
        rolagem.getVerticalScrollBar().setValue(0);
    }

    void selecionarEsquema(EsquemaGamepad esquema) {
        if (esquema == null || esquema == edicaoGamepad.esquema(slotAtual)) {
            return;
        }
        cancelarCaptura();
        edicaoGamepad = edicaoGamepad.comEsquema(slotAtual, esquema);
        modoLista = ModoLista.GAMEPAD;
        mostrarDispositivo();
        montarLista();
        mostrarStatus(slotAtual.getSigla() + ": esquema " + esquema.getRotulo() + " com os botões padrão dele"
                + (esquema == EsquemaGamepad.CONTEXTUAL
                        ? " (D-pad = técnicas; LT e LT+LB = camadas; agachar no B; RB livre)." : ".")
                + " Alterações não salvas.", false);
    }

    // =====================================================
    // CAPTURA DE BOTAO DO GAMEPAD (Entrega 5F)
    // =====================================================
    void iniciarCapturaGamepad(ItemGamepad item) {
        cancelarCaptura();
        capturandoBotao = item;
        seguradosNaCaptura.clear();
        seguradosNaCaptura.addAll(botoesSeguradosAgora());     // o que ja esta apertado e ignorado
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(captura);
        relogioCaptura.start();
        atualizarTeclas();
        mostrarStatus("Aperte o botão do gamepad para " + slotAtual.getSigla() + " · " + item.rotulo()
                + " (ESC ou Start cancelam; Backspace deixa sem botão)."
                + (gamepads.conectados().isEmpty() ? " Nenhum gamepad conectado agora." : ""), false);
    }

    /** Botoes apertados em qualquer gamepad conectado (gatilhos com histerese). */
    private java.util.Set<BotaoGamepad> botoesSeguradosAgora() {
        java.util.Set<BotaoGamepad> s = java.util.EnumSet.noneOf(BotaoGamepad.class);
        boolean[] gatilhos = new boolean[2];
        for (InfoGamepad g : gamepads.conectados()) {
            Optional<EstadoGamepad> e = gamepads.estado(g.chave());
            if (e.isEmpty()) {
                continue;
            }
            for (BotaoGamepad b : BotaoGamepad.values()) {
                if (!b.isVirtual() && e.get().pressionado(b)) {
                    s.add(b);
                }
            }
            float lt = e.get().eixo(com.irmaosmiau.dispositivos.EixoGamepad.GATILHO_ESQ);
            float rt = e.get().eixo(com.irmaosmiau.dispositivos.EixoGamepad.GATILHO_DIR);
            gatilhos[0] |= gatilhoNaCaptura[0] ? lt >= FonteGamepad.LIMIAR_SOLTAR_GATILHO : lt >= EstadoGamepad.LIMIAR_GATILHO;
            gatilhos[1] |= gatilhoNaCaptura[1] ? rt >= FonteGamepad.LIMIAR_SOLTAR_GATILHO : rt >= EstadoGamepad.LIMIAR_GATILHO;
        }
        gatilhoNaCaptura[0] = gatilhos[0];
        gatilhoNaCaptura[1] = gatilhos[1];
        if (gatilhos[0]) {
            s.add(BotaoGamepad.GATILHO_ESQ);
        }
        if (gatilhos[1]) {
            s.add(BotaoGamepad.GATILHO_DIR);
        }
        return s;
    }

    /** Timer da captura: o primeiro botao NOVO (apertado depois do inicio) vale. */
    private void lerBotoesDaCaptura() {
        if (capturandoBotao == null) {
            relogioCaptura.stop();
            return;
        }
        java.util.Set<BotaoGamepad> agora = botoesSeguradosAgora();
        seguradosNaCaptura.retainAll(agora);                    // soltou: volta a valer
        for (BotaoGamepad b : BotaoGamepad.values()) {
            if (agora.contains(b) && !seguradosNaCaptura.contains(b)) {
                receberBotao(b);
                return;
            }
        }
    }

    /** Botao apertado durante a captura (tambem usado pelos testes). */
    void receberBotao(BotaoGamepad botao) {
        if (capturandoBotao == null) {
            return;
        }
        ItemGamepad item = capturandoBotao;
        if (botao == MapaGamepad.BOTAO_PAUSA) {
            cancelarCaptura();
            return;
        }
        if (MapaGamepad.RESERVADOS.contains(botao)) {
            mostrarStatus(botao.getRotulo() + " é reservado ao sistema. Escolha outro (ESC cancela).", true);
            return;
        }
        encerrarCaptura();
        Optional<ItemGamepad> dono = Optional.empty();
        for (ItemGamepad outro : edicaoGamepad.itens(slotAtual)) {
            if (!outro.equals(item) && edicaoGamepad.botao(slotAtual, outro).orElse(null) == botao
                    && !podemDividir(item, outro)) {
                dono = Optional.of(outro);
                break;
            }
        }
        if (edicaoGamepad.botao(slotAtual, item).orElse(null) == botao) {
            mostrarStatus("Botão mantido.", false);
            return;
        }
        if (dono.isPresent()) {
            boolean trocar = dialogos.confirmar(this, "Controles",
                    "O botão " + botao.getRotulo() + " já é usado por " + slotAtual.getSigla() + " · "
                    + dono.get().rotulo() + ".\n\nTrocar os botões entre as duas funções?");
            if (!trocar) {
                mostrarStatus("Nada mudou: " + botao.getRotulo() + " continua em " + dono.get().rotulo() + ".", false);
                return;
            }
            edicaoGamepad = edicaoGamepad.trocandoSeNecessario(slotAtual, item, botao);
            mostrarStatus("Botões trocados. " + dono.get().rotulo() + " agora usa "
                    + edicaoGamepad.rotulo(slotAtual, dono.get()) + ". Alterações não salvas.", false);
        } else {
            edicaoGamepad = edicaoGamepad.comBotao(slotAtual, item, botao);
            mostrarStatus(slotAtual.getSigla() + " · " + item.rotulo() + " = " + botao.getRotulo()
                    + ". Alterações não salvas.", false);
        }
        atualizarTeclas();
    }

    private static boolean podemDividir(ItemGamepad a, ItemGamepad b) {
        // Unica divisao permitida: arrasto (com o analogico) e modificador da camada 3 (com LT).
        return (a.acao() == AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO
                        && b.comando() == com.irmaosmiau.dispositivos.ComandoGamepad.MODIFICADOR_CAMADA_3)
                || (b.acao() == AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO
                        && a.comando() == com.irmaosmiau.dispositivos.ComandoGamepad.MODIFICADOR_CAMADA_3);
    }

    /** Backspace durante a captura: o item fica sem botao (ex.: tirar uma acao do RB defeituoso). */
    void removerBotaoCapturado() {
        if (capturandoBotao == null) {
            return;
        }
        ItemGamepad item = capturandoBotao;
        encerrarCaptura();
        edicaoGamepad = edicaoGamepad.semBotao(slotAtual, item);
        mostrarStatus(slotAtual.getSigla() + " · " + item.rotulo() + " ficou sem botão. Alterações não salvas.", false);
        atualizarTeclas();
    }

    private void atualizarTeclas() {
        atualizarBotoesGamepad();
        List<ProblemaControle> problemas = edicao.problemas();
        for (Map.Entry<AcaoJogador, JButton> e : botoesTecla.entrySet()) {
            AcaoJogador a = e.getKey();
            JButton b = e.getValue();
            if (a == capturando) {
                b.setText("pressione uma tecla…");
                b.setForeground(new Color(150, 60, 0));
                continue;
            }
            b.setText(edicao.rotulo(slotAtual, a));
            boolean comProblema = problemas.stream().anyMatch(p -> p.slot() == slotAtual && p.acao() == a);
            b.setForeground(comProblema ? new Color(180, 30, 20) : Color.BLACK);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        EstiloTelas.pintarFundo(g, getWidth(), getHeight());
    }

    // =====================================================
    // SLOT
    // =====================================================
    void selecionarSlot(SlotJogador slot) {
        cancelarCaptura();
        slotAtual = slot;
        (slot == SlotJogador.JOGADOR_1 ? botaoJogador1 : botaoJogador2).setSelected(true);
        mostrarDispositivo();
        montarLista();
        rolagem.getVerticalScrollBar().setValue(0);
    }

    // =====================================================
    // CAPTURA DE TECLA
    // =====================================================
    void iniciarCaptura(AcaoJogador acao) {
        cancelarCaptura();
        capturando = acao;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(captura);
        atualizarTeclas();
        mostrarStatus("Pressione a nova tecla para " + slotAtual.getSigla() + " · " + acao.getRotulo()
                + " (ESC cancela).", false);
    }

    private void cancelarCaptura() {
        if (capturando != null || capturandoBotao != null) {
            encerrarCaptura();
            mostrarStatus("Captura cancelada.", false);
        }
    }

    private void encerrarCaptura() {
        capturando = null;
        capturandoBotao = null;
        relogioCaptura.stop();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(captura);
        atualizarTeclas();
    }

    /** Durante a captura, consome todas as teclas da janela. */
    private boolean capturarEvento(KeyEvent e) {
        if (capturando == null && capturandoBotao == null) {
            return false;
        }
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (capturandoBotao != null) {
                // Captura de botao: o teclado so cancela (ESC) ou remove (Backspace).
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelarCaptura();
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    removerBotaoCapturado();
                }
            } else {
                receberTecla(e.getKeyCode());
            }
        }
        e.consume();
        return true;
    }

    /** Tecla apertada durante a captura (KeyEvent.VK_*). */
    void receberTecla(int codigo) {
        if (capturando == null) {
            return;
        }
        AcaoJogador acao = capturando;
        if (codigo == KeyEvent.VK_ESCAPE) {
            cancelarCaptura();
            return;
        }
        Optional<TeclaFisica> lida = TeclaFisica.porCodigo(codigo);
        if (lida.isEmpty()) {
            mostrarStatus("Essa tecla não é suportada. Escolha outra (ESC cancela).", true);
            return;
        }
        TeclaFisica tecla = lida.get();
        if (tecla.isReservada()) {
            mostrarStatus(tecla.getRotulo() + " é reservada ao sistema. Escolha outra (ESC cancela).", true);
            return;
        }
        encerrarCaptura();

        Optional<MapaControles.Atribuicao> dono = edicao.quemUsa(tecla);
        if (dono.isPresent() && dono.get().equals(new MapaControles.Atribuicao(slotAtual, acao))) {
            mostrarStatus("Tecla mantida.", false);
            return;
        }
        if (dono.isPresent()) {
            String outro = dono.get().slot().getSigla() + " · " + dono.get().acao().getRotulo();
            boolean trocar = dialogos.confirmar(this, "Controles",
                    "A tecla " + tecla.getRotulo() + " já é usada por " + outro + ".\n\n"
                    + "Trocar as teclas entre as duas ações?");
            if (!trocar) {
                mostrarStatus("Nada mudou: " + tecla.getRotulo() + " continua em " + outro + ".", false);
                return;
            }
            edicao = edicao.trocandoSeNecessario(slotAtual, acao, tecla);
            mostrarStatus("Teclas trocadas. " + outro + " agora usa "
                    + edicao.rotulo(dono.get().slot(), dono.get().acao()) + ". Alterações não salvas.", false);
        } else {
            edicao = edicao.comTecla(slotAtual, acao, tecla);
            mostrarStatus(slotAtual.getSigla() + " · " + acao.getRotulo() + " = " + tecla.getRotulo()
                    + ". Alterações não salvas.", false);
        }
        atualizarTeclas();
    }

    // =====================================================
    // ACOES
    // =====================================================
    void restaurarPadroes() {
        cancelarCaptura();
        edicao = MapaControles.padrao();
        edicaoDispositivos = AssociacaoDispositivos.padrao();
        edicaoGamepad = MapaGamepad.padrao();
        mostrarDispositivo();
        montarLista();
        mostrarStatus("Controles padrão restaurados (os dois jogadores, no teclado; gamepad no esquema Clássico)."
                + " Clique em SALVAR para mantê-los.", false);
    }

    boolean temAlteracoesNaoSalvas() {
        return !edicao.equals(preferencias.getAtual())
                || !edicaoDispositivos.equals(preferenciasDispositivos.getAtual())
                || !edicaoGamepad.equals(preferenciasGamepad.getAtual());
    }

    // =====================================================
    // DISPOSITIVO (Entrega 5D)
    // =====================================================
    private void mostrarDispositivo() {
        mostrandoDispositivo = true;
        try {
            dispositivo.setSelectedItem(edicaoDispositivos.tipo(slotAtual));
            seletorLista.setSelectedItem(modoLista);
            seletorEsquema.setSelectedItem(edicaoGamepad.esquema(slotAtual));
            seletorEsquema.setEnabled(modoLista == ModoLista.GAMEPAD);
        } finally {
            mostrandoDispositivo = false;
        }
    }

    private void aoMudarDispositivo() {
        if (mostrandoDispositivo) {
            return;
        }
        selecionarDispositivo((TipoDispositivo) dispositivo.getSelectedItem());
    }

    void selecionarDispositivo(TipoDispositivo tipo) {
        if (tipo == null || tipo == edicaoDispositivos.tipo(slotAtual)) {
            return;
        }
        edicaoDispositivos = edicaoDispositivos.com(slotAtual, tipo);
        mostrarDispositivo();
        mostrarStatus(tipo == TipoDispositivo.GAMEPAD
                ? slotAtual.getSigla() + " usará um gamepad (esquema experimental); sem gamepad livre, "
                        + "as teclas abaixo continuam valendo. Alterações não salvas."
                : slotAtual.getSigla() + " usará o teclado. Alterações não salvas.", false);
    }

    void mostrarSituacaoGamepads() {
        String texto;
        if (!gamepads.isDisponivel()) {
            texto = "Gamepad indisponível: " + gamepads.descricao() + " (o teclado funciona normalmente).";
        } else {
            List<InfoGamepad> lista = gamepads.conectados();
            String nomes = lista.stream().map(InfoGamepad::nome).collect(Collectors.joining(", "));
            texto = lista.isEmpty()
                    ? "Nenhum gamepad conectado (" + gamepads.descricao() + ")."
                    : lista.size() + (lista.size() == 1 ? " gamepad: " : " gamepads: ") + nomes;
        }
        situacaoGamepads.setToolTipText(texto);
        situacaoGamepads.setText(texto);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        relogioGamepads.start();
    }

    /** @return true se gravou */
    boolean salvar() {
        cancelarCaptura();
        if (!edicao.isValido()) {
            mostrarProblemas();
            return false;
        }
        if (!edicaoGamepad.isValido()) {
            mostrarStatus("Resolva antes de salvar: " + edicaoGamepad.problemas().stream()
                    .map(MapaGamepad.Problema::descricao).collect(Collectors.joining("; ")), true);
            return false;
        }
        try {
            preferencias.salvar(edicao);
            preferenciasDispositivos.salvar(edicaoDispositivos);
            preferenciasGamepad.salvar(edicaoGamepad);
            mostrarStatus("Controles salvos. Valem a partir da próxima partida.", false);
            status.setForeground(EstiloTelas.OK);
            return true;
        } catch (PersistenciaException ex) {
            mostrarStatus("Não foi possível salvar: " + ex.getMessage(), true);
            dialogos.avisar(this, "Controles", "Não foi possível salvar os controles.\n\n"
                    + ex.getMessage() + "\n\nOs controles anteriores continuam valendo.");
            return false;
        }
    }

    void voltar() {
        cancelarCaptura();
        if (temAlteracoesNaoSalvas()) {
            Dialogos.Resposta r = dialogos.perguntar(this, "Controles",
                    "Há alterações não salvas nos controles. Deseja salvá-las antes de voltar?");
            if (r == Dialogos.Resposta.CANCELAR) {
                return;
            }
            if (r == Dialogos.Resposta.SIM && !salvar()) {
                return;
            }
            if (r == Dialogos.Resposta.NAO) {
                edicao = preferencias.getAtual();
                edicaoDispositivos = preferenciasDispositivos.getAtual();
                edicaoGamepad = preferenciasGamepad.getAtual();
            }
        }
        aoVoltar.run();
    }

    private void mostrarProblemas() {
        mostrarStatus("Resolva antes de salvar: " + edicao.problemas().stream()
                .map(ProblemaControle::descricao).collect(Collectors.joining("; ")), true);
    }

    private void mostrarStatus(String texto, boolean erro) {
        status.setText("<html>" + ConfiguracoesPanel.escapar(texto) + "</html>");
        status.setForeground(erro ? EstiloTelas.ERRO : EstiloTelas.TEXTO_SUAVE);
    }

    @Override
    public void removeNotify() {
        relogioGamepads.stop();
        relogioCaptura.stop();
        // Saiu da tela no meio de uma captura: nada fica consumindo teclas.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(captura);
        capturando = null;
        capturandoBotao = null;
        super.removeNotify();
    }

    // =====================================================
    // ACESSO PARA TESTES
    // =====================================================
    boolean capturarEventoParaTestes(KeyEvent e) {
        return capturarEvento(e);
    }

    MapaGamepad gamepadEmEdicao() {
        return edicaoGamepad;
    }

    JButton botaoGamepad(ItemGamepad item) {
        return botoesGamepad.get(item);
    }

    boolean isCapturandoBotao() {
        return capturandoBotao != null;
    }

    JComboBox<ModoLista> seletorLista() {
        return seletorLista;
    }

    JComboBox<EsquemaGamepad> seletorEsquema() {
        return seletorEsquema;
    }

    /** Uma leitura do timer de captura (testes com gamepad simulado). */
    void lerBotoesDaCapturaParaTestes() {
        lerBotoesDaCaptura();
    }

    AssociacaoDispositivos dispositivosEmEdicao() {
        return edicaoDispositivos;
    }

    JComboBox<TipoDispositivo> seletorDispositivo() {
        return dispositivo;
    }

    String textoSituacaoGamepads() {
        return situacaoGamepads.getText();
    }

    MapaControles mapaEmEdicao() {
        return edicao;
    }

    SlotJogador slotAtual() {
        return slotAtual;
    }

    boolean isCapturando() {
        return capturando != null;
    }

    JButton botaoTecla(AcaoJogador acao) {
        return botoesTecla.get(acao);
    }

    JScrollPane rolagem() {
        return rolagem;
    }

    String textoStatus() {
        return status.getText();
    }
}
