package com.irmaosmiau.treino;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.regras.Graduacao;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.2: situacoes do laboratorio no MotorSolo (estado valido, papeis, reinicio, catalogo). */
class LaboratorioTreinoTest {

    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;
    private final LaboratorioTreino lab = new LaboratorioTreino();

    private void novo(boolean doisBrancos) {
        a = new IrmaoMiauBranco(250, 150, 0.7);
        b = doisBrancos ? new IrmaoMiauBranco(520, 150, 0.7) : new IrmaoMiauPreto(520, 150, 0.7);
        b.descerLinha();
        b.descerLinha();                                       // comecam em linhas diferentes de proposito
        m = new MotorSolo(a, b, null, 15, 715);
    }

    private IrmaoMiau l(Lado x) {
        return x == Lado.A ? a : b;
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            m.informarIntencoes(MotorSolo.Intencao.NENHUMA, MotorSolo.Intencao.NENHUMA);
            m.atualizar(MotorSolo.Intencao.NENHUMA, MotorSolo.Intencao.NENHUMA);
            a.atualizar(false, false);
            b.atualizar(false, false);
        }
    }

    private void conferir(SituacaoTreino s, Lado ativo) {
        String caso = s + " ativo " + ativo;
        assertEquals(s.getPosicao(), m.getPosicao(), caso);
        assertEquals(s.porCima(ativo), m.getEstado().getPorCima().orElse(null), caso + ": por cima");
        assertEquals(s.controlador(ativo), m.getEstado().getControlador().orElse(null), caso + ": controla");
        assertEquals(a.getLinhaAtual(), b.getLinhaAtual(), caso + ": mesma linha");
        boolean vinculo = s.getPosicao().isVinculoDeSolo();
        assertEquals(vinculo, a.isVinculadoNoSolo(), caso);
        assertEquals(vinculo, b.isVinculadoNoSolo(), caso);
        if (s.porCima(ativo) != null) {
            Lado cima = s.porCima(ativo);
            int d = Math.abs(l(cima).getX() - l(cima.outro()).getX());
            assertEquals(Math.abs(MotorSolo.afastamento(s.getPosicao())), d, 3, caso + ": distancia da posicao");
            if (s.getPosicao() != Posicao.COSTAS) {
                assertTrue(l(cima.outro()).isDeitado(), caso + ": quem esta por baixo deitado");
            }
            if (s.getPosicao().isGuardaComOutroEmPe() || s.getPosicao() == Posicao.EM_PE_CONTRA_SOLO) {
                assertTrue(!l(cima).isDeitado() && !l(cima).isAjoelhado(), caso + ": quem esta por cima em pe");
            }
        }
        if (s.exigeManga()) {
            assertTrue(m.temConexao(ativo, MotorSolo.Conexao.MANGA), caso + ": com a manga");
        }
        assertTrue(m.getTentativa().isEmpty() && m.getAtaque().isEmpty() && m.getVitoria().isEmpty(), caso);
    }

    @Test
    void todasAsSituacoesComecamValidasNosDoisSentidos() {
        for (SituacaoTreino s : SituacaoTreino.values()) {
            if (s == SituacaoTreino.EM_PE_PEGADA) {
                continue;                                      // precisa do SistemaCombate (teste no GamePanel)
            }
            for (Lado ativo : Lado.values()) {
                for (boolean iguais : new boolean[] {false, true}) {
                    novo(iguais);
                    lab.setSituacao(s);
                    lab.setAtivo(ativo);
                    lab.aplicar(m, null, a, b);
                    ticks(30);                                 // fim das transicoes de postura
                    conferir(s, ativo);
                    assertTrue(lab.isAtiva());
                }
            }
        }
    }

    @Test
    void tecnicaLiberadaComecaEBloqueadaContinuaBloqueada() {
        novo(false);
        lab.setSituacao(SituacaoTreino.COSTAS);
        lab.setAtivo(Lado.B);                                  // J2 nas costas do J1
        lab.aplicar(m, null, a, b);
        ticks(30);
        List<LaboratorioTreino.Item> cat = lab.catalogo(m);
        // complemento 5Q.8 (pedido): + as 4 da roda RT das costas
        assertEquals(12, cat.size(), "as 12 finalizacoes das costas, so para quem controla");
        assertTrue(cat.stream().allMatch(i -> i.lado() == Lado.B));
        LaboratorioTreino.Item mata = cat.stream().filter(i -> i.finalizacao().tecnicaBase().equals("mata_leao"))
                .findFirst().orElseThrow();
        assertEquals(LaboratorioTreino.Status.LIBERADA, mata.status(), "faixa branca: mata-leao liberado");
        assertTrue(cat.stream().anyMatch(i -> i.status() == LaboratorioTreino.Status.GRADUACAO), "outras pedem faixa");
        assertEquals(MotorSolo.Resultado.INICIADA, m.comando(Lado.B,
                new ComandoSolo(mata.finalizacao().direcao(), mata.finalizacao().familia())).resultado());
        LaboratorioTreino.Item arco = cat.stream().filter(i -> i.finalizacao().tecnicaBase().equals("arco_flecha"))
                .findFirst().orElseThrow();
        assertEquals(LaboratorioTreino.Status.GRADUACAO, arco.status());
        assertTrue(arco.texto().contains("FAIXA AZUL"), arco.texto());
    }

    @Test
    void reiniciarRestauraPosicaoPapeisERecursosDepoisDeUmEscape() {
        novo(false);
        lab.setSituacao(SituacaoTreino.COSTAS);
        lab.setAtivo(Lado.A);
        lab.aplicar(m, null, a, b);
        ticks(30);
        m.comando(Lado.A, new ComandoSolo(com.irmaosmiau.posicional.TecnicaSolo.Direcao.CIMA,
                com.irmaosmiau.posicional.TecnicaSolo.Familia.L2));           // mata-leao
        for (int i = 0; i < 40 && m.getAtaque().map(x -> x.fase() == MotorSolo.FaseFinalizacao.ENTRADA).orElse(false); i++) {
            ticks(1);
        }
        m.comando(Lado.B, new ComandoSolo(com.irmaosmiau.posicional.TecnicaSolo.Direcao.BAIXO, false));   // defende
        ticks(MotorSolo.RECARGA_ACAO_FINALIZACAO);
        m.comando(Lado.B, new ComandoSolo(com.irmaosmiau.posicional.TecnicaSolo.Direcao.CIMA, true));     // escapa
        assertEquals(Posicao.GUARDA, m.getPosicao(), "escapou para a guarda");
        assertTrue(m.getEstado().getPorCima().isPresent() && m.getEstado().getControlador().isPresent(),
                "depois do escape a posicao continua valida");
        assertTrue(a.getFolegoAtual() < a.getAtributos().getFolego());
        // reiniciar a situacao = lutadores recriados pela sessao + aplicar de novo
        novo(false);
        lab.aplicar(m, null, a, b);
        ticks(30);
        conferir(SituacaoTreino.COSTAS, Lado.A);
        assertEquals(a.getAtributos().getFolego(), a.getFolegoAtual(), 1e-9, "folego restaurado");
    }

    @Test
    void inverterPapeisEGraduacaoDoLutadorPreservada() {
        novo(true);
        a.setGraduacao(Graduacao.ROXA);
        lab.setSituacao(SituacaoTreino.MONTADA);
        lab.inverterPapeis();
        assertEquals(Lado.B, lab.getAtivo());
        lab.aplicar(m, null, a, b);
        ticks(30);
        conferir(SituacaoTreino.MONTADA, Lado.B);
        assertEquals(Graduacao.ROXA, a.getGraduacao(), "o laboratorio nao mexe na faixa");
        List<String> hud = lab.linhasHud(m, a, b, "-", "-");
        assertTrue(hud.get(0).contains("MONTADA") && hud.get(0).contains("CONTROLA J2"), hud.get(0));
        assertTrue(hud.get(1).contains("J1 FAIXA ROXA"), hud.get(1));
        assertFalse(hud.stream().anyMatch(x -> x.startsWith("BLOQUEIO")));
        assertTrue(lab.linhasHud(m, a, b, "GRADUAÇÃO AZUL", "-").get(3).contains("J1 GRADUAÇÃO AZUL"));
    }
}
