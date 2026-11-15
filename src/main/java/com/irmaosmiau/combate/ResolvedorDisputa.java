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
 * Entrega 5B - FIRMEZA da pegada (f = firmeza / 100, entre 0 e 1):
 * <pre>
 * DESEQUILIBRIO:      fatorPosicaoAtaque = 1 + 0,10 * f    (1,10 com f = 1, como na 5A)
 * ROMPIMENTO_TECNICO: defesa *= 0,70 + 0,30 * f             (1,00 com f = 1, como na 5A)
 * </pre>
 * Firmeza alta: o aplicador mantem a vantagem de controle e segura melhor.
 * Firmeza baixa: ate 30% menos defesa no rompimento; nunca anula atributos.
 *
 * Pontuacoes ficam sempre em [0; 1,1]. Folego entra como multiplicador entre
 * 0,6 (exausto) e 1,0 (cheio): importa, mas nunca zera nem gera valor invalido.
 */
public final class ResolvedorDisputa {

    public static final double PESO_MINIMO_KG = 40.0;
    public static final double PESO_MAXIMO_KG = 120.0;
    public static final double FOLEGO_PISO = 0.6;
    /** Firmeza 0 deixa 70% da defesa do aplicador no rompimento tecnico. */
    public static final double PISO_CONTROLE_ROMPIMENTO = 0.7;
    public static final double FIRMEZA_MAXIMA = 100.0;
    static final double TOLERANCIA_EMPATE = 1e-9;

    private ResolvedorDisputa() {
    }

    /** Disputa entre duas instancias de lutador (atributos reais, inclusive de perfis). */
    public static ResultadoDisputa resolver(TipoDisputa tipo, IrmaoMiau atacante, IrmaoMiau defensor) {
        return resolver(tipo, atacante, defensor, FIRMEZA_MAXIMA);
    }

    /** Entrega 5B: com a firmeza atual da pegada (0..100). */
    /** Entrega 5L.1: com um multiplicador do ataque (insistencia; 1,0 = como antes). */
    public static ResultadoDisputa resolver(TipoDisputa tipo, IrmaoMiau atacante, IrmaoMiau defensor,
            double firmeza, double multiplicadorAtaque) {
        ResultadoDisputa r = resolver(tipo, atacante, defensor, firmeza);
        if (multiplicadorAtaque == 1.0) {
            return r;
        }
        double ataque = r.getPontuacaoAtaque() * multiplicadorAtaque;
        return new ResultadoDisputa(tipo, ataque, r.getPontuacaoDefesa(),
                ataque > r.getPontuacaoDefesa() + TOLERANCIA_EMPATE);
    }

    public static ResultadoDisputa resolver(TipoDisputa tipo, IrmaoMiau atacante, IrmaoMiau defensor,
            double firmeza) {
        return resolver(tipo,
                atacante.getAtributos(), atacante.getFolegoAtual(),
                defensor.getAtributos(), defensor.getFolegoAtual(), firmeza);
    }

    public static ResultadoDisputa resolver(
            TipoDisputa tipo,
            AtributosLutador atacante, double folegoAtacante,
            AtributosLutador defensor, double folegoDefensor
    ) {
        return resolver(tipo, atacante, folegoAtacante, defensor, folegoDefensor, FIRMEZA_MAXIMA);
    }

    public static ResultadoDisputa resolver(
            TipoDisputa tipo,
            AtributosLutador atacante, double folegoAtacante,
            AtributosLutador defensor, double folegoDefensor,
            double firmeza
    ) {
        if (tipo == null || atacante == null || defensor == null) {
            throw new IllegalArgumentException("Tipo e atributos dos dois lados sao obrigatorios.");
        }
        double f = limitar(firmeza / FIRMEZA_MAXIMA);
        double ataque = base(tipo.getPesosAtaque(), atacante)
                * fatorFolego(folegoAtacante, atacante.getFolego())
                * fatorPosicaoAtaque(tipo, f);
        double defesa = base(tipo.getPesosDefesa(), defensor)
                * fatorFolego(folegoDefensor, defensor.getFolego())
                * fatorControleDefesa(tipo, f);
        boolean sucesso = ataque > defesa + TOLERANCIA_EMPATE;
        return new ResultadoDisputa(tipo, ataque, defesa, sucesso);
    }

    /** Vantagem de quem controla a pegada, proporcional a firmeza (5B). */
    static double fatorPosicaoAtaque(TipoDisputa tipo, double f) {
        if (tipo == TipoDisputa.DESEQUILIBRIO) {
            return 1.0 + (tipo.getFatorPosicaoAtaque() - 1.0) * f;
        }
        return tipo.getFatorPosicaoAtaque();
    }

    /** Defesa do aplicador contra o rompimento cai com a firmeza (5B). */
    static double fatorControleDefesa(TipoDisputa tipo, double f) {
        if (tipo == TipoDisputa.ROMPIMENTO_TECNICO) {
            return PISO_CONTROLE_ROMPIMENTO + (1.0 - PISO_CONTROLE_ROMPIMENTO) * f;
        }
        return 1.0;
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
