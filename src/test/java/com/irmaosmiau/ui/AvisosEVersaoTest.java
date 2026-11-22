package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.controles.TeclaFisica;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioControles;
import com.irmaosmiau.persistencia.RepositorioDispositivos;
import com.irmaosmiau.persistencia.RepositorioGamepad;
import com.irmaosmiau.preferencias.PreferenciasControles;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.preferencias.PreferenciasGamepad;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Sem janela de avisos na abertura (aviso curto + AVISOS em CONTROLES) e creditos/versao no menu principal. */
class AvisosEVersaoTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private final DialogosFalsos dialogos = new DialogosFalsos();

    /** controles.json como os jogos antigos gravavam. */
    private static String controlesJson(MapaControles m) {
        StringBuilder sb = new StringBuilder("{\"tipo\":\"CONTROLES_IRMAOS_MIAU\",\"versaoEsquema\":1,\"jogadores\":{");
        boolean primeiro = true;
        for (SlotJogador s : SlotJogador.values()) {
            sb.append(primeiro ? "" : ",").append('"').append(s.getIdArquivo()).append("\":{");
            primeiro = false;
            boolean p2 = true;
            for (Map.Entry<AcaoJogador, TeclaFisica> e : m.doSlot(s).entrySet()) {
                sb.append(p2 ? "" : ",").append('"').append(e.getKey().name()).append("\":\"")
                        .append(e.getValue().getNome()).append('"');
                p2 = false;
            }
            sb.append('}');
        }
        return sb.append("}}").toString();
    }

    private ControlesPanel abrir() throws Exception {
        DiretorioDados d = new DiretorioDados(tmp);
        PreferenciasControles c = PreferenciasControles.carregar(new RepositorioControles(d));
        PreferenciasDispositivos disp = PreferenciasDispositivos.carregar(new RepositorioDispositivos(d));
        PreferenciasGamepad g = PreferenciasGamepad.carregar(new RepositorioGamepad(d));
        AtomicReference<ControlesPanel> t = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> t.set(new ControlesPanel(c, disp, g, new ProvedorGamepadSimulado(),
                dialogos, () -> { })));
        return t.get();
    }

    // Instalacao nova: nada de avisos, nada de botao
    @Test
    void instalacaoNovaSemAvisos() throws Exception {
        ControlesPanel tela = abrir();
        assertFalse(tela.botaoAvisos().isVisible());
        assertTrue(dialogos.avisos.isEmpty(), "nenhuma janela");
        assertFalse(Files.exists(tmp.resolve(RepositorioControles.NOME_ARQUIVO)), "nada e gravado sozinho");
    }

    // Instalacao antiga com padroes de antes: migracao aplicada na memoria, sem janela e sem linha de erro
    @Test
    void padroesAntigosSoNoBotaoAvisos() throws Exception {
        String antigo = controlesJson(MapaControles.padrao5O());
        Files.writeString(tmp.resolve(RepositorioControles.NOME_ARQUIVO), antigo, StandardCharsets.UTF_8);
        ControlesPanel tela = abrir();
        assertEquals(MapaControles.padrao(), tela.mapaEmEdicao(), "os padroes novos ja valem");
        assertTrue(tela.botaoAvisos().isVisible());
        assertFalse(tela.textoStatus().contains("teclas padrão atualizadas"), "informativo nao vira linha de erro");
        assertTrue(dialogos.avisos.isEmpty(), "nenhuma janela ao abrir");
        SwingUtilities.invokeAndWait(tela::mostrarAvisos);
        assertEquals(1, dialogos.avisos.size(), "detalhes so quando o jogador pede");
        assertTrue(dialogos.avisos.get(0).contains("ATUALIZAÇÕES JÁ APLICADAS"));
        assertEquals(antigo, Files.readString(tmp.resolve(RepositorioControles.NOME_ARQUIVO)),
                "o arquivo do jogador nao muda sem SALVAR");
        AtomicReference<Boolean> salvou = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> salvou.set(tela.salvar()));
        assertTrue(salvou.get());
        assertFalse(tela.botaoAvisos().isVisible(), "salvo: os avisos acabam");
        assertFalse(abrir().botaoAvisos().isVisible(), "e nao voltam na proxima abertura");
    }

    // Conflito real (tecla do J1 que agora e do teclado-controle do J2): linha curta e personalizacao mantida
    @Test
    void conflitoRealApareceCurtoNoControles() throws Exception {
        MapaControles meu = MapaControles.padrao()
                .comTecla(SlotJogador.JOGADOR_1, AcaoJogador.PULAR, TeclaFisica.porNome("BACK_SPACE").orElseThrow());
        Files.writeString(tmp.resolve(RepositorioControles.NOME_ARQUIVO),
                controlesJson(meu).replace("\"INICIAR_PEGADA\":\"2\"", "\"INICIAR_PEGADA\":\"W\""),
                StandardCharsets.UTF_8);
        ControlesPanel tela = abrir();
        assertEquals("BACKSPACE", tela.mapaEmEdicao().rotulo(SlotJogador.JOGADOR_1, AcaoJogador.PULAR),
                "personalizacao mantida");
        assertTrue(tela.textoStatus().contains("teclado-controle do J2"), tela.textoStatus());
        assertTrue(tela.botaoAvisos().isVisible());
        assertTrue(dialogos.avisos.isEmpty());
    }

    @Test
    void classificacaoDosAvisosReais() {
        List<String> info = List.of(
                "J1: teclas padrão atualizadas (5O.1: movimento no teclado numérico 8/4/5/6). Salve em CONTROLES para gravar.",
                "J1: o esquema padrão agora é o CONTEXTUAL (era o Clássico, nunca escolhido). Para voltar: CONTROLES → esquema Clássico → SALVAR.",
                "J2: respirar agora é no Y/Triângulo (o RT abre as finalizações por cima). Salve em CONTROLES para gravar.",
                "J1: botões padrão do gamepad atualizados (R3 rola, L3 deita, A pula, RB + A pulo alto). Salve em CONTROLES para gravar.",
                "J2: seus botões personalizados foram mantidos. Os novos padrões (R3 rola, L3 deita, RB + A pulo alto) estão em CONTROLES → RESTAURAR PADRÕES.");
        List<String> acao = List.of(
                "J1 · Pegada: a tecla W agora é do teclado-controle do J2. Escolha outra em CONTROLES.",
                "J2 · Respirar ficou sem tecla (a padrão K já está em uso). Escolha uma em CONTROLES.",
                "J1: respirar saiu do RT e o Y/Triângulo está ocupado nos seus botões: atribua RESPIRAR em CONTROLES.",
                "Não foi possível usar os controles salvos; controles padrão em uso. Detalhe: x",
                "Os botões de gamepad inválidos foram guardados em gamepad.json.corrompido-1.");
        info.forEach(a -> assertFalse(AvisosControles.pedeAcao(a), a));
        acao.forEach(a -> assertTrue(AvisosControles.pedeAcao(a), a));
        assertNull(AvisosControles.resumo(info), "so informativos: nenhuma linha");
        assertTrue(AvisosControles.resumo(acao).startsWith("5 ajustes"));
    }

    // Creditos e versao: constante central, cantos livres, nada sobre opcoes e descricao
    @Test
    void creditosEVersaoNoMenuPrincipal() throws Exception {
        assertEquals("v0.1.0", VersaoJogo.VERSAO);
        assertEquals("BETA", VersaoJogo.ETAPA);
        assertEquals("by frankstiwrx · 2026", VersaoJogo.creditos());
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Rectangle barra = new Rectangle(80, 410, 640, 34);
        Rectangle opcoes = new Rectangle(60, 112, 320, 290);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        int wAutor = g.getFontMetrics().stringWidth("by " + VersaoJogo.AUTOR);
        Rectangle autor = new Rectangle(6, 415, wAutor, 26);
        g.setFont(new Font("Arial", Font.BOLD, 11));
        int wVersao = g.getFontMetrics().stringWidth(VersaoJogo.VERSAO);
        Rectangle versao = new Rectangle(794 - wVersao, 414, wVersao, 30);
        for (Rectangle r : new Rectangle[] {autor, versao}) {
            assertFalse(r.intersects(barra), "nao cobre a descricao: " + r);
            assertFalse(r.intersects(opcoes));
            assertTrue(new Rectangle(0, 0, 800, 450).contains(r), "cabe em 800x450: " + r);
        }
        g.dispose();
        SwingUtilities.invokeAndWait(() -> {
            MenuPrincipalPanel m = new MenuPrincipalPanel(() -> { }, () -> { }, () -> { }, () -> { }, () -> { });
            m.setSize(800, 450);
            m.doLayout();
            Graphics2D gm = img.createGraphics();
            m.paint(gm);
            gm.dispose();
        });
        // pixel claro do texto da versao no canto direito (fundo escuro do patio nao e branco puro)
        boolean algo = false;
        for (int x = 794 - wVersao; x < 794 && !algo; x++) {
            for (int y = 414; y < 426; y++) {
                int rgb = img.getRGB(x, y);
                if (((rgb >> 16) & 0xFF) > 220 && ((rgb >> 8) & 0xFF) > 220 && (rgb & 0xFF) > 230) {
                    algo = true;
                    break;
                }
            }
        }
        assertTrue(algo, "a versao foi desenhada");
    }
}
