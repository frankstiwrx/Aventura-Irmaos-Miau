package com.irmaosmiau.combate;

import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.entities.IrmaoMiauBranco;
import com.irmaosmiau.entities.IrmaoMiauPreto;

import java.util.Locale;

/**
 * Entrega 5J: o "molde corporal" de um lutador, para escolher animacoes
 * conjuntas desenhadas para ele.
 *
 * Nao e o slot (J1/J2) nem o papel (aplicador/receptor): e a geometria do
 * rig. O Branco e o Preto usam o mesmo formato de pose (30 valores), mas os
 * pivos de tronco, quadril e pernas sao diferentes; por isso uma trilha
 * desenhada para o Preto nao e copiada para o Branco.
 *
 * Perfis personalizados (4D) sao instancias do personagem base, entao usam
 * o molde do base. Um personagem futuro acrescenta uma constante aqui.
 */
public enum IdentidadeRig {

    BRANCO("Branco"),
    PRETO("Preto");

    private final String nome;

    IdentidadeRig(String nome) {
        this.nome = nome;
    }

    /** Nome usado no MiauStudio ("aplicadorConjunto"). */
    public String getNome() {
        return nome;
    }

    /** Molde do lutador, ou null se for de uma classe ainda sem animacoes conjuntas. */
    public static IdentidadeRig de(IrmaoMiau lutador) {
        if (lutador instanceof IrmaoMiauPreto) {
            return PRETO;
        }
        if (lutador instanceof IrmaoMiauBranco) {
            return BRANCO;
        }
        return null;
    }

    /** "Branco"/"Preto" (sem diferenciar maiusculas). */
    public static IdentidadeRig doNome(String nome) {
        if (nome != null) {
            for (IdentidadeRig r : values()) {
                if (r.nome.toLowerCase(Locale.ROOT).equals(nome.trim().toLowerCase(Locale.ROOT))) {
                    return r;
                }
            }
        }
        throw new IllegalArgumentException("Identidade desconhecida: " + nome + " (use Branco ou Preto)");
    }
}
