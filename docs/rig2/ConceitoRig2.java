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
     * 6.0.3: 33,5, metade da exposicao de pescoco da 6.0.2.
     */
    static final double CABECA_Y = 33.5, PESCOCO_Y = 76;

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
        double inclinacao, baixar, cabecaZ = 2;
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

    static void montar(Corpo c, V[] maos, V[] polosB, V[] pes, V[] polosP) {
        c.pelve = c.transformar(new V(0, 165, 0));
        c.pescoco = c.transformar(new V(0, PESCOCO_Y, 0));
        c.cabeca = c.transformar(new V(0, CABECA_Y, c.cabecaZ));
        for (int i = 0; i < 2; i++) {
            double s = i == 0 ? -1 : 1;
            c.ombro[i] = c.transformar(new V(s * 34, 90, 0));
            V[] b = ik(c.ombro[i], maos[i], BRACO, ANTEBRACO, polosB[i]);
            c.cotovelo[i] = b[0];
            c.mao[i] = b[1];
            c.quadril[i] = c.transformar(new V(s * 16, 170, 0));
            V[] p = ik(c.quadril[i], pes[i], COXA, CANELA, polosP[i]);
            c.joelho[i] = p[0];
            c.tornozelo[i] = p[1];
        }
    }

    static Corpo neutro() {
        Corpo c = new Corpo();
        montar(c,
                new V[] {new V(-46, 152, 4), new V(46, 152, 4)},
                new V[] {new V(-1, .2, -.8), new V(1, .2, -.8)},
                new V[] {new V(-19, 258, -3), new V(19, 258, 3)},
                new V[] {new V(0, 0, 1), new V(0, 0, 1)});
        return c;
    }

    /** Base de combate: quadril baixo, tronco 10 graus a frente, guarda alta, pe esquerdo a frente. */
    static Corpo combate() {
        Corpo c = new Corpo();
        c.inclinacao = 8;
        c.baixar = 16;
        c.cabecaZ = -3;
        c.guinadaPe[0] = -28;
        c.guinadaPe[1] = 8;
        montar(c,
                new V[] {new V(-4, 118, 46), new V(22, 122, 64)},
                new V[] {new V(-.6, 1, -.4), new V(.5, 1, -.3)},
                new V[] {new V(-26, 258, -22), new V(22, 258, 26)},
                new V[] {new V(-.3, 0, 1), new V(.2, 0, 1)});
        return c;
    }

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
            if (longe >= 0) braco(longe, true);
            pernas(perto);
            casaco();
            faixa();
            cabeca(c.cabeca, guinadaCabeca(cam.graus));
            if (perto >= 0) braco(perto, false);
            else { braco(0, false); braco(1, false); }
        }

        /**
         * "Trapaca" de camera classica da animacao 2D: entre a frente e o perfil o rosto gira 75% do
         * angulo do corpo, para manter os dois olhos legiveis no 3/4 de combate (corpo 40 -> rosto 30).
         */
        static double guinadaCabeca(double g) {
            // 6.0.2: curva suave (sem salto em 90 graus); continua dando exatamente 30 para o corpo a 40.
            return g > 0 && g < 90 ? g - 10 / Math.sin(Math.toRadians(80)) * Math.sin(Math.toRadians(2 * g)) : g;
        }

        void sombraChao() {
            double x0 = Math.min(cam.p(c.tornozelo[0]).getX(), cam.p(c.tornozelo[1]).getX());
            double x1 = Math.max(cam.p(c.tornozelo[0]).getX(), cam.p(c.tornozelo[1]).getX());
            double meio = (x0 + x1) / 2, larg = Math.max(70, x1 - x0 + 46);
            p.preencher(new Ellipse2D.Double(meio - larg / 2, CHAO - 4, larg, 10), new Color(12, 14, 18, 120));
        }

        // ---------- roupa: luz de cima/esquerda, igual em todas as vistas
        void sombrear(Area a, Color base, Color borda, double w) {
            p.preencher(a, base);
            Area luz = new Area(a);
            luz.subtract(a.createTransformedArea(AffineTransform.getTranslateInstance(2.2, 2.6)));
            p.preencher(luz, base == GI_LONGE ? new Color(34, 36, 41) : GI_LUZ);
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
            if (lm < .5) return; // membro quase reto (dobra < ~30 graus): sem vinco
            mx /= lm; my /= lm;
            // um unico vinco curto, perpendicular a bissetriz, no lado interno da dobra
            double ox = j.getX() + mx * largura * .28, oy = j.getY() + my * largura * .28, px = -my, py = mx;
            Path2D v = new Path2D.Double();
            v.moveTo(ox - px * 5, oy - py * 5);
            v.quadTo(ox - mx * 2.5, oy - my * 2.5, ox + px * 5, oy + py * 5);
            p.tracar(v, cor, .9);
        }

        void braco(int i, boolean longe) {
            Point2D s = cam.p(c.ombro[i]), e = cam.p(c.cotovelo[i]), w = cam.p(c.mao[i]);
            Area manga = tubo(s, e, w, 27, 24, 24, 13);
            Color borda = longe ? BORDA_LONGE : BORDA;
            sombrear(manga, longe ? GI_LONGE : GI, borda, 1.2);
            dobra(s, e, w, 27, borda);
            // punho da manga (barra do kimono)
            double dx = w.getX() - e.getX(), dy = w.getY() - e.getY(), l = Math.max(.01, Math.hypot(dx, dy));
            double ux = dx / l, uy = dy / l, bx = w.getX() - ux * 6, by = w.getY() - uy * 6;
            p.linha(borda, 1, bx - uy * 12, by + ux * 12, bx + uy * 12, by - ux * 12);
            pata(w, Math.atan2(dy, dx), longe);
        }

        void pata(Point2D punho, double ang, boolean longe) {
            p.salvar();
            p.mover(punho.getX(), punho.getY());
            p.girar(ang);
            p.mover(6, 0);
            p.escalar(1.25);
            Path2D m = new Path2D.Double();
            m.moveTo(-7, -6); m.quadTo(-9, -10, -3, -10); m.lineTo(5, -8); m.quadTo(11, -7, 11, 0);
            m.quadTo(11, 8, 4, 9); m.lineTo(-4, 8); m.quadTo(-10, 6, -8, 1); m.quadTo(-14, -1, -10, -5); m.closePath();
            p.forma(m, longe ? PELO_LONGE : PELO, PELO_BORDA, .9);
            p.linha(PELO_BORDA, .75, 6, -5, 9, -5);
            p.linha(PELO_BORDA, .75, 7, 0, 10, 0);
            p.linha(PELO_BORDA, .75, 6, 5, 9, 5);
            p.restaurar();
        }

        void pe(int i, boolean longe) {
            double a = Math.toRadians(c.guinadaPe[i]);
            V d = new V(Math.sin(a), 0, Math.cos(a));
            V t = c.tornozelo[i];
            Point2D centro = cam.p(t.mais(new V(0, 7, 0)).mais(d.vezes(6)));
            double rel = Math.toRadians(cam.graus) + a;
            double rx = Math.hypot(10.5 * Math.cos(rel), 15 * Math.sin(rel)), ry = 7;
            p.forma(new Ellipse2D.Double(centro.getX() - rx, centro.getY() - ry, 2 * rx, 2 * ry),
                    longe ? PELO_LONGE : PELO, PELO_BORDA, .9);
            if (Math.cos(rel) > -.3) {
                V lado = new V(Math.cos(a), 0, -Math.sin(a));
                for (int k = -1; k <= 1; k += 2) {
                    Point2D q = cam.p(t.mais(new V(0, 7, 0)).mais(d.vezes(17 + 3 * k * Math.signum(Math.sin(rel) + 1e-9)))
                            .mais(lado.vezes(4 * k)));
                    p.linha(PELO_BORDA, .8, q.getX(), q.getY() + 1, q.getX(), q.getY() + 5);
                }
            }
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
                pe(0, false); pe(1, false);
                Area a = perna(0); a.add(perna(1)); a.add(quadris());
                sombrear(a, GI, BORDA, 1.2);
                for (int i = 0; i < 2; i++) dobraJoelho(i, BORDA);
                return;
            }
            int longe = 1 - perto;
            pe(longe, true);
            sombrear(perna(longe), GI_LONGE, BORDA_LONGE, 1.2);
            dobraJoelho(longe, BORDA_LONGE);
            pe(perto, false);
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
            if (vis(184, -8) > .05) {
                fenda.add(sp(170, -16, .4)); fenda.add(sp(196, -15, .4)); fenda.add(sp(196, 2, .4));
                p.forma(poligono(fenda), GI_SOMBRA, BORDA, .8);
            }
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
            if (frente < -.08) return; // no (amarracao) fica escondido nas vistas de costas
            double fs = Math.max(.42, frente);
            Point2D k = sp(155.5, 0, 4.5);
            p.salvar();
            p.mover(k.getX(), k.getY());
            p.escalarXY(fs, 1);
            p.forma(pts(-3.5, -1, 4.5, 1, -5.5, 30, -14.5, 27), FAIXA, FAIXA_BORDA, 1);
            p.forma(pts(.5, 0, 9.5, -1, 22.5, 22, 13.5, 27), new Color(20, 20, 22), FAIXA_BORDA, 1);
            p.preencher(pts(10.5, 17, 18.5, 13, 22.5, 22, 13.5, 27), PONTEIRA);
            p.forma(pts(-9.5, -4, .5, -7, 10.5, -3, 7.5, 5, -3.5, 7, -10.5, 2), FAIXA, FAIXA_BORDA, 1.2);
            p.linha(FAIXA_LUZ, 1, -5.5, -3, 1.5, -4, 6.5, -1);
            p.restaurar();
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
            sombra.intersect(silhuetaCabeca(topo, guinadaCabeca(cam.graus))
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

            boolean rosto = cs > -.05;
            // contorno do focinho no 3/4: o focinho encobre a bochecha mais distante
            if (rosto && Math.abs(sn) > .25 && cs > .1) {
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
            }
            // 6.0.3: o traco interno da bochecha no perfil foi removido; o focinho e definido pela silhueta.
            // olhos sobre a esfera: aproximam-se e estreitam conforme giram; no perfil recuam da testa
            for (int s = -1; s <= 1; s += 2) {
                double lon = 18 * s, k = encara.applyAsDouble(lon);
                if (k <= .12) continue;
                double la = Math.toRadians(lon), lt = Math.toRadians(-3);
                Point2D o = hp.apply(new V(RAIO_CABECA * Math.cos(lt) * Math.sin(la), RAIO_CABECA * Math.sin(lt),
                        RAIO_CABECA * Math.cos(lt) * Math.cos(la)));
                o = lerp(o, pp.apply(25.0, -4.0), w);
                double larg = (1 - w) * 3.1 * Math.max(.42, k) + w * 2.5, alt = (1 - w) * 3.2 + w * 3.1;
                p.preencher(new Ellipse2D.Double(o.getX() - larg, o.getY() - alt, 2 * larg, 2 * alt), OLHO);
            }
            if (rosto) {
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
            p.preencher(concha, longe && w > .5 ? PELO_LONGE : PELO);
            double k = encara.applyAsDouble(lon);
            if (k > .15) { // parte interna so aparece quando a orelha encara a camera
                Point2D m = lerp(lerp(a, c2, .5), b, .25);
                p.preencher(poligono(List.of(lerp(m, a, .55), lerp(m, b, .62), lerp(m, c2, .55))), ORELHA_INTERNA);
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
            p.texto("IRMÃO MIAU BRANCO  ·  RIG 2.0  ·  CINCO PERSPECTIVAS  ·  REFINADO 6.0.3", 40, 42, 20, TEXTO, true, -1);
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

    /** Rotulos das versoes comparadas, na ordem dos argumentos (a ultima e a gerada agora). */
    static final String[] ROTULOS = {"6.0.1  ·  original", "6.0.2  ·  perfil e pescoço", "6.0.3  ·  pescoço mais discreto, perfil sem traço"};

    /** Comparativo entre versoes a partir dos PNG (as previas anteriores nao sao regeradas). */
    static void comparativo(List<Path> anteriores, Path atual) throws IOException {
        List<Path> todas = new ArrayList<>(anteriores);
        todas.add(atual);
        compor(todas, "prancha-cinco-perspectivas.png", new Rectangle(40, 80, 1320, 500), false, atual.resolve("comparativo-prancha.png"));
        compor(todas, "combate-3-4.png", new Rectangle(20, 90, 720, 600), true, atual.resolve("comparativo-combate.png"));
    }

    static void compor(List<Path> dirs, String nome, Rectangle corte, boolean ladoALado, Path saida) throws IOException {
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
            String rotulo = ROTULOS[Math.max(0, ROTULOS.length - n + i)];
            g.drawString(rotulo, x + 16, y + 28);
            g.drawImage(parte, x, y + topo, null);
        }
        g.dispose();
        ImageIO.write(img, "png", saida.toFile());
        System.out.println("gerado: " + saida);
    }

    static void checar(double v, double esperado, String nome) {
        if (Math.abs(v - esperado) > 1e-6) throw new AssertionError(nome + ": " + v + " != " + esperado);
    }

    public static void main(String[] args) throws Exception {
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
