package com.irmaosmiau.treino;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.conteudo.ConteudoCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.personagens.CatalogoPersonagens;

/**
 * Estado de uma sessao de treino, independente de Swing.
 *
 * Cria jogador e parceiro pelos catalogos, monta o SistemaCombate com o
 * mesmo conteudo do jogo (PEGADA_1 do JSON, PoliticaInteracao padrao) e
 * sabe reiniciar tudo para as posicoes iniciais.
 */
public final class SessaoTreino {

    private final ConfiguracaoTreino configuracao;
    private final CatalogoPersonagens catalogo;
    private final ConteudoCombate conteudo;

    private IrmaoMiau jogador;
    private IrmaoMiau parceiro;
    private SistemaCombate sistema;
    private int reinicios;

    public SessaoTreino(ConfiguracaoTreino configuracao, CatalogoPersonagens catalogo,
            ConteudoCombate conteudo) {
        if (configuracao == null || catalogo == null || conteudo == null) {
            throw new IllegalArgumentException("Configuracao, catalogo e conteudo sao obrigatorios.");
        }
        // Falha cedo, com mensagem clara, se algum id nao existir.
        catalogo.exigir(configuracao.getIdJogador());
        catalogo.exigir(configuracao.getIdParceiro());

        this.configuracao = configuracao;
        this.catalogo = catalogo;
        this.conteudo = conteudo;
        montar();
    }

    private void montar() {
        // Entrega 4D: com escolha (oficial ou perfil), a instancia usa os
        // atributos dela; sem escolha, o oficial do catalogo (como antes).
        // Reiniciar (F5) passa por aqui: recursos voltam ao maximo DO PERFIL.
        jogador = configuracao.getEscolhaJogador() != null
                ? configuracao.getEscolhaJogador().criarLutador(
                        configuracao.getXJogador(), ConfiguracaoTreino.Y_INICIAL, ConfiguracaoTreino.ESCALA)
                : catalogo.exigir(configuracao.getIdJogador()).criarLutador(
                        configuracao.getXJogador(), ConfiguracaoTreino.Y_INICIAL, ConfiguracaoTreino.ESCALA);
        parceiro = configuracao.getEscolhaParceiro() != null
                ? configuracao.getEscolhaParceiro().criarLutador(
                        configuracao.getXParceiro(), ConfiguracaoTreino.Y_INICIAL, ConfiguracaoTreino.ESCALA)
                : catalogo.exigir(configuracao.getIdParceiro()).criarLutador(
                        configuracao.getXParceiro(), ConfiguracaoTreino.Y_INICIAL, ConfiguracaoTreino.ESCALA);

        boolean jogadorAEsquerda = jogador.getX() < parceiro.getX();
        jogador.setViradoParaDireita(jogadorAEsquerda);
        parceiro.setViradoParaDireita(!jogadorAEsquerda);

        sistema = conteudo.criarSistema(jogador, parceiro);
    }

    /** Um tick do parceiro (no lugar do segundo jogador). */
    public void atualizarParceiro() {
        configuracao.getComportamentoParceiro().atualizar(parceiro);
    }

    /**
     * Volta tudo ao inicio: novas instancias nas posicoes iniciais, folego
     * cheio, nenhuma interacao. As instancias antigas sao descartadas.
     */
    public void reiniciar() {
        sistema.encerrarInteracao();
        montar();
        reinicios++;
    }

    public ConfiguracaoTreino getConfiguracao() {
        return configuracao;
    }

    public IrmaoMiau getJogador() {
        return jogador;
    }

    public IrmaoMiau getParceiro() {
        return parceiro;
    }

    public SistemaCombate getSistema() {
        return sistema;
    }

    public int getReinicios() {
        return reinicios;
    }
}
