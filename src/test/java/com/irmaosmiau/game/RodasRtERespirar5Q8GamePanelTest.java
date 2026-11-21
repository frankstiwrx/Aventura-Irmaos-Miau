package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.FamiliaTecnica;
import com.irmaosmiau.controles.NaturezaAcao;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;
import com.irmaosmiau.regras.Graduacao;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Complemento da 5Q.8 no GamePanel: o RT abre a roda de finalizacoes por cima (montada, cem quilos, costas)
 * no Xbox do J1 e no teclado-controle do J2; soltar o RT nao executa nada; fora disso o RT continua sendo
 * defesa e base; respirar e o Y (I no teclado-controle), sem conflito com a funcao anterior do Y.
 */
class RodasRtERespirar5Q8GamePanelTest extends Base5OGamePanel {

    private ProvedorGamepadSimulado sim;

    private void ctl(String tecla, boolean p) {
        tecla("pretoControle" + tecla, p);
    }

    /** J1 no Xbox simulado (Contextual, o padrao), J2 no teclado-controle. */
    private void j1NoXbox(MapaGamepad mapa) {
        sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "ID-1", "Controle 1");
        ServicoGamepads.instalarGlobal(sim);
        PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD)));
        if (mapa != null) {
            PreferenciasGamepad.instalarGlobal(PreferenciasGamepad.emMemoria(mapa));
        }
        painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        painel.getLutador1().setGraduacao(Graduacao.PRETA);
        painel.getLutador2().setGraduacao(Graduacao.PRETA);
        ticks(2);
    }

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void em(Posicao p, Lado cima) {
        m().forcarPosicao(p, cima, cima);
        ticks(40);
        m().definirEstabilidade(95);
    }

    private void xbox(EstadoGamepad e) {
        sim.definir("g1", e);
        ticks(1);
    }

    private static EstadoGamepad rtComRs(float x, float y) {
        return EstadoGamepad.repouso().com(BotaoGamepad.GATILHO_DIR)
                .comEixo(EixoGamepad.DIR_X, x).comEixo(EixoGamepad.DIR_Y, y);
    }

    // 1-4: RT abre a roda nas tres posicoes, com quatro finalizacoes distintas (vermelhas)
    @Test
    void rtAbreARodaPorCimaNasTresPosicoes() throws Exception {
        naEdt(() -> {
            j1NoXbox(null);
            for (Posicao p : new Posicao[] {Posicao.MONTADA, Posicao.CONTROLE_LATERAL, Posicao.COSTAS}) {
                em(p, Lado.A);
                xbox(EstadoGamepad.repouso().com(BotaoGamepad.GATILHO_DIR));
                ticks(1);
                assertEquals(Familia.R2, painel.familiaHud(SlotJogador.JOGADOR_1), p + ": RT = roda de finalizacoes");
                PainelDpadContextual.Quadro q = painel.montarQuadroDpad(SlotJogador.JOGADOR_1);
                assertTrue(q.nomeCamada().startsWith("RT · FINALIZ."), q.nomeCamada());
                assertEquals(4, q.celulas().size(), p + ": " + q.celulas());
                Set<String> rotulos = new HashSet<>();
                q.celulas().values().forEach(c -> {
                    assertEquals(NaturezaAcao.FINALIZACAO, c.natureza(), "vermelho de finalizacao");
                    assertTrue(c.disponivelAgora(), c.rotulo());
                    rotulos.add(c.rotulo());
                });
                assertEquals(4, rotulos.size(), "quatro nomes diferentes");
                assertFalse(painel.getLutador1().isRespirando(), "o RT por cima nao respira");
                xbox(EstadoGamepad.repouso());
            }
        });
    }

    // 5 + 15: so a inclinacao do RS com o RT segurado executa; segurar e soltar o RT nao finaliza
    @Test
    void escolherExecutaESoltarRtNaoFinaliza() throws Exception {
        naEdt(() -> {
            j1NoXbox(null);
            em(Posicao.MONTADA, Lado.A);
            xbox(EstadoGamepad.repouso().com(BotaoGamepad.GATILHO_DIR));
            ticks(30);
            xbox(EstadoGamepad.repouso());
            ticks(10);
            assertTrue(m().getAtaque().isEmpty() && m().getTentativa().isEmpty(), "segurar e soltar o RT: nada");
            m().definirEstabilidade(95);
            xbox(EstadoGamepad.repouso().com(BotaoGamepad.GATILHO_DIR));
            xbox(rtComRs(1f, 0f));                       // RT + RS ->: chave de biceps
            ticks(1);
            assertEquals("biceps", m().getAtaque().orElseThrow().finalizacao().tecnicaBase());
            assertEquals(Posicao.MONTADA, m().getPosicao());
            xbox(EstadoGamepad.repouso());
        });
    }

    // 17: o J2 no teclado-controle usa o mesmo mapa (P = RT, setas = RS)
    @Test
    void j2NoTecladoControleAbreARodaRt() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            painel.getLutador2().setGraduacao(Graduacao.PRETA);
            em(Posicao.COSTAS, Lado.B);
            ctl("P", true);
            ticks(2);
            assertEquals(Familia.R2, painel.familiaHud(SlotJogador.JOGADOR_2));
            assertTrue(painel.montarQuadroDpad(SlotJogador.JOGADOR_2).nomeCamada().startsWith("P · FINALIZ."),
                    painel.montarQuadroDpad(SlotJogador.JOGADOR_2).nomeCamada());
            ctl("UP", true);
            ticks(1);
            ctl("UP", false);
            ctl("P", false);
            ticks(1);
            assertEquals("short_choke", m().getAtaque().orElseThrow().finalizacao().tecnicaBase());
            // J1 (teclas livres): 3 = RT e as teclas de pegada = direcoes
            novo(InicioLuta.EM_PE);
            painel.getLutador1().setGraduacao(Graduacao.PRETA);
            em(Posicao.CONTROLE_LATERAL, Lado.A);
            tecla("brancoModDefesa", true);
            ticks(1);
            toque("brancoPegada3");                     // v = Von Flue
            tecla("brancoModDefesa", false);
            assertEquals("von_flue", m().getAtaque().orElseThrow().finalizacao().tecnicaBase());
        });
    }

    // fora das posicoes dominantes por cima o RT nao abre a roda
    @Test
    void porBaixoENaGuardaORtNaoAbreARoda() throws Exception {
        naEdt(() -> {
            j1NoXbox(null);
            em(Posicao.MONTADA, Lado.B);                 // J1 por baixo
            xbox(EstadoGamepad.repouso().com(BotaoGamepad.GATILHO_DIR));
            ticks(1);
            assertNotEquals(Familia.R2, painel.familiaHud(SlotJogador.JOGADOR_1));
            xbox(rtComRs(0f, -1f));
            ticks(2);
            assertTrue(m().getAtaque().isEmpty(), "por baixo o RT nao finaliza");
            xbox(EstadoGamepad.repouso());
            m().forcarPosicao(Posicao.GUARDA, Lado.B, Lado.A);   // guarda fechada: J1 por baixo controla
            ticks(40);
            xbox(EstadoGamepad.repouso().com(BotaoGamepad.GATILHO_DIR));
            ticks(1);
            assertNotEquals(Familia.R2, painel.familiaHud(SlotJogador.JOGADOR_1), "na guarda o RT nao abre roda");
            xbox(EstadoGamepad.repouso());
        });
    }

    // 11: em pe o RT continua sendo defesa (guarda e sprawl) e base
    @Test
    void emPeORtContinuaDefensivo() throws Exception {
        naEdt(() -> {
            j1NoXbox(null);
            xbox(rtComRs(0f, -1f));                      // RT + RS ^ = guarda alta
            ticks(4);
            assertEquals(FamiliaTecnica.DEFESAS, painel.familiaDo(SlotJogador.JOGADOR_1));
            assertTrue(painel.getLutador1().getDefesa().isAtiva(), "a guarda continua no RT");
            xbox(EstadoGamepad.repouso());
            ticks(20);
            xbox(rtComRs(0f, 1f));                       // RT + RS v = sprawl
            ticks(2);
            assertTrue(painel.getLutador1().isFazendoSprawl(), "o sprawl continua no RT");
            xbox(EstadoGamepad.repouso());
        });
    }

    // 8 + 9: respirar no Y do Xbox e no I do teclado-controle
    @Test
    void yRespiraNoXboxEITecladoControle() throws Exception {
        naEdt(() -> {
            j1NoXbox(null);
            painel.getLutador1().gastarFolegoEsforco(10.0, "teste");
            double f = painel.getLutador1().getFolegoAtual();
            xbox(EstadoGamepad.repouso().com(BotaoGamepad.NORTE));
            ctl("I", true);
            ticks(30);
            assertTrue(painel.getLutador1().isRespirando(), "Y do Xbox respira");
            assertTrue(painel.getLutador2().isRespirando(), "I (Y) do teclado-controle respira");
            assertTrue(painel.getLutador1().getFolegoAtual() > f, "a recuperacao de sempre");
            ctl("I", false);
            xbox(EstadoGamepad.repouso());
            ticks(1);
            assertFalse(painel.getLutador1().isRespirando() || painel.getLutador2().isRespirando());
            // no solo tambem (mesma mecanica), por cima: o titulo mostra Y AR
            em(Posicao.MONTADA, Lado.A);
            xbox(EstadoGamepad.repouso().com(BotaoGamepad.NORTE));
            ticks(2);
            assertTrue(painel.getLutador1().isRespirando());
            assertTrue(painel.montarQuadroDpad(SlotJogador.JOGADOR_1).nomeCamada().contains("Y AR"),
                    painel.montarQuadroDpad(SlotJogador.JOGADOR_1).nomeCamada());
            xbox(EstadoGamepad.repouso());
        });
    }

    // 10: sem conflito com a funcao anterior do Y (Contextual: livre; Classico: reforco com pegada)
    @Test
    void ySemConflitoComAFuncaoAnterior() throws Exception {
        for (EsquemaGamepad e : EsquemaGamepad.values()) {
            MapaGamepad mapa = MapaGamepad.padrao().comEsquema(SlotJogador.JOGADOR_1, e);
            assertTrue(mapa.isValido(), e + ": " + mapa.problemas());
            assertEquals(BotaoGamepad.NORTE, mapa.botao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.RESPIRAR))
                    .orElseThrow(), e + ": respirar no Y");
            assertTrue(mapa.botao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.MODIFICADOR_DEFESA))
                    .filter(b -> b == BotaoGamepad.GATILHO_DIR).isPresent(), "o RT continua o modificador de defesas");
        }
        assertEquals(BotaoGamepad.NORTE, MapaGamepad.classico().botao(SlotJogador.JOGADOR_1,
                ItemGamepad.de(AcaoJogador.REFORCAR_PEGADA)).orElseThrow(), "o reforco do Classico continua no Y");
        naEdt(() -> {
            j1NoXbox(MapaGamepad.classico());
            painel.getLutador1().gastarFolegoEsforco(10.0, "teste");
            xbox(EstadoGamepad.repouso().com(BotaoGamepad.NORTE));
            ticks(5);
            assertTrue(painel.getLutador1().isRespirando(), "Classico, livre: o Y respira");
            xbox(EstadoGamepad.repouso());
            ticks(2);
            // com a pegada aplicada, o Y reforca e nao respira
            xbox(EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, 1f));
            for (int i = 0; i < 400 && !s().podeTentarAcao(j1(), j2(), "pegada_1"); i++) {
                ticks(1);
            }
            xbox(EstadoGamepad.repouso());
            xbox(EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_Y, -1f));    // RS ^ = pegada 1 no Classico
            xbox(EstadoGamepad.repouso());
            ticks(40);
            assertTrue(s().isInteracaoEstabelecida(), "pegada do J1");
            xbox(EstadoGamepad.repouso().com(BotaoGamepad.NORTE));
            ticks(3);
            assertFalse(painel.getLutador1().isRespirando(), "aplicando a pegada o Y e o reforco");
            xbox(EstadoGamepad.repouso());
        });
    }
}
