package com.irmaosmiau.ui;

import java.awt.Component;

import javax.swing.JOptionPane;

/**
 * Perguntas e avisos das telas. Interface para que os testes respondam
 * sem abrir janelas (headless).
 */
public interface Dialogos {

    enum Resposta {
        SIM,
        NAO,
        CANCELAR
    }

    /** Pergunta sim/nao/cancelar. */
    Resposta perguntar(Component origem, String titulo, String mensagem);

    /** Confirmacao simples (ex.: excluir). true = confirmado. */
    boolean confirmar(Component origem, String titulo, String mensagem);

    void avisar(Component origem, String titulo, String mensagem);

    /** Implementacao real com JOptionPane. */
    Dialogos SWING = new Dialogos() {
        @Override
        public Resposta perguntar(Component origem, String titulo, String mensagem) {
            int r = JOptionPane.showConfirmDialog(origem, mensagem, titulo,
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (r == JOptionPane.YES_OPTION) {
                return Resposta.SIM;
            }
            return r == JOptionPane.NO_OPTION ? Resposta.NAO : Resposta.CANCELAR;
        }

        @Override
        public boolean confirmar(Component origem, String titulo, String mensagem) {
            return JOptionPane.showConfirmDialog(origem, mensagem, titulo,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        }

        @Override
        public void avisar(Component origem, String titulo, String mensagem) {
            JOptionPane.showMessageDialog(origem, mensagem, titulo, JOptionPane.WARNING_MESSAGE);
        }
    };
}
