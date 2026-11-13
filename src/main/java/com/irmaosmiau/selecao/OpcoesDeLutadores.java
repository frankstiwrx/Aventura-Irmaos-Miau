package com.irmaosmiau.selecao;

import com.irmaosmiau.persistencia.PersistenciaException;
import com.irmaosmiau.persistencia.ResultadoCarga;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.DefinicaoPersonagem;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.personagens.PerfilLutador;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Opcoes de lutador para um slot (Entrega 4D): o personagem oficial
 * primeiro, depois os perfis salvos baseados nele (ordem alfabetica).
 *
 * Perfis com problema NUNCA impedem os oficiais: viram avisos. Ler as
 * opcoes nao grava nada.
 */
public final class OpcoesDeLutadores {

    private final CatalogoPersonagens catalogo;
    private final ServicoPerfis perfis;     // null = somente oficiais

    public OpcoesDeLutadores(CatalogoPersonagens catalogo, ServicoPerfis perfis) {
        if (catalogo == null) {
            throw new IllegalArgumentException("O catalogo e obrigatorio.");
        }
        this.catalogo = catalogo;
        this.perfis = perfis;
    }

    /** Catalogo oficial + perfis da pasta do jogador. */
    public static OpcoesDeLutadores padrao() {
        return new OpcoesDeLutadores(CatalogoPersonagens.padrao(), ServicoPerfis.padrao());
    }

    public CatalogoPersonagens getCatalogo() {
        return catalogo;
    }

    /**
     * Opcoes para o slot de um personagem-base (ex.: "miau_branco").
     *
     * @throws IllegalArgumentException se o personagem-base nao existir
     */
    public ResultadoCarga<List<EscolhaLutador>> paraBase(String idBase) {
        DefinicaoPersonagem base = catalogo.exigir(idBase);
        List<EscolhaLutador> opcoes = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        opcoes.add(EscolhaLutador.oficial(base));

        if (perfis == null || !base.aceitaPerfis()) {
            return new ResultadoCarga<>(opcoes, avisos);
        }

        try {
            ResultadoCarga<List<PerfilLutador>> lidos = perfis.listar();
            avisos.addAll(lidos.getAvisos());
            List<PerfilLutador> desta = new ArrayList<>();
            for (PerfilLutador p : lidos.getValor()) {
                if (p.getIdPersonagemBase().equals(base.getId())) {
                    desta.add(p);
                }
            }
            Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
            desta.sort(Comparator.comparing(PerfilLutador::getNome, ordem).thenComparing(PerfilLutador::getId));
            for (PerfilLutador p : desta) {
                opcoes.add(EscolhaLutador.perfil(p, base));
            }
        } catch (PersistenciaException ex) {
            avisos.add("Não foi possível ler os perfis personalizados; apenas o oficial está disponível. "
                    + ex.getMessage());
        }
        return new ResultadoCarga<>(opcoes, avisos);
    }

    /** Escolha oficial de um personagem (atalho). */
    public EscolhaLutador oficial(String idBase) {
        return EscolhaLutador.oficial(catalogo.exigir(idBase));
    }
}
