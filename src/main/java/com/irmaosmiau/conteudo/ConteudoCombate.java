package com.irmaosmiau.conteudo;

import com.irmaosmiau.combate.RegistroAcoes;
import com.irmaosmiau.combate.RegistroAnimacoesCombate;
import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Conjunto coerente de tecnicas + animacoes carregado de arquivos.
 *
 * Uso:
 * <pre>
 * ConteudoCombate conteudo = CarregadorConteudoCombate.carregarPadrao();
 * SistemaCombate sistema = conteudo.criarSistema(branco, preto);
 * </pre>
 */
public final class ConteudoCombate {

    private final RegistroAcoes registroAcoes;
    private final RegistroAnimacoesCombate registroAnimacoes;

    public ConteudoCombate(RegistroAcoes registroAcoes, RegistroAnimacoesCombate registroAnimacoes) {
        if (registroAcoes == null || registroAnimacoes == null) {
            throw new IllegalArgumentException("Registros de acoes e animacoes sao obrigatorios.");
        }
        this.registroAcoes = registroAcoes;
        this.registroAnimacoes = registroAnimacoes;
    }

    public RegistroAcoes getRegistroAcoes() {
        return registroAcoes;
    }

    public RegistroAnimacoesCombate getRegistroAnimacoes() {
        return registroAnimacoes;
    }

    public SistemaCombate criarSistema(IrmaoMiau lutadorA, IrmaoMiau lutadorB) {
        return new SistemaCombate(lutadorA, lutadorB, registroAcoes, registroAnimacoes);
    }
}
