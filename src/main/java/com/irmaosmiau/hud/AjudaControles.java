package com.irmaosmiau.hud;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.AcoesContextuais;
import com.irmaosmiau.controles.GrupoAcao;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.PapelCombate;
import com.irmaosmiau.controles.PosicaoComando;
import com.irmaosmiau.controles.RepertorioContextual;
import com.irmaosmiau.dispositivos.ComandoGamepad;
import com.irmaosmiau.dispositivos.EsquemaGamepad;
import com.irmaosmiau.dispositivos.ItemGamepad;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.MapaGamepad;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Textos de ajuda gerados a partir dos controles CONFIGURADOS (Entrega 5C):
 * nenhuma letra fixa no codigo do HUD. Com os controles padrao, o modo
 * individual mostra exatamente a mesma faixa de antes.
 *
 * Entrega 5D: o slot que estiver usando um gamepad mostra os botoes do
 * esquema experimental (MapaGamepad) no lugar das teclas.
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

    /**
     * De onde vem o rotulo de cada acao: teclas do slot ou, para os slots em
     * {@code noGamepad}, botoes do gamepad.
     */
    public record Rotulos(MapaControles teclado, MapaGamepad gamepad, Set<SlotJogador> noGamepad) {

        public static Rotulos soTeclado(MapaControles teclado) {
            return new Rotulos(teclado, null, EnumSet.noneOf(SlotJogador.class));
        }

        public boolean gamepad(SlotJogador slot) {
            return gamepad != null && noGamepad.contains(slot);
        }

        /** Entrega 5F: slot num gamepad no esquema CONTEXTUAL (tecnicas pelo D-pad). */
        public boolean contextual(SlotJogador slot) {
            return gamepad(slot) && gamepad.esquema(slot) == EsquemaGamepad.CONTEXTUAL;
        }

        public String tecla(SlotJogador slot, AcaoJogador acao) {
            if (contextual(slot) && gamepad.botoes(slot, acao).isEmpty()
                    && AcoesContextuais.ACOES_DE_COMBATE.contains(acao)) {
                return "RS/D-pad (contexto)";
            }
            return gamepad(slot) ? gamepad.rotulo(slot, acao) : teclado.rotulo(slot, acao);
        }

        String botaoCurto(SlotJogador slot, ComandoGamepad c) {
            return gamepad.botao(slot, ItemGamepad.de(c)).map(b -> b.getRotulo().split("/")[0]).orElse("—");
        }

        public String movimento(SlotJogador slot) {
            return gamepad(slot) ? "analógico" : AjudaControles.movimento(teclado, slot);
        }

        public String arrasto(SlotJogador slot) {
            if (gamepad(slot)) {
                return tecla(slot, AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO) + "+analógico";
            }
            return AjudaControles.arrasto(teclado, slot);
        }
    }

    private AjudaControles() {
    }

    // =====================================================
    // PEDACOS (teclado)
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
        return comando(Rotulos.soTeclado(mapa), slot, acao);
    }

    public static String comando(Rotulos r, SlotJogador slot, AcaoJogador acao) {
        if (acao == AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO) {
            return r.arrasto(slot) + " " + acao.getVerbo();
        }
        return r.tecla(slot, acao) + " " + acao.getVerbo();
    }

    /** Acoes de combate uteis agora, com as teclas configuradas. Vazio = nenhuma. */
    public static String agora(MapaControles mapa, SlotJogador slot, PapelCombate papel) {
        return agora(Rotulos.soTeclado(mapa), slot, papel);
    }

    public static String agora(Rotulos r, SlotJogador slot, PapelCombate papel) {
        List<String> partes = new ArrayList<>();
        for (AcaoJogador a : AcoesContextuais.disponiveis(papel, slot)) {
            java.util.Optional<PosicaoComando> pos = RepertorioContextual.posicaoDe(papel, a);
            if (r.contextual(slot) && pos.isPresent() && r.gamepad.botoes(slot, a).isEmpty()) {
                partes.add("RS" + pos.get().direcao().getSeta() + " " + a.getVerbo());
            } else {
                partes.add(comando(r, slot, a));
            }
        }
        return String.join(" · ", partes);
    }

    // =====================================================
    // FAIXA DE DICAS DO HUD (uma linha, contextual)
    // =====================================================
    public static String dicaHud(Modo modo, MapaControles mapa, PapelCombate papelJ1, PapelCombate papelJ2) {
        return dicaHud(modo, Rotulos.soTeclado(mapa), papelJ1, papelJ2);
    }

    public static String dicaHud(Modo modo, Rotulos r, PapelCombate papelJ1, PapelCombate papelJ2) {
        SlotJogador j1 = SlotJogador.JOGADOR_1;
        String pausa = r.gamepad(j1) ? "Start/ESC pausa" : "ESC pausa";
        switch (modo) {
            case INDIVIDUAL:
                return r.movimento(j1) + " mover · "
                        + r.tecla(j1, AcaoJogador.CORRER) + " correr · "
                        + r.tecla(j1, AcaoJogador.PULAR) + " pular · "
                        + r.tecla(j1, AcaoJogador.PULO_CURTO) + " pulo curto · "
                        + r.tecla(j1, AcaoJogador.AGACHAR) + " agachar · " + pausa + " · F4 cenário";
            case TREINO:
                return juntar(r.movimento(j1) + " · " + r.tecla(j1, AcaoJogador.PULAR) + " pular",
                        agora(r, j1, papelJ1), "F5 reiniciar · " + pausa + "/controles");
            default:
                SlotJogador j2 = SlotJogador.JOGADOR_2;
                return "J1: " + juntar(r.movimento(j1), agora(r, j1, papelJ1))
                        + "   J2: " + juntar(r.movimento(j2), agora(r, j2, papelJ2))
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
        return linhas(Rotulos.soTeclado(mapa), slot);
    }

    /** Idem, com os botoes do gamepad se o slot estiver usando um (5D). */
    public static List<Linha> linhas(Rotulos r, SlotJogador slot) {
        List<Linha> lista = new ArrayList<>();
        for (GrupoAcao g : GrupoAcao.values()) {
            List<Linha> doGrupo = new ArrayList<>();
            for (AcaoJogador a : slot.acoes()) {
                if (a.getGrupo() == g) {
                    String t;
                    if (a == AcaoJogador.MODO_DESLOCAMENTO_CONTROLADO) {
                        t = r.gamepad(slot)
                                ? r.tecla(slot, a) + " (+analógico)"
                                : r.tecla(slot, a) + " (+" + r.tecla(slot, AcaoJogador.MOVER_ESQUERDA)
                                        + "/" + r.tecla(slot, AcaoJogador.MOVER_DIREITA) + ")";
                    } else {
                        t = r.tecla(slot, a);
                    }
                    doGrupo.add(new Linha(a.getRotulo(), t));
                }
            }
            if (!doGrupo.isEmpty()) {
                lista.add(new Linha(g.getTitulo(), null));
                lista.addAll(doGrupo);
            }
        }
        if (r.contextual(slot)) {
            String m2 = r.botaoCurto(slot, ComandoGamepad.MODIFICADOR_CAMADA_2);
            String m3 = r.botaoCurto(slot, ComandoGamepad.MODIFICADOR_CAMADA_3);
            lista.add(new Linha("TÉCNICAS (contextuais, 3 camadas)", null));
            lista.add(new Linha("Camada 1 (analógico direito ou D-pad)", "RS"));
            lista.add(new Linha("Camada 2", m2 + " + RS"));
            lista.add(new Linha("Camada 3", m3 + " + RS"));
            lista.add(new Linha("Camada avançada (reservada)", m2 + " + " + m3 + " + RS"));
        }
        if (r.gamepad(slot)) {
            lista.add(new Linha(GrupoAcao.SISTEMA.getTitulo(), null));
            lista.add(new Linha("Pausar / continuar", r.gamepad().getBotaoPausa().getRotulo()));
        }
        return lista;
    }
}
