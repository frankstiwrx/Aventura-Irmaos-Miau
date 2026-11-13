package com.irmaosmiau.hud;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.AcoesContextuais;
import com.irmaosmiau.controles.GrupoAcao;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.PapelCombate;
import com.irmaosmiau.controles.SlotJogador;

import java.util.ArrayList;
import java.util.List;

/**
 * Textos de ajuda gerados a partir dos controles CONFIGURADOS (Entrega 5C):
 * nenhuma letra fixa no codigo do HUD. Com os controles padrao, o modo
 * individual mostra exatamente a mesma faixa de antes.
 */
public final class AjudaControles {

    public enum Modo {
        INDIVIDUAL,
        DOIS_JOGADORES,
        TREINO
    }

    /** Atalhos fixos do sistema (nao configuraveis). */
    public static final String SISTEMA =
            "ESC pausa · F3 debug · F4 cenário · F5 reiniciar treino · F9 música · F10 efeitos";

    /** Uma linha da tela de ajuda: titulo de grupo (tecla null) ou acao + tecla. */
    public record Linha(String texto, String tecla) {
        public boolean isTitulo() {
            return tecla == null;
        }
    }

    private AjudaControles() {
    }

    // =====================================================
    // PEDACOS
    // =====================================================
    public static String tecla(MapaControles mapa, SlotJogador slot, AcaoJogador acao) {
        return mapa.rotulo(slot, acao);
    }

    /** "WASD", "setas" ou as quatro teclas (cima/esquerda/baixo/direita). */
    public static String movimento(MapaControles mapa, SlotJogador slot) {
        String c = mapa.tecla(slot, AcaoJogador.MOVER_CIMA).map(t -> t.getNome()).orElse("");
        String e = mapa.tecla(slot, AcaoJogador.MOVER_ESQUERDA).map(t -> t.getNome()).orElse("");
        String b = mapa.tecla(slot, AcaoJogador.MOVER_BAIXO).map(t -> t.getNome()).orElse("");
        String d = mapa.tecla(slot, AcaoJogador.MOVER_DIREITA).map(t -> t.getNome()).orElse("");
        if (c.equals("W") && e.equals("A") && b.equals("S") && d.equals("D")) {
            return "WASD";
        }
        if (c.equals("UP") && e.equals("LEFT") && b.equals("DOWN") && d.equals("RIGHT")) {
            return "setas";
        }
        return tecla(mapa, slot, AcaoJogador.MOVER_CIMA) + "/" + tecla(mapa, slot, AcaoJogador.MOVER_ESQUERDA)
                + "/" + tecla(mapa, slot, AcaoJogador.MOVER_BAIXO) + "/" + tecla(mapa, slot, AcaoJogador.MOVER_DIREITA);
    }

    /** Ex.: "SHIFT+A/D". */
    public static String arrasto(MapaControles mapa, SlotJogador slot) {
        return tecla(mapa, slot, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO) + "+"
                + tecla(mapa, slot, AcaoJogador.MOVER_ESQUERDA) + "/"
                + tecla(mapa, slot, AcaoJogador.MOVER_DIREITA);
    }

    /** "TECLA verbo" de uma acao (o arrasto mostra a combinacao). */
    public static String comando(MapaControles mapa, SlotJogador slot, AcaoJogador acao) {
        if (acao == AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO) {
            return arrasto(mapa, slot) + " " + acao.getVerbo();
        }
        return tecla(mapa, slot, acao) + " " + acao.getVerbo();
    }

    /** Acoes de combate uteis agora, com as teclas configuradas. Vazio = nenhuma. */
    public static String agora(MapaControles mapa, SlotJogador slot, PapelCombate papel) {
        List<String> partes = new ArrayList<>();
        for (AcaoJogador a : AcoesContextuais.disponiveis(papel, slot)) {
            partes.add(comando(mapa, slot, a));
        }
        return String.join(" · ", partes);
    }

    // =====================================================
    // FAIXA DE DICAS DO HUD (uma linha, contextual)
    // =====================================================
    public static String dicaHud(Modo modo, MapaControles mapa, PapelCombate papelJ1, PapelCombate papelJ2) {
        SlotJogador j1 = SlotJogador.JOGADOR_1;
        switch (modo) {
            case INDIVIDUAL:
                return movimento(mapa, j1) + " mover · "
                        + tecla(mapa, j1, AcaoJogador.CORRER) + " correr · "
                        + tecla(mapa, j1, AcaoJogador.PULAR) + " pular · "
                        + tecla(mapa, j1, AcaoJogador.PULO_CURTO) + " pulo curto · "
                        + tecla(mapa, j1, AcaoJogador.AGACHAR) + " agachar · ESC pausa · F4 cenário";
            case TREINO:
                return juntar(movimento(mapa, j1) + " · " + tecla(mapa, j1, AcaoJogador.PULAR) + " pular",
                        agora(mapa, j1, papelJ1), "F5 reiniciar · ESC pausa/controles");
            default:
                SlotJogador j2 = SlotJogador.JOGADOR_2;
                return "J1: " + juntar(movimento(mapa, j1), agora(mapa, j1, papelJ1))
                        + "   J2: " + juntar(movimento(mapa, j2), agora(mapa, j2, papelJ2))
                        + "   ESC pausa/controles · F3 · F4";
        }
    }

    private static String juntar(String... partes) {
        List<String> uteis = new ArrayList<>();
        for (String p : partes) {
            if (p != null && !p.isBlank()) {
                uteis.add(p);
            }
        }
        return String.join(" · ", uteis);
    }

    // =====================================================
    // TELA DE AJUDA (pausa -> CONTROLES)
    // =====================================================
    /** Todas as acoes do slot, agrupadas, com as teclas configuradas. */
    public static List<Linha> linhas(MapaControles mapa, SlotJogador slot) {
        List<Linha> lista = new ArrayList<>();
        for (GrupoAcao g : GrupoAcao.values()) {
            List<Linha> doGrupo = new ArrayList<>();
            for (AcaoJogador a : slot.acoes()) {
                if (a.getGrupo() == g) {
                    String t = a == AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO
                            ? tecla(mapa, slot, a) + " (+" + tecla(mapa, slot, AcaoJogador.MOVER_ESQUERDA)
                                    + "/" + tecla(mapa, slot, AcaoJogador.MOVER_DIREITA) + ")"
                            : tecla(mapa, slot, a);
                    doGrupo.add(new Linha(a.getRotulo(), t));
                }
            }
            if (!doGrupo.isEmpty()) {
                lista.add(new Linha(g.getTitulo(), null));
                lista.addAll(doGrupo);
            }
        }
        return lista;
    }
}
