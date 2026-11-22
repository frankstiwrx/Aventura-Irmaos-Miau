import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * SUPERENTREGA 6.3 - Dois personagens no Rig 2.0: cena, animacoes sincronizadas, contatos, pegadas e a primeira tecnica.
 *
 * - Cada lutador e o MESMO modelo (ConceitoRig2) com aparencia propria (Branco ou Preto), posicao no mundo e
 *   orientacao. Uma animacao dupla devolve as duas poses no MESMO instante t (uma linha do tempo compartilhada).
 * - Pegadas: a mao de A tem como alvo um ponto de contato do kimono de B (gola, manga, nuca...), resolvido a cada
 *   quadro depois da pose de B; por isso a mao acompanha o adversario quando ele se move.
 * - A camada nao decide nada de combate: so descreve o movimento e os instantes (eventos) que o motor usaria.
 */
public class DuelosRig2 {

    // ===================================================================== quadro e contrato
    /** Uma pegada: mao (0 = direita, 1 = esquerda) de A no ponto de contato de B, com progresso 0..1. */
    record Pegada(int mao, String ponto, double progresso) {}

    /** Estado dos dois lutadores num instante. Posicoes no mundo: x lateral, z ao longo da linha entre eles. */
    static final class QuadroDuplo {
        final ConceitoRig2.ParametrosPose[] pose = new ConceitoRig2.ParametrosPose[2];
        final double[] x = new double[2], z = new double[2], guinada = new double[2];
        final List<Pegada> pegadas = new ArrayList<>();
        String fase = "";
    }

    interface AnimacaoDupla {
        String id();
        String nome();
        double duracao();
        boolean emLoop();
        default String proxima() { return null; }
        QuadroDuplo amostrar(double t);
        /** Instantes relevantes para o motor de combate (contato, janela, resolucao...). */
        default List<BibliotecaRig2.Evento> eventos() { return List.of(); }
    }

    // ===================================================================== geometria do mundo
    /** Corpo -> mundo: gira pela guinada do lutador e translada pela posicao. */
    static ConceitoRig2.V paraMundo(ConceitoRig2.V v, double x, double z, double guinada) {
        double r = Math.toRadians(guinada), c = Math.cos(r), s = Math.sin(r);
        return new ConceitoRig2.V(v.x() * c + v.z() * s + x, v.y(), -v.x() * s + v.z() * c + z);
    }

    static ConceitoRig2.V paraCorpo(ConceitoRig2.V w, double x, double z, double guinada) {
        double r = Math.toRadians(guinada), c = Math.cos(r), s = Math.sin(r), dx = w.x() - x, dz = w.z() - z;
        return new ConceitoRig2.V(dx * c - dz * s, w.y(), dx * s + dz * c);
    }

    /** Distancia do centro da pata ao punho (o alvo do IK fica antes do ponto, para a pata cobrir o contato). */
    static final double PATA = 8;

    /**
     * Resolve o quadro: monta B, calcula os pontos de contato de B no mundo, converte para o corpo de A e da as
     * maos de A como alvo (misturando a posicao livre da mao com o contato pelo progresso da pegada).
     */
    static ConceitoRig2.Corpo[] resolver(QuadroDuplo q) {
        ConceitoRig2.Corpo b = ConceitoRig2.montarPose(q.pose[1]);
        Map<String, ConceitoRig2.V> contB = ConceitoRig2.pontosContato(b);
        ConceitoRig2.ParametrosPose pa = q.pose[0].copia();
        ConceitoRig2.Corpo livre = ConceitoRig2.montarPose(pa);
        Map<Integer, ConceitoRig2.V> pontos = new HashMap<>();
        for (Pegada g : q.pegadas) {
            ConceitoRig2.V ponto = paraCorpo(paraMundo(contB.get(g.ponto()), q.x[1], q.z[1], q.guinada[1]), q.x[0], q.z[0], q.guinada[0]);
            pontos.put(g.mao(), ponto);
            pa.alvoMao[g.mao()] = ponto.menos(ponto.menos(livre.ombro[g.mao()]).unit().vezes(PATA));
            pa.maoFechada[g.mao()] = AnimacoesRig2.trecho(g.progresso(), .7, 1);
        }
        // a pata se estende na direcao do antebraco: ajusta o alvo do punho ate o centro da pata cair no ponto
        ConceitoRig2.Corpo a = ConceitoRig2.montarPose(pa);
        for (int it = 0; it < 30 && !q.pegadas.isEmpty(); it++) {
            for (Pegada g : q.pegadas) {
                ConceitoRig2.V u = a.mao[g.mao()].menos(a.cotovelo[g.mao()]).unit();
                pa.alvoMao[g.mao()] = pontos.get(g.mao()).menos(u.vezes(PATA));
            }
            a = ConceitoRig2.montarPose(pa);
        }
        if (q.pegadas.stream().anyMatch(g -> g.progresso() < 1)) {
            // durante o alcance: mistura a mao livre com o alvo final
            for (Pegada g : q.pegadas) pa.alvoMao[g.mao()] = ConceitoRig2.mixV(livre.mao[g.mao()], pa.alvoMao[g.mao()], g.progresso());
            a = ConceitoRig2.montarPose(pa);
        }
        return new ConceitoRig2.Corpo[] {a, b};
    }

    /** Centro da pata da mao i de um corpo, no mundo. */
    static ConceitoRig2.V pataNoMundo(ConceitoRig2.Corpo c, int i, double x, double z, double g) {
        ConceitoRig2.V w = c.mao[i], u = w.menos(c.cotovelo[i]).unit();
        return paraMundo(w.mais(u.vezes(PATA)), x, z, g);
    }

    // ===================================================================== desenho da cena
    static final ConceitoRig2.Aparencia[] APARENCIAS = {ConceitoRig2.Aparencia.BRANCO, ConceitoRig2.Aparencia.PRETO};

    /** Desenha os dois lutadores (o mais distante primeiro), com esqueleto e contatos opcionais. */
    static void desenhar(ConceitoRig2.Pintor p, QuadroDuplo q, ConceitoRig2.Corpo[] cs, double vista, double ox, double chao, double esc,
                         boolean esqueleto, boolean contatos, ConceitoRig2.Aparencia[] ap, boolean[] visivel) {
        ConceitoRig2.Camera cam = new ConceitoRig2.Camera(vista);
        Integer[] ordem = {0, 1};
        Arrays.sort(ordem, Comparator.comparingDouble(i -> cam.prof(new ConceitoRig2.V(q.x[i], 0, q.z[i]))));
        for (int i : ordem) {
            if (!visivel[i]) continue;
            ConceitoRig2.V raiz = new ConceitoRig2.V(q.x[i], 0, q.z[i]);
            double sx = cam.x(raiz), sy = ConceitoRig2.Camera.INCLINACAO * cam.prof(raiz);
            ConceitoRig2.figura(p, cs[i], vista + q.guinada[i], ox + sx * esc, chao + sy * esc, esc, 1, false, ap[i]);
            if (esqueleto) {
                p.salvar();
                p.mover(ox + sx * esc, chao + sy * esc);
                p.escalar(esc);
                p.mover(0, -ConceitoRig2.CHAO);
                new ConceitoRig2.Desenho(p, cs[i], vista + q.guinada[i], ap[i]).esqueleto(false);
                p.restaurar();
            }
        }
        if (contatos && visivel[0] && visivel[1]) {
            Map<String, ConceitoRig2.V> contB = ConceitoRig2.pontosContato(cs[1]);
            p.salvar();
            p.mover(ox, chao);
            p.escalar(esc);
            for (Pegada g : q.pegadas) {
                ConceitoRig2.V alvo = paraMundo(contB.get(g.ponto()), q.x[1], q.z[1], q.guinada[1]);
                ConceitoRig2.V pata = pataNoMundo(cs[0], g.mao(), q.x[0], q.z[0], q.guinada[0]);
                Point2D a = proj(cam, alvo), b = proj(cam, pata);
                Color cor = g.progresso() >= 1 ? new Color(90, 220, 120) : new Color(255, 208, 100);
                p.linha(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 160), .8, b.getX(), b.getY(), a.getX(), a.getY());
                p.forma(new Ellipse2D.Double(a.getX() - 3.2, a.getY() - 3.2, 6.4, 6.4), cor, new Color(20, 60, 30), .7);
                p.texto(g.ponto(), a.getX() + 5, a.getY() - 5, 7.5, ConceitoRig2.TEXTO, false, -1);
            }
            p.restaurar();
        }
    }

    static Point2D proj(ConceitoRig2.Camera cam, ConceitoRig2.V w) {
        return new Point2D.Double(cam.x(w), w.y() - ConceitoRig2.CHAO + ConceitoRig2.Camera.INCLINACAO * cam.prof(w));
    }

    /** Quadro isolado (para folhas de contato e testes). */
    static BufferedImage quadro(QuadroDuplo q, double vista, int w, int h, double esc, boolean esqueleto, boolean contatos) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        ConceitoRig2.Pintor p = new ConceitoRig2.Pintor(g, w, h);
        ConceitoRig2.fundo(p);
        p.linha(new Color(166, 176, 190, 50), 1, 6, h - 23, w - 6, h - 23);
        desenhar(p, q, resolver(q), vista, w / 2.0, h - 24, esc, esqueleto, contatos, APARENCIAS, new boolean[] {true, true});
        g.dispose();
        return img;
    }

    // ===================================================================== animacoes duplas
    /** Distancia entre as raizes na disputa de pegada (alcance confortavel dos bracos de 145%). */
    static final double DISTANCIA_PEGADA = 88;

    static QuadroDuplo base(double t, double distancia) {
        QuadroDuplo q = new QuadroDuplo();
        q.pose[0] = new AnimacoesRig2.IdleCombate().amostrar(t % AnimacoesRig2.IdleCombate.CICLO);
        q.pose[1] = new AnimacoesRig2.IdleCombate().amostrar((t + 1.2) % AnimacoesRig2.IdleCombate.CICLO); // fora de fase: dois corpos vivos
        // postura de disputa de pegada: tronco quase reto e queixo recolhido. Com a inclinacao da guarda solo (8 graus),
        // as cabecas grandes dos Miaus (raio 35) se sobreporiam na distancia da pegada.
        for (int i = 0; i < 2; i++) { q.pose[i].inclinacao -= 7; q.pose[i].cabecaZ = -6; }
        q.z[0] = -distancia / 2;
        q.z[1] = distancia / 2;
        q.guinada[0] = 0;
        q.guinada[1] = 180;
        return q;
    }

    /**
     * Definicao de uma pegada do jogo (tecnicas.json): ids, nome, descricao e os pontos de contato de cada mao de A
     * no kimono de B. Lados: a mao esquerda de A (lado da frente na base de combate) fica em frente ao lado direito de B.
     */
    record DefPegada(String id, String nome, String descricao, String pontoMaoDir, String pontoMaoEsq, int ladoPuxadoB, double alcanceJogo) {
        /** Distancia entre as raizes proporcional ao alcance da pegada no jogo (pegada_1: 125 px -> 88 unidades). */
        double distancia() { return DISTANCIA_PEGADA * alcanceJogo / 125.0; }
    }

    static final List<DefPegada> PEGADAS = List.of(
            new DefPegada("pegada_1", "Pegada 1", "uma lapela + um braço: mão da frente na lapela, mão de trás na manga", "manga_esq", "gola_dir", 0, 125),
            new DefPegada("pegada_2", "Pegada 2", "cruzada: mão da frente na manga, mão de trás cruza para a gola", "gola_dir", "manga_dir", 0, 115),
            new DefPegada("pegada_3", "Pegada 3", "duas mangas", "manga_esq", "manga_dir", 0, 135),
            new DefPegada("pegada_4", "Pegada 4", "gola e nuca: mão da frente na nuca, mão de trás na gola", "gola_esq", "nuca", 1, 110));

    /**
     * Pegada 4 (gola e nuca) ainda NAO e executavel: na distancia do jogo o braco precisa contornar a cabeca do Miau
     * (raio 35) para chegar a nuca e atravessa a cabeca ou o tronco do adversario; mais perto, os troncos se tocam.
     * Fica registrada como "Em implementacao" (ver README, secao 13).
     */
    static boolean executavel(DefPegada d) { return !d.id().equals("pegada_4"); }

    static DefPegada pegada(String id) {
        for (DefPegada d : PEGADAS) if (id.equals(d.id()) || id.equals(d.id() + "-mantida")) return executavel(d) ? d : null;
        return null;
    }

    /** Entrada da pegada: 30 quadros do jogo (16 ms) = 0,48 s. Termina na pegada mantida. */
    static final double ENTRADA_PEGADA = 30 * .016;

    static final class EntradaPegada implements AnimacaoDupla {
        final DefPegada d;
        EntradaPegada(DefPegada d) { this.d = d; }
        public String id() { return d.id(); }
        public String nome() { return d.nome() + " (entrada) — " + d.descricao(); }
        public double duracao() { return ENTRADA_PEGADA; }
        public boolean emLoop() { return false; }
        public String proxima() { return d.id() + "-mantida"; }
        public List<BibliotecaRig2.Evento> eventos() {
            return List.of(new BibliotecaRig2.Evento("contato das mãos", ENTRADA_PEGADA * .72),
                    new BibliotecaRig2.Evento("pegada estabelecida (estado " + d.id().toUpperCase(Locale.ROOT) + ")", ENTRADA_PEGADA));
        }

        public QuadroDuplo amostrar(double t) {
            double x = Math.max(0, Math.min(1, t / ENTRADA_PEGADA));
            QuadroDuplo q = base(t, d.distancia());
            // a mao de tras sai um pouco depois da mao da frente
            q.pegadas.add(new Pegada(0, d.pontoMaoDir(), AnimacoesRig2.trecho(x, .1, 1)));
            q.pegadas.add(new Pegada(1, d.pontoMaoEsq(), AnimacoesRig2.trecho(x, 0, .9)));
            reacaoReceptor(q.pose[1], d, AnimacoesRig2.trecho(x, .5, 1));
            q.fase = x < .72 ? "alcance" : x < 1 ? "contato" : "pegada";
            return q;
        }
    }

    /** Reacao de quem recebe a pegada: e puxado um pouco (tronco e o braco do lado puxado vem para a frente). */
    static void reacaoReceptor(ConceitoRig2.ParametrosPose b, DefPegada d, double k) {
        b.inclinacao += 3 * k;
        b.avanco[d.ladoPuxadoB()] += 8 * k;
        b.quadrilZ += 2 * k;
    }

    /** Pegada mantida (loop de 2,4 s): os dois respiram e ajustam a base; as maos de A nao soltam. */
    static final class PegadaMantida implements AnimacaoDupla {
        final DefPegada d;
        PegadaMantida(DefPegada d) { this.d = d; }
        public String id() { return d.id() + "-mantida"; }
        public String nome() { return d.nome() + " (mantida)"; }
        public double duracao() { return AnimacoesRig2.IdleCombate.CICLO; }
        public boolean emLoop() { return true; }
        public QuadroDuplo amostrar(double t) {
            QuadroDuplo q = base(ENTRADA_PEGADA + t, d.distancia());
            q.pegadas.add(new Pegada(0, d.pontoMaoDir(), 1));
            q.pegadas.add(new Pegada(1, d.pontoMaoEsq(), 1));
            reacaoReceptor(q.pose[1], d, 1);
            q.fase = "pegada";
            return q;
        }
    }

    /**
     * Primeira tecnica sincronizada: DERRUBADA FORCADA (derrubada_forcada, "FORCAR QUEDA") no desfecho DEFENDIDO.
     * Parte da pegada 1 mantida. Janela de preparacao do motor: 16 ticks = 0,256 s (A carrega o peso, da um passo e
     * empurra). Na resolucao a defesa vence: B perde a base (tronco para tras, passo atras, bracos), recupera e os dois
     * voltam a pegada 1 mantida. O desfecho de SUCESSO (B no chao: EM_PE_CONTRA_SOLO) fica pendente: exige que o corpo
     * possa cair (orientacao da raiz), o que o Rig 2.0 ainda nao tem.
     */
    static final class DerrubadaForcadaDefendida implements AnimacaoDupla {
        static final double DURACAO = 1.4, JANELA = 16 * .016;
        final DefPegada d = PEGADAS.get(0);
        final PegadaMantida mantida = new PegadaMantida(d);

        public String id() { return "derrubada_forcada"; }
        public String nome() { return "Derrubada forçada (defendida) — a partir da pegada 1"; }
        public double duracao() { return DURACAO; }
        public boolean emLoop() { return false; }
        public String proxima() { return d.id() + "-mantida"; }
        public List<BibliotecaRig2.Evento> eventos() {
            return List.of(new BibliotecaRig2.Evento("início da janela de preparação (A exposto: AVANCO)", 0),
                    new BibliotecaRig2.Evento("resolução: defendida (fim da janela de 16 ticks)", JANELA),
                    new BibliotecaRig2.Evento("desequilíbrio máximo de B", .5),
                    new BibliotecaRig2.Evento("B recupera a base", .95),
                    new BibliotecaRig2.Evento("volta à pegada 1 mantida", DURACAO));
        }

        /** Arco de passo: 0 -> 1 com o pe levantando no meio. */
        static double[] passo(double t, double a, double b, double altura) {
            double k = AnimacoesRig2.trecho(t, a, b);
            return new double[] {k, altura * Math.sin(Math.PI * Math.max(0, Math.min(1, (t - a) / (b - a))))};
        }

        public QuadroDuplo amostrar(double t) {
            QuadroDuplo q = mantida.amostrar(0);   // a tecnica parte da pegada mantida e volta a ela
            ConceitoRig2.ParametrosPose a = q.pose[0], b = q.pose[1];
            // ---- A: carrega, passo a frente com o pe da frente (esq.), inclina e empurra; depois volta
            double ida = AnimacoesRig2.trecho(t, .04, JANELA + .08), volta = AnimacoesRig2.trecho(t, .85, 1.3);
            double avancoA = 9 * (ida - volta);
            q.z[0] += avancoA;
            double[] pIda = passo(t, .04, .26, 8), pVolta = passo(t, .9, 1.22, 6);
            for (int i = 0; i < 2; i++) {
                double passoPe = i == 1 ? 15 * (pIda[0] - pVolta[0]) : 0;        // so o pe da frente da o passo
                double alt = i == 1 ? pIda[1] + pVolta[1] : 0;
                a.pe[i] = a.pe[i].mais(new ConceitoRig2.V(0, -alt, passoPe - avancoA)); // os outros pes ficam plantados no mundo
            }
            a.inclinacao += 10 * (ida - volta) - 4 * AnimacoesRig2.trecho(t, JANELA, .6) * (1 - volta);
            a.baixar += 6 * (ida - volta);
            // ---- B: na resolucao perde a base (tronco para tras, passo atras com o pe de tras), bracos abrem; recupera
            double perde = AnimacoesRig2.trecho(t, .12, .5), recupera = AnimacoesRig2.trecho(t, .6, 1.2);
            double recuoB = 11 * (perde - recupera);
            q.z[1] += recuoB;
            double[] bIda = passo(t, .22, .5, 9), bVolta = passo(t, .9, 1.25, 6);
            for (int i = 0; i < 2; i++) {
                double passoPe = i == 0 ? -18 * (bIda[0] - bVolta[0]) : 0;      // pe de tras (dir.) recua
                double alt = i == 0 ? bIda[1] + bVolta[1] : 0;
                b.pe[i] = b.pe[i].mais(new ConceitoRig2.V(0, -alt, passoPe + recuoB));
            }
            double deseq = perde - recupera;
            b.inclinacao -= 15 * deseq;
            b.quadrilZ -= 5 * deseq;
            b.baixar += 5 * deseq;
            for (int i = 0; i < 2; i++) { b.elevacao[i] += 18 * deseq; b.avanco[i] -= 10 * deseq; }
            b.giroCabeca += 0;
            q.fase = t < JANELA ? "janela de preparação" : t < .95 ? "defendida: desequilíbrio e recuperação" : "volta à pegada";
            return q;
        }
    }

    static AnimacaoDupla criar(String id) {
        if (id.equals("derrubada_forcada")) return new DerrubadaForcadaDefendida();
        DefPegada d = pegada(id);
        if (d == null) return null;
        return id.endsWith("-mantida") ? new PegadaMantida(d) : new EntradaPegada(d);
    }

    static final List<String> IDS = List.of("pegada_1", "pegada_1-mantida", "pegada_2", "pegada_2-mantida", "pegada_3", "pegada_3-mantida",
            "derrubada_forcada");

    // ===================================================================== reprodutor duplo
    /** O mesmo relogio do Reprodutor (baseado em tempo, com encadeamento), para animacoes de dois lutadores. */
    static final class ReprodutorDuplo {
        AnimacaoDupla atual;
        double tempo, velocidade = 1;
        AnimacoesRig2.Reprodutor.Estado estado = AnimacoesRig2.Reprodutor.Estado.PARADO;

        void tocar(AnimacaoDupla a) { atual = a; tempo = 0; estado = AnimacoesRig2.Reprodutor.Estado.TOCANDO; }
        void pausar() { if (estado == AnimacoesRig2.Reprodutor.Estado.TOCANDO) estado = AnimacoesRig2.Reprodutor.Estado.PAUSADO; }
        void continuar() { if (estado == AnimacoesRig2.Reprodutor.Estado.PAUSADO && atual != null) estado = AnimacoesRig2.Reprodutor.Estado.TOCANDO; }
        void parar() { atual = null; tempo = 0; estado = AnimacoesRig2.Reprodutor.Estado.PARADO; }
        void reiniciar() { if (atual != null) { tempo = 0; estado = AnimacoesRig2.Reprodutor.Estado.TOCANDO; } }
        boolean ativo() { return atual != null && estado != AnimacoesRig2.Reprodutor.Estado.PARADO; }

        boolean avancar(double dt) {
            if (estado != AnimacoesRig2.Reprodutor.Estado.TOCANDO || atual == null || !(dt > 0)) return false;
            tempo += Math.min(dt, AnimacoesRig2.Reprodutor.PASSO_MAXIMO) * velocidade;
            boolean trocou = false;
            while (!atual.emLoop() && tempo >= atual.duracao()) {
                if (atual.proxima() == null) { tempo = atual.duracao(); estado = AnimacoesRig2.Reprodutor.Estado.PAUSADO; break; }
                tempo -= atual.duracao();
                atual = criar(atual.proxima());
                trocou = true;
            }
            return trocou;
        }

        double tempoNoCiclo() {
            if (atual == null) return 0;
            if (!atual.emLoop()) return Math.min(tempo, atual.duracao());
            return tempo - Math.floor(tempo / atual.duracao()) * atual.duracao();
        }

        QuadroDuplo quadro() { return atual == null ? null : atual.amostrar(tempoNoCiclo()); }
    }

    // ===================================================================== verificacoes (6.3)
    /** O ponto (mundo) esta dentro do tronco ou da cabeca do lutador? (margem em unidades; positiva = folga exigida) */
    static boolean dentroDoCorpo(ConceitoRig2.V w, ConceitoRig2.Corpo c, double x, double z, double g, double margem) {
        ConceitoRig2.V l = paraCorpo(w, x, z, g);
        if (l.menos(c.cabeca).tam() < ConceitoRig2.RAIO_CABECA - margem) return true;
        double y = l.y() - c.baixar;
        if (y < ConceitoRig2.TY[0] || y > 196) return false;
        ConceitoRig2.V centro = c.transformar(new ConceitoRig2.V(0, y, 0));
        double a = ConceitoRig2.interp(ConceitoRig2.TA, y) - margem, b = ConceitoRig2.interp(ConceitoRig2.TB, y) - margem;
        double dx = (l.x() - centro.x()) / a, dz = (l.z() - centro.z()) / b;
        return dx * dx + dz * dz < 1;
    }

    static List<String> verificar() {
        List<String> rel = new ArrayList<>();
        double dt = 1 / 120.0;
        for (String id : IDS) {
            AnimacaoDupla an = criar(id);
            double d = an.duracao(), total = an.emLoop() ? 2 * d : d, piorContato = 0, menorDist = 1e9, menorCabecas = 1e9;
            int quadros = 0, invasoes = 0;
            for (double t = 0; t <= total + 1e-9; t += dt) {
                double tc = an.emLoop() ? t - Math.floor(t / d) * d : Math.min(t, d);
                QuadroDuplo q = an.amostrar(tc);
                ConceitoRig2.Corpo[] cs = resolver(q);
                for (int i = 0; i < 2; i++) {
                    ConceitoRig2.ParametrosPose pp = q.pose[i].copia();
                    if (i == 0) for (Pegada g : q.pegadas) pp.alvoMao[g.mao()] = cs[0].mao[g.mao()]; // o alvo efetivo
                    AnimacoesRig2.checarCorpo(pp, cs[i], id + " lutador " + i + String.format(Locale.ROOT, " t=%.3f", t));
                }
                Map<String, ConceitoRig2.V> contB = ConceitoRig2.pontosContato(cs[1]);
                for (Pegada g : q.pegadas) {
                    if (g.progresso() < 1) continue;
                    ConceitoRig2.V alvo = paraMundo(contB.get(g.ponto()), q.x[1], q.z[1], q.guinada[1]);
                    piorContato = Math.max(piorContato, pataNoMundo(cs[0], g.mao(), q.x[0], q.z[0], q.guinada[0]).menos(alvo).tam());
                }
                // interpenetracao: cotovelos, meio do antebraco e do braco de A fora do tronco e da cabeca de B (e vice-versa)
                for (int i = 0; i < 2; i++) {
                    int o = 1 - i;
                    for (int m = 0; m < 2; m++) {
                        ConceitoRig2.V[] pts = {cs[i].cotovelo[m], cs[i].cotovelo[m].mais(cs[i].mao[m]).vezes(.5),
                            cs[i].ombro[m].mais(cs[i].cotovelo[m]).vezes(.5)};
                        for (ConceitoRig2.V v : pts)
                            if (dentroDoCorpo(paraMundo(v, q.x[i], q.z[i], q.guinada[i]), cs[o], q.x[o], q.z[o], q.guinada[o], 2)) invasoes++;
                    }
                }
                menorDist = Math.min(menorDist, Math.abs(q.z[1] - q.z[0]));
                double cab = paraMundo(cs[0].cabeca, q.x[0], q.z[0], q.guinada[0]).menos(paraMundo(cs[1].cabeca, q.x[1], q.z[1], q.guinada[1])).tam();
                menorCabecas = Math.min(menorCabecas, cab);
                quadros++;
            }
            AnimacoesRig2.checar(piorContato < 1e-6, id + ": a pata nao esta no ponto de contato (" + piorContato + ")");
            AnimacoesRig2.checar(invasoes == 0, id + ": braco atravessa o corpo do adversario (" + invasoes + " pontos)");
            AnimacoesRig2.checar(menorDist > 70, id + ": lutadores perto demais (" + menorDist + ")");
            AnimacoesRig2.checar(menorCabecas >= 2 * ConceitoRig2.RAIO_CABECA - 4, id + ": as cabecas se sobrepoem (" + menorCabecas + ")");
            String fecho = "";
            if (an.emLoop()) {
                double f = AnimacoesRig2.maiorDiferenca(juntasDuplas(an.amostrar(0)), juntasDuplas(an.amostrar(d)));
                AnimacoesRig2.checar(f < 1e-6, id + ": loop nao fecha");
                fecho = "; loop fecha";
            } else {
                double f = AnimacoesRig2.maiorDiferenca(juntasDuplas(an.amostrar(d)), juntasDuplas(criar(an.proxima()).amostrar(0)));
                AnimacoesRig2.checar(f < 1e-6, id + ": nao termina no 1o quadro de " + an.proxima() + " (" + f + ")");
                fecho = "; termina exatamente no 1o quadro de " + an.proxima();
            }
            rel.add(String.format(Locale.ROOT, "OK %s: %d quadros (1/120 s), dois lutadores com ossos constantes e pes alcancados;"
                    + " patas exatamente nos contatos (erro %.1e); nenhum braco atravessa o tronco ou a cabeca do outro; distancia minima"
                    + " %.0f; cabecas a no minimo %.0f (raio 35)%s.", id, quadros, piorContato, menorDist, menorCabecas, fecho));
        }
        // derrubada: comeca e termina na pegada mantida, eventos em ordem, pegada nunca solta, pes plantados fora dos passos
        DerrubadaForcadaDefendida df = new DerrubadaForcadaDefendida();
        double ini = AnimacoesRig2.maiorDiferenca(juntasDuplas(df.amostrar(0)), juntasDuplas(new PegadaMantida(PEGADAS.get(0)).amostrar(0)));
        AnimacoesRig2.checar(ini < 1e-6, "derrubada: nao comeca na pegada mantida");
        double ant = -1;
        for (BibliotecaRig2.Evento e : df.eventos()) { AnimacoesRig2.checar(e.tempo() >= ant, "eventos fora de ordem"); ant = e.tempo(); }
        double maxDeseq = 0, escorrega = 0;
        ConceitoRig2.V peA0 = null, peB1 = null;
        for (double t = 0; t <= df.duracao(); t += .01) {
            QuadroDuplo q = df.amostrar(t);
            ConceitoRig2.Corpo[] cs = resolver(q);
            maxDeseq = Math.max(maxDeseq, -(q.pose[1].inclinacao - df.mantida.amostrar(0).pose[1].inclinacao));
            ConceitoRig2.V a0 = paraMundo(cs[0].tornozelo[0], q.x[0], q.z[0], q.guinada[0]);   // pe de tras de A nao da passo
            ConceitoRig2.V b1 = paraMundo(cs[1].tornozelo[1], q.x[1], q.z[1], q.guinada[1]);   // pe da frente de B nao da passo
            if (peA0 != null) escorrega = Math.max(escorrega, Math.max(a0.menos(peA0).tam(), b1.menos(peB1).tam()));
            peA0 = a0; peB1 = b1;
        }
        AnimacoesRig2.checar(escorrega < 1e-6, "derrubada: pe de apoio escorrega (" + escorrega + ")");
        AnimacoesRig2.checar(maxDeseq > 10, "derrubada: B quase nao perde a base");
        rel.add(String.format(Locale.ROOT, "OK derrubada_forcada (defendida): comeca e termina na pegada 1 mantida, eventos em ordem"
                + " (janela de 16 ticks = %.3f s), a pegada nunca solta, os pes que nao dao passo ficam plantados no mundo (erro %.1e)"
                + " e B inclina %.0f graus para tras no desequilibrio.", DerrubadaForcadaDefendida.JANELA, escorrega, maxDeseq));
        // perspectiva nao altera a cena; repeticao sem acumulo
        QuadroDuplo q = df.amostrar(.5);
        List<ConceitoRig2.V> antes = juntasDuplas(q);
        for (int g = 0; g <= 180; g += 15) quadro(q, g, 220, 300, .6, true, true);
        AnimacoesRig2.checar(antes.equals(juntasDuplas(q)), "a perspectiva alterou a cena");
        ReprodutorDuplo r = new ReprodutorDuplo();
        r.tocar(criar("pegada_1-mantida"));
        for (int k = 0; k < 40 * 144; k++) r.avancar(1 / 60.0);
        double desvio = AnimacoesRig2.maiorDiferenca(juntasDuplas(r.quadro()), juntasDuplas(r.atual.amostrar(r.tempoNoCiclo())));
        double raizes = Math.abs(r.quadro().z[0] + DISTANCIA_PEGADA / 2) + Math.abs(r.quadro().z[1] - DISTANCIA_PEGADA / 2);
        AnimacoesRig2.checar(desvio < 1e-6 && raizes < 1e-9, "pegada mantida acumulou deslocamento");
        r.tocar(criar("pegada_2"));
        for (int k = 0; k < 120; k++) r.avancar(1 / 60.0);
        AnimacoesRig2.checar(r.atual.id().equals("pegada_2-mantida"), "a entrada da pegada nao encadeou na pegada mantida");
        rel.add("OK: mudar a perspectiva (0 a 180 graus) nao altera a cena; 40 ciclos da pegada mantida sem deslocamento acumulado;"
                + " a entrada da pegada encadeia na pegada mantida pelo mesmo relogio.");
        return rel;
    }

    static List<ConceitoRig2.V> juntasDuplas(QuadroDuplo q) {
        ConceitoRig2.Corpo[] cs = resolver(q);
        List<ConceitoRig2.V> r = new ArrayList<>();
        for (int i = 0; i < 2; i++) for (ConceitoRig2.V v : ConceitoRig2.juntas(cs[i])) r.add(paraMundo(v, q.x[i], q.z[i], q.guinada[i]));
        return r;
    }

    public static void main(String[] args) {
        verificar().forEach(System.out::println);
    }
}
