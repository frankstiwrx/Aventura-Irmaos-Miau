package com.irmaosmiau.regras;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.posicional.CatalogoFinalizacoes;
import com.irmaosmiau.posicional.Finalizacao;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tecnicas por faixa: a tabela "Technical Fouls - Illegal Moves" do Livro de Regras da IBJJF (jun/2024,
 * adulto com kimono) comparada com a graduacao minima do nosso catalogo. So entram as tecnicas que
 * conseguimos identificar com seguranca no catalogo; as outras ficam documentadas como nao verificadas.
 * A graduacao do personagem ja bloqueia, entao nenhuma restricao nova foi preciso no modo com pontos.
 */
class TecnicasPorFaixaIbjjfTest {

    /** Base do catalogo -> faixa minima em que a IBJJF permite (adulto, kimono). */
    private static final Map<String, Graduacao> MINIMA_IBJJF = Map.of(
            "chave_punho", Graduacao.AZUL,          // wrist lock: proibida na branca
            "mao_de_vaca", Graduacao.AZUL,          // chave de punho
            "biceps", Graduacao.MARROM,             // bicep slicer: proibida ate a roxa
            "panturrilha", Graduacao.MARROM,        // calf slicer
            "chave_joelho", Graduacao.MARROM,       // knee bar
            "toe_hold", Graduacao.MARROM);          // toe hold

    /** Permitidas para todos os adultos (proibidas so para menores). */
    private static final List<String> LIVRES_ADULTO = List.of("chave_pe_reta", "ezequiel", "omoplata", "triangulo",
            "katagatame", "guilhotina");

    @Test
    void catalogoNuncaPermiteAntesDaFaixaDaIbjjf() {
        for (Finalizacao f : CatalogoFinalizacoes.referencia().todas()) {
            Graduacao min = MINIMA_IBJJF.get(f.tecnicaBase());
            if (min != null) {
                assertTrue(f.graduacao().ordinal() >= min.ordinal(),
                        f.id() + ": o jogo libera na " + f.graduacao() + ", a IBJJF so na " + min);
            }
        }
    }

    @Test
    void nadaProibidoParaTodosOsAdultosEstaNoCatalogo() {
        // heel hook, chaves que torcem o joelho, slam e chaves de coluna sem estrangulamento: proibidas
        for (Finalizacao f : CatalogoFinalizacoes.referencia().todas()) {
            String b = f.tecnicaBase();
            assertTrue(!b.contains("heel") && !b.contains("calcanhar") && !b.contains("slam") && !b.contains("coluna"),
                    "tecnica proibida para adultos no catalogo: " + f.id());
        }
    }

    @Test
    void permitidasAosAdultosContinuamDisponiveis() {
        for (String base : LIVRES_ADULTO) {
            assertTrue(CatalogoFinalizacoes.referencia().todas().stream().anyMatch(f -> f.tecnicaBase().equals(base)),
                    base + " continua no catalogo");
        }
    }
}
