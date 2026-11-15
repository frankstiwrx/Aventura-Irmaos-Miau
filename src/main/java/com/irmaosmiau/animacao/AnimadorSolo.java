package com.irmaosmiau.animacao;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Entrega 5P: desenho PROCEDURAL das posicoes de solo, comandado pela posicao
 * relacional do MotorSolo (nunca pelo que cada lutador "acha"): a postura de
 * cada um (de joelhos / deitado, ja animada pela transicao de postura), a altura
 * de quem esta por cima e pequenos ajustes de bracos e pernas por posicao. O
 * encaixe horizontal (a distancia entre os dois) e do motor.
 *
 * Usa o canal de pose externa como as outras camadas, so quando o lutador esta
 * ligado no solo e nenhuma transicao de postura esta desenhando. Fica pronto
 * para ser trocado por poses do MiauStudio (JSON) por posicao no futuro.
 */
public final class AnimadorSolo {

    private final TransicoesPostura posturas;
    private final Map<IrmaoMiau, int[]> aplicadas = new IdentityHashMap<>();
    private final Map<IrmaoMiau, String> descricao = new IdentityHashMap<>();

    public AnimadorSolo(TransicoesPostura posturas) {
        this.posturas = posturas;
    }

    public boolean quer(IrmaoMiau l, MotorSolo m) {
        return m != null && m.conectado(l) && l.getTransicaoAtiva() == null && !m.isAterrissando();
    }

    /** Altura extra (px; negativo = mais alto) de quem esta por cima, por posicao. */
    public static int alturaPorCima(Posicao p) {
        return switch (p) {
            case GUARDA -> 4;
            case MEIA_GUARDA -> 8;
            case CONTROLE_LATERAL -> 20;
            case JOELHO_BARRIGA -> -8;
            case MONTADA -> -4;
            case COSTAS -> -2;
            default -> 0;
        };
    }

    public String aplicar(IrmaoMiau l, MotorSolo m) {
        Lado lado = m.ladoDe(l);
        Posicao p = m.getPosicao();
        boolean cima = m.getEstado().getPorCima().map(c -> c == lado).orElse(false);
        TransicoesPostura.Pose base = posturas.pose(l, l.getEstadoCorporal());
        int[] pose = Arrays.copyOf(base.valores(), base.valores().length);
        double dy = base.dy() + (cima ? alturaPorCima(p) : 0);
        if (cima && l.isAjoelhado()) {
            switch (p) {
                case GUARDA, GUARDA_ABERTA, MEIA_GUARDA, GUARDA_BORBOLETA -> {
                    pose[9] += 18;                              // tronco inclinado para dentro da guarda
                    pose[20] += 10;                             // mao da frente no quadril do outro
                    pose[21] += 14;
                }
                case CONTROLE_LATERAL -> {
                    pose[9] += 40;                              // peito sobre o peito do outro
                    pose[20] += 6;
                    pose[21] += 26;
                    pose[16] += 10;
                    pose[17] += 22;
                }
                case MONTADA, JOELHO_BARRIGA -> {
                    pose[9] += 6;
                    pose[21] += 8;
                }
                case COSTAS -> {
                    pose[9] += 14;
                    pose[20] += 16;                             // bracos envolvendo
                    pose[16] += 16;
                }
                default -> {
                }
            }
        }
        int[] nossa = aplicadas.get(l);
        if (l.isPoseCombateExternaAtiva() && (nossa == null || !Arrays.equals(nossa, l.copiarPoseCombateExterna()))) {
            return null;                                        // outro dono da pose
        }
        l.aplicarPoseCombateExterna(pose, 0.0, dy);
        aplicadas.put(l, pose);
        String id = "solo:" + p.name() + ":" + (cima ? "CIMA" : "BAIXO");
        descricao.put(l, id);
        return id;
    }

    public void liberar(IrmaoMiau l) {
        int[] nossa = aplicadas.remove(l);
        descricao.remove(l);
        if (nossa != null && l.isPoseCombateExternaAtiva() && Arrays.equals(nossa, l.copiarPoseCombateExterna())) {
            l.limparPoseCombateExterna();
        }
    }

    public String descricao(IrmaoMiau l) {
        return descricao.get(l);
    }
}
