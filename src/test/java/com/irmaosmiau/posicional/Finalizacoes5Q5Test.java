package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.MotorSolo.FaseFinalizacao;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.Resultado;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.5: finalizacoes com tempo real de reacao (entrada mais longa, "na ponta"
 * antes de consolidar), bater/defender/soltar sem conflito de comandos (com LT/LB ainda
 * segurados), janela da guilhotina depois de quebrar a postura e os parametros num lugar so.
 */
class Finalizacoes5Q5Test {

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

    private int ticksAteDisputa() {
        int n = 0;
        while (m.getAtaque().map(x -> x.fase() == FaseFinalizacao.ENTRADA).orElse(false) && n < 200) {
            ticks(1);
            n++;
        }
        return n;
    }

    private void ateNaPonta() {
        for (int i = 0; i < 2000 && !m.getAtaque().map(MotorSolo.Ataque::naPonta).orElse(false); i++) {
            ticks(1);
        }
        assertTrue(m.getAtaque().orElseThrow().naPonta(), "chegou a 100%");
    }

    @Test
    void parametrosCentralizadosERepertorioPreservado() {
        assertEquals(ParametrosFinalizacao.PRESSAO_INICIAL, MotorSolo.PRESSAO_INICIAL);
        assertEquals(ParametrosFinalizacao.RECARGA_ACAO, MotorSolo.RECARGA_ACAO_FINALIZACAO);
        assertEquals(ParametrosFinalizacao.POSTURA_QUEBRADA_TICKS, MotorSolo.POSTURA_QUEBRADA_TICKS);
        // o catalogo do codigo tem 40 finalizacoes executaveis (23 tecnicas-base por posicao); o spec da 5Q.5
        // fala em 47: divergencia registrada no relatorio, nada foi adicionado nem removido
        assertEquals(40, CatalogoFinalizacoes.referencia().todas().size(), "nenhuma finalizacao a mais ou a menos");
        assertEquals(23, CatalogoFinalizacoes.referencia().todas().stream().map(Finalizacao::tecnicaBase).distinct().count());
        assertTrue(CatalogoFinalizacoes.referencia().todas().stream()
                .allMatch(f -> f.janelaEntrada() == ParametrosFinalizacao.janelaCatalogo(f.graduacao())));
    }

    @Test
    void entradaDaTempoDeCortarNosDoisSentidos() {
        for (Lado x : Lado.values()) {
            novo();
            em(Posicao.GUARDA, x.outro(), x);
            assertEquals(Resultado.INICIADA, cmd(x, Direcao.CIMA, Familia.L1).resultado());   // armlock
            int n = ticksAteDisputa();
            assertTrue(n >= 28, "entrada de ~0,5 s (antes ~13 ticks): " + n);
        }
    }

    @Test
    void naPontaNaoConsolidaNaHoraEAindaDaParaBater() {
        novo();
        em(Posicao.COSTAS, Lado.A, Lado.A);
        m.definirEstabilidade(90);
        cmd(Lado.A, Direcao.CIMA, Familia.L2);                  // mata-leao
        ateNaPonta();
        ticks(MotorSolo.TICKS_NA_PONTA / 2);
        assertTrue(m.getVitoria().isEmpty(), "em 100% ainda nao consolidou");
        assertEquals("BATEU", cmd(Lado.B, Direcao.BAIXO, Familia.R1).motivo());
        assertTrue(m.getVitoria().orElseThrow().rendicao());
    }

    @Test
    void naPontaADefesaVoltaParaADisputaESemReacaoConsolida() {
        novo();
        em(Posicao.COSTAS, Lado.B, Lado.B);
        m.definirEstabilidade(90);
        cmd(Lado.B, Direcao.CIMA, Familia.L2);
        ateNaPonta();
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.A, Direcao.BAIXO, Familia.RS).resultado(), "defende na ponta");
        assertFalse(m.getAtaque().orElseThrow().naPonta(), "a pressao caiu");
        ateNaPonta();
        ticks(MotorSolo.TICKS_NA_PONTA + 1);
        MotorSolo.Vitoria v = m.getVitoria().orElseThrow();
        assertFalse(v.rendicao());
        assertEquals(Lado.B, v.vencedor());
    }

    @Test
    void soltarEDefenderValemComOGatilhoAindaSegurado() {
        // triangulo (L2 ->): quem ataca ainda segura o LT; RS v chega como L2 v e antes era recusado
        novo();
        em(Posicao.GUARDA, Lado.B, Lado.A);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.DIREITA, Familia.L2).resultado(), "triangulo");
        ticksAteDisputa();
        assertEquals("SOLTOU", cmd(Lado.A, Direcao.BAIXO, Familia.L2).motivo());
        assertTrue(m.getAtaque().isEmpty());
        assertEquals(Posicao.GUARDA, m.getPosicao(), "soltar mantem a posicao");

        // quem defende com LB segurado tambem defende (antes: "SOB FINALIZACAO")
        novo();
        em(Posicao.COSTAS, Lado.A, Lado.A);
        m.definirEstabilidade(60);
        cmd(Lado.A, Direcao.CIMA, Familia.L2);
        ticksAteDisputa();
        double p = m.getAtaque().orElseThrow().pressao();
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.B, Direcao.BAIXO, Familia.L1).resultado());
        assertTrue(m.getAtaque().orElseThrow().pressao() < p);
        // o proprio comando da finalizacao continua apertando (nao vira soltar)
        double p2 = m.getAtaque().orElseThrow().pressao();
        assertEquals("APERTOU", cmd(Lado.A, Direcao.CIMA, Familia.L2).motivo());
        assertTrue(m.getAtaque().orElseThrow().pressao() > p2);
    }

    @Test
    void bateriaGanhaDeQualquerOutroComandoDoMesmoTick() {
        novo();
        em(Posicao.COSTAS, Lado.A, Lado.A);
        m.definirEstabilidade(60);
        cmd(Lado.A, Direcao.CIMA, Familia.L2);
        ticksAteDisputa();
        java.util.Map<Lado, ComandoSolo> mesmo = new java.util.EnumMap<>(Lado.class);
        mesmo.put(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.RS));
        mesmo.put(Lado.B, new ComandoSolo(Direcao.BAIXO, Familia.R1));
        assertEquals("BATEU", m.comandos(mesmo).get(Lado.B).motivo());
        assertTrue(MotorSolo.ehBater(new ComandoSolo(Direcao.BAIXO, Familia.R1)));
        assertFalse(MotorSolo.ehBater(new ComandoSolo(Direcao.BAIXO, Familia.RS)));
    }

    @Test
    void guilhotinaTemJanelaRealDepoisDeQuebrarAPostura() {
        novo();
        em(Posicao.GUARDA, Lado.B, Lado.A);
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.A, Direcao.BAIXO, Familia.RS).resultado(), "quebrou a postura");
        assertTrue(m.posturaBloqueada(Lado.B));
        assertEquals("POSTURA QUEBRADA", cmd(Lado.B, Direcao.BAIXO, Familia.RS).motivo(),
                "logo depois da quebra a postura ainda nao volta");
        ticks(ParametrosFinalizacao.POSTURA_BLOQUEADA_TICKS - 5);
        assertTrue(m.exposicaoDe(Lado.B).isPresent(), "ainda exposto");
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.BAIXO, Familia.L2).resultado(), "guilhotina entra");
        assertEquals("guilhotina", m.getAtaque().orElseThrow().finalizacao().tecnicaBase());

        novo();
        em(Posicao.GUARDA, Lado.B, Lado.A);
        cmd(Lado.A, Direcao.BAIXO, Familia.RS);
        ticks(ParametrosFinalizacao.POSTURA_BLOQUEADA_TICKS + 1);
        assertFalse(m.posturaBloqueada(Lado.B));
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.B, Direcao.BAIXO, Familia.RS).resultado(), "depois recompoe");
    }
}
