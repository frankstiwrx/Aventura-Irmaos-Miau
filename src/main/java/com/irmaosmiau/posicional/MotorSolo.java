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
    public static final int POSTURA_QUEBRADA_TICKS = ParametrosFinalizacao.POSTURA_QUEBRADA_TICKS;   // 5Q.5: antes 40
    public static final int MONTADA_ALTA_EXPOSTA_TICKS = 20;
    public static final double GANHO_PASSIVO_ESTABILIDADE = 0.12;          // 5P.1: antes 0,2
    public static final double ESTABILIDADE_PROGRESSO = 50;
    public static final double ESTABILIDADE_REVERSAO = 30;
    public static final double DELTA_CONSOLIDAR = 25;
    public static final double DELTA_ENQUADRAR = 25;                       // 5P.1: antes 20
    /**
     * 5Q.5 (escapes): enquadrar cria ESPACO por TICKS_ESPACO. Com espaco, o escape tolera
     * BONUS_ESPACO a mais de estabilidade e o consolidar de quem esta por cima nao o
     * cancela: so FECHA o espaco (e consolida menos). Quem usa um ajuste instantaneo como
     * contra fica RECARGA_CONTRA_AJUSTE sem outro ajuste (antes: contra infinita e gratis).
     */
    public static final int TICKS_ESPACO = 60;
    public static final double BONUS_ESPACO = 20;
    public static final double CONSOLIDAR_FECHANDO_ESPACO = 10;
    public static final int RECARGA_CONTRA_AJUSTE = 24;
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
    /** 5Q.6: destino da aterrissagem sem reacao (null = cem quilos, como antes). */
    private Posicao alvoAterrissagem;
    private int aterrissagemTicks;
    private TecnicaSolo.Direcao reacaoAterrissagem;
    private double estabilidade;
    private int frente = 1;
    private long tick;
    private final Map<Lado, Exposto> exposicao = new EnumMap<>(Lado.class);
    private final Map<Lado, Integer> postura = new EnumMap<>(Lado.class);
    private final Map<Lado, Integer> recarga = new EnumMap<>(Lado.class);
    /** 5Q.5: afastamento gradual pendente (quem e afastado -> distancia alvo). */
    private final Map<Lado, Integer> afastarAte = new EnumMap<>(Lado.class);
    /** 5Q.5: espaco criado pelo enquadrar (ticks): o escape seguinte nao e cancelado pelo consolidar. */
    private final Map<Lado, Integer> espaco = new EnumMap<>(Lado.class);
    private final Map<Lado, Double> base = new EnumMap<>(Lado.class);
    private final Deque<Evento> eventos = new ArrayDeque<>();
    private Evento ultimo;
    /** 5P.2: conexoes de quimono no chao (manga presa) por lado. */
    private final Map<Lado, java.util.EnumSet<Conexao>> conexoes = new EnumMap<>(Lado.class);
    /** 5P.2: espacos de finalizacao (reservados; nada executa). */
    private final RegistroFinalizacoes finalizacoes = RegistroFinalizacoes.referencia();
    /** 5P.2: eventos de luta para regras futuras (pontuacao/exaustao); nao dao pontos. */
    private final Deque<EventoLuta.Registro> eventosLuta = new ArrayDeque<>();
    private final java.util.EnumSet<Lado> exaustaoAvisada = java.util.EnumSet.noneOf(Lado.class);

    // =====================================================
    // ENTREGA 5Q: FINALIZACOES
    // =====================================================
    // 5Q.5: os valores vivem em ParametrosFinalizacao (um lugar so para o balanceamento)
    public static final double PRESSAO_INICIAL = ParametrosFinalizacao.PRESSAO_INICIAL;
    public static final double PRESSAO_APERTO = ParametrosFinalizacao.PRESSAO_APERTO;
    public static final double PRESSAO_DEFESA = ParametrosFinalizacao.PRESSAO_DEFESA;
    public static final double LIMIAR_ESCAPE_FINALIZACAO = ParametrosFinalizacao.LIMIAR_ESCAPE;
    public static final double ESCAPE_FALHO_PRESSAO = ParametrosFinalizacao.ESCAPE_FALHO_PRESSAO;
    public static final int RECARGA_ACAO_FINALIZACAO = ParametrosFinalizacao.RECARGA_ACAO;
    public static final double CUSTO_APERTO = ParametrosFinalizacao.CUSTO_APERTO;
    public static final double CUSTO_DEFESA = ParametrosFinalizacao.CUSTO_DEFESA;
    public static final double CUSTO_ESCAPE = ParametrosFinalizacao.CUSTO_ESCAPE;
    public static final int TICKS_NA_PONTA = ParametrosFinalizacao.TICKS_NA_PONTA;

    /** Fases da tentativa de finalizacao (preparacao = requisitos, verificados antes da entrada). */
    public enum FaseFinalizacao {
        /** Entrada: quem ataca esta exposto; a defesa corta a entrada. */
        ENTRADA,
        /** Controle/ameaca: disputa de pressao ate o desfecho. */
        DISPUTA
    }

    /** O ataque de finalizacao em andamento (no maximo um). */
    public static final class Ataque {
        private final Finalizacao finalizacao;
        private final Lado atacante;
        private FaseFinalizacao fase = FaseFinalizacao.ENTRADA;
        private int ticks;
        private final int entradaTotal;
        private double pressao;
        private int recargaAtaque;
        private int recargaDefesa;
        /** So visual: ticks desde a ultima defesa (pulso da reacao). */
        private int ticksDesdeDefesa = 99;
        /** 5Q.5: ticks em 100% (NA PONTA) antes de consolidar. */
        private int ticksNaPonta;
        /** 5Q.6: depois de uma defesa tecnica abre uma janela em que o escape fica mais facil. */
        private int ticksJanelaFuga;

        /** 5Q.6: a defesa abriu espaco: escapar agora tem limiar maior. */
        public boolean janelaDeFuga() {
            return ticksJanelaFuga > 0;
        }

        /** 5Q.6: pressao maxima em que o escape ainda funciona agora. */
        public double limiarEscape() {
            return ParametrosFinalizacao.LIMIAR_ESCAPE + (ticksJanelaFuga > 0 ? ParametrosFinalizacao.BONUS_JANELA_FUGA : 0);
        }

        Ataque(Finalizacao f, Lado atacante, int entrada) {
            this.finalizacao = f;
            this.atacante = atacante;
            this.ticks = entrada;
            this.entradaTotal = entrada;
        }

        public Finalizacao finalizacao() {
            return finalizacao;
        }

        public Lado atacante() {
            return atacante;
        }

        public Lado defensor() {
            return atacante.outro();
        }

        public FaseFinalizacao fase() {
            return fase;
        }

        public double pressao() {
            return pressao;
        }

        /** 5Q.5: a finalizacao chegou a 100% e esta para consolidar (ainda da para bater/defender). */
        public boolean naPonta() {
            return fase == FaseFinalizacao.DISPUTA && pressao >= 100;
        }

        /** 5Q.5: 0-1 do tempo NA PONTA ja gasto (1 = consolida). */
        public double progressoNaPonta() {
            return Math.min(1.0, (double) ticksNaPonta / Math.max(1, ParametrosFinalizacao.TICKS_NA_PONTA));
        }

        /** Progresso da entrada (0-1). */
        public double progressoEntrada() {
            return fase == FaseFinalizacao.DISPUTA ? 1.0 : 1.0 - (double) ticks / Math.max(1, entradaTotal);
        }
    }

    /** Fim da luta por finalizacao (rendicao ou finalizacao consolidada). */
    public record Vitoria(Lado vencedor, Finalizacao finalizacao, boolean rendicao) {
    }

    /** 5Q: efeito visual de um lado (o desenho reflete o motor, nunca decide nada). */
    public enum TipoEfeitoVisual {
        EMPURRANDO, EMPURRADO, AFASTANDO, SOLTANDO, LEVANTANDO, FINALIZANDO, DEFENDENDO_FINALIZACAO, BATENDO,
        /** 5Q.3: quem recebe o afastamento com as pernas (perde o contato, recua o tronco). */
        AFASTADO,
        /** 5Q.3: volta suave da pose de uma tecnica interrompida/terminada para a pose da posicao. */
        LIBERANDO,
        /** 5Q.3: tecnica de solo com animacao propria (ex.: berimbolo): quem faz e quem recebe. */
        TECNICA_ATAQUE, TECNICA_RECEBE
    }

    /**
     * Efeito visual (so desenho; o motor ja decidiu tudo).
     *
     * @param progresso   0-1 dentro do efeito (finalizacoes: 0-0,5 = entrada, 0,5-1 = pressao 0-100%)
     * @param tecnica     id da tecnica/finalizacao que o efeito representa (null = generico)
     * @param marco       fracao do efeito em que o motor resolve a tecnica (ex.: o empurrao chega)
     * @param deslocamento px de tela que o motor ja moveu este lutador no inicio do efeito (recuo do empurrao)
     * @param desde       LIBERANDO: de que ponto (progresso) da animacao interrompida a pose volta
     * @param pulsoDefesa 0-1: 1 logo depois de uma defesa na disputa, cai para 0 em 12 ticks
     */
    public record EfeitoVisual(TipoEfeitoVisual tipo, double progresso, CategoriaFinalizacao categoria, String tecnica,
            double marco, int deslocamento, double desde, double pulsoDefesa) {

        public EfeitoVisual(TipoEfeitoVisual tipo, double progresso, CategoriaFinalizacao categoria) {
            this(tipo, progresso, categoria, null, 1.0, 0, 0, 0);
        }
    }

    /** Estado (so visual) de um efeito em andamento. */
    private static final class Efeito {
        final TipoEfeitoVisual tipo;
        int restante;
        final int total;
        String tecnica;
        double marco = 1.0;
        int deslocamento;
        double desde;
        CategoriaFinalizacao categoria;

        Efeito(TipoEfeitoVisual tipo, int total) {
            this.tipo = tipo;
            this.restante = total;
            this.total = total;
        }

        double progresso() {
            return 1.0 - (double) restante / Math.max(1, total);
        }
    }

    private final CatalogoFinalizacoes catalogoFinalizacoes = CatalogoFinalizacoes.referencia();
    private com.irmaosmiau.regras.ModoRegras modoRegras = com.irmaosmiau.regras.ModoRegras.LIVRE;
    private Ataque ataque;
    private Vitoria vitoria;
    private final Map<Lado, Efeito> efeitos = new EnumMap<>(Lado.class);

    public CatalogoFinalizacoes getCatalogoFinalizacoes() {
        return catalogoFinalizacoes;
    }

    public com.irmaosmiau.regras.ModoRegras getModoRegras() {
        return modoRegras;
    }

    public void setModoRegras(com.irmaosmiau.regras.ModoRegras m) {
        modoRegras = m == null ? com.irmaosmiau.regras.ModoRegras.LIVRE : m;
    }

    public Optional<Ataque> getAtaque() {
        return Optional.ofNullable(ataque);
    }

    public Optional<Vitoria> getVitoria() {
        return Optional.ofNullable(vitoria);
    }

    /** Treino: depois da finalizacao a luta segue (a vitoria e so avisada). */
    public void limparVitoria() {
        vitoria = null;
    }

    /** Efeito visual do lado agora (vazio = so a pose da posicao). */
    public Optional<EfeitoVisual> efeitoVisual(Lado l) {
        if (ataque != null) {
            boolean ataca = ataque.atacante == l;
            double pulso = Math.max(0, 1.0 - ataque.ticksDesdeDefesa / 12.0);
            return Optional.of(new EfeitoVisual(ataca ? TipoEfeitoVisual.FINALIZANDO
                    : TipoEfeitoVisual.DEFENDENDO_FINALIZACAO, progressoVisualAtaque(), ataque.finalizacao.categoria(),
                    ataque.finalizacao.id(), 0.5, 0, 0, pulso));
        }
        Efeito e = efeitos.get(l);
        if (e == null || e.restante <= 0) {
            return Optional.empty();
        }
        return Optional.of(new EfeitoVisual(e.tipo, e.progresso(), e.categoria, e.tecnica, e.marco, e.deslocamento,
                e.desde, 0));
    }

    /** Finalizacao: 0-0,5 = entrada; 0,5-1 = pressao 0-100% (so desenho). */
    private double progressoVisualAtaque() {
        return ataque.fase == FaseFinalizacao.ENTRADA ? 0.5 * ataque.progressoEntrada()
                : 0.5 + 0.5 * Math.min(1.0, ataque.pressao / 100.0);
    }

    private Efeito efeito(Lado l, TipoEfeitoVisual t, int ticks) {
        Efeito e = new Efeito(t, ticks);
        efeitos.put(l, e);
        return e;
    }

    /**
     * 5Q.3: a pose especifica termina (defesa, escape, soltar, fim da janela): os dois voltam
     * suavemente a pose da posicao a partir do ponto em que estavam (nada de estalo).
     * {@code chave} = "tecnica:ATACA" ou "tecnica:RECEBE" (a pose de onde se volta).
     */
    private void liberarVisual(Lado l, String tecnica, double desde, CategoriaFinalizacao cat) {
        Efeito e = efeito(l, TipoEfeitoVisual.LIBERANDO, 14);
        e.tecnica = tecnica;
        e.desde = desde;
        e.categoria = cat;
    }

    /** Ao fim de uma tentativa com animacao propria, quem estava animando volta suave para a posicao. */
    private void encerrarVisualDaTentativa() {
        for (Lado l : Lado.values()) {
            Efeito e = efeitos.get(l);
            // 5Q.6: o efeito da janela acaba no MESMO tick da resolucao (restante 0): antes nao havia
            // volta suave e a pose saltava; agora o efeito da propria tentativa tambem e liberado
            boolean daTentativa = e != null && tentativa != null && tentativa.tecnica().id().equals(e.tecnica);
            if (e != null && (e.restante > 0 || daTentativa) && (e.tipo == TipoEfeitoVisual.TECNICA_ATAQUE
                    || e.tipo == TipoEfeitoVisual.TECNICA_RECEBE || e.tipo == TipoEfeitoVisual.AFASTANDO
                    || e.tipo == TipoEfeitoVisual.AFASTADO)) {
                boolean ataca = e.tipo == TipoEfeitoVisual.TECNICA_ATAQUE || e.tipo == TipoEfeitoVisual.AFASTANDO;
                Efeito lib = efeito(l, TipoEfeitoVisual.LIBERANDO, 14);
                lib.tecnica = (e.tecnica == null ? "-" : e.tecnica) + (ataca ? ":ATACA" : ":RECEBE");
                lib.desde = e.progresso();
            }
        }
    }

    /** 5Q.3/5Q.4: tecnicas com animacao propria de quem aplica e de quem recebe (AnimacoesTecnicas). */
    static final java.util.Set<String> ANIMACAO_PROPRIA = java.util.Set.of(CatalogoTecnicasSolo.BERIMBOLO,
            CatalogoTecnicasSolo.PRENDER_PERNAS,
            // 5Q.5: quedas e entradas com animacao propria (quem faz e quem recebe)
            CatalogoTecnicasSolo.RASTEIRA, CatalogoTecnicasSolo.ENTRADA_PERNAS, CatalogoTecnicasSolo.JOELHO_DIRETO,
            CatalogoTecnicasSolo.LEVANTAR_TECNICO,
            // 5Q.5: entradas no controle de perna (transicao corporal ate o ashi garami)
            CatalogoTecnicasSolo.ENTRADA_PERNA_GUARDA, CatalogoTecnicasSolo.ENTRADA_PERNA_DOMINANTE,
            CatalogoTecnicasSolo.ENTRADA_PERNA_ABERTA, CatalogoTecnicasSolo.ENTRADA_PERNA_ABERTA + "_dlr",
            // 5Q.6: quedas explosivas, body lock, projecoes e entradas de guarda do LB + LT
            CatalogoTecnicasSolo.SINGLE_LEG, CatalogoTecnicasSolo.HIGH_CROTCH, CatalogoTecnicasSolo.ANKLE_PICK,
            CatalogoTecnicasSolo.CARREGADA_OMBRO, CatalogoTecnicasSolo.BODY_LOCK,
            CatalogoTecnicasSolo.SINGLE_LEG_JOELHOS, CatalogoTecnicasSolo.DOUBLE_LEG_JOELHOS,
            CatalogoTecnicasSolo.ARM_DRAG, CatalogoTecnicasSolo.SNAP_DOWN,
            CatalogoTecnicasSolo.UCHI_MATA, CatalogoTecnicasSolo.OSOTO_GARI,
            CatalogoTecnicasSolo.PUXAR_ARANHA, CatalogoTecnicasSolo.PUXAR_LACO);
    /** 5Q.5: depois de prender as pernas, as maos FICAM nas canelas enquanto o controle durar. */
    public static final int TICKS_PERNAS_PRESAS = 45;
    /** 5Q.5: o recuo em pe (dois passos para tras) dura isto; o corpo desliza do lugar antigo. */
    static final int TICKS_RECUO = 28;
    /** 5Q.5: distancia (px) em que o afastar da guarda deixa quem estava por cima (fora do alcance de entrar). */
    public static final int DISTANCIA_APOS_AFASTAR = ALCANCE_ENTRAR_PX + 12;
    /** 5Q.5: velocidade do afastamento gradual (px/tick, sem teletransporte). */
    static final int VELOCIDADE_AFASTAMENTO = 3;
    /** 5Q.4: ajustes INSTANTANEOS com animacao curta propria (termina na pose da posicao). */
    static final java.util.Set<String> ANIMACAO_INSTANTANEA = java.util.Set.of(CatalogoTecnicasSolo.QUEBRAR_POSTURA,
            CatalogoTecnicasSolo.CONSOLIDAR_ASHI, CatalogoTecnicasSolo.UNDERHOOK_MEIA);
    static final int TICKS_ANIMACAO_INSTANTANEA = 18;

    /** Efeito visual de uma tecnica que comeca (empurrar, afastar, soltar, levantar). */
    private static TipoEfeitoVisual efeitoDaTecnica(String id) {
        return switch (id) {
            case CatalogoTecnicasSolo.EMPURRAO, CatalogoTecnicasSolo.DERRUBADA_FORCADA -> TipoEfeitoVisual.EMPURRANDO;
            case CatalogoTecnicasSolo.AFASTAR_DA_GUARDA, CatalogoTecnicasSolo.AFASTAR_DO_CHAO, CatalogoTecnicasSolo.RECUAR,
                    CatalogoTecnicasSolo.RETENCAO_PES -> TipoEfeitoVisual.AFASTANDO;
            case CatalogoTecnicasSolo.ABANDONAR_POSICAO, CatalogoTecnicasSolo.SOLTAR_PERNA, CatalogoTecnicasSolo.SOLTAR_GUARDA,
                    CatalogoTecnicasSolo.SAIR_DA_GUARDA, CatalogoTecnicasSolo.TIRAR_GANCHOS, CatalogoTecnicasSolo.LIVRAR_MANGAS,
                    CatalogoTecnicasSolo.SOLTAR_MANGA -> TipoEfeitoVisual.SOLTANDO;
            case CatalogoTecnicasSolo.LEVANTAR_TECNICO, CatalogoTecnicasSolo.LEVANTAR_DA_GUARDA,
                    CatalogoTecnicasSolo.LEVANTAR_DO_SOLO, CatalogoTecnicasSolo.LEVANTAR_DO_ASHI -> TipoEfeitoVisual.LEVANTANDO;
            default -> null;
        };
    }

    /** Por que a finalizacao nao pode comecar agora (null = pode). Nada e cobrado na recusa. */
    public String motivoFinalizacao(Lado lado, Finalizacao f) {
        IrmaoMiau eu = lutador(lado);
        if (!eu.getGraduacao().alcanca(f.graduacao())) {
            return "GRADUAÇÃO " + f.graduacao().name();
        }
        if (!eu.aprendeu(f.tecnicaBase())) {
            return "NÃO APRENDIDA";
        }
        if (!modoRegras.permite(f)) {
            return "PROIBIDA NO MODO";
        }
        if (recarga.get(lado) > 0) {
            return "EM RECUPERAÇÃO";
        }
        boolean ok = switch (f.requisito()) {
            case NENHUM -> true;
            case OUTRO_SEM_POSTURA -> !emPostura(lado.outro()) || exposto(lado.outro());
            case OUTRO_EXPOSTO -> exposto(lado.outro());
            case CONTROLE -> estabilidade >= f.limiarControle() || exposto(lado.outro());
            case MANGA -> temConexao(lado, Conexao.MANGA);
        };
        if (!ok) {
            return f.requisito().getMotivo();
        }
        if (eu.getFolegoAtual() < com.irmaosmiau.entities.CustosFolego.custoSolo(f.custo(), eu.getAtributos().getFolego())) {
            return "SEM FÔLEGO";
        }
        return null;
    }

    private Resposta iniciarFinalizacao(Lado lado, Finalizacao f) {
        String m = motivoFinalizacao(lado, f);
        if (m != null) {
            registrar("RECUSA", f.id(), lado, m);
            return new Resposta(Resultado.RECUSADA, f.nome().toUpperCase(java.util.Locale.ROOT) + " · " + m, null);
        }
        IrmaoMiau eu = lutador(lado);
        double custo = com.irmaosmiau.entities.CustosFolego.custoSolo(f.custo(), eu.getAtributos().getFolego());
        eu.gastarFolegoEsforco(custo, f.id());
        eu.gastarCondicionamentoEsforco(com.irmaosmiau.entities.CustosFolego.condicionamentoDaTecnica(custo, false), f.id());
        int janela = Math.max(1, (int) Math.round(f.janelaEntrada() * ParametrosFinalizacao.FATOR_ENTRADA
                * (1.0 + com.irmaosmiau.entities.CustosFolego.EXAUSTAO_JANELA * eu.fatorExaustao())));
        ataque = new Ataque(f, lado, janela);
        exposicao.put(lado, new Exposto(Exposicao.BASE, janela + 6));
        registrar("FIN_ENTRADA", f.id(), lado, "");
        return new Resposta(Resultado.INICIADA, f.nome(), null);
    }

    private double eficienciaAtaque() {
        IrmaoMiau a = lutador(ataque.atacante);
        return (0.6 + 0.4 * estabilidade / 100.0) * (1.0 - 0.4 * a.fatorExaustao());
    }

    private double eficienciaDefesa() {
        IrmaoMiau d = lutador(ataque.defensor());
        double folego = d.getFolegoAtual() / Math.max(1.0, d.getAtributos().getFolego());
        return (0.5 + 0.5 * Math.min(1.0, folego)) * (1.0 - 0.4 * d.fatorExaustao());
    }

    private double custoAcao(Lado l, double base) {
        return com.irmaosmiau.entities.CustosFolego.custoSolo(base, lutador(l).getAtributos().getFolego());
    }

    private static boolean eh(ComandoSolo c, TecnicaSolo.Familia f, TecnicaSolo.Direcao d) {
        return c != null && c.familia() == f && c.direcao() == d;
    }

    /** Comandos dos dois durante uma finalizacao (nada mais acontece no solo enquanto ela dura). */
    /** 5Q.3: os dois saem da pose da finalizacao a partir de onde estavam. */
    private void liberarFinalizacaoVisual(Ataque a) {
        double desde = progressoVisualAtaque();
        liberarVisual(a.atacante, a.finalizacao.id() + ":ATACA", desde, a.finalizacao.categoria());
        liberarVisual(a.defensor(), a.finalizacao.id() + ":RECEBE", desde, a.finalizacao.categoria());
    }

    /**
     * 5Q.5: mapa de comandos durante a finalizacao, sem conflito entre eles:
     * quem DEFENDE: R1 v = bater (sempre primeiro); R1 ^ = escapar; qualquer outra v
     * (RS, ou com LT/LB ainda segurados) = defender. Quem ATACA: o comando da propria
     * finalizacao ou RS ^ = apertar; qualquer outra v (RS, ou com LT/LB segurados, como
     * no triangulo) = soltar. Antes a v so valia pelo RS puro e o soltar/defender com
     * o gatilho ainda segurado era recusado ("EM FINALIZACAO").
     */
    public static boolean ehBater(ComandoSolo c) {
        // 5Q.6: a roda do defensor mostra BATER na ESQUERDA: qualquer <- (RS puro, com RB ou com LT/LB
        // ainda segurados) bate, alem do RB + RS v da 5Q.5. Antes so o RB + v batia e o item da roda
        // (RS <-) era recusado ("SOB FINALIZACAO"): na homologacao a luta nao terminava.
        return c != null && (c.direcao() == TecnicaSolo.Direcao.ESQUERDA
                || eh(c, TecnicaSolo.Familia.R1, TecnicaSolo.Direcao.BAIXO));
    }

    private static boolean ehBaixoNaoR1(ComandoSolo c) {
        return c != null && c.direcao() == TecnicaSolo.Direcao.BAIXO && c.familia() != TecnicaSolo.Familia.R1;
    }

    /** 5Q.6: escapar = ^ com qualquer familia (antes so RB + ^; RS ^ puro era recusado). */
    private static boolean ehEscapar(ComandoSolo c) {
        return c != null && c.direcao() == TecnicaSolo.Direcao.CIMA;
    }

    private Resposta comandoNaFinalizacao(Lado lado, ComandoSolo c) {
        Ataque a = ataque;
        Finalizacao f = a.finalizacao;
        if (lado == a.defensor()) {
            if (ehBater(c)) {
                vitoria = new Vitoria(a.atacante, f, true);   // 5Q.6: em qualquer fase e pressao   // BATER: prioridade, solta na hora
                liberarVisual(a.atacante, f.id() + ":ATACA", progressoVisualAtaque(), f.categoria());
                ataque = null;
                efeito(lado, TipoEfeitoVisual.BATENDO, 40);
                registrar("RENDICAO", f.id(), lado, f.nome());
                return new Resposta(Resultado.CONCLUIDA, "BATEU", null);
            }
            if (ehBaixoNaoR1(c)) {
                if (a.recargaDefesa > 0) {
                    return new Resposta(Resultado.RECUSADA, "EM RECUPERAÇÃO", null);
                }
                double ef = eficienciaDefesa();
                lutador(lado).gastarFolegoEsforco(custoAcao(lado, CUSTO_DEFESA), "defesa");
                if (a.fase == FaseFinalizacao.ENTRADA) {
                    falharFinalizacao("entrada cortada");
                    return new Resposta(Resultado.CONTRA, "CORTOU A ENTRADA", null);
                }
                a.pressao = Math.min(100, a.pressao) - PRESSAO_DEFESA * ef;
                a.ticksNaPonta = 0;                            // 5Q.5: saiu da ponta, volta a disputa
                a.recargaDefesa = RECARGA_ACAO_FINALIZACAO;
                a.ticksDesdeDefesa = 0;
                a.ticksJanelaFuga = ParametrosFinalizacao.TICKS_JANELA_FUGA;   // 5Q.6: defender abre a fuga
                if (a.pressao <= 0) {
                    falharFinalizacao("defendida");
                    return new Resposta(Resultado.CONTRA, "DEFENDEU", null);
                }
                return new Resposta(Resultado.CONCLUIDA, f.categoria().getDefesa(), null);
            }
            if (ehEscapar(c) && a.fase == FaseFinalizacao.ENTRADA) {
                return new Resposta(Resultado.RECUSADA, "DEFENDA A ENTRADA (↓)", null);
            }
            if (ehEscapar(c) && a.fase == FaseFinalizacao.DISPUTA) {
                if (a.recargaDefesa > 0) {
                    return new Resposta(Resultado.RECUSADA, "EM RECUPERAÇÃO", null);
                }
                lutador(lado).gastarFolegoEsforco(custoAcao(lado, CUSTO_ESCAPE), "escape");
                if (a.pressao <= a.limiarEscape()) {
                    escaparFinalizacao();
                    return new Resposta(Resultado.CONTRA, "ESCAPOU", null);
                }
                a.pressao = Math.min(100, a.pressao + ESCAPE_FALHO_PRESSAO);
                a.recargaDefesa = RECARGA_ACAO_FINALIZACAO * 2;
                a.ticksJanelaFuga = 0;                         // 5Q.6: o escape mal preparado gasta a janela
                return new Resposta(Resultado.RECUSADA, "SEM ESPAÇO PARA ESCAPAR", null);
            }
            return new Resposta(Resultado.RECUSADA, "SOB FINALIZAÇÃO", null);
        }
        if (a.fase == FaseFinalizacao.DISPUTA && (eh(c, TecnicaSolo.Familia.RS, TecnicaSolo.Direcao.CIMA) || f.aceita(c))) {
            if (a.recargaAtaque > 0) {
                return new Resposta(Resultado.RECUSADA, "EM RECUPERAÇÃO", null);
            }
            lutador(lado).gastarFolegoEsforco(custoAcao(lado, CUSTO_APERTO), "apertar");
            a.pressao = Math.min(100, a.pressao + PRESSAO_APERTO * eficienciaAtaque());
            a.recargaAtaque = RECARGA_ACAO_FINALIZACAO;
            return new Resposta(Resultado.CONCLUIDA, a.naPonta() ? "NA PONTA" : "APERTOU", null);
        }
        if (ehBaixoNaoR1(c) && !f.aceita(c)) {
            liberarFinalizacaoVisual(a);
            ataque = null;                                     // soltar: a posicao continua
            recarga.put(lado, RECARGA_TICKS);
            registrar("FIN_SOLTOU", f.id(), lado, "");
            return new Resposta(Resultado.ABANDONADA, "SOLTOU", null);
        }
        return new Resposta(Resultado.RECUSADA, a.fase == FaseFinalizacao.ENTRADA ? "ENTRANDO NA FINALIZAÇÃO"
                : "EM FINALIZAÇÃO", null);
    }

    /** 5Q.5: consolida so depois de TICKS_NA_PONTA em 100% (antes: no mesmo tick). */
    private void verificarConsolidada() {
        if (ataque == null) {
            return;
        }
        if (ataque.pressao < 100) {
            ataque.ticksNaPonta = 0;
            return;
        }
        ataque.pressao = 100;
        if (++ataque.ticksNaPonta < TICKS_NA_PONTA) {
            return;
        }
        Ataque a = ataque;
        liberarVisual(a.atacante, a.finalizacao.id() + ":ATACA", 1.0, a.finalizacao.categoria());
        vitoria = new Vitoria(a.atacante, a.finalizacao, false);
        ataque = null;
        registrar("FINALIZOU", a.finalizacao.id(), a.atacante, a.finalizacao.nome());
    }

    private void falharFinalizacao(String motivo) {
        Ataque a = ataque;
        liberarFinalizacaoVisual(a);
        ataque = null;
        Finalizacao f = a.finalizacao;
        if (f.falha() != null) {
            entrar(f.falha().aplicar(estado, a.atacante), a.defensor(), true);   // perdeu a posicao
        }
        exposicao.put(a.atacante, new Exposto(Exposicao.BASE, 30));
        recarga.put(a.atacante, RECARGA_FALHA_TICKS);
        registrar("FIN_DEFENDIDA", f.id(), a.defensor(), motivo);
    }

    private void escaparFinalizacao() {
        Ataque a = ataque;
        liberarFinalizacaoVisual(a);
        ataque = null;
        entrar(a.finalizacao.escape().aplicar(estado, a.atacante), a.defensor(), true);
        recarga.put(a.atacante, RECARGA_TICKS);
        registrar("FIN_ESCAPE", a.finalizacao.id(), a.defensor(), "");
    }

    /** Um tick da finalizacao (antes das tentativas comuns). */
    private void atualizarFinalizacao() {
        if (ataque == null) {
            return;
        }
        Ataque a = ataque;
        a.recargaAtaque = Math.max(0, a.recargaAtaque - 1);
        a.recargaDefesa = Math.max(0, a.recargaDefesa - 1);
        a.ticksDesdeDefesa = Math.min(99, a.ticksDesdeDefesa + 1);
        a.ticksJanelaFuga = Math.max(0, a.ticksJanelaFuga - 1);
        if (a.fase == FaseFinalizacao.ENTRADA) {
            if (--a.ticks <= 0) {
                a.fase = FaseFinalizacao.DISPUTA;
                a.pressao = PRESSAO_INICIAL;
                registrar("FIN_CONTROLE", a.finalizacao.id(), a.atacante, "");
            }
            return;
        }
        a.pressao += a.finalizacao.ganhoPorTick() * ParametrosFinalizacao.FATOR_GANHO_PASSIVO * eficienciaAtaque();
        lutador(a.atacante).gastarFolegoEsforco(0.004, "manter");
        verificarConsolidada();
    }

    /** 5P.2: contato de quimono que habilita guardas (o laco exige a manga). */
    public enum Conexao {
        MANGA
    }

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
        alvoAterrissagem = null;
        aterrissagemTicks = 0;
        reacaoAterrissagem = null;
        estabilidade = 0;
        exposicao.clear();
        eventos.clear();
        ultimo = null;
        mensagens.clear();
        ultimoBase.clear();
        desorientado.clear();
        eventosLuta.clear();
        exaustaoAvisada.clear();
        ataque = null;
        vitoria = null;
        efeitos.clear();
        ultimoMovimento.clear();                               // 5Q.5
        afastarAte.clear();
        for (Lado l : Lado.values()) {
            conexoes.put(l, java.util.EnumSet.noneOf(Conexao.class));
            postura.put(l, 0);
            recarga.put(l, 0);
            espaco.put(l, 0);
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

    /** 5P.2: o lado tem esta conexao de quimono agora? */
    public boolean temConexao(Lado l, Conexao c) {
        return conexoes.get(l).contains(c);
    }

    /** Teste/depuracao: da uma conexao a um lado. */
    public void adicionarConexao(Lado l, Conexao c) {
        conexoes.get(l).add(c);
    }

    public RegistroFinalizacoes getFinalizacoes() {
        return finalizacoes;
    }

    /** 5P.2: eventos de luta desde a ultima consulta (futuras regras de pontuacao/vitoria). */
    public List<EventoLuta.Registro> consumirEventosLuta() {
        List<EventoLuta.Registro> l = List.copyOf(eventosLuta);
        eventosLuta.clear();
        return l;
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

    /**
     * 5Q.5: logo depois de ter a postura quebrada, quem esta por cima ainda nao
     * consegue refazer a postura (POSTURA_BLOQUEADA_TICKS do inicio da exposicao).
     */
    public boolean posturaBloqueada(Lado l) {
        Exposto e = exposicao.get(l);
        return e != null && e.tipo() == Exposicao.POSTURA_QUEBRADA
                && e.ticks() > POSTURA_QUEBRADA_TICKS - ParametrosFinalizacao.POSTURA_BLOQUEADA_TICKS;
    }

    /** 5Q.5: espaco criado pelo enquadrar ainda vale. */
    public boolean temEspaco(Lado l) {
        return espaco.getOrDefault(l, 0) > 0;
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
    /**
     * 5Q.6: a tecnica que o comando aciona AGORA para o lado. Em pe sem vinculo, o mesmo comando
     * vale outra tecnica de joelhos (as quedas explosivas do LT): o confronto e de joelhos se
     * qualquer um dos dois estiver ajoelhado (o mesmo criterio do PERMITIDAS_DE_JOELHOS).
     */
    public Optional<TecnicaSolo> tecnicaPara(Lado lado, ComandoSolo c) {
        Boolean deJoelhos = null;
        if (!estado.getPosicao().isNoChao()) {
            deJoelhos = lutador(lado).isAjoelhado() || lutador(lado.outro()).isAjoelhado();
        }
        return catalogo.para(estado, lado, c, deJoelhos);
    }

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
        if (vitoria != null) {
            for (Lado l : pedidos.keySet()) {
                r.put(l, new Resposta(Resultado.RECUSADA, "LUTA ENCERRADA", null));
            }
            return r;
        }
        if (ataque != null) {
            Lado d = ataque.defensor();                        // 5Q: a defesa (e o bater) vem primeiro
            if (pedidos.containsKey(d)) {
                r.put(d, comandoNaFinalizacao(d, pedidos.get(d)));
            }
            for (Lado l : Lado.values()) {
                if (l != d && pedidos.containsKey(l)) {
                    r.put(l, ataque == null ? new Resposta(Resultado.RECUSADA, "FINALIZAÇÃO ENCERRADA", null)
                            : comandoNaFinalizacao(l, pedidos.get(l)));
                }
            }
            return r;
        }
        sincronizarEmPe();
        if (pedidos.size() == 2 && tentativa == null && aterrissagemCima == null) {
            Optional<TecnicaSolo> ta = tecnicaPara(Lado.A, pedidos.get(Lado.A));
            Optional<TecnicaSolo> tb = tecnicaPara(Lado.B, pedidos.get(Lado.B));
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
            Optional<TecnicaSolo> t2 = tecnicaPara(lado, c);
            if (t2.isPresent() && t2.get().id().equals(CatalogoTecnicasSolo.RETENCAO_PES)
                    && exposicaoDe(lado).orElse(null) == Exposicao.PERNAS_PRESAS) {
                return new Resposta(Resultado.RECUSADA, "PERNAS PRESAS", t2.get());   // 5P.2
            }
            if (t2.isPresent() && tentativa.tecnica().contras().contains(t2.get().id())
                    && t2.get().efeito() == TecnicaSolo.Efeito.CONSOLIDAR && temEspaco(tentativa.iniciador())) {
                if (recarga.get(lado) > 0) {
                    return new Resposta(Resultado.RECUSADA, "EM RECUPERAÇÃO", t2.get());
                }
                eu.gastarFolegoEsforco(custo(lado, t2.get()), t2.get().id());
                instantanea(lado, t2.get());                  // 5Q.5: o outro enquadrou: so fecha o espaco
                recarga.put(lado, RECARGA_TICKS / 2);
                return new Resposta(Resultado.CONCLUIDA, "FECHOU O ESPAÇO", t2.get());
            }
            if (t2.isPresent() && tentativa.tecnica().contras().contains(t2.get().id())) {
                if (recarga.get(lado) > 0 && t2.get().isInstantanea()) {
                    return new Resposta(Resultado.RECUSADA, "EM RECUPERAÇÃO", t2.get());   // 5Q.5
                }
                if (eu.getFolegoAtual() < custo(lado, t2.get())) {
                    return new Resposta(Resultado.RECUSADA, "SEM FÔLEGO", t2.get());
                }
                eu.gastarFolegoEsforco(custo(lado, t2.get()), t2.get().id());
                contra(lado, t2.get());
                return new Resposta(Resultado.CONTRA, "CONTRA", t2.get());
            }
            return new Resposta(Resultado.RECUSADA, "TÉCNICA DO OUTRO EM ANDAMENTO", t2.orElse(null));
        }
        Optional<TecnicaSolo> ot = tecnicaPara(lado, c);
        if (ot.isEmpty()) {
            Optional<Finalizacao> fz = catalogoFinalizacoes.para(estado, lado, c);
            if (fz.isPresent()) {
                return iniciarFinalizacao(lado, fz.get());   // 5Q
            }
            // 5P.2: espaco de finalizacao reservado: nao executa nem custa
            if (finalizacoes.para(estado, lado, c).isPresent()) {
                return new Resposta(Resultado.RECUSADA, "FINALIZAÇÃO FUTURA", null);
            }
            return new Resposta(Resultado.RECUSADA, "NADA NESTA POSIÇÃO", null);
        }
        TecnicaSolo t = ot.get();
        String motivo = motivoSem(lado, t);
        if (motivo != null) {
            registrar("RECUSA", t.id(), lado, motivo);
            return new Resposta(Resultado.RECUSADA, motivo, t);
        }
        eu.gastarFolegoEsforco(custo(lado, t), t.id());
        // 5P.2: o mesmo esforco tambem desgasta o condicionamento (uma cobranca so, aqui)
        eu.gastarCondicionamentoEsforco(com.irmaosmiau.entities.CustosFolego.condicionamentoDaTecnica(
                custo(lado, t), explosiva(t)), t.id());
        eu.registrarAtividadeDeCombate();
        if (t.isInstantanea()) {
            TipoEfeitoVisual evi = efeitoDaTecnica(t.id());
            if (evi != null) {
                efeito(lado, evi, 14).tecnica = t.id();
            }
            if (ANIMACAO_INSTANTANEA.contains(t.id())) {          // 5Q.4: os dois animam o ajuste
                efeito(lado, TipoEfeitoVisual.TECNICA_ATAQUE, TICKS_ANIMACAO_INSTANTANEA).tecnica = t.id();
                efeito(lado.outro(), TipoEfeitoVisual.TECNICA_RECEBE, TICKS_ANIMACAO_INSTANTANEA).tecnica = t.id();
            }
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
        j *= 1.0 + com.irmaosmiau.entities.CustosFolego.EXAUSTAO_JANELA * eu.fatorExaustao();   // 5P.2: exausto
        if (t.id().equals(CatalogoTecnicasSolo.LEVANTAR_TECNICO)) {
            j *= desorientado.getOrDefault(lado, 0) > 0 ? FATOR_LEVANTAR_DERRUBADO : FATOR_LEVANTAR_VOLUNTARIO;
        }
        int janela = Math.max(1, (int) Math.round(j));
        tentativa = new Tentativa(t, lado, janela, janela, tick);
        if (t.exposicao() != null) {
            exposicao.put(lado, new Exposto(t.exposicao(), janela + 6));
        }
        registrar("INICIOU", t.id(), lado, "");
        TipoEfeitoVisual ev = efeitoDaTecnica(t.id());
        if (ev == TipoEfeitoVisual.EMPURRANDO) {
            // 5Q.3: preparacao + empurrao (no fim da janela) + recuperacao (12 ticks depois)
            Efeito e = efeito(lado, ev, janela + 12);
            e.tecnica = t.id();
            e.marco = (double) janela / (janela + 12);
        } else if (t.id().equals(CatalogoTecnicasSolo.AFASTAR_DA_GUARDA)
                || t.id().equals(CatalogoTecnicasSolo.RETENCAO_PES)) {      // 5Q.4: retencao com os pes tambem
            efeito(lado, TipoEfeitoVisual.AFASTANDO, janela).tecnica = t.id();     // 5Q.3: pernas empurram
            efeito(lado.outro(), TipoEfeitoVisual.AFASTADO, janela).tecnica = t.id();
        } else if (ANIMACAO_PROPRIA.contains(t.id())) {
            efeito(lado, TipoEfeitoVisual.TECNICA_ATAQUE, janela).tecnica = t.id();  // 5Q.3/5Q.4: animacao propria
            efeito(lado.outro(), TipoEfeitoVisual.TECNICA_RECEBE, janela).tecnica = t.id();
        } else if (ev != null) {
            efeito(lado, ev, janela);                          // 5Q: a pose acompanha a janela
        }
        return new Resposta(Resultado.INICIADA, t.nome(), t);
    }

    /** 5P.1: custo de folego desta tecnica para este lado (catalogo x balanceamento central). */
    public double custo(Lado lado, TecnicaSolo t) {
        double c = com.irmaosmiau.entities.CustosFolego.custoSolo(t.custo(), lutador(lado).getAtributos().getFolego());
        if (explosiva(t)) {
            c *= 1.0 + com.irmaosmiau.entities.CustosFolego.EXAUSTAO_CUSTO_EXPLOSIVO * lutador(lado).fatorExaustao();
        }
        return c;
    }

    /** 5P.2: explosiva = familia R1 ou entrada em pe com janela (quedas, tiro de guarda). */
    static boolean explosiva(TecnicaSolo t) {
        return t.comandos().stream().anyMatch(ComandoSolo::explosivo)
                || (!t.isInstantanea() && t.origens().stream().noneMatch(Posicao::isNoChao));
    }

    /** 5P.2: requisito que falta (null = todos ok). */
    private String faltaRequisito(Lado lado, TecnicaSolo t, int dist, boolean mesmaFaixa) {
        for (RequisitoTecnica r : catalogo.requisitos(t.id())) {
            boolean ok = switch (r) {
                case PEGADA_APLICADOR -> sistema != null && sistema.isInteracaoEstabelecida()
                        && sistema.getAplicadorInteracao() == lutador(lado);
                case PEGADA_MANGAS -> sistema != null && "pegada_3".equals(sistema.getIdAcaoInteracao());
                case PEGADA_CRUZADA_OU_NUCA -> sistema != null && ("pegada_2".equals(sistema.getIdAcaoInteracao())
                        || "pegada_4".equals(sistema.getIdAcaoInteracao()));
                case CONEXAO_MANGA -> temConexao(lado, Conexao.MANGA);
                case PERTO_PARA_PUXAR -> (sistema != null && sistema.isInteracaoEstabelecida())
                        || dist <= ALCANCE_PUXAR_PX;
                case PERTO_NO_CHAO -> dist <= ALCANCE_ENTRAR_PX && mesmaFaixa;
            };
            if (!ok) {
                return r.getMotivo();
            }
        }
        return null;
    }

    /** Por que a tecnica nao pode comecar agora (null = pode). */
    public String motivoSem(Lado lado, TecnicaSolo t) {
        IrmaoMiau eu = lutador(lado);
        IrmaoMiau outro = lutador(lado.outro());
        if (recarga.get(lado) > 0) {
            return "EM RECUPERAÇÃO";
        }
        if (t.id().equals(CatalogoTecnicasSolo.POSTURA) && posturaBloqueada(lado)) {
            return "POSTURA QUEBRADA";                         // 5Q.5: a janela da guilhotina e real
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
        String falta = faltaRequisito(lado, t, dist, mesmaFaixa);   // 5P.2
        if (falta != null && p.isNoChao()) {
            return falta;
        }
        if (!p.isNoChao()) {
            if (eu.isDeitado() || outro.isDeitado()) {
                return "SÓ EM PÉ";
            }
            // 5Q.4: de joelhos (venha de onde vier) so as tecnicas compativeis; o resto continua so em pe
            if ((eu.isAjoelhado() || outro.isAjoelhado()) && !PERMITIDAS_DE_JOELHOS.contains(t.id())) {
                return "SÓ EM PÉ (DE JOELHOS NÃO)";
            }
            if (!eu.isAjoelhado() && !outro.isAjoelhado() && catalogo.posturaExigida(t.id())
                    .orElse(null) == CatalogoTecnicasSolo.PosturaExigida.DE_JOELHOS) {
                return "SÓ DE JOELHOS";                        // 5Q.6
            }
            if (sistema != null && sistema.estaEmInteracao() && !sistema.isInteracaoEstabelecida()) {
                return "PEGADA ENTRANDO";
            }
            if (!mesmaFaixa) {
                return "LINHA DISTANTE";
            }
            if (falta != null) {
                return falta;                                  // 5P.2: pegada certa, distancia
            }
            return switch (t.id()) {
                case CatalogoTecnicasSolo.EMPURRAO -> dist <= ALCANCE_EMPURRAO_PX ? null : "LONGE";
                case CatalogoTecnicasSolo.ENTRADA_PERNAS -> dist <= ALCANCE_PERNAS_PX ? null : "LONGE";
                case CatalogoTecnicasSolo.PUXAR_GUARDA ->
                        p == Posicao.EM_PE_PEGADA || dist <= ALCANCE_PUXAR_PX ? null : "LONGE";
                default -> QuedasExplosivas.perfil(t.id())                 // 5Q.6
                        .map(pf -> dist <= pf.alcancePx() ? null : "LONGE").orElse(null);
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
        encerrarVisualDaTentativa();                          // 5Q.3 (so desenho)
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
            espaco.computeIfPresent(l, (k, v) -> Math.max(0, v - 1));   // 5Q.5
            Exposto e = exposicao.get(l);
            if (e != null) {
                if (e.ticks() <= 1) {
                    exposicao.remove(l);
                } else {
                    exposicao.put(l, new Exposto(e.tipo(), e.ticks() - 1));
                }
            }
            if (!exposicao.containsKey(l) && !desequilibrado(l)) {   // 5Q.4: desequilibrado nao recupera a base
                base.put(l, Math.min(BASE_MAXIMA, base.get(l) + RECUPERACAO_BASE));
            }
        }
        efeitos.values().forEach(e -> e.restante = Math.max(0, e.restante - 1));
        afastarGradual();                                         // 5Q.5
        avancoDaQueda();                                          // 5Q.6
        atualizarFinalizacao();                                   // 5Q
        sincronizarEmPe();
        coerencia();
        if (aterrissagemCima != null && --aterrissagemTicks <= 0) {
            aterrissar();
        }
        if (tentativa != null) {
            Tentativa t = tentativa;
            if ((t.tecnica().id().equals(CatalogoTecnicasSolo.ENTRADA_PERNAS)
                    || QuedasExplosivas.defendePorSprawl(t.tecnica().id()))         // 5Q.6
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
                    ganho *= 1.0 - com.irmaosmiau.entities.CustosFolego.EXAUSTAO_GANHO_CONTROLE * c.fatorExaustao();
                    estabilidade = Math.min(100, estabilidade + ganho);
                }
            }
        }
        for (Lado l : Lado.values()) {
            lutador(l).setFatorRespiracaoNoSolo(fatorRespiracao(l));
        }
        desgastarCondicionamento();                               // 5P.2
        encaixar();
        for (Lado l : Lado.values()) {
            lutador(l).setVinculadoNoSolo(estado.getPosicao().isVinculoDeSolo() || aterrissagemCima != null);
        }
    }

    /** Posicoes de controle dominante (quem esta embaixo e pressionado; quem esta em cima mantem a pressao). */
    private static boolean dominante(Posicao p) {
        return p == Posicao.CONTROLE_LATERAL || p == Posicao.JOELHO_BARRIGA || p == Posicao.MONTADA
                || p == Posicao.COSTAS;
    }

    /**
     * 5P.2: desgaste fisico gradual: preso sob controle dominante (proporcional ao
     * controle) e, menos, quem mantem a pressao. Livre ou nas guardas: nada aqui (a
     * recuperacao lenta continua sendo a passiva do lutador livre).
     */
    private void desgastarCondicionamento() {
        Posicao p = estado.getPosicao();
        if (dominante(p) && aterrissagemCima == null) {
            Lado ctrl = estado.getControlador().orElse(null);
            if (ctrl != null) {
                IrmaoMiau c = lutador(ctrl);
                IrmaoMiau d = lutador(ctrl.outro());
                c.gastarCondicionamentoEsforco(com.irmaosmiau.entities.CustosFolego.drenoPressao(
                        c.getAtributos().getCondicionamento()), "pressao");
                d.gastarCondicionamentoEsforco(com.irmaosmiau.entities.CustosFolego.drenoDominado(estabilidade,
                        d.getAtributos().getCondicionamento()), "dominado");
            }
        }
        for (Lado l : Lado.values()) {
            if (lutador(l).isExaustaoCritica()) {
                if (exaustaoAvisada.add(l)) {
                    registrar("EXAUSTAO", "-", l, "condicionamento crítico");
                }
            } else if (lutador(l).getCondicionamentoAtual()
                    > lutador(l).getAtributos().getCondicionamento() * 0.10) {
                exaustaoAvisada.remove(l);
            }
        }
    }

    /**
     * 5P.1: quanto a respiracao rende no solo para este lado: 0 no meio da propria
     * tecnica; por cima / controlando RESPIRACAO_SOLO_POR_CIMA; por baixo, de 0,35
     * (controle fraco) a 0,15 (controle total do outro). Livre: 1.
     */
    public double fatorRespiracao(Lado l) {
        if ((tentativa != null && tentativa.iniciador() == l) || (ataque != null && ataque.atacante == l)) {
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

    /**
     * 5Q.4: tecnicas em pe que tambem valem com alguem de joelhos (e ninguem deitado). Tiro de guarda,
     * entrada nas pernas, rasteira, quadril, queda pela manga e De La Riva continuam so em pe.
     */
    static final java.util.Set<String> PERMITIDAS_DE_JOELHOS = java.util.Set.of(CatalogoTecnicasSolo.EMPURRAO,
            CatalogoTecnicasSolo.PUXAR_GUARDA, CatalogoTecnicasSolo.PUXAR_BORBOLETA,
            CatalogoTecnicasSolo.DERRUBADA_FORCADA, CatalogoTecnicasSolo.QUEDA_PEGADA,
            // 5Q.6: derrubadas explosivas de joelhos (LT sem pegada)
            CatalogoTecnicasSolo.SINGLE_LEG_JOELHOS, CatalogoTecnicasSolo.DOUBLE_LEG_JOELHOS,
            CatalogoTecnicasSolo.ARM_DRAG, CatalogoTecnicasSolo.SNAP_DOWN);

    /**
     * Nada contraditorio: um em pe contra o solo sem ninguem DEITADO vira separados (5Q.4: antes
     * "de joelhos" segurava o solo livre, e a mesma postura dava contextos diferentes pelo caminho).
     * Com controlador (o sprawl da entrada nas pernas) o estado fica, como na 5P.
     */
    private void coerencia() {
        if (estado.getPosicao() == Posicao.EM_PE_CONTRA_SOLO && tentativa == null && aterrissagemCima == null) {
            boolean alguemNoChao = false;
            for (IrmaoMiau l : lutadores) {
                alguemNoChao |= l.isDeitado() || (l.isAjoelhado() && estado.getControlador().isPresent());
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
        encerrarVisualDaTentativa();                          // 5Q.3 (so desenho)
        TecnicaSolo tec = t.tecnica();
        Lado ini = t.iniciador();
        Lado out = ini.outro();
        inicioJanelaAtual = t.inicio();
        if (QuedasExplosivas.perfil(tec.id()).isPresent()) {
            resolverQuedaExplosiva(ini, QuedasExplosivas.perfil(tec.id()).get(), intencaoIniciador);   // 5Q.6
            return;
        }
        if (tec.id().equals(CatalogoTecnicasSolo.PUXAR_ARANHA) || tec.id().equals(CatalogoTecnicasSolo.PUXAR_LACO)) {
            resolverPuxarParaGuarda(ini, tec);                 // 5Q.6
            return;
        }
        switch (tec.id()) {
            case CatalogoTecnicasSolo.EMPURRAO -> resolverEmpurrao(ini);
            case CatalogoTecnicasSolo.QUEDA_PEGADA -> resolverQuedaPegada(ini, intencaoIniciador);
            case CatalogoTecnicasSolo.ENTRADA_PERNAS -> resolverPernas(ini);
            case CatalogoTecnicasSolo.PUXAR_GUARDA -> resolverPuxarGuarda(ini);
            case CatalogoTecnicasSolo.TIRO_GUARDA -> resolverTiroGuarda(ini);
            case CatalogoTecnicasSolo.DERRUBADA_FORCADA -> resolverDerrubadaForcada(ini, intencaoIniciador);
            case CatalogoTecnicasSolo.RASTEIRA -> resolverRasteira(ini, intencaoIniciador);
            case CatalogoTecnicasSolo.PROJECAO_QUADRIL -> resolverProjecaoQuadril(ini);
            case CatalogoTecnicasSolo.QUEDA_MANGA -> resolverQuedaManga(ini, intencaoIniciador);
            default -> {
                boolean ok = switch (tec.criterio()) {
                    case SE_NAO_CONTRADA -> true;
                    case ESTABILIDADE_BAIXA -> estabilidade < tec.limiarEstabilidade() + (temEspaco(ini) ? BONUS_ESPACO : 0)
                            || exposto(out) || exausto(out);
                    case SEM_POSTURA_DO_OUTRO -> !emPostura(out) || exposto(out);
                    case OUTRO_EXPOSTO -> exposto(out);
                    case CONTROLE_PROPRIO -> estabilidade >= tec.limiarEstabilidade() && (!emPostura(out) || exposto(out));
                };
                if (ok && tec.sucesso() == null) {
                    aplicarEfeito(ini, tec);                    // 5P.2: efeito com janela (desequilibrar, prender...)
                    registrar("SUCESSO", tec.id(), ini, "efeito");
                } else if (ok) {
                    if (tec.id().equals(CatalogoTecnicasSolo.SOLTAR_MANGA)
                            || tec.id().equals(CatalogoTecnicasSolo.LIVRAR_MANGAS)) {
                        conexoes.get(out).remove(Conexao.MANGA);
                    }
                    entrar(tec.sucesso().aplicar(estado, ini), ini, tec.papel() == PapelPosicional.POR_BAIXO
                            || tec.papel() == PapelPosicional.CONTROLADO);
                    if (tec.id().equals(CatalogoTecnicasSolo.LEVANTAR_TECNICO)) {
                        lutador(ini).preferirTransicao(com.irmaosmiau.entities.TransicaoPostura.LEVANTADA_TECNICA);   // 5Q.5
                    }
                    if (tec.id().equals(CatalogoTecnicasSolo.AFASTAR_DA_GUARDA)
                            && estado.getPosicao() == Posicao.EM_PE_CONTRA_SOLO) {
                        afastarAte.put(out, DISTANCIA_APOS_AFASTAR);   // 5Q.5: a distancia logica aumenta de verdade
                    }
                    if (tec.id().equals(CatalogoTecnicasSolo.CONECTAR_GUARDA) && desorientado.getOrDefault(ini, 0) == 0) {
                        estabilidade = ESTABILIDADE_GUARDA_VOLUNTARIA;   // deitou por vontade: guarda mais firme
                    }
                    registrar("SUCESSO", tec.id(), ini, "");
                } else {
                    recarga.put(ini, RECARGA_FALHA_TICKS);
                    estabilidade = Math.min(100, estabilidade + 10);
                    Optional<Desfecho> perde = catalogo.falha(tec.id());
                    if (perde.isPresent()) {
                        entrar(perde.get().aplicar(estado, ini), out, true);   // 5Q: a entrada falhou e custou a posicao
                    }
                    if (tec.criterio() == TecnicaSolo.Criterio.SEM_POSTURA_DO_OUTRO) {
                        // 5Q.4: defendida pela POSTURA (diferente de ter a postura quebrada):
                        // o de cima se apruma, o de baixo perde a puxada
                        efeito(ini, TipoEfeitoVisual.TECNICA_ATAQUE, TICKS_ANIMACAO_INSTANTANEA).tecnica = "postura_firme";
                        efeito(out, TipoEfeitoVisual.TECNICA_RECEBE, TICKS_ANIMACAO_INSTANTANEA).tecnica = "postura_firme";
                    }
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
        encerrarVisualDaTentativa();                          // 5Q.3 (so desenho)
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
            if (c.isInstantanea()) {
                recarga.put(quem, RECARGA_CONTRA_AJUSTE);      // 5Q.5: segurar tambem cansa (nao e gratis)
            }
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
        reacaoDefendida(ini, t.tecnica().id());                // 5Q.6: recolhe a entrada (sem estalo)
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
        Efeito emp = efeito(def, TipoEfeitoVisual.EMPURRADO, 22);   // 5Q/5Q.3: recuo/desequilibrio visivel
        emp.tecnica = CatalogoTecnicasSolo.EMPURRAO;
        emp.deslocamento = recuo;
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

    /**
     * 5Q.6: quedas explosivas (LT sem pegada, em pe e de joelhos; RB -> sem pegada) e as projecoes
     * do LB + LT com a pegada. Recuar para longe ou ficar fora do alcance faz falhar; a defesa do
     * perfil (sprawl ja tratado na janela / base com RT) segura; senao derruba e a aterrissagem
     * leva ao destino do perfil (o defensor ainda pode reagir com guarda/meia-guarda).
     */
    private void resolverQuedaExplosiva(Lado ini, QuedasExplosivas.Perfil pf, Intencao intencao) {
        Lado def = ini.outro();
        IrmaoMiau a = lutador(ini);
        IrmaoMiau d = lutador(def);
        if (pf.pegada()) {
            if (semPegada(ini, pf.id())) {
                return;
            }
            if (fazBase(def, inicioJanelaAtual) && !exposto(def) && !desequilibrado(def)) {
                falhouEmPe(ini, pf.id(), "base firme", Exposicao.BASE, 30);
                return;
            }
            if (sistema.getFirmeza() < FIRMEZA_MINIMA_QUEDA && base.get(def) >= BASE_DERRUBAVEL && !exposto(def)) {
                falhouEmPe(ini, pf.id(), "pegada fraca", Exposicao.BASE, 20);
                return;
            }
            sistema.encerrarInteracao();
            alvoAterrissagem = pf.alvo();
            derrubar(def, ini, true);
            registrar("SUCESSO", pf.id(), ini, "projetou");
            return;
        }
        int longe = Integer.signum(d.getX() - a.getX());
        boolean recuou = intencoesAtuais.get(def) != null && intencoesAtuais.get(def).dx() == longe && longe != 0;
        boolean alcance = Math.abs(a.getX() - d.getX()) <= pf.contatoPx() + 24
                && Math.abs(a.getLinhaAtual() - d.getLinhaAtual()) <= 1;
        if (!alcance || recuou) {
            base.put(ini, Math.max(0, base.get(ini) - 30));
            falhouEmPe(ini, pf.id(), recuou ? "o outro recuou" : "longe", Exposicao.BASE, 30);
            return;
        }
        if (sistema != null && sistema.estaEmInteracao()) {
            sistema.encerrarInteracao();
        }
        boolean vulneravel = exposto(def) || desequilibrado(def);
        if (fazBase(def, inicioJanelaAtual) && !vulneravel) {
            if (pf.defesa() == QuedasExplosivas.Defesa.SPRAWL) {
                // base sem sprawl: os dois vao ao chao, ninguem consolida (o mesmo da entrada nas pernas)
                entrar(EstadoPosicional.noChao(Posicao.SOLO_DISPUTADO, null, null), ini, true);
                registrar("SUCESSO", pf.id(), ini, "queda sem controle (o outro fez base)");
                return;
            }
            falhouEmPe(ini, pf.id(), "base firme (RT)", Exposicao.BASE, 30);
            return;
        }
        if (!pf.acompanha()) {
            derrubar(def, ini, false);                         // o outro senta; quem pegou fica em pe
            registrar("SUCESSO", pf.id(), ini, "ficou em pé");
            return;
        }
        alvoAterrissagem = pf.alvo();
        derrubar(def, ini, true);
        registrar("SUCESSO", pf.id(), ini, "");
    }

    /** 5Q.6: puxar para a aranha / laco com a pegada: recusa se o outro recuar (como o puxar guarda). */
    private void resolverPuxarParaGuarda(Lado ini, TecnicaSolo tec) {
        Lado out = ini.outro();
        if (semPegada(ini, tec.id())) {
            return;
        }
        int longe = Integer.signum(lutador(out).getX() - lutador(ini).getX());
        boolean recuou = intencoesAtuais.get(out) != null && intencoesAtuais.get(out).dx() == longe && longe != 0;
        if (recuou || (fazBase(out, inicioJanelaAtual) && !exposto(out))) {
            falhouEmPe(ini, tec.id(), recuou ? "o outro recuou" : "base firme", Exposicao.BASE, 20);
            return;
        }
        conexoes.get(ini).add(Conexao.MANGA);                  // segura as mangas na descida (laco/aranha)
        entrar(EstadoPosicional.noChao(tec.sucesso().destino(), out, ini), ini, false);
        registrar("SUCESSO", tec.id(), ini, tec.sucesso().destino().name());
    }

    // ---------------- 5P.2: pegada ativa e quedas especificas
    /** Quem caiu com a pegada: acompanha (LS para o outro) = aterrissagem por cima; senao fica em pe. */
    private boolean acompanha(Lado ini, Intencao intencao) {
        int paraOOutro = Integer.signum(lutador(ini.outro()).getX() - lutador(ini).getX());
        return intencao != null && intencao.dx() != 0 && intencao.dx() == paraOOutro;
    }

    private boolean semPegada(Lado ini, String id) {
        if (sistema == null || !sistema.isInteracaoEstabelecida()) {
            registrar("FALHA", id, ini, "sem pegada");
            return true;
        }
        return false;
    }

    private void falhouEmPe(Lado ini, String id, String motivo, Exposicao exp, int ticks) {
        reacaoDefendida(ini, id);
        exposicao.put(ini, new Exposto(exp, ticks));
        recarga.put(ini, RECARGA_FALHA_TICKS);
        registrar("DEFENDIDA", id, ini, motivo);
    }

    /**
     * Tiro de guarda: salta com a pegada para fechar a guarda. Diferente de puxar:
     * mais rapido e explosivo, mas se o outro faz base (RT na janela), recua ou a
     * pegada esta fraca, quem saltou cai no chao SEM guarda (o outro fica em pe).
     */
    private void resolverTiroGuarda(Lado ini) {
        Lado out = ini.outro();
        if (semPegada(ini, CatalogoTecnicasSolo.TIRO_GUARDA)) {
            return;
        }
        int longe = Integer.signum(lutador(out).getX() - lutador(ini).getX());
        boolean recuou = intencoesAtuais.get(out) != null && intencoesAtuais.get(out).dx() == longe && longe != 0;
        String motivo = recuou ? "o outro recuou" : fazBase(out, inicioJanelaAtual) && !exposto(out) ? "base firme"
                : sistema.getFirmeza() < 40 ? "pegada fraca" : null;
        if (motivo != null) {
            sistema.encerrarInteracao();
            lutador(ini).aplicarPosturaDoSolo(EstadoCorporal.DEITADO);
            estado = EstadoPosicional.noChao(Posicao.EM_PE_CONTRA_SOLO, out, null);
            falhouEmPe(ini, CatalogoTecnicasSolo.TIRO_GUARDA, motivo + " (caiu sem guarda)", Exposicao.BASE, 30);
            return;
        }
        entrar(EstadoPosicional.noChao(Posicao.GUARDA, out, ini), ini, false);
        estabilidade = 55;
        registrar("SUCESSO", CatalogoTecnicasSolo.TIRO_GUARDA, ini, "guarda fechada");
    }

    /**
     * Derrubada forcada: pressao + deslocamento usando o vinculo da pegada. Tira
     * muita base; so derruba quem ja esta sem base (ou exposto). Base firme (RT)
     * deixa quem forcou com o avanco exposto (o outro pode empurrar/derrubar).
     */
    private void resolverDerrubadaForcada(Lado ini, Intencao intencao) {
        Lado def = ini.outro();
        if (semPegada(ini, CatalogoTecnicasSolo.DERRUBADA_FORCADA)) {
            return;
        }
        if (fazBase(def, inicioJanelaAtual) && !exposto(def)) {
            base.put(ini, Math.max(0, base.get(ini) - 25));
            falhouEmPe(ini, CatalogoTecnicasSolo.DERRUBADA_FORCADA, "defendeu com base (RT)", Exposicao.AVANCO, 30);
            return;
        }
        // 5Q.4: quem ja esta DESEQUILIBRADO (comando de desequilibrar da pegada, ou a derrubada anterior)
        // nao tem base para segurar: sem defesa ativa (RT), vai ao chao
        double dano = exposto(def) || desequilibrado(def) ? BASE_MAXIMA : 40 + sistema.getFirmeza() * 0.3;
        base.put(def, base.get(def) - dano);
        if (base.get(def) > 0) {
            lutador(def).getEfeitos().aplicar(com.irmaosmiau.entities.EfeitosTemporarios.Tipo.DESEQUILIBRIO,
                    com.irmaosmiau.combate.SistemaCombate.DURACAO_DESEQUILIBRIO_TICKS);   // 5Q.4: o mesmo desequilibrio
            efeito(def, TipoEfeitoVisual.EMPURRADO, 22).tecnica = CatalogoTecnicasSolo.DERRUBADA_FORCADA;
            exposicao.put(def, new Exposto(Exposicao.BASE, 12));
            exposicao.put(ini, new Exposto(Exposicao.AVANCO, 20));
            registrar("SUCESSO", CatalogoTecnicasSolo.DERRUBADA_FORCADA, ini,
                    "desequilibrou (base " + Math.round(base.get(def)) + ")");
            return;
        }
        derrubarPelaPegada(def, ini, intencao, CatalogoTecnicasSolo.DERRUBADA_FORCADA);
    }

    /**
     * 5Q.6: quem recebeu uma queda com animacao propria que FALHOU faz so um pequeno desequilibrio e
     * volta a postura (antes ficava com a pose de "perdendo a base" ate a pegada redesenhar); quem
     * tentou recolhe a perna/bracos suavemente.
     */
    private void reacaoDefendida(Lado ini, String id) {
        if (!ANIMACAO_PROPRIA.contains(id) || !AnimacoesDefendidas.TEM.contains(id)) {
            return;
        }
        Efeito r = efeito(ini.outro(), TipoEfeitoVisual.TECNICA_RECEBE, TICKS_REACAO_DEFENDIDA);
        r.tecnica = id + "_defendida";
        Efeito a = efeito(ini, TipoEfeitoVisual.LIBERANDO, TICKS_REACAO_DEFENDIDA);
        a.tecnica = id + ":ATACA";
        a.desde = 1.0;
    }

    /** 5Q.6: duracao da reacao de quem segurou a queda (pequeno desequilibrio e volta). */
    public static final int TICKS_REACAO_DEFENDIDA = 20;

    /** 5Q.6: quedas cuja falha tem a reacao curta propria ("id_defendida:RECEBE"). */
    static final class AnimacoesDefendidas {
        static final java.util.Set<String> TEM = java.util.Set.of(CatalogoTecnicasSolo.RASTEIRA);

        private AnimacoesDefendidas() {
        }
    }

    /**
     * 5Q.6: pontuacao da rasteira (deterministica). Antes: so entrava se o outro tivesse andado ha
     * 24 ticks, base < 85 ou estivesse exposto, e na luta real quase nunca. Agora somam: base
     * perdida, passo recente DE QUALQUER UM dos dois (puxar o outro na pegada tambem tira o pe
     * dele do lugar), desequilibrio/exposicao e a firmeza da pegada. Parado, com base cheia e
     * pegada media, continua nao entrando; o RT na janela segura quem esta parado.
     */
    public double pontuacaoRasteira(Lado def) {
        Lado ini = def.outro();
        Intencao id = intencoesAtuais.get(def);
        boolean passoDele = (id != null && (id.dx() != 0 || id.dy() != 0))
                || tick - ultimoMovimento.getOrDefault(def, Long.MIN_VALUE / 2) <= RASTEIRA_TICKS_DESDE_PASSO;
        boolean puxado = tick - ultimoMovimento.getOrDefault(ini, Long.MIN_VALUE / 2) <= RASTEIRA_TICKS_DESDE_PASSO;
        double p = 100 - base.get(def);
        p += passoDele ? RASTEIRA_PONTOS_PASSO : puxado ? RASTEIRA_PONTOS_PUXADO : 0;
        p += exposto(def) || desequilibrado(def) ? RASTEIRA_PONTOS_DESEQUILIBRIO : 0;
        p += sistema == null ? 0 : RASTEIRA_FATOR_FIRMEZA * sistema.getFirmeza();
        return p;
    }

    public static final double RASTEIRA_PONTOS_PASSO = 45;
    public static final double RASTEIRA_PONTOS_PUXADO = 40;
    public static final double RASTEIRA_PONTOS_DESEQUILIBRIO = 60;
    public static final double RASTEIRA_FATOR_FIRMEZA = 0.3;
    /** 5Q.6: a rasteira entra a partir desta pontuacao. */
    public static final double RASTEIRA_LIMIAR = 40;

    /** Rasteira: de tempo (quem esta andando/sem base cai); contra pe firme nao entra. */
    private void resolverRasteira(Lado ini, Intencao intencao) {
        Lado def = ini.outro();
        if (semPegada(ini, CatalogoTecnicasSolo.RASTEIRA)) {
            return;
        }
        Intencao id = intencoesAtuais.get(def);
        boolean andando = (id != null && (id.dx() != 0 || id.dy() != 0))
                || tick - ultimoMovimento.getOrDefault(def, Long.MIN_VALUE / 2) <= RASTEIRA_TICKS_DESDE_PASSO;
        boolean vulneravel = exposto(def) || desequilibrado(def);              // 5Q.4: inclui o desequilibrio
        boolean entra = pontuacaoRasteira(def) >= RASTEIRA_LIMIAR;              // 5Q.6
        if (fazBase(def, inicioJanelaAtual) && !andando && !vulneravel) {
            falhouEmPe(ini, CatalogoTecnicasSolo.RASTEIRA, "defendeu com base (RT)", Exposicao.BASE, 20);
            return;
        }
        if (!entra) {
            // 5Q.4: o outro nao defendeu: a base NATURAL dele (parado, base cheia) segurou
            falhouEmPe(ini, CatalogoTecnicasSolo.RASTEIRA, "base natural firme (puxe, desequilibre ou espere ele andar)",
                    Exposicao.BASE, 20);
            return;
        }
        derrubarPelaPegada(def, ini, intencao, CatalogoTecnicasSolo.RASTEIRA);
    }

    /** 5Q.4: efeito DESEQUILIBRIO ativo (o mesmo do SistemaCombate). */
    private boolean desequilibrado(Lado l) {
        return lutador(l).getEfeitos().isAtivo(com.irmaosmiau.entities.EfeitosTemporarios.Tipo.DESEQUILIBRIO);
    }

    /** Projecao de quadril: preparacao longa (gira de costas); base firme = REVERSAO (quem projetava cai). */

    private void resolverProjecaoQuadril(Lado ini) {
        Lado def = ini.outro();
        if (semPegada(ini, CatalogoTecnicasSolo.PROJECAO_QUADRIL)) {
            return;
        }
        sistema.encerrarInteracao();
        if (fazBase(def, inicioJanelaAtual) && !exposto(def)) {
            derrubar(ini, def, true);
            registrar("REVERSAO", CatalogoTecnicasSolo.PROJECAO_QUADRIL, def, "contra-quadril");
            return;
        }
        derrubar(def, ini, true);
        registrar("SUCESSO", CatalogoTecnicasSolo.PROJECAO_QUADRIL, ini, "projetou");
    }

    /** Queda pela manga: precisa da pegada das mangas firme; base firme segura. */
    private void resolverQuedaManga(Lado ini, Intencao intencao) {
        Lado def = ini.outro();
        if (semPegada(ini, CatalogoTecnicasSolo.QUEDA_MANGA)) {
            return;
        }
        if (fazBase(def, inicioJanelaAtual) && !exposto(def)) {
            falhouEmPe(ini, CatalogoTecnicasSolo.QUEDA_MANGA, "base firme", Exposicao.BASE, 30);
            return;
        }
        if (sistema.getFirmeza() < FIRMEZA_MINIMA_QUEDA && base.get(def) >= BASE_DERRUBAVEL && !exposto(def)) {
            falhouEmPe(ini, CatalogoTecnicasSolo.QUEDA_MANGA, "pegada fraca", Exposicao.BASE, 20);
            return;
        }
        derrubarPelaPegada(def, ini, intencao, CatalogoTecnicasSolo.QUEDA_MANGA);
    }

    private void derrubarPelaPegada(Lado def, Lado ini, Intencao intencao, String id) {
        if (sistema != null && sistema.estaEmInteracao()) {
            sistema.encerrarInteracao();
        }
        boolean ac = acompanha(ini, intencao);
        if (ac) {
            derrubar(def, ini, true);
        } else {
            derrubarSozinho(def, ini);
        }
        registrar("SUCESSO", id, ini, ac ? "acompanhou" : "ficou em pé");
    }

    private final Map<Lado, Intencao> intencoesAtuais = new EnumMap<>(Lado.class);
    /** 5Q.5: tick do ultimo passo de cada lado (a rasteira e de tempo: pega quem acabou de andar). */
    private final Map<Lado, Long> ultimoMovimento = new EnumMap<>(Lado.class);
    /** 5Q.5: a rasteira pega quem andou nestes ultimos ticks (antes: so no tick exato da resolucao). */
    public static final int RASTEIRA_TICKS_DESDE_PASSO = 40;   // 5Q.6: era 24
    /** 5Q.5: abaixo desta base a rasteira entra mesmo parado (antes 70: a base volta rapido a 100). */
    public static final double RASTEIRA_BASE_LIMITE = 85;
    private long inicioJanelaAtual;

    /** Intencoes do LS usadas nas resolucoes deste tick (chame antes de atualizar). */
    public void informarIntencoes(Intencao ia, Intencao ib) {
        intencoesAtuais.put(Lado.A, ia == null ? Intencao.NENHUMA : ia);
        intencoesAtuais.put(Lado.B, ib == null ? Intencao.NENHUMA : ib);
        for (Lado l : Lado.values()) {                            // 5Q.5: rasteira pega quem ANDOU ha pouco
            Intencao i = intencoesAtuais.get(l);
            if (i.dx() != 0 || i.dy() != 0) {
                ultimoMovimento.put(l, tick);
            }
        }
        for (Lado l : Lado.values()) {
            if (intencoesAtuais.get(l).base()) {
                ultimoBase.put(l, tick + 1);                     // vale para o tick que vai rodar
            }
        }
    }

    /** O lado cai e quem derrubou fica em pe (resultado A). */
    private final Map<Lado, Integer> desorientado = new EnumMap<>(Lado.class);

    private void impacto(Lado caiu) {
        IrmaoMiau l = lutador(caiu);
        l.gastarCondicionamentoEsforco(com.irmaosmiau.entities.CustosFolego.impactoQueda(
                l.getAtributos().getCondicionamento()), "queda");
    }

    private void derrubarSozinho(Lado caiu, Lado emPe) {
        impacto(caiu);                                         // 5P.2
        desorientado.put(caiu, DESORIENTADO_TICKS);
        lutador(caiu).aplicarPosturaDoSolo(EstadoCorporal.DEITADO);
        estado = EstadoPosicional.noChao(Posicao.EM_PE_CONTRA_SOLO, emPe, null);
    }

    /** O lado cai e o outro acompanha: aterrissagem (quem caiu ainda escolhe como recebe). */
    private void derrubar(Lado caiu, Lado cima, boolean acompanha) {
        impacto(caiu);                                         // 5P.2
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
        } else if (alvoAterrissagem != null) {
            p = alvoAterrissagem;                              // 5Q.6: destino da queda explosiva
            ctrl = p == Posicao.MEIA_GUARDA ? cima.outro() : cima;
        } else {
            p = Posicao.CONTROLE_LATERAL;                      // nao reagiu: cem quilos
            ctrl = cima;
        }
        alvoAterrissagem = null;
        entrar(EstadoPosicional.noChao(p, cima, ctrl), cima, false);
        registrar("ATERRISSOU", p.name(), cima, r == null ? "sem reação" : "reação " + r);
    }

    // ---------------- solo
    private void instantanea(Lado lado, TecnicaSolo t) {
        aplicarEfeito(lado, t);
        registrar("AJUSTE", t.id(), lado, "");
    }

    private void aplicarEfeito(Lado lado, TecnicaSolo t) {
        Lado out = lado.outro();
        switch (t.efeito()) {
            case DESEQUILIBRAR -> exposicao.put(out, new Exposto(Exposicao.BASE, 30));
            case CONTROLAR_PERNAS -> {
                exposicao.put(out, new Exposto(Exposicao.PERNAS_PRESAS, TICKS_PERNAS_PRESAS));
                // 5Q.5: o controle continua visivel: maos nas canelas / pernas juntas ate soltar
                efeito(lado, TipoEfeitoVisual.TECNICA_ATAQUE, TICKS_PERNAS_PRESAS).tecnica = "pernas_presas";
                efeito(out, TipoEfeitoVisual.TECNICA_RECEBE, TICKS_PERNAS_PRESAS).tecnica = "pernas_presas";
            }
            case PEGAR_MANGA -> conexoes.get(lado).add(Conexao.MANGA);
            case LIVRAR_MANGA -> conexoes.get(out).remove(Conexao.MANGA);
            case RECUAR -> {
                IrmaoMiau eu = lutador(lado);
                int s = Integer.signum(eu.getX() - lutador(out).getX());
                int alvo = Math.max(xMin, Math.min(xMax, eu.getX() + (s == 0 ? 1 : s) * 40));
                int antes = eu.getX();
                eu.encostarNaBorda(alvo - eu.getX());
                // 5Q.5: o desenho sai do lugar antigo e desliza (dois passos em pe / empurrao no chao)
                boolean emPe = t.id().equals(CatalogoTecnicasSolo.RECUAR);
                Efeito r = efeito(lado, emPe ? TipoEfeitoVisual.TECNICA_ATAQUE : TipoEfeitoVisual.AFASTANDO,
                        emPe ? TICKS_RECUO : 18);
                r.tecnica = t.id();
                r.deslocamento = eu.getX() - antes;
            }
            case CONSOLIDAR -> {
                if (temEspaco(out)) {                          // 5Q.5: primeiro fecha o espaco do outro
                    espaco.put(out, 0);
                    estabilidade = Math.min(100, estabilidade + CONSOLIDAR_FECHANDO_ESPACO);
                } else {
                    estabilidade = Math.min(100, estabilidade + DELTA_CONSOLIDAR);
                }
            }
            case ENQUADRAR -> {
                estabilidade = Math.max(0, estabilidade - DELTA_ENQUADRAR);
                espaco.put(lado, TICKS_ESPACO);               // 5Q.5: o enquadrar constroi a rota de escape
            }
            case POSTURA -> {
                postura.put(lado, POSTURA_TICKS);
                Efeito q = efeitos.get(lado);                  // 5Q.4: recompoe a postura (volta suave)
                boolean quebrada = q != null && q.restante > 0 && "postura_quebrada".equals(q.tecnica);
                if (t.id().equals(CatalogoTecnicasSolo.POSTURA)) {
                    // 5Q.5: animacao propria: de curvado (ou neutro) ate aprumado, maos no quadril do outro
                    efeito(lado, TipoEfeitoVisual.TECNICA_ATAQUE, POSTURA_TICKS).tecnica = quebrada ? "postura_recompor" : "postura";
                    efeito(out, TipoEfeitoVisual.TECNICA_RECEBE, 22).tecnica = "postura";
                } else if (quebrada) {
                    Efeito lib = efeito(lado, TipoEfeitoVisual.LIBERANDO, 14);
                    lib.tecnica = "postura_quebrada:RECEBE";
                    lib.desde = q.progresso();
                }
            }
            case QUEBRAR_POSTURA -> {
                postura.put(out, 0);
                exposicao.put(out, new Exposto(Exposicao.POSTURA_QUEBRADA, POSTURA_QUEBRADA_TICKS));
                // 5Q.4: quem teve a postura quebrada FICA curvado enquanto durar (e volta suave)
                efeito(out, TipoEfeitoVisual.TECNICA_RECEBE, POSTURA_QUEBRADA_TICKS).tecnica = "postura_quebrada";
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
    }

    /**
     * Entra numa posicao: os DOIS mudam juntos (postura, papel, encaixe). A
     * pegada em pe acaba; a postura vem da posicao.
     */
    private void entrar(EstadoPosicional novo, Lado iniciador, boolean reversao) {
        if (sistema != null && sistema.isInteracaoEstabelecida() && "pegada_3".equals(sistema.getIdAcaoInteracao())
                && novo.getPosicao().isNoChao()) {
            conexoes.get(ladoDe(sistema.getAplicadorInteracao())).add(Conexao.MANGA);   // 5P.2: caiu segurando as mangas
        }
        if (sistema != null && sistema.estaEmInteracao()) {
            sistema.encerrarInteracao();
        }
        if (!novo.getPosicao().isGuarda()) {
            for (Lado l : Lado.values()) {
                conexoes.get(l).clear();                       // fora das guardas a manga solta
            }
        }
        estado = novo;
        afastarAte.clear();                                    // 5Q.5: posicao nova cancela o afastamento
        postura.put(Lado.A, 0);
        postura.put(Lado.B, 0);
        espaco.put(Lado.A, 0);
        espaco.put(Lado.B, 0);
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
        if (p == Posicao.EM_PE_CONTRA_SOLO || p.isGuardaComOutroEmPe()) {
            c.aplicarPosturaDoSolo(EstadoCorporal.EM_PE);       // 5P.2: De La Riva e laco: quem esta por cima em pe
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
            case GUARDA_BORBOLETA -> 40;
            case DE_LA_RIVA -> 58;
            case GUARDA_LACO -> 62;
            case GUARDA_ARANHA -> 66;
            case SINGLE_LEG_X -> 44;
            case ASHI_GARAMI -> 40;
            default -> 50;
        };
    }

    /**
     * 5Q.5: depois do afastar da guarda, quem estava por cima e empurrado ate
     * DISTANCIA_APOS_AFASTAR, aos poucos (sem teletransporte). So no solo livre
     * (EM_PE_CONTRA_SOLO); qualquer posicao nova cancela. Respeita as bordas.
     */
    /**
     * 5Q.6: na janela de uma queda explosiva quem entra AVANCA de verdade ate o contato (antes as
     * entradas so desenhavam o avanco): a distancia restante e dividida pelos ticks que faltam,
     * no maximo VELOCIDADE_AVANCO_QUEDA px/tick (nada de teletransporte).
     */
    private void avancoDaQueda() {
        if (tentativa == null || estado.getPosicao().isNoChao()) {
            return;
        }
        QuedasExplosivas.Perfil pf = QuedasExplosivas.perfil(tentativa.tecnica().id()).orElse(null);
        if (pf == null || pf.pegada()) {
            return;
        }
        IrmaoMiau a = lutador(tentativa.iniciador());
        IrmaoMiau o = lutador(tentativa.iniciador().outro());
        int dist = Math.abs(a.getX() - o.getX());
        int falta = dist - pf.contatoPx();
        if (falta <= 0) {
            return;
        }
        int ticks = Math.max(1, tentativa.ticksRestantes() - 2);
        int passo = Math.min(VELOCIDADE_AVANCO_QUEDA, (falta + ticks - 1) / ticks);
        a.encostarNaBorda(Integer.signum(o.getX() - a.getX()) * passo);
    }

    /** 5Q.6: velocidade maxima do avanco de uma queda explosiva (px/tick). */
    public static final int VELOCIDADE_AVANCO_QUEDA = 9;

    private void afastarGradual() {
        if (afastarAte.isEmpty()) {
            return;
        }
        if (estado.getPosicao() != Posicao.EM_PE_CONTRA_SOLO || tentativa != null) {
            afastarAte.clear();
            return;
        }
        for (Lado l : Lado.values()) {
            Integer alvo = afastarAte.get(l);
            if (alvo == null) {
                continue;
            }
            IrmaoMiau a = lutador(l);
            IrmaoMiau o = lutador(l.outro());
            int dist = Math.abs(a.getX() - o.getX());
            int s = Integer.signum(a.getX() - o.getX());
            int passo = Math.min(VELOCIDADE_AFASTAMENTO, alvo - dist);
            int antes = a.getX();
            if (passo > 0) {
                a.encostarNaBorda((s == 0 ? 1 : s) * passo);
            }
            if (passo <= 0 || a.getX() == antes) {
                afastarAte.remove(l);                          // chegou (ou bateu na borda)
            }
        }
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
        EventoLuta.de(ultimo).ifPresent(t -> {
            eventosLuta.addLast(new EventoLuta.Registro(tick, t, lado, tecnica));
            while (eventosLuta.size() > 32) {
                eventosLuta.removeFirst();
            }
        });
        eventos.addLast(ultimo);
        while (eventos.size() > 40) {
            eventos.removeFirst();
        }
    }

    /**
     * 5Q.2 (laboratorio do treino): comeca uma SITUACAO num estado valido, pelas
     * mesmas regras do motor: limpa tudo (reiniciar), entra na posicao pelos dois
     * lados de uma vez (posturas, papeis, vinculo), da a manga quando a guarda exige,
     * e encaixa a dupla JA na distancia da posicao, na mesma linha e dentro da arena
     * (sem esperar o encaixe gradual). Nao mexe em folego/condicionamento (quem chama
     * recria os lutadores).
     */
    public void iniciarSituacao(Posicao p, Lado porCima, Lado controlador, boolean manga) {
        reiniciar();
        if (p == Posicao.EM_PE_SEPARADOS || p == Posicao.EM_PE_PEGADA) {
            entrar(EstadoPosicional.separados(), Lado.A, false);
            return;                                          // em pe: o estado e a leitura do SistemaCombate
        }
        forcarPosicao(p, porCima, controlador);
        if (manga && controlador != null) {
            conexoes.get(controlador).add(Conexao.MANGA);
        }
        if (porCima != null) {
            IrmaoMiau c = lutador(porCima);
            IrmaoMiau b = lutador(porCima.outro());
            while (c.getLinhaAtual() < b.getLinhaAtual()) {
                c.descerLinha();
            }
            while (c.getLinhaAtual() > b.getLinhaAtual()) {
                c.subirLinha();
            }
            int alvo = Math.max(xMin, Math.min(xMax, b.getX() + frente * afastamento(p)));
            if (alvo != b.getX() + frente * afastamento(p)) {
                b.encostarNaBorda(alvo - (b.getX() + frente * afastamento(p)));
            }
            c.encostarNaBorda(alvo - c.getX());
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
