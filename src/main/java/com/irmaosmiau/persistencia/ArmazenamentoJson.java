package com.irmaosmiau.persistencia;

import com.irmaosmiau.conteudo.ConteudoInvalidoException;
import com.irmaosmiau.conteudo.json.EscritorJson;
import com.irmaosmiau.conteudo.json.ObjetoJson;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Leitura e gravacao SEGURA de documentos JSON versionados.
 *
 * Todo documento comeca com:
 * <pre>
 * { "tipo": "...", "versaoEsquema": N, ...dados... }
 * </pre>
 *
 * Gravacao: o JSON e montado inteiro em memoria, escrito num arquivo
 * temporario na mesma pasta, forcado ao disco e so entao move-se sobre o
 * arquivo final (troca atomica quando o sistema permite). Se qualquer
 * passo falhar, o arquivo antigo continua intacto e o temporario e apagado.
 */
public final class ArmazenamentoJson {

    public static final String CAMPO_TIPO = "tipo";
    public static final String CAMPO_VERSAO = "versaoEsquema";

    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /* Somente para testes: simula uma queda entre escrever e substituir. */
    private Runnable antesDeSubstituir = () -> { };

    void setAntesDeSubstituirParaTestes(Runnable acao) {
        antesDeSubstituir = acao == null ? () -> { } : acao;
    }

    // =====================================================
    // LEITURA
    // =====================================================
    /**
     * @return vazio se o arquivo nao existe
     * @throws PersistenciaException ARQUIVO_CORROMPIDO, VERSAO_INCOMPATIVEL ou ERRO_DE_ARQUIVO
     */
    public Optional<ObjetoJson> ler(Path arquivo, String tipoEsperado, int versaoSuportada) {
        if (!Files.exists(arquivo)) {
            return Optional.empty();
        }
        String texto;
        try {
            texto = Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new PersistenciaException(PersistenciaException.Motivo.ERRO_DE_ARQUIVO, arquivo,
                    "nao foi possivel ler: " + ex.getMessage(), ex);
        }

        ObjetoJson doc;
        try {
            doc = ObjetoJson.ler(texto, arquivo.getFileName().toString());
        } catch (ConteudoInvalidoException ex) {
            throw new PersistenciaException(PersistenciaException.Motivo.ARQUIVO_CORROMPIDO, arquivo,
                    "arquivo corrompido: " + ex.getMessage(), ex);
        }

        String tipo;
        try {
            tipo = doc.textoOpcional(CAMPO_TIPO, null);
        } catch (ConteudoInvalidoException ex) {
            tipo = null;
        }
        if (!tipoEsperado.equals(tipo)) {
            throw new PersistenciaException(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL, arquivo,
                    "tipo de arquivo inesperado: \"" + tipo + "\" (esperado \"" + tipoEsperado + "\")");
        }
        if (!doc.contem(CAMPO_VERSAO)) {
            throw new PersistenciaException(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL, arquivo,
                    "campo \"" + CAMPO_VERSAO + "\" ausente");
        }
        int versao;
        try {
            versao = doc.inteiro(CAMPO_VERSAO);
        } catch (ConteudoInvalidoException ex) {
            throw new PersistenciaException(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL, arquivo,
                    ex.getMessage(), ex);
        }
        if (versao > versaoSuportada) {
            throw new PersistenciaException(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL, arquivo,
                    "versao de esquema " + versao + " criada por uma versao mais nova do jogo"
                    + " (esta versao entende ate " + versaoSuportada + ")");
        }
        if (versao < 1) {
            throw new PersistenciaException(PersistenciaException.Motivo.VERSAO_INCOMPATIVEL, arquivo,
                    "versao de esquema invalida: " + versao);
        }
        // Versoes antigas (versao < suportada) entram aqui quando houver migracoes.
        return Optional.of(doc);
    }

    // =====================================================
    // GRAVACAO
    // =====================================================
    /**
     * Grava o documento com tipo e versao no inicio.
     *
     * @throws PersistenciaException ERRO_DE_ARQUIVO (o arquivo anterior fica intacto)
     * @throws IllegalArgumentException se os dados tiverem tipos nao suportados
     *         (nada e escrito no disco)
     */
    public void gravar(Path arquivo, String tipo, int versao, Map<String, Object> dados) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put(CAMPO_TIPO, tipo);
        doc.put(CAMPO_VERSAO, versao);
        for (Map.Entry<String, Object> e : dados.entrySet()) {
            if (doc.containsKey(e.getKey())) {
                throw new IllegalArgumentException("Campo reservado nos dados: " + e.getKey());
            }
            doc.put(e.getKey(), e.getValue());
        }

        // 1) Serializa antes de tocar no disco.
        byte[] bytes = EscritorJson.escrever(doc).getBytes(StandardCharsets.UTF_8);

        Path pasta = arquivo.toAbsolutePath().getParent();
        Path temporario = null;
        try {
            // 2) Cria as pastas necessarias.
            Files.createDirectories(pasta);

            // 3) Escreve num temporario da mesma pasta e forca ao disco.
            temporario = Files.createTempFile(pasta, arquivo.getFileName().toString() + ".", ".tmp");
            try (FileChannel canal = FileChannel.open(temporario,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    canal.write(buffer);
                }
                canal.force(true);
            }

            antesDeSubstituir.run();

            // 4) Substitui o arquivo final.
            substituir(temporario, arquivo);
            temporario = null;
        } catch (IOException | RuntimeException ex) {
            throw new PersistenciaException(PersistenciaException.Motivo.ERRO_DE_ARQUIVO, arquivo,
                    "falha ao gravar (o arquivo anterior foi preservado): " + ex.getMessage(), ex);
        } finally {
            if (temporario != null) {
                try {
                    Files.deleteIfExists(temporario);
                } catch (IOException ignorado) {
                    // O temporario sobra com extensao .tmp; nunca e lido pelo jogo.
                }
            }
        }
    }

    /**
     * Troca atomica quando possivel. No Windows, antivirus/indexador podem
     * segurar o arquivo por alguns milissegundos (AccessDeniedException):
     * tenta de novo algumas vezes antes de desistir.
     */
    private static void substituir(Path origem, Path destino) throws IOException {
        int tentativas = 0;
        while (true) {
            try {
                try {
                    Files.move(origem, destino,
                            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (AccessDeniedException ex) {
                if (++tentativas >= 5) {
                    throw ex;
                }
                try {
                    Thread.sleep(40L * tentativas);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
    }

    // =====================================================
    // CONSERVACAO DE ARQUIVOS PROBLEMATICOS
    // =====================================================
    /**
     * Renomeia um arquivo problematico para
     * {@code <nome>.corrompido-<data-hora>} na mesma pasta, em vez de
     * apaga-lo. Devolve o novo caminho.
     */
    public Path guardarComoCorrompido(Path arquivo) {
        String nome = arquivo.getFileName().toString() + ".corrompido-" + LocalDateTime.now().format(CARIMBO);
        Path destino = arquivo.resolveSibling(nome);
        int n = 1;
        while (Files.exists(destino)) {
            destino = arquivo.resolveSibling(nome + "-" + n++);
        }
        try {
            return Files.move(arquivo, destino);
        } catch (IOException ex) {
            throw new PersistenciaException(PersistenciaException.Motivo.ERRO_DE_ARQUIVO, arquivo,
                    "nao foi possivel guardar uma copia do arquivo problematico: " + ex.getMessage(), ex);
        }
    }
}
