package com.irmaosmiau.combate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import org.junit.jupiter.api.Test;

/** Entrega 5L.1: balanceamento de folego, firmeza, desequilibrio e rompimento entre os oficiais. */
class Balanceamento5L1Test {

    private IrmaoMiau apl;
    private IrmaoMiau rec;
    private SistemaCombate s;

    private void pegada(IrmaoMiau a, IrmaoMiau r) {
        apl = a;
        rec = r;
        apl.setViradoParaDireita(apl.getX() < rec.getX());
        rec.setViradoParaDireita(!apl.isViradoParaDireita());
        s = CarregadorConteudoCombate.carregarPadrao().criarSistema(apl, rec);
        assertEquals(ResultadoAcao.SUCESSO, s.tentarAcao(apl, rec, "pegada_1"));
        ticks(31);
        assertTrue(s.isInteracaoEstabelecida());
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            apl.atualizar(false, false);
            rec.atualizar(false, false);
            s.atualizar();
        }
    }

    /** Tenta ate dar certo; devolve em qual tentativa (ou -1). */
    private int tentativasAte(java.util.function.Supplier<ResultadoAcaoTecnica> acao, int maximo) {
        for (int i = 1; i <= maximo; i++) {
            ResultadoAcaoTecnica r = acao.get();
            if (r == ResultadoAcaoTecnica.SUCESSO) {
                return i;
            }
            ticks(SistemaCombate.RECARGA_ACAO_TECNICA_TICKS);
        }
        return -1;
    }

    @Test
    void valoresDeBalanceamento() {
        // 5P.1 (pedido): custos menores.
        assertEquals(2.0, SistemaCombate.CUSTO_DESEQUILIBRIO);
        assertEquals(1.75, SistemaCombate.CUSTO_ROMPIMENTO_TECNICO);
        assertEquals(1.5, SistemaCombate.CUSTO_REFORCO);
        assertEquals(30.0, SistemaCombate.REFORCO_FIRMEZA);
        assertEquals(0.06, SistemaCombate.DESGASTE_FIRMEZA_POR_TICK);
        assertEquals(0.08, SistemaCombate.DESGASTE_RESISTENCIA_POR_TICK);
    }

    @Test
    void pretoDesequilibraOBrancoInsistindoSemSerGarantidoNaPrimeira() {
        pegada(new IrmaoMiauPreto(500, 150, 0.7), new IrmaoMiauBranco(440, 150, 0.7));
        assertEquals(ResultadoAcaoTecnica.RESISTIDO, s.tentarDesequilibrio(apl), "a primeira ainda falha");
        assertEquals(1, s.getInsistencia(apl));
        ticks(SistemaCombate.RECARGA_ACAO_TECNICA_TICKS);
        int n = 1 + tentativasAte(() -> s.tentarDesequilibrio(apl), SistemaCombate.MAX_INSISTENCIA);
        assertTrue(n > 1 && n <= SistemaCombate.MAX_INSISTENCIA + 1,
                "antes da 5L.1: impossivel em qualquer firmeza; agora em poucas tentativas (" + n + ")");
        assertEquals(0, s.getInsistencia(apl), "acertou: zera");
    }

    @Test
    void pretoRompeOBrancoComPoucasTentativasMasNaoNaPrimeira() {
        pegada(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        int n = tentativasAte(() -> s.tentarRompimentoTecnico(rec), 6);
        assertTrue(n > 1, "nao rompe toda pegada instantaneamente: " + n);
        assertTrue(n <= SistemaCombate.MAX_INSISTENCIA + 1, "sem tentativas excessivas: " + n);
        assertTrue(rec.getFolegoAtual() > 0, "ainda sobra folego");
    }

    @Test
    void brancoContinuaMaisForteNessasAcoesDiferencasPreservadas() {
        pegada(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.tentarDesequilibrio(apl), "Branco desequilibra de primeira");
        ResultadoDisputa sem = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, apl, rec, 100);
        ResultadoDisputa um = ResolvedorDisputa.resolver(TipoDisputa.DESEQUILIBRIO, apl, rec, 100, 1.0);
        assertEquals(sem.getPontuacaoAtaque(), um.getPontuacaoAtaque(), 1e-12, "sem insistencia: formula intacta");
        assertTrue(ParametrosPegadas.fatorRompimento("pegada_3", rec) < ParametrosPegadas.fatorRompimento("pegada_3", apl),
                "especialidades preservadas");
    }

    @Test
    void insistenciaZeraQuandoAPegadaAcabaENaoPassaEntreAcoes() {
        pegada(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        assertEquals(ResultadoAcaoTecnica.RESISTIDO, s.tentarRompimentoTecnico(rec));
        assertEquals(1, s.getInsistencia(rec));
        s.encerrarInteracao();
        assertEquals(0, s.getInsistencia(rec), "nova pegada, nova disputa");
    }

    @Test
    void pegadaDuraMasNaoParaSempreEReforcoAjuda() {
        pegada(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        int t = 0;
        while (s.estaEmInteracao() && t < 3000) {
            ticks(1);
            t++;
        }
        assertTrue(t >= 1600 && t < 1700, "sem reforco: ~27 s (antes ~16,7 s): " + t);
        assertEquals(MotivoEncerramentoInteracao.SEM_CONTROLE, s.getUltimoMotivoEncerramento());

        pegada(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        ticks(700);
        double f = s.getFirmeza();
        assertEquals(ResultadoAcaoTecnica.SUCESSO, s.tentarReforcarPegada(apl));
        assertEquals(f + 30, s.getFirmeza(), 1e-9, "+30 por 2 de folego");
        assertEquals(ResultadoAcaoTecnica.EM_RECARGA, s.tentarReforcarPegada(apl), "nao segura para sempre");
    }

    @Test
    void receptorForcandoAindaDeixaTempoDeReagir() {
        pegada(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        int t = 0;
        while (s.estaEmInteracao() && t < 3000) {
            s.resistirPegada(rec);
            ticks(1);
            t++;
        }
        assertTrue(t > 600, "forcando sem parar: mais de ~10 s para a firmeza acabar (" + t + ")");
        assertTrue(rec.getFolegoAtual() >= 0);
    }

    @Test
    void folegoNuncaNegativoInsistindo() {
        pegada(new IrmaoMiauBranco(440, 150, 0.7), new IrmaoMiauPreto(500, 150, 0.7));
        for (int i = 0; i < 40; i++) {
            s.tentarRompimentoTecnico(rec);
            s.tentarDesequilibrio(apl);
            s.tentarReforcarPegada(apl);
            ticks(SistemaCombate.RECARGA_ACAO_TECNICA_TICKS);
        }
        assertTrue(apl.getFolegoAtual() >= 0 && rec.getFolegoAtual() >= 0);
        assertTrue(s.getFirmeza() >= 0 && s.getFirmeza() <= 100);
    }
}
