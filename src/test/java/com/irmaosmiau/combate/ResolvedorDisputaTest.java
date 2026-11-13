package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/** Entrega 5A: formula deterministica das disputas tecnicas. */
class ResolvedorDisputaTest {

    private static final AtributosLutador BRANCO = IrmaoMiauBranco.atributosOficiais();
    private static final AtributosLutador PRETO = IrmaoMiauPreto.atributosOficiais();
    private static final AtributosLutador MEDIO = new AtributosLutador(50, 50, 50, 50, 50, 50, 50, 80);

    private static AtributosLutador com(AtributosLutador a, String campo, double v) {
        return new AtributosLutador(
                campo.equals("condicionamento") ? v : a.getCondicionamento(),
                campo.equals("folego") ? v : a.getFolego(),
                campo.equals("forca") ? v : a.getForca(),
                campo.equals("velocidade") ? v : a.getVelocidade(),
                campo.equals("agilidade") ? v : a.getAgilidade(),
                campo.equals("tecnica") ? v : a.getTecnica(),
                campo.equals("recuperacao") ? v : a.getRecuperacao(),
                campo.equals("peso") ? v : a.getPeso());
    }

    private static double ataque(TipoDisputa t, AtributosLutador a) {
        return ResolvedorDisputa.resolver(t, a, a.getFolego(), MEDIO, MEDIO.getFolego()).getPontuacaoAtaque();
    }

    private static double defesa(TipoDisputa t, AtributosLutador d) {
        return ResolvedorDisputa.resolver(t, MEDIO, MEDIO.getFolego(), d, d.getFolego()).getPontuacaoDefesa();
    }

    // =====================================================
    // CONTRATO DOS PESOS
    // =====================================================
    @Test
    void pesosSaoNaoNegativosSomamUmEPesoCorporalEModerado() {
        for (TipoDisputa t : TipoDisputa.values()) {
            for (TipoDisputa.Pesos p : new TipoDisputa.Pesos[]{t.getPesosAtaque(), t.getPesosDefesa()}) {
                assertEquals(1.0, p.soma(), 1e-12, t.name());
                assertTrue(p.getTecnica() >= 0 && p.getForca() >= 0 && p.getAgilidade() >= 0 && p.getPesoCorporal() >= 0);
                assertTrue(p.getPesoCorporal() <= 0.20, "peso corporal nunca domina");
                assertTrue(p.getPesoCorporal() < Math.max(p.getTecnica(), p.getForca()));
            }
        }
    }

    // =====================================================
    // DETERMINISMO E VALORES DE REFERENCIA
    // =====================================================
    @Test
    void mesmaEntradaSempreMesmoResultado() {
        ResultadoDisputa r1 = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, BRANCO, 22, PRETO, 40);
        for (int i = 0; i < 100; i++) {
            ResultadoDisputa r = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, BRANCO, 22, PRETO, 40);
            assertEquals(r1.getPontuacaoAtaque(), r.getPontuacaoAtaque());
            assertEquals(r1.getPontuacaoDefesa(), r.getPontuacaoDefesa());
            assertEquals(r1.isSucessoDoAtaque(), r.isSucessoDoAtaque());
        }
    }

    @Test
    void casosDeReferenciaComOsOficiais() {
        // Branco (22/25 de folego depois da PEGADA_1) tentando desequilibrar o Preto descansado.
        ResultadoDisputa d = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, BRANCO, 22, PRETO, 40);
        // ataque = (.40*.25 + .35*.42 + .10*.25 + .15*.40) * (.6 + .4*22/25) * 1.10
        assertEquals((0.10 + 0.147 + 0.025 + 0.06) * (0.6 + 0.4 * 22 / 25.0) * 1.10, d.getPontuacaoAtaque(), 1e-12);
        // defesa = (.35*.28 + .20*.18 + .25*.45 + .20*.35) * 1.0
        assertEquals(0.098 + 0.036 + 0.1125 + 0.07, d.getPontuacaoDefesa(), 1e-12);
        assertTrue(d.isSucessoDoAtaque(), d.toString());

        // Preto tentando o rompimento tecnico contra o Branco segurando a pegada.
        ResultadoDisputa r = ResolvedorDisputa.resolver(TipoDisputa.ROMPIMENTO_TECNICO, PRETO, 40, BRANCO, 22);
        assertFalse(r.isSucessoDoAtaque(), "forca/tecnica do Branco seguram a pegada: " + r);
        // Com o Branco exausto, o Preto rompe.
        assertTrue(ResolvedorDisputa.resolver(TipoDisputa.ROMPIMENTO_TECNICO, PRETO, 40, BRANCO, 0)
                .isSucessoDoAtaque());
    }

    // =====================================================
    // MONOTONIA: ATRIBUTOS NUNCA PREJUDICAM QUEM OS USA
    // =====================================================
    @Test
    void maisForcaOuTecnicaNuncaPioraOAtaque() {
        for (TipoDisputa t : TipoDisputa.values()) {
            for (String campo : new String[]{"tecnica", "forca", "agilidade", "peso"}) {
                double anterior = -1;
                for (int v = 1; v <= 120; v += 7) {
                    double s = ataque(t, com(MEDIO, campo, campo.equals("peso") ? Math.max(40, v) : Math.min(100, v)));
                    assertTrue(s >= anterior - 1e-12, t + " " + campo + "=" + v);
                    anterior = s;
                }
            }
            assertTrue(ataque(t, com(MEDIO, "tecnica", 90)) > ataque(t, com(MEDIO, "tecnica", 20)));
            assertTrue(ataque(t, com(MEDIO, "forca", 90)) > ataque(t, com(MEDIO, "forca", 20)));
        }
    }

    @Test
    void maisTecnicaOuAgilidadeNuncaPioraADefesa() {
        for (TipoDisputa t : TipoDisputa.values()) {
            for (String campo : new String[]{"tecnica", "agilidade", "forca", "peso"}) {
                double anterior = -1;
                for (int v = 1; v <= 100; v += 9) {
                    double s = defesa(t, com(MEDIO, campo, campo.equals("peso") ? 40 + v * 0.8 : v));
                    assertTrue(s >= anterior - 1e-12, t + " " + campo + "=" + v);
                    anterior = s;
                }
            }
            assertTrue(defesa(t, com(MEDIO, "tecnica", 90)) > defesa(t, com(MEDIO, "tecnica", 20)));
        }
        assertTrue(defesa(TipoDisputa.DESEQUILIBRIO, com(MEDIO, "agilidade", 90))
                > defesa(TipoDisputa.DESEQUILIBRIO, com(MEDIO, "agilidade", 20)));
    }

    @Test
    void pesoTemInfluenciaModerada() {
        // Peso do minimo ao maximo muda a pontuacao em no maximo 0,20 (o peso do atributo).
        for (TipoDisputa t : TipoDisputa.values()) {
            double leve = ataque(t, com(MEDIO, "peso", 40));
            double pesado = ataque(t, com(MEDIO, "peso", 120));
            assertTrue(pesado - leve <= 0.20 * 1.10 + 1e-12, t.name());
        }
        // Muito pesado mas sem tecnica/forca perde para tecnico e forte de peso minimo.
        AtributosLutador pesadoFraco = new AtributosLutador(50, 50, 10, 50, 10, 10, 50, 120);
        AtributosLutador leveTecnico = new AtributosLutador(50, 50, 80, 50, 60, 90, 50, 40);
        assertTrue(ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, leveTecnico, 50, pesadoFraco, 50)
                .isSucessoDoAtaque());
    }

    // =====================================================
    // FOLEGO, EMPATES E EXTREMOS
    // =====================================================
    @Test
    void folegoImportaMasNuncaZeraNemPassaDoLimite() {
        double cheio = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, MEDIO, 50, MEDIO, 50).getPontuacaoAtaque();
        double vazio = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, MEDIO, 0, MEDIO, 50).getPontuacaoAtaque();
        assertTrue(vazio < cheio);
        assertEquals(cheio * 0.6, vazio, 1e-12, "exausto = 60% da forca de ataque");
        double negativo = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, MEDIO, -10, MEDIO, 50).getPontuacaoAtaque();
        double acima = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, MEDIO, 999, MEDIO, 50).getPontuacaoAtaque();
        assertEquals(vazio, negativo, 1e-12);
        assertEquals(cheio, acima, 1e-12);
    }

    @Test
    void empateFavoreceODefensor() {
        // Rompimento: fator de posicao 1,0. Mesmos pesos? Nao: usa atributos que deem pontuacoes iguais.
        AtributosLutador zero = new AtributosLutador(50, 50, 0, 50, 0, 0, 50, 40);
        ResultadoDisputa r = ResolvedorDisputa.resolver(TipoDisputa.ROMPIMENTO_TECNICO, zero, 50, zero, 50);
        assertEquals(0.0, r.getPontuacaoAtaque());
        assertEquals(0.0, r.getPontuacaoDefesa());
        assertFalse(r.isSucessoDoAtaque(), "empate: o defensor resiste");
        assertEquals(0.0, r.getMargem());
    }

    @Test
    void valoresExtremosNuncaGeramPontuacaoInvalida() {
        AtributosLutador absurdo = new AtributosLutador(Double.NaN, 0, 1e9, -5, Double.NaN, -1e9, 0, 1e9);
        for (TipoDisputa t : TipoDisputa.values()) {
            ResultadoDisputa r = ResolvedorDisputa.resolver(t, absurdo, Double.NaN, absurdo, Double.NEGATIVE_INFINITY);
            for (double v : new double[]{r.getPontuacaoAtaque(), r.getPontuacaoDefesa()}) {
                assertFalse(Double.isNaN(v) || Double.isInfinite(v));
                assertTrue(v >= 0 && v <= 1.10 + 1e-12, String.valueOf(v));
            }
        }
        assertThrows(IllegalArgumentException.class,
                () -> ResolvedorDisputa.resolver(null, MEDIO, 1, MEDIO, 1));
    }

    @Test
    void perfisDeliberadamenteDiferentesMudamOResultado() {
        Function<AtributosLutador, Boolean> desequilibraOPreto = a ->
                ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, a, a.getFolego(), PRETO, 40).isSucessoDoAtaque();

        AtributosLutador brancoFraco = new AtributosLutador(35, 25, 5, 22, 25, 5, 22, 45);
        AtributosLutador brancoTecnico = new AtributosLutador(35, 25, 60, 22, 25, 95, 22, 72);
        assertFalse(desequilibraOPreto.apply(brancoFraco), "sem forca nem tecnica: resiste");
        assertTrue(desequilibraOPreto.apply(brancoTecnico), "tecnico e forte: desequilibra");

        AtributosLutador pretoMuralha = new AtributosLutador(25, 40, 60, 45, 95, 95, 38, 110);
        assertFalse(ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, BRANCO, 25, pretoMuralha, 40)
                .isSucessoDoAtaque(), "defesa alta resiste ao Branco oficial");
    }
}
