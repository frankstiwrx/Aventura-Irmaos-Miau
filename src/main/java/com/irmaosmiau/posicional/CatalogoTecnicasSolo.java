package com.irmaosmiau.posicional;

import com.irmaosmiau.posicional.Desfecho.RegraLado;
import com.irmaosmiau.posicional.TecnicaSolo.ComandoSolo;
import com.irmaosmiau.posicional.TecnicaSolo.Criterio;
import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Efeito;
import com.irmaosmiau.posicional.TecnicaSolo.Exposicao;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Entrega 5P: o repertorio de quedas, guardas, passagens, raspagens, controles
 * e escapes. Um lugar so: o MotorSolo, o HUD e a documentacao leem daqui.
 *
 * Mapa dos comandos (mesma logica em toda posicao):
 * - RS para CIMA = avancar / progredir (passar, montar, raspar, subir, levantar);
 * - RS para BAIXO = segurar / defender / estabilizar (postura, consolidar,
 *   retencao, fechar ou recuperar a guarda, enquadrar);
 * - RS para os LADOS (os dois fazem o mesmo) = quadril e transicoes laterais
 *   (escape de quadril, joelho na barriga, costas, joelho cortando);
 * - RB/Select + RS = o explosivo (quedas em pe; ponte/upa, levantar, escapar
 *   para o neutro no chao).
 */
public final class CatalogoTecnicasSolo {

    // ---- em pe
    public static final String EMPURRAO = "empurrao";
    public static final String QUEDA_PEGADA = "queda_pegada";
    public static final String ENTRADA_PERNAS = "entrada_pernas";
    public static final String PUXAR_GUARDA = "puxar_guarda";
    // ---- um em pe, outro no chao
    public static final String ENTRAR_GUARDA = "entrar_guarda";
    public static final String PASSAGEM_EM_PE = "passagem_em_pe";
    public static final String LEVANTAR_TECNICO = "levantar_tecnico";
    public static final String RETENCAO_PES = "retencao_pes";
    public static final String CONECTAR_GUARDA = "conectar_guarda";
    // ---- solo disputado
    public static final String SUBIR_POR_CIMA = "subir_por_cima";
    public static final String PUXAR_PARA_GUARDA = "puxar_para_guarda";
    public static final String LEVANTAR_DO_SOLO = "levantar_do_solo";
    // ---- guarda fechada / aberta / meia
    public static final String POSTURA = "postura";
    public static final String ABRIR_GUARDA = "abrir_guarda";
    public static final String RASPAGEM = "raspagem";
    public static final String QUEBRAR_POSTURA = "quebrar_postura";
    public static final String SOLTAR_GUARDA = "soltar_guarda";
    public static final String PASSAR_GUARDA = "passar_guarda";
    public static final String JOELHO_CORTANDO = "joelho_cortando";
    public static final String CONTROLAR_PERNAS = "controlar_pernas";
    public static final String FECHAR_GUARDA = "fechar_guarda";
    public static final String RETENCAO = "retencao";
    public static final String LEVANTAR_DA_GUARDA = "levantar_da_guarda";
    public static final String LIBERAR_PERNA = "liberar_perna";
    public static final String POSTURA_MEIA = "postura_meia";
    public static final String RECUPERAR_GUARDA = "recuperar_guarda";
    public static final String RASPAGEM_MEIA = "raspagem_meia";
    // ---- cem quilos / joelho na barriga / montada / costas
    public static final String CONSOLIDAR = "consolidar";
    public static final String MONTAR = "montar";
    public static final String JOELHO_NA_BARRIGA = "joelho_na_barriga";
    public static final String ENQUADRAR = "enquadrar";
    public static final String ESCAPE_QUADRIL = "escape_quadril";
    public static final String RECUPERAR_MEIA = "recuperar_meia";
    public static final String VIRAR_JOELHOS = "virar_joelhos";
    public static final String PEGAR_COSTAS_VIRADA = "pegar_costas_virada";
    public static final String MONTAR_DO_JOELHO = "montar_do_joelho";
    public static final String PRESSAO_JOELHO = "pressao_joelho";
    public static final String VOLTAR_CEM_QUILOS = "voltar_cem_quilos";
    public static final String FUGIR_DO_JOELHO = "fugir_do_joelho";
    public static final String ENQUADRAR_JOELHO = "enquadrar_joelho";
    public static final String INVERTER_JOELHO = "inverter_joelho";
    public static final String ESTABILIZAR_MONTADA = "estabilizar_montada";
    public static final String MONTADA_ALTA = "montada_alta";
    public static final String PEGAR_COSTAS = "pegar_costas";
    public static final String PONTE = "ponte";
    public static final String ESCAPE_MONTADA = "escape_montada";
    public static final String ENQUADRAR_MONTADA = "enquadrar_montada";
    public static final String AJUSTAR_GANCHOS = "ajustar_ganchos";
    public static final String DEFENDER_MAOS = "defender_maos";
    public static final String GIRAR_NAS_COSTAS = "girar_nas_costas";
    public static final String ESCAPAR_NEUTRO = "escapar_neutro";
    // ---- 5P.2: pegada ativa e familias em pe (L1 guardas / L2 quedas)
    public static final String TIRO_GUARDA = "tiro_guarda";
    public static final String DERRUBADA_FORCADA = "derrubada_forcada";
    public static final String PUXAR_BORBOLETA = "puxar_borboleta";
    public static final String PUXAR_DLR = "puxar_dlr";
    public static final String RASTEIRA = "rasteira";
    public static final String PROJECAO_QUADRIL = "projecao_quadril";
    public static final String QUEDA_MANGA = "queda_manga";
    // ---- 5P.2: solo livre
    public static final String PRENDER_PERNAS = "prender_pernas";
    /** 5Q.7: pegada persistente nas pernas de quem esta no chao (arrastar pelo tatame). */
    public static final String SEGURAR_PERNAS = "segurar_pernas";
    /** 5Q.7: soltar as pernas seguradas (o mesmo comando do SEGURAR, so enquanto segura). */
    public static final String SOLTAR_PERNAS = "soltar_pernas";
    // ---- 5Q.8: mais rotas nas costas (por cima) e no ashi (quem controla, por baixo)
    public static final String COSTAS_PARA_MONTADA = "costas_para_montada";
    public static final String COSTAS_PARA_CEM = "costas_para_cem";
    public static final String TRIANGULO_CORPO = "triangulo_corpo";
    public static final String ASHI_PARA_SLX = "ashi_para_slx";
    public static final String ASHI_PARA_GUARDA = "ashi_para_guarda";
    public static final String RASPAGEM_ASHI = "raspagem_ashi";
    public static final String RECUAR = "recuar";
    public static final String JOELHO_DIRETO = "joelho_direto";
    public static final String AFASTAR_DO_CHAO = "afastar_do_chao";
    public static final String CONECTAR_BORBOLETA = "conectar_borboleta";
    public static final String CONECTAR_DLR = "conectar_dlr";
    // ---- 5P.2: R1 no chao e desengajar
    public static final String ANGULO_GUARDA = "angulo_guarda";
    public static final String DESEQUILIBRAR_GUARDA = "desequilibrar_guarda";
    public static final String AFASTAR_DA_GUARDA = "afastar_da_guarda";
    public static final String SAIR_DA_GUARDA = "sair_da_guarda";
    public static final String ABANDONAR_POSICAO = "abandonar_posicao";
    // ---- 5P.2: variacoes de guarda
    public static final String PEGAR_MANGA = "pegar_manga";
    public static final String IR_BORBOLETA = "ir_borboleta";
    public static final String IR_DLR = "ir_dlr";
    public static final String IR_LACO = "ir_laco";
    public static final String IR_ABERTA = "ir_aberta";
    public static final String RASPAGEM_BORBOLETA = "raspagem_borboleta";
    public static final String DESEQ_BORBOLETA = "deseq_borboleta";
    public static final String RETER_BORBOLETA = "reter_borboleta";
    public static final String PASSAR_BORBOLETA = "passar_borboleta";
    public static final String POSTURA_BORBOLETA = "postura_borboleta";
    public static final String TIRAR_GANCHOS = "tirar_ganchos";
    public static final String RASPAGEM_DLR = "raspagem_dlr";
    public static final String RETER_DLR = "reter_dlr";
    public static final String DESEQ_DLR = "deseq_dlr";
    public static final String BERIMBOLO = "berimbolo";
    public static final String PASSAR_DLR = "passar_dlr";
    public static final String POSTURA_DLR = "postura_dlr";
    public static final String LIVRAR_GANCHO = "livrar_gancho";
    public static final String RASPAGEM_LACO = "raspagem_laco";
    public static final String TRAVAR_LACO = "travar_laco";
    public static final String PASSAR_LACO = "passar_laco";
    public static final String POSTURA_LACO = "postura_laco";
    public static final String SOLTAR_MANGA = "soltar_manga";
    // ---- 5Q: aranha, single-leg X, controle de perna (ashi garami)
    public static final String IR_ARANHA = "ir_aranha";
    public static final String RASPAGEM_ARANHA = "raspagem_aranha";
    public static final String RETER_ARANHA = "reter_aranha";
    public static final String DESEQ_ARANHA = "deseq_aranha";
    public static final String PASSAR_ARANHA = "passar_aranha";
    public static final String POSTURA_ARANHA = "postura_aranha";
    public static final String LIVRAR_MANGAS = "livrar_mangas";
    public static final String IR_SLX = "ir_slx";
    public static final String RASPAGEM_SLX = "raspagem_slx";
    public static final String RETER_SLX = "reter_slx";
    public static final String PASSAR_SLX = "passar_slx";
    public static final String POSTURA_SLX = "postura_slx";
    public static final String LIVRAR_SLX = "livrar_slx";
    public static final String ENTRADA_PERNA_GUARDA = "entrada_perna_guarda";
    public static final String ENTRADA_PERNA_DOMINANTE = "entrada_perna_dominante";
    public static final String ENTRADA_PERNA_ABERTA = "entrada_perna_aberta";
    public static final String CONSOLIDAR_ASHI = "consolidar_ashi";
    public static final String SOLTAR_PERNA = "soltar_perna";
    public static final String GIRAR_LIVRAR_PERNA = "girar_livrar_perna";
    public static final String ESCONDER_PE = "esconder_pe";
    /** 5Q.5: meia-guarda por baixo: underhook quebra a pressao de ombro (rota de escape). */
    public static final String UNDERHOOK_MEIA = "underhook_meia";
    public static final String LEVANTAR_DO_ASHI = "levantar_do_ashi";

    // ---------- 5Q.6: quedas explosivas sem pegada (LT em pe / de joelhos) e RB -> sem pegada
    public static final String SINGLE_LEG = "single_leg";
    public static final String HIGH_CROTCH = "high_crotch";
    public static final String ANKLE_PICK = "ankle_pick";
    public static final String CARREGADA_OMBRO = "carregada_ombro";
    public static final String BODY_LOCK = "body_lock";
    public static final String SINGLE_LEG_JOELHOS = "single_leg_joelhos";
    public static final String DOUBLE_LEG_JOELHOS = "double_leg_joelhos";
    public static final String ARM_DRAG = "arm_drag";
    public static final String SNAP_DOWN = "snap_down";
    // ---------- 5Q.6: LB + LT com a pegada: projecoes e entradas de guarda
    public static final String UCHI_MATA = "uchi_mata";
    public static final String OSOTO_GARI = "osoto_gari";
    public static final String PUXAR_ARANHA = "puxar_aranha";
    public static final String PUXAR_LACO = "puxar_laco";
    // ---------- 5Q.6: variacoes do berimbolo (L1+L2 <- e -> na De La Riva)
    public static final String BERIMBOLO_KISS = "berimbolo_kiss";
    public static final String BERIMBOLO_INVERTIDO = "berimbolo_invertido";

    /** 5Q.6: postura exigida (o mesmo comando vale outra tecnica em pe e de joelhos). */
    public enum PosturaExigida {
        EM_PE, DE_JOELHOS
    }

    private final Map<String, PosturaExigida> posturas = new LinkedHashMap<>();

    /** 5Q.6: postura exigida pela tecnica (vazio = vale nas duas, como antes). */
    public Optional<PosturaExigida> posturaExigida(String id) {
        return Optional.ofNullable(posturas.get(id));
    }

    /** Custos de folego (o explosivo custa mais; ajustes custam pouco). */
    public static final double CUSTO_AJUSTE = 1.5;

    private final Map<String, TecnicaSolo> porId = new LinkedHashMap<>();
    /** 5P.2: pre-condicoes alem de posicao e papel (o motor confere). */
    private final Map<String, Set<RequisitoTecnica>> requisitos = new LinkedHashMap<>();

    /** 5P.2: requisitos da tecnica (vazio = so posicao e papel). */
    public Set<RequisitoTecnica> requisitos(String id) {
        return requisitos.getOrDefault(id, Set.of());
    }

    private void exigir(String id, RequisitoTecnica... r) {
        requisitos.put(id, Set.of(r));
    }

    /** 5Q: posicao quando a tecnica e DEFENDIDA (relativa a quem tentou; vazio = continua onde estava). */
    private final Map<String, Desfecho> falhas = new LinkedHashMap<>();

    public Optional<Desfecho> falha(String id) {
        return Optional.ofNullable(falhas.get(id));
    }

    /**
     * 5P.2: identificador da animacao futura da tecnica (contrato: "solo/" + id;
     * hoje todas usam a pose procedural da posicao/transicao de postura).
     */
    public static String animacaoFutura(String id) {
        return "solo/" + id;
    }

    private CatalogoTecnicasSolo() {
    }

    public Optional<TecnicaSolo> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }

    public TecnicaSolo exigir(String id) {
        return buscar(id).orElseThrow(() -> new IllegalArgumentException("Tecnica de solo desconhecida: " + id));
    }

    public Collection<TecnicaSolo> todas() {
        return porId.values();
    }

    /** A tecnica que o comando aciona nesta posicao para este papel (no maximo uma). */
    public Optional<TecnicaSolo> para(EstadoPosicional estado, Lado lado, ComandoSolo comando) {
        return para(estado, lado, comando, null);
    }

    /**
     * 5Q.6: idem, sabendo se o confronto (em pe, sem vinculo) e DE JOELHOS: as tecnicas que exigem
     * uma postura so valem nela (null = sem filtro de postura, como antes).
     */
    public Optional<TecnicaSolo> para(EstadoPosicional estado, Lado lado, ComandoSolo comando, Boolean deJoelhos) {
        return porId.values().stream()
                .filter(t -> t.origens().contains(estado.getPosicao()))
                .filter(t -> estado.temPapel(lado, t.papel()))
                .filter(t -> t.aceita(comando))
                .filter(t -> deJoelhos == null || posturaCombina(t.id(), deJoelhos))
                .findFirst();
    }

    private boolean posturaCombina(String id, boolean deJoelhos) {
        PosturaExigida p = posturas.get(id);
        return p == null || (p == PosturaExigida.DE_JOELHOS) == deJoelhos;
    }

    /** O repertorio de um lado na posicao (HUD e documentacao). */
    public List<TecnicaSolo> repertorio(EstadoPosicional estado, Lado lado) {
        return porId.values().stream()
                .filter(t -> t.origens().contains(estado.getPosicao()))
                .filter(t -> estado.temPapel(lado, t.papel()))
                .toList();
    }

    // =====================================================
    // REFERENCIA
    // =====================================================
    private static final RegraLado INI = RegraLado.INICIADOR;
    private static final RegraLado OUT = RegraLado.OUTRO;
    private static final RegraLado NADA = RegraLado.NENHUM;
    private static final RegraLado MANTEM = RegraLado.MANTEM;

    private static ComandoSolo rs(Direcao d) {
        return new ComandoSolo(d, false);
    }

    private static ComandoSolo rb(Direcao d) {
        return new ComandoSolo(d, true);
    }

    private static ComandoSolo l1(Direcao d) {
        return new ComandoSolo(d, TecnicaSolo.Familia.L1);
    }

    private static ComandoSolo l2(Direcao d) {
        return new ComandoSolo(d, TecnicaSolo.Familia.L2);
    }

    private static ComandoSolo l12(Direcao d) {
        return new ComandoSolo(d, TecnicaSolo.Familia.L1_L2);
    }

    /** Efeito com janela (nao troca de posicao; o outro pode responder durante a preparacao). */
    private void efeitoComJanela(String id, String nome, String rotulo, Set<Posicao> de, PapelPosicional papel,
            List<ComandoSolo> cmd, double custo, int janela, Exposicao exp, Efeito efeito, String... contras) {
        add(new TecnicaSolo(id, nome, rotulo, de, papel, cmd, custo, janela, exp, Criterio.SE_NAO_CONTRADA, 0,
                null, Set.of(contras), efeito));
    }

    private static List<ComandoSolo> lados(boolean explosivo) {
        return List.of(new ComandoSolo(Direcao.ESQUERDA, explosivo), new ComandoSolo(Direcao.DIREITA, explosivo));
    }

    private static Desfecho para(Posicao p, RegraLado cima, RegraLado controle) {
        return new Desfecho(p, cima, controle);
    }

    private void add(TecnicaSolo t) {
        if (porId.putIfAbsent(t.id(), t) != null) {
            throw new IllegalArgumentException("Tecnica repetida: " + t.id());
        }
    }

    /** Transicao com janela (troca de posicao). */
    private void transicao(String id, String nome, String rotulo, Set<Posicao> de, PapelPosicional papel,
            List<ComandoSolo> cmd, double custo, int janela, Exposicao exp, Criterio crit, double limiar,
            Desfecho sucesso, String... contras) {
        add(new TecnicaSolo(id, nome, rotulo, de, papel, cmd, custo, janela, exp, crit, limiar, sucesso,
                Set.of(contras), Efeito.NENHUM));
    }

    /** Ajuste instantaneo (sem trocar de posicao). */
    private void ajuste(String id, String nome, String rotulo, Set<Posicao> de, PapelPosicional papel,
            List<ComandoSolo> cmd, Efeito efeito) {
        add(new TecnicaSolo(id, nome, rotulo, de, papel, cmd, CUSTO_AJUSTE, 0, null, Criterio.SE_NAO_CONTRADA, 0,
                null, Set.of(), efeito));
    }

    /** Mudanca instantanea de posicao (sem janela: so o lado que controla o ritmo pode). */
    private void mudanca(String id, String nome, String rotulo, Set<Posicao> de, PapelPosicional papel,
            List<ComandoSolo> cmd, Desfecho d) {
        add(new TecnicaSolo(id, nome, rotulo, de, papel, cmd, CUSTO_AJUSTE, 0, null, Criterio.SE_NAO_CONTRADA, 0,
                d, Set.of(), Efeito.NENHUM));
    }

    public static CatalogoTecnicasSolo referencia() {
        CatalogoTecnicasSolo c = new CatalogoTecnicasSolo();
        Set<Posicao> emPe = EnumSet.of(Posicao.EM_PE_SEPARADOS, Posicao.EM_PE_PEGADA);
        PapelPosicional qq = PapelPosicional.QUALQUER;
        PapelPosicional cima = PapelPosicional.POR_CIMA;
        PapelPosicional baixo = PapelPosicional.POR_BAIXO;
        PapelPosicional ctrl = PapelPosicional.CONTROLADOR;
        PapelPosicional ctrlado = PapelPosicional.CONTROLADO;
        Criterio sempre = Criterio.SE_NAO_CONTRADA;
        Criterio estab = Criterio.ESTABILIDADE_BAIXA;
        Criterio semPostura = Criterio.SEM_POSTURA_DO_OUTRO;

        // ================= EM PE (RB/Select + RS) — resolucao propria no MotorSolo
        c.transicao(EMPURRAO, "Empurrão / pressão", "EMPURRÃO", emPe, qq, List.of(rb(Direcao.CIMA)),
                3.0, 10, Exposicao.AVANCO, sempre, 0, para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA));
        c.transicao(QUEDA_PEGADA, "Queda com a pegada", "QUEDA", EnumSet.of(Posicao.EM_PE_PEGADA), qq,
                List.of(rb(Direcao.DIREITA)), 6.0, 24, Exposicao.BASE, sempre, 0,
                para(Posicao.CONTROLE_LATERAL, INI, INI), QUEDA_PEGADA);
        c.transicao(ENTRADA_PERNAS, "Entrada nas pernas", "PERNAS", emPe, qq, List.of(rb(Direcao.BAIXO)),
                7.0, 14, Exposicao.PERNAS, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI));
        c.transicao(PUXAR_GUARDA, "Puxar guarda", "PUXAR GUARDA", emPe, qq, List.of(rb(Direcao.ESQUERDA),
                l1(Direcao.BAIXO)),
                2.0, 10, null, sempre, 0, para(Posicao.GUARDA, OUT, INI));

        // ================= UM EM PE, OUTRO NO CHAO (por cima = quem esta em pe)
        Set<Posicao> umEmPe = EnumSet.of(Posicao.EM_PE_CONTRA_SOLO);
        c.transicao(ENTRAR_GUARDA, "Entrar na guarda", "ENTRAR", umEmPe, cima, List.of(rs(Direcao.CIMA)),
                2.0, 12, Exposicao.BASE, sempre, 0, para(Posicao.GUARDA_ABERTA, INI, OUT), RETENCAO_PES);
        c.transicao(PASSAGEM_EM_PE, "Passagem em pé (toreando)", "PASSAR", umEmPe, cima,
                List.of(rb(Direcao.CIMA)), 5.0, 16, Exposicao.BASE, sempre, 0,
                para(Posicao.CONTROLE_LATERAL, INI, INI), RETENCAO_PES, CONECTAR_GUARDA);
        c.transicao(LEVANTAR_TECNICO, "Levantar técnico", "LEVANTAR", umEmPe, baixo, List.of(rs(Direcao.CIMA)),
                3.0, 14, Exposicao.BASE, sempre, 0, para(Posicao.EM_PE_SEPARADOS, NADA, NADA), ENTRAR_GUARDA);
        c.transicao(RETENCAO_PES, "Manter distância (pés)", "PÉS", umEmPe, baixo, List.of(rs(Direcao.BAIXO)),
                1.5, 0, null, sempre, 0, null);
        c.transicao(CONECTAR_GUARDA, "Puxar guarda (do chão)", "PUXAR GUARDA", umEmPe, baixo, lados(false),
                2.0, 8, null, sempre, 0, para(Posicao.GUARDA_ABERTA, OUT, INI));

        // ================= SOLO DISPUTADO (ninguem por cima)
        Set<Posicao> disputa = EnumSet.of(Posicao.SOLO_DISPUTADO);
        c.transicao(SUBIR_POR_CIMA, "Subir por cima", "SUBIR", disputa, qq, List.of(rs(Direcao.CIMA)),
                4.0, 12, Exposicao.BASE, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI), PUXAR_PARA_GUARDA);
        c.transicao(PUXAR_PARA_GUARDA, "Puxar guarda", "PUXAR GUARDA", disputa, qq, List.of(rs(Direcao.BAIXO)),
                2.0, 8, null, sempre, 0, para(Posicao.GUARDA, OUT, INI));
        c.transicao(LEVANTAR_DO_SOLO, "Levantar", "LEVANTAR", disputa, qq, List.of(rb(Direcao.CIMA)),
                3.0, 10, Exposicao.BASE, sempre, 0, para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA), SUBIR_POR_CIMA);

        // ================= GUARDA FECHADA (quem esta por baixo controla)
        Set<Posicao> fechada = EnumSet.of(Posicao.GUARDA);
        c.ajuste(POSTURA, "Postura", "POSTURA", fechada, cima, List.of(rs(Direcao.BAIXO)), Efeito.POSTURA);
        c.transicao(ABRIR_GUARDA, "Abrir a guarda do outro", "ABRIR", fechada, cima, List.of(rs(Direcao.CIMA)),
                3.0, 16, Exposicao.BASE, sempre, 0, para(Posicao.GUARDA_ABERTA, MANTEM, MANTEM),
                RASPAGEM, QUEBRAR_POSTURA);
        c.transicao(RASPAGEM, "Raspagem", "RASPAR", EnumSet.of(Posicao.GUARDA, Posicao.GUARDA_ABERTA), baixo,
                List.of(rs(Direcao.CIMA)), 5.0, 14, null, semPostura, 0,
                para(Posicao.GUARDA_ABERTA, INI, OUT), POSTURA, CONTROLAR_PERNAS);
        c.ajuste(QUEBRAR_POSTURA, "Quebrar a postura", "QUEBRAR", fechada, baixo, List.of(rs(Direcao.BAIXO)),
                Efeito.QUEBRAR_POSTURA);
        // 5Q.8: o SOLTAR estava repetido no RS <- e no RS ->; o -> virou a entrada na perna (ver abaixo)
        c.mudanca(SOLTAR_GUARDA, "Soltar a própria guarda", "SOLTAR", fechada, baixo, List.of(rs(Direcao.ESQUERDA)),
                para(Posicao.GUARDA_ABERTA, MANTEM, MANTEM));

        // ================= GUARDA ABERTA (quem esta por baixo controla a distancia)
        Set<Posicao> aberta = EnumSet.of(Posicao.GUARDA_ABERTA);
        c.transicao(PASSAR_GUARDA, "Passar a guarda", "PASSAR", aberta, cima, List.of(rs(Direcao.CIMA)),
                4.0, 20, Exposicao.BASE, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI), RASPAGEM, RETENCAO);
        c.transicao(JOELHO_CORTANDO, "Joelho cortando", "MEIA", aberta, cima, lados(false),
                3.0, 14, Exposicao.BASE, sempre, 0, para(Posicao.MEIA_GUARDA, INI, OUT), RETENCAO);
        c.ajuste(CONTROLAR_PERNAS, "Controlar as pernas", "CONTROLAR", aberta, cima, List.of(rs(Direcao.BAIXO)),
                Efeito.POSTURA);
        c.transicao(FECHAR_GUARDA, "Fechar a guarda", "FECHAR", aberta, baixo, List.of(rs(Direcao.BAIXO)),
                2.0, 10, null, sempre, 0, para(Posicao.GUARDA, OUT, INI), CONTROLAR_PERNAS);
        c.transicao(RETENCAO, "Manter a guarda", "MANTER", aberta, baixo, lados(false),
                1.5, 0, null, sempre, 0, null);
        c.transicao(LEVANTAR_DA_GUARDA, "Levantar da guarda", "LEVANTAR", EnumSet.of(Posicao.GUARDA_ABERTA,
                Posicao.MEIA_GUARDA, Posicao.GUARDA_BORBOLETA, Posicao.DE_LA_RIVA, Posicao.GUARDA_LACO,
                Posicao.GUARDA_ARANHA, Posicao.SINGLE_LEG_X), baixo,
                List.of(rb(Direcao.CIMA)), 4.0, 16, Exposicao.BASE, sempre, 0,
                para(Posicao.EM_PE_SEPARADOS, NADA, NADA), PASSAR_GUARDA, LIBERAR_PERNA, PASSAR_BORBOLETA,
                PASSAR_DLR, PASSAR_LACO, PASSAR_ARANHA, PASSAR_SLX);

        // ================= MEIA-GUARDA (quem esta por baixo controla a perna)
        Set<Posicao> meia = EnumSet.of(Posicao.MEIA_GUARDA);
        c.transicao(LIBERAR_PERNA, "Liberar a perna e passar", "PASSAR", meia, cima, List.of(rs(Direcao.CIMA)),
                4.0, 18, Exposicao.BASE, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI),
                RECUPERAR_GUARDA, RASPAGEM_MEIA);
        c.ajuste(POSTURA_MEIA, "Pressão de ombro", "PRESSÃO", meia, cima, List.of(rs(Direcao.BAIXO)), Efeito.POSTURA);
        c.transicao(RECUPERAR_GUARDA, "Recuperar a guarda", "REC. GUARDA", meia, baixo, List.of(rs(Direcao.BAIXO)),
                3.0, 12, null, semPostura, 0, para(Posicao.GUARDA_ABERTA, OUT, INI), POSTURA_MEIA);
        // 5Q.5: rota de escape da meia: underhook (quebra a pressao de ombro) -> recuperar a guarda / raspar
        c.ajuste(UNDERHOOK_MEIA, "Underhook (tirar a pressão do ombro)", "UNDERHOOK", meia, baixo, lados(false),
                Efeito.QUEBRAR_POSTURA);
        c.transicao(RASPAGEM_MEIA, "Raspagem da meia-guarda", "RASPAR", meia, baixo, List.of(rs(Direcao.CIMA)),
                5.0, 14, null, semPostura, 0, para(Posicao.GUARDA_ABERTA, INI, OUT), POSTURA_MEIA);

        // ================= CONTROLE LATERAL (cem quilos)
        Set<Posicao> lateral = EnumSet.of(Posicao.CONTROLE_LATERAL);
        c.ajuste(CONSOLIDAR, "Consolidar a pressão", "PRESSÃO", lateral, ctrl, List.of(rs(Direcao.BAIXO)),
                Efeito.CONSOLIDAR);
        c.transicao(MONTAR, "Ir para a montada", "MONTAR", lateral, ctrl, List.of(rs(Direcao.CIMA)),
                3.0, 18, Exposicao.CONTROLE_REDUZIDO, sempre, 0, para(Posicao.MONTADA, INI, INI),
                ESCAPE_QUADRIL, RECUPERAR_MEIA);
        c.transicao(JOELHO_NA_BARRIGA, "Joelho na barriga", "JOELHO", lateral, ctrl, lados(false),
                2.5, 10, Exposicao.CONTROLE_REDUZIDO, sempre, 0, para(Posicao.JOELHO_BARRIGA, INI, INI),
                ESCAPE_QUADRIL);
        c.ajuste(ENQUADRAR, "Enquadrar (criar espaço)", "ENQUADRAR", lateral, ctrlado, List.of(rs(Direcao.BAIXO)),
                Efeito.ENQUADRAR);
        c.transicao(ESCAPE_QUADRIL, "Escape de quadril (recuperar a guarda)", "REC. GUARDA", lateral, ctrlado, lados(false),
                4.0, 12, null, estab, 60, para(Posicao.GUARDA_ABERTA, OUT, INI), CONSOLIDAR);
        c.transicao(RECUPERAR_MEIA, "Joelho no meio (recuperar meia-guarda)", "REC. MEIA", lateral, ctrlado,
                List.of(rb(Direcao.BAIXO)), 3.0, 10, null, estab, 65, para(Posicao.MEIA_GUARDA, OUT, INI), CONSOLIDAR);
        c.transicao(VIRAR_JOELHOS, "Virar para os joelhos", "VIRAR", lateral, ctrlado, List.of(rs(Direcao.CIMA)),
                4.0, 14, Exposicao.BASE, estab, 65, para(Posicao.SOLO_DISPUTADO, NADA, NADA), PEGAR_COSTAS_VIRADA);
        c.transicao(PEGAR_COSTAS_VIRADA, "Pegar as costas na virada", "COSTAS", lateral, ctrl,
                List.of(rb(Direcao.CIMA)), 4.0, 8, null, Criterio.OUTRO_EXPOSTO, 70, para(Posicao.COSTAS, INI, INI));
        // 5Q.6: antes so entrava com o outro exposto (virando para os joelhos): do cem quilos parado o RB ^
        // sempre falhava. Agora tambem com o controle consolidado (>= 70); e continua a contra da virada

        // ================= JOELHO NA BARRIGA
        Set<Posicao> joelho = EnumSet.of(Posicao.JOELHO_BARRIGA);
        c.transicao(MONTAR_DO_JOELHO, "Ir para a montada", "MONTAR", joelho, ctrl, List.of(rs(Direcao.CIMA)),
                3.0, 12, Exposicao.CONTROLE_REDUZIDO, sempre, 0, para(Posicao.MONTADA, INI, INI), FUGIR_DO_JOELHO);
        c.ajuste(PRESSAO_JOELHO, "Pressão do joelho", "PRESSÃO", joelho, ctrl, List.of(rs(Direcao.BAIXO)),
                Efeito.CONSOLIDAR);
        c.mudanca(VOLTAR_CEM_QUILOS, "Voltar aos cem quilos", "100 KG", joelho, ctrl, lados(false),
                para(Posicao.CONTROLE_LATERAL, MANTEM, MANTEM));
        c.transicao(FUGIR_DO_JOELHO, "Empurrar o joelho (recuperar a guarda)", "REC. GUARDA", joelho, ctrlado, lados(false),
                3.5, 10, null, estab, 65, para(Posicao.GUARDA_ABERTA, OUT, INI), PRESSAO_JOELHO);
        c.ajuste(ENQUADRAR_JOELHO, "Enquadrar o joelho", "ENQUADRAR", joelho, ctrlado, List.of(rs(Direcao.BAIXO)),
                Efeito.ENQUADRAR);
        c.transicao(INVERTER_JOELHO, "Inverter (base alta)", "INVERTER", joelho, ctrlado, List.of(rb(Direcao.CIMA)),
                5.0, 8, null, Criterio.OUTRO_EXPOSTO, 0, para(Posicao.SOLO_DISPUTADO, NADA, NADA));

        // ================= MONTADA
        Set<Posicao> montada = EnumSet.of(Posicao.MONTADA);
        c.ajuste(ESTABILIZAR_MONTADA, "Estabilizar a montada", "BASE", montada, ctrl, List.of(rs(Direcao.BAIXO)),
                Efeito.CONSOLIDAR);
        c.ajuste(MONTADA_ALTA, "Montada alta", "ALTA", montada, ctrl, List.of(rs(Direcao.CIMA)), Efeito.MONTADA_ALTA);
        c.transicao(PEGAR_COSTAS, "Pegar as costas", "COSTAS", montada, ctrl, lados(false),
                3.5, 16, Exposicao.CONTROLE_REDUZIDO, sempre, 0, para(Posicao.COSTAS, INI, INI),
                ESCAPE_MONTADA, PONTE);
        c.transicao(PONTE, "Ponte (upa)", "UPA", montada, ctrlado, List.of(rb(Direcao.CIMA)),
                6.0, 8, null, estab, 55, para(Posicao.GUARDA, INI, OUT), ESTABILIZAR_MONTADA);
        c.transicao(ESCAPE_MONTADA, "Escape da montada (meia-guarda)", "ESCAPAR", montada, ctrlado, lados(false),
                4.0, 14, null, estab, 65, para(Posicao.MEIA_GUARDA, OUT, INI), ESTABILIZAR_MONTADA);
        c.ajuste(ENQUADRAR_MONTADA, "Enquadrar", "ENQUADRAR", montada, ctrlado, List.of(rs(Direcao.BAIXO)),
                Efeito.ENQUADRAR);

        // ================= COSTAS
        Set<Posicao> costas = EnumSet.of(Posicao.COSTAS);
        c.ajuste(AJUSTAR_GANCHOS, "Ajustar os ganchos", "GANCHOS", costas, ctrl, List.of(rs(Direcao.BAIXO)),
                Efeito.CONSOLIDAR);
        c.ajuste(DEFENDER_MAOS, "Defender com as mãos", "DEFENDER", costas, ctrlado, List.of(rs(Direcao.BAIXO)),
                Efeito.ENQUADRAR);
        c.transicao(GIRAR_NAS_COSTAS, "Tirar os ganchos e girar", "GIRAR", costas, ctrlado, lados(false),
                4.0, 16, null, estab, 55, para(Posicao.GUARDA, INI, OUT), AJUSTAR_GANCHOS);
        c.transicao(ESCAPAR_NEUTRO, "Escapar para o neutro", "ESCAPAR", costas, ctrlado, List.of(rb(Direcao.CIMA)),
                5.0, 12, null, estab, 45, para(Posicao.SOLO_DISPUTADO, NADA, NADA), AJUSTAR_GANCHOS);
        // 5Q.8: o RS de quem esta nas costas tinha so GANCHOS. Agora: virar para a montada, ir para os cem
        // quilos (o outro pode girar e escapar na janela) e travar o quadril com o triangulo de corpo
        c.transicao(COSTAS_PARA_MONTADA, "Virar para a montada", "MONTADA", costas, ctrl, List.of(rs(Direcao.CIMA)),
                3.0, 14, Exposicao.CONTROLE_REDUZIDO, sempre, 0, para(Posicao.MONTADA, INI, INI), GIRAR_NAS_COSTAS);
        c.transicao(COSTAS_PARA_CEM, "Ir para os cem quilos", "100 KG", costas, ctrl, List.of(rs(Direcao.DIREITA)),
                2.5, 12, Exposicao.CONTROLE_REDUZIDO, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI),
                GIRAR_NAS_COSTAS);
        c.ajuste(TRIANGULO_CORPO, "Triângulo de corpo (travar o quadril)", "TRIÂNGULO", costas, ctrl,
                List.of(rs(Direcao.ESQUERDA)), Efeito.CONSOLIDAR);
        // =====================================================================
        // ENTREGA 5P.2
        // =====================================================================
        Set<Posicao> pegada = EnumSet.of(Posicao.EM_PE_PEGADA);
        Set<Posicao> borb = EnumSet.of(Posicao.GUARDA_BORBOLETA);
        Set<Posicao> dlr = EnumSet.of(Posicao.DE_LA_RIVA);
        Set<Posicao> laco = EnumSet.of(Posicao.GUARDA_LACO);
        Set<Posicao> guardas = EnumSet.of(Posicao.GUARDA, Posicao.GUARDA_ABERTA, Posicao.MEIA_GUARDA,
                Posicao.GUARDA_BORBOLETA, Posicao.DE_LA_RIVA, Posicao.GUARDA_LACO, Posicao.GUARDA_ARANHA,
                Posicao.SINGLE_LEG_X);
        Set<Posicao> dominantes = EnumSet.of(Posicao.CONTROLE_LATERAL, Posicao.JOELHO_BARRIGA, Posicao.MONTADA,
                Posicao.COSTAS);
        String[] passes = {PASSAR_GUARDA, LIBERAR_PERNA, PASSAR_BORBOLETA, PASSAR_DLR, PASSAR_LACO, PASSAR_ARANHA,
            PASSAR_SLX};

        // ---------- pegada ativa (aplicador): RS <- tiro de guarda, RS v derrubada forcada
        // (RS ^ reforco e RS -> desequilibrio continuam no SistemaCombate; o receptor continua com o romper)
        c.transicao(TIRO_GUARDA, "Tiro de guarda", "TIRO GUARDA", pegada, ctrl,
                List.of(rs(Direcao.ESQUERDA), l1(Direcao.CIMA)), 4.0, 12, Exposicao.BASE, sempre, 0,
                para(Posicao.GUARDA, OUT, INI));
        c.exigir(TIRO_GUARDA, RequisitoTecnica.PEGADA_APLICADOR);
        c.transicao(DERRUBADA_FORCADA, "Derrubada forçada", "FORÇAR QUEDA", pegada, ctrl,
                List.of(rs(Direcao.BAIXO), l2(Direcao.BAIXO)), 5.0, 16, Exposicao.AVANCO, sempre, 0,
                para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA));
        c.exigir(DERRUBADA_FORCADA, RequisitoTecnica.PEGADA_APLICADOR);

        // ---------- L1 em pe: GUARDAS (v puxar guarda e ^ tiro ja acima)
        c.transicao(PUXAR_BORBOLETA, "Puxar para a borboleta", "BORBOLETA", emPe, qq, List.of(l1(Direcao.ESQUERDA)),
                2.5, 10, null, sempre, 0, para(Posicao.GUARDA_BORBOLETA, OUT, INI));
        c.exigir(PUXAR_BORBOLETA, RequisitoTecnica.PERTO_PARA_PUXAR);
        c.transicao(PUXAR_DLR, "Puxar para a De La Riva", "DE LA RIVA", pegada, ctrl, List.of(l1(Direcao.DIREITA)),
                3.0, 12, Exposicao.BASE, sempre, 0, para(Posicao.DE_LA_RIVA, OUT, INI));
        c.exigir(PUXAR_DLR, RequisitoTecnica.PEGADA_APLICADOR);

        // ---------- L2 em pe: QUEDAS especificas da pegada (v derrubada forcada ja acima)
        c.transicao(RASTEIRA, "Rasteira", "RASTEIRA", pegada, ctrl, List.of(l2(Direcao.CIMA)),
                4.0, 10, Exposicao.BASE, sempre, 0, para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA));
        c.exigir(RASTEIRA, RequisitoTecnica.PEGADA_APLICADOR);
        c.transicao(PROJECAO_QUADRIL, "Projeção de quadril", "QUADRIL", pegada, ctrl, List.of(l2(Direcao.DIREITA)),
                6.5, 22, Exposicao.BASE, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI));
        c.exigir(PROJECAO_QUADRIL, RequisitoTecnica.PEGADA_APLICADOR, RequisitoTecnica.PEGADA_CRUZADA_OU_NUCA);
        c.transicao(QUEDA_MANGA, "Queda pela manga", "MANGA", pegada, ctrl, List.of(l2(Direcao.ESQUERDA)),
                5.5, 18, Exposicao.BASE, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI));
        c.exigir(QUEDA_MANGA, RequisitoTecnica.PEGADA_APLICADOR, RequisitoTecnica.PEGADA_MANGAS);

        // ---------- SOLO LIVRE: quem esta em pe (por cima) e quem esta no chao
        // 5Q.7: o PRENDER estava repetido no RS <- e no RS -> (a mesma tecnica). O <- continua PRENDER;
        // o -> virou SEGURAR AS PERNAS (pegada persistente para arrastar; o mesmo comando solta)
        c.efeitoComJanela(PRENDER_PERNAS, "Prender as pernas", "PRENDER", umEmPe, cima, List.of(rs(Direcao.ESQUERDA)),
                2.5, 12, Exposicao.BASE, Efeito.CONTROLAR_PERNAS, RETENCAO_PES);
        c.exigir(PRENDER_PERNAS, RequisitoTecnica.PERTO_NO_CHAO);
        c.efeitoComJanela(SEGURAR_PERNAS, "Segurar as pernas (arrastar)", "SEGURAR", umEmPe, cima,
                List.of(rs(Direcao.DIREITA)), 2.0, 10, Exposicao.BASE, Efeito.SEGURAR_PERNAS, RETENCAO_PES);
        c.exigir(SEGURAR_PERNAS, RequisitoTecnica.PERTO_NO_CHAO);
        // so aparece enquanto segura (MotorSolo.tecnicaPara troca o SEGURAR por ele); sem posicao propria
        c.ajuste(SOLTAR_PERNAS, "Soltar as pernas", "SOLTAR", EnumSet.noneOf(Posicao.class), cima,
                List.of(rs(Direcao.DIREITA)), Efeito.SOLTAR_PERNAS);
        c.ajuste(RECUAR, "Recuar (dar distância)", "RECUAR", umEmPe, cima, List.of(rs(Direcao.BAIXO)), Efeito.RECUAR);
        // 5Q.5: entra se o outro NAO responder (retencao com os pes / puxar guarda) - antes exigia o outro
        // ja exposto e o RB v direto sempre falhava; com as pernas presas a retencao e recusada
        c.transicao(JOELHO_DIRETO, "Joelho na barriga direto", "JOELHO", umEmPe, cima, List.of(rb(Direcao.BAIXO)),
                4.0, 14, Exposicao.BASE, sempre, 0, para(Posicao.JOELHO_BARRIGA, INI, INI),
                RETENCAO_PES, CONECTAR_GUARDA);
        c.exigir(JOELHO_DIRETO, RequisitoTecnica.PERTO_NO_CHAO);
        c.ajuste(AFASTAR_DO_CHAO, "Afastar (criar distância)", "AFASTAR", umEmPe, baixo, List.of(rb(Direcao.BAIXO)),
                Efeito.RECUAR);
        c.transicao(CONECTAR_BORBOLETA, "Sentar na borboleta", "BORBOLETA", umEmPe, baixo,
                List.of(l1(Direcao.ESQUERDA)), 2.0, 8, null, sempre, 0, para(Posicao.GUARDA_BORBOLETA, OUT, INI), RECUAR);
        c.exigir(CONECTAR_BORBOLETA, RequisitoTecnica.PERTO_NO_CHAO);
        c.transicao(CONECTAR_DLR, "Gancho De La Riva", "DE LA RIVA", umEmPe, baixo, List.of(l1(Direcao.DIREITA)),
                2.5, 10, null, sempre, 0, para(Posicao.DE_LA_RIVA, OUT, INI), RECUAR);
        c.exigir(CONECTAR_DLR, RequisitoTecnica.PERTO_NO_CHAO);

        // ---------- R1 na guarda fechada (por baixo)
        c.efeitoComJanela(DESEQUILIBRAR_GUARDA, "Puxar e desequilibrar", "DESEQ.", fechada, baixo,
                List.of(rb(Direcao.CIMA)), 2.5, 8, null, Efeito.DESEQUILIBRAR, POSTURA);
        c.ajuste(ANGULO_GUARDA, "Ajustar o ângulo do quadril", "ÂNGULO", fechada, baixo, lados(true), Efeito.CONSOLIDAR);

        // ---------- desengajar: abandonar o proprio controle e simples; sair do controle do outro exige chance
        c.transicao(AFASTAR_DA_GUARDA, "Abrir e afastar com os pés", "AFASTAR", guardas, baixo,
                List.of(rb(Direcao.BAIXO)), 2.5, 12, null, sempre, 0, para(Posicao.EM_PE_CONTRA_SOLO, OUT, NADA),
                passes);
        c.transicao(SAIR_DA_GUARDA, "Criar espaço e levantar", "LEVANTAR", guardas, cima, List.of(rb(Direcao.BAIXO)),
                3.5, 16, Exposicao.BASE, estab, 60, para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA),
                QUEBRAR_POSTURA, RETENCAO, RETER_BORBOLETA, RETER_DLR, TRAVAR_LACO, RETER_ARANHA, RETER_SLX);
        c.transicao(ABANDONAR_POSICAO, "Soltar a posição", "SOLTAR", dominantes, ctrl, List.of(rb(Direcao.BAIXO)),
                1.5, 6, Exposicao.CONTROLE_REDUZIDO, sempre, 0, para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA));

        // ---------- variacoes de guarda (por baixo, L1): roda de guardas; transicao curta e exposta (os passes entram)
        Set<Posicao> abertas = EnumSet.of(Posicao.GUARDA_ABERTA, Posicao.GUARDA_BORBOLETA, Posicao.DE_LA_RIVA,
                Posicao.GUARDA_LACO);
        c.ajuste(PEGAR_MANGA, "Pegar a manga", "MANGA", EnumSet.of(Posicao.GUARDA_ABERTA, Posicao.GUARDA_BORBOLETA,
                Posicao.DE_LA_RIVA), baixo, List.of(l2(Direcao.BAIXO)), Efeito.PEGAR_MANGA);
        c.transicao(IR_BORBOLETA, "Para a guarda borboleta", "BORBOLETA", EnumSet.of(Posicao.GUARDA_ABERTA,
                Posicao.DE_LA_RIVA),
                baixo, List.of(l1(Direcao.ESQUERDA)), 1.5, 6, Exposicao.BASE, sempre, 0,
                para(Posicao.GUARDA_BORBOLETA, MANTEM, MANTEM), passes);
        c.transicao(IR_DLR, "Para a De La Riva", "DE LA RIVA", menos(abertas, Posicao.DE_LA_RIVA), baixo,
                List.of(l1(Direcao.DIREITA)), 1.5, 6, Exposicao.BASE, sempre, 0,
                para(Posicao.DE_LA_RIVA, MANTEM, MANTEM), passes);
        c.transicao(IR_LACO, "Para a guarda laço", "LAÇO", EnumSet.of(Posicao.GUARDA_ABERTA, Posicao.GUARDA_BORBOLETA,
                Posicao.DE_LA_RIVA, Posicao.GUARDA_ARANHA), baixo,
                List.of(l1(Direcao.BAIXO)), 1.5, 6, Exposicao.BASE, sempre, 0,
                para(Posicao.GUARDA_LACO, MANTEM, MANTEM), passes);
        c.exigir(IR_LACO, RequisitoTecnica.CONEXAO_MANGA);
        c.transicao(IR_ABERTA, "Voltar à guarda aberta", "ABERTA", EnumSet.of(Posicao.GUARDA_BORBOLETA,
                Posicao.DE_LA_RIVA, Posicao.GUARDA_LACO, Posicao.GUARDA_ARANHA, Posicao.SINGLE_LEG_X), baixo,
                List.of(l1(Direcao.CIMA)), 1.5, 6, Exposicao.BASE, sempre, 0,
                para(Posicao.GUARDA_ABERTA, MANTEM, MANTEM), passes);

        // ---------- BORBOLETA: elevacao (raspagem), desequilibrio, manter; por cima: amassar, base, tirar ganchos
        c.transicao(RASPAGEM_BORBOLETA, "Raspagem de borboleta (elevação)", "ELEVAR", borb, baixo,
                List.of(rs(Direcao.CIMA)), 4.5, 14, null, semPostura, 0, para(Posicao.GUARDA_ABERTA, INI, OUT),
                POSTURA_BORBOLETA);
        c.efeitoComJanela(DESEQ_BORBOLETA, "Desequilibrar para o lado", "DESEQ.", borb, baixo, lados(false),
                2.0, 8, null, Efeito.DESEQUILIBRAR, POSTURA_BORBOLETA);
        c.ajuste(RETER_BORBOLETA, "Manter os ganchos", "MANTER", borb, baixo, List.of(rs(Direcao.BAIXO)),
                Efeito.CONSOLIDAR);
        c.transicao(PASSAR_BORBOLETA, "Amassar e passar", "PASSAR", borb, cima, List.of(rs(Direcao.CIMA)),
                4.5, 18, Exposicao.BASE, estab, 60, para(Posicao.CONTROLE_LATERAL, INI, INI), RASPAGEM_BORBOLETA);
        c.ajuste(POSTURA_BORBOLETA, "Afundar o quadril (base)", "BASE", borb, cima, List.of(rs(Direcao.BAIXO)),
                Efeito.POSTURA);
        c.ajuste(TIRAR_GANCHOS, "Tirar os ganchos", "GANCHOS", borb, cima, lados(false), Efeito.ENQUADRAR);

        // ---------- DE LA RIVA (quem esta por cima em pe): raspagem rapida, berimbolo (L1+L2)
        c.transicao(RASPAGEM_DLR, "Raspagem De La Riva", "RASPAR", dlr, baixo, List.of(rs(Direcao.CIMA)),
                4.0, 12, null, semPostura, 0, para(Posicao.MEIA_GUARDA, INI, OUT), POSTURA_DLR);
        c.ajuste(RETER_DLR, "Manter o gancho", "MANTER", dlr, baixo, List.of(rs(Direcao.BAIXO)), Efeito.CONSOLIDAR);
        c.efeitoComJanela(DESEQ_DLR, "Desequilibrar a perna", "DESEQ.", dlr, baixo, lados(false),
                2.0, 8, null, Efeito.DESEQUILIBRAR, POSTURA_DLR);
        c.transicao(BERIMBOLO, "Berimbolo", "BERIMBOLO", dlr, ctrl, List.of(l12(Direcao.CIMA)),
                6.0, 20, Exposicao.BASE, Criterio.CONTROLE_PROPRIO, 50, para(Posicao.COSTAS, INI, INI),
                POSTURA_DLR, LIVRAR_GANCHO);
        // 5Q.6: variacoes do berimbolo (o atual continua no ^; o ashi da DLR continua no v)
        c.transicao(BERIMBOLO_KISS, "Kiss of the dragon (inversão por baixo)", "KISS DRAGON", dlr, ctrl,
                List.of(l12(Direcao.ESQUERDA)), 7.0, 24, Exposicao.BASE, Criterio.CONTROLE_PROPRIO, 60,
                para(Posicao.COSTAS, INI, INI), POSTURA_DLR, LIVRAR_GANCHO);
        c.transicao(BERIMBOLO_INVERTIDO, "Baby bolo (berimbolo para cima)", "BABY BOLO", dlr, ctrl,
                List.of(l12(Direcao.DIREITA)), 5.0, 16, Exposicao.BASE, semPostura, 0,
                para(Posicao.CONTROLE_LATERAL, INI, INI), POSTURA_DLR);
        c.transicao(PASSAR_DLR, "Quebrar o gancho e passar", "PASSAR", dlr, cima, List.of(rs(Direcao.CIMA)),
                4.5, 18, Exposicao.BASE, estab, 55, para(Posicao.CONTROLE_LATERAL, INI, INI), RASPAGEM_DLR, BERIMBOLO);
        c.ajuste(POSTURA_DLR, "Base (quadril para trás)", "BASE", dlr, cima, List.of(rs(Direcao.BAIXO)), Efeito.POSTURA);
        c.transicao(LIVRAR_GANCHO, "Livrar a perna do gancho", "LIVRAR", dlr, cima, lados(false),
                2.5, 10, Exposicao.BASE, sempre, 0, para(Posicao.GUARDA_ABERTA, MANTEM, MANTEM), RETER_DLR);

        // ---------- LACO (exige a manga): controle forte, passagem dificil, raspagem pelo proprio controle
        c.transicao(RASPAGEM_LACO, "Raspagem do laço", "RASPAR", laco, baixo, List.of(rs(Direcao.CIMA)),
                4.5, 14, null, Criterio.CONTROLE_PROPRIO, 40, para(Posicao.MEIA_GUARDA, INI, OUT), POSTURA_LACO);
        c.ajuste(TRAVAR_LACO, "Travar o laço", "TRAVAR", laco, baixo, List.of(rs(Direcao.BAIXO)), Efeito.CONSOLIDAR);
        c.transicao(PASSAR_LACO, "Passar o laço", "PASSAR", laco, cima, List.of(rs(Direcao.CIMA)),
                5.0, 20, Exposicao.BASE, estab, 45, para(Posicao.CONTROLE_LATERAL, INI, INI), RASPAGEM_LACO);
        c.ajuste(POSTURA_LACO, "Base", "BASE", laco, cima, List.of(rs(Direcao.BAIXO)), Efeito.POSTURA);
        c.transicao(SOLTAR_MANGA, "Soltar a manga", "SOLTAR MANGA", laco, cima, lados(false),
                2.5, 10, Exposicao.BASE, sempre, 0, para(Posicao.GUARDA_ABERTA, MANTEM, MANTEM), TRAVAR_LACO);

        // =====================================================================
        // ENTREGA 5Q
        // =====================================================================
        // ---------- GUARDA ARANHA (exige a manga): pes nos biceps, distancia; por cima: livrar as mangas
        Set<Posicao> aranha = EnumSet.of(Posicao.GUARDA_ARANHA);
        c.transicao(IR_ARANHA, "Para a guarda aranha", "ARANHA", EnumSet.of(Posicao.GUARDA_ABERTA, Posicao.GUARDA_LACO),
                baixo, List.of(l2(Direcao.ESQUERDA)), 1.5, 6, Exposicao.BASE, sempre, 0,
                para(Posicao.GUARDA_ARANHA, MANTEM, MANTEM), passes);
        c.exigir(IR_ARANHA, RequisitoTecnica.CONEXAO_MANGA);
        c.transicao(RASPAGEM_ARANHA, "Raspagem da aranha", "RASPAR", aranha, baixo, List.of(rs(Direcao.CIMA)),
                4.5, 14, null, semPostura, 0, para(Posicao.GUARDA_ABERTA, INI, OUT), POSTURA_ARANHA);
        c.ajuste(RETER_ARANHA, "Esticar e recolher (manter)", "MANTER", aranha, baixo, List.of(rs(Direcao.BAIXO)),
                Efeito.CONSOLIDAR);
        c.efeitoComJanela(DESEQ_ARANHA, "Desequilibrar com os pés", "DESEQ.", aranha, baixo, lados(false),
                2.0, 8, null, Efeito.DESEQUILIBRAR, POSTURA_ARANHA);
        c.transicao(PASSAR_ARANHA, "Passar a aranha", "PASSAR", aranha, cima, List.of(rs(Direcao.CIMA)),
                5.0, 20, Exposicao.BASE, estab, 50, para(Posicao.CONTROLE_LATERAL, INI, INI), RASPAGEM_ARANHA);
        c.ajuste(POSTURA_ARANHA, "Base (cotovelos para dentro)", "BASE", aranha, cima, List.of(rs(Direcao.BAIXO)),
                Efeito.POSTURA);
        c.transicao(LIVRAR_MANGAS, "Livrar as mangas", "LIVRAR", aranha, cima, lados(false),
                2.5, 10, Exposicao.BASE, sempre, 0, para(Posicao.GUARDA_ABERTA, MANTEM, MANTEM), RETER_ARANHA);

        // ---------- SINGLE-LEG X (contra quem esta em pe): raspagem de levantar, entrada na perna
        Set<Posicao> slx = EnumSet.of(Posicao.SINGLE_LEG_X);
        c.transicao(IR_SLX, "Para a single-leg X", "SLX", EnumSet.of(Posicao.GUARDA_BORBOLETA, Posicao.DE_LA_RIVA),
                baixo, List.of(l2(Direcao.DIREITA)), 1.5, 8, Exposicao.BASE, sempre, 0,
                para(Posicao.SINGLE_LEG_X, MANTEM, MANTEM), passes);
        c.transicao(RASPAGEM_SLX, "Raspagem da single-leg X", "RASPAR", slx, baixo, List.of(rs(Direcao.CIMA)),
                4.0, 12, null, semPostura, 0, para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA), POSTURA_SLX);
        c.ajuste(RETER_SLX, "Manter o X", "MANTER", slx, baixo, List.of(rs(Direcao.BAIXO)), Efeito.CONSOLIDAR);
        c.transicao(PASSAR_SLX, "Livrar a perna e passar", "PASSAR", slx, cima, List.of(rs(Direcao.CIMA)),
                4.5, 18, Exposicao.BASE, estab, 55, para(Posicao.CONTROLE_LATERAL, INI, INI), RASPAGEM_SLX);
        c.ajuste(POSTURA_SLX, "Base (peso na perna presa)", "BASE", slx, cima, List.of(rs(Direcao.BAIXO)),
                Efeito.POSTURA);
        c.transicao(LIVRAR_SLX, "Tirar a perna do X", "LIVRAR", slx, cima, lados(false),
                2.5, 10, Exposicao.BASE, sempre, 0, para(Posicao.GUARDA_ABERTA, MANTEM, MANTEM), RETER_SLX);

        // ---------- ENTRADAS NO CONTROLE DE PERNA (L1+L2): transicoes reais, defensaveis, com risco
        List<ComandoSolo> quatroL12 = List.of(l12(Direcao.CIMA), l12(Direcao.DIREITA), l12(Direcao.BAIXO),
                l12(Direcao.ESQUERDA));
        Desfecho ashi = para(Posicao.ASHI_GARAMI, OUT, INI);
        // 5Q.8 (pedido): a entrada na perna ocupava as QUATRO direcoes do LT + LB (a mesma tecnica); foi para o
        // RS -> na guarda fechada (o SOLTAR estava repetido nos dois lados; o RB dali esta todo ocupado) e
        // RB <- nas dominantes (livre). O LT + LB da guarda, da montada e dos cem quilos ganhou 4 finalizacoes
        List<ComandoSolo> entradaPerna = List.of(rb(Direcao.ESQUERDA));
        c.transicao(ENTRADA_PERNA_GUARDA, "Abrir a guarda e entrar na perna", "→ PERNA", fechada, baixo,
                List.of(rs(Direcao.DIREITA)),
                3.5, 14, Exposicao.BASE, Criterio.CONTROLE_PROPRIO, 30, ashi, POSTURA);
        c.falhas.put(ENTRADA_PERNA_GUARDA, para(Posicao.GUARDA_ABERTA, OUT, INI));      // a guarda ja abriu
        c.transicao(ENTRADA_PERNA_DOMINANTE, "Abandonar o controle e disputar a perna", "→ PERNA", dominantes, ctrl,
                entradaPerna, 3.5, 16, Exposicao.CONTROLE_REDUZIDO, Criterio.CONTROLE_PROPRIO, 50, ashi,
                ESCAPE_QUADRIL, RECUPERAR_MEIA, ESCAPE_MONTADA, GIRAR_NAS_COSTAS, FUGIR_DO_JOELHO);
        c.falhas.put(ENTRADA_PERNA_DOMINANTE, para(Posicao.GUARDA_ABERTA, INI, OUT));   // o outro recupera a guarda
        c.transicao(ENTRADA_PERNA_ABERTA, "Controlar a perna (ashi garami)", "ASHI", EnumSet.of(Posicao.GUARDA_ABERTA,
                Posicao.SINGLE_LEG_X), baixo, List.of(l12(Direcao.CIMA)), 3.0, 10, Exposicao.BASE, semPostura, 0, ashi,
                CONTROLAR_PERNAS, POSTURA_SLX);
        c.transicao(ENTRADA_PERNA_ABERTA + "_dlr", "Da De La Riva para a perna", "ASHI", dlr, baixo,
                List.of(l12(Direcao.BAIXO)), 3.0, 10, Exposicao.BASE, semPostura, 0, ashi, POSTURA_DLR);

        // =====================================================================
        // ENTREGA 5Q.6
        // =====================================================================
        // ---------- LT sem pegada, EM PE: quedas explosivas (avanco, captura, derrubada); o sprawl vale
        Set<Posicao> livres = EnumSet.of(Posicao.EM_PE_SEPARADOS);
        c.transicao(SINGLE_LEG, "Single leg (uma perna)", "SINGLE LEG", livres, qq, List.of(l2(Direcao.CIMA)),
                6.0, 14, Exposicao.PERNAS, sempre, 0, para(Posicao.MEIA_GUARDA, INI, OUT));
        c.transicao(HIGH_CROTCH, "High crotch (por fora, no quadril)", "HIGH CROTCH", livres, qq,
                List.of(l2(Direcao.DIREITA)), 6.5, 14, Exposicao.PERNAS, sempre, 0,
                para(Posicao.CONTROLE_LATERAL, INI, INI));
        c.transicao(ANKLE_PICK, "Ankle pick (puxa a cabeça, pega o tornozelo)", "TORNOZELO", livres, qq,
                List.of(l2(Direcao.ESQUERDA)), 4.5, 10, Exposicao.BASE, sempre, 0,
                para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA));
        c.transicao(CARREGADA_OMBRO, "Carregada no ombro", "OMBRO", livres, qq, List.of(l2(Direcao.BAIXO)),
                7.5, 18, Exposicao.PERNAS, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI));
        // RB -> sem pegada (com pegada continua a queda com a pegada): mais uma queda explosiva no RB
        c.transicao(BODY_LOCK, "Agarrar a cintura (body lock)", "CINTURA", livres, qq, List.of(rb(Direcao.DIREITA)),
                6.0, 14, Exposicao.BASE, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI));
        for (String id : new String[] {SINGLE_LEG, HIGH_CROTCH, ANKLE_PICK, CARREGADA_OMBRO, BODY_LOCK}) {
            c.posturas.put(id, PosturaExigida.EM_PE);
        }
        // ---------- LT sem pegada, DE JOELHOS: entradas curtas, pouco deslocamento e amplitude
        c.transicao(SINGLE_LEG_JOELHOS, "Single leg de joelhos", "SINGLE LEG", livres, qq, List.of(l2(Direcao.CIMA)),
                4.5, 12, Exposicao.BASE, sempre, 0, para(Posicao.MEIA_GUARDA, INI, OUT));
        c.transicao(DOUBLE_LEG_JOELHOS, "Double leg de joelhos (tombar de lado)", "DOUBLE LEG", livres, qq,
                List.of(l2(Direcao.DIREITA)), 5.0, 12, Exposicao.BASE, sempre, 0,
                para(Posicao.CONTROLE_LATERAL, INI, INI));
        c.transicao(ARM_DRAG, "Arrastar o braço (arm drag)", "ARM DRAG", livres, qq, List.of(l2(Direcao.ESQUERDA)),
                4.0, 10, Exposicao.BASE, sempre, 0, para(Posicao.COSTAS, INI, INI));
        c.transicao(SNAP_DOWN, "Puxar a cabeça (snap down)", "SNAP DOWN", livres, qq, List.of(l2(Direcao.BAIXO)),
                4.0, 10, Exposicao.BASE, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI));
        for (String id : new String[] {SINGLE_LEG_JOELHOS, DOUBLE_LEG_JOELHOS, ARM_DRAG, SNAP_DOWN}) {
            c.posturas.put(id, PosturaExigida.DE_JOELHOS);
        }
        // ---------- LB + LT com a pegada (em pe): duas projecoes e duas entradas de guarda
        c.transicao(UCHI_MATA, "Uchi-mata (ceifa por dentro da coxa)", "UCHI-MATA", pegada, ctrl,
                List.of(l12(Direcao.CIMA)), 6.5, 20, Exposicao.BASE, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI));
        c.exigir(UCHI_MATA, RequisitoTecnica.PEGADA_APLICADOR);
        c.transicao(OSOTO_GARI, "Osoto-gari (grande ceifada por fora)", "OSOTO", pegada, ctrl,
                List.of(l12(Direcao.DIREITA)), 5.5, 16, Exposicao.BASE, sempre, 0,
                para(Posicao.CONTROLE_LATERAL, INI, INI));
        c.exigir(OSOTO_GARI, RequisitoTecnica.PEGADA_APLICADOR);
        c.transicao(PUXAR_ARANHA, "Puxar para a guarda aranha", "ARANHA", pegada, ctrl, List.of(l12(Direcao.ESQUERDA)),
                3.0, 12, Exposicao.BASE, sempre, 0, para(Posicao.GUARDA_ARANHA, OUT, INI));
        c.exigir(PUXAR_ARANHA, RequisitoTecnica.PEGADA_APLICADOR, RequisitoTecnica.PEGADA_MANGAS);
        c.transicao(PUXAR_LACO, "Puxar para a guarda laço", "LAÇO", pegada, ctrl, List.of(l12(Direcao.BAIXO)),
                3.0, 12, Exposicao.BASE, sempre, 0, para(Posicao.GUARDA_LACO, OUT, INI));
        c.exigir(PUXAR_LACO, RequisitoTecnica.PEGADA_APLICADOR);
        for (String id : new String[] {UCHI_MATA, OSOTO_GARI, PUXAR_ARANHA, PUXAR_LACO}) {
            c.posturas.put(id, PosturaExigida.EM_PE);
        }

        // ---------- ASHI GARAMI: quem controla consolida/solta; o outro gira, esconde o pe ou levanta
        Set<Posicao> perna = EnumSet.of(Posicao.ASHI_GARAMI);
        c.ajuste(CONSOLIDAR_ASHI, "Ajustar o controle da perna", "AJUSTAR", perna, ctrl, List.of(rs(Direcao.BAIXO)),
                Efeito.CONSOLIDAR);
        // 5Q.8: o RS de quem controla o ashi tinha so AJUSTAR. Rotas: subir para o single-leg X, recompor a
        // guarda aberta e raspar levantando (precisa de controle). A 50/50 nao e uma posicao do motor (so a
        // variacao da chave de pe): ficou de fora. O outro responde girando/escondendo o pe na janela.
        c.transicao(ASHI_PARA_SLX, "Subir para o single-leg X", "SLX", perna, ctrl, List.of(rs(Direcao.DIREITA)),
                2.5, 12, null, sempre, 0, para(Posicao.SINGLE_LEG_X, OUT, INI), GIRAR_LIVRAR_PERNA);
        c.transicao(ASHI_PARA_GUARDA, "Recompor a guarda aberta", "G. ABERTA", perna, ctrl, List.of(rs(Direcao.CIMA)),
                2.0, 10, null, sempre, 0, para(Posicao.GUARDA_ABERTA, OUT, INI));
        c.transicao(RASPAGEM_ASHI, "Raspar levantando (do ashi)", "RASPAR", perna, ctrl, List.of(rs(Direcao.ESQUERDA)),
                4.0, 14, Exposicao.BASE, Criterio.CONTROLE_PROPRIO, 45, para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA),
                ESCONDER_PE);
        c.transicao(SOLTAR_PERNA, "Soltar a perna", "SOLTAR", perna, ctrl, List.of(rb(Direcao.BAIXO)),
                1.5, 6, null, sempre, 0, para(Posicao.SOLO_DISPUTADO, NADA, NADA));
        c.ajuste(ESCONDER_PE, "Esconder o pé", "ESCONDER", perna, ctrlado, List.of(rs(Direcao.BAIXO)), Efeito.ENQUADRAR);
        c.transicao(GIRAR_LIVRAR_PERNA, "Girar e livrar a perna", "GIRAR", perna, ctrlado, lados(false),
                4.0, 14, null, estab, 60, para(Posicao.SOLO_DISPUTADO, NADA, NADA), CONSOLIDAR_ASHI);
        c.transicao(LEVANTAR_DO_ASHI, "Levantar e puxar a perna", "LEVANTAR", perna, ctrlado, List.of(rb(Direcao.CIMA)),
                4.5, 12, Exposicao.BASE, estab, 45, para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA), CONSOLIDAR_ASHI);
        return c;
    }

    private static Set<Posicao> menos(Set<Posicao> s, Posicao p) {
        Set<Posicao> r = EnumSet.copyOf(s);
        r.remove(p);
        return r;
    }
}
