package com.irmaosmiau.persistencia;

import com.irmaosmiau.conteudo.json.ObjetoJson;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.personagens.LimitesAtributos;
import com.irmaosmiau.personagens.PerfilLutador;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Perfis personalizados, um arquivo por perfil:
 * {@code <pasta de dados>/perfis/<id>.json}.
 *
 * <pre>
 * {
 *   "tipo": "PERFIL_LUTADOR_IRMAOS_MIAU",
 *   "versaoEsquema": 1,
 *   "id": "branco_forte",
 *   "nome": "Branco Forte",
 *   "personagemBase": "miau_branco",
 *   "atributos": { "condicionamento": 35.0, "folego": 25.0, "forca": 50.0,
 *                  "velocidade": 22.0, "agilidade": 25.0, "tecnica": 25.0,
 *                  "recuperacao": 22.0, "peso": 72.0 }
 * }
 * </pre>
 *
 * Este repositorio so guarda e valida o FORMATO. Regras que dependem do
 * catalogo (personagem-base existente, ids oficiais reservados) ficam no
 * {@link ServicoPerfis}.
 */
public final class RepositorioPerfis {

    public static final String PASTA = "perfis";
    public static final String TIPO = "PERFIL_LUTADOR_IRMAOS_MIAU";
    public static final int VERSAO_ESQUEMA = 1;

    private static final Set<String> CAMPOS = Set.of(
            ArmazenamentoJson.CAMPO_TIPO, ArmazenamentoJson.CAMPO_VERSAO,
            "id", "nome", "personagemBase", "atributos");

    private final Path pasta;
    private final ArmazenamentoJson armazenamento;

    public RepositorioPerfis(DiretorioDados diretorio) {
        this(diretorio, new ArmazenamentoJson());
    }

    RepositorioPerfis(DiretorioDados diretorio, ArmazenamentoJson armazenamento) {
        this.pasta = diretorio.resolver(PASTA);
        this.armazenamento = armazenamento;
    }

    public Path getPasta() {
        return pasta;
    }

    /** Arquivo de um perfil (o id e validado antes: sem risco de sair da pasta). */
    public Path arquivoDe(String idPerfil) {
        return pasta.resolve(PerfilLutador.normalizarId(idPerfil) + ".json");
    }

    public boolean existe(String idPerfil) {
        return Files.exists(arquivoDe(idPerfil));
    }

    // =====================================================
    // CRUD
    // =====================================================
    /** @throws PersistenciaException CONFLITO se ja existir um perfil com o mesmo id */
    public void criar(PerfilLutador perfil) {
        exigirPerfil(perfil);
        if (existe(perfil.getId())) {
            throw new PersistenciaException(PersistenciaException.Motivo.CONFLITO, arquivoDe(perfil.getId()),
                    "ja existe um perfil com o id \"" + perfil.getId() + "\"");
        }
        gravar(perfil);
    }

    /** @throws PersistenciaException CONFLITO se o perfil nao existir */
    public void atualizar(PerfilLutador perfil) {
        exigirPerfil(perfil);
        if (!existe(perfil.getId())) {
            throw new PersistenciaException(PersistenciaException.Motivo.CONFLITO, arquivoDe(perfil.getId()),
                    "perfil \"" + perfil.getId() + "\" nao existe (use criar)");
        }
        gravar(perfil);
    }

    /**
     * @return vazio se nao existir
     * @throws PersistenciaException se o arquivo estiver corrompido, incompativel ou invalido
     */
    public Optional<PerfilLutador> carregar(String idPerfil) {
        Path arquivo = arquivoDe(idPerfil);
        return armazenamento.ler(arquivo, TIPO, VERSAO_ESQUEMA)
                .map(doc -> converter(doc, arquivo));
    }

    /** @return true se o arquivo existia e foi apagado */
    public boolean excluir(String idPerfil) {
        Path arquivo = arquivoDe(idPerfil);
        try {
            return Files.deleteIfExists(arquivo);
        } catch (IOException ex) {
            throw new PersistenciaException(PersistenciaException.Motivo.ERRO_DE_ARQUIVO, arquivo,
                    "nao foi possivel excluir: " + ex.getMessage(), ex);
        }
    }

    /**
     * Todos os perfis validos, ordenados por id. Arquivos com problema NAO
     * sao apagados nem alterados: aparecem nos avisos.
     */
    public ResultadoCarga<List<PerfilLutador>> listar() {
        List<PerfilLutador> perfis = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        if (!Files.isDirectory(pasta)) {
            return new ResultadoCarga<>(perfis, avisos);
        }
        Set<String> ids = new HashSet<>();
        try (DirectoryStream<Path> arquivos = Files.newDirectoryStream(pasta, "*.json")) {
            for (Path arquivo : arquivos) {
                String nome = arquivo.getFileName().toString();
                String idDoNome = nome.substring(0, nome.length() - ".json".length());
                try {
                    Optional<PerfilLutador> p = armazenamento.ler(arquivo, TIPO, VERSAO_ESQUEMA)
                            .map(doc -> converter(doc, arquivo));
                    if (p.isEmpty()) {
                        continue;
                    }
                    if (!p.get().getId().equals(idDoNome)) {
                        avisos.add(nome + ": id interno \"" + p.get().getId()
                                + "\" diferente do nome do arquivo; ignorado");
                        continue;
                    }
                    if (ids.add(p.get().getId())) {
                        perfis.add(p.get());
                    }
                } catch (PersistenciaException ex) {
                    avisos.add(nome + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new PersistenciaException(PersistenciaException.Motivo.ERRO_DE_ARQUIVO, pasta,
                    "nao foi possivel listar os perfis: " + ex.getMessage(), ex);
        }
        perfis.sort(Comparator.comparing(PerfilLutador::getId));
        return new ResultadoCarga<>(perfis, avisos);
    }

    // =====================================================
    // CONVERSAO
    // =====================================================
    private void gravar(PerfilLutador p) {
        AtributosLutador a = p.getAtributos();
        Map<String, Object> atributos = new LinkedHashMap<>();
        for (LimitesAtributos.Limite l : LimitesAtributos.todos()) {
            atributos.put(l.getNome(), l.ler(a));
        }
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", p.getId());
        dados.put("nome", p.getNome());
        dados.put("personagemBase", p.getIdPersonagemBase());
        dados.put("atributos", atributos);
        armazenamento.gravar(arquivoDe(p.getId()), TIPO, VERSAO_ESQUEMA, dados);
    }

    private PerfilLutador converter(ObjetoJson doc, Path arquivo) {
        try {
            RepositorioConfiguracoes.rejeitarDesconhecidos(doc, CAMPOS);
            ObjetoJson a = doc.objeto("atributos");
            Set<String> nomes = new HashSet<>();
            for (LimitesAtributos.Limite l : LimitesAtributos.todos()) {
                nomes.add(l.getNome());
            }
            RepositorioConfiguracoes.rejeitarDesconhecidos(a, nomes);

            AtributosLutador atributos = new AtributosLutador(
                    a.decimal("condicionamento"),
                    a.decimal("folego"),
                    a.decimal("forca"),
                    a.decimal("velocidade"),
                    a.decimal("agilidade"),
                    a.decimal("tecnica"),
                    a.decimal("recuperacao"),
                    a.decimal("peso"));

            return new PerfilLutador(doc.texto("id"), doc.texto("nome"),
                    doc.texto("personagemBase"), atributos);
        } catch (IllegalArgumentException ex) {
            throw new PersistenciaException(PersistenciaException.Motivo.DADOS_INVALIDOS, arquivo,
                    ex.getMessage(), ex);
        }
    }

    private static void exigirPerfil(PerfilLutador perfil) {
        if (perfil == null) {
            throw new IllegalArgumentException("O perfil e obrigatorio.");
        }
    }
}
