package com.irmaosmiau.personagens;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Lutador escolhido para uma partida: um personagem OFICIAL ou um PERFIL
 * personalizado (que usa o desenho e as animacoes do personagem-base, com
 * os seus proprios atributos).
 *
 * Guarda uma fotografia do perfil no momento da escolha: excluir ou editar
 * o arquivo depois nao afeta a partida em andamento, e jogar nunca grava
 * nada no perfil.
 */
public final class EscolhaLutador {

    private final DefinicaoPersonagem base;
    private final PerfilLutador perfil;     // null = oficial

    private EscolhaLutador(DefinicaoPersonagem base, PerfilLutador perfil) {
        if (base == null) {
            throw new IllegalArgumentException("O personagem-base e obrigatorio.");
        }
        this.base = base;
        this.perfil = perfil;
    }

    public static EscolhaLutador oficial(DefinicaoPersonagem definicao) {
        return new EscolhaLutador(definicao, null);
    }

    /**
     * @throws IllegalArgumentException se o perfil nao for desta base ou a
     *         base nao aceitar perfis
     */
    public static EscolhaLutador perfil(PerfilLutador perfil, DefinicaoPersonagem base) {
        if (perfil == null) {
            throw new IllegalArgumentException("O perfil e obrigatorio.");
        }
        if (base == null || !base.getId().equals(perfil.getIdPersonagemBase())) {
            throw new IllegalArgumentException("O perfil " + perfil.getId() + " e baseado em "
                    + perfil.getIdPersonagemBase() + ", nao em " + (base == null ? null : base.getId()));
        }
        if (!base.aceitaPerfis()) {
            throw new IllegalArgumentException("O personagem " + base.getId() + " nao aceita perfis.");
        }
        return new EscolhaLutador(base, perfil);
    }

    /** Cria a instancia para a partida (oficial ou com os atributos do perfil). */
    public IrmaoMiau criarLutador(int x, int y, double escala) {
        if (perfil == null) {
            return base.criarLutador(x, y, escala);
        }
        return base.criarLutadorPersonalizado(x, y, escala, perfil.getAtributos(), perfil.getNome());
    }

    public boolean isOficial() {
        return perfil == null;
    }

    /** Id unico entre as opcoes: o id do oficial ou "perfil:&lt;id&gt;". */
    public String getId() {
        return perfil == null ? base.getId() : "perfil:" + perfil.getId();
    }

    public String getNome() {
        return perfil == null ? base.getNome() : perfil.getNome();
    }

    public String getIdBase() {
        return base.getId();
    }

    public DefinicaoPersonagem getBase() {
        return base;
    }

    /** Null quando oficial. */
    public PerfilLutador getPerfil() {
        return perfil;
    }

    /** Copia dos atributos que a instancia vai usar. */
    public AtributosLutador getAtributos() {
        return perfil == null ? base.getAtributosPadrao().copiar() : perfil.getAtributos();
    }

    /** Texto para listas: "Branco Forte — personalizado (base Irmão Miau Branco)". */
    public String getDescricao() {
        return perfil == null
                ? base.getNome() + " — oficial"
                : perfil.getNome() + " — personalizado (base " + base.getNome() + ")";
    }

    /** Texto curto para listas estreitas: "Branco Forte · personalizado". */
    @Override
    public String toString() {
        return getNome() + (perfil == null ? " · oficial" : " · personalizado");
    }
}
