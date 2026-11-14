package com.irmaosmiau.preferencias;

import com.irmaosmiau.dispositivos.AssociacaoDispositivos;
import com.irmaosmiau.persistencia.PersistenciaException;
import com.irmaosmiau.persistencia.RepositorioDispositivos;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Escolha de dispositivo de cada slot durante a execucao (Entrega 5D).
 * Mesmas regras de PreferenciasControles: nada e gravado so por abrir o
 * jogo; arquivo invalido = padrao (teclado) + aviso, e o arquivo so vira
 * copia ".corrompido-..." quando o jogador salvar.
 */
public final class PreferenciasDispositivos {

    private static PreferenciasDispositivos global;

    private final RepositorioDispositivos repositorio;   // null = em memoria
    private AssociacaoDispositivos atual;
    private boolean arquivoProblematico;
    private final List<String> avisos = new ArrayList<>();

    private PreferenciasDispositivos(RepositorioDispositivos repositorio, AssociacaoDispositivos inicial) {
        this.repositorio = repositorio;
        this.atual = inicial;
    }

    public static PreferenciasDispositivos carregar(RepositorioDispositivos repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("O repositorio e obrigatorio.");
        }
        PreferenciasDispositivos p = new PreferenciasDispositivos(repositorio, AssociacaoDispositivos.padrao());
        try {
            p.atual = repositorio.carregar();
        } catch (PersistenciaException ex) {
            p.arquivoProblematico = ex.getMotivo() != PersistenciaException.Motivo.ERRO_DE_ARQUIVO;
            p.avisos.add("Não foi possível usar a escolha de dispositivos salva; todos no teclado. "
                    + (p.arquivoProblematico
                            ? "O arquivo atual será guardado como cópia quando você salvar. "
                            : "")
                    + "Detalhe: " + ex.getMessage());
        }
        return p;
    }

    public static PreferenciasDispositivos emMemoria(AssociacaoDispositivos inicial) {
        return new PreferenciasDispositivos(null, inicial == null ? AssociacaoDispositivos.padrao() : inicial);
    }

    public static synchronized PreferenciasDispositivos global() {
        if (global == null) {
            global = emMemoria(AssociacaoDispositivos.padrao());
        }
        return global;
    }

    public static synchronized void instalarGlobal(PreferenciasDispositivos preferencias) {
        global = preferencias;
    }

    public AssociacaoDispositivos getAtual() {
        return atual;
    }

    public List<String> getAvisos() {
        return List.copyOf(avisos);
    }

    public boolean isPersistente() {
        return repositorio != null;
    }

    /** @throws PersistenciaException falha de disco (nada muda) */
    public void salvar(AssociacaoDispositivos nova) {
        if (nova == null) {
            throw new IllegalArgumentException("A associacao e obrigatoria.");
        }
        if (repositorio != null) {
            if (arquivoProblematico) {
                Path copia = repositorio.guardarComoCorrompido();
                if (copia != null) {
                    avisos.add("A escolha de dispositivos inválida foi guardada em " + copia.getFileName() + ".");
                }
                arquivoProblematico = false;
            }
            repositorio.salvar(nova);
        }
        atual = nova;
    }
}
