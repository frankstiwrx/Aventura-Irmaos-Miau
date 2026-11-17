package com.irmaosmiau.regras;

import com.irmaosmiau.posicional.CategoriaFinalizacao;
import com.irmaosmiau.posicional.Finalizacao;

import java.util.Set;

/**
 * Entrega 5Q: tabela de ELEGIBILIDADE por modo de jogo (separada da graduacao).
 *
 * - LIVRE: regras proprias do jogo; tudo o que a graduacao libera vale.
 * - CAMPEONATO_PROVISORIO: preparado para um regulamento esportivo com quimono.
 *   E uma tabela PROVISORIA e conservadora (nao e o regulamento oficial): so a
 *   chave de pe reta fica liberada entre os ataques as pernas. Antes de ativar
 *   este modo no jogo, a tabela deve ser conferida com o regulamento oficial
 *   da competicao escolhida (idade, faixa, categoria e versao das regras).
 *
 * Heel hook nao existe no catalogo e nunca deve ser liberado com quimono sem um
 * modo/regulamento proprio.
 */
public enum ModoRegras {
    LIVRE("Livre"),
    CAMPEONATO_PROVISORIO("Campeonato (provisório)");

    private static final Set<String> PERNAS_LIBERADAS_CAMPEONATO = Set.of("chave_pe_reta");

    private final String nome;

    ModoRegras(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    /** A finalizacao e permitida neste modo? */
    public boolean permite(Finalizacao f) {
        if (this == LIVRE) {
            return true;
        }
        return f.categoria() != CategoriaFinalizacao.PERNA || PERNAS_LIBERADAS_CAMPEONATO.contains(f.tecnicaBase());
    }
}
