package com.irmaosmiau.posicional;

import com.irmaosmiau.posicional.Desfecho.RegraLado;
import com.irmaosmiau.posicional.Finalizacao.Requisito;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Entrega 5Q: o repertorio de finalizacoes REAIS, por posicao.
 *
 * L1 = chaves de membros superiores; L2 = estrangulamentos; L1+L2 = ataques as
 * pernas, que so existem a partir do CONTROLE DE PERNA (ashi garami). Nas
 * posicoes de controle (guarda fechada, cem quilos, montada, costas) o L1+L2
 * e uma ENTRADA no controle de perna (TecnicaSolo do CatalogoTecnicasSolo):
 * quem ataca abandona a posicao, disputa a perna e so depois finaliza.
 *
 * Baseado no "Manual tecnico de finalizacoes e transicoes" (5Q), com estes
 * ajustes: no ashi garami a direcao escolhe a chave (^ pe reta, -> joelho,
 * v mao de vaca, <- panturrilha) para o jogador memorizar um mapa so; nada de
 * heel hook.
 */
public final class CatalogoFinalizacoes {

    private static final RegraLado INI = RegraLado.INICIADOR;
    private static final RegraLado OUT = RegraLado.OUTRO;
    private static final RegraLado NADA = RegraLado.NENHUM;
    private static final RegraLado MANTEM = RegraLado.MANTEM;

    private final Map<String, Finalizacao> porId = new LinkedHashMap<>();

    private CatalogoFinalizacoes() {
    }

    public Collection<Finalizacao> todas() {
        return porId.values();
    }

    public Optional<Finalizacao> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }

    /** A finalizacao deste comando para este lado nesta posicao (no maximo uma). */
    public Optional<Finalizacao> para(EstadoPosicional e, Lado lado, ComandoSolo c) {
        return porId.values().stream()
                .filter(f -> f.posicao() == e.getPosicao() && e.temPapel(lado, f.papel()) && f.aceita(c))
                .findFirst();
    }

    public Optional<Finalizacao> naPosicao(Posicao p, Familia fam, Direcao d) {
        return porId.values().stream()
                .filter(f -> f.posicao() == p && f.familia() == fam && f.direcao() == d).findFirst();
    }

    private static Desfecho para(Posicao p, RegraLado cima, RegraLado ctrl) {
        return new Desfecho(p, cima, ctrl);
    }

    private static final Desfecho FICA = null;              // falha: a posicao continua (quem atacou fica exposto)

    private Posicao pos;
    private PapelPosicional papel;
    private Desfecho escapePadrao;

    private void em(Posicao p, PapelPosicional papel, Desfecho escape) {
        this.pos = p;
        this.papel = papel;
        this.escapePadrao = escape;
    }

    private void f(Familia fam, Direcao d, String base, String nome, String rotulo, CategoriaFinalizacao cat,
            Graduacao g, Requisito req, double limiar, Desfecho falha) {
        // 5Q.5: valores centralizados em ParametrosFinalizacao
        double custo = cat == CategoriaFinalizacao.PERNA ? ParametrosFinalizacao.CUSTO_PERNA : ParametrosFinalizacao.CUSTO_OUTRAS;
        int janela = ParametrosFinalizacao.janelaCatalogo(g);
        double ganho = ParametrosFinalizacao.ganhoCatalogo(cat);
        String id = base + "@" + pos.name().toLowerCase(java.util.Locale.ROOT);
        Finalizacao fz = new Finalizacao(id, base, nome, rotulo, cat, g, pos, papel, fam, d, req, limiar, custo, janela,
                ganho, escapePadrao, falha);
        if (porId.putIfAbsent(id, fz) != null) {
            throw new IllegalArgumentException("Finalizacao repetida: " + id);
        }
    }

    public static CatalogoFinalizacoes referencia() {
        CatalogoFinalizacoes c = new CatalogoFinalizacoes();
        CategoriaFinalizacao br = CategoriaFinalizacao.BRACO;
        CategoriaFinalizacao es = CategoriaFinalizacao.ESTRANGULAMENTO;
        CategoriaFinalizacao pe = CategoriaFinalizacao.PERNA;
        Familia l1 = Familia.L1;            // ashi garami (5Q.6): L1 = joelho/quadril
        Familia l2 = Familia.L2;
        Familia l12 = Familia.L1_L2;
        Direcao cima = Direcao.CIMA;
        Direcao dir = Direcao.DIREITA;
        Direcao baixo = Direcao.BAIXO;
        Direcao esq = Direcao.ESQUERDA;
        Graduacao BR = Graduacao.BRANCA;
        Graduacao AZ = Graduacao.AZUL;
        Graduacao RX = Graduacao.ROXA;
        Graduacao MR = Graduacao.MARROM;
        Requisito sem = Requisito.OUTRO_SEM_POSTURA;
        Requisito exp = Requisito.OUTRO_EXPOSTO;
        Requisito ctl = Requisito.CONTROLE;
        Requisito manga = Requisito.MANGA;

        // ================= GUARDA FECHADA (quem esta por baixo controla)
        // escape: o outro tira o braco/cabeca e a guarda abre; omoplata/triangulo que falham abrem a guarda
        Desfecho abriu = para(Posicao.GUARDA_ABERTA, OUT, INI);
        c.em(Posicao.GUARDA, PapelPosicional.CONTROLADOR, abriu);
        c.f(l1, cima, "armlock", "Armlock", "ARMLOCK", br, BR, sem, 0, FICA);
        c.f(l1, dir, "kimura", "Kimura", "KIMURA", br, BR, sem, 0, FICA);
        c.f(l1, baixo, "omoplata", "Omoplata", "OMOPLATA", br, AZ, sem, 0, abriu);
        c.f(l1, esq, "chave_punho", "Chave de punho", "PUNHO", br, AZ, ctl, 40, FICA);
        c.f(l2, cima, "cruzado", "Estrangulamento cruzado", "CRUZADO", es, BR, ctl, 40, FICA);
        c.f(l2, dir, "triangulo", "Triângulo", "TRIÂNGULO", es, BR, sem, 0, abriu);
        c.f(l2, baixo, "guilhotina", "Guilhotina", "GUILHOT.", es, BR, exp, 0, FICA);
        c.f(l2, esq, "loop_choke", "Estrangulamento em laço (loop choke)", "LOOP", es, RX, ctl, 40, FICA);
        // 5Q.8: LT + LB da guarda fechada (antes so o espaco reservado "? PERNA"): ataques de canela/perna
        // por cima do ombro e do braco, e o ezequiel por baixo. Os "-platas" que falham abrem a guarda.
        c.f(l12, cima, "gogoplata", "Gogoplata (canela no pescoço)", "GOGOPLATA", es, MR, sem, 0, abriu);
        c.f(l12, dir, "baratoplata", "Baratoplata (ombro pela perna)", "BARATOPL.", br, RX, sem, 0, abriu);
        c.f(l12, baixo, "tarikoplata", "Tarikoplata (ombro com a perna)", "TARIKOPL.", br, MR, sem, 0, abriu);
        c.f(l12, esq, "ezequiel_guarda", "Ezequiel da guarda", "EZEQUIEL", es, AZ, ctl, 40, FICA);

        // ================= CEM QUILOS (quem controla, por cima)
        Desfecho recuperouGuarda = para(Posicao.GUARDA_ABERTA, INI, OUT);
        c.em(Posicao.CONTROLE_LATERAL, PapelPosicional.CONTROLADOR, recuperouGuarda);
        c.f(l1, cima, "americana", "Americana", "AMERICANA", br, BR, ctl, 40, FICA);
        c.f(l1, dir, "kimura", "Kimura", "KIMURA", br, BR, ctl, 40, FICA);
        c.f(l1, baixo, "armlock", "Armlock (girando)", "ARMLOCK", br, AZ, ctl, 60, recuperouGuarda);
        c.f(l1, esq, "braco_reto", "Chave de braço reto", "BRAÇO RETO", br, AZ, ctl, 40, FICA);
        c.f(l2, cima, "cortando_pao", "Estrangulamento cortando o pão", "PÃO", es, AZ, ctl, 40, FICA);
        c.f(l2, dir, "katagatame", "Katagatame", "KATA-GAT.", es, AZ, ctl, 50, FICA);
        c.f(l2, baixo, "ezequiel", "Ezequiel", "EZEQUIEL", es, RX, ctl, 60, FICA);
        c.f(l2, esq, "baseball_bat", "Estrangulamento baseball bat", "BASEBALL", es, RX, ctl, 60, FICA);
        // 5Q.8: LT + LB dos cem quilos (antes "? PERNA"): estrangulamentos de braco e cabeca e o norte-sul.
        // D'arce e anaconda que falham deixam o outro recuperar a guarda.
        c.f(l12, cima, "norte_sul", "Estrangulamento norte-sul", "NORTE-SUL", es, RX, ctl, 50, FICA);
        c.f(l12, dir, "darce", "D'arce", "D'ARCE", es, RX, ctl, 55, recuperouGuarda);
        c.f(l12, baixo, "anaconda", "Anaconda", "ANACONDA", es, MR, ctl, 55, recuperouGuarda);
        c.f(l12, esq, "gravata_peruana", "Gravata peruana (peruvian necktie)", "PERUANA", es, MR, ctl, 60, FICA);

        // ================= MONTADA (quem controla)
        Desfecho meia = para(Posicao.MEIA_GUARDA, INI, OUT);
        Desfecho perdeuMontada = para(Posicao.GUARDA, OUT, INI);          // o outro fica por cima, na guarda de quem atacou
        c.em(Posicao.MONTADA, PapelPosicional.CONTROLADOR, meia);
        c.f(l1, cima, "armlock", "Armlock da montada", "ARMLOCK", br, BR, ctl, 50, perdeuMontada);
        c.f(l1, dir, "americana", "Americana", "AMERICANA", br, BR, ctl, 40, FICA);
        c.f(l1, baixo, "kimura", "Kimura da montada", "KIMURA", br, AZ, ctl, 50, FICA);
        c.f(l1, esq, "chave_punho", "Chave de punho", "PUNHO", br, AZ, ctl, 40, FICA);
        c.f(l2, cima, "cruzado", "Estrangulamento cruzado", "CRUZADO", es, BR, ctl, 40, FICA);
        c.f(l2, dir, "ezequiel", "Ezequiel", "EZEQUIEL", es, AZ, ctl, 50, FICA);
        c.f(l2, baixo, "katagatame", "Katagatame", "KATA-GAT.", es, AZ, ctl, 50, FICA);
        c.f(l2, esq, "triangulo", "Triângulo da montada", "TRIÂNGULO", es, RX, ctl, 60, perdeuMontada);
        // 5Q.8: LT + LB da montada (antes "? PERNA"). Adaptacoes: o arm triangle ja existe (katagatame) e a
        // chave de punho tambem: no lugar, a gola unica e a mao de vaca (punho com as duas maos, outra entrada).
        c.f(l12, cima, "monoplata", "Monoplata", "MONOPLATA", br, MR, ctl, 55, perdeuMontada);
        c.f(l12, dir, "armlock_s", "Armlock da montada em S (barriga para baixo)", "ARMLOCK S", br, AZ, ctl, 55,
                perdeuMontada);
        c.f(l12, baixo, "gola_unica", "Estrangulamento de gola única", "GOLA ÚNICA", es, AZ, ctl, 45, FICA);
        c.f(l12, esq, "mao_de_vaca", "Mão de vaca (chave de punho)", "MÃO DE VACA", br, RX, ctl, 45, FICA);

        // ================= COSTAS (quem controla)
        Desfecho girou = para(Posicao.GUARDA, OUT, INI);
        c.em(Posicao.COSTAS, PapelPosicional.CONTROLADOR, girou);
        c.f(l1, cima, "armlock", "Armlock das costas", "ARMLOCK", br, AZ, ctl, 50, girou);
        c.f(l1, dir, "kimura", "Kimura das costas", "KIMURA", br, RX, ctl, 50, FICA);
        c.f(l1, baixo, "braco_reto", "Chave de braço reto", "BRAÇO RETO", br, AZ, ctl, 50, FICA);
        c.f(l1, esq, "hammerlock", "Hammerlock", "HAMMER", br, RX, ctl, 50, FICA);
        c.f(l2, cima, "mata_leao", "Mata-leão", "MATA-LEÃO", es, BR, ctl, 40, FICA);
        c.f(l2, dir, "arco_flecha", "Arco e flecha", "ARCO", es, AZ, ctl, 50, FICA);
        c.f(l2, baixo, "gola_tras", "Estrangulamento de gola por trás", "GOLA", es, AZ, ctl, 40, FICA);
        c.f(l2, esq, "cruzado_tras", "Estrangulamento cruzado pelas costas", "CRUZADO", es, RX, ctl, 50, FICA);

        // ================= CONTROLE DE PERNA (ashi garami): as chaves de perna
        c.em(Posicao.ASHI_GARAMI, PapelPosicional.CONTROLADOR, para(Posicao.SOLO_DISPUTADO, NADA, NADA));
        c.f(l12, cima, "chave_pe_reta", "Chave de pé reta", "PÉ RETA", pe, RX, ctl, 40, FICA);
        c.f(l12, dir, "chave_joelho", "Chave de joelho", "JOELHO", pe, MR, ctl, 50, FICA);
        // 5Q.6: o L1+L2 v era chamado de "Mao de vaca (toe hold)"; no jiu-jitsu mao de vaca e uma chave de
        // PUNHO. A tecnica (e o comando) continua a mesma, com o nome certo: toe hold / mata-leao no pe
        c.f(l12, baixo, "toe_hold", "Toe hold (mata-leão no pé)", "TOE HOLD", pe, MR, ctl, 50, FICA);
        c.f(l12, esq, "panturrilha", "Compressão de panturrilha", "PANTURR.", pe, MR, ctl, 50, FICA);
        // 5Q.6: LT (L2) e LB (L1) no ashi: tornozelo/pe no LT, joelho/quadril no LB. Sem heel hook (regra do
        // jogo: ModoRegras). Cada uma muda requisito, limiar, alvo da defesa ou o que acontece se falhar.
        Desfecho cinquenta = para(Posicao.SOLO_DISPUTADO, NADA, NADA);    // 50/50: defendida, ninguem controla
        Desfecho rolouErrado = para(Posicao.CONTROLE_LATERAL, OUT, OUT);  // o giro falhou: o outro passa por cima
        c.f(l2, cima, "aoki_lock", "Aoki lock (chave de tornozelo girando)", "AOKI", pe, RX, ctl, 45, FICA);
        c.f(l2, dir, "estima_lock", "Estima lock (chave de pé)", "ESTIMA", pe, MR, ctl, 50, FICA);
        c.f(l2, baixo, "pe_reta_5050", "Chave de pé reta da 50/50 (variação)", "PÉ 50/50", pe, RX, ctl, 30, cinquenta);
        c.f(l1, cima, "joelho_invertido", "Chave de joelho invertida", "JOELHO INV.", pe, MR, ctl, 55, FICA);
        c.f(l1, dir, "banana_split", "Banana split (abrir as pernas)", "BANANA", pe, MR, ctl, 50, FICA);
        c.f(l1, baixo, "kneebar_giro", "Chave de joelho com giro (variação)", "JOELHO GIRO", pe, MR, ctl, 35,
                rolouErrado);

        // ================= GUARDA ARANHA e LACO (por baixo, com a manga)
        Desfecho aberta = para(Posicao.GUARDA_ABERTA, MANTEM, MANTEM);
        c.em(Posicao.GUARDA_ARANHA, PapelPosicional.POR_BAIXO, aberta);
        // (L1 ^ e L1 v na aranha/laco sao trocas de guarda: os ataques ficam em L1 <- / L1 -> / L2 ->)
        c.f(l1, dir, "armlock", "Armlock da aranha", "ARMLOCK", br, AZ, manga, 0, aberta);
        c.f(l1, esq, "omoplata", "Omoplata da aranha", "OMOPLATA", br, AZ, manga, 0, aberta);
        c.f(l2, dir, "triangulo", "Triângulo da aranha", "TRIÂNGULO", es, AZ, manga, 0, aberta);
        c.em(Posicao.GUARDA_LACO, PapelPosicional.POR_BAIXO, aberta);
        c.f(l1, esq, "omoplata", "Omoplata do laço", "OMOPLATA", br, AZ, manga, 0, aberta);
        return c;
    }
}
