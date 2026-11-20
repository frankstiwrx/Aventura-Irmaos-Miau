package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.AnimacoesTecnicas;
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
 * Entrega 5Q.6: quedas explosivas sem pegada (LT em pe e de joelhos) e o body lock no
 * RB ->: avanco real ate o contato, destino proprio, defesa (sprawl / base com RT /
 * recuar) e as animacoes de quem entra e de quem recebe.
 */
class Quedas5Q6Test {

    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;
    private Intencao ia = Intencao.NENHUMA;
    private Intencao ib = Intencao.NENHUMA;

    private void novo(int xa, int xb, boolean joelhos) {
        a = new IrmaoMiauBranco(xa, 150, 0.7);
        b = new IrmaoMiauPreto(xb, 150, 0.7);
        a.setViradoParaDireita(xa < xb);
        b.setViradoParaDireita(xb < xa);
        a.setGraduacao(Graduacao.PRETA);
        b.setGraduacao(Graduacao.PRETA);
        if (joelhos) {
            a.ajoelhar();
            b.ajoelhar();
        }
        m = new MotorSolo(a, b, null, 15, 715);
        ticks(30);
        ia = Intencao.NENHUMA;
        ib = Intencao.NENHUMA;
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
        ticks(MotorSolo.ATERRISSAGEM_TICKS + 4);
    }

    private MotorSolo.Resposta cmd(Lado l, Direcao d, Familia f) {
        return m.comando(l, new ComandoSolo(d, f));
    }

    @Test
    void ltSemPegadaEmPeTemQuatroQuedasComDestinosProprios() {
        Object[][] casos = {
            {Direcao.CIMA, CatalogoTecnicasSolo.SINGLE_LEG, Posicao.MEIA_GUARDA},
            {Direcao.DIREITA, CatalogoTecnicasSolo.HIGH_CROTCH, Posicao.CONTROLE_LATERAL},
            {Direcao.BAIXO, CatalogoTecnicasSolo.CARREGADA_OMBRO, Posicao.CONTROLE_LATERAL},
            {Direcao.ESQUERDA, CatalogoTecnicasSolo.ANKLE_PICK, Posicao.EM_PE_CONTRA_SOLO}};
        for (Object[] c : casos) {
            novo(300, c[1] == CatalogoTecnicasSolo.ANKLE_PICK ? 420 : 450, false);   // o ankle pick e mais curto
            assertEquals(Posicao.EM_PE_SEPARADOS, m.getPosicao());
            int x0 = a.getX();
            MotorSolo.Resposta r = cmd(Lado.A, (Direcao) c[0], Familia.L2);
            assertEquals(Resultado.INICIADA, r.resultado(), c[1] + ": " + r.motivo());
            assertEquals(c[1], r.tecnica().id());
            ticks(4);
            assertTrue(a.getX() > x0, c[1] + ": avanca de verdade ate o outro");
            assertTrue(a.getX() - x0 <= 4 * MotorSolo.VELOCIDADE_AVANCO_QUEDA, "sem teletransporte");
            resolver();
            assertEquals(c[2], m.getPosicao(), c[1] + " sem reacao");
            assertTrue(b.isDeitado(), c[1] + ": o outro caiu");
            if (c[2] == Posicao.EM_PE_CONTRA_SOLO) {
                assertFalse(a.isDeitado() || a.isAjoelhado(), "no ankle pick quem pegou fica em pe");
            } else {
                assertEquals(Lado.A, m.getEstado().getPorCima().orElseThrow());
            }
        }
    }

    @Test
    void longeNaoEntraEMuitoLongeRecusa() {
        novo(100, 600, false);
        assertEquals("LONGE", cmd(Lado.A, Direcao.CIMA, Familia.L2).motivo());
    }

    @Test
    void sprawlParaAsEntradasNasPernas() {
        novo(300, 450, false);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.DIREITA, Familia.L2).resultado());
        ticks(3);
        b.iniciarSprawl();
        ticks(2);
        assertTrue(m.getTentativa().isEmpty(), "o sprawl parou a entrada");
        assertEquals("SPRAWL", m.getUltimoEvento().tipo());
        assertTrue(a.isAjoelhado() || a.getTransicaoAtiva() != null, "quem entrou fica de joelhos");
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow(), "quem fez o sprawl controla");
    }

    @Test
    void baseComRtSeguraOAnklePickERecuarFazFalhar() {
        novo(300, 420, false);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.ESQUERDA, Familia.L2).resultado());
        ib = new Intencao(0, 0, true);                         // RT: base
        resolver();
        assertEquals("DEFENDIDA", m.eventos().stream().filter(e -> e.tecnica().equals(CatalogoTecnicasSolo.ANKLE_PICK))
                .reduce((x, y) -> y).orElseThrow().tipo());
        assertFalse(b.isDeitado(), "segurou com a base");

        novo(300, 420, false);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.L2).resultado());
        ib = new Intencao(1, 0, false);                        // o J2 recua (para longe do J1)
        resolver();
        assertFalse(b.isDeitado(), "recuou: a entrada falhou");
        assertTrue(m.eventos().stream().anyMatch(e -> "DEFENDIDA".equals(e.tipo())
                && e.detalhe().contains("recuou")), "explica o motivo");
    }

    @Test
    void deJoelhosOMesmoLtValeAsDerrubadasDeJoelhos() {
        Object[][] casos = {
            {Direcao.CIMA, CatalogoTecnicasSolo.SINGLE_LEG_JOELHOS, Posicao.MEIA_GUARDA},
            {Direcao.DIREITA, CatalogoTecnicasSolo.DOUBLE_LEG_JOELHOS, Posicao.CONTROLE_LATERAL},
            {Direcao.ESQUERDA, CatalogoTecnicasSolo.ARM_DRAG, Posicao.COSTAS},
            {Direcao.BAIXO, CatalogoTecnicasSolo.SNAP_DOWN, Posicao.CONTROLE_LATERAL}};
        for (Object[] c : casos) {
            novo(300, 400, true);
            assertTrue(a.isAjoelhado() && b.isAjoelhado());
            MotorSolo.Resposta r = cmd(Lado.A, (Direcao) c[0], Familia.L2);
            assertEquals(Resultado.INICIADA, r.resultado(), c[1] + ": " + r.motivo());
            assertEquals(c[1], r.tecnica().id(), "de joelhos e outra tecnica");
            int yAntes = a.getY();
            ticks(3);
            assertTrue(a.isAjoelhado(), "nao levanta para entrar");
            assertEquals(yAntes, a.getY());
            resolver();
            assertEquals(c[2], m.getPosicao(), c[1].toString());
        }
        novo(300, 400, false);
        assertEquals(CatalogoTecnicasSolo.SINGLE_LEG, cmd(Lado.A, Direcao.CIMA, Familia.L2).tecnica().id(),
                "em pe continua a de pe");
    }

    @Test
    void bodyLockNoRbDireitaSemPegadaDosDoisLados() {
        for (Lado x : Lado.values()) {
            novo(300, 420, false);
            MotorSolo.Resposta r = cmd(x, Direcao.DIREITA, Familia.R1);
            assertEquals(CatalogoTecnicasSolo.BODY_LOCK, r.tecnica().id(), r.motivo());
            resolver();
            assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao());
            assertEquals(x, m.getEstado().getPorCima().orElseThrow());
        }
    }

    @Test
    void animacoesProprias() {
        for (String id : QuedasExplosivas.ids()) {
            assertTrue(AnimacoesTecnicas.tem(id + ":ATACA"), id);
            assertTrue(AnimacoesTecnicas.tem(id + ":RECEBE"), id);
            assertTrue(MotorSolo.ANIMACAO_PROPRIA.contains(id), id);
        }
        for (String id : new String[] {CatalogoTecnicasSolo.PUXAR_ARANHA, CatalogoTecnicasSolo.PUXAR_LACO}) {
            assertTrue(AnimacoesTecnicas.tem(id + ":ATACA") && AnimacoesTecnicas.tem(id + ":RECEBE"), id);
        }
        assertNotEquals(CatalogoTecnicasSolo.ENTRADA_PERNAS, CatalogoTecnicasSolo.SINGLE_LEG, "a PERNAS continua");
        assertTrue(AnimacoesTecnicas.tem("entrada_pernas:ATACA"), "a animacao aprovada continua");
    }
}
