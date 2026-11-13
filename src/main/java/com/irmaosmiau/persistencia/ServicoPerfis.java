package com.irmaosmiau.persistencia;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.DefinicaoPersonagem;
import com.irmaosmiau.personagens.PerfilLutador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Regras de perfis que dependem do catalogo de personagens:
 * - o personagem-base precisa existir;
 * - um perfil nao pode usar o id de um personagem oficial;
 * - os atributos padrao dos oficiais NUNCA sao alterados (o perfil
 *   sempre trabalha com uma copia).
 *
 * Ponto de entrada recomendado para a futura tela de edicao.
 */
public final class ServicoPerfis {

    private final RepositorioPerfis repositorio;
    private final CatalogoPersonagens catalogo;

    public ServicoPerfis(RepositorioPerfis repositorio, CatalogoPersonagens catalogo) {
        if (repositorio == null || catalogo == null) {
            throw new IllegalArgumentException("Repositorio e catalogo sao obrigatorios.");
        }
        this.repositorio = repositorio;
        this.catalogo = catalogo;
    }

    /** Servico com a pasta de dados do usuario e o catalogo padrao. */
    public static ServicoPerfis padrao() {
        return new ServicoPerfis(new RepositorioPerfis(DiretorioDados.doUsuario()), CatalogoPersonagens.padrao());
    }

    // =====================================================
    // CRIAR
    // =====================================================
    /** Novo perfil com os atributos padrao do personagem-base (copiados). */
    public PerfilLutador criarAPartirDoPersonagem(String idPerfil, String nome, String idPersonagemBase) {
        DefinicaoPersonagem base = exigirBase(idPersonagemBase);
        return criar(idPerfil, nome, base.getId(), base.getAtributosPadrao());
    }

    /** Novo perfil com atributos escolhidos (validados pelos LimitesAtributos). */
    public PerfilLutador criar(String idPerfil, String nome, String idPersonagemBase, AtributosLutador atributos) {
        DefinicaoPersonagem base = exigirBase(idPersonagemBase);
        String id = PerfilLutador.normalizarId(idPerfil);
        if (catalogo.contem(id)) {
            throw new PersistenciaException(PersistenciaException.Motivo.CONFLITO, null,
                    "\"" + id + "\" e o id de um personagem oficial; escolha outro id para o perfil");
        }
        PerfilLutador perfil = new PerfilLutador(id, nome, base.getId(), atributos);
        repositorio.criar(perfil);
        return perfil;
    }

    // =====================================================
    // LER / ATUALIZAR / EXCLUIR
    // =====================================================
    public Optional<PerfilLutador> carregar(String idPerfil) {
        return repositorio.carregar(idPerfil);
    }

    /** Lista perfis; avisa (sem apagar) arquivos com problema ou base inexistente. */
    public ResultadoCarga<List<PerfilLutador>> listar() {
        ResultadoCarga<List<PerfilLutador>> r = repositorio.listar();
        List<String> avisos = new ArrayList<>(r.getAvisos());
        for (PerfilLutador p : r.getValor()) {
            if (!catalogo.contem(p.getIdPersonagemBase())) {
                avisos.add(p.getId() + ": personagem-base \"" + p.getIdPersonagemBase()
                        + "\" nao existe mais no catalogo");
            }
        }
        return new ResultadoCarga<>(r.getValor(), avisos);
    }

    public PerfilLutador atualizarAtributos(String idPerfil, AtributosLutador novos) {
        PerfilLutador atualizado = exigir(idPerfil).comAtributos(novos);
        repositorio.atualizar(atualizado);
        return atualizado;
    }

    public PerfilLutador renomear(String idPerfil, String novoNome) {
        PerfilLutador atualizado = exigir(idPerfil).comNome(novoNome);
        repositorio.atualizar(atualizado);
        return atualizado;
    }

    public boolean excluir(String idPerfil) {
        return repositorio.excluir(idPerfil);
    }

    // =====================================================
    // CONSULTAS PARA A INTEGRACAO FUTURA
    // =====================================================
    /** Definicao oficial usada como base visual do perfil. */
    public DefinicaoPersonagem baseDe(PerfilLutador perfil) {
        return exigirBase(perfil.getIdPersonagemBase());
    }

    /** Atributos padrao oficiais do personagem-base (para comparar/restaurar). */
    public AtributosLutador atributosOficiais(PerfilLutador perfil) {
        return baseDe(perfil).getAtributosPadrao();
    }

    private PerfilLutador exigir(String idPerfil) {
        return repositorio.carregar(idPerfil).orElseThrow(() -> new PersistenciaException(
                PersistenciaException.Motivo.CONFLITO, repositorio.arquivoDe(idPerfil),
                "perfil \"" + idPerfil + "\" nao existe"));
    }

    private DefinicaoPersonagem exigirBase(String idPersonagemBase) {
        if (idPersonagemBase == null || !catalogo.contem(idPersonagemBase)) {
            throw new IllegalArgumentException("Personagem-base inexistente: " + idPersonagemBase
                    + " (oficiais: " + catalogo.listar().stream().map(DefinicaoPersonagem::getId).toList() + ")");
        }
        return catalogo.exigir(idPersonagemBase);
    }
}
