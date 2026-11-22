import java.util.*;

/**
 * SUPERENTREGA 6.2 - Camada de animacao do Rig 2.0.
 *
 * Independente de interface: nao usa Swing nem relogio do sistema. Uma animacao e uma funcao pura
 * do tempo, t (segundos) -> ParametrosPose, sobre o mesmo modelo corporal de ConceitoRig2.
 * O Reprodutor guarda o relogio (tocar, pausar, parar, reiniciar, velocidade, loop) e e avancado
 * por quem o usa (a janela do laboratorio hoje; o laco do jogo no futuro) com o tempo real decorrido.
 * Assim a velocidade dos movimentos nao depende da quantidade de repinturas, e qualquer instante
 * pode ser reproduzido nos testes chamando amostrar(t).
 *
 * As animacoes trabalham no espaco do corpo (x lateral, y para baixo, z para a frente do lutador) e
 * com medidas relativas ao comprimento dos membros (passo, altura do pe), para poderem servir a
 * personagens com outras proporcoes no futuro. A camera (perspectiva) nunca faz parte da animacao.
 */
public class AnimacoesRig2 {

    // ===================================================================== contrato
    /** Uma animacao do Rig 2.0. */
    interface Animacao {
        /** Identificador estavel (usado pela interface, pelos testes e por encadeamentos). */
        String id();

        /** Nome para exibicao. */
        String nome();

        /** Duracao em segundos (de um ciclo, quando em loop). */
        double duracao();

        /** true = repete sem fim; o fim de um ciclo coincide com o inicio do seguinte. */
        boolean emLoop();

        /** Pose no tempo t, com 0 <= t <= duracao(). O giroCabeca devolvido e um acrescimo ao giro manual. */
        ConceitoRig2.ParametrosPose amostrar(double t);

        /** Distancia percorrida pelo corpo ate o tempo total t (para o chao rolar na caminhada no lugar). */
        default double deslocamento(double tTotal) { return 0; }

        /** Animacao que assume quando esta termina (so para animacoes sem loop); null = congela no fim. */
        default String proxima() { return null; }

        /** Pose-base em que o personagem fica ao fim (o laboratorio usa para atualizar a edicao manual). */
        ConceitoRig2.PoseBase baseFinal();
    }

    static final String IDLE_NEUTRO = "idle-neutro", IDLE_COMBATE = "idle-combate", CAMINHADA = "caminhada";
    static final String NEUTRA_PARA_COMBATE = "neutra-para-combate", COMBATE_PARA_NEUTRA = "combate-para-neutra";
    static final String[] CATALOGO = {IDLE_NEUTRO, IDLE_COMBATE, CAMINHADA, NEUTRA_PARA_COMBATE, COMBATE_PARA_NEUTRA};

    /**
     * Cria uma animacao pelo id. Para as transicoes, `inicio` e a pose de onde o personagem parte
     * (a pose mostrada no momento do pedido); null = o primeiro quadro do idle de origem.
     */
    static Animacao criar(String id, ConceitoRig2.ParametrosPose inicio) {
        return switch (id) {
            case IDLE_NEUTRO -> new IdleNeutro();
            case IDLE_COMBATE -> new IdleCombate();
            case CAMINHADA -> new Caminhada();
            case NEUTRA_PARA_COMBATE -> new Transicao(NEUTRA_PARA_COMBATE, "Transição neutra → combate",
                    inicio != null ? inicio : new IdleNeutro().amostrar(0), ConceitoRig2.PoseBase.COMBATE, IDLE_COMBATE);
            case COMBATE_PARA_NEUTRA -> new Transicao(COMBATE_PARA_NEUTRA, "Transição combate → neutra",
                    inicio != null ? inicio : new IdleCombate().amostrar(0), ConceitoRig2.PoseBase.NEUTRA, IDLE_NEUTRO);
            default -> throw new IllegalArgumentException("animacao desconhecida: " + id);
        };
    }

    static String nome(String id) { return criar(id, null).nome(); }

    // ===================================================================== utilitarios
    static final double TAU = Math.PI * 2;

    /** Interpolacao com velocidade e aceleracao zero nas pontas (smootherstep). */
    static double suavizar(double x) {
        double t = Math.max(0, Math.min(1, x));
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /** Trecho [a, b] do tempo normalizado, ja suavizado. */
    static double trecho(double x, double a, double b) { return suavizar((x - a) / (b - a)); }

    // ===================================================================== idle neutro
    /**
     * Repouso: respiracao (2 por ciclo de 7,2 s), ombros que sobem na inspiracao, peito que se
     * abre levemente (tronco inclina para tras), bracos que acompanham, transferencia minima de peso
     * e pequenos ajustes de cabeca. Tudo periodico no ciclo: o fim coincide com o inicio.
     */
    static final class IdleNeutro implements Animacao {
        static final double CICLO = 7.2, RESPIRACAO = 3.6;
        public String id() { return IDLE_NEUTRO; }
        public String nome() { return "Idle neutro (respiração)"; }
        public double duracao() { return CICLO; }
        public boolean emLoop() { return true; }
        public ConceitoRig2.PoseBase baseFinal() { return ConceitoRig2.PoseBase.NEUTRA; }

        public ConceitoRig2.ParametrosPose amostrar(double t) {
            ConceitoRig2.ParametrosPose p = ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.NEUTRA);
            double b = TAU * t / RESPIRACAO, s = TAU * t / CICLO;
            double r = .5 - .5 * Math.cos(b);            // 0 = expirado, 1 = inspirado
            p.ombros = 1.3 * r;
            p.inclinacao += -.7 * r;                      // o peito sobe: tronco recua um pouco
            p.quadrilX = 1.5 * Math.sin(s);               // peso passa devagar de uma perna para a outra
            p.baixar += .35 * (.5 - .5 * Math.cos(2 * s));
            for (int i = 0; i < 2; i++) {
                double lado = i == 0 ? -1 : 1;
                p.elevacao[i] += 1.6 * r + .5 * Math.sin(s + lado);
                p.avanco[i] += 1.2 * Math.sin(b - .6 + .3 * lado);
                p.flexao[i] += 3 * r;
            }
            p.giroCabeca = 1.5 * Math.sin(s + .8);
            return p;
        }
    }

    // ===================================================================== idle de combate
    /**
     * Guarda viva (ciclo de 2,4 s): o peso passa de uma perna para a outra, os joelhos "quicam" duas
     * vezes por ciclo, o tronco oscila pouco, os bracos fazem pequenos ajustes de guarda em fases
     * opostas e a respiracao continua. Os pes ficam plantados.
     */
    static final class IdleCombate implements Animacao {
        static final double CICLO = 2.4;
        public String id() { return IDLE_COMBATE; }
        public String nome() { return "Idle de combate (guarda)"; }
        public double duracao() { return CICLO; }
        public boolean emLoop() { return true; }
        public ConceitoRig2.PoseBase baseFinal() { return ConceitoRig2.PoseBase.COMBATE; }

        public ConceitoRig2.ParametrosPose amostrar(double t) {
            ConceitoRig2.ParametrosPose p = ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.COMBATE);
            double w = TAU * t / CICLO;
            p.quadrilX = 2.4 * Math.sin(w);
            p.quadrilZ = 1.2 * Math.sin(w + Math.PI / 2);
            p.baixar += 1.4 * (.5 - .5 * Math.cos(2 * w));
            p.inclinacao += .8 * Math.sin(w + .5);
            p.ombros = .9 * (.5 - .5 * Math.cos(w));
            for (int i = 0; i < 2; i++) {
                double f = i == 0 ? 1 : 1 + Math.PI;     // bracos em fases opostas
                p.avanco[i] += 3 * Math.sin(w + f);
                p.elevacao[i] += 2 * Math.cos(w + f);
                p.flexao[i] += 4 * Math.sin(w + f + 1);
            }
            p.giroCabeca = 1.2 * Math.sin(w + 2);
            return p;
        }
    }

    // ===================================================================== caminhada
    /**
     * Caminhada no lugar, ciclo de 1,1 s (dois passos). Cada perna alterna apoio (60% do ciclo) e
     * balanco (40%). No apoio o tornozelo recua em linha reta, na mesma velocidade com que o chao rola
     * sob o personagem (deslocamento), entao o pe de apoio nao escorrega em relacao ao chao. No fim do
     * apoio o calcanhar sobe (a ponta continua no chao); no balanco a perna avanca por uma curva de
     * Hermite que sai e chega com a velocidade do chao, levanta o pe e o pousa plano.
     * O quadril sobe no meio do apoio, desce no duplo apoio e desloca o peso para a perna de apoio;
     * os bracos balancam em oposicao as pernas, sem exagero.
     */
    static final class Caminhada implements Animacao {
        static final double CICLO = 1.1, APOIO = .6;
        static final double PERNA = ConceitoRig2.COXA + ConceitoRig2.CANELA;
        /** Passo e altura do pe relativos ao comprimento da perna (servem a outras proporcoes). */
        static final double PASSO = .55 * PERNA, ALTURA = .15 * PERNA;
        static final double CHAO_TORNOZELO = 258, LARGURA = 13, CALCANHAR = 22;
        /** Velocidade do chao: o pe de apoio percorre o passo inteiro durante o apoio. */
        static final double VELOCIDADE = PASSO / (APOIO * CICLO);

        public String id() { return CAMINHADA; }
        public String nome() { return "Caminhada (no lugar)"; }
        public double duracao() { return CICLO; }
        public boolean emLoop() { return true; }
        public ConceitoRig2.PoseBase baseFinal() { return ConceitoRig2.PoseBase.NEUTRA; }
        public double deslocamento(double tTotal) { return VELOCIDADE * tTotal; }

        /** Fase da perna i (0 = toque do calcanhar). A perna esquerda esta meio ciclo adiante. */
        static double fase(int i, double t) {
            double u = t / CICLO + (i == 0 ? 0 : .5);
            return u - Math.floor(u);
        }

        /** {z do tornozelo, y do tornozelo, inclinacao do pe em graus, 1 = apoio / 0 = balanco}. */
        static double[] pe(double u) {
            double meio = PASSO / 2;
            if (u < APOIO) {
                double z = meio - PASSO * (u / APOIO);
                double incl = CALCANHAR * trecho(u, APOIO * .8, APOIO);
                return new double[] {z, alturaComCalcanhar(incl), incl, 1};
            }
            double w = (u - APOIO) / (1 - APOIO);
            // Hermite: sai em -meio e chega em +meio, com a velocidade do chao nas duas pontas
            double m = -PASSO * (1 - APOIO) / APOIO;
            double h00 = 2 * w * w * w - 3 * w * w + 1, h10 = w * w * w - 2 * w * w + w;
            double h01 = -2 * w * w * w + 3 * w * w, h11 = w * w * w - w * w;
            double z = h00 * -meio + h10 * m + h01 * meio + h11 * m;
            // inclinacao: o calcanhar levantado volta a zero; depois a ponta sobe um pouco antes de pousar
            double incl = w < .35 ? CALCANHAR * (1 - trecho(w, 0, .35)) : -10 * Math.sin(Math.PI * (w - .35) / .65);
            double yFim = alturaComCalcanhar(CALCANHAR);
            double y = CHAO_TORNOZELO - (CHAO_TORNOZELO - yFim) * (1 - trecho(w, 0, .35))
                    - ALTURA * Math.pow(Math.sin(Math.PI * w), 2);
            return new double[] {z, y, incl, 0};
        }

        /** Altura do tornozelo que mantem a ponta do pe no chao com o calcanhar levantado. */
        static double alturaComCalcanhar(double graus) {
            double r = Math.toRadians(graus);
            return CHAO_TORNOZELO + 7 - 17 * Math.sin(r) - 7 * Math.cos(r);
        }

        public ConceitoRig2.ParametrosPose amostrar(double t) {
            ConceitoRig2.ParametrosPose p = ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.NEUTRA);
            double fi = t / CICLO;
            // quadril: alto no meio do apoio (fase .3 e .8), baixo no duplo apoio; peso sobre a perna de apoio
            p.baixar = 1 - 2.5 * Math.cos(2 * TAU * (fi - .3));
            p.quadrilX = -2.2 * Math.cos(TAU * (fi - .3));
            p.inclinacao = 3 + .8 * Math.cos(2 * TAU * (fi - .3));
            p.ombros = .4 * (.5 - .5 * Math.cos(2 * TAU * (fi - .3)));
            double[] zPerna = new double[2];
            for (int i = 0; i < 2; i++) {
                double lado = i == 0 ? -1 : 1;
                double[] e = pe(fase(i, t));
                zPerna[i] = e[0];
                p.pe[i] = new ConceitoRig2.V(lado * LARGURA, e[1], e[0]);
                p.poloPerna[i] = new ConceitoRig2.V(lado * .15, 0, 1);
                p.guinadaPe[i] = lado * 6;
                p.inclinacaoPe[i] = e[2];
            }
            // bracos em oposicao: o braco direito vai a frente quando a perna esquerda esta a frente
            for (int i = 0; i < 2; i++) {
                double a = zPerna[1 - i] / (PASSO / 2);
                p.avanco[i] = 4 + 13 * a;
                p.flexao[i] = 20 + 9 * a;
            }
            return p;
        }
    }

    // ===================================================================== transicoes de postura
    /**
     * Transicao entre posturas, por canais com tempos diferentes: na ida para o combate os joelhos
     * dobram primeiro, depois quadril e tronco, por ultimo os bracos sobem para a guarda; na volta, os
     * bracos descem primeiro e as pernas por ultimo. Cada pe da um pequeno passo (levanta e pousa) para
     * mudar de base, um de cada vez, com o peso indo para o pe que fica no chao, em vez de deslizar.
     * Termina exatamente no primeiro quadro do idle de destino, que assume sem salto.
     */
    static final class Transicao implements Animacao {
        final String id, nome, proxima;
        final ConceitoRig2.ParametrosPose inicio, fim;
        final ConceitoRig2.PoseBase destino;
        final boolean paraCombate;
        final double duracao;

        Transicao(String id, String nome, ConceitoRig2.ParametrosPose inicio, ConceitoRig2.PoseBase destino, String proxima) {
            this.id = id;
            this.nome = nome;
            this.inicio = inicio.copia();
            this.inicio.pernas = ConceitoRig2.PresetPernas.DA_POSE;
            this.destino = destino;
            this.proxima = proxima;
            this.fim = criar(proxima, null).amostrar(0);
            this.paraCombate = destino == ConceitoRig2.PoseBase.COMBATE;
            this.duracao = paraCombate ? 1.0 : 1.1;
        }

        public String id() { return id; }
        public String nome() { return nome; }
        public double duracao() { return duracao; }
        public boolean emLoop() { return false; }
        public String proxima() { return proxima; }
        public ConceitoRig2.PoseBase baseFinal() { return destino; }

        public ConceitoRig2.ParametrosPose amostrar(double t) {
            double x = Math.max(0, Math.min(1, t / duracao));
            double kPernas, kCorpo, kBracos, kPe0, kPe1;
            if (paraCombate) {
                kPernas = trecho(x, 0, .65); kCorpo = trecho(x, .1, .8); kBracos = trecho(x, .3, 1);
                kPe0 = trecho(x, .05, .45); kPe1 = trecho(x, .35, .75);
            } else {
                kBracos = trecho(x, 0, .55); kCorpo = trecho(x, .15, .8); kPernas = trecho(x, .3, 1);
                kPe0 = trecho(x, .35, .7); kPe1 = trecho(x, .6, .95);
            }
            ConceitoRig2.ParametrosPose p = ConceitoRig2.ParametrosPose.interpolar(inicio, fim, kCorpo, kBracos, kPernas);
            // passos: cada pe vai da posicao inicial para a final no seu proprio trecho, com um arco
            double[] kPe = {kPe0, kPe1};
            double alivio = 0;
            for (int i = 0; i < 2; i++) {
                ConceitoRig2.V a = inicio.pe[i], b = fim.pe[i];
                double k = kPe[i], arco = Math.sin(Math.PI * k);
                double distancia = b.menos(a).tam();
                double altura = Math.min(9, distancia * .35) * arco;
                p.pe[i] = ConceitoRig2.mixV(a, b, k).menos(new ConceitoRig2.V(0, altura, 0));
                p.guinadaPe[i] = ConceitoRig2.mix(inicio.guinadaPe[i], fim.guinadaPe[i], k);
                // o peso vai para o pe que fica no chao enquanto este esta no ar
                alivio += (i == 0 ? 1 : -1) * 3 * arco * Math.min(1, distancia / 10);
            }
            p.quadrilX += alivio;
            if (x >= 1) {
                ConceitoRig2.ParametrosPose r = fim.copia();
                r.base = destino;
                return r;
            }
            return p;
        }
    }

    // ===================================================================== reprodutor
    /**
     * Relogio de reproducao. Nao sabe nada de Swing: quem o usa chama avancar(segundos reais).
     * Prioridade dos controles (documentada no README):
     *  - a perspectiva (guinada da camera) e sempre manual;
     *  - o giro da cabeca e manual e a animacao soma apenas um pequeno acrescimo;
     *  - corpo, bracos e pernas pertencem a animacao enquanto ela esta tocando ou pausada;
     *  - ao parar, volta a pose manual (a mesma de antes, ou a pose-base nova depois de uma transicao).
     */
    static final class Reprodutor {
        enum Estado { PARADO, TOCANDO, PAUSADO }

        /** Maior passo aceito por chamada: uma janela travada nao faz o personagem "teleportar". */
        static final double PASSO_MAXIMO = .25;

        Animacao atual;
        double tempo;          // tempo total desde o inicio da animacao atual (segundos de animacao)
        double velocidade = 1;
        Estado estado = Estado.PARADO;

        void tocar(Animacao a) { atual = a; tempo = 0; estado = Estado.TOCANDO; }
        void pausar() { if (estado == Estado.TOCANDO) estado = Estado.PAUSADO; }
        void continuar() { if (estado == Estado.PAUSADO && atual != null) estado = Estado.TOCANDO; }
        void parar() { atual = null; tempo = 0; estado = Estado.PARADO; }
        void reiniciar() { if (atual != null) { tempo = 0; estado = Estado.TOCANDO; } }
        boolean ativo() { return estado != Estado.PARADO && atual != null; }

        /**
         * Avanca dt segundos reais (multiplicados pela velocidade). Animacoes sem loop passam para a
         * `proxima` levando o tempo que sobrou, sem perder o quadro. Retorna true se a animacao mudou.
         */
        boolean avancar(double dt) {
            if (estado != Estado.TOCANDO || atual == null || !(dt > 0)) return false;
            tempo += Math.min(dt, PASSO_MAXIMO) * velocidade;
            boolean trocou = false;
            while (!atual.emLoop() && tempo >= atual.duracao()) {
                String prox = atual.proxima();
                if (prox == null) { tempo = atual.duracao(); estado = Estado.PAUSADO; break; }
                tempo -= atual.duracao();
                atual = criar(prox, null);
                trocou = true;
            }
            return trocou;
        }

        /** Tempo dentro do ciclo atual. */
        double tempoNoCiclo() {
            if (atual == null) return 0;
            if (!atual.emLoop()) return Math.min(tempo, atual.duracao());
            double d = atual.duracao();
            return tempo - Math.floor(tempo / d) * d;
        }

        /** Numero do ciclo atual (1 = primeiro). */
        int ciclo() { return atual == null || !atual.emLoop() ? 1 : (int) Math.floor(tempo / atual.duracao()) + 1; }

        /** Pose a desenhar: a da animacao com o giro manual da cabeca somado; sem animacao, a manual. */
        ConceitoRig2.ParametrosPose pose(ConceitoRig2.ParametrosPose manual) {
            if (!ativo()) return manual;
            ConceitoRig2.ParametrosPose p = atual.amostrar(tempoNoCiclo());
            p.giroCabeca = ConceitoRig2.limitar(p.giroCabeca + manual.giroCabeca, -ConceitoRig2.GIRO_CABECA_MAX, ConceitoRig2.GIRO_CABECA_MAX);
            return p;
        }

        double deslocamento() { return atual == null ? 0 : atual.deslocamento(tempo); }
    }

    // ===================================================================== verificacoes (6.2)
    static void checar(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }

    /** Comprimentos fixos de todos os ossos, coordenadas validas e pes alcancados pelo IK. */
    static void checarCorpo(ConceitoRig2.ParametrosPose p, ConceitoRig2.Corpo c, String onde) {
        double eps = 1e-6;
        for (ConceitoRig2.V v : ConceitoRig2.juntas(c))
            checar(Double.isFinite(v.x()) && Double.isFinite(v.y()) && Double.isFinite(v.z()), "coordenada invalida em " + onde);
        checar(Math.abs(c.pescoco.menos(c.transformar(new ConceitoRig2.V(0, ConceitoRig2.PIVO_CINTURA, 0))).tam()
                - (ConceitoRig2.PIVO_CINTURA - ConceitoRig2.PESCOCO_Y)) < eps, "tronco em " + onde);
        checar(Math.abs(c.cabeca.menos(c.pescoco).tam() - Math.hypot(ConceitoRig2.PESCOCO_Y - ConceitoRig2.CABECA_Y, c.cabecaZ)) < eps,
                "cabeca solta do pescoco em " + onde);
        for (int i = 0; i < 2; i++) {
            checar(Math.abs(c.cotovelo[i].menos(c.ombro[i]).tam() - ConceitoRig2.BRACO) < eps, "braco em " + onde);
            checar(Math.abs(c.mao[i].menos(c.cotovelo[i]).tam() - ConceitoRig2.ANTEBRACO) < eps, "antebraco em " + onde);
            checar(Math.abs(c.joelho[i].menos(c.quadril[i]).tam() - ConceitoRig2.COXA) < eps, "coxa em " + onde);
            checar(Math.abs(c.tornozelo[i].menos(c.joelho[i]).tam() - ConceitoRig2.CANELA) < eps, "canela em " + onde);
            checar(c.tornozelo[i].menos(p.pe[i]).tam() < 1e-6, "pe nao alcancado pela perna (flutuando) em " + onde);
            checar(c.tornozelo[i].y() <= Caminhada.CHAO_TORNOZELO + eps, "pe abaixo do chao em " + onde);
        }
    }

    static double maiorDiferenca(List<ConceitoRig2.V> a, List<ConceitoRig2.V> b) {
        double m = 0;
        for (int k = 0; k < a.size(); k++) m = Math.max(m, a.get(k).menos(b.get(k)).tam());
        return m;
    }

    static List<ConceitoRig2.V> juntasEm(Animacao a, double t) { return ConceitoRig2.juntas(ConceitoRig2.montarPose(a.amostrar(t))); }

    static List<String> verificar() {
        List<String> rel = new ArrayList<>();
        double dt = 1 / 120.0;
        for (String id : CATALOGO) {
            Animacao a = criar(id, null);
            double d = a.duracao(), total = a.emLoop() ? 3 * d : d;
            List<ConceitoRig2.V> ant = null;
            double maiorPasso = 0;
            int quadros = 0;
            for (double t = 0; t <= total + 1e-9; t += dt) {
                double tc = a.emLoop() ? t - Math.floor(t / d) * d : Math.min(t, d);
                ConceitoRig2.ParametrosPose p = a.amostrar(tc);
                ConceitoRig2.Corpo c = ConceitoRig2.montarPose(p);
                checarCorpo(p, c, id + " t=" + String.format(Locale.ROOT, "%.3f", t));
                List<ConceitoRig2.V> js = ConceitoRig2.juntas(c);
                if (ant != null) maiorPasso = Math.max(maiorPasso, maiorDiferenca(ant, js));
                ant = js;
                quadros++;
            }
            String extra = "";
            if (a.emLoop()) {
                double fecho = maiorDiferenca(juntasEm(a, 0), juntasEm(a, d));
                checar(fecho < 1e-6, id + ": o fim do ciclo nao coincide com o inicio (" + fecho + ")");
                // velocidade dos dois lados da emenda
                // diferencas finitas de um lado e do outro: o erro e da ordem de h x aceleracao
                // (a caminhada tem troca de aceleracao no toque do calcanhar, nao de velocidade)
                double h = 1e-6, saltoVel = 0;
                List<ConceitoRig2.V> f0 = juntasEm(a, d - h), f1 = juntasEm(a, d), i0 = juntasEm(a, 0), i1 = juntasEm(a, h);
                for (int k = 0; k < f0.size(); k++)
                    saltoVel = Math.max(saltoVel, f1.get(k).menos(f0.get(k)).menos(i1.get(k).menos(i0.get(k))).tam() / h);
                checar(saltoVel < 1e-2, id + ": a velocidade muda na emenda do loop (" + saltoVel + ")");
                // repeticao: 40 ciclos depois, nada acumulou
                Reprodutor r = new Reprodutor();
                r.tocar(a);
                for (int k = 0; k < 40 * Math.round(d * 60); k++) r.avancar(1 / 60.0);
                double desvio = maiorDiferenca(ConceitoRig2.juntas(ConceitoRig2.montarPose(r.pose(ConceitoRig2.ParametrosPose.padrao(a.baseFinal())))),
                        ConceitoRig2.juntas(ConceitoRig2.montarPose(withGiro(a.amostrar(r.tempoNoCiclo())))));
                checar(desvio < 1e-6, id + ": desvio apos 40 ciclos (" + desvio + ")");
                extra = String.format(Locale.ROOT, "; emenda do loop: posicao %.1e, velocidade %.1e; 40 ciclos sem desvio", fecho, saltoVel);
            }
            rel.add(String.format(Locale.ROOT, "OK %s: %d quadros (1/120 s) com ossos constantes, cabeca presa, pes alcancados e acima do chao;"
                    + " maior passo de junta %.2f por quadro%s.", id, quadros, maiorPasso, extra));
        }

        // caminhada: apoio sem escorregar, alternancia, joelhos e tornozelos participando, bracos em oposicao
        Caminhada cam = new Caminhada();
        double maxEscorrega = 0, maxAltura = 0, joelhoMin = 999, joelhoMax = -999, peMin = 999, peMax = -999;
        int semApoio = 0, trocas0 = 0;
        boolean apoioAnt = true;
        double somaCorrel = 0;
        for (double t = 0; t < Caminhada.CICLO; t += 1 / 240.0) {
            double h = 1e-4;
            int apoios = 0;
            for (int i = 0; i < 2; i++) {
                double u = Caminhada.fase(i, t);
                double[] e = Caminhada.pe(u);
                if (u < Caminhada.APOIO * .8 - .01 && u > .01) {
                    double[] e2 = Caminhada.pe(Caminhada.fase(i, t + h));
                    double vel = (e2[0] - e[0]) / h;
                    maxEscorrega = Math.max(maxEscorrega, Math.abs(vel + Caminhada.VELOCIDADE));
                    maxAltura = Math.max(maxAltura, Math.abs(e[1] - Caminhada.CHAO_TORNOZELO));
                }
                if (u < Caminhada.APOIO) {
                    apoios++;
                    // com o calcanhar levantado a ponta do pe continua no chao
                    double r = Math.toRadians(e[2]);
                    maxAltura = Math.max(maxAltura, Math.abs(e[1] + 17 * Math.sin(r) + 7 * Math.cos(r) - (Caminhada.CHAO_TORNOZELO + 7)));
                }
                if (i == 0) {
                    boolean apoio = u < Caminhada.APOIO;
                    if (apoio != apoioAnt) trocas0++;
                    apoioAnt = apoio;
                }
                peMin = Math.min(peMin, e[2]);
                peMax = Math.max(peMax, e[2]);
            }
            if (apoios == 0) semApoio++;
            ConceitoRig2.Corpo c = ConceitoRig2.montarPose(cam.amostrar(t));
            for (int i = 0; i < 2; i++) {
                ConceitoRig2.V coxa = c.joelho[i].menos(c.quadril[i]), canela = c.tornozelo[i].menos(c.joelho[i]);
                double ang = Math.toDegrees(Math.acos(coxa.ponto(canela) / (coxa.tam() * canela.tam())));
                joelhoMin = Math.min(joelhoMin, ang);
                joelhoMax = Math.max(joelhoMax, ang);
            }
            ConceitoRig2.ParametrosPose p = cam.amostrar(t);
            somaCorrel += (p.avanco[0] - 4) * p.pe[1].z();
        }
        checar(maxEscorrega < 1e-6, "caminhada: pe de apoio escorrega (" + maxEscorrega + ")");
        checar(maxAltura < 1e-6, "caminhada: pe de apoio fora do chao (" + maxAltura + ")");
        checar(semApoio == 0, "caminhada: instantes sem nenhum pe no chao");
        // dentro de [0, ciclo): apoio -> balanco uma vez; o balanco -> apoio e o proprio reinicio do ciclo
        checar(trocas0 == 1 && !apoioAnt, "caminhada: cada perna deveria apoiar e balancar uma vez por ciclo (" + trocas0 + ")");
        checar(joelhoMax - joelhoMin > 25, "caminhada: joelhos quase nao dobram");
        checar(peMax - peMin > 20, "caminhada: tornozelo quase nao articula");
        checar(somaCorrel > 0, "caminhada: bracos nao estao em oposicao as pernas");
        rel.add(String.format(Locale.ROOT, "OK caminhada: pe de apoio acompanha o chao (erro %.1e) e fica no chao (erro %.1e), inclusive com o"
                + " calcanhar levantado; sempre ha um pe apoiado; pernas alternam; joelho dobra de %.0f a %.0f graus; pe inclina de %.0f a %.0f"
                + " graus; bracos em oposicao as pernas.", maxEscorrega, maxAltura, joelhoMin, joelhoMax, peMin, peMax));

        // transicoes: comecam na pose de partida, terminam no primeiro quadro do idle e encadeiam sem salto
        for (String id : new String[] {NEUTRA_PARA_COMBATE, COMBATE_PARA_NEUTRA}) {
            Transicao tr = (Transicao) criar(id, null);
            double ini = maiorDiferenca(ConceitoRig2.juntas(ConceitoRig2.montarPose(tr.inicio.copia())), juntasEm(tr, 0));
            double fim = maiorDiferenca(juntasEm(tr, tr.duracao()), juntasEm(criar(tr.proxima(), null), 0));
            checar(ini < 1e-6, id + ": nao comeca na pose de partida (" + ini + ")");
            checar(fim < 1e-6, id + ": nao termina no primeiro quadro do idle (" + fim + ")");
            Reprodutor r = new Reprodutor();
            r.tocar(tr);
            List<ConceitoRig2.V> antes = ConceitoRig2.juntas(ConceitoRig2.montarPose(r.pose(ConceitoRig2.ParametrosPose.padrao(tr.baseFinal()))));
            double maior = 0;
            String ultimo = r.atual.id();
            for (int k = 0; k < 120; k++) {
                r.avancar(1 / 60.0);
                List<ConceitoRig2.V> agora = ConceitoRig2.juntas(ConceitoRig2.montarPose(r.pose(ConceitoRig2.ParametrosPose.padrao(tr.baseFinal()))));
                maior = Math.max(maior, maiorDiferenca(antes, agora));
                antes = agora;
                ultimo = r.atual.id();
            }
            checar(ultimo.equals(tr.proxima()), id + ": nao passou para " + tr.proxima());
            checar(maior < 6, id + ": salto entre quadros (" + maior + ")");
            rel.add(String.format(Locale.ROOT, "OK %s: comeca na pose de partida, termina exatamente no 1o quadro de %s e o reprodutor"
                    + " encadeia sem salto (maior passo de junta %.2f a 60 quadros/s).", id, tr.proxima(), maior));
        }
        // transicao partindo de uma pose qualquer (ex.: no meio da caminhada)
        ConceitoRig2.ParametrosPose meio = new Caminhada().amostrar(.37);
        Animacao tr = criar(NEUTRA_PARA_COMBATE, meio);
        checar(maiorDiferenca(ConceitoRig2.juntas(ConceitoRig2.montarPose(meio.copia())), juntasEm(tr, 0)) < 1e-6,
                "transicao a partir da caminhada nao parte da pose atual");
        rel.add("OK: uma transicao pedida no meio de outra animacao parte da pose mostrada naquele instante.");

        // reprodutor: baseado em tempo, independente da taxa de quadros
        Reprodutor a = new Reprodutor(), b = new Reprodutor();
        a.tocar(new Caminhada());
        b.tocar(new Caminhada());
        Random rnd = new Random(7);
        double ta = 0, tb = 0;
        for (int k = 0; k < 600; k++) { a.avancar(1 / 60.0); ta += 1 / 60.0; }
        while (tb < ta - 1e-9) { double passo = Math.min(.01 + rnd.nextDouble() * .03, ta - tb); b.avancar(passo); tb += passo; }
        double dif = maiorDiferenca(ConceitoRig2.juntas(ConceitoRig2.montarPose(a.pose(ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.NEUTRA)))),
                ConceitoRig2.juntas(ConceitoRig2.montarPose(b.pose(ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.NEUTRA)))));
        checar(dif < 1e-6, "o resultado depende da taxa de quadros (" + dif + ")");
        double tPausa = a.tempo;
        a.pausar(); a.avancar(.5);
        checar(a.tempo == tPausa, "pausado, o tempo andou");
        a.continuar(); a.avancar(.1);
        checar(Math.abs(a.tempo - tPausa - .1) < 1e-12, "continuar nao retomou");
        a.velocidade = 2; a.avancar(.1);
        checar(Math.abs(a.tempo - tPausa - .3) < 1e-12, "velocidade 2x nao dobrou o tempo");
        a.reiniciar();
        checar(a.tempo == 0 && a.estado == Reprodutor.Estado.TOCANDO, "reiniciar falhou");
        ConceitoRig2.ParametrosPose manual = ConceitoRig2.ParametrosPose.padrao(ConceitoRig2.PoseBase.COMBATE);
        manual.giroCabeca = 20;
        checar(Math.abs(a.pose(manual).giroCabeca - (20 + new Caminhada().amostrar(0).giroCabeca)) < 1e-12, "giro manual da cabeca nao foi somado");
        a.parar();
        checar(a.pose(manual) == manual, "parar nao devolveu a pose manual");
        a.velocidade = 1;
        a.tocar(new Caminhada());
        a.avancar(10);
        checar(Math.abs(a.tempo - Reprodutor.PASSO_MAXIMO) < 1e-12, "passo maximo nao aplicado");
        rel.add("OK reprodutor: 60 quadros/s e quadros irregulares chegam a mesma pose; pausar, continuar, velocidade 2x, reiniciar e"
                + " parar (volta a pose manual) funcionam; o giro manual da cabeca e somado; passo maximo de 0,25 s por chamada.");

        // continuidade visual no tempo (desenho real, 30 quadros/s, 40 graus), incluindo a emenda do loop
        for (String id : CATALOGO) {
            Animacao an = criar(id, null);
            int n = (int) Math.round(an.duracao() * 30);
            List<Double> difs = new ArrayList<>();
            java.awt.image.BufferedImage ant = null, primeiro = null;
            for (int k = 0; k <= n; k++) {
                double t = Math.min(an.duracao(), k / 30.0);
                java.awt.image.BufferedImage q = ConceitoRig2.quadro(ConceitoRig2.montarPose(an.amostrar(t)), 40, 200, 300, .95, false);
                if (ant != null) difs.add(ConceitoRig2.diferencaMedia(ant, q));
                if (primeiro == null) primeiro = q;
                ant = q;
            }
            List<Double> ord = new ArrayList<>(difs);
            Collections.sort(ord);
            double mediana = ord.get(ord.size() / 2), maior = ord.get(ord.size() - 1);
            String emenda = "";
            if (an.emLoop()) {
                java.awt.image.BufferedImage seguinte = ConceitoRig2.quadro(ConceitoRig2.montarPose(an.amostrar(1 / 30.0)), 40, 200, 300, .95, false);
                double e = ConceitoRig2.diferencaMedia(ant, seguinte); // ultimo quadro (= t duracao) -> segundo quadro do ciclo seguinte
                checar(e <= Math.max(3 * mediana, .05), id + ": salto visivel na emenda do loop (" + e + ")");
                emenda = String.format(Locale.ROOT, "; emenda do loop %.3f", e);
            }
            checar(maior <= Math.max(3 * mediana, .3), id + ": salto visivel entre quadros (" + maior + " vs mediana " + mediana + ")");
            rel.add(String.format(Locale.ROOT, "OK continuidade visual %s (30 quadros/s, 40 graus): maior diferenca %.3f, mediana %.3f%s.",
                    id, maior, mediana, emenda));
        }
        return rel;
    }

    static ConceitoRig2.ParametrosPose withGiro(ConceitoRig2.ParametrosPose p) {
        p.giroCabeca = ConceitoRig2.limitar(p.giroCabeca, -ConceitoRig2.GIRO_CABECA_MAX, ConceitoRig2.GIRO_CABECA_MAX);
        return p;
    }

    public static void main(String[] args) {
        verificar().forEach(System.out::println);
    }
}
