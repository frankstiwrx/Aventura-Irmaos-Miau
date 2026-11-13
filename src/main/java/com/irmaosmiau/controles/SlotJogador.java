package com.irmaosmiau.controles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Os dois conjuntos de controles do teclado (Entrega 5C).
 *
 * JOGADOR_1 controla o Miau Branco (ou o unico lutador do modo individual e
 * o jogador do treino); JOGADOR_2 controla o Miau Preto no modo Os Dois.
 * O prefixo e o mesmo dos comandos desde a Entrega 1 ("branco"/"preto").
 */
public enum SlotJogador {
    JOGADOR_1("branco", "jogador1", "JOGADOR 1", "J1"),
    JOGADOR_2("preto", "jogador2", "JOGADOR 2", "J2");

    private final String prefixoComando;
    private final String idArquivo;
    private final String nome;
    private final String sigla;

    SlotJogador(String prefixoComando, String idArquivo, String nome, String sigla) {
        this.prefixoComando = prefixoComando;
        this.idArquivo = idArquivo;
        this.nome = nome;
        this.sigla = sigla;
    }

    public String getPrefixoComando() {
        return prefixoComando;
    }

    /** Chave usada em controles.json. */
    public String getIdArquivo() {
        return idArquivo;
    }

    public String getNome() {
        return nome;
    }

    public String getSigla() {
        return sigla;
    }

    /** Nome do comando no ActionMap / TravaComandos (ex.: "brancoPegada1"). */
    public String comando(AcaoJogador acao) {
        return prefixoComando + acao.getSufixoComando();
    }

    /** Acoes que este slot usa hoje, na ordem do enum. */
    public List<AcaoJogador> acoes() {
        List<AcaoJogador> lista = new ArrayList<>();
        for (AcaoJogador a : AcaoJogador.values()) {
            if (a.nomeTeclaPadrao(this) != null) {
                lista.add(a);
            }
        }
        return Collections.unmodifiableList(lista);
    }

    public boolean usa(AcaoJogador acao) {
        return acao != null && acao.nomeTeclaPadrao(this) != null;
    }

    public static SlotJogador porIdArquivo(String id) {
        for (SlotJogador s : values()) {
            if (s.idArquivo.equals(id)) {
                return s;
            }
        }
        return null;
    }
}
