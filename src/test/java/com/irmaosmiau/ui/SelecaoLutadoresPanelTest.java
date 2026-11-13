package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class SelecaoLutadoresPanelTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private SelecaoLutadoresPanel tela;
    private final AtomicReference<Object[]> escolhido = new AtomicReference<>();

    private ServicoPerfis servico() {
        return new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao());
    }

    private void abrir(boolean treino) throws Exception {
        SwingUtilities.invokeAndWait(() -> tela = new SelecaoLutadoresPanel(
                new OpcoesDeLutadores(CatalogoPersonagens.padrao(), servico()), treino,
                (tipo, b, p) -> escolhido.set(new Object[]{tipo, b, p}), () -> { }));
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private static List<String> textos(Component raiz) {
        List<String> t = new ArrayList<>();
        if (raiz instanceof JLabel) {
            t.add(((JLabel) raiz).getText());
        }
        if (raiz instanceof java.awt.Container) {
            for (Component c : ((java.awt.Container) raiz).getComponents()) {
                t.addAll(textos(c));
            }
        }
        return t;
    }

    @Test
    void semPerfisMostraOsOficiaisEFuncionaComoAntes() throws Exception {
        abrir(false);
        naEdt(() -> {
            assertEquals(1, tela.comboBranco().getItemCount());
            assertEquals(1, tela.comboPreto().getItemCount());
            assertEquals("OFICIAL", tela.slotBranco().tipo.getText());
            tela.botaoBranco().doClick(0);
        });
        assertEquals(TipoPersonagem.BRANCO, escolhido.get()[0]);
        assertTrue(((EscolhaLutador) escolhido.get()[1]).isOficial());
    }

    @Test
    void perfilCriadoNoEditorApareceESeusAtributosSaoMostrados() throws Exception {
        servico().criar("branco_forte", "Branco Forte", "miau_branco",
                new AtributosLutador(35, 60, 90, 22, 25, 25, 22, 80));

        abrir(false);
        naEdt(() -> {
            assertEquals(2, tela.comboBranco().getItemCount());
            assertEquals(1, tela.comboPreto().getItemCount(), "perfil do Branco nao aparece no slot Preto");
            tela.comboBranco().setSelectedIndex(1);
            assertEquals("PERSONALIZADO", tela.slotBranco().tipo.getText());
            assertEquals("Base: Irmão Miau Branco", tela.slotBranco().base.getText());
            List<String> t = textos(tela.slotBranco().atributos);
            assertTrue(t.contains("Força") && t.contains("90"), t.toString());
            assertTrue(t.contains("Fôlego") && t.contains("60"), t.toString());
            assertTrue(t.contains("80"), "peso");
            assertTrue(((EscolhaLutador) tela.comboBranco().getSelectedItem()).getDescricao()
                    .startsWith("Branco Forte — personalizado"));
            tela.botaoAmbos().doClick(0);
        });

        assertEquals(TipoPersonagem.AMBOS, escolhido.get()[0]);
        EscolhaLutador b = (EscolhaLutador) escolhido.get()[1];
        assertEquals("Branco Forte", b.getNome());
        assertTrue(((EscolhaLutador) escolhido.get()[2]).isOficial());
    }

    @Test
    void modoTreinoEscolheJogadorEParceiro() throws Exception {
        servico().criarAPartirDoPersonagem("pretinho", "Pretinho", "miau_preto");
        abrir(true);
        naEdt(() -> {
            assertTrue(tela.isModoTreino());
            assertNull(tela.botaoBranco().getParent(), "no treino nao ha botoes de modo");
            tela.comboPreto().setSelectedIndex(1);
            tela.botaoTreino().doClick(0);
        });
        assertEquals("Pretinho", ((EscolhaLutador) escolhido.get()[2]).getNome());
        assertTrue(((EscolhaLutador) escolhido.get()[1]).isOficial());
    }

    @Test
    void perfilCorrompidoGeraAvisoMasOficiaisContinuam() throws Exception {
        servico().criarAPartirDoPersonagem("bom", "Bom", "miau_branco");
        Files.writeString(tmp.resolve("perfis/quebrado.json"), "{{");
        abrir(false);
        naEdt(() -> {
            assertEquals(2, tela.comboBranco().getItemCount());
            assertTrue(tela.textoAvisos().contains("quebrado.json"), tela.textoAvisos());
        });
    }

    @Test
    void cabeEm800x450NasDuasVariantes() throws Exception {
        servico().criarAPartirDoPersonagem("nome_bem_comprido", "Nome Bem Comprido Mesmo", "miau_branco");
        for (boolean treino : new boolean[]{false, true}) {
            abrir(treino);
            naEdt(() -> {
                tela.setSize(800, 450);
                ConfiguracoesPanelTest.validarDentro(tela, tela);
            });
        }
    }
}
