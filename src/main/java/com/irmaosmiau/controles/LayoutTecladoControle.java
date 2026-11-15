package com.irmaosmiau.controles;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Entrega 5O.1: teclado como CONTROLE (Xbox) para um jogador.
 *
 * O J2 no teclado deixa de usar teclas de acao "soltas": cada tecla deste
 * layout faz o papel de um botao/analogico do controle, e a leitura passa pelo
 * MESMO caminho do gamepad (MapaGamepad do slot, esquema Classico ou
 * Contextual, familias RT/RB, R3 contextual, Select = RB...). Assim teclado e
 * Xbox fazem exatamente a mesma coisa.
 *
 * Alvos: o nome de um BotaoGamepad ("SUL", "RB", "GATILHO_DIR"...) ou um
 * analogico: "LS_CIMA/BAIXO/ESQUERDA/DIREITA" (movimento) e
 * "RS_CIMA/BAIXO/ESQUERDA/DIREITA" (tecnicas).
 *
 * Layout fixo do J2 (pedido do usuario na 5O.1):
 * WASD = LS; setas = RS; K/L/J/I = A/B/X/Y; Z = L3; M = R3;
 * T/H/G/F = D-pad cima/direita/baixo/esquerda; O/P/E/Q = RB/RT/LB/LT;
 * ENTER = Start; SHIFT = Select/View (os dois Shifts: o Swing nao separa
 * esquerdo/direito nos atalhos).
 *
 * Desligar (voltar as teclas livres configuraveis do J2 de antes da 5O.1):
 * -Dirmaosmiau.teclado.j2=livre.
 */
public final class LayoutTecladoControle {

    public static final String PROPRIEDADE_J2 = "irmaosmiau.teclado.j2";

    private static final Map<String, String> J2 = new LinkedHashMap<>();

    static {
        J2.put("W", "LS_CIMA");
        J2.put("S", "LS_BAIXO");
        J2.put("A", "LS_ESQUERDA");
        J2.put("D", "LS_DIREITA");
        J2.put("UP", "RS_CIMA");
        J2.put("DOWN", "RS_BAIXO");
        J2.put("LEFT", "RS_ESQUERDA");
        J2.put("RIGHT", "RS_DIREITA");
        J2.put("K", "SUL");
        J2.put("L", "LESTE");
        J2.put("J", "OESTE");
        J2.put("I", "NORTE");
        J2.put("Z", "L3");
        J2.put("M", "R3");
        J2.put("T", "DPAD_CIMA");
        J2.put("H", "DPAD_DIREITA");
        J2.put("G", "DPAD_BAIXO");
        J2.put("F", "DPAD_ESQUERDA");
        J2.put("O", "RB");
        J2.put("P", "GATILHO_DIR");
        J2.put("E", "LB");
        J2.put("Q", "GATILHO_ESQ");
        J2.put("ENTER", "INICIAR");
        J2.put("SHIFT", "VOLTAR");
    }

    private static Set<SlotJogador> ativosForcados;

    private LayoutTecladoControle() {
    }

    /** Teclas (nome do KeyStroke) -> alvo no controle, do slot (vazio = slot sem teclado-controle). */
    public static Map<String, String> layout(SlotJogador slot) {
        return slot == SlotJogador.JOGADOR_2 ? Collections.unmodifiableMap(J2) : Map.of();
    }

    /** O slot usa o teclado como controle? Padrao: so o J2. */
    public static boolean usa(SlotJogador slot) {
        if (ativosForcados != null) {
            return ativosForcados.contains(slot);
        }
        return slot == SlotJogador.JOGADOR_2
                && !"livre".equalsIgnoreCase(System.getProperty(PROPRIEDADE_J2, "controle"));
    }

    /** Testes: fixa quem usa o teclado como controle (null = padrao). */
    public static void definirParaTestes(Set<SlotJogador> slots) {
        ativosForcados = slots == null ? null
                : slots.isEmpty() ? EnumSet.noneOf(SlotJogador.class) : EnumSet.copyOf(slots);
    }

    /** Slot dono da tecla no teclado-controle (null = nenhum). */
    public static SlotJogador donoDaTecla(TeclaFisica t) {
        for (SlotJogador s : SlotJogador.values()) {
            if (usa(s) && layout(s).containsKey(t.getNome())) {
                return s;
            }
        }
        return null;
    }

    /** Tecla do layout que faz o papel do alvo (rotulo; "?" se nenhuma). */
    public static String teclaDo(SlotJogador slot, String alvo) {
        for (Map.Entry<String, String> e : layout(slot).entrySet()) {
            if (e.getValue().equals(alvo)) {
                return TeclaFisica.porNome(e.getKey()).map(TeclaFisica::getRotulo).orElse(e.getKey());
            }
        }
        return "?";
    }
}
