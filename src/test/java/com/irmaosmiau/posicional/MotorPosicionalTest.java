package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.ResultadoAcao;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.MotorPosicional.Modo;
import com.irmaosmiau.posicional.MotorPosicional.ResultadoTentativa;
import com.irmaosmiau.posicional.RepertorioPosicional.SlotContextual;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Entrega 5E: estados posicionais, papeis dinamicos e transicoes (simulador isolado). */
class MotorPosicionalTest {

    private static final CatalogoTransicoes CAT = CatalogoTransicoes.referencia();
    private static final FatosCombate PEGADA = FatosCombate.pegadaFirme();

    private static MotorPosicional simulador(EstadoPosicional inicial) {
        return new MotorPosicional(Modo.SIMULACAO, CAT, inicial);
    }

    /** Inicia, espera a preparacao inteira (ticks do loop) e resolve. */
    private static EstadoPosicional executar(MotorPosicional m, Lado quem, String id, ResultadoTransicao r) {
        assertEquals(ResultadoTentativa.INICIADA, m.tentar(quem, id, PEGADA), id);
        int duracao = CAT.buscar(id).orElseThrow().getDuracaoTicks();
        for (int i = 0; i < duracao; i++) {
            assertThrows(IllegalStateException.class, () -> m.resolver(r), "ainda preparando");
            m.atualizar();
        }
        return m.resolver(r);
    }

    @Test
    void caminhoDeReferenciaAteOCemQuilos() {
        MotorPosicional m = simulador(EstadoPosicional.separados());
        EstadoPosicional e = executar(m, Lado.A, CatalogoTransicoes.ESTABELECER_PEGADA, ResultadoTransicao.SUCESSO);
        assertEquals(Posicao.EM_PE_PEGADA, e.getPosicao());
        assertEquals(Optional.of(Lado.A), e.getControlador());

        e = executar(m, Lado.A, CatalogoTransicoes.QUEDA, ResultadoTransicao.SUCESSO);
        assertEquals(Posicao.GUARDA, e.getPosicao());
        assertEquals(Optional.of(Lado.A), e.getPorCima());
        assertEquals(Optional.of(Lado.B), e.getPorBaixo());

        e = executar(m, Lado.A, CatalogoTransicoes.PASSAGEM_GUARDA, ResultadoTransicao.SUCESSO);
        assertEquals(Posicao.CONTROLE_LATERAL, e.getPosicao());
        assertEquals(Optional.of(Lado.A), e.getPorCima());
        assertEquals(Optional.of(Lado.A), e.getControlador());
        assertTrue(e.getTransicao().isEmpty());
    }

    @Test
    void passagemDefendidaMantemAGuardaEDaOControleAoDeBaixo() {
        MotorPosicional m = simulador(EstadoPosicional.noChao(Posicao.GUARDA, Lado.A, Lado.A));
        assertEquals(ResultadoTentativa.INICIADA, m.tentar(Lado.A, CatalogoTransicoes.PASSAGEM_GUARDA, PEGADA));
        assertTrue(m.sinalizarDefesa(Lado.B), "defesa simultanea dentro da janela");
        assertFalse(m.sinalizarDefesa(Lado.A), "quem tenta nao defende a propria tentativa");
        assertTrue(m.getEstado().getTransicao().orElseThrow().defesaSinalizada());
        for (int i = 0; i < 60; i++) {
            m.atualizar();
        }
        assertFalse(m.sinalizarDefesa(Lado.B), "janela fechada");
        EstadoPosicional e = m.resolver(ResultadoTransicao.DEFENDIDA);
        assertEquals(Posicao.GUARDA, e.getPosicao());
        assertEquals(Optional.of(Lado.A), e.getPorCima(), "continua por cima");
        assertEquals(Optional.of(Lado.B), e.getControlador(), "a guarda segurou: controle de quem esta por baixo");
    }

    @Test
    void raspagemInverteQuemEstaPorCima() {
        MotorPosicional m = simulador(EstadoPosicional.noChao(Posicao.GUARDA, Lado.A, Lado.A));
        assertEquals(ResultadoTentativa.PAPEL_INCOMPATIVEL, m.tentar(Lado.A, CatalogoTransicoes.RASPAGEM, PEGADA),
                "quem esta por cima nao raspa");
        EstadoPosicional e = executar(m, Lado.B, CatalogoTransicoes.RASPAGEM, ResultadoTransicao.SUCESSO);
        assertEquals(Posicao.MONTADA, e.getPosicao());
        assertEquals(Optional.of(Lado.B), e.getPorCima(), "o defensor de antes agora esta por cima");
        assertEquals(Optional.of(Lado.B), e.getControlador());
        assertTrue(e.temPapel(Lado.A, PapelPosicional.POR_BAIXO));
    }

    @Test
    void quedaRevertidaEFalhaEDefesa() {
        MotorPosicional m = simulador(EstadoPosicional.pegada(Lado.A));
        EstadoPosicional e = executar(m, Lado.A, CatalogoTransicoes.QUEDA, ResultadoTransicao.INVERSAO);
        assertEquals(Posicao.GUARDA, e.getPosicao());
        assertEquals(Optional.of(Lado.B), e.getPorCima(), "sprawl: quem defendeu termina por cima");

        m = simulador(EstadoPosicional.pegada(Lado.A));
        assertEquals(Posicao.EM_PE_PEGADA, executar(m, Lado.A, CatalogoTransicoes.QUEDA,
                ResultadoTransicao.DEFENDIDA).getPosicao());
        assertEquals(Optional.of(Lado.A), m.getEstado().getControlador());
        assertEquals(Posicao.EM_PE_SEPARADOS, executar(m, Lado.A, CatalogoTransicoes.QUEDA,
                ResultadoTransicao.FALHA).getPosicao());
    }

    @Test
    void controleNaoEPermanenteDoAplicador() {
        MotorPosicional m = simulador(EstadoPosicional.pegada(Lado.A));
        EstadoPosicional e = executar(m, Lado.B, CatalogoTransicoes.PUXAR_GUARDA, ResultadoTransicao.SUCESSO);
        assertEquals(Posicao.GUARDA, e.getPosicao());
        assertEquals(Optional.of(Lado.A), e.getPorCima(), "quem puxou fica por baixo");
        assertEquals(Optional.of(Lado.B), e.getControlador(), "...mas com o controle da guarda");
        assertEquals(ResultadoTentativa.PAPEL_INCOMPATIVEL,
                new MotorPosicional(Modo.SIMULACAO, CAT, EstadoPosicional.pegada(Lado.A))
                        .tentar(Lado.B, CatalogoTransicoes.QUEDA, PEGADA), "B nao controla: nao derruba");
    }

    @Test
    void transicoesInvalidas() {
        MotorPosicional m = simulador(EstadoPosicional.separados());
        assertEquals(ResultadoTentativa.POSICAO_INCOMPATIVEL, m.tentar(Lado.A, CatalogoTransicoes.QUEDA, PEGADA));
        assertEquals(ResultadoTentativa.DESCONHECIDA, m.tentar(Lado.A, "voadora", PEGADA));
        m = simulador(EstadoPosicional.pegada(Lado.A));
        assertEquals(ResultadoTentativa.CONDICAO_NAO_ATENDIDA,
                m.tentar(Lado.A, CatalogoTransicoes.QUEDA, FatosCombate.NENHUM), "sem pegada estabelecida");
        assertEquals(ResultadoTentativa.INICIADA, m.tentar(Lado.A, CatalogoTransicoes.QUEDA, PEGADA));
        assertEquals(ResultadoTentativa.TRANSICAO_EM_ANDAMENTO,
                m.tentar(Lado.A, CatalogoTransicoes.PUXAR_GUARDA, PEGADA));
        for (int i = 0; i < 45; i++) {
            m.atualizar();
        }
        m.cancelar();
        assertTrue(m.getEstado().getTransicao().isEmpty());
        assertEquals(Posicao.EM_PE_PEGADA, m.getEstado().getPosicao(), "cancelar nao muda a posicao");
        MotorPosicional semTransicao = m;
        assertThrows(IllegalStateException.class, () -> semTransicao.resolver(ResultadoTransicao.SUCESSO));
        // Raspagem nao tem INVERSAO.
        MotorPosicional r = simulador(EstadoPosicional.noChao(Posicao.GUARDA, Lado.A, Lado.A));
        r.tentar(Lado.B, CatalogoTransicoes.RASPAGEM, PEGADA);
        for (int i = 0; i < 45; i++) {
            r.atualizar();
        }
        assertThrows(IllegalStateException.class, () -> r.resolver(ResultadoTransicao.INVERSAO));
        assertThrows(IllegalArgumentException.class, () -> EstadoPosicional.noChao(Posicao.GUARDA, null, Lado.A));
    }

    @Test
    void modoJogoNaoLiberaPosicoesDeChaoAindaSemVisual() {
        MotorPosicional jogo = new MotorPosicional(Modo.JOGO, CAT, EstadoPosicional.pegada(Lado.A));
        assertEquals(ResultadoTentativa.NAO_LIBERADA_NO_JOGO, jogo.tentar(Lado.A, CatalogoTransicoes.QUEDA, PEGADA));
        assertEquals(ResultadoTentativa.NAO_LIBERADA_NO_JOGO,
                jogo.tentar(Lado.B, CatalogoTransicoes.PUXAR_GUARDA, PEGADA));
        assertEquals(Posicao.EM_PE_PEGADA, jogo.getEstado().getPosicao(), "nada mudou");
        MotorPosicional jogoEmPe = new MotorPosicional(Modo.JOGO, CAT, EstadoPosicional.separados());
        assertEquals(ResultadoTentativa.INICIADA,
                jogoEmPe.tentar(Lado.A, CatalogoTransicoes.ESTABELECER_PEGADA, FatosCombate.NENHUM),
                "a pegada em pe continua liberada");
        for (DefinicaoTransicao d : CAT.todas()) {
            boolean soEmPe = d.destinosPossiveis().stream().noneMatch(Posicao::isNoChao);
            assertEquals(soEmPe, MotorPosicional.liberadaNoJogo(d), d.getId());
        }
    }

    @Test
    void duracaoContadaEmTicksDoLoopLogico() {
        MotorPosicional m = simulador(EstadoPosicional.pegada(Lado.A));
        m.tentar(Lado.A, CatalogoTransicoes.QUEDA, PEGADA);
        for (int i = 0; i < 44; i++) {
            m.atualizar();
        }
        TransicaoEmCurso t = m.getEstado().getTransicao().orElseThrow();
        assertFalse(t.isPronta());
        assertEquals(44 / 45.0, t.progresso(), 1e-9);
        m.atualizar();
        assertTrue(m.getEstado().getTransicao().orElseThrow().isPronta());
        EstadoPosicional e = m.resolverCom((tr, f) -> tr.defesaSinalizada()
                ? ResultadoTransicao.DEFENDIDA : ResultadoTransicao.SUCESSO, PEGADA);
        assertEquals(Posicao.GUARDA, e.getPosicao(), "resolvedor externo (futura disputa 5A)");
    }

    // =====================================================
    // COMPATIBILIDADE COM O COMBATE EM PE ATUAL
    // =====================================================
    @Test
    void leituraEmPeAcompanhaAPegada1SemDuplicarEstado() {
        IrmaoMiau branco = new IrmaoMiauBranco(440, 150, 0.7);
        IrmaoMiau preto = new IrmaoMiauPreto(500, 150, 0.7);
        branco.setViradoParaDireita(true);
        preto.setViradoParaDireita(false);
        SistemaCombate sistema = CarregadorConteudoCombate.carregarParaJogo().criarSistema(branco, preto);
        Runnable tick = () -> {
            branco.atualizar(false, false);
            preto.atualizar(false, false);
            sistema.atualizar();
        };
        assertEquals(EstadoPosicional.separados(), LeituraPosicionalEmPe.ler(sistema, CAT));
        assertEquals(ResultadoAcao.SUCESSO, sistema.tentarAcao(branco, preto, "pegada_1"));
        for (int i = 0; i < 10; i++) {
            tick.run();
        }
        EstadoPosicional entrando = LeituraPosicionalEmPe.ler(sistema, CAT);
        assertEquals(Posicao.EM_PE_SEPARADOS, entrando.getPosicao());
        TransicaoEmCurso t = entrando.getTransicao().orElseThrow();
        assertEquals(CatalogoTransicoes.ESTABELECER_PEGADA, t.definicao().getId());
        assertEquals(Lado.A, t.iniciador());
        assertEquals("pegada_1", t.definicao().getIdTecnica().orElseThrow());
        assertTrue(t.ticksDecorridos() > 0 && !t.isPronta(), "os 30 quadros da PEGADA_1 como preparacao");
        for (int i = 0; i < 25; i++) {
            tick.run();
        }
        EstadoPosicional e = LeituraPosicionalEmPe.ler(sistema, CAT);
        assertEquals(EstadoPosicional.pegada(Lado.A), e);
        assertTrue(LeituraPosicionalEmPe.fatos(sistema).pegadaEstabelecida());
        assertEquals(100.0, LeituraPosicionalEmPe.fatos(sistema).firmeza(), 1.0);
        sistema.encerrarInteracao();
        assertEquals(EstadoPosicional.separados(), LeituraPosicionalEmPe.ler(sistema, CAT));
    }

    @Test
    void repertorioPropoeAteQuatroAcoesPorPapel() {
        RepertorioPosicional rep = RepertorioPosicional.referencia(CAT);
        EstadoPosicional pegada = EstadoPosicional.pegada(Lado.A);
        assertEquals(CatalogoTransicoes.QUEDA,
                rep.transicaoDo(pegada, Lado.A, SlotContextual.ACAO_1, false).orElseThrow().getId());
        assertEquals(CatalogoTransicoes.PUXAR_GUARDA,
                rep.transicaoDo(pegada, Lado.A, SlotContextual.ACAO_1, true).orElseThrow().getId(),
                "mesmo botao com modificador = variacao");
        assertTrue(rep.transicaoDo(pegada, Lado.B, SlotContextual.ACAO_1, false).isEmpty());
        EstadoPosicional guarda = EstadoPosicional.noChao(Posicao.GUARDA, Lado.A, Lado.A);
        assertEquals(List.of(CatalogoTransicoes.PASSAGEM_GUARDA), rep.opcoes(guarda, Lado.A).stream()
                .map(o -> o.transicao().getId()).toList());
        assertEquals(List.of(CatalogoTransicoes.RASPAGEM), rep.opcoes(guarda, Lado.B).stream()
                .map(o -> o.transicao().getId()).toList(), "mesmo slot, outro papel, outra tecnica");
        for (Posicao p : Posicao.values()) {
            rep.slotsUsados(p).values().forEach(n -> assertTrue(n <= 4));
        }
    }
}
