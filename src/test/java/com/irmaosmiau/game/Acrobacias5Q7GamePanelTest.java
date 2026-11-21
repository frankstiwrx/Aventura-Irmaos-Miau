package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoAcrobacia;
import com.irmaosmiau.regras.Graduacao;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.7: recuperacao DINAMICA das acrobacias (estrela e mortais) pelos atributos e pela
 * condicao fisica; a duracao da animacao nao muda. E o rolamento de joelhos 40% mais lento.
 */
class Acrobacias5Q7GamePanelTest extends Base5OGamePanel {

    private static IrmaoMiau branco(Graduacao g) {
        IrmaoMiau l = new IrmaoMiauBranco(100, 300, 0.7);
        l.setGraduacao(g);
        return l;
    }

    private static IrmaoMiau preto(Graduacao g) {
        IrmaoMiau l = new IrmaoMiauPreto(300, 300, 0.7);
        l.setGraduacao(g);
        return l;
    }

    @Test
    void atributosEFaixaDiminuemARecuperacaoSemContarAFaixaDuasVezes() {
        int brancaLento = branco(Graduacao.BRANCA).recargaAcrobaciaDinamica();
        int brancaAgil = preto(Graduacao.BRANCA).recargaAcrobaciaDinamica();
        int pretaLento = branco(Graduacao.PRETA).recargaAcrobaciaDinamica();
        int pretaAgil = preto(Graduacao.PRETA).recargaAcrobaciaDinamica();
        assertTrue(brancaAgil < brancaLento, "mais agil e rapido recupera antes: " + brancaAgil + " x " + brancaLento);
        assertTrue(pretaLento < brancaLento && pretaAgil < brancaAgil, "a faixa (atributos) ajuda");
        assertTrue(pretaAgil <= 9, "faixa preta agil, descansado: intervalo muito pequeno (" + pretaAgil + ")");
        assertTrue(brancaLento <= IrmaoMiau.RECARGA_ACROBACIA_TICKS, "descansado nunca passa da base antiga");
        // a faixa entra so pelos atributos: mesmos atributos = mesma recarga, qualquer que seja a faixa
        IrmaoMiau a = preto(Graduacao.PRETA);
        IrmaoMiau b = new IrmaoMiauPreto(300, 300, 0.7, a.getAtributos().copiar());
        assertEquals(a.recargaAcrobaciaDinamica(), b.recargaAcrobaciaDinamica());
    }

    @Test
    void cansacoEFaltaDeCondicionamentoAumentamARecuperacao() {
        IrmaoMiau l = preto(Graduacao.ROXA);
        int descansado = l.recargaAcrobaciaDinamica();
        l.gastarFolegoEsforco(l.getAtributos().getFolego() * 0.8, "teste");
        int semFolego = l.recargaAcrobaciaDinamica();
        assertTrue(semFolego > descansado, "folego baixo: " + descansado + " -> " + semFolego);
        l.gastarCondicionamentoEsforco(l.getAtributos().getCondicionamento() * 0.8, "teste");
        int semCondicionamento = l.recargaAcrobaciaDinamica();
        assertTrue(semCondicionamento > semFolego, "condicionamento baixo: " + semFolego + " -> " + semCondicionamento);
        assertTrue(semCondicionamento <= IrmaoMiau.RECARGA_ACROBACIA_MAXIMA_TICKS);
    }

    @Test
    void recursosInfinitosRefletemCondicaoIdealERespeitamOQueFicouLimitado() {
        IrmaoMiau l = branco(Graduacao.AZUL);
        int ideal = l.recargaAcrobaciaDinamica();
        l.gastarFolegoEsforco(l.getAtributos().getFolego() * 0.9, "teste");
        l.gastarCondicionamentoEsforco(l.getAtributos().getCondicionamento() * 0.9, "teste");
        int cansado = l.recargaAcrobaciaDinamica();
        l.setRecursosInfinitos(false, true);                       // so o condicionamento infinito
        int soCondicionamento = l.recargaAcrobaciaDinamica();
        assertTrue(soCondicionamento < cansado && soCondicionamento > ideal,
                "o folego continua limitado: " + ideal + " < " + soCondicionamento + " < " + cansado);
        l.setRecursosInfinitos(true, true);
        assertEquals(ideal, l.recargaAcrobaciaDinamica(), "infinitos: a melhor condicao do lutador");
        l.setRecursosInfinitos(false, false);
        l.gastarFolegoEsforco(l.getAtributos().getFolego() * 0.9, "teste");
        assertTrue(l.recargaAcrobaciaDinamica() > ideal, "desligado: volta a regra normal");
    }

    @Test
    void faixaPretaEncadeiaEstrelasEMortaisSemCortarAAnimacao() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            painel.configurarPartida("dojo", 0);
            j2().setGraduacao(Graduacao.PRETA);
            j2().setRecursosInfinitos(true, true);
            TipoAcrobacia[] seq = {TipoAcrobacia.ESTRELA_ESQUERDA, TipoAcrobacia.ESTRELA_DIREITA,
                    TipoAcrobacia.MORTAL_FRENTE, TipoAcrobacia.MORTAL_TRAS};
            for (TipoAcrobacia t : seq) {
                int espera = 0;
                while (j2().motivoSemAcrobacia(t) != null && espera < 200) {
                    ticks(1);
                    espera++;
                }
                assertTrue(espera <= j2().recargaAcrobaciaDinamica() + 1, t + ": esperou " + espera);
                assertTrue(j2().iniciarAcrobacia(t), t.name());
                int duracao = 0;
                while (j2().isEmAcrobacia() && duracao < 200) {
                    assertFalse(j2().iniciarAcrobacia(t), "nao sobrepoe nem cancela no meio");
                    ticks(1);
                    duracao++;
                }
                // 5Q.7 corretiva (pedido): a execucao agora e a do lutador (encurta pela agilidade/velocidade)
                assertEquals(j2().duracaoAcrobacia(t), duracao, "a animacao inteira (execucao), sem cortar");
                assertEquals(j2().recargaAcrobaciaDinamica(), j2().getRecargaAcrobacia());
            }
        });
    }

    @Test
    void rolamentoDeJoelhos40PorCentoMaisLentoComAMesmaDistancia() {
        assertEquals(0.6, IrmaoMiau.FATOR_ROLAMENTO_JOELHOS_5Q7, 1e-9);
        assertEquals(0.48, IrmaoMiau.MULTIPLICADOR_ROLAMENTO_JOELHOS, 1e-9);
        int antes = (int) Math.round(22 / 0.8);
        int agora = IrmaoMiau.duracaoRolamento(EstadoCorporal.AJOELHADO);
        assertEquals(1.67, agora / (double) antes, 0.05, "duracao ~1,67x a da 5Q.6: " + antes + " -> " + agora);
        assertEquals(37, IrmaoMiau.duracaoRolamento(EstadoCorporal.DEITADO), "deitado nao mudou");
        assertEquals(22, IrmaoMiau.duracaoRolamento(EstadoCorporal.EM_PE), "em pe nao mudou");
        assertEquals(32, IrmaoMiau.duracaoSprawl(), "sprawl nao mudou");
    }
}
