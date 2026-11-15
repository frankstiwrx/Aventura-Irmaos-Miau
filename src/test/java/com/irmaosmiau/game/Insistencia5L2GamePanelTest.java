package com.irmaosmiau.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.combate.ResultadoAcaoTecnica;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.InicioLuta;
import com.irmaosmiau.entities.TipoPersonagem;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/** Entrega 5L.2: diagnostico da insistencia (5L.1) no F3, para J1 e J2. */
class Insistencia5L2GamePanelTest {

    private static final int LINHA = 9;
    private GamePanel painel;

    private void ticks(int n) {
        for (int i = 0; i < n; i++) {
            painel.atualizarUmTickParaTestes();
        }
    }

    private String linha() {
        return painel.linhasTecnicas().get(LINHA);
    }

    @Test
    void mostraQuemInsisteAcaoFalhasEBonusEZera() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            painel = new GamePanel(TipoPersonagem.AMBOS, null, null, InicioLuta.DE_JOELHOS, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(1);
            SistemaCombate s = painel.getSistemaCombate();
            assertEquals(10, painel.linhasTecnicas().size());
            assertTrue(30 + 268 + (painel.linhasTecnicas().size() - 1) * 15 < GamePanel.ALTURA, "cabe na tela");
            assertEquals("INSISTENCIA: - (bonus 0%)", linha());

            // J2 (Preto) aplica e insiste no desequilibrio contra o Branco.
            painel.getActionMap().get("pretoPegada1Pressionada").actionPerformed(null);
            ticks(1);
            painel.getActionMap().get("pretoPegada1Solta").actionPerformed(null);
            ticks(40);
            assertEquals(ResultadoAcaoTecnica.RESISTIDO, s.tentarDesequilibrio(painel.getLutador2()));
            assertTrue(linha().startsWith("INSISTENCIA: J2 "), linha());
            assertTrue(linha().contains("DESEQUILIBRAR | falhas seguidas 1 | bonus +8%"), linha());
            ResultadoAcaoTecnica r = ResultadoAcaoTecnica.RESISTIDO;
            for (int i = 0; i < 5 && r != ResultadoAcaoTecnica.SUCESSO; i++) {
                ticks(SistemaCombate.RECARGA_ACAO_TECNICA_TICKS);
                r = s.tentarDesequilibrio(painel.getLutador2());
                if (r != ResultadoAcaoTecnica.SUCESSO) {
                    assertTrue(linha().contains("bonus +" + (8 * s.getInsistencia(painel.getLutador2())) + "%"), linha());
                }
            }
            assertEquals(ResultadoAcaoTecnica.SUCESSO, r);
            assertEquals("INSISTENCIA: - (bonus 0%)", linha(), "sucesso: zera");

            // O Branco (J1) rompe o Preto de primeira: nada a insistir.
            assertEquals(ResultadoAcaoTecnica.SUCESSO, s.tentarRompimentoTecnico(painel.getLutador1()));
            assertEquals("INSISTENCIA: - (bonus 0%)", linha());
        });
    }

    @Test
    void j1InsistindoNoRomperEFimDaPegadaZera() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // J1 = Preto, J2 = Branco: o Branco (J2) aplica e o Preto (J1) insiste no ROMPER.
            com.irmaosmiau.personagens.CatalogoPersonagens c = com.irmaosmiau.personagens.CatalogoPersonagens.padrao();
            painel = new GamePanel(TipoPersonagem.AMBOS,
                    com.irmaosmiau.personagens.EscolhaLutador.oficial(c.exigir(com.irmaosmiau.personagens.CatalogoPersonagens.ID_PRETO)),
                    com.irmaosmiau.personagens.EscolhaLutador.oficial(c.exigir(com.irmaosmiau.personagens.CatalogoPersonagens.ID_BRANCO)),
                    InicioLuta.DE_JOELHOS, () -> { }, () -> { }, () -> { });
            painel.pararLoopParaTestes();
            ticks(1);
            SistemaCombate s = painel.getSistemaCombate();
            painel.getActionMap().get("pretoPegada1Pressionada").actionPerformed(null);
            ticks(1);
            painel.getActionMap().get("pretoPegada1Solta").actionPerformed(null);
            ticks(40);
            assertEquals(ResultadoAcaoTecnica.RESISTIDO, s.tentarRompimentoTecnico(painel.getLutador1()));
            assertTrue(linha().startsWith("INSISTENCIA: J1 "), linha());
            assertTrue(linha().contains("ROMPER | falhas seguidas 1 | bonus +8%"), linha());
            s.encerrarInteracao();
            assertEquals("INSISTENCIA: - (bonus 0%)", linha(), "fim da pegada: zera");
        });
    }
}
