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
 *
 * Entrega 5G (novos padroes: R3 rola, L3 deita, A = pulo normal, RB + A =
 * pulo alto): um jogador cujo arquivo ainda e EXATAMENTE o padrao antigo
 * (nunca personalizado) passa para o padrao novo, com aviso. Um jogador
 * personalizado fica como esta, com aviso de que ha padroes novos.
 * O arquivo so muda quando o jogador salvar.
 */
public final class RepositorioGamepad {

    public static final String NOME_ARQUIVO = "gamepad.json";
    public static final String TIPO = "MAPEAMENTO_GAMEPAD_IRMAOS_MIAU";
    public static final int VERSAO_ESQUEMA = 1;

    private static final Set<String> CAMPOS_RAIZ = Set.of(
            ArmazenamentoJson.CAMPO_TIPO, ArmazenamentoJson.CAMPO_VERSAO, "jogadores");
    private static final Set<String> CAMPOS_SLOT = Set.of("esquema", "botoes", "esquemaEscolhido");
    /**
     * 5O.2: gravado como true ao SALVAR em CONTROLES (o jogador escolheu o esquema).
     * Arquivos antigos nao tem: um slot no Classico com os botoes padrao e sem esta
     * marca e tratado como "nunca escolhido" e passa para o Contextual (novo padrao).
     */
    public static final String CAMPO_ESCOLHIDO = "esquemaEscolhido";

    private final Path arquivo;
    private final ArmazenamentoJson armazenamento = new ArmazenamentoJson();

    public RepositorioGamepad(DiretorioDados diretorio) {
        this.arquivo = diretorio.resolver(NOME_ARQUIVO);
    }

    public Path getArquivo() {
        return arquivo;
    }

    /** Mapa lido + avisos de atualizacao de padroes (5G). */
    public record Leitura(MapaGamepad mapa, java.util.List<String> avisos) {
    }

    /** Sem arquivo = os dois no CLASSICO (nada e criado). */
    public MapaGamepad carregar() {
        return carregarComAvisos().mapa();
    }

    public Leitura carregarComAvisos() {
        Optional<ObjetoJson> doc = armazenamento.ler(arquivo, TIPO, VERSAO_ESQUEMA);
        if (doc.isEmpty()) {
            return new Leitura(MapaGamepad.padrao(), java.util.List.of());
        }
        MapaGamepad lido = converter(doc.get());
        java.util.List<String> avisos = new java.util.ArrayList<>();
        java.util.Set<String> noArquivo = doc.get().contem("jogadores")
                ? doc.get().objeto("jogadores").chaves() : java.util.Set.of();
        for (SlotJogador s : SlotJogador.values()) {
            if (!noArquivo.contains(s.getIdArquivo())) {
                continue;
            }
            EsquemaGamepad e = lido.esquema(s);
            // 5O.2: Classico padrao (de qualquer versao) e nunca escolhido = o novo padrao Contextual.
            boolean escolhido = doc.get().objeto("jogadores").objeto(s.getIdArquivo())
                    .booleanoOpcional(CAMPO_ESCOLHIDO, false);
            if (!escolhido && e == EsquemaGamepad.CLASSICO && e != MapaGamepad.ESQUEMA_PADRAO
                    && ehPadraoDeAlgumaVersao(lido.config(s), s, e)) {
                lido = lido.comEsquema(s, MapaGamepad.ESQUEMA_PADRAO);
                avisos.add(s.getSigla() + ": o esquema padrão agora é o CONTEXTUAL (era o Clássico, nunca escolhido)."
                        + " Para voltar: CONTROLES → esquema Clássico → SALVAR.");
                continue;
            }
            boolean ehAtual = lido.config(s).equals(MapaGamepad.padraoDo(s, e));
            boolean padraoAntigo = !ehAtual && (lido.config(s).equals(MapaGamepad.padraoAnterior(s, e))
                    || lido.config(s).equals(MapaGamepad.padrao5G(s, e))
                    || lido.config(s).equals(MapaGamepad.padrao5H(s, e))
                    || lido.config(s).equals(MapaGamepad.padrao5N(s, e))
                    || lido.config(s).equals(MapaGamepad.padrao5O(s, e)));
            if (padraoAntigo) {
                lido = lido.comEsquema(s, e);
                avisos.add(s.getSigla() + ": botões padrão do gamepad atualizados (R3 rola, L3 deita, A pula,"
                        + " RB + A pulo alto" + (e == EsquemaGamepad.CONTEXTUAL
                                ? "; técnicas no analógico direito, arrasto automático" : "")
                        + "). Salve em CONTROLES para gravar.");
            } else if (!ehAtual && e == EsquemaGamepad.CLASSICO
                    && lido.botao(s, ItemGamepad.de(AcaoJogador.INICIAR_PEGADA)).isEmpty()) {
                avisos.add(s.getSigla() + ": seus botões personalizados foram mantidos. Agora os dois jogadores"
                        + " aplicam e rompem pegadas: atribua botões em CONTROLES ou use RESTAURAR PADRÕES.");
            } else if (lido.botao(s, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO)).isEmpty()) {
                avisos.add(s.getSigla() + ": seus botões personalizados foram mantidos. Os novos padrões"
                        + " (R3 rola, L3 deita, RB + A pulo alto) estão em CONTROLES → RESTAURAR PADRÕES.");
            }
        }
        return new Leitura(lido, java.util.List.copyOf(avisos));
    }

    private static boolean ehPadraoDeAlgumaVersao(MapaGamepad.ConfigSlot c, SlotJogador s, EsquemaGamepad e) {
        return c.equals(MapaGamepad.padraoDo(s, e)) || c.equals(MapaGamepad.padraoAnterior(s, e))
                || c.equals(MapaGamepad.padrao5G(s, e)) || c.equals(MapaGamepad.padrao5H(s, e))
                || c.equals(MapaGamepad.padrao5N(s, e)) || c.equals(MapaGamepad.padrao5O(s, e));
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
            slot.put(CAMPO_ESCOLHIDO, Boolean.TRUE);
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
                        if (a == AcaoJogador.ACROBACIA) {
                            continue;                     // 5O.1: obsoleta (acrobacia = R3 + direcao em pe)
                        }
                        if (a != null && s.usa(a)) {
                            acoes.put(a, botao);
                        } else if (c != null && (!c.isSoContextual() || esquema == EsquemaGamepad.CONTEXTUAL)) {
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
