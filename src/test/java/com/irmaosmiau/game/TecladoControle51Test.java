package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.TipoDisputa;
import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.FamiliaTecnica;
import com.irmaosmiau.controles.LayoutTecladoControle;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.DefesaCorporal;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5O.1: o J2 no teclado como CONTROLE (WASD = LS, setas = RS,
 * K/L/J/I = A/B/X/Y, Z/M = L3/R3, T/H/G/F = D-pad, O/P/E/Q = RB/RT/LB/LT,
 * ENTER = Start, SHIFT = Select = RB logico) e o Select do Xbox como RB.
 */
class TecladoControle51Test extends Base5OGamePanel {

    private ProvedorGamepadSimulado sim;

    private void ctl(String tecla, boolean p) {
        tecla("pretoControle" + tecla, p);
    }

    private void toqueCtl(String tecla) {
        ctl(tecla, true);
        ticks(1);
        ctl(tecla, false);
        ticks(1);
    }

    /** J1 no Xbox simulado, J2 no teclado-controle. */
    private void j1NoXbox() {
        sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID-1", "Controle 1");
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD)));
        painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        ticks(2);
    }

    // ---------------- layout exato e auditoria de colisoes
    @Test
    void layoutDoJ2ExatamenteComoPedido() {
        Map<String, String> esperado = new LinkedHashMap<>();
        String[][] t = {{"W", "LS_CIMA"}, {"S", "LS_BAIXO"}, {"A", "LS_ESQUERDA"}, {"D", "LS_DIREITA"},
            {"UP", "RS_CIMA"}, {"DOWN", "RS_BAIXO"}, {"LEFT", "RS_ESQUERDA"}, {"RIGHT", "RS_DIREITA"},
            {"K", "SUL"}, {"L", "LESTE"}, {"J", "OESTE"}, {"I", "NORTE"}, {"Z", "L3"}, {"M", "R3"},
            {"T", "DPAD_CIMA"}, {"H", "DPAD_DIREITA"}, {"G", "DPAD_BAIXO"}, {"F", "DPAD_ESQUERDA"},
            {"O", "RB"}, {"P", "GATILHO_DIR"}, {"E", "LB"}, {"Q", "GATILHO_ESQ"}, {"ENTER", "INICIAR"},
            {"SHIFT", "VOLTAR"}};
        for (String[] l : t) {
            esperado.put(l[0], l[1]);
        }
        assertEquals(esperado, LayoutTecladoControle.layout(SlotJogador.JOGADOR_2));
        assertTrue(LayoutTecladoControle.usa(SlotJogador.JOGADOR_2));
        assertFalse(LayoutTecladoControle.usa(SlotJogador.JOGADOR_1));
    }

    @Test
    void nenhumaTeclaDoJ2DisparaOJ1() throws Exception {
        assertTrue(MapaControles.padrao().isValido(), MapaControles.padrao().problemas().toString());
        for (Map.Entry<AcaoJogador, TeclaFisica> e : MapaControles.padrao().doSlot(SlotJogador.JOGADOR_1).entrySet()) {
            assertFalse(LayoutTecladoControle.layout(SlotJogador.JOGADOR_2).containsKey(e.getValue().getNome()),
                    "J1 " + e.getKey() + " usa " + e.getValue().getNome());
        }
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            InputMap m = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            for (String k : LayoutTecladoControle.layout(SlotJogador.JOGADOR_2).keySet()) {
                for (String mod : new String[] {"", "shift ", "ctrl "}) {
                    Object cmd = m.get(KeyStroke.getKeyStroke(mod + "pressed " + k));
                    assertEquals("pretoControle" + k + "Pressionada", cmd, mod + k);
                }
            }
            for (TeclaFisica t : MapaControles.padrao().doSlot(SlotJogador.JOGADOR_1).values()) {
                Object cmd = m.get(KeyStroke.getKeyStroke("pressed " + t.getNome()));
                assertNotNull(cmd, t.getNome());
                assertTrue(cmd.toString().startsWith("branco"), t.getNome() + " -> " + cmd);
            }
        });
    }

    // ---------------- cada tecla faz o papel do botao
    @Test
    void wasdMoveESetasEscolhemAPegadaComoORs() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            int x = j2().getX();
            ctl("D", true);
            ticks(10);
            ctl("D", false);
            assertTrue(j2().getX() > x, "D = LS para a direita");
            // anda ate o J1 e escolhe a pegada pela seta (RS no Classico)
            ctl("A", true);
            for (int i = 0; i < 400 && !s().podeTentarAcao(j2(), j1(), "pegada_1"); i++) {
                ticks(1);
            }
            ctl("A", false);
            ticks(2);
            toqueCtl("UP");
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida(), "seta para cima = RS para cima = pegada 1");
            assertEquals(j2(), s().getAplicadorInteracao());

            novo(InicioLuta.EM_PE);
            int linha = j2().getLinhaAtual();
            ctl(linha > 0 ? "W" : "S", true);
            ticks(40);
            ctl(linha > 0 ? "W" : "S", false);
            assertTrue(j2().getLinhaAtual() != linha, "W/S trocam de linha");
        });
    }

    @Test
    void kPulaEOuShiftMaisKPulamAlto() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            int curto = alturaDoPulo(() -> toqueCtl("K"));
            int altoRb = alturaDoPulo(() -> {
                ctl("O", true);
                toqueCtl("K");
                ctl("O", false);
            });
            int altoSelect = alturaDoPulo(() -> {
                ctl("SHIFT", true);
                toqueCtl("K");
                ctl("SHIFT", false);
            });
            assertTrue(curto > 0, "K = A = pulo");
            assertTrue(altoRb > curto, "O (RB) + K = pulo alto: " + altoRb + " > " + curto);
            assertEquals(altoRb, altoSelect, 1, "SHIFT (Select) + K = o mesmo pulo alto");
        });
    }

    private int alturaDoPulo(Runnable acao) {
        ticks(60);
        int y0 = j2().getY();
        acao.run();
        int min = y0;
        for (int i = 0; i < 90; i++) {
            ticks(1);
            min = Math.min(min, j2().getY());
        }
        return y0 - min;
    }

    @Test
    void lRompeIReforcaZMudaPosturaQAgacha() throws Exception {
        // 5O.2 (pedido): o padrao virou o Contextual. Nele o receptor rompe com o RS para baixo (seta para
        // baixo), o B (L) e o agachamento suave e o LT (Q) e o modificador da camada 2 (antes: Classico).
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            toqueCtl("DOWN");
            assertEquals(TipoDisputa.ROMPIMENTO_TECNICO, s().getUltimaDisputa().getTipo());

            novo(InicioLuta.EM_PE);
            ctl("L", true);                                      // B = agachar suave (Contextual)
            ticks(10);
            assertTrue(j2().isAgachado());
            ctl("L", false);
            ticks(2);
            toqueCtl("Z");                                       // L3 = postura
            ticks(40);
            assertTrue(j2().isAjoelhado(), "Z = L3 = ajoelhar");
        });
    }

    @Test
    void dpadTHGFIndependenteDasSetas() throws Exception {
        // 5O.2 (pedido): no Contextual o D-pad e das tecnicas (camadas), nao anda (antes: Classico).
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            int x = j2().getX();
            ctl("H", true);
            ticks(10);
            ctl("H", false);
            assertEquals(x, j2().getX(), "D-pad nao e o LS");
            assertFalse(s().estaEmInteracao(), "longe: a tecnica do D-pad nao alcanca");
            assertEquals(FamiliaTecnica.PEGADAS, painel.familiaDo(SlotJogador.JOGADOR_2));
        });
    }

    @Test
    void pERtShiftEORbEscolhemAsFamilias() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            ctl("P", true);
            ctl("UP", true);
            ticks(DefesaCorporal.TICKS_ENTRADA + 2);
            assertEquals(FamiliaTecnica.DEFESAS, painel.familiaDo(SlotJogador.JOGADOR_2));
            assertEquals(DefesaCorporal.Tipo.GUARDA_ALTA, j2().getDefesa().getTipo());
            assertFalse(s().estaEmInteracao(), "com o RT a seta nunca pega");
            ctl("UP", false);
            ctl("P", false);
            ticks(DefesaCorporal.TICKS_SAIDA + 2);
            for (String rb : new String[] {"O", "SHIFT"}) {
                ctl(rb, true);
                ticks(1);
                assertEquals(FamiliaTecnica.PROJECOES, painel.familiaDo(SlotJogador.JOGADOR_2), rb);
                assertTrue(painel.montarQuadroDpad(SlotJogador.JOGADOR_2).nomeCamada().startsWith("QUEDAS"), rb);
                ctl(rb, false);
                ticks(1);
            }
            ctl("P", true);
            toqueCtl("DOWN");                                    // RT + baixo = sprawl
            assertTrue(j2().isFazendoSprawl());
            ctl("P", false);
        });
    }

    @Test
    void enterSoAbreAPausa() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            toqueCtl("ENTER");
            assertTrue(painel.isPausado(), "ENTER = Start = pausa");
            toqueCtl("ENTER");
            assertTrue(painel.isPausado(), "com a pausa aberta o ENTER e do menu (nao alterna de novo)");
        });
    }

    @Test
    void j1EJ2NoTecladoAoMesmoTempo() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            int x1 = j1().getX();
            int x2 = j2().getX();
            tecla("brancoEsquerda", true);
            ctl("D", true);
            tecla("brancoModDefesa", true);
            tecla("brancoPegada1", true);
            ticks(20);
            assertTrue(j1().getX() < x1 && j2().getX() > x2, "os dois andam");
            assertTrue(j1().getDefesa().isAtiva(), "J1 defende enquanto o J2 anda");
            assertFalse(j2().getDefesa().isAtiva());
        });
    }

    // ---------------- J1 no Xbox + J2 no teclado; Select = RB
    @Test
    void j1NoXboxEJ2NoTecladoComRbESelect() throws Exception {
        naEdt(() -> {
            j1NoXbox();
            int x1 = j1().getX();
            int x2 = j2().getX();
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f).com(BotaoGamepad.RB));
            ctl("D", true);
            ctl("SHIFT", true);
            ticks(5);
            assertTrue(j1().getX() < x1 && j2().getX() > x2);
            assertEquals(FamiliaTecnica.PROJECOES, painel.familiaDo(SlotJogador.JOGADOR_1), "RB do Xbox");
            assertEquals(FamiliaTecnica.PROJECOES, painel.familiaDo(SlotJogador.JOGADOR_2), "SHIFT do teclado");
            ctl("D", false);
            ctl("SHIFT", false);
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.VOLTAR));     // Select do Xbox = RB
            ticks(1);
            assertEquals(FamiliaTecnica.PROJECOES, painel.familiaDo(SlotJogador.JOGADOR_1), "Select = RB logico");
            String f3 = String.join(" ", painel.linhasTecnicas());
            assertTrue(f3.contains("J1 fis:nao sel:SIM log:SIM"), f3);
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.GATILHO_DIR));  // RT sozinho
            ctl("P", true);
            ticks(3);
            assertTrue(j1().isRespirando() && j2().isRespirando(), "RT do Xbox e P do teclado respiram");
            ctl("P", false);
            sim.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.INICIAR));      // Start continua pausando
            ticks(1);
            assertTrue(painel.isPausado());
        });
    }

    @Test
    void j1RolandoEnquantoJ2RespiraEPegadaContraDefesa() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.DE_JOELHOS);
            ctl("P", true);
            tecla("brancoEsquerda", true);
            toque("brancoRolar");
            tecla("brancoEsquerda", false);
            assertTrue(j1().isRolando(), "J1 rola de joelhos");
            ticks(2);
            assertTrue(j2().isRespirando(), "J2 respira ao mesmo tempo");
            ctl("P", false);
            ticks(40);

            novo(InicioLuta.EM_PE);
            ctl("P", true);
            ctl("LEFT", true);                                   // J2 guarda media
            j1Pega(1);
            assertTrue(s().isInteracaoEstabelecida(), "a pegada do J1 entra (a guarda ainda nao reduz nada)");
            assertFalse(j2().getDefesa().isAtiva(), "agarrado, a guarda some");
            ctl("LEFT", false);
            ctl("P", false);
        });
    }

    @Test
    void modoLivrePelaPropriedadeDevolveAsTeclasAntigasDoJ2() throws Exception {
        LayoutTecladoControle.definirParaTestes(java.util.Set.of());
        try {
            naEdt(() -> {
                novo(InicioLuta.EM_PE);
                InputMap m = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                assertEquals("pretoEsquerdaPressionada", m.get(KeyStroke.getKeyStroke("pressed LEFT")));
                assertNull(m.get(KeyStroke.getKeyStroke("pressed W")), "W fica livre");
            });
        } finally {
            LayoutTecladoControle.definirParaTestes(null);
        }
    }
}
