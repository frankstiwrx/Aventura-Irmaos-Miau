import java.util.*;

/**
 * SUPERENTREGA 6.3 - Movimentos basicos novos do Rig 2.0 e componentes de movimento reutilizaveis.
 *
 * Usa o mesmo contrato e o mesmo reprodutor de AnimacoesRig2 (funcoes puras do tempo). As animacoes aprovadas
 * na 6.2 (idles, caminhada, transicoes) continuam em AnimacoesRig2 e nao foram alteradas.
 */
public class MovimentosRig2 {

    static final String CORRIDA = "corrida", AGACHAR = "agachar", AGACHADO = "agachado",
            LEVANTAR_AGACHAMENTO = "levantar-agachamento", PULO = "pulo";
    static final String[] IDS = {CORRIDA, AGACHAR, AGACHADO, LEVANTAR_AGACHAMENTO, PULO};

    static AnimacoesRig2.Animacao criar(String id, ConceitoRig2.ParametrosPose inicio) {
        return switch (id) {
            case CORRIDA -> new Corrida();
            case AGACHAR -> new TransicaoSimples(AGACHAR, "Agachar (descida)", inicio != null ? inicio : idleNeutro(),
                    Agachado.pose(), .55, AGACHADO, ConceitoRig2.PoseBase.NEUTRA, new double[] {0, .9, .1, 1, 0, .85});
            case AGACHADO -> new Agachado();
            case LEVANTAR_AGACHAMENTO -> new TransicaoSimples(LEVANTAR_AGACHAMENTO, "Levantar do agachamento",
                    inicio != null ? inicio : Agachado.pose(), idleNeutro(), .6, AnimacoesRig2.IDLE_NEUTRO,
                    ConceitoRig2.PoseBase.NEUTRA, new double[] {.1, 1, 0, .7, 0, .9});
            case PULO -> new Pulo();
            default -> null;
        };
    }

    static ConceitoRig2.ParametrosPose idleNeutro() { return new AnimacoesRig2.IdleNeutro().amostrar(0); }

    // ===================================================================== componentes reutilizaveis
    /**
     * Componentes de movimento: pequenas funcoes que alteram canais de uma pose. As animacoes novas sao montadas com
     * eles, em vez de cada tecnica reimplementar tudo. (Giro do tronco ainda nao existe: o modelo nao tem esse eixo.)
     */
    static final class Componentes {
        /** Respiracao: fase 0..1 de um ciclo; amplitude 1 = a do idle neutro aprovado. */
        static void respirar(ConceitoRig2.ParametrosPose p, double fase, double amp) {
            double r = .5 - .5 * Math.cos(AnimacoesRig2.TAU * fase);
            p.ombros += 1.3 * amp * r;
            p.inclinacao -= .7 * amp * r;
            for (int i = 0; i < 2; i++) p.flexao[i] += 3 * amp * r;
        }

        /** Transferencia de peso: desloca o quadril (lateral e para a frente) sem mover os pes. */
        static void transferirPeso(ConceitoRig2.ParametrosPose p, double x, double z) { p.quadrilX += x; p.quadrilZ += z; }

        /** Flexao dos joelhos: abaixa o quadril (os pes continuam plantados; o IK dobra os joelhos). */
        static void flexionarJoelhos(ConceitoRig2.ParametrosPose p, double quanto) { p.baixar += quanto; }

        static void inclinarTronco(ConceitoRig2.ParametrosPose p, double graus) { p.inclinacao += graus; }

        /** Braco por angulos absolutos (elevacao, avanco, flexao do cotovelo). */
        static void posicionarBraco(ConceitoRig2.ParametrosPose p, int i, double elev, double av, double flex) {
            p.elevacao[i] = elev; p.avanco[i] = av; p.flexao[i] = flex;
        }

        /** Corpo inteiro no ar: sobe o quadril e os pes juntos (o chao e a sombra ficam). */
        static void elevar(ConceitoRig2.ParametrosPose p, double altura) {
            p.baixar -= altura;
            for (int i = 0; i < 2; i++) p.pe[i] = p.pe[i].mais(new ConceitoRig2.V(0, -altura, 0));
        }

        /** Preparacao para um impulso ou queda: agacha e inclina (k = 0..1). */
        static void prepararImpulso(ConceitoRig2.ParametrosPose p, double k) {
            flexionarJoelhos(p, 22 * k);
            inclinarTronco(p, 12 * k);
        }

        /** Recuperacao de postura: aproxima a pose atual de uma pose de referencia (k = 0..1). */
        static ConceitoRig2.ParametrosPose recuperar(ConceitoRig2.ParametrosPose atual, ConceitoRig2.ParametrosPose ref, double k) {
            return ConceitoRig2.ParametrosPose.interpolar(atual, ref, k);
        }

        /**
         * Passo generico de uma perna (caminhada, corrida): no apoio o tornozelo recua na velocidade do chao; no fim do
         * apoio o calcanhar sobe com a ponta no chao; no balanco a perna avanca por uma curva de Hermite que sai e chega
         * com a velocidade do chao. Retorna {z, y, inclinacao do pe, 1 = apoio}.
         */
        static double[] passo(double u, double apoio, double passo, double altura, double calcanhar) {
            double meio = passo / 2;
            if (u < apoio) {
                double z = meio - passo * (u / apoio);
                double incl = calcanhar * AnimacoesRig2.trecho(u, apoio * .75, apoio);
                return new double[] {z, AnimacoesRig2.Caminhada.alturaComCalcanhar(incl), incl, 1};
            }
            double w = (u - apoio) / (1 - apoio), m = -passo * (1 - apoio) / apoio;
            double h00 = 2 * w * w * w - 3 * w * w + 1, h10 = w * w * w - 2 * w * w + w;
            double h01 = -2 * w * w * w + 3 * w * w, h11 = w * w * w - w * w;
            double z = h00 * -meio + h10 * m + h01 * meio + h11 * m;
            double incl = w < .35 ? calcanhar * (1 - AnimacoesRig2.trecho(w, 0, .35)) : -10 * Math.sin(Math.PI * (w - .35) / .65);
            double yFim = AnimacoesRig2.Caminhada.alturaComCalcanhar(calcanhar), chao = AnimacoesRig2.Caminhada.CHAO_TORNOZELO;
            double y = chao - (chao - yFim) * (1 - AnimacoesRig2.trecho(w, 0, .35)) - altura * Math.pow(Math.sin(Math.PI * w), 2);
            return new double[] {z, y, incl, 0};
        }
    }

    // ===================================================================== corrida
    /**
     * Corrida no lugar (ciclo de 0,66 s, dois passos). Diferente da caminhada, e nao uma caminhada acelerada:
     * apoio curto (35% do ciclo por perna), entao ha duas fases de SUSPENSAO por ciclo, sem nenhum pe no chao;
     * passada e elevacao do pe maiores (o calcanhar sobe atras), tronco mais inclinado, quadril mais alto no voo e
     * baixo no meio do apoio (absorcao), bracos dobrados a ~85 graus balancando em oposicao as pernas.
     */
    static final class Corrida implements AnimacoesRig2.Animacao {
        static final double CICLO = .66, APOIO = .35;
        static final double PERNA = ConceitoRig2.COXA + ConceitoRig2.CANELA;
        static final double PASSO = .62 * PERNA, ALTURA = .32 * PERNA, CALCANHAR = 26, LARGURA = 11;
        static final double VELOCIDADE = PASSO / (APOIO * CICLO);

        public String id() { return CORRIDA; }
        public String nome() { return "Corrida (no lugar)"; }
        public double duracao() { return CICLO; }
        public boolean emLoop() { return true; }
        public ConceitoRig2.PoseBase baseFinal() { return ConceitoRig2.PoseBase.NEUTRA; }
        public double deslocamento(double tTotal) { return VELOCIDADE * tTotal; }

        static double fase(int i, double t) {
            double u = t / CICLO + (i == 0 ? 0 : .5);
            return u - Math.floor(u);
        }

        public ConceitoRig2.ParametrosPose amostrar(double t) {
            ConceitoRig2.ParametrosPose p = ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.NEUTRA);
            double fi = t / CICLO, tau = AnimacoesRig2.TAU;
            // voo no meio das fases .35-.5 e .85-1; apoio no meio em .175 e .675
            p.baixar = 4 - 3 * Math.cos(2 * tau * (fi - .425));
            p.inclinacao = 9 + 1.5 * Math.cos(2 * tau * (fi - .175));
            Componentes.transferirPeso(p, -1.5 * Math.cos(tau * (fi - .175)), 0);
            p.ombros = .6 * (.5 - .5 * Math.cos(2 * tau * (fi - .425)));
            double[] zPerna = new double[2];
            for (int i = 0; i < 2; i++) {
                double lado = i == 0 ? -1 : 1;
                double[] e = Componentes.passo(fase(i, t), APOIO, PASSO, ALTURA, CALCANHAR);
                zPerna[i] = e[0];
                p.pe[i] = new ConceitoRig2.V(lado * LARGURA, e[1], e[0]);
                p.poloPerna[i] = new ConceitoRig2.V(lado * .1, 0, 1);
                p.guinadaPe[i] = lado * 4;
                p.inclinacaoPe[i] = e[2];
            }
            for (int i = 0; i < 2; i++) {
                double a = zPerna[1 - i] / (PASSO / 2);
                p.avanco[i] = 8 + 34 * a;
                p.flexao[i] = 85 + 12 * a;
                p.maoFechada[i] = .6;
            }
            return p;
        }
    }

    // ===================================================================== agachamento
    /** Agachamento sustentado (loop de 3 s): respira e equilibra o peso, com os calcanhares no chao. */
    static final class Agachado implements AnimacoesRig2.Animacao {
        static final double CICLO = 3.0;

        /** Pose agachada: quadril 44 abaixo e 14 para tras, tronco 16 graus a frente, pes afastados e virados, bracos a frente para equilibrar. */
        static ConceitoRig2.ParametrosPose pose() {
            ConceitoRig2.ParametrosPose p = idleNeutro();
            Componentes.flexionarJoelhos(p, 44);
            Componentes.inclinarTronco(p, 16);
            Componentes.transferirPeso(p, 0, -14); // o quadril vai para tras, como num agachamento de verdade
            for (int i = 0; i < 2; i++) {
                double lado = i == 0 ? -1 : 1;
                p.pe[i] = new ConceitoRig2.V(lado * 24, 258, 2);
                p.poloPerna[i] = new ConceitoRig2.V(lado * .35, 0, 1);
                p.guinadaPe[i] = lado * 14;
                Componentes.posicionarBraco(p, i, 6, 55, 28);
            }
            p.giroCabeca = 0;
            return p;
        }

        public String id() { return AGACHADO; }
        public String nome() { return "Agachado (sustentado)"; }
        public double duracao() { return CICLO; }
        public boolean emLoop() { return true; }
        public ConceitoRig2.PoseBase baseFinal() { return ConceitoRig2.PoseBase.NEUTRA; }

        public ConceitoRig2.ParametrosPose amostrar(double t) {
            ConceitoRig2.ParametrosPose p = pose();
            double f = t / CICLO;
            Componentes.respirar(p, f, .8);
            Componentes.transferirPeso(p, 1.2 * Math.sin(AnimacoesRig2.TAU * f), .8 * Math.sin(2 * AnimacoesRig2.TAU * f));
            return p;
        }
    }

    // ===================================================================== transicao simples
    /**
     * Transicao entre duas poses por canais com tempos proprios (sem passos: os pes ficam no lugar ou deslizam pouco).
     * janelas = {corpo inicio, corpo fim, bracos inicio, bracos fim, pernas inicio, pernas fim} em fracao da duracao.
     */
    static final class TransicaoSimples implements AnimacoesRig2.Animacao {
        final String id, nome, proxima;
        final ConceitoRig2.ParametrosPose inicio, fim;
        final double duracao;
        final ConceitoRig2.PoseBase base;
        final double[] janelas;

        TransicaoSimples(String id, String nome, ConceitoRig2.ParametrosPose inicio, ConceitoRig2.ParametrosPose fim, double duracao,
                         String proxima, ConceitoRig2.PoseBase base, double[] janelas) {
            this.id = id; this.nome = nome; this.inicio = inicio.copia(); this.fim = fim.copia(); this.duracao = duracao;
            this.proxima = proxima; this.base = base; this.janelas = janelas;
            this.inicio.pernas = ConceitoRig2.PresetPernas.DA_POSE;
        }

        public String id() { return id; }
        public String nome() { return nome; }
        public double duracao() { return duracao; }
        public boolean emLoop() { return false; }
        public String proxima() { return proxima; }
        public ConceitoRig2.PoseBase baseFinal() { return base; }

        public ConceitoRig2.ParametrosPose amostrar(double t) {
            double x = Math.max(0, Math.min(1, t / duracao));
            if (x >= 1) return fim.copia();
            return ConceitoRig2.ParametrosPose.interpolar(inicio, fim, AnimacoesRig2.trecho(x, janelas[0], janelas[1]),
                    AnimacoesRig2.trecho(x, janelas[2], janelas[3]), AnimacoesRig2.trecho(x, janelas[4], janelas[5]));
        }
    }

    // ===================================================================== pulo
    /**
     * Pulo vertical (1,35 s, depois o idle neutro): preparacao (agacha, bracos para tras), impulsao (estende, bracos
     * sobem, calcanhares saem do chao), fase aerea com trajetoria balistica (o corpo inteiro sai do chao, joelhos
     * recolhem), aterrissagem com absorcao (joelhos dobram alem do neutro) e recuperacao ate o primeiro quadro do idle.
     */
    static final class Pulo implements AnimacoesRig2.Animacao {
        static final double DURACAO = 1.4, DECOLAGEM = .5, ATERRISSAGEM = .88, ALTURA = 46;

        final ConceitoRig2.ParametrosPose neutra = idleNeutro(), preparo, impulso, recolhido, absorcao;

        Pulo() {
            preparo = neutra.copia();
            Componentes.prepararImpulso(preparo, 1);
            for (int i = 0; i < 2; i++) Componentes.posicionarBraco(preparo, i, neutra.elevacao[i], -35, 15);
            impulso = neutra.copia();
            Componentes.flexionarJoelhos(impulso, -6);
            Componentes.inclinarTronco(impulso, 2);
            for (int i = 0; i < 2; i++) {
                Componentes.posicionarBraco(impulso, i, neutra.elevacao[i] + 6, 125, 10);
                impulso.inclinacaoPe[i] = 28;
                impulso.pe[i] = new ConceitoRig2.V(neutra.pe[i].x(), AnimacoesRig2.Caminhada.alturaComCalcanhar(28), neutra.pe[i].z());
            }
            recolhido = impulso.copia();
            Componentes.flexionarJoelhos(recolhido, 4);
            for (int i = 0; i < 2; i++) {
                recolhido.pe[i] = new ConceitoRig2.V(neutra.pe[i].x(), 258 - 20, neutra.pe[i].z() + 4);
                recolhido.inclinacaoPe[i] = 10;
                Componentes.posicionarBraco(recolhido, i, neutra.elevacao[i] + 10, 70, 30);
            }
            absorcao = neutra.copia();
            Componentes.flexionarJoelhos(absorcao, 20);
            Componentes.inclinarTronco(absorcao, 12);
            for (int i = 0; i < 2; i++) Componentes.posicionarBraco(absorcao, i, neutra.elevacao[i] + 4, 30, 25);
            pouso = neutraNoAr();
        }

        /** Pose no instante do contato com o chao (fim do voo e inicio da absorcao: a mesma pose, sem salto). */
        final ConceitoRig2.ParametrosPose pouso;

        public String id() { return PULO; }
        public String nome() { return "Pulo"; }
        public double duracao() { return DURACAO; }
        public boolean emLoop() { return false; }
        public String proxima() { return AnimacoesRig2.IDLE_NEUTRO; }
        public ConceitoRig2.PoseBase baseFinal() { return ConceitoRig2.PoseBase.NEUTRA; }

        /** Altura do corpo acima do chao (trajetoria balistica entre a decolagem e a aterrissagem). */
        static double altura(double t) {
            if (t <= DECOLAGEM || t >= ATERRISSAGEM) return 0;
            double x = (t - DECOLAGEM) / (ATERRISSAGEM - DECOLAGEM);
            return ALTURA * 4 * x * (1 - x);
        }

        public ConceitoRig2.ParametrosPose amostrar(double t) {
            ConceitoRig2.ParametrosPose p;
            if (t <= .3) p = ConceitoRig2.ParametrosPose.interpolar(neutra, preparo, AnimacoesRig2.trecho(t, 0, .3));
            else if (t <= DECOLAGEM) p = ConceitoRig2.ParametrosPose.interpolar(preparo, impulso, AnimacoesRig2.trecho(t, .3, DECOLAGEM));
            else if (t <= .69) p = ConceitoRig2.ParametrosPose.interpolar(impulso, recolhido, AnimacoesRig2.trecho(t, DECOLAGEM, .69));
            else if (t <= ATERRISSAGEM) p = ConceitoRig2.ParametrosPose.interpolar(recolhido, pouso, AnimacoesRig2.trecho(t, .69, ATERRISSAGEM));
            else if (t <= 1.06) p = ConceitoRig2.ParametrosPose.interpolar(pouso, absorcao, AnimacoesRig2.trecho(t, ATERRISSAGEM, 1.06));
            else p = ConceitoRig2.ParametrosPose.interpolar(absorcao, neutra, AnimacoesRig2.trecho(t, 1.06, DURACAO));
            if (t >= DURACAO) return neutra.copia();
            Componentes.elevar(p, altura(t));
            return p;
        }

        /** Pernas estendidas para pousar (pes planos, prontos para o contato). */
        ConceitoRig2.ParametrosPose neutraNoAr() {
            ConceitoRig2.ParametrosPose p = neutra.copia();
            for (int i = 0; i < 2; i++) Componentes.posicionarBraco(p, i, neutra.elevacao[i] + 6, 45, 20);
            return p;
        }

        List<double[]> eventos() { return List.of(new double[] {DECOLAGEM}, new double[] {(DECOLAGEM + ATERRISSAGEM) / 2}, new double[] {ATERRISSAGEM}); }
    }

    // ===================================================================== verificacoes
    static List<String> verificar() {
        List<String> rel = new ArrayList<>();
        double dt = 1 / 120.0;
        for (String id : IDS) {
            AnimacoesRig2.Animacao a = criar(id, null);
            double d = a.duracao(), total = a.emLoop() ? 3 * d : d, maiorPasso = 0;
            List<ConceitoRig2.V> ant = null;
            List<Double> passos = new ArrayList<>();
            int n = 0;
            for (double t = 0; t <= total + 1e-9; t += dt) {
                double tc = a.emLoop() ? t - Math.floor(t / d) * d : Math.min(t, d);
                ConceitoRig2.ParametrosPose p = a.amostrar(tc);
                ConceitoRig2.Corpo c = ConceitoRig2.montarPose(p);
                AnimacoesRig2.checarCorpo(p, c, id + String.format(Locale.ROOT, " t=%.3f", t));
                List<ConceitoRig2.V> js = ConceitoRig2.juntas(c);
                if (ant != null) { double ps = AnimacoesRig2.maiorDiferenca(ant, js); passos.add(ps); maiorPasso = Math.max(maiorPasso, ps); }
                ant = js;
                n++;
            }
            String extra = "";
            if (a.emLoop()) {
                double fecho = AnimacoesRig2.maiorDiferenca(AnimacoesRig2.juntasEm(a, 0), AnimacoesRig2.juntasEm(a, d));
                AnimacoesRig2.checar(fecho < 1e-6, id + ": loop nao fecha (" + fecho + ")");
                extra = String.format(Locale.ROOT, "; loop fecha (%.1e)", fecho);
            } else {
                double fim = AnimacoesRig2.maiorDiferenca(AnimacoesRig2.juntasEm(a, d),
                        AnimacoesRig2.juntasEm(AnimacoesRig2.criarQualquer(a.proxima(), null), 0));
                AnimacoesRig2.checar(fim < 1e-6, id + ": nao termina no 1o quadro de " + a.proxima() + " (" + fim + ")");
                extra = "; termina exatamente no 1o quadro de " + a.proxima();
            }
            // descontinuidade = um quadro isolado muito maior que os vizinhos (movimento rapido nao e salto)
            for (int k = 1; k + 1 < passos.size(); k++) {
                double viz = Math.max(passos.get(k - 1), passos.get(k + 1));
                AnimacoesRig2.checar(passos.get(k) <= Math.max(2.5 * viz, 1.5), id + ": salto de junta isolado (" + passos.get(k) + " vs " + viz + ")");
            }
            rel.add(String.format(Locale.ROOT, "OK %s: %d quadros (1/120 s) com ossos constantes, pes alcancados e nunca abaixo do chao;"
                    + " maior passo de junta %.2f por quadro, sem salto isolado%s.", id, n, maiorPasso, extra));
        }

        // corrida: suspensao, apoio sem escorregar, pernas alternando
        int semApoio = 0, amostras = 0;
        double escorrega = 0;
        for (double t = 0; t < Corrida.CICLO; t += 1 / 480.0) {
            int apoios = 0;
            for (int i = 0; i < 2; i++) {
                double u = Corrida.fase(i, t);
                double[] e = Componentes.passo(u, Corrida.APOIO, Corrida.PASSO, Corrida.ALTURA, Corrida.CALCANHAR);
                if (e[3] == 1) apoios++;
                if (u > .01 && u < Corrida.APOIO * .75 - .01) {
                    double[] e2 = Componentes.passo(Corrida.fase(i, t + 1e-4), Corrida.APOIO, Corrida.PASSO, Corrida.ALTURA, Corrida.CALCANHAR);
                    escorrega = Math.max(escorrega, Math.abs((e2[0] - e[0]) / 1e-4 + Corrida.VELOCIDADE));
                }
            }
            if (apoios == 0) semApoio++;
            amostras++;
        }
        double suspensao = (double) semApoio / amostras;
        AnimacoesRig2.checar(suspensao > .2 && suspensao < .4, "corrida: fase de suspensao fora do esperado (" + suspensao + ")");
        AnimacoesRig2.checar(escorrega < 1e-6, "corrida: pe de apoio escorrega (" + escorrega + ")");
        rel.add(String.format(Locale.ROOT, "OK corrida: %.0f%% do ciclo em suspensao (nenhum pe no chao), apoio sem escorregar (erro %.1e),"
                + " passada %.0f unidades (caminhada %.0f), pe sobe ate %.0f (caminhada %.0f).", 100 * suspensao, escorrega,
                Corrida.PASSO, AnimacoesRig2.Caminhada.PASSO, Corrida.ALTURA, AnimacoesRig2.Caminhada.ALTURA));

        // agachamento: profundidade, calcanhares no chao e equilibrio (centro do corpo sobre a base dos pes)
        AnimacoesRig2.Animacao ag = new Agachado();
        double piorEquilibrio = -1e9;
        for (double t = 0; t < Agachado.CICLO; t += .05) {
            ConceitoRig2.ParametrosPose p = ag.amostrar(t);
            ConceitoRig2.Corpo c = ConceitoRig2.montarPose(p);
            for (int i = 0; i < 2; i++)
                AnimacoesRig2.checar(Math.abs(c.tornozelo[i].y() - 258) < 1e-6 && p.inclinacaoPe[i] == 0, "agachado: calcanhar fora do chao");
            double cx = (c.pelve.x() + c.pescoco.x() + c.cabeca.x()) / 3, cz = (c.pelve.z() + c.pescoco.z() + c.cabeca.z()) / 3;
            double xMin = Math.min(c.tornozelo[0].x(), c.tornozelo[1].x()) - 10, xMax = Math.max(c.tornozelo[0].x(), c.tornozelo[1].x()) + 10;
            double zMin = Math.min(c.tornozelo[0].z(), c.tornozelo[1].z()) - 8, zMax = Math.max(c.tornozelo[0].z(), c.tornozelo[1].z()) + 22;
            double folga = Math.min(Math.min(cx - xMin, xMax - cx), Math.min(cz - zMin, zMax - cz));
            piorEquilibrio = Math.max(piorEquilibrio, -folga);
            AnimacoesRig2.checar(folga > 0, "agachado: centro do corpo fora da base dos pes");
        }
        double profundidade = ConceitoRig2.montarPose(ag.amostrar(0)).pelve.y() - ConceitoRig2.montarPose(idleNeutro()).pelve.y();
        AnimacoesRig2.checar(profundidade >= 40, "agachado: pouco profundo");
        rel.add(String.format(Locale.ROOT, "OK agachamento: quadril %.0f unidades abaixo do neutro, calcanhares sempre no chao, centro do corpo"
                + " (pelve, pescoco, cabeca) sempre dentro da base dos pes (folga minima %.1f).", profundidade, -piorEquilibrio));

        // pulo: sai do chao, aterrissa e absorve
        Pulo pulo = new Pulo();
        double menorPe = 999, baixarContato = pulo.amostrar(Pulo.ATERRISSAGEM).baixar, maiorAbsorcao = -999;
        for (double t = Pulo.DECOLAGEM + .01; t < Pulo.ATERRISSAGEM - .01; t += .01) {
            ConceitoRig2.Corpo c = ConceitoRig2.montarPose(pulo.amostrar(t));
            menorPe = Math.min(menorPe, Math.min(c.tornozelo[0].y(), c.tornozelo[1].y()));
            for (int i = 0; i < 2; i++) AnimacoesRig2.checar(c.tornozelo[i].y() < 258 - .5, "pulo: pe no chao durante o voo");
        }
        for (double t = Pulo.ATERRISSAGEM; t < Pulo.DURACAO; t += .01) maiorAbsorcao = Math.max(maiorAbsorcao, pulo.amostrar(t).baixar);
        AnimacoesRig2.checar(258 - menorPe > 40, "pulo: sobe pouco");
        AnimacoesRig2.checar(maiorAbsorcao > baixarContato + 15, "pulo: sem absorcao na aterrissagem");
        rel.add(String.format(Locale.ROOT, "OK pulo: os dois pes ficam fora do chao durante todo o voo (%.2f s a %.2f s), o pe sobe ate %.0f"
                + " unidades; na aterrissagem o quadril desce mais %.0f unidades (absorcao) e recupera.",
                Pulo.DECOLAGEM, Pulo.ATERRISSAGEM, 258 - menorPe, maiorAbsorcao - baixarContato));

        // continuidade visual (30 quadros/s, 40 graus)
        for (String id : IDS) {
            AnimacoesRig2.Animacao a = criar(id, null);
            int n = (int) Math.round(a.duracao() * 30);
            List<Double> difs = new ArrayList<>();
            java.awt.image.BufferedImage ant = null;
            for (int k = 0; k <= n; k++) {
                java.awt.image.BufferedImage q = ConceitoRig2.quadro(ConceitoRig2.montarPose(a.amostrar(Math.min(a.duracao(), k / 30.0))), 40, 200, 300, .95, false);
                if (ant != null) difs.add(ConceitoRig2.diferencaMedia(ant, q));
                ant = q;
            }
            List<Double> ord = new ArrayList<>(difs);
            Collections.sort(ord);
            double med = ord.get(ord.size() / 2), maior = ord.get(ord.size() - 1);
            AnimacoesRig2.checar(maior <= Math.max(3.5 * med, .3), id + ": salto visivel (" + maior + " vs " + med + ")");
            rel.add(String.format(Locale.ROOT, "OK continuidade visual %s: maior diferenca %.3f, mediana %.3f.", id, maior, med));
        }
        return rel;
    }

    public static void main(String[] args) {
        verificar().forEach(System.out::println);
    }
}
