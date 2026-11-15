package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.TipoDisputa;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5O.2: Contextual como padrao e o J2 com os mesmos recursos do J1:
 * romper com RS para baixo (seta para baixo no teclado-controle) e arrastar com
 * o LS (WASD) sendo o aplicador. Nos dois sentidos (J1 x J2) e com os
 * personagens trocados de slot.
 */
class Correcoes52GamePanelTest extends Base5OGamePanel {

    private ProvedorGamepadSimulado sim;

    private static EscolhaLutador oficial(String id) {
        return EscolhaLutador.oficial(CatalogoPersonagens.padrao().exigir(id));
    }

    /** invertidos = J1 com o Preto e J2 com o Branco. */
    private void partida(boolean invertidos) {
        painel = invertidos
                ? new GamePanel(TipoPersonagem.AMBOS, oficial(CatalogoPersonagens.ID_PRETO),
                        oficial(CatalogoPersonagens.ID_BRANCO), InicioLuta.EM_PE, () -> { }, () -> { }, () -> { })
                : new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        ticks(2);
        if (invertidos) {
            assertTrue(j1() instanceof IrmaoMiauPreto && j2() instanceof IrmaoMiauBranco);
        }
    }

    private void ctl(String tecla, boolean p) {
        tecla("pretoControle" + tecla, p);
    }

    private void toqueCtl(String tecla) {
        ctl(tecla, true);
        ticks(1);
        ctl(tecla, false);
        ticks(1);
    }

    /** J2 (teclado-controle) anda ate o J1 e pega com a seta para cima (RS para cima). */
    private void j2PegaPeloTeclado() {
        ctl("A", true);
        for (int i = 0; i < 400 && !s().podeTentarAcao(j2(), j1(), "pegada_1"); i++) {
            ticks(1);
        }
        ctl("A", false);
        ticks(2);
        toqueCtl("UP");
        ticks(40);
        assertTrue(s().isInteracaoEstabelecida(), "pegada do J2 estabelecida");
        assertEquals(j2(), s().getAplicadorInteracao());
    }

    // ---------------- 1-3: esquema padrao
    @Test
    void contextualEOPadraoDosDoisJogadoresInclusiveNoTecladoControle() throws Exception {
        assertEquals(EsquemaGamepad.CONTEXTUAL, MapaGamepad.padrao().esquema(SlotJogador.JOGADOR_1));
        assertEquals(EsquemaGamepad.CONTEXTUAL, MapaGamepad.padrao().esquema(SlotJogador.JOGADOR_2));
        naEdt(() -> {
            partida(false);
            assertEquals(EsquemaGamepad.CONTEXTUAL, PreferenciasGamepad.global().getAtual().esquema(SlotJogador.JOGADOR_2));
            String f3 = String.join(" ", painel.linhasTecnicas());
            assertTrue(f3.contains("J2 CONTEXTUAL"), f3);
        });
    }

    // ---------------- 7-12: rompimento
    @Test
    void j2AgarradoRompeComASetaParaBaixoNosDoisArranjos() throws Exception {
        naEdt(() -> {
            for (boolean inv : new boolean[] {false, true}) {
                partida(inv);
                j1Pega(1);
                assertEquals(j2(), s().getReceptorInteracao());
                toqueCtl("DOWN");                                 // seta para baixo = RS para baixo
                assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, s().getUltimaDisputa().getTipo(),
                        "invertidos=" + inv);
                assertFalse(s().estaEmInteracao() && s().getAplicadorInteracao() == j2(),
                        "o rompimento nao vira uma pegada nova do J2");
            }
        });
    }

    @Test
    void j1AgarradoRompeComORsParaBaixoNoXbox() throws Exception {
        naEdt(() -> {
            sim = new ProvedorGamepadSimulado();
            sim.conectar("g1", "ID-1", "Controle 1");
            ServicoGamepads.instalarGlobal(sim);
            PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                    AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD)));
            partida(false);
            j2PegaPeloTeclado();
            assertEquals(j1(), s().getReceptorInteracao());
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_Y, 1f));   // RS para baixo
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, s().getUltimaDisputa().getTipo());
        });
    }

    @Test
    void setaParaBaixoLivreEPegadaEAgarradoERompimento() throws Exception {
        naEdt(() -> {
            partida(false);
            ctl("A", true);
            for (int i = 0; i < 400 && !s().podeTentarAcao(j2(), j1(), "pegada_3"); i++) {
                ticks(1);
            }
            ctl("A", false);
            ticks(2);
            toqueCtl("DOWN");
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida(), "livre: seta para baixo = pegada 3 (duas mangas)");
            assertEquals(j2(), s().getAplicadorInteracao());
        });
    }

    // ---------------- 13-20: arrasto
    @Test
    void j2AplicadorArrastaOJ1NasOitoDirecoesNosDoisArranjos() throws Exception {
        // comecam na linha 1 (fundo): W so depois de descer (subir na linha 1 e o limite das cinco linhas)
        String[][] dirs = {{"A"}, {"D"}, {"S"}, {"S", "D"}, {"S", "A"}, {"W"}, {"W", "A"}};
        naEdt(() -> {
            for (boolean inv : new boolean[] {false, true}) {
                for (String[] d : dirs) {
                    partida(inv);
                    j2PegaPeloTeclado();
                    if (d[0].equals("W")) {
                        ctl("S", true);
                        ticks(40);
                        ctl("S", false);
                        assertTrue(j1().getLinhaAtual() > 0, "desceu arrastando");
                    }
                    IrmaoMiau a = j2();
                    IrmaoMiau r = j1();
                    int distX = r.getX() - a.getX();
                    int distY = r.getY() - a.getY();
                    int xr = r.getX();
                    int yr = r.getY();
                    double f = a.getFolegoAtual();
                    for (String k : d) {
                        ctl(k, true);
                    }
                    ticks(20);
                    for (String k : d) {
                        ctl(k, false);
                    }
                    String caso = String.join("+", d) + " invertidos=" + inv;
                    assertTrue(s().isInteracaoEstabelecida(), "a pegada continua: " + caso);
                    assertTrue(r.getX() != xr || r.getY() != yr, "o J1 foi levado: " + caso);
                    assertEquals(distX, r.getX() - a.getX(), 1, "dupla junta (x): " + caso);
                    assertEquals(distY, r.getY() - a.getY(), 1, "dupla junta (y): " + caso);
                    assertTrue(a.getFolegoAtual() < f, "arrastar custa folego: " + caso + " " + f + " -> " + a.getFolegoAtual() + " linha " + a.getLinhaAtual());
                }
            }
        });
    }

    @Test
    void j2ArrastaAteABordaSemSairDaArena() throws Exception {
        naEdt(() -> {
            partida(false);
            j2PegaPeloTeclado();
            ctl("D", true);
            ticks(600);
            ctl("D", false);
            assertTrue(Math.max(j1().getX(), j2().getX()) <= GamePanel.LIMITE_DIREITO_ARENA);
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    @Test
    void j1AplicadorTambemArrastaOJ2() throws Exception {
        naEdt(() -> {
            partida(false);
            j1Pega(1);
            int xr = j2().getX();
            tecla("brancoControle", true);                       // J1 nas teclas livres: CTRL + direcao
            tecla("brancoEsquerda", true);
            ticks(20);
            tecla("brancoEsquerda", false);
            tecla("brancoControle", false);
            assertTrue(j2().getX() < xr, "J1 arrasta o J2");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    @Test
    void receptorJ2AndandoNaoArrastaNinguemSoResiste() throws Exception {
        naEdt(() -> {
            partida(false);
            j1Pega(1);
            int x1 = j1().getX();
            ctl("D", true);
            ticks(20);
            ctl("D", false);
            assertEquals(x1, j1().getX(), "o receptor nao leva o aplicador");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }

    // ---------------- botoes faciais do J2 no Contextual
    @Test
    void botoesFaciaisDoJ2SeguemOContextual() throws Exception {
        naEdt(() -> {
            partida(false);
            ctl("L", true);                                      // B = agachar suave
            ticks(8);
            assertTrue(j2().isAgachado());
            ctl("L", false);
            ticks(2);
            ctl("A", true);
            for (int i = 0; i < 400 && !s().podeTentarAcao(j2(), j1(), "pegada_1"); i++) {
                ticks(1);
            }
            ctl("A", false);
            toqueCtl("J");                                       // X = correr no Contextual: nunca pega
            ticks(40);
            assertFalse(s().estaEmInteracao(), "X nao faz pegada no Contextual");
            int x = j2().getX();
            toqueCtl("H");                                       // D-pad: tecnica contextual, nao anda
            assertEquals(x, j2().getX(), 1);
        });
    }

    @Test
    void j2NoXboxRompeEArrastaComoNoTeclado() throws Exception {
        naEdt(() -> {
            sim = new ProvedorGamepadSimulado();
            sim.conectar("g1", "ID-1", "Controle 1");
            ServicoGamepads.instalarGlobal(sim);
            PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                    AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_2, TipoDispositivo.GAMEPAD)));
            partida(false);
            j1Pega(1);
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_Y, 1f));   // RS para baixo
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(1);
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, s().getUltimaDisputa().getTipo(), "J2 no Xbox rompe");

            partida(false);
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f));
            for (int i = 0; i < 400 && !s().podeTentarAcao(j2(), j1(), "pegada_1"); i++) {
                ticks(1);
            }
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(2);
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_Y, -1f));  // RS para cima = pegada 1
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(40);
            assertEquals(j2(), s().getAplicadorInteracao());
            int x1 = j1().getX();
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, 1f));   // LS: arrasto automatico
            ticks(20);
            sim.definir("g1", EstadoGamepad.repouso());
            assertTrue(j1().getX() > x1, "J2 no Xbox arrasta o J1");
            assertTrue(s().isInteracaoEstabelecida());
        });
    }
}
