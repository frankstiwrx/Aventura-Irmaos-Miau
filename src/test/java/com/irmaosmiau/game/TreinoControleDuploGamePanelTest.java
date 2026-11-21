package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import org.junit.jupiter.api.Test;

/**
 * TREINO: o parceiro (J2) pode ser comandado por um segundo dispositivo (PAUSA -> LABORATORIO -> J2:
 * CONTROLADO) ou pelo dispositivo principal, alternando com o F11, sem mudar nada da luta.
 */
class TreinoControleDuploGamePanelTest extends Base5OGamePanel {

    private ProvedorGamepadSimulado sim;

    private void treino() {
        painel = new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        ticks(2);
    }

    /** Treino com gamepads simulados: J1 e J2 associados a GAMEPAD. */
    private void treinoComGamepads(String... chaves) {
        sim = new ProvedorGamepadSimulado();
        for (String c : chaves) {
            sim.conectar(c, "ID-" + c, "Controle " + c);
        }
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(AssociacaoDispositivos.padrao()
                .com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD).com(SlotJogador.JOGADOR_2, TipoDispositivo.GAMEPAD)));
        treino();
    }

    private void f11() {
        painel.getActionMap().get("alternarControle").actionPerformed(null);
        painel.getActionMap().get("soltouF11").actionPerformed(null);
        ticks(1);
    }

    private static EstadoGamepad direita() {
        return EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, 1f);
    }

    // 1 + 11: o J1 continua controlavel; o parceiro sem comando continua passivo
    @Test
    void j1ControlavelEParceiroPassivo() throws Exception {
        naEdt(() -> {
            treino();
            assertTrue(painel.parceiroPassivo(SlotJogador.JOGADOR_2));
            assertEquals("CONTROLANDO: J1 — MIAU BRANCO", painel.textoControlando());
            int x1 = j1().getX();
            int x2 = j2().getX();
            tecla("brancoEsquerda", true);
            tecla("pretoControleD", true);                   // teclas do J2: o parceiro passivo nao anda
            ticks(30);
            tecla("brancoEsquerda", false);
            tecla("pretoControleD", false);
            assertTrue(j1().getX() < x1, "o J1 anda");
            assertEquals(x2, j2().getX(), "o parceiro passivo nao recebe comandos");
        });
    }

    // 4 + 5 + 6 + 7 + 9 + 10: F11 leva o dispositivo principal para o J2 e volta, sem mudar a luta
    @Test
    void f11AlternaOPersonagemDoDispositivoPrincipal() throws Exception {
        naEdt(() -> {
            treino();
            int x1 = j1().getX();
            int x2 = j2().getX();
            double folego2 = j2().getFolegoAtual();
            var graduacao2 = j2().getGraduacao();
            var kimono2 = j2().getCorKimono();
            var posicao = painel.getMotorSolo().getPosicao();
            // segurar F11 (auto-repeat do sistema): alterna uma vez so
            painel.getActionMap().get("alternarControle").actionPerformed(null);
            painel.getActionMap().get("alternarControle").actionPerformed(null);
            painel.getActionMap().get("alternarControle").actionPerformed(null);
            assertTrue(painel.getRota().isPrincipalNoJ2(), "um toque, uma troca");
            painel.getActionMap().get("soltouF11").actionPerformed(null);
            ticks(1);
            assertEquals("CONTROLANDO: J2 — MIAU PRETO", painel.textoControlando());
            assertEquals(x1, j1().getX(), "a troca nao move ninguem");
            assertEquals(x2, j2().getX());
            assertEquals(folego2, j2().getFolegoAtual(), 1e-9);
            assertEquals(graduacao2, j2().getGraduacao(), "faixa e kimono sao os mesmos");
            assertEquals(kimono2, j2().getCorKimono());
            assertEquals(posicao, painel.getMotorSolo().getPosicao(), "a luta continua no mesmo estado");
            assertFalse(painel.parceiroPassivo(SlotJogador.JOGADOR_2));
            tecla("brancoDireita", true);                    // o MESMO teclado do J1...
            ticks(30);
            tecla("brancoDireita", false);
            assertTrue(j2().getX() > x2, "...agora move o Miau Preto");
            assertEquals(x1, j1().getX(), "e o Miau Branco nao recebe mais nada");
            f11();
            assertEquals("CONTROLANDO: J1 — MIAU BRANCO", painel.textoControlando());
            int x2Antes = j2().getX();
            int x1Antes = j1().getX();
            tecla("brancoDireita", true);
            ticks(30);
            tecla("brancoDireita", false);
            assertTrue(j1().getX() > x1Antes, "voltou para o J1");
            assertEquals(x2Antes, j2().getX(), "o J2 deixou de receber");
        });
    }

    // 8: a troca nao deixa direcao presa nem repete o que estava segurado
    @Test
    void trocaNaoDeixaComandoPreso() throws Exception {
        naEdt(() -> {
            treino();
            tecla("brancoDireita", true);
            ticks(10);
            f11();                                           // troca com a direcao segurada
            int x1 = j1().getX();
            int x2 = j2().getX();
            ticks(30);
            assertEquals(x1, j1().getX(), "o J1 parou");
            assertEquals(x2, j2().getX(), "o J2 nao herda a direcao segurada (so depois de soltar)");
            tecla("brancoDireita", false);
            tecla("brancoDireita", true);
            ticks(20);
            tecla("brancoDireita", false);
            assertTrue(j2().getX() > x2, "soltou e apertou de novo: o J2 anda");
        });
    }

    // 6: os comandos de combate habituais (pegada) saem pelo personagem escolhido
    @Test
    void pegadaPeloPersonagemAlternado() throws Exception {
        naEdt(() -> {
            treino();
            f11();
            tecla("brancoEsquerda", true);
            for (int i = 0; i < 400 && !s().podeTentarAcao(j2(), j1(), "pegada_1"); i++) {
                ticks(1);
            }
            tecla("brancoEsquerda", false);
            ticks(2);
            toque("brancoPegada1");
            ticks(40);
            assertTrue(s().estaEmInteracao(), "a pegada saiu");
            assertEquals(j2(), s().getAplicadorInteracao(), "pelo Miau Preto");
        });
    }

    // 2 + 3: dois dispositivos (gamepad do J1 e teclado-controle do J2) comandam os dois ao mesmo tempo
    @Test
    void doisDispositivosIndependentes() throws Exception {
        naEdt(() -> {
            treinoComGamepads("g1");
            painel.alternarParceiroControlado();             // PAUSA -> LABORATORIO -> J2: CONTROLADO
            assertFalse(painel.parceiroPassivo(SlotJogador.JOGADOR_2));
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f));
            tecla("pretoControleD", true);                   // o J2 no teclado-controle
            int x1 = j1().getX();
            int x2 = j2().getX();
            ticks(30);
            assertTrue(j1().getX() < x1, "o gamepad move o J1");
            assertTrue(j2().getX() > x2, "o teclado-controle move o J2, ao mesmo tempo");
            tecla("pretoControleD", false);
            sim.definir("g1", EstadoGamepad.repouso());
        });
    }

    // 3: dois gamepads, um para cada; com o F11, o principal assume o J2 e o gamepad do J2 fica em espera
    @Test
    void doisGamepadsEOF11() throws Exception {
        naEdt(() -> {
            treinoComGamepads("g1", "g2");
            painel.alternarParceiroControlado();
            sim.definir("g2", direita());
            int x2 = j2().getX();
            ticks(30);
            assertTrue(j2().getX() > x2, "o segundo gamepad move o J2");
            sim.definir("g2", EstadoGamepad.repouso());
            ticks(2);
            f11();
            int x1 = j1().getX();
            x2 = j2().getX();
            sim.definir("g2", direita());                    // o gamepad do J2 em espera
            ticks(30);
            assertEquals(x2, j2().getX(), "duas fontes nunca disputam o mesmo personagem");
            assertEquals(x1, j1().getX());
            sim.definir("g2", EstadoGamepad.repouso());
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(2);
            sim.definir("g1", direita());
            ticks(30);
            assertTrue(j2().getX() > x2, "o principal (g1) comanda o J2");
            assertEquals(x1, j1().getX());
            sim.definir("g1", EstadoGamepad.repouso());
        });
    }

    // 12: no JOGAR o F11 nao faz nada e cada jogador segue com o proprio dispositivo
    @Test
    void jogarNaoAlterna() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            f11();
            assertFalse(painel.getRota().isPrincipalNoJ2());
            int x1 = j1().getX();
            int x2 = j2().getX();
            tecla("brancoEsquerda", true);
            tecla("pretoControleD", true);
            ticks(30);
            tecla("brancoEsquerda", false);
            tecla("pretoControleD", false);
            assertTrue(j1().getX() < x1);
            assertTrue(j2().getX() > x2);
        });
    }
}
