package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.6: terceira roda com a pegada (LB + LT): duas projecoes e duas entradas
 * de guarda, no GamePanel real; e as rodas de LT/LB com a pegada preservadas.
 */
class Projecoes5Q6GamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private MotorSolo.Resposta l12(Direcao d) {
        return m().comando(Lado.A, new ComandoSolo(d, Familia.L1_L2));
    }

    private void esperar() {
        for (int i = 0; i < 200 && m().getTentativa().isPresent(); i++) {
            ticks(1);
        }
        ticks(MotorSolo.ATERRISSAGEM_TICKS + 6);
    }

    @Test
    void projecoesDoLbLtDerrubam() throws Exception {
        naEdt(() -> {
            for (Direcao d : new Direcao[] {Direcao.CIMA, Direcao.DIREITA}) {
                novo(InicioLuta.EM_PE);
                j1Pega(1);
                ticks(20);
                MotorSolo.Resposta r = l12(d);
                assertEquals(d == Direcao.CIMA ? CatalogoTecnicasSolo.UCHI_MATA : CatalogoTecnicasSolo.OSOTO_GARI,
                        r.tecnica() == null ? r.motivo() : r.tecnica().id());
                esperar();
                assertTrue(j2().isDeitado() || m().getPosicao().isNoChao(), d + ": projetou");
                assertEquals(Lado.A, m().getEstado().getPorCima().orElse(null), "quem projetou fica por cima");
            }
        });
    }

    @Test
    void entradasDeGuardaDoLbLt() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(20);
            MotorSolo.Resposta r = l12(Direcao.BAIXO);
            assertEquals(CatalogoTecnicasSolo.PUXAR_LACO, r.tecnica().id(), r.motivo());
            esperar();
            assertEquals(Posicao.GUARDA_LACO, m().getPosicao());
            assertEquals(Lado.B, m().getEstado().getPorCima().orElseThrow(), "quem puxou fica por baixo");

            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(20);
            MotorSolo.Resposta sem = l12(Direcao.ESQUERDA);
            assertEquals("PRECISA DAS MANGAS", sem.motivo(), "a aranha precisa das mangas");
        });
    }

    @Test
    void rodasDoLtEDoLbComPegadaContinuamAsMesmas() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(20);
            assertEquals(CatalogoTecnicasSolo.RASTEIRA, m().tecnicaPara(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2))
                    .orElseThrow().id());
            assertEquals(CatalogoTecnicasSolo.TIRO_GUARDA, m().tecnicaPara(Lado.A,
                    new ComandoSolo(Direcao.CIMA, Familia.L1)).orElseThrow().id());
            assertEquals(CatalogoTecnicasSolo.QUEDA_PEGADA, m().tecnicaPara(Lado.A,
                    new ComandoSolo(Direcao.DIREITA, Familia.R1)).orElseThrow().id(), "com pegada o RB -> e a queda");
        });
    }
}
