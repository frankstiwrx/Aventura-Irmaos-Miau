package com.irmaosmiau.posicional;

import java.util.Optional;
import java.util.Set;

/**
 * Entrega 5P.2: eventos de LUTA classificados a partir dos eventos do MotorSolo,
 * para alimentar regras futuras (pontuacao, vitoria por exaustao...). Nesta
 * entrega so sao identificados e enfileirados: NENHUM ponto e concedido.
 */
public enum EventoLuta {
    QUEDA,
    RASPAGEM,
    PASSAGEM,
    MONTADA,
    COSTAS,
    ESCAPE,
    RECUPERACAO_GUARDA,
    ABANDONO,
    EXAUSTAO_CRITICA;

    /** Um evento de luta com quem o causou. */
    public record Registro(long tick, EventoLuta tipo, Lado lado, String tecnica) {
    }

    private static final Set<String> QUEDAS = Set.of(CatalogoTecnicasSolo.QUEDA_PEGADA,
            CatalogoTecnicasSolo.ENTRADA_PERNAS, CatalogoTecnicasSolo.RASTEIRA, CatalogoTecnicasSolo.PROJECAO_QUADRIL,
            CatalogoTecnicasSolo.QUEDA_MANGA, CatalogoTecnicasSolo.DERRUBADA_FORCADA);
    private static final Set<String> RASPAGENS = Set.of(CatalogoTecnicasSolo.RASPAGEM,
            CatalogoTecnicasSolo.RASPAGEM_MEIA, CatalogoTecnicasSolo.RASPAGEM_BORBOLETA,
            CatalogoTecnicasSolo.RASPAGEM_DLR, CatalogoTecnicasSolo.RASPAGEM_LACO, CatalogoTecnicasSolo.PONTE,
            CatalogoTecnicasSolo.INVERTER_JOELHO);
    private static final Set<String> PASSAGENS = Set.of(CatalogoTecnicasSolo.PASSAR_GUARDA,
            CatalogoTecnicasSolo.LIBERAR_PERNA, CatalogoTecnicasSolo.PASSAGEM_EM_PE,
            CatalogoTecnicasSolo.PASSAR_BORBOLETA, CatalogoTecnicasSolo.PASSAR_DLR, CatalogoTecnicasSolo.PASSAR_LACO);
    private static final Set<String> MONTADAS = Set.of(CatalogoTecnicasSolo.MONTAR,
            CatalogoTecnicasSolo.MONTAR_DO_JOELHO);
    private static final Set<String> COSTAS_ = Set.of(CatalogoTecnicasSolo.PEGAR_COSTAS,
            CatalogoTecnicasSolo.PEGAR_COSTAS_VIRADA, CatalogoTecnicasSolo.BERIMBOLO);
    private static final Set<String> ESCAPES = Set.of(CatalogoTecnicasSolo.ESCAPE_MONTADA,
            CatalogoTecnicasSolo.GIRAR_NAS_COSTAS, CatalogoTecnicasSolo.ESCAPAR_NEUTRO,
            CatalogoTecnicasSolo.VIRAR_JOELHOS);
    private static final Set<String> RECUPERACOES = Set.of(CatalogoTecnicasSolo.RECUPERAR_GUARDA,
            CatalogoTecnicasSolo.ESCAPE_QUADRIL, CatalogoTecnicasSolo.FUGIR_DO_JOELHO,
            CatalogoTecnicasSolo.RECUPERAR_MEIA);
    private static final Set<String> ABANDONOS = Set.of(CatalogoTecnicasSolo.ABANDONAR_POSICAO,
            CatalogoTecnicasSolo.SAIR_DA_GUARDA, CatalogoTecnicasSolo.AFASTAR_DA_GUARDA);

    /** Classificacao de um evento do motor (vazio = nao e evento de luta). */
    public static Optional<EventoLuta> de(MotorSolo.Evento e) {
        String t = e.tecnica();
        return switch (e.tipo()) {
            case "SUCESSO", "CONTRA" -> {
                if (QUEDAS.contains(t)) {
                    yield e.detalhe().startsWith("desequilibrou") ? Optional.empty() : Optional.of(QUEDA);
                }
                if (t.equals(CatalogoTecnicasSolo.EMPURRAO)) {
                    yield e.detalhe().startsWith("derrubou") ? Optional.of(QUEDA) : Optional.empty();
                }
                yield RASPAGENS.contains(t) ? Optional.of(RASPAGEM)
                        : PASSAGENS.contains(t) ? Optional.of(PASSAGEM)
                        : MONTADAS.contains(t) ? Optional.of(MONTADA)
                        : COSTAS_.contains(t) ? Optional.of(COSTAS)
                        : ESCAPES.contains(t) ? Optional.of(ESCAPE)
                        : RECUPERACOES.contains(t) ? Optional.of(RECUPERACAO_GUARDA)
                        : ABANDONOS.contains(t) ? Optional.of(ABANDONO)
                        : Optional.empty();
            }
            case "REVERSAO" -> Optional.of(QUEDA);
            case "EXAUSTAO" -> Optional.of(EXAUSTAO_CRITICA);
            default -> Optional.empty();
        };
    }
}
