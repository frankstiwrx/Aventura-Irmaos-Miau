package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.entities.CorKimono;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.regras.Graduacao;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5Q.4: o J1 escolhe faixa e kimono (teclado/controle e mouse), independente do J2/parceiro. */
class Selecao5Q4Test {

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    @AfterEach
    void limpar() {
        SelecaoLutadoresPanel.reiniciarInicioDaSessao();
    }

    private OpcoesDeLutadores opcoes() {
        return new OpcoesDeLutadores(CatalogoPersonagens.padrao(), new ServicoPerfis(
                new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao()));
    }

    @Test
    void j1VaiParaFaixaEKimonoAntesDeLutar() throws Exception {
        AtomicReference<EscolhaLutador[]> inicio = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            SelecaoPartidaPanel sel = new SelecaoPartidaPanel(CatalogoPersonagens.padrao(), opcoes(),
                    RegistroCenarios.padrao(), RegistroCenarios.ID_PADRAO,
                    (a, b, c, t) -> inicio.set(new EscolhaLutador[] {a, b}), () -> { }, () -> null, () -> null);
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J2, Comando.CONFIRMAR);
            // reorganizacao dos menus (pedido): FAIXA e KIMONO sao etapas de cada jogador
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(J1), "nao pula para PRONTO");
            sel.comandar(J1, Comando.DIREITA);                  // azul
            sel.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.KIMONO, sel.fase(J1));
            sel.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, sel.fase(J1));
            assertEquals(Graduacao.AZUL, sel.faixa(J1));
            assertEquals(Graduacao.BRANCA, sel.faixa(J2), "o J2 nao muda junto");
            sel.comandar(J2, Comando.CONFIRMAR);
            sel.comandar(J2, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.CONFIRMAR);                // AVANCAR
            for (int i = 0; i < 4; i++) {
                sel.comandar(J1, Comando.CONFIRMAR);            // ... INICIAR LUTA
            }
        });
        assertEquals(Graduacao.AZUL, inicio.get()[0].getGraduacao());
        assertEquals(Graduacao.BRANCA, inicio.get()[1].getGraduacao());
    }

    @Test
    void mouseMudaFaixaEKimonoDeCadaSlotELuta() throws Exception {
        AtomicReference<EscolhaLutador[]> inicio = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            SelecaoPartidaPanel sel = new SelecaoPartidaPanel(CatalogoPersonagens.padrao(), opcoes(),
                    RegistroCenarios.padrao(), RegistroCenarios.ID_PADRAO,
                    (a, b, c, t) -> inicio.set(new EscolhaLutador[] {a, b}), () -> { }, () -> null, () -> null);
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J2, Comando.CONFIRMAR);
            // reorganizacao dos menus (pedido): o clique vale na etapa em que cada um esta
            sel.clicar(200, 90);                                // J1 faixa ->
            sel.comandar(J1, Comando.CONFIRMAR);                // J1 vai ao kimono
            sel.clicar(200, 105);                               // J1 kimono ->
            sel.clicar(700, 90);                                // J2 faixa ->
            sel.clicar(700, 90);                                // J2 faixa -> (roxa)
            assertEquals(Graduacao.AZUL, sel.faixa(J1));
            assertEquals(Graduacao.ROXA, sel.faixa(J2));
            sel.comandar(J1, Comando.CONFIRMAR);                // J1 PRONTO
            sel.comandar(J2, Comando.CONFIRMAR);
            sel.comandar(J2, Comando.CONFIRMAR);                // J2 PRONTO
            sel.clicar(400, 380);                               // AVANCAR
            sel.clicar(400, 384);                               // INICIAR LUTA
        });
        EscolhaLutador[] e = inicio.get();
        assertEquals(Graduacao.AZUL, e[0].getGraduacao());
        assertNotEquals(CorKimono.PRETO, e[0].getKimono(), "o kimono padrao do Branco (preto) mudou pelo clique");
        assertEquals(Graduacao.ROXA, e[1].getGraduacao());
    }

    @Test
    void treinoMouseMudaOParceiroSemMexerNoJogador() throws Exception {
        AtomicReference<EscolhaLutador[]> inicio = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            SelecaoPartidaPanel sel = SelecaoPartidaPanel.treino(CatalogoPersonagens.padrao(), opcoes(),
                    RegistroCenarios.padrao(), RegistroCenarios.ID_PADRAO, InicioLuta.DE_JOELHOS,
                    (j, p, c, i) -> inicio.set(new EscolhaLutador[] {j, p}), () -> { }, () -> null, () -> null);
            // treino igual ao JOGAR (pedido): lutador pronto, depois o parceiro na faixa
            for (int i = 0; i < 4; i++) {
                sel.comandar(J1, Comando.CONFIRMAR);
            }
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(J2));
            sel.clicar(700, 90);                                // parceiro faixa ->
            assertEquals(Graduacao.BRANCA, sel.faixa(J1));
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.CONFIRMAR);                // parceiro PRONTO
            sel.clicar(400, 380);                               // AVANCAR
            sel.clicar(400, 384);                               // INICIAR TREINO
        });
        assertEquals(Graduacao.BRANCA, inicio.get()[0].getGraduacao());
        assertEquals(Graduacao.AZUL, inicio.get()[1].getGraduacao());
    }
}
