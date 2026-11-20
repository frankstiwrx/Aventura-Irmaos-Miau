package com.irmaosmiau.posicional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.CustosFolego;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.posicional.MotorSolo.Intencao;
import com.irmaosmiau.posicional.MotorSolo.Resultado;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5P.2 no MotorSolo isolado: familias (RS, R1, L1, L2, L1+L2), espacos
 * de finalizacao reservados, solo livre detalhado, guardas novas (borboleta, De
 * La Riva, laco), desengajar/levantar, condicionamento e eventos de luta.
 */
class MotorSolo5P2Test {

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
        for (int i = 0; i < 150 && m.getTentativa().isPresent(); i++) {
            ticks(1);
        }
        assertTrue(m.getTentativa().isEmpty(), "resolveu");
        ticks(1);
    }

    private static ComandoSolo c(Direcao d, Familia f) {
        return new ComandoSolo(d, f);
    }

    private MotorSolo.Resposta cmd(Lado l, Direcao d, Familia f) {
        return m.comando(l, c(d, f));
    }

    /** Posicao forcada com as posturas assentadas. */
    private void em(Posicao p, Lado cima, Lado ctrl) {
        m.forcarPosicao(p, cima, ctrl);
        ticks(30);
    }

    // ================= 1-3. FAMILIAS E RESOLUCAO
    @Test
    void comandoAntigoContinuaSendoRsOuR1() {
        assertEquals(Familia.R1, new ComandoSolo(Direcao.CIMA, true).familia());
        assertEquals(Familia.RS, new ComandoSolo(Direcao.CIMA, false).familia());
        assertTrue(new ComandoSolo(Direcao.CIMA, Familia.R1).explosivo());
        assertFalse(new ComandoSolo(Direcao.CIMA, Familia.L1).explosivo());
    }

    @Test
    void cadaComandoAcionaNoMaximoUmaAcaoEmQualquerContexto() {
        CatalogoTecnicasSolo cat = CatalogoTecnicasSolo.referencia();
        RegistroFinalizacoes fin = RegistroFinalizacoes.referencia();
        for (Posicao p : Posicao.values()) {
            for (Lado cima : new Lado[] {null, Lado.A, Lado.B}) {
                for (Lado ctrl : new Lado[] {null, Lado.A, Lado.B}) {
                    EstadoPosicional e;
                    try {
                        e = p == Posicao.EM_PE_SEPARADOS ? EstadoPosicional.separados()
                                : p == Posicao.EM_PE_PEGADA ? EstadoPosicional.pegada(ctrl)
                                : EstadoPosicional.noChao(p, cima, ctrl);
                    } catch (IllegalArgumentException invalido) {
                        continue;                              // combinacao que nao existe
                    }
                    for (Lado lado : Lado.values()) {
                        for (Familia f : Familia.values()) {
                            for (Direcao d : Direcao.values()) {
                                ComandoSolo cs = c(d, f);
                                long n = cat.repertorio(e, lado).stream().filter(t -> t.aceita(cs)).count();
                                // 5Q.6 (pedido): em pe sem pegada o LT tem uma roda EM PE e outra DE JOELHOS
                                // (o motor escolhe pela postura): conta por postura
                                for (boolean joelhos : new boolean[] {false, true}) {
                                    long np = cat.repertorio(e, lado).stream().filter(t -> t.aceita(cs))
                                            .filter(t -> cat.posturaExigida(t.id()).map(x -> (x
                                                    == CatalogoTecnicasSolo.PosturaExigida.DE_JOELHOS) == joelhos)
                                                    .orElse(true)).count();
                                    assertTrue(np <= 1, p + " " + lado + " " + cs + " joelhos=" + joelhos + ": " + np);
                                }
                                n = Math.min(n, 1);
                                // 5Q (pedido): o espaco aponta para UMA tecnica: a do catalogo de solo (entrada na
                                // perna) ou a finalizacao; nunca as duas
                                java.util.Optional<RegistroFinalizacoes.Espaco> es = fin.para(e, lado, cs);
                                long nf = CatalogoFinalizacoes.referencia().para(e, lado, cs).isPresent() ? 1 : 0;
                                assertTrue(n + nf <= 1, "duas acoes em " + p + " " + lado + " " + cs);
                                if (es.isPresent() && n == 1) {
                                    assertEquals(es.get().idTecnica(), cat.repertorio(e, lado).stream()
                                            .filter(t -> t.aceita(cs)).findFirst().orElseThrow().id());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void l1MaisL2NaoAcionaAsFamiliasIndividuais() {
        novo(300, 380);
        em(Posicao.DE_LA_RIVA, Lado.A, Lado.B);
        CatalogoTecnicasSolo cat = m.getCatalogo();
        assertEquals(CatalogoTecnicasSolo.BERIMBOLO, cat.para(m.getEstado(), Lado.B, c(Direcao.CIMA, Familia.L1_L2))
                .orElseThrow().id());
        assertEquals(CatalogoTecnicasSolo.IR_ABERTA, cat.para(m.getEstado(), Lado.B, c(Direcao.CIMA, Familia.L1))
                .orElseThrow().id());
        assertTrue(cat.para(m.getEstado(), Lado.B, c(Direcao.CIMA, Familia.L2)).isEmpty());
        assertEquals(CatalogoTecnicasSolo.RASPAGEM_DLR, cat.para(m.getEstado(), Lado.B, c(Direcao.CIMA, Familia.RS))
                .orElseThrow().id(), "RS sem modificador continua sendo o fundamento");
    }

    // ================= 8. ESPACOS DE FINALIZACAO
    @Test
    void dozeEspacosPorPosicaoComIdsEstaveisENenhumExecuta() {
        RegistroFinalizacoes r = RegistroFinalizacoes.referencia();
        assertEquals(48, r.todos().size());
        Set<String> ids = new HashSet<>();
        for (Posicao p : RegistroFinalizacoes.POSICOES.keySet()) {
            List<RegistroFinalizacoes.Espaco> es = r.daPosicao(p);
            assertEquals(12, es.size(), p.name());
            for (RegistroFinalizacoes.Espaco e : es) {
                assertTrue(ids.add(e.id()), "id repetido " + e.id());
                // 5Q (pedido): os 48 espacos agora apontam para tecnicas reais
                assertTrue(e.implementada(), "implementado na 5Q: " + e.id());
                assertEquals(RegistroFinalizacoes.id(e.posicao(), e.familia(), e.direcao()), e.id());
            }
        }
        assertEquals("fin.montada.l2.cima", RegistroFinalizacoes.id(Posicao.MONTADA, Familia.L2, Direcao.CIMA));
        assertEquals("fin.cem_quilos.l1l2.baixo",
                RegistroFinalizacoes.id(Posicao.CONTROLE_LATERAL, Familia.L1_L2, Direcao.BAIXO));
        assertEquals("Mata-leão", r.buscar("fin.costas.l2.cima").orElseThrow().planejada());
        // registro futuro de uma tecnica real (sem mexer no original)
        RegistroFinalizacoes r2 = r.comTecnica("fin.costas.l2.cima", "mata_leao_variante");
        assertEquals("mata_leao_variante", r2.buscar("fin.costas.l2.cima").orElseThrow().idTecnica());
        assertEquals("mata_leao@costas", r.buscar("fin.costas.l2.cima").orElseThrow().idTecnica(), "o original nao muda");
    }

    @Test
    void comandoDeFinalizacaoNaMontadaAgoraERealSoParaQuemMonta() {
        // 5Q (pedido): antes "FINALIZACAO FUTURA"; agora L1/L2 comecam finalizacoes reais e
        // L1+L2 e a entrada no controle de perna. Quem esta por baixo continua sem esses ataques.
        for (Lado x : Lado.values()) {
            novo(300, 380);
            em(Posicao.MONTADA, x, x);
            m.definirEstabilidade(80);
            for (Familia fam : RegistroFinalizacoes.FAMILIAS) {
                for (Direcao d : Direcao.values()) {
                    assertTrue(m.getCatalogo().para(m.getEstado(), x, c(d, fam)).isPresent()
                            || m.getCatalogoFinalizacoes().para(m.getEstado(), x, c(d, fam)).isPresent(), fam + " " + d);
                }
            }
            assertEquals(Resultado.INICIADA, cmd(x, Direcao.DIREITA, Familia.L1).resultado(), "americana");
            assertTrue(m.getAtaque().isPresent());
        }
        novo(300, 380);
        em(Posicao.MONTADA, Lado.A, Lado.A);
        assertEquals("NADA NESTA POSIÇÃO", cmd(Lado.B, Direcao.CIMA, Familia.L2).motivo());
    }

    // ================= 7. SOLO LIVRE
    @Test
    void aproximarNaoConsolidaNadaEJoelhoDiretoPrecisaDeChance() {
        novo(300, 380);
        b.alternarDeitado();
        ticks(30);
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao());
        ia = new Intencao(1, 0);
        ticks(20);                                             // A anda ate o B
        ia = Intencao.NENHUMA;
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao(), "aproximar nao vira cem quilos");
        assertTrue(m.getEstado().getControlador().isEmpty());
        // 5Q.5 (pedido): o joelho direto ENTRA se o outro nao reagir (antes exigia o outro ja exposto e o RB v
        // direto nunca funcionava). A chance continua sendo disputada: a retencao com os pes na janela defende.
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.BAIXO, Familia.R1).resultado());
        assertEquals(Resultado.CONTRA, cmd(Lado.B, Direcao.BAIXO, Familia.RS).resultado(), "pes no quadril");
        resolver();
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao(), "joelho direto com resposta nao entra");
        ticks(MotorSolo.RECARGA_FALHA_TICKS + 1);
        // prender as pernas (o de baixo nao reage) e entao o joelho entra
        // 5Q.7 (pedido): o PRENDER ficou so no RS <- (o RS -> virou SEGURAR AS PERNAS)
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.ESQUERDA, Familia.RS).resultado());
        resolver();
        assertEquals(TecnicaSolo.Exposicao.PERNAS_PRESAS, m.exposicaoDe(Lado.B).orElseThrow());
        ticks(MotorSolo.RECARGA_TICKS);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.BAIXO, Familia.R1).resultado());
        resolver();
        assertEquals(Posicao.JOELHO_BARRIGA, m.getPosicao());
        assertEquals(Lado.A, m.getEstado().getControlador().orElseThrow());
    }

    @Test
    void pernasPresasImpedemARetencaoComOsPes() {
        novo(300, 380);
        b.alternarDeitado();
        ticks(30);
        cmd(Lado.A, Direcao.ESQUERDA, Familia.RS);
        resolver();
        ticks(MotorSolo.RECARGA_TICKS);
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.R1).resultado(), "passagem em pe");
        MotorSolo.Resposta r = cmd(Lado.B, Direcao.BAIXO, Familia.RS);  // retencao com os pes
        assertEquals("PERNAS PRESAS", r.motivo());
        resolver();
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao(), "a passagem entra (so depois de prender)");
    }

    @Test
    void quemEstaNoChaoConectaGuardasDePertoERecuarAfasta() {
        novo(100, 600);
        b.alternarDeitado();
        ticks(30);
        assertEquals("LONGE", cmd(Lado.B, Direcao.DIREITA, Familia.L1).motivo(), "de longe nada conecta");
        assertEquals("LONGE", cmd(Lado.A, Direcao.ESQUERDA, Familia.RS).motivo(), "nem prende as pernas");
        novo(300, 380);
        b.alternarDeitado();
        ticks(30);
        int d0 = Math.abs(a.getX() - b.getX());
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.A, Direcao.BAIXO, Familia.RS).resultado(), "recuar");
        assertTrue(Math.abs(a.getX() - b.getX()) > d0, "recuou");
        ticks(MotorSolo.RECARGA_TICKS);
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao());
        novo(300, 370);
        b.alternarDeitado();
        ticks(30);
        assertEquals(Resultado.INICIADA, cmd(Lado.B, Direcao.DIREITA, Familia.L1).resultado(), "gancho De La Riva");
        resolver();
        assertEquals(Posicao.DE_LA_RIVA, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow());
        ticks(30);
        assertTrue(!a.isDeitado() && !a.isAjoelhado(), "na De La Riva quem esta por cima fica em pe");
        assertTrue(b.isDeitado());
        assertTrue(a.isVinculadoNoSolo() && b.isVinculadoNoSolo());
    }

    // ================= 5. GUARDAS NOVAS
    @Test
    void lacoExigeAMangaESoltarAMangaDesfazOLaco() {
        novo(300, 380);
        em(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        assertEquals("SEM A MANGA", cmd(Lado.B, Direcao.BAIXO, Familia.L1).motivo(), "laco sem contato: nao");
        assertEquals(Resultado.CONCLUIDA, cmd(Lado.B, Direcao.BAIXO, Familia.L2).resultado(), "pegar a manga");
        assertTrue(m.temConexao(Lado.B, MotorSolo.Conexao.MANGA));
        ticks(MotorSolo.RECARGA_TICKS);
        assertEquals(Resultado.INICIADA, cmd(Lado.B, Direcao.BAIXO, Familia.L1).resultado());
        resolver();
        assertEquals(Posicao.GUARDA_LACO, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow(), "os dois atualizados: B controla");
        assertEquals(Lado.A, m.getEstado().getPorCima().orElseThrow());
        ticks(MotorSolo.RECARGA_TICKS);
        cmd(Lado.A, Direcao.DIREITA, Familia.RS);              // soltar a manga
        resolver();
        assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao());
        assertFalse(m.temConexao(Lado.B, MotorSolo.Conexao.MANGA), "a manga foi solta");
    }

    @Test
    void sairDasGuardasSoltaAManga() {
        novo(300, 380);
        em(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        m.adicionarConexao(Lado.B, MotorSolo.Conexao.MANGA);
        m.forcarPosicao(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        assertFalse(m.temConexao(Lado.B, MotorSolo.Conexao.MANGA));
    }

    @Test
    void berimboloPrecisaDeControleEPodeSerParado() {
        novo(300, 380);
        em(Posicao.DE_LA_RIVA, Lado.A, Lado.B);
        m.definirEstabilidade(80);
        assertEquals(Resultado.INICIADA, cmd(Lado.B, Direcao.CIMA, Familia.L1_L2).resultado());
        assertEquals(TecnicaSolo.Exposicao.BASE, m.exposicaoDe(Lado.B).orElseThrow(), "exposto na inversao");
        resolver();
        assertEquals(Posicao.COSTAS, m.getPosicao());
        assertEquals(Lado.B, m.getEstado().getControlador().orElseThrow());

        novo(300, 380);
        em(Posicao.DE_LA_RIVA, Lado.A, Lado.B);
        m.definirEstabilidade(80);
        cmd(Lado.B, Direcao.CIMA, Familia.L1_L2);
        ticks(3);
        assertEquals(Resultado.CONTRA, cmd(Lado.A, Direcao.BAIXO, Familia.RS).resultado(), "base para o berimbolo");
        resolver();
        assertEquals(Posicao.DE_LA_RIVA, m.getPosicao());

        novo(300, 380);
        em(Posicao.DE_LA_RIVA, Lado.A, Lado.B);
        m.definirEstabilidade(20);
        cmd(Lado.B, Direcao.CIMA, Familia.L1_L2);
        resolver();
        assertEquals(Posicao.DE_LA_RIVA, m.getPosicao(), "sem controle o berimbolo nao sai");
    }

    @Test
    void borboletaRaspaEPassagemDependeDoControle() {
        novo(300, 380);
        em(Posicao.GUARDA_BORBOLETA, Lado.A, Lado.B);
        cmd(Lado.B, Direcao.CIMA, Familia.RS);                 // elevacao
        resolver();
        assertEquals(Lado.B, m.getEstado().getPorCima().orElseThrow(), "raspou: B por cima");
        assertEquals(Posicao.GUARDA_ABERTA, m.getPosicao());

        novo(300, 380);
        em(Posicao.GUARDA_BORBOLETA, Lado.A, Lado.B);
        m.definirEstabilidade(90);
        cmd(Lado.A, Direcao.CIMA, Familia.RS);
        resolver();
        assertEquals(Posicao.GUARDA_BORBOLETA, m.getPosicao(), "ganchos firmes seguram a passagem");
        ticks(MotorSolo.RECARGA_FALHA_TICKS + 1);
        m.definirEstabilidade(20);
        cmd(Lado.A, Direcao.CIMA, Familia.RS);
        resolver();
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao());
    }

    @Test
    void trocarDeGuardaExpoeAPassagem() {
        novo(300, 380);
        em(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        assertEquals(Resultado.INICIADA, cmd(Lado.B, Direcao.ESQUERDA, Familia.L1).resultado(), "para a borboleta");
        assertEquals(Resultado.CONTRA, cmd(Lado.A, Direcao.CIMA, Familia.RS).resultado(), "passa na troca");
        assertEquals(Posicao.CONTROLE_LATERAL, m.getPosicao());
    }

    // ================= emenda: DESENGAJAR E LEVANTAR
    @Test
    void quemEstaPorBaixoAbreAfastaELevantaNosDoisSentidos() {
        for (Lado baixo : Lado.values()) {
            Lado cima = baixo.outro();
            novo(300, 380);
            em(Posicao.GUARDA, cima, baixo);
            assertEquals(Resultado.INICIADA, cmd(baixo, Direcao.BAIXO, Familia.R1).resultado(), "abrir e afastar");
            resolver();
            assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao());
            assertFalse(a.isVinculadoNoSolo() || b.isVinculadoNoSolo(), "vinculo encerrado");
            assertTrue(m.getEstado().getControlador().isEmpty());
            ticks(30);
            assertEquals(Resultado.INICIADA, cmd(baixo, Direcao.CIMA, Familia.RS).resultado(), "levantar");
            resolver();
            assertEquals(Posicao.EM_PE_SEPARADOS, m.getPosicao(), "de volta em pe");
            ticks(30);
            int x = l(baixo).getX();
            l(baixo).moverX(-20);
            assertNotEquals(x, l(baixo).getX(), "movimento independente");
        }
    }

    @Test
    void quemEstaPorCimaNaGuardaPrecisaDeEspacoParaLevantar() {
        novo(300, 380);
        em(Posicao.GUARDA, Lado.A, Lado.B);
        m.definirEstabilidade(90);
        // 5Q.6 (pedido): na guarda FECHADA firme quem esta por cima precisa de postura antes de tentar
        // levantar (antes comecava e era segurado no fim da janela); o resultado (a guarda segura) e o mesmo
        assertEquals("FAÇA POSTURA ANTES (RS↓)", cmd(Lado.A, Direcao.BAIXO, Familia.R1).motivo());
        assertEquals(Posicao.GUARDA, m.getPosicao(), "a guarda firme segura");
        ticks(MotorSolo.RECARGA_FALHA_TICKS + 1);
        m.definirEstabilidade(20);
        cmd(Lado.A, Direcao.BAIXO, Familia.R1);
        ticks(2);
        assertEquals(Resultado.CONTRA, cmd(Lado.B, Direcao.BAIXO, Familia.RS).resultado(), "quebrar a postura: segura");
        resolver();
        assertEquals(Posicao.GUARDA, m.getPosicao(), "resistiu a separacao");
        ticks(MotorSolo.RECARGA_TICKS + 1);
        m.definirEstabilidade(20);
        cmd(Lado.A, Direcao.BAIXO, Familia.R1);
        resolver();
        assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao(), "com espaco levantou");
        assertTrue(!a.isDeitado() && !a.isAjoelhado());
    }

    @Test
    void quemDominaSoltaFacilQuemEstaDominadoPrecisaEscapar() {
        for (Lado dom : Lado.values()) {
            novo(300, 380);
            em(Posicao.MONTADA, dom, dom);
            assertEquals("NADA NESTA POSIÇÃO", cmd(dom.outro(), Direcao.BAIXO, Familia.R1).motivo(),
                    "dominado nao sai sem escapar");
            m.definirEstabilidade(100);
            assertEquals(Resultado.INICIADA, cmd(dom, Direcao.BAIXO, Familia.R1).resultado());
            m.consumirEventosLuta();
            resolver();
            assertEquals(Posicao.EM_PE_CONTRA_SOLO, m.getPosicao(), "soltou a posicao mesmo com controle total");
            assertFalse(a.isVinculadoNoSolo() || b.isVinculadoNoSolo());
            assertTrue(m.consumirEventosLuta().stream().anyMatch(r -> r.tipo() == EventoLuta.ABANDONO));
        }
    }

    @Test
    void nenhumaPosicaoFicaTravadaParaNinguem() {
        CatalogoTecnicasSolo cat = CatalogoTecnicasSolo.referencia();
        for (Posicao p : Posicao.values()) {
            if (!p.isVinculoDeSolo()) {
                continue;
            }
            EstadoPosicional e = p == Posicao.SOLO_DISPUTADO ? EstadoPosicional.noChao(p, null, null)
                    : p.isGuarda() ? EstadoPosicional.noChao(p, Lado.A, Lado.B)
                    : EstadoPosicional.noChao(p, Lado.A, Lado.A);
            for (Lado l : Lado.values()) {
                boolean saida = cat.repertorio(e, l).stream()
                        .anyMatch(t -> t.sucesso() != null && t.sucesso().destino() != p);
                assertTrue(saida, p + ": " + l + " sem nenhuma saida/progresso");
            }
        }
    }

    // ================= emenda: CONDICIONAMENTO
    @Test
    void tecnicaGastaCondicionamentoUmaVezSo() {
        novo(300, 380);
        em(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        TecnicaSolo passar = m.getCatalogo().exigir(CatalogoTecnicasSolo.PASSAR_GUARDA);
        double c0 = a.getCondicionamentoAtual();
        double custo = m.custo(Lado.A, passar);
        cmd(Lado.A, Direcao.CIMA, Familia.RS);
        assertEquals(CustosFolego.condicionamentoDaTecnica(custo, false), c0 - a.getCondicionamentoAtual(), 1e-9);
        assertTrue(c0 - a.getCondicionamentoAtual() < (custo) * 0.5, "condicionamento cai mais devagar que o folego");
        TecnicaSolo levantar = m.getCatalogo().exigir(CatalogoTecnicasSolo.LEVANTAR_DA_GUARDA);
        assertTrue(MotorSolo.explosiva(levantar) && !MotorSolo.explosiva(passar));
    }

    @Test
    void quedaCustaImpactoUmaVezEPressaoDesgastaAosPoucos() {
        novo(300, 380);
        double cb = b.getCondicionamentoAtual();
        m.comando(Lado.A, new ComandoSolo(Direcao.BAIXO, true));   // entrada nas pernas
        for (int i = 0; i < 60 && !m.isAterrissando(); i++) {
            ticks(1);
        }
        assertEquals(CustosFolego.impactoQueda(b.getAtributos().getCondicionamento()), cb - b.getCondicionamentoAtual(),
                1e-9, "impacto uma vez");
        // preso na montada com controle total: desgaste gradual
        novo(300, 380);
        em(Posicao.MONTADA, Lado.A, Lado.A);
        m.definirEstabilidade(100);
        double ca = a.getCondicionamentoAtual();
        cb = b.getCondicionamentoAtual();
        ticks(60);
        double perdaB = cb - b.getCondicionamentoAtual();
        double perdaA = ca - a.getCondicionamentoAtual();
        assertTrue(perdaB > 0.05 && perdaB < 0.5, "dominado: gradual " + perdaB);
        assertTrue(perdaA > 0 && perdaA < perdaB, "quem pressiona tambem gasta, menos " + perdaA);
    }

    @Test
    void descansoNaoDesgastaERecuperaDevagar() {
        novo(300, 380);
        b.alternarDeitado();                                   // deitado, livre (solo livre, sem vinculo)
        ticks(30);
        double cb = b.getCondicionamentoAtual();
        ticks(120);
        assertTrue(b.getCondicionamentoAtual() >= cb - 1e-9, "deitado livre nao perde condicionamento");
        a.gastarCondicionamentoEsforco(6, "teste");
        double ca = a.getCondicionamentoAtual();
        ticks(300);
        double ganho = a.getCondicionamentoAtual() - ca;
        assertTrue(ganho > 0, "recupera");
        assertTrue(ganho < 6, "mas devagar: " + ganho);
    }

    @Test
    void exaustaoEGradualCriticaSoIdentificada() {
        novo(300, 380);
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        TecnicaSolo montar = m.getCatalogo().exigir(CatalogoTecnicasSolo.MONTAR);
        TecnicaSolo costas = m.getCatalogo().exigir(CatalogoTecnicasSolo.PEGAR_COSTAS_VIRADA);
        double custoExplosivo = m.custo(Lado.A, costas);
        cmd(Lado.A, Direcao.CIMA, Familia.RS);
        int janelaDescansado = m.getTentativa().orElseThrow().janelaTotal();
        assertEquals(0.0, a.fatorExaustao(), 1e-9);

        novo(300, 380);
        em(Posicao.CONTROLE_LATERAL, Lado.A, Lado.A);
        a.gastarCondicionamentoEsforco(a.getAtributos().getCondicionamento() * 0.97, "teste");
        assertTrue(a.fatorExaustao() > 0.8 && a.isExaustaoCritica());
        assertTrue(m.custo(Lado.A, costas) > custoExplosivo, "explosivas custam mais exausto");
        assertEquals(m.custo(Lado.A, montar), CustosFolego.custoSolo(montar.custo(), a.getAtributos().getFolego()),
                1e-9, "o resto do folego segue as regras da 5P.1");
        ticks(1);
        assertTrue(m.consumirEventosLuta().stream().anyMatch(r -> r.tipo() == EventoLuta.EXAUSTAO_CRITICA));
        assertEquals(Resultado.INICIADA, cmd(Lado.A, Direcao.CIMA, Familia.RS).resultado(), "exausto ainda age");
        assertTrue(m.getTentativa().orElseThrow().janelaTotal() > janelaDescansado, "mas mais devagar");
        ticks(200);
        assertTrue(m.getPosicao().isNoChao(), "a luta nao termina por exaustao nesta entrega");
        assertEquals(0.0, CustosFolego.fatorExaustao(20, 30), 1e-9, "acima de 35%: sem efeito");
        assertEquals(0.5, CustosFolego.fatorExaustao(30 * 0.175, 30), 1e-9);
    }

    // ================= emenda 3: eventos de luta (sem pontos)
    @Test
    void eventosDeLutaClassificados() {
        novo(300, 380);
        em(Posicao.GUARDA_ABERTA, Lado.A, Lado.B);
        m.consumirEventosLuta();
        cmd(Lado.A, Direcao.CIMA, Familia.RS);
        resolver();
        ticks(MotorSolo.RECARGA_TICKS);
        cmd(Lado.A, Direcao.CIMA, Familia.RS);
        resolver();
        List<EventoLuta> tipos = m.consumirEventosLuta().stream().map(EventoLuta.Registro::tipo).toList();
        assertEquals(List.of(EventoLuta.PASSAGEM, EventoLuta.MONTADA), tipos);
        m.reiniciar();
        assertTrue(m.consumirEventosLuta().isEmpty());
    }
}
