package com.irmaosmiau.animacao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.conteudo.ConteudoInvalidoException;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Entrega 5N: biblioteca COMPARTILHADA das reacoes do receptor (por tecnica,
 * nao por personagem) e a camada minima de adaptacao ao rig.
 */
class ReacoesReceptor5NTest {

    /** Personagem futuro simulado: mesmo esqueleto, bracos mais longos. */
    static final class MiauJudoDeTeste extends IrmaoMiauBranco {
        MiauJudoDeTeste() {
            super(100, 150, 0.7);
        }
    }

    /** Personagem futuro simulado com OUTRO esqueleto (nao compativel). */
    static final class MiauOutroRigDeTeste extends IrmaoMiauPreto {
        MiauOutroRigDeTeste() {
            super(100, 150, 0.7);
        }

        @Override
        public String getRigAnimacao() {
            return "rig_quadrupede_de_teste";
        }
    }

    private final BibliotecaReacoesReceptor bib = BibliotecaReacoesReceptor.padrao();

    @AfterEach
    void limpar() {
        AdaptadorRigReacao.removerProporcao(MiauJudoDeTeste.class);
    }

    private ReacaoReceptor r(int n) {
        return bib.buscar("pegada_" + n).orElseThrow();
    }

    private static int soma(int[] d) {
        int s = 0;
        for (int i = 4; i < d.length; i++) {
            s += Math.abs(d[i]);
        }
        return s;
    }

    // 1-4. As quatro tecnicas tem reacao, com o contrato de quadros e rig semantico
    @Test
    void quatroTecnicasComReacao() {
        assertEquals(List.of("pegada_1", "pegada_2", "pegada_3", "pegada_4"), List.copyOf(bib.tecnicas()));
        for (int n = 1; n <= 4; n++) {
            ReacaoReceptor x = r(n);
            assertEquals("miau_v1", x.getRig());
            for (int q = 0; q < ReacaoReceptor.QUADROS; q++) {
                int[] d = x.quadro(q);
                assertEquals(30, d.length);
                assertEquals(0, d[0] | d[1] | d[2] | d[3], "cabecalho nao e delta");
                assertTrue(soma(d) > 0, "pegada_" + n + " quadro " + q + " mexe o corpo");
                for (int i = 4; i < 30; i++) {
                    assertTrue(Math.abs(d[i]) <= AdaptadorRigReacao.LIMITE, "sem exagero");
                }
            }
            assertTrue(x.getOrigem().contains("COMPARTILHADA"));
        }
    }

    // 5. Cada controle produz uma reacao diferente
    @Test
    void reacoesDiferentesPorTecnica() {
        for (int a = 1; a <= 4; a++) {
            for (int b = a + 1; b <= 4; b++) {
                int[] da = r(a).quadro(ReacaoReceptor.SUSTENTADA_A);
                int[] db = r(b).quadro(ReacaoReceptor.SUSTENTADA_A);
                int dif = 0;
                for (int i = 4; i < 30; i++) {
                    dif += Math.abs(da[i] - db[i]);
                }
                assertTrue(dif >= 30, "pegada_" + a + " x pegada_" + b + " parecidas demais: " + dif);
            }
        }
        // marcas de cada tecnica
        assertTrue(r(2).quadro(1)[6] < 0, "cruzada gira a cintura");
        assertTrue(r(3).quadro(1)[16] > 10 && r(3).quadro(1)[20] > 0, "duas mangas: os dois bracos a frente");
        assertTrue(r(4).quadro(1)[11] > r(1).quadro(1)[11] + 5, "gola e nuca: cabeca abaixa");
        assertTrue(r(1).quadro(1)[21] > 8, "gola e manga: o braco da manga e puxado");
    }

    // Entrada: nada antes do contato, sobe sem saltos, termina na sustentada
    @Test
    void entradaProgressivaSemSaltos() {
        ReacaoReceptor x = r(1);
        assertEquals(0, soma(x.deltaEntrada(0.0)));
        assertEquals(0, soma(x.deltaEntrada(ReacaoReceptor.INICIO_CONTATO)));
        assertArrayEquals(x.quadro(ReacaoReceptor.SUSTENTADA_A), x.deltaEntrada(1.0));
        int[] antes = x.deltaEntrada(0);
        for (int i = 1; i <= 100; i++) {
            int[] d = x.deltaEntrada(i / 100.0);
            for (int k = 4; k < 30; k++) {
                assertTrue(Math.abs(d[k] - antes[k]) <= 3, "salto de articulacao em " + i + "%");
            }
            antes = d;
        }
    }

    // Sustentada: periodica pelo tempo desde que a pegada existe (nao recomeca), sempre perto de A/B
    @Test
    void sustentadaRespiraSemRecomecar() {
        ReacaoReceptor x = r(3);
        assertArrayEquals(x.quadro(1), x.deltaSustentada(0));
        assertArrayEquals(x.deltaSustentada(300), x.deltaSustentada(300 + 2400));
        int[] a = x.quadro(1);
        int[] b = x.quadro(2);
        for (long ms = 0; ms < 5000; ms += 16) {
            int[] d = x.deltaSustentada(ms);
            for (int i = 4; i < 30; i++) {
                assertTrue(d[i] >= Math.min(a[i], b[i]) - 1 && d[i] <= Math.max(a[i], b[i]) + 1);
            }
        }
    }

    // Mesma reacao no Branco e no Preto, sem tabela por personagem
    @Test
    void mesmaReacaoEmBrancoEPreto() {
        IrmaoMiau b = new IrmaoMiauBranco(100, 150, 0.7);
        IrmaoMiau p = new IrmaoMiauPreto(100, 150, 0.7);
        for (int n = 1; n <= 4; n++) {
            int[] d = r(n).deltaSustentada(0);
            assertArrayEquals(AdaptadorRigReacao.adaptar(d, b, r(n)), AdaptadorRigReacao.adaptar(d, p, r(n)));
            assertNotNull(AdaptadorRigReacao.adaptar(d, b, r(n)));
        }
    }

    // Espelhamento: o delta e local (o lado nao muda nada)
    @Test
    void deltaNaoDependeDoLado() {
        IrmaoMiau d = new IrmaoMiauPreto(100, 150, 0.7);
        IrmaoMiau e = new IrmaoMiauPreto(100, 150, 0.7);
        d.setViradoParaDireita(true);
        e.setViradoParaDireita(false);
        int[] q = r(2).deltaSustentada(0);
        assertArrayEquals(AdaptadorRigReacao.adaptar(q, d, r(2)), AdaptadorRigReacao.adaptar(q, e, r(2)));
    }

    // Rig compativel adicional (personagem futuro) reutiliza a reacao, com a propria proporcao
    @Test
    void rigCompativelAdicionalReutiliza() {
        IrmaoMiau judo = new MiauJudoDeTeste();
        assertTrue(AdaptadorRigReacao.compativel(judo, r(1)));
        int[] base = AdaptadorRigReacao.adaptar(r(1).quadro(1), judo, r(1));
        AdaptadorRigReacao.registrarProporcao(MiauJudoDeTeste.class, new AdaptadorRigReacao.Proporcao(1.0, 1.5, 1.0));
        int[] longos = AdaptadorRigReacao.adaptar(r(1).quadro(1), judo, r(1));
        assertTrue(Math.abs(longos[21]) > Math.abs(base[21]), "bracos mais longos, delta maior");
        assertEquals(base[9], longos[9], "tronco igual");
        // proporcao exagerada nao deforma: tudo dentro do limite
        AdaptadorRigReacao.registrarProporcao(MiauJudoDeTeste.class, new AdaptadorRigReacao.Proporcao(5, 5, 5));
        for (int v : AdaptadorRigReacao.adaptar(r(4).quadro(1), judo, r(4))) {
            assertTrue(Math.abs(v) <= AdaptadorRigReacao.LIMITE);
        }
    }

    // Rig incompativel: fallback seguro (nenhum delta)
    @Test
    void rigIncompativelUsaFallback() {
        IrmaoMiau outro = new MiauOutroRigDeTeste();
        assertFalse(AdaptadorRigReacao.compativel(outro, r(1)));
        assertNull(AdaptadorRigReacao.adaptar(r(1).quadro(1), outro, r(1)));
    }

    // De joelhos: as pernas ficam com o motor (joelhos no tatame)
    @Test
    void deJoelhosNaoMexeNasPernas() {
        IrmaoMiau b = new IrmaoMiauBranco(100, 150, 0.7);
        b.ajoelhar();
        assertTrue(b.isAjoelhado());
        int[] d = AdaptadorRigReacao.adaptar(r(4).quadro(1), b, r(4));
        for (int i = 22; i < 30; i++) {
            assertEquals(0, d[i]);
        }
        assertTrue(Math.abs(d[9]) > 0, "o tronco continua reagindo");
    }

    // Uma tecnica sem arquivo nao tem reacao (fallback), e a convencao de caminho e por tecnica
    @Test
    void tecnicaSemArquivoSemReacao() {
        assertTrue(bib.buscar("pegada_inexistente").isEmpty());
        assertTrue(new BibliotecaReacoesReceptor().carregarSeExistir("pegada_inexistente").isEmpty());
        assertTrue(new BibliotecaReacoesReceptor().carregarSeExistir("pegada_2").isPresent());
        assertEquals("/conteudo/animacoes/receptor/receptor_pegada_3.json", BibliotecaReacoesReceptor.caminho("pegada_3"));
    }

    // O formato nao se mistura: uma reacao nao entra como animacao individual e vice-versa
    @Test
    void usoDoArquivoEValidado() {
        String receptor = "{\"tipo\":\"ANIMACAO_INDIVIDUAL_IRMAOS_MIAU\",\"versaoEsquema\":1,\"id\":\"x\","
                + "\"estado\":\"PEGADA_ESTABELECIDA\",\"rig\":\"miau_v1\",\"uso\":\"RECEPTOR\",\"duracaoQuadroMs\":100,"
                + "\"quadros\":[{\"pose\":[0,0,7,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]}]}";
        assertThrows(ConteudoInvalidoException.class, () -> CarregadorAnimacaoJson.ler(receptor, "t"));
        assertNotNull(CarregadorAnimacaoJson.lerReacaoReceptor(receptor, "t"));
        assertThrows(ConteudoInvalidoException.class,
                () -> CarregadorAnimacaoJson.lerReacaoReceptor(receptor.replace("PEGADA_ESTABELECIDA", "PARADO"), "t"));
        assertThrows(IllegalArgumentException.class, () -> ReacaoReceptor.de("x",
                CarregadorAnimacaoJson.lerReacaoReceptor(receptor, "t")), "precisa dos 4 quadros");
    }

    // 24. Arquivos autorais intactos (fim de linha normalizado)
    @Test
    void arquivosAutoraisIntactos() throws Exception {
        assertEquals("a5d37b2ad6a877bda9821f7bb7f065b25018f358172dacf6a94099a1a7edab72",
                hash("/conteudo/miaustudio/PEGADA_1.json"));
        assertEquals("dc75f6641a48f2f87b486d3e73ffa66b3bca33aff56115228228920f0210fd43",
                hash("/conteudo/miaustudio/PEGADA_1_PRETO.json"));
    }

    private static String hash(String recurso) throws Exception {
        try (InputStream in = ReacoesReceptor5NTest.class.getResourceAsStream(recurso)) {
            byte[] b = in.readAllBytes();
            String t = new String(b, java.nio.charset.StandardCharsets.UTF_8).replace("\r\n", "\n");
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(t.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
    }

    static int[] copia(int[] x) {
        return Arrays.copyOf(x, x.length);
    }
}
