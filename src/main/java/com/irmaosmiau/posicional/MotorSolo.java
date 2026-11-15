package com.irmaosmiau.posicional;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiau.EstadoCorporal;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Criterio;
import com.irmaosmiau.posicional.TecnicaSolo.Exposicao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Entrega 5P: o motor de quedas e de solo. E o UNICO dono da relacao entre os
 * dois lutadores no chao (posicao, quem esta por cima, quem controla, a
 * tecnica em andamento) e o unico que muda postura, lugar e vinculo dos dois,
 * sempre JUNTOS e no mesmo tick. Assim J1 e J2 nunca discordam.
 *
 * Em pe ele so LE o SistemaCombate (separados / pegada estabelecida) e usa as
 * quedas (RB + RS). No chao o SistemaCombate fica sem interacao (a pegada em pe
 * acaba ao cair) e o MotorSolo conduz.
 *
 * Regra de ouro: toda transicao com janela expoe quem a tenta (Exposicao) por
 * alguns ticks; nessa janela o outro pode responder com uma contra do catalogo,
 * que resolve na hora. Sem resposta, o criterio decide (estabilidade do
 * controle, exposicao, postura, folego). Tudo deterministico.
 *
 * Lados: A = J1, B = J2 (os dois podem tudo; nenhum papel e fixo).
 */
public final class MotorSolo {

    // ---------------- parametros (um lugar so)
    // 5P.1: mais responsivo (antes 18 / 40 / 10)
    public static final int RECARGA_TICKS = 12;
    public static final int RECARGA_FALHA_TICKS = 28;
    /** Tempo para quem caiu escolher como recebe (fechar a guarda / meia). Antes 10 (~0,16 s). */
    public static final int ATERRISSAGEM_TICKS = 24;
    /** 5P.1: todas as janelas x 1,3 (mais tempo para responder; centralizado). */
    public static final double FATOR_JANELA = 1.3;
    /** 5P.1: respirando, as proprias tecnicas saem 25% mais devagar (prontidao menor). */
    public static final double FATOR_JANELA_RESPIRANDO = 1.25;
    /** 5P.1: levantar de quem deitou por vontade propria e mais rapido; derrubado, mais lento. */
    public static final double FATOR_LEVANTAR_VOLUNTARIO = 0.6;
    public static final double FATOR_LEVANTAR_DERRUBADO = 1.4;
    /** 5P.1: ticks "desorientado" depois de ser derrubado (so afeta o levantar e o controle inicial). */
    public static final int DESORIENTADO_TICKS = 60;
    /** 5P.1: estabilidade inicial de uma guarda puxada por quem estava no chao por vontade propria. */
    public static final double ESTABILIDADE_GUARDA_VOLUNTARIA = 60;
    /** 5P.1: quem controla e respira perde controle (por tick) em vez de ganhar. */
    public static final double PERDA_ESTABILIDADE_RESPIRANDO = 0.3;
    /** 5P.1: a base (RT) conta se for APERTADA na janela do outro (ou ate N ticks antes). */
    public static final int BUFFER_BASE_TICKS = 6;
    public static final int POSTURA_TICKS = 60;
    public static final int POSTURA_QUEBRADA_TICKS = 40;
    public static final int MONTADA_ALTA_EXPOSTA_TICKS = 20;
    public static final double GANHO_PASSIVO_ESTABILIDADE = 0.12;          // 5P.1: antes 0,2
    public static final double ESTABILIDADE_PROGRESSO = 50;
    public static final double ESTABILIDADE_REVERSAO = 30;
    public static final double DELTA_CONSOLIDAR = 25;
    public static final double DELTA_ENQUADRAR = 25;                       // 5P.1: antes 20
    public static final double FRACAO_FOLEGO_CANSADO = 0.25;
    public static final double FRACAO_FOLEGO_EXAUSTO = 0.15;
    // em pe
    public static final double BASE_MAXIMA = 100;
    public static final double RECUPERACAO_BASE = 1.2;
    public static final double DANO_EMPURRAO = 45;
    public static final double CUSTO_BASE_EMPURRAO = 20;
    public static final double BASE_MINIMA_PARA_EMPURRAR = 40;
    public static final int RECUO_EMPURRAO_PX = 34;
    public static final int ALCANCE_EMPURRAO_PX = 95;
    public static final int ALCANCE_PERNAS_PX = 120;
    public static final int ALCANCE_PUXAR_PX = 80;
    public static final int ALCANCE_ENTRAR_PX = 110;
    public static final int ALCANCE_CONECTAR_PX = 90;
    public static final double FIRMEZA_MINIMA_QUEDA = 30;
    public static final double BASE_DERRUBAVEL = 60;
    public static final int VELOCIDADE_ENCAIXE = 3;

    /** O que aconteceu com um comando. */
    public enum Resultado {
        INICIADA, CONCLUIDA, CONTRA, ABANDONADA, RECUSADA
    }

    public record Resposta(Resultado resultado, String motivo, TecnicaSolo tecnica) {
        public boolean aceito() {
            return resultado != Resultado.RECUSADA;
        }
    }

    /** Tecnica em andamento (na janela). */
    public record Tentativa(TecnicaSolo tecnica, Lado iniciador, int ticksRestantes, int janelaTotal, long inicio) {
        public double progresso() {
            return janelaTotal == 0 ? 1 : 1 - ticksRestantes / (double) janelaTotal;
        }
    }

    /** Evento (HUD, F3 e testes). */
    public record Evento(long tick, String tipo, String tecnica, Lado lado, String detalhe) {
        @Override
        public String toString() {
            return tick + " " + lado + " " + tipo + " " + tecnica + (detalhe.isEmpty() ? "" : " (" + detalhe + ")");
        }
    }

    /**
     * Intencao do jogador neste tick: LS (-1/0/1 por eixo) e se segura a BASE
     * (o RT / modificador de defesas: agarrado nao ha guarda, mas da para baixar
     * o centro e fazer base contra quedas e empurroes).
     */
    public record Intencao(int dx, int dy, boolean base) {
        public static final Intencao NENHUMA = new Intencao(0, 0, false);

        public Intencao(int dx, int dy) {
            this(dx, dy, false);
        }
    }

    /**
     * 5P.1: a BASE (RT) e uma reacao: conta se o RT foi APERTADO durante a janela
     * de quem esta entrando (ou ate BUFFER_BASE_TICKS antes). Segurar o RT de antes
     * e so respirar: assim respirar nao vira defesa sem querer, e defender nao e
     * so deixar o botao apertado. Livre, a guarda levantada (RT + RS) tambem vale.
     */
    private boolean fazBase(Lado l, long inicioJanela) {
        Long t = ultimoBase.get(l);
        return lutador(l).getDefesa().isAtiva() || (t != null && t >= inicioJanela - BUFFER_BASE_TICKS);
    }

    private final Map<Lado, Long> ultimoBase = new EnumMap<>(Lado.class);

    private record Exposto(Exposicao tipo, int ticks) {
    }

    private final IrmaoMiau[] lutadores = new IrmaoMiau[2];
    private final SistemaCombate sistema;
    private final CatalogoTecnicasSolo catalogo;
    private final int xMin;
    private final int xMax;

    private EstadoPosicional estado = EstadoPosicional.separados();
    private Tentativa tentativa;
    private Lado aterrissagemCima;
    private int aterrissagemTicks;
    private TecnicaSolo.Direcao reacaoAterrissagem;
    private double estabilidade;
    private int frente = 1;
    private long tick;
    private final Map<Lado, Exposto> exposicao = new EnumMap<>(Lado.class);
    private final Map<Lado, Integer> postura = new EnumMap<>(Lado.class);
    private final Map<Lado, Integer> recarga = new EnumMap<>(Lado.class);
    private final Map<Lado, Double> base = new EnumMap<>(Lado.class);
    private final Deque<Evento> eventos = new ArrayDeque<>();
    private Evento ultimo;

    public MotorSolo(IrmaoMiau a, IrmaoMiau b, SistemaCombate sistema, int xMin, int xMax) {
        if (a == null || b == null || a == b) {
            throw new IllegalArgumentException("O MotorSolo precisa de dois lutadores diferentes.");
        }
        lutadores[0] = a;
        lutadores[1] = b;
        this.sistema = sistema;
        this.catalogo = CatalogoTecnicasSolo.referencia();
        this.xMin = xMin;
        this.xMax = xMax;
        reiniciar();
    }

    /** Luta nova / F5: nenhum vinculo, ninguem exposto, todos com base. */
    public void reiniciar() {
        estado = EstadoPosicional.separados();
        tentativa = null;
        aterrissagemCima = null;
        aterrissagemTicks = 0;
        reacaoAterrissagem = null;
        estabilidade = 0;
        exposicao.clear();
        eventos.clear();
        ultimo = null;
        mensagens.clear();
        ultimoBase.clear();
        desorientado.clear();
        for (Lado l : Lado.values()) {
            postura.put(l, 0);
            recarga.put(l, 0);
            base.put(l, BASE_MAXIMA);
            lutador(l).setVinculadoNoSolo(false);
        }
    }

    // =====================================================
    // CONSULTAS
    // =====================================================
    public IrmaoMiau lutador(Lado l) {
        return lutadores[l.ordinal()];
    }

    public Lado ladoDe(IrmaoMiau l) {
        return l == lutadores[0] ? Lado.A : l == lutadores[1] ? Lado.B : null;
    }

    public EstadoPosicional getEstado() {
        return estado;
    }

    public Posicao getPosicao() {
        return estado.getPosicao();
    }

    public CatalogoTecnicasSolo getCatalogo() {
        return catalogo;
    }

    public Optional<Tentativa> getTentativa() {
        return Optional.ofNullable(tentativa);
    }

    public boolean isAterrissando() {
        return aterrissagemCima != null;
    }

    public double getEstabilidade() {
        return estabilidade;
    }

    public double getBase(Lado l) {
        return base.get(l);
    }

    public Optional<Exposicao> exposicaoDe(Lado l) {
        Exposto e = exposicao.get(l);
        return e == null ? Optional.empty() : Optional.of(e.tipo());
    }

    public boolean emPostura(Lado l) {
        return postura.get(l) > 0;
    }

    public int recargaDe(Lado l) {
        return recarga.get(l);
    }

    public Evento getUltimoEvento() {
        return ultimo;
    }

    public List<Evento> eventos() {
        return List.copyOf(eventos);
    }

    /** O lutador esta numa posicao de solo com vinculo (nao anda, nao rola, nao muda de postura sozinho). */
    public boolean conectado(IrmaoMiau l) {
        Lado lado = ladoDe(l);
        return lado != null && (estado.getPosicao().isVinculoDeSolo() || aterrissagemCima != null);
    }

    /** O motor conduz os comandos de tecnica deste lutador (chao ou um em pe contra o solo). */
    public boolean conduzComandos(IrmaoMiau l) {
        return ladoDe(l) != null && (estado.getPosicao().isNoChao() || aterrissagemCima != null);
    }

    /** Lado para onde o lutador deve olhar (o motor manda no chao), ou vazio. */
    public Optional<Boolean> orientacao(IrmaoMiau l) {
        Lado lado = ladoDe(l);
        if (lado == null || !estado.getPosicao().isVinculoDeSolo() || estado.getPosicao() == Posicao.SOLO_DISPUTADO) {
            return Optional.empty();
        }
        Lado cima = estado.getPorCima().orElse(Lado.A);
        if (estado.getPosicao() == Posicao.COSTAS) {
            return Optional.of(frente > 0);                 // os dois olham para o mesmo lado
        }
        return Optional.of(lado == cima ? frente < 0 : frente > 0);
    }

    /** Papel legivel do lado (HUD/F3). */
    public String papel(Lado l) {
        Posicao p = estado.getPosicao();
        if (!p.isNoChao()) {
            return estado.getControlador().map(c -> c == l ? "APLICADOR" : "RECEPTOR").orElse("LIVRE");
        }
        String cima = estado.getPorCima().map(c -> c == l ? "POR CIMA" : "POR BAIXO").orElse("NO CHÃO");
        String ctrl = estado.getControlador().map(c -> c == l ? " · CONTROLA" : " · CONTROLADO").orElse("");
        return cima + ctrl;
    }

    /** Linha do F3. */
    public String resumo() {
        String t = tentativa == null ? "-" : tentativa.tecnica().id() + " " + tentativa.iniciador() + " "
                + tentativa.ticksRestantes();
        return "SOLO " + estado.getPosicao() + " cima " + estado.getPorCima().map(Enum::name).orElse("-")
                + " ctrl " + estado.getControlador().map(Enum::name).orElse("-")
                + " estab " + Math.round(estabilidade) + " tent " + t
                + " exp A " + exposicaoDe(Lado.A).map(Enum::name).orElse("-")
                + " B " + exposicaoDe(Lado.B).map(Enum::name).orElse("-");
    }

    // =====================================================
    // COMANDOS
    // =====================================================
    /** Um comando de um lado (mesmo tick dos dois: use {@link #comandos}). */
    public Resposta comando(Lado lado, ComandoSolo c) {
        Map<Lado, ComandoSolo> m = new EnumMap<>(Lado.class);
        m.put(lado, c);
        return comandos(m).get(lado);
    }

    /**
     * Comandos do MESMO tick dos dois lados, resolvidos de forma deterministica:
     * 1) a resposta a uma tecnica em andamento (contra) vale primeiro;
     * 2) se os dois comecam algo no mesmo tick e um e contra do outro, a contra vence;
     * 3) senao, quem CONTROLA a posicao tem a iniciativa (em pe sem controle: quem tem
     *    mais base; empate: os dois recusados, "choque").
     */
    public Map<Lado, Resposta> comandos(Map<Lado, ComandoSolo> pedidos) {
        Map<Lado, Resposta> r = new EnumMap<>(Lado.class);
        sincronizarEmPe();
        if (pedidos.size() == 2 && tentativa == null && aterrissagemCima == null) {
            Optional<TecnicaSolo> ta = catalogo.para(estado, Lado.A, pedidos.get(Lado.A));
            Optional<TecnicaSolo> tb = catalogo.para(estado, Lado.B, pedidos.get(Lado.B));
            if (ta.isPresent() && tb.isPresent() && !ta.get().isInstantanea() && !tb.get().isInstantanea()) {
                Lado primeiro = primeiroNoChoque(ta.get(), tb.get());
                if (primeiro == null) {
                    registrar("CHOQUE", ta.get().id() + "/" + tb.get().id(), Lado.A, "os dois ao mesmo tempo");
                    r.put(Lado.A, new Resposta(Resultado.RECUSADA, "CHOQUE: OS DOIS AO MESMO TEMPO", ta.get()));
                    r.put(Lado.B, new Resposta(Resultado.RECUSADA, "CHOQUE: OS DOIS AO MESMO TEMPO", tb.get()));
                    return r;
                }
                r.put(primeiro, umComando(primeiro, pedidos.get(primeiro)));
                r.put(primeiro.outro(), umComando(primeiro.outro(), pedidos.get(primeiro.outro())));
                return r;
            }
        }
        for (Lado l : Lado.values()) {
            if (pedidos.containsKey(l)) {
                r.put(l, umComando(l, pedidos.get(l)));
            }
        }
        return r;
    }

    private Lado primeiroNoChoque(TecnicaSolo a, TecnicaSolo b) {
        if (a.contras().contains(b.id())) {
            return Lado.A;                                     // B responde a A como contra
        }
        if (b.contras().contains(a.id())) {
            return Lado.B;
        }
        Optional<Lado> ctrl = estado.getControlador();
        if (ctrl.isPresent()) {
            return ctrl.get();
        }
        double ba = base.get(Lado.A);
        double bb = base.get(Lado.B);
        return ba > bb ? Lado.A : bb > ba ? Lado.B : null;
    }

    private Resposta umComando(Lado lado, ComandoSolo c) {
        if (c == null) {
            return new Resposta(Resultado.RECUSADA, "SEM COMANDO", null);
        }
        // Aterrissagem: o que caiu escolhe como recebe (fechar a guarda, meia ou nada).
        if (aterrissagemCima != null) {
            if (lado == aterrissagemCima.outro() && !c.explosivo()) {
                reacaoAterrissagem = c.direcao();
                return new Resposta(Resultado.CONCLUIDA, "REAÇÃO NA QUEDA", null);
            }
            return new Resposta(Resultado.RECUSADA, "ATERRISSANDO", null);
        }
        IrmaoMiau eu = lutador(lado);
        if (tentativa != null) {
            if (tentativa.iniciador() == lado) {
                abandonar();
                return new Resposta(Resultado.ABANDONADA, "ABANDONOU", null);
            }
            Optional<TecnicaSolo> t2 = catalogo.para(estado, lado, c);
            if (t2.isPresent() && tentativa.tecnica().contras().contains(t2.get().id())) {
                if (eu.getFolegoAtual() < custo(lado, t2.get())) {
                    return new Resposta(Resultado.RECUSADA, "SEM FÔLEGO", t2.get());
                }
                eu.gastarFolegoEsforco(custo(lado, t2.get()), t2.get().id());
                contra(lado, t2.get());
                return new Resposta(Resultado.CONTRA, "CONTRA", t2.get());
            }
            return new Resposta(Resultado.RECUSADA, "TÉCNICA DO OUTRO EM ANDAMENTO", t2.orElse(null));
        }
        Optional<TecnicaSolo> ot = catalogo.para(estado, lado, c);
        if (ot.isEmpty()) {
            return new Resposta(Resultado.RECUSADA, "NADA NESTA POSIÇÃO", null);
        }
        TecnicaSolo t = ot.get();
        String motivo = motivoSem(lado, t);
        if (motivo != null) {
            registrar("RECUSA", t.id(), lado, motivo);
            return new Resposta(Resultado.RECUSADA, motivo, t);
        }
        eu.gastarFolegoEsforco(custo(lado, t), t.id());
        eu.registrarAtividadeDeCombate();
        if (t.isInstantanea()) {
            instantanea(lado, t);
            recarga.put(lado, RECARGA_TICKS / 2);
            return new Resposta(Resultado.CONCLUIDA, t.nome(), t);
        }
        double j = t.janela() * FATOR_JANELA;
        if (eu.getFolegoAtual() < eu.getAtributos().getFolego() * FRACAO_FOLEGO_CANSADO) {
            j *= 1.5;                                          // cansado: mais lento, mais tempo para o outro
        }
        if (eu.isRespirando()) {
            j *= FATOR_JANELA_RESPIRANDO;                      // respirando: menos pronto
        }
        if (t.id().equals(CatalogoTecnicasSolo.LEVANTAR_TECNICO)) {
            j *= desorientado.getOrDefault(lado, 0) > 0 ? FATOR_LEVANTAR_DERRUBADO : FATOR_LEVANTAR_VOLUNTARIO;
        }
        int janela = Math.max(1, (int) Math.round(j));
        tentativa = new Tentativa(t, lado, janela, janela, tick);
        if (t.exposicao() != null) {
            exposicao.put(lado, new Exposto(t.exposicao(), janela + 6));
        }
        registrar("INICIOU", t.id(), lado, "");
        return new Resposta(Resultado.INICIADA, t.nome(), t);
    }

    /** 5P.1: custo de folego desta tecnica para este lado (catalogo x balanceamento central). */
    public double custo(Lado lado, TecnicaSolo t) {
        return com.irmaosmiau.entities.CustosFolego.custoSolo(t.custo(), lutador(lado).getAtributos().getFolego());
    }

    /** Por que a tecnica nao pode comecar agora (null = pode). */
    public String motivoSem(Lado lado, TecnicaSolo t) {
        IrmaoMiau eu = lutador(lado);
        IrmaoMiau outro = lutador(lado.outro());
        if (recarga.get(lado) > 0) {
            return "EM RECUPERAÇÃO";
        }
        if (eu.getFolegoAtual() < custo(lado, t)) {
            return "SEM FÔLEGO";
        }
        // 5P.1: no chao a animacao de troca de postura (que o proprio motor dispara) nao bloqueia:
        // antes, depois de toda troca de posicao os dois ficavam ~14 ticks recusando ate as contras.
        if ((eu.getTransicaoAtiva() != null && !estado.getPosicao().isNoChao()) || eu.isRolando() || eu.isPulando()
                || eu.isEmAcrobacia() || eu.isFazendoSprawl()) {
            return "EM MOVIMENTO";
        }
        int dist = Math.abs(eu.getX() - outro.getX());
        boolean mesmaFaixa = Math.abs(eu.getLinhaAtual() - outro.getLinhaAtual()) <= 1;
        Posicao p = estado.getPosicao();
        if (!p.isNoChao()) {
            if (eu.isDeitado() || eu.isAjoelhado() || outro.isDeitado() || outro.isAjoelhado()) {
                return "SÓ EM PÉ";
            }
            if (sistema != null && sistema.estaEmInteracao() && !sistema.isInteracaoEstabelecida()) {
                return "PEGADA ENTRANDO";
            }
            if (!mesmaFaixa) {
                return "LINHA DISTANTE";
            }
            return switch (t.id()) {
                case CatalogoTecnicasSolo.EMPURRAO -> dist <= ALCANCE_EMPURRAO_PX ? null : "LONGE";
                case CatalogoTecnicasSolo.ENTRADA_PERNAS -> dist <= ALCANCE_PERNAS_PX ? null : "LONGE";
                case CatalogoTecnicasSolo.PUXAR_GUARDA ->
                        p == Posicao.EM_PE_PEGADA || dist <= ALCANCE_PUXAR_PX ? null : "LONGE";
                default -> null;
            };
        }
        if (t.id().equals(CatalogoTecnicasSolo.ENTRAR_GUARDA) || t.id().equals(CatalogoTecnicasSolo.PASSAGEM_EM_PE)) {
            return dist <= ALCANCE_ENTRAR_PX && mesmaFaixa ? null : "LONGE";
        }
        if (t.id().equals(CatalogoTecnicasSolo.CONECTAR_GUARDA)) {
            return dist <= ALCANCE_CONECTAR_PX && mesmaFaixa ? null : "LONGE";
        }
        return null;
    }

    private void abandonar() {
        Tentativa t = tentativa;
        tentativa = null;
        if (t.tecnica().exposicao() != null) {
            exposicao.put(t.iniciador(), new Exposto(t.tecnica().exposicao(), 20));
        }
        recarga.put(t.iniciador(), RECARGA_TICKS);
        registrar("ABANDONOU", t.tecnica().id(), t.iniciador(), "");
    }

    // =====================================================
    // TICK
    // =====================================================
    /** Um tick: janelas, aterrissagem, estabilidade, encaixe dos corpos, vinculo. */
    public void atualizar(Intencao ia, Intencao ib) {
        tick++;
        for (Lado l : Lado.values()) {
            recarga.computeIfPresent(l, (k, v) -> Math.max(0, v - 1));
            postura.computeIfPresent(l, (k, v) -> Math.max(0, v - 1));
            Exposto e = exposicao.get(l);
            if (e != null) {
                if (e.ticks() <= 1) {
                    exposicao.remove(l);
                } else {
                    exposicao.put(l, new Exposto(e.tipo(), e.ticks() - 1));
                }
            }
            if (!exposicao.containsKey(l)) {
                base.put(l, Math.min(BASE_MAXIMA, base.get(l) + RECUPERACAO_BASE));
            }
        }
        sincronizarEmPe();
        coerencia();
        if (aterrissagemCima != null && --aterrissagemTicks <= 0) {
            aterrissar();
        }
        if (tentativa != null) {
            Tentativa t = tentativa;
            if (t.tecnica().id().equals(CatalogoTecnicasSolo.ENTRADA_PERNAS)
                    && lutador(t.iniciador().outro()).isFazendoSprawl()) {
                contraSprawl(t);
            } else if (t.ticksRestantes() <= 1) {
                tentativa = null;
                resolver(t, t.iniciador() == Lado.A ? ia : ib);
            } else {
                tentativa = new Tentativa(t.tecnica(), t.iniciador(), t.ticksRestantes() - 1, t.janelaTotal(),
                        t.inicio());
            }
        }
        desorientado.replaceAll((k, v) -> Math.max(0, v - 1));
        Posicao p = estado.getPosicao();
        if (p.isVinculoDeSolo() && tentativa == null) {
            Lado ctrl = estado.getControlador().orElse(null);
            if (ctrl != null) {
                IrmaoMiau c = lutador(ctrl);
                if (c.isRespirando()) {
                    // 5P.1: quem controla e respira afrouxa o controle (escapes e raspagens ficam possiveis)
                    estabilidade = Math.max(0, estabilidade - PERDA_ESTABILIDADE_RESPIRANDO);
                } else {
                    double ganho = GANHO_PASSIVO_ESTABILIDADE;
                    if (c.getFolegoAtual() < c.getAtributos().getFolego() * FRACAO_FOLEGO_CANSADO) {
                        ganho /= 2;
                    }
                    estabilidade = Math.min(100, estabilidade + ganho);
                }
            }
        }
        for (Lado l : Lado.values()) {
            lutador(l).setFatorRespiracaoNoSolo(fatorRespiracao(l));
        }
        encaixar();
        for (Lado l : Lado.values()) {
            lutador(l).setVinculadoNoSolo(estado.getPosicao().isVinculoDeSolo() || aterrissagemCima != null);
        }
    }

    /**
     * 5P.1: quanto a respiracao rende no solo para este lado: 0 no meio da propria
     * tecnica; por cima / controlando RESPIRACAO_SOLO_POR_CIMA; por baixo, de 0,35
     * (controle fraco) a 0,15 (controle total do outro). Livre: 1.
     */
    public double fatorRespiracao(Lado l) {
        if (tentativa != null && tentativa.iniciador() == l) {
            return com.irmaosmiau.entities.CustosFolego.RESPIRACAO_EM_ACAO;
        }
        if (!estado.getPosicao().isVinculoDeSolo()) {
            return 1.0;
        }
        boolean dominado = estado.getControlador().map(c -> c != l).orElse(false)
                && estado.getPorCima().map(c -> c != l).orElse(false);
        if (!dominado) {
            return com.irmaosmiau.entities.CustosFolego.RESPIRACAO_SOLO_POR_CIMA;
        }
        double max = com.irmaosmiau.entities.CustosFolego.RESPIRACAO_SOLO_POR_BAIXO_MAX;
        double min = com.irmaosmiau.entities.CustosFolego.RESPIRACAO_SOLO_POR_BAIXO_MIN;
        return max - (max - min) * estabilidade / 100.0;
    }

    /** 5P.1: o lado que acabou de cair e escolhe como recebe (aterrissagem), ou vazio. */
    public Optional<Lado> ladoQueCai() {
        return aterrissagemCima == null ? Optional.empty() : Optional.of(aterrissagemCima.outro());
    }

    /** 5P.1: um lado esta sofrendo uma entrada em pe (janela do outro): RT agora e base, nao respiracao. */
    public boolean sofrendoEntrada(Lado l) {
        return tentativa != null && tentativa.iniciador() == l.outro() && !estado.getPosicao().isNoChao();
    }

    /** Em pe, o estado e so uma LEITURA do SistemaCombate (e, 5P.1, de quem deitou por vontade propria). */
    private void sincronizarEmPe() {
        if (estado.getPosicao().isNoChao() || aterrissagemCima != null) {
            return;
        }
        // 5P.1: solo LIVRE: um deitou por vontade propria e o outro nao. Mesmo sistema do pos-queda:
        // um em pe contra o solo, SEM vinculo e sem controle (nada e conquistado a distancia).
        if (tentativa == null) {
            for (Lado l : Lado.values()) {
                if (lutador(l).isDeitado() && !lutador(l.outro()).isDeitado()) {
                    estado = EstadoPosicional.noChao(Posicao.EM_PE_CONTRA_SOLO, l.outro(), null);
                    desorientado.put(l, 0);
                    registrar("SOLO_LIVRE", "-", l, "deitou");
                    return;
                }
            }
        }
        Lado ap = sistema != null && sistema.isInteracaoEstabelecida() ? ladoDe(sistema.getAplicadorInteracao()) : null;
        EstadoPosicional novo = ap == null ? EstadoPosicional.separados() : EstadoPosicional.pegada(ap);
        if (!novo.equals(estado)) {
            estado = novo;
            if (tentativa != null && !tentativa.tecnica().origens().contains(novo.getPosicao())) {
                registrar("CANCELADA", tentativa.tecnica().id(), tentativa.iniciador(), "a pegada mudou");
                tentativa = null;
            }
        }
    }

    /** Nada contraditorio: um em pe contra o solo sem ninguem no chao vira separados. */
    private void coerencia() {
        if (estado.getPosicao() == Posicao.EM_PE_CONTRA_SOLO && tentativa == null && aterrissagemCima == null) {
            boolean alguemNoChao = false;
            for (IrmaoMiau l : lutadores) {
                alguemNoChao |= l.isDeitado() || l.isAjoelhado();
            }
            boolean osDoisDeitados = lutadores[0].isDeitado() && lutadores[1].isDeitado();
            if (!alguemNoChao || osDoisDeitados) {
                estado = EstadoPosicional.separados();
                registrar("SEPARADOS", "-", Lado.A, osDoisDeitados ? "os dois deitados, sem vinculo" : "os dois de pe");
            }
        }
    }

    // =====================================================
    // RESOLUCAO
    // =====================================================
    private boolean exposto(Lado l) {
        return exposicao.containsKey(l);
    }

    private boolean exausto(Lado l) {
        IrmaoMiau x = lutador(l);
        return x.getFolegoAtual() < x.getAtributos().getFolego() * FRACAO_FOLEGO_EXAUSTO;
    }

    private void resolver(Tentativa t, Intencao intencaoIniciador) {
        TecnicaSolo tec = t.tecnica();
        Lado ini = t.iniciador();
        Lado out = ini.outro();
        inicioJanelaAtual = t.inicio();
        switch (tec.id()) {
            case CatalogoTecnicasSolo.EMPURRAO -> resolverEmpurrao(ini);
            case CatalogoTecnicasSolo.QUEDA_PEGADA -> resolverQuedaPegada(ini, intencaoIniciador);
            case CatalogoTecnicasSolo.ENTRADA_PERNAS -> resolverPernas(ini);
            case CatalogoTecnicasSolo.PUXAR_GUARDA -> resolverPuxarGuarda(ini);
            default -> {
                boolean ok = switch (tec.criterio()) {
                    case SE_NAO_CONTRADA -> true;
                    case ESTABILIDADE_BAIXA -> estabilidade < tec.limiarEstabilidade() || exposto(out) || exausto(out);
                    case SEM_POSTURA_DO_OUTRO -> !emPostura(out) || exposto(out);
                    case OUTRO_EXPOSTO -> exposto(out);
                };
                if (ok) {
                    entrar(tec.sucesso().aplicar(estado, ini), ini, tec.papel() == PapelPosicional.POR_BAIXO
                            || tec.papel() == PapelPosicional.CONTROLADO);
                    if (tec.id().equals(CatalogoTecnicasSolo.CONECTAR_GUARDA) && desorientado.getOrDefault(ini, 0) == 0) {
                        estabilidade = ESTABILIDADE_GUARDA_VOLUNTARIA;   // deitou por vontade: guarda mais firme
                    }
                    registrar("SUCESSO", tec.id(), ini, "");
                } else {
                    recarga.put(ini, RECARGA_FALHA_TICKS);
                    estabilidade = Math.min(100, estabilidade + 10);
                    registrar("DEFENDIDA", tec.id(), ini, motivoDefesa(tec, out));
                }
            }
        }
        if (!exposto(ini) || tec.exposicao() == null) {
            return;
        }
        // a exposicao da janela acaba com ela (salvo as que a propria resolucao deixou)
        if (exposicao.get(ini).ticks() <= 6) {
            exposicao.remove(ini);
        }
    }

    private String motivoDefesa(TecnicaSolo tec, Lado out) {
        return switch (tec.criterio()) {
            case ESTABILIDADE_BAIXA -> "controle firme (" + Math.round(estabilidade) + ")";
            case SEM_POSTURA_DO_OUTRO -> "o outro em postura";
            case OUTRO_EXPOSTO -> "o outro nao estava exposto";
            default -> "";
        };
    }

    /** Resposta certa dentro da janela: a contra resolve na hora (vale o SUCESSO dela). */
    private void contra(Lado quem, TecnicaSolo c) {
        Tentativa t = tentativa;
        tentativa = null;
        if (t.tecnica().id().equals(CatalogoTecnicasSolo.QUEDA_PEGADA)) {
            // contra-queda: quem tentava a queda vai ao chao; quem reverteu cai por cima
            if (sistema != null && sistema.estaEmInteracao()) {
                sistema.encerrarInteracao();
            }
            derrubar(t.iniciador(), quem, true);
            registrar("REVERSAO", t.tecnica().id(), quem, "contra-queda");
            return;
        }
        recarga.put(t.iniciador(), RECARGA_TICKS);
        if (c.isInstantanea() || c.sucesso() == null) {
            // contra de "segurar" (postura, retencao, consolidar...): so desfaz a tentativa
            instantanea(quem, c);
            registrar("DEFENDIDA", t.tecnica().id(), t.iniciador(), "contra " + c.id());
            return;
        }
        entrar(c.sucesso().aplicar(estado, quem), quem, true);
        registrar("CONTRA", c.id(), quem, "contra " + t.tecnica().id());
    }

    private void contraSprawl(Tentativa t) {
        tentativa = null;
        Lado ini = t.iniciador();
        Lado def = ini.outro();
        if (sistema != null && sistema.estaEmInteracao()) {
            sistema.encerrarInteracao();
        }
        lutador(ini).aplicarPosturaDoSolo(EstadoCorporal.AJOELHADO);
        estado = EstadoPosicional.noChao(Posicao.EM_PE_CONTRA_SOLO, def, def);
        exposicao.put(ini, new Exposto(Exposicao.BASE, 30));
        recarga.put(ini, RECARGA_FALHA_TICKS);
        registrar("SPRAWL", t.tecnica().id(), def, "entrada nas pernas parada");
    }

    // ---------------- em pe
    private void resolverEmpurrao(Lado ini) {
        Lado def = ini.outro();
        IrmaoMiau d = lutador(def);
        base.put(ini, base.get(ini) - CUSTO_BASE_EMPURRAO);
        if (base.get(ini) < BASE_MINIMA_PARA_EMPURRAR - CUSTO_BASE_EMPURRAO) {
            // empurrao excessivo: o iniciador se expoe e perde a base
            exposicao.put(ini, new Exposto(Exposicao.AVANCO, 30));
            registrar("DEFENDIDA", CatalogoTecnicasSolo.EMPURRAO, ini, "avanço excessivo");
            if (base.get(ini) <= 0) {
                derrubarSozinho(ini, def);
            }
            return;
        }
        if (fazBase(def, inicioJanelaAtual) && !exposto(def)) {
            base.put(ini, base.get(ini) - 15);
            exposicao.put(ini, new Exposto(Exposicao.AVANCO, 30));
            registrar("DEFENDIDA", CatalogoTecnicasSolo.EMPURRAO, ini, "base firme");
            return;
        }
        int lado = Integer.signum(d.getX() - lutador(ini).getX());
        int recuo = Math.max(xMin, Math.min(xMax, d.getX() + (lado == 0 ? 1 : lado) * RECUO_EMPURRAO_PX)) - d.getX();
        d.encostarNaBorda(recuo);
        base.put(def, base.get(def) - (exposto(def) ? BASE_MAXIMA : DANO_EMPURRAO));
        exposicao.put(def, new Exposto(Exposicao.BASE, 12));
        if (base.get(def) <= 0) {
            if (sistema != null && sistema.estaEmInteracao()) {
                sistema.encerrarInteracao();
            }
            derrubarSozinho(def, ini);
            registrar("SUCESSO", CatalogoTecnicasSolo.EMPURRAO, ini, "derrubou");
        } else {
            registrar("SUCESSO", CatalogoTecnicasSolo.EMPURRAO, ini, "desequilibrou (base " + Math.round(base.get(def)) + ")");
        }
    }

    private void resolverQuedaPegada(Lado ini, Intencao intencao) {
        Lado def = ini.outro();
        IrmaoMiau d = lutador(def);
        if (sistema == null || !sistema.isInteracaoEstabelecida()) {
            registrar("FALHA", CatalogoTecnicasSolo.QUEDA_PEGADA, ini, "sem pegada");
            return;
        }
        if (fazBase(def, inicioJanelaAtual) && !exposto(def)) {
            base.put(ini, Math.max(0, base.get(ini) - 30));
            exposicao.put(ini, new Exposto(Exposicao.BASE, 30));
            recarga.put(ini, RECARGA_FALHA_TICKS);
            registrar("DEFENDIDA", CatalogoTecnicasSolo.QUEDA_PEGADA, ini, "base firme (guarda)");
            return;
        }
        boolean entra = sistema.getFirmeza() >= FIRMEZA_MINIMA_QUEDA || base.get(def) < BASE_DERRUBAVEL || exposto(def);
        if (!entra) {
            exposicao.put(ini, new Exposto(Exposicao.BASE, 20));
            registrar("DEFENDIDA", CatalogoTecnicasSolo.QUEDA_PEGADA, ini, "pegada fraca");
            return;
        }
        sistema.encerrarInteracao();
        int paraOOutro = Integer.signum(d.getX() - lutador(ini).getX());
        boolean acompanha = intencao.dx() != 0 && intencao.dx() == paraOOutro;
        if (acompanha) {
            derrubar(def, ini, true);
        } else {
            derrubarSozinho(def, ini);
        }
        registrar("SUCESSO", CatalogoTecnicasSolo.QUEDA_PEGADA, ini, acompanha ? "acompanhou" : "ficou em pé");
    }

    private void resolverPernas(Lado ini) {
        Lado def = ini.outro();
        IrmaoMiau a = lutador(ini);
        IrmaoMiau d = lutador(def);
        boolean alcance = Math.abs(a.getX() - d.getX()) <= ALCANCE_PERNAS_PX
                && Math.abs(a.getLinhaAtual() - d.getLinhaAtual()) <= 1;
        if (!alcance) {
            base.put(ini, Math.max(0, base.get(ini) - 30));
            exposicao.put(ini, new Exposto(Exposicao.BASE, 30));
            recarga.put(ini, RECARGA_FALHA_TICKS);
            registrar("FALHA", CatalogoTecnicasSolo.ENTRADA_PERNAS, ini, "longe");
            return;
        }
        if (sistema != null && sistema.estaEmInteracao()) {
            sistema.encerrarInteracao();
        }
        if (fazBase(def, inicioJanelaAtual)) {
            // base sem sprawl: os dois vao ao chao, ninguem consolida o controle (resultado E)
            entrar(EstadoPosicional.noChao(Posicao.SOLO_DISPUTADO, null, null), ini, true);
            registrar("SUCESSO", CatalogoTecnicasSolo.ENTRADA_PERNAS, ini, "queda sem controle (o outro fez base)");
            return;
        }
        derrubar(def, ini, true);
        registrar("SUCESSO", CatalogoTecnicasSolo.ENTRADA_PERNAS, ini, "");
    }

    private void resolverPuxarGuarda(Lado ini) {
        Lado out = ini.outro();
        IrmaoMiau puxador = lutador(ini);
        IrmaoMiau o = lutador(out);
        if (sistema != null && sistema.estaEmInteracao()) {
            sistema.encerrarInteracao();
        }
        // o outro recusa a guarda se estiver recuando (LS para longe de quem puxou) no fim da janela
        int longe = Integer.signum(o.getX() - puxador.getX());
        if (intencoesAtuais.get(out) != null && intencoesAtuais.get(out).dx() == longe && longe != 0) {
            puxador.aplicarPosturaDoSolo(EstadoCorporal.DEITADO);
            estado = EstadoPosicional.noChao(Posicao.EM_PE_CONTRA_SOLO, out, null);
            registrar("SUCESSO", CatalogoTecnicasSolo.PUXAR_GUARDA, ini, "o outro ficou em pé");
            return;
        }
        entrar(EstadoPosicional.noChao(Posicao.GUARDA, out, ini), ini, false);
        registrar("SUCESSO", CatalogoTecnicasSolo.PUXAR_GUARDA, ini, "guarda fechada");
    }

    private final Map<Lado, Intencao> intencoesAtuais = new EnumMap<>(Lado.class);
    private long inicioJanelaAtual;

    /** Intencoes do LS usadas nas resolucoes deste tick (chame antes de atualizar). */
    public void informarIntencoes(Intencao ia, Intencao ib) {
        intencoesAtuais.put(Lado.A, ia == null ? Intencao.NENHUMA : ia);
        intencoesAtuais.put(Lado.B, ib == null ? Intencao.NENHUMA : ib);
        for (Lado l : Lado.values()) {
            if (intencoesAtuais.get(l).base()) {
                ultimoBase.put(l, tick + 1);                     // vale para o tick que vai rodar
            }
        }
    }

    /** O lado cai e quem derrubou fica em pe (resultado A). */
    private final Map<Lado, Integer> desorientado = new EnumMap<>(Lado.class);

    private void derrubarSozinho(Lado caiu, Lado emPe) {
        desorientado.put(caiu, DESORIENTADO_TICKS);
        lutador(caiu).aplicarPosturaDoSolo(EstadoCorporal.DEITADO);
        estado = EstadoPosicional.noChao(Posicao.EM_PE_CONTRA_SOLO, emPe, null);
    }

    /** O lado cai e o outro acompanha: aterrissagem (quem caiu ainda escolhe como recebe). */
    private void derrubar(Lado caiu, Lado cima, boolean acompanha) {
        desorientado.put(caiu, DESORIENTADO_TICKS);
        lutador(caiu).aplicarPosturaDoSolo(EstadoCorporal.DEITADO);
        if (!acompanha) {
            estado = EstadoPosicional.noChao(Posicao.EM_PE_CONTRA_SOLO, cima, null);
            return;
        }
        lutador(cima).aplicarPosturaDoSolo(EstadoCorporal.AJOELHADO);
        estado = EstadoPosicional.noChao(Posicao.EM_PE_CONTRA_SOLO, cima, cima);
        aterrissagemCima = cima;
        aterrissagemTicks = ATERRISSAGEM_TICKS;
        reacaoAterrissagem = null;
    }

    private void aterrissar() {
        Lado cima = aterrissagemCima;
        aterrissagemCima = null;
        TecnicaSolo.Direcao r = reacaoAterrissagem;
        reacaoAterrissagem = null;
        Posicao p;
        Lado ctrl;
        if (r == TecnicaSolo.Direcao.CIMA) {
            p = Posicao.GUARDA;                                // fechou a guarda na queda
            ctrl = cima.outro();
        } else if (r == TecnicaSolo.Direcao.ESQUERDA || r == TecnicaSolo.Direcao.DIREITA) {
            p = Posicao.MEIA_GUARDA;                           // pegou uma perna
            ctrl = cima.outro();
        } else {
            p = Posicao.CONTROLE_LATERAL;                      // nao reagiu: cem quilos
            ctrl = cima;
        }
        entrar(EstadoPosicional.noChao(p, cima, ctrl), cima, false);
        registrar("ATERRISSOU", p.name(), cima, r == null ? "sem reação" : "reação " + r);
    }

    // ---------------- solo
    private void instantanea(Lado lado, TecnicaSolo t) {
        Lado out = lado.outro();
        switch (t.efeito()) {
            case CONSOLIDAR -> estabilidade = Math.min(100, estabilidade + DELTA_CONSOLIDAR);
            case ENQUADRAR -> estabilidade = Math.max(0, estabilidade - DELTA_ENQUADRAR);
            case POSTURA -> postura.put(lado, POSTURA_TICKS);
            case QUEBRAR_POSTURA -> {
                postura.put(out, 0);
                exposicao.put(out, new Exposto(Exposicao.POSTURA_QUEBRADA, POSTURA_QUEBRADA_TICKS));
            }
            case MONTADA_ALTA -> {
                estabilidade = Math.min(100, estabilidade + 35);
                exposicao.put(lado, new Exposto(Exposicao.BASE, MONTADA_ALTA_EXPOSTA_TICKS));
            }
            default -> {
                if (t.sucesso() != null) {
                    entrar(t.sucesso().aplicar(estado, lado), lado, false);
                }
            }
        }
        registrar("AJUSTE", t.id(), lado, "");
    }

    /**
     * Entra numa posicao: os DOIS mudam juntos (postura, papel, encaixe). A
     * pegada em pe acaba; a postura vem da posicao.
     */
    private void entrar(EstadoPosicional novo, Lado iniciador, boolean reversao) {
        if (sistema != null && sistema.estaEmInteracao()) {
            sistema.encerrarInteracao();
        }
        estado = novo;
        postura.put(Lado.A, 0);
        postura.put(Lado.B, 0);
        estabilidade = reversao ? ESTABILIDADE_REVERSAO : ESTABILIDADE_PROGRESSO;
        Posicao p = novo.getPosicao();
        if (p == Posicao.EM_PE_SEPARADOS) {
            for (IrmaoMiau l : lutadores) {
                l.aplicarPosturaDoSolo(EstadoCorporal.EM_PE);
            }
            return;
        }
        if (p == Posicao.SOLO_DISPUTADO) {
            for (IrmaoMiau l : lutadores) {
                l.aplicarPosturaDoSolo(EstadoCorporal.AJOELHADO);
            }
            return;
        }
        Lado cima = novo.getPorCima().orElseThrow();
        IrmaoMiau c = lutador(cima);
        IrmaoMiau b = lutador(cima.outro());
        int s = Integer.signum(c.getX() - b.getX());
        frente = s == 0 ? (b.isViradoParaDireita() ? 1 : -1) : s;
        if (p == Posicao.EM_PE_CONTRA_SOLO) {
            c.aplicarPosturaDoSolo(EstadoCorporal.EM_PE);
            b.aplicarPosturaDoSolo(EstadoCorporal.DEITADO);
            return;
        }
        c.aplicarPosturaDoSolo(EstadoCorporal.AJOELHADO);
        b.aplicarPosturaDoSolo(p == Posicao.COSTAS ? EstadoCorporal.AJOELHADO : EstadoCorporal.DEITADO);
    }

    /** Distancia (px) de quem esta por cima ate quem esta por baixo, por posicao. */
    public static int afastamento(Posicao p) {
        return switch (p) {
            case GUARDA -> 46;
            case GUARDA_ABERTA -> 70;
            case MEIA_GUARDA -> 36;
            case CONTROLE_LATERAL -> 18;
            case JOELHO_BARRIGA -> 22;
            case MONTADA -> 8;
            case COSTAS -> -26;
            default -> 50;
        };
    }

    /** Encaixe gradual (sem teletransporte) e sempre dentro da arena e na mesma linha. */
    private void encaixar() {
        Posicao p = estado.getPosicao();
        if (!p.isVinculoDeSolo() || p == Posicao.SOLO_DISPUTADO) {
            return;
        }
        Lado cima = estado.getPorCima().orElseThrow();
        IrmaoMiau c = lutador(cima);
        IrmaoMiau b = lutador(cima.outro());
        int alvo = b.getX() + frente * afastamento(p);
        int limitado = Math.max(xMin, Math.min(xMax, alvo));
        if (limitado != alvo) {
            b.encostarNaBorda(limitado - alvo);               // empurra a dupla para dentro
            alvo = limitado;
        }
        int dx = alvo - c.getX();
        if (dx != 0) {
            c.encostarNaBorda(Integer.signum(dx) * Math.min(VELOCIDADE_ENCAIXE, Math.abs(dx)));
        }
        int dy = b.getLinhaAtual() - c.getLinhaAtual();
        if (dy != 0) {
            c.moverVertical(Integer.signum(dy) * 2.0);
        }
    }

    private final Deque<String> mensagens = new ArrayDeque<>();

    /** 5P.1: frases dos resultados CONFIRMADOS desde a ultima consulta (aviso no topo). */
    public List<String> consumirMensagens() {
        List<String> l = List.copyOf(mensagens);
        mensagens.clear();
        return l;
    }

    private void registrar(String tipo, String tecnica, Lado lado, String detalhe) {
        ultimo = new Evento(tick, tipo, tecnica, lado, detalhe);
        String frase = NarradorSolo.mensagem(ultimo);
        if (frase != null) {
            mensagens.addLast(frase);
            while (mensagens.size() > 4) {
                mensagens.removeFirst();
            }
        }
        eventos.addLast(ultimo);
        while (eventos.size() > 40) {
            eventos.removeFirst();
        }
    }

    /** Teste/depuracao: forca uma posicao (os dois mudam juntos). */
    public void forcarPosicao(Posicao p, Lado porCima, Lado controlador) {
        tentativa = null;
        aterrissagemCima = null;
        entrar(p == Posicao.EM_PE_SEPARADOS ? EstadoPosicional.separados()
                : EstadoPosicional.noChao(p, porCima, controlador), porCima == null ? Lado.A : porCima, false);
        for (Lado l : Lado.values()) {
            lutador(l).setVinculadoNoSolo(estado.getPosicao().isVinculoDeSolo());
        }
    }

    /** Teste: fixa a estabilidade do controle. */
    public void definirEstabilidade(double v) {
        estabilidade = Math.max(0, Math.min(100, v));
    }

    /** Pares (posicao do catalogo) com as tecnicas de cada papel, para a documentacao. */
    public static List<String> tabelaDeComandos() {
        List<String> l = new ArrayList<>();
        for (TecnicaSolo t : CatalogoTecnicasSolo.referencia().todas()) {
            l.add(t.origens() + " " + t.papel() + " " + t.comandos() + " -> " + t.id());
        }
        return l;
    }
}
