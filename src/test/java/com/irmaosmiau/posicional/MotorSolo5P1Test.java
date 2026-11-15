package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.CustosFolego;
import com.irmaosmiau.entities.DefesaCorporal;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.Resultado;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5P.1 no MotorSolo isolado: solo livre (deitar por vontade propria),
 * recuperacao contextual e sua vulnerabilidade, execucao mais acessivel
 * (janelas, aterrissagem, comandos logo depois das transicoes), RT como base
 * por borda, nomes de guarda distintos, custos e mensagens confirmadas.
 */
class MotorSolo5P1Test {

    private IrmaoMiau a;
    private IrmaoMiau b;
    private MotorSolo m;
    private Intencao ia = Intencao.NENHUMA;
    private Intencao ib = Intencao.NENHUMA;

    private void novo(int xa, int xb) {
        a = new IrmaoMiauBranco(xa, 150, 0.7);
        b = new IrmaoMiauPreto(xb, 150, 0.7);
        a.setViradoParaDireita(xa < xb);
        b.setViradoParaDireita(xb < xa);
        m = new MotorSolo(a, b, null, 15, 715);
    }

    private IrmaoMiau l(Lado lado) {
        return lado == Lado.A ? a : b;
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            m.informarIntencoes(ia, ib);
            m.atualizar(ia, ib);
            a.atualizar(false, false);
            b.atualizar(false, false);
        }
    }

    private void resolver() {
        for (int i = 0; i < 120 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        assertTrue(m.getTentativa().isEmpty());
        ticks(1);
    }

    private static ComandoSolo rs(Direcao d) {
        return new ComandoSolo(d, false);
    }

    private static ComandoSolo rb(Direcao d) {
        return new ComandoSolo(d, true);
    }

    private void deitar(IrmaoMiau x) {
        x.alternarDeitado();
        assertTrue(x.isDeitado());
    }

    private void ateAterrissar() {
        for (int i = 0; i < 60 && !m.isAterrissando(); i++) {
            ticks(1);
        }
        assertTrue(m.isAterrissando());
    }

    // ================= 1. SOLO LIVRE
    @Test
    void deitarPorVontadePropriaEntraNoMesmoSistemaSemVinculo() {
        for (Lado deitou : Lado.values()) {
            novo(300, 380);
            deitar(l(deitou));
            ticks(30);
            assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao(), deitou + " deitou");
            assertEquals(deitou.outro(), m.getEstado().getPorCima().orElseThrow(), "quem ficou em pe esta 'por cima'");
            assertTrue(m.getEstado().getControlador().isEmpty(), "ninguem controla nada");
            assertFalse(m.conectado(a) || a.isVinculadoNoSolo() || b.isVinculadoNoSolo(), "sem vinculo");
            assertTrue(m.consumirMensagens().contains(NarradorSolo.sigla(deitou) + " foi ao chão."));
            // os dois lados tem respostas (nada de "EM BREVE")
            assertFalse(m.getCatalogo().repertorio(m.getEstado(), deitou).isEmpty());
            assertFalse(m.getCatalogo().repertorio(m.getEstado(), deitou.outro()).isEmpty());
        }
    }

    @Test
    void soloLivreNaoControlaADistanciaMasPertoAInteracaoEValida() {
        novo(100, 600);
        deitar(b);
        ticks(30);
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao());
        MotorSolo.Resposta longe = m.comando(Lado.A, rs(Direcao.CIMA));       // entrar na guarda de longe
        assertEquals(Resultado.RECUSADA, longe.resultado(), "a distancia nada e conquistado: " + longe.motivo());
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao());

        novo(300, 370);
        deitar(b);
        ticks(30);
        assertEquals(Resultado.INICIADA, m.comando(Lado.A, rs(Direcao.CIMA)).resultado(), "perto: entrar na guarda");
        resolver();
        assertTrue(m.getPosicao().isVinculoDeSolo(), "entrou: " + m.getPosicao());
        assertTrue(a.isVinculadoNoSolo() && b.isVinculadoNoSolo());
    }

    @Test
    void levantarDepoisDeDeitarPorVontadeEMaisRapidoQueDepoisDeSerDerrubado() {
        novo(300, 380);
        deitar(b);
        ticks(30);
        assertEquals(Resultado.INICIADA, m.comando(Lado.B, rs(Direcao.CIMA)).resultado(), "levantar");
        int voluntario = m.getTentativa().orElseThrow().janelaTotal();
        resolver();
        assertEquals(Posicao.EM_PE_SEPARADOS, m.getPosicao(), "levantou");

        // derrubado (avancou demais contra a guarda e o outro empurrou): fica desorientado
        novo(300, 370);
        for (int i = 0; i < DefesaCorporal.TICKS_ENTRADA + 1; i++) {
            b.getDefesa().manter(DefesaCorporal.Tipo.GUARDA_MEDIA);
            b.atualizar(false, false);
        }
        m.comando(Lado.A, rb(Direcao.CIMA));
        for (int i = 0; i < 120 && m.getTentativa().isPresent(); i++) {
            b.getDefesa().manter(DefesaCorporal.Tipo.GUARDA_MEDIA);
            ticks(1);
        }
        b.getDefesa().interromper();
        ticks(1);
        m.comando(Lado.B, rb(Direcao.CIMA));
        resolver();
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao());
        assertTrue(a.isDeitado(), "A foi derrubado");
        for (int i = 0; i < 40 && m.recargaDe(Lado.A) > 0; i++) {
            ticks(1);
        }
        MotorSolo.Resposta r = m.comando(Lado.A, rs(Direcao.CIMA));
        assertEquals(Resultado.INICIADA, r.resultado(), r.motivo());
        int derrubado = m.getTentativa().orElseThrow().janelaTotal();
        assertTrue(voluntario < derrubado, "voluntario " + voluntario + " < derrubado " + derrubado);
        assertEquals((int) Math.round(14 * MotorSolo.FATOR_JANELA * MotorSolo.FATOR_LEVANTAR_VOLUNTARIO), voluntario);
    }

    // ================= 3-4. RECUPERACAO CONTEXTUAL E VULNERABILIDADE
    @Test
    void fatorDeRespiracaoPorContexto() {
        novo(300, 380);
        assertEquals(1.0, m.fatorRespiracao(Lado.A), 1e-9, "livre: normal");
        m.forcarPosicao(Posicao.MONTADA, Lado.A, Lado.A);
        ticks(1);
        assertEquals(CustosFolego.RESPIRACAO_SOLO_POR_CIMA, m.fatorRespiracao(Lado.A), 1e-9, "por cima: devagar");
        m.definirEstabilidade(100);
        assertEquals(CustosFolego.RESPIRACAO_SOLO_POR_BAIXO_MIN, m.fatorRespiracao(Lado.B), 1e-9, "dominado: bem devagar");
        m.definirEstabilidade(0);
        assertEquals(CustosFolego.RESPIRACAO_SOLO_POR_BAIXO_MAX, m.fatorRespiracao(Lado.B), 1e-9);
        assertTrue(m.fatorRespiracao(Lado.B) < 1.0 && m.fatorRespiracao(Lado.B) > 0);
        // durante a propria tecnica: nada
        m.forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        ticks(1);
        assertEquals(Resultado.INICIADA, m.comando(Lado.A, rs(Direcao.CIMA)).resultado());   // montar
        assertEquals(0.0, m.fatorRespiracao(Lado.A), 1e-9, "na propria tecnica nao recupera");
        // o fator chega ao lutador
        ticks(1);
        assertEquals(0.0, a.fatorRespiracaoContexto(), 1e-9);
    }

    @Test
    void noSoloRespirarRecuperaDevagarEParadoNao() {
        novo(300, 380);
        m.forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        ticks(30);
        b.gastarFolegoEsforco(10, "teste");
        double f = b.getFolegoAtual();
        ticks(60);
        assertTrue(b.getFolegoAtual() <= f + 1e-9, "parado no solo: sem recuperacao passiva");
        b.setRespirando(true);
        f = b.getFolegoAtual();
        ticks(60);
        assertTrue(b.getFolegoAtual() > f, "respirando de proposito: recupera um pouco");
        double ganhoSolo = b.getFolegoAtual() - f;
        b.setRespirando(false);
        m.forcarPosicao(Posicao.EM_PE_SEPARADOS, null, null);
        ticks(30);
        b.setRespirando(true);
        f = b.getFolegoAtual();
        ticks(60);
        b.setRespirando(false);
        assertTrue(ganhoSolo < (b.getFolegoAtual() - f) * 0.5, "no solo bem mais devagar que livre");
    }

    @Test
    void quemControlaERespiraPerdeEstabilidade() {
        novo(300, 380);
        m.forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        ticks(30);
        m.definirEstabilidade(60);
        ticks(30);
        assertTrue(m.getEstabilidade() >= 60, "sem respirar o controle se mantem");
        m.definirEstabilidade(60);
        a.setRespirando(true);
        ticks(30);
        a.setRespirando(false);
        assertTrue(m.getEstabilidade() < 60, "respirando o controle afrouxa: " + m.getEstabilidade());
        assertTrue(m.getEstabilidade() > 45, "mas de forma sutil: " + m.getEstabilidade());
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao(), "respirar sozinho nao entrega a posicao");
    }

    @Test
    void respirandoAsPropriasTecnicasSaemMaisDevagar() {
        novo(300, 380);
        m.forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        ticks(30);
        m.comando(Lado.A, rs(Direcao.CIMA));
        int normal = m.getTentativa().orElseThrow().janelaTotal();
        novo(300, 380);
        m.forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        ticks(30);
        a.setRespirando(true);
        m.comando(Lado.A, rs(Direcao.CIMA));
        int respirando = m.getTentativa().orElseThrow().janelaTotal();
        assertTrue(respirando > normal, "respirando " + respirando + " > " + normal + " (mais tempo para o outro)");
    }

    // ================= 5. EXECUCAO MAIS ACESSIVEL
    @Test
    void comandoLogoDepoisDeUmaTransicaoNoSoloEAceito() {
        novo(300, 380);
        m.forcarPosicao(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        ticks(1);                                              // posturas ainda em transicao
        assertTrue(a.getTransicaoAtiva() != null || b.getTransicaoAtiva() != null, "transicao em andamento");
        MotorSolo.Resposta r = m.comando(Lado.A, rs(Direcao.CIMA));
        assertEquals(Resultado.INICIADA, r.resultado(), "antes era EM MOVIMENTO: " + r.motivo());
    }

    @Test
    void aterrissagemDaTempoDeFecharAGuarda() {
        assertTrue(MotorSolo.ATERRISSAGEM_TICKS >= 24, "~0,4 s para escolher como cair");
        novo(300, 380);
        m.comando(Lado.A, rb(Direcao.BAIXO));
        ateAterrissar();
        ticks(18);                                             // no 5P (10 ticks) ja teria passado
        assertTrue(m.isAterrissando());
        assertEquals(Lado.B, m.ladoQueCai().orElseThrow());
        assertEquals(Resultado.CONCLUIDA, m.comando(Lado.B, rs(Direcao.CIMA)).resultado());
        ticks(MotorSolo.ATERRISSAGEM_TICKS + 30);
        assertEquals(Posicao.GUARDA, m.getPosicao());
        assertTrue(m.consumirMensagens().contains("J2 fechou a guarda!"));
    }

    @Test
    void janelasMaioresERecargasMenores() {
        assertTrue(MotorSolo.FATOR_JANELA > 1.0);
        assertTrue(MotorSolo.RECARGA_TICKS < 18 && MotorSolo.RECARGA_FALHA_TICKS < 40);
        novo(300, 380);
        m.forcarPosicao(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        ticks(30);
        m.comando(Lado.A, rs(Direcao.CIMA));
        TecnicaSolo t = m.getTentativa().orElseThrow().tecnica();
        assertTrue(m.getTentativa().orElseThrow().janelaTotal() > t.janela(), "janela maior que a do catalogo");
    }

    // ================= 3. RT: base por BORDA (reacao) x respiracao (segurado)
    @Test
    void rtApertadoNaJanelaEBaseRtDeAntesNao() {
        // apertado durante a entrada: base (queda sem controle)
        novo(300, 380);
        m.comando(Lado.A, rb(Direcao.BAIXO));
        ticks(3);
        ib = new Intencao(0, 0, true);                         // borda do RT
        ticks(1);
        ib = Intencao.NENHUMA;
        ateAterrissarOuResolver();
        ticks(40);
        assertEquals(Posicao.SOLO_DISPUTADO, m.getPosicao(), "RT apertado na janela = base");

        // apertado bem antes da entrada (ja segurando para respirar): nao e base
        novo(300, 380);
        ib = new Intencao(0, 0, true);
        ticks(1);
        ib = Intencao.NENHUMA;
        ticks(MotorSolo.BUFFER_BASE_TICKS + 20);
        m.comando(Lado.A, rb(Direcao.BAIXO));
        assertTrue(m.sofrendoEntrada(Lado.B), "B sofre a entrada: o RT vira base, nao respiracao");
        assertFalse(m.sofrendoEntrada(Lado.A));
        ateAterrissarOuResolver();
        ticks(MotorSolo.ATERRISSAGEM_TICKS + 40);
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao(), "RT velho nao conta como base");
    }

    private void ateAterrissarOuResolver() {
        for (int i = 0; i < 80 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
    }

    // ================= 6. GUARDAS: acoes com nomes distintos
    @Test
    void acoesDeGuardaTemNomesDistintos() {
        CatalogoTecnicasSolo c = CatalogoTecnicasSolo.referencia();
        String fechar = c.buscar(CatalogoTecnicasSolo.FECHAR_GUARDA).orElseThrow().rotulo();
        String manter = c.buscar(CatalogoTecnicasSolo.RETENCAO).orElseThrow().rotulo();
        String recuperar = c.buscar(CatalogoTecnicasSolo.RECUPERAR_GUARDA).orElseThrow().rotulo();
        String abrir = c.buscar(CatalogoTecnicasSolo.ABRIR_GUARDA).orElseThrow().rotulo();
        String puxar = c.buscar(CatalogoTecnicasSolo.PUXAR_GUARDA).orElseThrow().rotulo();
        String soltar = c.buscar(CatalogoTecnicasSolo.SOLTAR_GUARDA).orElseThrow().rotulo();
        Set<String> rotulos = new HashSet<>(List.of(fechar, manter, recuperar, abrir, puxar, soltar));
        assertEquals(6, rotulos.size(), "puxar/fechar/recuperar/manter/abrir/soltar distintos: " + rotulos);
        assertTrue(puxar.startsWith("PUXAR") && fechar.startsWith("FECHAR") && manter.startsWith("MANTER")
                && recuperar.startsWith("REC") && abrir.startsWith("ABRIR") && soltar.startsWith("SOLTAR"), rotulos.toString());
        for (TecnicaSolo t : c.todas()) {
            assertNotEquals("GUARDA", t.rotulo(), t.id() + ": rotulo ambiguo");
        }
    }

    // ================= 2. CUSTOS
    @Test
    void custosDoSoloMenoresEProporcionaisAoFolegoMaximo() {
        novo(300, 380);
        TecnicaSolo passar = m.getCatalogo().buscar(CatalogoTecnicasSolo.PASSAR_GUARDA).orElseThrow();
        double branco = m.custo(Lado.A, passar);
        double preto = m.custo(Lado.B, passar);
        assertTrue(branco < passar.custo() && preto < passar.custo(), "mais baratos que no 5P");
        assertTrue(preto > branco, "o mesmo esforco pesa menos para quem tem mais folego");
        assertEquals(branco / a.getAtributos().getFolego(), preto / b.getAtributos().getFolego(), 0.02,
                "fracao do folego parecida");
        // sequencia alvo (queda, passar, montada, escape) nao esgota ninguem
        String[] seq = {CatalogoTecnicasSolo.ENTRADA_PERNAS, CatalogoTecnicasSolo.PASSAR_GUARDA,
            CatalogoTecnicasSolo.MONTAR, CatalogoTecnicasSolo.ESCAPE_MONTADA, CatalogoTecnicasSolo.RASPAGEM};
        double total = 0;
        for (String id : seq) {
            total += m.custo(Lado.A, m.getCatalogo().buscar(id).orElseThrow());
        }
        assertTrue(total < a.getAtributos().getFolego() * 0.6, "sequencia custa " + total);
    }

    // ================= 7. MENSAGENS: so resultados confirmados
    @Test
    void mensagensSoParaResultadosConfirmados() {
        novo(300, 380);
        m.forcarPosicao(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        ticks(30);
        m.consumirMensagens();
        m.comando(Lado.A, rs(Direcao.CIMA));                   // so a tentativa
        ticks(2);
        assertTrue(m.consumirMensagens().isEmpty(), "tentativa nao vira aviso");
        assertEquals(Resultado.CONTRA, m.comando(Lado.B, rs(Direcao.CIMA)).resultado());
        ticks(2);
        List<String> ms = m.consumirMensagens();
        assertTrue(ms.contains("J2 conseguiu uma raspagem!"), ms.toString());
        assertTrue(m.consumirMensagens().isEmpty(), "consumidas uma vez");
        // recusa nao gera aviso
        m.comando(Lado.A, rb(Direcao.ESQUERDA));
        assertTrue(m.consumirMensagens().isEmpty());
        // reiniciar limpa
        m.forcarPosicao(Posicao.MONTADA, Lado.A, Lado.A);
        m.comando(Lado.B, rs(Direcao.ESQUERDA));
        resolver();
        m.reiniciar();
        assertTrue(m.consumirMensagens().isEmpty());
    }
}
