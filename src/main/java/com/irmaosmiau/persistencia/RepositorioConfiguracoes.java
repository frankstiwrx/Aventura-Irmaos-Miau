package com.irmaosmiau.persistencia;

import com.irmaosmiau.conteudo.json.ObjetoJson;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Arquivo {@code configuracoes.json} na pasta de dados.
 *
 * <pre>
 * {
 *   "tipo": "CONFIGURACOES_IRMAOS_MIAU",
 *   "versaoEsquema": 1,
 *   "audio": { "musicaAtiva": true, "efeitosAtivos": true,
 *              "volumeMusica": 0.6, "volumeEfeitos": 0.8 },
 *   "visual": { "cenarioPreferido": "dojo" }
 * }
 * </pre>
 */
public final class RepositorioConfiguracoes {

    public static final String NOME_ARQUIVO = "configuracoes.json";
    public static final String TIPO = "CONFIGURACOES_IRMAOS_MIAU";
    public static final int VERSAO_ESQUEMA = 1;

    private static final Set<String> CAMPOS_RAIZ = Set.of(
            ArmazenamentoJson.CAMPO_TIPO, ArmazenamentoJson.CAMPO_VERSAO, "audio", "visual");
    private static final Set<String> CAMPOS_AUDIO = Set.of(
            "musicaAtiva", "efeitosAtivos", "volumeMusica", "volumeEfeitos");
    private static final Set<String> CAMPOS_VISUAL = Set.of("cenarioPreferido");

    private final Path arquivo;
    private final ArmazenamentoJson armazenamento;

    public RepositorioConfiguracoes(DiretorioDados diretorio) {
        this(diretorio, new ArmazenamentoJson());
    }

    RepositorioConfiguracoes(DiretorioDados diretorio, ArmazenamentoJson armazenamento) {
        this.arquivo = diretorio.resolver(NOME_ARQUIVO);
        this.armazenamento = armazenamento;
    }

    public Path getArquivo() {
        return arquivo;
    }

    /**
     * Carrega as configuracoes. Arquivo ausente = padroes.
     *
     * @throws PersistenciaException arquivo corrompido, versao incompativel
     *         ou valores invalidos (o arquivo NAO e alterado)
     */
    public ConfiguracoesJogo carregar() {
        Optional<ObjetoJson> doc = armazenamento.ler(arquivo, TIPO, VERSAO_ESQUEMA);
        return doc.map(this::converter).orElseGet(ConfiguracoesJogo::padrao);
    }

    /**
     * Versao tolerante para o jogo: se o arquivo estiver com problema, ele e
     * guardado como {@code configuracoes.json.corrompido-<data>} e os padroes
     * sao usados. O problema volta nos avisos (nunca e silencioso).
     */
    public ResultadoCarga<ConfiguracoesJogo> carregarOuPadrao() {
        try {
            return new ResultadoCarga<>(carregar(), List.of());
        } catch (PersistenciaException ex) {
            if (ex.getMotivo() == PersistenciaException.Motivo.ERRO_DE_ARQUIVO) {
                return new ResultadoCarga<>(ConfiguracoesJogo.padrao(), List.of(
                        "Configuracoes nao puderam ser lidas; usando padroes. " + ex.getMessage()));
            }
            Path copia = armazenamento.guardarComoCorrompido(arquivo);
            return new ResultadoCarga<>(ConfiguracoesJogo.padrao(), List.of(
                    "Configuracoes invalidas foram guardadas em " + copia.getFileName()
                    + " e os padroes foram restaurados. Detalhe: " + ex.getMessage()));
        }
    }

    public void salvar(ConfiguracoesJogo c) {
        if (c == null) {
            throw new IllegalArgumentException("As configuracoes sao obrigatorias.");
        }
        Map<String, Object> audio = new LinkedHashMap<>();
        audio.put("musicaAtiva", c.isMusicaAtiva());
        audio.put("efeitosAtivos", c.isEfeitosAtivos());
        audio.put("volumeMusica", c.getVolumeMusica());
        audio.put("volumeEfeitos", c.getVolumeEfeitos());

        Map<String, Object> visual = new LinkedHashMap<>();
        visual.put("cenarioPreferido", c.getCenarioPreferido());

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("audio", audio);
        dados.put("visual", visual);
        armazenamento.gravar(arquivo, TIPO, VERSAO_ESQUEMA, dados);
    }

    private ConfiguracoesJogo converter(ObjetoJson doc) {
        try {
            rejeitarDesconhecidos(doc, CAMPOS_RAIZ);
            ConfiguracoesJogo p = ConfiguracoesJogo.padrao();

            // Secoes e campos ausentes usam o padrao (arquivos parciais sao aceitos).
            boolean musica = p.isMusicaAtiva();
            boolean efeitos = p.isEfeitosAtivos();
            double volMusica = p.getVolumeMusica();
            double volEfeitos = p.getVolumeEfeitos();
            String cenario = p.getCenarioPreferido();

            if (doc.contem("audio")) {
                ObjetoJson a = doc.objeto("audio");
                rejeitarDesconhecidos(a, CAMPOS_AUDIO);
                musica = a.booleanoOpcional("musicaAtiva", musica);
                efeitos = a.booleanoOpcional("efeitosAtivos", efeitos);
                volMusica = a.decimalOpcional("volumeMusica", volMusica);
                volEfeitos = a.decimalOpcional("volumeEfeitos", volEfeitos);
            }
            if (doc.contem("visual")) {
                ObjetoJson v = doc.objeto("visual");
                rejeitarDesconhecidos(v, CAMPOS_VISUAL);
                cenario = v.textoOpcional("cenarioPreferido", cenario);
            }
            return new ConfiguracoesJogo(musica, efeitos, volMusica, volEfeitos, cenario);
        } catch (IllegalArgumentException ex) {
            // Inclui ConteudoInvalidoException (tipos errados no JSON).
            throw new PersistenciaException(PersistenciaException.Motivo.DADOS_INVALIDOS, arquivo,
                    ex.getMessage(), ex);
        }
    }

    /**
     * Campos desconhecidos sao erro: aceita-los e salvar de novo os apagaria
     * sem aviso.
     */
    static void rejeitarDesconhecidos(ObjetoJson objeto, Set<String> permitidos) {
        Set<String> extras = new LinkedHashSet<>(objeto.chaves());
        extras.removeAll(permitidos);
        if (!extras.isEmpty()) {
            throw objeto.erro(null, "campo(s) desconhecido(s) " + extras);
        }
    }
}
