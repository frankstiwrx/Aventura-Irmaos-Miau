package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.AnimacoesTecnicas;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TransicaoPostura;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.Resultado;
import com.irmaosmiau.posicional.MotorSolo.TipoEfeitoVisual;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.5 no MotorSolo: joelho na barriga direto nos dois sentidos, levantada
 * tecnica do solo livre, recuo em pe (deslize), afastar com distancia real, pernas
 * presas visiveis e as animacoes proprias das quedas/entradas.
 */
class Quedas5Q5Test {

    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;
    private Intencao ia = Intencao.NENHUMA;
    private Intencao ib = Intencao.NENHUMA;

    private void novo(int xa, int xb) {
        a = new IrmaoMiauBranco(xa, 150, 0.7);
        b = new IrmaoMiauPreto(xb, 150, 0.7);
        a.setViradoParaDireita(xa < xb);
        b.setViradoParaDireita(xb < xa);
        a.setGraduacao(Graduacao.PRETA);
        b.setGraduacao(Graduacao.PRETA);
        m = new MotorSolo(a, b, null, 15, 715);
    }

    private IrmaoMiau l(Lado x) {
        return x == Lado.A ? a : b;
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            m.informarIntencoes(ia, ib);
            m.atualizar(ia, ib);
            a.atualizar(false, false);
            b.atualizar(false, false);
        }
    }

    private void resolver() {
        for (int i = 0; i < 150 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        ticks(1);
    }

    private MotorSolo.Resposta cmd(Lado l, Direcao d, Familia f) {
        return m.comando(l, new ComandoSolo(d, f));
    }

    /** x em pe, o outro deitado e perto (solo livre). */
    private void soloLivre(Lado emPe) {
        novo(300, 380);
        l(emPe.outro()).alternarDeitado();
        ticks(30);
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao());
    }

    @Test
    void joelhoNaBarrigaDiretoEntraNosDoisSentidos() {
        for (Lado x : Lado.values()) {
            soloLivre(x);
            assertEquals(Resultado.INICIADA, cmd(x, Direcao.BAIXO, Familia.R1).resultado(), "RB v");
            assertEquals(TipoEfeitoVisual.TECNICA_ATAQUE, m.efeitoVisual(x).orElseThrow().tipo());
            assertEquals("joelho_direto", m.efeitoVisual(x).orElseThrow().tecnica());
            assertEquals(TipoEfeitoVisual.TECNICA_RECEBE, m.efeitoVisual(x.outro()).orElseThrow().tipo());
            resolver();
            assertEquals(Posicao.JOELHO_BARRIGA, m.getPosicao(), x + " por cima");
            assertEquals(x, m.getEstado().getControlador().orElseThrow());
        }
    }

    @Test
    void levantadaTecnicaDoSoloLivreUsaATransicaoPropria() {
        for (Lado x : Lado.values()) {
            soloLivre(x.outro());                              // x deitado
            assertEquals(Resultado.INICIADA, cmd(x, Direcao.CIMA, Familia.RS).resultado(), "levantar");
            assertEquals("levantar_tecnico", m.efeitoVisual(x).orElseThrow().tecnica());
            resolver();
            assertEquals(Posicao.EM_PE_SEPARADOS, m.getPosicao());
            ticks(0);
            assertTrue(l(x).getTransicaoAtiva() == TransicaoPostura.LEVANTADA_TECNICA
                    || !l(x).isDeitado(), "sobe pela levantada tecnica (nao a rapida de 16 ticks)");
        }
    }

    @Test
    void recuoEmPeDeslizaEmDoisPassos() {
        soloLivre(Lado.A);
        int x0 = a.getX();
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.A, Direcao.BAIXO, Familia.RS).resultado(), "recuar");
        MotorSolo.EfeitoVisual e = m.efeitoVisual(Lado.A).orElseThrow();
        assertEquals("recuar", e.tecnica());
        assertEquals(a.getX() - x0, e.deslocamento(), "o desenho parte do lugar antigo");
        assertNotEquals(0, e.deslocamento());
        assertTrue(AnimacoesTecnicas.tem("recuar:ATACA"));
    }

    @Test
    void afastarDaGuardaAumentaADistanciaLogicaAosPoucos() {
        novo(300, 380);
        m.forcarPosicao(Posicao.GUARDA, Lado.B, Lado.A);
        ticks(40);
        assertTrue(Math.abs(a.getX() - b.getX()) < MotorSolo.ALCANCE_ENTRAR_PX, "na guarda estao perto");
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.BAIXO, Familia.R1).resultado(), "afastar com os pes");
        resolver();
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao());
        int antes = Math.abs(a.getX() - b.getX());
        ticks(1);
        int depoisUmTick = Math.abs(a.getX() - b.getX());
        // 5Q.6 (pedido): quem empurrou com os pes tambem recua 1 px/tick no chao (era so quem estava por cima)
        assertTrue(depoisUmTick - antes <= 4, "sem teletransporte");
        ticks(60);
        assertTrue(Math.abs(a.getX() - b.getX()) >= MotorSolo.DISTANCIA_APOS_AFASTAR - 1,
                "fora do alcance de entrar na guarda: " + Math.abs(a.getX() - b.getX()));
        assertEquals("LONGE", cmd(Lado.B, Direcao.CIMA, Familia.RS).motivo(), "nao reentra na hora");
    }

    @Test
    void pernasPresasMantemAsMaosNasCanelasEnquantoOControleDura() {
        soloLivre(Lado.A);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.DIREITA, Familia.RS).resultado(), "prender as pernas");
        resolver();
        assertEquals(TecnicaSolo.Exposicao.PERNAS_PRESAS, m.exposicaoDe(Lado.B).orElseThrow());
        ticks(25);
        assertEquals("pernas_presas", m.efeitoVisual(Lado.A).orElseThrow().tecnica(), "as maos continuam nas pernas");
        assertEquals("pernas_presas", m.efeitoVisual(Lado.B).orElseThrow().tecnica());
        assertTrue(AnimacoesTecnicas.tem("pernas_presas:ATACA") && AnimacoesTecnicas.tem("pernas_presas:RECEBE"));
    }

    @Test
    void animacoesPropriasDasQuedasEntradasEPostura() {
        for (String t : new String[] {"rasteira", "entrada_pernas", "joelho_direto", "postura",
            "entrada_perna_guarda", "entrada_perna_dominante", "entrada_perna_aberta", "entrada_perna_aberta_dlr"}) {
            assertTrue(AnimacoesTecnicas.tem(t + ":ATACA"), t + " ATACA");
            assertTrue(AnimacoesTecnicas.tem(t + ":RECEBE"), t + " RECEBE");
        }
        assertTrue(AnimacoesTecnicas.tem("levantar_tecnico:ATACA"));
        assertTrue(AnimacoesTecnicas.tem("postura_recompor:ATACA"));
        assertTrue(AnimacoesTecnicas.tem("underhook_meia:ATACA"));
        assertTrue(MotorSolo.ANIMACAO_PROPRIA.contains(CatalogoTecnicasSolo.RASTEIRA));
        assertTrue(MotorSolo.ANIMACAO_PROPRIA.contains(CatalogoTecnicasSolo.ENTRADA_PERNAS));
    }

    @Test
    void posturaGanhaAnimacaoPropriaEVindoDaQuebraRecompoe() {
        novo(300, 380);
        m.forcarPosicao(Posicao.GUARDA, Lado.B, Lado.A);
        ticks(30);
        cmd(Lado.B, Direcao.BAIXO, Familia.RS);                // postura
        assertEquals("postura", m.efeitoVisual(Lado.B).orElseThrow().tecnica());
        assertEquals("postura", m.efeitoVisual(Lado.A).orElseThrow().tecnica());
        ticks(MotorSolo.POSTURA_TICKS + 2);
        cmd(Lado.A, Direcao.BAIXO, Familia.RS);                // quebrar
        ticks(ParametrosFinalizacao.POSTURA_BLOQUEADA_TICKS + 1);
        cmd(Lado.B, Direcao.BAIXO, Familia.RS);                // recompor
        assertEquals("postura_recompor", m.efeitoVisual(Lado.B).orElseThrow().tecnica());
    }
}
