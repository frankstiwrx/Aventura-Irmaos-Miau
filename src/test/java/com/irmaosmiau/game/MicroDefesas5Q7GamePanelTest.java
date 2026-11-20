package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.IntegridadeMembros;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.7 no GamePanel real: as acoes defensivas de quem esta por baixo (100 kg, montada,
 * costas) tem resposta visual curta, nos dois personagens, sem deformar os bracos; a TENTATIVA
 * que nao muda a posicao volta a pose anterior (a troca continua sendo do motor).
 */
class MicroDefesas5Q7GamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    /**
     * Executa o comando de quem esta por baixo e confere: o corpo mexe de verdade, os bracos nao
     * mudam de tamanho, a posicao nao muda (controle alto) e a pose volta ao fim.
     */
    private void confere(Posicao pos, Lado porCima, Direcao d, Familia f, String nome) {
        novo(InicioLuta.EM_PE);
        Lado baixo = porCima.outro();
        m().forcarPosicao(pos, porCima, porCima);
        ticks(50);
        m().definirEstabilidade(100);
        IrmaoMiau l = baixo == Lado.A ? j1() : j2();
        int[] antes = l.copiarPoseCombateExterna();
        assertNotNull(antes, nome);
        double[] bracos = IntegridadeMembros.comprimentos(antes, l.getBracosEmRepouso());
        MotorSolo.Resposta r = m().comando(baixo, new ComandoSolo(d, f));
        assertTrue(r.aceito(), nome + ": " + r.motivo());
        int mexeu = 0;
        for (int i = 0; i < 30; i++) {
            ticks(1);
            int[] p = l.copiarPoseCombateExterna();
            if (p == null) {
                continue;
            }
            for (int k = 4; k < 30; k++) {
                mexeu = Math.max(mexeu, Math.abs(p[k] - antes[k]));
            }
            double[] c = IntegridadeMembros.comprimentos(p, l.getBracosEmRepouso());
            for (int k = 0; k < 4; k++) {
                assertTrue(Math.abs(c[k] - bracos[k]) <= 2.5, nome + " tick " + i + ": braco " + k + " "
                        + Math.round(bracos[k]) + " -> " + Math.round(c[k]));
            }
        }
        assertTrue(mexeu >= 6, nome + ": a acao aparece no corpo (" + mexeu + ")");
        assertTrue(mexeu <= 40, nome + ": sutil (" + mexeu + ")");
        ticks(40);
        assertEquals(pos, m().getPosicao(), nome + ": a tentativa sozinha nao troca a posicao");
        assertArrayEquals(antes, l.copiarPoseCombateExterna(), nome + ": volta a pose anterior");
    }

    @Test
    void porBaixoDosCemQuilosRecuperarGuardaVirarEEnquadrar() throws Exception {
        naEdt(() -> {
            for (Lado cima : Lado.values()) {
                confere(Posicao.CONTROLE_LATERAL, cima, Direcao.BAIXO, Familia.RS, "enquadrar " + cima);
                confere(Posicao.CONTROLE_LATERAL, cima, Direcao.ESQUERDA, Familia.RS, "recuperar a guarda " + cima);
                confere(Posicao.CONTROLE_LATERAL, cima, Direcao.CIMA, Familia.RS, "virar " + cima);
            }
        });
    }

    @Test
    void porBaixoDaMontadaEscaparEEnquadrar() throws Exception {
        naEdt(() -> {
            for (Lado cima : Lado.values()) {
                confere(Posicao.MONTADA, cima, Direcao.ESQUERDA, Familia.RS, "escapar da montada " + cima);
                confere(Posicao.MONTADA, cima, Direcao.BAIXO, Familia.RS, "enquadrar na montada " + cima);
            }
        });
    }

    @Test
    void comAsCostasDominadasGirarEDefender() throws Exception {
        naEdt(() -> {
            for (Lado cima : Lado.values()) {
                confere(Posicao.COSTAS, cima, Direcao.ESQUERDA, Familia.RS, "girar " + cima);
                confere(Posicao.COSTAS, cima, Direcao.BAIXO, Familia.RS, "defender " + cima);
            }
        });
    }

    @Test
    void oEnquadrarContinuaTirandoControle() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
            ticks(50);
            m().definirEstabilidade(80);
            m().comando(Lado.B, new ComandoSolo(Direcao.BAIXO, Familia.RS));
            assertTrue(m().getEstabilidade() <= 80 - MotorSolo.DELTA_ENQUADRAR + 1, "efeito mecanico preservado");
        });
    }
}
