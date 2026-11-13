package com.irmaosmiau.audio;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

/**
 * Saida real com javax.sound.sampled.Clip.
 *
 * Cada reproducao abre o seu proprio Clip (efeitos podem se sobrepor).
 * Efeitos fecham o Clip sozinhos ao terminar; musicas em loop sao fechadas
 * em {@link Reproducao#parar()}.
 */
public final class SaidaJavaSound implements SaidaAudio {

    @Override
    public Reproducao tocar(SomCarregado som, boolean emLoop, double volume) {
        Clip clip;
        try {
            clip = AudioSystem.getClip();
            clip.open(som.getFormato(), som.dadosInternos(), 0, som.tamanhoBytes());
        } catch (LineUnavailableException | IllegalArgumentException | IllegalStateException
                | SecurityException ex) {
            System.err.println("[audio] Sem saida de som para " + som.getOrigem() + ": " + ex.getMessage());
            return Reproducao.NENHUMA;
        }

        ReproducaoClip r = new ReproducaoClip(clip);
        r.setVolume(volume);
        if (emLoop) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            clip.addLineListener(evento -> {
                if (evento.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        }
        return r;
    }

    @Override
    public boolean isSilenciosa() {
        return false;
    }

    private static final class ReproducaoClip implements Reproducao {

        private final Clip clip;

        ReproducaoClip(Clip clip) {
            this.clip = clip;
        }

        @Override
        public void parar() {
            clip.stop();
            clip.close();
        }

        @Override
        public void setVolume(double volume) {
            if (!clip.isOpen() || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                return;
            }
            FloatControl ganho = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float db = (float) ServicoAudio.volumeParaDecibeis(volume);
            ganho.setValue(Math.max(ganho.getMinimum(), Math.min(ganho.getMaximum(), db)));
        }

        @Override
        public boolean isAtiva() {
            return clip.isOpen() && clip.isActive();
        }
    }
}
