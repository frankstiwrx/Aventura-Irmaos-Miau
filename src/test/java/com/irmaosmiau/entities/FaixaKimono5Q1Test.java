package com.irmaosmiau.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.posicional.CatalogoFinalizacoes;
import com.irmaosmiau.posicional.EstadoPosicional;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo;
import com.irmaosmiau.regras.EvolucaoGraduacao;
import com.irmaosmiau.regras.Graduacao;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.1: graduacao como fonte unica (atributos, faixa, tecnicas) e o kimono escolhido. */
class FaixaKimono5Q1Test {

    private static EscolhaLutador oficial(String id) {
        return EscolhaLutador.oficial(CatalogoPersonagens.padrao().exigir(id));
    }

    @Test
    void padraoEFaixaBrancaComOsAtributosOriginaisEPadroesDeKimono() {
        IrmaoMiau b = new IrmaoMiauBranco(100, 150, 0.7);
        IrmaoMiau p = new IrmaoMiauPreto(100, 150, 0.7);
        assertEquals(Graduacao.BRANCA, b.getGraduacao());
        assertEquals(IrmaoMiauBranco.atributosOficiais().getForca(), b.getAtributos().getForca(), 1e-9);
        assertEquals(CorKimono.PRETO, b.getCorKimono(), "padrao real do Branco: kimono preto");
        assertEquals(CorKimono.BRANCO, p.getCorKimono(), "padrao real do Preto: kimono branco");
    }

    @Test
    void graduacaoMudaAtributosEPretaParaBrancaRestaura() {
        IrmaoMiau b = new IrmaoMiauBranco(100, 150, 0.7);
        b.setGraduacao(Graduacao.PRETA);
        assertEquals(78, b.getAtributos().getForca(), 1e-9);
        assertEquals(47, b.getFolegoAtual(), 1e-9, "comeca com o novo maximo");
        assertEquals(72, b.getAtributos().getPeso(), 1e-9);
        b.setGraduacao(Graduacao.PRETA);
        assertEquals(78, b.getAtributos().getForca(), 1e-9, "repetir nao acumula");
        b.setGraduacao(Graduacao.BRANCA);
        assertEquals(42, b.getAtributos().getForca(), 1e-9, "voltar a branca restaura");
        assertEquals(25, b.getAtributos().getFolego(), 1e-9);
    }

    @Test
    void cincoCoresDeFaixaDesenhadasETresKimonosNosDoisPersonagens() {
        Set<Integer> cores = new HashSet<>();
        IrmaoMiau b = new IrmaoMiauBranco(100, 150, 0.7);
        for (Graduacao g : Graduacao.values()) {
            b.setGraduacao(g);
            cores.add(b.getCorFaixa().getRGB());
        }
        assertEquals(5, cores.size(), "cinco cores diferentes");
        for (IrmaoMiau l : new IrmaoMiau[] {new IrmaoMiauBranco(100, 100, 0.7), new IrmaoMiauPreto(100, 100, 0.7)}) {
            for (CorKimono k : CorKimono.values()) {
                l.setCorKimono(k);
                assertEquals(k, l.getCorKimono());
            }
            l.setCorKimono(null);
            assertEquals(l.getCorKimonoPadrao(), l.getCorKimono(), "null volta ao padrao");
        }
        // a cor da faixa aparece de fato no desenho (e acompanha a mesma camada do corpo)
        b.setGraduacao(Graduacao.ROXA);
        b.setCorKimono(CorKimono.AZUL);
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        b.desenhar(g);
        g.dispose();
        boolean roxa = false;
        boolean azulKimono = false;
        for (int y = 0; y < 400; y++) {
            for (int x = 0; x < 400; x++) {
                int c = img.getRGB(x, y) & 0xFFFFFF;
                roxa |= c == (b.getCorFaixa().getRGB() & 0xFFFFFF);
                azulKimono |= c == (CorKimono.AZUL.getCor().getRGB() & 0xFFFFFF);
            }
        }
        assertTrue(roxa, "faixa roxa desenhada");
        assertTrue(azulKimono, "kimono azul desenhado");
    }

    @Test
    void escolhaIndependentePorSlotEReinicioSemBonusDuplo() {
        EscolhaLutador j1 = oficial(CatalogoPersonagens.ID_BRANCO).com(Graduacao.AZUL, CorKimono.BRANCO);
        EscolhaLutador j2 = oficial(CatalogoPersonagens.ID_BRANCO).com(Graduacao.MARROM, CorKimono.AZUL);
        IrmaoMiau a = j1.criarLutador(100, 150, 0.7);
        IrmaoMiau b = j2.criarLutador(300, 150, 0.7);
        assertEquals(Graduacao.AZUL, a.getGraduacao());
        assertEquals(Graduacao.MARROM, b.getGraduacao());
        assertEquals(CorKimono.BRANCO, a.getCorKimono());
        assertEquals(CorKimono.AZUL, b.getCorKimono());
        assertNotEquals(a.getAtributos().getForca(), b.getAtributos().getForca(), "dois Brancos diferentes");
        // reiniciar = criar de novo pela mesma escolha
        IrmaoMiau a2 = j1.criarLutador(100, 150, 0.7);
        assertEquals(a.getAtributos().getForca(), a2.getAtributos().getForca(), 1e-9, "sem bonus duplicado");
        assertEquals(EvolucaoGraduacao.aplicar(j1.getAtributosBase(), Graduacao.AZUL).getForca(),
                j1.getAtributos().getForca(), 1e-9, "a selecao mostra os atributos calculados");
    }

    @Test
    void faixaEscolhidaRespeitaAsRestricoesDa5Q() {
        IrmaoMiau a = new IrmaoMiauBranco(300, 150, 0.7);
        IrmaoMiau b = new IrmaoMiauPreto(380, 150, 0.7);
        MotorSolo m = new MotorSolo(a, b, null, 15, 715);
        m.forcarPosicao(Posicao.GUARDA, Lado.B, Lado.A);
        TecnicaSolo.ComandoSolo omoplata = new TecnicaSolo.ComandoSolo(TecnicaSolo.Direcao.BAIXO, TecnicaSolo.Familia.L1);
        var f = CatalogoFinalizacoes.referencia().para(EstadoPosicional.noChao(Posicao.GUARDA, Lado.B, Lado.A), Lado.A,
                omoplata).orElseThrow();
        assertTrue(m.motivoFinalizacao(Lado.A, f).contains("GRADUAÇÃO"), "branca: omoplata (azul) bloqueada");
        a.setGraduacao(Graduacao.AZUL);
        assertEquals(null, m.motivoFinalizacao(Lado.A, f), "azul libera");
        a.esquecerTecnica(f.tecnicaBase());
        assertEquals("NÃO APRENDIDA", m.motivoFinalizacao(Lado.A, f), "graduacao nao apaga o aprendizado");
    }
}
