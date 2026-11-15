package com.irmaosmiau.cenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

class CenariosTest {

    private static final int L = 800;
    private static final int A = 450;

    private static BufferedImage desenhar(Cenario c, boolean primeiroPlano) {
        BufferedImage img = new BufferedImage(L, A, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        c.desenharFundo(g, L, A);
        c.desenharChao(g, L, A);
        if (primeiroPlano) {
            c.desenharPrimeiroPlano(g, L, A);
        }
        g.dispose();
        return img;
    }

    // =====================================================
    // REGISTRO E SELECAO
    // =====================================================
    @Test
    void registroPadraoTemDojoEOriginal() {
        RegistroCenarios r = RegistroCenarios.padrao();

        // 5O.4 (pedido): + campo remasterizado e arena do torneio, depois dos dois de sempre.
        assertEquals(List.of("dojo", "original", "campo_remaster", "torneio", "academia", "fortaleza_neve",
                "academia_miau", "campeonato"), r.ids());   // 5P.4 (pedido)
        assertEquals("dojo", RegistroCenarios.ID_PADRAO);
        assertInstanceOf(CenarioDojo.class, r.exigir("DOJO"));
        assertInstanceOf(CenarioOriginal.class, r.exigir(" original "));
    }

    @Test
    void cenarioNovoPodeSerRegistradoSemTocarNoJogo() {
        RegistroCenarios r = RegistroCenarios.padrao();
        Cenario praia = new Cenario() {
            public String getId() { return "praia"; }
            public String getNome() { return "Praia"; }
            public void desenharFundo(Graphics2D g, int l, int a) { g.setColor(Color.CYAN); g.fillRect(0, 0, l, a); }
            public void desenharChao(Graphics2D g, int l, int a) { g.setColor(Color.YELLOW); g.fillRect(0, 300, l, a - 300); }
        };
        r.registrar(praia);

        assertEquals(9, r.quantidade());                    // 5P.4 (pedido): antes 7 (6 + praia)
        assertSame(praia, r.exigir("praia"));
        // primeiro plano e opcional (padrao nao desenha nada)
        BufferedImage img = desenhar(praia, true);
        assertEquals(Color.YELLOW.getRGB(), img.getRGB(10, 440));
    }

    @Test
    void duplicadosEInexistentes() {
        RegistroCenarios r = RegistroCenarios.padrao();

        assertThrows(IllegalArgumentException.class, () -> r.registrar(new CenarioDojo()));
        assertThrows(IllegalArgumentException.class, () -> r.registrar(null));
        assertNull(r.buscar("lua"));
        String msg = assertThrows(IllegalArgumentException.class, () -> r.exigir("lua")).getMessage();
        assertTrue(msg.contains("dojo"), msg);
    }

    @Test
    void proximoPercorreOsCenariosEmCirculo() {
        RegistroCenarios r = RegistroCenarios.padrao();
        Cenario dojo = r.exigir("dojo");
        Cenario original = r.exigir("original");

        assertSame(original, r.proximo(dojo));
        // 5O.4 (pedido): depois do original vem o campo remasterizado, depois a arena; o ciclo volta ao dojo.
        assertSame(r.exigir("campo_remaster"), r.proximo(original));
        // 5P.3 (pedido): depois do torneio vem a academia, depois a fortaleza; o ciclo volta ao dojo.
        assertSame(r.exigir("academia"), r.proximo(r.exigir("torneio")));
        assertSame(r.exigir("fortaleza_neve"), r.proximo(r.exigir("academia")));
        // 5P.4 (pedido): depois da fortaleza vem a academia dos Irmaos Miau e o campeonato.
        assertSame(r.exigir("academia_miau"), r.proximo(r.exigir("fortaleza_neve")));
        assertSame(r.exigir("campeonato"), r.proximo(r.exigir("academia_miau")));
        assertSame(dojo, r.proximo(r.exigir("campeonato")));
        assertSame(dojo, r.proximo(null));
    }

    // =====================================================
    // COMPATIBILIDADE DO CENARIO ORIGINAL
    // =====================================================
    @Test
    void cenarioOriginalReproduzOCeuEAGramaAntigos() {
        BufferedImage img = desenhar(new CenarioOriginal(), true);

        assertEquals(new Color(135, 206, 235).getRGB(), img.getRGB(400, 100));
        assertEquals(new Color(135, 206, 235).getRGB(), img.getRGB(5, 299));
        assertEquals(new Color(80, 180, 80).getRGB(), img.getRGB(5, 300));
        assertEquals(new Color(80, 180, 80).getRGB(), img.getRGB(795, 449));
    }

    // =====================================================
    // DOJO
    // =====================================================
    @Test
    void dojoTemUmaFaixaDeTatamePorLinhaDeMovimentacao() {
        BufferedImage img = desenhar(new CenarioDojo(), false);

        // Pes dos lutadores: y = 326 + 20 * linha (medido nos renderers atuais).
        for (int linha = 0; linha < 5; linha++) {
            int y = CenarioDojo.yCentroLinha(linha);
            assertEquals(326 + 20 * linha, y);
            int rgb = img.getRGB(37, y);   // fora das emendas
            Color c = new Color(rgb, true);
            assertTrue(c.getBlue() > c.getRed(), "linha " + (linha + 1) + " deveria ser tatame: " + c);
        }
        // Linhas vizinhas tem tons alternados
        assertNotEquals(img.getRGB(37, CenarioDojo.yCentroLinha(0)), img.getRGB(37, CenarioDojo.yCentroLinha(1)));
        // Borda vermelha na frente e parede clara no alto
        Color frente = new Color(img.getRGB(37, 430), true);
        assertTrue(frente.getRed() > frente.getBlue(), "borda vermelha: " + frente);
        Color parede = new Color(img.getRGB(5, 20), true);
        assertTrue(parede.getRed() > 200 && parede.getGreen() > 190, "parede clara: " + parede);
    }

    @Test
    void primeiroPlanoDoDojoNaoCobreAreaDosPersonagens() {
        CenarioDojo dojo = new CenarioDojo();
        BufferedImage semFrente = desenhar(dojo, false);
        BufferedImage comFrente = desenhar(dojo, true);

        // Personagens ocupam ate y ~ 406 (pes da linha 5): nada muda ate la.
        for (int y = 0; y <= 410; y += 2) {
            for (int x = 0; x < L; x += 7) {
                assertEquals(semFrente.getRGB(x, y), comFrente.getRGB(x, y), "x=" + x + " y=" + y);
            }
        }
    }

    @Test
    void dojoReaproveitaAImagemEntreFrames() {
        CenarioDojo dojo = new CenarioDojo();
        BufferedImage a = desenhar(dojo, true);
        BufferedImage b = desenhar(dojo, true);
        for (int y = 0; y < A; y += 9) {
            for (int x = 0; x < L; x += 9) {
                assertEquals(a.getRGB(x, y), b.getRGB(x, y));
            }
        }
    }
}
