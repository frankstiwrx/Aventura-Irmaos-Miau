package com.irmaosmiau.combate;

import com.irmaosmiau.entities.IrmaoMiau;

/**
 * Retrato imutavel do corpo de um lutador num instante.
 *
 * A PoliticaInteracao compara o retrato tirado quando a interacao foi
 * estabelecida com o retrato atual. Assim a regra depende do que o corpo
 * REALMENTE fez (andou, pulou, deitou...), e nao de qual tecla foi
 * apertada: funciona igual para teclado, IA ou testes.
 */
public final class EstadoCorporalObservado {

    private final int x;
    private final int y;
    private final int linha;
    private final boolean pulando;
    private final boolean agachado;
    private final boolean deitado;
    private final boolean rolando;
    private final boolean fazendoSprawl;
    // 5J: de joelhos (levantar no meio da pegada e uma mudanca corporal).
    private final boolean ajoelhado;

    public EstadoCorporalObservado(
            int x,
            int y,
            int linha,
            boolean pulando,
            boolean agachado,
            boolean deitado,
            boolean rolando,
            boolean fazendoSprawl
    ) {
        this.x = x;
        this.y = y;
        this.linha = linha;
        this.pulando = pulando;
        this.agachado = agachado;
        this.deitado = deitado;
        this.ajoelhado = false;
        this.rolando = rolando;
        this.fazendoSprawl = fazendoSprawl;
    }

    /** Entrega 5J: com a postura de joelhos. */
    public EstadoCorporalObservado(int x, int y, int linha, boolean pulando, boolean agachado, boolean deitado,
            boolean rolando, boolean fazendoSprawl, boolean ajoelhado) {
        this.x = x;
        this.y = y;
        this.linha = linha;
        this.pulando = pulando;
        this.agachado = agachado;
        this.deitado = deitado;
        this.rolando = rolando;
        this.fazendoSprawl = fazendoSprawl;
        this.ajoelhado = ajoelhado;
    }

    public boolean isAjoelhado() {
        return ajoelhado;
    }

    public static EstadoCorporalObservado de(IrmaoMiau lutador) {
        if (lutador == null) {
            throw new IllegalArgumentException("O lutador nao pode ser nulo.");
        }
        return new EstadoCorporalObservado(
                lutador.getX(),
                lutador.getY(),
                lutador.getLinhaAtual(),
                lutador.isPulando(),
                lutador.isAgachado(),
                lutador.isDeitado(),
                lutador.isRolando(),
                lutador.isFazendoSprawl(),
                lutador.isAjoelhado()
        );
    }

    /** Mesmo retrato deslocado no eixo X (deslocamento conjunto, 5B). */
    public EstadoCorporalObservado transladado(int dx) {
        return new EstadoCorporalObservado(x + dx, y, linha, pulando, agachado, deitado, rolando, fazendoSprawl,
                ajoelhado);
    }

    /** 5N: o mesmo retrato com a dupla deslocada em X e em profundidade (arrasto multidirecional). */
    public EstadoCorporalObservado transladado(int dx, int dy, int novaLinha) {
        return new EstadoCorporalObservado(x + dx, y + dy, novaLinha, pulando, agachado, deitado, rolando,
                fazendoSprawl, ajoelhado);
    }

    public int getX() {
        return x;
    }

    /** Y fisico (linha de profundidade menos a altura do pulo). */
    public int getY() {
        return y;
    }

    public int getLinha() {
        return linha;
    }

    public boolean isPulando() {
        return pulando;
    }

    public boolean isAgachado() {
        return agachado;
    }

    public boolean isDeitado() {
        return deitado;
    }

    public boolean isRolando() {
        return rolando;
    }

    public boolean isFazendoSprawl() {
        return fazendoSprawl;
    }

    @Override
    public String toString() {
        return "EstadoCorporal[x=" + x + ", y=" + y + ", linha=" + linha
                + (pulando ? ", pulando" : "")
                + (agachado ? ", agachado" : "")
                + (deitado ? ", deitado" : "")
                + (rolando ? ", rolando" : "")
                + (fazendoSprawl ? ", sprawl" : "")
                + "]";
    }
}
