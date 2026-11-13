package com.irmaosmiau.personagens;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalogo de personagens por id.
 *
 * {@link #padrao()} registra os dois Irmaos Miau usando os construtores
 * atuais, sem nenhuma alteracao em suas classes. Novos personagens sao
 * adicionados com {@link #registrar(DefinicaoPersonagem)}.
 *
 * Nesta entrega o catalogo ainda nao e usado pelo GamePanel nem pelos
 * menus (integracao prevista para entregas futuras).
 */
public final class CatalogoPersonagens {

    public static final String ID_BRANCO = "miau_branco";
    public static final String ID_PRETO = "miau_preto";

    private final Map<String, DefinicaoPersonagem> personagens = new LinkedHashMap<>();

    /** Catalogo vazio. */
    public CatalogoPersonagens() {
    }

    /** Catalogo com o Irmao Miau Branco e o Irmao Miau Preto. */
    public static CatalogoPersonagens padrao() {
        CatalogoPersonagens catalogo = new CatalogoPersonagens();

        // Valores identicos aos dos construtores de IrmaoMiauBranco/Preto.
        // O teste CatalogoPersonagensTest garante que continuem iguais.
        catalogo.registrar(new DefinicaoPersonagem(
                ID_BRANCO,
                "Irmão Miau Branco",
                "Jiu-Jitsu",
                "Mais pesado e forte; passador.",
                new AtributosLutador(35, 25, 42, 22, 25, 25, 22, 72),
                new ReferenciasVisuais(Color.WHITE, Color.BLACK, Color.BLACK,
                        "/assets/sprites/miau-branco.png"),
                IrmaoMiauBranco::new
        ));

        catalogo.registrar(new DefinicaoPersonagem(
                ID_PRETO,
                "Irmão Miau Preto",
                "Jiu-Jitsu",
                "Mais leve e movel; guardeiro.",
                new AtributosLutador(25, 40, 18, 45, 45, 28, 38, 68),
                new ReferenciasVisuais(Color.BLACK, Color.WHITE, Color.WHITE,
                        "/assets/sprites/miau-preto.png"),
                IrmaoMiauPreto::new
        ));

        return catalogo;
    }

    /**
     * @throws IllegalArgumentException se a definicao for nula ou o id ja existir
     */
    public void registrar(DefinicaoPersonagem definicao) {
        if (definicao == null) {
            throw new IllegalArgumentException("A definicao do personagem nao pode ser nula.");
        }
        if (personagens.containsKey(definicao.getId())) {
            throw new IllegalArgumentException(
                    "Ja existe um personagem registrado com o id: " + definicao.getId());
        }
        personagens.put(definicao.getId(), definicao);
    }

    public DefinicaoPersonagem buscar(String id) {
        return id == null ? null : personagens.get(DefinicaoPersonagem.normalizarId(id));
    }

    public DefinicaoPersonagem exigir(String id) {
        DefinicaoPersonagem d = buscar(id);
        if (d == null) {
            throw new IllegalArgumentException("Personagem nao registrado: " + id
                    + " (registrados: " + personagens.keySet() + ")");
        }
        return d;
    }

    public boolean contem(String id) {
        return buscar(id) != null;
    }

    /** Em ordem de cadastro; somente leitura. */
    public Collection<DefinicaoPersonagem> listar() {
        return Collections.unmodifiableCollection(personagens.values());
    }

    public int quantidade() {
        return personagens.size();
    }
}
