package com.irmaosmiau.controles;

import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tecla fisica suportada pelos controles (Entrega 5C).
 *
 * nome   = token do KeyStroke do Swing ("A", "SPACE", "LEFT"), gravado em controles.json;
 * codigo = KeyEvent.VK_* (captura na tela de controles);
 * rotulo = texto mostrado ao jogador ("ESPAÇO", "←").
 *
 * A lista e fechada de proposito: so entram teclas que o Swing entrega de
 * forma confiavel em WHEN_IN_FOCUSED_WINDOW. TAB fica de fora (o Swing usa
 * para trocar o foco). ESC e F1..F12 existem, mas sao RESERVADAS ao sistema
 * (pausa, debug, cenario, reinicio, audio) e nunca podem ser atribuidas.
 */
public final class TeclaFisica {

    private static final Map<String, TeclaFisica> POR_NOME = new LinkedHashMap<>();
    private static final Map<Integer, TeclaFisica> POR_CODIGO = new LinkedHashMap<>();

    static {
        for (char c = 'A'; c <= 'Z'; c++) {
            registrar(String.valueOf(c), KeyEvent.VK_A + (c - 'A'), String.valueOf(c), false);
        }
        for (int d = 0; d <= 9; d++) {
            registrar(String.valueOf(d), KeyEvent.VK_0 + d, String.valueOf(d), false);
        }
        for (int d = 0; d <= 9; d++) {
            registrar("NUMPAD" + d, KeyEvent.VK_NUMPAD0 + d, "NUM " + d, false);
        }
        registrar("LEFT", KeyEvent.VK_LEFT, "←", false);
        registrar("RIGHT", KeyEvent.VK_RIGHT, "→", false);
        registrar("UP", KeyEvent.VK_UP, "↑", false);
        registrar("DOWN", KeyEvent.VK_DOWN, "↓", false);
        registrar("SPACE", KeyEvent.VK_SPACE, "ESPAÇO", false);
        registrar("SHIFT", KeyEvent.VK_SHIFT, "SHIFT", false);
        registrar("CONTROL", KeyEvent.VK_CONTROL, "CTRL", false);
        registrar("ALT", KeyEvent.VK_ALT, "ALT", false);
        registrar("ENTER", KeyEvent.VK_ENTER, "ENTER", false);
        registrar("BACK_SPACE", KeyEvent.VK_BACK_SPACE, "BACKSPACE", false);
        registrar("INSERT", KeyEvent.VK_INSERT, "INSERT", false);
        registrar("DELETE", KeyEvent.VK_DELETE, "DELETE", false);
        registrar("HOME", KeyEvent.VK_HOME, "HOME", false);
        registrar("END", KeyEvent.VK_END, "END", false);
        registrar("PAGE_UP", KeyEvent.VK_PAGE_UP, "PG UP", false);
        registrar("PAGE_DOWN", KeyEvent.VK_PAGE_DOWN, "PG DOWN", false);
        registrar("COMMA", KeyEvent.VK_COMMA, ",", false);
        registrar("PERIOD", KeyEvent.VK_PERIOD, ".", false);
        registrar("SLASH", KeyEvent.VK_SLASH, "/", false);
        registrar("SEMICOLON", KeyEvent.VK_SEMICOLON, ";", false);
        registrar("MINUS", KeyEvent.VK_MINUS, "-", false);
        registrar("EQUALS", KeyEvent.VK_EQUALS, "=", false);
        registrar("OPEN_BRACKET", KeyEvent.VK_OPEN_BRACKET, "[", false);
        registrar("CLOSE_BRACKET", KeyEvent.VK_CLOSE_BRACKET, "]", false);
        registrar("BACK_SLASH", KeyEvent.VK_BACK_SLASH, "\\", false);
        registrar("QUOTE", KeyEvent.VK_QUOTE, "'", false);
        registrar("ADD", KeyEvent.VK_ADD, "NUM +", false);
        registrar("SUBTRACT", KeyEvent.VK_SUBTRACT, "NUM -", false);

        // Reservadas ao sistema (ESC pausa; F3 debug; F4 cenario; F5 treino; F9/F10 audio).
        registrar("ESCAPE", KeyEvent.VK_ESCAPE, "ESC", true);
        for (int f = 1; f <= 12; f++) {
            registrar("F" + f, KeyEvent.VK_F1 + (f - 1), "F" + f, true);
        }
    }

    private final String nome;
    private final int codigo;
    private final String rotulo;
    private final boolean reservada;

    private TeclaFisica(String nome, int codigo, String rotulo, boolean reservada) {
        this.nome = nome;
        this.codigo = codigo;
        this.rotulo = rotulo;
        this.reservada = reservada;
    }

    private static void registrar(String nome, int codigo, String rotulo, boolean reservada) {
        TeclaFisica t = new TeclaFisica(nome, codigo, rotulo, reservada);
        POR_NOME.put(nome, t);
        POR_CODIGO.put(codigo, t);
    }

    /** Tecla pelo nome gravado (ex.: "SPACE"); vazio se nao for suportada. */
    public static Optional<TeclaFisica> porNome(String nome) {
        return Optional.ofNullable(nome == null ? null : POR_NOME.get(nome));
    }

    /** Tecla pelo KeyEvent.VK_*; vazio se nao for suportada. */
    public static Optional<TeclaFisica> porCodigo(int codigo) {
        return Optional.ofNullable(POR_CODIGO.get(codigo));
    }

    /** Tecla que precisa existir (padroes do codigo). */
    static TeclaFisica exigir(String nome) {
        return porNome(nome).orElseThrow(() -> new IllegalStateException("Tecla desconhecida: " + nome));
    }

    public static List<TeclaFisica> todas() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(POR_NOME.values()));
    }

    public String getNome() {
        return nome;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** ESC e F1..F12: usadas pelo sistema, nunca atribuiveis. */
    public boolean isReservada() {
        return reservada;
    }

    @Override
    public String toString() {
        return rotulo;
    }
    // Instancias unicas por nome: equals/hashCode por identidade bastam.
}
