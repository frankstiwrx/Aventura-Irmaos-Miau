package com.irmaosmiau.animacao;

import com.irmaosmiau.combate.EventoDefesa;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Entrega 5L: reacoes PROCEDURAIS e curtas aos eventos defensivos que o motor
 * confirmou (nunca ao botao apertado), FORA da pegada:
 *
 * - DEFESA_EVASAO (quem evitou abaixando): abaixa e inclina para tras;
 * - DEFESA_ALCANCE_FALHO (quem tentou a pegada evitada): estica os bracos e recolhe;
 * - DEFESA_ROMPIMENTO_SUCESSO (quem rompeu): recua o tronco e recolhe os bracos;
 * - DEFESA_SOLTOU (quem perdeu a pegada): os bracos caem para a frente e voltam.
 *
 * Dentro da pegada (forcar e ROMPER resistido) quem desenha e a
 * CamadaPoseCombate. So desenho: nada se move, nenhuma regra muda. Usa o canal
 * de pose externa so quando esta livre (nunca sobre o combate).
 */
public final class ReacoesDefesa {

    /** Duracao de cada reacao (ticks). */
    public static final int DURACAO_TICKS = 14;

    /** Contratos estaveis (substituiveis por JSON do MiauStudio). */
    public enum Tipo {
        DEFESA_EVASAO, DEFESA_ALCANCE_FALHO, DEFESA_ROMPIMENTO_SUCESSO, DEFESA_SOLTOU
    }

    private record Ativa(Tipo tipo, long inicio) {
    }

    private final TransicoesPostura posturas;
    private final Map<IrmaoMiau, Ativa> ativas = new IdentityHashMap<>();
    private final Map<IrmaoMiau, int[]> aplicadas = new IdentityHashMap<>();
    private long ultimaSequencia;

    public ReacoesDefesa(TransicoesPostura posturas) {
        this.posturas = posturas;
    }

    /** Le o ultimo evento do motor (um tick logico). */
    public void observar(SistemaCombate s, long tick) {
        EventoDefesa e = s == null ? null : s.getUltimoEventoDefesa();
        if (e == null || e.sequencia() <= ultimaSequencia) {
            return;
        }
        ultimaSequencia = e.sequencia();
        switch (e.tipo()) {
            case PEGADA_EVITADA -> {
                if ("AGACHAMENTO".equals(e.detalhe())) {
                    ativas.put(e.defensor(), new Ativa(Tipo.DEFESA_EVASAO, tick));
                }
                ativas.put(e.oponente(), new Ativa(Tipo.DEFESA_ALCANCE_FALHO, tick));
            }
            case ROMPIMENTO_SUCESSO -> {
                ativas.put(e.defensor(), new Ativa(Tipo.DEFESA_ROMPIMENTO_SUCESSO, tick));
                ativas.put(e.oponente(), new Ativa(Tipo.DEFESA_SOLTOU, tick));
            }
            default -> {
                // ROMPIMENTO_FALHOU: a pegada continua; a CamadaPoseCombate mostra.
            }
        }
    }

    /** Reacao do lutador agora (null = nenhuma). */
    public Tipo ativa(IrmaoMiau l, long tick) {
        Ativa a = ativas.get(l);
        if (a == null) {
            return null;
        }
        if (tick - a.inicio() >= DURACAO_TICKS) {
            ativas.remove(l);
            return null;
        }
        return a.tipo();
    }

    /** 0..1..0: sobe em 4 ticks, segura ate 8, volta ate 14. */
    static double envelope(long t) {
        if (t < 0 || t >= DURACAO_TICKS) {
            return 0;
        }
        if (t < 4) {
            return (t + 1) / 4.0;
        }
        if (t < 8) {
            return 1;
        }
        return (DURACAO_TICKS - t) / (double) (DURACAO_TICKS - 8);
    }

    /** Pose da reacao sobre a pose da postura atual. */
    public TransicoesPostura.Pose pose(IrmaoMiau l, Tipo tipo, long t) {
        TransicoesPostura.Pose base = posturas.pose(l, l.getEstadoCorporal());
        int[] p = Arrays.copyOf(base.valores(), base.valores().length);
        double e = envelope(t);
        double ida = Math.sin(Math.PI * Math.min(1.0, t / (double) DURACAO_TICKS));
        double dy = base.dy();
        switch (tipo) {
            case DEFESA_EVASAO -> {
                dy += 10 * e;
                p[9] -= (int) Math.round(8 * e);
                p[11] -= (int) Math.round(4 * e);
                p[20] -= (int) Math.round(6 * e);
            }
            case DEFESA_ALCANCE_FALHO -> {
                p[20] += (int) Math.round(18 * ida);
                p[16] += (int) Math.round(30 * ida);
                p[9] += (int) Math.round(4 * ida);
            }
            case DEFESA_ROMPIMENTO_SUCESSO -> {
                p[9] -= (int) Math.round(10 * e);
                p[11] -= (int) Math.round(5 * e);
                p[20] -= (int) Math.round(8 * e);
                p[16] -= (int) Math.round(6 * e);
            }
            default -> {
                double cai = 1.0 - Math.min(1.0, t / (double) DURACAO_TICKS);
                p[20] += (int) Math.round(12 * cai);
                p[21] += (int) Math.round(6 * cai);
                p[9] += (int) Math.round(3 * cai);
            }
        }
        return new TransicoesPostura.Pose(p, dy);
    }

    /**
     * Desenha a reacao se houver uma e a pose externa estiver livre (ou for nossa).
     *
     * @return o tipo desenhado, ou null
     */
    public Tipo aplicar(IrmaoMiau l, long tick) {
        Tipo tipo = ativa(l, tick);
        if (tipo == null) {
            liberar(l);
            return null;
        }
        int[] nossa = aplicadas.get(l);
        if (l.isPoseCombateExternaAtiva() && (nossa == null || !Arrays.equals(nossa, l.copiarPoseCombateExterna()))) {
            return null;                                   // combate ou transicao: nao interferir
        }
        TransicoesPostura.Pose q = pose(l, tipo, tick - ativas.get(l).inicio());
        l.aplicarPoseCombateExterna(q.valores(), 0.0, q.dy());
        aplicadas.put(l, q.valores());
        return tipo;
    }

    public void liberar(IrmaoMiau l) {
        int[] nossa = aplicadas.remove(l);
        if (nossa != null && l.isPoseCombateExternaAtiva() && Arrays.equals(nossa, l.copiarPoseCombateExterna())) {
            l.limparPoseCombateExterna();
        }
    }

    /** Pausa/F5: esquece tudo. */
    public void esquecer(IrmaoMiau l) {
        ativas.remove(l);
        liberar(l);
    }
}
