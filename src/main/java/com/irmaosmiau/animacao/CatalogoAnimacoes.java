package com.irmaosmiau.animacao;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Todas as animacoes conhecidas (Entrega 5E): legadas (fallback, codigo
 * atual), JSON individuais e a conjunta PEGADA_1. Registrar uma animacao
 * aqui NAO muda o jogo: quem escolhe e o PerfilAnimacoes de cada personagem.
 */
public final class CatalogoAnimacoes {

    public static final String RECURSO_BRANCO_CHAO = "/conteudo/animacoes/individuais/branco_movimento_chao.json";
    public static final String RECURSO_PRETO_CHAO = "/conteudo/animacoes/individuais/preto_movimento_chao.json";

    private final Map<String, DefinicaoAnimacao> porId = new LinkedHashMap<>();

    public CatalogoAnimacoes registrar(DefinicaoAnimacao d) {
        if (porId.putIfAbsent(d.id(), d) != null) {
            throw new IllegalArgumentException("Animacao repetida: " + d.id());
        }
        return this;
    }

    public Optional<DefinicaoAnimacao> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }

    public DefinicaoAnimacao exigir(String id) {
        return buscar(id).orElseThrow(() -> new IllegalArgumentException("Animacao nao cadastrada: " + id));
    }

    public Collection<DefinicaoAnimacao> todas() {
        return porId.values();
    }

    /** Legadas do Branco e do Preto + JSON migradas + conjuntas. */
    public static CatalogoAnimacoes padrao() {
        CatalogoAnimacoes c = new CatalogoAnimacoes();
        for (String p : new String[]{"branco", "preto"}) {
            String classe = p.equals("branco") ? "IrmaoMiauBranco" : "IrmaoMiauPreto";
            c.registrar(DefinicaoAnimacao.legada(p + ".parado.legada", EstadoVisual.PARADO,
                    classe + ".desenharEmPe (idle procedural de IrmaoMiau)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".taunt.legada", EstadoVisual.TAUNT,
                    classe + ".TAUNT_MIAUSTUDIO"));
            c.registrar(DefinicaoAnimacao.legada(p + ".caminhada.legada", EstadoVisual.CAMINHADA,
                    classe + ".desenharEmPe + IrmaoMiau.getOscilacaoPasso (senoide, amplitude 10)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".corrida.legada", EstadoVisual.CORRIDA,
                    classe + ".desenharEmPe + IrmaoMiau.getOscilacaoPasso (senoide, amplitude 15)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".agachado.legada", EstadoVisual.AGACHADO,
                    classe + ".desenharEmPe (encolhimento procedural)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".pulo.legada", EstadoVisual.PULO,
                    classe + ".desenharEmPe (deslocamento vertical do pulo)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".rolamento.legada", EstadoVisual.ROLAMENTO,
                    classe + ".ROLAMENTO_MIAUSTUDIO (interpolado por progresso)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".sprawl.legada", EstadoVisual.SPRAWL,
                    classe + ".SPRAWL_MIAUSTUDIO (interpolado por progresso)"));
            c.registrar(DefinicaoAnimacao.legada(p + ".deitado.legada", EstadoVisual.DEITADO,
                    classe + ".MOVIMENTO_CHAO_MIAUSTUDIO[0]"));
            c.registrar(DefinicaoAnimacao.legada(p + ".movimento_chao.legada", EstadoVisual.MOVIMENTO_CHAO,
                    classe + ".MOVIMENTO_CHAO_MIAUSTUDIO"));
        }
        c.registrar(DefinicaoAnimacao.json("branco.movimento_chao.json", RECURSO_BRANCO_CHAO,
                CarregadorAnimacaoJson.doRecurso(RECURSO_BRANCO_CHAO)));
        c.registrar(DefinicaoAnimacao.json("preto.movimento_chao.json", RECURSO_PRETO_CHAO,
                CarregadorAnimacaoJson.doRecurso(RECURSO_PRETO_CHAO)));
        // Conjuntas: a PEGADA_1 continua com o MiauStudio + RegistroAnimacoesCombate.
        c.registrar(DefinicaoAnimacao.legada("pegada_1.entrada", EstadoVisual.PEGADA_ENTRADA,
                "RegistroAnimacoesCombate:pegada_1_entrada (/conteudo/miaustudio/PEGADA_1.json, 30 quadros)"));
        c.registrar(DefinicaoAnimacao.legada("pegada_1.estabelecida", EstadoVisual.PEGADA_ESTABELECIDA,
                "RegistroAnimacoesCombate:pegada_1_entrada, ultimo quadro (pose final persistente)"));
        c.registrar(DefinicaoAnimacao.legada("arrasto_conjunto.legado", EstadoVisual.ARRASTO_CONJUNTO,
                "pose final da PEGADA_1 transladada pelo SistemaCombate.deslocarPegadaControlada (5B)"));
        return c;
    }
}
