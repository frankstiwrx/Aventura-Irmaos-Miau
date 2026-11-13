package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.audio.SaidaAudio;
import com.irmaosmiau.audio.ServicoAudio;
import com.irmaosmiau.persistencia.ConfiguracoesJogo;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioConfiguracoes;
import com.irmaosmiau.preferencias.PreferenciasJogo;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.nio.file.Path;
import java.util.Optional;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Atalhos F4/F9/F10 do GamePanel sincronizados com as preferencias salvas.
 * Usa uma PreferenciasJogo persistente numa pasta de teste (nunca a pessoal).
 */
class PreferenciasGamePanelTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private ServicoAudio audio;

    private RepositorioConfiguracoes repo() {
        return new RepositorioConfiguracoes(new DiretorioDados(tmp));
    }

    private PreferenciasJogo instalar() {
        audio = new ServicoAudio(SaidaAudio.SILENCIOSA, c -> Optional.empty());
        PreferenciasJogo p = PreferenciasJogo.carregar(repo(), audio);
        PreferenciasJogo.instalarGlobal(p);
        return p;
    }

    @AfterEach
    void restaurarGlobal() {
        // Os outros testes usam preferencias em memoria, como antes.
        PreferenciasJogo.instalarGlobal(null);
    }

    private GamePanel painel;

    private void criar(TipoPersonagem tipo) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            painel = new GamePanel(tipo, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
        });
    }

    private void atalho(String nome) throws Exception {
        SwingUtilities.invokeAndWait(() -> painel.getActionMap().get(nome).actionPerformed(null));
    }

    @Test
    void f9EF10GravamEValemDepoisDeReiniciar() throws Exception {
        instalar();
        criar(TipoPersonagem.AMBOS);

        atalho("alternarMusica");
        atalho("alternarEfeitos");

        assertFalse(audio.isMusicaAtiva());
        assertFalse(audio.isEfeitosAtivos());
        assertFalse(repo().carregar().isMusicaAtiva(), "F9 gravado");
        assertFalse(repo().carregar().isEfeitosAtivos(), "F10 gravado");

        // "Reinicia o jogo": novas preferencias lidas do disco.
        ServicoAudio audioAntes = audio;
        instalar();
        assertFalse(audio.isMusicaAtiva());
        assertFalse(audio.isEfeitosAtivos());
        assertTrue(audioAntes != audio);

        atalho("alternarMusica");
        assertTrue(repo().carregar().isMusicaAtiva());
    }

    @Test
    void f4GravaOCenarioEONovoJogoComecaNele() throws Exception {
        instalar();
        criar(TipoPersonagem.AMBOS);
        assertEquals("dojo", painel.getCenario().getId());

        atalho("proximoCenario");
        assertEquals("original", painel.getCenario().getId());
        assertEquals("original", repo().carregar().getCenarioPreferido());

        instalar();
        criar(TipoPersonagem.PRETO);
        assertEquals("original", painel.getCenario().getId(), "preferencia aplicada ao abrir a arena");
    }

    @Test
    void telaDeConfiguracoesETeclasVeemOMesmoEstado() throws Exception {
        PreferenciasJogo p = instalar();
        p.salvar(p.getAtual().comMusicaAtiva(false));     // como se a tela tivesse salvo
        criar(TipoPersonagem.AMBOS);

        atalho("alternarMusica");                         // F9 liga de novo

        assertTrue(p.getAtual().isMusicaAtiva());
        assertTrue(audio.isMusicaAtiva());
        assertTrue(repo().carregar().isMusicaAtiva());
    }

    @Test
    void cenarioSalvoInexistenteAbreNoDojo() throws Exception {
        repo().salvar(ConfiguracoesJogo.padrao().comCenarioPreferido("lua"));
        instalar();
        criar(TipoPersonagem.AMBOS);
        assertEquals("dojo", painel.getCenario().getId());
        assertEquals("lua", repo().carregar().getCenarioPreferido(), "nada sobrescrito");
    }

    @Test
    void treinoEPegada1ContinuamFuncionandoComPreferencias() throws Exception {
        instalar().salvar(ConfiguracoesJogo.padrao().comCenarioPreferido("original"));
        SwingUtilities.invokeAndWait(() -> {
            painel = new GamePanel(ConfiguracaoTreino.padrao().comCenario("original"),
                    () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            assertEquals("original", painel.getCenario().getId());
            var s = painel.getSistemaCombate();
            painel.getActionMap().get("brancoDireitaPressionada").actionPerformed(null);
            for (int i = 0; i < 200 && !s.podeTentarAcao(painel.getLutador1(), painel.getLutador2(), "pegada_1"); i++) {
                painel.atualizarUmTickParaTestes();
            }
            painel.getActionMap().get("brancoDireitaSolta").actionPerformed(null);
            painel.getActionMap().get("brancoPegada1Pressionada").actionPerformed(null);
            painel.atualizarUmTickParaTestes();
            for (int i = 0; i < 30; i++) {
                painel.atualizarUmTickParaTestes();
            }
            assertTrue(s.isInteracaoEstabelecida());
        });
    }
}
