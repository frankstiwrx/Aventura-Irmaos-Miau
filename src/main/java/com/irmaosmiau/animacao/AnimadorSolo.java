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
    /** 5Q.5: pose da pegada guardada quando uma tecnica propria assume em pe (rasteira, derrubada). */
    private final Map<IrmaoMiau, int[]> daPegada = new IdentityHashMap<>();

    public AnimadorSolo(TransicoesPostura posturas) {
        this.posturas = posturas;
    }

    public boolean quer(IrmaoMiau l, MotorSolo m) {
        // 5Q: tambem fora do vinculo enquanto houver um efeito do motor (empurrar, ser empurrado, levantar...)
        return m != null && l.getTransicaoAtiva() == null && !m.isAterrissando()
                && (m.conectado(l) || m.efeitoVisual(m.ladoDe(l)).isPresent());
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
        int[] partida = base.valores();
        // 5Q.5: tecnica com animacao propria em pe NA PEGADA (rasteira, derrubada...): a pose e da pegada;
        // a tecnica assume a partir dela (guardada uma vez: sem acumular) e devolve quando termina
        java.util.Optional<MotorSolo.EfeitoVisual> ef0 = m.efeitoVisual(lado);
        boolean propria = ef0.map(AnimacoesTecnicas::chave).orElse(null) != null;
        int[] nossa0 = aplicadas.get(l);
        boolean deOutro = l.isPoseCombateExternaAtiva()
                && (nossa0 == null || !Arrays.equals(nossa0, l.copiarPoseCombateExterna()));
        if (propria && !m.conectado(l) && (deOutro || daPegada.containsKey(l))) {
            int[] pegada = daPegada.computeIfAbsent(l, k -> k.copiarPoseCombateExterna());
            if (pegada != null && pegada.length == partida.length) {
                partida = pegada;
            }
        } else if (!propria) {
            daPegada.remove(l);
        }
        int[] pose = Arrays.copyOf(partida, partida.length);
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
        // 5Q.4: quem esta por baixo, deitado, ganha as pernas proprias de cada guarda
        if (!cima && l.isDeitado() && m.conectado(l) && PernasGuarda.temPernasProprias(p)) {
            PernasGuarda.aplicar(pose, l, p);
        }
        java.util.Optional<MotorSolo.EfeitoVisual> ef = m.efeitoVisual(lado);
        // 5Q.6: a manga presa fica visivel: a mao da frente continua segurando enquanto a conexao durar
        if (!cima && l.isDeitado() && m.conectado(l) && ef.isEmpty() && m.temConexao(lado, MotorSolo.Conexao.MANGA)) {
            pose[20] += MAO_NA_MANGA[0];
            pose[21] += MAO_NA_MANGA[1];
            pose[18] += MAO_NA_MANGA[0] / 2;
            pose[19] += MAO_NA_MANGA[1] / 2;
        }
        // 5Q.3: animacao propria da tecnica (aplicador/receptor); sem ela, a pose provisoria da 5Q
        double[] desloc = ef.map(e -> AnimacoesTecnicas.aplicar(pose, e)).orElse(null);
        if (desloc == null) {
            ef.ifPresent(e -> aplicarEfeito(pose, e));
        }
        double dx = desloc == null ? 0.0 : desloc[0];
        dy += desloc == null ? 0.0 : desloc[1];
        int[] nossa = aplicadas.get(l);
        if (!daPegada.containsKey(l) && l.isPoseCombateExternaAtiva()
                && (nossa == null || !Arrays.equals(nossa, l.copiarPoseCombateExterna()))) {
            return null;                                        // outro dono da pose
        }
        l.aplicarPoseCombateExterna(pose, dx, dy);
        aplicadas.put(l, pose);
        String id = "solo:" + p.name() + ":" + (cima ? "CIMA" : "BAIXO")
                + ef.map(e -> ":" + e.tipo().name() + (e.categoria() == null ? "" : "_" + e.categoria().name())).orElse("");
        descricao.put(l, id);
        return id;
    }

    /**
     * 5Q: poses PROVISORIAS (procedurais) por cima da pose da posicao. So refletem
     * o que o motor ja decidiu (nao movem o lutador nem decidem quedas).
     * Indices do rig (corrigido na 5Q.3; o comentario da 5Q dizia "ombro/cotovelo"):
     * 9 = rotacao do tronco, 16/17 = mao de tras (x/y em px), 20/21 = mao da frente (x/y),
     * 17/21 tambem recebem os valores "cotovelo" daqui (deslocam a mao em y). So vale para
     * os efeitos SEM animacao propria (AnimacoesTecnicas tem prioridade).
     */
    public static void aplicarEfeito(int[] pose, MotorSolo.EfeitoVisual e) {
        double p = Math.max(0, Math.min(1, e.progresso()));
        double pico = Math.sin(Math.PI * p);                    // sobe e volta (acoes curtas)
        double firme = 0.4 + 0.6 * p;                          // cresce com a pressao (finalizacoes)
        switch (e.tipo()) {
            case EMPURRANDO -> mexer(pose, pico, 24, 50, -30, 50, -30);
            case EMPURRADO -> mexer(pose, pico, -26, -40, 10, -40, 10);
            case AFASTANDO -> mexer(pose, pico, -16, 40, -20, 40, -20);
            case SOLTANDO -> mexer(pose, pico, -10, -25, 0, 25, 0);
            case LEVANTANDO -> mexer(pose, pico, 18, -20, 10, -10, 10);
            case FINALIZANDO -> {
                switch (e.categoria()) {
                    case BRACO -> mexer(pose, firme, -20, 40, 40, 40, 0);
                    case ESTRANGULAMENTO -> mexer(pose, firme, 14, 70, 90, 70, 90);
                    default -> mexer(pose, firme, -30, 30, 0, 30, 0);
                }
            }
            case DEFENDENDO_FINALIZACAO -> {
                switch (e.categoria()) {
                    case BRACO -> mexer(pose, firme, 0, -30, 60, 0, 30);
                    case ESTRANGULAMENTO -> mexer(pose, firme, 0, 80, 110, 80, 110);
                    default -> mexer(pose, firme, 25, 40, 0, 0, 0);
                }
            }
            case BATENDO -> moverMao(pose, 16, 14, 30 + 10 * Math.sin(p * 6 * Math.PI), 20);   // bate no tatame
        }
    }

    /**
     * 5Q.4: alcance maximo do deslocamento da mao numa pose provisoria (px antes da escala). Antes
     * so a MAO andava (ate ~140 px no estrangulamento) e o cotovelo ficava parado: o antebraco
     * esticava ate virar uma linha. Agora a mao respeita esse raio e o cotovelo acompanha metade,
     * dividindo a variacao entre braco e antebraco (os dois segmentos mudam no maximo ~LIMITE/2).
     */
    public static final int LIMITE_MAO = 34;

    /** 5Q.6: mao da frente (deitado: +x sobe, +y vai para o outro) segurando a manga presa. */
    public static final int[] MAO_NA_MANGA = {10, 12};

    // os parametros sao: rotacao do tronco e deslocamento (x, y) da mao da frente e da mao de tras
    private static void mexer(int[] pose, double w, int tronco, int maoFx, int maoFy, int maoTx, int maoTy) {
        pose[9] += (int) Math.round(tronco * w);
        moverMao(pose, 20, 18, maoFx * w, maoFy * w);
        moverMao(pose, 16, 14, maoTx * w, maoTy * w);
    }

    /** Mao (indice x; y = x + 1) com o cotovelo junto, dentro do LIMITE_MAO, preservando a direcao. */
    private static void moverMao(int[] pose, int mao, int cotovelo, double dx, double dy) {
        double r = Math.hypot(dx, dy);
        if (r > LIMITE_MAO) {
            dx = dx * LIMITE_MAO / r;
            dy = dy * LIMITE_MAO / r;
        }
        pose[mao] += (int) Math.round(dx);
        pose[mao + 1] += (int) Math.round(dy);
        pose[cotovelo] += (int) Math.round(dx / 2);
        pose[cotovelo + 1] += (int) Math.round(dy / 2);
    }

    public void liberar(IrmaoMiau l) {
        liberar(l, false);
    }

    /**
     * 5Q.6: solta o lutador. Se a tecnica tinha assumido a pose da PEGADA (rasteira, derrubada...)
     * e a pegada continua (a tecnica falhou), devolve exatamente a pose guardada da pegada: antes a
     * pose era apagada (ou ficava a ultima da tecnica, que a camada da pegada passava a usar como
     * base) e o receptor ficava torto/congelado; a tentativa seguinte partia dessa pose e acumulava.
     */
    public void liberar(IrmaoMiau l, boolean pegadaContinua) {
        int[] pegada = daPegada.remove(l);
        int[] nossa = aplicadas.remove(l);
        descricao.remove(l);
        if (pegada != null && pegadaContinua && nossa != null) {
            l.aplicarPoseCombateExterna(Arrays.copyOf(pegada, pegada.length), 0.0, 0.0);
            return;
        }
        if (nossa != null && l.isPoseCombateExternaAtiva() && (pegada != null
                || Arrays.equals(nossa, l.copiarPoseCombateExterna()))) {
            l.limparPoseCombateExterna();
        }
    }

    /** 5Q.6: pose da pegada guardada enquanto uma tecnica propria assume (null = nenhuma). */
    public int[] poseDaPegadaGuardada(IrmaoMiau l) {
        int[] p = daPegada.get(l);
        return p == null ? null : Arrays.copyOf(p, p.length);
    }

    public String descricao(IrmaoMiau l) {
        return descricao.get(l);
    }
}
