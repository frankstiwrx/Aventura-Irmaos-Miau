package com.irmaosmiau.animacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.DefesaCorporal;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Entrega 5O: estado da guarda, biblioteca compartilhada (rig miau_v1) e fallback de rig. */
class Movimentos5OTest {

    /** Personagem futuro simulado com outro esqueleto. */
    static final class OutroRig extends IrmaoMiauPreto {
        OutroRig() {
            super(100, 150, 0.7);
        }

        @Override
        public String getRigAnimacao() {
            return "rig_de_teste";
        }
    }

    /** Personagem futuro simulado com o mesmo esqueleto (ex.: Miau Judo). */
    static final class MesmoRig extends IrmaoMiauBranco {
        MesmoRig() {
            super(100, 150, 0.7);
        }
    }

    private final BibliotecaMovimentos bib = BibliotecaMovimentos.padrao();

    @AfterEach
    void limpar() {
        VersaoMovimentos.restaurarPadrao();
    }

    @Test
    void cicloDaGuarda() {
        DefesaCorporal d = new DefesaCorporal();
        assertTrue(d.manter(DefesaCorporal.Tipo.GUARDA_ALTA));
        assertFalse(d.protege(DefesaCorporal.Nivel.ALTO), "entrando ainda nao protege");
        for (int i = 0; i < DefesaCorporal.TICKS_ENTRADA; i++) {
            assertFalse(d.manter(DefesaCorporal.Tipo.GUARDA_ALTA));
            d.atualizar();
        }
        assertEquals(DefesaCorporal.Fase.ATIVA, d.getFase());
        assertTrue(d.protege(DefesaCorporal.Nivel.ALTO));
        assertFalse(d.protege(DefesaCorporal.Nivel.BAIXO));
        assertTrue(d.manter(DefesaCorporal.Tipo.GUARDA_BAIXA), "troca recomeca a entrada");
        assertEquals(DefesaCorporal.Tipo.GUARDA_ALTA, d.getAnterior());
        d.soltar();
        assertEquals(DefesaCorporal.Fase.SAINDO, d.getFase());
        for (int i = 0; i < DefesaCorporal.TICKS_SAIDA; i++) {
            d.atualizar();
        }
        assertFalse(d.isAtiva());
        d.manter(DefesaCorporal.Tipo.GUARDA_MEDIA);
        d.interromper();
        assertFalse(d.isAtiva(), "interromper zera na hora");
    }

    @Test
    void bibliotecaCompartilhadaPorTecnica() {
        for (String id : BibliotecaMovimentos.GUARDAS) {
            BibliotecaMovimentos.Movimento m = bib.buscar(id).orElseThrow();
            assertEquals("miau_v1", m.getRig());
            assertEquals(2, m.quantidade(), id + ": sustentada A e B (respiracao)");
        }
        for (String id : BibliotecaMovimentos.SEQUENCIAS) {
            BibliotecaMovimentos.Movimento m = bib.buscar(id).orElseThrow();
            assertTrue(m.quantidade() >= 6, id + ": preparacao, execucao e recuperacao");
            int[] p = new int[30];
            int[] antes = new int[30];
            m.noProgresso(0, antes);
            for (int i = 1; i <= 100; i++) {
                m.noProgresso(i / 100.0, p);
                for (int k = 4; k < 30; k++) {
                    if (k != 10) {
                        assertTrue(Math.abs(p[k] - antes[k]) <= 20, id + ": salto na articulacao " + k + " em " + i);
                    }
                }
                System.arraycopy(p, 0, antes, 0, 30);
            }
        }
        // os rolamentos giram o corpo inteiro (360 graus) com o corpo grupado, nao so deslocam
        int[] meio = new int[30];
        bib.buscar("rolamento_frente_5o").orElseThrow().noProgresso(0.5, meio);
        assertTrue(meio[10] > 120 && meio[22] > 15, "frente: girando e com os joelhos recolhidos");
        bib.buscar("rolamento_tras_5o").orElseThrow().noProgresso(0.5, meio);
        assertTrue(meio[10] < -120, "tras: gira para o outro lado");
    }

    @Test
    void brancoPretoEPersonagemFuturoUsamOMesmoArquivo() {
        AnimadorMovimentos a = new AnimadorMovimentos(bib, new TransicoesPostura(l -> null));
        for (IrmaoMiau l : new IrmaoMiau[] {new IrmaoMiauBranco(100, 150, 0.7), new IrmaoMiauPreto(100, 150, 0.7),
            new MesmoRig()}) {
            l.getDefesa().manter(DefesaCorporal.Tipo.GUARDA_MEDIA);
            assertTrue(a.quer(l));
            assertEquals("guarda:GUARDA_MEDIA", a.aplicar(l));
            assertTrue(l.isPoseCombateExternaAtiva());
            l.getDefesa().interromper();
            a.liberar(l);
            assertFalse(l.isPoseCombateExternaAtiva());
        }
    }

    @Test
    void rigIncompativelCaiNoFallback() {
        AnimadorMovimentos a = new AnimadorMovimentos(bib, new TransicoesPostura(l -> null));
        IrmaoMiau outro = new OutroRig();
        outro.getDefesa().manter(DefesaCorporal.Tipo.GUARDA_ALTA);
        assertNull(a.aplicar(outro), "sem pose estranha: fica o desenho do proprio personagem");
        assertFalse(outro.isPoseCombateExternaAtiva());
    }

    @Test
    void versaoDosMovimentos() {
        assertEquals(VersaoMovimentos.ORIGINAL, VersaoMovimentos.atual(), "motor: o de sempre");
        assertEquals(VersaoMovimentos.EXPERIMENTAL_5O, VersaoMovimentos.padraoDoJogo());
        assertEquals(VersaoMovimentos.EXPERIMENTAL_5O, VersaoMovimentos.alternar());
        assertEquals(VersaoMovimentos.ORIGINAL, VersaoMovimentos.alternar());
        assertNotNull(CarregadorAnimacaoJson.movimentoDoRecurso(BibliotecaMovimentos.caminho("sprawl_5o"))
                .deslocamentosY());
    }
}
