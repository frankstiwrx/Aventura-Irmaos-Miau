package com.irmaosmiau.persistencia;

import java.nio.file.Path;

/**
 * Erro ao ler ou gravar dados do jogador.
 *
 * O {@link Motivo} permite que a interface futura reaja de forma diferente
 * (ex.: oferecer restaurar padroes para um arquivo corrompido) e que os
 * testes verifiquem o tipo de falha.
 */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Motivo {
        /** Nao e JSON valido (arquivo corrompido ou editado errado). */
        ARQUIVO_CORROMPIDO,
        /** JSON valido, mas de outro tipo ou de versao de esquema nao suportada. */
        VERSAO_INCOMPATIVEL,
        /** JSON e versao corretos, mas valores fora das regras. */
        DADOS_INVALIDOS,
        /** Registro ja existe / nao existe quando deveria. */
        CONFLITO,
        /** Falha de disco (permissao, espaco, etc.). */
        ERRO_DE_ARQUIVO
    }

    private final Motivo motivo;
    private final Path arquivo;

    public PersistenciaException(Motivo motivo, Path arquivo, String mensagem) {
        this(motivo, arquivo, mensagem, null);
    }

    public PersistenciaException(Motivo motivo, Path arquivo, String mensagem, Throwable causa) {
        super((arquivo == null ? "" : "[" + arquivo + "] ") + mensagem, causa);
        this.motivo = motivo;
        this.arquivo = arquivo;
    }

    public Motivo getMotivo() {
        return motivo;
    }

    /** Arquivo envolvido (pode ser null). */
    public Path getArquivo() {
        return arquivo;
    }
}
