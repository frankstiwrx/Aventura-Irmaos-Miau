package com.irmaosmiau.posicional;

/**
 * Entrega 5P.1: transforma os eventos CONFIRMADOS do MotorSolo em frases curtas
 * para o aviso no topo da tela ("J1 derrubou J2!"). Nunca narra tentativas:
 * so SUCESSO, CONTRA, REVERSAO, SPRAWL, ATERRISSOU e quedas defendidas.
 */
public final class NarradorSolo {

    private NarradorSolo() {
    }

    public static String sigla(Lado l) {
        return l == Lado.A ? "J1" : "J2";
    }

    /** Frase do evento (null = nada a dizer). */
    public static String mensagem(MotorSolo.Evento e) {
        String q = sigla(e.lado());
        String o = sigla(e.lado().outro());
        return switch (e.tipo()) {
            case "SUCESSO", "CONTRA" -> sucesso(e.tecnica(), e.detalhe(), q, o);
            case "REVERSAO" -> q + " reverteu a queda!";
            case "SPRAWL" -> q + " defendeu com sprawl!";
            case "ATERRISSOU" -> switch (e.tecnica()) {
                case "GUARDA" -> o + " fechou a guarda!";
                case "MEIA_GUARDA" -> o + " recuperou a meia-guarda!";
                default -> q + " chegou aos 100 quilos!";
            };
            case "DEFENDIDA" -> switch (e.tecnica()) {
                case CatalogoTecnicasSolo.QUEDA_PEGADA, CatalogoTecnicasSolo.EMPURRAO -> o + " defendeu a queda!";
                case CatalogoTecnicasSolo.PASSAR_GUARDA, CatalogoTecnicasSolo.LIBERAR_PERNA,
                        CatalogoTecnicasSolo.PASSAGEM_EM_PE, CatalogoTecnicasSolo.JOELHO_CORTANDO -> o + " manteve a guarda!";
                case CatalogoTecnicasSolo.RASPAGEM, CatalogoTecnicasSolo.RASPAGEM_MEIA -> o + " segurou a raspagem!";
                case CatalogoTecnicasSolo.MONTAR, CatalogoTecnicasSolo.MONTAR_DO_JOELHO -> o + " segurou a montada!";
                case CatalogoTecnicasSolo.PONTE, CatalogoTecnicasSolo.ESCAPE_MONTADA,
                        CatalogoTecnicasSolo.ESCAPE_QUADRIL, CatalogoTecnicasSolo.GIRAR_NAS_COSTAS,
                        CatalogoTecnicasSolo.FUGIR_DO_JOELHO -> o + " segurou o escape!";
                default -> null;
            };
            case "SOLO_LIVRE" -> q + " foi ao chão.";
            default -> null;
        };
    }

    private static String sucesso(String t, String detalhe, String q, String o) {
        return switch (t) {
            case CatalogoTecnicasSolo.EMPURRAO -> detalhe.startsWith("derrubou") ? q + " derrubou " + o + "!"
                    : q + " desequilibrou " + o + "!";
            case CatalogoTecnicasSolo.QUEDA_PEGADA, CatalogoTecnicasSolo.ENTRADA_PERNAS ->
                    detalhe.contains("sem controle") ? "Os dois foram ao chão!" : q + " derrubou " + o + "!";
            case CatalogoTecnicasSolo.PUXAR_GUARDA, CatalogoTecnicasSolo.PUXAR_PARA_GUARDA,
                    CatalogoTecnicasSolo.CONECTAR_GUARDA -> q + " puxou para a guarda!";
            case CatalogoTecnicasSolo.ENTRAR_GUARDA -> q + " entrou na guarda!";
            case CatalogoTecnicasSolo.PASSAGEM_EM_PE, CatalogoTecnicasSolo.PASSAR_GUARDA,
                    CatalogoTecnicasSolo.LIBERAR_PERNA -> q + " passou a guarda!";
            case CatalogoTecnicasSolo.JOELHO_CORTANDO -> q + " chegou à meia-guarda!";
            case CatalogoTecnicasSolo.LEVANTAR_TECNICO, CatalogoTecnicasSolo.LEVANTAR_DO_SOLO,
                    CatalogoTecnicasSolo.LEVANTAR_DA_GUARDA -> q + " levantou!";
            case CatalogoTecnicasSolo.SUBIR_POR_CIMA -> q + " subiu por cima!";
            case CatalogoTecnicasSolo.ABRIR_GUARDA -> q + " abriu a guarda!";
            case CatalogoTecnicasSolo.SOLTAR_GUARDA -> q + " abriu a própria guarda.";
            case CatalogoTecnicasSolo.RASPAGEM, CatalogoTecnicasSolo.RASPAGEM_MEIA -> q + " conseguiu uma raspagem!";
            case CatalogoTecnicasSolo.FECHAR_GUARDA -> q + " fechou a guarda!";
            case CatalogoTecnicasSolo.RECUPERAR_GUARDA, CatalogoTecnicasSolo.ESCAPE_QUADRIL,
                    CatalogoTecnicasSolo.FUGIR_DO_JOELHO -> q + " recuperou a guarda!";
            case CatalogoTecnicasSolo.RECUPERAR_MEIA -> q + " recuperou a meia-guarda!";
            case CatalogoTecnicasSolo.MONTAR, CatalogoTecnicasSolo.MONTAR_DO_JOELHO -> q + " conquistou a montada!";
            case CatalogoTecnicasSolo.JOELHO_NA_BARRIGA -> q + " colocou o joelho na barriga!";
            case CatalogoTecnicasSolo.VOLTAR_CEM_QUILOS -> q + " voltou aos 100 quilos!";
            case CatalogoTecnicasSolo.VIRAR_JOELHOS -> q + " virou para os joelhos!";
            case CatalogoTecnicasSolo.PEGAR_COSTAS, CatalogoTecnicasSolo.PEGAR_COSTAS_VIRADA -> q + " chegou às costas!";
            case CatalogoTecnicasSolo.INVERTER_JOELHO -> q + " inverteu a posição!";
            case CatalogoTecnicasSolo.PONTE -> q + " inverteu com a upa!";
            case CatalogoTecnicasSolo.ESCAPE_MONTADA -> q + " escapou da montada!";
            case CatalogoTecnicasSolo.GIRAR_NAS_COSTAS, CatalogoTecnicasSolo.ESCAPAR_NEUTRO -> q + " escapou das costas!";
            default -> null;
        };
    }
}
