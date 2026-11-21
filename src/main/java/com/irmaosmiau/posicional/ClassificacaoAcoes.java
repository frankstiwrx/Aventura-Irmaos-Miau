package com.irmaosmiau.posicional;

import com.irmaosmiau.controles.NaturezaAcao;

import java.util.Set;

/**
 * Entrega 5Q.6: classificacao CENTRAL das acoes do solo/quedas pela natureza (defesa,
 * transicao, finalizacao, queda, controle). Tecnicas novas recebem a identidade visual
 * automaticamente pelas regras abaixo (papel, efeito, origem e destino); so as excecoes
 * ficam listadas.
 */
public final class ClassificacaoAcoes {

    /** Tecnicas defensivas que trocam de posicao (escapes, recuperar a guarda, livrar a perna...). */
    private static final Set<String> ESCAPES = Set.of(CatalogoTecnicasSolo.RETENCAO_PES,
            CatalogoTecnicasSolo.AFASTAR_DO_CHAO, CatalogoTecnicasSolo.AFASTAR_DA_GUARDA,
            CatalogoTecnicasSolo.SAIR_DA_GUARDA, CatalogoTecnicasSolo.LIVRAR_GANCHO, CatalogoTecnicasSolo.LIVRAR_SLX,
            CatalogoTecnicasSolo.LIVRAR_MANGAS, CatalogoTecnicasSolo.SOLTAR_MANGA, CatalogoTecnicasSolo.TIRAR_GANCHOS,
            CatalogoTecnicasSolo.GIRAR_LIVRAR_PERNA, CatalogoTecnicasSolo.LEVANTAR_DO_ASHI,
            CatalogoTecnicasSolo.LEVANTAR_TECNICO, CatalogoTecnicasSolo.RECUAR);

    private ClassificacaoAcoes() {
    }

    public static NaturezaAcao de(Finalizacao f) {
        return NaturezaAcao.FINALIZACAO;
    }

    public static NaturezaAcao de(TecnicaSolo t) {
        if (t == null) {
            return NaturezaAcao.CONTROLE;
        }
        if (CatalogoTecnicasSolo.CHAVE_DEPOIS_DA_ENTRADA.containsKey(t.id())) {
            return NaturezaAcao.FINALIZACAO;               // complemento 5Q.8: entrada que termina numa chave
        }
        if (ESCAPES.contains(t.id()) || t.papel() == PapelPosicional.CONTROLADO) {
            return NaturezaAcao.DEFESA;
        }
        switch (t.efeito()) {
            case ENQUADRAR, LIVRAR_MANGA, RECUAR -> {
                return NaturezaAcao.DEFESA;
            }
            case POSTURA -> {
                // a postura/base de quem esta por cima e defensiva; "controlar as pernas" tambem usa
                // o efeito de postura, mas e controle de quem passa
                return t.id().equals(CatalogoTecnicasSolo.CONTROLAR_PERNAS) ? NaturezaAcao.CONTROLE : NaturezaAcao.DEFESA;
            }
            case NENHUM -> {
            }
            default -> {
                return NaturezaAcao.CONTROLE;
            }
        }
        if (t.sucesso() == null) {
            return NaturezaAcao.CONTROLE;
        }
        boolean comecaEmPe = t.origens().stream().noneMatch(Posicao::isNoChao);
        if (comecaEmPe && QuedasExplosivas.perfil(t.id()).isPresent()) {
            return NaturezaAcao.QUEDA;
        }
        if (comecaEmPe && t.sucesso().porCima() == Desfecho.RegraLado.INICIADOR) {
            return NaturezaAcao.QUEDA;                         // derrubar o outro (empurrao, pernas, rasteira...)
        }
        return NaturezaAcao.TRANSICAO;
    }
}
