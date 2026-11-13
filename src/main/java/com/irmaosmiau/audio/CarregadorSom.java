package com.irmaosmiau.audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Le arquivos WAV do classpath (NetBeans e JAR) e devolve PCM.
 *
 * Arquivo ausente ou invalido -> Optional.empty(), nunca excecao: a falta
 * de audio nao pode impedir o jogo de rodar.
 */
public final class CarregadorSom {

    private CarregadorSom() {
    }

    public static Optional<SomCarregado> carregar(String caminho) {
        if (caminho == null || caminho.isBlank()) {
            return Optional.empty();
        }
        String normalizado = caminho.startsWith("/") ? caminho : "/" + caminho;
        InputStream bruto = CarregadorSom.class.getResourceAsStream(normalizado);
        if (bruto == null) {
            return Optional.empty();
        }
        try (InputStream in = new BufferedInputStream(bruto);
                AudioInputStream original = AudioSystem.getAudioInputStream(in);
                AudioInputStream pcm = paraPcm(original)) {
            byte[] dados = pcm.readAllBytes();
            if (dados.length == 0) {
                return Optional.empty();
            }
            return Optional.of(new SomCarregado(normalizado, pcm.getFormat(), dados));
        } catch (UnsupportedAudioFileException | IOException | IllegalArgumentException ex) {
            System.err.println("[audio] Nao foi possivel ler " + normalizado + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    /** Converte para PCM 16 bits com sinal, se o arquivo vier em outro formato. */
    private static AudioInputStream paraPcm(AudioInputStream original) {
        AudioFormat f = original.getFormat();
        if (f.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && f.getSampleSizeInBits() == 16) {
            return original;
        }
        AudioFormat alvo = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                f.getSampleRate(),
                16,
                f.getChannels(),
                f.getChannels() * 2,
                f.getSampleRate(),
                false);
        return AudioSystem.getAudioInputStream(alvo, original);
    }
}
