package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.DirecaoTecnica;

/**
 * Analogico DIREITO como direcao de tecnica (Entrega 5H). Com estado: uma
 * tecnica por inclinacao intencional.
 *
 * 1. Zona morta radial (ZONA_MORTA) e normalizacao 0..1, como o esquerdo.
 * 2. Arma no NEUTRO: so pode escolher direcao depois de voltar abaixo de
 *    LIMIAR_NEUTRO (histerese: ativa em LIMIAR_ATIVAR, volta em LIMIAR_NEUTRO).
 * 3. Escolhe a direcao uma unica vez por inclinacao, so se o angulo estiver a
 *    no maximo MEIA_ABERTURA_GRAUS de um eixo; perto da diagonal (faixa
 *    ambigua) espera, sem escolher nada. Girar o analogico sem voltar ao
 *    centro nao troca a direcao.
 * 4. A direcao escolhida fica "segurada" ate o analogico voltar ao neutro;
 *    o ResolvedorComandosContextuais transforma isso em UM comando.
 * 5. desarmar() (R3 apertado, pausa, foco, desconexao): nada ate o neutro.
 */
public final class LeitorAnalogicoTecnicas {

    public static final double ZONA_MORTA = 0.25;
    public static final double LIMIAR_ATIVAR = 0.60;
    public static final double LIMIAR_NEUTRO = 0.30;
    public static final double MEIA_ABERTURA_GRAUS = 30.0;

    private boolean armado = true;
    private DirecaoTecnica segurada;

    /**
     * Uma leitura por tick.
     *
     * @param x eixo X do analogico direito (-1..1)
     * @param y eixo Y (-1..1, negativo = cima)
     * @return direcao segurada agora (null = nenhuma)
     */
    public DirecaoTecnica atualizar(double x, double y) {
        double[] n = LeitorAnalogico.normalizar(x, y);
        double modulo = Math.hypot(n[0], n[1]);
        if (modulo < LIMIAR_NEUTRO) {
            armado = true;
            segurada = null;
            return null;
        }
        if (segurada != null) {
            return segurada;                                // ja escolhida nesta inclinacao
        }
        if (!armado || modulo < LIMIAR_ATIVAR) {
            return null;
        }
        DirecaoTecnica d = direcaoClara(n[0], n[1]);
        if (d != null) {
            segurada = d;
            armado = false;
        }
        return segurada;
    }

    /** Direcao se o angulo estiver perto de um eixo; null na faixa diagonal. */
    static DirecaoTecnica direcaoClara(double x, double y) {
        double graus = Math.toDegrees(Math.atan2(-y, x));    // 0 = direita, 90 = cima
        if (graus < 0) {
            graus += 360;
        }
        DirecaoTecnica[] eixos = {DirecaoTecnica.DIREITA, DirecaoTecnica.CIMA, DirecaoTecnica.ESQUERDA,
            DirecaoTecnica.BAIXO};
        for (int i = 0; i < 4; i++) {
            double centro = i * 90.0;
            double dif = Math.abs(((graus - centro) + 540) % 360 - 180);
            if (dif <= MEIA_ABERTURA_GRAUS) {
                return eixos[i];
            }
        }
        return null;
    }

    /** Nada ate o analogico voltar ao neutro. */
    public void desarmar() {
        armado = false;
        segurada = null;
    }

    public DirecaoTecnica getSegurada() {
        return segurada;
    }
}
