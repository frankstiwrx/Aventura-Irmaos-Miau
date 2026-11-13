package com.irmaosmiau.personagens;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;

import java.util.Locale;

/**
 * Ficha de cadastro de um personagem: identidade, estilo, atributos padrao,
 * referencias visuais e a fabrica que cria a instancia jogavel.
 *
 * Nao e uma subclasse por personagem: qualquer personagem e um objeto
 * deste tipo registrado no {@link CatalogoPersonagens}.
 */
public final class DefinicaoPersonagem {

    private final String id;
    private final String nome;
    private final String estilo;
    private final String descricao;
    private final AtributosLutador atributosPadrao;
    private final ReferenciasVisuais visual;
    private final FabricaLutador fabrica;

    public DefinicaoPersonagem(
            String id,
            String nome,
            String estilo,
            String descricao,
            AtributosLutador atributosPadrao,
            ReferenciasVisuais visual,
            FabricaLutador fabrica
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O personagem precisa de um id.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O personagem " + id + " precisa de um nome.");
        }
        if (estilo == null || estilo.isBlank()) {
            throw new IllegalArgumentException("O personagem " + id + " precisa de um estilo de luta.");
        }
        if (atributosPadrao == null) {
            throw new IllegalArgumentException("O personagem " + id + " precisa de atributos padrao.");
        }
        if (visual == null) {
            throw new IllegalArgumentException("O personagem " + id + " precisa de referencias visuais.");
        }
        if (fabrica == null) {
            throw new IllegalArgumentException("O personagem " + id + " precisa de uma fabrica.");
        }
        this.id = normalizarId(id);
        this.nome = nome.trim();
        this.estilo = estilo.trim();
        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
        this.atributosPadrao = atributosPadrao;
        this.visual = visual;
        this.fabrica = fabrica;
    }

    static String normalizarId(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEstilo() {
        return estilo;
    }

    /** Pode ser null. */
    public String getDescricao() {
        return descricao;
    }

    public AtributosLutador getAtributosPadrao() {
        return atributosPadrao;
    }

    public ReferenciasVisuais getVisual() {
        return visual;
    }

    public FabricaLutador getFabrica() {
        return fabrica;
    }

    /** Cria a instancia jogavel na posicao e escala indicadas. */
    public IrmaoMiau criarLutador(int x, int y, double escala) {
        IrmaoMiau lutador = fabrica.criar(x, y, escala);
        if (lutador == null) {
            throw new IllegalStateException("A fabrica do personagem " + id + " retornou null.");
        }
        return lutador;
    }
}
