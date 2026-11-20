package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.controles.NaturezaAcao;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.hud.AvisosCombate;
import com.irmaosmiau.hud.PainelDpadContextual;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.ClassificacaoAcoes;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.NarradorSolo;
import com.irmaosmiau.posicional.TecnicaSolo;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import org.junit.jupiter.api.Test;

/**
 * Entrega 5Q.7: o aviso principal de combate usa a cor da natureza REAL da acao (a mesma tabela
 * das rodas); avisos do sistema continuam neutros.
 */
class Mensagens5Q7GamePanelTest extends Base5OGamePanel {

    private static MotorSolo.Evento ev(String tipo, String tecnica) {
        return new MotorSolo.Evento(1, tipo, tecnica, Lado.A, "");
    }

    @Test
    void aNaturezaDoAvisoEAMesmaDaRodaParaTodaTecnicaNarrada() {
        for (TecnicaSolo t : CatalogoTecnicasSolo.referencia().todas()) {
            MotorSolo.Evento e = ev("SUCESSO", t.id());
            if (NarradorSolo.mensagem(e) != null) {
                assertEquals(ClassificacaoAcoes.de(t), NarradorSolo.natureza(e), t.id());
            }
        }
        assertEquals(NaturezaAcao.FINALIZACAO, NarradorSolo.natureza(ev("FIN_CONTROLE", "armlock")), "encaixou");
        assertEquals(NaturezaAcao.DEFESA, NarradorSolo.natureza(ev("FIN_DEFENDIDA", "armlock")), "defendeu");
        assertEquals(NaturezaAcao.DEFESA, NarradorSolo.natureza(ev("DEFENDIDA", CatalogoTecnicasSolo.RASTEIRA)));
        assertEquals(NaturezaAcao.TRANSICAO, NarradorSolo.natureza(ev("SUCESSO", CatalogoTecnicasSolo.PASSAR_GUARDA)));
        assertEquals(NaturezaAcao.QUEDA, NarradorSolo.natureza(ev("SUCESSO", CatalogoTecnicasSolo.RASTEIRA)));
        assertEquals(NaturezaAcao.DESISTENCIA, NarradorSolo.natureza(ev("RENDICAO", "armlock")));
        assertNull(NarradorSolo.natureza(ev("EXAUSTAO", "")), "neutro");
    }

    @Test
    void aCorDoAvisoVemDaMesmaTabelaDasRodas() {
        for (NaturezaAcao n : NaturezaAcao.values()) {
            assertEquals(PainelDpadContextual.corDaNatureza(n), AvisosCombate.corDe(n), n.name());
        }
        assertEquals(AvisosCombate.COR_NEUTRA, AvisosCombate.corDe(null), "sistema: a cor de sempre");
        AvisosCombate a = new AvisosCombate();
        a.adicionar("J1 encaixou: armlock!", NaturezaAcao.FINALIZACAO);
        a.adicionar("MODO DESENVOLVEDOR LIGADO");
        assertEquals(PainelDpadContextual.FINALIZACAO, a.corAtual());
        for (int i = 0; i < AvisosCombate.DURACAO_COM_FILA_TICKS; i++) {
            a.atualizar();
        }
        assertEquals("MODO DESENVOLVEDOR LIGADO", a.getAtual());
        assertEquals(AvisosCombate.COR_NEUTRA, a.corAtual(), "a fila guarda a natureza de cada aviso");
    }

    @Test
    void derrubadaNoJogoApareceComACorDasQuedas() throws Exception {
        naEdt(() -> {
            novo(InicioLuta.EM_PE);
            j1Pega(1);
            tecla("brancoControle", true);
            tecla("brancoEsquerda", true);
            ticks(6);
            tecla("brancoEsquerda", false);
            tecla("brancoControle", false);
            painel.getMotorSolo().comando(Lado.A, new ComandoSolo(Direcao.CIMA, Familia.L2));
            boolean viu = false;
            for (int i = 0; i < 80 && !viu; i++) {
                ticks(1);
                String a = painel.avisoAtual();
                if (a != null && a.contains("derrubou")) {
                    viu = true;
                    assertEquals(NaturezaAcao.QUEDA, painel.naturezaDoAviso(), a);
                }
            }
            assertTrue(viu, "o aviso da derrubada apareceu");
        });
    }
}
