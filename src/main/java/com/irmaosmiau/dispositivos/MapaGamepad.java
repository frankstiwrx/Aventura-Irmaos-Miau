package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.SlotJogador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Botoes do gamepad -> acoes logicas da 5C (Entrega 5D).
 *
 * ESQUEMA EXPERIMENTAL E PROVISORIO: os botoes abaixo NAO sao a decisao de
 * game design; estao todos aqui para serem trocados num lugar so. As regras
 * de combate continuam no SistemaCombate; o gamepad so produz acoes logicas.
 *
 * Um botao pode listar acoes de slots diferentes (ex.: LESTE = desequilibrar
 * para quem usa o Jogador 1 e romper para o Jogador 2): cada slot so enxerga
 * as acoes que usa. E o embriao do botao de tecnica contextual.
 */
public final class MapaGamepad {

    private final Map<BotaoGamepad, List<AcaoJogador>> acoes;
    private final BotaoGamepad botaoPausa;
    private final boolean analogicoEsquerdoMove;

    private MapaGamepad(Map<BotaoGamepad, List<AcaoJogador>> acoes, BotaoGamepad botaoPausa,
            boolean analogicoEsquerdoMove) {
        for (Map.Entry<BotaoGamepad, List<AcaoJogador>> e : acoes.entrySet()) {
            if (e.getKey() == botaoPausa && !e.getValue().isEmpty()) {
                throw new IllegalArgumentException("O botao de pausa nao pode ter acao: " + e.getKey());
            }
        }
        for (SlotJogador s : SlotJogador.values()) {
            Set<AcaoJogador> vistas = EnumSet.noneOf(AcaoJogador.class);
            for (List<AcaoJogador> lista : acoes.values()) {
                for (AcaoJogador a : lista) {
                    if (s.usa(a) && !vistas.add(a)) {
                        throw new IllegalArgumentException(s.getSigla() + ": " + a + " em dois botoes");
                    }
                }
            }
        }
        this.acoes = acoes;
        this.botaoPausa = botaoPausa;
        this.analogicoEsquerdoMove = analogicoEsquerdoMove;
    }

    /** Esquema provisorio da 5D (ver docs/CONTROLES.md). */
    public static MapaGamepad experimental() {
        Map<BotaoGamepad, List<AcaoJogador>> m = new EnumMap<>(BotaoGamepad.class);
        // Movimento: D-pad e analogico esquerdo (os dois somam, sem duplicar).
        m.put(BotaoGamepad.DPAD_ESQUERDA, List.of(AcaoJogador.MOVER_ESQUERDA));
        m.put(BotaoGamepad.DPAD_DIREITA, List.of(AcaoJogador.MOVER_DIREITA));
        m.put(BotaoGamepad.DPAD_CIMA, List.of(AcaoJogador.MOVER_CIMA));
        m.put(BotaoGamepad.DPAD_BAIXO, List.of(AcaoJogador.MOVER_BAIXO));
        // Corpo.
        m.put(BotaoGamepad.SUL, List.of(AcaoJogador.PULAR));
        m.put(BotaoGamepad.GATILHO_ESQ, List.of(AcaoJogador.AGACHAR));
        m.put(BotaoGamepad.RB, List.of(AcaoJogador.CORRER));
        m.put(BotaoGamepad.L3, List.of(AcaoJogador.ROLAR));
        m.put(BotaoGamepad.R3, List.of(AcaoJogador.DEITAR_LEVANTAR));
        m.put(BotaoGamepad.VOLTAR, List.of(AcaoJogador.RESPIRAR));
        m.put(BotaoGamepad.GATILHO_DIR, List.of(AcaoJogador.SPRAWL));
        // Combate.
        m.put(BotaoGamepad.OESTE, List.of(AcaoJogador.INICIAR_PEGADA));
        m.put(BotaoGamepad.LESTE, List.of(AcaoJogador.TENTAR_DESEQUILIBRIO, AcaoJogador.ROMPIMENTO_TECNICO));
        m.put(BotaoGamepad.NORTE, List.of(AcaoJogador.REFORCAR_PEGADA));
        m.put(BotaoGamepad.LB, List.of(AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO));
        // PULO_CURTO fica sem botao no esquema provisorio; INICIAR = pausa.
        return new MapaGamepad(m, BotaoGamepad.INICIAR, true);
    }

    public BotaoGamepad getBotaoPausa() {
        return botaoPausa;
    }

    /** Acoes seguradas agora por este slot (botoes + analogico, sem repeticao). */
    public Set<AcaoJogador> acoesAtivas(EstadoGamepad estado, SlotJogador slot, Set<AcaoJogador> analogicoAnterior,
            Set<AcaoJogador> analogicoAtual) {
        Set<AcaoJogador> ativas = EnumSet.noneOf(AcaoJogador.class);
        for (Map.Entry<BotaoGamepad, List<AcaoJogador>> e : acoes.entrySet()) {
            if (estado.pressionado(e.getKey())) {
                for (AcaoJogador a : e.getValue()) {
                    if (slot.usa(a)) {
                        ativas.add(a);
                    }
                }
            }
        }
        analogicoAtual.clear();
        if (analogicoEsquerdoMove) {
            analogicoAtual.addAll(LeitorAnalogico.direcoes(
                    estado.eixo(EixoGamepad.ESQ_X), estado.eixo(EixoGamepad.ESQ_Y), analogicoAnterior));
            ativas.addAll(analogicoAtual);
        }
        return ativas;
    }

    /** Botoes que acionam a acao neste slot. */
    public List<BotaoGamepad> botoes(SlotJogador slot, AcaoJogador acao) {
        List<BotaoGamepad> lista = new ArrayList<>();
        if (!slot.usa(acao)) {
            return lista;
        }
        for (Map.Entry<BotaoGamepad, List<AcaoJogador>> e : acoes.entrySet()) {
            if (e.getValue().contains(acao)) {
                lista.add(e.getKey());
            }
        }
        return Collections.unmodifiableList(lista);
    }

    public boolean isAnalogicoEsquerdoMove() {
        return analogicoEsquerdoMove;
    }

    /** Rotulo curto para ajuda e HUD ("B/Bola", "D-pad ← / analógico", "—"). */
    public String rotulo(SlotJogador slot, AcaoJogador acao) {
        List<String> partes = new ArrayList<>();
        for (BotaoGamepad b : botoes(slot, acao)) {
            partes.add(b.getRotulo());
        }
        boolean movimento = acao == AcaoJogador.MOVER_ESQUERDA || acao == AcaoJogador.MOVER_DIREITA
                || acao == AcaoJogador.MOVER_CIMA || acao == AcaoJogador.MOVER_BAIXO;
        if (movimento && analogicoEsquerdoMove && slot.usa(acao)) {
            partes.add("analógico");
        }
        return partes.isEmpty() ? "—" : String.join(" / ", partes);
    }
}
