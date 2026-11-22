package com.irmaosmiau.posicional;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.regras.Graduacao;

/**
 * Permissao de GRADUACAO para uma tecnica (o unico lugar que decide isso; o MotorSolo pergunta aqui).
 *
 * Tres coisas independentes num lutador:
 * - aparencia: cores (incluindo a faixa visual), que nunca liberam nem bloqueiam nada;
 * - atributos: os salvos (perfil) ou os da faixa (original), nunca alterados aqui;
 * - permissoes: esta classe.
 *
 * Regra:
 * - personagem ORIGINAL: a graduacao escolhida na selecao precisa alcancar a da tecnica (como sempre);
 * - PERFIL personalizado ({@link IrmaoMiau#isTecnicasDaBaseLiberadas()}): nos modos casuais e no treino
 *   a graduacao nao bloqueia: valem todas as tecnicas que a base tem (a lista de aprendidas da base,
 *   posicao, papel, pegada, folego e estado continuam valendo no MotorSolo);
 * - com REGULAMENTO DE FAIXA ligado (PONTOS + FINALIZACAO, onde a graduacao faz o papel da tabela da
 *   IBJJF), ninguem e liberado: o perfil conta com a graduacao logica dele, que e a branca. A cor da faixa
 *   desenhada nunca entra nesta conta.
 */
public final class PermissoesTecnicas {

    private PermissoesTecnicas() {
    }

    /**
     * @param exigida graduacao minima da tecnica (null = nenhuma)
     * @param regulamentoDeFaixa true no modo competitivo (pontos): a graduacao logica vale para todos
     */
    public static boolean graduacaoPermite(IrmaoMiau lutador, Graduacao exigida, boolean regulamentoDeFaixa) {
        if (lutador.isTecnicasDaBaseLiberadas() && !regulamentoDeFaixa) {
            return true;
        }
        return lutador.getGraduacao().alcanca(exigida);
    }
}
