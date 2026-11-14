package com.irmaosmiau.animacao;

/** De onde vem os dados visuais de uma animacao (Entrega 5E). */
public enum FonteAnimacao {
    /**
     * Codigo atual dos renderizadores (arrays *_MIAUSTUDIO ou desenho
     * procedural como a caminhada senoidal). Executado pelo proprio
     * IrmaoMiauBranco/Preto, intocado: e o FALLBACK.
     */
    LEGADA_CODIGO,
    /** Arquivo de animacao individual (conteudo/animacoes/individuais/*.json). */
    JSON_INDIVIDUAL,
    /** Animacao conjunta do MiauStudio (PEGADA_1.json via RegistroAnimacoesCombate). */
    MIAUSTUDIO_CONJUNTA
}
