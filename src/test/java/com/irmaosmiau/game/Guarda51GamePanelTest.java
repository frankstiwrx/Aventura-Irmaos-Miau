package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.DefesaCorporal;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5O.1 (complemento): andar com a guarda a 50%, RT sozinho = respirar
 * e as regras de folego/prioridade. J1 pelas teclas (3 = RT; 2/6/7/8 = RS),
 * J2 pelo teclado-controle (P = RT; setas = RS; WASD = LS).
 */
class Guarda51GamePanelTest extends Base5OGamePanel {

    private static final int MIN = GamePanel.LIMITE_ESQUERDO_ARENA;

    /** Guarda do J1 pela tecla da direcao (com o RT/3 segurado). */
    private void guardaJ1(String teclaDirecao) {
        tecla("brancoModDefesa", true);
        tecla(teclaDirecao, true);
        ticks(DefesaCorporal.TICKS_ENTRADA + 2);
    }

    private void soltarGuardaJ1(String teclaDirecao) {
        tecla(teclaDirecao, false);
        tecla("brancoModDefesa", false);
    }

    /** Quanto o J1 andou para a esquerda em n ticks segurando as teclas. */
    private int andouEsquerda(int n, String... extras) {
        for (String e : extras) {
            tecla(e, true);
        }
        tecla("brancoEsquerda", true);
        int x = j1().getX();
        ticks(n);
        int d = x - j1().getX();
        tecla("brancoEsquerda", false);
        for (String e : extras) {
            tecla(e, false);
        }
        return d;
    }

    // 1-4: as tres guardas andam a 50%; sem guarda continua 100%
    @Test
    void asTresGuardasAndamAMetadeDaVelocidade() throws Exception {
        String[][] guardas = {{"brancoPegada1", "GUARDA_ALTA"}, {"brancoPegada4", "GUARDA_MEDIA"},
            {"brancoPegada2", "GUARDA_BAIXA"}};
        naEdt(() -> {
            for (String[] g : guardas) {
                novo(InicioLuta.EM_PE);
                int normal = andouEsquerda(20);
                ticks(3);
                guardaJ1(g[0]);
                assertEquals(DefesaCorporal.Tipo.valueOf(g[1]), j1().getDefesa().getTipo());
                int naGuarda = andouEsquerda(20);
                double r = naGuarda / (double) normal;
                assertEquals(DefesaCorporal.MULTIPLICADOR_VELOCIDADE, r, 0.08, g[1] + ": " + naGuarda + "/" + normal);
                // 9-10: andar nao cancela a guarda; parar tambem nao
                assertTrue(j1().getDefesa().isAtiva(), g[1] + " continua ativa depois de andar");
                ticks(20);
                assertEquals(DefesaCorporal.Fase.ATIVA, j1().getDefesa().getFase(), "parar nao cancela");
                soltarGuardaJ1(g[0]);
                ticks(DefesaCorporal.TICKS_SAIDA + 2);
                // 12: soltar o RT devolve a caminhada normal
                assertFalse(j1().getDefesa().isAtiva());
                assertEquals(normal, andouEsquerda(20), 2, "velocidade normal de volta");
            }
        });
    }

    // 5: sem corrida com velocidade integral na guarda
    @Test
    void naGuardaNaoCorre() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            int normal = andouEsquerda(30);
            guardaJ1("brancoPegada1");
            int correndoNaGuarda = andouEsquerda(30, "brancoCorrer");
            assertFalse(j1().isCorrendo());
            assertTrue(correndoNaGuarda <= normal * 0.6, correndoNaGuarda + " vs " + normal);
        });
    }

    // 6: diagonal normalizada (sem ganho de velocidade)
    @Test
    void diagonalDaGuardaNormalizada() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            guardaJ1("brancoPegada4");
            int reto = andouEsquerda(10);
            String vertical = j1().getLinhaAtual() > 1 ? "brancoCima" : "brancoBaixo";
            int x = j1().getX();
            int y = j1().getY();
            andouEsquerda(10, vertical);
            double dx = x - j1().getX();
            double dy = Math.abs(j1().getY() - y);
            double diag = Math.hypot(dx, dy);
            assertTrue(dy > 0, "mudou de linha andando na guarda");
            assertEquals(reto, diag, 2.5, "mesma distancia por tick (" + dx + ", " + dy + ")");
        });
    }

    // 7-8: cinco linhas e bordas da arena com a guarda
    @Test
    void guardaAndandoRespeitaLinhasEBordas() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            guardaJ1("brancoPegada1");
            tecla("brancoBaixo", true);
            ticks(200);
            tecla("brancoBaixo", false);
            assertEquals(4, j1().getLinhaAtual(), "linha da frente");
            tecla("brancoCima", true);
            ticks(300);
            tecla("brancoCima", false);
            assertEquals(0, j1().getLinhaAtual(), "linha do fundo");
            tecla("brancoEsquerda", true);
            ticks(900);
            tecla("brancoEsquerda", false);
            assertEquals(MIN, j1().getX(), "borda da arena");
            assertTrue(j1().getDefesa().isAtiva());
        });
    }

    // 11: trocar de guarda andando funciona, sem teletransporte
    @Test
    void trocaDeGuardaAndando() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            guardaJ1("brancoPegada1");
            tecla("brancoEsquerda", true);
            ticks(10);
            tecla("brancoPegada1", false);
            tecla("brancoPegada4", true);                        // alta -> media andando
            int xAntes = j1().getX();
            for (int i = 0; i < 12; i++) {
                int x = j1().getX();
                ticks(1);
                assertTrue(Math.abs(j1().getX() - x) <= 2, "sem salto");
            }
            assertEquals(DefesaCorporal.Tipo.GUARDA_MEDIA, j1().getDefesa().getTipo());
            assertTrue(j1().getX() < xAntes, "continuou andando");
            tecla("brancoPegada4", false);
            tecla("brancoPegada2", true);                        // media -> baixa andando
            ticks(DefesaCorporal.TICKS_ENTRADA + 2);
            assertEquals(DefesaCorporal.Tipo.GUARDA_BAIXA, j1().getDefesa().getTipo());
            assertEquals(DefesaCorporal.Fase.ATIVA, j1().getDefesa().getFase());
            tecla("brancoEsquerda", false);
        });
    }

    // 13: sem recuperacao gratuita de folego andando ou parado na guarda; nunca negativo
    @Test
    void guardaNaoRecuperaFolego() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            guardaJ1("brancoPegada1");
            double antes = j1().getFolegoAtual();
            tecla("brancoEsquerda", true);
            for (int i = 0; i < 120; i++) {
                double f = j1().getFolegoAtual();
                ticks(1);
                assertTrue(j1().getFolegoAtual() <= f + 1e-9, "andando na guarda nao recupera");
            }
            tecla("brancoEsquerda", false);
            for (int i = 0; i < 60; i++) {
                double f = j1().getFolegoAtual();
                ticks(1);
                assertTrue(j1().getFolegoAtual() <= f + 1e-9, "parado na guarda nao recupera");
                assertFalse(j1().isRespirando(), "RT com direcao: a defesa vence a respiracao");
            }
            assertTrue(j1().getFolegoAtual() < antes && j1().getFolegoAtual() >= 0);
        });
    }

    // 14: o sprawl continua sem caminhada livre
    @Test
    void sprawlContinuaSemAndar() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("brancoEsquerda", true);
            tecla("brancoModDefesa", true);
            toque("brancoPegada3");                              // RT + baixo = sprawl
            assertTrue(j1().isFazendoSprawl());
            int x = j1().getX();
            while (j1().isFazendoSprawl()) {
                ticks(1);
                assertEquals(x, j1().getX(), "nada de caminhada durante o sprawl");
            }
            tecla("brancoEsquerda", false);
            tecla("brancoModDefesa", false);
        });
    }

    // 15-16: Preto pelo teclado-controle (P + seta + WASD ao mesmo tempo)
    @Test
    void pretoNoTecladoControleAndaNaGuarda() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            IrmaoMiau p = j2();
            tecla("pretoControleD", true);
            int x = p.getX();
            ticks(30);
            int normal = p.getX() - x;
            tecla("pretoControleD", false);
            ticks(2);
            tecla("pretoControleP", true);
            tecla("pretoControleUP", true);
            ticks(DefesaCorporal.TICKS_ENTRADA + 2);
            assertEquals(DefesaCorporal.Tipo.GUARDA_ALTA, p.getDefesa().getTipo());
            tecla("pretoControleD", true);
            x = p.getX();
            ticks(30);
            assertEquals(0.5, (p.getX() - x) / (double) normal, 0.1);
            assertTrue(p.getDefesa().isAtiva(), "o LS nao mexe na guarda escolhida pelo RS");
            tecla("pretoControleD", false);
            tecla("pretoControleUP", false);
            tecla("pretoControleP", false);
        });
    }

    // RT sozinho = respirar (teclado do J1 e teclado-controle do J2)
    // Complemento 5Q.8 (pedido): o RT sozinho nao respira mais; respirar e o botao proprio (Y; J1 nas teclas:
    // a tecla de respirar; J2 no teclado-controle: I). A prioridade da defesa continua a mesma.
    @Test
    void rtSozinhoRespira() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            tecla("brancoModDefesa", true);
            tecla("pretoControleP", true);
            ticks(3);
            assertFalse(j1().isRespirando() || j2().isRespirando(), "RT/3 e P sozinhos nao respiram mais");
            tecla("brancoModDefesa", false);
            tecla("pretoControleP", false);
            tecla("brancoRespirar", true);
            tecla("pretoControleI", true);
            ticks(3);
            assertTrue(j1().isRespirando(), "J1: a tecla de respirar respira");
            assertTrue(j2().isRespirando(), "J2: I (Y) respira");
            assertFalse(j1().getDefesa().isAtiva());
            tecla("brancoModDefesa", true);
            tecla("brancoPegada1", true);                        // RT + direcao: defesa tem prioridade
            ticks(2);
            assertFalse(j1().isRespirando());
            assertTrue(j1().getDefesa().isSegurada());
            tecla("brancoPegada1", false);
            tecla("brancoModDefesa", false);
            ticks(DefesaCorporal.TICKS_SAIDA + 2);
            assertTrue(j1().isRespirando(), "voltou ao neutro: respira de novo");
            tecla("brancoRespirar", false);
            tecla("pretoControleI", false);
            ticks(1);
            assertFalse(j1().isRespirando() || j2().isRespirando());
        });
    }
}
