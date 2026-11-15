package com.irmaosmiau.animacao;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.personagens.CatalogoPersonagens;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Perfis de animacao por personagem (Entrega 5E), pelo MESMO id do
 * CatalogoPersonagens ("miau_branco", "miau_preto"). Perfis personalizados
 * (4D) usam o perfil do personagem base: mudam atributos, nao animacoes.
 *
 * Padrao: Branco e Preto apontam para as animacoes LEGADAS (o que o jogo
 * desenha hoje). As versoes JSON ficam cadastradas no catalogo com paridade
 * comprovada e entram trocando UMA linha aqui quando o renderizador passar
 * a consultar o perfil (ver docs/ANIMACOES.md).
 */
public final class PerfisAnimacoes {

    public static final String RIG_MIAU = "miau_v1";

    private final CatalogoAnimacoes catalogo;
    private final Map<String, PerfilAnimacoes> porPersonagem = new LinkedHashMap<>();
    /** Entrega 5G: classe de lutador -> id de personagem (sem "se for Branco/Preto" no runtime). */
    private final Map<Class<?>, String> classes = new LinkedHashMap<>();

    private static PerfisAnimacoes global;

    public PerfisAnimacoes(CatalogoAnimacoes catalogo) {
        this.catalogo = catalogo;
    }

    public PerfisAnimacoes registrar(PerfilAnimacoes perfil) {
        if (porPersonagem.putIfAbsent(perfil.getIdPersonagem(), perfil) != null) {
            throw new IllegalArgumentException("Perfil repetido: " + perfil.getIdPersonagem());
        }
        return this;
    }

    public CatalogoAnimacoes getCatalogo() {
        return catalogo;
    }

    public Optional<PerfilAnimacoes> buscar(String idPersonagem) {
        return Optional.ofNullable(porPersonagem.get(idPersonagem));
    }

    /**
     * Liga uma classe de lutador a um id de personagem. Um futuro personagem
     * (rig compativel) registra a propria classe e o proprio perfil; o runtime
     * nao muda. Perfis 4D sao instancias de Branco/Preto: usam o do base.
     */
    public PerfisAnimacoes registrarClasse(Class<? extends IrmaoMiau> classe, String idPersonagem) {
        classes.put(classe, idPersonagem);
        return this;
    }

    /** Perfil do lutador em jogo: a classe registrada mais especifica (subindo a hierarquia). */
    public Optional<PerfilAnimacoes> doLutador(IrmaoMiau lutador) {
        for (Class<?> c = lutador.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            String id = classes.get(c);
            if (id != null) {
                return buscar(id);
            }
        }
        return Optional.empty();
    }

    /** Perfis da 5E: tudo legado (o catalogo exige os JSON). */
    public static PerfisAnimacoes padrao() {
        CatalogoAnimacoes c = CatalogoAnimacoes.padrao();
        PerfisAnimacoes p = new PerfisAnimacoes(c);
        p.registrar(perfilLegado(CatalogoPersonagens.ID_BRANCO, "branco", c));
        p.registrar(perfilLegado(CatalogoPersonagens.ID_PRETO, "preto", c));
        p.registrarClasse(IrmaoMiauBranco.class, CatalogoPersonagens.ID_BRANCO);
        p.registrarClasse(IrmaoMiauPreto.class, CatalogoPersonagens.ID_PRETO);
        return p;
    }

    /**
     * Entrega 5G: perfis do JOGO. Igual ao padrao, mas cada animacao JSON
     * migrada que carregou com sucesso SUBSTITUI a legada no perfil (hoje:
     * movimento no chao do Branco e do Preto). JSON ausente/invalido = o
     * perfil fica com a legada e o motivo vai uma vez para o aviso.
     */
    public static PerfisAnimacoes paraJogo(java.util.function.Function<String, String> leitor,
            java.util.function.Consumer<String> aviso) {
        CatalogoAnimacoes c = CatalogoAnimacoes.paraJogo(leitor, aviso);
        PerfisAnimacoes p = new PerfisAnimacoes(c);
        for (String[] pc : new String[][]{{CatalogoPersonagens.ID_BRANCO, "branco"}, {CatalogoPersonagens.ID_PRETO, "preto"}}) {
            PerfilAnimacoes perfil = perfilLegado(pc[0], pc[1], c);
            for (DefinicaoAnimacao d : c.todas()) {
                if (d.fonte() == FonteAnimacao.JSON_INDIVIDUAL && d.id().startsWith(pc[1] + ".")
                        && !d.id().contains(".fuga_quadril")) {   // 5L: so pelo modo PROPOSTA (F6)
                    perfil = perfil.comAnimacao(d.estado(), d.id(), c);
                }
            }
            p.registrar(perfil);
        }
        p.registrarClasse(IrmaoMiauBranco.class, CatalogoPersonagens.ID_BRANCO);
        p.registrarClasse(IrmaoMiauPreto.class, CatalogoPersonagens.ID_PRETO);
        return p;
    }

    /** Perfis usados pelas partidas (carregados uma vez; avisos no System.err sem repeticao). */
    public static synchronized PerfisAnimacoes global() {
        if (global == null) {
            java.util.Set<String> ditos = new java.util.HashSet<>();
            global = paraJogo(CatalogoAnimacoes::lerRecurso, m -> {
                if (ditos.add(m)) {
                    System.err.println(m);
                }
            });
        }
        return global;
    }

    /** null = recarrega na proxima partida (testes). */
    public static synchronized void instalarGlobal(PerfisAnimacoes perfis) {
        global = perfis;
    }

    private static PerfilAnimacoes perfilLegado(String idPersonagem, String prefixo, CatalogoAnimacoes c) {
        PerfilAnimacoes perfil = PerfilAnimacoes.novo(idPersonagem, RIG_MIAU);
        for (EstadoVisual e : EstadoVisual.values()) {
            String id = switch (e) {
                case PEGADA_ENTRADA -> "pegada_1.entrada";
                case PEGADA_ESTABELECIDA -> "pegada_1.estabelecida";
                case ARRASTO_CONJUNTO -> "arrasto_conjunto.legado";
                default -> prefixo + "." + e.name().toLowerCase() + ".legada";
            };
            perfil = perfil.comAnimacao(e, id, c);
        }
        return perfil;
    }
}
