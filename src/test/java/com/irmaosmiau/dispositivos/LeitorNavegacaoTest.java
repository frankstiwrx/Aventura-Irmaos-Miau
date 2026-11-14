package com.irmaosmiau.dispositivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Entrega 5I (complemento): navegacao de menus pelo gamepad, sem Swing. */
class LeitorNavegacaoTest {

    private static EstadoGamepad r() {
        return EstadoGamepad.repouso();
    }

    private static LeitorNavegacao armado() {
        LeitorNavegacao l = new LeitorNavegacao();
        l.ler(r(), 0);
        assertTrue(l.isArmado());
        return l;
    }

    @Test
    void comecaDesarmadoEOBotaoSeguradoDaTelaAnteriorNaoVale() {
        LeitorNavegacao l = new LeitorNavegacao();
        assertEquals(List.of(), l.ler(r().com(BotaoGamepad.SUL), 0), "A ainda segurado ao entrar na tela");
        assertEquals(List.of(), l.ler(r().com(BotaoGamepad.SUL), 100));
        assertFalse(l.isArmado());
        assertEquals(List.of(), l.ler(r(), 200), "soltou: arma, sem comando");
        assertEquals(List.of(Comando.CONFIRMAR), l.ler(r().com(BotaoGamepad.SUL), 300));
        assertEquals(List.of(), l.ler(r().com(BotaoGamepad.SUL), 400), "segurar nao repete o A");
    }

    @Test
    void desarmarExigeSoltarTudo() {
        LeitorNavegacao l = armado();
        l.desarmar();
        assertEquals(List.of(), l.ler(r().com(BotaoGamepad.DPAD_BAIXO), 10));
        assertEquals(List.of(), l.ler(r().com(BotaoGamepad.LESTE), 20));
        l.ler(r(), 30);
        assertEquals(List.of(Comando.VOLTAR), l.ler(r().com(BotaoGamepad.LESTE), 40));
    }

    @Test
    void dpadMoveERepeteComAtrasoControlado() {
        LeitorNavegacao l = armado();
        EstadoGamepad baixo = r().com(BotaoGamepad.DPAD_BAIXO);
        assertEquals(List.of(Comando.PROXIMO), l.ler(baixo, 1000));
        assertEquals(List.of(), l.ler(baixo, 1000 + LeitorNavegacao.ATRASO_MS - 1), "nada antes do atraso");
        assertEquals(List.of(Comando.PROXIMO), l.ler(baixo, 1000 + LeitorNavegacao.ATRASO_MS));
        assertEquals(List.of(), l.ler(baixo, 1000 + LeitorNavegacao.ATRASO_MS + LeitorNavegacao.REPETICAO_MS - 1));
        assertEquals(List.of(Comando.PROXIMO),
                l.ler(baixo, 1000 + LeitorNavegacao.ATRASO_MS + LeitorNavegacao.REPETICAO_MS));
        assertEquals(List.of(), l.ler(r(), 2000));
        assertEquals(List.of(Comando.ANTERIOR), l.ler(r().com(BotaoGamepad.DPAD_CIMA), 2010));
        assertEquals(List.of(Comando.ESQUERDA), l.ler(r().com(BotaoGamepad.DPAD_ESQUERDA), 2020),
                "trocar de direcao vale na hora");
        assertEquals(List.of(Comando.DIREITA), l.ler(r().com(BotaoGamepad.DPAD_DIREITA), 2030));
    }

    @Test
    void analogicoComZonaMortaEHisterese() {
        LeitorNavegacao l = armado();
        assertEquals(List.of(), l.ler(r().comEixo(EixoGamepad.ESQ_Y, 0.4f), 0), "ruido/leve: nada");
        assertEquals(List.of(Comando.PROXIMO), l.ler(r().comEixo(EixoGamepad.ESQ_Y, 0.7f), 10));
        assertEquals(List.of(), l.ler(r().comEixo(EixoGamepad.ESQ_Y, 0.4f), 20), "entre 0,3 e 0,5: continua a mesma");
        assertEquals(List.of(), l.ler(r().comEixo(EixoGamepad.ESQ_Y, 0.2f), 30), "voltou ao neutro");
        assertEquals(List.of(Comando.PROXIMO), l.ler(r().comEixo(EixoGamepad.ESQ_Y, 0.7f), 40), "nova inclinacao");
        l.ler(r(), 50);
        assertEquals(List.of(Comando.ANTERIOR), l.ler(r().comEixo(EixoGamepad.ESQ_Y, -0.8f), 60));
        l.ler(r(), 70);
        assertEquals(List.of(Comando.DIREITA), l.ler(r().comEixo(EixoGamepad.ESQ_X, 0.8f), 80));
        l.ler(r(), 90);
        assertEquals(List.of(Comando.PROXIMO),
                l.ler(r().comEixo(EixoGamepad.ESQ_X, 0.6f).comEixo(EixoGamepad.ESQ_Y, 0.7f), 100),
                "diagonal: vertical tem prioridade");
    }

    @Test
    void desconectarDesarmaENaoUsaRbNemOutrosBotoes() {
        LeitorNavegacao l = armado();
        assertEquals(List.of(), l.ler(r().com(BotaoGamepad.RB).com(BotaoGamepad.NORTE).com(BotaoGamepad.INICIAR), 0),
                "so D-pad/analogico, A e B navegam");
        assertEquals(List.of(), l.ler(null, 10));
        assertFalse(l.isArmado());
        assertEquals(List.of(), l.ler(r().com(BotaoGamepad.SUL), 20), "A segurado ao reconectar nao vale");
    }
}
