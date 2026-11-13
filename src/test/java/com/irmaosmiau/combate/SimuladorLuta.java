package com.irmaosmiau.combate;

import com.irmaosmiau.conteudo.CarregadorConteudoCombate;
import com.irmaosmiau.entities.Direcao;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.util.function.Consumer;

/**
 * Reproduz, sem Swing, a ordem de um tick do GamePanel no modo dois Miaus:
 *
 * 1. cada lutador: se bloqueado por animacao -> atualiza parado e IGNORA o
 *    comando; senao aplica o comando e chama atualizar(andando, agachar);
 * 2. orientacao frente a frente pelo X;
 * 3. sistemaCombate.atualizar().
 *
 * O SistemaCombate e criado exatamente como no jogo:
 * CarregadorConteudoCombate.carregarParaJogo().criarSistema(...).
 */
final class SimuladorLuta {

    /** O que um jogador "faz" num tick. */
    record Comando(Consumer<IrmaoMiau> acao, boolean andando, boolean agachando) {

        static final Comando PARADO = new Comando(l -> { }, false, false);

        static Comando andarDireita() {
            return new Comando(l -> l.moverX(l.getVelocidadeBase()), true, false);
        }

        static Comando andarEsquerda() {
            return new Comando(l -> l.moverX(-l.getVelocidadeBase()), true, false);
        }

        static Comando andarParaBaixo() {
            return new Comando(l -> l.moverVertical(l.getVelocidadeBase()), true, false);
        }

        static Comando correrDireita() {
            return new Comando(l -> {
                l.setCorrendo(true);
                l.moverX(l.getVelocidadeCorrida());
            }, true, false);
        }

        static Comando agachar() {
            return new Comando(l -> { }, false, true);
        }

        static Comando pular() {
            return new Comando(IrmaoMiau::pular, false, false);
        }

        static Comando pularCurto() {
            return new Comando(IrmaoMiau::pularCurto, false, false);
        }

        static Comando deitar() {
            return new Comando(IrmaoMiau::alternarDeitado, false, false);
        }

        static Comando rolarCima() {
            return new Comando(l -> l.iniciarRolamento(Direcao.CIMA), false, false);
        }

        static Comando sprawl() {
            return new Comando(IrmaoMiau::iniciarSprawl, false, false);
        }

        static Comando respirar() {
            return new Comando(l -> l.setRespirando(true), false, false);
        }
    }

    final IrmaoMiau branco;
    final IrmaoMiau preto;
    final SistemaCombate sistema;

    /*
     * Branco em x=440 e Preto em x=500: 60 px = 85,7 u, folgado dentro do
     * alcance de 125 u. Assim um passo para tras rompe por MOVIMENTO, e nao
     * por ALCANCE (o limite exato e testado em Pegada1SistemaCombateTest).
     */
    static final int X_BRANCO = 440;
    static final int X_PRETO = 500;

    SimuladorLuta() {
        branco = new IrmaoMiauBranco(X_BRANCO, 150, 0.7);
        preto = new IrmaoMiauPreto(X_PRETO, 150, 0.7);
        orientar();
        sistema = CarregadorConteudoCombate.carregarParaJogo().criarSistema(branco, preto);
    }

    /** Entrega 5A: lutadores prontos (ex.: com atributos de perfil). */
    SimuladorLuta(IrmaoMiau branco, IrmaoMiau preto) {
        this.branco = branco;
        this.preto = preto;
        orientar();
        sistema = CarregadorConteudoCombate.carregarParaJogo().criarSistema(branco, preto);
    }

    SimuladorLuta(PoliticaInteracao politica) {
        branco = new IrmaoMiauBranco(X_BRANCO, 150, 0.7);
        preto = new IrmaoMiauPreto(X_PRETO, 150, 0.7);
        orientar();
        var conteudo = CarregadorConteudoCombate.carregarParaJogo();
        sistema = new SistemaCombate(branco, preto, conteudo.getRegistroAcoes(),
                conteudo.getRegistroAnimacoes(), politica);
    }

    private void orientar() {
        if (branco.getX() < preto.getX()) {
            branco.setViradoParaDireita(true);
            preto.setViradoParaDireita(false);
        } else if (branco.getX() > preto.getX()) {
            branco.setViradoParaDireita(false);
            preto.setViradoParaDireita(true);
        }
    }

    private static void aplicar(IrmaoMiau l, Comando c) {
        if (l.isBloqueadoPorAnimacaoCombate()) {
            l.setCorrendo(false);
            l.setRespirando(false);
            l.atualizar(false, false);
            return;
        }
        l.setCorrendo(false);
        l.setRespirando(false);
        c.acao().accept(l);
        l.atualizar(c.andando(), c.agachando());
    }

    void tick(Comando comandoBranco, Comando comandoPreto) {
        aplicar(branco, comandoBranco);
        aplicar(preto, comandoPreto);
        orientar();
        sistema.atualizar();
    }

    void ticks(int n) {
        for (int i = 0; i < n; i++) {
            tick(Comando.PARADO, Comando.PARADO);
        }
    }

    /** H do Branco + 30 ticks parados: pegada estabelecida. */
    void estabelecerPegada1() {
        ResultadoAcao r = sistema.tentarAcao(branco, preto, "pegada_1");
        if (r != ResultadoAcao.SUCESSO) {
            throw new IllegalStateException("PEGADA_1 deveria ter sucesso: " + r);
        }
        ticks(30);
        if (!sistema.isInteracaoEstabelecida()) {
            throw new IllegalStateException("PEGADA_1 deveria estar estabelecida");
        }
    }
}
