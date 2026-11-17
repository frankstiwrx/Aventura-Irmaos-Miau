package com.irmaosmiau.animacao;

import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.MotorSolo.TipoEfeitoVisual;

import java.util.HashMap;
import java.util.Map;

/**
 * Entrega 5Q.3: animacoes PROPRIAS de tecnicas, para quem aplica e quem recebe,
 * por cima da pose da postura/posicao (o mesmo pipeline do AnimadorSolo).
 *
 * Cada animacao e uma lista de quadros-chave no "tempo" q (0-1) da tecnica, com
 * deslocamentos sobre a pose-base no formato do MiauStudio (30 valores) e, nos
 * indices 30/31, um deslocamento visual do corpo (px locais; +x = para a frente).
 * Entre dois quadros a interpolacao e suave (smoothstep). O desenho SO reflete o
 * estado do MotorSolo (efeitoVisual): nunca decide nada.
 *
 * Rig (MiauStudio): 4/5 = cintura x/y; 6 = rotacao da cintura; 7/8 = tronco x/y;
 * 9 = rotacao do tronco; 10 = rotacao global; 11 = rotacao da cabeca; 12/13 =
 * cabeca x/y; 14/15 = cotovelo de tras; 16/17 = mao de tras; 18/19 = cotovelo da
 * frente; 20/21 = mao da frente; 22/23 = joelho de tras; 24/25 = pe de tras;
 * 26/27 = joelho da frente; 28/29 = pe da frente (x/y em px locais). Deitado (rotacao
 * global -88): +x local aponta para cima na tela e +y local para o adversario.
 *
 * Tempo q: finalizacoes 0-0,5 = entrada, 0,5-1 = pressao 0-100%; empurrao: 0-0,5 =
 * preparacao ate o empurrao chegar (marco do motor), 0,5-1 = recuperacao.
 */
public final class AnimacoesTecnicas {

    /** Um quadro-chave: tempo e pares (indice, valor). Indice 30 = dx, 31 = dy. */
    record Quadro(double t, int[] pares) {
    }

    private static final Map<String, Quadro[]> ANIMACOES = new HashMap<>();
    /** Reacao extra de quem defende (e recuo de quem ataca) logo apos uma defesa na disputa. */
    private static final Map<String, int[]> PULSO_DEFESA = new HashMap<>();

    private static Quadro q(double t, int... pares) {
        return new Quadro(t, pares);
    }

    private static void def(String chave, Quadro... quadros) {
        ANIMACOES.put(chave, quadros);
    }

    static {
        // ---------------- 1. EMPURRAR (em pe) e SER EMPURRADO
        def("empurrao:ATACA",
                q(0.00),
                q(0.30, 9, -8, 6, -4, 14, -10, 16, -14, 18, -12, 20, -16, 22, 6, 26, 6, 13, 2),      // carrega o peso atras
                q(0.50, 9, 10, 6, 6, 14, 26, 15, -14, 16, 40, 17, -28, 18, 24, 19, -16, 20, 38, 21, -24,
                        28, 18, 24, -12, 30, 6),                                                     // estende os bracos e da o passo
                q(0.72, 9, 8, 14, 12, 16, 22, 17, -8, 18, 12, 20, 22, 21, -6, 28, 12, 24, -6, 30, 4),
                q(1.00));
        def("empurrao:RECEBE",
                q(0.00),
                q(0.14, 9, -22, 11, -12, 6, -6, 14, 8, 15, -16, 16, 14, 17, -28, 18, 6, 19, -16, 20, 10, 21, -30,
                        24, -8, 28, -4),                                                             // impacto: tronco para tras
                q(0.42, 9, -14, 11, -6, 16, -18, 17, -24, 20, 18, 21, -20, 22, -12, 24, -24, 25, -6, 29, -10), // passo de tras
                q(0.72, 9, -5, 16, -8, 17, -8, 20, 6, 22, -4, 24, -10),                              // recupera a base
                q(1.00));
        alias("derrubada_forcada:ATACA", "empurrao:ATACA");

        // ---------------- 2. AFASTAR NO CHAO (pernas empurram) e SER AFASTADO
        def("afastar_da_guarda:ATACA",
                q(0.00),
                q(0.35, 6, 8, 9, 6, 22, 30, 23, -26, 24, 26, 25, -40, 26, 32, 27, -26, 28, 28, 29, -40,
                        16, 8, 17, -6, 20, 8, 21, -6),                                               // recolhe os joelhos
                q(0.75, 6, -4, 22, 8, 23, 18, 24, 4, 25, 40, 26, 8, 27, 18, 28, 4, 29, 42, 16, 14, 20, 14),   // estende: empurra
                q(1.00, 22, 6, 23, 18, 24, 2, 25, 40, 26, 6, 27, 18, 28, 2, 29, 42, 16, 10, 20, 10));
        def("afastar_da_guarda:RECEBE",
                q(0.00),
                q(0.35, 9, 6),
                q(0.75, 9, -18, 11, -8, 18, -8, 19, -18, 20, -10, 21, -20, 16, -10, 17, -20, 30, -8),   // empurrado para tras
                q(1.00, 9, -20, 11, -8, 20, -12, 21, -24, 16, -12, 17, -24, 30, -10));

        // ---------------- 3. ARMLOCK DA GUARDA FECHADA
        def("armlock@guarda:ATACA",
                q(0.00),
                q(0.25, 6, 10, 16, 14, 17, -6, 20, 10, 21, -10),                                     // segura o braco, angula o quadril
                q(0.50, 10, 10, 6, 14, 26, 70, 27, -26, 28, 92, 29, -40, 22, 20, 24, 26, 16, 20, 17, -14, 20, 16,
                        21, -16),                                                                    // perna sobre a cabeca
                q(1.00, 10, 14, 5, -6, 6, 18, 26, 76, 27, -30, 28, 100, 29, -46, 22, 22, 24, 30, 16, 24, 17, -18,
                        20, 20, 21, -20));
        def("armlock@guarda:RECEBE",
                q(0.00),
                q(0.25, 9, 10, 18, 16, 19, 20, 20, 22, 21, 30),                                      // braco dentro da guarda
                q(0.50, 9, 12, 11, 22, 18, 20, 19, 26, 20, 30, 21, 40, 30, 2),                       // postura quebrada, braco esticado
                q(1.00, 9, 14, 11, 26, 18, 22, 19, 30, 20, 34, 21, 46, 16, 18, 17, 30, 30, 2));
        PULSO_DEFESA.put("armlock@guarda:RECEBE", new int[] {9, -16, 16, 26, 17, 34});            // empilha e segura a mao
        PULSO_DEFESA.put("armlock@guarda:ATACA", new int[] {10, -6, 28, -10});

        // ---------------- 4. AMERICANA DA MONTADA
        def("americana@montada:ATACA",
                q(0.00),
                q(0.25, 9, 18, 20, 18, 21, 36, 16, 12, 17, 38),                                      // procura o punho
                q(0.50, 9, 32, 11, 10, 14, 10, 15, 20, 16, 18, 17, 54, 18, 10, 19, 24, 20, 24, 21, 52), // prende o braco no chao
                q(1.00, 9, 38, 6, 6, 14, 12, 15, 22, 16, 22, 17, 56, 18, 12, 19, 26, 20, 20, 21, 56));
        def("americana@montada:RECEBE",
                q(0.00),
                q(0.50, 18, 18, 19, -10, 20, 14, 21, -34),                                           // braco dobrado ao lado da cabeca
                q(1.00, 11, -10, 18, 20, 19, -14, 20, 10, 21, -44));
        PULSO_DEFESA.put("americana@montada:RECEBE", new int[] {16, 30, 17, -6, 6, 8});           // empurra o peito, gira o quadril
        PULSO_DEFESA.put("americana@montada:ATACA", new int[] {9, -10});

        // ---------------- 5. MATA-LEAO DAS COSTAS
        def("mata_leao@costas:ATACA",
                q(0.00),
                q(0.25, 9, 8, 18, -4, 19, -22, 20, -12, 21, -34),                                    // braco passa pelo pescoco
                q(0.50, 9, 10, 11, 8, 12, 8, 13, -10, 14, 28, 15, -46, 16, 46, 17, -68, 18, -6, 19, -30, 20, -20,
                        21, -48, 31, -10),                                                           // trava; a cabeca aparece no ombro
                q(1.00, 9, 2, 6, -6, 11, 4, 12, 10, 13, -12, 14, 30, 15, -50, 16, 48, 17, -72, 18, -8, 19, -32,
                        20, -24, 21, -50, 31, -12));
        def("mata_leao@costas:RECEBE",
                q(0.00),
                q(0.50, 11, -8, 16, 20, 17, -48, 20, 10, 21, -46),                                   // maos no braco que estrangula
                q(1.00, 9, -6, 11, -16, 16, 16, 17, -54, 20, 8, 21, -50));
        PULSO_DEFESA.put("mata_leao@costas:RECEBE", new int[] {9, 10, 11, 6, 16, 26, 17, -40});   // queixo baixo, puxa o braco
        PULSO_DEFESA.put("mata_leao@costas:ATACA", new int[] {9, -4, 20, -6});

        // ---------------- 6. BERIMBOLO (De La Riva -> costas)
        def("berimbolo:ATACA",
                q(0.00),
                q(0.25, 6, 20, 10, -10, 26, 20, 28, 20),                                             // vira o quadril por baixo
                q(0.55, 10, -70, 5, -6, 22, 20, 24, 24, 26, 30, 27, -20, 28, 36, 29, -30, 30, 18),   // inversao (de cabeca para baixo)
                q(0.85, 10, -20, 9, 20, 30, 42),                                                     // gira e sobe por tras
                q(1.00, 10, 30, 9, 16, 30, 50));
        def("berimbolo:RECEBE",
                q(0.00),
                q(0.30, 9, 10, 28, 10),
                q(0.60, 9, 24, 11, 10, 16, 14, 20, 14, 29, -16),                                     // perde a base: pe da frente sobe
                q(0.90, 9, 30, 22, 10, 23, 10, 31, 8));                                              // o joelho desce
    }

    private AnimacoesTecnicas() {
    }

    private static void alias(String nova, String existente) {
        ANIMACOES.put(nova, ANIMACOES.get(existente));
    }

    /** Ha animacao propria para esta chave ("tecnica:ATACA" / "tecnica:RECEBE")? */
    public static boolean tem(String chave) {
        return chave != null && ANIMACOES.containsKey(chave);
    }

    /** Chave da animacao do efeito (null = sem animacao propria: o generico da 5Q desenha). */
    public static String chave(MotorSolo.EfeitoVisual e) {
        if (e == null || e.tecnica() == null) {
            return null;
        }
        String papel = switch (e.tipo()) {
            case EMPURRANDO, AFASTANDO, TECNICA_ATAQUE, FINALIZANDO -> ":ATACA";
            case EMPURRADO, AFASTADO, TECNICA_RECEBE, DEFENDENDO_FINALIZACAO -> ":RECEBE";
            case LIBERANDO -> "";
            default -> null;
        };
        if (papel == null) {
            return null;
        }
        String tecnica = e.tipo() == TipoEfeitoVisual.EMPURRADO ? "empurrao" : e.tecnica();
        String c = tecnica + papel;
        return tem(c) ? c : null;
    }

    /** Tempo q da animacao a partir do progresso e do marco do motor. */
    static double tempo(MotorSolo.EfeitoVisual e) {
        double p = Math.max(0, Math.min(1, e.progresso()));
        double m = e.marco();
        if (m >= 1.0 || m <= 0.0 || e.tipo() == TipoEfeitoVisual.FINALIZANDO
                || e.tipo() == TipoEfeitoVisual.DEFENDENDO_FINALIZACAO) {
            return p;                                          // finalizacoes ja chegam no tempo q
        }
        return p < m ? 0.5 * p / m : 0.5 + 0.5 * (p - m) / (1 - m);
    }

    /** Deltas (32 valores: 30 do rig + dx/dy) da animacao no tempo q. */
    static double[] amostrar(String chave, double t) {
        double[] r = new double[32];
        Quadro[] qs = ANIMACOES.get(chave);
        if (qs == null || qs.length == 0) {
            return r;
        }
        t = Math.max(0, Math.min(1, t));
        Quadro a = qs[0];
        Quadro b = qs[qs.length - 1];
        for (int i = 0; i < qs.length - 1; i++) {
            if (t >= qs[i].t() && t <= qs[i + 1].t()) {
                a = qs[i];
                b = qs[i + 1];
                break;
            }
        }
        double u = b.t() <= a.t() ? 1.0 : (t - a.t()) / (b.t() - a.t());
        double w = u * u * (3 - 2 * u);                        // smoothstep
        double[] va = valores(a);
        double[] vb = valores(b);
        for (int i = 0; i < 32; i++) {
            r[i] = va[i] + (vb[i] - va[i]) * w;
        }
        return r;
    }

    private static double[] valores(Quadro q) {
        double[] v = new double[32];
        for (int i = 0; i + 1 < q.pares().length; i += 2) {
            v[q.pares()[i]] = q.pares()[i + 1];
        }
        return v;
    }

    /**
     * Aplica a animacao propria do efeito sobre a pose (deltas) e devolve o deslocamento
     * visual {dx, dy} em px LOCAIS (+x = para a frente do lutador), ou null quando a tecnica
     * nao tem animacao propria (o chamador usa a pose provisoria generica).
     */
    public static double[] aplicar(int[] pose, MotorSolo.EfeitoVisual e) {
        String c = chave(e);
        if (c == null) {
            return null;
        }
        double[] d;
        if (e.tipo() == TipoEfeitoVisual.LIBERANDO) {
            double[] de = amostrar(c, e.desde());
            double p = Math.max(0, Math.min(1, e.progresso()));
            double k = 1 - p * p * (3 - 2 * p);                  // volta suave para a pose da posicao
            d = new double[32];
            for (int i = 0; i < 32; i++) {
                d[i] = de[i] * k;
            }
        } else {
            d = amostrar(c, tempo(e));
            int[] pulso = PULSO_DEFESA.get(c);
            if (pulso != null && e.pulsoDefesa() > 0) {
                for (int i = 0; i + 1 < pulso.length; i += 2) {
                    d[pulso[i]] += pulso[i + 1] * e.pulsoDefesa();   // reacao da defesa, some em 12 ticks
                }
            }
        }
        for (int i = 4; i < 30; i++) {
            pose[i] += (int) Math.round(d[i]);
        }
        double dx = d[30];
        double dy = d[31];
        if (e.tipo() == TipoEfeitoVisual.EMPURRADO && e.deslocamento() != 0) {
            // o motor ja levou o corpo para tras: o desenho parte do lugar antigo (a frente) e desliza
            // ate o novo, sem teletransporte
            double p = Math.min(1.0, e.progresso() / 0.4);
            dx += Math.abs(e.deslocamento()) * (1 - p * p * (3 - 2 * p));
        }
        return new double[] {dx, dy};
    }
}
