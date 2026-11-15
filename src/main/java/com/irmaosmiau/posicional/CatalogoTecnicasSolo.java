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

    /** Custos de folego (o explosivo custa mais; ajustes custam pouco). */
    public static final double CUSTO_AJUSTE = 1.5;

    private final Map<String, TecnicaSolo> porId = new LinkedHashMap<>();

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
        return porId.values().stream()
                .filter(t -> t.origens().contains(estado.getPosicao()))
                .filter(t -> estado.temPapel(lado, t.papel()))
                .filter(t -> t.aceita(comando))
                .findFirst();
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
        c.transicao(PUXAR_GUARDA, "Puxar para a guarda", "PUXAR GUARDA", emPe, qq, List.of(rb(Direcao.ESQUERDA)),
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
        c.transicao(RETENCAO_PES, "Retenção com os pés", "PÉS", umEmPe, baixo, List.of(rs(Direcao.BAIXO)),
                1.5, 0, null, sempre, 0, null);
        c.transicao(CONECTAR_GUARDA, "Conectar na guarda", "CONECTAR", umEmPe, baixo, lados(false),
                2.0, 8, null, sempre, 0, para(Posicao.GUARDA_ABERTA, OUT, INI));

        // ================= SOLO DISPUTADO (ninguem por cima)
        Set<Posicao> disputa = EnumSet.of(Posicao.SOLO_DISPUTADO);
        c.transicao(SUBIR_POR_CIMA, "Subir por cima", "SUBIR", disputa, qq, List.of(rs(Direcao.CIMA)),
                4.0, 12, Exposicao.BASE, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI), PUXAR_PARA_GUARDA);
        c.transicao(PUXAR_PARA_GUARDA, "Puxar para a guarda", "GUARDA", disputa, qq, List.of(rs(Direcao.BAIXO)),
                2.0, 8, null, sempre, 0, para(Posicao.GUARDA, OUT, INI));
        c.transicao(LEVANTAR_DO_SOLO, "Levantar", "LEVANTAR", disputa, qq, List.of(rb(Direcao.CIMA)),
                3.0, 10, Exposicao.BASE, sempre, 0, para(Posicao.EM_PE_CONTRA_SOLO, INI, NADA), SUBIR_POR_CIMA);

        // ================= GUARDA FECHADA (quem esta por baixo controla)
        Set<Posicao> fechada = EnumSet.of(Posicao.GUARDA);
        c.ajuste(POSTURA, "Postura", "POSTURA", fechada, cima, List.of(rs(Direcao.BAIXO)), Efeito.POSTURA);
        c.transicao(ABRIR_GUARDA, "Abrir a guarda", "ABRIR", fechada, cima, List.of(rs(Direcao.CIMA)),
                3.0, 16, Exposicao.BASE, sempre, 0, para(Posicao.GUARDA_ABERTA, MANTEM, MANTEM),
                RASPAGEM, QUEBRAR_POSTURA);
        c.transicao(RASPAGEM, "Raspagem", "RASPAR", EnumSet.of(Posicao.GUARDA, Posicao.GUARDA_ABERTA), baixo,
                List.of(rs(Direcao.CIMA)), 5.0, 14, null, semPostura, 0,
                para(Posicao.GUARDA_ABERTA, INI, OUT), POSTURA, CONTROLAR_PERNAS);
        c.ajuste(QUEBRAR_POSTURA, "Quebrar a postura", "QUEBRAR", fechada, baixo, List.of(rs(Direcao.BAIXO)),
                Efeito.QUEBRAR_POSTURA);
        c.mudanca(SOLTAR_GUARDA, "Abrir a guarda (ajustar o quadril)", "ABRIR", fechada, baixo, lados(false),
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
        c.transicao(RETENCAO, "Retenção de guarda", "RETER", aberta, baixo, lados(false),
                1.5, 0, null, sempre, 0, null);
        c.transicao(LEVANTAR_DA_GUARDA, "Levantar da guarda", "LEVANTAR", aberta, baixo, List.of(rb(Direcao.CIMA)),
                4.0, 16, Exposicao.BASE, sempre, 0, para(Posicao.EM_PE_SEPARADOS, NADA, NADA), PASSAR_GUARDA);

        // ================= MEIA-GUARDA (quem esta por baixo controla a perna)
        Set<Posicao> meia = EnumSet.of(Posicao.MEIA_GUARDA);
        c.transicao(LIBERAR_PERNA, "Liberar a perna e passar", "PASSAR", meia, cima, List.of(rs(Direcao.CIMA)),
                4.0, 18, Exposicao.BASE, sempre, 0, para(Posicao.CONTROLE_LATERAL, INI, INI),
                RECUPERAR_GUARDA, RASPAGEM_MEIA);
        c.ajuste(POSTURA_MEIA, "Pressão de ombro", "PRESSÃO", meia, cima, List.of(rs(Direcao.BAIXO)), Efeito.POSTURA);
        c.transicao(RECUPERAR_GUARDA, "Recuperar a guarda", "GUARDA", meia, baixo, List.of(rs(Direcao.BAIXO)),
                3.0, 12, null, semPostura, 0, para(Posicao.GUARDA_ABERTA, OUT, INI), POSTURA_MEIA);
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
        c.transicao(ESCAPE_QUADRIL, "Escape de quadril", "QUADRIL", lateral, ctrlado, lados(false),
                4.0, 12, null, estab, 50, para(Posicao.GUARDA_ABERTA, OUT, INI), CONSOLIDAR);
        c.transicao(RECUPERAR_MEIA, "Joelho no meio (meia-guarda)", "MEIA", lateral, ctrlado,
                List.of(rb(Direcao.BAIXO)), 3.0, 10, null, estab, 60, para(Posicao.MEIA_GUARDA, OUT, INI), CONSOLIDAR);
        c.transicao(VIRAR_JOELHOS, "Virar para os joelhos", "VIRAR", lateral, ctrlado, List.of(rs(Direcao.CIMA)),
                4.0, 14, Exposicao.BASE, estab, 60, para(Posicao.SOLO_DISPUTADO, NADA, NADA), PEGAR_COSTAS_VIRADA);
        c.transicao(PEGAR_COSTAS_VIRADA, "Pegar as costas na virada", "COSTAS", lateral, ctrl,
                List.of(rb(Direcao.CIMA)), 4.0, 8, null, Criterio.OUTRO_EXPOSTO, 0, para(Posicao.COSTAS, INI, INI));

        // ================= JOELHO NA BARRIGA
        Set<Posicao> joelho = EnumSet.of(Posicao.JOELHO_BARRIGA);
        c.transicao(MONTAR_DO_JOELHO, "Ir para a montada", "MONTAR", joelho, ctrl, List.of(rs(Direcao.CIMA)),
                3.0, 12, Exposicao.CONTROLE_REDUZIDO, sempre, 0, para(Posicao.MONTADA, INI, INI), FUGIR_DO_JOELHO);
        c.ajuste(PRESSAO_JOELHO, "Pressão do joelho", "PRESSÃO", joelho, ctrl, List.of(rs(Direcao.BAIXO)),
                Efeito.CONSOLIDAR);
        c.mudanca(VOLTAR_CEM_QUILOS, "Voltar aos cem quilos", "100 KG", joelho, ctrl, lados(false),
                para(Posicao.CONTROLE_LATERAL, MANTEM, MANTEM));
        c.transicao(FUGIR_DO_JOELHO, "Empurrar o joelho e fugir", "FUGIR", joelho, ctrlado, lados(false),
                3.5, 10, null, estab, 60, para(Posicao.GUARDA_ABERTA, OUT, INI), PRESSAO_JOELHO);
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
                6.0, 8, null, estab, 45, para(Posicao.GUARDA, INI, OUT), ESTABILIZAR_MONTADA);
        c.transicao(ESCAPE_MONTADA, "Escape de quadril (meia-guarda)", "QUADRIL", montada, ctrlado, lados(false),
                4.0, 14, null, estab, 60, para(Posicao.MEIA_GUARDA, OUT, INI), ESTABILIZAR_MONTADA);
        c.ajuste(ENQUADRAR_MONTADA, "Enquadrar", "ENQUADRAR", montada, ctrlado, List.of(rs(Direcao.BAIXO)),
                Efeito.ENQUADRAR);

        // ================= COSTAS
        Set<Posicao> costas = EnumSet.of(Posicao.COSTAS);
        c.ajuste(AJUSTAR_GANCHOS, "Ajustar os ganchos", "GANCHOS", costas, ctrl, List.of(rs(Direcao.BAIXO)),
                Efeito.CONSOLIDAR);
        c.ajuste(DEFENDER_MAOS, "Defender com as mãos", "DEFENDER", costas, ctrlado, List.of(rs(Direcao.BAIXO)),
                Efeito.ENQUADRAR);
        c.transicao(GIRAR_NAS_COSTAS, "Tirar os ganchos e girar", "GIRAR", costas, ctrlado, lados(false),
                4.0, 16, null, estab, 50, para(Posicao.GUARDA, INI, OUT), AJUSTAR_GANCHOS);
        c.transicao(ESCAPAR_NEUTRO, "Escapar para o neutro", "ESCAPAR", costas, ctrlado, List.of(rb(Direcao.CIMA)),
                5.0, 12, null, estab, 35, para(Posicao.SOLO_DISPUTADO, NADA, NADA), AJUSTAR_GANCHOS);
        return c;
    }
}
