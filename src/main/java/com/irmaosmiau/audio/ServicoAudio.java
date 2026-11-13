package com.irmaosmiau.audio;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Servico de audio do jogo: uma musica em loop por vez e efeitos
 * independentes.
 *
 * Ids sao convertidos em arquivos do classpath:
 * <pre>
 * musica "menu"    -> /audio/musica/menu.wav
 * efeito "pegada"  -> /audio/efeitos/pegada.wav
 * </pre>
 * Arquivo ausente = silencio (avisado uma unica vez no console).
 */
public final class ServicoAudio {

    public static final String PASTA_MUSICAS = "/audio/musica/";
    public static final String PASTA_EFEITOS = "/audio/efeitos/";

    /** Propriedade de sistema para desligar o audio: -Dirmaosmiau.audio=desligado */
    public static final String PROPRIEDADE_DESLIGAR = "irmaosmiau.audio";

    private final SaidaAudio saida;
    private final Function<String, Optional<SomCarregado>> carregador;

    private final Map<String, Optional<SomCarregado>> cache = new HashMap<>();
    private final Set<String> ausentesAvisados = new HashSet<>();
    private final List<SaidaAudio.Reproducao> efeitosTocando = new ArrayList<>();

    private boolean musicaAtiva = true;
    private boolean efeitosAtivos = true;
    private double volumeMusica = 0.6;
    private double volumeEfeitos = 0.8;

    /* Musica pedida pelo jogo (mesmo com a musica desligada). */
    private String musicaDesejada;
    private SaidaAudio.Reproducao reproducaoMusica = SaidaAudio.Reproducao.NENHUMA;
    private boolean fechado;

    public ServicoAudio(SaidaAudio saida, Function<String, Optional<SomCarregado>> carregador) {
        if (saida == null || carregador == null) {
            throw new IllegalArgumentException("Saida e carregador sao obrigatorios.");
        }
        this.saida = saida;
        this.carregador = carregador;
    }

    /**
     * Servico real do jogo: Java Sound, exceto em ambiente headless (testes)
     * ou com -Dirmaosmiau.audio=desligado, quando fica silencioso.
     */
    public static ServicoAudio criarPadrao() {
        boolean desligado = "desligado".equalsIgnoreCase(System.getProperty(PROPRIEDADE_DESLIGAR, ""));
        SaidaAudio saida = desligado || GraphicsEnvironment.isHeadless()
                ? SaidaAudio.SILENCIOSA
                : new SaidaJavaSound();
        return new ServicoAudio(saida, CarregadorSom::carregar);
    }

    // =====================================================
    // MUSICA
    // =====================================================
    /** Toca a musica em loop; pedir a mesma musica de novo nao a reinicia. */
    public synchronized void tocarMusica(String id) {
        if (fechado || id == null) {
            return;
        }
        if (id.equals(musicaDesejada) && (reproducaoMusica.isAtiva() || !musicaAtiva)) {
            return;
        }
        pararReproducaoMusica();
        musicaDesejada = id;
        iniciarMusicaDesejada();
    }

    public synchronized void pararMusica() {
        musicaDesejada = null;
        pararReproducaoMusica();
    }

    private void iniciarMusicaDesejada() {
        if (!musicaAtiva || musicaDesejada == null) {
            return;
        }
        som(PASTA_MUSICAS + musicaDesejada + ".wav").ifPresent(s ->
                reproducaoMusica = saida.tocar(s, true, volumeMusica));
    }

    private void pararReproducaoMusica() {
        reproducaoMusica.parar();
        reproducaoMusica = SaidaAudio.Reproducao.NENHUMA;
    }

    // =====================================================
    // EFEITOS
    // =====================================================
    /** Toca um efeito uma vez, independente da musica e de outros efeitos. */
    public synchronized void tocarEfeito(String id) {
        if (fechado || id == null || !efeitosAtivos) {
            return;
        }
        efeitosTocando.removeIf(r -> !r.isAtiva());
        som(PASTA_EFEITOS + id + ".wav").ifPresent(s -> {
            SaidaAudio.Reproducao r = saida.tocar(s, false, volumeEfeitos);
            if (r.isAtiva()) {
                efeitosTocando.add(r);
            }
        });
    }

    // =====================================================
    // CONFIGURACAO
    // =====================================================
    public synchronized void setMusicaAtiva(boolean ativa) {
        if (musicaAtiva == ativa) {
            return;
        }
        musicaAtiva = ativa;
        if (ativa) {
            iniciarMusicaDesejada();
        } else {
            pararReproducaoMusica();
        }
    }

    public synchronized void setEfeitosAtivos(boolean ativos) {
        efeitosAtivos = ativos;
        if (!ativos) {
            efeitosTocando.forEach(SaidaAudio.Reproducao::parar);
            efeitosTocando.clear();
        }
    }

    /** 0.0 a 1.0 (valores fora sao limitados). */
    public synchronized void setVolumeMusica(double volume) {
        volumeMusica = limitar(volume);
        reproducaoMusica.setVolume(volumeMusica);
    }

    public synchronized void setVolumeEfeitos(double volume) {
        volumeEfeitos = limitar(volume);
    }

    /** Para tudo e libera as linhas de audio. Depois disso nada mais toca. */
    public synchronized void fechar() {
        pararReproducaoMusica();
        efeitosTocando.forEach(SaidaAudio.Reproducao::parar);
        efeitosTocando.clear();
        fechado = true;
    }

    // =====================================================
    // CONSULTAS
    // =====================================================
    public synchronized boolean isMusicaAtiva() {
        return musicaAtiva;
    }

    public synchronized boolean isEfeitosAtivos() {
        return efeitosAtivos;
    }

    public synchronized double getVolumeMusica() {
        return volumeMusica;
    }

    public synchronized double getVolumeEfeitos() {
        return volumeEfeitos;
    }

    /** Musica pedida pelo jogo (pode estar em silencio por estar desligada ou ausente). */
    public synchronized String getMusicaDesejada() {
        return musicaDesejada;
    }

    public synchronized boolean isTocandoMusica() {
        return reproducaoMusica.isAtiva();
    }

    public boolean isSilencioso() {
        return saida.isSilenciosa();
    }

    /** true se o arquivo do id existe e e um WAV legivel. */
    public synchronized boolean existeMusica(String id) {
        return som(PASTA_MUSICAS + id + ".wav").isPresent();
    }

    public synchronized boolean existeEfeito(String id) {
        return som(PASTA_EFEITOS + id + ".wav").isPresent();
    }

    // =====================================================
    // AUXILIARES
    // =====================================================
    private Optional<SomCarregado> som(String caminho) {
        Optional<SomCarregado> s = cache.computeIfAbsent(caminho, carregador);
        if (s.isEmpty() && ausentesAvisados.add(caminho)) {
            System.err.println("[audio] " + caminho + " ausente ou invalido; seguindo em silencio.");
        }
        return s;
    }

    private static double limitar(double v) {
        return Double.isNaN(v) ? 0.0 : Math.max(0.0, Math.min(1.0, v));
    }

    /** Converte volume linear (0..1) em ganho (dB) para o mixer. */
    public static double volumeParaDecibeis(double volume) {
        double v = Math.max(0.0001, limitar(volume));
        return 20.0 * Math.log10(v);
    }
}
