package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.audio.SaidaAudio;
import com.irmaosmiau.audio.ServicoAudio;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioConfiguracoes;
import com.irmaosmiau.persistencia.RepositorioControles;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasJogo;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5M.3: poses fixas nos menus, selecao nova no TREINO e tipografia comum. */
class Menus5M3Test {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;

    private SelecaoPartidaPanel sel;
    private final AtomicReference<Object[]> treinou = new AtomicReference<>();
    private final AtomicInteger inicios = new AtomicInteger();
    private final AtomicInteger voltas = new AtomicInteger();

    @AfterEach
    void limparSessao() {
        SelecaoLutadoresPanel.reiniciarInicioDaSessao();
    }

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private ServicoPerfis servico() {
        return new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao());
    }

    private SelecaoPartidaPanel treino(InicioLuta inicio, ProvedorGamepadSimulado pads, AssociacaoDispositivos a) {
        return SelecaoPartidaPanel.treino(CatalogoPersonagens.padrao(),
                new OpcoesDeLutadores(CatalogoPersonagens.padrao(), servico()), RegistroCenarios.padrao(),
                RegistroCenarios.ID_PADRAO, inicio,
                (j, p, c, i) -> {
                    inicios.incrementAndGet();
                    treinou.set(new Object[]{j, p, c, i});
                },
                voltas::incrementAndGet, () -> pads, () -> a);
    }

    private SelecaoPartidaPanel treino() {
        return treino(InicioLuta.DE_JOELHOS, null, null);
    }

    private static void dispor(Component c) {
        if (c instanceof Container k) {
            k.doLayout();
            for (Component f : k.getComponents()) {
                dispor(f);
            }
        }
    }

    private static void pintar(JComponent c) {
        c.setSize(800, 450);
        dispor(c);
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        c.paint(g);
        g.dispose();
    }

    // =====================================================
    // POSES DE APRESENTACAO
    // =====================================================
    // 1. Abertura com pose estatica
    @Test
    void aberturaPoseEstatica() throws Exception {
        naEdt(() -> {
            AberturaPanel a = new AberturaPanel(() -> { }, () -> null);
            for (int i = 0; i < 300; i++) {
                a.tick(16);
            }
            for (IrmaoMiau l : new IrmaoMiau[] {a.getBranco(), a.getPreto()}) {
                assertTrue(l.isPoseDeApresentacao());
                assertFalse(l.estaEmTaunt());
                assertFalse(l.estaEmIdleDeCombate());
            }
        });
    }

    // 2. Menu principal sem taunt nem idle de combate
    @Test
    void menuSemTauntNemIdle() throws Exception {
        naEdt(() -> {
            MenuPrincipalPanel m = new MenuPrincipalPanel(() -> { }, () -> { }, () -> { }, () -> { }, () -> { });
            for (int i = 0; i < 300; i++) {
                m.tick();
            }
            for (IrmaoMiau l : m.lutadores()) {
                assertTrue(l.isPoseDeApresentacao());
                assertFalse(l.estaEmTaunt());
                assertFalse(l.estaEmIdleDeCombate());
            }
        });
    }

    // 3. Selecao de luta e de treino com poses estaticas (previas)
    @Test
    void selecoesComPosesEstaticas() throws Exception {
        naEdt(() -> {
            for (SelecaoPartidaPanel s : new SelecaoPartidaPanel[] {
                new SelecaoPartidaPanel(CatalogoPersonagens.padrao(),
                        new OpcoesDeLutadores(CatalogoPersonagens.padrao(), servico()), RegistroCenarios.padrao(),
                        RegistroCenarios.ID_PADRAO, (a, b, c, d) -> { }, () -> { }, () -> null, () -> null),
                treino()}) {
                for (int i = 0; i < 300; i++) {
                    s.tick(16);
                }
                for (SlotJogador slot : SlotJogador.values()) {
                    IrmaoMiau l = s.previa(slot);
                    assertNotNull(l);
                    assertTrue(l.isPoseDeApresentacao());
                    assertFalse(l.estaEmTaunt());
                    assertFalse(l.estaEmIdleDeCombate());
                }
                pintar(s);
            }
        });
    }

    // =====================================================
    // SELECAO DO TREINO
    // =====================================================
    // 7. A selecao do treino e a tela nova, em modo TREINO
    @Test
    void treinoUsaASelecaoNova() throws Exception {
        naEdt(() -> {
            sel = treino();
            assertEquals(SelecaoPartidaPanel.Modo.TREINO, sel.modo());
            assertFalse(sel.aceitaNavegacao());
            pintar(sel);
        });
    }

    // 8. Um dispositivo so: os comandos do J2 (setas / segundo gamepad) movem o lutador da vez
    @Test
    void soOJ1Escolhe() throws Exception {
        naEdt(() -> {
            sel = treino();
            sel.comandar(J2, Comando.DIREITA);
            assertEquals(1, sel.cursor(J1));
            sel.tecla("LEFT", true);
            assertEquals(0, sel.cursor(J1), "setas tambem guiam o J1 no treino");
            sel.tecla("D", true);
            assertEquals(1, sel.cursor(J1));
        });
    }

    // 9. O J2 nao precisa confirmar (pedido): o mesmo controle confirma os dois e inicia uma vez so
    @Test
    void j2NaoPrecisaConfirmar() throws Exception {
        naEdt(() -> {
            sel = treino();
            for (int i = 0; i < 14; i++) {
                sel.comandar(J1, Comando.CONFIRMAR);
            }
        });
        assertEquals(1, inicios.get());
    }

    // 10. Elenco e espacos bloqueados no treino
    @Test
    void bloqueadosNoTreino() throws Exception {
        naEdt(() -> {
            sel = treino();
            sel.comandar(J1, Comando.PROXIMO);
            assertNull(sel.escolha(J1));
            sel.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, sel.fase(J1));
            assertTrue(sel.isAvisoBloqueado(J1));
        });
        assertEquals(0, inicios.get());
    }

    // 11. Perfis (pedido): o X / Quadrado abre os perfis do lutador e depois os do parceiro
    @Test
    void perfisNoTreino() throws Exception {
        servico().criarAPartirDoPersonagem("branquinho", "Branquinho", CatalogoPersonagens.ID_BRANCO);
        servico().criarAPartirDoPersonagem("pretinho", "Pretinho", CatalogoPersonagens.ID_PRETO);
        naEdt(() -> {
            sel = treino();
            sel.comandar(J1, Comando.PERFIS);
            assertEquals(SelecaoPartidaPanel.Fase.PERFIL, sel.fase(J1));
            sel.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(J1));
            assertEquals("Branquinho", sel.escolha(J1).getNome());
            assertEquals("Irmão Miau Preto", sel.escolha(J2).getNome(), "parceiro padrao: o outro irmao oficial");
            sel.comandar(J1, Comando.PERFIS);                 // agora vale para o parceiro
            sel.comandar(J1, Comando.CONFIRMAR);
            assertEquals("Pretinho", sel.escolha(J2).getNome());
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(J2));
        });
    }

    // 12. TREINAR entrega jogador, parceiro, mapa e inicio
    @Test
    void treinoInicia() throws Exception {
        naEdt(() -> {
            sel = treino();
            sel.comandar(J1, Comando.DIREITA);                // Preto
            personalizacao(sel);                              // treino igual ao JOGAR (pedido): tela da arena
            sel.comandar(J1, Comando.DIREITA);                // ARENA seguinte
            sel.comandar(J1, Comando.CONFIRMAR);               // INICIO
            sel.comandar(J1, Comando.CONFIRMAR);               // INICIAR TREINO
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.CONFIRMAR);               // nao inicia duas vezes
        });
        assertEquals(1, inicios.get());
        Object[] t = treinou.get();
        assertEquals(CatalogoPersonagens.ID_PRETO, ((EscolhaLutador) t[0]).getIdBase());
        assertEquals(CatalogoPersonagens.ID_BRANCO, ((EscolhaLutador) t[1]).getIdBase(),
                "treinar com o Preto deixa o Branco de parceiro");
        assertEquals("original", t[2]);
        assertEquals(InicioLuta.DE_JOELHOS, t[3]);
    }

    // 13. Regras antigas do treino (pedido: o inicio fica na tela da arena): inicio lembrado, sem tempo
    @Test
    void regrasAntigasDoTreino() throws Exception {
        naEdt(() -> {
            sel = treino(InicioLuta.EM_PE, null, null);
            assertEquals(InicioLuta.EM_PE, sel.inicioAtual(), "comeca com o inicio da sessao");
            personalizacao(sel);
            sel.comandar(J1, Comando.PROXIMO);                // INICIO
            assertEquals(ConfiguracaoLuta.Area.INICIO, sel.configuracao().foco());
            sel.comandar(J1, Comando.DIREITA);
            assertEquals(InicioLuta.values()[(InicioLuta.EM_PE.ordinal() + 1) % InicioLuta.values().length],
                    sel.inicioAtual());
            for (int i = 0; i < 6; i++) {
                sel.comandar(J1, i < 3 ? Comando.ANTERIOR : Comando.PROXIMO);
                ConfiguracaoLuta.Area a = sel.configuracao().foco();
                assertFalse(a == ConfiguracaoLuta.Area.TEMPO || a == ConfiguracaoLuta.Area.REGRAS
                        || a == ConfiguracaoLuta.Area.DIFICULDADE, "treino sem tempo, dificuldade e regras");
            }
            assertEquals(0, sel.tempoAtual(), "treino sem limite");
            for (int i = 0; i < 10 && voltas.get() == 0; i++) {
                sel.comandar(J1, Comando.VOLTAR);             // arena -> parceiro -> lutador -> menu
            }
        });
        assertEquals(1, voltas.get());
        SelecaoLutadoresPanel.lembrarInicioDaSessao(InicioLuta.EM_PE);
        assertEquals(InicioLuta.EM_PE, SelecaoLutadoresPanel.inicioDaSessao());
        SelecaoLutadoresPanel.lembrarInicioDaSessao(null);
        assertEquals(InicioLuta.EM_PE, SelecaoLutadoresPanel.inicioDaSessao(), "null nao apaga");
    }

    // 13b. Gamepad no treino: qualquer controle guia o J1
    @Test
    void gamepadNoTreino() throws Exception {
        ProvedorGamepadSimulado p = new ProvedorGamepadSimulado();
        p.conectar("g1", "id1", "Xbox");
        naEdt(() -> {
            sel = treino(InicioLuta.DE_JOELHOS, p, AssociacaoDispositivos.padrao());
            sel.tick(16);
            p.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.DPAD_DIREITA));
            sel.tick(16);
            assertEquals(1, sel.cursor(J1));
            p.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL));
            sel.tick(16);
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(J1), "(pedido) original: faixa e kimono");
        });
    }

    // 14. A selecao de dois jogadores continua igual
    @Test
    void partidaSemRegressao() throws Exception {
        AtomicInteger lutas = new AtomicInteger();
        naEdt(() -> {
            sel = new SelecaoPartidaPanel(CatalogoPersonagens.padrao(),
                    new OpcoesDeLutadores(CatalogoPersonagens.padrao(), servico()), RegistroCenarios.padrao(),
                    RegistroCenarios.ID_PADRAO, (a, b, c, d) -> lutas.incrementAndGet(), () -> { }, () -> null,
                    () -> null);
            assertEquals(SelecaoPartidaPanel.Modo.PARTIDA, sel.modo());
            sel.comandar(J2, Comando.ESQUERDA);
            assertEquals(0, sel.cursor(J2));
            assertEquals(0, sel.cursor(J1), "J2 tem o seu cursor na partida");
            // reorganizacao dos menus (pedido): personagem, faixa e kimono; AVANCAR; INICIAR LUTA
            for (int i = 0; i < 3; i++) {
                sel.comandar(J1, Comando.CONFIRMAR);
            }
            sel.comandar(J1, Comando.CONFIRMAR);              // AVANCAR sem o J2
            assertEquals(0, lutas.get(), "na partida o J2 precisa estar pronto");
            assertFalse(sel.naConfiguracao());
            for (int i = 0; i < 3; i++) {
                sel.comandar(J2, Comando.CONFIRMAR);
            }
            sel.comandar(J1, Comando.CONFIRMAR);              // AVANCAR
            for (int i = 0; i < 4; i++) {
                sel.comandar(J1, Comando.CONFIRMAR);          // ARENA -> TEMPO -> REGRAS -> INICIAR LUTA
            }
        });
        assertEquals(1, lutas.get());
    }

    // =====================================================
    // TIPOGRAFIA
    // =====================================================
    private ConfiguracoesPanel configuracoes() {
        PreferenciasJogo pj = PreferenciasJogo.carregar(new RepositorioConfiguracoes(new DiretorioDados(tmp)),
                new ServicoAudio(SaidaAudio.SILENCIOSA, c -> Optional.empty()));
        return new ConfiguracoesPanel(pj, RegistroCenarios.padrao(), new DialogosFalsos(), () -> { }, () -> { });
    }

    private ControlesPanel controles() {
        return new ControlesPanel(PreferenciasControles.carregar(new RepositorioControles(new DiretorioDados(tmp))),
                new DialogosFalsos(), () -> { });
    }

    private PerfisPanel perfis() {
        return new PerfisPanel(servico(), new DialogosFalsos(), () -> { });
    }

    private static List<Component> todos(Component c) {
        List<Component> l = new ArrayList<>();
        l.add(c);
        if (c instanceof Container k) {
            for (Component f : k.getComponents()) {
                l.addAll(todos(f));
            }
        }
        return l;
    }

    private static JLabel titulo(Container tela) {
        for (Component c : todos(tela)) {
            if (c instanceof JLabel l && l.getFont() == EstiloTelas.TITULO) {
                return l;
            }
        }
        return null;
    }

    // 15/16. Configuracoes, controles e personagens com o titulo novo, funcionando
    @Test
    void titulosNoEstiloNovo() throws Exception {
        naEdt(() -> {
            assertEquals("CONFIGURAÇÕES", titulo(configuracoes()).getText());
            assertEquals("CONTROLES", titulo(controles()).getText());
            assertEquals("PERSONAGENS PERSONALIZADOS", titulo(perfis()).getText());
            for (JComponent t : new JComponent[] {configuracoes(), controles(), perfis()}) {
                pintar(t);
            }
        });
        assertEquals(ArteMenus.TEXTO, EstiloTelas.ROTULO, "rotulos com a fonte dos menus novos");
        assertEquals(ArteMenus.PEQUENO, EstiloTelas.PEQUENA);
        assertEquals(Font.BOLD, EstiloTelas.TITULO.getStyle());
    }

    // 17. Sem cortes em 800 x 450: titulos, rotulos e botoes cabem nos seus espacos
    @Test
    void textosSemCorte() throws Exception {
        naEdt(() -> {
            for (JComponent tela : new JComponent[] {configuracoes(), controles(), perfis()}) {
                tela.setSize(800, 450);
                dispor(tela);
                for (Component c : todos(tela)) {
                    if (!c.isVisible() || c.getWidth() == 0) {
                        continue;
                    }
                    String texto = c instanceof JLabel l ? l.getText()
                            : c instanceof AbstractButton b ? b.getText() : null;
                    if (texto == null || texto.isBlank() || texto.startsWith("<html>")) {
                        continue;
                    }
                    if (texto.equals(((JComponent) c).getToolTipText())) {
                        continue;           // 5F: reticencias de proposito, texto inteiro no tooltip
                    }
                    int largura = c.getFontMetrics(((JComponent) c).getFont()).stringWidth(texto);
                    assertTrue(largura <= c.getWidth(), tela.getClass().getSimpleName() + ": \"" + texto
                            + "\" (" + largura + " px) nao cabe em " + c.getWidth() + " px");
                    Rectangle r = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), tela);
                    assertTrue(r.x >= 0 && r.x + r.width <= 800, texto + " fora da tela");
                }
            }
        });
    }

    // 18. Fontes com os acentos do portugues (a tipografia nao depende de arquivos de fonte)
    @Test
    void fontesComAcentos() {
        String acentos = "ÇÃÕÉÍÓÚÂÊÔÀçãõéíóúâêôà";
        for (Font f : new Font[] {EstiloTelas.TITULO, EstiloTelas.ROTULO, EstiloTelas.NORMAL, EstiloTelas.PEQUENA,
            ArteMenus.LETREIRO, ArteMenus.MENU}) {
            assertEquals(-1, f.canDisplayUpTo(acentos), f.getName());
        }
    }

    /**
     * Selecao do TREINO igual a do JOGAR (pedido): o mesmo controle confirma faixa e kimono do lutador e
     * depois personagem, faixa e kimono do parceiro, e AVANCA para a tela da arena.
     */
    private static void personalizacao(SelecaoPartidaPanel s) {
        for (int i = 0; i < 8 && !s.naConfiguracao(); i++) {
            s.comandar(SlotJogador.JOGADOR_1, Comando.CONFIRMAR);
        }
    }
}
