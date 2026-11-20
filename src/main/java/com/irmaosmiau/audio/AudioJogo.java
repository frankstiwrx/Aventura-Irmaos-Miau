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
    private static TrilhaSonora trilhas;

    /** 5Q.6: a trilha sonora por contexto (menus, treino, laboratorio, combate, resultados). */
    public static synchronized TrilhaSonora trilhas() {
        if (trilhas == null) {
            trilhas = TrilhaSonora.padrao(servico());
        }
        return trilhas;
    }

    /** 5Q.6: troca a trilha (testes). */
    public static synchronized void instalarTrilhas(TrilhaSonora t) {
        trilhas = t;
    }

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
