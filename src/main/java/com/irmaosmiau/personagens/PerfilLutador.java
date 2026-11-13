package com.irmaosmiau.personagens;

import com.irmaosmiau.entities.AtributosLutador;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Perfil personalizado criado pelo jogador a partir de um personagem
 * oficial: mesmo visual/renderer do personagem-base, atributos proprios.
 *
 * Imutavel. Guarda uma COPIA dos atributos: nada que se faca com um perfil
 * altera a definicao oficial do personagem-base.
 */
public final class PerfilLutador {

    /** Id seguro para nome de arquivo: minusculas, numeros, _ e -, 3 a 40. */
    public static final Pattern ID_VALIDO = Pattern.compile("[a-z0-9][a-z0-9_-]{2,39}");
    public static final int TAMANHO_MAXIMO_NOME = 40;

    private final String id;
    private final String nome;
    private final String idPersonagemBase;
    private final AtributosLutador atributos;

    /**
     * @throws IllegalArgumentException id, nome, base ou atributos invalidos
     */
    public PerfilLutador(String id, String nome, String idPersonagemBase, AtributosLutador atributos) {
        this.id = normalizarId(id);
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O perfil precisa de um nome.");
        }
        String n = nome.trim();
        if (n.length() > TAMANHO_MAXIMO_NOME) {
            throw new IllegalArgumentException("Nome do perfil com mais de " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
        if (n.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Nome do perfil com caracteres de controle.");
        }
        if (idPersonagemBase == null || idPersonagemBase.isBlank()) {
            throw new IllegalArgumentException("O perfil precisa do id do personagem-base.");
        }
        LimitesAtributos.validar(atributos);

        this.nome = n;
        this.idPersonagemBase = idPersonagemBase.trim().toLowerCase(Locale.ROOT);
        this.atributos = copiar(atributos);
    }

    /** Normaliza e valida um id de perfil. */
    public static String normalizarId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("O perfil precisa de um id.");
        }
        String n = id.trim().toLowerCase(Locale.ROOT);
        if (!ID_VALIDO.matcher(n).matches()) {
            throw new IllegalArgumentException("Id de perfil invalido: \"" + id
                    + "\" (3 a 40 caracteres: letras minusculas, numeros, _ ou -; comecando por letra ou numero)");
        }
        return n;
    }

    static AtributosLutador copiar(AtributosLutador a) {
        return new AtributosLutador(a.getCondicionamento(), a.getFolego(), a.getForca(),
                a.getVelocidade(), a.getAgilidade(), a.getTecnica(), a.getRecuperacao(), a.getPeso());
    }

    public PerfilLutador comNome(String novoNome) {
        return new PerfilLutador(id, novoNome, idPersonagemBase, atributos);
    }

    public PerfilLutador comAtributos(AtributosLutador novos) {
        return new PerfilLutador(id, nome, idPersonagemBase, novos);
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getIdPersonagemBase() {
        return idPersonagemBase;
    }

    /** Copia dos atributos (o perfil continua imutavel). */
    public AtributosLutador getAtributos() {
        return copiar(atributos);
    }

    /** true se os valores forem iguais aos de outro conjunto de atributos. */
    public static boolean mesmosValores(AtributosLutador a, AtributosLutador b) {
        for (LimitesAtributos.Limite l : LimitesAtributos.todos()) {
            if (Double.compare(l.ler(a), l.ler(b)) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PerfilLutador)) {
            return false;
        }
        PerfilLutador p = (PerfilLutador) o;
        return id.equals(p.id) && nome.equals(p.nome) && idPersonagemBase.equals(p.idPersonagemBase)
                && mesmosValores(atributos, p.atributos);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "PerfilLutador[" + id + ", \"" + nome + "\", base=" + idPersonagemBase + "]";
    }
}
