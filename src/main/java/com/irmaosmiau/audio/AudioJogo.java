package com.irmaosmiau.audio;

/**
 * Acesso global ao servico de audio do jogo e ids dos sons conhecidos.
 *
 * Os arquivos correspondentes ficam em src/main/resources/audio/
 * (ver LEIAME.md la). Um id sem arquivo simplesmente nao toca.
 */
public final class AudioJogo {

    // Musicas (loop)
    public static final String MUSICA_MENU = "menu";
    public static final String MUSICA_TREINO = "treino";
    public static final String MUSICA_COMBATE = "combate";

    // Efeitos
    public static final String EFEITO_PEGADA = "pegada";
    public static final String EFEITO_ROMPIMENTO = "rompimento";

    private static ServicoAudio servico;

    private AudioJogo() {
    }

    /** Servico unico, criado na primeira chamada e fechado ao sair da JVM. */
    public static synchronized ServicoAudio servico() {
        if (servico == null) {
            servico = ServicoAudio.criarPadrao();
            ServicoAudio criado = servico;
            Runtime.getRuntime().addShutdownHook(new Thread(criado::fechar, "fechar-audio"));
        }
        return servico;
    }
}
