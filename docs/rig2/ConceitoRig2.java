import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * SUPERENTREGA 6.0.1 - Conceito visual do Rig 2.0 (Irmao Miau Branco).
 *
 * Gerador ISOLADO: nao usa nenhuma classe de src/, nao entra no build do Maven e nao altera o jogo.
 * Ideia central: um unico esqueleto 3D leve (x lateral, y para baixo, z para a frente do lutador)
 * com comprimentos de osso fixos, e uma "camera" que so gira em torno do eixo vertical (guinada).
 * Cada perspectiva e o MESMO corpo visto de outro angulo; o desenho continua sendo Java2D puro.
 *
 * Execucao (Java 23, sem compilar o projeto):
 *   java docs/rig2/ConceitoRig2.java docs/rig2/previas
 * Gera PNG (Java2D) e SVG (as mesmas formas, convertidas por PathIterator).
 */
public class ConceitoRig2 {

    // ===================================================================== medidas do rig
    /** Comprimentos fixos (unidades do laboratorio: cabeca = 70). Base x 145% / 145% / 120%. */
    static final double BRACO = 39, ANTEBRACO = 30, COXA = 46.5, CANELA = 46;
    static final double RAIO_CABECA = 35, CHAO = 270, PIVO_CINTURA = 158;
    /**
     * Centro da cabeca. 6.0.1: 36 (cabeca colada na gola). 6.0.2: 31 (pescoco aparente).
     * 6.0.3: 33,5, metade da exposicao de pescoco da 6.0.2. 6.0.4: 34,75, metade da exposicao da 6.0.3.
     */
    static final double CABECA_Y = 34.75, PESCOCO_Y = 76;

    /** Secoes elipticas do casaco: y neutro, meia-largura (a) e meia-profundidade (b). */
    static final double[] TY = {70, 80, 88, 100, 120, 140, 152, 164, 180, 196};
    static final double[] TA = {24, 36, 43, 42, 38, 35, 34, 35, 37, 39};
    static final double[] TB = {17, 21, 23, 24, 24, 23, 22, 23, 24, 25};

    // ===================================================================== cores
    static final Color FUNDO = new Color(30, 34, 40), CARTAO = new Color(44, 49, 57);
    static final Color TEXTO = new Color(239, 241, 245), SUAVE = new Color(166, 176, 190);
    static final Color GI = new Color(22, 23, 27), GI_LONGE = new Color(14, 15, 18);
    static final Color GI_LUZ = new Color(54, 56, 63), GI_SOMBRA = new Color(8, 8, 10);
    static final Color BORDA = new Color(112, 116, 124), BORDA_LONGE = new Color(74, 77, 84);
    static final Color GOLA = new Color(40, 42, 48), COSTURA = new Color(84, 88, 96);
    static final Color PELO = new Color(250, 250, 250), PELO_LONGE = new Color(214, 217, 223);
    static final Color PELO_SOMBRA = new Color(224, 227, 233), PELO_BORDA = new Color(170, 174, 182);
    static final Color ORELHA_INTERNA = new Color(234, 216, 222);
    static final Color OLHO = new Color(21, 21, 21), NARIZ = new Color(220, 120, 140), BOCA = new Color(34, 34, 34);
    static final Color FAIXA = new Color(26, 26, 28), FAIXA_LUZ = new Color(62, 62, 66), FAIXA_BORDA = new Color(98, 98, 104);
    static final Color PONTEIRA = new Color(200, 30, 30);
    static final Color OSSO = new Color(245, 165, 55), RAIZ = new Color(100, 215, 240), JUNTA = new Color(255, 208, 100);

    // ===================================================================== vetores
    record V(double x, double y, double z) {
        V mais(V o) { return new V(x + o.x, y + o.y, z + o.z); }
        V menos(V o) { return new V(x - o.x, y - o.y, z - o.z); }
        V vezes(double k) { return new V(x * k, y * k, z * k); }
        double ponto(V o) { return x * o.x + y * o.y + z * o.z; }
        double tam() { return Math.sqrt(ponto(this)); }
        V unit() { double t = tam(); return t < 1e-9 ? new V(0, 0, 0) : vezes(1 / t); }
    }

    static double interp(double[] t, double y) {
        if (y <= TY[0]) return t[0];
        for (int i = 1; i < TY.length; i++)
            if (y <= TY[i]) return t[i - 1] + (t[i] - t[i - 1]) * (y - TY[i - 1]) / (TY[i] - TY[i - 1]);
        return t[t.length - 1];
    }

    // ===================================================================== camera
    /** Guinada em torno do eixo vertical + leve inclinacao (o mais proximo aparece um pouco mais baixo). */
    static final class Camera {
        static final double INCLINACAO = 0.10;
        final double graus, c, s;
        Camera(double graus) { this.graus = graus; double r = Math.toRadians(graus); c = Math.cos(r); s = Math.sin(r); }
        double x(V v) { return v.x() * c + v.z() * s; }
        double prof(V v) { return -v.x() * s + v.z() * c; }
        Point2D p(V v) { return new Point2D.Double(x(v), v.y() + INCLINACAO * prof(v)); }
    }

    // ===================================================================== esqueleto
    /** Indice 0 = lado direito do lutador (-x), 1 = lado esquerdo (+x). */
    static final class Corpo {
        double inclinacao, baixar, cabecaZ = 2, giroCabeca;
        final V[] ombro = new V[2], cotovelo = new V[2], mao = new V[2];
        final V[] quadril = new V[2], joelho = new V[2], tornozelo = new V[2];
        final double[] guinadaPe = new double[2];
        V cabeca, pescoco, pelve;

        /** Coordenada neutra -> pose: abaixa o corpo e inclina o tronco em torno da cintura. */
        V transformar(V v) {
            V b = new V(v.x(), v.y() + baixar, v.z());
            if (v.y() >= PIVO_CINTURA || inclinacao == 0) return b;
            double r = Math.toRadians(inclinacao), py = PIVO_CINTURA + baixar, dy = b.y() - py;
            return new V(b.x(), py + dy * Math.cos(r) + b.z() * Math.sin(r), -dy * Math.sin(r) + b.z() * Math.cos(r));
        }

        V dir(V n, double yNeutro) {
            if (yNeutro >= PIVO_CINTURA || inclinacao == 0) return n;
            double r = Math.toRadians(inclinacao);
            return new V(n.x(), n.y() * Math.cos(r) + n.z() * Math.sin(r), -n.y() * Math.sin(r) + n.z() * Math.cos(r));
        }
    }

    /** IK de dois ossos em 3D: comprimentos nunca mudam; o polo escolhe o lado da dobra. */
    static V[] ik(V a, V alvo, double l1, double l2, V polo) {
        V d = alvo.menos(a);
        double dist = Math.max(Math.abs(l1 - l2) + .01, Math.min(l1 + l2 - .01, d.tam()));
        V u = d.unit();
        double ao = (dist * dist + l1 * l1 - l2 * l2) / (2 * dist);
        double h = Math.sqrt(Math.max(0, l1 * l1 - ao * ao));
        V p = polo.menos(u.vezes(polo.ponto(u))).unit();
        return new V[] {a.mais(u.vezes(ao)).mais(p.vezes(h)), a.mais(u.vezes(dist))};
    }

    // ===================================================================== poses parametrizadas (6.1)
    /** Limites dos controles experimentais (graus). */
    static final double ELEVACAO_MIN = -90, ELEVACAO_MAX = 170, AVANCO_MIN = -50, AVANCO_MAX = 150;
    static final double FLEXAO_MIN = 0, FLEXAO_MAX = 150, GIRO_CABECA_MAX = 60;

    /** Pose-base: alvos de IK no espaco do corpo (x lateral, y para baixo, z para a frente do lutador). */
    enum PoseBase {
        NEUTRA("Neutra", 0, 0, 2, new double[] {0, 0},
                new V[] {new V(-46, 152, 4), new V(46, 152, 4)},
                new V[] {new V(-1, .2, -.8), new V(1, .2, -.8)},
                new V[] {new V(-19, 258, -3), new V(19, 258, 3)},
                new V[] {new V(0, 0, 1), new V(0, 0, 1)}),
        /** Quadril baixo, tronco 8 graus a frente, guarda a frente do peito, pe esquerdo a frente. */
        COMBATE("Combate", 8, 16, -3, new double[] {-28, 8},
                new V[] {new V(-4, 118, 46), new V(22, 122, 64)},
                new V[] {new V(-.6, 1, -.4), new V(.5, 1, -.3)},
                new V[] {new V(-26, 258, -22), new V(22, 258, 26)},
                new V[] {new V(-.3, 0, 1), new V(.2, 0, 1)});

        final String nome;
        final double inclinacao, baixar, cabecaZ;
        final double[] guinadaPe;
        final V[] maos, polosBraco, pes, polosPerna;

        PoseBase(String nome, double inclinacao, double baixar, double cabecaZ, double[] guinadaPe,
                 V[] maos, V[] polosBraco, V[] pes, V[] polosPerna) {
            this.nome = nome; this.inclinacao = inclinacao; this.baixar = baixar; this.cabecaZ = cabecaZ;
            this.guinadaPe = guinadaPe; this.maos = maos; this.polosBraco = polosBraco; this.pes = pes; this.polosPerna = polosPerna;
        }

        @Override public String toString() { return nome; }
    }

    /** Presets experimentais das pernas (aplicados sobre a pose-base). */
    enum PresetPernas {
        DA_POSE("Da pose"), JOELHOS("Joelhos flexionados"), ESQUERDA_FRENTE("Perna esquerda à frente"),
        DIREITA_FRENTE("Perna direita à frente"), PES_ABERTOS("Pés abertos");
        final String nome;
        PresetPernas(String nome) { this.nome = nome; }
        @Override public String toString() { return nome; }
    }

    /**
     * Rotacao de um braco a partir de dois angulos absolutos em relacao ao tronco:
     * avanco (flexao do ombro, em torno do eixo lateral) e elevacao (abducao, em torno do eixo frontal).
     * Com os dois em zero o braco aponta para baixo.
     */
    static V girarBraco(V v, double elevacao, double avanco, double lado) {
        double e = Math.toRadians(-lado * elevacao), a = Math.toRadians(avanco);
        V r1 = new V(v.x(), v.y() * Math.cos(a) - v.z() * Math.sin(a), v.y() * Math.sin(a) + v.z() * Math.cos(a));
        return new V(r1.x() * Math.cos(e) - r1.y() * Math.sin(e), r1.x() * Math.sin(e) + r1.y() * Math.cos(e), r1.z());
    }

    static V desgirarBraco(V v, double elevacao, double avanco, double lado) {
        double e = Math.toRadians(lado * elevacao), a = Math.toRadians(-avanco);
        V r1 = new V(v.x() * Math.cos(e) - v.y() * Math.sin(e), v.x() * Math.sin(e) + v.y() * Math.cos(e), v.z());
        return new V(r1.x(), r1.y() * Math.cos(a) - r1.z() * Math.sin(a), r1.y() * Math.sin(a) + r1.z() * Math.cos(a));
    }

    static double alcance(double flexao) {
        return Math.sqrt(BRACO * BRACO + ANTEBRACO * ANTEBRACO + 2 * BRACO * ANTEBRACO * Math.cos(Math.toRadians(flexao)));
    }

    static double limitar(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

    /**
     * Parametros de pose do laboratorio. Os bracos sao descritos por angulos (elevacao, avanco,
     * flexao do cotovelo) relativos ao tronco; a vista (guinada da camera) nao faz parte da pose,
     * por isso girar o personagem nunca altera os membros.
     */
    static final class ParametrosPose {
        PoseBase base;
        PresetPernas pernas = PresetPernas.DA_POSE;
        double giroCabeca;
        final double[] elevacao = new double[2], avanco = new double[2], flexao = new double[2];
        final V[] poloLocal = new V[2];

        /** Valores que reproduzem exatamente a pose-base. */
        static ParametrosPose padrao(PoseBase b) {
            ParametrosPose p = new ParametrosPose();
            p.base = b;
            p.restaurarBracos();
            return p;
        }

        void restaurarBracos() {
            Corpo c = new Corpo();
            c.inclinacao = base.inclinacao;
            c.baixar = base.baixar;
            for (int i = 0; i < 2; i++) {
                double lado = i == 0 ? -1 : 1;
                V ombro = c.transformar(new V(lado * 34, 90, 0));
                V v = base.maos[i].menos(ombro);
                double d = limitar(v.tam(), Math.abs(BRACO - ANTEBRACO) + .01, BRACO + ANTEBRACO - .01);
                V u = v.unit();
                avanco[i] = Math.toDegrees(Math.asin(limitar(u.z(), -1, 1)));
                elevacao[i] = -lado * Math.toDegrees(Math.atan2(-u.x(), u.y()));
                double cosF = (d * d - BRACO * BRACO - ANTEBRACO * ANTEBRACO) / (2 * BRACO * ANTEBRACO);
                flexao[i] = Math.toDegrees(Math.acos(limitar(cosF, -1, 1)));
                poloLocal[i] = desgirarBraco(base.polosBraco[i], elevacao[i], avanco[i], lado);
            }
        }

        void limitarTudo() {
            giroCabeca = limitar(giroCabeca, -GIRO_CABECA_MAX, GIRO_CABECA_MAX);
            for (int i = 0; i < 2; i++) {
                elevacao[i] = limitar(elevacao[i], ELEVACAO_MIN, ELEVACAO_MAX);
                avanco[i] = limitar(avanco[i], AVANCO_MIN, AVANCO_MAX);
                flexao[i] = limitar(flexao[i], FLEXAO_MIN, FLEXAO_MAX);
            }
        }

        ParametrosPose copia() {
            ParametrosPose p = new ParametrosPose();
            p.base = base; p.pernas = pernas; p.giroCabeca = giroCabeca;
            for (int i = 0; i < 2; i++) {
                p.elevacao[i] = elevacao[i]; p.avanco[i] = avanco[i]; p.flexao[i] = flexao[i]; p.poloLocal[i] = poloLocal[i];
            }
            return p;
        }
    }

    /** Monta o esqueleto de uma pose. A camera nao participa: a mesma pose serve para qualquer vista. */
    static Corpo montarPose(ParametrosPose pp) {
        pp.limitarTudo();
        PoseBase b = pp.base;
        Corpo c = new Corpo();
        c.inclinacao = b.inclinacao;
        c.baixar = b.baixar;
        c.cabecaZ = b.cabecaZ;
        c.giroCabeca = pp.giroCabeca;
        V[] pes = b.pes.clone(), polosP = b.polosPerna.clone();
        c.guinadaPe[0] = b.guinadaPe[0];
        c.guinadaPe[1] = b.guinadaPe[1];
        switch (pp.pernas) {
            case JOELHOS -> c.baixar += 14;
            case ESQUERDA_FRENTE -> {
                pes = new V[] {new V(-20, 258, -22), new V(18, 258, 26)};
                polosP = new V[] {new V(-.2, 0, 1), new V(.2, 0, 1)};
                c.guinadaPe[0] = -24;
                c.guinadaPe[1] = 6;
            }
            case DIREITA_FRENTE -> {
                pes = new V[] {new V(-18, 258, 26), new V(20, 258, -22)};
                polosP = new V[] {new V(-.2, 0, 1), new V(.2, 0, 1)};
                c.guinadaPe[0] = -6;
                c.guinadaPe[1] = 24;
            }
            case PES_ABERTOS -> {
                for (int i = 0; i < 2; i++) {
                    double lado = i == 0 ? -1 : 1;
                    pes[i] = pes[i].mais(new V(lado * 12, 0, 0));
                    c.guinadaPe[i] += lado * 20;
                }
            }
            default -> { }
        }
        c.pelve = c.transformar(new V(0, 165, 0));
        c.pescoco = c.transformar(new V(0, PESCOCO_Y, 0));
        c.cabeca = c.transformar(new V(0, CABECA_Y, c.cabecaZ));
        for (int i = 0; i < 2; i++) {
            double lado = i == 0 ? -1 : 1;
            c.ombro[i] = c.transformar(new V(lado * 34, 90, 0));
            V dirB = girarBraco(new V(0, 1, 0), pp.elevacao[i], pp.avanco[i], lado);
            V alvo = c.ombro[i].mais(dirB.vezes(alcance(pp.flexao[i])));
            V[] br = ik(c.ombro[i], alvo, BRACO, ANTEBRACO, girarBraco(pp.poloLocal[i], pp.elevacao[i], pp.avanco[i], lado));
            c.cotovelo[i] = br[0];
            c.mao[i] = br[1];
            c.quadril[i] = c.transformar(new V(lado * 16, 170, 0));
            V[] pe = ik(c.quadril[i], pes[i], COXA, CANELA, polosP[i]);
            c.joelho[i] = pe[0];
            c.tornozelo[i] = pe[1];
        }
        return c;
    }

    static Corpo neutro() { return montarPose(ParametrosPose.padrao(PoseBase.NEUTRA)); }

    static Corpo combate() { return montarPose(ParametrosPose.padrao(PoseBase.COMBATE)); }


    // ===================================================================== pintor duplo (Java2D + SVG)
    static final class Pintor {
        final Graphics2D g;
        final int w, h;
        final StringBuilder svg = new StringBuilder();
        AffineTransform t = new AffineTransform();
        double alfa = 1;
        final Deque<AffineTransform> pilha = new ArrayDeque<>();

        Pintor(Graphics2D g, int w, int h) {
            this.g = g; this.w = w; this.h = h;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        void salvar() { pilha.push(new AffineTransform(t)); }
        void restaurar() { t = pilha.pop(); }
        void mover(double x, double y) { t.translate(x, y); }
        void escalar(double k) { t.scale(k, k); }
        void escalarXY(double kx, double ky) { t.scale(kx, ky); }
        void girar(double rad) { t.rotate(rad); }
        double esc() { return Math.sqrt(Math.abs(t.getDeterminant())); }

        Color comAlfa(Color c) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(c.getAlpha() * alfa)); }

        void preencher(Shape s, Color c) {
            Shape d = t.createTransformedShape(s);
            g.setColor(comAlfa(c));
            g.fill(d);
            svg.append("<path d=\"").append(caminho(d)).append("\" fill=\"").append(hex(c)).append('"');
            opacidade("fill-opacity", c);
            if (d.getPathIterator(null).getWindingRule() == PathIterator.WIND_EVEN_ODD) svg.append(" fill-rule=\"evenodd\"");
            svg.append("/>\n");
        }

        void tracar(Shape s, Color c, double largura) {
            Shape d = t.createTransformedShape(s);
            double lw = largura * esc();
            g.setStroke(new BasicStroke((float) lw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(comAlfa(c));
            g.draw(d);
            svg.append("<path d=\"").append(caminho(d)).append("\" fill=\"none\" stroke=\"").append(hex(c))
                    .append("\" stroke-width=\"").append(n(lw)).append("\" stroke-linecap=\"round\" stroke-linejoin=\"round\"");
            opacidade("stroke-opacity", c);
            svg.append("/>\n");
        }

        void forma(Shape s, Color fill, Color borda, double largura) { preencher(s, fill); tracar(s, borda, largura); }

        void linha(Color c, double largura, double... xy) {
            Path2D p = new Path2D.Double();
            p.moveTo(xy[0], xy[1]);
            for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
            tracar(p, c, largura);
        }

        /** alinhar: -1 esquerda, 0 centro, 1 direita. Tamanho em unidades do sistema atual. */
        void texto(String s, double x, double y, double tamanho, Color c, boolean negrito, int alinhar) {
            Point2D p = t.transform(new Point2D.Double(x, y), null);
            float px = (float) (tamanho * esc());
            Font f = new Font(Font.SANS_SERIF, negrito ? Font.BOLD : Font.PLAIN, 1).deriveFont(px);
            int largura = g.getFontMetrics(f).stringWidth(s);
            double x0 = alinhar < 0 ? p.getX() : alinhar == 0 ? p.getX() - largura / 2.0 : p.getX() - largura;
            g.setFont(f);
            g.setColor(comAlfa(c));
            g.drawString(s, (float) x0, (float) p.getY());
            String ancora = alinhar < 0 ? "start" : alinhar == 0 ? "middle" : "end";
            svg.append("<text x=\"").append(n(p.getX())).append("\" y=\"").append(n(p.getY()))
                    .append("\" font-family=\"Segoe UI, Arial, sans-serif\" font-size=\"").append(n(px))
                    .append("\" font-weight=\"").append(negrito ? "700" : "400").append("\" fill=\"").append(hex(c))
                    .append("\" text-anchor=\"").append(ancora).append('"');
            opacidade("fill-opacity", c);
            svg.append('>').append(escapar(s)).append("</text>\n");
        }

        void opacidade(String atributo, Color c) {
            double a = c.getAlpha() / 255.0 * alfa;
            if (a < .999) svg.append(' ').append(atributo).append("=\"").append(n(a)).append('"');
        }

        String documento() {
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + w + "\" height=\"" + h + "\" viewBox=\"0 0 "
                    + w + " " + h + "\">\n" + svg + "</svg>\n";
        }

        static String caminho(Shape s) {
            StringBuilder b = new StringBuilder();
            double[] c = new double[6];
            for (PathIterator it = s.getPathIterator(null); !it.isDone(); it.next()) {
                switch (it.currentSegment(c)) {
                    case PathIterator.SEG_MOVETO -> b.append('M').append(n(c[0])).append(' ').append(n(c[1]));
                    case PathIterator.SEG_LINETO -> b.append('L').append(n(c[0])).append(' ').append(n(c[1]));
                    case PathIterator.SEG_QUADTO -> b.append('Q').append(n(c[0])).append(' ').append(n(c[1])).append(' ')
                            .append(n(c[2])).append(' ').append(n(c[3]));
                    case PathIterator.SEG_CUBICTO -> b.append('C').append(n(c[0])).append(' ').append(n(c[1])).append(' ')
                            .append(n(c[2])).append(' ').append(n(c[3])).append(' ').append(n(c[4])).append(' ').append(n(c[5]));
                    case PathIterator.SEG_CLOSE -> b.append('Z');
                    default -> { }
                }
            }
            return b.toString();
        }

        static String n(double v) {
            String s = String.format(Locale.ROOT, "%.1f", v);
            return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
        }

        static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }

        static String escapar(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    }

    // ===================================================================== geometria 2D
    static Point2D pt(double x, double y) { return new Point2D.Double(x, y); }
    static Ellipse2D circ(Point2D c, double r) { return new Ellipse2D.Double(c.getX() - r, c.getY() - r, 2 * r, 2 * r); }

    static Shape trapezio(Point2D a, Point2D b, double wa, double wb) {
        double dx = b.getX() - a.getX(), dy = b.getY() - a.getY(), l = Math.hypot(dx, dy);
        if (l < 1e-6) return circ(a, wa / 2);
        double nx = -dy / l, ny = dx / l;
        Path2D p = new Path2D.Double();
        p.moveTo(a.getX() + nx * wa / 2, a.getY() + ny * wa / 2);
        p.lineTo(b.getX() + nx * wb / 2, b.getY() + ny * wb / 2);
        p.lineTo(b.getX() - nx * wb / 2, b.getY() - ny * wb / 2);
        p.lineTo(a.getX() - nx * wa / 2, a.getY() - ny * wa / 2);
        p.closePath();
        return p;
    }

    /** Membro continuo: dois troncos de cone + junta redonda, unidos em UMA silhueta (sem blocos). */
    static Area tubo(Point2D a, Point2D b, Point2D c, double w0, double wj, double w1, double capa) {
        Area r = new Area(trapezio(a, b, w0, wj));
        r.add(new Area(trapezio(b, c, wj, w1)));
        r.add(new Area(circ(b, wj / 2)));
        if (capa > 0) r.add(new Area(circ(a, capa)));
        return r;
    }

    static Path2D poligono(List<Point2D> pts) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < pts.size(); i++) {
            if (i == 0) p.moveTo(pts.get(i).getX(), pts.get(i).getY());
            else p.lineTo(pts.get(i).getX(), pts.get(i).getY());
        }
        p.closePath();
        return p;
    }

    static Path2D polilinha(List<Point2D> pts) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < pts.size(); i++) {
            if (i == 0) p.moveTo(pts.get(i).getX(), pts.get(i).getY());
            else p.lineTo(pts.get(i).getX(), pts.get(i).getY());
        }
        return p;
    }

    static Point2D lerp(Point2D a, Point2D b, double k) {
        return pt(a.getX() + (b.getX() - a.getX()) * k, a.getY() + (b.getY() - a.getY()) * k);
    }

    // ===================================================================== desenho do personagem
    static final class Desenho {
        final Pintor p;
        final Corpo c;
        final Camera cam;

        Desenho(Pintor p, Corpo c, double guinada) { this.p = p; this.c = c; this.cam = new Camera(guinada); }

        // ---------- superficie do casaco
        V sup(double y, double phi, double off) {
            double f = Math.toRadians(phi);
            return c.transformar(new V((interp(TA, y) + off) * Math.sin(f), y, (interp(TB, y) + off) * Math.cos(f)));
        }
        Point2D sp(double y, double phi, double off) { return cam.p(sup(y, phi, off)); }
        double vis(double y, double phi) {
            double f = Math.toRadians(phi);
            V n = new V(Math.sin(f) / interp(TA, y), 0, Math.cos(f) / interp(TB, y)).unit();
            return cam.prof(c.dir(n, y));
        }
        List<Point2D> arco(double y, double off) {
            List<Point2D> r = new ArrayList<>();
            for (double f = -180; f < 180; f += 3) if (vis(y, f) >= 0) r.add(sp(y, f, off));
            r.sort(Comparator.comparingDouble(Point2D::getX));
            return r;
        }

        /** Lado perto da camera: 0 (direito) ou 1 (esquerdo); -1 nas vistas de frente/costas puras. */
        int ladoPerto() { return cam.s > .05 ? 0 : cam.s < -.05 ? 1 : -1; }

        // ---------- figura completa (a ordem das camadas e o contrato)
        void figura() {
            sombraChao();
            int perto = ladoPerto(), longe = perto < 0 ? -1 : 1 - perto;
            if (longe >= 0) braco(longe, fatorLonge());
            // 6.1: um braco "da frente" cuja pata esta do outro lado do peito (vistas de costas com a guarda
            // a frente) passa para tras do tronco, com transicao suave pela profundidade da pata.
            double[] frente = {frenteBraco(0), frenteBraco(1)};
            for (int i = 0; i < 2; i++) if (i != longe && frente[i] < 1) braco(i, 0);
            pernas(perto);
            casaco();
            faixa();
            cabeca(c.cabeca, guinadaRosto());
            if (perto >= 0) {
                // 6.1: perto das vistas de frente/costas puras o braco longe ainda esta ao lado do peito;
                // ele passa para tras do tronco gradualmente (copia na frente que desaparece entre ~3 e ~15 graus)
                double fl = fatorLonge();
                if (fl < 1) comOpacidade((1 - fl) * frente[longe], () -> braco(longe, fl));
                comOpacidade(frente[perto], () -> braco(perto, 0));
            } else for (int i = 0; i < 2; i++) { int b = i; comOpacidade(frente[b], () -> braco(b, 0)); }
        }

        /**
         * 1 = pata a frente do tronco ou fora da silhueta dele; 0 = pata escondida atras do tronco.
         * Compara a profundidade da pata com a da superficie visivel do casaco no mesmo ponto da tela;
         * as duas medidas sao continuas, entao a troca de camada e gradual.
         */
        double frenteBraco(int i) {
            V m = c.mao[i];
            double hx = cam.x(m), prof = cam.prof(m), y = m.y() - c.baixar;
            if (y < TY[0] || y > 196) return 1;
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, profSup = Double.NaN, melhor = Double.MAX_VALUE;
            for (double f = 0; f < 360; f += 3) {
                V s = sup(y, f, 0);
                double sx = cam.x(s);
                min = Math.min(min, sx);
                max = Math.max(max, sx);
                if (vis(y, f) >= 0 && Math.abs(sx - hx) < melhor) { melhor = Math.abs(sx - hx); profSup = cam.prof(s); }
            }
            double fora = Math.max(min - hx, hx - max);
            return Math.max(suave(-2, 6, fora), Double.isNaN(profSup) ? 1 : suave(-8, 4, prof - profSup));
        }

        /** 6.1: guinada final do rosto = vista + giro proprio da cabeca, com a trapaca de camera. */
        double guinadaRosto() { return guinadaCabeca(cam.graus + c.giroCabeca); }

        /**
         * 6.1: quanto o lado longe escurece (0 nas vistas de frente/costas puras, 1 a partir de ~15 graus).
         * Antes era 0 ou 1; a mistura evita um "estalo" de cor ao sair da vista frontal.
         */
        double fatorLonge() { return suave(.05, .25, Math.abs(cam.s)); }

        /** Interpolacao suave classica (0 abaixo de a, 1 acima de b). */
        static double suave(double a, double b, double x) {
            double t = limitar((x - a) / (b - a), 0, 1);
            return t * t * (3 - 2 * t);
        }

        static Color misturar(Color a, Color b, double t) {
            if (t <= 0) return a;
            if (t >= 1) return b;
            return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                    (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                    (int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
        }

        /** Desenha com opacidade reduzida (para elementos que aparecem/somem conforme a vista). */
        void comOpacidade(double f, Runnable r) {
            if (f <= 0.001) return;
            double antes = p.alfa;
            p.alfa = antes * Math.min(1, f);
            r.run();
            p.alfa = antes;
        }

        /**
         * "Trapaca" de camera classica da animacao 2D: entre a frente e o perfil o rosto gira menos que
         * o corpo, para manter os dois olhos legiveis no 3/4 de combate (corpo 40 -> rosto 30).
         * 6.1: simetrica para guinadas negativas (cabeca virada para o outro lado).
         */
        static double guinadaCabeca(double g) {
            // 6.0.2: curva suave (sem salto em 90 graus); continua dando exatamente 30 para o corpo a 40.
            return g > -90 && g < 90 ? g - 10 / Math.sin(Math.toRadians(80)) * Math.sin(Math.toRadians(2 * g)) : g;
        }

        void sombraChao() {
            double x0 = Math.min(cam.p(c.tornozelo[0]).getX(), cam.p(c.tornozelo[1]).getX());
            double x1 = Math.max(cam.p(c.tornozelo[0]).getX(), cam.p(c.tornozelo[1]).getX());
            double meio = (x0 + x1) / 2, larg = Math.max(70, x1 - x0 + 46);
            p.preencher(new Ellipse2D.Double(meio - larg / 2, CHAO - 4, larg, 10), new Color(12, 14, 18, 120));
        }

        // ---------- roupa: luz de cima/esquerda, igual em todas as vistas
        void sombrear(Area a, Color base, Color borda, double w) { sombrear(a, base == GI_LONGE ? 1 : 0, base, borda, w); }

        void sombrear(Area a, double longe, Color base, Color borda, double w) {
            p.preencher(a, base);
            Area luz = new Area(a);
            luz.subtract(a.createTransformedArea(AffineTransform.getTranslateInstance(2.2, 2.6)));
            p.preencher(luz, misturar(GI_LUZ, new Color(34, 36, 41), longe));
            Area sombra = new Area(a);
            sombra.subtract(a.createTransformedArea(AffineTransform.getTranslateInstance(-2.6, -3.2)));
            p.preencher(sombra, GI_SOMBRA);
            p.tracar(a, borda, w);
        }

        /** Dobra simplificada no lado interno de uma articulacao. */
        void dobra(Point2D a, Point2D j, Point2D b, double largura, Color cor) {
            double ax = a.getX() - j.getX(), ay = a.getY() - j.getY(), bx = b.getX() - j.getX(), by = b.getY() - j.getY();
            double la = Math.hypot(ax, ay), lb = Math.hypot(bx, by);
            if (la < 1 || lb < 1) return;
            double mx = ax / la + bx / lb, my = ay / la + by / lb, lm = Math.hypot(mx, my);
            // 6.1: o vinco aparece gradualmente conforme a dobra aumenta (antes surgia de uma vez)
            double f = suave(.42, .5, lm); // totalmente visivel a partir de .5, como na 6.0.4
            if (f <= 0) return;
            mx /= lm; my /= lm;
            // um unico vinco curto, perpendicular a bissetriz, no lado interno da dobra
            double ox = j.getX() + mx * largura * .28, oy = j.getY() + my * largura * .28, px = -my, py = mx;
            Path2D v = new Path2D.Double();
            v.moveTo(ox - px * 5, oy - py * 5);
            v.quadTo(ox - mx * 2.5, oy - my * 2.5, ox + px * 5, oy + py * 5);
            comOpacidade(f, () -> p.tracar(v, cor, .9));
        }

        /** longe: 0 = lado perto (ou vista frontal), 1 = totalmente do lado longe. */
        void braco(int i, double longe) {
            Point2D s = cam.p(c.ombro[i]), e = cam.p(c.cotovelo[i]), w = cam.p(c.mao[i]);
            Area manga = tubo(s, e, w, 27, 24, 24, 13);
            Color borda = misturar(BORDA, BORDA_LONGE, longe);
            sombrear(manga, longe, misturar(GI, GI_LONGE, longe), borda, 1.2);
            dobra(s, e, w, 27, borda);
            // punho da manga (barra do kimono)
            double dx = w.getX() - e.getX(), dy = w.getY() - e.getY(), l = Math.max(.01, Math.hypot(dx, dy));
            double ux = dx / l, uy = dy / l, bx = w.getX() - ux * 6, by = w.getY() - uy * 6;
            p.linha(borda, 1, bx - uy * 12, by + ux * 12, bx + uy * 12, by - ux * 12);
            pata(w, Math.atan2(dy, dx), longe);
        }

        void pata(Point2D punho, double ang, double longe) {
            p.salvar();
            p.mover(punho.getX(), punho.getY());
            p.girar(ang);
            p.mover(6, 0);
            p.escalar(1.25);
            Path2D m = new Path2D.Double();
            m.moveTo(-7, -6); m.quadTo(-9, -10, -3, -10); m.lineTo(5, -8); m.quadTo(11, -7, 11, 0);
            m.quadTo(11, 8, 4, 9); m.lineTo(-4, 8); m.quadTo(-10, 6, -8, 1); m.quadTo(-14, -1, -10, -5); m.closePath();
            p.forma(m, misturar(PELO, PELO_LONGE, longe), PELO_BORDA, .9);
            p.linha(PELO_BORDA, .75, 6, -5, 9, -5);
            p.linha(PELO_BORDA, .75, 7, 0, 10, 0);
            p.linha(PELO_BORDA, .75, 6, 5, 9, 5);
            p.restaurar();
        }

        void pe(int i, double longe) {
            double a = Math.toRadians(c.guinadaPe[i]);
            V d = new V(Math.sin(a), 0, Math.cos(a));
            V t = c.tornozelo[i];
            Point2D centro = cam.p(t.mais(new V(0, 7, 0)).mais(d.vezes(6)));
            double rel = Math.toRadians(cam.graus) + a;
            double rx = Math.hypot(10.5 * Math.cos(rel), 15 * Math.sin(rel)), ry = 7;
            p.forma(new Ellipse2D.Double(centro.getX() - rx, centro.getY() - ry, 2 * rx, 2 * ry),
                    misturar(PELO, PELO_LONGE, longe), PELO_BORDA, .9);
            // 6.1: os dedos somem gradualmente quando o pe vira de costas (antes sumiam de uma vez em -0,3)
            double f = suave(-.3, -.2, Math.cos(rel));
            V lado = new V(Math.cos(a), 0, -Math.sin(a));
            // lado do dedo maior: troca de lado em rel = 0/180, onde o deslocamento e so de profundidade (~0,3 px)
            double sinal = Math.signum(Math.sin(rel) + 1e-9);
            comOpacidade(f, () -> {
                for (int k = -1; k <= 1; k += 2) {
                    Point2D q = cam.p(t.mais(new V(0, 7, 0)).mais(d.vezes(17 + 3 * k * sinal)).mais(lado.vezes(4 * k)));
                    p.linha(PELO_BORDA, .8, q.getX(), q.getY() + 1, q.getX(), q.getY() + 5);
                }
            });
        }

        Area perna(int i) {
            Point2D h = cam.p(c.quadril[i]), k = cam.p(c.joelho[i]), t = cam.p(c.tornozelo[i]);
            double dx = t.getX() - k.getX(), dy = t.getY() - k.getY(), l = Math.max(.01, Math.hypot(dx, dy));
            Point2D barra = pt(t.getX() + dx / l * 4, t.getY() + dy / l * 4);
            return tubo(h, k, barra, 32, 28, 27, 0);
        }

        Area quadris() {
            List<Point2D> q = new ArrayList<>();
            for (V v : new V[] {new V(-26, 164, 0), new V(26, 164, 0), new V(22, 196, 0), new V(0, 205, 0), new V(-22, 196, 0)})
                q.add(cam.p(c.transformar(v)));
            return new Area(poligono(q));
        }

        void pernas(int perto) {
            if (perto < 0) {
                pe(0, 0); pe(1, 0);
                Area a = perna(0); a.add(perna(1)); a.add(quadris());
                sombrear(a, GI, BORDA, 1.2);
                for (int i = 0; i < 2; i++) dobraJoelho(i, BORDA);
                return;
            }
            int longe = 1 - perto;
            double f = fatorLonge();
            Color borda = misturar(BORDA, BORDA_LONGE, f);
            pe(longe, f);
            sombrear(perna(longe), f, misturar(GI, GI_LONGE, f), borda, 1.2);
            dobraJoelho(longe, borda);
            pe(perto, 0);
            Area a = perna(perto); a.add(quadris());
            sombrear(a, GI, BORDA, 1.2);
            dobraJoelho(perto, BORDA);
        }

        void dobraJoelho(int i, Color cor) {
            Point2D h = cam.p(c.quadril[i]), k = cam.p(c.joelho[i]), t = cam.p(c.tornozelo[i]);
            dobra(h, k, t, 30, cor);
            double dx = t.getX() - k.getX(), dy = t.getY() - k.getY(), l = Math.max(.01, Math.hypot(dx, dy));
            double ux = dx / l, uy = dy / l, bx = t.getX() - ux * 1, by = t.getY() - uy * 1;
            p.linha(cor, .9, bx - uy * 13, by + ux * 13, bx + uy * 13, by - ux * 13);
        }

        // ---------- casaco
        Area tronco() {
            List<Point2D> esq = new ArrayList<>(), dir = new ArrayList<>();
            for (double y = TY[0]; y <= 196.01; y += 2) {
                Point2D mn = null, mx = null;
                for (double f = 0; f < 360; f += 3) {
                    Point2D q = sp(y, f, 0);
                    if (mn == null || q.getX() < mn.getX()) mn = q;
                    if (mx == null || q.getX() > mx.getX()) mx = q;
                }
                esq.add(mn);
                dir.add(mx);
            }
            List<Point2D> contorno = new ArrayList<>(esq);
            contorno.addAll(arco(196, 0));
            List<Point2D> volta = new ArrayList<>(dir);
            Collections.reverse(volta);
            contorno.addAll(volta);
            return new Area(poligono(contorno));
        }

        /** Polilinha na superficie, desenhada so onde a superficie encara a camera. */
        void linhaSup(double[][] yphi, double off, Color cor, double w) {
            List<Point2D> run = new ArrayList<>();
            List<double[]> amostras = amostrar(yphi);
            for (double[] a : amostras) {
                if (vis(a[0], a[1]) > .02) run.add(sp(a[0], a[1], off));
                else { if (run.size() > 1) p.tracar(polilinha(run), cor, w); run.clear(); }
            }
            if (run.size() > 1) p.tracar(polilinha(run), cor, w);
        }

        static List<double[]> amostrar(double[][] yphi) {
            List<double[]> r = new ArrayList<>();
            for (int i = 0; i + 1 < yphi.length; i++) {
                double[] a = yphi[i], b = yphi[i + 1];
                int n = (int) Math.max(2, Math.ceil(Math.max(Math.abs(b[0] - a[0]) / 1.5, Math.abs(b[1] - a[1]) / 3)));
                for (int k = 0; k < n; k++) r.add(new double[] {a[0] + (b[0] - a[0]) * k / n, a[1] + (b[1] - a[1]) * k / n});
            }
            r.add(yphi[yphi.length - 1]);
            return r;
        }

        /** Faixa de tecido que segue a superficie (gola/lapela). Largura em unidades do corpo. */
        void lapela(double[][] yphi, double largura, Color fill, Color borda) {
            List<double[]> amostras = amostrar(yphi);
            List<Point2D> esq = new ArrayList<>(), dir = new ArrayList<>(), meio = new ArrayList<>();
            Runnable fechar = () -> {
                if (esq.size() > 1) {
                    List<Point2D> poly = new ArrayList<>(esq);
                    List<Point2D> volta = new ArrayList<>(dir);
                    Collections.reverse(volta);
                    poly.addAll(volta);
                    p.forma(poligono(poly), fill, borda, 1);
                    p.tracar(polilinha(meio), COSTURA, .7);
                }
                esq.clear(); dir.clear(); meio.clear();
            };
            for (double[] a : amostras) {
                double y = a[0], f = a[1], hw = Math.toDegrees(largura / 2 / interp(TA, y));
                if (vis(y, f) > .02) {
                    esq.add(sp(y, f - hw, 1.4));
                    dir.add(sp(y, f + hw, 1.4));
                    meio.add(sp(y, f, 1.6));
                } else fechar.run();
            }
            fechar.run();
        }

        /** Gola de tras: faixa horizontal em torno do pescoco, continua com as lapelas. */
        void golaTras() {
            List<Point2D> cima = new ArrayList<>(), baixo = new ArrayList<>();
            Runnable fechar = () -> {
                if (cima.size() > 1) {
                    List<Point2D> poly = new ArrayList<>(cima);
                    List<Point2D> volta = new ArrayList<>(baixo);
                    Collections.reverse(volta);
                    poly.addAll(volta);
                    p.forma(poligono(poly), GOLA, BORDA, 1);
                }
                cima.clear(); baixo.clear();
            };
            for (double f = 34; f <= 326; f += 3) {
                if (vis(74, f) > .02) { cima.add(sp(70.5, f, 1.6)); baixo.add(sp(79, f, 1.6)); }
                else fechar.run();
            }
            fechar.run();
        }

        void casaco() {
            Area corpo = tronco();
            sombrear(corpo, GI, BORDA, 1.3);
            // costuras laterais e das costas (indicam a orientacao do tronco)
            linhaSup(new double[][] {{100, -90}, {194, -90}}, .5, COSTURA, .8);
            linhaSup(new double[][] {{100, 90}, {194, 90}}, .5, COSTURA, .8);
            linhaSup(new double[][] {{84, 180}, {148, 180}}, .5, COSTURA, .8);
            // peito de pelo na abertura em V
            List<Point2D> v = new ArrayList<>(), volta = new ArrayList<>();
            for (double y = 77; y <= 109; y += 2) {
                double hwO = Math.toDegrees(5.5 / interp(TA, y));
                double fo = phiLapelaFora(y) - hwO, fi = phiLapelaDentro(y) + hwO;
                if (vis(y, (fo + fi) / 2) > .02) { v.add(sp(y, fo, .6)); volta.add(sp(y, fi, .6)); }
            }
            if (v.size() > 2) {
                Collections.reverse(volta);
                v.addAll(volta);
                // 6.0.2: mesmo tom da base do pescoco, para o peito continuar o pescoco sem degrau
                p.forma(poligono(v), PELO_SOMBRA, new Color(150, 154, 162), .8);
            }
            pescoco(); // 6.0.2: depois do corpo do casaco, antes da gola que o abraca
            golaTras();
            // lapela de baixo (painel direito) e de cima (painel esquerdo, cruza por cima)
            lapela(LAPELA_DENTRO, 11, GOLA, BORDA);
            lapela(LAPELA_FORA, 11, GOLA, BORDA);
            // saia do casaco: abertura frontal e continuacao da borda do painel
            List<Point2D> fenda = new ArrayList<>();
            // 6.1: a abertura some gradualmente quando a frente do casaco vira para tras
            double ff = suave(.0, .05, vis(184, -8));
            fenda.add(sp(170, -16, .4)); fenda.add(sp(196, -15, .4)); fenda.add(sp(196, 2, .4));
            comOpacidade(ff, () -> p.forma(poligono(fenda), GI_SOMBRA, BORDA, .8));
            linhaSup(new double[][] {{162, -20}, {196, -15}}, .5, BORDA, 1);
        }

        /** Centro das lapelas {y, angulo na superficie}: o painel esquerdo cruza por cima do direito. */
        static final double[][] LAPELA_FORA = {{76, 36}, {92, 22}, {112, 6}, {132, -9}, {150, -21}};
        static final double[][] LAPELA_DENTRO = {{76, -36}, {92, -22}, {104, -11}, {112, -3}};
        double phiLapelaFora(double y) { return interpTab(LAPELA_FORA, y); }
        double phiLapelaDentro(double y) { return interpTab(LAPELA_DENTRO, y); }
        static double interpTab(double[][] t, double y) {
            if (y <= t[0][0]) return t[0][1];
            for (int i = 1; i < t.length; i++)
                if (y <= t[i][0]) return t[i - 1][1] + (t[i][1] - t[i - 1][1]) * (y - t[i - 1][0]) / (t[i][0] - t[i - 1][0]);
            return t[t.length - 1][1];
        }

        // ---------- faixa
        void faixa() {
            List<Point2D> cima = arco(149.5, 1.8), baixo = arco(161, 1.8);
            if (cima.size() > 1 && baixo.size() > 1) {
                List<Point2D> poly = new ArrayList<>(cima);
                List<Point2D> volta = new ArrayList<>(baixo);
                Collections.reverse(volta);
                poly.addAll(volta);
                p.forma(poligono(poly), FAIXA, FAIXA_BORDA, 1.1);
                List<Point2D> meio = arco(155.3, 1.9);
                if (meio.size() > 3) p.tracar(polilinha(meio.subList(1, meio.size() - 1)), FAIXA_LUZ, .8);
            }
            double frente = cam.prof(c.dir(new V(0, 0, 1), 155));
            // no (amarracao) fica escondido nas vistas de costas; 6.1: some gradualmente entre ~92 e ~98 graus
            double fk = suave(-.14, -.04, frente);
            if (fk <= 0) return;
            double fs = Math.max(.42, frente);
            Point2D k = sp(155.5, 0, 4.5);
            double alfaAntes = p.alfa;
            p.alfa = alfaAntes * fk;
            p.salvar();
            p.mover(k.getX(), k.getY());
            p.escalarXY(fs, 1);
            p.forma(pts(-3.5, -1, 4.5, 1, -5.5, 30, -14.5, 27), FAIXA, FAIXA_BORDA, 1);
            p.forma(pts(.5, 0, 9.5, -1, 22.5, 22, 13.5, 27), new Color(20, 20, 22), FAIXA_BORDA, 1);
            p.preencher(pts(10.5, 17, 18.5, 13, 22.5, 22, 13.5, 27), PONTEIRA);
            p.forma(pts(-9.5, -4, .5, -7, 10.5, -3, 7.5, 5, -3.5, 7, -10.5, 2), FAIXA, FAIXA_BORDA, 1.2);
            p.linha(FAIXA_LUZ, 1, -5.5, -3, 1.5, -4, 6.5, -1);
            p.restaurar();
            p.alfa = alfaAntes;
        }

        static Path2D pts(double... xy) {
            Path2D q = new Path2D.Double();
            q.moveTo(xy[0], xy[1]);
            for (int i = 2; i < xy.length; i += 2) q.lineTo(xy[i], xy[i + 1]);
            q.closePath();
            return q;
        }

        // ---------- cabeca
        /**
         * Silhueta de perfil (6.0.2), olhando para a direita, centro da cabeca na origem (raio 35).
         * Cranio arredondado, testa descendo em uma "parada" suave, ponte do nariz curta, nariz,
         * labio, queixo pequeno e mandibula voltando para o pescoco. Mesma altura (~72) e mesmo
         * comprimento (~80) da cabeca 6.0.1 de perfil.
         */
        static final double[][] PERFIL = {
            {-35, 2}, {-31, -17}, {-17, -32}, {1, -36}, {18, -31.5}, {29.5, -20}, {34.5, -10.5}, {37.5, -4.5},
            {41.5, 0.5}, {45, 6}, {44.2, 11.2}, {41, 14.5}, {40.6, 19.5}, {36.5, 25.5}, {25, 31.5}, {9, 35.5},
            {-8, 35}, {-23, 28.5}, {-32, 16}};

        /** Peso do perfil (0 = cabeca 6.0.1, 1 = silhueta de perfil): so perto de 90 graus. */
        static double pesoPerfil(double guinada) {
            double s = Math.abs(Math.sin(Math.toRadians(guinada)));
            double t = Math.max(0, Math.min(1, (s - .87) / .13));
            return t * t * (3 - 2 * t);
        }

        static Path2D curvaFechada(double[][] q, double dir, double cx, double cy) {
            int n = q.length;
            Path2D p = new Path2D.Double();
            p.moveTo(cx + q[0][0] * dir, cy + q[0][1]);
            for (int i = 0; i < n; i++) {
                double[] a = q[(i - 1 + n) % n], b = q[i], c = q[(i + 1) % n], d = q[(i + 2) % n];
                p.curveTo(cx + (b[0] + (c[0] - a[0]) / 6) * dir, cy + b[1] + (c[1] - a[1]) / 6,
                        cx + (c[0] - (d[0] - b[0]) / 6) * dir, cy + c[1] - (d[1] - b[1]) / 6,
                        cx + c[0] * dir, cy + c[1]);
            }
            p.closePath();
            return p;
        }

        /** Maior distancia do centro ate o contorno, ao longo de um raio (para misturar silhuetas). */
        static double raio(Shape s, double cx, double cy, double ang) {
            double ux = Math.cos(ang), uy = Math.sin(ang), melhor = 0;
            double[] c = new double[6];
            double x0 = 0, y0 = 0, mx = 0, my = 0;
            for (PathIterator it = new FlatteningPathIterator(s.getPathIterator(null), .3); !it.isDone(); it.next()) {
                int tipo = it.currentSegment(c);
                double x1, y1;
                if (tipo == PathIterator.SEG_MOVETO) { x0 = mx = c[0]; y0 = my = c[1]; continue; }
                if (tipo == PathIterator.SEG_CLOSE) { x1 = mx; y1 = my; } else { x1 = c[0]; y1 = c[1]; }
                double ex = x1 - x0, ey = y1 - y0, den = ux * ey - uy * ex;
                if (Math.abs(den) > 1e-9) {
                    double ax = x0 - cx, ay = y0 - cy;
                    double t = (ax * ey - ay * ex) / den, u = (ax * uy - ay * ux) / den;
                    if (t > 0 && u >= 0 && u <= 1) melhor = Math.max(melhor, t);
                }
                x0 = x1; y0 = y1;
            }
            return melhor;
        }

        static Point2D projCabeca(Point2D c2, double guinada, V v) {
            double r = Math.toRadians(guinada), cs = Math.cos(r), sn = Math.sin(r);
            return pt(c2.getX() + v.x() * cs + v.z() * sn,
                    c2.getY() + v.y() + Camera.INCLINACAO * .5 * (-v.x() * sn + v.z() * cs));
        }

        /** Silhueta da cabeca sem orelhas. Frente e 3/4 identicos a 6.0.1; o perfil usa a curva PERFIL. */
        static Area silhuetaCabeca(Point2D c2, double guinada) {
            double r = Math.toRadians(guinada), cs = Math.cos(r), sn = Math.sin(r);
            double rx = Math.hypot(RAIO_CABECA * cs, 38 * sn);
            Point2D cc = projCabeca(c2, guinada, new V(0, 0, -2));
            Area base = new Area(new Ellipse2D.Double(cc.getX() - rx, cc.getY() - RAIO_CABECA, 2 * rx, 2 * RAIO_CABECA));
            Point2D fc = projCabeca(c2, guinada, new V(0, 10, .84 * RAIO_CABECA));
            base.add(new Area(new Ellipse2D.Double(fc.getX() - 13, fc.getY() - 10, 26, 20)));
            double w = pesoPerfil(guinada);
            if (w <= 0) return base;
            Area perfil = new Area(curvaFechada(PERFIL, sn >= 0 ? 1 : -1, c2.getX(), c2.getY()));
            if (w >= 1) return perfil;
            // transicao continua: mistura os raios das duas silhuetas a partir do centro
            List<Point2D> q = new ArrayList<>();
            for (int i = 0; i < 180; i++) {
                double a = Math.toRadians(i * 2);
                double rr = (1 - w) * raio(base, c2.getX(), c2.getY(), a) + w * raio(perfil, c2.getX(), c2.getY(), a);
                q.add(pt(c2.getX() + rr * Math.cos(a), c2.getY() + rr * Math.sin(a)));
            }
            return new Area(poligono(q));
        }

        /**
         * Pescoco (6.0.2): cilindro de pelo do pivo do pescoco ate o pivo da cabeca, desenhado depois do
         * casaco e antes da gola, que o abraca por fora. A cabeca projeta uma sombra curta nele.
         */
        void pescoco() {
            Point2D topo = cam.p(c.cabeca);
            // termina reto logo abaixo da borda de baixo da gola (y ~78,5): nunca aparece abaixo dela
            Point2D base = lerp(topo, cam.p(c.pescoco), (79 - CABECA_Y) / (PESCOCO_Y - CABECA_Y));
            Area n = new Area(trapezio(topo, base, 26, 29));
            p.preencher(n, PELO);
            Area sombra = new Area(n);
            sombra.intersect(silhuetaCabeca(topo, guinadaRosto())
                    .createTransformedArea(AffineTransform.getTranslateInstance(0, 5)));
            p.preencher(sombra, PELO_SOMBRA);
            Area lado = new Area(n);
            lado.subtract(n.createTransformedArea(AffineTransform.getTranslateInstance(-4, 0)));
            p.preencher(lado, PELO_SOMBRA);
        }

        /** Cabeca: cranio levemente mais fundo que largo + focinho; o rosto vive na superficie e gira com ela. */
        void cabeca(V centro, double guinada) {
            Point2D c2 = cam.p(centro);
            double r = Math.toRadians(guinada), cs = Math.cos(r), sn = Math.sin(r);
            double w = pesoPerfil(guinada), dir = sn >= 0 ? 1 : -1;
            java.util.function.Function<V, Point2D> hp = v -> projCabeca(c2, guinada, v);
            java.util.function.ToDoubleFunction<Double> encara = lon -> Math.cos(Math.toRadians(lon) + r);
            // ponto do desenho de perfil (olhando para a direita), espelhado conforme o lado
            java.util.function.BiFunction<Double, Double, Point2D> pp = (x, y) -> pt(c2.getX() + x * dir, c2.getY() + y);

            // orelhas: a mais distante primeiro
            int[] ordem = encara.applyAsDouble(-90.0) > encara.applyAsDouble(90.0) ? new int[] {1, -1} : new int[] {-1, 1};
            for (int i = 0; i < 2; i++) orelha(hp, ordem[i], encara, i == 0, w, dir);

            Area cab = silhuetaCabeca(c2, guinada);
            p.preencher(cab, PELO);
            // volume: meia-lua de sombra embaixo/a direita (luz fixa da tela)
            Area sombra = new Area(cab);
            sombra.subtract(cab.createTransformedArea(AffineTransform.getTranslateInstance(-4.5, -3.5)));
            p.preencher(sombra, PELO_SOMBRA);

            // 6.1: nariz e boca somem gradualmente logo depois do perfil (antes sumiam de uma vez em ~93 graus)
            double fRosto = suave(-.15, 0, cs);
            boolean rosto = fRosto > 0;
            // contorno do focinho no 3/4: o focinho encobre a bochecha mais distante (6.1: com entrada suave)
            double fContorno = suave(.2, .3, Math.abs(sn)) * suave(.05, .15, cs);
            if (fContorno > 0) {
                double alfaAntes = p.alfa;
                p.alfa = alfaAntes * fContorno;
                double rx = Math.hypot(RAIO_CABECA * cs, 38 * sn);
                Point2D cc = hp.apply(new V(0, 0, -2)), fc = hp.apply(new V(0, 10, .84 * RAIO_CABECA));
                Shape cranio = new Ellipse2D.Double(cc.getX() - rx, cc.getY() - RAIO_CABECA, 2 * rx, 2 * RAIO_CABECA);
                List<Point2D> arcoF = new ArrayList<>();
                for (double a = -65; a <= 65; a += 5) {
                    double ar = Math.toRadians(a);
                    Point2D q = pt(fc.getX() + dir * 13 * Math.cos(ar), fc.getY() + 10 * Math.sin(ar));
                    if (cranio.contains(q)) arcoF.add(q);
                }
                if (arcoF.size() > 2) p.tracar(polilinha(arcoF), new Color(205, 209, 216), 1.1);
                p.alfa = alfaAntes;
            }
            // 6.0.3: o traco interno da bochecha no perfil foi removido; o focinho e definido pela silhueta.
            // olhos sobre a esfera: aproximam-se e estreitam conforme giram; no perfil recuam da testa
            for (int s = -1; s <= 1; s += 2) {
                double lon = 18 * s, k = encara.applyAsDouble(lon);
                double fOlho = suave(.12, .22, k); // 6.1: o olho fecha gradualmente ao passar para tras
                if (fOlho <= 0) continue;
                double la = Math.toRadians(lon), lt = Math.toRadians(-3);
                Point2D o = hp.apply(new V(RAIO_CABECA * Math.cos(lt) * Math.sin(la), RAIO_CABECA * Math.sin(lt),
                        RAIO_CABECA * Math.cos(lt) * Math.cos(la)));
                o = lerp(o, pp.apply(25.0, -4.0), w);
                double larg = (1 - w) * 3.1 * Math.max(.42, k) + w * 2.5, alt = (1 - w) * 3.2 + w * 3.1;
                Ellipse2D olho = new Ellipse2D.Double(o.getX() - larg, o.getY() - alt, 2 * larg, 2 * alt);
                comOpacidade(fOlho, () -> p.preencher(olho, OLHO));
            }
            if (rosto) {
                double alfaAntes = p.alfa;
                p.alfa = alfaAntes * fRosto;
                // perfil: nariz como uma cunha rosa na ponta do focinho
                Point2D a = lerp(hp.apply(new V(-3.5, 8, 38.5)), pp.apply(40.2, 3.2), w);
                Point2D f = lerp(hp.apply(new V(0, 8.5, 41.8)), pp.apply(45.9, 5.6), w);
                Point2D b = lerp(hp.apply(new V(3.5, 8, 38.5)), pp.apply(45.0, 9.6), w);
                Point2D d = lerp(hp.apply(new V(0, 12.5, 40.2)), pp.apply(41.6, 9.4), w);
                p.preencher(poligono(List.of(a, f, b, d)), NARIZ);
                Point2D m1 = lerp(hp.apply(new V(0, 17.5, 39.5)), pp.apply(41.8, 14.8), w);
                Point2D me = lerp(hp.apply(new V(-5, 21.5, 37.5)), pp.apply(36.0, 17.6), w);
                Point2D md = lerp(hp.apply(new V(5, 21.5, 37.5)), pp.apply(36.0, 17.6), w);
                Point2D inicio = lerp(d, pp.apply(43.4, 9.8), w);
                p.linha(BOCA, 1.4, inicio.getX(), inicio.getY(), m1.getX(), m1.getY(), me.getX(), me.getY());
                p.linha(BOCA, 1.4, m1.getX(), m1.getY(), md.getX(), md.getY());
                p.alfa = alfaAntes;
            }
        }

        void orelha(java.util.function.Function<V, Point2D> hp, int s, java.util.function.ToDoubleFunction<Double> encara,
                    boolean longe, double w, double dir) {
            double lon = 48 * s, lo = Math.toRadians(lon);
            V base = esf(lon, -48, .97), tang = new V(Math.cos(lo), 0, -Math.sin(lo));
            V e1 = base.menos(tang.vezes(10.5)).mais(new V(0, 1.5, 0)), e2 = base.mais(tang.vezes(10.5)).mais(new V(0, 1.5, 0));
            V apice = base.mais(new V(s * 2, -34, -3));
            // orelha em concha: a base tem profundidade, entao ela nunca vira uma linha quando vista de lado
            V normal = new V(Math.sin(lo), 0, Math.cos(lo)), fundo = base.menos(normal.vezes(9)).mais(new V(0, 1.5, 0));
            // perfil (6.0.2): as duas orelhas se separam; a de longe vai a frente, um pouco mais baixa
            double dx = w * dir * (longe ? 10 : -4), dy = w * (longe ? 3 : 0);
            AffineTransform desl = AffineTransform.getTranslateInstance(dx, dy);
            Point2D a = desl.transform(hp.apply(e1), null), b = desl.transform(hp.apply(apice), null);
            Point2D c2 = desl.transform(hp.apply(e2), null), f = desl.transform(hp.apply(fundo), null);
            Area concha = new Area(poligono(List.of(a, b, c2)));
            concha.add(new Area(poligono(List.of(a, b, f))));
            concha.add(new Area(poligono(List.of(f, b, c2))));
            // 6.1: a orelha de longe escurece gradualmente conforme o perfil se forma (antes: degrau em w = 0,5)
            p.preencher(concha, longe ? misturar(PELO, PELO_LONGE, suave(.3, .7, w)) : PELO);
            double k = encara.applyAsDouble(lon);
            double fInterna = suave(.10, .20, k); // parte interna so aparece quando a orelha encara a camera
            if (fInterna > 0) {
                Point2D m = lerp(lerp(a, c2, .5), b, .25);
                Path2D interna = poligono(List.of(lerp(m, a, .55), lerp(m, b, .62), lerp(m, c2, .55)));
                comOpacidade(fInterna, () -> p.preencher(interna, ORELHA_INTERNA));
            }
        }

        static V esf(double lonG, double latG, double k) {
            double lo = Math.toRadians(lonG), la = Math.toRadians(latG), R = RAIO_CABECA * k;
            return new V(R * Math.cos(la) * Math.sin(lo), R * Math.sin(la), R * Math.cos(la) * Math.cos(lo));
        }

        // ---------- vista tecnica
        void esqueleto(boolean rotulos) {
            int perto = ladoPerto() < 0 ? 0 : ladoPerto();
            Point2D pel = cam.p(c.pelve), pes = cam.p(c.pescoco), cab = cam.p(c.cabeca);
            p.linha(OSSO, 1.6, pel.getX(), pel.getY(), pes.getX(), pes.getY(), cab.getX(), cab.getY());
            for (int i = 0; i < 2; i++) {
                Point2D s = cam.p(c.ombro[i]), e = cam.p(c.cotovelo[i]), w = cam.p(c.mao[i]);
                Point2D h = cam.p(c.quadril[i]), k = cam.p(c.joelho[i]), t = cam.p(c.tornozelo[i]);
                Color osso = i == perto ? OSSO : new Color(190, 130, 50);
                p.linha(osso, 1.4, pes.getX(), pes.getY(), s.getX(), s.getY());
                p.linha(osso, 1.6, s.getX(), s.getY(), e.getX(), e.getY(), w.getX(), w.getY());
                p.linha(osso, 1.4, pel.getX(), pel.getY(), h.getX(), h.getY());
                p.linha(osso, 1.6, h.getX(), h.getY(), k.getX(), k.getY(), t.getX(), t.getY());
            }
            for (int i = 0; i < 2; i++) {
                for (V v : new V[] {c.ombro[i], c.quadril[i]}) ponto(cam.p(v), RAIZ, 4.2);
                for (V v : new V[] {c.cotovelo[i], c.mao[i], c.joelho[i], c.tornozelo[i]}) ponto(cam.p(v), JUNTA, 3.8);
            }
            ponto(pes, RAIZ, 4.2);
            ponto(cab, JUNTA, 3.8);
            ponto(pel, RAIZ, 6);
            p.tracar(circ(pel, 9), RAIZ, 1);
            if (!rotulos) return;
            double t = 8.5;
            rotulo(pel, "pelve (raiz do corpo)", -14, 26, t);
            rotulo(pes, "pescoço / gola", -58, -4, t);
            rotulo(cab, "cabeça (pivô + guinada)", -100, -48, t);
            rotulo(cam.p(c.ombro[perto]), "ombro perto", -76, -2, t);
            rotulo(cam.p(c.ombro[1 - perto]), "ombro longe", 26, -20, t);
            rotulo(cam.p(c.cotovelo[perto]), "cotovelo", -58, 14, t);
            rotulo(cam.p(c.mao[perto]), "punho", 4, 30, t);
            rotulo(cam.p(c.mao[1 - perto]), "punho longe", 22, -10, t);
            rotulo(cam.p(c.quadril[perto]), "quadril", -62, -2, t);
            rotulo(cam.p(c.joelho[perto]), "joelho", -52, 4, t);
            rotulo(cam.p(c.tornozelo[perto]), "tornozelo", -64, 6, t);
            rotulo(cam.p(c.joelho[1 - perto]), "joelho longe", 18, 0, t);
        }

        void ponto(Point2D q, Color cor, double r) { p.forma(circ(q, r), cor, new Color(125, 85, 27), .8); }

        void rotulo(Point2D q, String s, double dx, double dy, double tam) {
            p.linha(new Color(200, 205, 214, 150), .6, q.getX(), q.getY(), q.getX() + dx * .55, q.getY() + dy * .55 - 2);
            p.texto(s, q.getX() + dx, q.getY() + dy, tam, TEXTO, false, dx < 0 ? -1 : -1);
        }
    }

    // ===================================================================== pranchas
    interface Cena { void desenhar(Pintor p); }

    static void salvar(Path dir, String nome, int w, int h, Cena cena) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Pintor p = new Pintor(g, w, h);
        cena.desenhar(p);
        g.dispose();
        ImageIO.write(img, "png", dir.resolve(nome + ".png").toFile());
        Files.writeString(dir.resolve(nome + ".svg"), p.documento(), StandardCharsets.UTF_8);
        System.out.println("gerado: " + dir.resolve(nome + ".png") + " / .svg");
    }

    static void figura(Pintor p, Corpo c, double guinada, double ox, double chao, double esc, double alfa, boolean tecnico) {
        p.salvar();
        p.mover(ox, chao);
        p.escalar(esc);
        p.mover(0, -CHAO);
        Desenho d = new Desenho(p, c, guinada);
        double a = p.alfa;
        p.alfa = alfa;
        d.figura();
        p.alfa = a;
        if (tecnico) d.esqueleto(true);
        p.restaurar();
    }

    static void fundo(Pintor p) { p.preencher(new Rectangle2D.Double(0, 0, p.w, p.h), FUNDO); }

    static void cartao(Pintor p, double x, double y, double w, double h) {
        p.preencher(new RoundRectangle2D.Double(x, y, w, h, 18, 18), CARTAO);
    }

    static void tracejado(Pintor p, double x0, double x1, double y, Color c) {
        for (double x = x0; x < x1; x += 10) p.linha(c, .8, x, y, Math.min(x + 5, x1), y);
    }

    static final double[] VISTAS = {0, 40, 90, 140, 180};
    static final String[] NOMES = {"FRENTE", "3/4 FRONTAL", "PERFIL", "3/4 COSTAS", "COSTAS"};

    static void prancha(Path dir) throws IOException {
        int w = 1400, h = 620;
        salvar(dir, "prancha-cinco-perspectivas", w, h, p -> {
            fundo(p);
            p.texto("IRMÃO MIAU BRANCO  ·  RIG 2.0  ·  CINCO PERSPECTIVAS  ·  REFINADO 6.0.4", 40, 42, 20, TEXTO, true, -1);
            p.texto("Mesmo esqueleto, mesmas medidas e mesma roupa nas cinco vistas: só muda o ângulo da câmera (guinada).",
                    40, 66, 13, SUAVE, false, -1);
            double esc = 1.22, chao = 528;
            for (int i = 0; i < 5; i++) cartao(p, 48 + i * 252, 86, 240, 488);
            Corpo n = neutro();
            double[][] guias = {{CABECA_Y - 60, 0}, {CABECA_Y - 1.8, 0}, {90, 0}, {155, 0}, {n.joelho[0].y(), 0}, {CHAO, 0}};
            String[] nomes = {"orelhas", "olhos", "ombros", "faixa", "joelhos", "chão"};
            for (int i = 0; i < guias.length; i++) {
                double y = chao + (guias[i][0] - CHAO) * esc;
                tracejado(p, 52, 1300, y, new Color(166, 176, 190, 70));
                p.texto(nomes[i], 1306, y + 4, 11, SUAVE, false, -1);
            }
            for (int i = 0; i < 5; i++) {
                double cx = 48 + 120 + i * 252;
                figura(p, neutro(), VISTAS[i], cx, chao, esc, 1, false);
                p.texto(NOMES[i], cx, chao + 30, 14, TEXTO, true, 0);
                p.texto((int) VISTAS[i] + "°", cx, chao + 48, 12, SUAVE, false, 0);
            }
            p.texto("Kimono preto e faixa preta (padrão do Miau Branco no laboratório). Guias tracejadas iguais nas cinco vistas.",
                    40, 606, 12, SUAVE, false, -1);
        });
    }

    static void combate(Path dir) throws IOException {
        int w = 760, h = 740;
        salvar(dir, "combate-3-4", w, h, p -> {
            fundo(p);
            cartao(p, 20, 20, w - 40, h - 40);
            p.texto("POSIÇÃO DE COMBATE  ·  3/4 FRONTAL (40°)", 44, 56, 18, TEXTO, true, -1);
            p.texto("Quadril 16 abaixo, tronco 8° à frente, guarda à frente do peito, pé esquerdo à frente, rosto a 30°.",
                    44, 78, 12, SUAVE, false, -1);
            figura(p, combate(), 40, 360, 660, 1.92, 1, false);
            p.texto("Lado perto (direito) desenhado por cima; lado longe mais escuro e por trás do tronco.", 44, 704, 12, SUAVE, false, -1);
        });
    }

    static void estrutura(Path dir) throws IOException {
        int w = 1240, h = 740;
        salvar(dir, "estrutura-3-4", w, h, p -> {
            fundo(p);
            cartao(p, 20, 20, 640, h - 40);
            cartao(p, 676, 20, w - 696, h - 40);
            p.texto("ESTRUTURA TÉCNICA  ·  3/4 (40°)", 44, 56, 18, TEXTO, true, -1);
            p.texto("Azul: raízes  |  Amarelo: articulações  |  Laranja: ossos (escuro = lado longe)", 44, 78, 12, SUAVE, false, -1);
            figura(p, combate(), 40, 330, 664, 1.88, .42, true);

            double x = 700, y = 60;
            p.texto("CAMADAS (ordem de desenho)", x, y, 15, TEXTO, true, -1);
            String[] camadas = {
                "1  sombra no chão",
                "2  braço longe: manga contínua + pata",
                "3  perna longe: pé + calça",
                "4  perna perto + quadril: pé + calça",
                "5  casaco: silhueta por seções elípticas",
                "6  costuras e peito em V",
                "7  pescoço (com a sombra curta da cabeça)",
                "8  gola de trás e lapelas, por fora do pescoço",
                "9  faixa: volta, nó e pontas (só de frente)",
                "10 cabeça: orelhas, crânio, focinho, rosto",
                "11 braço perto: manga contínua + pata"};
            for (int i = 0; i < camadas.length; i++) p.texto(camadas[i], x, y + 26 + i * 20, 12.5, SUAVE, false, -1);

            y += 266;
            p.texto("COMPRIMENTOS FIXOS (unidades do rig, cabeça = 70)", x, y, 15, TEXTO, true, -1);
            String[] medidas = {
                "braço 39  ·  antebraço 30   (≈145% do braço-base)",
                "coxa 46,5  ·  canela 46     (≈120% da perna-base)",
                "Os ossos são resolvidos por IK de dois ossos em 3D:",
                "nenhuma pose ou vista estica ou encolhe um membro."};
            for (int i = 0; i < medidas.length; i++) p.texto(medidas[i], x, y + 26 + i * 20, 12.5, SUAVE, false, -1);

            y += 124;
            p.texto("GUINADA DA CÂMERA (vista de cima)", x, y, 15, TEXTO, true, -1);
            double cx = x + 120, cy = y + 120, r = 82;
            p.tracar(circ(pt(cx, cy), r), new Color(166, 176, 190, 110), 1);
            p.forma(new Ellipse2D.Double(cx - 22, cy - 12, 44, 24), GI, BORDA, 1);
            p.linha(JUNTA, 2, cx, cy, cx, cy + 34);
            p.texto("frente do lutador", cx + 6, cy + 48, 10, SUAVE, false, -1);
            for (int i = 0; i < VISTAS.length; i++) {
                double a = Math.toRadians(VISTAS[i]);
                double px = cx + Math.sin(a) * r, py = cy + Math.cos(a) * r;
                p.forma(circ(pt(px, py), 5), i == 1 ? JUNTA : RAIZ, new Color(125, 85, 27), .8);
                p.texto((int) VISTAS[i] + "°  " + NOMES[i].toLowerCase(Locale.ROOT), px + 10, py + 4, 11, TEXTO, false, -1);
            }
            p.texto("Câmera em 40°: perspectiva principal de combate.", x, y + 232, 12, SUAVE, false, -1);
        });
    }

    /** Conferencias numericas: ossos com comprimento constante em todas as poses e vistas. */
    static void verificar() {
        for (Corpo c : new Corpo[] {neutro(), combate()}) {
            for (int i = 0; i < 2; i++) {
                checar(c.cotovelo[i].menos(c.ombro[i]).tam(), BRACO, "braco");
                checar(c.mao[i].menos(c.cotovelo[i]).tam(), ANTEBRACO, "antebraco");
                checar(c.joelho[i].menos(c.quadril[i]).tam(), COXA, "coxa");
                checar(c.tornozelo[i].menos(c.joelho[i]).tam(), CANELA, "canela");
            }
        }
        System.out.println("OK: braco, antebraco, coxa e canela com comprimento constante nas duas poses.");

        // 6.0.2: pescoco e cabeca
        for (Corpo c : new Corpo[] {neutro(), combate()}) {
            checar(c.cabeca.menos(c.pescoco).tam(), Math.hypot(PESCOCO_Y - CABECA_Y, c.cabecaZ - 0), "pescoco");
            for (double g : VISTAS) {
                Desenho d = new Desenho(null, c, g);
                Point2D base = d.cam.p(c.pescoco), topo = d.cam.p(c.cabeca);
                if (!d.tronco().contains(base)) throw new AssertionError("pescoco solto do tronco em " + g);
                Rectangle2D r = Desenho.silhuetaCabeca(topo, Desenho.guinadaCabeca(g)).getBounds2D();
                if (r.getHeight() < 69 || r.getHeight() > 74) throw new AssertionError("altura da cabeca em " + g + ": " + r.getHeight());
                if (r.getWidth() < 69 || r.getWidth() > 82) throw new AssertionError("largura da cabeca em " + g + ": " + r.getWidth());
            }
        }
        checar(Desenho.guinadaCabeca(40), 30, "rosto do 3/4");
        for (double g : new double[] {0, 40, 140, 180})
            checar(Desenho.pesoPerfil(Desenho.guinadaCabeca(g)), 0, "frente/3/4 sem silhueta de perfil em " + g);
        checar(Desenho.pesoPerfil(Desenho.guinadaCabeca(90)), 1, "perfil");
        double antes = Desenho.guinadaCabeca(89.9), depois = Desenho.guinadaCabeca(90);
        if (Math.abs(depois - antes) > .5) throw new AssertionError("salto na guinada do rosto em 90");
        System.out.println("OK: pescoco rigido e preso ao tronco, cabeca 69-74 de altura nas cinco vistas,"
                + " rosto de frente/3/4 sem mistura de perfil, rotacao do rosto continua.");
    }

    // ===================================================================== verificacoes do laboratorio (6.1)
    /** Pose de teste obrigatoria (7.3): braco direito flexionado, braco esquerdo estendido a frente. */
    static ParametrosPose poseTesteBracos() {
        ParametrosPose p = ParametrosPose.padrao(PoseBase.COMBATE);
        p.elevacao[0] = 10; p.avanco[0] = 35; p.flexao[0] = 130;   // direito: recolhido e dobrado
        p.elevacao[1] = 0;  p.avanco[1] = 90; p.flexao[1] = 0;     // esquerdo: estendido a frente
        return p;
    }

    /** Juntas do corpo como lista, na ordem: pelve, pescoco, cabeca, e por lado ombro..tornozelo. */
    static List<V> juntas(Corpo c) {
        List<V> r = new ArrayList<>(List.of(c.pelve, c.pescoco, c.cabeca));
        for (int i = 0; i < 2; i++) r.addAll(List.of(c.ombro[i], c.cotovelo[i], c.mao[i], c.quadril[i], c.joelho[i], c.tornozelo[i]));
        return r;
    }

    /** Ossos {pai, filho} pelos indices de juntas(). */
    static final int[][] OSSOS = {{0, 1}, {1, 2}, {3, 4}, {4, 5}, {6, 7}, {7, 8}, {9, 10}, {10, 11}, {12, 13}, {13, 14}};

    /** Quadro offscreen de uma vista (usado nas verificacoes e nas capturas). */
    static BufferedImage quadro(Corpo c, double guinada, int w, int h, double esc, boolean esqueleto) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Pintor p = new Pintor(g, w, h);
        fundo(p);
        figura(p, c, guinada, w / 2.0, h - 24, esc, 1, false);
        if (esqueleto) {
            p.salvar();
            p.mover(w / 2.0, h - 24);
            p.escalar(esc);
            p.mover(0, -CHAO);
            new Desenho(p, c, guinada).esqueleto(false);
            p.restaurar();
        }
        g.dispose();
        return img;
    }

    /**
     * Verificacoes deterministicas da 6.1. Retorna o relatorio (uma linha por conferencia); lanca
     * AssertionError na primeira falha.
     */
    static List<String> verificarLaboratorio() {
        List<String> rel = new ArrayList<>();
        Map<String, ParametrosPose> poses = new LinkedHashMap<>();
        poses.put("neutra", ParametrosPose.padrao(PoseBase.NEUTRA));
        poses.put("combate", ParametrosPose.padrao(PoseBase.COMBATE));
        poses.put("teste 7.3 (bracos)", poseTesteBracos());
        for (PresetPernas pr : PresetPernas.values()) {
            ParametrosPose q = ParametrosPose.padrao(PoseBase.COMBATE);
            q.pernas = pr;
            poses.put("combate + " + pr.nome, q);
            ParametrosPose n = ParametrosPose.padrao(PoseBase.NEUTRA);
            n.pernas = pr;
            poses.put("neutra + " + pr.nome, n);
        }
        double[] extremos = {ELEVACAO_MIN, ELEVACAO_MAX};
        for (double e : extremos) for (double a : new double[] {AVANCO_MIN, AVANCO_MAX}) for (double f : new double[] {FLEXAO_MIN, FLEXAO_MAX}) {
            ParametrosPose q = ParametrosPose.padrao(PoseBase.NEUTRA);
            q.elevacao[0] = q.elevacao[1] = e; q.avanco[0] = q.avanco[1] = a; q.flexao[0] = q.flexao[1] = f;
            q.giroCabeca = e > 0 ? GIRO_CABECA_MAX : -GIRO_CABECA_MAX;
            poses.put("extremos e=" + (int) e + " a=" + (int) a + " f=" + (int) f, q);
        }

        // 1) comprimentos dos ossos (3D) em todas as poses
        int nPoses = 0;
        for (Map.Entry<String, ParametrosPose> en : poses.entrySet()) {
            Corpo c = montarPose(en.getValue());
            double tronco = c.pescoco.menos(c.transformar(new V(0, PIVO_CINTURA, 0))).tam();
            checar(tronco, PIVO_CINTURA - PESCOCO_Y, "tronco (cintura->pescoco) em " + en.getKey());
            checar(c.cabeca.menos(c.pescoco).tam(), Math.hypot(PESCOCO_Y - CABECA_Y, c.cabecaZ), "pescoco->cabeca em " + en.getKey());
            for (int i = 0; i < 2; i++) {
                checar(c.cotovelo[i].menos(c.ombro[i]).tam(), BRACO, "braco em " + en.getKey());
                checar(c.mao[i].menos(c.cotovelo[i]).tam(), ANTEBRACO, "antebraco em " + en.getKey());
                checar(c.joelho[i].menos(c.quadril[i]).tam(), COXA, "coxa em " + en.getKey());
                checar(c.tornozelo[i].menos(c.joelho[i]).tam(), CANELA, "canela em " + en.getKey());
            }
            nPoses++;
        }
        rel.add("OK: comprimentos 3D constantes (braco 39, antebraco 30, coxa 46,5, canela 46, tronco 82, pescoco)"
                + " em " + nPoses + " poses, incluindo presets de pernas e limites dos controles.");

        // 2) limites dos controles
        ParametrosPose lim = ParametrosPose.padrao(PoseBase.COMBATE);
        lim.elevacao[0] = 999; lim.avanco[0] = -999; lim.flexao[0] = 999; lim.giroCabeca = -999;
        lim.limitarTudo();
        checar(lim.elevacao[0], ELEVACAO_MAX, "limite da elevacao");
        checar(lim.avanco[0], AVANCO_MIN, "limite do avanco");
        checar(lim.flexao[0], FLEXAO_MAX, "limite da flexao");
        checar(lim.giroCabeca, -GIRO_CABECA_MAX, "limite do giro da cabeca");
        rel.add("OK: controles limitados (elevacao " + (int) ELEVACAO_MIN + ".." + (int) ELEVACAO_MAX + ", avanco "
                + (int) AVANCO_MIN + ".." + (int) AVANCO_MAX + ", flexao " + (int) FLEXAO_MIN + ".." + (int) FLEXAO_MAX
                + ", cabeca +-" + (int) GIRO_CABECA_MAX + ").");

        // 3) padrao reproduz a pose-base (os parametros nao perdem informacao)
        for (PoseBase b : PoseBase.values()) {
            Corpo c = montarPose(ParametrosPose.padrao(b));
            for (int i = 0; i < 2; i++) {
                V alvo = c.ombro[i].mais(b.maos[i].menos(c.ombro[i]));
                if (c.mao[i].menos(alvo).tam() > 1e-6) throw new AssertionError("pose-base " + b + " nao reproduzida no braco " + i);
            }
        }
        rel.add("OK: os parametros padrao reproduzem exatamente as maos das poses NEUTRA e COMBATE.");

        // 4) rotacao continua: coordenadas validas, projecao so encurta, juntas sem saltos
        int amostras = 0;
        double piorRazao = 0;
        for (Map.Entry<String, ParametrosPose> en : poses.entrySet()) {
            Corpo c = montarPose(en.getValue());
            List<V> js = juntas(c);
            Point2D[] antes = null;
            for (double g = 0; g <= 180.0001; g += .5) {
                Camera cam = new Camera(g);
                Point2D[] agora = new Point2D[js.size()];
                for (int k = 0; k < js.size(); k++) {
                    agora[k] = cam.p(js.get(k));
                    if (!Double.isFinite(agora[k].getX()) || !Double.isFinite(agora[k].getY()))
                        throw new AssertionError("coordenada invalida em " + en.getKey() + " a " + g);
                }
                for (int[] o : OSSOS) {
                    double l3 = js.get(o[1]).menos(js.get(o[0])).tam(), l2 = agora[o[1]].distance(agora[o[0]]);
                    if (l2 > l3 * 1.006 + 1e-9) throw new AssertionError("osso projetado maior que o real em " + en.getKey() + " a " + g);
                }
                if (antes != null) for (int k = 0; k < js.size(); k++) {
                    V v = js.get(k);
                    double raio = Math.hypot(v.x(), v.z()), limite = raio * Math.toRadians(.5) * 1.006 + 1e-9;
                    double passo = agora[k].distance(antes[k]);
                    piorRazao = Math.max(piorRazao, passo / Math.max(limite, 1e-9));
                    if (passo > limite) throw new AssertionError("salto de junta em " + en.getKey() + " a " + g);
                }
                antes = agora;
                amostras++;
            }
        }
        rel.add(String.format(Locale.ROOT, "OK: %d vistas (0 a 180 graus, passo 0,5) sem coordenadas invalidas; nenhum osso"
                + " projetado maior que o real; maior passo de junta = %.2f do limite de uma rotacao rigida.", amostras, piorRazao));

        // 5) a pose nao muda ao girar a camera (40 -> 90 -> 140 -> 0)
        Corpo t = montarPose(poseTesteBracos());
        List<V> inicio = new ArrayList<>(juntas(t));
        for (double g : new double[] {40, 90, 140, 0}) quadro(t, g, 220, 330, 1, true);
        if (!inicio.equals(juntas(t))) throw new AssertionError("a pose mudou durante a rotacao");
        rel.add("OK: pose de teste (braco direito flexionado, esquerdo estendido) identica apos 40 -> 90 -> 140 -> 0.");

        // 6) rosto: guinada continua, inclusive com giro proprio da cabeca
        double maiorSalto = 0;
        for (double giro : new double[] {-GIRO_CABECA_MAX, 0, GIRO_CABECA_MAX}) {
            double ant = Desenho.guinadaCabeca(giro);
            for (double g = .1; g <= 180.0001; g += .1) {
                double at = Desenho.guinadaCabeca(g + giro);
                maiorSalto = Math.max(maiorSalto, Math.abs(at - ant));
                ant = at;
            }
        }
        if (maiorSalto > .2) throw new AssertionError("salto na guinada do rosto: " + maiorSalto);
        rel.add(String.format(Locale.ROOT, "OK: guinada do rosto continua (maior variacao %.3f grau por passo de 0,1).", maiorSalto));
        return rel;
    }

    /**
     * Continuidade visual: diferenca media por pixel entre quadros vizinhos (passo de 1 grau).
     * Retorna {angulo do maior salto, maior diferenca, mediana} por pose.
     */
    static Map<String, double[]> continuidadeVisual() {
        Map<String, double[]> r = new LinkedHashMap<>();
        Map<String, ParametrosPose> poses = new LinkedHashMap<>();
        poses.put("neutra", ParametrosPose.padrao(PoseBase.NEUTRA));
        poses.put("combate", ParametrosPose.padrao(PoseBase.COMBATE));
        poses.put("teste 7.3", poseTesteBracos());
        for (Map.Entry<String, ParametrosPose> en : poses.entrySet()) {
            Corpo c = montarPose(en.getValue());
            BufferedImage ant = null;
            double[] difs = new double[181];
            for (int g = 0; g <= 180; g++) {
                BufferedImage q = quadro(c, g, 200, 300, .95, false);
                if (ant != null) difs[g] = diferencaMedia(ant, q);
                ant = q;
            }
            double[] ord = Arrays.copyOfRange(difs, 1, 181);
            Arrays.sort(ord);
            int pior = 1;
            for (int g = 1; g <= 180; g++) if (difs[g] > difs[pior]) pior = g;
            r.put(en.getKey(), new double[] {pior, difs[pior], ord[ord.length / 2]});
        }
        return r;
    }

    static double diferencaMedia(BufferedImage a, BufferedImage b) {
        long soma = 0;
        for (int y = 0; y < a.getHeight(); y++)
            for (int x = 0; x < a.getWidth(); x++) {
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                soma += Math.abs((p >> 16 & 255) - (q >> 16 & 255)) + Math.abs((p >> 8 & 255) - (q >> 8 & 255)) + Math.abs((p & 255) - (q & 255));
            }
        return soma / (3.0 * a.getWidth() * a.getHeight());
    }

    /** Rotulo de cada versao, pelo nome da pasta de previas (a pasta gerada agora e a versao atual). */
    static final String VERSAO_ATUAL = "6.0.4  ·  pescoço curto, cabeça perto da gola";
    static String rotulo(Path dir, Path atual) {
        if (dir.equals(atual)) return VERSAO_ATUAL;
        return switch (dir.getFileName().toString()) {
            case "previas" -> "6.0.1  ·  original";
            case "previas-refinadas" -> "6.0.2  ·  perfil e pescoço";
            case "previas-6.0.3" -> "6.0.3  ·  pescoço mais discreto, perfil sem traço";
            default -> dir.getFileName().toString();
        };
    }

    /** Comparativo entre versoes a partir dos PNG (as previas anteriores nao sao regeradas). */
    static void comparativo(List<Path> anteriores, Path atual) throws IOException {
        List<Path> todas = new ArrayList<>(anteriores);
        todas.add(atual);
        compor(todas, atual, "prancha-cinco-perspectivas.png", new Rectangle(40, 80, 1320, 500), false, atual.resolve("comparativo-prancha.png"));
        compor(todas, atual, "combate-3-4.png", new Rectangle(20, 90, 720, 600), true, atual.resolve("comparativo-combate.png"));
        detalhe(todas, atual, atual.resolve("comparativo-detalhe-pescoco.png"));
    }

    static void compor(List<Path> dirs, Path atual, String nome, Rectangle corte, boolean ladoALado, Path saida) throws IOException {
        int n = dirs.size(), topo = 44, gap = 30;
        for (Path d : dirs) if (!Files.exists(d.resolve(nome))) { System.out.println("sem versao para comparar: " + d); return; }
        int w = ladoALado ? corte.width * n + gap * (n - 1) : corte.width;
        int h = ladoALado ? corte.height + topo : (corte.height + topo) * n;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(FUNDO);
        g.fillRect(0, 0, w, h);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.setColor(TEXTO);
        for (int i = 0; i < n; i++) {
            BufferedImage parte = ImageIO.read(dirs.get(i).resolve(nome).toFile()).getSubimage(corte.x, corte.y, corte.width, corte.height);
            int x = ladoALado ? i * (corte.width + gap) : 0, y = ladoALado ? 0 : i * (corte.height + topo);
            g.drawString(rotulo(dirs.get(i), atual), x + 16, y + 28);
            g.drawImage(parte, x, y + topo, null);
        }
        g.dispose();
        ImageIO.write(img, "png", saida.toFile());
        System.out.println("gerado: " + saida);
    }

    /**
     * Detalhe ampliado (2,5x) da regiao cabeca / pescoco / gola: uma linha por versao,
     * colunas frente, 3/4, perfil, costas (pose neutra) e combate 3/4.
     */
    static void detalhe(List<Path> dirs, Path atual, Path saida) throws IOException {
        String[] colunas = {"FRENTE", "3/4 FRONTAL", "PERFIL", "COSTAS", "COMBATE 3/4"};
        Rectangle[] cortes = {
            new Rectangle(98, 190, 140, 140), new Rectangle(350, 190, 140, 140), new Rectangle(602, 190, 140, 140),
            new Rectangle(1106, 190, 140, 140), new Rectangle(296, 196, 190, 190)};
        int cel = 300, gap = 16, topo = 70, rot = 36, n = dirs.size();
        int w = colunas.length * (cel + gap) + gap, h = topo + n * (cel + rot + gap);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(FUNDO);
        g.fillRect(0, 0, w, h);
        g.setColor(TEXTO);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString("DETALHE  ·  CABEÇA, PESCOÇO E GOLA (2,5x)", gap, 30);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g.setColor(SUAVE);
        for (int c = 0; c < colunas.length; c++) g.drawString(colunas[c], gap + c * (cel + gap) + 8, topo - 12);
        for (int i = 0; i < n; i++) {
            int y = topo + i * (cel + rot + gap);
            g.setColor(TEXTO);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            g.drawString(rotulo(dirs.get(i), atual), gap, y + 22);
            BufferedImage prancha = ImageIO.read(dirs.get(i).resolve("prancha-cinco-perspectivas.png").toFile());
            BufferedImage combate = ImageIO.read(dirs.get(i).resolve("combate-3-4.png").toFile());
            for (int c = 0; c < colunas.length; c++) {
                Rectangle r = cortes[c];
                BufferedImage parte = (c == 4 ? combate : prancha).getSubimage(r.x, r.y, r.width, r.height);
                g.drawImage(parte, gap + c * (cel + gap), y + rot, cel, cel, null);
            }
        }
        g.dispose();
        ImageIO.write(img, "png", saida.toFile());
        System.out.println("gerado: " + saida);
    }

    static void checar(double v, double esperado, String nome) {
        if (Math.abs(v - esperado) > 1e-6) throw new AssertionError(nome + ": " + v + " != " + esperado);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--verificar-6.1")) {
            verificar();
            verificarLaboratorio().forEach(System.out::println);
            for (Map.Entry<String, double[]> en : continuidadeVisual().entrySet())
                System.out.printf(Locale.ROOT, "continuidade visual (%s): maior diferenca %.3f em %d graus; mediana %.3f%n",
                        en.getKey(), en.getValue()[1], (int) en.getValue()[0], en.getValue()[2]);
            return;
        }
        Path saida = Paths.get(args.length > 0 ? args[0] : ".");
        Files.createDirectories(saida);
        verificar();
        prancha(saida);
        combate(saida);
        estrutura(saida);
        if (args.length > 1) {
            List<Path> anteriores = new ArrayList<>();
            for (int i = 1; i < args.length; i++) anteriores.add(Paths.get(args[i]));
            comparativo(anteriores, saida);
        }
    }
}
