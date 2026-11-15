package com.irmaosmiau.animacao;

import com.irmaosmiau.entities.IrmaoMiau;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entrega 5N: camada MINIMA de adaptacao entre uma reacao compartilhada do
 * receptor e o corpo real de quem recebe.
 *
 * - Compatibilidade: a reacao so e usada se o rig do lutador
 *   ({@link IrmaoMiau#getRigAnimacao()}) for o rig da reacao. Rig
 *   incompativel = fallback seguro: nenhum delta (fica a pose do motor).
 * - Proporcao: fatores por grupo (tronco/cabeca, bracos, pernas) para um
 *   personagem de membros mais longos ou mais curtos; padrao 1,0 (Branco e
 *   Preto). Os deltas sao limitados a {@link #LIMITE} unidades para nenhum
 *   membro se deformar.
 * - Postura: de joelhos as pernas ficam com o adaptador de joelhos do motor
 *   (nenhum delta nas pernas: os joelhos continuam no tatame) e a cintura
 *   reage pela metade.
 *
 * O espelhamento nao e tratado aqui: os deltas estao em espaco local (+X =
 * para onde o lutador olha) e o GamePanel espelha quem olha para a esquerda.
 */
public final class AdaptadorRigReacao {

    /** Maior delta permitido em qualquer articulacao (unidades do rig). */
    public static final int LIMITE = 24;

    /** Fatores de proporcao de um personagem (1,0 = como o Branco e o Preto). */
    public record Proporcao(double tronco, double bracos, double pernas) {
        public static final Proporcao PADRAO = new Proporcao(1.0, 1.0, 1.0);
    }

    private static final Map<Class<?>, Proporcao> PROPORCOES = new ConcurrentHashMap<>();

    private AdaptadorRigReacao() {
    }

    /** Um personagem futuro com outras proporcoes registra seus fatores aqui (opcional). */
    public static void registrarProporcao(Class<? extends IrmaoMiau> classe, Proporcao p) {
        PROPORCOES.put(classe, p);
    }

    public static void removerProporcao(Class<? extends IrmaoMiau> classe) {
        PROPORCOES.remove(classe);
    }

    public static Proporcao proporcao(IrmaoMiau l) {
        return PROPORCOES.getOrDefault(l.getClass(), Proporcao.PADRAO);
    }

    public static boolean compativel(IrmaoMiau l, ReacaoReceptor r) {
        return l != null && r != null && r.getRig().equals(l.getRigAnimacao());
    }

    /**
     * Delta pronto para somar na pose do receptor, ou null se o rig nao for
     * compativel (fallback: a pose do motor fica como esta).
     */
    public static int[] adaptar(int[] delta, IrmaoMiau receptor, ReacaoReceptor r) {
        if (!compativel(receptor, r) || delta == null) {
            return null;
        }
        Proporcao p = proporcao(receptor);
        boolean ajoelhado = receptor.isAjoelhado();
        int[] out = new int[delta.length];
        for (int i = 4; i < delta.length; i++) {
            double f;
            if (i <= 13) {
                f = p.tronco();
                if (ajoelhado && i <= 6) {
                    f *= 0.5;                                 // cintura: metade, os joelhos seguram
                }
            } else if (i <= 21) {
                f = p.bracos();
            } else {
                f = ajoelhado ? 0.0 : p.pernas();             // de joelhos: pernas do motor
            }
            int v = (int) Math.round(delta[i] * f);
            out[i] = Math.max(-LIMITE, Math.min(LIMITE, v));
        }
        return out;
    }
}
