package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.LimitesAtributos;
import com.irmaosmiau.personagens.PerfilLutador;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class PerfisPanelTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private DialogosFalsos dialogos;
    private final AtomicInteger voltou = new AtomicInteger();
    private PerfisPanel tela;

    /** Servicos "novos" = como se o jogo tivesse sido reiniciado. */
    private ServicoPerfis servico() {
        return new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao());
    }

    @BeforeEach
    void preparar() {
        dialogos = new DialogosFalsos();
    }

    private void abrir() throws Exception {
        SwingUtilities.invokeAndWait(() -> tela = new PerfisPanel(servico(), dialogos, voltou::incrementAndGet));
    }

    private void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    /** Escolhe a base pelo indice (0 = Branco, 1 = Preto) e cria um perfil novo. */
    private void novo(int indiceBase, String nome) throws Exception {
        naEdt(() -> {
            tela.comboBase().setSelectedIndex(indiceBase);
            tela.novoPerfil();
            tela.campoNome().setText(nome);
        });
    }

    // =====================================================
    // CRIAR E EDITAR OS OITO ATRIBUTOS
    // =====================================================
    @Test
    void criaPerfilNovoAPartirDoPretoComOsOitoAtributosEditados() throws Exception {
        abrir();
        novo(1, "Preto Ligeiro");
        double[] valores = {30, 50, 20, 60, 55, 33, 41, 65};
        naEdt(() -> {
            assertTrue(tela.isNovoPerfil());
            // Comeca com a COPIA dos atributos do Preto oficial.
            assertEquals(45.0, tela.linha("velocidade").valor());
            int i = 0;
            for (LimitesAtributos.Limite l : LimitesAtributos.todos()) {
                tela.linha(l.getNome()).campo.setValue(valores[i++]);
            }
            assertTrue(tela.salvar(), tela.textoStatus());
        });

        PerfilLutador salvo = servico().carregar("preto_ligeiro").orElseThrow();
        assertEquals("Preto Ligeiro", salvo.getNome());
        assertEquals("miau_preto", salvo.getIdPersonagemBase());
        int i = 0;
        for (LimitesAtributos.Limite l : LimitesAtributos.todos()) {
            assertEquals(valores[i++], l.ler(salvo.getAtributos()), l.getNome());
        }
        naEdt(() -> {
            assertEquals(1, tela.modeloLista().size());
            assertEquals("preto_ligeiro", tela.perfilEmEdicao().getId());
            assertFalse(tela.temAlteracoesNaoSalvas());
        });
    }

    @Test
    void sliderECampoNumericoFicamSincronizados() throws Exception {
        abrir();
        novo(0, "Sincronia");
        naEdt(() -> {
            PerfisPanel.Linha forca = tela.linha("forca");
            forca.slider.setValue(77);
            assertEquals(77.0, forca.valor());
            forca.campo.setValue(12.0);
            assertEquals(12, forca.slider.getValue());
            assertEquals(1, forca.slider.getMinimum());
            assertEquals(100, forca.slider.getMaximum());
            assertEquals(40, tela.linha("peso").slider.getMinimum());
            assertEquals(120, tela.linha("peso").slider.getMaximum());
        });
    }

    @Test
    void perfilRecuperadoDepoisDeReiniciarEEditado() throws Exception {
        servico().criar("branco_forte", "Branco Forte", "miau_branco",
                new AtributosLutador(35, 25, 90, 22, 25, 25, 22, 80));

        abrir();                                   // "jogo reiniciado"
        naEdt(() -> {
            assertEquals(1, tela.modeloLista().size());
            tela.lista().setSelectedIndex(0);
            assertEquals("branco_forte", tela.perfilEmEdicao().getId());
            assertEquals(90.0, tela.linha("forca").valor());
            assertEquals(80.0, tela.linha("peso").valor());

            tela.campoNome().setText("Branco Muito Forte");
            tela.linha("forca").campo.setValue(95.0);
            assertTrue(tela.temAlteracoesNaoSalvas());
            assertTrue(tela.salvar());
        });

        PerfilLutador p = servico().carregar("branco_forte").orElseThrow();
        assertEquals("Branco Muito Forte", p.getNome());
        assertEquals(95.0, p.getAtributos().getForca());
    }

    // =====================================================
    // VALIDACAO
    // =====================================================
    @Test
    void nomeInvalidoNaoSalvaEExplica() throws Exception {
        abrir();
        novo(0, "   ");
        naEdt(() -> {
            assertFalse(tela.salvar());
            assertTrue(tela.textoStatus().contains("nome"), tela.textoStatus());
        });
        assertTrue(servico().listar().getValor().isEmpty());
    }

    @Test
    void valorDigitadoForaDoLimiteNaoSalva() throws Exception {
        abrir();
        novo(0, "Exagerado");
        naEdt(() -> {
            javax.swing.JFormattedTextField texto =
                    ((javax.swing.JSpinner.DefaultEditor) tela.linha("forca").campo.getEditor()).getTextField();
            texto.setText("500");
            assertFalse(tela.salvar());
            assertTrue(tela.textoStatus().contains("Força: digite um número entre 1 e 100"), tela.textoStatus());
        });
        assertTrue(servico().listar().getValor().isEmpty());
    }

    @Test
    void nomesIguaisGeramIdsDiferentes() throws Exception {
        abrir();
        novo(0, "Gêmeo");
        naEdt(() -> assertTrue(tela.salvar()));
        novo(1, "Gêmeo");
        naEdt(() -> assertTrue(tela.salvar()));

        assertEquals(List.of("gemeo", "gemeo_2"),
                servico().listar().getValor().stream().map(PerfilLutador::getId).toList());
    }

    // =====================================================
    // EXCLUSAO COM CONFIRMACAO
    // =====================================================
    @Test
    void excluirPedeConfirmacao() throws Exception {
        servico().criarAPartirDoPersonagem("descartavel", "Descartável", "miau_preto");
        abrir();
        naEdt(() -> tela.lista().setSelectedIndex(0));

        dialogos.confirmacoes.add(false);
        naEdt(tela::excluir);
        assertTrue(servico().carregar("descartavel").isPresent(), "nao confirmou: nada apagado");
        assertTrue(dialogos.perguntas.get(0).contains("não pode ser desfeita"));

        dialogos.confirmacoes.add(true);
        naEdt(tela::excluir);
        assertTrue(servico().carregar("descartavel").isEmpty());
        naEdt(() -> {
            assertEquals(0, tela.modeloLista().size());
            assertNull(tela.perfilEmEdicao());
        });
    }

    // =====================================================
    // MUDANCAS NAO SALVAS
    // =====================================================
    @Test
    void voltarComMudancasPergunta() throws Exception {
        abrir();
        novo(0, "Rascunho");

        dialogos.respostas.add(Dialogos.Resposta.CANCELAR);
        naEdt(tela::voltar);
        assertEquals(0, voltou.get());

        dialogos.respostas.add(Dialogos.Resposta.NAO);
        naEdt(tela::voltar);
        assertEquals(1, voltou.get());
        assertTrue(servico().listar().getValor().isEmpty(), "descartado sem gravar");
    }

    @Test
    void trocarDePerfilComMudancasPodeSalvarAntes() throws Exception {
        ServicoPerfis s = servico();
        s.criarAPartirDoPersonagem("aaa", "A", "miau_branco");
        s.criarAPartirDoPersonagem("bbb", "B", "miau_preto");
        abrir();
        naEdt(() -> {
            tela.lista().setSelectedIndex(0);
            tela.linha("tecnica").campo.setValue(70.0);
        });
        dialogos.respostas.add(Dialogos.Resposta.SIM);
        naEdt(() -> tela.lista().setSelectedIndex(1));

        assertEquals(70.0, servico().carregar("aaa").orElseThrow().getAtributos().getTecnica());
        naEdt(() -> assertEquals("bbb", tela.perfilEmEdicao().getId()));
    }

    @Test
    void semAlteracoesVoltaDireto() throws Exception {
        abrir();
        naEdt(tela::voltar);
        assertEquals(1, voltou.get());
        assertTrue(dialogos.perguntas.isEmpty());
        assertFalse(Files.exists(tmp.resolve("perfis")), "abrir a tela nao cria pastas");
    }

    // =====================================================
    // OFICIAIS PROTEGIDOS E ARQUIVOS COM PROBLEMA
    // =====================================================
    @Test
    void personagensOficiaisNuncaMudam() throws Exception {
        abrir();
        novo(0, "Teste Oficial");
        naEdt(() -> {
            tela.linha("forca").campo.setValue(1.0);
            tela.linha("peso").campo.setValue(120.0);
            assertTrue(tela.salvar());
            tela.restaurarPadraoDaBase();
            assertEquals(42.0, tela.linha("forca").valor(), "padrao da base = Branco oficial");
        });

        assertEquals(42.0, CatalogoPersonagens.padrao().exigir("miau_branco").getAtributosPadrao().getForca());
        assertEquals(42.0, new IrmaoMiauBranco(0, 0, 0.7).getAtributos().getForca());
        assertEquals(18.0, new IrmaoMiauPreto(0, 0, 0.7).getAtributos().getForca());
        assertFalse(Files.exists(tmp.resolve("perfis/miau_branco.json")));
        assertEquals(1.0, servico().carregar("teste_oficial").orElseThrow().getAtributos().getForca());
    }

    @Test
    void perfilCorrompidoEAvisadoEPreservado() throws Exception {
        servico().criarAPartirDoPersonagem("bom", "Bom", "miau_branco");
        Files.writeString(tmp.resolve("perfis/ruim.json"), "{ estragado");

        abrir();
        naEdt(() -> {
            assertEquals(1, tela.modeloLista().size());
            assertTrue(tela.textoStatus().contains("ruim.json"), tela.textoStatus());
        });
        assertEquals("{ estragado", Files.readString(tmp.resolve("perfis/ruim.json")));

        // E o id "ruim" continua reservado: um perfil novo nao o sobrescreve.
        novo(0, "Ruim");
        naEdt(() -> assertTrue(tela.salvar()));
        assertEquals("{ estragado", Files.readString(tmp.resolve("perfis/ruim.json")));
        assertTrue(Files.exists(tmp.resolve("perfis/ruim_2.json")));
    }

    @Test
    void avisoDeOndeUsarOsPerfisEstaNaTela() throws Exception {
        abrir();
        naEdt(() -> {
            boolean achou = false;
            for (java.awt.Component c : tela.getComponents()) {
                if (c instanceof javax.swing.JLabel
                        && ((javax.swing.JLabel) c).getText().equals(PerfisPanel.AVISO_COMBATE)) {
                    achou = true;
                }
            }
            assertTrue(achou);
            // polimento dos menus (pedido): o menu se chama JOGAR (o COMEÇAR antigo nao existe mais)
            assertTrue(PerfisPanel.AVISO_COMBATE.contains("JOGAR ou TREINO"));
        });
    }

    @Test
    void cabeEm800x450() throws Exception {
        abrir();
        naEdt(() -> {
            tela.setSize(800, 450);
            ConfiguracoesPanelTest.validarDentro(tela, tela);
        });
    }

    // =====================================================
    // GERADOR DE ID
    // =====================================================
    @Test
    void geradorDeIdUsaORegraDaEntrega4B() {
        assertEquals("branco_forte", GeradorIdPerfil.gerar("Branco Forte!", id -> true));
        assertEquals("acao_reacao", GeradorIdPerfil.gerar("Ação & Reação", id -> true));
        assertEquals("perfil_ab", GeradorIdPerfil.gerar("ab", id -> true));
        assertEquals("perfil", GeradorIdPerfil.gerar("!!!", id -> true));
        assertEquals("repetido_2", GeradorIdPerfil.gerar("Repetido", id -> !id.equals("repetido")));
        assertEquals("repetido_3", GeradorIdPerfil.gerar("Repetido",
                id -> !id.equals("repetido") && !id.equals("repetido_2")));
        String longo = GeradorIdPerfil.gerar("a".repeat(80), id -> true);
        assertTrue(longo.length() <= 40);
        assertNotNull(PerfilLutador.normalizarId(longo));
        // Ids oficiais sao reservados pelo ServicoPerfis.idDisponivel
        assertEquals("miau_branco_2", GeradorIdPerfil.gerar("Miau Branco", servicoEstatico()::idDisponivel));
    }

    private ServicoPerfis servicoEstatico() {
        return servico();
    }
}
