package com.irmaosmiau.posicional;

/**
 * Entrega 5P.2: pre-condicoes de uma tecnica alem da posicao e do papel
 * (o MotorSolo confere em {@code motivoSem}; o HUD mostra a tecnica cinza
 * quando falta alguma). Nada aparece "a distancia" nem sem o contato certo.
 */
public enum RequisitoTecnica {
    /** Em pe, pegada ESTABELECIDA e quem inicia e quem aplica a pegada. */
    PEGADA_APLICADOR("SEM PEGADA"),
    /** Pegada das duas mangas (PEGADA_3) aplicada por quem inicia. */
    PEGADA_MANGAS("PRECISA DAS MANGAS"),
    /** Pegada cruzada (PEGADA_2) ou na nuca (PEGADA_4): controle das costas/pescoco para girar o quadril. */
    PEGADA_CRUZADA_OU_NUCA("PRECISA CRUZADA/NUCA"),
    /** Conexao de manga no chao (pegar a manga, ou ter caido com a pegada das mangas). */
    CONEXAO_MANGA("SEM A MANGA"),
    /** Perto o bastante para puxar (ou com qualquer pegada). */
    PERTO_PARA_PUXAR("LONGE"),
    /** Perto e na mesma faixa para conectar/entrar contra quem esta no chao. */
    PERTO_NO_CHAO("LONGE");

    private final String motivo;

    RequisitoTecnica(String motivo) {
        this.motivo = motivo;
    }

    /** Motivo curto da recusa (HUD/rodape). */
    public String getMotivo() {
        return motivo;
    }
}
