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

    /** Perfil do lutador em jogo (perfis 4D sao instancias de Branco/Preto). */
    public Optional<PerfilAnimacoes> doLutador(IrmaoMiau lutador) {
        if (lutador instanceof IrmaoMiauBranco) {
            return buscar(CatalogoPersonagens.ID_BRANCO);
        }
        if (lutador instanceof IrmaoMiauPreto) {
            return buscar(CatalogoPersonagens.ID_PRETO);
        }
        return Optional.empty();
    }

    public static PerfisAnimacoes padrao() {
        CatalogoAnimacoes c = CatalogoAnimacoes.padrao();
        PerfisAnimacoes p = new PerfisAnimacoes(c);
        p.registrar(perfilLegado(CatalogoPersonagens.ID_BRANCO, "branco", c));
        p.registrar(perfilLegado(CatalogoPersonagens.ID_PRETO, "preto", c));
        return p;
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
