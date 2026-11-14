package com.irmaosmiau.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5H: gamepad.json da 5G (Contextual padrao) -> padrao 5H com aviso; personalizados intactos. */
class GamepadPadroes5HPersistenciaTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private RepositorioGamepad repo() {
        return new RepositorioGamepad(new DiretorioDados(tmp));
    }

    @Test
    void contextualPadraoDa5GVaiParaO5HComAviso() {
        // Grava o padrao da 5G (LB tambem arrastava) pelo proprio repositorio.
        MapaGamepad m5g = MapaGamepad.padrao()
                .comConfig(SlotJogador.JOGADOR_1, MapaGamepad.padrao5G(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL));
        repo().salvar(m5g);
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertEquals(MapaGamepad.padraoDo(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL), p.getAtual().config(SlotJogador.JOGADOR_1));
        assertTrue(p.getAtual().botao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO)).isEmpty(),
                "LB fica so como camada 3; o arrasto e automatico");
        assertEquals(1, p.getAvisos().size(), p.getAvisos().toString());
        assertTrue(p.getAvisos().get(0).contains("analógico direito"), p.getAvisos().get(0));
    }

    @Test
    void padrao5HSalvoNaoGeraAvisoEClassicoNaoMuda() {
        repo().salvar(MapaGamepad.padrao().comEsquema(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL));
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertTrue(p.getAvisos().isEmpty(), p.getAvisos().toString());
        // 5I (pedido): padraoDo agora e o da 5I (J2 com pegada); a 5H continua sendo comparada pelo padrao5H.
        assertEquals(MapaGamepad.padrao5G(SlotJogador.JOGADOR_2, EsquemaGamepad.CLASSICO),
                MapaGamepad.padrao5H(SlotJogador.JOGADOR_2, EsquemaGamepad.CLASSICO), "o Classico nao mudou na 5H");
    }

    @Test
    void contextualPersonalizadoComLbArrastandoEMantido() {
        MapaGamepad meu = MapaGamepad.padrao().comConfig(SlotJogador.JOGADOR_1,
                MapaGamepad.padrao5G(SlotJogador.JOGADOR_1, EsquemaGamepad.CONTEXTUAL))
                .semBotao(SlotJogador.JOGADOR_1, ItemGamepad.de(AcaoJogador.RESPIRAR));
        repo().salvar(meu);
        PreferenciasGamepad p = PreferenciasGamepad.carregar(repo());
        assertEquals(meu, p.getAtual(), "nada sobrescrito em silencio");
        assertTrue(p.getAvisos().isEmpty(), "ja tinha o modificador do pulo alto: sem aviso");
    }
}
