package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.NaturezaAcao;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.Resultado;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.8: +12 finalizacoes (LT + LB da guarda fechada, da montada e dos cem quilos), rotas novas
 * no RS das costas (por cima) e do ashi (quem controla) e a entrada na perna no comando novo.
 */
class Repertorio5Q8Test {

    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;

    private void novo() {
        a = new IrmaoMiauBranco(300, 150, 0.7);
        b = new IrmaoMiauPreto(380, 150, 0.7);
        a.setViradoParaDireita(true);
        b.setViradoParaDireita(false);
        a.setGraduacao(Graduacao.PRETA);
        b.setGraduacao(Graduacao.PRETA);
        m = new MotorSolo(a, b, null, 15, 715);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            m.informarIntencoes(Intencao.NENHUMA, Intencao.NENHUMA);
            m.atualizar(Intencao.NENHUMA, Intencao.NENHUMA);
            a.atualizar(false, false);
            b.atualizar(false, false);
        }
    }

    private void em(Posicao p, Lado cima, Lado ctrl) {
        m.forcarPosicao(p, cima, ctrl);
        ticks(30);
        m.definirEstabilidade(95);
    }

    private void resolver() {
        for (int i = 0; i < 150 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        ticks(1);
    }

    private static final Map<Posicao, List<String>> NOVAS = Map.of(
            Posicao.GUARDA, List.of("gogoplata", "baratoplata", "tarikoplata", "ezequiel_guarda"),
            Posicao.MONTADA, List.of("monoplata", "armlock_s", "gola_unica", "mao_de_vaca"),
            Posicao.CONTROLE_LATERAL, List.of("norte_sul", "darce", "anaconda", "gravata_peruana"));
    private static final Direcao[] ORDEM = {Direcao.CIMA, Direcao.DIREITA, Direcao.BAIXO, Direcao.ESQUERDA};

    @Test
    void dozeFinalizacoesNovasNoLtLbDeCadaPosicao() {
        CatalogoFinalizacoes c = CatalogoFinalizacoes.referencia();
        assertEquals(70, c.todas().size());            // complemento 5Q.8 (pedido): + as 12 do RT
        for (Map.Entry<Posicao, List<String>> e : NOVAS.entrySet()) {
            for (int i = 0; i < 4; i++) {
                Finalizacao f = c.naPosicao(e.getKey(), Familia.L1_L2, ORDEM[i]).orElseThrow();
                assertEquals(e.getValue().get(i), f.tecnicaBase(), e.getKey() + " " + ORDEM[i]);
                assertEquals(NaturezaAcao.FINALIZACAO, ClassificacaoAcoes.de(f), "cor de finalizacao");
                assertTrue(f.categoria() != CategoriaFinalizacao.PERNA, "nao sao chaves de perna: " + f.id());
            }
        }
    }

    @Test
    void cadaFinalizacaoNovaComecaPeloComandoNaPosicaoCerta() {
        for (Map.Entry<Posicao, List<String>> e : NOVAS.entrySet()) {
            for (int i = 0; i < 4; i++) {
                for (Lado x : Lado.values()) {
                    novo();
                    Posicao p = e.getKey();
                    // guarda: quem controla esta por baixo; montada e cem quilos: por cima
                    em(p, p == Posicao.GUARDA ? x.outro() : x, x);
                    MotorSolo.Resposta r = m.comando(x, new ComandoSolo(ORDEM[i], Familia.L1_L2));
                    assertEquals(Resultado.INICIADA, r.resultado(), p + " " + ORDEM[i] + " " + x + ": " + r.motivo());
                    assertEquals(e.getValue().get(i), m.getAtaque().orElseThrow().finalizacao().tecnicaBase());
                }
            }
        }
    }

    @Test
    void costasPorCimaGanharamMontadaCemQuilosETrianguloDeCorpo() {
        CatalogoTecnicasSolo c = CatalogoTecnicasSolo.referencia();
        novo();
        em(Posicao.COSTAS, Lado.A, Lado.A);
        assertEquals(CatalogoTecnicasSolo.COSTAS_PARA_MONTADA, m.tecnicaPara(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.RS)).orElseThrow().id());
        assertEquals(CatalogoTecnicasSolo.COSTAS_PARA_CEM, m.tecnicaPara(Lado.A, new ComandoSolo(Direcao.DIREITA, Familia.RS)).orElseThrow().id());
        assertEquals(CatalogoTecnicasSolo.AJUSTAR_GANCHOS, m.tecnicaPara(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.RS)).orElseThrow().id(), "ganchos preservado");
        assertEquals(CatalogoTecnicasSolo.TRIANGULO_CORPO, m.tecnicaPara(Lado.A, new ComandoSolo(Direcao.ESQUERDA, Familia.RS)).orElseThrow().id());
        assertEquals(Resultado.INICIADA, m.comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.RS)).resultado());
        resolver();
        assertEquals(Posicao.MONTADA, m.getPosicao(), "virou para a montada");
        novo();
        em(Posicao.COSTAS, Lado.B, Lado.B);
        m.comando(Lado.B, new ComandoSolo(Direcao.DIREITA, Familia.RS));
        resolver();
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao(), "foi para os cem quilos");
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow());
        novo();
        em(Posicao.COSTAS, Lado.A, Lado.A);
        m.definirEstabilidade(40);
        assertEquals(Resultado.CONCLUIDA, m.comando(Lado.A, new ComandoSolo(Direcao.ESQUERDA, Familia.RS)).resultado());
        assertTrue(m.getEstabilidade() > 40, "o triangulo de corpo consolida");
        assertEquals(NaturezaAcao.TRANSICAO, ClassificacaoAcoes.de(c.exigir(CatalogoTecnicasSolo.COSTAS_PARA_MONTADA)));
    }

    @Test
    void ashiPorBaixoGanhouSlxGuardaERaspagem() {
        novo();
        em(Posicao.ASHI_GARAMI, Lado.B, Lado.A);
        assertEquals(CatalogoTecnicasSolo.CONSOLIDAR_ASHI, m.tecnicaPara(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.RS)).orElseThrow().id(), "ajustar preservado");
        m.comando(Lado.A, new ComandoSolo(Direcao.DIREITA, Familia.RS));
        resolver();
        assertEquals(Posicao.SINGLE_LEG_X, m.getPosicao(), "subiu para o SLX");
        assertEquals(Lado.A, m.getEstado().getControlador().orElseThrow());
        novo();
        em(Posicao.ASHI_GARAMI, Lado.B, Lado.A);
        m.comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.RS));
        resolver();
        assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao(), "recompos a guarda");
        novo();
        em(Posicao.ASHI_GARAMI, Lado.B, Lado.A);
        m.comando(Lado.A, new ComandoSolo(Direcao.ESQUERDA, Familia.RS));
        resolver();
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao(), "raspou e levantou (com controle)");
        assertEquals(Lado.A, m.getEstado().getPorCima().orElseThrow());
        novo();
        em(Posicao.ASHI_GARAMI, Lado.B, Lado.A);
        m.definirEstabilidade(10);
        m.comando(Lado.A, new ComandoSolo(Direcao.ESQUERDA, Familia.RS));
        resolver();
        assertEquals(Posicao.ASHI_GARAMI, m.getPosicao(), "sem controle a raspagem nao entra");
    }

    @Test
    void entradaNaPernaNoComandoNovo() {
        novo();
        em(Posicao.MONTADA, Lado.A, Lado.A);
        assertEquals(CatalogoTecnicasSolo.ENTRADA_PERNA_DOMINANTE,
                m.tecnicaPara(Lado.A, new ComandoSolo(Direcao.ESQUERDA, Familia.R1)).orElseThrow().id(), "RB <-");
        novo();
        em(Posicao.GUARDA, Lado.A, Lado.B);
        assertEquals(CatalogoTecnicasSolo.ENTRADA_PERNA_GUARDA,
                m.tecnicaPara(Lado.B, new ComandoSolo(Direcao.DIREITA, Familia.RS)).orElseThrow().id(), "RS ->");
        assertEquals(CatalogoTecnicasSolo.SOLTAR_GUARDA,
                m.tecnicaPara(Lado.B, new ComandoSolo(Direcao.ESQUERDA, Familia.RS)).orElseThrow().id(), "SOLTAR no RS <-");
    }

    @Test
    void avisoDeFinalizacaoNovaSaiComACorDeFinalizacao() {
        MotorSolo.Evento e = new MotorSolo.Evento(1, "FIN_CONTROLE", "gogoplata@guarda", Lado.B, "");
        assertEquals(NaturezaAcao.FINALIZACAO, NarradorSolo.natureza(e));
        assertTrue(NarradorSolo.mensagem(e).toLowerCase().contains("gogoplata"), NarradorSolo.mensagem(e));
    }
}
