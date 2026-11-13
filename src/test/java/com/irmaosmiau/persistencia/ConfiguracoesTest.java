package com.irmaosmiau.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.audio.SaidaAudio;
import com.irmaosmiau.audio.ServicoAudio;
import com.irmaosmiau.cenario.RegistroCenarios;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class ConfiguracoesTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private RepositorioConfiguracoes repo;

    @BeforeEach
    void preparar() {
        repo = new RepositorioConfiguracoes(new DiretorioDados(tmp));
    }

    // =====================================================
    // PADROES
    // =====================================================
    @Test
    void padroesEspelhamOComportamentoAtualDoJogo() {
        ConfiguracoesJogo p = ConfiguracoesJogo.padrao();
        ServicoAudio audioAtual = new ServicoAudio(SaidaAudio.SILENCIOSA, c -> Optional.empty());

        assertEquals(audioAtual.isMusicaAtiva(), p.isMusicaAtiva());
        assertEquals(audioAtual.isEfeitosAtivos(), p.isEfeitosAtivos());
        assertEquals(audioAtual.getVolumeMusica(), p.getVolumeMusica());
        assertEquals(audioAtual.getVolumeEfeitos(), p.getVolumeEfeitos());
        assertEquals(RegistroCenarios.ID_PADRAO, p.getCenarioPreferido());
        assertTrue(RegistroCenarios.padrao().contem(p.getCenarioPreferido()));
    }

    @Test
    void semArquivoCarregaPadroesENaoCriaNada() throws IOException {
        assertEquals(ConfiguracoesJogo.padrao(), repo.carregar());
        try (Stream<Path> s = Files.list(tmp)) {
            assertEquals(0, s.count());
        }
    }

    // =====================================================
    // SALVAR / CARREGAR
    // =====================================================
    @Test
    void salvaECarregaVolumesEPreferencias() {
        ConfiguracoesJogo c = ConfiguracoesJogo.padrao()
                .comMusicaAtiva(false)
                .comEfeitosAtivos(true)
                .comVolumeMusica(0.25)
                .comVolumeEfeitos(1.0)
                .comCenarioPreferido("Original");

        repo.salvar(c);
        ConfiguracoesJogo lida = new RepositorioConfiguracoes(new DiretorioDados(tmp)).carregar();

        assertEquals(c, lida);
        assertFalse(lida.isMusicaAtiva());
        assertEquals(0.25, lida.getVolumeMusica());
        assertEquals(1.0, lida.getVolumeEfeitos());
        assertEquals("original", lida.getCenarioPreferido());
    }

    @Test
    void arquivoSalvoTemTipoVersaoESecoes() throws IOException {
        repo.salvar(ConfiguracoesJogo.padrao());
        String json = Files.readString(repo.getArquivo());

        assertTrue(json.contains("\"tipo\": \"CONFIGURACOES_IRMAOS_MIAU\""), json);
        assertTrue(json.contains("\"versaoEsquema\": 1"), json);
        assertTrue(json.contains("\"audio\": {"), json);
        assertTrue(json.contains("\"volumeMusica\": 0.6"), json);
        assertTrue(json.contains("\"cenarioPreferido\": \"dojo\""), json);
    }

    @Test
    void arquivoParcialUsaPadroesNosCamposAusentes() throws IOException {
        Files.writeString(repo.getArquivo(),
                "{\"tipo\": \"CONFIGURACOES_IRMAOS_MIAU\", \"versaoEsquema\": 1, \"audio\": {\"volumeMusica\": 0.1}}");

        ConfiguracoesJogo c = repo.carregar();
        assertEquals(0.1, c.getVolumeMusica());
        assertEquals(ConfiguracoesJogo.VOLUME_EFEITOS_PADRAO, c.getVolumeEfeitos());
        assertEquals("dojo", c.getCenarioPreferido());
    }

    // =====================================================
    // VALIDACOES
    // =====================================================
    @Test
    void valoresInvalidosSaoRecusados() {
        ConfiguracoesJogo p = ConfiguracoesJogo.padrao();
        assertThrows(IllegalArgumentException.class, () -> p.comVolumeMusica(1.01));
        assertThrows(IllegalArgumentException.class, () -> p.comVolumeEfeitos(-0.1));
        assertThrows(IllegalArgumentException.class, () -> p.comVolumeMusica(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> p.comCenarioPreferido("../dojo"));
        assertThrows(IllegalArgumentException.class, () -> p.comCenarioPreferido(" "));
        assertThrows(IllegalArgumentException.class, () -> p.comCenarioPreferido(null));
        assertThrows(IllegalArgumentException.class, () -> repo.salvar(null));
    }

    @Test
    void valoresInvalidosNoArquivoGeramErroClaroSemAlterarOArquivo() throws IOException {
        String conteudo = "{\"tipo\": \"CONFIGURACOES_IRMAOS_MIAU\", \"versaoEsquema\": 1,"
                + " \"audio\": {\"volumeMusica\": 3.5}}";
        Files.writeString(repo.getArquivo(), conteudo);

        PersistenciaException ex = assertThrows(PersistenciaException.class, repo::carregar);
        assertEquals(PersistenciaException.Motivo.DADOS_INVALIDOS, ex.getMotivo());
        assertTrue(ex.getMessage().contains("volumeMusica"), ex.getMessage());
        assertEquals(conteudo, Files.readString(repo.getArquivo()));

        Files.writeString(repo.getArquivo(), "{\"tipo\": \"CONFIGURACOES_IRMAOS_MIAU\", \"versaoEsquema\": 1,"
                + " \"audio\": {\"musicaAtiva\": \"sim\"}}");
        assertEquals(PersistenciaException.Motivo.DADOS_INVALIDOS,
                assertThrows(PersistenciaException.class, repo::carregar).getMotivo());

        Files.writeString(repo.getArquivo(), "{\"tipo\": \"CONFIGURACOES_IRMAOS_MIAU\", \"versaoEsquema\": 1,"
                + " \"tela\": {}}");
        PersistenciaException extra = assertThrows(PersistenciaException.class, repo::carregar);
        assertTrue(extra.getMessage().contains("[tela]"), extra.getMessage());
    }

    @Test
    void versaoFuturaEIncompativel() throws IOException {
        Files.writeString(repo.getArquivo(), "{\"tipo\": \"CONFIGURACOES_IRMAOS_MIAU\", \"versaoEsquema\": 2}");
        assertEquals(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL,
                assertThrows(PersistenciaException.class, repo::carregar).getMotivo());
    }

    // =====================================================
    // MODO TOLERANTE: NUNCA SILENCIOSO
    // =====================================================
    @Test
    void carregarOuPadraoGuardaOArquivoCorrompidoEAvisa() throws IOException {
        Files.writeString(repo.getArquivo(), "{ isto nao e json");

        ResultadoCarga<ConfiguracoesJogo> r = repo.carregarOuPadrao();

        assertEquals(ConfiguracoesJogo.padrao(), r.getValor());
        assertTrue(r.temAvisos());
        assertTrue(r.getAvisos().get(0).contains("configuracoes.json.corrompido-"), r.getAvisos().get(0));
        assertFalse(Files.exists(repo.getArquivo()), "o arquivo ruim foi renomeado, nao reaproveitado");
        try (Stream<Path> s = Files.list(tmp)) {
            Path copia = s.filter(p -> p.getFileName().toString().contains("corrompido")).findFirst().orElseThrow();
            assertEquals("{ isto nao e json", Files.readString(copia), "conteudo original preservado");
        }

        // Depois disso, salvar volta a funcionar normalmente.
        repo.salvar(r.getValor().comVolumeMusica(0.4));
        assertEquals(0.4, repo.carregar().getVolumeMusica());
    }

    @Test
    void carregarOuPadraoSemProblemasNaoTemAvisos() {
        repo.salvar(ConfiguracoesJogo.padrao().comEfeitosAtivos(false));
        ResultadoCarga<ConfiguracoesJogo> r = repo.carregarOuPadrao();
        assertFalse(r.temAvisos());
        assertFalse(r.getValor().isEfeitosAtivos());
    }
}
