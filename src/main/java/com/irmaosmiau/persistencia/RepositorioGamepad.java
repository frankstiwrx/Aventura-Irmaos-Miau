package com.irmaosmiau.persistencia;

import com.irmaosmiau.conteudo.json.ObjetoJson;
import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.ComandoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.dispositivos.MapaGamepad;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Arquivo {@code gamepad.json} (Entrega 5F): esquema e botoes de cada slot.
 * Separado de controles.json e dispositivos.json (formatos anteriores intactos).
 * NAO e o arquivo SDL de reconhecimento de joysticks (gamecontrollerdb.txt).
 *
 * <pre>
 * {
 *   "tipo": "MAPEAMENTO_GAMEPAD_IRMAOS_MIAU",
 *   "versaoEsquema": 1,
 *   "jogadores": {
 *     "jogador1": { "esquema": "CONTEXTUAL",
 *                   "botoes": { "PULAR": "SUL", "TECNICA_CIMA": "DPAD_CIMA", ... } },
 *     "jogador2": { "esquema": "CLASSICO", "botoes": { ... } }
 *   }
 * }
 * </pre>
 *
 * Jogador ausente = padrao CLASSICO. Item ausente = sem botao (nenhum botao e
 * obrigatorio). Nomes desconhecidos e conflitos sao erro: o arquivo nao e
 * alterado e os padroes sao usados.
 */
public final class RepositorioGamepad {

    public static final String NOME_ARQUIVO = "gamepad.json";
    public static final String TIPO = "MAPEAMENTO_GAMEPAD_IRMAOS_MIAU";
    public static final int VERSAO_ESQUEMA = 1;

    private static final Set<String> CAMPOS_RAIZ = Set.of(
            ArmazenamentoJson.CAMPO_TIPO, ArmazenamentoJson.CAMPO_VERSAO, "jogadores");
    private static final Set<String> CAMPOS_SLOT = Set.of("esquema", "botoes");

    private final Path arquivo;
    private final ArmazenamentoJson armazenamento = new ArmazenamentoJson();

    public RepositorioGamepad(DiretorioDados diretorio) {
        this.arquivo = diretorio.resolver(NOME_ARQUIVO);
    }

    public Path getArquivo() {
        return arquivo;
    }

    /** Sem arquivo = os dois no CLASSICO (nada e criado). */
    public MapaGamepad carregar() {
        Optional<ObjetoJson> doc = armazenamento.ler(arquivo, TIPO, VERSAO_ESQUEMA);
        return doc.map(this::converter).orElseGet(MapaGamepad::padrao);
    }

    public void salvar(MapaGamepad mapa) {
        if (mapa == null || !mapa.isValido()) {
            throw new IllegalArgumentException("Mapeamento de gamepad com conflitos: "
                    + (mapa == null ? "nulo" : mapa.problemas().stream()
                            .map(MapaGamepad.Problema::descricao).collect(Collectors.joining("; "))));
        }
        Map<String, Object> jogadores = new LinkedHashMap<>();
        for (SlotJogador s : SlotJogador.values()) {
            Map<String, Object> botoes = new LinkedHashMap<>();
            for (ItemGamepad i : mapa.itens(s)) {
                mapa.botao(s, i).ifPresent(b -> botoes.put(i.nome(), b.name()));
            }
            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("esquema", mapa.esquema(s).name());
            slot.put("botoes", botoes);
            jogadores.put(s.getIdArquivo(), slot);
        }
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("jogadores", jogadores);
        armazenamento.gravar(arquivo, TIPO, VERSAO_ESQUEMA, dados);
    }

    public Path guardarComoCorrompido() {
        return Files.exists(arquivo) ? armazenamento.guardarComoCorrompido(arquivo) : null;
    }

    private MapaGamepad converter(ObjetoJson doc) {
        try {
            RepositorioConfiguracoes.rejeitarDesconhecidos(doc, CAMPOS_RAIZ);
            MapaGamepad mapa = MapaGamepad.padrao();
            if (!doc.contem("jogadores")) {
                return mapa;
            }
            ObjetoJson jogadores = doc.objeto("jogadores");
            for (String id : jogadores.chaves()) {
                SlotJogador s = SlotJogador.porIdArquivo(id);
                if (s == null) {
                    throw jogadores.erro(id, "jogador desconhecido");
                }
                ObjetoJson o = jogadores.objeto(id);
                RepositorioConfiguracoes.rejeitarDesconhecidos(o, CAMPOS_SLOT);
                EsquemaGamepad esquema = valor(EsquemaGamepad.class, o.texto("esquema"), o, "esquema");
                Map<AcaoJogador, BotaoGamepad> acoes = new EnumMap<>(AcaoJogador.class);
                Map<ComandoGamepad, BotaoGamepad> comandos = new EnumMap<>(ComandoGamepad.class);
                if (o.contem("botoes")) {
                    ObjetoJson b = o.objeto("botoes");
                    for (String nome : b.chaves()) {
                        BotaoGamepad botao = valor(BotaoGamepad.class, b.texto(nome), b, nome);
                        AcaoJogador a = constante(AcaoJogador.class, nome);
                        ComandoGamepad c = constante(ComandoGamepad.class, nome);
                        if (a != null && s.usa(a)) {
                            acoes.put(a, botao);
                        } else if (c != null && esquema == EsquemaGamepad.CONTEXTUAL) {
                            comandos.put(c, botao);
                        } else {
                            throw b.erro(nome, "item desconhecido para " + id + " no esquema " + esquema);
                        }
                    }
                }
                mapa = mapa.comConfig(s, new MapaGamepad.ConfigSlot(esquema, acoes, comandos));
            }
            if (!mapa.isValido()) {
                throw new IllegalArgumentException(mapa.problemas().stream()
                        .map(MapaGamepad.Problema::descricao).collect(Collectors.joining("; ")));
            }
            return mapa;
        } catch (IllegalArgumentException ex) {
            throw new PersistenciaException(PersistenciaException.Motivo.DADOS_INVALIDOS, arquivo,
                    ex.getMessage(), ex);
        }
    }

    private static <E extends Enum<E>> E constante(Class<E> tipo, String nome) {
        for (E e : tipo.getEnumConstants()) {
            if (e.name().equals(nome)) {
                return e;
            }
        }
        return null;
    }

    private static <E extends Enum<E>> E valor(Class<E> tipo, String nome, ObjetoJson o, String campo) {
        E e = constante(tipo, nome);
        if (e == null) {
            throw o.erro(campo, "valor desconhecido \"" + nome + "\"");
        }
        return e;
    }
}
