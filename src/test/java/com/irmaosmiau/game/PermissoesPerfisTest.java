package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.entities.AparenciaLutador;
import com.irmaosmiau.entities.AparenciaLutador.Parte;
import com.irmaosmiau.entities.AtributosLutador;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.personagens.CatalogoPersonagens;
import com.irmaosmiau.personagens.DefinicaoPersonagem;
import com.irmaosmiau.personagens.EscolhaLutador;
import com.irmaosmiau.personagens.PerfilLutador;
import com.irmaosmiau.posicional.CatalogoFinalizacoes;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.EstadoPosicional;
import com.irmaosmiau.posicional.Finalizacao;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.TecnicaSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.EvolucaoGraduacao;
import com.irmaosmiau.regras.Graduacao;
import com.irmaosmiau.regras.ModoLuta;
import com.irmaosmiau.treino.ConfiguracaoTreino;

import java.awt.Color;

import org.junit.jupiter.api.Test;

/**
 * Perfis personalizados usam as tecnicas da base sem a trava de graduacao (treino e modo livre), sem
 * bonus de atributos; no PONTOS + FINALIZACAO a graduacao logica (branca) continua sendo o regulamento.
 * Tecnicas reais do catalogo: omoplata (azul), chave de punho (azul), gogoplata e tarikoplata (marrom) da
 * guarda fechada, e a entrada das costas para a chave de joelho (marrom).
 */
class PermissoesPerfisTest extends Base5OGamePanel {

    private static final AtributosLutador PROPRIOS = new AtributosLutador(40, 30, 70, 25, 25, 30, 25, 75);
    private static final CatalogoFinalizacoes CATALOGO = CatalogoFinalizacoes.referencia();

    private static DefinicaoPersonagem base(String id) {
        return CatalogoPersonagens.padrao().exigir(id);
    }

    private static EscolhaLutador perfil(String idBase, AparenciaLutador aparencia) {
        return EscolhaLutador.perfil(new PerfilLutador("p_" + idBase, "Perfil " + idBase, idBase, PROPRIOS,
                aparencia), base(idBase));
    }

    private static EscolhaLutador perfil(String idBase) {
        return perfil(idBase, AparenciaLutador.ORIGINAL);
    }

    private static IrmaoMiau original(String idBase) {
        return EscolhaLutador.oficial(base(idBase)).criarLutador(300, 150, 0.7);
    }

    /** A (por baixo) controla a guarda fechada de B: as finalizacoes da guarda sao de A. */
    private static MotorSolo guarda(IrmaoMiau a, IrmaoMiau b) {
        MotorSolo m = new MotorSolo(a, b, null, 15, 715);
        m.forcarPosicao(Posicao.GUARDA, Lado.B, Lado.A);
        return m;
    }

    private static Finalizacao naGuarda(Lado lado, Familia f, Direcao d) {
        return CATALOGO.para(EstadoPosicional.noChao(Posicao.GUARDA, Lado.B, Lado.A), lado,
                new TecnicaSolo.ComandoSolo(d, f)).orElseThrow();
    }

    private static final Finalizacao OMOPLATA = naGuarda(Lado.A, Familia.L1, Direcao.BAIXO);
    private static final Finalizacao PUNHO = naGuarda(Lado.A, Familia.L1, Direcao.ESQUERDA);
    private static final Finalizacao GOGOPLATA = naGuarda(Lado.A, Familia.L1_L2, Direcao.CIMA);
    private static final Finalizacao TARIKOPLATA = naGuarda(Lado.A, Familia.L1_L2, Direcao.BAIXO);

    @Test
    void finalizacoesReaisDoCatalogo() {
        assertEquals("omoplata", OMOPLATA.tecnicaBase());
        assertEquals(Graduacao.AZUL, OMOPLATA.graduacao());
        assertEquals("chave_punho", PUNHO.tecnicaBase());
        assertEquals("gogoplata", GOGOPLATA.tecnicaBase());
        assertEquals(Graduacao.MARROM, GOGOPLATA.graduacao());
        assertEquals("tarikoplata", TARIKOPLATA.tecnicaBase());
        assertEquals(Graduacao.MARROM, TARIKOPLATA.graduacao());
    }

    // 1 e 3. Perfil do Branco: omoplata e gogoplata (antes travadas pela faixa branca) liberadas
    @Test
    void perfilDoBrancoUsaAsTecnicasDoBranco() {
        IrmaoMiau p = perfil(CatalogoPersonagens.ID_BRANCO).criarLutador(300, 150, 0.7);
        MotorSolo m = guarda(p, original(CatalogoPersonagens.ID_PRETO));
        assertTrue(p.isTecnicasDaBaseLiberadas());
        assertNull(m.motivoFinalizacao(Lado.A, OMOPLATA), "omoplata (azul) liberada");
        assertNull(m.motivoFinalizacao(Lado.A, GOGOPLATA), "gogoplata (marrom) liberada");
    }

    // 2 e 3. Perfil do Preto: tarikoplata e omoplata liberadas
    @Test
    void perfilDoPretoUsaAsTecnicasDoPreto() {
        IrmaoMiau p = perfil(CatalogoPersonagens.ID_PRETO).criarLutador(300, 150, 0.7);
        MotorSolo m = guarda(p, original(CatalogoPersonagens.ID_BRANCO));
        assertNull(m.motivoFinalizacao(Lado.A, TARIKOPLATA), "tarikoplata (marrom) liberada");
        assertNull(m.motivoFinalizacao(Lado.A, OMOPLATA));
    }

    // 3. A entrada das costas que termina na chave de joelho (marrom) tambem deixa de ser travada
    @Test
    void entradaDasCostasComChaveMarrom() {
        TecnicaSolo entrada = CatalogoTecnicasSolo.referencia().exigir(CatalogoTecnicasSolo.COSTAS_PERNA_JOELHO);
        IrmaoMiau orig = original(CatalogoPersonagens.ID_BRANCO);
        MotorSolo mo = guarda(orig, original(CatalogoPersonagens.ID_PRETO));
        assertEquals("GRADUAÇÃO MARROM", mo.motivoSem(Lado.A, entrada), "original na branca: travada");
        IrmaoMiau p = perfil(CatalogoPersonagens.ID_BRANCO).criarLutador(300, 150, 0.7);
        MotorSolo mp = guarda(p, original(CatalogoPersonagens.ID_PRETO));
        String motivo = mp.motivoSem(Lado.A, entrada);
        assertFalse(motivo != null && motivo.startsWith("GRADUAÇÃO"), "perfil: a graduacao nao trava (" + motivo + ")");
    }

    // 4. A faixa visual nao muda permissoes (nem para mais, nem para menos)
    @Test
    void faixaVisualNaoMudaPermissoes() {
        Color pretaVisual = new Color(20, 20, 20);
        IrmaoMiau orig = original(CatalogoPersonagens.ID_BRANCO);
        orig.aplicarAparencia(AparenciaLutador.ORIGINAL.com(Parte.FAIXA, pretaVisual));
        MotorSolo m = guarda(orig, original(CatalogoPersonagens.ID_PRETO));
        assertEquals("GRADUAÇÃO AZUL", m.motivoFinalizacao(Lado.A, OMOPLATA), "faixa preta desenhada nao libera");
        IrmaoMiau pBranca = perfil(CatalogoPersonagens.ID_BRANCO,
                AparenciaLutador.ORIGINAL.com(Parte.FAIXA, Color.WHITE)).criarLutador(300, 150, 0.7);
        MotorSolo mp = guarda(pBranca, original(CatalogoPersonagens.ID_PRETO));
        assertNull(mp.motivoFinalizacao(Lado.A, GOGOPLATA), "faixa branca desenhada nao trava o perfil");
        assertEquals(Color.WHITE, pBranca.getCorFaixa());
    }

    // 5 e 6. A liberacao nao toca atributos nem graduacao: nada de bonus de faixa preta
    @Test
    void liberacaoNaoMudaAtributos() {
        EscolhaLutador e = perfil(CatalogoPersonagens.ID_BRANCO);
        IrmaoMiau p = e.criarLutador(300, 150, 0.7);
        assertEquals(Graduacao.BRANCA, p.getGraduacao());
        assertTrue(PerfilLutador.mesmosValores(PROPRIOS, p.getAtributos()), "atributos do editor");
        assertTrue(PerfilLutador.mesmosValores(PROPRIOS, e.getAtributos()));
        AtributosLutador preta = EvolucaoGraduacao.aplicar(PROPRIOS, Graduacao.PRETA);
        assertNotEquals(preta.getForca(), p.getAtributos().getForca(), "sem o bonus da preta");
        MotorSolo m = guarda(p, original(CatalogoPersonagens.ID_PRETO));
        m.motivoFinalizacao(Lado.A, GOGOPLATA);
        assertTrue(PerfilLutador.mesmosValores(PROPRIOS, p.getAtributos()), "consultar nao muda nada");
    }

    // 7. O filtro da base (tecnicas que o lutador nao tem) continua valendo para o perfil
    @Test
    void tecnicaQueABaseNaoTemContinuaIndisponivel() {
        IrmaoMiau p = perfil(CatalogoPersonagens.ID_BRANCO).criarLutador(300, 150, 0.7);
        p.esquecerTecnica(GOGOPLATA.tecnicaBase());
        MotorSolo m = guarda(p, original(CatalogoPersonagens.ID_PRETO));
        assertEquals("NÃO APRENDIDA", m.motivoFinalizacao(Lado.A, GOGOPLATA));
        assertNull(m.motivoFinalizacao(Lado.A, OMOPLATA), "as outras continuam");
    }

    // 8. Posicao, papel, pegada (controle) e folego continuam valendo
    @Test
    void requisitosDePosicaoEControleContinuam() {
        IrmaoMiau p = perfil(CatalogoPersonagens.ID_BRANCO).criarLutador(300, 150, 0.7);
        MotorSolo m = guarda(p, original(CatalogoPersonagens.ID_PRETO));
        // quem esta por cima na guarda nao tem a omoplata da guarda (papel/posicao)
        assertTrue(CATALOGO.para(EstadoPosicional.noChao(Posicao.GUARDA, Lado.B, Lado.A), Lado.B,
                new TecnicaSolo.ComandoSolo(Direcao.BAIXO, Familia.L1)).map(f -> f != OMOPLATA).orElse(true));
        m.definirEstabilidade(10);
        assertEquals("CONTROLE BAIXO", m.motivoFinalizacao(Lado.A, PUNHO), "chave de punho exige controle");
        m.definirEstabilidade(80);
        assertNull(m.motivoFinalizacao(Lado.A, PUNHO));
        p.gastarFolegoEsforco(p.getFolegoAtual(), "teste");
        assertEquals("SEM FÔLEGO", m.motivoFinalizacao(Lado.A, OMOPLATA));
    }

    // 9. Originais como antes: a graduacao escolhida libera
    @Test
    void originaisComoAntes() {
        IrmaoMiau o = original(CatalogoPersonagens.ID_BRANCO);
        assertFalse(o.isTecnicasDaBaseLiberadas());
        MotorSolo m = guarda(o, original(CatalogoPersonagens.ID_PRETO));
        assertEquals("GRADUAÇÃO AZUL", m.motivoFinalizacao(Lado.A, OMOPLATA));
        o.setGraduacao(Graduacao.AZUL);
        assertNull(m.motivoFinalizacao(Lado.A, OMOPLATA));
        assertEquals("GRADUAÇÃO MARROM", m.motivoFinalizacao(Lado.A, GOGOPLATA));
        o.setGraduacao(Graduacao.MARROM);
        assertNull(m.motivoFinalizacao(Lado.A, GOGOPLATA));
    }

    // 10. Regulamento de faixa (PONTOS + FINALIZACAO): o perfil conta como a graduacao logica (branca)
    @Test
    void regulamentoDeFaixaValeParaOPerfil() {
        IrmaoMiau p = perfil(CatalogoPersonagens.ID_BRANCO).criarLutador(300, 150, 0.7);
        IrmaoMiau azul = EscolhaLutador.oficial(base(CatalogoPersonagens.ID_PRETO)).com(Graduacao.AZUL, null)
                .criarLutador(380, 150, 0.7);
        MotorSolo m = guarda(p, azul);
        m.setRegulamentoDeFaixa(true);
        assertEquals("GRADUAÇÃO AZUL", m.motivoFinalizacao(Lado.A, OMOPLATA), "a personalizacao nao fura a regra");
        assertEquals("GRADUAÇÃO MARROM", m.motivoFinalizacao(Lado.A, GOGOPLATA));
        MotorSolo m2 = guarda(azul, p);
        m2.setRegulamentoDeFaixa(true);
        assertNull(m2.motivoFinalizacao(Lado.A, OMOPLATA), "original azul: como sempre");
    }

    // 10 e 11. No jogo: so o PONTOS + FINALIZACAO liga o regulamento; FINALIZACAO e TREINO liberam o perfil
    @Test
    void gamePanelLigaORegulamentoSoNoModoComPontos() {
        novo(InicioLuta.EM_PE);
        painel.configurarPartida(RegistroCenarios.ID_PADRAO, 0);
        painel.configurarModoLuta(ModoLuta.PONTOS_E_FINALIZACAO);
        ticks(2);
        assertTrue(painel.getMotorSolo().isRegulamentoDeFaixa());
        assertTrue(painel.getArbitro().isPresent(), "arbitragem do modo com pontos");
        painel.configurarModoLuta(ModoLuta.FINALIZACAO);
        assertFalse(painel.getMotorSolo().isRegulamentoDeFaixa(), "modo livre");

        painel = new GamePanel(ConfiguracaoTreino.padrao().comJogador(perfil(CatalogoPersonagens.ID_BRANCO))
                .comParceiro(perfil(CatalogoPersonagens.ID_PRETO)), () -> { }, () -> { }, () -> { });
        painel.pararLoopParaTestes();
        ticks(2);
        MotorSolo m = painel.getMotorSolo();
        assertFalse(m.isRegulamentoDeFaixa(), "treino: sem regulamento");
        assertTrue(painel.getArbitro().isEmpty(), "treino: sem arbitragem");
        assertTrue(j1().isTecnicasDaBaseLiberadas());
        assertTrue(PerfilLutador.mesmosValores(PROPRIOS, j1().getAtributos()));
        m.forcarPosicao(Posicao.GUARDA, Lado.B, Lado.A);
        String motivo = m.motivoFinalizacao(Lado.A, GOGOPLATA);
        assertFalse(motivo != null && motivo.startsWith("GRADUAÇÃO"), "no treino o perfil usa a gogoplata: " + motivo);
    }
}
