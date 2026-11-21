package com.irmaosmiau.dispositivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.DirecaoTecnica;
import com.irmaosmiau.controles.FamiliaTecnica;
import com.irmaosmiau.controles.SlotJogador;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Entrega 5O (testes de controle 1-19): mapeamento real e familias no gamepad. */
class Familias5OTest {

    // 5O.2 (pedido): o padrao virou o Contextual; este teste e do Classico (MapaGamepad.classico() = o padrao antigo).

    private static final SlotJogador J1 = SlotJogador.JOGADOR_1;

    private static EstadoGamepad r() {
        return EstadoGamepad.repouso();
    }

    private static EstadoGamepad rs(EstadoGamepad e, float x, float y) {
        return e.comEixo(EixoGamepad.DIR_X, x).comEixo(EixoGamepad.DIR_Y, y);
    }

    private static EstadoGamepad rt(EstadoGamepad e) {
        return e.comEixo(EixoGamepad.GATILHO_DIR, 1f);
    }

    private static FonteGamepad fonte(EsquemaGamepad esquema) {
        FonteGamepad f = new FonteGamepad(J1, MapaGamepad.classico().comEsquema(J1, esquema));
        f.atualizar(r());                                  // arma o leitor do RS no neutro
        return f;
    }

    // Mapeamento real (auditoria): R3 rola, RB + A pulo alto, B agacha (Contextual), RT = defesas, RB = quedas, Y = acrobacia
    @Test
    void mapeamentoDosBotoes() {
        for (EsquemaGamepad e : EsquemaGamepad.values()) {
            MapaGamepad m = MapaGamepad.classico().comEsquema(J1, e);
            assertTrue(m.isValido(), m.problemas().toString());
            assertEquals(BotaoGamepad.R3, m.botao(J1, ItemGamepad.de(AcaoJogador.ROLAR)).orElseThrow(), "R3 rola");
            assertEquals(BotaoGamepad.RB, m.botao(J1, ItemGamepad.de(ComandoGamepad.MODIFICADOR_PULO_ALTO)).orElseThrow());
            assertEquals(BotaoGamepad.GATILHO_DIR, m.botao(J1, ItemGamepad.de(AcaoJogador.MODIFICADOR_DEFESA)).orElseThrow());
            assertEquals(BotaoGamepad.RB, m.botao(J1, ItemGamepad.de(AcaoJogador.MODIFICADOR_PROJECAO)).orElseThrow());
            // 5O.1 (pedido): o Y nao faz mais acrobacia (agora R3 + direcao em pe) e o Select nao respira.
            assertTrue(m.quemUsa(J1, BotaoGamepad.VOLTAR).isEmpty(), "Select livre = RB logico");
            // complemento 5Q.8 (pedido): respirar saiu do RT sozinho e foi para o Y/Triangulo
            assertEquals(BotaoGamepad.NORTE, m.botao(J1, ItemGamepad.de(AcaoJogador.RESPIRAR)).orElseThrow(), "respirar = Y");
            assertTrue(m.botao(J1, ItemGamepad.de(AcaoJogador.SPRAWL)).isEmpty(), "o sprawl agora e RT + RS para baixo");
        }
        MapaGamepad ctx = MapaGamepad.classico().comEsquema(J1, EsquemaGamepad.CONTEXTUAL);
        assertEquals(BotaoGamepad.LESTE, ctx.botao(J1, ItemGamepad.de(AcaoJogador.AGACHAR)).orElseThrow(), "B agacha");
        MapaGamepad cls = MapaGamepad.classico();
        assertEquals(BotaoGamepad.RB, cls.botao(J1, ItemGamepad.de(AcaoJogador.CORRER)).orElseThrow(), "corrida no RB");
        assertEquals(BotaoGamepad.NORTE, cls.botao(J1, ItemGamepad.de(AcaoJogador.REFORCAR_PEGADA)).orElseThrow());
    }

    // gamepad.json nunca personalizado da 5N vira o padrao da 5O
    @Test
    void padraoDa5NReconhecido() {
        for (EsquemaGamepad e : EsquemaGamepad.values()) {
            MapaGamepad.ConfigSlot antigo = MapaGamepad.padrao5N(J1, e);
            assertEquals(BotaoGamepad.GATILHO_DIR, antigo.acoes().get(AcaoJogador.SPRAWL), "na 5N o RT era o sprawl");
            assertFalse(antigo.acoes().containsKey(AcaoJogador.MODIFICADOR_DEFESA));
        }
    }

    // 1, 18, 19: RS sozinho = pegada, uma por inclinacao; volta ao neutro arma de novo
    @Test
    void rsSozinhoPegaUmaVez() {
        FonteGamepad f = fonte(EsquemaGamepad.CLASSICO);
        f.atualizar(rs(r(), 0, -1f));
        assertEquals(Optional.of(DirecaoTecnica.CIMA), f.consumirPegadaClassica());
        for (int i = 0; i < 30; i++) {
            f.atualizar(rs(r(), 0, -1f));
        }
        assertTrue(f.consumirPegadaClassica().isEmpty(), "segurar nao repete");
        f.atualizar(r());
        f.atualizar(rs(r(), 1f, 0));
        assertEquals(Optional.of(DirecaoTecnica.DIREITA), f.consumirPegadaClassica(), "neutro arma de novo");
        assertEquals(FamiliaTecnica.PEGADAS, f.familiaAtiva());
    }

    // 2-6: RT + RS = defesa (nunca pegada), com direcao segurada para as guardas
    @Test
    void rtMaisRsEscolheDefesa() {
        for (EsquemaGamepad e : EsquemaGamepad.values()) {
            FonteGamepad f = fonte(e);
            f.atualizar(rt(r()));
            assertEquals(FamiliaTecnica.DEFESAS, f.familiaAtiva());
            for (DirecaoTecnica d : DirecaoTecnica.values()) {
                float x = d == DirecaoTecnica.DIREITA ? 1f : d == DirecaoTecnica.ESQUERDA ? -1f : 0;
                float y = d == DirecaoTecnica.BAIXO ? 1f : d == DirecaoTecnica.CIMA ? -1f : 0;
                f.atualizar(rs(rt(r()), x, y));
                assertEquals(d, f.direcaoFamiliaSegurada(), e + " " + d);
                assertEquals(new FamiliaTecnica.Escolha(FamiliaTecnica.DEFESAS, d), f.consumirEscolhaFamilia().orElseThrow());
                assertTrue(f.consumirPegadaClassica().isEmpty(), "nao pega");
                assertTrue(f.consumirComandoContextual().isEmpty(), "nem no Contextual");
                f.atualizar(rt(r()));
            }
        }
    }

    // 17: RT e RB juntos: DEFESAS (prioridade), uma familia so
    @Test
    void prioridadeDosModificadores() {
        assertEquals(FamiliaTecnica.DEFESAS, FamiliaTecnica.dosModificadores(true, true));
        assertEquals(FamiliaTecnica.PROJECOES, FamiliaTecnica.dosModificadores(false, true));
        assertEquals(FamiliaTecnica.PEGADAS, FamiliaTecnica.dosModificadores(false, false));
        FonteGamepad f = fonte(EsquemaGamepad.CONTEXTUAL);
        f.atualizar(rs(rt(r()).com(BotaoGamepad.RB), 0, -1f));
        assertEquals(FamiliaTecnica.DEFESAS, f.consumirEscolhaFamilia().orElseThrow().familia());
        f.atualizar(r());
        f.atualizar(rs(r().com(BotaoGamepad.RB), 0, -1f));
        assertEquals(FamiliaTecnica.PROJECOES, f.consumirEscolhaFamilia().orElseThrow().familia());
    }

    // Soltar o RT com o RS inclinado NAO vira pegada (precisa voltar ao neutro)
    @Test
    void soltarRtComRsInclinadoNaoPega() {
        FonteGamepad f = fonte(EsquemaGamepad.CLASSICO);
        f.atualizar(rs(rt(r()), 0, -1f));
        f.consumirEscolhaFamilia();
        f.atualizar(rs(r(), 0, -1f));
        f.atualizar(rs(r(), 0, -1f));
        assertTrue(f.consumirPegadaClassica().isEmpty());
        f.atualizar(r());
        f.atualizar(rs(r(), 0, -1f));
        assertTrue(f.consumirPegadaClassica().isPresent(), "depois do neutro volta a pegar");
    }

    // 12: diagnostico do RB (fisico)
    @Test
    void diagnosticoDoRb() {
        FonteGamepad f = fonte(EsquemaGamepad.CLASSICO);
        f.atualizar(r().com(BotaoGamepad.RB));
        assertTrue(f.botaoFisico(BotaoGamepad.RB));
        assertTrue(f.isAtiva(AcaoJogador.MODIFICADOR_PROJECAO), "logico: quedas");
        assertTrue(f.isAtiva(AcaoJogador.CORRER), "e a corrida continua no Classico");
        f.atualizar(null);
        assertFalse(f.temLeitura());
    }

    // 8: RB + A continua sendo o pulo alto
    @Test
    void rbMaisAPuloAlto() {
        FonteGamepad f = fonte(EsquemaGamepad.CONTEXTUAL);
        f.atualizar(r().com(BotaoGamepad.RB).com(BotaoGamepad.SUL));
        assertTrue(f.isAtiva(AcaoJogador.PULAR));
    }
}
