package com.irmaosmiau.dispositivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.CamadaComando;
import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.EntradaContextual;
import com.irmaosmiau.controles.PosicaoComando;
import com.irmaosmiau.controles.SlotJogador;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Entrega 5F: mapa de gamepad editavel, esquemas, conflitos, RB opcional e gatilhos. */
class MapaGamepadEditavelTest {

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;

    private static Set<AcaoJogador> acoes(MapaGamepad m, SlotJogador s, EstadoGamepad e) {
        return m.acoesAtivas(e, s, EnumSet.noneOf(AcaoJogador.class), EnumSet.noneOf(AcaoJogador.class));
    }

    @Test
    void classicoIgualAo5D() {
        MapaGamepad m = MapaGamepad.padrao();
        assertEquals(EsquemaGamepad.CLASSICO, m.esquema(J1));
        assertEquals(EsquemaGamepad.CLASSICO, m.esquema(J2));
        // 5G (pedido do desenvolvedor): o padrao do Classico mudou (A pula, RB + A pulo alto,
        // R3 rola, L3 deita). O layout historico da 5D continua em experimental().
        assertEquals(BotaoGamepad.SUL, MapaGamepad.experimental().botao(J1, ItemGamepad.de(AcaoJogador.PULAR)).orElseThrow());
        assertEquals(BotaoGamepad.L3, MapaGamepad.experimental().botao(J1, ItemGamepad.de(AcaoJogador.ROLAR)).orElseThrow());
        assertEquals(BotaoGamepad.SUL, m.botao(J1, ItemGamepad.de(AcaoJogador.PULO_CURTO)).orElseThrow());
        assertEquals(BotaoGamepad.R3, m.botao(J1, ItemGamepad.de(AcaoJogador.ROLAR)).orElseThrow());
        assertEquals(BotaoGamepad.OESTE, m.botao(J1, ItemGamepad.de(AcaoJogador.INICIAR_PEGADA)).orElseThrow());
        assertEquals(BotaoGamepad.LESTE, m.botao(J2, ItemGamepad.de(AcaoJogador.ROMPIMENTO_TECNICO)).orElseThrow());
        assertEquals(BotaoGamepad.RB, m.botao(J1, ItemGamepad.de(AcaoJogador.CORRER)).orElseThrow());
        assertEquals(Set.of(AcaoJogador.MOVER_CIMA), acoes(m, J1, EstadoGamepad.repouso().com(BotaoGamepad.DPAD_CIMA)));
        assertEquals(EntradaContextual.NADA, m.entradaContextual(EstadoGamepad.repouso().com(BotaoGamepad.DPAD_CIMA), J1),
                "no classico o D-pad nao e tecnica");
        assertTrue(m.isValido());
    }

    @Test
    void contextualNaoDependeDoRbEAgacharSaiDoLt() {
        for (SlotJogador s : SlotJogador.values()) {
            MapaGamepad m = MapaGamepad.padrao().comEsquema(s, EsquemaGamepad.CONTEXTUAL);
            assertTrue(m.isValido(), m.problemas().toString());
            for (ItemGamepad i : m.itens(s)) {
                // 5G: o RB so e o modificador OPCIONAL do pulo alto (A sozinho pula; tecnicas nao usam RB).
                assertFalse(m.botao(s, i).orElse(null) == BotaoGamepad.RB
                        && i.comando() != ComandoGamepad.MODIFICADOR_PULO_ALTO, "RB nao e usado: " + i);
            }
            assertEquals(BotaoGamepad.LESTE, m.botao(s, ItemGamepad.de(AcaoJogador.AGACHAR)).orElseThrow());
            assertEquals(BotaoGamepad.GATILHO_ESQ, m.botao(s, ItemGamepad.de(ComandoGamepad.MODIFICADOR_CAMADA_2)).orElseThrow());
            assertEquals(BotaoGamepad.LB, m.botao(s, ItemGamepad.de(ComandoGamepad.MODIFICADOR_CAMADA_3)).orElseThrow());
            assertEquals(BotaoGamepad.DPAD_CIMA, m.botao(s, ItemGamepad.de(ComandoGamepad.TECNICA_CIMA)).orElseThrow());
        }
        // No classico, o RB pode ficar livre: correr vai para outro botao e nada e obrigatorio.
        // 5G: o RB tambem e o modificador (opcional) do pulo alto; tirando os dois, o RB fica livre.
        MapaGamepad semRb = MapaGamepad.padrao().semBotao(J1, ItemGamepad.de(AcaoJogador.CORRER))
                .semBotao(J1, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO));
        assertTrue(semRb.isValido());
        assertTrue(semRb.quemUsa(J1, BotaoGamepad.RB).isEmpty());
    }

    @Test
    void contextualProduzEntradaDoDpadEMoveSoPeloAnalogico() {
        MapaGamepad m = MapaGamepad.padrao().comEsquema(J1, EsquemaGamepad.CONTEXTUAL);
        EstadoGamepad e = EstadoGamepad.repouso().com(BotaoGamepad.DPAD_ESQUERDA).com(BotaoGamepad.GATILHO_ESQ)
                .com(BotaoGamepad.LB);
        EntradaContextual ent = m.entradaContextual(e, J1);
        assertEquals(Set.of(DirecaoTecnica.ESQUERDA), ent.direcoes());
        assertEquals(CamadaComando.QUATRO, ent.camada(), "5H: LT + LB = camada avancada");
        assertEquals(Set.of(), acoes(m, J1, EstadoGamepad.repouso().com(BotaoGamepad.DPAD_ESQUERDA)),
                "o D-pad nao anda no contextual");
        // 5H (pedido): no Contextual, andar para os lados tambem liga o "modo controle" (arrasto
        // automatico); o GamePanel so arrasta se o lutador for o aplicador de uma pegada.
        assertEquals(Set.of(AcaoJogador.MOVER_ESQUERDA, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO),
                acoes(m, J1, EstadoGamepad.repouso().comEixo(EixoGamepad.ESQ_X, -1f)));
    }

    @Test
    void lbArrastaComOAnalogicoMasNaoQuandoLtEstaSegurado() {
        MapaGamepad m = MapaGamepad.padrao().comEsquema(J1, EsquemaGamepad.CONTEXTUAL);
        EstadoGamepad arrasto = EstadoGamepad.repouso().com(BotaoGamepad.LB).comEixo(EixoGamepad.ESQ_X, 1f);
        assertTrue(acoes(m, J1, arrasto).contains(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO));
        // 5H (pedido): no Contextual o arrasto e automatico com o analogico; LT/LB nao interferem.
        assertTrue(acoes(m, J1, arrasto.com(BotaoGamepad.GATILHO_ESQ))
                .contains(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO), "arrasto automatico mesmo com LT");
    }

    @Test
    void remapearTrocarConflitosEReservados() {
        MapaGamepad m = MapaGamepad.padrao().comBotao(J1, ItemGamepad.de(AcaoJogador.CORRER), BotaoGamepad.SUL);
        assertFalse(m.isValido(), "SUL ja e pular");
        assertTrue(m.problemas().get(0).descricao().contains("A/Cruz"), m.problemas().toString());

        MapaGamepad t = MapaGamepad.padrao().trocandoSeNecessario(J1, ItemGamepad.de(AcaoJogador.CORRER), BotaoGamepad.SUL);
        assertTrue(t.isValido());
        assertEquals(BotaoGamepad.SUL, t.botao(J1, ItemGamepad.de(AcaoJogador.CORRER)).orElseThrow());
        // 5G: o SUL agora e o pulo normal; o RB antigo do correr e dividido com o pulo alto,
        // entao o pulo normal fica sem botao (nunca um mapa invalido).
        assertTrue(t.botao(J1, ItemGamepad.de(AcaoJogador.PULO_CURTO)).isEmpty(), "trocados sem conflito");
        assertEquals(BotaoGamepad.RB, t.botao(J1, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO)).orElseThrow());
        assertEquals(MapaGamepad.padrao().config(J2), t.config(J2), "slots independentes");

        assertFalse(MapaGamepad.padrao().comBotao(J1, ItemGamepad.de(AcaoJogador.PULAR), BotaoGamepad.INICIAR).isValido());
        assertFalse(MapaGamepad.padrao().comBotao(J1, ItemGamepad.de(AcaoJogador.PULAR), BotaoGamepad.GUIA).isValido());
        assertThrows(IllegalArgumentException.class,
                () -> MapaGamepad.padrao().comBotao(J1, ItemGamepad.de(ComandoGamepad.TECNICA_CIMA), BotaoGamepad.SUL),
                "comandos so no contextual");
        assertThrows(IllegalArgumentException.class,
                () -> MapaGamepad.padrao().comBotao(J2, ItemGamepad.de(AcaoJogador.INICIAR_PEGADA), BotaoGamepad.SUL));
    }

    @Test
    void restaurarPadroesDoSlotEDoEsquema() {
        MapaGamepad m = MapaGamepad.padrao().comEsquema(J1, EsquemaGamepad.CONTEXTUAL)
                .comBotao(J1, ItemGamepad.de(ComandoGamepad.TECNICA_CIMA), BotaoGamepad.RB)
                .semBotao(J1, ItemGamepad.de(AcaoJogador.PULAR));
        MapaGamepad r = m.comPadroesDo(J1);
        assertEquals(EsquemaGamepad.CONTEXTUAL, r.esquema(J1), "restaura os padroes do esquema escolhido");
        assertEquals(MapaGamepad.padraoDo(J1, EsquemaGamepad.CONTEXTUAL), r.config(J1));
        assertEquals(MapaGamepad.padrao(), r.comEsquema(J1, EsquemaGamepad.CLASSICO));
    }

    @Test
    void gatilhoComHisterese() {
        MapaGamepad m = MapaGamepad.padrao().comEsquema(J1, EsquemaGamepad.CONTEXTUAL);
        FonteGamepad f = new FonteGamepad(J1, m);
        EstadoGamepad base = EstadoGamepad.repouso().com(BotaoGamepad.DPAD_CIMA);
        f.atualizar(EstadoGamepad.repouso().comEixo(EixoGamepad.GATILHO_ESQ, 0.45f));
        assertEquals(CamadaComando.UM, f.camadaAtiva(), "0,45: ainda nao apertou");
        f.atualizar(EstadoGamepad.repouso().comEixo(EixoGamepad.GATILHO_ESQ, 0.55f));
        assertEquals(CamadaComando.DOIS, f.camadaAtiva());
        f.atualizar(EstadoGamepad.repouso().comEixo(EixoGamepad.GATILHO_ESQ, 0.40f));
        assertEquals(CamadaComando.DOIS, f.camadaAtiva(), "0,40 com histerese: continua apertado");
        f.atualizar(base.comEixo(EixoGamepad.GATILHO_ESQ, 0.40f));
        assertEquals(Optional.of(new PosicaoComando(CamadaComando.DOIS, DirecaoTecnica.CIMA)), f.consumirComandoContextual());
        f.atualizar(EstadoGamepad.repouso().comEixo(EixoGamepad.GATILHO_ESQ, 0.30f));
        assertEquals(CamadaComando.UM, f.camadaAtiva(), "abaixo de 0,35: soltou");
    }

    @Test
    void desconexaoEFocoLimpamOComandoContextual() {
        MapaGamepad m = MapaGamepad.padrao().comEsquema(J1, EsquemaGamepad.CONTEXTUAL);
        FonteGamepad f = new FonteGamepad(J1, m);
        EstadoGamepad cima = EstadoGamepad.repouso().com(BotaoGamepad.DPAD_CIMA);
        f.atualizar(cima);
        f.atualizar(null);                                  // desconectou antes de consumir
        assertTrue(f.consumirComandoContextual().isEmpty());
        f.atualizar(cima);
        assertTrue(f.consumirComandoContextual().isEmpty(), "segurado desde antes: precisa soltar");
        f.atualizar(EstadoGamepad.repouso());
        f.atualizar(cima);
        assertTrue(f.consumirComandoContextual().isPresent());
        f.atualizar(EstadoGamepad.repouso());
        f.atualizar(cima);
        f.liberarTodas();                                   // perdeu o foco
        assertTrue(f.consumirComandoContextual().isEmpty());
    }
}
