package com.irmaosmiau.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Avisos da leitura dos controles (teclado, dispositivos e gamepad), separados em dois tipos:
 *
 * - PEDEM ACAO: uma acao ficou sem tecla/botao, uma tecla passou a ser do teclado-controle do J2, um arquivo
 *   nao pode ser usado ou foi guardado como copia. Aparecem numa linha curta na tela CONTROLES;
 * - INFORMATIVOS: migracoes de padroes antigos ja aplicadas (novos padroes de teclas/botoes). Nao pedem nada
 *   e nao aparecem sozinhos; ficam no botao AVISOS da tela CONTROLES.
 *
 * Nenhum aviso abre janela na abertura do jogo (antes uma janela listava todos, a cada execucao, enquanto o
 * jogador nao salvasse os controles). Nada aqui muda mapeamentos ou arquivos.
 */
final class AvisosControles {

    /** Trechos que indicam algo para o jogador resolver (os textos vem dos repositorios e preferencias). */
    private static final String[] PEDEM_ACAO = {"escolha", "atribua", "não foi possível", "guardad", "sem tecla",
        "sem botão"};

    private AvisosControles() {
    }

    static boolean pedeAcao(String aviso) {
        String t = aviso.toLowerCase(Locale.ROOT);
        for (String p : PEDEM_ACAO) {
            if (t.contains(p)) {
                return true;
            }
        }
        return false;
    }

    static List<String> quePedemAcao(List<String> avisos) {
        List<String> r = new ArrayList<>();
        for (String a : avisos) {
            if (pedeAcao(a)) {
                r.add(a);
            }
        }
        return r;
    }

    /** Linha curta da tela CONTROLES (null = nada a mostrar: so informativos ou nenhum). */
    static String resumo(List<String> avisos) {
        List<String> acao = quePedemAcao(avisos);
        if (acao.isEmpty()) {
            return null;
        }
        return acao.size() == 1 ? acao.get(0)
                : acao.size() + " ajustes de controles precisam de atenção. Veja em AVISOS.";
    }

    /** Texto do botao AVISOS: primeiro o que pede acao, depois as atualizacoes ja aplicadas. */
    static String detalhes(List<String> avisos) {
        StringBuilder b = new StringBuilder();
        List<String> acao = quePedemAcao(avisos);
        if (!acao.isEmpty()) {
            b.append("PRECISAM DE ATENÇÃO").append(System.lineSeparator());
            acao.forEach(a -> b.append("• ").append(a).append(System.lineSeparator()));
        }
        List<String> info = new ArrayList<>(avisos);
        info.removeAll(acao);
        if (!info.isEmpty()) {
            if (b.length() > 0) {
                b.append(System.lineSeparator());
            }
            b.append("ATUALIZAÇÕES JÁ APLICADAS (SALVAR grava e encerra estes avisos)").append(System.lineSeparator());
            info.forEach(a -> b.append("• ").append(a).append(System.lineSeparator()));
        }
        return b.toString().trim();
    }
}
