package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5M: tela de titulo, menu principal e selecao em grade de JOGAR. */
class Menus5MTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;

    private SelecaoPartidaPanel sel;
    private final AtomicReference<Object[]> iniciou = new AtomicReference<>();
    private final AtomicInteger inicios = new AtomicInteger();
    private final AtomicInteger voltas = new AtomicInteger();

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private ServicoPerfis servico() {
        return new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao());
    }

    private SelecaoPartidaPanel selecao(ProvedorGamepadSimulado pads, AssociacaoDispositivos assoc) {
        return new SelecaoPartidaPanel(CatalogoPersonagens.padrao(),
                new OpcoesDeLutadores(CatalogoPersonagens.padrao(), servico()), RegistroCenarios.padrao(),
                RegistroCenarios.ID_PADRAO,
                (j1, j2, c, s) -> {
                    inicios.incrementAndGet();
                    iniciou.set(new Object[]{j1, j2, c, s});
                },
                voltas::incrementAndGet, () -> pads, () -> assoc);
    }

    private SelecaoPartidaPanel selecao() {
        return selecao(null, null);
    }

    private static void pintar(javax.swing.JComponent c) {
        c.setSize(800, 450);
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        c.paint(g);
        g.dispose();
    }

    /**
     * Reorganizacao dos menus (pedido): cada jogador confirma personagem, (perfil,) faixa e kimono; so
     * entao fica PRONTO.
     */
    private void pronto(SlotJogador s) {
        for (int i = 0; i < 5 && sel.fase(s) != SelecaoPartidaPanel.Fase.PRONTO; i++) {
            sel.comandar(s, Comando.CONFIRMAR);
        }
        assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(s));
    }

    private void prontos() {
        pronto(J1);
        pronto(J2);
    }

    /** Reorganizacao dos menus (pedido): AVANCAR e, nas configuracoes, confirmar ate INICIAR LUTA. */
    private void lutar() {
        sel.comandar(J1, Comando.CONFIRMAR);                // AVANCAR
        assertTrue(sel.naConfiguracao());
        for (int i = 0; i < 6 && inicios.get() == 0; i++) {
            sel.comandar(J1, Comando.CONFIRMAR);            // ARENA -> TEMPO -> REGRAS -> INICIAR LUTA
        }
    }

    // =====================================================
    // TELA DE TITULO
    // =====================================================
    // 1. ENTER continua, uma vez so
    @Test
    void aberturaEnterContinuaUmaVez() throws Exception {
        AtomicInteger n = new AtomicInteger();
        naEdt(() -> {
            AberturaPanel a = new AberturaPanel(n::incrementAndGet, () -> null);
            assertFalse(a.isConcluida());
            a.pressionarEnterParaTestes();
            a.pressionarEnterParaTestes();
            assertTrue(a.isConcluida());
            assertEquals(1, n.get(), "sem transicao dupla");
        });
    }

    // 2. A do gamepad continua (depois do neutro)
    @Test
    void aberturaBotaoAContinua() throws Exception {
        AtomicInteger n = new AtomicInteger();
        ProvedorGamepadSimulado p = new ProvedorGamepadSimulado();
        p.conectar("g1", "id", "Xbox");
        naEdt(() -> {
            AberturaPanel a = new AberturaPanel(n::incrementAndGet, () -> p);
            a.tick(16);
            p.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL));
            a.tick(16);
            a.tick(16);
            assertEquals(1, n.get());
        });
    }

    // 3. A ja apertado quando a tela abre nao passa direto
    @Test
    void aberturaIgnoraBotaoJaSegurado() throws Exception {
        AtomicInteger n = new AtomicInteger();
        ProvedorGamepadSimulado p = new ProvedorGamepadSimulado();
        p.conectar("g1", "id", "Xbox");
        p.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL).com(BotaoGamepad.INICIAR));
        naEdt(() -> {
            AberturaPanel a = new AberturaPanel(n::incrementAndGet, () -> p);
            for (int i = 0; i < 20; i++) {
                a.tick(16);
            }
            assertEquals(0, n.get(), "A/START segurados desde antes nao contam");
            p.definir("g1", EstadoGamepad.repouso());
            a.tick(16);
            p.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.INICIAR));
            a.tick(16);
            assertEquals(1, n.get(), "START solto e apertado de novo continua");
        });
    }

    // 4. Os dois irmaos oficiais, frente a frente, e o idle anda
    @Test
    void aberturaMostraOsDoisIrmaos() throws Exception {
        naEdt(() -> {
            AberturaPanel a = new AberturaPanel(() -> { }, () -> null);
            assertTrue(a.getBranco().isViradoParaDireita());
            assertFalse(a.getPreto().isViradoParaDireita());
            assertTrue(a.getBranco().getX() < a.getPreto().getX());
            a.tick(16);
            assertEquals(16, a.getTempoMs());
            pintar(a);
        });
    }

    // 5. A abertura nao usa o navegador global de menus
    @Test
    void aberturaNaoAceitaNavegacaoGlobal() throws Exception {
        naEdt(() -> assertFalse(new AberturaPanel(() -> { }, () -> null).aceitaNavegacao()));
    }

    // =====================================================
    // MENU PRINCIPAL
    // =====================================================
    // 6. Opcoes na ordem, sem "1 JOGADOR"
    @Test
    void menuTemAsCincoOpcoes() {
        // 5Q.6 (pedido): o MODO DESENVOLVEDOR saiu do menu principal (liga com F8 dentro da partida)
        assertEquals(List.of("JOGAR", "TREINO", "PERSONAGENS", "CONFIGURAÇÕES", "SAIR"),
                MenuPrincipalPanel.OPCOES);
        assertFalse(MenuPrincipalPanel.OPCOES.stream().anyMatch(o -> o.contains("1 JOGADOR")));
    }

    // 7-11. Cada botao chama a sua acao
    @Test
    void menuCadaBotaoChamaSuaAcao() throws Exception {
        List<String> chamadas = new ArrayList<>();
        naEdt(() -> {
            MenuPrincipalPanel m = new MenuPrincipalPanel(() -> chamadas.add("jogar"), () -> chamadas.add("treino"),
                    () -> chamadas.add("personagens"), () -> chamadas.add("config"), () -> chamadas.add("sair"));
            for (String o : MenuPrincipalPanel.OPCOES) {
                m.botao(o).doClick();
            }
            pintar(m);
        });
        assertEquals(List.of("jogar", "treino", "personagens", "config", "sair"), chamadas);
    }

    // 5Q.6 (pedido): o teste do botao MODO DESENVOLVEDOR do menu saiu junto com a opcao; o modo
    // agora e testado dentro da partida (Dev5Q6GamePanelTest)

    // 12. Navegacao pelo navegador de menus (setas / D-pad) percorre as opcoes
    @Test
    void menuNavegavelPeloNavegador() throws Exception {
        naEdt(() -> {
            MenuPrincipalPanel m = new MenuPrincipalPanel(() -> { }, () -> { }, () -> { }, () -> { }, () -> { });
            java.awt.Component f = NavegadorMenusAcesso.executar(m, m.botao("JOGAR"), Comando.PROXIMO);
            assertSame(m.botao("TREINO"), f);
        });
    }

    // =====================================================
    // SELECAO
    // =====================================================
    // 13. Cursores independentes, em lugares diferentes no comeco
    @Test
    void cursoresIndependentes() throws Exception {
        naEdt(() -> {
            sel = selecao();
            assertEquals(0, sel.cursor(J1));
            assertEquals(1, sel.cursor(J2));
            sel.comandar(J1, Comando.DIREITA);
            assertEquals(1, sel.cursor(J1));
            assertEquals(1, sel.cursor(J2), "mexer o J1 nao mexe o J2");
            sel.comandar(J2, Comando.PROXIMO);
            assertEquals(1 + SelecaoPartidaPanel.COLUNAS, sel.cursor(J2));
            assertEquals(1, sel.cursor(J1));
        });
    }

    // 14. Selecao simultanea: os dois confirmam em qualquer ordem
    @Test
    void confirmacaoSimultanea() throws Exception {
        naEdt(() -> {
            sel = selecao();
            // reorganizacao (pedido): personagem -> faixa -> kimono -> PRONTO, nos dois
            sel.comandar(J2, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(J2));
            pronto(J2);
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, sel.fase(J1));
            pronto(J1);
        });
    }

    // 15. Espaco bloqueado: so aviso, nao confirma
    @Test
    void bloqueadoNaoSelecionavel() throws Exception {
        naEdt(() -> {
            sel = selecao();
            sel.comandar(J1, Comando.DIREITA);
            sel.comandar(J1, Comando.DIREITA);           // terceiro espaco: bloqueado
            assertNull(sel.escolha(J1));
            sel.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, sel.fase(J1));
            assertTrue(sel.isAvisoBloqueado(J1));
            sel.tick(800);
            assertFalse(sel.isAvisoBloqueado(J1), "o aviso some");
            assertNull(sel.previa(J1), "sem previa: nada revelado");
        });
    }

    // 16. Mesmo personagem nos dois lados
    @Test
    void mesmoPersonagemNosDoisLados() throws Exception {
        naEdt(() -> {
            sel = selecao();
            sel.comandar(J2, Comando.ESQUERDA);
            prontos();
            lutar();                                        // reorganizacao (pedido)
        });
        assertEquals(1, inicios.get());
        EscolhaLutador a = (EscolhaLutador) iniciou.get()[0];
        EscolhaLutador b = (EscolhaLutador) iniciou.get()[1];
        assertEquals(CatalogoPersonagens.ID_BRANCO, a.getBase().getId());
        assertEquals(CatalogoPersonagens.ID_BRANCO, b.getBase().getId());
    }

    // 17. Grade: horizontal da a volta, vertical para nas bordas
    @Test
    void movimentoNaGrade() throws Exception {
        naEdt(() -> {
            sel = selecao();
            sel.comandar(J1, Comando.ESQUERDA);
            assertEquals(SelecaoPartidaPanel.COLUNAS - 1, sel.cursor(J1));
            sel.comandar(J1, Comando.DIREITA);
            assertEquals(0, sel.cursor(J1));
            sel.comandar(J1, Comando.ANTERIOR);
            assertEquals(0, sel.cursor(J1));
            for (int i = 0; i < 5; i++) {
                sel.comandar(J1, Comando.PROXIMO);
            }
            assertEquals((SelecaoPartidaPanel.LINHAS - 1) * SelecaoPartidaPanel.COLUNAS, sel.cursor(J1));
        });
    }

    // 18. Previa segue o cursor e usa o renderizador oficial
    @Test
    void previaSegueCursor() throws Exception {
        naEdt(() -> {
            sel = selecao();
            sel.tick(16);
            assertNotNull(sel.previa(J1));
            assertEquals("Irmão Miau Branco", sel.previa(J1).getNome());
            assertEquals("Irmão Miau Preto", sel.previa(J2).getNome());
            sel.comandar(J1, Comando.DIREITA);
            sel.tick(16);
            assertEquals("Irmão Miau Preto", sel.previa(J1).getNome());
        });
    }

    // 19. Nome exibido e estilo (arte marcial)
    @Test
    void nomeEEstilo() {
        CatalogoPersonagens c = CatalogoPersonagens.padrao();
        assertEquals("MIAU BRANCO", SelecaoPartidaPanel.nomeExibido(c.exigir(CatalogoPersonagens.ID_BRANCO)));
        assertEquals("MIAU PRETO", SelecaoPartidaPanel.nomeExibido(c.exigir(CatalogoPersonagens.ID_PRETO)));
        assertEquals("Jiu-Jitsu", c.exigir(CatalogoPersonagens.ID_BRANCO).getEstilo());
    }

    // 20. Cancelar depois de pronto volta a escolher
    @Test
    void cancelarDesfazPronto() throws Exception {
        naEdt(() -> {
            sel = selecao();
            // reorganizacao (pedido): cancelar depois de PRONTO volta ao kimono; cada cancelar, um passo
            pronto(J2);
            sel.comandar(J2, Comando.VOLTAR);
            assertEquals(SelecaoPartidaPanel.Fase.KIMONO, sel.fase(J2));
            sel.comandar(J2, Comando.VOLTAR);
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(J2));
            sel.comandar(J2, Comando.VOLTAR);
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, sel.fase(J2));
            assertEquals(0, voltas.get());
        });
    }

    // 21. Cancelar escolhendo volta ao menu (uma vez so)
    @Test
    void cancelarEscolhendoVoltaAoMenu() throws Exception {
        naEdt(() -> {
            sel = selecao();
            sel.comandar(J1, Comando.VOLTAR);
            sel.comandar(J2, Comando.VOLTAR);
            sel.tecla("ESCAPE", true);
        });
        assertEquals(1, voltas.get());
        assertEquals(0, inicios.get());
    }

    // 22. LUTAR sem o J2 pronto nao comeca
    @Test
    void lutarEsperaOJ2() throws Exception {
        naEdt(() -> {
            sel = selecao();
            pronto(J1);                                       // reorganizacao (pedido)
            sel.comandar(J1, Comando.CONFIRMAR);              // AVANCAR sem o J2: nada
            assertFalse(sel.naConfiguracao());
            assertEquals(0, inicios.get());
            pronto(J2);
            lutar();
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J2, Comando.CONFIRMAR);
        });
        assertEquals(1, inicios.get(), "inicia uma vez so");
    }

    // 23. Perfil personalizado aparece como fase extra e e o escolhido
    @Test
    void perfilPersonalizado() throws Exception {
        servico().criarAPartirDoPersonagem("pretinho", "Pretinho", CatalogoPersonagens.ID_PRETO);
        naEdt(() -> {
            sel = selecao();
            sel.comandar(J2, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.PERFIL, sel.fase(J2));
            sel.comandar(J2, Comando.DIREITA);
            sel.comandar(J2, Comando.CONFIRMAR);
            // reorganizacao (pedido): depois do perfil, faixa e kimono
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(J2));
            pronto(J2);
            assertEquals("Pretinho", sel.escolha(J2).getNome());
            pronto(J1);
            lutar();
        });
        assertEquals("Pretinho", ((EscolhaLutador) iniciou.get()[1]).getNome());
    }

    // 24. MAPA: cenarios reais, troca com as setas e vai para a luta
    @Test
    void mapaReal() throws Exception {
        naEdt(() -> {
            sel = selecao();
            assertTrue(sel.mapaTemOpcoes());
            assertEquals(RegistroCenarios.ID_PADRAO, sel.mapaAtual());
            prontos();
            // reorganizacao (pedido): a ARENA esta na segunda tela, com o foco inicial
            sel.comandar(J1, Comando.CONFIRMAR);             // AVANCAR
            assertEquals(ConfiguracaoLuta.Area.ARENA, sel.configuracao().foco());
            sel.comandar(J1, Comando.DIREITA);
            assertEquals("original", sel.mapaAtual());
            assertTrue(RegistroCenarios.padrao().contem(sel.mapaAtual()));
            for (int i = 0; i < 4; i++) {
                sel.comandar(J1, Comando.CONFIRMAR);         // ARENA -> TEMPO -> REGRAS -> INICIAR LUTA
            }
        });
        assertEquals("original", iniciou.get()[2]);
    }

    // 25. TEMPO: SEM LIMITE / 60 / 90 / 180
    @Test
    void temposOferecidos() throws Exception {
        naEdt(() -> {
            sel = selecao();
            prontos();
            sel.comandar(J1, Comando.CONFIRMAR);             // reorganizacao (pedido): AVANCAR
            sel.comandar(J1, Comando.PROXIMO);
            assertEquals(ConfiguracaoLuta.Area.TEMPO, sel.configuracao().foco());
            // sistema de regras (pedido): de 1 a 10 minutos e sem limite (o padrao continua SEM LIMITE)
            List<Integer> vistos = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                vistos.add(sel.tempoAtual());
                sel.comandar(J1, Comando.DIREITA);
            }
            assertEquals(List.of(0, 60, 90, 120, 180, 240, 300, 360, 420, 480, 540, 600), vistos);
            assertEquals(0, sel.tempoAtual(), "da a volta");
            sel.comandar(J1, Comando.ESQUERDA);
            sel.comandar(J1, Comando.ESQUERDA);
            sel.comandar(J1, Comando.ESQUERDA);
            sel.comandar(J1, Comando.ESQUERDA);
            sel.comandar(J1, Comando.ESQUERDA);
            sel.comandar(J1, Comando.ESQUERDA);
            sel.comandar(J1, Comando.ESQUERDA);
            sel.comandar(J1, Comando.ESQUERDA);
            assertEquals(180, sel.tempoAtual());
            sel.comandar(J1, Comando.PROXIMO);               // REGRAS
            sel.comandar(J1, Comando.PROXIMO);               // INICIAR LUTA
            sel.comandar(J1, Comando.CONFIRMAR);
        });
        assertEquals(180, iniciou.get()[3]);
        assertEquals("SEM LIMITE", SelecaoPartidaPanel.textoTempo(0));
    }

    // 26. DIFICULDADE e pulada (EM BREVE)
    @Test
    void dificuldadeDesabilitada() throws Exception {
        naEdt(() -> {
            sel = selecao();
            prontos();
            sel.comandar(J1, Comando.CONFIRMAR);             // reorganizacao (pedido): na segunda tela
            for (int i = 0; i < 5; i++) {
                sel.comandar(J1, Comando.PROXIMO);
                assertFalse(sel.configuracao().foco() == ConfiguracaoLuta.Area.DIFICULDADE);
            }
            for (int i = 0; i < 5; i++) {
                sel.comandar(J1, Comando.ANTERIOR);
                assertFalse(sel.configuracao().foco() == ConfiguracaoLuta.Area.DIFICULDADE);
            }
        });
    }

    // 27. Reorganizacao (pedido): na primeira tela o J2 nao mexe em nada do J1 nem da partida; com os
    // dois prontos o J2 tambem avanca, e na segunda tela os dois movem o mesmo foco (o J2 nao depende do J1)
    @Test
    void j2NaoMexeNasConfiguracoes() throws Exception {
        naEdt(() -> {
            sel = selecao();
            prontos();
            sel.comandar(J2, Comando.ANTERIOR);
            sel.comandar(J2, Comando.DIREITA);
            assertEquals(0, sel.tempoAtual());
            assertEquals(RegistroCenarios.ID_PADRAO, sel.mapaAtual());
            assertEquals(0, inicios.get());
            sel.comandar(J2, Comando.CONFIRMAR);             // o J2 avanca
            assertTrue(sel.naConfiguracao());
            sel.comandar(J2, Comando.DIREITA);               // e escolhe a arena
            assertEquals("original", sel.mapaAtual());
        });
        assertEquals(0, inicios.get(), "nada comeca sem INICIAR LUTA");
    }

    // 28. Teclado: cada aperto age uma vez (auto-repeat ignorado)
    @Test
    void tecladoUmaVezPorAperto() throws Exception {
        naEdt(() -> {
            sel = selecao();
            sel.tecla("D", true);
            sel.tecla("D", true);                           // auto-repeat do sistema
            assertEquals(1, sel.cursor(J1));
            sel.tecla("D", false);
            sel.tecla("D", true);
            assertEquals(2, sel.cursor(J1));
            sel.tecla("LEFT", true);
            assertEquals(0, sel.cursor(J2), "setas sao do J2");
        });
    }

    // 29. Dois gamepads: cada um controla o seu cursor
    @Test
    void doisGamepadsIndependentes() throws Exception {
        ProvedorGamepadSimulado p = new ProvedorGamepadSimulado();
        p.conectar("g1", "id1", "Xbox 1");
        p.conectar("g2", "id2", "Xbox 2");
        AssociacaoDispositivos a = AssociacaoDispositivos.padrao()
                .com(J1, TipoDispositivo.GAMEPAD).com(J2, TipoDispositivo.GAMEPAD);
        naEdt(() -> {
            sel = selecao(p, a);
            sel.tick(16);                                   // arma os leitores no neutro
            p.definir("g2", EstadoGamepad.repouso().com(BotaoGamepad.DPAD_ESQUERDA));
            sel.tick(16);
            assertEquals(0, sel.cursor(J2));
            assertEquals(0, sel.cursor(J1));
            p.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL));
            sel.tick(16);
            // reorganizacao (pedido): o A confirma o personagem e segue para a faixa
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(J1));
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, sel.fase(J2));
            sel.tick(16);
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(J1), "segurar o A nao confirma de novo");
            for (String g : new String[] {"g1", "g1", "g2", "g2", "g2"}) {
                p.definir(g, EstadoGamepad.repouso());
                sel.tick(16);
                p.definir(g, EstadoGamepad.repouso().com(BotaoGamepad.SUL));
                sel.tick(16);
            }
            p.definir("g2", EstadoGamepad.repouso());
            sel.tick(16);
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(J1));
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(J2), "o segundo gamepad conclui sozinho");
        });
    }

    // 30. Desenho completo sem erro (todas as fases)
    @Test
    void desenhaTodasAsFases() throws Exception {
        naEdt(() -> {
            sel = selecao();
            sel.tick(16);
            pintar(sel);
            sel.comandar(J1, Comando.DIREITA);
            sel.comandar(J1, Comando.DIREITA);
            sel.comandar(J1, Comando.CONFIRMAR);
            pintar(sel);
            sel.comandar(J1, Comando.ESQUERDA);
            sel.comandar(J1, Comando.ESQUERDA);
            prontos();
            sel.tick(16);
            pintar(sel);
            sel.comandar(J1, Comando.CONFIRMAR);             // reorganizacao (pedido): a segunda tela
            pintar(sel);
            assertFalse(sel.aceitaNavegacao());
        });
    }

    /**
     * 5Q.4 (pedido): depois de escolher o personagem o J1 cai na FAIXA (antes ia direto para LUTAR);
     * confirma faixa e kimono (e os do parceiro, no treino) como o jogador faria, ate voltar a LUTAR.
     */
    private static void personalizacao(SelecaoPartidaPanel s) {
        for (int i = 0; i < 6 && s.linhaJ1() != SelecaoPartidaPanel.Linha.LUTAR; i++) {
            s.comandar(SlotJogador.JOGADOR_1, Comando.CONFIRMAR);
        }
    }
}
