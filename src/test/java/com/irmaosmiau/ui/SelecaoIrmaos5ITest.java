package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5I (teste 1, 2, 3 e 17 na tela): qualquer irmao em qualquer coluna (slot). */
class SelecaoIrmaos5ITest {

    private static final String BRANCO = CatalogoPersonagens.ID_BRANCO;
    private static final String PRETO = CatalogoPersonagens.ID_PRETO;

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private SelecaoLutadoresPanel tela;
    private final AtomicReference<Object[]> escolhido = new AtomicReference<>();

    private void abrir(boolean treino) throws Exception {
        ServicoPerfis servico = new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)),
                CatalogoPersonagens.padrao());
        SwingUtilities.invokeAndWait(() -> tela = new SelecaoLutadoresPanel(
                new OpcoesDeLutadores(CatalogoPersonagens.padrao(), servico), treino,
                (tipo, b, p) -> escolhido.set(new Object[]{tipo, b, p}), () -> { }));
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private String base(int i) {
        return ((EscolhaLutador) escolhido.get()[i]).getIdBase();
    }

    @Test
    void padraoContinuaBrancoNoJ1EPretoNoJ2() throws Exception {
        abrir(false);
        naEdt(() -> {
            assertEquals(BRANCO, tela.slotBranco().idBase());
            assertEquals(PRETO, tela.slotPreto().idBase());
            assertEquals("MIAU BRANCO", tela.botaoBranco().getText());
            assertEquals("MIAU PRETO", tela.botaoPreto().getText());
            tela.botaoAmbos().doClick(0);
        });
        assertEquals(TipoPersonagem.AMBOS, escolhido.get()[0]);
        assertEquals(BRANCO, base(1));
        assertEquals(PRETO, base(2));
    }

    @Test
    void todasAsCombinacoesDeIrmaoPorSlot() throws Exception {
        abrir(false);
        String[][] combinacoes = {{BRANCO, PRETO}, {PRETO, BRANCO}, {BRANCO, BRANCO}, {PRETO, PRETO}};
        for (String[] c : combinacoes) {
            naEdt(() -> {
                tela.slotBranco().escolherIrmao(c[0]);
                tela.slotPreto().escolherIrmao(c[1]);
                tela.botaoAmbos().doClick(0);
            });
            assertEquals(c[0], base(1), String.join("/", c));
            assertEquals(c[1], base(2), String.join("/", c));
        }
    }

    @Test
    void seletorTrocaComboImagemEBotao() throws Exception {
        abrir(false);
        naEdt(() -> {
            tela.slotBranco().irmao.setSelectedIndex(1);          // pelo proprio seletor
            assertEquals(PRETO, tela.slotBranco().idBase());
            assertEquals(1, tela.comboBranco().getItemCount());
            assertEquals(PRETO, ((EscolhaLutador) tela.comboBranco().getSelectedItem()).getIdBase());
            assertEquals("Base: " + CatalogoPersonagens.padrao().exigir(PRETO).getNome(),
                    tela.slotBranco().base.getText());
            assertNotNull(tela.slotBranco().imagem.getIcon(), "imagem do irmao escolhido");
            assertEquals("MIAU PRETO", tela.botaoBranco().getText(), "sozinho com o Preto nos controles do J1");
            tela.botaoBranco().doClick(0);
        });
        assertEquals(TipoPersonagem.BRANCO, escolhido.get()[0], "botao da coluna 1 = jogar com o J1");
        assertEquals(PRETO, base(1));
    }

    @Test
    void treinoControlandoOPretoTrocaOParceiroParaOBranco() throws Exception {
        abrir(true);
        naEdt(() -> {
            tela.slotBranco().escolherIrmao(PRETO);
            assertEquals(BRANCO, tela.slotPreto().idBase(), "parceiro vira o outro irmao");
            tela.botaoTreino().doClick(0);
        });
        assertEquals(PRETO, base(1));
        assertEquals(BRANCO, base(2));
        naEdt(() -> {
            tela.slotBranco().escolherIrmao(BRANCO);
            assertEquals(PRETO, tela.slotPreto().idBase());
            tela.slotPreto().escolherIrmao(BRANCO);               // parceiro ainda pode ser trocado
            tela.botaoTreino().doClick(0);
        });
        assertEquals(BRANCO, base(1));
        assertEquals(BRANCO, base(2));
    }
}
