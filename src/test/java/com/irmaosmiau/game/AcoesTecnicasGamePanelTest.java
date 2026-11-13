package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.MotivoEncerramentoInteracao;
import com.irmaosmiau.combate.Pegada1RegressaoArraysTest;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.TipoPersonagem;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5A: teclas T/U no GamePanel real, HUD, treino e F5. */
class AcoesTecnicasGamePanelTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private GamePanel painel;

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private void tecla(String nome, boolean pressionada) {
        painel.getActionMap().get(nome + (pressionada ? "Pressionada" : "Solta")).actionPerformed(null);
    }

    private void toque(String nome) {
        tecla(nome, true);
        painel.atualizarUmTickParaTestes();
        tecla(nome, false);
        painel.atualizarUmTickParaTestes();
    }

    /** Aproxima com D, aperta H e espera a entrada terminar. */
    private void estabelecerPegada() {
        SistemaCombate s = painel.getSistemaCombate();
        tecla("brancoDireita", true);
        for (int i = 0; i < 200 && !s.podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
            painel.atualizarUmTickParaTestes();
        }
        tecla("brancoDireita", false);
        toque("brancoPegada1");
        for (int i = 0; i < 31; i++) {
            painel.atualizarUmTickParaTestes();
        }
        assertTrue(s.isInteracaoEstabelecida());
    }

    private GamePanel doisJogadores() {
        GamePanel g = new GamePanel(TipoPersonagem.AMBOS, () -> { }, () -> { }, () -> { });
        g.pararLoopParaTestes();
        return g;
    }

    @Test
    void teclaTDesequilibraUmaVezPorToqueEOHudMostra() throws Exception {
        naEdt(() -> {
            painel = doisJogadores();
            estabelecerPegada();
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            double folego = b.getFolegoAtual();

            tecla("brancoDesequilibrio", true);
            for (int i = 0; i < 90; i++) {           // segurar a tecla > recarga
                painel.atualizarUmTickParaTestes();
            }
            tecla("brancoDesequilibrio", false);

            // Uma cobranca (4) e nao duas (8), descontando a recuperacao natural dos 90 ticks.
            double gasto = folego - b.getFolegoAtual();
            assertTrue(gasto > SistemaCombate.CUSTO_DESEQUILIBRIO - 2.0
                    && gasto < SistemaCombate.CUSTO_DESEQUILIBRIO + 0.01,
                    "segurar T conta uma unica acao (gasto " + gasto + ")");
            assertTrue(p.getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));
            assertTrue(painel.montarEstadoHud().getEstadoInteracao().contains("DESEQUILIBRADO"),
                    painel.montarEstadoHud().getEstadoInteracao());
            assertTrue(painel.getSistemaCombate().isInteracaoEstabelecida());
            assertArrayEquals(Pegada1RegressaoArraysTest.RECEPTOR_APROVADO[2], p.copiarPoseCombateExterna());
        });
    }

    @Test
    void teclaTEmSeguidaMostraRecargaESemPegadaAvisa() throws Exception {
        naEdt(() -> {
            painel = doisJogadores();
            toque("brancoDesequilibrio");
            assertEquals("DESEQUILÍBRIO: só com a pegada estabelecida", painel.montarEstadoHud().getMensagem());

            estabelecerPegada();
            toque("brancoDesequilibrio");
            assertEquals("DESEQUILIBROU!", painel.montarEstadoHud().getMensagem());
            toque("brancoDesequilibrio");
            assertEquals("DESEQUILÍBRIO: aguarde", painel.montarEstadoHud().getMensagem());
        });
    }

    @Test
    void segundoJogadorRompeComUQuandoOAplicadorEstaCansado() throws Exception {
        naEdt(() -> {
            painel = doisJogadores();
            estabelecerPegada();
            IrmaoMiau b = painel.getLutador1();
            IrmaoMiau p = painel.getLutador2();
            int xp = p.getX();

            toque("pretoRompimento");
            assertEquals("DEFESA FALHOU", painel.montarEstadoHud().getMensagem());
            assertTrue(painel.getSistemaCombate().isInteracaoEstabelecida());

            b.consumirFolegoCombate(b.getFolegoAtual());       // aplicador exausto
            for (int i = 0; i < SistemaCombate.RECARGA_ACAO_TECNICA_TICKS; i++) {
                painel.atualizarUmTickParaTestes();
            }
            toque("pretoRompimento");
            assertEquals(MotivoEncerramentoInteracao.ROMPIMENTO_TECNICO,
                    painel.getSistemaCombate().getUltimoMotivoEncerramento());
            assertFalse(painel.getSistemaCombate().estaEmInteracao());
            assertEquals("PEGADA ROMPIDA!", painel.montarEstadoHud().getMensagem(),
                    "nao e sobrescrita por 'Pegada desfeita'");
            assertEquals(xp, p.getX(), "rompeu parado");
            assertFalse(p.isPoseCombateExternaAtiva());
        });
    }

    @Test
    void parceiroDoTreinoNaoDefendeNemObedeceAoU() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            estabelecerPegada();
            IrmaoMiau p = painel.getLutador2();
            double fp = p.getFolegoAtual();

            toque("pretoRompimento");
            for (int i = 0; i < 300; i++) {
                painel.atualizarUmTickParaTestes();
            }
            assertTrue(painel.getSistemaCombate().isInteracaoEstabelecida(), "nenhuma defesa automatica");
            assertNull(painel.getSistemaCombate().getUltimaDisputa());
            assertTrue(p.getFolegoAtual() >= fp);
        });
    }

    @Test
    void f5LimpaEfeitosERecarga() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            estabelecerPegada();
            toque("brancoDesequilibrio");
            IrmaoMiau parceiroAntigo = painel.getLutador2();
            assertTrue(parceiroAntigo.getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO));

            painel.getActionMap().get("reiniciarTreino").actionPerformed(null);

            assertNotSame(parceiroAntigo, painel.getLutador2());
            assertFalse(painel.getLutador2().getEfeitos().temAlgumAtivo());
            assertEquals(0, painel.getSistemaCombate().getRecargaRestante(painel.getLutador1()));
            assertNull(painel.getSistemaCombate().getUltimaDisputa());
            assertEquals(25.0, painel.getLutador1().getFolegoAtual());
        });
    }

    @Test
    void perfilUsaAtributosReaisENaoTemOArquivoAlterado() throws Exception {
        ServicoPerfis s = new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao());
        s.criar("branco_fraco", "Branco Fraco", "miau_branco", new AtributosLutador(35, 25, 5, 22, 25, 5, 22, 45));
        EscolhaLutador fraco = new OpcoesDeLutadores(CatalogoPersonagens.padrao(), s)
                .paraBase("miau_branco").getValor().get(1);
        Path arquivo = tmp.resolve("perfis/branco_fraco.json");
        byte[] antes = Files.readAllBytes(arquivo);

        naEdt(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, fraco, null, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            estabelecerPegada();
            toque("brancoDesequilibrio");
            assertEquals("TENTATIVA DE DESEQUILÍBRIO: RESISTIU", painel.montarEstadoHud().getMensagem(),
                    "perfil sem forca/tecnica nao desequilibra o Preto oficial");
            assertFalse(painel.getLutador2().getEfeitos().temAlgumAtivo());
            assertEquals(List.of(painel.getLutador1(), painel.getLutador2()), painel.ordemDeDesenho());
        });
        assertArrayEquals(antes, Files.readAllBytes(arquivo));
    }
}
