package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.AnimacoesTecnicas;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.TipoEfeitoVisual;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.4: quebrar postura (quebrada x defendida), prender pernas e ajustar o ashi animados. */
class Animacoes5Q4MotorTest {

    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;

    private void novo() {
        a = new IrmaoMiauBranco(300, 150, 0.7);
        b = new IrmaoMiauPreto(380, 150, 0.7);
        a.setViradoParaDireita(true);
        b.setViradoParaDireita(false);
        a.setGraduacao(Graduacao.PRETA);
        b.setGraduacao(Graduacao.PRETA);
        m = new MotorSolo(a, b, null, 15, 715);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            m.informarIntencoes(Intencao.NENHUMA, Intencao.NENHUMA);
            m.atualizar(Intencao.NENHUMA, Intencao.NENHUMA);
            a.atualizar(false, false);
            b.atualizar(false, false);
        }
    }

    private MotorSolo.Resposta cmd(Lado l, Direcao d, Familia f) {
        return m.comando(l, new ComandoSolo(d, f));
    }

    private String chave(Lado l) {
        return m.efeitoVisual(l).map(AnimacoesTecnicas::chave).orElse(null);
    }

    @Test
    void quebrarPosturaAnimaOsDoisEOCurvadoFicaAteRecompor() {
        novo();
        m.forcarPosicao(Posicao.GUARDA, Lado.B, Lado.A);       // A por baixo, guarda fechada
        ticks(30);
        assertEquals(MotorSolo.Resultado.CONCLUIDA, cmd(Lado.A, Direcao.BAIXO, Familia.RS).resultado());
        assertEquals("quebrar_postura:ATACA", chave(Lado.A));
        assertEquals("postura_quebrada:RECEBE", chave(Lado.B), "o de cima fica curvado");
        ticks(20);
        assertEquals("postura_quebrada:RECEBE", chave(Lado.B), "ainda curvado no meio da exposicao");
        int[] pose = new int[30];
        AnimacoesTecnicas.aplicar(pose, m.efeitoVisual(Lado.B).orElseThrow());
        assertTrue(pose[9] > 15, "tronco para a frente");
    }

    @Test
    void defendidaPelaPosturaTemAnimacaoPropria() {
        novo();
        m.forcarPosicao(Posicao.GUARDA, Lado.B, Lado.A);
        ticks(30);
        cmd(Lado.B, Direcao.BAIXO, Familia.RS);                 // postura
        ticks(2);
        assertEquals(MotorSolo.Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.RS).resultado());   // raspagem
        for (int i = 0; i < 150 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        assertEquals("DEFENDIDA", m.getUltimoEvento().tipo());
        assertEquals("postura_firme:RECEBE", chave(Lado.B), "o de cima se apruma (nao curva)");
        assertEquals("postura_firme:ATACA", chave(Lado.A));
        int[] pose = new int[30];
        ticks(4);
        AnimacoesTecnicas.aplicar(pose, m.efeitoVisual(Lado.B).orElseThrow());
        assertTrue(pose[9] < 0, "tronco para tras: diferente da postura quebrada");
    }

    @Test
    void ajustarOAshiAnimaETerminaNaPose() {
        novo();
        m.forcarPosicao(Posicao.ASHI_GARAMI, Lado.B, Lado.A);
        ticks(30);
        assertEquals(MotorSolo.Resultado.CONCLUIDA, cmd(Lado.A, Direcao.BAIXO, Familia.RS).resultado());
        assertEquals("consolidar_ashi:ATACA", chave(Lado.A));
        assertEquals("consolidar_ashi:RECEBE", chave(Lado.B));
        ticks(MotorSolo.TICKS_ANIMACAO_INSTANTANEA + 1);
        assertTrue(m.efeitoVisual(Lado.A).isEmpty(), "volta a pose da posicao");
    }

    @Test
    void prenderPernasTemAnimacaoDosDoisLados() {
        assertTrue(AnimacoesTecnicas.tem("prender_pernas:ATACA"));
        assertTrue(AnimacoesTecnicas.tem("prender_pernas:RECEBE"));
        assertTrue(MotorSolo.ANIMACAO_PROPRIA.contains(CatalogoTecnicasSolo.PRENDER_PERNAS));
        MotorSolo.EfeitoVisual e = new MotorSolo.EfeitoVisual(TipoEfeitoVisual.TECNICA_ATAQUE, 0.5, null);
        assertNotNull(e);
    }

    @Test
    void retencaoComOsPesEAfastarDoChaoAnimados() {
        assertTrue(AnimacoesTecnicas.tem("retencao_pes:ATACA") && AnimacoesTecnicas.tem("retencao_pes:RECEBE"));
        assertTrue(AnimacoesTecnicas.tem("afastar_do_chao:ATACA"));
        MotorSolo.EfeitoVisual fim = new MotorSolo.EfeitoVisual(TipoEfeitoVisual.AFASTANDO, 1.0, null, "afastar_do_chao",
                1.0, 0, 0, 0);
        int[] pose = new int[30];
        AnimacoesTecnicas.aplicar(pose, fim);
        assertEquals(0, java.util.Arrays.stream(pose).map(Math::abs).sum(), "ajuste instantaneo termina na pose da posicao");
    }
}
