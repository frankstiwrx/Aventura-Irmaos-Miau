package com.irmaosmiau.animacao;

import com.irmaosmiau.entities.IrmaoMiau;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Entrega 5O.1: assenta no tatame os movimentos de CHAO (rolamentos e sprawl)
 * para CADA personagem.
 *
 * Os deslocamentosY dos arquivos foram medidos no Miau Branco; o Miau Preto
 * (e um personagem futuro com o mesmo esqueleto) tem outras proporcoes, e o
 * corpo grupado poderia afundar ou flutuar alguns pixels. Aqui cada quadro e
 * desenhado UMA vez num lutador de rascunho da mesma classe, o ponto mais baixo
 * e comparado com o de quem esta em pe, e o resultado fica em cache por
 * (classe, movimento). Sem como criar o rascunho, valem os numeros do arquivo.
 * Nao mexe em regra, posicao logica nem tempo; e so desenho.
 */
public final class AssentamentoMovimentos {

    /** Amostras por intervalo entre quadros (o giro entre dois quadros tambem e assentado). */
    public static final int AMOSTRAS_POR_QUADRO = 4;

    private final Map<String, double[]> cache = new HashMap<>();

    /** Deslocamento vertical do movimento no progresso p para o lutador (do arquivo se nao der para medir). */
    public double dy(IrmaoMiau modelo, BibliotecaMovimentos.Movimento m, double p) {
        String chave = modelo.getClass().getName() + "|" + Math.round(modelo.getEscala() * 10) + "|" + m.getId();
        double[] t = cache.computeIfAbsent(chave, k -> medir(modelo, m));
        if (t == null) {
            return m.noProgresso(p, new int[30])[0];
        }
        double x = Math.max(0, Math.min(1, p)) * (t.length - 1);
        int i = Math.min(t.length - 2, (int) Math.floor(x));
        return t[i] + (t[i + 1] - t[i]) * (x - i);
    }

    private static double[] medir(IrmaoMiau modelo, BibliotecaMovimentos.Movimento m) {
        if (m.quantidade() < 2) {
            return null;
        }
        try {
            int escala10 = (int) Math.round(modelo.getEscala() * 10);
            int[] emPe = new int[30];
            emPe[2] = escala10;
            emPe[3] = 1;
            IrmaoMiau r = rascunho(modelo);
            int chao = base(r, emPe);
            int n = (m.quantidade() - 1) * AMOSTRAS_POR_QUADRO + 1;
            double[] t = new double[n];
            int[] q = new int[30];
            for (int i = 0; i < n; i++) {
                m.noProgresso(i / (double) (n - 1), q);
                q[2] = escala10;
                q[3] = 1;
                t[i] = chao - base(r, q);
            }
            return t;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private final Map<String, Integer> bases = new HashMap<>();

    /**
     * 5O.1: dy que encosta o ponto mais baixo de {@code pose} na mesma linha do
     * ponto mais baixo de {@code ref} desenhada com {@code refDy} (ex.: o rolamento
     * a partir de deitado fica no nivel do corpo deitado). null = nao deu para medir.
     */
    public Double alinharCom(IrmaoMiau modelo, int[] pose, int[] ref, double refDy) {
        try {
            String c = modelo.getClass().getName() + "|" + Math.round(modelo.getEscala() * 10) + "|";
            Integer yRef = bases.get(c + java.util.Arrays.toString(ref));
            if (yRef == null) {
                IrmaoMiau r = rascunho(modelo);
                yRef = base(r, ref, refDy);
                bases.put(c + java.util.Arrays.toString(ref), yRef);
            }
            String k = c + java.util.Arrays.toString(pose);
            Integer y = bases.get(k);
            if (y == null) {
                y = base(rascunho(modelo), pose, 0);
                if (bases.size() > 4000) {
                    bases.clear();
                }
                bases.put(k, y);
            }
            return (double) (yRef - y);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static IrmaoMiau rascunho(IrmaoMiau modelo) throws ReflectiveOperationException {
        return modelo.getClass().getConstructor(int.class, int.class, double.class)
                .newInstance(150, 40, modelo.getEscala());
    }

    /** Linha (y) do pixel visivel mais baixo do desenho. */
    private static int base(IrmaoMiau l, int[] pose) {
        return base(l, pose, 0);
    }

    private static int base(IrmaoMiau l, int[] pose, double dy) {
        BufferedImage img = new BufferedImage(420, 340, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        l.aplicarPoseCombateExterna(pose, 0, dy);
        l.desenhar(g);
        g.dispose();
        for (int y = img.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 40) {
                    return y;
                }
            }
        }
        throw new IllegalStateException("nada desenhado");
    }
}
