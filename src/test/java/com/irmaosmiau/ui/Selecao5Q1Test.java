package com.irmaosmiau.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5Q.1: faixa e kimono por slot na selecao de partida e de treino. */
class Selecao5Q1Test {

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
    void partidaCadaSlotEscolheFaixaEKimono() throws Exception {
        AtomicReference<EscolhaLutador[]> inicio = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            SelecaoPartidaPanel sel = new SelecaoPartidaPanel(CatalogoPersonagens.padrao(), opcoes(),
                    RegistroCenarios.padrao(), RegistroCenarios.ID_PADRAO,
                    (a, b, c, t) -> inicio.set(new EscolhaLutador[] {a, b}), () -> { }, () -> null, () -> null);
            sel.comandar(J1, Comando.CONFIRMAR);                // Branco
            sel.comandar(J2, Comando.CONFIRMAR);                // Preto (o cursor do J2 ja comeca nele)
            // reorganizacao dos menus (pedido): os dois caem na FAIXA; confirmar segue para o KIMONO
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(J1));
            assertEquals(SelecaoPartidaPanel.Fase.FAIXA, sel.fase(J2));
            sel.comandar(J1, Comando.DIREITA);                  // azul
            sel.comandar(J1, Comando.DIREITA);                  // roxa
            sel.comandar(J1, Comando.CONFIRMAR);                // KIMONO
            sel.comandar(J1, Comando.DIREITA);                  // preto (padrao) -> branco
            sel.comandar(J2, Comando.DIREITA);                  // J2: faixa azul
            sel.comandar(J2, Comando.CONFIRMAR);                // J2: kimono
            sel.comandar(J2, Comando.DIREITA);                  // branco (padrao) -> azul
            assertEquals(Graduacao.ROXA, sel.faixa(J1));
            assertEquals(Graduacao.AZUL, sel.faixa(J2));
            BufferedImage img = new BufferedImage(800, 450, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            sel.setSize(800, 450);
            sel.paint(g);                                      // previa e atributos com a faixa escolhida
            g.dispose();
            sel.comandar(J1, Comando.CONFIRMAR);                // kimono confirmado: PRONTO
            sel.comandar(J2, Comando.CONFIRMAR);
            sel.comandar(J1, Comando.CONFIRMAR);                // AVANCAR
            for (int i = 0; i < 4; i++) {
                sel.comandar(J1, Comando.CONFIRMAR);            // ... INICIAR LUTA
            }
        });
        EscolhaLutador[] e = inicio.get();
        assertEquals(Graduacao.ROXA, e[0].getGraduacao());
        assertEquals(CorKimono.BRANCO, e[0].getKimono());
        assertEquals(Graduacao.AZUL, e[1].getGraduacao());
        assertEquals(CorKimono.AZUL, e[1].getKimono());
        assertEquals(CatalogoPersonagens.ID_PRETO, e[1].getIdBase());
    }

    @Test
    void treinoJogadorEParceiroComFaixasProprias() throws Exception {
        AtomicReference<EscolhaLutador[]> inicio = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            SelecaoPartidaPanel sel = SelecaoPartidaPanel.treino(CatalogoPersonagens.padrao(), opcoes(),
                    RegistroCenarios.padrao(), RegistroCenarios.ID_PADRAO, InicioLuta.DE_JOELHOS,
                    (j, p, c, i) -> inicio.set(new EscolhaLutador[] {j, p}), () -> { }, () -> null, () -> null);
            sel.comandar(J1, Comando.CONFIRMAR);
            personalizacao(sel);                                // 5Q.4 (pedido)
            for (int i = 0; i < 5; i++) {
                sel.comandar(J1, Comando.ANTERIOR);             // ... -> FAIXA DO PARCEIRO
            }
            assertEquals(SelecaoPartidaPanel.Linha.FAIXA_PARCEIRO, sel.linhaJ1());
            for (int i = 0; i < 4; i++) {
                sel.comandar(J1, Comando.DIREITA);              // parceiro faixa preta
            }
            sel.comandar(J1, Comando.ANTERIOR);
            sel.comandar(J1, Comando.ANTERIOR);                 // FAIXA do jogador
            sel.comandar(J1, Comando.DIREITA);                  // azul
            personalizacao(sel);                                // 5Q.4 (pedido): confirmar segue as linhas ate LUTAR
            sel.comandar(J1, Comando.CONFIRMAR);                // TREINAR
        });
        EscolhaLutador[] e = inicio.get();
        assertEquals(Graduacao.AZUL, e[0].getGraduacao());
        assertEquals(Graduacao.PRETA, e[1].getGraduacao());
    }

    /**
     * 5Q.4 (pedido): depois de escolher o personagem o J1 cai na FAIXA (antes ia direto para LUTAR);
     * confirma faixa e kimono (e os do parceiro, no treino) como o jogador faria, ate voltar a LUTAR.
     */
    private static void personalizacao(SelecaoPartidaPanel s) {
        for (int i = 0; i < 6 && s.linhaJ1() != SelecaoPartidaPanel.Linha.LUTAR; i++) {
            s.comandar(SlotJogador.JOGADOR_1, Comando.CONFIRMAR);
        }
    }
}
