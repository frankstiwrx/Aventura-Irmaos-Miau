package com.irmaosmiau.animacao;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Desenha uma animacao JSON pelo RENDERIZADOR DO PROPRIO personagem (Entrega 5E).
 *
 * Usa o canal de pose externa que o Branco/Preto ja tem (o mesmo da
 * PEGADA_1): desenharPoseMiauStudio com o deslocamento visual da animacao.
 * Nao toca em arrays, renderizadores nem na posicao do lutador.
 *
 * Seguranca: so aplica se a pose externa estiver livre (nunca sobrescreve a
 * pose do motor de combate) e so limpa a pose que ele mesmo aplicou.
 */
public final class ReprodutorPoseJson {

    private final AnimacaoIndividual animacao;
    private IrmaoMiau aplicadoEm;
    private int[] ultimaPose;

    public ReprodutorPoseJson(AnimacaoIndividual animacao) {
        this.animacao = animacao;
    }

    /**
     * Mostra o quadro (indice de quadro, 0-based) no lutador.
     *
     * @return false se a pose externa ja pertence a outro sistema (ex.: combate)
     */
    public boolean aplicarQuadro(IrmaoMiau lutador, int indiceQuadro) {
        if (lutador.isPoseCombateExternaAtiva() && lutador != aplicadoEm) {
            return false;
        }
        if (lutador == aplicadoEm && lutador.isPoseCombateExternaAtiva()
                && !java.util.Arrays.equals(lutador.copiarPoseCombateExterna(), ultimaPose)) {
            return false;                        // alguem trocou a pose depois de nos
        }
        int[] pose = animacao.quadro(indiceQuadro);
        lutador.aplicarPoseCombateExterna(pose, animacao.getDeslocamentoVisualX(), animacao.getDeslocamentoVisualY());
        aplicadoEm = lutador;
        ultimaPose = pose;
        return true;
    }

    /** Quadro pelo tempo desde o inicio (mesma conta do legado). */
    public boolean aplicarNoTempo(IrmaoMiau lutador, long tempoMs) {
        return aplicarQuadro(lutador, animacao.indiceQuadroNoTempo(tempoMs));
    }

    /** Devolve o lutador ao desenho legado (so se a pose ainda for a nossa). */
    public void liberar() {
        if (aplicadoEm != null && aplicadoEm.isPoseCombateExternaAtiva()
                && java.util.Arrays.equals(aplicadoEm.copiarPoseCombateExterna(), ultimaPose)) {
            aplicadoEm.limparPoseCombateExterna();
        }
        aplicadoEm = null;
        ultimaPose = null;
    }
}
