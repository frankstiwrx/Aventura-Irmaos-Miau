package com.irmaosmiau.preferencias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.audio.SaidaAudio;
import com.irmaosmiau.audio.ServicoAudio;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.persistencia.ConfiguracoesJogo;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioConfiguracoes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class PreferenciasJogoTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private static ServicoAudio audioSilencioso() {
        return new ServicoAudio(SaidaAudio.SILENCIOSA, c -> Optional.empty());
    }

    private RepositorioConfiguracoes repo() {
        return new RepositorioConfiguracoes(new DiretorioDados(tmp));
    }

    private long arquivos() throws IOException {
        try (Stream<Path> s = Files.list(tmp)) {
            return s.count();
        }
    }

    // =====================================================
    // INICIALIZACAO
    // =====================================================
    @Test
    void semArquivoUsaPadroesAplicaNoAudioENaoCriaNada() throws IOException {
        ServicoAudio audio = audioSilencioso();
        audio.setVolumeMusica(0.1);

        PreferenciasJogo p = PreferenciasJogo.carregar(repo(), audio);

        assertEquals(ConfiguracoesJogo.padrao(), p.getAtual());
        assertEquals(0.6, audio.getVolumeMusica());
        assertTrue(p.getAvisos().isEmpty());
        assertFalse(p.temAlteracoesNaoSalvas());
        assertEquals(0, arquivos(), "abrir o jogo nao grava nada");
    }

    @Test
    void preferenciasSalvasSaoAplicadasNaInicializacao() {
        repo().salvar(ConfiguracoesJogo.padrao().comMusicaAtiva(false).comVolumeEfeitos(0.3)
                .comCenarioPreferido("original"));
        ServicoAudio audio = audioSilencioso();

        PreferenciasJogo p = PreferenciasJogo.carregar(repo(), audio);

        assertFalse(audio.isMusicaAtiva());
        assertEquals(0.3, audio.getVolumeEfeitos());
        assertEquals("original", p.cenarioValido(RegistroCenarios.padrao()));
    }

    // =====================================================
    // APLICAR x SALVAR
    // =====================================================
    @Test
    void aplicarMudaOAudioNaHoraSemGravar() throws IOException {
        ServicoAudio audio = audioSilencioso();
        PreferenciasJogo p = PreferenciasJogo.carregar(repo(), audio);

        p.aplicar(p.getAtual().comVolumeMusica(0.2).comEfeitosAtivos(false));

        assertEquals(0.2, audio.getVolumeMusica());
        assertFalse(audio.isEfeitosAtivos());
        assertTrue(p.temAlteracoesNaoSalvas());
        assertEquals(0, arquivos());

        p.descartarAlteracoes();
        assertEquals(0.6, audio.getVolumeMusica());
        assertTrue(audio.isEfeitosAtivos());
        assertFalse(p.temAlteracoesNaoSalvas());
    }

    @Test
    void salvarPersisteEReiniciarRecuperaTudo() {
        PreferenciasJogo p = PreferenciasJogo.carregar(repo(), audioSilencioso());
        p.salvar(p.getAtual().comVolumeMusica(0.45).comVolumeEfeitos(0.15).comCenarioPreferido("original"));

        PreferenciasJogo depois = PreferenciasJogo.carregar(repo(), audioSilencioso());
        assertEquals(0.45, depois.getAtual().getVolumeMusica());
        assertEquals(0.15, depois.getAtual().getVolumeEfeitos());
        assertEquals("original", depois.getAtual().getCenarioPreferido());
    }

    @Test
    void atalhosGravamEPermanecemDepoisDeReiniciar() {
        ServicoAudio audio = audioSilencioso();
        PreferenciasJogo p = PreferenciasJogo.carregar(repo(), audio);

        assertTrue(p.alternarMusica().isEmpty());          // F9
        assertTrue(p.alternarEfeitos().isEmpty());         // F10
        assertTrue(p.definirCenarioPreferido("original").isEmpty());   // F4
        assertFalse(audio.isMusicaAtiva());
        assertFalse(audio.isEfeitosAtivos());

        ServicoAudio audioNovo = audioSilencioso();
        PreferenciasJogo reiniciado = PreferenciasJogo.carregar(repo(), audioNovo);
        assertFalse(reiniciado.getAtual().isMusicaAtiva());
        assertFalse(reiniciado.getAtual().isEfeitosAtivos());
        assertEquals("original", reiniciado.getAtual().getCenarioPreferido());
        assertFalse(audioNovo.isMusicaAtiva(), "aplicado no audio ao iniciar");
    }

    // =====================================================
    // PROBLEMAS
    // =====================================================
    @Test
    void arquivoCorrompidoUsaPadroesAvisaEPreservaOArquivo() throws IOException {
        Path arquivo = repo().getArquivo();
        Files.writeString(arquivo, "{ quebrado");

        PreferenciasJogo p = PreferenciasJogo.carregar(repo(), audioSilencioso());

        assertEquals(ConfiguracoesJogo.padrao(), p.getAtual());
        assertEquals(1, p.getAvisos().size());
        assertEquals("{ quebrado", Files.readString(arquivo), "nada e sobrescrito so por abrir");

        p.salvar(p.getAtual().comVolumeMusica(0.5));

        assertEquals(0.5, repo().carregar().getVolumeMusica());
        try (Stream<Path> s = Files.list(tmp)) {
            Path copia = s.filter(x -> x.getFileName().toString().contains("corrompido")).findFirst().orElseThrow();
            assertEquals("{ quebrado", Files.readString(copia), "arquivo antigo guardado como copia");
        }
        assertTrue(p.getAvisos().size() >= 2, "a copia tambem e avisada");
    }

    @Test
    void cenarioSalvoQueNaoExisteMaisUsaODojoSemApagarAPreferencia() throws IOException {
        repo().salvar(ConfiguracoesJogo.padrao().comCenarioPreferido("praia"));
        String antes = Files.readString(repo().getArquivo());

        PreferenciasJogo p = PreferenciasJogo.carregar(repo(), audioSilencioso());

        assertEquals("dojo", p.cenarioValido(RegistroCenarios.padrao()));
        assertTrue(p.getAvisos().get(0).contains("praia"), p.getAvisos().toString());
        assertEquals("praia", p.getAtual().getCenarioPreferido());
        assertEquals(antes, Files.readString(repo().getArquivo()));
    }

    @Test
    void falhaAoGravarNaoDesfazAMudancaEInformaOErro() throws IOException {
        Path arquivoNoLugarDaPasta = tmp.resolve("nao_e_pasta");
        Files.writeString(arquivoNoLugarDaPasta, "x");
        RepositorioConfiguracoes impossivel = new RepositorioConfiguracoes(new DiretorioDados(arquivoNoLugarDaPasta));
        ServicoAudio audio = audioSilencioso();
        PreferenciasJogo p = PreferenciasJogo.carregar(impossivel, audio);

        Optional<String> erro = p.alternarMusica();

        assertTrue(erro.isPresent());
        assertTrue(erro.get().contains("não foi possível salvar"), erro.get());
        assertFalse(audio.isMusicaAtiva(), "a musica desliga mesmo sem conseguir salvar");
    }

    @Test
    void emMemoriaNuncaGrava() throws IOException {
        PreferenciasJogo p = PreferenciasJogo.emMemoria(ConfiguracoesJogo.padrao(), audioSilencioso());
        p.alternarMusica();
        p.salvar(p.getAtual().comVolumeMusica(0.1));
        assertFalse(p.isPersistente());
        assertEquals(0.1, p.getAtual().getVolumeMusica());
        assertEquals(0, arquivos());
    }

    @Test
    void funcionaSemDispositivoDeAudio() {
        ServicoAudio semSom = ServicoAudio.criarPadrao();     // headless nos testes = silencioso
        assertTrue(semSom.isSilencioso());
        PreferenciasJogo p = PreferenciasJogo.carregar(repo(), semSom);
        p.salvar(p.getAtual().comVolumeMusica(0.9).comMusicaAtiva(true));
        semSom.tocarMusica("menu");
        assertEquals(0.9, semSom.getVolumeMusica());
    }
}
