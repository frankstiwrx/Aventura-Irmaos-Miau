package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.NaturezaAcao;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.ClassificacaoAcoes;
import com.irmaosmiau.posicional.EventoLuta;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.7 no GamePanel real: PES no solo livre com deslocamento de verdade e a nova pegada
 * SEGURAR AS PERNAS (arrastar pelo tatame, mais devagar, com folego, rompimento e soltura).
 */
class Arrasto5Q7GamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void derrubarJ2() {
        j1Pega(1);
        tecla("brancoControle", true);
        tecla("brancoEsquerda", true);
        ticks(6);
        tecla("brancoEsquerda", false);
        tecla("brancoControle", false);
        m().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));
        ticks(80);
        assertTrue(j2().isDeitado(), "caiu");
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m().getPosicao());
    }

    /** J1 encosta em J2 (no alcance de SEGURAR) e segura as pernas. */
    private void segurar() {
        String ate = j2().getX() > j1().getX() ? "brancoDireita" : "brancoEsquerda";
        for (int i = 0; i < 200 && Math.abs(j1().getX() - j2().getX()) > 90; i++) {
            tecla(ate, true);
            ticks(1);
        }
        tecla(ate, false);
        ticks(2);
        MotorSolo.Resposta r = m().comando(Lado.A, new ComandoSolo(Direcao.DIREITA, Familia.RS));
        assertEquals("Segurar as pernas (arrastar)", r.tecnica().nome(), "RS -> agora e SEGURAR: " + r.motivo());
        ticks(30);
        assertEquals(Lado.A, m().getSeguradorPernas().orElse(null), "segurou");
    }

    /** Tecla de J1 que se AFASTA de J2 (arrasta o outro para longe de onde estava). */
    private String longeDeJ2() {
        return j2().getX() > j1().getX() ? "brancoEsquerda" : "brancoDireita";
    }

    @Test
    void pesNoChaoDeslizamDeVerdadeParaTrasSemSalto() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            derrubarJ2();
            int d0 = Math.abs(j1().getX() - j2().getX());
            int x = j2().getX();
            MotorSolo.Resposta r = m().comando(Lado.B, new ComandoSolo(Direcao.BAIXO, Familia.RS));
            assertTrue(r.aceito(), "PES aceito: " + r.motivo());
            int max = 0;
            for (int i = 0; i < 30; i++) {
                ticks(1);
                max = Math.max(max, Math.abs(j2().getX() - x));
                x = j2().getX();
                assertTrue(j2().isDeitado(), "continua deitado");
            }
            assertTrue(max <= 2, "suave (sem teletransporte): " + max);
            int d1 = Math.abs(j1().getX() - j2().getX());
            assertTrue(d1 >= d0 + MotorSolo.RECUO_PES_PX - 2, "a distancia aumentou de verdade: " + d0 + " -> " + d1);
        });
    }

    @Test
    void segurarAsPernasEArrastarDevagarComFolego() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            derrubarJ2();
            segurar();
            int x1 = j1().getX();
            int x2 = j2().getX();
            double folego = j1().getFolegoAtual();
            String tecla = longeDeJ2();
            tecla(tecla, true);
            ticks(40);
            tecla(tecla, false);
            ticks(2);
            int andou1 = Math.abs(j1().getX() - x1);
            int andou2 = Math.abs(j2().getX() - x2);
            assertTrue(andou2 > 5, "J2 foi arrastado pelo chao: " + andou2);
            assertTrue(Math.abs(andou1 - andou2) <= 2, "os dois andam juntos (a distancia fica): " + andou1 + "/" + andou2);
            assertTrue(j2().isDeitado(), "J2 continua deitado");
            assertTrue(j1().getFolegoAtual() < folego, "arrastar gasta folego");
            assertEquals(Lado.A, m().getSeguradorPernas().orElse(null), "ainda segura");
            // sem segurar, o mesmo tempo andando vai bem mais longe
            novo(InicioLuta.EM_PE);
            int livre0 = j1().getX();
            tecla("brancoEsquerda", true);
            ticks(40);
            tecla("brancoEsquerda", false);
            int livre = Math.abs(j1().getX() - livre0);
            assertTrue(andou1 * 2 < livre, "arrastando e bem mais lento: " + andou1 + " x " + livre);
        });
    }

    @Test
    void oSeguradoRompeSeDebatendoEOSeguradorSoltaPeloMesmoComando() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            derrubarJ2();
            segurar();
            // J2 se debate (RT segurado e direcao): rompe com relativa facilidade
            tecla("pretoCima", true);
            ticks((int) MotorSolo.RESISTENCIA_PARA_ROMPER + 5);
            tecla("pretoCima", false);
            assertTrue(m().getSeguradorPernas().isEmpty(), "J2 rompeu a pegada");
            assertTrue(painel.avisoAtual() != null);
            // segura de novo e solta pelo mesmo comando (RS ->)
            ticks(40);
            segurar();
            MotorSolo.Resposta r = m().comando(Lado.A, new ComandoSolo(Direcao.DIREITA, Familia.RS));
            assertEquals(CatalogoTecnicasSolo.SOLTAR_PERNAS, r.tecnica().id());
            assertTrue(m().getSeguradorPernas().isEmpty(), "soltou");
        });
    }

    @Test
    void oSeguradoUsandoOsPesTambemRompe() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            derrubarJ2();
            segurar();
            ticks(20);
            MotorSolo.Resposta r = m().comando(Lado.B, new ComandoSolo(Direcao.BAIXO, Familia.RS));
            assertTrue(r.aceito(), r.motivo());
            assertTrue(m().getSeguradorPernas().isEmpty(), "os pes empurram as maos: rompeu");
        });
    }

    @Test
    void arrastoNaoAtravessaABordaDaArena() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            derrubarJ2();
            segurar();
            String tecla = longeDeJ2();
            tecla(tecla, true);
            for (int i = 0; i < 900 && m().getSeguradorPernas().isPresent(); i++) {
                ticks(1);
                assertTrue(j2().getX() >= GamePanel.LIMITE_ESQUERDO_ARENA - 1
                        && j2().getX() <= GamePanel.LIMITE_DIREITO_ARENA + 1, "J2 dentro da arena: " + j2().getX());
                assertTrue(j1().getX() >= GamePanel.LIMITE_ESQUERDO_ARENA - 1
                        && j1().getX() <= GamePanel.LIMITE_DIREITO_ARENA + 1, "J1 dentro da arena: " + j1().getX());
            }
            tecla(tecla, false);
        });
    }

    @Test
    void oPrenderContinuaNoOutroLadoESegurarNaoEQuedaNemFinalizacao() {
        CatalogoTecnicasSolo c = CatalogoTecnicasSolo.referencia();
        assertTrue(c.exigir(CatalogoTecnicasSolo.PRENDER_PERNAS).aceita(new ComandoSolo(Direcao.ESQUERDA, false)));
        assertFalse(c.exigir(CatalogoTecnicasSolo.PRENDER_PERNAS).aceita(new ComandoSolo(Direcao.DIREITA, false)));
        assertTrue(c.exigir(CatalogoTecnicasSolo.SEGURAR_PERNAS).aceita(new ComandoSolo(Direcao.DIREITA, false)));
        assertEquals(NaturezaAcao.CONTROLE, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.SEGURAR_PERNAS)));
        assertNotEquals(NaturezaAcao.QUEDA, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.SOLTAR_PERNAS)));
        MotorSolo.Evento e = new MotorSolo.Evento(1, "SUCESSO", CatalogoTecnicasSolo.SEGURAR_PERNAS, Lado.A, "efeito");
        assertTrue(EventoLuta.de(e).isEmpty(), "nao conta como queda, passagem nem finalizacao");
    }
}
