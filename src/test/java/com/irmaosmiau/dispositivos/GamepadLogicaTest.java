package com.irmaosmiau.dispositivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.SlotJogador;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Entrega 5D: mapeamento de botoes, analogico e fonte de acoes do gamepad (sem hardware). */
class GamepadLogicaTest {

    private static final MapaGamepad MAPA = MapaGamepad.experimental();
    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;

    private static Set<AcaoJogador> acoes(EstadoGamepad e, SlotJogador s) {
        return MAPA.acoesAtivas(e, s, EnumSet.noneOf(AcaoJogador.class), EnumSet.noneOf(AcaoJogador.class));
    }

    // =====================================================
    // MAPEAMENTO
    // =====================================================
    @Test
    void botoesViramAcoesLogicasDoSlot() {
        EstadoGamepad r = EstadoGamepad.repouso();
        assertEquals(Set.of(AcaoJogador.PULAR), acoes(r.com(BotaoGamepad.SUL), J1));
        assertEquals(Set.of(AcaoJogador.INICIAR_PEGADA), acoes(r.com(BotaoGamepad.OESTE), J1));
        assertEquals(Set.of(AcaoJogador.REFORCAR_PEGADA), acoes(r.com(BotaoGamepad.NORTE), J1));
        assertEquals(Set.of(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO), acoes(r.com(BotaoGamepad.LB), J1));
        assertEquals(Set.of(AcaoJogador.AGACHAR), acoes(r.com(BotaoGamepad.GATILHO_ESQ), J1));
        assertEquals(Set.of(AcaoJogador.MOVER_CIMA), acoes(r.com(BotaoGamepad.DPAD_CIMA), J2));
        // Mesmo botao, acao do papel de cada slot: J1 desequilibra, J2 rompe.
        assertEquals(Set.of(AcaoJogador.TENTAR_DESEQUILIBRIO), acoes(r.com(BotaoGamepad.LESTE), J1));
        assertEquals(Set.of(AcaoJogador.ROMPIMENTO_TECNICO), acoes(r.com(BotaoGamepad.LESTE), J2));
        // J2 nao usa pegada: o botao nao produz nada para ele.
        assertEquals(Set.of(), acoes(r.com(BotaoGamepad.OESTE), J2));
        // Pausa nao e acao de jogador.
        assertEquals(Set.of(), acoes(r.com(BotaoGamepad.INICIAR), J1));
        assertEquals(BotaoGamepad.INICIAR, MAPA.getBotaoPausa());
        assertEquals("—", MAPA.rotulo(J1, AcaoJogador.PULO_CURTO), "sem botao no esquema provisorio");
        assertEquals("B/Bola", MAPA.rotulo(J2, AcaoJogador.ROMPIMENTO_TECNICO));
        assertEquals("D-pad ← / analógico", MAPA.rotulo(J1, AcaoJogador.MOVER_ESQUERDA));
    }

    @Test
    void gatilhoSoContaAcimaDoLimiar() {
        EstadoGamepad meio = EstadoGamepad.repouso().comEixo(EixoGamepad.GATILHO_ESQ, 0.3f);
        assertFalse(meio.pressionado(BotaoGamepad.GATILHO_ESQ));
        assertTrue(meio.comEixo(EixoGamepad.GATILHO_ESQ, 0.6f).pressionado(BotaoGamepad.GATILHO_ESQ));
    }

    @Test
    void dpadEAnalogicoJuntosNaoDuplicamAcao() {
        EstadoGamepad e = EstadoGamepad.repouso().com(BotaoGamepad.DPAD_DIREITA).comEixo(EixoGamepad.ESQ_X, 1f);
        assertEquals(Set.of(AcaoJogador.MOVER_DIREITA), acoes(e, J1), "uma acao, nao duas");
    }

    // =====================================================
    // ANALOGICO
    // =====================================================
    @Test
    void zonaMortaENormalizacao() {
        double[] centro = LeitorAnalogico.normalizar(0.2, -0.1);
        assertEquals(0.0, centro[0]);
        assertEquals(0.0, centro[1]);
        double[] cheio = LeitorAnalogico.normalizar(1.0, 0.0);
        assertEquals(1.0, cheio[0], 1e-9);
        double[] diagonal = LeitorAnalogico.normalizar(0.9, 0.9);
        assertTrue(Math.hypot(diagonal[0], diagonal[1]) <= 1.0 + 1e-9, "modulo limitado a 1");
        double[] nan = LeitorAnalogico.normalizar(Double.NaN, 0.5);
        assertEquals(0.0, nan[0]);

        Set<AcaoJogador> nada = EnumSet.noneOf(AcaoJogador.class);
        assertEquals(Set.of(), LeitorAnalogico.direcoes(0.2, 0.2, nada), "ruido em repouso nao move");
        assertEquals(Set.of(AcaoJogador.MOVER_ESQUERDA), LeitorAnalogico.direcoes(-0.9, 0.05, nada));
        assertEquals(Set.of(AcaoJogador.MOVER_CIMA), LeitorAnalogico.direcoes(0.0, -0.9, nada), "Y negativo = cima");
        assertEquals(Set.of(AcaoJogador.MOVER_BAIXO), LeitorAnalogico.direcoes(0.0, 0.9, nada));
    }

    @Test
    void histereseEvitaLigaDesligaNoLimiar() {
        Set<AcaoJogador> nada = EnumSet.noneOf(AcaoJogador.class);
        Set<AcaoJogador> direita = EnumSet.of(AcaoJogador.MOVER_DIREITA);
        // 0.6 cru -> ~0.47 normalizado: abaixo de ATIVAR, acima de SOLTAR.
        assertEquals(Set.of(), LeitorAnalogico.direcoes(0.6, 0, nada), "nao liga");
        assertEquals(direita, LeitorAnalogico.direcoes(0.6, 0, direita), "mas tambem nao desliga");
        assertEquals(Set.of(), LeitorAnalogico.direcoes(0.4, 0, direita), "desliga bem abaixo");
    }

    // =====================================================
    // FONTE DE ACOES (estado -> acoes, trava, desconexao)
    // =====================================================
    @Test
    void fonteContinuaEnquantoSeguraESoltaNaDesconexao() {
        FonteGamepad f = new FonteGamepad(J1, MAPA);
        EstadoGamepad direita = EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, 1f);
        for (int i = 0; i < 30; i++) {
            f.atualizar(direita);
            assertTrue(f.isAtiva(AcaoJogador.MOVER_DIREITA), "continua a cada tick");
        }
        f.atualizar(null);                                  // desconectou
        assertFalse(f.isAtiva(AcaoJogador.MOVER_DIREITA), "nada fica pressionado");
        assertFalse(f.temAlgoSegurado());
    }

    @Test
    void reconexaoComBotaoSeguradoNaoDisparaAteSoltar() {
        FonteGamepad f = new FonteGamepad(J1, MAPA);
        f.atualizar(null);
        f.travarNaProximaLeitura();
        EstadoGamepad pegada = EstadoGamepad.repouso().com(BotaoGamepad.OESTE).com(BotaoGamepad.INICIAR);
        f.atualizar(pegada);
        assertFalse(f.isAtiva(AcaoJogador.INICIAR_PEGADA), "segurado ao reconectar = travado");
        assertFalse(f.consumirPausa(), "Start segurado ao reconectar nao pausa");
        f.atualizar(EstadoGamepad.repouso());
        f.atualizar(pegada);
        assertTrue(f.isAtiva(AcaoJogador.INICIAR_PEGADA), "soltou e apertou de novo");
        assertTrue(f.consumirPausa());
        assertFalse(f.consumirPausa(), "uma vez por toque");
    }

    @Test
    void travaDeBloqueioEPerdaDeFoco() {
        FonteGamepad f = new FonteGamepad(J1, MAPA);
        EstadoGamepad e = EstadoGamepad.repouso().com(BotaoGamepad.DPAD_DIREITA);
        f.atualizar(e);
        f.travarPressionadas();                             // bloqueio da entrada da pegada (3A)
        f.atualizar(e);
        assertFalse(f.isAtiva(AcaoJogador.MOVER_DIREITA));
        f.atualizar(EstadoGamepad.repouso());
        f.atualizar(e);
        assertTrue(f.isAtiva(AcaoJogador.MOVER_DIREITA));
        f.liberarTodas();                                   // janela perdeu o foco
        f.atualizar(e);
        assertFalse(f.isAtiva(AcaoJogador.MOVER_DIREITA), "nada antigo volta sozinho");
    }

    @Test
    void cadaAcaoTemNoMaximoUmBotaoPorSlot() {
        for (AcaoJogador a : AcaoJogador.values()) {
            for (SlotJogador s : SlotJogador.values()) {
                assertTrue(MAPA.botoes(s, a).size() <= 1, s + " " + a);
            }
        }
    }
}
