package com.irmaosmiau.conteudo.miaustudio;

import com.irmaosmiau.conteudo.json.ObjetoJson;

import java.util.List;
import java.util.Locale;

/**
 * Converte projetos "MIAUSTUDIO_PROJECT" versao 2 em animacoes conjuntas
 * runtime (arrays de 30 inteiros), sem depender da classe do editor.
 *
 * A ordem dos 30 valores e EXATAMENTE a de PoseSalva.paraLinhaJava() no
 * MiauStudio (exportacao conjunta):
 *
 * <pre>
 *  0 offsetCentroX  1 offsetLinha  2 escalaX10  3 viradoDireita(1/0)
 *  4 offCinturaX  5 offCinturaY  6 rotCintura
 *  7 offPeitoX    8 offPeitoY    9 rotPeito
 * 10 rotGlobal   11 rotCabeca   12 offCabX   13 offCabY
 * 14 offCotEsqX  15 offCotEsqY  16 offMaoEsqX  17 offMaoEsqY
 * 18 offCotDirX  19 offCotDirY  20 offMaoDirX  21 offMaoDirY
 * 22 offJoelEsqX 23 offJoelEsqY 24 offPeEsqX   25 offPeEsqY
 * 26 offJoelDirX 27 offJoelDirY 28 offPeDirX   29 offPeDirY
 * </pre>
 *
 * Diferente do editor (que usa padroes para campos ausentes), o jogo
 * EXIGE todos os campos: um quadro incompleto vira erro com o caminho
 * exato, em vez de uma pose silenciosamente zerada.
 *
 * O arquivo de origem nunca e alterado.
 */
public final class ConversorMiauStudio {

    public static final String TIPO_PROJETO = "MIAUSTUDIO_PROJECT";
    public static final int VERSAO_SUPORTADA = 2;

    /** Limites iguais aos do MiauStudio. */
    public static final int MAX_QUADROS = 60;
    private static final int ESCALA_X10_MIN = 2;
    private static final int ESCALA_X10_MAX = 30;
    private static final int LINHA_MAX = 4;

    /** Campos das articulacoes, na ordem dos indices 4..29. */
    private static final String[] CAMPOS_ARTICULACOES = {
        "offCinturaX", "offCinturaY", "rotCintura",
        "offPeitoX", "offPeitoY", "rotPeito",
        "rotGlobal", "rotCabeca", "offCabX", "offCabY",
        "offCotEsqX", "offCotEsqY", "offMaoEsqX", "offMaoEsqY",
        "offCotDirX", "offCotDirY", "offMaoDirX", "offMaoDirY",
        "offJoelEsqX", "offJoelEsqY", "offPeEsqX", "offPeEsqY",
        "offJoelDirX", "offJoelDirY", "offPeDirX", "offPeDirY"
    };

    private ConversorMiauStudio() {
    }

    /**
     * Importa a timeline conjunta de um projeto.
     *
     * @param textoJson conteudo do arquivo .json do MiauStudio
     * @param origem    nome do arquivo, usado nas mensagens de erro
     */
    public static AnimacaoConjuntaMiauStudio importarConjunta(String textoJson, String origem) {

        ObjetoJson projeto = ObjetoJson.ler(textoJson, origem);

        String tipo = projeto.texto("tipo");
        if (!TIPO_PROJETO.equals(tipo)) {
            throw projeto.erro("tipo", "esperado \"" + TIPO_PROJETO + "\", encontrado \"" + tipo + "\"");
        }

        int versao = projeto.inteiro("versao");
        if (versao != VERSAO_SUPORTADA) {
            throw projeto.erro("versao", "versao " + versao + " nao suportada; o jogo importa "
                    + "somente projetos versao " + VERSAO_SUPORTADA
                    + " (abra e salve o projeto no MiauStudio atual)");
        }

        String nomeAnimacao = projeto.texto("nomeAnimacao");

        String aplicador = projeto.texto("aplicadorConjunto");
        String chaveAplicador;
        String chaveReceptor;
        String nomeReceptor;
        switch (aplicador.toLowerCase(Locale.ROOT)) {
            case "branco":
                aplicador = "Branco";
                chaveAplicador = "branco";
                chaveReceptor = "preto";
                nomeReceptor = "Preto";
                break;
            case "preto":
                aplicador = "Preto";
                chaveAplicador = "preto";
                chaveReceptor = "branco";
                nomeReceptor = "Branco";
                break;
            default:
                throw projeto.erro("aplicadorConjunto",
                        "esperado \"Branco\" ou \"Preto\", encontrado \"" + aplicador + "\"");
        }

        int centroX = projeto.inteiro("centroInteracaoX");
        int linha = projeto.inteiroEntre("linhaInteracao", 0, LINHA_MAX);
        int offsetVisualX = projeto.inteiroOpcional("offsetVisualAnimacaoX", 0);
        int offsetVisualY = projeto.inteiroOpcional("offsetVisualAnimacaoY", 0);

        if (!projeto.contem("timelineConjunta")) {
            throw projeto.erro("timelineConjunta",
                    "o projeto nao possui animacao conjunta");
        }
        int total = projeto.tamanhoLista("timelineConjunta");
        if (total == 0) {
            throw projeto.erro("timelineConjunta", "a animacao conjunta nao possui quadros");
        }
        if (total > MAX_QUADROS) {
            throw projeto.erro("timelineConjunta",
                    total + " quadros; o maximo do MiauStudio e " + MAX_QUADROS);
        }

        List<ObjetoJson> quadros = projeto.listaDeObjetos("timelineConjunta");

        int[][] trackAplicador = new int[total][];
        int[][] trackReceptor = new int[total][];

        for (int i = 0; i < total; i++) {
            ObjetoJson quadro = quadros.get(i);
            trackAplicador[i] = converterPose(quadro, chaveAplicador, i);
            trackReceptor[i] = converterPose(quadro, chaveReceptor, i);
        }

        return new AnimacaoConjuntaMiauStudio(
                nomeAnimacao, aplicador, nomeReceptor,
                centroX, linha, offsetVisualX, offsetVisualY,
                trackAplicador, trackReceptor);
    }

    private static int[] converterPose(ObjetoJson quadro, String personagem, int indice) {

        if (!quadro.contem(personagem)) {
            throw quadro.erro(personagem, "o quadro " + (indice + 1)
                    + " nao possui a pose do " + personagem
                    + " (quadro incompleto no MiauStudio)");
        }

        ObjetoJson pose = quadro.objeto(personagem);
        int[] valores = new int[30];

        valores[0] = pose.inteiro("offsetCentroX");
        valores[1] = pose.inteiroEntre("offsetLinha", -LINHA_MAX, LINHA_MAX);
        valores[2] = pose.inteiroEntre("escalaX10", ESCALA_X10_MIN, ESCALA_X10_MAX);
        valores[3] = pose.booleano("viradoDireita") ? 1 : 0;

        for (int c = 0; c < CAMPOS_ARTICULACOES.length; c++) {
            valores[4 + c] = pose.inteiro(CAMPOS_ARTICULACOES[c]);
        }
        return valores;
    }
}
