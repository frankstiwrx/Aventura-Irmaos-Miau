package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5P.2 no GamePanel real (J1 no teclado, J2 no teclado-controle):
 * acoes novas da pegada ativa (tiro de guarda, derrubada forcada) sem perder
 * reforco/desequilibrio/romper; familias L1/L2/L1+L2 do J2 (E/Q); R1 na guarda
 * fechada; desengajar de volta a luta em pe.
 */
class Familias5P2GamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void ctl(String tecla, boolean p) {
        tecla("pretoControle" + tecla, p);
    }

    private void toqueCtl(String tecla) {
        ctl(tecla, true);
        ticks(1);
        ctl(tecla, false);
        ticks(1);
    }

    private void j2Pega() {
        ctl("A", true);
        for (int i = 0; i < 400 && !s().podeTentarAcao(j2(), j1(), "pegada_1"); i++) {
            ticks(1);
        }
        ctl("A", false);
        ticks(2);
        toqueCtl("UP");
        ticks(40);
        assertTrue(s().isInteracaoEstabelecida());
        assertEquals(j2(), s().getAplicadorInteracao());
    }

    private void esperarResolver() {
        for (int i = 0; i < 120 && m().getTentativa().isPresent(); i++) {
            ticks(1);
        }
        ticks(2);
    }

    // ---------------- 4. pegada ativa: o que ja existia continua
    @Test
    void reforcoEDesequilibrioContinuamEOReceptorAindaRompe() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(60);
            double f = s().getFirmeza();
            toque("brancoReforco");
            assertTrue(s().getFirmeza() > f, "reforco");
            PainelDpadContextual.Quadro q = painel.montarQuadroDpad(SlotJogador.JOGADOR_2);
            assertEquals("ROMPER", PainelDpadContextual.texto(q, DirecaoTecnica.BAIXO), "o receptor continua rompendo");
            assertFalse(PainelDpadContextual.texto(q, DirecaoTecnica.ESQUERDA).contains("TIRO"),
                    "tiro de guarda e so de quem aplica");
        });
    }

    @Test
    void tiroDeGuardaPelaTeclaFechaAGuarda() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(30);
            toque("brancoPegada4");                             // <- = tiro de guarda
            assertEquals(CatalogoTecnicasSolo.TIRO_GUARDA, m().getTentativa().orElseThrow().tecnica().id());
            assertTrue(s().isInteracaoEstabelecida(), "nao e instantaneo: a pegada segue na janela");
            esperarResolver();
            assertEquals(Posicao.GUARDA, m().getPosicao());
            assertEquals(Lado.A, m().getEstado().getControlador().orElseThrow(), "J1 fechou a guarda");
            assertFalse(s().estaEmInteracao(), "a pegada virou o vinculo do solo");
            assertTrue(j1().isVinculadoNoSolo() && j2().isVinculadoNoSolo());
        });
    }

    @Test
    void tiroDeGuardaComBaseDoOutroDeixaQuemSaltouNoChaoSemGuarda() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(30);
            toque("brancoPegada4");
            toqueCtl("P");                                      // J2 aperta o RT na janela: base
            esperarResolver();
            assertEquals(Posicao.EM_PE_CONTRA_SOLO, m().getPosicao());
            assertTrue(j1().isDeitado(), "caiu");
            assertEquals(Lado.B, m().getEstado().getPorCima().orElseThrow(), "J2 ficou em pe");
            assertTrue(m().getEstado().getControlador().isEmpty(), "sem guarda estabelecida");
            assertFalse(s().estaEmInteracao());
        });
    }

    @Test
    void derrubadaForcadaPrecisaTirarABaseEBaseFirmeExpoeQuemForca() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(30);
            toque("brancoPegada3");                             // v = derrubada forcada
            assertEquals(CatalogoTecnicasSolo.DERRUBADA_FORCADA, m().getTentativa().orElseThrow().tecnica().id());
            esperarResolver();
            assertTrue(s().isInteracaoEstabelecida(), "a primeira so desequilibra (nao e queda automatica)");
            assertTrue(m().getBase(Lado.B) < MotorSolo.BASE_MAXIMA);
            toque("brancoPegada3");
            esperarResolver();
            assertTrue(j2().isDeitado() || m().isAterrissando() || m().getPosicao().isNoChao(), "sem base, foi ao chao");

            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(30);
            toque("brancoPegada3");
            toqueCtl("P");                                      // base firme
            esperarResolver();
            assertEquals(com.irmaosmiau.posicional.TecnicaSolo.Exposicao.AVANCO, m().exposicaoDe(Lado.A).orElseThrow(),
                    "quem forcou fica com o avanco exposto");
            assertFalse(j2().isDeitado());
        });
    }

    // ---------------- 3. familias do J2 no teclado-controle (Q = LT/L2, E = LB/L1)
    @Test
    void j2UsaL1L2EL1MaisL2PeloTecladoVirtual() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j2Pega();
            ticks(30);
            ctl("E", true);
            ticks(1);
            PainelDpadContextual.Quadro q = painel.montarQuadroDpad(SlotJogador.JOGADOR_2);
            assertTrue(q.nomeCamada().contains("GUARDAS"), q.nomeCamada());
            assertEquals("TIRO GUARDA", PainelDpadContextual.texto(q, DirecaoTecnica.CIMA));
            assertEquals("PUXAR GUARDA", PainelDpadContextual.texto(q, DirecaoTecnica.BAIXO));
            ctl("Q", true);                                     // L1 + L2: a familia combinada vence
            ticks(1);
            q = painel.montarQuadroDpad(SlotJogador.JOGADOR_2);
            assertTrue(q.nomeCamada().contains("ESPECIAIS"), q.nomeCamada());
            toqueCtl("UP");
            assertTrue(m().getTentativa().isEmpty(), "L1+L2 em pe nao dispara tiro (L1) nem rasteira (L2)");
            assertTrue(s().isInteracaoEstabelecida());
            ctl("E", false);
            ticks(1);
            q = painel.montarQuadroDpad(SlotJogador.JOGADOR_2);
            assertTrue(q.nomeCamada().contains("QUEDAS"), q.nomeCamada());
            assertEquals("RASTEIRA", PainelDpadContextual.texto(q, DirecaoTecnica.CIMA));
            toqueCtl("UP");                                     // L2 + cima = rasteira
            ctl("Q", false);
            assertEquals(CatalogoTecnicasSolo.RASTEIRA, m().getTentativa().orElseThrow().tecnica().id());
            assertEquals(Lado.B, m().getTentativa().orElseThrow().iniciador());
        });
    }

    // ---------------- emenda / R1: guarda fechada por baixo tem acoes; desengajar volta em pe
    @Test
    void r1NaGuardaFechadaPorBaixoTemAcoesEAfastarVoltaALutaEmPe() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.GUARDA, Lado.A, Lado.B);   // J2 por baixo, guarda fechada
            ticks(40);
            ctl("O", true);                                     // RB do J2
            ticks(1);
            PainelDpadContextual.Quadro q = painel.montarQuadroDpad(SlotJogador.JOGADOR_2);
            assertEquals("DESEQ.", PainelDpadContextual.texto(q, DirecaoTecnica.CIMA));
            assertEquals("ÂNGULO", PainelDpadContextual.texto(q, DirecaoTecnica.ESQUERDA));
            assertEquals("AFASTAR", PainelDpadContextual.texto(q, DirecaoTecnica.BAIXO));
            toqueCtl("DOWN");                                   // R1 + v = abrir e afastar
            ctl("O", false);
            esperarResolver();
            assertEquals(Posicao.EM_PE_CONTRA_SOLO, m().getPosicao());
            assertFalse(j1().isVinculadoNoSolo() || j2().isVinculadoNoSolo(), "vinculo encerrado");
            ticks(30);
            toqueCtl("UP");                                     // levantar
            esperarResolver();
            ticks(30);
            assertEquals(Posicao.EM_PE_SEPARADOS, m().getPosicao());
            int x = j2().getX();
            ctl("D", true);
            ticks(10);
            ctl("D", false);
            assertTrue(j2().getX() > x, "J2 anda de novo");
        });
    }

    @Test
    void reiniciarLutaLimpaConexoesEEventos() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
            m().adicionarConexao(Lado.B, MotorSolo.Conexao.MANGA);
            m().reiniciar();
            assertFalse(m().temConexao(Lado.B, MotorSolo.Conexao.MANGA));
            assertTrue(m().consumirEventosLuta().isEmpty());
            assertEquals(Posicao.EM_PE_SEPARADOS, m().getPosicao());
        });
    }
}
