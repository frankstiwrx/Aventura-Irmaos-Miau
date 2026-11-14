package com.irmaosmiau.animacao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dados visuais de uma animacao individual em formato de pose MiauStudio
 * (30 inteiros por quadro, rig "miau_v1"), Entrega 5E. Imutavel.
 *
 * So descreve POSES: nunca move o lutador (o deslocamentoVisual e apenas
 * onde a pose e desenhada em relacao ao corpo, como o "70" do chao legado).
 */
public final class AnimacaoIndividual {

    /** Tamanho da pose MiauStudio (IrmaoMiau.TAMANHO_POSE_MIAUSTUDIO). */
    public static final int TAMANHO_POSE = 30;

    /** Como a pose acompanha o lado para onde o lutador olha. */
    public enum Espelhamento {
        /** Desenhada sempre para a direita; o GamePanel espelha quem olha para a esquerda (como hoje). */
        LADO_DO_LUTADOR,
        /** Nunca espelhar (ex.: animacoes de frente). */
        NUNCA
    }

    private final String id;
    private final EstadoVisual estado;
    private final String rig;
    private final List<int[]> quadros;
    private final int[] sequencia;
    private final int duracaoQuadroMs;
    private final boolean loop;
    private final double deslocamentoVisualX;
    private final double deslocamentoVisualY;
    private final Espelhamento espelhamento;
    private final String origem;

    public AnimacaoIndividual(String id, EstadoVisual estado, String rig, List<int[]> quadros, int[] sequencia,
            int duracaoQuadroMs, boolean loop, double deslocamentoVisualX, double deslocamentoVisualY,
            Espelhamento espelhamento, String origem) {
        if (id == null || id.isBlank() || estado == null || rig == null || rig.isBlank()) {
            throw new IllegalArgumentException("Animacao sem id, estado ou rig.");
        }
        if (quadros == null || quadros.isEmpty()) {
            throw new IllegalArgumentException(id + ": precisa de pelo menos um quadro.");
        }
        List<int[]> copia = new ArrayList<>();
        for (int i = 0; i < quadros.size(); i++) {
            int[] q = quadros.get(i);
            if (q == null || q.length != TAMANHO_POSE) {
                throw new IllegalArgumentException(id + ": quadro " + (i + 1) + " precisa ter " + TAMANHO_POSE
                        + " valores (tem " + (q == null ? 0 : q.length) + ").");
            }
            copia.add(q.clone());
        }
        if (sequencia == null || sequencia.length == 0) {
            throw new IllegalArgumentException(id + ": sequencia vazia.");
        }
        for (int s : sequencia) {
            if (s < 0 || s >= copia.size()) {
                throw new IllegalArgumentException(id + ": sequencia aponta para quadro inexistente " + s + ".");
            }
        }
        if (duracaoQuadroMs < 1) {
            throw new IllegalArgumentException(id + ": duracaoQuadroMs precisa ser >= 1.");
        }
        this.id = id;
        this.estado = estado;
        this.rig = rig;
        this.quadros = List.copyOf(copia);
        this.sequencia = sequencia.clone();
        this.duracaoQuadroMs = duracaoQuadroMs;
        this.loop = loop;
        this.deslocamentoVisualX = deslocamentoVisualX;
        this.deslocamentoVisualY = deslocamentoVisualY;
        this.espelhamento = espelhamento == null ? Espelhamento.LADO_DO_LUTADOR : espelhamento;
        this.origem = origem;
    }

    public String getId() {
        return id;
    }

    public EstadoVisual getEstado() {
        return estado;
    }

    public String getRig() {
        return rig;
    }

    public int getQuantidadeQuadros() {
        return quadros.size();
    }

    /** Copia do quadro (0-based). */
    public int[] quadro(int indice) {
        return quadros.get(indice).clone();
    }

    public int[] getSequencia() {
        return sequencia.clone();
    }

    public int getDuracaoQuadroMs() {
        return duracaoQuadroMs;
    }

    public boolean isLoop() {
        return loop;
    }

    public double getDeslocamentoVisualX() {
        return deslocamentoVisualX;
    }

    public double getDeslocamentoVisualY() {
        return deslocamentoVisualY;
    }

    public Espelhamento getEspelhamento() {
        return espelhamento;
    }

    /** Onde os dados vieram (ex.: "IrmaoMiauBranco.MOVIMENTO_CHAO_MIAUSTUDIO"). */
    public String getOrigem() {
        return origem;
    }

    /**
     * Quadro mostrado depois de tempoMs desde o inicio. Mesma conta do
     * codigo legado: (tempo / duracao) % tamanho da sequencia.
     */
    public int indiceQuadroNoTempo(long tempoMs) {
        long passo = Math.max(0, tempoMs) / duracaoQuadroMs;
        if (!loop && passo >= sequencia.length) {
            return sequencia[sequencia.length - 1];
        }
        return sequencia[(int) (passo % sequencia.length)];
    }

    /** Idem, contando ticks do loop logico (msPorTick = 1000/60 no jogo). */
    public int indiceQuadroNoTick(long ticks, double msPorTick) {
        return indiceQuadroNoTempo((long) Math.floor(ticks * msPorTick));
    }

    /** Mesmos quadros (valor a valor)? */
    public boolean mesmosQuadros(int[][] outros) {
        if (outros.length != quadros.size()) {
            return false;
        }
        for (int i = 0; i < outros.length; i++) {
            if (!Arrays.equals(outros[i], quadros.get(i))) {
                return false;
            }
        }
        return true;
    }
}
