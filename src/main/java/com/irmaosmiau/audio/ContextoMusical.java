package com.irmaosmiau.audio;

import java.util.List;

/**
 * Entrega 5Q.6: cada tela/modo do jogo tem o PROPRIO mapeamento de musica, para que as
 * faixas possam ser trocadas uma a uma so substituindo arquivos (sem codigo).
 *
 * {@code pasta}/{@code id} e o arquivo procurado na pasta externa (audio/musicas/...);
 * {@code provisorias} sao as musicas internas usadas enquanto o arquivo proprio nao existir
 * (hoje o jogo tem duas musicas proprias: "menu" e "treino").
 *
 * So existem contextos para as telas que o jogo realmente tem: a selecao de personagens
 * inclui o mapa (nao ha tela separada de cenarios) e o menu JOGAR leva direto a ela.
 */
public enum ContextoMusical {
    MENU_INICIAL("menus", "menu_inicial", "menu"),
    MENU_PRINCIPAL("menus", "menu_principal", "menu"),
    SELECAO_PERSONAGENS("menus", "selecao_personagens", "menu"),
    MENU_TREINAMENTO("menus", "menu_treinamento", "menu"),
    CRIACAO_PERSONAGENS("menus", "criacao_personagens", "menu"),
    CONFIGURACOES("menus", "configuracoes", "menu"),
    RESULTADOS("menus", "resultados", "menu"),
    TREINAMENTO("treinamento", "treinamento", "treino"),
    LABORATORIO_TECNICO("treinamento", "laboratorio_tecnico", "treino"),
    COMBATE_01("combate", "combate_01", "combate", "treino"),
    COMBATE_02("combate", "combate_02", "combate", "menu"),
    COMBATE_03("combate", "combate_03", "combate", "treino"),
    COMBATE_04("combate", "combate_04", "combate", "menu"),
    COMBATE_05("combate", "combate_05", "combate", "treino"),
    COMBATE_06("combate", "combate_06", "combate", "menu");

    private final String pasta;
    private final String id;
    private final List<String> provisorias;

    ContextoMusical(String pasta, String id, String... provisorias) {
        this.pasta = pasta;
        this.id = id;
        this.provisorias = List.of(provisorias);
    }

    public String getPasta() {
        return pasta;
    }

    public String getId() {
        return id;
    }

    /** Musicas internas (ids de /audio/musica/) usadas se o arquivo proprio nao existir. */
    public List<String> getProvisorias() {
        return provisorias;
    }

    public boolean isCombate() {
        return pasta.equals("combate");
    }

    /** As faixas do combate (rodizio aleatorio). */
    public static List<ContextoMusical> combate() {
        return List.of(COMBATE_01, COMBATE_02, COMBATE_03, COMBATE_04, COMBATE_05, COMBATE_06);
    }
}
