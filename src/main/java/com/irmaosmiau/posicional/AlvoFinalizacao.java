package com.irmaosmiau.posicional;

import java.util.Map;

/**
 * Entrega 5Q.6: a ARTICULACAO (ou regiao) que a finalizacao ataca, separada da
 * familia/categoria (BRACO, ESTRANGULAMENTO, PERNA). Diferencia braco, ombro,
 * punho, pescoco, tornozelo, pe, joelho, panturrilha e quadril, e da a defesa
 * especifica que o HUD do defensor mostra ("GIRAR O PE", "DOBRAR O JOELHO"...).
 *
 * Uma tecnica nova recebe o alvo aqui pelo id-base; sem entrada, vale o padrao da
 * categoria (o mesmo rotulo de antes).
 */
public enum AlvoFinalizacao {
    BRACO("PROTEGER BRAÇO"),
    OMBRO("PROTEGER O OMBRO"),
    PUNHO("DOBRAR O PUNHO"),
    PESCOCO("MÃOS NO PESCOÇO"),
    TORNOZELO("GIRAR O PÉ"),
    PE("ESCONDER O PÉ"),
    JOELHO("DOBRAR O JOELHO"),
    PANTURRILHA("TIRAR A PERNA"),
    QUADRIL("FECHAR AS PERNAS");

    private final String defesa;

    AlvoFinalizacao(String defesa) {
        this.defesa = defesa;
    }

    /** Rotulo da defesa tecnica (RS baixo) no HUD de quem esta sendo atacado. */
    public String getDefesa() {
        return defesa;
    }

    private static final Map<String, AlvoFinalizacao> POR_BASE = Map.ofEntries(
            Map.entry("kimura", OMBRO), Map.entry("americana", OMBRO), Map.entry("omoplata", OMBRO),
            Map.entry("hammerlock", OMBRO), Map.entry("chave_punho", PUNHO),
            Map.entry("chave_pe_reta", TORNOZELO), Map.entry("aoki_lock", TORNOZELO),
            Map.entry("pe_reta_5050", TORNOZELO), Map.entry("toe_hold", PE), Map.entry("estima_lock", PE),
            Map.entry("chave_joelho", JOELHO), Map.entry("joelho_invertido", JOELHO),
            Map.entry("kneebar_giro", JOELHO), Map.entry("panturrilha", PANTURRILHA),
            Map.entry("banana_split", QUADRIL),
            // 5Q.8
            Map.entry("baratoplata", OMBRO), Map.entry("tarikoplata", OMBRO), Map.entry("monoplata", OMBRO),
            Map.entry("mao_de_vaca", PUNHO),
            // complemento 5Q.8 (biceps: BRACO, o padrao da categoria)
            Map.entry("mir_lock", OMBRO));

    /** O alvo da finalizacao (pelo id-base; senao o padrao da categoria). */
    public static AlvoFinalizacao de(Finalizacao f) {
        AlvoFinalizacao a = POR_BASE.get(f.tecnicaBase());
        if (a != null) {
            return a;
        }
        return switch (f.categoria()) {
            case BRACO -> BRACO;
            case ESTRANGULAMENTO -> PESCOCO;
            case PERNA -> TORNOZELO;
        };
    }
}
