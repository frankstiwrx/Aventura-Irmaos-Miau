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

        // 5Q.4: reter com os pes (quem esta no chao contra quem esta em pe) usa as mesmas pernas empurrando
        alias("retencao_pes:ATACA", "afastar_da_guarda:ATACA");
        alias("retencao_pes:RECEBE", "afastar_da_guarda:RECEBE");
        // 5Q.4: afastar do chao (ajuste instantaneo, 14 ticks): recolhe, estende e volta a pose
        def("afastar_do_chao:ATACA",
                q(0.00),
                q(0.35, 6, 8, 9, 6, 22, 30, 23, -26, 24, 26, 25, -40, 26, 32, 27, -26, 28, 28, 29, -40,
                        16, 8, 17, -6, 20, 8, 21, -6),
                q(0.70, 6, -4, 22, 8, 23, 18, 24, 4, 25, 40, 26, 8, 27, 18, 28, 4, 29, 42, 16, 14, 20, 14),
                q(1.00));

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

        // 5Q.6: KISS OF THE DRAGON: a inversao vai mais fundo (passa por baixo das pernas do outro) e sai
        // direto nas costas; o giro e maior e mais lento que o berimbolo
        def("berimbolo_kiss:ATACA",
                q(0.00),
                q(0.20, 6, 24, 10, -14, 26, 22, 28, 22, 31, 4),                                      // encaixa o quadril embaixo
                q(0.50, 10, -96, 5, -8, 22, 24, 24, 28, 26, 34, 27, -24, 28, 40, 29, -34, 30, 10, 31, 6),   // invertido, passando por baixo
                q(0.78, 10, -40, 9, 14, 26, 18, 28, 20, 30, 40),                                     // sai por tras
                q(1.00, 10, 24, 9, 18, 30, 56));
        def("berimbolo_kiss:RECEBE",
                q(0.00),
                q(0.40, 9, 8, 28, 8),
                q(0.70, 9, 18, 11, 8, 22, 6, 26, 8, 29, -10),                                        // as pernas ficam por cima de quem gira
                q(1.00, 9, 28, 11, 10, 22, 10, 23, 10, 31, 8));
        // 5Q.6: BABY BOLO: meia inversao, puxa a perna e sobe POR CIMA (raspagem), sem ir para as costas
        def("berimbolo_invertido:ATACA",
                q(0.00),
                q(0.30, 6, 16, 10, -8, 26, 18, 28, 18),                                              // vira o quadril
                q(0.60, 10, -40, 5, -4, 26, 26, 27, -14, 28, 30, 20, 16, 21, 10, 30, 10),            // meia inversao, puxa a perna
                q(1.00, 10, 10, 9, 22, 20, 18, 21, 16, 16, 14, 17, 18, 30, 30, 31, -6));             // sobe por cima
        def("berimbolo_invertido:RECEBE",
                q(0.00),
                q(0.35, 9, 8, 28, 10),
                q(0.70, 9, 20, 11, 8, 26, 16, 27, -12, 28, 22, 29, -18),                             // a perna presa sobe
                q(1.00, 9, -24, 11, -10, 10, -20, 31, 10));                                          // cai de costas

        // ---------------- 7. QUEBRAR A POSTURA (guarda fechada; ajuste instantaneo, 18 ticks) - 5Q.4
        // quem esta por baixo (deitado: +x = para cima, +y = para o outro) puxa com os joelhos e os bracos
        def("quebrar_postura:ATACA",
                q(0.00),
                q(0.35, 22, 12, 23, -12, 24, 6, 25, -8, 26, 14, 27, -12, 28, 6, 29, -8,
                        16, 4, 17, -12, 14, 2, 15, -6, 20, 6, 21, -14, 18, 3, 19, -7),                 // puxa para o peito
                q(1.00));
        // o de cima (receptor) usa "postura_quebrada": fica curvado os 40 ticks da exposicao e se recompoe
        def("postura_quebrada:RECEBE",
                q(0.00),
                q(0.18, 9, 30, 11, 16, 13, 6, 20, 22, 21, 26, 18, 11, 19, 13, 16, 18, 17, 26, 14, 9, 15, 13, 30, 6),
                q(0.80, 9, 26, 11, 14, 13, 5, 20, 20, 21, 24, 18, 10, 19, 12, 16, 16, 17, 24, 14, 8, 15, 12, 30, 5),
                q(1.00));
        // defendida pela postura (raspagem contra quem esta em postura): o de cima se apruma, as maos no
        // quadril do outro; o de baixo perde a puxada. Termina na pose da posicao.
        def("postura_firme:RECEBE",
                q(0.00),
                q(0.35, 9, -12, 11, -6, 20, 10, 21, 8, 18, 5, 19, 4, 16, 8, 17, 8, 14, 4, 15, 4, 30, -4),
                q(1.00));
        def("postura_firme:ATACA",
                q(0.00),
                q(0.35, 22, 2, 23, 4, 26, 2, 27, 4, 16, 6, 17, 14, 14, 3, 15, 7, 20, 8, 21, 16, 18, 4, 19, 8),
                q(1.00));

        // ---------------- 8. PRENDER AS PERNAS (em pe sobre quem esta no chao) - 5Q.4
        def("prender_pernas:ATACA",
                q(0.00),
                q(0.45, 9, 30, 11, 10, 6, 6, 22, -4, 26, 8, 27, 4, 20, 26, 21, 34, 18, 13, 19, 17,
                        16, 22, 17, 38, 14, 11, 15, 19, 31, 6),                                      // abaixa e alcanca as canelas
                q(1.00, 9, 22, 11, 6, 6, 2, 26, 6, 20, 16, 21, 30, 18, 8, 19, 15, 16, 12, 17, 32, 14, 6, 15, 16,
                        30, -6, 31, 4));                                                             // junta as pernas e puxa
        def("prender_pernas:RECEBE",
                q(0.00),
                q(0.45, 24, 2, 25, 6, 28, -2, 29, 6),
                q(1.00, 22, -6, 23, 6, 24, -10, 25, 12, 26, 4, 27, 6, 28, -4, 29, 12, 20, 6, 21, 10, 18, 3, 19, 5)); // pernas juntas, puxadas

        // ---------------- 9. AJUSTAR O CONTROLE DA PERNA (ashi garami; ajuste instantaneo) - 5Q.4
        def("consolidar_ashi:ATACA",
                q(0.00),
                q(0.45, 6, 14, 22, 10, 23, -8, 24, 12, 25, -6, 26, -8, 27, -6, 28, -12, 29, -4,
                        20, 12, 21, 12, 18, 6, 19, 6, 16, 10, 17, 10, 14, 5, 15, 5),                 // aperta os joelhos, gira o quadril
                q(1.00));
        def("consolidar_ashi:RECEBE",
                q(0.00),
                q(0.45, 9, -10, 11, -6, 26, 6, 27, -4, 28, 12, 29, -8, 20, -8, 21, 6, 18, -4, 19, 3),   // a perna e puxada
                q(1.00));

        // =====================================================================
        // ENTREGA 5Q.5
        // =====================================================================
        // ---------------- 10. RASTEIRA (em pe, com a pegada): carrega, varre o tornozelo, acompanha
        def("rasteira:ATACA",
                q(0.00),
                q(0.35, 9, -8, 26, 10, 27, -12, 28, 14, 29, -14, 20, -10, 21, -6, 18, -5, 19, -3,
                        16, -8, 17, -4, 14, -4, 15, -2),                                             // tira o peso, levanta o pe
                q(0.70, 9, -14, 26, 22, 27, -4, 28, 48, 29, -6, 20, -14, 21, 10, 18, -7, 19, 5,
                        16, -12, 17, 8, 14, -6, 15, 4, 30, 4),                                       // varre baixo, puxa as mangas
                q(1.00, 9, -10, 26, 18, 28, 40, 29, -2, 20, -12, 21, 12, 18, -6, 19, 6, 16, -10, 17, 10,
                        14, -5, 15, 5, 30, 4));
        def("rasteira:RECEBE",
                q(0.00),
                q(0.50, 9, 4),
                q(0.75, 9, -16, 26, 10, 27, -8, 28, 18, 29, -16, 20, 16, 21, -20, 18, 8, 19, -10,
                        16, -14, 17, -22, 14, -7, 15, -11),                                          // o pe sai do chao
                q(1.00, 9, -22, 11, -8, 26, 14, 27, -12, 28, 26, 29, -24, 20, 20, 21, -26, 18, 10, 19, -13,
                        16, -18, 17, -26, 14, -9, 15, -13, 31, -4));                                // perde a base

        // 5Q.6: RASTEIRA DEFENDIDA: parte de onde a janela parou (o pe saindo do chao), recoloca o pe,
        // um pequeno balanco e volta a postura da pegada (nada de ficar com a pose de queda)
        def("rasteira_defendida:RECEBE",
                q(0.00, 9, -22, 11, -8, 26, 14, 27, -12, 28, 26, 29, -24, 20, 20, 21, -26, 18, 10, 19, -13,
                        16, -18, 17, -26, 14, -9, 15, -13, 31, -4),
                q(0.30, 9, -8, 11, -3, 28, 6, 29, -4, 20, 8, 21, -8, 18, 4, 19, -4, 16, -6, 17, -8, 14, -3, 15, -4),
                q(0.60, 9, 4, 11, 2, 20, -2, 16, 2),                                                // recupera a base
                q(1.00));
        // 5Q.7: DERRUBADA FORCADA SEGURADA (RT): a referencia e o "ser empurrado" (aprovado), menor: o
        // tronco cede um pouco, meio passo de tras, os bracos acompanham (sem esticar: IntegridadeMembros)
        def("derrubada_forcada_defendida:RECEBE",
                q(0.00),
                q(0.25, 9, -10, 11, -5, 6, -3, 16, 6, 17, -10, 20, 4, 21, -12, 24, -4, 28, -2),
                q(0.60, 9, -3, 11, -1, 16, 2, 17, -3, 20, 1, 21, -4, 24, -2),
                q(1.00));

        // =====================================================================
        // ENTREGA 5Q.6: quedas explosivas (LT sem pegada), body lock (RB ->), projecoes do LB + LT
        // Referencia de qualidade: a ENTRADA NAS PERNAS (aprovada). O avanco ate o contato e REAL
        // (o motor move quem entra durante a janela); aqui so a forma do corpo e um pequeno dx.
        // =====================================================================
        // SINGLE LEG: baixa o nivel, joelho de dentro a frente, abraca UMA perna e a levanta
        def("single_leg:ATACA",
                q(0.00),
                q(0.30, 9, 18, 11, 6, 26, 10, 27, -14, 22, -4, 23, -10, 20, 26, 21, 24, 18, 13, 19, 12,
                        16, 10, 17, 16, 14, 5, 15, 8, 31, 12),                                       // muda o nivel
                q(0.62, 9, 30, 11, 10, 26, 26, 27, -4, 28, 28, 22, -14, 24, -24, 25, 4, 20, 34, 21, 34,
                        18, 17, 19, 17, 16, 28, 17, 34, 14, 14, 15, 17, 30, 14, 31, 22),               // penetra e abraca a perna
                q(1.00, 9, 8, 11, 4, 26, 18, 27, -8, 28, 16, 22, -10, 24, -18, 20, 24, 21, 6, 18, 12, 19, 3,
                        16, 20, 17, 10, 14, 10, 15, 5, 30, 16, 31, 6));                               // sobe com a perna presa
        def("single_leg:RECEBE",
                q(0.00),
                q(0.55, 9, 4, 26, 6),
                q(1.00, 9, -10, 11, -4, 26, 22, 27, -30, 28, 34, 29, -34, 20, 12, 21, -18, 18, 6, 19, -9,
                        16, -10, 17, -20, 14, -5, 15, -10, 24, -4, 31, -3));                          // perna no ar, bracos equilibram
        // HIGH CROTCH: entra por fora, cabeca do lado, corre e levanta pelo quadril
        def("high_crotch:ATACA",
                q(0.00),
                q(0.30, 9, 16, 11, 4, 22, -4, 23, -8, 26, 12, 27, -12, 20, 18, 21, 12, 18, 9, 19, 6,
                        16, 14, 17, 10, 14, 7, 15, 5, 31, 10),
                q(0.60, 9, 36, 11, 14, 26, 26, 27, -2, 28, 30, 22, -18, 24, -30, 25, 6, 20, 30, 21, 30,
                        18, 15, 19, 15, 16, 34, 17, 22, 14, 17, 15, 11, 30, 18, 31, 24),               // passo de penetracao por fora
                q(1.00, 9, 24, 11, -6, 26, 22, 27, -6, 28, 22, 22, -12, 24, -26, 20, 30, 21, 8, 18, 15, 19, 4,
                        16, 28, 17, 4, 14, 14, 15, 2, 30, 26, 31, 10));                               // corre e levanta o quadril
        def("high_crotch:RECEBE",
                q(0.00),
                q(0.50, 9, 6),
                q(1.00, 9, -20, 11, -10, 6, -8, 20, 18, 21, -24, 18, 9, 19, -12, 16, 12, 17, -26, 14, 6, 15, -13,
                        26, 10, 27, -10, 28, 14, 29, -14, 30, -8, 31, -4));                           // o quadril sai do chao
        // ANKLE PICK: puxa a cabeca para baixo com uma mao, mergulha e pega o tornozelo com a outra
        def("ankle_pick:ATACA",
                q(0.00),
                q(0.35, 9, 10, 22, -6, 26, 10, 27, -6, 28, 14, 16, 26, 17, -14, 14, 13, 15, -7),     // mao na cabeca
                q(0.70, 9, 42, 11, 14, 26, 28, 27, 4, 28, 24, 22, -12, 24, -22, 20, 30, 21, 40, 18, 15, 19, 20,
                        16, 28, 17, -2, 14, 14, 15, -1, 30, 10, 31, 26),                              // mergulha ate o tornozelo
                q(1.00, 9, 26, 11, 8, 26, 22, 28, 16, 20, 26, 21, 24, 18, 13, 19, 12, 16, 22, 17, -10,
                        14, 11, 15, -5, 30, 12, 31, 12));                                            // puxa o pe e empurra a cabeca
        def("ankle_pick:RECEBE",
                q(0.00),
                q(0.40, 9, 10, 11, 10, 13, 4),                                                       // a cabeca desce
                q(1.00, 9, 14, 11, 6, 28, 30, 29, -10, 26, 16, 27, -8, 20, 16, 21, 10, 16, 10, 17, 8, 31, 4));
        // CARREGADA NO OMBRO: mergulho fundo, cabeca por baixo do braco, levanta o outro no ombro
        def("carregada_ombro:ATACA",
                q(0.00),
                q(0.30, 9, 20, 11, 8, 22, -8, 23, -14, 26, 16, 27, -18, 20, 24, 21, 16, 18, 12, 19, 8,
                        16, 20, 17, 6, 14, 10, 15, 3, 31, 18),
                q(0.60, 9, 46, 11, 16, 26, 32, 27, 6, 28, 30, 22, -20, 24, -30, 25, 6, 20, 32, 21, 36,
                        18, 16, 19, 18, 16, 6, 17, -28, 14, 3, 15, -14, 30, 22, 31, 34),               // joelho no chao, ombro embaixo
                q(1.00, 9, 18, 26, 24, 27, -8, 28, 22, 22, -14, 24, -24, 20, 28, 21, -8, 18, 14, 19, -4,
                        16, 4, 17, -34, 14, 2, 15, -17, 30, 24, 31, 12));                             // sobe com o outro no ombro
        def("carregada_ombro:RECEBE",
                q(0.00),
                q(0.50, 9, 10, 11, 6),
                q(1.00, 9, 40, 11, 18, 6, 20, 20, -10, 21, 26, 18, -5, 19, 13, 16, -14, 17, 30, 14, -7, 15, 15,
                        26, -8, 27, -18, 28, -14, 29, -26, 22, 6, 23, -14, 24, 10, 25, -22, 31, -18));   // dobrado no ombro, sai do chao
        // BODY LOCK (RB -> sem pegada): abraca a cintura, perna por tras do pe e tomba por cima
        def("body_lock:ATACA",
                q(0.00),
                q(0.35, 9, 12, 22, -4, 26, 10, 27, -6, 28, 16, 20, 30, 21, 8, 18, 15, 19, 4, 16, 30, 17, 10,
                        14, 15, 15, 5, 30, 8),                                                        // abraca a cintura
                q(0.70, 9, 18, 11, 6, 26, 24, 27, -10, 28, 40, 29, -6, 20, 32, 21, 12, 18, 16, 19, 6,
                        16, 32, 17, 14, 14, 16, 15, 7, 30, 14),                                       // perna por tras do pe dele
                q(1.00, 9, 30, 11, 10, 26, 22, 27, -6, 28, 36, 20, 30, 21, 20, 18, 15, 19, 10, 16, 30, 17, 22,
                        14, 15, 15, 11, 30, 18, 31, 10));                                            // tomba por cima
        def("body_lock:RECEBE",
                q(0.00),
                q(0.40, 9, -4, 20, 10, 21, -10, 16, 10, 17, -10),                                    // maos nos ombros
                q(1.00, 9, -24, 11, -10, 6, -6, 26, 10, 27, -10, 28, 18, 29, -14, 20, 16, 21, -26, 18, 8, 19, -13,
                        16, 14, 17, -28, 14, 7, 15, -14, 30, -6, 31, -4));
        // ---------- DE JOELHOS: menos amplitude vertical, entradas curtas, sem levantar
        def("single_leg_joelhos:ATACA",
                q(0.00),
                q(0.40, 9, 20, 11, 6, 20, 20, 21, 18, 18, 10, 19, 9, 16, 12, 17, 14, 14, 6, 15, 7, 30, 6),
                q(1.00, 9, 26, 11, 8, 20, 28, 21, 22, 18, 14, 19, 11, 16, 24, 17, 24, 14, 12, 15, 12, 30, 14, 31, 4));
        def("single_leg_joelhos:RECEBE",
                q(0.00),
                q(0.50, 9, 4),
                q(1.00, 9, -12, 11, -6, 20, 12, 21, -14, 18, 6, 19, -7, 16, 8, 17, -16, 14, 4, 15, -8,
                        26, 8, 28, 12, 30, -6));
        def("double_leg_joelhos:ATACA",
                q(0.00),
                q(0.40, 9, 24, 11, 8, 20, 24, 21, 14, 18, 12, 19, 7, 16, 24, 17, 14, 14, 12, 15, 7, 30, 8),
                q(1.00, 9, 36, 11, 10, 20, 32, 21, 18, 18, 16, 19, 9, 16, 30, 17, 20, 14, 15, 15, 10, 30, 18, 31, 6));
        def("double_leg_joelhos:RECEBE",
                q(0.00),
                q(0.50, 9, 4),
                q(1.00, 9, -20, 11, -8, 6, -8, 20, 14, 21, -20, 18, 7, 19, -10, 16, 12, 17, -22, 14, 6, 15, -11,
                        30, -10, 31, -2));
        def("arm_drag:ATACA",
                q(0.00),
                q(0.35, 9, 6, 20, 26, 21, 4, 18, 13, 19, 2, 16, 30, 17, -6, 14, 15, 15, -3),         // pega o punho e o triceps
                q(0.70, 9, -6, 6, 10, 20, -6, 21, 10, 18, -3, 19, 5, 16, 4, 17, 8, 14, 2, 15, 4, 30, 10),   // puxa o braco para tras
                q(1.00, 9, 14, 6, 14, 20, 22, 21, 6, 18, 11, 19, 3, 16, 24, 17, 4, 14, 12, 15, 2, 30, 24));  // passa para as costas
        def("arm_drag:RECEBE",
                q(0.00),
                q(0.40, 20, 10, 21, -4),
                q(0.75, 9, 18, 11, 8, 20, 24, 21, 6, 18, 12, 19, 3, 30, 10),                         // o braco vai junto
                q(1.00, 9, 22, 11, 10, 6, -10, 20, 20, 21, 10, 18, 10, 19, 5, 16, -6, 17, 8, 30, 8));
        def("snap_down:ATACA",
                q(0.00),
                q(0.35, 9, -6, 20, 26, 21, -12, 18, 13, 19, -6, 16, 24, 17, -14, 14, 12, 15, -7),    // maos na nuca
                q(0.65, 9, 28, 11, 8, 20, 30, 21, 18, 18, 15, 19, 9, 16, 28, 17, 16, 14, 14, 15, 8, 30, 6),   // puxa para baixo
                q(1.00, 9, 36, 11, 10, 20, 26, 21, 24, 18, 13, 19, 12, 16, 24, 17, 22, 14, 12, 15, 11, 30, 10, 31, 4));
        def("snap_down:RECEBE",
                q(0.00),
                q(0.40, 9, 8, 11, 10),
                q(1.00, 9, 34, 11, 18, 13, 4, 20, 18, 21, 24, 18, 9, 19, 12, 16, 14, 17, 24, 14, 7, 15, 12, 31, 6));
        // ---------- LB + LT com a pegada
        def("uchi_mata:ATACA",
                q(0.00),
                q(0.35, 9, -6, 6, 18, 22, -6, 26, 8, 28, 10, 20, -10, 21, -4, 16, -12, 17, -6, 30, 10),   // gira entrando
                q(0.70, 9, 30, 11, 10, 6, 22, 26, -14, 27, -30, 28, -30, 29, -30, 22, -6, 24, -4, 20, -14, 21, 10,
                        18, -7, 19, 5, 16, -16, 17, 12, 30, 16),                                     // a perna sobe por dentro
                q(1.00, 9, 40, 11, 14, 6, 16, 26, -12, 27, -26, 28, -26, 29, -28, 20, -12, 21, 18, 18, -6, 19, 9,
                        16, -14, 17, 20, 30, 18, 31, 8));
        def("uchi_mata:RECEBE",
                q(0.00),
                q(0.50, 9, 10),
                q(1.00, 9, 30, 11, 10, 26, 16, 27, -30, 28, 24, 29, -36, 20, 16, 21, -10, 16, 10, 17, -14,
                        30, 10, 31, -10));                                                           // levantado pela coxa
        def("osoto_gari:ATACA",
                q(0.00),
                q(0.35, 9, -4, 22, -4, 26, 12, 27, -10, 28, 20, 29, -6, 20, -8, 21, -6, 16, -12, 17, -8, 30, 8),
                q(0.70, 9, 16, 11, 4, 26, 20, 27, -20, 28, 36, 29, -30, 20, -4, 21, -2),             // a perna passa por tras
                q(1.00, 9, 28, 11, 8, 26, -10, 27, -16, 28, -26, 29, -12, 20, 6, 21, 8, 16, -2, 17, 6, 30, 12, 31, 4));
        def("osoto_gari:RECEBE",
                q(0.00),
                q(0.50, 9, -8, 11, -6),
                q(1.00, 9, -28, 11, -12, 26, 18, 27, -18, 28, 30, 29, -28, 20, 18, 21, -24, 16, 14, 17, -26,
                        30, -6, 31, -4));
        def("puxar_aranha:ATACA",
                q(0.00),
                q(0.40, 9, -14, 11, -6, 20, -12, 21, -6, 16, -14, 17, -6, 26, 10, 27, -16, 31, 16),   // senta puxando as mangas
                q(1.00, 9, -30, 11, -12, 20, -16, 21, -8, 16, -18, 17, -8, 26, 20, 27, -34, 28, 30, 29, -40,
                        22, 14, 23, -24, 24, 22, 25, -30, 31, 40));                                  // pes nos biceps
        def("puxar_aranha:RECEBE",
                q(0.00),
                q(0.50, 9, 8),
                q(1.00, 9, 18, 11, 6, 20, 10, 21, 8, 16, 8, 17, 8, 30, 8));
        def("puxar_laco:ATACA",
                q(0.00),
                q(0.40, 9, -12, 20, -10, 21, -4, 16, -12, 17, -6, 26, 12, 27, -14, 31, 14),
                q(1.00, 9, -26, 11, -8, 20, -14, 21, -10, 16, -16, 17, -6, 26, 26, 27, -30, 28, 20, 29, -42, 31, 36));
        def("puxar_laco:RECEBE",
                q(0.00),
                q(0.50, 9, 6),
                q(1.00, 9, 16, 11, 4, 20, 18, 21, 6, 18, 9, 19, 3, 30, 6));

        // =====================================================================
        // ENTREGA 5Q.6: microanimacoes de confirmacao (ajustes) - suaves, proporcionais, cada uma a sua
        // Deitado: +x local = para cima, +y local = para o outro. Por cima de joelhos: o normal.
        // =====================================================================
        def("consolidar:ATACA", q(0.00), q(0.40, 9, 10, 31, 4, 20, 6, 21, 6, 16, 6, 17, 6, 22, -4), q(1.00));   // peso no peito
        def("consolidar:RECEBE", q(0.00), q(0.45, 9, -4, 6, -3, 11, -4), q(1.00));
        def("postura_meia:ATACA", q(0.00), q(0.45, 9, 12, 11, 6, 20, 10, 21, 8, 18, 5, 19, 4, 30, 4), q(1.00));   // ombro no queixo
        def("postura_meia:RECEBE", q(0.00), q(0.45, 11, -6, 13, -3), q(1.00));
        def("pressao_joelho:ATACA", q(0.00), q(0.45, 9, 6, 26, 4, 27, 6, 31, 5, 20, 4, 21, 6), q(1.00));       // desce o peso no joelho
        def("pressao_joelho:RECEBE", q(0.00), q(0.45, 9, -4, 6, -4), q(1.00));
        def("estabilizar_montada:ATACA", q(0.00),
                q(0.35, 22, -8, 26, 8, 16, -10, 17, 10, 14, -5, 15, 5, 20, 10, 21, 10, 18, 5, 19, 5, 31, 3),   // abre os joelhos, maos na base
                q(1.00));
        def("montada_alta:ATACA", q(0.00), q(0.30, 4, 8, 7, 8, 9, -6, 30, 8, 31, -6, 22, 6, 26, 6),            // o quadril sobe para o peito
                q(0.85, 4, 8, 7, 8, 9, -6, 30, 8, 31, -6, 22, 6, 26, 6), q(1.00));
        def("montada_alta:RECEBE", q(0.00), q(0.40, 9, -4, 11, -4), q(1.00));
        def("ajustar_ganchos:ATACA", q(0.00), q(0.40, 24, 8, 25, -6, 28, 8, 29, -6, 22, 4, 26, 4),             // os pes entram por dentro
                q(0.75, 24, 3, 25, 2, 28, 3, 29, 2), q(1.00));
        def("ajustar_ganchos:RECEBE", q(0.00), q(0.45, 9, 3, 26, 3), q(1.00));
        def("controlar_pernas:ATACA", q(0.00),                                                                  // maos nos joelhos
                q(0.15, 9, 8, 20, 14, 21, 18, 18, 7, 19, 9, 16, 12, 17, 18, 14, 6, 15, 9),
                q(0.85, 9, 8, 20, 14, 21, 18, 18, 7, 19, 9, 16, 12, 17, 18, 14, 6, 15, 9), q(1.00));
        def("controlar_pernas:RECEBE", q(0.00), q(0.30, 26, -6, 22, -6, 28, -4, 24, -4), q(1.00));            // as pernas juntas
        def("angulo_guarda:ATACA", q(0.00), q(0.40, 6, 10, 10, 6, 4, 6, 26, 4, 22, -4), q(1.00));              // gira o quadril
        def("angulo_guarda:RECEBE", q(0.00), q(0.45, 9, 3), q(1.00));
        def("pegar_manga:ATACA", q(0.00), q(0.40, 20, 16, 21, 22, 18, 8, 19, 11),                              // alcanca a manga
                q(0.80, 20, 10, 21, 12, 18, 5, 19, 6), q(1.00, 20, 10, 21, 12, 18, 5, 19, 6));                 // e fica segurando
        def("pegar_manga:RECEBE", q(0.00), q(0.60, 20, 6, 21, -6, 18, 3, 19, -3), q(1.00));                   // o braco e puxado
        def("reter_borboleta:ATACA", q(0.00), q(0.40, 26, 6, 27, -6, 22, 6, 23, -6), q(1.00));                 // ganchos tensos
        alias("reter_dlr:ATACA", "reter_borboleta:ATACA");
        alias("reter_aranha:ATACA", "reter_borboleta:ATACA");
        alias("reter_slx:ATACA", "reter_borboleta:ATACA");
        def("travar_laco:ATACA", q(0.00), q(0.40, 26, 8, 27, -8, 20, 6, 21, 8, 18, 3, 19, 4), q(1.00));       // enrola a perna no braco
        def("postura_borboleta:ATACA", q(0.00),                                                                 // afunda o quadril e segura
                q(0.20, 4, -6, 9, -8, 31, 4, 22, -6, 24, -8),
                q(0.85, 4, -6, 9, -8, 31, 4, 22, -6, 24, -8), q(1.00));
        alias("postura_dlr:ATACA", "postura_borboleta:ATACA");
        alias("postura_laco:ATACA", "postura_borboleta:ATACA");
        alias("postura_aranha:ATACA", "postura_borboleta:ATACA");
        alias("postura_slx:ATACA", "postura_borboleta:ATACA");
        // ---------- desequilibrar com os pes (janela): recolhe, empurra; o outro tropeca e recupera
        def("desequilibrar_pes:ATACA", q(0.00),
                q(0.40, 26, 10, 27, -16, 28, 12, 29, -18, 22, 6, 23, -10),
                q(0.80, 26, 6, 27, 18, 28, 4, 29, 26, 22, 4, 23, 12, 24, 2, 25, 16),
                q(1.00));
        def("desequilibrar_pes:RECEBE", q(0.00), q(0.60, 9, -4),
                q(0.85, 9, -14, 11, -6, 30, -6, 20, 8, 21, -10, 16, 6, 17, -8),
                q(1.00, 9, -6, 30, -3));
        for (String id : new String[] {"desequilibrar_guarda", "deseq_borboleta", "deseq_dlr", "deseq_aranha"}) {
            alias(id + ":ATACA", "desequilibrar_pes:ATACA");
            alias(id + ":RECEBE", "desequilibrar_pes:RECEBE");
        }
        // ---------- TIRO DE GUARDA (explosivo: prepara, salta, encaixa as pernas, desce) x PUXAR GUARDA (controlado)
        def("tiro_guarda:ATACA",
                q(0.00),
                q(0.15, 9, 10, 22, 10, 23, 8, 26, 10, 27, 8, 31, 10),                                // agacha (prepara)
                q(0.42, 9, -6, 20, -10, 21, -8, 16, -10, 17, -8, 22, 14, 23, -20, 26, 18, 27, -24,
                        24, 8, 25, -16, 28, 10, 29, -18, 30, 16, 31, -38),                                // salta e recolhe as pernas
                q(0.68, 9, -14, 20, -12, 21, -6, 16, -12, 17, -6, 22, 24, 23, -8, 24, 30, 25, -4,
                        26, 30, 27, -10, 28, 40, 29, -6, 30, 26, 31, -34),                                // pernas em volta da cintura
                q(0.84, 9, -16, 20, -12, 21, -4, 16, -12, 17, -4, 22, 24, 23, -8, 24, 30, 25, -4,
                        26, 30, 27, -10, 28, 40, 29, -6, 30, 26, 31, -30),                                // encaixado (um instante)
                q(1.00, 9, -22, 22, 20, 23, -6, 24, 26, 26, 26, 27, -8, 28, 34, 30, 24, 31, -8));        // desce para a guarda
        def("tiro_guarda:RECEBE",
                q(0.00),
                q(0.42, 9, -4, 20, 6, 21, -4),
                q(0.70, 9, -12, 11, -6, 20, 10, 21, -6, 16, 8, 17, -4, 22, -4, 24, -6),               // segura o peso
                q(1.00, 9, -18, 11, -6, 20, 10, 21, -4, 30, -4, 31, 4));
        def("puxar_guarda:ATACA",
                q(0.00),
                q(0.45, 9, -10, 20, -8, 21, -4, 16, -8, 17, -4, 22, 8, 23, 6, 26, 8, 27, 6, 31, 12),  // senta devagar puxando
                q(1.00, 9, -18, 20, -12, 21, -6, 16, -12, 17, -6, 22, 14, 23, 10, 26, 16, 27, -10, 28, 16, 31, 26));
        def("puxar_guarda:RECEBE",
                q(0.00),
                q(0.50, 9, 8, 20, 6, 21, 4),
                q(1.00, 9, 16, 11, 6, 20, 10, 21, 8, 16, 8, 17, 8, 30, 6));                           // vem junto, curvado

        // ---------------- 11. ENTRADA NAS PERNAS (baixa o nivel, penetra, abraca e empurra)
        def("entrada_pernas:ATACA",
                q(0.00),
                q(0.30, 9, 22, 11, 6, 22, -6, 23, -12, 26, 14, 27, -16, 20, 22, 21, 10, 18, 11, 19, 5,
                        16, 18, 17, 12, 14, 9, 15, 6, 31, 14),                                       // baixa o nivel
                q(0.65, 9, 40, 11, 12, 26, 28, 27, -6, 28, 30, 22, -16, 23, -4, 24, -26, 25, 4,
                        20, 34, 21, 26, 18, 17, 19, 13, 16, 30, 17, 30, 14, 15, 15, 15, 30, 20, 31, 28),   // passo de penetracao
                q(1.00, 9, 44, 11, 12, 26, 30, 27, -4, 28, 34, 22, -18, 24, -30, 25, 4,
                        20, 36, 21, 30, 18, 18, 19, 15, 16, 32, 17, 32, 14, 16, 15, 16, 30, 30, 31, 30));  // abraca e empurra
        def("entrada_pernas:RECEBE",
                q(0.00),
                q(0.60, 9, 6, 26, 6, 28, 4),
                q(1.00, 9, -14, 11, -6, 20, 14, 21, -16, 18, 7, 19, -8, 16, 10, 17, -18, 14, 5, 15, -9,
                        26, -6, 28, -10, 29, -4, 30, -6));                                          // pernas presas, quadril empurrado

        // ---------------- 12. JOELHO NA BARRIGA DIRETO (em pe sobre quem esta no chao)
        def("joelho_direto:ATACA",
                q(0.00),
                q(0.40, 9, 10, 26, 16, 27, -26, 28, 20, 29, -30, 20, 14, 21, 10, 18, 7, 19, 5),      // sobe o joelho
                q(0.75, 9, 18, 26, 30, 27, 10, 28, 6, 29, 10, 20, 26, 21, 30, 18, 13, 19, 15,
                        16, 20, 17, 26, 14, 10, 15, 13, 30, 10, 31, 12),                             // o joelho desce na barriga
                q(1.00, 9, 16, 26, 28, 27, 14, 28, 2, 29, 12, 20, 24, 21, 32, 18, 12, 19, 16,
                        16, 18, 17, 28, 14, 9, 15, 14, 30, 12, 31, 14));
        // quem esta deitado (+x = para cima, +y = para o outro): mãos sobem para enquadrar o joelho
        def("joelho_direto:RECEBE",
                q(0.00),
                q(0.70, 20, 16, 21, 10, 18, 8, 19, 5, 16, 12, 17, 8, 14, 6, 15, 4, 26, 10, 27, -8),
                q(1.00, 9, -6, 6, -6, 20, 10, 21, 14, 18, 5, 19, 7, 16, 8, 17, 12, 14, 4, 15, 6));      // impacto

        // ---------------- 13. LEVANTADA TECNICA (janela do comando; a transicao continua a subida)
        // deitado: senta um pouco, apoia a mao de tras e planta o pe da frente (o mesmo apoio com que a
        // transicao LEVANTADA_TECNICA comeca: sem estalo)
        def("levantar_tecnico:ATACA",
                q(0.00),
                q(0.55, 10, 16, 16, -16, 17, -22, 14, -8, 26, 18, 27, -12, 28, 12, 20, 10, 21, 8),
                q(1.00, 10, 16, 16, -16, 17, -22, 14, -8, 26, 18, 27, -12, 28, 12));

        // ---------------- 14. RECUO EM PE: dois passos para tras alternando a base (o corpo desliza)
        def("recuar:ATACA",
                q(0.00),
                q(0.22, 9, -4, 22, -6, 23, -6, 24, -16, 25, -12, 20, -4, 16, -4),                   // pe de tras sobe e recua
                q(0.45, 9, -6, 22, -8, 24, -20, 26, 4, 20, -6, 16, -6),                              // planta atras
                q(0.70, 9, -4, 26, -10, 27, -8, 28, -18, 29, -12, 22, -4, 24, -10, 20, -4, 16, -4),  // o da frente passa
                q(1.00));

        // ---------------- 15. POSTURA (guarda fechada): apruma, maos no quadril do outro, segura
        def("postura:ATACA",
                q(0.00),
                q(0.30, 9, -22, 11, -8, 20, 14, 21, 18, 18, 7, 19, 9, 16, 10, 17, 16, 14, 5, 15, 8, 30, -4),
                q(0.85, 9, -18, 11, -6, 20, 12, 21, 16, 18, 6, 19, 8, 16, 8, 17, 14, 14, 4, 15, 7, 30, -3),
                q(1.00));
        // vindo da postura quebrada: parte de curvado (o mesmo de "postura_quebrada"), passa do ponto e segura
        def("postura_recompor:ATACA",
                q(0.00, 9, 26, 11, 14, 13, 5, 20, 20, 21, 24, 18, 10, 19, 12, 16, 16, 17, 24, 14, 8, 15, 12, 30, 5),
                q(0.30, 9, -24, 11, -8, 20, 14, 21, 18, 18, 7, 19, 9, 16, 10, 17, 16, 14, 5, 15, 8, 30, -4),
                q(0.85, 9, -18, 11, -6, 20, 12, 21, 16, 18, 6, 19, 8, 16, 8, 17, 14, 14, 4, 15, 7, 30, -3),
                q(1.00));
        def("postura:RECEBE",
                q(0.00),
                q(0.35, 20, -8, 21, -6, 18, -4, 19, -3, 16, -6, 17, -4, 14, -3, 15, -2, 26, 4, 27, 4),  // perde a puxada
                q(1.00));

        // ---------------- 16. PERNAS PRESAS (depois de prender): as maos FICAM nas canelas
        def("pernas_presas:ATACA",
                q(0.00, 9, 22, 11, 6, 6, 2, 26, 6, 20, 16, 21, 30, 18, 8, 19, 15, 16, 12, 17, 32, 14, 6, 15, 16,
                        30, -6, 31, 4),
                q(0.85, 9, 22, 11, 6, 6, 2, 26, 6, 20, 16, 21, 30, 18, 8, 19, 15, 16, 12, 17, 32, 14, 6, 15, 16,
                        30, -6, 31, 4),
                q(1.00));
        def("pernas_presas:RECEBE",
                q(0.00, 22, -6, 23, 6, 24, -10, 25, 12, 26, 4, 27, 6, 28, -4, 29, 12, 20, 6, 21, 10, 18, 3, 19, 5),
                q(0.85, 22, -6, 23, 6, 24, -10, 25, 12, 26, 4, 27, 6, 28, -4, 29, 12, 20, 6, 21, 10, 18, 3, 19, 5),
                q(1.00));

        // ---------------- 18. ENTRADAS NO CONTROLE DE PERNA (ate o ashi garami)
        // por baixo (deitado: +x = para cima, +y = para o outro): gira o quadril, as pernas descem e enroscam
        def("entrada_perna_guarda:ATACA",
                q(0.00),
                q(0.40, 6, 16, 10, -8, 26, 24, 27, -10, 28, 18, 29, -16, 22, 10, 23, -6, 20, 12, 21, 10, 18, 6, 19, 5),
                q(1.00, 6, 22, 10, -12, 26, -6, 27, 8, 28, -12, 29, 14, 22, -10, 23, 12, 24, 8, 25, 18,
                        20, 16, 21, 14, 18, 8, 19, 7, 16, 10, 17, 10, 14, 5, 15, 5));
        alias("entrada_perna_aberta:ATACA", "entrada_perna_guarda:ATACA");
        alias("entrada_perna_aberta_dlr:ATACA", "entrada_perna_guarda:ATACA");
        // quem recebe (por cima, de joelhos/em pe): a perna da frente e puxada, o tronco vem junto
        def("entrada_perna_guarda:RECEBE",
                q(0.00),
                q(0.60, 9, 8, 26, 8, 27, 4, 28, 12, 29, 4),
                q(1.00, 9, 14, 11, 6, 26, 14, 27, 6, 28, 22, 29, 8, 20, -8, 21, 10, 18, -4, 19, 5));
        alias("entrada_perna_aberta:RECEBE", "entrada_perna_guarda:RECEBE");
        alias("entrada_perna_aberta_dlr:RECEBE", "entrada_perna_guarda:RECEBE");
        // por cima (de joelhos): abandona o controle, senta para tras e abraca a perna (fica exposto)
        def("entrada_perna_dominante:ATACA",
                q(0.00),
                q(0.40, 9, -18, 11, -6, 20, 16, 21, 22, 18, 8, 19, 11, 16, 12, 17, 22, 14, 6, 15, 11, 31, 8),
                q(1.00, 9, -34, 11, -10, 26, 20, 27, -10, 28, 30, 29, -8, 22, 10, 24, 14,
                        20, 12, 21, 16, 18, 6, 19, 8, 16, 10, 17, 18, 14, 5, 15, 9, 30, -10, 31, 22));
        // quem estava por baixo (deitado): a perna sobe presa
        def("entrada_perna_dominante:RECEBE",
                q(0.00),
                q(0.60, 26, 8, 27, -4, 28, 12, 29, -4),
                q(1.00, 26, 14, 27, -8, 28, 22, 29, -10, 20, 8, 21, 6, 18, 4, 19, 3));

        // ---------------- 17. UNDERHOOK NA MEIA-GUARDA (ajuste de quem esta por baixo): braco por baixo
        def("underhook_meia:ATACA",
                q(0.00),
                q(0.45, 6, 10, 20, 18, 21, 16, 18, 9, 19, 8, 16, 6, 17, 6, 14, 3, 15, 3, 26, 6, 27, -6),
                q(1.00));
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

    /** 5Q.6: amostra publica (testes de diferenca entre variacoes). */
    public static double[] amostraParaTeste(String chave, double t) {
        return amostrar(chave, t);
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
        if ((e.tipo() == TipoEfeitoVisual.EMPURRADO || e.tipo() == TipoEfeitoVisual.TECNICA_ATAQUE
                || e.tipo() == TipoEfeitoVisual.AFASTANDO) && e.deslocamento() != 0) {
            // o motor ja levou o corpo para tras: o desenho parte do lugar antigo (a frente) e desliza
            // ate o novo, sem teletransporte
            // 5Q.5: o recuo em pe / afastar no chao deslizam durante o efeito todo (o empurrao em 40%)
            double p = Math.min(1.0, e.progresso() / (e.tipo() == TipoEfeitoVisual.EMPURRADO ? 0.4 : 0.9));
            dx += Math.abs(e.deslocamento()) * (1 - p * p * (3 - 2 * p));
        }
        return new double[] {dx, dy};
    }
}
