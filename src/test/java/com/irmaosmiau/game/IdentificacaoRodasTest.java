package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.hud.IdentificacaoRodas;
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.Posicao;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Codigos estaveis das rodas no Modo Desenvolvedor (F8): registro completo e sem repeticao, codigos fixos,
 * o codigo acompanha a roda aberta e nada muda na tela sem o F8.
 */
class IdentificacaoRodasTest extends Base5OGamePanel {

    @Test
    void todaRodaPossivelTemCodigoUnicoNoRegistro() {
        Map<String, String> reg = IdentificacaoRodas.registro();
        for (String chave : IdentificacaoRodas.todasAsChavesPossiveis()) {
            assertTrue(reg.containsKey(chave), "roda sem codigo (acrescente no fim de dev/rodas.txt): " + chave);
        }
        Set<String> codigos = new HashSet<>();
        for (String c : reg.values()) {
            assertTrue(c.matches("M-\\d{3}"), c);
            assertTrue(codigos.add(c), "codigo repetido: " + c);
        }
    }

    // os codigos nao mudam entre execucoes (vem do arquivo, nao da ordem de uma lista em tempo de execucao)
    @Test
    void codigosFixos() {
        assertEquals("M-001", IdentificacaoRodas.codigo("EMPE/PEGADAS/LIVRE/UM").orElseThrow());
        assertEquals("M-068", IdentificacaoRodas.codigo("MOTOR/MONTADA/CIMA_CTRL/R2").orElseThrow());
        assertEquals("M-105", IdentificacaoRodas.codigo("MOTOR/DE_LA_RIVA/CIMA/RS").orElseThrow());
        assertEquals("RODA: M-068", IdentificacaoRodas.rotulo("MOTOR/MONTADA/CIMA_CTRL/R2"));
        assertTrue(IdentificacaoRodas.rotulo("NAO/EXISTE").startsWith("RODA: ?"));
    }

    // a mesma posicao vista de cima e de baixo, e familias diferentes, sao rodas diferentes; A e B iguais
    @Test
    void contextosDiferentesTemCodigosDiferentes() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            painel.getMotorSolo().forcarPosicao(Posicao.MONTADA, Lado.A, Lado.A);
            ticks(40);
            String cima = painel.chaveRodaAtual(SlotJogador.JOGADOR_1);
            String baixo = painel.chaveRodaAtual(SlotJogador.JOGADOR_2);
            assertEquals("MOTOR/MONTADA/CIMA_CTRL/RS", cima);
            assertEquals("MOTOR/MONTADA/BAIXO/RS", baixo);
            assertNotEquals(IdentificacaoRodas.codigo(cima), IdentificacaoRodas.codigo(baixo));
            // a mesma configuracao com os lados trocados = o mesmo codigo
            painel.getMotorSolo().forcarPosicao(Posicao.MONTADA, Lado.B, Lado.B);
            ticks(40);
            assertEquals(cima, painel.chaveRodaAtual(SlotJogador.JOGADOR_2));
            // mudou a posicao: a chave acompanha a roda nova
            painel.getMotorSolo().forcarPosicao(Posicao.GUARDA, Lado.A, Lado.B);
            ticks(40);
            assertEquals("MOTOR/GUARDA/CIMA/RS", painel.chaveRodaAtual(SlotJogador.JOGADOR_1));
            assertEquals("MOTOR/GUARDA/BAIXO_CTRL/RS", painel.chaveRodaAtual(SlotJogador.JOGADOR_2));
            assertTrue(IdentificacaoRodas.codigo(painel.chaveRodaAtual(SlotJogador.JOGADOR_2)).isPresent());
        });
    }

    private int[] pintar() {
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        painel.setSize(800, 450);
        painel.paintComponent(g);
        g.dispose();
        return img.getRGB(0, 0, 800, 450, null, 0, 800);
    }

    private static int[] regiao(int[] px, int x0, int y0, int w, int h) {
        int[] r = new int[w * h];
        for (int y = 0; y < h; y++) {
            System.arraycopy(px, (y0 + y) * 800 + x0, r, y * w, w);
        }
        return r;
    }

    // 1 + 2 + 6: sem o F8 nada aparece; com o F8 o codigo aparece ABAIXO da roda, sem tocar nela
    @Test
    void codigoSoComOF8EForaDaRoda() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            painel.getMotorSolo().forcarPosicao(Posicao.MONTADA, Lado.A, Lado.A);
            ticks(40);
            painel.pararLoopParaTestes();
            int[] sem = pintar();
            painel.configurarModoDesenvolvedor(new ModoDesenvolvedor());
            int[] com = pintar();
            int x = 800 - 10 - PainelDpadContextual.LARGURA;                // a roda do J2 (no teclado-controle)
            assertArrayEquals(regiao(sem, x, PainelDpadContextual.Y, PainelDpadContextual.LARGURA,
                    PainelDpadContextual.ALTURA), regiao(com, x, PainelDpadContextual.Y, PainelDpadContextual.LARGURA,
                    PainelDpadContextual.ALTURA), "a roda em si nao muda");
            int yCodigo = PainelDpadContextual.Y + PainelDpadContextual.ALTURA + 2;
            assertFalse(java.util.Arrays.equals(regiao(sem, x, yCodigo, 60, 13), regiao(com, x, yCodigo, 60, 13)),
                    "com o F8 aparece o codigo abaixo da roda");
            painel.desligarModoDesenvolvedor();
            assertArrayEquals(regiao(sem, x, yCodigo, 60, 13), regiao(pintar(), x, yCodigo, 60, 13),
                    "desligou o F8: o codigo some");
        });
    }
}
