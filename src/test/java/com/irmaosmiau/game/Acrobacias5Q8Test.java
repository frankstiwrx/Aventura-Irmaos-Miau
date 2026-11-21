package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;
import com.irmaosmiau.entities.IrmaoMiauPreto;
import com.irmaosmiau.entities.TipoAcrobacia;
import com.irmaosmiau.regras.Graduacao;

import org.junit.jupiter.api.Test;

/** Entrega 5Q.8: rolamento de joelhos mais lento e acrobacias mais responsivas com boa condicao. */
class Acrobacias5Q8Test {

    @Test
    void rolamentoDeJoelhosMaisQuarentaPorCentoLento() {
        assertEquals(0.6, IrmaoMiau.FATOR_ROLAMENTO_JOELHOS_5Q8, 1e-9);
        // adendo 5Q.8 (pedido): o rolamento de joelhos usa a velocidade do deitado (76 -> 37) e o sprawl
        // ficou 20% mais lento (32 -> 40)
        assertEquals(37, IrmaoMiau.duracaoRolamento(EstadoCorporal.AJOELHADO), "a velocidade do deitado");
        assertEquals(37, IrmaoMiau.duracaoRolamento(EstadoCorporal.DEITADO));
        assertEquals(22, IrmaoMiau.duracaoRolamento(EstadoCorporal.EM_PE));
        assertEquals(40, IrmaoMiau.duracaoSprawl());
    }

    @Test
    void boaCondicaoEncurtaAExecucaoERecursosInfinitosDaoAMaiorFluidez() {
        IrmaoMiau l = new IrmaoMiauPreto(300, 300, 0.7);
        l.setGraduacao(Graduacao.PRETA);
        int descansado = l.duracaoAcrobacia(TipoAcrobacia.ESTRELA_ESQUERDA);
        int recargaDescansado = l.recargaAcrobaciaDinamica();
        l.gastarFolegoEsforco(l.getAtributos().getFolego() * 0.9, "teste");
        l.gastarCondicionamentoEsforco(l.getAtributos().getCondicionamento() * 0.9, "teste");
        int cansado = l.duracaoAcrobacia(TipoAcrobacia.ESTRELA_ESQUERDA);
        assertTrue(descansado < cansado, "cansado executa mais devagar: " + descansado + " x " + cansado);
        l.setRecursosInfinitos(true, true);
        assertEquals(descansado, l.duracaoAcrobacia(TipoAcrobacia.ESTRELA_ESQUERDA), "infinitos = descansado");
        assertTrue(descansado <= 20 && descansado >= TipoAcrobacia.DURACAO_MINIMA_TICKS, "estrela agil " + descansado);
        assertTrue(recargaDescansado <= 4, "quase sem espera: " + recargaDescansado);
    }
}
