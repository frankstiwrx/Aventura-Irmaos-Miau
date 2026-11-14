package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.Pegada1RegressaoArraysTest;
import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.hud.DadosLutadorHud;
import com.irmaosmiau.hud.HudCombate;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.selecao.OpcoesDeLutadores;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Entrega 4D: perfis personalizados jogando de verdade no GamePanel real.
 */
class PerfisJogaveisGamePanelTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    /** Branco: folego 60, condicionamento 50, velocidade 100. */
    private static final AtributosLutador BRANCO_TURBO = new AtributosLutador(50, 60, 42, 100, 25, 25, 22, 72);
    /** Preto: folego 80, condicionamento 70. */
    private static final AtributosLutador PRETO_RESISTENTE = new AtributosLutador(70, 80, 18, 45, 45, 28, 38, 68);

    private EscolhaLutador brancoTurbo;
    private EscolhaLutador pretoResistente;
    private GamePanel painel;

    @BeforeEach
    void criarPerfis() {
        ServicoPerfis s = new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao());
        s.criar("branco_turbo", "Branco Turbo", "miau_branco", BRANCO_TURBO);
        s.criar("preto_resistente", "Preto Resistente", "miau_preto", PRETO_RESISTENTE);
        OpcoesDeLutadores opcoes = new OpcoesDeLutadores(CatalogoPersonagens.padrao(), s);
        brancoTurbo = opcoes.paraBase("miau_branco").getValor().get(1);
        pretoResistente = opcoes.paraBase("miau_preto").getValor().get(1);
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private GamePanel jogo(TipoPersonagem tipo, EscolhaLutador b, EscolhaLutador p) {
        GamePanel g = new GamePanel(tipo, b, p, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    private GamePanel treino(EscolhaLutador jogador, EscolhaLutador parceiro) {
        ConfiguracaoTreino c = ConfiguracaoTreino.padrao();
        if (jogador != null) {
            c = c.comJogador(jogador);
        }
        if (parceiro != null) {
            c = c.comParceiro(parceiro);
        }
        GamePanel g = new GamePanel(c, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    private void tecla(String nome) {
        painel.getActionMap().get(nome).actionPerformed(null);
    }

    // =====================================================
    // MODO INDIVIDUAL
    // =====================================================
    @Test
    void individualComPerfilDoBrancoUsaOsAtributosEOHudMostraOPerfil() throws Exception {
        naEdt(() -> {
            painel = jogo(TipoPersonagem.BRANCO, brancoTurbo, null);
            IrmaoMiau l = painel.getLutador1();
            assertInstanceOf(IrmaoMiauBranco.class, l);
            assertEquals("Branco Turbo", l.getNome());
            assertEquals(60.0, l.getFolegoAtual());

            DadosLutadorHud hud = painel.montarEstadoHud().getLutadores().get(0);
            assertEquals("Branco Turbo", hud.getNome());
            assertEquals("60/60", hud.textoFolego());
            assertEquals("50/50", hud.textoCondicionamento());

            // Velocidade do perfil no movimento real (WASD).
            int x0 = l.getX();
            tecla("brancoDireitaPressionada");
            for (int i = 0; i < 10; i++) {
                painel.atualizarUmTickParaTestes();
            }
            tecla("brancoDireitaSolta");
            assertEquals(x0 + 48, l.getX(), "10 ticks x (3.2 + 1.6) = 48 px");
        });
    }

    @Test
    void individualComPerfilDoPreto() throws Exception {
        naEdt(() -> {
            painel = jogo(TipoPersonagem.PRETO, null, pretoResistente);
            assertInstanceOf(IrmaoMiauPreto.class, painel.getLutador1());
            assertEquals("Preto Resistente", painel.montarEstadoHud().getLutadores().get(0).getNome());
            assertEquals("80/80", painel.montarEstadoHud().getLutadores().get(0).textoFolego());
        });
    }

    @Test
    void semEscolhaContinuaComOsOficiais() throws Exception {
        naEdt(() -> {
            painel = jogo(TipoPersonagem.AMBOS, null, null);
            assertEquals("Irmão Miau Branco", painel.getLutador1().getNome());
            assertEquals(25.0, painel.getLutador1().getFolegoAtual());
            assertEquals(40.0, painel.getLutador2().getFolegoAtual());
        });
    }

    // =====================================================
    // DOIS JOGADORES
    // =====================================================
    @Test
    void doisJogadoresComPerfisCompativeis() throws Exception {
        naEdt(() -> {
            painel = jogo(TipoPersonagem.AMBOS, brancoTurbo, pretoResistente);
            List<DadosLutadorHud> hud = painel.montarEstadoHud().getLutadores();
            assertEquals("Branco Turbo", hud.get(0).getNome());
            assertEquals("Preto Resistente", hud.get(1).getNome());
            assertEquals("80/80", hud.get(1).textoFolego());
            assertEquals("70/70", hud.get(1).textoCondicionamento());

            // Controles de cada slot preservados: setas movem o Preto (perfil).
            int x = painel.getLutador2().getX();
            tecla("pretoEsquerdaPressionada");
            painel.atualizarUmTickParaTestes();
            tecla("pretoEsquerdaSolta");
            assertTrue(painel.getLutador2().getX() < x);
        });
    }

    @Test
    void perfilDeOutraBaseEmQualquerSlot() {
        // 5I (pedido): antes "perfilNoSlotErradoERecusado" (assertThrows). Agora qualquer irmao
        // (ou perfil dele) joga em qualquer slot; os controles continuam os do slot.
        GamePanel a = jogo(TipoPersonagem.AMBOS, pretoResistente, null);
        assertTrue(a.getLutador1() instanceof com.irmaosmiau.entities.IrmaoMiauPreto, "perfil do Preto no J1");
        GamePanel b = jogo(TipoPersonagem.PRETO, null, brancoTurbo);
        assertTrue(b.getLutador1() instanceof com.irmaosmiau.entities.IrmaoMiauBranco, "perfil do Branco no J2 (sozinho)");
    }

    // =====================================================
    // TREINO, F5 E PEGADA_1
    // =====================================================
    @Test
    void treinoComPerfisEF5RestauraOsRecursosDoPerfil() throws Exception {
        naEdt(() -> {
            painel = treino(brancoTurbo, pretoResistente);
            IrmaoMiau jogador = painel.getLutador1();
            IrmaoMiau parceiro = painel.getLutador2();
            assertEquals("Branco Turbo", jogador.getNome());
            assertEquals("Preto Resistente", parceiro.getNome());
            assertTrue(painel.montarEstadoHud().isModoTreino());

            // Parceiro passivo continua parado.
            int xParceiro = parceiro.getX();
            tecla("pretoEsquerdaPressionada");
            for (int i = 0; i < 10; i++) {
                painel.atualizarUmTickParaTestes();
            }
            tecla("pretoEsquerdaSolta");
            assertEquals(xParceiro, parceiro.getX());

            jogador.consumirFolegoCombate(40);
            assertEquals(20.0, jogador.getFolegoAtual(), 0.5);

            tecla("reiniciarTreino");   // F5

            IrmaoMiau novo = painel.getLutador1();
            assertNotSame(jogador, novo);
            assertEquals("Branco Turbo", novo.getNome());
            assertEquals(60.0, novo.getFolegoAtual(), "folego cheio DO PERFIL, nao os 25 do oficial");
            assertEquals(50.0, novo.getCondicionamentoAtual());
            assertEquals(80.0, painel.getLutador2().getFolegoAtual());
            assertEquals(ConfiguracaoTreino.X_JOGADOR_PADRAO, novo.getX());
        });
    }

    @Test
    void pegada1ComPerfilSegueAsRegrasEUsaOFolegoDoPerfil() throws Exception {
        naEdt(() -> {
            painel = treino(brancoTurbo, null);
            SistemaCombate s = painel.getSistemaCombate();
            IrmaoMiau j = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();

            tecla("brancoDireitaPressionada");
            for (int i = 0; i < 200 && !s.podeTentarAcao(j, p, "pegada_1"); i++) {
                painel.atualizarUmTickParaTestes();
            }
            tecla("brancoDireitaSolta");
            double folego = j.getFolegoAtual();
            assertTrue(folego > 55, "folego do perfil (60), nao do oficial (25)");

            tecla("brancoPegada1Pressionada");
            painel.atualizarUmTickParaTestes();
            tecla("brancoPegada1Solta");
            assertTrue(s.estaEmPegada1());
            assertEquals("/conteudo/miaustudio/PEGADA_1.json",
                    s.getRegistroAnimacoes().getOrigem("pegada_1_entrada"));

            // H e processado depois do sistema.atualizar() do tick: a animacao
            // comeca no frame 0 ao fim desse tick.
            for (int f = 0; f < 30; f++) {
                assertArrayEquals(Pegada1RegressaoArraysTest.APLICADOR_APROVADO[f / 10],
                        j.copiarPoseCombateExterna(), "quadro do JSON no frame " + f);
                painel.atualizarUmTickParaTestes();
            }
            painel.atualizarUmTickParaTestes();
            assertTrue(s.isInteracaoEstabelecida());
        });
    }

    @Test
    void perfilComPoucoFolegoNaoConsegueAPegada() throws Exception {
        AtributosLutador cansado = new AtributosLutador(35, 2, 42, 22, 25, 25, 1, 72);
        ServicoPerfis s = new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao());
        s.criar("cansado", "Cansado", "miau_branco", cansado);
        EscolhaLutador esc = new OpcoesDeLutadores(CatalogoPersonagens.padrao(), s)
                .paraBase("miau_branco").getValor().stream()
                .filter(e -> e.getNome().equals("Cansado")).findFirst().orElseThrow();

        naEdt(() -> {
            painel = treino(esc, null);
            IrmaoMiau j = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            j.moverX(p.getX() - j.getX() - 60);
            painel.atualizarUmTickParaTestes();
            assertEquals(ResultadoAcao.INDISPONIVEL, painel.getSistemaCombate().tentarAcao(j, p, "pegada_1"),
                    "folego maximo 2 < custo 3 da PEGADA_1");
            HudCombate.Estado e = painel.montarEstadoHud();
            assertEquals("2/2", e.getLutadores().get(0).textoFolego());
        });
    }

    // =====================================================
    // PROFUNDIDADE, ARQUIVOS E EXCLUSAO
    // =====================================================
    @Test
    void ordemDeProfundidadeContinuaComPerfis() throws Exception {
        naEdt(() -> {
            painel = jogo(TipoPersonagem.AMBOS, brancoTurbo, pretoResistente);
            painel.getLutador1().descerLinha();
            painel.atualizarUmTickParaTestes();
            assertEquals(List.of(painel.getLutador2(), painel.getLutador1()), painel.ordemDeDesenho());
        });
    }

    @Test
    void jogarNaoAlteraOPerfilSalvoEPerfilExcluidoDepoisDaEscolhaAindaJoga() throws Exception {
        Path arquivo = tmp.resolve("perfis/branco_turbo.json");
        byte[] antes = Files.readAllBytes(arquivo);

        naEdt(() -> {
            painel = jogo(TipoPersonagem.AMBOS, brancoTurbo, pretoResistente);
            for (int i = 0; i < 60; i++) {
                painel.atualizarUmTickParaTestes();
            }
        });
        assertArrayEquals(antes, Files.readAllBytes(arquivo), "partida nao grava no perfil");

        Files.delete(arquivo);    // excluido no editor depois de escolhido
        naEdt(() -> {
            painel = jogo(TipoPersonagem.BRANCO, brancoTurbo, null);   // "tentar novamente"
            assertEquals("Branco Turbo", painel.getLutador1().getNome());
        });
        assertFalse(Files.exists(arquivo), "nao foi recriado");
    }

    @Test
    void oficiaisIntactosDepoisDeJogarComPerfis() throws Exception {
        naEdt(() -> {
            painel = treino(brancoTurbo, pretoResistente);
            painel.atualizarUmTickParaTestes();
        });
        assertEquals(25.0, new IrmaoMiauBranco(0, 0, 0.7).getAtributos().getFolego());
        assertEquals(22.0, CatalogoPersonagens.padrao().exigir("miau_branco").getAtributosPadrao().getVelocidade());
        assertEquals(40.0, new IrmaoMiauPreto(0, 0, 0.7).getAtributos().getFolego());
    }
}
