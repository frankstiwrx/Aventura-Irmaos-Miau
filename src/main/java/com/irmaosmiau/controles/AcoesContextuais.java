package com.irmaosmiau.controles;

import java.util.ArrayList;
import java.util.List;

/**
 * Ponto de extensao das acoes CONTEXTUAIS de combate (Entrega 5C).
 *
 * Hoje: diz quais acoes de combate fazem sentido para o papel atual do
 * lutador (ajuda contextual e faixa de dicas) e em que ordem o GamePanel
 * despacha as acoes de combate. Os resultados continuam vindo do
 * SistemaCombate, exatamente como na 5B (H, T, Y e U nao mudaram).
 *
 * Futuro (plano em docs/CONTROLES.md): uma acao generica EXECUTAR_TECNICA
 * consultara aqui as tecnicas EQUIPADAS do lutador, filtradas pelo papel e
 * pelo estado (alcance, firmeza, folego), para escolher qual executar sem
 * exigir uma tecla por tecnica.
 */
public final class AcoesContextuais {

    /** Acoes de combate lidas a cada tick, na ordem de despacho da 5B (H, T, Y, U). */
    public static final List<AcaoJogador> ACOES_DE_COMBATE = List.of(
            AcaoJogador.INICIAR_PEGADA,
            AcaoJogador.TENTAR_DESEQUILIBRIO,
            AcaoJogador.REFORCAR_PEGADA,
            AcaoJogador.ROMPIMENTO_TECNICO);

    private AcoesContextuais() {
    }

    /** Acoes de combate uteis agora para este papel, entre as que o slot usa. */
    public static List<AcaoJogador> disponiveis(PapelCombate papel, SlotJogador slot) {
        List<AcaoJogador> candidatas = switch (papel) {
            case LIVRE -> List.of(AcaoJogador.INICIAR_PEGADA);
            case ENTRANDO -> List.of();
            case APLICADOR -> List.of(
                    AcaoJogador.TENTAR_DESEQUILIBRIO,
                    AcaoJogador.REFORCAR_PEGADA,
                    AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO);
            case RECEPTOR -> List.of(AcaoJogador.ROMPIMENTO_TECNICO);
        };
        List<AcaoJogador> lista = new ArrayList<>();
        for (AcaoJogador a : candidatas) {
            if (slot.usa(a)) {
                lista.add(a);
            }
        }
        return List.copyOf(lista);
    }
}
