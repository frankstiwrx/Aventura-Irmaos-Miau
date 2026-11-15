package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.dispositivos.EixoGamepad;
import com.irmaosmiau.dispositivos.EstadoGamepad;
import com.irmaosmiau.dispositivos.ProvedorGamepadSimulado;
import com.irmaosmiau.dispositivos.ServicoGamepads;
import com.irmaosmiau.dispositivos.TipoDispositivo;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.TipoPersonagem;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.preferencias.PreferenciasDispositivos;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5P no GamePanel real: quedas com a pegada (acompanhar, ficar em pe,
 * reversao, defesa), J2 derrubando pelo teclado-controle, comandos de solo pelo
 * teclado do J1 / teclado-controle do J2 / Xbox, o mesmo comando por posicao,
 * vinculo (sem andar), F5, arenas e HUD.
 */
class Solo5PGamePanelTest extends Base5OGamePanel {

    private MotorSolo m() {
        return painel.getMotorSolo();
    }

    private void ctl(String tecla, boolean p) {
        tecla("pretoControle" + tecla, p);
    }

    private void toqueCtl(String tecla) {
        ctl(tecla, true);
        ticks(1);
        ctl(tecla, false);
        ticks(1);
    }

    /** J1 (teclas) segura o 4 (RB) e toca a tecla de direcao. */
    private void j1Rb(String direcao) {
        tecla("brancoModProjecao", true);
        toque(direcao);
        tecla("brancoModProjecao", false);
    }

    private void esperarAssentar() {
        ticks(60);
    }

    // ---------------- 10: J1 derruba com a pegada e acompanha
    @Test
    void j1QuedaComPegadaAcompanhandoTerminaPorCima() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            j1Rb("brancoPegada2");                              // RB + direita = queda com a pegada
            tecla("brancoDireita", true);                       // acompanhar (LS para o adversario) na janela
            ticks(30);
            tecla("brancoDireita", false);
            esperarAssentar();
            assertEquals(Posicao.CONTROLE_LATERAL, m().getPosicao());
            assertEquals(Lado.A, m().getEstado().getPorCima().orElseThrow());
            assertFalse(s().estaEmInteracao(), "a pegada em pe acabou na queda");
            assertTrue(j2().isDeitado() && j1().isAjoelhado());
        });
    }

    // ---------------- 9: derrubar e ficar em pe (sem acompanhar)
    @Test
    void j1QuedaSemAcompanharFicaEmPe() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            j1Rb("brancoPegada2");
            ticks(30);
            assertEquals(Posicao.EM_PE_CONTRA_SOLO, m().getPosicao());
            assertTrue(j2().isDeitado());
            assertFalse(j1().isDeitado() || j1().isAjoelhado(), "J1 continua em pe");
            assertFalse(m().conectado(j1()), "sem vinculo: o J1 pode se afastar ou entrar na guarda");
        });
    }

    // ---------------- 4: reversao da queda (contra-queda do J2 na janela)
    @Test
    void j2ReverteAQuedaDoJ1() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            j1Rb("brancoPegada2");
            ticks(4);
            ctl("O", true);                                     // J2: RB + direita na janela = contra-queda
            toqueCtl("RIGHT");
            ctl("O", false);
            ticks(MotorSolo.ATERRISSAGEM_TICKS + 2);
            esperarAssentar();
            assertTrue(j1().isDeitado(), "quem tentou a queda foi ao chao");
            assertEquals(Lado.B, m().getEstado().getPorCima().orElseThrow(), "o J2 caiu por cima");
        });
    }

    // ---------------- 3: entrada defendida, os dois continuam em pe
    @Test
    void quedaDefendidaComBaseOsDoisFicamEmPe() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            j1Rb("brancoPegada2");
            ctl("P", true);                                     // J2 agarrado segura o RT: faz base
            ticks(30);
            ctl("P", false);
            assertFalse(j1().isDeitado() || j2().isDeitado());
            assertTrue(m().getPosicao() == Posicao.EM_PE_PEGADA || m().getPosicao() == Posicao.EM_PE_SEPARADOS);
            assertTrue(m().exposicaoDe(Lado.A).isPresent(), "quem entrou ficou exposto");
        });
    }

    // ---------------- 2, 27: o J2 (teclado-controle) derruba o J1 pelas pernas
    @Test
    void j2DerrubaJ1PelasPernasPeloTeclado() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            ctl("A", true);
            for (int i = 0; i < 300 && Math.abs(j1().getX() - j2().getX()) > 100; i++) {
                ticks(1);
            }
            ctl("A", false);
            ctl("O", true);
            toqueCtl("DOWN");
            ctl("O", false);
            ticks(20);
            esperarAssentar();
            assertEquals(Posicao.CONTROLE_LATERAL, m().getPosicao());
            assertEquals(Lado.B, m().getEstado().getPorCima().orElseThrow());
        });
    }

    // ---------------- 12-14, 21, 23, 27: comandos de solo pelos dois teclados; raspagem troca os papeis
    @Test
    void naGuardaJ1PassaEJ2RaspaNaJanela() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
            esperarAssentar();
            toque("brancoPegada1");                             // J1 (por cima): cima = passar
            assertTrue(m().getTentativa().isPresent());
            toqueCtl("UP");                                     // J2 (por baixo): cima = raspagem (contra)
            esperarAssentar();
            assertEquals(Lado.B, m().getEstado().getPorCima().orElseThrow(), "J2 raspou");
            assertTrue(j2().isAjoelhado() && j1().isDeitado());
            assertTrue(painel.montarQuadroDpad(SlotJogador.JOGADOR_2).nomeCamada().contains("GUARDA ABERTA"));
        });
    }

    @Test
    void oMesmoComandoFazOutraCoisaEmOutraPosicao() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            m().forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
            esperarAssentar();
            toque("brancoPegada1");                             // cima nos cem quilos = montar
            assertEquals("montar", m().getTentativa().orElseThrow().tecnica().id());
            ticks(25);
            esperarAssentar();
            assertEquals(Posicao.MONTADA, m().getPosicao());
            ticks(MotorSolo.RECARGA_TICKS);
            double antes = m().getEstabilidade();
            toque("brancoPegada1");                             // cima na montada = montada alta (ajuste)
            assertTrue(m().getEstabilidade() > antes);
            assertEquals(Posicao.MONTADA, m().getPosicao());
        });
    }

    // ---------------- 27: J1 no Xbox (RS) no chao
    @Test
    void j1NoXboxUsaORsNoChao() throws Exception {
        naEdt(() -> {
            ProvedorGamepadSimulado sim = new ProvedorGamepadSimulado();
            sim.conectar("g1", "ID-1", "Controle 1");
            ServicoGamepads.instalarGlobal(sim);
            PreferenciasDispositivos.instalarGlobal(PreferenciasDispositivos.emMemoria(
                    AssociacaoDispositivos.padrao().com(SlotJogador.JOGADOR_1, TipoDispositivo.GAMEPAD)));
            painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.EM_PE, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(2);
            m().forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.B, Lado.B);
            esperarAssentar();
            m().definirEstabilidade(10);
            sim.definir("g1", EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_X, 1f));   // RS lado = escape de quadril
            ticks(1);
            sim.definir("g1", EstadoGamepad.repouso());
            ticks(14);
            esperarAssentar();
            assertEquals(Posicao.GUARDA_ABERTA, m().getPosicao(), "J1 escapou pelo Xbox");
            assertEquals(Lado.A, m().getEstado().getControlador().orElseThrow());
        });
    }

    // ---------------- 13 (movimento no solo), 26: ligado nao anda; a dupla fica junta, na mesma linha e na arena
    @Test
    void ligadoNoSoloNinguemSeAfastaEDuplaFicaNaArena() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            painel.usarCenario("torneio");
            m().forcarPosicao(Posicao.MONTADA, Lado.B, Lado.B);
            esperarAssentar();
            int x1 = j1().getX();
            tecla("brancoEsquerda", true);                      // o de baixo tenta sair andando
            tecla("brancoRolar", true);
            tecla("brancoDeitar", true);
            ticks(40);
            tecla("brancoEsquerda", false);
            tecla("brancoRolar", false);
            tecla("brancoDeitar", false);
            assertEquals(x1, j1().getX(), "preso na posicao: nao sai andando nem rolando");
            assertEquals(Posicao.MONTADA, m().getPosicao());
            assertEquals(j1().getLinhaAtual(), j2().getLinhaAtual(), "mesma linha");
            assertTrue(j1().isVinculadoNoSolo() && j2().isVinculadoNoSolo());
            for (int x : new int[] {j1().getX(), j2().getX()}) {
                assertTrue(x >= GamePanel.LIMITE_ESQUERDO_ARENA && x <= GamePanel.LIMITE_DIREITO_ARENA);
            }
            String f3 = String.join(" ", painel.linhasTecnicas());
            assertTrue(f3.contains("SOLO MONTADA cima B"), f3);
        });
    }

    // ---------------- 25: F5 (treino) e luta nova limpam os vinculos
    @Test
    void f5ELutaNovaLimpamTudo() throws Exception {
        naEdt(() -> {
            painel = new GamePanel(ConfiguracaoTreino.padrao(), () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(2);
            m().forcarPosicao(Posicao.COSTAS, Lado.A, Lado.A);
            ticks(5);
            painel.reiniciarTreino();
            ticks(2);
            assertEquals(Posicao.EM_PE_SEPARADOS, m().getPosicao());
            assertFalse(j1().isVinculadoNoSolo() || j2().isVinculadoNoSolo());
            novo(InicioLuta.EM_PE);
            assertEquals(Posicao.EM_PE_SEPARADOS, m().getPosicao(), "luta nova: sem vinculo");
        });
    }

    // ---------------- puxar guarda pelo teclado do J1 e HUD de quedas em pe
    @Test
    void j1PuxaGuardaEHudMostraAsQuedas() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            tecla("brancoModProjecao", true);
            ticks(1);
            assertTrue(painel.montarQuadroDpad(SlotJogador.JOGADOR_1).nomeCamada().startsWith("QUEDAS"));
            toque("brancoPegada4");                             // RB + esquerda = puxar guarda
            tecla("brancoModProjecao", false);
            ticks(12);
            esperarAssentar();
            assertEquals(Posicao.GUARDA, m().getPosicao());
            assertEquals(Lado.A, m().getEstado().getControlador().orElseThrow(), "quem puxou controla (por baixo)");
            assertEquals(Lado.B, m().getEstado().getPorCima().orElseThrow());
        });
    }
}
