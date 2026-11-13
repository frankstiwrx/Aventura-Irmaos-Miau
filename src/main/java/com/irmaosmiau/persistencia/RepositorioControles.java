package com.irmaosmiau.persistencia;

import com.irmaosmiau.conteudo.json.ObjetoJson;
import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.ProblemaControle;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Arquivo {@code controles.json} na pasta de dados (Entrega 5C), separado
 * de configuracoes.json para nao mudar o formato ja salvo dos jogadores.
 *
 * <pre>
 * {
 *   "tipo": "CONTROLES_IRMAOS_MIAU",
 *   "versaoEsquema": 1,
 *   "jogadores": {
 *     "jogador1": { "MOVER_ESQUERDA": "A", "INICIAR_PEGADA": "H", ... },
 *     "jogador2": { "MOVER_ESQUERDA": "LEFT", "ROMPIMENTO_TECNICO": "U", ... }
 *   }
 * }
 * </pre>
 *
 * Compatibilidade: uma acao que nao existe no arquivo (arquivo de uma versao
 * anterior a acao) recebe a tecla padrao se ela estiver livre; se estiver
 * ocupada, fica sem tecla e o jogador e avisado na tela de controles. Nomes
 * de acao, slot ou tecla desconhecidos e conflitos sao erro: o arquivo nao e
 * alterado e os padroes sao usados.
 */
public final class RepositorioControles {

    public static final String NOME_ARQUIVO = "controles.json";
    public static final String TIPO = "CONTROLES_IRMAOS_MIAU";
    public static final int VERSAO_ESQUEMA = 1;

    private static final Set<String> CAMPOS_RAIZ = Set.of(
            ArmazenamentoJson.CAMPO_TIPO, ArmazenamentoJson.CAMPO_VERSAO, "jogadores");

    /** Mapa lido e acoes que precisaram de migracao (para avisar o jogador). */
    public record Leitura(MapaControles mapa, List<String> avisos) {
    }

    private final Path arquivo;
    private final ArmazenamentoJson armazenamento;

    public RepositorioControles(DiretorioDados diretorio) {
        this(diretorio, new ArmazenamentoJson());
    }

    RepositorioControles(DiretorioDados diretorio, ArmazenamentoJson armazenamento) {
        this.arquivo = diretorio.resolver(NOME_ARQUIVO);
        this.armazenamento = armazenamento;
    }

    public Path getArquivo() {
        return arquivo;
    }

    /**
     * Arquivo ausente = padroes (nada e criado).
     *
     * @throws PersistenciaException arquivo corrompido, versao incompativel,
     *         nomes desconhecidos ou conflitos (o arquivo NAO e alterado)
     */
    public Leitura carregar() {
        Optional<ObjetoJson> doc = armazenamento.ler(arquivo, TIPO, VERSAO_ESQUEMA);
        return doc.map(this::converter).orElseGet(() -> new Leitura(MapaControles.padrao(), List.of()));
    }

    /** Guarda o arquivo problematico como copia (antes de gravar um novo); null se ele sumiu. */
    public Path guardarComoCorrompido() {
        return java.nio.file.Files.exists(arquivo) ? armazenamento.guardarComoCorrompido(arquivo) : null;
    }

    /**
     * @throws IllegalArgumentException mapa com conflitos (nada e gravado)
     * @throws PersistenciaException falha de disco (o arquivo anterior fica intacto)
     */
    public void salvar(MapaControles mapa) {
        if (mapa == null) {
            throw new IllegalArgumentException("Os controles sao obrigatorios.");
        }
        if (!mapa.isValido()) {
            throw new IllegalArgumentException("Controles com conflitos: "
                    + mapa.problemas().stream().map(ProblemaControle::descricao).collect(Collectors.joining("; ")));
        }
        Map<String, Object> jogadores = new LinkedHashMap<>();
        for (SlotJogador s : SlotJogador.values()) {
            Map<String, Object> acoes = new LinkedHashMap<>();
            for (AcaoJogador a : s.acoes()) {
                acoes.put(a.name(), mapa.tecla(s, a).orElseThrow().getNome());
            }
            jogadores.put(s.getIdArquivo(), acoes);
        }
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("jogadores", jogadores);
        armazenamento.gravar(arquivo, TIPO, VERSAO_ESQUEMA, dados);
    }

    private Leitura converter(ObjetoJson doc) {
        try {
            RepositorioConfiguracoes.rejeitarDesconhecidos(doc, CAMPOS_RAIZ);
            MapaControles mapa = MapaControles.semTeclas();
            List<AcaoFaltando> faltando = new ArrayList<>();

            ObjetoJson jogadores = doc.contem("jogadores") ? doc.objeto("jogadores") : null;
            if (jogadores != null) {
                for (String idSlot : jogadores.chaves()) {
                    if (SlotJogador.porIdArquivo(idSlot) == null) {
                        throw jogadores.erro(idSlot, "jogador desconhecido");
                    }
                }
            }
            for (SlotJogador s : SlotJogador.values()) {
                ObjetoJson acoes = jogadores != null && jogadores.contem(s.getIdArquivo())
                        ? jogadores.objeto(s.getIdArquivo()) : null;
                if (acoes != null) {
                    for (String nomeAcao : acoes.chaves()) {
                        AcaoJogador a = acaoPorNome(nomeAcao);
                        if (a == null || !s.usa(a)) {
                            throw acoes.erro(nomeAcao, "acao desconhecida para " + s.getIdArquivo());
                        }
                        String nomeTecla = acoes.texto(nomeAcao);
                        TeclaFisica t = TeclaFisica.porNome(nomeTecla).orElseThrow(
                                () -> acoes.erro(nomeAcao, "tecla nao suportada \"" + nomeTecla + "\""));
                        mapa = mapa.comTecla(s, a, t);
                    }
                }
                for (AcaoJogador a : s.acoes()) {
                    if (acoes == null || !acoes.contem(a.name())) {
                        faltando.add(new AcaoFaltando(s, a));
                    }
                }
            }

            List<ProblemaControle> problemas = mapa.problemas().stream()
                    .filter(p -> p.tipo() != ProblemaControle.Tipo.SEM_TECLA).toList();
            if (!problemas.isEmpty()) {
                throw new IllegalArgumentException(problemas.stream()
                        .map(ProblemaControle::descricao).collect(Collectors.joining("; ")));
            }

            // Acoes ausentes (arquivo antigo): padrao se a tecla estiver livre.
            List<String> avisos = new ArrayList<>();
            for (AcaoFaltando f : faltando) {
                TeclaFisica padrao = MapaControles.teclaPadrao(f.slot(), f.acao());
                if (mapa.quemUsa(padrao).isEmpty()) {
                    mapa = mapa.comTecla(f.slot(), f.acao(), padrao);
                } else {
                    avisos.add(f.slot().getSigla() + " · " + f.acao().getRotulo()
                            + " ficou sem tecla (a padrão " + padrao.getRotulo()
                            + " já está em uso). Escolha uma em CONTROLES.");
                }
            }
            return new Leitura(mapa, avisos);
        } catch (IllegalArgumentException ex) {
            // Inclui ConteudoInvalidoException (tipos errados no JSON).
            throw new PersistenciaException(PersistenciaException.Motivo.DADOS_INVALIDOS, arquivo,
                    ex.getMessage(), ex);
        }
    }

    private record AcaoFaltando(SlotJogador slot, AcaoJogador acao) {
    }

    private static AcaoJogador acaoPorNome(String nome) {
        for (AcaoJogador a : AcaoJogador.values()) {
            if (a.name().equals(nome)) {
                return a;
            }
        }
        return null;
    }
}
