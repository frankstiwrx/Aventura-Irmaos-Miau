package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.cenario.CenarioCampeonato;
import com.irmaosmiau.cenario.CenarioFachadaAcademia;
import com.irmaosmiau.cenario.CenarioFortalezaNevada;
import com.irmaosmiau.cenario.Cenario;
import com.irmaosmiau.cenario.CenarioDojo;
import com.irmaosmiau.entities.CorKimono;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.regras.Graduacao;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.1 no jogo real: partida e treino recebem faixa/kimono das escolhas;
 * reiniciar recria pela mesma escolha (sem bonus duplicado); arenas vivas.
 */
class Graduacao5Q1GamePanelTest extends Base5OGamePanel {

    private static EscolhaLutador oficial(String id, Graduacao g, CorKimono k) {
        return EscolhaLutador.oficial(CatalogoPersonagens.padrao().exigir(id)).com(g, k);
    }

    @Test
    void partidaUsaFaixaKimonoEAtributosCalculadosDosDoisSlots() throws Exception {
        AtomicInteger reinicios = new AtomicInteger();
        naEdt(() -> {
            EscolhaLutador e1 = oficial(CatalogoPersonagens.ID_BRANCO, Graduacao.PRETA, CorKimono.AZUL);
            EscolhaLutador e2 = oficial(CatalogoPersonagens.ID_BRANCO, Graduacao.AZUL, CorKimono.BRANCO);
            painel = new GamePanel(TipoPersonagem.AMBOS, e1, e2, InicioLuta.EM_PE, () -> { }, () -> { },
                    reinicios::incrementAndGet);
            painel.pararLoopParaTestes();
            ticks(2);
            assertEquals(Graduacao.PRETA, j1().getGraduacao());
            assertEquals(CorKimono.AZUL, j1().getCorKimono());
            assertEquals(78, j1().getAtributos().getForca(), 1e-9, "atributos usados no jogo");
            assertEquals(Graduacao.AZUL, j2().getGraduacao());
            assertEquals(CorKimono.BRANCO, j2().getCorKimono());
            assertEquals(50, j2().getAtributos().getForca(), 1e-9);
            assertTrue(painel.montarEstadoHud().getLutadores().get(0).textoFolego().endsWith("/47"),
                    "HUD com o folego da faixa preta");
            // reiniciar (mesma escolha) nao acumula
            GamePanel outro = new GamePanel(TipoPersonagem.AMBOS, e1, e2, InicioLuta.EM_PE, () -> { }, () -> { },
                    () -> { });
            outro.pararLoopParaTestes();
            assertEquals(78, outro.getLutador1().getAtributos().getForca(), 1e-9);
        });
    }

    @Test
    void treinoRecebeAsEscolhasDoJogadorEDoParceiro() throws Exception {
        naEdt(() -> {
            ConfiguracaoTreino c = ConfiguracaoTreino.padrao()
                    .comJogador(oficial(CatalogoPersonagens.ID_PRETO, Graduacao.ROXA, CorKimono.PRETO))
                    .comParceiro(oficial(CatalogoPersonagens.ID_BRANCO, Graduacao.MARROM, CorKimono.AZUL));
            painel = new GamePanel(c, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(2);
            assertEquals(Graduacao.ROXA, j1().getGraduacao());
            assertEquals(CorKimono.PRETO, j1().getCorKimono());
            assertEquals(Graduacao.MARROM, j2().getGraduacao());
            assertEquals(CorKimono.AZUL, j2().getCorKimono());
            assertEquals(68, j1().getAtributos().getPeso(), 1e-9, "peso do Preto preservado");
        });
    }

    // ---------------- arenas vivas
    private static BufferedImage fundo(Cenario c) {
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        c.desenharFundo(g, 800, 450);
        g.dispose();
        return img;
    }

    private static boolean iguais(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < 450; y += 2) {
            for (int x = 0; x < 800; x += 2) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void arenasVivasMudamComOTickSemTocarNoChaoNemNoPrimeiroPlano() {
        for (Cenario c : new Cenario[] {new CenarioFortalezaNevada(), new CenarioCampeonato(), new CenarioFachadaAcademia()}) {
            c.atualizarAmbiente(10);
            BufferedImage a = fundo(c);
            assertTrue(iguais(a, fundo(c)), c.getId() + ": o mesmo tick desenha a mesma imagem");
            c.atualizarAmbiente(70);
            assertTrue(!iguais(a, fundo(c)), c.getId() + ": o ambiente se mexe");
            BufferedImage frente = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frente.createGraphics();
            c.desenharPrimeiroPlano(g, 800, 450);
            g.dispose();
            int y0 = CenarioDojo.yCentroLinha(CenarioDojo.TOTAL_LINHAS - 1) + CenarioDojo.ESPACO_LINHAS - 14;
            for (int y = 0; y < y0; y += 3) {
                for (int x = 0; x < 800; x += 5) {
                    assertEquals(0, frente.getRGB(x, y) >>> 24, c.getId() + ": nada na frente dos lutadores");
                }
            }
        }
    }
}
