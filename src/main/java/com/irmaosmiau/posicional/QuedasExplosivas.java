package com.irmaosmiau.posicional;

import java.util.Map;
import java.util.Optional;

/**
 * Entrega 5Q.6: perfis das quedas explosivas e das projecoes novas (dados, nao codigo).
 *
 * Todas usam a mesma resolucao do MotorSolo: avanco real ate o contato durante a
 * janela, defesa na janela, derrubada com a aterrissagem que ja existe (o defensor
 * ainda pode reagir: guarda/meia-guarda) e um destino proprio quando ele nao reage.
 *
 * Defesas:
 * - SPRAWL: o sprawl na janela para a entrada (quem entrou fica de joelhos, o outro
 *   controla); o RT (base) sem sprawl leva os dois ao chao sem controle (como a PERNAS);
 * - BASE: o RT na janela (ou recuar para longe) segura; quem entrou fica exposto.
 * Recuar para fora do alcance no fim da janela sempre faz a entrada falhar.
 */
public final class QuedasExplosivas {

    /** Como a queda e defendida. */
    public enum Defesa {
        SPRAWL, BASE
    }

    /**
     * @param alcancePx distancia maxima para entrar (o avanco fecha o resto)
     * @param contatoPx distancia em que o avanco para (os corpos se tocam)
     * @param alvo      posicao quando o defensor nao reage na aterrissagem
     * @param acompanha quem derrubou vai ao chao por cima (false = fica em pe)
     * @param pegada    projecao com a pegada (resolve como as quedas da pegada)
     */
    public record Perfil(String id, int alcancePx, int contatoPx, Posicao alvo, boolean acompanha, Defesa defesa,
            boolean pegada) {
    }

    private static final Map<String, Perfil> PERFIS = Map.ofEntries(
            perfil(CatalogoTecnicasSolo.SINGLE_LEG, 170, 62, Posicao.MEIA_GUARDA, true, Defesa.SPRAWL, false),
            perfil(CatalogoTecnicasSolo.HIGH_CROTCH, 170, 60, Posicao.CONTROLE_LATERAL, true, Defesa.SPRAWL, false),
            perfil(CatalogoTecnicasSolo.ANKLE_PICK, 130, 70, Posicao.EM_PE_CONTRA_SOLO, false, Defesa.BASE, false),
            perfil(CatalogoTecnicasSolo.CARREGADA_OMBRO, 160, 56, Posicao.CONTROLE_LATERAL, true, Defesa.SPRAWL, false),
            perfil(CatalogoTecnicasSolo.BODY_LOCK, 150, 54, Posicao.CONTROLE_LATERAL, true, Defesa.BASE, false),
            // de joelhos: pouco deslocamento (a distancia de joelhos ja e curta)
            perfil(CatalogoTecnicasSolo.SINGLE_LEG_JOELHOS, 120, 56, Posicao.MEIA_GUARDA, true, Defesa.BASE, false),
            perfil(CatalogoTecnicasSolo.DOUBLE_LEG_JOELHOS, 120, 54, Posicao.CONTROLE_LATERAL, true, Defesa.BASE, false),
            perfil(CatalogoTecnicasSolo.ARM_DRAG, 110, 58, Posicao.COSTAS, true, Defesa.BASE, false),
            perfil(CatalogoTecnicasSolo.SNAP_DOWN, 110, 58, Posicao.CONTROLE_LATERAL, true, Defesa.BASE, false),
            // LB + LT com a pegada
            perfil(CatalogoTecnicasSolo.UCHI_MATA, 999, 0, Posicao.CONTROLE_LATERAL, true, Defesa.BASE, true),
            perfil(CatalogoTecnicasSolo.OSOTO_GARI, 999, 0, Posicao.CONTROLE_LATERAL, true, Defesa.BASE, true));

    private static Map.Entry<String, Perfil> perfil(String id, int alcance, int contato, Posicao alvo,
            boolean acompanha, Defesa defesa, boolean pegada) {
        return Map.entry(id, new Perfil(id, alcance, contato, alvo, acompanha, defesa, pegada));
    }

    private QuedasExplosivas() {
    }

    public static Optional<Perfil> perfil(String id) {
        return Optional.ofNullable(PERFIS.get(id));
    }

    public static boolean defendePorSprawl(String id) {
        return perfil(id).map(p -> p.defesa() == Defesa.SPRAWL).orElse(false);
    }

    /** Todos os ids (testes e documentacao). */
    public static java.util.Set<String> ids() {
        return PERFIS.keySet();
    }
}
