package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.CorKimono;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.regras.Graduacao;
import com.irmaosmiau.regras.ModoLuta;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reorganizacao dos menus de luta: SELECAO DOS LUTADORES (personagem -> faixa -> kimono -> PRONTO, igual
 * para J1 e J2) e CONFIGURACOES DA LUTA (arena com previa, tempo, dificuldade EM BREVE, regras e INICIAR
 * LUTA).
 */
class ReorganizacaoMenusTest {

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private final AtomicInteger inicios = new AtomicInteger();
    private final AtomicInteger voltas = new AtomicInteger();
    private final AtomicReference<Object[]> iniciou = new AtomicReference<>();
    private SelecaoPartidaPanel sel;

    private SelecaoPartidaPanel nova(ProvedorGamepadSimulado pads, AssociacaoDispositivos assoc) {
        OpcoesDeLutadores op = new OpcoesDeLutadores(CatalogoPersonagens.padrao(), new ServicoPerfis(
                new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao()));
        return new SelecaoPartidaPanel(CatalogoPersonagens.padrao(), op, RegistroCenarios.padrao(),
                RegistroCenarios.ID_PADRAO, new SelecaoPartidaPanel.AoIniciar() {
                    @Override
                    public void iniciar(EscolhaLutador a, EscolhaLutador b, String c, int s) {
                        throw new AssertionError("a tela deve usar o inicio com o modo de luta");
                    }

                    @Override
                    public void iniciar(EscolhaLutador a, EscolhaLutador b, String c, int s, ModoLuta m) {
                        inicios.incrementAndGet();
                        iniciou.set(new Object[] {a, b, c, s, m});
                    }
                }, voltas::incrementAndGet, () -> pads, () -> assoc);
    }

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void pronto(SlotJogador s) {
        for (int i = 0; i < 3; i++) {
            sel.comandar(s, Comando.CONFIRMAR);
        }
    }

    // 1-3: o mesmo fluxo para J1 e J2; PRONTO so depois do kimono
    @Test
    void j1Ej2ConfirmamPersonagemFaixaEKimono() throws Exception {
        naEdt(() -> {
            sel = nova(null, null);
            for (SlotJogador s : SlotJogador.values()) {
                assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, sel.fase(s));
                sel.comandar(s, Comando.CONFIRMAR);
                assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(s), s + ": personagem -> faixa");
                sel.comandar(s, Comando.DIREITA);
                sel.comandar(s, Comando.CONFIRMAR);
                assertEquals(SelecaoPartidaPanel.Fase.KIMONO, sel.fase(s), s + ": faixa -> kimono");
                assertFalse(sel.ambosProntos());
                sel.comandar(s, Comando.DIREITA);
                sel.comandar(s, Comando.CONFIRMAR);
                assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(s), s + ": kimono confirmado = PRONTO");
                assertEquals(Graduacao.AZUL, sel.faixa(s));
            }
            assertTrue(sel.ambosProntos());
            assertFalse(sel.naConfiguracao(), "sem confirmacao extra, mas nada avanca sozinho");
        });
    }

    // 4: PRONTO trava as escolhas (as direcoes nao mudam nada; so cancelar volta ao kimono)
    @Test
    void prontoNaoMudaAsEscolhasPorAcidente() throws Exception {
        naEdt(() -> {
            sel = nova(null, null);
            for (SlotJogador s : SlotJogador.values()) {
                pronto(s);
                int cursor = sel.cursor(s);
                Graduacao f = sel.faixa(s);
                CorKimono k = sel.kimonoEfetivo(s);
                String id = sel.escolha(s).getId();
                for (Comando c : new Comando[] {Comando.ESQUERDA, Comando.DIREITA, Comando.ANTERIOR, Comando.PROXIMO}) {
                    sel.comandar(s, c);
                }
                assertEquals(cursor, sel.cursor(s));
                assertEquals(f, sel.faixa(s));
                assertEquals(k, sel.kimonoEfetivo(s));
                assertEquals(id, sel.escolha(s).getId());
                assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(s));
            }
            sel.comandar(J2, Comando.VOLTAR);
            assertEquals(SelecaoPartidaPanel.Fase.KIMONO, sel.fase(J2), "cancelar e a acao explicita para corrigir");
        });
    }

    // 5: AVANCAR so com os dois prontos (pelo J1 ou pelo J2)
    @Test
    void avancarSoComOsDoisProntos() throws Exception {
        naEdt(() -> {
            sel = nova(null, null);
            pronto(J1);
            sel.comandar(J1, Comando.CONFIRMAR);
            assertFalse(sel.naConfiguracao(), "o J2 ainda nao esta pronto");
            sel.clicar(400, 380);
            assertFalse(sel.naConfiguracao(), "nem pelo mouse");
            pronto(J2);
            sel.comandar(J2, Comando.CONFIRMAR);
            assertTrue(sel.naConfiguracao(), "o J2 tambem avanca");
            assertEquals(ConfiguracaoLuta.Area.ARENA, sel.configuracao().foco(), "foco inicial na arena");
        });
    }

    // 6 + 9: as escolhas ficam entre as telas; voltar (cancelar ou ESC) nao quebra a navegacao
    @Test
    void escolhasFicamAoIrEVoltar() throws Exception {
        naEdt(() -> {
            sel = nova(null, null);
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.DIREITA);                  // faixa azul
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.DIREITA);                  // outro kimono
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J2, Comando.ESQUERDA);                 // J2 tambem Branco
            pronto(J2);
            EscolhaLutador a = sel.escolha(J1);
            EscolhaLutador b = sel.escolha(J2);
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.DIREITA);                  // arena
            String arena = sel.mapaAtual();
            sel.comandar(J1, Comando.VOLTAR);                   // cancelar: volta aos lutadores
            assertFalse(sel.naConfiguracao());
            assertEquals(0, voltas.get(), "nao volta ao menu");
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(J1));
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(J2));
            assertEquals(a.getId() + a.getGraduacao() + a.getKimono(),
                    sel.escolha(J1).getId() + sel.escolha(J1).getGraduacao() + sel.escolha(J1).getKimono());
            assertEquals(b.getId(), sel.escolha(J2).getId());
            sel.comandar(J2, Comando.CONFIRMAR);                // avanca de novo
            assertEquals(arena, sel.mapaAtual(), "a arena escolhida tambem fica");
            sel.tecla("ESCAPE", true);                          // ESC: volta uma tela
            assertFalse(sel.naConfiguracao());
            assertEquals(0, voltas.get());
            sel.tecla("ESCAPE", true);                          // nos lutadores, ESC volta ao menu
            assertEquals(1, voltas.get());
        });
    }

    // 7: a previa e o proprio cenario selecionado (sem lista duplicada)
    @Test
    void previaDaArenaCorrespondeAoMapa() throws Exception {
        naEdt(() -> {
            sel = nova(null, null);
            pronto(J1);
            pronto(J2);
            sel.comandar(J1, Comando.CONFIRMAR);
            ConfiguracaoLuta c = sel.configuracao();
            RegistroCenarios reg = RegistroCenarios.padrao();
            java.util.Set<String> vistos = new java.util.LinkedHashSet<>();
            for (int i = 0; i < reg.ids().size(); i++) {
                String id = sel.mapaAtual();
                vistos.add(id);
                BufferedImage esperado = new BufferedImage(800, 450, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = esperado.createGraphics();
                reg.exigir(id).desenharFundo(g, 800, 450);
                reg.exigir(id).desenharChao(g, 800, 450);
                reg.exigir(id).desenharPrimeiroPlano(g, 800, 450);
                g.dispose();
                assertArrayEquals(pixels(esperado), pixels(c.previa(id)), "previa de " + id);
                sel.comandar(J1, Comando.DIREITA);
            }
            assertEquals(new java.util.LinkedHashSet<>(reg.ids()), vistos, "todas as arenas do registro");
            assertNotEquals(pixels(c.previa(reg.ids().get(0)))[800 * 50 + 400],
                    0, "a previa nao e vazia");
        });
    }

    private static int[] pixels(BufferedImage i) {
        return i.getRGB(0, 0, i.getWidth(), i.getHeight(), null, 0, i.getWidth());
    }

    // 8: a luta comeca com tudo o que foi escolhido nas duas telas
    @Test
    void lutaIniciaComAsConfiguracoes() throws Exception {
        naEdt(() -> {
            sel = nova(null, null);
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.DIREITA);                  // azul
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J2, Comando.CONFIRMAR);
            sel.comandar(J2, Comando.DIREITA);
            sel.comandar(J2, Comando.DIREITA);                  // roxa
            sel.comandar(J2, Comando.CONFIRMAR);
            sel.comandar(J2, Comando.DIREITA);                  // outro kimono
            sel.comandar(J2, Comando.CONFIRMAR);
            CorKimono k2 = sel.kimonoEfetivo(J2);
            sel.comandar(J1, Comando.CONFIRMAR);                // AVANCAR
            sel.comandar(J1, Comando.DIREITA);                  // arena
            String arena = sel.mapaAtual();
            sel.comandar(J1, Comando.PROXIMO);
            sel.comandar(J1, Comando.DIREITA);                  // 60 s
            sel.comandar(J1, Comando.PROXIMO);                  // REGRAS (FINALIZACAO)
            sel.comandar(J1, Comando.PROXIMO);
            assertEquals(ConfiguracaoLuta.Area.INICIAR, sel.configuracao().foco());
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.CONFIRMAR);                // duplicado: ignorado
            assertEquals(1, inicios.get(), "uma vez so");
            Object[] o = iniciou.get();
            EscolhaLutador a = (EscolhaLutador) o[0];
            EscolhaLutador b = (EscolhaLutador) o[1];
            assertEquals(CatalogoPersonagens.ID_BRANCO, a.getIdBase());
            assertEquals(CatalogoPersonagens.ID_PRETO, b.getIdBase());
            assertEquals(Graduacao.AZUL, a.getGraduacao());
            assertEquals(Graduacao.ROXA, b.getGraduacao());
            assertEquals(k2, b.getKimono());
            assertEquals(arena, o[2]);
            assertEquals(60, o[3]);
            assertEquals(ModoLuta.FINALIZACAO, o[4]);
        });
    }

    // 11: modo EM BREVE nao inicia a luta (nem pelo mouse). Sistema de regras (pedido): PONTOS +
    // FINALIZACAO ficou disponivel; o ARCADE continua EM BREVE
    @Test
    void regraEmBreveNaoIniciaPartida() throws Exception {
        naEdt(() -> {
            sel = nova(null, null);
            pronto(J1);
            pronto(J2);
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.PROXIMO);
            sel.comandar(J1, Comando.PROXIMO);                  // REGRAS
            sel.comandar(J1, Comando.DIREITA);
            assertEquals(ModoLuta.PONTOS_E_FINALIZACAO, sel.configuracao().modoLuta());
            assertTrue(sel.configuracao().modoLuta().isDisponivel());
            sel.comandar(J1, Comando.DIREITA);                  // ARCADE
            assertEquals(ModoLuta.ARCADE, sel.configuracao().modoLuta());
            sel.comandar(J1, Comando.PROXIMO);
            sel.comandar(J1, Comando.CONFIRMAR);
            assertEquals(0, inicios.get(), "EM BREVE nao comeca");
            assertEquals(ConfiguracaoLuta.Area.REGRAS, sel.configuracao().foco(), "volta para as regras");
            sel.clicar(400, 384);
            assertEquals(0, inicios.get());
            sel.comandar(J1, Comando.ESQUERDA);                 // PONTOS + FINALIZACAO
            sel.clicar(400, 384);
            assertEquals(1, inicios.get());
            assertEquals(ModoLuta.PONTOS_E_FINALIZACAO, iniciou.get()[4], "o modo chega a partida");
        });
        assertTrue(ModoLuta.FINALIZACAO.isDisponivel());
        assertFalse(ModoLuta.ARCADE.isDisponivel());
    }

    // 12: dois gamepads levam cada um o seu jogador ate o fim; o A que avanca nao vale na outra tela
    @Test
    void gamepadsIndependentesAteAsConfiguracoes() throws Exception {
        ProvedorGamepadSimulado p = new ProvedorGamepadSimulado();
        p.conectar("g1", "id1", "Xbox 1");
        p.conectar("g2", "id2", "Xbox 2");
        AssociacaoDispositivos a = AssociacaoDispositivos.padrao()
                .com(J1, TipoDispositivo.GAMEPAD).com(J2, TipoDispositivo.GAMEPAD);
        naEdt(() -> {
            sel = nova(p, a);
            sel.tick(16);
            for (int i = 0; i < 3; i++) {                       // so o segundo gamepad
                p.definir("g2", EstadoGamepad.repouso().com(BotaoGamepad.SUL));
                sel.tick(16);
                p.definir("g2", EstadoGamepad.repouso());
                sel.tick(16);
            }
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(J2), "o J2 conclui sem o J1");
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, sel.fase(J1));
            for (int i = 0; i < 3; i++) {
                p.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL));
                sel.tick(16);
                p.definir("g1", EstadoGamepad.repouso());
                sel.tick(16);
            }
            p.definir("g2", EstadoGamepad.repouso().com(BotaoGamepad.SUL));   // J2 avanca
            sel.tick(16);
            assertTrue(sel.naConfiguracao());
            for (int i = 0; i < 5; i++) {
                sel.tick(16);                                   // A ainda segurado: nada na outra tela
            }
            assertEquals(ConfiguracaoLuta.Area.ARENA, sel.configuracao().foco());
            p.definir("g2", EstadoGamepad.repouso());
            sel.tick(16);
            p.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.LESTE));  // B volta aos lutadores
            sel.tick(16);
            assertFalse(sel.naConfiguracao());
            assertEquals(0, inicios.get());
        });
    }

    // 19 + 21 + 22: mudar a faixa (cancelando e confirmando de novo) nao muda o tempo escolhido, e tempo e
    // modo chegam a partida
    @Test
    void faixaNaoMudaOTempoEscolhido() throws Exception {
        naEdt(() -> {
            sel = nova(null, null);
            pronto(J1);
            pronto(J2);
            sel.comandar(J1, Comando.CONFIRMAR);                // AVANCAR
            sel.comandar(J1, Comando.PROXIMO);                  // TEMPO
            for (int i = 0; i < 11; i++) {
                sel.comandar(J1, Comando.DIREITA);              // ... 10 MIN
            }
            assertEquals(600, sel.tempoAtual());
            sel.comandar(J1, Comando.VOLTAR);                   // volta aos lutadores
            sel.comandar(J2, Comando.VOLTAR);                   // J2 corrige: kimono
            sel.comandar(J2, Comando.VOLTAR);                   // faixa
            sel.comandar(J2, Comando.DIREITA);
            sel.comandar(J2, Comando.DIREITA);
            sel.comandar(J2, Comando.DIREITA);
            sel.comandar(J2, Comando.DIREITA);                  // preta
            sel.comandar(J2, Comando.CONFIRMAR);
            sel.comandar(J2, Comando.CONFIRMAR);
            assertEquals(Graduacao.PRETA, sel.faixa(J2));
            sel.comandar(J2, Comando.CONFIRMAR);                // AVANCAR
            assertEquals(600, sel.tempoAtual(), "a faixa nao escolhe o tempo");
            sel.comandar(J1, Comando.PROXIMO);
            sel.comandar(J1, Comando.PROXIMO);                  // REGRAS
            sel.comandar(J1, Comando.DIREITA);                  // PONTOS + FINALIZACAO
            sel.comandar(J1, Comando.PROXIMO);
            sel.comandar(J1, Comando.CONFIRMAR);
            assertEquals(1, inicios.get());
            assertEquals(600, iniciou.get()[3]);
            assertEquals(ModoLuta.PONTOS_E_FINALIZACAO, iniciou.get()[4]);
        });
    }

    @Test
    void desenhaAsDuasTelas() throws Exception {
        naEdt(() -> {
            sel = nova(null, null);
            sel.setSize(800, 450);
            BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            sel.paint(g);
            pronto(J1);
            sel.paint(g);
            pronto(J2);
            sel.paint(g);
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.tick(16);
            sel.paint(g);
            sel.comandar(J1, Comando.PROXIMO);
            sel.comandar(J1, Comando.PROXIMO);
            sel.comandar(J1, Comando.DIREITA);                  // regra EM BREVE
            sel.paint(g);
            g.dispose();
        });
    }
}
