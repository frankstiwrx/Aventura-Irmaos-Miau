package com.irmaosmiau.ui;

import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.InfoGamepad;
import com.irmaosmiau.dispositivos.LeitorNavegacao;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.dispositivos.ProvedorGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.DefinicaoPersonagem;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.personagens.LimitesAtributos;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Entrega 5M: selecao de personagens em grade para JOGAR (2 jogadores).
 *
 * Esquerda J1, centro o elenco (5 x 3), direita J2, embaixo MAPA / TEMPO /
 * DIFICULDADE e LUTAR. Cada jogador tem o SEU cursor e confirma sozinho
 * (personagens iguais permitidos); nada de foco compartilhado:
 *
 * - teclado J1: W A S D mover · ESPACO (ou H) confirmar · Q cancelar;
 * - teclado J2: setas mover · ENTER (ou END) confirmar · BACKSPACE cancelar;
 * - gamepad: o slot associado a GAMEPAD em CONFIGURACOES recebe os gamepads
 *   conectados, na ordem (J1 primeiro): D-pad/analogico, A confirma, B cancela;
 * - ESC volta ao menu principal.
 *
 * Fases de cada jogador: ESCOLHENDO -> (PERFIL, se houver perfis do irmao) ->
 * PRONTO. Pronto, o J1 ajusta a partida e confirma LUTAR (o J2 precisa estar
 * pronto). Cancelar volta uma fase; cancelar escolhendo volta ao menu.
 *
 * Os espacos bloqueados sao so visuais: nao ha personagens cadastrados para
 * eles, nao revelam nada e nao podem ser confirmados.
 */
public final class SelecaoPartidaPanel extends JPanel implements TelaNavegavel {

    private static final long serialVersionUID = 1L;

    /** O que fazer quando os dois estao prontos e o J1 confirma LUTAR. */
    @FunctionalInterface
    public interface AoIniciar {
        void iniciar(EscolhaLutador j1, EscolhaLutador j2, String idCenario, int segundosLimite);
    }

    public static final int COLUNAS = 5;
    public static final int LINHAS = 3;
    static final int CELULA = 56;
    static final int ESPACO = 8;
    static final int GRADE_X = 244;
    static final int GRADE_Y = 64;

    /** Tempos de luta oferecidos (0 = sem limite). */
    public static final int[] TEMPOS = {0, 60, 90, 180};

    /** Fase de um jogador. */
    public enum Fase { ESCOLHENDO, PERFIL, PRONTO }

    /** Linhas de configuracao (o J1 navega quando esta pronto). */
    enum Linha { MAPA, TEMPO, DIFICULDADE, LUTAR }

    private static final class Jogador {
        int cursor;
        Fase fase = Fase.ESCOLHENDO;
        List<EscolhaLutador> perfis = List.of();
        int perfil;
        long avisoBloqueadoAte = -1;
        IrmaoMiau previa;
        String idPrevia;
    }

    private final transient List<DefinicaoPersonagem> elenco = new ArrayList<>();
    private final transient OpcoesDeLutadores opcoes;
    private final transient AoIniciar aoIniciar;
    private final transient Runnable aoVoltar;
    private final transient Supplier<ProvedorGamepads> provedor;
    private final transient Supplier<AssociacaoDispositivos> associacao;
    private final transient Map<SlotJogador, Jogador> jogadores = new EnumMap<>(SlotJogador.class);
    private final transient Map<String, LeitorNavegacao> leitores = new HashMap<>();
    private final transient Set<String> segurando = new HashSet<>();
    private final List<String> mapas;
    private final List<String> nomesMapas = new ArrayList<>();
    private int mapa;
    private int tempo;
    private Linha linhaJ1 = Linha.LUTAR;
    private long tempoMs;
    private long avisoAguardandoAte = -1;
    private boolean iniciada;
    private final Timer timer = new Timer(16, e -> tick(16));

    public SelecaoPartidaPanel(CatalogoPersonagens catalogo, OpcoesDeLutadores opcoes, RegistroCenarios cenarios,
            String idCenarioInicial, AoIniciar aoIniciar, Runnable aoVoltar,
            Supplier<ProvedorGamepads> provedor, Supplier<AssociacaoDispositivos> associacao) {
        if (catalogo == null || opcoes == null || cenarios == null || aoIniciar == null) {
            throw new IllegalArgumentException("Catalogo, opcoes, cenarios e acao de inicio sao obrigatorios.");
        }
        this.opcoes = opcoes;
        this.aoIniciar = aoIniciar;
        this.aoVoltar = aoVoltar;
        this.provedor = provedor;
        this.associacao = associacao;
        elenco.addAll(catalogo.listar());
        mapas = List.copyOf(cenarios.ids());
        for (String id : mapas) {
            nomesMapas.add(cenarios.exigir(id).getNome().toUpperCase(Locale.ROOT));
        }
        mapa = Math.max(0, mapas.indexOf(idCenarioInicial));
        for (SlotJogador s : SlotJogador.values()) {
            Jogador j = new Jogador();
            j.cursor = Math.min(s.ordinal(), elenco.size() - 1);    // J1 no 1o, J2 no 2o
            jogadores.put(s, j);
        }
        setPreferredSize(new Dimension(800, 450));
        setFocusable(true);
        instalarTeclas();
    }

    // =====================================================
    // ENTRADAS
    // =====================================================
    private void instalarTeclas() {
        teclas(SlotJogador.JOGADOR_1, Comando.ANTERIOR, "W");
        teclas(SlotJogador.JOGADOR_1, Comando.PROXIMO, "S");
        teclas(SlotJogador.JOGADOR_1, Comando.ESQUERDA, "A");
        teclas(SlotJogador.JOGADOR_1, Comando.DIREITA, "D");
        teclas(SlotJogador.JOGADOR_1, Comando.CONFIRMAR, "SPACE", "H");
        teclas(SlotJogador.JOGADOR_1, Comando.VOLTAR, "Q");
        teclas(SlotJogador.JOGADOR_2, Comando.ANTERIOR, "UP");
        teclas(SlotJogador.JOGADOR_2, Comando.PROXIMO, "DOWN");
        teclas(SlotJogador.JOGADOR_2, Comando.ESQUERDA, "LEFT");
        teclas(SlotJogador.JOGADOR_2, Comando.DIREITA, "RIGHT");
        teclas(SlotJogador.JOGADOR_2, Comando.CONFIRMAR, "ENTER", "END");
        teclas(SlotJogador.JOGADOR_2, Comando.VOLTAR, "BACK_SPACE");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed ESCAPE"), "voltarMenu");
        getActionMap().put("voltarMenu", acao(() -> voltarAoMenu()));
    }

    /** Cada tecla age uma vez por aperto (segurar nao repete; o auto-repeat do sistema e ignorado). */
    private void teclas(SlotJogador slot, Comando c, String... nomes) {
        for (String n : nomes) {
            String id = slot.name() + "." + n;
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + n), id + ".p");
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released " + n), id + ".s");
            getActionMap().put(id + ".p", acao(() -> {
                if (segurando.add(id)) {
                    comandar(slot, c);
                }
            }));
            getActionMap().put(id + ".s", acao(() -> segurando.remove(id)));
        }
    }

    private static AbstractAction acao(Runnable r) {
        return new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        };
    }

    private void lerGamepads() {
        ProvedorGamepads p = provedor == null ? null : provedor.get();
        AssociacaoDispositivos a = associacao == null ? null : associacao.get();
        if (p == null || a == null) {
            return;
        }
        List<SlotJogador> comGamepad = new ArrayList<>();
        for (SlotJogador s : SlotJogador.values()) {
            if (a.tipo(s) == TipoDispositivo.GAMEPAD) {
                comGamepad.add(s);
            }
        }
        List<InfoGamepad> conectados = p.conectados();
        for (int i = 0; i < conectados.size() && i < comGamepad.size(); i++) {
            InfoGamepad g = conectados.get(i);
            EstadoGamepad e = p.estado(g.chave()).orElse(null);
            LeitorNavegacao l = leitores.computeIfAbsent(g.chave(), k -> new LeitorNavegacao());
            for (Comando c : l.ler(e, tempoMs)) {
                comandar(comGamepad.get(i), c);
            }
        }
    }

    // =====================================================
    // REGRAS DA TELA
    // =====================================================
    /** Um comando de um jogador (teclado, gamepad ou testes). */
    void comandar(SlotJogador slot, Comando c) {
        if (iniciada) {
            return;
        }
        Jogador j = jogadores.get(slot);
        switch (j.fase) {
            case ESCOLHENDO -> escolhendo(slot, j, c);
            case PERFIL -> perfil(j, c);
            default -> pronto(slot, j, c);
        }
        repaint();
    }

    private void escolhendo(SlotJogador slot, Jogador j, Comando c) {
        int total = COLUNAS * LINHAS;
        switch (c) {
            case ANTERIOR -> j.cursor = j.cursor - COLUNAS >= 0 ? j.cursor - COLUNAS : j.cursor;
            case PROXIMO -> j.cursor = j.cursor + COLUNAS < total ? j.cursor + COLUNAS : j.cursor;
            case ESQUERDA -> j.cursor = j.cursor % COLUNAS == 0 ? j.cursor + COLUNAS - 1 : j.cursor - 1;
            case DIREITA -> j.cursor = j.cursor % COLUNAS == COLUNAS - 1 ? j.cursor - COLUNAS + 1 : j.cursor + 1;
            case CONFIRMAR -> {
                DefinicaoPersonagem d = personagemEm(j.cursor);
                if (d == null) {
                    j.avisoBloqueadoAte = tempoMs + 700;          // bloqueado: so um aviso
                    return;
                }
                j.perfis = opcoes.paraBase(d.getId()).getValor();
                j.perfil = 0;
                j.fase = j.perfis.size() > 1 ? Fase.PERFIL : Fase.PRONTO;
                if (j.fase == Fase.PRONTO && slot == SlotJogador.JOGADOR_1) {
                    linhaJ1 = Linha.LUTAR;
                }
            }
            case VOLTAR -> voltarAoMenu();
            default -> {
            }
        }
    }

    private void perfil(Jogador j, Comando c) {
        switch (c) {
            case ESQUERDA -> j.perfil = Math.floorMod(j.perfil - 1, j.perfis.size());
            case DIREITA -> j.perfil = Math.floorMod(j.perfil + 1, j.perfis.size());
            case CONFIRMAR -> {
                j.fase = Fase.PRONTO;
                if (j == jogadores.get(SlotJogador.JOGADOR_1)) {
                    linhaJ1 = Linha.LUTAR;
                }
            }
            case VOLTAR -> j.fase = Fase.ESCOLHENDO;
            default -> {
            }
        }
    }

    private void pronto(SlotJogador slot, Jogador j, Comando c) {
        if (c == Comando.VOLTAR) {
            j.fase = Fase.ESCOLHENDO;
            return;
        }
        if (slot != SlotJogador.JOGADOR_1) {
            return;                                             // a partida e configurada pelo J1
        }
        switch (c) {
            case ANTERIOR -> linhaJ1 = linhaVizinha(-1);
            case PROXIMO -> linhaJ1 = linhaVizinha(1);
            case ESQUERDA -> mudarValor(-1);
            case DIREITA -> mudarValor(1);
            case CONFIRMAR -> {
                if (linhaJ1 != Linha.LUTAR) {
                    linhaJ1 = Linha.LUTAR;
                } else if (jogadores.get(SlotJogador.JOGADOR_2).fase != Fase.PRONTO) {
                    avisoAguardandoAte = tempoMs + 900;
                } else {
                    iniciar();
                }
            }
            default -> {
            }
        }
    }

    /** DIFICULDADE e pulada: indisponivel enquanto nao houver IA. */
    private Linha linhaVizinha(int passo) {
        Linha[] ordem = {Linha.MAPA, Linha.TEMPO, Linha.LUTAR};
        int i = java.util.Arrays.asList(ordem).indexOf(linhaJ1);
        return ordem[Math.max(0, Math.min(ordem.length - 1, i + passo))];
    }

    private void mudarValor(int passo) {
        if (linhaJ1 == Linha.MAPA && mapas.size() > 1) {
            mapa = Math.floorMod(mapa + passo, mapas.size());
        } else if (linhaJ1 == Linha.TEMPO) {
            tempo = Math.floorMod(tempo + passo, TEMPOS.length);
        }
    }

    private void iniciar() {
        if (iniciada) {
            return;
        }
        iniciada = true;
        timer.stop();
        aoIniciar.iniciar(escolha(SlotJogador.JOGADOR_1), escolha(SlotJogador.JOGADOR_2), mapas.get(mapa),
                TEMPOS[tempo]);
    }

    private void voltarAoMenu() {
        if (iniciada) {
            return;
        }
        iniciada = true;
        timer.stop();
        if (aoVoltar != null) {
            aoVoltar.run();
        }
    }

    /** Escolha confirmada (ou em escolha) do jogador; null num espaco bloqueado. */
    EscolhaLutador escolha(SlotJogador slot) {
        Jogador j = jogadores.get(slot);
        if (j.fase != Fase.ESCOLHENDO && !j.perfis.isEmpty()) {
            return j.perfis.get(j.perfil);
        }
        DefinicaoPersonagem d = personagemEm(j.cursor);
        return d == null ? null : EscolhaLutador.oficial(d);
    }

    private DefinicaoPersonagem personagemEm(int i) {
        return i >= 0 && i < elenco.size() ? elenco.get(i) : null;
    }

    // =====================================================
    // TEMPO / CICLO DE VIDA
    // =====================================================
    @Override
    public boolean aceitaNavegacao() {
        return false;                                           // a tela le os dois jogadores sozinha
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
        requestFocusInWindow();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    void tick(long ms) {
        tempoMs += ms;
        lerGamepads();
        for (SlotJogador s : SlotJogador.values()) {
            atualizarPrevia(s);
        }
        repaint();
    }

    /** Previa grande: o lutador oficial (ou o perfil) em idle, recriado so quando muda. */
    private void atualizarPrevia(SlotJogador s) {
        Jogador j = jogadores.get(s);
        EscolhaLutador e = escolha(s);
        String id = e == null ? null : e.getId();
        if (id == null) {
            j.previa = null;
            j.idPrevia = null;
            return;
        }
        if (!id.equals(j.idPrevia)) {
            boolean esq = s == SlotJogador.JOGADOR_1;
            j.previa = e.criarLutador(esq ? 88 : 648, 108, 0.7);
            j.previa.setViradoParaDireita(esq);
            j.idPrevia = id;
        }
        j.previa.atualizar(false, false);
    }

    // =====================================================
    // DESENHO
    // =====================================================
    static Rectangle celula(int i) {
        int c = i % COLUNAS;
        int l = i / COLUNAS;
        return new Rectangle(GRADE_X + c * (CELULA + ESPACO), GRADE_Y + l * (CELULA + ESPACO), CELULA, CELULA);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = ArteMenus.suave(g0);
        g.setPaint(new GradientPaint(0, 0, new Color(40, 110, 200), 0, 450, ArteMenus.AZUL_ESCURO));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(255, 255, 255, 18));
        for (int x = -450; x < 800; x += 36) {
            g.fillPolygon(new int[] {x, x + 18, x + 468, x + 450}, new int[] {0, 0, 450, 450}, 4);
        }
        ArteMenus.letreiro(g, "ESCOLHA SEU LUTADOR", 400, 44, new Font("Arial", Font.BOLD, 26), ArteMenus.AMARELO);

        painelJogador(g, SlotJogador.JOGADOR_1, 10);
        painelJogador(g, SlotJogador.JOGADOR_2, 570);
        grade(g);
        configuracoes(g);
        g.dispose();
    }

    private void grade(Graphics2D g) {
        for (int i = 0; i < COLUNAS * LINHAS; i++) {
            Rectangle r = celula(i);
            DefinicaoPersonagem d = personagemEm(i);
            g.setColor(d == null ? new Color(14, 26, 60) : new Color(70, 140, 225));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            if (d == null) {
                silhueta(g, r);
                cadeado(g, r.x + r.width - 18, r.y + r.height - 20);
            } else {
                retrato(g, d, r);
            }
            g.setColor(new Color(160, 200, 255, 120));
            g.setStroke(new BasicStroke(1));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        }
        // cursores: J1 por fora (azul), J2 por dentro (vermelho); juntos continuam distintos
        Rectangle r1 = celula(jogadores.get(SlotJogador.JOGADOR_1).cursor);
        Rectangle r2 = celula(jogadores.get(SlotJogador.JOGADOR_2).cursor);
        cursor(g, r1, -4, ArteMenus.J1, "1", true, jogadores.get(SlotJogador.JOGADOR_1).fase);
        cursor(g, r2, 2, ArteMenus.J2, "2", false, jogadores.get(SlotJogador.JOGADOR_2).fase);
    }

    private void cursor(Graphics2D g, Rectangle r, int d, Color cor, String rotulo, boolean esquerda, Fase fase) {
        g.setColor(cor);
        g.setStroke(new BasicStroke(fase == Fase.ESCOLHENDO ? 3 : 4));
        g.drawRoundRect(r.x + d, r.y + d, r.width - 2 * d, r.height - 2 * d, 8, 8);
        int tx = esquerda ? r.x - 6 : r.x + r.width - 10;
        g.fillOval(tx, r.y - 8, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.drawString(rotulo, tx + 5, r.y + 4);
    }

    private static void silhueta(Graphics2D g, Rectangle r) {
        g.setColor(new Color(40, 54, 90));
        int cx = r.x + r.width / 2;
        g.fillOval(cx - 12, r.y + 10, 24, 22);
        g.fillPolygon(new int[] {cx - 12, cx - 6, cx - 11}, new int[] {r.y + 16, r.y + 12, r.y + 4}, 3);
        g.fillPolygon(new int[] {cx + 12, cx + 6, cx + 11}, new int[] {r.y + 16, r.y + 12, r.y + 4}, 3);
        g.fillRoundRect(cx - 16, r.y + 32, 32, 24, 8, 8);
        g.setColor(new Color(110, 130, 170));
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("?", cx - 4, r.y + 27);
    }

    private static void cadeado(Graphics2D g, int x, int y) {
        g.setColor(new Color(220, 190, 80));
        g.setStroke(new BasicStroke(2.2f));
        g.drawArc(x + 2, y - 1, 10, 12, 0, 180);
        g.fillRoundRect(x, y + 5, 14, 11, 3, 3);
        g.setColor(new Color(90, 70, 20));
        g.fillOval(x + 5, y + 8, 4, 4);
    }

    /** Retrato: cabeca e tronco do proprio renderizador, recortados na celula. */
    private final transient Map<String, IrmaoMiau> retratos = new HashMap<>();

    private void retrato(Graphics2D g0, DefinicaoPersonagem d, Rectangle r) {
        IrmaoMiau l = retratos.computeIfAbsent(d.getId() + "@" + r.x + "," + r.y,
                k -> EscolhaLutador.oficial(d).criarLutador(r.x + r.width / 2 - 25, r.y + 2, 0.62));
        l.setViradoParaDireita(true);
        Graphics2D g = (Graphics2D) g0.create();
        g.clip(new java.awt.geom.RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 8, 8));
        ArteMenus.desenharLutador(g, l);
        g.dispose();
    }

    private void painelJogador(Graphics2D g, SlotJogador s, int x) {
        Jogador j = jogadores.get(s);
        Color cor = s == SlotJogador.JOGADOR_1 ? ArteMenus.J1 : ArteMenus.J2;
        g.setColor(ArteMenus.AZUL_PAINEL);
        g.fillRoundRect(x, 56, 220, 386, 14, 14);
        g.setColor(cor);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(x, 56, 220, 386, 14, 14);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString(s == SlotJogador.JOGADOR_1 ? "J1" : "J2", x + 10, 80);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(j.fase == Fase.PRONTO ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        String fase = j.fase == Fase.PRONTO ? "PRONTO!" : j.fase == Fase.PERFIL ? "ESCOLHA O PERFIL" : "ESCOLHENDO";
        g.drawString(fase, x + 210 - g.getFontMetrics().stringWidth(fase), 78);

        // previa
        g.setColor(new Color(255, 255, 255, 30));
        g.fillOval(x + 40, 268, 140, 18);
        DefinicaoPersonagem d = personagemEm(j.cursor);
        if (j.previa != null) {
            ArteMenus.desenharLutador(g, j.previa);
        } else {
            Rectangle grande = new Rectangle(x + 60, 112, 100, 100);
            g.setColor(new Color(10, 22, 52));
            g.fillRoundRect(grande.x, grande.y, grande.width, grande.height + 60, 16, 16);
            g.setColor(new Color(40, 54, 90));
            g.fillOval(x + 85, 128, 50, 46);
            g.fillRoundRect(x + 78, 176, 64, 70, 14, 14);
            cadeado(g, x + 102, 246);
        }
        if (j.avisoBloqueadoAte > tempoMs) {
            g.setColor(new Color(200, 40, 40, 220));
            g.fillRoundRect(x + 30, 190, 160, 24, 10, 10);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            ArteMenus.centralizado(g, "BLOQUEADO", x + 110, 207);
        }

        // nome, estilo, perfil e atributos (dados reais do catalogo/perfil)
        EscolhaLutador e = escolha(s);
        g.setColor(ArteMenus.BRANCO);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        ArteMenus.centralizado(g, d == null ? "???" : nomeExibido(d), x + 110, 304);
        g.setFont(ArteMenus.PEQUENO);
        ArteMenus.centralizado(g, d == null ? "Em breve" : "Estilo: " + d.getEstilo(), x + 110, 319);
        if (e != null && j.perfis.size() > 1 && j.fase != Fase.ESCOLHENDO) {
            g.setColor(j.fase == Fase.PERFIL ? ArteMenus.AMARELO : ArteMenus.BRANCO);
            ArteMenus.centralizado(g, (j.fase == Fase.PERFIL ? "< " : "") + "Perfil: " + e.getNome()
                    + (j.fase == Fase.PERFIL ? " >" : ""), x + 110, 333);
        }
        if (e != null) {
            atributos(g, e.getAtributos(), x + 12, 344);
        }
    }

    /** "Irmão Miau Branco" -> "MIAU BRANCO". */
    static String nomeExibido(DefinicaoPersonagem d) {
        return d.getNome().replaceFirst("^Irmão ", "").toUpperCase(Locale.ROOT);
    }

    private static void atributos(Graphics2D g, AtributosLutador a, int x, int y) {
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        int i = 0;
        for (LimitesAtributos.Limite l : LimitesAtributos.todos()) {
            double v = l.ler(a);
            int yy = y + i * 12;
            g.setColor(ArteMenus.BRANCO);
            g.drawString(PerfisPanel.nomeVisivel(l.getNome()), x, yy + 9);
            g.setColor(new Color(10, 22, 52));
            g.fillRect(x + 92, yy + 2, 80, 7);
            double frac = (v - l.getMinimo()) / Math.max(1e-9, l.getMaximo() - l.getMinimo());
            g.setColor(ArteMenus.AMARELO);
            g.fillRect(x + 92, yy + 2, (int) Math.round(80 * Math.max(0, Math.min(1, frac))), 7);
            g.setColor(ArteMenus.BRANCO);
            g.drawString(v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v), x + 178, yy + 9);
            i++;
        }
    }

    private void configuracoes(Graphics2D g) {
        boolean j1Pronto = jogadores.get(SlotJogador.JOGADOR_1).fase == Fase.PRONTO;
        g.setColor(ArteMenus.AZUL_PAINEL);
        g.fillRoundRect(240, 258, 320, 184, 14, 14);
        linha(g, Linha.MAPA, "MAPA", nomesMapas.get(mapa), 284, mapas.size() > 1, j1Pronto);
        linha(g, Linha.TEMPO, "TEMPO", textoTempo(TEMPOS[tempo]), 316, true, j1Pronto);
        linha(g, Linha.DIFICULDADE, "DIFICULDADE", "EM BREVE", 348, false, j1Pronto);

        boolean focoLutar = j1Pronto && linhaJ1 == Linha.LUTAR;
        boolean prontos = j1Pronto && jogadores.get(SlotJogador.JOGADOR_2).fase == Fase.PRONTO;
        g.setColor(focoLutar ? new Color(255, 206, 46, 235) : new Color(30, 70, 150));
        g.fillRoundRect(320, 364, 160, 34, 12, 12);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(focoLutar ? ArteMenus.AZUL_ESCURO : new Color(170, 190, 230));
        ArteMenus.centralizado(g, "LUTAR!", 400, 388);

        g.setFont(ArteMenus.PEQUENO);
        g.setColor(ArteMenus.BRANCO);
        String dica;
        if (avisoAguardandoAte > tempoMs) {
            g.setColor(ArteMenus.AMARELO);
            dica = "AGUARDANDO O J2 FICAR PRONTO";
        } else if (prontos) {
            dica = "J1: confirme LUTAR! para começar";
        } else if (j1Pronto) {
            dica = "J1 ajusta a partida · aguardando o J2";
        } else {
            dica = "Confirme seu lutador · cancelar volta";
        }
        ArteMenus.centralizado(g, dica, 400, 416);
        g.setColor(new Color(190, 205, 235));
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        ArteMenus.centralizado(g, "J1 WASD·ESPAÇO·Q   J2 setas·ENTER·BACKSPACE   ESC menu", 400, 433);
    }

    /** Triangulo de seta (as fontes padrao nem sempre tem os caracteres de seta). */
    private static void seta(Graphics2D g, int cx, int cy, int lado) {
        g.fillPolygon(new int[] {cx + 5 * lado, cx - 5 * lado, cx - 5 * lado}, new int[] {cy, cy - 6, cy + 6}, 3);
    }

    static String textoTempo(int s) {
        return s == 0 ? "SEM LIMITE" : s + " s";
    }

    private void linha(Graphics2D g, Linha l, String rotulo, String valor, int y, boolean setas, boolean j1Pronto) {
        boolean foco = j1Pronto && linhaJ1 == l;
        if (foco) {
            g.setColor(new Color(255, 206, 46, 60));
            g.fillRoundRect(250, y - 18, 300, 26, 8, 8);
        }
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(foco ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        g.drawString(rotulo, 262, y);
        boolean habilitado = l != Linha.DIFICULDADE;
        g.setColor(!habilitado ? new Color(140, 150, 175) : foco ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        ArteMenus.centralizado(g, valor, 462, y);
        g.setColor(setas && habilitado ? (foco ? ArteMenus.AMARELO : ArteMenus.BRANCO) : new Color(90, 105, 140));
        seta(g, 392, y - 5, -1);
        seta(g, 532, y - 5, 1);
    }

    // =====================================================
    // ACESSO PARA TESTES
    // =====================================================
    Fase fase(SlotJogador s) {
        return jogadores.get(s).fase;
    }

    int cursor(SlotJogador s) {
        return jogadores.get(s).cursor;
    }

    String mapaAtual() {
        return mapas.get(mapa);
    }

    int tempoAtual() {
        return TEMPOS[tempo];
    }

    boolean mapaTemOpcoes() {
        return mapas.size() > 1;
    }

    Linha linhaJ1() {
        return linhaJ1;
    }

    boolean isAvisoBloqueado(SlotJogador s) {
        return jogadores.get(s).avisoBloqueadoAte > tempoMs;
    }

    IrmaoMiau previa(SlotJogador s) {
        return jogadores.get(s).previa;
    }

    /** Aciona uma tecla de verdade (pressed/released) pelos mapas de entrada. */
    void tecla(String nome, boolean pressionada) {
        Object id = getInputMap(WHEN_IN_FOCUSED_WINDOW).get(
                KeyStroke.getKeyStroke((pressionada ? "pressed " : "released ") + nome));
        getActionMap().get(id).actionPerformed(null);
    }
}
