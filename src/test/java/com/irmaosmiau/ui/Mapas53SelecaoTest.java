package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5P.3: os dois mapas novos aparecem na selecao do treino, na ordem, e iniciam a luta. */
class Mapas53SelecaoTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    @AfterEach
    void limparSessao() {
        SelecaoLutadoresPanel.reiniciarInicioDaSessao();
    }

    private String treinarNoMapa(int vezesDireita) throws Exception {
        AtomicReference<String> mapa = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            SelecaoPartidaPanel sel = SelecaoPartidaPanel.treino(CatalogoPersonagens.padrao(),
                    new OpcoesDeLutadores(CatalogoPersonagens.padrao(), new ServicoPerfis(
                            new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao())),
                    RegistroCenarios.padrao(), RegistroCenarios.ID_PADRAO, InicioLuta.DE_JOELHOS,
                    (j, p, c, i) -> mapa.set(c), () -> { }, () -> null, () -> null);
            SlotJogador j1 = SlotJogador.JOGADOR_1;
            personalizacao(sel);                              // treino igual ao JOGAR (pedido): a tela da arena
            assertEquals(ConfiguracaoLuta.Area.ARENA, sel.configuracao().foco());
            for (int i = 0; i < vezesDireita; i++) {
                sel.comandar(j1, Comando.DIREITA);
            }
            sel.comandar(j1, Comando.CONFIRMAR);              // INICIO
            sel.comandar(j1, Comando.CONFIRMAR);              // INICIAR TREINO
            sel.comandar(j1, Comando.CONFIRMAR);
        });
        return mapa.get();
    }

    @Test
    void academiaEFortalezaNaSelecaoDepoisDosQuatroAntigos() throws Exception {
        assertEquals("torneio", treinarNoMapa(3));
        assertEquals("academia", treinarNoMapa(4));
        assertEquals("fortaleza_neve", treinarNoMapa(5));
        // 5P.4 (pedido): os dois mapas novos vem em seguida; o ciclo volta ao dojo depois dos oito
        assertEquals("academia_miau", treinarNoMapa(6));
        assertEquals("campeonato", treinarNoMapa(7));
        assertEquals("dojo", treinarNoMapa(8), "o ciclo volta ao dojo");
    }

    /**
     * Selecao do TREINO igual a do JOGAR (pedido): o mesmo controle confirma faixa e kimono do lutador e
     * depois personagem, faixa e kimono do parceiro, e AVANCA para a tela da arena.
     */
    private static void personalizacao(SelecaoPartidaPanel s) {
        for (int i = 0; i < 8 && !s.naConfiguracao(); i++) {
            s.comandar(SlotJogador.JOGADOR_1, Comando.CONFIRMAR);
        }
    }
}
