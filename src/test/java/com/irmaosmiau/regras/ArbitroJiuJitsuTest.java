package com.irmaosmiau.regras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.Resultado;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import org.junit.jupiter.api.Test;

/**
 * Arbitragem do modo PONTOS + FINALIZACAO com o MOTOR DE VERDADE: as posicoes mudam por tecnicas reais
 * (comandos do MotorSolo); o arbitro so observa, um tick de luta por vez (16 ms, como no GamePanel).
 */
class ArbitroJiuJitsuTest {

    private static final CatalogoTecnicasSolo CAT = CatalogoTecnicasSolo.referencia();
    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;
    private ArbitroJiuJitsu arb;

    private void novo() {
        a = new IrmaoMiauBranco(300, 150, 0.7);
        b = new IrmaoMiauPreto(380, 150, 0.7);
        a.setViradoParaDireita(true);
        b.setViradoParaDireita(false);
        a.setGraduacao(Graduacao.PRETA);
        b.setGraduacao(Graduacao.PRETA);
        a.setRecursosInfinitos(true, true);
        b.setRecursosInfinitos(true, true);
        m = new MotorSolo(a, b, null, 15, 715);
        arb = new ArbitroJiuJitsu(16);
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            m.informarIntencoes(Intencao.NENHUMA, Intencao.NENHUMA);
            m.atualizar(Intencao.NENHUMA, Intencao.NENHUMA);
            a.atualizar(false, false);
            b.atualizar(false, false);
            arb.observar(m);
        }
    }

    /** Comeca numa posicao (referencia do arbitro: a posicao inicial nao pontua). */
    private void em(Posicao p, Lado cima, Lado ctrl) {
        m.forcarPosicao(p, cima, ctrl);
        ticks(40);
    }

    /** Executa uma tecnica real ate ela se resolver. */
    private void executar(Lado l, String id, double estabilidade) {
        TecnicaSolo t = CAT.exigir(id);
        m.definirEstabilidade(estabilidade);
        MotorSolo.Resposta r = m.comando(l, t.comandos().get(0));
        assertTrue(r.resultado() == Resultado.INICIADA || r.resultado() == Resultado.CONCLUIDA, id + ": " + r.motivo());
        for (int i = 0; i < 200 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        ticks(1);
    }

    private int estab() {
        return arb.getTicksEstabilizacao();
    }

    // 5 + 7 (antes de 3 s nada) + 8 (sem pontos infinitos)
    @Test
    void montadaValeQuatroSoDepoisDeTresSegundosEUmaVez() {
        novo();
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        assertEquals(0, arb.getPlacar().pontos(Lado.A), "a posicao inicial nao pontua");
        executar(Lado.A, CatalogoTecnicasSolo.MONTAR, 95);
        assertEquals(Posicao.MONTADA, m.getPosicao());
        assertEquals(188, estab(), "3 s em ticks de 16 ms");
        ticks(estab() - 10);
        assertEquals(0, arb.getPlacar().pontos(Lado.A), "antes de estabilizar, nada");
        ticks(20);
        assertEquals(4, arb.getPlacar().pontos(Lado.A));
        assertEquals("MONTADA +4", arb.consumirAvisos().get(0).texto());
        ticks(2000);
        assertEquals(4, arb.getPlacar().pontos(Lado.A), "ficar montado nao gera pontos de novo");
        assertTrue(arb.consumirAvisos().isEmpty());
    }

    // 4: passagem de guarda (guarda aberta -> cem quilos) = 3
    @Test
    void passagemValeTres() {
        novo();
        em(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        executar(Lado.A, CatalogoTecnicasSolo.PASSAR_GUARDA, 10);
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao());
        ticks(estab() + 5);
        assertEquals(3, arb.getPlacar().pontos(Lado.A));
        assertEquals(0, arb.getPlacar().pontos(Lado.B));
    }

    // 3.4: passagem seguida de montada soma 7, contando so os 3 s do fim
    @Test
    void passagemSeguidaDeMontadaSomaSete() {
        novo();
        em(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        executar(Lado.A, CatalogoTecnicasSolo.PASSAR_GUARDA, 10);
        ticks(30);
        executar(Lado.A, CatalogoTecnicasSolo.MONTAR, 95);
        assertEquals(Posicao.MONTADA, m.getPosicao());
        assertEquals(0, arb.getPlacar().pontos(Lado.A), "a contagem recomeca no fim da sequencia");
        ticks(estab() + 5);
        assertEquals(7, arb.getPlacar().pontos(Lado.A));
    }

    // 2: raspagem (quem estava por baixo na guarda termina por cima) = 2
    @Test
    void raspagemValeDois() {
        novo();
        em(Posicao.GUARDA, Lado.A, Lado.B);
        executar(Lado.B, CatalogoTecnicasSolo.RASPAGEM, 10);
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow());
        ticks(estab() + 5);
        assertEquals(2, arb.getPlacar().pontos(Lado.B));
        assertEquals(0, arb.getPlacar().pontos(Lado.A));
    }

    // 3: joelho na barriga = 2
    @Test
    void joelhoNaBarrigaValeDois() {
        novo();
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        executar(Lado.A, CatalogoTecnicasSolo.JOELHO_NA_BARRIGA, 95);
        assertEquals(Posicao.JOELHO_BARRIGA, m.getPosicao());
        ticks(estab() + 5);
        assertEquals(2, arb.getPlacar().pontos(Lado.A));
    }

    // 6 + 4.4.1 + 3.2: montada -> costas vale 4 + 4; voltar a montar (por vontade propria) nao vale de novo
    @Test
    void costasValemQuatroEAMontadaNaoSeRepete() {
        novo();
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        executar(Lado.A, CatalogoTecnicasSolo.MONTAR, 95);
        ticks(estab() + 5);
        executar(Lado.A, CatalogoTecnicasSolo.PEGAR_COSTAS, 95);
        assertEquals(Posicao.COSTAS, m.getPosicao());
        ticks(estab() + 5);
        assertEquals(8, arb.getPlacar().pontos(Lado.A), "montada e costas sao posicoes distintas");
        executar(Lado.A, CatalogoTecnicasSolo.COSTAS_PARA_MONTADA, 95);
        assertEquals(Posicao.MONTADA, m.getPosicao());
        ticks(estab() + 5);
        assertEquals(8, arb.getPlacar().pontos(Lado.A), "a mesma montada nao pontua de novo");
    }

    // 7: estabilizacao interrompida (o outro escapa) nao da pontos; da a vantagem do art. 5.1
    @Test
    void escapeDuranteAContagemNaoPontuaEDaVantagem() {
        novo();
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        executar(Lado.A, CatalogoTecnicasSolo.MONTAR, 95);
        ticks(60);
        executar(Lado.B, CatalogoTecnicasSolo.ESCAPE_MONTADA, 20);
        assertEquals(Posicao.MEIA_GUARDA, m.getPosicao());
        ticks(estab() + 5);
        assertEquals(0, arb.getPlacar().pontos(Lado.A));
        assertEquals(1, arb.getPlacar().vantagens(Lado.A), "chegou a montada sem estabilizar");
        assertEquals(0, arb.getPlacar().pontos(Lado.B), "escapar para a meia-guarda nao e pontuacao");
    }

    // 9: depois de perder o dominio pelo escape do outro, voltar a dominar pontua de novo (nao e duplicata)
    @Test
    void depoisDoEscapeOMesmoDominioPontuaDeNovoMasNuncaDuasVezesSeguidas() {
        novo();
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        executar(Lado.A, CatalogoTecnicasSolo.MONTAR, 95);
        ticks(estab() + 5);
        assertEquals(4, arb.getPlacar().pontos(Lado.A));
        executar(Lado.B, CatalogoTecnicasSolo.ESCAPE_MONTADA, 20);
        assertEquals(Posicao.MEIA_GUARDA, m.getPosicao());
        ticks(30);
        TecnicaSolo saida = CAT.repertorio(m.getEstado(), Lado.A).stream()
                .filter(t -> t.sucesso() != null && t.sucesso().destino() == Posicao.CONTROLE_LATERAL).findFirst()
                .orElseThrow();
        executar(Lado.A, saida.id(), 10);
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao());
        ticks(estab() + 5);
        assertEquals(7, arb.getPlacar().pontos(Lado.A), "passou a meia-guarda: +3");
        executar(Lado.A, CatalogoTecnicasSolo.MONTAR, 95);
        ticks(estab() + 5);
        assertEquals(11, arb.getPlacar().pontos(Lado.A), "montada de novo depois do escape do outro: +4");
        ticks(500);
        assertEquals(11, arb.getPlacar().pontos(Lado.A));
    }

    // 1: queda valida (tecnica de queda de quem fica por cima) = 2
    @Test
    void quedaValeDois() {
        novo();
        ticks(5);
        executar(Lado.A, CatalogoTecnicasSolo.ENTRADA_PERNAS, 50);
        for (int i = 0; i < 200 && (m.isAterrissando() || !m.getPosicao().isNoChao()); i++) {
            ticks(1);
        }
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao(), "caiu nos cem quilos");
        assertEquals(Lado.A, m.getEstado().getPorCima().orElseThrow());
        ticks(estab() + 5);
        assertEquals(2, arb.getPlacar().pontos(Lado.A), "QUEDA +2 (cair nos cem quilos nao e passagem)");
        ticks(600);
        assertEquals(2, arb.getPlacar().pontos(Lado.A));
        assertEquals(0, arb.getPlacar().pontos(Lado.B));
    }

    // 25 + 5.3: tentativa de finalizacao sem perigo real nao da vantagem
    @Test
    void finalizacaoSemPerigoRealNaoDaVantagem() {
        novo();
        em(Posicao.GUARDA, Lado.A, Lado.B);
        m.definirEstabilidade(95);
        assertEquals(Resultado.INICIADA, m.comando(Lado.B, new ComandoSolo(Direcao.CIMA, Familia.L2)).resultado());
        assertEquals(Resultado.CONTRA, m.comando(Lado.A, new ComandoSolo(Direcao.BAIXO, Familia.RS)).resultado(),
                "cortou a entrada");
        ticks(60);
        assertEquals(0, arb.getPlacar().vantagens(Lado.B), "tentativa sem perigo nao e vantagem");
        assertEquals(0, arb.getPlacar().punicoes(Lado.A) + arb.getPlacar().punicoes(Lado.B), "nenhuma punicao");
    }

    // 12: fim do tempo: pontos, depois vantagens, depois punicoes; empate total = empate declarado
    @Test
    void resultadoPeloPlacar() {
        Placar p = new Placar();
        assertEquals(ResultadoLuta.Motivo.EMPATE, ResultadoLuta.pelo(p).motivo());
        assertTrue(ResultadoLuta.pelo(p).texto().startsWith("EMPATE"), "sem vencedor inventado");
        p.somarVantagem(Lado.B);
        assertEquals(Lado.B, ResultadoLuta.pelo(p).vencedor());
        assertEquals(ResultadoLuta.Motivo.VANTAGENS, ResultadoLuta.pelo(p).motivo());
        p.somarPontos(Lado.A, 2);
        assertEquals(Lado.A, ResultadoLuta.pelo(p).vencedor());
        assertEquals(ResultadoLuta.Motivo.PONTOS, ResultadoLuta.pelo(p).motivo());
        Placar q = new Placar();
        q.punir(Lado.A);
        assertEquals(Lado.B, ResultadoLuta.pelo(q).vencedor(), "menos punicoes");
        assertEquals(ResultadoLuta.Motivo.PUNICOES, ResultadoLuta.pelo(q).motivo());
    }

    @Test
    void escalaDePunicoesDoArtigo7() {
        Placar p = new Placar();
        p.punir(Lado.A);
        assertEquals(0, p.vantagens(Lado.B));
        p.punir(Lado.A);
        assertEquals(1, p.vantagens(Lado.B), "2a: vantagem ao adversario");
        p.punir(Lado.A);
        assertEquals(2, p.pontos(Lado.B), "3a: 2 pontos ao adversario");
        p.punir(Lado.A);
        assertEquals(Lado.A, p.getDesclassificado().orElseThrow(), "4a: desclassificacao");
        assertEquals(ResultadoLuta.Motivo.DESCLASSIFICACAO, ResultadoLuta.pelo(p).motivo());
    }

    @Test
    void tempoAcabandoComPosicaoSemEstabilizarViraVantagem() {
        novo();
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        executar(Lado.A, CatalogoTecnicasSolo.MONTAR, 95);
        ticks(50);
        ResultadoLuta r = arb.encerrarPorTempo();
        assertEquals(0, arb.getPlacar().pontos(Lado.A));
        assertEquals(1, arb.getPlacar().vantagens(Lado.A));
        assertEquals(Lado.A, r.vencedor());
        assertEquals(ResultadoLuta.Motivo.VANTAGENS, r.motivo());
        assertFalse(r.texto().isEmpty());
    }
}
