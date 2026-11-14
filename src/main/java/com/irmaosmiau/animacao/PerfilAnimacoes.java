package com.irmaosmiau.animacao;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Quais animacoes um personagem usa (Entrega 5E). Composicao, nao heranca
 * de classes: um perfil pode partir de um BASE compativel (mesmo rig) e
 * substituir so o que quiser. Ex.: um futuro gato boxeador usa a corrida do
 * Branco e uma caminhada propria em JSON.
 *
 * Resolucao: animacao propria -> base (recursivo) -> nada (o renderizador
 * do personagem segue com o codigo legado).
 */
public final class PerfilAnimacoes {

    private final String idPersonagem;
    private final String rig;
    private final PerfilAnimacoes base;
    private final Map<EstadoVisual, String> proprias;

    private PerfilAnimacoes(String idPersonagem, String rig, PerfilAnimacoes base, Map<EstadoVisual, String> proprias) {
        if (idPersonagem == null || idPersonagem.isBlank() || rig == null) {
            throw new IllegalArgumentException("Perfil de animacoes sem personagem ou rig.");
        }
        if (base != null && !base.rig.equals(rig)) {
            throw new IllegalArgumentException(idPersonagem + ": o perfil base " + base.idPersonagem
                    + " usa o rig " + base.rig + ", incompativel com " + rig);
        }
        this.idPersonagem = idPersonagem;
        this.rig = rig;
        this.base = base;
        this.proprias = Collections.unmodifiableMap(new EnumMap<>(proprias));
    }

    public static PerfilAnimacoes novo(String idPersonagem, String rig) {
        return new PerfilAnimacoes(idPersonagem, rig, null, new EnumMap<>(EstadoVisual.class));
    }

    /** Perfil que herda tudo do base (mesmo rig) ate substituir algo. */
    public static PerfilAnimacoes baseadoEm(String idPersonagem, PerfilAnimacoes base) {
        return new PerfilAnimacoes(idPersonagem, base.rig, base, new EnumMap<>(EstadoVisual.class));
    }

    /** Copia com a animacao do estado trocada (valida no catalogo: existe, estado e rig batem). */
    public PerfilAnimacoes comAnimacao(EstadoVisual estado, String idAnimacao, CatalogoAnimacoes catalogo) {
        DefinicaoAnimacao d = catalogo.exigir(idAnimacao);
        if (d.estado() != estado) {
            throw new IllegalArgumentException(idAnimacao + " e de " + d.estado() + ", nao de " + estado);
        }
        if (!d.rig().equals(rig)) {
            throw new IllegalArgumentException(idAnimacao + " usa o rig " + d.rig() + "; "
                    + idPersonagem + " usa " + rig);
        }
        Map<EstadoVisual, String> m = new EnumMap<>(EstadoVisual.class);
        m.putAll(proprias);
        m.put(estado, idAnimacao);
        return new PerfilAnimacoes(idPersonagem, rig, base, m);
    }

    public String getIdPersonagem() {
        return idPersonagem;
    }

    public String getRig() {
        return rig;
    }

    public Optional<PerfilAnimacoes> getBase() {
        return Optional.ofNullable(base);
    }

    /** Animacao do estado, com de onde ela veio. */
    public Optional<AnimacaoResolvida> resolver(EstadoVisual estado, CatalogoAnimacoes catalogo) {
        String id = proprias.get(estado);
        if (id != null) {
            return Optional.of(new AnimacaoResolvida(catalogo.exigir(id), idPersonagem, false));
        }
        if (base != null) {
            return base.resolver(estado, catalogo)
                    .map(r -> new AnimacaoResolvida(r.definicao(), r.perfilDeOrigem(), true));
        }
        return Optional.empty();
    }
}
