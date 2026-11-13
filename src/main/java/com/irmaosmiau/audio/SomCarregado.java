package com.irmaosmiau.audio;

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

/**
 * Som ja decodificado em PCM, pronto para ser tocado quantas vezes for
 * preciso (cada reproducao abre a sua propria linha de audio).
 */
public final class SomCarregado {

    private final String origem;
    private final AudioFormat formato;
    private final byte[] dados;

    public SomCarregado(String origem, AudioFormat formato, byte[] dados) {
        if (formato == null || dados == null) {
            throw new IllegalArgumentException("Formato e dados sao obrigatorios.");
        }
        this.origem = origem;
        this.formato = formato;
        this.dados = Arrays.copyOf(dados, dados.length);
    }

    public String getOrigem() {
        return origem;
    }

    public AudioFormat getFormato() {
        return formato;
    }

    /** Dados PCM (copia defensiva nao e feita aqui para evitar custo por reproducao). */
    byte[] dadosInternos() {
        return dados;
    }

    public int tamanhoBytes() {
        return dados.length;
    }

    /** Duracao em segundos. */
    public double duracaoSegundos() {
        float bytesPorSegundo = formato.getFrameRate() * formato.getFrameSize();
        return bytesPorSegundo <= 0 ? 0.0 : dados.length / bytesPorSegundo;
    }
}
