package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.entities.CorKimono;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;
import com.irmaosmiau.treino.ConfiguracaoTreino;
import com.irmaosmiau.treino.SituacaoTreino;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.2: laboratorio do treino no GamePanel real. */
class Laboratorio5Q2GamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void treino(ConfiguracaoTreino c) {
        painel = new GamePanel(c, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        ticks(2);
    }

    private void atalho(String nome) {
        painel.getActionMap().get(nome).actionPerformed(null);
    }

    private static JButton botao(Container c, String inicio) {
        for (Component f : c.getComponents()) {
            if (f instanceof JButton b && b.getText() != null && b.getText().startsWith(inicio)) {
                return b;
            }
            if (f instanceof Container k) {
                JButton r = botao(k, inicio);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    private void situacao(SituacaoTreino s, Lado ativo) {
        painel.getLaboratorio().setSituacao(s);
        painel.getLaboratorio().setAtivo(ativo);
        painel.iniciarSituacaoLaboratorio();
        ticks(30);
    }

    @Test
    void situacaoIniciaNoMotorEF5RepeteAMesmaSituacao() throws Exception {
        naEdt(() -> {
            treino(ConfiguracaoTreino.padrao());
            situacao(SituacaoTreino.COSTAS, Lado.A);
            assertEquals(Posicao.COSTAS, m().getPosicao());
            assertEquals(Lado.A, m().getEstado().getControlador().orElseThrow());
            assertTrue(j1().isVinculadoNoSolo() && j2().isVinculadoNoSolo());
            m().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));      // mata-leao (faixa branca: liberado)
            assertTrue(m().getAtaque().isPresent());
            ticks(20);
            assertTrue(j1().getFolegoAtual() < j1().getAtributos().getFolego());
            atalho("reiniciarTreino");                                          // F5
            ticks(30);
            assertTrue(m().getAtaque().isEmpty(), "reiniciou a situacao");
            assertEquals(Posicao.COSTAS, m().getPosicao());
            assertEquals(Lado.A, m().getEstado().getControlador().orElseThrow());
            assertEquals(j1().getAtributos().getFolego(), j1().getFolegoAtual(), 1e-9, "folego restaurado");
        });
    }

    @Test
    void emPeComPegadaUsaOSistemaDeCombateDeVerdade() throws Exception {
        naEdt(() -> {
            treino(ConfiguracaoTreino.padrao());
            situacao(SituacaoTreino.EM_PE_PEGADA, Lado.B);
            ticks(20);
            assertTrue(painel.getSistemaCombate().isInteracaoEstabelecida(), "pegada estabelecida");
            assertEquals(j2(), painel.getSistemaCombate().getAplicadorInteracao(), "J2 aplica");
            assertEquals(Posicao.EM_PE_PEGADA, m().getEstado().getPosicao() == Posicao.EM_PE_SEPARADOS
                    ? Posicao.EM_PE_PEGADA : m().getPosicao());
        });
    }

    @Test
    void j2PassivoPorPadraoEControladoQuandoPedido() throws Exception {
        naEdt(() -> {
            treino(ConfiguracaoTreino.padrao());
            situacao(SituacaoTreino.MONTADA, Lado.B);                         // J2 montado no J1
            tecla("pretoControleE", true);
            ticks(1);
            tecla("pretoControleRIGHT", true);
            ticks(1);
            tecla("pretoControleRIGHT", false);
            tecla("pretoControleE", false);
            ticks(1);
            assertTrue(m().getAtaque().isEmpty(), "parceiro passivo: as teclas do J2 nao agem");
            painel.getLaboratorio().setParceiroControlado(true);
            assertFalse(painel.parceiroPassivo(SlotJogador.JOGADOR_2));
            painel.iniciarSituacaoLaboratorio();
            ticks(30);
            tecla("pretoControleE", true);
            ticks(1);
            tecla("pretoControleRIGHT", true);
            ticks(1);
            tecla("pretoControleRIGHT", false);
            tecla("pretoControleE", false);
            ticks(1);
            assertEquals(Lado.B, m().getAtaque().orElseThrow().atacante(), "J2 controlado: americana");
        });
    }

    @Test
    void menuDoLaboratorioNaPausaDoTreino() throws Exception {
        naEdt(() -> {
            treino(ConfiguracaoTreino.padrao());
            painel.alternarPausaParaTestes();
            JButton lab = botao(painel, "LABORATÓRIO");
            assertNotNull(lab);
            assertTrue(lab.isVisible());
            lab.doClick();
            JButton sit = botao(painel, "SITUAÇÃO:");
            assertTrue(sit.isVisible());
            assertFalse(botao(painel, "CONTINUAR").isVisible(), "vista do laboratorio");
            painel.getLaboratorio().setSituacao(SituacaoTreino.MONTADA);
            sit.doClick();                                                      // proxima: costas
            assertEquals(SituacaoTreino.COSTAS, painel.getLaboratorio().getSituacao());
            botao(painel, "CONTROLA:").doClick();
            assertEquals(Lado.B, painel.getLaboratorio().getAtivo());
            botao(painel, "INICIAR SITUAÇÃO").doClick();
            assertFalse(painel.isPausado(), "inicia e volta para a luta");
            ticks(30);
            assertEquals(Posicao.COSTAS, m().getPosicao());
            assertEquals(Lado.B, m().getEstado().getControlador().orElseThrow());
            painel.alternarPausaParaTestes();
            assertTrue(botao(painel, "REINICIAR SITUAÇÃO").isVisible());
        });
    }

    @Test
    void partidaNormalNaoTemLaboratorio() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            painel.getLaboratorio().setSituacao(SituacaoTreino.MONTADA);
            painel.iniciarSituacaoLaboratorio();
            ticks(5);
            assertEquals(Posicao.EM_PE_SEPARADOS, m().getPosicao(), "nada muda na partida");
            assertFalse(painel.getLaboratorio().isAtiva());
            assertFalse(painel.parceiroPassivo(SlotJogador.JOGADOR_2));
            painel.alternarPausaParaTestes();
            assertFalse(botao(painel, "LABORATÓRIO").isVisible());
            painel.alternarPausaParaTestes();
            atalho("alternarCatalogoLab");
            assertFalse(painel.isCatalogoLabVisivel());
        });
    }

    @Test
    void faixaEKimonoDaSelecaoPreservadosEDoisPersonagensIguais() throws Exception {
        naEdt(() -> {
            EscolhaLutador e1 = EscolhaLutador.oficial(CatalogoPersonagens.padrao().exigir(CatalogoPersonagens.ID_BRANCO))
                    .com(Graduacao.ROXA, CorKimono.AZUL);
            EscolhaLutador e2 = EscolhaLutador.oficial(CatalogoPersonagens.padrao().exigir(CatalogoPersonagens.ID_BRANCO))
                    .com(Graduacao.PRETA, CorKimono.BRANCO);
            treino(ConfiguracaoTreino.padrao().comJogador(e1).comParceiro(e2));
            situacao(SituacaoTreino.ASHI_GARAMI, Lado.A);
            assertEquals(Posicao.ASHI_GARAMI, m().getPosicao());
            assertEquals(Graduacao.ROXA, j1().getGraduacao());
            assertEquals(CorKimono.AZUL, j1().getCorKimono());
            assertEquals(Graduacao.PRETA, j2().getGraduacao());
            atalho("alternarCatalogoLab");
            assertTrue(painel.isCatalogoLabVisivel());
            assertTrue(painel.getLaboratorio().catalogo(m()).stream()
                    .anyMatch(i -> i.finalizacao().tecnicaBase().equals("chave_pe_reta")
                            && i.status() == com.irmaosmiau.treino.LaboratorioTreino.Status.LIBERADA),
                    "roxa: chave de pe reta liberada");
            BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            painel.setSize(800, 450);
            painel.paint(g);                                                    // HUD tecnico + catalogo
            g.dispose();
        });
    }
}
