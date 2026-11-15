package com.irmaosmiau.combate;

import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.FamiliaTecnica;
import com.irmaosmiau.entities.CustosFolego;
import com.irmaosmiau.entities.DefesaCorporal;
import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Entrega 5O: resolve "familia + direcao" numa tecnica concreta, ou explica
 * por que ela nao vale agora. E o mesmo resultado para quem executa (GamePanel),
 * para o HUD contextual e para o F3: o HUD nunca mostra disponivel o que o
 * motor vai recusar.
 *
 * Nada aqui depende do personagem (Branco/Preto): so da postura, do papel na
 * pegada, do movimento em andamento, da recarga e do folego.
 *
 * DEFESAS (RT + direcao): cima = guarda alta, esquerda = guarda media,
 * direita = guarda baixa, baixo = sprawl. PROJECOES (RB): reservado.
 * PEGADAS: a regra do SistemaCombate (5O: nunca com alguem deitado).
 */
public final class ResolvedorFamilias {

    /** O que a direcao faz na familia agora. motivo == null: pode executar. */
    public record Resolucao(FamiliaTecnica familia, DirecaoTecnica direcao, String id, String rotulo,
            String motivo) {
        public boolean disponivel() {
            return motivo == null;
        }
    }

    public static final String EM_PREPARACAO = "EM PREPARAÇÃO";

    private ResolvedorFamilias() {
    }

    /** Guarda da direcao (null = sprawl ou direcao nula). */
    public static DefesaCorporal.Tipo guardaDa(DirecaoTecnica d) {
        if (d == null) {
            return null;
        }
        return switch (d) {
            case CIMA -> DefesaCorporal.Tipo.GUARDA_ALTA;
            case ESQUERDA -> DefesaCorporal.Tipo.GUARDA_MEDIA;
            case DIREITA -> DefesaCorporal.Tipo.GUARDA_BAIXA;
            case BAIXO -> null;
        };
    }

    public static Resolucao resolver(FamiliaTecnica familia, DirecaoTecnica d, IrmaoMiau ator, IrmaoMiau outro,
            SistemaCombate sistema) {
        return switch (familia) {
            case DEFESAS -> defesa(d, ator, sistema);
            case PROJECOES -> new Resolucao(familia, d, null, rotuloProjecao(d), EM_PREPARACAO);
            case PEGADAS -> pegada(d, ator, outro, sistema);
            case ACROBACIAS -> new Resolucao(familia, d, null, "ACROBACIA", "USE A DIREÇÃO DO MOVIMENTO");
        };
    }

    private static Resolucao pegada(DirecaoTecnica d, IrmaoMiau ator, IrmaoMiau outro, SistemaCombate sistema) {
        String id = switch (d) {
            case CIMA -> "pegada_1";
            case DIREITA -> "pegada_2";
            case BAIXO -> "pegada_3";
            case ESQUERDA -> "pegada_4";
        };
        String rotulo = ParametrosPegadas.de(id).nome();
        String motivo = sistema == null || outro == null ? "SEM ADVERSARIO"
                : sistema.motivoIndisponivel(ator, outro, id);
        return new Resolucao(FamiliaTecnica.PEGADAS, d, id, rotulo, motivo);
    }

    private static Resolucao defesa(DirecaoTecnica d, IrmaoMiau ator, SistemaCombate sistema) {
        DefesaCorporal.Tipo g = guardaDa(d);
        String motivo = motivoComum(ator, sistema);
        if (g == null) {
            if (motivo == null) {
                motivo = ator.motivoSemSprawl();
            }
            return new Resolucao(FamiliaTecnica.DEFESAS, d, "sprawl", "SPRAWL", motivo);
        }
        if (motivo == null) {
            motivo = motivoGuarda(ator, g);
        }
        return new Resolucao(FamiliaTecnica.DEFESAS, d, g.name().toLowerCase(java.util.Locale.ROOT), g.getRotulo(),
                motivo);
    }

    /** O mesmo motivo comum, para as acrobacias (que leem o movimento, nao o RS). */
    public static String motivoComumPublico(IrmaoMiau ator, SistemaCombate sistema) {
        return motivoComum(ator, sistema);
    }

    /** Agarrado / segurando a pegada (as defesas so valem livre). */
    static String motivoComum(IrmaoMiau ator, SistemaCombate sistema) {
        if (sistema != null && sistema.estaEmInteracao()) {
            if (sistema.getReceptorInteracao() == ator) {
                return "PERSONAGEM AGARRADO";
            }
            if (sistema.getAplicadorInteracao() == ator) {
                return "SEGURANDO A PEGADA";
            }
        }
        return null;
    }

    private static String motivoGuarda(IrmaoMiau ator, DefesaCorporal.Tipo g) {
        if (ator.isDeitado()) {
            return "INDISPONIVEL NO SOLO";
        }
        if (ator.getTransicaoAtiva() != null) {
            return "MUDANDO DE POSTURA";
        }
        if (ator.isRolando() || ator.isFazendoSprawl() || ator.isPulando() || ator.isEmAcrobacia()) {
            return "EM MOVIMENTO";
        }
        boolean jaNaGuarda = ator.getDefesa().isSegurada();
        if (!jaNaGuarda && ator.getFolegoAtual() < CustosFolego.GUARDA_ENTRADA) {
            return "SEM FOLEGO";
        }
        return null;
    }

    private static String rotuloProjecao(DirecaoTecnica d) {
        return switch (d) {
            case CIMA -> "QUEDA (EM BREVE)";
            case DIREITA -> "QUEDA (EM BREVE)";
            case BAIXO -> "PUXAR PARA A GUARDA (EM BREVE)";
            case ESQUERDA -> "TRANSIÇÃO (EM BREVE)";
        };
    }
}
