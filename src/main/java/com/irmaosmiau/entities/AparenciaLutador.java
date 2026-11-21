package com.irmaosmiau.entities;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Aparencia personalizada de um lutador: uma cor por PARTE (null = a cor original do personagem).
 *
 * So aparencia: nada aqui muda graduacao, atributos, tecnicas ou regras. A cor da FAIXA e cosmetica (a
 * graduacao da luta continua a mesma). A pelagem muda o pelo, o rosto e as patas; os detalhes do rosto
 * (olhos, nariz, boca) ganham automaticamente a cor de contraste ({@link #contraste}).
 *
 * Pensada para o futuro Rig 2.0: as partes sao nomes logicos (casaco, calca, faixa, pelo), nao pecas do
 * desenho atual, e as cores sao guardadas como "#RRGGBB".
 */
public final class AparenciaLutador {

    /** As partes personalizaveis, na ordem do editor. */
    public enum Parte {
        CASACO("Kimono (parte de cima)"),
        CALCA("Calça do kimono"),
        FAIXA("Faixa (só visual)"),
        PELO("Pelagem (rosto e patas)");

        private final String rotulo;

        Parte(String rotulo) {
            this.rotulo = rotulo;
        }

        public String getRotulo() {
            return rotulo;
        }

        /** O rotulo (o seletor do editor mostra este texto). */
        @Override
        public String toString() {
            return rotulo;
        }

        /** Nome no arquivo do perfil. */
        public String chave() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Nenhuma parte personalizada: o personagem como e. */
    public static final AparenciaLutador ORIGINAL = new AparenciaLutador(new EnumMap<>(Parte.class));

    private final Map<Parte, Color> cores;

    private AparenciaLutador(Map<Parte, Color> cores) {
        this.cores = cores;
    }

    public Optional<Color> cor(Parte p) {
        return Optional.ofNullable(cores.get(p));
    }

    /** Uma copia com a cor desta parte (null volta a cor original). */
    public AparenciaLutador com(Parte p, Color c) {
        Map<Parte, Color> m = new EnumMap<>(Parte.class);
        m.putAll(cores);
        if (c == null) {
            m.remove(p);
        } else {
            m.put(p, new Color(c.getRGB() & 0xFFFFFF));
        }
        return new AparenciaLutador(m);
    }

    /** Sem casaco e calca (quando o kimono e escolhido explicitamente na selecao, ele vale nos dois). */
    public AparenciaLutador semKimono() {
        return com(Parte.CASACO, null).com(Parte.CALCA, null);
    }

    public boolean isOriginal() {
        return cores.isEmpty();
    }

    /** Detalhes do rosto legiveis: preto sobre pelagem clara, branco sobre pelagem escura (como os originais). */
    public static Color contraste(Color pelo) {
        double y = 0.299 * pelo.getRed() + 0.587 * pelo.getGreen() + 0.114 * pelo.getBlue();
        return y < 128 ? Color.WHITE : Color.BLACK;
    }

    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    /** "#RRGGBB" -> cor; IllegalArgumentException se invalido. */
    public static Color deHex(String s) {
        if (s == null || !s.trim().matches("#[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("Cor invalida: " + s + " (use #RRGGBB)");
        }
        return new Color(Integer.parseInt(s.trim().substring(1), 16));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AparenciaLutador a && a.cores.equals(cores);
    }

    @Override
    public int hashCode() {
        return cores.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Aparencia{");
        cores.forEach((p, c) -> b.append(p.chave()).append('=').append(hex(c)).append(' '));
        return b.append('}').toString();
    }
}
