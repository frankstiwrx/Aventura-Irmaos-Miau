package com.irmaosmiau.audio;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Entrega 5Q.6: onde esta a musica de cada contexto. Ordem de procura:
 * <ol>
 * <li>pasta EXTERNA (substituivel sem recompilar): {@code <pasta>/<contexto>/<id>.wav}
 * (tambem .aiff/.aif/.au);</li>
 * <li>arquivo INTERNO com o mesmo id: {@code /audio/musica/<id>.wav} (dentro do JAR);</li>
 * <li>as musicas internas PROVISORIAS do contexto ("menu", "treino"...).</li>
 * </ol>
 * A pasta externa e, nesta ordem: {@code -Dirmaosmiau.musicas=<pasta>}; {@code audio/musicas}
 * ao lado do JAR; {@code audio/musicas} na pasta de onde o jogo foi aberto (NetBeans).
 * Arquivo ausente, invalido ou incompativel e pulado (o proximo candidato toca).
 */
public final class BibliotecaMusical {

    public static final String PROPRIEDADE_PASTA = "irmaosmiau.musicas";
    public static final List<String> EXTENSOES = List.of("wav", "aiff", "aif", "au");
    static final String PREFIXO_ARQUIVO = "arquivo:";
    static final String PREFIXO_INTERNO = "interno:";

    private final Path pastaExterna;
    private final Function<String, Optional<SomCarregado>> interno;
    private final Function<Path, Optional<SomCarregado>> externo;

    public BibliotecaMusical(Path pastaExterna, Function<String, Optional<SomCarregado>> interno,
            Function<Path, Optional<SomCarregado>> externo) {
        this.pastaExterna = pastaExterna;
        this.interno = interno;
        this.externo = externo;
    }

    /** A biblioteca real (pasta externa padrao, classpath e arquivos do disco). */
    public static BibliotecaMusical padrao() {
        return new BibliotecaMusical(pastaExternaPadrao(), CarregadorSom::carregar, CarregadorSom::carregarArquivo);
    }

    /** A pasta externa em uso (pode nao existir: entao so as internas tocam). */
    public Path getPastaExterna() {
        return pastaExterna;
    }

    public static Path pastaExternaPadrao() {
        String p = System.getProperty(PROPRIEDADE_PASTA, "");
        if (!p.isBlank()) {
            return Path.of(p);
        }
        try {
            File jar = new File(BibliotecaMusical.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path aoLado = (jar.isFile() ? jar.getParentFile().toPath() : jar.toPath()).resolve("audio").resolve("musicas");
            if (Files.isDirectory(aoLado)) {
                return aoLado;
            }
        } catch (Exception | Error semLocal) {
            // sem a localizacao do JAR: usa a pasta atual
        }
        return Path.of(System.getProperty("user.dir", ".")).resolve("audio").resolve("musicas");
    }

    /** Candidatos na ordem de procura (descritores "arquivo:..." ou "interno:..."). */
    public List<String> candidatos(ContextoMusical c) {
        List<String> r = new ArrayList<>();
        if (pastaExterna != null) {
            for (String ext : EXTENSOES) {
                Path f = pastaExterna.resolve(c.getPasta()).resolve(c.getId() + "." + ext);
                if (Files.isRegularFile(f)) {
                    r.add(PREFIXO_ARQUIVO + f);
                }
            }
        }
        r.add(PREFIXO_INTERNO + ServicoAudio.PASTA_MUSICAS + c.getId() + ".wav");
        for (String p : c.getProvisorias()) {
            r.add(PREFIXO_INTERNO + ServicoAudio.PASTA_MUSICAS + p + ".wav");
        }
        return r;
    }

    /** Carrega um candidato (vazio = ausente/invalido). */
    public Optional<SomCarregado> carregar(String descritor) {
        try {
            if (descritor.startsWith(PREFIXO_ARQUIVO)) {
                return externo.apply(Path.of(descritor.substring(PREFIXO_ARQUIVO.length())));
            }
            if (descritor.startsWith(PREFIXO_INTERNO)) {
                return interno.apply(descritor.substring(PREFIXO_INTERNO.length()));
            }
        } catch (RuntimeException invalido) {
            System.err.println("[audio] " + descritor + " invalido: " + invalido.getMessage());
        }
        return Optional.empty();
    }
}
