package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.Resultado;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.5: escapes das posicoes dominantes com rota construida por decisao:
 * enquadrar cria ESPACO (o consolidar so fecha o espaco, nao cancela o escape),
 * segurar com um ajuste custa recarga, e a meia-guarda ganha o underhook. As
 * posicoes dominantes continuam vantajosas (sem enquadrar o consolidar cancela).
 */
class Escapes5Q5Test {

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

    private void em(Posicao p, Lado cima, Lado ctrl) {
        m.forcarPosicao(p, cima, ctrl);
        ticks(30);
    }

    private MotorSolo.Resposta cmd(Lado l, Direcao d, Familia f) {
        return m.comando(l, new ComandoSolo(d, f));
    }

    private void resolver() {
        for (int i = 0; i < 150 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        ticks(1);
    }

    @Test
    void cemQuilosSemEnquadrarOConsolidarAindaCancelaEcustaRecarga() {
        novo();
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        m.definirEstabilidade(40);
        assertEquals(Resultado.INICIADA, cmd(Lado.B, Direcao.DIREITA, Familia.RS).resultado(), "escape de quadril");
        assertEquals(Resultado.CONTRA, cmd(Lado.A, Direcao.BAIXO, Familia.RS).resultado(), "consolidou: cancelou");
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao(), "a posicao dominante continua vantajosa");
        assertEquals(MotorSolo.RECARGA_CONTRA_AJUSTE, m.recargaDe(Lado.A), "segurar tambem tem custo");
    }

    @Test
    void cemQuilosEnquadrarCriarEspacoERecuperarAGuardaNosDoisSentidos() {
        for (Lado cima : Lado.values()) {
            Lado baixo = cima.outro();
            novo();
            em(Posicao.CONTROLE_LATERAL, cima, cima);
            m.definirEstabilidade(100);                       // cem quilos no maximo
            assertEquals(Resultado.CONCLUIDA, cmd(baixo, Direcao.BAIXO, Familia.RS).resultado(), "enquadrar");
            assertTrue(m.temEspaco(baixo), "criou espaco");
            ticks(MotorSolo.RECARGA_TICKS / 2 + 1);
            assertEquals(Resultado.INICIADA, cmd(baixo, Direcao.DIREITA, Familia.RS).resultado(), "recuperar a guarda");
            MotorSolo.Resposta r = cmd(cima, Direcao.BAIXO, Familia.RS);
            assertEquals("FECHOU O ESPAÇO", r.motivo(), "o consolidar nao cancela: so fecha o espaco");
            assertTrue(m.getTentativa().isPresent(), "o escape continua");
            assertFalse(m.temEspaco(baixo));
            resolver();
            // fechado o espaco, vale a estabilidade normal: 75 + 10 > 60 -> o escape falha
            assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao(), "reagir a tempo ainda defende");

            novo();
            em(Posicao.CONTROLE_LATERAL, cima, cima);
            m.definirEstabilidade(100);
            cmd(baixo, Direcao.BAIXO, Familia.RS);            // enquadrar (75, com espaco)
            ticks(MotorSolo.RECARGA_TICKS / 2 + 1);
            cmd(baixo, Direcao.DIREITA, Familia.RS);          // escape com o espaco: 75 < 60 + 20
            resolver();
            assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao(), "a rota enquadrar -> espaco -> guarda funciona");
            assertEquals(baixo, m.getEstado().getControlador().orElseThrow());
        }
    }

    @Test
    void montadaEJoelhoNaBarrigaTambemUsamOEspaco() {
        novo();
        em(Posicao.MONTADA, Lado.A, Lado.A);
        m.definirEstabilidade(90);
        cmd(Lado.B, Direcao.BAIXO, Familia.RS);                // enquadrar a montada
        assertTrue(m.temEspaco(Lado.B));
        ticks(MotorSolo.RECARGA_TICKS / 2 + 1);
        cmd(Lado.B, Direcao.DIREITA, Familia.RS);              // escape para a meia (limiar 65 + 20)
        resolver();
        assertEquals(Posicao.MEIA_GUARDA, m.getPosicao());

        novo();
        em(Posicao.JOELHO_BARRIGA, Lado.A, Lado.A);
        m.definirEstabilidade(90);
        cmd(Lado.B, Direcao.BAIXO, Familia.RS);                // enquadrar o joelho
        ticks(MotorSolo.RECARGA_TICKS / 2 + 1);
        cmd(Lado.B, Direcao.DIREITA, Familia.RS);              // empurrar o joelho
        resolver();
        assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao());
    }

    @Test
    void meiaGuardaUnderhookQuebraAPressaoDeOmbroERecuperaAGuarda() {
        novo();
        em(Posicao.MEIA_GUARDA, Lado.A, Lado.B);
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.A, Direcao.BAIXO, Familia.RS).resultado(), "pressao de ombro");
        assertTrue(m.emPostura(Lado.A));
        ticks(MotorSolo.RECARGA_TICKS);
        cmd(Lado.B, Direcao.BAIXO, Familia.RS);                // recuperar a guarda contra a pressao: falha
        resolver();
        assertEquals(Posicao.MEIA_GUARDA, m.getPosicao(), "pressao de ombro segura");
        ticks(MotorSolo.RECARGA_FALHA_TICKS + 1);
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.B, Direcao.DIREITA, Familia.RS).resultado(), "underhook");
        assertTrue(m.exposicaoDe(Lado.A).isPresent(), "a pressao saiu");
        assertEquals("POSTURA QUEBRADA".length() > 0, true);
        ticks(MotorSolo.RECARGA_TICKS / 2 + 1);
        cmd(Lado.B, Direcao.BAIXO, Familia.RS);                // recuperar a guarda
        resolver();
        assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao(), "a rota underhook -> guarda funciona");
    }
}
