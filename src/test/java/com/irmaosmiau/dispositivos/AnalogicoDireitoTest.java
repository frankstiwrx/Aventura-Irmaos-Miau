package com.irmaosmiau.dispositivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.CamadaComando;
import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.PosicaoComando;
import com.irmaosmiau.controles.SlotJogador;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Entrega 5H: analogico direito como direcao de tecnica (gamepad simulado, sem hardware). */
class AnalogicoDireitoTest {

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;

    private final LeitorAnalogicoTecnicas l = new LeitorAnalogicoTecnicas();

    @Test
    void quatroDirecoes() {
        assertEquals(DirecaoTecnica.CIMA, l.atualizar(0, -1));
        l.atualizar(0, 0);
        assertEquals(DirecaoTecnica.BAIXO, l.atualizar(0, 1));
        l.atualizar(0, 0);
        assertEquals(DirecaoTecnica.ESQUERDA, l.atualizar(-1, 0));
        l.atualizar(0, 0);
        assertEquals(DirecaoTecnica.DIREITA, l.atualizar(1, 0));
    }

    @Test
    void zonaMortaLimiarEHisterese() {
        assertNull(l.atualizar(0.2, 0), "zona morta");
        assertNull(l.atualizar(0.5, 0), "0,5 cru = 0,33 normalizado: abaixo do limiar de ativacao");
        assertEquals(DirecaoTecnica.DIREITA, l.atualizar(0.8, 0), "inclinacao intencional");
        assertEquals(DirecaoTecnica.DIREITA, l.atualizar(0.5, 0), "histerese: ainda nao voltou ao neutro");
        assertNull(l.atualizar(0.4, 0), "0,4 cru = 0,2: voltou ao neutro");
        assertEquals(DirecaoTecnica.DIREITA, l.atualizar(0.8, 0), "nova inclinacao");
    }

    @Test
    void diagonalNaoEscolheDirecaoEGirarNaoTroca() {
        assertNull(l.atualizar(0.7, -0.7), "diagonal exata: ambigua, espera");
        assertNull(l.atualizar(0.8, -0.5), "ainda perto da diagonal");
        assertEquals(DirecaoTecnica.DIREITA, l.atualizar(0.95, -0.1), "assentou num eixo: escolhe uma vez");
        assertEquals(DirecaoTecnica.DIREITA, l.atualizar(0, -1), "girar para cima sem voltar ao centro nao troca");
        l.atualizar(0, 0);
        assertEquals(DirecaoTecnica.CIMA, l.atualizar(0, -1));
        assertEquals(DirecaoTecnica.DIREITA, LeitorAnalogicoTecnicas.direcaoClara(0.9, -0.4), "~24 graus: direita");
        assertNull(LeitorAnalogicoTecnicas.direcaoClara(0.9, -0.6), "~34 graus: faixa diagonal");
    }

    @Test
    void desarmarExigeNeutro() {
        l.desarmar();
        assertNull(l.atualizar(0, -1), "desarmado: nada");
        l.atualizar(0, 0);
        assertEquals(DirecaoTecnica.CIMA, l.atualizar(0, -1));
    }

    // =====================================================
    // NA FONTE DO GAMEPAD (com o resolvedor das camadas)
    // =====================================================
    private static EstadoGamepad rs(double x, double y) {
        return EstadoGamepad.repouso().comEixo(EixoGamepad.DIR_X, (float) x).comEixo(EixoGamepad.DIR_Y, (float) y);
    }

    private static FonteGamepad fonte(EsquemaGamepad e) {
        return new FonteGamepad(J1, MapaGamepad.padrao().comEsquema(J1, e));
    }

    private static PosicaoComando p(CamadaComando c, DirecaoTecnica d) {
        return new PosicaoComando(c, d);
    }

    @Test
    void umaExecucaoPorInclinacaoComRetornoAoNeutro() {
        FonteGamepad f = fonte(EsquemaGamepad.CONTEXTUAL);
        f.atualizar(rs(0, -1));
        assertEquals(Optional.of(p(CamadaComando.UM, DirecaoTecnica.CIMA)), f.consumirComandoContextual());
        for (int i = 0; i < 100; i++) {
            f.atualizar(rs(0, -1));
            assertTrue(f.consumirComandoContextual().isEmpty(), "inclinado: sem repeticao");
        }
        f.atualizar(rs(1, 0));
        assertTrue(f.consumirComandoContextual().isEmpty(), "girou sem voltar: nada");
        f.atualizar(rs(0, 0));
        f.atualizar(rs(1, 0));
        assertEquals(Optional.of(p(CamadaComando.UM, DirecaoTecnica.DIREITA)), f.consumirComandoContextual());
    }

    @Test
    void camadasComLtLbEPrioridade() {
        FonteGamepad f = fonte(EsquemaGamepad.CONTEXTUAL);
        EstadoGamepad lt = rs(0, 1).com(BotaoGamepad.GATILHO_ESQ);
        f.atualizar(EstadoGamepad.repouso().com(BotaoGamepad.GATILHO_ESQ));
        f.atualizar(lt);
        assertEquals(Optional.of(p(CamadaComando.DOIS, DirecaoTecnica.BAIXO)), f.consumirComandoContextual());
        f.atualizar(EstadoGamepad.repouso());
        f.atualizar(rs(0, 1).com(BotaoGamepad.LB));
        assertEquals(Optional.of(p(CamadaComando.TRES, DirecaoTecnica.BAIXO)), f.consumirComandoContextual());
        f.atualizar(EstadoGamepad.repouso());
        f.atualizar(rs(0, 1).com(BotaoGamepad.LB).com(BotaoGamepad.GATILHO_ESQ));
        assertEquals(Optional.of(p(CamadaComando.QUATRO, DirecaoTecnica.BAIXO)), f.consumirComandoContextual(),
                "LT + LB = avancada, nunca camada 2 ou 3");
    }

    @Test
    void r3NaoViraTecnicaEDpadContinuaAlternativo() {
        FonteGamepad f = fonte(EsquemaGamepad.CONTEXTUAL);
        f.atualizar(rs(0.1, -0.9).com(BotaoGamepad.R3));          // clique empurrando o analogico
        assertTrue(f.consumirComandoContextual().isEmpty(), "R3 apertado: sem tecnica");
        f.atualizar(rs(0.1, -0.9));                                // soltou o clique, ainda inclinado
        assertTrue(f.consumirComandoContextual().isEmpty(), "so depois do neutro");
        f.atualizar(rs(0, 0));
        f.atualizar(EstadoGamepad.repouso().com(BotaoGamepad.DPAD_ESQUERDA));
        assertEquals(Optional.of(p(CamadaComando.UM, DirecaoTecnica.ESQUERDA)), f.consumirComandoContextual(),
                "D-pad continua funcionando");
        f.atualizar(EstadoGamepad.repouso());
        f.atualizar(rs(0, -1).com(BotaoGamepad.DPAD_DIREITA));
        assertTrue(f.consumirComandoContextual().isEmpty(), "RS e D-pad em direcoes diferentes: nada");
    }

    @Test
    void limpezaEClassicoIgnoraOAnalogicoDireito() {
        FonteGamepad f = fonte(EsquemaGamepad.CONTEXTUAL);
        f.atualizar(rs(0, -1));
        f.liberarTodas();                                          // perda de foco
        assertTrue(f.consumirComandoContextual().isEmpty());
        f.atualizar(rs(0, -1));
        assertTrue(f.consumirComandoContextual().isEmpty(), "segurado desde antes");
        f.atualizar(null);                                         // desconexao
        f.atualizar(rs(0, -1));
        assertTrue(f.consumirComandoContextual().isEmpty());
        f.atualizar(rs(0, 0));
        f.atualizar(rs(0, -1));
        assertTrue(f.consumirComandoContextual().isPresent());

        FonteGamepad c = fonte(EsquemaGamepad.CLASSICO);
        c.atualizar(rs(0, -1));
        assertTrue(c.consumirComandoContextual().isEmpty(), "no Classico o analogico direito nao faz nada");
    }
}
