package com.irmaosmiau.personagens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import org.junit.jupiter.api.Test;

class CatalogoPersonagensTest {

    private static void assertMesmosAtributos(AtributosLutador e, AtributosLutador a, String quem) {
        assertEquals(e.getCondicionamento(), a.getCondicionamento(), quem + " condicionamento");
        assertEquals(e.getFolego(), a.getFolego(), quem + " folego");
        assertEquals(e.getForca(), a.getForca(), quem + " forca");
        assertEquals(e.getVelocidade(), a.getVelocidade(), quem + " velocidade");
        assertEquals(e.getAgilidade(), a.getAgilidade(), quem + " agilidade");
        assertEquals(e.getTecnica(), a.getTecnica(), quem + " tecnica");
        assertEquals(e.getRecuperacao(), a.getRecuperacao(), quem + " recuperacao");
        assertEquals(e.getPeso(), a.getPeso(), quem + " peso");
    }

    @Test
    void catalogoPadraoTemBrancoEPretoNaOrdem() {
        CatalogoPersonagens c = CatalogoPersonagens.padrao();

        assertEquals(2, c.quantidade());
        assertEquals(List.of(CatalogoPersonagens.ID_BRANCO, CatalogoPersonagens.ID_PRETO),
                c.listar().stream().map(DefinicaoPersonagem::getId).toList());
    }

    @Test
    void definicoesBatemComOsPersonagensReais() {
        CatalogoPersonagens c = CatalogoPersonagens.padrao();

        for (DefinicaoPersonagem d : c.listar()) {
            IrmaoMiau real = d.criarLutador(0, 150, 0.7);
            assertEquals(real.getNome(), d.getNome(), d.getId());
            assertEquals(real.getEstilo(), d.getEstilo(), d.getId());
            assertMesmosAtributos(real.getAtributos(), d.getAtributosPadrao(), d.getId());
        }

        assertEquals(Color.WHITE, c.exigir("miau_branco").getVisual().getCorPelo());
        assertEquals(Color.BLACK, c.exigir("miau_preto").getVisual().getCorPelo());
        assertEquals("/assets/sprites/miau-branco.png", c.exigir("miau_branco").getVisual().getCaminhoRetrato());
    }

    @Test
    void fabricasCriamAsClassesAtuaisNaPosicaoPedida() {
        CatalogoPersonagens c = CatalogoPersonagens.padrao();

        IrmaoMiau b = c.exigir("MIAU_BRANCO").criarLutador(220, 150, 0.7);
        IrmaoMiau p = c.exigir(" miau_preto ").criarLutador(500, 150, 0.7);

        assertInstanceOf(IrmaoMiauBranco.class, b);
        assertInstanceOf(IrmaoMiauPreto.class, p);
        assertEquals(220, b.getX());
        assertEquals(500, p.getX());
        assertEquals(0.7, p.getEscala());
    }

    @Test
    void personagemFicticioPodeSerRegistradoELutar() {
        CatalogoPersonagens c = CatalogoPersonagens.padrao();
        AtributosLutador atributos = new AtributosLutador(30, 30, 30, 30, 30, 30, 30, 70);

        // Nenhuma classe nova no codigo principal: so uma definicao + fabrica.
        c.registrar(new DefinicaoPersonagem(
                "gato_teste", "Gato de Teste", "Judo", null, atributos,
                new ReferenciasVisuais(Color.ORANGE, Color.BLUE, Color.BLACK, null),
                (x, y, escala) -> new IrmaoMiau(x, y, escala, Color.ORANGE, Color.BLUE,
                        Color.BLACK, "Gato de Teste", "Judo", atributos) {
                    @Override
                    public void desenhar(Graphics2D g2) {
                        // renderer ficticio
                    }
                }));

        assertEquals(3, c.quantidade());
        DefinicaoPersonagem d = c.exigir("gato_teste");
        assertNull(d.getDescricao());
        assertNull(d.getVisual().getCaminhoRetrato());

        IrmaoMiau gato = d.criarLutador(413, 150, 0.7);
        gato.setViradoParaDireita(true);
        IrmaoMiau preto = c.exigir("miau_preto").criarLutador(500, 150, 0.7);
        preto.setViradoParaDireita(false);

        // O motor aceita o personagem novo sem nenhuma alteracao.
        SistemaCombate s = new SistemaCombate(gato, preto);
        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(gato, preto, "pegada_1"));
        assertTrue(gato.isPoseCombateExternaAtiva());
        assertSame(gato, s.getAplicadorInteracao());
    }

    @Test
    void registrosDuplicadosEInexistentes() {
        CatalogoPersonagens c = CatalogoPersonagens.padrao();
        DefinicaoPersonagem branco = c.exigir("miau_branco");

        assertThrows(IllegalArgumentException.class, () -> c.registrar(branco));
        assertThrows(IllegalArgumentException.class, () -> c.registrar(null));
        assertNull(c.buscar("nao_existe"));
        assertNull(c.buscar(null));
        String msg = assertThrows(IllegalArgumentException.class, () -> c.exigir("nao_existe")).getMessage();
        assertTrue(msg.contains("miau_branco"), msg);
        assertThrows(UnsupportedOperationException.class, () -> c.listar().clear());
    }

    @Test
    void definicaoValidaCamposObrigatorios() {
        AtributosLutador a = new AtributosLutador(1, 1, 1, 1, 1, 1, 1, 1);
        ReferenciasVisuais v = new ReferenciasVisuais(Color.RED, Color.RED, Color.RED, null);
        FabricaLutador f = IrmaoMiauBranco::new;

        assertThrows(IllegalArgumentException.class, () -> new DefinicaoPersonagem(" ", "N", "E", null, a, v, f));
        assertThrows(IllegalArgumentException.class, () -> new DefinicaoPersonagem("i", null, "E", null, a, v, f));
        assertThrows(IllegalArgumentException.class, () -> new DefinicaoPersonagem("i", "N", "", null, a, v, f));
        assertThrows(IllegalArgumentException.class, () -> new DefinicaoPersonagem("i", "N", "E", null, null, v, f));
        assertThrows(IllegalArgumentException.class, () -> new DefinicaoPersonagem("i", "N", "E", null, a, null, f));
        assertThrows(IllegalArgumentException.class, () -> new DefinicaoPersonagem("i", "N", "E", null, a, v, null));
        assertThrows(IllegalArgumentException.class, () -> new ReferenciasVisuais(null, Color.RED, Color.RED, null));

        DefinicaoPersonagem quebrada = new DefinicaoPersonagem("i", "N", "E", null, a, v, (x, y, e) -> null);
        assertThrows(IllegalStateException.class, () -> quebrada.criarLutador(0, 0, 1));
    }
}
