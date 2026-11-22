package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.BotaoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.LeitorNavegacao;
import com.irmaosmiau.dispositivos.LeitorNavegacao.Comando;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.entities.AparenciaLutador;
import com.irmaosmiau.entities.AparenciaLutador.Parte;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.persistencia.DiretorioDados;
import com.irmaosmiau.persistencia.PastaDeTeste;
import com.irmaosmiau.persistencia.RepositorioPerfis;
import com.irmaosmiau.persistencia.ServicoPerfis;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.personagens.PerfilLutador;
import com.irmaosmiau.regras.Graduacao;
import com.irmaosmiau.selecao.OpcoesDeLutadores;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/**
 * Perfis pelo X / Quadrado (sem faixa nem kimono, atributos e aparencia do editor) e o TREINO com a mesma
 * selecao do JOGAR (um dispositivo escolhe os dois) e a tela da arena.
 */
class SelecaoPerfisTreinoTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;
    private static final SlotJogador J2 = SlotJogador.JOGADOR_2;
    private static final Color LARANJA = new Color(230, 140, 60);
    private static final Color VERDE = new Color(40, 170, 80);
    private static final AtributosLutador FORTE = new AtributosLutador(40, 30, 90, 25, 25, 30, 25, 80);

    private final AtomicInteger lutas = new AtomicInteger();
    private final AtomicInteger treinos = new AtomicInteger();
    private final AtomicInteger voltas = new AtomicInteger();
    private final AtomicReference<Object[]> iniciado = new AtomicReference<>();

    @AfterEach
    void limparSessao() {
        SelecaoLutadoresPanel.reiniciarInicioDaSessao();
    }

    private ServicoPerfis servico() {
        return new ServicoPerfis(new RepositorioPerfis(new DiretorioDados(tmp)), CatalogoPersonagens.padrao());
    }

    private OpcoesDeLutadores opcoes() {
        return new OpcoesDeLutadores(CatalogoPersonagens.padrao(), servico());
    }

    /** Relampago (Branco): atributos proprios e cores do editor; Trovao (Branco) e Sombra (Preto). */
    private PerfilLutador perfis() {
        ServicoPerfis s = servico();
        s.criarAPartirDoPersonagem("relampago", "Miau Relâmpago", CatalogoPersonagens.ID_BRANCO);
        s.atualizarAtributos("relampago", FORTE);
        PerfilLutador p = s.atualizarAparencia("relampago", AparenciaLutador.ORIGINAL.com(Parte.CASACO, VERDE)
                .com(Parte.FAIXA, Color.BLACK).com(Parte.PELO, LARANJA));
        s.criarAPartirDoPersonagem("trovao", "Miau Trovão", CatalogoPersonagens.ID_BRANCO);
        s.criarAPartirDoPersonagem("sombra", "Miau Sombra", CatalogoPersonagens.ID_PRETO);
        return p;
    }

    private SelecaoPartidaPanel partida(ProvedorGamepadSimulado pads, AssociacaoDispositivos a) {
        return new SelecaoPartidaPanel(CatalogoPersonagens.padrao(), opcoes(), RegistroCenarios.padrao(),
                RegistroCenarios.ID_PADRAO, new SelecaoPartidaPanel.AoIniciar() {
                    @Override
                    public void iniciar(EscolhaLutador j1, EscolhaLutador j2, String c, int s) {
                        lutas.incrementAndGet();
                        iniciado.set(new Object[] {j1, j2, c, s});
                    }

                    @Override
                    public void iniciar(EscolhaLutador j1, EscolhaLutador j2, String c, int s,
                            com.irmaosmiau.regras.ModoLuta m) {
                        lutas.incrementAndGet();
                        iniciado.set(new Object[] {j1, j2, c, s, m});
                    }
                }, voltas::incrementAndGet, () -> pads, () -> a);
    }

    private SelecaoPartidaPanel partida() {
        return partida(null, null);
    }

    private SelecaoPartidaPanel treino(ProvedorGamepadSimulado pads) {
        return SelecaoPartidaPanel.treino(CatalogoPersonagens.padrao(), opcoes(), RegistroCenarios.padrao(),
                RegistroCenarios.ID_PADRAO, InicioLuta.DE_JOELHOS, (j, p, c, i) -> {
                    treinos.incrementAndGet();
                    iniciado.set(new Object[] {j, p, c, i});
                }, voltas::incrementAndGet, () -> pads, () -> pads == null ? null : AssociacaoDispositivos.padrao());
    }

    private static void naEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private static void pintar(SelecaoPartidaPanel s) {
        s.setSize(800, 450);
        BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        s.tick(16);
        s.paint(g);
        g.dispose();
    }

    private static void vezes(SelecaoPartidaPanel s, SlotJogador slot, Comando c, int n) {
        for (int i = 0; i < n; i++) {
            s.comandar(slot, c);
        }
    }

    // 1. Confirmar o original mantem o fluxo: faixa, kimono, pronto
    @Test
    void originalMantemFaixaEKimono() throws Exception {
        perfis();
        naEdt(() -> {
            SelecaoPartidaPanel s = partida();
            s.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, s.fase(J1), "com perfis salvos, confirmar = o original");
            s.comandar(J1, Comando.DIREITA);
            s.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.KIMONO, s.fase(J1));
            s.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, s.fase(J1));
            assertTrue(s.escolha(J1).isOficial());
            assertEquals(Graduacao.AZUL, s.escolha(J1).getGraduacao());
        });
    }

    // 2 e 3. X / Quadrado (E no teclado do J1, J no do J2) abre so os perfis compativeis
    @Test
    void xAbreOsPerfisCompativeis() throws Exception {
        perfis();
        naEdt(() -> {
            SelecaoPartidaPanel s = partida();
            s.tecla("E", true);
            assertEquals(SelecaoPartidaPanel.Fase.PERFIL, s.fase(J1));
            assertEquals(List.of("Miau Relâmpago", "Miau Trovão"), s.perfisAbertos(J1), "so os do Branco");
            assertEquals("Miau Relâmpago", s.escolha(J1).getNome());
            assertEquals(CatalogoPersonagens.ID_BRANCO, s.escolha(J1).getIdBase());
            s.comandar(J1, Comando.DIREITA);
            assertEquals("Miau Trovão", s.escolha(J1).getNome());
            s.tecla("J", true);
            assertEquals(List.of("Miau Sombra"), s.perfisAbertos(J2), "o J2 abre os do Preto");
            pintar(s);
        });
    }

    // 3b. Sem perfis daquele personagem: so um aviso, o original continua disponivel
    @Test
    void semPerfisSoAvisa() throws Exception {
        naEdt(() -> {
            SelecaoPartidaPanel s = partida();
            s.comandar(J1, Comando.PERFIS);
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, s.fase(J1));
            assertTrue(s.isAvisoSemPerfis(J1));
            pintar(s);
            s.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, s.fase(J1));
        });
    }

    // 4, 5 e 8. Perfil confirmado: PRONTO sem faixa nem kimono, aparencia e atributos do editor
    @Test
    void perfilIgnoraFaixaEKimonoEMantemAparenciaEAtributos() throws Exception {
        PerfilLutador relampago = perfis();
        naEdt(() -> {
            SelecaoPartidaPanel s = partida();
            // o jogador mexe na faixa e no kimono do original, desiste e escolhe o perfil
            s.comandar(J1, Comando.CONFIRMAR);
            vezes(s, J1, Comando.DIREITA, 4);                 // faixa preta
            s.comandar(J1, Comando.CONFIRMAR);
            s.comandar(J1, Comando.DIREITA);                  // outro kimono
            s.comandar(J1, Comando.VOLTAR);
            s.comandar(J1, Comando.VOLTAR);                   // de volta ao personagem
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, s.fase(J1));
            s.comandar(J1, Comando.PERFIS);
            s.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, s.fase(J1), "sem as etapas de faixa e kimono");
            assertTrue(s.comPerfil(J1));
            vezes(s, J1, Comando.DIREITA, 3);                 // PRONTO trava: lados nao mudam nada
            EscolhaLutador e = s.escolha(J1);
            assertEquals(Graduacao.BRANCA, e.getGraduacao(), "a faixa preta do original nao vale no perfil");
            assertNull(e.getKimono(), "nenhum kimono do menu");
            assertTrue(PerfilLutador.mesmosValores(FORTE, e.getAtributos()), "atributos = os do editor");
            IrmaoMiau l = e.criarLutador(0, 0, 0.7);
            assertTrue(PerfilLutador.mesmosValores(FORTE, l.getAtributos()), "na luta tambem, sem bonus");
            assertEquals(VERDE, l.getCorCasaco());
            assertEquals(VERDE, l.getCorCalca(), "sem calca propria, a calca segue o casaco do perfil");
            assertEquals(Color.BLACK, l.getCorFaixa(), "faixa visual preta do perfil");
            assertEquals(Graduacao.BRANCA, l.getGraduacao(), "a faixa visual nao gradua");
            assertEquals(LARANJA, l.getCorPelo());
            assertEquals(relampago.getAparencia(), e.getPerfil().getAparencia());
            pintar(s);
        });
    }

    // 6. Cancelar o perfil volta ao fluxo do original
    @Test
    void cancelarPerfilVoltaAoOriginal() throws Exception {
        perfis();
        naEdt(() -> {
            SelecaoPartidaPanel s = partida();
            s.comandar(J1, Comando.PERFIS);
            s.comandar(J1, Comando.CONFIRMAR);
            s.comandar(J1, Comando.VOLTAR);
            assertEquals(SelecaoPartidaPanel.Fase.PERFIL, s.fase(J1), "do PRONTO volta a lista de perfis");
            s.comandar(J1, Comando.VOLTAR);
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, s.fase(J1));
            assertTrue(s.escolha(J1).isOficial());
            s.comandar(J1, Comando.CONFIRMAR);
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, s.fase(J1), "o original volta a ter faixa e kimono");
            assertFalse(s.comPerfil(J1));
        });
    }

    // 7 e 16. J1 e J2 escolhem perfis no JOGAR; a luta continua com tempo, regras e arena
    @Test
    void j1Ej2EscolhemPerfisNoJogar() throws Exception {
        perfis();
        naEdt(() -> {
            SelecaoPartidaPanel s = partida();
            s.comandar(J1, Comando.PERFIS);
            s.comandar(J1, Comando.CONFIRMAR);
            s.comandar(J2, Comando.PERFIS);
            s.comandar(J2, Comando.CONFIRMAR);
            assertTrue(s.ambosProntos());
            s.comandar(J2, Comando.CONFIRMAR);                // AVANCAR
            assertTrue(s.naConfiguracao());
            assertFalse(s.configuracao().isTreino(), "JOGAR: a tela de sempre");
            s.comandar(J1, Comando.PROXIMO);
            assertEquals(ConfiguracaoLuta.Area.TEMPO, s.configuracao().foco());
            s.comandar(J1, Comando.PROXIMO);
            assertEquals(ConfiguracaoLuta.Area.REGRAS, s.configuracao().foco());
            vezes(s, J1, Comando.CONFIRMAR, 3);
        });
        assertEquals(1, lutas.get());
        Object[] r = iniciado.get();
        assertEquals("Miau Relâmpago", ((EscolhaLutador) r[0]).getNome());
        assertEquals("Miau Sombra", ((EscolhaLutador) r[1]).getNome());
        assertEquals(5, r.length, "com o modo de regras");
    }

    // 9. Reabrir a selecao (e o perfil) varias vezes nao acumula nada
    @Test
    void reabrirNaoAcumula() throws Exception {
        perfis();
        naEdt(() -> {
            for (int volta = 0; volta < 3; volta++) {
                SelecaoPartidaPanel s = partida();
                for (int i = 0; i < 4; i++) {
                    s.comandar(J1, Comando.PERFIS);
                    s.comandar(J1, Comando.CONFIRMAR);
                    s.comandar(J1, Comando.VOLTAR);
                    s.comandar(J1, Comando.VOLTAR);
                    s.comandar(J1, Comando.CONFIRMAR);        // original com faixa roxa, ida e volta
                    vezes(s, J1, Comando.DIREITA, 2);
                    s.comandar(J1, Comando.VOLTAR);
                }
                s.comandar(J1, Comando.PERFIS);
                s.comandar(J1, Comando.CONFIRMAR);
                assertTrue(PerfilLutador.mesmosValores(FORTE, s.escolha(J1).getAtributos()));
                assertTrue(PerfilLutador.mesmosValores(FORTE, s.escolha(J1).criarLutador(0, 0, 0.7).getAtributos()));
                pintar(s);
                assertTrue(PerfilLutador.mesmosValores(FORTE, s.previa(J1).getAtributos()), "a previa tambem");
            }
        });
        assertTrue(PerfilLutador.mesmosValores(FORTE, servico().carregar("relampago").orElseThrow().getAtributos()),
                "selecionar nunca grava no perfil");
    }

    // 10, 11, 13, 14. TREINO: dois paineis, um dispositivo escolhe os dois, tela da arena, sem tempo nem regras
    @Test
    void treinoUmDispositivoEscolheOsDoisEVaiParaAArena() throws Exception {
        naEdt(() -> {
            SelecaoPartidaPanel s = treino(null);
            pintar(s);
            assertNotNull(s.previa(J1));
            assertNotNull(s.previa(J2), "os dois paineis com previa");
            assertEquals(J1, s.slotDaVez());
            vezes(s, J1, Comando.CONFIRMAR, 3);               // lutador: Branco, faixa, kimono
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, s.fase(J1));
            assertEquals(J2, s.slotDaVez(), "agora o mesmo controle escolhe o parceiro");
            assertEquals(CatalogoPersonagens.ID_PRETO, s.escolha(J2).getIdBase(), "parceiro padrao: o outro irmao");
            s.comandar(J1, Comando.CONFIRMAR);                // parceiro: FAIXA
            s.comandar(J1, Comando.DIREITA);                  // azul
            vezes(s, J1, Comando.CONFIRMAR, 2);               // kimono, PRONTO
            assertTrue(s.ambosProntos());
            pintar(s);
            s.comandar(J1, Comando.CONFIRMAR);                // AVANCAR
            assertTrue(s.naConfiguracao());
            assertTrue(s.configuracao().isTreino());
            pintar(s);
            for (int i = 0; i < 6; i++) {
                s.comandar(J1, i < 3 ? Comando.PROXIMO : Comando.ANTERIOR);
                ConfiguracaoLuta.Area a = s.configuracao().foco();
                assertTrue(a == ConfiguracaoLuta.Area.ARENA || a == ConfiguracaoLuta.Area.INICIO
                        || a == ConfiguracaoLuta.Area.INICIAR, "so arena, inicio e iniciar: " + a);
            }
            assertEquals(ConfiguracaoLuta.Area.ARENA, s.configuracao().foco());
            s.comandar(J1, Comando.DIREITA);                  // outra arena
            assertEquals(0, s.tempoAtual(), "treino sem limite de tempo");
            vezes(s, J1, Comando.CONFIRMAR, 4);               // INICIO, INICIAR TREINO, e mais um (nao repete)
        });
        assertEquals(1, treinos.get());
        Object[] r = iniciado.get();
        assertEquals(CatalogoPersonagens.ID_BRANCO, ((EscolhaLutador) r[0]).getIdBase());
        assertEquals(CatalogoPersonagens.ID_PRETO, ((EscolhaLutador) r[1]).getIdBase());
        assertEquals(Graduacao.AZUL, ((EscolhaLutador) r[1]).getGraduacao());
        assertFalse(RegistroCenarios.ID_PADRAO.equals(r[2]), "a arena escolhida");
        assertEquals(InicioLuta.DE_JOELHOS, r[3]);
        assertEquals(0, lutas.get(), "nada de luta competitiva");
    }

    // 12. Perfis no TREINO: lutador e parceiro pelo X, com aparencia e atributos do editor
    @Test
    void treinoComPerfis() throws Exception {
        perfis();
        naEdt(() -> {
            SelecaoPartidaPanel s = treino(null);
            s.comandar(J2, Comando.PERFIS);                   // qualquer teclado: e o lutador da vez
            assertEquals(SelecaoPartidaPanel.Fase.PERFIL, s.fase(J1));
            s.comandar(J1, Comando.CONFIRMAR);
            s.comandar(J1, Comando.PERFIS);
            s.comandar(J1, Comando.CONFIRMAR);
            assertTrue(s.ambosProntos());
            pintar(s);
            vezes(s, J1, Comando.CONFIRMAR, 4);
        });
        Object[] r = iniciado.get();
        EscolhaLutador l = (EscolhaLutador) r[0];
        assertEquals("Miau Relâmpago", l.getNome());
        assertEquals(Graduacao.BRANCA, l.getGraduacao());
        assertTrue(PerfilLutador.mesmosValores(FORTE, l.criarLutador(0, 0, 0.7).getAtributos()));
        assertEquals(LARANJA, l.criarLutador(0, 0, 0.7).getCorPelo());
        assertEquals("Miau Sombra", ((EscolhaLutador) r[1]).getNome());
    }

    // 18. Voltar entre as telas preserva as escolhas; cancelar no parceiro volta ao lutador
    @Test
    void voltarPreservaAsEscolhas() throws Exception {
        perfis();
        naEdt(() -> {
            SelecaoPartidaPanel s = treino(null);
            s.comandar(J1, Comando.PERFIS);
            s.comandar(J1, Comando.CONFIRMAR);                // lutador: perfil
            s.comandar(J1, Comando.CONFIRMAR);
            s.comandar(J1, Comando.DIREITA);
            s.comandar(J1, Comando.DIREITA);                  // parceiro: faixa roxa
            vezes(s, J1, Comando.CONFIRMAR, 3);               // kimono, PRONTO, AVANCAR
            s.comandar(J1, Comando.DIREITA);                  // outra arena
            String arena = s.mapaAtual();
            s.comandar(J1, Comando.VOLTAR);
            assertFalse(s.naConfiguracao());
            assertTrue(s.ambosProntos(), "voltar da arena mantem os dois prontos");
            assertEquals("Miau Relâmpago", s.escolha(J1).getNome());
            assertEquals(Graduacao.ROXA, s.escolha(J2).getGraduacao());
            s.comandar(J1, Comando.CONFIRMAR);
            assertEquals(arena, s.mapaAtual(), "e a arena tambem fica");
            s.comandar(J1, Comando.VOLTAR);
            vezes(s, J1, Comando.VOLTAR, 3);                  // parceiro: kimono, faixa, personagem
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, s.fase(J2));
            s.comandar(J1, Comando.VOLTAR);                   // cancelar no parceiro: volta ao lutador
            assertEquals(SelecaoPartidaPanel.Fase.PERFIL, s.fase(J1));
            assertEquals(J1, s.slotDaVez());
            assertEquals(0, voltas.get(), "nao saiu para o menu");
        });
    }

    // 17. Botao segurado: X, A e teclas confirmam uma vez so
    @Test
    void botaoSeguradoNaoConfirmaDuasVezes() throws Exception {
        perfis();
        ProvedorGamepadSimulado p = new ProvedorGamepadSimulado();
        p.conectar("g1", "id1", "Xbox");
        naEdt(() -> {
            SelecaoPartidaPanel s = treino(p);
            s.tick(16);                                       // arma o leitor
            p.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.OESTE));
            for (int i = 0; i < 40; i++) {
                s.tick(16);
            }
            assertEquals(SelecaoPartidaPanel.Fase.PERFIL, s.fase(J1), "X segurado: abre uma vez");
            p.definir("g1", EstadoGamepad.repouso().com(BotaoGamepad.SUL));
            for (int i = 0; i < 40; i++) {
                s.tick(16);
            }
            assertEquals(SelecaoPartidaPanel.Fase.PRONTO, s.fase(J1));
            assertEquals(SelecaoPartidaPanel.Fase.ESCOLHENDO, s.fase(J2), "A segurado nao confirma o parceiro");
            p.definir("g1", EstadoGamepad.repouso());
            s.tick(16);
            s.tecla("SPACE", true);
            s.tecla("SPACE", true);                           // repeticao automatica do sistema
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, s.fase(J2), "ESPACO segurado: um passo so");
            s.tecla("SPACE", false);
        });
    }

    // Os outros menus nao leem o X: o leitor padrao nunca emite PERFIS
    @Test
    void xSoNaSelecao() {
        LeitorNavegacao padrao = new LeitorNavegacao();
        padrao.ler(EstadoGamepad.repouso(), 0);
        assertTrue(padrao.ler(EstadoGamepad.repouso().com(BotaoGamepad.OESTE), 16).isEmpty());
        LeitorNavegacao selecao = new LeitorNavegacao(true);
        selecao.ler(EstadoGamepad.repouso(), 0);
        assertEquals(List.of(Comando.PERFIS), selecao.ler(EstadoGamepad.repouso().com(BotaoGamepad.OESTE), 16));
        assertTrue(selecao.ler(EstadoGamepad.repouso().com(BotaoGamepad.OESTE), 32).isEmpty(), "segurado");
        assertNull(NavegadorMenus.executar(new javax.swing.JPanel(), null, Comando.PERFIS), "nos menus, X nao volta");
    }
}
