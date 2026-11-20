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
            assertEquals(SelecaoPartidaPanel.Linha.FAIXA, sel.linhaJ1(), "nao pula para LUTAR");
            sel.comandar(J1, Comando.DIREITA);                  // azul
            sel.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Linha.KIMONO, sel.linhaJ1());
            sel.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Linha.LUTAR, sel.linhaJ1());
            assertEquals(Graduacao.AZUL, sel.faixa(J1));
            assertEquals(Graduacao.BRANCA, sel.faixa(J2), "o J2 nao muda junto");
            sel.comandar(J1, Comando.CONFIRMAR);
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
            sel.clicar(200, 90);                                // J1 faixa ->
            sel.clicar(200, 105);                               // J1 kimono ->
            sel.clicar(700, 90);                                // J2 faixa ->
            sel.clicar(700, 90);                                // J2 faixa -> (roxa)
            assertEquals(Graduacao.AZUL, sel.faixa(J1));
            assertEquals(Graduacao.ROXA, sel.faixa(J2));
            sel.clicar(400, 380);                               // LUTAR
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
            sel.comandar(J1, Comando.CONFIRMAR);
            sel.clicar(700, 90);                                // parceiro faixa ->
            assertEquals(SelecaoPartidaPanel.Linha.FAIXA_PARCEIRO, sel.linhaJ1());
            assertEquals(Graduacao.BRANCA, sel.faixa(J1));
            sel.clicar(400, 380);
        });
        assertEquals(Graduacao.BRANCA, inicio.get()[0].getGraduacao());
        assertEquals(Graduacao.AZUL, inicio.get()[1].getGraduacao());
    }
}
