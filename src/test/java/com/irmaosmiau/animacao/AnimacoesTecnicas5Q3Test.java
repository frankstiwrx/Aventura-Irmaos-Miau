package com.irmaosmiau.animacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.MotorSolo.EfeitoVisual;
import com.irmaosmiau.posicional.MotorSolo.TipoEfeitoVisual;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.3: animacoes proprias (empurrar/ser empurrado, afastar, armlock da
 * guarda, americana da montada, mata-leao das costas, berimbolo): acionadas pela
 * tecnica certa, os dois sincronizados, interrupcao suave, volta a postura,
 * espelhamento J1/J2, continuidade e nenhuma mudanca mecanica.
 */
class AnimacoesTecnicas5Q3Test {

    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;

    private void novo(boolean doisPretos) {
        a = doisPretos ? new IrmaoMiauPreto(300, 150, 0.7) : new IrmaoMiauBranco(300, 150, 0.7);
        b = new IrmaoMiauPreto(380, 150, 0.7);
        a.setViradoParaDireita(true);
        b.setViradoParaDireita(false);
        m = new MotorSolo(a, b, null, 15, 715);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            m.informarIntencoes(MotorSolo.Intencao.NENHUMA, MotorSolo.Intencao.NENHUMA);
            m.atualizar(MotorSolo.Intencao.NENHUMA, MotorSolo.Intencao.NENHUMA);
            a.atualizar(false, false);
            b.atualizar(false, false);
        }
    }

    private void situacao(Posicao p, Lado cima, Lado ctrl) {
        m.iniciarSituacao(p, cima, ctrl, false);
        ticks(30);
        m.definirEstabilidade(80);
    }

    private String chave(Lado l) {
        return AnimacoesTecnicas.chave(m.efeitoVisual(l).orElseThrow());
    }

    @Test
    void cadaTecnicaAcionaAAnimacaoCertaNosDoisLutadores() {
        Object[][] casos = {
            {Posicao.GUARDA, Lado.B, Lado.A, Direcao.CIMA, Familia.L1, "armlock@guarda"},
            {Posicao.MONTADA, Lado.A, Lado.A, Direcao.DIREITA, Familia.L1, "americana@montada"},
            {Posicao.COSTAS, Lado.A, Lado.A, Direcao.CIMA, Familia.L2, "mata_leao@costas"},
            {Posicao.DE_LA_RIVA, Lado.B, Lado.A, Direcao.CIMA, Familia.L1_L2, "berimbolo"},
            {Posicao.GUARDA, Lado.B, Lado.A, Direcao.BAIXO, Familia.R1, "afastar_da_guarda"}};
        for (Object[] c : casos) {
            for (boolean inverter : new boolean[] {false, true}) {
                novo(false);
                Lado cima = inverter ? ((Lado) c[1]).outro() : (Lado) c[1];
                Lado ctrl = inverter ? ((Lado) c[2]).outro() : (Lado) c[2];
                situacao((Posicao) c[0], cima, ctrl);
                m.comando(ctrl, new ComandoSolo((Direcao) c[3], (Familia) c[4]));
                ticks(2);
                assertEquals(c[5] + ":ATACA", chave(ctrl), c[5] + " quem aplica (" + ctrl + ")");
                assertEquals(c[5] + ":RECEBE", chave(ctrl.outro()), c[5] + " quem recebe");
                EfeitoVisual ea = m.efeitoVisual(ctrl).orElseThrow();
                EfeitoVisual eb = m.efeitoVisual(ctrl.outro()).orElseThrow();
                assertEquals(ea.progresso(), eb.progresso(), 1e-9, c[5] + ": os dois no mesmo tempo");
            }
        }
    }

    @Test
    void empurrarESerEmpurradoComDeslizeSemTeletransporte() {
        novo(false);
        b.encostarNaBorda(a.getX() + 80 - b.getX());
        m.comando(Lado.A, new ComandoSolo(Direcao.CIMA, true));
        assertEquals("empurrao:ATACA", chave(Lado.A));
        EfeitoVisual e = m.efeitoVisual(Lado.A).orElseThrow();
        assertTrue(e.marco() > 0.3 && e.marco() < 0.8, "o empurrao chega no meio do efeito: " + e.marco());
        for (int i = 0; i < 40 && m.efeitoVisual(Lado.B).isEmpty(); i++) {
            ticks(1);
        }
        EfeitoVisual r = m.efeitoVisual(Lado.B).orElseThrow();
        assertEquals(TipoEfeitoVisual.EMPURRADO, r.tipo());
        assertEquals("empurrao:RECEBE", AnimacoesTecnicas.chave(r));
        assertTrue(r.deslocamento() != 0, "o motor ja moveu o corpo");
        // no comeco o desenho ainda esta no lugar antigo (a frente) e desliza ate o novo
        double[] ini = AnimacoesTecnicas.aplicar(new int[30], new EfeitoVisual(TipoEfeitoVisual.EMPURRADO, 0.0, null,
                "empurrao", 1.0, r.deslocamento(), 0, 0));
        double[] fim = AnimacoesTecnicas.aplicar(new int[30], new EfeitoVisual(TipoEfeitoVisual.EMPURRADO, 0.5, null,
                "empurrao", 1.0, r.deslocamento(), 0, 0));
        assertEquals(Math.abs(r.deslocamento()), ini[0], 1e-6);
        assertEquals(0, fim[0], 1e-6);
        // quem empurra inclina para frente no empurrao e quem recebe para tras no impacto
        int[] pa = new int[30];
        AnimacoesTecnicas.aplicar(pa, new EfeitoVisual(TipoEfeitoVisual.EMPURRANDO, e.marco(), null, "empurrao",
                e.marco(), 0, 0, 0));
        int[] pb = new int[30];
        AnimacoesTecnicas.aplicar(pb, new EfeitoVisual(TipoEfeitoVisual.EMPURRADO, 0.14, null, "empurrao", 1.0, 0, 0, 0));
        assertTrue(pa[9] > 0 && pa[20] > 20, "tronco e bracos para frente");
        assertTrue(pb[9] < -15, "tronco para tras no impacto");
    }

    @Test
    void defesaInterrompeEOsDoisVoltamSuaveParaAPostura() {
        novo(false);
        situacao(Posicao.MONTADA, Lado.A, Lado.A);
        m.comando(Lado.A, new ComandoSolo(Direcao.DIREITA, Familia.L1));
        ticks(4);
        m.comando(Lado.B, new ComandoSolo(Direcao.BAIXO, false));                    // corta a entrada
        EfeitoVisual la = m.efeitoVisual(Lado.A).orElseThrow();
        EfeitoVisual lb = m.efeitoVisual(Lado.B).orElseThrow();
        assertEquals(TipoEfeitoVisual.LIBERANDO, la.tipo());
        assertEquals("americana@montada:ATACA", AnimacoesTecnicas.chave(la));
        assertEquals("americana@montada:RECEBE", AnimacoesTecnicas.chave(lb));
        assertTrue(la.desde() > 0 && la.desde() < 0.5, "volta do ponto da entrada: " + la.desde());
        int[] inicio = new int[30];
        AnimacoesTecnicas.aplicar(inicio, la);
        assertTrue(Arrays.stream(inicio).anyMatch(v -> v != 0), "comeca de onde estava");
        int[] fim = new int[30];
        AnimacoesTecnicas.aplicar(fim, new EfeitoVisual(TipoEfeitoVisual.LIBERANDO, 1.0, la.categoria(), la.tecnica(),
                1.0, 0, la.desde(), 0));
        assertTrue(Arrays.stream(fim).allMatch(v -> v == 0), "termina na pose da posicao");
        ticks(20);
        assertTrue(m.efeitoVisual(Lado.A).isEmpty() && m.efeitoVisual(Lado.B).isEmpty(), "efeito encerrado");
        assertEquals(Posicao.MONTADA, m.getPosicao(), "a mecanica decidiu: a montada continua");
    }

    @Test
    void escapeERendicaoTambemLiberamAPose() {
        novo(false);
        situacao(Posicao.COSTAS, Lado.A, Lado.A);
        m.comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));
        for (int i = 0; i < 40 && m.getAtaque().map(x -> x.fase() == MotorSolo.FaseFinalizacao.ENTRADA).orElse(false); i++) {
            ticks(1);
        }
        m.comando(Lado.B, new ComandoSolo(Direcao.BAIXO, false));
        assertTrue(m.efeitoVisual(Lado.B).orElseThrow().pulsoDefesa() > 0.9, "reacao da defesa no mesmo tick");
        m.comando(Lado.B, new ComandoSolo(Direcao.BAIXO, true));                     // bate
        assertEquals(TipoEfeitoVisual.BATENDO, m.efeitoVisual(Lado.B).orElseThrow().tipo());
        assertEquals("mata_leao@costas:ATACA", AnimacoesTecnicas.chave(m.efeitoVisual(Lado.A).orElseThrow()));
    }

    @Test
    void espelhamentoPersonagensEGraduacoesNaoMudamAAnimacao() {
        novo(true);                                                                  // dois Pretos
        a.setGraduacao(Graduacao.AZUL);
        b.setGraduacao(Graduacao.PRETA);
        situacao(Posicao.GUARDA, Lado.A, Lado.B);                                   // J2 por baixo ataca o J1
        m.comando(Lado.B, new ComandoSolo(Direcao.CIMA, Familia.L1));
        ticks(3);
        assertEquals("armlock@guarda:ATACA", chave(Lado.B));
        // as poses sao locais (a frente de cada um): o mesmo efeito da a mesma pose para J1 ou J2
        EfeitoVisual e = m.efeitoVisual(Lado.B).orElseThrow();
        int[] pb = new int[30];
        int[] pa = new int[30];
        AnimacoesTecnicas.aplicar(pb, e);
        AnimacoesTecnicas.aplicar(pa, e);
        assertEquals(Arrays.toString(pa), Arrays.toString(pb));
        // o AnimadorSolo descreve a pose com o efeito, para quem estiver em cada lado
        AnimadorSolo anim = new AnimadorSolo(new TransicoesPostura(null));
        String id = anim.aplicar(b, m);
        assertNotNull(id);
        assertTrue(id.contains("FINALIZANDO"), id);
        assertTrue(anim.aplicar(a, m).contains("DEFENDENDO_FINALIZACAO"));
    }

    @Test
    void quadrosContinuosSemEstalosEComInicioNaPoseBase() {
        String[] chaves = {"empurrao:ATACA", "empurrao:RECEBE", "afastar_da_guarda:ATACA", "afastar_da_guarda:RECEBE",
            "armlock@guarda:ATACA", "armlock@guarda:RECEBE", "americana@montada:ATACA", "americana@montada:RECEBE",
            "mata_leao@costas:ATACA", "mata_leao@costas:RECEBE", "berimbolo:ATACA", "berimbolo:RECEBE"};
        for (String c : chaves) {
            assertTrue(AnimacoesTecnicas.tem(c), c);
            double[] zero = AnimacoesTecnicas.amostrar(c, 0);
            assertTrue(Arrays.stream(zero).allMatch(v -> v == 0), c + ": comeca na pose da posicao");
            double[] ant = zero;
            for (int i = 1; i <= 200; i++) {
                double[] q = AnimacoesTecnicas.amostrar(c, i / 200.0);
                for (int k = 0; k < 32; k++) {
                    assertTrue(Math.abs(q[k] - ant[k]) < 4.0, c + " indice " + k + " salto em t=" + i / 200.0);
                }
                ant = q;
            }
        }
        double[] fimEmpurrao = AnimacoesTecnicas.amostrar("empurrao:ATACA", 1.0);
        assertTrue(Arrays.stream(fimEmpurrao).allMatch(v -> v == 0), "o empurrao volta a base");
        assertNotEquals(0, AnimacoesTecnicas.amostrar("berimbolo:ATACA", 0.55)[10], "inversao: rotacao global");
    }

    @Test
    void desenharNaoMexeNoMotor() {
        novo(false);
        situacao(Posicao.MONTADA, Lado.A, Lado.A);
        m.comando(Lado.A, new ComandoSolo(Direcao.DIREITA, Familia.L1));
        ticks(20);
        String antes = m.resumo() + m.getAtaque().map(x -> x.pressao() + "").orElse("") + a.getX() + b.getX();
        AnimadorSolo anim = new AnimadorSolo(new TransicoesPostura(null));
        for (int i = 0; i < 5; i++) {
            anim.aplicar(a, m);
            anim.aplicar(b, m);
        }
        String depois = m.resumo() + m.getAtaque().map(x -> x.pressao() + "").orElse("") + a.getX() + b.getX();
        assertEquals(antes, depois, "o desenho so le o estado do motor");
    }
}
