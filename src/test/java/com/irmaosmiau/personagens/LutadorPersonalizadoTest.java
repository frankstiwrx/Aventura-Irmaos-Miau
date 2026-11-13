package com.irmaosmiau.personagens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import org.junit.jupiter.api.Test;

/**
 * Entrega 4D: instancias de Branco/Preto com atributos personalizados.
 */
class LutadorPersonalizadoTest {

    /** Branco com folego 60, condicionamento 50, velocidade 100, recuperacao 90. */
    private static final AtributosLutador CUSTOM = new AtributosLutador(50, 60, 70, 100, 30, 40, 90, 95);

    private static void assertMesmosValores(AtributosLutador e, AtributosLutador a) {
        assertTrue(PerfilLutador.mesmosValores(e, a), "atributos diferentes");
    }

    // =====================================================
    // CONSTRUTORES
    // =====================================================
    @Test
    void construtoresAntigosContinuamComOsValoresOficiais() {
        IrmaoMiau b = new IrmaoMiauBranco(10, 150, 0.7);
        IrmaoMiau p = new IrmaoMiauPreto(10, 150, 0.7);

        assertMesmosValores(new AtributosLutador(35, 25, 42, 22, 25, 25, 22, 72), b.getAtributos());
        assertMesmosValores(new AtributosLutador(25, 40, 18, 45, 45, 28, 38, 68), p.getAtributos());
        assertMesmosValores(IrmaoMiauBranco.atributosOficiais(), b.getAtributos());
        assertMesmosValores(IrmaoMiauPreto.atributosOficiais(), p.getAtributos());
        assertEquals("Irmão Miau Branco", b.getNome());
        assertEquals("Irmão Miau Preto", p.getNome());
        assertEquals(25.0, b.getFolegoAtual());
        assertEquals(40.0, p.getFolegoAtual());
    }

    @Test
    void construtorComAtributosUsaOsValoresDoPerfil() {
        IrmaoMiau b = new IrmaoMiauBranco(10, 150, 0.7, CUSTOM, "Branco Turbo");

        assertMesmosValores(CUSTOM, b.getAtributos());
        assertEquals("Branco Turbo", b.getNome());
        assertEquals("Jiu-Jitsu", b.getEstilo());
        assertEquals(60.0, b.getFolegoAtual(), "folego maximo = perfil");
        assertEquals(50.0, b.getCondicionamentoAtual(), "condicionamento maximo = perfil");
        assertEquals(1.0, b.getPercentualFolego());

        IrmaoMiau semNome = new IrmaoMiauPreto(10, 150, 0.7, CUSTOM);
        assertEquals("Irmão Miau Preto", semNome.getNome());
        assertThrows(IllegalArgumentException.class, () -> new IrmaoMiauBranco(0, 0, 0.7, null));
    }

    @Test
    void cadaInstanciaRecebeUmaCopiaIndependente() {
        IrmaoMiau a = new IrmaoMiauBranco(0, 150, 0.7, CUSTOM, "A");
        IrmaoMiau b = new IrmaoMiauBranco(0, 150, 0.7, CUSTOM, "B");

        assertNotSame(CUSTOM, a.getAtributos());
        assertNotSame(a.getAtributos(), b.getAtributos());

        a.consumirFolegoCombate(20);
        assertEquals(40.0, a.getFolegoAtual());
        assertEquals(60.0, b.getFolegoAtual(), "gastar num nao afeta o outro");
        assertEquals(25.0, new IrmaoMiauBranco(0, 150, 0.7).getFolegoAtual(), "oficial intacto");
    }

    // =====================================================
    // ATRIBUTOS USADOS PELAS MECANICAS ATUAIS
    // =====================================================
    @Test
    void velocidadeDoPerfilMudaAVelocidadeDeMovimento() {
        IrmaoMiau oficial = new IrmaoMiauBranco(0, 150, 0.7);
        IrmaoMiau rapido = new IrmaoMiauBranco(0, 150, 0.7, CUSTOM, "Rapido");

        // Formula existente (inalterada): 3.2 + velocidade / 100 * 1.6
        assertEquals(3.2 + 22 / 100.0 * 1.6, oficial.getVelocidadeBase(), 1e-9);
        assertEquals(3.2 + 100 / 100.0 * 1.6, rapido.getVelocidadeBase(), 1e-9);

        for (int i = 0; i < 30; i++) {
            oficial.moverX(oficial.getVelocidadeBase());
            rapido.moverX(rapido.getVelocidadeBase());
        }
        assertTrue(rapido.getX() > oficial.getX() + 30, "perfil rapido anda mais");
    }

    @Test
    void recuperacaoDoPerfilMudaARecuperacaoDeFolego() {
        AtributosLutador lento = new AtributosLutador(35, 25, 42, 22, 25, 25, 1, 72);
        AtributosLutador rapido = new AtributosLutador(35, 25, 42, 22, 25, 25, 100, 72);
        IrmaoMiau a = new IrmaoMiauBranco(0, 150, 0.7, lento, "Lento");
        IrmaoMiau b = new IrmaoMiauBranco(0, 150, 0.7, rapido, "Rapido");
        a.consumirFolegoCombate(20);
        b.consumirFolegoCombate(20);

        for (int i = 0; i < 60; i++) {
            a.atualizar(false, false);
            b.atualizar(false, false);
        }
        assertTrue(b.getFolegoAtual() > a.getFolegoAtual(),
                "recuperacao 100 recupera mais que 1: " + b.getFolegoAtual() + " x " + a.getFolegoAtual());
        assertTrue(b.getFolegoAtual() <= 25.0, "nunca passa do maximo do perfil");
    }

    @Test
    void atributosAindaSemEfeitoFicamAssociadosAoLutador() {
        IrmaoMiau b = new IrmaoMiauBranco(0, 150, 0.7, CUSTOM, "X");
        assertEquals(70.0, b.getAtributos().getForca());
        assertEquals(30.0, b.getAtributos().getAgilidade());
        assertEquals(40.0, b.getAtributos().getTecnica());
        assertEquals(95.0, b.getAtributos().getPeso());
    }

    // =====================================================
    // DEFINICAO E ESCOLHA
    // =====================================================
    @Test
    void escolhaOficialEPerfilCriamAInstanciaCerta() {
        CatalogoPersonagens c = CatalogoPersonagens.padrao();
        DefinicaoPersonagem branco = c.exigir("miau_branco");
        assertTrue(branco.aceitaPerfis());

        EscolhaLutador oficial = EscolhaLutador.oficial(branco);
        IrmaoMiau o = oficial.criarLutador(5, 150, 0.7);
        assertInstanceOf(IrmaoMiauBranco.class, o);
        assertMesmosValores(branco.getAtributosPadrao(), o.getAtributos());
        assertTrue(oficial.isOficial());
        assertEquals("Irmão Miau Branco — oficial", oficial.getDescricao());

        PerfilLutador perfil = new PerfilLutador("branco_turbo", "Branco Turbo", "miau_branco", CUSTOM);
        EscolhaLutador esc = EscolhaLutador.perfil(perfil, branco);
        IrmaoMiau p = esc.criarLutador(5, 150, 0.7);
        assertInstanceOf(IrmaoMiauBranco.class, p, "mesmo renderer/animacoes do Branco");
        assertMesmosValores(CUSTOM, p.getAtributos());
        assertEquals("Branco Turbo", p.getNome());
        assertFalse(esc.isOficial());
        assertEquals("perfil:branco_turbo", esc.getId());
        assertTrue(esc.getDescricao().contains("personalizado (base Irmão Miau Branco)"));

        // Oficial intacto depois de tudo
        assertMesmosValores(IrmaoMiauBranco.atributosOficiais(), c.exigir("miau_branco").getAtributosPadrao());
        assertMesmosValores(IrmaoMiauBranco.atributosOficiais(), new IrmaoMiauBranco(0, 0, 0.7).getAtributos());
    }

    @Test
    void perfilDeOutraBaseNaoPodeSerUsadoNoSlotErrado() {
        CatalogoPersonagens c = CatalogoPersonagens.padrao();
        PerfilLutador perfilPreto = new PerfilLutador("preto_x", "Preto X", "miau_preto", CUSTOM);
        assertThrows(IllegalArgumentException.class, () -> EscolhaLutador.perfil(perfilPreto, c.exigir("miau_branco")));

        DefinicaoPersonagem semPerfis = new DefinicaoPersonagem("gato", "Gato", "Judo", null,
                CUSTOM, new ReferenciasVisuais(java.awt.Color.RED, java.awt.Color.RED, java.awt.Color.RED, null),
                IrmaoMiauBranco::new);
        assertFalse(semPerfis.aceitaPerfis());
        assertThrows(IllegalStateException.class,
                () -> semPerfis.criarLutadorPersonalizado(0, 0, 0.7, CUSTOM, "x"));
    }
}
