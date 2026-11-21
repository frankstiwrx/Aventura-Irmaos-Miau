package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Complemento da 5Q.8: as rodas de finalizacao do RT por cima (montada, cem quilos, costas), as quatro
 * entradas do LT + LB nas costas (entrar na perna ja indo para a chave) e a auditoria do catalogo.
 */
class RodasRt5Q8Test {

    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;

    private void novo(Graduacao g) {
        a = new IrmaoMiauBranco(300, 150, 0.7);
        b = new IrmaoMiauPreto(380, 150, 0.7);
        a.setViradoParaDireita(true);
        b.setViradoParaDireita(false);
        a.setGraduacao(g);
        b.setGraduacao(g);
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

    private static ComandoSolo rt(Direcao d) {
        return new ComandoSolo(d, Familia.R2);
    }

    private static final Direcao[] ORDEM = {Direcao.CIMA, Direcao.DIREITA, Direcao.BAIXO, Direcao.ESQUERDA};

    static final Map<Posicao, List<String>> RT = Map.of(
            Posicao.MONTADA, List.of("lapela", "biceps", "antebraco", "guilhotina"),
            Posicao.CONTROLE_LATERAL, List.of("cortador_papel", "lapela", "von_flue", "mir_lock"),
            Posicao.COSTAS, List.of("short_choke", "ezequiel", "triangulo", "lapela"));

    // 4 + 6: quatro opcoes distintas em cada roda RT, todas finalizacoes (vermelho) com graduacao e alvo
    @Test
    void quatroFinalizacoesDistintasEmCadaRodaRt() {
        CatalogoFinalizacoes c = CatalogoFinalizacoes.referencia();
        for (Map.Entry<Posicao, List<String>> e : RT.entrySet()) {
            Set<String> nomes = new HashSet<>();
            for (int i = 0; i < 4; i++) {
                Finalizacao f = c.naPosicao(e.getKey(), Familia.R2, ORDEM[i]).orElseThrow();
                assertEquals(e.getValue().get(i), f.tecnicaBase(), e.getKey() + " " + ORDEM[i]);
                assertEquals(NaturezaAcao.FINALIZACAO, ClassificacaoAcoes.de(f));
                assertEquals(PapelPosicional.CONTROLADOR, f.papel(), "so quem controla (por cima)");
                assertTrue(f.requisito() == Finalizacao.Requisito.CONTROLE && f.limiarControle() >= 45,
                        "pede controle: " + f.id());
                assertTrue(nomes.add(f.rotulo()), "rotulos diferentes na roda: " + f.rotulo());
                assertTrue(f.rotulo().length() <= 10, "cabe na celula: " + f.rotulo());
            }
        }
        // so nas tres posicoes dominantes
        assertEquals(12, c.todas().stream().filter(f -> f.familia() == Familia.R2).count());
        assertTrue(c.todas().stream().filter(f -> f.familia() == Familia.R2).allMatch(f -> RT.containsKey(f.posicao())));
    }

    // 16: nada duplicado indevidamente
    @Test
    void catalogoSemDuplicatasEContagemHonesta() {
        CatalogoFinalizacoes c = CatalogoFinalizacoes.referencia();
        assertEquals(70, c.todas().size(), "58 + 12");
        assertEquals(48, c.todas().stream().map(Finalizacao::tecnicaBase).distinct().count(), "41 + 7 ineditas");
        // a mesma tecnica-base nunca aparece duas vezes na mesma posicao (nome novo para a mesma entrada)
        Set<String> basePorPosicao = new HashSet<>();
        for (Finalizacao f : c.todas()) {
            assertTrue(basePorPosicao.add(f.posicao() + "/" + f.tecnicaBase()), "duplicada: " + f.id());
        }
        // as 7 ineditas e as 5 variacoes (bases que ja existiam em outra posicao)
        for (String nova : List.of("lapela", "biceps", "antebraco", "cortador_papel", "von_flue", "mir_lock",
                "short_choke")) {
            assertTrue(c.todas().stream().filter(f -> f.tecnicaBase().equals(nova)).allMatch(f -> f.familia() == Familia.R2),
                    "inedita so nas rodas RT: " + nova);
        }
        for (String var : List.of("guilhotina", "ezequiel", "triangulo")) {
            assertTrue(c.todas().stream().anyMatch(f -> f.tecnicaBase().equals(var) && f.familia() != Familia.R2),
                    "variacao de uma base que ja existia: " + var);
        }
        // as 12 do LT + LB da 5Q.8 continuam iguais (12)
        assertEquals("monoplata", c.naPosicao(Posicao.MONTADA, Familia.L1_L2, Direcao.CIMA).orElseThrow().tecnicaBase());
        assertEquals("gogoplata", c.naPosicao(Posicao.GUARDA, Familia.L1_L2, Direcao.CIMA).orElseThrow().tecnicaBase());
        assertEquals("gravata_peruana",
                c.naPosicao(Posicao.CONTROLE_LATERAL, Familia.L1_L2, Direcao.ESQUERDA).orElseThrow().tecnicaBase());
    }

    // 1-3 + 5: cada finalizacao do RT comeca pelo comando, na posicao certa, para quem esta por cima
    @Test
    void cadaFinalizacaoDoRtComecaPorCima() {
        for (Map.Entry<Posicao, List<String>> e : RT.entrySet()) {
            for (int i = 0; i < 4; i++) {
                for (Lado x : Lado.values()) {
                    novo(Graduacao.PRETA);
                    em(e.getKey(), x, x);
                    assertTrue(m.temFinalizacoes(x, Familia.R2), "por cima tem a roda RT");
                    assertFalse(m.temFinalizacoes(x.outro(), Familia.R2), "por baixo nao tem");
                    MotorSolo.Resposta r = m.comando(x, rt(ORDEM[i]));
                    assertEquals(Resultado.INICIADA, r.resultado(), e.getKey() + " " + ORDEM[i] + " " + x + ": " + r.motivo());
                    assertEquals(e.getValue().get(i), m.getAtaque().orElseThrow().finalizacao().tecnicaBase());
                }
            }
        }
    }

    // 6: restricoes de posicao e graduacao
    @Test
    void graduacaoEPosicaoRestringem() {
        novo(Graduacao.BRANCA);
        em(Posicao.MONTADA, Lado.A, Lado.A);
        MotorSolo.Resposta r = m.comando(Lado.A, rt(Direcao.DIREITA));
        assertEquals(Resultado.RECUSADA, r.resultado());
        assertTrue(r.motivo().contains("GRADUAÇÃO MARROM"), r.motivo());
        assertTrue(m.getAtaque().isEmpty());
        novo(Graduacao.PRETA);
        em(Posicao.MONTADA, Lado.A, Lado.A);
        assertEquals(Resultado.RECUSADA, m.comando(Lado.B, rt(Direcao.CIMA)).resultado(), "por baixo o RT nao finaliza");
        novo(Graduacao.PRETA);
        em(Posicao.GUARDA, Lado.A, Lado.B);
        assertFalse(m.temFinalizacoes(Lado.B, Familia.R2), "na guarda nao ha roda RT");
        assertFalse(m.temFinalizacoes(Lado.A, Familia.R2));
        novo(Graduacao.PRETA);
        em(Posicao.MONTADA, Lado.A, Lado.A);
        m.definirEstabilidade(20);
        r = m.comando(Lado.A, rt(Direcao.CIMA));
        assertEquals(Resultado.RECUSADA, r.resultado(), "sem controle a lapela nao entra: " + r.motivo());
    }

    // 7: defesa, escape e rendicao pelo sistema de sempre; falhando, a posicao fica (ou o desfecho previsto)
    @Test
    void defesaEscapeERendicao() {
        novo(Graduacao.PRETA);
        em(Posicao.COSTAS, Lado.A, Lado.A);
        assertEquals(Resultado.INICIADA, m.comando(Lado.A, rt(Direcao.CIMA)).resultado());
        assertEquals(Resultado.CONTRA, m.comando(Lado.B, new ComandoSolo(Direcao.BAIXO, Familia.RS)).resultado(),
                "cortou a entrada");
        assertTrue(m.getAtaque().isEmpty());
        assertEquals(Posicao.COSTAS, m.getPosicao(), "falhou: as costas continuam");

        novo(Graduacao.PRETA);
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        assertEquals(Resultado.INICIADA, m.comando(Lado.A, rt(Direcao.CIMA)).resultado());
        for (int i = 0; i < 200 && m.getAtaque().map(x -> x.fase() == MotorSolo.FaseFinalizacao.ENTRADA).orElse(false); i++) {
            ticks(1);
        }
        assertTrue(m.getAtaque().isPresent(), "a pressao comecou");
        assertEquals(Resultado.CONCLUIDA, m.comando(Lado.B, new ComandoSolo(Direcao.ESQUERDA, Familia.RS)).resultado(),
                "bateu");
        assertTrue(m.getVitoria().isPresent());
        assertEquals("cortador_papel", m.getVitoria().get().finalizacao().tecnicaBase());
    }

    // 13: LT + LB nas costas: quatro opcoes funcionais, sem FINALIZACAO FUTURA
    @Test
    void ltLbDasCostasTemQuatroEntradasQueTerminamNaChave() {
        String[] chaves = {"chave_pe_reta", "chave_joelho", "toe_hold", "panturrilha"};
        for (int i = 0; i < 4; i++) {
            novo(Graduacao.PRETA);
            em(Posicao.COSTAS, Lado.A, Lado.A);
            ComandoSolo c = new ComandoSolo(ORDEM[i], Familia.L1_L2);
            TecnicaSolo t = m.tecnicaPara(Lado.A, c).orElseThrow();
            assertNotEquals(CatalogoTecnicasSolo.ENTRADA_PERNA_DOMINANTE, t.id());
            assertEquals(chaves[i] + "@ashi_garami", m.chaveDaEntrada(t.id()).id());
            assertEquals(NaturezaAcao.FINALIZACAO, ClassificacaoAcoes.de(t), "a roda mostra que e ataque");
            RegistroFinalizacoes.Espaco es = m.getFinalizacoes().para(m.getEstado(), Lado.A, c).orElseThrow();
            assertEquals(t.id(), es.idTecnica(), "o espaco aponta para a entrada real");
            MotorSolo.Resposta r = m.comando(Lado.A, c);
            assertEquals(Resultado.INICIADA, r.resultado(), r.motivo());
            assertTrue(m.getAtaque().isEmpty(), "a chave NAO comeca nas costas");
            for (int k = 0; k < 120 && m.getTentativa().isPresent(); k++) {
                ticks(1);
            }
            assertEquals(Posicao.ASHI_GARAMI, m.getPosicao(), "transicao real para o controle de perna");
            assertTrue(m.getAtaque().isEmpty(), "o corpo chega antes da chave");
            assertTrue(m.getChaveEncadeada().isPresent());
            ticks(MotorSolo.TICKS_ATE_A_CHAVE + 1);
            assertEquals(chaves[i], m.getAtaque().orElseThrow().finalizacao().tecnicaBase(), "chegou: a chave comeca");
        }
    }

    @Test
    void entradaDasCostasRespeitaGraduacaoEOOutroPodeEscapar() {
        novo(Graduacao.ROXA);
        em(Posicao.COSTAS, Lado.A, Lado.A);
        ComandoSolo joelho = new ComandoSolo(Direcao.DIREITA, Familia.L1_L2);
        MotorSolo.Resposta r = m.comando(Lado.A, joelho);
        assertEquals(Resultado.RECUSADA, r.resultado());
        assertTrue(r.motivo().contains("GRADUAÇÃO MARROM"), "a chave de joelho pede marrom: " + r.motivo());
        assertEquals(Posicao.COSTAS, m.getPosicao());
        assertEquals(Resultado.INICIADA, m.comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L1_L2)).resultado(),
                "a pe reta (roxa) pode");
        // o outro gira na janela: a entrada nao acontece e nenhuma chave comeca
        assertEquals(Resultado.CONTRA, m.comando(Lado.B, new ComandoSolo(Direcao.ESQUERDA, Familia.RS)).resultado());
        ticks(MotorSolo.TICKS_ATE_A_CHAVE + 5);
        assertTrue(m.getAtaque().isEmpty());
        assertTrue(m.getChaveEncadeada().isEmpty());
        assertNotEquals(Posicao.ASHI_GARAMI, m.getPosicao());
    }

    // 14: o RB <- (entrar no controle de perna, sem chave) continua igual
    @Test
    void entradaSimplesNaPernaPreservada() {
        for (Posicao p : List.of(Posicao.MONTADA, Posicao.CONTROLE_LATERAL, Posicao.COSTAS)) {
            novo(Graduacao.PRETA);
            em(p, Lado.A, Lado.A);
            TecnicaSolo t = m.tecnicaPara(Lado.A, new ComandoSolo(Direcao.ESQUERDA, Familia.R1)).orElseThrow();
            assertEquals(CatalogoTecnicasSolo.ENTRADA_PERNA_DOMINANTE, t.id(), p.name());
            assertEquals(NaturezaAcao.TRANSICAO, ClassificacaoAcoes.de(t), "entrar na perna nao e finalizar");
            assertEquals(null, m.chaveDaEntrada(t.id()));
        }
        novo(Graduacao.PRETA);
        em(Posicao.COSTAS, Lado.A, Lado.A);
        m.comando(Lado.A, new ComandoSolo(Direcao.ESQUERDA, Familia.R1));
        for (int k = 0; k < 120 && m.getTentativa().isPresent(); k++) {
            ticks(1);
        }
        ticks(MotorSolo.TICKS_ATE_A_CHAVE + 5);
        assertEquals(Posicao.ASHI_GARAMI, m.getPosicao());
        assertTrue(m.getAtaque().isEmpty(), "o RB <- so entra no controle de perna");
    }
}
