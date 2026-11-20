package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.EfeitosTemporarios;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.TecnicaSolo;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.4 no GamePanel real: desequilibrio x quedas (resultado FINAL, nao so a
 * mensagem) e o combate de joelhos igual venha a postura de onde vier.
 */
class Consolidacao5Q4GamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void j2Base() {
        tecla("pretoControleP", true);
        ticks(1);
        tecla("pretoControleP", false);
        ticks(1);
    }

    private void esperarResolver() {
        for (int i = 0; i < 120 && m().getTentativa().isPresent(); i++) {
            ticks(1);
        }
        ticks(2);
    }

    private void desequilibrarJ2() {
        j2().getEfeitos().aplicar(EfeitosTemporarios.Tipo.DESEQUILIBRIO, SistemaCombate.DURACAO_DESEQUILIBRIO_TICKS);
    }

    private boolean j2Caiu() {
        return j2().isDeitado() || m().isAterrissando() || m().getPosicao().isNoChao();
    }

    @Test
    void derrubadaForcadaDerrubaQuemEstaDesequilibrado() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(30);
            desequilibrarJ2();
            toque("brancoPegada3");
            assertEquals(CatalogoTecnicasSolo.DERRUBADA_FORCADA, m().getTentativa().orElseThrow().tecnica().id());
            esperarResolver();
            assertTrue(j2Caiu(), "desequilibrado, sem RT: vai ao chao na primeira");
        });
    }

    @Test
    void derrubadaForcadaComBaseAtivaContinuaDefendida() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(30);
            toque("brancoPegada3");
            j2Base();
            esperarResolver();
            assertFalse(j2().isDeitado(), "defesa legitima com RT");
            assertEquals(TecnicaSolo.Exposicao.AVANCO, m().exposicaoDe(Lado.A).orElseThrow());
        });
    }

    @Test
    void aPrimeiraDerrubadaDesequilibraDeVerdade() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(30);
            toque("brancoPegada3");
            esperarResolver();
            assertTrue(j2().getEfeitos().isAtivo(EfeitosTemporarios.Tipo.DESEQUILIBRIO),
                    "o mesmo desequilibrio do SistemaCombate (o HUD e as quedas leem dele)");
            double b = m().getBase(Lado.B);
            ticks(20);
            assertEquals(b, m().getBase(Lado.B), 1e-9, "desequilibrado nao recupera a base");
        });
    }

    @Test
    void rasteiraContraBaseNaturalExplicaEContraDesequilibradoEntra() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(30);
            m().comando(Lado.A, new TecnicaSolo.ComandoSolo(TecnicaSolo.Direcao.CIMA, TecnicaSolo.Familia.L2));
            assertEquals(CatalogoTecnicasSolo.RASTEIRA, m().getTentativa().orElseThrow().tecnica().id());
            esperarResolver();
            assertFalse(j2().isDeitado(), "parado e com base cheia: nao entra");
            assertTrue(m().eventos().stream().anyMatch(e -> e.detalhe() != null && e.detalhe().contains("base natural")),
                    "o motivo explica a base natural (nao 'defendeu')");

            novo(InicioLuta.EM_PE);
            j1Pega(1);
            ticks(30);
            desequilibrarJ2();
            m().comando(Lado.A, new TecnicaSolo.ComandoSolo(TecnicaSolo.Direcao.CIMA, TecnicaSolo.Familia.L2));
            esperarResolver();
            assertTrue(j2Caiu(), "vulneravel (desequilibrado): a rasteira entra");
        });
    }

    @Test
    void deJoelhosOMesmoContextoVenhaDeOndeVier() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.DE_JOELHOS);
            String[] inicio = motivos();
            int d0 = Math.abs(j1().getX() - j2().getX());

            novo(InicioLuta.EM_PE);
            tecla("brancoDireita", true);                       // chega na mesma distancia do inicio de joelhos
            for (int i = 0; i < 400 && Math.abs(j1().getX() - j2().getX()) > d0; i++) {
                ticks(1);
            }
            tecla("brancoDireita", false);
            j1().ajoelhar();
            j2().ajoelhar();
            ticks(30);
            String[] depois = motivos();
            for (int i = 0; i < inicio.length; i++) {
                assertEquals(inicio[i], depois[i], "mesma regra: " + i);
            }
            // permitidas de joelhos: nao recusadas pela postura
            assertNotEquals("SÓ EM PÉ (DE JOELHOS NÃO)", inicio[0]);
            assertNotEquals("SÓ EM PÉ (DE JOELHOS NÃO)", inicio[1]);
            assertNotEquals("SÓ EM PÉ (DE JOELHOS NÃO)", inicio[2]);
            // so em pe
            assertEquals("SÓ EM PÉ (DE JOELHOS NÃO)", inicio[3]);
            assertEquals("SÓ EM PÉ (DE JOELHOS NÃO)", inicio[4]);
        });
    }

    private String[] motivos() {
        CatalogoTecnicasSolo c = m().getCatalogo();
        String[] ids = {CatalogoTecnicasSolo.EMPURRAO, CatalogoTecnicasSolo.PUXAR_GUARDA,
            CatalogoTecnicasSolo.DERRUBADA_FORCADA, CatalogoTecnicasSolo.TIRO_GUARDA, CatalogoTecnicasSolo.ENTRADA_PERNAS};
        String[] r = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            String mot = m().motivoSem(Lado.A, c.exigir(ids[i]));
            r[i] = mot == null ? "-" : mot;
        }
        return r;
    }
}
