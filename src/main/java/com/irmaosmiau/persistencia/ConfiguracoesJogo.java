package com.irmaosmiau.persistencia;

import com.irmaosmiau.cenario.RegistroCenarios;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Preferencias do jogador (imutavel; use os metodos "com..." para variar).
 *
 * Os padroes espelham o que o jogo ja faz hoje (ServicoAudio: musica e
 * efeitos ligados, volumes 0,6 e 0,8; cenario padrao "dojo"), para que
 * ligar as configuracoes no futuro nao mude nada para quem nunca salvou.
 */
public final class ConfiguracoesJogo {

    public static final double VOLUME_MUSICA_PADRAO = 0.6;
    public static final double VOLUME_EFEITOS_PADRAO = 0.8;

    private static final Pattern ID_VALIDO = Pattern.compile("[a-z0-9_-]{1,40}");

    private final boolean musicaAtiva;
    private final boolean efeitosAtivos;
    private final double volumeMusica;
    private final double volumeEfeitos;
    private final String cenarioPreferido;

    /**
     * @throws IllegalArgumentException volumes fora de [0, 1] ou id de cenario invalido
     */
    public ConfiguracoesJogo(boolean musicaAtiva, boolean efeitosAtivos,
            double volumeMusica, double volumeEfeitos, String cenarioPreferido) {
        validarVolume("volumeMusica", volumeMusica);
        validarVolume("volumeEfeitos", volumeEfeitos);
        if (cenarioPreferido == null) {
            throw new IllegalArgumentException("O cenario preferido e obrigatorio.");
        }
        String cenario = cenarioPreferido.trim().toLowerCase(Locale.ROOT);
        if (!ID_VALIDO.matcher(cenario).matches()) {
            throw new IllegalArgumentException("Id de cenario invalido: \"" + cenarioPreferido
                    + "\" (use letras minusculas, numeros, _ ou -, ate 40 caracteres)");
        }
        this.musicaAtiva = musicaAtiva;
        this.efeitosAtivos = efeitosAtivos;
        this.volumeMusica = volumeMusica;
        this.volumeEfeitos = volumeEfeitos;
        this.cenarioPreferido = cenario;
    }

    public static ConfiguracoesJogo padrao() {
        return new ConfiguracoesJogo(true, true, VOLUME_MUSICA_PADRAO, VOLUME_EFEITOS_PADRAO,
                RegistroCenarios.ID_PADRAO);
    }

    private static void validarVolume(String nome, double v) {
        if (Double.isNaN(v) || v < 0.0 || v > 1.0) {
            throw new IllegalArgumentException(nome + " deve ficar entre 0.0 e 1.0 (recebido " + v + ")");
        }
    }

    public ConfiguracoesJogo comMusicaAtiva(boolean v) {
        return new ConfiguracoesJogo(v, efeitosAtivos, volumeMusica, volumeEfeitos, cenarioPreferido);
    }

    public ConfiguracoesJogo comEfeitosAtivos(boolean v) {
        return new ConfiguracoesJogo(musicaAtiva, v, volumeMusica, volumeEfeitos, cenarioPreferido);
    }

    public ConfiguracoesJogo comVolumeMusica(double v) {
        return new ConfiguracoesJogo(musicaAtiva, efeitosAtivos, v, volumeEfeitos, cenarioPreferido);
    }

    public ConfiguracoesJogo comVolumeEfeitos(double v) {
        return new ConfiguracoesJogo(musicaAtiva, efeitosAtivos, volumeMusica, v, cenarioPreferido);
    }

    public ConfiguracoesJogo comCenarioPreferido(String id) {
        return new ConfiguracoesJogo(musicaAtiva, efeitosAtivos, volumeMusica, volumeEfeitos, id);
    }

    public boolean isMusicaAtiva() {
        return musicaAtiva;
    }

    public boolean isEfeitosAtivos() {
        return efeitosAtivos;
    }

    public double getVolumeMusica() {
        return volumeMusica;
    }

    public double getVolumeEfeitos() {
        return volumeEfeitos;
    }

    /**
     * Id do cenario preferido. A existencia no RegistroCenarios e conferida
     * por quem aplicar a configuracao (um cenario pode ser removido depois).
     */
    public String getCenarioPreferido() {
        return cenarioPreferido;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfiguracoesJogo)) {
            return false;
        }
        ConfiguracoesJogo c = (ConfiguracoesJogo) o;
        return musicaAtiva == c.musicaAtiva && efeitosAtivos == c.efeitosAtivos
                && Double.compare(volumeMusica, c.volumeMusica) == 0
                && Double.compare(volumeEfeitos, c.volumeEfeitos) == 0
                && cenarioPreferido.equals(c.cenarioPreferido);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(musicaAtiva, efeitosAtivos, volumeMusica, volumeEfeitos, cenarioPreferido);
    }

    @Override
    public String toString() {
        return "ConfiguracoesJogo[musica=" + musicaAtiva + ", efeitos=" + efeitosAtivos
                + ", volumeMusica=" + volumeMusica + ", volumeEfeitos=" + volumeEfeitos
                + ", cenario=" + cenarioPreferido + "]";
    }
}
