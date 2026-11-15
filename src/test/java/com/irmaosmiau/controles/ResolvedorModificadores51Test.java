package com.irmaosmiau.controles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.ResolvedorModificadores.Combinacao;
import com.irmaosmiau.controles.ResolvedorModificadores.Modificador;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Entrega 5O.1: combinacoes de modificadores -> familia, extensivel e deterministico. */
class ResolvedorModificadores51Test {

    private static Set<Modificador> m(Modificador... ms) {
        Set<Modificador> s = EnumSet.noneOf(Modificador.class);
        for (Modificador x : ms) {
            s.add(x);
        }
        return s;
    }

    @Test
    void familiasDeHoje() {
        assertEquals(FamiliaTecnica.PEGADAS, ResolvedorModificadores.familia(m()));
        assertEquals(FamiliaTecnica.DEFESAS, ResolvedorModificadores.familia(m(Modificador.RT)));
        assertEquals(FamiliaTecnica.PROJECOES, ResolvedorModificadores.familia(m(Modificador.RB)));
        assertEquals(FamiliaTecnica.DEFESAS, ResolvedorModificadores.familia(m(Modificador.RT, Modificador.RB)),
                "RT tem prioridade sobre RB");
        assertEquals(FamiliaTecnica.DEFESAS, FamiliaTecnica.dosModificadores(true, true));
        assertEquals(FamiliaTecnica.PROJECOES, FamiliaTecnica.dosModificadores(false, true));
    }

    @Test
    void reservadasNuncaDisparamENaoMudamAsFamiliasAtivas() {
        assertEquals(FamiliaTecnica.PEGADAS, ResolvedorModificadores.familia(m(Modificador.LT)), "LT segue agachar");
        assertEquals(FamiliaTecnica.PEGADAS, ResolvedorModificadores.familia(m(Modificador.LB)));
        assertEquals(FamiliaTecnica.DEFESAS, ResolvedorModificadores.familia(m(Modificador.RT, Modificador.LB)));
        assertEquals(FamiliaTecnica.DEFESAS, ResolvedorModificadores.familia(m(Modificador.RT, Modificador.LT)));
        assertEquals(FamiliaTecnica.PEGADAS, ResolvedorModificadores.familia(m(Modificador.LT, Modificador.LB)));
        Combinacao lt = ResolvedorModificadores.exata(m(Modificador.LT)).orElseThrow();
        assertFalse(lt.ativa());
        assertNull(lt.familia(), "nenhuma tecnica inventada");
        assertEquals("LT + RS", lt.comando());
        for (Modificador[] c : new Modificador[][] {{Modificador.LB}, {Modificador.RT, Modificador.LB},
            {Modificador.RT, Modificador.LT}, {Modificador.LT, Modificador.LB}}) {
            assertTrue(ResolvedorModificadores.exata(m(c)).orElseThrow().rotulo().startsWith("RESERVADA"));
        }
    }

    @Test
    void tabelaDeterministicaEDoisBotoesComoPadrao() {
        Set<Integer> prioridades = new HashSet<>();
        Set<Set<Modificador>> combinacoes = new HashSet<>();
        for (Combinacao c : ResolvedorModificadores.tabela()) {
            assertTrue(prioridades.add(c.prioridade()), "prioridade unica: " + c.rotulo());
            assertTrue(combinacoes.add(c.modificadores()), "combinacao unica: " + c.rotulo());
            if (c.ativa()) {
                assertTrue(c.modificadores().size() <= 1, "o que ja funciona usa no maximo um modificador");
            }
            if (c.modificadores().size() == 3) {
                assertFalse(c.ativa(), "tres botoes: so reservado/especial");
            }
        }
        for (int bits = 0; bits < 16; bits++) {
            Set<Modificador> s = EnumSet.noneOf(Modificador.class);
            for (Modificador x : Modificador.values()) {
                if ((bits & (1 << x.ordinal())) != 0) {
                    s.add(x);
                }
            }
            Combinacao r = ResolvedorModificadores.resolver(s);
            assertTrue(r.ativa() && r.familia() != null, "toda combinacao resolve para UMA familia ativa: " + s);
        }
    }
}
