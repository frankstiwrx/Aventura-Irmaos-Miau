package com.irmaosmiau.posicional;

/**
 * Um dos dois lutadores da dupla (Entrega 5E), igual ao par A/B do
 * SistemaCombate (getLutadorA/getLutadorB). O modelo posicional nao guarda
 * referencias a IrmaoMiau: e logica pura, testavel sem Swing.
 */
public enum Lado {
    A,
    B;

    public Lado outro() {
        return this == A ? B : A;
    }
}
