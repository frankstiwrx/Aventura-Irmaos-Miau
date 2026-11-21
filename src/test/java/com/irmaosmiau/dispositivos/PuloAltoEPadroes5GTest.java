package com.irmaosmiau.dispositivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.SlotJogador;

import org.junit.jupiter.api.Test;

/** Entrega 5G: L3/R3 invertidos, A = pulo normal, modificador + A = pulo alto (configuravel). */
class PuloAltoEPadroes5GTest {

    // 5O.2 (pedido): o padrao virou o Contextual; este teste e do Classico (MapaGamepad.classico() = o padrao antigo).

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;

    private static EstadoGamepad r() {
        return EstadoGamepad.repouso();
    }

    @Test
    void novosPadroesNosDoisEsquemasEJogadores() {
        for (EsquemaGamepad e : EsquemaGamepad.values()) {
            for (SlotJogador s : SlotJogador.values()) {
                MapaGamepad m = MapaGamepad.classico().comEsquema(s, e);
                assertTrue(m.isValido(), e + " " + s + " " + m.problemas());
                assertEquals(BotaoGamepad.R3, m.botao(s, ItemGamepad.de(AcaoJogador.ROLAR)).orElseThrow(), e + "");
                assertEquals(BotaoGamepad.L3, m.botao(s, ItemGamepad.de(AcaoJogador.DEITAR_LEVANTAR)).orElseThrow());
                assertEquals(BotaoGamepad.SUL, m.botao(s, ItemGamepad.de(AcaoJogador.PULO_CURTO)).orElseThrow());
                assertTrue(m.botao(s, ItemGamepad.de(AcaoJogador.PULAR)).isEmpty(), "pulo alto = combinacao");
                assertEquals(BotaoGamepad.RB, m.botao(s, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO)).orElseThrow());
                assertEquals("RB/R1 + A/Cruz", m.rotulo(s, AcaoJogador.PULAR), "ajuda mostra a combinacao");
                assertTrue(m.itens(s).contains(ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO)),
                        "configuravel nos dois esquemas");
            }
        }
        assertEquals(BotaoGamepad.L3, MapaGamepad.experimental().botao(J1, ItemGamepad.de(AcaoJogador.ROLAR)).orElseThrow(),
                "o layout historico da 5D nao mudou");
    }

    @Test
    void aSozinhoPulaNormalERbMaisAPulaAltoSemOsDois() {
        FonteGamepad f = new FonteGamepad(J1, MapaGamepad.classico());
        f.atualizar(r().com(BotaoGamepad.SUL));
        assertTrue(f.isAtiva(AcaoJogador.PULO_CURTO));
        assertFalse(f.isAtiva(AcaoJogador.PULAR));
        f.atualizar(r());

        f.atualizar(r().com(BotaoGamepad.RB));
        f.atualizar(r().com(BotaoGamepad.RB).com(BotaoGamepad.SUL));
        assertTrue(f.isAtiva(AcaoJogador.PULAR), "RB + A = pulo alto");
        assertFalse(f.isAtiva(AcaoJogador.PULO_CURTO), "e SO o pulo alto");
        f.atualizar(r().com(BotaoGamepad.SUL));          // soltou o RB segurando o A
        assertTrue(f.isAtiva(AcaoJogador.PULAR), "decidido no toque: nada de pulo normal extra");
        assertFalse(f.isAtiva(AcaoJogador.PULO_CURTO));
        f.atualizar(r());
        assertFalse(f.isAtiva(AcaoJogador.PULAR) || f.isAtiva(AcaoJogador.PULO_CURTO));

        f.atualizar(r().com(BotaoGamepad.SUL));          // A primeiro, RB depois
        f.atualizar(r().com(BotaoGamepad.SUL).com(BotaoGamepad.RB));
        assertTrue(f.isAtiva(AcaoJogador.PULO_CURTO));
        assertFalse(f.isAtiva(AcaoJogador.PULAR), "RB no meio do toque nao vira pulo alto");
        assertTrue(f.isAtiva(AcaoJogador.CORRER), "RB continua correndo no Classico");
    }

    @Test
    void modificadorConfiguravelSemConflitoComAsCamadas() {
        MapaGamepad ctx = MapaGamepad.classico().comEsquema(J1, EsquemaGamepad.CONTEXTUAL)
                // 5O.1 (pedido): o Y voltou a ficar livre no Contextual (acrobacia = R3 + direcao em pe).
                // complemento 5Q.8 (pedido): o Y e o respirar padrao; este teste tira o respirar para usar o Y
                .semBotao(J1, ItemGamepad.de(AcaoJogador.RESPIRAR))
                .comBotao(J1, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO), BotaoGamepad.NORTE);
        assertTrue(ctx.isValido(), "Y livre no contextual (sem o respirar)");
        FonteGamepad f = new FonteGamepad(J1, ctx);
        f.atualizar(r().com(BotaoGamepad.NORTE).com(BotaoGamepad.SUL));
        assertTrue(f.isAtiva(AcaoJogador.PULAR), "Y + A = pulo alto");
        f.atualizar(r());
        f.atualizar(r().com(BotaoGamepad.RB).com(BotaoGamepad.SUL));
        assertTrue(f.isAtiva(AcaoJogador.PULO_CURTO), "RB nao e mais o modificador");

        MapaGamepad conflito = MapaGamepad.classico().comEsquema(J1, EsquemaGamepad.CONTEXTUAL)
                .comBotao(J1, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO), BotaoGamepad.GATILHO_ESQ);
        assertFalse(conflito.isValido(), "LT ja e a camada 2");
        MapaGamepad troca = MapaGamepad.classico().comEsquema(J1, EsquemaGamepad.CONTEXTUAL)
                .semBotao(J1, ItemGamepad.de(AcaoJogador.MODIFICADOR_PROJECAO))   // 5O (pedido): RB tambem e quedas
                .trocandoSeNecessario(J1, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO), BotaoGamepad.GATILHO_ESQ);
        assertTrue(troca.isValido());
        assertEquals(BotaoGamepad.RB, troca.botao(J1, ItemGamepad.de(ComandoGamepad.MODIFICADOR_CAMADA_2)).orElseThrow());

        MapaGamepad semMod = MapaGamepad.classico().semBotao(J1, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO));
        FonteGamepad g = new FonteGamepad(J1, semMod);
        g.atualizar(r().com(BotaoGamepad.RB).com(BotaoGamepad.SUL));
        assertTrue(g.isAtiva(AcaoJogador.PULO_CURTO), "sem modificador: A e sempre o pulo normal");
        assertEquals("—", semMod.rotulo(J1, AcaoJogador.PULAR));
        MapaGamepad direto = semMod.comBotao(J1, ItemGamepad.de(AcaoJogador.PULAR), BotaoGamepad.NORTE);
        assertEquals("Y/Triângulo", direto.rotulo(J1, AcaoJogador.PULAR), "pulo alto pode ter botao proprio");
    }
}
