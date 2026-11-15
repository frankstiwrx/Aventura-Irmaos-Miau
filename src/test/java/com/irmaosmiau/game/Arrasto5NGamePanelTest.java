package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.CamadaPoseCombate.FaseReceptor;
import com.irmaosmiau.combate.ModoAnimacoesPegada;
import com.irmaosmiau.combate.ResultadoAcaoTecnica;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.CustosFolego;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Entrega 5N (testes 25-50 e integracao 52-68): arrasto multidirecional da dupla. */
class Arrasto5NGamePanelTest {

    private static final int MIN = GamePanel.LIMITE_ESQUERDO_ARENA;
    private static final int MAX = GamePanel.LIMITE_DIREITO_ARENA;

    private GamePanel painel;
    private IrmaoMiau apl;
    private IrmaoMiau rec;
    private ProvedorGamepadSimulado sim;

    @BeforeEach
    void modoDoJogo() {
        ModoAnimacoesPegada.definir(ModoAnimacoesPegada.EXPERIMENTAL);
    }

    @AfterEach
    void restaurar() {
        ModoAnimacoesPegada.restaurarPadrao();
        ServicoGamepads.instalarGlobal(null);
        PreferenciasDispositivos.instalarGlobal(null);
        PreferenciasGamepad.instalarGlobal(null);
    }

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void tecla(String nome, boolean p) {
        painel.getActionMap().get(nome + (p ? "Pressionada" : "Solta")).actionPerformed(null);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private void toque(String nome) {
        tecla(nome, true);
        ticks(1);
        tecla(nome, false);
        ticks(1);
    }

    private SistemaCombate s() {
        return painel.getSistemaCombate();
    }

    private void estabelecer(GamePanel g, boolean j1, int n) {
        painel = g;
        painel.pararLoopParaTestes();
        ticks(1);
        apl = j1 ? painel.getLutador1() : painel.getLutador2();
        rec = j1 ? painel.getLutador2() : painel.getLutador1();
        String andar = j1 ? "brancoDireita" : "pretoEsquerda";
        tecla(andar, true);
        for (int i = 0; i < 400 && !s().podeTentarAcao(apl, rec, "pegada_" + n); i++) {
            ticks(1);
        }
        tecla(andar, false);
        ticks(2);
        toque((j1 ? "branco" : "preto") + "Pegada" + n);
        ticks(40);
        assertTrue(s().isInteracaoEstabelecida());
    }

    private void pegada(InicioLuta inicio) {
        estabelecer(new GamePanel(TipoPersonagem.AMBOS, null, null, inicio, () -> { }, () -> { }, () -> { }), true, 1);
    }

    /** J1 conduz com o modo controle segurando as direcoes por n ticks. */
    private void conduzir(int n, String... direcoes) {
        tecla("brancoControle", true);
        for (String d : direcoes) {
            tecla(d, true);
        }
        ticks(n);
        for (String d : direcoes) {
            tecla(d, false);
        }
        tecla("brancoControle", false);
        ticks(1);
    }

    /**
     * Conduz ate a borda (ou n ticks), reforcando de vez em quando como um
     * jogador faria (sem reforco a firmeza acaba em ~470 ticks de arrasto).
     * A pegada precisa continuar o tempo todo.
     */
    private void conduzirLongo(int n, String... direcoes) {
        tecla("brancoControle", true);
        for (String d : direcoes) {
            tecla(d, true);
        }
        for (int i = 0; i < n; i++) {
            if (i % 120 == 119) {
                tecla("brancoReforco", true);
            } else if (i % 120 == 0) {
                tecla("brancoReforco", false);
            }
            ticks(1);
            assertTrue(s().isInteracaoEstabelecida(), "a pegada continua durante a conducao (tick " + i + ")");
            dentroDaArena();
        }
        tecla("brancoReforco", false);
        for (String d : direcoes) {
            tecla(d, false);
        }
        tecla("brancoControle", false);
        ticks(1);
    }

    private int distancia() {
        return rec.getX() - apl.getX();
    }

    private void dentroDaArena() {
        for (IrmaoMiau l : new IrmaoMiau[] {apl, rec}) {
            assertTrue(l.getX() >= MIN && l.getX() <= MAX, "x fora da arena: " + l.getX());
            assertTrue(l.getLinhaAtual() >= 0 && l.getLinhaAtual() <= 4, "linha fora do tatame");
        }
    }

    // 25, 26, 37, 39. Esquerda e direita: a dupla anda junta, distancia e lados iguais
    @Test
    void esquerdaEDireita() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            int d = distancia();
            int x = apl.getX();
            conduzir(20, "brancoDireita");
            assertEquals(x + 20, apl.getX());
            assertEquals(d, distancia());
            conduzir(30, "brancoEsquerda");
            assertEquals(x - 10, apl.getX());
            assertEquals(d, distancia());
            assertTrue(apl.isViradoParaDireita() && !rec.isViradoParaDireita(), "orientacao igual");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // 27, 28, 30, 31, 53. Frente/fundo: os dois mudam de linha juntos e a pegada continua
    @Test
    void frenteEFundoMudamALinhaDaDupla() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            int y = apl.getY();
            int x = apl.getX();
            assertEquals(apl.getLinhaAtual(), rec.getLinhaAtual());
            conduzir(25, "brancoBaixo");
            assertEquals(y + 25, apl.getY());
            assertEquals(apl.getY() - y, rec.getY() - y + (y - apl.getY()) + (apl.getY() - y), "mesmo deslocamento");
            assertEquals(apl.getLinhaAtual(), rec.getLinhaAtual(), "mesma linha (visual e motor)");
            assertTrue(apl.getLinhaAtual() >= 1);
            assertEquals(x, apl.getX());
            assertTrue(s().isInteracaoEstabelecida(), "mudanca conjunta nao rompe");
            assertEquals(FaseReceptor.SUSTENTADA, painel.getAnimador().getCamada().faseReceptor(rec));
            conduzir(25, "brancoCima");
            assertEquals(y, apl.getY());
            assertEquals(apl.getY(), rec.getY());
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // 29, 43, 54. Diagonal: anda nos dois eixos e nao e mais rapida
    @Test
    void diagonalSemVantagem() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            int x = apl.getX();
            int y = apl.getY();
            conduzir(60, "brancoDireita", "brancoBaixo");
            int dx = apl.getX() - x;
            int dy = apl.getY() - y;
            assertTrue(dx > 30 && dy > 30, "andou nos dois eixos: " + dx + "," + dy);
            assertTrue(Math.hypot(dx, dy) <= 60 * SistemaCombate.VELOCIDADE_DESLOCAMENTO_CONJUNTO + 1.5,
                    "diagonal nao e mais rapida: " + Math.hypot(dx, dy));
            assertEquals(apl.getY(), rec.getY());
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // 32, 33, 42. Linha 1 e linha 5 bloqueiam; bloqueado nao custa
    @Test
    void bloqueioNasLinhasSemCusto() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            while (apl.podeDeslocarProfundidade(-1) && rec.podeDeslocarProfundidade(-1)) {
                assertEquals(ResultadoAcaoTecnica.SUCESSO, s().deslocarPegadaControlada(apl, 0, -1, MIN, MAX));
            }
            assertEquals(0, apl.getLinhaAtual());
            double f = apl.getFolegoAtual();
            int y = rec.getY();
            assertEquals(ResultadoAcaoTecnica.LIMITE_ARENA, s().deslocarPegadaControlada(apl, 0, -1, MIN, MAX));
            assertEquals(f, apl.getFolegoAtual(), 1e-9, "bloqueado nao custa");
            assertEquals(y, rec.getY());
            conduzir(120, "brancoBaixo");
            assertEquals(4, apl.getLinhaAtual());
            assertEquals(4, rec.getLinhaAtual());
            f = apl.getFolegoAtual();
            assertEquals(ResultadoAcaoTecnica.LIMITE_ARENA, s().deslocarPegadaControlada(apl, 0, 1, MIN, MAX));
            assertEquals(f, apl.getFolegoAtual(), 1e-9);
            assertTrue(s().isInteracaoEstabelecida());
            assertTrue(s().getUltimoArrasto().bloqueadoY());
        });
    }

    // 34, 35, 36, 38, 40. Bordas: ninguem passa, ninguem fica para tras, ninguem atravessa o outro
    @Test
    void bordasEsquerdaEDireita() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            int d = distancia();
            conduzirLongo(400, "brancoEsquerda");
            assertEquals(MIN, Math.min(apl.getX(), rec.getX()));
            assertEquals(d, distancia(), "a dupla nao se separa na borda");
            dentroDaArena();
            assertTrue(apl.getX() < rec.getX(), "nao troca de lado");
            assertEquals(ResultadoAcaoTecnica.LIMITE_ARENA, s().deslocarPegadaControlada(apl, -1, 0, MIN, MAX));
            conduzirLongo(700, "brancoDireita");
            assertEquals(MAX, Math.max(apl.getX(), rec.getX()));
            assertEquals(d, distancia());
            dentroDaArena();
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // Diagonal na borda: o eixo livre continua (desliza), o outro fica parado
    @Test
    void diagonalDeslizaNaBorda() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            conduzirLongo(400, "brancoEsquerda");
            int x = apl.getX();
            int y = apl.getY();
            conduzir(40, "brancoEsquerda", "brancoBaixo");
            assertEquals(x, apl.getX(), "borda X");
            assertTrue(apl.getY() > y, "mas a profundidade anda");
            assertEquals(apl.getY(), rec.getY());
            dentroDaArena();
        });
    }

    // 41, 44. Folego: so o aplicador paga, pela distancia efetiva; nunca negativo
    @Test
    void folegoPelaDistancia() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            double fa = apl.getFolegoAtual();
            double fr = rec.getFolegoAtual();
            assertEquals(ResultadoAcaoTecnica.SUCESSO, s().deslocarPegadaControlada(apl, 0, 1, MIN, MAX));
            assertEquals(fa - CustosFolego.ARRASTO_POR_PIXEL, apl.getFolegoAtual(), 1e-9, "1 px de profundidade");
            assertEquals(fr, rec.getFolegoAtual(), 1e-9, "receptor nao paga");
            fa = apl.getFolegoAtual();
            int x = apl.getX();
            int y = apl.getY();
            for (int i = 0; i < 20; i++) {
                s().deslocarPegadaControlada(apl, 1, 1, MIN, MAX);
            }
            double andou = Math.hypot(apl.getX() - x, apl.getY() - y);
            assertEquals(fa - CustosFolego.ARRASTO_POR_PIXEL * andou, apl.getFolegoAtual(), 0.002,
                    "custo pelo que andou na diagonal");
            apl.consumirFolegoCombate(apl.getFolegoAtual());
            for (int i = 0; i < 30; i++) {
                s().deslocarPegadaControlada(apl, 1, 0, MIN, MAX);
            }
            assertTrue(apl.getFolegoAtual() >= 0, "nunca negativo");
        });
    }

    // 45, 46. O receptor nao escapa andando nem mudando de linha (5L preservada, tambem durante o arrasto)
    @Test
    void receptorNaoEscapa() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            int x = rec.getX();
            int linha = rec.getLinhaAtual();
            for (String d : new String[] {"pretoDireita", "pretoCima", "pretoBaixo"}) {
                tecla(d, true);
                ticks(60);
                tecla(d, false);
            }
            ticks(1);
            assertTrue(s().isInteracaoEstabelecida());
            assertEquals(x, rec.getX());
            assertEquals(linha, rec.getLinhaAtual());
            // forcando para cima enquanto e levado para baixo: vai junto com o aplicador
            tecla("pretoCima", true);
            conduzir(30, "brancoBaixo");
            tecla("pretoCima", false);
            assertEquals(apl.getLinhaAtual(), rec.getLinhaAtual());
            assertEquals(apl.getY(), rec.getY());
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // 47, 48, 49, 50. ROMPER, resistencia, reforco e desequilibrio continuam (depois de arrastar)
    @Test
    void mecanicasDaPegadaContinuam() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            conduzir(20, "brancoBaixo");
            double f = s().getFirmeza();
            tecla("pretoDireita", true);
            ticks(30);
            tecla("pretoDireita", false);
            assertTrue(s().getFirmeza() < f - 30 * SistemaCombate.DESGASTE_FIRMEZA_POR_TICK, "resistencia desgasta");
            f = s().getFirmeza();
            toque("brancoReforco");
            assertTrue(s().getFirmeza() > f, "reforco");
            ticks(SistemaCombate.RECARGA_ACAO_TECNICA_TICKS);
            rec.consumirFolegoCombate(rec.getFolegoAtual());
            for (int i = 0; i < 6 && rec.getEfeitos()
                    .getTicksRestantes(com.irmaosmiau.entities.EfeitosTemporarios.Tipo.DESEQUILIBRIO) == 0; i++) {
                toque("brancoDesequilibrio");
                ticks(SistemaCombate.RECARGA_ACAO_TECNICA_TICKS);
            }
            assertTrue(rec.getEfeitos().getTicksRestantes(com.irmaosmiau.entities.EfeitosTemporarios.Tipo.DESEQUILIBRIO) > 0
                    || s().getUltimaDisputa() != null, "desequilibrio disputado pelo motor");
            // ROMPER (receptor com folego, aplicador exausto) continua sendo a saida
            pegada(InicioLuta.EM_PE);
            conduzir(20, "brancoCima", "brancoDireita");
            apl.consumirFolegoCombate(apl.getFolegoAtual());
            toque("pretoRompimento");
            assertFalse(s().estaEmInteracao(), "ROMPER continua sendo a saida");
        });
    }

    // 52, 55. Passos dos dois durante o arrasto (X e profundidade); parados, volta a pose sustentada
    @Test
    void passosSincronizadosEVoltaASustentada() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            tecla("brancoControle", true);
            tecla("brancoBaixo", true);
            ticks(6);
            assertTrue(painel.getAnimador().getCamada().faseArrasto(apl) >= 0, "passos do aplicador na profundidade");
            assertTrue(painel.getAnimador().getCamada().faseArrasto(rec) >= 0, "passos do receptor junto");
            assertEquals(1, painel.getAnimador().getCamada().direcaoPuxao(rec)[1]);
            tecla("brancoBaixo", false);
            tecla("brancoControle", false);
            ticks(2);
            assertEquals(-1, painel.getAnimador().getCamada().faseArrasto(rec));
            assertEquals(FaseReceptor.SUSTENTADA, painel.getAnimador().getCamada().faseReceptor(rec));
        });
    }

    // 22, 65. De joelhos: a dupla e conduzida sem levantar
    @Test
    void arrastoDeJoelhos() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.DE_JOELHOS);
            conduzir(30, "brancoBaixo", "brancoDireita");
            assertTrue(apl.isAjoelhado() && rec.isAjoelhado(), "ninguem levanta");
            assertEquals(apl.getLinhaAtual(), rec.getLinhaAtual());
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // 59, 60, 61. J2 no Xbox (Contextual, simulado) conduz para outra linha; J1 no teclado e o receptor
    @Test
    void j2NoGamepadConduzEmProfundidade() throws Exception {
        sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID0", "Xbox 1");
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_2, TipoDispositivo.GAMEPAD)));
        PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(
                MapaGamepad.padrao().comEsquema(SlotJogador.JOGADOR_2, EsquemaGamepad.CONTEXTUAL)));
        naEdt(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(1);
            apl = painel.getLutador2();
            rec = painel.getLutador1();
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f));
            for (int i = 0; i < 300 && !s().podeTentarAcao(apl, rec, "pegada_1"); i++) {
                ticks(1);
            }
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_Y, -1f));
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            assertSame(apl, s().getAplicadorInteracao());
            int linha = apl.getLinhaAtual();
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_Y, 1f));
            ticks(30);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertTrue(apl.getLinhaAtual() > linha, "o Xbox (simulado) levou a dupla para a frente");
            assertEquals(apl.getLinhaAtual(), rec.getLinhaAtual());
            assertTrue(s().isInteracaoEstabelecida());
            // J1 (teclado, receptor) continua independente: forca sem sair
            tecla("brancoEsquerda", true);
            ticks(20);
            tecla("brancoEsquerda", false);
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // 63. Pausa: ninguem anda e nada custa
    @Test
    void pausaCongelaOArrasto() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            painel.alternarPausaParaTestes();
            int x = apl.getX();
            int y = apl.getY();
            conduzir(30, "brancoDireita", "brancoBaixo");
            assertEquals(x, apl.getX());
            assertEquals(y, apl.getY());
            painel.alternarPausaParaTestes();
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // 67, 68. No outro mapa e com tempo de partida: mesmas bordas e o relogio corre
    @Test
    void outroMapaEComTempo() throws Exception {
        naEdt(() -> {
            GamePanel g = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { },
                    () -> { });
            g.configurarPartida("original", 90);
            estabelecer(g, true, 1);
            int antes = painel.getSegundosRestantes();
            conduzirLongo(600, "brancoEsquerda", "brancoBaixo");
            dentroDaArena();
            assertEquals(MIN, Math.min(apl.getX(), rec.getX()));
            assertTrue(painel.getSegundosRestantes() < antes, "o relogio correu durante o arrasto");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // 66. Treino: parceiro passivo e conduzido e reage
    @Test
    void treinoComParceiroPassivo() throws Exception {
        naEdt(() -> {
            ConfiguracaoTreino c = ConfiguracaoTreino.padrao().comInicio(InicioLuta.DE_JOELHOS);
            painel = new GamePanel(c, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(1);
            apl = painel.getLutador1();
            rec = painel.getLutador2();
            toque("brancoPegada1");
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida());
            conduzir(25, "brancoBaixo");
            assertEquals(apl.getLinhaAtual(), rec.getLinhaAtual());
            assertEquals(FaseReceptor.SUSTENTADA, painel.getAnimador().getCamada().faseReceptor(rec));
        });
    }

    // 62. F3 com o arrasto e o receptor (linhas curtas, cabem na tela)
    @Test
    void f3MostraArrastoEReceptor() throws Exception {
        naEdt(() -> {
            pegada(InicioLuta.EM_PE);
            tecla("brancoControle", true);
            tecla("brancoBaixo", true);
            ticks(3);
            String tudo = String.join("\n", painel.linhasTecnicas());
            assertTrue(tudo.contains("ARRASTO: FRENTE OK"), tudo);
            assertTrue(tudo.contains("RECEPTOR: J2"), tudo);
            assertTrue(tudo.contains("SUSTENTADA"), tudo);
            assertTrue(tudo.contains("LINHAS J1"), tudo);
            tecla("brancoBaixo", false);
            tecla("brancoControle", false);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
            for (String l : painel.linhasTecnicas()) {
                assertTrue(g.getFontMetrics().stringWidth(l) < 780, "linha do F3 sai da tela: " + l);
            }
            g.dispose();
        });
    }
}
