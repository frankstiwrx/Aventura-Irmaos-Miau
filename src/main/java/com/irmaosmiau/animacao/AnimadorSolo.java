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

    /** 5Q.7: de onde vem a pose LIMPA da pegada (a base da camada, sem reacoes passageiras). */
    private java.util.function.Function<IrmaoMiau, int[]> fontePegada = l -> null;

    public void usarPoseDaPegada(java.util.function.Function<IrmaoMiau, int[]> fonte) {
        this.fontePegada = fonte == null ? l -> null : fonte;
    }

    /**
     * 5Q.7 (corretiva): antes era a pose ATUAL (a base + as reacoes da camada naquele tick); ela era
     * guardada no inicio da tecnica e devolvida no fim, entao uma reacao passageira (inclinacao do
     * desequilibrio, passo do arrasto) virava parte da "pegada" e se acumulava nas tentativas seguintes.
     */
    private int[] poseLimpaDaPegada(IrmaoMiau l) {
        int[] limpa = fontePegada.apply(l);
        int[] atual = l.copiarPoseCombateExterna();
        return limpa != null && atual != null && limpa.length == atual.length ? limpa : atual;
    }

    /** 5Q.7: a tecnica do solo assumiu a pose a partir da pegada (a camada da pegada fica parada). */
    public boolean assumiuPegada(IrmaoMiau l) {
        return daPegada.containsKey(l);
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
            int[] pegada = daPegada.computeIfAbsent(l, this::poseLimpaDaPegada);
            if (pegada != null && pegada.length == partida.length) {
                partida = pegada;
            }
        } else if (!propria) {
            daPegada.remove(l);
        }
        int[] pose = Arrays.copyOf(partida, partida.length);
        // 5Q.7 corretiva: no solo livre quem esta deitado e desenhado pelo desenho legado (corpo reto). Ao
        // ser segurado pelas pernas ele passava para a pose procedural de deitado (tronco 15 graus fora das
        // pernas, perna da frente dobrada): a troca parecia a cintura dobrando. Agora o segurado usa o
        // deitado RETO (so a rotacao global), o mesmo desenho de antes de segurar.
        boolean seguradoPelasPernas = ef0.map(e -> "segurando_pernas".equals(e.tecnica())
                || "segurar_pernas".equals(e.tecnica())
                || "segurando_pernas:RECEBE".equals(e.tecnica())).orElse(false);
        if (!cima && !m.conectado(l) && l.isDeitado() && seguradoPelasPernas) {
            for (int i = 4; i < pose.length; i++) {
                if (i != 10) {
                    pose[i] = 0;
                }
            }
            bracosAoLongoDoCorpo(pose, l.getBracosEmRepouso());
        }
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
        int[] antesDaTecnica = Arrays.copyOf(pose, pose.length);
        double[] desloc = ef.map(e -> AnimacoesTecnicas.aplicar(pose, e)).orElse(null);
        if (desloc == null) {
            ef.ifPresent(e -> aplicarEfeito(pose, e));
        }
        // 5Q.7: quem arrasta pelas pernas da passos curtos (pelo x real: parado, os pes ficam)
        // 5Q.7 corretiva: e mostra o ESFORCO de puxar enquanto anda (inclina para tras, bracos puxam);
        // de joelhos, os passos sao curtos e so dos joelhos; quem e arrastado ajusta o quadril de leve
        // 5Q.8: aplicado ANTES da integridade (os passos nao mudam o tamanho das pernas)
        boolean segurando = ef.map(e -> "segurando_pernas".equals(e.tecnica())).orElse(false);
        int andou = segurando ? l.getX() - ultimoXArrasto.getOrDefault(l, l.getX()) : 0;
        if (segurando) {
            ultimoXArrasto.put(l, l.getX());
            double esforco = esforcoArrasto.getOrDefault(l, 0.0);
            // 5Q.8: o arrasto anda 1 px a cada poucos ticks: "andando" = andou nos ultimos ticks (o esforco
            // nao pisca nem fica baixo); parado de verdade, ele volta suave
            int semAndar = andou != 0 ? 0 : ticksSemAndarArrasto.getOrDefault(l, 99) + 1;
            ticksSemAndarArrasto.put(l, semAndar);
            boolean andando = semAndar <= TICKS_ANDANDO_ARRASTO;
            esforco = andando ? Math.min(1.0, esforco + 0.2) : Math.max(0.0, esforco - 0.08);
            esforcoArrasto.put(l, esforco);
            // 5Q.8: a fase dos passos anda pelo TEMPO enquanto ha deslocamento (de joelhos o arrasto e tao lento
            // que, pelo x, o ciclo quase nao andava e parecia uma pose parada deslizando)
            double fase = faseArrasto.getOrDefault(l, 0.0);
            if (andando) {
                fase += l.isAjoelhado() ? 1.0 / TICKS_PASSO_ARRASTO_JOELHOS : 1.0 / TICKS_PASSO_ARRASTO;
            }
            faseArrasto.put(l, fase % 1.0);
            int xFase = (int) Math.round(fase % 1.0 * CICLO_PASSO_ARRASTO_PX);
            if (ef.get().tipo() == MotorSolo.TipoEfeitoVisual.TECNICA_ATAQUE) {
                passosDoArrasto(pose, xFase, l.isAjoelhado());
                esforcoDoArrasto(pose, esforco);
            } else {
                quadrilArrastado(pose, xFase, esforco);
            }
        } else {
            ultimoXArrasto.remove(l);
            esforcoArrasto.remove(l);
            faseArrasto.remove(l);
            ticksSemAndarArrasto.remove(l);
        }
        // 5Q.7: a tecnica partiu da pose da PEGADA (bracos ja estendidos ate o kimono): os bracos giram
        // e dobram sem mudar de tamanho (antes o antebraco esticava ate ~1,6x na rasteira/derrubada)
        if (daPegada.containsKey(l) || ef.map(AnimacoesTecnicas::preservaBracos).orElse(false)) {
            IntegridadeMembros.preservarCorpo(antesDaTecnica, pose, l, daPegada.containsKey(l));   // 5Q.8
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

    /** 5Q.7: comprimento (px de x real) de um ciclo de passos de quem arrasta. */
    public static final int CICLO_PASSO_ARRASTO_PX = 24;

    /**
     * 5Q.7 corretiva: deitado reto, os bracos ficam ao longo do corpo (na direcao dos pes, +y local), com
     * o mesmo tamanho do repouso (sem isso o braco do rig apontava para cima, para fora do chao).
     */
    static void bracosAoLongoDoCorpo(int[] pose, int[][] repouso) {
        int[][] idx = {{14, 16}, {18, 20}};
        for (int b = 0; b < 2; b++) {
            int[] r = repouso[b];
            double l1 = Math.hypot(r[2] - r[0], r[3] - r[1]);
            double l2 = Math.hypot(r[4] - r[2], r[5] - r[3]);
            double ex = r[0] + 2;
            double ey = r[1] + l1;
            pose[idx[b][0]] = (int) Math.round(ex - r[2]);
            pose[idx[b][0] + 1] = (int) Math.round(ey - r[3]);
            pose[idx[b][1]] = (int) Math.round(ex - r[4]);
            pose[idx[b][1] + 1] = (int) Math.round(ey + l2 - r[5]);
        }
    }

    private final Map<IrmaoMiau, Integer> ultimoXArrasto = new IdentityHashMap<>();
    private final Map<IrmaoMiau, Double> faseArrasto = new IdentityHashMap<>();
    private final Map<IrmaoMiau, Integer> ticksSemAndarArrasto = new IdentityHashMap<>();
    /** 5Q.8: andou nos ultimos ticks = ainda esta arrastando (o arrasto avanca de 1 em 1 px). */
    public static final int TICKS_ANDANDO_ARRASTO = 8;
    /** 5Q.8: duracao (ticks) de um ciclo de passos arrastando em pe e de joelhos. */
    public static final int TICKS_PASSO_ARRASTO = 22;
    public static final int TICKS_PASSO_ARRASTO_JOELHOS = 26;
    /** 5Q.7 corretiva: 0..1, sobe enquanto anda arrastando e volta parado (sem estalo). */
    private final Map<IrmaoMiau, Double> esforcoArrasto = new IdentityHashMap<>();
    /** 5Q.7 corretiva: inclinacao para tras (graus) e puxada das maos (px) no esforco maximo. */
    public static final int INCLINACAO_ESFORCO_ARRASTO = 8;
    public static final int PUXADA_MAOS_ARRASTO = 5;

    /** 5Q.7 corretiva: quem arrasta inclina o tronco para tras e as maos puxam (os bracos acompanham). */
    static void esforcoDoArrasto(int[] pose, double esforco) {
        pose[9] -= (int) Math.round(INCLINACAO_ESFORCO_ARRASTO * esforco);
        pose[11] -= (int) Math.round(3 * esforco);
        int p = (int) Math.round(PUXADA_MAOS_ARRASTO * esforco);
        pose[16] -= p;
        pose[20] -= p;
        pose[14] -= p / 2;
        pose[18] -= p / 2;
    }

    /** 5Q.7 corretiva: quem e arrastado mexe so um pouco o quadril e as pernas conforme desliza. */
    static void quadrilArrastado(int[] pose, int x, double esforco) {
        double f = Math.sin(2 * Math.PI * Math.floorMod(x, CICLO_PASSO_ARRASTO_PX) / CICLO_PASSO_ARRASTO_PX);
        int a = (int) Math.round(2 * f * esforco);
        pose[6] += a;
        pose[25] += a;
        pose[29] -= a;
        // 5Q.8: sendo puxado pelos pes, os bracos ficam para tras (para o lado da cabeca, -y local) e roçam o chao
        int b = (int) Math.round(10 * esforco);
        pose[17] -= b;
        pose[21] -= b;
        pose[15] -= b / 2;
        pose[19] -= b / 2;
        pose[11] -= (int) Math.round(3 * esforco);              // a cabeca acompanha
    }

    /** 5Q.7: passos curtos de quem arrasta: joelho e pe sobem alternados conforme o x anda. */
    static void passosDoArrasto(int[] pose, int x) {
        passosDoArrasto(pose, x, false);
    }

    /**
     * 5Q.7 corretiva / 5Q.8: passos de quem arrasta, pelo x real (parado, nada anda). Em pe: o pe do passo
     * sobe e vai para tras (quem arrasta recua), o joelho acompanha e o quadril sobe e desce de leve. De
     * joelhos: os joelhos "andam" alternados (sobe um pouco e recua), o quadril balanca para o lado do
     * passo. A integridade depois mantem o tamanho de coxa e canela.
     */
    static void passosDoArrasto(int[] pose, int x, boolean deJoelhos) {
        double f = Math.sin(2 * Math.PI * Math.floorMod(x, CICLO_PASSO_ARRASTO_PX) / CICLO_PASSO_ARRASTO_PX);
        double tras = Math.max(0, f);
        double frente = Math.max(0, -f);
        if (deJoelhos) {
            // joelho e canela juntos (a integridade recalcula o joelho pelo pe): sobe um pouco e recua
            int sobeT = (int) Math.round(4 * tras);
            int recuaT = (int) Math.round(6 * tras);
            int sobeF = (int) Math.round(4 * frente);
            int recuaF = (int) Math.round(6 * frente);
            pose[23] -= sobeT;
            pose[25] -= sobeT;
            pose[22] -= recuaT;
            pose[24] -= recuaT;
            pose[27] -= sobeF;
            pose[29] -= sobeF;
            pose[26] -= recuaF;
            pose[28] -= recuaF;
            pose[6] += (int) Math.round(3 * f);                 // o quadril balanca com o passo
            return;
        }
        pose[25] -= (int) Math.round(9 * tras);                  // pe sobe
        pose[24] -= (int) Math.round(6 * tras);                  // e vai para tras
        pose[22] -= (int) Math.round(3 * tras);
        pose[29] -= (int) Math.round(9 * frente);
        pose[28] -= (int) Math.round(6 * frente);
        pose[26] -= (int) Math.round(3 * frente);
        pose[5] -= (int) Math.round(2 * Math.abs(f));            // o quadril sobe no meio do passo
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
