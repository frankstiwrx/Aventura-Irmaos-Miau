package com.irmaosmiau.regras;

import com.irmaosmiau.controles.NaturezaAcao;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.posicional.CatalogoTecnicasSolo;
import com.irmaosmiau.posicional.ClassificacaoAcoes;
import com.irmaosmiau.posicional.EstadoPosicional;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.Posicao;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Arbitragem do modo PONTOS + FINALIZACAO (adaptacao do Livro de Regras da IBJJF, jun/2024).
 *
 * O MotorSolo continua sendo quem executa as tecnicas e muda as posicoes; o arbitro so OBSERVA, uma vez por
 * tick de luta (nunca na pausa), o estado posicional, o ataque de finalizacao e os eventos do motor, e decide
 * pontos, vantagens e o resultado. Nada aqui muda a luta.
 *
 * Pontos (art. 2.5.2 e 4), sempre com estabilizacao de 3 segundos de tempo de luta (art. 3.1):
 * - QUEDA 2: da posicao em pe para o chao com quem derrubou por cima, causado por uma tecnica de queda DELE
 *   (evento do motor; puxada de guarda nao conta, 4.1.11). Derrubar quem estava de joelhos so vale se quem
 *   derruba estava em pe (4.1.6). Contra-queda (reversao) vale para quem reverteu (4.1.7).
 * - RASPAGEM 2: quem estava por baixo numa guarda (incluindo meia, aberta, borboleta, De La Riva, laco,
 *   aranha, single-leg X, ashi e o outro em pe na frente das pernas) termina por cima (4.6).
 * - PASSAGEM 3: quem estava por cima na guarda chega aos cem quilos, joelho na barriga ou montada (4.2).
 * - JOELHO NA BARRIGA 2, MONTADA 4, COSTAS 4: a posicao do motor com quem a controla (4.3 a 4.5).
 * Progressao (3.4): passagem seguida de montada soma 3 + 4, contando so os 3 segundos do fim da sequencia.
 * Costas para montada e vice-versa valem as duas (4.4.1).
 *
 * Sem pontos duplicados: queda e raspagem saem de uma TRANSICAO (uma vez); as posicoes ficam na memoria de
 * quem domina e so voltam a valer depois que ele perde o dominio por acao do outro (escape, raspagem...).
 * Largar a posicao por vontade propria nao "zera" a memoria (3.2).
 *
 * Finalizacao durante a contagem (3.3): chegando a posicao preso numa finalizacao, a contagem so comeca
 * depois de escapar; se o outro prende uma finalizacao durante a contagem, ela e interrompida e vira uma
 * vantagem por posicao conquistada (3.3.2).
 *
 * Vantagens (art. 5), so com criterio verificavel: posicao pontuavel alcancada mas nao estabilizada (5.1/5.6)
 * e finalizacao com perigo real (5.3: fase de disputa com pressao >= {@link #PRESSAO_PERIGO_REAL} ou na
 * ponta). Punicoes (art. 6/7): o jogo ainda nao reconhece nenhuma infracao com seguranca; o placar tem a
 * escala pronta ({@link Placar#punir}), mas nada pune automaticamente.
 */
public final class ArbitroJiuJitsu {

    /** Estabilizacao (art. 3.1). */
    public static final int MS_ESTABILIZACAO = 3000;
    /** Pressao a partir da qual uma finalizacao poe o adversario em perigo real (art. 5.3). */
    public static final double PRESSAO_PERIGO_REAL = 70;
    /** Quanto tempo (observacoes) depois da tecnica de queda a aterrissagem ainda e dela. */
    static final int JANELA_QUEDA = 300;

    private static final Set<Posicao> DOMINANTES = EnumSet.of(Posicao.CONTROLE_LATERAL, Posicao.JOELHO_BARRIGA,
            Posicao.MONTADA, Posicao.COSTAS);
    private static final Set<String> PUXADAS = Set.of(CatalogoTecnicasSolo.PUXAR_GUARDA,
            CatalogoTecnicasSolo.TIRO_GUARDA, CatalogoTecnicasSolo.PUXAR_PARA_GUARDA,
            CatalogoTecnicasSolo.CONECTAR_GUARDA, CatalogoTecnicasSolo.PUXAR_ARANHA, CatalogoTecnicasSolo.PUXAR_LACO,
            CatalogoTecnicasSolo.PUXAR_BORBOLETA, CatalogoTecnicasSolo.PUXAR_DLR,
            CatalogoTecnicasSolo.CONECTAR_BORBOLETA, CatalogoTecnicasSolo.CONECTAR_DLR);
    private static final CatalogoTecnicasSolo CATALOGO = CatalogoTecnicasSolo.referencia();

    /** Um aviso para o placar: pontos concedidos ou vantagem. */
    public record Aviso(Lado lado, String texto, NaturezaAcao natureza) {
    }

    private record AcaoEmPe(Lado lado, boolean queda, boolean vitimaAjoelhada, boolean atacanteEmPe, long quando) {
    }

    private final int ticksEstabilizacao;
    private final Placar placar = new Placar();
    private final List<Aviso> avisos = new ArrayList<>();
    private final Map<Lado, EnumSet<Pontuacao>> jaPontuadas = new EnumMap<>(Lado.class);
    private final Map<Lado, Boolean> ajoelhadoAnt = new EnumMap<>(Lado.class);
    private final Map<Lado, Boolean> emPeAnt = new EnumMap<>(Lado.class);

    private boolean iniciado;
    private long observacoes;
    private Posicao posAnt;
    private Lado cimaAnt;
    private Lado ctrlAnt;
    private MotorSolo.Evento ultimoEvento;
    private AcaoEmPe acaoEmPe;

    private Lado pendLado;
    private final List<Pontuacao> pendItens = new ArrayList<>();
    private int pendTicks;
    private boolean pendSobAtaque;
    private boolean pendVantagemDada;

    private MotorSolo.Ataque ataqueAnt;
    private boolean perigoReal;
    /** O motor estava na aterrissagem de uma queda (a fase "um em pe, outro no chao" da propria queda). */
    private boolean aterrissandoAnt;

    /** @param msPorTick duracao de um tick de luta (o GamePanel usa 16 ms) */
    public ArbitroJiuJitsu(int msPorTick) {
        this.ticksEstabilizacao = (int) Math.ceil(MS_ESTABILIZACAO / (double) msPorTick);
        for (Lado l : Lado.values()) {
            jaPontuadas.put(l, EnumSet.noneOf(Pontuacao.class));
        }
    }

    public Placar getPlacar() {
        return placar;
    }

    public int getTicksEstabilizacao() {
        return ticksEstabilizacao;
    }

    /** Avisos novos desde a ultima consulta (so pontos e vantagens ja concedidos). */
    public List<Aviso> consumirAvisos() {
        List<Aviso> l = List.copyOf(avisos);
        avisos.clear();
        return l;
    }

    /** Quem esta estabilizando agora e ha quantos ticks (HUD e testes; vazio = ninguem). */
    public java.util.Optional<Lado> getEstabilizando() {
        return java.util.Optional.ofNullable(pendLado);
    }

    public int getTicksEstabilizados() {
        return pendTicks;
    }

    // =====================================================
    // OBSERVACAO (um tick de luta)
    // =====================================================
    public void observar(MotorSolo m) {
        observacoes++;
        EstadoPosicional e = m.getEstado();
        Posicao p = e.getPosicao();
        Lado cima = e.getPorCima().orElse(null);
        Lado ctrl = e.getControlador().orElse(null);
        MotorSolo.Ataque at = m.getAtaque().orElse(null);
        List<MotorSolo.Evento> novos = novosEventos(m);
        if (!iniciado) {
            iniciado = true;                                   // a posicao inicial e so a referencia
            guardar(m, p, cima, ctrl, at);
            return;
        }
        if (!p.isNoChao() || !posAnt.isNoChao()) {
            for (MotorSolo.Evento ev : novos) {
                acaoEmPe(ev);
            }
        }
        observarAtaque(at, m);
        boolean mudou = p != posAnt || !Objects.equals(cima, cimaAnt) || !Objects.equals(ctrl, ctrlAnt);
        if (mudou) {
            atualizarMemoria(p, ctrl, novos);
            conquistas(p, cima, ctrl, at);
        }
        avancarPendente(p, cima, ctrl, at);
        guardar(m, p, cima, ctrl, at);
    }

    private List<MotorSolo.Evento> novosEventos(MotorSolo m) {
        List<MotorSolo.Evento> todos = m.eventos();
        int desde = 0;
        if (ultimoEvento != null) {
            for (int i = todos.size() - 1; i >= 0; i--) {
                if (todos.get(i) == ultimoEvento) {
                    desde = i + 1;
                    break;
                }
            }
        }
        List<MotorSolo.Evento> n = new ArrayList<>(todos.subList(desde, todos.size()));
        if (!todos.isEmpty()) {
            ultimoEvento = todos.get(todos.size() - 1);
        }
        return iniciado ? n : List.of();
    }

    private void guardar(MotorSolo m, Posicao p, Lado cima, Lado ctrl, MotorSolo.Ataque at) {
        posAnt = p;
        aterrissandoAnt = m.isAterrissando();
        cimaAnt = cima;
        ctrlAnt = ctrl;
        ataqueAnt = at;
        for (Lado l : Lado.values()) {
            IrmaoMiau x = m.lutador(l);
            ajoelhadoAnt.put(l, x.isAjoelhado());
            emPeAnt.put(l, !x.isAjoelhado() && !x.isDeitado());
        }
    }

    /** Queda (tecnica de queda bem-sucedida ou contra-queda) ou puxada de guarda, ainda em pe. */
    private void acaoEmPe(MotorSolo.Evento ev) {
        if (ev.tecnica() == null) {
            return;
        }
        if (PUXADAS.contains(ev.tecnica()) || "SOLO_LIVRE".equals(ev.tipo())) {
            acaoEmPe = new AcaoEmPe(ev.lado(), false, false, false, observacoes);
            return;
        }
        boolean tipoOk = "SUCESSO".equals(ev.tipo()) || "REVERSAO".equals(ev.tipo()) || "CONTRA".equals(ev.tipo());
        if (tipoOk && ehQueda(ev)) {
            Lado vit = ev.lado().outro();
            acaoEmPe = new AcaoEmPe(ev.lado(), true, ajoelhadoAnt.getOrDefault(vit, false),
                    emPeAnt.getOrDefault(ev.lado(), true), observacoes);
        }
    }

    private static boolean ehQueda(MotorSolo.Evento ev) {
        String d = ev.detalhe() == null ? "" : ev.detalhe();
        if (d.startsWith("desequilibrou") || d.contains("sem controle")) {
            return false;
        }
        if ("REVERSAO".equals(ev.tipo())) {
            return true;                                       // contra-queda / contra-quadril
        }
        if (CatalogoTecnicasSolo.EMPURRAO.equals(ev.tecnica())) {
            return d.startsWith("derrubou");
        }
        return CATALOGO.buscar(ev.tecnica()).map(t -> ClassificacaoAcoes.de(t) == NaturezaAcao.QUEDA).orElse(false);
    }

    /** Vantagem por finalizacao com perigo real (5.3), quando o ataque acaba sem vitoria. */
    private void observarAtaque(MotorSolo.Ataque at, MotorSolo m) {
        if (at != ataqueAnt) {
            if (ataqueAnt != null && perigoReal && m.getVitoria().isEmpty()) {
                vantagem(ataqueAnt.atacante(), "FINALIZAÇÃO QUASE");
            }
            perigoReal = false;
        }
        if (at != null && at.fase() == MotorSolo.FaseFinalizacao.DISPUTA
                && (at.pressao() >= PRESSAO_PERIGO_REAL || at.naPonta())) {
            perigoReal = true;
        }
    }

    /** Quem perdeu o dominio por acao do outro pode voltar a pontuar as posicoes (3.2). */
    private void atualizarMemoria(Posicao p, Lado ctrl, List<MotorSolo.Evento> novos) {
        for (Lado l : Lado.values()) {
            boolean dominaAgora = DOMINANTES.contains(p) && ctrl == l;
            boolean voluntario = novos.stream().anyMatch(ev -> ev.lado() == l && "SUCESSO".equals(ev.tipo()));
            if (!dominaAgora && !voluntario) {
                jaPontuadas.get(l).clear();
            }
        }
    }

    private static boolean comGuarda(Posicao p) {
        return p.isGuarda() || p == Posicao.EM_PE_CONTRA_SOLO || p == Posicao.ASHI_GARAMI;
    }

    /** O que esta transicao conquistou (pode ser mais de uma coisa: passagem + montada). */
    private void conquistas(Posicao p, Lado cima, Lado ctrl, MotorSolo.Ataque at) {
        Lado x = null;
        List<Pontuacao> itens = new ArrayList<>();
        boolean porCima = cima != null && p.exigeAlguemPorCima();
        // QUEDA: de pe para o chao, quem derrubou por cima, pela tecnica dele
        if (!posAnt.isNoChao() && p.isNoChao() && porCima && acaoEmPe != null && acaoEmPe.queda()
                && acaoEmPe.lado() == cima && observacoes - acaoEmPe.quando() <= JANELA_QUEDA
                && !(acaoEmPe.vitimaAjoelhada() && !acaoEmPe.atacanteEmPe())) {
            x = cima;
            itens.add(Pontuacao.QUEDA);
            acaoEmPe = null;
        }
        // a aterrissagem da queda passa por "um em pe, outro no chao": isso nao e guarda; cair direto nos cem
        // quilos vale so a queda (obs. do art. 3.5), nao passagem nem raspagem
        boolean guardaDeVerdade = comGuarda(posAnt) && !aterrissandoAnt;
        // RASPAGEM: quem estava por baixo na guarda termina por cima
        if (x == null && guardaDeVerdade && cimaAnt != null && porCima && cima == cimaAnt.outro()) {
            x = cima;
            itens.add(Pontuacao.RASPAGEM);
        }
        // PASSAGEM: quem estava por cima na guarda chega ao dominio (nao nas costas)
        if (x == null && guardaDeVerdade && cimaAnt != null && DOMINANTES.contains(p) && p != Posicao.COSTAS
                && ctrl == cimaAnt) {
            x = ctrl;
            itens.add(Pontuacao.PASSAGEM);
        }
        // posicoes de dominio (montada, costas, joelho na barriga)
        Pontuacao pos = switch (p) {
            case MONTADA -> Pontuacao.MONTADA;
            case COSTAS -> Pontuacao.COSTAS;
            case JOELHO_BARRIGA -> Pontuacao.JOELHO_BARRIGA;
            default -> null;
        };
        if (pos != null && ctrl != null && (x == null || x == ctrl) && (p != posAnt || ctrl != ctrlAnt)) {
            x = ctrl;
            itens.add(pos);
        }
        if (x == null) {
            return;
        }
        Lado quem = x;
        itens.removeIf(i -> i.isPosicional() && (jaPontuadas.get(quem).contains(i) || pendItens.contains(i)
                && pendLado == quem));
        if (itens.isEmpty()) {
            return;
        }
        if (pendLado == x) {
            pendItens.addAll(itens);                           // progressao: conta 3 s no fim (3.4)
            pendTicks = 0;
            return;
        }
        if (pendLado != null) {
            cancelarPendente(true);
        }
        pendLado = x;
        pendItens.addAll(itens);
        pendTicks = 0;
        pendVantagemDada = false;
        pendSobAtaque = at != null && at.defensor() == x;   // chegou preso numa finalizacao (3.3)
    }

    private boolean valeAinda(Pontuacao i, Posicao p, Lado cima, Lado ctrl) {
        Lado x = pendLado;
        return switch (i) {
            case QUEDA, RASPAGEM -> cima == x && p.exigeAlguemPorCima();
            case PASSAGEM -> DOMINANTES.contains(p) && ctrl == x;
            case MONTADA -> p == Posicao.MONTADA && ctrl == x;
            case COSTAS -> p == Posicao.COSTAS && ctrl == x;
            case JOELHO_BARRIGA -> p == Posicao.JOELHO_BARRIGA && ctrl == x;
        };
    }

    private void avancarPendente(Posicao p, Lado cima, Lado ctrl, MotorSolo.Ataque at) {
        if (pendLado == null) {
            return;
        }
        Lado x = pendLado;
        boolean perdeu = pendItens.removeIf(i -> !valeAinda(i, p, cima, ctrl));
        if (perdeu && !pendSobAtaque && !pendVantagemDada) {
            vantagem(x, "QUASE " + (pendItens.isEmpty() ? "PONTUOU" : "MAIS PONTOS"));   // 5.1 / 5.6
            pendVantagemDada = true;
        }
        if (pendItens.isEmpty()) {
            limparPendente();
            return;
        }
        boolean preso = at != null && at.defensor() == x;
        if (preso && !pendSobAtaque) {
            // 3.3.2: o outro prendeu uma finalizacao durante a contagem: uma vantagem por posicao
            for (int i = 0; i < pendItens.size(); i++) {
                vantagem(x, "CONTAGEM INTERROMPIDA");
            }
            limparPendente();
            return;
        }
        if (preso) {
            pendTicks = 0;                                     // 3.3: so conta depois de escapar
            return;
        }
        if (pendSobAtaque) {
            pendSobAtaque = false;                             // escapou: comeca a contar agora
            pendTicks = 0;
        }
        if (++pendTicks >= ticksEstabilizacao) {
            for (Pontuacao i : pendItens) {
                placar.somarPontos(x, i.getPontos());
                if (i.isPosicional()) {
                    jaPontuadas.get(x).add(i);
                }
                avisos.add(new Aviso(x, i.aviso(), i == Pontuacao.QUEDA ? NaturezaAcao.QUEDA : NaturezaAcao.CONTROLE));
            }
            limparPendente();
        }
    }

    private void cancelarPendente(boolean comVantagem) {
        if (comVantagem && !pendSobAtaque && !pendVantagemDada) {
            vantagem(pendLado, "QUASE PONTUOU");
        }
        limparPendente();
    }

    private void limparPendente() {
        pendLado = null;
        pendItens.clear();
        pendTicks = 0;
        pendSobAtaque = false;
        pendVantagemDada = false;
    }

    private void vantagem(Lado l, String motivo) {
        placar.somarVantagem(l);
        avisos.add(new Aviso(l, "VANTAGEM · " + motivo, NaturezaAcao.TRANSICAO));
    }

    // =====================================================
    // FIM DA LUTA
    // =====================================================
    /**
     * O tempo acabou: o que ainda nao estabilizou vira vantagem (3.3.2/3.3.3, 5.4), o mesmo para uma
     * finalizacao com perigo real em andamento; depois, o resultado pelo placar.
     */
    public ResultadoLuta encerrarPorTempo() {
        if (pendLado != null && !pendVantagemDada) {
            vantagem(pendLado, "POSIÇÃO SEM ESTABILIZAR");
        }
        limparPendente();
        if (ataqueAnt != null && perigoReal) {
            vantagem(ataqueAnt.atacante(), "FINALIZAÇÃO QUASE");
            perigoReal = false;
        }
        return ResultadoLuta.pelo(placar);
    }
}
