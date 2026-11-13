package com.irmaosmiau.combate;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Motor de disputas tecnicas (Entrega 5A): deterministico, sem aleatoriedade,
 * sem interface grafica.
 *
 * <pre>
 * norm(a)      = limitar(a / 100, 0, 1)            tecnica, forca, agilidade
 * normPeso(p)  = limitar((p - 40) / 80, 0, 1)      peso 40 kg -&gt; 0, 120 kg -&gt; 1
 * base         = sum(peso_i * norm_i)              pesos do TipoDisputa (soma 1)
 * fatorFolego  = 0,6 + 0,4 * limitar(folegoAtual / folegoMaximo, 0, 1)
 * ataque       = base * fatorFolego * fatorPosicaoAtaque
 * defesa       = base * fatorFolego
 * SUCESSO      se ataque &gt; defesa + 1e-9 ; empate = o defensor resiste
 * </pre>
 *
 * Pontuacoes ficam sempre em [0; 1,1]. Folego entra como multiplicador entre
 * 0,6 (exausto) e 1,0 (cheio): importa, mas nunca zera nem gera valor invalido.
 */
public final class ResolvedorDisputa {

    public static final double PESO_MINIMO_KG = 40.0;
    public static final double PESO_MAXIMO_KG = 120.0;
    public static final double FOLEGO_PISO = 0.6;
    static final double TOLERANCIA_EMPATE = 1e-9;

    private ResolvedorDisputa() {
    }

    /** Disputa entre duas instancias de lutador (atributos reais, inclusive de perfis). */
    public static ResultadoDisputa resolver(TipoDisputa tipo, IrmaoMiau atacante, IrmaoMiau defensor) {
        return resolver(tipo,
                atacante.getAtributos(), atacante.getFolegoAtual(),
                defensor.getAtributos(), defensor.getFolegoAtual());
    }

    public static ResultadoDisputa resolver(
            TipoDisputa tipo,
            AtributosLutador atacante, double folegoAtacante,
            AtributosLutador defensor, double folegoDefensor
    ) {
        if (tipo == null || atacante == null || defensor == null) {
            throw new IllegalArgumentException("Tipo e atributos dos dois lados sao obrigatorios.");
        }
        double ataque = base(tipo.getPesosAtaque(), atacante)
                * fatorFolego(folegoAtacante, atacante.getFolego())
                * tipo.getFatorPosicaoAtaque();
        double defesa = base(tipo.getPesosDefesa(), defensor)
                * fatorFolego(folegoDefensor, defensor.getFolego());
        boolean sucesso = ataque > defesa + TOLERANCIA_EMPATE;
        return new ResultadoDisputa(tipo, ataque, defesa, sucesso);
    }

    static double base(TipoDisputa.Pesos p, AtributosLutador a) {
        return p.getTecnica() * norm(a.getTecnica())
                + p.getForca() * norm(a.getForca())
                + p.getAgilidade() * norm(a.getAgilidade())
                + p.getPesoCorporal() * normPeso(a.getPeso());
    }

    static double norm(double valor) {
        return limitar(valor / 100.0);
    }

    static double normPeso(double kg) {
        return limitar((kg - PESO_MINIMO_KG) / (PESO_MAXIMO_KG - PESO_MINIMO_KG));
    }

    static double fatorFolego(double atual, double maximo) {
        double proporcao = maximo > 0 ? limitar(atual / maximo) : 0.0;
        return FOLEGO_PISO + (1.0 - FOLEGO_PISO) * proporcao;
    }

    private static double limitar(double v) {
        if (Double.isNaN(v)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }
}
