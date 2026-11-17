package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.animacao.AnimadorSolo;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.MotorSolo.FaseFinalizacao;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.Resultado;
import com.irmaosmiau.posicional.MotorSolo.TipoEfeitoVisual;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;
import com.irmaosmiau.regras.ModoRegras;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q no MotorSolo isolado: ciclo de finalizacao (entrada, disputa,
 * defesa, escape, rendicao, vitoria), graduacao/aprendizado/modo, requisitos,
 * controle de perna, guardas aranha e single-leg X, berimbolo preservado e os
 * efeitos visuais sincronizados dos dois lutadores.
 */
class MotorSolo5QTest {

    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;

    private void novo() {
        a = new IrmaoMiauBranco(300, 150, 0.7);
        b = new IrmaoMiauPreto(380, 150, 0.7);
        a.setViradoParaDireita(true);
        b.setViradoParaDireita(false);
        m = new MotorSolo(a, b, null, 15, 715);
    }

    private IrmaoMiau l(Lado x) {
        return x == Lado.A ? a : b;
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
    }

    private MotorSolo.Resposta cmd(Lado l, Direcao d, Familia f) {
        return m.comando(l, new ComandoSolo(d, f));
    }

    private void ateDisputa() {
        for (int i = 0; i < 60 && m.getAtaque().map(x -> x.fase() == FaseFinalizacao.ENTRADA).orElse(false); i++) {
            ticks(1);
        }
        assertEquals(FaseFinalizacao.DISPUTA, m.getAtaque().orElseThrow().fase());
    }

    private void resolverTentativa() {
        for (int i = 0; i < 150 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        ticks(1);
    }

    // ================= ciclo basico, nos dois sentidos
    @Test
    void armlockDaGuardaFinalizaSemDefesaNosDoisSentidos() {
        for (Lado x : Lado.values()) {
            novo();
            em(Posicao.GUARDA, x.outro(), x);                 // x por baixo, com a guarda fechada
            double f = l(x).getFolegoAtual();
            assertEquals(Resultado.INICIADA, cmd(x, Direcao.CIMA, Familia.L1).resultado());
            assertTrue(l(x).getFolegoAtual() < f, "custa");
            assertEquals(FaseFinalizacao.ENTRADA, m.getAtaque().orElseThrow().fase());
            assertEquals(TecnicaSolo.Exposicao.BASE, m.exposicaoDe(x).orElseThrow(), "a entrada expoe");
            ateDisputa();
            assertEquals(MotorSolo.PRESSAO_INICIAL, m.getAtaque().orElseThrow().pressao(), 1.0);
            for (int i = 0; i < 600 && m.getVitoria().isEmpty(); i++) {
                ticks(1);
            }
            MotorSolo.Vitoria v = m.getVitoria().orElseThrow();
            assertEquals(x, v.vencedor());
            assertEquals("armlock", v.finalizacao().tecnicaBase());
            assertFalse(v.rendicao(), "finalizacao consolidada");
            assertTrue(m.getAtaque().isEmpty(), "o controle foi solto");
            assertEquals("LUTA ENCERRADA", cmd(x.outro(), Direcao.CIMA, Familia.RS).motivo());
            m.reiniciar();
            assertTrue(m.getVitoria().isEmpty() && m.getAtaque().isEmpty(), "reiniciar limpa tudo");
        }
    }

    // ================= graduacao, aprendizado, modo, requisitos e posicao
    @Test
    void graduacaoAprendizadoModoEPosicaoBloqueiamSemCusto() {
        novo();
        em(Posicao.GUARDA, Lado.B, Lado.A);
        a.setGraduacao(Graduacao.BRANCA);
        double f = a.getFolegoAtual();
        MotorSolo.Resposta r = cmd(Lado.A, Direcao.BAIXO, Familia.L1);       // omoplata (azul)
        assertEquals(Resultado.RECUSADA, r.resultado());
        assertTrue(r.motivo().contains("GRADUAÇÃO AZUL"), r.motivo());
        assertEquals(f, a.getFolegoAtual(), 1e-9, "recusa nao cobra");
        assertTrue(m.getAtaque().isEmpty());
        a.esquecerTecnica("armlock");
        assertTrue(cmd(Lado.A, Direcao.CIMA, Familia.L1).motivo().contains("NÃO APRENDIDA"));
        a.aprenderTecnica("armlock");
        // a faixa desenhada nao entra na conta: so a graduacao logica
        a.setGraduacao(Graduacao.PRETA);
        // postura do outro bloqueia o armlock (braco nao isolado)
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.B, Direcao.BAIXO, Familia.RS).resultado(), "postura");
        assertTrue(cmd(Lado.A, Direcao.CIMA, Familia.L1).motivo().contains("O OUTRO EM POSTURA"));
        // quem esta por cima na guarda fechada nao tem as finalizacoes de quem controla
        assertEquals("NADA NESTA POSIÇÃO", cmd(Lado.B, Direcao.CIMA, Familia.L2).motivo());

        novo();
        em(Posicao.ASHI_GARAMI, Lado.B, Lado.A);
        m.setModoRegras(ModoRegras.CAMPEONATO_PROVISORIO);
        assertTrue(cmd(Lado.A, Direcao.DIREITA, Familia.L1_L2).motivo().contains("PROIBIDA NO MODO"), "chave de joelho");
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.L1_L2).resultado(), "pe reta liberada");
    }

    // ================= defesa: na entrada, na disputa e a perda de posicao
    @Test
    void defesaCortaAEntradaOuZeraAPressaoEPodeCustarAPosicao() {
        novo();
        em(Posicao.GUARDA, Lado.B, Lado.A);
        cmd(Lado.A, Direcao.CIMA, Familia.L1);
        assertEquals(Resultado.CONTRA, cmd(Lado.B, Direcao.BAIXO, Familia.RS).resultado(), "cortou a entrada");
        assertTrue(m.getAtaque().isEmpty() && m.getVitoria().isEmpty());
        assertTrue(m.recargaDe(Lado.A) > MotorSolo.RECARGA_TICKS, "quem atacou paga a falha");

        // montada -> armlock defendido na disputa: quem atacou perde a montada (o outro fica por cima)
        novo();
        em(Posicao.MONTADA, Lado.A, Lado.A);
        m.definirEstabilidade(80);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.L1).resultado());
        ateDisputa();
        for (int i = 0; i < 40 && m.getAtaque().isPresent(); i++) {
            cmd(Lado.B, Direcao.BAIXO, Familia.RS);
            ticks(MotorSolo.RECARGA_ACAO_FINALIZACAO);
        }
        assertTrue(m.getAtaque().isEmpty() && m.getVitoria().isEmpty(), "defendido");
        assertEquals(Posicao.GUARDA, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow(), "perda de posicao: B por cima");
        assertEquals(Lado.A, m.getEstado().getControlador().orElseThrow(), "A com a guarda");
    }

    // ================= sequencia: 100 kg -> montada -> americana -> escape -> meia-guarda
    @Test
    void cemQuilosMontadaAmericanaEscapeMeiaGuarda() {
        novo();
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        cmd(Lado.A, Direcao.CIMA, Familia.RS);                // montar
        resolverTentativa();
        assertEquals(Posicao.MONTADA, m.getPosicao());
        ticks(MotorSolo.RECARGA_TICKS);
        m.definirEstabilidade(60);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.DIREITA, Familia.L1).resultado(), "americana");
        ateDisputa();
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.B, Direcao.BAIXO, Familia.RS).resultado(), "defende primeiro");
        assertTrue(m.getAtaque().orElseThrow().pressao() <= MotorSolo.LIMIAR_ESCAPE_FINALIZACAO);
        ticks(MotorSolo.RECARGA_ACAO_FINALIZACAO);
        assertEquals(Resultado.CONTRA, cmd(Lado.B, Direcao.CIMA, Familia.R1).resultado(), "escapou");
        assertEquals(Posicao.MEIA_GUARDA, m.getPosicao());
        assertEquals(Lado.A, m.getEstado().getPorCima().orElseThrow());
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow(), "B na meia-guarda");
    }

    @Test
    void escapeComPressaoAltaFalhaEApertarSobe() {
        novo();
        em(Posicao.COSTAS, Lado.A, Lado.A);
        m.definirEstabilidade(90);
        cmd(Lado.A, Direcao.CIMA, Familia.L2);                // mata-leao
        ateDisputa();
        double p0 = m.getAtaque().orElseThrow().pressao();
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.A, Direcao.CIMA, Familia.RS).resultado(), "apertar");
        assertTrue(m.getAtaque().orElseThrow().pressao() > p0 + 10);
        ticks(MotorSolo.RECARGA_ACAO_FINALIZACAO);
        cmd(Lado.A, Direcao.CIMA, Familia.L2);                // apertar tambem pelo mesmo comando
        double alta = m.getAtaque().orElseThrow().pressao();
        assertTrue(alta > MotorSolo.LIMIAR_ESCAPE_FINALIZACAO);
        MotorSolo.Resposta r = cmd(Lado.B, Direcao.CIMA, Familia.R1);
        assertEquals(Resultado.RECUSADA, r.resultado());
        assertEquals("SEM ESPAÇO PARA ESCAPAR", r.motivo());
        assertTrue(m.getAtaque().orElseThrow().pressao() > alta, "escape errado piora");
        assertEquals("SOLTOU", cmd(Lado.A, Direcao.BAIXO, Familia.RS).motivo(), "quem ataca pode soltar");
        assertEquals(Posicao.COSTAS, m.getPosicao(), "soltar mantem a posicao");
    }

    // ================= rendicao: prioridade e liberacao imediata
    @Test
    void rendicaoTemPrioridadeESoltaNaHora() {
        novo();
        em(Posicao.COSTAS, Lado.B, Lado.B);
        m.definirEstabilidade(60);
        cmd(Lado.B, Direcao.CIMA, Familia.L2);
        ateDisputa();
        Map<Lado, ComandoSolo> mesmoTick = new EnumMap<>(Lado.class);
        mesmoTick.put(Lado.B, new ComandoSolo(Direcao.CIMA, Familia.RS));      // apertar
        mesmoTick.put(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.R1));     // bater
        Map<Lado, MotorSolo.Resposta> r = m.comandos(mesmoTick);
        assertEquals("BATEU", r.get(Lado.A).motivo());
        MotorSolo.Vitoria v = m.getVitoria().orElseThrow();
        assertTrue(v.rendicao());
        assertEquals(Lado.B, v.vencedor());
        assertTrue(m.getAtaque().isEmpty(), "liberou o controle na hora");
        assertTrue(m.consumirEventosLuta().stream().anyMatch(e -> e.tipo() == EventoLuta.FINALIZACAO));
        assertEquals(TipoEfeitoVisual.BATENDO, m.efeitoVisual(Lado.A).orElseThrow().tipo());
    }

    // ================= De La Riva -> berimbolo -> costas -> mata-leao -> defesa -> rendicao
    @Test
    void deLaRivaBerimboloCostasMataLeaoDefesaRendicao() {
        novo();
        em(Posicao.DE_LA_RIVA, Lado.B, Lado.A);
        m.definirEstabilidade(70);
        assertEquals(CatalogoTecnicasSolo.BERIMBOLO, m.getCatalogo().para(m.getEstado(), Lado.A,
                new ComandoSolo(Direcao.CIMA, Familia.L1_L2)).orElseThrow().id(), "berimbolo preservado");
        cmd(Lado.A, Direcao.CIMA, Familia.L1_L2);
        resolverTentativa();
        assertEquals(Posicao.COSTAS, m.getPosicao());
        ticks(MotorSolo.RECARGA_TICKS);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.L2).resultado(), "mata-leao");
        ateDisputa();
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.B, Direcao.BAIXO, Familia.RS).resultado(), "maos no pescoco");
        ticks(MotorSolo.RECARGA_ACAO_FINALIZACAO);
        cmd(Lado.B, Direcao.BAIXO, Familia.R1);               // bate
        assertTrue(m.getVitoria().orElseThrow().rendicao());
        assertEquals("mata_leao", m.getVitoria().orElseThrow().finalizacao().tecnicaBase());
    }

    // ================= guarda aberta -> perna -> chave de pe -> defesa -> volta a luta
    @Test
    void guardaAbertaControleDePernaChaveDePeDefesaERetorno() {
        novo();
        em(Posicao.GUARDA_ABERTA, Lado.B, Lado.A);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.L1_L2).resultado(), "ashi garami");
        resolverTentativa();
        assertEquals(Posicao.ASHI_GARAMI, m.getPosicao());
        assertEquals(Lado.A, m.getEstado().getControlador().orElseThrow());
        ticks(MotorSolo.RECARGA_TICKS);
        m.definirEstabilidade(60);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.L1_L2).resultado(), "chave de pe reta");
        assertEquals(CategoriaFinalizacao.PERNA, m.getAtaque().orElseThrow().finalizacao().categoria());
        ateDisputa();
        for (int i = 0; i < 40 && m.getAtaque().isPresent(); i++) {
            cmd(Lado.B, Direcao.BAIXO, Familia.RS);           // girar o pe
            ticks(MotorSolo.RECARGA_ACAO_FINALIZACAO);
        }
        assertTrue(m.getAtaque().isEmpty() && m.getVitoria().isEmpty(), "defendida");
        m.definirEstabilidade(20);
        ticks(1);
        cmd(Lado.B, Direcao.DIREITA, Familia.RS);             // girar e livrar a perna
        resolverTentativa();
        assertEquals(Posicao.SOLO_DISPUTADO, m.getPosicao(), "livrou a perna");
        ticks(30);
        cmd(Lado.B, Direcao.CIMA, Familia.R1);                // levantar
        resolverTentativa();
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao(), "de volta a luta");
    }

    @Test
    void entrarNaPernaSaindoDaMontadaEArriscado() {
        novo();
        em(Posicao.MONTADA, Lado.A, Lado.A);
        m.definirEstabilidade(20);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.L1_L2).resultado());
        assertTrue(m.getAtaque().isEmpty(), "nao e uma chave instantanea: e uma transicao");
        resolverTentativa();
        assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao(), "falhou: perdeu a montada");
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow(), "o outro recuperou a guarda");

        novo();
        em(Posicao.MONTADA, Lado.A, Lado.A);
        m.definirEstabilidade(80);
        cmd(Lado.A, Direcao.CIMA, Familia.L1_L2);
        resolverTentativa();
        assertEquals(Posicao.ASHI_GARAMI, m.getPosicao());
    }

    // ================= guardas: aranha, laco, single-leg X
    @Test
    void aranhaExigeMangaETrocaComOLaco() {
        novo();
        em(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        assertEquals("SEM A MANGA", cmd(Lado.B, Direcao.ESQUERDA, Familia.L2).motivo());
        cmd(Lado.B, Direcao.BAIXO, Familia.L2);               // pegar a manga
        ticks(MotorSolo.RECARGA_TICKS);
        assertEquals(Resultado.INICIADA, cmd(Lado.B, Direcao.ESQUERDA, Familia.L2).resultado());
        resolverTentativa();
        assertEquals(Posicao.GUARDA_ARANHA, m.getPosicao());
        ticks(MotorSolo.RECARGA_TICKS);
        cmd(Lado.B, Direcao.BAIXO, Familia.L1);               // aranha -> laco
        resolverTentativa();
        assertEquals(Posicao.GUARDA_LACO, m.getPosicao());
        ticks(MotorSolo.RECARGA_TICKS);
        cmd(Lado.B, Direcao.ESQUERDA, Familia.L2);            // laco -> aranha
        resolverTentativa();
        assertEquals(Posicao.GUARDA_ARANHA, m.getPosicao());
        ticks(MotorSolo.RECARGA_TICKS);
        assertEquals(Resultado.INICIADA, cmd(Lado.B, Direcao.DIREITA, Familia.L2).resultado(), "triangulo da aranha");
        cmd(Lado.B, Direcao.BAIXO, Familia.RS);               // soltar (quem ataca)
        ticks(MotorSolo.RECARGA_TICKS);
        cmd(Lado.A, Direcao.DIREITA, Familia.RS);             // livrar as mangas
        resolverTentativa();
        assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao());
        assertFalse(m.temConexao(Lado.B, MotorSolo.Conexao.MANGA), "mangas livres");
    }

    @Test
    void singleLegXContraQuemEstaEmPe() {
        novo();
        em(Posicao.GUARDA_BORBOLETA, Lado.A, Lado.B);
        cmd(Lado.B, Direcao.DIREITA, Familia.L2);
        resolverTentativa();
        assertEquals(Posicao.SINGLE_LEG_X, m.getPosicao());
        ticks(30);
        assertTrue(!a.isDeitado() && !a.isAjoelhado(), "quem esta por cima fica em pe");
        cmd(Lado.B, Direcao.CIMA, Familia.RS);                // raspagem
        resolverTentativa();
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow(), "quem raspou ficou em pe");
        assertTrue(a.isDeitado());
    }

    // ================= cansaco: defesa mais fraca, mas possivel
    @Test
    void defensorCansadoDefendeMenos() {
        double[] reducao = new double[2];
        for (int i = 0; i < 2; i++) {
            novo();
            em(Posicao.COSTAS, Lado.A, Lado.A);
            m.definirEstabilidade(60);
            if (i == 1) {
                b.gastarFolegoEsforco(b.getFolegoAtual() - 1, "teste");
                b.gastarCondicionamentoEsforco(b.getCondicionamentoAtual() * 0.9, "teste");
            }
            cmd(Lado.A, Direcao.CIMA, Familia.L2);
            ateDisputa();
            double p = m.getAtaque().orElseThrow().pressao();
            assertEquals(Resultado.CONCLUIDA, cmd(Lado.B, Direcao.BAIXO, Familia.RS).resultado(), "sempre pode defender");
            reducao[i] = p - m.getAtaque().orElseThrow().pressao();
        }
        assertTrue(reducao[1] > 0 && reducao[1] < reducao[0], "cansado: " + reducao[1] + " < " + reducao[0]);
    }

    // ================= animacoes: os dois sincronizados; empurrar x ser empurrado; afastar e levantar
    @Test
    void efeitosVisuaisSincronizadosEDistintos() {
        novo();
        em(Posicao.MONTADA, Lado.A, Lado.A);
        m.definirEstabilidade(70);
        cmd(Lado.A, Direcao.CIMA, Familia.L2);
        assertEquals(TipoEfeitoVisual.FINALIZANDO, m.efeitoVisual(Lado.A).orElseThrow().tipo());
        assertEquals(TipoEfeitoVisual.DEFENDENDO_FINALIZACAO, m.efeitoVisual(Lado.B).orElseThrow().tipo());
        assertEquals(CategoriaFinalizacao.ESTRANGULAMENTO, m.efeitoVisual(Lado.B).orElseThrow().categoria());

        novo();
        m.comando(Lado.A, new ComandoSolo(Direcao.CIMA, true));      // empurrao
        assertEquals(TipoEfeitoVisual.EMPURRANDO, m.efeitoVisual(Lado.A).orElseThrow().tipo());
        resolverTentativa();
        assertEquals(TipoEfeitoVisual.EMPURRADO, m.efeitoVisual(Lado.B).orElseThrow().tipo(), "reacao propria");

        novo();
        em(Posicao.GUARDA, Lado.B, Lado.A);
        cmd(Lado.A, Direcao.BAIXO, Familia.R1);               // abrir e afastar
        assertEquals(TipoEfeitoVisual.AFASTANDO, m.efeitoVisual(Lado.A).orElseThrow().tipo());
        resolverTentativa();
        ticks(20);
        cmd(Lado.A, Direcao.CIMA, Familia.RS);                // levantar
        assertEquals(TipoEfeitoVisual.LEVANTANDO, m.efeitoVisual(Lado.A).orElseThrow().tipo());

        // as poses provisorias sao diferentes para cada efeito
        int[] base = new int[30];
        int[] empurra = base.clone();
        int[] empurrado = base.clone();
        AnimadorSolo.aplicarEfeito(empurra, new MotorSolo.EfeitoVisual(TipoEfeitoVisual.EMPURRANDO, 0.5, null));
        AnimadorSolo.aplicarEfeito(empurrado, new MotorSolo.EfeitoVisual(TipoEfeitoVisual.EMPURRADO, 0.5, null));
        assertTrue(empurra[9] > 0 && empurrado[9] < 0, "um inclina para frente, o outro para tras");
        assertNotEquals(Arrays.toString(empurra), Arrays.toString(base));
    }

    @Test
    void catalogoTemAsFamiliasCompletasNasQuatroPosicoes() {
        CatalogoFinalizacoes c = CatalogoFinalizacoes.referencia();
        for (Posicao p : RegistroFinalizacoes.POSICOES.keySet()) {
            for (Direcao d : Direcao.values()) {
                assertEquals(CategoriaFinalizacao.BRACO, c.naPosicao(p, Familia.L1, d).orElseThrow().categoria(), p + " L1 " + d);
                assertEquals(CategoriaFinalizacao.ESTRANGULAMENTO, c.naPosicao(p, Familia.L2, d).orElseThrow().categoria());
            }
        }
        for (Direcao d : Direcao.values()) {
            assertEquals(CategoriaFinalizacao.PERNA, c.naPosicao(Posicao.ASHI_GARAMI, Familia.L1_L2, d).orElseThrow().categoria());
        }
        assertTrue(c.todas().stream().noneMatch(f -> f.tecnicaBase().contains("heel")), "sem heel hook");
        assertTrue(c.todas().stream().filter(f -> f.categoria() == CategoriaFinalizacao.PERNA)
                .allMatch(f -> f.graduacao().ordinal() >= Graduacao.ROXA.ordinal()), "pernas: roxa ou mais");
    }
}
