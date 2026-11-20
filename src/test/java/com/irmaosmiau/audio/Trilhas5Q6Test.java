package com.irmaosmiau.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.persistencia.PastaDeTeste;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Entrega 5Q.6: trilha sonora por contexto. Mapeamento de todas as telas, seis faixas de
 * combate em rodizio sem repetir a anterior, nada sobreposto, volume/mudo, arquivos
 * externos substituiveis, fallback para os internos e arquivo invalido sem derrubar o jogo.
 */
class Trilhas5Q6Test {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path pasta;

    /** Saida que registra o que toca e deixa o teste "terminar" a faixa. */
    static final class Saida implements SaidaAudio {
        final List<Rep> tocando = new ArrayList<>();
        final List<String> log = new ArrayList<>();

        final class Rep implements Reproducao {
            final String origem;
            boolean ativa = true;
            double volume;

            Rep(String origem, double volume) {
                this.origem = origem;
                this.volume = volume;
            }

            @Override
            public void parar() {
                ativa = false;
            }

            @Override
            public void setVolume(double v) {
                volume = v;
            }

            @Override
            public boolean isAtiva() {
                return ativa;
            }
        }

        @Override
        public Reproducao tocar(SomCarregado som, boolean emLoop, double volume) {
            Rep r = new Rep(som.getOrigem(), volume);
            tocando.add(r);
            log.add((emLoop ? "loop " : "uma ") + som.getOrigem());
            return r;
        }

        @Override
        public boolean isSilenciosa() {
            return false;
        }

        long ativas() {
            return tocando.stream().filter(Rep::isAtiva).count();
        }

        Rep ultima() {
            return tocando.get(tocando.size() - 1);
        }
    }

    private static Optional<SomCarregado> interno(String caminho) {
        if (!caminho.endsWith("/menu.wav") && !caminho.endsWith("/treino.wav")) {
            return Optional.empty();                         // so as duas musicas proprias existem
        }
        return Optional.of(new SomCarregado(caminho, new AudioFormat(22050, 16, 1, true, false), new byte[] {0, 0}));
    }

    private TrilhaSonora trilha(Saida saida, ServicoAudio[] s) {
        s[0] = new ServicoAudio(saida, c -> Optional.empty());
        BibliotecaMusical b = new BibliotecaMusical(pasta, Trilhas5Q6Test::interno, CarregadorSom::carregarArquivo);
        return new TrilhaSonora(s[0], b, Runnable::run, new Random(7), false);
    }

    private static void wav(Path f) throws Exception {
        Files.createDirectories(f.getParent());
        AudioFormat fmt = new AudioFormat(8000, 16, 1, true, false);
        byte[] dados = new byte[1600];
        try (AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(dados), fmt, 800)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, f.toFile());
        }
    }

    @Test
    void todasAsTelasTemMapeamentoProprioESeisFaixasDeCombate() {
        assertEquals(6, ContextoMusical.combate().size());
        for (ContextoMusical c : ContextoMusical.values()) {
            assertFalse(c.getProvisorias().isEmpty(), c + " tem musica provisoria");
        }
        List<String> ids = new ArrayList<>();
        for (ContextoMusical c : ContextoMusical.values()) {
            ids.add(c.getId());
        }
        assertTrue(ids.containsAll(List.of("menu_inicial", "menu_principal", "selecao_personagens", "menu_treinamento",
                "treinamento", "laboratorio_tecnico", "criacao_personagens", "configuracoes", "resultados",
                "combate_01", "combate_06")));
    }

    @Test
    void menusTocamEmLoopSemReiniciarESemSobrepor() {
        Saida saida = new Saida();
        ServicoAudio[] s = new ServicoAudio[1];
        TrilhaSonora t = trilha(saida, s);
        t.tocar(ContextoMusical.MENU_PRINCIPAL);
        t.tocar(ContextoMusical.MENU_PRINCIPAL);                 // a mesma tela nao reinicia
        assertEquals(1, saida.tocando.size());
        assertTrue(saida.log.get(0).startsWith("loop ") && saida.log.get(0).endsWith("/menu.wav"), "provisoria");
        t.tocar(ContextoMusical.TREINAMENTO);
        assertEquals(1, saida.ativas(), "nunca duas musicas juntas");
        assertTrue(saida.ultima().origem.endsWith("/treino.wav"));
    }

    @Test
    void combateFazRodizioSemRepetirAAnteriorENaoReiniciaNoMesmoCombate() {
        Saida saida = new Saida();
        ServicoAudio[] s = new ServicoAudio[1];
        TrilhaSonora t = trilha(saida, s);
        t.tocar(ContextoMusical.COMBATE_01);
        assertTrue(t.isRodizioCombate());
        assertEquals(1, saida.tocando.size());
        assertTrue(saida.log.get(0).startsWith("uma "), "faixa de combate toca uma vez e passa para a proxima");
        t.tocarCombate();                                        // F5 / outra tecnica: continua
        assertEquals(1, saida.tocando.size());
        for (int i = 0; i < 8; i++) {
            String antes = saida.ultima().origem;
            saida.ultima().ativa = false;                        // a faixa terminou
            t.proximaDoCombate();
            assertNotEquals(antes, saida.ultima().origem, "nao repete a anterior (ha outra distinta)");
            assertEquals(1, saida.ativas());
        }
    }

    @Test
    void pastaExternaSubstituiSemCodigoEInvalidoVoltaParaOInterno() throws Exception {
        wav(pasta.resolve("combate").resolve("combate_03.wav"));
        wav(pasta.resolve("menus").resolve("menu_principal.wav"));
        Files.createDirectories(pasta.resolve("menus"));
        Files.writeString(pasta.resolve("menus").resolve("configuracoes.wav"), "isto nao e um wav");
        Saida saida = new Saida();
        ServicoAudio[] s = new ServicoAudio[1];
        TrilhaSonora t = trilha(saida, s);
        t.tocar(ContextoMusical.MENU_PRINCIPAL);
        assertTrue(t.getOrigemAtual().startsWith("arquivo:"), "a externa vence: " + t.getOrigemAtual());
        assertTrue(saida.ultima().origem.endsWith("menu_principal.wav"));
        t.tocar(ContextoMusical.CONFIGURACOES);
        assertTrue(saida.ultima().origem.endsWith("/menu.wav"), "arquivo invalido: volta para a provisoria");
        BibliotecaMusical b = new BibliotecaMusical(pasta, Trilhas5Q6Test::interno, CarregadorSom::carregarArquivo);
        assertTrue(b.candidatos(ContextoMusical.COMBATE_03).get(0).endsWith("combate_03.wav"));
        assertTrue(b.candidatos(ContextoMusical.COMBATE_04).get(0).startsWith("interno:"), "sem externa: interna");
    }

    @Test
    void volumeEMudoValemParaATrilha() {
        Saida saida = new Saida();
        ServicoAudio[] s = new ServicoAudio[1];
        TrilhaSonora t = trilha(saida, s);
        s[0].setVolumeMusica(0.3);
        t.tocar(ContextoMusical.MENU_INICIAL);
        assertEquals(0.3, saida.ultima().volume, 1e-9);
        s[0].setMusicaAtiva(false);
        assertEquals(0, saida.ativas(), "mudo para");
        t.tocar(ContextoMusical.TREINAMENTO);
        assertEquals(0, saida.ativas(), "continua mudo");
        s[0].setMusicaAtiva(true);
        assertEquals(1, saida.ativas());
        assertTrue(saida.ultima().origem.endsWith("/treino.wav"), "religa na musica da tela atual");
    }

    @Test
    void arquivoReadDoDiscoEFechamentoSemErro() throws Exception {
        Path f = pasta.resolve("t.wav");
        wav(f);
        assertTrue(CarregadorSom.carregarArquivo(f).isPresent());
        assertTrue(CarregadorSom.carregarArquivo(pasta.resolve("nao_existe.wav")).isEmpty());
        assertTrue(CarregadorSom.carregarArquivo(new File(pasta.toFile(), "x").toPath()).isEmpty());
        Saida saida = new Saida();
        ServicoAudio[] s = new ServicoAudio[1];
        TrilhaSonora t = trilha(saida, s);
        t.tocar(ContextoMusical.COMBATE_02);
        t.fechar();
        assertEquals(0, saida.ativas());
    }

    @Test
    void wavsInternosDoJogoContinuamNoJar() {
        assertTrue(CarregadorSom.carregar(ServicoAudio.PASTA_MUSICAS + "menu.wav").isPresent());
        assertTrue(CarregadorSom.carregar(ServicoAudio.PASTA_MUSICAS + "treino.wav").isPresent());
    }
}
