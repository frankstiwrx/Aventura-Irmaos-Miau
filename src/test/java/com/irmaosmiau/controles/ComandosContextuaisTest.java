package com.irmaosmiau.controles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.posicional.EstadoPosicional;
import com.irmaosmiau.posicional.Lado;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Entrega 5F: resolvedor das 3 camadas do D-pad e repertorio por papel (logica pura). */
class ComandosContextuaisTest {

    private static final DirecaoTecnica C = DirecaoTecnica.CIMA;
    private static final DirecaoTecnica B = DirecaoTecnica.BAIXO;
    private static final DirecaoTecnica E = DirecaoTecnica.ESQUERDA;
    private static final DirecaoTecnica D = DirecaoTecnica.DIREITA;

    private final ResolvedorComandosContextuais r = new ResolvedorComandosContextuais();

    private Optional<PosicaoComando> tick(boolean lt, boolean lb, DirecaoTecnica... dirs) {
        r.atualizar(EntradaContextual.de(lt, lb, dirs));
        return r.consumir();
    }

    private static PosicaoComando p(CamadaComando c, DirecaoTecnica d) {
        return new PosicaoComando(c, d);
    }

    @Test
    void quatroDirecoesNaCamada1() {
        for (DirecaoTecnica d : DirecaoTecnica.values()) {
            assertEquals(Optional.of(p(CamadaComando.UM, d)), tick(false, false, d), d.name());
            assertTrue(tick(false, false).isEmpty(), "soltar nao dispara");
        }
    }

    @Test
    void ltMaisDirecaoECamada2ELtLbMaisDirecaoECamada3() {
        tick(true, false);
        assertEquals(CamadaComando.DOIS, r.getCamadaAtiva(), "HUD mostra a camada 2 ao segurar LT");
        assertEquals(Optional.of(p(CamadaComando.DOIS, D)), tick(true, false, D));
        tick(true, false);
        tick(true, true);
        // 5H (pedido): LT + LB = camada avancada (4); a camada 3 passou a ser LB sozinho.
        assertEquals(CamadaComando.QUATRO, r.getCamadaAtiva());
        assertEquals(Optional.of(p(CamadaComando.QUATRO, E)), tick(true, true, E));
        tick(false, false);
        assertEquals(CamadaComando.UM, r.getCamadaAtiva());
    }

    @Test
    void prioridadeDasCombinacoesELbSozinhoNaoMudaCamada() {
        // 5H (pedido): LT+LB = 4 (avancada), LT = 2, LB = 3, nada = 1.
        assertEquals(Optional.of(p(CamadaComando.QUATRO, C)), tick(true, true, C), "LT+LB+dir antes de LT+dir");
        tick(false, false);
        assertEquals(Optional.of(p(CamadaComando.DOIS, C)), tick(true, false, C), "LT+dir antes de dir");
        tick(false, false);
        assertEquals(Optional.of(p(CamadaComando.TRES, C)), tick(false, true, C), "LB sozinho = camada 3 (5H)");
    }

    @Test
    void segurarNaoRepeteENovoToqueENecessario() {
        assertTrue(tick(false, false, C).isPresent());
        for (int i = 0; i < 120; i++) {
            assertTrue(tick(false, false, C).isEmpty(), "segurando: nada de repeticao");
        }
        tick(false, false);
        assertTrue(tick(false, false, C).isPresent(), "novo toque, nova execucao");
    }

    @Test
    void trocarModificadorDuranteOPressionamentoNaoDisparaOutroGolpe() {
        assertEquals(Optional.of(p(CamadaComando.UM, D)), tick(false, false, D));
        assertTrue(tick(true, false, D).isEmpty(), "apertou LT com a direcao segurada");
        assertTrue(tick(true, true, D).isEmpty(), "e LB");
        assertTrue(tick(false, false, D).isEmpty(), "soltou os dois");
        tick(true, false);
        assertEquals(Optional.of(p(CamadaComando.DOIS, D)), tick(true, false, D), "so com novo toque");
    }

    @Test
    void duasDirecoesJuntasNaoDisparamNada() {
        assertTrue(tick(false, false, C, D).isEmpty(), "diagonal");
        assertTrue(tick(false, false, C).isEmpty(), "sobrou uma: nao e toque novo");
        tick(false, false);
        assertEquals(Optional.of(p(CamadaComando.UM, D)), tick(false, false, D));
        assertTrue(tick(false, false, D, B).isEmpty(), "outra direcao com uma segurada");
    }

    @Test
    void limparEsqueceOPendenteEEsperaSoltar() {
        r.atualizar(EntradaContextual.de(false, false, C));
        r.limpar();                                         // pausa/foco/desconexao no mesmo tick
        assertTrue(r.consumir().isEmpty());
        assertTrue(tick(false, false, C).isEmpty(), "segurado desde antes da limpeza");
        tick(false, false);
        assertTrue(tick(false, false, C).isPresent());
    }

    // =====================================================
    // REPERTORIO POR CONTEXTO
    // =====================================================
    private static List<CelulaContextual> celulas(SlotJogador s, PapelCombate papel, EstadoPosicional e, Lado l) {
        return RepertorioContextual.celulas(s, papel, e, l, a -> true);
    }

    private static String resumo(List<CelulaContextual> l) {
        return l.stream().map(c -> c.posicao() + "=" + c.rotulo() + (c.isExecutavel() ? "" : "*")).toList().toString();
    }

    @Test
    void livreSoPegar() {
        List<CelulaContextual> l = celulas(SlotJogador.JOGADOR_1, PapelCombate.LIVRE, EstadoPosicional.separados(), Lado.A);
        assertEquals("[C1↑=PEGAR]", resumo(l));
        assertEquals(AcaoJogador.INICIAR_PEGADA, l.get(0).acao());
        assertEquals("[]", resumo(celulas(SlotJogador.JOGADOR_2, PapelCombate.LIVRE,
                EstadoPosicional.separados(), Lado.B)), "o Jogador 2 ainda nao aplica pegada");
    }

    @Test
    void aplicadorDesequilibraEReforcaEQuedaFicaReservada() {
        List<CelulaContextual> l = celulas(SlotJogador.JOGADOR_1, PapelCombate.APLICADOR,
                EstadoPosicional.pegada(Lado.A), Lado.A);
        assertEquals("[C1↑=REFORÇO, C1→=DESEQ., C2↑=QUEDA*, C3↑=GUARDA*]", resumo(l));
        CelulaContextual queda = RepertorioContextual.celula(l, new PosicaoComando(CamadaComando.DOIS,
                DirecaoTecnica.CIMA)).orElseThrow();
        assertFalse(queda.isExecutavel());
        assertEquals("queda", queda.idTransicao());
    }

    @Test
    void receptorRompeEPuxarGuardaReservado() {
        List<CelulaContextual> l = celulas(SlotJogador.JOGADOR_2, PapelCombate.RECEPTOR,
                EstadoPosicional.pegada(Lado.A), Lado.B);
        assertEquals("[C1↓=ROMPER, C3↑=GUARDA*]", resumo(l));
    }

    @Test
    void entrandoNinguemAgeEAte12PosicoesSemDuplicar() {
        assertTrue(celulas(SlotJogador.JOGADOR_1, PapelCombate.ENTRANDO, EstadoPosicional.separados(), Lado.A).isEmpty());
        for (PapelCombate papel : PapelCombate.values()) {
            List<CelulaContextual> l = celulas(SlotJogador.JOGADOR_1, papel, EstadoPosicional.pegada(Lado.A), Lado.A);
            assertTrue(l.size() <= 12);
            assertEquals(l.size(), l.stream().map(CelulaContextual::posicao).distinct().count(), "uma coisa por posicao");
        }
    }

    @Test
    void disponibilidadeVemDoPredicado() {
        List<CelulaContextual> l = RepertorioContextual.celulas(SlotJogador.JOGADOR_1, PapelCombate.APLICADOR,
                EstadoPosicional.pegada(Lado.A), Lado.A, a -> a != AcaoJogador.REFORCAR_PEGADA);
        assertFalse(l.get(0).disponivelAgora(), "reforco em recarga: aparece, mas cinza");
        assertTrue(l.get(1).disponivelAgora());
        assertEquals(Optional.of(new PosicaoComando(CamadaComando.UM, DirecaoTecnica.BAIXO)),
                RepertorioContextual.posicaoDe(PapelCombate.RECEPTOR, AcaoJogador.ROMPIMENTO_TECNICO));
    }
}
