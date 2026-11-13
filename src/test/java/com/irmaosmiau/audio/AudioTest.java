package com.irmaosmiau.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sound.sampled.AudioFormat;

import org.junit.jupiter.api.Test;

class AudioTest {

    // =====================================================
    // SAIDA FALSA: registra o que seria tocado
    // =====================================================
    static final class SaidaFalsa implements SaidaAudio {

        final List<String> log = new ArrayList<>();
        final List<FalsaReproducao> reproducoes = new ArrayList<>();

        final class FalsaReproducao implements Reproducao {
            final String origem;
            final boolean loop;
            double volume;
            boolean ativa = true;

            FalsaReproducao(String origem, boolean loop, double volume) {
                this.origem = origem;
                this.loop = loop;
                this.volume = volume;
            }

            @Override
            public void parar() {
                if (ativa) {
                    log.add("parar " + origem);
                }
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
            log.add((emLoop ? "loop " : "tocar ") + som.getOrigem());
            FalsaReproducao r = new FalsaReproducao(som.getOrigem(), emLoop, volume);
            reproducoes.add(r);
            return r;
        }

        @Override
        public boolean isSilenciosa() {
            return false;
        }
    }

    /** Carregador que "encontra" qualquer arquivo, exceto os que contem "faltando". */
    private static Optional<SomCarregado> carregadorFalso(String caminho) {
        if (caminho.contains("faltando")) {
            return Optional.empty();
        }
        AudioFormat f = new AudioFormat(22050, 16, 1, true, false);
        return Optional.of(new SomCarregado(caminho, f, new byte[]{0, 0, 0, 0}));
    }

    private static ServicoAudio servicoFalso(SaidaFalsa saida) {
        return new ServicoAudio(saida, AudioTest::carregadorFalso);
    }

    // =====================================================
    // CARREGAMENTO REAL (CLASSPATH / JAR)
    // =====================================================
    @Test
    void wavsProvisoriosDoJogoSaoCarregadosDoClasspath() {
        for (String c : new String[]{"/audio/musica/menu.wav", "/audio/musica/treino.wav",
            "/audio/efeitos/pegada.wav", "/audio/efeitos/rompimento.wav"}) {
            SomCarregado s = CarregadorSom.carregar(c).orElseThrow(() -> new AssertionError("ausente: " + c));
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, s.getFormato().getEncoding(), c);
            assertEquals(16, s.getFormato().getSampleSizeInBits(), c);
            assertTrue(s.duracaoSegundos() > 0.1, c);
        }
        assertTrue(CarregadorSom.carregar("/audio/musica/menu.wav").get().duracaoSegundos() > 5,
                "a musica do menu e um loop de varios segundos");
    }

    @Test
    void arquivoAusenteOuInvalidoNaoLancaExcecao() {
        assertTrue(CarregadorSom.carregar("/audio/musica/combate.wav").isEmpty(),
                "combate.wav ainda nao existe (proposital)");
        assertTrue(CarregadorSom.carregar("/audio/nao/existe.wav").isEmpty());
        assertTrue(CarregadorSom.carregar("/audio/LEIAME.md").isEmpty(), "texto nao e WAV");
        assertTrue(CarregadorSom.carregar(null).isEmpty());
    }

    @Test
    void servicoPadraoEmAmbienteHeadlessESilenciosoEFunciona() {
        ServicoAudio s = ServicoAudio.criarPadrao();
        assertTrue(s.isSilencioso());
        s.tocarMusica(AudioJogo.MUSICA_MENU);
        s.tocarEfeito(AudioJogo.EFEITO_PEGADA);
        s.tocarMusica(AudioJogo.MUSICA_COMBATE);       // ausente
        assertEquals(AudioJogo.MUSICA_COMBATE, s.getMusicaDesejada());
        assertTrue(s.existeMusica(AudioJogo.MUSICA_TREINO));
        assertFalse(s.existeMusica(AudioJogo.MUSICA_COMBATE));
        s.fechar();
    }

    // =====================================================
    // LOGICA DO SERVICO
    // =====================================================
    @Test
    void musicaTocaEmLoopETrocaParaAAnterior() {
        SaidaFalsa saida = new SaidaFalsa();
        ServicoAudio s = servicoFalso(saida);

        s.tocarMusica("menu");
        s.tocarMusica("menu");          // pedir de novo nao reinicia
        s.tocarMusica("treino");

        assertEquals(List.of(
                "loop /audio/musica/menu.wav",
                "parar /audio/musica/menu.wav",
                "loop /audio/musica/treino.wav"), saida.log);
        assertTrue(s.isTocandoMusica());
    }

    @Test
    void musicaAusenteParaAAnteriorEFicaEmSilencio() {
        SaidaFalsa saida = new SaidaFalsa();
        ServicoAudio s = servicoFalso(saida);

        s.tocarMusica("menu");
        s.tocarMusica("faltando");

        assertEquals(List.of("loop /audio/musica/menu.wav", "parar /audio/musica/menu.wav"), saida.log);
        assertFalse(s.isTocandoMusica());
        assertEquals("faltando", s.getMusicaDesejada());
    }

    @Test
    void desligarERelicarMusicaRetomaAMusicaPedida() {
        SaidaFalsa saida = new SaidaFalsa();
        ServicoAudio s = servicoFalso(saida);

        s.tocarMusica("treino");
        s.setMusicaAtiva(false);
        assertFalse(s.isTocandoMusica());
        s.tocarMusica("menu");           // pedida enquanto desligada: nao toca
        assertFalse(s.isTocandoMusica());
        s.setMusicaAtiva(true);

        assertTrue(s.isTocandoMusica());
        assertEquals("loop /audio/musica/menu.wav", saida.log.get(saida.log.size() - 1));
    }

    @Test
    void efeitosSaoIndependentesDaMusicaEPodemSerDesligados() {
        SaidaFalsa saida = new SaidaFalsa();
        ServicoAudio s = servicoFalso(saida);

        s.tocarMusica("menu");
        s.tocarEfeito("pegada");
        s.tocarEfeito("pegada");          // sobrepostos
        assertTrue(s.isTocandoMusica(), "efeito nao interrompe a musica");
        assertEquals(2, saida.log.stream().filter(l -> l.equals("tocar /audio/efeitos/pegada.wav")).count());

        s.setEfeitosAtivos(false);
        int antes = saida.log.size();
        s.tocarEfeito("rompimento");
        assertEquals(antes, saida.log.size(), "efeitos desligados nao tocam");
        assertTrue(s.isTocandoMusica(), "desligar efeitos nao desliga a musica");

        s.setEfeitosAtivos(true);
        s.tocarEfeito("faltando");        // ausente: silencio, sem erro
    }

    @Test
    void volumeELimitadoEAplicadoNaMusicaAtual() {
        SaidaFalsa saida = new SaidaFalsa();
        ServicoAudio s = servicoFalso(saida);

        s.setVolumeMusica(0.3);
        s.tocarMusica("menu");
        assertEquals(0.3, saida.reproducoes.get(0).volume, 1e-9);

        s.setVolumeMusica(5.0);
        assertEquals(1.0, s.getVolumeMusica());
        assertEquals(1.0, saida.reproducoes.get(0).volume, 1e-9);

        s.setVolumeEfeitos(-2);
        assertEquals(0.0, s.getVolumeEfeitos());

        assertEquals(0.0, ServicoAudio.volumeParaDecibeis(1.0), 1e-9);
        assertEquals(-6.02, ServicoAudio.volumeParaDecibeis(0.5), 0.01);
        assertEquals(-80.0, ServicoAudio.volumeParaDecibeis(0.0), 1e-9);
    }

    @Test
    void fecharLiberaTudoEImpedeNovosSons() {
        SaidaFalsa saida = new SaidaFalsa();
        ServicoAudio s = servicoFalso(saida);

        s.tocarMusica("menu");
        s.tocarEfeito("pegada");
        s.fechar();

        assertTrue(saida.reproducoes.stream().noneMatch(r -> r.ativa), "todas as linhas paradas");
        int antes = saida.log.size();
        s.tocarMusica("treino");
        s.tocarEfeito("pegada");
        assertEquals(antes, saida.log.size());
    }
}
