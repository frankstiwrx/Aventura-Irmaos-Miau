package com.irmaosmiau.persistencia;

import com.irmaosmiau.conteudo.json.ObjetoJson;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.TipoDispositivo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Arquivo {@code dispositivos.json} (Entrega 5D), separado de
 * configuracoes.json e controles.json para nao mudar os formatos ja salvos.
 *
 * <pre>
 * {
 *   "tipo": "DISPOSITIVOS_IRMAOS_MIAU",
 *   "versaoEsquema": 1,
 *   "jogadores": { "jogador1": "TECLADO", "jogador2": "GAMEPAD" }
 * }
 * </pre>
 *
 * Guarda so a ESCOLHA (teclado ou gamepad); qual gamepad fisico e decidido
 * na partida, entao um controle desconectado nunca deixa o arquivo invalido.
 * Jogador ausente = TECLADO. Valores ou campos desconhecidos sao erro (o
 * arquivo nao e alterado).
 */
public final class RepositorioDispositivos {

    public static final String NOME_ARQUIVO = "dispositivos.json";
    public static final String TIPO = "DISPOSITIVOS_IRMAOS_MIAU";
    public static final int VERSAO_ESQUEMA = 1;

    private static final Set<String> CAMPOS_RAIZ = Set.of(
            ArmazenamentoJson.CAMPO_TIPO, ArmazenamentoJson.CAMPO_VERSAO, "jogadores");

    private final Path arquivo;
    private final ArmazenamentoJson armazenamento;

    public RepositorioDispositivos(DiretorioDados diretorio) {
        this.arquivo = diretorio.resolver(NOME_ARQUIVO);
        this.armazenamento = new ArmazenamentoJson();
    }

    public Path getArquivo() {
        return arquivo;
    }

    /** Arquivo ausente = os dois no teclado (nada e criado). */
    public AssociacaoDispositivos carregar() {
        Optional<ObjetoJson> doc = armazenamento.ler(arquivo, TIPO, VERSAO_ESQUEMA);
        return doc.map(this::converter).orElseGet(AssociacaoDispositivos::padrao);
    }

    public void salvar(AssociacaoDispositivos associacao) {
        if (associacao == null) {
            throw new IllegalArgumentException("A associacao de dispositivos e obrigatoria.");
        }
        Map<String, Object> jogadores = new LinkedHashMap<>();
        for (SlotJogador s : SlotJogador.values()) {
            jogadores.put(s.getIdArquivo(), associacao.tipo(s).name());
        }
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("jogadores", jogadores);
        armazenamento.gravar(arquivo, TIPO, VERSAO_ESQUEMA, dados);
    }

    /** Guarda o arquivo problematico como copia; null se ele sumiu. */
    public Path guardarComoCorrompido() {
        return Files.exists(arquivo) ? armazenamento.guardarComoCorrompido(arquivo) : null;
    }

    private AssociacaoDispositivos converter(ObjetoJson doc) {
        try {
            RepositorioConfiguracoes.rejeitarDesconhecidos(doc, CAMPOS_RAIZ);
            AssociacaoDispositivos a = AssociacaoDispositivos.padrao();
            if (doc.contem("jogadores")) {
                ObjetoJson j = doc.objeto("jogadores");
                for (String id : j.chaves()) {
                    SlotJogador s = SlotJogador.porIdArquivo(id);
                    if (s == null) {
                        throw j.erro(id, "jogador desconhecido");
                    }
                    String valor = j.texto(id);
                    TipoDispositivo tipo = null;
                    for (TipoDispositivo t : TipoDispositivo.values()) {
                        if (t.name().equals(valor)) {
                            tipo = t;
                        }
                    }
                    if (tipo == null) {
                        throw j.erro(id, "dispositivo desconhecido \"" + valor + "\"");
                    }
                    a = a.com(s, tipo);
                }
            }
            return a;
        } catch (IllegalArgumentException ex) {
            throw new PersistenciaException(PersistenciaException.Motivo.DADOS_INVALIDOS, arquivo,
                    ex.getMessage(), ex);
        }
    }
}
