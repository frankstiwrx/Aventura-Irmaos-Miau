package com.irmaosmiau.ui;

import com.irmaosmiau.cenario.Cenario;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.regras.ModoLuta;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Segunda tela de JOGAR: CONFIGURACOES DA LUTA (ARENA | TEMPO | DIFICULDADE | REGRAS e INICIAR LUTA).
 *
 * So estado, comandos e desenho; a {@link SelecaoPartidaPanel} cuida das entradas (as mesmas da primeira
 * tela, entao um aperto nunca vale nas duas) e das escolhas dos lutadores, que ficam intactas.
 *
 * - As arenas vem do {@link RegistroCenarios} (nenhuma lista duplicada) e a previa e desenhada pelo
 *   proprio cenario, reduzida.
 * - DIFICULDADE fica EM BREVE (nao ha IA de combate): nao recebe foco.
 * - REGRAS mostra os {@link ModoLuta}; os EM BREVE podem ser vistos, mas INICIAR LUTA recusa.
 */
final class ConfiguracaoLuta {

    /** O que a tela pede depois de um comando. */
    enum Resultado { NADA, INICIAR, VOLTAR }

    /** Areas com foco (DIFICULDADE nao entra enquanto estiver EM BREVE). */
    enum Area { ARENA, TEMPO, DIFICULDADE, REGRAS, INICIAR }

    static final Rectangle PREVIA = new Rectangle(40, 104, 336, 189);
    static final Rectangle BOTAO_INICIAR = new Rectangle(300, 366, 200, 36);

    private final RegistroCenarios cenarios;
    private final List<String> arenas;
    private final List<String> nomesArenas = new ArrayList<>();
    private final Map<String, BufferedImage> previas = new HashMap<>();
    private int arena;
    private int tempo;
    private int regra;
    private Area foco = Area.ARENA;
    private long avisoAte = -1;
    private String aviso = "";

    ConfiguracaoLuta(RegistroCenarios cenarios, String idArenaInicial) {
        this.cenarios = cenarios;
        this.arenas = List.copyOf(cenarios.ids());
        for (String id : arenas) {
            nomesArenas.add(cenarios.exigir(id).getNome().toUpperCase(Locale.ROOT));
        }
        arena = Math.max(0, arenas.indexOf(idArenaInicial));
        regra = ModoLuta.PADRAO.ordinal();
    }

    /** Volta o foco para a ARENA ao (re)abrir a tela; as escolhas ficam. */
    void abrir() {
        foco = Area.ARENA;
        avisoAte = -1;
    }

    Resultado comandar(Comando c, long agoraMs) {
        switch (c) {
            case ANTERIOR -> foco = vizinha(-1);
            case PROXIMO -> foco = vizinha(1);
            case ESQUERDA -> mudar(-1);
            case DIREITA -> mudar(1);
            case VOLTAR -> {
                return Resultado.VOLTAR;
            }
            case CONFIRMAR -> {
                if (foco != Area.INICIAR) {
                    foco = vizinha(1);                         // confirmar numa area segue para a proxima
                    return Resultado.NADA;
                }
                return iniciar(agoraMs);
            }
            default -> {
            }
        }
        return Resultado.NADA;
    }

    /** INICIAR LUTA: so com um modo de regras que o motor suporta. */
    Resultado iniciar(long agoraMs) {
        if (!modoLuta().isDisponivel()) {
            aviso = modoLuta().getNome() + ": EM BREVE · escolha outra regra";
            avisoAte = agoraMs + 1400;
            foco = Area.REGRAS;
            return Resultado.NADA;
        }
        return Resultado.INICIAR;
    }

    private Area vizinha(int passo) {
        Area[] ordem = {Area.ARENA, Area.TEMPO, Area.REGRAS, Area.INICIAR};
        int i = java.util.Arrays.asList(ordem).indexOf(foco);
        return ordem[Math.max(0, Math.min(ordem.length - 1, i + passo))];
    }

    private void mudar(int passo) {
        switch (foco) {
            case ARENA -> arena = Math.floorMod(arena + passo, arenas.size());
            case TEMPO -> tempo = Math.floorMod(tempo + passo, SelecaoPartidaPanel.TEMPOS.length);
            case REGRAS -> regra = Math.floorMod(regra + passo, ModoLuta.values().length);
            default -> {
            }
        }
    }

    /** Um clique (testavel sem janela): setas das areas ou INICIAR LUTA. */
    Resultado clicar(int x, int y, long agoraMs) {
        if (BOTAO_INICIAR.contains(x, y)) {
            foco = Area.INICIAR;
            return iniciar(agoraMs);
        }
        if (PREVIA.contains(x, y) || (y >= 296 && y < 330 && x >= 40 && x < 376)) {
            foco = Area.ARENA;
            mudar(x < PREVIA.x + PREVIA.width / 2 ? -1 : 1);
        } else if (x >= 410 && x < 760 && y >= 96 && y < 150) {
            foco = Area.TEMPO;
            mudar(x < 585 ? -1 : 1);
        } else if (x >= 410 && x < 760 && y >= 230 && y < 300) {
            foco = Area.REGRAS;
            mudar(x < 585 ? -1 : 1);
        }
        return Resultado.NADA;
    }

    // ---- escolhas
    String idArena() {
        return arenas.get(arena);
    }

    int segundos() {
        return SelecaoPartidaPanel.TEMPOS[tempo];
    }

    ModoLuta modoLuta() {
        return ModoLuta.values()[regra];
    }

    Area foco() {
        return foco;
    }

    boolean temVariasArenas() {
        return arenas.size() > 1;
    }

    /** Previa da arena: o proprio cenario desenhado em 800 x 450 (uma vez por arena) e reduzido. */
    BufferedImage previa(String id) {
        return previas.computeIfAbsent(id, k -> {
            BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            Cenario c = cenarios.exigir(k);
            c.desenharFundo(g, 800, 450);
            c.desenharChao(g, 800, 450);
            c.desenharPrimeiroPlano(g, 800, 450);
            g.dispose();
            return img;
        });
    }

    // =====================================================
    // DESENHO (mesma identidade da selecao de lutadores)
    // =====================================================
    void desenhar(Graphics2D g, long agoraMs, String resumoJ1, String resumoJ2) {
        ArteMenus.letreiro(g, "CONFIGURAÇÕES DA LUTA", 400, 44, new Font("Arial", Font.BOLD, 26), ArteMenus.AMARELO);
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(ArteMenus.J1);
        g.drawString("J1 · " + resumoJ1, 40, 72);
        g.setColor(ArteMenus.J2);
        g.drawString("J2 · " + resumoJ2, 760 - g.getFontMetrics().stringWidth("J2 · " + resumoJ2), 72);

        // ARENA: a previa grande com o nome e as setas
        boolean fa = foco == Area.ARENA;
        painel(g, 28, 80, 360, 262, fa);
        rotulo(g, "ARENA", 40, 98, fa);
        Graphics2D gi = (Graphics2D) g.create();
        gi.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gi.drawImage(previa(idArena()), PREVIA.x, PREVIA.y, PREVIA.width, PREVIA.height, null);
        gi.dispose();
        g.setColor(fa ? ArteMenus.AMARELO : new Color(160, 200, 255, 160));
        g.setStroke(new BasicStroke(fa ? 3 : 1.5f));
        g.drawRect(PREVIA.x, PREVIA.y, PREVIA.width, PREVIA.height);
        valor(g, nomesArenas.get(arena), 208, 318, fa, temVariasArenas());
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        g.setColor(new Color(190, 205, 235));
        ArteMenus.centralizado(g, (arena + 1) + " / " + arenas.size(), 208, 334);

        // TEMPO, DIFICULDADE e REGRAS
        boolean ft = foco == Area.TEMPO;
        painel(g, 402, 80, 370, 62, ft);
        rotulo(g, "TEMPO", 416, 98, ft);
        valor(g, SelecaoPartidaPanel.textoTempo(segundos()), 587, 128, ft, true);

        painel(g, 402, 150, 370, 62, false);
        rotulo(g, "DIFICULDADE", 416, 168, false);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(new Color(140, 150, 175));
        ArteMenus.centralizado(g, "EM BREVE", 587, 198);

        boolean fr = foco == Area.REGRAS;
        ModoLuta m = modoLuta();
        painel(g, 402, 220, 370, 122, fr);
        rotulo(g, "REGRAS", 416, 238, fr);
        valor(g, m.getNome(), 587, 266, fr, true);
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(m.isDisponivel() ? new Color(120, 230, 140) : new Color(240, 170, 80));
        ArteMenus.centralizado(g, m.isDisponivel() ? "DISPONÍVEL" : "EM BREVE · não inicia a luta", 587, 288);
        g.setFont(ArteMenus.PEQUENO);
        g.setColor(ArteMenus.BRANCO);
        ArteMenus.centralizado(g, m.getDescricao(), 587, 308);
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        g.setColor(new Color(190, 205, 235));
        ArteMenus.centralizado(g, (regra + 1) + " / " + ModoLuta.values().length, 587, 330);

        // INICIAR LUTA
        boolean fi = foco == Area.INICIAR;
        boolean pode = m.isDisponivel();
        g.setColor(fi && pode ? new Color(255, 206, 46, 235) : fi ? new Color(150, 120, 60) : new Color(30, 70, 150));
        g.fillRoundRect(BOTAO_INICIAR.x, BOTAO_INICIAR.y, BOTAO_INICIAR.width, BOTAO_INICIAR.height, 12, 12);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(fi && pode ? ArteMenus.AZUL_ESCURO : pode ? new Color(170, 190, 230) : new Color(120, 130, 160));
        ArteMenus.centralizado(g, "INICIAR LUTA", 400, 390);

        g.setFont(ArteMenus.PEQUENO);
        if (avisoAte > agoraMs) {
            g.setColor(ArteMenus.AMARELO);
            ArteMenus.centralizado(g, aviso, 400, 420);
        } else {
            g.setColor(ArteMenus.BRANCO);
            ArteMenus.centralizado(g, "cima/baixo escolhe · lados mudam · confirme INICIAR LUTA", 400, 420);
        }
        g.setColor(new Color(190, 205, 235));
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        ArteMenus.centralizado(g, "J1 WASD·ESPAÇO·Q   J2 setas·ENTER·BACKSPACE   gamepad A/B   ESC volta aos lutadores",
                400, 437);
    }

    private static void painel(Graphics2D g, int x, int y, int w, int h, boolean foco) {
        g.setColor(ArteMenus.AZUL_PAINEL);
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(foco ? ArteMenus.AMARELO : new Color(160, 200, 255, 90));
        g.setStroke(new BasicStroke(foco ? 2.5f : 1));
        g.drawRoundRect(x, y, w, h, 14, 14);
    }

    private static void rotulo(Graphics2D g, String t, int x, int y, boolean foco) {
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(foco ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        g.drawString(t, x, y);
    }

    private static void valor(Graphics2D g, String t, int cx, int y, boolean foco, boolean setas) {
        g.setFont(new Font("Arial", Font.BOLD, 15));
        if (g.getFontMetrics().stringWidth(t) > 230) {
            g.setFont(new Font("Arial", Font.BOLD, 12));
        }
        g.setColor(foco ? ArteMenus.AMARELO : ArteMenus.BRANCO);
        ArteMenus.centralizado(g, t, cx, y);
        if (setas) {
            g.setColor(foco ? ArteMenus.AMARELO : new Color(160, 180, 220));
            int y0 = y - 5;
            g.fillPolygon(new int[] {cx - 150, cx - 140, cx - 140}, new int[] {y0, y0 - 6, y0 + 6}, 3);
            g.fillPolygon(new int[] {cx + 150, cx + 140, cx + 140}, new int[] {y0, y0 - 6, y0 + 6}, 3);
        }
    }
}
