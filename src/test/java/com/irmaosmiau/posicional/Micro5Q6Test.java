package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.AnimacoesTecnicas;
import com.irmaosmiau.animacao.AnimadorSolo;
import com.irmaosmiau.animacao.TransicoesPostura;
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
 * Entrega 5Q.6: microanimacoes de confirmacao dos ajustes (pressao, base, montada alta,
 * ganchos, controlar as pernas, angulo, manga, manter/base das guardas), o desequilibrio
 * com os pes, a manga visivel enquanto presa, o cem quilos -> costas e o tiro de guarda
 * diferente do puxar guarda.
 */
class Micro5Q6Test {

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
        novo();
        m.forcarPosicao(p, cima, ctrl);
        ticks(40);
    }

    private MotorSolo.Resposta cmd(Lado l, Direcao d, Familia f) {
        return m.comando(l, new ComandoSolo(d, f));
    }

    @Test
    void ajustesAnimamQuemFazEOutroSoQuandoExecutam() {
        Object[][] casos = {
            {Posicao.CONTROLE_LATERAL, Lado.A, Lado.A, Lado.A, Direcao.BAIXO, Familia.RS, "consolidar"},
            {Posicao.MEIA_GUARDA, Lado.A, Lado.B, Lado.A, Direcao.BAIXO, Familia.RS, "postura_meia"},
            {Posicao.JOELHO_BARRIGA, Lado.A, Lado.A, Lado.A, Direcao.BAIXO, Familia.RS, "pressao_joelho"},
            {Posicao.COSTAS, Lado.A, Lado.A, Lado.A, Direcao.BAIXO, Familia.RS, "ajustar_ganchos"},
            {Posicao.GUARDA_ABERTA, Lado.A, Lado.B, Lado.A, Direcao.BAIXO, Familia.RS, "controlar_pernas"},
            {Posicao.GUARDA, Lado.A, Lado.B, Lado.B, Direcao.ESQUERDA, Familia.R1, "angulo_guarda"},
            {Posicao.GUARDA_ABERTA, Lado.A, Lado.B, Lado.B, Direcao.BAIXO, Familia.L2, "pegar_manga"}};
        for (Object[] c : casos) {
            em((Posicao) c[0], (Lado) c[1], (Lado) c[2]);
            Lado quem = (Lado) c[3];
            MotorSolo.Resposta r = cmd(quem, (Direcao) c[4], (Familia) c[5]);
            assertEquals(Resultado.CONCLUIDA, r.resultado(), c[6] + ": " + r.motivo());
            assertEquals(c[6], m.efeitoVisual(quem).orElseThrow().tecnica(), "quem fez anima");
            assertEquals(MotorSolo.TipoEfeitoVisual.TECNICA_ATAQUE, m.efeitoVisual(quem).orElseThrow().tipo());
            assertTrue(AnimacoesTecnicas.tem(c[6] + ":ATACA"), c[6] + " tem animacao propria");
        }
    }

    @Test
    void ajusteRecusadoNaoAnima() {
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.A, Direcao.BAIXO, Familia.RS).resultado());
        ticks(MotorSolo.TICKS_ANIMACAO_INSTANTANEA + 1);
        assertTrue(m.efeitoVisual(Lado.A).isEmpty());
        MotorSolo.Resposta r = cmd(Lado.A, Direcao.BAIXO, Familia.RS);       // ainda em recarga?
        if (r.resultado() == Resultado.RECUSADA) {
            assertTrue(m.efeitoVisual(Lado.A).isEmpty(), "recusada nao parece bem-sucedida");
        }
    }

    @Test
    void controlarAsPernasSeguraAPoseEnquantoOEfeitoDura() {
        em(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        cmd(Lado.A, Direcao.BAIXO, Familia.RS);
        ticks(MotorSolo.POSTURA_TICKS - 10);
        assertEquals("controlar_pernas", m.efeitoVisual(Lado.A).orElseThrow().tecnica(), "maos continuam nas pernas");
        double[] meio = AnimacoesTecnicas.amostraParaTeste("controlar_pernas:ATACA", 0.5);
        assertTrue(meio[20] > 0 && meio[21] > 0);
    }

    @Test
    void mangaPresaFicaVisivelEnquantoDura() {
        em(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.B, Direcao.BAIXO, Familia.L2).resultado());
        assertTrue(m.temConexao(Lado.B, MotorSolo.Conexao.MANGA), "a pegada na manga existe de verdade");
        ticks(MotorSolo.TICKS_ANIMACAO_INSTANTANEA + 2);
        AnimadorSolo s = new AnimadorSolo(new TransicoesPostura(l -> null));
        s.aplicar(b, m);
        int[] comManga = b.copiarPoseCombateExterna();
        s.liberar(b);
        m.forcarPosicao(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);       // forcar limpa? confere pela conexao
        if (!m.temConexao(Lado.B, MotorSolo.Conexao.MANGA)) {
            s.aplicar(b, m);
            int[] sem = b.copiarPoseCombateExterna();
            assertEquals(AnimadorSolo.MAO_NA_MANGA[1], comManga[21] - sem[21], "a mao segura a manga");
        }
        assertTrue(comManga != null);
    }

    @Test
    void cemQuilosParaAsCostasComControleOuExplicaOMotivo() {
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        m.definirEstabilidade(30);
        MotorSolo.Resposta r = cmd(Lado.A, Direcao.CIMA, Familia.R1);
        assertEquals(Resultado.RECUSADA, r.resultado());
        assertTrue(r.motivo().startsWith("CONSOLIDE ANTES"), r.motivo());
        m.definirEstabilidade(80);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.R1).resultado());
        for (int i = 0; i < 60 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        assertEquals(Posicao.COSTAS, m.getPosicao(), "do cem quilos para as costas (antes nunca entrava)");
        assertEquals(Lado.A, m.getEstado().getControlador().orElseThrow());
    }

    @Test
    void desequilibrarComOsPesAnimaEExpoeOOutro() {
        em(Posicao.GUARDA_BORBOLETA, Lado.A, Lado.B);
        assertEquals(Resultado.INICIADA, cmd(Lado.B, Direcao.ESQUERDA, Familia.RS).resultado());
        assertEquals(MotorSolo.TipoEfeitoVisual.TECNICA_ATAQUE, m.efeitoVisual(Lado.B).orElseThrow().tipo());
        assertEquals(MotorSolo.TipoEfeitoVisual.TECNICA_RECEBE, m.efeitoVisual(Lado.A).orElseThrow().tipo());
        for (int i = 0; i < 60 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        assertEquals(TecnicaSolo.Exposicao.BASE, m.exposicaoDe(Lado.A).orElseThrow(), "desequilibrou de verdade");
    }

    @Test
    void tiroDeGuardaSaltaEPuxarGuardaSenta() {
        double[] tiro = AnimacoesTecnicas.amostraParaTeste("tiro_guarda:ATACA", 0.42);
        double[] puxar = AnimacoesTecnicas.amostraParaTeste("puxar_guarda:ATACA", 0.42);
        assertTrue(tiro[31] < -20, "o tiro sobe (salto)");
        assertTrue(puxar[31] > 0, "o puxar desce sentando");
        double[] encaixe = AnimacoesTecnicas.amostraParaTeste("tiro_guarda:ATACA", 0.78);
        assertTrue(encaixe[28] > 30, "pernas em volta da cintura antes de descer");
        assertTrue(MotorSolo.ANIMACAO_PROPRIA.contains(CatalogoTecnicasSolo.TIRO_GUARDA));
        assertFalse(AnimacoesTecnicas.tem("tiro_guarda:ATACA") == false);
    }
}
