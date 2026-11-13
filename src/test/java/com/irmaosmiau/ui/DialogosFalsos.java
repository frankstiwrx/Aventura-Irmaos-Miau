package com.irmaosmiau.ui;

import java.awt.Component;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Dialogos com respostas programadas, sem abrir janelas. */
final class DialogosFalsos implements Dialogos {

    final Deque<Resposta> respostas = new ArrayDeque<>();
    final Deque<Boolean> confirmacoes = new ArrayDeque<>();
    final List<String> perguntas = new ArrayList<>();
    final List<String> avisos = new ArrayList<>();

    @Override
    public Resposta perguntar(Component origem, String titulo, String mensagem) {
        perguntas.add(mensagem);
        if (respostas.isEmpty()) {
            throw new AssertionError("pergunta inesperada: " + mensagem);
        }
        return respostas.removeFirst();
    }

    @Override
    public boolean confirmar(Component origem, String titulo, String mensagem) {
        perguntas.add(mensagem);
        if (confirmacoes.isEmpty()) {
            throw new AssertionError("confirmacao inesperada: " + mensagem);
        }
        return confirmacoes.removeFirst();
    }

    @Override
    public void avisar(Component origem, String titulo, String mensagem) {
        avisos.add(mensagem);
    }
}
