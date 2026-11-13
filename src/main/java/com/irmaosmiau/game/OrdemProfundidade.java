package com.irmaosmiau.game;

import com.irmaosmiau.entities.IrmaoMiau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Ordem de desenho dos lutadores pela profundidade da arena.
 *
 * Criterio: a LINHA logica (0 = mais ao fundo, 4 = mais perto da camera).
 * Quem esta numa linha mais ao fundo e desenhado primeiro (fica atras).
 *
 * Nao usa Y da tela nem articulacoes: pular, agachar ou uma pose externa
 * do MiauStudio nao mudam a linha, entao nao trocam a ordem. Empate
 * (mesma linha) mantem a ordem recebida (ordenacao estavel), sem oscilar.
 */
final class OrdemProfundidade {

    private static final Comparator<IrmaoMiau> POR_LINHA =
            Comparator.comparingInt(IrmaoMiau::getLinhaAtual);

    private OrdemProfundidade() {
    }

    /** Lutadores do fundo para a frente; empate preserva a ordem dos argumentos. */
    static List<IrmaoMiau> doFundoParaFrente(IrmaoMiau... lutadores) {
        List<IrmaoMiau> ordem = new ArrayList<>(Arrays.asList(lutadores));
        ordem.sort(POR_LINHA);   // List.sort e estavel
        return ordem;
    }
}
