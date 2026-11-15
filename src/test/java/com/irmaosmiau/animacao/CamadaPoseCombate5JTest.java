package com.irmaosmiau.animacao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.ResultadoAcaoTecnica;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import org.junit.jupiter.api.Test;

/** Entrega 5J (testes 9, 10, 11, 12 e 13): passos do arrasto e reacao ao desequilibrio. */
class CamadaPoseCombate5JTest {

    private IrmaoMiau apl;
    private IrmaoMiau rec;
    private SistemaCombate s;
    private final CamadaPoseCombate camada = new CamadaPoseCombate();
    private long tick;

    private void montar(IrmaoMiau aplicador, IrmaoMiau receptor) {
        apl = aplicador;
        rec = receptor;
        apl.setViradoParaDireita(apl.getX() < rec.getX());
        rec.setViradoParaDireita(!apl.isViradoParaDireita());
        s = CarregadorConteudoCombate.carregarPadrao().criarSistema(apl, rec);
        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(apl, rec, "pegada_1"));
        for (int i = 0; i < 31; i++) {
            tick(false);
        }
        assertTrue(s.isInteracaoEstabelecida());
    }

    private void tick(boolean arrastou) {
        apl.atualizar(false, false);
        rec.atualizar(false, false);
        s.atualizar();
        camada.atualizar(apl, s, arrastou, tick);
        camada.atualizar(rec, s, arrastou, tick);
        tick++;
    }

    private void arrastar(int direcao, int ticks) {
        for (int i = 0; i < ticks; i++) {
            assertEquals(ResultadoAcaoTecnica.SUCESSO, s.deslocarPegadaControlada(apl, direcao, 15, 715));
            tick(true);
        }
    }

    private static void soPernasMudaram(int[] base, int[] agora) {
        for (int i = 0; i < 22; i++) {
            assertEquals(base[i], agora[i], "indice " + i + " (tronco/bracos/cabeca) nao muda no arrasto");
        }
        boolean mudou = false;
        for (int i = 22; i < 30; i++) {
            mudou |= base[i] != agora[i];
            assertTrue(Math.abs(base[i] - agora[i]) <= CamadaPoseCombate.PASSO, "passo de poucas unidades");
        }
        assertTrue(mudou, "as pernas deram um passo");
    }

    @Test
    void semArrastoAPoseDaPegadaFicaIntacta() {
        montar(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        int[] base = apl.copiarPoseCombateExterna();
        for (int i = 0; i < 60; i++) {
            tick(false);
        }
        assertArrayEquals(base, apl.copiarPoseCombateExterna(), "pegada mantida: nada de reentrada nem passo");
        assertEquals(-1, camada.faseArrasto(apl));
        assertFalse(s.isAnimacaoCombateAtiva(), "a entrada nao recomeca enquanto a pegada continua");
    }

    @Test
    void passosSoDuranteOArrastoParamERetomam() {
        montar(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        int[] baseA = apl.copiarPoseCombateExterna();
        int[] baseR = rec.copiarPoseCombateExterna();
        int xa = apl.getX();
        arrastar(1, 5);                                  // 5 px -> fase 1
        assertEquals(xa + 5, apl.getX(), "a camada nao muda a fisica (1 px por tick)");
        assertEquals(1, camada.faseArrasto(apl));
        soPernasMudaram(baseA, apl.copiarPoseCombateExterna());
        soPernasMudaram(baseR, rec.copiarPoseCombateExterna());
        tick(false);                                     // parou
        assertArrayEquals(baseA, apl.copiarPoseCombateExterna(), "parou: volta a pose exata (sem acumulo)");
        assertArrayEquals(baseR, rec.copiarPoseCombateExterna());
        assertEquals(-1, camada.faseArrasto(apl));
        arrastar(1, 3);                                  // 8 px no total -> fase 2 (continua, nao recomeca)
        assertEquals(2, camada.faseArrasto(apl));
        arrastar(1, 4);                                  // 12 px -> fase 3: a outra perna
        assertEquals(3, camada.faseArrasto(apl));
        soPernasMudaram(baseA, apl.copiarPoseCombateExterna());
        tick(false);
        assertArrayEquals(baseA, apl.copiarPoseCombateExterna());
    }

    @Test
    void arrastoParaAEsquerdaEComOPretoAplicador() {
        montar(new IrmaoMiauPreto(500, 150, 0.7), new IrmaoMiauBranco(440, 150, 0.7));
        int[] base = apl.copiarPoseCombateExterna();
        int xr = rec.getX();
        arrastar(-1, 5);
        assertEquals(xr - 5, rec.getX());
        assertEquals(1, camada.faseArrasto(apl));
        assertEquals(1, camada.faseArrasto(rec), "as pernas dos dois andam juntas");
        soPernasMudaram(base, apl.copiarPoseCombateExterna());
    }

    @Test
    void desequilibrioComSucessoInclinaOReceptorEVolta() {
        montar(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        int[] base = rec.copiarPoseCombateExterna();
        int[] baseA = apl.copiarPoseCombateExterna();
        int xr = rec.getX();
        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.tentarDesequilibrio(apl));
        tick(false);
        assertTrue(camada.intensidadeReacao(rec) > 0);
        for (int i = 0; i < 6; i++) {
            tick(false);
        }
        int[] reagindo = rec.copiarPoseCombateExterna();
        assertEquals(base[9] + CamadaPoseCombate.INCLINACAO_PEITO, reagindo[9], "tronco inclinado");
        assertEquals(base[6] + CamadaPoseCombate.INCLINACAO_CINTURA, reagindo[6]);
        assertArrayEquals(java.util.Arrays.copyOfRange(base, 14, 22), java.util.Arrays.copyOfRange(reagindo, 14, 22),
                "bracos continuam na pegada");
        assertArrayEquals(baseA, apl.copiarPoseCombateExterna(), "o aplicador nao reage");
        assertEquals(xr, rec.getX(), "reacao visual: nada de deslocar a posicao");
        for (int i = 0; i < CamadaPoseCombate.DURACAO_REACAO_TICKS; i++) {
            tick(false);
        }
        assertArrayEquals(base, rec.copiarPoseCombateExterna(), "volta a pose da pegada");
        assertEquals(0.0, camada.intensidadeReacao(rec));
        assertTrue(s.isInteracaoEstabelecida(), "resultado mecanico inalterado");
    }

    @Test
    void semReacaoQuandoATentativaNaoValeOuEResistida() {
        montar(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        int[] base = rec.copiarPoseCombateExterna();
        assertEquals(ResultadoAcaoTecnica.SEM_PEGADA, s.tentarDesequilibrio(rec), "o receptor nao desequilibra");
        tick(false);
        assertEquals(0.0, camada.intensidadeReacao(apl));
        assertEquals(0.0, camada.intensidadeReacao(rec));
        assertArrayEquals(base, rec.copiarPoseCombateExterna());

        IrmaoMiau fraco = new IrmaoMiauBranco(440, 150, 0.7, new AtributosLutador(35, 25, 5, 22, 25, 5, 22, 45));
        CamadaPoseCombate5JTest outro = new CamadaPoseCombate5JTest();
        outro.montar(fraco, new IrmaoMiauPreto(500, 150, 0.7));
        int[] baseOutro = outro.rec.copiarPoseCombateExterna();
        assertEquals(ResultadoAcaoTecnica.RESISTIDO, outro.s.tentarDesequilibrio(fraco));
        for (int i = 0; i < 8; i++) {
            outro.tick(false);
        }
        assertEquals(0.0, outro.camada.intensidadeReacao(outro.rec), "resistido: sem reacao");
        assertArrayEquals(baseOutro, outro.rec.copiarPoseCombateExterna());
    }

    @Test
    void reacaoInterrompidaPeloFimDaPegadaNaoDeixaPoseSobrando() {
        montar(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.tentarDesequilibrio(apl));
        tick(false);
        tick(false);
        assertTrue(camada.intensidadeReacao(rec) > 0);
        s.encerrarInteracao();
        tick(false);
        assertEquals(0.0, camada.intensidadeReacao(rec));
        assertFalse(rec.isPoseCombateExternaAtiva(), "fim da pegada limpa a pose (nada da camada sobra)");
        assertNotEquals(null, s);
    }

    @Test
    void envelopeCurtoESemValoresForaDaJanela() {
        assertEquals(0.0, CamadaPoseCombate.envelope(-1));
        assertEquals(1.0, CamadaPoseCombate.envelope(8));
        assertEquals(0.0, CamadaPoseCombate.envelope(CamadaPoseCombate.DURACAO_REACAO_TICKS));
        for (long t = 0; t < CamadaPoseCombate.DURACAO_REACAO_TICKS; t++) {
            double v = CamadaPoseCombate.envelope(t);
            assertTrue(v > 0 && v <= 1.0, "t=" + t);
        }
    }
}
