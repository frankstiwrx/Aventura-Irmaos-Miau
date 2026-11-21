package com.irmaosmiau.hud;

import com.irmaosmiau.controles.CamadaComando;
import com.irmaosmiau.controles.FamiliaTecnica;
import com.irmaosmiau.controles.PapelCombate;
import com.irmaosmiau.posicional.CatalogoFinalizacoes;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.EstadoPosicional;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.Posicao;
import com.irmaosmiau.posicional.RegistroFinalizacoes;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Ferramenta de desenvolvimento: o codigo estavel de cada roda (M-001, M-002...), mostrado SO com o Modo
 * Desenvolvedor (F8) ligado, abaixo da roda aberta. As rodas, seus nomes e o resto do HUD nao mudam.
 *
 * Cada roda e identificada por uma CHAVE DE CONTEXTO (o que decide o conjunto de comandos dela), montada
 * aqui pelo mesmo GamePanel que monta a roda:
 * - MOTOR/posicao/papel/familia[/JOELHOS]: as rodas do motor de solo (e as de queda em pe), pela posicao,
 *   pelo papel de quem a ve (CIMA, BAIXO, com _CTRL quando controla; NEUTRO/CTRL em pe) e pela familia
 *   (RS, R1 = RB, L1 = LB, L2 = LT, L1_L2 = LT+LB, R2 = RT); em pe, /JOELHOS quando alguem esta de joelhos;
 * - FIN/ATACA|DEFENDE/ENTRADA|DISPUTA: a roda durante uma finalizacao; MOTOR/CAINDO: como receber a queda;
 * - EMPE/PEGADAS/papel/camada: as pegadas em pe (RS, LT, LB, LT+LB); EMPE/DEFESAS e EMPE/QUEDAS (RT/RB).
 *
 * A tabela chave -> codigo fica no arquivo {@code /dev/rodas.txt} (versionado): os codigos NAO sao
 * calculados em tempo de execucao, entao nao mudam entre execucoes nem com a ordem de listas. Roda nova:
 * acrescentar uma linha com o PROXIMO codigo livre (nunca reutilizar). O teste IdentificacaoRodasTest
 * garante que toda roda possivel tem codigo e que nenhum codigo se repete.
 */
public final class IdentificacaoRodas {

    public static final String ARQUIVO = "/dev/rodas.txt";
    private static final Map<String, String> CODIGOS = carregar();

    private IdentificacaoRodas() {
    }

    // =====================================================
    // CHAVES DE CONTEXTO
    // =====================================================
    /** Papel de quem ve a roda neste estado (relativo ao lado: A e B veem a mesma chave). */
    public static String papel(EstadoPosicional e, Lado l) {
        Lado cima = e.getPorCima().orElse(null);
        Lado ctrl = e.getControlador().orElse(null);
        String base = cima == null ? "" : cima == l ? "CIMA" : "BAIXO";
        String c = ctrl == l ? "CTRL" : "";
        if (base.isEmpty()) {
            return c.isEmpty() ? "NEUTRO" : c;
        }
        return c.isEmpty() ? base : base + "_" + c;
    }

    public static String motor(EstadoPosicional e, Lado l, Familia f, boolean deJoelhos) {
        return "MOTOR/" + e.getPosicao().name() + "/" + papel(e, l) + "/" + f.name()
                + (!e.getPosicao().isNoChao() && deJoelhos ? "/JOELHOS" : "");
    }

    public static String caindo() {
        return "MOTOR/CAINDO";
    }

    public static String finalizacao(boolean atacante, boolean disputa) {
        return "FIN/" + (atacante ? "ATACA" : "DEFENDE") + "/" + (disputa ? "DISPUTA" : "ENTRADA");
    }

    public static String familiaEmPe(FamiliaTecnica f) {
        return "EMPE/" + (f == FamiliaTecnica.DEFESAS ? "DEFESAS" : "QUEDAS");
    }

    public static String pegadasEmPe(PapelCombate p, CamadaComando c) {
        return "EMPE/PEGADAS/" + p.name() + "/" + c.name();
    }

    public static String semMotor() {
        return "SEMMOTOR/SOLO";
    }

    /** "RODA: M-026" (ou "RODA: ? chave" se faltar no registro, para o desenvolvedor acrescentar). */
    public static String rotulo(String chave) {
        return codigo(chave).map(c -> "RODA: " + c).orElse("RODA: ? " + chave);
    }

    public static Optional<String> codigo(String chave) {
        return chave == null ? Optional.empty() : Optional.ofNullable(CODIGOS.get(chave));
    }

    /** O registro inteiro, na ordem do arquivo (testes e documentacao). */
    public static Map<String, String> registro() {
        return Collections.unmodifiableMap(CODIGOS);
    }

    private static Map<String, String> carregar() {
        Map<String, String> m = new LinkedHashMap<>();
        try (InputStream in = IdentificacaoRodas.class.getResourceAsStream(ARQUIVO)) {
            if (in == null) {
                return m;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String linha = r.readLine(); linha != null; linha = r.readLine()) {
                String t = linha.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                String[] p = t.split("\\s+", 2);          // "M-001  EMPE/PEGADAS/LIVRE/UM"
                if (p.length == 2) {
                    m.put(p[1].trim(), p[0].trim());
                }
            }
        } catch (IOException ex) {
            return m;
        }
        return m;
    }

    // =====================================================
    // TODAS AS RODAS POSSIVEIS (para o teste e a documentacao; o jogo nao usa)
    // =====================================================
    /** Toda chave de roda que o jogo pode abrir hoje, numa ordem fixa e legivel. */
    public static List<String> todasAsChavesPossiveis() {
        Set<String> s = new LinkedHashSet<>();
        for (PapelCombate p : PapelCombate.values()) {
            for (CamadaComando c : CamadaComando.values()) {
                s.add(pegadasEmPe(p, c));
            }
        }
        s.add(familiaEmPe(FamiliaTecnica.DEFESAS));
        s.add(familiaEmPe(FamiliaTecnica.PROJECOES));
        s.add(semMotor());
        s.add(caindo());
        for (boolean atacante : new boolean[] {true, false}) {
            for (boolean disputa : new boolean[] {false, true}) {
                s.add(finalizacao(atacante, disputa));
            }
        }
        CatalogoTecnicasSolo cat = CatalogoTecnicasSolo.referencia();
        CatalogoFinalizacoes fin = CatalogoFinalizacoes.referencia();
        RegistroFinalizacoes reg = RegistroFinalizacoes.referencia();
        for (EstadoPosicional e : estados()) {
            for (Lado l : Lado.values()) {
                for (Familia f : Familia.values()) {
                    for (boolean joelhos : e.getPosicao().isNoChao() ? new boolean[] {false} : new boolean[] {false, true}) {
                        if (temAlgo(cat, fin, reg, e, l, f, e.getPosicao().isNoChao() ? null : joelhos)) {
                            s.add(motor(e, l, f, joelhos));
                        }
                    }
                }
            }
        }
        return new ArrayList<>(s);
    }

    private static boolean temAlgo(CatalogoTecnicasSolo cat, CatalogoFinalizacoes fin, RegistroFinalizacoes reg,
            EstadoPosicional e, Lado l, Familia f, Boolean joelhos) {
        for (Direcao d : Direcao.values()) {
            ComandoSolo c = new ComandoSolo(d, f);
            if (cat.para(e, l, c, joelhos).isPresent() || fin.para(e, l, c).isPresent() || reg.para(e, l, c).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static List<EstadoPosicional> estados() {
        List<EstadoPosicional> l = new ArrayList<>();
        Lado[] opcoes = {null, Lado.A, Lado.B};
        for (Posicao p : Posicao.values()) {
            for (Lado cima : opcoes) {
                for (Lado ctrl : opcoes) {
                    if (!existeNoJogo(p, cima, ctrl)) {
                        continue;
                    }
                    try {
                        l.add(p == Posicao.EM_PE_SEPARADOS ? EstadoPosicional.separados()
                                : p == Posicao.EM_PE_PEGADA ? EstadoPosicional.pegada(ctrl)
                                : EstadoPosicional.noChao(p, cima, ctrl));
                    } catch (IllegalArgumentException invalido) {
                        // combinacao que nao existe
                    }
                }
            }
        }
        return l;
    }

    /**
     * Papeis que o motor de fato produz em cada posicao (os destinos das tecnicas e das quedas): dominio
     * (cem quilos, joelho na barriga, montada, costas) = quem esta por cima controla; guardas e ashi = quem
     * esta por baixo controla; um em pe e outro no chao = quem esta em pe controla ou ninguem; solo
     * disputado e em pe separados = ninguem; pegada em pe = um dos dois ou ninguem.
     */
    static boolean existeNoJogo(Posicao p, Lado cima, Lado ctrl) {
        return switch (p) {
            case EM_PE_SEPARADOS, SOLO_DISPUTADO -> cima == null && ctrl == null;
            case EM_PE_PEGADA -> cima == null;
            case CONTROLE_LATERAL, JOELHO_BARRIGA, MONTADA, COSTAS -> cima != null && ctrl == cima;
            case EM_PE_CONTRA_SOLO -> cima != null && (ctrl == null || ctrl == cima);
            default -> cima != null && ctrl == cima.outro();          // guardas e ashi: quem esta por baixo controla
        };
    }

    /** Controles que abrem a familia (documentacao). */
    public static String controles(Familia f) {
        return switch (f) {
            case RS -> "RS";
            case R1 -> "RB + RS";
            case L1 -> "LB + RS";
            case L2 -> "LT + RS";
            case L1_L2 -> "LT + LB + RS";
            case R2 -> "RT + RS";
        };
    }
}
