package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.AcaoJogador;

/** Algo que pode receber um botao: uma acao de jogador OU um comando contextual (Entrega 5F). */
public record ItemGamepad(AcaoJogador acao, ComandoGamepad comando) {

    public ItemGamepad {
        if ((acao == null) == (comando == null)) {
            throw new IllegalArgumentException("Exatamente um: acao ou comando.");
        }
    }

    public static ItemGamepad de(AcaoJogador a) {
        return new ItemGamepad(a, null);
    }

    public static ItemGamepad de(ComandoGamepad c) {
        return new ItemGamepad(null, c);
    }

    public String rotulo() {
        return acao != null ? acao.getRotulo() : comando.getRotulo();
    }

    /** Nome estavel para o arquivo (nome da constante). */
    public String nome() {
        return acao != null ? acao.name() : comando.name();
    }
}
