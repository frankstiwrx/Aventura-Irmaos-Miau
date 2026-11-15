package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.DefesaCorporal;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.Resultado;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Entrega 5P: o MotorSolo isolado (sem GamePanel), deterministico, nos dois sentidos. */
class MotorSolo5PTest {

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
            invariantes();
        }
    }

    /**
     * 5P.1 (pedido): as janelas ficaram ~30% maiores; em vez de contar os ticks
     * da janela antiga, espera a tentativa atual se resolver.
     */
    private void resolver() {
        for (int i = 0; i < 120 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        assertTrue(m.getTentativa().isEmpty(), "a tentativa se resolveu");
        ticks(1);
    }

    private static ComandoSolo rs(Direcao d) {
        return new ComandoSolo(d, false);
    }

    private static ComandoSolo rb(Direcao d) {
        return new ComandoSolo(d, true);
    }

    private MotorSolo.Resposta cmd(Lado l, ComandoSolo c) {
        MotorSolo.Resposta r = m.comando(l, c);
        invariantes();
        return r;
    }

    /** 22: nunca ha estados contraditorios entre os dois. */
    private void invariantes() {
        EstadoPosicional e = m.getEstado();
        assertEquals(m.conectado(a), m.conectado(b), "conectados juntos");
        assertEquals(a.isVinculadoNoSolo(), b.isVinculadoNoSolo(), "vinculo nos dois");
        if (e.getPosicao().exigeAlguemPorCima()) {
            Lado cima = e.getPorCima().orElseThrow();
            assertNotEquals(cima, e.getPorBaixo().orElseThrow());
            assertTrue(m.papel(Lado.A).startsWith("POR ") && m.papel(Lado.B).startsWith("POR "));
            assertNotEquals(m.papel(Lado.A), m.papel(Lado.B), "papeis complementares");
        }
        if (e.getPosicao().isVinculoDeSolo() && e.getPosicao() != Posicao.SOLO_DISPUTADO && !m.isAterrissando()) {
            Lado cima = e.getPorCima().orElseThrow();
            if (e.getPosicao() != Posicao.COSTAS && l(cima).getTransicaoAtiva() == null
                    && l(cima.outro()).getTransicaoAtiva() == null) {
                assertTrue(l(cima.outro()).isDeitado(), "quem esta por baixo esta deitado em " + e.getPosicao());
            }
        }
    }

    private void derrubarPelasPernas(Lado ini, Direcao reacaoDeQuemCai) {
        assertEquals(Resultado.INICIADA, cmd(ini, rb(Direcao.BAIXO)).resultado());
        for (int i = 0; i < 60 && !m.isAterrissando(); i++) {
            ticks(1);                                          // 5P.1 (pedido): janela maior
        }
        assertTrue(m.isAterrissando());
        if (reacaoDeQuemCai != null) {
            assertEquals(Resultado.CONCLUIDA, cmd(ini.outro(), rs(reacaoDeQuemCai)).resultado());
        }
        ticks(MotorSolo.ATERRISSAGEM_TICKS + 1);
        ticks(30);                                             // fim das transicoes de postura
    }

    // ---------------- 1-2, 10: derrubar e acompanhar nos dois sentidos
    @Test
    void j1DerrubaJ2EJ2DerrubaJ1AcompanhandoParaOsCemQuilos() {
        for (Lado ini : Lado.values()) {
            novo(300, 380);
            derrubarPelasPernas(ini, null);
            assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao());
            assertEquals(ini, m.getEstado().getPorCima().orElseThrow());
            assertEquals(ini, m.getEstado().getControlador().orElseThrow());
            assertTrue(l(ini.outro()).isDeitado() && l(ini).isAjoelhado());
            assertTrue(m.conectado(a) && a.isVinculadoNoSolo() && b.isVinculadoNoSolo());
        }
    }

    // ---------------- 11: quem cai fecha a guarda (ou pega a meia) na aterrissagem
    @Test
    void quemCaiRecuperaGuardaOuMeiaNaAterrissagem() {
        novo(300, 380);
        derrubarPelasPernas(Lado.A, Direcao.CIMA);
        assertEquals(Posicao.GUARDA, m.getPosicao());
        assertEquals(Lado.A, m.getEstado().getPorCima().orElseThrow());
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow(), "por cima, mas quem controla e a guarda");
        novo(300, 380);
        derrubarPelasPernas(Lado.B, Direcao.ESQUERDA);
        assertEquals(Posicao.MEIA_GUARDA, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow());
    }

    // ---------------- 7: entrada nas pernas parada pelo sprawl (nao universal)
    @Test
    void sprawlParaAEntradaNasPernas() {
        novo(300, 380);
        cmd(Lado.A, rb(Direcao.BAIXO));
        ticks(3);
        b.iniciarSprawl();
        ticks(2);
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow(), "quem fez o sprawl fica por cima");
        assertTrue(a.isAjoelhado(), "quem entrou fica de joelhos");
        assertTrue(m.exposicaoDe(Lado.A).isPresent(), "e exposto");
        assertFalse(m.conectado(a), "sem vinculo: o de pe pode se afastar ou entrar");
    }

    // ---------------- 5-6: empurrao desequilibra sem derrubar; excessivo expoe quem empurra
    @Test
    void empurraoDesequilibraEOExcessivoExpoeOIniciador() {
        novo(300, 370);
        int x = b.getX();
        cmd(Lado.A, rb(Direcao.CIMA));
        resolver();                                            // 5P.1 (pedido)
        assertTrue(b.getX() > x, "recuou");
        assertTrue(m.getBase(Lado.B) < 60 && m.getBase(Lado.B) > 0, "desequilibrou: base " + m.getBase(Lado.B));
        assertEquals(Posicao.EM_PE_SEPARADOS, m.getPosicao(), "nao caiu");
        // empurrar sem parar: a base de quem empurra acaba e ele fica exposto
        for (int i = 0; i < 4 && m.exposicaoDe(Lado.A).isEmpty(); i++) {
            ticks(MotorSolo.RECARGA_TICKS);
            b.encostarNaBorda(a.getX() + 60 - b.getX());
            cmd(Lado.A, rb(Direcao.CIMA));
            resolver();                                        // 5P.1 (pedido)
        }
        assertTrue(m.getBase(Lado.A) < MotorSolo.BASE_MINIMA_PARA_EMPURRAR || m.getPosicao() != Posicao.EM_PE_SEPARADOS,
                "insistir gasta a propria base");
    }

    @Test
    void empurraoContraGuardaEBloqueadoEAvancoExcessivoEPunido() {
        novo(300, 370);
        b.getDefesa().manter(DefesaCorporal.Tipo.GUARDA_MEDIA);
        for (int i = 0; i < DefesaCorporal.TICKS_ENTRADA + 1; i++) {
            b.getDefesa().manter(DefesaCorporal.Tipo.GUARDA_MEDIA);
            b.atualizar(false, false);
        }
        cmd(Lado.A, rb(Direcao.CIMA));
        for (int i = 0; i < 120 && m.getTentativa().isPresent(); i++) {   // 5P.1 (pedido): janela maior
            b.getDefesa().manter(DefesaCorporal.Tipo.GUARDA_MEDIA);
            ticks(1);
        }
        assertEquals(TecnicaSolo.Exposicao.AVANCO, m.exposicaoDe(Lado.A).orElseThrow(), "bloqueado: avanço exposto");
        b.getDefesa().interromper();
        ticks(1);
        cmd(Lado.B, rb(Direcao.CIMA));                         // o outro aproveita
        resolver();                                            // 5P.1 (pedido)
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao(), "quem avançou demais cai");
        assertTrue(a.isDeitado());
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow());
    }

    // ---------------- 8: puxar guarda (e recusar a guarda recuando)
    @Test
    void puxarGuardaOuRecusarRecuando() {
        novo(300, 360);
        cmd(Lado.B, rb(Direcao.ESQUERDA));
        ticks(10);
        ticks(30);
        assertEquals(Posicao.GUARDA, m.getPosicao());
        assertEquals(Lado.A, m.getEstado().getPorCima().orElseThrow(), "quem puxou fica por baixo");
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow(), "e controla (guarda fechada)");

        novo(300, 360);
        cmd(Lado.B, rb(Direcao.ESQUERDA));
        ia = new Intencao(-1, 0);                               // A recua (para longe do B)
        resolver();                                            // 5P.1 (pedido): recua a janela toda
        ia = Intencao.NENHUMA;
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao(), "o outro ficou em pe");
        assertTrue(b.isDeitado() && !a.isDeitado());
    }

    // ---------------- 12-14: passagem com janela; raspagem troca os papeis
    @Test
    void passarAGuardaAbreJanelaParaARaspagemQueTrocaOsPapeis() {
        novo(300, 380);
        m.forcarPosicao(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        ticks(30);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, rs(Direcao.CIMA)).resultado(), "A tenta passar");
        ticks(5);
        assertEquals(Resultado.CONTRA, cmd(Lado.B, rs(Direcao.CIMA)).resultado(), "B raspa na janela");
        ticks(30);
        assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow(), "B agora por cima");
        assertEquals(Lado.A, m.getEstado().getControlador().orElseThrow(), "A, por baixo, controla a guarda");
        assertTrue(b.isAjoelhado() && a.isDeitado(), "posturas trocadas");
        // sem resposta, a passagem entra
        ticks(MotorSolo.RECARGA_TICKS);
        cmd(Lado.B, rs(Direcao.CIMA));
        resolver();                                            // 5P.1 (pedido)
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow());
    }

    @Test
    void raspagemContraPosturaFalhaSemExposicao() {
        novo(300, 380);
        m.forcarPosicao(Posicao.GUARDA, Lado.A, Lado.B);
        ticks(30);
        cmd(Lado.A, rs(Direcao.BAIXO));                         // postura
        ticks(2);
        cmd(Lado.B, rs(Direcao.CIMA));                          // raspagem
        resolver();                                             // 5P.1 (pedido)
        assertEquals(Posicao.GUARDA, m.getPosicao(), "postura segura a raspagem");
        assertEquals(Lado.A, m.getEstado().getPorCima().orElseThrow());
        // quebrar a postura expoe e a raspagem entra
        ticks(MotorSolo.RECARGA_FALHA_TICKS + 1);
        cmd(Lado.B, rs(Direcao.BAIXO));
        resolver();                                             // 5P.1 (pedido)
        ticks(MotorSolo.RECARGA_TICKS);
        cmd(Lado.B, rs(Direcao.CIMA));
        resolver();
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow(), "raspou com a postura quebrada");
    }

    // ---------------- 15-19: cem quilos -> montada (janela de escape); escapes bons e ruins
    @Test
    void montadaAbreJanelaDeEscape() {
        novo(300, 380);
        m.forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        ticks(30);
        cmd(Lado.A, rs(Direcao.CIMA));
        ticks(4);
        assertEquals(TecnicaSolo.Exposicao.CONTROLE_REDUZIDO, m.exposicaoDe(Lado.A).orElseThrow());
        assertEquals(Resultado.CONTRA, cmd(Lado.B, rs(Direcao.DIREITA)).resultado(), "escape de quadril na janela");
        ticks(30);
        assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao(), "recuperou a guarda");
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow());

        novo(300, 380);
        m.forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.B, Lado.B);
        ticks(30);
        cmd(Lado.B, rs(Direcao.CIMA));
        resolver();                                             // 5P.1 (pedido)
        assertEquals(Posicao.MONTADA, m.getPosicao(), "sem reacao: montou");
    }

    @Test
    void escapeMalFeitoNaoGaranteNadaEBemFeitoFunciona() {
        novo(300, 380);
        m.forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        ticks(30);                                             // fim da animacao das posturas
        m.definirEstabilidade(90);
        cmd(Lado.B, rs(Direcao.ESQUERDA));
        resolver();                                             // 5P.1 (pedido)
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao(), "controle firme: o escape nao entra");
        assertTrue(m.recargaDe(Lado.B) > MotorSolo.RECARGA_TICKS, "e custa caro");
        // enquadrar (criar espaco) baixa o controle e o escape entra
        ticks(MotorSolo.RECARGA_FALHA_TICKS);
        for (int i = 0; i < 3; i++) {
            cmd(Lado.B, rs(Direcao.BAIXO));
            resolver();                                         // 5P.1 (pedido)
            ticks(MotorSolo.RECARGA_TICKS);
        }
        assertTrue(m.getEstabilidade() < 50, "estab " + m.getEstabilidade());
        cmd(Lado.B, rs(Direcao.ESQUERDA));
        resolver();
        assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao());
    }

    @Test
    void montadaEstabilizadaEDisputada() {
        novo(300, 380);
        m.forcarPosicao(Posicao.MONTADA, Lado.A, Lado.A);
        ticks(30);
        cmd(Lado.A, rs(Direcao.BAIXO));                         // estabilizar
        resolver();                                             // 5P.1 (pedido)
        ticks(MotorSolo.RECARGA_TICKS);
        assertTrue(m.getEstabilidade() >= 70);
        cmd(Lado.B, rb(Direcao.CIMA));                          // upa contra base firme
        resolver();
        assertEquals(Posicao.MONTADA, m.getPosicao(), "upa defendida");
        ticks(MotorSolo.RECARGA_FALHA_TICKS);
        cmd(Lado.A, rs(Direcao.CIMA));                          // montada alta: base alta exposta
        ticks(2);
        cmd(Lado.B, rb(Direcao.CIMA));                          // upa aproveita
        resolver();
        assertEquals(Posicao.GUARDA, m.getPosicao(), "upa: inverteu");
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow(), "quem deu a upa caiu por cima");
        assertEquals(Lado.A, m.getEstado().getControlador().orElseThrow(), "dentro da guarda do outro");
    }

    @Test
    void costasPelaMontadaEEscapeGirando() {
        novo(300, 380);
        m.forcarPosicao(Posicao.MONTADA, Lado.B, Lado.B);
        ticks(30);
        cmd(Lado.B, rs(Direcao.ESQUERDA));
        resolver();                                             // 5P.1 (pedido)
        assertEquals(Posicao.COSTAS, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow());
        assertEquals(m.orientacao(a), m.orientacao(b), "nas costas os dois olham para o mesmo lado");
        ticks(30);
        m.definirEstabilidade(20);
        cmd(Lado.A, rs(Direcao.DIREITA));
        resolver();
        assertEquals(Posicao.GUARDA, m.getPosicao(), "girou para dentro da guarda");
        assertEquals(Lado.A, m.getEstado().getPorCima().orElseThrow());
    }

    // ---------------- 23: comandos no mesmo tick
    @Test
    void comandosNoMesmoTickAContraVence() {
        novo(300, 380);
        m.forcarPosicao(Posicao.SOLO_DISPUTADO, null, null);
        ticks(30);
        Map<Lado, MotorSolo.Resposta> r = m.comandos(Map.of(Lado.A, rs(Direcao.CIMA), Lado.B, rs(Direcao.BAIXO)));
        assertEquals(Resultado.INICIADA, r.get(Lado.A).resultado());
        assertEquals(Resultado.CONTRA, r.get(Lado.B).resultado(), "puxar para a guarda responde a subir por cima");
        ticks(30);
        assertEquals(Posicao.GUARDA, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow());

        novo(300, 380);
        m.forcarPosicao(Posicao.SOLO_DISPUTADO, null, null);
        ticks(30);
        r = m.comandos(Map.of(Lado.A, rs(Direcao.CIMA), Lado.B, rs(Direcao.CIMA)));
        assertEquals(Resultado.RECUSADA, r.get(Lado.A).resultado(), "os dois subindo: choque");
        assertEquals(Resultado.RECUSADA, r.get(Lado.B).resultado());
    }

    // ---------------- 24: folego (custos; sem recuperar no vinculo; volta ao separar)
    @Test
    void folegoCustaENaoRecuperaNoVinculo() {
        novo(300, 380);
        m.forcarPosicao(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        ticks(30);
        a.gastarFolegoEsforco(10, "teste");
        b.gastarFolegoEsforco(10, "teste");
        double fa = a.getFolegoAtual();
        double fb = b.getFolegoAtual();
        ticks(120);
        assertTrue(a.getFolegoAtual() <= fa + 1e-9 && b.getFolegoAtual() <= fb + 1e-9, "vinculados nao recuperam");
        double antes = a.getFolegoAtual();
        cmd(Lado.A, rs(Direcao.CIMA));
        // 5P.1 (pedido): custos do solo reduzidos e proporcionais ao folego maximo
        assertEquals(antes - com.irmaosmiau.entities.CustosFolego.custoSolo(4.0, a.getAtributos().getFolego()),
                a.getFolegoAtual(), 1e-6, "passar a guarda custa a base 4 reduzida");
        assertTrue(antes - a.getFolegoAtual() < 4.0, "mais barato que no 5P");
        // separar: volta a recuperar
        m.forcarPosicao(Posicao.EM_PE_SEPARADOS, null, null);
        ticks(40);
        double f = a.getFolegoAtual();
        ticks(60);
        assertTrue(a.getFolegoAtual() > f, "livre recupera");
    }

    // ---------------- 25: reiniciar limpa todos os vinculos
    @Test
    void reiniciarLimpaTudo() {
        novo(300, 380);
        m.forcarPosicao(Posicao.MONTADA, Lado.A, Lado.A);
        cmd(Lado.A, rs(Direcao.ESQUERDA));
        m.reiniciar();
        assertEquals(Posicao.EM_PE_SEPARADOS, m.getPosicao());
        assertTrue(m.getTentativa().isEmpty());
        assertFalse(a.isVinculadoNoSolo() || b.isVinculadoNoSolo());
    }

    // ---------------- sequencia completa nos dois sentidos
    @Test
    void sequenciaCompletaNosDoisSentidos() {
        for (Lado x : Lado.values()) {
            Lado y = x.outro();
            novo(300, 380);
            // em pe -> queda (x derruba y), y fecha a guarda na aterrissagem
            derrubarPelasPernas(x, Direcao.CIMA);
            assertEquals(Posicao.GUARDA, m.getPosicao());
            // x abre a guarda (expondo a base) e y raspa na janela
            ticks(MotorSolo.RECARGA_TICKS);
            assertEquals(Resultado.INICIADA, cmd(x, rs(Direcao.CIMA)).resultado());
            ticks(3);
            assertEquals(Resultado.CONTRA, cmd(y, rs(Direcao.CIMA)).resultado());
            ticks(30);
            assertEquals(y, m.getEstado().getPorCima().orElseThrow(), "raspagem: y por cima");
            // y passa para os cem quilos
            ticks(MotorSolo.RECARGA_TICKS);
            cmd(y, rs(Direcao.CIMA));
            resolver();                                         // 5P.1 (pedido)
            assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao());
            // y tenta a montada; x escapa na janela e recupera a guarda
            ticks(MotorSolo.RECARGA_TICKS + 30);
            cmd(y, rs(Direcao.CIMA));
            ticks(3);
            assertEquals(Resultado.CONTRA, cmd(x, rs(Direcao.ESQUERDA)).resultado());
            ticks(30);
            assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao());
            assertEquals(x, m.getEstado().getControlador().orElseThrow(), "x recuperou a guarda");
            assertEquals(y, m.getEstado().getPorCima().orElseThrow());
            assertTrue(m.eventos().size() > 6);
        }
    }

    // ---------------- resultado E: queda sem controle consolidado
    @Test
    void pernasContraBaseSemSprawlViraSoloDisputado() {
        novo(300, 380);
        cmd(Lado.A, rb(Direcao.BAIXO));
        ib = new Intencao(0, 0, true);                         // B faz base (RT), sem sprawl
        ticks(14);
        ib = Intencao.NENHUMA;
        ticks(30);
        assertEquals(Posicao.SOLO_DISPUTADO, m.getPosicao());
        assertTrue(m.getEstado().getControlador().isEmpty() && m.getEstado().getPorCima().isEmpty());
        assertTrue(a.isAjoelhado() && b.isAjoelhado());
    }

    // ---------------- 20: toda posicao de solo tem respostas para os dois papeis
    @Test
    void todaPosicaoTemRespostasParaOsDoisLados() {
        CatalogoTecnicasSolo c = CatalogoTecnicasSolo.referencia();
        Object[][] casos = {{Posicao.GUARDA, Lado.A, Lado.B}, {Posicao.GUARDA_ABERTA, Lado.A, Lado.B},
            {Posicao.MEIA_GUARDA, Lado.A, Lado.B}, {Posicao.CONTROLE_LATERAL, Lado.A, Lado.A},
            {Posicao.JOELHO_BARRIGA, Lado.A, Lado.A}, {Posicao.MONTADA, Lado.A, Lado.A},
            {Posicao.COSTAS, Lado.A, Lado.A}, {Posicao.EM_PE_CONTRA_SOLO, Lado.A, null},
            {Posicao.SOLO_DISPUTADO, null, null}};
        for (Object[] k : casos) {
            EstadoPosicional e = EstadoPosicional.noChao((Posicao) k[0], (Lado) k[1], (Lado) k[2]);
            for (Lado l : Lado.values()) {
                assertFalse(c.repertorio(e, l).isEmpty(), k[0] + " sem resposta para " + l);
                // nenhum comando aciona duas tecnicas
                for (Direcao d : Direcao.values()) {
                    for (boolean x : new boolean[] {false, true}) {
                        ComandoSolo cmd = new ComandoSolo(d, x);
                        long n = c.repertorio(e, l).stream().filter(t -> t.aceita(cmd)).count();
                        assertTrue(n <= 1, k[0] + " " + l + " " + cmd + ": " + n + " tecnicas");
                    }
                }
            }
        }
        for (TecnicaSolo t : c.todas()) {
            for (String contra : t.contras()) {
                assertTrue(c.buscar(contra).isPresent(), t.id() + " tem contra inexistente " + contra);
            }
            assertTrue(t.custo() > 0, t.id());
        }
    }
}
