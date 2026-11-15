package com.irmaosmiau.controles;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.posicional.CatalogoTransicoes;
import com.irmaosmiau.posicional.DefinicaoTransicao;
import com.irmaosmiau.posicional.EstadoPosicional;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.LeituraPosicionalEmPe;
import com.irmaosmiau.posicional.MotorPosicional;
import com.irmaosmiau.posicional.RepertorioPosicional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * As ate 12 posicoes de comando de cada contexto (Entrega 5F).
 *
 * Camada 1 (direcao sozinha): as acoes que JA existem no jogo, pelo papel:
 * - livre: cima = PEGAR (PEGADA_1);
 * - aplicador da pegada: cima = REFORCAR, direita = DESEQUILIBRAR;
 * - receptor da pegada: baixo = ROMPER.
 * Camadas 2 e 3: as transicoes da 5E pelo RepertorioPosicional (ACAO_1..4
 * -> cima, direita, baixo, esquerda; sem/com modificador -> camada 2/3). Como
 * ainda terminam no chao, aparecem RESERVADAS e nao executam nada.
 *
 * Posicoes sem nada ficam vazias: nenhuma tecnica foi inventada para preencher.
 */
public final class RepertorioContextual {

    private static final Map<PapelCombate, Map<PosicaoComando, AcaoJogador>> EXECUTAVEIS =
            new EnumMap<>(PapelCombate.class);

    static {
        for (PapelCombate p : PapelCombate.values()) {
            EXECUTAVEIS.put(p, new java.util.LinkedHashMap<>());
        }
        EXECUTAVEIS.get(PapelCombate.LIVRE).put(c1(DirecaoTecnica.CIMA), AcaoJogador.INICIAR_PEGADA);
        // Entrega 5L: livre, as quatro direcoes sao as quatro pegadas.
        EXECUTAVEIS.get(PapelCombate.LIVRE).put(c1(DirecaoTecnica.DIREITA), AcaoJogador.PEGADA_2);
        EXECUTAVEIS.get(PapelCombate.LIVRE).put(c1(DirecaoTecnica.BAIXO), AcaoJogador.PEGADA_3);
        EXECUTAVEIS.get(PapelCombate.LIVRE).put(c1(DirecaoTecnica.ESQUERDA), AcaoJogador.PEGADA_4);
        EXECUTAVEIS.get(PapelCombate.APLICADOR).put(c1(DirecaoTecnica.CIMA), AcaoJogador.REFORCAR_PEGADA);
        EXECUTAVEIS.get(PapelCombate.APLICADOR).put(c1(DirecaoTecnica.DIREITA), AcaoJogador.TENTAR_DESEQUILIBRIO);
        EXECUTAVEIS.get(PapelCombate.RECEPTOR).put(c1(DirecaoTecnica.BAIXO), AcaoJogador.ROMPIMENTO_TECNICO);
    }

    private static final DirecaoTecnica[] DIRECAO_DO_SLOT = {
        DirecaoTecnica.CIMA, DirecaoTecnica.DIREITA, DirecaoTecnica.BAIXO, DirecaoTecnica.ESQUERDA
    };

    private static final CatalogoTransicoes TRANSICOES = CatalogoTransicoes.referencia();
    private static final RepertorioPosicional POSICIONAL = RepertorioPosicional.referencia(TRANSICOES);

    private RepertorioContextual() {
    }

    private static PosicaoComando c1(DirecaoTecnica d) {
        return new PosicaoComando(CamadaComando.UM, d);
    }

    /** Rotulo curto para o HUD. */
    public static String rotuloCurto(AcaoJogador a) {
        return switch (a) {
            case INICIAR_PEGADA -> "PEGAR";
            case PEGADA_2 -> "CRUZADA";
            case PEGADA_3 -> "MANGAS";
            case PEGADA_4 -> "NUCA";
            case TENTAR_DESEQUILIBRIO -> "DESEQ.";
            case REFORCAR_PEGADA -> "REFORÇO";
            case ROMPIMENTO_TECNICO -> "ROMPER";
            default -> a.getVerbo().toUpperCase();
        };
    }

    static String rotuloCurto(DefinicaoTransicao t) {
        return switch (t.getId()) {
            case CatalogoTransicoes.QUEDA -> "QUEDA";
            case CatalogoTransicoes.PUXAR_GUARDA -> "GUARDA";
            case CatalogoTransicoes.PASSAGEM_GUARDA -> "PASSAR";
            case CatalogoTransicoes.RASPAGEM -> "RASPAR";
            case CatalogoTransicoes.REPOSICAO_GUARDA -> "REPOR";
            default -> t.getNome().toUpperCase();
        };
    }

    /**
     * Celulas de um contexto (logica pura, testavel).
     *
     * @param disponivel previsao de disponibilidade das acoes executaveis
     */
    public static List<CelulaContextual> celulas(SlotJogador slot, PapelCombate papel, EstadoPosicional estado,
            Lado lado, Predicate<AcaoJogador> disponivel) {
        List<CelulaContextual> lista = new ArrayList<>();
        if (papel == PapelCombate.ENTRANDO) {
            return lista;                                   // 30 quadros da pegada: ninguem age
        }
        for (Map.Entry<PosicaoComando, AcaoJogador> e : EXECUTAVEIS.get(papel).entrySet()) {
            if (slot.usa(e.getValue())) {
                lista.add(new CelulaContextual(e.getKey(), CelulaContextual.Tipo.EXECUTAVEL,
                        rotuloCurto(e.getValue()), e.getValue(), null, disponivel.test(e.getValue())));
            }
        }
        for (RepertorioPosicional.Opcao o : POSICIONAL.opcoes(estado, lado)) {
            if (MotorPosicional.liberadaNoJogo(o.transicao())) {
                continue;                                   // ja e uma acao da camada 1 (PEGADA_1)
            }
            PosicaoComando pos = new PosicaoComando(o.comModificador() ? CamadaComando.TRES : CamadaComando.DOIS,
                    DIRECAO_DO_SLOT[o.slot().ordinal()]);
            lista.add(new CelulaContextual(pos, CelulaContextual.Tipo.RESERVADA, rotuloCurto(o.transicao()), null,
                    o.transicao().getId(), false));
        }
        return lista;
    }

    /** Celulas do lutador na luta real (le o SistemaCombate; nao muda nada). */
    public static List<CelulaContextual> paraCombate(SistemaCombate sistema, SlotJogador slot, IrmaoMiau ator,
            IrmaoMiau outro) {
        PapelCombate papel = PapelCombate.de(sistema, ator);
        EstadoPosicional estado = LeituraPosicionalEmPe.ler(sistema, TRANSICOES);
        Lado lado = LeituraPosicionalEmPe.lado(sistema, ator);
        return celulas(slot, papel, estado, lado, a -> disponivelAgora(sistema, a, ator, outro));
    }

    static boolean disponivelAgora(SistemaCombate s, AcaoJogador a, IrmaoMiau ator, IrmaoMiau outro) {
        return switch (a) {
            case INICIAR_PEGADA, PEGADA_2, PEGADA_3, PEGADA_4 ->
                    s.podeTentarAcao(ator, outro, AcoesContextuais.idPegada(a));
            case TENTAR_DESEQUILIBRIO, ROMPIMENTO_TECNICO -> s.getRecargaRestante(ator) == 0;
            case REFORCAR_PEGADA -> s.getRecargaReforcoRestante(ator) == 0 && s.getFirmeza() < 100;
            default -> true;
        };
    }

    /** Posicao (camada 1) de uma acao existente no papel, se houver. */
    public static Optional<PosicaoComando> posicaoDe(PapelCombate papel, AcaoJogador acao) {
        return EXECUTAVEIS.get(papel).entrySet().stream()
                .filter(e -> e.getValue() == acao).map(Map.Entry::getKey).findFirst();
    }

    public static Optional<CelulaContextual> celula(List<CelulaContextual> celulas, PosicaoComando pos) {
        return celulas.stream().filter(c -> c.posicao().equals(pos)).findFirst();
    }
}
