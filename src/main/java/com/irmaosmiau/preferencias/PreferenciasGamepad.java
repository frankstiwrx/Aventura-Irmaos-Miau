package com.irmaosmiau.preferencias;

import com.irmaosmiau.dispositivos.MapaGamepad;
import com.irmaosmiau.persistencia.PersistenciaException;
import com.irmaosmiau.persistencia.RepositorioGamepad;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Esquema e botoes do gamepad em uso (Entrega 5F). Mesmas regras das outras
 * preferencias: nada e gravado ao abrir; arquivo invalido = padrao CLASSICO
 * + aviso; o arquivo so vira copia ".corrompido-..." ao salvar.
 */
public final class PreferenciasGamepad {

    private static PreferenciasGamepad global;

    private final RepositorioGamepad repositorio;   // null = em memoria
    private MapaGamepad atual;
    private boolean arquivoProblematico;
    private final List<String> avisos = new ArrayList<>();

    private PreferenciasGamepad(RepositorioGamepad repositorio, MapaGamepad inicial) {
        this.repositorio = repositorio;
        this.atual = inicial;
    }

    public static PreferenciasGamepad carregar(RepositorioGamepad repositorio) {
        PreferenciasGamepad p = new PreferenciasGamepad(repositorio, MapaGamepad.padrao());
        try {
            p.atual = repositorio.carregar();
        } catch (PersistenciaException ex) {
            p.arquivoProblematico = ex.getMotivo() != PersistenciaException.Motivo.ERRO_DE_ARQUIVO;
            p.avisos.add("Não foi possível usar os botões de gamepad salvos; esquema Clássico em uso. "
                    + (p.arquivoProblematico ? "O arquivo atual será guardado como cópia quando você salvar. " : "")
                    + "Detalhe: " + ex.getMessage());
        }
        return p;
    }

    public static PreferenciasGamepad emMemoria(MapaGamepad inicial) {
        return new PreferenciasGamepad(null, inicial == null ? MapaGamepad.padrao() : inicial);
    }

    public static synchronized PreferenciasGamepad global() {
        if (global == null) {
            global = emMemoria(MapaGamepad.padrao());
        }
        return global;
    }

    public static synchronized void instalarGlobal(PreferenciasGamepad p) {
        global = p;
    }

    public MapaGamepad getAtual() {
        return atual;
    }

    public List<String> getAvisos() {
        return List.copyOf(avisos);
    }

    /**
     * @throws IllegalArgumentException conflitos (nada muda)
     * @throws PersistenciaException falha de disco (nada muda)
     */
    public void salvar(MapaGamepad novo) {
        if (novo == null || !novo.isValido()) {
            throw new IllegalArgumentException("Botões de gamepad com conflitos não podem ser salvos.");
        }
        if (repositorio != null) {
            if (arquivoProblematico) {
                Path copia = repositorio.guardarComoCorrompido();
                if (copia != null) {
                    avisos.add("Os botões de gamepad inválidos foram guardados em " + copia.getFileName() + ".");
                }
                arquivoProblematico = false;
            }
            repositorio.salvar(novo);
        }
        atual = novo;
    }
}
