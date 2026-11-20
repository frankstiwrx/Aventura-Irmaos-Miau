package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.AnimacoesTecnicas;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.Resultado;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;
import com.irmaosmiau.regras.ModoRegras;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.6: novas finalizacoes de perna no LB e no LT do ashi garami (o L1+L2
 * preservado), alvo/defesa por articulacao, graduacao e modo; o toe hold com o nome
 * certo; e as duas variacoes do berimbolo na De La Riva (o berimbolo e o ashi mantidos).
 */
class Pernas5Q6Test {

    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;

    private void novo(Graduacao g) {
        a = new IrmaoMiauBranco(300, 150, 0.7);
        b = new IrmaoMiauPreto(380, 150, 0.7);
        a.setViradoParaDireita(true);
        b.setViradoParaDireita(false);
        a.setGraduacao(g);
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

    private void ashi() {
        m.forcarPosicao(Posicao.ASHI_GARAMI, Lado.B, Lado.A);
        ticks(30);
        m.definirEstabilidade(90);
    }

    private void ateDisputa() {
        for (int i = 0; i < 400 && m.getAtaque().orElseThrow().fase() != MotorSolo.FaseFinalizacao.DISPUTA; i++) {
            ticks(1);
        }
    }

    private MotorSolo.Resposta cmd(Lado l, Direcao d, Familia f) {
        return m.comando(l, new ComandoSolo(d, f));
    }

    private static final Map<String, ComandoSolo> NOVAS = Map.of(
            "aoki_lock", new ComandoSolo(Direcao.CIMA, Familia.L2),
            "estima_lock", new ComandoSolo(Direcao.DIREITA, Familia.L2),
            "pe_reta_5050", new ComandoSolo(Direcao.BAIXO, Familia.L2),
            "joelho_invertido", new ComandoSolo(Direcao.CIMA, Familia.L1),
            "banana_split", new ComandoSolo(Direcao.DIREITA, Familia.L1),
            "kneebar_giro", new ComandoSolo(Direcao.BAIXO, Familia.L1));

    @Test
    void lbELtDoAshiTemAsNovasEOL1L2ContinuaIgual() {
        novo(Graduacao.PRETA);
        ashi();
        for (Map.Entry<String, ComandoSolo> e : NOVAS.entrySet()) {
            Finalizacao f = m.getCatalogoFinalizacoes().para(m.getEstado(), Lado.A, e.getValue()).orElseThrow();
            assertEquals(e.getKey(), f.tecnicaBase());
            assertEquals(CategoriaFinalizacao.PERNA, f.categoria());
        }
        String[] l12 = {"chave_pe_reta", "chave_joelho", "toe_hold", "panturrilha"};
        Direcao[] ds = {Direcao.CIMA, Direcao.DIREITA, Direcao.BAIXO, Direcao.ESQUERDA};
        for (int i = 0; i < 4; i++) {
            assertEquals(l12[i], m.getCatalogoFinalizacoes().para(m.getEstado(), Lado.A,
                    new ComandoSolo(ds[i], Familia.L1_L2)).orElseThrow().tecnicaBase(), "L1+L2 preservado");
        }
        Finalizacao toe = m.getCatalogoFinalizacoes().buscar("toe_hold@ashi_garami").orElseThrow();
        assertEquals("Toe hold (mata-leão no pé)", toe.nome(), "o nome certo (mao de vaca e chave de punho)");
        assertTrue(m.getCatalogoFinalizacoes().todas().stream().noneMatch(f -> f.tecnicaBase().contains("heel")),
                "sem heel hook (regra do jogo)");
    }

    @Test
    void cadaNovaEntraTemDefesaPeloAlvoEBaterEncerra() {
        for (Map.Entry<String, ComandoSolo> e : NOVAS.entrySet()) {
            novo(Graduacao.PRETA);
            ashi();
            MotorSolo.Resposta r = m.comando(Lado.A, e.getValue());
            assertEquals(Resultado.INICIADA, r.resultado(), e.getKey() + ": " + r.motivo());
            Finalizacao f = m.getAtaque().orElseThrow().finalizacao();
            ateDisputa();
            MotorSolo.Resposta d = cmd(Lado.B, Direcao.BAIXO, Familia.RS);
            assertEquals(f.alvo().getDefesa(), d.motivo(), e.getKey() + ": defesa pelo alvo");
            assertEquals("BATEU", cmd(Lado.B, Direcao.ESQUERDA, Familia.RS).motivo());
            assertEquals(Lado.A, m.getVitoria().orElseThrow().vencedor());
        }
    }

    @Test
    void alvosDiferenciados() {
        CatalogoFinalizacoes c = CatalogoFinalizacoes.referencia();
        Set<AlvoFinalizacao> pernas = c.todas().stream().filter(f -> f.categoria() == CategoriaFinalizacao.PERNA)
                .map(Finalizacao::alvo).collect(Collectors.toSet());
        assertTrue(pernas.containsAll(Set.of(AlvoFinalizacao.TORNOZELO, AlvoFinalizacao.PE, AlvoFinalizacao.JOELHO,
                AlvoFinalizacao.PANTURRILHA, AlvoFinalizacao.QUADRIL)), pernas.toString());
        assertEquals(AlvoFinalizacao.PUNHO, c.buscar("chave_punho@guarda").orElseThrow().alvo());
        assertEquals(AlvoFinalizacao.OMBRO, c.buscar("kimura@guarda").orElseThrow().alvo());
        assertEquals(AlvoFinalizacao.BRACO, c.buscar("armlock@guarda").orElseThrow().alvo());
        assertEquals("PROTEGER BRAÇO", c.buscar("armlock@guarda").orElseThrow().defesa(), "o rotulo antigo continua");
    }

    @Test
    void graduacaoEModoRestringem() {
        novo(Graduacao.AZUL);
        ashi();
        MotorSolo.Resposta r = cmd(Lado.A, Direcao.DIREITA, Familia.L2);         // estima (marrom)
        assertTrue(r.motivo().contains("GRADUAÇÃO"), r.motivo());
        novo(Graduacao.PRETA);
        m.setModoRegras(ModoRegras.CAMPEONATO_PROVISORIO);
        ashi();
        assertTrue(cmd(Lado.A, Direcao.DIREITA, Familia.L2).motivo().endsWith("PROIBIDA NO MODO"));
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.BAIXO, Familia.L2).resultado(),
                "a pe reta da 50/50 e a mesma chave reta");
    }

    @Test
    void kneebarComGiroQueFalhaEntregaAPosicao() {
        novo(Graduacao.PRETA);
        ashi();
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.BAIXO, Familia.L1).resultado());
        ateDisputa();
        for (int i = 0; i < 20 && m.getAtaque().isPresent(); i++) {
            cmd(Lado.B, Direcao.BAIXO, Familia.RS);
            ticks(MotorSolo.RECARGA_ACAO_FINALIZACAO + 1);
        }
        assertTrue(m.getAtaque().isEmpty() && m.getVitoria().isEmpty(), "defendida");
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao(), "o giro errado deixa o outro por cima");
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow());
    }

    // ---------------- berimbolo
    private void dlr() {
        m.forcarPosicao(Posicao.DE_LA_RIVA, Lado.B, Lado.A);
        ticks(30);
        m.definirEstabilidade(90);
    }

    private void resolver() {
        for (int i = 0; i < 200 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        ticks(2);
    }

    @Test
    void tresBerimbolosEOAshiNaDeLaRiva() {
        novo(Graduacao.PRETA);
        dlr();
        assertEquals(CatalogoTecnicasSolo.BERIMBOLO, m.tecnicaPara(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L1_L2))
                .orElseThrow().id());
        assertEquals(CatalogoTecnicasSolo.ENTRADA_PERNA_ABERTA + "_dlr", m.tecnicaPara(Lado.A,
                new ComandoSolo(Direcao.BAIXO, Familia.L1_L2)).orElseThrow().id(), "o ashi continua no baixo");
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.ESQUERDA, Familia.L1_L2).resultado());
        assertEquals("berimbolo_kiss", m.efeitoVisual(Lado.A).orElseThrow().tecnica());
        resolver();
        assertEquals(Posicao.COSTAS, m.getPosicao());

        novo(Graduacao.PRETA);
        dlr();
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.DIREITA, Familia.L1_L2).resultado());
        assertEquals("berimbolo_invertido", m.efeitoVisual(Lado.A).orElseThrow().tecnica());
        resolver();
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao(), "o baby bolo raspa para cima");
        assertEquals(Lado.A, m.getEstado().getPorCima().orElseThrow());

        novo(Graduacao.PRETA);
        dlr();
        m.definirEstabilidade(40);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.ESQUERDA, Familia.L1_L2).resultado());
        resolver();
        assertNotEquals(Posicao.COSTAS, m.getPosicao(), "o kiss exige mais controle que o berimbolo");
    }

    @Test
    void variacoesTemAnimacoesDiferentes() {
        for (String id : new String[] {"berimbolo", "berimbolo_kiss", "berimbolo_invertido"}) {
            assertTrue(AnimacoesTecnicas.tem(id + ":ATACA") && AnimacoesTecnicas.tem(id + ":RECEBE"), id);
        }
        assertNotEquals(AnimacoesTecnicas.amostraParaTeste("berimbolo:ATACA", 0.55)[10],
                AnimacoesTecnicas.amostraParaTeste("berimbolo_kiss:ATACA", 0.55)[10]);
        assertNotEquals(AnimacoesTecnicas.amostraParaTeste("berimbolo:ATACA", 1.0)[30],
                AnimacoesTecnicas.amostraParaTeste("berimbolo_invertido:ATACA", 1.0)[30]);
    }
}
