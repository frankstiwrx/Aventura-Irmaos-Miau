package com.irmaosmiau.preferencias;

import com.irmaosmiau.controles.MapaControles;
import com.irmaosmiau.persistencia.PersistenciaException;
import com.irmaosmiau.persistencia.RepositorioControles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Ponto UNICO de verdade dos controles durante a execucao (Entrega 5C).
 *
 * Mesmas regras de PreferenciasJogo:
 * - persistente ({@link #carregar}) le/grava controles.json; em memoria
 *   ({@link #emMemoria}) nunca toca no disco (testes, protótipo);
 * - nada e gravado so por abrir o jogo;
 * - arquivo invalido nao e sobrescrito sem copia: na primeira gravacao ele e
 *   guardado como "controles.json.corrompido-..." e o jogador e avisado.
 *
 * Cada partida (GamePanel) le {@link #getAtual()} ao ser criada.
 */
public final class PreferenciasControles {

    private static PreferenciasControles global;

    private final RepositorioControles repositorio;   // null = em memoria
    private MapaControles atual;
    private boolean arquivoProblematico;
    private final List<String> avisos = new ArrayList<>();

    private PreferenciasControles(RepositorioControles repositorio, MapaControles inicial) {
        this.repositorio = repositorio;
        this.atual = inicial;
    }

    /** Le do disco; arquivo ausente = padroes; arquivo com problema = padroes + aviso. */
    public static PreferenciasControles carregar(RepositorioControles repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("O repositorio e obrigatorio.");
        }
        PreferenciasControles p = new PreferenciasControles(repositorio, MapaControles.padrao());
        try {
            RepositorioControles.Leitura lida = repositorio.carregar();
            p.atual = lida.mapa();
            p.avisos.addAll(lida.avisos());
        } catch (PersistenciaException ex) {
            p.arquivoProblematico = ex.getMotivo() != PersistenciaException.Motivo.ERRO_DE_ARQUIVO;
            p.avisos.add("Não foi possível usar os controles salvos; controles padrão em uso. "
                    + (p.arquivoProblematico
                            ? "O arquivo atual será guardado como cópia quando você salvar. "
                            : "")
                    + "Detalhe: " + ex.getMessage());
        }
        return p;
    }

    public static PreferenciasControles emMemoria(MapaControles inicial) {
        return new PreferenciasControles(null, inicial == null ? MapaControles.padrao() : inicial);
    }

    public static synchronized PreferenciasControles global() {
        if (global == null) {
            global = emMemoria(MapaControles.padrao());
        }
        return global;
    }

    /** null volta para os padroes em memoria. */
    public static synchronized void instalarGlobal(PreferenciasControles preferencias) {
        global = preferencias;
    }

    public MapaControles getAtual() {
        return atual;
    }

    public boolean isPersistente() {
        return repositorio != null;
    }

    public List<String> getAvisos() {
        return List.copyOf(avisos);
    }

    /**
     * Valida e grava. So troca o mapa em uso se a gravacao der certo, para a
     * tela nunca mostrar "salvo" quando nao foi.
     *
     * @throws IllegalArgumentException mapa com conflitos (nada muda)
     * @throws PersistenciaException falha de disco (nada muda)
     */
    public void salvar(MapaControles novo) {
        if (novo == null || !novo.isValido()) {
            throw new IllegalArgumentException("Controles com conflitos não podem ser salvos.");
        }
        if (repositorio != null) {
            if (arquivoProblematico) {
                Path copia = repositorio.guardarComoCorrompido();
                if (copia != null) {
                    avisos.add("Os controles inválidos foram guardados em " + copia.getFileName() + ".");
                }
                arquivoProblematico = false;
            }
            repositorio.salvar(novo);
        }
        atual = novo;
    }
}
